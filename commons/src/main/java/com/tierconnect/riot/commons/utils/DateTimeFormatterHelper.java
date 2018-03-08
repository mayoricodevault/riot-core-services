package com.tierconnect.riot.commons.utils;


import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.time.temporal.ChronoField.*;

/**
 * Created by vealaro on 5/24/17.
 */
public class DateTimeFormatterHelper {

    private static Logger logger = Logger.getLogger(DateTimeFormatterHelper.class);

    private static final Map<String, String> CHRONO_FIELDS_REPLACE = new LinkedHashMap<String, String>();
    private static final Pattern PATTERN_WORDS;

    static {
        CHRONO_FIELDS_REPLACE.put(ChronoField.CLOCK_HOUR_OF_AMPM.toString(), "'clock hour of AM/PM'");
        CHRONO_FIELDS_REPLACE.put(ChronoField.SECOND_OF_MINUTE.toString(), "'second of minute'");
        CHRONO_FIELDS_REPLACE.put(ChronoField.MINUTE_OF_HOUR.toString(), "'minute of hour'");
        CHRONO_FIELDS_REPLACE.put(ChronoField.HOUR_OF_DAY.toString(), "'hour of day'");
        CHRONO_FIELDS_REPLACE.put(ChronoField.DAY_OF_MONTH.toString(), "'day of month'");
        CHRONO_FIELDS_REPLACE.put(ChronoField.DAY_OF_WEEK.toString(), "'day of week'");
        CHRONO_FIELDS_REPLACE.put(ChronoField.MONTH_OF_YEAR.toString(), "'month of year'");
        PATTERN_WORDS = Pattern.compile(StringUtils.join(CHRONO_FIELDS_REPLACE.keySet(), "|"));
    }

    private static final DateTimeFormatter OFFSET_FORMAT;

    static {
        OFFSET_FORMAT = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .optionalStart().appendOffset("+HHMMss", "Z").optionalEnd()
                .optionalStart().appendOffset("+HHMM", "Z").optionalEnd()
                .optionalStart().appendOffset("+HHMMss", "+0000").optionalEnd()
                .optionalStart().appendOffset("+HHMM", "+0000").optionalEnd()
                .optionalStart().appendOffset("+HH:MM:ss", "Z").optionalEnd()
                .optionalStart().appendOffset("+HH:MM", "Z").optionalEnd()
                .optionalStart().appendOffset("+HH:MM:ss", "+0000").optionalEnd()
                .optionalStart().appendOffset("+HH:MM", "+0000").optionalEnd()
                .toFormatter(Locale.US);
    }

    /**
     * <table>
     * <thead align='center'><tr><td colspan='2'>TIME FORMAT</td></tr></thead>
     * <tbody>
     * <tr><td>'HHmmss.SSSSSSSSSZ'</td><td>'082159.000000502-050030'<br />'082159.000000502+0500'</td></tr>
     * <tr><td>'HHmmss.SSSSSSSSZ'</td><td>'081055.00000337-050030'<br />'081055.00000337+0500'</td></tr>
     * <tr><td>'HHmmss.SSSSSSSZ'</td><td>'081339.0000177-050030'<br />'081339.0000177+0500'</td></tr>
     * <tr><td>'HHmmss.SSSSSSZ'</td><td>'081421.000739-050030'<br />'081421.000739+0500'</td></tr>
     * <tr><td>'HHmmss.SSSSSZ'</td><td>'081531.00684-050030'<br />'081531.00684+0500'</td></tr>
     * <tr><td>'HHmmss.SSSSZ'</td><td>'081635.0887-050030'<br />'081635.0887+0500'</td></tr>
     * <tr><td>'HHmmss.SSSZ'</td><td>'081810.190-050030'<br />'081810.190+0500'</td></tr>
     * <tr><td>'HHmmss.SSZ'</td><td>'081810.190-050030'<br />'081810.190+0500'</td></tr>
     * <tr><td>'HHmmss.SZ'</td><td>'081810.190-050030'<br />'081810.190+0500'</td></tr>
     * <tr><td>'HHmmssZ'</td><td>'081810-050030'<br />'081810+0500'</td></tr>
     * <tr><td>'HHmm.SSSSSSSSSZ'</td><td>'0821.000000502-050030'<br />'0821.000000502+0500'</td></tr>
     * <tr><td>'HHmm.SSSSSSSSZ'</td><td>'0810.00000337-050030'<br />'0810.00000337+0500'</td></tr>
     * <tr><td>'HHmm.SSSSSSSZ'</td><td>'0813.0000177-050030'<br />'0813.0000177+0500'</td></tr>
     * <tr><td>'HHmm.SSSSSSZ'</td><td>'0814.000739-050030'<br />'0814.000739+0500'</td></tr>
     * <tr><td>'HHmm.SSSSSZ'</td><td>'0815.00684-050030'<br />'0815.00684+0500'</td></tr>
     * <tr><td>'HHmm.SSSSZ'</td><td>'0816.0887-050030'<br />'0816.0887+0500'</td></tr>
     * <tr><td>'HHmm.SSSZ'</td><td>'0818.190-050030'<br />'0818.190+0500'</td></tr>
     * <tr><td>'HHmm.SSZ'</td><td>'0818.190-050030'<br />'0818.190+0500'</td></tr>
     * <tr><td>'HHmm.SZ'</td><td>'0818.190-050030'<br />'0818.190+0500'</td></tr>
     * <tr><td>'HHmmZ'</td><td>'0818-050030'<br />'0818+0500'</td></tr>
     * <tr><td>'HHmmss.SSSSSSSSSXXX'</td><td>'082159.000000502-05:00:30'<br />'082159.000000502+05:00'</td></tr>
     * <tr><td>'HHmmss.SSSSSSSSXXX'</td><td>'081055.00000337-05:00:30'<br />'081055.00000337+05:00'</td></tr>
     * <tr><td>'HHmmss.SSSSSSSXXX'</td><td>'081339.0000177-05:00:30'<br />'081339.0000177+05:00'</td></tr>
     * <tr><td>'HHmmss.SSSSSSXXX'</td><td>'081421.000739-05:00:30'<br />'081421.000739+05:00'</td></tr>
     * <tr><td>'HHmmss.SSSSSXXX'</td><td>'081531.00684-05:00:30'<br />'081531.00684+05:00'</td></tr>
     * <tr><td>'HHmmss.SSSSXXX'</td><td>'081635.0887-05:00:30'<br />'081635.0887+05:00'</td></tr>
     * <tr><td>'HHmmss.SSSXXX'</td><td>'081810.190-05:00:30'<br />'081810.190+05:00'</td></tr>
     * <tr><td>'HHmmss.SSXXX'</td><td>'081810.190-05:00:30'<br />'081810.190+05:00'</td></tr>
     * <tr><td>'HHmmss.SXXX'</td><td>'081810.190-05:00:30'<br />'081810.190+05:00'</td></tr>
     * <tr><td>'HHmmssXXX'</td><td>'081810-05:00:30'<br />'081810+05:00'</td></tr>
     * <tr><td>'HHmm.SSSSSSSSSXXX'</td><td>'0821.000000502-05:00:30'<br />'0821.000000502+05:00'</td></tr>
     * <tr><td>'HHmm.SSSSSSSSXXX'</td><td>'0810.00000337-05:00:30'<br />'0810.00000337+05:00'</td></tr>
     * <tr><td>'HHmm.SSSSSSSXXX'</td><td>'0813.0000177-05:00:30'<br />'0813.0000177+05:00'</td></tr>
     * <tr><td>'HHmm.SSSSSSXXX'</td><td>'0814.000739-05:00:30'<br />'0814.000739+05:00'</td></tr>
     * <tr><td>'HHmm.SSSSSXXX'</td><td>'0815.00684-05:00:30'<br />'0815.00684+05:00'</td></tr>
     * <tr><td>'HHmm.SSSSXXX'</td><td>'0816.0887-05:00:30'<br />'0816.0887+05:00'</td></tr>
     * <tr><td>'HHmm.SSSXXX'</td><td>'0818.190-05:00:30'<br />'0818.190+05:00'</td></tr>
     * <tr><td>'HHmm.SSXXX'</td><td>'0818.190-05:00:30'<br />'0818.190+05:00'</td></tr>
     * <tr><td>'HHmm.SXXX'</td><td>'0818.190-05:00:30'<br />'0818.190+05:00'</td></tr>
     * <tr><td>'HHmmXXX'</td><td>'0818-05:00:30'<br />'0818+05:00'</td></tr>
     * <tr><td>'HHmmss'</td><td>'081810'</td></tr>
     * <tr><td>'HHmm'</td><td>'0818'</td></tr>
     * </tbody>
     * </table>
     */
    public static final DateTimeFormatter TIME_HHMMSS_FORMAT_1;

