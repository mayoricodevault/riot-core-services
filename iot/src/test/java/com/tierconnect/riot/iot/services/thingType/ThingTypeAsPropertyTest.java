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
public class ThingTypeAsPropertyTest extends ThingTypeServiceTest {

    @Test
    public void case_1_ThingTypeAsProperty() {
        String preffix = "TTP_CASE_1_";
        String ttPipeCode = preffix + "PIPE_CUSTOM";
        String ttSizeCode = preffix + "SIZE_CUSTOM";

        // create SIZE
        List<Map<String, Object>> ttFieldSize = new ArrayList<>();
        ttFieldSize.add(getMapFieldStandard("sizeText", Boolean.TRUE, TYPE_TEXT));
        ttFieldSize.add(getMapFieldStandard("sizeNumber", Boolean.TRUE, TYPE_NUMBER));
        Map<String, Object> mapSize = getMapThingTypeCustom(ttSizeCode, Boolean.FALSE, ttFieldSize);
        ThingType thingTypeSize = executeTest(ttSizeCode, mapSize, ttFieldSize);

        // create PIPE with SIZE as thing type UDF
        List<Map<String, Object>> ttFieldsPipe = new ArrayList<>();
        ttFieldsPipe.add(getMapFieldStandard("status", Boolean.FALSE, TYPE_TEXT));
        ttFieldsPipe.add(getMapFieldThingType("sizeField", Boolean.TRUE, thingTypeSize.getId(), TYPE_THING_TYPE));
        Map<String, Object> mapPipe = getMapThingTypeCustom(ttPipeCode, ttFieldsPipe);
        ThingType thingTypePipe = executeTest(ttPipeCode, mapPipe, ttFieldsPipe);

        // check associate
        checkRelationPath(thingTypePipe, thingTypeSize, "sizeField.value");

        if (!removeThingType) return;

        // update disassociate
        List<Map<String, Object>> newTTFieldsPipe = removeFields(ttFieldsPipe, "sizeField");
        mapPipe = getMapThingTypeCustom(ttPipeCode, newTTFieldsPipe);
        thingTypePipe = executeTest(thingTypePipe.getId(), ttPipeCode, mapPipe, newTTFieldsPipe);

        // delete all thing types
        deleteThingType(thingTypeSize);
        deleteThingType(thingTypePipe);
    }

    @Test
    public void case_2_childWithUDFThingType() {
    }

    @Test
    public void case_3_parentWithUDFThingType() {
        String preffix = "TTP_CASE_3_";
        String ttSizeCode = preffix + "SIZE_CUSTOM";
        String ttAssetCode = preffix + "ASSET_CUSTOM";
        String ttTagCode = preffix + "TAG_CUSTOM";

        // create SIZE
        List<Map<String, Object>> ttFieldSize = new ArrayList<>();
        ttFieldSize.add(getMapFieldStandard("sizeText", Boolean.TRUE, TYPE_TEXT));
        ttFieldSize.add(getMapFieldStandard("sizeNumber", Boolean.TRUE, TYPE_NUMBER));
        Map<String, Object> mapSize = getMapThingTypeCustom(ttSizeCode, Boolean.FALSE, ttFieldSize);
        ThingType thingTypeSize = executeTest(ttSizeCode, mapSize, ttFieldSize);


        // create Asset
        List<Map<String, Object>> ttFieldsAsset = new ArrayList<>();
        ttFieldsAsset.add(getMapFieldStandard("status", Boolean.FALSE, TYPE_TEXT));
        ttFieldsAsset.add(getMapFieldThingType("sizeField", Boolean.TRUE, thingTypeSize.getId(), TYPE_THING_TYPE));
        Map<String, Object> mapAsset = getMapThingTypeCustom(ttAssetCode, ttFieldsAsset);
        ThingType thingTypeAsset = executeTest(ttAssetCode, mapAsset, ttFieldsAsset);

        // create Tag with asset parent
        List<Map<String, Object>> ttFieldsTag = getTagFields();
        Map<String, Object> mapTag = getMapThingTypeCustom(ttTagCode, Boolean.FALSE, ttFieldsTag, null,
                singletonList(thingTypeAsset.getId()));
        ThingType thingTypeTag = executeTest(ttTagCode, mapTag, ttFieldsTag);

        // check associate
        checkRelationPath(thingTypeAsset, thingTypeSize, "sizeField.value");
        checkParentChildRelationship(thingTypeAsset, thingTypeTag);

        if (!removeThingType) return;

        // update disassociate
        List<Map<String, Object>> newTTFieldsPipe = removeFields(ttFieldsAsset, "sizeField");
        mapAsset = getMapThingTypeCustom(ttAssetCode, Boolean.FALSE, newTTFieldsPipe, emptyList(), null);
        thingTypeAsset = executeTest(thingTypeAsset.getId(), ttAssetCode, mapAsset, newTTFieldsPipe);

        // delete all thing types
        deleteThingType(thingTypeAsset);
        deleteThingType(thingTypeSize);
        deleteThingType(thingTypeTag);
    }

