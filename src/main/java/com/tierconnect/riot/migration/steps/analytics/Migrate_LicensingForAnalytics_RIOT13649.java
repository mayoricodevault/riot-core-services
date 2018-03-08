package com.tierconnect.riot.migration.steps.analytics;

import com.tierconnect.riot.iot.popdb.PopDBML;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_LicensingForAnalytics_RIOT13649 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_LicensingForAnalytics_RIOT13649.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateLicensingForAnalytics();
    }

    private void migrateLicensingForAnalytics() {
        PopDBML popDBML = new PopDBML();
        popDBML.addResources();
    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
