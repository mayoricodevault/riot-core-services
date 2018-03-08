package com.tierconnect.riot.iot.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.mysema.query.types.expr.BooleanExpression;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.Hibernate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tierconnect.riot.appcore.entities.Connection;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.GroupType;
import com.tierconnect.riot.appcore.services.ConnectionService;
import com.tierconnect.riot.appcore.utils.Configuration;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.commons.dtos.ConnectionDto;
import com.tierconnect.riot.commons.dtos.EdgeboxConfigurationDto;
import com.tierconnect.riot.commons.dtos.EdgeboxDto;
import com.tierconnect.riot.commons.dtos.EdgeboxRuleDto;
import com.tierconnect.riot.commons.dtos.GroupDto;
import com.tierconnect.riot.commons.dtos.GroupTypeDto;
import com.tierconnect.riot.commons.dtos.LogicalReaderDto;
import com.tierconnect.riot.commons.dtos.ShiftDto;
import com.tierconnect.riot.commons.dtos.ShiftZoneDto;
import com.tierconnect.riot.commons.dtos.ThingTypeDto;
import com.tierconnect.riot.commons.dtos.ZoneDto;
import com.tierconnect.riot.commons.dtos.ZoneTypeDto;
import com.tierconnect.riot.commons.services.broker.*;
import com.tierconnect.riot.commons.utils.KafkaZkUtils;
import com.tierconnect.riot.commons.utils.SerialNumberMapper;
import com.tierconnect.riot.commons.utils.TenantUtil;
import com.tierconnect.riot.commons.utils.Topics;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.utils.Translator;

/**
 * this class is used to publish health and status and cache update tickle
 * messages to the mqtt bus.
 *
 * @author tcrown
 */
public class BrokerClientHelper {
    private static final String MQTT_TYPE = "mqtt";
    private static final String KAFKA_TYPE = "kafka";

    private static final int HEALTH_AND_STATUS_INTERVAL = 10000;
    private static Logger logger = Logger.getLogger(BrokerClientHelper.class);
    private static List<String> bridgesTypes = Arrays.asList("core", "Rules_Processor");
    private static BrokerPublisherService publisher;
    private static final List<String> BROKER_TYPES = new ArrayList<String>();
    private static Map<String, ConcurrentLinkedQueue<Message>> messages = new ConcurrentHashMap<>();

    private static LoadTrigger lt;

    private static SerialNumberMapper serialNumberMapper;
    private static final String[] VALID_TOPICS = {"___v1___data", "___v1___cache", "___v1___edge___dn___thingCache"};
    static boolean kafkaEnabled;
    static {

        // initiating broker list
        String kafkaEnabledValue = Configuration.getProperty("kafka.enabled");
        kafkaEnabled = kafkaEnabledValue != null ? Boolean.parseBoolean(kafkaEnabledValue) : false;
		logger.info( "kafkaEnabled=" + kafkaEnabled );

        BROKER_TYPES.add(MQTT_TYPE);
        if (kafkaEnabled) {
            BROKER_TYPES.add(KAFKA_TYPE);
        }
        logger.info("Broker connection types to be used: " + BROKER_TYPES);

        // initiating publishing thread
        lt = new LoadTrigger() {
            public void callback(boolean publishMessage, String threadName) {
                if(kafkaEnabled) {
                    try {
                        String thingTypeCode = riotMessageBuilder.getThingTypeCode();
                        ThingType thingType = ThingTypeService.getInstance().getByCode(thingTypeCode);
                        Group tenant = thingType.getGroup();
                        String tenantCode = TenantUtil.getTenantCode(tenant.getHierarchyName());
                        final String key = String.format("%s-%s-%s", tenantCode, thingTypeCode, riotMessageBuilder.getSerialNumber());
                        publish(topic, key, riotMessageBuilder.build("JSON"), groupMqtt, "loadTrigger", publishMessage, threadName);
                    } catch (Exception exception) {
                        logger.error(exception.getMessage(), exception);
                    }
                } else {
                    // TODO: Remove in future
                    publish(topic, body.toString(), "loadTrigger", groupMqtt, publishMessage, threadName);
                }
            }
        };
    }

