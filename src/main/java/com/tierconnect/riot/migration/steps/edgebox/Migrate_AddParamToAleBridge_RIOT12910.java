package com.tierconnect.riot.migration.steps.edgebox;

import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.entities.EdgeboxRule;
import com.tierconnect.riot.iot.services.EdgeboxRuleService;
import com.tierconnect.riot.iot.services.EdgeboxService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.MigrationException;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;
import org.jose4j.json.internal.json_simple.JSONArray;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;

import java.util.List;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_AddParamToAleBridge_RIOT12910 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_AddParamToAleBridge_RIOT12910.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateEdgeBoxForNecessaryParametersOnBridges();
        migrateMqttPushSubscriberWrongExample();
    }

    /**
     * Update data in Edgebox field configuration with necessary on Bridges
     */
    private void migrateEdgeBoxForNecessaryParametersOnBridges() {

            //CORE BRIDGE data
            List<Edgebox> coreList = EdgeboxService.getInstance().getByType("core");
            for (Edgebox ebc : coreList) {
                try{
                    JSONObject configuration = (JSONObject) new JSONParser().parse(ebc.getConfiguration());
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
                } catch (ParseException e) {
                    throw new MigrationException("Error parsing Edgebox CoreBridge type, " +
                            "code: " + ebc.getCode() + ", " +
                            "name: " + ebc.getName() + ", " +
                            "configuration: " + ebc.getConfiguration(), e);
                }
            }
            //ALE bridge
            List<Edgebox> aleList = EdgeboxService.getInstance().getByType("edge");
            for (Edgebox eba : aleList) {
                try{
                    JSONObject configuration = (JSONObject) new JSONParser().parse(eba.getConfiguration());
                    //RIOT-12855: Adding evaluateStats boolean parameter
                    configuration.put("evaluateStats", true);
                    eba.setConfiguration(configuration.toJSONString());
                    EdgeboxService.getInstance().update(eba);
                } catch (ParseException e) {
                    throw new MigrationException("Error parsing Edgebox EdgeBridge type, " +
                            "code: " + eba.getCode() + ", " +
                            "name: " + eba.getName() + ", " +
                            "configuration: " + eba.getConfiguration(), e);
                }
            }

    }

    /**
     * Update data in EdgeboxRule field outputConfig with correct json value mqtt-body instead of fields
     */
    private void migrateMqttPushSubscriberWrongExample() {
        try {
            //RIOT-12900: MqttPushSubscriber example is wrong
            //RIOT-12990: It is not possible migrate 3.3.4 to 4.5.0
            List<EdgeboxRule> edgeboxRulesList = EdgeboxRuleService.getInstance().
                    selectByAction("com.tierconnect.riot.bridges.cep.SuperSubscriber");
            for (EdgeboxRule edgeboxRule : edgeboxRulesList) {
                //RIOT-13001: MqttPushSubscriber example is wrong (not for all)
                if (edgeboxRule.getOutputConfig() != null && !edgeboxRule.getOutputConfig().isEmpty()) {
                    JSONArray outputConfig = (JSONArray) new JSONParser().parse(edgeboxRule.getOutputConfig());
                    for (Object obj : outputConfig) {
                        if (obj instanceof JSONObject) {
                            JSONObject jobj = (JSONObject) obj;
                            if (jobj.get("name") != null &&
                                    jobj.get("name") instanceof String) {
                                String name = (String) jobj.get("name");
                                if (name.equals("com.tierconnect.riot.bridges.cep.MQTTPushSubscriber")) {
                                    if (jobj.containsKey("config") &&
                                            jobj.get("config") instanceof JSONObject) {
                                        JSONObject config = (JSONObject) jobj.get("config");
                                        if (config.containsKey("fields")) {
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

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

}
