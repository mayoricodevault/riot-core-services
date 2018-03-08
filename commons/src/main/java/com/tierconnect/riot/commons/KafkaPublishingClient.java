package com.tierconnect.riot.commons;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.Serializable;
import java.util.Properties;
import java.util.UUID;

/***
 * see http://allegro.tech/2015/08/spark-kafka-integration.html for the
 * motivation behind this class
 *
 * @author tcrown
 *
 */
public class KafkaPublishingClient implements Serializable {
    private static final long serialVersionUID = 1L;

    private static Logger logger = Logger.getLogger(KafkaPublishingClient.class);

    private String servers;

    @SuppressWarnings("unused")
    private String clientId;

    private KafkaProducer<String, String> producer;

    private static KafkaPublishingClient instance;

    public static synchronized KafkaPublishingClient getInstance(String servers) {
        if (instance == null) {
            instance = new KafkaPublishingClient(servers,
                                                 "kafka-pub-" + UUID.randomUUID().toString());
        }
        return instance;
    }

    private KafkaPublishingClient(String servers,
                                  String clientId) {
        this.servers = servers;
        this.clientId = clientId;
    }

    public void init()
    throws IOException {
        Properties props = new Properties();
        props.put("bootstrap.servers", servers);
        // props.put( "acks", "1" ); // "1" is default
        props.put("retries", 0);
        // props.put( "batch.size", 16384 ); // 16384 is default
        // props.put( "linger.ms", 0 ); // ""
        // props.put( "buffer.memory", 33554432 ); // 33554432 is detauld
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("block.on.buffer.full", true);
        producer = new KafkaProducer<String, String>(props);
    }

    public void publish(ProducerRecord<String, String> pr)
    throws IOException {
        try {
            // lazy init the producer when needed (as it is not serialized and deserialized)
            if (producer == null) {
                logger.info("initializing KafkaPublishingClient hasCode=" + this.hashCode());
                init();
            }

            producer.send(pr);
            // Future<RecordMetadata> f = producer.send( pr );
            // f.get();//if can not send, this line throw exception
        } catch (Exception e) {
            logger.error("Cannot publish message to topic " + pr.topic(), e);
        }
    }
}
