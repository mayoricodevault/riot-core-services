package com.tierconnect.riot.appcore.dao;

import com.tierconnect.riot.sdk.dao.UserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.filter.OAuth2ClientAuthenticationProcessingFilter;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.common.AuthenticationScheme;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.Map;

/**
 * Created by aruiz on 6/26/17.
 */
public class AuthenticationOAuth2DAOImpl implements AuthenticationOAuth2DAO {
    OAuth2ClientAuthenticationProcessingFilter oAuth2ClientAuthenticationProcessingFilter;
    OAuth2AccessToken accessToken;
    static boolean authenticated = false;

    String username;

    @Autowired
    private OAuth2ClientContext oauth2ClientContext;

    public String getTokenValue() {
        return tokenValue;
    }

    public void setTokenValue(String tokenValue) {
        this.tokenValue = tokenValue;
    }

    public AuthenticationOAuth2DAOImpl(String token) {
        this.tokenValue = token;
        this.accessToken = new DefaultOAuth2AccessToken(getTokenValue());
        this.oauth2ClientContext = new DefaultOAuth2ClientContext(accessToken);
    }

    private String tokenValue;

    @Override
    public void setContext(Map<String, String> contextProperties) {
        oAuth2ClientAuthenticationProcessingFilter = oauth2ClientAuthenticationProcessingFilter(contextProperties, googleTokenServices(contextProperties.get("clientId"),contextProperties.get("userInfoUri")));
    }

    @Override
    public boolean authenticate(String userDn, String credentials, HttpServletRequest request) {

        try {
            org.springframework.security.core.Authentication authentication = oAuth2ClientAuthenticationProcessingFilter.attemptAuthentication(request, null);
            setUsername( ResourceServerTokenServicesImpl.getUserName());

        }catch (ServletException |AuthenticationException | InvalidTokenException |IOException e){
            throw  new UserException("It is not possible to authenticate because " + e.getMessage());
        }
        return  authenticated;
    }

    private OAuth2ClientAuthenticationProcessingFilter oauth2ClientAuthenticationProcessingFilter(Map<String, String> properties, ResourceServerTokenServices googleTokenService) {
        OAuth2RestOperations restTemplate = new OAuth2RestTemplate(
                authorizationCodeResource(properties),
                oauth2ClientContext);
        OAuth2ClientAuthenticationProcessingFilter filter =
                new OAuth2ClientAuthenticationProcessingFilter("/Home");
        filter.setRestTemplate(restTemplate);
        filter.setTokenServices(googleTokenService);
        return filter;
    }


    private OAuth2ProtectedResourceDetails authorizationCodeResource(Map<String, String> properties) {

            AuthorizationCodeResourceDetails details = new AuthorizationCodeResourceDetails();
            details.setId(properties.get("clientName"));
            details.setClientId(properties.get("clientId"));
            details.setClientSecret(properties.get("clientSecret"));
            details.setUserAuthorizationUri(properties.get("userAuthUri"));
            details.setAccessTokenUri(properties.get("accessTokenUri"));
            details.setTokenName(properties.get("tokenName"));
            details.setAuthenticationScheme(AuthenticationScheme.query);
            details.setClientAuthenticationScheme(AuthenticationScheme.form);
            return details;
    }

    private ResourceServerTokenServicesImpl googleTokenServices(String clientId, String userInfoUri) {
        ResourceServerTokenServicesImpl userInfoTokenServices =
                new ResourceServerTokenServicesImpl(userInfoUri, clientId);
        return userInfoTokenServices;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

}
