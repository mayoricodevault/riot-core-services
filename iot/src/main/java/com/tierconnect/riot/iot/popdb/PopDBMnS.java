package com.tierconnect.riot.iot.popdb;

import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.popdb.PopDBRequired;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;
import com.tierconnect.riot.appcore.services.*;
import com.tierconnect.riot.appcore.utils.Configuration;
import com.tierconnect.riot.iot.dao.mongo.MongoScriptDAO;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.sdk.dao.MongoExecutionException;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.hibernate.Transaction;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * TODO: Delete all dependencies of Mojix and create thingStructure for MnS
 *
 * @author : rchirinos
 * @date : 10/14/16 7:56 AM
 * @version:
 */
public class PopDBMnS {
    private static final Logger logger = Logger.getLogger(PopDBMojixRetail.class);

    public static void main(String args[]) throws Exception {
        PopDBRequired.initJDBCDrivers();
        System.getProperties().put("hibernate.hbm2ddl.auto", "update");
        System.getProperties().put("hibernate.cache.use_second_level_cache", "false");
        System.getProperties().put("hibernate.cache.use_query_cache", "false");
        Configuration.init(null);

        PopDBMnS popdb = new PopDBMnS();
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
            GroupService.getGroupDAO().getSession().refresh(RoleResourceService.getInstance().get(Long.parseLong(i + "")));
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
    }

    public void run() throws NonUniqueResultException {
        createData();
        PopulateDBRiotMaker prm = new PopulateDBRiotMaker();
        prm.demo();
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

        //TODO Set Group MARKS & SPENCER and Stores.
        Group marksAndSpencerGroup = PopDBUtils.popGroup("Marks & Spencer", "MnS", rootGroup, companyGroupType, "");
        PopDBUtils.popGroup("Brooklands ", "Brooklands", marksAndSpencerGroup, storeGroupType, "");
        PopDBUtils.popGroup("Derby", "Derby", marksAndSpencerGroup, storeGroupType, "");
        PopDBUtils.popGroup("Fosse Park", "Fosse", marksAndSpencerGroup, storeGroupType, "");

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
        Role companyUser = PopDBUtils.popRole( "Store User", "Store User", new ArrayList<Resource>(), mojix, storeGroupType );
        Role companyadmin = PopDBUtils.popRole("Store Administrator", "Store Administrator", new ArrayList<Resource>(), mojix,
                storeGroupType);

        Role storeManager = PopDBUtils.popRole("Store Manager", "Role store manager", null, mojix, storeGroupType);
        Role storeEmployee = PopDBUtils.popRole("Store Employee", "Role store employee", null, mojix, storeGroupType);
        Role pantManager = PopDBUtils.popRole("Pants Manager", "Pants manager", null, mojix, storeGroupType);
        Role reportManager = PopDBUtils.popRole("Report Manager", "Report manager", null, mojix, null);
        List<Resource> resources1 = ResourceService.list();

        for (Resource resource : resources1) {

            if (resource.getName().toString().equals("Reports")) {
                RoleResourceService.getInstance().insert(storeEmployee, resource, "x");
                RoleResourceService.getInstance().insert(reportManager, resource, "x");
            }

            if (resource.getName().toString().equals("reportDefinition") || resource.getName().toString().equals("reportFilter")
                    || resource.getName().toString().equals("reportProperty")) {

                // RoleResourceService.getInstance().insert( storeManager,
                // resource, "iuda" );
                RoleResourceService.getInstance().insert(storeEmployee, resource, "r");
                RoleResourceService.getInstance().insert(reportManager, resource, "r");

            }
            if (resource.getName().toString().equals("$Pants") || resource.getName().toString().equals("$Jackets")
                    || resource.getName().toString().equals("$Passive RFID Tags")) {
                if (resource.getName().toString().equals("$Pants")) {
                    RoleResourceService.getInstance().insert(pantManager, resource, "riuda");
                }

            }

            if (resource.getName().toString().equals("localMap")) {
                RoleResourceService.getInstance().insert(pantManager, resource, "riuda");
            }

        }
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
        samUser.setEmail("samuel.levy@mojix.com ");

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
        paulUser.setEmail("paul.barriga@mojix.com ");
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

        final String[ ] topicMqtt = {"/v1/data/ALEB2", "/v1/data/ALEB3"};

        //TODO Ruth needs to review this
//        Edgebox edgeboxMCB2 = PopDBIOTUtils.popEdgebox(mojix, "* Core Bridge Mojix", "CB1", "core",
//                PopDBRequiredIOT.getOldCoreBridgeConfiguration(topicMqtt), 0L);

        EdgeboxService.getInstance().insertEdgebox(marksAndSpencerGroup, "FTP Bridge Colour", "MnSFTPcolour", "FTP",
                PopDBMojixRetail.getFtpBridgeColourConfiguration("FTPMNS", "MQTTMNS"), 0L);

        EdgeboxService.getInstance().insertEdgebox(marksAndSpencerGroup, "FTP Bridge Dept", "MnSFTPdept", "FTP",
                getFtpBridgeDeptConfiguration(), 0L);

        EdgeboxService.getInstance().insertEdgebox(marksAndSpencerGroup, "FTP Bridge UPC", "MnSFTPupc", "FTP",
                getFtpBridgeUpcConfiguration(), 0L);

        EdgeboxService.getInstance().insertEdgebox(marksAndSpencerGroup, "FTP Bridge Reassociation", "MnSFTPCRFIDCSV", "FTP",
                getBridgeReassociationConfiguration(), 0L);

        EdgeboxService.getInstance().insertEdgebox(marksAndSpencerGroup, "FTP Bridge Transaction", "MnSFTPedw", "FTP",
                getBridgeTransactionConfiguration(), 0L);

        EdgeboxService.getInstance().insertEdgebox(marksAndSpencerGroup, "FTP Bridge GMD", "MnSFTPgmd", "FTP",
                getFtpBridgeGmdConfiguration(), 0L);

        EdgeboxService.getInstance().insertEdgebox(marksAndSpencerGroup, "FTP Bridge SYW", "MnSFTPsyw", "FTP",
                getFtpBridgeBywConfiguration(), 0L);
    }

