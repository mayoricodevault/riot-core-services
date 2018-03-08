package com.tierconnect.riot.iot.services.thing;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.tierconnect.riot.iot.services.ThingService;
import net.minidev.json.JSONArray;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Created by vealaro on 8/1/17.
 */
public class ThingServiceUpdateTest extends ThingServiceTest {

    private static final Logger LOGGER = Logger.getLogger(ThingServiceUpdateTest.class);
    static final String thingTypeCodeRFID = "default_rfid_thingtype";
    static final String thingTypeCodeColor = "Colour";
    static final String thingTypeCodeCustomTT002 = "CustomTT002";
    static final String thingTypeCodeCustomTT003 = "CustomTT003";
    static final String thingTypeCodeCustomTT006 = "CustomTT006";
    static final String thingTypeCodeSO = "shippingorder_code";
    static final String thingTypeCodeAsset = "asset_code";
    static final String thingTypeCodeTag = "tag_code";

    protected Map<String, Map<String, Object>> mapUpdates = new LinkedHashMap<>();
    protected Map<String, Object> mapResponseExpected = new LinkedHashMap<>();

    @SuppressWarnings("unchecked")
    protected Map<String, Object> executeCreateAndUpdateThing(Map<String, Map<String, Object>> mapUpdates,
                                                              Long thingID) throws Exception {
//        execution update
        Map<String, Object> lastThing = Collections.emptyMap();
        for (Map.Entry<String, Map<String, Object>> thingMap : mapUpdates.entrySet()) {
            lastThing = compareEqualityResult(thingMap.getKey(), thingMap.getValue(), thingID);
        }
        // clear objects
        mapUpdates.clear();
        return lastThing;
    }

    protected void validateResultExpected(Map<String, Object> lastThing, Map<String, Object> mapResponseExpected) throws Exception {
        Configuration configuration = Configuration.builder().options(Option.SUPPRESS_EXCEPTIONS).build();
        String jsonString = mapToJson(lastThing);
        if (mapResponseExpected != null && !mapResponseExpected.isEmpty()) {
            for (Map.Entry<String, Object> expected : mapResponseExpected.entrySet()) {
                Object valueObject = JsonPath.using(configuration).parse(jsonString).read(expected.getKey());
                if (expected.getValue() != null) {
                    assertThat("does not exist : " + expected.getKey(), valueObject, is(notNullValue()));
                }
                if (expected.getKey().endsWith("thingTypeFieldId")) {
                    assertThat("thingTypeFieldId is null", valueObject, is(notNullValue()));
                } else {
                    assertThat("check path: " + expected.getKey(), valueObject, is(equalTo(expected.getValue())));
                }
            }
            // clear objects
            mapResponseExpected.clear();
        }
    }

    protected JSONArray array(Object... objects) {
        JSONArray jsonArray = new JSONArray();
        for (Object o : objects) {
            jsonArray.appendElement(o);
        }
        return jsonArray;
    }

    protected void deleteThing(Map<String, Object> map) {
        initTransaction();
        // delete thing
        ThingService.getInstance().secureDelete((Long) map.get("_id"), true, true, true, true, false);
        waitForThingDeletion();
    }
}