package com.tierconnect.riot.iot.popdb;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.popdb.PopDBRequired;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;
import com.tierconnect.riot.appcore.services.*;
import com.tierconnect.riot.appcore.utils.Configuration;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.iot.dao.mongo.MongoScriptDAO;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.sdk.dao.MongoExecutionException;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.hibernate.Transaction;
import org.jose4j.json.internal.json_simple.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class PopDBMojixRetailApp {
    private static final Logger logger = Logger.getLogger(PopDBMojixRetailApp.class);

    public static void main(String args[]) throws Exception {
        PopDBRequired.initJDBCDrivers();
        System.getProperties().put("hibernate.hbm2ddl.auto", "update");
        System.getProperties().put("hibernate.cache.use_second_level_cache", "false");
        System.getProperties().put("hibernate.cache.use_query_cache", "false");
        Configuration.init(null);
        //CassandraUtils.init(Configuration.getProperty("cassandra.host"), Configuration.getProperty("cassandra
        // .keyspace"));


        PopDBMojixRetailApp popdb = new PopDBMojixRetailApp();
        Transaction transaction = GroupService.getGroupDAO().getSession().getTransaction();
        transaction.begin();
        PopDBIOTUtils.initShiroWithRoot();
        popdb.run();
        transaction.commit();

        Transaction transaction2 = GroupService.getGroupDAO().getSession().getTransaction();
        logger.info("******* Populating Things... ");
        transaction2.begin();
        GroupService.getGroupDAO().getSession().refresh(ThingTypeService.getInstance().get(5L));
        GroupService.getGroupDAO().getSession().refresh(ThingTypeFieldService.getInstance().get(28L));
        GroupService.getGroupDAO().getSession().refresh(ThingTypeFieldService.getInstance().get(29L));
        GroupService.getGroupDAO().getSession().refresh(ThingTypeFieldService.getInstance().get(30L));
        GroupService.getGroupDAO().getSession().refresh(ThingTypeFieldService.getInstance().get(31L));
        GroupService.getGroupDAO().getSession().refresh(ThingTypeFieldService.getInstance().get(32L));
        GroupService.getGroupDAO().getSession().refresh(ThingTypeFieldService.getInstance().get(33L));
        for (int i = 1; i <= 6; i++) {
            GroupService.getGroupDAO().getSession().refresh(GroupService.getInstance().get(Long.parseLong(i + "")));
        }
        for (int i = 1; i <= 145; i++) {
            GroupService.getGroupDAO().getSession().refresh(RoleResourceService.getInstance().get(Long.parseLong(i +
                    "")));
        }
        for (int i = 1; i <= 8; i++) {
            GroupService.getGroupDAO().getSession().refresh(RoleService.getInstance().get(Long.parseLong(i + "")));
        }
        for (int i = 1; i <= 7; i++) {
            GroupService.getGroupDAO().getSession().refresh(UserService.getInstance().get(Long.parseLong(i + "")));
        }
        for (int i = 1; i <= 2; i++) {
            GroupService.getGroupDAO().getSession().refresh(UserRoleService.getInstance().get(Long.parseLong(i + "")));
        }
        PopulateThings.populateThings();
        populateThingTypePath();
        transaction2.commit();
        logger.info("******* End Populating Things... ");
        System.exit(0);
    }

    public void run() throws NonUniqueResultException {
        required();

        createData();
        createDataAppRetail();

        List<Resource> resources1 = ResourceService.list();
        System.out.println("-------------------------------------------------------------------------------------------------->");
        System.out.print(resources1);
        // PopulateDBRiotMaker prm = new PopulateDBRiotMaker();
        // prm.demo();


        // PopDBML pml = new PopDBML();
        // pml.run();
    }

    private void createData() throws NonUniqueResultException {
        PopDBMojixUtils.modifyExistingRecords();

        User rootUser = UserService.getInstance().getRootUser();
        Group rootGroup = GroupService.getInstance().get(1L);
        Group mojix = GroupService.getInstance().get(2L);
        Group sm = GroupService.getInstance().get(3L);
        mojix.setName("Mojix Retail");
        mojix.setCode("mojix");

        // facility, test wing and zone
        GroupType storeGroupType = GroupTypeService.getInstance().get(3L);
        GroupType companyGroupType = GroupTypeService.getInstance().get(2L);
        storeGroupType.setName("Store");
        storeGroupType.setCode("Store");

        GroupType departamentGroupType = PopDBUtils.popGroupType("Department", mojix, storeGroupType, "");


        Group santaMonica = GroupService.getInstance().get(3L);
        santaMonica.setName("Santa Monica");
        santaMonica.setCode("SM");

        // set group to Santa Monica and Discovered MQTT and MONGO connections
       /* Connection mqttSM = ConnectionService.getInstance().getByCode("MQTTSM");
        Connection mongoSM = ConnectionService.getInstance().getByCode("MONGOSM");
        Connection ftpSM = ConnectionService.getInstance().getByCode("FTPSM");
        mqttSM.setGroup(santaMonica);
        mongoSM.setGroup(santaMonica);
        ftpSM.setGroup(santaMonica);
*/
        PopDBIOTUtils.popShift(santaMonica, "DAY-M-W", 800L, 1700L, "23456", "DAY-M-W");

        Role tenantRole = RoleService.getInstance().getTenantAdminRole();


        // MINS: -118.444142 34.047880
        LocalMap localmap = PopDBIOTUtils.populateFacilityMap("Map Store Santa Monica",
                "images/mojixmap.png",
                santaMonica,
                -118.444142,
                205.5,
                34.047880,
                174,
                -118.443969,
                34.048092,
                20.0,
                "ft");


        //createReportDefinitionData2(santaMonica, storeGroupType, rootUser);

        // new groups
        PopDBUtils.popGroup("Front-end", "FE", santaMonica, departamentGroupType, "");
        PopDBUtils.popGroup("Stocking", "S", santaMonica, departamentGroupType, "");
        Group clothing = PopDBUtils.popGroup("Clothing", "C", santaMonica, departamentGroupType, "");

        //Zone Type Default
        ZoneType ztd1 = ZoneTypeService.getInstance().get(1L);
        ZoneType ztd2 = ZoneTypeService.getInstance().get(2L);

        // Zones
        Zone z1 = PopDBIOTUtils.popZone(santaMonica, localmap, "Entrance", "#FF0000", "Off-Site");
        PopDBIOTUtils.popZonePoint(z1, 0, -118.443980544741, 34.048119816839275);
        PopDBIOTUtils.popZonePoint(z1, 1, -118.443972498113, 34.04810259330263);
        PopDBIOTUtils.popZonePoint(z1, 2, -118.443932724570, 34.048114422716736);
        PopDBIOTUtils.popZonePoint(z1, 3, -118.443940646881, 34.04813120659546);
        z1.setZoneType(ztd2);

        Zone z2 = PopDBIOTUtils.popZone(santaMonica, localmap, "PoS", "#FF0000", "On-Site");
        PopDBIOTUtils.popZonePoint(z2, 0, -118.44393829994901, 34.04814426314336);
        PopDBIOTUtils.popZonePoint(z2, 1, -118.44393071291418, 34.04812720146832);
        PopDBIOTUtils.popZonePoint(z2, 2, -118.4439104720305, 34.04813315118781);
        PopDBIOTUtils.popZonePoint(z2, 3, -118.44391784810527, 34.04815037471825);
        z2.setZoneType(ztd1);

        Zone z3 = PopDBIOTUtils.popZone(santaMonica, localmap, "Stockroom", "#FF0000", "On-Site");
        PopDBIOTUtils.popZonePoint(z3, 0, -118.44396414381346, 34.04826240930372);
        PopDBIOTUtils.popZonePoint(z3, 1, -118.4439424738065, 34.0482158438294);
        PopDBIOTUtils.popZonePoint(z3, 2, -118.44388656651233, 34.04823362294016);
        PopDBIOTUtils.popZonePoint(z3, 3, -118.44390781742415, 34.0482776882189);
        PopDBIOTUtils.popZonePoint(z3, 4, -118.44392070445531, 34.048283253696866);
        PopDBIOTUtils.popZonePoint(z3, 5, -118.44395968030507, 34.04827165561227);
        z3.setZoneType(ztd2);

        Zone z4 = PopDBIOTUtils.popZone(santaMonica, localmap, "Salesfloor", "#FF0000", "Off-Site");
        PopDBIOTUtils.popZonePoint(z4, 0, -118.4438802149873, 34.0482288161938);
        PopDBIOTUtils.popZonePoint(z4, 1, -118.4438541638471, 34.048170531850744);
        PopDBIOTUtils.popZonePoint(z4, 2, -118.44374701635302, 34.04820320715321);
        PopDBIOTUtils.popZonePoint(z4, 3, -118.44377259594499, 34.04826137303725);
        z4.setZoneType(ztd1);

        ThingType rfid = ThingTypeService.getInstance().get(1L);
        ThingType gps = ThingTypeService.getInstance().get(2L);
        ThingType pantThingType = PopDBMojixUtils.popThingTypeClothingItem(santaMonica, "Pants");
        ThingType jacketThingType = PopDBMojixUtils.popThingTypeClothingItem(santaMonica, "Jackets");
        ThingType shippingOrder = PopDBMojixUtils.popThingTypeShippingOrder(santaMonica, "ShippingOrder");
        ThingType asset = PopDBMojixUtils.popThingTypeAssetMultiLevel(santaMonica, "Asset", shippingOrder);
        ThingType tag = PopDBMojixUtils.popThingTypeTag(santaMonica, "Tag");
        ThingType colour = PopDBMojixUtils.popThingTypeColour(santaMonica, "ColorFile", "Colour");
        ThingType upc = PopDBMojixUtils.popThingTypeUpc(santaMonica, "UPC file", "UPC");
        ThingType transaction = PopDBMojixUtils.popThingTypeTransaction(santaMonica, "Transaction", "Transaction");

        Group starFlexGroup = PopDBUtils.popGroup("Discovered", "SF", rootGroup, companyGroupType, "");
/*
        Connection mqttSF = ConnectionService.getInstance().getByCode("MQTTSF");
        Connection mongoSF = ConnectionService.getInstance().getByCode("MONGOSF");
        mqttSF.setGroup(starFlexGroup);
        mongoSF.setGroup(starFlexGroup);
*/
        List<Field> starFlexFields = new ArrayList<>();
        starFlexFields.add(FieldService.getInstance().get(4L));
        starFlexFields.add(FieldService.getInstance().get(5L));
        starFlexFields.add(FieldService.getInstance().get(8L));
        starFlexFields.add(FieldService.getInstance().get(9L));
        starFlexFields.add(FieldService.getInstance().get(10L));
        starFlexFields.add(FieldService.getInstance().get(14L));
        starFlexFields.add(FieldService.getInstance().get(16L));
        starFlexFields.add(FieldService.getInstance().get(17L));
        starFlexFields.add(FieldService.getInstance().get(18L));
        starFlexFields.add(FieldService.getFieldDAO().selectBy("name", "zoneGroup"));


        for (Field itemField : starFlexFields) {
            GroupField starFlexField = new GroupField();
            starFlexField.setField(itemField);
            starFlexField.setGroup(starFlexGroup);
            starFlexField.setValue("2");
            GroupFieldService.getGroupFieldDAO().insert(starFlexField);
        }

        //TODO RRCC
        ThingType starFlexConfigThingType    = PopDBMojixUtils.popThingType(
                starFlexGroup, Constants.TT_STARflex_CONFIG_NAME, Constants.TT_STARflex_CONFIG_CODE,
                Constants.TT_STARflex_CONFIG_NAME, false);
        ThingType starFlexStatusThingType = PopDBMojixUtils.popThingType(
                starFlexGroup, Constants.TT_STARflex_STATUS_NAME,Constants.TT_STARflex_STATUS_CODE,
                Constants.TT_STARflex_STATUS_NAME, true);
        ThingType starFlexThingType = PopDBMojixUtils.popThingType(
                starFlexGroup, Constants.TT_STARflex_NAME,Constants.TT_STARflex_CODE,
                Constants.TT_STARflex_NAME, true);
        DevicesService.getInstance().createStarFLEXReport(
                rootUser,starFlexGroup,starFlexConfigThingType,starFlexStatusThingType,starFlexThingType );


        PopDBIOTUtils.popThingTypeMap(jacketThingType, rfid);
        PopDBIOTUtils.popThingTypeMap(pantThingType, rfid);
        PopDBIOTUtils.popThingTypeMap(asset, tag);

        logger.info("adding corebridge and edgebridge to thingType");
        PopDBMojixUtils.popThingType(rootGroup, "CoreBridge", "coreBridge", "CoreBridge", true);
        PopDBMojixUtils.popThingType(rootGroup, "EdgeBridge", "edgeBridge", "EdgeBridge", true);

        // Users
        rootUser.setEmail("root@company.com");
        rootUser.setApiKey("7B4BCCDC");

        UserService.getInstance().update(rootUser);

        // TODO watch out for empty resources
        Role companyUser = PopDBUtils.popRole("Store User", "Store User", new ArrayList<Resource>(), mojix,
                storeGroupType);
        Role companyadmin = PopDBUtils.popRole("Store Administrator", "Store Administrator", new ArrayList<Resource>
                        (), mojix,
                storeGroupType);

        //
        Role storeManager = PopDBUtils.popRole("Store Manager", "Role store manager", null, mojix, storeGroupType);
        Role storeEmployee = PopDBUtils.popRole("Store Employee", "Role store employee", null, mojix, storeGroupType);
        Role pantManager = PopDBUtils.popRole("Pants Manager", "Pants manager", null, mojix, storeGroupType);
        Role reportManager = PopDBUtils.popRole("Report Manager", "Report manager", null, mojix, null);
        List<Resource> resources1 = ResourceService.list();

        for(Resource resource : resources1){

            if (resource.getName().toString().equals("Analytics")) {
                RoleResourceService.getInstance().insert(storeEmployee, resource, "x");
                RoleResourceService.getInstance().insert(reportManager, resource, "x");
            }

            if (resource.getName().toString().equals("reportDefinition") || resource.getName().toString().equals(
                    "reportFilter") || resource.getName().toString().equals("reportProperty")) {
                RoleResourceService.getInstance().insert(storeEmployee, resource, "r");
                RoleResourceService.getInstance().insert(reportManager, resource, "r");
            }

            if (resource.getName().toString().equals("$Pants")
                    || resource.getName().toString().equals("$Jackets")
                    || resource.getName().toString().equals("$Passive RFID Tags")) {
                if (resource.getName().toString().equals("$Pants")) {
                    RoleResourceService.getInstance().insert(pantManager, resource, "riuda");
                }

            }

            if (resource.getName().toString().equals("localMap")) {
                RoleResourceService.getInstance().insert(pantManager, resource, "riuda");
            }

        }

        // Roles for root
        Role rootRole = RoleService.getInstance().getRootRole();

        // Roles for store manager
        for (Resource resource : resources1) {
            if (!resource.getName().startsWith("license")) {
                RoleResourceService.getInstance().insert(storeManager, resource, resource.getAcceptedAttributes());
            }
        }
        // user mojix
        User samUser = PopDBUtils.popUser("samuel", "samuel", santaMonica, storeManager);
        samUser.setFirstName("Samuel");
        samUser.setLastName("Levy");
        samUser.setEmail("samuel.levy@mojix.com");

        Set<UserRole> copia = new HashSet<UserRole>();


        UserRole sam2 = new UserRole();
        sam2.setRole(storeEmployee);
        sam2.setUser(samUser);
        copia.add(sam2);
        sam2 = UserRoleService.getInstance().insert(sam2);
        samUser.setUserRoles(copia);

        // paulUser.setUserRoles(userRoles);
        User paulUser = PopDBUtils.popUser("paul", "paul", clothing, pantManager);
        paulUser.setFirstName("Paul");
        paulUser.setLastName("Barriga");
        paulUser.setEmail("paul.barriga@mojix.com");
        //createReportDefinitionData(santaMonica, departamentGroupType);
        Set<UserRole> copia2 = new HashSet<UserRole>();


        UserRole paul2 = new UserRole();
        paul2.setRole(reportManager);
        paul2.setUser(paulUser);
        copia2.add(paul2);
        paul2 = UserRoleService.getInstance().insert(paul2);
        paulUser.setUserRoles(copia2);

        User adminc = PopDBUtils.popUser("adminc", "adminc", mojix, tenantRole);

        User adminp = PopDBUtils.popUser("adminp", "adminp", santaMonica, companyadmin);
        User employee = PopDBUtils.popUser("employee", santaMonica, storeEmployee);

        final String[] topicMqtt = {"/v1/data/APP2/#", "/v1/data/ALEB/#", "/v1/data/ALEB2/#", "/v1/data/FTPcolour/#", "/v1/data/GPS/#"};

        Edgebox edgeboxMCB2 = EdgeboxService.getInstance().insertEdgebox(mojix, "* Core Bridge Mojix", "CB1", "core",
                PopDBRequiredIOT.getOldCoreBridgeConfiguration(topicMqtt, "MQTTSM", "MONGOSM"), 0L);

        Edgebox mcb = EdgeboxService.getInstance().selectByCode("MCB");
        Edgebox starMCB = EdgeboxService.getInstance().insertEdgebox(
                starFlexGroup, "StarFlex Core Bridge", "STAR_MCB_TEMP", "core", mcb.getConfiguration(), 0L);
        Edgebox starALEB = EdgeboxService.getInstance().insertEdgebox(
                starFlexGroup, "StarFlex ALE Edge Bridge", "STAR_ALEB_TEMP", "edge", getAleBridgeConfiguration("flextag_code", "MQTTSF"), 9093L);
        Edgebox starflexCombinator = EdgeboxService.getInstance().insertEdgebox(
                starFlexGroup,
                Constants.TT_STARflex_NAME+" Bridge",
                Constants.STARFLEX_MAIN_BRIDGE_CODE,
                Constants.EDGEBOX_STARFLEX_TYPE,
                PopDBRequiredIOT.getStarFlexBridgeConfiguration(
                        Constants.TT_STARflex_CONFIG_CODE,
                        Constants.TT_STARflex_STATUS_CODE,
                        Constants.TT_STARflex_CODE, "MONGOSF", "MQTTSF"),
                9092L);


//        Edgebox starMCB = EdgeboxService.getInstance().selectByCode("STAR_MCB_TEMP");
//        Edgebox starALEB = EdgeboxService.getInstance().selectByCode("STAR_ALEB_TEMP");


//        eb4.setGroup(starFlexGroup);
//        eb5.setGroup(starFlexGroup);
        // update data aleBridge
        starALEB.setCode("ALEB_" + starFlexGroup.getName().toUpperCase());
        starALEB.setGroup(starFlexGroup);
        // update data coreBridge
        final String[] topicMqttCore = {
                "/v1/data/APP2/#",
                "/v1/data/" + starflexCombinator.getCode() + "/#",
                "/v1/data/" + starALEB.getCode() + "/#"};
        starMCB.setCode("MCB_" + starFlexGroup.getName().toUpperCase());
        starMCB.setGroup(starFlexGroup);
        starMCB.setConfiguration(PopDBRequiredIOT.getOldCoreBridgeConfiguration(topicMqttCore, "MQTTSF", "MONGOSF"));
        EdgeboxService.getInstance().update(starMCB);


        EdgeboxService.getInstance().insertEdgebox(sm, "FTP Bridge Colour", "FTPcolour", "FTP",
                getFtpBridgeColourConfiguration("FTPSM", "MQTTSM"), 0L);
    }

    private void createDataAppRetail() throws NonUniqueResultException {

        Date storageDate = new Date();

        Group santaMonica = GroupService.getInstance().get(3L);
        santaMonica.setName("Santa Monica");
        santaMonica.setCode("SM");

        // facility, test wing and zone
        GroupType storeGroupType = GroupTypeService.getInstance().get(3L);
        storeGroupType.setName("Store");
        storeGroupType.setCode("Store");

        User rootUser = UserService.getInstance().getRootUser();


        System.out.print("***************************************************************>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        ThingType userSettingsThingType = createRetailUserSettingsThingType();
        populateRetailUserSettingsThings(storageDate, userSettingsThingType);

        ThingType globalConfigurationSettingsThingType = createRetailGlobalConfigurationSettingsThingType();

        ThingType globalPriority = createRetailGlobalPriorityThingType();
        populateRetailGlobalPriorityThings(storageDate, globalPriority);

        createRetailGroupCategoryThingType();
        createRetailHotReplenishmentThingType();
        createRetailReplenishmentThingType();
        createRetailDressingRoomThingType();

        ThingType columnsSettings = createRetailReportColumnsSettingsThingType();
        populateRetailReportColumnsSettingsThings(storageDate, columnsSettings);

        createRetailReportList();

        Map<String, Long> reportsIds = createReportDefinitionDataRetail(santaMonica, storeGroupType, rootUser);
        populateRetailGlobalConfigurationSettingsThings(storageDate, globalConfigurationSettingsThingType, reportsIds);


        Role roleRetail = PopDBUtils.popRole("Retail", "Retail role", null, santaMonica, storeGroupType);
        Role roleRetailAdmin = PopDBUtils.popRole("Retail Admin", "Retail Admin role", null, santaMonica, storeGroupType);
        Role roleRetailReplenishment = PopDBUtils.popRole("Retail Replenishment", "Retail Replenishment role", null, santaMonica, storeGroupType);
        Role roleRetailHotReplenishment = PopDBUtils.popRole("Retail Hot Replenishment", "Retail Hot Replenishment role", null, santaMonica, storeGroupType);
        Role roleRetailSellThruReplenishment = PopDBUtils.popRole("Retail Sell Thru Replenishment", "Retail Sell Thru Replenishment role", null, santaMonica, storeGroupType);
        Role roleRetailDressingRoom = PopDBUtils.popRole("Retail Dressing Room", "Retail Dressing Room role", null, santaMonica, storeGroupType);
        Role roleRetailMoneyMapping = PopDBUtils.popRole("Retail Money Mapping", "Retail Money Mapping role", null, santaMonica, storeGroupType);
        Role roleRetailReports = PopDBUtils.popRole("Retail Reports", "Retail Reports role", null, santaMonica, storeGroupType);

        User user = new User("mojix");
        user.setPassword("1234");
        user.setGroup(santaMonica);
        UserService.getInstance().insert(user);

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(roleRetail);
        UserRoleService.getInstance().insert(userRole);

        UserRole userRoleAdmin = new UserRole();
        userRoleAdmin.setUser(user);
        userRoleAdmin.setRole(roleRetailAdmin);
        UserRoleService.getInstance().insert(userRoleAdmin);

        setAdminPermissions(roleRetail);
        setAdminPermissions(roleRetailAdmin);
        setGeneralPermissions(roleRetailReplenishment, "[Retail] Replenishment");
        setGeneralPermissions(roleRetailHotReplenishment, "Hot Replenishment");
        setGeneralPermissions(roleRetailDressingRoom, "Dressing Room");
        setGeneralPermissions(roleRetailSellThruReplenishment, "Sell Thru Replenishment");
        setGeneralPermissions(roleRetailMoneyMapping, "Money Mapping");
        setAdminPermissions(roleRetailReports);

    }

    /*
    * Permissions
    * */
    private void setAdminPermissions(Role role){
        List<Resource> resources = ResourceService.list();

        for (Resource resource : resources) {

            if (resource.getName().equals("Control")) {
                RoleResourceService.getInstance().insert(role, resource, resource.getAcceptedAttributes());
            }
            if (resource.getName().equals("group")) {
                RoleResourceService.getInstance().insert(role, resource, resource.getAcceptedAttributes());
            }
            if (resource.getName().equals("groupType")) {
                RoleResourceService.getInstance().insert(role, resource, resource.getAcceptedAttributes());
            }
            if (resource.getName().equals("thing")) {
                RoleResourceService.getInstance().insert(role, resource, resource.getAcceptedAttributes());
            }
            if (resource.getName().equals("thingType")) {
                RoleResourceService.getInstance().insert(role, resource, resource.getAcceptedAttributes());
            }
            if (resource.getName().equals("thingTypeFieldTemplate")) {
                RoleResourceService.getInstance().insert(role, resource, resource.getAcceptedAttributes());
            }
            if (resource.getName().equals("thingTypeTemplate")) {
                RoleResourceService.getInstance().insert(role, resource, resource.getAcceptedAttributes());
            }
            if (resource.getName().equals("license")) {
                RoleResourceService.getInstance().insert(role, resource, resource.getAcceptedAttributes());
            }
            if (resource.getName().equals("role")) {
                RoleResourceService.getInstance().insert(role, resource, resource.getAcceptedAttributes());
            }
            if (resource.getName().equals("user")) {
                RoleResourceService.getInstance().insert(role, resource, resource.getAcceptedAttributes());
            }
            if (resource.getName().equals("Report Instances")) {
                RoleResourceService.getInstance().insert(role, resource, resource.getAcceptedAttributes());
            }
            if (resource.getLabel().contains("Retail")) {
                RoleResourceService.getInstance().insert(role, resource, resource.getAcceptedAttributes());
            }
            if (resource.getName().equals("Reports")) {
                RoleResourceService.getInstance().insert(role, resource, resource.getAcceptedAttributes());
            }
            if (resource.getName().equals("reportDefinition") || resource.getName().equals
                    ("reportFilter")
                    || resource.getName().equals("reportProperty")) {

                RoleResourceService.getInstance().insert(role, resource, resource.getAcceptedAttributes());
            }
        }
    }

    private void setGeneralPermissions(Role role, String target){
        List<Resource> resources = ResourceService.list();

        for (Resource resource : resources) {

            if (resource.getName().equals("Control")) {
                RoleResourceService.getInstance().insert(role, resource, resource.getAcceptedAttributes());
            }
            if (resource.getName().equals("group")) {
                RoleResourceService.getInstance().insert(role, resource, resource.getAcceptedAttributes());
            }
            if (resource.getName().equals("groupType")) {
                RoleResourceService.getInstance().insert(role, resource, resource.getAcceptedAttributes());
            }
            if (resource.getName().equals("thing")) {
                RoleResourceService.getInstance().insert(role, resource, resource.getAcceptedAttributes());
            }
            if (resource.getName().equals("thingType")) {
                RoleResourceService.getInstance().insert(role, resource, resource.getAcceptedAttributes());
            }
            if (resource.getName().equals("thingTypeFieldTemplate")) {
                RoleResourceService.getInstance().insert(role, resource, resource.getAcceptedAttributes());
            }
            if (resource.getName().equals("thingTypeTemplate")) {
                RoleResourceService.getInstance().insert(role, resource, resource.getAcceptedAttributes());
            }
            if (resource.getName().equals("license")) {
                RoleResourceService.getInstance().insert(role, resource, resource.getAcceptedAttributes());
            }
            if (resource.getName().equals("role")) {
                RoleResourceService.getInstance().insert(role, resource, resource.getAcceptedAttributes());
            }
            if (resource.getName().equals("Report Instances")) {
                RoleResourceService.getInstance().insert(role, resource, resource.getAcceptedAttributes());
            }

            if (resource.getLabel().contains("Retail") && resource.getParent() != null && resource.getParent().getName().equals("ThingType")){
                RoleResourceService.getInstance().insert(role, resource, resource.getAcceptedAttributes());
            }

            if (resource.getLabel().contains("Retail") && resource.getParent() != null && resource.getParent().getName().equals("Report Instances") && (resource.getLabel().contains(target) || resource.getLabel().contains("Groups All") || resource.getLabel().contains("Home Totals")) ){
                RoleResourceService.getInstance().insert(role, resource, resource.getAcceptedAttributes());
            }

            if (resource.getName().equals("Reports")) {
                RoleResourceService.getInstance().insert(role, resource, resource.getAcceptedAttributes());
            }
            if (resource.getName().equals("reportDefinition") || resource.getName().equals
                    ("reportFilter")
                    || resource.getName().equals("reportProperty")) {

                RoleResourceService.getInstance().insert(role, resource, resource.getAcceptedAttributes());
            }
        }
    }

    private void setReportsPermissions(Role role){
        List<Resource> resources = ResourceService.list();

        for (Resource resource : resources) {

            if (resource.getName().equals("Control")) {
                RoleResourceService.getInstance().insert(role, resource, resource.getAcceptedAttributes());
            }
            if (resource.getName().equals("group")) {
                RoleResourceService.getInstance().insert(role, resource, resource.getAcceptedAttributes());
            }
            if (resource.getName().equals("groupType")) {
                RoleResourceService.getInstance().insert(role, resource, resource.getAcceptedAttributes());
            }
            if (resource.getName().equals("thing")) {
                RoleResourceService.getInstance().insert(role, resource, resource.getAcceptedAttributes());
            }
            if (resource.getName().equals("thingType")) {
                RoleResourceService.getInstance().insert(role, resource, resource.getAcceptedAttributes());
            }
            if (resource.getName().equals("thingTypeFieldTemplate")) {
                RoleResourceService.getInstance().insert(role, resource, resource.getAcceptedAttributes());
            }
            if (resource.getName().equals("thingTypeTemplate")) {
                RoleResourceService.getInstance().insert(role, resource, resource.getAcceptedAttributes());
            }
            if (resource.getName().equals("license")) {
                RoleResourceService.getInstance().insert(role, resource, resource.getAcceptedAttributes());
            }
            if (resource.getName().equals("role")) {
                RoleResourceService.getInstance().insert(role, resource, resource.getAcceptedAttributes());
            }
            if (resource.getName().equals("Report Instances")) {
                RoleResourceService.getInstance().insert(role, resource, resource.getAcceptedAttributes());
            }

            if (resource.getLabel().contains("Retail") && resource.getParent() != null && resource.getParent().getName().equals("ThingType")){
                RoleResourceService.getInstance().insert(role, resource, resource.getAcceptedAttributes());
            }

            if (resource.getLabel().contains("Retail") && resource.getParent() != null && resource.getParent().getName().equals("Report Instances") && (resource.getLabel().contains("Inventory") || resource.getLabel().contains("Out Of Stock") || resource.getLabel().contains("Replenishment Report") || resource.getLabel().contains("Retired"))) {
                RoleResourceService.getInstance().insert(role, resource, resource.getAcceptedAttributes());
            }

            if (resource.getName().equals("Reports")) {
                RoleResourceService.getInstance().insert(role, resource, resource.getAcceptedAttributes());
            }
            if (resource.getName().equals("reportDefinition") || resource.getName().equals
                    ("reportFilter")
                    || resource.getName().equals("reportProperty")) {

                RoleResourceService.getInstance().insert(role, resource, resource.getAcceptedAttributes());
            }
        }
    }

    /*
    * Retail User Settings
    */
    private ThingType createRetailUserSettingsThingType() throws NonUniqueResultException {
        Group rootGroup = GroupService.getInstance().get(1L);
        User rootUser = UserService.getInstance().getRootUser();
        Group mojix = GroupService.getInstance().get(2L);
        Group sm = GroupService.getInstance().get(3L);

        String thingTypeCode = "RetailUserSettings";

        ThingType retailUserSettings = PopDBIOTUtils.popThingType(rootGroup, null, "Retail User Settings");

        retailUserSettings.setThingTypeCode(thingTypeCode);
        retailUserSettings.setAutoCreate(true);
        ThingTypeService.getInstance().update(retailUserSettings);
        PopDBIOTUtils.popThingTypeField(retailUserSettings,
                "retailParentGroup",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);

        PopDBIOTUtils.popThingTypeField(retailUserSettings,
                "retailParentGroupName",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);

        PopDBIOTUtils.popThingTypeField(retailUserSettings,
                "store",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailUserSettings,
                "storeCode",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);

        PopDBIOTUtils.popThingTypeField(retailUserSettings,
                "username",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);


        return retailUserSettings;

    }

    private void populateRetailUserSettingsThings(Date storageDate, ThingType userSettingsThingType) {
        Map<String, Object> udfs = new HashMap<String, Object>();
        udfs = getRetailUserSettingsUdf(storageDate.getTime(), userSettingsThingType);

        ThingsService.getInstance()
                .create(new Stack<Long>(),
                        userSettingsThingType.getThingTypeCode(),
                        GroupService.getInstance().get(3L).getHierarchyName(
                                false),
                        "User Mojix", "mojix",
                        null,
                        udfs,
                        null,
                        null,
                        false,
                        false,
                        storageDate,
                        true,
                        true);
    }

    private Map<String, Object> getRetailUserSettingsUdf(Long storageTime, ThingType userSettingsThingType) {
        Map<String, Object> udf = new HashMap<String, Object>();
        Map<String, Object> fieldMap = new HashMap<String, Object>();
        for (ThingTypeField field : userSettingsThingType.getThingTypeFields()) {
            fieldMap = new HashMap<String, Object>();
            switch (field.getName().toString()) {
                case ("retailParentGroup"): {
                    fieldMap.put("thingTypeFieldId", field.getId());
                    fieldMap.put("value", "gcwomen");
                    fieldMap.put("time", storageTime);

                    udf.put(field.getName().toString(), fieldMap);
                    break;
                }

                case ("retailParentGroupName"): {
                    fieldMap.put("thingTypeFieldId", field.getId());
                    fieldMap.put("value", "Women");
                    fieldMap.put("time", storageTime);

                    udf.put(field.getName().toString(), fieldMap);
                    break;
                }
                case ("store"): {
                    fieldMap.put("thingTypeFieldId", field.getId());
                    fieldMap.put("value", "Foose Park");
                    fieldMap.put("time", storageTime);

                    udf.put(field.getName().toString(), fieldMap);
                    break;
                }

                case ("storeCode"): {
                    fieldMap.put("thingTypeFieldId", field.getId());
                    fieldMap.put("value", "0369");
                    fieldMap.put("time", storageTime);

                    udf.put(field.getName().toString(), fieldMap);
                    break;
                }

                case ("username"): {
                    fieldMap.put("thingTypeFieldId", field.getId());
                    fieldMap.put("value", "mojix");
                    fieldMap.put("time", storageTime);

                    udf.put(field.getName().toString(), fieldMap);
                    break;
                }
            }
        }
        return udf;
    }

    /*
    * Retail Global Configuration Settings
    */
    private ThingType createRetailGlobalConfigurationSettingsThingType() throws NonUniqueResultException {
        Group rootGroup = GroupService.getInstance().get(1L);
        User rootUser = UserService.getInstance().getRootUser();
        Group mojix = GroupService.getInstance().get(2L);
        Group sm = GroupService.getInstance().get(3L);

        String thingTypeCode = "RetailGlobalConfigurationSettings";

        ThingType globalConfigurationSettings = PopDBIOTUtils.popThingType(rootGroup, null, "Retail Global Configuration Settings");

        globalConfigurationSettings.setThingTypeCode(thingTypeCode);
        globalConfigurationSettings.setAutoCreate(true);
        ThingTypeService.getInstance().update(globalConfigurationSettings);
        PopDBIOTUtils.popThingTypeField(globalConfigurationSettings,
                "api_key",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);

        PopDBIOTUtils.popThingTypeField(globalConfigurationSettings,
                "dressingRoomReport",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_NUMBER.value,
                true,
                null,
                null,
                null);

        PopDBIOTUtils.popThingTypeField(globalConfigurationSettings,
                "dressingRoomReportAllDetail",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_NUMBER.value,
                true,
                null,
                null,
                null);

        PopDBIOTUtils.popThingTypeField(globalConfigurationSettings,
                "dressingRoomReportDetail",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_NUMBER.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(globalConfigurationSettings,
                "homeTimeRefresh",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_NUMBER.value,
                true,
                "10000",
                null,
                null);
        PopDBIOTUtils.popThingTypeField(globalConfigurationSettings,
                "homeTotalsReport",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_NUMBER.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(globalConfigurationSettings,
                "host",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(globalConfigurationSettings,
                "hotReplenishDetailReport",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_NUMBER.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(globalConfigurationSettings,
                "hotReplenishReport",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_NUMBER.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(globalConfigurationSettings,
                "imageHost",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(globalConfigurationSettings,
                "limitDataCanBeLoaded",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_NUMBER.value,
                true,
                "100",
                null,
                null);
        PopDBIOTUtils.popThingTypeField(globalConfigurationSettings,
                "pageNumberDefault",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_NUMBER.value,
                true,
                "1",
                null,
                null);
        PopDBIOTUtils.popThingTypeField(globalConfigurationSettings,
                "pageSizeDefault",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_NUMBER.value,
                true,
                "20",
                null,
                null);
        PopDBIOTUtils.popThingTypeField(globalConfigurationSettings,
                "port",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(globalConfigurationSettings,
                "replenishDetailReport",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_NUMBER.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(globalConfigurationSettings,
                "replenishGroupsReport",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_NUMBER.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(globalConfigurationSettings,
                "replenishReport",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_NUMBER.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(globalConfigurationSettings,
                "reportInventory",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_NUMBER.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(globalConfigurationSettings,
                "reportOutOfStock",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_NUMBER.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(globalConfigurationSettings,
                "reportReplenishment",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_NUMBER.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(globalConfigurationSettings,
                "reportRetairedItems",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_NUMBER.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(globalConfigurationSettings,
                "reportSellThruReplenishment",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_NUMBER.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(globalConfigurationSettings,
                "reportTotalInventory",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_NUMBER.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(globalConfigurationSettings,
                "sellThruReplenishReport",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_NUMBER.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(globalConfigurationSettings,
                "statusDone",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                "Done",
                null,
                null);
        PopDBIOTUtils.popThingTypeField(globalConfigurationSettings,
                "statusPartial",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                "Partial",
                null,
                null);
        PopDBIOTUtils.popThingTypeField(globalConfigurationSettings,
                "thingGroup",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(globalConfigurationSettings,
                "thingTypeCodeDressingRoom",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                "RetailDressingRoom",
                null,
                null);
        PopDBIOTUtils.popThingTypeField(globalConfigurationSettings,
                "thingTypeCodeHotReplenishment",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                "RetailHotReplenishment",
                null,
                null);
        PopDBIOTUtils.popThingTypeField(globalConfigurationSettings,
                "thingTypeCodePriority",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                "RetailGlobalPriority",
                null,
                null);
        PopDBIOTUtils.popThingTypeField(globalConfigurationSettings,
                "thingTypeCodeReplenishment",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                "RetailReplenishment",
                null,
                null);
        PopDBIOTUtils.popThingTypeField(globalConfigurationSettings,
                "timeDonePopUp",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_NUMBER.value,
                true,
                "1000",
                null,
                null);
        PopDBIOTUtils.popThingTypeField(globalConfigurationSettings,
                "timeRefresh",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_NUMBER.value,
                true,
                "20000",
                null,
                null);

        return globalConfigurationSettings;
    }

    private void populateRetailGlobalConfigurationSettingsThings(Date storageDate, ThingType globalConfigurationSettings, Map<String, Long> reportsIds) {
        Map<String, Object> udfs = new HashMap<String, Object>();

        udfs = getRetailGlobalConfigurationSettingsUdf(storageDate.getTime(), globalConfigurationSettings, reportsIds);

        ThingsService.getInstance()
                .create(new Stack<Long>(),
                        globalConfigurationSettings.getThingTypeCode(),
                        GroupService.getInstance().get(3L).getHierarchyName(
                                false),
                        "Retail Settings Tasks and Reports Groups",
                        "RETAILGLOBALCONFIGURATIONSETTINGS",
                        null,
                        udfs,
                        null,
                        null,
                        false,
                        false,
                        storageDate,
                        true,
                        true);
    }

    private Map<String, Object> getRetailGlobalConfigurationSettingsUdf(Long storageTime, ThingType configurationSettings, Map<String, Long> reportsIds) {
        Map<String, Object> udf = new HashMap<String, Object>();
        Map<String, Object> fieldMap = new HashMap<String, Object>();
        for (ThingTypeField field : configurationSettings.getThingTypeFields()) {
            fieldMap = new HashMap<String, Object>();
            switch (field.getName().toString()) {
                case ("dressingRoomReport"): {
                    fieldMap.put("thingTypeFieldId", field.getId());
                    fieldMap.put("value", reportsIds.get("dressingRoomReport"));
                    fieldMap.put("time", storageTime);

                    udf.put(field.getName().toString(), fieldMap);
                    break;
                }

                case ("dressingRoomReportAllDetail"): {
                    fieldMap.put("thingTypeFieldId", field.getId());
                    fieldMap.put("value", reportsIds.get("dressingRoomReportAllDetail"));
                    fieldMap.put("time", storageTime);

                    udf.put(field.getName().toString(), fieldMap);
                    break;
                }
                case ("dressingRoomReportDetail"): {
                    fieldMap.put("thingTypeFieldId", field.getId());
                    fieldMap.put("value", reportsIds.get("dressingRoomReportDetail"));
                    fieldMap.put("time", storageTime);

                    udf.put(field.getName().toString(), fieldMap);
                    break;
                }

                case ("hotReplenishDetailReport"): {
                    fieldMap.put("thingTypeFieldId", field.getId());
                    fieldMap.put("value", reportsIds.get("hotReplenishDetailReport"));
                    fieldMap.put("time", storageTime);

                    udf.put(field.getName().toString(), fieldMap);
                    break;
                }

                case ("hotReplenishReport"): {
                    fieldMap.put("thingTypeFieldId", field.getId());
                    fieldMap.put("value", reportsIds.get("hotReplenishReport"));
                    fieldMap.put("time", storageTime);

                    udf.put(field.getName().toString(), fieldMap);
                    break;
                }

                case ("replenishDetailReport"): {
                    fieldMap.put("thingTypeFieldId", field.getId());
                    fieldMap.put("value", reportsIds.get("replenishDetailReport"));
                    fieldMap.put("time", storageTime);

                    udf.put(field.getName().toString(), fieldMap);
                    break;
                }

                case ("replenishGroupsReport"): {
                    fieldMap.put("thingTypeFieldId", field.getId());
                    fieldMap.put("value", reportsIds.get("replenishGroupsReport"));
                    fieldMap.put("time", storageTime);

                    udf.put(field.getName().toString(), fieldMap);
                    break;
                }

                case ("replenishReport"): {
                    fieldMap.put("thingTypeFieldId", field.getId());
                    fieldMap.put("value", reportsIds.get("replenishReport"));
                    fieldMap.put("time", storageTime);

                    udf.put(field.getName().toString(), fieldMap);
                    break;
                }

                case ("reportInventory"): {
                    fieldMap.put("thingTypeFieldId", field.getId());
                    fieldMap.put("value", reportsIds.get("reportInventory"));
                    fieldMap.put("time", storageTime);

                    udf.put(field.getName().toString(), fieldMap);
                    break;
                }

                case ("reportOutOfStock"): {
                    fieldMap.put("thingTypeFieldId", field.getId());
                    fieldMap.put("value", reportsIds.get("reportOutOfStock"));
                    fieldMap.put("time", storageTime);

                    udf.put(field.getName().toString(), fieldMap);
                    break;
                }

                case ("reportReplenishment"): {
                    fieldMap.put("thingTypeFieldId", field.getId());
                    fieldMap.put("value", reportsIds.get("reportReplenishment"));
                    fieldMap.put("time", storageTime);

                    udf.put(field.getName().toString(), fieldMap);
                    break;
                }

                case ("reportRetairedItems"): {
                    fieldMap.put("thingTypeFieldId", field.getId());
                    fieldMap.put("value", reportsIds.get("reportRetairedItems"));
                    fieldMap.put("time", storageTime);

                    udf.put(field.getName().toString(), fieldMap);
                    break;
                }
                case ("reportTotalInventory"): {
                    fieldMap.put("thingTypeFieldId", field.getId());
                    fieldMap.put("value", reportsIds.get("reportTotalInventory"));
                    fieldMap.put("time", storageTime);

                    udf.put(field.getName().toString(), fieldMap);
                    break;
                }
                case ("reportSellThruReplenishment"): {
                    fieldMap.put("thingTypeFieldId", field.getId());
                    fieldMap.put("value", reportsIds.get("reportSellThruReplenishment"));
                    fieldMap.put("time", storageTime);

                    udf.put(field.getName().toString(), fieldMap);
                    break;
                }
                case ("sellThruReplenishReport"): {
                    fieldMap.put("thingTypeFieldId", field.getId());
                    fieldMap.put("value", reportsIds.get("sellThruReplenishReport"));
                    fieldMap.put("time", storageTime);

                    udf.put(field.getName().toString(), fieldMap);
                    break;
                }
                case ("homeTotalsReport"): {
                    fieldMap.put("thingTypeFieldId", field.getId());
                    fieldMap.put("value", reportsIds.get("homeTotalsReport"));
                    fieldMap.put("time", storageTime);

                    udf.put(field.getName().toString(), fieldMap);
                    break;
                }

                case ("thingGroup"): {
                    fieldMap.put("thingTypeFieldId", field.getId());
                    fieldMap.put("value", GroupService.getInstance().get(3L).getHierarchyName(false));
                    fieldMap.put("time", storageTime);

                    udf.put(field.getName().toString(), fieldMap);
                    break;
                }

                case ("thingTypeCodePriority"): {
                    fieldMap.put("thingTypeFieldId", field.getId());
                    fieldMap.put("value", "thingTypeCodePriority");
                    fieldMap.put("time", storageTime);

                    udf.put(field.getName().toString(), fieldMap);
                    break;
                }

            }
        }

        return udf;
    }

    /*
    * Retail Global Priority
    */
    private ThingType createRetailGlobalPriorityThingType() throws NonUniqueResultException {
        Group rootGroup = GroupService.getInstance().get(1L);
        User rootUser = UserService.getInstance().getRootUser();
        Group mojix = GroupService.getInstance().get(2L);
        Group sm = GroupService.getInstance().get(3L);

        String thingTypeCode = "RetailGlobalPriority";

        ThingType retailGlobalPriority = PopDBIOTUtils.popThingType(rootGroup, null, "Retail Global Priority");

        retailGlobalPriority.setThingTypeCode(thingTypeCode);
        retailGlobalPriority.setAutoCreate(true);
        ThingTypeService.getInstance().update(retailGlobalPriority);
        PopDBIOTUtils.popThingTypeField(retailGlobalPriority,
                "end",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_NUMBER.value,
                true,
                null,
                null,
                null);

        PopDBIOTUtils.popThingTypeField(retailGlobalPriority,
                "icon",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);

        PopDBIOTUtils.popThingTypeField(retailGlobalPriority,
                "label",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);

        PopDBIOTUtils.popThingTypeField(retailGlobalPriority,
                "start",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_NUMBER.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailGlobalPriority,
                "status",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);

        PopDBIOTUtils.popThingTypeField(retailGlobalPriority,
                "type",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);

        return retailGlobalPriority;
    }

    private void populateRetailGlobalPriorityThings(Date storageDate, ThingType globalPriorityThingType) {

        Map<String, Map<String, String>> globalPriorities = new HashMap<String, Map<String, String>>();

        Map<String, String> valuesGlobalPriority1 = new HashMap<String, String>();
        valuesGlobalPriority1.put("end", "");
        valuesGlobalPriority1.put("icon", "priority-high");
        valuesGlobalPriority1.put("label", "high");
        valuesGlobalPriority1.put("start", "5001");
        valuesGlobalPriority1.put("status", "Active");
        valuesGlobalPriority1.put("type", "Price");
        globalPriorities.put("PriorityHighPrice", valuesGlobalPriority1);

        Map<String, String> valuesGlobalPriority2 = new HashMap<String, String>();
        valuesGlobalPriority2.put("end", "");
        valuesGlobalPriority2.put("icon", "priority-high");
        valuesGlobalPriority2.put("label", "high");
        valuesGlobalPriority2.put("start", "71");
        valuesGlobalPriority2.put("status", "Active");
        valuesGlobalPriority2.put("type", "Quantity");
        globalPriorities.put("PriorityHighQuantity", valuesGlobalPriority2);

        Map<String, String> valuesGlobalPriority3 = new HashMap<String, String>();
        valuesGlobalPriority3.put("end", "3000");
        valuesGlobalPriority3.put("icon", "priority-low");
        valuesGlobalPriority3.put("label", "low");
        valuesGlobalPriority3.put("start", "0");
        valuesGlobalPriority3.put("status", "Active");
        valuesGlobalPriority3.put("type", "Price");
        globalPriorities.put("PriorityLowPrice", valuesGlobalPriority3);

        Map<String, String> valuesGlobalPriority4 = new HashMap<String, String>();
        valuesGlobalPriority4.put("end", "50");
        valuesGlobalPriority4.put("icon", "priority-low");
        valuesGlobalPriority4.put("label", "low");
        valuesGlobalPriority4.put("start", "0");
        valuesGlobalPriority4.put("status", "Active");
        valuesGlobalPriority4.put("type", "Quantity");
        globalPriorities.put("PriorityLowQuantity", valuesGlobalPriority4);

        Map<String, String> valuesGlobalPriority5 = new HashMap<String, String>();
        valuesGlobalPriority5.put("end", "5000");
        valuesGlobalPriority5.put("icon", "priority-medium");
        valuesGlobalPriority5.put("label", "medium");
        valuesGlobalPriority5.put("start", "3001");
        valuesGlobalPriority5.put("status", "Active");
        valuesGlobalPriority5.put("type", "Price");
        globalPriorities.put("PriorityMediumPrice", valuesGlobalPriority5);

        Map<String, String> valuesGlobalPriority6 = new HashMap<String, String>();
        valuesGlobalPriority6.put("end", "70");
        valuesGlobalPriority6.put("icon", "priority-medium");
        valuesGlobalPriority6.put("label", "medium");
        valuesGlobalPriority6.put("start", "51");
        valuesGlobalPriority6.put("status", "Active");
        valuesGlobalPriority6.put("type", "Quantity");
        globalPriorities.put("PriorityMediumQuantity", valuesGlobalPriority6);

        for (String key : globalPriorities.keySet()) {
            Map<String, String> valuesGlobalPriority = globalPriorities.get(key);
            Map<String, Object> udfs = new HashMap<String, Object>();
            udfs = getRetailGlobalPriorityUdf(storageDate.getTime(), globalPriorityThingType, valuesGlobalPriority);

            ThingsService.getInstance()
                    .create(new Stack<Long>(),
                            globalPriorityThingType.getThingTypeCode(),
                            GroupService.getInstance().get(3L).getHierarchyName(
                                    false),
                            key,
                            key.toUpperCase(),
                            null,
                            udfs,
                            null,
                            null,
                            false,
                            false,
                            storageDate,
                            true,
                            true);

        }
    }

    private Map<String, Object> getRetailGlobalPriorityUdf(Long storageTime, ThingType globalPriorityThingType, Map<String, String> valuesGlobalPriority) {

        Map<String, Object> udf = new HashMap<String, Object>();
        Map<String, Object> fieldMap = new HashMap<String, Object>();
        for (ThingTypeField field : globalPriorityThingType.getThingTypeFields()) {
            fieldMap = new HashMap<String, Object>();
            switch (field.getName().toString()) {
                case ("end"): {
                    fieldMap.put("thingTypeFieldId", field.getId());
                    fieldMap.put("value", valuesGlobalPriority.get("end"));
                    fieldMap.put("time", storageTime);

                    udf.put(field.getName().toString(), fieldMap);
                    break;
                }

                case ("icon"): {
                    fieldMap.put("thingTypeFieldId", field.getId());
                    fieldMap.put("value", valuesGlobalPriority.get("icon"));
                    fieldMap.put("time", storageTime);

                    udf.put(field.getName().toString(), fieldMap);
                    break;
                }
                case ("label"): {
                    fieldMap.put("thingTypeFieldId", field.getId());
                    fieldMap.put("value", valuesGlobalPriority.get("label"));
                    fieldMap.put("time", storageTime);

                    udf.put(field.getName().toString(), fieldMap);
                    break;
                }

                case ("start"): {
                    fieldMap.put("thingTypeFieldId", field.getId());
                    fieldMap.put("value", valuesGlobalPriority.get("start"));
                    fieldMap.put("time", storageTime);

                    udf.put(field.getName().toString(), fieldMap);
                    break;
                }

                case ("status"): {
                    fieldMap.put("thingTypeFieldId", field.getId());
                    fieldMap.put("value", valuesGlobalPriority.get("status"));
                    fieldMap.put("time", storageTime);

                    udf.put(field.getName().toString(), fieldMap);
                    break;
                }

                case ("type"): {
                    fieldMap.put("thingTypeFieldId", field.getId());
                    fieldMap.put("value", valuesGlobalPriority.get("type"));
                    fieldMap.put("time", storageTime);

                    udf.put(field.getName().toString(), fieldMap);
                    break;
                }
            }
        }
        return udf;
    }

    /*
    * Retail Global Group Category
    */
    private void createRetailGroupCategoryThingType() throws NonUniqueResultException {
        Group rootGroup = GroupService.getInstance().get(1L);
        User rootUser = UserService.getInstance().getRootUser();
        Group mojix = GroupService.getInstance().get(2L);
        Group sm = GroupService.getInstance().get(3L);

        String thingTypeCode = "RetailGroupCategory";

        ThingType retailGroupCategory = PopDBIOTUtils.popThingType(rootGroup, null, "Retail Group Category");

        retailGroupCategory.setThingTypeCode(thingTypeCode);
        retailGroupCategory.setAutoCreate(true);
        ThingTypeService.getInstance().update(retailGroupCategory);
        PopDBIOTUtils.popThingTypeField(retailGroupCategory,
                "retailGroup",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);

        PopDBIOTUtils.popThingTypeField(retailGroupCategory,
                "retailParentGroup",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
    }

    /*
    * Retail HotReplenishment
    */
    private void createRetailHotReplenishmentThingType() throws NonUniqueResultException {
        Group rootGroup = GroupService.getInstance().get(1L);
        User rootUser = UserService.getInstance().getRootUser();
        Group mojix = GroupService.getInstance().get(2L);
        Group sm = GroupService.getInstance().get(3L);

        String thingTypeCode = "RetailHotReplenishment";

        ThingType retailHotReplenishment = PopDBIOTUtils.popThingType(rootGroup, null, "Retail Hot Replenishment");

        retailHotReplenishment.setThingTypeCode(thingTypeCode);
        retailHotReplenishment.setAutoCreate(true);
        ThingTypeService.getInstance().update(retailHotReplenishment);
        PopDBIOTUtils.popThingTypeField(retailHotReplenishment,
                "itemCategory",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);

        PopDBIOTUtils.popThingTypeField(retailHotReplenishment,
                "itemColor",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);

        PopDBIOTUtils.popThingTypeField(retailHotReplenishment,
                "itemPrice",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailHotReplenishment,
                "itemSize",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailHotReplenishment,
                "itemStoreCode",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailHotReplenishment,
                "itemStoreName",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailHotReplenishment,
                "itemSubCategory",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailHotReplenishment,
                "itemUpc",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailHotReplenishment,
                "replenishBackCount",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_NUMBER.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailHotReplenishment,
                "replenishBackZone",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailHotReplenishment,
                "replenishBackZoneId",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_NUMBER.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailHotReplenishment,
                "replenishDepartmentCode",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailHotReplenishment,
                "replenishDepartmentName",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailHotReplenishment,
                "replenishFrontCount",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_NUMBER.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailHotReplenishment,
                "replenishFrontZone",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailHotReplenishment,
                "replenishFrontZoneId",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_NUMBER.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailHotReplenishment,
                "replenishLastDate",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailHotReplenishment,
                "replenishMax",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailHotReplenishment,
                "replenishMin",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailHotReplenishment,
                "replenishQuantity",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_NUMBER.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailHotReplenishment,
                "replenishQuantityDone",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailHotReplenishment,
                "replenishStatus",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailHotReplenishment,
                "replenishUser",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);

    }

    /*
    * Retail Replenishment
    */
    private void createRetailReplenishmentThingType() throws NonUniqueResultException {
        Group rootGroup = GroupService.getInstance().get(1L);
        User rootUser = UserService.getInstance().getRootUser();
        Group mojix = GroupService.getInstance().get(2L);
        Group sm = GroupService.getInstance().get(3L);

        String thingTypeCode = "RetailReplenishment";

        ThingType retailReplenishment = PopDBIOTUtils.popThingType(rootGroup, null, "Retail Replenishment");

        retailReplenishment.setThingTypeCode(thingTypeCode);
        retailReplenishment.setAutoCreate(true);
        ThingTypeService.getInstance().update(retailReplenishment);
        PopDBIOTUtils.popThingTypeField(retailReplenishment,
                "itemCategory",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);

        PopDBIOTUtils.popThingTypeField(retailReplenishment,
                "itemColor",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);

        PopDBIOTUtils.popThingTypeField(retailReplenishment,
                "itemPrice",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailReplenishment,
                "itemSize",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailReplenishment,
                "itemStoreCode",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailReplenishment,
                "itemStoreName",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailReplenishment,
                "itemSubCategory",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailReplenishment,
                "itemUpc",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailReplenishment,
                "replenishBackCount",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_NUMBER.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailReplenishment,
                "replenishBackZone",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailReplenishment,
                "replenishBackZoneId",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_NUMBER.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailReplenishment,
                "replenishDepartmentCode",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailReplenishment,
                "replenishDepartmentName",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailReplenishment,
                "replenishFrontCount",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_NUMBER.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailReplenishment,
                "replenishFrontZone",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailReplenishment,
                "replenishFrontZoneId",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_NUMBER.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailReplenishment,
                "replenishLastDate",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailReplenishment,
                "replenishMax",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailReplenishment,
                "replenishMin",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailReplenishment,
                "replenishQuantity",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_NUMBER.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailReplenishment,
                "replenishQuantityDone",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailReplenishment,
                "replenishStatus",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailReplenishment,
                "replenishUser",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);

    }

    /*
    * Retail DressingRoom
    */
    private void createRetailDressingRoomThingType() throws NonUniqueResultException {
        Group rootGroup = GroupService.getInstance().get(1L);
        User rootUser = UserService.getInstance().getRootUser();
        Group mojix = GroupService.getInstance().get(2L);
        Group sm = GroupService.getInstance().get(3L);

        String thingTypeCode = "RetailDressingRoom";

        ThingType retailDressingRoom = PopDBIOTUtils.popThingType(rootGroup, null, "Retail Dressing Room");

        retailDressingRoom.setThingTypeCode(thingTypeCode);
        retailDressingRoom.setAutoCreate(true);
        ThingTypeService.getInstance().update(retailDressingRoom);
        PopDBIOTUtils.popThingTypeField(retailDressingRoom,
                "itemCategory",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);

        PopDBIOTUtils.popThingTypeField(retailDressingRoom,
                "itemColor",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailDressingRoom,
                "itemHexadecimal",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);

        PopDBIOTUtils.popThingTypeField(retailDressingRoom,
                "itemPrice",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailDressingRoom,
                "itemSize",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailDressingRoom,
                "itemStoreCode",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailDressingRoom,
                "itemStoreName",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailDressingRoom,
                "itemSubCategory",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailDressingRoom,
                "itemUpc",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);


        PopDBIOTUtils.popThingTypeField(retailDressingRoom,
                "replenishDepartmentCode",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailDressingRoom,
                "replenishDepartmentName",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailDressingRoom,
                "replenishDressingCount",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_NUMBER.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailDressingRoom,
                "replenishDressingZone",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailDressingRoom,
                "replenishFrontCount",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_NUMBER.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailDressingRoom,
                "replenishFrontZone",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailDressingRoom,
                "replenishFrontZoneId",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_NUMBER.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailDressingRoom,
                "replenishLastDate",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailDressingRoom,
                "replenishMax",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailDressingRoom,
                "replenishMin",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailDressingRoom,
                "replenishQuantity",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_NUMBER.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailDressingRoom,
                "replenishStatus",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailDressingRoom,
                "replenishUser",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);

    }

    /*
    * Retail ReportColumnsSettings
    */
    private ThingType createRetailReportColumnsSettingsThingType() throws NonUniqueResultException {
        Group rootGroup = GroupService.getInstance().get(1L);
        User rootUser = UserService.getInstance().getRootUser();
        Group mojix = GroupService.getInstance().get(2L);
        Group sm = GroupService.getInstance().get(3L);

        String thingTypeCode = "RetailReportColumns";

        ThingType retailReportColumns = PopDBIOTUtils.popThingType(rootGroup, null, "Retail Report Columns Settings");

        retailReportColumns.setThingTypeCode(thingTypeCode);
        retailReportColumns.setAutoCreate(true);
        ThingTypeService.getInstance().update(retailReportColumns);
        PopDBIOTUtils.popThingTypeField(retailReportColumns,
                "dataType",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);

        PopDBIOTUtils.popThingTypeField(retailReportColumns,
                "enableColumnMenu",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_BOOLEAN.value,
                true,
                "false",
                null,
                null);

        PopDBIOTUtils.popThingTypeField(retailReportColumns,
                "fieldName",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);

        PopDBIOTUtils.popThingTypeField(retailReportColumns,
                "isVisible",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_BOOLEAN.value,
                true,
                "false",
                null,
                null);

        PopDBIOTUtils.popThingTypeField(retailReportColumns,
                "reportType",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
        PopDBIOTUtils.popThingTypeField(retailReportColumns,
                "status",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                "Active",
                null,
                null);

        return retailReportColumns;

    }

    private void populateRetailReportColumnsSettingsThings(Date storageDate, ThingType columnsSettingsThingType) {

        Map<String, Map<String, String>> columnsSettings = new HashMap<String, Map<String, String>>();

        Map<String, String> values1 = new HashMap<String, String>();
        values1.put("dataType", "string");
        values1.put("enableColumnMenu", "false");
        values1.put("fieldName", "backroom");
        values1.put("isVisible", "false");
        values1.put("reportType", "Inventory");
        values1.put("status", "Active");
        columnsSettings.put("IRCBACKROOM", values1);

        Map<String, String> values2 = new HashMap<String, String>();
        values2.put("dataType", "string");
        values2.put("enableColumnMenu", "false");
        values2.put("fieldName", "backroom");
        values2.put("isVisible", "false");
        values2.put("reportType", "Replenishment");
        values2.put("status", "Active");
        columnsSettings.put("RRCBACKROOM", values2);

        Map<String, String> values3 = new HashMap<String, String>();
        values3.put("dataType", "string");
        values3.put("enableColumnMenu", "false");
        values3.put("fieldName", "backroom");
        values3.put("isVisible", "false");
        values3.put("reportType", "OutOfStock");
        values3.put("status", "Active");
        columnsSettings.put("OOSRCBACKROOM", values3);

        Map<String, String> values4 = new HashMap<String, String>();
        values4.put("dataType", "string");
        values4.put("enableColumnMenu", "false");
        values4.put("fieldName", "backroom");
        values4.put("isVisible", "false");
        values4.put("reportType", "SellThruReplenishment");
        values4.put("status", "Active");
        columnsSettings.put("STRRCBACKROOM", values4);

        Map<String, String> values5 = new HashMap<String, String>();
        values5.put("dataType", "string");
        values5.put("enableColumnMenu", "false");
        values5.put("fieldName", "backroom");
        values5.put("isVisible", "false");
        values5.put("reportType", "RetiredItems");
        values5.put("status", "Active");
        columnsSettings.put("RIRCBACKROOM", values5);

        Map<String, String> values6 = new HashMap<String, String>();
        values6.put("dataType", "string");
        values6.put("enableColumnMenu", "false");
        values6.put("fieldName", "color");
        values6.put("isVisible", "false");
        values6.put("reportType", "Inventory");
        values6.put("status", "Active");
        columnsSettings.put("IRCCOLOR", values6);

        Map<String, String> values7 = new HashMap<String, String>();
        values7.put("dataType", "string");
        values7.put("enableColumnMenu", "false");
        values7.put("fieldName", "color");
        values7.put("isVisible", "false");
        values7.put("reportType", "Replenishment");
        values7.put("status", "Active");
        columnsSettings.put("RRCCOLOR", values7);

        Map<String, String> values8 = new HashMap<String, String>();
        values8.put("dataType", "string");
        values8.put("enableColumnMenu", "false");
        values8.put("fieldName", "color");
        values8.put("isVisible", "false");
        values8.put("reportType", "OutOfStock");
        values8.put("status", "Active");
        columnsSettings.put("OOSRCCOLOR", values8);

        Map<String, String> values9 = new HashMap<String, String>();
        values9.put("dataType", "string");
        values9.put("enableColumnMenu", "false");
        values9.put("fieldName", "color");
        values9.put("isVisible", "false");
        values9.put("reportType", "SellThruReplenishment");
        values9.put("status", "Active");
        columnsSettings.put("STRRCCOLOR", values9);

        Map<String, String> values10 = new HashMap<String, String>();
        values10.put("dataType", "string");
        values10.put("enableColumnMenu", "false");
        values10.put("fieldName", "color");
        values10.put("isVisible", "false");
        values10.put("reportType", "RetiredItems");
        values10.put("status", "Active");
        columnsSettings.put("RIRCCOLOR", values10);

        Map<String, String> values11 = new HashMap<String, String>();
        values11.put("dataType", "string");
        values11.put("enableColumnMenu", "false");
        values11.put("fieldName", "daysoos");
        values11.put("isVisible", "false");
        values11.put("reportType", "OutOfStock");
        values11.put("status", "Active");
        columnsSettings.put("OOSRCDAYSOOS", values11);

        Map<String, String> values12 = new HashMap<String, String>();
        values12.put("dataType", "string");
        values12.put("enableColumnMenu", "false");
        values12.put("fieldName", "departmentName");
        values12.put("isVisible", "false");
        values12.put("reportType", "Inventory");
        values12.put("status", "Active");
        columnsSettings.put("IRCDEPTNAME", values12);

        Map<String, String> values13 = new HashMap<String, String>();
        values13.put("dataType", "string");
        values13.put("enableColumnMenu", "false");
        values13.put("fieldName", "departmentName");
        values13.put("isVisible", "false");
        values13.put("reportType", "Replenishment");
        values13.put("status", "Active");
        columnsSettings.put("RRCDEPTNAME", values13);

        Map<String, String> values14 = new HashMap<String, String>();
        values14.put("dataType", "string");
        values14.put("enableColumnMenu", "false");
        values14.put("fieldName", "departmentName");
        values14.put("isVisible", "false");
        values14.put("reportType", "OutOfStock");
        values14.put("status", "Active");
        columnsSettings.put("OOSRCDEPTNAME", values14);

        Map<String, String> values15 = new HashMap<String, String>();
        values15.put("dataType", "string");
        values15.put("enableColumnMenu", "false");
        values15.put("fieldName", "departmentName");
        values15.put("isVisible", "false");
        values15.put("reportType", "SellThruReplenishment");
        values15.put("status", "Active");
        columnsSettings.put("STRRCDEPTNAME", values15);

        Map<String, String> values16 = new HashMap<String, String>();
        values16.put("dataType", "string");
        values16.put("enableColumnMenu", "false");
        values16.put("fieldName", "departmentName");
        values16.put("isVisible", "false");
        values16.put("reportType", "RetiredItems");
        values16.put("status", "Active");
        columnsSettings.put("RIRCDEPTNAME", values16);

        Map<String, String> values17 = new HashMap<String, String>();
        values17.put("dataType", "string");
        values17.put("enableColumnMenu", "false");
        values17.put("fieldName", "dressingRoom");
        values17.put("isVisible", "false");
        values17.put("reportType", "Inventory");
        values17.put("status", "Active");
        columnsSettings.put("IRCDRESSINGROOM", values17);

        Map<String, String> values18 = new HashMap<String, String>();
        values18.put("dataType", "string");
        values18.put("enableColumnMenu", "false");
        values18.put("fieldName", "productName");
        values18.put("isVisible", "false");
        values18.put("reportType", "Inventory");
        values18.put("status", "Active");
        columnsSettings.put("IRCPRODUCTNAME", values18);

        Map<String, String> values19 = new HashMap<String, String>();
        values19.put("dataType", "string");
        values19.put("enableColumnMenu", "false");
        values19.put("fieldName", "productName");
        values19.put("isVisible", "false");
        values19.put("reportType", "Replenishment");
        values19.put("status", "Active");
        columnsSettings.put("RRCPRODUCTNAME", values19);

        Map<String, String> values20 = new HashMap<String, String>();
        values20.put("dataType", "string");
        values20.put("enableColumnMenu", "false");
        values20.put("fieldName", "productName");
        values20.put("isVisible", "false");
        values20.put("reportType", "OutOfStock");
        values20.put("status", "Active");
        columnsSettings.put("OOSRCPRODUCTNAME", values20);


        Map<String, String> values21 = new HashMap<String, String>();
        values21.put("dataType", "string");
        values21.put("enableColumnMenu", "false");
        values21.put("fieldName", "productName");
        values21.put("isVisible", "false");
        values21.put("reportType", "SellThruReplenishment");
        values21.put("status", "Active");
        columnsSettings.put("STRRCPRODUCTNAME", values21);

        Map<String, String> values22 = new HashMap<String, String>();
        values22.put("dataType", "string");
        values22.put("enableColumnMenu", "false");
        values22.put("fieldName", "productName");
        values22.put("isVisible", "false");
        values22.put("reportType", "RetiredItems");
        values22.put("status", "Active");
        columnsSettings.put("RIRCPRODUCTNAME", values22);

        Map<String, String> values23 = new HashMap<String, String>();
        values23.put("dataType", "string");
        values23.put("enableColumnMenu", "false");
        values23.put("fieldName", "reason");
        values23.put("isVisible", "false");
        values23.put("reportType", "RetiredItems");
        values23.put("status", "Active");
        columnsSettings.put("RIRCREASON", values23);

        Map<String, String> values24 = new HashMap<String, String>();
        values24.put("dataType", "string");
        values24.put("enableColumnMenu", "false");
        values24.put("fieldName", "restock");
        values24.put("isVisible", "false");
        values24.put("reportType", "Inventory");
        values24.put("status", "Active");
        columnsSettings.put("IRCRESTOCK", values24);

        Map<String, String> values25 = new HashMap<String, String>();
        values25.put("dataType", "string");
        values25.put("enableColumnMenu", "false");
        values25.put("fieldName", "restock");
        values25.put("isVisible", "false");
        values25.put("reportType", "Replenishment");
        values25.put("status", "Active");
        columnsSettings.put("RRCRESTOCK", values25);

        Map<String, String> values26 = new HashMap<String, String>();
        values26.put("dataType", "string");
        values26.put("enableColumnMenu", "false");
        values26.put("fieldName", "restock");
        values26.put("isVisible", "false");
        values26.put("reportType", "OutOfStock");
        values26.put("status", "Active");
        columnsSettings.put("OOSRCRESTOCK", values26);

        Map<String, String> values27 = new HashMap<String, String>();
        values27.put("dataType", "string");
        values27.put("enableColumnMenu", "false");
        values27.put("fieldName", "restock");
        values27.put("isVisible", "false");
        values27.put("reportType", "SellThruReplenishment");
        values27.put("status", "Active");
        columnsSettings.put("STRRCRESTOCK", values27);

        Map<String, String> values28 = new HashMap<String, String>();
        values28.put("dataType", "string");
        values28.put("enableColumnMenu", "false");
        values28.put("fieldName", "restock");
        values28.put("isVisible", "false");
        values28.put("reportType", "RetiredItems");
        values28.put("status", "Active");
        columnsSettings.put("RIRCRESTOCK", values28);

        Map<String, String> values29 = new HashMap<String, String>();
        values29.put("dataType", "string");
        values29.put("enableColumnMenu", "false");
        values29.put("fieldName", "sku");
        values29.put("isVisible", "false");
        values29.put("reportType", "Inventory");
        values29.put("status", "Active");
        columnsSettings.put("IRCSKU", values29);

        Map<String, String> values30 = new HashMap<String, String>();
        values30.put("dataType", "string");
        values30.put("enableColumnMenu", "false");
        values30.put("fieldName", "sku");
        values30.put("isVisible", "false");
        values30.put("reportType", "Replenishment");
        values30.put("status", "Active");
        columnsSettings.put("RRCSKU", values30);

        Map<String, String> values31 = new HashMap<String, String>();
        values31.put("dataType", "string");
        values31.put("enableColumnMenu", "false");
        values31.put("fieldName", "sku");
        values31.put("isVisible", "false");
        values31.put("reportType", "OutOfStock");
        values31.put("status", "Active");
        columnsSettings.put("OOSRCSKU", values31);

        Map<String, String> values32 = new HashMap<String, String>();
        values32.put("dataType", "string");
        values32.put("enableColumnMenu", "false");
        values32.put("fieldName", "sku");
        values32.put("isVisible", "false");
        values32.put("reportType", "SellThruReplenishment");
        values32.put("status", "Active");
        columnsSettings.put("STRRCSKU", values32);

        Map<String, String> values33 = new HashMap<String, String>();
        values33.put("dataType", "string");
        values33.put("enableColumnMenu", "false");
        values33.put("fieldName", "sku");
        values33.put("isVisible", "false");
        values33.put("reportType", "RetiredItems");
        values33.put("status", "Active");
        columnsSettings.put("RIRCSKU", values33);

        Map<String, String> values34 = new HashMap<String, String>();
        values34.put("dataType", "string");
        values34.put("enableColumnMenu", "false");
        values34.put("fieldName", "sellfloor");
        values34.put("isVisible", "false");
        values34.put("reportType", "Inventory");
        values34.put("status", "Active");
        columnsSettings.put("IRCSALESFLOOR", values34);


        Map<String, String> values35 = new HashMap<String, String>();
        values35.put("dataType", "string");
        values35.put("enableColumnMenu", "false");
        values35.put("fieldName", "sellfloor");
        values35.put("isVisible", "false");
        values35.put("reportType", "Replenishment");
        values35.put("status", "Active");
        columnsSettings.put("RRCSALESFLOOR", values35);

        Map<String, String> values36 = new HashMap<String, String>();
        values36.put("dataType", "string");
        values36.put("enableColumnMenu", "false");
        values36.put("fieldName", "sellfloor");
        values36.put("isVisible", "false");
        values36.put("reportType", "OutOfStock");
        values36.put("status", "Active");
        columnsSettings.put("OOSRCSALESFLOOR", values36);

        Map<String, String> values37 = new HashMap<String, String>();
        values37.put("dataType", "string");
        values37.put("enableColumnMenu", "false");
        values37.put("fieldName", "sellfloor");
        values37.put("isVisible", "false");
        values37.put("reportType", "SellThruReplenishment");
        values37.put("status", "Active");
        columnsSettings.put("STRRCSALESFLOOR", values37);

        Map<String, String> values38 = new HashMap<String, String>();
        values38.put("dataType", "string");
        values38.put("enableColumnMenu", "false");
        values38.put("fieldName", "sellfloor");
        values38.put("isVisible", "false");
        values38.put("reportType", "RetiredItems");
        values38.put("status", "Active");
        columnsSettings.put("RIRCSALESFLOOR", values38);

        Map<String, String> values39 = new HashMap<String, String>();
        values39.put("dataType", "string");
        values39.put("enableColumnMenu", "false");
        values39.put("fieldName", "size");
        values39.put("isVisible", "false");
        values39.put("reportType", "Inventory");
        values39.put("status", "Active");
        columnsSettings.put("IRCSIZE", values39);

        Map<String, String> values40 = new HashMap<String, String>();
        values3.put("dataType", "string");
        values3.put("enableColumnMenu", "false");
        values3.put("fieldName", "size");
        values3.put("isVisible", "false");
        values3.put("reportType", "Replenishment");
        values3.put("status", "Active");
        columnsSettings.put("RRCSIZE", values40);

        Map<String, String> values41 = new HashMap<String, String>();
        values3.put("dataType", "string");
        values3.put("enableColumnMenu", "false");
        values3.put("fieldName", "size");
        values3.put("isVisible", "false");
        values3.put("reportType", "OutOfStock");
        values3.put("status", "Active");
        columnsSettings.put("OOSRCSIZE", values41);

        Map<String, String> values42 = new HashMap<String, String>();
        values42.put("dataType", "string");
        values42.put("enableColumnMenu", "false");
        values42.put("fieldName", "size");
        values42.put("isVisible", "false");
        values42.put("reportType", "SellThruReplenishment");
        values42.put("status", "Active");
        columnsSettings.put("STRRCSIZE", values42);

        Map<String, String> values43 = new HashMap<String, String>();
        values43.put("dataType", "string");
        values43.put("enableColumnMenu", "false");
        values43.put("fieldName", "size");
        values43.put("isVisible", "false");
        values43.put("reportType", "RetiredItems");
        values43.put("status", "Active");
        columnsSettings.put("RIRCSIZE", values43);

        Map<String, String> values44 = new HashMap<String, String>();
        values44.put("dataType", "string");
        values44.put("enableColumnMenu", "false");
        values44.put("fieldName", "sold");
        values44.put("isVisible", "false");
        values44.put("reportType", "SellThruReplenishment");
        values44.put("status", "Active");
        columnsSettings.put("STRRCSOLD", values44);

        Map<String, String> values45 = new HashMap<String, String>();
        values45.put("dataType", "string");
        values45.put("enableColumnMenu", "false");
        values45.put("fieldName", "target");
        values45.put("isVisible", "false");
        values45.put("reportType", "Inventory");
        values45.put("status", "Active");
        columnsSettings.put("IRCTARGET", values45);

        Map<String, String> values46 = new HashMap<String, String>();
        values46.put("dataType", "string");
        values46.put("enableColumnMenu", "false");
        values46.put("fieldName", "target");
        values46.put("isVisible", "false");
        values46.put("reportType", "Replenishment");
        values46.put("status", "Active");
        columnsSettings.put("RRCTARGET", values46);

        Map<String, String> values47 = new HashMap<String, String>();
        values47.put("dataType", "string");
        values47.put("enableColumnMenu", "false");
        values47.put("fieldName", "target");
        values47.put("isVisible", "false");
        values47.put("reportType", "OutOfStock");
        values47.put("status", "Active");
        columnsSettings.put("OOSRCTARGET", values47);

        Map<String, String> values48 = new HashMap<String, String>();
        values48.put("dataType", "string");
        values48.put("enableColumnMenu", "false");
        values48.put("fieldName", "target");
        values48.put("isVisible", "false");
        values48.put("reportType", "SellThruReplenishment");
        values48.put("status", "Active");
        columnsSettings.put("STRRCTARGET", values48);

        Map<String, String> values49 = new HashMap<String, String>();
        values49.put("dataType", "string");
        values49.put("enableColumnMenu", "false");
        values49.put("fieldName", "total");
        values49.put("isVisible", "false");
        values49.put("reportType", "Inventory");
        values49.put("status", "Active");
        columnsSettings.put("IRCTOTAL", values49);

        for (String key : columnsSettings.keySet()) {
            Map<String, String> valuesColumnsSettings = columnsSettings.get(key);
            Map<String, Object> udfs = new HashMap<String, Object>();
            udfs = getRetailReportColumnsSettingsUdf(storageDate.getTime(), columnsSettingsThingType, valuesColumnsSettings);

            ThingsService.getInstance()
                    .create(new Stack<Long>(),
                            columnsSettingsThingType.getThingTypeCode(),
                            GroupService.getInstance().get(3L).getHierarchyName(
                                    false),
                            key,
                            key.toUpperCase(),
                            null,
                            udfs,
                            null,
                            null,
                            false,
                            false,
                            storageDate,
                            true,
                            true);

        }
    }

    private Map<String, Object> getRetailReportColumnsSettingsUdf(Long storageTime, ThingType columnsSettingsThingType, Map<String, String> valuesColumnsSettings) {

        Map<String, Object> udf = new HashMap<String, Object>();
        Map<String, Object> fieldMap = new HashMap<String, Object>();
        for (ThingTypeField field : columnsSettingsThingType.getThingTypeFields()) {
            fieldMap = new HashMap<String, Object>();
            switch (field.getName().toString()) {
                case ("dataType"): {
                    fieldMap.put("thingTypeFieldId", field.getId());
                    fieldMap.put("value", valuesColumnsSettings.get("dataType"));
                    fieldMap.put("time", storageTime);

                    udf.put(field.getName().toString(), fieldMap);
                    break;
                }

                case ("enableColumnMenu"): {
                    fieldMap.put("thingTypeFieldId", field.getId());
                    fieldMap.put("value", valuesColumnsSettings.get("enableColumnMenu"));
                    fieldMap.put("time", storageTime);

                    udf.put(field.getName().toString(), fieldMap);
                    break;
                }
                case ("fieldName"): {
                    fieldMap.put("thingTypeFieldId", field.getId());
                    fieldMap.put("value", valuesColumnsSettings.get("fieldName"));
                    fieldMap.put("time", storageTime);

                    udf.put(field.getName().toString(), fieldMap);
                    break;
                }

                case ("isVisible"): {
                    fieldMap.put("thingTypeFieldId", field.getId());
                    fieldMap.put("value", valuesColumnsSettings.get("isVisible"));
                    fieldMap.put("time", storageTime);

                    udf.put(field.getName().toString(), fieldMap);
                    break;
                }

                case ("status"): {
                    fieldMap.put("thingTypeFieldId", field.getId());
                    fieldMap.put("value", valuesColumnsSettings.get("status"));
                    fieldMap.put("time", storageTime);

                    udf.put(field.getName().toString(), fieldMap);
                    break;
                }

                case ("reportType"): {
                    fieldMap.put("thingTypeFieldId", field.getId());
                    fieldMap.put("value", valuesColumnsSettings.get("reportType"));
                    fieldMap.put("time", storageTime);

                    udf.put(field.getName().toString(), fieldMap);
                    break;
                }
            }
        }
        return udf;
    }

    /*
    * Retail RetailReportList
    */
    private void createRetailReportList() throws NonUniqueResultException {
        Group rootGroup = GroupService.getInstance().get(1L);
        User rootUser = UserService.getInstance().getRootUser();
        Group mojix = GroupService.getInstance().get(2L);
        Group sm = GroupService.getInstance().get(3L);

        String thingTypeCode = "RetailReportList";

        ThingType retailUserSettings = PopDBIOTUtils.popThingType(rootGroup, null, "Retail Report List");

        retailUserSettings.setThingTypeCode(thingTypeCode);
        retailUserSettings.setAutoCreate(true);
        ThingTypeService.getInstance().update(retailUserSettings);
        PopDBIOTUtils.popThingTypeField(retailUserSettings,
                "filter",
                "",
                "",
                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                ThingTypeField.Type.TYPE_TEXT.value,
                true,
                null,
                null,
                null);
    }

    /*
    *
    * */
    private Map<String, Long> createReportDefinitionDataRetail(Group group, GroupType gt, User createdByUser) {
        Map<String, Long> reportsIds = new HashMap<String, Long>();

        reportsIds.put("dressingRoomReportDetail", createRawMongoReport(createdByUser, group, gt, "[Retail] Dressing Room Detail", "/mongo/Retail/retail_dressing_room_detail_groups.js", null));
        reportsIds.put("dressingRoomReportAllDetail", createRawMongoReport(createdByUser, group, gt, "[Retail] Dressing Room Groups", "/mongo/Retail/retail_dressing_room_groups.js", null));
        reportsIds.put("dressingRoomReport", createRawMongoReport(createdByUser, group, gt, "[Retail] Dressing Room Quantity Groups", "/mongo/Retail/retail_dressing_room_quantity_groups.js", null));

        reportsIds.put("replenishGroupsReport", createRawMongoReport(createdByUser, group, gt, "[Retail] Groups All", "/mongo/Retail/retail_groups_all.js", null));

        reportsIds.put("hotReplenishDetailReport", createRawMongoReport(createdByUser, group, gt, "[Retail] Hot Replenishment Detail", "/mongo/Retail/retail_hot_replenishment_detail.js", null));
        reportsIds.put("hotReplenishReport", createRawMongoReport(createdByUser, group, gt, "[Retail] Hot Replenishment Groups", "/mongo/Retail/retail_hot_replenishment_groups.js", null));

        reportsIds.put("reportInventory", createRawMongoReport(createdByUser, group, gt, "[Retail] Inventory Report Groups", "/mongo/Retail/retail_inventory_report_groups.js", null));
        reportsIds.put("reportTotalInventory", createRawMongoReport(createdByUser, group, gt, "[Retail] Inventory Total Report Groups", "/mongo/Retail/retail_inventory_total_report_groups.js", null));

        reportsIds.put("reportOutOfStock", createRawMongoReport(createdByUser, group, gt, "[Retail] Out Of Stock Report", "/mongo/Retail/retail_out_of_stock_report_groups.js", null));

        reportsIds.put("reportRetairedItems", createRawMongoReport(createdByUser, group, gt, "[Retail] Retired Report Groups", "/mongo/Retail/retail_retired_items_groups.js", null));


        reportsIds.put("replenishDetailReport", createRawMongoReport(createdByUser, group, gt, "[Retail] Replenishment Detail", "/mongo/Retail/retail_replenishment_detail.js", null));
        reportsIds.put("replenishReport", createRawMongoReport(createdByUser, group, gt, "[Retail] Replenishment Groups", "/mongo/Retail/retail_replenishment_groups.js", null));
        reportsIds.put("reportReplenishment", createRawMongoReport(createdByUser, group, gt, "[Retail] Replenishment Report Groups", "/mongo/Retail/retail_replenishment_report_groups.js", null));

        reportsIds.put("homeTotalsReport", createRawMongoReport(createdByUser, group, gt, "[Retail] Home Totals", "/mongo/Retail/retail_total_groups.js", null));

        reportsIds.put("sellThruReplenishReport", createRawMongoReport(createdByUser, group, gt, "[Retail] Sell Thru Replenishment Groups", "/mongo/Retail/retail_sell_thru_replenishment_groups.js", null));
        reportsIds.put("reportSellThruReplenishment", createRawMongoReport(createdByUser, group, gt, "[Retail] Sell Thru Replenishment Report Groups", "/mongo/Retail/retail_sell_thru_replenishment_report_groups.js", null));

        try {
            URL fileURL = PopDBMojixRetailApp.class.getClassLoader().getResource("mongo/JsonFormatter.js");
            if (fileURL != null) {
                String text = IOUtils.toString(fileURL, Charset.forName("UTF-8"));
                MongoScriptDAO.getInstance().insertRaw("JSONFormatter", text);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return reportsIds;
    }

    //*********************************************************************************************

    private ReportFilter createReportFilter(String label,
                                            String propertyName,
                                            String propertyOrder,
                                            String operatorFilter,
                                            String value,
                                            Boolean isEditable,
                                            Long ttId,
                                            ReportDefinition reportDefinition) {
        ReportFilter reportFilter = new ReportFilter();
        reportFilter.setLabel(label);
        reportFilter.setPropertyName(propertyName);
        reportFilter.setDisplayOrder(Float.parseFloat(propertyOrder));
        reportFilter.setOperator(operatorFilter);
        reportFilter.setValue(value);
        reportFilter.setEditable(isEditable);
        System.out.println("ThingTypeService******>> " + ThingTypeService.getInstance().get(ttId).getId());
        ThingType thingtype = ThingTypeService.getInstance().get(ttId);
        reportFilter.setThingType(thingtype != null ? thingtype : null);
        System.out.println("ThingTypeFieldService******>> " + ThingTypeFieldService.getInstance().get(ttId).getId());
        reportFilter.setThingTypeField(ThingTypeFieldService.getInstance().getThingTypeFieldByName(propertyName).get
                (0));
        reportFilter.setReportDefinition(reportDefinition);
        System.out.println("reportDefinition******>> " + reportDefinition.getId());
        return reportFilter;
    }

    private static ReportFilter createReportFilter(String label, String propertyName, String propertyOrder, String
            operatorFilter, String value,
                                                   Boolean isEditable, ReportDefinition reportDefinition) {
        ReportFilter reportFilter = new ReportFilter();
        reportFilter.setLabel(label);
        reportFilter.setPropertyName(propertyName);
        reportFilter.setDisplayOrder(Float.parseFloat(propertyOrder));
        reportFilter.setOperator(operatorFilter);
        reportFilter.setValue(value);
        reportFilter.setEditable(isEditable);

        reportFilter.setReportDefinition(reportDefinition);
        return reportFilter;
    }

    private long createRawMongoReport(User createdByUser, Group group, GroupType gt, String name, String fileName,
                                      Map<String, Object[]> reportFilterData) {
        ReportDefinition rd = new ReportDefinition();
        rd.setName(name);
        rd.setCreatedByUser(createdByUser);
        rd.setGroup(group);
        rd.setReportType("mongo");
        rd.setIsMobile(Boolean.FALSE);
        rd.setIsMobileDataEntry(Boolean.FALSE);
        ReportDefinitionConfig rdc = new ReportDefinitionConfig();
        rdc.setReportDefinition(rd);
        rdc.setKeyType("SCRIPT");
        try {
            InputStream is = this.getClass().getResourceAsStream(fileName);
            String text = IOUtils.toString(is, "UTF-8");
            logger.info("SCRIPT=" + text);
            rdc.setKeyValue(text);
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<ReportDefinitionConfig> list = new ArrayList<ReportDefinitionConfig>();
        list.add(rdc);
        rd.setReportDefinitionConfig(list);

        rd.setRunOnLoad(true);

        rd = ReportDefinitionService.getInstance().insert(rd);

        try {
            MongoScriptDAO.getInstance().insert(rd.getId().toString(), rdc.getKeyValue());
        } catch (MongoExecutionException e) {
            e.printStackTrace();
        }

        if (reportFilterData != null && reportFilterData.size() > 0) {
            String[] labelsFilter = (String[]) reportFilterData.get("labelsFilter");
            String[] propertyNamesFilter = (String[]) reportFilterData.get("propertyNamesFilter");
            String[] operatorFilter = (String[]) reportFilterData.get("operatorFilter");
            String[] value = (String[]) reportFilterData.get("value");
            Boolean[] isEditable = (Boolean[]) reportFilterData.get("isEditable");
            String[] propertyOrdersFilter = (String[]) reportFilterData.get("propertyOrdersFilter");
            Long[] thingTypeId = (Long[]) reportFilterData.get("thingTypeFieldId");
            for (int it = 0; it < Array.getLength(labelsFilter); it++) {
                if (thingTypeId == null || thingTypeId.length == 0) {
                    ReportFilterService.getInstance().insert(createReportFilter(labelsFilter[it],
                            propertyNamesFilter[it],
                            propertyOrdersFilter[it], operatorFilter[it], value[it], isEditable[it], rd));
                } else if (thingTypeId[it] != null) {
                    ReportFilterService.getInstance().insert(createReportFilter(labelsFilter[it],
                            propertyNamesFilter[it],
                            propertyOrdersFilter[it], operatorFilter[it], value[it], isEditable[it], thingTypeId[it],
                            rd));
                } else {
                    ReportFilterService.getInstance().insert(createReportFilter(labelsFilter[it],
                            propertyNamesFilter[it],
                            propertyOrdersFilter[it], operatorFilter[it], value[it], isEditable[it], rd));
                }

            }
        }

        return rd.getId();
    }

    public static String getFtpBridgeColourConfiguration(String ftpCon, String brokerCon) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n" +
                "  \"thingTypeCode\": \"colour\",\n" +
                "  \"logRawMessages\": 0,\n" +
                "  \"ftp\": {\n" +
                "    \"connectionCode\": \""+ftpCon+"\"\n" +
                "  },\n" +
                "  \"path\": \"/StoreReferenceData\",\n" +
                "  \"pattern\": \"*.COLOUR\",\n" +
                "  \"patternCaseSensitive\": false,\n" +
                "  \"schedule\": \"0 0/10 * 1/1 * ? *\",\n" +
                "  \"configParser\": {\n" +
                "    \"parserType\": \"fixedlength\",\n" +
                "    \"separator\": null,\n" +
                "    \"fieldLengths\": \"3,16,1\",\n" +
                "    \"ignoreFooter\": true,\n" +
                "    \"ignoreHeader\": false,\n" +
                "    \"fieldNames\": [\n" +
                "      \"Code\",\n" +
                "      \"Description\",\n" +
                "      \"Action\"\n" +
                "    ],\n" +
                "    \"columnNumberAsSerial\": 0\n" +
                "  },\n" +
                "  \"processPolicy\": \"Move\",\n" +
                "  \"localBackupFolder\": \"/tmp\",\n" +
                "  \"ftpDestinationFolder\": \"processed/colour\",\n" +
                "  \"mqtt\": {\n" +
                "    \"connectionCode\": \""+brokerCon+"\"\n" +
                "  }\n" +
                "}");
        return sb.toString();
    }

    public static void populateThingTypePath() {
        List<ThingType> thingTypes = ThingTypeService.getInstance().getAllThingTypes();
        for (ThingType thingType : thingTypes) {
            List<ThingTypeField> thingTypeFields = ThingTypeFieldService.getInstance().getThingTypeField(thingType
                    .getId());
            List<ThingTypeField> thingTypeUdfList = new ArrayList<>();
            for (ThingTypeField thingTypeField : thingTypeFields) {
                if (thingTypeField.isThingTypeUDF()) {
                    thingTypeUdfList.add(thingTypeField);
                }
            }
            ThingTypeService.getInstance().associate(thingType, thingTypeUdfList, null, null, null);
        }
    }

    private void required() throws NonUniqueResultException {
        Group             rootGroup       = GroupService.getInstance().getRootGroup();
        Date              storageDate     = new Date();

        GroupType rootGroupType = GroupTypeService.getInstance().getRootGroupType();

//        GroupType tenantGroupType = new GroupType();
//        tenantGroupType.setGroup(rootGroup);
//        tenantGroupType.setName("Tenant");
//        tenantGroupType.setParent(rootGroupType);
//        tenantGroupType.setCode("tenant");
//        GroupTypeService.getInstance().insert(tenantGroupType);
//
        GroupType tenantGroupType = GroupTypeService.getInstance().getTenantGroupType();
        Group tenantGroup = new Group();
        tenantGroup.setParent(rootGroup);
        tenantGroup.setName("Default Tenant");
        tenantGroup.setCode("DT");
        tenantGroup.setGroupType(tenantGroupType);
        GroupService.getInstance().insert(tenantGroup);

        GroupType facilityGroupType = new GroupType();
        facilityGroupType.setGroup(tenantGroup);
        facilityGroupType.setName("Facility");
        facilityGroupType.setParent(tenantGroupType);
        GroupTypeService.getInstance().insert(facilityGroupType);

        Group facilityGroup = new Group();
        facilityGroup.setParent(tenantGroup);
        facilityGroup.setName("Default Facility");
        facilityGroup.setCode("DF");
        facilityGroup.setGroupType(facilityGroupType);
        GroupService.getInstance().insert(facilityGroup);

        HashSet<Resource> resources = new HashSet<>();

        // Populate Resources and Roles
        Role tenantAdminRole = PopDBUtils.popRole("Tenant Administrator", "TA", resources, tenantGroup,
                tenantGroupType);

        User tenantUser = PopDBUtils.popUser("tenant", "tenant", tenantGroup, tenantAdminRole);
        tenantUser.setFirstName("Tenant");
        tenantUser.setLastName("User");
        tenantUser.setEmail("");
        UserService.getInstance().update(tenantUser);


        //--<REQ-4417>

        // KEEP THESE IN REQUIRED !!!
        ThingType rfid = PopDBIOTUtils.popThingTypeRFID(facilityGroup, "default_rfid_thingtype");
        ThingType gps  = PopDBIOTUtils.popThingTypeGPS(facilityGroup, "default_gps_thingtype");

        //Populate Thing Type ZPL
        PopDBRequiredIOT.populateThingTypeZPL(facilityGroup);

        //Populate Notification Template
        PopDBRequiredIOT.populateNotificationTemplate();

        //Populate Connection
        populateConnection();


        final String[ ] topicMqtt = {"/v1/data/#"};

        String mcbConfig = PopDBRequiredIOT.getOldCoreBridgeConfiguration(topicMqtt, "MQTT", "MONGO");
        Edgebox edgeboxMCB = EdgeboxService.getInstance().insertEdgebox(rootGroup, "* Core Bridge", "MCB", "core", mcbConfig,0L);
        //Create the child RFID
        Map<String, Object> result = ThingsService.getInstance()
                .create(
                        new Stack<Long>(),
                        rfid.getThingTypeCode(),
                        GroupService.getInstance().get(3L).getHierarchyName(
                                false),
                        "RFID1234567890",
                        "RFID1234567890",
                        null,
                        null,
                        null,
                        null,
                        false,
                        false,
                        storageDate,
                        true,
                        true);
        //Create the child GPS
        result = ThingsService.getInstance().create(new Stack<Long>(),
                gps.getThingTypeCode(),
                GroupService.getInstance().get(3L).getHierarchyName(
                        false),
                "GPS1234567890",
                "GPS1234567890",
                null,
                null,
                null,
                null,
                false,
                false,
                storageDate,
                true,
                true);


        // AlienReaderGPIOSubscriber Example
        EdgeboxRule edgeboxRule0 = new EdgeboxRule();
        edgeboxRule0.setName("Door_#1_Light_Buzzer");
        edgeboxRule0.setDescription("Light Buzzer Example");
        edgeboxRule0.setInput("ThingMessage");
        edgeboxRule0.setRule(
                "select * from messageEventType where udf('thingTypeCode') = 'default_rfid_thingtype' and ( udf" +
                        "('logicalReader') = \"Door1\" and udf('status') != \"Sold\"  )");
        edgeboxRule0.setOutput("AlienReaderGPIOSubscriber");
        edgeboxRule0.setOutputConfig(getLightBuzzerRuleActionConfig());
        edgeboxRule0.setActive(false);
        edgeboxRule0.setRunOnReorder(false);
        edgeboxRule0.setEdgebox(edgeboxMCB);
        edgeboxRule0.setCronSchedule("");
        edgeboxRule0.setGroup(facilityGroup);
        edgeboxRule0.setParameterConditionType("CONDITION_TYPE");
        edgeboxRule0.setConditionType("CEP");
        EdgeboxRuleService.getInstance().insert(edgeboxRule0);

        // AlienReaderGPIOSubscriber Example
        EdgeboxRule edgeboxRule = new EdgeboxRule();
        edgeboxRule.setName("Door_#2_Light_Buzzer");
        edgeboxRule.setDescription("Light Buzzer Example");
        edgeboxRule.setInput("ThingMessage");
        edgeboxRule.setRule(
                "select * from messageEventType where udf('thingTypeCode') = 'default_rfid_thingtype' and ( udf" +
                        "('logicalReader') = \"Door2\" and udf('status') != \"Sold\" )");
        edgeboxRule.setOutput("AlienReaderGPIOSubscriber");
        edgeboxRule.setOutputConfig(getLightBuzzerRuleActionConfig());
        edgeboxRule.setActive(false);
        edgeboxRule.setRunOnReorder(false);
        edgeboxRule.setEdgebox(edgeboxMCB);
        edgeboxRule.setCronSchedule("");
        edgeboxRule.setGroup(facilityGroup);
        edgeboxRule.setParameterConditionType("CONDITION_TYPE");
        edgeboxRule.setConditionType("CEP");
        EdgeboxRuleService.getInstance().insert(edgeboxRule);

        // MFRReaderGPIOSubscriber Example
        EdgeboxRule edgeboxRule11 = new EdgeboxRule();
        edgeboxRule11.setName("Door_#1_Light_BuzzerMFR");
        edgeboxRule11.setDescription("Example MFR Subscriber");
        edgeboxRule11.setInput("ThingMessage");
        edgeboxRule11.setRule(
                "select * from messageEventType where udf('thingTypeCode') = 'default_rfid_thingtype' and (  udf" +
                        "('zone') = \"Stockroom\" )");
        edgeboxRule11.setOutput("MFRReaderGPIOSubscriber");
        edgeboxRule11.setOutputConfig(getMFRLightBuzzerRuleActionConfig());
        edgeboxRule11.setActive(false);
        edgeboxRule11.setRunOnReorder(false);
        edgeboxRule11.setEdgebox(edgeboxMCB);
        edgeboxRule11.setGroup(facilityGroup);
        edgeboxRule11.setCronSchedule("");
        edgeboxRule11.setParameterConditionType("CONDITION_TYPE");
        edgeboxRule11.setConditionType("CEP");
        EdgeboxRuleService.getInstance().insert(edgeboxRule11);

        // MFRTurnOffGPIOSubscriber Example
        EdgeboxRule edgeboxRule12 = new EdgeboxRule();
        edgeboxRule12.setName("TurnOff_Light_BuzzerMFR");
        edgeboxRule12.setDescription("Example TurnOff light buzzer MFR Subscriber");
        edgeboxRule12.setInput("ThingMessage");
        edgeboxRule12.setRule(
                "select * from messageEventType where udf('thingTypeCode') = 'default_rfid_thingtype' and ( udf" +
                        "('zone') != \"Stockroom\" )");
        edgeboxRule12.setOutput("MFRTurnOffGPIOSubscriber");
        edgeboxRule12.setOutputConfig(getMFRTurnOffLightBuzzerRuleActionConfig());
        edgeboxRule12.setActive(false);
        edgeboxRule12.setRunOnReorder(false);
        edgeboxRule12.setEdgebox(edgeboxMCB);
        edgeboxRule12.setGroup(facilityGroup);
        edgeboxRule12.setCronSchedule("");
        edgeboxRule12.setParameterConditionType("CONDITION_TYPE");
        edgeboxRule12.setConditionType("CEP");
        EdgeboxRuleService.getInstance().insert(edgeboxRule12);

        // rule example for junit testing, do not modify !!!
        EdgeboxRule edgeboxRule4 = new EdgeboxRule();
        edgeboxRule4.setName("Location_Event_Test");
        edgeboxRule4.setDescription("Sets status to 'BAD' if a thing is not in it's assigned zone");
        edgeboxRule4.setInput("ThingMessage");
        edgeboxRule4.setRule(
                "select * from messageEventType where udf('thingTypeCode') = 'default_rfid_thingtype' and ( udf" +
                        "('logicalReader') != 'LR1' )");
        edgeboxRule4.setOutput("ThingPropertySetter");
        edgeboxRule4.setOutputConfig("status=\"BAD\"");
        edgeboxRule4.setActive(false);
        edgeboxRule4.setRunOnReorder(false);
        edgeboxRule4.setEdgebox(edgeboxMCB);
        edgeboxRule4.setCronSchedule("");
        edgeboxRule4.setGroup(facilityGroup);
        edgeboxRule4.setParameterConditionType("CONDITION_TYPE");
        edgeboxRule4.setConditionType("CEP");
        EdgeboxRuleService.getInstance().insert(edgeboxRule4);

        // rule example for junit testing, do not modify !!!
        EdgeboxRule edgeboxRule5 = new EdgeboxRule();
        edgeboxRule5.setName("Door_Event_Test");
        edgeboxRule5.setDescription("Test rule for junit testing");
        edgeboxRule5.setInput("ThingMessage");
        edgeboxRule5.setRule(
                "select * from messageEventType where udf('thingTypeCode') = 'default_rfid_thingtype' and (  udf" +
                        "('doorEvent') = 'LR1:in' )");
        edgeboxRule5.setOutput("ThingPropertySetter");
        edgeboxRule5.setOutputConfig("status=\"YOU ARE IN\"");
        edgeboxRule5.setActive(false);
        edgeboxRule5.setRunOnReorder(false);
        edgeboxRule5.setEdgebox(edgeboxMCB);
        edgeboxRule5.setParameterConditionType("CONDITION_TYPE");
        edgeboxRule5.setConditionType("CEP");
        edgeboxRule5.setCronSchedule("");
        edgeboxRule5.setGroup(facilityGroup);
        EdgeboxRuleService.getInstance().insert(edgeboxRule5);

        // rule example for junit testing, do not modify !!!
        EdgeboxRule edgeboxRule6 = new EdgeboxRule();
        edgeboxRule6.setName("Point_In_Zone_Test");
        edgeboxRule6.setDescription("Test rule for junit testing");
        edgeboxRule6.setInput("ThingMessage");
        edgeboxRule6.setRule(
                "select * from messageEventType where udf('thingTypeCode') = 'default_rfid_thingtype' and (  udf" +
                        "('zone') = 'PoS' )");
        edgeboxRule6.setOutput("ThingPropertySetter");
        edgeboxRule6.setOutputConfig("status=\"YOU ARE IN POS\"");
        edgeboxRule6.setActive(false);
        edgeboxRule6.setRunOnReorder(false);
        edgeboxRule6.setEdgebox(edgeboxMCB);
        edgeboxRule6.setCronSchedule("");
        edgeboxRule6.setGroup(facilityGroup);
        edgeboxRule6.setParameterConditionType("CONDITION_TYPE");
        edgeboxRule6.setConditionType("CEP");
        EdgeboxRuleService.getInstance().insert(edgeboxRule6);

        // NETAPP rule example
        EdgeboxRule edgeboxRule2 = new EdgeboxRule();
        edgeboxRule2.setName("Out_Of_Assigned_Zone");
        edgeboxRule2.setDescription("Sets status to 'bad' if a thing is not in it's assigned zone");
        edgeboxRule2.setInput("ThingMessage");
        edgeboxRule2.setRule(
                "select * from messageEventType where udf('thingTypeCode') = 'default_rfid_thingtype' and ( udf" +
                        "('zone') != 'assignedZone' )");
        edgeboxRule2.setOutput("ThingPropertySetter");
        edgeboxRule2.setOutputConfig("status='bad'");
        edgeboxRule2.setActive(false);
        edgeboxRule2.setRunOnReorder(false);
        edgeboxRule2.setEdgebox(edgeboxMCB);
        edgeboxRule2.setCronSchedule("");
        edgeboxRule2.setGroup(facilityGroup);
        edgeboxRule2.setParameterConditionType("CONDITION_TYPE");
        edgeboxRule2.setConditionType("CEP");
        EdgeboxRuleService.getInstance().insert(edgeboxRule2);

        // Xively Rule: When a thing is moved to another area, this rule sends the status of the thing to the broker
        // configured.
        EdgeboxRule edgeboxRule7 = new EdgeboxRule();
        edgeboxRule7.setName("MQTTPushSubscriber");
        edgeboxRule7.setDescription(
                "When a thing is moved to another area, this rule sends information from the thing to the broker " +
                        "configured.");
        edgeboxRule7.setInput("ThingMessage");
        edgeboxRule7.setRule(
                "select * from messageEventType where udf('thingTypeCode') = 'default_rfid_thingtype' and (  udf" +
                        "('zone') = 'Stockroom' )");
        edgeboxRule7.setOutput("MQTTPushSubscriber");
        edgeboxRule7.setOutputConfig(
                "{\"host\":\"localhost\",\"port\":1883,\"topic\":\"MQTTDemo\",\"mqtt-body\":\"Serial Number: " +
                        "${serialNumber}. Hi. This is the mqtt message for thing ${name}\"}");
        edgeboxRule7.setActive(false);
        edgeboxRule7.setRunOnReorder(false);
        edgeboxRule7.setEdgebox(edgeboxMCB);
        edgeboxRule7.setCronSchedule("");
        edgeboxRule7.setGroup(facilityGroup);
        edgeboxRule7.setParameterConditionType("CONDITION_TYPE");
        edgeboxRule7.setConditionType("CEP");
        EdgeboxRuleService.getInstance().insert(edgeboxRule7);

        // Xively Rule: When a thing is moved to another area, this rule sends the status of the thing to the broker
        // configured.
        EdgeboxRule edgeboxRule14 = new EdgeboxRule();
        edgeboxRule14.setName("EmailSubscriber");
        edgeboxRule14.setDescription(
                "When a thing is moved to another area, this rule sends an email to recipients configured.");
        edgeboxRule14.setInput("ThingMessage");
        edgeboxRule14.setRule(
                "select * from messageEventType where udf('thingTypeCode') = 'default_rfid_thingtype' and (  udf" +
                        "('zone') = 'Stockroom' )");
        edgeboxRule14.setOutput("EmailSubscriber");
        edgeboxRule14.setOutputConfig(
                "{\"mqtt\": {" +
                        "\"connectionCode\": \"MQTT\"" +
                        "}," + "\"contentType\": \"text/html; charset=utf-8\"," +
                        "\"subject\": \"Subject Serial: ${serialNumber}\"," +
                        "\"to\": [\"administrator@mojix.com\"]," +
                        "\"email-body\": \"Hi. This is an automated message from Vizix from thing: ${name}.\"" +
                        "}");
        edgeboxRule14.setActive(false);
        edgeboxRule14.setRunOnReorder(false);
        edgeboxRule14.setEdgebox(edgeboxMCB);
        edgeboxRule14.setCronSchedule("");
        edgeboxRule14.setGroup(facilityGroup);
        edgeboxRule14.setParameterConditionType("CONDITION_TYPE");
        edgeboxRule14.setConditionType("CEP");
        EdgeboxRuleService.getInstance().insert(edgeboxRule14);

        // Example of rule using gate exit report table of MongoDB (client: Sharaf)
        EdgeboxRule edgeboxRule8 = new EdgeboxRule();
        edgeboxRule8.setName("Exit_Gate_Rule");
        edgeboxRule8.setDescription(
                "Valid values of groupBy are:\n - id or thingId (child/current thing_id)\n - parentId (parent " +
                        "child_id)\n - <any valid udf> (example: zone) \n\n records in table exit_report are snapshots of the" +
                        " " +
                        "thing, but this table only keeps last record");
        edgeboxRule8.setInput("ThingMessage");
        edgeboxRule8.setRule(
                "select * from messageEventType where udf('thingTypeCode') = 'default_rfid_thingtype' and (  udf" +
                        "('zone') = 'Stockroom' )");
        edgeboxRule8.setOutput("ReportGeneratorSubscriber");
        edgeboxRule8.setOutputConfig(getExitGateRuleDefaultActionConfig());
        edgeboxRule8.setActive(false);
        edgeboxRule8.setRunOnReorder(false);
        edgeboxRule8.setEdgebox(edgeboxMCB);
        edgeboxRule8.setCronSchedule("");
        edgeboxRule8.setGroup(facilityGroup);
        edgeboxRule8.setParameterConditionType("CONDITION_TYPE");
        edgeboxRule8.setConditionType("CEP");
        EdgeboxRuleService.getInstance().insert(edgeboxRule8);

        // Example of rule using Rest Endpoint Subscriber (client: Sharaf)
        EdgeboxRule edgeboxRule9 = new EdgeboxRule();
        edgeboxRule9.setName("Example_Rest_Endpoint_Subscriber");
        edgeboxRule9.setDescription("Example Rest Endpoint Subscriber");
        edgeboxRule9.setInput("ThingMessage");
        edgeboxRule9.setRule(
                "select * from messageEventType where udf('thingTypeCode') = 'default_rfid_thingtype' and ( )");
        edgeboxRule9.setOutput("com.tierconnect.riot.bridges.cep.RestEndpointSubscriber");
        edgeboxRule9.setOutputConfig(getRestEndpointSubscriberConfig());
        edgeboxRule9.setActive(false);
        edgeboxRule9.setRunOnReorder(false);
        edgeboxRule9.setEdgebox(edgeboxMCB);
        edgeboxRule9.setCronSchedule( "" );
        edgeboxRule9.setCronSchedule( "" );
        edgeboxRule9.setGroup( facilityGroup );
        edgeboxRule9.setParameterConditionType("CONDITION_TYPE");
        edgeboxRule9.setConditionType("CEP");
        EdgeboxRuleService.getInstance().insert( edgeboxRule9 );

        // Example of rule using exit report table of Cassandra (client: Sharaf)
        EdgeboxRule edgeboxRule10 = new EdgeboxRule();
        edgeboxRule10.setName("Example_Super_Subscriber");
        edgeboxRule10.setDescription(
                "Example Super Subscriber. This allows more than one subscriber to be executed per Esper rule.");
        edgeboxRule10.setInput("ThingMessage");
        edgeboxRule10.setRule(
                "select * from messageEventType where udf('thingTypeCode') = 'default_rfid_thingtype' and (  )");
        edgeboxRule10.setOutput("com.tierconnect.riot.bridges.cep.SuperSubscriber");
        edgeboxRule10.setOutputConfig(getSuperSubscriberConfig());
        edgeboxRule10.setActive(false);
        edgeboxRule10.setRunOnReorder(false);
        edgeboxRule10.setEdgebox(edgeboxMCB);
        edgeboxRule10.setCronSchedule("");
        edgeboxRule10.setGroup(facilityGroup);
        edgeboxRule10.setParameterConditionType("CONDITION_TYPE");
        edgeboxRule10.setConditionType("CEP");
        EdgeboxRuleService.getInstance().insert(edgeboxRule10);

        // Integration SQLServer
        EdgeboxRule edgeboxRule13 = new EdgeboxRule();
        edgeboxRule13.setName("ExternalDataBaseSubscriber");
        edgeboxRule13.setDescription(
                "When a thing is moved to another area, this rule sends the value of the UDF to the external Data " +
                        "Base" +
                        " in SQL Server.");
        edgeboxRule13.setInput("ThingMessage");
        edgeboxRule13.setRule(
                "select * from messageEventType where udf('thingTypeCode') = 'Document' and (  udf('vizixFlag') = " +
                        "\"Vizix\" )");
        edgeboxRule13.setOutput("ExternalDataBaseSubscriber");
        StringBuffer sb = new StringBuffer("");
        sb.append("{\"connectionCode\":\"MSSQLServer\",");
        sb.append("\"storeProcedure\":\"VizixDocument\",");
        sb.append("\"input\":[\"documentId\",\"documentName\",\"documentType\",\"documentStatus\",\"category1\"," +
                "\"category2\",\"boxId\",\"imagePath\",\"shelfId\",\"applySecurity\",\"cabinetId\",\"vizixFlag\"],");
        sb.append("\"inputTypeData\":[\"Integer\",\"String\",\"String\",\"String\",\"String\",\"String\"," +
                "\"Integer\"," +
                "\"String\",\"Integer\",\"Integer\",\"Integer\",\"String\"]}");
        edgeboxRule13.setOutputConfig(sb.toString());
        edgeboxRule13.setActive(false);
        edgeboxRule13.setRunOnReorder(false);
        edgeboxRule13.setEdgebox(edgeboxMCB);
        edgeboxRule13.setCronSchedule("");
        edgeboxRule13.setGroup(facilityGroup);
        edgeboxRule13.setParameterConditionType("CONDITION_TYPE");
        edgeboxRule13.setConditionType("CEP");
        EdgeboxRuleService.getInstance().insert(edgeboxRule13);


        EdgeboxRule edgeboxRule15 = new EdgeboxRule();
        edgeboxRule15.setName("TCP Flow (Sync)");
        edgeboxRule15.setActive(Boolean.FALSE);
        edgeboxRule15.setRunOnReorder(Boolean.FALSE);
        edgeboxRule15.setConditionType("CEP");
        edgeboxRule15.setCronSchedule("");
        edgeboxRule15.setDescription("Send payload to a TCP server");
        edgeboxRule15.setInput("ThingMessage");
        edgeboxRule15.setName("TCP Flow (Sync)");
        edgeboxRule15.setOutput("TCPAction");
        edgeboxRule15.setOutputConfig("{\"host\":\"localhost\",\"port\":23,\"payLoad\":\"Test\",\"typeMessage\":\"plainText\",\"encoding\":\"text\"}");
        edgeboxRule15.setParameterConditionType("CONDITION_TYPE");
        edgeboxRule15.setRule("select * from messageEventType where udf('thingTypeCode') = 'default_rfid_thingtype' and ( udf('zone') != \"Stockroom\" )");
        edgeboxRule15.setEdgebox(edgeboxMCB);
        edgeboxRule15.setGroup(facilityGroup);
        EdgeboxRuleService.getInstance().insert(edgeboxRule15);

        PopDBIOTUtils.popZoneType(facilityGroup, "Default Zone Type 1", "DefaultZoneType1", null);
        PopDBIOTUtils.popZoneType(facilityGroup, "Default Zone Type 2", "DefaultZoneType2", null);

        EdgeboxService.getInstance().insertEdgebox(facilityGroup, "ALE Edge Bridge", "ALEB", "edge", getAleBridgeConfiguration("default_rfid_thingtype", "MQTTSM"),9090L);
        EdgeboxService.getInstance().insertEdgebox(facilityGroup, "ALE Edge Bridge #2", "ALEB2", "edge", getAleBridgeConfiguration("default_rfid_thingtype", "MQTTSM"),0L);
        EdgeboxService.getInstance().insertEdgebox(facilityGroup, "GPS Edge Bridge", "GPS", "GPS", getGpsBridgeConfiguration("MQTTSM"),0L);

//        EdgeboxService.getInstance().insertEdgebox(
//                starFlexGroup,
//                Constants.TT_STARflex_NAME+" Bridge",
//                Constants.STARFLEX_MAIN_BRIDGE_CODE,
//                Constants.EDGEBOX_STARFLEX_TYPE,
//                PopDBRequiredIOT.getStarFlexBridgeConfiguration(
//                        Constants.TT_STARflex_CONFIG_CODE,
//                        Constants.TT_STARflex_STATUS_CODE,
//                        Constants.TT_STARflex_CODE, "MONGO", "MQTT"),
//                9092L);

//        EdgeboxService.getInstance().insertEdgebox(facilityGroup, "StarFlex Core Bridge", "STAR_MCB_TEMP", "core", mcbConfig, 0L);
//        EdgeboxService.getInstance().insertEdgebox(facilityGroup, "StarFlex ALE Edge Bridge", "STAR_ALEB_TEMP", "edge", getAleBridgeConfiguration("flextag_code"), 9093L);

        for (Resource resource : resources) {
            RoleResourceService.getInstance().insert(tenantAdminRole, resource, resource.getAcceptedAttributes());
            ResourceService.getInstance().update(resource);

        }
        List<Resource> resources1 = ResourceService.list();
        for (Resource resource : resources1) {
            if(resource.getLabel().toString().equals("Default RFID Thing 1") ||
                    (resource.getParent()!=null &&
                            resource.getParent().getName().equals("Thing Types"))) {
                RoleResourceService.getInstance().insert(tenantAdminRole, resource, resource.getAcceptedAttributes());
            }
        }


    }

    public void populateConnection() {
        // SQLServer connection example
        Group rootGroup = GroupService.getInstance().getRootGroup();
        Connection connection = new Connection();
        connection.setName("MSSQLServer");
        connection.setCode("MSSQLServer");
        connection.setGroup(rootGroup);
        connection.setConnectionType(ConnectionTypeService.getInstance()
                .getConnectionTypeDAO()
                .selectBy(QConnectionType.connectionType.code.eq(
                        "DBConnection")));
        JSONObject jsonProperties = new JSONObject();
        Map<String, Object> mapProperties = new LinkedHashMap<String, Object>();
        mapProperties.put("driver", "com.microsoft.sqlserver.jdbc.SQLServerDriver");
        mapProperties.put("password", "YWJjMTIz");
        mapProperties.put("schema", "DWMS");
        mapProperties.put("url", "jdbc:sqlserver://localhost;DatabaseName=DWMS");
        mapProperties.put("user", "sa");
        jsonProperties.putAll(mapProperties);
        connection.setProperties(jsonProperties.toJSONString());
        ConnectionService.getInstance().insert(connection);

        // MySQL connection example
        connection = new Connection();
        connection.setName("MySQLServer");
        connection.setCode("MySQLServer");
        connection.setGroup(rootGroup);
        connection.setConnectionType(ConnectionTypeService.getInstance().getConnectionTypeDAO()
                .selectBy(QConnectionType.connectionType.code.eq("DBConnection")));
        jsonProperties = new JSONObject();
        mapProperties = new LinkedHashMap<String, Object>();
        mapProperties.put("driver", "com.mysql.jdbc.Driver");
        mapProperties.put("password", "Y29udHJvbDEyMyE=");
        mapProperties.put("schema", "ct-app-center");
        mapProperties.put("url", "jdbc:mysql://localhost:3306/ct-app-center");
        mapProperties.put("user", "root");
        jsonProperties.putAll(mapProperties);
        connection.setProperties(jsonProperties.toJSONString());
        ConnectionService.getInstance().insert(connection);


        // MySQL connection (for the RIOT_MAIN schema)
        connection = new Connection();
        connection.setName("SQL");
        connection.setCode("SQL");
        connection.setGroup(rootGroup);
        connection.setConnectionType(ConnectionTypeService.getInstance().getConnectionTypeDAO()
                .selectBy(QConnectionType.connectionType.code.eq("SQL")));
        jsonProperties = new JSONObject();
        mapProperties = new LinkedHashMap<String, Object>();
        mapProperties.put("driver", "com.mysql.jdbc.Driver");
        mapProperties.put("dialect", "org.hibernate.dialect.MySQLDialect");
        mapProperties.put("username", "root");
        mapProperties.put("password", "Y29udHJvbDEyMyE=");
        mapProperties.put("url", "jdbc:mysql://localhost:3306/riot_main");
        mapProperties.put("hazelcastNativeClientAddress", "localhost");
        jsonProperties.putAll(mapProperties);
        connection.setProperties(jsonProperties.toJSONString());
        ConnectionService.getInstance().insert(connection);

        // VIZIX REST
        connection = new Connection();
        connection.setName("Services");
        connection.setCode("SERVICES");
        connection.setGroup(rootGroup);
        connection.setConnectionType(
                ConnectionTypeService.getInstance().getConnectionTypeDAO().selectBy(QConnectionType.connectionType.code.eq("REST")));
        jsonProperties = new JSONObject();
        mapProperties = new LinkedHashMap<String, Object>();
        mapProperties.put("host", "localhost");
        mapProperties.put("port", 8080);
        mapProperties.put("contextpath", "/riot-core-services");
        mapProperties.put("apikey", "7B4BCCDC");
        mapProperties.put("secure", false);
        jsonProperties.putAll(mapProperties);
        connection.setProperties(jsonProperties.toJSONString());
        ConnectionService.getInstance().insert(connection);

        // MQTT connection example
        connection = new Connection();
        connection.setName("Mqtt");
        connection.setCode("MQTT");
        connection.setGroup(rootGroup);
        connection.setConnectionType(
                ConnectionTypeService.getInstance().getConnectionTypeDAO().selectBy(QConnectionType.connectionType.code.eq("MQTT")));
        jsonProperties = new JSONObject();
        mapProperties = new LinkedHashMap<String, Object>();
        mapProperties.put("host", "localhost");
        mapProperties.put("port", 1883);
        mapProperties.put("qos", 2);
        mapProperties.put("secure", false);
        jsonProperties.putAll(mapProperties);
        connection.setProperties(jsonProperties.toJSONString());
        ConnectionService.getInstance().insert(connection);


        // MONGO
        connection = new Connection();
        connection.setName("MongoDB");
        connection.setCode("MONGO");
        connection.setGroup(rootGroup);
        connection.setConnectionType(
                ConnectionTypeService.getInstance().getConnectionTypeDAO().selectBy(QConnectionType.connectionType.code.eq("MONGO")));
        jsonProperties = new JSONObject();
        mapProperties = new LinkedHashMap<String, Object>();

        mapProperties.put("mongoPrimary", "127.0.0.1:27017");
        mapProperties.put("mongoSecondary", "");
        mapProperties.put("mongoReplicaSet", "");
        mapProperties.put("mongoSSL", false);
        mapProperties.put("username", "admin");
        mapProperties.put("password", "Y29udHJvbDEyMyE=");
        mapProperties.put("mongoAuthDB", "admin");
        mapProperties.put("mongoDB", "riot_main");
        mapProperties.put("mongoSharding", false);
        mapProperties.put("mongoConnectTimeout", 0);
        mapProperties.put("mongoMaxPoolSize", 0);

        jsonProperties.putAll(mapProperties);
        connection.setProperties(jsonProperties.toJSONString());
        ConnectionService.getInstance().insert(connection);

        // FTP connection example
        connection = new Connection();
        connection.setName("Ftp");
        connection.setCode("FTP");
        connection.setGroup(rootGroup);
        connection.setConnectionType(
                ConnectionTypeService.getInstance().getConnectionTypeDAO().selectBy(QConnectionType.connectionType.code.eq("FTP")));
        jsonProperties = new JSONObject();
        mapProperties = new LinkedHashMap<String, Object>();
        mapProperties.put("username", "ftpUser");
        mapProperties.put("password", "MTIzNA==");
        mapProperties.put("host", "localhost");
        mapProperties.put("port", 21);
        mapProperties.put("secure", false);
        jsonProperties.putAll(mapProperties);
        connection.setProperties(jsonProperties.toJSONString());
        ConnectionService.getInstance().insert(connection);
    }

    public static String getRestEndpointSubscriberConfig() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"method\" : \"POST\",");
        sb.append("\"protocol\" : \"http\",");
        sb.append("\"host\" : \"localhost\",");
        sb.append("\"port\" : 8080,");
        sb.append("\"host\" : \"localhost\",");
        sb.append("\"path\" : \"/riot-core-services/api/thingBridge/test/testRestEndpointSubscriber\",");
        sb.append("\"headers\" : { \"Api_key\" : \"root\"},");
        sb.append("\"basicAuth\" : { \"username\" : \"myname\", \"password\" : \"mypasss\" },");
        sb.append("\"body\" : \"zone=$zone\"");

        //sb.append( "\"mqtt\" : { \"host\" : \"localhost\", \"port\" : 1883 }," );
        //sb.append( "\"timeDistanceFilter\" : { \"active\" : 1, \"time\" : 0.0, \"distance\" : 10.0 }," );
        //sb.append( "\"timeZoneFilter\" : { \"active\" : 0, \"time\" : 10.0 }," );
        //sb.append( "\"zoneChangeFilter\" : { \"active\" : 0}," );
        //sb.append( "\"esperRuleMessages\" : [ \"default_rfid_thingtype\" ]" );
        sb.append("}");
        return sb.toString();
    }

    private String getSuperSubscriberConfig() {
        InputStream is = this.getClass()
                .getClassLoader()
                .getResourceAsStream("superSubscriberConfig.json");
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        String line = null;
        StringBuilder stringBuilder = new StringBuilder();
        String ls = System.getProperty("line.separator");

        try {
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append(ls);
            }
        } catch (IOException e) {
            logger.warn("exception: ", e);
        }

        return stringBuilder.toString();
    }

    /**
     * Populate Parameters table
     */
    public static void populateParameters() {
        Parameters parameters = new Parameters(Constants.CONDITION_TYPE, "ALWAYS_TRUE", "@SYSTEM_PARAMETERS_CONDITION_TYPE_ALWAYS_TRUE", null);
        ParametersService.getInstance().insert(parameters);
        parameters = new Parameters(Constants.CONDITION_TYPE, "CEP", "@SYSTEM_PARAMETERS_CONDITION_TYPE_CEP", null);

        ParametersService.getInstance().insert(parameters);
        parameters = new Parameters(Constants.BRIDGE_TYPE, "edge", "@SYSTEM_PARAMETERS_BRIDGE_TYPE_EDGE",
                "{\"thingTypeCode\":\"\",\"logRawMessages\":0,\"numberOfThreads\":10," +
                        "\"socketTimeout\":60000,\"dynamicTimeoutRate\":0,\"send500ErrorOnTimeout\":0,\"mqtt\":{\"connectionCode\":\"\"}," +
                        "\"zoneDwellFilter\":{\"active\":0,\"unlockDistance\":25,\"inZoneDistance\":10,\"zoneDwellTime\":300," +
                        "\"lastDetectTimeActive\":1,\"lastDetectTimeWindow\":0}," +
                        "\"streaming\":{\"active\":false,\"bufferSize\":10}}");
        ParametersService.getInstance().insert(parameters);
        parameters = new Parameters(Constants.BRIDGE_TYPE, "StarFLEX", "@SYSTEM_PARAMETERS_BRIDGE_TYPE_STARFLEX",
                "{\"thingTypeCode\":\"\",\"thingTypeCodeDevice\":\"STR_400\",\"messageMode\":\"FlexTag\",\"mqtt\":{\"connectionCode\":\"\"}," +
                        "\"zoneDwellFilter\":{\"active\":1,\"unlockDistance\":0,\"inZoneDistance\":0,\"zoneDwellTime\":1,\"lastDetectTimeActive\":1," +
                        "\"lastDetectTimeWindow\":30},\"rateFilter\":{\"active\":1,\"timeLimit\":5},\"mongo\":{\"connectionCode\":\"\"}}");
        ParametersService.getInstance().insert(parameters);
        parameters = new Parameters(Constants.BRIDGE_TYPE, "FTP", "@SYSTEM_PARAMETERS_BRIDGE_TYPE_FTP",
                "{\"thingTypeCode\":\"\",\"logRawMessages\":0,\"ftp\":{\"connectionCode\":\"\"}," +
                        "\"path\":\"/StoreReferenceData\",\"pattern\":\"*.COLOUR\",\"patternCaseSensitive\":false," +
                        "\"schedule\":\"0 0/10 * 1/1 * ? *\",\"configParser\":{\"parserType\":\"fixedlength\"," +
                        "\"separator\":null,\"fieldLengths\":\"3,16,1\",\"ignoreFooter\":true,\"ignoreHeader\":false," +
                        "\"fieldNames\":[\"Code\",\"Description\",\"Action\"],\"columnNumberAsSerial\":0},\"processPolicy\":\"Move\"," +
                        "\"localBackupFolder\":\"/tmp\",\"ftpDestinationFolder\":\"processed/colour\",\"mqtt\":{\"connectionCode\":\"\"}}");
        ParametersService.getInstance().insert(parameters);
        parameters = new Parameters(Constants.BRIDGE_TYPE, "GPS", "@SYSTEM_PARAMETERS_BRIDGE_TYPE_GPS",
                "{\"thingTypeCode\":\"\",\"logRawMessages\":0,\"mqtt\":{\"connectionCode\":\"\"}," +
                        "\"geoforce\":{\"host\":\"app.geoforce.com\"," +
                        "\"path\":\"/feeds/asset_inventory.xml\",\"port\":443,\"user\":\"datafeed@mojix.com\"," +
                        "\"password\":\"AHmgooCk8l0jo95f7YSo\",\"period\":60}}");
        ParametersService.getInstance().insert(parameters);
        parameters = new Parameters(Constants.BRIDGE_TYPE, "OPEN_RTLS", "@SYSTEM_PARAMETERS_BRIDGE_TYPE_OPEN_RTLS", null);
        ParametersService.getInstance().insert(parameters);
        parameters = new Parameters(Constants.BRIDGE_TYPE, "core", "@SYSTEM_PARAMETERS_BRIDGE_TYPE_CORE",
                "{\"threadDispatchMode\":1,\"numberOfThreads\":32,\"mqtt\":{\"connectionCode\":\"\",\"topics\":[\"/v1/data/ALEB/#\"," +
                        "\"/v1/data/APP2/#\",\"/v1/data/STAR/#\",\"/v1/data/STAR1/#\"]},\"mongo\":{\"connectionCode\":\"\"}," +
                        "\"sequenceNumberLogging\":{\"active\":0,\"TTL\":86400,\"GC_GRACE_SECONDS\":0},\"sourceRule\":{\"active\":0}," +
                        "\"CEPLogging\":{\"active\":0},\"pointInZoneRule\":{\"active\":1},\"doorEventRule\":{\"active\":1}," +
                        "\"shiftZoneRule\":{\"active\":0,\"shiftProperty\":\"shift\",\"zoneViolationStatusProperty\":\"zoneViolationStatus\"," +
                        "\"zoneViolationFlagProperty\":\"zoneViolationFlag\"},\"checkMultilevelReferences\":{\"active\":0}}");
        ParametersService.getInstance().insert(parameters);
    }

    public static String getAleBridgeConfiguration(String thingTypeCode, String brokerCon) {
        JSONObject configuration = new JSONObject();
        configuration.put("thingTypeCode", thingTypeCode);
        configuration.put("logRawMessages", 0);
        configuration.put("numberOfThreads", 10);

        configuration.put("socketTimeout", 60000);
        configuration.put("dynamicTimeoutRate", 0);
        configuration.put("send500ErrorOnTimeout", 0);

        //MQTT
        JSONObject mqtt = new JSONObject();
        mqtt.put("connectionCode", brokerCon);
        mqtt.put("active", true);
        configuration.put("mqtt", mqtt);

        //ZoneDwellFilter
        JSONObject zoneDwellFilter = new JSONObject();
        zoneDwellFilter.put("active", 0);
        zoneDwellFilter.put("unlockDistance", 25.0);
        zoneDwellFilter.put("inZoneDistance", 10.0);
        zoneDwellFilter.put("zoneDwellTime", 300.0);
        zoneDwellFilter.put("lastDetectTimeActive", 1);
        zoneDwellFilter.put("lastDetectTimeWindow", 0);

        configuration.put("zoneDwellFilter", zoneDwellFilter);

        //RIOT-12855: Adding evaluateStats boolean parameter
        configuration.put("evaluateStats", true);

        //VIZIX-4818: Adding bridgePort Number parameter
        configuration.put("bridgePort", 9090);

        //VIZIX-1852: Add "streaming" mode to ALEBridges configuration
        JSONObject streaming = new JSONObject();
        streaming.put("active", Boolean.FALSE);
        streaming.put("bufferSize", 10);

        configuration.put("streaming", streaming);

        return configuration.toJSONString();
    }

    public static String getGpsBridgeConfiguration(String brokerCon) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"thingTypeCode\" : \"default_gps_thingtype\",");
        sb.append("\"logRawMessages\" : 0,");
        sb.append("\"mqtt\" : {\"connectionCode\" : \"" + brokerCon + "\"},");
        sb.append(
                "\"geoforce\" : { \"host\" : \"app.geoforce.com\", \"path\" : \"/feeds/asset_inventory.xml\", \"port\"" +
                        " : 443, \"user\" : \"datafeed@mojix.com\", \"password\" : \"AHmgooCk8l0jo95f7YSo\", \"period\" : " +
                        "60}");
        sb.append("}");
        return sb.toString();
    }

    public static String getStarFlexBridgeConfiguration(String thingTypeCode,
                                                        String thingTypeCodeDevice,
                                                        String messageMode,
                                                        Boolean rateFilter) {
        ObjectMapper mapper = new ObjectMapper();

        ObjectNode configuration = mapper.createObjectNode();
        configuration.put("thingTypeCode", thingTypeCode);
        configuration.put("thingTypeCodeDevice", thingTypeCodeDevice);
        configuration.put("messageMode", messageMode);
        //MQTT
        ObjectNode mqtt = mapper.createObjectNode();
        mqtt.put("connectionCode", "MQTT");
        configuration.set("mqtt", mqtt);

        //MONGO
        ObjectNode mongo = mapper.createObjectNode();
        mongo.put("connectionCode", "MONGO ");
        configuration.set("mongo", mongo);
        //RATE_FILTER
        if (rateFilter) {
            ObjectNode rateFilterO = mapper.createObjectNode();
            rateFilterO.put("active", 1);
            rateFilterO.put("timeLimit", 5);
            configuration.set("rateFilter", rateFilterO);
        }

        //ZONE_DWELL_FILTER
        ObjectNode zoneDwellFilter = mapper.createObjectNode();
        zoneDwellFilter.put("lastDetectTimeActive", 1);
        zoneDwellFilter.put("active", 1);
        zoneDwellFilter.put("inZoneDistance", 0);
        zoneDwellFilter.put("lastDetectTimeWindow", 30);
        zoneDwellFilter.put("unlockDistance", 0);
        zoneDwellFilter.put("zoneDwellTime", 1);
        configuration.set("zoneDwellFilter", zoneDwellFilter);

        return configuration.toString();

    }

    public static String getStarFlexTagBridgeConfiguration(String thingTypeCode,
                                                           String thingTypeCodeDevice,
                                                           String messageMode,
                                                           Boolean rateFilter) {
        ObjectMapper mapper = new ObjectMapper();

        ObjectNode configuration = mapper.createObjectNode();
        configuration.put("thingTypeCode", thingTypeCode);
        configuration.put("thingTypeCodeDevice", thingTypeCodeDevice);
        configuration.put("messageMode", messageMode);

        //MQTT
        ObjectNode mqtt = mapper.createObjectNode();
        mqtt.put("connectionCode", "MQTT");
        configuration.set("mqtt", mqtt);

        //MONGO
        ObjectNode mongo = mapper.createObjectNode();
        mongo.put("connectionCode", "MONGO");
        configuration.set("mongo", mongo);

        //RATE_FILTER
        if (rateFilter) {
            ObjectNode rateFilterO = mapper.createObjectNode();
            rateFilterO.put("active", 1);
            rateFilterO.put("timeLimit", 5);
            configuration.set("rateFilter", rateFilterO);
        }

        //ZONE_DWELL_FILTER
        ObjectNode zoneDwellFilter = mapper.createObjectNode();
        zoneDwellFilter.put("inZoneDistance", 0);
        zoneDwellFilter.put("lastDetectTimeActive", 1);
        zoneDwellFilter.put("lastDetectTimeWindow", 30);
        zoneDwellFilter.put("unlockDistance", 0);
        zoneDwellFilter.put("zoneDwellTime", 1);
        zoneDwellFilter.put("active", 1);
        configuration.set("zoneDwellFilter", zoneDwellFilter);

        return configuration.toString();
    }


    public static String getLightBuzzerRuleActionConfig() {
        return "{\"ip\":\"10.100.1.61\",\"port\":23,\"username\":\"alien\",\"password\":\"password\"," +
                "\"times\":{\"lightOn\":5000,\"buzzerOn\":4000,\"buzzerOff\":3000,\"numberOfRetries\":5,\"retryTime\":5000,\"delay\":2000}," +
                "\"lightPinMap\":{\"light1\":0,\"light2\":2,\"light3\":3,\"light4\":4}," +
                "\"buzzerPinMap\":{\"buzzer1\":1},\"zoneLightBuzzerMap\":{\"Stockroom\":[\"light1\"," +
                "\"buzzer1\"],\"Salesfloor\":[\"light2\",\"buzzer1\"],\"PoS\":[\"light3\",\"buzzer1\"]," +
                "\"Entrance\":[\"light4\",\"buzzer1\"]}}";

    }

    public static String getMFRLightBuzzerRuleActionConfig() {
        return "{\"ip\":\"10.100.1.124\",\"port\":65200," +
                "\"times\":{\"lightOn\":5000,\"buzzerOn\":3000,\"buzzerOff\":2000,\"delayBeforeTrigger\":0," +
                "\"timeBuzzer\":3000,\"maxTimeBuzzer\":5000}," +
                "\"lightPinMap\":{\"light1\":0,\"light2\":2,\"light3\":3,\"light4\":4}," +
                "\"buzzerPinMap\":{\"buzzer1\":1},\"zoneLightBuzzerMap\":{\"Stockroom\":[\"light1\"," +
                "\"buzzer1\"],\"Salesfloor\":[\"light2\",\"buzzer1\"],\"PoS\":[\"light3\",\"buzzer1\"]," +
                "\"Entrance\":[\"light4\",\"buzzer1\"]}}";

    }

    public static String getMFRTurnOffLightBuzzerRuleActionConfig() {
        return "{\"ip\":\"10.100.1.124\",\"port\":65200," +
                "\"times\":{\"lightOn\":5000,\"buzzerOn\":3000,\"buzzerOff\":2000,\"delayBeforeTrigger\":0," +
                "\"timeBuzzer\":3000,\"maxTimeBuzzer\":5000}," +
                "\"lightPinMap\":{\"light1\":0,\"light2\":2,\"light3\":3,\"light4\":4}," +
                "\"buzzerPinMap\":{\"buzzer1\":1}," +
                "\"zoneLightBuzzerMap\":{\"Stockroom\":[\"light1\",\"buzzer1\"],\"Salesfloor\":[\"light2\"," +
                "\"buzzer1\"],\"PoS\":[\"light3\",\"buzzer1\"],\"Entrance\":[\"light4\",\"buzzer1\"]}," +
                "\"counterUDFs\":{\"zoneUDF\":\"zone\",\"lastZoneIdUDF\":\"lastZoneId\"," +
                "\"zoneAlertFlagUDF\":\"zoneAlertFlag\"}}";

    }

    public static String getExitGateRuleDefaultActionConfig() {
        return "{}";
    }
}
