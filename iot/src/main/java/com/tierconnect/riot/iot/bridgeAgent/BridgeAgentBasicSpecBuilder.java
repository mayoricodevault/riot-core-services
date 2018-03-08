package com.tierconnect.riot.iot.bridgeAgent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tierconnect.riot.appcore.entities.Connection;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.services.ConnectionService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.commons.ConnectionConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jose4j.json.internal.json_simple.JSONObject;

import java.io.IOException;

/**
 * Created by cfernandez
 * on 2/6/17.
 */
public class BridgeAgentBasicSpecBuilder implements BridgeAgentSpecBuilder {

    private static Logger logger = Logger.getLogger(BridgeAgentSpecBuilder.class);

    public final static String COMMAND_FIELD = "command";
    public final static String BRIDGE_TYPE_FIELD = "bridgeType";
    public final static String BRIDGE_CODE_FIELD = "bridgeCode";

    /**
     * Define all the available commands to interact with a bridge
     */
    public enum Command {
        START("ON"),
        STOP("OFF"),
        ERROR("ERROR"),
        DEPLOY("DEPLOY");

        private String command;

        Command(String command){
            this.command = command;
        }

        public String getCommand(){
            return command;
        }
    }

    private String configuration;
    private String bridgeCode;
    private String bridgeType;
    private String command;
    private String bridgeStatus;
    private long groupId;
    private ObjectMapper objectMapper;

    public BridgeAgentBasicSpecBuilder(String configuration, String bridgeCode, String bridgeType, String bridgeStatus, long groupId) {
        this.configuration = configuration;
        this.bridgeCode = bridgeCode;
        this.bridgeType = bridgeType;
        this.bridgeStatus = bridgeStatus;
        this.groupId = groupId;
        objectMapper = new ObjectMapper();
    }

    @Override
    public String buildBody() {
        String body;
        command = "";
        logger.info("Bridge Status='" + bridgeStatus + "'");

        if (!StringUtils.isEmpty(bridgeStatus)
                && (bridgeStatus.equals(Command.START.getCommand()) || bridgeStatus.equals(Command.STOP.getCommand()) || bridgeStatus.equals(Command.ERROR.getCommand()))){
            command = bridgeStatus;
        }

        if (command.equals(Command.START.getCommand())){
            body = getBodyForSTART();
        }else if(command.equals(Command.STOP.getCommand())){
            body = getBodyForSTOP();
        }else if (command.equals(Command.ERROR.getCommand())){
            body = getBodyForSTART();
        }else{
            logger.debug("Command='" + command + "' is not supported.");
            body = "";
        }
        return body;
    }

    @Override
    public String buildTopic() {
        String topic;
        if (Command.START.getCommand().equals(command) || Command.STOP.getCommand().equals(command)){
            // setting default topic
            topic = "/v1/agent/default/control";
            String agentCode = getFieldFromConfiguration("bridgeAgent", "agentCode", configuration);
            logger.info("Agent code found in the configuration: '" + agentCode + "'");
            if (!agentCode.isEmpty()){
                // building topic based on the agent code
                topic = "/v1/agent/" + agentCode + "/control";
            }
            logger.info("Agent topic: '" + topic + "'");
        }else {
            logger.error("Command '" + command + "' is not supported...");
            topic = "";
        }
        return topic;
    }

    public String getBodyForSTOP(){
        JSONObject command = new JSONObject();
        command.put("bridgeCode", bridgeCode);
        command.put("command", Command.STOP.getCommand());
        return command.toJSONString();
    }

    public String getBodyForSTART(){
        String body = "";
        if (bridgeType.equals(BridgeType.ALE.getType())){
            body = buildStandardBody();
        }else if (bridgeType.equals(BridgeType.CORE.getType())){
            body = buildCoreBridgeBody();
        }else if (bridgeType.equals(BridgeType.FTP.getType())){
            body = buildStandardBody();
        }else if (bridgeType.equals(BridgeType.STAR_FLEX.getType())){
            body = buildStandardBody();
        }else if (bridgeType.equals(BridgeType.SARP.getType())){
            body = buildStandardBody();
        }else if (bridgeType.equals(BridgeType.MONGO_INJECTOR.getType())){
            body = buildStandardBody();
        }else {
            logger.error("BridgeType=" + bridgeType + " is not supported.");
        }
        logger.debug("Message for START=" + body);
        return body;
    }

    public JsonNode getStandardJsonNodeBody(){
        JsonNode rootNode = null;
        try {
            rootNode = objectMapper.readTree(configuration);
        } catch (IOException e) {
            logger.error("",e);
        }

        if (rootNode == null){
            return null;
        }

        addAttribute(rootNode, COMMAND_FIELD, command);
        addAttribute(rootNode, BRIDGE_CODE_FIELD, bridgeCode);
        addAttribute(rootNode, BRIDGE_TYPE_FIELD, bridgeType);

        Connection serviceConnection = getServiceConnection();
        if (serviceConnection == null){
            return null;
        }
        String servicesHost = serviceConnection.getPropertyAsString(ConnectionConstants.SERVICES_HOST);
        int servicesPort = serviceConnection.getPropertyAsNumber(ConnectionConstants.SERVICES_PORT);
        String servicesApiKey = serviceConnection.getPropertyAsString(ConnectionConstants.SERVICES_APIKEY);

        addAttribute(rootNode, "httpHost", servicesHost);
        addAttribute(rootNode, "httpPort", servicesPort);
        addAttribute(rootNode, "apikey", servicesApiKey);

        return rootNode;
    }

