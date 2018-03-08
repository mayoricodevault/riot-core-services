package com.tierconnect.riot.iot.utils;


import com.tierconnect.riot.appcore.utils.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by achambi on 11/25/16.
 * Class utility to load test properties file.
 */
public class PropReaderUtil {

    public static Properties getCustomConFile(String propertyFileName) throws IOException {
        InputStream input = null;
        Properties prop = null;
        try {
            input = PropReaderUtil.class.getClassLoader().getResourceAsStream(propertyFileName);
            if (input == null) {
                System.out.println("Error loading the configuration file: " + propertyFileName);
                throw new IOException("Error loading the configuration file: " + propertyFileName);
            }
            prop = System.getProperties();
            prop.load(input);
            System.setProperties(prop);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return prop;
    }
}
