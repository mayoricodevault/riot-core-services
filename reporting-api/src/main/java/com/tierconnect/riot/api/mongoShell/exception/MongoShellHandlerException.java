package com.tierconnect.riot.api.mongoShell.exception;

import com.tierconnect.riot.api.mongoShell.MongoErrorMessage;

import java.io.IOException;


/**
 * Created by achambi on 8/15/17.
 * Class that handles mongo shell exceptions.
 */
public class MongoShellHandlerException {

    public static void handleException(MongoErrorMessage mongoErrorMessage, String value)
            throws IOException, IndexNotFoundException {
        throwBadHintException(mongoErrorMessage, value);
        throw new IOException(mongoErrorMessage.getErrorMessage());
    }

    private static void throwBadHintException(MongoErrorMessage error, String value) throws IndexNotFoundException {
        if (error.getCode().equals("2") && error.getErrorMessage().contains("bad hint")) {
            throw new IndexNotFoundException("The index \"" + value + "\" was deleted");
        }
    }
}
