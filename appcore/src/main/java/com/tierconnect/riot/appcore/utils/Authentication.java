package com.tierconnect.riot.appcore.utils;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.User;

import java.util.Map;

/**
 * Created by fflores on 9/1/16.
 */
public interface Authentication {

    public boolean authenticateUser(String username, String password, Group group);
    public void setContextSource(Map<String, String> properties);

}
