package com.tierconnect.riot.commons.services.broker;

import org.apache.commons.io.Charsets;
import org.apache.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * Publisher implementation for mosquitto
 * Created by vramos on 10/12/16.
 */
public class MqttPublisher implements BrokerPublisher {

    private static Logger logger = Logger.getLogger(MqttPublisher.class);
    private String clientId;
    private String host;
    private int port;
    private int qos;
    private String mqttUsername;
    private String mqttPassword;

    /**
     * @param clientId User-specified string sent in each request to help trace calls
     * @param host
     * @param port
     * @param qos
     * @param mqttUsername
     * @param mqttPassword
     */
    public MqttPublisher(String clientId, String host, int port, int qos, String mqttUsername, String mqttPassword){
        this.clientId = clientId;
        this.host = host;
        this.port = port;
        this.qos = qos;
        this.mqttUsername = mqttUsername;
        this.mqttPassword = mqttPassword;
    }

    /**
     * Publishes a message to mosquitto and retries 10 times if it fails
     * @param topic Receives the topic in format /v1/status/ALEB
     * @param body Message to publish
     */
    public boolean publish(String topic, String body){
        synchronized (logger) {
            boolean loop = true;
            int count = 0;
            while (loop && count < 10) {
                try {
                    publish0(host, port, qos, topic, body == null ? "" : body, 0);
                    logger.trace("stacktrace=", new Exception("stacktrace"));
                    logger.debug("OK: SENDING MQTT MESSAGE: COUNT=" + count + " " + host + ":" + port + " topic='" + topic
                        + "' body='" + body + "'");
                    loop = false;
                } catch (Exception e) {
                    count++;
                    logger.warn("retrying publish ! count=" + count);
                }
            }
            if (loop) {
                logger.info("ERROR: FAILED PUBLISH: host=" + host+ " port = "+port+" topic = " + topic + " body = " + body);
                return false;
            }
        }
        return true;
    }

    public String publishTest(){
        try {
            publish0(host, port, qos, "/v1/test", "connectionTest", 5);
            logger.trace("stacktrace=", new Exception("stacktrace"));
        } catch (Exception e) {
            return e.toString();
        }
        return "";
    }

    /**
     * Publishes a message to mosquitto and retries 10 times if it fails
     * @param topic Receives the topic in format /v1/status/ALEB
     * @param body Message to publish
     */
    public void publishWithRetry(String topic, String body){
        synchronized (logger) {
            boolean retry = true;
            do{

                try {
                    publish0(host, port, qos, topic, body == null ? "" : body, 0);
                    logger.trace("stacktrace=", new Exception("stacktrace"));
                    logger.debug("OK: SENDING MQTT MESSAGE: " + host + ":" + port + " topic='" + topic
                            + "' body='" + body + "'");
                    retry = false;
                } catch (Exception e) {
                    logger.warn("Startup process for [Services] waiting for [MQTT], retry in 30s");
                    try {
                        Thread.sleep(30000);
                    } catch (InterruptedException e1) {
                        Thread.currentThread().interrupt();
                    }
                }
            }while(retry);

        }
    }

    private void publish0(String mqttHost, int mqttPort, int qos, String topic, String body, int timeout) throws MqttException {
        boolean controlAuth=false;
        String serverURI = "tcp://" + mqttHost + ":" + mqttPort;
        MemoryPersistence persistence = new MemoryPersistence();
        MqttClient client = new MqttClient(serverURI, clientId, persistence);

        MqttConnectOptions co = new MqttConnectOptions();
        if (mqttUsername != null && !mqttUsername.isEmpty() && mqttPassword != null && !mqttPassword.isEmpty()) {
            controlAuth=true;
            co.setUserName(mqttUsername);
            co.setPassword(mqttPassword.toCharArray());
        }
        co.setCleanSession(true);
        if (timeout > 0) {
            co.setConnectionTimeout(timeout);
        }
        client.connect(co);

        MqttMessage message = new MqttMessage(body.getBytes(Charsets.UTF_8));
        message.setQos(qos);

        client.publish(topic, message);
        if (controlAuth){
            logger.debug("Publishing with MQTT Authentication active for user="+mqttUsername+" to serverURI="+serverURI);
        }
        client.disconnect();
    }
}
