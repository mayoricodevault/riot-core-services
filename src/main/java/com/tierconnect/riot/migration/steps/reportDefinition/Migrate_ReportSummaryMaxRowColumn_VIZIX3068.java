package com.tierconnect.riot.migration.steps.reportDefinition;

import com.tierconnect.riot.appcore.entities.Field;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.migration.steps.MigrationStep;

/**
 * Created by vealaro on 3/28/17.
 */
public class Migrate_ReportSummaryMaxRowColumn_VIZIX3068 implements MigrationStep {
    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {

    }

    @Override
    public void migrateHibernate() throws Exception {
        addNewFieldReports();
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {

    }

    private void addNewFieldReports() {
        Group rootGroup = GroupService.getInstance().getRootGroup();
        Field maxColumns = PopDBUtils.popFieldService("max_number_of_columns", "max_number_of_columns",
                "Max Table Summary Columns", rootGroup, "Reports", "java.lang.Long", null, true);
        PopDBUtils.popGroupField(rootGroup, maxColumns, "50");

        Field maxRows = PopDBUtils.popFieldService("max_number_of_rows", "max_number_of_rows",
                "Max Table Summary Rows", rootGroup, "Reports", "java.lang.Long", null, true);
        PopDBUtils.popGroupField(rootGroup, maxRows, "1000");
    }
}
