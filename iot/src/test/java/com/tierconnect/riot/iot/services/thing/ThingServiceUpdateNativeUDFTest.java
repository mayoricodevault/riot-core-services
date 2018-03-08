package com.tierconnect.riot.iot.services.thing;

import org.apache.log4j.Logger;
import org.bson.Document;
import org.junit.Test;

import java.util.Map;

/**
 * Created by vealaro on 8/8/17.
 */
public class ThingServiceUpdateNativeUDFTest extends ThingServiceUpdateTest {

    private static final Logger LOGGER = Logger.getLogger(ThingServiceUpdateNativeUDFTest.class);

    /**
     * <pre>
     * THING TYPE : RFID
     * 1) create thing without udf's
     * 2) update native udf (Zone)
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void updateNativeUDFZone0001() throws Exception {
        String serialNumber = "UPDATEZONE001";
        // create
        Map<String, Object> map = compareEqualityResult("2017-08-01T08:00Z",
                createMap(thingTypeCodeRFID, serialNumber, null), Boolean.FALSE);

        // update
        LOGGER.info("init update thing= " + map.get("_id"));
        mapUpdates.put("2017-08-01T09:00Z", createMap(thingTypeCodeRFID, serialNumber, new Document("zone", "Enance")));
        map = executeCreateAndUpdateThing(mapUpdates, (Long) map.get("_id"));

        // validate result
        mapResponseExpected.put("lastDetectTime.time", "2017-08-01T09:00Z");
        mapResponseExpected.put("lastDetectTime.value", 1501578000000L);
        mapResponseExpected.put("lastLocateTime.time", "2017-08-01T09:00Z");
        mapResponseExpected.put("lastLocateTime.value", 1501578000000L);
        mapResponseExpected.put("location.time", "2017-08-01T09:00Z");
        mapResponseExpected.put("location.value", "-118.44395660357625;34.048117009863525;0.0");
        mapResponseExpected.put("locationXYZ.time", "2017-08-01T09:00Z");
        mapResponseExpected.put("locationXYZ.value", "6.6;7.3;0.0");
        mapResponseExpected.put("zone.time", "2017-08-01T09:00Z");
        mapResponseExpected.put("zone.value.code", "Enance");
        mapResponseExpected.put("zone.value.name", "Entrance");
        mapResponseExpected.put("zone.value.facilityMap", "Map Store Santa Monica");
        mapResponseExpected.put("zone.value.zoneGroup", "Off-Site");
        mapResponseExpected.put("zone.value.zoneType", "DefaultZoneType2");
        mapResponseExpected.put("zone.value.facilityMapTime", "2017-08-01T09:00Z");
        mapResponseExpected.put("zone.value.zoneGroupTime", "2017-08-01T09:00Z");
        mapResponseExpected.put("zone.value.zoneTypeTime", "2017-08-01T09:00Z");
        validateResultExpected(map, mapResponseExpected);

        // delete thing
        deleteThing(map);
    }

    /**
     * <pre>
     * THING TYPE : RFID
     * 1) create thing with udf zone
     * 2) update data with the same value
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void updateNativeUDFZone0002() throws Exception {
        String serialNumber = "UPDATEZONE002";
        // create thing
        Map<String, Object> map = compareEqualityResult("2017-08-01T08:00Z",
                createMap(thingTypeCodeRFID, serialNumber, new Document("zone", "Enance")), Boolean.FALSE);

        // update thing
        LOGGER.info("init update thing= " + map.get("_id"));
        mapUpdates.put("2017-08-01T09:00Z", createMap(thingTypeCodeRFID, serialNumber, new Document("zone", "Enance")));
        map = executeCreateAndUpdateThing(mapUpdates, (Long) map.get("_id"));

        // validate result
        mapResponseExpected.put("lastDetectTime.time", "2017-08-01T09:00Z");
        mapResponseExpected.put("lastDetectTime.value", 1501578000000L);
        mapResponseExpected.put("lastLocateTime.time", "2017-08-01T09:00Z");
        mapResponseExpected.put("lastLocateTime.value", 1501578000000L);
        mapResponseExpected.put("location.time", "2017-08-01T08:00Z");
        mapResponseExpected.put("location.value", "-118.44395660357625;34.048117009863525;0.0");
        mapResponseExpected.put("locationXYZ.time", "2017-08-01T08:00Z");
        mapResponseExpected.put("locationXYZ.value", "6.6;7.3;0.0");
        mapResponseExpected.put("zone.time", "2017-08-01T08:00Z");
        mapResponseExpected.put("zone.value.code", "Enance");
        mapResponseExpected.put("zone.value.name", "Entrance");
        mapResponseExpected.put("zone.value.facilityMap", "Map Store Santa Monica");
        mapResponseExpected.put("zone.value.zoneGroup", "Off-Site");
        mapResponseExpected.put("zone.value.zoneType", "DefaultZoneType2");
        mapResponseExpected.put("zone.value.facilityMapTime", "2017-08-01T08:00Z");
        mapResponseExpected.put("zone.value.zoneGroupTime", "2017-08-01T08:00Z");
        mapResponseExpected.put("zone.value.zoneTypeTime", "2017-08-01T08:00Z");
        validateResultExpected(map, mapResponseExpected);

        // delete thing
        deleteThing(map);
    }

    /**
     * <pre>
     * THING TYPE : RFID
     * 1) create thing with udf zone
     * 2) update data with the zone
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void updateNativeUDFZone0003() throws Exception {
        String serialNumber = "UPDATEZONE003";
        // create thing
        Map<String, Object> map = compareEqualityResult("2017-08-01T08:00Z",
                createMap(thingTypeCodeRFID, serialNumber, new Document("zone", "Enance")), Boolean.FALSE);

        // update thing
        LOGGER.info("init update thing= " + map.get("_id"));
        mapUpdates.put("2017-08-01T09:00Z", createMap(thingTypeCodeRFID, serialNumber, new Document("zone", "Stroom")));
        map = executeCreateAndUpdateThing(mapUpdates, (Long) map.get("_id"));

        // validate result
        mapResponseExpected.put("zone.time", "2017-08-01T09:00Z");
        mapResponseExpected.put("zone.value.code", "Stroom");
        mapResponseExpected.put("zone.value.name", "Stockroom");
        mapResponseExpected.put("zone.value.facilityMap", "Map Store Santa Monica");
        mapResponseExpected.put("zone.value.zoneType", "DefaultZoneType2");
        mapResponseExpected.put("zone.value.facilityMapTime", "2017-08-01T08:00Z");
        mapResponseExpected.put("zone.value.zoneTypeTime", "2017-08-01T08:00Z");
        // update zone group
        mapResponseExpected.put("zone.value.zoneGroupTime", "2017-08-01T09:00Z");
        mapResponseExpected.put("zone.value.zoneGroup", "On-Site");
        validateResultExpected(map, mapResponseExpected);

        // delete thing
        deleteThing(map);
    }

    /**
     * change zone, update zone type
     *
     * @throws Exception
     */
    @Test
    public void updateNativeUDFZone0004() throws Exception {
        String serialNumber = "UPDATEZONE004";
        // create thing
        Map<String, Object> map = compareEqualityResult("2017-08-01T08:00Z",
                createMap(thingTypeCodeRFID, serialNumber, new Document("zone", "Enance")), Boolean.FALSE);

        // update thing
        LOGGER.info("init update thing= " + map.get("_id"));
        mapUpdates.put("2017-08-01T09:00Z", createMap(thingTypeCodeRFID, serialNumber, new Document("zone", "Saloor")));
        map = executeCreateAndUpdateThing(mapUpdates, (Long) map.get("_id"));

        // validate result
        mapResponseExpected.put("zone.time", "2017-08-01T09:00Z");
        mapResponseExpected.put("zone.value.code", "Saloor");
        mapResponseExpected.put("zone.value.name", "Salesfloor");
        mapResponseExpected.put("zone.value.facilityMap", "Map Store Santa Monica");
        mapResponseExpected.put("zone.value.facilityMapTime", "2017-08-01T08:00Z");
        mapResponseExpected.put("zone.value.zoneGroupTime", "2017-08-01T08:00Z");
        mapResponseExpected.put("zone.value.zoneGroup", "Off-Site");
        // update zone type
        mapResponseExpected.put("zone.value.zoneType", "DefaultZoneType1");
        mapResponseExpected.put("zone.value.zoneTypeTime", "2017-08-01T09:00Z");
        validateResultExpected(map, mapResponseExpected);

        // delete thing
        deleteThing(map);
    }

