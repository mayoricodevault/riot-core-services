package com.tierconnect.riot.core.test;

import com.tierconnect.riot.appcore.controllers.RiotShiroRealm;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.popdb.PopDBRequired;
import com.tierconnect.riot.appcore.services.UserService;
import com.tierconnect.riot.commons.dao.mongo.MongoDAOUtil;
import com.tierconnect.riot.sdk.dao.HibernateDAOUtils;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;
import com.tierconnect.riot.sdk.servlet.security.ApiKeyToken;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.config.IniSecurityManagerFactory;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.Factory;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import java.net.UnknownHostException;
import java.util.Properties;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.math.NumberUtils.isNumber;

/**
 * Created by vealaro on 9/23/16.
 * Modified by julio.rocha on 27-07-17.
 */
public abstract class BaseTest {

    private static Logger logger = Logger.getLogger(BaseTest.class);

    public static Properties propertiesMongo = new Properties();
    public static Properties propertiesHibernate = new Properties();

    public User userRoot;
    public Subject currentUser;

    static {
        logger.info("write properties for mongo");
        propertiesMongo.put("mongo.primary", "127.0.0.1:27017");
        propertiesMongo.put("mongo.secondary", "");
        propertiesMongo.put("mongo.replicaset", "");
        propertiesMongo.put("mongo.ssl", "false");
        propertiesMongo.put("mongo.username", "admin");
        propertiesMongo.put("mongo.password", "control123!");
        propertiesMongo.put("mongo.authdb", "admin");
        propertiesMongo.put("mongo.db", "riot_main");
        propertiesMongo.put("mongo.sharding", "false");
        propertiesMongo.put("mongo.connectiontimeout", "0");
        propertiesMongo.put("mongo.maxpoolsize", "0");


        logger.info("write properties for hibernate");
        propertiesHibernate.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        propertiesHibernate.put("hibernate.connection.username", "root");
        propertiesHibernate.put("hibernate.connection.url", "jdbc:mysql://localhost:3306/riot_main");
        propertiesHibernate.put("hibernate.connection.driver_class", "org.gjt.mm.mysql.Driver");
        propertiesHibernate.put("hibernate.connection.password", "control123!");
    }

    @BeforeClass
    public static void initDriver() {
        PopDBRequired.initJDBCDrivers();
        initMongo();
        initHibernate();
    }

    @Before
    public void initCallBase() throws Exception {
        initNoTransactionalConfiguration();
        initTransaction();
        initShiro();
        previousConfiguration();
    }

    @After
    public void endTransaction() {
        Session session = HibernateSessionFactory.getInstance().getCurrentSession();
        Transaction transaction = session.getTransaction();
        try {
            if (transaction.isActive()) {
                transaction.commit();
            }
        } catch (Exception ex) {
            logger.error("Error in End call hibernate");
            try {
                HibernateDAOUtils.rollback(transaction);
            } catch (Exception e) {
                logger.error("Error in rollback Hibernate transaction ", e);
            }
        }
    }

    protected void rollback() {
        Session session = HibernateSessionFactory.getInstance().getCurrentSession();
        Transaction transaction = session.getTransaction();
        try {
            HibernateDAOUtils.rollback(transaction);
        } catch (Exception e) {
            logger.error("Error in rollback Hibernate transaction ", e);
        }
    }

    @AfterClass
    public static void removeDriver() {
        HibernateSessionFactory.closeInstance();
        PopDBRequired.closeJDBCDrivers();
    }

    protected void initShiro() {
        Factory<SecurityManager> factory = new IniSecurityManagerFactory("classpath:shiro_riot_test.ini");
        SecurityManager securityManager = factory.getInstance();
        SecurityUtils.setSecurityManager(securityManager);

        currentUser = SecurityUtils.getSubject();
        RiotShiroRealm.initCaches();
        userRoot = UserService.getInstance().getRootUser();
        ApiKeyToken token = new ApiKeyToken(userRoot.getApiKey());
        currentUser.login(token);
    }

    protected void initNoTransactionalConfiguration() {

    }

    protected void previousConfiguration() throws Exception {

    }

    private static void initHibernate() {
        logger.info("Create new SesionFactory for test");
        Configuration configuration = new Configuration();
        configuration.addProperties(propertiesHibernate);
        configuration.configure();
        ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().applySettings(configuration.getProperties()).build();
        HibernateSessionFactory.setInstanceForTest(configuration.buildSessionFactory(serviceRegistry));
        logger.debug("init hibernate");
    }

    public void initTransaction() {
        Session session = HibernateSessionFactory.getInstance().getCurrentSession();
        Transaction transaction = session.getTransaction();
        try {
            if (!transaction.isActive()) {
                transaction.begin();
            }
        } catch (Exception ex) {
            logger.error("Error init call hibernate for test", ex);
        }
    }

    public static String getPropertyMongo(String key) {
        return (String) propertiesMongo.get(key);
    }

    public static void initMongo() {
        try {
            MongoDAOUtil.setupMongodb(getPropertyMongo("mongo.primary"),
                    getPropertyMongo("mongo.secondary"),
                    getPropertyMongo("mongo.replicaset"),
                    Boolean.valueOf(getPropertyMongo("mongo.ssl")),
                    getPropertyMongo("mongo.username"),
                    getPropertyMongo("mongo.password"),
                    getPropertyMongo("mongo.authdb"),
                    getPropertyMongo("mongo.db"),
                    getPropertyMongo("mongo.controlReadPreference"),
                    getPropertyMongo("mongo.reportsReadPreference"),
                    Boolean.valueOf(getPropertyMongo("mongo.sharding")),
                    (StringUtils.isNotBlank(getPropertyMongo("mongo.connectiontimeout"))
                            && isNumber(getPropertyMongo("mongo.connectiontimeout"))) ? Integer.parseInt(getPropertyMongo
                            ("mongo.connectiontimeout")) : null,
                    (isNotBlank(getPropertyMongo("mongo.maxpoolsize"))
                            && isNumber(getPropertyMongo
                            ("mongo.maxpoolsize"))) ? Integer.parseInt(getPropertyMongo("mongo.maxpoolsize"))
                            : null);
        } catch (UnknownHostException e) {
            logger.info("Error connection to Mongo", e);
        }
    }
}
