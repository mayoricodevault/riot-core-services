package com.tierconnect.riot.commons.utils;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

/**
 * FormatUtilTest.
 *
 * @author jantezana
 * @version 2017/03/09
 */
public class FormatUtilTest {

    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

    @Test
    public void format()
    throws Exception {
        long dateLong = 1493125038000L;
        Date date = new Date(dateLong);
        String actual = FormatUtil.format(date, DATE_FORMAT);
        String expected = "2017-04-25T08:57:18.000-04:00";
        assertNotNull(actual);
        assertEquals(expected, actual);
    }

    @Test
    public void formatOtherFormat()
    throws Exception {
        String dateString = "2017-02-01T04:00:00.000Z";
        Date actual = FormatUtil.getDateAndDetermineFormat(dateString);
        assertNotNull(actual);
    }


    @Test
    public void formatWithSpecificFormat()
    throws Exception {
        String dateString = "2017-02-01T04:00:00.000Z";
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        Date actual = sdf.parse(dateString);
        assertNotNull(actual);
    }

    @Test
    public void getDateAndDetermineFormat()
    throws Exception {
        String dateString = "2017-04-25T08:57:18.000-04:00";
        Date actual = FormatUtil.getDateAndDetermineFormat(dateString);
        assertNotNull(actual);
    }
}