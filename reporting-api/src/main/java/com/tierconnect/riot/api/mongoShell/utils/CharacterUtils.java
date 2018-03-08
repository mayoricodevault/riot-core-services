package com.tierconnect.riot.api.mongoShell.utils;

/**
 * Created by vealaro on 12/5/16.
 *
 */
public class CharacterUtils {

    public static final String OPEN_PARENTHESIS = "(";
    public static final String CLOSE_PARENTHESIS = ")";
    public static final String OPEN_BRACE = "{";
    public static final String CLOSE_BRACE = "}";
    public static final String OPEN_BRACKET = "[";
    public static final String CLOSE_BRACKET = "]";
    public static final String QUOTE = "\"";
    public static final String SINGLE_QUOTE = "'";
    public static final String COLON = ":";
    public static final String COMMA = ",";
    public static final String SPACE = " ";

    private CharacterUtils() {
    }

    public static String betweenBraces(String value) {
        return OPEN_BRACE + value + CLOSE_BRACE;
    }

    public static String betweenBrackets(String value) {
        return OPEN_BRACKET + value + CLOSE_BRACKET;
    }

    public static String betweenParenthesis(String value) {
        return OPEN_PARENTHESIS + value + CLOSE_PARENTHESIS;
    }

    public static String betweenSingleQuote(String value) {
        return SINGLE_QUOTE + value + SINGLE_QUOTE;
    }

    public static String betweenDoubleQuote(String value) {
        return QUOTE + value + QUOTE;
    }

    public static String converterKeyValueToString(String key, Object value) {
        return key + COLON + value;
    }
}
