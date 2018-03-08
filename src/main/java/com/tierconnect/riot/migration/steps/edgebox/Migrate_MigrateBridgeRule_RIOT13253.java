package com.tierconnect.riot.migration.steps.edgebox;

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
public class Migrate_MigrateBridgeRule_RIOT13253 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_MigrateBridgeRule_RIOT13253.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateBridgeRule();
    }

    private void migrateBridgeRule() {
        EdgeboxRuleService instance = EdgeboxRuleService.getInstance();
        List<EdgeboxRule> edgeboxRuleList = instance.selectByAction("ThingPropertySetterJSSubscriber");
        for (EdgeboxRule rule : edgeboxRuleList) {
            rule.setConditionType("ALWAYS_TRUE");
            rule.setRule("select * from messageEventType where 1=1 ");
            instance.update(rule);
            logger.info("update rule " + rule.getName());
        }
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