    static {
        TIME_HHMMSS_FORMAT_1 = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendValue(HOUR_OF_DAY, 2)
                .appendValue(MINUTE_OF_HOUR, 2)
                .optionalStart().append(OFFSET_FORMAT).optionalEnd()
                .optionalStart().appendValue(SECOND_OF_MINUTE, 2)
                .optionalStart().appendFraction(NANO_OF_SECOND, 0, 9, true)
                .optionalStart().append(OFFSET_FORMAT)
                .toFormatter(Locale.US);
    }

    /**
     * <table>
     * <thead align='center'><tr><td colspan='2'>TIME FORMAT</td></tr></thead>
     * <tbody>
     * <tr><td>'HH:mm:ss.SSSSSSSSSZ'</td><td>'08:21:59.000000502-050030'<br />'08:21:59.000000502+0500'</td></tr>
     * <tr><td>'HH:mm:ss.SSSSSSSSZ'</td><td>'08:10:55.00000337-050030'<br />'08:10:55.00000337+0500'</td></tr>
     * <tr><td>'HH:mm:ss.SSSSSSSZ'</td><td>'08:13:39.0000177-050030'<br />'08:13:39.0000177+0500'</td></tr>
     * <tr><td>'HH:mm:ss.SSSSSSZ'</td><td>'08:14:21.000739-050030'<br />'08:14:21.000739+0500'</td></tr>
     * <tr><td>'HH:mm:ss.SSSSSZ'</td><td>'08:15:31.00684-050030'<br />'08:15:31.00684+0500'</td></tr>
     * <tr><td>'HH:mm:ss.SSSSZ'</td><td>'08:16:35.0887-050030'<br />'08:16:35.0887+0500'</td></tr>
     * <tr><td>'HH:mm:ss.SSSZ'</td><td>'08:18:10.190-050030'<br />'08:18:10.190+0500'</td></tr>
     * <tr><td>'HH:mm:ss.SSZ'</td><td>'08:18:10.190-050030'<br />'08:18:10.190+0500'</td></tr>
     * <tr><td>'HH:mm:ss.SZ'</td><td>'08:18:10.190-050030'<br />'08:18:10.190+0500'</td></tr>
     * <tr><td>'HH:mm:ssZ'</td><td>'08:18:10-050030'<br />'08:18:10+0500'</td></tr>
     * <tr><td>'HH:mm.SSSSSSSSSZ'</td><td>'08:21.000000502-050030'<br />'08:21.000000502+0500'</td></tr>
     * <tr><td>'HH:mm.SSSSSSSSZ'</td><td>'08:10.00000337-050030'<br />'08:10.00000337+0500'</td></tr>
     * <tr><td>'HH:mm.SSSSSSSZ'</td><td>'08:13.0000177-050030'<br />'08:13.0000177+0500'</td></tr>
     * <tr><td>'HH:mm.SSSSSSZ'</td><td>'08:14.000739-050030'<br />'08:14.000739+0500'</td></tr>
     * <tr><td>'HH:mm.SSSSSZ'</td><td>'08:15.00684-050030'<br />'08:15.00684+0500'</td></tr>
     * <tr><td>'HH:mm.SSSSZ'</td><td>'08:16.0887-050030'<br />'08:16.0887+0500'</td></tr>
     * <tr><td>'HH:mm.SSSZ'</td><td>'08:18.190-050030'<br />'08:18.190+0500'</td></tr>
     * <tr><td>'HH:mm.SSZ'</td><td>'08:18.190-050030'<br />'08:18.190+0500'</td></tr>
     * <tr><td>'HH:mm.SZ'</td><td>'08:18.190-050030'<br />'08:18.190+0500'</td></tr>
     * <tr><td>'HH:mmZ'</td><td>'08:18-050030'<br />'08:18+0500'</td></tr>
     * <tr><td>'HH:mm:ss.SSSSSSSSSXXX'</td><td>'08:21:59.000000502-05:00:30'<br />'08:21:59.000000502+05:00'</td></tr>
     * <tr><td>'HH:mm:ss.SSSSSSSSXXX'</td><td>'08:10:55.00000337-05:00:30'<br />'08:10:55.00000337+05:00'</td></tr>
     * <tr><td>'HH:mm:ss.SSSSSSSXXX'</td><td>'08:13:39.0000177-05:00:30'<br />'08:13:39.0000177+05:00'</td></tr>
     * <tr><td>'HH:mm:ss.SSSSSSXXX'</td><td>'08:14:21.000739-05:00:30'<br />'08:14:21.000739+05:00'</td></tr>
     * <tr><td>'HH:mm:ss.SSSSSXXX'</td><td>'08:15:31.00684-05:00:30'<br />'08:15:31.00684+05:00'</td></tr>
     * <tr><td>'HH:mm:ss.SSSSXXX'</td><td>'08:16:35.0887-05:00:30'<br />'08:16:35.0887+05:00'</td></tr>
     * <tr><td>'HH:mm:ss.SSSXXX'</td><td>'08:18:10.190-05:00:30'<br />'08:18:10.190+05:00'</td></tr>
     * <tr><td>'HH:mm:ss.SSXXX'</td><td>'08:18:10.190-05:00:30'<br />'08:18:10.190+05:00'</td></tr>
     * <tr><td>'HH:mm:ss.SXXX'</td><td>'08:18:10.190-05:00:30'<br />'08:18:10.190+05:00'</td></tr>
     * <tr><td>'HH:mm:ssXXX'</td><td>'08:18:10-05:00:30'<br />'08:18:10+05:00'</td></tr>
     * <tr><td>'HH:mm.SSSSSSSSSXXX'</td><td>'08:21.000000502-05:00:30'<br />'08:21.000000502+05:00'</td></tr>
     * <tr><td>'HH:mm.SSSSSSSSXXX'</td><td>'08:10.00000337-05:00:30'<br />'08:10.00000337+05:00'</td></tr>
     * <tr><td>'HH:mm.SSSSSSSXXX'</td><td>'08:13.0000177-05:00:30'<br />'08:13.0000177+05:00'</td></tr>
     * <tr><td>'HH:mm.SSSSSSXXX'</td><td>'08:14.000739-05:00:30'<br />'08:14.000739+05:00'</td></tr>
     * <tr><td>'HH:mm.SSSSSXXX'</td><td>'08:15.00684-05:00:30'<br />'08:15.00684+05:00'</td></tr>
     * <tr><td>'HH:mm.SSSSXXX'</td><td>'08:16.0887-05:00:30'<br />'08:16.0887+05:00'</td></tr>
     * <tr><td>'HH:mm.SSSXXX'</td><td>'08:18.190-05:00:30'<br />'08:18.190+05:00'</td></tr>
     * <tr><td>'HH:mm.SSXXX'</td><td>'08:18.190-05:00:30'<br />'08:18.190+05:00'</td></tr>
     * <tr><td>'HH:mm.SXXX'</td><td>'08:18.190-05:00:30'<br />'08:18.190+05:00'</td></tr>
     * <tr><td>'HH:mmXXX'</td><td>'08:18-05:00:30'<br />'08:18+05:00'</td></tr>
     * <tr><td>'HH:mm:ss'</td><td>'08:18:10'</td></tr>
     * <tr><td>'HH:mm'</td><td>'08:18'</td></tr>
     * </tbody>
     * </table>
     */
    public static final DateTimeFormatter TIME_HHMMSS_FORMAT_2;

    static {
        TIME_HHMMSS_FORMAT_2 = new DateTimeFormatterBuilder()
                .appendValue(HOUR_OF_DAY, 2).appendLiteral(':').appendValue(MINUTE_OF_HOUR, 2)
                .optionalStart().append(OFFSET_FORMAT).optionalEnd()
                .optionalStart().appendLiteral(':').appendValue(SECOND_OF_MINUTE, 2)
                .optionalStart().appendFraction(NANO_OF_SECOND, 0, 9, true)
                .optionalStart().append(OFFSET_FORMAT)
                .toFormatter(Locale.US);
    }