    @Test
    public void case_4_parentAndChildWithSameThingTypeUDF() {
    }

    @Test
    public void case_5_childrenUDFWithThingTypeUDF() {
        String preffix = "TTP_CASE_5_";
        String ttSizeCode = preffix + "SIZE_CUSTOM";
        String ttSOCode = preffix + "SHIPPING_ORDER_CUSTOM";
        String ttAssetCode = preffix + "ASSET_CUSTOM";
        String ttTagCode = preffix + "TAG_CUSTOM";

        // create SIZE
        List<Map<String, Object>> ttFieldSize = new ArrayList<>();
        ttFieldSize.add(getMapFieldStandard("sizeText", Boolean.TRUE, TYPE_TEXT));
        ttFieldSize.add(getMapFieldStandard("sizeNumber", Boolean.TRUE, TYPE_NUMBER));
        Map<String, Object> mapSize = getMapThingTypeCustom(ttSizeCode, ttFieldSize);
        ThingType thingTypeSize = executeTest(ttSizeCode, mapSize, ttFieldSize);

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
        ttFieldsAsset.add(getMapFieldThingType("sizeField", Boolean.TRUE, thingTypeSize.getId(), TYPE_THING_TYPE));
        Map<String, Object> mapAsset = getMapThingTypeCustom(ttAssetCode, Boolean.FALSE, ttFieldsAsset);
        ThingType thingTypeAsset = executeTest(ttAssetCode, mapAsset, ttFieldsAsset);

        // create tag with parent Asset
        List<Map<String, Object>> ttFieldsTag = getTagFields();
        Map<String, Object> mapTag = getMapThingTypeCustom(ttTagCode, Boolean.FALSE, ttFieldsTag, null,
                singletonList(thingTypeAsset.getId()));
        ThingType thingTypeTag = executeTest(ttTagCode, mapTag, ttFieldsTag);

        // check structure
        checkParentChildRelationship(thingTypeAsset, thingTypeTag);
        checkRelationPath(thingTypeAsset, thingTypeSize, "sizeField.value");
        checkRelationPath(thingTypeSO, thingTypeSize, ttAssetCode + "_children.sizeField.value");
        checkRelationPath(thingTypeSO, thingTypeAsset, ttAssetCode + "_children");
        checkRelationPath(thingTypeAsset, thingTypeSO, "shippingOrderField.value");
        checkRelationPath(thingTypeTag, thingTypeSO, "parent.shippingOrderField.value");
        checkRelationPath(thingTypeSO, thingTypeTag, ttAssetCode + "_children.children");

        if (!removeThingType) return;

        // update disassociate
        List<Map<String, Object>> newFields = removeFields(ttFieldsAsset, "shippingOrderField", "sizeField");
        mapAsset = getMapThingTypeCustom(ttAssetCode, Boolean.FALSE, newFields, emptyList(), null);
        thingTypeAsset = executeTest(thingTypeAsset.getId(), ttAssetCode, mapAsset, newFields);

        // remove thing types
        deleteThingType(thingTypeSO);
        deleteThingType(thingTypeAsset);
        deleteThingType(thingTypeTag);
        deleteThingType(thingTypeSize);
    }

