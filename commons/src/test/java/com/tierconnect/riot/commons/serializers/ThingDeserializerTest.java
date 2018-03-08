package com.tierconnect.riot.commons.serializers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tierconnect.riot.commons.dtos.GroupDto;
import com.tierconnect.riot.commons.dtos.GroupTypeDto;
import com.tierconnect.riot.commons.dtos.LogicalReaderDto;
import com.tierconnect.riot.commons.dtos.MetaDto;
import com.tierconnect.riot.commons.dtos.ShiftDto;
import com.tierconnect.riot.commons.dtos.ThingDto;
import com.tierconnect.riot.commons.dtos.ThingPropertyDto;
import com.tierconnect.riot.commons.dtos.ThingTypeDto;
import com.tierconnect.riot.commons.dtos.ZoneDto;
import com.tierconnect.riot.commons.serdes.JsonDeserializer;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * ThingDeserializerTest class.
 *
 * @author jantezana
 * @version 2017/01/26
 */
public class ThingDeserializerTest {

    private ObjectMapper mapper;
    private String[] validJsons = {
            "{\"meta\":{\"bridgeCode\":\"ALEB\",\"sqn\":22,\"specName\":\"MYSPECNAME\",\"origin\":[-118.44397,34.04809,0.0,20.0],\"units\":\"ft\"},\"id\":1231,\"serialNumber\":\"000000000000000000030\",\"name\":\"000000000000000000030\",\"createdTime\":\"2016-12-19T10:25:42.749-04:00\",\"modifiedTime\":\"2016-12-19T12:31:15.211-04:00\",\"time\":\"2016-12-19T12:24:01.474-04:00\",\"group\":{\"id\":2,\"name\":\"Marks and Spencer\",\"code\":\"MnS\",\"groupType\":{\"id\":2,\"name\":\"Tenant\",\"code\":\"tenant\"}},\"thingType\":{\"id\":4,\"name\":\"Item\",\"code\":\"item\"},\"properties\":[{\"STRCode\":{\"id\":277,\"time\":\"2016-12-19T10:25:40.663-04:00\",\"value\":\"\",\"dataTypeId\":1},\"flag\":{\"id\":40,\"time\":\"2016-12-19T10:25:40.663-04:00\",\"value\":false,\"dataTypeId\":5},\"tsEdgeIn\":{\"id\":852,\"time\":\"2016-12-19T12:24:01.474-04:00\",\"value\":1482164642575,\"dataTypeId\":24},\"ADJ\":{\"id\":35,\"time\":\"2016-12-19T10:25:40.663-04:00\",\"value\":false,\"dataTypeId\":5},\"FindIt\":{\"id\":841,\"time\":\"2016-12-19T10:25:40.663-04:00\",\"value\":false,\"dataTypeId\":5},\"source\":{\"id\":34,\"time\":\"2016-12-19T12:24:01.474-04:00\",\"value\":\"ALEBDerby0\",\"dataTypeId\":1},\"locationXYZ\":{\"id\":61,\"time\":\"2016-12-19T12:24:01.474-04:00\",\"value\":\"52.89;20.42;0.0\",\"dataTypeId\":3},\"FirstDetectedDate\":{\"id\":896,\"time\":\"2016-12-19T10:25:40.663-04:00\",\"value\":1482157540663,\"dataTypeId\":24},\"zone\":{\"id\":66,\"time\":\"2016-12-19T12:24:01.474-04:00\",\"value\":{\"id\":0,\"name\":\"Unknown\",\"code\":\"unknown\",\"facilityMap\":{\"code\":\"unknown\",\"time\":\"2016-12-19T12:24:01.474-04:00\"},\"zoneType\":{\"code\":\"unknown\",\"time\":\"2016-12-19T12:24:01.474-04:00\"},\"zoneGroup\":{\"code\":\"unknown\",\"time\":\"2016-12-19T12:24:01.474-04:00\"}},\"dataTypeId\":9},\"tsCoreIn\":{\"id\":851,\"time\":\"2016-12-19T12:24:01.474-04:00\",\"value\":1482165074291,\"dataTypeId\":24},\"EDW\":{\"id\":53,\"time\":\"2016-12-19T10:25:40.663-04:00\",\"value\":false,\"dataTypeId\":5},\"Reassociation\":{\"id\":42,\"time\":\"2016-12-19T10:25:40.663-04:00\",\"value\":false,\"dataTypeId\":5},\"ZoneChangeCounter\":{\"id\":67,\"time\":\"2016-12-19T10:25:40.663-04:00\",\"value\":1.0,\"dataTypeId\":4},\"exp\":{\"id\":62,\"time\":\"2016-12-19T10:25:40.663-04:00\",\"value\":\"00000000\",\"dataTypeId\":26},\"GMD\":{\"id\":51,\"time\":\"2016-12-19T10:25:40.663-04:00\",\"value\":false,\"dataTypeId\":5},\"Excluded\":{\"id\":28,\"time\":\"2016-12-19T10:25:40.663-04:00\",\"value\":false,\"dataTypeId\":5},\"Status\":{\"id\":280,\"time\":\"2016-12-19T10:25:40.663-04:00\",\"value\":\"Excluded\",\"dataTypeId\":1},\"sourceX\":{\"id\":24,\"time\":\"2016-12-19T10:25:40.663-04:00\",\"value\":\"na\",\"dataTypeId\":1},\"Returned\":{\"id\":44,\"time\":\"2016-12-19T10:25:40.663-04:00\",\"value\":false,\"dataTypeId\":5},\"FlowCounter\":{\"id\":251,\"time\":\"2016-12-19T10:25:40.663-04:00\",\"value\":1.0,\"dataTypeId\":4},\"FirstDetectedZone\":{\"id\":897,\"time\":\"2016-12-19T10:25:40.663-04:00\",\"value\":\"unknown\",\"dataTypeId\":1},\"TagBlinkCounter\":{\"id\":23,\"time\":\"2016-12-19T12:24:01.474-04:00\",\"value\":59.0,\"dataTypeId\":4},\"POE\":{\"id\":60,\"time\":\"2016-12-19T10:25:40.663-04:00\",\"value\":false,\"dataTypeId\":5},\"Received\":{\"id\":248,\"time\":\"2016-12-19T10:25:40.663-04:00\",\"value\":false,\"dataTypeId\":5},\"prevZoneType\":{\"id\":836,\"time\":\"2016-12-19T10:25:40.663-04:00\",\"value\":\"\",\"dataTypeId\":1},\"lastLocateTime\":{\"id\":38,\"time\":\"2016-12-19T12:24:01.474-04:00\",\"value\":1482164641474,\"dataTypeId\":24},\"Questionable\":{\"id\":910,\"time\":\"2016-12-19T10:25:40.663-04:00\",\"value\":\"N\",\"dataTypeId\":1},\"FirstDetected\":{\"id\":895,\"time\":\"2016-12-19T10:25:40.663-04:00\",\"value\":true,\"dataTypeId\":5},\"SYW\":{\"id\":59,\"time\":\"2016-12-19T10:25:40.663-04:00\",\"value\":false,\"dataTypeId\":5},\"prevZone\":{\"id\":32,\"time\":\"2016-12-19T10:25:40.663-04:00\",\"value\":\"unknown\",\"dataTypeId\":1},\"POS\":{\"id\":64,\"time\":\"2016-12-19T10:25:40.663-04:00\",\"value\":false,\"dataTypeId\":5},\"lastDetectTime\":{\"id\":41,\"time\":\"2016-12-19T12:24:01.474-04:00\",\"value\":1482164641474,\"dataTypeId\":24},\"SYWDonington\":{\"id\":909,\"time\":\"2016-12-19T10:25:40.663-04:00\",\"value\":false,\"dataTypeId\":5},\"location\":{\"id\":46,\"time\":\"2016-12-19T12:24:01.474-04:00\",\"value\":\"-118.44382784080261;34.048194069500845;0.0\",\"dataTypeId\":2},\"Rule\":{\"id\":250,\"time\":\"2016-12-19T10:25:40.663-04:00\",\"value\":\"InFlow\",\"dataTypeId\":1},\"DressingRoom\":{\"id\":27,\"time\":\"2016-12-19T10:25:40.663-04:00\",\"value\":false,\"dataTypeId\":5},\"Stolen\":{\"id\":898,\"time\":\"2016-12-19T10:25:40.663-04:00\",\"value\":false,\"dataTypeId\":5}}]}",
            "{\"meta\":{\"bridgeCode\":\"ALEB\",\"sqn\":23,\"specName\":\"MYSPECNAME\",\"origin\":[-118.44397,34.04809,0.0,20.0],\"units\":\"ft\"},\"id\":1279,\"serialNumber\":\"000000000000000000150\",\"name\":\"000000000000000000150\",\"createdTime\":\"2016-12-19T10:25:49.418-04:00\",\"modifiedTime\":\"2016-12-19T12:27:35.904-04:00\",\"time\":\"2016-12-19T12:24:03.474-04:00\",\"group\":{\"id\":2,\"name\":\"Marks and Spencer\",\"code\":\"MnS\",\"groupType\":{\"id\":2,\"name\":\"Tenant\",\"code\":\"tenant\"}},\"thingType\":{\"id\":4,\"name\":\"Item\",\"code\":\"item\"},\"properties\":[{\"STRCode\":{\"id\":277,\"time\":\"2016-12-19T10:25:42.663-04:00\",\"value\":\"\",\"dataTypeId\":1},\"flag\":{\"id\":40,\"time\":\"2016-12-19T10:25:42.663-04:00\",\"value\":false,\"dataTypeId\":5},\"tsEdgeIn\":{\"id\":852,\"time\":\"2016-12-19T12:24:03.474-04:00\",\"value\":1482164644513,\"dataTypeId\":24},\"ADJ\":{\"id\":35,\"time\":\"2016-12-19T10:25:42.663-04:00\",\"value\":false,\"dataTypeId\":5},\"FindIt\":{\"id\":841,\"time\":\"2016-12-19T10:25:42.663-04:00\",\"value\":false,\"dataTypeId\":5},\"source\":{\"id\":34,\"time\":\"2016-12-19T12:24:03.474-04:00\",\"value\":\"ALEBDerby0\",\"dataTypeId\":1},\"locationXYZ\":{\"id\":61,\"time\":\"2016-12-19T12:24:03.474-04:00\",\"value\":\"4.22;7.37;0.0\",\"dataTypeId\":3},\"FirstDetectedDate\":{\"id\":896,\"time\":\"2016-12-19T10:25:42.663-04:00\",\"value\":1482157542663,\"dataTypeId\":24},\"zone\":{\"id\":66,\"time\":\"2016-12-19T12:24:03.474-04:00\",\"value\":{\"id\":0,\"name\":\"Unknown\",\"code\":\"unknown\",\"facilityMap\":{\"code\":\"unknown\",\"time\":\"2016-12-19T12:24:03.474-04:00\"},\"zoneType\":{\"code\":\"unknown\",\"time\":\"2016-12-19T12:24:03.474-04:00\"},\"zoneGroup\":{\"code\":\"unknown\",\"time\":\"2016-12-19T12:24:03.474-04:00\"}},\"dataTypeId\":9},\"tsCoreIn\":{\"id\":851,\"time\":\"2016-12-19T12:24:03.474-04:00\",\"value\":1482164854522,\"dataTypeId\":24},\"Reassociation\":{\"id\":42,\"time\":\"2016-12-19T10:25:42.663-04:00\",\"value\":false,\"dataTypeId\":5},\"EDW\":{\"id\":53,\"time\":\"2016-12-19T10:25:42.663-04:00\",\"value\":false,\"dataTypeId\":5},\"ZoneChangeCounter\":{\"id\":67,\"time\":\"2016-12-19T10:25:42.663-04:00\",\"value\":1.0,\"dataTypeId\":4},\"exp\":{\"id\":62,\"time\":\"2016-12-19T10:25:42.663-04:00\",\"value\":\"00000055\",\"dataTypeId\":26},\"Excluded\":{\"id\":28,\"time\":\"2016-12-19T10:25:42.663-04:00\",\"value\":false,\"dataTypeId\":5},\"GMD\":{\"id\":51,\"time\":\"2016-12-19T10:25:42.663-04:00\",\"value\":false,\"dataTypeId\":5},\"Status\":{\"id\":280,\"time\":\"2016-12-19T10:25:42.663-04:00\",\"value\":\"Excluded\",\"dataTypeId\":1},\"sourceX\":{\"id\":24,\"time\":\"2016-12-19T10:25:42.663-04:00\",\"value\":\"na\",\"dataTypeId\":1},\"Returned\":{\"id\":44,\"time\":\"2016-12-19T10:25:42.663-04:00\",\"value\":false,\"dataTypeId\":5},\"FlowCounter\":{\"id\":251,\"time\":\"2016-12-19T10:25:42.663-04:00\",\"value\":1.0,\"dataTypeId\":4},\"FirstDetectedZone\":{\"id\":897,\"time\":\"2016-12-19T10:25:42.663-04:00\",\"value\":\"unknown\",\"dataTypeId\":1},\"TagBlinkCounter\":{\"id\":23,\"time\":\"2016-12-19T12:24:03.474-04:00\",\"value\":59.0,\"dataTypeId\":4},\"POE\":{\"id\":60,\"time\":\"2016-12-19T10:25:42.663-04:00\",\"value\":false,\"dataTypeId\":5},\"Received\":{\"id\":248,\"time\":\"2016-12-19T10:25:42.663-04:00\",\"value\":false,\"dataTypeId\":5},\"prevZoneType\":{\"id\":836,\"time\":\"2016-12-19T10:25:42.663-04:00\",\"value\":\"\",\"dataTypeId\":1},\"lastLocateTime\":{\"id\":38,\"time\":\"2016-12-19T12:24:03.474-04:00\",\"value\":1482164643474,\"dataTypeId\":24},\"Questionable\":{\"id\":910,\"time\":\"2016-12-19T10:25:42.663-04:00\",\"value\":\"N\",\"dataTypeId\":1},\"FirstDetected\":{\"id\":895,\"time\":\"2016-12-19T10:25:42.663-04:00\",\"value\":true,\"dataTypeId\":5},\"SYW\":{\"id\":59,\"time\":\"2016-12-19T10:25:42.663-04:00\",\"value\":false,\"dataTypeId\":5},\"prevZone\":{\"id\":32,\"time\":\"2016-12-19T10:25:42.663-04:00\",\"value\":\"unknown\",\"dataTypeId\":1},\"POS\":{\"id\":64,\"time\":\"2016-12-19T10:25:42.663-04:00\",\"value\":false,\"dataTypeId\":5},\"lastDetectTime\":{\"id\":41,\"time\":\"2016-12-19T12:24:03.474-04:00\",\"value\":1482164643474,\"dataTypeId\":24},\"SYWDonington\":{\"id\":909,\"time\":\"2016-12-19T10:25:42.663-04:00\",\"value\":false,\"dataTypeId\":5},\"location\":{\"id\":46,\"time\":\"2016-12-19T12:24:03.474-04:00\",\"value\":\"-118.44396422550214;34.048114914481495;0.0\",\"dataTypeId\":2},\"Rule\":{\"id\":250,\"time\":\"2016-12-19T10:25:42.663-04:00\",\"value\":\"InFlow\",\"dataTypeId\":1},\"DressingRoom\":{\"id\":27,\"time\":\"2016-12-19T10:25:42.663-04:00\",\"value\":false,\"dataTypeId\":5},\"Stolen\":{\"id\":898,\"time\":\"2016-12-19T10:25:42.663-04:00\",\"value\":false,\"dataTypeId\":5}}]}",
            "{\"meta\":{\"bridgeCode\":\"ALEB\",\"sqn\":24,\"specName\":\"MYSPECNAME\",\"origin\":[-118.44397,34.04809,0.0,20.0],\"units\":\"ft\"},\"id\":2217,\"serialNumber\":\"ALGO0002\",\"name\":\"algo0002\",\"createdTime\":\"2017-01-18T12:18:24.499-04:00\",\"modifiedTime\":\"2017-01-18T12:18:24.499-04:00\",\"time\":\"2017-01-18T12:18:24.346-04:00\",\"group\":{\"id\":2,\"name\":\"Marks and Spencer\",\"code\":\"MnS\",\"groupType\":{\"id\":2,\"name\":\"Tenant\",\"code\":\"tenant\"}},\"thingType\":{\"id\":55,\"name\":\"kstreamstt\",\"code\":\"kstreamstt\"},\"properties\":[{\"zone\":{\"id\":918,\"time\":\"2017-01-18T12:18:24.346-04:00\",\"value\":{\"id\":83,\"name\":\"ReassociationBrooklands\",\"code\":\"ReassociationBrooklands\",\"facilityMap\":{\"code\":\"Brooklands\",\"time\":\"2017-01-18T12:18:24.346-04:00\"},\"zoneType\":{\"code\":\"Salesfloor\",\"time\":\"2017-01-18T12:18:24.346-04:00\"},\"zoneGroup\":{\"code\":\"On-Site\",\"time\":\"2017-01-18T12:18:24.346-04:00\"}},\"dataTypeId\":9},\"shift\":{\"id\":916,\"time\":\"2017-01-18T12:18:24.346-04:00\",\"value\":{\"id\":1,\"name\":\"unshift\",\"code\":\"unshift0001\",\"active\":true,\"endTimeOfDay\":1210,\"daysOfWeek\":\"147\"},\"dataTypeId\":7},\"logicalReader\":{\"id\":915,\"time\":\"2017-01-18T12:18:24.346-04:00\",\"value\":{\"id\":46,\"name\":\"StaffCafe\",\"code\":\"StaffCafe\",\"zoneInId\":154,\"zoneOutId\":154,\"x\":\"61651.0\",\"y\":\"10.35\",\"z\":\"-55.0\"},\"dataTypeId\":23},\"group1\":{\"id\":917,\"time\":\"2017-01-18T12:18:24.346-04:00\",\"value\":{\"id\":3,\"name\":\"Brooklands\",\"code\":\"Brooklands\"},\"dataTypeId\":22}}]}",
//            "{\"meta\":{\"bridgeCode\":\"ALEB\",\"sqn\":19,\"specName\":\"MYSPECNAME\",\"origin\":[-118.44397,34.04809,0.0,20.0],\"units\":\"ft\"},\"serialNumber\":\"000000000000000000123\",\"time\":\"2017-02-08T14:56:25.790-04:00\",\"thingType\":{\"code\":\"default_rfid_thingtype\"},\"properties\":[{\"eNode\":{\"time\":\"2017-02-08T14:56:25.790-04:00\",\"value\":\"en-STOCK\"},\"tsEdgeIn\":{\"time\":\"2017-02-08T14:56:25.790-04:00\",\"value\":\"1486580186305\"},\"logicalReader\":{\"time\":\"2017-02-08T14:56:25.790-04:00\",\"value\":\"LR-STOCK\"},\"lastDetectTime\":{\"time\":\"2017-02-08T14:56:25.790-04:00\",\"value\":\"1486580185790\"},\"location\":{\"time\":\"2017-02-08T14:56:25.790-04:00\",\"value\":\"-118.44393235301621;34.0482487411745;0.0\"},\"lastLocateTime\":{\"time\":\"2017-02-08T14:56:25.790-04:00\",\"value\":\"1486580185790\"},\"locationXYZ\":{\"time\":\"2017-02-08T14:56:25.790-04:00\",\"value\":\"30.0;50.0;0.0\"}}]}",
            "{\"meta\":{\"bridgeCode\":\"ALEB\",\"sqn\":25,\"specName\":\"MYSPECNAME\",\"origin\":[-118.44397,34.04809,0.0,20.0],\"units\":\"ft\"},\"id\":0,\"serialNumber\":\"000000000000000000123\",\"time\":\"2017-02-08T16:37:49.673-04:00\",\"group\":{\"id\":2,\"name\":\"Marks and Spencer\",\"code\":\"MnS\",\"groupType\":{\"id\":2,\"name\":\"Tenant\",\"code\":\"tenant\"},\"hierarchyName\":\">MnS\",\"archived\":false,\"treeLevel\":2},\"thingType\":{\"id\":1,\"name\":\"Default RFID Thing Type\",\"code\":\"default_rfid_thingtype\",\"fields\":{\"image\":{\"id\":8,\"name\":\"image\",\"multiple\":false,\"symbol\":\"\",\"timeSeries\":false,\"typeParent\":\"DATA_TYPE\",\"unit\":\"\",\"dataTypeId\":6},\"doorEvent\":{\"id\":10,\"name\":\"doorEvent\",\"multiple\":false,\"symbol\":\"\",\"timeSeries\":true,\"typeParent\":\"DATA_TYPE\",\"unit\":\"\",\"dataTypeId\":1},\"eNode\":{\"id\":5,\"name\":\"eNode\",\"multiple\":false,\"symbol\":\"\",\"timeSeries\":true,\"typeParent\":\"DATA_TYPE\",\"unit\":\"\",\"dataTypeId\":1},\"zone\":{\"id\":4,\"name\":\"zone\",\"multiple\":false,\"symbol\":\"\",\"timeSeries\":true,\"typeParent\":\"DATA_TYPE\",\"unit\":\"\",\"dataTypeId\":9},\"shift\":{\"id\":11,\"name\":\"shift\",\"multiple\":false,\"symbol\":\"\",\"timeSeries\":true,\"typeParent\":\"DATA_TYPE\",\"unit\":\"\",\"dataTypeId\":7},\"logicalReader\":{\"id\":3,\"name\":\"logicalReader\",\"multiple\":false,\"symbol\":\"\",\"timeSeries\":true,\"typeParent\":\"DATA_TYPE\",\"unit\":\"\",\"dataTypeId\":23},\"lastDetectTime\":{\"id\":7,\"name\":\"lastDetectTime\",\"multiple\":false,\"symbol\":\"ms\",\"timeSeries\":false,\"typeParent\":\"DATA_TYPE\",\"unit\":\"millisecond\",\"dataTypeId\":24},\"registered\":{\"id\":9,\"name\":\"registered\",\"multiple\":false,\"symbol\":\"ms\",\"timeSeries\":true,\"typeParent\":\"DATA_TYPE\",\"unit\":\"millisecond\",\"dataTypeId\":4},\"location\":{\"id\":1,\"name\":\"location\",\"multiple\":false,\"symbol\":\"\",\"timeSeries\":true,\"typeParent\":\"DATA_TYPE\",\"unit\":\"\",\"dataTypeId\":2},\"lastLocateTime\":{\"id\":6,\"name\":\"lastLocateTime\",\"multiple\":false,\"symbol\":\"ms\",\"timeSeries\":false,\"typeParent\":\"DATA_TYPE\",\"unit\":\"millisecond\",\"dataTypeId\":24},\"status\":{\"id\":12,\"name\":\"status\",\"multiple\":false,\"symbol\":\"\",\"timeSeries\":true,\"typeParent\":\"DATA_TYPE\",\"unit\":\"\",\"dataTypeId\":1},\"locationXYZ\":{\"id\":2,\"name\":\"locationXYZ\",\"multiple\":false,\"symbol\":\"\",\"timeSeries\":true,\"typeParent\":\"DATA_TYPE\",\"unit\":\"\",\"dataTypeId\":3}},\"archived\":false,\"autoCreate\":true,\"isParent\":false,\"groupId\":3,\"modifiedTime\":1464129399344,\"thingTypeTemplateId\":1},\"properties\":[{\"eNode\":{\"id\":0,\"time\":\"2017-02-08T16:37:49.673-04:00\",\"blinked\":true,\"modified\":false,\"timeSeries\":false,\"value\":\"en-STOCK\",\"dataTypeId\":1},\"lastDetectTime\":{\"id\":0,\"time\":\"2017-02-08T16:37:49.673-04:00\",\"blinked\":true,\"modified\":false,\"timeSeries\":false,\"value\":\"1486586269673\",\"dataTypeId\":24},\"location\":{\"id\":0,\"time\":\"2017-02-08T16:37:49.673-04:00\",\"blinked\":true,\"modified\":false,\"timeSeries\":false,\"value\":\"-118.44393235301621;34.0482487411745;0.0\",\"dataTypeId\":2},\"lastLocateTime\":{\"id\":0,\"time\":\"2017-02-08T16:37:49.673-04:00\",\"blinked\":true,\"modified\":false,\"timeSeries\":false,\"value\":\"1486586269673\",\"dataTypeId\":24},\"locationXYZ\":{\"id\":0,\"time\":\"2017-02-08T16:37:49.673-04:00\",\"blinked\":true,\"modified\":false,\"timeSeries\":false,\"value\":\"30.0;50.0;0.0\",\"dataTypeId\":3}}]}"
    };

