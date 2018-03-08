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
public class Migrate_ShowHover_RIOT7589 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_ShowHover_RIOT7589.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateReportProperties();
    }

    private void migrateReportProperties(){
        for (ReportProperty reportProperty : ReportPropertyService.getReportPropertyDAO().selectAll()){
            reportProperty.setShowHover(false);
            ReportPropertyService.getReportPropertyDAO().update(reportProperty);
        }
    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
