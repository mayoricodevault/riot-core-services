package com.tierconnect.riot.migration.steps.edgebox;

import com.tierconnect.riot.appcore.entities.Field;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

/**
 * Created by fflores
 * on 11/17/15.
 */
public class Migrate_StartStopBridges_VIZIX1604 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_StartStopBridges_VIZIX1604.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateFeature();
    }

    private void migrateFeature() {
        //Alerting and Notification fields
        Group rootGroup = GroupService.getInstance().getRootGroup();
        Field field1 = PopDBUtils.popFieldService("bridgeStatusUpdateInterval", "bridgeStatusUpdateInterval",
                "Bridge Status Update Interval (ms)", rootGroup, "Alerting & Notification", "java.lang.Long", 1L, true);
        PopDBUtils.popGroupField(rootGroup, field1, "10000");

        Field field2 = PopDBUtils.popFieldService("bridgeErrorStatusTimeout", "bridgeErrorStatusTimeout", "Bridge Error Status Timeout (ms)",
                rootGroup, "Alerting & Notification", "java.lang.Long", 1L, true);
        PopDBUtils.popGroupField(rootGroup, field2, "30000");
    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
