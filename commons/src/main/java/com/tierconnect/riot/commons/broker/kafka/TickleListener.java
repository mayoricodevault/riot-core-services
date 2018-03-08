package com.tierconnect.riot.commons.broker.kafka;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Observable;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

/**
 * TickleListener class.
 *
 * @author jantezana
 * @version 2017/06/19
 */
public class TickleListener extends Observable implements Runnable {
    private static final Logger logger = Logger.getLogger(TickleListener.class);
    private static final String DEFAULT_GROUP = "TickleListener";
    private static final String ID = "ID";
    private static final long DEFAULT_POLL_TIMEOUT = 100L;
    private Set<String> topics;
    private KafkaConsumer<String, String> consumer;

    /**
     * Builds a instance of TickleListener.
     *
     * @param servers the servers
     */
    public TickleListener(final String servers) {
        Preconditions.checkNotNull(servers, "The servers are null");
        String groupId = String.format("%s-%s", DEFAULT_GROUP, UUID.randomUUID().toString());
        Properties properties = buildConsumerProperties(servers, groupId);
        this.consumer = new KafkaConsumer<>(properties);
        this.topics = new LinkedHashSet<>();
    }

    /**
     * Builds the consumer properties.
     *
     * @param servers the servers
     * @param groupId the group Id
     * @return the consumer properties
     */
    private Properties buildConsumerProperties(final String servers,
                                               final String groupId) {
        final Properties consumerProperties = new Properties();
        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        return consumerProperties;
    }

    /**
     * Sets the list of topics.
     *
     * @param topics the new list of topics
     */
    public void setTopics(final Set<String> topics) {
        Preconditions.checkNotNull(topics, "The list of topics is null");
        this.topics = topics;
    }

    /**
     * Adds a topic.
     *
     * @param topic the topic
     */
    public void addTopic(final String topic) {
        Preconditions.checkNotNull(topic, "The topic is null");
        this.topics.add(topic);
    }

    @Override
    public void run() {
        try {
            logger.info("Starting the tickle listener ...");
            this.consumer.subscribe(topics);

            Map<String, ConsumerRecord<String, String>> recordsMap;
            ConsumerRecords<String, String> records;
            boolean continueReading;
            int maxNonReads;
            int nonReads;
            String key;

            while (true) {
                maxNonReads = 10;
                nonReads = 0;
                continueReading = true;
                recordsMap = new LinkedHashMap<>();

                while (continueReading) {
                    records = this.consumer.poll(DEFAULT_POLL_TIMEOUT);
                    nonReads = (records.count() > 0) ? 0 : nonReads + 1;
                    if (nonReads > maxNonReads) {
                        continueReading = false;
                    }

                    for (ConsumerRecord<String, String> record : records) {
                        key = record.key();
                        if (StringUtils.startsWith(key, ID)) {
                            recordsMap.put(record.key(), record);
                        }
                    }
                }

                for (ConsumerRecord<String, String> record : recordsMap.values()) {
                    super.setChanged();
                    super.notifyObservers(record);
                }
            }
        } catch (WakeupException e) {
            logger.info("ignore for shutdown");
        } finally {
            consumer.close();
        }
    }

    /**
     * Shutdown.
     */
    public void shutdown() {
        consumer.wakeup();
    }
}
