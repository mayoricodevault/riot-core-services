package com.tierconnect.riot.appcore.servlet;

import java.io.IOException;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import com.tierconnect.riot.appcore.annotations.RequiresSession;
import com.tierconnect.riot.appcore.controllers.RiotShiroRealm;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.services.LicenseService;
import com.tierconnect.riot.sdk.servlet.security.ApiKeyToken;
import com.tierconnect.riot.sdk.utils.PermissionsUtils;

import org.apache.log4j.Logger;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresGuest;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresUser;
import org.apache.shiro.config.IniSecurityManagerFactory;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.Factory;
import org.apache.shiro.util.ThreadContext;
import org.jboss.resteasy.core.Headers;
import org.jboss.resteasy.core.ResourceMethodInvoker;
import org.jboss.resteasy.core.ServerResponse;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Provider
public class SecurityFilter implements javax.ws.rs.container.ContainerRequestFilter, ContainerResponseFilter {
    static Logger logger = Logger.getLogger(SecurityFilter.class);

    private static final String API_KEY_HEADER = "api_key";

    private static final String TOKEN_HEADER = "token";

    private static final String IGNORE_SESSION_UPDATE = "ignore_session_update";


    // Invalid User
    private static final ServerResponse ACCESS_DENIED = new ServerResponse(
            "{\"error\":\"Not Authenticated, Access Denied\"}", 401, new Headers<>());

    private static final ServerResponse SESSION_EXPIRED = new ServerResponse(
            "{\"error\":\"Your Session has expired\"}", 401, new Headers<>());

    private static final ServerResponse LICENSE_EXPIRED = new ServerResponse(
            "{\"error\":\"This ViZix license has expired. Please contact your application administrator.\"}", 403, new
            Headers<>());

    // Invalid Role, Resource or Group
    private static final ServerResponse ACCESS_FORBIDDEN = new ServerResponse(
            "{\"error\":\"Not Authorized, Access Denied\"}", 403, new Headers<>());

    private static ThreadLocal<String> token = new ThreadLocal<>();

    static {
        Factory<SecurityManager> factory = new IniSecurityManagerFactory("classpath:shiro_riot.ini");
        SecurityManager securityManager = factory.getInstance();
        SecurityUtils.setSecurityManager(securityManager);
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        before(requestContext);
    }

    @Override
    public void filter(ContainerRequestContext arg0,
                       ContainerResponseContext arg1) throws IOException {
        after();
    }

