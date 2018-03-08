package com.tierconnect.riot.commons.dtos;

import com.tierconnect.riot.commons.serdes.JsonDeserializer;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * DataTypeDtoTest class.
 *
 * @author jantezana
 * @author achambi
 * @version 2017/11/09
 */
public class ThingDtoTest {

    private String currentTemplate = "{\"meta\":{\"bridgeCode\":\"APP2\",\"sqn\":1,\"specName\":\"SERVICES\"},\"serialNumber\":\"KAFKA0002\",\"time\":\"2017-03-14T14:29:02.603Z\",\"thingType\":{\"code\":\"default_rfid_thingtype\"},\"properties\":[{%s}]}";
    private String currentPreviousTemplate = "{\"meta\":{\"bridgeCode\":\"APP2\",\"sqn\":1,\"specName\":\"SERVICES\"},\"serialNumber\":\"KAFKA0002\",\"time\":\"2017-03-14T14:29:02.603Z\",\"thingType\":{\"code\":\"default_rfid_thingtype\"},\"properties\":[{%s},{%s}]}";
    private String udfTemplate = "\"%s\":{\"modified\":false,\"value\":\"%s\",\"time\":\"%s\",\"blinked\":%s}";

    // private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd\'T\'HH:mm:ss.SSSXXX");
    private Date time1 = new Date(1_000_000);
    private Date time2 = new Date(2_000_000);
    private Date time3 = new Date(3_000_000);

    @Test
    public void putUdf_equalValues() throws Exception{
        boolean blinked = Math.random() < 0.5;
        boolean ruleChanged = Math.random() < 0.5;
        ThingDto blink = buildCurrentBlink("zone","A",time1,blinked,ruleChanged);
        ThingPropertyDto udf = new ThingPropertyDto();
        blink.meta.newBlink = Math.random() < 0.5;

        udf.value = "A";
        udf.time = time2;

        blink.putUdf("zone",udf);
        ThingPropertyDto zone = blink.getUdf("zone");

        assertEquals("A", zone.value);
        assertEquals(time1, zone.time);
        assertFalse(zone.modified);
        assertEquals(ruleChanged, zone.ruleChanged);
    }

    @Test
    public void putUdf_difValues_ruleChanged_noPrevious() throws Exception{
        boolean blinked = Math.random() < 0.5;
        boolean ruleChanged = true;
        ThingDto blink = buildCurrentBlink("zone","A",time1,blinked,ruleChanged);
        ThingPropertyDto udf = new ThingPropertyDto();
        blink.meta.newBlink = Math.random() < 0.5;
        udf.value = "B";
        udf.time = time2;

        blink.putUdf("zone",udf);
        ThingPropertyDto zone = blink.getUdf("zone");

        assertEquals("B", zone.value);
        assertEquals(time2, zone.time);
        assertTrue(blink.properties.size()==1);
        assertTrue(zone.modified);
        assertTrue(zone.ruleChanged);
    }

    @Test
    public void putUdf_difValues_newBlink() throws Exception{
        boolean blinked = Math.random() < 0.5;
        boolean ruleChanged = false;
        ThingDto blink = buildCurrentBlink("zone","A",time1,blinked,ruleChanged);
        ThingPropertyDto udf = new ThingPropertyDto();
        blink.meta.newBlink = true;
        udf.value = "B";
        udf.time = time2;

        blink.putUdf("zone",udf);
        ThingPropertyDto zone = blink.getUdf("zone");

        assertEquals("B", zone.value);
        assertEquals(time2, zone.time);
        assertTrue(blink.properties.size()==1);
        assertTrue(zone.modified);
        assertTrue(zone.ruleChanged);
    }

    @Test
    public void putUdf_difValues_ruleChanged_withPrevious() throws Exception{
        boolean blinked = Math.random() < 0.5;
        boolean ruleChanged = true;
        ThingDto blink = buildPreviousBlink("zone","B",time2,blinked,ruleChanged,"zone","A", time1);
        blink.meta.newBlink = Math.random() < 0.5;
        ThingPropertyDto udf = new ThingPropertyDto();
        udf.value = "C";
        udf.time = time3;

        blink.putUdf("zone",udf);
        ThingPropertyDto zone1 = blink.getUdf("zone");

        assertEquals("C", zone1.value);
        assertEquals(time3, zone1.time);
        assertTrue(zone1.modified);
        assertTrue(zone1.ruleChanged);
        assertTrue(blink.properties.size()==2);

        ThingPropertyDto zone2 = blink.getPreviousUdf("zone");
        assertEquals("A", zone2.value);
        assertEquals(time1, zone2.time);
    }


    @Test
    public void putUdf_difValues_blinked_noPrevious() throws Exception{
        boolean blinked = true;
        boolean ruleChanged = false;
        ThingDto blink = buildCurrentBlink("zone","A",time1,blinked,ruleChanged);
        ThingPropertyDto udf = new ThingPropertyDto();
        blink.meta.newBlink = false;
        udf.value = "B";
        udf.time = time2;

        blink.putUdf("zone",udf);
        ThingPropertyDto zone = blink.getUdf("zone");

        assertEquals("B", zone.value);
        assertEquals(time2, zone.time);
        assertTrue(zone.modified);
        assertTrue(zone.ruleChanged);
        assertTrue(blink.properties.size()==2);

        ThingPropertyDto zone2 = blink.getPreviousUdf("zone");
        assertEquals("A", zone2.value);
        assertEquals(time1, zone2.time);
    }


