package com.tierconnect.riot.migration.steps.edgebox;

import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;

/**
 * Created by julio.rocha on 16-05-17.
 */
public class Migrate_CreateSlowness_VIZIX4715 implements MigrationStep {
    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {

    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {

    }
}
