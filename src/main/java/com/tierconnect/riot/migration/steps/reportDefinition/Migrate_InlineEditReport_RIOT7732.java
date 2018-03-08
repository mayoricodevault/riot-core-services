package com.tierconnect.riot.migration.steps.reportDefinition;

import com.tierconnect.riot.iot.entities.ReportProperty;
import com.tierconnect.riot.iot.services.ReportPropertyService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_InlineEditReport_RIOT7732 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_InlineEditReport_RIOT7732.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateInlineEdit();
    }

    private void migrateInlineEdit(){

        for(ReportProperty reportProperty : ReportPropertyService.getReportPropertyDAO().selectAll()){
            reportProperty.setEditInline(false);
            ReportPropertyService.getReportPropertyDAO().update(reportProperty);
        }
    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