    @Test
    public void putUdf_difValues_blinked_withPrevious() throws Exception{
        boolean blinked = true;
        boolean ruleChanged = false;
        ThingDto blink = buildPreviousBlink("zone","B",time2,blinked,ruleChanged,"zone","A",time1);
        ThingPropertyDto udf = new ThingPropertyDto();
        blink.meta.newBlink = false;
        udf.value = "C";
        udf.time = time3;

        blink.putUdf("zone",udf);
        ThingPropertyDto zone = blink.getUdf("zone");

        assertEquals("C", zone.value);
        assertEquals(time3, zone.time);
        assertTrue(zone.modified);
        assertTrue(zone.ruleChanged);
        assertTrue(blink.properties.size()==2);

        ThingPropertyDto zone2 = blink.getPreviousUdf("zone");
        assertEquals("B", zone2.value);
        assertEquals(time2, zone2.time);
    }

    @Test
    public void putUdf_difValues_noCurrent() throws Exception{
        boolean blinked = Math.random()<0.5;
        boolean ruleChanged = Math.random()<0.5;
        ThingDto blink = buildCurrentBlink("status","OK",time1,blinked,ruleChanged);
        ThingPropertyDto udf = new ThingPropertyDto();
        blink.meta.newBlink = Math.random()<0.5;
        udf.value = "A";
        udf.time = time2;

        blink.putUdf("zone",udf);
        ThingPropertyDto zone = blink.getUdf("zone");

        assertEquals("A", zone.value);
        assertEquals(time2, zone.time);
        assertTrue(zone.modified);
        assertTrue(zone.ruleChanged);
        assertTrue(blink.properties.size()==1);
    }


    @Test
    public void putUdf_difValues_not_blinked_withPrevious() throws Exception{
        boolean blinked = false;
        boolean ruleChanged = false;
        ThingDto blink = buildCurrentBlink("zone","A",time1,blinked,ruleChanged);
        ThingPropertyDto udf = new ThingPropertyDto();
        blink.meta.newBlink = false;
        udf.blinked = false;
        udf.value = "C";
        udf.time = time3;

        blink.putUdf("zone",udf);
        ThingPropertyDto zone = blink.getUdf("zone");

        assertEquals("C", zone.value);
        assertEquals(time3, zone.time);
        assertTrue(zone.modified);
        assertTrue(blink.properties.size()==2);

        ThingPropertyDto zone2 = blink.getPreviousUdf("zone");
        assertEquals("A", zone2.value);
    }

    @Test
    public void putUdf_ZoneValue() {
        // Creates the zone DTO.
        ZoneDto zoneDto = buildZone();

        // Creates the thing DTO.
        ThingDto thingDto = new ThingDto();
        MetaDto metaDto = new MetaDto();
        metaDto.bridgeCode = "ALEB";
        metaDto.newBlink = true;
        thingDto.meta = metaDto;

        List<Map<String, ThingPropertyDto>> properties = new LinkedList<>();
        properties.add(new HashMap<>());
        thingDto.properties = properties;

        // Create the zoneProperty
        ThingPropertyDto zoneProperty = new ThingPropertyDto();
        zoneProperty.id = 1L;
        zoneProperty.time = new GregorianCalendar(2017, 3, 21, 8, 1).getTime();
        zoneProperty.blinked = true;
        zoneProperty.ruleChanged = true;
        zoneProperty.value = zoneDto;
        thingDto.putUdf("zone", zoneProperty);

        ThingPropertyDto actualZoneProperty = thingDto.getUdf("zone");
        ZoneDto actualZoneDto = (ZoneDto) actualZoneProperty.value;
        assertNotNull(actualZoneDto.facilityMap.time);
        assertNotNull(actualZoneDto.zoneGroup.time);
        assertNotNull(actualZoneDto.zoneType.time);

        assertEquals(actualZoneProperty.time, actualZoneDto.facilityMap.time);
        assertEquals(actualZoneProperty.time, actualZoneDto.zoneGroup.time);
        assertEquals(actualZoneProperty.time, actualZoneDto.zoneType.time);

        assertTrue(actualZoneDto.facilityMap.modified);
        assertTrue(actualZoneDto.zoneGroup.modified);
        assertTrue(actualZoneDto.zoneType.modified);

        assertEquals(actualZoneProperty.blinked, actualZoneDto.facilityMap.blinked);
        assertEquals(actualZoneProperty.blinked, actualZoneDto.zoneGroup.blinked);
        assertEquals(actualZoneProperty.blinked, actualZoneDto.zoneType.blinked);
    }

