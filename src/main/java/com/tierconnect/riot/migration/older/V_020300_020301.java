package com.tierconnect.riot.migration.older;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.migration.DBHelper;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.List;

/**
 * Created by agutierrez on 4/22/15.
 */
@Deprecated
public class V_020300_020301 implements MigrationStepOld {

    private Logger logger = Logger.getLogger(V_020300_020301.class);

    @Override
    public List<Integer> getFromVersions() {
        return Arrays.asList(20300);
    }

    @Override
    public int getToVersion() {
        return 20301;
    }

    @Override
    public void migrateSQLBefore() throws Exception {
        DBHelper dbHelper = new DBHelper();
        String databaseType = dbHelper.getDataBaseType();
        dbHelper.executeSQLFile("sql/" + databaseType + "/V020300_to_020301.sql");
    }

    @Override
    public void migrateHibernate() throws Exception {

        GroupService groupService = GroupService.getInstance();
        Group rootGroup = groupService.getRootGroup();
        PopDBUtils.migrateFieldService("Report Time Out Cache", "Report Time Out Cache", "Report Time Out Cache", rootGroup, "Look & Feel", "java.lang.Integer", 1L, true,
                "15000");
    }

    @Override
    public void migrateSQLAfter() throws Exception {

    }

}
