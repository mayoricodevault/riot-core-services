package com.tierconnect.riot.migration.steps.performance;

import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
@SuppressWarnings("unused")
public class Migrate_CreateIndexesDB_NOISSUE implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_CreateIndexesDB_NOISSUE.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        logger.info("Migration only run SQL scripts.");
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
