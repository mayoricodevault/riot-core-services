package com.tierconnect.riot.iot.services.thing;

import org.apache.log4j.Logger;
import org.bson.Document;
import org.junit.Test;

import java.util.Map;

/**
 * Created by vealaro on 8/8/17.
 */
public class ThingServiceUpdateStandardTest extends ThingServiceUpdateTest {

    private static final Logger LOGGER = Logger.getLogger(ThingServiceUpdateStandardTest.class);

    /**
     * <pre>
     * THING TYPE : RFID
     * 1) create thing without udf's
     * 2) Update data with different values
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void updateStandardString0001() throws Exception {
        String serialNumber = "UPDATESTANDARD0001";
        // create
        Map<String, Object> map = compareEqualityResult(
                "2017-08-01T08:00Z",
                createMap(thingTypeCodeRFID, serialNumber, null),
                Boolean.FALSE);

        // update
        LOGGER.info("init update thing= " + map.get("_id"));
        mapUpdates.put("2017-08-01T09:00Z", createMap(thingTypeCodeRFID, serialNumber, new Document("status", "ACTIVE")));
        mapUpdates.put("2017-08-01T10:00Z", createMap(thingTypeCodeRFID, serialNumber, new Document("status", "INACTIVE")));
        map = executeCreateAndUpdateThing(mapUpdates, (Long) map.get("_id"));

        // validate result
        mapResponseExpected.put("status.thingTypeFieldId", -1);
        mapResponseExpected.put("status.value", "INACTIVE");
        mapResponseExpected.put("status.time", "2017-08-01T10:00Z");
        validateResultExpected(map, mapResponseExpected);

        // delete thing
        deleteThing(map);
    }

    /**
     * <pre>
     * THING TYPE : RFID
     * 1) create thing with udf
     * 2) Update the data with the same value
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void updateStandardString0002() throws Exception {
        String serialNumber = "UPDATESTANDARD0002";
        // create
        Map<String, Object> map = compareEqualityResult("2017-08-01T08:00Z",
                createMap(thingTypeCodeRFID, serialNumber, new Document("status", "ACTIVE")), Boolean.FALSE);

        // udpate
        LOGGER.info("init update thing= " + map.get("_id"));
        mapUpdates.put("2017-08-01T10:00Z", createMap(thingTypeCodeRFID, serialNumber, new Document("status", "ACTIVE")));
        executeCreateAndUpdateThing(mapUpdates, (Long) map.get("_id"));

        // validate result
        mapResponseExpected.put("status.thingTypeFieldId", -1);
        mapResponseExpected.put("status.value", "ACTIVE");
        mapResponseExpected.put("status.time", "2017-08-01T08:00Z");
        validateResultExpected(map, mapResponseExpected);

        // delete thing
        deleteThing(map);
    }
}
