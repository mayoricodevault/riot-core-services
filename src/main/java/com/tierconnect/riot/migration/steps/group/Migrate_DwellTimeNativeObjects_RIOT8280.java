package com.tierconnect.riot.migration.steps.group;

import com.tierconnect.riot.appcore.entities.Field;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.iot.entities.Shift;
import com.tierconnect.riot.iot.services.ShiftService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_DwellTimeNativeObjects_RIOT8280 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_DwellTimeNativeObjects_RIOT8280.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateField();
        migrateNativeObjects();
    }

    private void migrateField() {
        Field f33 = PopDBUtils.popFieldService( "executeRulesForLastDetectTime", "executeRulesForLastDetectTime", "execute CEP rules when only lastDetectTime is sent", GroupService.getInstance().getRootGroup(), "Data Storage Configuration", "java.lang.Boolean", 3L, true);
        PopDBUtils.popGroupField(GroupService.getInstance().getRootGroup(), f33, "true");
    }

    private void migrateNativeObjects(){

        for(Shift shift : ShiftService.getShiftDAO().selectAll()){
            ShiftService.getInstance().update(shift);
        }
        for(Group group : GroupService.getGroupDAO().selectAll()){
            GroupService.getInstance().update(group);
        }
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
