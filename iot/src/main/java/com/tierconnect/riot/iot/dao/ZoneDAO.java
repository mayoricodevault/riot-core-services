package com.tierconnect.riot.iot.dao;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.types.OrderSpecifier;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.path.EntityPathBase;
import com.mysema.query.types.path.ListPath;
import com.mysema.query.types.path.SetPath;
import com.tierconnect.riot.iot.entities.QZoneGroup;
import com.tierconnect.riot.iot.entities.QZonePoint;
import com.tierconnect.riot.iot.entities.Zone;
import com.tierconnect.riot.iot.entities.ZonePoint;
import com.tierconnect.riot.sdk.dao.HibernateDAOUtils;
import com.tierconnect.riot.sdk.dao.Pagination;

import javax.annotation.Generated;
import java.util.List;

@Generated("com.tierconnect.riot.appgen.service.GenDAO")
public class ZoneDAO extends ZoneDAOBase 
{
    public List<Zone> selectAll(Predicate be,
                             List<EntityPathBase<?>> properties,
                             SetPath<?, ?> qZones,
                             Pagination pagination,
                             OrderSpecifier... orders) {
        return HibernateDAOUtils.selectAllSet(getSession(), getEntityPathBase(), be, properties,
                qZones, pagination, orders);
    }


}

