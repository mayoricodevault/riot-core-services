package com.tierconnect.riot.migration.steps.parameters;

import com.tierconnect.riot.appcore.entities.Connection;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.services.ConnectionService;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.entities.Parameters;
import com.tierconnect.riot.iot.popdb.PopDBRequiredIOT;
import com.tierconnect.riot.iot.services.EdgeboxService;
import com.tierconnect.riot.iot.services.ParametersService;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by dbascope on 08/11/2017.
 */
public class Migrate_BridgeParameters_VIZIX6437 implements MigrationStep {

    private static Logger logger = Logger.getLogger(Migrate_BridgeParameters_VIZIX6437.class);

    @Override public void migrateSQLBefore(String scriptPath) throws Exception {
    }

    @Override public void migrateHibernate() throws Exception {
        migrateParameters();
        migrateCoreBridge();
        migrateEdgeBridge();
        migrateAgentForBridges();
        migrateZoneDwellFilter();
    }

    @Override public void migrateSQLAfter(String scriptPath) throws Exception {
    }

    private void migrateParameters() {
        Parameters param =
            ParametersService.getInstance().getByCategoryAndCode(Constants.BRIDGE_TYPE, "edge");
        param.setValue(PopDBRequiredIOT.getEdgeBridgeCofiguration());
        ParametersService.getInstance().update(param);
        param = ParametersService.getInstance()
            .getByCategoryAndCode(Constants.BRIDGE_TYPE, Constants.EDGEBOX_STARFLEX_TYPE);
        param.setValue(PopDBRequiredIOT
            .getStarFlexBridgeConfiguration(Constants.TT_STARflex_CONFIG_CODE,
                Constants.TT_STARflex_STATUS_CODE, Constants.TT_STARflex_CODE, "", ""));
        ParametersService.getInstance().update(param);
        param = ParametersService.getInstance().getByCategoryAndCode(Constants.BRIDGE_TYPE, "FTP");
        param.setValue(PopDBRequiredIOT.getFtpConfiguration());
        ParametersService.getInstance().update(param);
        param = ParametersService.getInstance().getByCategoryAndCode(Constants.BRIDGE_TYPE, "GPS");
        param.setValue(PopDBRequiredIOT.getGpsConfiguration());
        ParametersService.getInstance().update(param);
        param = ParametersService.getInstance().getByCategoryAndCode(Constants.BRIDGE_TYPE, "core");
        param.setValue(PopDBRequiredIOT.getCoreBridgeConfiguration());
        ParametersService.getInstance().update(param);
        param = ParametersService.getInstance()
            .getByCategoryAndCode(Constants.BRIDGE_TYPE, "Rules_Processor");
        if (param != null) {
            param.setValue(PopDBRequiredIOT.getRulesProcessorConfiguration());
            ParametersService.getInstance().update(param);
        }
        param = ParametersService.getInstance()
            .getByCategoryAndCode(Constants.BRIDGE_TYPE, "Mongo_Injector");
        if (param != null) {
            param.setValue(PopDBRequiredIOT.getMongoInjectorConfiguration());
            ParametersService.getInstance().update(param);
        }
    }