    @Before
    public void setUp()
    throws Exception {
        this.mapper = new ObjectMapper();
    }

    @Test(expected = NullPointerException.class)
    public void deserializeANullJsonString()
    throws Exception {
        final String jsonString = null;
        this.mapper.readValue(jsonString, ThingDto.class);
    }

    @Test
    public void deserializeAValidJsonString()
    throws Exception {
        final String jsonString = this.buildAValidJsonString();
        ThingDto thingDto = this.mapper.readValue(jsonString, ThingDto.class);
        assertNotEquals(null, thingDto, "The thing DTO is null");
    }

    @Test
    public void deserializeAJsonStringAndTestThingDTOFields()
    throws Exception {
        SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT);
        final String jsonString = this.buildAValidJsonString();
        ThingDto thingDto = this.mapper.readValue(jsonString, ThingDto.class);

        Long expectedId = 219667L;
        assertEquals("The id is not equal", expectedId, thingDto.id);

        String expectedSerialNumber = "TESTDEV0001";
        assertEquals("The Serial Number is not equal", expectedSerialNumber, thingDto.serialNumber);

        String expectedName = "TESTDEV0001";
        assertEquals("The Name is not equal", expectedName, thingDto.name);

        Date createdTime = thingDto.createdTime;
        Date expectedCreatedTime = dateFormat.parse("2017-01-24T21:59:58.254-04:00");
        assertEquals("The created time is not equal", expectedCreatedTime, createdTime);

