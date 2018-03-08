package com.tierconnect.riot.commons.services.broker;

import java.text.BreakIterator;
import java.util.*;

import org.apache.log4j.Logger;
import scala.tools.ant.sabbus.Break;

/**
 * Class intended to have a pool of actime MQTT connections.
 * But for now this will only hold connections and when sending
 * messages it will create a new client for each message to be send.
 * 
 * 
 * @author Renan Huanca
 */
public class MQTTEdgeConnectionPool {
    private static final Logger logger = Logger.getLogger(MQTTEdgeConnectionPool.class);
    
    private static boolean initialized = false;
    private List<BrokerEdgeBox> mqttEdgeConnections = new ArrayList<>();
    private static MQTTEdgeConnectionPool instance = new MQTTEdgeConnectionPool();

    public static MQTTEdgeConnectionPool getInstance(){
        return instance;
    }

    public synchronized void init(List<BrokerEdgeBox> connections) {
        if(initialized) {
            logger.info("Already initialized.");
            return;
        }
        
        // initialize only MQTT connections
        connections.stream().filter(x->x.getMqttConnection()!=null).forEach(x->mqttEdgeConnections.add(x));
        initialized = true;
    }

    /**
     * Add a new EdgeConnection
     * @param brokerEdgeBox Object EdgeBox
     */
    public synchronized void addEdgeConnection(BrokerEdgeBox brokerEdgeBox){
        mqttEdgeConnections.add(brokerEdgeBox);
    }

    /**
     * Update a new EdgeConnection
     * @param newBrokerEdgeBox Object EdgeBox
     */
    public synchronized  void updateEdgeConnection(BrokerEdgeBox newBrokerEdgeBox){
        BrokerEdgeBox edgeBox = null;
        for(BrokerEdgeBox brokerEdgebox: mqttEdgeConnections){
            if(brokerEdgebox.getCode().equals(newBrokerEdgeBox.getCode())){
                edgeBox = brokerEdgebox;
                break;
            }
        }
        if(edgeBox!=null){
            mqttEdgeConnections.remove(edgeBox);
        }
        mqttEdgeConnections.add(newBrokerEdgeBox);
    }

    /**
     * Delete Edge connection
     * @param brokerEdgeBoxCode Code broker EdgeBox code
     */
    public synchronized void deleteEdgeConnection(String brokerEdgeBoxCode){
        BrokerEdgeBox edgeBox = null;
        for(BrokerEdgeBox brokerEdgebox: mqttEdgeConnections){
            if(brokerEdgebox.getCode().equals(brokerEdgeBoxCode)){
                edgeBox = brokerEdgebox;
                break;
            }
        }
        if(edgeBox!=null){
            mqttEdgeConnections.remove(edgeBox);
        }
    }

    /**
     * Filtering List of connection according to the group Id
     * @param groupMqtt List of group Ids
     * @return List filtered by group Id
     */
    public Map<String, BrokerConnection> getLstConnections(String bridgeCode, List<Long> groupMqtt) {
        //TODO Use implementation by one value in order to get a List of Connections
        Map<String, BrokerConnection> results = new HashMap<>();
        if (!mqttEdgeConnections.isEmpty()) {
            if ( (bridgeCode != null) && !bridgeCode.trim().isEmpty() ) {
                mqttEdgeConnections.stream()
                        .filter(x -> x.getCode().equals(bridgeCode))
                        .forEach(x -> results.put(x.getMqttConnection().getProperties().get("host")
                                        +":"+x.getMqttConnection().getProperties().get("port")
                                ,x.getMqttConnection()));
            } else if( (groupMqtt != null) && (!groupMqtt.isEmpty()) ){
                groupMqtt.forEach(groupId-> {
                            for (BrokerEdgeBox x : mqttEdgeConnections) {
                                if (x.getGroupId().compareTo(groupId) == 0) {
                                    results.put(x.getMqttConnection().getProperties().get("host")
                                                    + ":" + x.getMqttConnection().getProperties().get("port")
                                            , x.getMqttConnection());
                                    break;
                                }
                            }
                        });
            } else {
                mqttEdgeConnections.stream()
                        .forEach(x -> results.put(x.getMqttConnection().getProperties().get("host")
                                        +":"+x.getMqttConnection().getProperties().get("port"),
                                x.getMqttConnection()));
            }
        }
        return results;
    }

    /**
     * Get List of all connections by bridgeCode
     * @param bridgeCode Edge Bridge code
     * @return List og Broker Connections
     */
    public Map<String, BrokerConnection> getLstConnections(String bridgeCode) {
        Map<String, BrokerConnection> results = new HashMap<>();
        if ( (bridgeCode != null) && !bridgeCode.trim().isEmpty() ) {
            mqttEdgeConnections.stream()
                    .filter(x -> x.getCode().equals(bridgeCode))
                    .forEach(x -> results.put(x.getMqttConnection().getProperties().get("host")
                                    +":"+x.getMqttConnection().getProperties().get("port")
                            ,x.getMqttConnection()));
        }
        return results;
    }

    /**
     * Get List of all connections by bridgeCode
     * @param groupMqtt List of groups where we need to find MQTT Connections
     * @return List og Broker Connections
     */
    public Map<String, BrokerConnection> getLstConnections(List<Long> groupMqtt) {
        Map<String, BrokerConnection> results = new HashMap<>();
        if( (groupMqtt != null) && (!groupMqtt.isEmpty()) ){
            groupMqtt.forEach(groupId-> {
                for (BrokerEdgeBox x : mqttEdgeConnections) {
                    if (x.getGroupId().compareTo(groupId) == 0) {
                        results.put(x.getMqttConnection().getProperties().get("host")
                                        + ":" + x.getMqttConnection().getProperties().get("port")
                                , x.getMqttConnection());
                        break;
                    }
                }
            });
        }
        return results;
    }

    /**
     * Get All List of all connections
     * @return List og Broker Connections
     */
    public Map<String, BrokerConnection> getAllLstConnections() {
        Map<String, BrokerConnection> results = new HashMap<>();
        mqttEdgeConnections.stream()
                .forEach(x -> results.put(x.getMqttConnection().getProperties().get("host")
                                +":"+x.getMqttConnection().getProperties().get("port"),
                        x.getMqttConnection()));
        return results;
    }
}
