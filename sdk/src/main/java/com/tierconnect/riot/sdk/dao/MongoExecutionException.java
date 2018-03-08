package com.tierconnect.riot.sdk.dao;

/**
 * Created by cvertiz on 5/15/2015.
 */
public class MongoExecutionException extends Exception{
    private static final long serialVersionUID = 1L;

    public MongoExecutionException() {
        super();
    }

    public MongoExecutionException(String message, Throwable cause,
                                   boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public MongoExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public MongoExecutionException(String message) {
        super(message);
    }

    public MongoExecutionException(Throwable cause) {
        super(cause);
    }
}