    /**
     * Delete a thing of cache sending a message null.
     * @param key
     */
    public static void deleteCacheThing(String key){
        publishByKey(Topics.CACHE_THING.getKafkaName(),key,null);
    }

    public static void initLazyLoading() {
        initializeSerialNumberMapper();
    }

    /**
     * Initialize serial number mapper.
     *
     */
    private static void initializeSerialNumberMapper() {
        serialNumberMapper = new SerialNumberMapper();

        // TODO: Gets the list of topics to initialize the serial number Mapper.
        List<String> topics = new LinkedList<>();
        topics.add("/v1/data1");

        List<BrokerConnection> connections = KafkaConnectionPool.getConnections();
        for (BrokerConnection connection : connections) {
            Map<String, Object> properties = connection.getProperties();
            String zookeeper = properties.get("zookeeper").toString();

            KafkaZkUtils kafkaZkUtils = new KafkaZkUtils(zookeeper);
            for (String topic:
                 topics) {
                String kafkaTopic = StringUtils.replace(topic, "/", "___");
                int numberOfPartitions = kafkaZkUtils.getNumberOfPartitions(kafkaTopic);
                try {
                    serialNumberMapper.addTopic(kafkaTopic, numberOfPartitions);
                } catch (Exception exception) {
                    logger.error("Mistakes adding the topic in serial number mapper..");
                }

            }
        }
    }

    public void init(String clientId, boolean waitConnections) {
        init(clientId);
    }

    public void init(String clientId) {
        List<BrokerEdgeBox> lstBrokerEdgebox  = loadBridgeConnections();
        if ( (lstBrokerEdgebox != null) && !lstBrokerEdgebox.isEmpty()) {
            //MQTT Connections
            MQTTEdgeConnectionPool.getInstance().init(lstBrokerEdgebox);
            //Kafka Connections
            if(kafkaEnabled) {
                KafkaConnectionPool.init(lstBrokerEdgebox, clientId);
            }
            initLazyLoading();
            publisher = new BrokerPublisherService(clientId);
            publisher.setSerialNumberMapper(serialNumberMapper);
            BrokerClientHelper.start();
        } else {
            logger.warn("No initialize BrokerClientHelper. Verify the following causes:\n " +
                    "1. There is no a bridge configuration (type:core or Rules_processor) registered.\n" +
                    "2. Some  bridge configuration (type:core or Rules_processor) does not have a broker connection(mqtt or kafka) assigned.");

        }
    }

    // starts a thread to publish health and status messages every 10 seconds
    static public void start() {
        Thread t = new Thread() {
            public void run() {
                boolean run = true;

                while (run) {
                    long timestamp = System.currentTimeMillis();

                    try {
                        final String mqttClientName = Configuration.getProperty("mqtt.client.name");
                        publish("/v1/status/APP", HealthAndStatus.getInstance().getMessage(timestamp, mqttClientName),
                                "healthAndStatus", null, true, null);
                    } catch (org.hibernate.exception.JDBCConnectionException e) {
                        logger.error("Connection refused, there is a problem with connectivity to MySQL.");
                    } catch (Exception e) {
                        logger.warn("caught e:", e);
                    }

                    if (!messages.isEmpty()) {
                        Iterator<Map.Entry<String, ConcurrentLinkedQueue<Message>>> messageIterator = messages.entrySet().iterator();
                        while (messageIterator.hasNext()) {
                            Map.Entry<String, ConcurrentLinkedQueue<Message>> messageList = messageIterator.next();
                            if (!messageList.getValue().isEmpty()) {
                                logger.warn("There are pending tickles: thread=[" + messageList.getKey() + "], messages=" + messageList.getValue().toString());
                            }else{
                                messageIterator.remove();
                            }
                        }
                    }

                    try {
                        Thread.sleep(HEALTH_AND_STATUS_INTERVAL);
                    } catch (InterruptedException e) {
                        run = false;
                    }
                }
                logger.info("shutting down Health and Status thread");
            }
        };

        t.start();
    }