    public String buildStandardBody(){
        JsonNode rootNode = getStandardJsonNodeBody();
        return rootNode.toString();
    }

    private String buildCoreBridgeBody(){
        JsonNode rootNode = getStandardJsonNodeBody();
        Connection connection = getMyqlConnection();
        if (connection == null){
            return null;
        }

        // Adding Hazelcast parameters
        String driver = connection.getPropertyAsString(ConnectionConstants.SQL_DRIVER);
        String dialect = connection.getPropertyAsString(ConnectionConstants.SQL_DIALECT);
        String username = connection.getPropertyAsString(ConnectionConstants.SQL_USERNAME);
        String password = connection.getPassword(false);
        String url = connection.getPropertyAsString(ConnectionConstants.SQL_URL);
        String hcNativeClientAddress = connection.getPropertyAsString(ConnectionConstants.SQL_HZ);

        addAttribute(rootNode, "hibernate.connection.driver_class", driver);
        addAttribute(rootNode, "hibernate.connection.url", url);
        addAttribute(rootNode, "hibernate.connection.username", username);
        addAttribute(rootNode, "hibernate.connection.password", password);
        addAttribute(rootNode, "hibernate.dialect", dialect);
        addAttribute(rootNode, "hibernate.cache.hazelcast.native_client_address", hcNativeClientAddress);

        return rootNode.toString();
    }

    private void addAttribute(JsonNode rootNode, String fieldName, String fieldValue){
        ((ObjectNode)(rootNode)).put(fieldName, fieldValue);
    }

    private void addAttribute(JsonNode rootNode, String fieldName, int fieldValue){
        ((ObjectNode)(rootNode)).put(fieldName, fieldValue);
    }

    private Connection getServiceConnection(){
        String serviceConnectionCode = getFieldFromConfiguration("bridgeStartupOptions","servicesConnectionCode", configuration);
        if (StringUtils.isEmpty(serviceConnectionCode)){
            logger.error("Cannot be found servicesConnectionCode configuration entry, please review your coreBridge " +
                    "configuration an add this parameter, bridgeCode=" + bridgeCode);
            return null;
        }
        GroupService groupService = GroupService.getInstance();
        Group group = groupService.get(groupId);
        Connection connection = getConnection(serviceConnectionCode, group);
        return connection;
    }

    private Connection getMyqlConnection(){
        String mysqlConnectionCode = getFieldFromConfiguration("bridgeStartupOptions","sqlConnectionCode", configuration);
        if (StringUtils.isEmpty(mysqlConnectionCode)){
            logger.error("Cannot be found sqlConnectionCode configuration entry, please review your coreBridge " +
                    "configuration an add this parameter, bridgeCode=" + bridgeCode);
            return null;
        }

        GroupService groupService = GroupService.getInstance();
        Group group = groupService.get(groupId);
        Connection connection = getConnection(mysqlConnectionCode, group);
        return connection;
    }

    /**
     * Try to get the connection according to the group of the edgebox, if it does not exist, it will be taken the connection
     * based on the tenant group or based on the root group
     * @param mysqlConnectionCode mysqlConnectionCode
     * @param group edgebox group
     * @return Connection
     */
    public Connection getConnection(String mysqlConnectionCode, Group group){
        ConnectionService connectionService = ConnectionService.getInstance();
        Connection connection = connectionService.getByCodeAndGroup(mysqlConnectionCode, group);
        if (connection == null){
            logger.warn(String.format("Cannot found connection with code='%s' and groupCode='%s', trying to get using " +
                    "the Tenant group instead", mysqlConnectionCode, group.getCode()));
            // trying to get the connection of the Tenant
            connection = connectionService.getByCodeAndGroup(mysqlConnectionCode, group.getParentLevel2());
            if (connection == null) {
                logger.warn(String.format("Cannot found connection with code='%s' and groupCode='%s', trying to get using " +
                        "the root group instead", mysqlConnectionCode, group.getParentLevel2().getCode()));
                // trying to get connection of root
                // todo: kafka.enabled review with system connections
                connection = connectionService.getByCodeAndGroup(mysqlConnectionCode, group.getParentLevel1());
                if (connection == null) {
                    logger.error(String.format("Cannot found connection with code='%s' and groupCode='root', AGENT CANNOT " +
                            "SEND start/stop signals", mysqlConnectionCode));
                    return null;
                }
            }
        }
        return connection;
    }

    public String getFieldFromConfiguration(String fieldName, String configuration){
        String field = "";
        try {
            JsonNode configurationJson = objectMapper.readTree(configuration);
            field = configurationJson.get(fieldName).asText();
        } catch (Exception e) {
            logger.error("Field='" + fieldName + "' cannot be found in the configuration.");
        }
        return field;
    }

    public String getFieldFromConfiguration(String parentField, String fieldName, String configuration){
        String field = "";
        try {
            JsonNode configurationJson = objectMapper.readTree(configuration);
            JsonNode parentFieldConfig = configurationJson.get(parentField);
            field = parentFieldConfig.get(fieldName).asText();
        } catch (Exception e) {
            logger.error("Field='" + fieldName + "' cannot be found in the configuration.");
        }
        return field;
    }

}
