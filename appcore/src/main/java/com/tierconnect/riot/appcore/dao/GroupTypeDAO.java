package com.tierconnect.riot.appcore.dao;

import com.mysema.query.types.path.EntityPathBase;
import com.tierconnect.riot.appcore.entities.GroupType;
import com.tierconnect.riot.appcore.entities.QGroupType;
import com.tierconnect.riot.sdk.dao.DAOHibernateImp;

public class GroupTypeDAO extends DAOHibernateImp<GroupType, Long>{

	@Override
	public EntityPathBase<GroupType> getEntityPathBase() {
		return QGroupType.groupType;
	}
}
