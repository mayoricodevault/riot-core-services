package com.tierconnect.riot.sdk.utils;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;

import java.util.*;

public class PermissionsUtils {
    //When disabled the logged user permission validation is not executed
    static boolean  authorizationEnabled = true;
    static boolean  fieldAuthorizationEnabled = false;

    static Logger logger = Logger.getLogger(PermissionsUtils.class);

    public static boolean  isAuthorizationEnabled() {
        return  authorizationEnabled;
    }

    public static boolean  isFieldAuthorizationEnabled() {
        return  fieldAuthorizationEnabled;
    }

/*
    public static boolean isPermitted(String field, String object, String permission) {
        if (!isAuthorizationEnabled()) {
            return true;
        }
        Subject subject = SecurityUtils.getSubject();
        return subject.isPermitted(object + "." + field + ":" + permission);
    }

    public static String getGetterName(String field) {
        if (field == null) {
            return null;
        }
        return "get" + generateMethodName(field);
    }

    public static String getSetterName(String field) {
        if (field == null) {
            return null;
        }
        return "set" + generateMethodName(field);
    }

    public static String generateMethodName(String field) {
        if (field == null) {
            return null;
        }
        return field.substring(0, 1).toUpperCase() + field.substring(1);
    }

    public static Object get(String field, String clazz, Object object) {
        if (isAuthorizationEnabled() &&  isPermitted(field, clazz, "r")) {
            throw new UserException(String.format("User has no permissions for getting the field [%s].", field));
        } else {
            try {
                return object.getClass().getMethod(getGetterName(field)).invoke(object);
            } catch (Exception e) {
                throw new UserException(String.format("Error getting the field [%s].", field));
            }
        }
    }

    public static void set(String field, String clazz, Object object, Object value) {
        if (isAuthorizationEnabled() && isPermitted(field, clazz, "w")) {
            throw new UserException(String.format("User has no permissions for setting the field [%s].", field));
        } else {
            try {
                object.getClass().getMethod(getSetterName(field)).invoke(object, value);
            } catch (Exception e) {
                throw new UserException(String.format("Error setting the field [%s].", field));
            }
        }
    }
    */

    public static boolean isPermittedAny(Subject currentUser, List<String> permissions) {
        return isPermittedAny(currentUser, permissions.toArray(new String[]{}));
    }

    public static boolean isPermittedAny(Subject currentUser, String... permissions) {
        return isPermittedAny(currentUser, new HashMap<String, Boolean>(), permissions);
    }

    public static boolean isPermittedAny(Subject currentUser, Map<String, Boolean> permissionCache, List<String> permissions) {
        return isPermittedAny(currentUser, permissionCache, permissions.toArray(new String[]{}));
    }

    public static boolean isPermittedAny(Subject currentUser, Map<String, Boolean> permissionCache, String... permissions) {
        if (!isAuthorizationEnabled()) {
            return true;
        }
        for (String permission : permissions) {
            if (isPermitted(currentUser, permissionCache, permission)) {
                return true;
            }
        }
        logger.debug("AGG User doesn't any of the resources: " + Arrays.asList(permissions) + " user:" + currentUser.getPrincipal(), new Exception());
        return false;
    }

    public static boolean isPermittedAll(Subject currentUser, List<String> permissions) {
        return isPermittedAll(currentUser, permissions.toArray(new String[]{}));
    }

    public static boolean isPermittedAll(Subject currentUser, String... permissions) {
        return isPermittedAll(currentUser, new HashMap<String, Boolean>(), permissions);
    }

    public static boolean isPermittedAll(Subject currentUser, Map<String, Boolean> permissionCache, List<String> permissions) {
        return isPermittedAll(currentUser, permissionCache, permissions.toArray(new String[]{}));
    }

    public static boolean isPermittedAll(Subject currentUser, Map<String, Boolean> permissionCache, String... permissions) {
        if (!isAuthorizationEnabled()) {
            return true;
        }
        for (String permission : permissions) {
            if (!isPermitted(currentUser, permissionCache, permission)) {
                //Exception e = new Exception();
                logger.warn("AGG User doesn't have resource: " + permission + " user:" + currentUser.getPrincipal());
                return false;
            }
        }
        return true;
    }

    public static boolean isPermitted(Subject currentUser, String permission) {
        return isPermitted(currentUser, new HashMap<String, Boolean>(), permission);
    }

    public static boolean isPermitted(Subject currentUser, Map<String, Boolean> permissionCache, String permission) {
        permission= permission.trim();
        if ("true".equals(permission)) {
            return true;
        } else if ("false".equals(permission)) {
            return false;
        }
        Boolean permitted = permissionCache.get(permission);
        if (permitted == null) {
            permitted = currentUser.isPermitted(permission);
            permissionCache.put(permission, permitted);
        }
        return permitted;
    }

