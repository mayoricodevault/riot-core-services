package com.tierconnect.riot.migration.steps.reportDefinition;

import com.tierconnect.riot.iot.services.ReportDefinitionService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_ReportTypeOrder_VIZIX3811 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_ReportTypeOrder_VIZIX3811.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateFeature();
    }

    private void migrateFeature() {
        ReportDefinitionService.getInstance().updateAllReports();
    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
