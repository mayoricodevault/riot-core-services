package com.tierconnect.riot.commons.serdes;

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

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;

/**
 * Created by jantezana on 10-03-17.
 */
public class ThingDtoSerializerTest {

    private ThingDtoSerializer thingDtoSerializer;

    @Before
    public void setUp()
    throws Exception {
        this.thingDtoSerializer = new ThingDtoSerializer();
    }

    @Test
    public void serialize()
    throws Exception {
        ThingDto thingDto = buildThingDtoWithADatePropertyValue();
        byte[] thingDtoBytes = thingDtoSerializer.serialize("", thingDto);
        String thingDtoString = new String(thingDtoBytes);
        assertNotNull(thingDtoString);
    }

    /**
     * Builds a thing DTO with a null thing dto property value
     *
     * @return the thing DTO
     */
    private ThingDto buildThingDtoWithADatePropertyValue() {
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
        flag.value = new Date();
        flag.dataTypeId = 11L;
        currentValue.put("flag", flag);

        properties.add(currentValue);
        Map<String, ThingPropertyDto> previousValue = new HashMap<>();
        properties.add(previousValue);

        thingDto.properties = properties;

        return thingDto;
    }

}