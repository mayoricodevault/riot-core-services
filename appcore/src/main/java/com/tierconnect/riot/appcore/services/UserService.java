package com.tierconnect.riot.appcore.services;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.mysema.query.types.expr.BooleanExpression;
import com.tierconnect.riot.appcore.dao.TokenDAO;
import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.utils.AuthenticationUtils;
import com.tierconnect.riot.appcore.utils.Utilities;
import com.tierconnect.riot.appcore.utils.VisibilityUtils;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.commons.DateFormatAndTimeZone;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.servlet.exception.ForbiddenException;
import com.tierconnect.riot.sdk.utils.BeanUtils;
import com.tierconnect.riot.sdk.utils.HashUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.DefaultAccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.common.AuthenticationScheme;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.tierconnect.riot.appcore.utils.AuthenticationUtils.AUTHENTICATION_MODE;
import static com.tierconnect.riot.appcore.utils.Utilities.*;

public class UserService extends UserServiceBase {

    static Logger logger = Logger.getLogger(UserService.class);

    private static final QUser qUser = QUser.user;

    private String[] week_days = new String[]{"monday","tuesday","wednesday","thursday","friday","saturday","sunday"};

    public User getRootUser()
    {
    	return getInstance().get( 1L );
    }

    public User insert(User user) {
        if (existsUserByUsernameGroup(user.getUsername(), user.getActiveGroup())) {
            //return null;
        	throw new UserException( "duplicate username: '" + user.getUsername() + "'" );
        }
        if (AuthenticationUtils.NATIVE_AUTHENTICATION.equals(ConfigurationService.getAsString(user.getActiveGroup(), AUTHENTICATION_MODE)) ){
            if (StringUtils.isBlank(user.getPassword())) {
                throw new UserException("password is required.");
            }
        }
        if (AuthenticationUtils.LDAP_AD_AUTHENTICATION.equals(ConfigurationService.getAsString(user.getActiveGroup(), AUTHENTICATION_MODE))
                || AuthenticationUtils.OAUTH2_AUTHENTICATION.equals(ConfigurationService.getAsString(user.getActiveGroup(), AUTHENTICATION_MODE))){
            String password = "p455w0rd" + user.getUsername();
            user.setPassword(password);
        }

        String password = user.getPassword();

        user.setApiKey(generateApiKeySimple());
        // validate if user exists for another tenant, password must be different
        List<User> users = selectUsersByUsername(user.getUsername());
        if (users != null && !users.isEmpty()){
            for (User usr : users){
                if (user.getHashedPassword() != null && usr.getHashedPassword() != null && user.getHashedPassword().equals(usr.getHashedPassword())){
                    throw new UserException("Invalid password.");
                }
            }
        }
//        if (StringUtils.isEmpty(user.getApiKey())) {
//            if (user.getGroup().getParent() == null) {
//                user.setApiKey("root");
//            } else {
//                user.setApiKey(HashUtils.hashSHA256(user.getHashedPassword()+ UUID.randomUUID()));
//            }
//        }
        user.setPassword(null);
        Long id = getUserDAO().insert(user);
        user.setId(id);

        UserPassword userPassword = new UserPassword();
        userPassword.setUser(user);
        userPassword.setStatus(Constants.PASSWORD_STATUS_PENDING);
        userPassword.setHashedPassword(HashUtils.hashSHA256(password));
        UserPasswordService.getInstance().insert(userPassword);

        return user;
    }

    public User update(User user) {
        if (verifyUserByUsernameGroup(user.getUsername(), user.getActiveGroup())) {
            return null;
        }
        if (StringUtils.isNotBlank(user.getPassword())) {
            UserPassword userPassword = new UserPassword();
            userPassword.setUser(user);
            userPassword.setStatus(Constants.PASSWORD_STATUS_ACTIVE);
            userPassword.setHashedPassword(HashUtils.hashSHA256(user.getPassword()));
            UserPasswordService.getInstance().insert(userPassword);
        }
        // validate if user exists for another tenant, password must be different
        List<User> users = selectUsersByUsername(user.getUsername());
        if (users != null && !users.isEmpty()){
            int count = 0;
            for (User usr : users){
                if (user.getHashedPassword() != null && usr.getHashedPassword() != null && user.getHashedPassword().equals(usr.getHashedPassword())){
                    count++;
                }
            }
            if (count > 1){
                return null;
            }
        }
        user.setPassword(null);
        getUserDAO().update(user);
        updateFavorite(user);
        return user;
    }

    public User updateTimeZoneOfUser(Long userId, String timeZone) {
        User user = UserService.getInstance().get(userId);
        if (user == null) {
            throw new UserException("User ID: [" + userId + "] not found.");
        }
        logger.info("Update timeZone user ID [" + userId + "], username [" + user.getUsername() +
                "] of [" + user.getTimeZone() + "] to [" + timeZone + "]");
        user.setTimeZone(timeZone);
        return UserService.getInstance().update(user);
    }

