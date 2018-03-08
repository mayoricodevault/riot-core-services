package com.tierconnect.riot.appcore.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.services.*;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by fflores on 9/1/16.
 */
public class AuthenticationUtils {

    static Logger logger = Logger.getLogger(AuthenticationUtils.class);

    public static final String NATIVE_AUTHENTICATION = "nativeAuthentication";
    public static final String LDAP_AD_AUTHENTICATION = "ldapAdAuthentication";
    public static final String OAUTH2_AUTHENTICATION = "oauth2Authentication";
    public static final String AUTHENTICATION_MODE = "authenticationMode";
    public static final String PROPERTY_LDAP_AD_CONNECTION = "ldapAdConnection";
    public static final String VALIDATE_USER_CREATION = "ldapValidateUserCreation";
    public static final String PROPERTY_AUTO_CREATE_USER = "autocreateUser";
    public static final String PROPERTY_USER_ROLE = "userRole";

    /**
     * executes an Authentication Strategy with strategies defined by tenant group
     * @param username
     * @param password
     * @param group
     * @param user
     * @param request
     */
    public User executeAuthenticationStrategy(String username, String password, User user, Group group, HttpServletRequest request){
        User userResponse = null;
        List<String> authenticationModes = new ArrayList<>();
        if (group != null){
            authenticationModes = getAuthenticationTypeByGroup(AUTHENTICATION_MODE,group);
        } else if (request.getHeader("oauth2token") != null && !(request.getHeader("oauth2token")).isEmpty()){
            authenticationModes.add(OAUTH2_AUTHENTICATION);
        }
        boolean authenticated = false;
        for (String authenticationMode : authenticationModes){
            switch (authenticationMode){
                case NATIVE_AUTHENTICATION:
                    ViZixAuthentication viZixAuthentication = new ViZixAuthentication();
                    authenticated = viZixAuthentication.authenticateUser(username,password,group);
                    if (authenticated){
                        userResponse = user;
                    }
                    break;
                case LDAP_AD_AUTHENTICATION:
                    LdapAuthentication ldapAuthentication = new LdapAuthentication();
                    ldapAuthentication.setContextSource(getConnectionProperties(group, PROPERTY_LDAP_AD_CONNECTION));
                    String ldapUserIdentifier = ConfigurationService.getAsString(group,"ldapUserIdentifier");
                    ldapAuthentication.setUserIdentifier(ldapUserIdentifier);
                    authenticated = ldapAuthentication.authenticateUser(username,password,group);
                    if (authenticated){
                        userResponse = user;
                    }
                    break;
                case OAUTH2_AUTHENTICATION:
                    group = GroupService.getInstance().getRootGroup();
                    OAuth2Authentication oAuth2Authentication = new OAuth2Authentication(request);
                    oAuth2Authentication.setContextSource(getConnectionProperties(group,"oauth2Connection"));
                    authenticated = oAuth2Authentication.authenticateUser(username,password,group);
                    if (username == null || username.isEmpty()) {
                        username = oAuth2Authentication.getUserName();
                    }
                    if (username != null && !username.isEmpty()){
                        List<User> users = UserService.selectUsersByUsername(username);
                        if (users.size() > 1){
                            throw new UserException("There is more than one user with the same userName.");
                        }
                        if (users != null && !users.isEmpty()){
                            userResponse = users.get(0);
                            group = userResponse.getActiveGroup();
                        }
                    }
                    break;
            }
            if (authenticated){
                break;
            }
        }
        return userResponse;
    }

    /**
     * validate if user exists in an external server, it depends of type authentication
     * @param user
     * @param group
     * @return
     */
    public void validateUserExists(User user, Group group, String operation){
        if(user.getUsername()==null || (user.getUsername()!=null && user.getUsername().isEmpty())){
            throw  new UserException("'User Name' is required.");
        }
        List<String> authenticationTypes = getAuthenticationTypeByGroup(AUTHENTICATION_MODE,group);
        for (String authenticationType : authenticationTypes){
            switch (authenticationType){
                case NATIVE_AUTHENTICATION:
                    if ("create".equals(operation)) {
                        if(user.getPassword()==null || (user.getPassword()!=null && user.getPassword().isEmpty())){
                            throw  new UserException("'Password' is required.");
                        }
                    }
                case LDAP_AD_AUTHENTICATION:
                    Boolean validateUserCreation = ConfigurationService.getAsBoolean(group,VALIDATE_USER_CREATION);
                    if (validateUserCreation){
                        LdapAuthentication ldapAuthentication = new LdapAuthentication();
                        String ldapUserIdentifier = ConfigurationService.getAsString(group,"ldapUserIdentifier");
                        ldapAuthentication.setUserIdentifier(ldapUserIdentifier);
                        ldapAuthentication.setContextSource(getConnectionProperties(group, PROPERTY_LDAP_AD_CONNECTION));
                        if (!ldapAuthentication.validateUser(user.getUsername())){
                            throw new UserException("User [" + user.getUsername() + "] cannot be created or modified because does not exist in LDAP/AD server.");
                        }
                    }
                    break;
            }
        }
    }

