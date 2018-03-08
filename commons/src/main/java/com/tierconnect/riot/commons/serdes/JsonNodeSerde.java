package com.tierconnect.riot.commons.serdes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tierconnect.riot.commons.serializers.Constants;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Map;

/**
 * JsonNodeSerde class..
 *
 * @author vramos
 * @version 2017/01/17
 */
public class JsonNodeSerde implements Serde<JsonNode> {

    private Serde<JsonNode> inner;

    public JsonNodeSerde() {
        inner = Serdes.serdeFrom(new JsonNodeSerializer(), new JsonNodeDeserializer());
    }

    @Override
    public void configure(Map<String, ?> configs,
                          boolean isKey) {
        inner.serializer().configure(configs, isKey);
        inner.deserializer().configure(configs, isKey);
    }

    @Override
    public Serializer<JsonNode> serializer() {
        return inner.serializer();
    }

    @Override
    public Deserializer<JsonNode> deserializer() {
        return inner.deserializer();
    }

    @Override
    public void close() {
        inner.serializer().close();
        inner.deserializer().close();
    }

    /**
     * JsonNodeSerializer class..
     *
     * @author vramos
     * @version 2017/01/17
     */
    public static class JsonNodeSerializer implements Serializer<JsonNode> {

        private ObjectMapper mapper = new ObjectMapper();
        SimpleDateFormat df = new SimpleDateFormat(Constants.DATE_FORMAT);

        @Override
        public void configure(Map<String, ?> map,
                              boolean b) {
            mapper.setDateFormat(df);
        }

        @Override
        public byte[] serialize(String topic,
                                JsonNode data) {
            try {
                return mapper.writeValueAsBytes(data);
            } catch (JsonProcessingException e) {
                return null;
            }
        }

        @Override
        public void close() {

        }
    }


    /**
     * JsonNodeDeserializer class..
     *
     * @author vramos
     * @version 2017/01/17
     */
    public static class JsonNodeDeserializer implements Deserializer<JsonNode> {

        private ObjectMapper mapper = new ObjectMapper();
        SimpleDateFormat df = new SimpleDateFormat(Constants.DATE_FORMAT);

        @Override
        public void configure(Map<String, ?> map,
                              boolean b) {
            mapper.setDateFormat(df);
        }

        @Override
        public JsonNode deserialize(String topic,
                                    byte[] data) {
            if (data == null) {
                return null;
            }
            try {
                return mapper.readTree(data);
            } catch (IOException e) {
                return null;
            }
        }

        @Override
        public void close() {

        }
    }
}






