package com.tierconnect.riot.migration.older;

import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.services.ThingService;
import com.tierconnect.riot.iot.services.ThingTypeService;
import com.tierconnect.riot.migration.DBHelper;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by cfernandez
 * 5/28/15.
 */
@Deprecated
public class V_020301_020302 implements MigrationStepOld
{

    private Logger logger = Logger.getLogger(V_020301_020302.class);

    @Override
    public List<Integer> getFromVersions() {
        return Arrays.asList(20301);
    }

    @Override
    public int getToVersion() {
        return 20302;
    }

    @Override
    public void migrateSQLBefore() throws Exception {
        DBHelper dbHelper = new DBHelper();
        String databaseType = dbHelper.getDataBaseType();
        dbHelper.executeSQLFile("sql/" + databaseType + "/V020301_to_020302.sql");
    }

    @Override
    public void migrateHibernate() throws Exception {
//        migrateThingTypes();
//        migrateThings();
    }



    @Override
    public void migrateSQLAfter() throws Exception {

    }

    private void migrateThingTypes()
    {
        logger.info("Start migrating ThingTypes....");
        ThingTypeService thingTypeService = ThingTypeService.getInstance();
        List<ThingType> thingTypes = thingTypeService.getAllThingTypes();
        logger.info("Total ThingTypes="+thingTypes.size());
        Date date = new Date();
        for (ThingType thingType : thingTypes) {
            thingType.setModifiedTime(date.getTime());
            thingTypeService.update(thingType);
        }
        logger.info("End migrating ThingTypes....");
    }

    private void migrateThings()
    {
        logger.info("Start migrating Things....");
        ThingService thingService = ThingService.getInstance();
        thingService.updateModifiedTime();
        logger.info("End migrating Things....");
    }

}
