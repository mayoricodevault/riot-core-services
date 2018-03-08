package com.tierconnect.riot.commons.utils;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.FileNotFoundException;
import java.util.Date;

import static com.tierconnect.riot.commons.utils.MomentDateFormatUtils.*;
import static org.junit.Assert.assertEquals;

/**
 * Created by vealaro on 4/18/17.
 */
public class MomentDateFormatUtilsTest {

    private static Logger logger = Logger.getLogger(MomentDateFormatUtilsTest.class);

    @BeforeClass
    public static void createEngine() throws ScriptException {
        long start = System.currentTimeMillis();
        String pathDirectory = System.getProperty("user.dir");
        pathDirectory = pathDirectory.substring(0, pathDirectory.length() - 7) + "src/main/resources/libraries";
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
        engine.eval(MomentDateFormatUtils.getFunctionWithMoment(pathDirectory));
        MomentDateFormatUtils.invocable = (Invocable) engine;
        logger.info("time create engine with nashorn = " + (System.currentTimeMillis() - start) + "\n");
    }

    @Test
    public void testPatternToJava() throws Exception {
        // Date checks
        assertEquals("Check day", momentPatternToJava("DD."), "dd.");
        assertEquals("Check month", momentPatternToJava("DD.MM."), "dd.MM.");
        assertEquals("Check date", momentPatternToJava("DD.MM.YYYY"), "dd.MM.yyyy");
        assertEquals("Check date with single day and month", momentPatternToJava("D.M.YYYY"), "d.M.yyyy");
        assertEquals("Check year with 4 digits", momentPatternToJava("YYYY"), "yyyy");
        assertEquals("Check year with 2 digits", momentPatternToJava("YY"), "yy");
        assertEquals("Check just the month", momentPatternToJava("M"), "M");
        assertEquals("Check just the month with leading zero", momentPatternToJava("MM"), "MM");
        assertEquals("Check the month name short", momentPatternToJava("MMM"), "MMM");
        assertEquals("Check the month name long", momentPatternToJava("MMMM"), "MMMM");

        //Hour and minute checks
        assertEquals("Check 24h time", momentPatternToJava("HH:mm"), "HH:mm");
        assertEquals("Check 12h time", momentPatternToJava("hh:mm"), "hh:mm");
        assertEquals("Check am/pm upper case", momentPatternToJava("hh:mm A"), "hh:mm a");
        assertEquals("Check am/pm lower case", momentPatternToJava("hh:mm a"), "hh:mm a");
        assertEquals("Check am/pm upper case single hour", momentPatternToJava("h:mm A"), "h:mm a");
        assertEquals("Check am/pm lower case single hour", momentPatternToJava("h:mm a"), "h:mm a");
        assertEquals("Check single minute", momentPatternToJava("m"), "m");
        assertEquals("Check single hour (12 hour format)", momentPatternToJava("h"), "h");
        assertEquals("Check single hour (24 hour format)", momentPatternToJava("H"), "H");

        // Seconds and milliseconds checks
        assertEquals("Check time with seconds of the minute", momentPatternToJava("HH:mm:ss"), "HH:mm:ss");
        assertEquals("Check time with milliseconds", momentPatternToJava("HH:mm:ss.SSS"), "HH:mm:ss.S");
        assertEquals("Check just the seconds", momentPatternToJava("s"), "s");
        assertEquals("Check just the seconds with leading zero", momentPatternToJava("ss"), "ss");
        assertEquals("Check just the milliseconds S", momentPatternToJava("S"), "S");
        assertEquals("Check just the milliseconds SS", momentPatternToJava("SS"), "S");
        assertEquals("Check just the milliseconds SSS", momentPatternToJava("SSS"), "S");

        // Weekday checks
        assertEquals("Check weekday name short", momentPatternToJava("ddd"), "E");
        assertEquals("Check weekday name long", momentPatternToJava("dddd"), "EEEE");
        assertEquals("Check day in year", momentPatternToJava("DDD"), "D");
        assertEquals("Check day in week", momentPatternToJava("u"), "u");
        assertEquals("Check week in year", momentPatternToJava("W"), "w");
        assertEquals("Check week in year with leading zero", momentPatternToJava("WW"), "ww");

        assertEquals("Check format", momentPatternToJava("MM/DD/YYYY hh:mm:ss A"), "MM/dd/yyyy hh:mm:ss a");

    }


