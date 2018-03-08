package com.tierconnect.riot.appcore.services;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.types.OrderSpecifier;
import com.tierconnect.riot.appcore.dao.UserPasswordDAO;
import com.tierconnect.riot.appcore.entities.QUserPassword;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.entities.UserPassword;
import com.tierconnect.riot.appcore.utils.QueryUtils;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.sdk.dao.Pagination;

import javax.annotation.Generated;
import java.util.ArrayList;
import java.util.List;

@Generated("com.tierconnect.riot.appgen.service.GenService")
public class UserPasswordService extends UserPasswordServiceBase 
{
    @Override
    public UserPassword insert(UserPassword userPassword) {
        UserPassword userPassword1 = getUserPassword(userPassword.getUser());
        if (userPassword1 != null) {
            userPassword1.setStatus(Constants.PASSWORD_STATUS_INACTIVE);
            update(userPassword1);
        }
        userPassword.setCreationTime(System.currentTimeMillis());
        userPassword.setFailedAttempts(0L);
        return super.insert(userPassword);
    }

    public UserPassword getUserPassword(User user) {
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QUserPassword.userPassword.user.eq(user));
        be = be.and(QUserPassword.userPassword.status.eq(Constants.PASSWORD_STATUS_ACTIVE));
        UserPassword userPassword = getUserPasswordDAO().selectBy(be);
        if (userPassword != null) {
            return userPassword;
        } else {
            be = new BooleanBuilder();
            be = be.and(QUserPassword.userPassword.user.eq(user));
            be = be.and(QUserPassword.userPassword.status.eq(Constants.PASSWORD_STATUS_PENDING));
            return getUserPasswordDAO().selectBy(be);
        }
    }

    public List<UserPassword> getLastUserPasswords(User user, int maxPasswords) {
        if (user.getId() != null) {
            BooleanBuilder be = new BooleanBuilder();
            be = be.and(QUserPassword.userPassword.user.eq(user));
            Pagination pg = new Pagination(1, maxPasswords);
            OrderSpecifier[] os =
                QueryUtils.getOrderFields(QUserPassword.userPassword, "creationTime:desc,id:desc");
            return getUserPasswordDAO().selectAll(be, pg, os);
        }
        return new ArrayList<>();
    }

    public void forcePasswordChange(User user) {
        UserPassword userPassword = getUserPassword(user);
        if (userPassword != null) {
            userPassword.setStatus(Constants.PASSWORD_STATUS_INACTIVE);
            update(userPassword);
            userPassword.setStatus(Constants.PASSWORD_STATUS_PENDING);
            super.insert(userPassword);
        }
    }

    public void avoidPasswordChange(User user) {
        UserPassword userPassword = getUserPassword(user);
        if (userPassword != null) {
            userPassword.setStatus(Constants.PASSWORD_STATUS_ACTIVE);
            update(userPassword);
        }
    }

    public void deleteByUser(User user) {
        UserPasswordDAO userPasswordDAO = getUserPasswordDAO();
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QUserPassword.userPassword.user.id.eq(user.getId()));

        List<UserPassword> userPasswords = userPasswordDAO.selectAll(be, null, null);
        for (UserPassword userPassword : userPasswords) {
            userPasswordDAO.delete(userPassword);
        }

    }
}

