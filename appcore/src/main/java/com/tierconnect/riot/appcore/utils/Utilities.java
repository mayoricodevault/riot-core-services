package com.tierconnect.riot.appcore.utils;


import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.entities.UserPassword;
import com.tierconnect.riot.appcore.services.ConfigurationService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.UserPasswordService;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.utils.HashUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * This class contains utilities for java code
 * Created by rchirinos on 6/5/2015.
 */
public class Utilities {
    static final Logger logger = Logger.getLogger(Utilities.class);

    /**
     * This method trim the data of a list
     */
    public static List<String> trimList(List<String> listData) {
        List<String> result = new ArrayList<String>();
        if (listData != null && !listData.isEmpty()) {
            for (String field : listData) {
                result.add(removeSpaces(field));
            }
        } else {
            logger.error("The value 'listData' should have values.");
        }
        return result;
    }

    /****
     * This method search a specific value in the list.
     * It returns true if the values exist
     *****/
    public static boolean isValueInTheList(List<String> listData, String value) {
        boolean response = false;
        if (listData != null && listData.size() > 0) {
            for (String field : listData) {
                if (field.trim().equals(value)) {
                    response = true;
                    break;
                }
            }
        } else {
            logger.error("The value 'listData' should have values.");
        }
        return response;
    }

    /**
     * Clone Map
     *
     * @param origin
     * @param destiny
     * @return
     */
    public static Map<String, Object> cloneHashMap(Map<String, Object> origin, Map<String, Object> destiny) {
        for (Object o : origin.entrySet()) {
            Map.Entry pair = (Map.Entry) o;
            destiny.put(pair.getKey().toString(), pair.getValue());
        }
        return destiny;
    }

    /**
     * @param value Value to be evaluated
     * @return true if it is an AlphaNumeric value
     */
    public static boolean isAlphaNumeric(String value) {
        if (value != null) {
            Pattern pattern = Pattern.compile("^[0-9a-zA-Z]+$");
            Matcher matcher = pattern.matcher(value);
            return (matcher.matches());
        } else {
            return false;
        }
    }

    /**
     * Replace character escape
     *
     * @param value
     * @return
     */
    public static String replaceCharacterEscape(String value) {
        if (value != null) {
            value = value.replaceAll("\b", "\\\\b");
            value = value.replaceAll("\n", "\\\\n");
            value = value.replaceAll("\f", "\\\\f");
            value = value.replaceAll("\r", "\\\\r");
            value = value.replaceAll("\t", "\\\\t");
        }
        return value;
    }

    /**
     * @param value Value to be evaluated
     * @return true if it is an AlphaNumeric with/without some character special value
     */
    public static boolean isAlphaNumericCharacterSpecials(String value, String characterSpecials) {
        if (value != null) {
            Pattern pattern = Pattern.compile("^[0-9a-zA-Z" + characterSpecials + "]+$");
            Matcher matcher = pattern.matcher(value);
            return (matcher.matches());
        }
        return false;
    }

    /**
     * convert a string in TimeStampFormat to Date
     *
     * @param timeStamp Object to contains a timeStamp value.
     * @return a Date value.
     */
    public static Date getDate(Object timeStamp) {
        if (timeStamp == null) {
            return new Date();
        }
        try {
            long dateLong = Long.parseLong(timeStamp.toString());
            return new Date(dateLong);
        } catch (NumberFormatException e) {
            return new Date();
        }
    }

    /**
     * <p>Checks if an list of Strings is not empty or not <code>null</code>.</p>
     *
     * @param list the list to test
     * @return <code>true</code> if the list is not empty or not <code>null</code>
     */
    public static boolean isNotEmpty(List<String> list) {
        return (list != null && !list.isEmpty());
    }

    /**
     * Remove all ilegal characters in a string.
     *
     * @param value string to remove all ilegal characters.
     * @return sanitize string or empty string if error exists.
     */
    public static String sanitizeString(String value) {
        if (isNotBlank(value)) {
            return value.replaceAll("[^a-zA-Z0-9.-]", "_");
        } else {
            return "";
        }
    }

    /**
     * @param value value
     * @return true if the value is Integer
     */
    public static boolean isInteger(Object value) {
        return (value instanceof Integer);
    }

    /**
     * @param value value
     * @return true if the value is String
     */
    public static boolean isString(Object value) {
        return (value instanceof String);
    }