    @Test
    public void putUdf_ZoneValue_Update_FacilityMap() {
        // Creates the zone DTO.
        ZoneDto zoneDto = buildZone();

        // Creates the thing DTO.
        ThingDto thingDto = new ThingDto();
        MetaDto metaDto = new MetaDto();
        metaDto.bridgeCode = "ALEB";
        metaDto.newBlink = true;
        thingDto.meta = metaDto;

        List<Map<String, ThingPropertyDto>> properties = new LinkedList<>();
        properties.add(new HashMap<>());
        thingDto.properties = properties;

        // Create the zoneProperty
        ThingPropertyDto zoneProperty = new ThingPropertyDto();
        zoneProperty.id = 1L;
        zoneProperty.time = new GregorianCalendar(2017, 3, 21, 8, 1).getTime();
        zoneProperty.blinked = true;
        zoneProperty.ruleChanged = true;
        zoneProperty.value = zoneDto;
        thingDto.putUdf("zone", zoneProperty);

        ThingPropertyDto actualZoneProperty = thingDto.getUdf("zone");
        ZoneDto actualZoneDto = (ZoneDto) actualZoneProperty.value;
        assertNotNull(actualZoneDto.facilityMap.time);
        assertNotNull(actualZoneDto.zoneGroup.time);
        assertNotNull(actualZoneDto.zoneType.time);

        assertEquals(actualZoneProperty.blinked, actualZoneDto.facilityMap.blinked);
        assertEquals(actualZoneProperty.blinked, actualZoneDto.zoneGroup.blinked);
        assertEquals(actualZoneProperty.blinked, actualZoneDto.zoneType.blinked);

        ZoneDto zoneDto2 = buildZone();
        FacilityMapDto facilityMap = new FacilityMapDto();
        facilityMap.id = 3L;
        facilityMap.name = "FMCBB";
        facilityMap.code = "FMCBB";
        zoneDto2.facilityMap = facilityMap;

        thingDto.meta.newBlink = false;
        ThingPropertyDto zoneProperty2 = new ThingPropertyDto();
        zoneProperty2.id = 1L;
        zoneProperty2.time = new GregorianCalendar(2017, 3, 21, 8, 4).getTime();
        zoneProperty2.blinked = true;
        zoneProperty2.ruleChanged = true;
        zoneProperty2.value = zoneDto2;
        thingDto.putUdf("zone", zoneProperty2);

        actualZoneProperty = thingDto.getUdf("zone");
        actualZoneDto = (ZoneDto) actualZoneProperty.value;

        assertTrue(actualZoneDto.facilityMap.modified);
        assertFalse(actualZoneDto.zoneGroup.modified);
        assertFalse(actualZoneDto.zoneType.modified);

        assertEquals(actualZoneProperty.blinked, actualZoneDto.facilityMap.blinked);
        assertEquals(actualZoneProperty.blinked, actualZoneDto.zoneGroup.blinked);
        assertEquals(actualZoneProperty.blinked, actualZoneDto.zoneType.blinked);
    }

    @Test
    public void putUdf_ZoneValue_Update_ZoneType() {
        // Creates the zone DTO.
        ZoneDto zoneDto = buildZone();

        // Creates the thing DTO.
        ThingDto thingDto = new ThingDto();
        MetaDto metaDto = new MetaDto();
        metaDto.bridgeCode = "ALEB";
        metaDto.newBlink = true;
        thingDto.meta = metaDto;

        List<Map<String, ThingPropertyDto>> properties = new LinkedList<>();
        properties.add(new HashMap<>());
        thingDto.properties = properties;

        // Create the zoneProperty
        ThingPropertyDto zoneProperty = new ThingPropertyDto();
        zoneProperty.id = 1L;
        zoneProperty.time = new GregorianCalendar(2017, 3, 21, 8, 1).getTime();
        zoneProperty.blinked = true;
        zoneProperty.ruleChanged = true;
        zoneProperty.value = zoneDto;
        thingDto.putUdf("zone", zoneProperty);

        ThingPropertyDto actualZoneProperty = thingDto.getUdf("zone");
        ZoneDto actualZoneDto = (ZoneDto) actualZoneProperty.value;
        assertNotNull(actualZoneDto.facilityMap.time);
        assertNotNull(actualZoneDto.zoneGroup.time);
        assertNotNull(actualZoneDto.zoneType.time);

        assertEquals(actualZoneProperty.blinked, actualZoneDto.facilityMap.blinked);
        assertEquals(actualZoneProperty.blinked, actualZoneDto.zoneGroup.blinked);
        assertEquals(actualZoneProperty.blinked, actualZoneDto.zoneType.blinked);

        ZoneDto zoneDto2 = buildZone();
        ZonePropertyDto zoneType = new ZonePropertyDto();
        zoneType.id = 6L;
        zoneType.name = "ZoneType0002";
        zoneType.code = "ZoneType0002";
        zoneDto2.zoneType = zoneType;

        thingDto.meta.newBlink = false;
        ThingPropertyDto zoneProperty2 = new ThingPropertyDto();
        zoneProperty2.id = 1L;
        zoneProperty.time = new GregorianCalendar(2017, 3, 21, 8, 5).getTime();
        zoneProperty2.blinked = true;
        zoneProperty2.ruleChanged = true;
        zoneProperty2.value = zoneDto2;
        thingDto.putUdf("zone", zoneProperty2);

        actualZoneProperty = thingDto.getUdf("zone");
        actualZoneDto = (ZoneDto) actualZoneProperty.value;

        assertFalse(actualZoneDto.facilityMap.modified);
        assertFalse(actualZoneDto.zoneGroup.modified);
        assertTrue(actualZoneDto.zoneType.modified);

        assertEquals(actualZoneProperty.blinked, actualZoneDto.facilityMap.blinked);
        assertEquals(actualZoneProperty.blinked, actualZoneDto.zoneGroup.blinked);
        assertEquals(actualZoneProperty.blinked, actualZoneDto.zoneType.blinked);
    }

    /**
     * Case 0001:
     * UDF   |   CURRENT UDF  | NEW UDF
     * Z1,G1 |   -------------| Z1,G1 modified (true, true) time (udfTime, udfTime)
     */
    @Test
    public void putUdf_ZoneValue_Update_ZoneGroup_Case0001() {
        // Creates the zone DTO.
        ZoneDto zoneDto = buildZone();

        // Creates the thing DTO.
        ThingDto thingDto = new ThingDto();
        MetaDto metaDto = new MetaDto();
        metaDto.bridgeCode = "ALEB";
        metaDto.newBlink = true;
        thingDto.meta = metaDto;

        List<Map<String, ThingPropertyDto>> properties = new LinkedList<>();
        properties.add(new HashMap<>());
        thingDto.properties = properties;

        // Create the zoneProperty
        ThingPropertyDto zoneProperty = new ThingPropertyDto();
        zoneProperty.id = 1L;
        zoneProperty.time = new GregorianCalendar(2017, 3, 21, 8, 1).getTime();
        zoneProperty.blinked = true;
        zoneProperty.ruleChanged = true;
        zoneProperty.value = zoneDto;
        thingDto.putUdf("zone", zoneProperty);

        ThingPropertyDto actualZoneProperty = thingDto.getUdf("zone");
        ZoneDto actualZoneDto = (ZoneDto) actualZoneProperty.value;
        assertTrue(actualZoneProperty.modified);
        assertTrue(actualZoneDto.zoneGroup.modified);
        assertEquals(actualZoneProperty.time, actualZoneDto.zoneGroup.time);
        assertEquals(actualZoneProperty.blinked, actualZoneDto.zoneGroup.blinked);
    }


