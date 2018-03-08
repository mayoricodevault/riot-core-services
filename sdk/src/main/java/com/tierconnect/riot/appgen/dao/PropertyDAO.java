package com.tierconnect.riot.appgen.dao;

import com.mysema.query.types.path.EntityPathBase;
import com.tierconnect.riot.appgen.model.Property;
//import com.tierconnect.riot.appgen.model.QProperty;
import com.tierconnect.riot.sdk.dao.DAOHibernateImp;

public class PropertyDAO extends DAOHibernateImp<Property, Long> 
{
		@Override
		public EntityPathBase<Property> getEntityPathBase() 
		{
			//return QProperty.property;
			return null;
		}
}

