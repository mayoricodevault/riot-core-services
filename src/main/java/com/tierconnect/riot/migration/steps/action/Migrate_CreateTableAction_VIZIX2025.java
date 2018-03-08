package com.tierconnect.riot.migration.steps.action;

import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

/**
 * Created by vealaro on 4/11/17.
 */
public class Migrate_CreateTableAction_VIZIX2025 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_CreateTableAction_VIZIX2025.class);

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