    /**
     * <table>
     * <thead align='center'><tr><td colspan='2'>TIME FORMAT</td></tr></thead>
     * <tbody>
     * <tr><td>'HH:mm:ssa'</td><td>'11:30:59AM'</td></tr>
     * <tr><td>'HH:mm:ssa'</td><td>'11:30:59PM'</td></tr>
     * <tr><td>'HH:mm:ss a'</td><td>'11:30:59 AM'</td></tr>
     * <tr><td>'HH:mm:ss a'</td><td>'11:30:59 PM'</td></tr>
     * </tbody>
     * </table>
     */
    public static final DateTimeFormatter TIME_HHMMSS_FORMAT_3;

    static {
        TIME_HHMMSS_FORMAT_3 = new DateTimeFormatterBuilder()
                .appendValue(CLOCK_HOUR_OF_AMPM, 2).appendLiteral(':').appendValue(MINUTE_OF_HOUR, 2)
                .optionalStart().appendLiteral(':').appendValue(SECOND_OF_MINUTE, 2).optionalEnd()
                .optionalStart().appendLiteral(' ').appendText(AMPM_OF_DAY, TextStyle.SHORT).optionalEnd()
                .optionalStart().appendText(AMPM_OF_DAY, TextStyle.SHORT).optionalEnd()
                .toFormatter(Locale.US);
    }

    /**
     * <table>
     * <thead align='center'><tr><td colspan='2'>TIME FORMAT</td></tr></thead>
     * <tbody>
     * <tr><td>'HH.mm.ss.SSSSSSSSSZ'</td><td>'08.21.59.000000502-050030'<br />'08.21.59.000000502+0500'</td></tr>
     * <tr><td>'HH.mm.ss.SSSSSSSSZ'</td><td>'08.10.55.00000337-050030'<br />'08.10.55.00000337+0500'</td></tr>
     * <tr><td>'HH.mm.ss.SSSSSSSZ'</td><td>'08.13.39.0000177-050030'<br />'08.13.39.0000177+0500'</td></tr>
     * <tr><td>'HH.mm.ss.SSSSSSZ'</td><td>'08.14.21.000739-050030'<br />'08.14.21.000739+0500'</td></tr>
     * <tr><td>'HH.mm.ss.SSSSSZ'</td><td>'08.15.31.00684-050030'<br />'08.15.31.00684+0500'</td></tr>
     * <tr><td>'HH.mm.ss.SSSSZ'</td><td>'08.16.35.0887-050030'<br />'08.16.35.0887+0500'</td></tr>
     * <tr><td>'HH.mm.ss.SSSZ'</td><td>'08.18.10.190-050030'<br />'08.18.10.190+0500'</td></tr>
     * <tr><td>'HH.mm.ss.SSZ'</td><td>'08.18.10.190-050030'<br />'08.18.10.190+0500'</td></tr>
     * <tr><td>'HH.mm.ss.SZ'</td><td>'08.18.10.190-050030'<br />'08.18.10.190+0500'</td></tr>
     * <tr><td>'HH.mm.ssZ'</td><td>'08.18.10-050030'<br />'08.18.10+0500'</td></tr>
     * <tr><td>'HH.mm.SSSSSSSSSZ'</td><td>'08.21.000000502-050030'<br />'08.21.000000502+0500'</td></tr>
     * <tr><td>'HH.mm.SSSSSSSSZ'</td><td>'08.10.00000337-050030'<br />'08.10.00000337+0500'</td></tr>
     * <tr><td>'HH.mm.SSSSSSSZ'</td><td>'08.13.0000177-050030'<br />'08.13.0000177+0500'</td></tr>
     * <tr><td>'HH.mm.SSSSSSZ'</td><td>'08.14.000739-050030'<br />'08.14.000739+0500'</td></tr>
     * <tr><td>'HH.mm.SSSSSZ'</td><td>'08.15.00684-050030'<br />'08.15.00684+0500'</td></tr>
     * <tr><td>'HH.mm.SSSSZ'</td><td>'08.16.0887-050030'<br />'08.16.0887+0500'</td></tr>
     * <tr><td>'HH.mm.SSSZ'</td><td>'08.18.190-050030'<br />'08.18.190+0500'</td></tr>
     * <tr><td>'HH.mm.SSZ'</td><td>'08.18.190-050030'<br />'08.18.190+0500'</td></tr>
     * <tr><td>'HH.mm.SZ'</td><td>'08.18.190-050030'<br />'08.18.190+0500'</td></tr>
     * <tr><td>'HH.mmZ'</td><td>'08.18-050030'<br />'08.18+0500'</td></tr>
     * <tr><td>'HH.mm.ss.SSSSSSSSSXXX'</td><td>'08.21.59.000000502-05:00:30'<br />'08.21.59.000000502+05:00'</td></tr>
     * <tr><td>'HH.mm.ss.SSSSSSSSXXX'</td><td>'08.10.55.00000337-05:00:30'<br />'08.10.55.00000337+05:00'</td></tr>
     * <tr><td>'HH.mm.ss.SSSSSSSXXX'</td><td>'08.13.39.0000177-05:00:30'<br />'08.13.39.0000177+05:00'</td></tr>
     * <tr><td>'HH.mm.ss.SSSSSSXXX'</td><td>'08.14.21.000739-05:00:30'<br />'08.14.21.000739+05:00'</td></tr>
     * <tr><td>'HH.mm.ss.SSSSSXXX'</td><td>'08.15.31.00684-05:00:30'<br />'08.15.31.00684+05:00'</td></tr>
     * <tr><td>'HH.mm.ss.SSSSXXX'</td><td>'08.16.35.0887-05:00:30'<br />'08.16.35.0887+05:00'</td></tr>
     * <tr><td>'HH.mm.ss.SSSXXX'</td><td>'08.18.10.190-05:00:30'<br />'08.18.10.190+05:00'</td></tr>
     * <tr><td>'HH.mm.ss.SSXXX'</td><td>'08.18.10.190-05:00:30'<br />'08.18.10.190+05:00'</td></tr>
     * <tr><td>'HH.mm.ss.SXXX'</td><td>'08.18.10.190-05:00:30'<br />'08.18.10.190+05:00'</td></tr>
     * <tr><td>'HH.mm.ssXXX'</td><td>'08.18.10-05:00:30'<br />'08.18:10+05.00'</td></tr>
     * <tr><td>'HH.mm.SSSSSSSSSXXX'</td><td>'08.21.000000502-05:00:30'<br />'08.21.000000502+05:00'</td></tr>
     * <tr><td>'HH.mm.SSSSSSSSXXX'</td><td>'08.10.00000337-05:00:30'<br />'08.10.00000337+05:00'</td></tr>
     * <tr><td>'HH.mm.SSSSSSSXXX'</td><td>'08.13.0000177-05:00:30'<br />'08.13.0000177+05:00'</td></tr>
     * <tr><td>'HH.mm.SSSSSSXXX'</td><td>'08.14.000739-05:00:30'<br />'08.14.000739+05:00'</td></tr>
     * <tr><td>'HH.mm.SSSSSXXX'</td><td>'08.15.00684-05:00:30'<br />'08.15.00684+05:00'</td></tr>
     * <tr><td>'HH.mm.SSSSXXX'</td><td>'08.16.0887-05:00:30'<br />'08.16.0887+05:00'</td></tr>
     * <tr><td>'HH.mm.SSSXXX'</td><td>'08.18.190-05:00:30'<br />'08.18.190+05:00'</td></tr>
     * <tr><td>'HH.mm.SSXXX'</td><td>'08.18.190-05:00:30'<br />'08.18.190+05:00'</td></tr>
     * <tr><td>'HH.mm.SXXX'</td><td>'08.18.190-05:00:30'<br />'08.18.190+05:00'</td></tr>
     * <tr><td>'HH.mmXXX'</td><td>'08.18-05:00:30'<br />'08.18+05:00'</td></tr>
     * <tr><td>'HH.mm.ss'</td><td>'08.18.10'</td></tr>
     * <tr><td>'HH.mm'</td><td>'08.18'</td></tr>
     * </tbody>
     * </table>
     */
    public static final DateTimeFormatter TIME_HHMMSS_FORMAT_4;

    static {
        TIME_HHMMSS_FORMAT_4 = new DateTimeFormatterBuilder()
                .appendValue(HOUR_OF_DAY, 2).appendLiteral('.').appendValue(MINUTE_OF_HOUR, 2)
                .optionalStart().append(OFFSET_FORMAT).optionalEnd()
                .optionalStart().appendLiteral('.').appendValue(SECOND_OF_MINUTE, 2)
                .optionalStart().appendFraction(NANO_OF_SECOND, 0, 9, true)
                .optionalStart().append(OFFSET_FORMAT)
                .toFormatter(Locale.US);
    }

