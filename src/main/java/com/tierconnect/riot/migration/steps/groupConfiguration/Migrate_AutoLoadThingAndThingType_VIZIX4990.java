package com.tierconnect.riot.migration.steps.groupConfiguration;

import com.tierconnect.riot.appcore.entities.Field;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.GroupField;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;
import com.tierconnect.riot.appcore.services.FieldService;
import com.tierconnect.riot.appcore.services.GroupFieldService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_AutoLoadThingAndThingType_VIZIX4990 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_StopAlertsChangeName_VIZIX3951.class);

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
        PopDBUtils.migrateFieldService("autoLoadThingList", "autoLoadThingList", "Auto-Load Thing List",
                rootGroup, "Look & Feel", "java.lang.Boolean", 3L, true, "false");
        PopDBUtils.migrateFieldService("autoLoadThingTypeList", "autoLoadThingTypeList", "Auto-Load Thing Type List",
                rootGroup, "Look & Feel", "java.lang.Boolean", 3L, true, "false");
        PopDBUtils.migrateFieldService("homeUrl", "homeUrl", "Home URL",
                rootGroup, "Home Configuration", "java.lang.String", 1L, true, "http://home.mojix.com/5-0-0");
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }
}


