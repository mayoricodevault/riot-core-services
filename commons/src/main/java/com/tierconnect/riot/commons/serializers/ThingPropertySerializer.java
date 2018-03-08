package com.tierconnect.riot.commons.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.common.base.Preconditions;
import com.tierconnect.riot.commons.dtos.ThingPropertyDto;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.text.SimpleDateFormat;

/**
 * ThingPropertySerializer class.
 *
 * @author jantezana
 * @version 2017/03/07
 */
public class ThingPropertySerializer extends JsonSerializer<ThingPropertyDto> {

    private static final Logger logger = Logger.getLogger(ThingPropertySerializer.class);
    private final SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT);

    @Override
    public void serialize(ThingPropertyDto thingPropertyDto,
                          JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider)
    throws IOException, JsonProcessingException {
        Preconditions.checkNotNull(thingPropertyDto, "The thingPropertyDto is null");

        ObjectMapper objectMapper = (ObjectMapper) jsonGenerator.getCodec();
        objectMapper.setDateFormat(dateFormat);

        jsonGenerator.writeStartObject();
        jsonGenerator.writeObjectField("id", thingPropertyDto.id);
        jsonGenerator.writeObjectField("time", thingPropertyDto.time);
        jsonGenerator.writeObjectField("blinked", thingPropertyDto.blinked);
        jsonGenerator.writeObjectField("modified", thingPropertyDto.modified);
        jsonGenerator.writeObjectField("timeSeries", thingPropertyDto.timeSeries);
        jsonGenerator.writeObjectField("value", thingPropertyDto.value);
        if (thingPropertyDto.dataTypeId != null) {
            jsonGenerator.writeObjectField("dataTypeId", thingPropertyDto.dataTypeId);
        }
        jsonGenerator.writeEndObject();
    }
}