    static public void sendRefreshEdgebox(String bridgeCode, String config, boolean publishMessage, List<Long> groupMqtt) {
        publish("/v1/edge/dn/" + bridgeCode + "/update/config", config, "sendRefreshEdgebox", groupMqtt,
                publishMessage, Thread.currentThread().getName());
    }

    static public void sendRefreshEdgeboxRule(String bridgeCode, String rule, boolean publishMessage, List<Long> groupMqtt) {
        publish("/v1/edge/dn/" + bridgeCode + "/update/cep/rules", rule, "sendRefreshEdgeboxRule",
                groupMqtt, publishMessage, Thread.currentThread().getName());
    }

    /**
     * Send messages to reload all things in coreBridge cache
     */
    static public void sendRefreshThingMessage(boolean publishMessage, List<Long> groupMqtt) {
        publish("/v1/edge/dn/_ALL_/update/things", null,  "sendRefreshThingMessage", groupMqtt,
                publishMessage, Thread.currentThread().getName());
    }

    //TODO dont send
    static public void sendRefreshSingleThingMessage(String thingTypeCode, String serial, boolean publishMessage,
                                                     List<Long> groupMqtt) {
        publish("/v1/edge/dn/" + thingTypeCode + "/" + serial + "/update/singleThing", null, "sendRefreshSingleThingMessage", groupMqtt,
                publishMessage, Thread.currentThread().getName());

        String body = "{\"command\":\"evict\",\"things\":[{\"thingTypeCode\" : \""+thingTypeCode+"\",\"serialNumber\":\""+serial+"\"}]}";
        publish("/v1/edge/dn/thingCache", body, "sendRefreshSingleThingMessage", groupMqtt, publishMessage,
                Thread.currentThread().getName());
    }

    static public void sendDeleteThingMessage(String thingTypeCode, String serial, boolean publishMessage, List<Long> groupMqtt) {
        publish("/v1/edge/dn/" + thingTypeCode + "/" + serial + "/delete/thing", null, "sendDeleteThingMessage",
                groupMqtt, publishMessage, Thread.currentThread().getName());

        String body = "{\"command\":\"evict\",\"things\":[{\"thingTypeCode\" : \""+thingTypeCode+"\",\"serialNumber\":\""+serial+"\"}]}";
        publish("/v1/edge/dn/thingCache", body, "sendRefreshSingleThingMessage", groupMqtt, publishMessage,
                Thread.currentThread().getName());
    }

    static public void sendRefreshThingTypeMessage(boolean publishMessage, List<Long> groupMqtt) {
        publish("/v1/edge/dn/_ALL_/update/thingTypes", null, "sendRefreshThingTypeMessage", groupMqtt, publishMessage, Thread.currentThread().getName());
    }

    static public void sendRefreshZonesMessage(boolean publishMessage, List<Long> groupMqtt) {
        publish("/v1/edge/dn/_ALL_/update/zones", null, "sendRefreshZonesMessage", groupMqtt, publishMessage,
                Thread.currentThread().getName());
    }

    static public void sendRefreshShiftsMessage(boolean publishMessage, List<Long> groupMqtt) {
        publish("/v1/edge/dn/_ALL_/update/shifts", null, "sendRefreshShiftsMessage", groupMqtt, publishMessage, Thread.currentThread().getName());
    }

    static public void sendRefreshLogicalReadersMessage(boolean publishMessage, List<Long> groupMqtt) {
        publish("/v1/edge/dn/_ALL_/update/logicalReaders", null, "sendRefreshLogicalReadersMessage", groupMqtt, publishMessage,
                Thread.currentThread().getName());
    }

    /**
     * Send Messages for STARflex
     */
    static public void sendInitDeviceBridge(String bridgeCode, String groupCode, boolean publishMessage, List<Long> groupMqtt) {
        String topic = "/v1/edge/dn/device/" + bridgeCode + "/" + groupCode + "/initBridge";
        publish(topic, null, "sendInitSTARflexBridge", groupMqtt, publishMessage, Thread.currentThread().getName());
        logger.info("Topic message to initialize Bridges has been sent."+topic );
    }

