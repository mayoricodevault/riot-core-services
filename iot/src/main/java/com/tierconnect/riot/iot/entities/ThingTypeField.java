package com.tierconnect.riot.iot.entities;

import org.apache.commons.lang.StringUtils;

import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.HashMap;
import java.util.Map;


/**
 * @author garivera
 */
@SuppressWarnings("JpaDataSourceORMInspection")
@Entity
@Table(name = "ThingTypeField", indexes = {@Index(name = "IDX_thingtypefield_name", columnList = "name")})
@XmlRootElement(name = "ThingTypeField")
public class ThingTypeField extends ThingTypeFieldBase implements com.tierconnect.riot.commons.entities.IThingTypeField {
    @Override
    public Class getClazz() {
        return null;
    }
//
//    public enum Type {
//        TYPE_TEXT       ,
//        TYPE_LONLATALT  ,
//        TYPE_XYZ        ,
//        TYPE_NUMBER     ,
//        TYPE_BOOLEAN    ,
//        TYPE_IMAGE_ID   ,
//        TYPE_SHIFT      ,
//        TYPE_IMAGE_URL  ,
//        TYPE_ZONE       ,
//        TYPE_JSON       ,
//        TYPE_DATE       ,
//        TYPE_URL
//    }

    public enum TypeParent {
        TYPE_PARENT_DATA_TYPE("DATA_TYPE"),
        TYPE_PARENT_NATIVE_THING_TYPE("NATIVE_THING_TYPE");

        public String value;

        TypeParent(String value) {
            this.value = value;
        }
    }

    public enum TypeParentSubGroup {
        TYPE_PARENT_DATA_TYPE_STANDARD_DATA("Standard Data Types"),
        TYPE_PARENT_DATA_TYPE_NATIVE_OBJECT("Native Objects");

        public String value;

        TypeParentSubGroup(String value) {
            this.value = value;
        }
    }

    public enum Type {
        TYPE_TEXT(1L),
        TYPE_LONLATALT(2L),
        TYPE_XYZ(3L),
        TYPE_NUMBER(4L),
        TYPE_BOOLEAN(5L),
        TYPE_IMAGE_ID(6L),
        TYPE_SHIFT(7L),
        TYPE_IMAGE_URL(8L),
        TYPE_ZONE(9L),
        TYPE_JSON(10L),
        TYPE_DATE(11L),
        TYPE_URL(12L),
        TYPE_ZPL_SCRIPT(13L),
        //TYPE_DATA_TYPE          (20L),
        //TYPE_NATIVE_THING_TYPE  (21L),
        TYPE_GROUP(22L),
        TYPE_LOGICAL_READER(23L),
        TYPE_TIMESTAMP(24L),
        TYPE_SEQUENCE(25L),
        TYPE_FORMULA(26L),
        TYPE_THING_TYPE(27L),
        TYPE_ATTACHMENTS(28L),
        TYPE_ICON(29L),
        TYPE_COLOR(30L);

        public long value;

        Type(Long value) {
            this.value = value;
        }

        public static Type getTypeByValue(long value) {
            Type result = null;
            for (Type type : Type.values()) {
                if (type.value == value)
                    result = type;
            }
            return result;
        }

        public static Boolean isDateOrTimestamp(Long value) {
            return value != null && (value.equals(TYPE_DATE.value) || value.equals(TYPE_TIMESTAMP.value));
        }

        public static Boolean isDate(Long value) {
            return value != null && value.equals(TYPE_DATE.value);
        }

        public static Boolean isTimestamp(Long value) {
            return value != null && value.equals(TYPE_TIMESTAMP.value);
        }

    }

    public ThingTypeField() {
        super();
        dataType = getDefaultDataType();
    }

    public ThingTypeField(String name, String units, String symbol, String thingTypeParent, DataType dataType, Long dataTypeThingTypeId) {
        this.name = name;
        this.unit = units;
        this.symbol = symbol;
        this.dataType = dataType;
        this.dataTypeThingTypeId = dataTypeThingTypeId;
        this.typeParent = thingTypeParent;
        this.timeSeries = true;
    }

    public ThingTypeField(String name, String units, String symbol, DataType dataType, Long dataTypeThingTypeId, String typeParent, boolean multiple, boolean timeSeries, String defaultValue) {
        this.name = name;
        this.unit = units;
        this.symbol = symbol;
        this.dataType = dataType;
        this.dataTypeThingTypeId = dataTypeThingTypeId;
        this.typeParent = typeParent;
        this.multiple = multiple;
        this.timeSeries = timeSeries;
        this.defaultValue = defaultValue;
    }