    /**
     *
     * @param userId User ID
     * @param userMap User Map
     * @param validateVisibility boolean validate visibility true | false
     * @return Map with user updated, if an error is detected return map with "error" key
     */
    public Map<String, Object> updateUser(Long userId, Map<String, Object> userMap, boolean validateVisibility) {
        Map<String, Object> response = new HashMap<>();
        String oldPassword;
        User user = UserService.getInstance().get(userId);
        if(user == null){
            throw new UserException("User ID: [" + userId + "] not found.");
        }
        oldPassword=user.getPassword();
        if (validateVisibility) {
            Group visibilityGroup = VisibilityUtils.getVisibilityGroup(User.class.getCanonicalName(), null);
            if (GroupService.getInstance().isGroupNotInsideTree(user.getActiveGroup(), visibilityGroup)) {
                throw new ForbiddenException("Forbidden User");
            }
        }
        if(userMap.containsKey("roamingGroup.id")) {
            Number number = (Number) userMap.get("roamingGroup.id");
            if (number == null) {
                user.setRoamingGroup(null);
            } else {
                Group roamingGroup = GroupService.getInstance().get(number.longValue());
                if (roamingGroup == null) {
                    throw new UserException("Invalid Roaming Group");
                }
                user.setRoamingGroup(roamingGroup);
            }
        }
        // validate email
        EmailValidator validator = EmailValidator.getInstance();
        if (userMap.get("email") != null && userMap.get("email") instanceof String && !((String) userMap.get("email")).isEmpty()
                && !validator.isValid((String)userMap.get("email"))) {
            throw new UserException("Invalid E-mail Address for username: " + userMap.get("username"));
        }

        if(userMap.containsKey("username")){
            user.setUsername((String) userMap.get("username"));
        }
        if(userMap.containsKey("firstName")){
            user.setFirstName((String)userMap.get("firstName"));
        }
        if(userMap.containsKey("lastName")){
            user.setLastName((String)userMap.get("lastName"));
        }
        if(userMap.containsKey("email")){
            user.setEmail((String)userMap.get("email"));
        }
        String changedByMessage = null;
        if(userMap.containsKey("password") && !StringUtils.isBlank((String)userMap.get("password"))){
            Utilities.validatePassword(user.getActiveGroup(), user, (String)userMap.get("password"));
            user.setPassword((String)userMap.get("password"));
            User currentUser = (User) SecurityUtils.getSubject().getPrincipal();
            if (user.getId().equals(currentUser.getId())) {
                changedByMessage = "Password Policy Auditing: User '" + user.getUsername()
                    + "' changed password himself in Edit User form.";
            } else {
                changedByMessage = "Password Policy Auditing: User '" + currentUser.getUsername() + "' changed password of '"
                    + user.getUsername() + "' in Edit User form.";
            }
        }
        if (userMap.containsKey("timeZone")) {
            user.setTimeZone((String) userMap.get("timeZone"));
        }
        if (userMap.containsKey("dateFormat")) {
            user.setDateFormat(null);
            if (!isEmptyOrNull((String) userMap.get("dateFormat"))) {
                user.setDateFormat((String) userMap.get("dateFormat"));
            }
        }
        validateRegionalSetting(user);
        if(userMap.containsKey("apiKey")){
            if(userMap.get("apiKey").toString().isEmpty()){
                throw new UserException("API Key can't be empty");
            }
            if(!StringUtils.isAlphanumeric(userMap.get("apiKey").toString())){
                throw new UserException("Invalid API Key. Only alphanumeric characters allowed");
            }

            if(UserService.apiKeyTaken(userMap.get("username").toString(),userMap.get("apiKey").toString())){
                throw new UserException("This API Key is already in use.");
            }
            user.setApiKey((String)userMap.get("apiKey"));
        }
        if(userMap.containsKey("group.id")){
            Group group = GroupService.getInstance().get(((Number)userMap.get("group.id")).longValue());
            if(group == null){
                throw new UserException("Invalid Group");
            }
            user.setGroup(group);
        }
        if(userMap.containsKey("hiddenTabs")){
            user.setHiddenTabs((String)userMap.get("hiddenTabs"));
        }

        // validate user
        UserService.getInstance().validateUpdate(user,user.getActiveGroup());

        Map<String,Object> shifts = UserService.getInstance().getShifts(userMap);

        BeanUtils.setProperties(shifts, user);

        List<String> authenticationTypes = AuthenticationUtils.getAuthenticationTypeByGroup(AUTHENTICATION_MODE,user.getGroup());
        User userUpdated=null;
        RecentService.getInstance().updateName(user.getId(), user.getUsername(),"user");

        if(authenticationTypes != null && authenticationTypes.contains(AuthenticationUtils.LDAP_AD_AUTHENTICATION)
                && oldPassword == null && userMap.containsKey("password") && userMap.get("password") != null
                && !((String)userMap.get("password")).isEmpty()){
            logger.debug("size of authenticationTypes "+ authenticationTypes.size() +" "+authenticationTypes.get(0));
            response.put("error", true);
            response.put("message", String.format("[%s] is not a ViZix user, you cannot change this user password ", user.getUsername()));
            return response;
        }else {
            userUpdated = UserService.getInstance().update(user);
        }
        if (userUpdated == null) {
            response.put("error", true);
            response.put("message", String.format("Name [%s] already exists", user.getUsername()));
        } else {
            if (userMap.containsKey("forcePasswordChange") && userMap.get("forcePasswordChange") != null) {
                if (Boolean.parseBoolean(userMap.get("forcePasswordChange").toString())) {
                    UserPasswordService.getInstance().forcePasswordChange(user);
                } else {
                    UserPasswordService.getInstance().avoidPasswordChange(user);
                }
            }
            response.put("userUpdated", userUpdated.publicMap());
            if (changedByMessage != null) {
                logger.info(changedByMessage);
            }
        }
        return response;
    }

