package com.tierconnect.riot.commons.broker.mqtt;

import com.google.common.base.Preconditions;
import com.tierconnect.riot.commons.broker.BrokerSubscriber;
import com.tierconnect.riot.commons.broker.MqttConfig;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.UUID;

/**
 * MqttSubscriber class.
 *
 * @author jantezana
 * @version 2017/06/07
 */
public class MqttSubscriber implements BrokerSubscriber {
    private static final Logger LOGGER = Logger.getLogger(MqttSubscriber.class);
    private static final String DEFAULT_ID = "MqttClient";
    public static final int DEFAULT_QOS = 2;
    private MqttConfig mqttConfig;
    private MqttAsyncClient client;
    private MqttCallback mqttCallback;

    /**
     * Builds an instance of MqttSubscriber
     *
     * @param mqttConfig the mqtt configuration
     */
    public MqttSubscriber(final MqttConfig mqttConfig)
    throws MqttException {
        this(mqttConfig, null);
    }

    /**
     * Builds an instance of MqttSubscriber
     *
     * @param mqttConfig   the mqtt configuration
     * @param mqttCallback the mqtt callback
     */
    public MqttSubscriber(final MqttConfig mqttConfig,
                          final MqttCallback mqttCallback)
    throws MqttException {
        Preconditions.checkNotNull(mqttConfig, "The mqtt configuration is null");
        this.mqttConfig = mqttConfig;
        this.mqttCallback = mqttCallback;

        String url = String.format("tcp://%s:%d", this.mqttConfig.host, this.mqttConfig.port);
        String clientId = String.format("%s-%s", DEFAULT_ID, UUID.randomUUID().toString());
        this.client = new MqttAsyncClient(url, clientId);
    }

    /**
     * Sets the mqtt callback.
     *
     * @param mqttCallback the new mqtt callback
     */
    public void setMqttCallback(final MqttCallback mqttCallback) {
        Preconditions.checkNotNull(mqttCallback, "The mqttCallback is null");
        this.mqttCallback = mqttCallback;
    }

    /**
     * Gets the client.
     *
     * @return the client
     */
    public MqttAsyncClient getClient() {
        return client;
    }

    /**
     * Initialize.
     *
     * @throws Exception
     */
    public void init()
    throws Exception {
        if (this.mqttCallback == null) {
            DefaultMqttCallback callback = new DefaultMqttCallback();
            callback.setClient(this.client);
            this.client.setCallback(callback);
        } else {
            this.client.setCallback(this.mqttCallback);
        }

        MqttConnectOptions options = new MqttConnectOptions();
        String username = this.mqttConfig.username;
        String password = this.mqttConfig.password;

        if (StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(password)) {
            options.setUserName(username);
            options.setPassword(password.toCharArray());
        }

        this.client.connect(options).waitForCompletion();
    }

    @Override
    public void subscribe(String... topic) {
        final int[] qos = new int[topic.length];
        for (int i = 0; i < topic.length; i++) {
            qos[i] = DEFAULT_QOS;
        }

        try {
            this.client.subscribe(topic, qos);
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

    /**
     * DefaultMqttCallback class.
     *
     * @author jantezana
     * @version 2017/06/07
     */
    private static class DefaultMqttCallback implements MqttCallback {
        private static final Logger LOGGER = Logger.getLogger(DefaultMqttCallback.class);
        public static final int DELAY_MILLIS = 3000;
        private MqttAsyncClient client;

        /**
         * Sets the mqtt client.
         *
         * @param client the new mqtt client
         */
        public void setClient(final MqttAsyncClient client) {
            Preconditions.checkNotNull(client, "The mqtt client is null");
            this.client = client;
        }

        @Override
        public void connectionLost(Throwable cause) {
            LOGGER.error("Connection lost ...");
            Preconditions.checkNotNull(client, "The mqtt client is null");
            try {
                while (!this.client.isConnected()) {
                    this.client.reconnect();
                    Thread.sleep(DELAY_MILLIS);
                }
            } catch (MqttException mqttException) {
                LOGGER.error(mqttException.getMessage(), mqttException);
            } catch (InterruptedException exception) {
                LOGGER.error(exception.getMessage(), exception);
            }
        }

        @Override
        public void messageArrived(String topic,
                                   MqttMessage message)
        throws Exception {
            LOGGER.info(String.format("Topic: %s, Message: %s", topic, message.toString()));
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            LOGGER.info("deliveryComplete ...");
        }
    }
}
