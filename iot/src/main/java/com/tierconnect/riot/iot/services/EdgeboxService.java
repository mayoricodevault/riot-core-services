package com.tierconnect.riot.iot.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.tierconnect.riot.appcore.entities.Connection;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.LicenseDetail;
import com.tierconnect.riot.appcore.services.*;
import com.tierconnect.riot.appcore.utils.EntityVisibility;
import com.tierconnect.riot.appcore.utils.GeneralVisibilityUtils;
import com.tierconnect.riot.appcore.utils.VisibilityUtils;
import com.tierconnect.riot.commons.ConnectionConstants;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.commons.services.broker.BrokerEdgeBox;
import com.tierconnect.riot.commons.services.broker.MQTTEdgeConnectionPool;
import com.tierconnect.riot.commons.utils.JsonUtils;
import com.tierconnect.riot.iot.bridgeAgent.BridgeAgentService;
import com.tierconnect.riot.iot.bridgeAgent.BridgeType;
import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.entities.QEdgebox;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.utils.BeanUtils;
import com.tierconnect.riot.sdk.utils.RestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.ftp.FTPSClient;
import org.jose4j.json.internal.json_simple.JSONArray;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;

import javax.ws.rs.core.Response;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.IOException;
import java.util.*;

import org.apache.log4j.Logger;

import static com.mysema.query.group.GroupBy.groupBy;
import static com.mysema.query.group.GroupBy.list;

public class EdgeboxService extends EdgeboxServiceBase
{
    private Logger logger = Logger.getLogger(String.valueOf(EdgeboxService.class));
    private String[] restrictedTypes = {"SARP", "Mongo_Injector"};
    private String dummyValue = "-1";
    private ObjectMapper objectMapper = new ObjectMapper();

	public void refreshConfiguration( String bridgeCode, String bridgeType, String bridgeStatus, Group group,
                                      Map<String, Object> publicMap, boolean publishMessage ) throws IOException,IllegalArgumentException {

        BrokerClientHelper.initLazyLoading();
        String bridgeConfiguration = publicMap.get( "configuration" ).toString();
		BrokerClientHelper.sendRefreshEdgebox( bridgeCode, bridgeConfiguration, publishMessage,
                GroupService.getInstance().getMqttGroups(group));

        statusServiceAction(bridgeConfiguration, bridgeCode, bridgeType, bridgeStatus, group.getId());
	}

    /**
     * Send the action to Start or Stop the bridge
     * @param bridgeConfiguration
     * @param bridgeCode
     * @param bridgeType
     * @param bridgeStatus
     * @param groupId
     */
    public void statusServiceAction(String bridgeConfiguration, String bridgeCode, String bridgeType,
                                    String bridgeStatus, Long groupId){
        BridgeAgentService bridgeAgentService = new BridgeAgentService();
        bridgeAgentService.doAction(bridgeConfiguration, bridgeCode, bridgeType, bridgeStatus, groupId);
        if (bridgeStatus != null) {
            if (bridgeStatus.equals("ON")){
                StatusService.getInstance().start(bridgeCode);
            }
            if (bridgeStatus.equals("OFF")){
                StatusService.getInstance().stop(bridgeCode);
            }
        }
    }

    public Edgebox selectByNameAndGroup(String name, String groupCode){
        HibernateQuery query = getEdgeboxDAO().getQuery();
        return query.where(QEdgebox.edgebox.name.eq(name).and(QEdgebox.edgebox.group.code.eq(groupCode)))
                .uniqueResult(QEdgebox.edgebox);
    }

    public List<Edgebox> getByType(String type){
        HibernateQuery query = getEdgeboxDAO().getQuery();
        return query.where(QEdgebox.edgebox.type.eq(type)).list(QEdgebox.edgebox);
    }

    public List<Edgebox> getByTenant(Group group){
        HibernateQuery query = getEdgeboxDAO().getQuery();
        Group tenant = group;
        if(tenant.getTreeLevel() > 1){
            tenant = tenant.getParentLevel2();
        }
        return query.where(QEdgebox.edgebox.group.eq(tenant)).list(QEdgebox.edgebox);
    }

    public Edgebox selectByCode(String code){
        HibernateQuery query = getEdgeboxDAO().getQuery();
        return query.where(QEdgebox.edgebox.code.eq(code))
                .uniqueResult(QEdgebox.edgebox);
    }

    public List<Edgebox> selectAll(){
        return getEdgeboxDAO().selectAll();
    }

    public Boolean existBridgeCode(String code, Long excludeId)
    {
        BooleanBuilder be = new BooleanBuilder();
        if (excludeId != null){
            be.and(QEdgebox.edgebox.id.ne(excludeId));
        }
        be.and(QEdgebox.edgebox.code.eq(code));
        return getEdgeboxDAO().getQuery().where(be).exists();
    }

    public Boolean existBridgeName(String name, String groupCode, Long excludeId)
    {
        BooleanBuilder be = new BooleanBuilder();
        if (excludeId != null){
            be.and(QEdgebox.edgebox.id.ne(excludeId));
        }
        be.and(QEdgebox.edgebox.name.eq(name)).and(QEdgebox.edgebox.group.code.eq(groupCode));
        return getEdgeboxDAO().getQuery().where(be).exists();
    }

    /**
     * This Method checks if a Bridge exists by code
     * @param code
     * @param groupCode
     * @param excludeId
     * @return
     */
    public Boolean existBridgeCode(String code, String groupCode, Long excludeId)
    {
        BooleanBuilder be = new BooleanBuilder();
        if (excludeId != null){
            be.and(QEdgebox.edgebox.id.ne(excludeId));
        }
        be.and(QEdgebox.edgebox.code.eq(code)).and(QEdgebox.edgebox.group.code.eq(groupCode));
        return getEdgeboxDAO().getQuery().where(be).exists();
    }

    public Edgebox getMaxPortBridges() {
        List<Edgebox> edgeboxList = getEdgeboxDAO().getQuery().orderBy(QEdgebox.edgebox.port.desc()).list(QEdgebox.edgebox);
        if (!edgeboxList.isEmpty()) {
            return edgeboxList.get(0);
        }
        return null;
    }

