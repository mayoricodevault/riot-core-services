package com.tierconnect.riot.commons.utils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.junit.Assert;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created by IntelliJ IDEA.
 *
 * @author : dbascope
 * @date : 8/11/16 9:44 AM
 * @version:
 */
public class RelativeDateUtilTest {

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testSetRelativeDateToday() throws Exception {

        String relativeDate = "TODAY";

        String stringDate = "03/17/2016 17:15:00";
        DateFormat format = new SimpleDateFormat("MM/dd/yyy HH:mm:ss");
        Date now = format.parse(stringDate);

        RelativeDateUtil relDateUtil = new RelativeDateUtil();
        relDateUtil.setRelativeDate(relativeDate, now, null);
        String startDate = format.format(relDateUtil.getStartDate());
        String endDate = format.format(relDateUtil.getEndDate());

        Assert.assertEquals("03/17/2016 00:00:00", startDate);
        Assert.assertEquals(endDate, "03/17/2016 17:15:00");
    }

    @Test
    public void testSetRelativeDateYesterday() throws Exception {

        String relativeDate = "LAST_DAY_1";

        String stringDate = "03/17/2016 17:15:00";
        DateFormat format = new SimpleDateFormat("MM/dd/yyy HH:mm:ss");
        Date now = format.parse(stringDate);

        RelativeDateUtil relDateUtil = new RelativeDateUtil();
        relDateUtil.setRelativeDate(relativeDate, now, null);


        String startDate = format.format(relDateUtil.getStartDate());
        String endDate = format.format(relDateUtil.getEndDate());

        Assert.assertEquals("03/16/2016 00:00:00", startDate);
        Assert.assertEquals("03/16/2016 23:59:59", endDate);
    }

    @Test
    public void testSetRelativeDateThisHour() throws Exception {

        String relativeDate = "THIS_HOUR";

        String stringDate = "03/17/2016 17:15:00";

        DateFormat format = new SimpleDateFormat("MM/dd/yyy HH:mm:ss");
        Date now = format.parse(stringDate);

        RelativeDateUtil relDateUtil = new RelativeDateUtil();
        relDateUtil.setRelativeDate(relativeDate, now, null);


        String startDate = format.format(relDateUtil.getStartDate());
        String endDate = format.format(relDateUtil.getEndDate());

        Assert.assertEquals("03/17/2016 17:00:00", startDate);
        Assert.assertEquals("03/17/2016 17:15:00", endDate);
    }

    @Test
    public void testSetRelativeDateThisWeek() throws Exception {

        String relativeDate = "THIS_WEEK";

        String stringDate = "03/17/2016 17:15:00";

        DateFormat format = new SimpleDateFormat("MM/dd/yyy HH:mm:ss");
        Date now = format.parse(stringDate);

        RelativeDateUtil relDateUtil = new RelativeDateUtil();
        relDateUtil.setRelativeDate(relativeDate, now, null);


        String startDate = format.format(relDateUtil.getStartDate());
        String endDate = format.format(relDateUtil.getEndDate());

        Assert.assertEquals("03/14/2016 00:00:00", startDate);
        Assert.assertEquals("03/17/2016 17:15:00", endDate);
    }

    @Test
    public void testSetRelativeDateThisMonth() throws Exception {

        String relativeDate = "THIS_MONTH";

        String stringDate = "03/17/2016 17:15:00";

        DateFormat format = new SimpleDateFormat("MM/dd/yyy HH:mm:ss");
        Date now = format.parse(stringDate);

        RelativeDateUtil relDateUtil = new RelativeDateUtil();
        relDateUtil.setRelativeDate(relativeDate, now, null);


        String startDate = format.format(relDateUtil.getStartDate());
        String endDate = format.format(relDateUtil.getEndDate());

        Assert.assertEquals("03/01/2016 00:00:00", startDate);
        Assert.assertEquals("03/17/2016 17:15:00", endDate);
    }

    @Test
    public void testSetRelativeDateThisYear() throws Exception {

        String relativeDate = "THIS_YEAR";

        String stringDate = "03/17/2016 17:15:00";

        DateFormat format = new SimpleDateFormat("MM/dd/yyy HH:mm:ss");
        Date now = format.parse(stringDate);

        RelativeDateUtil relDateUtil = new RelativeDateUtil();
        relDateUtil.setRelativeDate(relativeDate, now, null);


        String startDate = format.format(relDateUtil.getStartDate());
        String endDate = format.format(relDateUtil.getEndDate());

        Assert.assertEquals("01/01/2016 00:00:00", startDate);
        Assert.assertEquals("03/17/2016 17:15:00", endDate);

    }

