package com.tierconnect.riot.iot.services.thing.control;

import com.mongodb.DBObject;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.iot.entities.DataType;
import com.tierconnect.riot.iot.entities.Thing;
import com.tierconnect.riot.iot.entities.ThingTypeField;
import com.tierconnect.riot.iot.services.DataTypeService;
import com.tierconnect.riot.iot.services.ThingService;
import com.tierconnect.riot.iot.services.ThingTypeFieldService;
import com.tierconnect.riot.iot.services.ThingsService;
import com.tierconnect.riot.iot.services.thing.entity.CrudParameters;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Created by julio.rocha on 08-08-17.
 */
public class ThingMongoBuilder {
    private static Logger logger = Logger.getLogger(ThingMongoBuilder.class);
    private static final ThingMongoBuilder INSTANCE = new ThingMongoBuilder();

    private ThingMongoBuilder() {
    }

    public static ThingMongoBuilder getInstance() {
        return INSTANCE;
    }

    public Map<String, Object> newMongoThing(Thing thing, CrudParameters parameters) {
        Map<String, Object> udfMapMongo = getUdfMapForMongo(thing.getId(), parameters);
        Map<String, Object> thingTypeFieldResponse;
        if (udfMapMongo != null && !udfMapMongo.isEmpty()) {
            if (udfMapMongo.get("thingTypeFieldResponse") != null) {
                thingTypeFieldResponse = (Map<String, Object>) udfMapMongo.get("thingTypeFieldResponse");
                /*thingTypeFieldResponse = FormulaUtil.getFormulaValues(thingTypeFieldResponse, thing,
                        parameters.getThingType());*/
                thingTypeFieldResponse = ThingTypeFieldService.getInstance().getFormulaValuesFromCacheThingType(thingTypeFieldResponse,
                        thing, parameters.getThingType());
                udfMapMongo.put("thingTypeFieldResponse", thingTypeFieldResponse);
            }
        }
        return udfMapMongo;
    }

    public Map<String, Object> getUdfMapForMongo(Long thingId, CrudParameters parameters) {
        return getUdfMapForMongo(thingId, parameters, null);
    }

    public Map<String, Object> getUdfMapForMongo(Long thingId, CrudParameters parameters, DBObject thingMongo) {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> thingTypeFieldResponse = new HashMap<>();
        Map<String, Object> thingTypeFieldTickle = new HashMap<>();
        Map<String, Object> mapUdfs = parameters.getUdfs();//update by reference
        boolean timeSeries = false;
        try {
            if (mapUdfs != null) {
                addUDFSource(parameters);
                //Iterate Udf's Data
                for (Map.Entry<String, Object> udfObject : mapUdfs.entrySet()) {
                    Map<String, Object> udfField = new HashMap<>();
                    String udfLabel = udfObject.getKey().trim();
                    List<ThingTypeField> thingTypeFields = ThingTypeFieldService.getInstance()
                            .getThingTypeFieldByNameAndTypeCode(
                                    udfLabel,
                                    parameters.getThingType().getThingTypeCode());

                    if (!thingTypeFields.isEmpty()) {
                        ThingTypeField thingTypeField = thingTypeFields.get(0);
                        timeSeries = timeSeries || thingTypeField.getTimeSeries();
                        DataType dataType = getDataType(thingTypeField);
                        addUdfPropertiesToField(udfObject, udfField);
                        Map<String, Object> udfMap = (Map<String, Object>) udfObject.getValue();
                        Object udfValue = udfMap.get("value");
                        Map<String, Object> values;
                        if (thingMongo == null) {
                            addThingTypeFieldIdAndTimeToField(parameters.getTransactionDate(), udfField, thingTypeField);
                            values = ThingsService.getInstance().getValueMapForMongo(
                                    thingId,
                                    thingTypeField,
                                    dataType,
                                    udfValue,
                                    null,
                                    parameters.getGroup(),
                                    parameters.getTransactionDate(),
                                    parameters.getFillSource());
                        } else {
                            addThingTypeFieldIdAndTimeToField(parameters.getTransactionDate(), udfField, thingTypeField,
                                    (DBObject) thingMongo.get(udfLabel), udfValue);
                            values = ThingService.getInstance().getValueMapForMongo(
                                    thingId,
                                    thingTypeField,
                                    udfValue,
                                    null,
                                    parameters.getGroup(),
                                    parameters.getTransactionDate(),
                                    thingMongo,
                                    parameters.getFillSource());
                        }
                        udfField.put("value", values.get("responseMap"));
                        thingTypeFieldTickle.put(thingTypeField.getName(), values.get("responseObject"));
                        thingTypeFieldResponse.put(udfLabel, udfField);
                    }
                }
                response.put("thingTypeFieldResponse", thingTypeFieldResponse);
                response.put("thingTypeFieldTickle", thingTypeFieldTickle);
                response.put("isTimeSeries", timeSeries);
            }
        } catch (Exception e) {
            logger.error("Error when build the udf's Map for Mongo.", e);
            throw new UserException("Error when build the udf's Map for Mongo." + e.getMessage(), e);
        }
        return response;
    }

