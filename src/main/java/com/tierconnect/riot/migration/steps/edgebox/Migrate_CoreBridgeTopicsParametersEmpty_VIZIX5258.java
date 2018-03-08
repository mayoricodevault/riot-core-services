package com.tierconnect.riot.migration.steps.edgebox;

import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.iot.entities.Parameters;
import com.tierconnect.riot.iot.services.ParametersService;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;
import org.jose4j.json.internal.json_simple.JSONArray;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;

/**
 * Created by brayan on 6/1/17.
 */
public class Migrate_CoreBridgeTopicsParametersEmpty_VIZIX5258 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_CoreBridgeTopicsParametersEmpty_VIZIX5258.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {

    }

    @Override
    public void migrateHibernate() throws Exception {
        removeTopicsFromCBTemplate();
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {

    }

    private void removeTopicsFromCBTemplate(){
        Parameters coreParameters = ParametersService.getInstance().getByCategoryAndCode(Constants.BRIDGE_TYPE, "core");
        if (coreParameters != null){
            JSONObject outputConfig = null;
            try {
                outputConfig = (JSONObject) new JSONParser().parse(coreParameters.getValue());
            } catch (ParseException e) {
                e.printStackTrace();
            }

            if (outputConfig.get("configuration") != null){
                JSONObject configuration = (JSONObject)outputConfig.get("configuration");
                JSONObject mqtt = (JSONObject)configuration.get("mqtt");
                JSONObject mqttValue;
                JSONObject mqttValueTopics;
                if (mqtt.containsKey("value")){
                    mqttValue = (JSONObject)mqtt.get("value");
                    if (mqttValue.containsKey("topics")){
                        mqttValueTopics = (JSONObject)mqttValue.get("topics");
                        JSONArray mqttValueTopicsValues = (JSONArray)mqttValueTopics.get("value");
                        mqttValueTopicsValues.clear();
                        logger.info("coreBridge mqttTopics cleared from coreBridge template");
                    }
                }
                coreParameters.setValue(outputConfig.toJSONString());
                ParametersService.getInstance().update(coreParameters);
            }
        }
    }
}
