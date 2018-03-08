package com.tierconnect.riot.iot.services.thingType;

import com.tierconnect.riot.iot.entities.ThingType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.tierconnect.riot.iot.entities.ThingTypeField.Type.*;
import static com.tierconnect.riot.iot.services.thingType.ThingTypeUtilitiesForTest.*;
/**
 * Created by vealaro on 8/31/17.
 */
public class ThingTypeServiceCreateUpdateTest extends ThingTypeServiceTest {

    @Test
    public void createThingTypeWithAllStandardDataType() {
        String thingTypeCode = "TT_ALL_STANDARD_DATA_TYPE";
        List<Map<String, Object>> allStandardUdf = getAllStandardUdf(Boolean.FALSE, Boolean.FALSE);
        Map<String, Object> map = getMapThingTypeCustom(thingTypeCode, Boolean.FALSE, allStandardUdf);
        ThingType thingType = executeTest(thingTypeCode, map, allStandardUdf);
        deleteThingType(thingType);
    }

    @Test
    public void createThingTypeWithAllStandardDataTypeMoreExpression() {
        String thingTypeCode = "TT_ALL_STANDARD_DATA_TYPE_WITH_EXPRESSION";
        List<Map<String, Object>> allStandardUdf = getAllStandardUdf(true, true);
        Map<String, Object> map = getMapThingTypeCustom(thingTypeCode, Boolean.FALSE, allStandardUdf);
        ThingType thingType = executeTest(thingTypeCode, map, allStandardUdf);
        deleteThingType(thingType);
    }

    @Test
    public void validateThingTypeNameTest() {
        String thingTypeCode = "NAME_()|&$";
        List<Map<String, Object>> udfs = Collections.emptyList();
        Map<String, Object> map = getMapThingTypeCustom(thingTypeCode, Boolean.FALSE, udfs);
        executeTest(thingTypeCode, map, udfs, "Invalid Name"); // validate Name
    }

    @Test
    public void validateThingTypeUpdateNameTest() {
        String thingTypeCode = "NAME";
        List<Map<String, Object>> udfs = Collections.emptyList();
        Map<String, Object> map = getMapThingTypeCustom(thingTypeCode, Boolean.FALSE, udfs);
        ThingType thingType = executeTest(thingTypeCode, map, udfs);

        map = getMapThingTypeCustom("NAME_().&$|", thingTypeCode, Boolean.FALSE, udfs);
        executeTest(thingType.getId(), thingTypeCode, map, udfs, "Invalid Name"); // validate Name

        deleteThingType(thingType);
    }

    @Test
    public void validateGroupThingTypeCase1Test() {
        String thingTypeCode = "INVALID_GROUP";
        List<Map<String, Object>> udfs = Collections.emptyList();
        Map<String, Object> map = getMapThingTypeCustom(thingTypeCode, Boolean.FALSE, udfs);
        map.remove("group.id");
        executeTest(thingTypeCode, map, udfs, "The group.id is required."); // validate Group
    }

    @Test
    public void validateGroupThingTypeCase2Test() {
        String thingTypeCode = "INVALID_GROUP";
        List<Map<String, Object>> udfs = Collections.emptyList();
        Map<String, Object> map = getMapThingTypeCustom(thingTypeCode, Boolean.FALSE, udfs);
        map.put("group.id", 99999);
        executeTest(thingTypeCode, map, udfs, "Invalid Group"); // validate Group
    }

    @Test
    public void validateThingTemplateCase1Test() {
        String thingTypeCode = "INVALID_THIN_TYPE_TEMPLATE";
        List<Map<String, Object>> udfs = Collections.emptyList();
        Map<String, Object> map = getMapThingTypeCustom(thingTypeCode, Boolean.FALSE, udfs);
        map.remove("thingTypeTemplateId");
        executeTest(thingTypeCode, map, udfs, "The thingTypeTemplateId is required."); // validate Group
    }

    @Test
    public void validateThingTemplateCase2Test() {
        String thingTypeCode = "INVALID_THIN_TYPE_TEMPLATE";
        List<Map<String, Object>> udfs = Collections.emptyList();
        Map<String, Object> map = getMapThingTypeCustom(thingTypeCode, Boolean.FALSE, udfs);
        map.put("thingTypeTemplateId", 99999);
        executeTest(thingTypeCode, map, udfs, "Invalid thingTypeTemplateId"); // validate Group
    }

    @Test
    public void validateThingTypeFieldCase1Test() {
        String thingTypeCode = "INVALID_THIN_TYPE_FIELDS";
        List<Map<String, Object>> udfs = new ArrayList<>();
        udfs.add(getMapFieldStandard(".status", Boolean.FALSE, TYPE_TEXT));
        udfs.add(getMapFieldStandard("status.value", Boolean.FALSE, TYPE_TEXT));
        udfs.add(getMapFieldStandard("status.", Boolean.FALSE, TYPE_TEXT));
        Map<String, Object> map = getMapThingTypeCustom(thingTypeCode, Boolean.FALSE, udfs);
        executeTest(thingTypeCode, map, udfs,
                "Property Name : '.status' should not have the character dot (.)|||"
                        + "Property Name : 'status.value' should not have the character dot (.)|||"
                        + "Property Name : 'status.' should not have the character dot (.)");
    }

