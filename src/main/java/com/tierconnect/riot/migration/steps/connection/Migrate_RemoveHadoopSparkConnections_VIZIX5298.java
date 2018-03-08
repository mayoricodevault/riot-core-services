package com.tierconnect.riot.migration.steps.connection;

import com.tierconnect.riot.appcore.entities.Connection;
import com.tierconnect.riot.appcore.entities.ConnectionType;
import com.tierconnect.riot.appcore.entities.QConnection;
import com.tierconnect.riot.appcore.services.ConnectionService;
import com.tierconnect.riot.appcore.services.ConnectionTypeService;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

/**
 * Created by dbascope
 * on 07/26/2017
 */

public class Migrate_RemoveHadoopSparkConnections_VIZIX5298 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_RemoveHadoopSparkConnections_VIZIX5298.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
    }

    @Override
    public void migrateHibernate() throws Exception {
        removeConnections("HADOOP");
        removeConnections("SPARK");
    }

    private void removeConnections(String code) {
        ConnectionType connectionType = ConnectionTypeService.getInstance().getConnectionTypeByCode(code);
        if (connectionType != null) {
            for (Connection connection : ConnectionService.getConnectionDAO().selectAllBy(QConnection.connection.connectionType.eq(connectionType))) {
                ConnectionService.getInstance().delete(connection);
            }
            ConnectionTypeService.getInstance().delete(connectionType);
        }
    }

    @Override
    public void migrateSQLAfter(String scriptPath)
    throws Exception {
    }
}
