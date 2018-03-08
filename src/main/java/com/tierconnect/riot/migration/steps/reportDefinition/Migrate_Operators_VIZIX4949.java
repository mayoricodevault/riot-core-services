package com.tierconnect.riot.migration.steps.reportDefinition;

import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.iot.entities.ReportFilter;
import com.tierconnect.riot.iot.entities.ReportRule;
import com.tierconnect.riot.iot.services.ReportFilterService;
import com.tierconnect.riot.iot.services.ReportRuleService;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Created by ybarriga
 * on 05/23/17
 */
@SuppressWarnings("unused")
public class Migrate_Operators_VIZIX4949 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_Operators_VIZIX4949.class);
    private static final String OP_IS_DEFINED = "isDefined";
    private static final String OP_IS_NOT_DEFINED = "isNotDefined";
    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateFilterOperators();
        logger.info("Successful migration of the Operators into filters.");
        migrateRuleOperators();
        logger.info("Successful migration of the Operators into Rules.");
    }

    private void migrateFilterOperators() {
        updateFilterOperator(OP_IS_DEFINED, Constants.OP_IS_NOT_EMPTY);
        updateFilterOperator(OP_IS_NOT_DEFINED, Constants.OP_IS_EMPTY);
    }

    private void updateFilterOperator(String operatorToSearch, String operatorToReplace) {
        List<ReportFilter> reportFilters = ReportFilterService.getInstance().getFiltersByOperator(operatorToSearch);
        for (ReportFilter reportFilter : reportFilters) {
            reportFilter.setOperator(operatorToReplace);
            ReportFilterService.getInstance().update(reportFilter);

        }
    }

    private void migrateRuleOperators() {
        updateRulesOperator(OP_IS_DEFINED, Constants.OP_IS_NOT_EMPTY);
        updateRulesOperator(OP_IS_NOT_DEFINED, Constants.OP_IS_EMPTY);
    }

    private void updateRulesOperator(String operatorToSearch, String operatorToReplace) {
        List<ReportRule> reportRules = ReportRuleService.getInstance().getRulesByOperator(operatorToSearch);
        for (ReportRule reportRule : reportRules) {
            reportRule.setOperator(operatorToReplace);
            ReportRuleService.getInstance().update(reportRule);
        }
    }

    @Override

    public void migrateSQLAfter(String scriptPath) throws Exception {
    }
}
