package com.tierconnect.riot.migration.steps.reportDefinition;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.iot.entities.QReportDefinition;
import com.tierconnect.riot.iot.entities.ReportDefinition;
import com.tierconnect.riot.iot.services.ReportDefinitionService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Created by cess on 9/4/17.
 */

public class Migrate_AllReportsFolder_VIZIX7488 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_AllReportsFolder_VIZIX7488.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateFeature();
    }

    private void migrateFeature() {
        updateGroupReports(GroupService.getInstance().getRootGroup());
    }

    private void updateGroupReports(Group rootGroup) {
        BooleanBuilder be = new BooleanBuilder();
        //tutorials
        be.and(QReportDefinition.reportDefinition.name.eq("Mongo Filters Query (Now)")).or(
                QReportDefinition.reportDefinition.name.eq("Mongo Filters Query (Custom)")).or(
                QReportDefinition.reportDefinition.name.eq("Mongo Example #1a")).or(
                QReportDefinition.reportDefinition.name.eq("Mongo Example #1b")).or(
                QReportDefinition.reportDefinition.name.eq("Mongo Example #1c")).or(
                QReportDefinition.reportDefinition.name.eq("Mongo Example #2")).or(
                QReportDefinition.reportDefinition.name.eq("Mongo TimeZone Test")).or(
                QReportDefinition.reportDefinition.name.eq("SQL Report Example")).or(
                //diagnostics
                QReportDefinition.reportDefinition.name.eq("ViZix Out-of-Order")).or(
                QReportDefinition.reportDefinition.name.eq("ViZix Out-of-Order by Filter")).or(
                QReportDefinition.reportDefinition.name.eq("ViZix Out-of-Order by Starflex")).or(
                QReportDefinition.reportDefinition.name.eq("ViZix Upserts CB by Source - I")).or(
                QReportDefinition.reportDefinition.name.eq("ViZix Upserts CB by Source - E")).or(
                QReportDefinition.reportDefinition.name.eq("ViZix Upserts EB - I")).or(
                QReportDefinition.reportDefinition.name.eq("ViZix Upserts by SF Filter - E")).or(
                QReportDefinition.reportDefinition.name.eq("ViZix Upserts E vs EB vs CB")).or(
                QReportDefinition.reportDefinition.name.eq("ViZix Upserts by StarFlex")).or(
                QReportDefinition.reportDefinition.name.eq("ViZix Queues Detailed")).or(
                QReportDefinition.reportDefinition.name.eq("ViZix Queues Summary")).or(
                QReportDefinition.reportDefinition.name.eq("ViZix Indexes"));

        List<ReportDefinition> lstReportDefinition= ReportDefinitionService.getInstance().listPaginated(be, null, null);

        if ( (lstReportDefinition != null) && (!lstReportDefinition.isEmpty())) {
            for(ReportDefinition repDef: lstReportDefinition ) {
                repDef.setGroup(rootGroup);
                ReportDefinitionService.getInstance().update(repDef);
            }
        }
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
