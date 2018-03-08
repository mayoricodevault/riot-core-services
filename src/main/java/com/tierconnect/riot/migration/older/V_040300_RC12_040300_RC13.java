package com.tierconnect.riot.migration.older;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tierconnect.riot.appcore.dao.RoleResourceDAO;
import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.popdb.PopDBRequired;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;
import com.tierconnect.riot.appcore.services.*;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.popdb.PopDBIOTUtils;
import com.tierconnect.riot.iot.popdb.PopDBRequiredIOT;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.MongoDBHelper;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import org.apache.log4j.Logger;
import org.jose4j.json.internal.json_simple.JSONArray;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author : vealaro
 * @version : 4.3.0 RC-13
 * @date : 9/12/16
 */
@Deprecated
public class V_040300_RC12_040300_RC13 implements MigrationStepOld {

    private Logger logger = Logger.getLogger(V_040300_RC12_040300_RC13.class);

    @Override
    public List<Integer> getFromVersions() {
        return Collections.singletonList(4030012);
    }

    @Override
    public int getToVersion() {
        return 4030013;
    }

    @Override
    public void migrateSQLBefore() throws Exception {
        DBHelper dbHelperSQL = new DBHelper();
        Boolean table = dbHelperSQL.existTable("parameters");
        Boolean conditionType = dbHelperSQL.existColumn("edgeboxRule", "conditionType");
        Boolean parameterConditionType = dbHelperSQL.existColumn("edgeboxRule", "parameterConditionType");
        Boolean parameterType = dbHelperSQL.existColumn("edgebox", "parameterType");
        Boolean parentField_id = dbHelperSQL.existColumn("apc_field", "parentField_id");
        if (!table || !conditionType || !parameterConditionType || !parameterType || !parentField_id) {
            String databaseType = DBHelper.getDataBaseType();
            dbHelperSQL.executeSQLFile("sql/" + databaseType + "/V040300_RC12_to_040300_RC13.sql");
        }
    }

    @Override
    public void migrateHibernate() throws Exception {
        Group rootGroup = GroupService.getInstance().get(1L);
        updateCoreBridge(rootGroup);
        insertLDAPFields();
        updateLDAPFields();
        deleteUserFromConnectionType(rootGroup);
        addNewTableResourceParameters();
        populateParametersMigra();
        populateTopicForService();
        populateConditionTypeAndType();
    }

