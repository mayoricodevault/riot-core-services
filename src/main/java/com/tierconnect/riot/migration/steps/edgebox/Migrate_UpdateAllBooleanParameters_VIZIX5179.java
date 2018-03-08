package com.tierconnect.riot.migration.steps.edgebox;

import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.services.EdgeboxService;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;

import java.util.Arrays;
import java.util.List;

/**
 * Created by brayan on 5/29/17.
 */
public class Migrate_UpdateAllBooleanParameters_VIZIX5179 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_AddBridgePortALEBridge_VIZIX4818.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {

    }

    @Override
    public void migrateHibernate() throws Exception {
        updateCoreBridgeBooleanParameters();
        updateAleBridgeBooleanParameters();
        updateStarflexBridgeBooleanParameters();
        updateFTPBridgeBooleanParameters();
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {

    }

    private void updateCoreBridgeBooleanParameters(){
        List<Edgebox> coreBridgeList = EdgeboxService.getInstance().getByType("core");
        List<String> controlCoreBoolean = Arrays.asList("sourceRule", "CEPLogging", "pointInZoneRule", "doorEventRule", "shiftZoneRule");
        for (Edgebox coreBridge : coreBridgeList){
            try {
                JSONObject outputConfig = (JSONObject) new JSONParser().parse(coreBridge.getConfiguration());
                for (String control : controlCoreBoolean){
                    if (outputConfig.containsKey(control)){
                        JSONObject jsonControlBoolean = (JSONObject) outputConfig.get(control);
                        if (jsonControlBoolean.containsKey("active")){
                            String actualValue = String.valueOf(jsonControlBoolean.get("active"));
                            jsonControl(actualValue, jsonControlBoolean, "active");
                            logger.info("CoreBridge value=" + control + " updated with value=" + jsonControlBoolean.get("active"));
                        }
                        outputConfig.put(control, jsonControlBoolean);
                    }
                    coreBridge.setConfiguration(outputConfig.toJSONString());
                    EdgeboxService.getInstance().update(coreBridge);
                }
            } catch (ParseException e) {
                logger.error("Error in updating bridge configuration: " + coreBridge.getCode(), e);
            }
        }
    }

    private void jsonControl(String actualValue, JSONObject jsonControlBoolean, String parameter){
        if (actualValue.equals("1") || actualValue.equals("true")) {
            jsonControlBoolean.put(parameter, true);
        } else if (actualValue.equals("0") || actualValue.equals("false")) {
            jsonControlBoolean.put(parameter, false);
        }
    }

    private void updateAleBridgeBooleanParameters(){
        updateEdgeboxBridgeBooleanParameters("edge", Arrays.asList("send500ErrorOnTimeout", "logRawMessages"), true);
    }

    private void updateStarflexBridgeBooleanParameters(){
        updateEdgeboxBridgeBooleanParameters("STARflex", Arrays.asList("logRawMessages"), true);
    }

    private void updateFTPBridgeBooleanParameters(){
        updateEdgeboxBridgeBooleanParameters("FTP", Arrays.asList("logRawMessages"), false);
    }

    private void updateEdgeboxBridgeBooleanParameters(String bridgeType, List<String> controlEdgeBooleanInLine, boolean internal){
        List<Edgebox> edgeBridgeList = EdgeboxService.getInstance().getByType(bridgeType);
        String controlEdgeBooleanZDF = "zoneDwellFilter";
        for (Edgebox edgeBridge : edgeBridgeList){
            try {
                JSONObject outputConfig = (JSONObject) new JSONParser().parse(edgeBridge.getConfiguration());
                for (String control : controlEdgeBooleanInLine){
                    if (outputConfig.containsKey(control)){
                        String actualValue = String.valueOf(outputConfig.get(control));
                        jsonControl(actualValue, outputConfig, control);
                        logger.info("Edgebridge: "+bridgeType+" value=" + control + " updated with value=" + outputConfig.get(control));
                    }
                }
                if (outputConfig.containsKey(controlEdgeBooleanZDF) && internal){
                    JSONObject jsonControlBoolean = (JSONObject) outputConfig.get(controlEdgeBooleanZDF);
                    if (jsonControlBoolean.containsKey("active")){
                        String actualValue = String.valueOf(jsonControlBoolean.get("active"));
                        jsonControl(actualValue, jsonControlBoolean, "active");
                    }
                    if (jsonControlBoolean.containsKey("lastDetectTimeActive")){
                        String actualValue = String.valueOf(jsonControlBoolean.get("lastDetectTimeActive"));
                        jsonControl(actualValue, jsonControlBoolean, "lastDetectTimeActive");
                    }
                }
                if (bridgeType.equals("STARflex") && outputConfig.containsKey("rateFilter")){
                    JSONObject jsonControlBoolean = (JSONObject) outputConfig.get("rateFilter");
                    if (jsonControlBoolean.containsKey("active")){
                        String actualValue = String.valueOf(jsonControlBoolean.get("active"));
                        jsonControl(actualValue, jsonControlBoolean, "active");
                    }
                }
                edgeBridge.setConfiguration(outputConfig.toJSONString());
                EdgeboxService.getInstance().update(edgeBridge);
            } catch (ParseException e) {
                logger.error("Error in updating bridge configuration: " + edgeBridge.getCode(), e);
            }
        }
    }
}
