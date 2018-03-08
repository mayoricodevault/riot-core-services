package com.tierconnect.riot.iot.dao;

import java.util.Date;
import java.util.List;

import javax.persistence.Query;

import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.mysema.query.types.path.EntityPathBase;
import com.tierconnect.riot.iot.entities.ThingField;
import com.tierconnect.riot.sdk.dao.DAOHibernateImp;

@Deprecated
public class ThingFieldDAO extends DAOHibernateImp<ThingField, Long>
{

	@Override
	public EntityPathBase<ThingField> getEntityPathBase()
	{
        throw new RuntimeException("This service is canceled and should not be used.");
	}

	public void update( long thingFieldId, String value, long time )
	{
        throw new RuntimeException("This service is canceled and should not be used.");
		//org.hibernate.Query q = getSession().createQuery( "from ThingField where id = :id " );
		//q.setParameter( "id", thingFieldId );
		//ThingField tf = (ThingField) q.list().get( 0 );
		//tf.setValue( value );
		//tf.setBlinkDate( new Date( time ) );
		//getSession().update( tf );
	}

	public List<ThingField> getThingFieldsByNameAndThing (String name, Long thingId) {
        throw new RuntimeException("This service is canceled and should not be used.");
		//JPQLQuery query = new HibernateQuery(getSession());
		//QThingField qThingField = QThingField.thingField;
		//query.from(qThingField).where(qThingField.name.eq(name).and(qThingField.thing.id.eq(thingId)));
		//return query.list(qThingField);
	}

}
