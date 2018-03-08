package com.tierconnect.riot.commons.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * DataTypeDto enumerator.
 *
 * @author jantezana
 * @version 2017/01/24
 */
@Deprecated
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum DataTypeDto implements Serializable {
    TYPE_TEXT(1L, "STRING", "String", "Standard Data Types", "DATA_TYPE", "String", "java.lang.String"),
    TYPE_LON_LAT_ALT(2L, "COORDINATES", "Coordinates", "Standard Data Types", "DATA_TYPE", "Coordinates", "java.lang.String"),
    TYPE_XYZ(3L, "XYZ", "XYZ", "Standard Data Types", "DATA_TYPE", "XYZ", "java.lang.String"),
    TYPE_NUMBER(4L, "NUMBER", "Number (Float)", "Standard Data Types", "DATA_TYPE", "Number (Float)", "java.math.BigDecimal"),
    TYPE_BOOLEAN(5L, "BOOLEAN", "Boolean", "Standard Data Types", "DATA_TYPE", "Boolean", "java.lang.Boolean"),
    TYPE_IMAGE_ID(6L, "IMAGE", "Image", "Standard Data Types", "DATA_TYPE", "Image", "java.lang.String"),
    TYPE_SHIFT(7L, "SHIFT", "Shift", "Native Objects", "DATA_TYPE", "Shift", "com.tierconnect.riot.iot.entities.Shift"),
    TYPE_IMAGE_URL(8L, "IMAGE_URL", "Image URL", "Standard Data Types", "DATA_TYPE", "Image URL", "java.lang.String"),
    TYPE_ZONE(9L, "ZONE", "Zone", "Native Objects", "DATA_TYPE", "Zone", "com.tierconnect.riot.iot.entities.Zone"),
    TYPE_DATE(11L, "DATE", "Date", "Standard Data Types", "DATA_TYPE", "Date", "java.util.Date"),
    TYPE_URL(12L, "URL", "Url", "Standard Data Types", "DATA_TYPE", "Url", "java.lang.String"),
    TYPE_ZPL_SCRIPT(13L, "ZPL_SCRIPT", "ZPL Script", "Standard Data Types", "DATA_TYPE", "ZPL Script", "java.lang.String"),
    TYPE_GROUP(22L, "GROUP", "Group", "Native Objects", "DATA_TYPE", "Group", "com.tierconnect.riot.appcore.entities.Group"),
    TYPE_LOGICAL_READER(23L, "LOGICAL_READER", "Logical Reader", "Native Objects", "DATA_TYPE", "Logical Reader",
                        "com.tierconnect.riot.iot.entities.LogicalReader"),
    TYPE_TIMESTAMP(24L, "TIMESTAMP", "Timestamp", "Standard Data Types", "DATA_TYPE", "TimesTamp", "java.lang.Long"),
    TYPE_SEQUENCE(25L, "SEQUENCE", "Sequence", "Standard Data Types", "DATA_TYPE", "Sequence", "java.math.BigDecimal"),
    TYPE_FORMULA(26L, "FORMULA", "Expression of formula", "Standard Data Types", "DATA_TYPE", "Expression", "java.lang.String"),
    TYPE_THING_TYPE(27L, "THING_TYPE", "Thing Type", "Native Objects", "DATA_TYPE", "Thing Type", "com.tierconnect.riot.iot.entities.Thing"),
    TYPE_ATTACHMENTS(28L, "ATTACHMENT", "Attach one or many files", "Standard Data Types", "DATA_TYPE", "Attachments" + "", "java.lang.String");

    private static final long serialVersionUID = 1L;

    @JsonProperty
    public Long id;

    @JsonProperty
    public String code;

    @JsonProperty
    public String description;

    @JsonProperty
    public String type;

    @JsonProperty
    public String typeParent;

    @JsonProperty
    public String value;

    @JsonProperty
    public String clazz;

    /**
     * Default constructor of {@link com.tierconnect.riot.commons.dtos.DataTypeDto}
     *
     * @param id          the id
     * @param code        the code
     * @param description the description
     * @param type        the type
     * @param typeParent  the type parent
     * @param value       the value
     * @param clazz       the class
     */
    DataTypeDto(Long id,
                String code,
                String description,
                String type,
                String typeParent,
                String value,
                String clazz) {
        this.id = id;
        this.code = code;
        this.description = description;
        this.type = type;
        this.typeParent = typeParent;
        this.value = value;
        this.clazz = clazz;
    }

    /**
     * Gets the data type by code.
     *
     * @param code the code
     * @return the data type
     */
    @JsonCreator
    public static DataTypeDto fromCode(String code) {
        DataTypeDto result = null;
        for (DataTypeDto dataTypeDto : DataTypeDto.values()) {
            if (dataTypeDto.code.equalsIgnoreCase(code)) {
                result = dataTypeDto;
                break;
            }
        }
        return result;
    }

    /**
     * Gets the data type by id.
     *
     * @param id the id
     * @return the data type
     */
    @JsonCreator
    public static DataTypeDto fromId(Long id) {
        DataTypeDto result = null;
        for (DataTypeDto dataTypeDto : DataTypeDto.values()) {
            if (dataTypeDto.id.equals(id)) {
                result = dataTypeDto;
                break;
            }
        }
        return result;
    }
}
