package com.tierconnect.riot.iot.services;

import com.tierconnect.riot.iot.entities.QZoneProperty;
import com.tierconnect.riot.iot.entities.ZoneProperty;
import com.tierconnect.riot.iot.entities.ZoneType;
import javax.annotation.Generated;
import java.util.List;

@Generated("com.tierconnect.riot.appgen.service.GenService")
public class ZonePropertyService extends ZonePropertyServiceBase 
{

    public List<ZoneProperty> getbyZoneType(ZoneType zoneType) {
        return getZonePropertyDAO().getQuery()
                .where(QZoneProperty.zoneProperty.zoneType.eq(zoneType)).list(QZoneProperty.zoneProperty);
    }

    /**
     * get Zone Property By Zone Property object
     * @param zoneProperty zone property
     * @return a Zone Property by Zone Property Object
     */
    public ZoneProperty getZonePropertyByZoneProperty(ZoneProperty zoneProperty) {
        return getZonePropertyDAO().getQuery()
                .where(QZoneProperty.zoneProperty.eq(zoneProperty)).uniqueResult(QZoneProperty.zoneProperty);
    }

    /**
     * get Zone Property By Zone Property Id
     * @param id zone property id
     * @return a Zone Property by Zone Property Id
     */

    public ZoneProperty getZonePropertyById(Long id) {
        return getZonePropertyDAO().getQuery()
                .where(QZoneProperty.zoneProperty.id.eq(id)).uniqueResult(QZoneProperty.zoneProperty);
    }

    /**
     * get Zone Property By Zone Property's Name and Zone Type
     * @param name
     * @return a Zone Property by Name and ZoneType
     */
    public ZoneProperty getZonePropertyByName(String name, ZoneType zoneType) {
        return getZonePropertyDAO().getQuery()
                .where(QZoneProperty.zoneProperty.name.eq(name).and(QZoneProperty.zoneProperty.zoneType.eq(zoneType))).uniqueResult(QZoneProperty.zoneProperty);
    }
}

