package com.tierconnect.riot.migration.steps.reportDefinition;

import com.tierconnect.riot.iot.entities.ReportFilter;
import com.tierconnect.riot.iot.entities.ReportGroupBy;
import com.tierconnect.riot.iot.entities.ReportProperty;
import com.tierconnect.riot.iot.entities.ReportRule;
import com.tierconnect.riot.iot.services.ReportFilterService;
import com.tierconnect.riot.iot.services.ReportGroupByService;
import com.tierconnect.riot.iot.services.ReportPropertyService;
import com.tierconnect.riot.iot.services.ReportRuleService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_MigrateReportProperties_NOISSUE implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_MigrateReportProperties_NOISSUE.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migratePropertyName();
    }

    private static void migratePropertyName() {
        migrateReportFiler();
        migrateReportProperty();
        migrateReportRule();
        migrateReportGroupBy();
    }

    private static void migrateReportFiler() {
        List<ReportFilter> reportFilters = ReportFilterService.getInstance().getFiltersByPropertyName("localMap.id");
        for (ReportFilter reportFilter : reportFilters) {
            reportFilter.setPropertyName("zoneLocalMap.id");
        }
    }

    private static void migrateReportProperty() {
        List<ReportProperty> reportProperties = ReportPropertyService.getInstance().getPropertiesByPropertyName
                ("localMap.id");
        for (ReportProperty reportProperty : reportProperties) {
            reportProperty.setPropertyName("zoneLocalMap.id");
        }
    }

    private static void migrateReportRule() {
        List<ReportRule> reportRules = ReportRuleService.getInstance().getRuleByPropertyName("localMap.id");
        for (ReportRule reportRule : reportRules) {
            reportRule.setPropertyName("zoneLocalMap.id");
        }
    }

    private static void migrateReportGroupBy() {
        List<ReportGroupBy> reportGroupBys = ReportGroupByService.getInstance().getGroupByPropertyName("localMap.id");
        for (ReportGroupBy reportGroupBy : reportGroupBys) {
            reportGroupBy.setPropertyName("zoneLocalMap.id");
        }
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
