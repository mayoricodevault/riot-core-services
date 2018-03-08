package com.tierconnect.riot.commons.serializers;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.tierconnect.riot.commons.dtos.DataTypeDto;
import com.tierconnect.riot.commons.dtos.GroupDto;
import com.tierconnect.riot.commons.dtos.GroupTypeDto;
import com.tierconnect.riot.commons.dtos.MetaDto;
import com.tierconnect.riot.commons.dtos.ThingDto;
import com.tierconnect.riot.commons.dtos.ThingPropertyDto;
import com.tierconnect.riot.commons.dtos.ThingTypeDto;
import com.tierconnect.riot.commons.dtos.ThingTypeFieldDto;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * ThingSerializerTest class.
 *
 * @author jantezana
 * @author achambi
 * @version 2017/01/26
 */
public class ThingSerializerTest {

    private ObjectMapper objectMapper;

    @Before
    public void setUp()
    throws Exception {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new SimpleModule() {
            @Override
            public void setupModule(SetupContext context) {
                super.setupModule(context);
                context.addBeanSerializerModifier(new BeanSerializerModifier() {
                    @Override
                    public JsonSerializer<?> modifySerializer(SerializationConfig config,
                                                              BeanDescription beanDesc,
                                                              JsonSerializer<?> serializer) {
                        if (ThingDto.class.isAssignableFrom(beanDesc.getBeanClass())) {
                            return new ThingSerializer();
                        }

                        return serializer;
                    }
                });
            }
        });
    }

    @Test
    public void serializeANullThingProperty()
    throws IOException {
        ThingDto thingDto = buildThingDtoWithANullPropertyValue();
        String outputTarget = this.objectMapper.writeValueAsString(thingDto);
        assertNotNull(outputTarget);
        String expectedJson = buildThingDtoJsonWithNullThingPropertyValue();
        assertEquals(expectedJson, outputTarget.toString());
    }

    @Test
    public void serializeAThingPropertyWithBooleanDataTypeId()
    throws IOException {
        ThingDto thingDto = buildThingDtoWithABooleanDataTypeId();
        String outputTarget = this.objectMapper.writeValueAsString(thingDto);
        assertNotNull(outputTarget);
        String expectedJson = buildThingDtoJsonWithBooleanThingPropertyValue();
        assertEquals(expectedJson, outputTarget.toString());
    }

    @Test
    public void serializeAThingPropertyWithNumberDataTypeId()
    throws IOException {
        ThingDto thingDto = buildThingDtoWithANumberDataTypeId();
        String outputTarget = this.objectMapper.writeValueAsString(thingDto);
        assertNotNull(outputTarget);
        String expectedJson = buildThingDtoJsonWithNumberThingPropertyValue();
        assertEquals(expectedJson, outputTarget.toString());
    }

    @Test
    public void serializeAThingPropertyWithDateDataTypeId()
    throws IOException {
        ThingDto thingDto = buildThingDtoWithADateDataTypeId();
        String outputTarget = this.objectMapper.writeValueAsString(thingDto);
        assertNotNull(outputTarget);
        String expectedJson = buildThingDtoJsonWithDateThingPropertyValue();
        assertEquals(expectedJson, outputTarget.toString());
    }

    /**
     * Builds a thing DTO with a null thing dto property value
     *
     * @return the thing DTO
     */
    private ThingDto buildThingDtoWithANullPropertyValue() {
        ThingDto thingDto = new ThingDto();
        thingDto.id = 1L;
        thingDto.serialNumber = "TEST0001";
        thingDto.name = "TEST0001";
        thingDto.createdTime = new GregorianCalendar(2017, 1, 1).getTime();
        thingDto.modifiedTime = new GregorianCalendar(2017, 1, 1).getTime();
        thingDto.time = new GregorianCalendar(2017, 1, 1).getTime();

        MetaDto metaDto = new MetaDto();
        metaDto.bridgeCode = "ALEB";
        metaDto.sqn = 1L;
        metaDto.specName = "MYSPECNAME";
        metaDto.origin = new Double[] {Double.parseDouble("-118.43969"), Double.parseDouble("34.048092"), Double.parseDouble("0"), Double.parseDouble(
            "20")};
        metaDto.units = "ft";
        metaDto.partition = 1L;
        metaDto.numPartitions = 10L;
        metaDto.reblinked = false;
        metaDto.outOfOrder = false;
        metaDto.newBlink = true;

        thingDto.meta = metaDto;

        GroupTypeDto groupTypeDto = new GroupTypeDto();
        groupTypeDto.id = 2L;
        groupTypeDto.code = "tenant";
        groupTypeDto.name = "Tenant";

        GroupDto groupDto = new GroupDto();
        groupDto.id = 2L;
        groupDto.code = "Mns";
        groupDto.name = "Marks and Spencer";
        groupDto.groupType = groupTypeDto;

        thingDto.group = groupDto;

        ThingTypeDto thingTypeDto = new ThingTypeDto();
        thingTypeDto.id = 4L;
        thingTypeDto.code = "item";
        thingTypeDto.name = "Item";

        Map<String, ThingTypeFieldDto> fields = new HashMap<>();
        ThingTypeFieldDto soldDate = new ThingTypeFieldDto();
        soldDate.id = 50L;
        soldDate.name = "SoldDate";
        soldDate.dataTypeId = DataTypeDto.TYPE_TIMESTAMP.id;
        fields.put("SoldDate", soldDate);

        thingTypeDto.fields = fields;

        thingDto.thingType = thingTypeDto;

        List<Map<String, ThingPropertyDto>> properties = new LinkedList<>();
        Map<String, ThingPropertyDto> currentValue = new HashMap<>();
        ThingPropertyDto flag = new ThingPropertyDto();
        flag.id = 40L;
        flag.time = new GregorianCalendar(2017, 1, 1).getTime();
        flag.blinked = false;
        flag.modified = false;
        flag.value = null;
        flag.dataTypeId = 5L;
        currentValue.put("flag", flag);

        properties.add(currentValue);
        Map<String, ThingPropertyDto> previousValue = new HashMap<>();
        properties.add(previousValue);

        thingDto.properties = properties;

        return thingDto;
    }

    private ThingDto buildThingDtoWithABooleanDataTypeId() {
        ThingDto thingDto = buildThingDtoWithANullPropertyValue();
        ThingPropertyDto flag = thingDto.getUdf("flag");
        flag.value = true;
        return thingDto;
    }

    private ThingDto buildThingDtoWithANumberDataTypeId() {
        ThingDto thingDto = buildThingDtoWithANullPropertyValue();
        ThingPropertyDto flag = thingDto.getUdf("flag");
        flag.dataTypeId = 4L;
        flag.value = 1L;
        return thingDto;
    }

    private ThingDto buildThingDtoWithADateDataTypeId() {
        ThingDto thingDto = buildThingDtoWithANullPropertyValue();
        ThingPropertyDto flag = thingDto.getUdf("flag");
        flag.dataTypeId = 11L;
        flag.value = new GregorianCalendar(2017,1,1).getTime();
        return thingDto;
    }

    /**
     * Builds a thing DTO JSON with a null thing property value.
     *
     * @return the Json String
     */
    private String buildThingDtoJsonWithNullThingPropertyValue() {
        String output = "{\"meta\":{\"bridgeCode\":\"ALEB\",\"sqn\":1,\"specName\":\"MYSPECNAME\",\"origin\":[-118.43969,34.048092,0.0,20.0],\"units\":\"ft\",\"partition\":1,\"numPartitions\":10,\"reblinked\":false,\"outOfOrder\":false,\"newBlink\":true},\"id\":1,\"serialNumber\":\"TEST0001\",\"name\":\"TEST0001\",\"createdTime\":\"2017-02-01T00:00:00.000-04:00\",\"modifiedTime\":\"2017-02-01T00:00:00.000-04:00\",\"time\":\"2017-02-01T00:00:00.000-04:00\",\"group\":{\"id\":2,\"name\":\"Marks and Spencer\",\"code\":\"Mns\",\"groupType\":{\"id\":2,\"name\":\"Tenant\",\"code\":\"tenant\"}},\"thingType\":{\"id\":4,\"name\":\"Item\",\"code\":\"item\",\"fields\":{\"SoldDate\":{\"id\":50,\"name\":\"SoldDate\",\"dataTypeId\":24}}},\"properties\":[{\"flag\":{\"id\":40,\"time\":\"2017-02-01T04:00:00.000Z\",\"blinked\":false,\"modified\":false,\"timeSeries\":null,\"value\":null,\"dataTypeId\":5}},{}]}";
        return output;
    }

    private String buildThingDtoJsonWithBooleanThingPropertyValue() {
        String output = "{\"meta\":{\"bridgeCode\":\"ALEB\",\"sqn\":1,\"specName\":\"MYSPECNAME\",\"origin\":[-118.43969,34.048092,0.0,20.0],\"units\":\"ft\",\"partition\":1,\"numPartitions\":10,\"reblinked\":false,\"outOfOrder\":false,\"newBlink\":true},\"id\":1,\"serialNumber\":\"TEST0001\",\"name\":\"TEST0001\",\"createdTime\":\"2017-02-01T00:00:00.000-04:00\",\"modifiedTime\":\"2017-02-01T00:00:00.000-04:00\",\"time\":\"2017-02-01T00:00:00.000-04:00\",\"group\":{\"id\":2,\"name\":\"Marks and Spencer\",\"code\":\"Mns\",\"groupType\":{\"id\":2,\"name\":\"Tenant\",\"code\":\"tenant\"}},\"thingType\":{\"id\":4,\"name\":\"Item\",\"code\":\"item\",\"fields\":{\"SoldDate\":{\"id\":50,\"name\":\"SoldDate\",\"dataTypeId\":24}}},\"properties\":[{\"flag\":{\"id\":40,\"time\":\"2017-02-01T04:00:00.000Z\",\"blinked\":false,\"modified\":false,\"timeSeries\":null,\"value\":true,\"dataTypeId\":5}},{}]}";
        return output;
    }

    private String buildThingDtoJsonWithNumberThingPropertyValue() {
        String output = "{\"meta\":{\"bridgeCode\":\"ALEB\",\"sqn\":1,\"specName\":\"MYSPECNAME\",\"origin\":[-118.43969,34.048092,0.0,20.0],\"units\":\"ft\",\"partition\":1,\"numPartitions\":10,\"reblinked\":false,\"outOfOrder\":false,\"newBlink\":true},\"id\":1,\"serialNumber\":\"TEST0001\",\"name\":\"TEST0001\",\"createdTime\":\"2017-02-01T00:00:00.000-04:00\",\"modifiedTime\":\"2017-02-01T00:00:00.000-04:00\",\"time\":\"2017-02-01T00:00:00.000-04:00\",\"group\":{\"id\":2,\"name\":\"Marks and Spencer\",\"code\":\"Mns\",\"groupType\":{\"id\":2,\"name\":\"Tenant\",\"code\":\"tenant\"}},\"thingType\":{\"id\":4,\"name\":\"Item\",\"code\":\"item\",\"fields\":{\"SoldDate\":{\"id\":50,\"name\":\"SoldDate\",\"dataTypeId\":24}}},\"properties\":[{\"flag\":{\"id\":40,\"time\":\"2017-02-01T04:00:00.000Z\",\"blinked\":false,\"modified\":false,\"timeSeries\":null,\"value\":1,\"dataTypeId\":4}},{}]}";
        return output;
    }

    private String buildThingDtoJsonWithDateThingPropertyValue() {
        String output = "{\"meta\":{\"bridgeCode\":\"ALEB\",\"sqn\":1,\"specName\":\"MYSPECNAME\",\"origin\":[-118.43969,34.048092,0.0,20.0],\"units\":\"ft\",\"partition\":1,\"numPartitions\":10,\"reblinked\":false,\"outOfOrder\":false,\"newBlink\":true},\"id\":1,\"serialNumber\":\"TEST0001\",\"name\":\"TEST0001\",\"createdTime\":\"2017-02-01T00:00:00.000-04:00\",\"modifiedTime\":\"2017-02-01T00:00:00.000-04:00\",\"time\":\"2017-02-01T00:00:00.000-04:00\",\"group\":{\"id\":2,\"name\":\"Marks and Spencer\",\"code\":\"Mns\",\"groupType\":{\"id\":2,\"name\":\"Tenant\",\"code\":\"tenant\"}},\"thingType\":{\"id\":4,\"name\":\"Item\",\"code\":\"item\",\"fields\":{\"SoldDate\":{\"id\":50,\"name\":\"SoldDate\",\"dataTypeId\":24}}},\"properties\":[{\"flag\":{\"id\":40,\"time\":\"2017-02-01T04:00:00.000Z\",\"blinked\":false,\"modified\":false,\"timeSeries\":null,\"value\":\"2017-02-01T04:00:00.000Z\",\"dataTypeId\":11}},{}]}";
        return output;
    }
}