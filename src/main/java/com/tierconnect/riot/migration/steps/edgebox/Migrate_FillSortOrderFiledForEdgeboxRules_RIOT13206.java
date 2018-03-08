package com.tierconnect.riot.migration.steps.edgebox;

import com.mysema.query.types.expr.BooleanExpression;
import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.entities.EdgeboxRule;
import com.tierconnect.riot.iot.entities.QEdgeboxRule;
import com.tierconnect.riot.iot.services.EdgeboxRuleService;
import com.tierconnect.riot.iot.services.EdgeboxService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_FillSortOrderFiledForEdgeboxRules_RIOT13206 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_FillSortOrderFiledForEdgeboxRules_RIOT13206.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        fillSortOrderFiledForEdgeboxRules();
    }

    public void fillSortOrderFiledForEdgeboxRules() {
        //all registers
        final List<Edgebox> edgeboxes = EdgeboxService.getInstance()
                .listPaginated(null, null);
        final String orderString = "name:asc";
        for(Edgebox eb : edgeboxes){
            BooleanExpression be = QEdgeboxRule.edgeboxRule.edgebox.id.eq(eb.getId());
            //all registers
            List<EdgeboxRule> edgeboxRules = EdgeboxRuleService.getInstance()
                    .listPaginated(be, null, orderString);
            int orderSequence = 0;
            for(EdgeboxRule er : edgeboxRules){
                er.setSortOrder(orderSequence);
                EdgeboxRuleService.getInstance().update(er);
                orderSequence++;
            }
        }
    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