    @Test
    public void case_6_parentUDFWithThingTypeUDF() {
        String preffix = "TTP_CASE_6_";
        String ttSizeCode = preffix + "SIZE_CUSTOM";
        String ttSOCode = preffix + "SHIPPING_ORDER_CUSTOM";
        String ttAssetCode = preffix + "ASSET_CUSTOM";

        // create SIZE
        List<Map<String, Object>> ttFieldSize = new ArrayList<>();
        ttFieldSize.add(getMapFieldStandard("sizeText", Boolean.TRUE, TYPE_TEXT));
        ttFieldSize.add(getMapFieldStandard("sizeNumber", Boolean.TRUE, TYPE_NUMBER));
        Map<String, Object> mapSize = getMapThingTypeCustom(ttSizeCode, ttFieldSize);
        ThingType thingTypeSize = executeTest(ttSizeCode, mapSize, ttFieldSize);

        // create S.O. with UDF size
        List<Map<String, Object>> ttFieldsSO = new ArrayList<>();
        ttFieldsSO.add(getMapFieldStandard("countAsset", Boolean.FALSE, TYPE_FORMULA, "${count(\"\",\"\")}"));
        ttFieldsSO.add(getMapFieldStandard("countOpenAsset", Boolean.FALSE, TYPE_FORMULA, "${count(\"asset_code\",\"status.value=Open\")}"));
        ttFieldsSO.add(getMapFieldStandard("owner", Boolean.TRUE, TYPE_TEXT));
        ttFieldsSO.add(getMapFieldStandard("status", Boolean.TRUE, TYPE_TEXT));
        ttFieldsSO.add(getMapFieldThingType("sizeField", Boolean.TRUE, thingTypeSize.getId(), TYPE_THING_TYPE));
        Map<String, Object> mapParentSO = getMapThingTypeCustom(ttSOCode, Boolean.TRUE, ttFieldsSO);
        ThingType thingTypeSO = executeTest(ttSOCode, mapParentSO, ttFieldsSO);

        // create Asset with udf S.O.
        List<Map<String, Object>> ttFieldsAsset = new ArrayList<>();
        ttFieldsAsset.add(getMapFieldStandard("status", Boolean.FALSE, TYPE_TEXT));
        ttFieldsAsset.add(getMapFieldThingType("shippingOrderField", Boolean.TRUE, thingTypeSO.getId(), TYPE_THING_TYPE)); //always true
        Map<String, Object> mapAsset = getMapThingTypeCustom(ttAssetCode, Boolean.FALSE, ttFieldsAsset);
        ThingType thingTypeAsset = executeTest(ttAssetCode, mapAsset, ttFieldsAsset);

        // check structure
        checkRelationPath(thingTypeSO, thingTypeAsset, ttAssetCode + "_children");
        checkRelationPath(thingTypeAsset, thingTypeSO, "shippingOrderField.value");
        checkRelationPath(thingTypeSO, thingTypeSize, "sizeField.value");

        if (!removeThingType) return;

        // update disassociate
        List<Map<String, Object>> newFieldsAsset = removeFields(ttFieldsAsset, "shippingOrderField");
        mapAsset = getMapThingTypeCustom(ttAssetCode, Boolean.FALSE, newFieldsAsset);
        thingTypeAsset = executeTest(thingTypeAsset.getId(), ttAssetCode, mapAsset, newFieldsAsset);

        List<Map<String, Object>> newFieldSO = removeFields(ttFieldsSO, "sizeField");
        mapParentSO = getMapThingTypeCustom(ttSOCode, Boolean.TRUE, newFieldSO);
        thingTypeSO = executeTest(thingTypeSO.getId(), ttSOCode, mapParentSO, newFieldSO);

        // remove thing types
        deleteThingType(thingTypeSize);
        deleteThingType(thingTypeSO);
        deleteThingType(thingTypeAsset);
    }

    @Test
    public void case_7_parentUDFAndChildrenUDFWithSameThingTypeUDF() {
    }