    public Map<Group, List<Edgebox>> getEdgeBoxGroupByGroup(String type) {
        HibernateQuery query = getEdgeboxDAO().getQuery();
        query.where(QEdgebox.edgebox.type.eq(type));
        return query.transform(groupBy(QEdgebox.edgebox.group).as(list(QEdgebox.edgebox)));
    }

    /**
     * Populate Bridge Type
     * @param group
     * @param name
     * @param code
     * @param type
     * @param configuration
     * @param port
     * @return
     */
    public Edgebox insertEdgebox(Group group, String name, String code, String type, String configuration, Long port )
    {
        Edgebox b = new Edgebox();
        b.setName( name );
        b.setCode( code );
        b.setGroup( group );
        b.setConfiguration( configuration );
        b.setParameterType(Constants.BRIDGE_TYPE);
        b.setType( type );
        b.setPort(port);
        EdgeboxService.getInstance().insert( b );
        this.addMqttConnectionPool(b);
        return b;
    }

    /**
     *
     * @param code
     * @return
     */
    public Edgebox getByCode(String code){
        HibernateQuery query = getEdgeboxDAO().getQuery();
        return query.where(QEdgebox.edgebox.code.eq(code)).uniqueResult(QEdgebox.edgebox);
    }

    public Map<String, Object> getConfiguration(String code) {

        //1. Get edbox by code
        Edgebox edgebox = EdgeboxService.getInstance().selectByCode( code );
        if( edgebox == null )
        {
        	throw new UserException(String.format( "EdgeboxCode[%s] not found", code ));
        }

		//2. Deserialize edgebox configuration into a map
        ObjectMapper mapper = new ObjectMapper();
        Map<String,Object> configMap = null;
        try {
            configMap = mapper.readValue(edgebox.getConfiguration(), Map.class);
        } catch (IOException e) {
            throw new UserException(String.format( "EdgeboxCode[%s] has an error when retrieving configuration. ", code ));
        }

        configMap.put("id",edgebox.getId());
        configMap.put("code",edgebox.getCode());

        //3. Get connections by connectionCode
        if(configMap != null){
            //4. Complete map for mqtt
            if(configMap.containsKey(ConnectionConstants.MQTT)){
                Map<String,Object> mqttConnection = (Map<String,Object>)configMap.get(ConnectionConstants.MQTT);
                if(mqttConnection.containsKey(ConnectionConstants.CONNECTION_CODE)){
                    String connectionCode = String.valueOf(mqttConnection.get(ConnectionConstants.CONNECTION_CODE));
                    Connection connection = ConnectionService.getInstance().getByCode(connectionCode);
                    if(connection != null){
                        mqttConnection.put(ConnectionConstants.MQTT_HOST,connection.getProperty(ConnectionConstants.MQTT_HOST));
                        mqttConnection.put(ConnectionConstants.MQTT_PORT,connection.getProperty(ConnectionConstants.MQTT_PORT));
                        mqttConnection.put(ConnectionConstants.MQTT_QOS,connection.getProperty(ConnectionConstants.MQTT_QOS));
                        mqttConnection.put(ConnectionConstants.MQTT_SECURE,connection.getProperty(ConnectionConstants.MQTT_SECURE));
                        configMap.put(ConnectionConstants.MQTT,mqttConnection);
                    }
                }
            }

            //5. Complete map for kafka
            if(configMap.containsKey(ConnectionConstants.KAFKA)){
                Map<String,Object> kafkaConnection = (Map<String,Object>)configMap.get(ConnectionConstants.KAFKA);
                if(kafkaConnection.containsKey(ConnectionConstants.CONNECTION_CODE)){
                    String connectionCode = String.valueOf(kafkaConnection.get(ConnectionConstants.CONNECTION_CODE));
                    Connection connection = ConnectionService.getInstance().getByCode(connectionCode);
                    if(connection != null){
                        kafkaConnection.put(ConnectionConstants.KAFKA_ZOOKEEPER,connection.getProperty(ConnectionConstants.KAFKA_ZOOKEEPER));
                        kafkaConnection.put(ConnectionConstants.KAFKA_SERVER,connection.getProperty(ConnectionConstants.KAFKA_SERVER));
                        kafkaConnection.put(ConnectionConstants.KAFKA_TOPICS,connection.getProperty(ConnectionConstants.KAFKA_TOPICS));
                        configMap.put(ConnectionConstants.KAFKA,kafkaConnection);
                    }
                }
            }

            //6. Complete map for services
            if(configMap.containsKey(ConnectionConstants.SERVICES)){
                Map<String,Object> servicesConnection = (Map<String,Object>)configMap.get(ConnectionConstants.SERVICES);
                if(servicesConnection.containsKey(ConnectionConstants.CONNECTION_CODE)){
                    String connectionCode = String.valueOf(servicesConnection.get(ConnectionConstants.CONNECTION_CODE));
                    Connection connection = ConnectionService.getInstance().getByCode(connectionCode);
                    if(connection != null){
                        servicesConnection.put(ConnectionConstants.SERVICES_HOST,connection.getProperty(ConnectionConstants.SERVICES_HOST));
                        servicesConnection.put(ConnectionConstants.SERVICES_PORT,connection.getProperty(ConnectionConstants.SERVICES_PORT));
                        servicesConnection.put(ConnectionConstants.SERVICES_CONTEXT_PATH,connection.getProperty(ConnectionConstants.SERVICES_CONTEXT_PATH));
                        servicesConnection.put(ConnectionConstants.SERVICES_APIKEY,connection.getProperty(ConnectionConstants.SERVICES_APIKEY));
                        servicesConnection.put(ConnectionConstants.SERVICES_SECURE,connection.getProperty(ConnectionConstants.SERVICES_SECURE));
                        configMap.put(ConnectionConstants.SERVICES,servicesConnection);
                    }
                }
            }

            //7. Complete map for mongo
            if(configMap.containsKey(ConnectionConstants.MONGO)){
                Map<String,Object> mongoConnection = (Map<String,Object>)configMap.get(ConnectionConstants.MONGO);
                if(mongoConnection.containsKey(ConnectionConstants.CONNECTION_CODE)){
                    String connectionCode = String.valueOf(mongoConnection.get(ConnectionConstants.CONNECTION_CODE));
                    Connection connection = ConnectionService.getInstance().getByCode(connectionCode);
                    if(connection != null){
                    	mongoConnection.put("mongoPrimary",connection.getPropertyAsString(ConnectionConstants.MONGO_PRIMARY));
                        mongoConnection.put("mongoSecondary",connection.getPropertyAsString(ConnectionConstants.MONGO_SECONDARY));
                        mongoConnection.put("mongoReplicaSet",connection.getPropertyAsString(ConnectionConstants.MONGO_REPLICASET));
                        mongoConnection.put("mongoSSL",connection.getPropertyAsBoolean(ConnectionConstants.MONGO_SSL));
                        mongoConnection.put("username",connection.getPropertyAsString(ConnectionConstants.MONGO_USERNAME));
                        mongoConnection.put("password",connection.getPropertyAsString(ConnectionConstants.MONGO_PASSWORD));
                        mongoConnection.put("mongoAuthDB",connection.getPropertyAsString(ConnectionConstants.MONGO_AUTHDB));
                        mongoConnection.put("mongoDB",connection.getPropertyAsString(ConnectionConstants.MONGO_DB));
                        mongoConnection.put("mongoSharding",connection.getPropertyAsBoolean(ConnectionConstants.MONGO_SHARDING));
                        mongoConnection.put("mongoConnectTimeout",connection.getPropertyAsNumber(ConnectionConstants.MONGO_CONNTIMEOUT));
                        mongoConnection.put("mongoMaxPoolSize",connection.getPropertyAsNumber(ConnectionConstants.MONGO_MAXPOOLSIZE));

                        configMap.put(ConnectionConstants.MONGO,mongoConnection);
                    }
                }
            }

            //8. Complete map for ftp
            if(configMap.containsKey(ConnectionConstants.FTP)){
                Map<String,Object> ftpConnection = (Map<String,Object>)configMap.get(ConnectionConstants.FTP);
                if(ftpConnection.containsKey(ConnectionConstants.CONNECTION_CODE)){
                    String connectionCode = String.valueOf(ftpConnection.get(ConnectionConstants.CONNECTION_CODE));
                    Connection connection = ConnectionService.getInstance().getByCode(connectionCode);
                    if(connection != null){
                        ftpConnection.put(ConnectionConstants.FTP_HOST,connection.getProperty(ConnectionConstants.FTP_HOST));
                        ftpConnection.put(ConnectionConstants.FTP_PORT,connection.getProperty(ConnectionConstants.FTP_PORT));
                        ftpConnection.put(ConnectionConstants.FTP_USERNAME,connection.getProperty(ConnectionConstants.FTP_USERNAME));
                        ftpConnection.put(ConnectionConstants.FTP_PASSWORD,connection.getPassword(false));
                        ftpConnection.put(ConnectionConstants.FTP_SECURE,connection.getProperty(ConnectionConstants.FTP_SECURE));
                        configMap.put(ConnectionConstants.FTP,ftpConnection);
                    }
                }
            }

            //9. Complete map for hadoop
            if(configMap.containsKey(ConnectionConstants.HADOOP)){
                Map<String,Object> hadoopConnection = (Map<String,Object>)configMap.get(ConnectionConstants.HADOOP);
                if(hadoopConnection.containsKey(ConnectionConstants.CONNECTION_CODE)){
                    String connectionCode = String.valueOf(hadoopConnection.get(ConnectionConstants.CONNECTION_CODE));
                    Connection connection = ConnectionService.getInstance().getByCode(connectionCode);
                    if(connection != null){
                        hadoopConnection.put(ConnectionConstants.HADOOP_HOST,connection.getProperty(ConnectionConstants.HADOOP_HOST));
                        hadoopConnection.put(ConnectionConstants.HADOOP_PORT,connection.getProperty(ConnectionConstants.HADOOP_PORT));
                        hadoopConnection.put(ConnectionConstants.HADOOP_PATH,connection.getProperty(ConnectionConstants.HADOOP_PATH));
                        hadoopConnection.put(ConnectionConstants.HADOOP_SECURE,connection.getProperty(ConnectionConstants.HADOOP_SECURE));
                        configMap.put(ConnectionConstants.HADOOP,hadoopConnection);
                    }
                }
            }

            //10. Complete map for SQL
            if(configMap.containsKey(ConnectionConstants.SQL)){
                Map<String,Object> hadoopConnection = (Map<String,Object>)configMap.get(ConnectionConstants.SQL);
                if(hadoopConnection.containsKey(ConnectionConstants.CONNECTION_CODE)){
                    String connectionCode = String.valueOf(hadoopConnection.get(ConnectionConstants.CONNECTION_CODE));
                    Connection connection = ConnectionService.getInstance().getByCode(connectionCode);
                    if(connection != null){
                        hadoopConnection.put(ConnectionConstants.SQL_DRIVER,connection.getProperty(ConnectionConstants.SQL_DRIVER));
                        hadoopConnection.put(ConnectionConstants.SQL_DIALECT,connection.getProperty(ConnectionConstants.SQL_DIALECT));
                        hadoopConnection.put(ConnectionConstants.SQL_USERNAME,connection.getProperty(ConnectionConstants.SQL_USERNAME));
                        hadoopConnection.put(ConnectionConstants.SQL_PASSWORD,connection.getProperty(ConnectionConstants.SQL_PASSWORD));
                        hadoopConnection.put(ConnectionConstants.SQL_URL,connection.getProperty(ConnectionConstants.SQL_URL));
                        hadoopConnection.put(ConnectionConstants.SQL_HZ,connection.getProperty(ConnectionConstants.SQL_HZ));
                        configMap.put(ConnectionConstants.SQL,hadoopConnection);
                    }
                }
            }

            //11. Complete map for SPARK
            if(configMap.containsKey(ConnectionConstants.SPARK)){
                Map<String,Object> sparkConnection = (Map<String,Object>)configMap.get(ConnectionConstants.SPARK);
                if(sparkConnection.containsKey(ConnectionConstants.CONNECTION_CODE)){
                    String connectionCode = String.valueOf(sparkConnection.get(ConnectionConstants.CONNECTION_CODE));
                    Connection connection = ConnectionService.getInstance().getByCode(connectionCode);
                    if(connection != null){
                        sparkConnection.put(ConnectionConstants.SPARK_MASTER_HOST,connection.getProperty(ConnectionConstants.SPARK_MASTER_HOST));
                        sparkConnection.put(ConnectionConstants.SPARK_PORT,connection.getProperty(ConnectionConstants.SPARK_PORT));
                        // TODO: Disable restart app in spark and remove extra configuration.
                        // sparkConnection.put(ConnectionConstants.SPARK_APPLICATION_ID,connection.getProperty(ConnectionConstants.SPARK_APPLICATION_ID));
                        // sparkConnection.put(ConnectionConstants.SPARK_DRIVER_HOST,connection.getProperty(ConnectionConstants.SPARK_DRIVER_HOST));
                        // sparkConnection.put(ConnectionConstants.SPARK_EXECUTOR_MEMORY,connection.getProperty(ConnectionConstants.SPARK_EXECUTOR_MEMORY));
                        // sparkConnection.put(ConnectionConstants.SPARK_TOTAL_EXECUTOR_CORES,connection.getProperty(ConnectionConstants.SPARK_TOTAL_EXECUTOR_CORES));
                        // sparkConnection.put(ConnectionConstants.SPARK_EXECUTOR_CORES,connection.getProperty(ConnectionConstants.SPARK_EXECUTOR_CORES));
                        // sparkConnection.put(ConnectionConstants.SPARK_SCHEDULER_MODE,connection.getProperty(ConnectionConstants.SPARK_SCHEDULER_MODE));
                        // sparkConnection.put(ConnectionConstants.SPARK_NUM_EXECUTORS,connection.getProperty(ConnectionConstants.SPARK_NUM_EXECUTORS));
                        // sparkConnection.put(ConnectionConstants.SPARK_BATCH_INTERVAL,connection.getProperty(ConnectionConstants.SPARK_BATCH_INTERVAL));
                        // sparkConnection.put(ConnectionConstants.SPARK_WRITE_TO_MONGO,connection.getProperty(ConnectionConstants.SPARK_WRITE_TO_MONGO));
                        // sparkConnection.put(ConnectionConstants.SPARK_CONSUMER_POLL_MS,connection.getProperty(ConnectionConstants.SPARK_CONSUMER_POLL_MS));
                        configMap.put(ConnectionConstants.SPARK,sparkConnection);
                    }
                }
            }
            //12. verify if a coreBridge has a license valid for ESPER Rule
            if (edgebox.getType().equalsIgnoreCase("core")){
            	// esper license
                boolean isAvailable = false;
                Group group = edgebox.getGroup();
				LicenseDetail licenseDetail = LicenseService.getInstance().getLicenseDetail(group,true);
                if (licenseDetail != null) {
                    List<String> features =licenseDetail.getFeatures();
                    String feature=features.stream().filter( f -> f.equals("Esper Rules Plugin")).findAny().orElse("");
                    if (!feature.isEmpty()){
                        // is a valid license wih ESPER
                        isAvailable = true;
                    }
                }
                configMap.put("isValidLicenceEsper",isAvailable);

                // executeRulesForLastDetectTime
                Boolean executeRulesForLastDetectTime = ConfigurationService.getAsBoolean(GroupService.getInstance().getRootGroup(), "executeRulesForLastDetectTime");
                configMap.put("executeRulesForLastDetectTime", executeRulesForLastDetectTime);
            }
        }
		return configMap;
	}

