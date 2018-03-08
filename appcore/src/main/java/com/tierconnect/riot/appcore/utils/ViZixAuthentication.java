package com.tierconnect.riot.appcore.utils;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.services.ConfigurationService;
import com.tierconnect.riot.appcore.services.UserService;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.utils.HashUtils;
import org.apache.log4j.Logger;
import org.hibernate.Transaction;
import org.jose4j.json.internal.json_simple.JSONObject;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by fflores on 9/1/16.
 */
public class ViZixAuthentication implements Authentication {

    private static final Logger logger = Logger.getLogger( ViZixAuthentication.class );

    @Override
    public boolean authenticateUser(String username, String password, Group group) {
        User user = UserService.getInstance().selectByUsernameGroup(username, group.getId());
        if (user != null){
            int maxFailedAttempts =
                ConfigurationService.getAsInteger(group, "passwordFailedAttempts");
            Long passwordExpirationTime =
                TimeUnit.DAYS.toMillis(ConfigurationService.getAsLong(group, "passwordExpirationDays"));
            Long lastPasswordChange = user.getLastPasswordChange();
            Long passwordFailedAttempts = user.getPasswordFailedAttempts();
            if (passwordFailedAttempts >= maxFailedAttempts){
                Long nextTime = user.getLastFailedAttempt() +
                    TimeUnit.SECONDS.toMillis(ConfigurationService.getAsInteger(group, "passwordFailedLockTime"))
                    - System.currentTimeMillis();
                if (nextTime > 0) {
                    logger.warn("Password Policy Auditing: " + passwordFailedAttempts
                        + " failed password attempts, account locked for " + Utilities.getTimeMessageFromMillis(nextTime));
                    throw new UserException(
                        "Too many failed login attempts, your account has been locked for "
                            + Utilities.getTimeMessageFromMillis(nextTime), 429);
                }
            }
            if (HashUtils.hashSHA256(password).equals(user.getHashedPassword())){
                if (passwordExpirationTime > 0L && lastPasswordChange != null
                    && (lastPasswordChange + passwordExpirationTime) < System.currentTimeMillis()
                    && UserService.getInstance().hasPasswordExpirationPolicy(user)){
                    JSONObject message = new JSONObject();
                    message.put("passwordPolicy", "Password expired on "
                        + UserService.getInstance().getPasswordExpirationTime(user).get("passwordExpirationTime"));
                    message.put("id", String.valueOf(user.getId()));
                    message.put("passwordPolicies",
                        UserService.getInstance().getPasswordPolicies(user.getActiveGroup()));
                    logger.warn("Password Policy Auditing: User must change password after expiration time.");
                    throw new UserException(message.toString(), 403);
                }
                if (UserService.getInstance().isFirstLogin(user)){
                    JSONObject message = new JSONObject();
                    message.put("passwordPolicy", "Change Password on First Login");
                    message.put("id", String.valueOf(user.getId()));
                    message.put("passwordPolicies",
                        UserService.getInstance().getPasswordPolicies(user.getActiveGroup()));
                    logger.warn("Password Policy Auditing: User must change password on first login.");
                    throw new UserException(message.toString(), 403);
                }
                if (UserService.getInstance().forcePasswordChange(user)){
                    JSONObject message = new JSONObject();
                    message.put("passwordPolicy", "Change Password on Next Login");
                    message.put("id", String.valueOf(user.getId()));
                    message.put("passwordPolicies",
                        UserService.getInstance().getPasswordPolicies(user.getActiveGroup()));
                    logger.warn("Password Policy Auditing: User must change password on next login.");
                    throw new UserException(message.toString(), 403);
                }
                user.setPasswordFailedAttempts(0L);
                UserService.getInstance().update(user);
            } else {
                Transaction transaction = UserService.getUserDAO().getSession().getTransaction();
                user.setPasswordFailedAttempts(passwordFailedAttempts + 1L);
                UserService.getInstance().update(user);
                transaction.commit();
                logger.warn("Password Policy Auditing: " + (passwordFailedAttempts + 1)
                    + " failed password attempts.");
                return false;
            }
        } else {
            try {
                if (UserService.getInstance().getByUsername(username, group) != null) {
                    logger.warn("Insufficient permissions to enter application.");
                    throw new UserException("Insufficient permissions to enter application.");
                }
            } catch (NonUniqueResultException e) {
                logger.warn("Insufficient permissions to enter application.");
                throw new UserException("Insufficient permissions to enter application.");
            } return false;
        }
        logger.info(username + " has been authenticated using ViZix");
        return true;
    }

    @Override
    public void setContextSource(Map<String, String> properties) {

    }

}
