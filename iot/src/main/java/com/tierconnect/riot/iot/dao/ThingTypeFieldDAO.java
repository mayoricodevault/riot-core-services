package com.tierconnect.riot.iot.dao;

import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.tierconnect.riot.iot.entities.QThingTypeField;
import com.tierconnect.riot.iot.entities.ThingTypeField;

/**
 * 
 * @author garivera
 *
 */
public class ThingTypeFieldDAO extends ThingTypeFieldDAOBase {

    public ThingTypeField getThingTypeFieldByNameAndThingType (String name, Long thingTypeId){
        JPQLQuery query = new HibernateQuery(getSession());
        QThingTypeField qThingTypeField = QThingTypeField.thingTypeField;
        query.from(qThingTypeField).where(qThingTypeField.name.eq(name).and(qThingTypeField.thingType.id.eq(thingTypeId)));
        return query.uniqueResult(qThingTypeField);
    }
}
