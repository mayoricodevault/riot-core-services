package com.tierconnect.riot.iot.services.thingType;

import com.tierconnect.riot.appcore.core.BaseTestIOT;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.iot.controllers.ThingTypeController;
import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.entities.ThingTypeField;
import com.tierconnect.riot.iot.entities.ThingTypePath;
import com.tierconnect.riot.iot.services.thing.ThingServiceTest;
import com.tierconnect.riot.iot.utils.DirectedGraph;
import com.tierconnect.riot.sdk.dao.NotFoundException;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.iot.services.ThingTypeFieldService;
import com.tierconnect.riot.iot.services.ThingTypeMapService;
import com.tierconnect.riot.iot.services.ThingTypePathService;
import com.tierconnect.riot.iot.services.ThingTypeService;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

import static com.tierconnect.riot.iot.entities.ThingTypeField.Type.*;
import static com.tierconnect.riot.iot.services.thingType.ThingTypeUtilitiesForTest.getMapFieldStandard;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Created by achambi on 7/21/17.
 * Test for verify the creation of directed graph.
 */
public class ThingTypeServiceTest extends BaseTestIOT {

    private static boolean configured = false;
    private static final Logger logger = Logger.getLogger(ThingTypeServiceTest.class);

    protected ThingTypeController controller;
    ThingTypeMapService ttMapService = ThingTypeMapService.getInstance();
    ThingTypePathService ttPathService = ThingTypePathService.getInstance();

    boolean removeThingType = true;

    @Override
    protected void previousConfiguration() throws Exception {
        if (!configured) {
            logger.info("Initializing Thing Test configuration");
            super.previousConfiguration();
            ThingServiceTest thingTypeServiceTest = new ThingServiceTest();
            thingTypeServiceTest.createThingTypes();
            configured = true;
            logger.info("Finishing Thing Test configuration");
        }
    }

    @Override
    protected void initNoTransactionalConfiguration() {
        super.initNoTransactionalConfiguration();
        // TODO: remove validation in Controller
        controller = new ThingTypeController();
    }

    /**
     * Checking that the Thing Type Fields were created correctly
     *
     * @param thingTypeCode
     * @param udfs
     * @return
     * @throws Exception
     */
    protected ThingType checkThingTypeFields(String thingTypeCode, List<Map<String, Object>> udfs) throws Exception {
        ThingType thingType = ThingTypeService.getInstance().getByCode(thingTypeCode);
        assertThat(thingType, is(notNullValue()));
        assertThat(thingType.getThingTypeFields().size(), is(equalTo(udfs.size())));
        ThingTypeFieldService instance = ThingTypeFieldService.getInstance();
        for (Map<String, Object> udf : udfs) {
            ThingTypeField field = instance.getByNameAndThingTypeCode(String.valueOf(udf.get("name")), thingTypeCode);
            assertThat(field, is(notNullValue()));
            assertThat(field.getTimeSeries(), is(equalTo((Boolean) udf.get("timeSeries"))));
            assertThat(field.getDataType().getId(), is(equalTo(Long.valueOf((Integer) udf.get("type")))));
            // set ID in map
            udf.put("id", field.getId());
        }
        return thingType;
    }

    /**
     * Check that the relationship exists inthe corresponding tables
     *
     * @param thingTypeParent parent
     * @param thingTypeChild  child
     */
    protected void checkParentChildRelationship(ThingType thingTypeParent, ThingType thingTypeChild) {
        // asserts
        assertThat(ttMapService.isChild(thingTypeChild), is(Boolean.TRUE));
        assertThat(ttMapService.isParent(thingTypeParent), is(Boolean.TRUE));
        assertThat(ttMapService.getRelationParentChild(thingTypeParent, thingTypeChild), is(notNullValue()));
        checkRelationPath(thingTypeParent, thingTypeChild, Constants.CHILDREN);
        checkRelationPath(thingTypeChild, thingTypeParent, Constants.PARENT);
    }

