package com.tierconnect.riot.migration.steps.edgebox;

import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.entities.EdgeboxRule;
import com.tierconnect.riot.iot.services.EdgeboxRuleService;
import com.tierconnect.riot.iot.services.EdgeboxService;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;
import org.jose4j.json.internal.json_simple.JSONArray;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;

import java.util.List;

/**
 * Created by brayan on 6/7/17.
 */
public class Migrate_RenameSuperSubscriberRulesDef_VIZIX5507 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_RenameSuperSubscriberRulesDef_VIZIX5507.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {

    }

    @Override
    public void migrateHibernate() throws Exception {
        renameClassPackageRestEndpointAction();
        renameClassPackageSuperSubscriberAtion();
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {

    }

    private void renameClassPackageRestEndpointAction(){
        // update RestEndpoint subscriber output (action)
        List<EdgeboxRule> edgeboxRules = EdgeboxRuleService.getInstance().selectByAction("com.tierconnect.riot.bridges.cep.RestEndpointSubscriber");
        for (EdgeboxRule edgeboxRule : edgeboxRules){
            edgeboxRule.setOutput("com.tierconnect.riot.bridges.rules.actions.RestEndpointSubscriber");
            EdgeboxRuleService.getInstance().update(edgeboxRule);
        }
    }

    private void renameClassPackageSuperSubscriberAtion(){
        // update Super subscriber (action)
        List<EdgeboxRule> edgeboxRules = EdgeboxRuleService.getInstance().selectByAction("com.tierconnect.riot.bridges.cep.SuperSubscriber");
        for (EdgeboxRule edgeboxRule : edgeboxRules){
            try {
                edgeboxRule.setOutput("com.tierconnect.riot.bridges.rules.actions.SuperSubscriber");
                JSONArray outputConfig = (JSONArray) new JSONParser().parse(edgeboxRule.getOutputConfig());
                for (Object o : outputConfig){
                    if (((JSONObject)o).containsKey("name")) {
                        String name = (String) ((JSONObject)o).get("name");
                        // verify if name starts with 'com.tierconnect.riot.bridges.cep'
                        if (name.startsWith("com.tierconnect.riot.bridges.cep")){
                            String newName = name.replace("com.tierconnect.riot.bridges.cep", "com.tierconnect.riot.bridges.rules.actions");
                            ((JSONObject)o).put("name", newName);
                        }
                    }
                }
                edgeboxRule.setOutputConfig(outputConfig.toJSONString());
                EdgeboxRuleService.getInstance().update(edgeboxRule);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }
}