    public static boolean buildSearch(Subject currentUser, Map<String, Boolean> permissionCache, String searchString) {
        if (StringUtils.isBlank(searchString)) {
            return false;
        }
        //1st Priority Parenthesis
        int startParenthesisIndex = getIndexOf(searchString, '(');
        if (startParenthesisIndex != -1) {
            //a&(
            //(
            int endParenthesisIndex = getIndexOfEndParenthesis(searchString, startParenthesisIndex);
            if (endParenthesisIndex == -1) {
                throw new RuntimeException("Matching closing parenthesis was not found.");
            }
            boolean center = buildSearch(currentUser, permissionCache, searchString.substring(startParenthesisIndex + 1, endParenthesisIndex));
            if (startParenthesisIndex >= 2) {
                boolean left = buildSearch(currentUser, permissionCache, searchString.substring(0, startParenthesisIndex - 1));
                char operator = searchString.substring(startParenthesisIndex - 1, startParenthesisIndex).charAt(0);
                center = andOrExpressions(left, center, operator);
            }
            //)
            //)|a
            if (endParenthesisIndex < searchString.length() - 2) {
                boolean right = buildSearch(currentUser, permissionCache, searchString.substring(endParenthesisIndex + 1, searchString.length()));
                char operator = searchString.substring(endParenthesisIndex + 1, endParenthesisIndex + 2).charAt(0);
                center = andOrExpressions(center, right, operator);
            }
            return center;
        }
        //2nd Priority And
        {
            int startAndIndex = getIndexOf(searchString, '&');
            if (startAndIndex != -1) {
                boolean left = buildSearch(currentUser, permissionCache, searchString.substring(0, startAndIndex));
                boolean right = buildSearch(currentUser, permissionCache, searchString.substring(startAndIndex + 1, searchString.length()));
                return andOrExpressions(left, right, '&');
            }
        }

        //3er Priority Or
        {
            int startOrIndex = getIndexOf(searchString, '|');
            if (startOrIndex != -1) {
                boolean left = buildSearch(currentUser, permissionCache, searchString.substring(0, startOrIndex));
                boolean right = buildSearch(currentUser, permissionCache, searchString.substring(startOrIndex + 1, searchString.length()));
                return andOrExpressions(left, right, '|');
            }
        }
        //we have a simple condition string
        return isPermitted(currentUser, permissionCache, searchString);
    }


    private static boolean andOrExpressions(boolean left, boolean right, char operator) {
        if (operator == '&') {
            return left && right;
        }
        if (operator == '|') {
            return left || right;
        }
        throw new RuntimeException("Not supported");
    }


    //With escaping
    public static int getIndexOf(String s, char c) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == c) {
                if (i == 0 || s.charAt(i - 1) != '\\') {
                    return i;
                }
            }
        }
        return -1;
    }

    //With escaping
    public static int getIndexOfEndParenthesis(String s, int start) {
        if (s.charAt(start) != '(') {
            throw new RuntimeException("Error start parenthesis not found");
        }
        int n = 1;
        for (int i = start + 1; i < s.length(); i++) {
            if (s.charAt(i) == '(') {
                if (s.charAt(i - 1) != '\\') { //for Escaping
                    n++;
                }
            }
            if (s.charAt(i) == ')') {
                if (s.charAt(i - 1) != '\\') { //for Escaping
                    n--;
                }
            }
            if (n == 0) {
                return i;
            }
        }
        return -1;
    }

    public static Subject loginUser(AuthenticationToken credentials) {
        ensureUserIsLoggedOut();
        Subject subject = getSubject();
        subject.login(credentials);
        //TODO this is necessary?
        ThreadContext.bind(subject);
        return subject;
    }

    // Clean way to get the subject
    public static Subject getSubject() {
        Subject currentUser = ThreadContext.getSubject();
        if (currentUser == null) {
            currentUser = SecurityUtils.getSubject();
        }
        return currentUser;
    }

    // Logout the user fully before continuing.
    private static void ensureUserIsLoggedOut()
    {
        try
        {
            // Get the user if one is logged in.
            Subject currentUser = getSubject();
            if (currentUser == null)
                return;

            // Log the user out and kill their session if possible.
            currentUser.logout();
            Session session = currentUser.getSession(false);
            if (session == null)
                return;

            session.stop();
        }
        catch (Exception e)
        {
            // Ignore all errors, as we're trying to silently
            // log the user out.
        }
    }
    
}
