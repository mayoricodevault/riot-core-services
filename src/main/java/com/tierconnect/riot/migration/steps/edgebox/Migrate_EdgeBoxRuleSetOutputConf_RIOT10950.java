package com.tierconnect.riot.migration.steps.edgebox;

import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_EdgeBoxRuleSetOutputConf_RIOT10950 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_EdgeBoxRuleSetOutputConf_RIOT10950.class);

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