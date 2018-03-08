package com.tierconnect.riot.commons.serdes;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * JsonDeserializer class..
 *
 * @author vramos
 * @version 2017/01/13
 */
public class JsonDeserializer<T> implements Deserializer<T> {

    private static Logger logger = Logger.getLogger(JsonDeserializer.class);

    private ObjectMapper mapper = new ObjectMapper();
    private Class<T> deserializedClass;

    public JsonDeserializer(Class<T> deserializedClass) {
        this.deserializedClass = deserializedClass;
    }

    @Override
    public void configure(Map<String, ?> map,
                          boolean b) {
        if (deserializedClass == null) {
            deserializedClass = (Class<T>) map.get("serializedClass");
        }
    }

    @Override
    public T deserialize(String topic,
                         byte[] data) {
        if (data == null) {
            return null;
        }
        try {
            return mapper.reader().forType(deserializedClass).readValue(data);
        } catch (IOException e) {
            String message = new String(data, Charset.forName("UTF-8"));
            logger.error("Cannot parse message to json: "+message, e);
            return null;
        }
    }

    @Override
    public void close() {
    }
}