    @Override
    public void migrateSQLAfter() throws Exception {
//        MongoDAOUtil.setupMongodb(Configuration.getProperty("mongo.host"),
//                Integer.parseInt(Configuration.getProperty("mongo.port")),
//                Configuration.getProperty("mongo.mongoAddressReplica"),
//                Configuration.getProperty("mongo.db"),
//                null,
//                null,
//                Configuration.getProperty("mongo.username"),
//                Configuration.getProperty("mongo.password"));
        MongoDBHelper dbHelper = new MongoDBHelper();
        dbHelper.executeMongoFile("no-sql/mongo/V_040300_RC12_040300_RC13.js");
    }
    /**
     * Update and delete LDAP Fields
     */
    private void updateLDAPFields() {
        Field nativeField = FieldService.getInstance().selectByName("native");
        Field ldapField = FieldService.getInstance().selectByName("ldap");
        Field ldapConnectionField = FieldService.getInstance().selectByName("ldapConnection");
        Field ldapPasswordRequiredField = FieldService.getInstance().selectByName("passwordRequired");
        List<Group> groupList = GroupService.getGroupDAO().selectAll();
        for (Group aGroup:groupList) {
            GroupField nativeGFValue = GroupFieldService.getInstance().selectByGroupField(aGroup, nativeField);
            if (nativeGFValue != null) {
                logger.debug("Group ["+aGroup.getName()+"] has native GroupField");
                GroupField ldapGFValue = GroupFieldService.getInstance().selectByGroupField(aGroup, ldapField);
                GroupField ldapConnectionGFValue = GroupFieldService.getInstance().selectByGroupField(aGroup, ldapConnectionField);
                // updating LDAP values
                if ((ldapGFValue != null) && (ldapConnectionGFValue != null)) {
                    Field newLDAPAuthenticationMode = FieldService.getInstance().selectByName("authenticationMode");
                    Field newLDAPADConnection = FieldService.getInstance().selectByName("ldapAdConnection");
                    Boolean isNative = Boolean.valueOf(nativeGFValue.getValue());
                    if (isNative) {
                        PopDBUtils.popGroupField(aGroup, newLDAPAuthenticationMode, "nativeAuthentication");
                        PopDBUtils.popGroupField(aGroup, newLDAPADConnection, "");
                    } else {
                        PopDBUtils.popGroupField(aGroup, newLDAPAuthenticationMode, "ldapAdAuthentication");
                        PopDBUtils.popGroupField(aGroup, newLDAPADConnection, ldapConnectionGFValue.getValue());
                    }
                } else {
                    logger.warn("Is not possible to migrate LDAP values for group [" + aGroup.getName() + "].");
                }
                // Deleting old LDAP Values (groupfield)
                GroupFieldService.getInstance().delete(nativeGFValue);
                if (ldapGFValue != null) {
                    GroupFieldService.getInstance().delete(ldapGFValue);
                }
                if (ldapConnectionGFValue != null) {
                    GroupFieldService.getInstance().delete(ldapConnectionGFValue);
                }
                if (ldapPasswordRequiredField != null) {
                    GroupField ldapPasswordRequiredGFValue = GroupFieldService.getInstance().selectByGroupField(aGroup, ldapPasswordRequiredField);
                    if (ldapPasswordRequiredGFValue != null) {
                        GroupFieldService.getInstance().delete(ldapPasswordRequiredGFValue);
                    }
                }
            } else {
                logger.debug("Group ["+aGroup.getName()+"] has NOT native GroupField");
            }
        }
        // Deleting old LDAP Values (apc_field)
        if (nativeField != null) {
            FieldService.getInstance().delete(nativeField);
        }
        if (ldapField != null) {
            FieldService.getInstance().delete(ldapField);
        }
        if (ldapConnectionField != null) {
            FieldService.getInstance().delete(ldapConnectionField);
        }
        if (ldapPasswordRequiredField != null) {
            FieldService.getInstance().delete(ldapPasswordRequiredField);
        }

    }

