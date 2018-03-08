package com.tierconnect.riot.commons.broker.kafka;

import com.google.common.base.Preconditions;
import com.tierconnect.riot.commons.broker.BrokerPublisher;
import com.tierconnect.riot.commons.broker.KafkaConfig;
import org.apache.commons.lang.StringUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.log4j.Logger;

import java.util.Properties;
import java.util.UUID;

/**
 * KafkaPublisher class.
 *
 * @author jantezana
 * @version 2017/06/07
 */
public class KafkaPublisher implements BrokerPublisher {
    private static final Logger LOGGER = Logger.getLogger(KafkaPublisher.class);
    private static final String DEFAULT_ID = "KafkaPublisher";
    private KafkaConfig kafkaConfig;
    private KafkaProducer<String, String> producer;

    /**
     * Builds an instance of KafkaPublisher
     *
     * @param kafkaConfig the kafka configuration
     */
    public KafkaPublisher(final KafkaConfig kafkaConfig) {
        Preconditions.checkNotNull(kafkaConfig, "The kafka configuration is null");
        this.kafkaConfig = kafkaConfig;
    }

    @Override
    public void init()
    throws Exception {
        Properties producerProperties = this.buildProducerProperties();
        this.producer = new KafkaProducer<>(producerProperties);
        LOGGER.info("Initialized the kafka publisher ...");
    }

    /**
     * Builds the producer properties.
     *
     * @return the producer properties
     */
    private Properties buildProducerProperties() {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, this.kafkaConfig.server);
        properties.put(ProducerConfig.BATCH_SIZE_CONFIG, 65536);
        properties.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, String.format("%s-%s-pub", DEFAULT_ID, UUID.randomUUID().toString()));
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return properties;
    }

    @Override
    public void publish(final String topic,
                        final String message) {
        Preconditions.checkNotNull(topic, "The topic is null");
        Preconditions.checkNotNull(message, "The message is null");

        final String formattedTopic = StringUtils.replace(topic, "/", "___");
        final ProducerRecord<String, String> record = new ProducerRecord<>(formattedTopic, message);
        this.producer.send(record);
    }

    @Override
    public void shutdown()
    throws Exception {
        if (this.producer != null) {
            this.producer.close();
        }
    }
}