    static public void sendInitDevice(String thingTypeCode, String groupCode, String serialNumber, String bridgeCode, boolean publishMessage,
                                      List<Long> groupMqtt) {
        String topic = "/v1/edge/dn/device/" + thingTypeCode + "/" + serialNumber + "/" + groupCode + "/" + bridgeCode + "/initDevice";
        publish(topic, null, "sendInitSTARflexDevice", groupMqtt, publishMessage, Thread.currentThread().getName());
        logger.info("Topic message to initialize Device has been sent."+topic );
    }

    static public void sendReinitializeSDeviceBridge(String bridgeCode, String groupCode, boolean publishMessage,
                                                     List<Long> groupMqtt) {
        String topic = "/v1/edge/dn/devices/" + bridgeCode + "/" + groupCode + "/reinitializeBridge";
        publish(topic , null, "sendReinitializeSDeviceBridge", groupMqtt, publishMessage, Thread.currentThread().getName());
        logger.info("Topic message to initialize Device has been sent."+topic );
    }

    /**
     * This method is called from GroupConfigurationController using reflexion
     */
    public static void sendRefreshGroupConfiguration(Boolean publishMessage, String threadName, List<Long> groupMqtt) {
        // This call refreshes just root group configuration
        publish("/v1/edge/dn/_ALL_/update/groupConfiguration", null, "sendRefreshGroupConfiguration", groupMqtt,
                publishMessage, threadName);
    }

    /**
     * Send mqtt message to reload facilityMaps in CoreBridge
     */
    public static void sendRefreshFacilityMapsMessage(boolean publishMessage, List<Long> groupMqtt) {
        publish("/v1/edge/dn/_ALL_/update/facilityMaps", null, "sendRefreshFacilityMapsMessage", groupMqtt, publishMessage,
                Thread.currentThread().getName());
    }

    /**
     * Send mqtt message to reload zoneGroups in CoreBridge
     */
    public static void sendRefreshZoneGroupsMessage(boolean publishMessage, List<Long> groupMqtt) {
        publish("/v1/edge/dn/_ALL_/update/zoneGroups", null, "sendRefreshZoneGroupsMessage", groupMqtt, publishMessage,
                Thread.currentThread().getName());
    }

    /**
     * Send mqtt message to reload zoneGroups in CoreBridge
     */
    public static void sendRefreshZoneTypesMessage(boolean publishMessage, List<Long> groupMqtt) {
        publish("/v1/edge/dn/_ALL_/update/zoneTypes", null, "sendRefreshZoneTypesMessage", groupMqtt, publishMessage,
                Thread.currentThread().getName());
    }

    /**
     * This method is called from ConnectionController using reflexion
     */
    public static void sendRefreshConnectionConfigs(Boolean publishMessage, String threadName, List<Long> groupMqtt) {
        publish("/v1/edge/dn/_ALL_/update/connections", null, "sendRefreshConnectionConfigs", groupMqtt, publishMessage,
                threadName);
    }

    /**
     * Send mqtt message to reload groups in CoreBridge, it is called using reflexion
     */
    public static void sendRefreshGroupsMessage(Boolean publishMessage, String threadName, List<Long> groupMqtt) {
        publish("/v1/edge/dn/_ALL_/update/groups", null, "sendRefreshGroupsMessage", groupMqtt, publishMessage, threadName);
    }

    /**
     * Send mqtt message to reload groupTypes in CoreBridge, it is called using reflexion
     */
    public static void sendRefreshGroupTypesMessage(Boolean publishMessage, String threadName, List<Long> groupMqtt) {
        publish("/v1/edge/dn/_ALL_/update/groupTypes", null, "sendRefreshGroupTypesMessage", groupMqtt, publishMessage, threadName);
    }

    public static void initThingFieldTickle(String thingTypeCode, List<Long> groupMqtt) {
        lt.initThingFieldTickle("APP2", thingTypeCode, groupMqtt);
    }

    public static void initThingFieldTickleKafka(String thingTypeCode, Boolean runRules) {
        lt.initThingFieldTickleKafka("APP2", thingTypeCode, runRules);
    }

    public static void setThingField(String serial, long time, String name, String value) {
        lt.setThingField(serial, time, name, value);
    }

    public static void sendThingFieldTickle() {
        lt.sendThingFieldTickle();
    }