    /**
     * <pre>
     * TAG
     *  |--SIZE (parent=false)
     *
     * TT1  (parent=true)
     *  |--SIZE (children UDF)
     * </pre>
     */
    @Test
    public void case_8_thingTypeWithUDFAndParentUDF() {
    }

    /**
     * Parent-Child native with thing type UDF
     * <pre>
     *
     * </pre>
     */
    @Test
    public void case_9_childrenWithThingTypeUDF() {
        String preffix = "TTP_CASE_9_";
        String ttSizeCode = preffix + "SIZE_CUSTOM";
        String ttParentCodeJACKET = preffix + "JACKET_CUSTOM";
        String ttChildCodeRFID = preffix + "RFID_CUSTOM";
        String ttParentCodePANTS = preffix + "PANTS_CUSTOM";

        // create SIZE
        List<Map<String, Object>> ttFieldSize = new ArrayList<>();
        ttFieldSize.add(getMapFieldStandard("sizeText", Boolean.TRUE, TYPE_TEXT));
        ttFieldSize.add(getMapFieldStandard("sizeNumber", Boolean.TRUE, TYPE_NUMBER));
        Map<String, Object> mapSize = getMapThingTypeCustom(ttSizeCode, ttFieldSize);
        ThingType thingTypeSize = executeTest(ttSizeCode, mapSize, ttFieldSize);

        // create Jacket
        List<Map<String, Object>> ttFieldsParentJACKET = getJacketFields();
        ttFieldsParentJACKET.add(getMapFieldThingType("sizeField", Boolean.TRUE, thingTypeSize.getId(), TYPE_THING_TYPE));
        Map<String, Object> mapParentJACKET = getMapThingTypeCustom(ttParentCodeJACKET, ttFieldsParentJACKET);
        ThingType thingTypeParentJACKET = executeTest(ttParentCodeJACKET, mapParentJACKET, ttFieldsParentJACKET);

        // create RFID with paren Jacket
        List<Map<String, Object>> ttFieldsChildRFID = getRFIDFields();
        Map<String, Object> mapChildRFID = getMapThingTypeCustom(ttChildCodeRFID, Boolean.FALSE, ttFieldsChildRFID,
                null, singletonList(thingTypeParentJACKET.getId()));
        ThingType thingTypeChildRFID = executeTest(ttChildCodeRFID, mapChildRFID, ttFieldsChildRFID);

        // create Pants with RFID child
        List<Map<String, Object>> ttFieldsParentPANTS = getPantFields();
        ttFieldsParentPANTS.add(getMapFieldThingType("sizeField", Boolean.TRUE, thingTypeSize.getId(), TYPE_THING_TYPE));
        Map<String, Object> mapChildPANTS = getMapThingTypeCustom(ttParentCodePANTS, ttFieldsParentPANTS);
        ThingType thingTypeParentPANTS = executeTest(ttParentCodePANTS, mapChildPANTS, ttFieldsParentPANTS);

        // associate PANTS with RFID
        mapChildPANTS = getMapThingTypeCustom(ttParentCodePANTS, Boolean.FALSE, ttFieldsParentPANTS,
                singletonList(thingTypeChildRFID.getId()), null);
        thingTypeParentPANTS = executeTest(thingTypeParentPANTS.getId(), ttParentCodePANTS, mapChildPANTS, ttFieldsParentPANTS);

        // check associate
        checkParentChildRelationship(thingTypeParentJACKET, thingTypeChildRFID);
        checkParentChildRelationship(thingTypeParentPANTS, thingTypeChildRFID);
        checkRelationPath(thingTypeParentJACKET, thingTypeSize, "sizeField.value");

        if (!removeThingType) return;

        // update disassociate
        List<Map<String, Object>> newFieldsJACKET = removeFields(ttFieldsParentJACKET, "sizeField");
        mapParentJACKET = getMapThingTypeCustom(ttParentCodeJACKET, Boolean.FALSE, newFieldsJACKET, emptyList(), null);
        thingTypeParentJACKET = executeTest(thingTypeParentJACKET.getId(), ttParentCodeJACKET, mapParentJACKET, newFieldsJACKET);

        List<Map<String, Object>> newFieldsPANTS = removeFields(ttFieldsParentPANTS, "sizeField");
        mapChildPANTS = getMapThingTypeCustom(ttParentCodePANTS, Boolean.FALSE, newFieldsPANTS, emptyList(), null);
        thingTypeParentPANTS = executeTest(thingTypeParentPANTS.getId(), ttParentCodePANTS, mapChildPANTS, newFieldsPANTS);

        // remove thing types
        deleteThingType(thingTypeSize);
        deleteThingType(thingTypeParentJACKET);
        deleteThingType(thingTypeParentPANTS);
        deleteThingType(thingTypeChildRFID);
    }

