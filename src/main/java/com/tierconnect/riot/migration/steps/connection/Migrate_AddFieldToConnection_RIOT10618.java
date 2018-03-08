package com.tierconnect.riot.migration.steps.connection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tierconnect.riot.appcore.entities.Connection;
import com.tierconnect.riot.appcore.entities.ConnectionType;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.QConnectionType;
import com.tierconnect.riot.appcore.popdb.PopDBRequired;
import com.tierconnect.riot.appcore.services.ConnectionService;
import com.tierconnect.riot.appcore.services.ConnectionTypeService;
import com.tierconnect.riot.appcore.services.ConnectionTypeServiceBase;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.commons.lang.WordUtils;
import org.apache.log4j.Logger;
import org.jose4j.json.internal.json_simple.JSONObject;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
@SuppressWarnings("unused")
public class Migrate_AddFieldToConnection_RIOT10618 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_AddFieldToConnection_RIOT10618.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateConnectionType();
        migrateConnection();
    }

    private void migrateConnection() {
        logger.info("Migrating Connection");
        List<Connection> connections = ConnectionService.getConnectionDAO().selectAll();
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode propertiesNode;
        for (Connection connection : connections) {
            boolean setPropDef = false;
            try {
                propertiesNode = (ObjectNode) mapper.readTree(connection.getProperties());
                switch (connection.getCode()) {
                    case "SERVICES":
                        if (!propertiesNode.findValuesAsText("secure").contains("false")) {
                            propertiesNode.put("secure", false);
                            setPropDef = true;
                        }
                        if (!propertiesNode.findValuesAsText("port").contains(propertiesNode.get("port").asText())) {
                            propertiesNode.put("port", Integer.parseInt(propertiesNode.get("port").asText()));
                            setPropDef = true;
                        }
                        if (setPropDef) {
                            connection.setProperties(propertiesNode.toString());
                        }
                        break;
                    case "MQTT":
                        if (!propertiesNode.findValuesAsText("secure").contains("false")) {
                            propertiesNode.put("secure", false);
                            setPropDef = true;
                        }
                        if (!propertiesNode.findValuesAsText("qos").contains("2")) {
                            propertiesNode.put("qos", 2);
                            setPropDef = true;
                        }
                        if (!propertiesNode.findValuesAsText("port").contains(propertiesNode.get("port").asText())) {
                            propertiesNode.put("port", Integer.parseInt(propertiesNode.get("port").asText()));
                            setPropDef = true;
                        }
                        if (setPropDef) {
                            connection.setProperties(propertiesNode.toString());
                        }
                        break;
                    case "MONGO":
                        if (!propertiesNode.findValuesAsText("secure").contains("false")) {
                            propertiesNode.put("secure", false);
                            setPropDef = true;
                        }
                        if (!propertiesNode.findValuesAsText("port").contains(propertiesNode.get("port").asText())) {
                            propertiesNode.put("port", Integer.parseInt(propertiesNode.get("port").asText()));
                            setPropDef = true;
                        }
                        if (setPropDef) {
                            connection.setProperties(propertiesNode.toString());
                        }
                        break;
                    case "HADOOP":
                        if (!propertiesNode.findValuesAsText("secure").contains("false")) {
                            propertiesNode.put("secure", false);
                            setPropDef = true;
                        }
                        if (!propertiesNode.findValuesAsText("port").contains(propertiesNode.get("port").asText())) {
                            propertiesNode.put("port", Integer.parseInt(propertiesNode.get("port").asText()));
                            setPropDef = true;
                        }
                        if (setPropDef) {
                            connection.setProperties(propertiesNode.toString());
                        }
                        break;
                }
                connection.setProperties(propertiesNode.toString());
            } catch (IOException e) {
                logger.error(e);
            }
            ConnectionService.getInstance().update(connection);
        }

        if (ConnectionService.getInstance().getByCode("FTP") == null) {
            // FTP connection example
            Connection connection = new Connection();
            connection.setName("Ftp");
            connection.setCode("FTP");
            connection.setGroup(GroupService.getInstance().getRootGroup());
            connection.setConnectionType(
                    ConnectionTypeServiceBase.getConnectionTypeDAO().selectBy(QConnectionType.connectionType.code.eq("FTP")));

            JSONObject jsonProperties = new JSONObject();
            Map<String, Object> mapProperties = new LinkedHashMap<>();
            mapProperties.put("username", "ftpUser");
            mapProperties.put("password", "MTIzNA==");
            mapProperties.put("host", "localhost");
            mapProperties.put("port", 21);
            mapProperties.put("secure", false);
            //noinspection unchecked
            jsonProperties.putAll(mapProperties);
            connection.setProperties(jsonProperties.toJSONString());
            ConnectionService.getInstance().insert(connection);
        }
    }

    private void migrateConnectionType() {
        logger.info("Migrating Connection Types");
        List<ConnectionType> connectionTypes = ConnectionTypeServiceBase.getConnectionTypeDAO().selectAll();
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode propertiesNode;
        ObjectNode secureNode;
        for (ConnectionType connectionType : connectionTypes) {
            try {
                boolean setPropDef = false;
                propertiesNode = (ArrayNode) mapper.readTree(connectionType.getPropertiesDefinitions());
                switch (connectionType.getCode()) {
                    case "SERVICES":
                        connectionType.setDescription("ViZix REST API");
                        secureNode = propertiesNode.addObject();
                        if (!propertiesNode.findValuesAsText("code").contains("secure")) {
                            secureNode.put("code", "secure");
                            setPropDef = true;
                        }
                        if (!propertiesNode.findValuesAsText("label").contains("Secure")) {
                            secureNode.put("label", "Secure");
                            setPropDef = true;
                        }
                        if (!propertiesNode.findValuesAsText("type").contains("Boolean")) {
                            secureNode.put("type", "Boolean");
                            setPropDef = true;
                        }
                        if (setPropDef) {
                            connectionType.setPropertiesDefinitions(propertiesNode.toString());
                        }
                        break;
                    case "MQTT":
                        connectionType.setDescription("MQTT Broker (Mosquitto)");
                        secureNode = propertiesNode.addObject();
                        if (!propertiesNode.findValuesAsText("code").contains("secure")) {
                            secureNode.put("code", "secure");
                            setPropDef = true;
                        }
                        if (!propertiesNode.findValuesAsText("label").contains("Secure")) {
                            secureNode.put("label", "Secure");
                            setPropDef = true;
                        }
                        if (!propertiesNode.findValuesAsText("type").contains("Boolean")) {
                            secureNode.put("type", "Boolean");
                            setPropDef = true;
                        }
                        if (!propertiesNode.findValuesAsText("code").contains("qos")) {
                            secureNode.put("code", "qos");
                            setPropDef = true;
                        }
                        if (!propertiesNode.findValuesAsText("label").contains("QoS")) {
                            secureNode.put("label", "QoS");
                            setPropDef = true;
                        }
                        if (!propertiesNode.findValuesAsText("type").contains("Number")) {
                            secureNode.put("type", "Number");
                            setPropDef = true;
                        }
                        if (setPropDef) {
                            connectionType.setPropertiesDefinitions(propertiesNode.toString());
                        }
                        break;
                    case "MONGO":
                        connectionType.setDescription("Mongo DB");
                        secureNode = propertiesNode.addObject();
                        if (!propertiesNode.findValuesAsText("code").contains("secure")) {
                            secureNode.put("code", "secure");
                            setPropDef = true;
                        }
                        if (!propertiesNode.findValuesAsText("label").contains("Secure")) {
                            secureNode.put("label", "Secure");
                            setPropDef = true;
                        }
                        if (!propertiesNode.findValuesAsText("type").contains("Boolean")) {
                            secureNode.put("type", "Boolean");
                            setPropDef = true;
                        }
                        if (setPropDef) {
                            connectionType.setPropertiesDefinitions(propertiesNode.toString());
                        }
                        break;
                    case "HADOOP":
                        connectionType.setDescription("Hadoop DB");
                        secureNode = propertiesNode.addObject();
                        if (!propertiesNode.findValuesAsText("type").contains("Boolean")) {
                            secureNode.put("code", "secure");
                            setPropDef = true;
                        }
                        if (!propertiesNode.findValuesAsText("type").contains("Boolean")) {
                            secureNode.put("label", "Secure");
                            setPropDef = true;
                        }
                        if (!propertiesNode.findValuesAsText("type").contains("Boolean")) {
                            secureNode.put("type", "Boolean");
                            setPropDef = true;
                        }
                        if (setPropDef) {
                            connectionType.setPropertiesDefinitions(propertiesNode.toString());
                        }
                        break;
                    case "DBConnection":
                        connectionType.setDescription("External Relational DB");
                        break;
                }
                for (JsonNode property : propertiesNode) {
                    if (property.get("code") != null && property.get("code").asText().equals("port")) {
                        ((ObjectNode) property).put("port", "Number");
                    }
                    if (property.get("label") != null) {
                        ((ObjectNode) property).put("label", WordUtils.capitalize(property.get("label").asText()));
                    }
                }
                connectionType.setPropertiesDefinitions(propertiesNode.toString());
            } catch (IOException e) {
                logger.error(e);
            }
            ConnectionTypeService.getInstance().update(connectionType);
        }

        //Insert if Connection Types does not exist
        Group group = GroupService.getInstance().getRootGroup();
        if (ConnectionTypeService.getInstance().getConnectionTypeByCode("FTP") == null) {
            PopDBRequired.populateFTPConnection(group);
        }
        if (ConnectionTypeService.getInstance().getConnectionTypeByCode("SERVICES") == null) {
            PopDBRequired.populateRestConnection(group);
        }
        if (ConnectionTypeService.getInstance().getConnectionTypeByCode("MQTT") == null) {
            PopDBRequired.populateMQTTConnection(group);
        }
        if (ConnectionTypeService.getInstance().getConnectionTypeByCode("MONGO") == null) {
            PopDBRequired.populateMongoConnection(group);
        }
    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
