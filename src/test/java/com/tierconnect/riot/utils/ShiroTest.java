package com.tierconnect.riot.utils;

import org.apache.shiro.SecurityUtils;
import org.hibernate.Transaction;

import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.sdk.servlet.security.ApiKeyToken;

import org.apache.shiro.authc.*;
import org.apache.shiro.config.IniSecurityManagerFactory;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.Factory;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ShiroTest {

//    private static final transient Logger log = LoggerFactory.getLogger(ShiroTest.class);
//
//    @Test
//    public void testBasicPermissioning() throws Exception {
//        Transaction transaction = GroupService.getInstance().getGroupDAO().getSession().getTransaction();
//        transaction.begin();
//        RiotShiroTestRealm.setResultPermissions("user");
//        Factory<SecurityManager> factory = new IniSecurityManagerFactory("classpath:shiro_riot_test.ini");
//        SecurityManager securityManager = factory.getInstance();
//        SecurityUtils.setSecurityManager(securityManager);
//        Subject currentUser = SecurityUtils.getSubject();
//        ApiKeyToken token = new ApiKeyToken("root");
//        currentUser.login(token);
//        Assert.assertTrue(currentUser.isPermitted("user"));
//        Assert.assertTrue(currentUser.isPermitted("user:r"));
//        Assert.assertTrue(currentUser.isPermitted("user:x"));
//        Assert.assertTrue(currentUser.isPermitted("user:z"));
//        Assert.assertTrue(currentUser.isPermitted("user::123"));
//        Assert.assertTrue(currentUser.isPermitted("user:*:123"));
//        Assert.assertTrue(currentUser.isPermitted("user:z:123"));
//        Assert.assertFalse(currentUser.isPermitted("abc"));
//        Assert.assertFalse(currentUser.isPermitted("abc:r"));
//        Assert.assertFalse(currentUser.isPermitted("abc:w"));
//        Assert.assertFalse(currentUser.isPermitted("abc::123"));
//        Assert.assertFalse(currentUser.isPermitted("abc:*:123"));
//        Assert.assertFalse(currentUser.isPermitted("abc:r:123"));
//        transaction.commit();
//    }
//
//    @Test
//    public void testClassPermissioning() throws Exception {
//        Transaction transaction = GroupService.getInstance().getGroupDAO().getSession().getTransaction();
//        transaction.begin();
//        RiotShiroTestRealm.setResultPermissions("user:r");
//        Factory<SecurityManager> factory = new IniSecurityManagerFactory("classpath:shiro_riot_test.ini");
//        SecurityManager securityManager = factory.getInstance();
//        SecurityUtils.setSecurityManager(securityManager);
//        Subject currentUser = SecurityUtils.getSubject();
//        ApiKeyToken token = new ApiKeyToken("root");
//        currentUser.login(token);
//        Assert.assertTrue(currentUser.isPermitted("user:r"));
//        Assert.assertTrue(currentUser.isPermitted("user:r:123"));
//        Assert.assertFalse(currentUser.isPermitted("user"));
//        Assert.assertFalse(currentUser.isPermitted("user:x"));
//        Assert.assertFalse(currentUser.isPermitted("user::123"));
//        Assert.assertFalse(currentUser.isPermitted("user:*:123"));
//
//        Assert.assertFalse(currentUser.isPermitted("abc"));
//        Assert.assertFalse(currentUser.isPermitted("abc:r"));
//        Assert.assertFalse(currentUser.isPermitted("abc:w"));
//        Assert.assertFalse(currentUser.isPermitted("abc::123"));
//        Assert.assertFalse(currentUser.isPermitted("abc:*:123"));
//        Assert.assertFalse(currentUser.isPermitted("abc:r:123"));
//
//        transaction.commit();
//    }
//
//    @Test
//    public void testPropertyPermissioning() throws Exception {
//        Transaction transaction = GroupService.getInstance().getGroupDAO().getSession().getTransaction();
//        transaction.begin();
//
//        RiotShiroTestRealm.setResultPermissions("user.username");
//        Factory<SecurityManager> factory = new IniSecurityManagerFactory("classpath:shiro_riot_test.ini");
//        SecurityManager securityManager = factory.getInstance();
//        SecurityUtils.setSecurityManager(securityManager);
//        Subject currentUser = SecurityUtils.getSubject();
//        ApiKeyToken token = new ApiKeyToken("root");
//        currentUser.login(token);
//        Assert.assertTrue(currentUser.isPermitted("user.username"));
//        Assert.assertTrue(currentUser.isPermitted("user.username:r"));
//        Assert.assertTrue(currentUser.isPermitted("user.username:w"));
//        Assert.assertTrue(currentUser.isPermitted("user.username::123"));
//        Assert.assertTrue(currentUser.isPermitted("user.username:*:123"));
//        Assert.assertTrue(currentUser.isPermitted("user.username:z:123"));
//
//        Assert.assertFalse(currentUser.isPermitted("user.lastName"));
//        Assert.assertFalse(currentUser.isPermitted("user.lastName:r"));
//        Assert.assertFalse(currentUser.isPermitted("user.lastName:w"));
//        Assert.assertFalse(currentUser.isPermitted("user.lastName::123"));
//        Assert.assertFalse(currentUser.isPermitted("user.lastName:*:123"));
//        Assert.assertFalse(currentUser.isPermitted("user.lastName:r:123"));
//
//        Assert.assertFalse(currentUser.isPermitted("user"));
//        Assert.assertFalse(currentUser.isPermitted("user:r"));
//        Assert.assertFalse(currentUser.isPermitted("user:x"));
//        Assert.assertFalse(currentUser.isPermitted("user:z"));
//        Assert.assertFalse(currentUser.isPermitted("user::123"));
//        Assert.assertFalse(currentUser.isPermitted("user:*:123"));
//        Assert.assertFalse(currentUser.isPermitted("user:z:123"));
//
//        RiotShiroTestRealm.setResultPermissions("user.username:r");
//        factory = new IniSecurityManagerFactory("classpath:shiro_riot_test.ini");
//        securityManager = factory.getInstance();
//        SecurityUtils.setSecurityManager(securityManager);
//        currentUser = SecurityUtils.getSubject();
//        token = new ApiKeyToken("root");
//        currentUser.login(token);
//        Assert.assertFalse(currentUser.isPermitted("user.username"));
//        Assert.assertTrue(currentUser.isPermitted("user.username:r"));
//        Assert.assertFalse(currentUser.isPermitted("user.username:w"));
//        Assert.assertFalse(currentUser.isPermitted("user.username::123"));
//        Assert.assertFalse(currentUser.isPermitted("user.username:*:123"));
//        Assert.assertTrue(currentUser.isPermitted("user.username:r:123"));
//
//        Assert.assertFalse(currentUser.isPermitted("user.lastName"));
//        Assert.assertFalse(currentUser.isPermitted("user.lastName:r"));
//        Assert.assertFalse(currentUser.isPermitted("user.lastName:w"));
//        Assert.assertFalse(currentUser.isPermitted("user.lastName::123"));
//        Assert.assertFalse(currentUser.isPermitted("user.lastName:*:123"));
//        Assert.assertFalse(currentUser.isPermitted("user.lastName:r:123"));
//
//        Assert.assertFalse(currentUser.isPermitted("user"));
//        Assert.assertFalse(currentUser.isPermitted("user:r"));
//        Assert.assertFalse(currentUser.isPermitted("user:x"));
//        Assert.assertFalse(currentUser.isPermitted("user:z"));
//        Assert.assertFalse(currentUser.isPermitted("user::123"));
//        Assert.assertFalse(currentUser.isPermitted("user:*:123"));
//        Assert.assertFalse(currentUser.isPermitted("user:z:123"));
//
//        transaction.commit();
//    }
}