    /**
     * <table>
     * <thead align='center'><tr><td colspan='2'>DATE FORMAT</td></tr></thead>
     * <tbody>
     * <tr><td>'yyyyMMdd'</td><td>'20170829'</td></tr>
     * </tbody>
     * </table>
     */
    public static final DateTimeFormatter DF_yyyyMMdd;

    static {
        DF_yyyyMMdd = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendValue(YEAR, 4)
                .appendValue(MONTH_OF_YEAR, 2)
                .appendValue(DAY_OF_MONTH, 2)
                .toFormatter(Locale.US);
    }

    /**
     * <table>
     * <thead align='center'><tr><td colspan='2'>DATE FORMAT</td></tr></thead>
     * <tbody>
     * <tr><td>'dd-MM-yyyy'</td><td>'29-08-2017'</td></tr>
     * <tr><td>'dd/MM/yyyy'</td><td>'29/08/2017'</td></tr>
     * </tbody>
     * </table>
     */
    public static final DateTimeFormatter DF_dd_MM_yyyy;

    static {
        DF_dd_MM_yyyy = new DateTimeFormatterBuilder()
                .appendPattern("[dd-MM-yyyy][dd/MM/yyyy]")
                .toFormatter(Locale.US);
    }

    /**
     * <table>
     * <thead align='center'><tr><td colspan='2'>DATE FORMAT</td></tr></thead>
     * <tbody>
     * <tr><td>'MM-dd-yyyy'</td><td>'08-29-2017'</td></tr>
     * <tr><td>'MM/dd/yyyy'</td><td>'08/29/2017'</td></tr>
     * </tbody>
     * </table>
     */
    public static final DateTimeFormatter DF_MM_dd_yyyy;

    static {
        DF_MM_dd_yyyy = new DateTimeFormatterBuilder()
                .appendPattern("[MM-dd-yyyy][MM/dd/yyyy]")
                .toFormatter(Locale.US);
    }

    /**
     * <table>
     * <thead align='center'><tr><td colspan='2'>DATE FORMAT</td></tr></thead>
     * <tbody>
     * <tr><td>'yyyy-MM-dd'</td><td>'2016-02-29'</td></tr>
     * <tr><td>'yyyy/MM/dd'</td><td>'2016/02/29'</td></tr>
     * </tbody>
     * </table>
     */
    public static final DateTimeFormatter DF_yyyy_MM_dd;

    static {
        DF_yyyy_MM_dd = new DateTimeFormatterBuilder()
                .appendPattern("[yyyy-MM-dd][yyyy/MM/dd]")
                .toFormatter(Locale.US);
    }

    /**
     * <table>
     * <thead align='center'><tr><td colspan='2'>DATE FORMAT</td></tr></thead>
     * <tbody>
     * <tr><td>'dd MMM yyyy'</td><td>'16 Jul 2017'</td></tr>
     * <tr><td>'dd MMMM yyyy'</td><td>'16 July 2017'</td></tr>
     * </tbody>
     * </table>
     */
    public static final DateTimeFormatter DF_dd_MMMorMMMM_yyyy;

    static {
        DF_dd_MMMorMMMM_yyyy = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern("[dd MMM yyyy][dd MMMM yyyy]")
                .toFormatter(Locale.US);
    }

    /**
     * <table>
     * <thead align='center'><tr><td colspan='2'>DATE FORMAT</td></tr></thead>
     * <tbody>
     * <tr><td>'MM-dd-yy'</td><td>'06-15-17'</td></tr>
     * <tr><td>'MM/dd/yy'</td><td>'06/15/17'</td></tr>
     * </tbody>
     * </table>
     */
    public static final DateTimeFormatter DF_MM_dd_yy;

    static {
        DF_MM_dd_yy = new DateTimeFormatterBuilder()
                .appendPattern("[MM-dd-yy][MM/dd/yy]")
                .toFormatter(Locale.US);
    }

    /**
     * Date time starts with yyyyMMdd <br />
     * <table>
     * <thead align='center'><tr><td colspan='2'>DATE TIME FORMAT</td></tr></thead>
     * <tbody>
     * <tr><td>'yyyyMMdd'</td><td>'20170502'</td></tr>
     * <tr><td>'yyyyMMddHHmm'</td><td>'201705022334'</td></tr>
     * <tr><td>'yyyyMMddHHmmss'</td><td>'20170502233401'</td></tr>
     * <tr><td>'yyyyMMdd HHmm'</td><td>'20170502 2334'</td></tr>
     * <tr><td>'yyyyMMdd HHmmss'</td><td>'20170502 233401'</td></tr>
     * </tbody>
     * </table>
     */
    public static final DateTimeFormatter DTF_SW_yyyyMMdd;

    static {
        DTF_SW_yyyyMMdd = new DateTimeFormatterBuilder()
                .append(DF_yyyyMMdd)
                .optionalStart().appendLiteral(' ').append(TIME_HHMMSS_FORMAT_1).optionalEnd()
                .optionalStart().append(TIME_HHMMSS_FORMAT_1).optionalEnd()
                .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
                .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
                .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
                .parseDefaulting(ChronoField.NANO_OF_SECOND, 0)
                .toFormatter(Locale.US);
    }

    /**
     * Examples Date time formats
     * <p>
     * date format : {@link DateTimeFormatterHelper#DF_yyyy_MM_dd} and <br />
     * time format : {@link DateTimeFormatterHelper#TIME_HHMMSS_FORMAT_2}
     * </p>
     * <table>
     * <thead align='center'><tr><td colspan='2'>DATE TIME FORMAT</td></tr></thead>
     * <tbody>
     * <tr><td>'yyyy-MM-dd'</td><td>'2017-05-02'</td></tr>
     * <tr><td>'yyyy-MM-dd HH:mm'</td><td>'2017-05-02 23:15'</td></tr>
     * <tr><td>'yyyy-MM-dd HH:mm:ss'</td><td>'2017-05-02 23:15:30'</td></tr>
     * <tr><td>'yyyy-MM-dd HH:mm:ss.SSS'</td><td>'2017-05-02 23:15:30.999'</td></tr>
     * <tr><td>'yyyy-MM-dd HH:mm:ss.SSS'</td><td>'2017-05-02 23:15:30.999'</td></tr>
     * <tr><td>'yyyy/MM/dd HH:mm'</td><td>'2017/05/02 23:15'</td></tr>
     * <tr><td>'yyyy/MM/dd HH:mm:ss'</td><td>'2017/05/02 23:15:30'</td></tr>
     * <tr><td>'yyyy/MM/dd HH:mm:ss.SSS'</td><td>'2017/05/02 23:15:30.999'</td></tr>
     * <tr><td>'yyyy/MM/dd HH:mm:ss.SSSSSS'</td><td>'2017/05/02 23:15:30.999999'</td></tr>
     * <tr><td>'yyyy/MM/dd HH:mm:ss.SSSSSSSSS'</td><td>'2017/05/02 23:15:30.999999999'</td></tr>
     * </tbody>
     * </table>
     */
    public static final DateTimeFormatter DTF_SW_yyyy_MM_dd_FORMAT_1;

    static {
        DTF_SW_yyyy_MM_dd_FORMAT_1 = new DateTimeFormatterBuilder()
                .append(DF_yyyy_MM_dd)
                .optionalStart()
                .appendLiteral(' ')
                .append(TIME_HHMMSS_FORMAT_2)
                .optionalEnd()
                .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
                .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
                .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
                .parseDefaulting(ChronoField.NANO_OF_SECOND, 0)
                .toFormatter(Locale.US);
    }

    /**
     * Examples Date time formats
     * <p>
     * date format : {@link DateTimeFormatterHelper#DF_yyyy_MM_dd} and <br />
     * time format : {@link DateTimeFormatterHelper#TIME_HHMMSS_FORMAT_3}
     * </p>
     * <table>
     * <thead align='center'><tr><td colspan='2'>DATE TIME FORMAT</td></tr></thead>
     * <tbody>
     * <tr><td>'yyyy-MM-dd hh:mma'</td><td>'2017-05-02 11:15AM'</td></tr>
     * <tr><td>'yyyy-MM-dd hh:mm:ssa'</td><td>'2017-05-02 11:15:30AM'</td></tr>
     * <tr><td>'yyyy-MM-dd hh:mm a'</td><td>'2017-05-02 11:15 AM'</td></tr>
     * <tr><td>'yyyy-MM-dd hh:mm:ss a'</td><td>'2017-05-02 11:15:30 AM'</td></tr>
     * <tr><td>'yyyy/MM/dd hh:mma'</td><td>'2017/05/02 11:15PM'</td></tr>
     * <tr><td>'yyyy/MM/dd hh:mm:ssa'</td><td>'2017/05/02 11:15:30PM'</td></tr>
     * <tr><td>'yyyy/MM/dd hh:mm a'</td><td>'2017/05/02 11:15 PM'</td></tr>
     * <tr><td>'yyyy/MM/dd hh:mm:ss a'</td><td>'2017/05/02 11:15:30 PM'</td></tr>
     * </tbody>
     * </table>
     */
    public static final DateTimeFormatter DTF_SW_yyyy_MM_dd_FORMAT_2;

