package com.tierconnect.riot.commons.services.broker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.tierconnect.riot.commons.dtos.ConnectionDto;
import com.tierconnect.riot.commons.dtos.EdgeboxConfigurationDto;
import com.tierconnect.riot.commons.dtos.EdgeboxDto;
import com.tierconnect.riot.commons.dtos.EdgeboxRuleDto;
import com.tierconnect.riot.commons.dtos.GroupDto;
import com.tierconnect.riot.commons.dtos.GroupTypeDto;
import com.tierconnect.riot.commons.dtos.LogicalReaderDto;
import com.tierconnect.riot.commons.dtos.ShiftDto;
import com.tierconnect.riot.commons.dtos.ShiftZoneDto;
import com.tierconnect.riot.commons.dtos.ThingTypeDto;
import com.tierconnect.riot.commons.dtos.ZoneDto;
import com.tierconnect.riot.commons.dtos.ZoneTypeDto;
import com.tierconnect.riot.commons.utils.Topics;
import org.apache.commons.lang.StringUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static com.tierconnect.riot.commons.serializers.Constants.DATE_FORMAT;

/**
 * Publisher implementation for kafka.
 * Sends messages in format ___v1___status___ALEB using ___ as SEPARATOR
 * Created by vramos on 10/12/16.
 */
public class KafkaPublisher {

    private static Logger logger = Logger.getLogger(KafkaPublisher.class);

    private final String STARTUP_KEY = "services_startup";
    public static final String[] VALID_TOPICS = {"___v1___data", "___v1___cache", "___v1___edge___dn___thingCache", "___v1___bridgeAgent___control"};
    private final String SEPARATOR = "___";
    private String servers;
    private ObjectMapper objectMapper;
    private static KafkaProducer<String, String> client;

    private String clientId;

    private String connectionCode;
    private String connectionType;

    public KafkaPublisher(String servers, String clientId, String connectionCode, String connectionType){
        this.servers = servers;
        this.clientId = clientId;

        this.connectionCode = connectionCode;
        this.connectionType = connectionType;

        // Initialize date format.
        this.objectMapper = new ObjectMapper();
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        this.objectMapper.setDateFormat(dateFormat);
    }

    /**
     * Initiates connection to kafka brokers
     */
    public synchronized void initConnection() {

        Properties props = new Properties();

        props.put("client.id","SERVICES");
        props.put("bootstrap.servers", servers);

        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        props.put("acks", "all"); // topic's leader will wait ack from all followers
        props.put("max.block.ms", 5000);

        // This verifies problems with the client initialization. e.g. DNS problem
        boolean retry = true;
        do{
            try {
                client = new KafkaProducer<>(props);
                retry = false;
            } catch (Exception e){
                if(client != null){
                    client.close();
                }
                logger.warn(String.format("\nCheck if exists a connection to kafka server: %s. \n1. Verify if the ip:port is correct.\n2. Verify if kafka container is reachable from the services container. Use ping.\n3. Verify if kafka container port is open. Use telnet.\n4. Verify logs of kafka and zookeeper, possibly is a connection problem between kafka and zookeeper. ",servers));
                logger.warn("Startup process for [Services] waiting for [Kafka], retry in 30s");
                try {
                    Thread.sleep(30000);
                } catch (InterruptedException e1) {
                }
            }
        }while(retry);
        checkConecction();
    }