    private ReportFilter createReportFilter(String label, String propertyName, String propertyOrder, String operatorFilter, String value,
                                            Boolean isEditable, Long ttId, ReportDefinition reportDefinition) {
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
        reportFilter.setThingTypeField(ThingTypeFieldService.getInstance().getThingTypeFieldByName(propertyName).get(0));
        reportFilter.setReportDefinition(reportDefinition);
        System.out.println("reportDefinition******>> " + reportDefinition.getId());
        return reportFilter;
    }

    private static ReportFilter createReportFilter(String label, String propertyName, String propertyOrder, String operatorFilter, String value,
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

    private static ReportProperty createReportProperty(String label, String propertyName, String propertyOrder, Long propertyTypeId, Boolean showPopUp,
                                                       ReportDefinition reportDefinition) {
        ReportProperty reportProperty = new ReportProperty();
        reportProperty.setLabel(label);
        reportProperty.setPropertyName(propertyName);
        reportProperty.setDisplayOrder(Float.parseFloat(propertyOrder));
        reportProperty.setThingType(ThingTypeService.getInstance().get(propertyTypeId));
        List<ThingTypeField> lstThingTypeField = ThingTypeFieldService.getInstance().getThingTypeFieldByName(propertyName);
        if (lstThingTypeField != null && lstThingTypeField.size() > 0) {
            reportProperty.setThingTypeField(lstThingTypeField.get(0));
        }

        reportProperty.setShowHover(showPopUp);
        reportProperty.setReportDefinition(reportDefinition);
        return reportProperty;
    }

    private ReportRule createReportRule(String propertyName, String operator, String value, String color, String style, Long TID,
                                        ReportDefinition reportDefinition) {
        ReportRule reportRule = new ReportRule();
        reportRule.setPropertyName(propertyName);
        reportRule.setOperator(operator);
        reportRule.setValue(value);
        reportRule.setColor(color);
        reportRule.setStyle(style);
        reportRule.setReportDefinition(reportDefinition);
        reportRule.setThingType(ThingTypeService.getInstance().get(TID));
        List<ThingTypeField> lstThingTypeField = ThingTypeFieldService.getInstance().getThingTypeFieldByName(propertyName);
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

        String[] labels5 = {"Brand", "Category", "RFID Tag #", "Department", "Last Detect Time", "Logical Reader", "Color", "Size", "Price",
                "Name", "Type"};
        String[] propertyNames5 = {"brand", "category", "serial", "group.name", "lastDetectTime", "logicalReader", "color", "size",
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
        for (int it = 0; it < Array.getLength(labelsFilter5); it++) {
            ReportFilter reportFilter = createReportFilter(labelsFilter5[it], propertyNamesFilter5[it], propertyOrdersFilter5[it],
                    operatorFilter5[it], value5[it], isEditable5[it], thingTypeIdReport5[it], reportDefinition5);
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

        String[] labels6 = {"Brand", "Category", "RFID Tag #", "Department", "Last Detect Time", "Logical Reader", "Color", "Size", "Price",
                "Name", "Type"};
        String[] propertyNames6 = {"brand", "category", "serial", "group.name", "lastDetectTime", "logicalReader", "color", "size",
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

        for (int it = 0; it < Array.getLength(labelsFilter6); it++) {
            ReportFilter reportFilter = createReportFilter(labelsFilter6[it], propertyNamesFilter6[it], propertyOrdersFilter6[it],
                    operatorFilter6[it], value6[it], isEditable6[it], thingTypeIdReport6[it], reportDefinition6);

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

        String[] labels = {"RFID Tag #", "Group", "Zone", "Zone Dwell Time (zone)", "Last Detect Time", "Name", "Location"};
        String[] propertyNames = {"serial", "group.name", "zone", "dwellTime( zone )", "lastDetectTime", "name", "location"};
        String[] propertyOrders = {"1", "2", "3", "4", "5", "6", "7"};
        Long[] propertyTypeIds = {1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L};
        Boolean[] propertyShowPopUp = {true, false, false, false, false, true, false};

        for (int it = 0; it < Array.getLength(labels); it++) {
            ReportProperty reportProperty = createReportProperty(labels[it], propertyNames[it], propertyOrders[it], propertyTypeIds[it],
                    propertyShowPopUp[it], reportDefinition);
            ReportPropertyService.getInstance().insert(reportProperty);
        }

        String[] labelsFilter = {"Thing Type", "group"};
        String[] propertyNamesFilter = {"thingType.id", "group.id"};

        String[] propertyOrdersFilter = {"1", "2"};
        String[] operatorFilter = {"==", "="};

        String[] value = {"1", "2"};
        Boolean[] isEditable = {true, false};

        for (int it = 0; it < Array.getLength(labelsFilter); it++) {
            ReportFilter reportFilter = createReportFilter(labelsFilter[it], propertyNamesFilter[it], propertyOrdersFilter[it],
                    operatorFilter[it], value[it], isEditable[it], reportDefinition);
            ReportFilterService.getInstance().insert(reportFilter);
        }

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

        String[] labels4 = {"RFID Tag #", "Group", "Zone", "Zone Dwell Time (zone)", "Last Detect Time", "Name", "Location"};
        String[] propertyNames4 = {"serial", "group.name", "zone", "dwellTime( zone )", "lastDetectTime", "name", "location"};
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

        for (int it = 0; it < Array.getLength(labelsFilter4); it++) {
            ReportFilter reportFilter = createReportFilter(labelsFilter4[it], propertyNamesFilter4[it], propertyOrdersFilter4[it],
                    operatorFilter4[it], value4[it], isEditable4[it], reportDefinition4);
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
			ReportFilter reportFilter = createReportFilter( labelsFilter2[it], propertyNamesFilter2[it], propertyOrdersFilter2[it],
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
			ReportFilter reportFilter = createReportFilter( labelsFilter3[it], propertyNamesFilter3[it], propertyOrdersFilter3[it],
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

        String[] labels7 = {"RFID Tag #", "Category", "Logical Reader", "Zone Dwell Time (ms)", "Last Detect Time", "Name", "Location"};
        String[] propertyNames7 = {"serial", "category", "logicalReader", "dwellTime( zone )", "lastDetectTime", "name", "location"};
        String[] propertyOrders7 = {"1", "2", "3", "4", "5", "6", "7"};
        Long[] propertyTypeIds7 = {0L, 0L, 0L, 0L, 0L, 0L, 0L};
        Boolean[] propertyShowPopUp7 = {true, false, false, false, false, false, false};

        for (int it = 0; it < Array.getLength(labels7); it++) {
            ReportProperty reportProperty = createReportProperty(labels7[it], propertyNames7[it], propertyOrders7[it],
                    propertyTypeIds7[it], propertyShowPopUp7[it], reportDefinition7);
            ReportPropertyService.getInstance().insert(reportProperty);
        }
        reportDefinition7.setPinLabel("1");

        String[] propertyNamesRules7 = {"dwellTime( zone )"};
        String[] operatorRules7 = {">"};
        String[] valueRules7 = {"5000"};
        String[] color7 = {"FF0000"};
        Long[] ids = {3L};

        for (int it = 0; it < Array.getLength(propertyNamesRules7); it++) {
            ReportRule reportRule = createReportRule(propertyNamesRules7[it], operatorRules7[it], valueRules7[it], color7[it], "",
                    ids[it], reportDefinition7);

            ReportRuleService.getInstance().insert(reportRule);
        }

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
        createRawMongoReport(createdByUser, group, gt, "Mongo Integrity Summary", "/mongo/raw_query_integrity_summary.js",
                setReportFilterData());
        createRawMongoReport(createdByUser, group, gt, "Mongo TimeZone Test", "/mongo/raw_query_timezone_test.js", null);
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
//        rd.setDefaultTypeIcon( "pin" );
//        rd.setPinLabels( false );
//        rd.setZoneLabels( false );
//        rd.setTrails( false );
//        rd.setClustering( false );
//        rd.setPlayback( true );
//        rd.setNupYup( false );
//        rd.setDefaultList( false );
//        rd.setGroupTypeFloor( gt );
//        rd.setDefaultColorIcon( "009F6B" );

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
            for (int it = 0; it < Array.getLength(labelsFilter); it++) {
                ReportFilter reportFilter = createReportFilter(labelsFilter[it], propertyNamesFilter[it],
                        propertyOrdersFilter[it], operatorFilter[it], value[it], isEditable[it], rd);
                ReportFilterService.getInstance().insert(reportFilter);
            }
        }
//        String[]  labels3            = {"Name Shipping Order", "name Asset", "Name Tag"};
//        String[]  propertyNames3     = {"name", "name", "name", "name"};
//        String[]  propertyOrders3    = {"1", "2", "3"};
//        Long[]    propertyTypeIds3   = {6L, 7L, 8L};
//        Boolean[] propertyShowPopUp3 = {true, true, true};
//        for (int it = 0; it < Array.getLength(labels3); it++)
//        {
//            ReportProperty reportProperty = createReportProperty(labels3[it],
//                                                                 propertyNames3[it],
//                                                                 propertyOrders3[it],
//                                                                 propertyTypeIds3[it],
//                                                                 propertyShowPopUp3[it],
//                                                                 rd);
//            ReportPropertyService.getInstance().insert(reportProperty);
//        }
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
            for (int it = 0; it < Array.getLength(labelsFilter); it++) {
                ReportFilter reportFilter = createReportFilter(
                        labelsFilter[it]
                        , propertyNamesFilter[it]
                        , propertyOrdersFilter[it]
                        , operatorFilter[it]
                        , value[it]
                        , isEditable[it]
                        , reportDefinitionDevice);
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

    public static String getFtpBridgeDeptConfiguration() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n" +
                "  \"thingTypeCode\": \"dept\",\n" +
                "  \"logRawMessages\": 0,\n" +
                "  \"ftp\": {\n" +
                "    \"connectionCode\": \"FTP\"\n" +
                "  },\n" +
                "  \"path\": \"/StoreReferenceData\",\n" +
                "  \"pattern\": \"*.DEPT\",\n" +
                "  \"patternCaseSensitive\": false,\n" +
                "  \"schedule\": \"0 0/10 * 1/1 * ? *\",\n" +
                "  \"configParser\": {\n" +
                "    \"parserType\": \"fixedLength\",\n" +
                "    \"separator\": null,\n" +
                "    \"fieldLengths\": \"4,30,14,7,7,1,2,2,2,7,1,2,1,1,1,1\",\n" +
                "    \"ignoreFooter\": true,\n" +
                "    \"ignoreHeader\": true,\n" +
                "    \"fieldNames\": [\n" +
                "      \"DepartmentNumber\",\n" +
                "      \"DepartmentName\",\n" +
                "      \"ShortDepartmentName\",\n" +
                "      \"MinimumDepartmentPrice\",\n" +
                "      \"MaximumDepartmentPrice\",\n" +
                "      \"Ind\",\n" +
                "      \"DepartmentSequence\",\n" +
                "      \"DepartmentGroupCode\",\n" +
                "      \"DepartmentGroupNumber\",\n" +
                "      \"RTMPriceThreshold\",\n" +
                "      \"StaffDiscountBand\",\n" +
                "      \"QtyHalo\",\n" +
                "      \"CanQtySale\",\n" +
                "      \"AllowGiftReceipts\",\n" +
                "      \"DefaultVATBand\",\n" +
                "      \"Action\"\n" +
                "    ],\n" +
                "    \"columnNumberAsSerial\": 0\n" +
                "  },\n" +
                "  \"processPolicy\": \"Incremental\",\n" +
                "  \"localBackupFolder\": \"/tmp\",\n" +
                "  \"ftpDestinatiomFolder\": \"/processed/dept\",\n" +
                "  \"mqtt\": {\n" +
                "    \"connectionCode\": \"MQTT\",\"active\": true\n" +
                "  },\n" +
                "  \"kafka\": {\n" +
                "    \"connectionCode\": \"KAFKA\",\"active\": false\n" +
                "  }\n" +
                "}");
        return sb.toString();
    }

    public static String getFtpBridgeUpcConfiguration() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n" +
                "  \"thingTypeCode\": \"upc\",\n" +
                "  \"logRawMessages\": 0,\n" +
                "  \"ftp\": {\n" +
                "    \"connectionCode\": \"FTP\"\n" +
                "  },\n" +
                "  \"path\": \"/StoreReferenceData\",\n" +
                "  \"pattern\": \"*.IPOSUPC\",\n" +
                "  \"patternCaseSensitive\": false,\n" +
                "  \"schedule\": \"0 0/10 * 1/1 * ? *\",\n" +
                "  \"configParser\": {\n" +
                "    \"parserType\": \"fixedLength\",\n" +
                "    \"separator\": null,\n" +
                "    \"fieldLengths\": \"4,5,3,2,2,2,2,5,3,8,8,1,3,1,8,7,1,3,4,1,1,3,3,3,3,3,4,4,4,4,1,8,1,1,5,1,5,1," +
                "5,1,5,1,1,1,2,1,1\",\n" +
                "    \"ignoreFooter\": true,\n" +
                "    \"ignoreHeader\": true,\n" +
                "    \"fieldNames\": [\n" +
                "      \"DepartmentNumber\",\n" +
                "      \"StrokeNumber\",\n" +
                "      \"ColourCode\",\n" +
                "      \"PrimarySizeIndex\",\n" +
                "      \"SecondarySizeIndex\",\n" +
                "      \"PrimarySizePosition\",\n" +
                "      \"SecondarySizePosition\",\n" +
                "      \"PrimarySizeDescription\",\n" +
                "      \"SecondarySizeDescription\",\n" +
                "      \"UPCNumber\",\n" +
                "      \"CreationDate\",\n" +
                "      \"WayStatus\",\n" +
                "      \"ItemNumber\",\n" +
                "      \"TaxCode\",\n" +
                "      \"StoreDate\",\n" +
                "      \"Price\",\n" +
                "      \"ReducedSuffix\",\n" +
                "      \"PreviousItemNumber\",\n" +
                "      \"GarmenMultiplies\",\n" +
                "      \"CustomMadeIndicator\",\n" +
                "      \"BoxedHangIndicator\",\n" +
                "      \"PackWidth\",\n" +
                "      \"PackLength\",\n" +
                "      \"PackHeight\",\n" +
                "      \"QuantityPerPack\",\n" +
                "      \"RangeCode\",\n" +
                "      \"DisplaySetQuantity\",\n" +
                "      \"TransportSetQuantity\",\n" +
                "      \"DisplayPackQuantity\",\n" +
                "      \"TransportPackQuantity\",\n" +
                "      \"PreReducedIndicator\",\n" +
                "      \"ReducedEffDate\",\n" +
                "      \"ReplenishmentIndicator\",\n" +
                "      \"UKVATBand1\",\n" +
                "      \"UKVATpersentage1\",\n" +
                "      \"UKVATBand2\",\n" +
                "      \"UKVATpersentage2\",\n" +
                "      \"UKVATBand3\",\n" +
                "      \"UKVATpersentage3\",\n" +
                "      \"UKVATBand4\",\n" +
                "      \"UKVATpersentage4\",\n" +
                "      \"TVTunerIndicator\",\n" +
                "      \"CouponIndicator\",\n" +
                "      \"ServiceItemIndicator\",\n" +
                "      \"AgeRestriction\",\n" +
                "      \"SVGCIndicator\",\n" +
                "      \"Action\"\n" +
                "    ],\n" +
                "    \"columnNumberAsSerial\": 9\n" +
                "  },\n" +
                "  \"processPolicy\": \"Incremental\",\n" +
                "  \"ftpDestinatiomFolder\": \"/processed/upc\",\n" +
                "  \"localBackupFolder\": \"/tmp\",\n" +
                "  \"mqtt\": {\n" +
                "    \"connectionCode\": \"MQTT\",\"active\": true\n" +
                "  },\n" +
                "  \"kafka\": {\n" +
                "    \"connectionCode\": \"KAFKA\",\"active\": false\n" +
                "  }\n" +
                "}");
        return sb.toString();
    }

    public static String getBridgeReassociationConfiguration() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n" +
                "  \"thingTypeCode\": \"item\",\n" +
                "  \"logRawMessages\": 0,\n" +
                "  \"ftp\": {\n" +
                "    \"connectionCode\": \"FTP\"\n" +
                "  },\n" +
                "  \"path\": \"/Reassociations\",\n" +
                "  \"pattern\": \"CRFID*.csv\",\n" +
                "  \"patternCaseSensitive\": false,\n" +
                "  \"schedule\": \"0 0/1 * 1/1 * ? *\",\n" +
                "  \"configParser\": {\n" +
                "    \"parserType\": \"CSV\",\n" +
                "    \"separator\": \",\",\n" +
                "    \"fieldLengths\": null,\n" +
                "    \"ignoreFooter\": false,\n" +
                "    \"ignoreHeader\": false,\n" +
                "    \"fieldNames\": [\n" +
                "      \"Store\",\n" +
                "      \"RFID\",\n" +
                "      \"Department\",\n" +
                "      \"UPC\",\n" +
                "      \"lastDetectTime\"\n" +
                "    ],\n" +
                "    \"columnNumberAsSerial\": 1,\n" +
                "    \"appendFileNameAsUDF\": true,\n" +
                "    \"appendedUDFName\": \"FileName\",\n" +
                "    \"regex\": \"\\\\d+\"\n" +
                "  },\n" +
                "  \"processPolicy\": \"Move\",\n" +
                "  \"localBackupFolder\": \"/tmp\",\n" +
                "  \"ftpDestinationFolder\": \"/processed/reassociation\",\n" +
                "  \"mqtt\": {\n" +
                "    \"connectionCode\": \"MQTT\",\"active\": true\n" +
                "  },\n" +
                "  \"kafka\": {\n" +
                "    \"connectionCode\": \"KAFKA\",\"active\": false\n" +
                "  }\n" +
                "}");
        return sb.toString();
    }

    public static String getBridgeTransactionConfiguration() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n" +
                "  \"thingTypeCode\": \"edw\",\n" +
                "  \"logRawMessages\": 0,\n" +
                "  \"ftp\": {\n" +
                "    \"connectionCode\": \"FTP\"\n" +
                "  },\n" +
                "  \"path\": \"/SalesReturns\",\n" +
                "  \"pattern\": \"WO*.csv\",\n" +
                "  \"patternCaseSensitive\": false,\n" +
                "  \"schedule\": \"0 0/1 * 1/1 * ? *\",\n" +
                "  \"configParser\": {\n" +
                "    \"parserType\": \"CSV\",\n" +
                "    \"separator\": \",\",\n" +
                "    \"fieldLengths\": null,\n" +
                "    \"ignoreFooter\": true,\n" +
                "    \"ignoreHeader\": true,\n" +
                "    \"fieldNames\": [\n" +
                "      \"site_no\",\n" +
                "      \"natural_date_dte\",\n" +
                "      \"trans_start_tsp\",\n" +
                "      \"trans_end_tsp\",\n" +
                "      \"upc\",\n" +
                "      \"till_num\",\n" +
                "      \"transaction_id\",\n" +
                "      \"line_id\",\n" +
                "      \"quantity\",\n" +
                "      \"regular_sales_unit_price_gbp\",\n" +
                "      \"actual_sales_unit_price_gbp\",\n" +
                "      \"actual_sales_amount_gbp\",\n" +
                "      \"actual_refund_amount_gbp\",\n" +
                "      \"tax_amount_gbp\",\n" +
                "      \"sale_refund_ind\",\n" +
                "      \"line_type_desc\",\n" +
                "      \"line_item_discount_gbp\"\n" +
                "    ],\n" +
                "    \"columnNumberAsSerial\": null\n" +
                "  },\n" +
                "  \"processPolicy\": \"Move\",\n" +
                "  \"localBackupFolder\": \"/usr/tmp/processedFile\",\n" +
                "  \"ftpDestinationFolder\": \"/processed/edw\",\n" +
                "  \"mqtt\": {\n" +
                "    \"connectionCode\": \"MQTT\",\"active\": true\n" +
                "  },\n" +
                "  \"kafka\": {\n" +
                "    \"connectionCode\": \"KAFKA\",\"active\": false\n" +
                "  }\n" +
                "}");
        return sb.toString();
    }

    public static String getFtpBridgeGmdConfiguration() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n" +
                "  \"thingTypeCode\": \"gmd\",\n" +
                "  \"logRawMessages\": 0,\n" +
                "  \"ftp\": {\n" +
                "    \"connectionCode\": \"FTP\"\n" +
                "  },\n" +
                "  \"path\": \"/Deliveries\",\n" +
                "  \"pattern\": \"CSSMGMDELIVER*.csv\",\n" +
                "  \"patternCaseSensitive\": false,\n" +
                "  \"schedule\": \"0 0/1 * 1/1 * ? *\",\n" +
                "  \"configParser\": {\n" +
                "    \"parserType\": \"CSV\",\n" +
                "    \"separator\": \",\",\n" +
                "    \"fieldLengths\": null,\n" +
                "    \"ignoreFooter\": false,\n" +
                "    \"ignoreHeader\": false,\n" +
                "    \"fieldNames\": [\n" +
                "      \"from_WH_code\",\n" +
                "      \"from_WH_name\",\n" +
                "      \"to_Store_code\",\n" +
                "      \"to_Store_name\",\n" +
                "      \"upc\",\n" +
                "      \"gm_stroke\",\n" +
                "      \"stroke\",\n" +
                "      \"stroke_desc\",\n" +
                "      \"colour_desc\",\n" +
                "      \"primary_size\",\n" +
                "      \"stock_quantity_delivered\",\n" +
                "      \"stock_delv_selling_values\",\n" +
                "      \"gm_indicator\",\n" +
                "      \"dispatch_time\",\n" +
                "      \"store_arrival_date\",\n" +
                "      \"vehicle_number\",\n" +
                "      \"dept_concat\"\n" +
                "    ],\n" +
                "    \"columnNumberAsSerial\": null,\n" +
                "    \"appendFileNameAsUDF\": true,\n" +
                "    \"appendedUDFName\": \"gm_date\",\n" +
                "    \"regex\": \"\\\\d+\"\n" +
                "  },\n" +
                "  \"processPolicy\": \"Move\",\n" +
                "  \"localBackupFolder\": \"/tmp\",\n" +
                "  \"ftpDestinationFolder\": \"/processed/gmd\",\n" +
                "  \"mqtt\": {\n" +
                "    \"connectionCode\": \"MQTT\",\"active\": true\n" +
                "  },\n" +
                "  \"kafka\": {\n" +
                "    \"connectionCode\": \"KAFKA\",\"active\": false\n" +
                "  }\n" +
                "}");
        return sb.toString();
    }

    public static String getFtpBridgeBywConfiguration() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n" +
                "  \"thingTypeCode\": \"item\",\n" +
                "  \"logRawMessages\": 0,\n" +
                "  \"ftp\": {\n" +
                "    \"connectionCode\": \"FTP\"\n" +
                "  },\n" +
                "  \"path\": \"/syw\",\n" +
                "  \"pattern\": \"BYW*.csv\",\n" +
                "  \"patternCaseSensitive\": false,\n" +
                "  \"schedule\": \"0 0/1 * 1/1 * ? *\",\n" +
                "  \"configParser\": {\n" +
                "    \"parserType\": \"CSV\",\n" +
                "    \"separator\": \",\",\n" +
                "    \"fieldLengths\": null,\n" +
                "    \"ignoreFooter\": true,\n" +
                "    \"ignoreHeader\": true,\n" +
                "    \"fieldNames\": [\n" +
                "      \"Store\",\n" +
                "      \"epc\",\n" +
                "      \"CycleCountDetect\"\n" +
                "    ],\n" +
                "    \"columnNumberAsSerial\": 1\n" +
                "  },\n" +
                "  \"processPolicy\": \"Move\",\n" +
                "  \"localBackupFolder\": \"/tmp\",\n" +
                "  \"ftpDestinationFolder\": \"/processed/syw\",\n" +
                "  \"mqtt\": {\n" +
                "    \"connectionCode\": \"MQTT\",\"active\": true\n" +
                "  },\n" +
                "  \"kafka\": {\n" +
                "    \"connectionCode\": \"KAFKA\",\"active\": false\n" +
                "  }\n" +
                "}");
        return sb.toString();
    }
}
