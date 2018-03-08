package com.tierconnect.riot.iot.services.thingType;

import com.tierconnect.riot.iot.entities.ThingTypeField;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by vealaro on 8/30/17.
 */
class ThingTypeUtilitiesForTest {

    public static Map<String, Object> getMapThingTypeCustom(String thingTypeCode, boolean isParent,
                                                            List<Map<String, Object>> fields, List<Long> children,
                                                            List<Long> parents) {
        return getMapThingType(thingTypeCode, thingTypeCode, StringUtils.EMPTY, 3L,
                children, parents, isParent, 1L, true, fields);
    }

    /**
     * thing type with parent false
     *
     * @param thingTypeCode
     * @param fields
     * @return
     */
    public static Map<String, Object> getMapThingTypeCustom(String thingTypeCode, List<Map<String, Object>> fields) {
        return getMapThingType(thingTypeCode, thingTypeCode, StringUtils.EMPTY, 3L,
                null, null, Boolean.FALSE, 1L, true, fields);
    }

    public static Map<String, Object> getMapThingTypeCustom(String name, String thingTypeCode, boolean isParent, List<Map<String, Object>> fields) {
        return getMapThingType(name, thingTypeCode, StringUtils.EMPTY, 3L,
                null, null, isParent, 1L, true, fields);
    }

    public static Map<String, Object> getMapThingTypeCustom(String thingTypeCode, boolean isParent, List<Map<String, Object>> fields) {
        return getMapThingType(thingTypeCode, thingTypeCode, StringUtils.EMPTY, 3L,
                null, null, isParent, 1L, true, fields);
    }

    public static Map<String, Object> getMapThingTypeCustom(String thingTypeCode, boolean isParent, boolean autoCreate, List<Map<String, Object>> fields) {
        return getMapThingType(thingTypeCode, thingTypeCode, StringUtils.EMPTY, 3L,
                null, null, isParent, 1L, autoCreate, fields);
    }

    private static Map<String, Object> getMapThingType(String name, String thingTypeCode, String serialFormula, Long groupID,
                                                       List childrenID, List parents, boolean isParent, Long thingTypeTemplateId,
                                                       boolean autoCreate, List<Map<String, Object>> fields) {
        Map<String, Object> thingType = new HashMap<>();
        thingType.put("serialFormula", serialFormula);
        thingType.put("group.id", groupID);
        thingType.put("name", name);
        thingType.put("isParent", isParent);
        thingType.put("thingTypeTemplateId", thingTypeTemplateId);
        thingType.put("thingTypeCode", thingTypeCode);
        thingType.put("autoCreate", autoCreate);
        if (childrenID != null) {
            thingType.put("child.ids", childrenID);
        }
        if (parents != null) {
            thingType.put("parent.ids", parents);
        }
        if (fields != null) {
            thingType.put("fields", fields);
        }
        return thingType;
    }

    public static Map<String, Object> getMapFieldThingType(String name, boolean timeSeries, Long idThingTypeFieldID,
                                                           ThingTypeField.Type type) {
        return getMapField(null, name, StringUtils.EMPTY, null, timeSeries, "NATIVE_THING_TYPE", type
                , idThingTypeFieldID, null);
    }

    public static Map<String, Object> getMapFieldStandard(String name, boolean timeSeries, ThingTypeField.Type type) {
        return getMapField(null, name, StringUtils.EMPTY, null, timeSeries,
                "DATA_TYPE", type, null, null);
    }

    public static Map<String, Object> getMapFieldStandard(String name, boolean timeSeries, ThingTypeField.Type type, String defaultValue) {
        return getMapField(null, name, StringUtils.EMPTY, null, timeSeries,
                "DATA_TYPE", type, null, defaultValue);
    }

    public static Map<String, Object> getMapFieldStandard(String name, boolean timeSeries, String symbol, String unit, ThingTypeField.Type type) {
        return getMapField(null, name, symbol, unit, timeSeries,
                "DATA_TYPE", type, null, null);
    }

    private static Map<String, Object> getMapField(Long thingTypeFieldTemplateId, String name, String symbol, String unit,
                                                   boolean timeSeries, String typeParent, ThingTypeField.Type type,
                                                   Long dataTypeThingTypeId, String defaultValue) {
        Map<String, Object> thingTypeField = new HashMap<>();
        thingTypeField.put("thingTypeFieldTemplateId", thingTypeFieldTemplateId);
        thingTypeField.put("name", name);
        thingTypeField.put("unit", unit);
        thingTypeField.put("symbol", symbol);
        thingTypeField.put("timeSeries", timeSeries);
        thingTypeField.put("typeParent", typeParent);
        thingTypeField.put("type", Integer.parseInt(String.valueOf(type.value)));
        thingTypeField.put("dataTypeThingTypeId", dataTypeThingTypeId);
        thingTypeField.put("defaultValue", defaultValue);
        thingTypeField.put("multiple", false);
        return thingTypeField;
    }
}
