package com.tierconnect.riot.iot.utils.rest;

/**
 * Created by vealaro on 3/27/17.
 */
public class ExecuteActionException extends Exception {

    public ExecuteActionException(String message) {
        super(message);
    }

    public ExecuteActionException(Throwable cause) {
        super(cause);
    }

    public ExecuteActionException(String message, Throwable cause) {
        super(message, cause);
    }
}