    /**
     * Check that exists path between thing types
     *
     * @param thingTypeOrigin  origin
     * @param thingTypeDestiny destiny
     * @param pathExpected     path
     */
    protected void checkRelationPath(ThingType thingTypeOrigin, ThingType thingTypeDestiny, String pathExpected) {
        ThingTypePath thingTypePath = ttPathService.getPathByThingTypes(thingTypeOrigin, thingTypeDestiny);
        assertThat(thingTypePath, is(notNullValue()));
        assertThat(thingTypePath.getPath(), is(equalTo(pathExpected)));
    }

    protected ThingType executeTest(String thingTypeCode, Map<String, Object> map, List<Map<String, Object>> udfs, String errorMessageExpected) {
        return executeTest(null, thingTypeCode, map, udfs, errorMessageExpected);
    }

    protected ThingType executeTest(String thingTypeCode, Map<String, Object> map, List<Map<String, Object>> udfs) {
        return executeTest(null, thingTypeCode, map, udfs);
    }

    protected ThingType executeTest(Long idThingType, String thingTypeCode, Map<String, Object> map, List<Map<String, Object>> allStandardUdf) {
        return executeTest(idThingType, thingTypeCode, map, allStandardUdf, null);
    }

    protected ThingType executeTest(Long idThingType, String thingTypeCode, Map<String, Object> map,
                                    List<Map<String, Object>> allStandardUdf, String errorMessageExpected) {
        try {
            Response response = (idThingType != null ?
                    controller.updateThingType(idThingType, map) :
                    controller.insertThingType(map, false));
            assertThat(response, is(notNullValue()));
            if (response.getStatus() >= 200 && response.getStatus() <= 299) {
                return checkThingTypeFields(thingTypeCode, allStandardUdf);
            } else {
                rollback();
                if (errorMessageExpected != null) {
                    Map mapResponse = (Map) response.getEntity();
                    assertThat("ERROR" + response.getEntity(), mapResponse.get("message"), is(equalTo(errorMessageExpected)));
                } else {
                    fail("FAIL TEST");
                }
            }
        } catch (Exception e) {
            logger.error("ERROR", e);
            rollback();
            if (errorMessageExpected != null) {
                assertThat(e.getMessage(), is(equalTo(errorMessageExpected)));
            } else {
                fail("FAIL TEST");
            }
        }
        return null;
    }

    protected void deleteThingType(ThingType thingType) {
        if (thingType != null) {
            initTransaction();
            ThingType thingTypeDB = ThingTypeService.getInstance().get(thingType.getId());
            controller.deleteThingType(thingType.getId());
            ThingTypeService.getThingTypeDAO().delete(thingTypeDB);
        }
    }

    List<Map<String, Object>> removeFields(List<Map<String, Object>> udf, String... fieldNames) {
        List<String> fieldNamelist = Arrays.asList(fieldNames);
        return udf.stream()
                .filter(v -> !fieldNamelist.contains(String.valueOf(v.get("name"))))
                .collect(Collectors.toList());
    }

