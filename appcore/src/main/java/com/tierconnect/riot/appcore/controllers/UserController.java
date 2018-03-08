package com.tierconnect.riot.appcore.controllers;

import com.tierconnect.riot.appcore.annotations.RequiresSession;
import com.tierconnect.riot.appcore.dao.TokenDAO;
import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.services.*;
import com.tierconnect.riot.appcore.servlet.TokenCacheHelper;
import com.tierconnect.riot.appcore.utils.*;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.sdk.dao.NotFoundException;
import com.tierconnect.riot.sdk.dao.Pagination;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.servlet.exception.ForbiddenException;
import com.tierconnect.riot.sdk.servlet.security.ApiKeyToken;
import com.tierconnect.riot.sdk.utils.HashUtils;
import com.tierconnect.riot.sdk.utils.PermissionsUtils;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.wordnik.swagger.annotations.*;
import com.wordnik.swagger.jaxrs.PATCH;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.annotation.*;
import org.apache.shiro.subject.Subject;
import org.jose4j.json.internal.json_simple.JSONObject;


import javax.annotation.security.PermitAll;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.tierconnect.riot.sdk.dao.QueryDSLUtils.getPath;

@Path("/user")
@Api("/user")
public class UserController extends UserControllerBase {
    static Logger logger = Logger.getLogger(UserController.class);

    private String[] IGNORE_USER_FIELDS = new String[]{"userRoles", "userFields"};

