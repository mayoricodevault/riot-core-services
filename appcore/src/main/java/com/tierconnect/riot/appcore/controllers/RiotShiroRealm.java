package com.tierconnect.riot.appcore.controllers;

import com.tierconnect.riot.appcore.dao.UserDAO;
import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.services.LicenseService;
import com.tierconnect.riot.appcore.services.UserRoleService;
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
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RiotShiroRealm extends AuthorizingRealm {
	static Logger logger = Logger.getLogger(RiotShiroRealm.class);

	QUser qUser = QUser.user;

    private static ThreadLocal<Set<Permission>> permissionsCache = new ThreadLocal<Set<Permission>>() {
        @Override
        protected Set<Permission> initialValue() {
            return Collections.newSetFromMap(new ConcurrentHashMap<>());
        }
    };

    private static ThreadLocal<Map<String, Group>> visibilityCache = new ThreadLocal<Map<String, Group>>() {
        @Override
        protected Map<String, Group> initialValue() {
            return new ConcurrentHashMap<>();
        }
    };

    private static ThreadLocal<Map<Long, String>> visibilityTypeCache = new ThreadLocal<Map<Long, String>>() {
        @Override
        protected Map<Long, String> initialValue() {
            return new ConcurrentHashMap<>();
        }
    };

    private static ThreadLocal<Map<String, Group>> overrideVisibilityCache = new ThreadLocal<Map<String, Group>>() {
        @Override
        protected Map<String, Group> initialValue() {
            return new ConcurrentHashMap<>();
        }
    };

    private static ThreadLocal<Map<String, Set<Long>>> objectPermissionCache = new ThreadLocal<Map<String, Set<Long>>>() {
        @Override
        protected Map<String, Set<Long>> initialValue() {
            return new ConcurrentHashMap<>();
        }
    };

    public static Set<Permission> getPermissionsCache() {
		return permissionsCache.get();
	}

    public static Map<String, Group> getVisibilityCache() {
        return visibilityCache.get();
    }

    public static Map<String, Group> getOverrideVisibilityCache() {
        return overrideVisibilityCache.get();
    }

    public static Map<Long, String> getVisibilityTypeCache() {
        return visibilityTypeCache.get();
    }

    public static Map<String, Set<Long>> getObjectPermissionsCache() {
        return objectPermissionCache.get();
    }
    @Override
	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        logger.debug("AGG 4.START doGetAuthorizationInfo{");
		Collection<User> principalsList = principals.byType(User.class);
		if (principalsList.isEmpty()) {
			throw new AuthorizationException("Empty principals list!");
		}
		Set<String> resultRoles = Collections.newSetFromMap(new ConcurrentHashMap<>());
		Set<Permission> resultPermissions = Collections.newSetFromMap(new ConcurrentHashMap<>());
		calculateRolesAndResources(principalsList, resultRoles, resultPermissions);
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo(resultRoles);
        permissionsCache.set(resultPermissions);
		info.setRoles(resultRoles);
		info.setObjectPermissions(resultPermissions);
		return info;
	}

    public static void initCaches() {
        permissionsCache.set(Collections.newSetFromMap(new ConcurrentHashMap<>()));
        visibilityCache.set(new ConcurrentHashMap<>());
        overrideVisibilityCache.set(new ConcurrentHashMap<>());
        visibilityTypeCache.set(new ConcurrentHashMap<>());
    }

    public static void removeCaches() {
        permissionsCache.remove();
        visibilityCache.remove();
        overrideVisibilityCache.remove();
        visibilityTypeCache.remove();
        objectPermissionCache.remove();
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        logger.debug("AGG 3.START doGetAuthenticationInfo{");
        ApiKeyToken apiKeyToken = (ApiKeyToken) token;
        User user = getUserByApiKey(apiKeyToken.getApiKey());
        if (user == null) {
            throw new AuthenticationException("Api Key [" + apiKeyToken.getApiKey() + "] not found!");
        }
        if (user.isArchived()) {
            throw new AuthenticationException("User is Archived");
        }
        if (user.getActiveGroup().isArchived()) {
            throw new AuthenticationException("Group is Archived");
        }
        return new SimpleAuthenticationInfo(user, user.getApiKey(), getName());
    }


	@Override
	public Class getAuthenticationTokenClass() {
		return ApiKeyToken.class;
	}


    /**
     * @param principalsList    Input list of principals (User/Passwords)
     * @param resultRoles       Output list of Shiro Resources
     * @param resultPermissions Output list of Shiro Permission
     */
    protected void calculateRolesAndResources(Collection<User> principalsList, Set<String> resultRoles,
                                              Set<Permission> resultPermissions) {
        LicenseService licenseService = LicenseService.getInstance();
        visibilityCache.get().clear();
        Map<String, String> resourceMap = new ConcurrentHashMap<>();
        AtomicInteger count = new AtomicInteger();
        for (User userPrincipal : principalsList) {
            // To reload current version of the object
            User user = getUserByApiKey(userPrincipal.getApiKey());
            Group activeGroup = user.getActiveGroup();
            GroupType groupType = activeGroup.getGroupType();
            List<UserRoleResource> userRoleResources = UserRoleService.getInstance().listUserRoleAndResources(user);

            // Iterate Roles and saving permissions in a concurrent collection
            Set<String> permissionList = Collections.newSetFromMap(new ConcurrentHashMap<>());
            userRoleResources.stream().forEach(urr -> {
                resultPermissions.add(new RiotPermission("role:r:" + urr.getRoleId()));
                resultRoles.add(urr.getRoleName());
                if(urr.getPermissions().length() == 1 && urr.getPermissions().charAt(0) == 'x'){
                    count.addAndGet(1);
                }
                boolean evaluatePermissions = false;
                if(!resourceMap.containsKey(urr.getResourceName())){
                    evaluatePermissions = licenseService.isValidResource(user, getResourceName(urr.getResourceName()));
                    resourceMap.put(urr.getResourceName(), urr.getResourceName());
                }

                if(evaluatePermissions) {
                    resultPermissions.add(new RiotPermission("resource:r:" + urr.getResourceId()));
                    List<String> operations = Arrays.asList(urr.getPermissions().split("(?!^)"));
                    if (urr.getResourceType() == ResourceType.THING_TYPE_CLASS.getId()) {
                        try{
                            visibilityTypeCache.get().put(urr.getResourceTypeId(), urr.getResourceName());
                        } catch(NullPointerException npe){
                            logger.warn("resourceTypeId is null for resourceType = " + urr.getResourceTypeId() + " resource = " + urr.getResourceName());
                        }
                    }
                    String visibilityCacheKey = urr.getResourceFqName();
                    if (urr.getGroupTypeCeiling() != null) {
                        if (!groupType.getId().equals(urr.getGroupTypeCeiling())) {
                            Group visibilityGroup = activeGroup;
                            while (visibilityGroup != null) {
                                if (urr.getGroupTypeCeiling().compareTo(visibilityGroup.getGroupType().getId()) == 0 ) {
                                    Group oldVisibilityGroup = visibilityCache.get().get(visibilityCacheKey);
                                    if (oldVisibilityGroup == null || (visibilityGroup.getTreeLevel() < oldVisibilityGroup.getTreeLevel())) {
                                        visibilityCache.get().put(visibilityCacheKey, visibilityGroup);
                                    }
                                    break;
                                }
                                visibilityGroup = visibilityGroup.getParent();
                            }
                        }
                    }
                    Group visibilityGroup = activeGroup;
                    Group oldVisibilityGroup = visibilityCache.get().get(visibilityCacheKey);
                    if (oldVisibilityGroup == null) {
                        visibilityCache.get().put(visibilityCacheKey, visibilityGroup);
                    }
                    boolean operationsDefined = !operations.isEmpty();
                    if (operationsDefined) {
                        List<String> acceptedAttributeList = Arrays.asList(urr.getAcceptedAttributes().split("(?!^)"));
                        //Object permission
                        if (urr.getResourceType() == ResourceType.DATA_ENTRY.getId() || urr.getResourceType() == ResourceType.REPORT_DEFINITION.getId()) {
                            //reportEntryOption:p:5
                            for (String operation : operations) {
                                if (acceptedAttributeList.contains(operation)) {
                                    permissionList.add(getResourceName(urr.getResourceName()) + ":" + operation + ":" + urr.getResourceTypeId());
                                    Set<Long> l = objectPermissionCache.get().get(getResourceName(urr.getResourceName()));
                                    if (l == null) {
                                        l = new HashSet<>();
                                        objectPermissionCache.get().put(getResourceName(urr.getResourceName()), l);
                                    }
                                    l.add(urr.getResourceTypeId());
                                }
                            }
                        } else {
                            //user:r
                            for (String operation : operations) {
                                if (acceptedAttributeList.contains(operation)) {
                                    permissionList.add(urr.getResourceName().toLowerCase() + ":" + operation);
                                }
                            }
                        }
                    } else {
                        //user:*
                        permissionList.add(urr.getResourceName().toLowerCase() + ":*");
                    }
                }
            });

            if(count.get() == userRoleResources.size()){//all permissions are just x
                permissionList.clear();
            }
            Set<Permission> roleResourcePermission = new HashSet<>();
            // removing model if user doesn't have localmap or zone
            if ( permissionList.contains("model:x") && permissionList.contains("mapmaker:x") ){
                boolean removePermissions = true;
                for (String permission : permissionList){
                    if (permission.contains("localmap") || permission.contains("zone")){
                        removePermissions = false;
                    }
                }
                if (removePermissions) {
                    permissionList.remove("model:x");
                    permissionList.remove("mapmaker:x");
                }
            }

            for (String permissionAsString : permissionList){
                Permission permission = new RiotPermission(permissionAsString);
                roleResourcePermission.add(permission);
            }
            resultPermissions.addAll(roleResourcePermission);
            //Default Permissions
            // A user can see is own information
            resultPermissions.add(new RiotPermission("user:r:" + user.getId()));
            Group aux = user.getGroup();
            while (aux!= null) {
                resultPermissions.add(new RiotPermission("group:r:" + aux.getId()));
                resultPermissions.add(new RiotPermission("grouptype:r:" + aux.getGroupType().getId()));
                aux = aux.getParent();
            }
            aux = user.getRoamingGroup();
            while (aux!= null) {
                resultPermissions.add(new RiotPermission("group:r:" + aux.getId()));
                resultPermissions.add(new RiotPermission("grouptype:r:" + aux.getGroupType().getId()));
                aux = aux.getParent();
            }
        }
    }

    private static String getResourceName(String name) {
        if (name.indexOf("_") != name.lastIndexOf("_")) {
            return name.substring(name.indexOf("_")+1, name.lastIndexOf("_"));
        } else {
            return name;
        }
    }

    private User getUserByApiKey(String apiKey) {
        return new UserDAO().getQuery().where(qUser.apiKey.eq(apiKey)).uniqueResult(qUser);
    }

    public static void main (String[] args) {
        System.out.println(getResourceName("_reportDefinition_6"));
    }

}
