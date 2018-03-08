package com.tierconnect.riot.iot.services;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.Tuple;
import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.commons.utils.DateHelper;
import com.tierconnect.riot.iot.dao.SequenceDAO;
import com.tierconnect.riot.iot.dao.util.FormulaUtil;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static com.mysema.query.group.GroupBy.groupBy;
import static com.tierconnect.riot.commons.utils.DateHelper.isTimeStampMillis;

/**
 * @author garivera
 */
public class ThingTypeFieldService extends ThingTypeFieldServiceBase {
    static Logger logger = Logger.getLogger(ThingTypeFieldService.class);

    /**
     * return list of {@link ThingTypeField} by  idThingTypeFieldTemplate
     *
     * @param idThingTypeFieldTemplate
     * @return
     */
    public List<ThingTypeField> getThingTypeFieldByIdThing(Long idThingTypeFieldTemplate) {
        HibernateQuery query = ThingTypeFieldService.getThingTypeFieldDAO().getQuery();
        return query.where(QThingTypeField.thingTypeField.thingTypeFieldTemplateId.eq(idThingTypeFieldTemplate))
                .list(QThingTypeField.thingTypeField);
    }

    public List<ThingTypeField> getThingTypeFieldByName(String name) {
        HibernateQuery query = ThingTypeFieldService.getThingTypeFieldDAO().getQuery();
        return query.where(QThingTypeField.thingTypeField.name.eq(name))
                .list(QThingTypeField.thingTypeField);
    }

    public List<ThingTypeField> getThingTypeFieldByNameAndTimeSeries(String name, Boolean timeSeries) {
        HibernateQuery query = ThingTypeFieldService.getThingTypeFieldDAO().getQuery();
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(QThingTypeField.thingTypeField.name.eq(name));
        if (timeSeries != null) {
            builder.and(QThingTypeField.thingTypeField.timeSeries.eq(timeSeries));
        }
        return query.where(builder).list(QThingTypeField.thingTypeField);
    }

    public List<ThingTypeField> getThingTypeFieldByNameAndTypeCode(String name, String thingTypeCode) {
        HibernateQuery query = new HibernateQuery(HibernateSessionFactory.getInstance().getCurrentSession());
        return query.from(QThingTypeField.thingTypeField)
                .join(QThingTypeField.thingTypeField.thingType, QThingType.thingType)
                .where(QThingType.thingType.thingTypeCode.eq(thingTypeCode)
                        .and(QThingTypeField.thingTypeField.name.eq(name)))
                .setCacheable(true)
                .list(QThingTypeField.thingTypeField);
    }

    public List<ThingTypeField> getThingTypeFieldByType(Long type, Long thingType) {
        HibernateQuery query = ThingTypeFieldService.getThingTypeFieldDAO().getQuery();
        return query.where(QThingTypeField.thingTypeField.dataType.id.eq(type)
                .and(QThingTypeField.thingTypeField.thingType.id
                        .eq(
                                thingType)))
                .list(QThingTypeField.thingTypeField);
    }

    /**
     * return the fields associated to a thing type
     */
    public Map<String, ThingTypeField> getThingTypeFieldByType(Number thingType) {
        HibernateQuery query = ThingTypeFieldService.getThingTypeFieldDAO().getQuery();
        query.where(QThingTypeField.thingTypeField.thingType.id.eq(thingType.longValue()));
        return query.transform(groupBy(QThingTypeField.thingTypeField.name).as(QThingTypeField.thingTypeField));
    }

    /**
     * return the fields associated to a thing type
     */
    public Map<String, ThingTypeField> getThingTypeFieldByThingTypeCode(String thingTypeCode) {
        HibernateQuery query = ThingTypeFieldService.getThingTypeFieldDAO().getQuery();
        query.where(QThingTypeField.thingTypeField.thingType.thingTypeCode.eq(thingTypeCode));
        return query.transform(groupBy(QThingTypeField.thingTypeField.name).as(QThingTypeField.thingTypeField));
    }

