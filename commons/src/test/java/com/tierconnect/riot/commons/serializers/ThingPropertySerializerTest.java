package com.tierconnect.riot.commons.serializers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tierconnect.riot.commons.dtos.ThingPropertyDto;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.GregorianCalendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * ThingPropertySerializerTest class.
 *
 * @author jantezana
 * @version 2017/02/07
 */
public class ThingPropertySerializerTest {
    private static final Logger logger = Logger.getLogger(ThingPropertySerializerTest.class);

    private ObjectMapper objectMapper;

    @Before
    public void setUp()
    throws Exception {
        this.objectMapper = new ObjectMapper();
    }

    @Test
    public void serializeAThingPropertyWithBooleanDataTypeId()
    throws IOException {
        ThingPropertyDto thingPropertyDto = new ThingPropertyDto();
        thingPropertyDto.id = 40L;
        thingPropertyDto.time = new GregorianCalendar(2017, 1, 1).getTime();
        thingPropertyDto.blinked = false;
        thingPropertyDto.modified = false;
        thingPropertyDto.value = true;
        thingPropertyDto.dataTypeId = 5L;

        String outputTarget = this.objectMapper.writeValueAsString(thingPropertyDto);
        assertNotNull(outputTarget);
        String expectedJson = buildJsonWithBooleanThingPropertyValue();
        assertEquals(expectedJson, outputTarget.toString());
    }

    @Test
    public void serializeAThingPropertyWithNumberDataTypeId()
    throws IOException {
        ThingPropertyDto thingPropertyDto = new ThingPropertyDto();
        thingPropertyDto.id = 40L;
        thingPropertyDto.time = new GregorianCalendar(2017, 1, 1).getTime();
        thingPropertyDto.blinked = false;
        thingPropertyDto.modified = false;
        thingPropertyDto.value = 1;
        thingPropertyDto.dataTypeId = 4L;

        String outputTarget = this.objectMapper.writeValueAsString(thingPropertyDto);
        assertNotNull(outputTarget);
        String expectedJson = buildJsonWithNumberThingPropertyValue();
        assertEquals(expectedJson, outputTarget.toString());
    }

    @Test
    public void serializeAThingPropertyWithDateDataTypeId()
    throws IOException {
        ThingPropertyDto thingPropertyDto = new ThingPropertyDto();
        thingPropertyDto.id = 40L;
        thingPropertyDto.time = new GregorianCalendar(2017, 1, 1).getTime();
        thingPropertyDto.blinked = false;
        thingPropertyDto.modified = false;
        thingPropertyDto.value = new GregorianCalendar(2017, 1, 1).getTime();
        thingPropertyDto.dataTypeId = 11L;

        String outputTarget = this.objectMapper.writeValueAsString(thingPropertyDto);
        assertNotNull(outputTarget);
        String expectedJson = buildJsonWithDateThingPropertyValue();
        assertEquals(expectedJson, outputTarget.toString());
    }

    @Test
    public void serializeANullThingProperty()
    throws IOException {
        ThingPropertyDto thingPropertyDto = new ThingPropertyDto();
        thingPropertyDto.id = 40L;
        thingPropertyDto.time = new GregorianCalendar(2017, 1, 1).getTime();
        thingPropertyDto.blinked = false;
        thingPropertyDto.modified = false;
        thingPropertyDto.value = null;
        thingPropertyDto.dataTypeId = 4L;

        String outputTarget = this.objectMapper.writeValueAsString(thingPropertyDto);
        assertNotNull(outputTarget);
        String expectedJson = buildJsonWithNullThingPropertyValue();
        assertEquals(expectedJson, outputTarget.toString());
    }

    @Test
    public void testPerformance()
    throws IOException {
        ThingPropertyDto thingPropertyDto = new ThingPropertyDto();
        thingPropertyDto.id = 40L;
        thingPropertyDto.time = new GregorianCalendar(2017, 1, 1).getTime();
        thingPropertyDto.blinked = false;
        thingPropertyDto.modified = false;
        thingPropertyDto.value = new GregorianCalendar(2017, 1, 1).getTime();
        thingPropertyDto.dataTypeId = 11L;

        ObjectMapper objectMapper = new ObjectMapper();
        String expectedJson = buildJsonWithDateThingPropertyValue();
        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            String actualJson = objectMapper.writeValueAsString(thingPropertyDto);
            assertEquals(expectedJson, actualJson);

        }
        long end = System.currentTimeMillis();
        logger.info(String.format("time: %d", end - start));
    }

    private String buildJsonWithNullThingPropertyValue() {
        String output = "{\"id\":40,\"time\":\"2017-02-01T04:00:00.000Z\",\"blinked\":false,\"modified\":false,\"timeSeries\":null,\"value\":null,\"dataTypeId\":4}";
        return output;
    }

    private String buildJsonWithBooleanThingPropertyValue() {
        String output = "{\"id\":40,\"time\":\"2017-02-01T04:00:00.000Z\",\"blinked\":false,\"modified\":false,\"timeSeries\":null,\"value\":true,\"dataTypeId\":5}";
        return output;
    }

    private String buildJsonWithNumberThingPropertyValue() {
        String output = "{\"id\":40,\"time\":\"2017-02-01T04:00:00.000Z\",\"blinked\":false,\"modified\":false,\"timeSeries\":null,\"value\":1,\"dataTypeId\":4}";
        return output;
    }

    private String buildJsonWithDateThingPropertyValue() {
        String output = "{\"id\":40,\"time\":\"2017-02-01T04:00:00.000Z\",\"blinked\":false,\"modified\":false,\"timeSeries\":null,\"value\":\"2017-02-01T04:00:00.000Z\",\"dataTypeId\":11}";
        return output;
    }

}