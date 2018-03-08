package com.tierconnect.riot.iot.services;

import com.google.common.base.Preconditions;
import com.tierconnect.riot.commons.utils.Timer;
import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;
import org.apache.log4j.Logger;
import org.eclipse.paho.client.mqttv3.*;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.concurrent.ConcurrentHashMap;

import static com.tierconnect.riot.commons.broker.mqtt.MqttSubscriber.DEFAULT_QOS;

/**
 * StatusServiceMqttCallback class.
 *
 * @author fflores
 * @author jantezana
 * @version 2017/06/07
 */
public class StatusServiceMqttCallback implements StatusServiceCallback, MqttCallbackExtended {
    private final static Logger LOGGER = Logger.getLogger(StatusServiceCallback.class);
    private MqttAsyncClient client;
    private ConcurrentHashMap<String, Timer> timers;

    /**
     * Builds an instance of StatusServiceMqttCallback.
     *
     * @param timers the timers
     */
    public StatusServiceMqttCallback(final ConcurrentHashMap<String, Timer> timers) {
        this.timers = timers;
    }

    /**
     * Sets the mqtt client.
     *
     * @param client the new mqtt client
     */
    public void setClient(MqttAsyncClient client) {
        this.client = client;
    }

    @Override
    public ConcurrentHashMap<String, Timer> getTimers() {
        return this.timers;
    }

    @Override
    public void connectionLost(Throwable throwable) {
        LOGGER.error("connectionLost ...");
        Preconditions.checkNotNull(this.client, "The client is null");
        while (!this.client.isConnected()) {
            try {
                this.client.connect();
                this.client.subscribe("/v1/bridgestatus/#", DEFAULT_QOS);
            } catch (MqttException e) {
                LOGGER.error(e.getMessage(), e);
                try {
                    Thread.sleep(3000);
                    LOGGER.info("Trying to reconnect");
                } catch (InterruptedException e1) {
                    LOGGER.error("", e);
                }
            }
        }
    }

    @Override
    public void messageArrived(final String topic,
                               MqttMessage mqttMessage)
    throws Exception {
        String[] t = topic.split("/");
        String bridgeCode = t[3];

        Transaction transaction;
        Session session = HibernateSessionFactory.getInstance().getCurrentSession();
        transaction = session.getTransaction();
        transaction.begin();

        final Edgebox edgebox = EdgeboxService.getInstance().selectByCode(bridgeCode);
        if (edgebox.getStatus() != null && !edgebox.getStatus().equals("OFF")) {
            Timer timer = this.timers.get(bridgeCode);
            if (timer == null) {
                timer = new Timer();
                this.timers.put(bridgeCode, timer);
            }

            timer.start(bridgeCode + "-status");

            if (edgebox.getStatus().equals("ERROR")) {
                edgebox.setStatus("ON");
                EdgeboxService.getInstance().update(edgebox);
            }
        }
        transaction.commit();
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        try {
            if (!reconnect) {
                LOGGER.info("Connection completed");
            } else {
                LOGGER.info("Reconnection completed");
            }
            client.subscribe("/v1/bridgestatus/#", DEFAULT_QOS);
            LOGGER.info("Subscription completed to [/v1/bridgestatus/#] to serverURI=" + serverURI);
        } catch (MqttException e) {
            LOGGER.error("It is not possible to subscribe", e);
        }
    }
}
