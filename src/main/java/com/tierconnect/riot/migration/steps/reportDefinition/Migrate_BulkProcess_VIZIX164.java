package com.tierconnect.riot.migration.steps.reportDefinition;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;

/**
 * Created by IntelliJ IDEA.
 *
 * @author : rchirinos
 * @date : 1/19/17 1:36 PM
 * @version:
 */
public class Migrate_BulkProcess_VIZIX164 implements MigrationStep {
    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        addBatchSizeForBulkProcess();
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {

    }

    /**
     * Adds a configuration field for batch size used in Bulk process
     * */
    public void addBatchSizeForBulkProcess() {
        GroupService groupService = GroupService.getInstance();
        Group rootGroup = groupService.getRootGroup();
        PopDBUtils.migrateFieldService("batchSize_bulkProcess", "batchSize_bulkProcess", "Batch Size Bulk Process",
                rootGroup, "Reports", "java.lang.Long", null, true, "10000");
    }
}
