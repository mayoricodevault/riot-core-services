package com.tierconnect.riot.iot.dao.util;

import com.mongodb.BasicDBObject;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.commons.entities.IThingField;
import com.tierconnect.riot.commons.utils.epcDecoder.TagMemoryToEpcTagDecoder;
import com.tierconnect.riot.iot.dao.mongo.ThingMongoDAO;
import com.tierconnect.riot.iot.entities.Thing;
import com.tierconnect.riot.iot.entities.ThingField;
import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;

import javax.el.*;
import java.beans.FeatureDescriptor;
import java.math.BigInteger;
import java.util.*;

/**
 * Created by fflores on 2/24/16.
 */
public class ThingResolver extends ELResolver {
    private static Logger logger = Logger.getLogger( ThingResolver.class );
    static long count = 0;
    public Object object;

    public ThingResolver(Object object) {
        this.object = object;
    }

    @Override
    public Object getValue(ELContext context, Object base, Object property) {
        logger.debug( "count: " + count + " base='" + base + "' property='" + property + "'" );
        Object value = null;

        if( base == null ) {
            if (object != null && object instanceof Thing){
                    value = new BeanELResolver().getValue(context, object, property);
                    if (value instanceof Map) {
                        value = ((Map) value).get("value");
                    }
            } else {
                value = new MapELResolver().getValue(context, null, property);
            }
        } else {
            if (("dwelltime").equals(property) || "timestamp".equals(property)) {
                if (base instanceof Map && ((Map) base).containsKey("time")) {
                    Date dateTime = (Date)((Map) base).get("time");
                    Long timeStamp = dateTime.getTime();
                    Long dwelltime = (System.currentTimeMillis()-timeStamp)/1000;
                    if ("dwelltime".equals(property)) value=dwelltime;
                    else value=new Date(timeStamp);
                } else {
                    if (base instanceof Map && ((Map) base).containsKey(property)) {
                        value = ((Map) base).get(property);
                    }
                    else if (base instanceof ThingField) {
                        if (property.equals("dwelltime")) {
                            value = new BeanELResolver().getValue(context, base, "timestamp");
                            value = (System.currentTimeMillis()-Long.parseLong(value.toString()))/1000;
                        } else {
                            value = new BeanELResolver().getValue(context, base, property);
                        }
                    }
                    else {
                        value = new Date();
                        if ("dwelltime".equals(property))
                            value = 0;
                    }
                }
            } else
            if (base instanceof Map || base instanceof BasicDBObject){
                value = new MapELResolver().getValue(context, base, property);
            }
            else if (base instanceof Thing){
                Map<String,IThingField> thingFields = ((Thing)base).getThingFields();
                if (thingFields != null && !thingFields.isEmpty()){
                    ThingField thingField = (ThingField) thingFields.get(property);
                    if (thingField != null){
                        value = thingField.getValue();
                    }
                }
                if (value == null){
                    value = new BeanELResolver().getValue(context, base, property);
                }
            } else if (base instanceof ThingType) {
                if (object != null){
                    value = new BeanELResolver().getValue(context, base, property);
                } else {
                    value = ((ThingType) base).getThingTypeField(property.toString());
                }
            } else {
                value = new BeanELResolver().getValue(context, base, property);
            }
        }
        if (value == null){
            throw new PropertyNotFoundException("Cannot find property " + property);
        }
        context.setPropertyResolved( true );

        logger.debug( "count: " + count + " base='" + base + "' property='" + property + "' value='" + value + "'" );
        count++;

        return value;

    }

    @Override
    public Object invoke(ELContext context, Object base, Object method, Class<?>[] paramTypes, Object[] params) {
        return super.invoke(context, base, method, paramTypes, params);
    }

    @Override
    public Class<?> getType(ELContext context, Object base, Object property) {
        return null;
    }

    @Override
    public void setValue(ELContext context, Object base, Object property, Object value) {
    }

    @Override
    public boolean isReadOnly(ELContext context, Object base, Object property) {
        return false;
    }

