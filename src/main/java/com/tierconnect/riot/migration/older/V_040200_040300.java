package com.tierconnect.riot.migration.older;

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
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.iot.dao.EdgeboxDAO;
import com.tierconnect.riot.iot.dao.mongo.ThingMongoDAO;
import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.entities.EdgeboxRule;
import com.tierconnect.riot.iot.entities.ThingTypeFieldTemplate;
import com.tierconnect.riot.iot.entities.ThingTypeTemplate;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.migration.DBHelper;
import org.apache.commons.io.Charsets;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.log4j.Logger;
import org.jose4j.json.internal.json_simple.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by cvertiz on 05/14/2016.
 *
 * Modify by cvertiz on 05/14/2016.
 */
@Deprecated
public class V_040200_040300 implements MigrationStepOld {

    Logger logger = Logger.getLogger(V_040200_040300.class);

    @Override
    public List<Integer> getFromVersions() {
        return Arrays.asList(40200, 40300, 4030000);
    }

    @Override
    public int getToVersion() {
        return 4030001;
    }

    @Override
    public void migrateSQLBefore() throws Exception {
        DBHelper dbHelper = new DBHelper();
        if (!dbHelper.existColumn("version", "computerIP")) {
            String databaseType = DBHelper.getDataBaseType();
            dbHelper.executeSQLFile("sql/" + databaseType + "/V040200_to_040300.sql");

            //Create Index thingTypeId
            ThingMongoDAO.createIndexInThingsCollection(ThingMongoDAO.THINGS, "thingTypeId");
        }
    }

    @Override
    public void migrateHibernate() throws Exception {
            migrateConnectionType();
            migrateConnection();
            regularizeTypeEdgeBox();
            migrateCoreBridgeConfiguration();
            migrationAlienReaderRules();
    }

    @Override
    public void migrateSQLAfter() throws Exception {

    }

    private void migrateConnection() {
        logger.info("Migrating Connection");
        List<Connection> connections = ConnectionService.getConnectionDAO().selectAll();
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode propertiesNode = null;
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
                    ConnectionTypeService.getInstance().getConnectionTypeDAO().selectBy(QConnectionType.connectionType.code.eq("FTP")));

