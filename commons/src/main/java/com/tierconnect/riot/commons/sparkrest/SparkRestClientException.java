package com.tierconnect.riot.commons.sparkrest;

/**
 * Created by alfredo on 11/28/16.
 */
public class SparkRestClientException extends Exception {

    public SparkRestClientException() {
        super();
    }

    public SparkRestClientException(String message) {
        super(message);
    }

    public SparkRestClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public SparkRestClientException(Throwable cause) {
        super(cause);
    }

    protected SparkRestClientException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