    private void validateRegionalSetting(User user) {
        if (!isEmptyOrNull(user.getDateFormat())
                && !isAlphaNumericCharacterSpecials(user.getDateFormat(), GroupService.CHARACTER_SPECIALS_DATE_FORMAT)) {
            throw new UserException("Date format has invalid characters, only alphanumeric and character _-&/.:,\"[]()\\  are allowed.");
        }
        if (!isEmptyOrNull(user.getTimeZone()) && !timeZoneIsValid(user.getTimeZone())) {
            throw new UserException("Invalid timezone");
        }
    }

    /**
     * method to authenticate a user through an Authentication Strategy
     * @param username
     * @param password
     * @param request
     * @return
     */
    public static User authenticateUser(String username, String password, HttpServletRequest request){
        User user = null;
        Group group = null;
        List<User> users = new ArrayList<>();
        if (username != null){
            users = selectUsersByUsername(username);
        }
        if (users.size() > 1){
            throw new UserException("There is more than one user with the same username.");
        }
        if (users != null && !users.isEmpty()){
            user = users.get(0);
            group = user.getActiveGroup();
        }
        // execute authenticationStrategy
        AuthenticationUtils authenticationUtils = new AuthenticationUtils();
        user = authenticationUtils.executeAuthenticationStrategy(username, password, user, group, request);
        if (user == null){
            throw new UserException("Username and/or password incorrect.");
        }
        // This option could be enabled if we need create a user that does not exist in our database but it exits in an
        // external authentication server like Active Directory
        // user = authenticationUtils.verifyCreateUser(user,group,authenticationMode,username);
        return user;
    }

    public static Map authenticationMode(){
        Map<String, String> mapResponse = new HashMap<>();
        Group group = GroupService.getInstance().getRootGroup();
        List<String> authenticationTypes = new ArrayList<>();
            authenticationTypes = AuthenticationUtils.getAuthenticationTypeByGroup(AUTHENTICATION_MODE,group);
            for (String type: authenticationTypes){
                mapResponse.put("authenticationMode",type);
                if (type.equals(AuthenticationUtils.OAUTH2_AUTHENTICATION)) {
                    Map<String, String> temp = AuthenticationUtils.getConnectionProperties(group,"oauth2Connection");
                    mapResponse.put("provider", temp.get("provider"));
                    String clientId = temp.get("clientId");
                    mapResponse.put("clientId",clientId);
                    mapResponse.put("redirectUri",temp.get("redirectUri"));
                    mapResponse.put("accessTokenUri",temp.get("accessTokenUri"));
                    mapResponse.put("userInfoUri",temp.get("userInfoUri"));
                    mapResponse.put("userAuthUri",temp.get("userAuthUri"));
                    mapResponse.put("loginMessage", ConfigurationService.getAsString(group,"oauth2LoginMessage"));
                    mapResponse.put("userIdentifier",temp.get("userIdentifier"));
                    mapResponse.put("grantType",temp.get("grantType"));
                    mapResponse.put("accessTokenMethod",temp.get("accessTokenMethod"));
                    if (temp.get("scope") != null) {
                        mapResponse.put("scope", temp.get("scope"));
                    }
                }
            }
        return mapResponse;
    }

    public static Map accessToken(String code){
        Map mapResponse = new HashMap<>();
        Group group = GroupService.getInstance().getRootGroup();
        List<String> authenticationTypes = new ArrayList<>();
        authenticationTypes = AuthenticationUtils.getAuthenticationTypeByGroup(AUTHENTICATION_MODE,group);
        if (authenticationTypes.contains(AuthenticationUtils.OAUTH2_AUTHENTICATION)){
            try {
                AuthorizationCodeResourceDetails clientResources = new AuthorizationCodeResourceDetails();
                Map<String, String> temp = AuthenticationUtils.getConnectionProperties(group, "oauth2Connection");
                clientResources.setClientId(temp.get("clientId"));
                clientResources.setClientSecret(temp.get("clientSecret"));
                clientResources.setAccessTokenUri(temp.get("accessTokenUri"));
                clientResources.setGrantType("authorization_code");
                clientResources.setAuthenticationScheme(AuthenticationScheme.query);
                clientResources.setClientAuthenticationScheme(AuthenticationScheme.form);
                clientResources.setPreEstablishedRedirectUri(temp.get("redirectUri"));
                clientResources.setTokenName("access_token");

                AccessTokenRequest atr = new DefaultAccessTokenRequest();
                atr.setPreservedState(temp.get("redirectUri"));
                atr.setAuthorizationCode(code);
                OAuth2RestOperations restTemplate = new OAuth2RestTemplate(clientResources, new DefaultOAuth2ClientContext(atr));
                OAuth2AccessToken oAuth2AccessToken = restTemplate.getAccessToken();
                mapResponse.put("accessToken", oAuth2AccessToken.getValue());
                mapResponse.put("expirationDate", oAuth2AccessToken.getExpiration() != null ? oAuth2AccessToken.getExpiration().getTime() : 0);
            }catch (Exception e){
                throw new UserException(e.getMessage());
            }
        }else{
            throw new UserException("OAuth2Authentication is not configured");
        }
        return mapResponse;
    }

