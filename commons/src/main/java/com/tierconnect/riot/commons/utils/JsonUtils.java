package com.tierconnect.riot.commons.utils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Preconditions;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author jantezana
 * @version 02/23/2017
 */
public final class JsonUtils {

    static Logger logger = Logger.getLogger(JsonUtils.class);
    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        OBJECT_MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    /**
     * Converts object to json string.
     *
     * @param object the object
     * @return a json string
     */
    public static String convertObjectToJson(final Object object)
    throws JsonProcessingException {
        Preconditions.checkNotNull(object);
        String result = OBJECT_MAPPER.writeValueAsString(object);

        return result;
    }

    /**
     * Convert a string to object
     *
     * @param value the string value
     * @param clazz the class
     * @param <T>   the type
     * @return the object
     * @throws java.io.IOException
     */
    public static <T> T convertStringToObject(final String value,
                                              final Class<T> clazz)
    throws IOException {
        Preconditions.checkNotNull(value, "The value is null");
        Preconditions.checkNotNull(clazz, "The clazz is null");
        T result = OBJECT_MAPPER.readValue(value, clazz);

        return result;
    }

    /**
     * Converts a input stream to object.
     *
     * @param inputStream the input Stream
     * @param clazz       the class
     * @param <T>         the type
     * @return the object
     * @throws java.io.IOException
     */
    public static <T> T convertInputStreamToObject(final InputStream inputStream,
                                                   final Class<T> clazz)
    throws IOException {
        Preconditions.checkNotNull(inputStream, "The inputStream is null");
        Preconditions.checkNotNull(clazz, "The clazz is null");
        T result = OBJECT_MAPPER.readValue(inputStream, clazz);

        return result;
    }

    /**
     * Converts an input Stream to object
     *
     * @param inputStream   the input Stream
     * @param typeReference the type Reference
     * @param <T>           the type
     * @return the object
     * @throws java.io.IOException
     */
    public static <T> T convertInputStreamToObject(final InputStream inputStream,
                                                   final TypeReference<T> typeReference)
    throws IOException {
        Preconditions.checkNotNull(inputStream, "The inputStream is null");
        Preconditions.checkNotNull(typeReference, "The typeReference is null");
        T result = OBJECT_MAPPER.readValue(inputStream, typeReference);

        return result;
    }

    /**
     *
     * @param json
     * @return
     */
    public static boolean isValidJSON(final String json) {
        boolean valid = false;
        try {
            final JsonParser parser = new ObjectMapper().getJsonFactory()
                    .createJsonParser(json);
            while (parser.nextToken() != null) {
            }
            valid = true;
        } catch (JsonParseException jpe) {
            jpe.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return valid;
    }

}
