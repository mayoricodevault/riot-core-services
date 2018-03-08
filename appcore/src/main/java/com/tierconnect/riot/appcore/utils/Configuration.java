package com.tierconnect.riot.appcore.utils;

import org.apache.log4j.Logger;

import javax.servlet.ServletContext;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by agutierrez on 1/29/15.
 */
//TODO: ADLO: these classes are still AFU - see Configuration, AppcoreConfigHelper, AppcoreContextListener
public class Configuration {

    private static final Logger logger = Logger.getLogger( Configuration.class );

    private static ConcurrentHashMap<String, String> properties = null;

    private static final String CONFIGURATION_FILE_PATH = "conf.properties";

    static {
        properties = new ConcurrentHashMap<>();
        logEnvironmentVariablesDefined();
        loadDefaultConfig();
    }

    public static synchronized void init(ServletContext sc) {
        loadFromContextAndEnvironment(sc);
        setConfigParametersAsJavaProperties();
    }

    private static final void logEnvironmentVariablesDefined() {
        Map<String, String> environmentVariables = System.getenv();
        environmentVariables.keySet()
                .stream()
                .forEach(var -> logger.info("Environment variable defined: " + var));
    }

    private static void loadDefaultConfig() {
        Properties prop = new Properties();
        try
        {
            prop.load( new FileInputStream(CONFIGURATION_FILE_PATH) );
            logger.info( "Reading from file: " + CONFIGURATION_FILE_PATH);
        }
        catch( Exception e )
        {
            try
            {
                prop.load( Configuration.class.getClassLoader().getResourceAsStream(CONFIGURATION_FILE_PATH) );
                logger.info( "Reading from classpath: " + CONFIGURATION_FILE_PATH);
            }
            catch( IOException e1 )
            {
                throw new RuntimeException( e );
            }
        }
        for (Map.Entry<Object, Object> entry:  prop.entrySet()) {
            if (entry.getValue() != null) {
                properties.put(entry.getKey().toString(), entry.getValue().toString());
                logger.info("Reading Property '" + entry.getKey().toString() + "' from file " + CONFIGURATION_FILE_PATH);
            }
        }
    }

    private static void loadFromContextAndEnvironment(ServletContext sc) {
        if (sc != null) {
            Enumeration<String> en = sc.getInitParameterNames();
            while (en.hasMoreElements()) {
                String name = en.nextElement();
                properties.put(name, sc.getInitParameter(name));
                logger.info("Reading Property '" + name + "' from context file");
            }
        }
        for (Map.Entry<Object, Object> entry:  System.getProperties().entrySet()) {
            if (entry.getValue() != null) {
                properties.put(entry.getKey().toString(), entry.getValue().toString());
                logger.info("Reading Property '" + entry.getKey().toString() + "' from environment variable");
            }
        }
    }

    private static void setConfigParametersAsJavaProperties() {
        for (Map.Entry<String,String> entry: properties.entrySet()) {
            if (entry.getKey().startsWith("hibernate") && !entry.getKey().startsWith("hibernate.cache")) {
                System.getProperties().put(entry.getKey(), entry.getValue());
            }
            if (entry.getKey().startsWith("hazelcast")) {
                System.getProperties().put(entry.getKey(), entry.getValue());
            }
            if (properties.containsKey("vizix.hazelcast.distributed.enable") && Boolean.valueOf(properties.get("vizix.hazelcast.distributed.enable"))){
                if (entry.getKey().startsWith("hibernate.cache")) {
                    System.getProperties().put(entry.getKey(), entry.getValue());
                }
            }
            if(entry.getKey().startsWith("mongo.")){
                System.getProperties().put(entry.getKey(), entry.getValue());
            }
        }
    }

    public static String getProperty(String property) {
        return properties.get(property);
    }

    public static Map<String,String> getHibernateProperties() {
        Map<String,String> result = new HashMap<>();
        for (Map.Entry<String,String> entry: properties.entrySet()) {
            if (entry.getKey().startsWith("hibernate")) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * Only for Test
     * @param prop Properties from test configuration file.
     */
    public static void setConfigurationFilePath(Properties prop) {
        for (Map.Entry<Object, Object> entry : prop.entrySet()) {
            if (entry.getValue() != null) {
                properties.put(entry.getKey().toString(), entry.getValue().toString());
                logger.info("Reading Property '" + entry.getKey().toString() + "' from custom file.");
            }
        }
    }
}