    static {
        DTF_SW_yyyy_MM_dd_FORMAT_2 = new DateTimeFormatterBuilder()
                .append(DF_yyyy_MM_dd)
                .optionalStart()
                .appendLiteral(' ')
                .append(TIME_HHMMSS_FORMAT_3)
                .optionalEnd()
                .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
                .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
                .parseDefaulting(ChronoField.NANO_OF_SECOND, 0)
                .toFormatter(Locale.US);
    }

    /**
     * Examples Date time formats with 'yyyy-MM-dd-' or 'yyyy/MM/dd-'
     * <p>
     * date format : {@link DateTimeFormatterHelper#DF_yyyy_MM_dd} and <br />
     * time format : {@link DateTimeFormatterHelper#TIME_HHMMSS_FORMAT_4}
     * </p>
     * <table>
     * <thead align='center'><tr><td colspan='2'>DATE TIME FORMAT</td></tr></thead>
     * <tbody>
     * <tr><td>'yyyy-MM-dd-HH.mm'</td><td>'2017-05-02 23.15'</td></tr>
     * <tr><td>'yyyy-MM-dd-HH.mm.ss'</td><td>'2017-05-02 23.15.30'</td></tr>
     * <tr><td>'yyyy-MM-dd-HH.mm.ss.SSS'</td><td>'2017-05-02 23.15.30.999'</td></tr>
     * <tr><td>'yyyy/MM/dd-HH.mm'</td><td>'2017/05/02 23.15'</td></tr>
     * <tr><td>'yyyy/MM/dd-HH.mm.ss'</td><td>'2017/05/02 23.15.30'</td></tr>
     * <tr><td>'yyyy/MM/dd-HH.mm.ss.SSS'</td><td>'2017/05/02 23.15.30.999'</td></tr>
     * <tr><td>'yyyy/MM/dd-HH.mm.ss.SSSSSS'</td><td>'2017/05/02 23.15.30.999999'</td></tr>
     * <tr><td>'yyyy/MM/dd-HH.mm.ss.SSSSSSSSS'</td><td>'2017/05/02 23.15.30.999999999'</td></tr>
     * </tbody>
     * </table>
     */
    public static final DateTimeFormatter DTF_SW_yyyy_MM_dd_FORMAT_3;

    static {
        DTF_SW_yyyy_MM_dd_FORMAT_3 = new DateTimeFormatterBuilder()
                .append(DF_yyyy_MM_dd)
                .appendLiteral('-')
                .append(TIME_HHMMSS_FORMAT_4)
                .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
                .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
                .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
                .parseDefaulting(ChronoField.NANO_OF_SECOND, 0)
                .toFormatter(Locale.US);
    }

    /**
     * Examples Date time formats
     * <p>
     * date format : {@link DateTimeFormatterHelper#DF_MM_dd_yyyy} and <br />
     * time format : {@link DateTimeFormatterHelper#TIME_HHMMSS_FORMAT_2}
     * </p>
     * <table>
     * <thead align='center'><tr><td colspan='2'>DATE TIME FORMAT</td></tr></thead>
     * <tbody>
     * <tr><td>'MM-dd-yyyy'</td><td>'05-02-2017'</td></tr>
     * <tr><td>'MM-dd-yyyy HH:mm'</td><td>'05-02-2017 23:15'</td></tr>
     * <tr><td>'MM-dd-yyyy HH:mm:ss'</td><td>'05-02-2017 23:15:30'</td></tr>
     * <tr><td>'MM-dd-yyyy HH:mm:ss.SSS'</td><td>'05-02-2017 23:15:30.999'</td></tr>
     * <tr><td>'MM/dd/yyyy'</td><td>'05/02/2017'</td></tr>
     * <tr><td>'MM/dd/yyyy HH:mm'</td><td>'05/02/2017 23:15'</td></tr>
     * <tr><td>'MM/dd/yyyy HH:mm:ss'</td><td>'05/02/2017 23:15:30'</td></tr>
     * <tr><td>'MM/dd/yyyy HH:mm:ss.SSS'</td><td>'05/02/2017 23:15:30.999'</td></tr>
     * <tr><td>'MM/dd/yyyy HH:mm:ss.SSSSSS'</td><td>'05/02/2017 23:15:30.999999'</td></tr>
     * <tr><td>'MM/dd/yyyy HH:mm:ss.SSSSSSSSS'</td><td>'05/02/2017 23:15:30.999999999'</td></tr>
     * </tbody>
     * </table>
     */
    public static final DateTimeFormatter DTF_SW_MM_dd_yyyy_FORMAT_1;

    static {
        DTF_SW_MM_dd_yyyy_FORMAT_1 = new DateTimeFormatterBuilder()
                .append(DF_MM_dd_yyyy)
                .optionalStart()
                .appendLiteral(' ')
                .append(TIME_HHMMSS_FORMAT_2)
                .optionalEnd()
                .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
                .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
                .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
                .parseDefaulting(ChronoField.NANO_OF_SECOND, 0)
                .toFormatter(Locale.US);
    }

    /**
     * Examples Date time formats
     * <p>
     * date format : {@link DateTimeFormatterHelper#DF_MM_dd_yyyy} and <br />
     * time format : {@link DateTimeFormatterHelper#TIME_HHMMSS_FORMAT_3}
     * </p>
     * <table>
     * <thead align='center'><tr><td colspan='2'>DATE TIME FORMAT</td></tr></thead>
     * <tbody>
     * <tr><td>'MM-dd-yyyy hh:mma'</td><td>'05-02-2017 11:15AM'</td></tr>
     * <tr><td>'MM-dd-yyyy hh:mm a'</td><td>'05-02-2017 11:15 AM'</td></tr>
     * <tr><td>'MM-dd-yyyy hh:mm:ssa'</td><td>'05-02-2017 11:15:30AM'</td></tr>
     * <tr><td>'MM-dd-yyyy hh:mm:ss a'</td><td>'05-02-2017 11:15:30 AM'</td></tr>
     * <tr><td>'MM/dd/yyyy hh:mma'</td><td>'05/02/2017 11:15PM'</td></tr>
     * <tr><td>'MM/dd/yyyy hh:mm a'</td><td>'05/02/2017 11:15 PM'</td></tr>
     * <tr><td>'MM/dd/yyyy hh:mm:ssa'</td><td>'05/02/2017 11:15:30PM'</td></tr>
     * <tr><td>'MM/dd/yyyy hh:mm:ss a'</td><td>'05/02/2017 11:15:30 PM'</td></tr>
     * </tbody>
     * </table>
     */
    public static final DateTimeFormatter DTF_SW_MM_dd_yyyy_FORMAT_2;

    static {
        DTF_SW_MM_dd_yyyy_FORMAT_2 = new DateTimeFormatterBuilder()
                .append(DF_MM_dd_yyyy)
                .optionalStart()
                .appendLiteral(' ')
                .append(TIME_HHMMSS_FORMAT_3)
                .optionalEnd()
                .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
                .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
                .parseDefaulting(ChronoField.NANO_OF_SECOND, 0)
                .toFormatter(Locale.US);
    }

    /**
     * Examples Date time formats
     * <p>
     * date format : {@link DateTimeFormatterHelper#DF_dd_MM_yyyy} and <br />
     * time format : {@link DateTimeFormatterHelper#TIME_HHMMSS_FORMAT_2}
     * </p>
     * <table>
     * <thead align='center'><tr><td colspan='2'>DATE TIME FORMAT</td></tr></thead>
     * <tbody>
     * <tr><td>'dd-MM-yyyy HH:mm'</td><td>'02-05-2017 23.15'</td></tr>
     * <tr><td>'dd-MM-yyyy HH:mm.ss'</td><td>'02-05-2017 23.15.30'</td></tr>
     * <tr><td>'dd-MM-yyyy HH:mm:ss.SSS'</td><td>'02-05-2017 23.15.30.999'</td></tr>
     * <tr><td>'dd/MM/yyyy HH:mm'</td><td>'02/05/2017 23.15'</td></tr>
     * <tr><td>'dd/MM/yyyy HH:mm:ss'</td><td>'02/05/2017 23.15.30'</td></tr>
     * <tr><td>'dd/MM/yyyy HH:mm:ss.SSS'</td><td>'02/05/2017 23.15.30.999'</td></tr>
     * <tr><td>'dd/MM/yyyy HH:mm:ss.SSSSSS'</td><td>'02/05/2017 23.15.30.999999'</td></tr>
     * <tr><td>'dd/MM/yyyy HH:mm:ss.SSSSSSSSS'</td><td>'02/05/2017 23.15.30.999999999'</td></tr>
     * </tbody>
     * </table>
     */
    public static final DateTimeFormatter DTF_SW_dd_MM_yyyy_FORMAT_1;