    /**
     * get authentication type by group
     * @param fieldName
     * @param group
     * @return
     */
    public static List<String> getAuthenticationTypeByGroup(String fieldName,Group group){
        List<String> result = new ArrayList<>();
        String authenticationTypeProperty = getConfProperty("authentication.mode");
        if (authenticationTypeProperty == null || authenticationTypeProperty.isEmpty()){
            String authenticationType = ConfigurationService.getAsString(group, fieldName);
            if (authenticationType != null && !authenticationType.isEmpty()) {
                result.add(authenticationType);
            } else {
                logger.error("Initial authentication configuration has not been defined");
                throw new UserException("Service unavailable, please try again later.");
            }
        }else{
                result.add(authenticationTypeProperty);
        }

        return result;
    }

    /**
     * get connection properties from a defined connection for the tenant group
     * @param group
     * @param connectionName
     * @return
     */
    public static Map<String,String> getConnectionProperties(Group group, String connectionName){
        String connectionCode = ConfigurationService.getAsString(group, connectionName);
        if (connectionCode == null || connectionCode.isEmpty()){
            logger.error("[" + connectionName + "]" + " connection has not been defined for group " + group.getCode());
            throw new UserException("Service unavailable, please try again later.");
        }
        Connection connection = ConnectionService.getInstance().getByCode(connectionCode);
        if (connection == null){
            logger.error("[" + connectionCode + "]" + " connection does not exist");
            throw new UserException("Service unavailable, please try again later.");
        }
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> mapResponse = null;
        try {
            switch (connectionName){
                case "oauth2Connection":
                    mapResponse = mapper.readValue( connection.getProperties().toString(), Map.class );
                    mapResponse.put("clientName", connectionName);
                    mapResponse.put("tokenName", "access_token");
                    break;
                case PROPERTY_LDAP_AD_CONNECTION:
                    mapResponse = mapper.readValue( connection.getProperties().toString(), Map.class );
                    Field ldapField = FieldService.getInstance().selectByName("ldapUserIdentifier");
                    GroupField groupField = GroupFieldService.getInstance().selectByGroupField(connection.getGroup(),ldapField);
                    mapResponse.put("password",connection.getPassword(false));
                    if (groupField != null) {
                        mapResponse.put("typeAuthentication", groupField.getValue());
                    }
                    break;
            }
        } catch( IOException e ) {
            throw new UserException("Connection is not defined correctly", e);
        }
        return mapResponse;
    }
//
//    /**
//     * it creates a user if the authentication is different from VIZIX_AUTHENTICATION and user does not exist
//     * @param user
//     * @param group
//     * @param authenticationMode
//     * @param userName
//     * @return
//     */
//    public static User verifyCreateUser(User user, Group group,String authenticationMode,String userName){
//        if (user == null){
//            // get auto-create property
//            if (ConfigurationService.getAsBoolean(group, PROPERTY_AUTO_CREATE_USER)){
//                // get default role
//                Long roleId = ConfigurationService.getAsLong(group, PROPERTY_USER_ROLE);
//
//                if(!authenticationMode.equals(NATIVE_AUTHENTICATION)){
//                    // create user using group and role defined
//                    User temporalUser = new User(userName);
//                    temporalUser.setGroup(group);
//                    user=UserService.getInstance().insertLoginTime(temporalUser);
//                    Role role = RoleService.getInstance().get(roleId);
//
//                    UserRole userRole = new UserRole();
//                    userRole.setUser(user);
//                    userRole.setRole(role);
//                    userRole = UserRoleService.getInstance().insert(userRole);
//                    if (userRole == null) {
//                        throw new UserException(String.format("Name [%s] already exists", user.getUserName()));
//                    }
//                }
//
//            } else {
//                throw new UserException("User does not exist");
//            }
//        }
//        return user;
//    }

    public static String getConfProperty(String propertyName) {
        if (System.getProperty(propertyName) != null) {
            return System.getProperty(propertyName);
        }
        return Configuration.getProperty(propertyName);
    }

}