        Date modifiedTime = thingDto.modifiedTime;
        Date expectedModifiedTime = dateFormat.parse("2017-01-24T21:59:58.254-04:00");
        assertEquals("The modified time is not equal", expectedModifiedTime, modifiedTime);

        Date time = thingDto.time;
        Date expectedTime = dateFormat.parse("2017-01-24T21:59:58.060-04:00");
        assertEquals("The time is not equal", expectedTime, time);

        GroupDto expectedGroupDto = new GroupDto();
        expectedGroupDto.id = 2L;
        expectedGroupDto.name = "Marks and Spencer";
        expectedGroupDto.code = "MnS";
        expectedGroupDto.archived = false;

        GroupTypeDto expectedGroupType = new GroupTypeDto();
        expectedGroupType.id = 2L;
        expectedGroupType.code = "tenant";
        expectedGroupType.name = "Tenant";

        expectedGroupDto.groupType = expectedGroupType;
        assertEquals("The group is not equal", expectedGroupDto, thingDto.group);
        assertEquals("The group type is not equal", expectedGroupType, thingDto.group.groupType);


        ThingTypeDto expectedThingTypeDto = new ThingTypeDto();
        expectedThingTypeDto.id = 4L;
        expectedThingTypeDto.code = "item";
        expectedThingTypeDto.name = "Item";
        assertEquals("The thing type is not equal", expectedThingTypeDto, thingDto.thingType);