    public static Map<String, Object> toSmallMap(Group group) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", group.getId());
        map.put("name", group.getName());
        map.put("treeLevel", group.getTreeLevel());
        Map<String, Object> map1 = new HashMap<>();
        map1.put("id", group.getGroupType().getId());
        map1.put("name", group.getGroupType().getName());
        map.put("type", map1);
        return map;
    }

    @PUT
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value = {"user:i"})
    @ApiOperation(value = "Create User")
    public Response insertUser(Map<String, Object> userMap, @QueryParam("createRecent") @DefaultValue("false") Boolean createRecent) {
        EntityVisibility entityVisibility = getEntityVisibility();
        GeneralVisibilityUtils.limitVisibilityInsert(entityVisibility, VisibilityUtils.getObjectGroup(userMap));
        User user = UserService.getInstance().createUser(userMap);
        if (createRecent) {
            RecentService.getInstance().insertRecent(user.getId(), user.getUsername(), "user", user.getActiveGroup());
        }
        Map<String, Object> userResponseMap = (Map) user.publicMap();
        return RestUtils.sendCreatedResponse(userResponseMap);
    }

    public void validateInsert(User user) {
        UserService.getInstance().validateInsert(user);
    }

    @PATCH
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value = {"user:u:{id}", "user:r:{id}"}, logical = Logical.OR)
    @ApiOperation(value = "Update User")
    public Response updateUser(@PathParam("id") Long userId, Map<String, Object> userMap) {
        Map<String, Object> userUpdated = UserService.getInstance().updateUser(userId, userMap, true);
        if (userUpdated.containsKey("error")) {
            return RestUtils.sendBadResponse(userUpdated.get("message").toString());
        } else {
            Map<String, Object> userResponseMap = (Map) userUpdated.get("userUpdated");
            return RestUtils.sendOkResponse(userResponseMap);
        }
    }


    /**
     * This End Point user @PermitAll, then is without restrictions of authentication
     * It is used for modify just some fields of the User. Original Case: Timezone and DateFormat
     *
     * @param userId
     * @param userMap
     * @return
     */
    @PATCH
    @Path("/{id}/regionalSetting/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @PermitAll
    @ApiOperation(value = "Update User")
    public Response updateFieldsUser(
            @PathParam("id") Long userId,
            @ApiParam(value = "Parameters of User to change, format {key,value}") Map<String, Object> userMap) {

        if (userMap == null) {
            return RestUtils.sendBadResponse("You should enter the parameters of the User to change.");
        }
        if (!UserService.getInstance().validateSettingsUser(userMap)) {
            return RestUtils.sendBadResponse("You can only modify 'timezone' and 'dateFormat' fields of the user.");
        }
        Map<String, Object> userUpdated = UserService.getInstance().updateUser(userId, userMap, false);
        if (userUpdated.containsKey("error")) {
            return RestUtils.sendBadResponse(userUpdated.get("message").toString());
        } else {
            Map<String, Object> userResponseMap = (Map) userUpdated.get("userUpdated");
            userResponseMap.putAll(UserService.getInstance().getPasswordExpirationTime(UserService.getInstance().get(userId)));
            return RestUtils.sendOkResponse(userResponseMap);
        }
    }

    @POST
    @Path("/login")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @PermitAll
    @RequiresGuest
    @ApiOperation(
            value = "Auth user",
            notes = "Authentication of a user.<br>"
                    + "<font face=\"courier\">{\n" +
                    "<br> \"username\":\"valueusername\",\n" +
                    "<br> \"password\":\"valuepassword\"\n" +
                    "<br>}</font>")
    /**
     * It can receive
     * 1) username/password then logins and generates a token for the session
     * 2) apiKey then logins and generates a token for the session
     * 3) token then keeps session
     */
    public Response loginUser(Map<String, Object> loginMap, @Context HttpServletRequest request) {
        TokenService tokenService = TokenService.getInstance();
        TokenDAO tokenDAO = tokenService.getTokenDAO();
        QToken qToken = QToken.token;
        String username = (String) loginMap.get("username");
        String password = (String) loginMap.get("password");
        String apiKeyParam = (String) loginMap.get("apiKey");
        String tokenParam = (String) loginMap.get("token");
        User user;
        Token tokenObject = null;
        UserService userService = UserService.getInstance();

        if (StringUtils.isNotEmpty(apiKeyParam)) {
            user = userService.getUserDAO().selectBy(QUser.user.apiKey.eq(apiKeyParam));
            if (user == null) {
                throw new UserException("Invalid apiKey [" + apiKeyParam + "]");
            }
        } else if (StringUtils.isNotEmpty(tokenParam)) {
            tokenObject = tokenDAO.selectBy(qToken.tokenString.eq(tokenParam));
            if (tokenObject == null) {
                throw new UserException("Invalid Token [" + tokenParam + "]");
            }
            if (tokenObject.getTokenExpirationTime().before(new Date())) {
                throw new UserException("Invalid token [" + tokenParam + "], session has expired");
            }
            user = tokenObject.getUser();
        } else {
            try {
                // user authentication
                user = UserService.authenticateUser(username, password, request);
                if (username == null) {
                    username = user.getUsername();
                }
            } catch (UserException e) {
                if (e.getStatus() == 401) {
                    return RestUtils.sendJSONResponseWithCode(e.getMessage(), e.getStatus());
                } else if (e.getStatus() == 403) {
                    return RestUtils.sendJSONResponseWithCode(e.getMessage(), e.getStatus());
                } else if (e.getStatus() == 429) {
                    return RestUtils.sendResponseWithCode(e.getMessage(), e.getStatus());
                } else {
                    throw new UserException(e.getMessage());
                }
            }
        }
        if (user == null) {
            throw new UserException("User was not found ");
        }
        apiKeyParam = user.getApiKey();
        GroupService groupService = GroupService.getInstance();
        if (LicenseService.enableLicense) {
            LicenseDetail licenseDetail = LicenseService.getInstance().getLicenseDetail(user.getActiveGroup(), true);
            if (licenseDetail != null) {
                //Validate Concurrent Users
                Group licenseGroup = groupService.get(licenseDetail.getGroupId());
                boolean isRootLicense = groupService.getRootGroup().getId().equals(licenseGroup.getId());
                Long maxConcurrentUsers = licenseDetail.getMaxConcurrentUsers();
                if (StringUtils.isEmpty(tokenParam) && maxConcurrentUsers != null && maxConcurrentUsers > 0) {
                    Long countAll;
                    if (isRootLicense) {
                        countAll = userService.countAllOpenSessions();
                    } else {
                        countAll = userService.countAllOpenSessions(licenseGroup);
                    }
                    if (countAll >= maxConcurrentUsers) {
                        List<Token> list;
                        if (isRootLicense) {
                            list = userService.getAllOpenSessionsUser(user);
                        } else {
                            list = userService.getAllOpenSessionsUser(licenseGroup, user);
                        }
                        if (list.size() == 0) {
                            throw new UserException("You cannot login as the limit of concurrent users has been reached");
                        } else {
                            //logoff one session of the user
                            Token tokenObject2 = tokenDAO.selectBy(qToken.tokenString.eq(list.get(0).getTokenString()));
                            if (tokenObject2 != null) {
                                tokenObject2.setTokenExpirationTime(new Date());
                            }
                            TokenCacheHelper.invalidate(list.get(0).getTokenString());
                        }
                    }
                }
                //Validate client ip
                {
                    String clientIpLicense = licenseDetail.getClientIp();
                    if (StringUtils.isNotEmpty(clientIpLicense)) {
                        boolean matches = NetUtils.isAddressInSubnet(NetUtils.getClientIpAddress(request), clientIpLicense);
                        if (!matches) {
                            throw new UserException("You cannot login as your ip address is not authorized according to your license");
                        }
                    }
                }
                {
                    String serverIpLicense = licenseDetail.getServerIp();
                    if (StringUtils.isNotEmpty(serverIpLicense)) {
                        boolean matches = NetUtils.isAddressInSubnet(NetUtils.getServerIpAddress(request), serverIpLicense);
                        if (!matches) {
                            throw new UserException("You cannot login as the server ip address is not authorized according to your license");
                        }
                    }

                }
            }
        }

        String token = "";
        if (StringUtils.isNotEmpty(tokenParam)) {
            Long sessionTimeOutInMinutes = ConfigurationService.getAsLong(user, "sessionTimeout");
            token = tokenParam;
            tokenObject.setTokenExpirationTime(new Date(new Date().getTime() + sessionTimeOutInMinutes * 1000 * 60));
            TokenService.getInstance().update(tokenObject);
            TokenCacheHelper.invalidate(tokenParam);
        } else if (StringUtils.isNotEmpty(username)) {
            Long sessionTimeOutInMinutes = ConfigurationService.getAsLong(user, "sessionTimeout");
            token = HashUtils.hashSHA256(UUID.randomUUID().toString());
            tokenObject = new Token();
            tokenObject.setTokenString(token);
            tokenObject.setCreationTime(new Date());
            tokenObject.setTokenExpirationTime(new Date(new Date().getTime() + sessionTimeOutInMinutes * 1000 * 60));
            tokenObject.setUser(user);
            tokenObject.setTokenActive(true);
            TokenService.getInstance().insert(tokenObject);
        }
        ApiKeyToken apiKeyToken = new ApiKeyToken(user.getApiKey());
        Subject currentUser = SecurityUtils.getSubject();
        currentUser.login(apiKeyToken);
        Map<String, Object> publicMap = user.publicMap();
        publicMap.put("apiKey", apiKeyParam);
        publicMap.put("token", token);
        if (user.getGroup() != null) {
            publicMap.put("group", user.getGroup().publicMap(false));
            publicMap.put("groupType", user.getGroup().getGroupType().publicMap());
        }
        publicMap.put("activeGroup", user.getActiveGroup().publicMap(false));
        publicMap.put("activeGroupType", user.getActiveGroup().getGroupType().publicMap());
        if (user.getRoamingGroup() != null) {
            publicMap.put("roamingGroup", user.getRoamingGroup().publicMap());
            publicMap.put("roamingGroupType", user.getRoamingGroup().getGroupType().publicMap());
        }
        Map<String, Object> menuQueries = (Map<String, Object>) loginMap.get("menus");
        Map<String, Object> menus = evaluatePermissions(menuQueries);
        publicMap.put("menus", menus);

        Map<String, Object> visibilityGroup = new HashMap<>();
        visibilityGroup.put("user", toSmallMap(VisibilityUtils.getVisibilityGroup(User.class.getCanonicalName(), null)));
        visibilityGroup.put("group", toSmallMap(VisibilityUtils.getVisibilityGroup(Group.class.getCanonicalName(), null)));
        visibilityGroup.put("role", toSmallMap(VisibilityUtils.getVisibilityGroup(Role.class.getCanonicalName(), null)));
        visibilityGroup.put("groupType", toSmallMap(VisibilityUtils.getVisibilityGroup(GroupType.class.getCanonicalName(), null)));
        visibilityGroup.put("resource", toSmallMap(VisibilityUtils.getVisibilityGroup(Resource.class.getCanonicalName(), null)));
        visibilityGroup.put("field", toSmallMap(VisibilityUtils.getVisibilityGroup(Field.class.getCanonicalName(), null)));

        try {
            Map<String, Object> visibilityGroupIOT = (Map<String, Object>) Class.forName("com.tierconnect.riot.iot.controllers.IOTController").getMethod("calculateVisibilityGroups").invoke(null);
            visibilityGroup.putAll(visibilityGroupIOT);
        } catch (Exception e) {
            e.printStackTrace();
        }
        publicMap.put("visibilityGroups", visibilityGroup);

        //get company group for root
        if (user.getActiveGroup().getTreeLevel() == 1) {
            publicMap.put("companies", getChildrenMaps(user.getActiveGroup()));
        }
        //company group for company admin
        else if (user.getActiveGroup().getTreeLevel() == 2) {
            Map company = user.getActiveGroup().getParentLevel2().publicMap();
            company.put("type", user.getActiveGroup().getParentLevel2().getGroupType().publicMap());
            publicMap.put("companyGroup", company);
            publicMap.put("facilities", getChildrenMaps(user.getActiveGroup()));
        } else {
            Map company = user.getActiveGroup().getParentLevel2().publicMap();
            company.put("type", user.getActiveGroup().getParentLevel2().getGroupType().publicMap());
            publicMap.put("companyGroup", company);
        }

        Map<String, Object> userFields = UserFieldService.getInstance().listInheritedFieldsByUser(user, true);
        publicMap.put("configurations", userFields);

        List<String> notifications = new ArrayList<>();

        if (LicenseService.enableLicense) {
            Map<String, Object> license = new HashMap<>();
            boolean isLicenseDetailInheritance = LicenseService.getInstance().getLicenseDetailInheritance(user.getActiveGroup());
            LicenseDetail licenseDetail = LicenseService.getInstance().getLicenseDetail(user.getActiveGroup(), isLicenseDetailInheritance);
            publicMap.put("license", license);

            if (licenseDetail != null) {
                boolean expiredLicense = false;
                // verify license version
                if (!(LicenseService.getInstance().isValidLicenseVersion(licenseDetail))) {
                    expiredLicense = true;
                }
                // verify expiration date
                if (licenseDetail.getExpirationDate() != null) {
                    license.put("expirationDate", licenseDetail.getExpirationDate().getTime());
                }
                license.put("licenseType", licenseDetail.getLicenseType());
                if (licenseDetail.getExpirationDate() != null) {
                    long diffInMillis = licenseDetail.getExpirationDate().getTime() - (new Date()).getTime();
                    if (diffInMillis < 1000 * 60 * 60 * 24 * 5) {
                        TimeUnit timeUnit = TimeUnit.DAYS;
                        long expireTime = timeUnit.convert(diffInMillis, TimeUnit.MILLISECONDS);
                        if (expireTime > 1) {
                            notifications.add("The license is about to expire in " + expireTime + " days");
                        } else if (expireTime <= 1 && expireTime > 0) {
                            timeUnit = TimeUnit.HOURS;
                            expireTime = timeUnit.convert(diffInMillis, TimeUnit.MILLISECONDS);
                            notifications.add("The license is about to expire in " + expireTime + " hours");
                        } else {
                            expiredLicense = true;
                        }
                    }
                }
                if (expiredLicense) {
                    if (!PermissionsUtils.buildSearch(currentUser, new HashMap<>(), "license:r&(license:u|license:i)")) {
                        logger.info("Expired license. User do not have license resource");
                        throw new ForbiddenException("This ViZix license has expired. Please contact your application administrator.");
                    } else {
                        logger.info("Expired license, but user has license resource");
                        if (licenseDetail.getExpirationDate() != null && licenseDetail.getExpirationDate().before(new Date())) {
                            notifications.add("This ViZix license has expired. Please update your license.");
                        } else {
                            notifications.add("This ViZix license is obsolete. Please update your license.");
                        }
                    }
                }
            } else {
                if (!PermissionsUtils.buildSearch(currentUser, new HashMap<>(), "license:r&(license:u|license:i)")) {
                    logger.info("License is not installed. User do not have license resource");
                    throw new ForbiddenException("ViZix license is not installed. Please contact your application administrator.");
                } else {
                    logger.info("License is not installed, but user has license resource");
                    notifications.add("ViZix license is not installed. Please install your license.");
                }

            }

        }
        //Remove apiKey from response
        publicMap.remove("apiKey");
        publicMap.putAll(UserService.getInstance().getPasswordExpirationTime(user));
        publicMap.put("notifications", notifications);
        publicMap.put("kafkaEnabled", Configuration.getProperty("kafka.enabled"));
        publicMap.put("iconSetPath", Constants.TYPE_ICON_PATH);
        publicMap.put("iconSetPrefix", Constants.TYPE_ICON_PREFIX);
        return RestUtils.sendOkResponse(publicMap);
    }

    @GET
    @Path("/validateToken")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresSession
    @ApiOperation(
            value = "Auth user",
            notes = "Checks if the token is still valid")
    public Response validateToken() {
        return RestUtils.sendOkResponse("Has an active session");
    }

    @GET
    @Path("/authenticationMode")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    @RequiresGuest
    @ApiOperation(
            value = "Auth user",
            position = 5,
            notes = "Check which authentication type is created. Below is the full response for OAuth Authentication.<br>"
                    + "<font face=\"courier\">{ <br>&nbsp;&nbsp;\"accessTokenMethod\": \"get\", \n"
                    + "<br>&nbsp;&nbsp;\"accessTokenUri\": \"https://api.twitter.com/oauth/access_token\",\n"
                    + "<br>&nbsp;&nbsp;\"authenticationMode\": \"oauth2Authentication\",\n"
                    + "<br>&nbsp;&nbsp;\"clientId\": \"vLFzIvXV4vILMK216G952FSrT\", "
                    + "<br>&nbsp;&nbsp;\"loginMessage\":\"Log in with:\",\n"
                    + " \n"
                    + "<br>&nbsp;&nbsp;\"provider\":\"custom\",\n"
                    + "<br>&nbsp;&nbsp;\"redirectUri\": \"http://localhost:9000/\",\n"
                    + "<br>&nbsp;&nbsp;\"userAuthUri\": \"https://api.twitter.com/oauth/authenticate\",\n"
                    + "  <br>&nbsp;&nbsp;\"userIdentifier\": \"username\", "
                    + "  <br>&nbsp;&nbsp;\"grantType\": \"implicit\", "
                    + "<br>&nbsp;&nbsp;\"userInfoUri\":\"https://api.twitter.com/1.1/account/verify_credentials.json\"\n" +
                    "&nbsp;}</font>")
    public Response authenticationMode() {
        Map<String, String> response = UserService.authenticationMode();
        return RestUtils.sendOkResponse(response);
    }

    @GET
    @Path("/accessToken")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    @RequiresGuest
    @ApiOperation(
            value = "Auth user",
            position = 6,
            notes = "Returns access token for explicit OAuth 2.0 authentication. Below is the full response:<br>"
                    + "<font face=\"courier\">{ <br>&nbsp;&nbsp;\"accessToken\": \"12348954RTGSu\", \n"
                    + "<br>&nbsp;&nbsp;\"expirationDate\": \"1500408090\",\n"
                    + "<br>&nbsp;&nbsp;\"authenticationType\": \"oauth2Authentication\",\n"
                    + "<br>&nbsp;&nbsp;\"clientId\": \"vLFzIvXV4vILMK216G952FSrT\"\n" +
                    "&nbsp;}</font>")
    public Response accessToken(@QueryParam("code") String code) {
        if (code == null || code.isEmpty()) {
            return RestUtils.sendBadResponse("Authorization code cannot be null or empty.");
        }
        Map<String, String> response = UserService.accessToken(code);
        return RestUtils.sendOkResponse(response);
    }

    @POST
    @Path("/logout")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresUser
    @ApiOperation(value = "Auth User")
    public Response logoutUser(Map<String, Object> loginMap) {
        QToken qToken = QToken.token;
        Subject currentUser = SecurityUtils.getSubject();
        User user = ((User) currentUser.getPrincipal());
        String tokenParam = (String) loginMap.get("token");
        TokenService tokenService = TokenService.getInstance();
        TokenDAO tokenDAO = tokenService.getTokenDAO();
        if (StringUtils.isNotEmpty(tokenParam)) {
            Token tokenObject = tokenDAO.selectBy(qToken.tokenString.eq(tokenParam).and(qToken.user.eq(user).and(qToken.tokenExpirationTime.after(new Date()))));
            if (tokenObject != null) {
                tokenObject.setTokenExpirationTime(new Date());
                TokenCacheHelper.invalidate(tokenParam);
            }

        } else {
            List<Token> tokenObjects = tokenDAO.selectAllBy(qToken.user.eq(user));
            for (Token tokenObject : tokenObjects) {
                tokenObject.setTokenExpirationTime(new Date());
                TokenCacheHelper.invalidate(tokenObject.getTokenString());
            }
        }
        Map publicMap = new HashMap();
        return RestUtils.sendOkResponse(publicMap);
    }


    private List<Map<String, Object>> getChildrenMaps(Group group) {
        List<Map<String, Object>> maps = new ArrayList<>();
        //get root companies
        for (Group _group : GroupService.getInstance().findByParent(group)) {
            Map<String, Object> groupMap = _group.publicMap();
            groupMap.put("groupType", _group.getGroupType().publicMap());
            maps.add(groupMap);
        }
        return maps;
    }

    private Map<String, Object> evaluatePermissions(Map<String, Object> permissionQueries) {
        Map<String, Object> menus = new HashMap<>();
        Subject subject = SecurityUtils.getSubject();
        HashMap<String, Boolean> permissionCache = new HashMap<>();
        List<String> result = new ArrayList<>();
        try {
            result = (List<String>) Class.forName("com.tierconnect.riot.iot.controllers.IOTController").getMethod("calculateThingsPermissions").invoke(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (permissionQueries != null) {
            for (Map.Entry<String, Object> queryEntry : permissionQueries.entrySet()) {
                String query = (String) queryEntry.getValue();
                if (query.contains("_anything:r")) {
                    StringBuilder anyThing = new StringBuilder();
                    anyThing.append("(");
                    anyThing.append("false");
                    for (String s : result) {
                        anyThing.append("|").append(s).append(":r");
                    }
                    anyThing.append(")");
                    query = query.replace("_anything:r", anyThing.toString());
                }
                if (query.contains("_anything:i")) {
                    StringBuilder anyThing = new StringBuilder();
                    anyThing.append("(");
                    anyThing.append("false");
                    for (String s : result) {
                        anyThing.append("|").append(s).append(":i");
                    }
                    anyThing.append(")");
                    query = query.replace("_anything:i", anyThing.toString());
                }
                if (query.contains("_anything:u")) {
                    StringBuilder anyThing = new StringBuilder();
                    anyThing.append("(");
                    anyThing.append("false");
                    for (String s : result) {
                        anyThing.append("|").append(s).append(":u");
                    }
                    anyThing.append(")");
                    query = query.replace("_anything:u", anyThing.toString());
                }
                if (query.contains("_anything:d")) {
                    StringBuilder anyThing = new StringBuilder();
                    anyThing.append("(");
                    anyThing.append("false");
                    for (String s : result) {
                        anyThing.append("|").append(s).append(":d");
                    }
                    anyThing.append(")");
                    query = query.replace("_anything:d", anyThing.toString());
                }
                if (query.contains("_anything:a")) {
                    StringBuilder anyThing = new StringBuilder();
                    anyThing.append("(");
                    anyThing.append("false");
                    for (String s : result) {
                        anyThing.append("|").append(s).append(":a");
                    }
                    anyThing.append(")");
                    query = query.replace("_anything:a", anyThing.toString());
                }
                menus.put(queryEntry.getKey(), PermissionsUtils.buildSearch(subject, permissionCache, query));
            }
        }
        return menus;
    }

    private static Map<String, Object> detailedPublicMap(User user, Map<String, Object> publicMap) {
        List<Map<String, Object>> roleList = new LinkedList<>();

        for (UserRole userRole : UserRoleService.getInstance().listByUser(user)) {
            roleList.add(userRole.getRole().publicMap());
        }

        publicMap.put("roles", roleList);

        return publicMap;
    }

    @PUT
    @Path("/{user_id}/role/{role_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value = {"user:u:{user_id}", "user:i:{user_id}"}, logical = Logical.OR)
    @ApiOperation(value = "Assign Role to a User")
    public Response addUserRole(@PathParam("user_id") Long userId, @PathParam("role_id") Long roleId) {
        User user = UserService.getInstance().get(userId);
        Role role = RoleService.getInstance().get(roleId);

        UserService.getInstance().addUserRole(userId, roleId, user, role);
        return RestUtils.sendOkResponse(user.publicMap());
    }

    @DELETE
    @Path("/{user_id}/role/{role_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value = {"user:u:{user_id}"})
    @ApiOperation(value = "Remove Role from a User")
    public Response removeUserRole(@PathParam("user_id") Long userId, @PathParam("role_id") Long roleId) {
        User user = UserService.getInstance().get(userId);
        Role role = RoleService.getInstance().get(roleId);
        UserRole userRole = UserRoleService.getInstance().getByUserAndRole(user, role);

        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(User.class.getCanonicalName(), null);
        if (GroupService.getInstance().isGroupNotInsideTree(user.getActiveGroup(), visibilityGroup)) {
            throw new ForbiddenException("Forbidden User");
        }
        if (GroupService.getInstance().isGroupNotInsideTree(role.getGroup(), visibilityGroup)) {
            throw new ForbiddenException("Forbidden Role");
        }
        UserRoleService.getInstance().delete(userRole);
        return RestUtils.sendDeleteResponse();
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value = {"user:d:{id}"})
    // 1a. Limit access based on CLASS level resources
    @ApiOperation(position = 5, value = "Delete a User (AUTO)")
    @Override
    public Response deleteUser(@PathParam("id") Long id) {
        // 1c. TODO: Restrict access based on OBJECT level read permissions
        User user = UserService.getInstance().get(id);
        if (user == null) {
            return RestUtils.sendBadResponse(String.format("UserId[%d] not found", id));
        }
        // 2. Limit visibility based on user's group and the object's group (group based authorization)
        EntityVisibility entityVisibility = getEntityVisibility();
        GeneralVisibilityUtils.limitVisibilityDelete(entityVisibility, user);
        // handle validation in an Extensible manner
        validateDelete(user);


        User currentUser = (User) SecurityUtils.getSubject().getPrincipal();

        if (user.equals(currentUser)) {
            throw new UserException("Current user can not be deleted");
        }

        if (user.equals(UserService.getInstance().getRootUser())) {
            throw new UserException("User root can not be deleted");
        }

        JSONObject jsonObject = new JSONObject();
        String[] folders = getUserFolderNames(user);
        String[] reportsDefinition = getUserReportsNamesDefinition(user);

        String[] backgroundProcess = getBackGroundprocessInProgressByUser(user);
        ///********************
        ///deleteBackgroundProcessByUser(user);

        if (reportsDefinition.length == 0 && backgroundProcess.length == 0 && folders.length == 0) {
            try {
                UserService.getInstance().deleteUserReferences(user);

                deleteBackgroundProcessByUser(user);

                UserService.getInstance().delete(user);

                return RestUtils.sendDeleteResponse();
            } catch (Exception e) {
                return RestUtils.sendBadResponse(e.getMessage());
            }
        } else {

            jsonObject.put("folders", folders);
            jsonObject.put("reports", reportsDefinition);
            jsonObject.put("backgroundProcess", backgroundProcess);

            return RestUtils.sendJSONResponseWithCode(jsonObject.toJSONString(), 400);
        }
    }

    private String[] getUserFolderNames(User user) {
        String[] result = new String[10000];
        try {
            Class clazz = Class.forName("com.tierconnect.riot.iot.services.FolderService");
            Object ssInstance = clazz.getMethod("getInstance").invoke(null);
            result = (String[]) ssInstance.getClass().getMethod("getUserFolderNames", User.class).invoke(ssInstance, user);

        } catch (Exception e) {
            logger.info("User Folder Error", e);
        }
        return result;

    }

    private String[] getUserReportsNamesDefinition(User user) {
        String[] result = new String[10000];
        try {
            Class clazz = Class.forName("com.tierconnect.riot.iot.services.ReportDefinitionService");
            Object ssInstance = clazz.getMethod("getInstance").invoke(null);
            result = (String[]) ssInstance.getClass().getMethod("getReportsDefinitionNamesByUser", User.class).invoke(ssInstance, user);

        } catch (Exception e) {
            logger.info("User ReportsDefinition Error", e);
        }
        return result;
    }

    private String[] getBackGroundprocessInProgressByUser(User user) {
        String[] result = new String[10000];
        try {
            Class clazz = Class.forName("com.tierconnect.riot.iot.services.BackgroundProcessService");
            Object ssInstance = clazz.getMethod("getInstance").invoke(null);
            result = (String[]) ssInstance.getClass().getMethod("getBackGroundProcessInProgressByUser", User.class).invoke(ssInstance, user);

        } catch (Exception e) {
            logger.info("User BackgroundProcess Error", e);
        }
        return result;
    }

    private void deleteBackgroundProcessByUser(User user) {

        try {
            Class clazz = Class.forName("com.tierconnect.riot.iot.services.BackgroundProcessService");
            Object ssInstance = clazz.getMethod("getInstance").invoke(null);
            ssInstance.getClass().getMethod("deleteBackgroundProcessByUser", User.class).invoke(ssInstance, user);

        } catch (Exception e) {
            logger.info("User BackgroundProcess Error", e);
        }

    }

    @PUT
    @Path("/{id}/field/{field}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(value = "Update User Field")
    public Response setGroupField(@PathParam("id") Long userId, @PathParam("field") String fieldParam, Map<String, Object> params) {
        User currentUser = (User) SecurityUtils.getSubject().getPrincipal();
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(User.class.getCanonicalName(), null);
        User user = UserService.getInstance().get(userId);
        if (user == null) {
            throw new NotFoundException(String.format("User[%d] not found", userId));
        }
        Field field;
        try {
            field = FieldService.getInstance().get(Long.valueOf(fieldParam));
        } catch (Exception ex) {
            field = FieldService.getInstance().selectByName(fieldParam);
        }
        if (field == null) {
            throw new NotFoundException(String.format("Field[%s] not found", fieldParam));
        }
        if (!field.getUserEditable()) {
            throw new ForbiddenException(String.format("Forbidden field %s, for user %s", field.getName(), user.getUsername()));
        }
        UserField userField = UserFieldService.getInstance().selectByUserAndUserField(user, field);
        if (!currentUser.getId().equals(userId)) {
            String[] permissions = new String[]{"user:u:" + userId, "user:i:" + userId};
            if (!PermissionsUtils.isPermittedAny(SecurityUtils.getSubject(), Arrays.asList(permissions))) {
                throw new ForbiddenException(String.format("Forbidden UserField[%d]", userField.getId()));
            }
        }
        if (GroupService.getInstance().isGroupNotInsideTree(user.getActiveGroup(), visibilityGroup)) {
            throw new ForbiddenException(String.format("Forbidden User[%d]", user.getId()));
        }
        if (GroupService.getInstance().isGroupNotInsideTree(field.getGroup(), visibilityGroup)) {
            throw new ForbiddenException(String.format("Forbidden Field[%d]", field.getId()));
        }
        if (LicenseService.getInstance().isValidField(user, userField.getField().getName())) {
            throw new ForbiddenException(String.format("Forbidden UserField[%d]", userField.getId()));
        }
        if (!params.containsKey(getPath(QUserField.userField.value))) {
            throw new UserException("Parameter value is required");
        }
        userField = UserFieldService.getInstance().set(user, field, (String) params.get(getPath(QUserField.userField.value)));
        return RestUtils.sendCreatedResponse(userField.publicMap(true));
    }

    @GET
    @Path("/{id}/field/{field}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(value = "Update User Field")
    public Response getGroupField(@PathParam("id") Long userId, @PathParam("field") String fieldParam) {
        User currentUser = (User) SecurityUtils.getSubject().getPrincipal();
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(User.class.getCanonicalName(), null);
        User user = UserService.getInstance().get(userId);
        if (user == null) {
            throw new NotFoundException(String.format("User[%d] not found", userId));
        }
        Field field;
        try {
            field = FieldService.getInstance().get(Long.valueOf(fieldParam));
        } catch (Exception ex) {
            field = FieldService.getInstance().selectByName(fieldParam);
        }

        UserField userField = UserFieldService.getInstance().selectByUserAndUserField(user, field);
        if (!currentUser.getId().equals(userId)) {
            String[] permissions = new String[]{"user:r:" + userId};
            if (!PermissionsUtils.isPermittedAny(SecurityUtils.getSubject(), Arrays.asList(permissions))) {
                throw new ForbiddenException(String.format("Forbidden UserField"));
            }
        }
        if (GroupService.getInstance().isGroupNotInsideTree(user.getActiveGroup(), visibilityGroup)) {
            throw new ForbiddenException(String.format("Forbidden User[%d]", user.getId()));
        }
        if (GroupService.getInstance().isGroupNotInsideTree(field.getGroup(), visibilityGroup)) {
            throw new ForbiddenException(String.format("Forbidden Field[%d]", field.getId()));
        }
        return RestUtils.sendOkResponse(userField.publicMap(true));
    }

    @GET
    @Path("/{id}/permissions")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(value = "Get Shiro Permissions")
    public Response setGroupField(@PathParam("id") Long userId) {
        User user = (User) SecurityUtils.getSubject().getPrincipal();
        if (!user.getId().equals(userId)) {
            throw new ForbiddenException("Not Allowed Access");
        }
        SecurityUtils.getSubject().hasRole("");
        List<String> results = new ArrayList<>();
        Map<String, Object> mapResponse = new HashMap<>();
        Set<Permission> permissions = RiotShiroRealm.getPermissionsCache();
        if (permissions != null) {
            for (Permission permission1 : permissions) {
                results.add(((RiotPermission) permission1).toString().replaceAll("\\[", "").replaceAll("\\]", ""));
            }
        }

        Map<String, Object> permissionQuery = new HashMap<>();
        permissionQuery.put("_anything:r", "_anything:r");
        permissionQuery.put("_anything:i", "_anything:i");
        permissionQuery.put("_anything:u", "_anything:u");
        permissionQuery.put("_anything:d", "_anything:d");
        permissionQuery.put("_anything:a", "_anything:a");
        Map<String, Object> result = evaluatePermissions(permissionQuery);
        for (Map.Entry<String, Object> entry : result.entrySet()) {
            if ((Boolean) entry.getValue()) {
                results.add(entry.getKey());
            }
        }
        Collections.sort(results);
        mapResponse.put("results", results);
        return RestUtils.sendOkResponse(mapResponse);
    }

    @POST
    @Path("/{id}/hasPermissions")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(value = "Get Shiro Permissions")
    public Response hasPermissions(@PathParam("id") Long userId, Map<String, Object> permissionMap) {
        User user = (User) SecurityUtils.getSubject().getPrincipal();
        if (!user.getId().equals(userId)) {
            throw new UserException("Not available for you");
        }
        Map<String, Object> result = evaluatePermissions(permissionMap);
        return RestUtils.sendOkResponse(result);
    }

    @DELETE
    @Path("/{id}/field/{field}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(value = "Delete UserField")
    public Response deleteUserField(@PathParam("id") Long userId, @PathParam("field") String fieldParam) {
        User currentUser = (User) SecurityUtils.getSubject().getPrincipal();
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(User.class.getCanonicalName(), null);
        User user = UserService.getInstance().get(userId);
        if (user == null) {
            throw new NotFoundException(String.format("User[%d] not found", userId));
        }
        Field field;
        try {
            field = FieldService.getInstance().get(Long.valueOf(fieldParam));
        } catch (Exception ex) {
            field = FieldService.getInstance().selectByName(fieldParam);
        }
        if (field == null) {
            throw new NotFoundException(String.format("Field[%s] not found", fieldParam));
        }
        UserField userField = UserFieldService.getInstance().selectByUserAndUserField(user, field);
        if (userField == null) {
            return RestUtils.sendDeleteResponse();
            //throw new NotFoundException(String.format("User Field[%s, %s] not found", userId, fieldParam));
        }
        if (!currentUser.getId().equals(userId)) {
            String[] permissions = new String[]{"user:u:" + userId, "user:i:" + userId};
            if (!PermissionsUtils.isPermittedAny(SecurityUtils.getSubject(), Arrays.asList(permissions))) {
                throw new ForbiddenException(String.format("Forbidden UserField[%d]", userField.getId()));
            }
        }
        if (GroupService.getInstance().isGroupNotInsideTree(user.getActiveGroup(), visibilityGroup)) {
            throw new ForbiddenException(String.format("Forbidden User[%d]", user.getId()));
        }
        if (GroupService.getInstance().isGroupNotInsideTree(field.getGroup(), visibilityGroup)) {
            throw new ForbiddenException(String.format("Forbidden Field[%d]", field.getId()));
        }
        UserFieldService.getInstance().unset(user, field);
        return RestUtils.sendDeleteResponse();
    }


    @GET
    @Path("/{id}/fields")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(value = "Get UserFields")
    public Response listUserFields(@PathParam("id") Long userId, @QueryParam("pageSize") Integer pageSize,
                                   @QueryParam("pageNumber") Integer pageNumber) throws Exception {
        User currentUser = (User) SecurityUtils.getSubject().getPrincipal();
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(User.class.getCanonicalName(), null);
        User user = UserService.getInstance().get(userId);
        if (user == null) {
            throw new NotFoundException(String.format("User [%d] not found", userId));
        }
        if (GroupService.getInstance().isGroupNotInsideTree(user.getActiveGroup(), visibilityGroup)) {
            throw new ForbiddenException(String.format("Forbidden User[%d]", user.getId()));
        }
        if (!currentUser.getId().equals(userId)) {
            String[] permissions = new String[]{"user:r:" + userId};
            if (!PermissionsUtils.isPermittedAny(SecurityUtils.getSubject(), Arrays.asList(permissions))) {
                throw new ForbiddenException(String.format("Forbidden Access"));
            }
        }
        Map<String, Object> mapResponse = new HashMap<>();
        Pagination pagination = new Pagination(pageNumber, pageSize);
        Long count = GroupFieldService.getInstance().countFieldsByGroup(user.getActiveGroup());
        List<UserField> userFields = UserFieldService.getInstance().listFieldsPaginatedByUser(pagination, user);
        List<Map<String, Object>> list = new LinkedList<>();
        for (UserField userField : userFields) {
            if (LicenseService.getInstance().isValidField(user, userField.getValue())) {
                list.add(userField.publicMap(true));
            }
        }
        mapResponse.put("total", count);

        mapResponse.put("results", list);
        return RestUtils.sendOkResponse(mapResponse);
    }

    @GET
    @Path("/{id}/inheritedFields")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(value = "Get Inherited UserFields")
    public Response listDerivedUserFields(@PathParam("id") Long userId) throws Exception {
        User currentUser = (User) SecurityUtils.getSubject().getPrincipal();
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(User.class.getCanonicalName(), null);
        User user = UserService.getInstance().get(userId);
        if (user == null) {
            throw new NotFoundException(String.format("User [%d] not found", userId));
        }
        if (GroupService.getInstance().isGroupNotInsideTree(user.getActiveGroup(), visibilityGroup)) {
            throw new ForbiddenException(String.format("Forbidden User[%d]", user.getId()));
        }
        if (!currentUser.getId().equals(userId)) {
            String[] permissions = new String[]{"user:r:" + userId};
            if (!PermissionsUtils.isPermittedAny(SecurityUtils.getSubject(), Arrays.asList(permissions))) {
                throw new ForbiddenException(String.format("Forbidden Access"));
            }
        }
        //Map<String, Object> mapResponse = new HashMap<>();
        Map<String, Object> userFields = new HashMap<>();
        Map<String, Object> userFieldsAux = UserFieldService.getInstance().listInheritedFieldsByUser(user, true);
        LicenseService licenseService = LicenseService.getInstance();
        for (Map.Entry<String, Object> entry : userFieldsAux.entrySet()) {
            String fieldName = entry.getKey();
            if (licenseService.isValidField(user, fieldName)) {
                userFields.put(fieldName, entry.getValue());
            }
        }
        //mapResponse.put("total", userFields.size());
        //mapResponse.put("results", userFields);
        return RestUtils.sendOkResponse(userFields);
    }

    @GET
    @Path("/{id}/inheritedFields/{field}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(value = "Get Inherited UserFields")
    public Response listDerivedUserField(@PathParam("id") Long userId, @PathParam("field") String fieldParam) throws Exception {
        User currentUser = (User) SecurityUtils.getSubject().getPrincipal();
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(User.class.getCanonicalName(), null);
        User user = UserService.getInstance().get(userId);
        if (user == null) {
            throw new NotFoundException(String.format("User [%d] not found", userId));
        }
        if (GroupService.getInstance().isGroupNotInsideTree(user.getActiveGroup(), visibilityGroup)) {
            throw new ForbiddenException(String.format("Forbidden User[%d]", user.getId()));
        }
        if (!currentUser.getId().equals(userId)) {
            String[] permissions = new String[]{"user:r:" + userId};
            if (!PermissionsUtils.isPermittedAny(SecurityUtils.getSubject(), Arrays.asList(permissions))) {
                throw new ForbiddenException(String.format("Forbidden Access"));
            }
        }
        Field field;
        try {
            field = FieldService.getInstance().get(Long.valueOf(fieldParam));
        } catch (Exception ex) {
            field = FieldService.getInstance().selectByName(fieldParam);
        }

        if (field == null) {
            throw new NotFoundException(String.format("Field[%s] not found", fieldParam));
        }

        Map<String, Object> userFields = UserFieldService.getInstance().listInheritedFieldsByUser(user, true);
        return RestUtils.sendOkResponse(userFields.get(field.getName()));
    }

    @GET
    @Path("/role/")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value = {"user:r"})
    @ApiOperation(position = 1, value = "Get a List of Roles (AUTO)")
    public Response listRoles(@QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber, @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only, @QueryParam("visibilityGroupId") Long visibilityGroupId, @DefaultValue("") @QueryParam("upVisibility") String upVisibility, @DefaultValue("") @QueryParam("downVisibility") String downVisibility, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project) {
        RoleController c = new RoleController();
        //TODO VERIFY THIS OVERRIDE
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(User.class.getCanonicalName(), visibilityGroupId);
        RiotShiroRealm.getOverrideVisibilityCache().put(Role.class.getCanonicalName(), visibilityGroup);
        return c.listRoles(pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility, downVisibility, false, extend, project);
    }

    @GET
    @Path("/licenseDetail/features")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 1, value = "Get a List of Features that the user can see")
    public Response listUserFeatures() {
        Subject currentUser = SecurityUtils.getSubject();
        User user = (User) currentUser.getPrincipal();
        LicenseService licenseService = LicenseService.getInstance();
        LicenseDetail licenseDetail = licenseService.getLicenseDetail(user.getActiveGroup(), false);
        List<String> features = licenseDetail.getFeatures();
        return RestUtils.sendOkResponse(features);
    }

    @GET
    @Path("/licenseDetail/modules")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 1, value = "Get a List of Modules that the use can see")
    public Response listRoles() {
        Subject currentUser = SecurityUtils.getSubject();
        User user = (User) currentUser.getPrincipal();
        LicenseService licenseService = LicenseService.getInstance();
        LicenseDetail licenseDetail = licenseService.getLicenseDetail(user.getActiveGroup(), false);
        List<String> modules = licenseDetail.getModules();
        return RestUtils.sendOkResponse(modules);
    }

    @Override
    public List<String> getExtraPropertyNames() {
        return Arrays.asList(IGNORE_USER_FIELDS);
    }

    @GET
    @Path("/limits")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 20, value = "Get a List of Limits for Users")
    public Response verifyObjectLimits() {
        User user = (User) SecurityUtils.getSubject().getPrincipal();
        HashMap<String, Number> defaultHM = new HashMap<>();
        defaultHM.put("limit", -1);
        defaultHM.put("used", 0);
        Map<String, Map> mapResponse = new HashMap<String, Map>();
        mapResponse.put("numberOfUsers", defaultHM);
        if (LicenseService.enableLicense) {
            LicenseDetail licenseDetail = LicenseService.getInstance().getLicenseDetail(user.getActiveGroup(), true);
            Long maxNumberOfUsers = licenseDetail.getMaxNumberOfUsers();
            if (maxNumberOfUsers != null && maxNumberOfUsers > 0) {
                Long countAll = count(licenseDetail);
                defaultHM.put("limit", maxNumberOfUsers);
                defaultHM.put("used", countAll);
            }
        }
        return RestUtils.sendOkResponse(mapResponse);
    }

    public static Long count(LicenseDetail licenseDetail) {
        GroupService groupService = GroupService.getInstance();
        Group licenseGroup = groupService.get(licenseDetail.getGroupId());
        UserService userService = UserService.getInstance();
        Long countAll;
        boolean isRootLicense = groupService.getRootGroup().getId().equals(licenseGroup.getId());
        if (isRootLicense) {
            countAll = userService.countAllActive();
        } else {
            countAll = userService.countAllActive(licenseGroup);
        }
        return countAll;
    }

    @PATCH
    @Path("/{id}/changePassword")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @PermitAll
    @ApiOperation(
            position = 21,
            value = "Update password",
            notes = "This method change the current password of any user.<br>"
                    + "{<br>&nbsp;&nbsp;\"currentPassword\": \"password\", \n"
                    + "<br>&nbsp;&nbsp;\"newPassword\": \"newpassword\",\n"
                    + "<br>&nbsp;&nbsp;\"confirmPassword\": \"newpassword\"\n<br>}")

    public Response changeOldPassword(@ApiParam(value = "Id of the User.") @PathParam("id") Long userId, Map<String, Object> passwordConf) {
        if (!passwordConf.containsKey("currentPassword") || !passwordConf.containsKey("newPassword") || !passwordConf.containsKey("confirmPassword")) {
            throw new UserException("All fields are required, please try again.");
        }
        Map<String, Object> result = UserService.getInstance().changeOldPassword(
                (String) passwordConf.get("currentPassword"),
                (String) passwordConf.get("newPassword"),
                (String) passwordConf.get("confirmPassword"), userId);
        return RestUtils.sendOkResponse(result);
    }

    @GET
    @Path("/{id}/apiKey")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @RequiresPermissions(value = {"user:r:{id}"})
    @ApiOperation(
            position = 22,
            value = "Get User API_KEY",
            notes = "This method returns current user API KEY.")

    public Response apiKey(@ApiParam(value = "User ID") @PathParam("id") Long userId) {
        User user = UserService.getInstance().get(userId);
        return RestUtils.sendOkResponse(user.getApiKey());
    }

    @POST
    @Path("/{id}/apiKey/regenerate")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @RequiresPermissions(value = {"user:r:{id}"})
    @ApiOperation(
            position = 23,
            value = "Get User API_KEY",
            notes = "This method returns current user API KEY.")

    public Response apiKeyRegenerate(@ApiParam(value = "User ID") @PathParam("id") Long userId, Map<String, Object> loginMap) {
        return RestUtils.sendOkResponse(UserService.getInstance().regenerateApiKey(userId));
    }
}
