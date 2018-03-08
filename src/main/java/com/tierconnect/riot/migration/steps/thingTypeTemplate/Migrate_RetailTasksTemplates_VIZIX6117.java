package com.tierconnect.riot.migration.steps.thingTypeTemplate;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.popdb.PopDBRequiredIOT;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;

import static com.tierconnect.riot.commons.Constants.*;

/**
 * Created by dbascope on 07/07/2017
 */
public class Migrate_RetailTasksTemplates_VIZIX6117 implements MigrationStep {

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateThingTypeTemplate();
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    private void migrateThingTypeTemplate() {

        Group group = GroupService.getInstance().getRootGroup();
        PopDBRequiredIOT.populateMojixRetailAppThingTypeTemplateBase(group);
        PopDBRequiredIOT.populateMojixRetailAppThingTypeTemplateSync(group);
        PopDBRequiredIOT.populateMojixRetailAppThingTypeTemplateConfig(group);

    }
}
