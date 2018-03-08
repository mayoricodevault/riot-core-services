package com.tierconnect.riot.migration.steps.group;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_DwellTimeFlag_RIOT6340 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_DwellTimeFlag_RIOT6340.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateFields();
    }

    private void migrateFields()
    {
        GroupService groupService = GroupService.getInstance();
        Group rootGroup = groupService.getRootGroup();
        PopDBUtils.migrateFieldService("saveDwellTimeHistory", "saveDwellTimeHistory", "Save DwellTime History", rootGroup, "Data Storage Configuration", "java.lang.Boolean", 3L, true, "true");
        PopDBUtils.migrateFieldService("cutoffTimeseries", "cutoffTimeseries", "Max size of snapshots ids", rootGroup, "Data Storage Configuration", "java.lang.Long", 3L, true, "400000");
    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
