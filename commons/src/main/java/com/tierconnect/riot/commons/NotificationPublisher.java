package com.tierconnect.riot.commons;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

/**
 * NotificationPublisher class.
 *
 * @author jantezana
 * @author dbascope
 * @version 2016/12/16
 */
public class NotificationPublisher implements Serializable {

    private List<String> recipients;
    private static NotificationPublisher instance;
    private static MqttClient mqttClient;
    private int qos;
    private String topic;
    private static boolean initialized;

    /**
     * Creates an instance of {@link NotificationPublisher}
     *
     * @param host       the host
     * @param port       the port
     * @param qos        the qos
     * @param topic      the topic
     * @param recipients the recipients
     * @throws MqttException the error
     */
    private NotificationPublisher(final String host,
                                  final int port,
                                  final int qos,
                                  final String topic,
                                  List<String> recipients)
    throws MqttException {
        this.qos = qos;
        this.topic = topic;
        String serverURI = String.format("tcp://%s:%d", host, port);
        MemoryPersistence persistence = new MemoryPersistence();
        mqttClient = new MqttClient(serverURI, "mqtt", persistence);
        MqttConnectOptions co = new MqttConnectOptions();
        mqttClient.connect(co);
        co.setCleanSession(true);
        this.recipients = recipients;
    }

    /**
     * Initialize a {@link NotificationPublisher}
     *
     * @param params the params
     * @return the notification publisher
     * @throws MqttException the exception
     */
    @Deprecated
    public static synchronized NotificationPublisher init(Map<String, Object> params)
    throws MqttException {
        if (instance == null || mqttClient == null) {
            //noinspection unchecked
            instance = new NotificationPublisher(String.valueOf(params.get("mqttHost")),
                                                 Integer.parseInt(
                                                     String.valueOf(params.get("mqttPort"))),
                                                 Integer.parseInt(
                                                     String.valueOf(params.get("mqttQos"))),
                                                 String.valueOf(params.get("topic")),
                                                 (List<String>) params.get("recipients"));
        }

        return instance;
    }

    /**
     * Initialize a {@link NotificationPublisher}
     *
     * @param host       the host
     * @param port       the port
     * @param qos        the qos
     * @param topic      the topic
     * @param recipients the recipients
     * @return the notification publisher
     * @throws MqttException the exception
     */
    public static synchronized void initIfNeeded(final String host,
                                                 final int port,
                                                 final int qos,
                                                 final String topic,
                                                 List<String> recipients)
    throws MqttException {
        if (!initialized) {
            init(host, port, qos, topic, recipients);
        }
    }

    /**
     * Initialize the notification publisher.
     *
     * @param host       the host
     * @param port       the port
     * @param qos        the qos
     * @param topic      the topic
     * @param recipients the recipients
     * @throws MqttException the mqtt exception
     */
    private static void init(final String host,
                             final int port,
                             final int qos,
                             final String topic,
                             List<String> recipients)
    throws MqttException {
        instance = new NotificationPublisher(host, port, qos, topic, recipients);
        initialized = true;
    }

    /**
     * Gets the instance.
     *
     * @return
     */
    public static synchronized NotificationPublisher getInstance() {
        if (instance == null) {
            throw new RuntimeException(
                "You have to init the notification publisher calling the initIfNeeded method");
        }

        return instance;
    }


