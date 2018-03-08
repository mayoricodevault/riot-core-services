package com.tierconnect.riot.iot.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.services.*;
import com.tierconnect.riot.appcore.utils.Utilities;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.iot.dao.mongo.ThingMongoDAO;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.utils.JsonFlattenerUtil;
import com.tierconnect.riot.iot.utils.rest.RestClient;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import com.tierconnect.riot.sdk.dao.Pagination;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.jose4j.json.internal.json_simple.JSONArray;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.*;
import org.apache.commons.lang.StringUtils;

/**
 * Created by fflores on 2/10/16.
 */
public class DevicesService {
    private static Logger logger = Logger.getLogger(DevicesService.class);
    public final static String HOST = "api.macvendors.com";
    public final static String MAC_VALIDATION_RESPONSE = "MOJIX INC.";

    public final static String STARFLEX_TEMPLATE = "STARflex";
    public final static String STARFLEX_TAG_TEMPLATE = "FlexTag";

    public final static String STARFLEX_THINGTYPECODE = "STR_400";
//    public final static String STARFLEX_THINGTYPENAME = "SF_";
//    public final static String STARFLEX_TAG_THINGTYPENAME = "SFT_";
//
    public final static String STARFLEX_BRIDGES_NAME = "Starflex_";
    public final static String STARFLEX_TAG_BRIDGES_NAME = "StarflexTag_";
    public final static String ALEBRIDGES_NAME = "Alebridge_";
    public final static String COREBRIDGES_NAME = "Corebridge_";
//
    public final static String STARFLEX_BRIDGES_CODE = "STARD_";
    public final static String STARFLEX_TAG_BRIDGES_CODE = "START_";
    public final static String ALEBRIDGES_CODE = "ALEB_";
    public final static String COREBRIDGES_CODE = "CORE_";

    /*public final static String BRIDGE_TYPE_STARFLEX = "StarFLEX";
    public final static String MESSAGE_MODE_STARFLEX = "StarFlex";
    public final static String MESSAGE_MODE_FLEXTAG = "FlexTag";*/

//    public final static String MONGO_CONN = "MONGO";
//    public final static String MQTT_CONN = "MQTT";

    public final static String LEVEL = "2";

    //Table MySQL: apc_field and the relation with group in groupfield
    public enum GroupFieldName {
        THING("thing"),
        THING_TYPE("thingType"),
        GROUP_TYPE("groupType"),
        ZONE("zone"),
        LOCAL_MAP("localMap"),
        REPORT("report"),
        LOGICAL_READER("logicalReader"),
        SHIFTS("shift"),
        ZONE_TYPE("zoneType"),
        EDGEBOX("edgebox"),
        ZONE_GROUP("zoneGroup");

        public String value;

        GroupFieldName(String value) {
            this.value = value;
        }
    }

    //Table MySQL: resource
    public enum GroupResources {
        thing("thing"),
        thing_editOwn("thing_editOwn"),
        Thing_Types("Thing Types"),
        groupType("groupType"),
        group("group"),
        reportDefinition("reportDefinition"),
        reportDefinition_editOwn("reportDefinition_editOwn"),
        reportDefinition_emailRecipients("reportDefinition_emailRecipients"),
        reportDefinition_assignUnAssignThing("reportDefinition_assignUnAssignThing"),
        reportDefinition_inlineEdit("reportDefinition_inlineEdit"),
        reportDefinition_inlineEditGroup("reportDefinition_inlineEditGroup"),
        zone("zone"),
        zoneGroup("zoneGroup"),
        zonePoint("zonePoint"),
        zoneType("zoneType"),
        localMap("localMap"),
        logicalReader("logicalReader"),
        _thingtype_("_thingtype_"),
        connection("connection"),
        connectionType("connectionType"),
        thingType("thingType"),
        edgebox("edgebox"),
        edgeboxRule("edgeboxRule"),
        parameters("parameters"),
        dataType("dataType"),
        role("role"),
        resource("resource"),
        user("user"),
        user_editRoamingGroup("user_editRoamingGroup"),
        license("license"),
        // new resource
        Analytics("Analytics"),
        Gateway("Gateway"),
        Model("Model"),
        Tenants("Tenants"),
        Things("Things"),
        mapMaker("mapMaker"),
        services("Services");
        public String value;

        GroupResources(String value) {
            this.value = value;
        }
    }

    static private DevicesService INSTANCE = new DevicesService();

    public static DevicesService getInstance() {
        return INSTANCE;

    }

    /***************************************************************
     * Initialize values for Tenant Group Map
     **************************************************************/
    public Map<String, Object>
    initializeValuesInTenant(
            Map<String, Object> map, Group parent, String groupTypeName) {
        try {
            if (map.get("id") != null) {
                map.put("code", map.get("code").toString());
            } else {
                String groupName = map.get("name").toString();
                String groupCode = groupName.trim().replace(" ", "_");
                map.put("name", groupName);
                map.put("code", groupCode);
                map.put("description", groupName);
            }
            //Get group Type ID
            GroupType groupType = null;
            BooleanBuilder be = new BooleanBuilder();
            //be = be.and(QGroupType.groupType.name.equalsIgnoreCase(groupTypeName));
            be = be.and(QGroupType.groupType.parent.id.eq(parent.getGroupType().getId()));
            List<GroupType> lstGroup = GroupTypeService.getInstance().listPaginated(be, null, null);
            if ((lstGroup != null) && (!lstGroup.isEmpty())) {
                groupType = lstGroup.get(0);
            }
            if (groupType == null) {
                throw new UserException("Group Type '" + groupTypeName + "' does not exist.");
            }
            map.put("groupType.id", groupType.getId());
            map.put("parent.id", parent.getId());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new UserException(e.getMessage(), e);
        }
        return map;
    }

    /***************************************************************
     * Initialize values to create a Rol
     **************************************************************/
    public Map<String, Object> initializeRolValues(String name, Group group) {
        Map<String, Object> response = new HashMap<>();
        response.put("group.id", group.getId());
        response.put("name", name);
        response.put("description", "rol" + group.getName());
        return response;
    }

    /***************************************************************
     * Initialize values for permissions
     **************************************************************/
    public Map<String, Object> initializePermissionValues(Long thingTypeId, User rootUser) throws NonUniqueResultException {
        Map<String, Object> response = new HashMap<>();

        List<String> lstPermission = new ArrayList<>();
        lstPermission.add(GroupResources.thing.value);
        lstPermission.add(GroupResources.thing_editOwn.value);
        lstPermission.add(GroupResources.Thing_Types.value);
        lstPermission.add(GroupResources.group.value);
        lstPermission.add(GroupResources.groupType.value);
        lstPermission.add(GroupResources.reportDefinition.value);
        lstPermission.add(GroupResources.reportDefinition_editOwn.value);
        lstPermission.add(GroupResources.reportDefinition_emailRecipients.value);
        lstPermission.add(GroupResources.reportDefinition_assignUnAssignThing.value);
        lstPermission.add(GroupResources.reportDefinition_inlineEdit.value);
        lstPermission.add(GroupResources.reportDefinition_inlineEditGroup.value);
        lstPermission.add(GroupResources.zone.value);
        lstPermission.add(GroupResources.zoneGroup.value);
        lstPermission.add(GroupResources.zonePoint.value);
        lstPermission.add(GroupResources.zoneType.value);
        lstPermission.add(GroupResources.localMap.value);
        lstPermission.add(GroupResources.logicalReader.value);
        lstPermission.add(GroupResources._thingtype_.value + thingTypeId);
        lstPermission.add(GroupResources.connection.value);
        lstPermission.add(GroupResources.connectionType.value);
        lstPermission.add(GroupResources.thingType.value);
        lstPermission.add(GroupResources.edgebox.value);
        lstPermission.add(GroupResources.edgeboxRule.value);
        lstPermission.add(GroupResources.parameters.value);
        lstPermission.add(GroupResources.dataType.value);
        lstPermission.add(GroupResources.role.value);
        lstPermission.add(GroupResources.resource.value);
        lstPermission.add(GroupResources.user.value);
        lstPermission.add(GroupResources.user_editRoamingGroup.value);
        lstPermission.add(GroupResources.license.value);
        lstPermission.add(GroupResources.Analytics.value);
        lstPermission.add(GroupResources.Gateway.value);
        lstPermission.add(GroupResources.Model.value);
        lstPermission.add(GroupResources.Tenants.value);
        lstPermission.add(GroupResources.Things.value);
        lstPermission.add(GroupResources.Things.value);
        lstPermission.add(GroupResources.services.value);
        lstPermission.add(GroupResources.mapMaker.value);

        for (String resources : lstPermission) {
            Resource resource = ResourceService.getInstance().getByNameAndGroup(resources, rootUser.getGroup());
            if (resource != null) {
                response.put(resource.getId().toString(), resource.getAcceptedAttributes());
            } else {
                throw new UserException("Is not possible to get the resource.");
            }
        }
        return response;
    }