    public static void sendThingFieldTickle(boolean publishTickleFlag) {
        lt.sendThingFieldTickle(publishTickleFlag);
    }

    public static void sendUpdateThingField(String bridgeCode, String thingTypeCode, String serial, List<Long> groupMqtt,
                                            long time, String name, String value) {
        lt.tickle(bridgeCode, thingTypeCode, serial, groupMqtt, time, name, value);
    }

    static public void sendLoggers(String bridgeCode, String body, boolean publishMessage, List<Long> groupMqtt) {
        publish("/v1/edge/dn/" + bridgeCode + "/log4j/logger", body, "sendLoggers", groupMqtt, publishMessage, Thread.currentThread().getName());
    }

    static public void publish(String topic, String body, String tickleType,  List<Long> groupMqtt, boolean publishMessage, String threadName) {
//        TODO lazyLoading validation remove in order to always send tickle
//        if (!(CHECK_LAZY_LOADING.get(tickleType) && isLazyLoading)) {
            publishToConnections(topic, body, groupMqtt, publishMessage, threadName);
//        }
    }

    static public void publish(String topic, String key, String body, List<Long> groupMqtt, String tickleType, boolean publishMessage,
            String threadName) {
        publishToConnections(topic, key, body, groupMqtt, publishMessage, threadName);
    }

    public static void publishToConnections(String topic, String body, List<Long> groupMqtt, boolean publishMessage, String threadName) {
        publishToConnections(topic, null, body, groupMqtt, publishMessage, threadName);
    }

    public static void publishToConnections(String topic, String key, String body, List<Long> groupMqtt,
                                            boolean publishMessage, String threadName) {
        if (publishMessage) {
            publisher.publish(topic, key, body, groupMqtt, false, null);
        } else {
            if (!messages.containsKey(Thread.currentThread().getName())) {
                messages.put(Thread.currentThread().getName(), new ConcurrentLinkedQueue<Message>());
            }

            threadName = threadName != null ? threadName : Thread.currentThread().getName();
            Message message = new Message(topic, key, body, groupMqtt);

            // VIZIX-4219: only message supported by kafka core bridge should be saved in the message queue.
//            String kafkaEnabledValue = Configuration.getProperty("kafka.enabled");
//            boolean kafkaEnabled = kafkaEnabledValue != null ? Boolean.parseBoolean(kafkaEnabledValue) : false;
//            if (kafkaEnabled) {
//                if (StringUtils.startsWithAny(topic, VALID_TOPICS)) {
//                    if (!messages.get(threadName).contains(message)) {
//                        messages.get(threadName).add(message);
//                    }
//                }
//            } else {
                if (!messages.get(threadName).contains(message)) {
                    messages.get(threadName).add(message);
                }
//            }
        }
    }

    /**
     * Sends tickles registered in local messages map NOTE: Be careful, this
     * method is being called from TransactionJAXRXFilter.java:75 via reflection
     *
     * @param threadName
     *            Name og the thread tickles are registered of
     * @return
     */
    //
    public static String publishTickle(String threadName, String bridgeCode) {
        if (messages.containsKey(threadName)) {
            ConcurrentLinkedQueue<Message> messagesList = messages.get(threadName);
            StringBuilder sb = new StringBuilder();

            for (Message message : messagesList) {
                logger.debug("Message to sent=" + message + " for threadName=" + threadName + " bridgeCode=" + bridgeCode);
                List<String> connectionsSent = publisher.publish(
                        message.getTopic(), message.getKey(), message.getBody(), message.getGroupMqtt(), false, bridgeCode);
                sb.append("\nConnections:\n" + (connectionsSent.isEmpty()? "No Connections found to send tickles" : StringUtils.join(connectionsSent, "\n")) + "\nMessage: " + message.toString() + "\n");
            }

            messages.remove(threadName);
            return sb.toString();
        } else {
            return null;
        }
    }

    public static void removeMessage (String threadName){
        messages.remove(threadName);
    }

    @Deprecated
    public static void doNow() {
        lt.goNow();
    }

