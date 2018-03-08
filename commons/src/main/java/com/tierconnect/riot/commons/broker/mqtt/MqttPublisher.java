package com.tierconnect.riot.commons.broker.mqtt;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.tierconnect.riot.commons.broker.BrokerPublisher;
import com.tierconnect.riot.commons.broker.MqttConfig;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.UUID;

/**
 * MqttPublisher class.
 *
 * @author jantezana
 * @version 2017/06/07
 */
public class MqttPublisher implements BrokerPublisher {

    private static final Logger LOGGER = Logger.getLogger(MqttPublisher.class);
    private static final String DEFAULT_ID = "MqttPublisher";
    private MqttConfig mqttConfig;
    private MqttAsyncClient client;

    /**
     * Builds an instance of MqttPublisher.
     *
     * @param mqttConfig the mqtt configuration
     */
    public MqttPublisher(MqttConfig mqttConfig) {
        this.mqttConfig = mqttConfig;
    }

    @Override
    public void init() throws MqttException {
        String url = String.format("tcp://%s:%d", this.mqttConfig.host, this.mqttConfig.port);
        String clientId = String.format("%s-%s-pub", DEFAULT_ID, UUID.randomUUID().toString());
        this.client = new MqttAsyncClient(url, clientId);

        final MqttConnectOptions options = new MqttConnectOptions();
        String username = this.mqttConfig.username;
        String password = this.mqttConfig.password;

        if (username!=null && StringUtils.isNotEmpty(username) && password!=null && StringUtils.isNotEmpty(password)) {
            options.setUserName(username);
            options.setPassword(password.toCharArray());
        }

        this.client.connect(options).waitForCompletion();
    }

    @Override
    public void publish(final String topic,
                        final String message) {
        Preconditions.checkNotNull(topic, "The topic is null");
        Preconditions.checkNotNull(message, "The message is null");

        try {
            this.client.publish(topic, new MqttMessage(message.getBytes(Charsets.UTF_8)));
        } catch (MqttException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Override
    public void shutdown()
    throws Exception {
        if (this.client != null) {
            this.client.close();
        }
    }
}
