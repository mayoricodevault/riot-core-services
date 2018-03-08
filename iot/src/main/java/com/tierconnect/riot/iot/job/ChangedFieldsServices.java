package com.tierconnect.riot.iot.job;

import com.mongodb.DBObject;
import com.tierconnect.riot.commons.dao.mongo.ChangedFields;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by cvertiz on 9/15/16.
 */
public class ChangedFieldsServices implements ChangedFields {

    Map<String, String> changedFields;
    Map<String, Long> currentFieldDates;

    public ChangedFieldsServices(Map<String, Object> thingMessage, DBObject thingMongo, DBObject thingMongoCurrent) {
        changedFields = new HashMap<>();
        currentFieldDates = new HashMap<>();
        for (Map.Entry<String, Object> fieldMessage : thingMessage.entrySet()){
            String value;
            String valueOld = null;
            Object field = ((Map)fieldMessage.getValue()).get("value");
            Long currentFieldDate = null;
            Object fieldOld = null;
            if(thingMongo.containsField(fieldMessage.getKey())){
                fieldOld = ((Map)thingMongo.get(fieldMessage.getKey())).get("value");
                currentFieldDate = ((Date)((Map)thingMongoCurrent.get(fieldMessage.getKey())).get("time")).getTime();
            }

            if(field instanceof Map){
                if(((Map)field).containsKey("serialNumber")){
                    value = ((Map)field).get("serialNumber").toString();
                    if(fieldOld != null){
                        valueOld = ((Map)fieldOld).get("serialNumber").toString();
                    }
                }else{
                    value = ((Map)field).get("id").toString();
                    if(fieldOld != null){
                        valueOld = ((Map)fieldOld).get("id").toString();
                    }
                }
            }else if(field instanceof Date){
                value = ((Date)field).getTime() + "";
                if(fieldOld != null){
                    valueOld = ((Date)fieldOld).getTime() + "";
                }
            }else{
                value = field.toString();
                if(fieldOld != null){
                    valueOld = fieldOld.toString();
                }
            }

            if(!value.equals(valueOld)){
                changedFields.put(fieldMessage.getKey(), value);
                currentFieldDates.put(fieldMessage.getKey(), currentFieldDate);
            }
        }
    }

    @Override
    public Map<String, String> getChangedFields() {
        return changedFields;
    }

    @Override
    public Long getCurrentFieldDate(String fieldName) {
        return currentFieldDates.get(fieldName);
    }
}