    List<Map<String, Object>> getAllStandardUdf(boolean timeSeries, boolean includeExpression) {
        List<Map<String, Object>> udfs = new ArrayList<>();
        udfs.add(getMapFieldStandard("abcString", timeSeries, TYPE_TEXT));
        udfs.add(getMapFieldStandard("abcCoordinates", timeSeries, TYPE_LONLATALT));
        udfs.add(getMapFieldStandard("abcXYZ", timeSeries, TYPE_XYZ));
        udfs.add(getMapFieldStandard("abcNumber", timeSeries, TYPE_NUMBER));
        udfs.add(getMapFieldStandard("abcBoolean", timeSeries, TYPE_BOOLEAN));
        udfs.add(getMapFieldStandard("abcImage", timeSeries, TYPE_IMAGE_ID));
        udfs.add(getMapFieldStandard("abcImageURL", timeSeries, TYPE_IMAGE_URL));
        udfs.add(getMapFieldStandard("abcDate", timeSeries, TYPE_DATE));
        udfs.add(getMapFieldStandard("abcUrl", timeSeries, TYPE_URL));
        udfs.add(getMapFieldStandard("abcZPL", false, TYPE_ZPL_SCRIPT));
        udfs.add(getMapFieldStandard("abcTimestamp", timeSeries, TYPE_TIMESTAMP));
        udfs.add(getMapFieldStandard("abcSequence", false, TYPE_SEQUENCE));
        if (includeExpression) {
            udfs.add(getMapFieldStandard("abcExpressionString", false, TYPE_FORMULA, "${abcString}"));
            udfs.add(getMapFieldStandard("abcExpressionCoordinates", false, TYPE_FORMULA, "${abcCoordinates}"));
            udfs.add(getMapFieldStandard("abcExpressionXYZ", false, TYPE_FORMULA, "${abcXYZ}"));
            udfs.add(getMapFieldStandard("abcExpressionNumber", false, TYPE_FORMULA, "${abcNumber}"));
            udfs.add(getMapFieldStandard("abcExpressionBoolean", false, TYPE_FORMULA, "${abcBoolean}"));
            udfs.add(getMapFieldStandard("abcExpressionImage", false, TYPE_FORMULA, "${abcImage}"));
            udfs.add(getMapFieldStandard("abcExpressionImageURL", false, TYPE_FORMULA, "${abcImageURL}"));
            udfs.add(getMapFieldStandard("abcExpressionDate", false, TYPE_FORMULA, "${abcDate}"));
            udfs.add(getMapFieldStandard("abcExpressionUrl", false, TYPE_FORMULA, "${abcUrl}"));
            udfs.add(getMapFieldStandard("abcExpressionZPL", false, TYPE_FORMULA, "${abcZPL}"));
            udfs.add(getMapFieldStandard("abcExpressionTimestamp", false, TYPE_FORMULA, "${abcTimestamp}"));
            udfs.add(getMapFieldStandard("abcExpressionSequence", false, TYPE_FORMULA, "${abcSequence}"));
        }
        return udfs;
    }

    List<Map<String, Object>> getJacketFields() {
        List<Map<String, Object>> udfs = new ArrayList<>();
        udfs.add(getMapFieldStandard("brand", Boolean.FALSE, TYPE_TEXT));
        udfs.add(getMapFieldStandard("category", Boolean.FALSE, TYPE_TEXT));
        udfs.add(getMapFieldStandard("color", Boolean.FALSE, TYPE_TEXT));
        udfs.add(getMapFieldStandard("material", Boolean.FALSE, TYPE_TEXT));
        udfs.add(getMapFieldStandard("price", Boolean.TRUE, "$", "dollar", TYPE_NUMBER));
        udfs.add(getMapFieldStandard("size", Boolean.FALSE, TYPE_TEXT));
        return udfs;
    }

    List<Map<String, Object>> getRFIDFields() {
        List<Map<String, Object>> udfs = new ArrayList<>();
        udfs.add(getMapFieldStandard("lastDetectTime", Boolean.FALSE, "ms", "millisecond", TYPE_TIMESTAMP));
        udfs.add(getMapFieldStandard("lastLocateTime", Boolean.FALSE, "ms", "millisecond", TYPE_TIMESTAMP));
        udfs.add(getMapFieldStandard("location", Boolean.TRUE, TYPE_LONLATALT));
        udfs.add(getMapFieldStandard("locationXYZ", Boolean.TRUE, TYPE_XYZ));
        udfs.add(getMapFieldStandard("zone", Boolean.TRUE, TYPE_ZONE));
        udfs.add(getMapFieldStandard("doorEvent", Boolean.TRUE, TYPE_TEXT));
        udfs.add(getMapFieldStandard("eNode", Boolean.TRUE, TYPE_TEXT));
        udfs.add(getMapFieldStandard("image", Boolean.FALSE, TYPE_IMAGE_ID));
        udfs.add(getMapFieldStandard("logicalReader", Boolean.TRUE, TYPE_LOGICAL_READER));
        udfs.add(getMapFieldStandard("registered", Boolean.TRUE, TYPE_NUMBER));
        udfs.add(getMapFieldStandard("shift", Boolean.TRUE, TYPE_SHIFT));
        udfs.add(getMapFieldStandard("status", Boolean.TRUE, TYPE_TEXT));
        return udfs;
    }

