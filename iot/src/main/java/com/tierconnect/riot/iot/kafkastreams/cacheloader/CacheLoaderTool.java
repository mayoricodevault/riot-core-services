package com.tierconnect.riot.iot.kafkastreams.cacheloader;

import com.google.common.base.Preconditions;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.tierconnect.riot.appcore.entities.Connection;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.GroupType;
import com.tierconnect.riot.appcore.popdb.PopDBRequired;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.commons.dtos.ConnectionDto;
import com.tierconnect.riot.commons.dtos.EdgeboxConfigurationDto;
import com.tierconnect.riot.commons.dtos.EdgeboxDto;
import com.tierconnect.riot.commons.dtos.EdgeboxRuleDto;
import com.tierconnect.riot.commons.dtos.GroupDto;
import com.tierconnect.riot.commons.dtos.GroupTypeDto;
import com.tierconnect.riot.commons.dtos.LogicalReaderDto;
import com.tierconnect.riot.commons.dtos.ShiftDto;
import com.tierconnect.riot.commons.dtos.ShiftZoneDto;
import com.tierconnect.riot.commons.dtos.ThingDto;
import com.tierconnect.riot.commons.dtos.ThingTypeDto;
import com.tierconnect.riot.commons.dtos.ZoneDto;
import com.tierconnect.riot.commons.dtos.ZoneTypeDto;
import com.tierconnect.riot.commons.serdes.JsonSerializer;
import com.tierconnect.riot.commons.utils.Topics;
import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.entities.EdgeboxRule;
import com.tierconnect.riot.iot.entities.LogicalReader;
import com.tierconnect.riot.iot.entities.Shift;
import com.tierconnect.riot.iot.entities.ShiftZone;
import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.entities.Zone;
import com.tierconnect.riot.iot.entities.ZoneType;
import com.tierconnect.riot.iot.utils.Translator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.log4j.Logger;
import org.hibernate.Transaction;

/**
 * CacheLoaderTool class.
 *
 * @author jantezana
 * @author vramos
 * @version 2017/01/11
 * 
 * @deprecated 
 *         NO LON USED
 *         use  CacheLoaderToolNew INSTEAD !
 *         kafkaCacheLoader.sh and run.sh have already been changed
 *         TODO: DELETE this class when the new loader tool is proven
 */
public class CacheLoaderTool {

    private static final int THINGS_BATCH_SIZE = getValue("THINGS_BATCH_SIZE", 10_000);

    private static Logger logger = Logger.getLogger(CacheLoaderTool.class);

    private DataProvider dataProvider;
    private KafkaProducer<String, Object> producer;

    public static void main(String args[]) {
        try {
            CacheLoaderTool cacheLoaderTool = new CacheLoaderTool();
            cacheLoaderTool.init();
            cacheLoaderTool.publishEntities();
            cacheLoaderTool.exit();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            System.exit(1);
        }
    }

    public void init()
    throws CacheLoaderException, IOException {
        logger.info("*******************************************");
        logger.info("*****   Initializing CacheLoaderTool   ****");
        logger.info("*******************************************");
        initDataProvider();
        initProducer();
    }

    /**
     * Initializes data provider objects for mysql and mongo.
     */
    private void initDataProvider()
    throws CacheLoaderException {
        try {
            final MongoConfig mongo = getMongoConfig();
            logger.info("==> Mongo configuration <<==");
            logger.info("AuthDB="+mongo.mongoAuthDB);
            logger.info("mongoDB="+mongo.mongoDB);
            logger.info("mongoPrimary="+mongo.mongoPrimary);
            logger.info("username="+mongo.username);
            logger.info("password="+mongo.password);
            final JdbcConfig mysql = getSQlConfig();
            logger.info("==> MySQL configuration <<==");
            logger.info("dialect="+mysql.dialect);
            logger.info("driverClassName="+mysql.driverClassName);
            logger.info("jdbcUrl="+mysql.jdbcUrl);
            logger.info("userName="+mysql.userName);
            logger.info("password="+mysql.password);
            PopDBRequired.initJDBCDrivers();
            System.getProperties().put("hibernate.hbm2ddl.auto", "update");
            System.getProperties().put("hibernate.cache.use_second_level_cache", "false");
            System.getProperties().put("hibernate.cache.use_query_cache", "false");
            // Read the configurations .
            this.dataProvider = new DataProvider(mysql, mongo);
        } catch (Exception exception) {
            throw new CacheLoaderException(exception);
        }
    }

