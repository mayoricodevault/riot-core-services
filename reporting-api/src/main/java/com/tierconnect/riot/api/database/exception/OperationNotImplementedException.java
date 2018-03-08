package com.tierconnect.riot.api.database.exception;

/**
 * Created by vealaro on 12/20/16.
 */
public class OperationNotImplementedException extends RuntimeException {
    private static final long serialVersionUID = 2807527402282286649L;

    public OperationNotImplementedException(String message) {
        super(message);
    }
}