    @Test
    public void updateNativeUDFZone0005() throws Exception {
        String serialNumber = "UPDATEZONE005";
        // create thing
        Map<String, Object> map = compareEqualityResult("2017-08-01T08:00Z",
                createMap(thingTypeCodeRFID, serialNumber, new Document("zone", "Enance")), Boolean.FALSE);

        // update thing
        LOGGER.info("init update thing= " + map.get("_id"));
        mapUpdates.put("2017-08-01T09:00Z", createMap(thingTypeCodeRFID, serialNumber,
                new Document("location", "-120.44397538751744;34.048111344342104;0.0"))); //unknown zone
        map = executeCreateAndUpdateThing(mapUpdates, (Long) map.get("_id"));

        // validate result
        mapResponseExpected.put("zone.time", "2017-08-01T09:00Z");
        mapResponseExpected.put("zone.value.code", "unknown");
        mapResponseExpected.put("zone.value.name", "Unknown");
        mapResponseExpected.put("zone.value.facilityMap", "unknown");
        mapResponseExpected.put("zone.value.facilityMapTime", "2017-08-01T09:00Z");
        mapResponseExpected.put("zone.value.zoneGroup", "unknown");
        mapResponseExpected.put("zone.value.zoneGroupTime", "2017-08-01T09:00Z");
        mapResponseExpected.put("zone.value.zoneType", "unknown");
        mapResponseExpected.put("zone.value.zoneTypeTime", "2017-08-01T09:00Z");
        validateResultExpected(map, mapResponseExpected);

        // delete thing
        deleteThing(map);
    }

}
