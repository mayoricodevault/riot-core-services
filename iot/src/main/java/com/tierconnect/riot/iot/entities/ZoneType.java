package com.tierconnect.riot.iot.entities;

import javax.annotation.Generated;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
@Entity
@Table(name="zoneType")
@Generated("com.tierconnect.riot.appgen.service.GenModel")
public class ZoneType extends ZoneTypeBase 
{
    public Map<String,Object> publicMapProperties(List<ZonePropertyValue> ZonePropertyValuesList)
    {
        //Getting values from database

        Map<String, Object> map = publicMap();
        List<Map<String, Object>> zoneProperties = new LinkedList<>();
        Map<Long, Map<String, Object> > zonePropertyMapUnique = new HashMap<>();
        if(this.zoneProperties != null) {
            for (ZoneProperty zoneProperty : this.zoneProperties) {
                zonePropertyMapUnique.put(zoneProperty.getId(), zoneProperty.publicMap());
//                zoneProperties.add(zoneProperty.publicMap());
            }
            for(Map.Entry<Long, Map<String, Object> > entry : zonePropertyMapUnique.entrySet()) {
                Map<String, Object> entryItem = entry.getValue();
                entryItem.put("value", "");
                if(entryItem.containsKey("id")) {
                    for (ZonePropertyValue zonePropertyValue : ZonePropertyValuesList) {
                        if (zonePropertyValue.getZonePropertyId().equals(Long.valueOf(entryItem.get("id").toString()))) {
                            entryItem.put("value", zonePropertyValue.getValue());
                        }
                    }
                }
                zoneProperties.add(entry.getValue());
            }
        }
        map.put("zoneProperties", zoneProperties);
        return map;
    }
}