	public String validateFtp(JSONObject configuration){
        String message = null;
        String path = ((JSONObject) configuration.get("remoteFTPFolders")).get("source").toString();
        String connectionCode = ((JSONObject)configuration.get("ftp")).get("connectionCode").toString();
        String destinationFolder = ((JSONObject) configuration.get("remoteFTPFolders")).get("destination").toString();

        if (path == null){
            return  "FTP Folder Source value is empty.";
        }
        if (connectionCode == null){
            return  "FTP Connection Code value is empty.";
        }
        //TODO: make validation dynamically dependant
        if (destinationFolder == null && String.valueOf(configuration.get("processingType")).equals("full")){
            return  "FTP Destination Folder value is empty.";
        }

        Connection connection = ConnectionService.getInstance().getByCode(connectionCode);

        if (connection == null){
            return "FTP Connection Code is invalid.";
        }
        String secure = connection.getPropertyAsString("secure");
        if (secure.equals("true")){
            FTPSClient ftpsClient = new FTPSClient();
            try {
                ftpsClient.connect(connection.getPropertyAsString("host"));
                if (!FTPReply.isPositiveCompletion(ftpsClient.getReplyCode())) {
                    return "It is not possible to connect to FTP Server, Code response " + ftpsClient.getReplyCode();
                }
                boolean loggedIn = ftpsClient.login(connection.getPropertyAsString("username"), connection.getPassword(false));
                if (!loggedIn) {
                    return "Could not login to FTP Server";
                }
                ftpsClient.changeWorkingDirectory(destinationFolder);
                if (ftpsClient.getReplyCode() == 550 && String.valueOf(configuration.get("processingType")).equals("full")) {
                    return destinationFolder + " directory is invalid for FTP Server " + connectionCode;
                }

                ftpsClient.changeWorkingDirectory(path);
                if (ftpsClient.getReplyCode() == 550) {
                    return path + " directory is invalid for FTP Server " + connectionCode;
                }

            } catch (IOException e) {
                return "It is not possible to connect to FTP Server" + e.getMessage();
            } finally {
                if (ftpsClient != null && ftpsClient.isConnected()) {
                    try {
                        ftpsClient.logout();
                        ftpsClient.disconnect();
                    } catch (IOException e) {
                        throw new UserException(e.getMessage());
                    }
                }
            }

        }else {

            FTPClient ftpClient = new FTPClient();
            try {
                ftpClient.connect(connection.getPropertyAsString("host"), connection.getPropertyAsNumber("port"));
                if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
                    return "It is not possible to connect to FTP Server, Code response " + ftpClient.getReplyCode();
                }
                boolean loggedIn = ftpClient.login(connection.getPropertyAsString("username"), connection.getPassword(false));
                if (!loggedIn) {
                    return "Could not login to FTP Server";
                }
                ftpClient.changeWorkingDirectory(destinationFolder);
                if (ftpClient.getReplyCode() == 550 && String.valueOf(configuration.get("processingType")).equals("full")) {
                    return destinationFolder + " directory is invalid for FTP Server " + connectionCode;
                }

                ftpClient.changeWorkingDirectory(path);
                if (ftpClient.getReplyCode() == 550) {
                    return path + " directory is invalid for FTP Server " + connectionCode;
                }
            } catch (IOException e) {
                return "It is not possible to connect to FTP Server" + e.getMessage();
            } finally {
                if (ftpClient != null && ftpClient.isConnected()) {
                    try {
                        ftpClient.logout();
                        ftpClient.disconnect();
                    } catch (IOException e) {
                        throw new UserException(e.getMessage());
                    }
                }
            }
        }

