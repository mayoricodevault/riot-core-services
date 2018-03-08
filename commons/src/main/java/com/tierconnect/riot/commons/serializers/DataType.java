package com.tierconnect.riot.commons.serializers;

import com.tierconnect.riot.commons.dtos.GroupDto;
import com.tierconnect.riot.commons.dtos.LogicalReaderDto;
import com.tierconnect.riot.commons.dtos.ShiftDto;
import com.tierconnect.riot.commons.dtos.ThingDto;
import com.tierconnect.riot.commons.dtos.ZoneDto;
import com.tierconnect.riot.commons.utils.FormatUtil;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;

/**
 * Created by achambi on 11/8/16.
 * Enum class to convert via reflection a object to corresponding data type.
 */
public enum DataType {
    /**
     * Enum values
     */

    TYPE_TEXT(1L, ConvertUtils.class, "convert", "STANDARD", Object.class),
    TYPE_LON_LAT_ALT(2L, FormatUtil.class, "parseArray", "STANDARD", String.class),
    TYPE_XYZ(3L, FormatUtil.class, "parseArray", "STANDARD", String.class),
    TYPE_NUMBER(4L, NumberUtils.class, "createDouble", "STANDARD", String.class),
    TYPE_BOOLEAN(5L, BooleanUtils.class, "toBoolean", "STANDARD", String.class),
    TYPE_IMAGE_ID(6L, ConvertUtils.class, "convert", "STANDARD", Object.class),
    TYPE_SHIFT(7L, ShiftDto.class, "", "NATIVE", null),
    TYPE_IMAGE_URL(8L, ConvertUtils.class, "convert", "STANDARD", Object.class),
    TYPE_ZONE(9L, ZoneDto.class, "", "NATIVE", null),
    TYPE_JSON(10L, ConvertUtils.class, "convert", "STANDARD", Object.class),
    TYPE_DATE(11L, FormatUtil.class, "parse", "STANDARD", String.class),
    TYPE_URL(12L, ConvertUtils.class, "convert", "STANDARD", Object.class),
    TYPE_ZPL_SCRIPT(13L, ConvertUtils.class, "convert", "STANDARD", Object.class),
    TYPE_GROUP(22L, GroupDto.class, "", "NATIVE", null),
    TYPE_LOGICAL_READER(23L, LogicalReaderDto.class, "", "NATIVE", null),
    TYPE_TIMESTAMP(24L, NumberUtils.class, "toLong", "STANDARD", String.class),
    TYPE_SEQUENCE(25L, NumberUtils.class, "createDouble", "STANDARD", String.class),
    TYPE_FORMULA(26L, ConvertUtils.class, "convert", "STANDARD", Object.class),
    TYPE_THING_TYPE(27L, ThingDto.class, "", "NATIVE", null),
    TYPE_ATTACHMENTS(28L, ConvertUtils.class, "convert", "STANDARD", Object.class),
    TYPE_ICON(29L, ConvertUtils.class, "convert", "STANDARD", String.class),
    TYPE_COLOR(30L, ConvertUtils.class, "convert", "STANDARD", String.class);

    /**
     * Code correspondingly to correlation id in mysql table [dataType].
     */
    private final Long id;

    /**
     * Field class that is responsible for instantiating the call via reflection to the parse method.
     */
    private final Class<?> clazz;

    /**
     * Method to invoke via reflection.
     */
    private final String parseMethodName;

    /**
     * Type of data [NATIVE, STANDARD]
     */
    private final String type;
    private final Class<?> argumentType;

    /**
     * Enum constructor.
     *
     * @param id              id of data type.
     * @param clazz           Class that converts the data.
     * @param parseMethodName method name to parse the value.
     * @param type            Type of data [NATIVE, STANDARD]
     */
    DataType(Long id,
             Class<?> clazz,
             String parseMethodName,
             String type,
             Class<?> argumentType) {
        this.id = id;
        this.clazz = clazz;
        this.parseMethodName = parseMethodName;
        this.type = type;
        this.argumentType = argumentType;
    }

    /**
     * Get data type enum.
     *
     * @param id a integer to get data type.
     * @return enum data type.
     * @throws IllegalArgumentException exception when the code not found in enum.
     */
    public static DataType getDataTypeById(final long id)
    throws IllegalArgumentException {
        Logger logger = Logger.getLogger(DataType.class);
        try {
            for (DataType item : DataType.values()) {
                if (item.getId() == id)
                    return item;
            }
            throw new IllegalArgumentException(String.format("Data type %1$d not found.", id));
        } catch (IllegalArgumentException ex) {
            logger.error(ex);
            throw ex;
        }
    }

    /**
     * Get Code via getter.
     *
     * @return Long Contains the correlative id
     */
    public Long getId() {
        return id;
    }

    /**
     * get Class declaration of data type.
     *
     * @return Class declaration.
     */
    public Class<?> getClazz() {
        return clazz;
    }

    /**
     * Get method name to parse the value.
     *
     * @return Method name in string format.
     */
    public String getParseMethodName() {
        return parseMethodName;
    }

    /**
     * get data Type [NATIVE, STANDARD]
     *
     * @return String to contains the data Type
     */
    //TODO Optional Change this to enum values: [NATIVE, STANDARD]
    public String getType() {
        return type;
    }

    /**
     * get argument type to use in method call via reflection.
     *
     * @return Class argument type.
     */
    public Class<?> getArgumentType() {
        return argumentType;
    }
}