    @Test
    public void case_10_childrenNativeWithThingTypeUDF() {
    }

    @Test
    public void case_11_twoParentUDFWithSameThingTypeUDF() {
    }

    @Test
    public void case_12_childrenUDFWithThingTypeUDF() {
        String preffix = "TTP_CASE_12_";
        String ttSizeCode = preffix + "SIZE_CUSTOM";
        String ttAssetCode = preffix + "ASSET_CUSTOM";
        String ttGPSCode = preffix + "GPS_CUSTOM";
        String ttRFIDCode = preffix + "RFID_CUSTOM";

        // create SIZE
        List<Map<String, Object>> ttFieldSize = new ArrayList<>();
        ttFieldSize.add(getMapFieldStandard("sizeText", Boolean.TRUE, TYPE_TEXT));
        ttFieldSize.add(getMapFieldStandard("sizeNumber", Boolean.TRUE, TYPE_NUMBER));
        Map<String, Object> mapSize = getMapThingTypeCustom(ttSizeCode, ttFieldSize);
        ThingType thingTypeSize = executeTest(ttSizeCode, mapSize, ttFieldSize);

        // create Asset
        List<Map<String, Object>> ttFieldsAsset = new ArrayList<>();
        ttFieldsAsset.add(getMapFieldStandard("status", Boolean.FALSE, TYPE_TEXT));
        Map<String, Object> mapAsset = getMapThingTypeCustom(ttAssetCode, Boolean.TRUE, ttFieldsAsset); // is Parent
        ThingType thingTypeAsset = executeTest(ttAssetCode, mapAsset, ttFieldsAsset);

        // create GPS with Asset UDF
        List<Map<String, Object>> ttFieldsChildGPS = new ArrayList<>();
        ttFieldsChildGPS.add(getMapFieldStandard("status", Boolean.FALSE, TYPE_TEXT));
        ttFieldsChildGPS.add(getMapFieldThingType("assetField", Boolean.TRUE, thingTypeAsset.getId(), TYPE_THING_TYPE)); // always timeseries=TRUE
        ttFieldsChildGPS.add(getMapFieldThingType("sizeField", Boolean.TRUE, thingTypeSize.getId(), TYPE_THING_TYPE)); // always timeseries=TRUE
        Map<String, Object> mapGPS = getMapThingTypeCustom(ttGPSCode, Boolean.FALSE, ttFieldsChildGPS);
        ThingType thingTypeGPS = executeTest(ttGPSCode, mapGPS, ttFieldsChildGPS);

        // create RFID with RFID UDF
        List<Map<String, Object>> ttFieldsChildRFID = getRFIDFields();
        ttFieldsChildRFID.add(getMapFieldThingType("assetField", Boolean.TRUE, thingTypeAsset.getId(), TYPE_THING_TYPE)); // always timeseries=TRUE
        ttFieldsChildRFID.add(getMapFieldThingType("sizeField", Boolean.TRUE, thingTypeSize.getId(), TYPE_THING_TYPE)); // always timeseries=TRUE
        Map<String, Object> mapChildRFID = getMapThingTypeCustom(ttRFIDCode, Boolean.FALSE, ttFieldsChildRFID);
        ThingType thingTypeRFID = executeTest(ttRFIDCode, mapChildRFID, ttFieldsChildRFID);

        // check associate
        checkRelationPath(thingTypeAsset, thingTypeGPS, ttGPSCode + "_children");
        checkRelationPath(thingTypeGPS, thingTypeAsset, "assetField.value");
        checkRelationPath(thingTypeAsset, thingTypeRFID, ttRFIDCode + "_children");
        checkRelationPath(thingTypeRFID, thingTypeAsset, "assetField.value");
        checkRelationPath(thingTypeRFID, thingTypeSize, "sizeField.value");
        checkRelationPath(thingTypeGPS, thingTypeSize, "sizeField.value");

        if (!removeThingType) return;

        // disassociate
        List<Map<String, Object>> newFieldsGPS = removeFields(ttFieldsChildGPS, "assetField", "sizeField");
        mapGPS = getMapThingTypeCustom(ttGPSCode, newFieldsGPS);
        thingTypeGPS = executeTest(thingTypeGPS.getId(), ttGPSCode, mapGPS, newFieldsGPS);

        List<Map<String, Object>> newFieldsRFID = removeFields(ttFieldsChildRFID, "assetField", "sizeField");
        mapChildRFID = getMapThingTypeCustom(ttRFIDCode, newFieldsRFID);
        thingTypeRFID = executeTest(thingTypeRFID.getId(), ttRFIDCode, mapChildRFID, newFieldsRFID);

        // remove thing types
        deleteThingType(thingTypeAsset);
        deleteThingType(thingTypeGPS);
        deleteThingType(thingTypeSize);
        deleteThingType(thingTypeRFID);
    }