        MetaDto expectedMetaDto = new MetaDto();
        expectedMetaDto.bridgeCode = "ALEB";
        expectedMetaDto.sqn = 1L;
        expectedMetaDto.specName = "SPECNAME";
        expectedMetaDto.origin = new Double[] {new Double(10.0), new Double(20.0), new Double(30.0)};
        expectedMetaDto.units = "ft";
        expectedMetaDto.partition = 1L;
        expectedMetaDto.numPartitions = 8L;
        expectedMetaDto.reblinked = false;
        expectedMetaDto.outOfOrder = false;
        expectedMetaDto.newBlink = false;
        assertEquals("The meta is not equal", expectedMetaDto, thingDto.meta);
    }
    
    @Test 
    public void testStackTraceContainsJson() {
    	String invalidDate = "ABCD-02-25";
		String source = "{\"meta\":{\"bridgeCode\":\"STAR\"},\"serialNumber\":\"\",\"time\":\"%sT10:06:15.457-04:00\",\"thingType\":{}}";
        String input = String.format(source, invalidDate);
		try {
        	this.mapper.readValue(input, ThingDto.class);	
        } catch(IOException e) {
        	ByteArrayOutputStream stream = new ByteArrayOutputStream(2048);
        	e.printStackTrace(new PrintStream(stream));
        	String stacktrace = new String(stream.toByteArray());
        	Assert.assertTrue("stacktrace does not contain input json", stacktrace.contains(input));
        }
    }
    
    @Test
    public void testProperties() {
        JsonDeserializer<ThingDto> deserializer = new JsonDeserializer<>(ThingDto.class);
        for (String json : validJsons) {
            ThingDto thing = deserializer.deserialize("", json.getBytes());
            assertNotNull(thing);
            assertNotNull(thing.properties);
            assertNotNull(thing.properties.get(0));
            assertTrue(thing.properties.get(0).size() > 0);
            thing.properties.get(0).forEach((s, property) -> {
                assertNotNull(property.id);
                assertNotNull(property.time);
                assertNotNull(property.dataTypeId);
                assertNotNull(property.value);

                switch (property.dataTypeId.intValue()) {
                    case 1:
                        assertThat(property.value, instanceOf(String.class));
                        break;
                    case 2:
                        assertThat(property.value, instanceOf(String.class));
                        break;
                    case 3:
                        assertThat(property.value, instanceOf(String.class));
                        break;
                    case 4:
                        assertThat(property.value, instanceOf(Double.class));
                        break;
                    case 5:
                        assertThat(property.value, instanceOf(Boolean.class));
                        break;
                    case 6:
                        assertThat(property.value, instanceOf(String.class));
                        break;
                    case 7:
                        assertThat(property.value, instanceOf(ShiftDto.class));
                        ShiftDto shift = (ShiftDto) property.value;
                        assertNotNull(shift.id);
                        assertNotNull(shift.name);
                        assertNotNull(shift.code);
                        assertNotNull(shift.active);
                        assertNotNull(shift.endTimeOfDay);
                        assertNotNull(shift.daysOfWeek);
                        break;
                    case 8:
                        assertThat(property.value, instanceOf(String.class));
                        break;
                    case 9:
                        assertThat(property.value, instanceOf(ZoneDto.class));
                        ZoneDto zone = (ZoneDto) property.value;
                        assertNotNull(zone.id);
                        assertNotNull(zone.name);
                        assertNotNull(zone.code);
                        assertNotNull(zone.facilityMap);
                        assertNotNull(zone.facilityMap.code);
                        assertNotNull(zone.facilityMap.time);
                        assertNotNull(zone.zoneType);
                        assertNotNull(zone.zoneType.code);
                        assertNotNull(zone.zoneType.time);
                        assertNotNull(zone.zoneGroup);
                        assertNotNull(zone.zoneGroup.code);
                        assertNotNull(zone.zoneGroup.time);
                        break;
                    case 10:
                        assertThat(property.value, instanceOf(String.class));
                        break;
                    case 11:
                        assertThat(property.value, instanceOf(Date.class));
                        break;
                    case 12:
                        assertThat(property.value, instanceOf(String.class));
                        break;
                    case 13:
                        assertThat(property.value, instanceOf(String.class));
                        break;
                    case 22:
                        assertThat(property.value, instanceOf(GroupDto.class));
                        GroupDto group = (GroupDto) property.value;
                        assertNotNull(group.id);
                        assertNotNull(group.name);
                        assertNotNull(group.code);
                        break;
                    case 23:
                        assertThat(property.value, instanceOf(LogicalReaderDto.class));
                        LogicalReaderDto logicalReader = (LogicalReaderDto) property.value;
                        assertNotNull(logicalReader.id);
                        assertNotNull(logicalReader.name);
                        assertNotNull(logicalReader.code);
                        assertNotNull(logicalReader.zoneInId);
                        assertNotNull(logicalReader.zoneOutId);
                        assertNotNull(logicalReader.x);
                        assertNotNull(logicalReader.y);
                        assertNotNull(logicalReader.z);
                        break;
                    case 24:
                        assertThat(property.value, instanceOf(Long.class));
                        break;
                    case 25:
                        assertThat(property.value, instanceOf(Double.class));
                        break;
                    case 26:
                        assertThat(property.value, instanceOf(String.class));
                        break;
                    case 27:
                        assertThat(property.value, instanceOf(ThingDto.class));
                        break;
                    case 28:
                        assertThat(property.value, instanceOf(String.class));
                        break;
                }
            });
        }
        
        deserializer.close();
    }

    @Test(expected = IOException.class)
    public void deserializeAInvalidJsonString()
    throws Exception {
        final String jsonString = this.buildAnInvalidJsonString();
        this.mapper.readValue(jsonString, ThingDto.class);
    }

    @Test
    public void deserializeAValidJsonWithNullThingPropertyValue()
    throws IOException {
        final String jsonString = this.buildAnValidJsonWithNullThingPropertyValue();
        ThingDto thingDto = this.mapper.readValue(jsonString, ThingDto.class);
        ThingPropertyDto thingPropertyDto = thingDto.getUdf("flag");
        assertNull(thingPropertyDto.getValue());
    }

    @Test(expected = IOException.class)
    public void deserializeAJsonWithoutMeta()
    throws IOException {
        final String jsonString = this.buildAnJsonWithoutMeta();
        ThingDto thingDto = this.mapper.readValue(jsonString, ThingDto.class);
    }

    /**
     * Builds a valid json string.
     *
     * @return a valid json string
     */
    private String buildAValidJsonString() {
        String jsonString = "{\n" + "  \"id\": 219667,\n" + "  \"serialNumber\": \"TESTDEV0001\",\n" + "  \"name\": \"TESTDEV0001\",\n"
            + "  \"createdTime\": \"2017-01-24T21:59:58.254-04:00\",\n" + "  \"modifiedTime\": \"2017-01-24T21:59:58.254-04:00\",\n"
            + "  \"time\": \"2017-01-24T21:59:58.060-04:00\",\n" + "  \"group\": {\n" + "    \"id\": 2,\n" + "    \"name\": \"Marks and Spencer\",\n"
            + "    \"code\": \"MnS\",\n" + "    \"groupType\": {\n" + "      \"id\": 2,\n" + "      \"name\": \"Tenant\",\n"
            + "      \"code\": \"tenant\"\n" + "    },\n" + "    \"archived\": false\n" + "  },\n" + "  \"thingType\": {\n" + "    \"id\": 4,\n"
            + "    \"name\": \"Item\",\n" + "    \"code\": \"item\",\n" + "    \"udfs\": []\n" + "  },\n" + "  \"properties\": [\n" + "    {\n"
            + "      \"flag\": {\n" + "        \"id\": 40,\n" + "        \"time\": \"2017-01-24T21:59:58.060-04:00\",\n"
            + "        \"blinked\": false,\n" + "        \"modified\": false,\n" + "        \"timeSeries\": false,\n" + "        \"value\": false,\n"
            + "        \"dataTypeId\": 5\n" + "      },\n" + "      \"ADJ\": {\n" + "        \"id\": 35,\n"
            + "        \"time\": \"2017-01-24T21:59:58.060-04:00\",\n" + "        \"blinked\": false,\n" + "        \"modified\": false,\n"
            + "        \"timeSeries\": false,\n" + "        \"value\": false,\n" + "        \"dataTypeId\": 5\n" + "      },\n"
            + "      \"FindIt\": {\n" + "        \"id\": 841,\n" + "        \"time\": \"2017-01-24T21:59:58.060-04:00\",\n"
            + "        \"blinked\": false,\n" + "        \"modified\": false,\n" + "        \"timeSeries\": false,\n" + "        \"value\": false,\n"
            + "        \"dataTypeId\": 5\n" + "      },\n" + "      \"source\": {\n" + "        \"id\": 34,\n"
            + "        \"time\": \"2017-01-24T21:59:58.060-04:00\",\n" + "        \"blinked\": false,\n" + "        \"modified\": false,\n"
            + "        \"timeSeries\": false,\n" + "        \"value\": \"SERVICES\",\n" + "        \"dataTypeId\": 1\n" + "      },\n"
            + "      \"locationXYZ\": {\n" + "        \"id\": 61,\n" + "        \"time\": \"2017-01-24T21:59:58.060-04:00\",\n"
            + "        \"blinked\": false,\n" + "        \"modified\": false,\n" + "        \"timeSeries\": false,\n"
            + "        \"value\": \"4.6;59.1;0.0\",\n" + "        \"dataTypeId\": 3\n" + "      },\n" + "      \"zone\": {\n"
            + "        \"id\": 66,\n" + "        \"time\": \"2017-01-24T21:59:58.060-04:00\",\n" + "        \"blinked\": false,\n"
            + "        \"modified\": false,\n" + "        \"timeSeries\": false,\n" + "        \"value\": {\n" + "          \"id\": 83,\n"
            + "          \"name\": \"ReassociationBrooklands\",\n" + "          \"code\": \"ReassociationBrooklands\",\n"
            + "          \"facilityMap\": {\n" + "            \"code\": \"Brooklands\",\n"
            + "            \"time\": \"2017-01-24T21:59:58.060-04:00\"\n" + "          },\n" + "          \"zoneType\": {\n"
            + "            \"code\": \"Salesfloor\",\n" + "            \"time\": \"2017-01-24T21:59:58.060-04:00\"\n" + "          },\n"
            + "          \"zoneGroup\": {\n" + "            \"code\": \"On-Site\",\n" + "            \"time\": \"2017-01-24T21:59:58.060-04:00\"\n"
            + "          }\n" + "        },\n" + "        \"dataTypeId\": 9\n" + "      },\n" + "      \"EDW\": {\n" + "        \"id\": 53,\n"
            + "        \"time\": \"2017-01-24T21:59:58.060-04:00\",\n" + "        \"blinked\": false,\n" + "        \"modified\": false,\n"
            + "        \"timeSeries\": false,\n" + "        \"value\": false,\n" + "        \"dataTypeId\": 5\n" + "      },\n"
            + "      \"Reassociation\": {\n" + "        \"id\": 42,\n" + "        \"time\": \"2017-01-24T21:59:58.060-04:00\",\n"
            + "        \"blinked\": false,\n" + "        \"modified\": false,\n" + "        \"timeSeries\": false,\n" + "        \"value\": false,\n"
            + "        \"dataTypeId\": 5\n" + "      },\n" + "      \"ZoneChangeCounter\": {\n" + "        \"id\": 67,\n"
            + "        \"time\": \"2017-01-24T21:59:58.060-04:00\",\n" + "        \"blinked\": false,\n" + "        \"modified\": false,\n"
            + "        \"timeSeries\": false,\n" + "        \"value\": 0.0,\n" + "        \"dataTypeId\": 4\n" + "      },\n" + "      \"exp\": {\n"
            + "        \"id\": 62,\n" + "        \"time\": \"2017-01-24T21:59:58.060-04:00\",\n" + "        \"blinked\": false,\n"
            + "        \"modified\": false,\n" + "        \"timeSeries\": false,\n" + "        \"value\": \"00000000\",\n"
            + "        \"dataTypeId\": 26\n" + "      },\n" + "      \"Excluded\": {\n" + "        \"id\": 28,\n"
            + "        \"time\": \"2017-01-24T21:59:58.060-04:00\",\n" + "        \"blinked\": false,\n" + "        \"modified\": false,\n"
            + "        \"timeSeries\": false,\n" + "        \"value\": false,\n" + "        \"dataTypeId\": 5\n" + "      },\n" + "      \"GMD\": {\n"
            + "        \"id\": 51,\n" + "        \"time\": \"2017-01-24T21:59:58.060-04:00\",\n" + "        \"blinked\": false,\n"
            + "        \"modified\": false,\n" + "        \"timeSeries\": false,\n" + "        \"value\": false,\n" + "        \"dataTypeId\": 5\n"
            + "      },\n" + "      \"Status\": {\n" + "        \"id\": 280,\n" + "        \"time\": \"2017-01-24T21:59:58.060-04:00\",\n"
            + "        \"blinked\": false,\n" + "        \"modified\": false,\n" + "        \"timeSeries\": false,\n"
            + "        \"value\": \"Free\",\n" + "        \"dataTypeId\": 1\n" + "      },\n" + "      \"Returned\": {\n" + "        \"id\": 44,\n"
            + "        \"time\": \"2017-01-24T21:59:58.060-04:00\",\n" + "        \"blinked\": false,\n" + "        \"modified\": false,\n"
            + "        \"timeSeries\": false,\n" + "        \"value\": false,\n" + "        \"dataTypeId\": 5\n" + "      },\n"
            + "      \"sourceX\": {\n" + "        \"id\": 24,\n" + "        \"time\": \"2017-01-24T21:59:58.060-04:00\",\n"
            + "        \"blinked\": false,\n" + "        \"modified\": false,\n" + "        \"timeSeries\": false,\n" + "        \"value\": \"na\",\n"
            + "        \"dataTypeId\": 1\n" + "      },\n" + "      \"FlowCounter\": {\n" + "        \"id\": 251,\n"
            + "        \"time\": \"2017-01-24T21:59:58.060-04:00\",\n" + "        \"blinked\": false,\n" + "        \"modified\": false,\n"
            + "        \"timeSeries\": false,\n" + "        \"value\": 0.0,\n" + "        \"dataTypeId\": 4\n" + "      },\n"
            + "      \"TagBlinkCounter\": {\n" + "        \"id\": 23,\n" + "        \"time\": \"2017-01-24T21:59:58.060-04:00\",\n"
            + "        \"blinked\": false,\n" + "        \"modified\": false,\n" + "        \"timeSeries\": false,\n" + "        \"value\": 0.0,\n"
            + "        \"dataTypeId\": 4\n" + "      },\n" + "      \"Received\": {\n" + "        \"id\": 248,\n"
            + "        \"time\": \"2017-01-24T21:59:58.060-04:00\",\n" + "        \"blinked\": false,\n" + "        \"modified\": false,\n"
            + "        \"timeSeries\": false,\n" + "        \"value\": false,\n" + "        \"dataTypeId\": 5\n" + "      },\n" + "      \"POE\": {\n"
            + "        \"id\": 60,\n" + "        \"time\": \"2017-01-24T21:59:58.060-04:00\",\n" + "        \"blinked\": false,\n"
            + "        \"modified\": false,\n" + "        \"timeSeries\": false,\n" + "        \"value\": false,\n" + "        \"dataTypeId\": 5\n"
            + "      },\n" + "      \"lastLocateTime\": {\n" + "        \"id\": 38,\n" + "        \"time\": \"2017-01-24T21:59:58.060-04:00\",\n"
            + "        \"blinked\": false,\n" + "        \"modified\": false,\n" + "        \"timeSeries\": false,\n"
            + "        \"value\": 1485309598060,\n" + "        \"dataTypeId\": 24\n" + "      },\n" + "      \"Questionable\": {\n"
            + "        \"id\": 910,\n" + "        \"time\": \"2017-01-24T21:59:58.060-04:00\",\n" + "        \"blinked\": false,\n"
            + "        \"modified\": false,\n" + "        \"timeSeries\": false,\n" + "        \"value\": \"N\",\n" + "        \"dataTypeId\": 1\n"
            + "      },\n" + "      \"FirstDetected\": {\n" + "        \"id\": 895,\n" + "        \"time\": \"2017-01-24T21:59:58.060-04:00\",\n"
            + "        \"blinked\": false,\n" + "        \"modified\": false,\n" + "        \"timeSeries\": false,\n" + "        \"value\": false,\n"
            + "        \"dataTypeId\": 5\n" + "      },\n" + "      \"SYW\": {\n" + "        \"id\": 59,\n"
            + "        \"time\": \"2017-01-24T21:59:58.060-04:00\",\n" + "        \"blinked\": false,\n" + "        \"modified\": false,\n"
            + "        \"timeSeries\": false,\n" + "        \"value\": false,\n" + "        \"dataTypeId\": 5\n" + "      },\n"
            + "      \"prevZone\": {\n" + "        \"id\": 32,\n" + "        \"time\": \"2017-01-24T21:59:58.060-04:00\",\n"
            + "        \"blinked\": false,\n" + "        \"modified\": false,\n" + "        \"timeSeries\": false,\n"
            + "        \"value\": \"Unknown\",\n" + "        \"dataTypeId\": 1\n" + "      },\n" + "      \"POS\": {\n" + "        \"id\": 64,\n"
            + "        \"time\": \"2017-01-24T21:59:58.060-04:00\",\n" + "        \"blinked\": false,\n" + "        \"modified\": false,\n"
            + "        \"timeSeries\": false,\n" + "        \"value\": false,\n" + "        \"dataTypeId\": 5\n" + "      },\n"
            + "      \"lastDetectTime\": {\n" + "        \"id\": 41,\n" + "        \"time\": \"2017-01-24T21:59:58.060-04:00\",\n"
            + "        \"blinked\": false,\n" + "        \"modified\": false,\n" + "        \"timeSeries\": false,\n"
            + "        \"value\": 1485309598060,\n" + "        \"dataTypeId\": 24\n" + "      },\n" + "      \"SYWDonington\": {\n"
            + "        \"id\": 909,\n" + "        \"time\": \"2017-01-24T21:59:58.060-04:00\",\n" + "        \"blinked\": false,\n"
            + "        \"modified\": false,\n" + "        \"timeSeries\": false,\n" + "        \"value\": false,\n" + "        \"dataTypeId\": 5\n"
            + "      },\n" + "      \"location\": {\n" + "        \"id\": 46,\n" + "        \"time\": \"2017-01-24T21:59:58.060-04:00\",\n"
            + "        \"blinked\": false,\n" + "        \"modified\": false,\n" + "        \"timeSeries\": false,\n"
            + "        \"value\": \"-0.4780315048992634;51.34545352840299;0.0\",\n" + "        \"dataTypeId\": 2\n" + "      },\n"
            + "      \"DressingRoom\": {\n" + "        \"id\": 27,\n" + "        \"time\": \"2017-01-24T21:59:58.060-04:00\",\n"
            + "        \"blinked\": false,\n" + "        \"modified\": false,\n" + "        \"timeSeries\": false,\n" + "        \"value\": false,\n"
            + "        \"dataTypeId\": 5\n" + "      },\n" + "      \"Stolen\": {\n" + "        \"id\": 898,\n"
            + "        \"time\": \"2017-01-24T21:59:58.060-04:00\",\n" + "        \"blinked\": false,\n" + "        \"modified\": false,\n"
            + "        \"timeSeries\": false,\n" + "        \"value\": false,\n" + "        \"dataTypeId\": 5\n" + "      }\n" + "    }\n" + "  ],\n"
            + "  \"meta\": {\n" + "      \"bridgeCode\": \"ALEB\",\n" + "      \"sqn\": 1,\n" + "      \"specName\": \"SPECNAME\",\n"
            + "      \"origin\": [10.0,20.0,30.0],\n" + "      \"units\": \"ft\",\n" + "      \"partition\": 1,\n" + "      \"numPartitions\": 8,\n"
            + "      \"reblinked\": false,\n" + "      \"outOfOrder\": false,\n" + "      \"newBlink\": false\n" + "  }\n" + "}";
        return jsonString;
    }

    /**
     * Builds an invalid json string, build a thing type body.
     *
     * @return an invalid json string
     */
    private String buildAnInvalidJsonString() {
        String jsonString = "{\n" + "  \"id\": 13,\n" + "  \"name\": \"FlowExecutionPickList\",\n" + "  \"code\": \"FlowExecutionPickList\",\n"
            + "  \"fields\": {},\n" + "  \"udfs\": []\n" + "}";
        return jsonString;
    }


    private String buildAnValidJsonWithNullThingPropertyValue() {
        String thingDtoJson = "{\"meta\":{\"bridgeCode\":\"ALEB\",\"sqn\":1,\"specName\":\"MYSPECNAME\",\"origin\":[-118.43969,34.048092,0.0,20.0],\"units\":\"ft\",\"partition\":1,\"numPartitions\":10,\"reblinked\":false,\"outOfOrder\":false,\"newBlink\":true},\"id\":1,\"serialNumber\":\"TEST0001\",\"name\":\"TEST0001\",\"createdTime\":\"2017-02-01T00:00:00.000-04:00\",\"modifiedTime\":\"2017-02-01T00:00:00.000-04:00\",\"time\":\"2017-02-01T00:00:00.000-04:00\",\"group\":{\"groupType\":{\"id\":2,\"name\":\"Tenant\",\"code\":\"tenant\"},\"id\":2,\"name\":\"Marks and Spencer\",\"code\":\"Mns\"},\"thingType\":{\"id\":4,\"name\":\"Item\",\"code\":\"item\",\"fields\":{\"SoldDate\":{\"id\":50,\"name\":\"SoldDate\",\"dataTypeId\":24}}},\"properties\":[{\"flag\":{\"id\":40,\"time\":\"2017-02-01T00:00:00.000-04:00\",\"blinked\":false,\"modified\":false,\"timeSeries\":false,\"value\":null,\"dataTypeId\":5}},{}]}";
        return thingDtoJson;
    }

    private String buildAnJsonWithoutMeta() {
        String thingDtoJson = "{\"serialNumber\":\"DEVTESTDDD0001\",\"time\":\"2017-03-06T12:24:14.447-04:00\",\"thingType\":{\"code\":\"BPKwinana\"}}";
        return thingDtoJson;
    }

}