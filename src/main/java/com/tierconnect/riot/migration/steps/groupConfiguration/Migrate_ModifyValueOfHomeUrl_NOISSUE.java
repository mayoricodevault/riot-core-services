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
public class Migrate_ModifyValueOfHomeUrl_NOISSUE implements MigrationStep{
    private static Logger logger = Logger.getLogger(Migrate_ModifyValueOfHomeUrl_NOISSUE.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateAutoLoadFields();
    }

    private void migrateAutoLoadFields() {
        GroupService groupService = GroupService.getInstance();
        Group rootGroup = groupService.getRootGroup();
        PopDBUtils.migrateFieldService("homeUrl", "homeUrl", "Home URL",
                rootGroup, "Home Configuration", "java.lang.String", 1L, true, "home.mojix.com");
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }
}