    public static boolean existsUserByUsername(String name) {
        return existsUserByUsername(name, null);
    }

    public static boolean existsUserByUsername(String name, Long excludeId) {
        BooleanExpression predicate = qUser.username.eq(name);

        if (excludeId != null) {
            predicate = predicate.and(qUser.id.ne(excludeId));
        }

        return getUserDAO().getQuery().where(predicate).exists();
    }

    public static boolean existsUserByUsernameGroup(String name, Group group) {
        BooleanExpression predicate = qUser.username.eq(name);

        if (group != null) {
            predicate = predicate.and(qUser.group.eq(group));
        }
        return getUserDAO().getQuery().where(predicate).exists();
    }
    public static boolean verifyUserByUsernameGroup(String name, Group group) {
        BooleanExpression predicate = qUser.username.eq(name);

        if (group != null) {
            predicate = predicate.and(qUser.group.eq(group));
        }
        return getUserDAO().getQuery().where(predicate).count() > 1;
    }

	public static User selectByUsername(String username) {
        return getUserDAO().getQuery().where(qUser.username.eq(username)).uniqueResult(qUser);
	}

    public static User selectByUsernameGroup(String username, Long groupId) {
        return getUserDAO().getQuery().where(qUser.username.eq(username).and(qUser.group.id.eq(groupId))).uniqueResult(qUser);
    }

    public static List<User> selectUsersByUsername(String username) {
        return getUserDAO().getQuery().where(qUser.username.eq(username)).list(qUser);
    }

    public Long countAllOpenSessions() {
        TokenService tokenService = getTokenService();
        TokenDAO tokenDAO = tokenService.getTokenDAO();
        QToken qToken = QToken.token;
        return tokenDAO.countAll(qToken.tokenExpirationTime.after(new Date()));
    }

    public List<Token> getAllOpenSessionsUser(User user) {
        TokenService tokenService = getTokenService();
        TokenDAO tokenDAO = tokenService.getTokenDAO();
        QToken qToken = QToken.token;
        return tokenDAO.selectAllBy(qToken.user.eq(user).and(qToken.tokenExpirationTime.after(new Date())));
    }

    public Long countAllOpenSessions(Group tenantGroup) {
        TokenService tokenService = getTokenService();
        TokenDAO tokenDAO = tokenService.getTokenDAO();
        QToken qToken = QToken.token;
        QUser qUser = QUser.user;
        return new HibernateQuery(tokenDAO.getSession()).from(qToken).innerJoin(qToken.user, qUser).where(qToken.tokenExpirationTime.after(new Date())
                        .and(qUser.group.parentLevel2.id.eq(tenantGroup.getParentLevel2().getId()))).count();
    }

    public List<Token> getAllOpenSessionsUser(Group tenantGroup, User user) {
        TokenService tokenService = getTokenService();
        TokenDAO tokenDAO = tokenService.getTokenDAO();
        QToken qToken = QToken.token;
        QUser qUser = QUser.user;
        return new HibernateQuery(tokenDAO.getSession()).from(qToken).innerJoin(qToken.user, qUser).where(qToken.tokenExpirationTime.after(new Date())
                .and(qUser.group.parentLevel2.id.eq(tenantGroup.getParentLevel2().getId()).and(qToken.user.eq(user)))).list(qToken);
    }

    public Long countAllActive() {
        return getUserDAO().countAll(qUser.archived.isFalse());
    }

    public Long countAllActive(Group tenantGroup) {
        return getUserDAO().countAll(qUser.archived.isFalse().and(qUser.group.parentLevel2.id.eq(tenantGroup.getId())));
    }

    public static boolean apiKeyTaken(String username, String apiKey) {
        BooleanExpression predicate = qUser.apiKey.eq(apiKey);
        predicate = predicate.and(qUser.username.ne(username));
        return getUserDAO().getQuery().where(predicate).exists();
    }

    private TokenService getTokenService() {
        return TokenService.getInstance();
    }