    /**
     * Case 0002:
     * UDF   |   CURRENT UDF                 | NEW UDF
     * Z1,G2 |   Z1,G1                       | ----------
     *         (update current udf)
     *           Z1,G2
     *           modified (true, true)
     *           time (currentTime, udfTime)
     */
    @Test
    public void putUdf_ZoneValue_Update_ZoneGroup_Case0002() {
        // Creates the zone DTO.
        ZoneDto zoneDto = buildZone();

        // Creates the thing DTO.
        ThingDto thingDto = new ThingDto();
        MetaDto metaDto = new MetaDto();
        metaDto.bridgeCode = "ALEB";
        metaDto.newBlink = true;
        thingDto.meta = metaDto;

        List<Map<String, ThingPropertyDto>> properties = new LinkedList<>();
        properties.add(new HashMap<>());
        thingDto.properties = properties;

        // Create the zoneProperty
        ThingPropertyDto zoneProperty = new ThingPropertyDto();
        zoneProperty.id = 1L;
        zoneProperty.time = new GregorianCalendar(2017, 3, 21, 8, 1).getTime();
        zoneProperty.blinked = true;
        zoneProperty.ruleChanged = true;
        zoneProperty.value = zoneDto;
        thingDto.putUdf("zone", zoneProperty);

        ThingPropertyDto actualZoneProperty = thingDto.getUdf("zone");
        ZoneDto actualZoneDto = (ZoneDto) actualZoneProperty.value;
        assertNotNull(actualZoneDto.zoneGroup.time);
        assertEquals(actualZoneProperty.blinked, actualZoneDto.zoneGroup.blinked);

        ZoneDto zoneDto2 = buildZone();
        ZonePropertyDto zoneGroup = new ZonePropertyDto();
        zoneGroup.id = 6L;
        zoneGroup.name = "ZoneType0002";
        zoneGroup.code = "ZoneType0002";
        zoneDto2.zoneGroup = zoneGroup;

        thingDto.meta.newBlink = false;
        ThingPropertyDto zoneProperty2 = new ThingPropertyDto();
        zoneProperty2.id = 1L;
        zoneProperty2.time = new GregorianCalendar(2017, 3, 21, 8, 3).getTime();
        zoneProperty2.blinked = true;
        zoneProperty2.ruleChanged = true;
        zoneProperty2.value = zoneDto2;
        thingDto.putUdf("zone", zoneProperty2);

        actualZoneProperty = thingDto.getUdf("zone");
        actualZoneDto = (ZoneDto) actualZoneProperty.value;

        assertTrue(actualZoneProperty.modified);
        assertTrue(actualZoneDto.zoneGroup.modified);

        assertEquals(actualZoneProperty.blinked, actualZoneDto.zoneGroup.blinked);
    }

    /**
     * Case 0003:
     * UDF   |   CURRENT UDF                 | NEW UDF
     * Z1,G1 |   Z2,G1                       | Z1, G1
     *                                         modified (true, false)
     *                                         time (udfTime, currentTime)
     */
    @Test
    public void putUdf_ZoneValue_Update_ZoneGroup_Case0003() {
        // Creates the zone DTO.
        ZoneDto zoneDto = buildZone();

        // Creates the thing DTO.
        ThingDto thingDto = new ThingDto();
        MetaDto metaDto = new MetaDto();
        metaDto.bridgeCode = "ALEB";
        metaDto.newBlink = true;
        thingDto.meta = metaDto;

        List<Map<String, ThingPropertyDto>> properties = new LinkedList<>();
        properties.add(new HashMap<>());
        thingDto.properties = properties;

        // Create the zoneProperty
        ThingPropertyDto zoneProperty = new ThingPropertyDto();
        zoneProperty.id = 1L;
        // int year, int month, int dayOfMonth, int hourOfDay, int minute
        zoneProperty.time = new GregorianCalendar(2017, 3, 21, 8, 1).getTime();
        zoneProperty.blinked = true;
        zoneProperty.ruleChanged = true;
        zoneProperty.value = zoneDto;
        thingDto.putUdf("zone", zoneProperty);

        ThingPropertyDto actualZoneProperty = thingDto.getUdf("zone");
        ZoneDto actualZoneDto = (ZoneDto) actualZoneProperty.value;
        assertNotNull(actualZoneDto.zoneGroup.time);
        assertEquals(actualZoneProperty.blinked, actualZoneDto.zoneGroup.blinked);

        ZoneDto zoneDto2 = buildZone2();
        thingDto.meta.newBlink = false;
        ThingPropertyDto zoneProperty2 = new ThingPropertyDto();
        zoneProperty2.id = 1L;
        zoneProperty2.time = new GregorianCalendar(2017, 3, 21, 8, 2).getTime();
        zoneProperty2.blinked = true;
        zoneProperty2.ruleChanged = true;
        zoneProperty2.value = zoneDto2;
        thingDto.putUdf("zone", zoneProperty2);

        actualZoneProperty = thingDto.getUdf("zone");
        actualZoneDto = (ZoneDto) actualZoneProperty.value;

        assertTrue(actualZoneProperty.modified);
        assertFalse(actualZoneDto.zoneGroup.modified);
        assertNotEquals(actualZoneProperty.time, actualZoneDto.zoneGroup.time);
        assertEquals(actualZoneProperty.blinked, actualZoneDto.zoneGroup.blinked);
    }

