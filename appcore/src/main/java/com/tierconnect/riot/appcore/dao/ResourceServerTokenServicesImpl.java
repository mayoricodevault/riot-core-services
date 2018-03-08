package com.tierconnect.riot.appcore.dao;


import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.sdk.dao.UserException;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.BaseOAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.common.AuthenticationScheme;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.*;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;

import java.util.*;

/**
 * Created by aruiz on 6/26/17.
 */
public class ResourceServerTokenServicesImpl implements ResourceServerTokenServices {

    protected static final Logger logger = LoggerFactory.getLogger(ResourceServerTokenServicesImpl.class);

    private final String userInfoEndpointUrl;

    private final String clientId;

    private static String userName;

    private OAuth2RestOperations restTemplate;

    private String tokenType = DefaultOAuth2AccessToken.BEARER_TYPE;

    public ResourceServerTokenServicesImpl(String userInfoEndpointUrl, String clientId) {
        this.userInfoEndpointUrl = userInfoEndpointUrl;
        this.clientId = clientId;
    }

    @Override
    public OAuth2Authentication loadAuthentication(String accessToken) throws AuthenticationException, InvalidTokenException {
        Map<String, Object> map = getMap(this.userInfoEndpointUrl, accessToken);
        if (map.containsKey("error")) {
            this.logger.debug("userinfo returned error: {}", map.get("error"));
            throw new InvalidTokenException(accessToken);
        }
        this.logger.debug("userinfo returned: {}", map);
        return extractAuthentication(map);
    }

    @Override
    public OAuth2AccessToken readAccessToken(String accessToken) {
        throw new UnsupportedOperationException("Not supported: read access token");
    }

    private Map<String, Object> getMap(String path, String accessToken) {
        this.logger.info("Getting user info from: {}", path);
        Group group = null;
        Map<String, String> temp = new HashMap<>();
        try {
            Class<?> srv = Class.forName("com.tierconnect.riot.appcore.services.GroupService");
            Group groupObject = (Group) srv.newInstance().getClass().getMethod("getRootGroup").invoke(srv.newInstance());

             if (groupObject instanceof  Group){
                 group = (Group) groupObject;
             }
            Map mapValue = (Map) Class.forName("com.tierconnect.riot.appcore.utils.AuthenticationUtils").
                            getMethod("getConnectionProperties",Group.class,String.class).
                            invoke(null,group,"oauth2Connection" );
            if (mapValue instanceof  Map){
                temp = (Map) mapValue;
            }
        }catch (Exception e){
                throw  new UserException("It is not possible to extract Authentication details.");
        }
        String userIdentifier = temp.get("userIdentifier");
        try {
            OAuth2RestOperations restTemplate = this.restTemplate;
            if (restTemplate == null) {
                BaseOAuth2ProtectedResourceDetails resource = new BaseOAuth2ProtectedResourceDetails();
                resource.setAuthenticationScheme(AuthenticationScheme.query);
                restTemplate = new OAuth2RestTemplate(resource);
            }
            OAuth2AccessToken existingToken = restTemplate.getOAuth2ClientContext()
                    .getAccessToken();
            if (existingToken == null || !accessToken.equals(existingToken.getValue())) {
                DefaultOAuth2AccessToken token = new DefaultOAuth2AccessToken(
                        accessToken);
                token.setTokenType(this.tokenType);
                restTemplate.getOAuth2ClientContext().setAccessToken(token);
            }
            if (path.contains("api.linkedin.")){
                path = path +accessToken;
            }
            ResponseEntity responseEntity = restTemplate.getForEntity(path, JSONObject.class);
            Map responseDetails = new HashMap();
            if (responseEntity.getStatusCode().is2xxSuccessful()){
                JSONObject userInformation = (JSONObject) responseEntity.getBody();
                String information = (String) userInformation.get(userIdentifier);
                setUserName(information);
                responseDetails.put("details", userInformation);
                AuthenticationOAuth2DAOImpl.authenticated = true;
            }else{
                responseDetails.put("error","Status code "+responseEntity.getStatusCode());
            }
            return responseDetails;
        }
        catch (Exception ex) {
            this.logger.info("Could not fetch user details: " + ex.getClass() + ", "
                    + ex.getMessage());
            return Collections.<String, Object>singletonMap("error",
                    "Could not fetch user details");
        }
    }


    private OAuth2Authentication extractAuthentication(Map<String, Object> map) {
        Object principal = null;
        List<GrantedAuthority> authorities = new LinkedList<>();
        OAuth2Request request = new OAuth2Request(null, this.clientId, null, true, null,
                null, null, null, null);
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                principal, "N/A", authorities);
        token.setDetails(map);
        return new OAuth2Authentication(request, token);
    }

    public static String getUserName() {
        return userName;
    }

    public static void  setUserName(String username) {
        userName = username;
    }
}
