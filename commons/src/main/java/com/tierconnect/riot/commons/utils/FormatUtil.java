package com.tierconnect.riot.commons.utils;

import com.google.common.base.Preconditions;
import com.tierconnect.riot.commons.serializers.Constants;
import org.apache.commons.lang.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by achambi on 11/8/16.
 * Class to convert from string to distinct formats.
 */
public class FormatUtil {

    /**
     * Method invoke by reflexion a constant to parse timeDate to timestamp.
     *
     * @param stringDate A string date in the DATE_FORMAT format.
     * @return A instance of Date.
     * @throws ParseException exception to parse the string date to Date.
     */
    public static Date parse(String stringDate)
    throws ParseException {
        //        Assertions.isTrueArgument("value", stringDate, Constants.DATE_REG_EXP);
        //        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMAT);
        //        return simpleDateFormat.parse(stringDate);
        if (StringUtils.isNotEmpty(stringDate)){
            SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT);
            try {
                return sdf.parse(stringDate);
            } catch (Exception e){
                return getDateAndDetermineFormat(stringDate);
            }
        }else {
            throw new IllegalArgumentException("Cannot parse to timestamp because the parameter is empty.");
        }
    }

    /**
     * Method invoke by reflexion to parse timeDate to timestamp.
     *
     * @param stringDate A string date in the DATE_FORMAT format.
     * @return a long value to contains the timestamp.
     * @throws ParseException exception to parse the string date to long.
     */
    @SuppressWarnings("unused")
    public static long parseTimeStamp(String stringDate)
    throws ParseException {
        if (StringUtils.isNotEmpty(stringDate)){
            try {
                return parse(stringDate).getTime();
            } catch (Exception e){
                return getDateAndDetermineFormat(stringDate).getTime();
            }
        }else {
            throw new IllegalArgumentException("Cannot parse to timestamp because the parameter is empty.");
        }
    }

    /**
     * Method invoke by reflexion to convert ArrayString to String.
     *
     * @param stringArray A string array in the next format "[12.12,13.13,14.14]".
     * @return A string in the next format "12.12;13.13;14.14".
     * @throws ParseException exception to parse the arrayString to String.
     */
    @SuppressWarnings("unused")
    public static String parseArray(String stringArray)
    throws ParseException {
        return stringArray.replace("[", "").replace(",", ";").replace("]", "").replace(" ", "");
    }

    /**
     * Determine SimpleDateFormat pattern matching with the given date string. Returns null if
     * format is unknown. You can simply extend DateUtil with more formats if needed.
     *
     * @param dateString The date string to determine the SimpleDateFormat pattern for.
     * @return The matching SimpleDateFormat pattern, or null if format is unknown.
     */
    private static String determineDateFormat(String dateString) {
        for (String regexp : Constants.DATE_FORMAT_REGEXPS.keySet()) {
            if (dateString.toUpperCase().matches(regexp)) {
                return Constants.DATE_FORMAT_REGEXPS.get(regexp);
            }
        }
        return null; // Unknown format.
    }

    /**
     * Returns date by trying to guess format
     *
     * @param dateString Date in String format
     * @return Date
     */
    public static Date getDateAndDetermineFormat(String dateString)
    throws ParseException {
        Date date = null;
        if (isTimeStampMillis(dateString)) {
            date = new Date(Long.parseLong(dateString));
        } else if (isTimeStampSecs(dateString)) {
            date = new Date(Long.parseLong(dateString) * 1000);
        } else {
            String dateFormat = determineDateFormat(dateString);
            if (dateFormat != null) {
                SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
                date = sdf.parse(dateString);
            }
        }
        return date;
    }

    private static boolean isTimeStampMillis(String value) {
        return value.matches("^\\d{13}$");
    }

    private static boolean isTimeStampSecs(String value) {
        return value.matches("^\\d{13}$");
    }

    public static String format(final Date date, final String format)
    throws ParseException {
        Preconditions.checkNotNull(date, "The date is null");
        Preconditions.checkNotNull(format, "The format is null");
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        String dateString = sdf.format(date);
        return dateString;
    }

}
