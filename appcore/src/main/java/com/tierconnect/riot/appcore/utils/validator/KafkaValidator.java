package com.tierconnect.riot.appcore.utils.validator;

import com.tierconnect.riot.appcore.entities.ConnectionType;
import com.tierconnect.riot.commons.services.broker.MqttPublisher;
import com.tierconnect.riot.commons.utils.KafkaZkUtils;
import com.tierconnect.riot.commons.utils.SerialNumberMapper;
import com.tierconnect.riot.commons.utils.Topics;
import org.apache.commons.lang.StringUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.log4j.Logger;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;

import java.util.Properties;
import java.util.concurrent.Future;


public class KafkaValidator implements ConnectionValidator {
    private static Logger logger = Logger.getLogger(KafkaValidator.class);
    private int status;
    private String cause;

    @Override public int getStatus() {
        return status;
    }

    @Override public String getCause() {
        return cause;
    }

    @Override public boolean testConnection(ConnectionType connectionType, String properties) {
        JSONParser parser = new JSONParser();
        try {
            JSONObject kafkaProperties = (JSONObject) parser.parse(properties);



            KafkaZkUtils kafkaZkUtils = new KafkaZkUtils(String.valueOf(kafkaProperties.get("zookeeper")));
            if (!kafkaZkUtils.isConnectionActive()) {
                status = 400;
                cause = "Zookeeper is not active";
                return false;
            }
            try {
                SerialNumberMapper serialNumberMapper = new SerialNumberMapper();
                serialNumberMapper.addTopic("___v1___test", kafkaZkUtils.getNumberOfPartitions("___v1___test"));
                kafkaZkUtils.deleteTopic("___v1___test");
            } catch (Exception exception) {
                logger.error("Mistakes adding the topic in serial number mapper.");
                status = 400;
                cause = "Cannot add topic in serial number mapper";
                return false;
            }


            Properties props = new Properties();

            props.put("client.id","validator");
            props.put("bootstrap.servers", kafkaProperties.get("server"));

            props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

            props.put("acks", "all"); // topic's leader will wait ack from all followers
            props.put("max.block.ms", 5000);
            KafkaProducer<String, String> client = new KafkaProducer<>(props);

            ProducerRecord<String,String> record =
                new ProducerRecord<>(Topics.BROKER_CHECK.getKafkaName(), "connection_validator",
                    String.format("{\"uuid\": \"%s\", \"ts\":%d}", "validator", System.currentTimeMillis()));
            Future<RecordMetadata> result = client.send(record);
            client.close();
            result.get();
            status = 200;
            cause = "Success";
            return true;

        } catch (ParseException e) {
            status = 400;
            cause = "Cannot parse configuration, " + e.getMessage();
            logger.warn("Cannot parse connection properties.", e);
            return false;
        } catch (Exception e){
            status = 400;
            cause = e.getMessage();
            return false;
        }
    }
}