    /***************************************
     * Create a new User
     **************************************/
    public User createUser(Map<String, Object> userMap)
    {
        Group group = GroupService.getInstance().get(((Number)userMap.get("group.id")).longValue());
        if(group == null){
            throw new UserException("Invalid Group");
        }

        User user = new User();
        String username = (String)userMap.get("username");
        if (username.isEmpty()) {
            throw new UserException("'Username' is required.");
        }
        // validate email
        EmailValidator validator = EmailValidator.getInstance();
        if (userMap.get("email") != null && userMap.get("email") instanceof String && !((String) userMap.get("email")).isEmpty()
                && !validator.isValid((String)userMap.get("email"))) {
            throw new UserException("Invalid E-mail Address for username: " + userMap.get("username"));
        }
        user.setUsername(username);
        user.setPassword((String) userMap.get("password"));
        user.setFirstName((String)userMap.get("firstName"));
        user.setLastName((String)userMap.get("lastName"));
        user.setEmail((String)userMap.get("email"));
        user.setTimeZone((String)userMap.get("timeZone"));
        user.setDateFormat((String)userMap.get("dateFormat"));
        user.setGroup(group);
        Utilities.validatePassword(group, user, (String) userMap.get("password"));
        validateRegionalSetting(user);
        if (userMap.containsKey("roamingGroup.id")) {
            Number roamingGroupNumber = (Number) userMap.get("roamingGroup.id");
            if (roamingGroupNumber != null) {
                Group roamingGroup = GroupService.getInstance().get(roamingGroupNumber.longValue());
                if (roamingGroup == null) {
                    throw new UserException("Invalid Group");
                }
                user.setRoamingGroup(roamingGroup);
            }
        }
        user.setUsername(replaceCharacterEscape(user.getUsername()));
        if (!isAlphaNumericCharacterSpecials(user.getUsername(), "@_\\-\\\\.")) {
            throw new UserException("Username has invalid characters, only alphanumeric and character [. _ - \\ @] are allowed.");
        }

        Map<String,Object> shifts = getShifts(userMap);

        BeanUtils.setProperties(shifts, user);

        validateInsert(user, group);

        try {
            user = UserService.getInstance().insert(user);
            if (userMap.containsKey("forcePasswordChange") && userMap.get("forcePasswordChange") != null
                && !Boolean.parseBoolean(userMap.get("forcePasswordChange").toString())) {
                UserPasswordService.getInstance().avoidPasswordChange(user);
            }
        } catch ( UserException ue ) {
            //return RestUtils.sendBadResponse(String.format("Username [%s] already exists", username));
            throw new UserException(String.format("Username [%s] already exists", username), ue);
        }
        return user;
    }

    private void validateInsert(User user, Group group ){
        //Required files
        AuthenticationUtils authenticationUtils = new AuthenticationUtils();
        authenticationUtils.validateUserExists(user,group,"create");
        validateInsert(user);
    }

    /**
     * Validate Insert
     * @param user to validate
     */
    public void validateInsert(User user) {
        GroupService groupService = GroupService.getInstance();
        UserService userService = UserService.getInstance();

        if (LicenseService.enableLicense) {
            LicenseDetail licenseDetail = LicenseService.getInstance().getLicenseDetail(user.getActiveGroup(), true);
            Group licenseGroup = groupService.get(licenseDetail.getGroupId());
            boolean isRootLicense = groupService.getRootGroup().getId().equals(licenseGroup.getId());
            Long maxNumberOfUsers = licenseDetail.getMaxNumberOfUsers();
            if (maxNumberOfUsers != null && maxNumberOfUsers > 0) {
                Long countAll;
                if (isRootLicense) {
                    countAll = userService.countAllActive();
                } else {
                    countAll = userService.countAllActive(licenseGroup);
                }
                if (countAll >= maxNumberOfUsers) {
                    throw new UserException("You cannot insert more users because you reach the limit of your license");
                }
            }
        }
    }

    @Deprecated
    private String getApiKey(String username, String password, boolean isHashedPassword, String domain){


        String hashedUsername = new StringBuffer(HashUtils.hashSHA256(username)).reverse().toString();
        String hashedDomain = new StringBuffer(HashUtils.hashSHA256(domain)).reverse().toString();
        String hashedPassword = password;
        if(!isHashedPassword){
            hashedPassword = HashUtils.hashSHA256(password);
        }
        hashedPassword = new StringBuffer(hashedPassword).reverse().toString();

        int ix = 0;

        StringBuilder sb = new StringBuilder();
        while (ix < hashedPassword.length()) {
            sb.append(hashedUsername.charAt(ix));
            sb.append(hashedPassword.charAt(ix));
            sb.append(hashedDomain.charAt(ix));
            ix++;
        }

        int primeNumber = 7;
        int shortHashNumber = primeNumber;
        for (int i = 0; i <  sb.toString().length(); i++) {
            shortHashNumber = shortHashNumber*31 + sb.toString().charAt(i);
        }

        String apiKey = Integer.toHexString(Math.abs(shortHashNumber)).toUpperCase();


        logger.debug("hashedUsername = " + hashedUsername);
        logger.debug("hashedPassword = " + hashedPassword);
        logger.debug("hashedDomain = " + hashedDomain);
        logger.debug("compound shortHashNumber = " + sb.toString());
        logger.debug("shortHashNumber = " + Math.abs(shortHashNumber));
        logger.debug("apiKey = " + apiKey);

        return apiKey;
    }

