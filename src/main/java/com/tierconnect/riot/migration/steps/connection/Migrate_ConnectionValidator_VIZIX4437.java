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
 * on 08/03/2017
 */

public class Migrate_ConnectionValidator_VIZIX4437 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_ConnectionValidator_VIZIX4437.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
    }

    @Override
    public void migrateHibernate() throws Exception {
        for (ConnectionType connectionType : ConnectionTypeService.getConnectionTypeDAO().selectAll()) {
            connectionType.setRequiredTestOnCreateEdit(false);
            ConnectionTypeService.getInstance().update(connectionType);
            logger.info("ConnectionType [" + connectionType.getCode() + "] requiredTest updated to false.");
        }

        ConnectionType servicesConnectionType = ConnectionTypeService.getInstance().getConnectionTypeByCode("SERVICES");
        ConnectionType restConnectionType = ConnectionTypeService.getInstance().getConnectionTypeByCode("REST");
        if (servicesConnectionType != null) {
            for (Connection connection : ConnectionService.getConnectionDAO().selectAllBy(QConnection.connection.connectionType.eq(servicesConnectionType))) {
                connection.setConnectionType(restConnectionType);
                ConnectionService.getInstance().update(connection);
                logger.info("Connection [" + connection.getCode() + "] changed to [REST] ConnectionType.");
            }
            ConnectionTypeService.getInstance().delete(servicesConnectionType);
            logger.info("ConnectionType [SERVICES] removed.");
        }
    }

    @Override
    public void migrateSQLAfter(String scriptPath)
    throws Exception {
    }
}