    static {
        DTF_SW_dd_MM_yyyy_FORMAT_1 = new DateTimeFormatterBuilder()
                .append(DF_dd_MM_yyyy)
                .optionalStart()
                .appendLiteral(' ')
                .append(TIME_HHMMSS_FORMAT_2)
                .optionalEnd()
                .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
                .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
                .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
                .parseDefaulting(ChronoField.NANO_OF_SECOND, 0)
                .toFormatter(Locale.US);
    }

    /**
     * Examples Date time formats
     * <p>
     * date format : {@link DateTimeFormatterHelper#DF_dd_MM_yyyy} and <br />
     * time format : {@link DateTimeFormatterHelper#TIME_HHMMSS_FORMAT_3}
     * </p>
     * <table>
     * <thead align='center'><tr><td colspan='2'>DATE TIME FORMAT</td></tr></thead>
     * <tbody>
     * <tr><td>'dd-MM-yyyy hh:mma'</td><td>'25-02-2017 11:15AM'</td></tr>
     * <tr><td>'dd-MM-yyyy hh:mm a'</td><td>'25-02-2017 11:15 AM'</td></tr>
     * <tr><td>'dd-MM-yyyy hh:mm:ssa'</td><td>'25-02-2017 11:15:30AM'</td></tr>
     * <tr><td>'dd-MM-yyyy hh:mm:ss a'</td><td>'25-02-2017 11:15:30 AM'</td></tr>
     * <tr><td>'dd/MM/yyyy hh:mma'</td><td>'25/02/2017 11:15PM'</td></tr>
     * <tr><td>'dd/MM/yyyy hh:mm a'</td><td>'25/02/2017 11:15 PM'</td></tr>
     * <tr><td>'dd/MM/yyyy hh:mm:ssa'</td><td>'25/02/2017 11:15:30PM'</td></tr>
     * <tr><td>'dd/MM/yyyy hh:mm:ss a'</td><td>'25/02/2017 11:15:30 PM'</td></tr>
     * </tbody>
     * </table>
     */
    public static final DateTimeFormatter DTF_SW_dd_MM_yyyy_FORMAT_2;

    static {
        DTF_SW_dd_MM_yyyy_FORMAT_2 = new DateTimeFormatterBuilder()
                .append(DF_dd_MM_yyyy)
                .optionalStart()
                .appendLiteral(' ')
                .append(TIME_HHMMSS_FORMAT_3)
                .optionalEnd()
                .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
                .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
                .parseDefaulting(ChronoField.NANO_OF_SECOND, 0)
                .toFormatter(Locale.US);
    }

    /**
     * Examples Date time formats
     * <p>
     * date format : {@link DateTimeFormatterHelper#DF_dd_MMMorMMMM_yyyy} and <br />
     * time format : {@link DateTimeFormatterHelper#TIME_HHMMSS_FORMAT_2}
     * </p>
     * <table>
     * <thead align='center'><tr><td colspan='2'>DATE TIME FORMAT</td></tr></thead>
     * <tbody>
     * <tr><td>'dd MMM yyyy'</td><td>'15 Jun 2017'</td></tr>
     * <tr><td>'dd MMM yyyy HH:mm'</td><td>'15 Jun 2017 23:15'</td></tr>
     * <tr><td>'dd MMM yyyy HH:mm.ss'</td><td>'15 Jun 2017 23:15:30'</td></tr>
     * <tr><td>'dd MMM yyyy HH:mm:ss.SSS'</td><td>'15 Jun 2017 23:15:30.999'</td></tr>
     * <tr><td>'dd MMMM yyyy'</td><td>'30 November 2017'</td></tr>
     * <tr><td>'dd MMMM yyyy HH:mm'</td><td>'30 November 2017 23:15'</td></tr>
     * <tr><td>'dd MMMM yyyy HH:mm:ss'</td><td>'30 November 2017 23:15:30'</td></tr>
     * <tr><td>'dd MMMM yyyy HH:mm:ss.SSS'</td><td>'30 November 2017 23:15:30.999'</td></tr>
     * <tr><td>'dd MMMM yyyy HH:mm:ss.SSSSSS'</td><td>'30 November 2017 23:15:30.999999'</td></tr>
     * <tr><td>'dd MMMM yyyy HH:mm:ss.SSSSSSSSS'</td><td>'30 November 2017 23:15:30.999999999'</td></tr>
     * </tbody>
     * </table>
     */
    public static final DateTimeFormatter DTF_SW_dd_MMMorMMMM_yyyy_FORMAT_1;

    static {
        DTF_SW_dd_MMMorMMMM_yyyy_FORMAT_1 = new DateTimeFormatterBuilder()
                .append(DF_dd_MMMorMMMM_yyyy)
                .optionalStart()
                .appendLiteral(' ')
                .append(TIME_HHMMSS_FORMAT_2)
                .optionalEnd()
                .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
                .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
                .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
                .parseDefaulting(ChronoField.NANO_OF_SECOND, 0)
                .toFormatter(Locale.US);
    }

    /**
     * Examples Date time formats
     * <p>
     * date format : {@link DateTimeFormatterHelper#DF_dd_MMMorMMMM_yyyy} and <br />
     * time format : {@link DateTimeFormatterHelper#TIME_HHMMSS_FORMAT_3}
     * </p>
     * <table>
     * <thead align='center'><tr><td colspan='2'>DATE TIME FORMAT</td></tr></thead>
     * <tbody>
     * <tr><td>'dd MMM yyyy hh:mma'</td><td>'15 Jun 2017 11:15AM'</td></tr>
     * <tr><td>'dd MMM yyyy hh:mm a'</td><td>'15 Jun 2017 11:15 AM'</td></tr>
     * <tr><td>'dd MMM yyyy hh:mm:ssa'</td><td>'15 Jun 2017 11:15:30AM'</td></tr>
     * <tr><td>'dd MMM yyyy hh:mm:ss a'</td><td>'15 Jun 2017 11:15:30 AM'</td></tr>
     * <tr><td>'dd MMMM yyyy hh:mma'</td><td>'30 November 2017 11:15PM'</td></tr>
     * <tr><td>'dd MMMM yyyy hh:mm a'</td><td>'30 November 2017 11:15 PM'</td></tr>
     * <tr><td>'dd MMMM yyyy hh:mm:ssa'</td><td>'30 November 2017 11:15:30PM'</td></tr>
     * <tr><td>'dd MMMM yyyy hh:mm:ss a'</td><td>'30 November 2017 11:15:30 PM'</td></tr>
     * </tbody>
     * </table>
     */
    public static final DateTimeFormatter DTF_SW_dd_MMMorMMMM_yyyy_FORMAT_2;

    static {
        DTF_SW_dd_MMMorMMMM_yyyy_FORMAT_2 = new DateTimeFormatterBuilder()
                .append(DF_dd_MMMorMMMM_yyyy)
                .optionalStart()
                .appendLiteral(' ')
                .append(TIME_HHMMSS_FORMAT_3)
                .optionalEnd()
                .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
                .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
                .parseDefaulting(ChronoField.NANO_OF_SECOND, 0)
                .toFormatter(Locale.US);
    }

