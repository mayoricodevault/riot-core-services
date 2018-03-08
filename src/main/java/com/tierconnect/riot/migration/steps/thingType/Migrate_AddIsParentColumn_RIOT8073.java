package com.tierconnect.riot.migration.steps.thingType;

import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_AddIsParentColumn_RIOT8073 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_AddIsParentColumn_RIOT8073.class);

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
