package com.tierconnect.riot.iot.utils;

import com.tierconnect.riot.iot.entities.Zone;
import com.tierconnect.riot.iot.entities.ZoneProperty;
import com.tierconnect.riot.iot.entities.ZoneType;
import com.tierconnect.riot.iot.services.ZonePropertyService;
import com.tierconnect.riot.iot.services.ZoneService;
import com.tierconnect.riot.iot.services.ZoneTypeService;

import java.util.*;

/**
 * Created by user on 1/9/15.
 */
public class ReportDashboardUtils {

    public static Map<String, Object> executeDashboardReport(List<Object> zonePropertiesIdList, String thingPropertyName) {
        Map<String, Object> mapResponse = new HashMap<>();

        List<ZoneProperty> zonePropertiesList = new LinkedList<>();

        //Getting ZonePropertiesBean from database
        if (zonePropertiesIdList != null && zonePropertiesIdList.size() > 0) {
            for(int it=0; it < zonePropertiesIdList.size(); it++) {
                Long zonePropertyId = Long.valueOf( zonePropertiesIdList.get(it).toString() );
                ZoneProperty zoneProperty = ZonePropertyService.getInstance().get(zonePropertyId);
                if(zoneProperty != null) {
                    zonePropertiesList.add(zoneProperty);
                }
            }
        }

        //Getting Zones from ZoneProperty.ZoneType & Group by zonePropertyName
        Map<String, List<Zone> > zonePropertyMap = new HashMap<>();
        for (ZoneProperty zoneProperty : zonePropertiesList) {
            ZoneType zoneType = zoneProperty.getZoneType();
            if(zonePropertyMap.containsKey(zoneProperty.getName())) {
                List<Zone> zones = ZoneService.getZonesByZoneTypeId(zoneType.getId(), zoneProperty.getId(), "true");
                List<Zone> zonesFromMap = zonePropertyMap.get(zoneProperty.getName());
                zonesFromMap.addAll(zones);
                zonePropertyMap.put(zoneProperty.getName(), zonesFromMap);
            }
            else {
                List<Zone> zones = ZoneService.getZonesByZoneTypeId(zoneType.getId(), zoneProperty.getId(), "true");
                zonePropertyMap.put(zoneProperty.getName(), zones);
            }
        }

        for (Map.Entry<String, List<Zone> > entryMap : zonePropertyMap.entrySet() ) {

        }

        return mapResponse;
    }
}