    /**
     * Get List of "Core" Edge Boxes with their Mqtt Connection
     * @return List of Broker EdgeBoxes
     */
    public static List<BrokerEdgeBox> loadBridgeConnections() {
        List<Edgebox> edgeBoxes = new ArrayList<>();
        BooleanExpression query = QEdgebox.edgebox.type.in(bridgesTypes);
        edgeBoxes.addAll(EdgeboxService.getInstance().listPaginated(query, null, null));

        List<BrokerEdgeBox> edgeBridgesBrokers = new ArrayList<>();
        for (Edgebox edgeBox : edgeBoxes) {
            BrokerEdgeBox brokerEdgeBox = getBrokerEdgebox(edgeBox);
            if(brokerEdgeBox != null) {
                edgeBridgesBrokers.add(brokerEdgeBox);
            }
        }
        return edgeBridgesBrokers;
    }

    /**
     * Get the list of broker Edgeboxes based on BROKER_TYPES and
     * @param edgeBox Object Edge Box
     * @return
     */
    public static BrokerEdgeBox getBrokerEdgebox(Edgebox edgeBox){
        BrokerEdgeBox brokerEdgeBox = new BrokerEdgeBox();
        try {
            brokerEdgeBox.setId(edgeBox.getId());
            brokerEdgeBox.setGroupId(edgeBox.getGroup().getId());
            brokerEdgeBox.setName(edgeBox.getName());
            brokerEdgeBox.setCode(edgeBox.getCode());
            brokerEdgeBox.setType(edgeBox.getType());
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(edgeBox.getConfiguration());
            for (String broker : BROKER_TYPES) {
                JsonNode brokerNode = root.get(broker);
                if (brokerNode != null) {
                    String connectionCode = brokerNode.get("connectionCode").asText();
                    Connection connection = ConnectionService.getInstance().getByCode(connectionCode);
                    if (connection == null) {
                        logger.warn("Connection does not exist in database: " + connectionCode);
                    } else {
                        // active property
                        boolean active = connection.getConnectionType().getCode().equalsIgnoreCase(Constants.MQTT_CONNECTION)
                                || connection.getConnectionType().getCode().equalsIgnoreCase(Constants.KAFKA_CONNECTION)
                                ? true:false;
                        if (brokerNode.has("active")) {
                            active = brokerNode.get("active").asBoolean();
                        }
                        BrokerConnection brokerConnIns = new BrokerConnection(
                                connection.getCode(),
                                connection.getConnectionType().getCode(),
                                active,
                                connection.getPropertiesMap(),
                                connection.getPassword(false),
                                connection.getId()
                        );
                        if(brokerConnIns.getConnectionType().equals(Constants.MQTT_CONNECTION)) {
                            brokerEdgeBox.setMqttConnection(brokerConnIns);
                        } else if (brokerConnIns.getConnectionType().equals(Constants.KAFKA_CONNECTION)) {
                            brokerEdgeBox.setKafkaConnection(brokerConnIns);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Could not load connection for Edge Bridge:"+ edgeBox.getName()
                    +"("+edgeBox.getCode()+"), error message=" + e.getMessage());
        }
        if((brokerEdgeBox.getMqttConnection() != null) || (brokerEdgeBox.getKafkaConnection() != null)) {
            return brokerEdgeBox;
        } else {
            return null;
        }
    }

    public static void refreshConnectionCache( Connection connection, boolean delete )
	{
		ConnectionDto convertConnectionDTO = Translator.convertConnectionDTO( connection );
		String id = KeyGenEntities.getId( connection );
		String code = KeyGenEntities.getCode( connection );
		publishKafka( Topics.CACHE_CONNECTION.getKafkaName(), id, code, delete ? null : convertConnectionDTO );
	}

	public static void refreshEdgeboxCache( Edgebox edgebox, boolean delete )
	{
		EdgeboxDto edgeboxDto = Translator.convertEdgeboxDTO( edgebox );
		String id = KeyGenEntities.getId( edgebox );
		String code = KeyGenEntities.getCode( edgebox );
		publishKafka( Topics.CACHE_EDGEBOX.getKafkaName(), id, code, delete ? null : edgeboxDto );
	}

	public static void refreshEdgeboxConfigurationCache( Edgebox edgebox, Map<String, Object> configuration, boolean delete ) throws JsonProcessingException
	{
		String transformedConfiguration = new ObjectMapper().writeValueAsString( configuration );
		EdgeboxConfigurationDto edgeboxConfigurationDto = Translator.convertEdgeboxConfgurationDTO( edgebox, transformedConfiguration );
		String id = KeyGenEntities.getId( edgebox );
		String code = KeyGenEntities.getCode( edgebox );
		publishKafka( Topics.CACHE_EDGEBOXES_CONFIGURATION.getKafkaName(), id, code, delete ? null : edgeboxConfigurationDto );
	}

	public static void refreshEdgeboxRuleCache( EdgeboxRule edgeboxRule, boolean delete )
	{
		EdgeboxRuleDto convertEdgeboxRuleDTO = Translator.convertEdgeboxRuleDTO( edgeboxRule );
		String id = KeyGenEntities.getId( edgeboxRule );
		publishByKey( Topics.CACHE_EDGEBOX_RULE.getKafkaName(), id, delete ? null : convertEdgeboxRuleDTO );
	}

	public static void refreshGroupCache( Group group, boolean delete )
	{
		GroupDto groupDto = Translator.convertToGroupDTO( group );
		String id = KeyGenEntities.getId( group );
		String code = KeyGenEntities.getCode( group );
		publishKafka( Topics.CACHE_GROUP.getKafkaName(), id, code, delete ? null : groupDto );
	}

	public static void refreshGroupTypeCache( GroupType groupType, boolean delete )
	{
		GroupTypeDto groupTypeDto = Translator.convertToGroupTypeDTO( groupType );
		String id = KeyGenEntities.getId( groupType );
		publishByKey( Topics.CACHE_GROUP_TYPE.getKafkaName(), id, delete ? null : groupTypeDto );
	}

	public static void refreshLogicalReaderCache( LogicalReader logicalReader, boolean delete )
	{
		LogicalReaderDto logicalReaderDto = Translator.convertLogicalReaderDTO( logicalReader );
		String id = KeyGenEntities.getId( logicalReader );
		String code = KeyGenEntities.getCode( logicalReader );
		publishKafka( Topics.CACHE_LOGICAL_READER.getKafkaName(), id, code, delete ? null : logicalReaderDto );
	}

	public static void refreshShiftCache( Shift shift, boolean delete )
	{
		ShiftDto shiftDto = Translator.convertToShiftDTO( shift );
		String id = KeyGenEntities.getId( shift );
		String code = KeyGenEntities.getCode( shift );
		publishKafka( Topics.CACHE_SHIFT.getKafkaName(), id, code, delete ? null : shiftDto );
	}

	public static void refreshShiftZoneCache( Shift shift, boolean delete )
	{
		ShiftZoneDto shiftZone = null;
		if( shift != null )
		{
			List<ShiftZone> shiftZones = ShiftZoneService.getShiftZoneDAO().selectAllBy( "shift.id", shift.getId() );
			List<Zone> zones = new ArrayList<>();
			for( ShiftZone shiftzone : shiftZones )
			{
				zones.add( shiftzone.getZone() );
			}
			shiftZone = Translator.convertShiftZoneDTO( shift, zones, new HashMap<>() );
		}
		String id = KeyGenEntities.getId( shift );
		String code = KeyGenEntities.getCode( shift );
		publishKafka( Topics.CACHE_SHIFT_ZONE.getKafkaName(), id, code, delete ? null : shiftZone );
	}

	static public void refreshThingTypeCache( ThingType thingType, boolean delete )
	{
		String id = KeyGenEntities.getId( thingType );
		String code = KeyGenEntities.getCode( thingType );
		initializeLazyObjects( thingType );
		ThingTypeDto convertToThingTypeDTO = Translator.convertToThingTypeDTO( thingType );
		publishKafka( Topics.CACHE_THING_TYPE.getKafkaName(), id, code, delete ? null : convertToThingTypeDTO );
	}

	/**
	 * Initialize lazy objects of thing type.
	 */
	static private void initializeLazyObjects( ThingType thingType )
	{
		Hibernate.initialize( thingType.getGroup() );
		Hibernate.initialize( thingType.getParentTypeMaps() );
		Hibernate.initialize( thingType.getChildrenTypeMaps() );
		Hibernate.initialize( thingType.getThingTypeFields() );
		Hibernate.initialize( thingType.getDefaultOwnerGroupType() );
		Set<ThingTypeField> thingTypeFields = thingType.getThingTypeFields();
		if( thingTypeFields != null )
		{
			for( ThingTypeField thingTypeField : thingTypeFields )
			{
				Hibernate.initialize( thingTypeField.getThingType() );
				Hibernate.initialize( thingTypeField.getDataType() );
			}
		}
		Hibernate.initialize( thingType.getThingTypeTemplate() );
	}

	public static void refreshZoneCache( Zone zone, Map<Long, Map<String, Object>> properties, boolean delete )
	{
		ZoneDto convertZoneDTO = Translator.convertZoneDTO( zone, properties );
		String id = KeyGenEntities.getId( zone );
		String code = KeyGenEntities.getCode( zone );
		publishKafka( Topics.CACHE_ZONE.getKafkaName(), id, code, delete ? null : convertZoneDTO );
		//publishKafka( Topics.CACHE_ZONE.getKafkaName(), id, code, delete ? null : convertZoneDTO );
	}

	public static void refreshZoneTypeCache( ZoneType zoneType, boolean delete )
	{
		ZoneTypeDto zoneTypeDto = Translator.convertZoneTypeDTO( zoneType );
		String id = KeyGenEntities.getId( zoneType );
		String code = KeyGenEntities.getCode( zoneType );
		publishKafka( Topics.CACHE_ZONE_TYPE.getKafkaName(), id, code, zoneTypeDto );
	}

    /**
     *
     * Sends one message with one topic to many brokers.
     * @param topic
     *            Topic in format ___v1___cache___zone
     * @param key
     * @param message
     *            Message to publish
     */
    //TODO Must permit any topic, not only cache
    private static void publishByKey(String topic, String key,  Object message) {
        Collection<KafkaPublisher> publishers = KafkaConnectionPool.getPublishers();
        Iterator<KafkaPublisher> it = publishers.iterator();
        while(it.hasNext()) {
        	KafkaPublisher p = it.next();
        	logger.info( p + " topic=" + topic + " key=" + key + " body=" + message );
            p.updateCacheByKey(topic, key, message);
        }
    }

    /**
     *
     * Sends one message with one topic to many brokers.
     * @param topic
     *            Topic in format ___v1___cache___zone
     * @param idKey
     * @param codeKey
     * @param message
     *            Message to publish
     */
    //TODO Must permit any topic, not only cache
    private static void publishKafka(String topic, String idKey, String codeKey, Object message) {
        Collection<KafkaPublisher> publishers = KafkaConnectionPool.getPublishers();
        Iterator<KafkaPublisher> it = publishers.iterator();
        while(it.hasNext()) {
        	KafkaPublisher p = it.next();
        	logger.info( p + " topic=" + topic + " key=" + idKey + " code=" + codeKey + " body=" + message );
            p.updateCache(topic, idKey, codeKey, message);
        }
    }



    public static void cleanMessagesQueue() {
        messages.clear();
    }

    private static class Message {
        private String topic;
        private String body;
        private String key;
        private List<Long> groupMqtt;

        public Message(String topic, String body, List<Long> groupMqtt) {
            this.topic = topic;
            this.body = body;
            this.key = null;
            this.groupMqtt = groupMqtt;
        }

        public Message(String topic, String key, String body, List<Long> groupMqtt) {
            this.topic = topic;
            this.key = key;
            this.body = body;
            this.groupMqtt = groupMqtt;
        }

        public String getTopic() {
            return topic;
        }

        public String getBody() {
            return body;
        }

        public String getKey() {
            return key;
        }

        public List<Long> getGroupMqtt() {
            return groupMqtt;
        }

        @Override
        public String toString() {
            return "topic=" + topic + " body=" + body;
        }

        public boolean equals(Message obj) {
            return topic.equals(obj.getTopic()) && body.equals(obj.getBody());
        }

    }
}
