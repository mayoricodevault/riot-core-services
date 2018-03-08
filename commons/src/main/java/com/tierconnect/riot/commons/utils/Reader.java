package com.tierconnect.riot.commons.utils;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Created by achambi on 10/19/16.
 * Render class to convert InputStream to StringBuffer.
 */
public class Reader {

    private static Logger logger = Logger.getLogger(Reader.class);

    /**
     * Load a InputStream and return  StringBuffer.
     *
     * @param inputStream to convert to StringBuffer
     * @return a instance of StringBuffer
     * @throws IOException If error exists.
     */
    public static StringBuffer loadInputStream(InputStream inputStream) throws IOException {
        StringBuffer output;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String line;
            output = new StringBuffer();
            if (reader.ready()) {
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
        } catch (NullPointerException ex) {
            logger.error("the inputStream is Null", ex);
            throw new NullPointerException("the inputStream is Null.");
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return output;
    }
}
