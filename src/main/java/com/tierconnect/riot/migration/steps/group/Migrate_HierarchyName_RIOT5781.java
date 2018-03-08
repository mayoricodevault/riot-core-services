package com.tierconnect.riot.migration.steps.group;

import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_HierarchyName_RIOT5781 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_HierarchyName_RIOT5781.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateHierarchyName();
    }

    private void migrateHierarchyName() {
        GroupService.getInstance().refreshHierarchyName();
    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
