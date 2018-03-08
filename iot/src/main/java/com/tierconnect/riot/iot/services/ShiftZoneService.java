package com.tierconnect.riot.iot.services;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.hibernate.HibernateDeleteClause;
import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.sdk.dao.HibernateDAOUtils;
import org.hibernate.Query;
import org.hibernate.Session;

import java.util.*;

/**
 * Created by cfernandez
 * Modified by bbustillos
 * 2/5/2015.
 */
public class ShiftZoneService extends ShiftZoneServiceBase
{
    public Map<String, Map> selectAllShiftZone()
    {
        Map<String, Map> mapResult = new LinkedHashMap<>();
        Map<String, String> mapZone = new LinkedHashMap<>();
        Session session = getShiftZoneDAO().getSession();
        String hqlQuery = "from ShiftZone";
        Query query = HibernateDAOUtils.createQuery(session, hqlQuery);
        List<ShiftZone> results = query.list();
        Iterator<ShiftZone> iterator = results.iterator();
        while (iterator.hasNext()){
            ShiftZone shiftZone = iterator.next();
            Shift shift = shiftZone.getShift();
            Zone zone = shiftZone.getZone();
            mapZone.put(zone.getName(), zone.getCode());
            mapResult.put(shift.getCode(), mapZone);
        }
        return mapResult;
    }

    public List<ShiftZone> selectAllByZone(Zone zone) {
        HibernateQuery query = getShiftZoneDAO().getQuery();
        BooleanBuilder shiftZoneWhereQuery = new BooleanBuilder(QShiftZone.shiftZone.zone.eq(zone));
        return query.where(shiftZoneWhereQuery).list(QShiftZone.shiftZone);
    }

    public void deleteAllByShift(Shift shift) {
        HibernateDeleteClause query = getShiftZoneDAO().getDeleteQuery();
        BooleanBuilder shiftZoneWhereQuery = new BooleanBuilder(QShiftZone.shiftZone.shift.eq(shift));
        query.where(shiftZoneWhereQuery).execute();
    }

}

