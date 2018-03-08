package com.tierconnect.riot.appcore.dao;

import com.mysema.query.types.path.EntityPathBase;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.QGroup;
import com.tierconnect.riot.sdk.dao.DAOHibernateImp;

public class GroupDAO extends DAOHibernateImp<Group, Long>{
	@Override
	public EntityPathBase<Group> getEntityPathBase() {
		return QGroup.group;
	}



}
