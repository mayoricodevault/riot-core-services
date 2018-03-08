package com.tierconnect.riot.iot.services.thingType;

import com.tierconnect.riot.iot.entities.ThingType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.tierconnect.riot.iot.entities.ThingTypeField.Type.*;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static com.tierconnect.riot.iot.services.thingType.ThingTypeUtilitiesForTest.*;

/**
 * Created by vealaro on 9/4/17.
 */
public class MultilevelThingTypeCasesTest extends ThingTypeServiceTest {

    /**
     * <pre>
     * Simple Parent-Child native
     *
     * ASSET
     * |-TAG
     * </pre>
     */
    @Test
    public void case_1_parentChild() {
        String preffix = "MUL_CASE_1_";
        String ttParentAsset = preffix + "ASSET_CUSTOM";
        String ttChildTag = preffix + "TAG_CUSTOM";

        // create Asset
        List<Map<String, Object>> ttFieldsAsset = new ArrayList<>();
        ttFieldsAsset.add(getMapFieldStandard("status", Boolean.FALSE, TYPE_TEXT));
        Map<String, Object> mapAsset = getMapThingTypeCustom(ttParentAsset, ttFieldsAsset);
        ThingType thingTypeAsset = executeTest(ttParentAsset, mapAsset, ttFieldsAsset);

        // create Tag with asset parent
        List<Map<String, Object>> ttFieldsTag = getTagFields();
        Map<String, Object> mapTag = getMapThingTypeCustom(ttChildTag, Boolean.FALSE, ttFieldsTag, null,
                singletonList(thingTypeAsset.getId()));
        ThingType thingTypeTag = executeTest(ttChildTag, mapTag, ttFieldsTag);

        // check associate
        checkParentChildRelationship(thingTypeAsset, thingTypeTag);

        if (!removeThingType) return;

        // update disassociate
        mapAsset = getMapThingTypeCustom(ttParentAsset, Boolean.FALSE, ttFieldsAsset, emptyList(), null);
        thingTypeAsset = executeTest(thingTypeAsset.getId(), ttParentAsset, mapAsset, ttFieldsAsset);

        // delete all thing types
        deleteThingType(thingTypeAsset);
        deleteThingType(thingTypeTag);
    }

    /**
     * <pre>
     * Simple Parent UDF-Child
     *
     * S.O. (isParent=TRUE)
     * |-Asset (udf=S.O.)
     * </pre>
     */
    @Test
    public void case_2_ParentUDFChild() {
        String preffix = "MUL_CASE_2_";
        String ttCodeSO = preffix + "SHIPPING_ORDER_CUSTOM";
        String ttAsset = preffix + "ASSET_CUSTOM";

        // create S.O.
        List<Map<String, Object>> ttFieldsSO = new ArrayList<>();
        ttFieldsSO.add(getMapFieldStandard("countAsset", Boolean.FALSE, TYPE_FORMULA, "${count(\"\",\"\")}"));
        ttFieldsSO.add(getMapFieldStandard("countOpenAsset", Boolean.FALSE, TYPE_FORMULA, "${count(\"asset_code\",\"status.value=Open\")}"));
        ttFieldsSO.add(getMapFieldStandard("owner", Boolean.TRUE, TYPE_TEXT));
        ttFieldsSO.add(getMapFieldStandard("status", Boolean.TRUE, TYPE_TEXT));
        Map<String, Object> mapParentSO = getMapThingTypeCustom(ttCodeSO, Boolean.TRUE, ttFieldsSO);
        ThingType thingTypeSO = executeTest(ttCodeSO, mapParentSO, ttFieldsSO);

        // create asset
        List<Map<String, Object>> ttFieldsAsset = new ArrayList<>();
        ttFieldsAsset.add(getMapFieldStandard("status", Boolean.FALSE, TYPE_TEXT));
        ttFieldsAsset.add(getMapFieldThingType("shippingOrderField", Boolean.TRUE, thingTypeSO.getId(), TYPE_THING_TYPE));
        Map<String, Object> mapAsset = getMapThingTypeCustom(ttAsset, ttFieldsAsset);
        ThingType thingTypeAsset = executeTest(ttAsset, mapAsset, ttFieldsAsset);

        // check associate
        checkRelationPath(thingTypeSO, thingTypeAsset, ttAsset + "_children");
        checkRelationPath(thingTypeAsset, thingTypeSO, "shippingOrderField.value");

        if (!removeThingType) return;

        // update disassociate
        List<Map<String, Object>> newFields = removeFields(ttFieldsAsset, "shippingOrderField");
        mapAsset = getMapThingTypeCustom(ttAsset, newFields);
        thingTypeAsset = executeTest(thingTypeAsset.getId(), ttAsset, mapAsset, newFields);

        // delete all thing types
        deleteThingType(thingTypeSO);
        deleteThingType(thingTypeAsset);
    }

