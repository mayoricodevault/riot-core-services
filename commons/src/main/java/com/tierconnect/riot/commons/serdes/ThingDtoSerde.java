package com.tierconnect.riot.commons.serdes;

import com.tierconnect.riot.commons.dtos.ThingDto;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

/**
 * ThingDtoSerde class..
 *
 * @author jantezana
 * @version 2017/02/22
 */
public class ThingDtoSerde implements Serde<ThingDto> {

    private Serde<ThingDto> inner;

    public ThingDtoSerde() {
        inner = Serdes.serdeFrom(new ThingDtoSerializer(), new ThingDtoDeserializer());
    }

    @Override
    public void configure(Map<String, ?> configs,
                          boolean isKey) {
        inner.serializer().configure(configs, isKey);
        inner.deserializer().configure(configs, isKey);
    }

    @Override
    public void close() {
        inner.serializer().close();
        inner.deserializer().close();
    }

    @Override
    public Serializer<ThingDto> serializer() {
        return inner.serializer();
    }

    @Override
    public Deserializer<ThingDto> deserializer() {
        return inner.deserializer();
    }
}
