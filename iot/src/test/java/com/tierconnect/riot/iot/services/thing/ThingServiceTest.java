package com.tierconnect.riot.iot.services.thing;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.tierconnect.riot.appcore.core.BaseTestIOT;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.iot.controllers.ThingTypeController;
import com.tierconnect.riot.iot.dao.mongo.ThingMongoDAO;
import com.tierconnect.riot.iot.entities.BackgroundProcessDetailLog;
import com.tierconnect.riot.iot.entities.Thing;
import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.entities.ThingTypeField;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


/**
 * Created by ybarriga on 5/17/16.
 * Modified by julio.rocha on 31-07-17.
 */
public class ThingServiceTest extends BaseTestIOT {

    private static Logger logger = Logger.getLogger(ThingServiceTest.class);

    private static boolean configured = false;
    protected final String BASE_PATH = "iot/services/thing/";
    protected List<String> expectedTimesToReplace = Collections.unmodifiableList(
            Arrays.asList("time", "zoneGroupTime", "facilityMapTime", "zoneTypeTime"));

    @Override
    protected void previousConfiguration() throws Exception {
        if (!configured) {
            logger.info("Initializing Thing Test configuration");
            super.previousConfiguration();
            createThingTypes();
            configured = true;
            logger.info("Finishing Thing Test configuration");
        }
    }

    public void createThingTypes() throws Exception {
        initCaches();
        try {
            String[] thingSchemas = {"thingTypes/001.json", "thingTypes/002.json", "thingTypes/003.json",
                    "thingTypes/004.json", "thingTypes/005.json", "thingTypes/006.json"};
            ThingTypeController ttc = new ThingTypeController();
            for (String ts : thingSchemas) {
                Response response = ttc.insertThingType(jsonToMap(BASE_PATH + ts), false);
                assertThat(201, is(equalTo(response.getStatus())));
            }
            super.endTransaction();
            super.initTransaction();
            super.initShiro();//have to restart security, because thingTypes has been added
            initCaches();
        } catch (UserException e) {
            assertThat(e.getMessage(), allOf(containsString("Thing Type Code '[CustomTT"),
                    endsWith("]' already exists.")));
        }
    }

    private void initCaches() {
        GroupService.getInstance().loadAllInCache();
        ThingTypeService.getInstance().loadAllInCache();
        DataTypeService.getInstance().loadAllInCache();
    }

    private Map<String, Object> executeCreation(String testCase, Boolean useDefaultValues, Date transactionDate) throws Exception {
        Map<String, Object> thingMap = jsonToMap(testCase);
        transactionDate = (transactionDate != null) ? transactionDate : new Date();
        return executeCreation(thingMap, useDefaultValues, transactionDate);
    }

    private Map<String, Object> executeUpdate(String testCase, Long thingID, Date transactionDate) throws Exception {
        Map<String, Object> thingMap = jsonToMap(testCase);
        transactionDate = (transactionDate != null) ? transactionDate : new Date();
        return executeUpdate(thingMap, thingID, transactionDate);
    }

    /**
     * @param thingMap
     * @param useDefaultValues
     * @param transactionDate
     * @return
     * @throws Exception
     */
    private Map<String, Object> executeCreation(Map<String, Object> thingMap, Boolean useDefaultValues, Date transactionDate) throws Exception {
        return ThingsService.getInstance().create(
                new Stack<>(), (String) thingMap.get("thingTypeCode"), (String) thingMap.get("group"),
                (String) thingMap.get("name"), (String) thingMap.get("serialNumber"),
                (Map<String, Object>) thingMap.get("parent"), (Map<String, Object>) thingMap.get("udfs"),
                thingMap.get("children"), thingMap.get("childrenUdf"), true, true, transactionDate,
                false, true, useDefaultValues, null, null, true, super.currentUser);
    }

