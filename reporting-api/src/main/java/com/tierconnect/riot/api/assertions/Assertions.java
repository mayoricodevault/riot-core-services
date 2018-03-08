package com.tierconnect.riot.api.assertions;

import org.apache.commons.lang.StringUtils;

/**
 * Created by achambi on 11/9/16.
 * <p>Design by contract assertions.</p> <p>This class is not part of the public API and may be removed or changed at
 * any time.</p>
 */
public final class Assertions {

    private Assertions() {
    }

    /**
     * @param name         the name of the state that is being checked
     * @param dataTypeName the dataTypeName to verify.
     * @param condition    the condition about the parameter to check
     * @throws java.lang.IllegalArgumentException if the condition is false
     */
    public static void isTrueArgument(final String name, final String dataTypeName, final boolean condition) {
        if (!condition) {
            throw new IllegalArgumentException(name + " should be: " + dataTypeName);
        }
    }

    /**
     * Throw IllegalArgumentException if the match if false.
     *
     * @param name   the name of the state that is being checked
     * @param value  the value to check
     * @param regExp the regular expression about the parameter to check
     * @throws java.lang.IllegalArgumentException if the condition is false
     */
    public static void isTrueArgument(final String name, final String value, final String regExp) {
        if (!value.matches(regExp)) {
            throw new IllegalArgumentException(name + " should have the next format: " + regExp + ", but it is " +
                    value);
        }
    }

    /**
     * Throw IllegalStateException if the condition if false.
     *
     * @param name      the name of the state that is being checked
     * @param condition the condition about the parameter to check
     * @throws java.lang.IllegalStateException if the condition is false
     */
    public static void isTrue(final String name, final boolean condition) {
        if (!condition) {
            throw new IllegalArgumentException(name + " is not valid");
        }
    }

    /**
     * Throw IllegalArgumentException if the value is null.
     *
     * @param name  the parameter name
     * @param value the value that should not be null
     * @param <T>   the value type
     * @return the value
     * @throws java.lang.IllegalArgumentException if value is null
     */
    public static <T> T notNull(final String name, final T value) {
        if (value == null) {
            throw new IllegalArgumentException(name + " can not be null");
        }
        return value;
    }

    /**
     * return true if the value is not blank or false is blank.
     *
     * @param value The value to verify if it is null
     * @return true or false if the value is null or not
     */
    public static Boolean isNotBlank(String value) {
        return !StringUtils.isBlank(value);
    }

    /**
     * return true if the value is null or false is not null.
     *
     * @param value The value to verify if it is null
     * @return true or false if the value is null or not
     */
    public static Boolean isBlank(String value) {
        return StringUtils.isBlank(value);
    }

    /**
     * return true if the value is not blank or false is blank.
     *
     * @param value A {@link Object} instance to verify is a not empty string.
     * @return true or false if the value is null or not
     */
    public static Boolean isNotBlank(Object value) {
        return isNotNull(value) && !StringUtils.isBlank(value.toString());
    }

    /**
     * if the value is null return true or false.
     *
     * @param value the value that should not be null
     * @return true or false.
     */
    public static Boolean isNull(final Object value) {
        return value == null;
    }

    /**
     * if the value is not null return true or false.
     *
     * @param value a {@link String} to evaluate.
     * @return true or false.
     */
    public static Boolean isNotNull(final Object value) {
        return !isNull(value);
    }

    /**
     * Throw IllegalArgumentException if the value is null.
     *
     * @param name  the parameter name
     * @param value the value that should not be null
     * @throws java.lang.IllegalArgumentException if value is null
     */
    public static void voidNotNull(final String name, final Object value) {
        if (value == null) {
            throw new IllegalArgumentException(name + " cannot be null");
        }
    }

    /**
     * Throw IllegalArgumentException if the condition if false.
     *
     * @param name      the name of the state that is being checked
     * @param value     The value to verify is permit.
     * @param maxNumber Max length permit in value.
     * @throws java.lang.IllegalArgumentException if the condition is false
     */
    public static void isLessOrEqualsThan(final String name, final int value, final int maxNumber) {
        if (!(value <= maxNumber)) {
            throw new NumberFormatException(name + " should be less or equals than " + maxNumber);
        }
    }

    public static void isJustAWord(final String field, final String message) {
        if (isBlank(field) || field.contains(" ")) {
            throw new IllegalArgumentException(message);
        }
    }
}
