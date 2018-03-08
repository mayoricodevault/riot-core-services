package com.tierconnect.riot.iot.popdb;

import com.google.common.base.Preconditions;
import com.tierconnect.riot.appcore.entities.Connection;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.QConnectionType;
import com.tierconnect.riot.appcore.popdb.PopDBRequired;
import com.tierconnect.riot.appcore.services.ConnectionService;
import com.tierconnect.riot.appcore.services.ConnectionTypeService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import com.tierconnect.riot.sdk.utils.HashUtils;

import org.apache.commons.io.Charsets;
import org.apache.log4j.Logger;
import org.hibernate.Transaction;
import org.jose4j.json.internal.json_simple.JSONObject;

import java.nio.charset.Charset;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.tierconnect.riot.commons.ConnectionConstants.KAFKA_SERVER;
import static com.tierconnect.riot.commons.ConnectionConstants.KAFKA_ZOOKEEPER;
import static com.tierconnect.riot.commons.ConnectionConstants.MONGO_AUTHDB;
import static com.tierconnect.riot.commons.ConnectionConstants.MONGO_CONNTIMEOUT;
import static com.tierconnect.riot.commons.ConnectionConstants.MONGO_DB;
import static com.tierconnect.riot.commons.ConnectionConstants.MONGO_MAXPOOLSIZE;
import static com.tierconnect.riot.commons.ConnectionConstants.MONGO_PASSWORD;
import static com.tierconnect.riot.commons.ConnectionConstants.MONGO_PRIMARY;
import static com.tierconnect.riot.commons.ConnectionConstants.MONGO_REPLICASET;
import static com.tierconnect.riot.commons.ConnectionConstants.MONGO_SECONDARY;
import static com.tierconnect.riot.commons.ConnectionConstants.MONGO_SHARDING;
import static com.tierconnect.riot.commons.ConnectionConstants.MONGO_SSL;
import static com.tierconnect.riot.commons.ConnectionConstants.MONGO_USERNAME;
import static com.tierconnect.riot.commons.ConnectionConstants.MQTT_HOST;
import static com.tierconnect.riot.commons.ConnectionConstants.MQTT_PORT;
import static com.tierconnect.riot.commons.ConnectionConstants.MQTT_QOS;
import static com.tierconnect.riot.commons.ConnectionConstants.MQTT_SECURE;
import static com.tierconnect.riot.commons.ConnectionConstants.SQL_DIALECT;
import static com.tierconnect.riot.commons.ConnectionConstants.SQL_DRIVER;
import static com.tierconnect.riot.commons.ConnectionConstants.SQL_HZ;
import static com.tierconnect.riot.commons.ConnectionConstants.SQL_PASSWORD;
import static com.tierconnect.riot.commons.ConnectionConstants.SQL_URL;
import static com.tierconnect.riot.commons.ConnectionConstants.SQL_USERNAME;

/**
 * PopDBKafka class.
 *
 * @author jantezana
 * @version 2017/02/15
 */
public class PopDBKafka {
    private static final Logger LOGGER = Logger.getLogger(PopDBKafka.class);
    private static final String MONGO_CONNECTION_CODE = "MONGO";
    private static final String MONGO_CONNECTION_TYPE_CODE = "MONGO";
    private static final String MQTT_CONNECTION_CODE = "MQTT";
    private static final String MQTT_CONNECTION_TYPE_CODE = "MQTT";
    private static final String KAFKA_CONNECTION_CODE = "KAFKA";
    private static final String KAFKA_CONNECTION_TYPE_CODE = "KAFKA";
    private static final String KAFKA_CONNECTION_TYPE_DESCRIPTION = "KAFKA Broker";
    private static final String SQL_CONNECTION_CODE = "SQL";
    private static final String SQL_CONNECTION_TYPE_CODE = "SQL";

    /**
     * Runs.
     *
     * @throws NonUniqueResultException
     */
    public void run()
    throws NonUniqueResultException {
        Group rootGroup = GroupService.getInstance().getByCode("root");
        populateConnections(rootGroup);
    }

