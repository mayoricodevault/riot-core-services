package com.tierconnect.riot.commons.broker;

/**
 * BrokerSubscriber interface.
 *
 * @author jantezana
 * @version 2017/06/07
 */
public interface BrokerSubscriber {

    /**
     * Initialize the broker subscriber.
     */
    void init()
    throws Exception;

    /**
     * Subscribe to list of topics.
     *
     * @param topic the list of topics
     */
    void subscribe(String... topic);

    /**
     * Shutdown.
     */
    void shutdown()
    throws Exception;
}
