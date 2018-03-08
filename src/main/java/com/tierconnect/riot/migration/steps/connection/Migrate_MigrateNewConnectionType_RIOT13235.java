package com.tierconnect.riot.migration.steps.connection;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.popdb.PopDBRequired;
import com.tierconnect.riot.appcore.services.ConnectionTypeService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_MigrateNewConnectionType_RIOT13235 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_MigrateNewConnectionType_RIOT13235.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateNewConnectiontype();
    }

    /**
     * create connexion (SQL, KAFKA, SERVICE, HADOOP) if not exist
     */
    private void migrateNewConnectiontype() {
        GroupService groupService = GroupService.getInstance();
        Group rootGroup = groupService.getRootGroup();
        ConnectionTypeService connectionTypeService = ConnectionTypeService.getInstance();
        if (connectionTypeService.getConnectionTypeByCode("SQL") == null) {
            PopDBRequired.populateSQLConnection0(rootGroup);
            logger.info("create connection type SQL");
        }
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

}
