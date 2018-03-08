package com.tierconnect.riot.utils;

import com.tierconnect.riot.appcore.controllers.RiotPermission;
import com.tierconnect.riot.appcore.dao.UserDAO;
import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.sdk.servlet.security.ApiKeyToken;
import org.apache.log4j.Logger;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.authz.permission.WildcardPermission;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;

import java.util.*;

/**
 * Created by agutierrez on 30-05-14.
 */
public class RiotShiroTestRealm extends AuthorizingRealm {
    static Logger logger = Logger.getLogger(RiotShiroTestRealm.class);

    QUser qUser = QUser.user;
    QResource qResource = QResource.resource;

    static Set<Permission> resultPermissions = new HashSet<>();
    static Set<String> resultRoles = new HashSet<>();

    public static void setRoles(String... theRoles) {
        resultRoles.clear();
        for (String s : theRoles) {
            resultRoles.add(s);
        }
    }

    public static void setResultPermissions(String... thePermissions) {
        resultPermissions.clear();
        for (String s : thePermissions) {
            resultPermissions.add(new RiotPermission(s));
        }
    }


    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        Collection<User> principalsList = principals.byType(User.class);
        if (principalsList.isEmpty()) {
            throw new AuthorizationException("Empty principals list!");
        }
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo(resultRoles);
        info.setRoles(resultRoles);
        info.setObjectPermissions(resultPermissions);
        return info;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        ApiKeyToken apiKeyToken = (ApiKeyToken) token;
        User user = getUserByApiKey(apiKeyToken.getApiKey());
        if (user == null) {
            throw new AuthenticationException("Api Key [" + apiKeyToken.getApiKey() + "] not found!");
        }
        return new SimpleAuthenticationInfo(user, user.getApiKey(), getName());
    }


    @Override
    public Class getAuthenticationTokenClass() {
        return ApiKeyToken.class;
    }


    private User getUserByApiKey(String apiKey) {
        return new UserDAO().getQuery().where(qUser.apiKey.eq(apiKey)).uniqueResult(qUser);
    }

}
