package com.tierconnect.riot.commons;

import com.tierconnect.riot.commons.utils.MomentDateFormatUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Date;
import java.util.Locale;

/**
 * Created by vealaro on 4/17/17.
 */
public class DateFormatAndTimeZone {

    public static final String JAVA_ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    public static final String MOMENT_ISO_FORMAT = "YYYY-MM-DD[T]HH:mm:ss.SSS[]ZZ";
    private String momentDateFormat = MOMENT_ISO_FORMAT;
    private String timeZone = "UTC";

    public DateFormatAndTimeZone() {
    }

    public DateFormatAndTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public DateFormatAndTimeZone(String timeZone, String momentDateFormat) {
        this.timeZone = timeZone;
        this.momentDateFormat = momentDateFormat;
    }

    public String format(Long timestamp) {
        return format(new Date(timestamp));
    }

    public String format(Date date) {
        return MomentDateFormatUtils.formatDate(date, timeZone, momentDateFormat);
    }

    public String getISODateTimeFormatWithTimeZone(Long timestamp) {
        Instant instant = Instant.ofEpochMilli(timestamp);
        ZonedDateTime zonedDateTime = instant.atZone(ZoneId.of(timeZone));
        return String.valueOf(zonedDateTime);
    }

    public String getISODateTimeFormatWithTimeZone(Date date) {
        return getISODateTimeFormatWithTimeZone(date.getTime());
    }

    public String getISODateTimeFormatWithoutTimeZone(Date date) {
        return getISODateTimeFormatWithoutTimeZone(date.getTime());
    }

    public String getISODateTimeFormatWithoutTimeZone(Long timestamp) {
        Instant instant = Instant.ofEpochMilli(timestamp);
        ZonedDateTime zonedDateTime = instant.atZone(ZoneId.of(timeZone));
        return zonedDateTime.toLocalDateTime() + "" + zonedDateTime.getOffset().getId().replaceAll(":", "");
    }

    public Long parseISOStringToTimeStamp(String isoDate) {
        return parseISOStringToDate(isoDate).getTime();
    }

    public Date parseISOStringToDate(String isoDate) {
        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .optionalStart().appendOffset("+HH:MM:ss", "Z").optionalEnd()
                .optionalStart().appendOffset("+HHMMSS", "Z").toFormatter(Locale.US);

        ZonedDateTime zonedDateTime = ZonedDateTime.parse(isoDate, formatter);
        return Date.from(zonedDateTime.toInstant());
    }

    @Override
    public String toString() {
        return "DATE FORMAT='" + momentDateFormat + "', TIMEZONE='" + timeZone + "'}";
    }

    public String getMomentDateFormat() {
        return momentDateFormat;
    }

    public String getTimeZone() {
        return timeZone;
    }
}