    /**
     * @param value
     * @return
     */
    public static boolean isEmptyOrNull(String value) {
        return (value == null || value.trim().isEmpty());
    }

    /**
     * @param value
     * @return
     */
    public static boolean isNotEmptyOrNull(Object value) {
        return ((value != null) && (StringUtils.isNotEmpty(value.toString())));
    }

    /**
     * @param value
     * @return
     */
    public static boolean isNotEmptyOrNullList(Object value) {
        return ((value != null) && (((List<Object>) value).size() > 0));
    }

    /**
     * isValidBoolean
     *
     * @param s
     * @return
     */
    public static boolean isValidBoolean(String s) {
        return ((s != null) && (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("false")));
    }

    public static boolean isValidBoolean(Object s) {
        return s != null && isValidBoolean(s.toString());
    }

    /**
     * @param s
     * @return true if the value s is a number (could be a decimal or bigdecimal or a number)
     */
    public static boolean isNumber(String s) {
        try {
            new BigDecimal(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isNumber(Object s) {
        return s != null && isNumber(s.toString());
    }

    public static String removeSpaces(String value) {
        if (value != null) {
            value = value.replaceAll("\\s+", " ").trim();
        }
        return value;
    }

    /**
     * validate url with schemes
     *
     * @param url
     * @param schemes example http, https
     * @return true if the value
     */
    public static boolean urlIsValid(String url, String[] schemes) {
        UrlValidator validator = new UrlValidator(schemes);
        return validator.isValid(url);
    }

    /**
     * Validate exists timezone
     *
     * @param timeZone
     * @return
     */
    public static boolean timeZoneIsValid(String timeZone) {
        return GroupService.zoneList.contains(timeZone);
    }

    /**
     * @param s
     * @return true if the value s is a number (could be long number)
     */
    public static boolean isLongNumber(String s) {
        try {
            new Long(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns a message from milliseconds to human readable format
     *
     * @param millis milliseconds to be converted
     * @return message in format [days], [hours], [minutes], [seconds]
     */
    public static String getTimeMessageFromMillis(Long millis) {
        String output = "";
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        long hours = TimeUnit.MILLISECONDS.toHours(millis - TimeUnit.DAYS.toMillis(days));
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis - TimeUnit.DAYS.toMillis(days) - TimeUnit.HOURS.toMillis(hours));
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis - TimeUnit.DAYS.toMillis(days) - TimeUnit.HOURS.toMillis(hours) - TimeUnit.MINUTES.toMillis(minutes));
        String dayMsg = validateMsg(days, "day");
        String hourMsg = validateMsg(hours, "hour");
        String minMsg = validateMsg(minutes, "minute");
        String secMsg = validateMsg(seconds, "second");
        output = appendToMsg(output, dayMsg);
        output = appendToMsg(output, hourMsg);
        output = appendToMsg(output, minMsg);
        output = appendToMsg(output, secMsg);
        return StringUtils.isBlank(output) ? "1 second" : output;
    }

    /**
     * Returns a message according its value and unit
     *
     * @param value Value to be added in message
     * @param unit  Unit to be added to message
     * @return Message in format [value] unit[s]
     */
    private static String validateMsg(long value, String unit) {
        if (value > 0) {
            if (value > 1) {
                return value + " " + unit + "s";
            } else {
                return value + " " + unit;
            }
        }
        return "";
    }

    /**
     * Appends a string to a message
     *
     * @param in input message
     * @param ap append message
     * @return input message, comma, appended message
     */
    private static String appendToMsg(String in, String ap) {
        if (StringUtils.isBlank(in)) {
            return ap;
        }
        if (StringUtils.isBlank(ap)) {
            return in;
        }
        return in + ", " + ap;
    }

    /**
     * Validates a password according to configuration
     *
     * @param group    Group of user to be validated
     * @param user     User who wants to use a password
     * @param password Password to be validated
     */
    public static void validatePassword(Group group, User user, String password) {
        if (StringUtils.equals(ConfigurationService.getAsString(group, "authenticationMode"),
                AuthenticationUtils.NATIVE_AUTHENTICATION)) {

            int minLength = ConfigurationService.getAsInteger(group, "passwordMinLength");
            int maxLength = ConfigurationService.getAsInteger(group, "passwordMaxLength");
            boolean upperRequired =
                    ConfigurationService.getAsBoolean(group, "passwordUppercaseRequired");
            boolean numberRequired =
                    ConfigurationService.getAsBoolean(group, "passwordNumberRequired");
            boolean specialRequired =
                    ConfigurationService.getAsBoolean(group, "passwordSpecialCharRequired");
            int consecutiveChar = ConfigurationService.getAsInteger(group, "passwordConsecutiveChar");
            int maxReusablePasswords =
                    ConfigurationService.getAsInteger(group, "passwordReusePrevious");
            boolean useReserved =
                    ConfigurationService.getAsBoolean(group, "passwordUseReservedWords");
            String reservedWords = ConfigurationService.getAsString(group, "passwordReservedWords");

            if (password.length() < minLength) {
                logger.error("Password Policy Auditing: The minimum password length is " + minLength + " characters.");
                throw new UserException("The minimum password length is " + minLength + " characters.");
            }
            if (password.length() > maxLength) {
                logger.error("Password Policy Auditing: The maximum password length is " + maxLength + " characters.");
                throw new UserException("The maximum password length is " + maxLength + " characters.");
            }
            if (upperRequired && !password.matches("^(.*?[A-Z]).*$")) {
                logger.error("Password Policy Auditing: Password must contain at least one Uppercase character.");
                throw new UserException("Password must contain at least one Uppercase character.");
            }
            if (numberRequired && !password.matches("^(.*?\\d).*$")) {
                logger.error("Password Policy Auditing: Password must contain at least one numerical character.");
                throw new UserException("Password must contain at least one numerical character.");
            }
            if (specialRequired && !password.matches("^(.*?[_\\W]).*$")) {
                logger.error("Password Policy Auditing: Password must contain at least one symbol character.");
                throw new UserException("Password must contain at least one symbol character.");
            }
            if (consecutiveChar != 0 && (Pattern.compile("(.)\\1{" + consecutiveChar + ",}")).matcher(password).find()) {
                logger.error("Password Policy Auditing: Password must not contain more than " + consecutiveChar + " identical consecutive characters.");
                throw new UserException("Password must not contain more than " + consecutiveChar + " identical consecutive characters.");
            }
            if (maxReusablePasswords > 0) {
                for (UserPassword userPassword : UserPasswordService.getInstance()
                        .getLastUserPasswords(user, maxReusablePasswords)) {
                    if (userPassword.getHashedPassword().equals(HashUtils.hashSHA256(password))) {
                        logger.error("Password Policy Auditing: Cannot reuse previous " + maxReusablePasswords + " passwords.");
                        throw new UserException("Cannot reuse previous " + maxReusablePasswords + " passwords.");
                    }
                }
            }

            if (useReserved && !StringUtils.isBlank(reservedWords)) {
                List<String> dictionaryList = new ArrayList<>();
                dictionaryList.add(user.getUsername());
                dictionaryList.add(user.getFirstName());
                dictionaryList.add(user.getLastName());
                dictionaryList.add(user.getEmail());
                for (String reserved : dictionaryList) {
                    if (StringUtils.contains(password, reserved)
                            && StringUtils.isNotBlank(reserved)) {
                        logger.error("Password Policy Auditing: Password must not contain user information.");
                        throw new UserException("Password must not contain user information.");
                    }
                }
                dictionaryList.addAll(Arrays.asList(reservedWords.split(",")));
                if (dictionaryList.contains(password)) {
                    logger.error("Password Policy Auditing: Password cannot be a word contained in the dictionary.");
                    throw new UserException("Password cannot be a word contained in the dictionary.");
                }
                URL url = Utilities.class.getClassLoader().getResource("passwordDictionaries");
                if (url != null) {
                    checkPasswordInDictionaries(url.getPath(), password);
                }
            }
        }
    }

    /**
     * Verify if any dictionary contains a password
     *
     * @param path     path of the dictionaries repository
     * @param password password to be validated
     */
    private static void checkPasswordInDictionaries(String path, String password) {
        File folder = new File(path);
        for (final File dictionary : folder.listFiles()) {
            try {
                InputStream is = new FileInputStream(dictionary);
                for (String line : IOUtils.toString(is, "UTF-8").split("\\n")) {
                    if (StringUtils.equals(password, line)) {
                        logger.error("Password Policy Auditing: Password cannot be a word contained in the dictionary.");
                        throw new UserException("Password cannot be a word contained in the dictionary.");
                    }
                }
            } catch (IOException e) {
                logger.error("Error getting dictionaries.", e);
                throw new UserException("Error getting dictionaries.", e);
            }
        }
    }
}