    public DataType getDataType() {
        if (this.dataType == null) {
            return getDefaultDataType();
        }
        return this.dataType;
    }

    public boolean isThisDataType(Long idDataType) {
        return getDataType() != null && getDataType().getId().compareTo(idDataType) == 0;
    }

    //Set default Data Type
    public DataType getDefaultDataType() {
        DataType dt = new DataType();
        dt.setId(1L);
        dt.setTypeParent("DATA_TYPE");
        dt.setCode("STRING");
        dt.setValue("String");
        dt.setType("Standard Data Types");
        dt.setDescription("String");
        return dt;
    }

    //Override publicMap
    @Override
    public Map<String, Object> publicMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", getId());
        map.put("name", getName());
        map.put("symbol", getSymbol());
        map.put("unit", getUnit());
        map.put("timeToLive", getTimeToLive());
        map.put("timeSeries", getTimeSeries());
        map.put("typeParent", getTypeParent());
        map.put("defaultValue", getDefaultValue());
        map.put("multiple", getMultiple());
        map.put("thingTypeFieldTemplateId", getThingTypeFieldTemplateId());
        map.put("type", getDataType().getId());
        map.put("dataType", getDataType().publicMap());
        map.put("dataTypeThingTypeId", getDataTypeThingTypeId());
        map.put("dataType", getDataType().publicMap());
        map.put("dataTypeThingTypeId", getDataTypeThingTypeId());
        return map;
    }


    /**
     * Evaluate if a Thing Type Id is native object
     *
     * @return true if thingTypeId is native object, false otherwise
     */
    public boolean isNativeObject() {
        return getDataType().getId() == ThingTypeField.Type.TYPE_ZONE.value ||
                getDataType().getId() == ThingTypeField.Type.TYPE_SHIFT.value ||
                getDataType().getId() == ThingTypeField.Type.TYPE_GROUP.value ||
                getDataType().getId() == ThingTypeField.Type.TYPE_LOGICAL_READER.value;
    }

    /**
     * Evaluate if a Thing Type Id is thing type udf
     *
     * @return true if thingTypeId is thing type udf, false otherwise
     */
    public boolean isThingTypeUDF() {
        return getDataType().getId() == ThingTypeField.Type.TYPE_THING_TYPE.value;
    }

    /**
     * Transform and return the thing type field in Map format to reports.
     * example:<br>
     * {<br>
     *              "propertyName": "doorEvent",<br>
     *              "label": "doorEvent",<br>
     *              "sortBy": "",<br>
     *              "thingTypeId": 3,<br>
     *              "editInline": false,<br>
     *              "thingTypeFieldId": 41,<br>
     *              "parentThingType": null,<br>
     *              "required": false,<br>
     *              "thingTypeIdReport": 3,<br>
     *              "pickList": false,<br>
     *              "allPropertyData": false,<br>
     *              "displayOrder": 1<br>
     * }
     *
     * @return a instance of {@link Map}<{@link String}, {@link Object}>
     */
    public Map<String, Object> getReportFieldMap() {
        Map<String, Object> thingFieldMap = new HashMap<>();
        thingFieldMap.put("propertyName", this.getName());
        thingFieldMap.put("label", this.getName());
        thingFieldMap.put("sortBy", StringUtils.EMPTY);
        thingFieldMap.put("thingTypeId", this.getThingType().getId());
        thingFieldMap.put("editInline", false);
        thingFieldMap.put("thingTypeFieldId", this.getId());
        thingFieldMap.put("parentThingType", null);
        thingFieldMap.put("required", false);
        thingFieldMap.put("thingTypeIdReport", this.getThingType().getId());
        thingFieldMap.put("pickList", false);
        thingFieldMap.put("allPropertyData", false);
        thingFieldMap.put("displayOrder", false);
        return thingFieldMap;
    }

    public Map<String, Object> getThingTypeFieldMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", getId());
        map.put("name", getName());
        map.put("type", getDataType().getId());
        map.put("typeCode", dataType.getCode());
        map.put("dataTypeThingTypeId", getDataTypeThingTypeId());
        map.put("typeCode.type", dataType.getType());
        map.put("typeParentCode", getTypeParent());
        map.put("dataType", getDataType().publicMap());
        return map;
    }
}
