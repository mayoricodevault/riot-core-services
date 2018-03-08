package com.tierconnect.riot.commons.utils;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static junitparams.JUnitParamsRunner.$;

/**
 * Created by vealaro on 10/11/16.
 */
@RunWith(JUnitParamsRunner.class)
public class DateHelperTest {

    @Test
    public void testFormatDwellTime() throws Exception {
        Map<String, Long> mapDwellTime = new LinkedHashMap<>();
        mapDwellTime.put("0 Days 00:08:39", 519754L);
        mapDwellTime.put("0 Days 00:00:12", 12996L);
        mapDwellTime.put("0 Days 00:00:12", 12000L);
        mapDwellTime.put("0 Days 00:00:01", 1000L);
        mapDwellTime.put("-0 Days 00:00:01", -1000L);
        mapDwellTime.put("2 Days 21:37:32", 250652671L);
        mapDwellTime.put("-2 Days 21:37:32", -250652671L);
        mapDwellTime.put("0 Days 00:22:55", 1375858L);
        mapDwellTime.put("0 Days 00:18:39", 1119211L);
        for (Map.Entry<String, Long> entry : mapDwellTime.entrySet()) {
            Assert.assertTrue(entry.getKey().equals(DateHelper.formatDwellTime(entry.getValue())));
        }
    }

}