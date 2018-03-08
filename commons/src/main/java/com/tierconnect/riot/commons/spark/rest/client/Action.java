package com.tierconnect.riot.commons.spark.rest.client;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Preconditions;

/**
 * Action enumerator.
 */
enum Action {
    CREATE_SUBMISSION_REQUEST("CreateSubmissionRequest"),
    CREATE_SUBMISSION_RESPONSE("CreateSubmissionResponse"),
    KILL_SUBMISSION_RESPONSE("KillSubmissionResponse"),
    SUBMISSION_STATUS_RESPONSE("SubmissionStatusResponse");

    private String value;

    /**
     * Creates an instance of Action.
     *
     * @param value the value
     */
    Action(final String value) {
        Preconditions.checkNotNull(value, "The value is null");
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