    /**
     * Method to create or update a device as a thing
     * @param recursivelyStackConfig
     * @param recursivelyStackStatus
     * @param mac
     * @param thingTypeCodeConfig
     * @param thingTypeCodeStatus
     * @param deviceMap
     * @param currentUser
     * @return
     */
    public Map<String, Object> createDevice (
            Stack<Long> recursivelyStackConfig,
            Stack<Long> recursivelyStackStatus,
            String mac,
            String thingTypeCodeConfig,
            String thingTypeCodeStatus,
            Map<String, Object> deviceMap, User currentUser) {
        Map<String, Object> response = new HashMap<>();
//        String output = "";
        // 1. parse input
        if (mac == null || mac.isEmpty() || thingTypeCodeConfig == null || thingTypeCodeConfig.isEmpty()) {
            throw new UserException("Invalid input parameters");
        }
        Map<String, Object> deviceMapAsUDFs = getUdfValues(deviceMap,mac);
        // 3. create or update
        Map<String, Object> resultConfig = getConfigThingType(thingTypeCodeConfig,
                                                              deviceMapAsUDFs,
                                                              mac,
                                                              recursivelyStackConfig,
                                                              recursivelyStackStatus,
                                                              currentUser);
        // 4. Send response
        Long thingId = (Long) ((Map) resultConfig.get("thing")).get("id");
        String whereThing = "_id=" + thingId;
        List<String> filterFields = new ArrayList<>();
        filterFields.add("mqtt_host");
        filterFields.add("mqtt_port");
        filterFields.add("mqtt_topic_interesting_events");
        filterFields.add("mqtt_topic_data");
        filterFields.add("mqtt_topic_req");
        filterFields.add("mqtt_topic_res");
        Map<String, Object> thingUdfVal = ThingMongoDAO.getInstance().getThingUdfValues(whereThing, null, filterFields, null);
        if (null != thingUdfVal && !thingUdfVal.isEmpty()) {
            List<Map<String, Object>> udfValueList = (ArrayList) thingUdfVal.get("results");
            if (null == udfValueList || udfValueList.isEmpty() || udfValueList.size() > 1) {
                throw new UserException("No results for device " + mac);
            }
            for (Map.Entry<String, Object> entry : udfValueList.get(0).entrySet()) {
                if (!entry.getKey().equals("treeLevel")) {
                    if (entry.getValue() != null && entry.getValue() instanceof Map) {
                        response.put(entry.getKey(), ((Map) entry.getValue()).get("value"));
                    } else {
                        response.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
        return response;
    }

    public Map<String, Object> getConfigThingType(String thingTypeCodeConfig,
                                        Map<String, Object> deviceMapAsUDFs,
                                        String mac,
                                        Stack<Long> recursivelyStackConfig,
                                        Stack<Long> recursivelyStackStatus,
                                        User currentUser){
        Map<String, Object> resultConfig = new HashMap<>();
        Map<String, Object> resultStatus = new HashMap<>();
        try {
            ThingType thingTypeConfig = ThingTypeService.getInstance().getByCode(thingTypeCodeConfig.trim());
            if(thingTypeConfig == null){
                throw new UserException("Invalid Thing Type Code");
            }else{
                if (isValidThingTypeGroup(thingTypeConfig)) {
                    if (!thingTypeConfig.getThingTypeTemplate().getCode().equalsIgnoreCase(Constants.TT_STARflex_CONFIG_NAME)){
                        throw new UserException("Thing Type code does not belong to the STARflex Config Thing Type template.");
                    }
                    resultConfig = this.createThingStarflex(deviceMapAsUDFs,thingTypeConfig, mac, currentUser, recursivelyStackConfig);

                    ThingType thingTypeStatus = getStatusThingTypeCode(thingTypeConfig, Constants.TT_STARflex_STATUS_NAME);
                    if (thingTypeStatus == null){
                        throw new UserException("STARflex Status Thing Type Code does not exist in Tenant Group " + thingTypeConfig.getGroup().getName() );
                    } else {
                        if (!thingTypeStatus.getThingTypeTemplate().getCode().equalsIgnoreCase(Constants.TT_STARflex_STATUS_NAME)){
                            throw new UserException("STARflex Status Thing Type Code does not exist in Tenant Group " + thingTypeConfig.getGroup().getName() );
                        }
                        resultStatus = this.createThingStarflex(deviceMapAsUDFs,thingTypeStatus, mac, currentUser, recursivelyStackStatus);
                    }
                } else {
                    throw new UserException("Thing Type does not belong to its corresponding Tenant Group.");
                }
            }
        } catch (NonUniqueResultException e) {
            throw new UserException("Error registering the device.", e);
        }

        return resultConfig;
    }

    /**
     * The mothod validates that the group of the ThingType is the correct one. Belongs to root its from Company type
     * and is SF type.
     * @param thingType the thingType to be validated.
     * @return true if it passes all the validations.
     */
    public boolean isValidThingTypeGroup(ThingType thingType){
        if (!thingType.getGroup().getCode().equalsIgnoreCase(Constants.STARFLEX_MAIN_GROUP_CODE)){
            throw new UserException("Thing Type does not belong to its corresponding Tenant Group.");
        }

        if (thingType.getGroup().getParent().getId().longValue() != GroupService.getInstance().getRootGroup().getId().longValue()){
            throw new UserException("Group should be child of Root Group.");
        }
        if (!Constants.TENANT_COPANY.equalsIgnoreCase(thingType.getGroup().getGroupType().getCode())){
            throw new UserException("The level of the Group should be 'Company'.");
        }

        return true;
    }

    /**
     * The methods gets the ThingType of an STARflex Status thingType from a specific ThingTpe group.
     * @param thingtype the thingType where we are going to get the group id.
     * @return a ThingType from specific group and STARflex Status ThingType.
     */
    public ThingType getStatusThingTypeCode(ThingType thingtype, String thingTypeTemplateName){
        ThingType thingTypeStatus = null;
        BooleanBuilder bb = new BooleanBuilder();
        bb.and(QThingType.thingType.thingTypeTemplate.code.eq(thingTypeTemplateName)
                .and(QThingType.thingType.group.eq(thingtype.getGroup())));
        List<ThingType> lst = ThingTypeService.getInstance().listPaginated(bb,null,null);
        if(lst!=null && !lst.isEmpty()){
            thingTypeStatus = lst.get(0);
        }
        return thingTypeStatus;
    }

    /**
     * Method to convert initial input map to UDFs standard map
     *
     * @param deviceMap
     * @return
     */
    private static Map<String, Object> getUdfValues(Map<String, Object> deviceMap, String mac) {
        Map<String, Object> result = new HashMap<>();
        // replace macId instead of id
        if (deviceMap.get("id") != null) {
            String id = (String) deviceMap.get("id");
            if (!id.trim().equals(mac.trim())) {
                throw new UserException("Invalid ID");
            }
            deviceMap.put("macId", id);
            deviceMap.remove("id");
        }
        // add discoveryDate
        Date current = new Date();
        Timestamp ts = new Timestamp(current.getTime());
        deviceMap.put("discoveryDate", ts);

        String json = "";
        try {
            json = new ObjectMapper().writeValueAsString(deviceMap);
        } catch (Exception e) {
            e.printStackTrace();
            throw new UserException("Input invalid", e);
        }
        Map<String, Object> flattenJson = JsonFlattenerUtil.flattenAsMap(json, "--", "__");
        if (flattenJson != null) {
            logger.debug("flattenJson" + flattenJson.toString());
            for (Map.Entry<String, Object> entry : flattenJson.entrySet()) {
                Map<String, Object> valueMap = new HashMap<>();
                valueMap.put("value", entry.getValue());
                if(entry.getKey().equals("loadAvg__0__") || entry.getKey().equals("loadAvg__1__") ||
                        entry.getKey().equals("loadAvg__2__")) {
                    result.put(entry.getKey().replace("__", "_"), valueMap);
                } else {
                    result.put(entry.getKey(), valueMap);
                }
            }
        }
        return result;
    }

    public String getURI(String host, String macId) {
        return "http://" + host + "/" + macId;
    }

    private Map<String, Object> cleanUdfMap(Map<String, Object> udfMap, String thingTypeCode) {
        Map<String, Object> result = new HashMap<>();
        List<ThingTypeField> thingTypeFields = ThingTypeFieldService.getInstance().getByThingTypeCode(thingTypeCode);
        if (thingTypeFields != null) {
            for (ThingTypeField thingTypeField : thingTypeFields) {
                if (udfMap.containsKey(thingTypeField.getName())) {
                    result.put(thingTypeField.getName(), udfMap.get(thingTypeField.getName()));
                }
            }
        }
        return result;
    }

    /**********************************************
     * Method to get Udf's so as to send to Thing Service
     **********************************************/
    public Map<String, Object> createNewTenantProcess(
            Stack<Long> recursivelyStack,
            Map<String, Object> map,
            User rootUser) throws Exception {

        logger.info("Associating Device with the group " + (map != null ? ": " + map.toString() : ""));
        Map<String, Object> result = new HashMap<>();
        ValidationBean validationBean = this.validaCreateNewTenant(map);
        if (validationBean.isError()) {
            throw new UserException(validationBean.getErrorDescription());
        }
        Map<String, Object> tenantMap = (Map<String, Object>) map.get("group");
        Map<String, Object> rolMap = null;
        Map<String, Object> userMap = (Map<String, Object>) map.get("user");
        List<Map<String, Object>> serials = (List<Map<String, Object>>) map.get("serials");

        Group group = null;
        Role role = null;
        User user = null;
        ThingType thingTypeStarflexConfig = null;
        ThingType thingTypeStarflexStatus = null;
        ThingType thingTypeStarflexTag = null;

        DevicesService devicesService = DevicesService.getInstance();

        if (tenantMap.get("id") != null) {
            //Group exists
            group = GroupService.getInstance().get(Long.parseLong(tenantMap.get("id").toString()));

            //Changing level in config of the Group for Reports
            Map<String, Object> mapConfigGroup = this.getMapGroupConfig();
            GroupService.getInstance().setGroupFieldsBase(group.getId(), mapConfigGroup);

            //*** get a better way to obtain the thing type code for startflex of the group
            String nameThingTypeStarflexConfig = Constants.THING_STARflex_CONFIG_ACRONYM +group.getCode();
            String nameThingTypeStarflexStatus = Constants.THING_STARflex_STATUS_ACRONYM +group.getCode();
            String nameThingTypeStarflexTag = Constants.THING_STARflex_ACRONYM +group.getCode();

            thingTypeStarflexConfig = ThingTypeService.getInstance().getByCode(nameThingTypeStarflexConfig+"_code");
            thingTypeStarflexStatus = ThingTypeService.getInstance().getByCode(nameThingTypeStarflexStatus+"_code");
            thingTypeStarflexTag = ThingTypeService.getInstance().getByCode(nameThingTypeStarflexTag+"_code");

            if( (thingTypeStarflexConfig == null) || (thingTypeStarflexStatus == null) || (thingTypeStarflexTag == null) )
            {
                //Create Thing Type with the group and Create Permission to Role
                if(thingTypeStarflexConfig == null){
                    thingTypeStarflexConfig = createCompleteThingTypeByTemplate(
                            group, nameThingTypeStarflexConfig, Constants.TT_STARflex_CONFIG_NAME, false);
                }
                if(thingTypeStarflexStatus == null){
                    thingTypeStarflexStatus = createCompleteThingTypeByTemplate(
                            group, nameThingTypeStarflexStatus, Constants.TT_STARflex_STATUS_NAME, false);
                }
                if(thingTypeStarflexTag == null){
                    thingTypeStarflexTag = createCompleteThingTypeByTemplate(
                            group, nameThingTypeStarflexTag, Constants.TT_STARflex_NAME, false);
                }
                //Send tickle, inform Bridges that thing types were created
                BrokerClientHelper.sendRefreshThingTypeMessage(false, GroupService.getInstance().getMqttGroups(group));
            }
        } else {
            //Create Groups: Company, Store and Department
            tenantMap = devicesService.initializeValuesInTenant(tenantMap, GroupService.getInstance().getRootGroup(), "Company");
            group = GroupService.getInstance().createGroup(tenantMap);

            Map<String, Object> tenantStoreMap = new HashMap<>();
            tenantStoreMap.put("name", group.getName() + "_Store");
            tenantStoreMap = devicesService.initializeValuesInTenant(tenantStoreMap, group, "Store");
            Group groupStore = GroupService.getInstance().createGroup(tenantStoreMap);

            Map<String, Object> tenantDeptMap = new HashMap<>();
            tenantDeptMap.put("name", group.getName() + "_Department");
            tenantDeptMap = devicesService.initializeValuesInTenant(tenantDeptMap, groupStore, "Department");
            Group groupDept = GroupService.getInstance().createGroup(tenantDeptMap);

            //Changing level in config of the Group for Reports
            Map<String, Object> mapConfigGroup = getMapGroupConfig();
            GroupService.getInstance().setGroupFieldsBase(group.getId(), mapConfigGroup);

            //Create Thing Type with the new group
            String nameThingTypeStarflexConfig = Constants.THING_STARflex_CONFIG_ACRONYM +group.getCode();
            String nameThingTypeStarflexStatus = Constants.THING_STARflex_STATUS_ACRONYM +group.getCode();
            String nameThingTypeStarflexTag = Constants.THING_STARflex_ACRONYM +group.getCode();

            thingTypeStarflexConfig = createCompleteThingTypeByTemplate(group, nameThingTypeStarflexConfig, Constants.TT_STARflex_CONFIG_NAME, false);
            thingTypeStarflexStatus = createCompleteThingTypeByTemplate(group, nameThingTypeStarflexStatus, Constants.TT_STARflex_STATUS_NAME, false);
            thingTypeStarflexTag = createCompleteThingTypeByTemplate(group, nameThingTypeStarflexTag, Constants.TT_STARflex_NAME, true);
        }

        //Create role
        role = this.createUpdateRole(thingTypeStarflexConfig, thingTypeStarflexStatus,thingTypeStarflexTag,group, rootUser);

        //Create User or update User
        user = this.createUpdateUser(userMap, group, role, rootUser);

        //Create Connections
        String mongoConnection = Constants.STARFLEX_MONGO_CONN_ACRONYM + group.getCode();
        String mqttConnection = Constants.STARFLEX_MQTT_CONN_ACRONYM + group.getCode();
        this.createConnections(group,Constants.STARFLEX_MONGO_CONN_NAME, mongoConnection);
        this.createConnections(group,Constants.STARFLEX_MQTT_CONN_NAME, mqttConnection );
        //Create Bridges
        this.createBridges(thingTypeStarflexConfig, thingTypeStarflexStatus, thingTypeStarflexTag, group,
                mongoConnection,mqttConnection);
        //Create Reports
        if(!isStarFLEXreportsExists(group)) {
            createStarFLEXReport(user, group, thingTypeStarflexConfig, thingTypeStarflexStatus, thingTypeStarflexTag);
        }

        //Migrate ThingsPopdb
        result = this.migrateThingsToAnotherGroup(
                recursivelyStack, serials, thingTypeStarflexConfig, thingTypeStarflexStatus, group, rootUser);
        result.put("thingTypeStarflexConfig", thingTypeStarflexConfig.publicMap());
        result.put("thingTypeStarflexStatus", thingTypeStarflexStatus.publicMap());
        result.put("thingTypeStarflexTag", thingTypeStarflexTag.publicMap());
        result.put("response", "OK");


        return result;
    }

    /**
     * Create or Update Role
     * @param thingTypeStarflexConfig
     * @param thingTypeStarflexStatus
     * @param thingTypeStarflexTag
     * @param group
     * @param rootUser
     * @return
     */
    public Role createUpdateRole(
            ThingType thingTypeStarflexConfig,
            ThingType thingTypeStarflexStatus,
            ThingType thingTypeStarflexTag,
            Group group,User rootUser )
    {
        Role role = null;
        String roleName = "rol" + group.getName();
        List<Role> lstRole = RoleService.getInstance().getByName(roleName, group);
        Map<String, Object> rolMap = null;

        if (lstRole != null && lstRole.size() > 0) {
            role = lstRole.get(0);
        } else {
            if( (thingTypeStarflexConfig != null) && (thingTypeStarflexStatus!=null) && (thingTypeStarflexTag != null) ) {
                //Create Role
                rolMap = DevicesService.getInstance().initializeRolValues(roleName,group);
                role = RoleService.getInstance().createRol(rolMap);
                //Create Permission to Role
                try {
                    rolMap = DevicesService.getInstance().initializePermissionValues(thingTypeStarflexConfig.getId(), rootUser);
                    List list = RoleService.getInstance().setResources(role.getId(), rolMap);
                    rolMap = DevicesService.getInstance().initializePermissionValues(thingTypeStarflexStatus.getId(), rootUser);
                    list = RoleService.getInstance().setResources(role.getId(), rolMap);
                    rolMap = DevicesService.getInstance().initializePermissionValues(thingTypeStarflexTag.getId(), rootUser);
                    list = RoleService.getInstance().setResources(role.getId(), rolMap);
                } catch(Exception e) {
                    throw new UserException("Error getting the roles for the group.", e);
                }
            }
        }
        return role;
    }

    /**
     * Create or Update a User
     *
     * @param userMap
     * @param group
     * @param role
     * @return
     */
    public User createUpdateUser(Map<String, Object> userMap, Group group, Role role, User rootUser) {
        User user = null;
        userMap.put("group.id", group.getId());
        if (userMap.get("id") != null) {   //Update User
            user = UserService.getInstance().get(Long.parseLong(userMap.get("id").toString()));
            user.setLastName(userMap.get("lastName") != null ? userMap.get("lastName").toString() : null);
            user.setFirstName(userMap.get("firstName") != null ? userMap.get("firstName").toString() : null);
            user.setUsername(userMap.get("username") != null ? userMap.get("username").toString() : null);
            user.setPassword(userMap.get("password") != null ? userMap.get("password").toString() : null);
            user.setEmail(userMap.get("email") != null ? userMap.get("email").toString() : null);
            // replace charater
            user.setUsername(Utilities.replaceCharacterEscape(user.getUsername()));
            if (!Utilities.isAlphaNumericCharacterSpecials(user.getUsername(), "@_\\-\\\\.")) {
                throw new UserException("Username has invalid characters, only alphanumeric and character [. _ - \\ @] are allowed.");
            }
            user = UserService.getInstance().update(user);
        } else {
            //Create User
            userMap.put("group.id", group.getId());
            try {
                user = UserService.getInstance().createUser(userMap);
            } catch (Exception e) {
                throw new UserException(e.getMessage() + ". Please, choose another Username.", e);
            }
        }
        //Assign Permission to User and RootUser
        if (!UserRoleService.getInstance().isUserWithRoles(user, role)) {
            UserService.getInstance().addUserRole(user.getId(), role.getId(), user, role);
        }
        if (!UserRoleService.getInstance().isUserWithRoles(rootUser, role)) {
            UserService.getInstance().addUserRole(rootUser.getId()
                    , role.getId()
                    , rootUser
                    , role);
        }
        return user;
    }

    /**
     * Create connections
     *
     * @param group
     * @param typeConnection
     * @param nameConnection
     */
    public void createConnections(Group group, String typeConnection, String nameConnection) {
        Connection conn = ConnectionService.getInstance().getByCodeAndGroup(nameConnection, group);
        if (conn == null) {
            Connection originConn = ConnectionService.getInstance().getByCodeAndGroup(typeConnection, GroupService.getInstance().getRootGroup());
            if (originConn != null) {
                createConnection(originConn, group, nameConnection);
            } else {
                BooleanBuilder b = new BooleanBuilder();
                b.and(QConnection.connection.connectionType.code.eq(typeConnection))
                        .and(QConnection.connection.group.eq(GroupService.getInstance().getRootGroup()));
                List<Connection> lstConn = ConnectionService.getInstance().listPaginated(b, null, null);
                if ((lstConn != null) && (!lstConn.isEmpty())) {
                    originConn = lstConn.get(0);
                    createConnection(originConn, group, nameConnection);
                }
            }
        }
    }

    /**
     * Create Connection based on specific connection, group and name
     *
     * @param originConn
     * @param group
     * @param nameConnection
     */
    public Connection createConnection(Connection originConn, Group group, String nameConnection) {
        Connection connection = originConn.duplicateConnetion();
        connection.setGroup(group);
        connection.setCode(nameConnection);
        connection.setName(nameConnection);
        return ConnectionService.getInstance().insert(connection);
    }

    /**
     * Create Bridges
     *
     * @param thingTypeStarflexConfig    Thing Type Config
     * @param thingTypeStarflexStatus Thing Type Status
     * @param thingTypeStarflexTag Thing Type Tag
     * @param group                Group
     * @param mongoConnection      Mongo Connection
     * @param mqttConnection       Mosquitto Connection
     */
    public void createBridges(
            ThingType thingTypeStarflexConfig,
            ThingType thingTypeStarflexStatus,
            ThingType thingTypeStarflexTag,
            Group group,
            String mongoConnection,
            String mqttConnection) {
        try {
            // Bridges
            String aleBridgeCode = ALEBRIDGES_CODE + group.getCode();
            String coreBridgeCode = COREBRIDGES_CODE + group.getCode();
            String starflexBridge = STARFLEX_BRIDGES_CODE + group.getCode();
            String starflexTagBridge = STARFLEX_TAG_BRIDGES_CODE + group.getCode();
            // edgebox
            Edgebox aleBridgeEdgebox = EdgeboxService.getInstance().selectByCode(aleBridgeCode);
            Edgebox coreBridgeEdgebox = EdgeboxService.getInstance().selectByCode(coreBridgeCode);
            String starflexBridgeName    = Constants.STARFLEX_TAG_BRIDGES_CODE_ACRONYM +group.getCode();
            List<Long> groupMqtt = GroupService.getInstance().getMqttGroups(group);

            if(!EdgeboxService.getInstance().existBridgeCode(starflexBridgeName,group.getCode(), null))
            {
                String config = getStarFlexBridgeConfiguration(
                        thingTypeStarflexConfig.getCode()
                        , thingTypeStarflexStatus.getCode()
                        , thingTypeStarflexTag.getCode()
                        , mongoConnection
                        , mqttConnection);
                EdgeboxService.getInstance().insertEdgebox(
                        group
                        , Constants.STARFLEX_TAG_BRIDGES_NAME_ACRONYM + group.getCode()
                        , Constants.STARFLEX_TAG_BRIDGES_CODE_ACRONYM + group.getCode()
                        , Constants.EDGEBOX_STARFLEX_TYPE
                        , config
                        , 0L); //port

                //Initialize Bridge
                BrokerClientHelper.sendInitDeviceBridge(
                        Constants.STARFLEX_TAG_BRIDGES_CODE_ACRONYM + group.getCode(), group.getCode(), false, groupMqtt);
            }

            if (aleBridgeEdgebox == null) {
                aleBridgeEdgebox = createEdgebox(
                        group,                                                                       // group
                        ALEBRIDGES_NAME + group.getCode(),                                           // name
                        aleBridgeCode,                                                               // code
                        ParametersService.CODE_EDGE,                                                 // type
                        getAleBridgeConfiguration(mqttConnection, thingTypeStarflexTag.getCode()),   // config
                        0L);                                                                         // port
                // Initialize Bridge
                BrokerClientHelper.sendInitDeviceBridge(aleBridgeEdgebox.getCode(), group.getCode(), false, groupMqtt);
            }

            List<String> topics = new ArrayList<>(4);
            topics.add("/v1/data/APP2/#");                                      // topic UI
            topics.add("/v1/data/" + aleBridgeEdgebox.getCode() + "/#");        // topic ALEBRIDGE
//            topics.add("/v1/data/" + starFlexDeviceEdgebox.getCode() + "/#");   // topic STARFLEX DEVICE
//            topics.add("/v1/data/" + starFlexTagEdgebox.getCode() + "/#");      // topic STARFLEX TAG
            topics.add("/v1/data/" + starflexBridgeName + "/#");      // topic STARFLEX TAG
            if (coreBridgeEdgebox == null) {
                coreBridgeEdgebox = createEdgebox(
                        group,
                        COREBRIDGES_NAME + group.getCode(),
                        coreBridgeCode,
                        ParametersService.CODE_CORE,
                        getCoreBridgeConfiguration(mqttConnection, mongoConnection, topics),
                        0L);
                //Initialize Bridge
                BrokerClientHelper.sendInitDeviceBridge(coreBridgeEdgebox.getCode(), group.getCode(), false, groupMqtt);
            } else {
                updateTopicsCoreBridge(coreBridgeEdgebox, topics, new ArrayList<>());
            }
            // update corebridge
            Edgebox coreBridgeMCB = EdgeboxService.getInstance().selectByCode("MCB");
            updateAllTopicsCoreBridge(coreBridgeMCB, Collections.singletonList("/v1/data/#"));
        } catch (Exception e) {
            logger.error("Error in the creation of Bridges with group[" + group.getCode() + "]", e);
            throw new UserException("Is not possible create Bridges", e);
        }
    }



    /**
     * update all topics
     *
     * @param coreBridgeEdgebox
     * @param topics
     * @throws ParseException
     */
    public void updateAllTopicsCoreBridge(Edgebox coreBridgeEdgebox, List<String> topics) throws ParseException {
        JSONObject jsonParameterValue = (JSONObject) new JSONParser().parse(coreBridgeEdgebox.getConfiguration());
        JSONObject objectMQTT = (JSONObject) jsonParameterValue.get("mqtt");
        objectMQTT.put("topics", topics);
        jsonParameterValue.put("mqtt", objectMQTT);
        coreBridgeEdgebox.setConfiguration(jsonParameterValue.toJSONString());
        EdgeboxService.getInstance().update(coreBridgeEdgebox);
    }

    /**
     * update/merge topics in corebridges
     *
     * @param coreBridgeEdgebox edgebox
     * @param topics            topics
     * @throws ParseException when errors occur with {@link JSONParser}
     */
    @SuppressWarnings("unchecked")
    public void updateTopicsCoreBridge(Edgebox coreBridgeEdgebox, List<String> topics, List<String> exclude) throws ParseException {
        JSONObject jsonParameterValue = (JSONObject) new JSONParser().parse(coreBridgeEdgebox.getConfiguration());
        JSONObject objectMQTT = (JSONObject) jsonParameterValue.get("mqtt");
        if (objectMQTT.get("topics") != null && objectMQTT.get("topics") instanceof JSONArray) {
            JSONArray jsonArrayTopics = (JSONArray) objectMQTT.get("topics");
            jsonArrayTopics.stream().filter(topic -> !topics.contains(String.valueOf(topic))).forEach(topic -> {
                topics.add(String.valueOf(topic));
            });
            jsonArrayTopics.stream().filter(excluded -> exclude.contains(String.valueOf(excluded))).forEach(excluded -> {
                topics.remove(String.valueOf(excluded));
            });
        }
        objectMQTT.put("topics", topics);
        jsonParameterValue.put("mqtt", objectMQTT);
        coreBridgeEdgebox.setConfiguration(jsonParameterValue.toJSONString());
        EdgeboxService.getInstance().update(coreBridgeEdgebox);
    }

    /**
     * return configuration ALE bridge in JSON format
     *
     * @param mqttConnection Mosquitto code connection
     * @param thingTypeCode  thing type code
     * @return configuration
     * @throws ParseException when errors occur with {@link JSONParser}
     */
    public String getAleBridgeConfiguration(String mqttConnection, String thingTypeCode) throws ParseException {
        return getBridgesConfiguration(ParametersService.CODE_EDGE, mqttConnection, null, null, thingTypeCode);
    }

    /**
     * return configuration CORE bridge in JSON format
     *
     * @param mqttConnection  code connection MOSQUITTO
     * @param mongoConnection code connection MONGO
     * @param topics          list topics
     * @return configuration
     * @throws ParseException when errors occur with {@link JSONParser}
     */
    public String getCoreBridgeConfiguration(String mqttConnection, String mongoConnection, List<String> topics) throws ParseException {
        return getBridgesConfiguration(ParametersService.CODE_CORE, mqttConnection, mongoConnection, topics, null);
    }

    @SuppressWarnings("unchecked")
    private String getBridgesConfiguration(String codeParameter, String mqttConnection, String mongoConnection, List<String> topics, String thingTypeCode) throws ParseException {
        Parameters parameterBridge = ParametersService.getInstance().getByCategoryAndCode(ParametersService.CATEGORY_BRIDGE_TYPE, codeParameter);
        String result = null;
        if (parameterBridge != null && !Utilities.isEmptyOrNull(parameterBridge.getValue())) {
            //TODO This step is only until Bridges accept the current Parameters Table implementation
            //JSONObject jsonParameterValue = (JSONObject) new JSONParser().parse(parameterBridge.getValue());
            JSONObject jsonParameterValue = getConfigurationOfBridgeType(parameterBridge.getValue());

            if (ParametersService.CODE_EDGE.equals(codeParameter)) {
                jsonParameterValue.put("mqtt", Collections.<String, String>singletonMap("connectionCode", mqttConnection));
                jsonParameterValue.put("thingTypeCode", thingTypeCode);
            } else if (ParametersService.CODE_CORE.equals(codeParameter)) {
                Map<String, Object> mapMQTTConnection = new LinkedHashMap<>(2);
                mapMQTTConnection.put("connectionCode", mqttConnection);
                mapMQTTConnection.put("topics", topics);
                jsonParameterValue.put("mqtt", mapMQTTConnection);
                jsonParameterValue.put("mongo", Collections.<String, String>singletonMap("connectionCode", mongoConnection));
            }
            result = jsonParameterValue.toJSONString();
        }
        return result;
    }

    /**
     * Method populates configuration of starflex bridge
     *
     * @param starflexConfigCode starflexConfigCode
     * @param starflexStatusCode starflexStatusCode
     * @param starflexCode starflexCode
     * @param mqttCode mqttCode
     * @param mongoCode mongoCode
     * @return String with configuration of Starflex BRidges
     */
    public String getStarFlexBridgeConfiguration(
            String starflexConfigCode,
            String starflexStatusCode,
            String starflexCode,
            String mongoCode,
            String mqttCode) {
        Parameters param = ParametersService.getInstance().getByCategoryAndCode(Constants.BRIDGE_TYPE, Constants.EDGEBOX_STARFLEX_TYPE);
        //TODO This step is only until Bridges accept the current Parameters Table implementation
        JSONObject values = getConfigurationOfBridgeType(param.getValue());
        //Replace values
        ((JSONObject)values.get("mongo")).put("connectionCode", mongoCode);
        ((JSONObject)values.get("mqtt")).put("connectionCode", mqttCode);
        values.put("thingTypeCodeConfig",starflexConfigCode);
        values.put("thingTypeCodeStatus",starflexStatusCode);
        values.put("thingTypeCode", starflexCode);
        return values.toJSONString();
    }

    /**
     * Configuration of the Bridge Type
     * @param config String with configuration of Bridge Code from Parameters
     * @return JSON Object
     */
    public static JSONObject getConfigurationOfBridgeType(String config) {
        JSONObject result = new JSONObject();
        JSONObject configuration = new JSONObject();
        try {
            configuration = (JSONObject) (new JSONParser().parse(config));
        } catch(Exception e) {
            logger.error("It is not possible to parse value of Edge Bridge"+Constants.EDGEBOX_STARFLEX_TYPE+
                    " from Parameters Table",e);
            throw  new UserException("It is not possible to parse value of Edge Bridge"+Constants.EDGEBOX_STARFLEX_TYPE+
                    " from Parameters Table");
        }

        String[] sections = {"configuration", "filters", "extra"};
        for(String section : sections) {
            if(configuration.containsKey(section)) {
                Set<String> resultKeys = ((JSONObject)configuration.get(section)).keySet();
                for(String key : resultKeys) {
                    result.putAll(getValueFromJson(((JSONObject)configuration.get(section)), key));
                }
            }
        }
        return result;
    }

    /**
     * Get single value from JSON object
     * @param groupData JSON Object father
     * @param key Key to find
     * @return JSONObject with key and value
     */
    public static JSONObject getValueFromJson(JSONObject groupData, String key) {
        JSONObject result = new JSONObject();
        if( groupData.containsKey(key) && ((JSONObject)groupData.get(key)).containsKey("value")) {
            if( ((JSONObject)groupData.get(key)).get("value") instanceof Map ) {
                JSONObject value = (JSONObject) ((JSONObject)groupData.get(key)).get("value");
                Set<String> resultKeys = value.keySet();
                JSONObject subResults = new JSONObject();
                for(String subKey : resultKeys) {
                    subResults.putAll(getValueFromJson(value, subKey));
                }
                result.put(key,subResults);
            } else {
                result.put(key,((JSONObject)groupData.get(key)).get("value"));
            }
        }
        return result;
    }

    /**
     * Create edgebox with parameters
     *
     * @param group         Group
     * @param name          Name edgebox
     * @param code          Code edgebox
     * @param type          Type edgebox
     * @param configuration configuration in JSON format
     * @param port          port
     * @return {@link Edgebox}
     */
    public Edgebox createEdgebox(Group group, String name, String code, String type, String configuration, Long port) {
        Edgebox bridge = new Edgebox();
        bridge.setActive(Boolean.FALSE);
        bridge.setName(name);
        bridge.setCode(code);
        bridge.setGroup(group);
        bridge.setConfiguration(configuration);
        bridge.setParameterType(ParametersService.CATEGORY_BRIDGE_TYPE);
        bridge.setType(type);
        bridge.setPort(port);
        return EdgeboxService.getInstance().insert(bridge);
    }

    /**
     * This method returns the config for the group fields
     *
     * @return
     */
    public Map<String, Object> getMapGroupConfig() {
        Map<String, Object> mapConfigGroup = new HashMap<>();
        mapConfigGroup.put(GroupFieldName.THING.value, LEVEL);
        mapConfigGroup.put(GroupFieldName.THING_TYPE.value, LEVEL);
        mapConfigGroup.put(GroupFieldName.GROUP_TYPE.value, LEVEL);
        mapConfigGroup.put(GroupFieldName.REPORT.value, LEVEL);
        mapConfigGroup.put(GroupFieldName.LOGICAL_READER.value, LEVEL);

        mapConfigGroup.put(GroupFieldName.ZONE.value, LEVEL);
        mapConfigGroup.put(GroupFieldName.LOCAL_MAP.value, LEVEL);
        mapConfigGroup.put(GroupFieldName.SHIFTS.value, LEVEL);
        mapConfigGroup.put(GroupFieldName.ZONE_TYPE.value, LEVEL);
        mapConfigGroup.put(GroupFieldName.EDGEBOX.value, LEVEL);
        mapConfigGroup.put(GroupFieldName.ZONE_GROUP.value, LEVEL);
        return mapConfigGroup;
    }

    /**********************************************
     * Validation of creation a new tenant
     **********************************************/
    public ValidationBean validaCreateNewTenant(Map<String, Object> map) {
        ValidationBean valida = new ValidationBean();
        if (map == null) {
            valida.setErrorDescription("A JSON map is required");
            return valida;
        } else {
            if (map.get("group") == null) {
                valida.setErrorDescription("Company Name is required");
                return valida;
            } else {
                Map<String, Object> group = (Map<String, Object>) map.get("group");
                if (group.get("id") != null) {
                    Long idgroup = null;
                    try {
                        idgroup = Long.parseLong(group.get("id").toString());
                    } catch (Exception e) {
                        valida.setErrorDescription("Company Name ID has to be numeric.");
                        return valida;
                    }
                    if (GroupService.getInstance().get(idgroup) == null) {
                        valida.setErrorDescription("Company Name does not exist.");
                        return valida;
                    }
                } else {
                    if (group.get("name") == null) {
                        valida.setErrorDescription("Company Name 'group.name' value is required.");
                        return valida;
                    } else {
                        try {
                            Group newGroup = GroupService.getInstance().getByCode(group.get("name").toString().replace(" ", "_"));
                            if (newGroup != null) {
                                valida.setErrorDescription("Company Name already exists.");
                            }
                        } catch (NonUniqueResultException e) {
                            valida.setErrorDescription("Company Name already exists.");
                        }
                        if (!Utilities.isAlphaNumericCharacterSpecials(group.get("name").toString(), GroupService.CHARACTER_SPECIALS_GROUP_NAME)) {
                            throw new UserException("Company Name has invalid characters, only alphanumeric and character [_ & $ @] are allowed.");
                        }
                    }
                }
            }
            if (map.get("user") == null) {
                valida.setErrorDescription("User information is required.");
                return valida;
            } else {
                Map<String, Object> user = (Map<String, Object>) map.get("user");
                if (user.get("id") != null) {
                    Long idUser = null;
                    try {
                        idUser = Long.parseLong(user.get("id").toString());
                    } catch (Exception e) {
                        valida.setErrorDescription("User ID has to be numeric.");
                        return valida;
                    }
                    if (UserService.getInstance().get(idUser) == null) {
                        valida.setErrorDescription("User does not exist.");
                        return valida;
                    }
                } else {
                    if (user.get("username") == null || user.get("password") == null || user.get("firstName") == null
                            || user.get("lastName") == null) {
                        valida.setErrorDescription("'username', 'password', 'firstName' and 'lastName' " +
                                " are required values.");
                        return valida;
                    }
                }
            }
            if (map.get("serials") == null) {
                valida.setErrorDescription("STARflex ID Map is required");
                return valida;
            } else {
                //Validate if serials exist
                List<Map<String, Object>> serials = (List<Map<String, Object>>) map.get("serials");
                for (Object serial : serials) {
                    Map<String, Object> serialMap = (Map<String, Object>) serial;
                    try {
                        //Validate if serial Exist STR_400
                        Thing thingConfig = ThingService.getInstance().getBySerialAndThingTypeCode(
                                serialMap.get("serial").toString(),serialMap.get("thingTypeCode").toString());
                        if(thingConfig==null)
                        {
                            valida.setErrorDescription("STARflex ID with serial number '"+serialMap.get("serial").toString()+"' does not exist.");
                            return valida;
                        }
                        //Validate if serial Exist starflex_status
                        Map<String, Object> statusUnclaimedMap = getStarflexStatusUnclaimed(
                                serialMap.get("serial").toString(),
                                thingConfig.getGroup(),
                                Constants.TT_STARflex_STATUS_NAME);
                        Thing thingStatus = ThingService.getInstance().get(Long.parseLong(statusUnclaimedMap.get("_id").toString()));
                        if(thingStatus==null)
                        {
                            valida.setErrorDescription("STARflex Status ID with serial number '"+serialMap.get("serial").toString()+"' does not exist.");
                            return valida;
                        }
                    } catch (Exception e) {
                        valida.setErrorDescription("STARflex ID with serial number '" + serialMap.get("serial").toString() + "' does not exist.");
                        return valida;
                    }
                }
            }
        }
        return valida;
    }

    /**********************************************
     * Method to get Udf's so as to send to Thing Service
     **********************************************/
    public Map<String, Object> migrateThingsToAnotherGroup(
            Stack<Long> recursivelyStack
            , List<Map<String, Object>> serials
            , ThingType thingTypeStarflexConfig
            , ThingType thingTypeStarflexStatus
            , Group group
            , User user)
    {
        Map<String, Object> result = new HashMap<>();
        result.put("group", group.publicMap(true));
        if (serials != null) {
            for (Object serial : serials) {
                Map<String, Object> serialMap = (Map<String, Object>) serial;
                Map<String, Object> configUnclaimedMap = getMapThing(
                        serialMap.get("serial").toString(),
                        serialMap.get("thingTypeCode").toString());
                Thing thingConfigUnclaimed = ThingService.getInstance().get(Long.parseLong(configUnclaimedMap.get("_id").toString()));
                Map<String, Object> statusUnclaimedMap = getStarflexStatusUnclaimed(
                        serialMap.get("serial").toString(),
                        thingConfigUnclaimed.getGroup(),
                        Constants.TT_STARflex_STATUS_NAME);
                Thing thingStatusUnclaimed = ThingService.getInstance().get(Long.parseLong(statusUnclaimedMap.get("_id").toString()));
                if ( (thingConfigUnclaimed != null) && (thingStatusUnclaimed != null) ) {
                    try {
                        //If serial exist in another group it means that it  has been claimed by other company. It needs to be reactivated
                        Thing thingConfig = getThing(serialMap.get("serial").toString(),thingTypeStarflexConfig.getThingTypeCode() );
                        Thing thingStatus = getThing(serialMap.get("serial").toString(),thingTypeStarflexStatus.getThingTypeCode() );
                        if( (thingConfig != null) && (thingStatus != null)) {
                            Group groupThing = thingConfig.getGroup();
                            List<Long> groupMqtt = GroupService.getInstance().getMqttGroups(groupThing);
                            //Re-Initialize Bridges
                            BrokerClientHelper.sendReinitializeSDeviceBridge(Constants.STARFLEX_TAG_BRIDGES_CODE_ACRONYM + groupThing.getCode(),
                                    groupThing.getCode(), false, groupMqtt);
                            BrokerClientHelper.sendReinitializeSDeviceBridge(ALEBRIDGES_CODE + groupThing.getCode(), groupThing.getCode(),
                                    false, groupMqtt);
                            BrokerClientHelper.sendReinitializeSDeviceBridge(COREBRIDGES_CODE + groupThing.getCode(), groupThing.getCode(),
                                    false, groupMqtt);
                            //Delete the device with unclaimed state
                            ThingService.getInstance().secureDelete(thingConfigUnclaimed.getId(), true, true, true, true, false);
                            openTransaction();
                            ThingService.getInstance().secureDelete(thingStatusUnclaimed.getId(), true, true, true, true, false);
                            openTransaction();
                            result.put("thing", (ThingMongoDAO.getInstance().getThing(thingConfig.getId())));
                            result.put("thingStatus", (ThingMongoDAO.getInstance().getThing(thingStatus.getId())));
                            String message = "STARflex ID '"+serialMap.get("serial").toString()+
                                    "' has been already claimed for Company '"+ thingConfig.getGroup().getName()+"'. It has been reactivated.";
                            result.put("message",message);
                        } else {
                            Date transactionDate = ThingService.getInstance().getDate(null);
                            //Every Claim has to update the 'discoveryDate'
                            configUnclaimedMap.put("claimDate", new Date());
                            //Update Thing
                            result = ThingService.getInstance().update(
                                    recursivelyStack
                                    , thingConfigUnclaimed
                                    , thingTypeStarflexConfig.getThingTypeCode()
                                    , group.getHierarchyName(false)
                                    , configUnclaimedMap.get("name").toString()
                                    , configUnclaimedMap.get("serialNumber").toString()
                                    , null //parent
                                    , this.getMapOfUdfs(thingTypeStarflexConfig, configUnclaimedMap)
                                    , null //children
                                    , null //childrenUdf
                                    , true //executeTickle
                                    , false //validateVisibility
                                    , transactionDate //transactionDate
                                    , false, null, null, true, true, user, true);
                            result = ThingService.getInstance().update(
                                    recursivelyStack
                                    , thingStatusUnclaimed
                                    , thingTypeStarflexStatus.getThingTypeCode()
                                    , group.getHierarchyName(false)
                                    , statusUnclaimedMap.get("name").toString()
                                    , statusUnclaimedMap.get("serialNumber").toString()
                                    , null //parent
                                    , this.getMapOfUdfs(thingTypeStarflexConfig, statusUnclaimedMap)
                                    , null //children
                                    , null //childrenUdf
                                    , true //executeTickle
                                    , false //validateVisibility
                                    , transactionDate //transactionDate
                                    , false, null, null, true, true, user, true);

                            //Initialize Device
                            BrokerClientHelper.sendInitDevice(
                                    thingTypeStarflexConfig.getThingTypeCode(),
                                    thingTypeStarflexConfig.getGroup().getCode(),
                                    configUnclaimedMap.get("serialNumber").toString(),
                                    Constants.STARFLEX_TAG_BRIDGES_CODE_ACRONYM + group.getCode(),
                                    false,
                                    GroupService.getInstance().getMqttGroups(thingTypeStarflexConfig.getGroup()));
                            result.put("thing", (ThingMongoDAO.getInstance().getThing(thingConfigUnclaimed.getId())));
                            result.put("thingStatus", (ThingMongoDAO.getInstance().getThing(thingStatusUnclaimed.getId())));
                        }
                    } catch (Exception e) {
                        String errorMessage = e.getMessage();
                        if (errorMessage.contains("Thing with serial number '")) {
                            errorMessage = "The STARflex ID '" + serialMap.get("serial").toString() + "' has not succesfully phoned home to ViZix yet.";
                        }
                        if (errorMessage.contains("Thing ") || errorMessage.contains("Serial: ")) {
                            errorMessage = errorMessage.replace("Thing ", "STARflex ID ");
                            errorMessage = errorMessage.replace("Serial: ", "STARflex ID ");
                        }
                        throw new UserException(errorMessage);
                    }
                } else {
                    throw new UserException("Cannot migrate things from root to "+ group.getName() + "because Config and Status Things are inconsistent.");
                }
            }
            result.put("totalSerial", serials.size());
            result.put("totalMigrated", serials.size());
        }
        return result;
    }

    private void openTransaction(){
        Session session = HibernateSessionFactory.getInstance().getCurrentSession();
        Transaction transaction = session.getTransaction();
        if(!transaction.isActive()){
            transaction.begin();
        }
    }


    /**********************************************
     * Method to get Udf's so as to send to Thing Service
     **********************************************/
    public Map<String, Object> getMapOfUdfs(ThingType thingType, Map<String, Object> map) {
        Map<String, Object> udfs = new HashMap<>();
        Map<String, Object> value = new HashMap<>();

        if (map != null) {
            if (thingType.getThingTypeFields() == null){
                thingType.setThingTypeFields(new HashSet<ThingTypeField>());

                List<ThingTypeField> thingTypeFields = ThingTypeFieldService.getInstance().getByThingTypeCode(thingType.getThingTypeCode());
                for (ThingTypeField thingTypeField: thingTypeFields) {
                    thingType.getThingTypeFields().add(thingTypeField);
                }
            }
            //Iterate thing types
            for (ThingTypeField thingTypeField : thingType.getThingTypeFields()) {
                if (map.containsKey(thingTypeField.getName())) {
                    Object valueProperty = null;
                    value = new HashMap<String, Object>();
                    if (map.get(thingTypeField.getName()) != null && map.get(thingTypeField.getName()) instanceof Date) {
                        valueProperty = ((Date) map.get(thingTypeField.getName())).getTime();
                    } else {
                        valueProperty = map.get(thingTypeField.getName());
                    }
                    value.put("value", valueProperty);
                    udfs.put(thingTypeField.getName(), value);
                }
            }
        }
        return udfs;
    }

    /**
     * Boolean method, true:StarFLEX reports exists
     * false: they do not exist
     *
     * @return
     */
    public boolean isStarFLEXreportsExists(Group group) {
        boolean response = false;
        Pagination pagination = new Pagination(1, 1000);
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QReportDefinition.reportDefinition.name.eq(Constants.STARFLEX_DASHBOARD + " - " + group.getName()));
        be = be.and(QReportDefinition.reportDefinition.reportType.eq("table"));
        be = be.and(QReportDefinition.reportDefinition.group.eq(group));
        List<ReportDefinition> reportDefinitions = ReportDefinitionService.getInstance().listPaginated(be, pagination, null);
        if (reportDefinitions != null && reportDefinitions.size() > 0) {
            response = true;
        }
        return response;
    }

    /**
     * Create StarFLEX Reports: Device and Tag
     * @param createdByUser User who creates the report
     * @param group Company
     */
    public void createStarFLEXReport(
            User createdByUser
            , Group group
            , ThingType thingTypeStarFlexConfig
            , ThingType thingTypeStarFlexStatus
            , ThingType thingTypeStarFlexTag)
    {
        //STARflex Dashboard Report
        Map<String,Object> filtersDashboard = new HashMap<>();
        filtersDashboard.put("labels", new String[]{ "Thing Type", "group" });
        filtersDashboard.put("propertyNames", new String[]{ "thingType.id", "group.id" });
        filtersDashboard.put("propertyOrders", new String[]{ "1", "2" });
        filtersDashboard.put("operators", new String[]{ "==", "=" });
        filtersDashboard.put("values", new String[]{ thingTypeStarFlexConfig.getId().toString(), group.getId().toString() });
        filtersDashboard.put("isEditable", new Boolean[]{ false, false });

        Map<String, Object> propertiesDashboard = new HashMap<>();
        propertiesDashboard.put("labels", new String[]{"MAC id", "IP", "Firmware", "Discovered","Claimed","State"});
        propertiesDashboard.put("propertyNames", new String[]{"macId", "netIfs--eth0__0__--address", "firmwareVersion"
                , "discoveryDate","claimDate","state"});
        propertiesDashboard.put("propertyOrders", new String[]{"1", "2", "3", "4", "5", "6"});
        propertiesDashboard.put("propertyTypes", new Long[]{thingTypeStarFlexConfig.getId(), thingTypeStarFlexConfig.getId()
                ,thingTypeStarFlexConfig.getId(),thingTypeStarFlexConfig.getId(), thingTypeStarFlexConfig.getId()
                ,thingTypeStarFlexConfig.getId()});
        propertiesDashboard.put("showPopUp", new Boolean[]{true, true, true, true, true, true, true, true, true});

        ReportDefinitionService.getInstance().createReportDefinition(Constants.STARFLEX_DASHBOARD
                , group
                , createdByUser
                , filtersDashboard
                , propertiesDashboard);

        //STARflex Monitor
        Map<String, Object> propertiesMonitor = new HashMap<>();
        propertiesMonitor.put("labels", new String[]{"IP", "MAC id", "State","CPU Utilization", "LED Status"});
        propertiesMonitor.put("propertyNames", new String[]{
                "netIfs--eth0__0__--address", "macId",
                "state","cpuUtilization","ledStatus"});
        propertiesMonitor.put("propertyOrders", new String[]{"1", "2", "3","4","5"});
        propertiesMonitor.put("propertyTypes", new Long[]{
                thingTypeStarFlexConfig.getId(), thingTypeStarFlexConfig.getId()
                ,thingTypeStarFlexConfig.getId(),thingTypeStarFlexConfig.getId()
                ,thingTypeStarFlexConfig.getId()});
        propertiesMonitor.put("showPopUp", new Boolean[]{true, true, true, true, true});

        ReportDefinitionService.getInstance().createReportDefinition(Constants.STARFLEX_MONITOR
                , group
                , createdByUser
                , filtersDashboard
                , propertiesMonitor);

        //STARflex Status Report
        Map<String,Object> filtersConfig = new HashMap<>();
        filtersConfig.put("labels", new String[]{ "Thing Type", "group" });
        filtersConfig.put("propertyNames", new String[]{ "thingType.id", "group.id" });
        filtersConfig.put("propertyOrders", new String[]{ "1", "2" });
        filtersConfig.put("operators", new String[]{ "==", "=" });
        filtersConfig.put("values", new String[]{ thingTypeStarFlexStatus.getId().toString(), group.getId().toString() });
        filtersConfig.put("isEditable", new Boolean[]{ false, false });

        Map<String, Object> propertiesConfig = new HashMap<>();
        propertiesConfig.put("labels", new String[]{"MAC id", "Date Time", "Uptime", "Free Memory", "Temperature"});
        propertiesConfig.put("propertyNames", new String[]{"serial", "wallclock", "upTime_sec", "freeMem", "temperature_f_"});
        propertiesConfig.put("propertyOrders", new String[]{"1", "2", "3", "4", "5"});
        propertiesConfig.put("propertyTypes", new Long[]{thingTypeStarFlexStatus.getId(), thingTypeStarFlexStatus.getId(),
                thingTypeStarFlexStatus.getId(),thingTypeStarFlexStatus.getId(),thingTypeStarFlexStatus.getId()});
        propertiesConfig.put("showPopUp", new Boolean[]{true, true, true, true, true});

        ReportDefinitionService.getInstance().createReportDefinition(Constants.STARFLEX_STATUS
                , group
                , createdByUser
                , filtersConfig
                , propertiesConfig);

        //STARflex Unique Tags Report
        Map<String,Object> filtersUniqueTags = new HashMap<>();
        filtersUniqueTags.put("labels", new String[]{ "Thing Type", "group" });
        filtersUniqueTags.put("propertyNames", new String[]{ "thingType.id", "group.id" });
        filtersUniqueTags.put("propertyOrders", new String[]{ "1", "2" });
        filtersUniqueTags.put("operators", new String[]{ "==", "=" });
        filtersUniqueTags.put("values", new String[]{ thingTypeStarFlexTag.getId().toString(), group.getId().toString() });
        filtersUniqueTags.put("isEditable", new Boolean[]{ false, false });

        Map<String, Object> propertiesUniqueTags = new HashMap<>();
        propertiesUniqueTags.put("labels", new String[]{ "Name", "Timestamp", "TxID" });
        propertiesUniqueTags.put("propertyNames", new String[]{ "name", "datestamp", "TxID" });
        propertiesUniqueTags.put("propertyOrders", new String[]{"1", "2", "3"});
        propertiesUniqueTags.put("propertyTypes", new Long[]{thingTypeStarFlexTag.getId(), thingTypeStarFlexTag.getId()
                , thingTypeStarFlexTag.getId()});
        propertiesUniqueTags.put("showPopUp", new Boolean[]{true, true, true});

        ReportDefinitionService.getInstance().createReportDefinition(Constants.STARFLEX_UNIQUE_TAGS
                , group
                , createdByUser
                , filtersUniqueTags
                , propertiesUniqueTags);
    }

    /**
     * Create Thing for Starflex (Used by Phone Home)
     * @param mapAsUDFs
     * @param thingType
     * @param mac
     * @param recursivelyStack
     * @throws NonUniqueResultException
     */
    public Map<String, Object> createThingStarflex(
            Map<String, Object> mapAsUDFs,
            ThingType thingType,
            String mac,
            User currentUser,
            Stack<Long> recursivelyStack )
    {
        Map<String,Object> result = new HashMap<>();
        try {
            //ThingType thingType = ThingTypeService.getInstance().getByCode(thingTypeCode.trim());
            Thing thing = ThingService.getInstance().getBySerialNumberWithFields(mac, thingType);
            Map<String, Object> udf = getUdfValuesForThingType(thingType.getThingTypeFields(), mapAsUDFs);
            if (thing == null) {
                //Create a new Thing
                result = ThingsService.getInstance().create(
                        recursivelyStack
                        , thingType.getThingTypeCode()
                        , null
                        , mac
                        , mac
                        , null
                        , udf
                        , null
                        , null
                        , true // executeTickle
                        , true // validateVisibility
                        , new Date() // transactionDate
                        , true // disableFMCLogic
                        , false // createAndFlush
                        , true // useDefaultValues
                        , null
                        , null
                        , true
                        , null);
                logger.info( "A new thing created with serial "+mac);
            } else {
                Map <String, Boolean> validations = new HashMap<>();
                validations.put("thingType", true);
                validations.put("group", true);
                validations.put("thing.exists", false);
                validations.put("thing.serial", false);
                validations.put("thing.parent", false);
                validations.put("thing.children", false);
                validations.put("thing.udfs", true);
                //Update Thing
                result = ThingService.getInstance().update(
                        recursivelyStack
                        , thing
                        , thingType.getThingTypeCode()
                        , null
                        , mac
                        , mac
                        , null
                        , udf
                        , null
                        , null
                        , false // executeTickle
                        , true // validateVisibility
                        , new Date() // transactionDate
                        , true // disableFMCLogic
                        , validations // validations
                        , null // cache
                        , false // updateAndFlush
                        , true // recursivilyUpdate
                        , currentUser
                        , true
                );
                logger.info("Thing updated with serial "+mac);
            }
        } catch (NonUniqueResultException e) {
            logger.error("We cannot get Thing Type neither Thing with serial"+mac+", Thing Type:" +thingType.getThingTypeCode(),e);
            throw new UserException("We cannot get Thing Type neither Thing with serial"+mac+", Thing Type:"+thingType.getThingTypeCode(), e);
        }

        return result;
    }

    /**
     * Get Udf Values for Thing Type
     * @param fields
     * @param mapAsUDFs
     * @return
     */
    public Map<String, Object> getUdfValuesForThingType(Set<ThingTypeField> fields, Map<String, Object> mapAsUDFs) {
        Map<String, Object> result = new HashMap<>();
        if( (fields != null) && (!fields.isEmpty()) ) {
            for(ThingTypeField field : fields) {
                if (mapAsUDFs.containsKey(field.getName())) {
                    result.put(field.getName(), mapAsUDFs.get(field.getName()));
                }
            }
        }
        return result;
    }

    /**
     * Create Complete Thing Type
     * @param group
     * @param name
     * @param templateName
     * @param autoCreate
     * @return
     */
    public ThingType createCompleteThingTypeByTemplate(Group group, String name, String templateName, boolean autoCreate) {
        String code = ThingTypeService.getInstance().getThingTypeCodeByName(name);
        ThingTypeTemplate template = null;
        try {
            template = ThingTypeTemplateService.getInstance().getByCode(templateName);
        } catch(NonUniqueResultException e) {
            throw new UserException("Error to get Template Object :" + templateName,e);
        }
        ThingType thingType = ThingTypeService.getInstance().insertThingTypeAndFieldsWithTemplate(
                group, name, code, template,autoCreate);
        return thingType;
    }

    /**
     * Get Thing
     * @param serial
     * @param thingTypeCode
     * @return
     */
    public Map<String, Object> getMapThing (String serial, String thingTypeCode) {
        String where = "serialNumber='"+ serial+"'&thingTypeCode=" + thingTypeCode;
        List<String> filterValues = new ArrayList<>();
        filterValues.add("*");
        Map<String, Object> object = ThingMongoDAO.getInstance().getThingUdfValues(where,null,filterValues,null);
        Map<String, Object> map = (Map<String, Object>) ((List)object.get("results")).get(0);
        return map;
    }

    /**
     * Get Thing
     * @param serial
     * @param thingTypeCode
     * @return
     */
    public Thing getThing(String serial, String thingTypeCode) {
        Thing thing = null;
        if ((!StringUtils.isEmpty(serial)) && (!StringUtils.isEmpty(thingTypeCode))) {
            try {
                BooleanBuilder be = new BooleanBuilder();
                be = be.and(QThing.thing.serial.eq(serial));
                be = be.and(QThing.thing.thingType.eq(ThingTypeService.getInstance().getByCode(thingTypeCode)));
                List<Thing> things = ThingService.getInstance().listPaginated( be, null, null);
                if( (things != null) && (!things.isEmpty()) ) {
                    thing = things.get(0);
                }
            } catch (NonUniqueResultException e) {
                throw new UserException("Error to get Thing: "+ serial+", Thing Type Code: "+thingTypeCode,e);
            }
        }
        return thing;
    }

    /**
     * Get STARflex Status
     * @param serial
     * @param group
     * @param thingTypeTemplateName
     * @return
     */
    public Map<String, Object> getStarflexStatusUnclaimed(String serial, Group group, String thingTypeTemplateName) {
        Map<String, Object> result = new HashMap<>();
        ThingTypeTemplate template = null;
        try {
            template = ThingTypeTemplateService.getInstance().getByCode(thingTypeTemplateName);
        } catch(NonUniqueResultException e) {
            throw new UserException("Error to get Template Object :" + thingTypeTemplateName ,e);
        }
        BooleanBuilder bb = new BooleanBuilder();
        bb.and(QThingType.thingType.group.eq(group)).and(QThingType.thingType.thingTypeTemplate.eq(template));
        List<ThingType> lstThingType = ThingTypeService.getInstance().listPaginated(bb, null, null);
        if ( (lstThingType != null) && (!lstThingType.isEmpty()) ) {
            result = getMapThing(serial, lstThingType.get(0).getThingTypeCode());
        }
        return result;
    }

    /**
     * Search Device , only things that do not own to "STARflex Status" Template
     * @param serialNumber Serial Number
     * @param groupId Group Id
     * @param currentUserBean User Bean
     * @return Map with results
     */
    public Map<String, Object> searchDevice(String serialNumber, Long groupId, User currentUserBean) {
        Map<String, Object> result = new HashMap<>();
        BooleanBuilder bb = new BooleanBuilder();
        bb.and(QThing.thing.serial.eq(serialNumber));
        if(groupId != null) {
            bb.and(QThing.thing.group.id.eq(groupId));
        }
        bb.and(QThing.thing.thingType.thingTypeTemplate.name.ne(Constants.TT_STARflex_STATUS_NAME));
        List<Thing> lstThing = ThingService.getInstance().listPaginated(bb, null, null);
        String where = "";
        if ( (lstThing != null) && (!lstThing.isEmpty())) {
            for(Thing thing : lstThing) {
                where = where + "_id=" + thing.getId()+"|";
            }
            where = where.substring(0,where.length()-1);
            result = ThingService.getInstance().processListThings(null,null,null,where,null,null,null,null,"","",false, currentUserBean, false);
        } else {
            result.put("total", "0");
            result.put("results", new ArrayList<>());
        }
        return result;
    }
}

/**
 * Class to handle http request
 */
class SimpleResponseHandler implements RestClient.ResponseHandler {
    private static Logger logger = Logger.getLogger(SimpleResponseHandler.class);
    private String response;

    @Override
    public void success(InputStream is) {
        logger.info(is.toString());
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charsets.UTF_8));
        StringBuilder result = new StringBuilder();
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
        } catch (IOException e) {
            logger.error(e);
            throw new UserException("Error connecting to external MAC validation", e);
        }
        logger.info("MAC validation response from external web service: " + result.toString());
        response = result.toString();
    }

    @Override
    public void error(InputStream is) {
        logger.info(is.toString());
    }

    public String getResponse() {
        return response;
    }

}
