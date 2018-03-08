package com.tierconnect.riot.migration.steps.edgebox;

import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.entities.Parameters;
import com.tierconnect.riot.iot.services.EdgeboxService;
import com.tierconnect.riot.iot.services.ParametersService;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;

import java.util.List;

/**
 * Created by brayan on 5/26/17.
 */
public class Migrate_AddBridgePortALEBridge_VIZIX4818 implements MigrationStep {

    private static Logger logger = Logger.getLogger(Migrate_AddBridgePortALEBridge_VIZIX4818.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {

    }

    @Override
    public void migrateHibernate() throws Exception {
        addBridgePortToAleBridgeConfiguration();
        addBridgePortToAleBridgeConfigurationTemplate();
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {

    }

    private void addBridgePortToAleBridgeConfiguration(){
        List<Edgebox> edgeBridgeList = EdgeboxService.getInstance().getByType("edge");
        for (Edgebox edgebox : edgeBridgeList){
            try {
                JSONObject outputConfig = (JSONObject) new JSONParser().parse(edgebox.getConfiguration());
                if (!outputConfig.containsKey("bridgePort")) {
                    outputConfig.put("bridgePort", 9090);
                    edgebox.setConfiguration(outputConfig.toJSONString());
                    EdgeboxService.getInstance().update(edgebox);
                    logger.info("Bridge "+edgebox.getCode()+" updated with bridgePort");
                }
            } catch (ParseException e) {
                logger.error("Error in updating bridge configuration: " + edgebox.getCode(), e);
            }
        }
    }

    private void addBridgePortToAleBridgeConfigurationTemplate(){
        Parameters edgeParameters = ParametersService.getInstance().getByCategoryAndCode(Constants.BRIDGE_TYPE, "edge");
        if (edgeParameters != null) {
            JSONObject outputConfig = null;
            try {
                outputConfig = (JSONObject) new JSONParser().parse(edgeParameters.getValue());
            } catch (ParseException e) {
                logger.error("Error in updating edgeBridge parameters: " + outputConfig, e);
            }

            if (outputConfig.get("configuration") != null){
                JSONObject configuration = (JSONObject)outputConfig.get("configuration");

                JSONObject bridgePort = new JSONObject();
                bridgePort.put("value", 9090);
                bridgePort.put("type", "Number");
                bridgePort.put("required", false);

                configuration.put("bridgePort",bridgePort);
                outputConfig.put("configuration", configuration);
                edgeParameters.setValue(outputConfig.toJSONString());
                ParametersService.getInstance().update(edgeParameters);
            }
        }
    }
}
