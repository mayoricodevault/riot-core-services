package com.tierconnect.riot.migration.steps.reportDefinition;

import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;

/**
 * Created by julio.rocha on 18-05-17.
 */
public class Migrate_DismissIndexAnalysisSchedule_VIZIX4837 implements MigrationStep {
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
