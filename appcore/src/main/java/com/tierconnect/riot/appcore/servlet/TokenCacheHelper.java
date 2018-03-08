package com.tierconnect.riot.appcore.servlet;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.QToken;
import com.tierconnect.riot.appcore.entities.Token;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.services.ConfigurationService;
import com.tierconnect.riot.appcore.services.TokenServiceBase;
import com.tierconnect.riot.appcore.services.UserService;
import com.tierconnect.riot.sdk.dao.HibernateDAOUtils;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;
import org.apache.log4j.Logger;
import org.apache.shiro.authz.annotation.RequiresUser;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.jboss.resteasy.core.Headers;
import org.jboss.resteasy.core.ResourceMethodInvoker;
import org.jboss.resteasy.core.ServerResponse;

import javax.ws.rs.container.ContainerRequestContext;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by dbascope on 1/19/17
 */
public class TokenCacheHelper {
    private static Logger logger = Logger.getLogger(TokenCacheHelper.class);

    private static final long WAIT_FOR_STORE_TOKEN = 60000;

    private static final Cache<String, TokenCacheElement> tokenCache;

    private static ThreadLocal<String> token = new ThreadLocal<>();

    private static final ServerResponse ACCESS_DENIED = new ServerResponse(
            "{\"error\":\"Not Authenticated, Access Denied\"}", 401, new Headers<>());

    private static final ServerResponse SESSION_EXPIRED = new ServerResponse(
            "{\"error\":\"Your Session has expired\"}", 401, new Headers<>());

    private static class TokenCacheElement {
        long userId;
        long expirationTime;
        long expirationTimeDb;
        Boolean isActive;
        String tokenString;
    }

    static {
        tokenCache = CacheBuilder.newBuilder().maximumSize(10000).expireAfterWrite(60, TimeUnit.MINUTES).build();
    }

    private static void setTokenExpirationDate(final TokenCacheHelper.TokenCacheElement tokenCacheElement, final long expirationTime) {
        final long currentTimeMillis = System.currentTimeMillis();
        if (tokenCacheElement.expirationTimeDb - currentTimeMillis <= WAIT_FOR_STORE_TOKEN) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    Session session = HibernateSessionFactory.getInstance().getCurrentSession();
                    Transaction transaction = session.getTransaction();
                    transaction.begin();
                    try {
                        QToken qToken = QToken.token;
                        Token aux = TokenServiceBase.getTokenDAO().selectBy(qToken.tokenString.eq(tokenCacheElement
                                .tokenString));
                        if (aux != null) {
                            if (aux.getTokenActive()) {
                                aux.setTokenExpirationTime(new Date(expirationTime));
                                tokenCacheElement.expirationTimeDb = expirationTime;
                                tokenCacheElement.isActive = true;
                            } else {
                                logger.warn("Token " + tokenCacheElement.tokenString + " is inactive.");
                                tokenCacheElement.isActive = false;
                            }
                        } else {
                            logger.warn("Token " + tokenCacheElement.tokenString + " exists in tokenCacheElement but " +
                                    "does not exist in database");
                        }
                        transaction.commit();
                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                        HibernateDAOUtils.rollback(transaction);
                    }

                }
            });
            t.start();
        }
        tokenCacheElement.expirationTime = expirationTime;
    }

    private static TokenCacheElement getToken(String tokenString) {
        TokenCacheElement tokenCacheElement = tokenCache.getIfPresent(tokenString);
        if (tokenCacheElement != null) {
            return tokenCacheElement;
        } else {
            QToken qToken = QToken.token;
            Token token = TokenServiceBase.getTokenDAO().selectBy(qToken.tokenString.eq(tokenString));
            if (token == null) {
                return null;
            }
            tokenCacheElement = new TokenCacheElement();
            tokenCacheElement.userId = token.getUser().getId();
            tokenCacheElement.tokenString = tokenString;
            tokenCacheElement.expirationTime = token.getTokenExpirationTime().getTime();
            tokenCacheElement.isActive = token.getTokenActive();
            tokenCache.put(tokenString, tokenCacheElement);
            return tokenCacheElement;
        }
    }

    static String getTokenApiKey(ContainerRequestContext requestContext,
                                 boolean ignoreHeader,
                                 String tokenHeaderElement){
        logger.debug("AGG 2.START Security Filter ");
        String path = requestContext.getUriInfo().getPath();
        ResourceMethodInvoker methodInvoker = (ResourceMethodInvoker) requestContext.
                getProperty("org.jboss.resteasy.core.ResourceMethodInvoker");
        Method method = methodInvoker.getMethod();
        token.set(tokenHeaderElement);
        TokenCacheElement tokenObject = getToken(tokenHeaderElement);
        if (tokenObject == null) {
            logger.error("Invalid token:" + token + " path: " + path);
            requestContext.abortWith(ACCESS_DENIED);
            return null;
        }
        User user = UserService.getInstance().get(tokenObject.userId);
        long currentTime = System.currentTimeMillis();
        if (tokenObject.expirationTime < currentTime || !tokenObject.isActive) {
            if (!method.isAnnotationPresent(RequiresUser.class)) {
                logger.info("Expired token:" + token + " path: " + path);
                requestContext.abortWith(SESSION_EXPIRED);
                return null;
            }
        }
        if (!ignoreHeader && !method.isAnnotationPresent(RequiresUser.class)) {
            Long sessionTimeOutInMinutes = ConfigurationService.getAsLong(user, "sessionTimeout");
            setTokenExpirationDate(tokenObject,
                    currentTime +
                            (sessionTimeOutInMinutes == null ? 0L : sessionTimeOutInMinutes) * 60 * 1000);
        }
        return user.getApiKey();
    }

    public static void invalidate(String token) {
        tokenCache.invalidate(token);
    }

    public static void deActivate(String token) {
        TokenCacheElement tokenCacheElement = tokenCache.getIfPresent(token);
        if (tokenCacheElement != null){
            tokenCacheElement.isActive = false;
            tokenCacheElement.expirationTime = (new Date()).getTime();
        }
    }

    public static long getConcurrentUsers(){
        long count = 0;
        for (String token : tokenCache.asMap().keySet()){
            if (tokenCache.asMap().get(token).expirationTime > System.currentTimeMillis()
                    && tokenCache.asMap().get(token).isActive){
                count++;
            }
        }
        return count;
    }

}
