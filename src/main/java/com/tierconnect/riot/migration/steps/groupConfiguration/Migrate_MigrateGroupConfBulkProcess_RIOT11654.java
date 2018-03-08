package com.tierconnect.riot.migration.steps.groupConfiguration;

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
public class Migrate_MigrateGroupConfBulkProcess_RIOT11654 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_MigrateGroupConfBulkProcess_RIOT11654.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateGroupConfigurationForBulkProcess();
    }

    /**
     * Migrate group configuration for bulk process
     */
    public static void migrateGroupConfigurationForBulkProcess() {
        GroupService groupService = GroupService.getInstance();
        Group rootGroup = groupService.getRootGroup();
        PopDBUtils.migrateFieldService("notification_bulkProcess", "notification_bulkProcess", "Notification Bulk " +
                "Process " +
                "(secs)", rootGroup, "Reports", "java.lang.Integer", null, true, "15");
        PopDBUtils.migrateFieldService("reloadAllThingsThreshold_bulkProcess",
                "reloadAllThingsThreshold_bulkProcess", "Things Cache " +
                "Reload Threshold", rootGroup, "Reports", "jjava.lang.Longr", 3L, true, "1000");
        PopDBUtils.migrateFieldService("sendThingFieldTickle_bulkProcess", "sendThingFieldTickle_bulkProcess", "Run " +
                "Rules After " +
                "Bulk Process", rootGroup, "Reports", "java.lang.Boolean", 3L, true, "false");
        PopDBUtils.migrateFieldService("fmcSapEnableSapSync_bulkProcess", "fmcSapEnableSapSync_bulkProcess", "Enable " +
                "SAP Sync on " +
                "Bulk Process", rootGroup, "Reports", "java.lang.Boolean", 2L, false, "false");
    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
