package com.tierconnect.riot.commons.utils;

import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.commons.DateFormatAndTimeZone;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;
import org.joda.time.format.ISODateTimeFormat;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Helper class that determines the values of the to and from dates. If the relative flag is set
 * it has priority over the from and to values.
 */
public class DateHelper {
    private static Logger logger = Logger.getLogger(DateHelper.class);
    private Date startDate;
    private Date endDate;

    private boolean relativePast;

    private Date parseWithOffset(Long timeStamp, String offsetTimeZone) {
        LocalDateTime ldt = (new Date(timeStamp)).toInstant().atZone(ZoneId.of(offsetTimeZone)).toLocalDateTime();
        return Date.from(ldt.atZone(ZoneId.of(offsetTimeZone)).toInstant());
    }

    /**
     * This constructor activates the relative past option. The start date is null (start of everything)...
     * and the end date is the calculated relative date.
     * <p>
     * The difference when relative date is off is that end date is
     * the time of creation of this object
     */
    private DateHelper(Builder builder) {
        Long from = builder.from;
        Long to = builder.to;

        String relativeOpt = builder.relativeOpt;
        this.relativePast = builder.relativePast;

        if (relativeOpt.compareToIgnoreCase("NOW") != 0) {
            if (from != null) {
                startDate = parseWithOffset(from, builder.offsetTimeZone);
                logger.info("Start Date: " + from + ", " + startDate.getTime());
            }
            if (to != null) {
                endDate = parseWithOffset(to, builder.offsetTimeZone);
                logger.info("End Date: " + to + ", " + endDate.getTime());
            }
            logger.debug("Date range " + from() + " to " + to());
        }
    }


    /*Constructor to set the dates */
    public DateHelper(String relativeDate, Date now) {
        RelativeDateUtil relDateUtil = new RelativeDateUtil();
        relDateUtil.setRelativeDate(relativeDate, now, null);
        startDate = relDateUtil.getStartDate();
        endDate = relDateUtil.getEndDate();

        logger.debug("Date range " + from() + " to " + to());
    }

    public DateHelper(String relativeDate, Date now, String offsetTimeZone) {
        RelativeDateUtil relDateUtil = new RelativeDateUtil();
        relDateUtil.setRelativeDate(relativeDate, now, offsetTimeZone);
        startDate = relDateUtil.getStartDate();
        endDate = relDateUtil.getEndDate();

        logger.debug("Date range " + from() + " to " + to());
    }

    public DateHelper(Date startDate, Date endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
        relativePast = false;
    }

    public Date to() {
        return relativePast ? startDate : endDate;
    }

    public Date from() {
        return relativePast ? null : startDate;
    }

    public static class Builder {
        private Long from;
        private Long to;

        private boolean relativePast = false;

        private String relativeOpt = "";

        private String offsetTimeZone = Constants.DEFAULT_TIME_ZONE;


        public Builder range(Long from, Long to) {
            this.from = from;
            this.to = to;
            return this;
        }

        public Builder relative(String relativeOpt) {
            this.relativeOpt = relativeOpt;
            return this;
        }

        public Builder relativePast(boolean relativePast) {
            this.relativePast = relativePast;
            return this;
        }

        public Builder timeZone(String offsetTimeZone) {
            if (!StringUtils.isEmpty(offsetTimeZone)) {
                this.offsetTimeZone = offsetTimeZone;
            }
            return this;
        }

        public DateHelper build() {
            return new DateHelper(this);
        }
    }

    /*Get a specific period of time , relative date or custom date*/
    public static DateHelper getRelativeDateHelper(String relativeDate, Long startDate, Long endDate, Date now, DateFormatAndTimeZone dateFormatAndTimeZone) {
        if (relativeDate != null && RelativeDateUtil.isValidRelativeDateCode(relativeDate)) {
            if (startDate != null) {
                now = new Date(startDate);
            }
            return new DateHelper(relativeDate, now, dateFormatAndTimeZone.getTimeZone());
        } else {
            return StringUtils.isEmpty(relativeDate) || relativeDate.equals("0/0/0") || relativeDate.equals("CUSTOM")
                    ? new DateHelper.Builder().range(startDate, endDate).timeZone(dateFormatAndTimeZone.getTimeZone()).build()
                    : new DateHelper.Builder().relative(relativeDate)
                    .timeZone(dateFormatAndTimeZone.getTimeZone()).relativePast(true).build();
        }
    }

