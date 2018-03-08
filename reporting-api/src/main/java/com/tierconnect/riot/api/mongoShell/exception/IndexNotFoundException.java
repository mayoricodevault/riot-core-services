package com.tierconnect.riot.api.mongoShell.exception;

/**
 * Created by achambi on 8/15/17.
 * Class that handles the index exception not found in shell.
 */
public class IndexNotFoundException extends Exception {
    private static final long serialVersionUID = -6385143932728364749L;


    public IndexNotFoundException(String message) {
        super(message);
    }
}
