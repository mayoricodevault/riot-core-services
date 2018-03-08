package com.tierconnect.riot.migration.steps.thingType;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.tierconnect.riot.commons.dao.mongo.MongoDAOUtil;
import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.entities.ThingTypeField;
import com.tierconnect.riot.iot.popdb.PopDBIOTUtils;
import com.tierconnect.riot.iot.services.ThingTypeFieldService;
import com.tierconnect.riot.iot.services.ThingTypeService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

import java.util.Date;
import java.util.List;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_LastDetectLocateTime_RIOT7493 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_LastDetectLocateTime_RIOT7493.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateFeature();
    }

    /*Update Rfid Encode*/
    private void migrateFeature() {
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
