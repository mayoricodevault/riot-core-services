package com.tierconnect.riot.migration.steps.reportDefinition;

import com.tierconnect.riot.appcore.entities.Field;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;

/**
 * Created by julio.rocha on 08-06-17.
 */
public class Migrate_TableConnectionReport_VIZIX4549 implements MigrationStep {
    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
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
        Field maxColumns = PopDBUtils.popFieldService("max_number_of_table_columns", "max_number_of_table_columns",
                "Max Table Columns", rootGroup, "Reports", "java.lang.Long", null, true);
        PopDBUtils.popGroupField(rootGroup, maxColumns, "50");

    }
}
