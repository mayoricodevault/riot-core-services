package com.tierconnect.riot.iot.dao.util;

import com.mongodb.DBObject;
import com.tierconnect.riot.iot.dao.mongo.ThingMongoDAO;
import com.tierconnect.riot.iot.entities.Thing;
import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.entities.ThingTypeField;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by fflores on 8/25/2015.
 */
public class FormulaUtil {

    private static Logger logger = Logger.getLogger( FormulaUtil.class );

    public static Object validateFormula (String formula,Map<String, Object> thingTypeFields,boolean validateProperties){
        Object result = null;
        if ( !thingTypeFields.isEmpty() ){
            try {
            ElExpressionService ees = new ElExpressionService();
            ees.initialize(getMapValues(thingTypeFields),null);
            result = ees.evaluate(formula,validateProperties);
            } catch (javax.el.PropertyNotFoundException er){
                // when UDFs used in formula don't exist in thingTypeFields, because they don't have values
                if (validateProperties) {
                    if (isNonUDF(er.getMessage()))
                        return null;
                    logger.error(er, er);
                    throw new UserException(er.getMessage());
                }
                else
                    return null;
            }
        }
        return result;
    }

    private static boolean isNonUDF(String property) {
        property = getPropertyFromMessage(property);
        for (ThingType.NonUDF nonUDF: ThingType.NonUDF.values()) {
            if (nonUDF.toString().equals(property)) {
                logger.debug(property + " is NonUDF property");
                return true;
            }
        }
        return false;
    }

    private static String getPropertyFromMessage(String property) {
        property = property.replaceAll("Cannot find property ","");
        if (property.startsWith("Property")) {
            Integer pos = property.indexOf('\'');
            property = property.substring(pos+1, property.length());
            pos = property.indexOf('\'');
            property = property.substring(0, pos);
        }
        return property;
    }

    public static Map<String,Object> getMapValues(Map<String, Object> thingTypeFields){
        Map<String,Object> result = new HashMap<>();
        for (Map.Entry<String, Object> current : thingTypeFields.entrySet()) {
            String tfkey = current.getKey();
            Object udfValue = null;
            if ((current.getValue() != null) && (current.getValue() instanceof Map)) {
                HashMap<String, Object> tfvalue = (HashMap<String, Object>) current.getValue();
                if (null != tfvalue) {
                    if (tfvalue.containsKey("value")) {
                        udfValue = tfvalue.get("value");
                    } else {
                        udfValue = tfvalue;
                    }
                }
            } else {
                udfValue = current.getValue();
            }
            result.put(tfkey, udfValue);
        }
        return result;
    }

    public static Map<String, Object> getFormulaValues(Map<String, Object> udf, Thing thing, ThingType thingType){
        List<ThingTypeField> thingTypeFieldsFormula = thingType.getThingTypeFieldsByType(Long.valueOf(ThingTypeField.Type.TYPE_FORMULA.value));
        executeFormula(udf, thing, thingTypeFieldsFormula);
        return udf;
    }

    public static void executeFormula(Map<String, Object> udf, Thing thing, List<ThingTypeField> thingTypeFieldsFormula) {
        if (thingTypeFieldsFormula != null && !thingTypeFieldsFormula.isEmpty()) {
            ElExpressionService ees = new ElExpressionService();
            ees.initialize(getMapValues(udf),thing);
            for (ThingTypeField thingTypeField : thingTypeFieldsFormula) {
                String formula = thingTypeField.getDefaultValue();
                if(udf.containsKey(thingTypeField.getName())){
                    ((Map)udf.get(thingTypeField.getName())).put("value",ees.evaluate(formula,false));
                }
            }
        }
    }

    public static Object getFormulaValues(Map<String, Object> udf, Thing thing, String formula){
        Object result;
        ElExpressionService ees = new ElExpressionService();
        if (udf != null){
            udf = getMapValues(udf);
        }
        ees.initialize(udf,thing);
        result = ees.evaluate(formula,false);
        return result;
    }