    /**
     * Initializes kafka producer
     *
     * @throws CacheLoaderException
     */
    private void initProducer()
    throws CacheLoaderException {
        KafkaConfig kafka;
        try {
            kafka = getKafkaConfig();
            logger.info("Init producer...");
            logger.info("KafkaHosts="+kafka.server);
            logger.info("zookeeperHosts="+kafka.zookeeper);
        } catch (Exception e) {
            throw new CacheLoaderException(e);
        }

        Properties propProducer = new Properties();
        propProducer.put("bootstrap.servers", kafka.server);
        propProducer.put("batch.size", 131072);
        propProducer.put("linger.ms", 20);
        propProducer.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        propProducer.put("value.serializer", JsonSerializer.class);

        producer = new KafkaProducer<>(propProducer);
    }

    /**
     * Loads cache entities from mysql and publish them into kafka
     * ___v1___cache___*
     */
    private void publishEntities() {
        try {

            Transaction  transaction = GroupService.getInstance().getGroupDAO().getSession().getTransaction();
            transaction.begin();

            List<Group> tenans = dataProvider.getTenants();

            logger.info("### Loadding tenant agnostic entities.");

            // Thing types.
            List<ThingTypeDto> thingTypeDtos = loadThingTypes(null);

            // Group.
            logger.info("Loading groups...");
            List<Group> groups = this.dataProvider.getGroups();
            List<GroupDto> groupDtos = Translator.convertToGroupDTOs(groups);
            publishGroups(groupDtos, Topics.CACHE_GROUP);

            // Group Types.
            logger.info("Loading group types...");
            List<GroupType> groupTypes = this.dataProvider.getGroupTypes();
            List<GroupTypeDto> groupTypeDtos = Translator.convertToGroupTypeDTOs(groupTypes);
            publishGroupTypes(groupTypeDtos, Topics.CACHE_GROUP_TYPE);

            // Edge box.
            loadEdgeBoxes(null);

            // Edge box rule.
            logger.info("Loading edgebox rules...");
            List<EdgeboxRule> edgeboxRules = this.dataProvider.getEdgeBoxRules();
            List<EdgeboxRuleDto> edgeboxRuleDtos = Translator.convertToEdgeboxRuleDTOs(edgeboxRules);
            publishEdgeboxRules(edgeboxRuleDtos, Topics.CACHE_EDGEBOX_RULE);

            // Connection.
            logger.info("Loading connections...");
            List<Connection> connections = this.dataProvider.getConnections();
            List<ConnectionDto> connectionsDtos = Translator.convertToConnectionDTOs(connections);
            publishConnections(connectionsDtos, Topics.CACHE_CONNECTION);

            // Zone types.
            logger.info("Loading zone types...");
            List<ZoneType> zoneTypes = this.dataProvider.getZoneTypes();
            List<ZoneTypeDto> zoneTypeDtos = Translator.convertToZoneTypeDTOs(zoneTypes);
            publishZoneTypes(zoneTypeDtos, Topics.CACHE_ZONE_TYPE);

            logger.info("Loading things...");


            int batchSize = THINGS_BATCH_SIZE;
            String envThingsBatchSize = System.getenv("THINGS_BATCH_SIZE");
            if (envThingsBatchSize != null && NumberUtils.isNumber(envThingsBatchSize)) {
                batchSize = Integer.parseInt(envThingsBatchSize);
            }
            // Publish things from mongo.
            for (Group tenant : tenans) {
                // Logical Reader.
                logger.info(String.format("### Loading logical readers for tenant: %s", tenant.getCode()));
                List<LogicalReaderDto> logicalReaderDtos = loadLogicalReaders(tenant);

                // Shifts.
                logger.info(String.format("### Loading shifts for tenant: %s", tenant.getCode()));
                List<ShiftDto> shiftDtos = loadShifts(tenant);

                // Shift Zones.
                logger.info(String.format("### Loading shift zones for tenant: %s", tenant.getCode()));
                loadShiftZones(tenant);

                // Zones.
                logger.info(String.format("### Loading zones for tenant: %s", tenant.getCode()));
                List<ZoneDto> zoneDtos = loadZones(tenant);

                logger.info("### Loadding things for tenant: " + tenant.getCode());

                List<Long> groupIds = this.dataProvider.getTenantChildrenIds(tenant);
                DBCursor thingsCursor = this.dataProvider.getThings(groupIds.toArray(new Long[groupIds.size()]));

                Map<Long, Long> dataTypesByThingField = buildDataTypesByThingField();
                int total = 0;
                while (thingsCursor.hasNext()) {
                    int count = 0;
                    List<ThingDto> list = new ArrayList<>();
                    while (count < batchSize && thingsCursor.hasNext()) {
                        BasicDBObject thingMongo = (BasicDBObject) thingsCursor.next();
                        try {
                            ThingDto thingDto = MongoDataProvider.buildThingDto(thingMongo, dataTypesByThingField, zoneDtos, logicalReaderDtos, shiftDtos, thingTypeDtos, groupDtos, groupTypeDtos);
                            list.add(thingDto);
                            count++;
                        } catch (Exception e) {
                            logger.error("Unable to load thing: " + thingMongo);
                        }
                    }
                    total += list.size();
                    logger.info(String.format("Publishing %d things. %d published things until now. ", list.size(), total));
                    List<ThingDto> thingPairs = Translator.convertToThingPairs(list, groups);
                    publishThings(thingPairs, Topics.CACHE_THING, tenant);
                }
                logger.info(String.format("Total things published: %d ", total));
            }

            transaction.commit();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void loadEdgeBoxes(Group tenant)
            throws Exception {
        // Edge box.
        List<Edgebox> edgeboxes = this.dataProvider.getEdgeBoxes(tenant);
        List<EdgeboxDto> edgeboxDtos = Translator.convertToEdgeboxDTOs(edgeboxes);

        List<EdgeboxDto> edgeBoxesToPublish = new ArrayList<>();
        List<EdgeboxConfigurationDto> edgeBoxConfigurationToPublish = new ArrayList<>();

        for (EdgeboxDto edgeboxDto : edgeboxDtos) {
            String edgeboxConfiguration = this.dataProvider.getConfiguration(edgeboxDto.code);
            edgeBoxesToPublish.add(edgeboxDto);
            EdgeboxConfigurationDto edgeboxConfigurationDto = new EdgeboxConfigurationDto();
            edgeboxConfigurationDto.id = edgeboxDto.id;
            edgeboxConfigurationDto.code = edgeboxDto.code;
            edgeboxConfigurationDto.type = edgeboxDto.type;
            edgeboxConfigurationDto.configuration = edgeboxConfiguration;
            GroupDto groupDto = new GroupDto();
            groupDto.id = edgeboxDto.group.id;
            edgeboxConfigurationDto.group = groupDto;
            edgeBoxConfigurationToPublish.add(edgeboxConfigurationDto);
        }

        publishEdgeboxes(edgeBoxesToPublish, Topics.CACHE_EDGEBOX, tenant);
        publishEdgeboxesConfigurations(edgeBoxesToPublish, edgeBoxConfigurationToPublish, Topics.CACHE_EDGEBOXES_CONFIGURATION, tenant);
    }


    private List<ThingTypeDto> loadThingTypes(Group tenant)
    throws Exception {
        logger.info("loading thing types...");
        List<ThingType> thingTypes = this.dataProvider.getThingTypes(tenant);
        List<ThingTypeDto> thingTypeDtos = Translator.convertToThingTypeDTOs(thingTypes);
        publishThingTypes(thingTypeDtos, Topics.CACHE_THING_TYPE, tenant);
        return thingTypeDtos;
    }

    private List<ZoneDto> loadZones(Group tenant)
    throws Exception {
        logger.info("Loading zones...");
        Map<Long, Map<String, Object>> properties = this.dataProvider.getZoneProperties();
        List<Zone> zones = this.dataProvider.getZones(tenant);
        List<ZoneDto> zoneDtos = Translator.convertToZoneDTOs(zones, properties);
        publishZones(zoneDtos, Topics.CACHE_ZONE, tenant);
        return zoneDtos;
    }

    private List<ShiftDto> loadShifts(Group tenant)
    throws Exception {
        logger.info("Loading Shifts...");
        List<Shift> shifts = this.dataProvider.getShifts(tenant);
        List<ShiftDto> shiftDtos = Translator.convertToShiftDTOs(shifts);
        publishShifts(shiftDtos, Topics.CACHE_SHIFT, tenant);
        return shiftDtos;
    }

    private void loadShiftZones(Group tenant)
    throws Exception {
        logger.info("Loading Shift Zones...");
        List<ShiftZone> shiftZones = this.dataProvider.getShiftZones(tenant);
        List<ShiftZoneDto> shiftZoneDtos = Translator.convertToShiftZoneDTOs(shiftZones, new HashMap<>());
        publishShiftZones(shiftZoneDtos, Topics.CACHE_SHIFT_ZONE, tenant);
    }

    private List<LogicalReaderDto> loadLogicalReaders(Group tenant)
    throws Exception {
        logger.info("Loading Logical readers...");
        List<LogicalReader> logicalReaders = this.dataProvider.getLogicalReaders(tenant);
        List<LogicalReaderDto> logicalReaderDtos = Translator.convertToLogicalReaderDTOs(logicalReaders);
        publishLogicalReaders(logicalReaderDtos, Topics.CACHE_LOGICAL_READER, tenant);
        return logicalReaderDtos;
    }


    /**
     * Publish thing types
     *
     * @param thingTypeDtos
     * @param cacheThingType
     * @param tenant
     */
    private void publishThingTypes(List<ThingTypeDto> thingTypeDtos,
                                   Topics cacheThingType,
                                   Group tenant) {
        Preconditions.checkNotNull(thingTypeDtos, "The thing type DTOs is null");
        long start = System.currentTimeMillis();
        for (ThingTypeDto thingTypeDto : thingTypeDtos) {
            String idKey = "ID-" + String.valueOf(thingTypeDto.id);
            String codeKey = "CODE-" + (tenant != null ? (tenant.getCode() + "-") : "") + thingTypeDto.code;
            // Id
            ProducerRecord<String, Object> producerRecord = new ProducerRecord<String, Object>(cacheThingType.getKafkaName(), idKey, thingTypeDto);
            this.producer.send(producerRecord);
            // Code
            producerRecord = new ProducerRecord<String, Object>(cacheThingType.getKafkaName(), codeKey, thingTypeDto);
            this.producer.send(producerRecord);
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Published thing type: key: %s", idKey));
                logger.debug(String.format("Published thing type: key: %s", codeKey));
            }
        }
        long end = System.currentTimeMillis();
        logger.info(String.format("Write %d thing types in %d milliseconds", thingTypeDtos.size(), end - start));
    }

    /**
     * Publish zones.
     *
     * @param zoneDtos
     * @param topic
     * @param tenant
     */
    private void publishZones(List<ZoneDto> zoneDtos,
                              Topics topic,
                              Group tenant) {
        long start = System.currentTimeMillis();
        for (ZoneDto zoneDto : zoneDtos) {
            String idKey = "ID-" + String.valueOf(zoneDto.id);
            String codeKey = "CODE-" + (tenant != null ? (tenant.getCode() + "-") : "") + zoneDto.code;

            // Id.
            ProducerRecord<String, Object> producerRecord = new ProducerRecord<String, Object>(topic.getKafkaName(), idKey, zoneDto);
            this.producer.send(producerRecord);
            // Code.
            producerRecord = new ProducerRecord<String, Object>(topic.getKafkaName(), codeKey, zoneDto);
            this.producer.send(producerRecord);
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Published zone: key: %s", idKey));
                logger.debug(String.format("Published zone: key: %s", codeKey));
            }
        }
        long end = System.currentTimeMillis();
        logger.info(String.format("Write %d zones in %d milliseconds", zoneDtos.size(), end - start));
    }