            JSONObject jsonProperties = new JSONObject();
            Map<String, Object> mapProperties = new LinkedHashMap<String, Object>();
            mapProperties.put("username", "ftpUser");
            mapProperties.put("password", "MTIzNA==");
            mapProperties.put("host", "localhost");
            mapProperties.put("port", 21);
            mapProperties.put("secure", false);
            jsonProperties.putAll(mapProperties);
            connection.setProperties(jsonProperties.toJSONString());
            ConnectionService.getInstance().insert(connection);
        }
    }

    private void migrateConnectionType() {
        logger.info("Migrating Connection Types");
        List<ConnectionType> connectionTypes = ConnectionTypeService.getInstance().getConnectionTypeDAO().selectAll();
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode propertiesNode = null;
        ObjectNode secureNode = null;
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

    private void migrateCoreBridgeConfiguration() {
        logger.info("Migrating Core Bridge Configuration");
        List<Edgebox> edgeboxes = EdgeboxService.getEdgeboxDAO().selectAll();
        for (Edgebox edgebox : edgeboxes) {

            if ("".equals(edgebox.getConfiguration())){
                logger.info("The JSON configuration for bridge=" + edgebox.getCode()
                        + " is empty, skipping migration for this bridge...");
                continue;
            }

            //to add a comma when the text config is without it
            String textChangedEdgebox = getChangeEdgebox(edgebox.getCode(), edgebox.getConfiguration());
            if (textChangedEdgebox.length() > 1) {
                edgebox.setConfiguration(textChangedEdgebox);
            }
            String configurationMCB = edgebox.getConfiguration().replace(" ", "");
            ObjectMapper mapper = new ObjectMapper();
            JsonNode configurationNode;
            String[] toRemove = new String[]{"host", "port", "dbname", "username", "password"};
            List<Connection> connections = ConnectionService.getConnectionDAO().selectAll();
            try {
                configurationNode = mapper.readTree(configurationMCB);

                if (configurationNode.get("mqtt") != null) {

                    JsonNode jsonHost = ((ObjectNode) configurationNode.get("mqtt")).get("host");
                    if (jsonHost != null) {
                        String host = jsonHost.asText();
                        Integer port = Integer.parseInt(((ObjectNode) configurationNode.get("mqtt")).get("port").asText());

                        for (Connection connection : connections) {
                            if (connection.getConnectionType().getCode().startsWith("MQTT")) {
                                if (connection.getPropertyAsString("host").equals(host) && connection.getPropertyAsNumber("port").equals(port)) {
                                    ((ObjectNode) configurationNode.get("mqtt")).put("connectionCode", connection
                                            .getCode());
                                }
                            }
                        }

                        if (((ObjectNode) configurationNode.get("mqtt")).get("connectionCode") == null) {
                            Connection connection = new Connection();
                            connection.setCode("MQTT_FOR_" + edgebox.getCode());
                            connection.setName("MQTT for " + edgebox.getName());
                            connection.setGroup(edgebox.getGroup());
                            connection.setConnectionType(ConnectionTypeService.getInstance().getConnectionTypeByCode("MQTT"));
                            connection.setProperties(getConfigForMqtt(host, port));
                            ConnectionService.getInstance().insert(connection);
                            ((ObjectNode) configurationNode.get("mqtt")).put("connectionCode", connection.getCode());
                        }

                        for (int i = 0; i < toRemove.length; i++) {
                            if (configurationNode.get("mqtt").get(toRemove[i]) != null) {
                                ((ObjectNode) configurationNode.get("mqtt")).remove(toRemove[i]);
                            }
                        }
                    }
                }

                if (configurationNode.get("mongo") != null) {

                    JsonNode jsonHost = ((ObjectNode) configurationNode.get("mongo")).get("host");
                    JsonNode jsonPort = ((ObjectNode) configurationNode.get("mongo")).get("port");
                    JsonNode jsonUsername = ((ObjectNode) configurationNode.get("mongo")).get("username");
                    JsonNode jsonPassword = ((ObjectNode) configurationNode.get("mongo")).get("password");

                    if (jsonHost != null && jsonPort != null && jsonUsername != null && jsonPassword != null) {

                        String host = jsonHost.asText();
                        Integer port = Integer.parseInt(jsonPort.asText());
                        String username = jsonUsername.asText();
                        String password = jsonPassword.asText();
                        for (Connection connection : connections) {
                            if (connection.getConnectionType().getCode().startsWith("MONGO")) {
                                if (connection.getPropertyAsString("host").equals(host) &&
                                        connection.getPropertyAsNumber("port").equals(port) &&
                                        connection.getPropertyAsString("username").equals(username) &&
                                        connection.getPassword(false).equals(password)) {
                                    ((ObjectNode) configurationNode.get("mongo")).put("connectionCode", connection
                                            .getCode());
                                }
                            }
                        }

                        if (((ObjectNode) configurationNode.get("mongo")).get("connectionCode") == null) {
                            Connection connection = new Connection();
                            connection.setCode("MONGO_FOR_" + edgebox.getCode());
                            connection.setName("Mongo DB for " + edgebox.getName());
                            connection.setGroup(edgebox.getGroup());
                            connection.setConnectionType(ConnectionTypeService.getInstance().getConnectionTypeByCode("MONGO"));
                            connection.setProperties(getConfigForMongo(host, port, username, password));
                            ConnectionService.getInstance().insert(connection);
                            ((ObjectNode) configurationNode.get("mongo")).put("connectionCode", connection.getCode());
                        }

                        for (int i = 0; i < toRemove.length; i++) {
                            if (configurationNode.get("mongo").get(toRemove[i]) != null) {
                                ((ObjectNode) configurationNode.get("mongo")).remove(toRemove[i]);
                            }
                        }
                    }
                }

                if (configurationNode.get("ftp") != null) {

                    JsonNode jsonHost = ((ObjectNode) configurationNode.get("ftp")).get("host");
                    JsonNode jsonPort = ((ObjectNode) configurationNode.get("ftp")).get("port");
                    JsonNode jsonUsername = ((ObjectNode) configurationNode.get("ftp")).get("username");
                    JsonNode jsonPassword = ((ObjectNode) configurationNode.get("ftp")).get("password");

                    if (jsonHost != null && jsonPort != null && jsonUsername != null && jsonPassword != null) {
                        String host = jsonHost.asText();
                        Integer port = Integer.parseInt(jsonPort.asText());
                        String username = jsonUsername.asText();
                        String password = jsonPassword.asText();
                        for (Connection connection : connections) {
                            if (connection.getConnectionType().getCode().startsWith("FTP")) {
                                if (connection.getPropertyAsString("host").equals(host) &&
                                        connection.getPropertyAsNumber("port").equals(port) &&
                                        connection.getPropertyAsString("username").equals(username) &&
                                        connection.getPassword(false).equals(password)) {
                                    ((ObjectNode) configurationNode.get("ftp")).put("connectionCode", connection
                                            .getCode());
                                }
                            }
                        }

                        if (((ObjectNode) configurationNode.get("ftp")).get("connectionCode") == null) {
                            Connection connection = new Connection();
                            connection.setCode("FTP_FOR_" + edgebox.getCode());
                            connection.setName("Ftp Server for " + edgebox.getName());
                            connection.setGroup(edgebox.getGroup());
                            connection.setConnectionType(ConnectionTypeService.getInstance().getConnectionTypeByCode("FTP"));
                            connection.setProperties(getConfigForFtp(host, port, username, password));
                            ConnectionService.getInstance().insert(connection);
                            ((ObjectNode) configurationNode.get("ftp")).put("connectionCode", connection.getCode());
                        }

                        for (int i = 0; i < toRemove.length; i++) {
                            if (configurationNode.get("ftp").get(toRemove[i]) != null) {
                                ((ObjectNode) configurationNode.get("ftp")).remove(toRemove[i]);
                            }
                        }
                    }
                }

                if (configurationNode.get("zoneDwellFilter") != null) {
                    if (configurationNode.get("zoneDwellFilter").get("lastDetectTimeActive") == null) {
                        ((ObjectNode) configurationNode.get("zoneDwellFilter")).put("lastDetectTimeActive", 1);
                    }
                    if (configurationNode.get("zoneDwellFilter").get("lastDetectTimeWindow") == null) {
                        ((ObjectNode) configurationNode.get("zoneDwellFilter")).put("lastDetectTimeWindow", 0);
                    }
                }

                if (edgebox.getType().equals("core") && configurationNode.get("CEPLogging") == null) {
                    ObjectNode cEPLogging = ((ObjectNode) configurationNode).putObject("CEPLogging");
                    cEPLogging.put("active", 0);
                }

                if (edgebox.getType().equals("core") && (configurationMCB.contains("lazyLoadCache"))) {
                    ((ObjectNode) configurationNode).remove("lazyLoadCache");
                }

                if (edgebox.getType().equals("core") && !(configurationMCB.contains("checkMultilevelReferences"))) {
                    ObjectNode checkMultilevelReferences = ((ObjectNode) configurationNode).putObject("checkMultilevelReferences");
                    checkMultilevelReferences.put("active", 0);
                }

                // adding property to all corebridges
                if (edgebox.getType().equals("core") && !configurationMCB.contains("threadDispatchMode")) {
                    ((ObjectNode) configurationNode).put("threadDispatchMode", 1);
                }

                if (edgebox.getType().equals("core") && !configurationMCB.contains("numberOfThreads")) {
                    ((ObjectNode) configurationNode).put("numberOfThreads", 32);
                }
                // adding property to all alebridges not ftp's
                if (edgebox.getType().equals("edge") && !configurationMCB.contains("numberOfThreads")
                        && configurationNode.get("ftp") == null && configurationNode.get("geoforce")==null
                        && configurationNode.get("messageMode") == null) {
                    ((ObjectNode) configurationNode).put("numberOfThreads", 10);
                }


                edgebox.setConfiguration(configurationNode.toString());
                EdgeboxService.getInstance().update(edgebox);
            } catch (IOException e) {
                logger.error(e);
            }
        }

    }

    private void migrationAlienReaderRules(){
        logger.info("Migration Alien reader rules");
        EdgeboxRuleService edgeboxRuleService = EdgeboxRuleService.getInstance();
        List<EdgeboxRule> edgeBoxRuleList = edgeboxRuleService.selectByAction("AlienReaderGPIOSubscriber");
        for (EdgeboxRule edgeboxRule : edgeBoxRuleList) {
            ObjectMapper mapper = new ObjectMapper();
            String outputConfig = edgeboxRule.getOutputConfig().replace(" ", "");
            JsonNode configurationNode;
            try {
                configurationNode = mapper.readTree(outputConfig);
                if (configurationNode.get("times") != null) {
                    if (configurationNode.get("times").get("numberOfRetries") == null) {
                        ((ObjectNode) configurationNode.get("times")).put("numberOfRetries", 5);
                    }
                    if (configurationNode.get("times").get("retryTime") == null) {
                        ((ObjectNode) configurationNode.get("times")).put("retryTime", 5000);
                    }
                    if (configurationNode.get("times").get("delay") == null) {
                        ((ObjectNode) configurationNode.get("times")).put("delay", 2000);
                    }
                }
                edgeboxRule.setOutputConfig(configurationNode.toString());
                edgeboxRuleService.update(edgeboxRule);
            }catch (Exception e){
                logger.error("Error in migrate AlienReader", e);
            }
        }
    }

    private void insertThingTypeFieldTemplate(ThingTypeTemplate thingType, String UDFString) throws IOException {

        ThingTypeTemplate thingTypeTemp = ThingTypeTemplateService.getInstance().insert(thingType);
        ObjectMapper mapper = new ObjectMapper();
        ThingTypeFieldTemplate field;

        List UDFList = mapper.readValue(UDFString, List.class);
        for (int i = 0; i < UDFList.size(); i++) {
            Map options = (Map) UDFList.get(i);

            field = new ThingTypeFieldTemplate();
            field.setName((String) options.get("name"));
            field.setDescription((String) options.get("name"));
            field.setDefaultValue("");
            field.setUnit("");
            field.setSymbol("");
            field.setType(DataTypeService.getInstance().get(new Long((String) options.get("type"))));
            field.setTypeParent("DATA_TYPE");
            field.setThingTypeTemplate(thingTypeTemp);
            field.setTimeSeries((Boolean) options.get("timeSeries"));
            if (options.containsKey("defaultValue")) {
                field.setDefaultValue((String) options.get("defaultValue"));
            }
            ThingTypeFieldTemplateService.getInstance().insert(field);
        }
    }

    public String getConfigForMqtt(String host, Integer port) {
        return "{\"host\":\"" + host + "\",\"port\":" + port + ",\"secure\":false,\"qos\":1}";
    }

    public String getConfigForMongo(String host, Integer port, String username, String password) {
        String newPassword = org.apache.commons.codec.binary.Base64.encodeBase64String(password.getBytes(Charsets.UTF_8));
        return "{\"host\":\"" + host + "\",\"password\":\"" + newPassword + "\",\"dbname\":\"riot_main\",\"port\":" + port + ",\"username\":\"" + username + "\",\"secure\":false}";
    }

    public String getConfigForFtp(String host, Integer port, String username, String password) {
        String newPassword = org.apache.commons.codec.binary.Base64.encodeBase64String(password.getBytes(Charsets.UTF_8));
        return "{\"host\":\"" + host + "\",\"password\":\"" + newPassword + "\",\"port\":" + port + ",\"username\":\"" + username + "\",\"secure\":false}";
    }

    /**
     * @param edgeboxCode
     * @param edgeboxConfiguration
     * @return a replaced text Edgebox (to add a comma when the text is without it)
     */
    public String getChangeEdgebox(String edgeboxCode, String edgeboxConfiguration) {
        String changedText = "";
        if (edgeboxCode.equalsIgnoreCase("FTPBBYW")) {
            String regexProcess = "[\"processPolicy\": \"Move\"\n\"localBackupFolder\":]";
            String textProcessToReplace = "\"processPolicy\": \"Move\"";
            String textProcessRight = "\"processPolicy\": \"Move\",";
            Pattern pProcess = Pattern.compile(regexProcess);
            Matcher mProcess = pProcess.matcher(edgeboxConfiguration);
            if (mProcess.find()) {
                changedText = edgeboxConfiguration.replace(textProcessToReplace, textProcessRight);
            }
        }
        return changedText;
    }

    /**
     * Method to regularize types of edgeboxes
     */
    private void regularizeTypeEdgeBox() {
        EdgeboxDAO edgeboxDAO = EdgeboxService.getEdgeboxDAO();
        List<Edgebox> listEdgebox = edgeboxDAO.selectAll();
        for (Edgebox edge : listEdgebox) {
            boolean setEdgeType = false;
            if (isCoreBridge(edge.getConfiguration())) {
                if (!StringUtils.equals(edge.getType(), "core")) {
                    edge.setType("core");
                    setEdgeType = true;
                }
            } else {
                if (!StringUtils.equals(edge.getType(), "edge")) {
                    edge.setType("edge");
                    setEdgeType = true;
                }
            }
            if (setEdgeType) {
                edgeboxDAO.update(edge);
            }
        }
    }

    /**
     * @param configuration the configuration attribute of edgebox table
     * @return true if it is a core bridge configuration
     */
    public boolean isCoreBridge(String configuration) {
        return (!StringUtils.isBlank(configuration) && configuration.toLowerCase().contains("pointinzonerule")); //specific of core bridges
    }

}
