package com.tierconnect.riot.iot.services.exceptions;

/**
 * Created by cvertiz on 5/15/2015.
 */
public class UserIsNotOperatorException extends Exception{
    private static final long serialVersionUID = 1L;

    public UserIsNotOperatorException() {
        super();
    }

    public UserIsNotOperatorException(String message, Throwable cause,
                                      boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public UserIsNotOperatorException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserIsNotOperatorException(String message) {
        super(message);
    }

    public UserIsNotOperatorException(Throwable cause) {
        super(cause);
    }
}