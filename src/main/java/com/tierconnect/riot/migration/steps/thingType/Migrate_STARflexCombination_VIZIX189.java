package com.tierconnect.riot.migration.steps.thingType;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.entities.Connection;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.QConnection;
import com.tierconnect.riot.appcore.services.ConnectionService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.popdb.PopDBRequiredIOT;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import org.apache.log4j.Logger;
import org.jose4j.json.internal.json_simple.JSONArray;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;

import java.util.*;

/**
 * Created by rchirinos on 05-02-17.
 */
public class Migrate_STARflexCombination_VIZIX189 implements MigrationStep{
    private static Logger logger = Logger.getLogger(Migrate_STARflexCombination_VIZIX189.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
    }

    @Override
    public void migrateHibernate() throws Exception {
        Map<String, Object> codes = migrateThingTypeTemplatesAndBridgeTypeSTARflex();
        migrateRootBridges();
        migrateThingTypesBridgesSTARflex(codes);
        migrateEdgeboxConfiguration();
    }

    /**
     *
     */
    public void migrateEdgeboxConfiguration(){
        JSONArray topics = new JSONArray();
        topics.add("/v1/data/#");
        updateEdgeboxConfiguration(Constants.EDGEBOX_CODE_MCB, topics);
    }

    /**
     *
     * @param edgeboxCode
     * @param topics
     */
    public void updateEdgeboxConfiguration(String edgeboxCode, JSONArray topics){
        Edgebox edgebox = EdgeboxService.getInstance().getByCode(edgeboxCode);
        if (edgebox != null){
            String configuration = edgebox.getConfiguration();
            try {
                JSONObject edgeboxConfiguration = (JSONObject) new JSONParser().parse(configuration);
                JSONObject mqtt = (JSONObject) edgeboxConfiguration.get("mqtt");
                mqtt.replace("topics", topics);
                edgeboxConfiguration.replace("mqtt", mqtt);
                edgebox.setConfiguration(edgeboxConfiguration.toString());
                EdgeboxService.getInstance().update(edgebox);
            } catch (ParseException e) {
                logger.error ("migrateConfigurationEdgebox", e);
            }
        }
    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    public void migrateRootBridges() throws NonUniqueResultException {
        Group starFlexGroup = GroupService.getInstance().getByCode(Constants.STARFLEX_MAIN_GROUP_CODE);
        if( starFlexGroup != null ) {
            // Delete old bridges
            BooleanBuilder bb = new BooleanBuilder();
            bb.and(QEdgebox.edgebox.group.eq(starFlexGroup)).and(QEdgebox.edgebox.type.eq("StarFLEX"));
            List<Edgebox> lstEdgebox = EdgeboxService.getInstance().listPaginated(bb, null,null);
            if ((lstEdgebox  != null) && (!lstEdgebox.isEmpty())) {
                for (int i = 0 ; i< lstEdgebox.size() ; i++) {
                    EdgeboxService.getInstance().delete(lstEdgebox.get(i));
                }
            }

            //Insert new Bridge
            EdgeboxService.getInstance().insertEdgebox(
                    starFlexGroup,
                    Constants.TT_STARflex_NAME+" Bridge",
                    Constants.STARFLEX_MAIN_BRIDGE_CODE,
                    Constants.EDGEBOX_STARFLEX_TYPE,
                    getConfigurationStarflexBridgeCombined(
                            Constants.TT_STARflex_CONFIG_CODE,
                            Constants.TT_STARflex_STATUS_CODE,
                            Constants.TT_STARflex_CODE, "MONGO", "MQTT"),
                    9092L);
        }
    }

    /**
     * Migrate all ThingType Templates and Config Bridge Type
     * @return
     * @throws NonUniqueResultException
     */
    public Map<String, Object> migrateThingTypeTemplatesAndBridgeTypeSTARflex() throws NonUniqueResultException {
        Map<String, Object> result = new HashMap<>();
        //STARflex Config
        ThingTypeTemplate str400 = ThingTypeTemplateService.getInstance().getByName("STARflex");
        str400.setName(Constants.TT_STARflex_CONFIG_NAME);
        str400.setDescription(Constants.TT_STARflex_CONFIG_NAME);
        str400.setPathIcon("sprite template icon-starflex");
        Set<ThingTypeFieldTemplate> thingTypeFieldTemplates = thingTypeFieldTemplateStarFlexConfig(str400);
        str400.setThingTypeFieldTemplate(thingTypeFieldTemplates);
        str400 =ThingTypeTemplateService.getInstance().update(str400);
        result.put(Constants.TT_STARflex_CONFIG_CODE, str400);
        //STARflex
        ThingTypeTemplate flexTag = ThingTypeTemplateService.getInstance().getByName("FlexTag");
        flexTag.setName(Constants.TT_STARflex_NAME);
        flexTag.setDescription(Constants.TT_STARflex_NAME);
        flexTag.setPathIcon("sprite template icon-flextag");
        flexTag =ThingTypeTemplateService.getInstance().update(flexTag);
        result.put(Constants.TT_STARflex_CODE, flexTag);
        //STARflex Status
        ThingTypeTemplate starflexStatus = insertSTARflexStatusTemplate(GroupService.getInstance().getRootGroup());
        result.put(Constants.TT_STARflex_STATUS_CODE, starflexStatus);
        //Parameters, Bridges template
        Parameters parameter = ParametersService.getInstance().getByCategoryAndCode(Constants.BRIDGE_TYPE, "StarFLEX");
        parameter.setCode(Constants.TT_STARflex_NAME);
        parameter.setValue(getConfigurationStarflexBridgeCombined(
                Constants.TT_STARflex_CONFIG_CODE,
                Constants.TT_STARflex_STATUS_CODE,
                Constants.TT_STARflex_CODE, "MONGO", "MQTT"));
        parameter = ParametersService.getInstance().update(parameter);
        //Change Bridge Type in EdgeBox
        updateEdgeBoxWithSTARflexBridgeType();

        result.put(Constants.BRIDGE_TYPE, parameter);
        return result;
    }

    /**
     * Insert STARflex Status Template
     * @param group group
     */
    public ThingTypeTemplate insertSTARflexStatusTemplate(Group group){
        ThingTypeTemplate thingTypeTemplateHead = PopDBRequiredIOT.insertThingTypeTemplateHead(
                Constants.TT_STARflex_STATUS_NAME, Constants.TT_STARflex_STATUS_NAME, group,
                "sprite template icon-devicesstatus", false);
        PopDBRequiredIOT.insertUdfField("cpuUtilPercent", "cpuUtilPercent", "", "", DataTypeService.getInstance().get(4L), "DATA_TYPE", false, thingTypeTemplateHead, "");
        PopDBRequiredIOT.insertUdfField("freeMem", "freeMem", "", "", DataTypeService.getInstance().get(4L), "DATA_TYPE", true, thingTypeTemplateHead, "");
        PopDBRequiredIOT.insertUdfField("loadAvg_0_", "loadAvg_0_", "", "", DataTypeService.getInstance().get(4L), "DATA_TYPE", false, thingTypeTemplateHead, "");
        PopDBRequiredIOT.insertUdfField("loadAvg_1_", "loadAvg_1_", "", "", DataTypeService.getInstance().get(4L), "DATA_TYPE", false, thingTypeTemplateHead, "");
        PopDBRequiredIOT.insertUdfField("loadAvg_2_", "loadAvg_2_", "", "", DataTypeService.getInstance().get(4L), "DATA_TYPE", false, thingTypeTemplateHead, "");
        PopDBRequiredIOT.insertUdfField("metaData_myID", "metaData_myID", "", "", DataTypeService.getInstance().get(1L), "DATA_TYPE", false, thingTypeTemplateHead, "");
        PopDBRequiredIOT.insertUdfField("temperature_c_", "temperature_c_", "", "", DataTypeService.getInstance().get(4L), "DATA_TYPE", true, thingTypeTemplateHead, "");
        PopDBRequiredIOT.insertUdfField("temperature_f_", "temperature_f_", "", "", DataTypeService.getInstance().get(4L), "DATA_TYPE", true, thingTypeTemplateHead, "");
        PopDBRequiredIOT.insertUdfField("totMem", "totMem", "", "", DataTypeService.getInstance().get(4L), "DATA_TYPE", false, thingTypeTemplateHead, "");
        PopDBRequiredIOT.insertUdfField("ts_ms", "ts_ms", "", "",  DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TIMESTAMP.value), "DATA_TYPE", false, thingTypeTemplateHead, "");
        PopDBRequiredIOT.insertUdfField("tz", "tz", "", "", DataTypeService.getInstance().get(1L), "DATA_TYPE", false, thingTypeTemplateHead, "");
        PopDBRequiredIOT.insertUdfField("upTime_sec", "upTime_sec", "", "", DataTypeService.getInstance().get(4L), "DATA_TYPE", false, thingTypeTemplateHead, "");
        PopDBRequiredIOT.insertUdfField("wallclock", "wallclock", "", "", DataTypeService.getInstance().get(1L), "DATA_TYPE", false, thingTypeTemplateHead, "");
        return thingTypeTemplateHead;
    }

