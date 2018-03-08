package com.tierconnect.riot.iot.services.thing;

import com.tierconnect.riot.iot.entities.Thing;
import com.tierconnect.riot.iot.services.ThingService;
import org.bson.Document;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Created by vealaro on 8/14/17.
 */
public class ThingServiceUpdateParentChildThingTypeUDFTest extends ThingServiceUpdateTest {

    /**
     * <pre>
     * THING TYPE : SO = shippingorder_code, ASSET = asset_code, TAG = tag_code
     * 1) Create thing SO without udf's
     * 2) Create thing ASSET with SO and udf( status=Open )
     * 3) Create thing TAG with parent ASSET and udf( zone=Enance )
     * 4)
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void testShippingOrderAssetTag0001() throws Exception {
        String prefix = "SOASSTAG001";
        String serialNumberSO = "SO0000" + prefix;
        String serialNumberAsset = "ASSET000" + prefix;
        String serialNumberTAG = "TAG000" + prefix;

        // create thing SO
        Map<String, Object> mapSO = compareEqualityResult("2017-08-01T08:00Z",
                createMap(thingTypeCodeSO, serialNumberSO, null), Boolean.FALSE);

        // create thing ASSET
        Map<String, Object> mapAsset = compareEqualityResult("2017-08-01T08:10Z",
                createMap(thingTypeCodeAsset, serialNumberAsset, new Document("status", "Open")
                        .append("shippingOrderField", serialNumberSO)), Boolean.FALSE);

        // create thing TAG
        Map<String, Object> mapTag = compareEqualityResult("2017-08-01T08:20Z",
                createMap(thingTypeCodeTag, serialNumberTAG,
                        // UDF
                        new Document("zone", "Enance"),
                        // PARENT
                        new Document("serialNumber", serialNumberAsset)
                                .append("thingTypeCode", thingTypeCodeAsset)
                                .append("id", mapAsset.get("_id"))),
                Boolean.FALSE);

        mapSO = getCurrentThing((Long) mapSO.get("_id"));
        mapAsset = getCurrentThing((Long) mapAsset.get("_id"));
        // expected time
        mapResponseExpected.put("countOpenAsset.time", "2017-08-01T08:10Z");
        mapResponseExpected.put("countAsset.time", "2017-08-01T08:10Z");

        checkStructureSOAssetTagAssociate(mapSO, mapAsset, mapTag, serialNumberSO, serialNumberAsset, serialNumberTAG);

        // delete ASSET
        deleteThing(mapAsset);

        // check SO and TAG
        mapSO = getCurrentThing((Long) mapSO.get("_id"));
        mapTag = getCurrentThing((Long) mapTag.get("_id"));
        checkStructureSOAssetTagDisassociate(mapSO, null, mapTag);

        // delete things
        deleteThing(mapSO);
        deleteThing(mapTag);
    }


    /**
     * <pre>
     * THING TYPE : SO = shippingorder_code, ASSET = asset_code, TAG = tag_code
     * 1) Create thing ASSET with udf( status=Open )
     * 2) Create thing SO without udf's and childrenUDF ASSET
     * 3) Create TAG with udf( zone=Enance ) AND parent ASSET
     * 4)
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void testShippingOrderAssetTag0002() throws Exception {
        String prefix = "SOASSTAG002";
        String serialNumberSO = "SO0000" + prefix;
        String serialNumberAsset = "ASSET000" + prefix;
        String serialNumberTAG = "TAG000" + prefix;

        // create thing ASSET
        Map<String, Object> mapAsset = compareEqualityResult("2017-08-01T08:00Z",
                createMap(thingTypeCodeAsset, serialNumberAsset,
                        new Document("status", "Open")), Boolean.FALSE);

        // create thing SO
        Map<String, Object> mapSO = compareEqualityResult("2017-08-01T08:10Z",
                createMap(
                        thingTypeCodeSO, serialNumberSO, null,
                        Collections.<Document>singletonList(
                                new Document("serialNumber", serialNumberAsset)
                                        .append("thingTypeCode", thingTypeCodeAsset)
                                        .append("udfs",
                                                new Document("shippingOrderField",
                                                        new Document("value", serialNumberSO)))),
                        Collections.<Document>emptyList()
                ), Boolean.FALSE);

        // create thing TAG
        Map<String, Object> mapTag = compareEqualityResult("2017-08-01T08:20Z",
                createMap(thingTypeCodeTag, serialNumberTAG,
                        // UDF
                        new Document("zone", "Enance"),
                        // PARENT
                        new Document("serialNumber", serialNumberAsset)
                                .append("thingTypeCode", thingTypeCodeAsset)
                                .append("id", mapAsset.get("_id"))),
                Boolean.FALSE);

        mapAsset = getCurrentThing((Long) mapAsset.get("_id"));
        mapSO = getCurrentThing((Long) mapSO.get("_id"));
        // expected time
        mapResponseExpected.put("countOpenAsset.time", "2017-08-01T08:10Z");
        mapResponseExpected.put("countAsset.time", "2017-08-01T08:10Z");

        checkStructureSOAssetTagAssociate(mapSO, mapAsset, mapTag, serialNumberSO, serialNumberAsset, serialNumberTAG);

        // delete ASSET
        deleteThing(mapTag);

        // check SO and TAG
        mapSO = getCurrentThing((Long) mapSO.get("_id"));
        mapAsset = getCurrentThing((Long) mapAsset.get("_id"));
        checkStructureSOAssetTagDisassociate(mapSO, mapAsset, null);

        // delete things
        deleteThing(mapSO);
        deleteThing(mapAsset);
    }

    /**
     * <pre>
     * THING TYPE : SO = shippingorder_code, ASSET = asset_code, TAG = tag_code
     * 1) Create TAG with udf( zone=Enance )
     * 2) Create thing ASSET with udf( status=Open ) and child TAG
     * 3) Create thing SO without udf's and childrenUDF ASSET
     * 4)
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void testShippingOrderAssetTag0003() throws Exception {
        String prefix = "SOASSTAG003";
        String serialNumberSO = "SO0000" + prefix;
        String serialNumberAsset = "ASSET000" + prefix;
        String serialNumberTAG = "TAG000" + prefix;

        // create thing TAG
        Map<String, Object> mapTag = compareEqualityResult("2017-08-01T08:00Z",
                createMap(thingTypeCodeTag, serialNumberTAG, new Document("zone", "Enance")),
                Boolean.FALSE);

        // create thing ASSET
        Map<String, Object> mapAsset = compareEqualityResult("2017-08-01T08:10Z",
                createMap(thingTypeCodeAsset, serialNumberAsset,
                        new Document("status", "Open"),
                        Collections.<Document>emptyList(),
                        Collections.<Document>singletonList(
                                new Document("serialNumber", serialNumberTAG)
                                        .append("thingTypeCode", thingTypeCodeTag))), Boolean.FALSE);

        // create thing SO
        Map<String, Object> mapSO = compareEqualityResult("2017-08-01T08:20Z",
                createMap(
                        thingTypeCodeSO, serialNumberSO, null,
                        Collections.<Document>singletonList(
                                new Document("serialNumber", serialNumberAsset)
                                        .append("thingTypeCode", thingTypeCodeAsset)
                                        .append("udfs",
                                                new Document("shippingOrderField",
                                                        new Document("value", serialNumberSO)))),
                        Collections.<Document>emptyList()
                ), Boolean.FALSE);

        mapTag = getCurrentThing((Long) mapTag.get("_id"));
        mapAsset = getCurrentThing((Long) mapAsset.get("_id"));
        // expected time
        mapResponseExpected.put("countOpenAsset.time", "2017-08-01T08:20Z");
        mapResponseExpected.put("countAsset.time", "2017-08-01T08:20Z");

        checkStructureSOAssetTagAssociate(mapSO, mapAsset, mapTag, serialNumberSO, serialNumberAsset, serialNumberTAG);

        // delete SO
        deleteThing(mapSO);

        // check SO and TAG
        mapAsset = getCurrentThing((Long) mapAsset.get("_id"));
        mapTag = getCurrentThing((Long) mapTag.get("_id"));
        checkStructureSOAssetTagDisassociate(null, mapAsset, mapTag);

        // delete things
        deleteThing(mapAsset);
        deleteThing(mapTag);

    }

    /**
     * <pre>
     * THING TYPE : SO = shippingorder_code, ASSET = asset_code, TAG = tag_code
     * 1) Create thing SO without udf's
     * 2) Create TAG with udf( zone=Enance )
     * 3) Create thing ASSET with udf( status=Open ) ,Child TAG and parent udf SO
     * 4)
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void testShippingOrderAssetTag0004() throws Exception {
        String prefix = "SOASSTAG004";
        String serialNumberSO = "SO0000" + prefix;
        String serialNumberAsset = "ASSET000" + prefix;
        String serialNumberTAG = "TAG000" + prefix;

        // create thing SO
        Map<String, Object> mapSO = compareEqualityResult("2017-08-01T08:00Z",
                createMap(thingTypeCodeSO, serialNumberSO, null), Boolean.FALSE);

        // create thing TAG
        Map<String, Object> mapTag = compareEqualityResult("2017-08-01T08:10Z",
                createMap(thingTypeCodeTag, serialNumberTAG, new Document("zone", "Enance")), Boolean.FALSE);

        // create thing ASSET
        Map<String, Object> mapAsset = compareEqualityResult("2017-08-01T08:20Z",
                createMap(thingTypeCodeAsset, serialNumberAsset, new Document("status", "Open")
                                .append("shippingOrderField", serialNumberSO),
                        Collections.<Document>emptyList(),
                        Collections.<Document>singletonList(
                                new Document("serialNumber", serialNumberTAG)
                                        .append("thingTypeCode", thingTypeCodeTag)))
                , Boolean.FALSE);

        mapTag = getCurrentThing((Long) mapTag.get("_id"));
        mapSO = getCurrentThing((Long) mapSO.get("_id"));
        // expected time
        mapResponseExpected.put("countOpenAsset.time", "2017-08-01T08:20Z");
        mapResponseExpected.put("countAsset.time", "2017-08-01T08:20Z");

        checkStructureSOAssetTagAssociate(mapSO, mapAsset, mapTag, serialNumberSO, serialNumberAsset, serialNumberTAG);

        // disassociate SO of ASSET
        mapUpdates.put("2017-08-01T08:30Z", createMap(thingTypeCodeAsset, serialNumberAsset,
                new Document("shippingOrderField", null)));
        mapAsset = executeCreateAndUpdateThing(mapUpdates, (Long) mapAsset.get("_id"));

        // check ASSET
        mapResponseExpected.put("shippingOrderField.time", "2017-08-01T08:20Z");
        mapResponseExpected.put("shippingOrderField.value", null);
        mapResponseExpected.put("children.length()", 1);
        validateResultExpected(mapAsset, mapResponseExpected);

        // check SO
        mapSO = getCurrentThing((Long) mapSO.get("_id"));
//        mapResponseExpected.put("countAsset.value", "0");
//        mapResponseExpected.put("countOpenAsset.value", "0");
        mapResponseExpected.put("asset_code_children.length()", 0);
        validateResultExpected(mapSO, mapResponseExpected);

        // delete things
        deleteThing(mapSO);
        deleteThing(mapAsset);
        deleteThing(mapTag);
    }

    /**
     * <pre>
     * THING TYPE : SO = shippingorder_code, ASSET = asset_code, TAG = tag_code
     * 1) Create thing SO without udf's
     * 2) Create thing ASSET with SO and udf( status=Open )
     * 3) Create thing TAG with udf( zone=Enance )
     * 4) Update thing TAG with parent ASSET
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void testShippingOrderAssetTag0005() throws Exception {
        String prefix = "SOASSTAG005";
        String serialNumberSO = "SO0000" + prefix;
        String serialNumberAsset = "ASSET000" + prefix;
        String serialNumberTAG = "TAG000" + prefix;

        // create thing SO
        Map<String, Object> mapSO = compareEqualityResult("2017-08-01T08:00Z",
                createMap(thingTypeCodeSO, serialNumberSO, null), Boolean.FALSE);

        // create thing ASSET
        Map<String, Object> mapAsset = compareEqualityResult("2017-08-01T08:10Z",
                createMap(thingTypeCodeAsset, serialNumberAsset, new Document("status", "Open")
                        .append("shippingOrderField", serialNumberSO)), Boolean.FALSE);

        // create thing TAG
        Map<String, Object> mapTag = compareEqualityResult("2017-08-01T08:20Z",
                createMap(thingTypeCodeTag, serialNumberTAG, new Document("zone", "Enance")),
                Boolean.FALSE);

        // update TAG with ASSET
        mapUpdates.put("2017-08-01T08:30Z", createMap(thingTypeCodeTag, serialNumberTAG,
                new Document(), new Document("serialNumber", serialNumberAsset)
                        .append("thingTypeCode", thingTypeCodeAsset)
                        .append("id", mapAsset.get("_id"))));
        mapTag = executeCreateAndUpdateThing(mapUpdates, (Long) mapTag.get("_id"));

        mapAsset = getCurrentThing((Long) mapAsset.get("_id"));
        mapSO = getCurrentThing((Long) mapSO.get("_id"));
        // expected time
        mapResponseExpected.put("countOpenAsset.time", "2017-08-01T08:10Z");
        mapResponseExpected.put("countAsset.time", "2017-08-01T08:10Z");

        checkStructureSOAssetTagAssociate(mapSO, mapAsset, mapTag, serialNumberSO, serialNumberAsset, serialNumberTAG);

        // disassociate TAG of ASSET
        mapUpdates.put("2017-08-01T08:20Z", createMap(thingTypeCodeAsset, serialNumberAsset, null, null,
                Collections.<Document>emptyList()));

        mapAsset = executeCreateAndUpdateThing(mapUpdates, (Long) mapAsset.get("_id"));

        mapResponseExpected.put("shippingOrderField.value.countAsset.value", "1");
        mapResponseExpected.put("shippingOrderField.value.countOpenAsset.value", "1");
        mapResponseExpected.put("status.value", "Open");
        mapResponseExpected.put("children.length()", 0);
        validateResultExpected(mapAsset, mapResponseExpected);

        mapTag = getCurrentThing((Long) mapTag.get("_id"));
        mapResponseExpected.put("parent", null);
        validateResultExpected(mapTag, mapResponseExpected);

        // delete things
        deleteThing(mapSO);
        deleteThing(mapAsset);
        deleteThing(mapTag);
    }

    /**
     * <pre>
     * THING TYPE : SO = shippingorder_code, ASSET = asset_code, TAG = tag_code
     * 1) Create thing SO without udf's
     * 2) Create thing ASSET with SO and udf( status=Open )
     * 3) Create thing TAG with udf( zone=Enance )
     * 4) Update thing ASSET with child TAG
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void testShippingOrderAssetTag0006() throws Exception {
        String prefix = "SOASSTAG006";
        String serialNumberSO = "SO0000" + prefix;
        String serialNumberAsset = "ASSET000" + prefix;
        String serialNumberTAG = "TAG000" + prefix;

        // create thing SO
        Map<String, Object> mapSO = compareEqualityResult("2017-08-01T08:00Z",
                createMap(thingTypeCodeSO, serialNumberSO, null), Boolean.FALSE);

        // create thing ASSET
        Map<String, Object> mapAsset = compareEqualityResult("2017-08-01T08:10Z",
                createMap(thingTypeCodeAsset, serialNumberAsset, new Document("status", "Open")
                        .append("shippingOrderField", serialNumberSO)), Boolean.FALSE);

        // create thing TAG
        Map<String, Object> mapTag = compareEqualityResult("2017-08-01T08:20Z",
                createMap(thingTypeCodeTag, serialNumberTAG, new Document("zone", "Enance")),
                Boolean.FALSE);


        // update ASSET with TAG
        mapUpdates.put("2017-08-01T08:30Z", createMap(thingTypeCodeAsset, serialNumberAsset, null,
                Collections.<Document>emptyList(),
                Collections.<Document>singletonList(
                        new Document("serialNumber", serialNumberTAG)
                                .append("thingTypeCode", thingTypeCodeTag))));
        mapAsset = executeCreateAndUpdateThing(mapUpdates, (Long) mapAsset.get("_id"));

        mapTag = getCurrentThing((Long) mapTag.get("_id"));
        mapSO = getCurrentThing((Long) mapSO.get("_id"));
        // expected time
        mapResponseExpected.put("countOpenAsset.time", "2017-08-01T08:30Z");
        mapResponseExpected.put("countAsset.time", "2017-08-01T08:30Z");

        checkStructureSOAssetTagAssociate(mapSO, mapAsset, mapTag, serialNumberSO, serialNumberAsset, serialNumberTAG);

        // disassociate ASSET of TAG
        mapUpdates.put("2017-08-01T08:40Z", createMap(thingTypeCodeTag, serialNumberTAG, new Document(), new Document()));
        mapTag = executeCreateAndUpdateThing(mapUpdates, (Long) mapTag.get("_id"));

        mapResponseExpected.put("parent", null);
        validateResultExpected(mapTag, mapResponseExpected);

        mapAsset = getCurrentThing((Long) mapAsset.get("_id"));
        mapResponseExpected.put("shippingOrderField.value.countAsset.value", "1");
        mapResponseExpected.put("shippingOrderField.value.countOpenAsset.value", "1");
        mapResponseExpected.put("status.value", "Open");
        mapResponseExpected.put("children.length()", 0);
        validateResultExpected(mapAsset, mapResponseExpected);

        // delete things
        deleteThing(mapSO);
        deleteThing(mapAsset);
        deleteThing(mapTag);
    }

    /**
     * <pre>
     * THING TYPE : SO = shippingorder_code, ASSET = asset_code, TAG = tag_code
     * 1) Create thing ASSET with udf( status=Open )
     * 2) Create thing SO without udf's and childrenUDF ASSET
     * 3) Create thing TAG with udf( zone=Enance )
     * 4) Update thing TAG with parent ASSET
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void testShippingOrderAssetTag0007() throws Exception {
        String prefix = "SOASSTAG007";
        String serialNumberSO = "SO0000" + prefix;
        String serialNumberAsset = "ASSET000" + prefix;
        String serialNumberTAG = "TAG000" + prefix;

        // create thing ASSET
        Map<String, Object> mapAsset = compareEqualityResult("2017-08-01T08:00Z",
                createMap(thingTypeCodeAsset, serialNumberAsset,
                        new Document("status", "Open")), Boolean.FALSE);

        // create thing SO
        Map<String, Object> mapSO = compareEqualityResult("2017-08-01T08:10Z",
                createMap(
                        thingTypeCodeSO, serialNumberSO, null,
                        Collections.<Document>singletonList(
                                new Document("serialNumber", serialNumberAsset)
                                        .append("thingTypeCode", thingTypeCodeAsset)
                                        .append("udfs",
                                                new Document("shippingOrderField",
                                                        new Document("value", serialNumberSO)))),
                        Collections.<Document>emptyList()
                ), Boolean.FALSE);

        // create thing TAG
        Map<String, Object> mapTag = compareEqualityResult("2017-08-01T08:20Z",
                createMap(thingTypeCodeTag, serialNumberTAG, new Document("zone", "Enance")),
                Boolean.FALSE);

        // update TAG with ASSET
        mapUpdates.put("2017-08-01T08:30Z", createMap(thingTypeCodeTag, serialNumberTAG,
                new Document(), new Document("serialNumber", serialNumberAsset)
                        .append("thingTypeCode", thingTypeCodeAsset)
                        .append("id", mapAsset.get("_id"))));
        mapTag = executeCreateAndUpdateThing(mapUpdates, (Long) mapTag.get("_id"));

        mapAsset = getCurrentThing((Long) mapAsset.get("_id"));
        mapSO = getCurrentThing((Long) mapSO.get("_id"));
        // expected time
        mapResponseExpected.put("countOpenAsset.time", "2017-08-01T08:10Z");
        mapResponseExpected.put("countAsset.time", "2017-08-01T08:10Z");

        checkStructureSOAssetTagAssociate(mapSO, mapAsset, mapTag, serialNumberSO, serialNumberAsset, serialNumberTAG);

        // delete things
        deleteThing(mapTag);
        deleteThing(mapAsset);
        deleteThing(mapSO);
    }

    /**
     * <pre>
     * THING TYPE : SO = shippingorder_code, ASSET = asset_code, TAG = tag_code
     * 1) Create thing ASSET with udf( status=Open )
     * 2) Create thing SO without udf's and childrenUDF ASSET
     * 3) Create thing TAG with udf( zone=Enance )
     * 4) Update thing ASSET with child TAG
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void testShippingOrderAssetTag0008() throws Exception {
        String prefix = "SOASSTAG008";
        String serialNumberSO = "SO0000" + prefix;
        String serialNumberAsset = "ASSET000" + prefix;
        String serialNumberTAG = "TAG000" + prefix;

        // create thing ASSET
        Map<String, Object> mapAsset = compareEqualityResult("2017-08-01T08:00Z",
                createMap(thingTypeCodeAsset, serialNumberAsset,
                        new Document("status", "Open")), Boolean.FALSE);

        // create thing SO
        Map<String, Object> mapSO = compareEqualityResult("2017-08-01T08:10Z",
                createMap(
                        thingTypeCodeSO, serialNumberSO, null,
                        Collections.<Document>singletonList(
                                new Document("serialNumber", serialNumberAsset)
                                        .append("thingTypeCode", thingTypeCodeAsset)
                                        .append("udfs",
                                                new Document("shippingOrderField",
                                                        new Document("value", serialNumberSO)))),
                        Collections.<Document>emptyList()
                ), Boolean.FALSE);

        // create thing TAG
        Map<String, Object> mapTag = compareEqualityResult("2017-08-01T08:20Z",
                createMap(thingTypeCodeTag, serialNumberTAG, new Document("zone", "Enance")),
                Boolean.FALSE);

        // update ASSET with TAG
        mapUpdates.put("2017-08-01T08:30Z", createMap(thingTypeCodeAsset, serialNumberAsset, null,
                Collections.<Document>emptyList(),
                Collections.<Document>singletonList(
                        new Document("serialNumber", serialNumberTAG)
                                .append("thingTypeCode", thingTypeCodeTag))));
        mapAsset = executeCreateAndUpdateThing(mapUpdates, (Long) mapAsset.get("_id"));

        mapTag = getCurrentThing((Long) mapTag.get("_id"));
        mapSO = getCurrentThing((Long) mapSO.get("_id"));
        // expected time
        mapResponseExpected.put("countOpenAsset.time", "2017-08-01T08:30Z");
        mapResponseExpected.put("countAsset.time", "2017-08-01T08:30Z");

        checkStructureSOAssetTagAssociate(mapSO, mapAsset, mapTag, serialNumberSO, serialNumberAsset, serialNumberTAG);

        // delete things
        deleteThing(mapTag);
        deleteThing(mapAsset);
        deleteThing(mapSO);
    }

    /**
     * <pre>
     * THING TYPE : SO = shippingorder_code, ASSET = asset_code, TAG = tag_code
     * 1) Create thing TAG with udf( zone=Enance )
     * 2) Create thing ASSET with udf( status=Open )
     * 3) Update thing TAG with parent ASSET
     * 4) Create thing SO without udf's and childrenUDF ASSET
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void testShippingOrderAssetTag0009() throws Exception {
        String prefix = "SOASSTAG009";
        String serialNumberSO = "SO0000" + prefix;
        String serialNumberAsset = "ASSET000" + prefix;
        String serialNumberTAG = "TAG000" + prefix;

        // create thing ASSET
        Map<String, Object> mapAsset = compareEqualityResult("2017-08-01T08:00Z",
                createMap(thingTypeCodeAsset, serialNumberAsset,
                        new Document("status", "Open")), Boolean.FALSE);

        // create thing TAG
        Map<String, Object> mapTag = compareEqualityResult("2017-08-01T08:10Z",
                createMap(thingTypeCodeTag, serialNumberTAG, new Document("zone", "Enance")), Boolean.FALSE);

        // update TAG with ASSET
        mapUpdates.put("2017-08-01T08:20Z", createMap(thingTypeCodeTag, serialNumberTAG,
                new Document(), new Document("serialNumber", serialNumberAsset)
                        .append("thingTypeCode", thingTypeCodeAsset)
                        .append("id", mapAsset.get("_id"))));
        mapTag = executeCreateAndUpdateThing(mapUpdates, (Long) mapTag.get("_id"));

        // create thing SO
        Map<String, Object> mapSO = compareEqualityResult("2017-08-01T08:30Z",
                createMap(
                        thingTypeCodeSO, serialNumberSO, null,
                        Collections.<Document>singletonList(
                                new Document("serialNumber", serialNumberAsset)
                                        .append("thingTypeCode", thingTypeCodeAsset)
                                        .append("udfs",
                                                new Document("shippingOrderField",
                                                        new Document("value", serialNumberSO)))),
                        Collections.<Document>emptyList()
                ), Boolean.FALSE);

        mapTag = getCurrentThing((Long) mapTag.get("_id"));
        mapAsset = getCurrentThing((Long) mapAsset.get("_id"));
        // expected time
        mapResponseExpected.put("countOpenAsset.time", "2017-08-01T08:30Z");
        mapResponseExpected.put("countAsset.time", "2017-08-01T08:30Z");

        checkStructureSOAssetTagAssociate(mapSO, mapAsset, mapTag, serialNumberSO, serialNumberAsset, serialNumberTAG);

        // delete things
        deleteThing(mapTag);
        deleteThing(mapAsset);
        deleteThing(mapSO);
    }

    /**
     * <pre>
     * THING TYPE : SO = shippingorder_code, ASSET = asset_code, TAG = tag_code
     * 1) Create thing TAG with udf( zone=Enance )
     * 2) Create thing ASSET with udf( status=Open )
     * 3) Update thing ASSET with child TAG
     * 4) Create thing SO without udf's and childrenUDF ASSET
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void testShippingOrderAssetTag0010() throws Exception {
        String prefix = "SOASSTAG010";
        String serialNumberSO = "SO0000" + prefix;
        String serialNumberAsset = "ASSET000" + prefix;
        String serialNumberTAG = "TAG000" + prefix;

        // create thing ASSET
        Map<String, Object> mapAsset = compareEqualityResult("2017-08-01T08:00Z",
                createMap(thingTypeCodeAsset, serialNumberAsset,
                        new Document("status", "Open")), Boolean.FALSE);

        // create thing TAG
        Map<String, Object> mapTag = compareEqualityResult("2017-08-01T08:10Z",
                createMap(thingTypeCodeTag, serialNumberTAG, new Document("zone", "Enance")), Boolean.FALSE);

        // update ASSET with TAG
        mapUpdates.put("2017-08-01T08:20Z", createMap(thingTypeCodeAsset, serialNumberAsset, null,
                Collections.<Document>emptyList(),
                Collections.<Document>singletonList(
                        new Document("serialNumber", serialNumberTAG)
                                .append("thingTypeCode", thingTypeCodeTag))));
        mapAsset = executeCreateAndUpdateThing(mapUpdates, (Long) mapAsset.get("_id"));

        // create thing SO
        Map<String, Object> mapSO = compareEqualityResult("2017-08-01T08:30Z",
                createMap(
                        thingTypeCodeSO, serialNumberSO, null,
                        Collections.<Document>singletonList(
                                new Document("serialNumber", serialNumberAsset)
                                        .append("thingTypeCode", thingTypeCodeAsset)
                                        .append("udfs",
                                                new Document("shippingOrderField",
                                                        new Document("value", serialNumberSO)))),
                        Collections.<Document>emptyList()
                ), Boolean.FALSE);

        mapTag = getCurrentThing((Long) mapTag.get("_id"));
        mapAsset = getCurrentThing((Long) mapAsset.get("_id"));
        // expected time
        mapResponseExpected.put("countOpenAsset.time", "2017-08-01T08:30Z");
        mapResponseExpected.put("countAsset.time", "2017-08-01T08:30Z");

        checkStructureSOAssetTagAssociate(mapSO, mapAsset, mapTag, serialNumberSO, serialNumberAsset, serialNumberTAG);

        // delete things
        deleteThing(mapTag);
        deleteThing(mapAsset);
        deleteThing(mapSO);
    }

    /**
     * <pre>
     * THING TYPE : SO = shippingorder_code, ASSET = asset_code, TAG = tag_code
     * 1) Create thing SO without udf's
     * 2) Create thing ASSET with udf( status=Open )
     * 3) Create thing TAG with udf( zone=Enance )
     * 4) Update thing ASSET with udf( status=Open ) ,Child TAG and parent udf SO
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void testShippingOrderAssetTag0011() throws Exception {
        String prefix = "SOASSTAG011";
        String serialNumberSO = "SO0000" + prefix;
        String serialNumberAsset = "ASSET000" + prefix;
        String serialNumberTAG = "TAG000" + prefix;

        // create thing ASSET
        Map<String, Object> mapAsset = compareEqualityResult("2017-08-01T08:00Z",
                createMap(thingTypeCodeAsset, serialNumberAsset,
                        new Document("status", "Open")), Boolean.FALSE);

        // create thing TAG
        Map<String, Object> mapTag = compareEqualityResult("2017-08-01T08:10Z",
                createMap(thingTypeCodeTag, serialNumberTAG, new Document("zone", "Enance")), Boolean.FALSE);

        // create thing SO
        Map<String, Object> mapSO = compareEqualityResult("2017-08-01T08:20Z",
                createMap(thingTypeCodeSO, serialNumberSO, null), Boolean.FALSE);

        // update ASSET with TAG and SO
        mapUpdates.put("2017-08-01T08:30Z", createMap(thingTypeCodeAsset, serialNumberAsset,
                new Document("shippingOrderField", serialNumberSO),
                Collections.<Document>emptyList(),
                Collections.<Document>singletonList(
                        new Document("serialNumber", serialNumberTAG)
                                .append("thingTypeCode", thingTypeCodeTag))));
        mapAsset = executeCreateAndUpdateThing(mapUpdates, (Long) mapAsset.get("_id"));

        mapTag = getCurrentThing((Long) mapTag.get("_id"));
        mapSO = getCurrentThing((Long) mapSO.get("_id"));
        // expected time
        mapResponseExpected.put("countOpenAsset.time", "2017-08-01T08:30Z");
        mapResponseExpected.put("countAsset.time", "2017-08-01T08:30Z");

        checkStructureSOAssetTagAssociate(mapSO, mapAsset, mapTag, serialNumberSO, serialNumberAsset, serialNumberTAG);

        // delete things
        deleteThing(mapTag);
        deleteThing(mapAsset);
        deleteThing(mapSO);
    }

    /**
     * <pre>
     * THING TYPE : SO = shippingorder_code, ASSET = asset_code, TAG = tag_code
     * 1) Create thing SO without udf's
     * 2) Create thing TAG with udf( zone=Enance )
     * 3) Create thing ASSET with udf( status=Open ) and child TAG
     * 4) Update thing SO with childrenUDF ASSET
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void testShippingOrderAssetTag0012() throws Exception {
        String prefix = "SOASSTAG012";
        String serialNumberSO = "SO0000" + prefix;
        String serialNumberAsset = "ASSET000" + prefix;
        String serialNumberTAG = "TAG000" + prefix;

        // create thing SO
        Map<String, Object> mapSO = compareEqualityResult("2017-08-01T08:00Z",
                createMap(thingTypeCodeSO, serialNumberSO, null), Boolean.FALSE);

        // create thing TAG
        Map<String, Object> mapTag = compareEqualityResult("2017-08-01T08:10Z",
                createMap(thingTypeCodeTag, serialNumberTAG, new Document("zone", "Enance")), Boolean.FALSE);

        // create thing ASSET
        Map<String, Object> mapAsset = compareEqualityResult("2017-08-01T08:20Z",
                createMap(thingTypeCodeAsset, serialNumberAsset,
                        new Document("status", "Open"),
                        Collections.<Document>emptyList(),
                        Collections.<Document>singletonList(
                                new Document("serialNumber", serialNumberTAG)
                                        .append("thingTypeCode", thingTypeCodeTag))), Boolean.FALSE);

        // update SO with ASSET
        mapUpdates.put("2017-08-01T08:30Z", createMap(thingTypeCodeSO, serialNumberSO, null,
                Collections.<Document>singletonList(new Document("serialNumber", serialNumberAsset)
                        .append("thingTypeCode", thingTypeCodeAsset)
                        .append("udfs",
                                new Document("shippingOrderField",
                                        new Document("value", serialNumberSO)))),
                Collections.<Document>emptyList()));
        mapSO = executeCreateAndUpdateThing(mapUpdates, (Long) mapSO.get("_id"));

        mapTag = getCurrentThing((Long) mapTag.get("_id"));
        mapAsset = getCurrentThing((Long) mapAsset.get("_id"));

        // expected time
        mapResponseExpected.put("countOpenAsset.time", "2017-08-01T08:30Z");
        mapResponseExpected.put("countAsset.time", "2017-08-01T08:30Z");

        checkStructureSOAssetTagAssociate(mapSO, mapAsset, mapTag, serialNumberSO, serialNumberAsset, serialNumberTAG);

        // delete things
        deleteThing(mapTag);
        deleteThing(mapAsset);
        deleteThing(mapSO);
    }

    /**
     * <pre>
     * THING TYPE : SO = shippingorder_code, ASSET = asset_code, TAG = tag_code
     * 1) Create thing SO without udf's
     * 2) Create thing ASSET with udf( status=Open )
     * 3) Create thing TAG with udf( zone=Enance ) and parent udf ASSET
     * 4) Update thing SO with childrenUDF ASSET
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void testShippingOrderAssetTag0013() throws Exception {
        String prefix = "SOASSTAG013";
        String serialNumberSO = "SO0000" + prefix;
        String serialNumberAsset = "ASSET000" + prefix;
        String serialNumberTAG = "TAG000" + prefix;

        // create thing SO
        Map<String, Object> mapSO = compareEqualityResult("2017-08-01T08:00Z",
                createMap(thingTypeCodeSO, serialNumberSO, null), Boolean.FALSE);

        // create thing ASSET
        Map<String, Object> mapAsset = compareEqualityResult("2017-08-01T08:00Z",
                createMap(thingTypeCodeAsset, serialNumberAsset,
                        new Document("status", "Open")), Boolean.FALSE);

        // create thing TAG
        Map<String, Object> mapTag = compareEqualityResult("2017-08-01T08:10Z",
                createMap(thingTypeCodeTag, serialNumberTAG, new Document("zone", "Enance"),
                        new Document("serialNumber", serialNumberAsset)
                                .append("thingTypeCode", thingTypeCodeAsset)
                                .append("id", mapAsset.get("_id"))), Boolean.FALSE);

        // update SO with ASSET
        mapUpdates.put("2017-08-01T08:30Z", createMap(thingTypeCodeSO, serialNumberSO, null,
                Collections.<Document>singletonList(new Document("serialNumber", serialNumberAsset)
                        .append("thingTypeCode", thingTypeCodeAsset)
                        .append("udfs",
                                new Document("shippingOrderField",
                                        new Document("value", serialNumberSO)))),
                Collections.<Document>emptyList()));
        mapSO = executeCreateAndUpdateThing(mapUpdates, (Long) mapSO.get("_id"));

        mapTag = getCurrentThing((Long) mapTag.get("_id"));
        mapAsset = getCurrentThing((Long) mapAsset.get("_id"));

        // expected time
        mapResponseExpected.put("countOpenAsset.time", "2017-08-01T08:30Z");
        mapResponseExpected.put("countAsset.time", "2017-08-01T08:30Z");

        checkStructureSOAssetTagAssociate(mapSO, mapAsset, mapTag, serialNumberSO, serialNumberAsset, serialNumberTAG);

        // delete things
        deleteThing(mapTag);
        deleteThing(mapAsset);
        deleteThing(mapSO);
    }

    /**
     * <pre>
     * THING TYPE : SO = shippingorder_code, ASSET = asset_code, TAG = tag_code
     * 1) Create thing SO without udf's
     * 2) Create thing ASSET with udf( status=Open )
     * 3) Create thing TAG with udf( zone=Enance )
     * 4) Update thing SO with childrenUDF ASSET
     * 5) Update thing ASSET with child TAG
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void testShippingOrderAssetTag0014() throws Exception {
        String prefix = "SOASSTAG014";
        String serialNumberSO = "SO0000" + prefix;
        String serialNumberAsset = "ASSET000" + prefix;
        String serialNumberTAG = "TAG000" + prefix;

        // create thing SO
        Map<String, Object> mapSO = compareEqualityResult("2017-08-01T08:00Z",
                createMap(thingTypeCodeSO, serialNumberSO, null), Boolean.FALSE);

        // create thing ASSET
        Map<String, Object> mapAsset = compareEqualityResult("2017-08-01T08:10Z",
                createMap(thingTypeCodeAsset, serialNumberAsset,
                        new Document("status", "Open")), Boolean.FALSE);

        // create thing TAG
        Map<String, Object> mapTag = compareEqualityResult("2017-08-01T08:20Z",
                createMap(thingTypeCodeTag, serialNumberTAG, new Document("zone", "Enance")), Boolean.FALSE);

        // update SO with ASSET
        mapUpdates.put("2017-08-01T08:30Z", createMap(thingTypeCodeSO, serialNumberSO, null,
                Collections.<Document>singletonList(new Document("serialNumber", serialNumberAsset)
                        .append("thingTypeCode", thingTypeCodeAsset)
                        .append("udfs",
                                new Document("shippingOrderField",
                                        new Document("value", serialNumberSO)))),
                Collections.<Document>emptyList()));
        mapSO = executeCreateAndUpdateThing(mapUpdates, (Long) mapSO.get("_id"));

        // update ASSET with TAG
        mapUpdates.put("2017-08-01T08:40Z", createMap(thingTypeCodeAsset, serialNumberAsset, null,
                Collections.<Document>emptyList(),
                Collections.<Document>singletonList(
                        new Document("serialNumber", serialNumberTAG)
                                .append("thingTypeCode", thingTypeCodeTag))));
        mapAsset = executeCreateAndUpdateThing(mapUpdates, (Long) mapAsset.get("_id"));


        mapTag = getCurrentThing((Long) mapTag.get("_id"));
        mapSO = getCurrentThing((Long) mapSO.get("_id"));

        // expected time
        mapResponseExpected.put("countOpenAsset.time", "2017-08-01T08:40Z");
        mapResponseExpected.put("countAsset.time", "2017-08-01T08:40Z");

        checkStructureSOAssetTagAssociate(mapSO, mapAsset, mapTag, serialNumberSO, serialNumberAsset, serialNumberTAG);

        // delete things
        deleteThing(mapTag);
        deleteThing(mapAsset);
        deleteThing(mapSO);
    }

    /**
     * <pre>
     * THING TYPE : SO = shippingorder_code, ASSET = asset_code, TAG = tag_code
     * 1) Create thing SO without udf's
     * 2) Create thing ASSET with udf( status=Open )
     * 3) Create thing TAG with udf( zone=Enance )
     * 4) Update thing SO with childrenUDF ASSET
     * 5) Update thing TAG with parent ASSET
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void testShippingOrderAssetTag0015() throws Exception {
        String prefix = "SOASSTAG015";
        String serialNumberSO = "SO0000" + prefix;
        String serialNumberAsset = "ASSET000" + prefix;
        String serialNumberTAG = "TAG000" + prefix;

        // create thing SO
        Map<String, Object> mapSO = compareEqualityResult("2017-08-01T08:00Z",
                createMap(thingTypeCodeSO, serialNumberSO, null), Boolean.FALSE);

        // create thing ASSET
        Map<String, Object> mapAsset = compareEqualityResult("2017-08-01T08:10Z",
                createMap(thingTypeCodeAsset, serialNumberAsset,
                        new Document("status", "Open")), Boolean.FALSE);

        // create thing TAG
        Map<String, Object> mapTag = compareEqualityResult("2017-08-01T08:20Z",
                createMap(thingTypeCodeTag, serialNumberTAG, new Document("zone", "Enance")), Boolean.FALSE);

        // update SO with ASSET
        mapUpdates.put("2017-08-01T08:30Z", createMap(thingTypeCodeSO, serialNumberSO, null,
                Collections.<Document>singletonList(new Document("serialNumber", serialNumberAsset)
                        .append("thingTypeCode", thingTypeCodeAsset)
                        .append("udfs",
                                new Document("shippingOrderField",
                                        new Document("value", serialNumberSO)))),
                Collections.<Document>emptyList()));
        mapSO = executeCreateAndUpdateThing(mapUpdates, (Long) mapSO.get("_id"));

        // update TAG with ASSET
        mapUpdates.put("2017-08-01T08:40Z", createMap(thingTypeCodeTag, serialNumberTAG,
                new Document(), new Document("serialNumber", serialNumberAsset)
                        .append("thingTypeCode", thingTypeCodeAsset)
                        .append("id", mapAsset.get("_id"))));
        mapTag = executeCreateAndUpdateThing(mapUpdates, (Long) mapTag.get("_id"));

        mapAsset = getCurrentThing((Long) mapAsset.get("_id"));
        mapSO = getCurrentThing((Long) mapSO.get("_id"));

        // expected time
        mapResponseExpected.put("countOpenAsset.time", "2017-08-01T08:30Z");
        mapResponseExpected.put("countAsset.time", "2017-08-01T08:30Z");

        checkStructureSOAssetTagAssociate(mapSO, mapAsset, mapTag, serialNumberSO, serialNumberAsset, serialNumberTAG);

        // delete things
        deleteThing(mapTag);
        deleteThing(mapAsset);
        deleteThing(mapSO);
    }

    /**
     * <pre>
     * THING TYPE : SO = shippingorder_code, ASSET = asset_code, TAG = tag_code
     * 1) Create thing SO without udf's
     * 2) Create thing ASSET with udf( status=Open )
     * 3) Create thing TAG with udf( zone=Enance )
     * 4) Update thing ASSET with child TAG
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void testShippingOrderAssetTag0016() throws Exception {
        String prefix = "SOASSTAG016A";
        String serialNumberSO = "SO0000" + prefix;
        String serialNumberAsset = "ASSET000" + prefix;
        String serialNumberTAG = "TAG000" + prefix;

        // create thing SO
        Map<String, Object> mapSO = compareEqualityResult("2017-08-01T08:00Z",
                createMap(thingTypeCodeSO, serialNumberSO, null), Boolean.FALSE);

        // create thing ASSET
        Map<String, Object> mapAsset = compareEqualityResult("2017-08-01T08:10Z",
                createMap(thingTypeCodeAsset, serialNumberAsset,
                        new Document("status", "Open")), Boolean.FALSE);

        // create thing TAG
        Map<String, Object> mapTag = compareEqualityResult("2017-08-01T08:20Z",
                createMap(thingTypeCodeTag, serialNumberTAG, new Document("zone", "Enance")), Boolean.FALSE);

        // update ASSET with TAG
        mapUpdates.put("2017-08-01T08:30Z", createMap(thingTypeCodeAsset, serialNumberAsset, null,
                Collections.<Document>emptyList(),
                Collections.<Document>singletonList(
                        new Document("serialNumber", serialNumberTAG)
                                .append("thingTypeCode", thingTypeCodeTag))));
        mapAsset = executeCreateAndUpdateThing(mapUpdates, (Long) mapAsset.get("_id"));


        // update SO with ASSET
        mapUpdates.put("2017-08-01T08:40Z", createMap(thingTypeCodeSO, serialNumberSO, null,
                Collections.<Document>singletonList(new Document("serialNumber", serialNumberAsset)
                        .append("thingTypeCode", thingTypeCodeAsset)
                        .append("udfs",
                                new Document("shippingOrderField",
                                        new Document("value", serialNumberSO)))),
                Collections.<Document>emptyList()));
        mapSO = executeCreateAndUpdateThing(mapUpdates, (Long) mapSO.get("_id"));

        mapAsset = getCurrentThing((Long) mapAsset.get("_id"));
        mapTag = getCurrentThing((Long) mapTag.get("_id"));

        // expected time
        mapResponseExpected.put("countOpenAsset.time", "2017-08-01T08:40Z");
        mapResponseExpected.put("countAsset.time", "2017-08-01T08:40Z");

        checkStructureSOAssetTagAssociate(mapSO, mapAsset, mapTag, serialNumberSO, serialNumberAsset, serialNumberTAG);

        // delete things
        deleteThing(mapTag);
        deleteThing(mapAsset);
        deleteThing(mapSO);
    }

    /**
     * <pre>
     * THING TYPE : SO = shippingorder_code, ASSET = asset_code, TAG = tag_code
     * 1) Create thing SO without udf's
     * 2) Create thing ASSET with udf( status=Open )
     * 3) Create thing TAG with udf( zone=Enance )
     * 4)
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void testShippingOrderAssetTag0017() throws Exception {
        String prefix = "SOASSTAG017";
        String serialNumberSO = "SO0000" + prefix;
        String serialNumberAsset = "ASSET000" + prefix;
        String serialNumberTAG = "TAG000" + prefix;

        // create thing SO
        Map<String, Object> mapSO = compareEqualityResult("2017-08-01T08:00Z",
                createMap(thingTypeCodeSO, serialNumberSO, null), Boolean.FALSE);

        // create thing ASSET
        Map<String, Object> mapAsset = compareEqualityResult("2017-08-01T08:10Z",
                createMap(thingTypeCodeAsset, serialNumberAsset,
                        new Document("status", "Open")), Boolean.FALSE);

        // create thing TAG
        Map<String, Object> mapTag = compareEqualityResult("2017-08-01T08:20Z",
                createMap(thingTypeCodeTag, serialNumberTAG, new Document("zone", "Enance")), Boolean.FALSE);

        // update TAG with ASSET
        mapUpdates.put("2017-08-01T08:30Z", createMap(thingTypeCodeTag, serialNumberTAG,
                new Document(), new Document("serialNumber", serialNumberAsset)
                        .append("thingTypeCode", thingTypeCodeAsset)
                        .append("id", mapAsset.get("_id"))));
        mapTag = executeCreateAndUpdateThing(mapUpdates, (Long) mapTag.get("_id"));

        // update SO with ASSET
        mapUpdates.put("2017-08-01T08:40Z", createMap(thingTypeCodeSO, serialNumberSO, null,
                Collections.<Document>singletonList(new Document("serialNumber", serialNumberAsset)
                        .append("thingTypeCode", thingTypeCodeAsset)
                        .append("udfs",
                                new Document("shippingOrderField",
                                        new Document("value", serialNumberSO)))),
                Collections.<Document>emptyList()));
        mapSO = executeCreateAndUpdateThing(mapUpdates, (Long) mapSO.get("_id"));

        mapAsset = getCurrentThing((Long) mapAsset.get("_id"));
        mapTag = getCurrentThing((Long) mapTag.get("_id"));

        // expected time
        mapResponseExpected.put("countOpenAsset.time", "2017-08-01T08:40Z");
        mapResponseExpected.put("countAsset.time", "2017-08-01T08:40Z");

        checkStructureSOAssetTagAssociate(mapSO, mapAsset, mapTag, serialNumberSO, serialNumberAsset, serialNumberTAG);

        // delete things
        deleteThing(mapTag);
        deleteThing(mapAsset);
        deleteThing(mapSO);
    }

    /**
     * <pre>
     * THING TYPE : SO = shippingorder_code, ASSET = asset_code, TAG = tag_code
     * 1) Create thing ASSET with udf( status=Open )
     * 2) Create thing TAG with udf( zone=Enance ) and paren ASSET
     * 3) Create thing SO with childrenUDF ASSET
     * 4)
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void testShippingOrderAssetTag0018() throws Exception {
        String prefix = "SOASSTAG018";
        String serialNumberSO = "SO0000" + prefix;
        String serialNumberAsset = "ASSET000" + prefix;
        String serialNumberTAG = "TAG000" + prefix;

        // create thing ASSET
        Map<String, Object> mapAsset = compareEqualityResult("2017-08-01T08:00Z",
                createMap(thingTypeCodeAsset, serialNumberAsset,
                        new Document("status", "Open")), Boolean.FALSE);

        // create thing TAG
        Map<String, Object> mapTag = compareEqualityResult("2017-08-01T08:10Z",
                createMap(thingTypeCodeTag, serialNumberTAG, new Document("zone", "Enance"),
                        new Document("serialNumber", serialNumberAsset)
                                .append("thingTypeCode", thingTypeCodeAsset)
                                .append("id", mapAsset.get("_id"))), Boolean.FALSE);

        // create thing SO
        Map<String, Object> mapSO = compareEqualityResult("2017-08-01T08:10Z",
                createMap(
                        thingTypeCodeSO, serialNumberSO, null,
                        Collections.<Document>singletonList(
                                new Document("serialNumber", serialNumberAsset)
                                        .append("thingTypeCode", thingTypeCodeAsset)
                                        .append("udfs",
                                                new Document("shippingOrderField",
                                                        new Document("value", serialNumberSO)))),
                        Collections.<Document>emptyList()
                ), Boolean.FALSE);

        mapAsset = getCurrentThing((Long) mapAsset.get("_id"));
        mapTag = getCurrentThing((Long) mapTag.get("_id"));

        // expected time
        mapResponseExpected.put("countOpenAsset.time", "2017-08-01T08:10Z");
        mapResponseExpected.put("countAsset.time", "2017-08-01T08:10Z");

        checkStructureSOAssetTagAssociate(mapSO, mapAsset, mapTag, serialNumberSO, serialNumberAsset, serialNumberTAG);

        // delete things
        deleteThing(mapTag);
        deleteThing(mapAsset);
        deleteThing(mapSO);
    }

    private void checkStructureSOAssetTagAssociate(Map<String, Object> mapSO,
                                                   Map<String, Object> mapAsset,
                                                   Map<String, Object> mapTag,
                                                   String serialNumberSO,
                                                   String serialNumberAsset,
                                                   String serialNumberTAG) throws Exception {
        initTransaction(); // for queries

        // validate thing SO
        mapSO = getCurrentThing((Long) mapSO.get("_id"));
        mapResponseExpected.put("countOpenAsset.value", "1");
        mapResponseExpected.put("countAsset.value", "1");
        mapResponseExpected.put("$.asset_code_children.length()", 1);
        mapResponseExpected.put("$.asset_code_children[*].serialNumber", array(serialNumberAsset));
        mapResponseExpected.put("$.asset_code_children[*].status.value", array("Open"));
        mapResponseExpected.put("$.asset_code_children[*].children[*].serialNumber", array(serialNumberTAG));
        validateResultExpected(mapSO, mapResponseExpected);

        // validate thing ASSET
        mapAsset = getCurrentThing((Long) mapAsset.get("_id"));
        mapResponseExpected.put("shippingOrderField.value.serialNumber", serialNumberSO);
        mapResponseExpected.put("shippingOrderField.value.countOpenAsset.value", "1");
        mapResponseExpected.put("shippingOrderField.value.countAsset.value", "1");
        mapResponseExpected.put("shippingOrderField.thingTypeFieldId", -1);
        mapResponseExpected.put("status.value", "Open");
        mapResponseExpected.put("$.children[*].serialNumber", array(serialNumberTAG));
        mapResponseExpected.put("$.children[*].zone.value.code", array("Enance"));
        validateResultExpected(mapAsset, mapResponseExpected);

        // validate thing TAG
        mapResponseExpected.put("zone.value.code", "Enance");
        mapResponseExpected.put("parent.serialNumber", serialNumberAsset);
        mapResponseExpected.put("parent.status.value", "Open");
        mapResponseExpected.put("parent.shippingOrderField.thingTypeFieldId", -1);
        mapResponseExpected.put("parent.shippingOrderField.value.serialNumber", serialNumberSO);
        mapResponseExpected.put("parent.shippingOrderField.value.countOpenAsset.value", "1");
        mapResponseExpected.put("parent.shippingOrderField.value.countAsset.value", "1");
        validateResultExpected(mapTag, mapResponseExpected);

        // check database
        Thing thingAsset = ThingService.getInstance().get((Long) mapAsset.get("_id"));
        Thing thingTag = ThingService.getInstance().get((Long) mapTag.get("_id"));
        Thing thingSO = ThingService.getInstance().get((Long) mapSO.get("_id"));

        assertThat(thingAsset, is(notNullValue()));
        assertThat(thingTag, is(notNullValue()));
        assertThat(thingSO, is(notNullValue()));
        assertThat(thingAsset.getParent(), is(nullValue()));
        assertThat("Parent is null in Tag", thingTag.getParent(), is(notNullValue()));
        assertThat("Error in parent tag", thingTag.getParent().getId(), is(equalTo(thingAsset.getId())));
    }

    private void checkStructureSOAssetTagDisassociate(Map<String, Object> mapSO,
                                                      Map<String, Object> mapAsset,
                                                      Map<String, Object> mapTag) throws Exception {
        initTransaction(); // for queries
        if (mapSO != null && mapAsset != null) {
            mapResponseExpected.put("countAsset.value", "1");
            mapResponseExpected.put("countOpenAsset.value", "1");
            mapResponseExpected.put("asset_code_children.length()", 1);
            mapResponseExpected.put("$.asset_code_children[*].children.length()", array(0));
            validateResultExpected(mapSO, mapResponseExpected);

            mapResponseExpected.put("status.value", "Open");
            mapResponseExpected.put("shippingOrderField.value.countOpenAsset.value", "1");
            mapResponseExpected.put("shippingOrderField.value.countAsset.value", "1");
            mapResponseExpected.put("children.length()", 0);
            validateResultExpected(mapAsset, mapResponseExpected);
        }
        if (mapSO != null && mapTag != null) {
//        mapResponseExpected.put("countAsset.value", "0");
//        mapResponseExpected.put("countOpenAsset.value", "0");
            mapResponseExpected.put("asset_code_children.length()", 0);
            validateResultExpected(mapSO, mapResponseExpected);

            mapResponseExpected.put("parent", null);
            validateResultExpected(mapTag, mapResponseExpected);

            Thing thingTag = ThingService.getInstance().get((Long) mapTag.get("_id"));
            assertThat(thingTag, is(notNullValue()));
            assertThat(thingTag.getParent(), is(nullValue()));
        }
        if (mapAsset != null && mapTag != null) {
            mapResponseExpected.put("status.value", "Open");
            mapResponseExpected.put("children.length()", 1);
            mapResponseExpected.put("$.children[*].zone.value.code", array("Enance"));
            validateResultExpected(mapAsset, mapResponseExpected);

            mapResponseExpected.put("zone.value.code", "Enance");
            mapResponseExpected.put("parent.status.value", "Open");
            validateResultExpected(mapTag, mapResponseExpected);
        }
    }

}