    @Test
    public void testSetRelativeDateLastHour() throws Exception {

        String relativeDate = "LAST_HOUR_1";

        String stringDate = "03/17/2016 17:15:00";

        DateFormat format = new SimpleDateFormat("MM/dd/yyy HH:mm:ss");
        Date now = format.parse(stringDate);

        RelativeDateUtil relDateUtil = new RelativeDateUtil();
        relDateUtil.setRelativeDate(relativeDate, now, null);


        String startDate = format.format(relDateUtil.getStartDate());
        String endDate = format.format(relDateUtil.getEndDate());

        Assert.assertEquals("03/17/2016 16:00:00", startDate);
        Assert.assertEquals("03/17/2016 16:59:59", endDate);

    }

    @Test
    public void testSetRelativeDateLastWeek() throws Exception {

        String relativeDate = "LAST_WEEK_1";

        String stringDate = "03/17/2016 17:15:00";

        DateFormat format = new SimpleDateFormat("MM/dd/yyy HH:mm:ss");
        Date now = format.parse(stringDate);

        RelativeDateUtil relDateUtil = new RelativeDateUtil();
        relDateUtil.setRelativeDate(relativeDate, now, null);


        String startDate = format.format(relDateUtil.getStartDate());
        String endDate = format.format(relDateUtil.getEndDate());

        Assert.assertEquals("03/07/2016 00:00:00", startDate);
        Assert.assertEquals("03/13/2016 23:59:59", endDate);

    }

    @Test
    public void testSetRelativeDateLastMonth() throws Exception {

        String relativeDate = "LAST_MONTH_1";

        String stringDate = "03/17/2016 17:15:00";

        DateFormat format = new SimpleDateFormat("MM/dd/yyy HH:mm:ss");
        Date now = format.parse(stringDate);

        RelativeDateUtil relDateUtil = new RelativeDateUtil();
        relDateUtil.setRelativeDate(relativeDate, now, null);


        String startDate = format.format(relDateUtil.getStartDate());
        String endDate = format.format(relDateUtil.getEndDate());

        Assert.assertEquals("02/01/2016 00:00:00", startDate);
        Assert.assertEquals("02/29/2016 23:59:59", endDate);


    }

    @Test
    public void testSetRelativeDateLastYear() throws Exception {

        String relativeDate = "LAST_YEAR_1";

        String stringDate = "03/17/2016 17:15:00";

        DateFormat format = new SimpleDateFormat("MM/dd/yyy HH:mm:ss");
        Date now = format.parse(stringDate);

        RelativeDateUtil relDateUtil = new RelativeDateUtil();
        relDateUtil.setRelativeDate(relativeDate, now, null);


        String startDate = format.format(relDateUtil.getStartDate());
        String endDate = format.format(relDateUtil.getEndDate());

        Assert.assertEquals("01/01/2015 00:00:00", startDate);
        Assert.assertEquals("12/31/2015 23:59:59", endDate);


    }

    @Test
    public void testSetRelativeDateAHourAgo() throws Exception {

        String relativeDate = "AGO_HOUR_1";

        String stringDate = "03/17/2016 17:15:00";

        DateFormat format = new SimpleDateFormat("MM/dd/yyy HH:mm:ss");
        Date now = format.parse(stringDate);

        RelativeDateUtil relDateUtil = new RelativeDateUtil();
        relDateUtil.setRelativeDate(relativeDate, now, null);


        String startDate = format.format(relDateUtil.getStartDate());
        String endDate = format.format(relDateUtil.getEndDate());

        Assert.assertEquals("03/17/2016 16:15:00", startDate);
        Assert.assertEquals("03/17/2016 17:15:00", endDate);


    }

    @Test
    public void testSetRelativeDateADayAgo() throws Exception {

        String relativeDate = "AGO_DAY_1";

        String stringDate = "03/17/2016 17:15:00";

        DateFormat format = new SimpleDateFormat("MM/dd/yyy HH:mm:ss");
        Date now = format.parse(stringDate);

        RelativeDateUtil relDateUtil = new RelativeDateUtil();
        relDateUtil.setRelativeDate(relativeDate, now, null);


        String startDate = format.format(relDateUtil.getStartDate());
        String endDate = format.format(relDateUtil.getEndDate());

        Assert.assertEquals("03/16/2016 17:15:00", startDate);
        Assert.assertEquals("03/17/2016 17:15:00", endDate);


    }

    @Test
    public void testSetRelativeDateAWeekAgo() throws Exception {

        String relativeDate = "AGO_WEEK_1";

        String stringDate = "03/17/2016 17:15:00";

        DateFormat format = new SimpleDateFormat("MM/dd/yyy HH:mm:ss");
        Date now = format.parse(stringDate);

        RelativeDateUtil relDateUtil = new RelativeDateUtil();
        relDateUtil.setRelativeDate(relativeDate, now, null);


        String startDate = format.format(relDateUtil.getStartDate());
        String endDate = format.format(relDateUtil.getEndDate());

        Assert.assertEquals("03/10/2016 17:15:00", startDate);
        Assert.assertEquals("03/17/2016 17:15:00", endDate);

    }

