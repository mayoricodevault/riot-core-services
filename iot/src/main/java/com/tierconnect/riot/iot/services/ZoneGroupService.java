package com.tierconnect.riot.iot.services;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.Generated;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.types.expr.BooleanExpression;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.QUser;
import com.tierconnect.riot.iot.entities.*;

@Generated("com.tierconnect.riot.appgen.service.GenService")
public class ZoneGroupService extends ZoneGroupServiceBase 
{

    private static final QZoneGroup qZoneGroup = QZoneGroup.zoneGroup;

    public static void updateZones(ZoneGroup zoneGroup, List<Map<String, Object> > zoneList) {
        List<Zone> zones = zoneGroup.getZones();
        List<Zone> newZones = new LinkedList<>();

        //Cleaning zones
        zoneGroup.setZones(null);
        for(Zone zone : zones) {
            zone.setZoneGroup(null);
            ZoneService.getInstance().update(zone);
        }
        ZoneGroupService.getInstance().update(zoneGroup);

        for(Map<String, Object> zoneMap : zoneList) {
            String zoneMapId = zoneMap.get("id") != null ? zoneMap.get("id").toString() : "0";
            Long zoneId = Long.valueOf(zoneMapId);
            if(zoneId > 0) {
                Zone newZone = ZoneService.getInstance().get(zoneId);
                newZone.setZoneGroup(zoneGroup);
                ZoneService.getInstance().update(newZone);
                newZones.add(newZone);
            }
        }
        zoneGroup.setZones(newZones);
        ZoneGroupService.getInstance().update(zoneGroup);
    }

    public boolean existsZoneGroupByNameAndLocalMap(String name, Long idTenantGroup) {
        return existsZoneGroupByNameAndLocalMap(name, idTenantGroup, null);
    }

    public boolean existsZoneGroupByNameAndLocalMap(String name, Long idLocalMap, Long excludedIdLocalMap) {
        BooleanExpression predicate = qZoneGroup.name.eq(name);
        predicate = predicate.and(qZoneGroup.localMap.id.eq(idLocalMap));
        if(excludedIdLocalMap!=null){
            predicate = predicate.and(qZoneGroup.id.ne(excludedIdLocalMap));
        }
        return getZoneGroupDAO().getQuery().where(predicate).exists();
    }

    public ZoneGroup getZoneForThing(Thing thing) {
        if(thing == null) return null;
        //TODO FIX THIS METHOD OR DELETE IT
        return null;
        /*
        ThingTypeField zoneField = thing.getThingTypeField("zone");
        ZoneGroup zoneGroup = null;
        if(zoneField != null) {
            String value ;
            value = FieldValueService.value(thing.getId(), zoneField.getId());
            if(value != null && value.length() > 0) {
                List<Zone> zoneList = ZoneService.getInstance().getZonesByName(value);
                if(zoneList.size() > 0) {
                    Zone zone = zoneList.get(0);
                    zoneGroup = zone.getZoneGroup();
                }
            }
        }return zoneGroup;
        */
    }
    public ZoneGroup getByName(String name, Group group){
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QZoneGroup.zoneGroup.name.eq(name));
        be = be.and(QZoneGroup.zoneGroup.group.eq(group));
        ZoneGroup zoneGroup = getZoneGroupDAO().selectBy(be);
        return zoneGroup;
    }
}