    /**
     * <pre>
     * Parent UDF, Parent-Child native
     *
     * S.O. (isParent=TRUE)
     * |-Asset (udf=S.O.) (parent-native)
     * |-- Tag (Child- native)
     * </pre>
     */
    @Test
    public void case_3_threeLevels() {
        String preffix = "MUL_CASE_3_";
        String ttSOCode = preffix + "SHIPPING_ORDER_CUSTOM";
        String ttAssetCode = preffix + "ASSET_CUSTOM";
        String ttTagCode = preffix + "TAG_CUSTOM";
        // create S.O.
        List<Map<String, Object>> ttFieldsSO = new ArrayList<>();
        ttFieldsSO.add(getMapFieldStandard("countAsset", Boolean.FALSE, TYPE_FORMULA, "${count(\"\",\"\")}"));
        ttFieldsSO.add(getMapFieldStandard("countOpenAsset", Boolean.FALSE, TYPE_FORMULA, "${count(\"asset_code\",\"status.value=Open\")}"));
        ttFieldsSO.add(getMapFieldStandard("owner", Boolean.TRUE, TYPE_TEXT));
        ttFieldsSO.add(getMapFieldStandard("status", Boolean.TRUE, TYPE_TEXT));
        Map<String, Object> mapParentSO = getMapThingTypeCustom(ttSOCode, Boolean.TRUE, ttFieldsSO);
        ThingType thingTypeSO = executeTest(ttSOCode, mapParentSO, ttFieldsSO);

        // create Asset with udf S.O.
        List<Map<String, Object>> ttFieldsAsset = new ArrayList<>();
        ttFieldsAsset.add(getMapFieldStandard("status", Boolean.FALSE, TYPE_TEXT));
        ttFieldsAsset.add(getMapFieldThingType("shippingOrderField", Boolean.TRUE, thingTypeSO.getId(), TYPE_THING_TYPE)); //always true
        Map<String, Object> mapAsset = getMapThingTypeCustom(ttAssetCode, Boolean.FALSE, ttFieldsAsset);
        ThingType thingTypeAsset = executeTest(ttAssetCode, mapAsset, ttFieldsAsset);

        // create tag with parent Asset
        List<Map<String, Object>> ttFieldsTag = getTagFields();
        Map<String, Object> mapTag = getMapThingTypeCustom(ttTagCode, Boolean.FALSE, ttFieldsTag, null,
                singletonList(thingTypeAsset.getId()));
        ThingType thingTypeTag = executeTest(ttTagCode, mapTag, ttFieldsTag);

        // check structure
        checkParentChildRelationship(thingTypeAsset, thingTypeTag);
        checkRelationPath(thingTypeSO, thingTypeAsset, ttAssetCode + "_children");
        checkRelationPath(thingTypeAsset, thingTypeSO, "shippingOrderField.value");
        checkRelationPath(thingTypeTag, thingTypeSO, "parent.shippingOrderField.value");
        checkRelationPath(thingTypeSO, thingTypeTag, ttAssetCode + "_children.children");

        if (!removeThingType) return;

        // update disassociate
        mapAsset = getMapThingTypeCustom(ttAssetCode, Boolean.FALSE, ttFieldsAsset, emptyList(), null);
        thingTypeAsset = executeTest(thingTypeAsset.getId(), ttAssetCode, mapAsset, ttFieldsAsset);

        List<Map<String, Object>> newFields = removeFields(ttFieldsAsset, "shippingOrderField");
        mapAsset = getMapThingTypeCustom(ttAssetCode, Boolean.FALSE, newFields);
        thingTypeAsset = executeTest(thingTypeAsset.getId(), ttAssetCode, mapAsset, newFields);

        // remove thing types
        deleteThingType(thingTypeSO);
        deleteThingType(thingTypeAsset);
        deleteThingType(thingTypeTag);
    }

    @Test
    public void case_4_threeLevels() {
    }

    @Test
    public void case_5_mixedParent() {
    }

    @Test
    public void case_6_twoParentsUDF() {
    }