    /**
     * This logic adds a new Thing Type : STARflex Status for each STARflex Config found
     * @param codes
     */
    public void migrateThingTypesBridgesSTARflex(Map<String,Object> codes) throws NonUniqueResultException {
        Long templateSTR400_id = ((ThingTypeTemplate) codes.get(Constants.TT_STARflex_CONFIG_CODE)).getId();
        ThingTypeTemplate thingTypetemplateStatus = (ThingTypeTemplate) codes.get(Constants.TT_STARflex_STATUS_CODE);
        // Add STARflex Status for each STARflex Config found
        List<ThingType> lstThingType = ThingTypeService.getInstance().getThingTypesByThingTypeTemplate(templateSTR400_id);
        for(ThingType starflexConfigThingType : lstThingType) {
            String name = null;
            String code = null;
            String flexTagCode = null;
            if (starflexConfigThingType.getCode().equals(Constants.TT_STARflex_CONFIG_CODE)) {
                name = Constants.TT_STARflex_STATUS_NAME;
                code = Constants.TT_STARflex_STATUS_CODE;
                flexTagCode = Constants.TT_STARflex_CODE;
            } else {
                name = Constants.THING_STARflex_STATUS_ACRONYM + starflexConfigThingType.getGroup().getCode();
                code = ThingTypeService.getInstance().getThingTypeCodeByName(name);
                ThingTypeTemplate thingTypetemplateFlexTag = (ThingTypeTemplate) codes.get(Constants.TT_STARflex_CODE);
                BooleanBuilder bb = new BooleanBuilder();
                bb.and(QThingType.thingType.thingTypeTemplate.eq(thingTypetemplateFlexTag).and(
                        QThingType.thingType.group.eq(starflexConfigThingType.getGroup())));
                List<ThingType> lstFlexTag = ThingTypeService.getInstance().listPaginated(bb, null, null);
                flexTagCode = lstFlexTag.get(0).getCode();
            }
            ThingType starflexStatusThingType = ThingTypeService.getInstance().insertThingTypeAndFieldsWithTemplate(
                    starflexConfigThingType.getGroup(),name, code , thingTypetemplateStatus, true);
            migrateEdgeBoxeSTARflex(
                    starflexConfigThingType.getGroup(), starflexConfigThingType.getCode(), starflexStatusThingType.getCode(),
                    flexTagCode);
        }
    }

