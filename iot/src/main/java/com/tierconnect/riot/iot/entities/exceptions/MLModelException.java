package com.tierconnect.riot.iot.entities.exceptions;

/**
 * Created by pablo on 6/15/16.
 */
public class MLModelException extends Exception {
    public MLModelException() {
        super();
    }

    public MLModelException(String message) {
        super(message);
    }

    public MLModelException(String message, Throwable cause) {
        super(message, cause);
    }

    public MLModelException(Throwable cause) {
        super(cause);
    }

    protected MLModelException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