    List<Map<String, Object>> getPantFields() {
        List<Map<String, Object>> udfs = new ArrayList<>();
        udfs.add(getMapFieldStandard("size", Boolean.TRUE, TYPE_TEXT));
        udfs.add(getMapFieldStandard("color", Boolean.TRUE, TYPE_TEXT));
        udfs.add(getMapFieldStandard("material", Boolean.TRUE, TYPE_TEXT));
        udfs.add(getMapFieldStandard("price", Boolean.FALSE, "$", "dollar", TYPE_NUMBER));
        udfs.add(getMapFieldStandard("category", Boolean.TRUE, TYPE_TEXT));
        udfs.add(getMapFieldStandard("brand", Boolean.TRUE, TYPE_TEXT));
        return udfs;
    }

    List<Map<String, Object>> getTagFields() {
        List<Map<String, Object>> udfs = new ArrayList<>();
        udfs.add(getMapFieldStandard("active", Boolean.TRUE, TYPE_BOOLEAN));
        udfs.add(getMapFieldStandard("lastDetectTime", Boolean.TRUE, TYPE_TIMESTAMP));
        udfs.add(getMapFieldStandard("lastLocateTime", Boolean.TRUE, TYPE_TIMESTAMP));
        udfs.add(getMapFieldStandard("location", Boolean.TRUE, TYPE_LONLATALT));
        udfs.add(getMapFieldStandard("locationXYZ", Boolean.TRUE, TYPE_XYZ));
        udfs.add(getMapFieldStandard("zone", Boolean.TRUE, TYPE_ZONE));
        return udfs;
    }

    @Test
    public void getThingTypeGraphByGroupId() throws Exception {
        List<Long> groupList = new ArrayList<>();
        groupList.add(3L);
        DirectedGraph thingTypeGraph = new ThingTypeService().getThingTypeGraphByGroupId(groupList);
        assertNotNull(thingTypeGraph);
    }

    @Test
    public void getThingTypeGraph() throws Exception {
        List<Long> groupList = new ArrayList<>();
        groupList.add(3L);
        DirectedGraph thingTypeGraph = new ThingTypeService().getThingTypeGraph();
        assertNotNull(thingTypeGraph);
    }

    @Test
    public void getSerialFormulaFields() throws Exception {
        List<Map<String, Object>> reportDataEntryMap = new ThingTypeService()
                .getSerialFormulaFields("CustomTT005");
        assertNotNull(reportDataEntryMap);
        assertEquals(2, reportDataEntryMap.size());
        String codes = reportDataEntryMap.get(0).get("propertyName") + "," +
                reportDataEntryMap.get(1).get("propertyName");
        assertTrue(codes.contains("stringField"));
        assertTrue(codes.contains("numberFloatField"));
    }

    @Test
    public void getSerialFormulaFieldsWithThingTypeCodeNotExists() throws Exception {
        try {
            new ThingTypeService().getSerialFormulaFields("CustoLTT005");
            Assert.fail("Exception NotFoundException expected.");
        } catch (NotFoundException ex) {
            assertEquals("thing type with code : CustoLTT005 not found.", ex.getMessage());
        }
    }

    @Test
    public void validateFormulaFields() throws Exception {
        Set<String> thingFieldKeys = new TreeSet<>();
        thingFieldKeys.add("numberFloatField");
        thingFieldKeys.add("stringField");
        thingFieldKeys.add("attributeSequence");
        new ThingTypeService().validateFormulaFields("CustomTT005", thingFieldKeys);
    }

