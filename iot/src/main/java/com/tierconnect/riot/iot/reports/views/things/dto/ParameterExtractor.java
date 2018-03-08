package com.tierconnect.riot.iot.reports.views.things.dto;

import org.apache.log4j.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by julio.rocha on 07-07-17.
 */
class ParameterExtractor {
    private static Logger logger = Logger.getLogger(ParameterExtractor.class);

    static String getValueFromWhereParam(String where, String textPattern, String parameter) {
        String value = null;
        if (where.contains(parameter)) {
            try {
                Pattern pattern = Pattern.compile(textPattern);
                Matcher matcher = pattern.matcher(where);
                value = matcher.find() ? matcher.group(1) : null;
            } catch (Exception e) {
                logger.warn("Exception in parameter extraction:" +
                        " where = " + where +
                        " textPattern = " + textPattern +
                        " parameter = " + parameter, e);
            }
        }
        return value;
    }
}
