package com.tierconnect.riot.iot.dao.mongo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import com.tierconnect.riot.api.mongoShell.testUtils.PropertiesReaderUtil;
import com.tierconnect.riot.appcore.core.BaseTestIOT;
import com.tierconnect.riot.iot.utils.VisibilityThingUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Created by achambi on 7/3/17.
 * Test for list things.
 */
public class ThingMongoDAOTest extends BaseTestIOT {

    @Before
    public void setUp() throws Exception {
        PropertiesReaderUtil.setConfigurationFile("propertiesTest.properties");
    }

    @Test
    public void getValue() throws Exception {
        ThingMongoDAO thingMongoDAO = new ThingMongoDAO();
        BasicDBObject dbObject = BasicDBObject.parse("\n" +
                "{\n" +
                "    \"_id\" : NumberLong(5),\n" +
                "    \"groupTypeId\" : NumberLong(3),\n" +
                "    \"groupTypeName\" : \"Store\",\n" +
                "    \"groupTypeCode\" : \"Store\",\n" +
                "    \"groupId\" : NumberLong(3),\n" +
                "    \"groupCode\" : \"SM\",\n" +
                "    \"groupName\" : \"Santa Monica\",\n" +
                "    \"thingTypeId\" : NumberLong(5),\n" +
                "    \"thingTypeCode\" : \"jackets_code\",\n" +
                "    \"thingTypeName\" : \"Jackets\",\n" +
                "    \"name\" : \"Jacket2\",\n" +
                "    \"serialNumber\" : \"J00002\",\n" +
                "    \"modifiedTime\" : ISODate(\"2017-07-03T15:25:12.001Z\"),\n" +
                "    \"createdTime\" : ISODate(\"2017-07-03T15:25:12.001Z\"),\n" +
                "    \"time\" : ISODate(\"2017-07-03T15:25:11.991Z\"),\n" +
                "    \"color\" : {\n" +
                "        \"thingTypeFieldId\" : NumberLong(54),\n" +
                "        \"time\" : ISODate(\"2017-07-03T15:25:11.991Z\"),\n" +
                "        \"value\" : \"Black\"\n" +
                "    },\n" +
                "    \"size\" : {\n" +
                "        \"thingTypeFieldId\" : NumberLong(53),\n" +
                "        \"time\" : ISODate(\"2017-07-03T15:25:11.991Z\"),\n" +
                "        \"value\" : \"Large\"\n" +
                "    },\n" +
                "    \"children\" : [ \n" +
                "        {\n" +
                "            \"_id\" : NumberLong(6),\n" +
                "            \"groupTypeId\" : NumberLong(3),\n" +
                "            \"groupTypeName\" : \"Store\",\n" +
                "            \"groupTypeCode\" : \"Store\",\n" +
                "            \"groupId\" : NumberLong(3),\n" +
                "            \"groupCode\" : \"SM\",\n" +
                "            \"groupName\" : \"Santa Monica\",\n" +
                "            \"thingTypeId\" : NumberLong(3),\n" +
                "            \"thingTypeCode\" : \"default_rfid_thingtype\",\n" +
                "            \"thingTypeName\" : \"Default RFID Thing Type\",\n" +
                "            \"name\" : \"000000000000000000474\",\n" +
                "            \"serialNumber\" : \"000000000000000000474\",\n" +
                "            \"modifiedTime\" : ISODate(\"2017-07-03T15:25:12.036Z\"),\n" +
                "            \"createdTime\" : ISODate(\"2017-07-03T15:25:12.036Z\"),\n" +
                "            \"time\" : ISODate(\"2017-07-03T15:25:12.030Z\"),\n" +
                "            \"eNode\" : {\n" +
                "                \"thingTypeFieldId\" : NumberLong(35),\n" +
                "                \"time\" : ISODate(\"2017-07-03T15:25:12.030Z\"),\n" +
                "                \"value\" : \"x3ed9371\"\n" +
                "            }\n" +
                "        }\n" +
                "    ]\n" +
                "}");
        ObjectMapper mapper = new ObjectMapper();
        List<String> myList = new ArrayList<>(Arrays.asList("_id,name,thingTypeName,serialNumber,groupId,groupName,groupTypeName,thingTypeId,children.value.serialNumber,color.value".split(",")));
        Map<String, Object> value = thingMongoDAO.getValue(dbObject, myList, 1, true);
        HashMap readValue = mapper.readValue(
                this.getClass().getClassLoader().getResourceAsStream("thing/fieldTreeViewTrue.json"),
                HashMap.class);
        assertNotNull(value);
        assertEquals(readValue.toString(), value.toString());
    }

