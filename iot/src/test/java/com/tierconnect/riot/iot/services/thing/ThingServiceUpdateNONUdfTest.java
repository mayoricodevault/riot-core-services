package com.tierconnect.riot.iot.services.thing;

import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * Created by vealaro on 8/8/17.
 */
public class ThingServiceUpdateNONUdfTest extends ThingServiceUpdateTest {

    private static final Logger LOGGER = Logger.getLogger(ThingServiceUpdateNONUdfTest.class);

    /**
     * <pre>
     * THING TYPE : RFID
     * 1) create thing without udf's
     * 2) update name
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void createSerialNameCT001() throws Exception {
        String serialNumber = "UPDATENONDUF0001";
        // create thing RFID
        Map<String, Object> map = compareEqualityResult("2017-08-01T08:00Z",
                createMap(thingTypeCodeRFID, serialNumber, "First Name", null), Boolean.FALSE);

        // validate creation
        mapResponseExpected.put("name", "First Name");
        validateResultExpected(map, mapResponseExpected);

        // update thing RFID
        mapUpdates.put("2017-08-01T09:00Z", createMap(thingTypeCodeRFID, serialNumber, "Second Name", null));
        map = executeCreateAndUpdateThing(mapUpdates, (Long) map.get("_id"));

        // validate update
        mapResponseExpected.put("name", "Second Name");
        validateResultExpected(map, mapResponseExpected);

        // delete thing
        deleteThing(map);
    }

    @Test
    public void createSerialNameCT002() throws Exception {
        String serialNumber = "UPDATENONDUF_0002";

        Map<String, Object> map = compareEqualityResult("2017-08-01T08:00Z",
                createMap(thingTypeCodeRFID, serialNumber, null), Boolean.FALSE);

        // validate creation
        mapResponseExpected.put("name", serialNumber);
        mapResponseExpected.put("serialNumber", serialNumber);
        validateResultExpected(map, mapResponseExpected);

        // update thing RFID
        mapUpdates.put("2017-08-01T09:00Z", createMap(thingTypeCodeRFID, "OTHER_SERIAL", "Second Name", null));
        map = executeCreateAndUpdateThing(mapUpdates, (Long) map.get("_id"));

        // validate update
        mapResponseExpected.put("name", "Second Name");
        mapResponseExpected.put("serialNumber", serialNumber);
        validateResultExpected(map, mapResponseExpected);

        deleteThing(map);
    }

    @Test
    public void createSerialNameCT003() throws Exception {
        String serialNumber = "UPDATENONDUF0003A";

        Map<String, Object> map = compareEqualityResult("2017-08-01T08:00Z",
                createMap(thingTypeCodeCustomTT006, serialNumber, null, null,
                        Collections.<Document>emptyList()), Boolean.TRUE);

        mapResponseExpected.put("name", serialNumber);
        mapResponseExpected.put("serialNumber", serialNumber);
        mapResponseExpected.put("expressionUDFName.value", serialNumber);
        mapResponseExpected.put("expressionUDFSerial.value", serialNumber);
        mapResponseExpected.put("expressionUDFTTName.value", thingTypeCodeCustomTT006);
        mapResponseExpected.put("expressionUDFTTCode.value", thingTypeCodeCustomTT006);
        mapResponseExpected.put("expressionUDFGroupTypeName.value", "Store");
        mapResponseExpected.put("expressionUDFGroupTypeCode.value", "Store");
        mapResponseExpected.put("expressionUDFGroupName.value", "Santa Monica");
        mapResponseExpected.put("expressionUDFGroupCode.value", "SM");
        validateResultExpected(map, mapResponseExpected);

        // delete thing
        deleteThing(map);
    }

    /**
     * serial with character special without underscore(_)
     */
    @Test
    public void createSerialNameCT004() throws Exception {
        String serialNumber = "UPDATENONDUF_0004_/*?";
        try {
            compareEqualityResult("2017-08-01T08:00Z",
                    createMap(thingTypeCodeRFID, serialNumber, null), Boolean.FALSE);
        } catch (UserException e) {
            assertThat("Error with" + serialNumber, e.getMessage(),
                    is(equalTo("Error in validation Serial Number [" + serialNumber
                            + "], Serial has invalid characters, only alphanumeric characters are allowed.")));
        }
    }
}