    @Test
    public void testSetRelativeDateAMonthAgo() throws Exception {

        String relativeDate = "AGO_MONTH_1";

        String stringDate = "03/17/2016 17:15:00";

        DateFormat format = new SimpleDateFormat("MM/dd/yyy HH:mm:ss");
        Date now = format.parse(stringDate);

        RelativeDateUtil relDateUtil = new RelativeDateUtil();
        relDateUtil.setRelativeDate(relativeDate, now, null);


        String startDate = format.format(relDateUtil.getStartDate());
        String endDate = format.format(relDateUtil.getEndDate());

        Assert.assertEquals("02/17/2016 17:15:00", startDate);
        Assert.assertEquals("03/17/2016 17:15:00", endDate);

    }

    @Test
    public void testSetRelativeDateAYearAgo() throws Exception {

        String relativeDate = "AGO_YEAR_1";

        String stringDate = "03/17/2016 17:15:00";

        DateFormat format = new SimpleDateFormat("MM/dd/yyy HH:mm:ss");
        Date now = format.parse(stringDate);

        RelativeDateUtil relDateUtil = new RelativeDateUtil();
        relDateUtil.setRelativeDate(relativeDate, now, null);


        String startDate = format.format(relDateUtil.getStartDate());
        String endDate = format.format(relDateUtil.getEndDate());

        Assert.assertEquals("03/17/2015 17:15:00", startDate);
        Assert.assertEquals("03/17/2016 17:15:00", endDate);

    }

    @Test
    public void testSetRelativeDateAsOfAnHourAgo() throws Exception {

        String relativeDate = "AS_OF_HOUR_1";

        String stringDate = "03/17/2016 17:15:00";

        DateFormat format = new SimpleDateFormat("MM/dd/yyy HH:mm:ss");
        Date now = format.parse(stringDate);

        RelativeDateUtil relDateUtil = new RelativeDateUtil();
        relDateUtil.setRelativeDate(relativeDate, now, null);


        String startDate = format.format(relDateUtil.getStartDate());
        String endDate = format.format(relDateUtil.getEndDate());

        Assert.assertEquals("01/01/1900 00:00:00", startDate);
        Assert.assertEquals("03/17/2016 16:59:59", endDate);

    }

    @Test
    public void testSetRelativeDateAsOfADayAgo() throws Exception {

        String relativeDate = "AS_OF_DAY_1";

        String stringDate = "03/17/2016 17:15:00";

        DateFormat format = new SimpleDateFormat("MM/dd/yyy HH:mm:ss");
        Date now = format.parse(stringDate);

        RelativeDateUtil relDateUtil = new RelativeDateUtil();
        relDateUtil.setRelativeDate(relativeDate, now, null);


        String startDate = format.format(relDateUtil.getStartDate());
        String endDate = format.format(relDateUtil.getEndDate());

        Assert.assertEquals("01/01/1900 00:00:00", startDate);
        Assert.assertEquals("03/16/2016 23:59:59", endDate);

    }


    @Test
    public void testSetRelativeDateAsOfAWeekAgo() throws Exception {

        String relativeDate = "AS_OF_WEEK_1";

        String stringDate = "03/17/2016 17:15:00";

        DateFormat format = new SimpleDateFormat("MM/dd/yyy HH:mm:ss");
        Date now = format.parse(stringDate);

        RelativeDateUtil relDateUtil = new RelativeDateUtil();
        relDateUtil.setRelativeDate(relativeDate, now, null);


        String startDate = format.format(relDateUtil.getStartDate());
        String endDate = format.format(relDateUtil.getEndDate());

        Assert.assertEquals("01/01/1900 00:00:00", startDate);
        Assert.assertEquals("03/13/2016 23:59:59", endDate);

    }

    @Test
    public void testSetRelativeDateAsOfAMonthAgo() throws Exception {

        String relativeDate = "AS_OF_MONTH_1";

        String stringDate = "03/17/2016 17:15:00";

        DateFormat format = new SimpleDateFormat("MM/dd/yyy HH:mm:ss");
        Date now = format.parse(stringDate);

        RelativeDateUtil relDateUtil = new RelativeDateUtil();
        relDateUtil.setRelativeDate(relativeDate, now, null);


        String startDate = format.format(relDateUtil.getStartDate());
        String endDate = format.format(relDateUtil.getEndDate());

        Assert.assertEquals("01/01/1900 00:00:00", startDate);
        Assert.assertEquals("02/29/2016 23:59:59", endDate);

    }