    /**
     * Publish groups.
     *
     * @param groupDtos the list of group DTOs
     * @param topic     the topic
     */
    private void publishGroups(List<GroupDto> groupDtos,
                               Topics topic) {
        long start = System.currentTimeMillis();
        for (GroupDto groupDto : groupDtos) {
            String idKey = "ID-" + String.valueOf(groupDto.id);
            String codeKey = "CODE-" + groupDto.code;

            // Id.
            ProducerRecord<String, Object> producerRecord = new ProducerRecord<String, Object>(topic.getKafkaName(), idKey, groupDto);
            this.producer.send(producerRecord);
            // Code.
            producerRecord = new ProducerRecord<String, Object>(topic.getKafkaName(), codeKey, groupDto);
            this.producer.send(producerRecord);
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Published group: key: %s", idKey));
                logger.debug(String.format("Published group: key: %s", codeKey));
            }
        }
        long end = System.currentTimeMillis();
        logger.info(String.format("Write %d groups in %d milliseconds", groupDtos.size(), end - start));
    }

    /**
     * publish group types.
     *
     * @param groupTypeDtos the list of group type DTOs
     * @param topic         the topic
     */
    private void publishGroupTypes(List<GroupTypeDto> groupTypeDtos,
                                   Topics topic) {
        long start = System.currentTimeMillis();
        for (GroupTypeDto groupTypeDto : groupTypeDtos) {
            String idKey = "ID-" + String.valueOf(groupTypeDto.id);
            // Id.
            ProducerRecord<String, Object> producerRecord = new ProducerRecord<String, Object>(topic.getKafkaName(), idKey, groupTypeDto);
            this.producer.send(producerRecord);
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Published group type: key: %s", idKey));
            }
        }
        long end = System.currentTimeMillis();
        logger.info(String.format("Write %d group types in %d milliseconds", groupTypeDtos.size(), end - start));
    }

    /**
     * Publish shifts.
     *
     * @param shiftDtos
     * @param topic
     * @param tenant
     */
    private void publishShifts(List<ShiftDto> shiftDtos,
                               Topics topic,
                               Group tenant) {
        long start = System.currentTimeMillis();
        for (ShiftDto shiftDto : shiftDtos) {
            String idKey = "ID-" + String.valueOf(shiftDto.id);
            String codeKey = "CODE-" + (tenant != null ? (tenant.getCode() + "-") : "") + shiftDto.code;

            // Id.
            ProducerRecord<String, Object> producerRecord = new ProducerRecord<String, Object>(topic.getKafkaName(), idKey, shiftDto);
            this.producer.send(producerRecord);
            // Code.
            producerRecord = new ProducerRecord<String, Object>(topic.getKafkaName(), codeKey, shiftDto);
            this.producer.send(producerRecord);
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Published shift: key: %s", idKey));
                logger.debug(String.format("Published shift: key: %s", codeKey));
            }
        }
        long end = System.currentTimeMillis();
        logger.info(String.format("Write %d shifts in %d milliseconds", shiftDtos.size(), end - start));
    }

    /**
     * Publish shift zones.
     *
     * @param shiftZoneDtos
     * @param topic
     * @param tenant
     */
    private void publishShiftZones(List<ShiftZoneDto> shiftZoneDtos,
                                   Topics topic,
                                   Group tenant) {
        long start = System.currentTimeMillis();
        for (ShiftZoneDto shiftZoneDto : shiftZoneDtos) {
            String idKey = "ID-" + String.valueOf(shiftZoneDto.shift.id);
            String codeKey = "CODE-" + (tenant != null ? (tenant.getCode() + "-") : "") + String.valueOf(shiftZoneDto.shift.id);
            // Id.
            ProducerRecord<String, Object> producerRecord = new ProducerRecord<String, Object>(topic.getKafkaName(), idKey, shiftZoneDto);
            this.producer.send(producerRecord);

            producerRecord = new ProducerRecord<String, Object>(topic.getKafkaName(), codeKey, shiftZoneDto);
            this.producer.send(producerRecord);

            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Published shift zone: key: %d", shiftZoneDto.shift.id));
            }
        }
        long end = System.currentTimeMillis();
        logger.info(String.format("Write %d shift zones in %d milliseconds", shiftZoneDtos.size(), end - start));
    }

    /**
     * Publish logical readers.
     *
     * @param logicalReaderDtos
     * @param topic
     * @param tenant
     */
    private void publishLogicalReaders(List<LogicalReaderDto> logicalReaderDtos,
                                       Topics topic,
                                       Group tenant) {
        long start = System.currentTimeMillis();
        for (LogicalReaderDto logicalReaderDto : logicalReaderDtos) {
            String idKey = "ID-" + String.valueOf(logicalReaderDto.id);
            String codeKey = "CODE-" + (tenant != null ? (tenant.getCode() + "-") : "") + logicalReaderDto.code;

            // Id.
            ProducerRecord<String, Object> producerRecord = new ProducerRecord<String, Object>(topic.getKafkaName(), idKey, logicalReaderDto);
            this.producer.send(producerRecord);
            // Code.
            producerRecord = new ProducerRecord<String, Object>(topic.getKafkaName(), codeKey, logicalReaderDto);
            this.producer.send(producerRecord);
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Published logical reader: key: %s", idKey));
                logger.debug(String.format("Published logical reader: key: %s", codeKey));
            }
        }
        long end = System.currentTimeMillis();
        logger.info(String.format("Write %d logical readers in %d milliseconds", logicalReaderDtos.size(), end - start));
    }

    /**
     * Publish edge boxes.
     *
     * @param edgeboxDtos
     * @param topic
     * @param tenant
     */
    private void publishEdgeboxes(List<EdgeboxDto> edgeboxDtos,
                                  Topics topic,
                                  Group tenant) {
        long start = System.currentTimeMillis();
        for (EdgeboxDto edgeboxDto : edgeboxDtos) {
            String idKey = "ID-" + String.valueOf(edgeboxDto.id);
            String codeKey = "CODE-" + (tenant != null ? (tenant.getCode() + "-") : "") + edgeboxDto.code;
            // Id.
            ProducerRecord<String, Object> producerRecord = new ProducerRecord<String, Object>(topic.getKafkaName(), idKey, edgeboxDto);
            this.producer.send(producerRecord);
            // Code.
            producerRecord = new ProducerRecord<String, Object>(topic.getKafkaName(), codeKey, edgeboxDto);
            this.producer.send(producerRecord);
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Published edgebox: key: %s", idKey));
                logger.debug(String.format("Published edgebox: key: %s", codeKey));
            }
        }

        long end = System.currentTimeMillis();
        logger.info(String.format("Write %d edge boxes in %d milliseconds", edgeboxDtos.size(), end - start));
    }

    /**
     * Publish edge boxes configurations.
     *
     * @param edgeboxDtos
     * @param configurations
     * @param topic
     * @param tenant
     */
    private void publishEdgeboxesConfigurations(List<EdgeboxDto> edgeboxDtos,
                                                List<EdgeboxConfigurationDto> configurations,
                                                Topics topic,
                                                Group tenant) {
        long start = System.currentTimeMillis();
        for (int i = 0; i < edgeboxDtos.size(); i++) {
            EdgeboxDto edgeboxDto = edgeboxDtos.get(i);
            String idKey = "ID-" + String.valueOf(edgeboxDto.id);
            String codeKey = "CODE-" + (tenant != null ? (tenant.getCode() + "-") : "") + edgeboxDto.code;
            // Id.
            ProducerRecord<String, Object> producerRecord = new ProducerRecord<String, Object>(topic.getKafkaName(), idKey, configurations.get(i));
            this.producer.send(producerRecord);
            // Code.
            producerRecord = new ProducerRecord<String, Object>(topic.getKafkaName(), codeKey, configurations.get(i));
            this.producer.send(producerRecord);
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Published edgebox: key: %s", idKey));
                logger.debug(String.format("Published edgebox: key: %s", codeKey));
            }

        }

        long end = System.currentTimeMillis();
        logger.info(String.format("Write %d edge boxes configurations in %d milliseconds", edgeboxDtos.size(), end - start));
    }


    /**
     * Publish edge box rules.
     *
     * @param edgeboxRuleDtos the list of edge box rule DTOs
     * @param topic           the topic
     */
    private void publishEdgeboxRules(List<EdgeboxRuleDto> edgeboxRuleDtos,
                                     Topics topic) {
        long start = System.currentTimeMillis();
        for (EdgeboxRuleDto edgeboxRuleDto : edgeboxRuleDtos) {
            String idKey = "ID-" + String.valueOf(edgeboxRuleDto.id);
            // Id.
            ProducerRecord<String, Object> producerRecord = new ProducerRecord<String, Object>(topic.getKafkaName(), idKey, edgeboxRuleDto);
            this.producer.send(producerRecord);

            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Published edgebox rule: key: %s", idKey));
            }
        }
        long end = System.currentTimeMillis();
        logger.info(String.format("Write %d edge box rules in %d milliseconds", edgeboxRuleDtos.size(), end - start));
    }

    /**
     * Publish edge boxes.
     *
     * @param connectionDtos the list of connection DTOs
     * @param topic          the topic
     */
    private void publishConnections(List<ConnectionDto> connectionDtos,
                                    Topics topic) {
        long start = System.currentTimeMillis();
        for (ConnectionDto connectionDto : connectionDtos) {
            String idKey = "ID-" + String.valueOf(connectionDto.id);
            String codeKey = "CODE-" + connectionDto.code;
            // Id.
            ProducerRecord<String, Object> producerRecord = new ProducerRecord<String, Object>(topic.getKafkaName(), idKey, connectionDto);
            this.producer.send(producerRecord);
            // Code.
            producerRecord = new ProducerRecord<String, Object>(topic.getKafkaName(), codeKey, connectionDto);
            this.producer.send(producerRecord);
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Published connection: key: %s", idKey));
                logger.debug(String.format("Published connection: key: %s", codeKey));
            }
        }
        long end = System.currentTimeMillis();
        logger.info(String.format("Write %d connections in %d milliseconds", connectionDtos.size(), end - start));
    }

    /**
     * Publish zone types.
     *
     * @param zoneTypeDtos the list of zone type DTOs
     * @param topic        the topic
     */
    private void publishZoneTypes(List<ZoneTypeDto> zoneTypeDtos,
                                  Topics topic) {
        long start = System.currentTimeMillis();
        for (ZoneTypeDto zoneTypeDto : zoneTypeDtos) {
            String idKey = "ID-" + String.valueOf(zoneTypeDto.id);
            String codeKey = "CODE-" + zoneTypeDto.code;

            // Id.
            ProducerRecord<String, Object> producerRecord = new ProducerRecord<String, Object>(topic.getKafkaName(), idKey, zoneTypeDto);
            this.producer.send(producerRecord);
            // Code.
            producerRecord = new ProducerRecord<String, Object>(topic.getKafkaName(), codeKey, zoneTypeDto);
            this.producer.send(producerRecord);
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Published zone type: key: %s", idKey));
                logger.debug(String.format("Published zone type: key: %s", codeKey));
            }
        }
        long end = System.currentTimeMillis();
        logger.info(String.format("Write %d zone types in %d milliseconds", zoneTypeDtos.size(), end - start));
    }

    /**
     * Publish things into kafka
     * ___v1___cache___things
     *
     * @param thingDtos the list of thing DTOs
     * @param topic     the topic
     */
    public void publishThings(List<ThingDto> thingDtos,
                              Topics topic,
                              Group tenant) {
        Preconditions.checkNotNull(thingDtos, "The thingDtos is null");
        Preconditions.checkNotNull(topic, "The topic is null");
        long start = System.currentTimeMillis();
        ProducerRecord<String, Object> producerRecord;
        for (ThingDto thingDto : thingDtos) {
            String key = (tenant != null ? (tenant.getCode() + "-") : "") + thingDto.thingType.code + "-"
                + thingDto.serialNumber;
            producerRecord = new ProducerRecord<String, Object>(topic.getKafkaName(), key, thingDto);
            this.producer.send(producerRecord);
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Published thing: key: %s", key));
            }
        }
        long end = System.currentTimeMillis();
        logger.info(String.format("Write %d things in %d milliseconds", thingDtos.size(), end - start));
    }

    /**
     * Exit.
     */
    private void exit() {
        logger.info("Finished .....");
        this.producer.close();
        System.exit(0);
    }

    /**
     * build data types by thing field.
     *
     * @throws Exception the exception
     */
    private Map<Long, Long> buildDataTypesByThingField()
    throws Exception {
        Map<Long, Long> dataTypesByThingField = new HashMap<>();
        List<ThingType> thingTypes = this.dataProvider.getThingTypes();
        thingTypes.forEach(tt -> {
            tt.getThingTypeFields().forEach(ttf -> {
                dataTypesByThingField.put(ttf.getId(), ttf.getDataType().getId());
            });
        });
        return dataTypesByThingField;
    }

    private static int getValue(String env,
                                int defaultValue) {
        int value = defaultValue;
        String envValue = System.getenv(env);
        if (envValue != null && NumberUtils.isNumber(envValue)) {
            value = Integer.parseInt(envValue);
        }
        return value;
    }

    private MongoConfig getMongoConfig(){
        logger.info("Creating mongo connection ...");
        String mongoPrimary = System.getProperty("mongo.primary");
        String mongoSecondary = System.getProperty("mongo.secondary");
        String mongoReplicaSet = System.getProperty("mongo.replicaset");
        String mongoSsl = System.getProperty("mongo.ssl");
        String mongoUserName = System.getProperty("mongo.username");
        String mongoPassword = System.getProperty("mongo.password");
        String mongoAuthenticationDb = System.getProperty("mongo.authdb");
        String mongoDataBase = System.getProperty("mongo.db");
        String mongoSharding = System.getProperty("mongo.sharding");
        String mongoConnectionTimeout = System.getProperty("mongo.connectiontimeout");
        String mongoMaxPoolSize = System.getProperty("mongo.maxpoolsize");
        MongoConfig mongoConfig = new MongoConfig();
        mongoConfig.mongoPrimary = mongoPrimary;
        mongoConfig.mongoSecondary =  mongoSecondary;
        mongoConfig.mongoReplicaSet = mongoReplicaSet;
        mongoConfig.mongoSSL = Boolean.valueOf(mongoSsl);
        mongoConfig.username = mongoUserName;
        mongoConfig.password = mongoPassword;
        mongoConfig.mongoAuthDB = mongoAuthenticationDb;
        mongoConfig.mongoDB = mongoDataBase;
        mongoConfig.mongoSharding = Boolean.valueOf(mongoSharding);
        mongoConfig.mongoConnectTimeout = Integer.parseInt(mongoConnectionTimeout);
        mongoConfig.mongoMaxPoolSize = Integer.parseInt(mongoMaxPoolSize);

        return mongoConfig;
    }

    private KafkaConfig getKafkaConfig(){
        KafkaConfig kafkaConfig = new KafkaConfig();
        logger.info("Creating kafka connection ...");
        String kafkaZookeeper = System.getProperty("kafka.zookeeper");
        String kafkaServers = System.getProperty("kafka.servers");
        kafkaConfig.zookeeper = kafkaZookeeper;
        kafkaConfig.server = kafkaServers;
        return kafkaConfig;
    }

    private JdbcConfig getSQlConfig(){
        logger.info("Creating sql connection ...");
        JdbcConfig jdbcConfig = new JdbcConfig();
        String driver = System.getProperty("hibernate.connection.driver_class");
        String dialect = System.getProperty("hibernate.dialect");
        String username = System.getProperty("hibernate.connection.username");
        String password = System.getProperty("hibernate.connection.password");
        String url = System.getProperty("hibernate.connection.url");
        String hazelcastNativeClientAddress = System.getProperty("hibernate.cache.hazelcast.native_client_address");
        jdbcConfig.dialect = dialect;
        jdbcConfig.driverClassName = driver;
        jdbcConfig.userName = username;
        jdbcConfig.password = password;
        jdbcConfig.jdbcUrl = url;
        jdbcConfig.hazelcastNativeClientAddress = hazelcastNativeClientAddress;

        return jdbcConfig;
    }

}
