package com.tierconnect.riot.commons.broker.kafka;

import com.google.common.base.Preconditions;
import com.tierconnect.riot.commons.broker.BrokerSubscriber;
import com.tierconnect.riot.commons.broker.KafkaCallback;
import com.tierconnect.riot.commons.broker.KafkaConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * KafkaSubscriber class.
 *
 * @author jantezana
 * @version 2017/06/07
 */
public class KafkaSubscriber implements BrokerSubscriber {
    private static final Logger LOGGER = Logger.getLogger(KafkaSubscriber.class);
    private static final String DEFAULT_ID = "KafkaSubscriber";
    private KafkaConfig kafkaConfig;
    private KafkaConsumer<String, String> consumer;
    private KafkaCallback callback;
    private ExecutorService executor;
    private Pattern pattern;

    /**
     * Builds an instance of KafkaSubscriber.
     *
     * @param kafkaConfig the kafka configuration
     */
    public KafkaSubscriber(final KafkaConfig kafkaConfig) {
        Preconditions.checkNotNull(kafkaConfig, "The kafka configuration is null");
        this.kafkaConfig = kafkaConfig;
        this.callback = new KafkaCallback();
        final Properties consumerProperties = this.buildConsumerProperties();
        this.consumer = new KafkaConsumer<>(consumerProperties);
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * Builds an instance of KafkaSubscriber.
     *
     * @param kafkaConfig the kafka configuration
     * @param callback    the kafka callback.
     */
    public KafkaSubscriber(KafkaConfig kafkaConfig,
                           KafkaCallback callback) {
        Preconditions.checkNotNull(kafkaConfig, "The kafka configuration is null");
        this.kafkaConfig = kafkaConfig;
        this.callback = (callback != null) ? callback : new KafkaCallback();
        final Properties consumerProperties = this.buildConsumerProperties();
        this.consumer = new KafkaConsumer<>(consumerProperties);
    }

    /**
     * Gets the consumer.
     *
     * @return the consumer
     */
    public KafkaConsumer<String, String> getConsumer() {
        return consumer;
    }

    /**
     * Sets the kafka callback.
     *
     * @param callback the new kafka callback
     */
    public void setCallback(final KafkaCallback callback) {
        Preconditions.checkNotNull(callback, "The kafka callback is null");
        this.callback = callback;
    }

    @Override
    public void init()
    throws Exception {
        this.callback.setConsumer(this.consumer);
        this.executor.execute(this.callback);

        LOGGER.info("Initialized the kafka client ...");
    }

    /**
     * Builds the consumer properties.
     *
     * @return the consumer properties
     */
    private Properties buildConsumerProperties() {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.kafkaConfig.server);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, String.format("%s-%s-sub", DEFAULT_ID, UUID.randomUUID().toString()));
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        return properties;
    }

    @Override
    public void subscribe(String... topic) {
        Preconditions.checkNotNull(topic, "The topic is null");
        this.consumer.subscribe(Arrays.asList(topic));
    }

    /**
     * Subscribe using a pattern
     *
     * @param pattern                   the pattern
     * @param consumerRebalanceListener the consumer rebalance listener
     */
    public void subscriber(String pattern,
                           ConsumerRebalanceListener consumerRebalanceListener) {
        Preconditions.checkNotNull(pattern, "The pattern is null");
        this.pattern = Pattern.compile(pattern);
        if (consumerRebalanceListener == null) {
            consumerRebalanceListener = new DefaultConsumerRebalanceListener();
        }
        this.consumer.subscribe(this.pattern, consumerRebalanceListener);
    }

    @Override
    public void shutdown()
    throws Exception {
        if (this.consumer != null) {
            this.consumer.close();
        }

        if (this.executor != null) {
            this.executor.shutdown();
        }
    }
}
