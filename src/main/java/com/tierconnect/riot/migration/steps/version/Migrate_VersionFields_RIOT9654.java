package com.tierconnect.riot.migration.steps.version;

import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.Migrate_MigrationStepTemplate_VIZIX000;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

/**
 * Created by achambi on 1/16/17.
 */
public class Migrate_VersionFields_RIOT9654 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_MigrationStepTemplate_VIZIX000.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {

    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }
}
