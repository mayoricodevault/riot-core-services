package com.tierconnect.riot.migration.steps.reportDefinition;

import com.tierconnect.riot.iot.entities.ReportEntryOptionProperty;
import com.tierconnect.riot.iot.services.ReportEntryOptionPropertyService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_MigrateReportEntryOptionProp_RIOT12671 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_MigrateReportEntryOptionProp_RIOT12671.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateReportEntryOptionProperty();
    }

    /**
     * Update data in reportEntryOptionProperty
     */
    public void migrateReportEntryOptionProperty() {
        List<ReportEntryOptionProperty> lstReportEntryOptionProperty = ReportEntryOptionPropertyService.getInstance()
                .getAllReportEntryOptionProperties();
        for (ReportEntryOptionProperty reportEntryOption : lstReportEntryOptionProperty) {
            reportEntryOption.setDefaultMobileValue(reportEntryOption.getSortBy());
            reportEntryOption.setSortBy(null);
            ReportEntryOptionPropertyService.getInstance().update(reportEntryOption);
        }
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