        return message;
    }

    public Response updateEdgebox(Long id, Map<String, Object> map, EntityVisibility<Edgebox> entityVisibility) {
        // 1[b,c]. Restrict access based on OBJECT and PROPERTY level write
        // permissions
        //QueryUtils.filterWritePermissions( Edgebox.class, map );
        Edgebox edgebox = EdgeboxService.getInstance().get(id);
        if (edgebox == null) {
            return RestUtils.sendBadResponse(String.format("EdgeboxId[%d] not found", id));
        }

        // "coreBridge" is a fake parameter in the configuration which will not have to be saved
        String previousCoreBridgeCode = EdgeboxService.getInstance().getRelatedCoreBridgeCode(edgebox.getCode(), edgebox.getGroup());

        // 2. Limit visibility based on user's group and the object's group
        // (group based authorization)
        GeneralVisibilityUtils.limitVisibilityUpdate(entityVisibility, edgebox, VisibilityUtils.getObjectGroup(map));
        // Adds the field numberOfThreads in configuration for the core bridges and ale bridges

        // RIOT-12660: Services > Do not permit save the property "numberOfThreads" with zero or null
        String bridgeType = edgebox.getType();
        if (bridgeType.equalsIgnoreCase("core") || bridgeType.equalsIgnoreCase("edge")) {
            if (map.get("configuration") != null && map.get("configuration").toString().contains("\"numberOfThreads\":0")) {
                map.replace("configuration", map.get("configuration").toString().replace("\"numberOfThreads\":0", "\"numberOfThreads\":1"));
                logger.info("numberOfThreads invalid, return default value numberOfThreads = 1");
            }
            if (map.get("configuration") != null && map.get("configuration").toString().contains("\"numberOfThreads\":null")) {
                map.replace("configuration", map.get("configuration").toString().replace("\"numberOfThreads\":null", "\"numberOfThreads\":1"));
                logger.info("numberOfThreads invalid, return default value numberOfThreads = 1");
            }

            if (map.get("configuration") != null && !map.get("configuration").toString().contains("\"numberOfThreads\"")) {
                map.replace("configuration", map.get("configuration").toString().replaceFirst("\\{", "{\"numberOfThreads\":1,"));
                logger.info("numberOfThreads invalid, return default value numberOfThreads = 1");
            }
        }
        // validate configuration as valid json
        if (map.get("configuration") != null && map.get("configuration") instanceof String) {
            String configuration = (String) map.get("configuration");
            if (!JsonUtils.isValidJSON(configuration)) {
                throw new UserException("Configuration is not valid");
            }
        }

        String validationMessage = validationMessage(edgebox.getParameterType(), bridgeType, map);
        if (validationMessage == null) {
            // 7. handle insert and update
            BeanUtils.setProperties(map, edgebox);

            String coreBridgeCode = getCoreBridgeCode(edgebox.getConfiguration(), edgebox.getType());
            EdgeboxService.getInstance().removeCoreBridgeCodeFakeParam(edgebox);

            // 6. handle validation in an Extensible manner
            validateUpdate(edgebox);
            JSONParser parser = new JSONParser();
            try {
                edgebox.setConfiguration(cleanDummyValues(
                    (JSONObject) parser.parse(edgebox.getConfiguration())).toJSONString());
            } catch (ParseException ignored) {
            }
            EdgeboxService.getInstance().update(edgebox);
            EdgeboxService.getInstance().updateFavorite(edgebox);
            RecentService.getInstance().updateName(edgebox.getId(), edgebox.getName(), "edgebox");
            Map<String, Object> publicMap = edgebox.publicMap();

            try {
                EdgeboxService.getInstance().refreshConfiguration(edgebox.getCode(),
                                                                edgebox.getType(), edgebox.getStatus(),
                                                                edgebox.getGroup(), publicMap, false);
                try {
                    Map<String, Object> configuration = EdgeboxService.getInstance().getConfiguration(edgebox.getCode());
                    updateRelatedCoreBridgeConfiguration(edgebox.getType(), edgebox.getCode(), coreBridgeCode, previousCoreBridgeCode);

                    EdgeboxService.getInstance().refreshEdgeboxCache(edgebox, configuration, false);
                } catch (JsonProcessingException e) {
                    logger.error("[Kafka] Unable update edgebox and edgebox configuration cache.",e);
                }
            } catch (IOException ioe) {
                logger.error("Occurred an error. Reason:", ioe);
            } catch (IllegalArgumentException iae) {
                logger.error("Occurred an error. Reason:", iae);
            } catch (Exception e) {
                logger.error("Occurred an error. Reason:", e);
            }

            return RestUtils.sendOkResponse(publicMap);
        } else {
            return RestUtils.sendBadResponse(validationMessage);
        }
    }

    /**
     *
     * @param edgebox
     * @param configuration it is edgebox cofiguration
     * @param delete if it is true it means send a payload null.
     */
    public void refreshEdgeboxCache(Edgebox edgebox, Map<String, Object> configuration, boolean delete) throws JsonProcessingException {

        BrokerClientHelper.refreshEdgeboxCache(edgebox, delete);
        BrokerClientHelper.refreshEdgeboxConfigurationCache(edgebox,configuration,delete);
    }

    public String validationMessage(String parameterType, String type, Map<String, Object> map){
        List<String> invalidFields = new ArrayList<>();
        JSONParser parser = new JSONParser();
        JSONObject parameter;
        try {
            parameter = (JSONObject) parser.parse(ParametersService.getInstance()
                .getByCategoryAndCode(parameterType, type).getValue());
            for (Object section : parameter.keySet()) {
                invalidFields.addAll(validateParameterField((JSONObject) parameter.get(section),
                    (JSONObject)parser.parse(map.get("configuration").toString()),
                    (JSONObject)parser.parse(map.get("configuration").toString()), null));
            }
            int invalidSize = invalidFields.size();
            if (invalidSize == 0){
                if (type.equals("FTP")){
                    return EdgeboxService.getInstance().validateFtp((JSONObject)parser.parse(map.get("configuration").toString()));
                }
                return null;
            } else {
                if (invalidSize == 1) {
                    return "Parameter " + invalidFields.toString() + " is required.";
                } else {
                    return "Parameters " + invalidFields.toString() + " are required.";
                }
            }
        } catch (ParseException e) {
            logger.error("An error occurred in validation.", e);
            return "An error occurred in validation.";
        }
    }

    private List<String> validateParameterField(JSONObject parameters, JSONObject fieldConfig, JSONObject config, String fieldName) {
        List<String> invalidFields = new ArrayList<>();
        for (Object field : parameters.keySet()) {
            if (parameters.get(field) instanceof JSONObject) {
                JSONObject fieldValue = (JSONObject) parameters.get(field);
                if (String.valueOf(fieldValue.get("type")).equals("JSON")) {
                    String fieldNameDisplay = fieldName != null ? fieldName + "->" + String.valueOf(field) : String.valueOf(field);
                    invalidFields.addAll(validateParameterField((JSONObject) fieldValue.get("value"),
                        (JSONObject) fieldConfig.get(field), config, fieldNameDisplay));
                }
                if (Boolean.parseBoolean(String.valueOf(fieldValue.get("required")))
                    && getConditionalRequired((JSONObject) fieldValue.get("dependsOn"), config)){
                    boolean requiredError = false;
                    if (fieldConfig.get(field) == null) {
                        requiredError = true;
                    } else if (String.valueOf(fieldValue.get("type")).equals("ARRAY")
                        && ((JSONArray) fieldConfig.get(field)).isEmpty()){
                        requiredError = true;
                    } else if (StringUtils.isBlank(String.valueOf(fieldConfig.get(field)))) {
                        requiredError = true;
                    }
                    if (requiredError) {
                        invalidFields.add(fieldName != null ? fieldName + "->" + String.valueOf(field) : String.valueOf(field));
                    }
                }
            }
        }
        return invalidFields;
    }

    private boolean getConditionalRequired(JSONObject dependant, JSONObject config) {
        if (dependant != null) {
            Object fieldValue = getFieldValue(String.valueOf(dependant.get("field")), config);
            fieldValue = fieldValue != null ? fieldValue : "";
            switch (String.valueOf(dependant.get("operator"))) {
                case "=":
                    return fieldValue.equals(dependant.get("value"));
                case "!=":
                    return !fieldValue.equals(dependant.get("value"));
                case "isEmpty":
                    return fieldValue == null;
                case "isNotEmpty":
                    return fieldValue != null;
                case "<>":
                    return fieldValue instanceof Integer
                        && Integer.parseInt(String.valueOf(fieldValue)) >= Integer.parseInt
                        (String.valueOf(dependant.get("value")))
                        && Integer.parseInt(String.valueOf(fieldValue)) < Integer.parseInt(String
                        .valueOf(dependant.get("value")));
                case "><":
                    return fieldValue instanceof Integer && (
                        Integer.parseInt(String.valueOf(fieldValue)) < Integer.parseInt(String
                            .valueOf(dependant.get("value")))
                            || Integer.parseInt(String.valueOf(fieldValue)) >= Integer.parseInt
                            (String.valueOf(dependant.get("value"))));
                case ">":
                    return fieldValue instanceof Integer
                        && Integer.parseInt(String.valueOf(fieldValue)) > Integer.parseInt(String
                        .valueOf(dependant.get("value")));
                case "<":
                    return fieldValue instanceof Integer
                        && Integer.parseInt(String.valueOf(fieldValue)) < Integer.parseInt(String
                        .valueOf(dependant.get("value")));
                case ">=":
                    return fieldValue instanceof Integer
                        && Integer.parseInt(String.valueOf(fieldValue)) >= Integer.parseInt
                        (String.valueOf(dependant.get("value")));
                case "<=":
                    return fieldValue instanceof Integer
                        && Integer.parseInt(String.valueOf(fieldValue)) <= Integer.parseInt
                        (String.valueOf(dependant.get("value")));
                case "~":
                    if (fieldValue instanceof JSONArray) {
                        return ((JSONArray) fieldValue).contains(dependant.get("value"));
                    }
                    return fieldValue instanceof String && String.valueOf(fieldValue)
                        .contains(String.valueOf(dependant.get("value")));
                default:
                    return true;
            }
        }
        return true;
//        Map<String, String> dependant0 = new HashMap<>();
//        dependant0.put("lastDetectTimeWindow", "lastDetectTimeActive");
//        if (dependant0.containsKey(field) && Boolean.parseBoolean(String.valueOf(config.get(dependant0.get(field))))) {
//            return true;
//        }
//        return false;
    }

    private Object getFieldValue(String fieldSeq, JSONObject config) {
        for (String field : fieldSeq.split("\\.")) {
            Object value = config.get(field);
            if (value instanceof JSONObject) {
                config = (JSONObject) config.get(field);
            } else {
                return value;
            }
        }
        return null;
    }

    public JSONObject cleanDummyValues(JSONObject configuration) {
        JSONObject out = new JSONObject();
        for (Object key : configuration.keySet()) {
            if (configuration.get(key) instanceof JSONObject) {
                out.put(key, cleanDummyValues((JSONObject) configuration.get(key)));
            } else {
                if (configuration.get(key) instanceof JSONArray) {
                    JSONArray outArr = new JSONArray();
                    for (Object value : (JSONArray)configuration.get(key)) {
                        if (value instanceof JSONObject) {
                            outArr.add(cleanDummyValues((JSONObject) value));
                        } else {
                            if (StringUtils.equals(String.valueOf(value), dummyValue)) {
                                outArr.add(String.valueOf(value).replace(dummyValue, ""));
                            } else {
                                outArr.add(value);
                            }
                        }
                    }
                    out.put(key, outArr);
                } else {
                    if (StringUtils.equals(String.valueOf(configuration.get(key)), dummyValue)) {
                        out.put(key, String.valueOf(configuration.get(key)).replace(dummyValue, ""));
                    } else {
                        out.put(key, configuration.get(key));
                    }
                }
            }
        }
        return out;
    }

    public Response startStopAllEdgeboxesByTenant(Long group_id, String status, EntityVisibility entityVisibility, Group visibilityGroup){
        BooleanBuilder be = new BooleanBuilder();
        be = be.and( GeneralVisibilityUtils.limitVisibilitySelectAll(entityVisibility, QEdgebox.edgebox, visibilityGroup, "", "") );
        HibernateQuery query = getEdgeboxDAO().getQuery();
        List<Edgebox> edgeboxes = query.where(be).list(QEdgebox.edgebox);
        Iterator<Edgebox> edgeboxIterator = edgeboxes.iterator();
        try {
            while (edgeboxIterator.hasNext()) {
                Edgebox edgebox = edgeboxIterator.next();
                edgebox.setStatus(status);
                EdgeboxService.getInstance().update(edgebox);
                BrokerClientHelper.initLazyLoading();
                BrokerClientHelper.sendRefreshEdgebox(edgebox.getCode(), edgebox.getConfiguration(), false,
                        GroupService.getInstance().getMqttGroups(edgebox.getGroup()));
                statusServiceAction(edgebox.getConfiguration(), edgebox.getCode(), edgebox.getType(), status, edgebox.getGroup().getId());
            }
        } catch (Exception e){
            logger.error("Occurred and error", e);
            return RestUtils.sendBadResponse("Error");
        }
        return RestUtils.sendOkResponse("OK");
    }

    @Override public void validateInsert(Edgebox edgebox) {
        super.validateInsert(edgebox);
        if (edgebox.getId() == null && getByCode(edgebox.getCode()) != null){
            throw new UserException("Edgebox already exists.");
        }

        // validating unique bridges
        for (String type : restrictedTypes) {
            if (StringUtils.equals(edgebox.getType(), type) && getByType(type).size() > 0) {
                throw new UserException(type + " Bridge already created, please use existent one");
            }
        }
    }

    @Override public void validateUpdate(Edgebox edgebox) {
        super.validateUpdate(edgebox);
        if (edgebox.getId() == null && getByCode(edgebox.getCode()) != null){
            throw new UserException("Edgebox already exists.");
        }
    }

    @Override public void validateDelete(Edgebox edgebox) {
        super.validateDelete(edgebox);
        // validating unique bridges
        for (String type : restrictedTypes) {
            if (StringUtils.equals(edgebox.getType(), type) && getByType(type).size() > 0) {
                throw new UserException(type + " Bridge cannot be deleted");
            }
        }
    }

    public List<Edgebox> getByTypesAndGroup(String[] types, Group group) {
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QEdgebox.edgebox.type.in(types));
        be = be.and(QEdgebox.edgebox.group.eq(group));
        return getEdgeboxDAO().selectAllBy(be);
    }

    @Override
    public Edgebox update( Edgebox edgebox )
    {
        validateUpdate( edgebox );
        getEdgeboxDAO().update( edgebox );
        updateFavorite( edgebox );
        this.updateMqttConnectionPool(edgebox);
        return edgebox;
    }

    @Override
    public void delete( Edgebox edgebox )
    {
        validateDelete( edgebox );
        this.deleteMqttConnectionPool(edgebox);
        getEdgeboxDAO().delete( edgebox );
        deleteFavorite( edgebox );
    }

    public void addMqttConnectionPool(Edgebox edgeBox) {
        BrokerEdgeBox brokerEdgeBox = BrokerClientHelper.getBrokerEdgebox(edgeBox);
        if((brokerEdgeBox != null) && brokerEdgeBox.getMqttConnection() != null) {
            MQTTEdgeConnectionPool.getInstance().addEdgeConnection(brokerEdgeBox);
        }
    }

    public void updateMqttConnectionPool(Edgebox edgeBox) {
        BrokerEdgeBox brokerEdgeBox = BrokerClientHelper.getBrokerEdgebox(edgeBox);
        if( (brokerEdgeBox != null) && brokerEdgeBox.getMqttConnection() != null) {
            MQTTEdgeConnectionPool.getInstance().updateEdgeConnection(brokerEdgeBox);
        }
    }
    public void deleteMqttConnectionPool(Edgebox edgeBox) {
        BrokerEdgeBox brokerEdgeBox = BrokerClientHelper.getBrokerEdgebox(edgeBox);
        if((brokerEdgeBox != null) && brokerEdgeBox.getMqttConnection() != null) {
            MQTTEdgeConnectionPool.getInstance().deleteEdgeConnection(brokerEdgeBox.getCode());
        }
    }

    public void updateRelatedCoreBridgeConfiguration(String edgeBoxType, String edgeBoxCode,
                                                     String coreBridgeCode, String previousCoreBridgeCode) {
        if (edgeBoxType.equals(BridgeType.CORE.getType())){
            return;
        }
        // Process ony for EdgeBridges
        logger.info("Processing configuration for coreBridge selected.");
        if (!coreBridgeCode.equals(previousCoreBridgeCode)) {
            logger.info("coreBridgeCode: " + coreBridgeCode);
            Edgebox coreBridge = EdgeboxService.getInstance().selectByCode(coreBridgeCode);
            if (coreBridge != null) {
                String coreBridgeConfig = coreBridge.getConfiguration();
                String newConfiguration = addEdgeBridgeTopicToConfig(edgeBoxCode, coreBridgeConfig);
                coreBridge.setConfiguration(newConfiguration);
                try {
                    refreshConfiguration(coreBridge.getCode(),
                            coreBridge.getType(), coreBridge.getStatus(),
                            coreBridge.getGroup(), coreBridge.publicMap(), false);
                } catch (IOException e) {
                    logger.error("Cannot send update configuration tickle for bridge: " + coreBridge.getCode(), e);
                }
            }
            if (previousCoreBridgeCode!= null && !previousCoreBridgeCode.isEmpty()) {
                removeEdgeFromPreviousCoreBridge(previousCoreBridgeCode, edgeBoxCode);
            }
        }
    }

    private void removeEdgeFromPreviousCoreBridge(String previousCoreBridgeCode, String edgeBoxCode) {
        Edgebox coreBridge = EdgeboxService.getInstance().selectByCode(previousCoreBridgeCode);
        String coreBridgeConfig = coreBridge.getConfiguration();
        JsonNode bridgeConfig = null;
        try {
            bridgeConfig = objectMapper.readValue(coreBridgeConfig, JsonNode.class);
            JsonNode mqtt = bridgeConfig.get("mqtt");
            ArrayNode topics = (ArrayNode) mqtt.get("topics");
            int index = 0;
            for (JsonNode topic : topics) {
                String value = topic.asText();
                if (value.contains(edgeBoxCode)){
                    topics.remove(index);
                    break;
                }
                index++;
            }
            ((ObjectNode)mqtt).put("topics", topics);
            logger.info("new bridgeConfig: " + bridgeConfig);
            coreBridge.setConfiguration(bridgeConfig.toString());
            refreshConfiguration(coreBridge.getCode(),
                    coreBridge.getType(), coreBridge.getStatus(),
                    coreBridge.getGroup(), coreBridge.publicMap(), false);
        } catch (Exception e) {
            logger.error("Cannot remove topic from previous coreBridge with code: " + previousCoreBridgeCode, e);
        }
    }

    public String addEdgeBridgeTopicToConfig(String edgeBoxCode, String coreBridgeConfig) {
        String TOPICS0 = "topics";
        String newConfiguration = "";
        JsonNode bridgeConfig = null;
        String newTopic = "/v1/data/"  + edgeBoxCode + "/#";
        try {
            bridgeConfig = objectMapper.readValue(coreBridgeConfig, JsonNode.class);
            JsonNode mqtt = bridgeConfig.get("mqtt");
            ArrayNode topics = (ArrayNode) mqtt.get(TOPICS0);
            topics.add(newTopic);
            ((ObjectNode)mqtt).put(TOPICS0, topics);
            logger.info("new bridgeConfig: " + bridgeConfig);
            newConfiguration = bridgeConfig.toString();
        } catch (Exception e) {
            logger.error("Cannot add the new topic to coreBridge config with code: " + edgeBoxCode, e);
        }
        return newConfiguration;
    }

    public String getCoreBridgeCode(String configuration, String edgeBoxType) {
        String coreBridgeCode = "";
        if(edgeBoxType.equals(BridgeType.CORE.getType())){
            return coreBridgeCode;
        }
        JsonNode bridgeConfig = null;
        try {
            bridgeConfig = objectMapper.readValue(configuration, JsonNode.class);
            JsonNode coreBridge = bridgeConfig.get("coreBridge");
            logger.info("new bridgeConfig: " + bridgeConfig);
            if (coreBridge != null){
                coreBridgeCode = coreBridge.asText();
            }
        } catch (Exception e) {
            logger.error("Cannot get coreBridge code from configuration: " + configuration, e);
        }
        return coreBridgeCode;
    }

    /**
     * Search in all coreBridges if the edge is been referenced in the mqtt topics
     */
    public void getCoreBridgeReference(Map<String, Object> publicMap, String edgeBoxType, String edgeBoxCode, Group edgeBoxGroup) {
        String CONFIGURATION = "configuration";
        if (edgeBoxType.equals(BridgeType.CORE.getType())){
            return;
        }
        String bridgeConfig = String.valueOf(publicMap.get(CONFIGURATION));
        logger.info("configuration: " + bridgeConfig);

        String relatedCoreBridgeCode = getRelatedCoreBridgeCode(edgeBoxCode, edgeBoxGroup);
        try {
            JsonNode config = objectMapper.readValue(bridgeConfig, JsonNode.class);
            ((ObjectNode)config).put("coreBridge", relatedCoreBridgeCode);
            publicMap.put(CONFIGURATION, config.toString());
            logger.info("publicMap: " + publicMap);
        } catch (Exception e) {
            logger.error("Cannot get coreBridge references for edgeBridge: " + edgeBoxCode, e);
        }
    }

    public String getRelatedCoreBridgeCode(String edgeBoxCode, Group edgeBoxGroup) {
        String coreBridgeCode = "";
        String [] types = {BridgeType.CORE.getType()};
        // searching by edgebox group
        List<Edgebox> bridgesList = getByTypesAndGroup(types, edgeBoxGroup);
        // searching by edgebox tenant
        bridgesList.addAll(getByTypesAndGroup(types, edgeBoxGroup.getParentLevel2()));

        for (Edgebox edgebox : bridgesList) {
            String configuration  = edgebox.getConfiguration();
            try {
                JsonNode configRootJsonNode = objectMapper.readValue(configuration, JsonNode.class);
                JsonNode mqtt = configRootJsonNode.get("mqtt");
                ArrayNode topics = (ArrayNode) mqtt.get("topics");
                for (JsonNode topic : topics) {
                    String value = topic.asText();
                    String inputTopic = "/v1/data/" + edgeBoxCode + "/";
                    if (value.contains(inputTopic)){
                        coreBridgeCode = edgebox.getCode();
                        break;
                    }
                }
            } catch (Exception e) {
                logger.error("Cannot get topics from bridge: " + edgebox.getCode(), e);
            }
        }
        return coreBridgeCode;
    }

    public void removeCoreBridgeCodeFakeParam(Edgebox edgebox) {
        try {
            JsonNode rootConfigNode = objectMapper.readValue(edgebox.getConfiguration(), JsonNode.class);
            ((ObjectNode)rootConfigNode).remove("coreBridge");
            edgebox.setConfiguration(rootConfigNode.toString());
        } catch (IOException e) {
            logger.error("Cannot remove 'coreBridge' param", e);
        }
    }

    public List<String> getBridgesConfigurationList(String agentCode) {
        BooleanBuilder be = new BooleanBuilder();
        be = be.or(QEdgebox.edgebox.status.eq("ERROR"));
        be = be.or(QEdgebox.edgebox.status.eq("ON"));

        List<String> oficialList = new ArrayList<>();

        List<Edgebox> listBridges = EdgeboxService.getInstance().listPaginated(be, null, null);
        BridgeAgentService agentService = new BridgeAgentService();
        for (Edgebox edgebox: listBridges){
            Map  detailMap = EdgeboxService.getInstance().getConfiguration(edgebox.getCode());
            Map agent = (Map) detailMap.get("bridgeAgent");
            if (agent != null && agent.get("agentCode").toString().equals(agentCode)) {
               String expandedProperties = agentService.getPayload(edgebox.getConfiguration(), edgebox.getCode(), edgebox.getType(), edgebox.getStatus(),edgebox.getGroup().getId());
                oficialList.add(expandedProperties);
            }
        }

        return oficialList;
    }

}
