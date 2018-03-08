package com.tierconnect.riot.migration.steps.edgebox;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.QResource;
import com.tierconnect.riot.appcore.entities.Resource;
import com.tierconnect.riot.appcore.services.*;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.entities.Parameters;
import com.tierconnect.riot.iot.services.EdgeboxService;
import com.tierconnect.riot.iot.services.ParametersService;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;

/**
 * Created by dbascope on 07/18/2017
 */
public class Migrate_KafkaBridges_VIZIX5605 implements MigrationStep {

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {

    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateResources();
        migrateParameters();
        migrateKafkaBridges();
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {

    }

    private void  migrateResources() {
        Group rootGroup = GroupService.getInstance().getRootGroup();
        Resource moduleStreamApp = Resource.getModuleResource(rootGroup, "Stream App", "streamApp",
            ResourceServiceBase.getResourceDAO().selectBy(QResource.resource.name.eq("Gateway")));
        RoleResourceService.getInstance().insert(
            RoleService.getInstance().getRootRole(),
            ResourceService.getInstance().insert(moduleStreamApp),
            "x");
    }

    private void migrateParameters() {
        Parameters rulesProcessor = ParametersService.getInstance().getByCategoryAndCode(Constants.BRIDGE_TYPE, "Rules_Processor");
        if (rulesProcessor != null) {
            ParametersService.getInstance().delete(rulesProcessor);
        }
        Parameters thingJoiner = ParametersService.getInstance().getByCategoryAndCode(Constants.BRIDGE_TYPE, "Thing_Joiner");
        if (thingJoiner != null) {
            ParametersService.getInstance().delete(thingJoiner);
        }
        Parameters rules_processor = ParametersService.getInstance().getByCategoryAndCode(Constants.BRIDGE_TYPE, "Rules_Processor");
        if (rules_processor == null) {
            // RulesProcessor parameters.
            Parameters rulesProcessorParameters = new Parameters(Constants.BRIDGE_TYPE, "Rules_Processor",
                "@SYSTEM_PARAMETERS_BRIDGE_TYPE_RULES_PROCESSOR",
                "{\"filters\":{},\"configuration\":{\"streamConfig\":{\"value\":{\"lingerMs\":{\"value"
                    + "\":5,\"type\":\"Number\",\"order\":0,\"required\":true},"
                    + "\"numStreamThreads\":{\"value\":4,\"type\":\"Number\",\"order\":1,\"required\":true},"
                    + "\"batchSize\":{\"value\":65536,\"type\":\"Number\",\"order\":2,\"required\":true}},"
                    + "\"type\":\"JSON\",\"order\":0,\"required\":false},"
                    + "\"kafka\":{\"value\":{\"connectionCode\":{\"value\":\"KAFKA\",\"type\":\"String\","
                    + "\"order\":0,\"required\":true}},\"type\":\"JSON\",\"order\":1,\"required\":false},"
                    + "\"mongo\":{\"value\":{\"connectionCode\":{\"value\":\"MONGO\",\"type\":\"String\","
                    + "\"order\":0,\"required\":true}},\"type\":\"JSON\",\"order\":2,\"required\":false}},"
                    + "\"extra\":{\"apikey\":{\"value\":\"7B4BCCDC\",\"type\":\"String\",\"required\":false},"
                    + "\"bridgeTopic\":{\"value\":\"\",\"type\":\"String\",\"required\":false},"
                    + "\"httpHost\":{\"value\":\"\",\"type\":\"String\",\"required\":false}}}");

            ParametersService.getInstance().insert(rulesProcessorParameters);
        }
        Parameters mongo_injector = ParametersService.getInstance().getByCategoryAndCode(Constants.BRIDGE_TYPE, "Mongo_Injector");
        if (mongo_injector == null) {
            // RulesProcessor parameters.
            Parameters mongoInjectorParameters = new Parameters(Constants.BRIDGE_TYPE, "Mongo_Injector",
                "@SYSTEM_PARAMETERS_BRIDGE_TYPE_MONGO_INJECTOR",
                "{\"filters\":{},\"configuration\":{\"mongo\":{\"value\":{\"connectionCode\":{\"value"
                    + "\":\"MONGO\",\"type\":\"String\",\"order\":0,\"required\":true}},\"type\":\"JSON\","
                    + "\"order\":0,\"required\":false},"
                    + "\"streamConfig\":{\"value\":{\"lingerMs\":{\"value\":5,\"type\":\"Number\","
                    + "\"order\":0,\"required\":true},\"numStreamThreads\":{\"value\":4,\"type\":\"Number\","
                    + "\"order\":1,\"required\":true},\"batchSize\":{\"value\":65536,\"type\":\"Number\","
                    + "\"order\":2,\"required\":true}},\"type\":\"JSON\",\"order\":1,\"required\":false},"
                    + "\"kafka\":{\"value\":{\"connectionCode\":{\"value\":\"KAFKA\",\"type\":\"String\","
                    + "\"order\":0,\"required\":true}},\"type\":\"JSON\",\"order\":2,\"required\":false}},"
                    + "\"extra\":{\"apikey\":{\"value\":\"7B4BCCDC\",\"type\":\"String\",\"required\":false},"
                    + "\"bridgeTopic\":{\"value\":\"\",\"type\":\"String\",\"required\":false},"
                    + "\"httpHost\":{\"value\":\"\",\"type\":\"String\",\"required\":false}}}");

            ParametersService.getInstance().insert(mongoInjectorParameters);
        } else {
            mongo_injector.setValue("{\"filters\":{},\"configuration\":{\"mongo\":{\"value\":{\"connectionCode\":{\"value"
                + "\":\"MONGO\",\"type\":\"String\",\"order\":0,\"required\":true}},\"type\":\"JSON\","
                + "\"order\":0,\"required\":false},"
                + "\"streamConfig\":{\"value\":{\"lingerMs\":{\"value\":5,\"type\":\"Number\","
                + "\"order\":0,\"required\":true},\"numStreamThreads\":{\"value\":4,\"type\":\"Number\","
                + "\"order\":1,\"required\":true},\"batchSize\":{\"value\":65536,\"type\":\"Number\","
                + "\"order\":2,\"required\":true}},\"type\":\"JSON\",\"order\":1,\"required\":false},"
                + "\"kafka\":{\"value\":{\"connectionCode\":{\"value\":\"KAFKA\",\"type\":\"String\","
                + "\"order\":0,\"required\":true}},\"type\":\"JSON\",\"order\":2,\"required\":false}},"
                + "\"extra\":{\"apikey\":{\"value\":\"7B4BCCDC\",\"type\":\"String\",\"required\":false},"
                + "\"bridgeTopic\":{\"value\":\"\",\"type\":\"String\",\"required\":false},"
                + "\"httpHost\":{\"value\":\"\",\"type\":\"String\",\"required\":false}}}");
            ParametersService.getInstance().update(mongo_injector);
        }
    }

    private void migrateKafkaBridges() {
        for (Edgebox edgebox : EdgeboxService.getInstance().getByType("Rules_Processor")) {
            EdgeboxService.getInstance().delete(edgebox);
        }
        boolean checkFirst = true;
        try {
            for (Edgebox edgebox : EdgeboxService.getInstance().getByType("Thing_Joiner")) {
                if (checkFirst) {
                    edgebox.setType("Rules_Processor");
                    edgebox.setName("Rules Processor");
                    edgebox.setCode("Rules_Processor");
                    JSONParser parser = new JSONParser();;
                    JSONObject jsonConfig = (JSONObject) parser.parse(edgebox.getConfiguration());
                    jsonConfig.remove("inputTopic");
                    jsonConfig.remove("outputTopic");
                    jsonConfig.remove("notificationService");
                    JSONObject streamConfig = (JSONObject) jsonConfig.get("streamConfig");
                    streamConfig.remove("appId");
                    streamConfig.remove("stateDirPath");
                    jsonConfig.put("streamConfig", streamConfig);
                    edgebox.setConfiguration(jsonConfig.toJSONString());
                    EdgeboxService.getInstance().update(edgebox);
                    checkFirst = false;
                } else {
                    EdgeboxService.getInstance().delete(edgebox);
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        checkFirst = true;
        try {
            for (Edgebox edgebox : EdgeboxService.getInstance().getByType("Mongo_Injector")) {
                if (checkFirst) {
                    JSONParser parser = new JSONParser();
                    JSONObject jsonConfig = (JSONObject) parser.parse(edgebox.getConfiguration());
                    jsonConfig.remove("inputTopic");
                    jsonConfig.remove("outputTopic");
                    jsonConfig.remove("notificationService");
                    JSONObject streamConfig = (JSONObject) jsonConfig.get("streamConfig");
                    streamConfig.remove("appId");
                    streamConfig.remove("stateDirPath");
                    jsonConfig.put("streamConfig", streamConfig);
                    edgebox.setConfiguration(jsonConfig.toJSONString());
                    EdgeboxService.getInstance().update(edgebox);
                    checkFirst = false;
                } else {
                    EdgeboxService.getInstance().delete(edgebox);
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
