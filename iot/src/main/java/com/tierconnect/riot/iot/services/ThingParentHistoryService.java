package com.tierconnect.riot.iot.services;

import com.tierconnect.riot.iot.dao.ThingParentHistoryDAO;
import com.tierconnect.riot.iot.entities.QThingParentHistory;
import com.tierconnect.riot.iot.entities.Thing;
import com.tierconnect.riot.iot.entities.ThingParentHistory;

public class ThingParentHistoryService extends ThingParentHistoryServiceBase {
	private static ThingParentHistoryDAO _thingParentHistoryDAO;
    public static ThingParentHistoryDAO getThingParentHistoryDAO() {
        if (_thingParentHistoryDAO == null) {
        	_thingParentHistoryDAO = new ThingParentHistoryDAO();
        }
        return _thingParentHistoryDAO;
    }
    
    public static ThingParentHistory selectActiveByParentAndChild(Thing parent,Thing child){
    	QThingParentHistory qThingParentHistory = QThingParentHistory.thingParentHistory;
		getThingParentHistoryDAO().getQuery().where(qThingParentHistory.parent.eq(parent).and(qThingParentHistory.child.eq(child)).and(qThingParentHistory.endDate.isNull()));
    	return null;
    }
    
    public ThingParentHistory insert(ThingParentHistory thingParentHistory){
    	Long id = getThingParentHistoryDAO().insert(thingParentHistory);
    	thingParentHistory.setId(id);    	
    	return thingParentHistory;
    }
    
    public ThingParentHistory update(ThingParentHistory thingParentHistory){
    	getThingParentHistoryDAO().update(thingParentHistory);
		return thingParentHistory;    	
    	
    }
}