    /**
     * @param thingMap
     * @param idThing
     * @param transactionDate
     * @return
     */
    private Map<String, Object> executeUpdate(Map<String, Object> thingMap, Long idThing, Date transactionDate) {
        return ThingService.getInstance().update(new Stack<Long>()
                , idThing
                , (String) thingMap.get("thingTypeCode")
                , (String) thingMap.get("group")
                , (String) thingMap.get("name")
                , (String) thingMap.get("serialNumber")
                , (Map<String, Object>) thingMap.get("parent")
                , (Map<String, Object>) thingMap.get("udfs")
                , thingMap.get("children")
                , thingMap.get("childrenUdf")
                , true, true, transactionDate, false, super.userRoot, true);
    }

    protected Long getIdFromResponse(Map<String, Object> response) {
        return new Long(getPropertyFromThingResponse(response, "id").toString());
    }

    protected Object getPropertyFromThingResponse(Map<String, Object> response, String key) {
        return ((Map<String, Object>) response.get("thing")).get(key);
    }

    protected void replaceAllTimeFieldsInExpected(Date transactionDate, Map<String, Object> expected) {
        for (String k : expected.keySet()) {
            if (expectedTimesToReplace.contains(k)) {
                expected.put(k, getIsoDateFormatWithoutZone(transactionDate));
            } else if (expected.get(k) instanceof Map) {
                replaceAllTimeFieldsInExpected(transactionDate, (Map<String, Object>) expected.get(k));
            }
        }
    }

    protected void replaceAllFieldsOfTypeTimeInResult(BasicDBObject thing) {
        for (String k : thing.keySet()) {
            if (thing.get(k) instanceof Date) {
                thing.put(k, getIsoDateFormatWithoutZone(thing.getDate(k)));
            } else if (thing.get(k) instanceof BasicDBObject) {
                replaceAllFieldsOfTypeTimeInResult((BasicDBObject) thing.get(k));
            } else if (thing.get(k) instanceof BasicDBList) {
                for (Object value : (BasicDBList) thing.get(k)) {
                    replaceAllFieldsOfTypeTimeInResult((BasicDBObject) value);
                }
            }
        }
    }

    protected void replaceAllThingTypeFieldIds(Long id, Map<String, Object> expected) {
        Thing thing = ThingService.getInstance().get(id);
        ThingType thingType = thing.getThingType();
        expected.put("thingTypeId", thingType.getId());
        Set<ThingTypeField> thingTypeFields = thingType.getThingTypeFields();
        for (ThingTypeField ttf : thingTypeFields) {
            replaceThingTypeFieldId(expected, ttf.getId(), ttf.getName());
        }
    }

    protected void replaceThingTypeFieldId(Map<String, Object> expected, Long id, String fieldName) {
        for (String k : expected.keySet()) {
            if (k.equals(fieldName)) {
                Map<String, Object> udf = (Map<String, Object>) expected.get(k);
                udf.put("thingTypeFieldId", id);
            } else if (expected.get(k) instanceof Map) {
                replaceThingTypeFieldId((Map<String, Object>) expected.get(k), id, fieldName);
            }
        }
    }

    protected Map<String, Object> getExpectedResult(String testCaseResponse, Map<String, Object> response,
                                                    Date transactionDate) throws Exception {
        Date modifiedTime = (Date) getPropertyFromThingResponse(response, "modifiedTime");
        Date createdTime = (Date) getPropertyFromThingResponse(response, "createdTime");
        Long id = getIdFromResponse(response);
        Map<String, Object> expected = jsonToMap(testCaseResponse);
        expected.put("_id", id);
        expected.put("modifiedTime", getIsoDateFormatWithoutZone(modifiedTime));
        expected.put("createdTime", getIsoDateFormatWithoutZone(createdTime));
        replaceAllTimeFieldsInExpected(transactionDate, expected);
        replaceAllThingTypeFieldIds(id, expected);
        return expected;
    }

    public Map<String, Object> getCurrentThing(Long id) throws Exception {
        BasicDBObject thing = (BasicDBObject) ThingMongoDAO.getInstance().getThing(id);
        replaceAllFieldsOfTypeTimeInResult(thing);
        return thing.toMap();
    }

