package com.tierconnect.riot.iot.services;

import com.google.common.base.Preconditions;
import com.tierconnect.riot.commons.broker.KafkaCallback;
import com.tierconnect.riot.commons.utils.Timer;
import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.concurrent.ConcurrentHashMap;

/**
 * StatusServiceKafkaCallback class.
 *
 * @author jantezana
 * @version 2017/06/07
 */
public class StatusServiceKafkaCallback extends KafkaCallback implements StatusServiceCallback {
    private ConcurrentHashMap<String, Timer> timers;

    /**
     * Builds an instance of StatusServiceKafkaCallback.
     *
     * @param timers the map of timers.
     */
    public StatusServiceKafkaCallback(final ConcurrentHashMap<String, Timer> timers) {
        Preconditions.checkNotNull(timers, "The map of timers is null");
        this.timers = timers;
    }

    @Override
    public ConcurrentHashMap<String, Timer> getTimers() {
        return this.timers;
    }

    @Override
    public void run() {
        try {
            Preconditions.checkNotNull(super.consumer, "The consumer is null");
            Timer timer;
            ConsumerRecords<String, String> records;
            String[] t;
            String bridgeCode;
            Edgebox edgebox;


            Session session;
            Transaction transaction;
            while (true) {
                records = super.consumer.poll(super.pollTimeout);
                session = HibernateSessionFactory.getInstance().getCurrentSession();
                transaction = session.getTransaction();
                if (!transaction.isActive()) {
                    transaction.begin();
                }

                for (ConsumerRecord<String, String> record : records) {
                    t = record.topic().split("___");
                    bridgeCode = t[3];
                    edgebox = EdgeboxService.getInstance().selectByCode(bridgeCode);
                    if (edgebox.getStatus() != null && !edgebox.getStatus().equals("OFF")) {
                        timer = this.timers.get(bridgeCode);
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

                }

                transaction.commit();
            }
        } catch (WakeupException exception) {
            // ignore for shutdown
        } catch (Exception exception) {
            super.LOGGER.error(exception.getMessage(), exception);
        } finally {
            if (super.consumer != null) {
                super.consumer.close();
            }
        }
    }
}
