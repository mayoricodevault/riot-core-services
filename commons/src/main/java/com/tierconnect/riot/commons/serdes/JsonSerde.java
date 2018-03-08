package com.tierconnect.riot.commons.serdes;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

/**
 * JsonSerde class..
 *
 * @author vramos
 * @version 2017/01/13
 */
public class JsonSerde<T> implements Serde<T> {

    private Serde<T> inner;
    private Class<T> deserializedClass;

    public JsonSerde(Class<T> deserializedClass) {
        this.deserializedClass = deserializedClass;
        inner = Serdes.serdeFrom(new JsonSerializer(), new JsonDeserializer<T>(deserializedClass));
    }

    @Override
    public void configure(Map<String, ?> configs,
                          boolean isKey) {
        inner.serializer().configure(configs, isKey);
        inner.deserializer().configure(configs, isKey);
        if (deserializedClass == null) {
            deserializedClass = (Class<T>) configs.get("serializedClass");
        }
    }

    @Override
    public void close() {
        inner.serializer().close();
        inner.deserializer().close();
    }

    @Override
    public Serializer<T> serializer() {
        return inner.serializer();
    }

    @Override
    public Deserializer<T> deserializer() {
        return inner.deserializer();
    }
}
