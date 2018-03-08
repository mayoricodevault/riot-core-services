package com.tierconnect.riot.migration.steps.reportDefinition;

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
public class Migrate_PlaybackMaxThing_RIOT6515 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_PlaybackMaxThing_RIOT6515.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateFeature();
    }

    private void migrateFeature(){
        Group rootGroup = GroupService.getInstance().getRootGroup();
        Field f34 = PopDBUtils.popFieldService("playbackMaxThings", "playbackMaxThings", "Playback Max Things", rootGroup, "Look & Feel", "java.lang.Integer", 2L, true);
        PopDBUtils.popGroupField(rootGroup, f34, "100");
    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