    public static Map<String,Object> addFormulaValues (Map<String,Object> udfs,Thing thing,Date storageDate) {
        try {
            if (thing == null)
                return udfs;
            ThingType thingType = thing.getThingType();
            List<ThingTypeField> thingTypeFieldsFormula = thingType.getThingTypeFieldsByType(ThingTypeField.Type.TYPE_FORMULA.value);
            if (null != thingTypeFieldsFormula && !thingTypeFieldsFormula.isEmpty()) {
                // determine if dwelltime and timestamp will be added to the expression context
                boolean addDwelltimeTimestamp = false;
                for (ThingTypeField thingTypeField : thingTypeFieldsFormula){
                    if (thingTypeField.getDefaultValue().contains("dwelltime") || thingTypeField.getDefaultValue().contains("timestamp")){
                        addDwelltimeTimestamp = true;
                    }
                }
                // get thing udfValues
                String whereThing = "_id=" + thing.getId();
                List<String> filterFields = new ArrayList<>();
                filterFields.add("*");
                Map<String, Object> udfResult = ThingMongoDAO.getInstance().getThingUdfValues(whereThing, null, filterFields, null);
                DBObject thingAux = ThingMongoDAO.getInstance().getThing(thing.getId());
                Map<String, Object> udfValues = new HashMap<>();
                if (null != udfResult && !udfResult.isEmpty()) {
                    List<Map<String, Object>> udfValuesList = (List<Map<String, Object>>) udfResult.get("results");
                    // convert udfValues to Map
                    if (null != udfValuesList && !udfValuesList.isEmpty()) {
                        for (Map<String, Object> udfValuesAsMap : udfValuesList) {
                            for (Map.Entry<String, Object> entry : udfValuesAsMap.entrySet()) {
                                Map<String, Object> newEntry = new HashMap<>();
                                newEntry.put("value", entry.getValue());
                                ThingTypeField thingTypeField = thing.getThingTypeField(entry.getKey());
                                if (thingAux != null && thingTypeField!=null) {
                                    Date dateTime = (Date)((DBObject)thingAux.get(entry.getKey())).get("time");
                                    newEntry.put("time", dateTime);
                                    newEntry.put("thingTypeFieldId", thingTypeField.getId());
                                }
                                udfValues.put(entry.getKey(), newEntry);
                            }
                        }
                    }
                }
                List<ThingTypeField> formulas = new ArrayList<>();
                for (ThingTypeField thingTypeField : thingTypeFieldsFormula){
                    if (!thingTypeField.getDefaultValue().contains("count")){
                        formulas.add(thingTypeField);
                    }
                }
                if (udfs != null) {
                    udfValues.putAll(udfs);
                }
                // add dwelltime and timestamp for zones when is timeseries
                if(addDwelltimeTimestamp && thingAux != null){
                    addDwelltimeProperties(udfValues, thing, thingAux);
                }
                // exclude nonUDFs from the map because they are in thing object
                for (ThingType.NonUDF nonUDF: ThingType.NonUDF.values()) {
                    udfValues.remove(nonUDF.toString());
                }
                if (null != formulas && !formulas.isEmpty()) {
                    for (ThingTypeField thingTypeField : formulas) {
                        String formula = thingTypeField.getDefaultValue();
                        Object formulaValue = FormulaUtil.getFormulaValues(udfValues,thing,formula);
                        if (null != formulaValue) {
                            Map<String, Object> props = new HashMap<>();
                            props.put("thingTypeFieldId",thingTypeField.getId());
                            props.put("time",storageDate);
                            props.put("value",formulaValue);
                            udfValues.put(thingTypeField.getName(),props);
                        } else {
                            // remove from the map if value is null
                            udfValues.remove(thingTypeField.getName());
                        }
                    }
                }
                // add values for formula UDFs in the map udfs
                if (null != thingType.getThingTypeFields() && !thingType.getThingTypeFields().isEmpty()) {
                    for (ThingTypeField thingTypeField : thingType.getThingTypeFields()) {
                        if (thingTypeField.getTypeParent().equals(ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value) &&
                                thingTypeField.getDataType().getId().compareTo(ThingTypeField.Type.TYPE_FORMULA.value) == 0) {
                            Map<String, Object> formulaEntry = (Map<String, Object>) udfValues.get(thingTypeField.getName());
                            if (null != formulaEntry && !formulaEntry.isEmpty()) {
                                if (udfs == null)
                                    udfs = new HashMap<>();
                                (udfs).put(thingTypeField.getName(), formulaEntry);
                            }
                        }
                    }
                }
            }
        } catch (UserException e){
            throw new UserException(e.getMessage(), e);
        } catch (Exception e){
            throw new UserException("There is an error evaluating formula expression", e);
        }
        return udfs;
    }

    public static void addDwelltimeProperties(Map<String, Object> udfValuesAsMap, Thing thing, DBObject thingAux){
        for (Map.Entry<String, Object> entry : udfValuesAsMap.entrySet()) {
            ThingTypeField thingTypeField = thing.getThingTypeField(entry.getKey());
            if (thingTypeField != null && thingTypeField.getTimeSeries()) {
                if(thingTypeField.isNativeObject()){
                    addDwelltimeToObject(entry, thing, thingAux, thingTypeField);
                }else{
                    addDwelltimeToStandard(entry, thing, thingAux, thingTypeField);
                }
            }
        }
    }

