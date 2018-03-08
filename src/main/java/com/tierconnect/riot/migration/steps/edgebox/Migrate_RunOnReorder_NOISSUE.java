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
public class Migrate_RunOnReorder_NOISSUE implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_RunOnReorder_NOISSUE.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper dbHelper = new DBHelper();
        if(!dbHelper.existTable("edgeboxrule") && !dbHelper.existColumn("edgeboxrule", "runOnReorder")) {
            DBHelper.executeSQLFile(scriptPath);
        }
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateEdgeBoxRules();
    }

    private void migrateEdgeBoxRules() {
        List<EdgeboxRule> edgeboxRules = EdgeboxRuleService.getEdgeBoxRuleDAO().selectAll();
        for (EdgeboxRule edgeboxRule : edgeboxRules){
            edgeboxRule.setRunOnReorder(false);
            EdgeboxRuleService.getEdgeBoxRuleDAO().update(edgeboxRule);
        }
    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
