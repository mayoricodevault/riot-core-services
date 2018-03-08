package com.tierconnect.riot.appcore.dao;

import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 *
 * @author : aruiz
 * @date : 8/29/16 4:21 PM
 * @version:
 */
public interface AuthenticationLdapDAO {

    boolean authenticate(String userDn, String credentials,String userIdentifier);
    void setContext (Map<String,String> contextProperties);
    boolean userExists(String userDn,String userIdentifier);
    boolean testConnection(String username, String password);

}
