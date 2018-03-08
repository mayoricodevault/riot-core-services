package com.tierconnect.riot.migration.steps;

import com.tierconnect.riot.migration.DBHelper;
import org.apache.log4j.Logger;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_MigrationStepTemplate_VIZIX000 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_MigrationStepTemplate_VIZIX000.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateFeature();
    }

    private void migrateFeature() {
        //TODO rename this function and put your code here
    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