    private void addUDFSource(CrudParameters parameters) {
        Map<String, Object> udfs = parameters.getUdfs();//update by reference
        if (parameters.getFillSource()) {
            List<ThingTypeField> thingTypeFields = ThingTypeFieldService.getInstance()
                    .getThingTypeFieldByNameAndTypeCode(Constants.UDF_SOURCE,
                            parameters.getThingType().getThingTypeCode());
            if (thingTypeFields != null && !thingTypeFields.isEmpty()
                    && doesNotHaveUdfSource(udfs)) {
                LinkedHashMap<Object, Object> valueSourceDefault = new LinkedHashMap<>();
                valueSourceDefault.put("value", Constants.SOURCE_SERVICE);
                udfs.put(Constants.UDF_SOURCE, valueSourceDefault);
            }
        }
    }

    private DataType getDataType(ThingTypeField thingTypeField) {
        DataType dataType = null;
        if (thingTypeField.getDataType().getTypeParent()
                .equals(ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value)) {
            dataType = DataTypeService.getInstance().get(thingTypeField.getDataType().getId());
        }
        return dataType;
    }

    private void addUdfPropertiesToField(Map.Entry<String, Object> udfObject, Map<String, Object> udfField) {
        for (Map.Entry<String, Object> udfProperties : ((Map<String, Object>) udfObject.getValue())
                .entrySet()) {
            if (!udfProperties.getKey().equals("value")) {
                udfField.put(udfProperties.getKey(), udfProperties.getValue());
            }
        }
    }

    private void addThingTypeFieldIdAndTimeToField(Date transactionDate, Map<String, Object> udfField,
                                                   ThingTypeField thingTypeField, DBObject thingUdfLabel,
                                                   Object udfValue) {
        addThingTypeFieldIdAndTimeToField(transactionDate, udfField, thingTypeField);
        if (thingUdfLabel != null) {
            Object prevVal = thingUdfLabel.get("value");
            if (prevVal instanceof DBObject) {
                if (thingTypeField.isNativeObject()) {
                    prevVal = ((DBObject) prevVal).get("code");
                } else if (thingTypeField.isThingTypeUDF()) {
                    prevVal = ((DBObject) prevVal).get("id");
                }
            }
            if (udfValue != null && udfValue.equals(prevVal) || udfValue == null && prevVal == null
                    || prevVal instanceof Boolean && String.valueOf(prevVal).equals(udfValue)) {
                udfField.put("time", thingUdfLabel.get("time"));
            } else {
                udfField.put("time", transactionDate);
            }
        }
    }

    private Object getValue(DBObject map, String key) {
        return map.get(key);
    }

    private void addThingTypeFieldIdAndTimeToField(Date transactionDate, Map<String, Object> udfField,
                                                   ThingTypeField thingTypeField) {
        if (!udfField.containsKey("thingTypeFieldId")) {
            udfField.put("thingTypeFieldId", thingTypeField.getId());
        }
        try {
            Object timeUDF = udfField.get("time");
            if (timeUDF != null && !(timeUDF instanceof Date)) {
                udfField.put("time", new Date(Long.parseLong(timeUDF.toString())));
            }
        } catch (Exception e) {
            udfField.put("time", transactionDate);
        }
    }

    private boolean doesNotHaveUdfSource(Map<String, Object> udfs) {
        return udfs.get(Constants.UDF_SOURCE) == null || ((Map) udfs.get(Constants.UDF_SOURCE)).isEmpty();
    }
}