    @Test
    public void validateReservedWordsCreate() {
        String thingTypeCode = "RESERVED_WORDS";
        List<Map<String, Object>> udfs = new ArrayList<>();
        udfs.add(getMapFieldStandard("status", Boolean.FALSE, TYPE_TEXT));
        udfs.add(getMapFieldStandard("serialNumber", Boolean.FALSE, TYPE_TEXT));
        udfs.add(getMapFieldStandard("parent", Boolean.FALSE, TYPE_TEXT));
        udfs.add(getMapFieldStandard("children", Boolean.FALSE, TYPE_TEXT));
        udfs.add(getMapFieldStandard("sqn", Boolean.FALSE, TYPE_TEXT));
        udfs.add(getMapFieldStandard("id", Boolean.FALSE, TYPE_TEXT));
        udfs.add(getMapFieldStandard("_id", Boolean.FALSE, TYPE_TEXT));
        Map<String, Object> map = getMapThingTypeCustom(thingTypeCode, Boolean.FALSE, udfs);
        executeTest(thingTypeCode, map, udfs,
                "Field names are not allowed because are reserved words [serialNumber, parent, children, sqn, id, _id].");
    }

    @Test
    public void validateReservedWordsUpdate() {
        String thingTypeCode = "RESERVED_WORDS_UPDATE";
        List<Map<String, Object>> udfs = new ArrayList<>();
        udfs.add(getMapFieldStandard("status", Boolean.FALSE, TYPE_TEXT));
        Map<String, Object> map = getMapThingTypeCustom(thingTypeCode, Boolean.FALSE, udfs);
        ThingType thingType = executeTest(thingTypeCode, map, udfs);

        udfs.add(getMapFieldStandard("serialNumber", Boolean.FALSE, TYPE_TEXT));
        udfs.add(getMapFieldStandard("parent", Boolean.FALSE, TYPE_TEXT));
        udfs.add(getMapFieldStandard("children", Boolean.FALSE, TYPE_TEXT));
        udfs.add(getMapFieldStandard("sqn", Boolean.FALSE, TYPE_TEXT));
        udfs.add(getMapFieldStandard("id", Boolean.FALSE, TYPE_TEXT));
        udfs.add(getMapFieldStandard("_id", Boolean.FALSE, TYPE_TEXT));
        executeTest(thingType.getId(), thingTypeCode, map, udfs,
                "Field names are not allowed because are reserved words [serialNumber, parent, children, sqn, id, _id].");

        deleteThingType(thingType);
    }

    @Test
    public void validateRepeatUDF() {
        String thingTypeCode = "REPEAT_UDF";
        List<Map<String, Object>> udfs = new ArrayList<>();
        udfs.add(getMapFieldStandard("status", Boolean.FALSE, TYPE_TEXT));
        udfs.add(getMapFieldStandard("status", Boolean.FALSE, TYPE_NUMBER));
        Map<String, Object> map = getMapThingTypeCustom(thingTypeCode, Boolean.FALSE, udfs);
        executeTest(thingTypeCode, map, udfs, "Thing type field already exists.");
    }

    @Test
    public void validateThingTypeFieldCase2Test() {
        String thingTypeCode = "INVALID_THIN_TYPE_FIELDS";
        List<Map<String, Object>> udfs = new ArrayList<>();
        Map<String, Object> field1 = getMapFieldStandard("status", Boolean.FALSE, TYPE_TEXT);
        Map<String, Object> field2 = getMapFieldStandard("price", Boolean.FALSE, TYPE_TEXT);
//        field1.remove("type"); // TODO: validate exist type
        field2.put("type", -1);
        udfs.add(field1);
        udfs.add(field2);
        Map<String, Object> map = getMapThingTypeCustom(thingTypeCode, Boolean.FALSE, udfs);
        executeTest(thingTypeCode, map, udfs, "Invalid type in fields:  price");
    }

    @Test
    public void validateThingTypeSerialNumberExpression() {
        String thingTypeCode = "SERIAL_FORMULA";
        List<Map<String, Object>> udfs = new ArrayList<>();
        udfs.add(getMapFieldStandard("statusA", Boolean.FALSE, TYPE_TEXT));
        udfs.add(getMapFieldStandard("statusB", Boolean.FALSE, TYPE_FORMULA, ""));
        Map<String, Object> map = getMapThingTypeCustom(thingTypeCode, Boolean.FALSE, udfs);
        executeTest(thingTypeCode, map, udfs, "Error with '${', it is required for Expression Data Type");
    }

    @Test
    public void validateThingTypeUDFisTimeseriesTrueTest() {
        String preffix = "THING_TYPE_UDF_TRUE_";
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
        ttFieldsAsset.add(getMapFieldThingType("shippingOrderField", Boolean.FALSE, thingTypeSO.getId(), TYPE_THING_TYPE));
        Map<String, Object> mapAsset = getMapThingTypeCustom(ttAsset, ttFieldsAsset);
        executeTest(ttAsset, mapAsset, ttFieldsAsset,
                "Time Series value in Property Thing Type UDF 'shippingOrderField' should be true");

        deleteThingType(thingTypeSO);
    }
}
