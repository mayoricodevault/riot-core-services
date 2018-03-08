package com.tierconnect.riot.appgen.dao;

import com.mysema.query.types.path.EntityPathBase;
import com.tierconnect.riot.appgen.model.Clazz;
//import com.tierconnect.riot.appgen.model.QClazz;
import com.tierconnect.riot.sdk.dao.DAOHibernateImp;


public class ClazzDAO extends DAOHibernateImp<Clazz, Long> 
{
		@Override
		public EntityPathBase<Clazz> getEntityPathBase() 
		{
			//return QClazz.clazz;
			return null;
		}
}