    /**
     * Case 0004:
     * UDF   |   CURRENT UDF                 | NEW UDF
     * Z1,G1 |   Z1,G1                       |
     *           modified (false, false)
     *           time (currentTime, currentTime)
     */
    @Test
    public void putUdf_ZoneValue_Update_ZoneGroup_Case0004() {
        // Creates the zone DTO.
        ZoneDto zoneDto = buildZone();

        // Creates the thing DTO.
        ThingDto thingDto = new ThingDto();
        MetaDto metaDto = new MetaDto();
        metaDto.bridgeCode = "ALEB";
        metaDto.newBlink = true;
        thingDto.meta = metaDto;

        List<Map<String, ThingPropertyDto>> properties = new LinkedList<>();
        properties.add(new HashMap<>());
        thingDto.properties = properties;

        // Create the zoneProperty
        ThingPropertyDto zoneProperty = new ThingPropertyDto();
        zoneProperty.id = 1L;
        zoneProperty.time = new GregorianCalendar(2017, 3, 21, 8, 1).getTime();
        zoneProperty.blinked = true;
        zoneProperty.ruleChanged = true;
        zoneProperty.value = zoneDto;
        thingDto.putUdf("zone", zoneProperty);

        ThingPropertyDto actualZoneProperty = thingDto.getUdf("zone");
        ZoneDto actualZoneDto = (ZoneDto) actualZoneProperty.value;
        assertNotNull(actualZoneDto.zoneGroup.time);
        assertEquals(actualZoneProperty.blinked, actualZoneDto.zoneGroup.blinked);

        ZoneDto zoneDto2 = buildZone2();
        ZonePropertyDto zoneGroup = new ZonePropertyDto();
        zoneGroup.id = 5L;
        zoneGroup.name = "ZoneGroup0002";
        zoneGroup.code = "ZoneGroup0002";
        zoneDto2.zoneGroup = zoneGroup;

        thingDto.meta.newBlink = false;
        ThingPropertyDto zoneProperty2 = new ThingPropertyDto();
        zoneProperty2.id = 1L;
        zoneProperty2.time = new GregorianCalendar(2017, 3, 21, 8, 4).getTime();
        zoneProperty2.blinked = true;
        zoneProperty2.ruleChanged = true;
        zoneProperty2.value = zoneDto2;
        thingDto.putUdf("zone", zoneProperty2);

        actualZoneProperty = thingDto.getUdf("zone");
        actualZoneDto = (ZoneDto) actualZoneProperty.value;

        assertTrue(actualZoneProperty.modified);
        assertTrue(actualZoneDto.zoneGroup.modified);
        assertEquals(actualZoneProperty.time, actualZoneDto.zoneGroup.time);
        assertEquals(actualZoneProperty.blinked, actualZoneDto.zoneGroup.blinked);
    }

    /**
     * Case 0005:
     * UDF   |   CURRENT UDF                 | NEW UDF
     * Z2,G2 |   Z1,G1                       |  Z2,G2
     *                                          modified (true, true)
     *                                          time (udfTime, udfTime)
     */
    @Test
    public void putUdf_ZoneValue_Update_ZoneGroup_Case0005() {
        // Creates the zone DTO.
        ZoneDto zoneDto = buildZone();

        // Creates the thing DTO.
        ThingDto thingDto = new ThingDto();
        MetaDto metaDto = new MetaDto();
        metaDto.bridgeCode = "ALEB";
        metaDto.newBlink = true;
        thingDto.meta = metaDto;

        List<Map<String, ThingPropertyDto>> properties = new LinkedList<>();
        properties.add(new HashMap<>());
        thingDto.properties = properties;

        // Create the zoneProperty
        ThingPropertyDto zoneProperty = new ThingPropertyDto();
        zoneProperty.id = 1L;
        zoneProperty.time = new GregorianCalendar(2017, 3, 21, 8, 1).getTime();
        zoneProperty.blinked = true;
        zoneProperty.ruleChanged = true;
        zoneProperty.value = zoneDto;
        thingDto.putUdf("zone", zoneProperty);

        ThingPropertyDto actualZoneProperty = thingDto.getUdf("zone");
        ZoneDto actualZoneDto = (ZoneDto) actualZoneProperty.value;
        assertNotNull(actualZoneDto.zoneGroup.time);
        assertEquals(actualZoneProperty.blinked, actualZoneDto.zoneGroup.blinked);

        ZoneDto zoneDto2 = buildZone2WithGroup2();
        thingDto.meta.newBlink = false;
        ThingPropertyDto zoneProperty2 = new ThingPropertyDto();
        zoneProperty2.id = 1L;
        zoneProperty2.time = new GregorianCalendar(2017, 3, 21, 8, 5).getTime();
        zoneProperty2.blinked = true;
        zoneProperty2.ruleChanged = true;
        zoneProperty2.value = zoneDto2;
        thingDto.putUdf("zone", zoneProperty2);

        actualZoneProperty = thingDto.getUdf("zone");
        actualZoneDto = (ZoneDto) actualZoneProperty.value;

        assertTrue(actualZoneProperty.modified);
        assertTrue(actualZoneDto.zoneGroup.modified);
        assertEquals(actualZoneProperty.time, actualZoneDto.zoneGroup.time);
        assertEquals(actualZoneProperty.blinked, actualZoneDto.zoneGroup.blinked);
    }