    /**
     * <pre>
     * Two Parent-Child native
     *
     * JACKET
     * |- RFID
     *
     * PANTS
     * |- RFID
     * </pre>
     */
    @Test
    public void case_7_twoParentsNativeParentChild() {
        String preffix = "MUL_CASE_7_";
        String ttParentCodeJACKET = preffix + "JACKET_CUSTOM";
        String ttChildCodeRFID = preffix + "RFID_CUSTOM";
        String ttParentCodePANTS = preffix + "PANTS_CUSTOM";

        // create Jacket
        List<Map<String, Object>> ttFieldsParentJACKET = getJacketFields();
        Map<String, Object> mapParentJACKET = getMapThingTypeCustom(ttParentCodeJACKET, ttFieldsParentJACKET);
        ThingType thingTypeParentJACKET = executeTest(ttParentCodeJACKET, mapParentJACKET, ttFieldsParentJACKET);

        // create RFID with paren Jacket
        List<Map<String, Object>> ttFieldsChildRFID = getRFIDFields();
        Map<String, Object> mapChildRFID = getMapThingTypeCustom(ttChildCodeRFID, Boolean.FALSE, ttFieldsChildRFID,
                null, singletonList(thingTypeParentJACKET.getId()));
        ThingType thingTypeChildRFID = executeTest(ttChildCodeRFID, mapChildRFID, ttFieldsChildRFID);

        // create Pants with RFID child
        List<Map<String, Object>> ttFieldsParentPANTS = getPantFields();
        Map<String, Object> mapChildPANTS = getMapThingTypeCustom(ttParentCodePANTS, Boolean.FALSE, ttFieldsParentPANTS,
                null, null);
        ThingType thingTypeParentPANTS = executeTest(ttParentCodePANTS, mapChildPANTS, ttFieldsParentPANTS);

        // associate PANTS with RFID
        mapChildPANTS = getMapThingTypeCustom(ttParentCodePANTS, Boolean.FALSE, ttFieldsParentPANTS,
                singletonList(thingTypeChildRFID.getId()), null);
        thingTypeParentPANTS = executeTest(thingTypeParentPANTS.getId(), ttParentCodePANTS, mapChildPANTS, ttFieldsParentPANTS);

        // check associate
        checkParentChildRelationship(thingTypeParentJACKET, thingTypeChildRFID);
        checkParentChildRelationship(thingTypeParentPANTS, thingTypeChildRFID);

        if (!removeThingType) return;

        // update disassociate
        mapChildRFID = getMapThingTypeCustom(ttChildCodeRFID, Boolean.FALSE, ttFieldsChildRFID,
                null, emptyList());
        thingTypeChildRFID = executeTest(thingTypeChildRFID.getId(), ttChildCodeRFID, mapChildRFID, ttFieldsChildRFID);

        // remove thing types
        deleteThingType(thingTypeParentJACKET);
        deleteThingType(thingTypeParentPANTS);
        deleteThingType(thingTypeChildRFID);
    }

    @Test
    public void case_8_twoChildrenNativeParentChild() {
        String preffix = "MUL_CASE_8_";
        String ttAsset = preffix + "ASSET_CUSTOM";
        String ttGPS = preffix + "GPS_CUSTOM";
        String ttChildCodeRFID = preffix + "RFID_CUSTOM";

        // create Asset
        List<Map<String, Object>> ttFieldsAsset = new ArrayList<>();
        ttFieldsAsset.add(getMapFieldStandard("status", Boolean.FALSE, TYPE_TEXT));
        Map<String, Object> mapAsset = getMapThingTypeCustom(ttAsset, ttFieldsAsset);
        ThingType thingTypeAsset = executeTest(ttAsset, mapAsset, ttFieldsAsset);

        // create GPS with parent Asset
        List<Map<String, Object>> ttFieldsGPS = new ArrayList<>();
        ttFieldsGPS.add(getMapFieldStandard("status", Boolean.FALSE, TYPE_TEXT));
        Map<String, Object> mapGPS = getMapThingTypeCustom(ttGPS, Boolean.FALSE, ttFieldsGPS,
                null, singletonList(thingTypeAsset.getId()));
        ThingType thingTypeGPS = executeTest(ttGPS, mapGPS, ttFieldsGPS);

        // create RFID with parent Asset
        List<Map<String, Object>> ttFieldsChildRFID = getRFIDFields();
        Map<String, Object> mapChildRFID = getMapThingTypeCustom(ttChildCodeRFID, Boolean.FALSE, ttFieldsChildRFID,
                null, singletonList(thingTypeAsset.getId()));
        ThingType thingTypeRFID = executeTest(ttChildCodeRFID, mapChildRFID, ttFieldsChildRFID);

        // check associate
        checkParentChildRelationship(thingTypeAsset, thingTypeRFID);
        checkParentChildRelationship(thingTypeAsset, thingTypeGPS);

        if (!removeThingType) return;

        // disassociate
        mapAsset = getMapThingTypeCustom(ttAsset, Boolean.FALSE, ttFieldsAsset, emptyList(), null);
        thingTypeAsset = executeTest(thingTypeAsset.getId(), ttAsset, mapAsset, ttFieldsAsset);

        // remove thing types
        deleteThingType(thingTypeAsset);
        deleteThingType(thingTypeGPS);
        deleteThingType(thingTypeRFID);
    }

