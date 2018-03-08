package com.tierconnect.riot.iot.services.thing.control;

import com.tierconnect.riot.iot.dao.SequenceDAO;
import com.tierconnect.riot.iot.dao.util.FormulaUtil;
import com.tierconnect.riot.iot.entities.Thing;
import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.entities.ThingTypeField;
import com.tierconnect.riot.iot.entities.ValidationBean;
import com.tierconnect.riot.iot.services.AppLocationAdapter;
import com.tierconnect.riot.iot.services.ThingTypeFieldService;
import com.tierconnect.riot.iot.services.thing.entity.CrudParameters;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Created by julio.rocha on 08-08-17.
 */
public class ThingBuilder {
    private static Logger logger = Logger.getLogger(ThingBuilder.class);
    private static final ThingBuilder INSTANCE = new ThingBuilder();

    private ThingBuilder() {
    }

    public static ThingBuilder getInstance() {
        return INSTANCE;
    }


    public Thing newThing(CrudParameters parameters) {
        Thing thing = new Thing();
        thing.setThingType(parameters.getThingType());
        thing.setGroup(parameters.getGroup());
        thing.setName(parameters.getName());
        thing.setCreatedByUser(parameters.getCurrentUser());
        thing.setModifiedTime(parameters.getTransactionDate().getTime());
        // set udf values
        setUdfValues(parameters);
        // set zones
        AppLocationAdapter locationAdapter = new AppLocationAdapter();
        locationAdapter.processFields(parameters.getUdfs(), parameters.getTransactionDate(),
                parameters.getGroupFacilityMap().getId(), parameters.getThingTypeCode(), true);
        // set sequence values
        setSequenceValues(parameters);
        // set parent
        setThingParent(parameters, thing);
        //set serial number
        setThingSerialNumber(parameters, thing);
        return thing;
    }

    private void setUdfValues(CrudParameters parameters) {
        if (fillUdfs(parameters)) {
            Map<String, Object> udfs = parameters.getUdfs();//fill by reference
            Boolean useDefaultValues = parameters.getUseDefaultValues();
            Map<String, Object> udfValues = new HashMap<>();
            ThingType thingType = parameters.getThingType();
            if (thingType.getThingTypeFields() == null) {
                thingType.setThingTypeFields(new HashSet<>());
                List<ThingTypeField> thingTypeFields = ThingTypeFieldService.getInstance().getByThingTypeCode(thingType.getThingTypeCode());
                thingTypeFields.stream().forEach(ttf -> thingType.getThingTypeFields().add(ttf));
            }
            thingType.getThingTypeFields().stream().forEach(ttf -> {
                Object udfValue = udfs.get(ttf.getName());
                if (udfs != null && !udfs.isEmpty() && udfValue != null) {
                    udfValues.put(ttf.getName(), udfValue);
                } else if (useDefaultValues) {
                    setDefaultUdfValue(parameters, udfValues, ttf);
                }
            });
            udfs.putAll(udfValues);
        }
    }

    private void setSequenceValues(CrudParameters parameters) {
        try {
            Map<String, Object> udfs = parameters.getUdfs();//fill by reference
            if (udfs != null) {
                Map<String, Object> udfsWithSequenceValues = new HashMap<>();
                List<ThingTypeField> sequenceThingTypeFields = ThingTypeFieldService.getInstance()
                        .getSequenceThingTypeFields(parameters.getThingType().getId());
                sequenceThingTypeFields.stream().forEach(ttf -> {
                    Map<String, Object> value = new HashMap<>();
                    value.put("value", SequenceDAO.getInstance().incrementAndGetSequence(ttf.getId()));
                    value.put("time", parameters.getTransactionDate().getTime());
                    udfsWithSequenceValues.put(ttf.getName(), value);
                });
                udfs.putAll(udfsWithSequenceValues);
            }
        } catch (Exception e) {
            throw new UserException("Error building sequence udf's Map.", e);
        }
    }

    private void setThingParent(CrudParameters parameters, Thing thing) {
        Map<String, Object> parent = parameters.getParentObj();
        if (parent != null && !parent.isEmpty()) {
            Object parentThing = parent.get("thing");
            if (parentThing != null && parentThing instanceof Thing) {
                thing.setParent((Thing) parentThing);
            }
        }
    }

    private void setThingSerialNumber(CrudParameters parameters, Thing thing) {
        ThingType thingType = parameters.getThingType();
        String serialFormula = thingType.getSerialFormula();
        String serialNumber = parameters.getSerialNumber();
        if (StringUtils.isNotEmpty(serialFormula)) {
            Object evaluation = FormulaUtil.getFormulaValues(parameters.getUdfs(), thing, thingType.getSerialFormula());
            if (evaluation != null && StringUtils.isNotEmpty(evaluation.toString())) {
                serialNumber = evaluation.toString();
                ValidationBean validationBean = InsertParameterExtractor.getInstance()
                        .validateSerialFormat(thingType, serialNumber, true);
                if (validationBean.isError()) {
                    throw new UserException(validationBean.getErrorDescription() + " - " + serialNumber);
                } else {
                    thing.setSerial(serialNumber);
                }
            } else {
                throw new UserException("Serial number cannot be evaluated");
            }
        } else {
            thing.setSerial(serialNumber);
        }
    }

    private boolean fillUdfs(CrudParameters parameters) {
        if (parameters.getUseDefaultValues() && parameters.getUdfs() == null) {
            parameters.initializeUDFIfNull();
        }
        return parameters.getUdfs() != null;
    }

    private void setDefaultUdfValue(CrudParameters parameters, Map<String, Object> udfValues, ThingTypeField ttf) {
        Map<String, Object> udfDefaultValue = getUdfDefaultValue(ttf, parameters.getTransactionDate());
        udfValues.putAll(udfDefaultValue);
    }

    private Map<String, Object> getUdfDefaultValue(ThingTypeField thingTypeField, Date transactionDate) {
        try {
            Map<String, Object> defaultValueMap = new HashMap<>();
            String defaultValue = thingTypeField.getDefaultValue();
            if (StringUtils.isNotEmpty(defaultValue)) {
                Map<String, Object> value = new HashMap<>();
                value.put("value", defaultValue);
                value.put("time", transactionDate.getTime());
                defaultValueMap.put(thingTypeField.getName(), value);
            }
            return defaultValueMap;
        } catch (Exception e) {
            logger.error("Error building default values udf's Map." + e.getMessage(), e);
            throw new UserException("Error building default values udf's Map.", e);
        }
    }

    public void removeUdfsSequence(CrudParameters parameters) {
        try {
            removeUdfByDataType(parameters, ThingTypeField.Type.TYPE_SEQUENCE.value);
        } catch (Exception e) {
            logger.error("Error occurred while pre-processing sequences in thing '"
                    + parameters.getSerialNumber() + "'", e);
            throw new UserException("Error occurred while pre-processing sequences in thing '"
                    + parameters.getSerialNumber() + "'", e);
        }
    }

    private void removeUdfByDataType(CrudParameters parameters, Long idDataType) {
        if (parameters.getThingType() != null) {
            List<ThingTypeField> udfList = parameters.getThingType().getThingTypeFieldsByType(idDataType);
            for (ThingTypeField typeField : udfList) {
                parameters.getUdfs().remove(typeField.getName());
            }
        }
    }
}
