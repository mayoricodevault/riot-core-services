package com.tierconnect.riot.migration.steps.older2;

import com.tierconnect.riot.iot.entities.EdgeboxRule;
import com.tierconnect.riot.iot.services.EdgeboxRuleService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_AlienReaderRules_RIOT4576 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_AlienReaderRules_RIOT4576.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateAlienReaderRules();
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    private void migrateAlienReaderRules() {
        logger.info("Migrating Alien Reader rules...");
        EdgeboxRuleService edgeboxRuleService = EdgeboxRuleService.getInstance();
        List<EdgeboxRule> edgeBoxRuleList = edgeboxRuleService.selectByCodeAndAction("MCB",
                "AlienReaderGPIOSubscriber");
        String actionConfig, newActionConfig, ip = "10.100.1.61", username = "alien", password = "password";
        for (EdgeboxRule edgeboxRule : edgeBoxRuleList) {
            actionConfig = edgeboxRule.getOutputConfig();
            if (!actionConfig.contains("lightPinMap")) {
                logger.info("Updating action configuration, Rule=" + edgeboxRule.getName());
                String[] lines = actionConfig.split("\n");

                try {
                    String authentication[] = lines[0].split("\\s+");
                    ip = authentication[1];
                    username = authentication[3];
                    password = authentication[5];
                } catch (Exception e) {
                    logger.error("Action configuration from rule=" + edgeboxRule.getName() + " could not be read, " +
                            "please set it manually");
                }

                newActionConfig = getLightBuzzerRuleActionConfig(ip, username, password);
                edgeboxRule.setOutputConfig(newActionConfig);
                edgeboxRule.setOutput("AlienReaderGPIOSubscriber3");
                edgeboxRuleService.update(edgeboxRule);
            }
        }
    }

    private String getLightBuzzerRuleActionConfig(String ip, String username, String password) {
        return "{\"ip\":\"" + ip + "\"," +
                "\"port\":23," +
                "\"username\":\"" + username + "\"," +
                "\"password\":\"" + password + "\"," +
                "\"times\":{\"lightOn\":5000,\"buzzerOn\":3000,\"buzzerOff\":1000}," +
                "\"lightPinMap\":{\"light1\":0,\"light2\":2,\"light3\":3,\"light4\":4}," +
                "\"buzzerPinMap\":{\"buzzer1\":1},\"zoneLightBuzzerMap\":{\"Exit1\":[\"light1\"," +
                "\"buzzer1\"],\"Exit2\":[\"light2\",\"buzzer1\"],\"Exit3\":[\"light3\",\"buzzer1\"]," +
                "\"Exit4\":[\"light4\",\"buzzer1\"]}}";

    }
}