    /**
     * Populates the connections.
     *
     * @param rootGroup the group
     */
    public static void populateConnections(Group rootGroup) {
        Preconditions.checkNotNull(rootGroup, "the group is null");
        ConnectionTypeService connectionTypeService = ConnectionTypeService.getInstance();
        ConnectionService connectionService = ConnectionService.getInstance();

        // Mongo connection.
        LOGGER.info("Creating mongo connection ...");
        String mongoPrimary = System.getProperty("mongo.primary");
        String mongoSecondary = System.getProperty("mongo.secondary");
        String mongoReplicaSet = System.getProperty("mongo.replicaset");
        String mongoSsl = System.getProperty("mongo.ssl");
        String mongoUserName = System.getProperty("mongo.username");
        String mongoPassword = System.getProperty("mongo.password");
        String mongoPasswordEncrypted = (mongoPassword != null) ? Base64.getEncoder().encodeToString(mongoPassword.getBytes(Charsets.UTF_8)) : "";
        String mongoAuthenticationDb = System.getProperty("mongo.authdb");
        String mongoDataBase = System.getProperty("mongo.db");
        String mongoSharding = System.getProperty("mongo.sharding");
        String mongoConnectionTimeout = System.getProperty("mongo.connectiontimeout");
        String mongoMaxPoolSize = System.getProperty("mongo.maxpoolsize");

        Connection mongoConnection = null;
        try {
            mongoConnection = ConnectionService.getInstance().getByCode(MONGO_CONNECTION_CODE);
        } catch (Exception exception) {
            LOGGER.error(exception.getMessage(), exception);
        }

        if (mongoConnection == null) {
            mongoConnection = new Connection();
            mongoConnection.setName("MongoDB");
            mongoConnection.setCode(MONGO_CONNECTION_CODE);
            mongoConnection.setGroup(rootGroup);
            mongoConnection.setConnectionType(
                connectionTypeService.getConnectionTypeDAO().selectBy(QConnectionType.connectionType.code.eq(MONGO_CONNECTION_TYPE_CODE)));

            // Create the  mongo configuration.
            Map<String, Object> mongoProperties = new LinkedHashMap<>();
            mongoProperties.put(MONGO_PRIMARY, mongoPrimary);
            mongoProperties.put(MONGO_SECONDARY, mongoSecondary);
            mongoProperties.put(MONGO_REPLICASET, mongoReplicaSet);
            mongoProperties.put(MONGO_SSL, mongoSsl);
            mongoProperties.put(MONGO_USERNAME, mongoUserName);
            mongoProperties.put(MONGO_PASSWORD, mongoPasswordEncrypted);
            mongoProperties.put(MONGO_AUTHDB, mongoAuthenticationDb);
            mongoProperties.put(MONGO_DB, mongoDataBase);
            mongoProperties.put(MONGO_SHARDING, mongoSharding);
            mongoProperties.put(MONGO_CONNTIMEOUT, mongoConnectionTimeout);
            mongoProperties.put(MONGO_MAXPOOLSIZE, mongoMaxPoolSize);

            mongoConnection.setProperties(new JSONObject(mongoProperties).toJSONString());
            connectionService.insert(mongoConnection);
        } else {
            LOGGER.warn("Mistakes creating the mongo connection, The connection exists ...");
        }

        // MQTT connection.
        LOGGER.info("Creating mqtt connection ...");
        String mqttHost = System.getProperty("mqtt.host");
        String mqttPort = System.getProperty("mqtt.port");

        Connection mqttConnection = null;
        try {
            mqttConnection = ConnectionService.getInstance().getByCode(MQTT_CONNECTION_CODE);
        } catch (Exception exception) {
            LOGGER.error(exception.getMessage(), exception);
        }

        if (mqttConnection == null) {
            mqttConnection = new Connection();
            mqttConnection.setName("Mqtt");
            mqttConnection.setCode(MQTT_CONNECTION_CODE);
            mqttConnection.setGroup(rootGroup);
            mqttConnection.setConnectionType(
                connectionTypeService.getConnectionTypeDAO().selectBy(QConnectionType.connectionType.code.eq(MQTT_CONNECTION_TYPE_CODE)));

            // Create the mqtt configuration.
            Map<String, Object> mqttProperties = new LinkedHashMap<>();
            mqttProperties.put(MQTT_HOST, mqttHost);
            mqttProperties.put(MQTT_PORT, mqttPort);
            mqttProperties.put(MQTT_QOS, 2);
            mqttProperties.put(MQTT_SECURE, false);

            mqttConnection.setProperties(new JSONObject(mqttProperties).toJSONString());
            connectionService.insert(mqttConnection);
        } else {
            LOGGER.warn("Mistakes creating the mqtt connection, The connection exists ...");
        }

        // KAFKA connection.
        LOGGER.info("Creating kafka connection ...");
        String kafkaZookeeper = System.getProperty("kafka.zookeeper");
        String kafkaServers = System.getProperty("kafka.servers");

        Connection kafkaConnection = null;
        try {
            kafkaConnection = ConnectionService.getInstance().getByCode(KAFKA_CONNECTION_CODE);
        } catch (Exception exception) {
            LOGGER.error(exception.getMessage(), exception);
        }

        if (kafkaConnection == null) {
            kafkaConnection = new Connection();
            kafkaConnection.setName("Kafka");
            kafkaConnection.setCode(KAFKA_CONNECTION_CODE);
            kafkaConnection.setGroup(rootGroup);
            kafkaConnection.setConnectionType(
                connectionTypeService.getConnectionTypeDAO().selectBy(QConnectionType.connectionType.code.eq(KAFKA_CONNECTION_TYPE_CODE)));

            // Create the kafka configuration.
            Map<String, Object> kafkaProperties = new LinkedHashMap<>();
            kafkaProperties.put(KAFKA_ZOOKEEPER, kafkaZookeeper);
            kafkaProperties.put(KAFKA_SERVER, kafkaServers);

            kafkaConnection.setProperties(new JSONObject(kafkaProperties).toJSONString());
            connectionService.insert(kafkaConnection);
        } else {
            LOGGER.warn("Mistakes creating the kafka connection, The connection exists ...");
        }

        // SQL connection.
        LOGGER.info("Creating sql connection ...");
        String driver = System.getProperty("hibernate.connection.driver_class");
        String dialect = System.getProperty("hibernate.dialect");
        String username = System.getProperty("hibernate.connection.username");
        String password = System.getProperty("hibernate.connection.password");
        String passwordEncrypted = (password != null) ? HashUtils.hashSHA256(password) : "";
        String url = System.getProperty("hibernate.connection.url");
        String hazelcastNativeClientAddress = System.getProperty("hibernate.cache.hazelcast.native_client_address");

        Connection sqlConnection = null;
        try {
            sqlConnection = ConnectionService.getInstance().getByCode(SQL_CONNECTION_CODE);
        } catch (Exception exception) {
            LOGGER.error(exception.getMessage(), exception);
        }

        if (sqlConnection == null) {
            sqlConnection = new Connection();
            sqlConnection.setName(SQL_CONNECTION_CODE);
            sqlConnection.setCode(SQL_CONNECTION_CODE);
            sqlConnection.setGroup(GroupService.getInstance().getRootGroup());
            sqlConnection.setConnectionType(
                connectionTypeService.getConnectionTypeDAO().selectBy(QConnectionType.connectionType.code.eq(SQL_CONNECTION_TYPE_CODE)));

            // Create the sql configuration.
            Map<String, Object> sqlProperties = new LinkedHashMap<>();
            sqlProperties.put(SQL_DRIVER, driver);
            sqlProperties.put(SQL_DIALECT, dialect);
            sqlProperties.put(SQL_USERNAME, username);
            sqlProperties.put(SQL_PASSWORD, passwordEncrypted);
            sqlProperties.put(SQL_URL, url);
            sqlProperties.put(SQL_HZ, hazelcastNativeClientAddress);

            final String properties = new JSONObject(sqlProperties).toJSONString();
            sqlConnection.setProperties(properties);
            connectionService.insert(sqlConnection);
        } else {
            LOGGER.warn("Mistakes creating the sql connection, The connection exists ...");
        }
    }