    @Test
    public void validateFormulaFieldsTwo() throws Exception {
        Set<String> thingFieldKeys = new TreeSet<>();
        thingFieldKeys.add("numberFloatField");
        thingFieldKeys.add("stringField");
        thingFieldKeys.add("attributeSequence");
        thingFieldKeys.add("intField");
        thingFieldKeys.add("zone.value.name");
        new ThingTypeService().validateFormulaFields("CustomTT005", thingFieldKeys);
    }

    @Test
    public void validateFormulaFieldsThree() throws Exception {
        Set<String> thingFieldKeys = new TreeSet<>();
        thingFieldKeys.add("numberFloatField");
        thingFieldKeys.add("attributeSequence");
        thingFieldKeys.add("intField");
        thingFieldKeys.add("zone.value.name");
        try {
            new ThingTypeService().validateFormulaFields("CustomTT005", thingFieldKeys);
            Assert.fail("UserException expected");
        } catch (UserException ex) {
            assertEquals("There are missing fields to create the serial number: " +
                    "[stringField] for the type type: CustomTT005", ex.getMessage());
        }
    }

    @Test
    public void validateFormulaFieldsFour() throws Exception {
        Set<String> thingFieldKeys = new TreeSet<>();
        thingFieldKeys.add("numberFloatField");
        thingFieldKeys.add("attributeSequence");
        thingFieldKeys.add("intField");
        thingFieldKeys.add("zone.value.name");
        new ThingTypeService().validateFormulaFields("CustomTT004", thingFieldKeys);
    }

    @Test
    public void getThingTypeListBy() throws Exception {
        List<Long> groupIds = new ArrayList<>();
        groupIds.add(3L);

        List<ThingType> thingTypeList = new ThingTypeService().getByThingTypeAndGroup(3L, groupIds);
        assertNotNull(thingTypeList);
        assertEquals(3, thingTypeList.size());
        assertEquals("default_rfid_thingtype", thingTypeList.get(0).getCode());
        assertEquals("jackets_code", thingTypeList.get(1).getCode());
        assertEquals("pants_code", thingTypeList.get(2).getCode());

        thingTypeList = new ThingTypeService().getByThingTypeAndGroup(5L, groupIds);
        assertNotNull(thingTypeList);
        assertEquals(2, thingTypeList.size());
        assertEquals("default_rfid_thingtype", thingTypeList.get(0).getCode());
        assertEquals("jackets_code", thingTypeList.get(1).getCode());

        thingTypeList = new ThingTypeService().getByThingTypeAndGroup(7L, groupIds);
        assertNotNull(thingTypeList);
        assertEquals(2, thingTypeList.size());
        assertEquals("default_rfid_thingtype", thingTypeList.get(0).getCode());
        assertEquals("pants_code", thingTypeList.get(1).getCode());

        thingTypeList = new ThingTypeService().getByThingTypeAndGroup(8L, groupIds);
        assertNotNull(thingTypeList);
        assertEquals(3, thingTypeList.size());
        assertEquals("shippingorder_code", thingTypeList.get(0).getCode());
        assertEquals("asset_code", thingTypeList.get(1).getCode());
        assertEquals("tag_code", thingTypeList.get(2).getCode());

        thingTypeList = new ThingTypeService().getByThingTypeAndGroup(9L, groupIds);
        assertNotNull(thingTypeList);
        assertEquals(3, thingTypeList.size());
        assertEquals("shippingorder_code", thingTypeList.get(0).getCode());
        assertEquals("asset_code", thingTypeList.get(1).getCode());
        assertEquals("tag_code", thingTypeList.get(2).getCode());

        thingTypeList = new ThingTypeService().getByThingTypeAndGroup(10L, groupIds);
        assertNotNull(thingTypeList);
        assertEquals(3, thingTypeList.size());
        assertEquals("shippingorder_code", thingTypeList.get(0).getCode());
        assertEquals("asset_code", thingTypeList.get(1).getCode());
        assertEquals("tag_code", thingTypeList.get(2).getCode());
    }