    /**
     *
     * @param topic
     * @param idKey
     * @param codeKey
     * @param message
     */
    public void updateCache(final String topic,final String idKey, final String codeKey, Object message){

        Preconditions.checkNotNull(topic, "topic is null");
        Preconditions.checkNotNull(topic, "idKey is null");
        Preconditions.checkNotNull(topic, "codeKey is null");

        if (topic.equals(Topics.CACHE_ZONE.getKafkaName())){
            ZoneDto zoneDto = (ZoneDto)message;
            publish(topic,zoneDto,idKey);
            publish(topic,zoneDto,codeKey);
        }else  if (topic.equals(Topics.CACHE_ZONE_TYPE.getKafkaName())) {
            ZoneTypeDto zoneTypeDto = (ZoneTypeDto) message;
            publish(topic, zoneTypeDto, idKey);
            publish(topic, zoneTypeDto, codeKey);
        }else  if (topic.equals(Topics.CACHE_THING_TYPE.getKafkaName())){
            ThingTypeDto thingTypeDto=(ThingTypeDto)message;
            publish(topic,thingTypeDto,idKey);
            publish(topic,thingTypeDto,codeKey);
        }else if(topic.equals(Topics.CACHE_GROUP.getKafkaName())){
            GroupDto groupDto=(GroupDto)message;
            publish(topic,groupDto,idKey);
            publish(topic,groupDto,codeKey);
        } else if(topic.equals(Topics.CACHE_SHIFT.getKafkaName())){
            ShiftDto shiftDto=(ShiftDto)message;
            publish(topic,shiftDto,idKey);
            publish(topic,shiftDto,codeKey);
        } else if(topic.equals(Topics.CACHE_SHIFT_ZONE.getKafkaName())){
            ShiftZoneDto shiftZoneDto=(ShiftZoneDto)message;
            publish(topic,shiftZoneDto,idKey);
            publish(topic,shiftZoneDto,codeKey);
        } else if(topic.equals(Topics.CACHE_LOGICAL_READER.getKafkaName())){
            LogicalReaderDto logicalReaderDto=(LogicalReaderDto)message;
            publish(topic,logicalReaderDto,idKey);
            publish(topic,logicalReaderDto,codeKey);
        } else if(topic.equals(Topics.CACHE_EDGEBOX.getKafkaName())){
            EdgeboxDto edgeboxDto=(EdgeboxDto)message;
            publish(topic,edgeboxDto,idKey);
            publish(topic,edgeboxDto,codeKey);
        } else if(topic.equals(Topics.CACHE_EDGEBOXES_CONFIGURATION.getKafkaName())){
            EdgeboxConfigurationDto edgeboxConfigurationDto=(EdgeboxConfigurationDto) message;
            publish(topic,edgeboxConfigurationDto,idKey);
            publish(topic,edgeboxConfigurationDto,codeKey);
        } else if(topic.equals(Topics.CACHE_CONNECTION.getKafkaName())){
            ConnectionDto connectionDto=(ConnectionDto)message;
            publish(topic,connectionDto,idKey);
            publish(topic,connectionDto,codeKey);
        } else {
            logger.error(String.format("Cannot updateCache for topic='%s', idKey='%s', codeKey='%s'",topic,idKey,codeKey));
        }
    }

    /**
     *
     * @param topic
     *            Topic in format ___v1___cache___zone
     * @param key
     * @param message
     */
    public void updateCacheByKey(final String topic,final String key, Object message){

        Preconditions.checkNotNull(topic, "topic is null");
        Preconditions.checkNotNull(topic, "key is null");

        if(topic.equals(Topics.CACHE_GROUP_TYPE.getKafkaName())){
            GroupTypeDto groupTypeDto=(GroupTypeDto)message;
            publish(topic,groupTypeDto,key);
        } else if(topic.equals(Topics.CACHE_EDGEBOX_RULE.getKafkaName())){
            EdgeboxRuleDto edgeboxRuleDto=(EdgeboxRuleDto) message;
            publish(topic,edgeboxRuleDto,key);
        } else if(topic.equals(Topics.CACHE_THING.getKafkaName())) {
            publish(topic, message, key);
        } else {
            logger.error(String.format("Cannot updateCache for topic='%s', key='%s'",topic,key));
        }
    }

    /**
     *
     * @param topic
     * @param message
     * @param key
     */
    public void publish(final String topic,final  Object message, final String key){
        this.publish(topic, null, key, message);
    }