    /**
     * Creates a new property definition
     *
     * @param code  the code
     * @param label the label
     * @param type  the type
     * @return the property definition
     */
    public static Map<String, Object> newPropertyDefinition(final String code,
                                                            final String label,
                                                            final String type) {
        Preconditions.checkNotNull(code, "The code is null");
        Preconditions.checkNotNull(label, "The label is null");
        Preconditions.checkNotNull(type, "The type is null");
        return newPropertyDefinition(code, label, type, null);
    }

    /**
     * Creates a new property definition.
     *
     * @param code         the code
     * @param label        the label
     * @param type         the type
     * @param defaultValue the default value
     * @return the property definition
     */
    public static Map<String, Object> newPropertyDefinition(final String code,
                                                            final String label,
                                                            final String type,
                                                            final String defaultValue) {
        Preconditions.checkNotNull(code, "The code is null");
        Preconditions.checkNotNull(label, "the label is null");
        Preconditions.checkNotNull(type, "Teh type is null");

        Map<String, Object> propertyDefinition = new LinkedHashMap<>();
        propertyDefinition.put("code", code);
        propertyDefinition.put("label", label);
        propertyDefinition.put("type", type);
        if (defaultValue != null) {
            propertyDefinition.put("defaultValue", defaultValue);
        }
        return propertyDefinition;
    }

    /**
     * Main Task to Populate Data Base.
     *
     * @param args Arguments to set in command prompt.
     */
    public static void main(String args[])
    throws NonUniqueResultException {
        PopDBRequired.initJDBCDrivers();
        System.getProperties().put("hibernate.hbm2ddl.auto", "update");
        System.getProperties().put("hibernate.cache.use_second_level_cache", "false");
        System.getProperties().put("hibernate.cache.use_query_cache", "false");
        PopDBKafka popDBKafka = new PopDBKafka();
        Transaction transaction = GroupService.getGroupDAO().getSession().getTransaction();
        transaction.begin();
        popDBKafka.run();
        transaction.commit();
    }
}