    public static List<ThingTypeField> getThingTypeFieldsByDataTypeThingType(Long thingTypeId) {
        HibernateQuery query = ThingTypeFieldService.getThingTypeFieldDAO().getQuery();
        return query.where(QThingTypeField.thingTypeField.dataTypeThingTypeId.eq(thingTypeId)).list(QThingTypeField.thingTypeField);
    }

    /**
     * return the fields associated to a thing type
     */
    public List<ThingTypeField> getThingTypeField(Long thingTypeId) {
        HibernateQuery query = ThingTypeFieldService.getThingTypeFieldDAO().getQuery();
        List<ThingTypeField> thingTypeFields = query.where(QThingTypeField.thingTypeField.thingType.id.eq(thingTypeId))
                .list(QThingTypeField.thingTypeField);

        return thingTypeFields;
    }


    /**
     * Get Thing Type Fiel Templated Id By Thing Type
     *
     * @param thingTypeId thing Type  Id
     * @param name        name
     * @return Thing Type Field
     */
    public ThingTypeField getThingTypeFielByThingType(Long thingTypeId, String name) {
        HibernateQuery query = ThingTypeFieldService.getThingTypeFieldDAO().getQuery();
        return query.where(QThingTypeField.thingTypeField.thingType.id.eq(thingTypeId)
                .and(QThingTypeField.thingTypeField.name.eq(name)))
                .uniqueResult(QThingTypeField.thingTypeField);
    }

    class CustomComparator implements Comparator<String> {
        @Override
        public int compare(String o1, String o2) {

            if (o1 != null && o2 != null) {
                return o1.compareTo(o2);
            } else {
                return o1 == null ? -1 : 1;
            }
        }

    }

    public static List<Long> getThingTypeFieldIds(List<Thing> things, List<String> filters, boolean parents) {
        List<Long> idList = new ArrayList<Long>();
        for (Thing thing : things) {
            for (ThingTypeField thingField : thing.getThingType().getThingTypeFields()) {
                if (filters != null) {
                    if (filters.contains(thingField.getName())) {
                        idList.add(thingField.getId());
                    }
                } else  //no filters add all
                {
                    idList.add(thingField.getId());
                }
            }

            //get parents' ids
            if (parents && thing.getParent() != null) {
                for (ThingTypeField thingField : thing.getParent().getThingType().getThingTypeFields()) {
                    if (filters != null && filters.contains(thingField.getName())) {
                        idList.add(thingField.getId());
                    }
                }

            }
        }
        return idList;
    }

    public static List<Long> getThingTypeFieldIds(List<Thing> things) {
        return getThingTypeFieldIds(things, null, false);
    }

    public static List<Long> getThingTypeFieldIds(List<Thing> things, List<String> filters) {
        return getThingTypeFieldIds(things, filters, false);
    }

    public static List<Long> getIds(List<ThingTypeField> thingFields) {
        List<Long> idList = new ArrayList<>();
        for (ThingTypeField thingField : thingFields) {
            idList.add(thingField.getId());
        }
        return idList;
    }


    public List<ThingTypeField> getThingTypeFieldByPropertyName(List<String> propertyNames) {
        if (propertyNames != null && propertyNames.size() > 0) {
            return ThingTypeFieldService.getThingTypeFieldDAO().getQuery().where(QThingTypeField.thingTypeField.name.in(propertyNames)).list(QThingTypeField.thingTypeField);
        }
        return new LinkedList<>();
    }

    public List<Long> getThingTypeFieldIdsByPropertyName(List<String> propertyNames) {
        if (propertyNames != null && propertyNames.size() > 0) {
            return ThingTypeFieldService.getThingTypeFieldDAO().getQuery().where(QThingTypeField.thingTypeField.name.in(propertyNames)).list(QThingTypeField.thingTypeField.id);
        }
        return new LinkedList<>();
    }

    public ThingTypeField insert(ThingTypeField thingTypeField) {
        validateInsert(thingTypeField);
        Long id = getThingTypeFieldDAO().insert(thingTypeField);
        thingTypeField.setId(id);
        // create sequence just for sequence type
        ThingTypeFieldService.getInstance().createSequence(thingTypeField);
        return thingTypeField;
    }

