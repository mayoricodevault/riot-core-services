package com.tierconnect.riot.migration.older;

import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.iot.dao.mongo.ThingMongoDAO;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.migration.DBHelper;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Created by rchirinos
 * on 12/28/15.
 */
@Deprecated
public class V_030400_030402 implements MigrationStepOld
{

	static Logger logger = Logger.getLogger(V_030400_030402.class);

	@Override
	public List<Integer> getFromVersions() {
		return Arrays.asList(30400);

	}

	@Override
	public int getToVersion() {
		return 30402;
	}

	@Override
	public void migrateSQLBefore() throws Exception {
		DBHelper dbHelper = new DBHelper();
		String databaseType = dbHelper.getDataBaseType();
		dbHelper.executeSQLFile("sql/" + databaseType + "/V030400_to_030402.sql");
	}

	@Override
	public void migrateHibernate() throws Exception {
        //migrateMongoIndexes();
        migrateField();
//		migrateThingTypeIsParent();
        migrateNativeObjects();
//        migrateThingMongo();
	}

	@Override
	public void migrateSQLAfter() throws Exception {

	}

//	public void migrateThingTypeIsParent()
//	{
//		List<ThingType> lstThingType = ThingTypeService.getInstance().getAllThingTypes();
//		for(ThingType thingTypeData : lstThingType)
//		{
//			if(this.isParentUdf( thingTypeData.getId() ))
//			{
//				thingTypeData.setIsParent( true );
//				ThingTypeService.getInstance().update( thingTypeData );
//			}
//		}
//	}

    private void migrateField() {
        Field f33 = PopDBUtils.popFieldService( "executeRulesForLastDetectTime", "executeRulesForLastDetectTime", "execute CEP rules when only lastDetectTime is sent", GroupService.getInstance().getRootGroup(), "Data Storage Configuration", "java.lang.Boolean", 3L, true);
        PopDBUtils.popGroupField(GroupService.getInstance().getRootGroup(), f33, "true");
    }

	/**********************************************************
	 * Check if the ThingType is ParentUdf
	 ***********************************************************/
	public boolean isParentUdf(Long thingTypeParentId){
		boolean response = false;
		List<ThingType> lstThingtypes = ThingTypeService.getInstance().getAllThingTypes();
		if(lstThingtypes != null && lstThingtypes.size() > 0){
			for(ThingType thingType : lstThingtypes){
				List<ThingTypeField> lstThingTypeField = ThingTypeFieldService.getInstance()
																			  .getThingTypeField(thingType.getId());
				if(lstThingTypeField != null && lstThingTypeField.size() > 0){
					for(ThingTypeField thingTypeField : lstThingTypeField){
						if(thingTypeField.getDataTypeThingTypeId() != null
						   && thingTypeField.getDataTypeThingTypeId().compareTo(thingTypeParentId) == 0){
							response = true;
							break;
						}
					}
				}
			}
		}
		return response;
	}

    public void migrateNativeObjects(){

        for(Shift shift : ShiftService.getShiftDAO().selectAll()){
            ShiftService.getInstance().update(shift);
        }
        for(Group group : GroupService.getGroupDAO().selectAll()){
            GroupService.getInstance().update(group);
        }
    }

    private void migrateThingMongo() {

		List<Thing> things = ThingService.getThingDAO().selectAll();
		int counter = 0, lastPercentLogged = 0, percent = 0;
		if(things != null && things.size() > 0){
			int totalThings=things.size();
			for(Thing thing : things){
				if(ThingMongoDAO.getInstance().getThing(thing.getId()) != null){
					//Associate parent
					Thing thingParent = thing.getParent();
					if(thingParent != null){
						ThingMongoService.getInstance().associateChild(thingParent, thing.getId());
					}
//            //Associate children
//            Object childrenList = thing.getChildren();
//            if(childrenList != null){
//                List<Long> childrenIds = new ArrayList<>();
//                for(Thing child : (List<Thing>) childrenList){
//                    childrenIds.add(child.getId());
//                }
//                ThingMongoDAO.getInstance().associateChildren(thing, childrenIds);
//            }

					percent = (counter * 100 / totalThings);
					if (percent != lastPercentLogged) {
						logger.info("Denormalization progress: " + percent + "% (" + counter + " things of " + totalThings + ")");
						lastPercentLogged = percent;
					}
				}
			}
		}
    }

//    private void migrateMongoIndexes()
//	{
//		logger.info("Migrating mongo \"parent\" to \"parent._id\" index from thing collection");
//		try {
//
//			MongoDAOUtil.getInstance().things.dropIndex(new BasicDBObject("parent", 1));
//		}catch(Exception e)
//		{
//			logger.info("migrateMongoIndexes: "+ e.getMessage());
//		}
//		BasicDBObject parentIdx = new BasicDBObject("parent._id", 1);
//		MongoDAOUtil.getInstance().thingSnapshots.createIndex(parentIdx, "parent._id_");
//    }

}
