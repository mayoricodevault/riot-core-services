package com.tierconnect.riot.migration.older;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.popdb.PopDBRequired;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;
import com.tierconnect.riot.appcore.services.ConnectionTypeService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.entities.EdgeboxRule;
import com.tierconnect.riot.iot.entities.ReportEntryOptionProperty;
import com.tierconnect.riot.iot.services.EdgeboxRuleService;
import com.tierconnect.riot.iot.services.EdgeboxService;
import com.tierconnect.riot.iot.services.ReportEntryOptionPropertyService;
import com.tierconnect.riot.migration.DBHelper;
import org.apache.log4j.Logger;
import org.jose4j.json.internal.json_simple.JSONArray;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;

import java.util.Arrays;
import java.util.List;

/**
 * Created by rchirinos on 10/25/2016
 */
@Deprecated
public class V_040401_040500_RC1 implements MigrationStepOld {
    static Logger logger = Logger.getLogger(V_040401_040500_RC1.class);

    @Override
    public List<Integer> getFromVersions() {
        return Arrays.asList(40401, 4050000);
    }

    @Override
    public int getToVersion() {
        return 4050001;
    }

    @Override
    public void migrateSQLBefore() throws Exception {
        DBHelper dbHelper = new DBHelper();
        String databaseType = DBHelper.getDataBaseType();
        if(!dbHelper.existColumn("reportentryoptionproperty", "defaultMobileValue")) {
            dbHelper.executeSQLFile("sql/" + databaseType + "/V040401_to_040500_RC1.sql");
        }
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateGroupConfigurationForBulkProcess();
        migrateReportEntryOptionProperty();
        migrateEdgeBoxForNecessaryParametersOnBridges();
        migrateMqttPushSubscriberWrongExample();
        migrateNewConnectiontype();
    }

    @Override
    public void migrateSQLAfter() throws Exception {
    }

