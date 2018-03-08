package com.tierconnect.riot.iot.popdb;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.Resource;
import com.tierconnect.riot.appcore.entities.Role;
import com.tierconnect.riot.appcore.popdb.PopDBRequired;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.ResourceService;
import com.tierconnect.riot.appcore.services.RoleResourceService;
import com.tierconnect.riot.appcore.services.RoleService;
import com.tierconnect.riot.appcore.utils.Configuration;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.iot.dao.mongo.MongoScriptDAO;
import com.tierconnect.riot.iot.dao.mongo.ThingMongoDAO;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.hibernate.Transaction;
import org.jose4j.json.internal.json_simple.JSONArray;
import org.jose4j.json.internal.json_simple.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.tierconnect.riot.commons.Constants.*;

/**
 * @author terry
 *         <p>
 *         This class populates the minimum required records for any appcore
 *         instance
 */
public class PopDBRequiredIOT {

    public static final String thing_editOwn_label = "Always allow a user to edit his own report";
    public static final String thing_editOwn_description = "Allows a user to see/edit reportDefinition  objects created by him that are in an upper level";
    public static final String reportDefinition_emailRecipients_label = "Allow see/edit Email Recipients ";
    public static final String reportDefinition_emailRecipients_description = "Allow see/edit Email Recipients ";
    public static final String reportDefinition_editTableScript_label = "Allow edit reportDefinition on table script type ";
    public static final String reportDefinition_editTableScript_description = "Allow edit reportDefinition on table script type ";
    public static final String reportDefinition_assignUnAssignThing_label = "Allow assign or un-assign a thing to other on Reports ";
    public static final String reportDefinition_assignUnAssignThing_description = "Allow user to assign or un-assign a thing to other on report definition ";
    public static final String reportDefinition_assignThing_label = "Allow assign a thing to other on Reports ";
    public static final String reportDefinition_assignThing_description = "Allow user to assign a thing to other on report definition ";
    public static final String reportDefinition_unAssignThing_label = "Allow un-assign a thing from other on Reports ";
    public static final String reportDefinition_unAssignThing_description = "Allow user un-assign a thing from other on report definition ";
    public static final String reportDefinition_inlineEdit_label = "Allow edit UDF of things on Reports ";
    public static final String reportDefinition_inlineEdit_description = "Allow user to edit UDF of things on Report definition ";
    static Logger logger = Logger.getLogger(PopDBRequiredIOT.class);

    public static void main(String args[]) {
        PopDBRequired.initJDBCDrivers();
        System.getProperties().put("hibernate.hbm2ddl.auto", "update");
        System.getProperties().put("hibernate.cache.use_second_level_cache", "false");
        System.getProperties().put("hibernate.cache.use_query_cache", "false");
        Configuration.init(null);

        PopDBRequiredIOT popdb = new PopDBRequiredIOT();
        Transaction transaction = GroupService.getInstance().getGroupDAO().getSession().getTransaction();
        transaction.begin();
        PopDBIOTUtils.initShiroWithRoot();
        popdb.run();
        //Populate Parameters
        populateParameters();
        transaction.commit();
    }

    public void run() {
        Group rootGroup = GroupService.getInstance().getRootGroup();
        Role rootRole = RoleService.getInstance().getRootRole();
//		Role              tenantAdminRole = RoleService.getInstance().getTenantAdminRole();
//		Date              storageDate     = new Date();

        HashSet<Resource> resources = PopulateResourcesIOT.populatePopDBRequiredIOT(rootGroup);

        Group tenant = GroupService.getInstance().get(2L);

        //<REQ-4417>
        //Populate Data type
        populateDataType();
        // populate category template
        populateCategory();
        //Populate Thing Type Templates
        populateThingTypetemplates(rootGroup);
        //Populate StarFlex Thing Type Templates
        polulateStarFlexThingTypeTemplates(rootGroup);
        //Populate Core and Edge Bridge Thing Type Templates
        polulateBridgeThingTypeTemplates(rootGroup);
        //Populate Mojix Retail App Thing Type Templates
        populateMojixRetailAppThingTypeTemplateBase(rootGroup);

        //Creation of indexes
        ThingMongoDAO.getInstance().createThingSnapshotsIndexes();
        ThingMongoDAO.createIndexInThingsCollection(ThingMongoDAO.THINGS, "thingTypeId");

        for (Resource resource : resources) {
            RoleResourceService.getInstance().insert(rootRole, resource, resource.getAcceptedAttributes());
//			RoleResourceService.getInstance().insert(tenantAdminRole, resource, resource.getAcceptedAttributes());
            ResourceService.getInstance().update(resource);

        }
        List<Resource> resources1 = ResourceService.list();
        for (Resource resource : resources1) {
            if (resource.getLabel().toString().equals("Default RFID Thing 1") ||
                    (resource.getParent() != null &&
                            resource.getParent().getName().equals("Thing Types"))) {
                RoleResourceService.getInstance().insert(rootRole, resource, resource.getAcceptedAttributes());
            }
        }

        insertUnit(tenant);

        ZoneType zoneType = ZoneTypeService.getInstance().get(1L);
        PopDBIOTUtils.popZoneProperty("Default Property", 1, zoneType);

        // maker
        PopulateDBRiotMaker popDbMaker = new PopulateDBRiotMaker();
        popDbMaker.install();

        //TODO: HACK: XXX: change this code to another package.
        try {
            Class clazz = Class.forName("com.tierconnect.riot.migration.DBHelper");
            Method executeSQLScript = clazz.getMethod("executeSQLScript", String.class);
            executeSQLScript.invoke(null, "DELIMITER $$ CREATE VIEW `thingtypedirectionmap` AS\n" +
                    "        SELECT\n" +
                    "            `ttp`.`id` AS `thingTypeParentId`,\n" +
                    "            `ttp`.`thingTypeCode` AS `thingTypeParentCode`,\n" +
                    "            `ttp`.`group_id` AS `thingTypeParentGroupId`,\n" +
                    "            `ttc`.`id` AS `thingTypeChildId`,\n" +
                    "            `ttc`.`thingTypeCode` AS `thingTypeChildCode`,\n" +
                    "            `ttc`.`group_id` AS `thingTypeChildGroupId`,\n" +
                    "            'down' AS `mapDirection`\n" +
                    "        FROM\n" +
                    "            ((`thingtype` `ttp`\n" +
                    "            JOIN `thingtypemap` `ttm` ON ((`ttp`.`id` = `ttm`.`parent_id`)))\n" +
                    "            JOIN `thingtype` `ttc` ON ((`ttc`.`id` = `ttm`.`child_id`)))\n" +
                    "        UNION SELECT\n" +
                    "            `ttp`.`id` AS `thingTypeParentId`,\n" +
                    "            `ttp`.`thingTypeCode` AS `thingTypeParentCode`,\n" +
                    "            `ttp`.`group_id` AS `thingTypeParentGroupId`,\n" +
                    "            `ttc`.`id` AS `thingTypeChildId`,\n" +
                    "            `ttc`.`thingTypeCode` AS `thingTypeChildCode`,\n" +
                    "            `ttc`.`group_id` AS `thingTypeChildGroupId`,\n" +
                    "            'down' AS `mapDirection`\n" +
                    "        FROM\n" +
                    "            ((`thingtype` `ttp`\n" +
                    "            JOIN `thingtypefield` `ttcf` ON ((`ttp`.`id` = `ttcf`.`dataTypeThingTypeId`)))\n" +
                    "            JOIN `thingtype` `ttc` ON ((`ttc`.`id` = `ttcf`.`thingType_id`)))\n" +
                    "        WHERE\n" +
                    "            (`ttp`.`isParent` = TRUE AND ttcf.typeParent = 'NATIVE_THING_TYPE')\n" +
                    "        UNION SELECT\n" +
                    "            `ttp`.`id` AS `thingTypeParentId`,\n" +
                    "            `ttp`.`thingTypeCode` AS `thingTypeParentCode`,\n" +
                    "            `ttp`.`group_id` AS `thingTypeParentGroupId`,\n" +
                    "            `ttc`.`id` AS `thingTypeChildId`,\n" +
                    "            `ttc`.`thingTypeCode` AS `thingTypeChildCode`,\n" +
                    "            `ttc`.`group_id` AS `thingTypeChildGroupId`,\n" +
                    "            'down' AS `mapDirection`\n" +
                    "        FROM\n" +
                    "            ((`thingtype` `ttp`\n" +
                    "            JOIN `thingtypefield` `ttpf` ON ((`ttp`.`id` = `ttpf`.`thingType_id`)))\n" +
                    "            JOIN `thingtype` `ttc` ON ((`ttc`.`id` = `ttpf`.`dataTypeThingTypeId`)))\n" +
                    "        WHERE\n" +
                    "            (`ttc`.`isParent` = FALSE  AND ttpf.typeParent = 'NATIVE_THING_TYPE');\n" +
                    "$$ DELIMITER;\n");
        } catch (ClassNotFoundException | IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
            logger.error("Error creating view: ", ex);
        }
    }

    private void insertUnit(Group tenant) {
        // BASE UNITS (PER ISO STANDARDS), see
        // http://en.wikipedia.org/wiki/International_System_of_Units

        insertUnit(tenant, "meter", "m", "length", "1");
        insertUnit(tenant, "kilogram", "kg", "mass", "1");
        insertUnit(tenant, "second", "s", "time", "1");
        insertUnit(tenant, "kelvin", "K", "temperature", "1");
        insertUnit(tenant, "ampere", "A", "electric current", "1");
        insertUnit(tenant, "mole", "mol", "amount of substance", "1");
        insertUnit(tenant, "candela", "cd", "luminous intensity", "1");

        insertUnit(tenant, "slug", "slug", "mass", "14.5939029*kg");

        insertUnit(tenant, "feet", "ft", "length", "0.3048*m");
        insertUnit(tenant, "inch", "in", "length", "ft/12.0");
        insertUnit(tenant, "yard", "yd", "length", "3.0*ft");
        insertUnit(tenant, "mile", "mile", "length", "5280.0*ft");

        insertUnit(tenant, "radian", "rad", "angle", "1");
        insertUnit(tenant, "degree", "deg", "angle", "180.0*rad/PI");

        insertUnit(tenant, "newton", "N", "force", "kg * m / s ^ 2");

        insertUnit(tenant, "lb-force", "lbf", "force", "slug * ft / s ^ 2");

        insertUnit(tenant, "lb-mass", "lbm", "mass", "32.2 * slug");

        insertUnit(tenant, "pound force per inch squared", "psi", "pressure", "lbf/in^2");

        insertUnit(tenant, "kilo pound force per inch squared", "kips", "pressure", "1000*lbf/in^2");

        insertUnit(tenant, "pascal", "pascal", "pressure", "N/m^2");

        ZoneType zoneType = ZoneTypeService.getInstance().get(1L);
        PopDBIOTUtils.popZoneProperty("Default Property", 1, zoneType);

        // maker
        PopulateDBRiotMaker popDbMaker = new PopulateDBRiotMaker();
        popDbMaker.install();

        addMongoFunctions();
    }

    public void insertUnit(Group group, String unitName, String unitSymbol, String quantityName, String definition) {
        Unit unit = new Unit();
        unit.setGroup(group);
        unit.setUnitName(unitName);
        unit.setUnitSymbol(unitSymbol);
        unit.setQuantityName(quantityName);
        unit.setDefinition(definition);
        UnitService.getInstance().insert(unit);
    }

    public static String getOldCoreBridgeConfiguration(String[] topicsMqtt, String brokerCon, String mongoCon) {
        ObjectMapper mapper = new ObjectMapper();

        ObjectNode configuration = mapper.createObjectNode();
        configuration.put("numberOfThreads", 32);

        //SQL
        ObjectNode sql = mapper.createObjectNode();
        sql.put("connectionCode", "SQL");
        configuration.set("sql", sql);

        //REST
        ObjectNode rest = mapper.createObjectNode();
        rest.put("connectionCode", "SERVICES");
        configuration.set("rest", rest);

        //MQTT
        ArrayNode mqttTopicsArray = mapper.createArrayNode();
        for (String topic : topicsMqtt) {
            mqttTopicsArray.add(topic);
        }

        ObjectNode mqtt = mapper.createObjectNode();
        mqtt.put("connectionCode", brokerCon);
        mqtt.put("active", true);
        mqtt.put("topics", mqttTopicsArray);

        configuration.set("mqtt", mqtt);

        //MONGO
        ObjectNode mongo = mapper.createObjectNode();
        mongo.put("connectionCode", mongoCon);

        configuration.set("mongo", mongo);

        //SEQUENCE_NUMBER_LOGGING
        ObjectNode sequenceNumberLogging = mapper.createObjectNode();
        sequenceNumberLogging.put("active", 0);
        sequenceNumberLogging.put("TTL", 86400);
        sequenceNumberLogging.put("GC_GRACE_SECONDS", 0);

        configuration.set("sequenceNumberLogging", sequenceNumberLogging);

        //SOURCE_RULE
        ObjectNode sourceRule = mapper.createObjectNode();
        sourceRule.put("active", 0);
        configuration.set("sourceRule", sourceRule);

        //OUTOFORDER_RULE
        ObjectNode outOfOrderRule = mapper.createObjectNode();
        outOfOrderRule.put("active", false);

        configuration.set("outOfOrderRule", outOfOrderRule);

        //TIMEORDER_RULE
        ObjectNode timeOrderRule = mapper.createObjectNode();
        timeOrderRule.put("active", false);
        timeOrderRule.put("period", 0);

        configuration.set("timeOrderRule", timeOrderRule);

        //INTER_CACHE_EVICTION
        ObjectNode interCacheEviction = mapper.createObjectNode();
        interCacheEviction.put("active", false);

        configuration.set("interCacheEviction", interCacheEviction);


        //SWARM_FILTER
        ObjectNode swarmFilter = mapper.createObjectNode();
        swarmFilter.put("active", false);
        swarmFilter.put("timeGroupTimer", 5000);
        swarmFilter.put("swarmAlgorithm", "followLastDetect");

        ObjectNode swarmFilterThingType = mapper.createObjectNode();
        swarmFilterThingType.put("thingTypeCode", "default_rfid_thingtype");
        swarmFilterThingType.put("udfGroupStatus", "groupStatus");
        swarmFilterThingType.put("udfGroup", "grouping");
        swarmFilterThingType.put("distanceFilter", 10000);

        ArrayNode swarmFilterThingTypes = mapper.createArrayNode();
        swarmFilterThingTypes.add(swarmFilterThingType);

        swarmFilter.set("thingTypes", swarmFilterThingTypes);

        configuration.set("swarmFilter", swarmFilter);


        //CEP_LOGGINGG
        ObjectNode cepLogging = mapper.createObjectNode();
        cepLogging.put("active", 0);

        configuration.set("CEPLogging", cepLogging);

        //SHIFT_ZONE_RULE
        ObjectNode shiftZoneRule = mapper.createObjectNode();
        shiftZoneRule.put("active", 1);
        shiftZoneRule.put("shiftProperty", "shift");
        shiftZoneRule.put("zoneViolationStatusProperty", "zoneViolationStatus");
        shiftZoneRule.put("zoneViolationFlagProperty", "zoneViolationFlag");

        configuration.set("shiftZoneRule", shiftZoneRule);

        //CHECK_MULTILEVEN_REFERENCE
        ObjectNode checkMultilevelReferences = mapper.createObjectNode();
        shiftZoneRule.put("active", 0);

        configuration.set("checkMultilevelReferences", checkMultilevelReferences);

        //Bridges necessary parameters
        ObjectNode cepEngineConfiguration = mapper.createObjectNode();
        cepEngineConfiguration.put("insertIntoDispatchPreserveOrder", false);
        cepEngineConfiguration.put("listenerDispatchPreserveOrder", false);
        cepEngineConfiguration.put("multipleInstanceMode", false);

        configuration.set("CEPEngineConfiguration", cepEngineConfiguration);

        configuration.put("interCacheEvictionQueueSize", 20000);

        configuration.put("fixOlderSnapshotsQueueSize", 20000);

        //RIOT-12855: Adding evaluateStats boolean parameter
        configuration.put("evaluateStats", true);

        return configuration.toString();
    }

