package com.tierconnect.riot.migration.steps.group;

import com.tierconnect.riot.appcore.entities.Field;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

/**
 * Created by rchirinos
 * on 07/03/2017
 */
public class Migrate_MigrateRegionalSettings_VIZIX2475 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_MigrateRegionalSettings_VIZIX2475.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateTimezone();
    }

    /**
     * Migrate Time Zone
     */
    private static void migrateTimezone() {
        Group rootGroup = GroupService.getInstance().getRootGroup();
        Field field = PopDBUtils.popFieldService(Constants.TIME_ZONE_CONFIG, Constants.TIME_ZONE_CONFIG, "Time Zone",
                rootGroup, "Regional Settings", "java.lang.String", 3L, true);
        PopDBUtils.popGroupField(rootGroup, field, Constants.DEFAULT_TIME_ZONE);
        Field fieldDate = PopDBUtils.popFieldService(Constants.DATE_FORMAT_CONFIG, Constants.DATE_FORMAT_CONFIG, "Date Format",
                rootGroup, "Regional Settings", "java.lang.String", 3L, true);
        PopDBUtils.popGroupField(rootGroup, fieldDate, Constants.DEFAULT_DATE_FORMAT);
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {

    }


}
