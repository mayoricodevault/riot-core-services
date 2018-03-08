package com.tierconnect.riot.migration.steps.groupConfiguration;

import com.tierconnect.riot.appcore.entities.Field;
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
public class Migrate_GroupConfigurationTreeView_RIOT13279 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_GroupConfigurationTreeView_RIOT13279.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateGroupConfiguration();
    }

    public void migrateGroupConfiguration(){
        GroupService groupService = GroupService.getInstance();
        Group rootGroup = groupService.getRootGroup();
        Field field = PopDBUtils.popFieldService("thingListInTreeView", "thingListInTreeView", "Thing List In Tree View", rootGroup, "Look & Feel",
                "java.lang.Boolean", null, true);
        PopDBUtils.popGroupField(rootGroup, field, "false");
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
