package com.tierconnect.riot.iot.services.exceptions;

/**
 * Created by cvertiz on 5/15/2015.
 */
public class ZoneHasNoCameraException extends Exception{
    private static final long serialVersionUID = 1L;

    public ZoneHasNoCameraException() {
        super();
    }

    public ZoneHasNoCameraException(String message, Throwable cause,
                                     boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ZoneHasNoCameraException(String message, Throwable cause) {
        super(message, cause);
    }

    public ZoneHasNoCameraException(String message) {
        super(message);
    }

    public ZoneHasNoCameraException(Throwable cause) {
        super(cause);
    }
}