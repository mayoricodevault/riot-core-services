package com.tierconnect.riot.iot.services;

import com.tierconnect.riot.iot.dao.mongo.ThingMongoDAO;
import com.tierconnect.riot.iot.dao.util.FormulaUtil;
import com.tierconnect.riot.iot.entities.ReportEntryOption;
import com.tierconnect.riot.iot.entities.ReportEntryOptionProperty;
import com.tierconnect.riot.iot.entities.Thing;
import com.tierconnect.riot.iot.utils.RFIDPrinterUtil;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by fflores on 1/9/17.
 */
public class RFIDPrinterService {
    private static Logger logger = Logger.getLogger(RFIDPrinterService.class);
    static private RFIDPrinterService INSTANCE = new RFIDPrinterService();
    public static RFIDPrinterService getInstance()
    {
        return INSTANCE;
    }

    public static final int TAG_LENGTH = 24;
    public static final String RFID_ENCODE = "rfidEncode";

    public Map encodeSerial (List<Map<String, Object>> inList, Boolean offLine){
        Map<String, Object> mapResponse = new HashMap<>();
        List<String> printedTags = new ArrayList<>();
        List<String> zplPrintedTags = new ArrayList<>();
        List<Map<String, String>> notPrintedTags = new ArrayList<>();
        String tagId = null;

        if (null == inList || inList.isEmpty()) {
            throw new UserException ("Invalid input parameters");
        }

        for (Map<String, Object> inMap : inList){
            try{
                Long parentThingId = null;
                if (inMap.get("parentThingId") != null){
                    if ( inMap.get("parentThingId") instanceof Long ) {
                        parentThingId = (Long) inMap.get("parentThingId");
                    } else {
                        parentThingId = Long.valueOf((Integer) inMap.get("parentThingId"));
                    }
                }
                Long childThingId = null;
                if (inMap.get("childThingId") != null){
                    if ( inMap.get("childThingId") instanceof Long ) {
                        childThingId = (Long) inMap.get("childThingId");
                    } else {
                        childThingId = Long.valueOf((Integer) inMap.get("childThingId"));
                    }
                }
                String thingTypeCodeRFIDPrinter = inMap.get("thingTypeCodeRFIDPrinter") != null ? (String) inMap.get(
                        "thingTypeCodeRFIDPrinter") : null;
                String thingRFIDPrinter = inMap.get("thingRFIDPrinter") != null ? (String) inMap.get("thingRFIDPrinter") : null;
                String thingTypeCodeZPL = inMap.get("thingTypeCodeZPL") != null ? (String) inMap.get("thingTypeCodeZPL") : null;
                String thingZPL = inMap.get("thingZPL") != null ? (String) inMap.get("thingZPL") : null;
                Integer numCopies = inMap.get("numCopies") != null ? (Integer) inMap.get("numCopies") : null;
                // input parameter reportEntryOptionId is optional
                Long reportEntryOptionId = null;
                if (inMap.get("reportEntryOptionId") != null){
                    if ( inMap.get("reportEntryOptionId") instanceof Long ) {
                        reportEntryOptionId = (Long) inMap.get("reportEntryOptionId");
                    } else if ( inMap.get("reportEntryOptionId") instanceof Integer ) {
                        reportEntryOptionId = Long.valueOf((Integer) inMap.get("reportEntryOptionId"));
                    }
                }

                // input parameters are optional: thingTypeCode, keyFieldName, keyFieldValue
                String thingTypeCode = inMap.get("thingTypeCode") != null ? (String) inMap.get("thingTypeCode") : null;
                String keyFieldName  = inMap.get("keyFieldName") != null ? (String) inMap.get("keyFieldName") : null;
                String keyFieldValue = inMap.get("keyFieldValue") != null ? (String) inMap.get("keyFieldValue") : null;

                // input parameters validation
                if ( null == thingTypeCodeRFIDPrinter
                        || thingTypeCodeRFIDPrinter.isEmpty()
                        || null == thingRFIDPrinter
                        || thingRFIDPrinter.isEmpty()
                        || null == thingTypeCodeZPL
                        || thingTypeCodeZPL.isEmpty()
                        || null == thingZPL || thingZPL.isEmpty() ){
                    throw new UserException ("Invalid input parameters");
                }

                // determine selectedThing
                Map<String, Object> selectedThings = new HashMap<>();
                if (parentThingId == null && childThingId == null){
                    if (null == thingTypeCode || null == keyFieldName || null == keyFieldValue){
                        throw new Exception("Invalid input parameters");
                    }
                    String whereThing = "thingTypeCode=" + thingTypeCode;
                    String whereFieldValue = keyFieldName + ".value='" + keyFieldValue + "'";
                    List<String> filterFields = new ArrayList<>();
                    filterFields.add("*");
                    Map<String, Object> udfResult = ThingMongoDAO.getInstance().getThingUdfValues(whereThing,
                            whereFieldValue,
                            filterFields,
                            null);
                    if (null != udfResult && !udfResult.isEmpty()){
                        List<Map<String, Object>> udfValuesList = (List<Map<String, Object>>) udfResult.get("results");
                        if (null != udfValuesList && !udfValuesList.isEmpty()){
                            if (udfValuesList.size() > 1){
                                throw new Exception("There is more than one thing with " + keyFieldName + "=" + keyFieldValue);
                            } else {
                                Long id = (Long) ((Map) udfValuesList.get(0)).get("_id");
                                Thing thing = ThingService.getInstance().get(id);
                                if (null == thing) {
                                    throw new Exception("Invalid input parameters");
                                }
                                selectedThings.put("thing", thing);
                                tagId = thing.getSerial();
                            }
                        }
                    }
                }
                else if ( parentThingId == null && childThingId != null ){
                    Thing childThing = ThingService.getInstance().get(childThingId);
                    if (null == childThing) {
                        throw new Exception("Invalid input parameters");
                    }
                    selectedThings.put("child",childThing);
                    tagId = childThing.getSerial();
                } else if ( parentThingId != null && childThingId == null ){
                    Thing parentThing = ThingService.getInstance().get(parentThingId);
                    if (null == parentThing) {
                        throw new Exception("Invalid input parameters");
                    }
                    selectedThings.put("thing",parentThing);
                    tagId = parentThing.getSerial();
                }else if ( parentThingId != null && childThingId != null ){
                    Thing childThing = ThingService.getInstance().get(childThingId);
                    if (null == childThing) {
                        throw new Exception("Invalid input parameters");
                    }
                    selectedThings.put("child",childThing);
                    tagId = childThing.getSerial();
                    Thing parentThing = ThingService.getInstance().get(parentThingId);
                    if (null == parentThing) {
                        throw new Exception("Invalid input parameters");
                    }
                    selectedThings.put("parent",parentThing);
                    // TODO validate relation between parent and child
                }

                // get udf and property values for selectedThings
                Map<String, Object> udfValues = new HashMap<>();
                for (Map.Entry<String, Object> selectedThing : selectedThings.entrySet()){
                    Thing thing = (Thing) selectedThing.getValue();
                    Map<String, Object> udfResult = ThingService.getInstance().getThingFullFields(thing.getId(),thing.getThingType().getId());
                    if (null != udfResult && !udfResult.isEmpty()){
                        List<Map<String, Object>> udfValuesList = (List<Map<String, Object>>) udfResult.get("results");
                        // convert udfValues to Map
                        if (null != udfValuesList && !udfValuesList.isEmpty()){
                            for (Map<String, Object> udfValuesAsMap : udfValuesList){
                                for (Map.Entry<String, Object> entry : udfValuesAsMap.entrySet()){
                                    Map<String, Object> newEntry = new HashMap<>();
                                    newEntry.put("value", entry.getValue() != null ? entry.getValue().toString() : null);
                                    udfValues.put(entry.getKey(), newEntry);
                                }
                            }
                        }
                    }
                }

                // put in the map the entry for number of copies
                String numberCopies = null == numCopies ? "1" : String.valueOf(numCopies);
                Map<String,String> numcopiesMap = new HashMap<>();
                numcopiesMap.put("value", numberCopies);
                udfValues.put("numCopies", numcopiesMap);

                // input parameter rfidEncode is optional, by default is true
                Boolean rfidEncode = Boolean.TRUE;

                // get zpl instruction
                String zplString = "zpl";
                String serialNumber = "serialNumber";
                String zpl = null;
                String whereThingZpl = "thingTypeCode=" + thingTypeCodeZPL;
                List<String> filterFieldsZpl = new ArrayList<>();
                filterFieldsZpl.add("*");
                Map<String, Object> thingUdfVal = ThingMongoDAO.getInstance().getThingUdfValues(whereThingZpl, null, filterFieldsZpl, null);
                if (null != thingUdfVal && !thingUdfVal.isEmpty()){
                    List<Map<String,Object>> udfValueList = (ArrayList) thingUdfVal.get("results");
                    if (null != udfValueList && !udfValueList.isEmpty()){
                        for (Map<String,Object> udfValuesMap : udfValueList){
                            if (null != udfValuesMap.get(serialNumber) && udfValuesMap.get(serialNumber).equals(thingZPL)){
                                zpl = (String) udfValuesMap.get(zplString);
                            }
                            // determining rfidEncode
                            if (null != udfValuesMap.get(RFID_ENCODE)
                                    && udfValuesMap.get(RFID_ENCODE) instanceof Boolean
                                    && udfValuesMap.get(RFID_ENCODE).equals(false)){
                                rfidEncode = Boolean.FALSE;
                            }
                        }
                    }
                }
                if (null == zpl || zpl.isEmpty())
                    throw new Exception("Cannot determine zpl instruction");
                String evaluatedZpl = null;
                // replace udf and property values in zpl instruction
                for (Map.Entry<String, Object> selectedThing : selectedThings.entrySet()){
                    Thing thing = (Thing) selectedThing.getValue();
                    if (udfValues.containsKey("parent")){
                        udfValues.remove("parent"); // because parent is set in thing
                    }
                    Object replacedString = FormulaUtil.getFormulaValues(udfValues,thing,zpl);
                    if (replacedString != null){
                        evaluatedZpl = replacedString.toString();
                    }
                }
                if (null == evaluatedZpl){
                    throw new Exception("ZPL cannot be evaluated");
                }

                // get the label name for the serial
                String serialLabel = "serial";
                if (null != reportEntryOptionId){
                    ReportEntryOption reportEntryOption = ReportEntryOptionService.getInstance().get(reportEntryOptionId);
                    if ( null != reportEntryOption){
                        List<ReportEntryOptionProperty> reoList = reportEntryOption.getReportEntryOptionProperties();
                        if (null != reoList && !reoList.isEmpty()){
                            for (ReportEntryOptionProperty reop : reoList){
                                if ( "serial".equals(reop.getPropertyName()) ) {
                                    serialLabel = reop.getLabel();
                                    break;
                                }
                            }
                        }
                    }
                }

                // validate tagId
                if (rfidEncode.compareTo(Boolean.TRUE) == 0){
                    tagId = validateRegex(evaluatedZpl, "(.*)RFW,H.FD(.*).FN3.FS.FN3.RFR,H(.*)");
                    if (null != tagId){
                        // validate tagId length
                        if (tagId.length() != TAG_LENGTH)
                            throw new Exception(serialLabel + " field must be 24 characters of length.");
                        // validate tagId digits
                        String resValidateDigits = validateRegex (tagId, "[0-9a-fA-F]*");
                        if (null == resValidateDigits)
                            throw new Exception(serialLabel + " is not valid: " + tagId);
                    }
                }

                // get ip and port to determine the printer that receives the zpl instruction
                String ipAddressString = "ipAddress.value";
                String portString = "port.value";
                String ip = null;
                String port = null;
                String whereThingPrinter = "thingTypeCode=" + thingTypeCodeRFIDPrinter;
                List<String> filterFieldsPrinter = new ArrayList<>();
                filterFieldsPrinter.add(serialNumber);
                filterFieldsPrinter.add(ipAddressString);
                filterFieldsPrinter.add(portString);

                Map<String, Object> thingUdfValues = ThingMongoDAO.getInstance().getThingUdfValues(whereThingPrinter, null, filterFieldsPrinter, null);
                if (null != thingUdfValues && !thingUdfValues.isEmpty()){
                    List<Map<String,Object>> udfValueList = (ArrayList) thingUdfValues.get("results");
                    if (null != udfValueList && !udfValueList.isEmpty()){
                        for (Map<String,Object> udfValuesMap : udfValueList){
                            if (null != udfValuesMap.get(serialNumber) && udfValuesMap.get(serialNumber).equals(thingRFIDPrinter)){
                                ip = (String) udfValuesMap.get(ipAddressString);
                                port = (String) udfValuesMap.get(portString);
                            }
                        }
                    }
                }
                if (null == ip || null == port || ip.isEmpty() || port.isEmpty()) {
                    throw new Exception("Cannot determine the printer");
                }
                logger.info("RFID printer has been determined with ipAddress " + ip + " and port " + port);

                // send ZPL instruction to the printer in order to encode RFID tag and print barcode
                if (offLine == Boolean.FALSE){
                    RFIDPrinterUtil.sendZplToPrint(ip, Integer.parseInt(port), evaluatedZpl, rfidEncode);
                } else {
                    zplPrintedTags.add(evaluatedZpl);
                }

                // response
                printedTags.add(tagId);

            } catch (Exception e) {
                logger.error(e);
                handleError(notPrintedTags, tagId, e.getMessage());
            }
        }
        mapResponse.put("printedTags", printedTags);
        mapResponse.put("notPrintedTags", notPrintedTags);
        if (!zplPrintedTags.isEmpty()){
            mapResponse.put("zplPrintedTags", zplPrintedTags);
        }
        return mapResponse;
    }

    private static void handleError (List errors, String key, String value){
        Map<String,Object> error = new HashMap<String,Object>();
        error.put("serial", key);
        error.put("message", value);
        errors.add(error);
    }

    public static String validateRegex (String formula, String regex) throws Exception{
        String result = null;
        try{
            if (null != formula && !formula.isEmpty()) {
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(formula);
                if (matcher.matches()) {
                    int total = matcher.groupCount();
                    if (total == 0)
                        result = matcher.group();
                    else
                        result = matcher.group(total-1);
                }
            }
        } catch (Exception e) {
            throw new Exception("Error getting the id to encode.", e);
        }
        return result;
    }

}