    /**
     * Examples Date time formats
     * <p>
     * date format : {@link DateTimeFormatterHelper#DF_MM_dd_yy} and <br />
     * time format : {@link DateTimeFormatterHelper#TIME_HHMMSS_FORMAT_2}
     * </p>
     * <table>
     * <thead align='center'><tr><td colspan='2'>DATE TIME FORMAT</td></tr></thead>
     * <tbody>
     * <tr><td>'MM-dd-yy'</td><td>'12-25-17'</td></tr>
     * <tr><td>'MM-dd-yy HH:mm'</td><td>'12-25-17 23.15'</td></tr>
     * <tr><td>'MM-dd-yy HH:mm.ss'</td><td>'12-25-17 23.15.30'</td></tr>
     * <tr><td>'MM-dd-yy HH:mm:ss.SSS'</td><td>'12-25-17 23.15.30.999'</td></tr>
     * <tr><td>'MM/dd/yy'</td><td>'12/25/17'</td></tr>
     * <tr><td>'MM/dd/yy HH:mm'</td><td>'12/25/17 23.15'</td></tr>
     * <tr><td>'MM/dd/yy HH:mm:ss'</td><td>'12/25/17 23.15.30'</td></tr>
     * <tr><td>'MM/dd/yy HH:mm:ss.SSS'</td><td>'12/25/17 23.15.30.999'</td></tr>
     * <tr><td>'MM/dd/yy HH:mm:ss.SSSSSS'</td><td>'12/25/17 23.15.30.999999'</td></tr>
     * <tr><td>'MM/dd/yy HH:mm:ss.SSSSSSSSS'</td><td>'12/25/17 23.15.30.999999999'</td></tr>
     * </tbody>
     * </table>
     */
    public static final DateTimeFormatter DTF_SW_MM_dd_yy_FORMAT_1;

    static {
        DTF_SW_MM_dd_yy_FORMAT_1 = new DateTimeFormatterBuilder()
                .append(DF_MM_dd_yy)
                .optionalStart()
                .appendLiteral(' ')
                .append(TIME_HHMMSS_FORMAT_2)
                .optionalEnd()
                .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
                .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
                .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
                .parseDefaulting(ChronoField.NANO_OF_SECOND, 0)
                .toFormatter(Locale.US);
    }

    /**
     * Examples Date time formats
     * <p>
     * date format : {@link DateTimeFormatterHelper#DF_MM_dd_yy} and <br />
     * time format : {@link DateTimeFormatterHelper#TIME_HHMMSS_FORMAT_3}
     * </p>
     * <table>
     * <thead align='center'><tr><td colspan='2'>DATE TIME FORMAT</td></tr></thead>
     * <tbody>
     * <tr><td>'MM-dd-yy hh:mma'</td><td>'25-02-2017 11:15AM'</td></tr>
     * <tr><td>'MM-dd-yy hh:mm a'</td><td>'25-02-2017 11:15 AM'</td></tr>
     * <tr><td>'MM-dd-yy hh:mm:ssa'</td><td>'25-02-2017 11:15:30AM'</td></tr>
     * <tr><td>'MM-dd-yy hh:mm:ss a'</td><td>'25-02-2017 11:15:30 AM'</td></tr>
     * <tr><td>'MM/dd/yy hh:mma'</td><td>'25/02/2017 11:15PM'</td></tr>
     * <tr><td>'MM/dd/yy hh:mm a'</td><td>'25/02/2017 11:15 PM'</td></tr>
     * <tr><td>'MM/dd/yy hh:mm:ssa'</td><td>'25/02/2017 11:15:30PM'</td></tr>
     * <tr><td>'MM/dd/yy hh:mm:ss a'</td><td>'25/02/2017 11:15:30 PM'</td></tr>
     * </tbody>
     * </table>
     */
    public static final DateTimeFormatter DTF_SW_MM_dd_yy_FORMAT_2;

    static {
        DTF_SW_MM_dd_yy_FORMAT_2 = new DateTimeFormatterBuilder()
                .append(DF_MM_dd_yy)
                .optionalStart()
                .appendLiteral(' ')
                .append(TIME_HHMMSS_FORMAT_3)
                .optionalEnd()
                .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
                .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
                .parseDefaulting(ChronoField.NANO_OF_SECOND, 0)
                .toFormatter(Locale.US);
    }

    /**
     * <table>
     * <thead align='center'><tr><td colspan='2'>DATE TIME FORMAT</td></tr></thead>
     * <tbody>
     * <tr><td>'E MMM dd HH:mm:ss z yyyy'</td><td>'Tue May 30 11:40:51 EST 2017'</td></tr>
     * </tbody>
     * </table>
     */
    public static final DateTimeFormatter DATE_FORMAT_E_MMM_dd_HH_mm_ss_z_yyyy;

    static {
        DATE_FORMAT_E_MMM_dd_HH_mm_ss_z_yyyy = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendText(DAY_OF_WEEK, TextStyle.SHORT)
                .appendLiteral(' ')
                .appendText(MONTH_OF_YEAR, TextStyle.SHORT)
                .appendLiteral(' ')
                .appendValue(DAY_OF_MONTH, 2)
                .appendLiteral(' ')
                .appendValue(HOUR_OF_DAY, 2)
                .appendLiteral(':')
                .appendValue(MINUTE_OF_HOUR, 2)
                .appendLiteral(':')
                .appendValue(SECOND_OF_MINUTE, 2)
                .appendLiteral(' ')
                .appendZoneText(TextStyle.SHORT)
                .appendLiteral(' ')
                .appendValue(YEAR, 4)
                .toFormatter(Locale.US);
    }

    /**
     * Examples Date time formats
     * <p>
     * date format : {@link DateTimeFormatter#ISO_LOCAL_DATE} and <br />
     * time format : {@link DateTimeFormatterHelper#TIME_HHMMSS_FORMAT_2}
     * </p>
     * <table>
     * <thead align='center'><tr><td colspan='2'>DATE TIME FORMAT</td></tr></thead>
     * <tbody>
     * <tr><td>'yyyy-MM-dd'T'HH:mm:ss.SSSZ'</td><td>'2017-05-02T10:19:24.972-0500'</td></tr>
     * <tr><td>'yyyy-MM-dd'T'HH:mm:ssZ'</td><td>'2017-05-02T10:20:07-0100'</td></tr>
     * <tr><td>'yyyy-MM-dd'T'HH:mm:ss.SSSXXX'</td><td>'2017-05-02T10:20:07.057-05:00:30'</td></tr>
     * <tr><td>'yyyy-MM-dd'T'HH:mm:ssXXX'</td><td>'2017-05-02T10:20:07+01:00:01'</td></tr>
     * <tr><td>'yyyy-MM-dd'T'HH:mmZ'</td><td>'2017-05-02T10:20-0400'</td></tr>
     * <tr><td>'yyyy-MM-dd'T'HH:mmXXX'</td><td>'2017-05-02T10:20-04:00:30'</td></tr>
     * </tbody>
     * </table>
     */
    public static final DateTimeFormatter DATE_FORMAT_ISO_LOCAL_DATE_TIME;

    static {
        DATE_FORMAT_ISO_LOCAL_DATE_TIME = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .append(DateTimeFormatter.ISO_LOCAL_DATE)
                .appendLiteral('T')
                .append(TIME_HHMMSS_FORMAT_2)
                .toFormatter(Locale.US);
    }

    /**
     * <table>
     * <thead align='center'><tr><td colspan='2'>DATE TIME FORMAT</td></tr></thead>
     * <tbody>
     * <tr><td>'EEE, dd MMM yyyy HH:mm:ss Z'</td><td>'Tue, 30 May 2017 17:04:34 -0400'</td></tr>
     * </tbody>
     * </table>
     */
    public static final DateTimeFormatter START_WITH_EEE_dd_MMM_yyyy_HH_mm_ss_Z;

    static {
        START_WITH_EEE_dd_MMM_yyyy_HH_mm_ss_Z = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendText(DAY_OF_WEEK, TextStyle.SHORT)
                .appendLiteral(',')
                .appendLiteral(' ')
                .appendValue(DAY_OF_MONTH, 2)
                .appendLiteral(' ')
                .appendText(MONTH_OF_YEAR, TextStyle.SHORT)
                .appendLiteral(' ')
                .appendValue(YEAR_OF_ERA, 4, 19, SignStyle.EXCEEDS_PAD)
                .appendLiteral(' ')
                .appendValue(HOUR_OF_DAY, 2)
                .appendLiteral(':')
                .appendValue(MINUTE_OF_HOUR, 2)
                .appendLiteral(':')
                .appendValue(SECOND_OF_MINUTE, 2)
                .appendLiteral(' ')
                .appendOffset("+HHMMSS", "+000000")
                .toFormatter(Locale.US);
    }

