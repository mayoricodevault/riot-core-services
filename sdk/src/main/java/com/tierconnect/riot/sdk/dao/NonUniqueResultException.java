package com.tierconnect.riot.sdk.dao;

/**
 * Created by pablo on 12/17/14.
 *
 * Thrown when more than one result is return from a query that expects a single result.
 */
public class NonUniqueResultException extends Exception {
    public NonUniqueResultException() {
        super();
    }

    public NonUniqueResultException(String message) {
        super(message);
    }

    public NonUniqueResultException(String message, Throwable cause) {
        super(message, cause);
    }

    public NonUniqueResultException(Throwable cause) {
        super(cause);
    }

    protected NonUniqueResultException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
