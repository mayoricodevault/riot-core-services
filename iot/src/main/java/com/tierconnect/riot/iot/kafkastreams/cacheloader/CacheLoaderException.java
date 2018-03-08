package com.tierconnect.riot.iot.kafkastreams.cacheloader;

/**
 * CacheLoaderException class,
 *
 * @author jantezana
 * @version 2017/01/11
 */
public class CacheLoaderException extends Exception {

    /**
     * Builds an instance of {@link CacheLoaderException}
     *
     * @param message the message
     */
    public CacheLoaderException(String message) {
        super(message);
    }

    /**
     * Builds an instance of {@link com.tierconnect.riot.iot.kafkastreams.cacheloader.CacheLoaderException}
     *
     * @param message the message
     * @param cause   the cause
     */
    public CacheLoaderException(String message,
                                Throwable cause) {
        super(message, cause);
    }

    /**
     * Builds an instance of {@link com.tierconnect.riot.iot.kafkastreams.cacheloader.CacheLoaderException}
     *
     * @param cause the cause
     */
    public CacheLoaderException(Throwable cause) {
        super(cause);
    }
}
