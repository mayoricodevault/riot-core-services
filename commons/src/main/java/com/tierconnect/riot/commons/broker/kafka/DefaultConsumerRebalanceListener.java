package com.tierconnect.riot.commons.broker.kafka;


import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.common.TopicPartition;
import org.apache.log4j.Logger;

import java.util.Collection;

/**
 * DefaultConsumerRebalanceListener class.
 *
 * @author jantezana
 * @version 2017/06/08
 */
public final class DefaultConsumerRebalanceListener implements ConsumerRebalanceListener {
    private static final Logger LOGGER = Logger.getLogger(DefaultConsumerRebalanceListener.class);

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        LOGGER.info("partitions revoked:" + partitions);
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        LOGGER.info("partitions assigned:" + partitions);
    }
}
