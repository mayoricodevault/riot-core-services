package com.tierconnect.riot.appcore.dao;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Created by aruiz on 6/26/17.
 */
public interface AuthenticationOAuth2DAO {
    void setContext (Map<String,String> contextProperties);
    boolean authenticate(String userDn, String credentials,HttpServletRequest request);
}
