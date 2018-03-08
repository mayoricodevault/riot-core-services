package com.tierconnect.riot.iot.geojson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;

/**
 * Created by user on 11/3/14.
 */
public class PinStyleSerializer extends JsonSerializer<PinStyle> {
    @Override
    public void serialize(PinStyle value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
        jgen.writeStartArray();
        jgen.writeString(value.getColor());
        jgen.writeString(value.getIconImage());
        jgen.writeNumber(value.getTimestamp());
        jgen.writeEndArray();
    }
}