    @Test
    public void getThingTypeListByError() throws Exception {
        List<Long> groupIds = new ArrayList<>();
        groupIds.add(3L);

        List<ThingType> thingTypeList = new ThingTypeService().getByThingTypeAndGroup(43L, groupIds);
        assertNull(thingTypeList);

        thingTypeList = new ThingTypeService().getByThingTypeAndGroup(null, groupIds);
        assertNotNull(thingTypeList);
        assertEquals(10, thingTypeList.size());
        assertEquals("default_rfid_thingtype", thingTypeList.get(0).getCode());
        assertEquals("jackets_code", thingTypeList.get(1).getCode());
        assertEquals("pants_code", thingTypeList.get(2).getCode());
        assertEquals("shippingorder_code", thingTypeList.get(3).getCode());
        assertEquals("asset_code", thingTypeList.get(4).getCode());
        assertEquals("tag_code", thingTypeList.get(5).getCode());
        assertEquals("Colour", thingTypeList.get(6).getCode());
        assertEquals("CustomTT002", thingTypeList.get(7).getCode());
        assertEquals("CustomTT003", thingTypeList.get(8).getCode());
        assertEquals("CustomTT004", thingTypeList.get(9).getCode());
    }

    @Test
    public void getThingTypeTreeByIdAndGroups() throws Exception {
        List<Long> groupIds = new ArrayList<>();
        groupIds.add(3L);
        List<Map<String, Object>> thingTypeListOfMap = new ThingTypeService()
                .getThingTypeTreeByIdAndGroups(3L, groupIds);
        assertNotNull(thingTypeListOfMap);
        assertEquals(2, thingTypeListOfMap.size());
        assertEquals("jackets_code", thingTypeListOfMap.get(0).get("thingTypeCode"));
        assertEquals("pants_code", thingTypeListOfMap.get(1).get("thingTypeCode"));
        assertEquals("default_rfid_thingtype", ((List<Map<String, Object>>) thingTypeListOfMap.get(0).get("children")).get(0).get("thingTypeCode"));
        assertEquals("default_rfid_thingtype", ((List<Map<String, Object>>) thingTypeListOfMap.get(1).get("children")).get(0).get("thingTypeCode"));
    }

    @Test
    public void getThingTypeTreeByIdAndGroupsCase2() throws Exception {
        List<Long> groupIds = new ArrayList<>();
        groupIds.add(3L);
        List<Map<String, Object>> thingTypeListOfMap = new ThingTypeService()
                .getThingTypeTreeByIdAndGroups(10L, groupIds);
        assertNotNull(thingTypeListOfMap);
        assertEquals(1, thingTypeListOfMap.size());
        assertEquals("shippingorder_code", thingTypeListOfMap.get(0).get("thingTypeCode"));
        LinkedList<Map<String, Object>> child = (LinkedList<Map<String, Object>>) thingTypeListOfMap.get(0).get("children");
        assertEquals("asset_code", child.get(0).get("thingTypeCode"));
        LinkedList<Map<String, Object>> secondChild = (LinkedList<Map<String, Object>>) child.get(0).get("children");
        assertEquals("tag_code", secondChild.get(0).get("thingTypeCode"));
    }

    @Test
    public void getThingTypeTreeByIdAndGroupsCase3() throws Exception {
        List<Long> groupIds = new ArrayList<>();
        groupIds.add(3L);
        ThingType customTT005 = ThingTypeService.getInstance().getByCode("CustomTT005");
        List<Map<String, Object>> thingTypeListOfMap = new ThingTypeService()
                .getThingTypeTreeByIdAndGroups(customTT005.getId(), groupIds);
        assertNotNull(thingTypeListOfMap);
        assertEquals(1, thingTypeListOfMap.size());
        assertEquals(customTT005.publicMapTreeView(true), thingTypeListOfMap.get(0));
    }
}