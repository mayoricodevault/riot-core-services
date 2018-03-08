package com.tierconnect.riot.commons.spark.rest.client;

/**
 * FailedSparkRequestException class.
 */
public final class FailedSparkRequestException extends Exception {


    /**
     * Creates an instance of FailedSparkRequestException
     *
     * @param message the value of message
     */
    public FailedSparkRequestException(final String message) {
        super(message);
    }

    /**
     * Creates an instance of FailedSparkRequestException.
     *
     * @param cause the cause of the error
     */
    public FailedSparkRequestException(final Throwable cause) {
        super(cause);
    }
}
