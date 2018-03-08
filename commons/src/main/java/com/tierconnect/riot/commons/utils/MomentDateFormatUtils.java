package com.tierconnect.riot.commons.utils;

import org.apache.log4j.Logger;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by vealaro on 4/18/17.
 */
public class MomentDateFormatUtils {

    private static final Map<String, String> JAVA_FORMAT_MAPPING = new HashMap<>();
    private static final Map<String, String> MOMENT_FORMAT_MAPPING = new HashMap<>();
    private static Logger logger = Logger.getLogger(MomentDateFormatUtils.class);
    public static Invocable invocable;

    static {
        JAVA_FORMAT_MAPPING.put("d", "D");
        JAVA_FORMAT_MAPPING.put("dd", "DD");
        JAVA_FORMAT_MAPPING.put("y", "YYYY");
        JAVA_FORMAT_MAPPING.put("yy", "YY");
        JAVA_FORMAT_MAPPING.put("yyy", "YYYY");
        JAVA_FORMAT_MAPPING.put("yyyy", "YYYY");
        JAVA_FORMAT_MAPPING.put("a", "a");
        JAVA_FORMAT_MAPPING.put("A", "A");
        JAVA_FORMAT_MAPPING.put("M", "M");
        JAVA_FORMAT_MAPPING.put("MM", "MM");
        JAVA_FORMAT_MAPPING.put("MMM", "MMM");
        JAVA_FORMAT_MAPPING.put("MMMM", "MMMM");
        JAVA_FORMAT_MAPPING.put("h", "h");
        JAVA_FORMAT_MAPPING.put("hh", "hh");
        JAVA_FORMAT_MAPPING.put("H", "H");
        JAVA_FORMAT_MAPPING.put("HH", "HH");
        JAVA_FORMAT_MAPPING.put("m", "m");
        JAVA_FORMAT_MAPPING.put("mm", "mm");
        JAVA_FORMAT_MAPPING.put("s", "s");
        JAVA_FORMAT_MAPPING.put("ss", "ss");
        JAVA_FORMAT_MAPPING.put("S", "SSS");
        JAVA_FORMAT_MAPPING.put("SS", "SSS");
        JAVA_FORMAT_MAPPING.put("SSS", "SSS");
        JAVA_FORMAT_MAPPING.put("E", "ddd");
        JAVA_FORMAT_MAPPING.put("EE", "ddd");
        JAVA_FORMAT_MAPPING.put("EEE", "ddd");
        JAVA_FORMAT_MAPPING.put("EEEE", "dddd");
        JAVA_FORMAT_MAPPING.put("EEEEE", "dddd");
        JAVA_FORMAT_MAPPING.put("EEEEEE", "dddd");
        JAVA_FORMAT_MAPPING.put("D", "DDD");
        JAVA_FORMAT_MAPPING.put("w", "W");
        JAVA_FORMAT_MAPPING.put("ww", "WW");
        JAVA_FORMAT_MAPPING.put("z", "ZZ");
        JAVA_FORMAT_MAPPING.put("zzzz", "Z");
        JAVA_FORMAT_MAPPING.put("Z", "ZZ");
        JAVA_FORMAT_MAPPING.put("X", "ZZ");
        JAVA_FORMAT_MAPPING.put("XX", "ZZ");
        JAVA_FORMAT_MAPPING.put("XXX", "Z");
        JAVA_FORMAT_MAPPING.put("u", "E");

        MOMENT_FORMAT_MAPPING.put("D", "d");
        MOMENT_FORMAT_MAPPING.put("DD", "dd");
        MOMENT_FORMAT_MAPPING.put("YY", "yy");
        MOMENT_FORMAT_MAPPING.put("YYY", "yyyy");
        MOMENT_FORMAT_MAPPING.put("YYYY", "yyyy");
        MOMENT_FORMAT_MAPPING.put("a", "a");
        MOMENT_FORMAT_MAPPING.put("A", "a");
        MOMENT_FORMAT_MAPPING.put("M", "M");
        MOMENT_FORMAT_MAPPING.put("MM", "MM");
        MOMENT_FORMAT_MAPPING.put("MMM", "MMM");
        MOMENT_FORMAT_MAPPING.put("MMMM", "MMMM");
        MOMENT_FORMAT_MAPPING.put("h", "h");
        MOMENT_FORMAT_MAPPING.put("hh", "hh");
        MOMENT_FORMAT_MAPPING.put("H", "H");
        MOMENT_FORMAT_MAPPING.put("HH", "HH");
        MOMENT_FORMAT_MAPPING.put("m", "m");
        MOMENT_FORMAT_MAPPING.put("mm", "mm");
        MOMENT_FORMAT_MAPPING.put("s", "s");
        MOMENT_FORMAT_MAPPING.put("ss", "ss");
        MOMENT_FORMAT_MAPPING.put("S", "S");
        MOMENT_FORMAT_MAPPING.put("SS", "S");
        MOMENT_FORMAT_MAPPING.put("SSS", "S");
        MOMENT_FORMAT_MAPPING.put("ddd", "E");
        MOMENT_FORMAT_MAPPING.put("dddd", "EEEE");
        MOMENT_FORMAT_MAPPING.put("DDD", "D");
        MOMENT_FORMAT_MAPPING.put("W", "w");
        MOMENT_FORMAT_MAPPING.put("WW", "ww");
        MOMENT_FORMAT_MAPPING.put("ZZ", "z");
        MOMENT_FORMAT_MAPPING.put("Z", "XXX");
        MOMENT_FORMAT_MAPPING.put("E", "u");
    }

