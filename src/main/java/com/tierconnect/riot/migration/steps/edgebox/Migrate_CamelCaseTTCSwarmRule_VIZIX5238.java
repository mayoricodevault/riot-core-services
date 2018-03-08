package com.tierconnect.riot.migration.steps.edgebox;

import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.entities.Parameters;
import com.tierconnect.riot.iot.services.EdgeboxService;
import com.tierconnect.riot.iot.services.ParametersService;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.jose4j.json.internal.json_simple.JSONArray;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.apache.log4j.Logger;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.ParseException;

import java.util.Iterator;
import java.util.List;

/**
 * Created by brayan on 6/1/17.
 */
public class Migrate_CamelCaseTTCSwarmRule_VIZIX5238 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_CamelCaseTTCSwarmRule_VIZIX5238.class);


    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {

    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateCoreBridgeTTCSwarmRule();
        updateCoreBridgeConfTemplateSwarmThingTypeCode();
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {

    }

    private void migrateCoreBridgeTTCSwarmRule(){
        List<Edgebox> coreBridges = EdgeboxService.getInstance().getByType("core");
        for (Edgebox coreBridge : coreBridges){
            try {
                JSONObject coreBridgeConfig = (JSONObject) new JSONParser().parse(coreBridge.getConfiguration());
                if (coreBridgeConfig.containsKey("swarmFilter")){
                    updateSwarmConfThingTypeCode(coreBridgeConfig);
                }
                coreBridge.setConfiguration(coreBridgeConfig.toJSONString());
                EdgeboxService.getInstance().update(coreBridge);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateSwarmConfThingTypeCode(JSONObject coreBridgeConfig){
        JSONObject swarmRuleObject = (JSONObject) coreBridgeConfig.get("swarmFilter");
        JSONArray thingTypes;
        if (swarmRuleObject.containsKey("value")){
            swarmRuleObject = (JSONObject) swarmRuleObject.get("value");
            JSONObject thingTypesObject = (JSONObject) swarmRuleObject.get("thingTypes");
            thingTypes = (JSONArray) thingTypesObject.get("value");
        } else {
            thingTypes = (JSONArray) swarmRuleObject.get("thingTypes");
        }
        Iterator iterator = thingTypes.iterator();
        while (iterator.hasNext()){
            JSONObject swarmRuleConf = (JSONObject) iterator.next();
            if (swarmRuleConf.containsKey("thingtypeCode")){
                String thingTypeCode = (String) swarmRuleConf.get("thingtypeCode");
                swarmRuleConf.remove("thingtypeCode");
                swarmRuleConf.put("thingTypeCode", thingTypeCode);
                logger.info("CoreBridge SwarmRule thingTypeCode updated");
            }
        }
    }

    private void updateCoreBridgeConfTemplateSwarmThingTypeCode(){
        Parameters coreParameters = ParametersService.getInstance().getByCategoryAndCode(Constants.BRIDGE_TYPE, "core");
        if (coreParameters != null) {
            JSONObject outputConfig = null;
            try {
                outputConfig = (JSONObject) new JSONParser().parse(coreParameters.getValue());
            } catch (ParseException e) {
                e.printStackTrace();
            }

            if (outputConfig.get("configuration") != null){
                JSONObject configuration = (JSONObject)outputConfig.get("configuration");
                if (configuration.containsKey("swarmFilter")){
                    updateSwarmConfThingTypeCode(configuration);
                }
                outputConfig.put("configuration", configuration);
                coreParameters.setValue(outputConfig.toJSONString());
                ParametersService.getInstance().update(coreParameters);
            }
        }
    }
}
