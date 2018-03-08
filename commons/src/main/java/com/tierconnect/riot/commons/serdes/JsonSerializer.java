package com.tierconnect.riot.commons.serdes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tierconnect.riot.commons.serializers.Constants;
import org.apache.kafka.common.serialization.Serializer;

import java.text.SimpleDateFormat;
import java.util.Map;

/**
 * JsonSerde class..
 *
 * @author vramos
 * @version 2017/01/13
 */
public class JsonSerializer<T> implements Serializer<T> {

    private ObjectMapper mapper = new ObjectMapper();
    private SimpleDateFormat df = new SimpleDateFormat(Constants.DATE_FORMAT);

    @Override
    public void configure(Map<String, ?> map,
                          boolean b) {
    }

    @Override
    public byte[] serialize(String topic,
                            T data) {
        try {
            mapper.setDateFormat(df);
            return mapper.writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            return new byte[0];
        }
    }

    @Override
    public void close() {
    }
}
