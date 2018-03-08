package com.tierconnect.riot.migration.steps.role;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.dao.ResourceDAO;
import com.tierconnect.riot.appcore.dao.RoleResourceDAO;
import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.LicenseService;
import com.tierconnect.riot.appcore.services.ResourceService;
import com.tierconnect.riot.appcore.services.RoleResourceService;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;
import com.tierconnect.riot.appcore.services.*;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_DecoupledMenues_VIZIX2195 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_DecoupledMenues_VIZIX2195.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateFeature();
        migrateGroupConfigurationRecentFavorite();
        migrateGroupConfigurationAutoLoadTabMenus();
        migrateGroupConfigurationLookAndFeelRedesign();
    }

    private void migrateFeature() {
        Resource resource = new Resource();
        resource.setAcceptedAttributes("x");
        resource.setDescription("Services");
        resource.setFqname("Services");
        resource.setLabel("Services");
        resource.setName("Services");
        resource.setTreeLevel(1);
        resource.setType(6);
        Group group = GroupService.getInstance().get(1L);
        resource.setGroup(group);
        ResourceService.getInstance().insert(resource);

        Resource moduleHealthAndStatus = Resource.getModuleResource(group, "Health & Status","healthAndStatus",resource);
        moduleHealthAndStatus.setAcceptedAttributes("x");
        ResourceService.getInstance().insert(moduleHealthAndStatus);

        Resource moduleLogs = Resource.getModuleResource(group, "Logs", "logs",resource);
        moduleLogs.setAcceptedAttributes("x");
        ResourceService.getInstance().insert(moduleLogs);

        Resource resourceThing = new Resource();
        resourceThing.setAcceptedAttributes("x");
        resourceThing.setDescription("Things");
        resourceThing.setFqname("Things");
        resourceThing.setLabel("Things");
        resourceThing.setName("Things");
        resourceThing.setTreeLevel(1);
        resourceThing.setType(6);
        resourceThing.setGroup(group);
        ResourceService.getInstance().insert(resourceThing);

        Resource resourceTenant = new Resource();
        resourceTenant.setAcceptedAttributes("x");
        resourceTenant.setDescription("Tenants");
        resourceTenant.setFqname("Tenants");
        resourceTenant.setLabel("Tenants");
        resourceTenant.setName("Tenants");
        resourceTenant.setTreeLevel(1);
        resourceTenant.setType(6);
        resourceTenant.setGroup(group);
        ResourceService.getInstance().insert(resourceTenant);

        Resource resourceGateway = new Resource();
        resourceGateway.setAcceptedAttributes("x");
        resourceGateway.setDescription("Gateway");
        resourceGateway.setFqname("Gateway");
        resourceGateway.setLabel("Gateway");
        resourceGateway.setName("Gateway");
        resourceGateway.setTreeLevel(1);
        resourceGateway.setType(6);
        resourceGateway.setGroup(group);
        ResourceService.getInstance().insert(resourceGateway);

        Resource resourceModel = new Resource();
        resourceModel.setAcceptedAttributes("x");
        resourceModel.setDescription("Model");
        resourceModel.setFqname("Model");
        resourceModel.setLabel("Model");
        resourceModel.setName("Model");
        resourceModel.setTreeLevel(1);
        resourceModel.setType(6);
        resourceModel.setGroup(group);
        ResourceService.getInstance().insert(resourceModel);

        Resource moduleMapMaker = Resource.getModuleResource(group, "Map Maker", "mapMaker",resourceModel);
        moduleMapMaker.setAcceptedAttributes("x");
        ResourceService.getInstance().insert(moduleMapMaker);

        Resource resourceAnalytics = new Resource();
        resourceAnalytics.setAcceptedAttributes("x");
        resourceAnalytics.setDescription("Analytics");
        resourceAnalytics.setFqname("Analytics");
        resourceAnalytics.setLabel("Analytics");
        resourceAnalytics.setName("Analytics");
        resourceAnalytics.setTreeLevel(1);
        resourceAnalytics.setType(6);
        resourceAnalytics.setGroup(group);
        ResourceService.getInstance().insert(resourceAnalytics);

        Resource resourceBridges = new Resource();
        resourceBridges.setAcceptedAttributes("x");
        resourceBridges.setDescription("Bridges & Rules");
        resourceBridges.setFqname("Bridges & Rules");
        resourceBridges.setLabel("Bridges & Rules");
        resourceBridges.setName("Bridges & Rules");
        resourceBridges.setTreeLevel(2);
        resourceBridges.setType(6);
        resourceBridges.setGroup(group);
        resourceBridges.setParent(resourceGateway);
        ResourceService.getInstance().insert(resourceBridges);

        Resource resourceFlow = new Resource();
        resourceFlow.setAcceptedAttributes("x");
        resourceFlow.setDescription("Flows");
        resourceFlow.setFqname("Flows");
        resourceFlow.setLabel("Flows");
        resourceFlow.setName("Flows");
        resourceFlow.setTreeLevel(2);
        resourceFlow.setType(6);
        resourceFlow.setGroup(group);
        resourceFlow.setParent(resourceGateway);
        ResourceService.getInstance().insert(resourceFlow);

        //names are listed for thing Resource
        List<String> thingResources = Arrays.asList("thing","thing_editOwn",
                "dataType","thingType","thingTypeTemplate","thingTypeFieldTemplate");

        for (String name : thingResources){
            updateResource(name,resourceThing);
        }

        List<String> tenantResources = Arrays.asList("user","user_editRoamingGroup",
                "group","groupType","role","resource","license","field","shift");

        for (String name : tenantResources){
            updateResource(name,resourceTenant);
        }

        List<String> modelResources = Arrays.asList("mapMaker","zone","zoneType",
                "zoneGroup","zonePoint","localMap","logicalReader");
        for (String name : modelResources){
            updateResource(name,resourceModel);
        }

        QRoleResource qRoleResource = QRoleResource.roleResource;
        RoleResourceDAO roleResourceDAO = RoleResourceService.getInstance().getRoleResourceDAO();

        List<Role> rolesModel = roleResourceDAO.getQuery().where(qRoleResource.resource.name.in("zone","zoneType",
                "zoneGroup","zonePoint","localMap","logicalReader")).distinct().list(qRoleResource.role);
        insertRoleResources(rolesModel, resourceModel);

        List<String> servicesResources = Arrays.asList("connection","connectionType",
                "license_generator","importExport","Monitor");
        for (String name : servicesResources){
            updateResource(name,resource);
        }

        List<String> analyticsResources = Arrays.asList("reportDefinition",
                "mlExtraction","mlModel",
                "mlPrediction","mlBusinessModel","reportDefinition_inlineEditGroup");
        for (String name : analyticsResources){
            updateResource(name,resourceAnalytics);
        }

        List<String> bridgesResources = Arrays.asList("parameters","edgebox", "edgeboxRule");

        for (String name : bridgesResources){
            updateResource(name,resourceBridges);
        }

        List<Role> rolesServices = roleResourceDAO.getQuery().where(qRoleResource.resource.name.in("license_generator",
                "importExport","Monitor","connection")).distinct().list(qRoleResource.role);
        insertRoleResources(rolesServices, resource);

        List<Role> rolesGateway = roleResourceDAO.getQuery().where(qRoleResource.resource.name.in("edgeboxRule",
                "parameters","edgebox","Flows")).distinct().list(qRoleResource.role);
        insertRoleResources(rolesGateway, resourceGateway);

        List<Role> rolesThings = roleResourceDAO.getQuery().where(qRoleResource.resource.name.in("thing","thing_editOwn",
                "dataType","thingType","thingTypeTemplate","thingTypeFieldTemplate")).distinct().list(qRoleResource.role);
        insertRoleResources(rolesThings, resourceThing);

        List<Role> rolesTenants = roleResourceDAO.getQuery().where(qRoleResource.resource.name.in("user","user_editRoamingGroup",
                "group","groupType","role","resource","license","field","shift")).distinct().list(qRoleResource.role);
        insertRoleResources(rolesThings, resourceThing);

        try {
            Resource oldResourceMonitor = ResourceService.getInstance().getByName("Monitor");
            if (oldResourceMonitor != null){
                BooleanBuilder beAnd = new BooleanBuilder();
                beAnd = beAnd.and(QRoleResource.roleResource.resource.id.eq(oldResourceMonitor.getId()));
                List<RoleResource> rs = RoleResourceService.getInstance().listPaginated(beAnd, null,null);
                for(RoleResource roleResource: rs){
                    RoleResourceService.getInstance().delete(roleResource);
                }
                ResourceService.getInstance().delete(oldResourceMonitor);
            }
        } catch (NonUniqueResultException e) {
            logger.warn(e.getMessage() + " for Resource Monitor");
        }

        try {
            Resource oldResourceControl = ResourceService.getInstance().getByName("Control");
            BooleanBuilder be = new BooleanBuilder();
            be = be.and(QRoleResource.roleResource.resource.eq(oldResourceControl));
            if (oldResourceControl != null){

                updateRoleResources(be, resourceTenant, rolesTenants);
                BooleanBuilder beControl = new BooleanBuilder();
                beControl = beControl.and(QResource.resource.parent.eq(oldResourceControl));
                List<Resource> listControlResources = ResourceService.getInstance().listPaginated(beControl,null, null);
                for (Resource resourceControl: listControlResources){
                    resourceControl.setParent(resourceTenant);
                    ResourceService.getInstance().update(resourceControl);
                }
                ResourceService.getInstance().delete(oldResourceControl);
            }
        } catch (NonUniqueResultException e) {
            logger.warn(e.getMessage() + " for Resource Control");
        }

        try {
            Resource oldResourceReports = ResourceService.getInstance().getByName("Reports");
            BooleanBuilder be = new BooleanBuilder();
            be = be.and(QRoleResource.roleResource.resource.eq(oldResourceReports));
            updateRoleResources(be, resourceAnalytics, null);
            if (oldResourceReports != null){
                List<RoleResource> rs = RoleResourceService.getInstance().listPaginated(be, null,null);
                for(RoleResource roleResource: rs){
                    RoleResourceService.getInstance().delete(roleResource);
                }
                BooleanBuilder beReports = new BooleanBuilder();
                beReports = beReports.and(QResource.resource.parent.eq(oldResourceReports));
                List<Resource> listReportsResources = ResourceService.getInstance().listPaginated(beReports,null, null);
                for (Resource resourceReports: listReportsResources){
                    resourceReports.setParent(resourceAnalytics);
                    ResourceService.getInstance().update(resourceReports);
                }
                oldResourceReports.setParent(resourceAnalytics);
                ResourceService.getInstance().update(oldResourceReports);
            }
        } catch (NonUniqueResultException e) {
            logger.warn(e.getMessage() + " for Resource Control");
        }

        try {
            Resource oldResourceZones = ResourceService.getInstance().getByName("Zones");
            BooleanBuilder be = new BooleanBuilder();
            be = be.and(QRoleResource.roleResource.resource.eq(oldResourceZones));
            if (oldResourceZones != null){
                List<RoleResource> rs = RoleResourceService.getInstance().listPaginated(be, null,null);
                for(RoleResource roleResource: rs){
                    RoleResourceService.getInstance().delete(roleResource);
                }
                ResourceService.getInstance().delete(oldResourceZones);
            }
        } catch (NonUniqueResultException e) {
            logger.warn(e.getMessage() + " for Resource Control");
        }


        BooleanBuilder beResource = new BooleanBuilder();
        beResource = beResource.and(QResource.resource.description.eq("Analytics Resources"));
        beResource = beResource.and(QResource.resource.name.eq("Analytics"));
        List<Resource> oldResourceAnalytics = ResourceService.getInstance().listPaginated(beResource,null,null);
        if (!oldResourceAnalytics.isEmpty()) {
            Resource oldResourceAnalytic = oldResourceAnalytics.get(0);
            BooleanBuilder be = new BooleanBuilder();
            be = be.and(QRoleResource.roleResource.resource.eq(oldResourceAnalytic));
            if (oldResourceAnalytic != null) {
                updateRoleResources(be, resourceAnalytics, null);
                ResourceService.getInstance().delete(oldResourceAnalytic);
            }
        }

        removeModuleResources("Maker");
        Map<String, String> defaultNavBarMapping = new HashMap<>();
        defaultNavBarMapping.put("control", "tenants");
        defaultNavBarMapping.put("facilities", "model");
        defaultNavBarMapping.put("reports", "analytics");
        defaultNavBarMapping.put("analytics", "analytics");
        reassignDefaultNavBar(defaultNavBarMapping);
        migrateBridgeParameters();
        migrateFolder(group);
    }

    public void insertRoleResources(List<Role> roles, Resource resource){
        for (Role role : roles){
            try {
                BooleanBuilder be = new BooleanBuilder();
                be = be.and(QRoleResource.roleResource.resource.eq(ResourceService.getInstance().getByName("Monitor")));
                be = be.and(QRoleResource.roleResource.role.id.eq(role.getId()));
                if (RoleResourceService.getInstance().countList(be) > 0){
                    RoleResourceService.getInstance().insert(role, ResourceService.getInstance().getByName("healthAndStatus"), "x");
                    RoleResourceService.getInstance().insert(role, ResourceService.getInstance().getByName("logs"), "x");
                }
                be = new BooleanBuilder();
                be = be.and(QRoleResource.roleResource.resource.eq(ResourceService.getInstance().getByName("localMap")));
                be = be.or(QRoleResource.roleResource.resource.eq(ResourceService.getInstance().getByName("zone")));
                be = be.and(QRoleResource.roleResource.role.id.eq(role.getId()));
                if (RoleResourceService.getInstance().countList(be) > 0){
                    RoleResourceService.getInstance().insert(role, ResourceService.getInstance().getByName("mapMaker"), "x");
                }
            } catch (NonUniqueResultException e) {
                e.printStackTrace();
            }
            if (allowInsertRoleResource(role, resource)) {
                RoleResource roleResource = new RoleResource();
                roleResource.setRole(role);
                roleResource.setPermissions("x");
                roleResource.setResource(resource);
                RoleResourceService.getInstance().insert(roleResource);
            }
        }
    }

    public void updateRoleResources(BooleanBuilder be, Resource parentResource, List<Role> listRoles){
        List<RoleResource> listRoleResources = RoleResourceService.getInstance().listPaginated(be,null,null);
        if (listRoles == null) {
            for (RoleResource roleResource : listRoleResources) {
                roleResource.setResource(parentResource);
                RoleResourceService.getInstance().update(roleResource);
               logger.info("Resource updated for " + roleResource.getId() + " with parent " + parentResource.getName());
            }
        }else{
            for (RoleResource roleResource : listRoleResources) {
                for (Role role : listRoles) {
                    if (role.getId().equals(roleResource.getRole().getId()) ) {
                        roleResource.setResource(parentResource);
                        RoleResourceService.getInstance().update(roleResource);
                        logger.info("Resource updated for " + roleResource.getId() + " with parent " + parentResource.getName());
                    }
                }
            }
        }
        List<RoleResource> listRoleResourcesOld = RoleResourceService.getInstance().listPaginated(be,null,null);
        for (RoleResource roleResource: listRoleResourcesOld){
            RoleResourceService.getInstance().delete(roleResource);
        }
    }

    public void updateResource(String name,Resource parentResource){
        try {
            Resource temporalResource = ResourceService.getInstance().getByName(name);
            if (temporalResource != null){
                temporalResource.setParent(parentResource);
                ResourceService.getInstance().update(temporalResource);
            }
        } catch (NonUniqueResultException e) {
            logger.warn(e.getMessage()+" found for resource "+name);
        }
    }

    public void removeModuleResources(String moduleName){
        try {
            ConcurrentHashMap<String, Module> globalModules = LicenseDetail.getGlobalModules();
            Resource moduleResource = ResourceService.getInstance().getByName(moduleName);
            BooleanBuilder be = new BooleanBuilder();
            be = be.and(QRoleResource.roleResource.resource.eq(moduleResource));
            for (RoleResource roleResource : RoleResourceService.getRoleResourceDAO().selectAllBy(be)){
                RoleResourceService.getInstance().delete(roleResource);
                logger.info("Role resource deleted for " + roleResource.getId() + " with " + moduleName);
            }
            be = new BooleanBuilder();
            be = be.and(QResource.resource.parent.eq(moduleResource));
            for (Resource childResource : ResourceService.getResourceDAO().selectAllBy(be)){
                be = new BooleanBuilder();
                be = be.and(QRoleResource.roleResource.resource.eq(childResource));
                for (RoleResource roleResource : RoleResourceService.getRoleResourceDAO().selectAllBy(be)){
                    RoleResourceService.getInstance().delete(roleResource);
                    logger.info("Role resource deleted for " + roleResource.getId() + " with " + moduleName);
                }
                ResourceService.getInstance().delete(childResource);
                logger.info("Resource deleted for " + childResource.getId() + " with " + moduleName);
            }
            ResourceService.getInstance().delete(moduleResource);
            globalModules.remove(moduleName);
            LicenseDetail.setGlobalModules(globalModules);
            logger.info("Resource deleted for " + moduleResource.getId() + " with " + moduleName);
        } catch (Exception e) {
            logger.error("Cannot remove module resources for " + moduleName, e);
        }
    }

    public void migrateGroupConfigurationAutoLoadTabMenus() {
        GroupService groupService = GroupService.getInstance();
        Group rootGroup = groupService.getRootGroup();

        PopDBUtils.migrateFieldService("autoLoadTabMenus", "autoLoadTabMenus", "Auto-Load Tab Menus",
                rootGroup, "Look & Feel", "java.lang.Boolean", 2L, true, "true");
    }


    private void migrateGroupConfigurationRecentFavorite() {
        GroupService groupService = GroupService.getInstance();
        Group rootGroup = groupService.getRootGroup();
        PopDBUtils.migrateFieldService("max_favoriteItem", "max_favoriteItem", "Max Favorite Items",
                rootGroup, "Look & Feel", "java.lang.Integer", null, true, "50");
        PopDBUtils.migrateFieldService("max_recentItem", "max_recentItem", "Max Recent Items",
                rootGroup, "Look & Feel", "java.lang.Integer", null, true, "50");
    }

    private void reassignDefaultNavBar(Map<String, String> defaultNavBarMapping){
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QGroupField.groupField.field.id.eq(2L));
        for (GroupField groupField : GroupFieldService.getGroupFieldDAO().selectAllBy(be)){
            groupField.setValue(defaultNavBarMapping.get(groupField.getValue()));
            GroupFieldService.getGroupFieldDAO().update(groupField);
        }
    }

    private void migrateBridgeParameters(){
        Parameters edge = ParametersService.getInstance().getByCategoryAndCode(Constants.BRIDGE_TYPE, "edge");
        edge.setValue("{\"thingTypeCode\":\"\",\"logRawMessages\":0,\"numberOfThreads\":10,\"socketTimeout\":60000," +
                "\"dynamicTimeoutRate\":0,\"send500ErrorOnTimeout\":0,\"mqtt\":{\"connectionCode\":\"\"}," +
                "\"zoneDwellFilter\":{\"active\":0,\"unlockDistance\":25,\"inZoneDistance\":10," +
                "\"zoneDwellTime\":300,\"lastDetectTimeActive\":1,\"lastDetectTimeWindow\":0}," +
                "\"timeDistanceFilter\":{\"active\":0,\"time\":0,\"distance\":10}," +
                "\"timeZoneFilter\":{\"active\":0,\"time\":10},\"zoneChangeFilter\":{\"active\":0}," +
                "\"streaming\":{\"active\":false,\"bufferSize\":10},\"evaluateStats\":true}");
        ParametersService.getInstance().update(edge);
        Parameters core = ParametersService.getInstance().getByCategoryAndCode(Constants.BRIDGE_TYPE, "core");
        core.setValue("{\"threadDispatchMode\":1,\"numberOfThreads\":32,\"mqtt\":{\"connectionCode\":\"\"," +
                "\"topics\":[\"/v1/data/ALEB/#\",\"/v1/data/APP2/#\",\"/v1/data/STAR/#\",\"/v1/data/STAR1/#\"]}," +
                "\"mongo\":{\"connectionCode\":\"\"},\"sequenceNumberLogging\":{\"active\":0,\"TTL\":86400," +
                "\"GC_GRACE_SECONDS\":0},\"sourceRule\":{\"active\":0},\"CEPLogging\":{\"active\":0}," +
                "\"pointInZoneRule\":{\"active\":1},\"doorEventRule\":{\"active\":1,\"action\":\"Undefined\"," +
                "\"sendZoneInEvent\":1,\"sendZoneOutEvent\":1},\"shiftZoneRule\":{\"active\":0," +
                "\"shiftProperty\":\"shift\",\"zoneViolationStatusProperty\":\"zoneViolationStatus\"," +
                "\"zoneViolationFlagProperty\":\"zoneViolationFlag\"}," +
                "\"checkMultilevelReferences\":{\"active\":0},\"outOfOrderRule\":{\"active\":false}," +
                "\"timeOrderRule\":{\"active\":false,\"period\":0},\"reloadCacheTickle\":{\"active\":false}," +
                "\"interCacheEviction\":{\"active\":false},\"swarmFilter\":{\"active\":false," +
                "\"timeGroupTimer\":5000,\"swarmAlgorithm\":\"followLastDetect\"," +
                "\"thingTypes\":[{\"thingtypeCode\":\"default_rfid_thingtype\"," +
                "\"udfGroupStatus\":\"groupStatus\",\"udfGroup\":\"grouping\"," +
                "\"distanceFilter\":10000}]}," +
                "\"CEPEngineConfiguration\":{\"insertIntoDispatchPreserveOrder\":false," +
                "\"listenerDispatchPreserveOrder\":false,\"multipleInstanceMode\":false}," +
                "\"interCacheEvictionQueueSize\":20000,\"fixOlderSnapshotsQueueSize\":20000," +
                "\"evaluateStats\":true}");
        ParametersService.getInstance().update(core);
    }

    private void migrateGroupConfigurationLookAndFeelRedesign() {
        GroupService groupService = GroupService.getInstance();
        Group rootGroup = groupService.getRootGroup();
        PopDBUtils.migrateFieldService("language", "language", "Language",
                rootGroup, "Internationalization", "java.lang.String", null, true,
                "en");
        PopDBUtils.migrateFieldService("maxReportRecords", "maxReportRecords", "Max Report Records",
                rootGroup, "Reports", "java.lang.Integer", null, true, "15");
        PopDBUtils.migrateFieldService("pagePanel", "pagePanel", "Panel Pagination Size",
                rootGroup, "Reports", "java.lang.Integer", null, true, "15");
        PopDBUtils.migrateFieldService("alertPollFrequency", "alertPollFrequency", "Alert Poll Frequency (secs)",
                rootGroup, "Alerting & Notification", "java.lang.Integer", null, true, "15");
        PopDBUtils.migrateFieldService("stopAlerts", "stopAlerts", "Stop Alerts",
                rootGroup, "Alerting & Notification","java.lang.Boolean", null, true, "true");
        PopDBUtils.migrateFieldService("playbackMaxThings", "playbackMaxThings", "Playback Max Things",
                rootGroup, "Reports", "java.lang.Integer", 2L, true, "100");
        PopDBUtils.migrateFieldService("mapReportLimit", "mapReportLimit", "Map Report Limit",
                rootGroup, "Reports", "java.lang.Integer", null, true, "50000");
        PopDBUtils.migrateFieldService("tableSummaryReportLimit", "tableSummaryReportLimit", "Table Summary "
                + "Report Limit", rootGroup, "Reports", "java.lang.Integer", null, true, "1000000");
        PopDBUtils.migrateFieldService("Report Time Out Cache", "reportTimeOutCache", "Report Time Out Cache",
                rootGroup, "Reports", "java.lang.Integer", 1L, true, "15000");
        PopDBUtils.migrateFieldService("i18NDirectory", "i18NDirectory", "I18N Directory",
                rootGroup, "Internationalization","java.lang.String", 1L, true,"");
        PopDBUtils.migrateFieldService("executeRulesForLastDetectTime", "executeRulesForLastDetectTime",
                "Execute CEP rules when only lastDetectTime is sent", rootGroup, "Data Storage Configuration",
                "java.lang.Boolean", 3L, true, "true");
    }

    public void migrateFolder(Group group){
        Resource moduleFolder = Resource.getModuleResource(group, "Folder", "Folder");
        ResourceService.getInstance().insert(moduleFolder);
        Resource moduleReportFolder = Resource.getModuleResource(group, "Report Folder", "reportFolder",moduleFolder);
        moduleReportFolder.setAcceptedAttributes("uid");
        ResourceService.getInstance().insert(moduleReportFolder);
        try {
            Resource resource = ResourceService.getInstance().getByName("reportDefinition");
            List<Role> listRoles = RoleService.getInstance().listPaginated(null, null);
            for (Role role: listRoles){
                List<RoleResource> listRoleResource = RoleResourceService.getInstance().list(role);
                for (RoleResource roleResource: listRoleResource){
                   if( roleResource.getResource().equals(resource)) {
                       if (allowInsertRoleResource(role, moduleReportFolder)) {
                           RoleResource roleResourceReport = new RoleResource();
                           roleResourceReport.setRole(role);
                           roleResourceReport.setResource(moduleReportFolder);
                           roleResourceReport.setPermissions(moduleReportFolder.getAcceptedAttributes());
                           RoleResourceService.getInstance().insert(roleResourceReport);
                       }
                   }
                }
            }
        } catch (NonUniqueResultException e) {
            logger.error("Cannot find resource Report Definition.", e);
        }


        try {
            Resource tableScript = ResourceService.getInstance().getByName("reportDefinition_editTableScript");
            Resource reportDefinition = ResourceService.getInstance().getByName("reportDefinition");
            if (tableScript != null){
               tableScript.setParent(reportDefinition);
            }
        } catch (NonUniqueResultException e) {
            logger.error("Cannot find resource Edit Table Script.", e);
        }
    }

    private boolean allowInsertRoleResource(Role role, Resource resource){
        boolean allow = false;
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QRoleResource.roleResource.role.id.eq(role.getId()));
        be = be.and(QRoleResource.roleResource.resource.id.eq(resource.getId()));
        List<RoleResource> rs = RoleResourceService.getInstance().listPaginated(be, null,null);
        if (rs.isEmpty()){
            allow = true;
        }
        return allow;
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {

    }


}
