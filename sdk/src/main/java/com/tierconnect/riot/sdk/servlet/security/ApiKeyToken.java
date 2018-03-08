package com.tierconnect.riot.sdk.servlet.security;

import org.apache.shiro.authc.AuthenticationToken;

/**
 * Created by agutierrez on 17-04-14.
 */
public class ApiKeyToken implements AuthenticationToken {


	private static final long serialVersionUID = 1L;

	private String apiKey;

    public ApiKeyToken(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public Object getPrincipal() {
        return apiKey;
    }

    @Override
    public Object getCredentials() {
        return apiKey;
    }

    public String getApiKey() {
        return apiKey;
    }

}
