package com.tierconnect.riot.commons.broker;

import com.google.common.base.Preconditions;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * KafkaCallback class.
 *
 * @author jantezana
 * @version 2017/06/06
 */
public class KafkaCallback implements Runnable {

    protected final static Logger LOGGER = Logger.getLogger(KafkaCallback.class);
    protected KafkaConsumer<String, String> consumer;
    protected long pollTimeout = Long.MAX_VALUE;

    /**
     * Set the consumer.
     *
     * @param consumer the new consumer
     */
    public void setConsumer(KafkaConsumer<String, String> consumer) {
        Preconditions.checkNotNull(consumer, "The consumer is null");
        this.consumer = consumer;
    }

    /**
     * Sets the poll timeout
     *
     * @param pollTimeout the new poll timeout
     */
    public void setPollTimeout(long pollTimeout) {
        this.pollTimeout = pollTimeout;
    }

    @Override
    public void run() {
        try {
            Preconditions.checkNotNull(this.consumer, "The consumer is null");
            ConsumerRecords<String, String> records;
            Map<String, Object> data;
            while (true) {
                records = this.consumer.poll(this.pollTimeout);
                for (ConsumerRecord<String, String> record : records) {
                    data = new HashMap<>();
                    data.put("topic", record.topic());
                    data.put("key", record.key());
                    data.put("value", record.value());
                    LOGGER.info(data);
                }
            }
        } catch (WakeupException exception) {
            // ignore for shutdown
        } catch (Exception exception) {
            LOGGER.error(exception.getMessage(), exception);
        } finally {
            if (consumer != null) {
                consumer.close();
            }
        }
    }
}