    @Test
    public void testPatternToMoment() throws Exception {
        // Date checks
        assertEquals("Check day", javaPatternToMoment("dd."), "DD.");
        assertEquals("Check day and Month", javaPatternToMoment("dd.MM."), "DD.MM.");
        assertEquals("Check date", javaPatternToMoment("dd.MM.yyyy"), "DD.MM.YYYY");
        assertEquals("Check date single day and month", javaPatternToMoment("d.M.yyyy"), "D.M.YYYY");
        assertEquals("Check just the year with 4 digits", javaPatternToMoment("yyyy"), "YYYY");
        assertEquals("Check just the year with 2 digits", javaPatternToMoment("yy"), "YY");
        assertEquals("Check just the year with 1 digit resolves to year with 4 digits", javaPatternToMoment("y"), "YYYY");
        assertEquals("Check just the month", javaPatternToMoment("M"), "M");
        assertEquals("Check just the month with leading zero", javaPatternToMoment("MM"), "MM");
        assertEquals("Check the month name short", javaPatternToMoment("MMM"), "MMM");
        assertEquals("Check the month name long", javaPatternToMoment("MMMM"), "MMMM");

        // Hour and minute checks
        assertEquals("Check 24h time", javaPatternToMoment("HH:mm"), "HH:mm");
        assertEquals("Check 12h time", javaPatternToMoment("hh:mm"), "hh:mm");
        assertEquals("Check am/pm upper case", javaPatternToMoment("hh:mm A"), "hh:mm A");
        assertEquals("Check am/pm lower case", javaPatternToMoment("hh:mm a"), "hh:mm a");
        assertEquals("Check am/pm upper case single hour", javaPatternToMoment("h:mm A"), "h:mm A");
        assertEquals("Check am/pm lower case single hour", javaPatternToMoment("h:mm a"), "h:mm a");
        assertEquals("Check single minute", javaPatternToMoment("m"), "m");
        assertEquals("Check single hour (12 hour format)", javaPatternToMoment("h"), "h");
        assertEquals("Check single hour (24 hour format)", javaPatternToMoment("H"), "H");

        // Seconds and milliseconds checks
        assertEquals("Check time with seconds of the minute", javaPatternToMoment("HH:mm:ss"), "HH:mm:ss");
        assertEquals("Check time with milliseconds", javaPatternToMoment("HH:mm:ss.SSS"), "HH:mm:ss.SSS");
        assertEquals("Check just the seconds", javaPatternToMoment("s"), "s");
        assertEquals("Check just the seconds with leading zero", javaPatternToMoment("ss"), "ss");
        assertEquals("Check just the milliseconds S", javaPatternToMoment("S"), "SSS");
        assertEquals("Check just the milliseconds SS", javaPatternToMoment("SS"), "SSS");
        assertEquals("Check just the milliseconds SSS", javaPatternToMoment("SSS"), "SSS");

        // Weekday checks
        assertEquals("Check weekday name short", javaPatternToMoment("E"), "ddd");
        assertEquals("Check weekday name long", javaPatternToMoment("EEEE"), "dddd");
        assertEquals("Check day in year", javaPatternToMoment("D"), "DDD");
        assertEquals("Check day in week", javaPatternToMoment("u"), "E");
        assertEquals("Check week in year", javaPatternToMoment("w"), "W");
        assertEquals("Check week in year with leading zero", javaPatternToMoment("ww"), "WW");

        // Timezone checks
        assertEquals("Check timezone short", javaPatternToMoment("z"), "ZZ");
        assertEquals("timezone long", javaPatternToMoment("zzzz"), "Z");
        assertEquals("Check day in year", javaPatternToMoment("Z"), "ZZ");

        assertEquals("Check week in year", javaPatternToMoment("X"), "ZZ");
        assertEquals("Check week in year with leading zero", javaPatternToMoment("XX"), "ZZ");
        assertEquals("Check week in month", javaPatternToMoment("XXX"), "Z");

        assertEquals("Check format", javaPatternToMoment("MM/dd/yyyy hh:mm:ss a"), "MM/DD/YYYY hh:mm:ss a");
    }


    @Test
    public void testNashorn() throws FileNotFoundException, ScriptException, NoSuchMethodException {
        Long date = 1492803795345L;
        logger.info("DATE " + new Date(date));
        logger.info(formatDate(date, "Europe/London", "MM/DD/YYYY hh:mm:ss A"));
        logger.info(formatDate(date, "Europe/London", "MM/DD/YYYY hh:mm:ss"));
        logger.info(formatDate(date, "Europe/London", "MM/DD/YYYY"));
        logger.info(formatDate(date, "Europe/London", "MMMM Do YYYY, h:mm:ss a"));
        logger.info(formatDate(date, "Europe/London", "dddd"));
        logger.info(formatDate(date, "Europe/London", "MMM Do YY"));
        logger.info(formatDate(date, "Europe/London", "YYYY [escaped] YYYY"));
        logger.info(formatDate(date, "Europe/London", "dd[//]MM[//]YYYY"));
        logger.info(formatDate(date, "Europe/London", "Y"));
    }
}