    private String generateApiKeySimple() {

        return RandomStringUtils.randomAlphanumeric(10).toUpperCase();

    }

    public Map<String,Object> getShifts(Map<String,Object> userMap){
        Map<String,Object> shifts = new HashMap<>();

        for(String day : week_days){

            if(userMap.get(day+"Start") == null && userMap.get(day+"End") == null){
                continue;
            } else if(userMap.get(day+"Start") == null || userMap.get(day+"End") == null){
                throw new UserException("You must define both Start and End");
            } else if((userMap.get(day+"Start") instanceof Integer) == false || (userMap.get(day+"End") instanceof Integer) == false){
                throw new UserException("Start and End must be Integers");
            }

            Short start = ((Number)userMap.get(day+"Start")).shortValue();
            Short end = ((Number)userMap.get(day+"End")).shortValue();

            //Check if the start and end minutes are in day range
            if(start < 0 || start >= 24*60 || end < 0 || end >= 24*60 || start > end){
                throw new UserException("Invalid range of time");
            }

            shifts.put(day+"Start",start);
            shifts.put(day+"End",end);
        }

        return shifts;
    }

    /**
     * @param roles Role list to add
     * @param user User to add roles
     * @return Not detected Role's names
     */
    public List<String> addRolesToUser(String roles, User user) {
        List<String> rolesNotDetected = new ArrayList<>();
        roles = roles.trim();
        if (!roles.isEmpty()) {
            String rolesList[] = roles.split(";");
            for (String roleStr:rolesList) {
                roleStr = roleStr.trim();
                List<Role> roles1;
                if (user.getActiveGroup().getName().equals("root")) {
                    roles1 = RoleService.getInstance().getByName(roleStr, null);
                } else {
                    roles1 = RoleService.getInstance().getByName(roleStr, user.getActiveGroup());
                }
                if (roles1.isEmpty()) {
                    List<Group> groupList = user.getActiveGroup().getAscendants();
                    for (Group group:groupList) {
                        if (!group.equals(GroupService.getInstance().getRootGroup())) {
                            roles1 = RoleService.getInstance().getByName(roleStr, group);
                        } else {
                            roles1 = RoleService.getInstance().getByName(roleStr, null);
                        }
                        if (!roles1.isEmpty()) {
                            break;
                        }
                    }
                }
                if (roles1.isEmpty()) {
                    rolesNotDetected.add(roleStr);
                } else {
                    for (Role role : roles1) {
                        addUserRole(user.getId(), role.getId(), user, role);
                    }
                }
            }
        }
        return rolesNotDetected;
    }

    /**************************************
     * This method add rol to user
     **************************************/
    public void addUserRole(Long userId, Long roleId, User user, Role role )
    {
        boolean roleExists = false;
        if(user.getUserRoles()!=null) {
            for (UserRole userRole : user.getUserRoles()) {
                if ((long) userRole.getRole().getId() == (long) roleId) {
                    roleExists = true;
                }
            }
        }

        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(User.class.getCanonicalName(), null);
        if (GroupService.getInstance().isGroupNotInsideTree(user.getActiveGroup(), visibilityGroup)) {
            throw new ForbiddenException("Forbidden User");
        }
        if (GroupService.getInstance().isGroupNotInsideTree(role.getGroup(), visibilityGroup)) {
            //If the role is a root role
            if (role.getGroup().getTreeLevel() == 1) {
                //trying to assign a root role to a user without having the root role and root visibility
                throw new ForbiddenException("Forbidden Role");
            } else {
                //As all the roles are stored at company level then just validate that the company is correct
                if (GroupService.getInstance().isGroupNotInsideTree(role.getGroup(), visibilityGroup.getParentLevel2())) {
                    throw new ForbiddenException("Forbidden Role");
                }
            }
        }

        if (roleExists) {
            throw new UserException(String.format("User [%s] is already assigned to the Role [%s]", user.getUsername(), role.getName()));
        } else {
            UserRole userRole = new UserRole();
            userRole.setUser(user);
            userRole.setRole(role);
            userRole = UserRoleService.getInstance().insert(userRole);
            if (userRole == null) {
                throw new UserException(String.format("Name [%s] already exists", user.getUsername()));
            }
        }
    }
    /**
     * This method change the password the user
     * */
    public Map<String, Object>  changeOldPassword(String currentPassword, String newPassword, String confirmPassword, Long userId)
    {
        Map<String, Object> response = new  HashMap<String, Object>();

        User user = this.get(userId);

        if (user == null || !HashUtils.hashSHA256(currentPassword).equals(user.getHashedPassword()))
        {
            throw new UserException("Incorrect password.");
        }
        if (!HashUtils.hashSHA256(newPassword).equals(HashUtils.hashSHA256(confirmPassword)))
        {
            throw new UserException("New password do not match.");
        }
        int changeType = 0;
        if (isFirstLogin(user)) {
            changeType = 1;
        } else if (forcePasswordChange(user)) {
            changeType = 2;
        }

        Utilities.validatePassword(user.getActiveGroup(), user, newPassword);
        UserPassword userPassword = new UserPassword();
        userPassword.setUser(user);
        userPassword.setStatus(Constants.PASSWORD_STATUS_ACTIVE);
        userPassword.setHashedPassword(HashUtils.hashSHA256(newPassword));
        UserPasswordService.getInstance().insert(userPassword);

        this.update(user);

        response.put("message", "Password Changed Successfully.");

        switch (changeType) {
            case 1:
                logger.info("Password Policy Auditing: User '" + user.getUsername()
                    + "' changed password on first login.");
                break;
            case 2:
                logger.info("Password Policy Auditing: User '" + user.getUsername()
                    + "' changed password on next login.");
                break;
            default:
                logger.info("Password Policy Auditing: User '" + user.getUsername()
                    + "' changed password himself in user settings.");
        }

        return response;
    }