    @Test
    public void case_9_twoChildrenWithParentUDF() {
        String preffix = "MUL_CASE_9_";
        String ttAsset = preffix + "ASSET_CUSTOM";
        String ttGPS = preffix + "GPS_CUSTOM";
        String ttRFID = preffix + "RFID_CUSTOM";

        // create Asset
        List<Map<String, Object>> ttFieldsAsset = new ArrayList<>();
        ttFieldsAsset.add(getMapFieldStandard("status", Boolean.FALSE, TYPE_TEXT));
        Map<String, Object> mapAsset = getMapThingTypeCustom(ttAsset, Boolean.TRUE, ttFieldsAsset); // is Parent
        ThingType thingTypeAsset = executeTest(ttAsset, mapAsset, ttFieldsAsset);

        // create GPS with Asset UDF
        List<Map<String, Object>> ttFieldsGPS = new ArrayList<>();
        ttFieldsGPS.add(getMapFieldStandard("status", Boolean.FALSE, TYPE_TEXT));
        ttFieldsGPS.add(getMapFieldThingType("assetField", Boolean.TRUE, thingTypeAsset.getId(), TYPE_THING_TYPE)); // always timeseries=TRUE
        Map<String, Object> mapGPS = getMapThingTypeCustom(ttGPS, Boolean.FALSE, ttFieldsGPS);
        ThingType thingTypeGPS = executeTest(ttGPS, mapGPS, ttFieldsGPS);

        // create RFID with RFID UDF
        List<Map<String, Object>> ttFieldsChildRFID = getRFIDFields();
        ttFieldsChildRFID.add(getMapFieldThingType("assetField", Boolean.TRUE, thingTypeAsset.getId(), TYPE_THING_TYPE)); // always timeseries=TRUE
        Map<String, Object> mapChildRFID = getMapThingTypeCustom(ttRFID, Boolean.FALSE, ttFieldsChildRFID);
        ThingType thingTypeRFID = executeTest(ttRFID, mapChildRFID, ttFieldsChildRFID);

        // check associate
        checkRelationPath(thingTypeAsset, thingTypeGPS, ttGPS + "_children");
        checkRelationPath(thingTypeGPS, thingTypeAsset, "assetField.value");
        checkRelationPath(thingTypeAsset, thingTypeRFID, ttRFID + "_children");
        checkRelationPath(thingTypeRFID, thingTypeAsset, "assetField.value");

        if (!removeThingType) return;

        // disassociate
        List<Map<String, Object>> newFieldsGPS = removeFields(ttFieldsGPS, "assetField");
        mapGPS = getMapThingTypeCustom(ttGPS, Boolean.FALSE, newFieldsGPS);
        thingTypeGPS = executeTest(thingTypeGPS.getId(), ttGPS, mapGPS, newFieldsGPS);

        List<Map<String, Object>> newFieldsRFID = removeFields(ttFieldsChildRFID, "assetField");
        mapChildRFID = getMapThingTypeCustom(ttRFID, Boolean.FALSE, newFieldsRFID);
        thingTypeRFID = executeTest(thingTypeRFID.getId(), ttRFID, mapChildRFID, newFieldsRFID);

        // remove thing types
        deleteThingType(thingTypeAsset);
        deleteThingType(thingTypeGPS);
        deleteThingType(thingTypeRFID);
    }

    @Test
    public void case_10_mixedChildren() {
    }

    @Test
    public void case_11_multilevelChildren() {
    }

    @Test
    public void case_12_grandChildUsingThingType() {
    }

    @Test
    public void case_13_cycleScenario() {
    }

    @Test
    public void case_14_parentUDFandNativeChild() {
    }
}
