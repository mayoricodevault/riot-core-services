package com.tierconnect.riot.api.configuration;

import org.apache.commons.lang.StringUtils;

/**
 * Created by achambi on 11/29/16.
 * Class to get a system property value.
 */
public class PropertyReader {

    private PropertyReader(){}

    /**
     * Searches for the property with the specified key in this property list.
     * If the key is not found in System property list, the default property list,
     * and its defaults, recursively, are then checked. The method returns the
     * default value argument if the property is not found or is blank.
     *
     * @param key          the hashtable key.
     * @param defaultValue a default value.
     * @param discardBlankValues a flag to discart or not blank values of the property to get.
     * @return the value in this property list with the specified key value.
     */
    public static String getProperty(String key, String defaultValue, boolean discardBlankValues) {
        String val = System.getProperty(key);
        if(discardBlankValues)
            return (StringUtils.isBlank(val)) ? defaultValue : val;
        else
            return (val == null) ? defaultValue : val;
    }
}