    public boolean matchesPassword(User user, String password) {
        return HashUtils.hashSHA256(password).equals(user.getHashedPassword());
    }

    /**
     * Validate User for update
     * @param user
     * @param group
     */
    public void validateUpdate(User user, Group group){
        //Required files
        AuthenticationUtils authenticationUtils = new AuthenticationUtils();
        authenticationUtils.validateUserExists(user,group,"update");
    }

    /**
     *
     * @param userRoleList User Role List
     * @param roleListStr Role List separated by ";"
     * @return Role List to add
     */
    public String getRolesToAdd(List<UserRole> userRoleList, String roleListStr) {
        String[] roles = roleListStr.split(";");
        List<String> newRoleList = new ArrayList<>();
        for (String role:roles) {
            if (!role.trim().isEmpty()) {
                newRoleList.add(role.trim());
            }
        }
        List<String> roleList = new ArrayList<>();
        List<String> roleNames = new ArrayList<>();
        for (UserRole userRole:userRoleList) {
            Role role = userRole.getRole();
            roleNames.add(role.getName());
        }
        for (String role:newRoleList) {
            if (!roleNames.contains(role)) {
                roleList.add(role);
            }
        }
        return StringUtils.join(roleList, ";");
    }

    /**
     *
     * @param userRoleList User Role List
     * @param roleListStr Role List separated by ";"
     * @return Role List to add
     */
    public List<UserRole> getRolesToDelete(List<UserRole> userRoleList, String roleListStr) {
        String[] roles = roleListStr.split(";");
        List<String> newRoleList = new ArrayList<>();
        for (String role:roles) {
            if (!role.trim().isEmpty()) {
                newRoleList.add(role.trim());
            }
        }
        List<UserRole> roleList = new ArrayList<>();
        for (UserRole userRole:userRoleList) {
            Role role = userRole.getRole();
            if (!newRoleList.contains(role.getName())) {
                roleList.add(userRole);
                newRoleList.remove(role.getName());
            }
        }
        // roleListStr = StringUtils.join(newRoleList, ";");
        return roleList;
    }

    /**
     *
     * @param rolesToDelete Role List to delete
     */
    public void removeUserRoles(List<UserRole> rolesToDelete) {
        for (UserRole userRole:rolesToDelete) {
            UserRoleService.getInstance().delete(userRole);
        }
    }

    /**
     * If user does not have Time Zone configuration, then the group config should be returned
     * @param user
     * @return
     */
    public String getValueRegionalSettings(User user, String value){
        String response = null;
        if(value.equals(Constants.TIME_ZONE_CONFIG)) {
            response = user.getTimeZone();
        } else if(value.equals(Constants.DATE_FORMAT_CONFIG)) {
            response = user.getDateFormat();
        }
        if(response == null) {
            response = GroupFieldService.getInstance().getGroupField(user.getGroup(), value);
            if (response == null) {
                if(value.equals(Constants.TIME_ZONE_CONFIG)) {
                    response = Constants.DEFAULT_TIME_ZONE;
                } else if(value.equals(Constants.DATE_FORMAT_CONFIG)) {
                    response = Constants.DEFAULT_DATE_FORMAT;
                }
            }
        }
        logger.debug("REGIONAL SETTINGS: " + value + " = " + response);
        return response;
    }

    public DateFormatAndTimeZone getDateFormatAndTimeZone(User user) {
        return new DateFormatAndTimeZone(
                getValueRegionalSettings(user, Constants.TIME_ZONE_CONFIG),
                getValueRegionalSettings(user, Constants.DATE_FORMAT_CONFIG));
    }
    /**
     * Validate Settings of the User
     * @param userMap  Data to being modified
     * @return
     */
    public boolean validateSettingsUser(Map<String, Object> userMap) {
        boolean response = true;
        for (Map.Entry<String, Object> entry : userMap.entrySet()) {
            if(!entry.getKey().equals("timeZone") && !entry.getKey().equals("dateFormat") ) {
                response = false;
                break;
            }
        }
        return response;
    }

