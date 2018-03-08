package com.tierconnect.riot.migration.steps.edgebox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tierconnect.riot.appcore.entities.Connection;
import com.tierconnect.riot.appcore.services.ConnectionService;
import com.tierconnect.riot.appcore.services.ConnectionTypeService;

import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.entities.EdgeboxRule;
import com.tierconnect.riot.iot.services.EdgeboxRuleService;
import com.tierconnect.riot.iot.services.EdgeboxService;

import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.commons.io.Charsets;
import org.apache.log4j.Logger;

import java.io.IOException;

import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
@SuppressWarnings("unused")
public class Migrate_MigrateCoreBridgeAndConfiguration_RIOT10609 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_MigrateCoreBridgeAndConfiguration_RIOT10609.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateCoreBridgeConfiguration();
        migrationAlienReaderRules();
    }

    private void migrateCoreBridgeConfiguration() {
        logger.info("Migrating Core Bridge Configuration");
        List<Edgebox> edgeboxes = EdgeboxService.getEdgeboxDAO().selectAll();
        for (Edgebox edgebox : edgeboxes) {

            if ("".equals(edgebox.getConfiguration())) {
                logger.info("The JSON configuration for bridge=" + edgebox.getCode()
                        + " is empty, skipping migration for this bridge...");
                continue;
            }

            //to add a comma when the text config is without it
            String textChangedEdgeBox = getChangeEdgeBox(edgebox.getCode(), edgebox.getConfiguration());
            if (textChangedEdgeBox.length() > 1) {
                edgebox.setConfiguration(textChangedEdgeBox);
            }
            String configurationMCB = edgebox.getConfiguration().replace(" ", "");
            ObjectMapper mapper = new ObjectMapper();
            JsonNode configurationNode;
            String[] toRemove = new String[]{"host", "port", "dbname", "username", "password"};
            List<Connection> connections = ConnectionService.getConnectionDAO().selectAll();
            try {
                configurationNode = mapper.readTree(configurationMCB);

                if (configurationNode.get("mqtt") != null) {

                    JsonNode jsonHost = configurationNode.get("mqtt").get("host");
                    if (jsonHost != null) {
                        String host = jsonHost.asText();
                        Integer port = Integer.parseInt(configurationNode.get("mqtt").get("port").asText());

                        for (Connection connection : connections) {
                            if (connection.getConnectionType().getCode().startsWith("MQTT")) {
                                if (connection.getPropertyAsString("host").equals(host) && connection
                                        .getPropertyAsNumber("port").equals(port)) {
                                    ((ObjectNode) configurationNode.get("mqtt")).put("connectionCode", connection
                                            .getCode());
                                }
                            }
                        }

                        if (configurationNode.get("mqtt").get("connectionCode") == null) {
                            Connection connection = new Connection();
                            connection.setCode("MQTT_FOR_" + edgebox.getCode());
                            connection.setName("MQTT for " + edgebox.getName());
                            connection.setGroup(edgebox.getGroup());
                            connection.setConnectionType(ConnectionTypeService.getInstance().getConnectionTypeByCode
                                    ("MQTT"));
                            connection.setProperties(getConfigForMqtt(host, port));
                            ConnectionService.getInstance().insert(connection);
                            ((ObjectNode) configurationNode.get("mqtt")).put("connectionCode", connection.getCode());
                        }

                        for (String aToRemove : toRemove) {
                            if (configurationNode.get("mqtt").get(aToRemove) != null) {
                                ((ObjectNode) configurationNode.get("mqtt")).remove(aToRemove);
                            }
                        }
                    }
                }

                if (configurationNode.get("mongo") != null) {
                    JsonNode jsonHost = configurationNode.get("mongo").get("host");
                    JsonNode jsonPort = configurationNode.get("mongo").get("port");
                    JsonNode jsonUsername = configurationNode.get("mongo").get("username");
                    JsonNode jsonPassword = configurationNode.get("mongo").get("password");

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

                        if (configurationNode.get("mongo").get("connectionCode") == null) {
                            Connection connection = new Connection();
                            connection.setCode("MONGO_FOR_" + edgebox.getCode());
                            connection.setName("Mongo DB for " + edgebox.getName());
                            connection.setGroup(edgebox.getGroup());
                            connection.setConnectionType(ConnectionTypeService.getInstance().getConnectionTypeByCode
                                    ("MONGO"));
                            connection.setProperties(getConfigForMongo(host, port, username, password));
                            ConnectionService.getInstance().insert(connection);
                            ((ObjectNode) configurationNode.get("mongo")).put("connectionCode", connection.getCode());
                        }

                        for (String aToRemove : toRemove) {
                            if (configurationNode.get("mongo").get(aToRemove) != null) {
                                ((ObjectNode) configurationNode.get("mongo")).remove(aToRemove);
                            }
                        }
                    }
                }

                if (configurationNode.get("ftp") != null) {

                    JsonNode jsonHost = configurationNode.get("ftp").get("host");
                    JsonNode jsonPort = configurationNode.get("ftp").get("port");
                    JsonNode jsonUsername = configurationNode.get("ftp").get("username");
                    JsonNode jsonPassword = configurationNode.get("ftp").get("password");

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

                        if (configurationNode.get("ftp").get("connectionCode") == null) {
                            Connection connection = new Connection();
                            connection.setCode("FTP_FOR_" + edgebox.getCode());
                            connection.setName("Ftp Server for " + edgebox.getName());
                            connection.setGroup(edgebox.getGroup());
                            connection.setConnectionType(ConnectionTypeService.getInstance().getConnectionTypeByCode
                                    ("FTP"));
                            connection.setProperties(getConfigForFtp(host, port, username, password));
                            ConnectionService.getInstance().insert(connection);
                            ((ObjectNode) configurationNode.get("ftp")).put("connectionCode", connection.getCode());
                        }

                        for (String aToRemove : toRemove) {
                            if (configurationNode.get("ftp").get(aToRemove) != null) {
                                ((ObjectNode) configurationNode.get("ftp")).remove(aToRemove);
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
                    ObjectNode checkMultilevelReferences = ((ObjectNode) configurationNode).putObject
                            ("checkMultilevelReferences");
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
                        && configurationNode.get("ftp") == null && configurationNode.get("geoforce") == null
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

    private String getConfigForMqtt(String host, Integer port) {
        return "{\"host\":\"" + host + "\",\"port\":" + port + ",\"secure\":false,\"qos\":1}";
    }

    private String getConfigForMongo(String host, Integer port, String username, String password) {
        String newPassword = org.apache.commons.codec.binary.Base64.encodeBase64String(password.getBytes(Charsets
                .UTF_8));
        return "{\"host\":\"" + host + "\",\"password\":\"" + newPassword + "\",\"dbname\":\"riot_main\",\"port\":" +
                port + ",\"username\":\"" + username + "\",\"secure\":false}";
    }

    private String getConfigForFtp(String host, Integer port, String username, String password) {
        String newPassword = org.apache.commons.codec.binary.Base64.encodeBase64String(password.getBytes(Charsets
                .UTF_8));
        return "{\"host\":\"" + host + "\",\"password\":\"" + newPassword + "\",\"port\":" + port + "," +
                "\"username\":\"" + username + "\",\"secure\":false}";
    }

    /**
     * @param edgeBoxCode          Edge box code to get change.
     * @param edgeBoxConfiguration EdgeBox configuration in string format.
     * @return a replaced text EdgeBox (to add a comma when the text is without it)
     */
    private String getChangeEdgeBox(String edgeBoxCode, String edgeBoxConfiguration) {
        String changedText = "";
        if (edgeBoxCode.equalsIgnoreCase("FTPBBYW")) {
            String regexProcess = "[\"processPolicy\": \"Move\"\n\"localBackupFolder\":]";
            String textProcessToReplace = "\"processPolicy\": \"Move\"";
            String textProcessRight = "\"processPolicy\": \"Move\",";
            Pattern pProcess = Pattern.compile(regexProcess);
            Matcher mProcess = pProcess.matcher(edgeBoxConfiguration);
            if (mProcess.find()) {
                changedText = edgeBoxConfiguration.replace(textProcessToReplace, textProcessRight);
            }
        }
        return changedText;
    }

    private void migrationAlienReaderRules() {
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
            } catch (Exception e) {
                logger.error("Error in migrate AlienReader", e);
            }
        }
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

}