    public void delete(ThingTypeField thingTypeField) {
        validateDelete(thingTypeField);
        // delete sequence just for sequence type
        ThingTypeFieldService.getInstance().removeSequence(thingTypeField);

        //TODO: remove the if statement, it was added just to fix error on sequence deletion
        if (thingTypeField.getDataType().getId().compareTo(Long.valueOf(ThingTypeField.Type.TYPE_SEQUENCE.value)) != 0) {
            getThingTypeFieldDAO().delete(thingTypeField);
        }
    }

    public void createSequence(ThingTypeField thingTypeField) {
        // create sequence just for sequence type
        try {
            if (thingTypeField.getDataType().getId().compareTo(Long.valueOf(ThingTypeField.Type.TYPE_SEQUENCE.value)) == 0) {
                Long initialValue = null;
                if (null == thingTypeField.getDefaultValue() || thingTypeField.getDefaultValue().isEmpty())
                    initialValue = 1L; // default value
                else
                    initialValue = Long.valueOf(thingTypeField.getDefaultValue());
                SequenceDAO.getInstance().loadSequence(thingTypeField, initialValue, true);
            }
        } catch (NumberFormatException e) {
            throw new UserException("Default value must be a number for property " + thingTypeField.getName(), e);
        }
    }

    public void removeSequence(ThingTypeField thingTypeField) {
        // remove sequence just for sequence type
        try {
            if (thingTypeField.getDataType().getId().compareTo(Long.valueOf(ThingTypeField.Type.TYPE_SEQUENCE.value)) == 0) {
                SequenceDAO.getInstance().removeSequence(thingTypeField);
            }
        } catch (Exception e) {
            throw new UserException("Error removing sequence for property " + thingTypeField.getName(), e);
        }
    }

    /*****************************************************
     * Method to get a Native Object based on the id of its type and its name
     *******************************************************/
    public Object getNativeObject(
            Long type
            , String codeNativeObject
            , Map<String, Map<String, Object>> cache
            , Group group
            , Date storageDate) {
        Object nativeObject = null;

        try {
            if (type == ThingTypeField.Type.TYPE_LOGICAL_READER.value) {
                if (cache != null)
                    nativeObject = cache.get("logicalReader").get(codeNativeObject);
                else {
                    nativeObject = LogicalReaderService.getInstance().getByCodeAndGroup(codeNativeObject, group.getHierarchyName());
                }
            } else if (type == ThingTypeField.Type.TYPE_ZONE.value) {
                //Support for unknown zones
                Map<String, Object> unknownZone = new HashMap<>();
                unknownZone.put("name", "Unknown");
                unknownZone.put("code", "unknown");
                unknownZone.put("facilityMap", "unknown");
                unknownZone.put("zoneGroup", "unknown");
                unknownZone.put("zoneType", "unknown");
                unknownZone.put("facilityMapTime", storageDate);
                unknownZone.put("zoneGroupTime", storageDate);
                unknownZone.put("zoneTypeTime", storageDate);
                unknownZone.put("id", 0);
                nativeObject = unknownZone;

                if (cache != null) {
                    if (cache.get("zone").containsKey(codeNativeObject))
                        nativeObject = cache.get("zone").get(codeNativeObject);
                    else if (cache.get("zoneByName").containsKey(codeNativeObject))
                        nativeObject = cache.get("zoneByName").get(codeNativeObject);
                    else if (cache.get("zoneById").containsKey(codeNativeObject))
                        nativeObject = cache.get("zoneById").get(codeNativeObject);
                    else if (!codeNativeObject.toLowerCase().equals("unknown") &&
                            !codeNativeObject.equals("0")
                            && (Integer.parseInt(codeNativeObject) != 0)
                            && (Long.parseLong(codeNativeObject) != 0L)) {
                        logger.warn("Zone \"" + codeNativeObject + "\" does not exist in database. Unknown zone assigned instead of.");
                    }
                } else {
                    if (codeNativeObject != null && codeNativeObject.equals("unknown")) {
                        nativeObject = unknownZone;
                        logger.warn("Zone \"" + codeNativeObject + "\" does not exist in database. Unknown zone assigned instead of.");
                    } else {
                        nativeObject = ZoneService.getInstance().getByCodeAndGroup(codeNativeObject, group);
                    }

                }

            } else if (type == ThingTypeField.Type.TYPE_SHIFT.value) {
                if (cache != null) {
                    if (cache.get("shift").containsKey(codeNativeObject))
                        nativeObject = cache.get("shift").get(codeNativeObject);
                    else if (cache.get("shiftByName").containsKey(codeNativeObject))
                        nativeObject = cache.get("shiftByName").get(codeNativeObject);
                    else if (cache.get("shiftById").containsKey(codeNativeObject))
                        nativeObject = cache.get("shiftById").get(codeNativeObject);
                } else {
                    nativeObject = ShiftService.getInstance().getByCodeAndGroup(codeNativeObject, group.getHierarchyName());
                }
            } else if (type == ThingTypeField.Type.TYPE_GROUP.value) {
                if (cache != null)
                    nativeObject = cache.get("group").get(codeNativeObject);
                else
                    nativeObject = GroupService.getInstance().getByCodeAndGroup(codeNativeObject, group.getHierarchyName());
            }
        } catch (Exception e) {
            throw new UserException("Error to obtain data of the Native Object. " + e.getMessage(), e);
        }

        return nativeObject;
    }

