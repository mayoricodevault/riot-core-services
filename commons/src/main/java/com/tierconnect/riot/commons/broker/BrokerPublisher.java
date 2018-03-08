package com.tierconnect.riot.commons.broker;

/**
 * BrokerPublisher interface.
 *
 * @author jantezana
 * @version 2017/06/07
 */
public interface BrokerPublisher {

    /**
     * Initialize the broker publisher.
     */
    void init()
    throws Exception;

    /**
     * Publish.
     *
     * @param topic   the topic
     * @param message the message
     */
    void publish(String topic,
                 String message);

    /**
     * Shutdown.
     */
    void shutdown()
    throws Exception;
}
