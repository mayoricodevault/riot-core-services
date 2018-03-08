package com.tierconnect.riot.iot.utils;

import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.iot.entities.BackgroundProcess;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 *
 * @author : aruiz
 * @date : 2/14/17 5:49 PM
 * @version:
 */
public class BackgroundProgressStatus {
    static private BackgroundProgressStatus INSTANCE = new BackgroundProgressStatus();

    static Logger logger = Logger.getLogger(BackgroundProgressStatus.class);

    public static BackgroundProgressStatus getInstance()
    {
        return INSTANCE;
    }

    public Map<String, Map<String,Object>> mapStatus = new HashMap<>();

    public Map<String,Object> getMapTemporalStatus(String id){
        Map<String, Object> mapValues = new HashMap<>();
        if (this.mapStatus.get(id) == null){
            mapValues.put("count",1);
            mapValues.put("percent",0);
            mapValues.put("status", "ADDED");
            this.mapStatus.put(id,mapValues);

        }else{
            mapValues = this.mapStatus.get(id);
            int count = Integer.parseInt(mapValues.get("count").toString());
            if (count >=3){
                return null;
            }else{
                if (mapValues.get("backgroundProcess")==null){
                    mapValues.put("count",count++);
                    this.mapStatus.put(id,mapValues);                }
            }
        }
        return  mapValues;
    }

    public Map<String,Object> getMapStatus(String id){
        Map<String, Object> mapValues = this.mapStatus.get(id);
        return  mapValues;
    }

    public void setMapStatus(String id, String status,Integer percent, BackgroundProcess backgroundProcess, String module, Long userId, int total, int processed, Date iniDate){
        Map<String, Object> mapValues = new HashMap<>();
        String temporalName = id+module;
        mapValues.put("status", status);
        mapValues.put("moduleId",id);
        mapValues.put("typeProcess",module);
        mapValues.put("percent", percent);
        mapValues.put("backgroundProcess", backgroundProcess);
        mapValues.put("count",0);
        mapValues.put("userId",userId);
        mapValues.put("totalRecords",total);
        mapValues.put("processedRecords",processed);
        mapValues.put("iniDate",iniDate);

        this.mapStatus.put(temporalName,mapValues);
    }

    public List<Map<String,Object>> getAllMapStatus(){
        User user = (User) SecurityUtils.getSubject().getPrincipal();
        List<Map<String,Object>> result =  new ArrayList<>();
        Iterator<Map.Entry<String, Map<String,Object>>> it = this.mapStatus.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String,Map<String,Object>> mapValue = it.next();
            Map<String,Object> mapValues = mapValue.getValue();
            if (mapValues.get("userId") != null ) {
                if (mapValues.get("userId").equals(user.getId())) {
                    result.add(mapValues);
                }
            }else {
                logger.error("userId value is null in BackgroundProgressStatus");
            }
        }
        return result;
    }


}
