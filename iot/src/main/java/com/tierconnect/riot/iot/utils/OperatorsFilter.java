package com.tierconnect.riot.iot.utils;

/**
 * Created by vealaro on 10/24/16.
 */
public enum OperatorsFilter {

    OPERATOR_CONTAINS("~"),
    OPERATOR_IN("IN"),
    OPERATOR_NOT_IN("NOT_IN"),
    OPERATOR_NOT_EQUALS("!="),
    OPERATOR_EQUALS("="),
    OPERATOR_EMPTY("isEmpty");

    private String value;

    private OperatorsFilter(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
