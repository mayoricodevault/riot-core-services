package com.tierconnect.riot.api.database.exception;

/**
 * Created by vealaro on 12/2/16.
 */
public class ValueNotPermittedException extends RuntimeException {

    private static final long serialVersionUID = -6335983496583521401L;

    public ValueNotPermittedException(String message) {
        super(message);
    }
}
