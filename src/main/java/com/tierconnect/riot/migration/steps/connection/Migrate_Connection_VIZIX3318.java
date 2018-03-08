package com.tierconnect.riot.migration.steps.connection;

import com.tierconnect.riot.appcore.entities.Connection;
import com.tierconnect.riot.appcore.entities.ConnectionType;
import com.tierconnect.riot.appcore.entities.QConnectionType;
import com.tierconnect.riot.appcore.services.ConnectionService;
import com.tierconnect.riot.appcore.services.ConnectionTypeService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.commons.utils.JsonUtils;
import com.tierconnect.riot.migration.steps.MigrationStep;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import org.apache.log4j.Logger;
import org.jose4j.json.internal.json_simple.JSONObject;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static com.tierconnect.riot.iot.popdb.PopDBKafka.newPropertyDefinition;

/**
 * Migrate_Connection_VIZIX3318 class.
 *
 * @author jantezana
 * @version 2017/03/30
 */
public class Migrate_Connection_VIZIX3318 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_Connection_VIZIX3318.class);
    private static final String KAFKA_CONNECTION_CODE = "KAFKA";
    private static final String SQL_CONNECTION_CODE = "SQL";
    private static final String SQL_CONNECTION_TYPE_CODE = "SQL";
    private static final String ROOT_CODE = "root";
    private static final String KAFKA_CONNECTION_TYPE_CODE = "KAFKA";
    private static final String KAFKA_CONNECTION_TYPE_DESCRIPTION = "KAFKA Broker";


    @Override
    public void migrateSQLBefore(String scriptPath)
    throws Exception {
    }

    @Override
    public void migrateHibernate()
    throws Exception {
        migrateConnectionType();
        migrateConnections();
    }

    /**
     * Migrate connection type.
     *
     * @throws NonUniqueResultException
     */
    private void migrateConnectionType()
    throws NonUniqueResultException, IOException {
        Set<Map<String, Object>> propertyDefinitionsList = new HashSet<>();
        String propertiesDefinitions;
        ConnectionType kafkaConnectionType = null;

        try {
            kafkaConnectionType = ConnectionTypeService.getInstance().getConnectionTypeByCode(KAFKA_CONNECTION_CODE);
        } catch (Exception exception) {
            logger.error(exception.getMessage(), exception);
        }

        if (kafkaConnectionType == null) {
            kafkaConnectionType = new ConnectionType();
            kafkaConnectionType.setCode(KAFKA_CONNECTION_TYPE_CODE);
            kafkaConnectionType.setDescription(KAFKA_CONNECTION_TYPE_DESCRIPTION);
            kafkaConnectionType.setGroup(GroupService.getInstance().getByCode(ROOT_CODE));

            propertyDefinitionsList.add(newPropertyDefinition("zookeeper", "Zookeeper", "String"));
            propertyDefinitionsList.add(newPropertyDefinition("server", "Server(s)", "String"));
            propertiesDefinitions = JsonUtils.convertObjectToJson(propertyDefinitionsList);
            kafkaConnectionType.setPropertiesDefinitions(propertiesDefinitions);
            ConnectionTypeService.getInstance().insert(kafkaConnectionType);
        } else {
            propertiesDefinitions = kafkaConnectionType.getPropertiesDefinitions();
            propertyDefinitionsList = JsonUtils.convertStringToObject(propertiesDefinitions, new HashSet<Map<String, Object>>().getClass());
            if (!propertiesDefinitions.contains("zookeeper")) {
                propertyDefinitionsList.add(newPropertyDefinition("zookeeper", "Zookeeper", "String"));
            }

            if (!propertiesDefinitions.contains("server")) {
                propertyDefinitionsList.add(newPropertyDefinition("server", "Server(s)", "String"));
            }

            propertiesDefinitions = JsonUtils.convertObjectToJson(propertyDefinitionsList);
            kafkaConnectionType.setPropertiesDefinitions(propertiesDefinitions);
            ConnectionTypeService.getInstance().update(kafkaConnectionType);
        }
        logger.info("Migration of connection type was completed successfully");
    }

    /**
     * Migrate connection.
     *
     * @throws NonUniqueResultException
     */
    private void migrateConnections()
    throws NonUniqueResultException {
        // Gets the kafka kafkaConnection.
        Connection kafkaConnection = null;
        try {
            kafkaConnection = ConnectionService.getInstance().getByCode(KAFKA_CONNECTION_CODE);
        } catch (Exception exception) {
            logger.error(exception.getMessage(), exception);
        }

        Map<String, Object> kafkaProperties = new LinkedHashMap<>();
        if (kafkaConnection == null) {
            kafkaConnection = new Connection();
            kafkaConnection.setName("Kafka");
            kafkaConnection.setCode(KAFKA_CONNECTION_CODE);
            kafkaConnection.setGroup(GroupService.getInstance().getRootGroup());
            kafkaConnection.setConnectionType(ConnectionTypeService.getInstance().getConnectionTypeDAO().selectBy(
                QConnectionType.connectionType.code.eq(KAFKA_CONNECTION_TYPE_CODE)));

            // Create the kafka configuration.
            kafkaProperties.put("zookeeper", "127.0.0.1:2181");
            kafkaProperties.put("server", "127.0.0.1:9092");

            kafkaConnection.setProperties(new JSONObject(kafkaProperties).toJSONString());
            ConnectionService.getInstance().insert(kafkaConnection);
        }

        // SQL Connection.
        Connection sqlConnection = null;
        try {
            sqlConnection = ConnectionService.getInstance().getByCode(SQL_CONNECTION_CODE);
        } catch (Exception exception) {
            logger.error(exception.getMessage(), exception);
        }

        Map<String, Object> sqlProperties = new LinkedHashMap<>();
        if (sqlConnection == null) {
            sqlConnection = new Connection();
            sqlConnection.setName(SQL_CONNECTION_CODE);
            sqlConnection.setCode(SQL_CONNECTION_CODE);
            sqlConnection.setGroup(GroupService.getInstance().getRootGroup());
            sqlConnection.setConnectionType(ConnectionTypeService.getInstance().getConnectionTypeDAO().selectBy(
                QConnectionType.connectionType.code.eq(SQL_CONNECTION_TYPE_CODE)));

            // Create the sql configuration.
            sqlProperties.put("driver", "com.mysql.jdbc.Driver");
            sqlProperties.put("dialect", "org.hibernate.dialect.MySQLDialect");
            sqlProperties.put("username", "root");
            sqlProperties.put("password", "Y29udHJvbDEyMyE=");
            sqlProperties.put("url", "jdbc:mysql://127.0.0.1:3306/riot_main");
            sqlProperties.put("hazelcastNativeClientAddress", "127.0.0.1");

            sqlConnection.setProperties(new JSONObject(sqlProperties).toJSONString());
            ConnectionService.getInstance().insert(sqlConnection);
        }

        logger.info("The migration of kafkaConnection and SqlConnection was completed successfully");
    }

    @Override
    public void migrateSQLAfter(String scriptPath)
    throws Exception {
    }
}
