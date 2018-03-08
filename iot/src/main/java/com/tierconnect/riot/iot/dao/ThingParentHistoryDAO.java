package com.tierconnect.riot.iot.dao;

import com.mysema.query.types.path.EntityPathBase;
import com.tierconnect.riot.iot.entities.QThingParentHistory;
import com.tierconnect.riot.iot.entities.ThingParentHistory;
import com.tierconnect.riot.sdk.dao.DAOHibernateImp;

public class ThingParentHistoryDAO extends DAOHibernateImp<ThingParentHistory, Long> {

	@Override
	public EntityPathBase<ThingParentHistory> getEntityPathBase() {
		return QThingParentHistory.thingParentHistory;
	}
}