    /*Populate Entity Description*/
    public static void populateDataType() {
        DataType dataType = new DataType();
        dataType.setId(20L);
        dataType.setTypeParent("THING_TYPE_PROPERTY");
        dataType.setCode(THING_TYPE_DATA_TYPE);
        dataType.setValue("Data Type");
        dataType.setType(null);
        dataType.setDescription("");
        DataTypeService.getInstance().insert(dataType);
        dataType = new DataType();
        dataType.setId(21L);
        dataType.setTypeParent("THING_TYPE_PROPERTY");
        dataType.setCode("NATIVE_THING_TYPE");
        dataType.setValue("Thing Type");
        dataType.setType(null);
        dataType.setDescription("");
        DataTypeService.getInstance().insert(dataType);
        //DATA_TYPE
        dataType = new DataType();
        dataType.setId(ThingTypeField.Type.TYPE_BOOLEAN.value);
        dataType.setTypeParent(THING_TYPE_DATA_TYPE);
        dataType.setCode("BOOLEAN");
        dataType.setValue("Boolean");
        dataType.setType("Standard Data Types");
        dataType.setDescription("Boolean");
        dataType.setClazz("java.lang.Boolean");
        DataTypeService.getInstance().insert(dataType);
        dataType = new DataType();
        dataType.setId(ThingTypeField.Type.TYPE_LONLATALT.value);
        dataType.setTypeParent(THING_TYPE_DATA_TYPE);
        dataType.setCode("COORDINATES");
        dataType.setValue("Coordinates");
        dataType.setType("Standard Data Types");
        dataType.setDescription("Coordinates");
        dataType.setClazz("java.lang.String");
        DataTypeService.getInstance().insert(dataType);
        dataType = new DataType();
        dataType.setId(ThingTypeField.Type.TYPE_IMAGE_ID.value);
        dataType.setTypeParent(THING_TYPE_DATA_TYPE);
        dataType.setCode("IMAGE");
        dataType.setValue("Image");
        dataType.setType("Standard Data Types");
        dataType.setDescription("Image");
        dataType.setClazz("java.lang.String");
        DataTypeService.getInstance().insert(dataType);
        dataType = new DataType();
        dataType.setId(ThingTypeField.Type.TYPE_IMAGE_URL.value);
        dataType.setTypeParent(THING_TYPE_DATA_TYPE);
        dataType.setCode("IMAGE_URL");
        dataType.setValue("Image URL");
        dataType.setType("Standard Data Types");
        dataType.setDescription("Image URL");
        dataType.setClazz("java.lang.String");
        DataTypeService.getInstance().insert(dataType);
        dataType = new DataType();
        dataType.setId(ThingTypeField.Type.TYPE_NUMBER.value);
        dataType.setTypeParent(THING_TYPE_DATA_TYPE);
        dataType.setCode("NUMBER");
        dataType.setValue("Number (Float)");
        dataType.setType("Standard Data Types");
        dataType.setDescription("Number (Float)");
        dataType.setClazz("java.math.BigDecimal");
        DataTypeService.getInstance().insert(dataType);
        dataType = new DataType();
        dataType.setId(ThingTypeField.Type.TYPE_TEXT.value);
        dataType.setTypeParent(THING_TYPE_DATA_TYPE);
        dataType.setCode("STRING");
        dataType.setValue("String");
        dataType.setType("Standard Data Types");
        dataType.setDescription("String");
        dataType.setClazz("java.lang.String");
        DataTypeService.getInstance().insert(dataType);
        dataType = new DataType();
        dataType.setId(ThingTypeField.Type.TYPE_TIMESTAMP.value);
        dataType.setTypeParent(THING_TYPE_DATA_TYPE);
        dataType.setCode("TIMESTAMP");
        dataType.setValue("Timestamp");
        dataType.setType("Standard Data Types");
        dataType.setDescription("Timestamp");
        dataType.setClazz("java.lang.Long");
        DataTypeService.getInstance().insert(dataType);
        dataType = new DataType();
        dataType.setId(ThingTypeField.Type.TYPE_XYZ.value);
        dataType.setTypeParent(THING_TYPE_DATA_TYPE);
        dataType.setCode("XYZ");
        dataType.setValue("XYZ");
        dataType.setType("Standard Data Types");
        dataType.setDescription("XYZ");
        dataType.setClazz("java.lang.String");
        DataTypeService.getInstance().insert(dataType);
//		dataType = new DataType();
//		dataType.setId(10L);
//		dataType.setTypeParent("DATA_TYPE");
//		dataType.setCode("JSON");
//		dataType.setValue( "Json");
//		dataType.setType("Standard Data Types");
//		dataType.setDescription("JSON");
//		DataTypeService.getInstance().insert(dataType);
        dataType = new DataType();
        dataType.setId(ThingTypeField.Type.TYPE_DATE.value);
        dataType.setTypeParent(THING_TYPE_DATA_TYPE);
        dataType.setCode("DATE");
        dataType.setValue("Date");
        dataType.setType("Standard Data Types");
        dataType.setDescription("Date");
        dataType.setClazz("java.util.Date");
        DataTypeService.getInstance().insert(dataType);
        dataType = new DataType();
        dataType.setId(ThingTypeField.Type.TYPE_URL.value);
        dataType.setTypeParent(THING_TYPE_DATA_TYPE);
        dataType.setCode("URL");
        dataType.setValue("Url");
        dataType.setType("Standard Data Types");
        dataType.setDescription("Url");
        dataType.setClazz("java.lang.String");
        DataTypeService.getInstance().insert(dataType);
        dataType = new DataType();
        dataType.setId(ThingTypeField.Type.TYPE_ZPL_SCRIPT.value);
        dataType.setTypeParent(THING_TYPE_DATA_TYPE);
        dataType.setCode("ZPL_SCRIPT");
        dataType.setValue("ZPL Script");
        dataType.setType("Standard Data Types");
        dataType.setDescription("ZPL Script");
        dataType.setClazz("java.lang.String");
        DataTypeService.getInstance().insert(dataType);


        //NATIVE_OBJECT
        dataType = new DataType();
        dataType.setId(ThingTypeField.Type.TYPE_GROUP.value);
        dataType.setTypeParent(THING_TYPE_DATA_TYPE);
        dataType.setCode("GROUP");
        dataType.setValue("Group");
        dataType.setType("Native Objects");
        dataType.setDescription("Group");
        dataType.setClazz("com.tierconnect.riot.appcore.entities.Group");
        DataTypeService.getInstance().insert(dataType);
        dataType = new DataType();
        dataType.setId(ThingTypeField.Type.TYPE_LOGICAL_READER.value);
        dataType.setTypeParent(THING_TYPE_DATA_TYPE);
        dataType.setCode("LOGICAL_READER");
        dataType.setValue("Logical Reader");
        dataType.setType("Native Objects");
        dataType.setDescription("Logical Reader");
        dataType.setClazz("com.tierconnect.riot.iot.entities.LogicalReader");
        DataTypeService.getInstance().insert(dataType);
        dataType = new DataType();
        dataType.setId(ThingTypeField.Type.TYPE_SHIFT.value);
        dataType.setTypeParent(THING_TYPE_DATA_TYPE);
        dataType.setCode("SHIFT");
        dataType.setValue("Shift");
        dataType.setType("Native Objects");
        dataType.setDescription("Shift");
        dataType.setClazz("com.tierconnect.riot.iot.entities.Shift");
        DataTypeService.getInstance().insert(dataType);
        dataType = new DataType();
        dataType.setId(ThingTypeField.Type.TYPE_ZONE.value);
        dataType.setTypeParent(THING_TYPE_DATA_TYPE);
        dataType.setCode("ZONE");
        dataType.setValue("Zone");
        dataType.setType("Native Objects");
        dataType.setDescription("Zone");
        dataType.setClazz("com.tierconnect.riot.iot.entities.Zone");
        DataTypeService.getInstance().insert(dataType);

        dataType = new DataType();
        dataType.setId(ThingTypeField.Type.TYPE_SEQUENCE.value);
        dataType.setTypeParent(THING_TYPE_DATA_TYPE);
        dataType.setCode("SEQUENCE");
        dataType.setValue("Sequence");
        dataType.setType("Standard Data Types");
        dataType.setDescription("Sequence");
        dataType.setClazz("java.math.BigDecimal");
        DataTypeService.getInstance().insert(dataType);

        dataType = new DataType();
        dataType.setId(ThingTypeField.Type.TYPE_FORMULA.value);
        dataType.setTypeParent(THING_TYPE_DATA_TYPE);
        dataType.setCode("FORMULA");
        dataType.setValue("Expression");
        dataType.setType("Standard Data Types");
        dataType.setDescription("Expression of formula");
        dataType.setClazz("java.lang.String");
        DataTypeService.getInstance().insert(dataType);

        if (DataTypeService.getDataTypeDAO().selectById(ThingTypeField.Type.TYPE_THING_TYPE.value) == null) {
            dataType = new DataType();
            dataType.setId(ThingTypeField.Type.TYPE_THING_TYPE.value);
            dataType.setTypeParent("NATIVE_THING_TYPE");
            dataType.setCode("THING_TYPE");
            dataType.setValue("Thing Type");
            dataType.setType("Native Thing Type");
            dataType.setDescription("Thing Type");
            dataType.setClazz("com.tierconnect.riot.iot.entities.Thing");
            DataTypeService.getInstance().insert(dataType);
        }

        if (DataTypeService.getDataTypeDAO().selectById(ThingTypeField.Type.TYPE_ATTACHMENTS.value) == null) {
            dataType = new DataType();
            dataType.setId(ThingTypeField.Type.TYPE_ATTACHMENTS.value);
            dataType.setTypeParent(THING_TYPE_DATA_TYPE);
            dataType.setCode("ATTACHMENT");
            dataType.setValue("Attachments");
            dataType.setType("Standard Data Types");
            dataType.setDescription("Attach one or many files");
            dataType.setClazz("java.lang.String");
            DataTypeService.getInstance().insert(dataType);
        }

        dataType = new DataType();
        dataType.setId(ThingTypeField.Type.TYPE_ICON.value);
        dataType.setTypeParent(THING_TYPE_DATA_TYPE);
        dataType.setCode("ICON");
        dataType.setValue("Icon");
        dataType.setType("Standard Data Types");
        dataType.setDescription("Icon data type");
        dataType.setClazz("java.lang.String");
        DataTypeService.getInstance().insert(dataType);

        dataType = new DataType();
        dataType.setId(ThingTypeField.Type.TYPE_COLOR.value);
        dataType.setTypeParent(THING_TYPE_DATA_TYPE);
        dataType.setCode("COLOR");
        dataType.setValue("Color");
        dataType.setType("Standard Data Types");
        dataType.setDescription("Color type");
        dataType.setClazz("java.lang.String");
        DataTypeService.getInstance().insert(dataType);
    }

    public static void populateCategory() {
        ThingTypeTemplateCategoryService categoryService = ThingTypeTemplateCategoryServiceBase.getInstance();
        categoryService.insert(new ThingTypeTemplateCategory(CATEGORY_THING_TYPE_TEMPLATE_CUSTOM, "CUSTOM", 8, "icon-thingtype-custom-custom"));
        categoryService.insert(new ThingTypeTemplateCategory(CATEGORY_THING_TYPE_TEMPLATE_SENSORS, "SENSORS THINGS", 2, "icon-thingtype-sensors"));
        categoryService.insert(new ThingTypeTemplateCategory(CATEGORY_THING_TYPE_TEMPLATE_M2M, "M2M THINGS", 3, "icon-thingtype-m2m"));
        categoryService.insert(new ThingTypeTemplateCategory(CATEGORY_THING_TYPE_TEMPLATE_MOJIX_RETAIL_APP_BASE, "RETAIL THINGS", 4, "icon-thingtype-retail-app-things"));
    }

    /**
     * Template with category
     *
     * @param group
     * @version 5.0.0 RC10
     */
    public void populateThingTypetemplates(Group group) {
        insertCustomTemplate(group);
        insertRFIDPrinterTemplate(group);
        insertLogicalReaderTemplate(group);
        insertRFIDTagTemplate(group);
        insertGPSTagTemplate(group);
        insertZPLTemplate(group);
    }

    /**
     * Insert Custom Template
     *
     * @param group group
     */
    public void insertCustomTemplate(Group group) {
        ThingTypeTemplateCategoryService categoryService = ThingTypeTemplateCategoryServiceBase.getInstance();
        ThingTypeTemplate thingTypeTemplate = new ThingTypeTemplate();
        thingTypeTemplate.setCode("Custom");
        thingTypeTemplate.setName("Custom Thing Type");
        thingTypeTemplate.setDisplayOrder(1);
        thingTypeTemplate.setThingTypeTemplateCategory(categoryService.getByCode(CATEGORY_THING_TYPE_TEMPLATE_CUSTOM));
        thingTypeTemplate.setDescription("Basic Custom Template");
        thingTypeTemplate.setGroup(group);
        thingTypeTemplate.setPathIcon("icon-thingtype-custom");
        ThingTypeTemplateService.getInstance().insert(thingTypeTemplate);
    }