    @Test
    public void testSetRelativeDateAsOfAYearAgo() throws Exception {

        String relativeDate = "AS_OF_YEAR_1";

        String stringDate = "03/17/2016 17:15:00";

        DateFormat format = new SimpleDateFormat("MM/dd/yyy HH:mm:ss");
        Date now = format.parse(stringDate);

        RelativeDateUtil relDateUtil = new RelativeDateUtil();
        relDateUtil.setRelativeDate(relativeDate, now, null);


        String startDate = format.format(relDateUtil.getStartDate());
        String endDate = format.format(relDateUtil.getEndDate());

        Assert.assertEquals("01/01/1900 00:00:00", startDate);
        Assert.assertEquals("12/31/2015 23:59:59", endDate);

    }

    @Test
    public void testSetRelativeDateAllDay() throws Exception {

        String relativeDate = "ALL_DAY";

        String stringDate = "03/17/2016 17:15:00";

        DateFormat format = new SimpleDateFormat("MM/dd/yyy HH:mm:ss");
        Date now = format.parse(stringDate);

        RelativeDateUtil relDateUtil = new RelativeDateUtil();
        relDateUtil.setRelativeDate(relativeDate, now, null);


        String startDate = format.format(relDateUtil.getStartDate());
        String endDate = format.format(relDateUtil.getEndDate());

        Assert.assertEquals("03/17/2016 00:00:00", startDate);
        Assert.assertEquals("03/17/2016 23:59:59", endDate);


    }

    @Test
    public void testStartTimeOfThisTime() throws Exception {

        DateFormat format = new SimpleDateFormat("MM/dd/yyy HH:mm:ss");
        Date now = format.parse("03/17/2016 17:15:00");

        Calendar calendarDate = new GregorianCalendar();
        calendarDate.setTime(now);

        Date startDate = RelativeDateUtil.startTimeOfThisTime(calendarDate, "TODAY").getTime();

        String stringStartDate = format.format(startDate);
        Assert.assertEquals("03/17/2016 00:00:00", stringStartDate);

        startDate = RelativeDateUtil.startTimeOfThisTime(calendarDate, "HOUR_OF_DAY").getTime();
        stringStartDate = format.format(startDate);
        Assert.assertEquals("03/17/2016 17:00:00", stringStartDate);

        startDate = RelativeDateUtil.startTimeOfThisTime(calendarDate, "WEEK").getTime();
        stringStartDate = format.format(startDate);
        Assert.assertEquals("03/13/2016 00:00:00", stringStartDate);

        startDate = RelativeDateUtil.startTimeOfThisTime(calendarDate, "MONTH").getTime();
        stringStartDate = format.format(startDate);
        Assert.assertEquals("03/01/2016 00:00:00", stringStartDate);

        startDate = RelativeDateUtil.startTimeOfThisTime(calendarDate, "YEAR").getTime();
        stringStartDate = format.format(startDate);
        Assert.assertEquals("01/01/2016 00:00:00", stringStartDate);

    }

    @Test
    public void testFirstTimeOfLastTime() throws Exception {

        DateFormat format = new SimpleDateFormat("MM/dd/yyy HH:mm:ss");
        Date now = format.parse("03/17/2016 17:15:00");

        Calendar calendarDate = new GregorianCalendar();
        calendarDate.setTime(now);

        Date startDate = RelativeDateUtil.firstTimeOfLastTime(calendarDate, -1, "HOUR").getTime();
        String stringStartDate = format.format(startDate);
        Assert.assertEquals("03/17/2016 16:00:00", stringStartDate);

        startDate = RelativeDateUtil.firstTimeOfLastTime(calendarDate, -1, "DAY").getTime();
        stringStartDate = format.format(startDate);
        Assert.assertEquals("03/16/2016 00:00:00", stringStartDate);

        startDate = RelativeDateUtil.firstTimeOfLastTime(calendarDate, -1, "WEEK").getTime();
        stringStartDate = format.format(startDate);
        Assert.assertEquals("03/06/2016 00:00:00", stringStartDate);

        startDate = RelativeDateUtil.firstTimeOfLastTime(calendarDate, -1, "MONTH").getTime();
        stringStartDate = format.format(startDate);
        Assert.assertEquals("02/01/2016 00:00:00", stringStartDate);

        startDate = RelativeDateUtil.firstTimeOfLastTime(calendarDate, -1, "YEAR").getTime();
        stringStartDate = format.format(startDate);
        Assert.assertEquals("01/01/2015 00:00:00", stringStartDate);
    }

    @Test
    public void testEndTimeOfLastTime() throws Exception {

    }

    @Test
    public void testFirstTimeOfTimeAgo() throws Exception {

    }

    @Test
    public void testIsValidRelativeDateCode() throws Exception {

    }

    @Test
    public void testGetEndDate() throws Exception {

    }

    @Test
    public void testSetEndDate() throws Exception {

    }

    @Test
    public void testGetStartDate() throws Exception {

    }

    @Test
    public void testSetStartDate() throws Exception {

    }
}