    public static Date truncateDate(Date date, String truncateLevel) {
        int level = -1;
        switch (truncateLevel) {
            case "hour":
                level = Calendar.HOUR;
                break;
            case "day":
                level = Calendar.DATE;
                break;
            case "week":
                level = Calendar.DAY_OF_WEEK;
                break;
            case "month":
                level = Calendar.MONTH;
                break;
            case "year":
                level = Calendar.YEAR;
                break;
        }

        return DateUtils.truncate(date, level);
    }

    public static Date roundDate(Date date, String roundLevel) {

        int level = -1;
        switch (roundLevel) {
            case "hour":
                level = Calendar.HOUR;
                break;
            case "day":
                level = Calendar.DATE;
                break;
            case "week":
                level = Calendar.DAY_OF_WEEK;
                break;
            case "month":
                level = Calendar.MONTH;
                break;
            case "year":
                level = Calendar.YEAR;
                break;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(level, 1);
        return new Date(DateUtils.truncate(cal.getTime(), level).getTime() - 1L);
    }


    /**
     * Returns date by trying to guess format
     *
     * @param dateString
     * @return
     */
    public static Date getDateAndDetermineFormat(String dateString) {
        dateString = removeSpaces(dateString);
        Date date = null;
        if (isTimeStampMillis(dateString)) {
            date = new Date(Long.parseLong(dateString));
        } else if (isTimeStampSecs(dateString)) {
            date = new Date(Long.parseLong(dateString) * 1000);
        } else {
            List<DateTimeFormatter> dateTimeFormatterList = DateTimeFormatterHelper.getDateTimeFormatOfDateString(dateString);
            if (dateTimeFormatterList != null) {
                try {
                    date = DateTimeFormatterHelper.parseDateText(dateString, dateTimeFormatterList);
                } catch (Exception e) {
                    logger.error(e);
                }
            }
        }
        return date;
    }

    private static String removeSpaces(String value) {
        if (value != null) {
            value = value.replaceAll("\\s+", " ").trim();
        }
        return value;
    }

    public static boolean isTimeStampMillis(String value) {
        return value.matches("^\\d{13}$");
    }

    public static boolean isTimeStampSecs(String value) {
        return value.matches("^\\d{13}$");
    }

    /**
     * Same logic of UI in order to show the dwell time
     *
     * @param value
     * @param byTranslate
     */
    public static String formatDwellTime(Long value, String byTranslate) {
        String sign = (value < 0) ? "-" : "";
        value = (value < 0) ? value * -1 : value;
        int x = ((Long) (value / 1000)).intValue();
        int seconds = x % 60;
        x = (x / 60);
        int minutes = x % 60;
        x = (x / 60);
        int hours = x % 24;
        x = (x / 24);
        int days = x;
        String hoursS = (hours < 10 ? "0" + hours + ":" : hours + ":");
        String minutesS = (minutes < 10 ? "0" + minutes + ":" : minutes + ":");
        String secondsS = seconds < 10 ? "0" + seconds : seconds + "";
        return byTranslate != null ? "[" + sign + "(" + days + "), " + hoursS + minutesS + secondsS + "]" : sign + days + " Days " + hoursS + minutesS + secondsS;
    }

    public static String formatDwellTime(Long value) {
        return formatDwellTime(value, null);
    }

    public static String dateToISODate(final Date date) {
        if (date != null) {
            return "--ISODate(\"" + ISODateTimeFormat.dateTime().print(date.getTime()) + "\")--";
        }
        return null;
    }

    private static ZonedDateTime toZonedDateTime(Date utilDate, ZoneId zoneId) {
        return ZonedDateTime.ofInstant(utilDate.toInstant(), zoneId);
    }

    //Possible date format matches
    private static final Map<String, String> DATE_FORMAT_REGEXPS = new HashMap<String, String>() {{
        put("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{3}([A-Za-z]{0,3}|([+|-]\\d{2}:\\d{2}))$",
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        put("^[A-Za-z]{3}\\s[A-Za-z]{3}\\s\\d{2}\\s\\d{1,2}:\\d{2}:\\d{2}\\s[A-Z]{3}\\s\\d{4}$",
                "E MMM dd HH:mm:ss z yyyy");
        put("^\\d{8}$", "yyyyMMdd");
        put("^\\d{12}$", "yyyyMMddHHmm");
        put("^\\d{8}\\s\\d{4}$", "yyyyMMdd HHmm");
        put("^\\d{14}$", "yyyyMMddHHmmss");
        put("^\\d{8}\\s\\d{6}$", "yyyyMMdd HHmmss");
        put("^\\d{1,2}-\\d{1,2}-\\d{4}$", "dd-MM-yyyy");
        put("^\\d{1,2}-\\d{1,2}-\\d{4}\\s\\d{1,2}:\\d{2}$", "dd-MM-yyyy HH:mm");
        put("^\\d{1,2}-\\d{1,2}-\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$", "dd-MM-yyyy HH:mm:ss");
        put("^\\d{1,2}/\\d{1,2}/\\d{4}$", "MM/dd/yyyy");
        put("^\\d{1,2}/\\d{1,2}/\\d{4}\\s\\d{1,2}:\\d{2}$", "MM/dd/yyyy HH:mm");
        put("^\\d{1,2}/\\d{1,2}/\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$", "MM/dd/yyyy HH:mm:ss");
        put("^\\d{1,2}/\\d{1,2}/\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}(\\s(A|P)M)$", "MM/dd/yyyy hh:mm:ss a");
        put("^\\d{1,2}\\/\\d{1,2}\\/\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}\\s[a-zA-Z]{3}$", "MM/dd/yyyy HH:mm:ss Z");
        put("^\\d{1,2}\\/\\d{1,2}\\/\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}\\sGMT-\\d{1,}$", "MM/dd/yyyy HH:mm:ss z");
        put("^\\d{1,2}\\/\\d{1,2}\\/\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}\\s[+|-]\\d{2}:\\d{2}$", "MM/dd/yyyy HH:mm:ss XXX");
        put("^\\d{1,2}\\/\\d{1,2}\\/\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}\\s[+|-]\\d{4}$", "MM/dd/yyyy HH:mm:ss Z");
        put("^\\d{1,2}\\/\\d{1,2}\\/\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}[+|-]\\d{4}$","MM/dd/yyyy HH:mm:ssZ");

        put("^\\d{4}/\\d{1,2}/\\d{1,2}$", "yyyy/MM/dd");
        put("^\\d{4}/\\d{1,2}/\\d{1,2}\\s\\d{1,2}:\\d{2}$", "yyyy/MM/dd HH:mm");
        put("^\\d{4}/\\d{1,2}/\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}$", "yyyy/MM/dd HH:mm:ss");
        put("^\\d{1,2}\\s[a-zA-Z]{3}\\s\\d{4}$", "dd MMM yyyy");
        put("^\\d{1,2}\\s[a-zA-Z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}$", "dd MMM yyyy HH:mm");
        put("^\\d{1,2}\\s[a-zA-Z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$", "dd MMM yyyy HH:mm:ss");

        put("^\\d{1,2}\\s[a-zA-Z]{4,}\\s\\d{4}$", "dd MMMM yyyy");
        put("^\\d{1,2}\\s[a-zA-Z]{4,}\\s\\d{4}\\s\\d{1,2}:\\d{2}$", "dd MMMM yyyy HH:mm");
        put("^\\d{1,2}\\s[a-zA-Z]{4,}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$", "dd MMMM yyyy HH:mm:ss");

        put("\\d{1,2}+/\\d{1,2}+/\\d{2}", "MM/dd/yy");
        put("^\\d{1,2}/\\d{1,2}/\\d{2}\\s\\d{1,2}:\\d{2}$", "MM/dd/yy HH:mm");
        put("^\\d{1,2}/\\d{1,2}/\\d{2}\\s\\d{1,2}:\\d{2}(\\s(A|P)M)$", "MM/dd/yy HH:mm a");
        // put("^\\d{1,2}\\/\\d{1,2}\\/\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}\\s[a-z|A-Z|\\/|_|\\s]{1,}$", "MM/dd/yyyy HH:mm:ss zzzz"); TODO review how to use this format

        put("^[a-zA-Z]{3}\\s[A-Z]{3}\\s\\d{2}\\s\\d{1,2}:\\d{2}:\\d{2}\\s[A-Z]{3}\\s\\d{4}$", "E MMM dd HH:mm:ss z yyyy");
        put("^[a-zA-Z,]{4}\\s\\d{2}\\s[a-zA-Z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}\\s[A-Z]{3}$", "E MMM dd HH:mm:ss z yyyy");
        put("\\d{1,2}+/\\d{1,2}+/\\d{4}\\s+\\d{1,2}+:\\d{1,2}((AM)|(PM))", "dd/MM/yyyy hh:mma");


        put("^\\d{4}-\\d{1,2}-\\d{1,2}$", "yyyy-MM-dd");
        put("^\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}$", "yyyy-MM-dd HH:mm");
        put("^\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}$", "yyyy-MM-dd HH:mm:ss");
        put("^\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}.\\d{1,5}$", "yyyy-MM-dd HH:mm:ss.S");
        put("^\\d{4}-\\d{1,2}-\\d{1,2}-\\d{1,2}.\\d{2}.\\d{2}.\\d{1,6}$", "yyyy-MM-dd-HH.mm.ss.S");

        // ISO
        put("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{3}[+|-]\\d{2}", "yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        put("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{3}[+|-]\\d{2,4}", DateFormatAndTimeZone.JAVA_ISO_FORMAT);
        put("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{3}[+|-]\\d{2}:\\d{2}:\\d{2}", "yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        put("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{3}[+|-]\\d{2}:\\d{2}", "yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

        put("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{3}Z", "yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        put("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+|-]\\d{2}", "yyyy-MM-dd'T'HH:mm:ssXXX");
        put("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+|-]\\d{2,4}", "yyyy-MM-dd'T'HH:mm:ssZ");
        put("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+|-]\\d{2}:\\d{2}:\\d{2}", "yyyy-MM-dd'T'HH:mm:ssXXX");
        put("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+|-]\\d{2}:\\d{2}", "yyyy-MM-dd'T'HH:mm:ssXXX");

        put("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z", "yyyy-MM-dd'T'HH:mm:ssXXX");
        put("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}[+|-]\\d{2}", "yyyy-MM-dd'T'HH:mmXXX");
        put("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}[+|-]\\d{2,4}", "yyyy-MM-dd'T'HH:mmZ");
        put("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}[+|-]\\d{2}:\\d{2}:\\d{2}", "yyyy-MM-dd'T'HH:mmXXX");
        put("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}[+|-]\\d{2}:\\d{2}", "yyyy-MM-dd'T'HH:mmXXX");
        put("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}Z", "yyyy-MM-dd'T'HH:mmXXX");
    }};

    /**
     * Determine SimpleDateFormat pattern matching with the given date string. Returns null if
     * format is unknown. You can simply extend DateUtil with more formats if needed.
     *
     * @param dateString The date string to determine the SimpleDateFormat pattern for.
     * @return The matching SimpleDateFormat pattern, or null if format is unknown.
     */
    @Deprecated
    public static String determineDateFormat(String dateString) {
        for (String regexp : DATE_FORMAT_REGEXPS.keySet()) {
            if (dateString.toUpperCase().matches(regexp)) {
                return DATE_FORMAT_REGEXPS.get(regexp);
            }
        }
        return null; // Unknown format.
    }
}
