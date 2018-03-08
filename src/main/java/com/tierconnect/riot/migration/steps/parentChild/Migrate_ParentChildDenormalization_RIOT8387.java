package com.tierconnect.riot.migration.steps.parentChild;

import com.tierconnect.riot.iot.dao.mongo.ThingMongoDAO;
import com.tierconnect.riot.iot.entities.Thing;
import com.tierconnect.riot.iot.services.ThingMongoService;
import com.tierconnect.riot.iot.services.ThingService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_ParentChildDenormalization_RIOT8387 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_ParentChildDenormalization_RIOT8387.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        //Commented because it was moved to manuallly migration step
        //        migrateThingMongo();
    }

    /*Update Rfid Encode*/
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

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