    /**
     * This method change user API Key
     * */
    public Map<String, Object>  regenerateApiKey(Long userId)
    {
        Map<String, Object> response = new  HashMap<String, Object>();
        User user = this.get(userId);
        String apiKey = generateApiKeySimple();
        user.setApiKey(apiKey);
        logger.info(String.format("[APIKey] Regenerate. User: %d | %s, APIKey: %s", userId, user.getUsername(), apiKey));
        this.update(user);
        response.put("apiKey", apiKey);
        return response;
    }

    public User getByUsername(String username, Group group) throws NonUniqueResultException {
        try {
            BooleanBuilder be = new BooleanBuilder();
            be = be.and(QUser.user.username.eq(username));
            be = be.and(QUser.user.group.eq(group));
            return getUserDAO().selectBy(be);
        }
        catch (org.hibernate.NonUniqueResultException e) {
            throw new NonUniqueResultException(e);
        }
    }

    public User getGroupUser(Group group) {
        List<User> users = getUserDAO().selectAllBy(QUser.user.group.eq(group));
        if (users.size() > 0){
            return users.get(0);
        }
        return getRootUser();
    }

    public boolean isFirstLogin(User user) {
        return (StringUtils.equals(user.getLastPasswordStatus(), Constants.PASSWORD_STATUS_PENDING)
            && user.getUserPasswords().size() == 1);
    }

    public boolean forcePasswordChange(User user) {
        return user.getLastPasswordStatus() != null
            && user.getLastPasswordStatus().equals(Constants.PASSWORD_STATUS_PENDING);
    }

    public boolean hasPasswordExpirationPolicy(User user) {
        for (UserRole userRole : user.getUserRoles()){
            for (RoleResource roleResource : userRole.getRole().getRoleResources()){
                if (roleResource.getResource().getName().equals("passwordExpirationPolicy")) {
                    return true;
                }
            }
        }
        return false;
    }

    public Map<String, Object> getPasswordExpirationTime(User user) {
        Map<String, Object> output = new HashMap<>();
        if (hasPasswordExpirationPolicy(user)) {
            int expirationDays = ConfigurationService.getAsInteger(user, "passwordExpirationNoticeDays");
            Long expirationTime = user.getLastPasswordChange()
                + TimeUnit.DAYS.toMillis(
                ConfigurationService.getAsLong(user, "passwordExpirationDays"));
            output.put("passwordExpirationTime", getDateFormatAndTimeZone(user).format(expirationTime));
            if (TimeUnit.MILLISECONDS.toDays(expirationTime - System.currentTimeMillis())
                <= expirationDays) {
                output.put("displayPasswordExpirationTime", true);
            } else {
                output.put("displayPasswordExpirationTime", false);
            }
        }
        return output;
    }

    public Map<String, Object> getPasswordPolicies(Group group) {
        Map<String, Object> policies = new HashMap<>();
        policies.put("passwordMinLength", ConfigurationService.getAsInteger(group, "passwordMinLength"));
        policies.put("passwordMaxLength", ConfigurationService.getAsInteger(group, "passwordMaxLength"));
        policies.put("passwordUppercaseRequired", ConfigurationService.getAsBoolean(group, "passwordUppercaseRequired"));
        policies.put("passwordNumberRequired", ConfigurationService.getAsBoolean(group, "passwordNumberRequired"));
        policies.put("passwordSpecialCharRequired", ConfigurationService.getAsBoolean(group, "passwordSpecialCharRequired"));
        policies.put("passwordConsecutiveChar", ConfigurationService.getAsInteger(group, "passwordConsecutiveChar"));
        policies.put("passwordReusePrevious", ConfigurationService.getAsInteger(group, "passwordReusePrevious"));
        policies.put("passwordStrength", ConfigurationService.getAsString(group, "passwordStrength"));
        policies.put("passwordUseReservedWords", ConfigurationService.getAsBoolean(group, "passwordUseReservedWords"));
        policies.put("passwordReservedWords", ConfigurationService.getAsString(group, "passwordReservedWords"));
        policies.put("passwordLoginChange", ConfigurationService.getAsBoolean(group, "passwordLoginChange"));
        policies.put("passwordExpirationDays", ConfigurationService.getAsInteger(group, "passwordExpirationDays"));
        policies.put("passwordExpirationNoticeDays", ConfigurationService.getAsInteger(group, "passwordExpirationNoticeDays"));
        policies.put("passwordFailedAttempts", ConfigurationService.getAsInteger(group, "passwordFailedAttempts"));
        policies.put("passwordFailedLockTime", ConfigurationService.getAsInteger(group, "passwordFailedLockTime"));
        return policies;
    }

    public void deleteUserReferences(User user){

        List<UserRole> userRoleList =  UserRoleService.getInstance().listByUser(user);
        for (UserRole userRole: userRoleList) {
            UserRoleService.getInstance().delete(userRole);
        }

        UserFieldService.getInstance().deleteByUser(user);

        UserPasswordService.getInstance().deleteByUser(user);

        RecentService.getInstance().deleteRecentByUser(user);

        RecentService.getInstance().deleteRecent(user.getId(),"user");

        TokenService.getInstance().deleteByUser(user);




    }


}
