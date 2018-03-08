package com.tierconnect.riot.iot.fixdb;

import java.util.*;

/**
 * Created by cvertiz on 12/1/2015.
 */
public class CassandraCache {

    private Map<Long, Map<Long, List<Map<String, Object>>>> fieldTypeHistory = new HashMap<>();

    private Map<Long, Map<Long, Map<String, Object>>> fieldType = new HashMap<>();

    private static CassandraCache instance = new CassandraCache();

    private boolean cached = false;

    public static CassandraCache getInstance(){
        return instance;
    }

    public static void clearCache(){
        instance = new CassandraCache();
    }

    public Map<Long, List<Map<String, Object>>> getHistory(long thingId){
        return fieldTypeHistory.get(thingId);
    }

    public  Map<Long, Map<String, Object>> getValue(long thingId){
        return fieldType.get(thingId);
    }

    public void addValueToCache(long thingId, long fieldTypeId, Date time, String value){
        Map<String, Object> aRow = new HashMap<>();
        aRow.put("value", value);
        aRow.put("time", time);

        if(fieldType.containsKey(thingId)){
            fieldType.get(thingId).put(fieldTypeId, aRow);
        }else{
            Map<Long, Map<String, Object>> thingTypeField = new HashMap<>();
            thingTypeField.put(fieldTypeId, aRow);
            fieldType.put(thingId,thingTypeField);
        }
    }

    public void addHistoryToCache(long thingId, long fieldTypeId, Date time, String value){
        Map<String, Object> aRow = new HashMap<>();
        aRow.put("value", value);
        aRow.put("time", time);

        if(fieldTypeHistory.containsKey(thingId)){
            if (fieldTypeHistory.get(thingId).containsKey(fieldTypeId)) {
                fieldTypeHistory.get(thingId).get(fieldTypeId).add(aRow);
            } else {
                List<Map<String, Object>> data = new ArrayList<>();
                data.add(aRow);
                fieldTypeHistory.get(thingId).put(fieldTypeId, data);
            }
        }else{
            List<Map<String, Object>> data = new ArrayList<>();
            data.add(aRow);
            Map<Long, List<Map<String, Object>>> field = new HashMap<>();
            field.put(fieldTypeId, data);
            fieldTypeHistory.put(thingId, field);
        }
    }

    public boolean isCached() {
        return cached;
    }

    public void setCached(boolean cached) {
        this.cached = cached;
    }
}
