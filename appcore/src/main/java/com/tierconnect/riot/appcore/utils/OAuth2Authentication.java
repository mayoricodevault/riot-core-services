package com.tierconnect.riot.appcore.utils;

import com.tierconnect.riot.appcore.dao.AuthenticationOAuth2DAOImpl;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.services.UserService;
import com.tierconnect.riot.sdk.dao.UserException;
import org.jose4j.json.internal.json_simple.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Created by aruiz on 6/26/17.
 */
public class OAuth2Authentication implements Authentication {

    HttpServletRequest request;
    AuthenticationOAuth2DAOImpl authenticationOAuth2DAO;
    String userName;

    public OAuth2Authentication(HttpServletRequest request) {
        this.request = request;
        this.authenticationOAuth2DAO = new AuthenticationOAuth2DAOImpl(getToken());
        this.authenticationOAuth2DAO.setTokenValue(getToken());
    }

    @Override
    public boolean authenticateUser(String username, String password, Group group) {
        boolean authenticate = authenticationOAuth2DAO.authenticate(username,password,request);
        if (username == null) {
            setUserName(authenticationOAuth2DAO.getUsername());
        }
        return authenticate;
    }

    @Override
    public void setContextSource(Map<String, String> properties) {
        authenticationOAuth2DAO.setContext(properties);

    }
    private String getToken(){
        String tokenValue = request.getHeader("oauth2token");
        if (tokenValue == null){
            Map authenticationData = UserService.authenticationMode();
            authenticationData.put("message","OAuth authentication required.");
            String authenticationAsJSon = JSONObject.toJSONString(authenticationData);
            throw  new UserException(authenticationAsJSon,401);
        }
        return tokenValue;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
