package com.tierconnect.riot.migration.steps.connection;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.popdb.PopDBRequired;
import com.tierconnect.riot.appcore.popdb.PopDBSpark;
import com.tierconnect.riot.appcore.services.ConnectionTypeService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

/**
 * Created by ruth on 24-01-17.
 */
public class Migrate_MigrateConnectionType_RIOT12980 implements MigrationStep {
    static Logger logger = Logger.getLogger(Migrate_MigrateConnectionType_RIOT12980.class);
    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {

    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateConnectiontype();
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {

    }

    /**
     * create connexion (SQL, KAFKA, SERVICE, SPARK) if not exist
     */
    private void migrateConnectiontype() {
        GroupService groupService = GroupService.getInstance();
        Group rootGroup = groupService.getRootGroup();
        ConnectionTypeService connectionTypeService = ConnectionTypeService.getInstance();
        if (connectionTypeService.getConnectionTypeByCode("SPARK") == null) {
            logger.info("create connection type SPARK");
            PopDBSpark.populateSparkConnection(rootGroup);
        }
        if (connectionTypeService.getConnectionTypeByCode("KAFKA") == null) {
            logger.info("create connection type KAFKA");
            PopDBSpark.populateKAFKAConnection(rootGroup);
        }
        if (connectionTypeService.getConnectionTypeByCode("SQL") == null) {
            logger.info("create connection type SQL");
            PopDBRequired.populateSQLConnection0(rootGroup);
        }
        if (connectionTypeService.getConnectionTypeByCode("HADOOP") == null) {
            logger.info("create connection type HADOOP");
            PopDBSpark.populateHadoopConnection(rootGroup);
        }
    }
}
