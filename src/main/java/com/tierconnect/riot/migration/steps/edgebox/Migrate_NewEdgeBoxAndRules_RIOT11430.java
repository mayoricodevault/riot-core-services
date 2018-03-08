package com.tierconnect.riot.migration.steps.edgebox;

import com.tierconnect.riot.appcore.dao.RoleResourceDAO;
import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.services.*;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.entities.EdgeboxRule;
import com.tierconnect.riot.iot.entities.Parameters;
import com.tierconnect.riot.iot.popdb.PopDBIOTUtils;
import com.tierconnect.riot.iot.popdb.PopDBRequiredIOT;
import com.tierconnect.riot.iot.services.EdgeboxRuleService;
import com.tierconnect.riot.iot.services.EdgeboxService;
import com.tierconnect.riot.iot.services.ParametersService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_NewEdgeBoxAndRules_RIOT11430 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_NewEdgeBoxAndRules_RIOT11430.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        //TODO DBHelper use static method to run sql file but use instance method to verify exists a table and columns.
        DBHelper dbHelperSQL = new DBHelper();
        Boolean table = dbHelperSQL.existTable("parameters");
        Boolean conditionType = dbHelperSQL.existColumn("edgeboxRule", "conditionType");
        Boolean parameterConditionType = dbHelperSQL.existColumn("edgeboxRule", "parameterConditionType");
        Boolean parameterType = dbHelperSQL.existColumn("edgebox", "parameterType");
        Boolean parentField_id = dbHelperSQL.existColumn("apc_field", "parentField_id");
        if (!table || !conditionType || !parameterConditionType || !parameterType || !parentField_id) {
            DBHelper.executeSQLFile(scriptPath);
        }
    }

    @Override
    public void migrateHibernate() throws Exception {
        addNewTableResourceParameters();
        populateParametersMigra();
        populateConditionTypeAndType();
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
     * Populate paremeters
     */
    private void populateParametersMigra() {
        String conditionType = "CONDITION_TYPE";
        String bridgeType = "BRIDGE_TYPE";
        Parameters parameters = new Parameters(conditionType, "ALWAYS_TRUE",
                "@SYSTEM_PARAMETERS_CONDITION_TYPE_ALWAYS_TRUE", null);
        ParametersService.getInstance().insert(parameters);
        parameters = new Parameters(conditionType, "CEP", "@SYSTEM_PARAMETERS_CONDITION_TYPE_CEP", null);

        ParametersService.getInstance().insert(parameters);
        parameters = new Parameters(bridgeType, "edge", "@SYSTEM_PARAMETERS_BRIDGE_TYPE_EDGE",
                "{\"thingTypeCode\":\"\",\"logRawMessages\":0,\"numberOfThreads\":10," +
                        "\"mqtt\":{\"connectionCode\":\"\"}," +
                        "\"zoneDwellFilter\":{\"active\":0,\"unlockDistance\":25,\"inZoneDistance\":10," +
                        "\"zoneDwellTime\":300," +
                        "\"lastDetectTimeActive\":1,\"lastDetectTimeWindow\":0},\"timeDistanceFilter\":{\"active\":0," +
                        "\"time\":0," +
                        "\"distance\":10},\"timeZoneFilter\":{\"active\":0,\"time\":10}," +
                        "\"zoneChangeFilter\":{\"active\":0}}");
        ParametersService.getInstance().insert(parameters);
        parameters = new Parameters(Constants.BRIDGE_TYPE, Constants.EDGEBOX_STARFLEX_TYPE, "@SYSTEM_PARAMETERS_BRIDGE_TYPE_STARFLEX",
                PopDBRequiredIOT.getStarFlexBridgeConfiguration(
                        Constants.TT_STARflex_CONFIG_CODE,
                        Constants.TT_STARflex_STATUS_CODE,
                        Constants.TT_STARflex_CODE, "", ""));
        ParametersService.getInstance().insert(parameters);
        parameters = new Parameters(bridgeType, "FTP", "@SYSTEM_PARAMETERS_BRIDGE_TYPE_FTP",
                "{\"thingTypeCode\":\"\",\"logRawMessages\":0,\"ftp\":{\"connectionCode\":\"\"}," +
                        "\"path\":\"/StoreReferenceData\",\"pattern\":\"*.COLOUR\",\"patternCaseSensitive\":false," +
                        "\"schedule\":\"0 0/10 * 1/1 * ? *\",\"configParser\":{\"parserType\":\"fixedlength\"," +
                        "\"separator\":null,\"fieldLengths\":\"3,16,1\",\"ignoreFooter\":true,\"ignoreHeader\":false," +
                        "\"fieldNames\":[\"Code\",\"Description\",\"Action\"],\"columnNumberAsSerial\":0}," +
                        "\"processPolicy\":\"Move\"," +
                        "\"localBackupFolder\":\"/tmp\",\"ftpDestinationFolder\":\"processed/colour\"," +
                        "\"mqtt\":{\"connectionCode\":\"\"}}");
        ParametersService.getInstance().insert(parameters);
        parameters = new Parameters(bridgeType, "GPS", "@SYSTEM_PARAMETERS_BRIDGE_TYPE_GPS",
                "{\"thingTypeCode\":\"\",\"logRawMessages\":0,\"mqtt\":{\"connectionCode\":\"\"}," +
                        "\"timeDistanceFilter\":{\"active\":0,\"time\":0,\"distance\":10}," +
                        "\"timeZoneFilter\":{\"active\":0,\"time\":10},\"geoforce\":{\"host\":\"app.geoforce.com\"," +
                        "\"path\":\"/feeds/asset_inventory.xml\",\"port\":443,\"user\":\"datafeed@mojix.com\"," +
                        "\"password\":\"AHmgooCk8l0jo95f7YSo\",\"period\":60},\"zoneChangeFilter\":{\"active\":0}}");
        ParametersService.getInstance().insert(parameters);
        parameters = new Parameters(bridgeType, "OPEN_RTLS", "@SYSTEM_PARAMETERS_BRIDGE_TYPE_OPEN_RTLS", null);
        ParametersService.getInstance().insert(parameters);
        parameters = new Parameters(bridgeType, "core", "@SYSTEM_PARAMETERS_BRIDGE_TYPE_CORE",
                "{\"threadDispatchMode\":1,\"numberOfThreads\":32,\"mqtt\":{\"connectionCode\":\"\"," +
                        "\"topics\":[\"/v1/data/ALEB/#\"," +
                        "\"/v1/data/APP2/#\",\"/v1/data/STAR/#\",\"/v1/data/STAR1/#\"]}," +
                        "\"mongo\":{\"connectionCode\":\"\"}," +
                        "\"sequenceNumberLogging\":{\"active\":0,\"TTL\":86400,\"GC_GRACE_SECONDS\":0}," +
                        "\"sourceRule\":{\"active\":0}," +
                        "\"CEPLogging\":{\"active\":0},\"pointInZoneRule\":{\"active\":1}," +
                        "\"doorEventRule\":{\"active\":1}," +
                        "\"shiftZoneRule\":{\"active\":0,\"shiftProperty\":\"shift\"," +
                        "\"zoneViolationStatusProperty\":\"zoneViolationStatus\"," +
                        "\"zoneViolationFlagProperty\":\"zoneViolationFlag\"}," +
                        "\"checkMultilevelReferences\":{\"active\":0}}");
        ParametersService.getInstance().insert(parameters);
        logger.info("Parameters has been migrated successfully.");
    }

    /**
     * Populate field conditionType in table edgeBoxRule
     */
    private static void populateConditionTypeAndType() {

        List<Edgebox> lstEdgeBox = EdgeboxService.getInstance().listPaginated(null, null);
        if ((lstEdgeBox != null) && (!lstEdgeBox.isEmpty())) {
            for (Edgebox edgebox : lstEdgeBox) {
                edgebox.setParameterType("BRIDGE_TYPE");
                edgebox.setType(PopDBIOTUtils.getCorrectBridgeTypeCode(edgebox));
                EdgeboxService.getInstance().update(edgebox);
            }
        }

        List<EdgeboxRule> lstEdgeBoxRule = EdgeboxRuleService.getInstance().listPaginated(null, null);
        if ((lstEdgeBoxRule != null) && (!lstEdgeBoxRule.isEmpty())) {
            for (EdgeboxRule edgeboxRule : lstEdgeBoxRule) {
                if ((edgeboxRule.getRule().contains("(1=1)"))) {
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

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