    public void migrateEdgeBoxeSTARflex(
            Group group, String starflexConfigCode, String starflexStatusCode, String starflexCode) {
        BooleanBuilder bb = new BooleanBuilder();
        bb.and(QEdgebox.edgebox.group.id.eq(group.getId()).and(QEdgebox.edgebox.parameterType.eq(Constants.BRIDGE_TYPE)
                .and(QEdgebox.edgebox.type.eq("StarFLEX"))));
        List<Edgebox> lstEdgeBox = EdgeboxService.getInstance().listPaginated(bb, null, null);
        Set<Long> edgeboxesDevices = new HashSet<>();
        if( (lstEdgeBox != null) && (!lstEdgeBox.isEmpty())) {
            for (Edgebox edgebox : lstEdgeBox) {
                String thingTypeTemplateName = getThingTypeTemplateName(edgebox.getConfiguration());//VIZIX-3989
                if (thingTypeTemplateName != null){//VIZIX-3989
                    if(Constants.TT_STARflex_NAME.equals(thingTypeTemplateName) && lstEdgeBox.size() > 1) { //VIZIX-3989
                        edgeboxesDevices.add(edgebox.getId());//to delete
                    } else if (Constants.TT_STARflex_CONFIG_NAME.equals(thingTypeTemplateName)) { //VIZIX-3989
                        Connection mongoConnection = getConnection("MONGO", group);
                        Connection mqttConnection = getConnection("MQTT", group);
                        edgebox.setConfiguration(
                                getConfigurationStarflexBridgeCombined(
                                        starflexConfigCode,starflexStatusCode,starflexCode,
                                        mongoConnection.getCode(),
                                        mqttConnection.getCode() ));
                    }
                }
                edgebox.setType(Constants.TT_STARflex_NAME);
                EdgeboxService.getInstance().update(edgebox);
            }
        }
        //Delete STARD_ Starflex
        for(Long id : edgeboxesDevices) {
            EdgeboxService.getInstance().delete(EdgeboxService.getInstance().get(id));
        }

        JSONParser parser = new JSONParser();
        for (Edgebox core : EdgeboxService.getInstance().getByType("core")) {
            try {
                JSONObject config = (JSONObject) parser.parse(core.getConfiguration());
                JSONArray topics = (JSONArray) ((JSONObject)config.get("mqtt")).get("topics");
                topics.remove("/v1/data/START_" + core.getGroup().getCode() + "/#");
                core.setConfiguration(config.toJSONString());
                EdgeboxService.getInstance().update(core);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        for (Edgebox star : EdgeboxService.getInstance().getByType("StarFLEX")) {
            String code = star.getCode();
            if (code.contains("START_")) {
                if (EdgeboxService.getInstance().getByCode(code.replace("START_", "STARD_")) != null) {
                    EdgeboxService.getInstance().delete(star);
                } else {
                    star.setCode(code.replace("START_", "STARD_"));
                    EdgeboxService.getInstance().update(star);
                }
            }
        }
    }



    /**
     * Method populates configuration of starflex bridge
     * @param starflexConfigCode
     * @param starflexStatusCode
     * @param starflexCode
     * @param mqttCode
     * @param mongoCode
     * @return
     */
    public String getConfigurationStarflexBridgeCombined(
            String starflexConfigCode,
            String starflexStatusCode,
            String starflexCode,
            String mongoCode,
            String mqttCode) {
        JSONObject jo = new JSONObject();
        JSONObject mqtt = new JSONObject();
        mqtt.put("connectionCode", mqttCode);
        JSONObject mongo = new JSONObject();
        mongo.put("connectionCode", mongoCode);
        JSONObject rateFilter = new JSONObject();
        rateFilter.put("active", 1);
        rateFilter.put("timeLimit", 20);
        JSONObject zoneDwellFilter = new JSONObject();
        zoneDwellFilter.put("active", 1);
        zoneDwellFilter.put("inZoneDistance", 0);
        zoneDwellFilter.put("lastDetectTimeActive", 1);
        zoneDwellFilter.put("lastDetectTimeWindow", 20);
        zoneDwellFilter.put("unlockDistance", 0);
        zoneDwellFilter.put("zoneDwellTime", 10);

        jo.put("mqtt", mqtt);
        jo.put("mongo", mongo);
        jo.put("rateFilter", rateFilter);
        jo.put("zoneDwellFilter", zoneDwellFilter);
        jo.put("lastDetectFilterTypes", "");
        jo.put("numberOfThreads", 1);
        jo.put("thingTypeCode", starflexCode);
        jo.put("thingTypeCodeStatus", starflexStatusCode);
        jo.put("thingTypeCodeConfig", starflexConfigCode);

        return jo.toJSONString();
    }


    /**
     * Get Connection
     * @param connectionType
     * @param group
     * @return
     */
    public Connection getConnection(String connectionType, Group group) {
        Connection result = null;
        BooleanBuilder connBuild = new BooleanBuilder();
        connBuild.and(QConnection.connection.connectionType.code.eq(connectionType)).and(QConnection.connection.group.eq(group));
        List<Connection> lstConn = ConnectionService.getInstance().listPaginated(connBuild, null, null);
        if ( (lstConn != null) && (!lstConn.isEmpty())) {
            result = lstConn.get(0);
        } else {
            if( (connectionType != null) && (connectionType.equals("MONGO"))) {
                String mongoConnection = Constants.STARFLEX_MONGO_CONN_ACRONYM + group.getCode();
                result = this.createConnections(group,Constants.STARFLEX_MONGO_CONN_NAME, mongoConnection);
            } else if( (connectionType != null) && (connectionType.equals("MQTT"))) {
                String mqttConnection = Constants.STARFLEX_MQTT_CONN_ACRONYM + group.getCode();
                result =this.createConnections(group,Constants.STARFLEX_MQTT_CONN_NAME, mqttConnection );
            }
        }
        return result;
    }

    /**
     * Create connections
     * @param group
     * @param typeConnection
     * @param nameConnection
     */
    public Connection createConnections(Group group, String typeConnection, String nameConnection){
        Connection conn = ConnectionService.getInstance().getByCodeAndGroup(nameConnection, group);
        if(conn == null) {
            Connection originConn = ConnectionService.getInstance().getByCodeAndGroup(typeConnection, GroupService.getInstance().getRootGroup());
            if (originConn != null) {
                if (ConnectionService.getInstance().existsCodeConnection(nameConnection)){
                    nameConnection = nameConnection + "_" + group.getId();
                }
                conn = createConnection(originConn, group, nameConnection);
            } else {
                BooleanBuilder b = new BooleanBuilder();
                b.and(QConnection.connection.connectionType.code.eq(typeConnection))
                        .and(QConnection.connection.group.eq(GroupService.getInstance().getRootGroup()));
                List<Connection> lstConn = ConnectionService.getInstance().listPaginated(b, null, null);
                if ( (lstConn != null) && (!lstConn.isEmpty()) ){
                    originConn = lstConn.get(0);
                    conn = createConnection(originConn, group, nameConnection);
                }
            }
        }
        return conn;
    }

    /**
     * Create Connection based on specific connection, group and name
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
     * Update Edge Box Type from StarFLEX to STARflex
     */
    public void updateEdgeBoxWithSTARflexBridgeType() {
        BooleanBuilder bb = new BooleanBuilder();
        bb.and(QEdgebox.edgebox.type.eq("StarFLEX"));
        List<Edgebox> lstEdgeBox = EdgeboxService.getInstance().listPaginated(bb, null, null);
        if( (lstEdgeBox != null) && (!lstEdgeBox.isEmpty())) {
            for (Edgebox edgebox : lstEdgeBox) {
                edgebox.setType(Constants.TT_STARflex_NAME);
                EdgeboxService.getInstance().update(edgebox);
            }
        }
    }

    /**
     *
     * @param str400
     * @return Set<ThingTypeFieldTemplate>
     * @throws NonUniqueResultException
     */
    public Set<ThingTypeFieldTemplate> thingTypeFieldTemplateStarFlexConfig(ThingTypeTemplate str400) throws NonUniqueResultException {
        ArrayList<String> thingTypeFieldTempRemove = new ArrayList<String>();
        thingTypeFieldTempRemove.add("cpuUtilPercent");
        thingTypeFieldTempRemove.add("freeMem");
        thingTypeFieldTempRemove.add("loadAvg_0_");
        thingTypeFieldTempRemove.add("loadAvg_1_");
        thingTypeFieldTempRemove.add("loadAvg_2_");
        thingTypeFieldTempRemove.add("metaData_myID");
        thingTypeFieldTempRemove.add("temperature_c_");
        thingTypeFieldTempRemove.add("temperature_f_");
        thingTypeFieldTempRemove.add("totMem");
        thingTypeFieldTempRemove.add("ts_ms");
        thingTypeFieldTempRemove.add("tz");
        thingTypeFieldTempRemove.add("upTime_sec");
        thingTypeFieldTempRemove.add("wallclock");
        ThingTypeTemplate thingTypeTemplate = ThingTypeTemplateService.getInstance().
                getByName(Constants.TT_STARflex_CONFIG_NAME);
        //Update dependencies in ThingTypeField
        for (String name : thingTypeFieldTempRemove) {
            ThingTypeFieldTemplate field = ThingTypeFieldTemplateService.getInstance().
                    getThingTypeFieldTemplateByThingTypeTemplate(thingTypeTemplate.getId(), name);
            List<ThingTypeField> thingTypeFieldList = ThingTypeFieldService.getInstance().getThingTypeField(field.getId());
            for (ThingTypeField thingTypeField : thingTypeFieldList){
                thingTypeField.setThingTypeFieldTemplateId(null);
                ThingTypeFieldService.getInstance().update(thingTypeField);
            }

        }
        //Delete from thing type field template
        Set<ThingTypeFieldTemplate> thingTypeFieldTemplates  = str400.getThingTypeFieldTemplate();
        if (thingTypeFieldTemplates != null ){
            for (Iterator<ThingTypeFieldTemplate> i = thingTypeFieldTemplates.iterator(); i.hasNext(); ) {
                ThingTypeFieldTemplate thingTypeFieldTemplate = i.next();
                for (String name : thingTypeFieldTempRemove) {
                    if (name.equals(thingTypeFieldTemplate.getName())){
                        i.remove();
                        ThingTypeFieldTemplateService.getInstance().delete(thingTypeFieldTemplate);
                        thingTypeFieldTemplates.remove(thingTypeFieldTemplate);
                    }
                }
            }
        }
        return thingTypeFieldTemplates;
    }
    /**
     *
     * @param configuration
     * @return
     */
    private String getThingTypeCode(String configuration) {//VIZIX-3989
        JSONObject jsonObject = null;
        try {
            jsonObject = (JSONObject) new JSONParser().parse(configuration);
            return (String) jsonObject.get("thingTypeCode");
        } catch (ParseException e) {
            logger.error("getThingTypeCode: ", e);
        }
        return null;
    }

    /**
     *
     * @param configuration
     * @return
     */

    private String getThingTypeTemplateName(String configuration){//VIZIX-3989
        String thingTypeCode = getThingTypeCode(configuration);
        if (thingTypeCode != null){
            try {
                ThingType thingType = ThingTypeService.getInstance().getByCode(thingTypeCode);
                return thingType.getThingTypeTemplate().getName();
            } catch (NonUniqueResultException e) {
                logger.error("getThingTypeTemplateName: ", e);
            }
        }
        return null;
    }
}