    /*********************************
     * Method to get a List of Ids of an Object
     ********************************/
    public List<Long> getListOfIds(List<?> listOfObjects) {
        List<Long> response = null;
        if (listOfObjects != null && listOfObjects.size() > 0) {
            response = new ArrayList<Long>();

            for (Object data : listOfObjects) {
                if (data instanceof Zone) {
                    response.add(((Zone) data).getId());
                } else if (data instanceof Group) {
                    response.add(((Group) data).getId());
                } else if (data instanceof LocalMap) {
                    response.add(((LocalMap) data).getId());
                } else if (data instanceof ZoneGroup) {
                    response.add(((ZoneGroup) data).getId());
                } else {
                    throw new Error("unsupported type=" + data);
                }
            }
        }
        return response;
    }

    public ThingTypeField getByThingTypeCodeAndDataTypeCode(String thingTypeCode, String dataTypeCode) {
        HibernateQuery query = new HibernateQuery(HibernateSessionFactory.getInstance().getCurrentSession());
        return query.from(QThingTypeField.thingTypeField)
                .join(QThingTypeField.thingTypeField.thingType, QThingType.thingType)
                .join(QThingTypeField.thingTypeField.dataType, QDataType.dataType)
                .where(QThingType.thingType.thingTypeCode.eq(thingTypeCode)
                        .and(QDataType.dataType.code.eq(dataTypeCode)))
                .setCacheable(true)
                .singleResult(QThingTypeField.thingTypeField);
    }

    public ThingTypeField getByNameAndThingTypeCode(String name, String thingTypeCode) {
        return getThingTypeFieldDAO().selectBy(QThingTypeField.thingTypeField.name.eq(name)
                .and(QThingTypeField.thingTypeField.thingType.thingTypeCode.eq(thingTypeCode)));
    }

    public List<ThingTypeField> getByThingTypeCode(String thingTypeCode) {
        return getThingTypeFieldDAO().selectAllBy(QThingTypeField.thingTypeField.thingType.thingTypeCode.eq(thingTypeCode));
    }
    //Validations

    /**
     * Used to validate the type of a value an return a validation object response
     *
     * @param fieldName
     * @param value
     * @param type
     * @return the object with the response of the validation
     */
    public ValidationBean validateFieldValue(String fieldName, Object value, Long type) {
        ValidationBean val = new ValidationBean();
        val.setError(!checkDataType(value, type, false));
        if (val.isError()) {
            val.setErrorDescription("Invalid value for field '" + fieldName + "'");
        }
        return val;
    }