    @Test
    public void listThings() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ArrayList readValue = mapper.readValue(
                this.getClass().getClassLoader().getResourceAsStream("thing/listThing.json"),
                ArrayList.class);
        Map<String, Object> result = ThingMongoDAO.getInstance().getListOfThings(
                15,
                1,
                "name:asc",
                "",
                null,
                "_id,name,thingTypeName,serialNumber,groupId,groupName,groupTypeName,thingTypeId",
                null,
                1L,
                "false",
                "",
                VisibilityThingUtils.calculateThingsVisibility(1L),
                new HashMap<>(), null, null, false, true);
        assertNotNull(result);
        assertEquals(15, ((List) result.get("list")).size());
        assertEquals(37, Integer.parseInt(result.get("total").toString()));
        List resultList = (List) result.get("list");
        assertEquals(readValue.toString(), resultList.toString());
    }

    @Test
    public void listThingsTreeView() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        HashMap<String, Object> queryForTree = mapper.readValue("{\n" +
                "  \"nameUdfParent\": {\n" +
                "    \"shippingOrderField\": 8\n" +
                "  },\n" +
                "  \"thingTypeIsParentMap\": {\n" +
                "    \"jackets_code\": null,\n" +
                "    \"asset_code\": null,\n" +
                "    \"shippingorder_code\": [\n" +
                "      \"asset_code_children\"\n" +
                "    ],\n" +
                "    \"pants_code\": null\n" +
                "  },\n" +
                "  \"thingTypeIsNotParentMap\": {\n" +
                "    \"asset_code\": []\n" +
                "  }\n" +
                "}", new TypeReference<Map<String, Object>>() {
        });
        queryForTree.put("nativeThingTypeIsNotParentMap", new TreeSet<Long>());
        ArrayList readValue = mapper.readValue(
                this.getClass().getClassLoader().getResourceAsStream("thing/listThingsTreeViewTrue.json"),
                ArrayList.class);

        Map<String, Object> result = ThingMongoDAO.getInstance().getListOfThings(
                15,
                1,
                "name:asc",
                "",
                null,
                "_id,name,thingTypeName,serialNumber,groupId,groupName,children.name,children.thingTypeName,children.serialNumber,children.groupName,groupTypeName,thingTypeId, color.value",
                null,
                1L,
                "false",
                "",
                VisibilityThingUtils.calculateThingsVisibility(1L),
                queryForTree,
                null,
                null,
                true,
                true);

        assertNotNull(result);
        assertEquals(15, ((List) result.get("list")).size());
        assertEquals(17, Integer.parseInt(result.get("total").toString()));
        List resultList = (List) result.get("list");
        assertEquals(mapper.writeValueAsString(readValue), mapper.writeValueAsString(resultList));
    }

    @Test
    public void listThingsTreeNullOnly() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        HashMap<String, Object> queryForTree = mapper.readValue("{\n" +
                "  \"nameUdfParent\": {\n" +
                "    \"shippingOrderField\": 8\n" +
                "  },\n" +
                "  \"thingTypeIsParentMap\": {\n" +
                "    \"jackets_code\": null,\n" +
                "    \"asset_code\": null,\n" +
                "    \"shippingorder_code\": [\n" +
                "      \"asset_code_children\"\n" +
                "    ],\n" +
                "    \"pants_code\": null\n" +
                "  },\n" +
                "  \"thingTypeIsNotParentMap\": {\n" +
                "    \"asset_code\": []\n" +
                "  }\n" +
                "}", new TypeReference<Map<String, Object>>() {
        });
        ArrayList readValue = mapper.readValue(
                this.getClass().getClassLoader().getResourceAsStream("thing/listThings*TreeViewTrue.json"),
                ArrayList.class);
        queryForTree.put("nativeThingTypeIsNotParentMap", new TreeSet<Long>());

        Map<String, Object> result = ThingMongoDAO.getInstance().getListOfThings(
                15,
                1,
                "name:asc",
                "",
                null,
                null,
                null,
                1L,
                "false",
                "",
                VisibilityThingUtils.calculateThingsVisibility(1L),
                queryForTree,
                null,
                null,
                true,
                true);

        assertNotNull(result);
        assertEquals(15, ((List) result.get("list")).size());
        assertEquals(17, Integer.parseInt(result.get("total").toString()));
        List resultList = (List) result.get("list");
        assertEquals(mapper.writeValueAsString(readValue), mapper.writeValueAsString(resultList));
    }

    @Test
    public void listThingGroupView() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        HashMap<String, Object> queryForTree = mapper.readValue("{\n" +
                "  \"nameUdfParent\": {\n" +
                "    \"shippingOrderField\": 8\n" +
                "  },\n" +
                "  \"thingTypeIsParentMap\": {\n" +
                "    \"jackets_code\": null,\n" +
                "    \"asset_code\": null,\n" +
                "    \"shippingorder_code\": [\n" +
                "      \"asset_code_children\"\n" +
                "    ],\n" +
                "    \"pants_code\": null\n" +
                "  },\n" +
                "  \"thingTypeIsNotParentMap\": {\n" +
                "    \"asset_code\": []\n" +
                "  }\n" +
                "}", new TypeReference<Map<String, Object>>() {
        });
        queryForTree.put("nativeThingTypeIsNotParentMap", new TreeSet<Long>());

        Map<String, Object> result = ThingMongoDAO.getInstance().getListOfThings(
                15,
                1,
                "name:asc",
                "",
                null,
                "_id,name,thingTypeName,serialNumber,groupId,groupName,children.name,children.thingTypeName,children.serialNumber,children.groupName,groupTypeName,thingTypeId, color.value",
                "color",
                1L,
                "false",
                "",
                VisibilityThingUtils.calculateThingsVisibility(1L),
                queryForTree,
                null,
                null,
                true,
                true);
        assertNotNull(result);
        assertEquals("{total=2, list=[{color=Black}, {color=null}]}", result.toString());
    }

}