    private JSONObject migrateAgentConfig(Group group, JSONObject currentConfig, boolean addSql) {
        JSONObject newConfig = new JSONObject();
        JSONObject bootstrap = new JSONObject();
        if (currentConfig.containsKey("rest") &&
            !StringUtils.isBlank(
                String.valueOf(((JSONObject)currentConfig.get("rest")).get("connectionCode")))) {
                bootstrap.put("servicesConnectionCode", ((JSONObject)currentConfig.get("rest")).get("connectionCode"));
        } else {
            Connection rest = ConnectionService.getInstance().getByCodeAndGroup("REST", group);
            if (rest == null) {
                rest = ConnectionService.getInstance().getByCodeAndGroup("SERVICES", group);
            }
            bootstrap.put("servicesConnectionCode", rest != null ? rest.getCode() : "SERVICES" + group.getCode());
        }
        if (addSql) {
            if (currentConfig.containsKey("mysqlConnectionCode") &&
                !StringUtils.isBlank(String.valueOf(currentConfig.get("mysqlConnectionCode")))) {
                bootstrap.put("sqlConnectionCode", currentConfig.get("mysqlConnectionCode"));
            } else if (currentConfig.containsKey("sql") &&
                !StringUtils.isBlank(
                    String.valueOf(((JSONObject)currentConfig.get("sql")).get("connectionCode")))) {
                bootstrap.put("sqlConnectionCode", ((JSONObject) currentConfig.get("sql")).get("connectionCode"));
            } else {
                Connection sql = ConnectionService.getInstance().getByCodeAndGroup("SQL", group);
                bootstrap.put("sqlConnectionCode", sql != null ? sql.getCode() : "SQL" + group.getCode());
            }
        }
        JSONObject jvmHeapMemory = new JSONObject();
        if (currentConfig.containsKey("javaMemory")) {
            jvmHeapMemory.put("Xmx", ((JSONObject)currentConfig.get("javaMemory")).get("Xmx"));
            jvmHeapMemory.put("Xms", ((JSONObject)currentConfig.get("javaMemory")).get("Xms"));
        } else {
            jvmHeapMemory.put("Xmx", "1024m");
            jvmHeapMemory.put("Xms", "512m");
        }
        bootstrap.put("jvmHeapMemory", jvmHeapMemory);
        newConfig.put("bridgeStartupOptions", bootstrap);
        JSONObject agent = new JSONObject();
        String groupCode = group.getCode().toUpperCase();
        String agentCode = groupCode.substring(0, Math.min(groupCode.length(), 20));
        if (currentConfig.containsKey("bridgeTopic")) {
            String topic = String.valueOf(currentConfig.get("bridgeTopic"));
            agentCode = topic.substring(topic.lastIndexOf("/") + 1);
        }
        agent.put("agentCode", agentCode);
        newConfig.put("bridgeAgent", agent);
        return newConfig;
    }

    private void migrateCoreBridge() {
        JSONParser parser = new JSONParser();
        for (Edgebox coreBridge : EdgeboxService.getInstance().getByType("core")) {
            try {
                JSONObject currentConfig = (JSONObject) parser.parse(coreBridge.getConfiguration());
                JSONObject newConfig = new JSONObject();
                JSONObject active = new JSONObject();
                active.put("active", currentConfig.get("evaluateStats") != null ? currentConfig.get("evaluateStats") : false);
                newConfig.put("coreBridgeStatistics", active);
                newConfig.put("cepChangeLogs", currentConfig.get("CEPLogging"));
                newConfig.put("numberOfThreads", currentConfig.get("numberOfThreads"));
                newConfig.put("mongo", currentConfig.get("mongo"));
                newConfig.put("mqtt", currentConfig.get("mqtt"));
                JSONObject swarm = new JSONObject();
                if (currentConfig.containsKey("swarmFilter")) {
                    swarm = (JSONObject) currentConfig.get("swarmFilter");
                    swarm.put("timeInterval", swarm.get("timeGroupTimer"));
                    swarm.remove("timeGroupTimer");
                    swarm.put("algorithm", swarm.get("swarmAlgorithm"));
                    swarm.remove("swarmAlgorithm");
                } else {
                    swarm.put("timeInterval", 5000);
                    swarm.put("algorithm", "followLastDetect");
                }
                newConfig.put("swarmRule", swarm);
                newConfig.put("shiftZoneRule", currentConfig.get("shiftZoneRule"));
                newConfig.put("outOfOrderRule", currentConfig.get("outOfOrderRule"));
                newConfig.put("thingCache", currentConfig.get("thingCache"));
                JSONObject restAPI = new JSONObject();
                restAPI.put("retryAttemptLimit", 10);
                restAPI.put("retryIntervalPeriodSecs", 5000);
                newConfig.put("thingInsertRestApi", restAPI);
                newConfig.putAll(migrateAgentConfig(coreBridge.getGroup(), currentConfig, true));
                coreBridge.setConfiguration(newConfig.toJSONString());
                EdgeboxService.getInstance().update(coreBridge);
            } catch (ParseException e) {
                logger.error("Error migrating CoreBridge " + coreBridge.getCode(), e);
            }
        }
    }

