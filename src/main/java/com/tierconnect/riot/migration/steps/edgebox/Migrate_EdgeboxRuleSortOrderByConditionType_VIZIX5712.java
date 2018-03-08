package com.tierconnect.riot.migration.steps.edgebox;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.utils.QueryUtils;
import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.entities.EdgeboxRule;
import com.tierconnect.riot.iot.entities.QEdgeboxRule;
import com.tierconnect.riot.iot.services.EdgeboxRuleService;
import com.tierconnect.riot.iot.services.EdgeboxService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import com.tierconnect.riot.sdk.dao.Pagination;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_EdgeboxRuleSortOrderByConditionType_VIZIX5712 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_EdgeboxRuleSortOrderByConditionType_VIZIX5712.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {

        Pagination pagination = new Pagination( 1, -1 );
        List<Edgebox> edgeboxList= EdgeboxService.getInstance().listPaginated( null, pagination, "");

        for (Edgebox edgebox : edgeboxList) {
            List<ObjectNode> conditionTypeGroups = EdgeboxRuleService.getInstance().getRuleGroupParameters();

            for (ObjectNode groupParameter : conditionTypeGroups) {
                BooleanBuilder be = new BooleanBuilder();
                be = be.and( QueryUtils.buildSearch( QEdgeboxRule.edgeboxRule, groupParameter.get("where").asText() ) );
                be = be.and( QueryUtils.buildSearch( QEdgeboxRule.edgeboxRule, "edgebox.id=" + Long.toString( edgebox.getId() ) ) );

                List<EdgeboxRule> edgeboxRulesList = EdgeboxRuleService.getInstance().listPaginated(be, pagination, "sortOrder:asc");

                int count = 0;
                for (EdgeboxRule edgeboxRule : edgeboxRulesList) {
                    edgeboxRule.setSortOrder(count);
                    count = count + 1;
                    EdgeboxRuleService.getInstance().update(edgeboxRule);
                }
            }
        }
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

}