    public static void initCompileMomentJS() {
        try {
            ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
            engine.eval(getFunctionWithMoment(MomentDateFormatUtils.class.getClassLoader().getResource("libraries").getPath()));
            invocable = (Invocable) engine;
        } catch (ScriptException e) {
            logger.error("Error in compile moment.js ", e);
        }
    }

    public static String getFunctionWithMoment(String pathLibraries) {
        return
                String.format("load(\"%1s/moment.min.js\");\n", pathLibraries) +
                        String.format("load(\"%1s/moment-timezone.min.js\");\n", pathLibraries) +
                        "var format = function(value,timeZone,dateFormat) {\n" +
                        "    var dateValue = value;\n" +
                        "    if(!(value instanceof Date)) {dateValue = new Date(Number(value));}\n" +
//                        "    var applyOffset  = moment.tz(new Date(), timeZone).utcOffset();\n" +
//                        "return moment(dateValue.getTime()).utcOffset(applyOffset).format(dateFormat);}\n";
                        "return moment(dateValue.getTime()).tz(timeZone).format(dateFormat);}\n";
    }

    public static String momentPatternToJava(String momentDateFormat) {
        return translateFormat(momentDateFormat, MOMENT_FORMAT_MAPPING);
    }

    public static String javaPatternToMoment(String javaDateFormat) {
        return translateFormat(javaDateFormat, JAVA_FORMAT_MAPPING);
    }

    private static String translateFormat(String formatString, Map<String, String> mapping) {
        int len = formatString.length();
        int beginIndex = -1;
        int i = 0;
        String lastChar = null;
        String currentChar = "";
        String resultString = "";
        for (; i < len; i++) {
            currentChar = String.valueOf(formatString.charAt(i));
            if (lastChar == null || !currentChar.equals(lastChar)) {
                resultString = appendMappedString(formatString, mapping, beginIndex, i, resultString);
                beginIndex = i;
            }
            lastChar = currentChar;
        }
        return appendMappedString(formatString, mapping, beginIndex, i, resultString);
    }

    private static String appendMappedString(String formatString, Map<String, String> mapping, int beginIndex, int currentIndex, String resultString) {
        String tempString;
        if (beginIndex != -1) {
            tempString = formatString.substring(beginIndex, currentIndex);
            if (mapping.get(tempString) != null) {
                tempString = mapping.get(tempString);
            }
            resultString = resultString + tempString;
        }
        return resultString;
    }

    public static String formatDate(Long timestamp, String timeZone, String format) {
        return formatDate(new Date(timestamp), timeZone, format);
    }

    public static String formatDate(Date date, String timeZone, String format) {
        try {
            if (invocable == null) {
                initCompileMomentJS();
            }
            return String.valueOf(invocable.invokeFunction("format", date.getTime(), timeZone, format));
        } catch (Exception e) {
            logger.error("Exception in format date with moment.js", e);
        }
        return String.valueOf(date);
    }
}
