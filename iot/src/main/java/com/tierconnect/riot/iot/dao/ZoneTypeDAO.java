package com.tierconnect.riot.iot.dao;

import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.tierconnect.riot.iot.entities.QZoneType;

public class ZoneTypeDAO extends ZoneTypeDAOBase
{
    public boolean validateDuplicatedNameByGroup (String name, long groupId)
    {
        JPQLQuery query = new HibernateQuery(getSession());
        QZoneType qZoneType = QZoneType.zoneType;
        query.from(qZoneType).where(qZoneType.name.eq(name).and(qZoneType.group.id.eq(groupId)));
        Long constraintNum = query.count();
        boolean response = (null != constraintNum && constraintNum > 0)? false : true;
        return response;
    }
}