    protected Map<String, Object> executeAndFormatExpectedResult(Date transactionDate, String testCase, String testCaseResponse,
                                                                 Boolean useDefaultValues) throws Exception {
        return executeAndFormatExpectedResult(transactionDate, testCase, testCaseResponse, useDefaultValues, null);
    }

    protected Map<String, Object> executeAndFormatExpectedResult(Date transactionDate, String testCase, String testCaseResponse,
                                                                 Boolean useDefaultValues,
                                                                 Map<String, Map<String, Object>> attributesToReplace) throws Exception {
        Map<String, Object> executionResponse = executeCreation(BASE_PATH + testCase, useDefaultValues, transactionDate);
        return formatExpectedResult(testCaseResponse, executionResponse, transactionDate, attributesToReplace);
    }

    protected Map<String, Object> executeAndFormatExpectedResult(Date transactionDate, String testCase, String testCaseResponse,
                                                                 Long thingID,
                                                                 Map<String, Map<String, Object>> attributesToReplace) throws Exception {
        transactionDate = (transactionDate != null) ? transactionDate : new Date();
        Map<String, Object> executionResponse = executeUpdate(BASE_PATH + testCase, thingID, transactionDate);
        return formatExpectedResult(testCaseResponse, executionResponse, transactionDate, attributesToReplace);
    }

    /**
     * @param transactionDate
     * @param mapTestCase
     * @param useDefaultValues
     * @param attributesToReplace
     * @return
     * @throws Exception
     */
    protected Map<String, Object> executionService(Date transactionDate, Map<String, Object> mapTestCase,
                                                   Boolean useDefaultValues,
                                                   Map<String, Map<String, Object>> attributesToReplace) throws Exception {
        return executeCreation(mapTestCase, useDefaultValues, transactionDate);
    }

    /**
     * @param transactionDate
     * @param mapTestCase
     * @param thingID
     * @param attributesToReplace
     * @return
     */
    protected Map<String, Object> executionService(Date transactionDate, Map<String, Object> mapTestCase,
                                                   Long thingID, Map<String, Map<String, Object>> attributesToReplace) {
        return executeUpdate(mapTestCase, thingID, transactionDate);
    }

    private Map<String, Object> formatExpectedResult(String testCaseResponse, Map<String, Object> executionResponse, Date transactionDate,
                                                     Map<String, Map<String, Object>> attributesToReplace) throws Exception {
        Map<String, Object> response = getExpectedResult(BASE_PATH + testCaseResponse, executionResponse, transactionDate);
        if (attributesToReplace != null && !attributesToReplace.isEmpty()) {
            final Date time = transactionDate;
            response.keySet().stream()
                    .filter(k -> attributesToReplace.containsKey(k))
                    .forEach(k -> {
                                Map<String, Object> udfAttribute = (Map<String, Object>) response.get(k);
                                response.put(k, getFormatUDFAttribute(time, (Long) udfAttribute.get("thingTypeFieldId"), attributesToReplace.get(k)));
                            }
                    );
        }
        return response;
    }

    protected Map<String, Object> getFormatUDFAttribute(Date time, Long thingTypeFieldId, Map<String, Object> udfValue) {
        Map<String, Object> udfAttribute = new LinkedHashMap<>();
        udfAttribute.put("thingTypeFieldId", thingTypeFieldId);
        udfAttribute.put("time", getIsoDateFormatWithoutZone(time));
        udfAttribute.put("value", udfValue);
        return udfAttribute;
    }

    protected Map<String, Object> compareEqualityResult(String transactionDateISO, String testCase, String testCaseResponse) throws Exception {
        return compareEqualityResult(transactionDateISO, testCase, testCaseResponse, null);
    }

    protected Map<String, Object> compareEqualityResult(String transactionDateISO, String testCase, String testCaseResponse, Boolean useDefaultValues) throws Exception {
        return compareEqualityResult(transactionDateISO, testCase, testCaseResponse, useDefaultValues, null);
    }