    private synchronized ZoneDto buildZone() {
        // Creates the zone DTO.
        ZoneDto zoneDto = new ZoneDto();
        zoneDto.id = 1L;
        zoneDto.name = "LPZ";
        zoneDto.code = "LPZ";

        FacilityMapDto facilityMap = new FacilityMapDto();
        facilityMap.id = 2L;
        facilityMap.name = "FMLPZ";
        facilityMap.code = "FMLPZ";
        zoneDto.facilityMap = facilityMap;

        ZonePropertyDto zoneType = new ZonePropertyDto();
        zoneType.id = 3L;
        zoneType.name = "ZoneType0001";
        zoneType.code = "ZoneType0001";
        zoneDto.zoneType = zoneType;

        ZonePropertyDto zoneGroup = new ZonePropertyDto();
        zoneGroup.id = 4L;
        zoneGroup.name = "ZoneGroup0001";
        zoneGroup.code = "ZoneGroup0001";
        zoneDto.zoneGroup = zoneGroup;

        return zoneDto;
    }

    private synchronized ZoneDto buildZone2() {
        // Creates the zone DTO.
        ZoneDto zoneDto = new ZoneDto();
        zoneDto.id = 2L;
        zoneDto.name = "SCZ";
        zoneDto.code = "SCZ";

        FacilityMapDto facilityMap = new FacilityMapDto();
        facilityMap.id = 3L;
        facilityMap.name = "FMSCZ";
        facilityMap.code = "FMSCZ";
        zoneDto.facilityMap = facilityMap;

        ZonePropertyDto zoneType = new ZonePropertyDto();
        zoneType.id = 4L;
        zoneType.name = "ZoneType0002";
        zoneType.code = "ZoneType0002";
        zoneDto.zoneType = zoneType;

        ZonePropertyDto zoneGroup = new ZonePropertyDto();
        zoneGroup.id = 4L;
        zoneGroup.name = "ZoneGroup0001";
        zoneGroup.code = "ZoneGroup0001";
        zoneDto.zoneGroup = zoneGroup;

        return zoneDto;
    }

    private synchronized ZoneDto buildZone2WithGroup2() {
        // Creates the zone DTO.
        ZoneDto zoneDto = new ZoneDto();
        zoneDto.id = 2L;
        zoneDto.name = "SCZ";
        zoneDto.code = "SCZ";

        FacilityMapDto facilityMap = new FacilityMapDto();
        facilityMap.id = 3L;
        facilityMap.name = "FMSCZ";
        facilityMap.code = "FMSCZ";
        zoneDto.facilityMap = facilityMap;

        ZonePropertyDto zoneType = new ZonePropertyDto();
        zoneType.id = 4L;
        zoneType.name = "ZoneType0002";
        zoneType.code = "ZoneType0002";
        zoneDto.zoneType = zoneType;

        ZonePropertyDto zoneGroup = new ZonePropertyDto();
        zoneGroup.id = 5L;
        zoneGroup.name = "ZoneGroup0002";
        zoneGroup.code = "ZoneGroup0002";
        zoneDto.zoneGroup = zoneGroup;

        return zoneDto;
    }

