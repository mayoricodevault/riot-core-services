package com.tierconnect.riot.appcore.utils;

import com.tierconnect.riot.appcore.dao.AuthenticationLdapDAO;
import com.tierconnect.riot.appcore.entities.Group;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.util.Map;

/**
 * Created by fflores on 9/1/16.
 */
public class LdapAuthentication implements Authentication {

    Resource resource = new ClassPathResource("SpringContext.xml");
    BeanFactory factory = new XmlBeanFactory(resource);
    AuthenticationLdapDAO ldapAuth=(AuthenticationLdapDAO) factory.getBean("authenticationDAOImpl");
    String userIdentifier;

    @Override
    public boolean authenticateUser(String username, String password, Group group) {
        return ldapAuth.authenticate(username,password,userIdentifier);
    }

    @Override
    public void setContextSource(Map<String, String> contextProperties) {
        ldapAuth.setContext(contextProperties);
    }

    public boolean validateUser(String username){
        return ldapAuth.userExists(username,userIdentifier);
    }

    public String getUserIdentifier() {
        return userIdentifier;
    }

    public void setUserIdentifier(String userIdentifier) {
        this.userIdentifier = userIdentifier;
    }

    public  boolean testConnection(String username, String password){
        return ldapAuth.testConnection(username, password);
    }
}
