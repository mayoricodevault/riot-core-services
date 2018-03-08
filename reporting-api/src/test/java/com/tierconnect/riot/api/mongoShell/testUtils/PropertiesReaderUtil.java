package com.tierconnect.riot.api.mongoShell.testUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by achambi on 11/25/16.
 * Class utility to load test properties file.
 */
public class PropertiesReaderUtil {


    public static void setConfigurationFile(String propertyFileName) throws IOException {
        InputStream input = null;
        try {
            input = PropertiesReaderUtil.class.getClassLoader().getResourceAsStream(propertyFileName);
            if (input == null) {
                System.out.println("Sorry, unable to find " + propertyFileName);
                return;
            }
            Properties prop = System.getProperties();
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
    }
}
