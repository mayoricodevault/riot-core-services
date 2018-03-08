package com.tierconnect.riot.iot.dao;

import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.types.OrderSpecifier;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.path.EntityPathBase;
import com.mysema.query.types.path.ListPath;
import com.tierconnect.riot.iot.entities.QThing;
import com.tierconnect.riot.iot.entities.Thing;
import com.tierconnect.riot.sdk.dao.HibernateDAOUtils;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;
import com.tierconnect.riot.sdk.dao.Pagination;
import com.mysema.query.jpa.hibernate.HibernateQuery;
import java.util.List;

public class ThingDAO extends ThingDAOBase 
{
	public Thing selectBySerial( String serial )
	{
		return this.selectBy("serial", serial);
	}

    public List<Thing> selectAll(Predicate be,
                                 List<EntityPathBase<?>> properties,
                                 ListPath<?, ?> qThings,
                                 Pagination pagination,
                                 OrderSpecifier... orders) {
        return HibernateDAOUtils.selectAll(getSession(), getEntityPathBase(), be, properties,
                qThings, pagination, orders);
    }

    public boolean existsSerial(String serial, Long thingTypeId) {
        HibernateQuery query = new HibernateQuery(HibernateSessionFactory.getInstance().getCurrentSession());
        QThing qThing = QThing.thing;
        boolean notExists = query.from(qThing).where(qThing.serial.eq(serial).and(qThing.thingType.id.eq(thingTypeId)))
                .setCacheable(true).list(qThing.id).isEmpty();
        return !notExists;
    }

    public boolean existsSerial(String serial, Long thingTypeId, Long excludeId)
    {
        JPQLQuery query = new HibernateQuery(getSession());
        QThing qThing = QThing.thing;
        query.from(qThing).where(qThing.serial.eq(serial).and(qThing.thingType.id.eq(thingTypeId)).and(qThing.id.eq(excludeId).not()));
        Long constraintNum = query.count();
        boolean response = (null != constraintNum && constraintNum > 0)? true : false;
        return response;
    }

}