    private void migrateEdgeBridge() {
        JSONParser parser = new JSONParser();
        for (Edgebox edgeBridge : EdgeboxService.getInstance().getByType("edge")) {
            try {
                JSONObject currentConfig = (JSONObject) parser.parse(edgeBridge.getConfiguration());
                JSONObject newConfig = new JSONObject();
                newConfig.put("thingTypeCode", currentConfig.get("thingTypeCode"));
                JSONObject active = new JSONObject();
                active.put("active", currentConfig.get("evaluateStats") != null ? currentConfig.get("evaluateStats") : false);
                newConfig.put("edgeBridgeStatistics", active);
                newConfig.put("storeAleMessages", currentConfig.get("logRawMessages"));
                newConfig.put("numberOfThreads", currentConfig.get("numberOfThreads"));
                newConfig.put("facilityMapForOrigin", currentConfig.get("facilityMap"));
                newConfig.put("zoneDwellFilter", currentConfig.get("zoneDwellFilter"));
                JSONObject listener = new JSONObject();
                listener.put("bridgePort", currentConfig.get("bridgePort"));
                listener.put("socketTimeout", currentConfig.get("socketTimeout"));
                newConfig.put("httpListener", listener);
                newConfig.put("mqtt", currentConfig.get("mqtt"));
                newConfig.put("kafka", currentConfig.get("kafka"));
                newConfig.putAll(migrateAgentConfig(edgeBridge.getGroup(), currentConfig, false));
                edgeBridge.setConfiguration(newConfig.toJSONString());
                EdgeboxService.getInstance().update(edgeBridge);
            } catch (ParseException e) {
                logger.error("Error migrating EdgeBridge " + edgeBridge.getCode(), e);
            }
        }
    }

    private void migrateAgentForBridges() {
        String[] bridges = {"FTP", "GPS", "STARflex", "Rules_Processor", "Mongo_Injector"};
        JSONParser parser = new JSONParser();
        for (String bridgeCode : bridges) {
            for (Edgebox edgebox : EdgeboxService.getInstance().getByType(bridgeCode)) {
                try {
                    JSONObject currentConfig = (JSONObject) parser.parse(edgebox.getConfiguration());
                    currentConfig.putAll(migrateAgentConfig(edgebox.getGroup(), currentConfig, false));
                    currentConfig.remove("rest");
                    currentConfig.remove("sql");
                    currentConfig.remove("javaMemory");
                    currentConfig.remove("apikey");
                    currentConfig.remove("httpHost");
                    currentConfig.remove("bridgeTopic");
                    edgebox.setConfiguration(currentConfig.toJSONString());
                    EdgeboxService.getInstance().update(edgebox);
                } catch (ParseException e) {
                    logger.error("Error migrating Edgebox " + edgebox.getCode(), e);
                }
            }
        }
    }

    private void migrateZoneDwellFilter() {
        JSONParser parser = new JSONParser();
        for (Edgebox edgeBridge : EdgeboxService.getInstance().getByType("edge")) {
            try {
                JSONObject currentConfig = (JSONObject) parser.parse(edgeBridge.getConfiguration());
                if(currentConfig.containsKey("zoneDwellFilter") && currentConfig.get("zoneDwellFilter") != null){
                    JSONObject zoneDwellFilter = (JSONObject)currentConfig.get("zoneDwellFilter");
                    zoneDwellFilter.putIfAbsent("evictionTime",24);
                    currentConfig.put("zoneDwellFilter",zoneDwellFilter);
                }
                edgeBridge.setConfiguration(currentConfig.toJSONString());
                EdgeboxService.getInstance().update(edgeBridge);
            } catch (ParseException e) {
                logger.error("Error migrating EdgeBridge " + edgeBridge.getCode(), e);
            }
        }
        
        for (Edgebox edgeBridge : EdgeboxService.getInstance().getByType("STARflex")) {
            try {
                JSONObject currentConfig = (JSONObject) parser.parse(edgeBridge.getConfiguration());
                if(currentConfig.containsKey("zoneDwellFilter") && currentConfig.get("zoneDwellFilter") != null){
                    JSONObject zoneDwellFilter = (JSONObject)currentConfig.get("zoneDwellFilter");
                    zoneDwellFilter.putIfAbsent("evictionTime",24);
                    currentConfig.put("zoneDwellFilter",zoneDwellFilter);
                }
                edgeBridge.setConfiguration(currentConfig.toJSONString());
                EdgeboxService.getInstance().update(edgeBridge);
            } catch (ParseException e) {
                logger.error("Error migrating STARFlexBridge " + edgeBridge.getCode(), e);
            }
        }
    }
}
