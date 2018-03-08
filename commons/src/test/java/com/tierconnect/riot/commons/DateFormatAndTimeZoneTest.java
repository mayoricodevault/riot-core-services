package com.tierconnect.riot.commons;

import com.tierconnect.riot.commons.utils.MomentDateFormatUtils;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import static org.junit.Assert.assertEquals;

/**
 * Created by vealaro on 4/20/17.
 */
@RunWith(value = Parameterized.class)
public class DateFormatAndTimeZoneTest {

    private static Logger logger = Logger.getLogger(DateFormatAndTimeZoneTest.class);

    private Long timestamp;
    private String timeZone;
    private String dateFormat;

    public DateFormatAndTimeZoneTest(Long timestamp, String timeZone, String dateFormat) {
        this.timestamp = timestamp;
        this.timeZone = timeZone;
        this.dateFormat = dateFormat;
    }

    @BeforeClass
    public static void createEngine() throws ScriptException {
        String pathDirectory = System.getProperty("user.dir");
        pathDirectory = pathDirectory.substring(0, pathDirectory.length() - 7) + "src/main/resources/libraries";
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
        engine.eval(MomentDateFormatUtils.getFunctionWithMoment(pathDirectory));
        MomentDateFormatUtils.invocable = (Invocable) engine;
    }

    @Test
    public void testGetISODateTimeFormat() {
        DateFormatAndTimeZone formatAndTimeZone = new DateFormatAndTimeZone(timeZone);
        String isoDate = formatAndTimeZone.getISODateTimeFormatWithoutTimeZone(timestamp);
        logger.info("    TZ= " + timeZone);
        logger.info("    TS= " + timestamp);
        logger.info("  DATE= " + new Date(timestamp));
        logger.info("   ISO= " + isoDate);
        logger.info("FORMAT= " + formatAndTimeZone.format(timestamp));
        assertEquals("Check format ISO", timestamp, formatAndTimeZone.parseISOStringToTimeStamp(isoDate));
        logger.info("-----------------------------------------\n");
    }

    @Test
    public void testGetISODateTimeFormat2() throws ParseException {
        DateFormatAndTimeZone formatAndTimeZone = new DateFormatAndTimeZone();
        assertEquals(
                formatAndTimeZone.parseISOStringToTimeStamp("2017-04-24T22:20:41.540+01:00"),
                formatAndTimeZone.parseISOStringToTimeStamp("2017-04-24T22:20:41.540+0100"));
        assertEquals(
                formatAndTimeZone.parseISOStringToTimeStamp("2017-04-25T16:13:45.647-03:00"),
                formatAndTimeZone.parseISOStringToTimeStamp("2017-04-25T16:13:45.647-0300"));
        assertEquals(
                formatAndTimeZone.parseISOStringToTimeStamp("2017-04-25T15:13:45.647-04:00"),
                formatAndTimeZone.parseISOStringToTimeStamp("2017-04-25T15:13:45.647-0400"));
        assertEquals(
                formatAndTimeZone.parseISOStringToTimeStamp("2017-04-26T03:13:45.647+08:00"),
                formatAndTimeZone.parseISOStringToTimeStamp("2017-04-26T03:13:45.647+0800"));
        assertEquals(
                formatAndTimeZone.parseISOStringToTimeStamp("2017-04-25T14:43:45.647-04:30"),
                formatAndTimeZone.parseISOStringToTimeStamp("2017-04-25T14:43:45.647-0430"));
        assertEquals(
                formatAndTimeZone.parseISOStringToTimeStamp("2017-04-27T15:30+01:00"),
                formatAndTimeZone.parseISOStringToTimeStamp("2017-04-27T15:30+0100"));
        assertEquals(
                formatAndTimeZone.parseISOStringToTimeStamp("2017-04-27T15:30Z"),
                formatAndTimeZone.parseISOStringToTimeStamp("2017-04-27T15:30Z"));
    }

    @Parameterized.Parameters(name = "{index} timestamp={0}, timezone=\"{1}\", dateFormat=\"{2}\"")
    public static Iterable<Object[]> parametersDateFormat() {
        return Arrays.asList(
                new Object[]{System.currentTimeMillis(), "Europe/London", ""},
                new Object[]{System.currentTimeMillis(), "America/Santiago", ""},
                new Object[]{System.currentTimeMillis(), "EST5EDT", ""},
                new Object[]{System.currentTimeMillis(), "Australia/Perth", ""},
                new Object[]{System.currentTimeMillis(), "America/Caracas", ""}
        );
    }

    @Test
    public void parseISOStringToTimeStampTest() {
        String isoDate = "2017-04-24T21:52:26.572+01:00";
        //String isoDate = "2017-04-24T11:25:06.578-0500";
        DateFormatAndTimeZone dateTimeZone = new DateFormatAndTimeZone("Europe/London");
        Long dateLong = dateTimeZone.parseISOStringToTimeStamp(isoDate);
        Date date = new Date(dateLong);
        logger.info("Date: " + date);
    }

    @Test
    public void testDateFormat() throws ParseException {
        String dateFormat = "dd MMMM yyyy HH:mm:ss";
        String value = "28 April 2017 17:30:09";
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat, Locale.US);
        Date parse = sdf.parse(value);
        System.out.println(parse);
        System.out.println(value.matches("^\\d{1,2}\\s[a-zA-Z]{4,}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$"));
    }
}