    /**
     * <code>new Date().toString()</code> = 'Mon Dec 25 23:34:59 UTC 2017'
     */
    private static final String PD_TIME_JAVA_DEFAULT = "^[A-Za-z]{3}\\s[A-Za-z]{3}\\s\\d{2}\\s\\d{2}:\\d{2}:\\d{2}\\s[A-Za-z]{3}\\s\\d{4}";
    // RCF-2822 and RFC-1123
    private static final String PD_TIME_RFC_2822 = "^[A-Za-z]{3},\\s\\d{2}\\s[A-Za-z]{3}\\s\\d{4}\\s\\d{2}:\\d{2}:\\d{2}\\s[+|-]\\d{4,6}";
    // ISO-8601
    private static final String PD_TIME_ISO = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(:\\d{2})?";
    // Pattern Date
    private static final String PD_1 = "^\\d{2}[/|-]\\d{2}[/|-]\\d{2}";
    private static final String PD_2 = "^\\d{2}[/|-]\\d{2}[/|-]\\d{4}";
    private static final String PD_3 = "^\\d{4}[/|-]\\d{2}[/|-]\\d{2}";
    private static final String PD_4 = "^\\d{2}\\s[A-Za-z]{3,10}\\s\\d{4}";
    private static final String PD_6 = "^\\d{8}";

    // Pattern Time
    private static final String PTIME_1 = "\\d{2}:\\d{2}(:\\d{2})?";
    private static final String PTIME_2 = "\\d{2}:\\d{2}(:\\d{2})?\\s?(A|P)M";
    private static final String PTIME_3 = "\\d{2}.\\d{2}(.\\d{2})?";
    private static final String PTIME_4 = "\\d{4,6}";

    // nano seconds
    private static final String P_NANO_SECONDS = "(.\\d{1,9})?";

    // Pattern offset
    private static final String P_OFFSET_0 = "[z|Z]?";
    private static final String P_OFFSET_1 = "\\d{4,6}";
    private static final String P_OFFSET_2 = "\\d{2}:\\d{2}(:\\d{2})?";
    private static final String P_OFFSET_ALL = String.format("(%1s|[+|-](%2s|%3s))", P_OFFSET_0, P_OFFSET_1, P_OFFSET_2);

    // Pattern Time with nano second and offset optional
    private static final String PTIME_5_NUMBERS = String.format("\\s?(%1s)?", PTIME_4);
    private static final String PTIME_4_NANO_SECOND_OFFSET = String.format("(-%1s)?", PTIME_3 + P_NANO_SECONDS + P_OFFSET_ALL);
    private static final String PTIME_2_NANO_SECOND_OFFSET = String.format("(\\s%1s)?", PTIME_1 + P_NANO_SECONDS + P_OFFSET_ALL);
    private static final String PTIME_3_AM_PM = String.format("\\s%1s", PTIME_2);

    private static final Map<String, List<DateTimeFormatter>> DATE_FORMAT_REGEXPS = new LinkedHashMap<String, List<DateTimeFormatter>>();

    static {
        // all combination of ISO Date
        DATE_FORMAT_REGEXPS.put(PD_TIME_ISO + P_NANO_SECONDS + P_OFFSET_ALL, Collections.<DateTimeFormatter>singletonList(DATE_FORMAT_ISO_LOCAL_DATE_TIME));
        // literal date
        DATE_FORMAT_REGEXPS.put(PD_TIME_RFC_2822, Collections.<DateTimeFormatter>singletonList(START_WITH_EEE_dd_MMM_yyyy_HH_mm_ss_Z));
        DATE_FORMAT_REGEXPS.put(PD_TIME_JAVA_DEFAULT, Collections.<DateTimeFormatter>singletonList(DATE_FORMAT_E_MMM_dd_HH_mm_ss_z_yyyy));
        DATE_FORMAT_REGEXPS.put(PD_4 + PTIME_2_NANO_SECOND_OFFSET, Collections.<DateTimeFormatter>singletonList(DTF_SW_dd_MMMorMMMM_yyyy_FORMAT_1));
        DATE_FORMAT_REGEXPS.put(PD_4 + PTIME_3_AM_PM, Collections.<DateTimeFormatter>singletonList(DTF_SW_dd_MMMorMMMM_yyyy_FORMAT_2));

        // date starts with 'yyyyMMdd'
        DATE_FORMAT_REGEXPS.put(PD_6 + PTIME_5_NUMBERS, Collections.<DateTimeFormatter>singletonList(DTF_SW_yyyyMMdd));

        // date starts with 'MM/dd/yyyy', 'MM-dd-yyyy', 'dd/MM/yyyy' o 'dd-MM-yyyy'
        DATE_FORMAT_REGEXPS.put(PD_2 + PTIME_2_NANO_SECOND_OFFSET, Arrays.asList(DTF_SW_MM_dd_yyyy_FORMAT_1, DTF_SW_dd_MM_yyyy_FORMAT_1));
        DATE_FORMAT_REGEXPS.put(PD_2 + PTIME_3_AM_PM, Arrays.asList(DTF_SW_MM_dd_yyyy_FORMAT_2, DTF_SW_dd_MM_yyyy_FORMAT_2));

        // date starts with 'yyyy/MM/dd' 0 'yyyy-MM-dd'
        DATE_FORMAT_REGEXPS.put(PD_3 + PTIME_2_NANO_SECOND_OFFSET, Collections.<DateTimeFormatter>singletonList(DTF_SW_yyyy_MM_dd_FORMAT_1));
        DATE_FORMAT_REGEXPS.put(PD_3 + PTIME_3_AM_PM, Collections.<DateTimeFormatter>singletonList(DTF_SW_yyyy_MM_dd_FORMAT_2));

        // date starts with 'yyyy-MM-dd-HH.mm.ss'
        DATE_FORMAT_REGEXPS.put(PD_3 + PTIME_4_NANO_SECOND_OFFSET, Collections.<DateTimeFormatter>singletonList(DTF_SW_yyyy_MM_dd_FORMAT_3));

        // date starts with 'MM/dd/yy' o 'MM-dd-yy'
        DATE_FORMAT_REGEXPS.put(PD_1 + PTIME_2_NANO_SECOND_OFFSET, Collections.<DateTimeFormatter>singletonList(DTF_SW_MM_dd_yy_FORMAT_1));
        DATE_FORMAT_REGEXPS.put(PD_1 + PTIME_3_AM_PM, Collections.<DateTimeFormatter>singletonList(DTF_SW_MM_dd_yy_FORMAT_2));
    }


    public static List<DateTimeFormatter> getDateTimeFormatOfDateString(String dateString) {

        for (String patterDate : DATE_FORMAT_REGEXPS.keySet()) {
            if (dateString.matches(patterDate)) {
                return DATE_FORMAT_REGEXPS.get(patterDate);
            }
        }
        return null;
    }

    public static Date parseDateText(String dateString, List<DateTimeFormatter> dateTimeFormatOfDateStringList) throws Exception {
        if (dateTimeFormatOfDateStringList != null) {
            List<String> listErrorMessage = new ArrayList<>();
            List<LocalDateTime> listLocalDateTime = new ArrayList<>();
            for (DateTimeFormatter dateTimeFormatter : dateTimeFormatOfDateStringList) {
                try {
                    LocalDateTime localDateTimeTemp = LocalDateTime.parse(dateString, dateTimeFormatter);
                    if (!listLocalDateTime.contains(localDateTimeTemp)) {
                        listLocalDateTime.add(localDateTimeTemp);
                    }
                } catch (Exception e) {
                    logger.trace("One of the formats does not match:" + dateTimeFormatter, e);
                    String message = e.getMessage();
                    String matchesPattern = matchesPattern(PATTERN_WORDS, message);
                    if (matchesPattern != null) {
                        message = message.replaceAll(matchesPattern, CHRONO_FIELDS_REPLACE.get(matchesPattern));
                    }
                    if (!listErrorMessage.contains(message)) {
                        listErrorMessage.add(message);
                    }
                }
            }
            if (!listLocalDateTime.isEmpty()) {
                if (listErrorMessage.isEmpty() && listLocalDateTime.size() > 1) {
                    throw new Exception("There are more date formats that match with: '" + dateString + "'");
                }
                return Date.from(listLocalDateTime.get(0).atZone(ZoneId.systemDefault()).toInstant());
            } else if (!listErrorMessage.isEmpty()) {
                throw new Exception(listErrorMessage.get(0));
            }
        }
        throw new Exception("Not found date format for: '" + dateString + "'");
    }

    public static Date parseDateTextAndDetermineFormat(String dateString) throws Exception {
        return parseDateText(dateString, getDateTimeFormatOfDateString(dateString));
    }

    private static String matchesPattern(Pattern p, String sentence) {
        Matcher m = p.matcher(sentence);
        if (m.find()) {
            return m.group();
        }
        return null;
    }

}
