package com.tierconnect.riot.iot.entities;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Created by pablo on 11/10/15.
 * <p>
 * Utils class that has methods common to ReportProperty, ReportRule, ReportFilter, etc.
 * These classes do no have a common parent class (not sure how to add it using code generation).  So, in order
 * to not repeat code, the common methods in these classes call this util class.
 * <p>
 * TODO make ReportProperty, ReportRule, ReportFilter, etc children of a common parent class
 */
public class ReportDefinitionUtils {
    public static final String[] NON_UDF_FIELDS = new String[]{"name", "serial", "group", "thingType", "parent"};
    public static final String DWELL_TIME = "dwellTime(";
    public static final String TIMESTAMP = "timeStamp(";


    public static boolean isNative(String property) {
        String[] tokens = StringUtils.split(property, ".");
        return ArrayUtils.contains(NON_UDF_FIELDS, tokens[0]);
    }

    public static boolean isDwell(String property) {
        return StringUtils.remove(property, " ").startsWith(DWELL_TIME);
    }

    public static boolean isTimestamp(String property) {
        return StringUtils.remove(property, " ").startsWith(TIMESTAMP);
    }

    public static String stripDwell(String in) {
        String out = StringUtils.removeStart(StringUtils.remove(in, " "), DWELL_TIME);
        out = StringUtils.removeEnd(out, ")");
        out = StringUtils.trim(out);
        return out;
    }

    public static String stripTimestamp(String in) {
        String out = StringUtils.removeStart(StringUtils.remove(in, " "), TIMESTAMP);
        out = StringUtils.removeEnd(out, ")");
        out = StringUtils.trim(out);
        return out;
    }

    public static boolean isThingTypeUdf(ThingType tt) {
        return tt != null && tt.getId() != null;
    }

}