    @Test
    public void parse()
    throws Exception {
        String jsonString = "{\"meta\":{\"bridgeCode\":\"ALEBDerby0\",\"sqn\":10514826,\"specName\":\"RTLS\","
            + "\"origin\":[\"-1.472006\",\"52.918259\",\"135.0\",\"0.0\"],\"units\":\"meters\",\"partition\":1,"
            + "\"numPartitions\":5,\"reblinked\":false,\"outOfOrder\":false,\"newBlink\":false},\"id\":7520,"
            + "\"serialNumber\":\"3039ECBC02D674012A06D34D\",\"name\":\"3039ECBC02D674012A06D34D\","
            + "\"createdTime\":\"1969-12-31T20:00:00.000-04:00\","
            + "\"modifiedTime\":\"1969-12-31T20:00:00.000-04:00\",\"time\":\"2016-11-10T10:10:50.970-04:00\","
            + "\"group\":{\"id\":2,\"name\":\"Marks and Spencer\",\"code\":\"MnS\",\"groupType\":{\"id\":2,"
            + "\"name\":\"Tenant\",\"code\":\"tenant\"}},\"thingType\":{\"id\":4,\"name\":\"Item\","
            + "\"code\":\"item\"},\"properties\":[{\"ADJ\":{\"id\":35,\"blinked\":false,\"modified\":false,"
            + "\"dataTypeId\":5,\"time\":\"2016-11-10T10:10:50.970-04:00\",\"value\":\"false\"},"
            + "\"DressingRoom\":{\"id\":27,\"blinked\":false,\"modified\":false,\"dataTypeId\":5,"
            + "\"time\":\"2016-11-10T10:10:50.970-04:00\",\"value\":\"false\"},\"EDW\":{\"id\":53,"
            + "\"blinked\":false,\"modified\":false,\"dataTypeId\":5,\"time\":\"2016-11-10T10:10:50.970-04:00\","
            + "\"value\":\"false\"},\"Excluded\":{\"id\":28,\"blinked\":false,\"modified\":false,\"dataTypeId\":5,"
            + "\"time\":\"2016-11-10T10:10:50.970-04:00\",\"value\":\"false\"},\"FindIt\":{\"id\":841,"
            + "\"blinked\":false,\"modified\":false,\"dataTypeId\":5,\"time\":\"2016-11-10T10:10:50.970-04:00\","
            + "\"value\":\"false\"},\"FirstDetected\":{\"id\":895,\"blinked\":false,\"modified\":false,"
            + "\"dataTypeId\":5,\"time\":\"2016-11-10T10:10:50.970-04:00\",\"value\":\"false\"},"
            + "\"FlowCounter\":{\"id\":251,\"blinked\":false,\"modified\":false,\"dataTypeId\":4,"
            + "\"time\":\"2016-11-10T10:10:50.970-04:00\",\"value\":\"0.0\"},\"GMD\":{\"id\":51,\"blinked\":false,"
            + "\"modified\":false,\"dataTypeId\":5,\"time\":\"2016-11-10T10:10:50.970-04:00\",\"value\":\"false\"},"
            + "\"POE\":{\"id\":60,\"blinked\":false,\"modified\":false,\"dataTypeId\":5,"
            + "\"time\":\"2016-11-10T10:10:50.970-04:00\",\"value\":\"false\"},\"POS\":{\"id\":64,"
            + "\"blinked\":false,\"modified\":false,\"dataTypeId\":5,\"time\":\"2016-11-10T10:10:50.970-04:00\","
            + "\"value\":\"false\"},\"Questionable\":{\"id\":910,\"blinked\":false,\"modified\":false,"
            + "\"dataTypeId\":1,\"time\":\"2016-11-10T10:10:50.970-04:00\",\"value\":\"N\"},"
            + "\"Reassociation\":{\"id\":42,\"blinked\":false,\"modified\":false,\"dataTypeId\":5,"
            + "\"time\":\"2016-11-10T10:10:50.970-04:00\",\"value\":\"false\"},\"Received\":{\"id\":248,"
            + "\"blinked\":false,\"modified\":false,\"dataTypeId\":5,\"time\":\"2016-11-10T10:10:50.970-04:00\","
            + "\"value\":\"false\"},\"Returned\":{\"id\":44,\"blinked\":false,\"modified\":false,\"dataTypeId\":5,"
            + "\"time\":\"2016-11-10T10:10:50.970-04:00\",\"value\":\"false\"},\"SYW\":{\"id\":59,"
            + "\"blinked\":false,\"modified\":false,\"dataTypeId\":5,\"time\":\"2016-11-10T10:10:50.970-04:00\","
            + "\"value\":\"false\"},\"SYWDonington\":{\"id\":909,\"blinked\":false,\"modified\":false,"
            + "\"dataTypeId\":5,\"time\":\"2016-11-10T10:10:50.970-04:00\",\"value\":\"false\"},"
            + "\"Status\":{\"id\":280,\"blinked\":false,\"modified\":false,\"dataTypeId\":1,"
            + "\"time\":\"2016-11-10T10:10:50.970-04:00\",\"value\":\"Free\"},\"Stolen\":{\"id\":898,"
            + "\"blinked\":false,\"modified\":false,\"dataTypeId\":5,\"time\":\"2016-11-10T10:10:50.970-04:00\","
            + "\"value\":\"false\"},\"TagBlinkCounter\":{\"id\":23,\"blinked\":true,\"modified\":true,"
            + "\"dataTypeId\":4,\"time\":\"2016-11-10T10:10:50.970-04:00\",\"value\":\"1\"},"
            + "\"ZoneChangeCounter\":{\"id\":67,\"blinked\":false,\"modified\":false,\"dataTypeId\":4,"
            + "\"time\":\"2016-11-10T10:10:50.970-04:00\",\"value\":\"0.0\"},\"exp\":{\"id\":62,\"blinked\":false,"
            + "\"modified\":false,\"dataTypeId\":26,\"time\":\"2016-11-10T10:10:50.970-04:00\","
            + "\"value\":\"07438882\"},\"flag\":{\"id\":40,\"blinked\":false,\"modified\":false,\"dataTypeId\":5,"
            + "\"time\":\"2016-11-10T10:10:50.970-04:00\",\"value\":\"false\"},\"lastDetectTime\":{\"id\":41,"
            + "\"blinked\":true,\"modified\":true,\"dataTypeId\":24,\"time\":\"2016-11-10T10:10:50.970-04:00\","
            + "\"value\":\"2016-11-10T10:10:50.970-04:00\"},\"prevZone\":{\"id\":32,\"blinked\":false,"
            + "\"modified\":false,\"dataTypeId\":1,\"time\":\"2016-11-10T10:10:50.970-04:00\","
            + "\"value\":\"Unknown\"},\"source\":{\"id\":34,\"blinked\":true,\"modified\":true,\"dataTypeId\":1,"
            + "\"time\":\"2016-11-10T10:10:50.970-04:00\",\"value\":\"ALEBDerby0\"},\"sourceX\":{\"id\":24,"
            + "\"blinked\":false,\"modified\":false,\"dataTypeId\":1,\"time\":\"2016-11-10T10:10:50.970-04:00\","
            + "\"value\":\"na\"},\"tsCoreIn\":{\"id\":851,\"blinked\":true,\"modified\":true,\"dataTypeId\":24,"
            + "\"time\":\"2016-11-10T10:10:50.970-04:00\",\"value\":\"2016-11-10T10:12:09.888-04:00\"},"
            + "\"tsEdgeIn\":{\"id\":852,\"blinked\":true,\"modified\":true,\"dataTypeId\":24,"
            + "\"time\":\"2016-11-10T10:10:50.970-04:00\",\"value\":\"2016-11-10T10:10:51.462-04:00\"}},"
            + "{\"ADJ\":{\"id\":35,\"dataTypeId\":5,\"time\":\"2016-11-10T10:10:50.970-04:00\","
            + "\"value\":\"false\"},\"DressingRoom\":{\"id\":27,\"dataTypeId\":5,"
            + "\"time\":\"2016-11-10T10:10:50.970-04:00\",\"value\":\"false\"},\"EDW\":{\"id\":53,\"dataTypeId\":5,"
            + "\"time\":\"2016-11-10T10:10:50.970-04:00\",\"value\":\"false\"},\"Excluded\":{\"id\":28,"
            + "\"dataTypeId\":5,\"time\":\"2016-11-10T10:10:50.970-04:00\",\"value\":\"false\"},"
            + "\"FindIt\":{\"id\":841,\"dataTypeId\":5,\"time\":\"2016-11-10T10:10:50.970-04:00\","
            + "\"value\":\"false\"},\"FirstDetected\":{\"id\":895,\"dataTypeId\":5,"
            + "\"time\":\"2016-11-10T10:10:50.970-04:00\",\"value\":\"false\"},\"FlowCounter\":{\"id\":251,"
            + "\"dataTypeId\":4,\"time\":\"2016-11-10T10:10:50.970-04:00\",\"value\":\"0.0\"},\"GMD\":{\"id\":51,"
            + "\"dataTypeId\":5,\"time\":\"2016-11-10T10:10:50.970-04:00\",\"value\":\"false\"},\"POE\":{\"id\":60,"
            + "\"dataTypeId\":5,\"time\":\"2016-11-10T10:10:50.970-04:00\",\"value\":\"false\"},\"POS\":{\"id\":64,"
            + "\"dataTypeId\":5,\"time\":\"2016-11-10T10:10:50.970-04:00\",\"value\":\"false\"},"
            + "\"Questionable\":{\"id\":910,\"dataTypeId\":1,\"time\":\"2016-11-10T10:10:50.970-04:00\","
            + "\"value\":\"N\"},\"Reassociation\":{\"id\":42,\"dataTypeId\":5,"
            + "\"time\":\"2016-11-10T10:10:50.970-04:00\",\"value\":\"false\"},\"Received\":{\"id\":248,"
            + "\"dataTypeId\":5,\"time\":\"2016-11-10T10:10:50.970-04:00\",\"value\":\"false\"},"
            + "\"Returned\":{\"id\":44,\"dataTypeId\":5,\"time\":\"2016-11-10T10:10:50.970-04:00\","
            + "\"value\":\"false\"},\"SYW\":{\"id\":59,\"dataTypeId\":5,\"time\":\"2016-11-10T10:10:50.970-04:00\","
            + "\"value\":\"false\"},\"SYWDonington\":{\"id\":909,\"dataTypeId\":5,"
            + "\"time\":\"2016-11-10T10:10:50.970-04:00\",\"value\":\"false\"},\"Status\":{\"id\":280,"
            + "\"dataTypeId\":1,\"time\":\"2016-11-10T10:10:50.970-04:00\",\"value\":\"Free\"},"
            + "\"Stolen\":{\"id\":898,\"dataTypeId\":5,\"time\":\"2016-11-10T10:10:50.970-04:00\","
            + "\"value\":\"false\"},\"TagBlinkCounter\":{\"id\":23,\"dataTypeId\":4,"
            + "\"time\":\"2016-11-10T10:10:50.970-04:00\",\"value\":\"0.0\"},\"ZoneChangeCounter\":{\"id\":67,"
            + "\"dataTypeId\":4,\"time\":\"2016-11-10T10:10:50.970-04:00\",\"value\":\"0.0\"},\"exp\":{\"id\":62,"
            + "\"dataTypeId\":26,\"time\":\"2016-11-10T10:10:50.970-04:00\",\"value\":\"07438882\"},"
            + "\"flag\":{\"id\":40,\"dataTypeId\":5,\"time\":\"2016-11-10T10:10:50.970-04:00\","
            + "\"value\":\"false\"},\"prevZone\":{\"id\":32,\"dataTypeId\":1,"
            + "\"time\":\"2016-11-10T10:10:50.970-04:00\",\"value\":\"Unknown\"},\"source\":{\"id\":34,"
            + "\"dataTypeId\":1,\"time\":\"2016-11-10T10:10:50.970-04:00\",\"value\":\"Service\"},"
            + "\"sourceX\":{\"id\":24,\"dataTypeId\":1,\"time\":\"2016-11-10T10:10:50.970-04:00\"," + "\"value\":\"na\"}}]}";
        ThingDto thingWrapper = ThingDto.parse(jsonString);
        assertNotEquals(null, thingWrapper);
    }

