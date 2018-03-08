package com.tierconnect.riot.api.database.exception;

/**
 * Project: reporting-api
 * Author: edwin
 * Date: 28/11/2016
 */
public class OperationNotSupportedException extends Exception {

    private static final long serialVersionUID = 5613372351718900940L;

    public OperationNotSupportedException(String message) {
        super(message);
    }
}