    @Test
    public void case_13_childUDFWithThingTypeUDF() {
        String preffix = "TTP_CASE_13_";
        String ttSizeCode = preffix + "SIZE_CUSTOM";
        String ttCodeSO = preffix + "SHIPPING_ORDER_CUSTOM";
        String ttAsset = preffix + "ASSET_CUSTOM";

        // create SIZE
        List<Map<String, Object>> ttFieldSize = new ArrayList<>();
        ttFieldSize.add(getMapFieldStandard("sizeText", Boolean.TRUE, TYPE_TEXT));
        ttFieldSize.add(getMapFieldStandard("sizeNumber", Boolean.TRUE, TYPE_NUMBER));
        Map<String, Object> mapSize = getMapThingTypeCustom(ttSizeCode, ttFieldSize);
        ThingType thingTypeSize = executeTest(ttSizeCode, mapSize, ttFieldSize);

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
        ttFieldsAsset.add(getMapFieldThingType("sizeField", Boolean.TRUE, thingTypeSize.getId(), TYPE_THING_TYPE)); // always timeseries=TRUE
        Map<String, Object> mapAsset = getMapThingTypeCustom(ttAsset, ttFieldsAsset);
        ThingType thingTypeAsset = executeTest(ttAsset, mapAsset, ttFieldsAsset);

        // check associate
        checkRelationPath(thingTypeAsset, thingTypeSO, "shippingOrderField.value");
        checkRelationPath(thingTypeAsset, thingTypeSize, "sizeField.value");
        checkRelationPath(thingTypeSO, thingTypeAsset, ttAsset + "_children");
        checkRelationPath(thingTypeSO, thingTypeSize, ttAsset + "_children.sizeField.value");

        if (!removeThingType) return;

        // update disassociate
        List<Map<String, Object>> newFields = removeFields(ttFieldsAsset, "shippingOrderField", "sizeField");
        mapAsset = getMapThingTypeCustom(ttAsset, newFields);
        thingTypeAsset = executeTest(thingTypeAsset.getId(), ttAsset, mapAsset, newFields);

        // delete all thing types
        deleteThingType(thingTypeSO);
        deleteThingType(thingTypeSize);
        deleteThingType(thingTypeAsset);
    }

    @Test
    public void case_14_chidlrenUDFWithOtherParentNative() {
    }

    @Test
    public void case_15_thingTypeWithTwoThingTypesEquals() {
    }

    @Test
    public void case_16_threeLevelThingTypeUDF() {
    }
}
