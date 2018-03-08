package com.tierconnect.riot.iot.services.thing;

import org.apache.log4j.Logger;
import org.bson.Document;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

/**
 * Created by vealaro on 8/8/17.
 */
public class ThingServiceUpdateThingTypeUDFTest extends ThingServiceUpdateTest {

    private static final Logger LOGGER = Logger.getLogger(ThingServiceUpdateThingTypeUDFTest.class);

    /**
     * <pre>
     * THING TYPE : SO = shippingorder_code, ASSET = asset_code
     * 1) Create thing SO without udf's
     * 2) Create thing ASSET with udf( status=Open )
     * 3) Update thing SO with childrenUDF ASSET
     * 4)
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void createUpdateSOAsset0001() throws Exception {
        String prefix = "SOA01";
        String serialNumberSO = "SO0000" + prefix;
        String serialNumberAsset = "ASSET000" + prefix;

        // create thing SO
        Map<String, Object> mapSO = compareEqualityResult("2017-08-01T08:00Z",
                createMap(thingTypeCodeSO, serialNumberSO, null), Boolean.FALSE);

        // create thing ASSET
        Map<String, Object> mapAsset = compareEqualityResult("2017-08-01T08:10Z",
                createMap(thingTypeCodeAsset, serialNumberAsset,
                        new Document("status", "Open")), Boolean.FALSE);

        // update SO with ASSET
        mapUpdates.put("2017-08-01T08:20Z", createMap(thingTypeCodeAsset, serialNumberAsset,
                new Document("shippingOrderField", serialNumberSO)));
        mapAsset = executeCreateAndUpdateThing(mapUpdates, (Long) mapAsset.get("_id"));

        // validate SO
        mapSO = getCurrentThing((Long) mapSO.get("_id"));
        mapResponseExpected.put("countOpenAsset.time", "2017-08-01T08:20Z");
        mapResponseExpected.put("countAsset.time", "2017-08-01T08:20Z");
        validateResultExpected(mapSO, mapResponseExpected);

        checkStructureSOAssetAssociate(serialNumberSO, mapSO, serialNumberAsset, mapAsset);

        // delete SO
        deleteThing(mapSO);
        mapAsset = getCurrentThing((Long) mapAsset.get("_id"));
        checkStructureSOAssetDisassociate(null, mapAsset);
        deleteThing(mapAsset);
    }

    /**
     * <pre>
     * THING TYPE : SO = shippingorder_code, ASSET = asset_code
     * 1) Create thing SO without udf's
     * 2) Create thing ASSET with udf( status=Open )
     * 3) Update thing ASSET with parent udf SO
     * 4)
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void createUpdateSOAsset0002() throws Exception {
        String prefix = "SOA02";
        String serialNumberSO = "SO0000" + prefix;
        String serialNumberAsset = "ASSET000" + prefix;

        // create thing SO
        Map<String, Object> mapSO = compareEqualityResult("2017-08-01T08:00Z",
                createMap(thingTypeCodeSO, serialNumberSO, null), Boolean.FALSE);

        // create thing ASSET
        Map<String, Object> mapAsset = compareEqualityResult("2017-08-01T08:10Z",
                createMap(thingTypeCodeAsset, serialNumberAsset,
                        new Document("status", "Open")), Boolean.FALSE);

        // update SO with ASSET
        mapUpdates.put("2017-08-01T08:20Z", createMap(thingTypeCodeSO, serialNumberSO, null,
                Collections.<Document>singletonList(new Document("serialNumber", serialNumberAsset)
                        .append("thingTypeCode", thingTypeCodeAsset)
                        .append("udfs",
                                new Document("shippingOrderField",
                                        new Document("value", serialNumberSO)))),
                Collections.<Document>emptyList()
        ));

        mapSO = executeCreateAndUpdateThing(mapUpdates, (Long) mapSO.get("_id"));
        mapResponseExpected.put("countOpenAsset.time", "2017-08-01T08:20Z");
        mapResponseExpected.put("countAsset.time", "2017-08-01T08:20Z");
        validateResultExpected(mapSO, mapResponseExpected);

        mapAsset = getCurrentThing((Long) mapAsset.get("_id"));
        checkStructureSOAssetAssociate(serialNumberSO, mapSO, serialNumberAsset, mapAsset);

        // delete ASSET
        deleteThing(mapAsset);
        mapSO = getCurrentThing((Long) mapSO.get("_id"));
        checkStructureSOAssetDisassociate(mapSO, null);
        deleteThing(mapSO);
    }

    /**
     * <pre>
     * THING TYPE : SO = shippingorder_code, ASSET = asset_code
     * 1) Create thing SO without udf's
     * 2) Create thing ASSET with udf( status=Open ) and parent udf SO
     * 3)
     * 4)
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void createUpdateSOAsset0003() throws Exception {
        String prefix = "SOA03";
        String serialNumberSO = "SO0000" + prefix;
        String serialNumberAsset = "ASSET000" + prefix;

        // create thing SO
        Map<String, Object> mapSO = compareEqualityResult("2017-08-01T08:00Z",
                createMap(thingTypeCodeSO, serialNumberSO, null), Boolean.FALSE);

        // create thing ASSET
        Map<String, Object> mapAsset = compareEqualityResult("2017-08-01T08:10Z",
                createMap(thingTypeCodeAsset, serialNumberAsset,
                        new Document("status", "Open").append("shippingOrderField", serialNumberSO)), Boolean.FALSE);

        // validate SO
        mapSO = getCurrentThing((Long) mapSO.get("_id"));
        mapResponseExpected.put("countOpenAsset.time", "2017-08-01T08:10Z");
        mapResponseExpected.put("countAsset.time", "2017-08-01T08:10Z");
        validateResultExpected(mapSO, mapResponseExpected);

        checkStructureSOAssetAssociate(serialNumberSO, mapSO, serialNumberAsset, mapAsset);

        // disassociate SO of ASSET
        mapUpdates.put("2017-08-01T08:20Z", createMap(thingTypeCodeAsset, serialNumberAsset,
                new Document("shippingOrderField", null)));
        mapAsset = executeCreateAndUpdateThing(mapUpdates, (Long) mapAsset.get("_id"));
        mapSO = getCurrentThing((Long) mapSO.get("_id"));

        checkStructureSOAssetDisassociate(mapSO, mapAsset);

        // delete SO and ASSET
        deleteThing(mapAsset);
        deleteThing(mapSO);
    }

    /**
     * <pre>
     * THING TYPE : SO = shippingorder_code, ASSET = asset_code
     * 1) Create thing ASSET with udf( status=Open )
     * 2) Create thing SO without udf's and childrenUDF ASSET
     * 3)
     * 4)
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void createUpdateSOAsset0004() throws Exception {
        String prefix = "SOA04E";
        String serialNumberSO = "SO0000" + prefix;
        String serialNumberAsset = "ASSET000" + prefix;

        // create thing ASSET
        Map<String, Object> mapAsset = compareEqualityResult("2017-08-01T08:00Z",
                createMap(thingTypeCodeAsset, serialNumberAsset,
                        new Document("status", "Open")), Boolean.FALSE);

        // create thing SO
        Map<String, Object> mapSO = compareEqualityResult("2017-08-01T08:10Z",
                createMap(
                        thingTypeCodeSO, serialNumberSO, new Document(),
                        Collections.<Document>singletonList(
                                new Document("serialNumber", serialNumberAsset)
                                        .append("thingTypeCode", thingTypeCodeAsset)
                                        .append("udfs",
                                                new Document("shippingOrderField",
                                                        new Document("value", serialNumberSO)))),
                        Collections.<Document>emptyList()
                ), Boolean.FALSE);


        // time
        mapResponseExpected.put("countOpenAsset.time", "2017-08-01T08:10Z");
        mapResponseExpected.put("countAsset.time", "2017-08-01T08:10Z");
        validateResultExpected(mapSO, mapResponseExpected);

        checkStructureSOAssetAssociate(serialNumberSO, mapSO, serialNumberAsset, mapAsset);

        // disassociate SO of ASSET
        mapUpdates.put("2017-08-01T08:20Z", createMap(thingTypeCodeAsset, serialNumberAsset,
                new Document("shippingOrderField", null)));
        mapAsset = executeCreateAndUpdateThing(mapUpdates, (Long) mapAsset.get("_id"));
        mapSO = getCurrentThing((Long) mapSO.get("_id"));

        checkStructureSOAssetDisassociate(mapSO, mapAsset);

        // delete SO and ASSET
        deleteThing(mapAsset);
        deleteThing(mapSO);
    }

    private void checkStructureSOAssetAssociate(String serialNumberSO, Map<String, Object> mapSO,
                                                String serialNumberAsset, Map<String, Object> mapAsset) throws Exception {
        // validate SO
        mapResponseExpected.put("countOpenAsset.value", "1");
        mapResponseExpected.put("countAsset.value", "1");
        mapResponseExpected.put("$.asset_code_children[*].serialNumber", array(serialNumberAsset));
        validateResultExpected(mapSO, mapResponseExpected);

        // validate Asset
        mapAsset = getCurrentThing((Long) mapAsset.get("_id"));
        mapResponseExpected.put("shippingOrderField.value.serialNumber", serialNumberSO);
        mapResponseExpected.put("shippingOrderField.value.countOpenAsset.value", "1");
        mapResponseExpected.put("shippingOrderField.value.countAsset.value", "1");
        validateResultExpected(mapAsset, mapResponseExpected);
    }

    private void checkStructureSOAssetDisassociate(Map<String, Object> mapSO, Map<String, Object> mapAsset) throws Exception {
        //
        if (mapSO != null) {
            mapResponseExpected.put("asset_code_children.length()", 0);
//        mapResponseExpected.put("countOpenAsset.value", "0"); // TODO: expression
//        mapResponseExpected.put("countAsset.value", "0");
            validateResultExpected(mapSO, mapResponseExpected);
        }

        if (mapAsset != null) {
            // TODO : remove udf (native udf and thing type udf)
            mapResponseExpected.put("shippingOrderField.value", null);
            validateResultExpected(mapAsset, mapResponseExpected);
        }
    }

    /**
     * <pre>
     * THING TYPE : CustomTT002, Colour
     * 1) Create thing Colour
     * 2) Create thing CustomTT002 with thing Colour
     * 3)
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void updateThingTypeUDF0001() throws Exception {
        String prefix = "0001";
        String serialNumberColor = "COLORFILE" + prefix;
        String serialNumberCustomTT002 = "CUSTOM" + prefix;

        // create thing with thing type color
        Map<String, Object> mapColour = compareEqualityResult("2017-08-01T08:00Z",
                createMap(thingTypeCodeColor, serialNumberColor, new Document("Action", "None")), Boolean.FALSE);

        // create thing with thing type CustomTT002
        Map<String, Object> mapCustomTT002 = compareEqualityResult("2017-08-01T08:30Z",
                createMap(thingTypeCodeCustomTT002, serialNumberCustomTT002,
                        new Document("thingTypeAttribute", serialNumberColor).append("zoneNativeUDF", "Enance")
                ), Boolean.FALSE);

        // expected
        mapResponseExpected.put("thingTypeAttribute.thingTypeFieldId", -1);
        mapResponseExpected.put("thingTypeAttribute.time", "2017-08-01T08:30Z");
        mapResponseExpected.put("thingTypeAttribute.value.serialNumber", serialNumberColor);
        validateResultExpected(mapCustomTT002, mapResponseExpected);

        mapUpdates.put("2017-08-01T08:45Z",
                createMap(thingTypeCodeCustomTT002, serialNumberCustomTT002, new Document("thingTypeAttribute", null)));
        mapCustomTT002 = executeCreateAndUpdateThing(mapUpdates, (Long) mapCustomTT002.get("_id"));

        // expected
        mapResponseExpected.put("thingTypeAttribute.value", null);
        validateResultExpected(mapCustomTT002, mapResponseExpected);

        // delete Colour and CustomTT002
        deleteThing(mapCustomTT002);
        deleteThing(mapColour);
    }

    /**
     * <pre>
     * THING TYPE : CustomTT002, Colour
     * 1) Create thing Colour
     * 2) Create thing CustomTT002 without udf's
     * 3) update thing CustomTT002 with thing Colour
     * 4)
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void updateThingTypeUDF0002() throws Exception {
        String prefix = "0002";
        String serialNumberColor = "COLORFILE" + prefix;
        String serialNumberCustomTT002 = "CUSTOM" + prefix;

        // create thing with thing type color
        Map<String, Object> mapColour = compareEqualityResult("2017-08-01T08:00Z",
                createMap(thingTypeCodeColor, serialNumberColor, new Document("Action", "None")), Boolean.FALSE);

        // create thing with thing type CustomTT002
        Map<String, Object> mapCustomTT02 = compareEqualityResult("2017-08-01T08:30Z",
                createMap(thingTypeCodeCustomTT002, serialNumberCustomTT002, new Document("zoneNativeUDF", "Enance")),
                Boolean.FALSE);

        // update thing with udf
        LOGGER.info("update thing thingTypeCode=" + thingTypeCodeCustomTT002 + " id= " + mapCustomTT02.get("_id"));
        mapUpdates.put("2017-08-01T09:00Z", createMap(thingTypeCodeCustomTT002, serialNumberCustomTT002,
                new Document("thingTypeAttribute", serialNumberColor)));
        mapCustomTT02 = executeCreateAndUpdateThing(mapUpdates, (Long) mapCustomTT02.get("_id"));

        // expected
        mapResponseExpected.put("thingTypeAttribute.thingTypeFieldId", -1);
        mapResponseExpected.put("thingTypeAttribute.time", "2017-08-01T09:00Z");
        mapResponseExpected.put("thingTypeAttribute.value.serialNumber", serialNumberColor);
        validateResultExpected(mapCustomTT02, mapResponseExpected);

        // delete Colour
        deleteThing(mapColour);

        //
        mapCustomTT02 = getCurrentThing((Long) mapCustomTT02.get("_id"));
        mapResponseExpected.put("thingTypeAttribute.time", null);
        mapResponseExpected.put("thingTypeAttribute.value", null);
        validateResultExpected(mapCustomTT02, mapResponseExpected);

        // delete CustomTT02
        deleteThing(mapCustomTT02);

    }

    /**
     * <pre>
     * THING TYPE : CustomTT002, Colour
     * 1) Create thing Colour
     * 2) Create thing CustomTT002 without udf's
     * 3) Update thing CustomTT002 with thing Colour
     * 4)
     * 5) Update thing one udf of Colour
     * 6)
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void updateThingTypeUDF0003() throws Exception {
        String prefix = "0003";
        String serialNumberColor = "COLORFILE" + prefix;
        String serialNumberCustomTT002 = "CUSTOM" + prefix;

        // create thing with thing type color
        Map<String, Object> mapColor = compareEqualityResult("2017-08-01T08:00Z",
                createMap(thingTypeCodeColor, serialNumberColor, new Document("Action", "None")),
                Boolean.FALSE);

        // create thing with thing type CustomTT002
        Map<String, Object> mapCustom = compareEqualityResult("2017-08-01T08:30Z",
                createMap(thingTypeCodeCustomTT002, serialNumberCustomTT002, null), Boolean.FALSE);

        // update thing with udf
        LOGGER.info("update thing thingTypeCode=" + thingTypeCodeCustomTT002 + " id= " + mapCustom.get("_id"));
        mapUpdates.put("2017-08-01T09:00Z",
                createMap(thingTypeCodeCustomTT002, serialNumberCustomTT002,
                        new Document("thingTypeAttribute", serialNumberColor)));
        mapCustom = executeCreateAndUpdateThing(mapUpdates, (Long) mapCustom.get("_id"));

        // validate thing CustomTT002
        mapResponseExpected.put("thingTypeAttribute.thingTypeFieldId", -1);
        mapResponseExpected.put("thingTypeAttribute.time", "2017-08-01T09:00Z");
        mapResponseExpected.put("thingTypeAttribute.value.serialNumber", serialNumberColor);
        validateResultExpected(mapCustom, mapResponseExpected);

        // update color
        mapUpdates.put("2017-08-01T09:30Z", createMap(thingTypeCodeColor, serialNumberColor,
                new Document("Action", "BUILD")));
        executeCreateAndUpdateThing(mapUpdates, (Long) mapColor.get("_id"));

        // get thing CustomTT002
        mapCustom = getCurrentThing((Long) mapCustom.get("_id"));

        // validate thing CustomTT002
        mapResponseExpected.put("thingTypeAttribute.value.serialNumber", serialNumberColor);
        mapResponseExpected.put("thingTypeAttribute.value.Action.time", "2017-08-01T09:30Z");
        mapResponseExpected.put("thingTypeAttribute.value.Action.value", "BUILD");
        validateResultExpected(mapCustom, mapResponseExpected);

        // delete CustomTT002
        deleteThing(mapCustom);
        deleteThing(mapColor);
    }

    /**
     * <pre>
     * THING TYPE : SO = shippingorder_code, ASSET = asset_code, TAG = tag_code
     * 1) Create thing SO without udf's
     * 2) Create thing ASSET(1) with SO and udf( status=Open )
     * 3) Create thing ASSET(2) with udf( status=Close )
     * 4) Update thing ASSET(2) with SO
     * 5)
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void updateThingTypeUDF0005() throws Exception {
        String prefix = "0005";
        String serialNumberSO = "SO0000" + prefix;
        String serialNumberAsset1 = "ASSET000" + prefix + "A";
        String serialNumberAsset2 = "ASSET000" + prefix + "B";

        // create thing SO
        Map<String, Object> mapSO = compareEqualityResult("2017-08-01T08:00Z",
                createMap(thingTypeCodeSO, serialNumberSO, null), Boolean.FALSE);

        // create thing ASSET 1
        Map<String, Object> mapAsset1 = compareEqualityResult("2017-08-01T08:10Z",
                createMap(thingTypeCodeAsset, serialNumberAsset1, new Document("status", "Open")
                        .append("shippingOrderField", serialNumberSO)), Boolean.FALSE);

        // create thing ASSET 2 and update udf
        Map<String, Object> mapAsset2 = compareEqualityResult("2017-08-01T08:20Z",
                createMap(thingTypeCodeAsset, serialNumberAsset2, new Document("status", "Close")), Boolean.FALSE);
        mapUpdates.put("2017-08-01T08:30Z", createMap(thingTypeCodeAsset, serialNumberAsset2,
                new Document("shippingOrderField", serialNumberSO)));
        mapAsset2 = executeCreateAndUpdateThing(mapUpdates, (Long) mapAsset2.get("_id"));

        // validate SO
        mapSO = getCurrentThing((Long) mapSO.get("_id"));
        mapResponseExpected.put("countOpenAsset.time", "2017-08-01T08:30Z");
        mapResponseExpected.put("countOpenAsset.value", "1");
        mapResponseExpected.put("countAsset.time", "2017-08-01T08:30Z");
        mapResponseExpected.put("countAsset.value", "2");
        mapResponseExpected.put("$.asset_code_children.length()", 2);
        mapResponseExpected.put("$.asset_code_children[*].serialNumber", array(serialNumberAsset1, serialNumberAsset2));
        validateResultExpected(mapSO, mapResponseExpected);

        // validate Asset 1
        mapAsset1 = getCurrentThing((Long) mapAsset1.get("_id"));
        mapResponseExpected.put("shippingOrderField.value.serialNumber", serialNumberSO);
        mapResponseExpected.put("shippingOrderField.value.countOpenAsset.value", "1");
        mapResponseExpected.put("shippingOrderField.value.countAsset.value", "2");
        validateResultExpected(mapAsset1, mapResponseExpected);

        // validate Asset 2
        mapResponseExpected.put("shippingOrderField.value.serialNumber", serialNumberSO);
        mapResponseExpected.put("shippingOrderField.value.countOpenAsset.value", "1");
        mapResponseExpected.put("shippingOrderField.value.countAsset.value", "2");
        validateResultExpected(mapAsset2, mapResponseExpected);

        deleteThing(mapAsset2);
        deleteThing(mapAsset1);

        mapResponseExpected.put("$.asset_code_children.length()", 0);
//        mapResponseExpected.put("countOpenAsset.value", "0");
//        mapResponseExpected.put("countAsset.value", "0");
        mapSO = getCurrentThing((Long) mapSO.get("_id"));
        validateResultExpected(mapSO, mapResponseExpected);

        deleteThing(mapSO);
    }

    /**
     * <pre>
     * THING TYPE : CustomTT002, CustomTT003, Colour
     * 1) Create thing Colour
     * 2) Create thing CustomTT002 with thing Colour
     * 3) Create thing CustomTT002 with thing Colour
     * 4)
     * 5)
     * 6)
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void updateThingTypeUDF0006() throws Exception {
        String prefix = "0006";
        String serialNumberColor = "COLORFILE" + prefix;
        String serialNumberCustomTT002A = "CUSTOMTT02" + prefix + "A";
        String serialNumberCustomTT002B = "CUSTOMTT02" + prefix + "B";
        String serialNumberCustomTT003A = "CUSTOMTT03" + prefix + "A";

        // create thing with thing type color
        Map<String, Object> mapColor = compareEqualityResult("2017-08-01T08:00Z",
                createMap(thingTypeCodeColor, serialNumberColor, new Document("Action", "None")),
                Boolean.FALSE);

        // create thing with thing type CustomTT002
        Map<String, Object> mapCustom002A = compareEqualityResult("2017-08-01T08:30Z",
                createMap(thingTypeCodeCustomTT002, serialNumberCustomTT002A,
                        new Document("thingTypeAttribute", serialNumberColor)), Boolean.FALSE);

        // create thing with thing type CustomTT002
        Map<String, Object> mapCustom002B = compareEqualityResult("2017-08-01T08:40Z",
                createMap(thingTypeCodeCustomTT002, serialNumberCustomTT002B,
                        new Document("thingTypeAttribute", serialNumberColor)), Boolean.FALSE);

        Map<String, Object> mapCustom003A = compareEqualityResult("2017-08-01T08:50Z",
                createMap(thingTypeCodeCustomTT003, serialNumberCustomTT003A,
                        new Document("thingTypeAttribute", serialNumberColor)), Boolean.FALSE);

        mapResponseExpected.put("thingTypeAttribute.time", "2017-08-01T08:30Z");
        mapResponseExpected.put("thingTypeAttribute.value.Action.value", "None");
        validateResultExpected(mapCustom002A, mapResponseExpected);

        mapResponseExpected.put("thingTypeAttribute.time", "2017-08-01T08:40Z");
        mapResponseExpected.put("thingTypeAttribute.value.Action.value", "None");
        validateResultExpected(mapCustom002B, mapResponseExpected);

        mapResponseExpected.put("thingTypeAttribute.time", "2017-08-01T08:50Z");
        mapResponseExpected.put("thingTypeAttribute.value.Action.value", "None");
        validateResultExpected(mapCustom003A, mapResponseExpected);

        deleteThing(mapColor);

        mapCustom002A = getCurrentThing((Long) mapCustom002A.get("_id"));
        mapResponseExpected.put("thingTypeAttribute", null);
        validateResultExpected(mapCustom002A, mapResponseExpected);

        mapCustom002B = getCurrentThing((Long) mapCustom002B.get("_id"));
        mapResponseExpected.put("thingTypeAttribute", null);
        validateResultExpected(mapCustom002B, mapResponseExpected);

        mapCustom003A = getCurrentThing((Long) mapCustom003A.get("_id"));
        mapResponseExpected.put("thingTypeAttribute", null);
        validateResultExpected(mapCustom003A, mapResponseExpected);

        deleteThing(mapCustom002A);
        deleteThing(mapCustom002B);
        deleteThing(mapCustom003A);
    }

}
