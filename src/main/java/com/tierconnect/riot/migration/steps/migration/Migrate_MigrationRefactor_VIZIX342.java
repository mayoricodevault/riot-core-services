package com.tierconnect.riot.migration.steps.migration;

import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_MigrationRefactor_VIZIX342 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_MigrationRefactor_VIZIX342.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateRefactorMigration();
    }

    private void migrateRefactorMigration() {
//        int i = 10 / 0;
    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
