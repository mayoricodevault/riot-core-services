package com.tierconnect.riot.commons.services.broker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * Holds live kafka connections.
 * We need to have live kafka connections, because doing connections every time
 * we need a send a message is very time expensive.
 * @author Renan Huanca
 *
 */
public class KafkaConnectionPool {
    private static final String KAFKA_SERVER_PROPERTY = "server";

    private static final Logger logger = Logger.getLogger(KafkaConnectionPool.class);
    
    private static boolean initialized = false;
    private static Map<String, KafkaPublisher> pool = new HashMap<>();
    private static List<BrokerConnection> connections = new ArrayList<>();
    
    public synchronized static void init(List<BrokerEdgeBox> brokerConnections, String clientId) {
        if(initialized) {
            logger.info("Already initialized.");
            return;
        }
        
        // initialize only KAFKA brokerConnections
        Map<String, BrokerConnection> tempMap = new HashMap<>();
        brokerConnections.stream().filter(x->x.getKafkaConnection()!=null).forEach(brokerEdgeBox->{
            BrokerConnection conn = brokerEdgeBox.getKafkaConnection();
            String servers = (String)conn.getProperties().get(KAFKA_SERVER_PROPERTY);
            servers = servers.trim();
            boolean exists = tempMap.get(servers) != null;
            StringBuilder sb = new StringBuilder();
            sb.append("Initializing kafka connection:\n");
            sb.append("\tcode: "+ conn.getCode()).append("\n");
            sb.append("\tservers: " + servers);
            logger.info(sb.toString());
            if(!exists) {
                connections.add(conn);
                String kafkaClientCode = clientId + conn.getCode();
                logger.info("\tkafkaClientCode: " + kafkaClientCode);
                KafkaPublisher publisher = new KafkaPublisher(servers, kafkaClientCode, conn.getCode(), conn.getConnectionType());
                publisher.initConnection();
                pool.put(conn.getCode(), publisher);
                tempMap.put(servers, conn);
            } else {
                logger.info("Kafka connection already registered.");
            }
        });
        initialized = true;
    }
 
    public static List<BrokerConnection> getConnections() {
        return connections;
    }
    public static Collection<KafkaPublisher> getPublishers() {
        return pool.values();
    }
}