    private void deleteUserFromConnectionType(Group rootGroup){
        ConnectionType connectionType=ConnectionTypeService.getInstance().getConnectionTypeByCode("ldap");

        if (connectionType != null){
            List<Map<String, Object>> propertiesDefinition = new ArrayList<>();
            propertiesDefinition.add(PopDBRequired.newPropertyDefinition("userDn", "UserDn", "String", true));
            propertiesDefinition.add(PopDBRequired.newPropertyDefinition("password", "Password", "String", true));
            propertiesDefinition.add(PopDBRequired.newPropertyDefinition("base", "Base", "String", true));
            propertiesDefinition.add(PopDBRequired.newPropertyDefinition("url", "Url", "String", true));
            propertiesDefinition.add(PopDBRequired.newPropertyDefinition("referral", "Referral ('follow' by default)", "String", false));
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                connectionType.setPropertiesDefinitions(objectMapper.writeValueAsString(propertiesDefinition));
                ConnectionTypeService.getInstance().update(connectionType);
            } catch (JsonProcessingException e) {
                logger.warn("Migrating LDAP connection, error updating connectionType", e);
            }
        }
        else {
            PopDBRequired.populateLDAPConnection(rootGroup);
        }
    }

    /**
     * Insert data for LDAP
     */
    private void insertLDAPFields() {
        Group rootGroup = GroupService.getInstance().getRootGroup();
        // Authentication values begin
        Field f50 = PopDBUtils.popFieldService("authenticationMode", "authenticationMode", "Authentication Mode",
                rootGroup, "Security Configuration", "java.lang.String", 3L, false);
         PopDBUtils.popFieldWithParentService("ldapAdAuthentication", "ldapAdAuthentication", "LDAP/AD",
                rootGroup, "Security Configuration", "java.lang.String", 3L, false, f50);
        Field f52 = PopDBUtils.popFieldWithParentService("nativeAuthentication", "nativeAuthentication", "Native",
                rootGroup, "Security Configuration", "java.lang.String", 3L, false, f50);
        Field f53 = PopDBUtils.popFieldService("ldapAdConnection", "ldapAdConnection", "LDAP/AD Connection",
                rootGroup, "Security Configuration", "java.lang.String", 3L, true);
        // Authentication values end.
    }

    /**
     * add new UDF to Thing Type CoreBridge "lpt_cache"
     */
    private void updateCoreBridge(Group rootGroup) {
        try {
            ThingTypeTemplate coreBridge = ThingTypeTemplateService.getInstance().getByName("CoreBridge");
            if (coreBridge == null) {
                logger.info("Insert ThingTypeTemplate [CoreBridge] ");
                PopDBRequiredIOT.insertCoreBridgeTemplate4_3RC13(rootGroup);
            } else {
                ThingTypeFieldTemplate lptCacheFieldTemplate = ThingTypeFieldTemplateService.getInstance().getThingTypeFieldTemplateByThingTypeTemplate(coreBridge.getId(), "lpt_cache");
                if (lptCacheFieldTemplate == null){
                    logger.info("Update ThingTypeTemplate [CoreBridge] with new thing type field [lpt_cache] ");
                    PopDBRequiredIOT.insertUdfField("lpt_cache", "lpt_cache", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_NUMBER.value), "DATA_TYPE", true, coreBridge, "");
                }
            }
        } catch (NonUniqueResultException e) {
            logger.error("Error in add new UDF [lpt_cache] to thing type template CoreBridge ");
        }
    }


    /**
     * Populate paremeters
     */
    public void populateParametersMigra() {
        String conditionType = "CONDITION_TYPE";
        String bridgeType = "BRIDGE_TYPE";
        Parameters parameters = new Parameters(conditionType, "ALWAYS_TRUE", "@SYSTEM_PARAMETERS_CONDITION_TYPE_ALWAYS_TRUE", null);
        ParametersService.getInstance().insert(parameters);
        parameters = new Parameters(conditionType, "CEP", "@SYSTEM_PARAMETERS_CONDITION_TYPE_CEP",null);

        ParametersService.getInstance().insert(parameters);
        parameters = new Parameters(bridgeType, "edge", "@SYSTEM_PARAMETERS_BRIDGE_TYPE_EDGE",
                "{\"thingTypeCode\":\"\",\"logRawMessages\":0,\"numberOfThreads\":10,\"mqtt\":{\"connectionCode\":\"\"}," +
                        "\"zoneDwellFilter\":{\"active\":0,\"unlockDistance\":25,\"inZoneDistance\":10,\"zoneDwellTime\":300," +
                        "\"lastDetectTimeActive\":1,\"lastDetectTimeWindow\":0},\"timeDistanceFilter\":{\"active\":0,\"time\":0," +
                        "\"distance\":10},\"timeZoneFilter\":{\"active\":0,\"time\":10},\"zoneChangeFilter\":{\"active\":0}}");
        ParametersService.getInstance().insert(parameters);
        parameters = new Parameters(bridgeType, "StarFLEX", "@SYSTEM_PARAMETERS_BRIDGE_TYPE_STARFLEX",
                "{\"thingTypeCode\":\"\",\"thingTypeCodeDevice\":\"STR_400\",\"messageMode\":\"FlexTag\"," +
                        "\"mqtt\":{\"connectionCode\":\"\"},\"mongo\":{\"connectionCode\":\"\"}}");
        ParametersService.getInstance().insert(parameters);
        parameters = new Parameters(bridgeType, "FTP", "@SYSTEM_PARAMETERS_BRIDGE_TYPE_FTP",
                "{\"thingTypeCode\":\"\",\"logRawMessages\":0,\"ftp\":{\"connectionCode\":\"\"}," +
                        "\"path\":\"/StoreReferenceData\",\"pattern\":\"*.COLOUR\",\"patternCaseSensitive\":false," +
                        "\"schedule\":\"0 0/10 * 1/1 * ? *\",\"configParser\":{\"parserType\":\"fixedlength\"," +
                        "\"separator\":null,\"fieldLengths\":\"3,16,1\",\"ignoreFooter\":true,\"ignoreHeader\":false," +
                        "\"fieldNames\":[\"Code\",\"Description\",\"Action\"],\"columnNumberAsSerial\":0},\"processPolicy\":\"Move\"," +
                        "\"localBackupFolder\":\"/tmp\",\"ftpDestinationFolder\":\"processed/colour\",\"mqtt\":{\"connectionCode\":\"\"}}");
        ParametersService.getInstance().insert(parameters);
        parameters = new Parameters(bridgeType, "GPS", "@SYSTEM_PARAMETERS_BRIDGE_TYPE_GPS",
                "{\"thingTypeCode\":\"\",\"logRawMessages\":0,\"mqtt\":{\"connectionCode\":\"\"}," +
                        "\"timeDistanceFilter\":{\"active\":0,\"time\":0,\"distance\":10}," +
                        "\"timeZoneFilter\":{\"active\":0,\"time\":10},\"geoforce\":{\"host\":\"app.geoforce.com\"," +
                        "\"path\":\"/feeds/asset_inventory.xml\",\"port\":443,\"user\":\"datafeed@mojix.com\"," +
                        "\"password\":\"AHmgooCk8l0jo95f7YSo\",\"period\":60},\"zoneChangeFilter\":{\"active\":0}}");
        ParametersService.getInstance().insert(parameters);
        parameters = new Parameters(bridgeType, "OPEN_RTLS", "@SYSTEM_PARAMETERS_BRIDGE_TYPE_OPEN_RTLS",null);
        ParametersService.getInstance().insert(parameters);
        parameters = new Parameters(bridgeType, "core", "@SYSTEM_PARAMETERS_BRIDGE_TYPE_CORE",
                "{\"threadDispatchMode\":1,\"numberOfThreads\":32,\"mqtt\":{\"connectionCode\":\"\",\"topics\":[\"/v1/data/ALEB/#\"," +
                        "\"/v1/data/APP2/#\",\"/v1/data/STAR/#\",\"/v1/data/STAR1/#\"]},\"mongo\":{\"connectionCode\":\"\"}," +
                        "\"sequenceNumberLogging\":{\"active\":0,\"TTL\":86400,\"GC_GRACE_SECONDS\":0},\"sourceRule\":{\"active\":0}," +
                        "\"CEPLogging\":{\"active\":0},\"pointInZoneRule\":{\"active\":1},\"doorEventRule\":{\"active\":1}," +
                        "\"shiftZoneRule\":{\"active\":0,\"shiftProperty\":\"shift\",\"zoneViolationStatusProperty\":\"zoneViolationStatus\"," +
                        "\"zoneViolationFlagProperty\":\"zoneViolationFlag\"},\"checkMultilevelReferences\":{\"active\":0}}");
        ParametersService.getInstance().insert(parameters);
        logger.info("Parameters has been migrated successfully.");
    }

    /**
     * Add table as new resource
     */
    public static void addNewTableResourceParameters() {
        ResourceService resourceService = ResourceService.getInstance();
        QResource qResource = QResource.resource;
        RoleResourceService roleResourceService = RoleResourceServiceBase.getInstance();
        GroupService groupService = GroupService.getInstance();
        Group rootGroup = groupService.getRootGroup();
        //New Resources
        Resource moduleControl = ResourceService.getInstance().getResourceDAO().selectBy("name", "Control");
        Role rootRole = RoleService.getInstance().getRootRole();

        QRoleResource qRoleResource = QRoleResource.roleResource;
        RoleResourceDAO roleResourceDAO = RoleResourceService.getInstance().getRoleResourceDAO();
        Resource re = null;
        if (resourceService.countList(qResource.name.eq("parameters")) == 0) {
            re = resourceService.insert(Resource.getClassResource(rootGroup, Parameters.class, moduleControl));
            List<Role> roles = roleResourceDAO.getQuery().where(qRoleResource.resource.name.eq("thingType")).distinct().list(qRoleResource.role);
            for (Role role : roles){
                roleResourceService.insert(role, re, "riuda");
            }
            RoleResourceService.getInstance().insert(rootRole, re, re.getAcceptedAttributes());
            ResourceService.getInstance().update(re);
        }
    }

    /**
     * Populate field conditionType in table edgeBoxRule
     */
    public static void populateConditionTypeAndType() {

        List<Edgebox> lstEdgeBox = EdgeboxService.getInstance().listPaginated(null, null);
        if ((lstEdgeBox!=null) && (!lstEdgeBox.isEmpty()) ) {
            for (Edgebox edgebox: lstEdgeBox) {
                edgebox.setParameterType("BRIDGE_TYPE");
                edgebox.setType(PopDBIOTUtils.getCorrectBridgeTypeCode(edgebox));
                EdgeboxService.getInstance().update(edgebox);
            }
        }

        List<EdgeboxRule> lstEdgeBoxRule = EdgeboxRuleService.getInstance().listPaginated(null, null);
        if ((lstEdgeBoxRule!=null) && (!lstEdgeBoxRule.isEmpty()) ) {
            for (EdgeboxRule edgeboxRule: lstEdgeBoxRule) {
                if( (edgeboxRule.getRule().contains("(1=1)")) ) {
                    edgeboxRule.setParameterConditionType("CONDITION_TYPE");
                    edgeboxRule.setConditionType("ALWAYS_TRUE");
                } else {
                    edgeboxRule.setParameterConditionType("CONDITION_TYPE");
                    edgeboxRule.setConditionType("CEP");
                }
                EdgeboxRuleService.getInstance().update(edgeboxRule);
            }
        }

    }

    /**
     * This method populates the topic APP2 for Services
     */
    public void populateTopicForService(){
        List<Edgebox> lstEdgeBox = EdgeboxService.getInstance().getByType(Constants.EDGEBOX_CORE_TYPE);
        String topics = "topics";
        String mqtt = "mqtt";
        if( (lstEdgeBox!=null) && (!lstEdgeBox.isEmpty()) ){
            try{
                for(Edgebox edgebox:lstEdgeBox) {
                    if ((edgebox.getConfiguration() != null) && (!edgebox.getConfiguration().isEmpty())) {
                        JSONObject outputConfig = (JSONObject) new JSONParser().parse(edgebox.getConfiguration());
                        if ((outputConfig != null) && (outputConfig.containsKey(mqtt)) ) {
                            JSONObject mqttJson = ((JSONObject) outputConfig.get(mqtt));
                            if( mqttJson.containsKey(topics) ) {
                                JSONArray topicsArray = (JSONArray) mqttJson.get(topics);
                                if ((topics != null) && (!topics.isEmpty())) {
                                    int count = 0;
                                    for (Object topic : topicsArray) {
                                        if (topic.equals(Constants.APP2_MQTT_TOPIC)) {
                                            break;
                                        }
                                        count++;
                                    }
                                    if (count < topicsArray.size()) {
                                        topicsArray.add(Constants.APP2_MQTT_TOPIC);
                                        mqttJson.put(topics, topicsArray);
                                        outputConfig.put(mqtt, mqttJson);
                                        edgebox.setConfiguration(outputConfig.toJSONString());
                                        EdgeboxService.getInstance().update(edgebox);
                                    }
                                }
                            }  else {
                                JSONArray topicsArray = new JSONArray();
                                topicsArray.add(Constants.APP2_MQTT_TOPIC);
                                mqttJson.put(topics, topicsArray);
                                outputConfig.put(mqtt, mqttJson);
                                edgebox.setConfiguration(outputConfig.toJSONString());
                                EdgeboxService.getInstance().update(edgebox);
                            }
                        }
                    }
                }

            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }
}