    public static void addDwelltimeToStandard (Map.Entry<String, Object> entry, Thing thing, DBObject thingAux,
                                               ThingTypeField thingTypeField){
        Object val0 = thing.getThingFields().get(entry.getKey()).getValue();
        Object val1 = ((Map) entry.getValue()).get("value");

        Date dateTime = (Date) ((DBObject) thingAux.get(entry.getKey())).get("time");
        Long dwelltime = val1.equals(val0)? (modifiedDate(thing).getTime() - dateTime.getTime()): 0;
        ((Map)((Map) entry.getValue())).put("dwelltime", dwelltime);
        ((Map)((Map) entry.getValue())).put("timestamp", dateTime);

        Map<String, Object> newEntry = new HashMap<>();
        newEntry.put("time", dateTime);
        newEntry.put("thingTypeFieldId", thingTypeField.getId());
        newEntry.put("value", ((Map)entry.getValue()).get("value"));
    }

    public static void addDwelltimeToObject (Map.Entry<String, Object> entry, Thing thing, DBObject thingAux,
                                             ThingTypeField thingTypeField){
        String code0 = (String) ((Map) thing.getThingFields().get(entry.getKey()).getValue()).get("code");
        String code1 = (String) ((Map)((Map)((Map) entry.getValue())).get("value")).get("code");

        Date dateTime = (Date) ((DBObject) thingAux.get(entry.getKey())).get("time");
        Long dwelltime = code1.equals(code0)? (modifiedDate(thing).getTime() - dateTime.getTime()): 0;
        ((Map)((Map)((Map) entry.getValue())).get("value")).put("dwelltime", dwelltime);
        ((Map)((Map)((Map) entry.getValue())).get("value")).put("timestamp", dateTime);

        Map<String, Object> newEntry = new HashMap<>();
        newEntry.put("time", dateTime);
        newEntry.put("thingTypeFieldId", thingTypeField.getId());
        newEntry.put("value", ((Map)entry.getValue()).get("value"));
    }

    public static Date modifiedDate(Thing thing){
        return new Date(thing.getModifiedTime().equals(null)? System.currentTimeMillis():
                thing.getModifiedTime());
    }

    /**
     * This method gets a list of thingTypeFields from a list of thing types
     * @param lstThingType list of thing Types
     * @return
     */
    public static Map<ThingType, List<ThingTypeField>> getListOfFieldsWithExpression(List<ThingType> lstThingType)
    {
        Map<ThingType, List<ThingTypeField>> result = new HashMap<>();
        List<ThingTypeField> lstThingTypeField = new ArrayList<>();
        if(lstThingType!=null && lstThingType.size()>0)
        {
            for(ThingType thingType: lstThingType)
            {
                if(thingType.getThingTypeFields()!=null && thingType.getThingTypeFields().size()>0)
                {
                    lstThingTypeField = new ArrayList<>();
                    for(ThingTypeField thingTypeField: thingType.getThingTypeFields())
                    {
                        if(thingTypeField.getDataType().getId().compareTo(ThingTypeField.Type.TYPE_FORMULA.value)==0)
                        {
                            lstThingTypeField.add(thingTypeField);
                        }
                    }
                    if(lstThingTypeField!=null && lstThingTypeField.size()>0)
                    {
                        result.put(thingType, lstThingTypeField);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Validate if a field is in a formula.
     *
     * @param formula a {@link String} that contained formula to evaluate.
     * @param value   a value to verify.
     * @return a boolean.
     */
    public static boolean isPartOfFormula(String formula, String value) {
        String[] regExpArray = {
                "^(?:.*(\\$\\{){1}(\\s)*" + value + "(\\s)*(\\}){1}.*)$",
                "^(?:.*(\\(){1}(\\s)*" + value + "(\\s)*(\\)){1}.*)$",
                "^(?:.*(\\(){1}(\\s)*" + value + "(\\s)*(\\,){1}.*)$",
                "^(?:.*(\\,){1}(\\s)*" + value + "(\\s)*(\\,){1}.*)$",
                "^(?:.*(\\,){1}(\\s)*" + value + "(\\s)*(\\)){1}.*)$"
        };
        for (String regExp : regExpArray) {
            Pattern pat = Pattern.compile(regExp);
            Matcher mat = pat.matcher(formula);
            if (mat.matches()) {
                return true;
            }
        }
        return false;
    }
}
