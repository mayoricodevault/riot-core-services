package com.tierconnect.riot.migration.older;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.migration.DBHelper;

import java.util.Arrays;
import java.util.List;

/**
 * Created by cfernandez
 * on 9/25/15.
 */
@Deprecated
public class V_030101_030102 implements MigrationStepOld
{

    @Override
    public List<Integer> getFromVersions() {
        return Arrays.asList(30101);
    }

    @Override
    public int getToVersion() {
        return 30102;
    }

    @Override
    public void migrateSQLBefore() throws Exception {
        DBHelper dbHelper = new DBHelper();
        String databaseType = dbHelper.getDataBaseType();
        dbHelper.executeSQLFile("sql/" + databaseType + "/V030101_to_030102.sql");
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateFields();
    }

    @Override
    public void migrateSQLAfter() throws Exception {

    }

    private void migrateFields()
    {
        GroupService groupService = GroupService.getInstance();
        Group rootGroup = groupService.getRootGroup();
//        PopDBUtils.migrateFieldService("saveDwellTimeHistory", "saveDwellTimeHistory", "Save DwellTime History", rootGroup, "Data Storage Configuration", "java.lang.Boolean", 3L, true, "true");
        PopDBUtils.migrateFieldService("cutoffTimeseries", "cutoffTimeseries", "Max size of snapshots ids", rootGroup, "Data Storage Configuration", "java.lang.Long", 3L, true, "400000");
    }
}