    protected Map<String, Object> compareEqualityResult(String transactionDateISO, String testCase, String testCaseResponse, Boolean useDefaultValues,
                                                        Map<String, Map<String, Object>> attributesToReplace) throws Exception {
        return compareEqualityResult(transactionDateISO, testCase, testCaseResponse, useDefaultValues, null, attributesToReplace);
    }

    protected Map<String, Object> compareEqualityResult(String transactionDateISO, Long thingID, String testCase, String testCaseResponse) throws Exception {
        return compareEqualityResult(transactionDateISO, thingID, testCase, testCaseResponse, null);
    }

    protected Map<String, Object> compareEqualityResult(String transactionDateISO, Long thingID, String testCase, String testCaseResponse,
                                                        Map<String, Map<String, Object>> attributesToReplace) throws Exception {
        return compareEqualityResult(transactionDateISO, testCase, testCaseResponse, null, thingID, attributesToReplace);
    }

    private Map<String, Object> compareEqualityResult(String transactionDateISO, String testCase, String testCaseResponse,
                                                      Boolean useDefaultValues, Long thingID,
                                                      Map<String, Map<String, Object>> attributesToReplace) throws Exception {
        Map<String, Object> expectedResult;
        Date transactionDate = getDate(transactionDateISO);
        if (thingID == null) { // creation
            expectedResult = executeAndFormatExpectedResult(transactionDate, testCase, testCaseResponse,
                    useDefaultValues, attributesToReplace);
        } else {               // update
            expectedResult = executeAndFormatExpectedResult(transactionDate, testCase, testCaseResponse,
                    thingID, attributesToReplace);
        }
        // expected
        String expectedResultJsonString = mapToJson(expectedResult);

        // current value
        String currentResultJsonString = mapToJson(getCurrentThing((Long) expectedResult.get("_id")));

        assertThat(currentResultJsonString, is(equalTo(expectedResultJsonString)));
        return expectedResult;
    }

    protected Map<String, Object> compareEqualityResult(String tsISO, Map<String, Object> mapTestCase,
                                                        Boolean useDefaultValues) throws Exception {
        return compareEqualityResult(tsISO, mapTestCase, useDefaultValues, null, null);
    }

    protected Map<String, Object> compareEqualityResult(String tsISO, Map<String, Object> mapTestCase,
                                                        Boolean useDefaultValues,
                                                        Map<String, Map<String, Object>> attributesToReplace) throws Exception {
        return compareEqualityResult(tsISO, mapTestCase, useDefaultValues, null, attributesToReplace);
    }

    protected Map<String, Object> compareEqualityResult(String tsISO, Map<String, Object> mapTestCase,
                                                        Long thingID,
                                                        Map<String, Map<String, Object>> attributesToReplace) throws Exception {
        return compareEqualityResult(tsISO, mapTestCase, null, thingID, attributesToReplace);
    }

    protected Map<String, Object> compareEqualityResult(String tsISO, Map<String, Object> mapTestCase,
                                                        Long thingID) throws Exception {
        return compareEqualityResult(tsISO, mapTestCase, null, thingID, null);
    }

    private Map<String, Object> compareEqualityResult(String tsISO, Map<String, Object> mapTestCase,
                                                      Boolean useDefaultValues, Long thingID,
                                                      Map<String, Map<String, Object>> attributesToReplace) throws Exception {
        Map<String, Object> expectedResult;
        Date transactionDate = getDate(tsISO);
        if (thingID == null) { // creation
            expectedResult = executionService(transactionDate, mapTestCase, useDefaultValues, attributesToReplace);
        } else {               // update
            expectedResult = executionService(transactionDate, mapTestCase, thingID, attributesToReplace);
        }
        // valida non UDF and time
        Object thingIDObject = ((Map) expectedResult.get("thing")).get("id");
        Map<String, Object> currentResult = getCurrentThing((Long) thingIDObject);
        nonUDFExpectedResult(currentResult, tsISO);
        return currentResult;
    }