    public static void before(ContainerRequestContext requestContext)
            throws IOException {
        token.set(null);

        String path = requestContext.getUriInfo().getPath();
        logger.debug("AGG 2.START Security Filter ");
        ResourceMethodInvoker methodInvoker = (ResourceMethodInvoker) requestContext.getProperty("org.jboss.resteasy" +
                ".core.ResourceMethodInvoker");
        Method method = methodInvoker.getMethod();

        if (method.getDeclaringClass().getName().startsWith("com.wordnik.swagger")) {
            return;
        }

        // Access denied for all
        if (PermissionsUtils.isAuthorizationEnabled()) {//Temporary disabling all the permission validations
            if (method.isAnnotationPresent(DenyAll.class)) {//For Deprecation
                requestContext.abortWith(ACCESS_FORBIDDEN);
                return;
            }
        }

        if (method.isAnnotationPresent(PermitAll.class) || method.isAnnotationPresent(RequiresGuest.class)) {
            return;
        }

        //RequiresUser, RequiresAuthentication
        final MultivaluedMap<String, String> headers = requestContext.getHeaders();
        final List<String> apiKeyHeader = headers.get(API_KEY_HEADER);
        final List<String> tokenHeader = headers.get(TOKEN_HEADER);
        if ((apiKeyHeader == null || apiKeyHeader.isEmpty()) && (tokenHeader == null || tokenHeader.isEmpty())) {
            logger.error("AGG No security headers: " + API_KEY_HEADER + " and " + TOKEN_HEADER + " path: " + path);
            requestContext.abortWith(ACCESS_DENIED);
            return;
        }

        boolean ignoreHeader = false;
        final List<String> ignoreHeaders = headers.get(IGNORE_SESSION_UPDATE);
        if (ignoreHeaders != null && !ignoreHeaders.isEmpty() && "true".equals(ignoreHeaders.get(0))) {
            ignoreHeader = true;
        }

        ApiKeyToken apiKeyToken;
        if (apiKeyHeader == null) {
            String tokenApiKey = TokenCacheHelper.getTokenApiKey(requestContext, ignoreHeader, tokenHeader.get(0));
            if (tokenApiKey == null){
                return;
            }
            apiKeyToken = new ApiKeyToken(tokenApiKey);
        } else {
            apiKeyToken = new ApiKeyToken(apiKeyHeader.get(0));
        }
        Subject currentUser;
        try {
            logger.debug("AGG 2.1 START LOGIN Security Filter{");
            RiotShiroRealm.initCaches();
            currentUser = PermissionsUtils.loginUser(apiKeyToken);
            //Subject subject =currentUser
            // subject.getSession().setTimeout(sessionTimeOutInMinutes * 60 * 1000 + 1000);
            // this was causing problems to core bridge
            logger.debug("AGG 2.2 OK LOGIN Security Filter{");
        } catch (Exception ex) {
            logger.debug("AGG 2.3 BAD LOGIN Security Filter{");
            requestContext.abortWith(ACCESS_DENIED);
            return;
        }

        if (!PermissionsUtils.isAuthorizationEnabled()) {
            return;
        }

        // Verify roles
        //E.g. This endpoint can only be used by ROOT Role.
        //if (method.isAnnotationPresent(RolesAllowed.class)) {
        //RolesAllowed rolesAnnotation = method.getAnnotation(RolesAllowed.class);
//			if (!currentUser.hasAllRoles(Arrays.asList(rolesAnnotation.value()))) {
//                logger.error("User doesn't have roles A : " + Arrays.asList(rolesAnnotation.value())+ " user:"+
// currentUser.getPrincipal());
//				requestContext.abortWith(ACCESS_FORBIDDEN);
//				return;
//			}
        //}

//		// Verify roles
//		if (method.isAnnotationPresent(RequiresRoles.class)) {
//			RequiresRoles rolesAnnotation = method.getAnnotation(RequiresRoles.class);
//			if (!currentUser.hasAllRoles(Arrays.asList(rolesAnnotation.value()))) {
//                logger.error("User doesn't have roles B : " + Arrays.asList(rolesAnnotation.value())+ " user:"+
// currentUser.getPrincipal());
//				requestContext.abortWith(ACCESS_FORBIDDEN);
//				return;
//			}
//		}

        // Verify resources
        boolean hasActiveLicense =
            LicenseService.getInstance().hasValidActiveLicense((User) currentUser.getPrincipal());
        if (method.isAnnotationPresent(RequiresPermissions.class)) {
            RequiresPermissions resourcesAnnotation = method.getAnnotation(RequiresPermissions.class);
            MultivaluedMap pathParameters = requestContext.getUriInfo().getPathParameters();
            List<String> finalPermissions = new ArrayList<>();
            //Replace variables
            for (String permission : resourcesAnnotation.value()) {
                if (permission.contains("{")) {
                    while (permission.contains("{")) {
                        if (permission.contains("}")) {
                            String idName = permission.substring(permission.indexOf('{') + 1, permission.indexOf('}'));
                            Object id = pathParameters.getFirst(idName);
                            if (id != null) {
                                permission = permission.replaceAll("\\{" + idName + "\\}", id.toString());
                            } else {
                                logger.error("Invalid resource definition can't find path param: " + permission + " " +
                                        "for path:" + path);
                                requestContext.abortWith(ACCESS_FORBIDDEN);
                                return;
                            }
                        } else {
                            logger.error("Invalid resource definition: " + permission + " for path:" + path);
                            requestContext.abortWith(ACCESS_FORBIDDEN);
                            return;
                        }
                    }
                }
                finalPermissions.add(permission);
            }
            Logical logical = resourcesAnnotation.logical();
            if (logical.equals(Logical.AND) && !PermissionsUtils.isPermittedAll(currentUser, finalPermissions)) {
                logger.error("User doesn't have All resources in: " + finalPermissions + " user:" + currentUser
                        .getPrincipal() + " for path:" + path);
                requestContext.abortWith(ACCESS_FORBIDDEN);
                return;

            } else if (logical.equals(Logical.OR) && !PermissionsUtils.isPermittedAny(currentUser, finalPermissions)) {
                if (LicenseService.enableLicense && !hasActiveLicense) {
                    logger.error("User doesn't have Any resource in: " + finalPermissions + " user:"
                        + currentUser.getPrincipal() + " for path:" + path);
                    requestContext.abortWith(ACCESS_FORBIDDEN);
                    return;
                } else {
                    logger.info("Expired license.");
                    requestContext.abortWith(LICENSE_EXPIRED);
                    return;
                }
            }
        }
        if (LicenseService.enableLicense && !hasActiveLicense) {
            if (!method.isAnnotationPresent(RequiresSession.class) && !method.isAnnotationPresent(RequiresUser.class) &&
                    !PermissionsUtils.buildSearch(currentUser, new HashMap<String, Boolean>(), "license:r&" +
                    "(license:u|license:i)")) {
                logger.info("Expired license. User do not have license resource");
//                requestContext.abortWith(LICENSE_EXPIRED);
            } else {
                logger.debug("Expired license, but user has license resource");
            }
        }

    }

    public static void after() throws IOException {
        logger.debug("AGG 2.END Security Filter}");
        Subject currentUser = SecurityUtils.getSubject();
        if (currentUser.isAuthenticated() || currentUser.isRemembered()) {
            try {
                currentUser.logout();
            } catch (Exception ex) {
                logger.error(ex.getMessage());
            }
        }
        try {
            ThreadContext.unbindSubject();
        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }
        try {
            ThreadContext.unbindSecurityManager();
        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }
        try {
            RiotShiroRealm.removeCaches();
        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }
    }

}