    public void publish(final String topic, final Integer partition, final String key, final  Object message){
        boolean loop = true;
        int count = 0;

        String messageString = null;
        try {
            if (message != null){
                if (message instanceof String) {
                    messageString = message.toString();
                } else {
                    messageString = this.objectMapper.writeValueAsString(message);
                }
            }
        } catch (JsonProcessingException e) {
            logger.error(String.format("mistakes publishing the message: %s", message), e);
            return;
        }

        while (loop && count < 10) {
            try {
                send(topic, partition, key, messageString);
                loop = false;
            } catch (Exception e) {
                count++;
                logger.warn("retrying publish ! count=" + count, e);
            }
        }
        if (loop) {
            logger.info("ERROR: FAILED PUBLISH: servers=" + servers + " topic = " + topic + " message = " + message);
            logger.error("Connection refused, there is a problem with connectivity to Kafka.");
        }
    }

    /**
     * Publishes a message to kafka and retries 10 times if it fails
     * @param topic Receives the topic in format /v1/status/ALEB but this is translated with the SEPARATOR variable value
     * @param body Message to publish
     */
    public void publishWithRetry(String topic, String body) {
        boolean retry = true;

        do{
            try {
                topic = topic.replaceAll("/", SEPARATOR);
                ProducerRecord<String, String> record = new ProducerRecord<String, String>(topic, body == null ? "" : body);
                Future<RecordMetadata> result = client.send(record);
                result.get();//if can not send, this line throw exception
                retry = false;
            } catch (Exception e) {
                logger.warn("Startup process for [Services] waiting for [Kafka], retry in 30s");
                try {
                    Thread.sleep(30000);
                } catch (InterruptedException e1) {
                    Thread.currentThread().interrupt();
                }

            }
        }while(retry);
    }

    private void send(final String topic, final Integer partition, final String key, String message) throws ExecutionException, InterruptedException {
        if(client == null) {
            throw new IllegalStateException("client is not initialized.");
        }

        if (StringUtils.startsWithAny(topic, VALID_TOPICS)) {
            ProducerRecord<String, String> record;
            if (partition == null) {
                record = new ProducerRecord<>(topic, key, message);
            } else {
                record = new ProducerRecord<>(topic, partition, key, message);
            }

            Future<RecordMetadata> result = client.send(record);
            result.get(); //if can not send, this line throw exception
        } else {
            if (logger.isDebugEnabled()){
                logger.debug(String.format("Skipped ... the following topic: %s because is not supported in kafka core bridge", topic));
            }
        }

    }

    /**
     * Verifies if is possible send a message to kafka
     */
    private void checkConecction(){
        boolean retry = true;
        do{
            try {
                ProducerRecord<String,String> record = new ProducerRecord<String, String>(Topics.BROKER_CHECK.getKafkaName(),STARTUP_KEY,String.format("{\"uuid\": \"%s\", \"ts\":%d}", clientId, System.currentTimeMillis()));
                Future<RecordMetadata> result = client.send(record);
                result.get();
                retry = false;
            } catch (Exception e){
                logger.warn(String.format("\nCheck if exists a connection to kafka server %s and services is able to publish to kafka. \n1. Verify if the ip:port is correct.\n2. Verify if kafka container is reachable from the services container. Use ping.\n3. Verify if kafka container port is open. Use telnet.\n4. Verify logs of kafka and zookeeper, possibly is a connection problem between kafka and zookeeper. \n5. Verify if the topic '%s' exists in kafka.\n6. Verify the kafka configuration: advertised.listeners. \n\tIt's a environment variable for docker.\n\tIt's in KAFKA_HOME/config/server.properties for standalone.",servers,Topics.BROKER_CHECK.getKafkaName()));
                logger.warn("Startup process for [Services] waiting for [Kafka], retry in 30s");
                try {
                    Thread.sleep(30000);
                } catch (InterruptedException e1) {
                    Thread.currentThread().interrupt();
                }
            }
        }while(retry);
    }

    @Override
    public String toString(){
        return "Connection code: " + connectionCode + ", type: " + connectionType + ", host: " + servers;
    }
}


