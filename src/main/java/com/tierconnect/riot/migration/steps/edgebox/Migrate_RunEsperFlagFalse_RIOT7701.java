package com.tierconnect.riot.migration.steps.edgebox;

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
public class Migrate_RunEsperFlagFalse_RIOT7701 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_RunEsperFlagFalse_RIOT7701.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateGroupFields();
    }

    private void migrateGroupFields()
    {
        GroupService groupService = GroupService.getInstance();
        Group rootGroup = groupService.getRootGroup();
        PopDBUtils.migrateFieldService("sendThingFieldTickle", "sendThingFieldTickle", "Run Rules After Import", rootGroup, "Import Configuration", "java.lang.Boolean", 3L, true, "false");
    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
