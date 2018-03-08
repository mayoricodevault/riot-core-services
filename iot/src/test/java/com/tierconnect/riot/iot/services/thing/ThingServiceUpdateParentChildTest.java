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
public class ThingServiceUpdateParentChildTest extends ThingServiceUpdateTest {

    /**
     * <pre>
     * THING TYPE : ASSET = asset_code, TAG = tag_code
     * 1) Create thing ASSET with udf( status=Open )
     * 2) Create thing TAG with udf ( active=YES )
     * 3) Update thing ASSET with child TAG
     * 4)
     * 4)
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void createUpdateAssetTag0001() throws Exception {
        String prefix = "ASSTAG01";
        String serialNumberAsset = "ASSET000" + prefix;
        String serialNumberTAG = "TAG000" + prefix;

        // create thing ASSET
        Map<String, Object> mapAsset = compareEqualityResult("2017-08-01T08:00Z",
                createMap(thingTypeCodeAsset, serialNumberAsset,
                        new Document("status", "Open")), Boolean.FALSE);

        // create thing TAG
        Map<String, Object> mapTag = compareEqualityResult("2017-08-01T08:10Z",
                createMap(thingTypeCodeTag, serialNumberTAG, new Document("active", true)), Boolean.FALSE);

        // update ASSET with TAG
        mapUpdates.put("2017-08-01T08:20Z", createMap(thingTypeCodeAsset, serialNumberAsset, null, null,
                Collections.<Document>singletonList(
                        new Document("serialNumber", serialNumberTAG)
                                .append("thingTypeCode", thingTypeCodeTag))));
        mapAsset = executeCreateAndUpdateThing(mapUpdates, (Long) mapAsset.get("_id"));

        mapTag = getCurrentThing((Long) mapTag.get("_id"));

        // time
        mapResponseExpected.put("active.time", "2017-08-01T08:10Z");
        mapResponseExpected.put("parent.status.time", "2017-08-01T08:00Z");
        mapResponseExpected.put("time", "2017-08-01T08:20Z");
        validateResultExpected(mapTag, mapResponseExpected);

        mapResponseExpected.put("status.time", "2017-08-01T08:00Z");
        mapResponseExpected.put("$.children[*].active.time", array("2017-08-01T08:10Z"));
        mapResponseExpected.put("time", "2017-08-01T08:20Z");
        checkStructureAssetTagAssociate(serialNumberAsset, mapAsset, serialNumberTAG, mapTag);

        // delete ASSET
        deleteThing(mapAsset);
        mapTag = getCurrentThing((Long) mapTag.get("_id"));
        checkStructureAssetTagDisassociate(null, mapTag);
        // delete TAG
        deleteThing(mapTag);
    }

    /**
     * <pre>
     * THING TYPE : ASSET = asset_code, TAG = tag_code
     * 1) Create thing ASSET with udf( status=Open )
     * 2) Create thing TAG with udf ( active=YES )
     * 3) Update thing TAG with parent ASSET
     * 4)
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void createUpdateAssetTag0002() throws Exception {
        String prefix = "ASSTAG02";
        String serialNumberAsset = "ASSET000" + prefix;
        String serialNumberTAG = "TAG000" + prefix;

        // create thing ASSET
        Map<String, Object> mapAsset = compareEqualityResult("2017-08-01T08:00Z",
                createMap(thingTypeCodeAsset, serialNumberAsset,
                        new Document("status", "Open")), Boolean.FALSE);

        // create thing TAG
        Map<String, Object> mapTag = compareEqualityResult("2017-08-01T08:10Z",
                createMap(thingTypeCodeTag, serialNumberTAG, new Document("active", true)), Boolean.FALSE);

        // update TAG with ASSET
        mapUpdates.put("2017-08-01T08:20Z", createMap(thingTypeCodeTag, serialNumberTAG,
                new Document(), new Document("serialNumber", serialNumberAsset)
                        .append("thingTypeCode", thingTypeCodeAsset)
                        .append("id", mapAsset.get("_id"))));
        mapTag = executeCreateAndUpdateThing(mapUpdates, (Long) mapTag.get("_id"));

        // time
        mapResponseExpected.put("active.time", "2017-08-01T08:10Z");
        mapResponseExpected.put("parent.status.time", "2017-08-01T08:00Z");
        mapResponseExpected.put("time", "2017-08-01T08:20Z");
        validateResultExpected(mapTag, mapResponseExpected);

        mapAsset = getCurrentThing((Long) mapAsset.get("_id"));
        mapResponseExpected.put("status.time", "2017-08-01T08:00Z");
        mapResponseExpected.put("$.children[*].active.time", array("2017-08-01T08:10Z"));
        mapResponseExpected.put("time", "2017-08-01T08:00Z");
        checkStructureAssetTagAssociate(serialNumberAsset, mapAsset, serialNumberTAG, mapTag);

        // delete TAG
        deleteThing(mapTag);
        mapAsset = getCurrentThing((Long) mapAsset.get("_id"));
        checkStructureAssetTagDisassociate(mapAsset, null);
        // delete ASSET
        deleteThing(mapAsset);
    }

    /**
     * <pre>
     * THING TYPE : ASSET = asset_code, TAG = tag_code
     * 1) Create thing ASSET with udf( status=Open )
     * 2) Create thing TAG with udf ( active=YES ) and parent ASSET
     * 3)
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void createUpdateAssetTag0003() throws Exception {
        String prefix = "ASSTAG03";
        String serialNumberAsset = "ASSET000" + prefix;
        String serialNumberTAG = "TAG000" + prefix;

        // create thing ASSET
        Map<String, Object> mapAsset = compareEqualityResult("2017-08-01T08:00Z",
                createMap(thingTypeCodeAsset, serialNumberAsset,
                        new Document("status", "Open")), Boolean.FALSE);

        // create thing TAG
        Map<String, Object> mapTag = compareEqualityResult("2017-08-01T08:10Z",
                createMap(thingTypeCodeTag, serialNumberTAG, new Document("active", true),
                        new Document("serialNumber", serialNumberAsset)
                                .append("thingTypeCode", thingTypeCodeAsset)
                                .append("id", mapAsset.get("_id"))), Boolean.FALSE);

        // time
        mapResponseExpected.put("active.time", "2017-08-01T08:10Z");
        mapResponseExpected.put("parent.status.time", "2017-08-01T08:00Z");
        mapResponseExpected.put("time", "2017-08-01T08:10Z");
        validateResultExpected(mapTag, mapResponseExpected);

        mapAsset = getCurrentThing((Long) mapAsset.get("_id"));
        mapResponseExpected.put("status.time", "2017-08-01T08:00Z");
        mapResponseExpected.put("$.children[*].active.time", array("2017-08-01T08:10Z"));
        mapResponseExpected.put("time", "2017-08-01T08:00Z");
        checkStructureAssetTagAssociate(serialNumberAsset, mapAsset, serialNumberTAG, mapTag);

        // disassociate ASSET of TAG
        mapUpdates.put("2017-08-01T08:20Z", createMap(thingTypeCodeTag, serialNumberTAG, new Document(),
                new Document()));

        mapTag = executeCreateAndUpdateThing(mapUpdates, (Long) mapTag.get("_id"));
        mapAsset = getCurrentThing((Long) mapAsset.get("_id"));

        checkStructureAssetTagDisassociate(mapAsset, mapTag);

        // delete ASSET and TAG
        deleteThing(mapAsset);
        deleteThing(mapTag);
    }

    /**
     * <pre>
     * THING TYPE : ASSET = asset_code, TAG = tag_code
     * 1) Create thing TAG with udf ( active=YES )
     * 2) Create thing ASSET with udf( status=Open ) and child ASSET
     * 3)
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void createUpdateAssetTag0004() throws Exception {
        String prefix = "ASSTAG04";
        String serialNumberAsset = "ASSET000" + prefix;
        String serialNumberTAG = "TAG000" + prefix;

        // create thing TAG
        Map<String, Object> mapTag = compareEqualityResult("2017-08-01T08:00Z",
                createMap(thingTypeCodeTag, serialNumberTAG, new Document("active", true)), Boolean.FALSE);

        // create thing ASSET
        Map<String, Object> mapAsset = compareEqualityResult("2017-08-01T08:10Z",
                createMap(thingTypeCodeAsset, serialNumberAsset, new Document("status", "Open"), null,
                        Collections.<Document>singletonList(
                                new Document("serialNumber", serialNumberTAG)
                                        .append("thingTypeCode", thingTypeCodeTag))), Boolean.FALSE);

        // time
        mapTag = getCurrentThing((Long) mapTag.get("_id"));
        mapResponseExpected.put("active.time", "2017-08-01T08:00Z");
        mapResponseExpected.put("parent.status.time", "2017-08-01T08:10Z");
        mapResponseExpected.put("time", "2017-08-01T08:00Z");
        validateResultExpected(mapTag, mapResponseExpected);

        mapResponseExpected.put("status.time", "2017-08-01T08:10Z");
        mapResponseExpected.put("$.children[*].active.time", array("2017-08-01T08:00Z"));
        mapResponseExpected.put("time", "2017-08-01T08:10Z");
        checkStructureAssetTagAssociate(serialNumberAsset, mapAsset, serialNumberTAG, mapTag);

        // disassociate TAG of ASSET
        mapUpdates.put("2017-08-01T08:20Z", createMap(thingTypeCodeAsset, serialNumberAsset, null, null,
                Collections.<Document>emptyList()));

        mapAsset = executeCreateAndUpdateThing(mapUpdates, (Long) mapAsset.get("_id"));
        mapTag = getCurrentThing((Long) mapTag.get("_id"));

        checkStructureAssetTagDisassociate(mapAsset, mapTag);

        // delete ASSET and TAG
        deleteThing(mapAsset);
        deleteThing(mapTag);
    }

    private void checkStructureAssetTagAssociate(String serialNumberAsset, Map<String, Object> mapAsset,
                                                 String serialNumberTAG, Map<String, Object> mapTag) throws Exception {
        // validate Asset
        mapResponseExpected.put("status.thingTypeFieldId", -1);
        mapResponseExpected.put("status.value", "Open");
        mapResponseExpected.put("$.children.length()", 1);
        mapResponseExpected.put("$.children[*].serialNumber", array(serialNumberTAG));
        mapResponseExpected.put("$.children[*].active.value", array(true));
        validateResultExpected(mapAsset, mapResponseExpected);

        // validate Tag
        mapResponseExpected.put("active.thingTypeFieldId", -1);
        mapResponseExpected.put("active.value", true);
        mapResponseExpected.put("parent.serialNumber", serialNumberAsset);
        mapResponseExpected.put("parent.status.thingTypeFieldId", -1);
        mapResponseExpected.put("parent.status.value", "Open");
        validateResultExpected(mapTag, mapResponseExpected);

        // check database
        Thing thingAsset = ThingService.getInstance().get((Long) mapAsset.get("_id"));
        Thing thingTag = ThingService.getInstance().get((Long) mapTag.get("_id"));

        assertThat(thingAsset, is(notNullValue()));
        assertThat(thingTag, is(notNullValue()));
        assertThat(thingAsset.getParent(), is(nullValue()));
        assertThat("Parent is null in Tag", thingTag.getParent(), is(notNullValue()));
        assertThat("Error in parent tag", thingTag.getParent().getId(), is(equalTo(thingAsset.getId())));
    }

    private void checkStructureAssetTagDisassociate(Map<String, Object> mapAsset,
                                                    Map<String, Object> mapTag) throws Exception {
        initTransaction(); // for queries
        if (mapAsset != null) {
//            mapResponseExpected.put("time", "2017-08-01T08:20Z"); //TODO: bug time in associate/diassociate ASSET>TAG
            mapResponseExpected.put("children.length()", 0);
            validateResultExpected(mapAsset, mapResponseExpected);

            // check database
            Thing thingAsset = ThingService.getInstance().get((Long) mapAsset.get("_id"));
            assertThat(thingAsset, is(notNullValue()));
            assertThat(thingAsset.getParent(), is(nullValue()));
        }
        if (mapTag != null) {
//            mapResponseExpected.put("time", "2017-08-01T08:20Z"); //TODO: bug time in associate/diassociate ASSET>TAG
            mapResponseExpected.put("parent", null);
            validateResultExpected(mapTag, mapResponseExpected);

            // check database
            Thing thingTag = ThingService.getInstance().get((Long) mapTag.get("_id"));
            assertThat(thingTag, is(notNullValue()));
            assertThat(thingTag.getParent(), is(nullValue()));
        }
    }
}