    private void nonUDFExpectedResult(Map<String, Object> mapThing, String isoTransactionDate) {
        assertThat(mapThing, hasKey("_id"));
        assertThat(mapThing, hasKey("groupTypeId"));
        assertThat(mapThing, hasKey("groupTypeName"));
        assertThat(mapThing, hasKey("groupTypeCode"));
        assertThat(mapThing, hasKey("groupId"));
        assertThat(mapThing, hasKey("groupCode"));
        assertThat(mapThing, hasKey("groupName"));
        assertThat(mapThing, hasKey("thingTypeId"));
        assertThat(mapThing, hasKey("thingTypeCode"));
        assertThat(mapThing, hasKey("thingTypeName"));
        assertThat(mapThing, hasKey("name"));
        assertThat(mapThing, hasKey("serialNumber"));
        assertThat(mapThing, hasKey("modifiedTime"));
        assertThat(mapThing, hasKey("createdTime"));
        assertThat(mapThing, hasKey("time"));
        assertThat(mapThing.get("time"), is(equalTo(isoTransactionDate)));
    }


    protected Map<String, Object> createMap(String thingTypeCode, String serialName, Document udfMap, List<Document> childrenUDF, List<Document> children) {
        return createMap(">mojix>SM", serialName, serialName, thingTypeCode, udfMap, null, childrenUDF, children);
    }

    protected Map<String, Object> createMap(String thingTypeCode, String serialName, Document udfMap) {
        return createMap(">mojix>SM", serialName, serialName, thingTypeCode, udfMap, null, null, null);
    }

    protected Map<String, Object> createMap(String thingTypeCode, String serialName, Document udfMap,
                                            Document parent) {
        return createMap(">mojix>SM", serialName, serialName, thingTypeCode, udfMap, parent,
                Collections.<Document>emptyList(), Collections.<Document>emptyList());
    }

    protected Map<String, Object> createMap(String thingTypeCode, String serialName, String name, Document udfMap) {
        return createMap(">mojix>SM", serialName, name, thingTypeCode, udfMap, null,
                Collections.<Document>emptyList(), Collections.<Document>emptyList());
    }

    protected Map<String, Object> createMap(String group, String serialName, String name, String thingTypeCode,
                                            Document udfMap, Document parent, List<Document> childrenUDF,
                                            List<Document> children) {
        Map<String, Object> document = new HashMap<String, Object>();
        document.put("group", group);
        document.put("serialNumber", serialName);
        document.put("name", name);
        document.put("thingTypeCode", thingTypeCode);
        if (udfMap != null && !udfMap.isEmpty()) {
            Map<String, Object> udfDocument = new HashMap<String, Object>();
            for (Map.Entry<String, Object> udf : udfMap.entrySet()) {
                udfDocument.put(udf.getKey(), Collections.<String, Object>singletonMap("value", udf.getValue()));
            }
            document.put("udfs", udfDocument);
        }
        if (parent != null) {
            document.put("parent", parent);
        }
        if (childrenUDF != null) {
            document.put("childrenUdf", childrenUDF);
        }
        if (children != null) {
            document.put("children", children);
        }
        return document;
    }

    @Test
    @Ignore
    public void deleteThingTypes() throws Exception {
        String[] serials = {"CustomTT001", "CustomTT002", "CustomTT003", "CustomTT004", "CustomTT005", "CustomTT006"};
        ThingTypeController ttc = new ThingTypeController();
        for (String s : serials) {
            ThingType thingType = ThingTypeService.getInstance().getByCode(s);
            if (thingType != null) {
                ttc.deleteThingType(thingType.getId());
                ThingTypeService.getThingTypeDAO().delete(thingType);
            }
        }
    }

    /**
     * waits until compensation algorithm is completed
     */
    protected void waitForThingDeletion() {
        List<BackgroundProcessDetailLog> lstIdsToDeleteObj;
        do{
            super.initTransaction();
            logger.info("Waiting for complete deletion");
            lstIdsToDeleteObj = BackgroundProcessDetailLogService.getInstance().getThingsPendingToDelete();
            super.endTransaction();
        } while (lstIdsToDeleteObj != null && lstIdsToDeleteObj.size() > 0);
    }
}
