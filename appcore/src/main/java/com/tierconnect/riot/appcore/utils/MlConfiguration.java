package com.tierconnect.riot.appcore.utils;

import com.tierconnect.riot.appcore.entities.Connection;
import com.tierconnect.riot.appcore.services.ConnectionService;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Note: This class in located in this package com.tierconnect.riot.appcore.utils
 * because otherwise it wasn't possible to use it in MlModel class (which is an
 * entity class).
 *
 * Created by alfredo on 11/30/16.
 */
public class MlConfiguration {

    private final static Logger logger = Logger.getLogger(MlConfiguration.class);
    public final static String ANALYTICS_CONNECTION_CODE = "ANALYTICS";
    public static final Map<String, String> CONF_TO_CONNECTION_PROPERTIES = new HashMap<>();
    static {
        CONF_TO_CONNECTION_PROPERTIES.put("mongo.password", "password");
    }


    public static String property(String p) {

        String value;

        // First try using connection services...
        Connection conn = ConnectionService.getInstance().getByCode(ANALYTICS_CONNECTION_CODE);
        String propertyConn = CONF_TO_CONNECTION_PROPERTIES.containsKey(p) ? CONF_TO_CONNECTION_PROPERTIES.get(p) : p;
        if ((conn != null) && (conn.getProperty(propertyConn) != null)) {
            if (propertyConn.equals("password")) {
                value = conn.getPassword(false);
            }
            else {
                value = (String) conn.getProperty(propertyConn);
            }
            logger.info("Property " + p + " has been read from analytics connection");
        }
        // And then try using conf.properties...
        else {
            value = Configuration.getProperty(p);
            logger.info("Property " + p + " has been read from conf.properties");
        }

        return value;
    }
}
