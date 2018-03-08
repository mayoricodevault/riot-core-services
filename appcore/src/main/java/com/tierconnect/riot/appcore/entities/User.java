package com.tierconnect.riot.appcore.entities;

import com.tierconnect.riot.commons.Constants;

import java.util.*;

import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

@Entity
//"User is reserved in MS SQL - cannot be used"
@Table(name = "User0", indexes = {@Index(name = "IDX_user_apiKey", columnList = "apiKey"), @Index(name =
        "IDX_user_username", columnList = "username")})
public class User extends UserBase {

    public User() {
        super();
    }

    public User(String username) {
        super();
        this.username = username;
    }

    //TODO: refactor to eliminate this ! Bad idea ! Do this in the services class, or better yet, handle through use
    // of the extra param !
    public Map<String, Object> publicMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", this.id);
        map.put("archived", this.archived);
        map.put("username", this.username);
        map.put("firstName", this.firstName);
        map.put("lastName", this.lastName);
        map.put("email", this.email);
        map.put("hiddenTabs", this.hiddenTabs);
        map.put("timeZone", this.timeZone);
        map.put("dateFormat", this.dateFormat);

        //Userfields
        List<Map<String, Object>> userFieldsList = new LinkedList<>();
        if (this.getUserFieldUsers() != null) {
            for (UserField userFieldUser : this.getUserFieldUsers()) {
                Map<String, Object> userFieldMap = userFieldUser.publicMap();
                userFieldsList.add(userFieldMap);
            }
        }
        map.put("userFields", userFieldsList);
        //Roles

        List<Map<String, Object>> userRolesList = new LinkedList<>();
        if (this.getUserRoles() != null) {
            for (UserRole userRole : this.getUserRoles()) {
                userRolesList.add(userRole.getRole().publicMap());
            }
        }
        map.put("userRoles", userRolesList);

        return map;
    }

    @Override
    public String toString() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", this.id);
        map.put("archived", this.archived);
        map.put("username", this.username);
        map.put("email", this.email);
        return map.toString();
    }

    @Override
    @Deprecated
    /**
     * @deprecated use user.getActiveGroup when no specific table/entity/class is provided, otherwise VisibilityUtils
     * .getVisibilityGroup(Class);
     */
    public Group getGroup() {
        return super.getGroup();
    }

    /**
     * active group will depend on roamingGroup in the future
     *
     * @return a Group instance
     */
    public Group getActiveGroup() {
        if (getRoamingGroup() != null) {
            return getRoamingGroup();
        } else {
            //noinspection deprecation
            return getGroup();
        }
    }

    public Set<Long> getRoleIds() {
        Set<Long> result = new HashSet<>();
        Set<UserRole> userRoles1 = getUserRoles();
        if (userRoles1 != null) {
            for (UserRole userRole : userRoles1) {
                result.add(userRole.getRole().getId());
            }
        }
        return result;
    }

    public Set<Long> getThingTypeResources() {
        Set<Long> ttList = new HashSet<>();
        for (UserRole userRole : getUserRoles()){
            for (RoleResource roleResource : userRole.getRole().getRoleResources()){
                String resourceName = roleResource.getResource().getName();
                if (resourceName.contains("_thingtype_")){
                    ttList.add(Long.parseLong(resourceName.replace("_thingtype_", "")));
                }
            }
        }
        return ttList;
    }

    private UserPassword getActiveUserPassword() {
        if (userPasswords != null) {
            for (UserPassword password : userPasswords) {
                if (password.getStatus().equals(Constants.PASSWORD_STATUS_ACTIVE)) {
                    return password;
                }
            }
            for (UserPassword password : userPasswords) {
                if (password.getStatus().equals(Constants.PASSWORD_STATUS_PENDING)) {
                    return password;
                }
            }
        } else {
            userPasswords = new HashSet<>();
        }
        return null;
    }

    public String getHashedPassword() {
        UserPassword userPassword = getActiveUserPassword();
        if (userPassword != null) {
            return userPassword.getHashedPassword();
        }
        return "";
    }

    public Long getLastPasswordChange() {
        UserPassword userPassword = getActiveUserPassword();
        if (userPassword != null) {
            return userPassword.getCreationTime();
        }
        return 0L;
    }

    public String getLastPasswordStatus() {
        UserPassword userPassword = getActiveUserPassword();
        if (userPassword != null) {
            return userPassword.getStatus();
        }
        return null;
    }

    public Long getLastFailedAttempt() {
        UserPassword userPassword = getActiveUserPassword();
        if (userPassword != null) {
            return userPassword.getLastFailedTime();
        }
        return 0L;
    }

    public Long getPasswordFailedAttempts() {
        UserPassword userPassword = getActiveUserPassword();
        if (userPassword != null) {
            return userPassword.getFailedAttempts();
        }
        return 0L;
    }

    public void setLastPasswordChange(Long lastPasswordChange) {
        UserPassword userPassword = getActiveUserPassword();
        if (userPassword != null) {
            userPassword.setCreationTime(lastPasswordChange);
        }
    }

    public void setPasswordFailedAttempts(Long failedAttempts) {
        UserPassword userPassword = getActiveUserPassword();
        if (userPassword != null) {
            userPassword.setFailedAttempts(failedAttempts);
            if (failedAttempts > 0) {
                userPassword.setLastFailedTime(System.currentTimeMillis());
            }
        }
    }
}
