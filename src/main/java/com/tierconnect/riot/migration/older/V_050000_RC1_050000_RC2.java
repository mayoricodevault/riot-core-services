package com.tierconnect.riot.migration.older;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.migration.DBHelper;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.List;

/**
 * Created by rsejas on 1/17/17.
 */
@Deprecated
public class V_050000_RC1_050000_RC2 implements MigrationStepOld {
    private static Logger logger = Logger.getLogger(V_050000_RC1_050000_RC2.class);

    @Override
    public List<Integer> getFromVersions() {
        return Collections.singletonList(5000001);
    }

    @Override
    public int getToVersion() {
        return 5000002;
    }

    @Override
    public void migrateSQLBefore() throws Exception {
        logger.info("Migrating from: " + getFromVersions() + " To: " + getToVersion());
        DBHelper dbHelper = new DBHelper();
        String databaseType = DBHelper.getDataBaseType();
        if(!dbHelper.existTable("reportbulkprocessdetaillog")) {
            dbHelper.executeSQLFile("sql/" + databaseType + "/V050000_RC1_to_050000_RC2.sql");
        }
    }

    @Override
    public void migrateHibernate() throws Exception {
        addBatchSizeForBulkProcess();
    }

    @Override
    public void migrateSQLAfter() throws Exception {

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