    private ThingDto buildCurrentBlink( String udfName, String udfValue, Date time, boolean blinked, boolean ruleChanged ){
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd\'T\'HH:mm:ss.SSSXXX");
        JsonDeserializer<ThingDto> deserializer = new JsonDeserializer<>(ThingDto.class);
        String udf = String.format(udfTemplate,udfName,udfValue,df.format(time),blinked);
        String blink = String.format(currentTemplate,udf);
        ThingDto thing = deserializer.deserialize("", blink.getBytes());
        thing.getUdf(udfName).ruleChanged = ruleChanged;
        deserializer.close();
        return thing;
    }

    private ThingDto buildPreviousBlink( String udfName, String udfValue, Date time, boolean blinked, boolean ruleChanged, String udfName2, String udfValue2, Date time2 ){
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd\'T\'HH:mm:ss.SSSXXX");
        JsonDeserializer<ThingDto> deserializer = new JsonDeserializer<>(ThingDto.class);
        String currentUdf = String.format(udfTemplate,udfName,udfValue,df.format(time),blinked);
        String previousUdf = String.format(udfTemplate,udfName2,udfValue2,df.format(time2),blinked);
        String blink = String.format(currentPreviousTemplate,currentUdf,previousUdf);
        ThingDto thing = deserializer.deserialize("", blink.getBytes());
        thing.getUdf(udfName).ruleChanged = ruleChanged;
        deserializer.close();
        return thing;
    }

}