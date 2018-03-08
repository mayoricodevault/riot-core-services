package com.tierconnect.riot.migration.steps.edgebox;

import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.iot.entities.EdgeboxRule;
import com.tierconnect.riot.iot.services.EdgeboxRuleService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Created by ruth on 23-08-17.
 */
public class Migrate_AddSerialExecutionColumn_VIZIX6767 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_AddSerialExecutionColumn_VIZIX6767.class);
    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        //All edge boxes should be migrated with serialExecution TRUE, because all Vizix Functions(conditionType!=CEP) are
        //executed serializable
        List<EdgeboxRule> lstEdgeBoxRule = EdgeboxRuleService.getInstance().listPaginated(null, null);
        if ((lstEdgeBoxRule != null) && (!lstEdgeBoxRule.isEmpty())){
            for(EdgeboxRule edgeboxRule : lstEdgeBoxRule) {
                if (!edgeboxRule.getConditionType().equals(Constants.CONDITION_TYPE_CEP)) {
                    //Select all edge box rules different that condition type like CEP
                    edgeboxRule.setSerialExecution(true);
                } else {
                    edgeboxRule.setSerialExecution(false);
                }
                EdgeboxRuleService.getInstance().update(edgeboxRule);
            }
        }
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {

    }
}