    /**
     * Migrate group configuration for bulk process
     */
    public static void migrateGroupConfigurationForBulkProcess() {
        GroupService groupService = GroupService.getInstance();
        Group rootGroup = groupService.getRootGroup();
        PopDBUtils.migrateFieldService("notification_bulkProcess", "notification_bulkProcess", "Notification Bulk Process " +
                "(secs)", rootGroup, "Reports", "java.lang.Integer", null, true, "15");
        PopDBUtils.migrateFieldService("reloadAllThingsThreshold_bulkProcess", "reloadAllThingsThreshold_bulkProcess", "Things Cache " +
                "Reload Threshold", rootGroup, "Reports", "java.lang.Long", 3L, true, "1000");
        PopDBUtils.migrateFieldService("sendThingFieldTickle_bulkProcess", "sendThingFieldTickle_bulkProcess", "Run Rules After " +
                "Bulk Process", rootGroup, "Reports", "java.lang.Boolean", 3L, true, "false");
        PopDBUtils.migrateFieldService("fmcSapEnableSapSync_bulkProcess", "fmcSapEnableSapSync_bulkProcess", "Enable SAP Sync on " +
                "Bulk Process", rootGroup, "Reports", "java.lang.Boolean", 2L, false, "false");
    }
    /**
     * Update data in reportEntryOptionProperty
     */
    public void migrateReportEntryOptionProperty (){
        List<ReportEntryOptionProperty> lstReportEntryOptionProperty = ReportEntryOptionPropertyService.getInstance().getAllReportEntryOptionProperties();
        for (ReportEntryOptionProperty reportEntryOption : lstReportEntryOptionProperty){
            reportEntryOption.setDefaultMobileValue(reportEntryOption.getSortBy());
            reportEntryOption.setSortBy(null);
            ReportEntryOptionPropertyService.getInstance().update(reportEntryOption);
        }
    }
    /**
     * Update data in Edgebox field configuration with necessary on Bridges
     */
    public void migrateEdgeBoxForNecessaryParametersOnBridges() {
        try {
            //CORE BRIDGE data
            List<Edgebox> coreList = EdgeboxService.getInstance().getByType("core");
            for(Edgebox ebc: coreList){
                JSONObject configuration = (JSONObject)new JSONParser().parse(ebc.getConfiguration());
                //RIOT-12749: Bridges necessary parameters
                JSONObject cepEngineConfiguration = new JSONObject();
                cepEngineConfiguration.put("insertIntoDispatchPreserveOrder", false);
                cepEngineConfiguration.put("listenerDispatchPreserveOrder", false);
                cepEngineConfiguration.put("multipleInstanceMode", false);

                configuration.put("CEPEngineConfiguration", cepEngineConfiguration);

                configuration.put("interCacheEvictionQueueSize", 20000);

                configuration.put("fixOlderSnapshotsQueueSize", 20000);

                //RIOT-12855: Adding evaluateStats boolean parameter
                configuration.put("evaluateStats", true);

                ebc.setConfiguration(configuration.toJSONString());
                EdgeboxService.getInstance().update(ebc);
            }
            //ALE bridge
            List<Edgebox> aleList = EdgeboxService.getInstance().getByType("edge");
            for(Edgebox eba: aleList){
                JSONObject configuration = (JSONObject)new JSONParser().parse(eba.getConfiguration());
                //RIOT-12855: Adding evaluateStats boolean parameter
                configuration.put("evaluateStats", true);
                eba.setConfiguration(configuration.toJSONString());
                EdgeboxService.getInstance().update(eba);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * Update data in EdgeboxRule field outputConfig with correct json value mqtt-body instead of fields
     */
    public void migrateMqttPushSubscriberWrongExample() {
        try{
            //RIOT-12900: MqttPushSubscriber example is wrong
            //RIOT-12990: It is not possible migrate 3.3.4 to 4.5.0
            List<EdgeboxRule> edgeboxRulesList = EdgeboxRuleService.getInstance().
                    selectByAction("com.tierconnect.riot.bridges.cep.SuperSubscriber");
            for(EdgeboxRule edgeboxRule : edgeboxRulesList){
                //RIOT-13001: MqttPushSubscriber example is wrong (not for all)
                if(edgeboxRule.getOutputConfig() != null && !edgeboxRule.getOutputConfig().isEmpty()){
                    JSONArray outputConfig = (JSONArray)new JSONParser().parse(edgeboxRule.getOutputConfig());
                    for(Object obj : outputConfig){
                        if(obj instanceof JSONObject){
                            JSONObject jobj = (JSONObject) obj;
                            if(jobj.get("name") != null &&
                                    jobj.get("name") instanceof String){
                                String name = (String)jobj.get("name");
                                if(name.equals("com.tierconnect.riot.bridges.cep.MQTTPushSubscriber")){
                                    if(jobj.containsKey("config") &&
                                            jobj.get("config") instanceof JSONObject){
                                        JSONObject config = (JSONObject)jobj.get("config");
                                        if(config.containsKey("fields")){
                                            config.remove("fields");
                                            config.put("mqtt-body", "Serial Number: ${serialNumber}. " +
                                                    "Hi. This is the mqtt message for thing ${name}");
                                            edgeboxRule.setOutputConfig(outputConfig.toJSONString());
                                            EdgeboxRuleService.getInstance().update(edgeboxRule);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * create connexion (SQL, KAFKA, SERVICE, HADOOP) if not exist
     */
    private void migrateNewConnectiontype() {
        GroupService groupService = GroupService.getInstance();
        Group rootGroup = groupService.getRootGroup();
        ConnectionTypeService connectionTypeService = ConnectionTypeService.getInstance();
        if (connectionTypeService.getConnectionTypeByCode("SQL") == null) {
            PopDBRequired.populateSQLConnection0(rootGroup);
            logger.info("create connection type SQL");
        }
    }
}