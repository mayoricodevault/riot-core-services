package com.tierconnect.riot.migration.steps.edgebox;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.iot.popdb.PopDBRequiredIOT;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_StarFlexThingTypeTemplate_RIOT11467 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_StarFlexThingTypeTemplate_RIOT11467.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        Group rootGroup = GroupService.getInstance().getRootGroup();
        PopDBRequiredIOT.populateStartFlexThingTypeOldTemplate(rootGroup);
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