    /**
     * @param value       to check
     * @param type        value's data type
     * @param strinctDate check if the value is of type Date ignoring milliseconds format
     * @return true if value is a valid data type value
     */
    public boolean checkDataType(Object value, Long type, boolean strinctDate) {
        if (type.equals(ThingTypeField.Type.TYPE_LONLATALT.value)) {
            return isCoordinates(value);
        }
        if (type.equals(ThingTypeField.Type.TYPE_XYZ.value)) {
            return isCoordinates(value);
        }
        if (type.equals(ThingTypeField.Type.TYPE_BOOLEAN.value)) {
            return (strinctDate ? value instanceof Boolean : isBoolean(value));
        }
        if (type.equals(ThingTypeField.Type.TYPE_NUMBER.value)) {
            return isNumberFloat(value);
        }
        if (type.equals(ThingTypeField.Type.TYPE_TIMESTAMP.value)) {
            return isNumber(value);
        }
        if (type.equals(ThingTypeField.Type.TYPE_DATE.value)) {
            return (strinctDate) ? !isTimeStampMillis(String.valueOf(value)) && (value instanceof Date) : isDate(value);
        }
        if (type.equals(ThingTypeField.Type.TYPE_IMAGE_URL.value) || type.equals(ThingTypeField.Type.TYPE_URL.value)) {
            return isURL(value);
        }
        if (type.equals(ThingTypeField.Type.TYPE_FORMULA.value)) {
            return isExpression(value);
        }
        if (type.equals(ThingTypeField.Type.TYPE_ZPL_SCRIPT.value)) {
            return isZPLScript(value);
        }
        if (type.equals(ThingTypeField.Type.TYPE_TEXT.value)) {
            return !strinctDate || (value instanceof String);
        }

        // Todo: validate those data types
//        TYPE_IMAGE_ID   (6L),
//        TYPE_SHIFT      (7L),
//        TYPE_ZONE       (9L),
//        TYPE_JSON       (10L),
//        TYPE_GROUP      (22L),
//        TYPE_LOGICAL_READER  (23L),
//        TYPE_TIMESTAMP  (24L),
//        TYPE_SEQUENCE   (25L),
//        TYPE_FORMULA    (26L),
//        TYPE_THING_TYPE (27L),
//        TYPE_ATTACHMENTS(28L);

        return false;
    }

    /**
     * @param type
     * @return true if it is a valid Data type to check
     */
    public boolean isValidDataTypeToCheck(Long type) {
        List<Long> types = new ArrayList<>();
        types.add(ThingTypeField.Type.TYPE_LONLATALT.value);
        types.add(ThingTypeField.Type.TYPE_XYZ.value);
        types.add(ThingTypeField.Type.TYPE_BOOLEAN.value);
        types.add(ThingTypeField.Type.TYPE_NUMBER.value);
        types.add(ThingTypeField.Type.TYPE_DATE.value);
        types.add(ThingTypeField.Type.TYPE_IMAGE_URL.value);
        types.add(ThingTypeField.Type.TYPE_URL.value);
        types.add(ThingTypeField.Type.TYPE_FORMULA.value);
        types.add(ThingTypeField.Type.TYPE_ZPL_SCRIPT.value);
        types.add(ThingTypeField.Type.TYPE_TIMESTAMP.value);
        return types.contains(type);
    }

    /**
     * Verify value is a Boolean data type
     *
     * @param value
     * @return true if it is a Boolean data type
     */
    public boolean isBoolean(Object value) {
        return (("true".equalsIgnoreCase(String.valueOf(value))) || ("false".equalsIgnoreCase(String.valueOf(value))));
    }

    /**
     * @param value
     * @return true if it is a Coordinate data type
     */
    public boolean isCoordinates(Object value) {
        return isValidLocation(value);
    }

    /**
     * @param value
     * @return true if it is a Date data type
     */
    public boolean isDate(Object value) {
        return (!isEmptyData(value) && DateHelper.getDateAndDetermineFormat(value.toString()) != null);
    }

    /**
     * @param value
     * @return true if if is an Expression data type
     */
    public boolean isExpression(Object value) {
        //return true only if value different than null because FORMULA field could be anything from result of evaluating expression
        return value != null;
//        return ((value != null) && value.toString().contains("${") && value.toString().contains("}") &&
//        !value.toString().contains("^XA") && !value.toString().contains("^XZ"));
    }

