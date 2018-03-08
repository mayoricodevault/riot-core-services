package com.tierconnect.riot.iot.reports.views.things;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tierconnect.riot.appcore.core.BaseTestIOT;
import com.tierconnect.riot.appcore.entities.User;

import com.tierconnect.riot.iot.reports.views.things.dto.ListResult;
import org.apache.shiro.SecurityUtils;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Created by achambi on 7/10/17.
 * New Test for thing list.
 */
public class ThingListExecutorTest extends BaseTestIOT {
    @Test
    public void getInstance() throws Exception {
    }

    @Test
    public void list() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        LinkedList readValue = mapper.readValue(
                this.getClass().getClassLoader().getResourceAsStream("thing/listThingReportAPI.json"),
                LinkedList.class);
        //Map<String, Object> result =
        ListResult listResult = ThingListExecutor.getInstance().list(
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
                false,
                SecurityUtils.getSubject(),
                (User) SecurityUtils.getSubject().getPrincipal(),
                true,
                true,
                true);
        assertNotNull(listResult);
        assertEquals(15, listResult.getResults().size());
        assertEquals(new Long(37), listResult.getTotal());
        List resultList = listResult.getResults();
        assertEquals(mapper.writeValueAsString(readValue), mapper.writeValueAsString(resultList));
    }

    @Test
    public void listWithTreeFlag() throws Exception {
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
        LinkedList readValue = mapper.readValue(
                this.getClass().getClassLoader().getResourceAsStream("thing/listThingsTreeViewTrue.json"),
                LinkedList.class);
        ListResult result = ThingListExecutor.getInstance().list(
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
                true,
                SecurityUtils.getSubject(),
                (User) SecurityUtils.getSubject().getPrincipal(),
                true,
                true,
                true);
        assertNotNull(result);
        assertEquals(15, result.getResults().size());
        assertEquals(new Long(17), result.getTotal());
        ArrayList resultList = (ArrayList) result.getResults();
        assertEquals(mapper.writeValueAsString(readValue), mapper.writeValueAsString(resultList));
    }
}