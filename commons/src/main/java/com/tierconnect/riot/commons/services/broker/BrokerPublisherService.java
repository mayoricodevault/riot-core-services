package com.tierconnect.riot.commons.services.broker;

import com.tierconnect.riot.commons.utils.SerialNumberMapper;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Manages the requests to publish to one or more brokers.
 * Created by vramos on 10/13/16.
 */
public class BrokerPublisherService {

    private static Logger logger = Logger.getLogger(BrokerPublisherService.class);
    private String clientId;

    private SerialNumberMapper serialNumberMapper;

    public BrokerPublisherService(String clientId){
        this.clientId = clientId;
    }

    public void setSerialNumberMapper(SerialNumberMapper serialNumberMapper) {
        this.serialNumberMapper = serialNumberMapper;
    }

    /**
     * Sends one message with one topic to many brokers.
     * @param topic Topic in format /v1/status/ALEB
     * @param body Message to publish
     */
    public List<String> publish(String topic, String key, String body, List<Long> groupMqtt, boolean retry, String bridgeCode) {
        List<String> connectionsTicklesSentTo = new ArrayList<>();
        Map<String, BrokerConnection> mapConnections = MQTTEdgeConnectionPool.getInstance().getLstConnections(bridgeCode, groupMqtt);
        logger.debug(String.format("Publishing to %d mqtt connections...", mapConnections.size()) );
        for (Map.Entry<String, BrokerConnection> map : mapConnections.entrySet()){
            BrokerConnection brokerConnection = map.getValue();
            //If bridgeCode is null send to all otherwise only to bridgeCode
            Map<String, Object> properties = brokerConnection.getProperties();
            String host = properties.get("host").toString();
            int port = Integer.parseInt(properties.get("port").toString());
            int qos = Integer.parseInt(properties.get("qos").toString());
            String username = String.valueOf(properties.get("username"));
            String password = String.valueOf(brokerConnection.getPassword());
            MqttPublisher client = new MqttPublisher(clientId, host, port, qos, username, password);
            if(retry){
                client.publishWithRetry(topic,body);
            }else{
                client.publish(topic,body);
            }
            connectionsTicklesSentTo.add(brokerConnection.toString());
        }

        Collection<KafkaPublisher> kafkaConnections = KafkaConnectionPool.getPublishers();
        if(kafkaConnections.size() > 0) {
            logger.debug(String.format("Publishing to %d kafka connections...", kafkaConnections.size()) );
            for (KafkaPublisher kafkaPublisher : kafkaConnections) {
                //TODO Reny check if services will pint to more than one kafka so we need to filter by {@param bridgeCOde}

                topic = StringUtils.replace(topic, "/", "___");
                if (key != null) {
                    try {
                        int partition = serialNumberMapper.getKafkaPartitionNew(topic, key);
                        kafkaPublisher.publish(topic, partition, key, body);
                        connectionsTicklesSentTo.add(kafkaConnections.toString());
                    } catch (Exception e) {
                        logger.error(String.format("Cannot publish to topic='%s' with key='%s'and body='%s' because cannot get the partition of kafka.",topic,key,body));
                    }
                } else {
                    kafkaPublisher.publish(topic, null, key, body);
                    connectionsTicklesSentTo.add(kafkaConnections.toString());
                }
            }
        }

        return  connectionsTicklesSentTo;
    }

//    /**
//     * Sends one message with one topic to one broker.
//     * @param connection Connection where the message is published
//     * @param topic Topic in format /v1/status/ALEB
//     * @param body Message to publish
//     */
//    public void publish(BrokerConnection connection, String topic, String key, String body, List<Long> groupMqtt){
//        publish(topic, key, body, groupMqtt, false, null);
//    }

}