    /**
     * Format message.
     *
     * @param body       the body
     * @param stacktrace the stacktrace
     * @return the formatted message
     */
    private String formatMessage(final String body,
                                 final String stacktrace) {
        String bodyMessage = StringEscapeUtils.escapeJava(StringEscapeUtils.escapeJava(
            body.replaceAll("\\\\+n|\n", "<br />").replaceAll("\\\\+t|\t", "&emsp;")));
        String bodyStacktrace = "";
        if (stacktrace == null) {
            for (StackTraceElement stack : Thread.currentThread().getStackTrace()) {
                bodyStacktrace += stack.toString() + "\n";
            }
        } else {
            bodyStacktrace = stacktrace;
        }
        bodyStacktrace = bodyStacktrace.replaceAll("\\\\+n|\n", "<br />").replaceAll("\\\\+t|\t",
                                                                                     "&emsp;");
        return "An error occurred, here is a detail of what happened:<p>"
            + "<table border=0><tr><th align='right' valign='top'>Error Message</th><td>&nbsp;</td><td>"
            + bodyMessage
            + "</td></tr><tr><th align='right' valign='top'>Error Stacktrace</th><td>&nbsp;</td><td>"
            + bodyStacktrace + "</td></tr></table></p>";
    }

    /**
     * Notify.
     *
     * @param body the body
     */
    private void notify(String body) {
        String mqttMessage = "{\"mqtt-body\":\"{\\\"mqtt\\\":{\\\"connectionCode\\\":\\\"MQTT\\\"},"
            + "\\\"contentType\\\":\\\"text/html; charset=utf-8\\\",\\\"subject\\\":\\\"Spark CoreBridge "
            + "notification:\\\",\\\"to\\\":" + this.formatRecipients(recipients)
            + ",\\\"email-body\\\":\\\"" + body
            + "\\\"}\", \"contentType\":\"text/html; charset=utf-8\"}";
        try {
            if (!mqttClient.isConnected()) {
                mqttClient.reconnect();
            }
            MqttMessage message = new MqttMessage(mqttMessage.getBytes(Charset.forName("UTF-8")));
            message.setQos(qos);
            mqttClient.publish(topic, message);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * Notify.
     *
     * @param body    the body
     * @param subject the subject
     */
    private void notify(String body,String subject) {
        String mqttMessage = "{\"mqtt-body\":\"{\\\"mqtt\\\":{\\\"connectionCode\\\":\\\"MQTT\\\"},"
                + "\\\"contentType\\\":\\\"text/html; charset=utf-8\\\",\\\"subject\\\":\\\""+subject
                + "notification:\\\",\\\"to\\\":" + this.formatRecipients(recipients)
                + ",\\\"email-body\\\":\\\"" + body
                + "\\\"}\", \"contentType\":\"text/html; charset=utf-8\"}";
        try {
            if (!mqttClient.isConnected()) {
                mqttClient.reconnect();
            }
            MqttMessage message = new MqttMessage(mqttMessage.getBytes(Charset.forName("UTF-8")));
            message.setQos(qos);
            mqttClient.publish(topic, message);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    /**
     * Send the notification.
     *
     * @param body      the body
     * @param throwable the error
     */
    public void sendNotification(String body,
                                 Throwable throwable) {
        String stacktrace = null;
        if (body == null) {
            body = "ERROR";
        }

        if (throwable != null) {
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw, true);
            throwable.printStackTrace(pw);
            stacktrace = sw.getBuffer().toString();
        }
        notify(formatMessage(body, stacktrace));
    }

    /**
     * Send the notification.
     *
     * @param body      the body
     * @param subject   the subject
     * @param throwable the error
     */
    public void sendNotification(String body,String subject,
                                 Throwable throwable) {
        String stacktrace = null;
        if (body == null) {
            body = "ERROR";
        }

        if (throwable != null) {
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw, true);
            throwable.printStackTrace(pw);
            stacktrace = sw.getBuffer().toString();
        }
        notify(formatMessage(body, stacktrace),subject);
    }

    /**
     * Send notification.
     *
     * @param body the body
     */
    public void sendNotification(String body) {
        sendNotification(body, null);
    }

    /**
     * Format recipients list to json format.
     *
     * @param recipients the list of recipients
     * @return the formatted list of recipients
     */
    private String formatRecipients(List<String> recipients) {
        String output = "";
        for (String recipient : recipients) {
            if (!StringUtils.isBlank(output)) {
                output += ",";
            }
            output += "\\\"" + recipient + "\\\"";
        }
        return "[" + output + "]";
    }
}
