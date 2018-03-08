package com.tierconnect.riot.commons.services.broker;

/**
 * Generic publisher to send messages to a broker.
 * This interface and its implementations apply strategy pattern in order to publish to different brokers
 * Created by vramos on 10/12/16.
 */
interface BrokerPublisher {
    void publishWithRetry(String topic, String body);

    boolean publish(String topic, String body);
}