    /**
     * Insert RFID Printer Template
     *
     * @param group group
     */
    public void insertRFIDPrinterTemplate(Group group) {
        ThingTypeTemplateCategoryService categoryService = ThingTypeTemplateCategoryServiceBase.getInstance();
        ThingTypeTemplate thingTypeTemplate = new ThingTypeTemplate();
        thingTypeTemplate.setCode("RFID Printer");
        thingTypeTemplate.setName("RFID Printer");
        thingTypeTemplate.setDescription("RFID PRINTER");
        thingTypeTemplate.setDisplayOrder(4);
        thingTypeTemplate.setThingTypeTemplateCategory(categoryService.getByCode(CATEGORY_THING_TYPE_TEMPLATE_M2M));
        thingTypeTemplate.setGroup(group);
        thingTypeTemplate.setPathIcon("icon-thingtype-rfid-printer");
        ThingTypeTemplate thingTypeTemp = ThingTypeTemplateService.getInstance().insert(thingTypeTemplate);

        insertUdfField("ipAddress", "ipAddress", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemp);
        insertUdfField("port", "port", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemp);
        insertUdfField("proxy", "proxy", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_BOOLEAN), THING_TYPE_DATA_TYPE, false, thingTypeTemp);
        insertUdfField("printerProxyIp", "printerProxyIp", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemp);
        insertUdfField("printerProxyPort", "printerProxyPort", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemp);
        insertUdfField("printerProxyEndPoint", "printerProxyEndPoint", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemp);
        insertUdfField("location", "location", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_LONLATALT), THING_TYPE_DATA_TYPE, false, thingTypeTemp);
        insertUdfField("locationXYZ", "locationXYZ", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_XYZ), THING_TYPE_DATA_TYPE, false, thingTypeTemp);
        insertUdfField("zone", "zone", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_ZONE), THING_TYPE_DATA_TYPE, true, thingTypeTemp);

    }

    /**
     * Insert Logical Reader Template
     *
     * @param group group
     */
    public void insertLogicalReaderTemplate(Group group) {
        ThingTypeTemplateCategoryService categoryService = ThingTypeTemplateCategoryServiceBase.getInstance();
        ThingTypeTemplate thingTypeTemplate = new ThingTypeTemplate();
        thingTypeTemplate.setCode("Logical Reader");
        thingTypeTemplate.setName("Logical Reader");
        thingTypeTemplate.setDescription("Logical Reader");
        thingTypeTemplate.setDisplayOrder(6);
        thingTypeTemplate.setThingTypeTemplateCategory(categoryService.getByCode(CATEGORY_THING_TYPE_TEMPLATE_SENSORS));
        thingTypeTemplate.setGroup(group);
        thingTypeTemplate.setPathIcon("icon-thingtype-logical-reader");
        thingTypeTemplate = ThingTypeTemplateService.getInstance().insert(thingTypeTemplate);

        insertUdfField("location", "location", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_LONLATALT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("locationXYZ", "locationXYZ", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_XYZ), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("zoneIn", "zoneIn", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_ZONE), THING_TYPE_DATA_TYPE, true, thingTypeTemplate);
    }

    /**
     * Insert RFID Tag Template
     *
     * @param group group
     */
    public void insertRFIDTagTemplate(Group group) {
        ThingTypeTemplateCategoryService categoryService = ThingTypeTemplateCategoryServiceBase.getInstance();
        ThingTypeTemplate thingTypeTemplate = new ThingTypeTemplate();
        thingTypeTemplate.setCode("RFID Tag");
        thingTypeTemplate.setName("RFID Tag");
        thingTypeTemplate.setDescription("RFID Tag");
        thingTypeTemplate.setDisplayOrder(1);
        thingTypeTemplate.setThingTypeTemplateCategory(categoryService.getByCode(CATEGORY_THING_TYPE_TEMPLATE_SENSORS));
        thingTypeTemplate.setGroup(group);
        thingTypeTemplate.setPathIcon("icon-thingtype-rfid-tag");
        thingTypeTemplate = ThingTypeTemplateService.getInstance().insert(thingTypeTemplate);

        insertRFIDTagBasicTemplate(thingTypeTemplate);
        insertMandatoryValuesZoneInThingTypeFieldTemplate(thingTypeTemplate, null);
    }

    /**
     * Insert GPS Tag Template
     *
     * @param group group
     */
    public void insertGPSTagTemplate(Group group) {
        ThingTypeTemplateCategoryService categoryService = ThingTypeTemplateCategoryServiceBase.getInstance();
        ThingTypeTemplate thingTypeTemplate = new ThingTypeTemplate();
        thingTypeTemplate.setCode("GPS Tag");
        thingTypeTemplate.setName("GPS Tag");
        thingTypeTemplate.setDescription("GPS Tag");
        thingTypeTemplate.setDisplayOrder(5);
        thingTypeTemplate.setThingTypeTemplateCategory(categoryService.getByCode(CATEGORY_THING_TYPE_TEMPLATE_SENSORS));
        thingTypeTemplate.setGroup(group);
        thingTypeTemplate.setPathIcon("icon-thingtype-gps-tag");
        thingTypeTemplate = ThingTypeTemplateService.getInstance().insert(thingTypeTemplate);

        insertGPSTagBasicTemplate(thingTypeTemplate);
        insertGPSTagNewUdfs(thingTypeTemplate, null);
    }

    /**
     * Insert ZPL Template
     *
     * @param group group
     */
    private void insertZPLTemplate(Group group) {
        ThingTypeTemplateCategoryService categoryService = ThingTypeTemplateCategoryServiceBase.getInstance();
        ThingTypeTemplate thingTypeTemplate = new ThingTypeTemplate();
        thingTypeTemplate.setCode("ZPL");
        thingTypeTemplate.setName("ZPL");
        thingTypeTemplate.setDescription("ZPL");
        thingTypeTemplate.setDisplayOrder(3);
        thingTypeTemplate.setThingTypeTemplateCategory(categoryService.getByCode(CATEGORY_THING_TYPE_TEMPLATE_M2M));
        thingTypeTemplate.setGroup(group);
        thingTypeTemplate.setPathIcon("icon-thingtype-zpl");
        thingTypeTemplate = ThingTypeTemplateService.getInstance().insert(thingTypeTemplate);
        insertUdfField("zpl", "zpl", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_ZPL_SCRIPT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("rfidEncode", "rfidEncode", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_BOOLEAN), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
    }

    /**
     * Populate Thing Type Old Templates
     *
     * @param group
     */
    @Deprecated
    public static void populateThingTypeOldTemplates(Group group) {
        insertCustomTemplateOld(group);
        insertRFIDPrinterOldTemplate(group);
        insertGPIOTemplate(group);
        insertLogicalReaderOldTemplate(group);
        insertRFIDTagOldTemplate(group);
        insertGPSTagOldTemplate(group);
        insertZPLTemplateOld(group);
    }

    /**
     * Insert Custom Template
     *
     * @param group group
     */
    private static void insertCustomTemplateOld(Group group) {
        insertThingTypeTemplateHead("Custom", "Basic Custom Template", group, "sprite template icon-custom", false);
    }

    /**
     * Insert RFID Printer Template with old structure
     *
     * @param group group
     */
    private static void insertRFIDPrinterOldTemplate(Group group) {
        ThingTypeTemplate thingTypeTemplateHead = insertThingTypeTemplateHead("RFID Printer", "RFID PRINTER", group, "sprite template icon-rfidprinter", false);
        insertUdfField("ipAddress", "ipAddress", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("port", "port", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("proxy", "proxy", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_BOOLEAN), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("printerProxyIp", "printerProxyIp", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("printerProxyPort", "printerProxyPort", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("printerProxyEndPoint", "printerProxyEndPoint", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("location", "location", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_LONLATALT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("locationXYZ", "locationXYZ", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_XYZ), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
    }

    /**
     * Insert GPIO Template
     *
     * @param group group
     */
    private static void insertGPIOTemplate(Group group) {
        ThingTypeTemplate thingTypeTemplateHead = insertThingTypeTemplateHead("GPIO", "GPIO", group, "sprite template icon-gpio", false);
        insertUdfField("location", "location", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_LONLATALT.value), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("locationXYZ", "locationXYZ", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_XYZ.value), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
    }

    /**
     * Insert Logical Reader Template with old structure
     *
     * @param group group
     */
    private static void insertLogicalReaderOldTemplate(Group group) {
        ThingTypeTemplate thingTypeTemplateHead = insertThingTypeTemplateHead("Logical Reader", "Logical Reader", group, "sprite template icon-logicalreader", false);
        insertLogicalReaderBasicTemplate(thingTypeTemplateHead);
    }

    /**
     * Insert Logical Reader Basic Template
     *
     * @param thingTypeTemplateHead thingTypeTemplateHead
     */
    private static void insertLogicalReaderBasicTemplate(ThingTypeTemplate thingTypeTemplateHead) {
        insertUdfField("location", "location", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_LONLATALT.value), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("locationXYZ", "locationXYZ", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_XYZ.value), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
    }

    /* Populate StarFlex ThingType Templates */
    public static void polulateStarFlexThingTypeTemplates(Group group) {
        insertSTARflexTemplate(group);
        insertSTARflexConfigTemplate(group);
        insertSTARflexStatusTemplate(group);
    }

    /**
     * Populate Start Flex Thing Type Template's old structure
     *
     * @param group group
     */
    public static void populateStartFlexThingTypeOldTemplate(Group group) {
        insertSTARTflexTemplate(group);
        insertFlexTagOldTemplate(group);
    }

    /* Populate CoreBridge and EdgeBridge ThingType Templates */
    public void polulateBridgeThingTypeTemplates(Group group) {
        insertCoreBridgeTemplate5_0_0RC10(group);
        insertEdgeBridgeTemplate5_0_0RC10(group);
    }

    public static void populateMojixRetailAppThingTypeTemplateBase(Group group) {
        ThingTypeTemplateCategory category = ThingTypeTemplateCategoryService.getInstance().getByCode(CATEGORY_THING_TYPE_TEMPLATE_MOJIX_RETAIL_APP_BASE);
        DataTypeService typeService = DataTypeService.getInstance();
        //
        ThingTypeTemplate thingTypeTemplate;

        //region Item
        thingTypeTemplate = new ThingTypeTemplate();
        thingTypeTemplate.setCode("item");
        thingTypeTemplate.setName("Item");
        thingTypeTemplate.setDescription("Item");
        thingTypeTemplate.setDisplayOrder(1);
        thingTypeTemplate.setAutoCreate(true);
        thingTypeTemplate.setThingTypeTemplateCategory(category);
        thingTypeTemplate.setGroup(group);
        thingTypeTemplate.setPathIcon("icon-thingtype-item");
        thingTypeTemplate = ThingTypeTemplateService.getInstance().insert(thingTypeTemplate);

        insertUdfField("status", "status", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("upcCode", "upcCode", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("source", "source", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("deptCode", "deptSectionCode", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("zone", "zone", typeService.get(ThingTypeField.Type.TYPE_ZONE), THING_TYPE_DATA_TYPE, true, thingTypeTemplate);
        insertUdfField("enode", "enode", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("lastLocateTime", "lastLocateTime", typeService.get(ThingTypeField.Type.TYPE_TIMESTAMP), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("lastDetectTime", "lastDetectTime", typeService.get(ThingTypeField.Type.TYPE_TIMESTAMP), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("location", "location", typeService.get(ThingTypeField.Type.TYPE_LONLATALT), THING_TYPE_DATA_TYPE, true, thingTypeTemplate);
        insertUdfField("locationXYZ", "locationXYZ", typeService.get(ThingTypeField.Type.TYPE_XYZ), THING_TYPE_DATA_TYPE, true, thingTypeTemplate);
        insertUdfField("logicalReader", "logicalReader", typeService.get(ThingTypeField.Type.TYPE_LOGICAL_READER), THING_TYPE_DATA_TYPE, true, thingTypeTemplate);
        insertUdfField("doorEvent", "doorEvent", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("skuCode", "skuCode", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        //endregion

        // region UPC
        thingTypeTemplate = new ThingTypeTemplate();
        thingTypeTemplate.setCode("upc");
        thingTypeTemplate.setName("UPC");
        thingTypeTemplate.setDescription("UPC");
        thingTypeTemplate.setDisplayOrder(2);
        thingTypeTemplate.setThingTypeTemplateCategory(category);
        thingTypeTemplate.setGroup(group);
        thingTypeTemplate.setPathIcon("icon-thingtype-upc");
        thingTypeTemplate = ThingTypeTemplateService.getInstance().insert(thingTypeTemplate);

        insertUdfField("price", "price", "dollar", "$", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate, "");
        insertUdfField("size", "size", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("min", "min", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("max", "max", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("brand", "brand", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("material", "material", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);

        insertUdfField("color", "color", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        if (typeService.get(ThingTypeField.Type.TYPE_COLOR) != null) {
            insertUdfField("colorHexadecimal", "colorHexadecimal", typeService.get(ThingTypeField.Type.TYPE_COLOR), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        } else {
            insertUdfField("colorHexadecimal", "colorHexadecimal", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        }
        insertUdfField("upcCategoryCode", "upcCategoryCode", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        // endregion

        // region SKU
        thingTypeTemplate = new ThingTypeTemplate();
        thingTypeTemplate.setCode("sku");
        thingTypeTemplate.setName("SKU");
        thingTypeTemplate.setDescription("SKU");
        thingTypeTemplate.setDisplayOrder(3);
        thingTypeTemplate.setThingTypeTemplateCategory(category);
        thingTypeTemplate.setGroup(group);
        thingTypeTemplate.setPathIcon("icon-thingtype-sku");
        thingTypeTemplate = ThingTypeTemplateService.getInstance().insert(thingTypeTemplate);

        insertUdfField("price", "price", "dollar", "$", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate, "");
        insertUdfField("size", "size", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("min", "min", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("max", "max", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("brand", "brand", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("material", "material", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("color", "color", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("colorHexadecimal", "colorHexadecimal", typeService.get(ThingTypeField.Type.TYPE_COLOR), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("skuCategoryCode", "skuCategoryCode", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        // endregion

        // region UPC Category
        thingTypeTemplate = new ThingTypeTemplate();
        thingTypeTemplate.setCode("upcCategory");
        thingTypeTemplate.setName("UPC Category");
        thingTypeTemplate.setDescription("UPC Category");
        thingTypeTemplate.setDisplayOrder(4);
        thingTypeTemplate.setThingTypeTemplateCategory(category);
        thingTypeTemplate.setGroup(group);
        thingTypeTemplate.setPathIcon("icon-thingtype-product-sub-category");
        thingTypeTemplate = ThingTypeTemplateService.getInstance().insert(thingTypeTemplate);
        insertUdfField("frontImage", "frontImage", typeService.get(ThingTypeField.Type.TYPE_IMAGE_ID), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("backImage", "backImage", typeService.get(ThingTypeField.Type.TYPE_IMAGE_ID), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("sideImage", "sideImage", typeService.get(ThingTypeField.Type.TYPE_IMAGE_ID), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        // endregion

        // region SKU Category
        thingTypeTemplate = new ThingTypeTemplate();
        thingTypeTemplate.setCode("skuCategory");
        thingTypeTemplate.setName("SKU Category");
        thingTypeTemplate.setDescription("SKU Category");
        thingTypeTemplate.setDisplayOrder(5);
        thingTypeTemplate.setThingTypeTemplateCategory(category);
        thingTypeTemplate.setGroup(group);
        thingTypeTemplate.setPathIcon("icon-thingtype-sku-category");
        thingTypeTemplate = ThingTypeTemplateService.getInstance().insert(thingTypeTemplate);

        insertUdfField("frontImage", "frontImage", typeService.get(ThingTypeField.Type.TYPE_IMAGE_ID), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("backImage", "backImage", typeService.get(ThingTypeField.Type.TYPE_IMAGE_ID), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("sideImage", "sideImage", typeService.get(ThingTypeField.Type.TYPE_IMAGE_ID), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        // endregion

        // region Department
        thingTypeTemplate = new ThingTypeTemplate();
        thingTypeTemplate.setCode("dept");
        thingTypeTemplate.setName("Department");
        thingTypeTemplate.setDescription("Department");
        thingTypeTemplate.setDisplayOrder(6);
        thingTypeTemplate.setThingTypeTemplateCategory(category);
        thingTypeTemplate.setGroup(group);
        thingTypeTemplate.setPathIcon("icon-thingtype-department");
        thingTypeTemplate = ThingTypeTemplateService.getInstance().insert(thingTypeTemplate);
        insertUdfField("icon", "icon", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("departmentGroup", "departmentGroup", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("departmentGroupIcon", "departmentGroupIcon", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("departmentGroupName", "departmentGroupName", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("departmentSubGroup", "departmentSubGroup", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("departmentSubGroupIcon", "departmentSubGroupIcon", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("departmentSubGroupName", "departmentSubGroupName", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);

        // endregion

    }

    public static void populateMojixRetailAppThingTypeTemplateSync(Group group) {
        ThingTypeTemplateCategory category = ThingTypeTemplateCategoryService.getInstance().getByCode(CATEGORY_THING_TYPE_TEMPLATE_MOJIX_RETAIL_APP_SYNC);
        DataTypeService typeService = DataTypeService.getInstance();
        ThingTypeTemplate thingTypeTemplate;

        //region Replenishment
        thingTypeTemplate = new ThingTypeTemplate();
        thingTypeTemplate.setCode("replenishment");
        thingTypeTemplate.setName("Replenishment");
        thingTypeTemplate.setDescription("Replenishment");
        thingTypeTemplate.setDisplayOrder(1);
        thingTypeTemplate.setThingTypeTemplateCategory(category);
        thingTypeTemplate.setGroup(group);
        thingTypeTemplate.setPathIcon("icon-thingtype-replenishment");
        thingTypeTemplate = ThingTypeTemplateService.getInstance().insert(thingTypeTemplate);

        insertUdfField("itemDepartmentCategory", "itemDepartmentCategory", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("itemColor", "itemColor", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("itemPrice", "itemPrice", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("itemSize", "itemSize", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("itemStoreCode", "itemStoreCode", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("itemStoreName", "itemStoreName", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("itemUPCCategory", "itemUPCCategory", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("itemUpc", "itemUpc", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishBackCount", "replenishBackCount", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishBackZone", "replenishBackZone", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishBackZoneId", "replenishBackZoneId", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishDepartmentCode", "replenishDepartmentCode", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishDepartmentName", "replenishDepartmentName", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishFrontCount", "replenishFrontCount", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishFrontZone", "replenishFrontZone", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishFrontZoneId", "replenishFrontZoneId", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishLastDate", "replenishLastDate", typeService.get(ThingTypeField.Type.TYPE_DATE), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishMax", "replenishMax", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishMin", "replenishMin", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishQuantity", "replenishQuantity", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishStatus", "replenishStatus", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishUser", "replenishUser", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishQuantityDone", "replenishQuantityDone", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        //endregion

        // region Hot Replenishment
        thingTypeTemplate = new ThingTypeTemplate();
        thingTypeTemplate.setCode("hotReplenishment");
        thingTypeTemplate.setName("Hot Replenishment");
        thingTypeTemplate.setDescription("Hot Replenishment");
        thingTypeTemplate.setDisplayOrder(2);
        thingTypeTemplate.setThingTypeTemplateCategory(category);
        thingTypeTemplate.setGroup(group);
        thingTypeTemplate.setPathIcon("icon-thingtype-hot-replenishment");
        thingTypeTemplate = ThingTypeTemplateService.getInstance().insert(thingTypeTemplate);

        insertUdfField("itemDepartmentCategory", "itemDepartmentCategory", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("itemColor", "itemColor", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("itemPrice", "itemPrice", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("itemSize", "itemSize", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("itemStoreCode", "itemStoreCode", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("itemStoreName", "itemStoreName", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("itemUPCCategory", "itemUPCCategory", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("itemUpc", "itemUpc", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishBackCount", "replenishBackCount", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishBackZone", "replenishBackZone", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishBackZoneId", "replenishBackZoneId", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishBoxCount", "replenishBoxCount", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishDepartmentCode", "replenishDepartmentCode", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishDepartmentName", "replenishDepartmentName", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishFrontCount", "replenishFrontCount", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishFrontZone", "replenishFrontZone", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishFrontZoneId", "replenishFrontZoneId", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishLastDate", "replenishLastDate", typeService.get(ThingTypeField.Type.TYPE_DATE), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishMax", "replenishMax", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishMin", "replenishMin", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishQuantity", "replenishQuantity", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishStatus", "replenishStatus", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishUser", "replenishUser", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishQuantityDone", "replenishQuantityDone", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        // endregion

        // region Sell Thru Replenishment
        thingTypeTemplate = new ThingTypeTemplate();
        thingTypeTemplate.setCode("sellThruReplenishment");
        thingTypeTemplate.setName("Sell Thru Replenishment");
        thingTypeTemplate.setDescription("Sell Thru Replenishment");
        thingTypeTemplate.setDisplayOrder(3);
        thingTypeTemplate.setThingTypeTemplateCategory(category);
        thingTypeTemplate.setGroup(group);
        thingTypeTemplate.setPathIcon("icon-thingtype-sell-thru-rep");
        thingTypeTemplate = ThingTypeTemplateService.getInstance().insert(thingTypeTemplate);

        // endregion

        // region DressingRoom
        thingTypeTemplate = new ThingTypeTemplate();
        thingTypeTemplate.setCode("dressingRoom");
        thingTypeTemplate.setName("Dressing Room Clean Up");
        thingTypeTemplate.setDescription("Dressing Room");
        thingTypeTemplate.setDisplayOrder(3);
        thingTypeTemplate.setThingTypeTemplateCategory(category);
        thingTypeTemplate.setGroup(group);
        thingTypeTemplate.setPathIcon("icon-thingtype-dressingroom-cleanup");
        thingTypeTemplate = ThingTypeTemplateService.getInstance().insert(thingTypeTemplate);

        insertUdfField("itemDepartmentCategory", "itemDepartmentCategory", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("itemColor", "itemColor", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("itemPrice", "itemPrice", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("itemSize", "itemSize", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("itemStoreCode", "itemStoreCode", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("itemStoreName", "itemStoreName", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("itemUPCCategory", "itemUPCCategory", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("itemUpc", "itemUpc", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishDressingCount", "replenishDressingCount", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishDressingZone", "replenishDressingZone", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishDressingZoneId", "replenishDressingZoneId", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishDepartmentCode", "replenishDepartmentCode", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishDepartmentName", "replenishDepartmentName", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishFrontCount", "replenishFrontCount", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishFrontZone", "replenishFrontZone", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishFrontZoneId", "replenishFrontZoneId", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishLastDate", "replenishLastDate", typeService.get(ThingTypeField.Type.TYPE_DATE), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishQuantity", "replenishQuantity", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishStatus", "replenishStatus", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishUser", "replenishUser", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("itemHexadecimal", "itemHexadecimal", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishMin", "replenishMin", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishMax", "replenishMax", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        // endregion

    }

    public static void populateMojixRetailAppThingTypeTemplateConfig(Group group) {
        ThingTypeTemplateCategory category = ThingTypeTemplateCategoryService.getInstance().getByCode(CATEGORY_THING_TYPE_TEMPLATE_MOJIX_RETAIL_APP_CONFIG);
        DataTypeService typeService = DataTypeService.getInstance();
        ThingTypeTemplate thingTypeTemplate;

        // region Retail App Config
        thingTypeTemplate = new ThingTypeTemplate();
        thingTypeTemplate.setCode("retailAppConfig");
        thingTypeTemplate.setName("Mobile App Config");
        thingTypeTemplate.setDescription("Retail App Configuration");
        thingTypeTemplate.setDisplayOrder(1);
        thingTypeTemplate.setThingTypeTemplateCategory(category);
        thingTypeTemplate.setGroup(group);
        thingTypeTemplate.setPathIcon("icon-thingtype-mobile-config");
        thingTypeTemplate = ThingTypeTemplateService.getInstance().insert(thingTypeTemplate);

        insertUdfField("homeTimeRefresh", "homeTimeRefresh", "", "", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate, "10000");
        insertUdfField("homeTotalsReport", "homeTotalsReport", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("dressingRoomReport", "dressingRoomReport", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("dressingRoomReportAllDetail", "dressingRoomReportAllDetail", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("dressingRoomReportDetail", "dressingRoomReportDetail", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("hotReplenishReport", "hotReplenishReport", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("hotReplenishDetailReport", "hotReplenishDetailReport", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishReport", "replenishReport", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishDetailReport", "replenishDetailReport", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishGroupsReport", "replenishGroupsReport", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("sellThruReplenishReport", "sellThruReplenishReport", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("imageHost", "imageHost", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("limitDataCanBeLoaded", "limitDataCanBeLoaded", "", "", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate, "100");
        insertUdfField("pageNumberDefault", "pageNumberDefault", "", "", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate, "0");
        insertUdfField("pageSizeDefault", "pageSizeDefault", "", "", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate, "20");
        insertUdfField("statusDone", "statusDone", "", "", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate, "Done");
        insertUdfField("statusPartial", "statusPartial", "", "", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate, "Partial");
        insertUdfField("thingGroup", "thingGroup", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("thingTypeCodeReplenishment", "thingTypeCodeReplenishment", "", "", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate, "replenishment");
        insertUdfField("thingTypeCodeDressingRoom", "thingTypeCodeDressingRoom", "", "", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate, "dressingRoom");
        insertUdfField("thingTypeCodeHotReplenishment", "thingTypeCodeHotReplenishment", "", "", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate, "hotReplenishment");
        insertUdfField("thingTypeCodePriority", "thingTypeCodePriority", "", "", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate, "PriorityConfiguration");
        insertUdfField("timeDonePopUp", "timeDonePopUp", "", "", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate, "1000");
        insertUdfField("timeRefresh", "timeRefresh", "", "", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate, "20000");
        //endregion

        // region User Settings Config
        thingTypeTemplate = new ThingTypeTemplate();
        thingTypeTemplate.setCode("userSettings");
        thingTypeTemplate.setName("User Settings Config");
        thingTypeTemplate.setDescription("User Settings");
        thingTypeTemplate.setDisplayOrder(2);
        thingTypeTemplate.setThingTypeTemplateCategory(category);
        thingTypeTemplate.setGroup(group);
        thingTypeTemplate.setPathIcon("icon-thingtype-user-settings");
        thingTypeTemplate = ThingTypeTemplateService.getInstance().insert(thingTypeTemplate);

        insertUdfField("username", "username", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("departmentCode", "retailParentGroup", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("departmentName", "retailParentGroupName", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("storeName", "store", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("storeCode", "storeCode", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        // endregion

        // region Pick list priority config
        thingTypeTemplate = new ThingTypeTemplate();
        thingTypeTemplate.setCode("priorityConfig");
        thingTypeTemplate.setName("Pick list Priority Config");
        thingTypeTemplate.setDescription("Priority Configuration");
        thingTypeTemplate.setDisplayOrder(3);
        thingTypeTemplate.setThingTypeTemplateCategory(category);
        thingTypeTemplate.setGroup(group);
        thingTypeTemplate.setPathIcon("icon-thingtype-picklist-priority");
        thingTypeTemplate = ThingTypeTemplateService.getInstance().insert(thingTypeTemplate);

        insertUdfField("icon", "icon", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("start", "start", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("type", "type", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("status", "status", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("label", "label", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("end", "end", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        // endregion

        // region Retail Things Config
        thingTypeTemplate = new ThingTypeTemplate();
        thingTypeTemplate.setCode("retailThingsConfig");
        thingTypeTemplate.setName("Retail Things Config");
        thingTypeTemplate.setDescription("Retail Things Configuration");
        thingTypeTemplate.setDisplayOrder(4);
        thingTypeTemplate.setThingTypeTemplateCategory(category);
        thingTypeTemplate.setGroup(group);
        thingTypeTemplate.setPathIcon("icon-thingtype-things-config");
        thingTypeTemplate = ThingTypeTemplateService.getInstance().insert(thingTypeTemplate);

        insertUdfField("fromThingType", "from ThingType", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("fromThingTypeUDF", "from ThingType UDF", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("toThingType", "to ThingType", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("toThingTypeUDF", "to ThingType UDF", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        // endregion

        // region Retail Things Config
        thingTypeTemplate = new ThingTypeTemplate();
        thingTypeTemplate.setCode("tilesConfiguration");
        thingTypeTemplate.setName("Tiles Configuration");
        thingTypeTemplate.setDescription("Home Tiles Configuration");
        thingTypeTemplate.setDisplayOrder(5);
        thingTypeTemplate.setThingTypeTemplateCategory(category);
        thingTypeTemplate.setGroup(group);
        thingTypeTemplate.setPathIcon("icon-thingtype-mobile-tiles-config");
        thingTypeTemplate = ThingTypeTemplateService.getInstance().insert(thingTypeTemplate);

        insertUdfField("resource", "Target of Resource", typeService.get(ThingTypeField.Type.TYPE_TEXT),
                THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("backgroundColor", "Background Color", typeService.get(ThingTypeField.Type.TYPE_COLOR),
                THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("textColor", "Text Color", typeService.get(ThingTypeField.Type.TYPE_COLOR),
                THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("badgeColor", "Badge Color", typeService.get(ThingTypeField.Type.TYPE_COLOR),
                THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("badgeTextColor", "Badge Text Color", typeService.get(ThingTypeField.Type.TYPE_COLOR),
                THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("order", "Order/Position of Tile", typeService.get(ThingTypeField.Type.TYPE_NUMBER),
                THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        // endregion

    }

    /**
     * Insert Mandatory Values Zone in Thing Type Field Template
     *
     * @param thingTypeTemplate thing Type Template
     */
    public static void insertMandatoryValuesZoneInThingTypeFieldTemplate(ThingTypeTemplate thingTypeTemplate, Set<String> labels) {
        if ((labels == null) || (!labels.contains("source"))) {
            insertUdfField("source", "source", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        }
        if ((labels == null) || (!labels.contains("logicalReader"))) {
            insertUdfField("logicalReader", "logicalReader", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_LOGICAL_READER), THING_TYPE_DATA_TYPE, true, thingTypeTemplate);
        }
        if ((labels == null) || (!labels.contains("tsCoreIn"))) {
            insertUdfField("tsCoreIn", "tsCoreIn", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TIMESTAMP), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        }
        if ((labels == null) || (!labels.contains("tsEdgeIn"))) {
            insertUdfField("tsEdgeIn", "tsEdgeIn", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TIMESTAMP), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        }
        if ((labels == null) || (!labels.contains("doorEvent"))) {
            insertUdfField("doorEvent", "doorEvent", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        }
    }

    /**
     * Insert a new Udf Field
     *
     * @param udfName           udf Name
     * @param udfDescription    udf Description
     * @param udfUnit           udf Unit
     * @param udfSymbol         udf Symbol
     * @param udfType           udf Type
     * @param udfTypeParent     udf Type Parent
     * @param udfTimeSeries     udf TimeSeries
     * @param thingTypeTemplate thing Type Template
     * @param defaultValue      default value
     */
    public static ThingTypeFieldTemplate insertUdfField(String udfName, String udfDescription, String udfUnit, String udfSymbol, DataType udfType,
                                                        String udfTypeParent, boolean udfTimeSeries, ThingTypeTemplate thingTypeTemplate, String defaultValue) {
        return ThingTypeFieldTemplateService.getInstance().create(udfName, udfDescription, udfUnit, udfSymbol, udfType, udfTypeParent, udfTimeSeries, thingTypeTemplate, defaultValue);
    }

    /**
     * Insert a new Udf Field, with unit, symbol and udf value empty
     *
     * @param udfName           udf name
     * @param udfDescription    udf description
     * @param udfType           udf Type
     * @param udfTypeParent     udf Type Parent
     * @param udfTimeSeries     udf TimeSeries
     * @param thingTypeTemplate thing Type Template
     * @return
     */
    public static ThingTypeFieldTemplate insertUdfField(String udfName, String udfDescription, DataType udfType,
                                                        String udfTypeParent, boolean udfTimeSeries, ThingTypeTemplate thingTypeTemplate) {
        return insertUdfField(udfName, udfDescription, "", "", udfType, udfTypeParent, udfTimeSeries, thingTypeTemplate, "");
    }

    /**
     * Insert a new Thing Type Template <br />
     * Deprecated Version 5.0.0RC_10
     *
     * @param templateName        thing Type Template's name
     * @param templateDescription thing Type Template's description
     * @param templateGroup       thing Type Template's  group
     * @param templatePathIcon    thing Type Template's path icon
     * @param templateAutoCreate  thing Type Template's auto create
     * @return a Thing Type Template
     */
    @Deprecated
    public static ThingTypeTemplate insertThingTypeTemplateHead(String templateName, String templateDescription, Group templateGroup, String templatePathIcon, boolean templateAutoCreate) {
        ThingTypeTemplate thingTypeTemplate = new ThingTypeTemplate();
        thingTypeTemplate.setName(templateName);
        thingTypeTemplate.setDescription(templateDescription);
        thingTypeTemplate.setGroup(templateGroup);
        thingTypeTemplate.setPathIcon(templatePathIcon);
        thingTypeTemplate.setAutoCreate(templateAutoCreate);
        return ThingTypeTemplateService.getInstance().insert(thingTypeTemplate);
    }

    /**
     * Insert  Logical Reader's new udfs
     *
     * @param thingTypeTemplateHead thing Type Template
     */
    public static void insertLogicalReaderNewUdfs(ThingTypeTemplate thingTypeTemplateHead, Set<String> labelsThingTypeFieldTemplate) {
        if ((labelsThingTypeFieldTemplate == null) || (!labelsThingTypeFieldTemplate.isEmpty() && !labelsThingTypeFieldTemplate.contains("zoneIn"))) {
            insertUdfField("zoneIn", "zoneIn", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_ZONE.value), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        }
    }

    /**
     * Insert RFID Tag Template old structure
     *
     * @param group group
     */
    private static void insertRFIDTagOldTemplate(Group group) {
        ThingTypeTemplate thingTypeTemplateHead = insertThingTypeTemplateHead("RFID Tag", "RFID Tag", group, "sprite template icon-rfidtag", false);
        insertRFIDTagBasicTemplate(thingTypeTemplateHead);
    }

    /**
     * Insert RFID Tag Basic Template
     *
     * @param thingTypeTemplateHead thingTypeTemplateHead
     */
    private static void insertRFIDTagBasicTemplate(ThingTypeTemplate thingTypeTemplateHead) {
        insertUdfField("location", "location", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_LONLATALT.value), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("locationXYZ", "locationXYZ", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_XYZ.value), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("zone", "zone", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_ZONE.value), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("lastDetectTime", "lastDetectTime", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TIMESTAMP.value), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("lastLocateTime", "lastLocateTime", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TIMESTAMP.value), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
    }


    /**
     * Insert GPS Tag's new udfs
     *
     * @param thingTypeTemplateHead thing Type Template
     */
    public static void insertGPSTagNewUdfs(ThingTypeTemplate thingTypeTemplateHead, Set<String> labels) {
        insertMandatoryValuesZoneInThingTypeFieldTemplate(thingTypeTemplateHead, labels);
        if ((labels == null) || (!labels.contains("zone"))) {
            insertUdfField("zone", "zone", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_ZONE.value), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        }
        if ((labels == null) || (!labels.contains("lastDetectTime"))) {
            insertUdfField("lastDetectTime", "lastDetectTime", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TIMESTAMP.value), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        }
        if ((labels == null) || (!labels.contains("lastLocateTime"))) {
            insertUdfField("lastLocateTime", "lastLocateTime", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TIMESTAMP.value), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        }
    }

    /**
     * @param group group
     */
    private static void insertGPSTagOldTemplate(Group group) {
        ThingTypeTemplate thingTypeTemplateHead = insertThingTypeTemplateHead("GPS Tag", "GPS Tag", group, "sprite template icon-gpstag", false);
        insertGPSTagBasicTemplate(thingTypeTemplateHead);
    }

    /**
     * Insert GPS Tag Basic Template
     *
     * @param thingTypeTemplateHead thingTypeTemplateHead
     */
    private static void insertGPSTagBasicTemplate(ThingTypeTemplate thingTypeTemplateHead) {
        insertUdfField("location", "location", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_LONLATALT.value), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("locationXYZ", "locationXYZ", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_XYZ.value), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
    }

    /**
     * Insert ZPL Template
     *
     * @param group group
     */
    private static void insertZPLTemplateOld(Group group) {
        ThingTypeTemplate thingTypeTemplateHead = insertThingTypeTemplateHead("ZPL", "ZPL", group, "sprite template icon-zpl", false);
        insertUdfField("zpl", "zpl", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_ZPL_SCRIPT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead);
        insertUdfField("rfidEncode", "rfidEncode", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_BOOLEAN), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead);
    }

    /**
     * Insert START flex Template
     *
     * @param group group
     */
    public static void insertSTARTflexTemplate(Group group) {
        ThingTypeTemplate thingTypeTemplateHead = insertThingTypeTemplateHead("STARflex", "STARflex", group, "sprite template icon-starflex", false);
        //TODO: starflex https://docs.google.com/spreadsheets/d/1qFrYCN0Zjd8-q4Fn0y_tnPPjXMSRC_N7upnodSA8lp8/edit#gid=0
        DataTypeService service = DataTypeService.getInstance();
        insertUdfField("ts_ms", "ts_ms", "", "", service.get(ThingTypeField.Type.TYPE_TIMESTAMP.value), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("tz", "tz", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("wallclock", "wallclock", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("upTime_sec", "upTime_sec", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("totMem", "totMem", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("freeMem", "freeMem", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("hostName", "hostName", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("macId", "macId", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("releaseLabel", "releaseLabel", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("firmwareVersion", "firmwareVersion", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("apiVersion", "apiVersion", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("deviceType", "deviceType", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("webServerVersion", "webServerVersion", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("netIfs", "netIfs", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("netIfs--lo__0__--address", "netIfs--lo__0__--address", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("netIfs--lo__0__--family", "netIfs--lo__0__--family", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("netIfs--lo__0__--internal", "netIfs--lo__0__--internal", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("netIfs--lo__1__--address", "netIfs--lo__1__--address", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("netIfs--lo__1__--family", "netIfs--lo__1__--family", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("netIfs--lo__1__--internal", "netIfs--lo__1__--internal", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("netIfs--eth0__0__--address", "netIfs--eth0__0__--address", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("netIfs--eth0__0__--family", "netIfs--eth0__0__--family", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("netIfs--eth0__0__--internal", "netIfs--eth0__0__--internal", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("netIfs--eth0__1__--address", "netIfs--eth0__1__--address", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("netIfs--eth0__1__--family", "netIfs--eth0__1__--family", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("netIfs--eth0__1__--internal", "netIfs--eth0__1__--internal", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("netIfs--eth0:1__0__--address", "netIfs--eth0:1__0__--address", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("netIfs--eth0:1__0__--family", "netIfs--eth0:1__0__--family", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("netIfs--eth0:1__0__--internal", "netIfs--eth0:1__0__--internal", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("latestMQTTClientMsg", "latestMQTTClientMsg", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("latestMQTTClientMsg--level", "latestMQTTClientMsg--level", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("latestMQTTClientMsg--msg", "latestMQTTClientMsg--msg", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("latestMQTTClientMsg--ts_ms", "latestMQTTClientMsg--ts_ms", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("latestMQTTClientMsg--metaData--type", "latestMQTTClientMsg--metaData--type", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("latestMQTTClientMsg--metaData--myID", "latestMQTTClientMsg--metaData--myID", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("jurisdiction", "jurisdiction", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("cpuUtilization", "cpuUtilization", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("ledStatus", "ledStatus", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("freeFlash", "freeFlash", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("state", "state", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("activeRFIDProgramName", "activeRFIDProgramName", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("bridge_code", "bridge_code", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("bridge_tag_code", "bridge_tag_code", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("cpuUtilPercent", "cpuUtilPercent", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("loadAvg_0_", "loadAvg_0_", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("loadAvg_1_", "loadAvg_1_", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("loadAvg_2_", "loadAvg_2_", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("metaData_type", "metaData_type", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("metaData_myID", "metaData_myID", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("temperature_c_", "temperature_c_", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("temperature_f_", "temperature_f_", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("discoveryDate", "discoveryDate", "", "", service.get(ThingTypeField.Type.TYPE_DATE), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("claimDate", "claimDate", "", "", service.get(ThingTypeField.Type.TYPE_DATE), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("modifiedDate", "modifiedDate", "", "", service.get(ThingTypeField.Type.TYPE_DATE), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("mqtt_subscription_name", "mqtt_subscription_name", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("mqtt_subscription_body", "mqtt_subscription_body", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("mqtt_host", "mqtt_host", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "starflex.mojix.com");
        insertUdfField("mqtt_port", "mqtt_port", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "1883");
        insertUdfField("mqtt_username", "mqtt_username", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("mqtt_password", "mqtt_password", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("mqtt_topic_data", "mqtt_topic_data", "", "", service.get(ThingTypeField.Type.TYPE_FORMULA), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "/v1/flex/${serialNumber}/data");
        insertUdfField("mqtt_topic_interesting_events", "mqtt_topic_interesting_events", "", "", service.get(ThingTypeField.Type.TYPE_FORMULA), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "/v1/flex/${serialNumber}/interesting_events");
        insertUdfField("mqtt_topic_req", "mqtt_topic_req", "", "", service.get(ThingTypeField.Type.TYPE_FORMULA), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "/v1/flex/${serialNumber}/request");
        insertUdfField("mqtt_topic_res", "mqtt_topic_res", "", "", service.get(ThingTypeField.Type.TYPE_FORMULA), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "/v1/flex/${serialNumber}/response");
        insertUdfField("topology", "topology", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("topology--ports__0__--PORT_1", "topology--ports__0__--PORT_1", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("topology--ports__1__--PORT_2", "topology--ports__1__--PORT_2", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("topology--ports__2__--PORT_3", "topology--ports__2__--PORT_3", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("topology--ports__3__--PORT_4", "topology--ports__3__--PORT_4", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("topology--devices__0__--type", "topology--devices__0__--type", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("topology--devices__0__--id", "topology--devices__0__--id", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("topology--devices__0__--enode", "topology--devices__0__--enode", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("topology--devices__0__--bsp", "topology--devices__0__--bsp", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("topology--devices__1__--type", "topology--devices__1__--type", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("topology--devices__1__--id", "topology--devices__1__--id", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("topology--devices__1__--enode", "topology--devices__1__--enode", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("topology--devices__1__--bsp", "topology--devices__1__--bsp", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("topology--devices__2__--type", "topology--devices__2__--type", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("topology--devices__2__--id", "topology--devices__2__--id", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("topology--devices__2__--enode", "topology--devices__2__--enode", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("topology--devices__2__--bsp", "topology--devices__2__--bsp", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("license", "license", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("license__0__--featurePack", "license__0__--featurePack", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("license__1__--featurePack", "license__1__--featurePack", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("license__1__--options--expire", "license__1__--options--expire", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("license__1__--options--upgrade", "license__1__--options--upgrade", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("license__2__--featurePack", "license__2__--featurePack", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("license__2__--options--maxDoors", "license__2__--options--maxDoors", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
    }

    /**
     * Insert STARflex Config Template
     *
     * @param group group
     */
    public static void insertSTARflexConfigTemplate(Group group) {
        ThingTypeTemplateCategoryService categoryService = ThingTypeTemplateCategoryService.getInstance();
        ThingTypeTemplate thingTypeTemplateHead = new ThingTypeTemplate();
        thingTypeTemplateHead.setCode(TT_STARflex_CONFIG_NAME);
        thingTypeTemplateHead.setName(TT_STARflex_CONFIG_NAME);
        thingTypeTemplateHead.setDescription(TT_STARflex_CONFIG_NAME);
        thingTypeTemplateHead.setDisplayOrder(2);
        thingTypeTemplateHead.setThingTypeTemplateCategory(categoryService.getByCode(CATEGORY_THING_TYPE_TEMPLATE_SENSORS));
        thingTypeTemplateHead.setGroup(group);
        thingTypeTemplateHead.setPathIcon("icon-thingtype-star");
        thingTypeTemplateHead = ThingTypeTemplateService.getInstance().insert(thingTypeTemplateHead);

        DataTypeService dataTypeService = DataTypeService.getInstance();
        insertUdfField("hostName", "hostName", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("macId", "macId", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("releaseLabel", "releaseLabel", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("firmwareVersion", "firmwareVersion", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("apiVersion", "apiVersion", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("deviceType", "deviceType", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("webServerVersion", "webServerVersion", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("netIfs", "netIfs", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("netIfs--lo__0__--address", "netIfs--lo__0__--address", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("netIfs--lo__0__--family", "netIfs--lo__0__--family", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("netIfs--lo__0__--internal", "netIfs--lo__0__--internal", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("netIfs--lo__1__--address", "netIfs--lo__1__--address", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("netIfs--lo__1__--family", "netIfs--lo__1__--family", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("netIfs--lo__1__--internal", "netIfs--lo__1__--internal", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("netIfs--eth0__0__--address", "netIfs--eth0__0__--address", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("netIfs--eth0__0__--family", "netIfs--eth0__0__--family", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("netIfs--eth0__0__--internal", "netIfs--eth0__0__--internal", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("netIfs--eth0__1__--address", "netIfs--eth0__1__--address", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("netIfs--eth0__1__--family", "netIfs--eth0__1__--family", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("netIfs--eth0__1__--internal", "netIfs--eth0__1__--internal", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("netIfs--eth0:1__0__--address", "netIfs--eth0:1__0__--address", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("netIfs--eth0:1__0__--family", "netIfs--eth0:1__0__--family", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("netIfs--eth0:1__0__--internal", "netIfs--eth0:1__0__--internal", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("latestMQTTClientMsg", "latestMQTTClientMsg", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("latestMQTTClientMsg--level", "latestMQTTClientMsg--level", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("latestMQTTClientMsg--msg", "latestMQTTClientMsg--msg", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("latestMQTTClientMsg--ts_ms", "latestMQTTClientMsg--ts_ms", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("latestMQTTClientMsg--metaData--type", "latestMQTTClientMsg--metaData--type", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("latestMQTTClientMsg--metaData--myID", "latestMQTTClientMsg--metaData--myID", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("jurisdiction", "jurisdiction", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("cpuUtilization", "cpuUtilization", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("ledStatus", "ledStatus", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("freeFlash", "freeFlash", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("state", "state", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("activeRFIDProgramName", "activeRFIDProgramName", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("bridge_code", "bridge_code", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("bridge_tag_code", "bridge_tag_code", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("metaData_type", "metaData_type", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("discoveryDate", "discoveryDate", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_DATE), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("claimDate", "claimDate", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_DATE), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("modifiedDate", "modifiedDate", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_DATE), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("mqtt_subscription_name", "mqtt_subscription_name", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("mqtt_subscription_body", "mqtt_subscription_body", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("mqtt_host", "mqtt_host", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "starflex.mojix.com");
        insertUdfField("mqtt_port", "mqtt_port", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "1883");
        insertUdfField("mqtt_username", "mqtt_username", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("mqtt_password", "mqtt_password", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("mqtt_topic_data", "mqtt_topic_data", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_FORMULA), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "/v1/flex/${serialNumber}/data");
        insertUdfField("mqtt_topic_interesting_events", "mqtt_topic_interesting_events", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_FORMULA), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "/v1/flex/${serialNumber}/interesting_events");
        insertUdfField("mqtt_topic_req", "mqtt_topic_req", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_FORMULA), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "/v1/flex/${serialNumber}/request");
        insertUdfField("mqtt_topic_res", "mqtt_topic_res", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_FORMULA), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "/v1/flex/${serialNumber}/response");
        insertUdfField("topology", "topology", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("topology--ports__0__--PORT_1", "topology--ports__0__--PORT_1", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("topology--ports__1__--PORT_2", "topology--ports__1__--PORT_2", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("topology--ports__2__--PORT_3", "topology--ports__2__--PORT_3", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("topology--ports__3__--PORT_4", "topology--ports__3__--PORT_4", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("topology--devices__0__--type", "topology--devices__0__--type", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("topology--devices__0__--id", "topology--devices__0__--id", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("topology--devices__0__--enode", "topology--devices__0__--enode", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("topology--devices__0__--bsp", "topology--devices__0__--bsp", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("topology--devices__1__--type", "topology--devices__1__--type", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("topology--devices__1__--id", "topology--devices__1__--id", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("topology--devices__1__--enode", "topology--devices__1__--enode", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("topology--devices__1__--bsp", "topology--devices__1__--bsp", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("topology--devices__2__--type", "topology--devices__2__--type", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("topology--devices__2__--id", "topology--devices__2__--id", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("topology--devices__2__--enode", "topology--devices__2__--enode", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("topology--devices__2__--bsp", "topology--devices__2__--bsp", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("license", "license", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("license__0__--featurePack", "license__0__--featurePack", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("license__1__--featurePack", "license__1__--featurePack", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("license__1__--options--expire", "license__1__--options--expire", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("license__1__--options--upgrade", "license__1__--options--upgrade", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("license__2__--featurePack", "license__2__--featurePack", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("license__2__--options--maxDoors", "license__2__--options--maxDoors", "", "", dataTypeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
    }


    /**
     * Insert STARflex Status Template
     *
     * @param group group
     */
    public static void insertSTARflexStatusTemplate(Group group) {
        ThingTypeTemplateCategoryService categoryService = ThingTypeTemplateCategoryService.getInstance();
        DataTypeService service = DataTypeService.getInstance();
        ThingTypeTemplate thingTypeTemplateHead = new ThingTypeTemplate();
        thingTypeTemplateHead.setCode(TT_STARflex_STATUS_NAME);
        thingTypeTemplateHead.setName("STARflex H&S");
        thingTypeTemplateHead.setDescription("Starflex H&S");
        thingTypeTemplateHead.setDisplayOrder(4);
        thingTypeTemplateHead.setThingTypeTemplateCategory(categoryService.getByCode(CATEGORY_THING_TYPE_TEMPLATE_SENSORS));
        thingTypeTemplateHead.setGroup(group);
        thingTypeTemplateHead.setAutoCreate(true);
        thingTypeTemplateHead.setPathIcon("icon-thingtype-antenna-status");
        thingTypeTemplateHead = ThingTypeTemplateService.getInstance().insert(thingTypeTemplateHead);

        insertUdfField("cpuUtilPercent", "cpuUtilPercent", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("loadAvg_0_", "loadAvg_0_", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("loadAvg_1_", "loadAvg_1_", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("loadAvg_2_", "loadAvg_2_", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("metaData_myID", "metaData_myID", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("temperature_c_", "temperature_c_", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("temperature_f_", "temperature_f_", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("freeMem", "freeMem", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("totMem", "totMem", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("ts_ms", "ts_ms", "", "", service.get(ThingTypeField.Type.TYPE_TIMESTAMP.value), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("tz", "tz", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("upTime_sec", "upTime_sec", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("wallclock", "wallclock", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
    }

    /**
     * Insert STARflex Template
     *
     * @param group group
     */
    public static void insertSTARflexTemplate(Group group) {
        ThingTypeTemplateCategoryService categoryService = ThingTypeTemplateCategoryService.getInstance();
        DataTypeService service = DataTypeService.getInstance();
        ThingTypeTemplate thingTypeTemplateHead = new ThingTypeTemplate();
        thingTypeTemplateHead.setCode(TT_STARflex_NAME);
        thingTypeTemplateHead.setName("STARflex Tag");
        thingTypeTemplateHead.setDescription("Starflex tag");
        thingTypeTemplateHead.setDisplayOrder(3);
        thingTypeTemplateHead.setThingTypeTemplateCategory(categoryService.getByCode(CATEGORY_THING_TYPE_TEMPLATE_SENSORS));
        thingTypeTemplateHead.setGroup(group);
        thingTypeTemplateHead.setAutoCreate(true);
        thingTypeTemplateHead.setPathIcon("icon-thingtype-flextag");
        thingTypeTemplateHead = ThingTypeTemplateService.getInstance().insert(thingTypeTemplateHead);

        insertUdfField("location", "location", "", "", service.get(ThingTypeField.Type.TYPE_LONLATALT), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("locationXYZ", "locationXYZ", "", "", service.get(ThingTypeField.Type.TYPE_XYZ), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("thingReader", "thingReader", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("TxID", "TxID", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("zone", "zone", "", "", service.get(ThingTypeField.Type.TYPE_ZONE), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("lastDetectTime", "lastDetectTime", "", "", service.get(ThingTypeField.Type.TYPE_TIMESTAMP), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("txAntennaPort", "txAntennaPort", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("txExpanderPort", "txExpanderPort", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("transmitSource", "transmitSource", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("macId", "macId", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("datestamp", "datestamp", "", "", service.get(ThingTypeField.Type.TYPE_TIMESTAMP), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("round", "round", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("rssi", "rssi", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("preambleQuality", "preambleQuality", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("decoderQuality", "decoderQuality", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("vgaValues", "vgaValues", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("numBits", "numBits", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("rxPhasors", "rxPhasors", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("digitalGain", "digitalGain", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("alias", "alias", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("source", "source", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("logicalReader", "logicalReader", "", "", service.get(ThingTypeField.Type.TYPE_LOGICAL_READER), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("tsCoreIn", "tsCoreIn", "", "", service.get(ThingTypeField.Type.TYPE_TIMESTAMP), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("tsEdgeIn", "tsEdgeIn", "", "", service.get(ThingTypeField.Type.TYPE_TIMESTAMP), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("doorEvent", "doorEvent", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("lastLocateTime", "lastLocateTime", "", "", service.get(ThingTypeField.Type.TYPE_TIMESTAMP), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("filter", "filter", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
    }

    /**
     * Insert the Flex Tag Template Old Structure
     *
     * @param group group
     */
    public static void insertFlexTagOldTemplate(Group group) {
        ThingTypeTemplate thingTypeTemplateHead = insertThingTypeTemplateHead("FlexTag", "FlexTag", group, "sprite template icon-flextag", false);
        insertFlexTagBasicTemplate(thingTypeTemplateHead);
        insertUdfField("logicalReader", "logicalReader", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_LOGICAL_READER.value), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
    }

    /**
     * @param thingTypeTemplateHead thing Type Template
     */
    public static void insertFlexTagBasicTemplate(ThingTypeTemplate thingTypeTemplateHead) {
        DataTypeService service = DataTypeService.getInstance();
        insertUdfField("location", "location", "", "", service.get(ThingTypeField.Type.TYPE_LONLATALT), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("locationXYZ", "locationXYZ", "", "", service.get(3L), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("thingReader", "thingReader", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("TxID", "TxID", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("zone", "zone", "", "", service.get(ThingTypeField.Type.TYPE_ZONE.value), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("lastDetectTime", "lastDetectTime", "", "", service.get(ThingTypeField.Type.TYPE_TIMESTAMP), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("txAntennaPort", "txAntennaPort", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("txExpanderPort", "txExpanderPort", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("transmitSource", "transmitSource", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("macId", "macId", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("datestamp", "datestamp", "", "", service.get(ThingTypeField.Type.TYPE_TIMESTAMP.value), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("round", "round", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("rssi", "rssi", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("preambleQuality", "preambleQuality", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("decoderQuality", "decoderQuality", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("vgaValues", "vgaValues", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("numBits", "numBits", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("rxPhasors", "rxPhasors", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("digitalGain", "digitalGain", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplateHead, "");
        insertUdfField("alias", "alias", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
    }

    /**
     * @param thingTypeTemplateHead
     */
    public static void insertBasicCoreBridgeTemplate(ThingTypeTemplate thingTypeTemplateHead) {
        DataTypeService service = DataTypeService.getInstance();
        insertUdfField("age", "age", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("lpt", "lpt", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("lpt_cep", "lpt_cep", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("lpt_lazy_load", "lpt_lazy_load", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("lpt_mongo", "lpt_mongo", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("lpt_native", "lpt_native", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("lpt_new", "lpt_new", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("lpt_total", "lpt_total", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("new_things", "new_things", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("source", "source", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("things", "things", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("tsCoreIn", "tsCoreIn", "", "", service.get(ThingTypeField.Type.TYPE_TIMESTAMP), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("uptime", "uptime", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
    }

    public void insertCoreBridgeTemplate5_0_0RC10(Group group) {
        ThingTypeTemplateCategoryService categoryService = ThingTypeTemplateCategoryServiceBase.getInstance();
        ThingTypeTemplate thingTypeTemplateHead = new ThingTypeTemplate();
        thingTypeTemplateHead.setCode("CoreBridge");
        thingTypeTemplateHead.setName("CoreBridge");
        thingTypeTemplateHead.setDescription("CoreBridge");
        thingTypeTemplateHead.setDisplayOrder(1);
        thingTypeTemplateHead.setAutoCreate(true);
        thingTypeTemplateHead.setThingTypeTemplateCategory(categoryService.getByCode(CATEGORY_THING_TYPE_TEMPLATE_M2M));
        thingTypeTemplateHead.setGroup(group);
        thingTypeTemplateHead.setPathIcon("icon-thingtype-corebridge");
        thingTypeTemplateHead = ThingTypeTemplateService.getInstance().insert(thingTypeTemplateHead);

        insertBasicCoreBridgeTemplate(thingTypeTemplateHead);
        insertUdfField("lpt_cache", "lpt_cache", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead);
        insertUdfField("que_idle_count", "que_idle_count", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead);
        insertUdfField("que_pop_count", "que_pop_count", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead);
        insertUdfField("que_size_to", "que_size_to", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead);
        insertUdfField("que_size_blog", "que_size_blog", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead);
        insertUdfField("que_size_ooo", "que_size_ooo", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead);
        insertUdfField("que_period_to", "que_period_to", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead);
    }

    /**
     * Insert Core Bridge Template for 5.0.0_RC10
     *
     * @param group
     */
    public void insertEdgeBridgeTemplate5_0_0RC10(Group group) {
        ThingTypeTemplateCategoryService categoryService = ThingTypeTemplateCategoryServiceBase.getInstance();
        ThingTypeTemplate thingTypeTemplateHead = new ThingTypeTemplate();
        thingTypeTemplateHead.setCode("EdgeBridge");
        thingTypeTemplateHead.setName("EdgeBridge");
        thingTypeTemplateHead.setDescription("EdgeBridge");
        thingTypeTemplateHead.setDisplayOrder(2);
        thingTypeTemplateHead.setAutoCreate(true);
        thingTypeTemplateHead.setThingTypeTemplateCategory(categoryService.getByCode(CATEGORY_THING_TYPE_TEMPLATE_M2M));
        thingTypeTemplateHead.setGroup(group);
        thingTypeTemplateHead.setPathIcon("icon-thingtype-edgebridge");
        insertEdgeBridgeTemplateFields(ThingTypeTemplateService.getInstance().insert(thingTypeTemplateHead));
    }

    /**
     * insert Core Bridge Template for 4.3.0_RC13
     *
     * @param group group
     */
    public static void insertCoreBridgeTemplate4_3RC13(Group group) {
        ThingTypeTemplate thingTypeTemplateHead = insertThingTypeTemplateHead("CoreBridge", "CoreBridge", group,
                "sprite template icon-corebridge", true);
        insertBasicCoreBridgeTemplate(thingTypeTemplateHead);
        insertUdfField("lpt_cache", "lpt_cache", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_NUMBER),
                THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
    }

    /**
     * Insert an Edge Bridge Template
     *
     * @param group group
     */
    public static void insertEdgeBridgeTemplate(Group group) {
        ThingTypeTemplate thingTypeTemplateHead = insertThingTypeTemplateHead("EdgeBridge", "EdgeBridge", group, "sprite template icon-edgebridge", true);
        insertEdgeBridgeTemplateFields(thingTypeTemplateHead);
    }

    public static void insertEdgeBridgeTemplateFields(ThingTypeTemplate thingTypeTemplateHead) {
        DataTypeService service = DataTypeService.getInstance();
        insertUdfField("age", "age", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("lastDetects", "lastDetects", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("lastLocates", "lastLocates", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("lpt", "lpt", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("new_things", "new_things", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("socketTimeoutCount", "socketTimeoutCount", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("socketTimeouts", "socketTimeouts", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("source", "source", "", "", service.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("things", "things", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("tsEdgeIn", "tsEdgeIn", "", "", service.get(ThingTypeField.Type.TYPE_TIMESTAMP), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
        insertUdfField("uptime", "uptime", "", "", service.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, true, thingTypeTemplateHead, "");
    }

    /* Populate Thing Type ZPL Template */
    public static void populateThingTypeZPL(Group facility) {
        ThingType zpl = PopDBIOTUtils.popThingTypeZPL(facility, "default_zpl_thingtype");
        try {
            addThingTypeTemplateZPL(ThingTypeTemplateService.getInstance().getByCode("RFID Printer"), zpl);
        } catch (NonUniqueResultException e) {
            throw new UserException("Error to get Template Object : RFID Printer", e);
        }
    }

    /*Update ThingType template ZPL*/
    public static void addThingTypeTemplateZPL(ThingTypeTemplate rfidPrinterTemplate, ThingType zplThingType) {
        ThingTypeFieldTemplate field = new ThingTypeFieldTemplate();
        field.setName("zpl");
        field.setDescription("zpl");
        field.setUnit("");
        field.setSymbol("");
        //field.setType(zplThingType.getId());
        field.setDataTypeThingTypeId(zplThingType.getId());
        field.setType(DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_THING_TYPE));
        field.setTypeParent("NATIVE_THING_TYPE");
        field.setThingTypeTemplate(rfidPrinterTemplate);
        ThingTypeFieldTemplateService.getInstance().insert(field);
    }

    public static void populateNotificationTemplate() {
        NotificationTemplate notificationTemplate = new NotificationTemplate();
        notificationTemplate.setTemplateName("notificationTemplate");
        notificationTemplate.setTemplateBody(
                "{\"subject\":\"Subject Serial: ${serialNumber}\",\"to\":[\"administrator@mojix.com\"]," +
                        "\"email-body\":\"Hi. This is the mail transport agent at mail.elsewhere.com. UDFs:zone=${zone} " +
                        "status=${status}\"}");
        NotificationTemplateService.getInstance().insert(notificationTemplate);
    }

    /**
     * Populate Parameters table
     */
    public static void populateParameters() {
        Parameters parameters = new Parameters(Constants.CONDITION_TYPE, "ALWAYS_TRUE", "@SYSTEM_PARAMETERS_CONDITION_TYPE_ALWAYS_TRUE", "{\"group\":null,\"defaultCondition\":\"1=1\"}");
        ParametersService.getInstance().insert(parameters);

        parameters = new Parameters(Constants.CONDITION_TYPE, "CEP", "@SYSTEM_PARAMETERS_CONDITION_TYPE_CEP", "{\"group\":\"@SYSTEM_PARAMETERS_CONDITION_GROUP_CEP\",\"defaultCondition\":\"1=1\"}");
        ParametersService.getInstance().insert(parameters);

        parameters = new Parameters(Constants.CONDITION_TYPE, "JS", "@SYSTEM_PARAMETERS_CONDITION_TYPE_JS", "{\"group\":\"@SYSTEM_PARAMETERS_CONDITION_GROUP_FUNCTIONS\",\"defaultCondition\":\"function condition(thingWrapper,things,messages,logger){\\n\\tvar zoneCode = thingWrapper.getUdf(\\\"zone\\\");\\n\\tvar result = zoneCode === \\\"Stockroom\\\";\\n\\tlogger.info(\\\"JS condition zone=\\\"+zoneCode+\\\" result=\\\"+result);\\n\\treturn result;\\n}\"}");
        ParametersService.getInstance().insert(parameters);

		parameters = new Parameters(Constants.BRIDGE_TYPE, "edge", "@SYSTEM_PARAMETERS_BRIDGE_TYPE_EDGE",
        getEdgeBridgeCofiguration());

        ParametersService.getInstance().insert(parameters);

		//TODO: use new config
		parameters = new Parameters(Constants.BRIDGE_TYPE, Constants.EDGEBOX_STARFLEX_TYPE, "@SYSTEM_PARAMETERS_BRIDGE_TYPE_STARFLEX",
				PopDBRequiredIOT.getStarFlexBridgeConfiguration(
						Constants.TT_STARflex_CONFIG_CODE,
						Constants.TT_STARflex_STATUS_CODE,
						Constants.TT_STARflex_CODE, "", ""));
		ParametersService.getInstance().insert(parameters);
		parameters = new Parameters(Constants.BRIDGE_TYPE, "FTP", "@SYSTEM_PARAMETERS_BRIDGE_TYPE_FTP",
        getFtpConfiguration());
      ParametersService.getInstance().insert(parameters);
		parameters = new Parameters(Constants.BRIDGE_TYPE, "GPS", "@SYSTEM_PARAMETERS_BRIDGE_TYPE_GPS",
        getGpsConfiguration());
      ParametersService.getInstance().insert(parameters);
		parameters = new Parameters(Constants.BRIDGE_TYPE, "OPEN_RTLS", "@SYSTEM_PARAMETERS_BRIDGE_TYPE_OPEN_RTLS",null);
		ParametersService.getInstance().insert(parameters);
		parameters = new Parameters(Constants.BRIDGE_TYPE, "core", "@SYSTEM_PARAMETERS_BRIDGE_TYPE_CORE",
        getCoreBridgeConfiguration());
        ParametersService.getInstance().insert(parameters);

        parameters = new Parameters(Constants.ACTION_TYPE, "http", "@SYSTEM_PARAMETERS_ACTION_TYPE_HTTP",
                "{\"method\": \"POST\",\"url\": \"\", \"timeout\": -1, \"openResponseIn\":\"MODAL\", " +
                        "\"" + ActionHTTPConstants.EXECUTION_TYPE.value + "\":\"" + ActionHTTPConstants.EXECUTION_TYPE_REST.value + "\"," +
                        "\"headers\": {\"Content-Type\": \"application/json\"}, \"basicAuth\": {\"username\": \"\",\"password\": \"\"}}");
        ParametersService.getInstance().insert(parameters);

		// RulesProcessor parameters.
		Parameters rulesProcessorParameters = new Parameters(Constants.BRIDGE_TYPE, "Rules_Processor",
				"@SYSTEM_PARAMETERS_BRIDGE_TYPE_RULES_PROCESSOR",
        getRulesProcessorConfiguration());

      ParametersService.getInstance().insert(rulesProcessorParameters);
		logger.info("The new parameter to create the new bridge type for RulesProcessor were created successfully");

		// Mongo Injector parameters.
		Parameters mongoInjectorParameters = new Parameters(Constants.BRIDGE_TYPE, "Mongo_Injector",
				"@SYSTEM_PARAMETERS_BRIDGE_TYPE_MONGO_INJECTOR",
        getMongoInjectorConfiguration());
      ParametersService.getInstance().insert(mongoInjectorParameters);
		logger.info("The new parameter to create the new bridge type for mongo injector were created successfully");

        createRulesActionParameteres();
        populateOperators();
    }

    private static JSONObject setParameterValues(Object value, String type, int order, boolean required) {
        JSONObject json = new JSONObject();
        json.put("value", value);
        json.put("type", type);
        json.put("order", order);
        json.put("required", required);
        return json;
    }

    private static JSONObject setParameterValues(Object value, String type, int order, boolean required, String field, String operator, Object dependsValue) {
        JSONObject json = new JSONObject();
        json.put("value", value);
        json.put("type", type);
        json.put("order", order);
        json.put("required", required);
        JSONObject dependsOn = new JSONObject();
        dependsOn.put("field", field);
        dependsOn.put("operator", operator);
        dependsOn.put("value", dependsValue);
        json.put("dependsOn", dependsOn);
        return json;
    }

    public static String getCoreBridgeConfiguration() {
        JSONObject parameter = new JSONObject();
        JSONObject filters = new JSONObject();
        JSONObject config = new JSONObject();
        JSONObject extra = new JSONObject();
        JSONObject jsonValue = new JSONObject();
        JSONArray jsonArray = new JSONArray();

      jsonValue.put("active", setParameterValues(false, "Boolean", 0, false));
      jsonValue.put("shiftProperty", setParameterValues("shift", "String", 1, false));
      jsonValue.put("zoneViolationStatusProperty", setParameterValues("zoneViolationStatus", "String", 2, false));
      jsonValue.put("zoneViolationFlagProperty", setParameterValues("zoneViolationFlag", "String", 3, false));
      filters.put("shiftZoneRule", setParameterValues(jsonValue, "JSON", 0, false));
      jsonValue = new JSONObject();
      jsonValue.put("active", setParameterValues(false, "Boolean", 0, false));
      filters.put("outOfOrderRule", setParameterValues(jsonValue, "JSON", 1, false));
      jsonValue = new JSONObject();
      jsonValue.put("active", setParameterValues(false, "Boolean", 0, false));
      jsonValue.put("timeInterval", setParameterValues(5000, "Number", 1, false));
      jsonValue.put("algorithm", setParameterValues("followLastDetect", "String", 2, false));
      JSONObject arrayNode = new JSONObject();
      arrayNode.put("thingTypeCode", "default_rfid_thingtype");
      arrayNode.put("udfGroupStatus", "groupStatus");
      arrayNode.put("udfGroup", "grouping");
      arrayNode.put("distanceFilter", 10000);
      jsonArray.add(arrayNode);
      jsonValue.put("thingTypes", setParameterValues(jsonArray, "ARRAY", 3, false));
      filters.put("swarmRule", setParameterValues(jsonValue, "JSON", 2, false));

        config.put("numberOfThreads", setParameterValues(2, "Number", 0, true));
        jsonValue = new JSONObject();
        jsonValue.put("active", setParameterValues(false, "Boolean", 0, false));
        config.put("cepChangeLogs", setParameterValues(jsonValue, "JSON", 1, false));
        jsonValue = new JSONObject();
        jsonValue.put("active", setParameterValues(false, "Boolean", 0, false));
        config.put("coreBridgeStatistics", setParameterValues(jsonValue, "JSON", 2, false));
        jsonValue = new JSONObject();
        jsonValue.put("size", setParameterValues(1000000, "Number", 0, false));
        jsonValue.put("evictionTime", setParameterValues(60, "Number", 1, false));
        config.put("thingCache", setParameterValues(jsonValue, "JSON", 3, false));
        jsonValue = new JSONObject();
        jsonValue.put("retryAttemptLimit", setParameterValues(10, "Number", 0, false));
        jsonValue.put("retryIntervalPeriodSecs", setParameterValues(5000, "Number", 1, false));
        config.put("thingInsertRestApi", setParameterValues(jsonValue, "JSON", 4, false));
        jsonValue = new JSONObject();
        jsonValue.put("connectionCode", setParameterValues("", "String", 0, true));
        jsonArray = new JSONArray();
        jsonArray.add("/v1/data/APP2/#");
        jsonArray.add("/v1/data/ALEB/#");
        jsonValue.put("topics", setParameterValues(jsonArray, "ARRAY", 1, false));
        config.put("mqtt", setParameterValues(jsonValue, "JSON", 5, false));
        jsonValue = new JSONObject();
        jsonValue.put("connectionCode", setParameterValues("MONGO", "String", 0, true));
        config.put("mongo", setParameterValues(jsonValue, "JSON", 6, false));
        jsonValue = new JSONObject();
        jsonValue.put("servicesConnectionCode", setParameterValues("SERVICES", "String", 1, false));
        jsonValue.put("sqlConnectionCode", setParameterValues("SQL", "String", 2, false));
        JSONObject memoryValue = new JSONObject();
        memoryValue.put("Xms", "512m");
        memoryValue.put("Xmx", "1024m");
        jsonValue.put("jvmHeapMemory", setParameterValues(memoryValue, "JSON", 2, false));
        config.put("bridgeStartupOptions", setParameterValues(jsonValue, "JSON", 7, false));
        jsonValue = new JSONObject();
        jsonValue.put("agentCode", setParameterValues("", "String", 0, false));
        config.put("bridgeAgent", setParameterValues(jsonValue, "JSON", 8, false));

      parameter.put("filters", filters);
      parameter.put("configuration", config);
      parameter.put("extra", extra);
      return parameter.toJSONString();
  }

  public static String getEdgeBridgeCofiguration() {
      JSONObject parameter = new JSONObject();
      JSONObject filters = new JSONObject();
      JSONObject config = new JSONObject();
      JSONObject extra = new JSONObject();
      JSONObject jsonValue = new JSONObject();

        jsonValue.put("active", setParameterValues(true, "Boolean", 0, false));
        jsonValue.put("unlockDistance", setParameterValues(25, "Number", 1, true));
        jsonValue.put("inZoneDistance", setParameterValues(10, "Number", 2, true));
        jsonValue.put("zoneDwellTime", setParameterValues(300, "Number", 3, true));
        jsonValue.put("lastDetectTimeActive", setParameterValues(true, "Boolean", 4, false));
        jsonValue.put("lastDetectTimeWindow", setParameterValues(0, "Number", 5, true, "zoneDwellFilter.active", "=", true));
        jsonValue.put("evictionTime", setParameterValues(24, "Number", 6, true));
        filters.put("zoneDwellFilter", setParameterValues(jsonValue, "JSON", 0, false));

        config.put("thingTypeCode", setParameterValues("default_rfid_thingtype", "String", 0, true));
        config.put("numberOfThreads", setParameterValues(2, "Number", 1, true));

        jsonValue = new JSONObject();
        jsonValue.put("active", setParameterValues(false, "Boolean", 0, false));
        config.put("coreBridge", setParameterValues("", "String", 2, false));
        config.put("storeAleMessages", setParameterValues(jsonValue, "JSON", 3, false));
        config.put("facilityMapForOrigin", setParameterValues("", "String", 4, true));
        jsonValue = new JSONObject();
        jsonValue.put("active", setParameterValues(true, "Boolean", 0, false));
        config.put("edgeBridgeStatistics", setParameterValues(jsonValue, "JSON", 5, false));
        jsonValue = new JSONObject();
        jsonValue.put("bridgePort", setParameterValues(9090, "Number", 0, true));
        jsonValue.put("socketTimeout", setParameterValues(60000, "Number", 1, true));
        config.put("httpListener", setParameterValues(jsonValue, "JSON", 6, false));
        jsonValue = new JSONObject();
        jsonValue.put("connectionCode", setParameterValues("", "String", 0, true));
        config.put("mqtt", setParameterValues(jsonValue, "JSON", 7, false));
        jsonValue = new JSONObject();
        jsonValue.put("active", setParameterValues(false, "Boolean", 0, false));
        jsonValue.put("connectionCode", setParameterValues("KAFKA", "String", 0, false));
        config.put("kafka", setParameterValues(jsonValue, "JSON", 8, false));
        jsonValue = new JSONObject();
        jsonValue.put("servicesConnectionCode", setParameterValues("SERVICES", "String", 1, false));
        JSONObject memoryValue = new JSONObject();
        memoryValue.put("Xms", "512m");
        memoryValue.put("Xmx", "1024m");
        jsonValue.put("jvmHeapMemory", setParameterValues(memoryValue, "JSON", 2, false));
        config.put("bridgeStartupOptions", setParameterValues(jsonValue, "JSON", 9, false));
        jsonValue = new JSONObject();
        jsonValue.put("agentCode", setParameterValues("", "String", 0, false));
        config.put("bridgeAgent", setParameterValues(jsonValue, "JSON", 10, false));

        parameter.put("filters", filters);
        parameter.put("configuration", config);
        parameter.put("extra", extra);
        return parameter.toJSONString();
    }

    public static String getFtpConfiguration() {
        JSONObject parameter = new JSONObject();
        JSONObject filters = new JSONObject();
        JSONObject config = new JSONObject();
        JSONObject extra = new JSONObject();
        JSONObject jsonValue = new JSONObject();
        JSONArray jsonArray = new JSONArray();

        config.put("thingTypeCode", setParameterValues("", "String", 0, true));
        config.put("coreBridge", setParameterValues("", "String", 1, false));
        config.put("processingType", setParameterValues("", "String", 2, true));
        jsonValue.put("source", setParameterValues("", "String", 0, true));
        jsonValue.put("destination", setParameterValues("", "String", 1, true, "processingType", "=", "full"));
        config.put("remoteFTPFolders", setParameterValues(jsonValue, "JSON", 3, false));
        config.put("filePattern", setParameterValues("", "String", 4, true));
        config.put("patternCaseSensitive", setParameterValues("", "Boolean", 5, true));
        jsonValue = new JSONObject();
        jsonValue.put("ignoreHeader", setParameterValues(false, "Boolean", 0, false));
        jsonValue.put("ignoreFooter", setParameterValues(true, "Boolean", 1, false));
        jsonValue.put("parserType", setParameterValues("fixedlength", "String", 2, true));
        jsonValue.put("fieldLengths", setParameterValues("", "String", 3, true, "configParser.parserType", "=", "fixedlength"));
        jsonValue.put("separator", setParameterValues("", "String", 4, true, "configParser.parserType", "=", "CSV"));
        jsonArray.add("Code");
        jsonArray.add("Description");
        jsonArray.add("Action");
        jsonValue.put("fieldNames", setParameterValues(jsonArray, "ARRAY", 5, true));
        jsonValue.put("columnNumberAsSerial", setParameterValues(0, "Number", 6, false));
        config.put("configParser", setParameterValues(jsonValue, "JSON", 6, false));
        config.put("cronSchedule", setParameterValues("", "String", 7, true));
        jsonValue = new JSONObject();
        jsonValue.put("connectionCode", setParameterValues("", "String", 0, true));
        config.put("mqtt", setParameterValues(jsonValue, "JSON", 8, false));
        jsonValue = new JSONObject();
        jsonValue.put("connectionCode", setParameterValues("", "String", 0, true));
        config.put("ftp", setParameterValues(jsonValue, "JSON", 9, false));
        jsonValue = new JSONObject();
        jsonValue.put("servicesConnectionCode", setParameterValues("SERVICES", "String", 0, false));
        JSONObject memoryValue = new JSONObject();
        memoryValue.put("Xms", "512m");
        memoryValue.put("Xmx", "1024m");
        jsonValue.put("jvmHeapMemory", setParameterValues(memoryValue, "JSON", 1, false));
        config.put("bridgeStartupOptions", setParameterValues(jsonValue, "JSON", 10, false));
        jsonValue = new JSONObject();
        jsonValue.put("agentCode", setParameterValues("", "String", 0, false));
        config.put("bridgeAgent", setParameterValues(jsonValue, "JSON", 11, false));

        parameter.put("filters", filters);
        parameter.put("configuration", config);
        parameter.put("extra", extra);
        return parameter.toJSONString();
    }

    public static String getGpsConfiguration() {
        JSONObject parameter = new JSONObject();
        JSONObject filters = new JSONObject();
        JSONObject config = new JSONObject();
        JSONObject extra = new JSONObject();
        JSONObject jsonValue = new JSONObject();

        config.put("thingTypeCode", setParameterValues("", "String", 0, true));
        config.put("logRawMessages", setParameterValues(false, "Boolean", 1, false));
        jsonValue.put("connectionCode", setParameterValues("", "String", 0, true));
        config.put("mqtt", setParameterValues(jsonValue, "JSON", 2, false));
        jsonValue = new JSONObject();
        jsonValue.put("host", setParameterValues("app.geoforce.com", "String", 0, true));
        jsonValue.put("path", setParameterValues("/feeds/asset_inventory.xml", "String", 1, true));
        jsonValue.put("port", setParameterValues(443, "Number", 2, true));
        jsonValue.put("user", setParameterValues("datafeed@mojix.com", "String", 3, true));
        jsonValue.put("password", setParameterValues("AHmgooCk8l0jo95f7YSo", "String", 4, true));
        jsonValue.put("period", setParameterValues(60, "Number", 5, true));
        config.put("geoforce", setParameterValues(jsonValue, "JSON", 3, false));
        jsonValue = new JSONObject();
        jsonValue.put("servicesConnectionCode", setParameterValues("SERVICES", "String", 1, false));
        JSONObject memoryValue = new JSONObject();
        memoryValue.put("Xms", "512m");
        memoryValue.put("Xmx", "1024m");
        jsonValue.put("jvmHeapMemory", setParameterValues(memoryValue, "JSON", 2, false));
        config.put("bridgeStartupOptions", setParameterValues(jsonValue, "JSON", 4, false));
        jsonValue = new JSONObject();
        jsonValue.put("agentCode", setParameterValues("", "String", 0, false));
        config.put("bridgeAgent", setParameterValues(jsonValue, "JSON", 5, false));

        parameter.put("filters", filters);
        parameter.put("configuration", config);
        parameter.put("extra", extra);
        return parameter.toJSONString();
    }

    public static String getRulesProcessorConfiguration() {
        JSONObject parameter = new JSONObject();
        JSONObject filters = new JSONObject();
        JSONObject config = new JSONObject();
        JSONObject extra = new JSONObject();

        JSONObject jsonValue = new JSONObject();
        jsonValue.put("lingerMs", setParameterValues(5, "Number", 0, true));
        jsonValue.put("numStreamThreads", setParameterValues(4, "Number", 1, true));
        jsonValue.put("batchSize", setParameterValues(65536, "Number", 2, true));

        config.put("streamConfig", setParameterValues(jsonValue, "JSON", 0, false));
        jsonValue = new JSONObject();
        jsonValue.put("connectionCode", setParameterValues("KAFKA", "String", 0, false));
        config.put("kafka", setParameterValues(jsonValue, "JSON", 1, false));
        jsonValue = new JSONObject();
        jsonValue.put("connectionCode", setParameterValues("MONGO", "String", 0, false));
        config.put("mongo", setParameterValues(jsonValue, "JSON", 2, false));
        jsonValue = new JSONObject();
        jsonValue.put("servicesConnectionCode", setParameterValues("SERVICES", "String", 1, false));
        JSONObject memoryValue = new JSONObject();
        memoryValue.put("Xms", "512m");
        memoryValue.put("Xmx", "1024m");
        jsonValue.put("jvmHeapMemory", setParameterValues(memoryValue, "JSON", 2, false));
        config.put("bridgeStartupOptions", setParameterValues(jsonValue, "JSON", 3, false));
        jsonValue = new JSONObject();
        jsonValue.put("agentCode", setParameterValues("", "String", 0, false));
        config.put("bridgeAgent", setParameterValues(jsonValue, "JSON", 4, false));

        parameter.put("filters", filters);
        parameter.put("configuration", config);
        parameter.put("extra", extra);
        return parameter.toJSONString();
    }

    public static String getMongoInjectorConfiguration() {
        JSONObject parameter = new JSONObject();
        JSONObject filters = new JSONObject();
        JSONObject config = new JSONObject();
        JSONObject extra = new JSONObject();

        JSONObject jsonValue = new JSONObject();
        jsonValue.put("connectionCode", setParameterValues("MONGO", "String", 0, false));
        config.put("mongo", setParameterValues(jsonValue, "JSON", 0, false));
        jsonValue = new JSONObject();
        jsonValue.put("lingerMs", setParameterValues(5, "Number", 0, true));
        jsonValue.put("numStreamThreads", setParameterValues(4, "Number", 1, true));
        jsonValue.put("batchSize", setParameterValues(65536, "Number", 2, true));
        config.put("streamConfig", setParameterValues(jsonValue, "JSON", 1, false));
        jsonValue = new JSONObject();
        jsonValue.put("connectionCode", setParameterValues("KAFKA", "String", 0, false));
        config.put("kafka", setParameterValues(jsonValue, "JSON", 2, false));
        jsonValue = new JSONObject();
        jsonValue.put("servicesConnectionCode", setParameterValues("SERVICES", "String", 1, false));
        JSONObject memoryValue = new JSONObject();
        memoryValue.put("Xms", "512m");
        memoryValue.put("Xmx", "1024m");
        jsonValue.put("jvmHeapMemory", setParameterValues(memoryValue, "JSON", 2, false));
        config.put("bridgeStartupOptions", setParameterValues(jsonValue, "JSON", 3, false));
        jsonValue = new JSONObject();
        jsonValue.put("agentCode", setParameterValues("", "String", 0, false));
        config.put("bridgeAgent", setParameterValues(jsonValue, "JSON", 4, false));

        parameter.put("filters", filters);
        parameter.put("configuration", config);
        parameter.put("extra", extra);
        return parameter.toJSONString();
    }

    /**
     * Method populates configuration of starflex bridge
     *
     * @param starflexConfigCode
     * @param starflexStatusCode
     * @param starflexCode
     * @param mqttCode
     * @param mongoCode
     * @return
     */
    public static String getStarFlexBridgeConfiguration(
            String starflexConfigCode,
            String starflexStatusCode,
            String starflexCode,
            String mongoCode,
            String mqttCode) {
        JSONObject jo = new JSONObject();

        JSONObject active2 = new JSONObject();
        active2.put("value", true);
        active2.put("type", "Boolean");
        active2.put("order", 0);
        active2.put("required", false);
        JSONObject lastDetectTimeActive = new JSONObject();
        lastDetectTimeActive.put("value", true);
        lastDetectTimeActive.put("type", "Boolean");
        lastDetectTimeActive.put("order", 0);
        lastDetectTimeActive.put("required", false);
        JSONObject zoneDwellTime = new JSONObject();
        zoneDwellTime.put("value", 10);
        zoneDwellTime.put("type", "Number");
        zoneDwellTime.put("order", 1);
        zoneDwellTime.put("required", true);
        JSONObject inZoneDistance = new JSONObject();
        inZoneDistance.put("value", 0);
        inZoneDistance.put("type", "Number");
        inZoneDistance.put("order", 2);
        inZoneDistance.put("required", false);
        JSONObject lastDetectTimeWindow = new JSONObject();
        lastDetectTimeWindow.put("value", 20);
        lastDetectTimeWindow.put("type", "Number");
        lastDetectTimeWindow.put("order", 3);
        lastDetectTimeWindow.put("required", false);
        JSONObject unlockDistance = new JSONObject();
        unlockDistance.put("value", 0);
        unlockDistance.put("type", "Number");
        unlockDistance.put("order", 4);
        unlockDistance.put("required", false);
        JSONObject evictionTime = new JSONObject();
        evictionTime.put("value", 24);
        evictionTime.put("type", "Number");
        evictionTime.put("order", 5);
        evictionTime.put("required", true);

        JSONObject valueZoneDwellFilter = new JSONObject();
        valueZoneDwellFilter.put("active", active2);
        valueZoneDwellFilter.put("lastDetectTimeActive", lastDetectTimeActive);
        valueZoneDwellFilter.put("zoneDwellTime", zoneDwellTime);
        valueZoneDwellFilter.put("inZoneDistance", inZoneDistance);
        valueZoneDwellFilter.put("lastDetectTimeWindow", lastDetectTimeWindow);
        valueZoneDwellFilter.put("unlockDistance", unlockDistance);
        valueZoneDwellFilter.put("evictionTime", evictionTime);

        JSONObject zoneDwellFilter = new JSONObject();
        zoneDwellFilter.put("value", valueZoneDwellFilter);
        zoneDwellFilter.put("type", "JSON");
        zoneDwellFilter.put("order", 0);
        zoneDwellFilter.put("required", false);

        JSONObject active = new JSONObject();
        active.put("value", false);
        active.put("type", "Boolean");
        active.put("order", 0);
        active.put("required", false);
        JSONObject timeLimit = new JSONObject();
        timeLimit.put("value", 20);
        timeLimit.put("type", "Number");
        timeLimit.put("order", 0);
        timeLimit.put("required", true);
        JSONObject value = new JSONObject();
        value.put("active", active);
        value.put("timeLimit", timeLimit);

        JSONObject rateFilter = new JSONObject();
        rateFilter.put("value", value);
        rateFilter.put("type", "JSON");
        rateFilter.put("order", 1);
        rateFilter.put("required", false);

        JSONObject filters = new JSONObject();
        filters.put("zoneDwellFilter", zoneDwellFilter);
        filters.put("rateFilter", rateFilter);

        JSONObject thingTypeCode = new JSONObject();
        thingTypeCode.put("value", starflexCode);
        thingTypeCode.put("type", "String");
        thingTypeCode.put("order", 1);
        thingTypeCode.put("required", true);
        JSONObject thingTypeCodeConfig = new JSONObject();
        thingTypeCodeConfig.put("value", starflexConfigCode);
        thingTypeCodeConfig.put("type", "String");
        thingTypeCodeConfig.put("order", 2);
        thingTypeCodeConfig.put("required", true);
        JSONObject thingTypeCodeStatus = new JSONObject();
        thingTypeCodeStatus.put("value", starflexStatusCode);
        thingTypeCodeStatus.put("type", "String");
        thingTypeCodeStatus.put("order", 3);
        thingTypeCodeStatus.put("required", true);
        JSONObject mqttConnectionCode = new JSONObject();
        mqttConnectionCode.put("value", mqttCode);
        mqttConnectionCode.put("type", "String");
        mqttConnectionCode.put("order", 0);
        mqttConnectionCode.put("required", true);
        JSONObject mqttValueConnectionCode = new JSONObject();
        mqttValueConnectionCode.put("connectionCode", mqttConnectionCode);
        JSONObject mqtt = new JSONObject();
        mqtt.put("value", mqttValueConnectionCode);
        mqtt.put("type", "JSON");
        mqtt.put("order", 5);
        mqtt.put("required", true);
        JSONObject lastDetectFilterTypes = new JSONObject();
        lastDetectFilterTypes.put("value", "");
        lastDetectFilterTypes.put("type", "String");
        lastDetectFilterTypes.put("order", 6);
        lastDetectFilterTypes.put("required", false);

        JSONObject logRawMessages = new JSONObject();
        logRawMessages.put("value", false);
        logRawMessages.put("type", "Boolean");
        logRawMessages.put("order", 7);
        logRawMessages.put("required", false);

        JSONObject configuration = new JSONObject();
        configuration.put("thingTypeCodeConfig", thingTypeCodeConfig);
        configuration.put("thingTypeCodeStatus", thingTypeCodeStatus);
        configuration.put("thingTypeCode", thingTypeCode);
        configuration.put("coreBridge", setParameterValues("", "String", 4, false));
        configuration.put("mqtt", mqtt);
        configuration.put("lastDetectFilterTypes", lastDetectFilterTypes);
        configuration.put("logRawMessages", logRawMessages);

        JSONObject jsonValue = new JSONObject();
        jsonValue.put("servicesConnectionCode", setParameterValues("SERVICES", "String", 1, false));
        JSONObject jvmHeapMemory = new JSONObject();
        jvmHeapMemory.put("Xms", "512m");
        jvmHeapMemory.put("Xmx", "1024m");
        jsonValue.put("jvmHeapMemory", setParameterValues(jvmHeapMemory, "JSON", 2, false));
        configuration.put("bridgeStartupOptions", setParameterValues(jsonValue, "JSON", 8, false));
        jsonValue = new JSONObject();
        jsonValue.put("agentCode", setParameterValues("", "String", 0, false));
        configuration.put("bridgeAgent", setParameterValues(jsonValue, "JSON", 9, false));
        JSONObject extra = new JSONObject();

        jo.put("filters", filters);
        jo.put("configuration", configuration);
        jo.put("extra", extra);

        return jo.toJSONString();
    }

    public static void populateOperators() {
        Parameters parameters = new Parameters(Constants.OPERATORS_TYPE, "IS_EQUAL_TO", "@OPERATOR_IS_EQUAL_TO", "=");
        ParametersService.getInstance().insert(parameters);
        parameters = new Parameters(Constants.OPERATORS_TYPE, "IS_NOT_EQUAL_TO", "@OPERATOR_IS_NOT_EQUAL_TO", "!=");
        ParametersService.getInstance().insert(parameters);
        parameters = new Parameters(Constants.OPERATORS_TYPE, "IS_EMPTY", "@OPERATOR_IS_EMPTY", "isEmpty");
        ParametersService.getInstance().insert(parameters);
        parameters = new Parameters(Constants.OPERATORS_TYPE, "IS_NOT_EMPTY", "@OPERATOR_IS_NOT_EMPTY", "isNotEmpty");
        ParametersService.getInstance().insert(parameters);
        parameters = new Parameters(Constants.OPERATORS_TYPE, "IS_BETWEEN", "@OPERATOR_IS_BETWEEN", "<>");
        ParametersService.getInstance().insert(parameters);
        parameters = new Parameters(Constants.OPERATORS_TYPE, "IS_NOT_BETWEEN", "@OPERATOR_IS_NOT_BETWEEN", "><");
        ParametersService.getInstance().insert(parameters);
        parameters = new Parameters(Constants.OPERATORS_TYPE, "IS_GREATER_THAN", "@OPERATOR_IS_GREATER_THAN", ">");
        ParametersService.getInstance().insert(parameters);
        parameters = new Parameters(Constants.OPERATORS_TYPE, "IS_LESS_THAN", "@OPERATOR_IS_LESS_THAN", "<");
        ParametersService.getInstance().insert(parameters);
        parameters = new Parameters(Constants.OPERATORS_TYPE, "IS_GREATER_THAN_OR_EQUAL_TO", "@OPERATOR_IS_GREATER_THAN_OR_EQUAL_TO", ">=");
        ParametersService.getInstance().insert(parameters);
        parameters = new Parameters(Constants.OPERATORS_TYPE, "IS_LESS_THAN_OR_EQUAL_TO", "@OPERATOR_IS_LESS_THAN_OR_EQUAL_TO", "<=");
        ParametersService.getInstance().insert(parameters);
        parameters = new Parameters(Constants.OPERATORS_TYPE, "CONTAINS", "@OPERATOR_CONTAINS", "~");
        ParametersService.getInstance().insert(parameters);
    }

    public static void addMongoFunctions() {
        try {
            URL fileURL = PopDBMojixRetail.class.getClassLoader().getResource("mongo/JsonFormatter.js");
            if (fileURL != null) {
                String text = IOUtils.toString(fileURL, Charset.forName("UTF-8"));
                MongoScriptDAO.getInstance().insertRaw("JSONFormatter", text);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void createRulesActionParameteres() {
        Parameters parameters =
                new Parameters(Constants.RULES_ACTION_TYPE, "ThingPropertySetterJSSubscriber",
                        "@RULES_ACTION_JAVASCRIPT_ACTION",
                        "logger.setLevel(org.apache.log4j.Level.DEBUG);\nlogger.info"
                                + "(\"ThingPropertySetterJS example\");\nvar thing = thingWrapper.getThing();"
                                + "\nvar thingMessage = thingWrapper.getThingMessage();\nthingMessage.putField"
                                + "(thingMessage.getTime(),\"status\",\"AUTO\");");
        ParametersService.getInstance().insert(parameters);

        parameters = new Parameters(Constants.RULES_ACTION_TYPE, "AlienReaderGPIOSubscriber",
                "@RULES_ACTION_ALIEN_READER_GPIO_SUBSCRIBER",
                "{\"ip\":\"localhost\",\"port\":23,\"username\":\"alien\",\"password\":\"password\","
                        + "\"times\":{\"lightOn\":5000,\"buzzerOn\":3000,\"buzzerOff\":2000,"
                        + "\"numberOfRetries\":5,\"retryTime\":5000,\"delay\":2000},"
                        + "\"lightPinMap\":{\"light1\":0,\"light2\":2,\"light3\":3,\"light4\":4},"
                        + "\"buzzerPinMap\":{\"buzzer1\":1},"
                        + "\"zoneLightBuzzerMap\":{\"Stockroom\":[\"light1\",\"buzzer1\"],"
                        + "\"Salesfloor\":[\"light2\",\"buzzer1\"],\"PoS\":[\"light3\",\"buzzer1\"],"
                        + "\"Entrance\":[\"light4\",\"buzzer1\"]}}");
        ParametersService.getInstance().insert(parameters);

        parameters = new Parameters(Constants.RULES_ACTION_TYPE, "MFRReaderGPIOSubscriber",
                "@RULES_ACTION_MRF_READER_GPIO_SUBSCRIBER",
                "{\"ip\":\"localhost\",\"port\":65200,\"times\":{\"lightOn\":5000,\"buzzerOn\":3000,"
                        + "\"buzzerOff\":2000,\"delayBeforeTrigger\":0,\"timeBuzzer\":3000,"
                        + "\"maxTimeBuzzer\":5000},\"lightPinMap\":{\"light1\":0,\"light2\":2,\"light3\":3,"
                        + "\"light4\":4},\"buzzerPinMap\":{\"buzzer1\":1},"
                        + "\"zoneLightBuzzerMap\":{\"Stockroom\":[\"light1\",\"buzzer1\"],"
                        + "\"Salesfloor\":[\"light2\",\"buzzer1\"],\"PoS\":[\"light3\",\"buzzer1\"],"
                        + "\"Entrance\":[\"light4\",\"buzzer1\"]}}");
        ParametersService.getInstance().insert(parameters);

        parameters = new Parameters(Constants.RULES_ACTION_TYPE, "MFRTurnOffGPIOSubscriber",
                "@RULES_ACTION_MRF_TURN_OFF_GPIO_SUBSCRIBER",
                "{\"ip\":\"localhost\",\"port\":65200,\"times\":{\"lightOn\":5000,\"buzzerOn\":3000,"
                        + "\"buzzerOff\":2000,\"delayBeforeTrigger\":0,\"timeBuzzer\":3000,"
                        + "\"maxTimeBuzzer\":5000},\"lightPinMap\":{\"light1\":0,\"light2\":2,\"light3\":3,"
                        + "\"light4\":4},\"buzzerPinMap\":{\"buzzer1\":1},"
                        + "\"zoneLightBuzzerMap\":{\"Stockroom\":[\"light1\",\"buzzer1\"],"
                        + "\"Salesfloor\":[\"light2\",\"buzzer1\"],\"PoS\":[\"light3\",\"buzzer1\"],"
                        + "\"Entrance\":[\"light4\",\"buzzer1\"]},\"counterUDFs\":{\"zoneUDF\":\"zone\","
                        + "\"lastZoneIdUDF\":\"lastZoneId\",\"zoneAlertFlagUDF\":\"zoneAlertFlag\"}}");
        ParametersService.getInstance().insert(parameters);

        parameters = new Parameters(Constants.RULES_ACTION_TYPE, "EmailSubscriber",
                "@RULES_ACTION_EMAIL_SUBSCRIBER",
                "{\"mqtt\":{\"connectionCode\":\"MQTT\"},\"contentType\":\"text/html; "
                        + "charset=utf-8\",\"subject\":\"Subject Serial: ${serialNumber}\","
                        + "\"to\":[\"administrator@mojix.com\"],\"email-body\":\"Hi. This is an automated "
                        + "message from Vizix from thing: ${name}.\"}");
        ParametersService.getInstance().insert(parameters);

        parameters = new Parameters(Constants.RULES_ACTION_TYPE,
                "com.tierconnect.riot.bridges.rules.actions.RestEndpointSubscriber",
                "@RULES_ACTION_REST_QUERY",
                "{\"method\" : \"POST\",\"protocol\" : \"http\",\"host\" : \"localhost\",\"port\" : "
                        + "8080,\"host\" : \"localhost\",\"path\" : "
                        + "\"/riot-core-services/api/thingBridge/test/testRestEndpointSubscriber\","
                        + "\"headers\" : { \"Api_key\" : \"root\"},\"basicAuth\" : { \"username\" : "
                        + "\"myname\", \"password\" : \"mypasss\" },\"body\" : \"zone=$zone\"}");
        ParametersService.getInstance().insert(parameters);

        parameters = new Parameters(Constants.RULES_ACTION_TYPE, "MQTTPushSubscriber",
                "@RULES_ACTION_MQTT_PUSH_SUBSCRIBER",
                "{\"host\":\"localhost\",\"port\":1883,\"topic\":\"MQTTDemo\",\"mqtt-body\":\"Serial "
                        + "Number: ${serialNumber}. Hi. This is the mqtt message for thing ${name}\"}");
        ParametersService.getInstance().insert(parameters);

        parameters =
                new Parameters(Constants.RULES_ACTION_TYPE, "TCPAction", "@RULES_ACTION_SET_TCP_FLOW",
                        "{\"host\":\"localhost\",\"port\":23,\"payLoad\":\"Serial Number: ${serialNumber}"
                                + ". Hi. This is the payload message for thing ${name}\","
                                + "\"typeMessage\":\"plainText\",\"encoding\":\"text\"}");
        ParametersService.getInstance().insert(parameters);

        parameters = new Parameters(Constants.RULES_ACTION_TYPE, "ThingPropertySetter",
                "@RULES_ACTION_SET_THING_PROPERTY", "status=\"BAD\"");
        ParametersService.getInstance().insert(parameters);

        parameters = new Parameters(Constants.RULES_ACTION_TYPE, "ReportGeneratorSubscriber",
                "@RULES_ACTION_CREATE_BIG_DATA_TABLE", "{\"groupBy\":\"thingId\"}");
        ParametersService.getInstance().insert(parameters);

        parameters = new Parameters(Constants.RULES_ACTION_TYPE, "ExternalDataBaseSubscriber",
                "@RULES_ACTION_EXTERNAL_DATA_BASE",
                "{\"connectionCode\":\"MSSQLServer\",\"storeProcedure\":\"VizixDocument\","
                        + "\"input\":[\"documentId\",\"documentName\",\"documentType\",\"documentStatus\","
                        + "\"category1\",\"category2\",\"boxId\",\"imagePath\",\"vizixFlag\"],"
                        + "\"inputTypeData\":[\"Integer\",\"String\",\"String\",\"String\",\"String\","
                        + "\"String\",\"Integer\",\"String\",\"String\"]}");
        ParametersService.getInstance().insert(parameters);

        parameters = new Parameters(Constants.RULES_ACTION_TYPE,
                "com.tierconnect.riot.bridges.rules.actions.SuperSubscriber",
                "@RULES_ACTION_SUPER_SUBSCRIBER",
                "[{\"name\":\"com.tierconnect.riot.bridges.rules.actions"
                        + ".ThingPropertySetterSubcriber\",\"active\":1,\"config\":\"status=$zone\"},"
                        + "{\"name\":\"com.tierconnect.riot.bridges.rules.actions"
                        + ".AlienReaderGPIOSubscriber3\",\"active\":1,\"config\":{\"ip\":\"localhost\","
                        + "\"port\":23,\"username\":\"alien\",\"password\":\"password\","
                        + "\"times\":{\"lightOn\":5000,\"buzzerOn\":3000,\"buzzerOff\":2000,"
                        + "\"delayBeforeTrigger\":0,\"timeBuzzer\":3000,\"maxTimeBuzzer\":5000},"
                        + "\"lightPinMap\":{\"light1\":0,\"light2\":2,\"light3\":3,\"light4\":4},"
                        + "\"buzzerPinMap\":{\"buzzer1\":1},"
                        + "\"zoneLightBuzzerMap\":{\"Stockroom\":[\"light1\",\"buzzer1\"],"
                        + "\"Salesfloor\":[\"light2\",\"buzzer1\"],\"PoS\":[\"light3\",\"buzzer1\"],"
                        + "\"Entrance\":[\"light4\",\"buzzer1\"]}}},{\"name\":\"com.tierconnect.riot.bridges"
                        + ".rules.actions.RestEndpointSubscriber\",\"active\":1,"
                        + "\"config\":{\"method\":\"POST\",\"protocol\":\"http\",\"host\":\"localhost\","
                        + "\"port\":8080,\"path\":\"/riot-core-services/api/thingBridge/test"
                        + "/testRestEndpointSubscriber\",\"headers\":{\"Api_key\":\"root\"},"
                        + "\"basicAuth\":{\"username\":\"myname\",\"password\":\"mypasss\"},"
                        + "\"body\":\"zone=$serialNumber\"}},{\"name\":\"com.tierconnect.riot.bridges.rules"
                        + ".actions.MQTTPushSubscriber\",\"active\":1,\"config\":{\"host\":\"localhost\","
                        + "\"port\":1883,\"topic\":\"MQTTDemo\",\"mqtt-body\":\"Serial Number: "
                        + "${serialNumber}. Hi. This is the mqtt message for thing ${name}\"}}]");
        ParametersService.getInstance().insert(parameters);

        parameters = new Parameters(Constants.RULES_ACTION_TYPE, "GooglePubSubPublish",
                "@RULES_ACTION_GOOGLE_PUBSUB_PUBLISH",
                "{\"connectionCode\":\"GPubSub\",\"topic\":\"topic1\","
                        + "\"data\":\"Message body serial=${serialNumber} name=${name} zone=${zone.code}  lastDetectTime=${lastDetectTime}\"," +
                        "\"attributes\":{\"epc\":\"${serialNumber}\"}}");
        ParametersService.getInstance().insert(parameters);
    }
}
