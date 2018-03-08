package com.tierconnect.riot.migration.steps.connection;

import com.tierconnect.riot.appcore.entities.Connection;
import com.tierconnect.riot.appcore.entities.ConnectionType;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.services.ConnectionService;
import com.tierconnect.riot.appcore.services.ConnectionTypeService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

/**
 * Class used to verify if there is an instance of blockchain connection that was
 * populated in other migrations, this instance should not be present in the migration;
 * therefore, it is going to be removed if an instance is found.
 */
public class Migrate_RestConnection_VIZIX5137 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_RestConnection_VIZIX5137.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateConnection();
    }

    private void migrateConnection() {
        Group rootGroup = GroupService.getInstance().getRootGroup();
        ConnectionService connectionService = ConnectionService.getInstance();

        Connection connection = null;

        // We look for an connection whose code is BlockchainAdapter
        connection = connectionService.getByCodeAndGroup("BlockchainAdapter", rootGroup);

        if(connection != null) {
            // We remove the connection only if there is a row
            connectionService.delete(connection);
        }

        ConnectionTypeService connectionTypeService = ConnectionTypeService.getInstance();

        // Anyway, we verify if there is the rest connection type because this data is needed
        ConnectionType restConnectionType = null;
        restConnectionType = connectionTypeService.getConnectionTypeByCode("REST");

        if(restConnectionType == null) { // We insert only if another row does not exist
            restConnectionType = new ConnectionType();

            restConnectionType.setCode("REST");
            restConnectionType.setGroup(rootGroup);
            restConnectionType.setDescription("REST Connection");
            restConnectionType.setPropertiesDefinitions("[{\"code\":\"host\",\"label\":\"Host\",\"type\":\"String\"}," +
                    "{\"code\":\"port\",\"label\":\"Port\",\"type\":\"Number\",\"port\":\"Number\"}," +
                    "{\"code\":\"contextpath\",\"label\":\"Contextpath\",\"type\":\"String\"}," +
                    "{\"code\":\"apikey\",\"label\":\"Apikey\",\"type\":\"String\"}," +
                    "{\"code\":\"secure\",\"label\":\"Secure\",\"type\":\"Boolean\"}]");

            ConnectionTypeService.getInstance().insert(restConnectionType);
        }
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
    }
}