    @Override
    public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context, Object base) {
        return null;
    }

    @Override
    public Class<?> getCommonPropertyType(ELContext context, Object base) {
        return null;
    }

    @Override
    public Object convertToType(ELContext context, Object obj, Class<?> targetType) {
        return super.convertToType(context, obj, targetType);
    }

    public static long count(String thingTypeCode, String condition){
        return 0;
    }

    public static long countParentChild(long value, String thingTypeCode, String condition){
        long result = 0;
        StringBuilder whereThing = new StringBuilder();
        whereThing.append("parent._id=" + value);
        if (null != thingTypeCode && !thingTypeCode.isEmpty()) {
            whereThing.append("&thingTypeCode=" + thingTypeCode);
        }
        List<String> filterFields = new ArrayList<>();
        filterFields.add("serialNumber");
        String cond = condition != null && !condition.isEmpty() ? condition : null;
        Map<String, Object> udfResult = ThingMongoDAO.getInstance().getThingUdfValues(whereThing.toString(), cond, filterFields, null);
        if (null != udfResult && !udfResult.isEmpty()) {
            List<Map<String, Object>> udfValuesList = (List<Map<String, Object>>) udfResult.get("results");
            result = udfValuesList.size();
        }
        return result;
    }

    public static long countThingTypeUDF(long value,String udfCondition,String thingTypeCode,String condition){
        long result = 0;
        StringBuilder whereThing = new StringBuilder();
        whereThing.append(udfCondition + value);
        if (null != thingTypeCode && !thingTypeCode.isEmpty()) {
            whereThing.append("&thingTypeCode=" + thingTypeCode);
        }
        List<String> filterFields = new ArrayList<>();
        filterFields.add("serialNumber");
        String cond = condition != null && !condition.isEmpty() ? condition : null;
        Map<String, Object> udfResult = ThingMongoDAO.getInstance().getThingUdfValues(whereThing.toString(), cond, filterFields, null);
        if (null != udfResult && !udfResult.isEmpty()) {
            List<Map<String, Object>> udfValuesList = (List<Map<String, Object>>) udfResult.get("results");
            result = udfValuesList.size();
        }
        return result;
    }

    public static String decoderRing001(String serialNumber){
        String UPC = null;
        try{
            if (serialNumber != null){
                // convert serialNumber to binary
                StringBuilder binary = new StringBuilder();
                StringBuilder logString = new StringBuilder();
                for (int i=0;i<(serialNumber.length()/2);i++){
                    int j = i*2;
                    String substr = serialNumber.substring(j,j+2);
                    String subBinary = new BigInteger(substr,16).toString(2);
                    binary.append(StringUtils.leftPad(subBinary,8,"0"));
                }
                logString.append("binary: " + binary.length() + ", ");

                // header
                String header = binary.substring(0,8);
                logString.append("header: " + header + ", ");
                int headerDecimal = Integer.parseInt(header, 2);
                logString.append("headerDecimal: " + headerDecimal + ", ");

                // filter
                String filter = binary.substring(8,11);
                logString.append("filter: " + filter + ", ");
                int filterDecimal = Integer.parseInt(filter, 2);
                logString.append("filterDecimal: " + filterDecimal + ", ");

                // partition
                String partition = binary.substring(11,14);
                logString.append("partition: " + partition + ", ");
                int partitionDecimal = Integer.parseInt(partition, 2);
                logString.append("partitionDecimal: " + partitionDecimal + ", ");

                // company
                Map<Integer,Integer> companyReference = new HashMap<>();
                companyReference.put(0,40);
                companyReference.put(1,37);
                companyReference.put(2,34);
                companyReference.put(3,31);
                companyReference.put(4,27);
                companyReference.put(5,24);
                companyReference.put(6,20);
                Integer cr = companyReference.get(partitionDecimal);
                String company = binary.substring(14,14 + cr);
                logString.append("company: " + company + ", ");
                int companyDecimal = Integer.parseInt(company, 2);
                logString.append("companyDecimal: " + companyDecimal + ", ");

                // item
                String item = binary.substring(14+cr,24+14+cr);
                logString.append("item: " + item + ", ");
                int itemDecimal = Integer.parseInt(item, 2);
                logString.append("itemDecimal: " + itemDecimal + ", ");

                // item string
                String companyStr = String.valueOf(companyDecimal);
                String itemStr = companyStr.substring(companyStr.length()-1) + StringUtils.leftPad(String.valueOf(itemDecimal),6,"0");
                logString.append("itemStr: " + itemStr + ", ");

                // preSerial
                String preSerial = "0" + companyStr + itemStr.substring(1,7);
                logString.append("preSerial: " + preSerial + ", ");

                // checkDigit
                int a = (-3 * ( Integer.parseInt(itemStr.substring(0,1)) + Integer.parseInt(itemStr.substring(2,3)) + Integer.parseInt(itemStr.substring(4,5)) + Integer.parseInt(itemStr.substring(6,7)) )) - (Integer.parseInt(itemStr.substring(1,2)) + Integer.parseInt(itemStr.substring(3,4)) + Integer.parseInt(itemStr.substring(5,6)));
                int checkDigit = a < 0 ? (a % 10 + 10) % 10 : a % 10;
                logString.append("checkDigit: " + checkDigit + ", ");

                // UPC
                UPC = itemStr + checkDigit;
                logString.append("UPC: " + UPC);
                logger.info("UPC calculation > " + logString);
            }
        }catch (Exception e){
            logger.info("Not possible to generate a valid UPC from: " + serialNumber + ", setting default value (00000000)");
            UPC = "00000000";
        }
        return UPC;
    }

    public static String decoderRing002(String serialNumber){
        String EANWithCkeckDigit;
        TagMemoryToEpcTagDecoder tagMemoryToEpcTagDecoder = new TagMemoryToEpcTagDecoder();
        EANWithCkeckDigit = tagMemoryToEpcTagDecoder.tagFieldsDecoder(serialNumber, "gs1Key");
        long temporal = Long.parseLong(EANWithCkeckDigit);
        EANWithCkeckDigit = String.valueOf(temporal);
        return EANWithCkeckDigit;
    }

    public static String currentUser(String property){
        String result = null;
        if(property != null){
            try{
                User currentUser = (User)SecurityUtils.getSubject().getPrincipal();
                switch (property){
                    case "firstName":
                        result = currentUser.getFirstName();
                        break;
                    case "id":
                        result = currentUser.getId().toString();
                        break;
                    case "lastName":
                        result = currentUser.getLastName();
                        break;
                    case "email":
                        result = currentUser.getEmail();
                        break;
                    case "userGroup":
                        result = currentUser.getGroup().getName();
                        break;
                    case "userActiveGroup":
                        result = currentUser.getActiveGroup().getName();
                        break;
                    case "groupCode":
                        result = currentUser.getGroup().getCode();
                        break;
                    case "activeGroupCode":
                        result = currentUser.getActiveGroup().getCode();
                        break;
                    case "username":
                    default:
                        result = currentUser.getUsername();
                }
            }catch (Exception e){
                logger.error("Not possible to resolve currentUser(\"" + property + "\") expression.", e);
            }
        }
        return result;
    }


    public static String epcDecode(String serialNumber, String field){
        TagMemoryToEpcTagDecoder tagMemoryToEpcTagDecoder = new TagMemoryToEpcTagDecoder();
        return tagMemoryToEpcTagDecoder.tagFieldsDecoder(serialNumber, field);
    }
}
