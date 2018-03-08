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
 * Created by brayan on 6/7/17.
 */
public class Migrate_RemoveDeprecatedConfBridges_VIZIX5319 implements MigrationStep{
    private static Logger logger = Logger.getLogger(Migrate_RemoveDeprecatedConfBridges_VIZIX5319.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {

    }

    @Override
    public void migrateHibernate() throws Exception {
        removeConfigurationCoreBridge();
        removeConfigurationEdgeBridge();
        removeDeprecatedCoreParameters();
        removeDeprecatedEdgeParameters("edge");
        removeDeprecatedEdgeParameters("GPS");
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {

    }

    private void removeConfigurationCoreBridge(){
        List<Edgebox> coreBridgeList = EdgeboxService.getInstance().getByType("core");
        for (Edgebox coreBridge : coreBridgeList){
            try {
                JSONObject outputConfig = (JSONObject) new JSONParser().parse(coreBridge.getConfiguration());
                if (outputConfig.containsKey("pointInZoneRule")) {
                    outputConfig.remove("pointInZoneRule");
                }
                if (outputConfig.containsKey("doorEventRule")) {
                    outputConfig.remove("doorEventRule");
                }
                if (outputConfig.containsKey("reloadCacheTickle")){
                    outputConfig.remove("reloadCacheTickle");
                }
                coreBridge.setConfiguration(outputConfig.toJSONString());
            } catch (ParseException e) {
                logger.error("Error in updating coreBridge configuration: " + coreBridge.getCode(), e);
            }
            EdgeboxService.getInstance().update(coreBridge);
        }
    }

    private void removeConfigurationEdgeBridge(){
        List<Edgebox> edgeBridgeList = EdgeboxService.getInstance().getByType("edge");
        edgeBridgeList.addAll(EdgeboxService.getInstance().getByType("GPS"));
        for (Edgebox edgeBridge : edgeBridgeList){
            try {
                JSONObject outputConfig = (JSONObject) new JSONParser().parse(edgeBridge.getConfiguration());
                if (outputConfig.containsKey("timeDistanceFilter")) {
                    outputConfig.remove("timeDistanceFilter");
                }
                if (outputConfig.containsKey("zoneChangeFilter")) {
                    outputConfig.remove("zoneChangeFilter");
                }
                if (outputConfig.containsKey("timeZoneFilter")) {
                    outputConfig.remove("timeZoneFilter");
                }
                edgeBridge.setConfiguration(outputConfig.toJSONString());
            } catch (ParseException e) {
                logger.error("Error in updating edgeBridge configuration: " + edgeBridge.getCode(), e);
            }
            EdgeboxService.getInstance().update(edgeBridge);
        }
    }

    private void removeDeprecatedCoreParameters(){
        Parameters coreParameters = ParametersService.getInstance().getByCategoryAndCode(Constants.BRIDGE_TYPE, "core");
        if (coreParameters != null){
            JSONObject outputConfig = null;
            try {
                outputConfig = (JSONObject) new JSONParser().parse(coreParameters.getValue());
                if (outputConfig.containsKey("filters")){
                    JSONObject filters = (JSONObject) outputConfig.get("filters");
                    if (filters.containsKey("pointInZoneRule")){
                        filters.remove("pointInZoneRule");
                    }
                    if (filters.containsKey("doorEventRule")){
                        filters.remove("doorEventRule");
                    }
                    if (filters.containsKey("reloadCacheTickle")){
                        filters.remove("reloadCacheTickle");
                    }
                    outputConfig.put("filters", filters);
                }
                if (outputConfig.containsKey("configuration")){
                    JSONObject configuration = (JSONObject) outputConfig.get("configuration");
                    if (configuration.containsKey("reloadCacheTickle")){
                        configuration.remove("reloadCacheTickle");
                    }
                    outputConfig.put("configuration", configuration);
                }
                coreParameters.setValue(outputConfig.toJSONString());
            } catch (ParseException e) {
                logger.error("Error in updating edgeBridge parameters: " + coreParameters, e);
            }
            ParametersService.getInstance().update(coreParameters);
        }
    }

    private void removeDeprecatedEdgeParameters(String type){
        Parameters edgeParameters = ParametersService.getInstance().getByCategoryAndCode(Constants.BRIDGE_TYPE, type);
        if (edgeParameters != null){
            JSONObject outputConfig = null;
            try {
                outputConfig = (JSONObject) new JSONParser().parse(edgeParameters.getValue());
                if (outputConfig.containsKey("filters")){
                    JSONObject filters = (JSONObject) outputConfig.get("filters");
                    if (filters.containsKey("timeDistanceFilter")){
                        filters.remove("timeDistanceFilter");
                    }
                    if (filters.containsKey("zoneChangeFilter")){
                        filters.remove("zoneChangeFilter");
                    }
                    if (filters.containsKey("timeZoneFilter")){
                        filters.remove("timeZoneFilter");
                    }
                    outputConfig.put("filters", filters);
                }
                edgeParameters.setValue(outputConfig.toJSONString());
            } catch (ParseException e) {
                logger.error("Error in updating edgeBridge parameters: " + edgeParameters, e);
            }
            ParametersService.getInstance().update(edgeParameters);
        }
    }

}
