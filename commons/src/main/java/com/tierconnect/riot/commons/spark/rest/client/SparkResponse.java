package com.tierconnect.riot.commons.spark.rest.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * SparkResponse class.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class SparkResponse {
    private Action action;
    protected String message;
    protected String serverSparkVersion;
    protected String submissionId;
    protected Boolean success;

    public Action getAction() {
        return action;
    }

    public String getMessage() {
        return message;
    }

    public String getServerSparkVersion() {
        return serverSparkVersion;
    }

    public String getSubmissionId() {
        return submissionId;
    }

    public Boolean getSuccess() {
        return success;
    }
}
