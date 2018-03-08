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

public class PopDBMojixRetail {
    private static final Logger logger = Logger.getLogger(PopDBMojixRetail.class);

    public static void main(String args[]) throws Exception {
        PopDBRequired.initJDBCDrivers();
        System.getProperties().put("hibernate.hbm2ddl.auto", "update");
        System.getProperties().put("hibernate.cache.use_second_level_cache", "false");
        System.getProperties().put("hibernate.cache.use_query_cache", "false");
        Configuration.init(null);
        //CassandraUtils.init(Configuration.getProperty("cassandra.host"), Configuration.getProperty("cassandra
        // .keyspace"));


        PopDBMojixRetail popdb = new PopDBMojixRetail();
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

        PopulateDBRiotMaker prm = new PopulateDBRiotMaker();
        prm.demo();


        PopDBML pml = new PopDBML();
        pml.run();
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
        Connection mqttSM = ConnectionService.getInstance().getByCode("MQTTSM");
        Connection mongoSM = ConnectionService.getInstance().getByCode("MONGOSM");
        Connection ftpSM = ConnectionService.getInstance().getByCode("FTPSM");
        mqttSM.setGroup(santaMonica);
        mongoSM.setGroup(santaMonica);
        ftpSM.setGroup(santaMonica);

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


        createReportDefinitionData2(santaMonica, storeGroupType, rootUser);

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

        Connection mqttSF = ConnectionService.getInstance().getByCode("MQTTSF");
        Connection mongoSF = ConnectionService.getInstance().getByCode("MONGOSF");
        mqttSF.setGroup(starFlexGroup);
        mongoSF.setGroup(starFlexGroup);

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
        createReportDefinitionData(santaMonica, departamentGroupType);
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


    private ReportFilter createReportFilter(String label,
                                            String propertyName,
                                            String propertyOrder,
                                            String operatorFilter,
                                            String value,
                                            Boolean isEditable,
                                            Long ttId,
                                            ReportDefinition reportDefinition,
                                            Integer fieldType) {
        ReportFilter reportFilter = new ReportFilter();
        reportFilter.setLabel(label);
        reportFilter.setPropertyName(propertyName);
        reportFilter.setDisplayOrder(Float.parseFloat(propertyOrder));
        reportFilter.setOperator(operatorFilter);
        reportFilter.setValue(value);
        reportFilter.setEditable(isEditable);
        reportFilter.setFieldType(fieldType);
        ThingType thingType = ThingTypeService.getInstance().get(ttId);
        if (thingType != null){
            System.out.println("ThingTypeService******>> " + thingType.getId());
            Long thingTypeFieldId = ThingTypeFieldService.getInstance().get(ttId).getId();
            if (thingTypeFieldId!= null){
                System.out.println("ThingTypeFieldService******>> " + thingTypeFieldId);
                reportFilter.setThingTypeField(ThingTypeFieldService.getInstance().getThingTypeFieldByName(propertyName).get
                        (0));
            }
        }
        reportFilter.setThingType(thingType);
        reportFilter.setReportDefinition(reportDefinition);
        System.out.println("reportDefinition******>> " + reportDefinition.getId());
        return reportFilter;
    }

    private static ReportFilter createReportFilter(String label, String propertyName, String propertyOrder, String
            operatorFilter, String value,
                                                   Boolean isEditable, ReportDefinition reportDefinition, Integer fieldType) {
        ReportFilter reportFilter = new ReportFilter();
        reportFilter.setLabel(label);
        reportFilter.setPropertyName(propertyName);
        reportFilter.setDisplayOrder(Float.parseFloat(propertyOrder));
        reportFilter.setOperator(operatorFilter);
        reportFilter.setValue(value);
        reportFilter.setEditable(isEditable);
        reportFilter.setFieldType(fieldType);
        reportFilter.setReportDefinition(reportDefinition);
        return reportFilter;
    }

    private static ReportProperty createReportProperty(String label, String propertyName, String propertyOrder, Long
            propertyTypeId, Boolean showPopUp,
                                                       ReportDefinition reportDefinition) {
        ReportProperty reportProperty = new ReportProperty();
        reportProperty.setLabel(label);
        reportProperty.setPropertyName(propertyName);
        reportProperty.setDisplayOrder(Float.parseFloat(propertyOrder));
        reportProperty.setThingType(ThingTypeService.getInstance().get(propertyTypeId));
        List<ThingTypeField> lstThingTypeField = ThingTypeFieldService.getInstance().getThingTypeFieldByName
                (propertyName);
        if (lstThingTypeField != null && lstThingTypeField.size() > 0) {
            reportProperty.setThingTypeField(lstThingTypeField.get(0));
        }

        reportProperty.setShowHover(showPopUp);
        reportProperty.setReportDefinition(reportDefinition);
        return reportProperty;
    }

    private ReportRule createReportRule(String propertyName, String operator, String value, String color, String
            style, Long TID,
                                        ReportDefinition reportDefinition, Float displayOrder) {
        ReportRule reportRule = new ReportRule();
        reportRule.setDisplayOrder(displayOrder);
        reportRule.setPropertyName(propertyName);
        reportRule.setOperator(operator);
        reportRule.setValue(value);
        reportRule.setColor(color);
        reportRule.setStyle(style);
        reportRule.setReportDefinition(reportDefinition);
        reportRule.setThingType(ThingTypeService.getInstance().get(TID));
        List<ThingTypeField> lstThingTypeField = ThingTypeFieldService.getInstance().getThingTypeFieldByName
                (propertyName);
        if (lstThingTypeField != null && lstThingTypeField.size() > 0) {
            reportRule.setThingTypeField(lstThingTypeField.get(0));
        }

        return reportRule;
    }

    private void createReportDefinitionData(Group group, GroupType gt) {
        User rootUser = UserService.getInstance().get(1L);
        // Reporte3

        ReportDefinition reportDefinition5 = new ReportDefinition();
        reportDefinition5.setName("Clothing by Brand");
        reportDefinition5.setCreatedByUser(rootUser);
        reportDefinition5.setGroup(group);
        reportDefinition5.setReportType("table");
        reportDefinition5.setDefaultTypeIcon("pin");
        reportDefinition5.setPinLabels(true);
        reportDefinition5.setZoneLabels(true);
        reportDefinition5.setTrails(false);
        reportDefinition5.setClustering(true);
        reportDefinition5.setPlayback(true);
        reportDefinition5.setNupYup(true);
        reportDefinition5.setDefaultList(false);
        reportDefinition5.setDefaultColorIcon("4DD000");
        reportDefinition5.setRunOnLoad(true);
        reportDefinition5.setIsMobile(Boolean.FALSE);
        reportDefinition5.setIsMobileDataEntry(Boolean.FALSE);
        reportDefinition5 = ReportDefinitionService.getInstance().insert(reportDefinition5);

        String[] labels5 = {"Brand", "Category", "RFID Tag #", "Department", "Last Detect Time", "Logical Reader",
                "Color", "Size", "Price",
                "Name", "Type"};
        String[] propertyNames5 = {"brand", "category", "serial", "group.name", "lastDetectTime", "logicalReader",
                "color", "size",
                "price", "name", "thingType.name"};
        String[] propertyOrders5 = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"};
        Long[] propertyTypeIds5 = {0L, 0L, 1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L};
        Boolean[] propertyShowPopUp5 = {false, false, true, false, false, false, false, false, false, true, false};

        for (int it = 0; it < Array.getLength(labels5); it++) {
            ReportProperty reportProperty = createReportProperty(labels5[it], propertyNames5[it], propertyOrders5[it],
                    propertyTypeIds5[it], propertyShowPopUp5[it], reportDefinition5);
            ReportPropertyService.getInstance().insert(reportProperty);
        }

        String[] labelsFilter5 = {"Brand"};

        String[] propertyNamesFilter5 = {"brand"};
        String[] propertyOrdersFilter5 = {"2"};
        String[] operatorFilter5 = {"="};
        String[] value5 = {"Calvin Klein"};
        Boolean[] isEditable5 = {true};
        Long[] thingTypeIdReport5 = {1L};
        Integer[] fieldType5 = {null};
        for (int it = 0; it < Array.getLength(labelsFilter5); it++) {
            ReportFilter reportFilter = createReportFilter(labelsFilter5[it], propertyNamesFilter5[it],
                    propertyOrdersFilter5[it],
                    operatorFilter5[it], value5[it], isEditable5[it], thingTypeIdReport5[it], reportDefinition5,fieldType5[it]);
            System.out.println("Before Intert");
            ReportFilterService.getInstance().insert(reportFilter);
            System.out.println("After Intert");
        }

        // Reporte3

        ReportDefinition reportDefinition6 = new ReportDefinition();
        reportDefinition6.setName("Clothing by Brand");
        reportDefinition6.setGroup(group);
        reportDefinition6.setReportType("map");
        reportDefinition6.setDefaultTypeIcon("pin");
        reportDefinition6.setPinLabel("3");
        reportDefinition6.setLocalMapId(1L);
        reportDefinition6.setDefaultZoom(20L);
        reportDefinition6.setCenterLat("34.048139");
        reportDefinition6.setCenterLon("-118.443818");
        reportDefinition6.setPinLabels(false);
        reportDefinition6.setZoneLabels(false);
        reportDefinition6.setTrails(false);
        reportDefinition6.setClustering(false);
        reportDefinition6.setPlayback(true);
        reportDefinition6.setNupYup(true);
        reportDefinition6.setDefaultList(false);
        reportDefinition6.setDefaultTypeIcon("Pin");
        reportDefinition6.setDefaultColorIcon("4DD000");
        reportDefinition6.setCreatedByUser(rootUser);
        reportDefinition6.setRunOnLoad(true);
        reportDefinition6 = ReportDefinitionService.getInstance().insert(reportDefinition6);

        String[] labels6 = {"Brand", "Category", "RFID Tag #", "Department", "Last Detect Time", "Logical Reader",
                "Color", "Size", "Price",
                "Name", "Type"};
        String[] propertyNames6 = {"brand", "category", "serial", "group.name", "lastDetectTime", "logicalReader",
                "color", "size",
                "price", "name", "thingType.name"};
        String[] propertyOrders6 = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"};
        Long[] propertyTypeIds6 = {0L, 0L, 1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L};
        Boolean[] propertyShowPopUp6 = {false, false, true, false, false, false, false, false, false, true, false};
        for (int it = 0; it < Array.getLength(labels6); it++) {
            ReportProperty reportProperty = createReportProperty(labels6[it], propertyNames6[it], propertyOrders6[it],
                    propertyTypeIds6[it], propertyShowPopUp5[it], reportDefinition6);
            ReportPropertyService.getInstance().insert(reportProperty);
        }

        String[] labelsFilter6 = {"Brand"};

        String[] propertyNamesFilter6 = {"brand"};
        String[] propertyOrdersFilter6 = {"1"};
        String[] operatorFilter6 = {"="};
        String[] value6 = {"Calvin Klein"};
        Boolean[] isEditable6 = {true};
        Long[] thingTypeIdReport6 = {1L};
        Integer[] fieldType6 = {null};

        for (int it = 0; it < Array.getLength(labelsFilter6); it++) {
            ReportFilter reportFilter = createReportFilter(labelsFilter6[it], propertyNamesFilter6[it],
                    propertyOrdersFilter6[it],
                    operatorFilter6[it], value6[it], isEditable6[it], thingTypeIdReport6[it], reportDefinition6, fieldType6[it]);

            ReportFilterService.getInstance().insert(reportFilter);
        }

    }

    private void createReportDefinitionData2(Group group, GroupType gt, User createdByUser) {
        // Report 1
        ReportDefinition reportDefinition = new ReportDefinition();
        reportDefinition.setName("All RFID Tags");
        reportDefinition.setCreatedByUser(createdByUser);
        reportDefinition.setGroup(group);
        reportDefinition.setReportType("map");
        reportDefinition.setDefaultTypeIcon("pin");
        reportDefinition.setPinLabel("1");
        reportDefinition.setLocalMapId(1L);
        reportDefinition.setDefaultZoom(20L);
        reportDefinition.setCenterLat("34.048139");
        reportDefinition.setCenterLon("-118.443818");
        reportDefinition.setPinLabels(false);
        reportDefinition.setZoneLabels(false);
        reportDefinition.setTrails(false);
        reportDefinition.setClustering(false);
        reportDefinition.setPlayback(true);
        reportDefinition.setNupYup(true);
        reportDefinition.setDefaultTypeIcon("Pin");
        reportDefinition.setDefaultColorIcon("009F6B");
        reportDefinition.setRunOnLoad(true);
        reportDefinition.setDefaultList(false);
        reportDefinition.setIsMobile(Boolean.FALSE);
        reportDefinition.setIsMobileDataEntry(Boolean.FALSE);
        reportDefinition = ReportDefinitionService.getInstance().insert(reportDefinition);

        String[] labels = {"RFID Tag #", "Group", "Zone", "Zone Dwell Time (zone)", "Last Detect Time", "Name",
                "Location"};
        String[] propertyNames = {"serial", "group.name", "zone", "dwellTime( zone )", "lastDetectTime", "name",
                "location"};
        String[] propertyOrders = {"1", "2", "3", "4", "5", "6", "7"};
        Long[] propertyTypeIds = {1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L};
        Boolean[] propertyShowPopUp = {true, false, false, false, false, true, false};

        for (int it = 0; it < Array.getLength(labels); it++) {
            ReportProperty reportProperty = createReportProperty(labels[it], propertyNames[it], propertyOrders[it],
                    propertyTypeIds[it],
                    propertyShowPopUp[it], reportDefinition);
            ReportPropertyService.getInstance().insert(reportProperty);
        }

        String[] labelsFilter = {"Thing Type", "group"};
        String[] propertyNamesFilter = {"thingType.id", "group.id"};

        String[] propertyOrdersFilter = {"1", "2"};
        String[] operatorFilter = {"==", "="};

        String[] value = {"1", "2"};
        Boolean[] isEditable = {true, false};
        Integer[] fieldType = {null, null};

        for (int it = 0; it < Array.getLength(labelsFilter); it++) {
            ReportFilter reportFilter = createReportFilter(labelsFilter[it], propertyNamesFilter[it],
                    propertyOrdersFilter[it],
                    operatorFilter[it], value[it], isEditable[it], reportDefinition, fieldType[it]);
            ReportFilterService.getInstance().insert(reportFilter);
        }
        /*
         * for( int it = 0; it < Array.getLength( propertyNamesRules ); it++ ) {
		 * ReportRule reportRule = createReportRule( propertyNamesRules[it],
		 * operatorRules[it], valueRules[it], color[it], "",reportDefinition );
		 * 
		 * ReportRuleService.getInstance().insert(reportRule); }
		 */

        // Reporte 1
        ReportDefinition reportDefinition4 = new ReportDefinition();
        reportDefinition4.setName("All RFID Tags");
        reportDefinition4.setCreatedByUser(createdByUser);
        reportDefinition4.setGroup(group);
        reportDefinition4.setReportType("table");
        reportDefinition4.setDefaultTypeIcon("pin");
        reportDefinition4.setPinLabels(false);
        reportDefinition4.setZoneLabels(false);
        reportDefinition4.setTrails(false);
        reportDefinition4.setClustering(false);
        reportDefinition4.setPlayback(true);
        reportDefinition4.setNupYup(false);
        reportDefinition4.setDefaultList(false);
        reportDefinition4.setDefaultColorIcon("009F6B");
        reportDefinition4.setRunOnLoad(true);
        reportDefinition4 = ReportDefinitionService.getInstance().insert(reportDefinition4);

        String[] labels4 = {"RFID Tag #", "Group", "Zone", "Zone Dwell Time (zone)", "Last Detect Time", "Name",
                "Location"};
        String[] propertyNames4 = {"serial", "group.name", "zone", "dwellTime( zone )", "lastDetectTime", "name",
                "location"};
        String[] propertyOrders4 = {"1", "2", "3", "4", "5", "6", "7"};
        Long[] propertyTypeIds4 = {1L, 1L, 1L, 1L, 1L, 1L, 1L};
        Boolean[] propertyShowPopUp4 = {true, false, false, false, false, true, false};
        for (int it = 0; it < Array.getLength(labels4); it++) {
            ReportProperty reportProperty = createReportProperty(labels4[it], propertyNames4[it], propertyOrders4[it],
                    propertyTypeIds4[it], propertyShowPopUp4[it], reportDefinition4);
            ReportPropertyService.getInstance().insert(reportProperty);
        }

        String[] labelsFilter4 = {"Thing Type"};
        String[] propertyNamesFilter4 = {"thingType.id"};

        String[] propertyOrdersFilter4 = {"1"};
        String[] operatorFilter4 = {"=="};

        String[] value4 = {"1"};
        Boolean[] isEditable4 = {false};
        Integer[] fieldType4 = {null};
        for (int it = 0; it < Array.getLength(labelsFilter4); it++) {
            ReportFilter reportFilter = createReportFilter(labelsFilter4[it], propertyNamesFilter4[it],
                    propertyOrdersFilter4[it],
                    operatorFilter4[it], value4[it], isEditable4[it], reportDefinition4, fieldType4[it]);
            ReportFilterService.getInstance().insert(reportFilter);
        }

        ReportDefinition reportDefinition2 = new ReportDefinition();
        reportDefinition2.setName("Clothing by Zone");
        reportDefinition2.setCreatedByUser(createdByUser);
        reportDefinition2.setGroup(group);
        reportDefinition2.setReportType("map");
        reportDefinition2.setDefaultTypeIcon("pin");
        reportDefinition2.setDefaultZoom(20L);
        reportDefinition2.setLocalMapId(1L);
        reportDefinition2.setCenterLat("34.048139");
        reportDefinition2.setCenterLon("-118.443818");
        reportDefinition2.setPinLabels(false);
        reportDefinition2.setZoneLabels(false);
        reportDefinition2.setTrails(false);
        reportDefinition2.setClustering(false);
        reportDefinition2.setPlayback(true);
        reportDefinition2.setNupYup(true);
        reportDefinition2.setDefaultList(false);
        reportDefinition2.setDefaultTypeIcon("Pin");
        reportDefinition2.setDefaultColorIcon("009F6B");
        reportDefinition2.setRunOnLoad(true);
        reportDefinition2 = ReportDefinitionService.getInstance().insert(reportDefinition2);
        reportDefinition2.setPinLabel("3");
        String[] labels2 = {"Name", "Category", "RFID Tag", "Last Detect Time", "Brand", "Color", "Size", "Price"};
        String[] propertyNames2 = {"name", "category", "serial", "lastDetectTime", "brand", "color", "size", "price"};
        String[] propertyOrders2 = {"1", "2", "3", "4", "5", "6", "7", "8"};
        Long[] propertyTypeIds2 = {0L, 0L, 1L, 1L, 0L, 0L, 0L, 0L};
        Boolean[] propertyShowPopUp2 = {true, true, true, false, false, false, false, false};
        for (int it = 0; it < Array.getLength(labels2); it++) {
            ReportProperty reportProperty = createReportProperty(labels2[it], propertyNames2[it], propertyOrders2[it],
                    propertyTypeIds2[it], propertyShowPopUp2[it], reportDefinition2);
            ReportPropertyService.getInstance().insert(reportProperty);
        }

		/*String[] labelsFilter2 = { "Group", "Thing Type" };
        String[] propertyNamesFilter2 = { "group.id", "thingType.id" };
		String[] propertyOrdersFilter2 = { "1", "2" };
		String[] operatorFilter2 = { "=", "=" };
		String[] value2 = { "", "2" };
		Boolean[] isEditable2 = { true, true };

		for( int it = 0; it < Array.getLength( labelsFilter2 ); it++ )
		{
			ReportFilter reportFilter = createReportFilter( labelsFilter2[it], propertyNamesFilter2[it],
			propertyOrdersFilter2[it],
					operatorFilter2[it], value2[it], isEditable2[it], reportDefinition2 );
			ReportFilterService.getInstance().insert( reportFilter );
		}*/
        // Reporte2

        ReportDefinition reportDefinition3 = new ReportDefinition();
        reportDefinition3.setName("Clothing by Zone");
        reportDefinition3.setCreatedByUser(createdByUser);
        reportDefinition3.setGroup(group);
        reportDefinition3.setReportType("table");
        reportDefinition3.setDefaultTypeIcon("pin");

        reportDefinition3.setPinLabels(false);
        reportDefinition3.setZoneLabels(false);
        reportDefinition3.setTrails(false);
        reportDefinition3.setClustering(false);
        reportDefinition3.setPlayback(true);
        reportDefinition3.setNupYup(false);
        reportDefinition3.setDefaultList(false);
        reportDefinition3.setDefaultColorIcon("009F6B");
        reportDefinition3.setRunOnLoad(true);
        reportDefinition3 = ReportDefinitionService.getInstance().insert(reportDefinition3);
        reportDefinition3.setDefaultColorIcon("009F6B");
        String[] labels3 = {"Name", "Category", "RFID Tag", "Last Detect Time", "Brand", "Color", "Size", "Price"};
        String[] propertyNames3 = {"name", "category", "serial", "lastDetectTime", "brand", "color", "size", "price"};
        String[] propertyOrders3 = {"1", "2", "3", "4", "5", "6", "7", "8", "9"};
        Long[] propertyTypeIds3 = {3L, 1L, 3L, 3L, 1L, 1L, 1L, 1L, 3L};
        Boolean[] propertyShowPopUp3 = {true, true, true, false, false, false, false, false, false};
        for (int it = 0; it < Array.getLength(labels3); it++) {
            ReportProperty reportProperty = createReportProperty(labels3[it], propertyNames3[it], propertyOrders3[it],
                    propertyTypeIds3[it], propertyShowPopUp3[it], reportDefinition3);
            ReportPropertyService.getInstance().insert(reportProperty);
        }

	/*	String[] labelsFilter3 = { "Group", "Thing Type" };
        String[] propertyNamesFilter3 = { "group.id", "thingType.id" };
		String[] propertyOrdersFilter3 = { "1", "2" };
		String[] operatorFilter3 = { "<", "=" };
		String[] value3 = { "2", "2" };
		Boolean[] isEditable3 = { true, false };

		for( int it = 0; it < Array.getLength( labelsFilter3 ); it++ )
		{
			ReportFilter reportFilter = createReportFilter( labelsFilter3[it], propertyNamesFilter3[it],
			propertyOrdersFilter3[it],
					operatorFilter3[it], value3[it], isEditable3[it], reportDefinition3 );
			ReportFilterService.getInstance().insert( reportFilter );
		}
*/
        // last report

        ReportDefinition reportDefinition7 = new ReportDefinition();
        reportDefinition7.setName("Jackets & Pants Dwell");
        reportDefinition7.setCreatedByUser(createdByUser);
        reportDefinition7.setGroup(group);
        reportDefinition7.setReportType("map");
        reportDefinition7.setDefaultTypeIcon("pin");
        reportDefinition7.setDefaultZoom(20L);
        reportDefinition7.setLocalMapId(1L);
        reportDefinition7.setCenterLat("34.048139");
        reportDefinition7.setCenterLon("-118.443818");
        reportDefinition7.setPinLabels(false);
        reportDefinition7.setZoneLabels(false);
        reportDefinition7.setTrails(false);
        reportDefinition7.setClustering(false);
        reportDefinition7.setPlayback(true);
        reportDefinition7.setNupYup(true);
        reportDefinition7.setDefaultList(false);
        reportDefinition7.setDefaultTypeIcon("Pin");
        reportDefinition7.setDefaultColorIcon("009F6B");
        reportDefinition7.setRunOnLoad(true);
        reportDefinition7 = ReportDefinitionService.getInstance().insert(reportDefinition7);

        String[] labels7 = {"RFID Tag #", "Category", "Logical Reader", "Zone Dwell Time (ms)", "Last Detect Time",
                "Name", "Location"};
        String[] propertyNames7 = {"serial", "category", "logicalReader", "dwellTime( zone )", "lastDetectTime",
                "name", "location"};
        String[] propertyOrders7 = {"1", "2", "3", "4", "5", "6", "7"};
        Long[] propertyTypeIds7 = {0L, 0L, 0L, 0L, 0L, 0L, 0L};
        Boolean[] propertyShowPopUp7 = {true, false, false, false, false, false, false};

        for (int it = 0; it < Array.getLength(labels7); it++) {
            ReportProperty reportProperty = createReportProperty(labels7[it], propertyNames7[it], propertyOrders7[it],
                    propertyTypeIds7[it], propertyShowPopUp7[it], reportDefinition7);
            ReportPropertyService.getInstance().insert(reportProperty);
        }
        reportDefinition7.setPinLabel("1");
        String[] labelsFilter7 = {"Thing Type", "group"};
        String[] propertyNamesFilter7 = {"thingType.id", "group.id"};

        String[] propertyOrdersFilter7 = {"1", "2"};
        String[] operatorFilter7 = {"=", "="};

        String[] value7 = {"2", "2"};
        Boolean[] isEditable7 = {true, false};

        String[] propertyNamesRules7 = {"dwellTime( zone )"};
        String[] operatorRules7 = {">"};
        String[] valueRules7 = {"5000"};
        String[] color7 = {"FF0000"};
        Long[] ids = {3L};

      /*  for( int it = 0; it < Array.getLength( labelsFilter7 ); it++ )
        {
            ReportFilter reportFilter = createReportFilter( labelsFilter7[it], propertyNamesFilter7[it],
            propertyOrdersFilter7[it],
                    operatorFilter7[it], value7[it], isEditable7[it], reportDefinition7 );
            ReportFilterService.getInstance().insert( reportFilter );
        }*/
        float countDisplayOrder = 1;
        for (int it = 0; it < Array.getLength(propertyNamesRules7); it++) {
            ReportRule reportRule = createReportRule(propertyNamesRules7[it], operatorRules7[it], valueRules7[it],
                    color7[it], "",
                    ids[it], reportDefinition7, countDisplayOrder);
            countDisplayOrder++;
            ReportRuleService.getInstance().insert(reportRule);
        }


        // last report

			/*	ReportDefinition reportDefinition8 = new ReportDefinition();
                reportDefinition8.setName( "Zone Dwell-Time Report" );
				reportDefinition8.setCreatedByUser( createdByUser );
				reportDefinition8.setGroup( group );
				reportDefinition8.setReportType( "timeSeries" );
				reportDefinition8.setChartType("line");
				reportDefinition8.setLocalMapId(1L);
				//reportDefinition8.setDefaultZoom( 20L );
				//reportDefinition8.setCenterLat( "34.048139" );
				//reportDefinition8.setCenterLon( "-118.443818" );
				//reportDefinition8.setPinLabels( false );
				//reportDefinition8.setZoneLabels( false );
				//reportDefinition8.setTrails( false );
				//reportDefinition8.setClustering( false );
				//reportDefinition8.setPlayback( true );
				//reportDefinition8.setNupYup( true );
				//reportDefinition8.setDefaultList( false );
				//reportDefinition8.setDefaultTypeIcon( "Pin" );
				//reportDefinition8.setGroupTypeFloor( gt );
				//reportDefinition8.setDefaultColorIcon( "009F6B" );
				reportDefinition8 = ReportDefinitionService.getInstance().insert( reportDefinition8 );

				//String[] labels7 = { "RFID Tag #", "Category", "Logical Reader", "Zone Dwell Time (ms)", "Last
				Detect Time", "Name", "Location" };
				//String[] propertyNames7 = { "serial", "category", "logicalReader", "dwellTime", "lastDetectTime",
				"name", "location" };
				//String[] propertyOrders7 = { "1", "2", "3", "4", "5", "6", "7" };
				//Long[] propertyTypeIds7 = { 3L, 1L, 3L, 3L, 3L, 3L, 3L };

				for( int it = 0; it < Array.getLength( labels7 ); it++ )
				{
					ReportProperty reportProperty = createReportProperty( labels7[it], propertyNames7[it],
					propertyOrders7[it],
							propertyTypeIds7[it], reportDefinition7 );
					ReportPropertyService.getInstance().insert( reportProperty );
				}
				reportDefinition7.setPinLabel( "1" );
				//String[] labelsFilter7 = { "Thing Type", "group" };
				//String[] propertyNamesFilter7 = { "thingType.id", "group.id" };

				//String[] propertyOrdersFilter7 = { "1", "2" };
				//String[] operatorFilter7 = { "=", "=" };

				//String[] value7 = { "2", "2" };
				//Boolean[] isEditable7 = { true, false };

				//String[] propertyNamesRules7 = { "dwellTime" };
				//String[] operatorRules7 = { ">" };
				//String[] valueRules7 = { "5000" };
				//String[] color7 = { "FF0000" };
				//Long[] ids = { 3L };

				/*for( int it = 0; it < Array.getLength( labelsFilter7 ); it++ )
				{
					ReportFilter reportFilter = createReportFilter( labelsFilter7[it], propertyNamesFilter7[it],
					propertyOrdersFilter7[it],
							operatorFilter7[it], value7[it], isEditable7[it], reportDefinition7 );
					ReportFilterService.getInstance().insert( reportFilter );
				}*/

        ReportDefinition reportDefinition8 = new ReportDefinition();
        reportDefinition8.setName("Test_exit_report");
        reportDefinition8.setCreatedByUser(createdByUser);
        reportDefinition8.setGroup(group);
        reportDefinition8.setReportType("tableTimeSeries");
        reportDefinition8.setDefaultTypeIcon("pin");

        reportDefinition8.setPinLabels(false);
        reportDefinition8.setZoneLabels(false);
        reportDefinition8.setTrails(false);
        reportDefinition8.setClustering(false);
        reportDefinition8.setPlayback(false);
        reportDefinition8.setNupYup(false);
        reportDefinition8.setDefaultList(false);
        reportDefinition8.setDefaultColorIcon("009F6B");
        reportDefinition8.setRunOnLoad(true);
        reportDefinition8 = ReportDefinitionService.getInstance().insert(reportDefinition8);
        reportDefinition8.setDefaultColorIcon("009F6B");
        String[] labels8 = {"Assets Serial", "Tag Serial", "Brand", "Category", "Zone", "Location", "Size", "Price"};
        String[] propertyNames8 = {"serial", "serial", "brand", "category", "zone", "location", "size", "price"};
        String[] propertyOrders8 = {"1", "2", "3", "4", "5", "6", "7", "8", "9"};
        Long[] propertyTypeIds8 = {5L, 1L, 5L, 5L, 1L, 1L, 5L, 5L, 5L};
        Boolean[] propertyShowPopUp8 = {true, true, true, true, true, false, false, false, false};
        for (int it = 0; it < Array.getLength(labels8); it++) {
            ReportProperty reportProperty = createReportProperty(labels8[it], propertyNames8[it], propertyOrders8[it],
                    propertyTypeIds8[it], propertyShowPopUp8[it], reportDefinition8);
            ReportPropertyService.getInstance().insert(reportProperty);
        }

        createReportMultiLevel(createdByUser, group, gt);

        createRawMongoReport(createdByUser, group, gt, "Mongo Example #1a", "/mongo/raw_query_report_1a.js", null);
        createRawMongoReport(createdByUser, group, gt, "Mongo Example #1b", "/mongo/raw_query_report_1b.js", null);
        createRawMongoReport(createdByUser, group, gt, "Mongo Example #1c", "/mongo/raw_query_report_1c.js", null);
        createRawMongoReport(createdByUser, group, gt, "Mongo Example #2", "/mongo/raw_query_report_2.js", null);
        createRawMongoReport(createdByUser, group, gt, "Mongo Integrity Test", "/mongo/raw_query_integrity_test.js",
                setReportFilterData());
        createRawMongoReport(createdByUser, group, gt, "Mongo TimeZone Test", "/mongo/raw_query_timezone_test.js", null);
        createRawMongoReport(createdByUser, group, gt, "Mongo Integrity Summary", "/mongo/raw_query_integrity_summary" +
                ".js", setReportFilterData());
        createRawMongoReport(createdByUser, group, gt, "Mongo Filters Query (Now)", "/mongo/filters_query_now" +
                ".js", setReportFilterDataNow()); //VIZIX-2494
        createRawMongoReport(createdByUser, group, gt, "Mongo Filters Query (Custom)", "/mongo/filters_query_custom" +
                ".js", setReportFilterDataCustom()); //VIZIX-2494
        Boolean[] isEditableArray = {true, true, true, true};
        String[] displayOrderArray = {"1", "2", "3", "4"};
        String[] labelFilterArray = {"Thing Type", "Window Size (minutes)", "UTC Offset (+/- hours)", "History (hours)"};
        String[] operatorFilterArray = {"=", "=", "=", "="};
        String[] propertyNameArray = {"thingType.id", "registered", "registered", "registered"};
        String[] propertyValueArray = {"1", "1", "-4", "6"};
        Long[] thingTypeFieldId = {null, 1L, 1L, 1L};

        Map<String, Object[]> filters = new HashMap<>();
        filters.put("isEditable", isEditableArray);
        filters.put("propertyOrdersFilter", displayOrderArray);
        filters.put("labelsFilter", labelFilterArray);
        filters.put("operatorFilter", operatorFilter);
        filters.put("propertyNamesFilter", propertyNameArray);
        filters.put("operatorFilter", operatorFilterArray);
        filters.put("value", propertyValueArray);
        filters.put("thingTypeFieldId", thingTypeFieldId);

        /*Time series scripts from popDB*/
        createRawMongoReport(createdByUser, group, gt, "ViZix TimeSeries Order", "/mongo/ReportScript-TimeseriesOrder" +
                ".js", filters);
        createRawMongoReport(createdByUser, group, gt, "ViZix TimeSeries Order F", "/mongo/ReportScript" +
                "-TimeseriesOrderF.js", filters);
        createRawMongoReport(createdByUser, group, gt, "ViZix TimeSeries Order S", "/mongo/ReportScript" +
                "-TimeseriesOrderS.js", filters);
        createRawMongoReport(createdByUser, group, gt, "ViZix TimeSeries Upsert CB", "/mongo/ReportScript" +
                "-TimeseriesUpsertCB.js", filters);
        createRawMongoReport(createdByUser, group, gt, "ViZix TimeSeries Upsert E", "/mongo/ReportScript" +
                "-TimeseriesUpsertE.js", filters);
        createRawMongoReport(createdByUser, group, gt, "ViZix TimeSeries Upsert EB", "/mongo/ReportScript" +
                "-TimeseriesUpsertEB.js", filters);
        createRawMongoReport(createdByUser, group, gt, "ViZix TimeSeries Upsert FE", "/mongo/ReportScript" +
                "-TimeseriesUpsertFE.js", filters);
        createRawMongoReport(createdByUser, group, gt, "ViZix TimeSeries Upsert IvE", "/mongo/ReportScript" +
                "-TimeseriesUpsertIvE.js", filters);
        createRawMongoReport(createdByUser, group, gt, "ViZix TimeSeries Upsert S#", "/mongo/ReportScript" +
                "-TimeseriesUpsertS#.js", filters);

        isEditableArray = new Boolean[]{true, true};
        displayOrderArray = new String[]{"1", "2"};
        labelFilterArray = new String[]{"Date", "Thing Type"};
        operatorFilterArray = new String[]{"", "="};
        propertyNameArray = new String[]{"relativeDate", "thingType.id"};
        propertyValueArray = new String[]{null};
        thingTypeFieldId = new Long[]{null};
        operatorFilter = new String[]{"==", "="};

        filters = new HashMap<>();
        filters.put("isEditable", isEditableArray);
        filters.put("propertyOrdersFilter", displayOrderArray);
        filters.put("labelsFilter", labelFilterArray);
        filters.put("operatorFilter", operatorFilter);
        filters.put("propertyNamesFilter", propertyNameArray);
        filters.put("operatorFilter", operatorFilterArray);
        filters.put("value", propertyValueArray);
        filters.put("thingTypeFieldId", thingTypeFieldId);

        createRawMongoReport(createdByUser, group, gt, "ViZix TimeSeries Statistic CoreBridge", "/mongo/ReportScript" +
                "-StatisticCoreBridge.js", getReportFilterDataCustomstatistic(),
                "[{\"type\":\"lineCharts\",\"title\":\"Line Charts\",\"subtype\":\"withDataLabels\"," +
                        "\"lib\":\"hc\",\"label\":\"@CHART_LINECHART_LABEL_WITH_DATA_LABELS\",\"disabled\":false," +
                        "\"routeImg\":\"images/charts/line_charts/with_data_labels.svg\",\"activeStatus\":true," +
                        "\"transpose\":true,\"pinDefault\":true,\"name\":\"STAT\"}]");

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

    private static Map<String, Object[]> getReportFilterDataCustomstatistic() {
        Long id=1L;
        try {
            ThingType thingType = ThingTypeService.getInstance().getByCode("coreBridge");
            if (thingType!=null){
                id = thingType.getId();
            }
        } catch (NonUniqueResultException e) {
            logger.error("Error in get thing type by code Corebridge", e);
        }
        String[] labelsFilter = {"Date",  "Thing Type", "Date", "Date" };
        String[] propertyNamesFilter = {"relativeDate", "thingType.id", "startDate", "endDate"};
        String[] operatorFilter = {"", "=", ">=", "<="};
        String[] value = {"CUSTOM", String.valueOf(id), "", ""};
        Boolean[] isEditable = {true, true, true, true};
        String[] propertyOrdersFilter = {"1", "2", "3", "4"};
        Long[] thingTypeFieldId = {0L, 0L, 0L, 0L};
        Integer [] fieldType = {null, null, null, null};
        Map<String, Object[]> reportFilterData = new HashMap<>();
        reportFilterData.put("labelsFilter", labelsFilter);
        reportFilterData.put("propertyNamesFilter", propertyNamesFilter);
        reportFilterData.put("operatorFilter", operatorFilter);
        reportFilterData.put("value", value);
        reportFilterData.put("isEditable", isEditable);
        reportFilterData.put("propertyOrdersFilter", propertyOrdersFilter);
        reportFilterData.put("thingTypeFieldId", thingTypeFieldId);
        reportFilterData.put("fieldType", fieldType);
        return reportFilterData;
    }

    /**
     * @return a Map with report filter data
     */
    public static Map<String, Object[]> setReportFilterData() {
        String[] labelsFilter = {"Date"};
        String[] propertyNamesFilter = {"relativeDate"};
        String[] operatorFilter = {""};
        String[] value = {"NOW"};
        Boolean[] isEditable = {true};
        String[] propertyOrdersFilter = {"1"};
        Map<String, Object[]> reportFilterData = new HashMap<>();
        reportFilterData.put("labelsFilter", labelsFilter);
        reportFilterData.put("propertyNamesFilter", propertyNamesFilter);
        reportFilterData.put("operatorFilter", operatorFilter);
        reportFilterData.put("value", value);
        reportFilterData.put("isEditable", isEditable);
        reportFilterData.put("propertyOrdersFilter", propertyOrdersFilter);
        return reportFilterData;
    }
    /**
     * @return a Map with report filter data
     */
    public static Map<String, Object[]> setReportFilterDataNow() {
        String[] labelsFilter = {"Date", "Tenant Group", "Thing Type", "shift"};
        String[] propertyNamesFilter = {"relativeDate", "group.id", "thingType.id", "shift"};
        String[] operatorFilter = {"", "<",  "=", "isNotEmpty"};
        String[] value = {"NOW", "3",  "1", ""};
        Boolean[] isEditable = {true, true, true, true};
        String[] propertyOrdersFilter = {"2", "1", "3", "4"};
        Long[] thingTypeFieldId = {0L, 0L, 0L, 1L};
        Integer [] fieldType = {null, null, null, null};
        Map<String, Object[]> reportFilterData = new HashMap<>();
        reportFilterData.put("labelsFilter", labelsFilter);
        reportFilterData.put("propertyNamesFilter", propertyNamesFilter);
        reportFilterData.put("operatorFilter", operatorFilter);
        reportFilterData.put("value", value);
        reportFilterData.put("isEditable", isEditable);
        reportFilterData.put("propertyOrdersFilter", propertyOrdersFilter);
        reportFilterData.put("thingTypeFieldId", thingTypeFieldId);
        reportFilterData.put("fieldType", fieldType);
        return reportFilterData;
    }
    /**
     * @return a Map with report filter data
     */
    public static Map<String, Object[]> setReportFilterDataCustom() {
        String[] labelsFilter = {"Tenant Group", "Date",  "Thing Type", "zone", "shift", "registered", "Date", "Date" };
        String[] propertyNamesFilter = {"group.id", "relativeDate", "thingType.id", "zone", "shift", "registered", "startDate", "endDate"};
        String[] operatorFilter = {"<", "", "=", "isNotEmpty", "isEmpty", ">", ">=", "<="};
        String[] value = {"3", "CUSTOM", "1", "", "", "5", "1483300233000", "1514749838999"};
        Boolean[] isEditable = {true, true, true, true, true, true, true, true};
        String[] propertyOrdersFilter = {"1", "2", "5", "6", "7", "8", "3", "4"};
        Long[] thingTypeFieldId = {0L, 0L, 0L, 1L, 1L, 1L, 0L, 0L};
        Integer [] fieldType = {null, null, null, 9, 7, 4, null, null};
        Map<String, Object[]> reportFilterData = new HashMap<>();
        reportFilterData.put("labelsFilter", labelsFilter);
        reportFilterData.put("propertyNamesFilter", propertyNamesFilter);
        reportFilterData.put("operatorFilter", operatorFilter);
        reportFilterData.put("value", value);
        reportFilterData.put("isEditable", isEditable);
        reportFilterData.put("propertyOrdersFilter", propertyOrdersFilter);
        reportFilterData.put("thingTypeFieldId", thingTypeFieldId);
        reportFilterData.put("fieldType", fieldType);
        return reportFilterData;
    }
    private void createReportMultiLevel(User createdByUser, Group group, GroupType gt) {
        ReportDefinition reportDefinition3 = new ReportDefinition();
        reportDefinition3.setName("Multilevel Report");
        reportDefinition3.setCreatedByUser(createdByUser);
        reportDefinition3.setGroup(group);
        reportDefinition3.setReportType("table");
        reportDefinition3.setDefaultTypeIcon("pin");

        reportDefinition3.setPinLabels(false);
        reportDefinition3.setZoneLabels(false);
        reportDefinition3.setTrails(false);
        reportDefinition3.setClustering(false);
        reportDefinition3.setPlayback(true);
        reportDefinition3.setNupYup(false);
        reportDefinition3.setDefaultList(false);
        reportDefinition3.setDefaultColorIcon("009F6B");
        reportDefinition3.setRunOnLoad(true);
        reportDefinition3 = ReportDefinitionService.getInstance().insert(reportDefinition3);
        reportDefinition3.setDefaultColorIcon("009F6B");
        reportDefinition3.setIsMobile(Boolean.FALSE);
        reportDefinition3.setIsMobileDataEntry(Boolean.FALSE);
        String[] labels3 = {"Name Shipping Order", "name Asset", "Name Tag"};
        String[] propertyNames3 = {"name", "name", "name", "name"};
        String[] propertyOrders3 = {"1", "2", "3"};
        Long[] propertyTypeIds3 = {6L, 7L, 8L};
        Boolean[] propertyShowPopUp3 = {true, true, true};
        for (int it = 0; it < Array.getLength(labels3); it++) {
            ReportProperty reportProperty = createReportProperty(labels3[it],
                    propertyNames3[it],
                    propertyOrders3[it],
                    propertyTypeIds3[it],
                    propertyShowPopUp3[it],
                    reportDefinition3);
            ReportPropertyService.getInstance().insert(reportProperty);
        }
    }

    private void createRawMongoReport(User createdByUser, Group group, GroupType gt, String name, String fileName,
                                      Map<String, Object[]> reportFilterData) {
        createRawMongoReport(createdByUser, group, gt, name, fileName, reportFilterData,null);
    }

    private void createRawMongoReport(User createdByUser, Group group, GroupType gt, String name, String fileName,
                                      Map<String, Object[]> reportFilterData, String charViewProperties0) {
        ReportDefinition rd = new ReportDefinition();
        rd.setName(name);
        rd.setCreatedByUser(createdByUser);
        rd.setGroup(group);
        rd.setReportType("mongo");
        rd.setIsMobile(Boolean.FALSE);
        rd.setIsMobileDataEntry(Boolean.FALSE);
        rd.setChartViewProperties(charViewProperties0);
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
            Integer[] fieldType = (Integer[]) reportFilterData.get("fieldType");
            Integer fieldTypeIndividual = null;
            for (int it = 0; it < Array.getLength(labelsFilter); it++) {
                if ((fieldType != null) && (fieldType.length > 0)){
                    fieldTypeIndividual = fieldType[it];
                }
                if (thingTypeId == null || thingTypeId.length == 0) {
                    ReportFilterService.getInstance().insert(createReportFilter(labelsFilter[it],
                            propertyNamesFilter[it],
                            propertyOrdersFilter[it], operatorFilter[it], value[it], isEditable[it], rd, fieldTypeIndividual));
                } else if (thingTypeId[it] != null) {
                    ReportFilterService.getInstance().insert(
                            createReportFilter(
                                    labelsFilter[it],
                            propertyNamesFilter[it],
                            propertyOrdersFilter[it], operatorFilter[it], value[it], isEditable[it], thingTypeId[it],
                            rd,
                            fieldTypeIndividual));
                } else {
                    ReportFilterService.getInstance().insert(createReportFilter(labelsFilter[it],
                            propertyNamesFilter[it],
                            propertyOrdersFilter[it], operatorFilter[it], value[it], isEditable[it], rd, fieldTypeIndividual));
                }

            }
        }
    }

    /**
     * Create StarFLEX Reports: Device and Tag
     *
     * @param createdByUser User who creates the report
     * @param group         Company
     */
    public static void createStarFLEXReport(
            User createdByUser
            , Group group
            , ThingType thingTypeStarFlex
            , ThingType thingTypeStarFlexTag
    ) {
        /*************STARflex Dashboard Report*/
        Map<String, Object> filtersDashboard = new HashMap<>();
        filtersDashboard.put("labels", new String[]{"Thing Type", "group"});
        filtersDashboard.put("propertyNames", new String[]{"thingType.id", "group.id"});
        filtersDashboard.put("propertyOrders", new String[]{"1", "2"});
        filtersDashboard.put("operators", new String[]{"==", "="});
        filtersDashboard.put("values", new String[]{thingTypeStarFlex.getId().toString(), group.getId().toString()});
        filtersDashboard.put("isEditable", new Boolean[]{false, false});

        Map<String, Object> propertiesDashboard = new HashMap<>();
        propertiesDashboard.put("labels", new String[]{"MAC id", "IP", "Firmware", "Discovered", "Claimed", "State",
                "Date Time"});
        propertiesDashboard.put("propertyNames", new String[]{"macId", "netIfs--eth0__0__--address", "firmwareVersion"
                , "discoveryDate", "claimDate", "state", "wallclock"});
        propertiesDashboard.put("propertyOrders", new String[]{"1", "2", "3", "4", "5", "6", "7"});
        propertiesDashboard.put("propertyTypes", new Long[]{thingTypeStarFlex.getId(), thingTypeStarFlex.getId()
                , thingTypeStarFlex.getId(), thingTypeStarFlex.getId(), thingTypeStarFlex.getId()
                , thingTypeStarFlex.getId(), thingTypeStarFlex.getId()});
        propertiesDashboard.put("showPopUp", new Boolean[]{true, true, true, true, true, true, true, true, true});

        createReportDefinition(Constants.STARFLEX_DASHBOARD
                , group
                , createdByUser
                , filtersDashboard
                , propertiesDashboard);

        /****************STARflex Monitor*/
        Map<String, Object> propertiesMonitor = new HashMap<>();
        propertiesMonitor.put("labels", new String[]{"IP", "MAC id", "State", "Uptime", "Free Memory", "CPU Utilization"
                , "Temperature", "LED Status"});
        propertiesMonitor.put("propertyNames", new String[]{"netIfs--eth0__0__--address", "macId", "state",
                "upTime_sec", "freeMem", "cpuUtilization"
                , "temperature_f_", "ledStatus"});
        propertiesMonitor.put("propertyOrders", new String[]{"1", "2", "3", "4", "5", "6", "7", "8"});
        propertiesMonitor.put("propertyTypes", new Long[]{thingTypeStarFlex.getId(), thingTypeStarFlex.getId()
                , thingTypeStarFlex.getId(), thingTypeStarFlex.getId()
                , thingTypeStarFlex.getId(), thingTypeStarFlex.getId()
                , thingTypeStarFlex.getId(), thingTypeStarFlex.getId()});
        propertiesMonitor.put("showPopUp", new Boolean[]{true, true, true, true, true, true, true, true});

        createReportDefinition(Constants.STARFLEX_MONITOR
                , group
                , createdByUser
                , filtersDashboard
                , propertiesMonitor);

        /***************STARflex Unique Tags Report*/
        Map<String, Object> filtersUniqueTags = new HashMap<>();
        filtersUniqueTags.put("labels", new String[]{"Thing Type", "group"});
        filtersUniqueTags.put("propertyNames", new String[]{"thingType.id", "group.id"});
        filtersUniqueTags.put("propertyOrders", new String[]{"1", "2"});
        filtersUniqueTags.put("operators", new String[]{"==", "="});
        filtersUniqueTags.put("values", new String[]{thingTypeStarFlexTag.getId().toString(), group.getId().toString
                ()});
        filtersUniqueTags.put("isEditable", new Boolean[]{false, false});

        Map<String, Object> propertiesUniqueTags = new HashMap<>();
        propertiesUniqueTags.put("labels", new String[]{"Name", "Timestamp", "TxID"});
        propertiesUniqueTags.put("propertyNames", new String[]{"name", "datestamp", "TxID"});
        propertiesUniqueTags.put("propertyOrders", new String[]{"1", "2", "3"});
        propertiesUniqueTags.put("propertyTypes", new Long[]{thingTypeStarFlexTag.getId(), thingTypeStarFlexTag.getId()
                , thingTypeStarFlexTag.getId()});
        propertiesUniqueTags.put("showPopUp", new Boolean[]{true, true, true});

        createReportDefinition(Constants.STARFLEX_UNIQUE_TAGS
                , group
                , createdByUser
                , filtersUniqueTags
                , propertiesUniqueTags);
    }

    /**
     * Method to create a new Report Definition
     *
     * @param namereport
     * @param group         Group object
     * @param createdByUser User object
     * @param filters       Map with properties of filter
     * @param properties    Map with properties of the section 'properties'
     */
    public static void createReportDefinition(
            String namereport
            , Group group
            , User createdByUser
            , Map<String, Object> filters
            , Map<String, Object> properties) {
        ReportDefinition reportDefinitionDevice = new ReportDefinition();
        reportDefinitionDevice.setName(namereport.trim() + " - " + group.getName());
        reportDefinitionDevice.setCreatedByUser(createdByUser);
        reportDefinitionDevice.setGroup(group);
        reportDefinitionDevice.setReportType("table");
        reportDefinitionDevice.setDefaultTypeIcon("pin");

        reportDefinitionDevice.setPinLabels(false);
        reportDefinitionDevice.setZoneLabels(false);
        reportDefinitionDevice.setTrails(false);
        reportDefinitionDevice.setClustering(false);
        reportDefinitionDevice.setPlayback(true);
        reportDefinitionDevice.setNupYup(false);
        reportDefinitionDevice.setDefaultList(false);
        reportDefinitionDevice.setDefaultColorIcon("009F6B");
        reportDefinitionDevice.setRunOnLoad(true);
        reportDefinitionDevice = ReportDefinitionService.getInstance().insert(reportDefinitionDevice);
        reportDefinitionDevice.setDefaultColorIcon("009F6B");
        reportDefinitionDevice.setIsMobile(Boolean.FALSE);
        reportDefinitionDevice.setIsMobileDataEntry(Boolean.FALSE);

        //Report Filter
        if (filters != null && filters.size() > 0) {
            String[] labelsFilter = (String[]) filters.get("labels");
            String[] propertyNamesFilter = (String[]) filters.get("propertyNames");
            String[] propertyOrdersFilter = (String[]) filters.get("propertyOrders");
            String[] operatorFilter = (String[]) filters.get("operators");
            String[] value = (String[]) filters.get("values");
            Boolean[] isEditable = (Boolean[]) filters.get("isEditable");
            Integer[] fieldType = (Integer[]) filters.get("fieldType");
            Integer fieldTypeIndividual = null;
            for (int it = 0; it < Array.getLength(labelsFilter); it++) {
                if ((fieldType != null) && (fieldType.length > 0)){
                    fieldTypeIndividual = fieldType[it];
                }
                ReportFilter reportFilter = createReportFilter(
                        labelsFilter[it]
                        , propertyNamesFilter[it]
                        , propertyOrdersFilter[it]
                        , operatorFilter[it]
                        , value[it]
                        , isEditable[it]
                        , reportDefinitionDevice
                        , fieldTypeIndividual);
                ReportFilterService.getInstance().insert(reportFilter);
            }
        }

        //Report Property
        String[] labels3 = (String[]) properties.get("labels");
        String[] propertyNames3 = (String[]) properties.get("propertyNames");
        String[] propertyOrders3 = (String[]) properties.get("propertyOrders");
        Long[] propertyTypeIds3 = (Long[]) properties.get("propertyTypes");
        Boolean[] propertyShowPopUp3 = (Boolean[]) properties.get("showPopUp");
        for (int it = 0; it < Array.getLength(labels3); it++) {
            ReportProperty reportProperty = createReportProperty(labels3[it],
                    propertyNames3[it],
                    propertyOrders3[it],
                    propertyTypeIds3[it],
                    propertyShowPopUp3[it],
                    reportDefinitionDevice);
            ReportPropertyService.getInstance().insert(reportProperty);
        }
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
        Group      rootGroup  = GroupService.getInstance().getRootGroup();
        Connection connection = new Connection();
        connection.setName("MSSQLServer");
        connection.setCode("MSSQLServer");
        connection.setGroup(rootGroup);
        connection.setConnectionType(ConnectionTypeService.getInstance()
                .getConnectionTypeDAO()
                .selectBy(QConnectionType.connectionType.code.eq(
                        "DBConnection")));
        JSONObject jsonProperties = new JSONObject();
        Map<String, Object> mapProperties  = new LinkedHashMap<String, Object>();
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
        connection.setName( "MySQLServer" );
        connection.setCode( "MySQLServer" );
        connection.setGroup( rootGroup );
        connection.setConnectionType( ConnectionTypeService.getInstance().getConnectionTypeDAO()
                .selectBy( QConnectionType.connectionType.code.eq( "DBConnection" ) ) );
        jsonProperties = new JSONObject();
        mapProperties = new LinkedHashMap<String, Object>();
        mapProperties.put( "driver", "com.mysql.jdbc.Driver" );
        mapProperties.put( "password", "Y29udHJvbDEyMyE=" );
        mapProperties.put( "schema", "ct-app-center" );
        mapProperties.put( "url", "jdbc:mysql://localhost:3306/ct-app-center" );
        mapProperties.put( "user", "root" );
        jsonProperties.putAll( mapProperties );
        connection.setProperties( jsonProperties.toJSONString() );
        ConnectionService.getInstance().insert( connection );


        // MySQL connection (for the RIOT_MAIN schema)
        connection = new Connection();
        connection.setName( "SQL" );
        connection.setCode( "SQL" );
        connection.setGroup( rootGroup );
        connection.setConnectionType( ConnectionTypeService.getInstance().getConnectionTypeDAO()
                .selectBy( QConnectionType.connectionType.code.eq( "SQL" ) ) );
        jsonProperties = new JSONObject();
        mapProperties = new LinkedHashMap<String, Object>();
        mapProperties.put( "driver", "com.mysql.jdbc.Driver" );
        mapProperties.put( "dialect", "org.hibernate.dialect.MySQLDialect" );
        mapProperties.put( "username", "root" );
        mapProperties.put( "password", "Y29udHJvbDEyMyE=" );
        mapProperties.put( "url", "jdbc:mysql://localhost:3306/riot_main" );
        mapProperties.put( "hazelcastNativeClientAddress", "localhost" );
        jsonProperties.putAll( mapProperties );
        connection.setProperties( jsonProperties.toJSONString() );
        ConnectionService.getInstance().insert( connection );

        // VIZIX REST
        connection = new Connection();
        connection.setName( "Services" );
        connection.setCode( "SERVICES" );
        connection.setGroup( rootGroup );
        connection.setConnectionType(
                ConnectionTypeService.getInstance().getConnectionTypeDAO().selectBy( QConnectionType.connectionType.code.eq( "REST" ) ) );
        jsonProperties = new JSONObject();
        mapProperties = new LinkedHashMap<String, Object>();
        mapProperties.put( "host", "localhost" );
        mapProperties.put( "port", 8080 );
        mapProperties.put( "contextpath", "/riot-core-services" );
        mapProperties.put( "apikey", "7B4BCCDC" );
        mapProperties.put("secure", false );
        jsonProperties.putAll( mapProperties );
        connection.setProperties( jsonProperties.toJSONString() );
        ConnectionService.getInstance().insert( connection );

        // MQTT connection example
        connection = new Connection();
        connection.setName( "Mqtt" );
        connection.setCode( "MQTT" );
        connection.setGroup( rootGroup );
        connection.setConnectionType(
                ConnectionTypeService.getInstance().getConnectionTypeDAO().selectBy( QConnectionType.connectionType.code.eq( "MQTT" ) ) );
        jsonProperties = new JSONObject();
        mapProperties = new LinkedHashMap<String, Object>();
        mapProperties.put( "host", "localhost" );
        mapProperties.put( "port", 1883 );
        mapProperties.put( "qos", 2 );
        mapProperties.put( "secure", false );
        jsonProperties.putAll( mapProperties );
        connection.setProperties( jsonProperties.toJSONString() );
        ConnectionService.getInstance().insert( connection );

        // MQTT connection example Santa Monica
        connection = new Connection();
        connection.setName( "Mqtt Santa Monica" );
        connection.setCode( "MQTTSM" );
        connection.setGroup( rootGroup );
        connection.setConnectionType(
                ConnectionTypeService.getInstance().getConnectionTypeDAO().selectBy( QConnectionType.connectionType.code.eq( "MQTT" ) ) );
        jsonProperties.putAll( mapProperties );
        connection.setProperties( jsonProperties.toJSONString() );
        ConnectionService.getInstance().insert( connection );

        // MQTT connection example Discovered
        connection = new Connection();
        connection.setName( "Mqtt Discovered" );
        connection.setCode( "MQTTSF" );
        connection.setGroup( rootGroup );
        connection.setConnectionType(
                ConnectionTypeService.getInstance().getConnectionTypeDAO().selectBy( QConnectionType.connectionType.code.eq( "MQTT" ) ) );
        jsonProperties.putAll( mapProperties );
        connection.setProperties( jsonProperties.toJSONString() );
        ConnectionService.getInstance().insert( connection );

        // MONGO
        connection = new Connection();
        connection.setName( "MongoDB" );
        connection.setCode( "MONGO" );
        connection.setGroup( rootGroup );
        connection.setConnectionType(
                ConnectionTypeService.getInstance().getConnectionTypeDAO().selectBy( QConnectionType.connectionType.code.eq( "MONGO" ) ) );
        jsonProperties = new JSONObject();
        mapProperties = new LinkedHashMap<String, Object>();

        mapProperties.put( "mongoPrimary", "127.0.0.1:27017" );
        mapProperties.put( "mongoSecondary", "" );
        mapProperties.put( "mongoReplicaSet", "" );
        mapProperties.put( "mongoSSL", false );
        mapProperties.put( "username", "admin" );
        mapProperties.put( "password", "Y29udHJvbDEyMyE=" );
        mapProperties.put( "mongoAuthDB", "admin" );
        mapProperties.put( "mongoDB", "riot_main" );
        mapProperties.put( "mongoSharding", false );
        mapProperties.put( "mongoConnectTimeout", 0 );
        mapProperties.put( "mongoMaxPoolSize", 0 );

        jsonProperties.putAll( mapProperties );
        connection.setProperties( jsonProperties.toJSONString() );
        ConnectionService.getInstance().insert( connection );

        // MONGO Santa Monica
        connection = new Connection();
        connection.setName( "MongoDB Santa Monica" );
        connection.setCode( "MONGOSM" );
        connection.setGroup( rootGroup );
        connection.setConnectionType(
                ConnectionTypeService.getInstance().getConnectionTypeDAO().selectBy( QConnectionType.connectionType.code.eq( "MONGO" ) ) );
        jsonProperties.putAll( mapProperties );
        connection.setProperties( jsonProperties.toJSONString() );
        ConnectionService.getInstance().insert( connection );

        // MONGO Discovered
        connection = new Connection();
        connection.setName( "MongoDB Discovered" );
        connection.setCode( "MONGOSF" );
        connection.setGroup( rootGroup );
        connection.setConnectionType(
                ConnectionTypeService.getInstance().getConnectionTypeDAO().selectBy( QConnectionType.connectionType.code.eq( "MONGO" ) ) );
        jsonProperties.putAll( mapProperties );
        connection.setProperties( jsonProperties.toJSONString() );
        ConnectionService.getInstance().insert( connection );

        // FTP connection example
        connection = new Connection();
        connection.setName( "Ftp" );
        connection.setCode( "FTP" );
        connection.setGroup( rootGroup );
        connection.setConnectionType(
                ConnectionTypeService.getInstance().getConnectionTypeDAO().selectBy( QConnectionType.connectionType.code.eq( "FTP" ) ) );
        jsonProperties = new JSONObject();
        mapProperties = new LinkedHashMap<String, Object>();
        mapProperties.put( "username", "ftpUser" );
        mapProperties.put( "password", "MTIzNA==" );
        mapProperties.put( "host", "localhost" );
        mapProperties.put( "port", 21 );
        mapProperties.put("secure", false );
        jsonProperties.putAll( mapProperties );
        connection.setProperties( jsonProperties.toJSONString() );
        ConnectionService.getInstance().insert( connection );

        // FTP connection example
        connection = new Connection();
        connection.setName( "Ftp Santa Monica" );
        connection.setCode( "FTPSM" );
        connection.setGroup( rootGroup );
        connection.setConnectionType(
                ConnectionTypeService.getInstance().getConnectionTypeDAO().selectBy( QConnectionType.connectionType.code.eq( "FTP" ) ) );
        jsonProperties.putAll( mapProperties );
        connection.setProperties( jsonProperties.toJSONString() );
        ConnectionService.getInstance().insert( connection );


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
        InputStream    is            = this.getClass()
                .getClassLoader()
                .getResourceAsStream("superSubscriberConfig.json");
        BufferedReader reader        = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        String         line          = null;
        StringBuilder  stringBuilder = new StringBuilder();
        String         ls            = System.getProperty("line.separator");

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
                                                        Boolean rateFilter,
                                                        Boolean lastDetectFilterTypes,
                                                        String brokerCon,
                                                        String mongoCon) {
        ObjectMapper mapper = new ObjectMapper();

        ObjectNode configuration = mapper.createObjectNode();
        configuration.put("thingTypeCode" , thingTypeCode );
        configuration.put("thingTypeCodeDevice" , thingTypeCodeDevice );
        configuration.put("messageMode" , messageMode);
        //MQTT
        ObjectNode mqtt= mapper.createObjectNode();
        mqtt.put("connectionCode", brokerCon);
        configuration.set("mqtt", mqtt );

        //MONGO
        ObjectNode mongo= mapper.createObjectNode();
        mongo.put("connectionCode", mongoCon);
        configuration.set("mongo", mongo);
        //RATE_FILTER
        if(rateFilter){
            setRateFilter(configuration, mapper);
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

        //LAST_DETECT_FILTER_TYPES
        if (lastDetectFilterTypes){
            configuration.put("lastDetectFilterTypes" , "");
        }

        return configuration.toString();
    }

    private static void setRateFilter(ObjectNode configuration, ObjectMapper mapper){
        ObjectNode rateFilterO = mapper.createObjectNode();
        rateFilterO.put("active", 1);
        rateFilterO.put("timeLimit", 5);
        configuration.set("rateFilter", rateFilterO);
    }

    public static String getStarFlexTagBridgeConfiguration(String thingTypeCode,
                                                           String thingTypeCodeDevice,
                                                           String messageMode,
                                                           Boolean rateFilter,
                                                           Boolean lastDetectFilterTypes,
                                                           String brokerCon,
                                                           String mongoCon) {
        ObjectMapper mapper = new ObjectMapper();

        ObjectNode configuration = mapper.createObjectNode();
        configuration.put("thingTypeCode", thingTypeCode);
        configuration.put("thingTypeCodeDevice", thingTypeCodeDevice);
        configuration.put("messageMode", messageMode);

        //MQTT
        ObjectNode mqtt = mapper.createObjectNode();
        mqtt.put("connectionCode", brokerCon);
        configuration.set("mqtt", mqtt);

        //MONGO
        ObjectNode mongo = mapper.createObjectNode();
        mongo.put("connectionCode", mongoCon);
        configuration.set("mongo", mongo);

        //RATE_FILTER
        if(rateFilter){
            setRateFilter(configuration, mapper);
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

        //LAST_DETECT_FILTER_TYPES
        if (lastDetectFilterTypes){
            configuration.put("lastDetectFilterTypes" , "");
        }

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
