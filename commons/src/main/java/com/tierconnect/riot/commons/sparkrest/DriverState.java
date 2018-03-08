package com.tierconnect.riot.commons.sparkrest;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Preconditions;

/**
 * DriverState enumerator.
 */
public enum DriverState {
    SUBMITTED("SUBMITTED"),
    RUNNING("RUNNING"),
    FINISHED("FINISHED"),
    RELAUNCHING("RELAUNCHING"),
    UNKNOWN("UNKNOWN"),
    KILLED("KILLED"),
    FAILED("FAILED"),
    ERROR("ERROR");

    private String value;

    /**
     * Creates an instance of DriverState.
     *
     * @param value the value
     */
    DriverState(final String value) {
        Preconditions.checkNotNull(value, "The value is null");
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
