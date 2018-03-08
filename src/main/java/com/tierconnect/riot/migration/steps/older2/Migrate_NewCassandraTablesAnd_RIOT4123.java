package com.tierconnect.riot.migration.steps.older2;

import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_NewCassandraTablesAnd_RIOT4123 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_NewCassandraTablesAnd_RIOT4123.class);

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