    /**
     * isNumber
     *
     * @param value
     * @return true if it is a Number data type
     */
    public boolean isNumber(Object value) {
        try {
            if (!isEmptyData(value)) {
                new BigDecimal(value.toString());
                return true;
            }
            return false;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

    /**
     * isNumberFloat
     *
     * @param value value to validate
     * @return true if it is a Number data type
     */
    public boolean isNumberFloat(Object value) {
        try {
            if (!isEmptyData(value)) {
                Float.parseFloat(value.toString());
                return true;
            }
            return false;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

    /**
     * @param value
     * @return true if it is a valid ZPLScript
     */
    public boolean isZPLScript(Object value) {
        return ((value != null) && value.toString().contains("^XA") && value.toString().contains("^XZ"));
    }

    /**
     * @param value
     * @return true if it is an URL data type
     */
    public boolean isURL(Object value) {
        try {

            if (!isEmptyData(value)) {
                new URL(value.toString());
                return true && new UrlValidator().isValid(value.toString());//URL class accepts only protocol
            }
            return false;
        } catch (MalformedURLException e) {
            return false;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * @param value
     * @return true if it is a valid Location
     */
    public boolean isValidLocation(Object value) {
        return (!isEmptyData(ConvertUtils.convert(value)) && isValidLengthLocation(ConvertUtils.convert(value)) &&
                hasThreeDoubleNumbers
                        (ConvertUtils.convert(value)));
    }

    /**
     * @param value
     * @return true if it has a three double numbers
     */
    private boolean hasThreeDoubleNumbers(String value) {
        String[] values = value.split(";");
        return (isNumberFloat(values[0]) && isNumberFloat(values[1]) && isNumberFloat(values[2]));
    }

    /**
     * @param value
     * @return true if it is an empty data
     */
    public boolean isEmptyData(Object value) {
        return (StringUtils.isBlank(ConvertUtils.convert(value)));
    }

    /**
     * @param value
     * @return true if it is a valid Length Location
     */
    public boolean isValidLengthLocation(Object value) {
        if (!isEmptyData(value)) {
            String[] location = value.toString().split(";");
            return !(location.length != 3 || location[0] == null || location[1] == null || location[2] == null);
        }
        return false;
    }

    /**
     * @param thingTypeFieldTemplateId
     * @return
     */
    public List<ThingTypeField> getThingTypeFieldByThingTypeFieldTemplate(Long thingTypeFieldTemplateId) {
        HibernateQuery query = ThingTypeFieldService.getThingTypeFieldDAO().getQuery();
        return query.where(QThingTypeField.thingTypeField.thingTypeFieldTemplateId.eq(thingTypeFieldTemplateId))
                .list(QThingTypeField.thingTypeField);
    }

    /**
     * Get list of fields of an specific thing type template
     *
     * @param
     * @return
     */
    public List<ThingTypeField> getThingTypeFieldByTemplate(Long thingTypeTemplateId) {
        HibernateQuery query = ThingTypeFieldService.getThingTypeFieldDAO().getQuery();
        return query.where(QThingTypeField.thingTypeField.thingType.thingTypeTemplate.id.eq(thingTypeTemplateId))
                .list(QThingTypeField.thingTypeField);
    }

    /**
     * Populate ThingTypeField for a specific Thing Type
     *
     * @param thingType
     * @param name
     * @param unitName
     * @param unitSymbol
     * @param thingTypeParent
     * @param type
     * @param isTimeSeries
     * @param defaultValue
     * @param thingTypeFieldTemplateId
     * @return
     */
    public ThingTypeField insertThingTypeField(
            ThingType thingType,
            String name,
            String unitName,
            String unitSymbol,
            String thingTypeParent,
            DataType type,
            boolean isTimeSeries,
            String defaultValue,
            Long thingTypeFieldTemplateId) {
        ThingTypeField thingTypeField = new ThingTypeField(name,
                unitName,
                unitSymbol,
                thingTypeParent,
                type,
                null);
        thingTypeField.setTimeSeries(isTimeSeries);
        thingTypeField.setThingType(thingType);
        thingTypeField.setMultiple(false);
        if (thingType.getThingTypeFields() == null) {
            thingType.setThingTypeFields(new HashSet());
        }
        thingType.getThingTypeFields().add(thingTypeField);
        if (defaultValue != null && !defaultValue.trim().equals("")) {
            thingTypeField.setDefaultValue(defaultValue);
        }
        if (thingTypeFieldTemplateId != null) {
            thingTypeField.setThingTypeFieldTemplateId(thingTypeFieldTemplateId);
        }
        thingTypeField.setDataType(type);
        ThingTypeFieldService.getInstance().insert(thingTypeField);
        return thingTypeField;
    }

    public static boolean isDateTimeStampType(DataType dataType) {
        return dataType != null && isDateTimeStampType(dataType.getId());
    }

    public static boolean isDateTimeStampType(Long thingFieldType) {
        return (ThingTypeField.Type.isDateOrTimestamp(thingFieldType));
    }

    @Override
    public void validateInsert(ThingTypeField thingTypeField) {
        super.validateInsert(thingTypeField);
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QThingTypeField.thingTypeField.name.eq(thingTypeField.getName()));
        be = be.and(QThingTypeField.thingTypeField.thingType.thingTypeCode.eq(thingTypeField.getThingType().getThingTypeCode()));
        List<ThingTypeField> thingTypeFields = getThingTypeFieldDAO().selectAllBy(be);
        if (thingTypeField.getId() == null && !thingTypeFields.isEmpty()) {
            //TODO: add more info to the error message, maybe get query from predicate?
            throw new UserException("Thing type field already exists.");
        }
    }

    @Override
    public void validateUpdate(ThingTypeField thingTypeField) {
        super.validateUpdate(thingTypeField);
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QThingTypeField.thingTypeField.name.eq(thingTypeField.getName()));
        be = be.and(QThingTypeField.thingTypeField.thingType.thingTypeCode.eq(thingTypeField.getThingType().getThingTypeCode()));
        List<ThingTypeField> thingTypeFields = getThingTypeFieldDAO().selectAllBy(be);
        if (thingTypeField.getId() == null && !thingTypeFields.isEmpty()) {
            throw new UserException("Thing type field already exists.");
        }
    }

    /**
     * @param type
     * @return
     */
    public List<ThingTypeField> getThingTypeFieldsByType(Long type) {
        HibernateQuery query = ThingTypeFieldService.getThingTypeFieldDAO().getQuery();
        return query.where(QThingTypeField.thingTypeField.dataType.id.eq(type))
                .list(QThingTypeField.thingTypeField);
    }

    /**
     * @param thingTypeId
     * @return A list of ThingTypes that has a dataTypeThingTypeId equals to thingTypeId
     */
    public List<ThingType> getThingTypesByDataTypeThingType(Long thingTypeId) {
        HibernateQuery query = new HibernateQuery(HibernateSessionFactory.getInstance().getCurrentSession());
        return query.from(QThingTypeField.thingTypeField)
                .innerJoin(QThingTypeField.thingTypeField.thingType, QThingType.thingType)
                .where(QThingTypeField.thingTypeField.dataTypeThingTypeId.eq(thingTypeId))
                .setCacheable(true)
                .list(QThingType.thingType);
    }

    /**
     * @param parentUdfIds list of ids of parent udf to exclude in thing type field list names
     * @return map of thing type field list names grouped by thing type code
     */
    public Map<String, List<String>> getNativeThingTypeUDFNameGroupByCode(List<Long> parentUdfIds) {
        HibernateQuery query = new HibernateQuery(HibernateSessionFactory.getInstance().getCurrentSession());
        Map<String, List<String>> result = new HashMap<>();
        List<Tuple> list = query.from(QThingTypeField.thingTypeField)
                .innerJoin(QThingTypeField.thingTypeField.thingType, QThingType.thingType)
                .where(QThingTypeField.thingTypeField.dataTypeThingTypeId.isNotNull()
                        .and(QThingType.thingType.isParent.isFalse()))
                .setCacheable(true)
                .list(QThingType.thingType.thingTypeCode, QThingTypeField.thingTypeField.name,
                        QThingTypeField.thingTypeField.dataTypeThingTypeId);
        list.stream().forEach(row -> {
            String ttCode = row.get(0, String.class);
            String fieldName = row.get(1, String.class);
            Long dataTypeThingTypeId = row.get(2, Long.class);
            List<String> fieldNames = result.get(ttCode);
            if (fieldNames == null) {
                fieldNames = new LinkedList<>();
            }
            if (!parentUdfIds.contains(dataTypeThingTypeId)) {
                fieldNames.add(fieldName);
            }
            result.put(ttCode, fieldNames);
        });
        return result;
    }

    /**
     * @param parent if the list must consider parent UDF
     * @return list of thing type fields that depending of parent variable
     */
    public List<ThingTypeField> getThingTypeFieldThatAreUDF(boolean parent) {
        HibernateQuery query = new HibernateQuery(HibernateSessionFactory.getInstance().getCurrentSession());
        return query.from(QThingTypeField.thingTypeField, QThingType.thingType)
                .where(QThingTypeField.thingTypeField.dataTypeThingTypeId.eq(QThingType.thingType.id)
                        .and(QThingType.thingType.isParent.eq(parent)))
                .setCacheable(true)
                .list(QThingTypeField.thingTypeField);
    }

    /**
     * @param thingTypeId
     * @return list of Thing Type Fields of type SEQUENCE from a given thingTypeId
     */
    public List<ThingTypeField> getSequenceThingTypeFields(Long thingTypeId) {
        HibernateQuery query = new HibernateQuery(HibernateSessionFactory.getInstance().getCurrentSession());
        return query.from(QThingTypeField.thingTypeField)
                .innerJoin(QThingTypeField.thingTypeField.dataType, QDataType.dataType)
                .where(QDataType.dataType.id.eq(ThingTypeField.Type.TYPE_SEQUENCE.value)
                        .and(QThingTypeField.thingTypeField.thingType.id.eq(thingTypeId)))
                .setCacheable(true)
                .list(QThingTypeField.thingTypeField);
    }

    public List<ThingTypeField> getAllThingTypeFields() {
        return getThingTypeFieldDAO().selectAll();
    }

    public List<ThingTypeField> getAllThingTypeFieldsByDataType(Long dataTypeId) {
        HibernateQuery query = new HibernateQuery(HibernateSessionFactory.getInstance().getCurrentSession());
        return query.from(QThingTypeField.thingTypeField)
                .innerJoin(QThingTypeField.thingTypeField.dataType, QDataType.dataType)
                .where(QDataType.dataType.id.eq(dataTypeId))
                .setCacheable(true)
                .list(QThingTypeField.thingTypeField);
    }

    public List<ThingTypeField> getThingTypeFieldsByThingTypeAndDataType(Long dataTypeId, Long thingTypeId) {
        HibernateQuery query = new HibernateQuery(HibernateSessionFactory.getInstance().getCurrentSession());
        return query.from(QThingTypeField.thingTypeField)
                .innerJoin(QThingTypeField.thingTypeField.dataType, QDataType.dataType)
                .where(QDataType.dataType.id.eq(dataTypeId)
                        .and(QThingTypeField.thingTypeField.thingType.id.eq(thingTypeId)))
                .setCacheable(true)
                .list(QThingTypeField.thingTypeField);
    }

    public Map<ThingType, List<ThingTypeField>> getListOfFieldsWithExpressionCache(List<ThingType> lstThingType){
        Map<ThingType, List<ThingTypeField>> result = new HashMap<>();
        lstThingType.forEach(tt -> {
            List<ThingTypeField> lstThingTypeField = DataTypeService.getInstance()
                    .getThingTypeFieldsFromCache(tt.getThingTypeCode(), ThingTypeField.Type.TYPE_FORMULA.value);
            if(lstThingTypeField != null && lstThingTypeField.size() > 0) {
                result.put(tt, lstThingTypeField);
            }
        });
        return result;
    }

    public Map<String, Object> getFormulaValuesFromCacheThingType(Map<String, Object> udf, Thing thing, ThingType thingType){
        List<ThingTypeField> thingTypeFieldsFormula = DataTypeService.getInstance()
                .getThingTypeFieldsFromCache(thingType.getThingTypeCode(), ThingTypeField.Type.TYPE_FORMULA.value);
        FormulaUtil.executeFormula(udf, thing, thingTypeFieldsFormula);
        return udf;
    }
}
