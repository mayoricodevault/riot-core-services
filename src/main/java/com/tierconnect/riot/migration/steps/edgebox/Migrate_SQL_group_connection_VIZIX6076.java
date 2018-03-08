package com.tierconnect.riot.migration.steps.edgebox;

import com.tierconnect.riot.appcore.entities.Connection;
import com.tierconnect.riot.appcore.entities.ConnectionType;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.services.ConnectionService;
import com.tierconnect.riot.appcore.services.ConnectionTypeService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.services.EdgeboxService;
import com.tierconnect.riot.migration.steps.MigrationStep;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import org.apache.log4j.Logger;
import java.util.List;

/**
 * Created by dbascope
 * on 06/28/17
 */
public class Migrate_SQL_group_connection_VIZIX6076 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_SQL_group_connection_VIZIX6076.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
    }

    @Override
    public void migrateHibernate() throws Exception {
        cleanupConnectionTypes();
        createSqlConnections();
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
    }

    private void cleanupConnectionTypes() {
      for (ConnectionType connectionType : ConnectionTypeService.getInstance().listPaginated(null, null)) {
          connectionType.setPropertiesDefinitions(connectionType.getPropertiesDefinitions().replace(",{}", ""));
          ConnectionTypeService.getInstance().update(connectionType);
      }
    }

    private void createSqlConnections() throws NonUniqueResultException {
        ConnectionType sqlConnectionType =
            ConnectionTypeService.getInstance().getConnectionTypeByCode("SQL");
        String apikey = "\"apikey\": \"7B4BCCDC\"";
        String httpHost = "\"httpHost\": \"services\"";
        List<Edgebox> edgeBoxList =
            EdgeboxService.getInstance().getByType("core");
        if (edgeBoxList.size() > 0) {
            for (Edgebox coreBridge : edgeBoxList) {
                Connection sqlConnection = getConnection(sqlConnectionType, coreBridge.getGroup());
                String bridgeTopic =
                    "\"bridgeTopic\": \"/v1/bridgeAgent/control/" + coreBridge.getGroup().getCode() + "\"";
                String msqlConfig = "\"mysqlConnectionCode\":\"" + sqlConnection.getCode() + "\"";
                String config = coreBridge.getConfiguration();
                if (!config.contains("bridgeTopic")) {
                    config = appendConfig(config, apikey);
                    config = appendConfig(config, msqlConfig);
                    config = appendConfig(config, bridgeTopic);
                    config = appendConfig(config, httpHost);
                    coreBridge.setConfiguration(config);
                    EdgeboxService.getInstance().update(coreBridge);
                    logger.info(
                        "Edgebox '" + coreBridge.getCode() + "' for '" + coreBridge.getGroup().getCode()
                            + "' was updated.");
                }
            }
        }
        for (String type : new String[] {"edge", "STARFlex", "FTP", "GPS", "Thing_Joiner", "Rules_Processor", "Mongo_Injector"}) {
            edgeBoxList = EdgeboxService.getInstance().getByType(type);
            if (edgeBoxList.size() > 0) {
                for (Edgebox edgebox : edgeBoxList) {
                    String bridgeTopic =
                        "\"bridgeTopic\": \"/v1/bridgeAgent/control/" + edgebox.getGroup().getCode() + "\"";
                    String config = edgebox.getConfiguration();
                    if (!config.contains("bridgeTopic")) {
                        config = appendConfig(config, apikey);
                        config = appendConfig(config, bridgeTopic);
                        config = appendConfig(config, httpHost);
                        edgebox.setConfiguration(config);
                        EdgeboxService.getInstance().update(edgebox);
                        logger.info("Edgebox '" + edgebox.getCode() + "' for '" + edgebox.getGroup()
                            .getCode() + "' was updated.");
                    }
                }
            }
        }
    }

    private Connection getConnection(ConnectionType connectionType, Group group) {
        List<Connection> connectionList =
            ConnectionService.getInstance().getByTypeAndGroup(connectionType, group);
        Connection connection;
        if (connectionList.isEmpty()) {
            connection = new Connection();
            connection.setCode(connectionType.getCode() + "_" + group.getCode());
            connection.setConnectionType(connectionType);
            connection.setGroup(group);
            connection.setName(connectionType.getCode() +  group.getName());
            connection.setProperties("{\"password\":\"Y29udHJvbDEyMyE=\",\"dialect\":\"org"
                + ".hibernate.dialect.MySQLDialect\",\"driver\":\"com.mysql.jdbc.Driver\","
                + "\"url\":\"jdbc:mysql://mysql:3306/riot_main\",\"username\":\"root\","
                + "\"hazelcastNativeClientAddress\":\"hazelcast\"}");
            ConnectionService.getInstance().insert(connection);
            logger.info("Connection '" + connectionType.getCode() + "_" + group.getCode() + "' created for group '"
                + group.getCode() + "'");
        } else {
            connection = connectionList.get(0);
        }
        return connection;
    }

    private String appendConfig(String config, String newConfig) {
        if (!config.contains(newConfig)) {
            return config.substring(0, config.length() - 1) + "," + newConfig + "}";
        }
        return config;
    }
}
