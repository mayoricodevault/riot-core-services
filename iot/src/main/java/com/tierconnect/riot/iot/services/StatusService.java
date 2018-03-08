package com.tierconnect.riot.iot.services;

import com.google.common.base.Preconditions;
import com.tierconnect.riot.appcore.services.ConfigurationService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.commons.broker.BrokerSubscriber;
import com.tierconnect.riot.commons.broker.KafkaConfig;
import com.tierconnect.riot.commons.broker.MqttConfig;
import com.tierconnect.riot.commons.broker.kafka.KafkaSubscriber;
import com.tierconnect.riot.commons.broker.mqtt.MqttSubscriber;
import com.tierconnect.riot.commons.services.broker.*;
import com.tierconnect.riot.commons.utils.Timer;
import org.apache.log4j.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * StatusService class.
 *
 * @author fflores
 * @author jantezana
 */
public class StatusService {

    private static StatusService INSTANCE;
    private static final Logger LOGGER = Logger.getLogger(StatusService.class);
    private ConcurrentHashMap<String, Timer> timers = new ConcurrentHashMap<>();
    private List<StatusServiceCallback> statusServiceCallbacks = new LinkedList<>();
    private List<BrokerSubscriber> brokerSubscribers = new LinkedList<>();
    private Long bridgeErrorStatusTimeout;

    /**
     * Initialize the status service
     *
     * @param clientId Client ID
     */
    public void init(String clientId) {

        bridgeErrorStatusTimeout = Long.parseLong(
                ConfigurationService.getAsString(GroupService.getInstance().getRootGroup(), "bridgeErrorStatusTimeout"));
        MqttConfig mqttConfig;
        KafkaConfig kafkaConfig;
        MqttSubscriber mqttSubscriber;
        KafkaSubscriber kafkaSubscriber;
        StatusServiceMqttCallback statusServiceMqttCallback;
        StatusServiceKafkaCallback statusServiceKafkaCallback;
        Map<String, BrokerConnection> mapConnections = MQTTEdgeConnectionPool.getInstance().getAllLstConnections();
        for (Map.Entry<String, BrokerConnection> map : mapConnections.entrySet()){
            BrokerConnection brokerConnection = map.getValue();
            try {
                statusServiceMqttCallback = new StatusServiceMqttCallback(this.timers);
                mqttConfig = buildMqttConfig(brokerConnection);
                mqttSubscriber = new MqttSubscriber(mqttConfig);
                statusServiceMqttCallback.setClient(mqttSubscriber.getClient());
                mqttSubscriber.setMqttCallback(statusServiceMqttCallback);
                mqttSubscriber.init();
                mqttSubscriber.subscribe("/v1/bridgestatus/#");

                this.brokerSubscribers.add(mqttSubscriber);
                this.statusServiceCallbacks.add(statusServiceMqttCallback);
            } catch (Exception exception) {
                LOGGER.error(exception.getMessage(), exception);
            }
        }

        for (BrokerConnection brokerConnection : KafkaConnectionPool.getConnections()) {
            try {
                statusServiceKafkaCallback = new StatusServiceKafkaCallback(this.timers);
                kafkaConfig = buildKafkaConfig(brokerConnection);
                kafkaSubscriber = new KafkaSubscriber(kafkaConfig);
                kafkaSubscriber.setCallback(statusServiceKafkaCallback);
                kafkaSubscriber.subscriber("___v1___bridgestatus___.+", null);
                kafkaSubscriber.init();

                this.brokerSubscribers.add(kafkaSubscriber);
                this.statusServiceCallbacks.add(statusServiceKafkaCallback);
            } catch (Exception exception) {
                LOGGER.error(exception.getMessage(), exception);
            }
        }
    }

    /**
     * Builds a mqtt configuration
     *
     * @param brokerConnection the broker connection
     * @return the mqtt configuration
     */
    private MqttConfig buildMqttConfig(BrokerConnection brokerConnection) {
        Preconditions.checkNotNull(brokerConnection, "The broker connection is null");
        MqttConfig mqttConfig = new MqttConfig();
        Map<String, Object> properties = brokerConnection.getProperties();
        Object host = properties.get("host");
        mqttConfig.host = (host != null) ? host.toString() : null;
        Object port = properties.get("port");
        mqttConfig.port = (port != null) ? Integer.parseInt(port.toString()) : -1;
        Object username = properties.get("username");
        mqttConfig.username = (username != null) ? username.toString() : "";
        Object password = properties.get("password");
        mqttConfig.password = (password != null) ? password.toString() : "";
        return mqttConfig;
    }

    /**
     * Builds the kafka configuration.
     *
     * @param brokerConnection the broker connection
     * @return the kafka configuration
     */
    private KafkaConfig buildKafkaConfig(BrokerConnection brokerConnection) {
        Preconditions.checkNotNull(brokerConnection, "The broker connection is null");
        KafkaConfig kafkaConfig = new KafkaConfig();
        Map<String, Object> properties = brokerConnection.getProperties();
        Object server = properties.get("server");
        kafkaConfig.server = (server != null) ? server.toString() : "";
        Object zookeeper = properties.get("zookeeper");
        kafkaConfig.zookeeper = (zookeeper != null) ? zookeeper.toString() : "";
        return kafkaConfig;
    }

    public static StatusService getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new StatusService();
        }
        return INSTANCE;
    }

    private StatusService() {
    }

    public void start(String bridgeCode) {
        timers.put(bridgeCode, new Timer().start(bridgeCode + "-status"));
    }

    public void stop(String bridgeCode) {
        if (!timers.isEmpty() && timers.containsKey(bridgeCode)) {
            timers.get(bridgeCode).stop(bridgeCode + "-status");
            timers.remove(bridgeCode);
        }
    }

    public List<StatusServiceCallback> getStatusServiceCallbacks() {
        return statusServiceCallbacks;
    }

    public Long getBridgeErrorStatusTimeout() {
        return bridgeErrorStatusTimeout;
    }

    public void setBridgeErrorStatusTimeout(Long bridgeErrorStatusTimeout) {
        this.bridgeErrorStatusTimeout = bridgeErrorStatusTimeout;
    }

    /**
     * Gets the list of broker subscribers.
     *
     * @return the list of broker subscribers
     */
    public List<BrokerSubscriber> getBrokerSubscribers() {
        return brokerSubscribers;
    }
}
