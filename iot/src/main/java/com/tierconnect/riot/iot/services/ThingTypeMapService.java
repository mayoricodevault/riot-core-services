package com.tierconnect.riot.iot.services;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.tierconnect.riot.iot.entities.QThingType;
import com.tierconnect.riot.iot.entities.QThingTypeMap;
import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.entities.ThingTypeMap;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;

import java.util.List;
import java.util.Set;

public class ThingTypeMapService extends ThingTypeMapServiceBase
{
    private static final QThingTypeMap qThingTypeMap = QThingTypeMap.thingTypeMap;

    public List<ThingTypeMap> getThingTypeMapByParentId(Long parentId)
    {
        return getThingTypeMapDAO().selectAllBy("parent.id", parentId);
    }
    public List<ThingTypeMap> getThingTypeMapByChildId(Long parentId)
    {
        return getThingTypeMapDAO().selectAllBy("child.id", parentId);
    }

    /**
     * This function checks if a thing type is Child
     */
    public boolean isChild(ThingType thingType) {

        Long count = getThingTypeMapDAO().countAll( qThingTypeMap.child.eq( thingType ) );
        return count>0;
    }

    /**
     * This function checks if a thing type is Parent
     */
    public boolean isParent(ThingType thingType) {

        Long count = getThingTypeMapDAO().countAll( qThingTypeMap.parent.eq( thingType ) );
        return count>0;
    }

    /**
     * create for Unit Test
     * @return
     */
    public ThingTypeMap getRelationParentChild(ThingType parent, ThingType child) {
        BooleanBuilder builder = new BooleanBuilder();
        builder = builder.and(QThingTypeMap.thingTypeMap.parent.id.eq(parent.getId()));
        builder = builder.and(QThingTypeMap.thingTypeMap.child.id.eq(child.getId()));
        return getThingTypeMapDAO().getQuery().where(builder).uniqueResult(QThingTypeMap.thingTypeMap);
    }

    public List<ThingType> getParentThingTypes(){
        HibernateQuery query = new HibernateQuery(HibernateSessionFactory.getInstance().getCurrentSession());
        return query.from(QThingTypeMap.thingTypeMap)
                .innerJoin(QThingTypeMap.thingTypeMap.parent, QThingType.thingType)
                .setCacheable(true)
                .list(QThingType.thingType);
    }

    public List<ThingTypeMap> getChildrenTypeMaps(Long thingTypeId){
        HibernateQuery query = new HibernateQuery(HibernateSessionFactory.getInstance().getCurrentSession());
        return query.from(QThingTypeMap.thingTypeMap)
                .innerJoin(QThingTypeMap.thingTypeMap.parent, QThingType.thingType)
                .where(QThingType.thingType.id.eq(thingTypeId))
                .setCacheable(true)
                .list(QThingTypeMap.thingTypeMap);
    }

    public List<ThingTypeMap> getParentTypeMaps(Long thingTypeId){
        HibernateQuery query = new HibernateQuery(HibernateSessionFactory.getInstance().getCurrentSession());
        return query.from(QThingTypeMap.thingTypeMap)
                .innerJoin(QThingTypeMap.thingTypeMap.child, QThingType.thingType)
                .where(QThingType.thingType.id.eq(thingTypeId))
                .setCacheable(true)
                .list(QThingTypeMap.thingTypeMap);
    }

}

