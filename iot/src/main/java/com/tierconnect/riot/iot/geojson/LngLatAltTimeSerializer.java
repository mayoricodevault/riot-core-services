package com.tierconnect.riot.iot.geojson;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class LngLatAltTimeSerializer extends JsonSerializer<LngLatAltTime>  {

    @Override
    public void serialize(LngLatAltTime value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
        jgen.writeStartArray();
        jgen.writeNumber(value.getLongitude());
        jgen.writeNumber(value.getLatitude());
        
        if (value.hasColor()) {
        	jgen.writeNumber(value.getColor());
        }
        else
        	jgen.writeNumber(value.getAltitude());
        	
        if (value.hasTime()) {
            jgen.writeNumber(value.getTime());
        }
        
        jgen.writeEndArray();
    }

}
