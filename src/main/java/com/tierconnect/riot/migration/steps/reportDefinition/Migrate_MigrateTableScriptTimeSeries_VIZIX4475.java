package com.tierconnect.riot.migration.steps.reportDefinition;

import com.tierconnect.riot.iot.dao.ReportDefinitionConfigDAO;
import com.tierconnect.riot.iot.dao.mongo.MongoScriptDAO;
import com.tierconnect.riot.iot.entities.ReportDefinition;
import com.tierconnect.riot.iot.entities.ReportDefinitionConfig;
import com.tierconnect.riot.iot.services.ReportDefinitionService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import com.tierconnect.riot.sdk.dao.MongoExecutionException;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
@SuppressWarnings("unused")
public class Migrate_MigrateTableScriptTimeSeries_VIZIX4475 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_MigrateTableScriptTimeSeries_VIZIX4475.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateMongoScripts();
    }

    private void migrateMongoScripts() throws MongoExecutionException, NonUniqueResultException {
        List<ReportDefinition> reportDefinitionList = ReportDefinitionService.getInstance().getByReportType("mongo");
        for (ReportDefinition item : reportDefinitionList) {
            if (item.getName().contains("ViZix TimeSeries")) {
                ReportDefinitionConfig repDefConfigMongoScript = item.getReportDefConfigItem("SCRIPT");
                repDefConfigMongoScript.setKeyValue(repDefConfigMongoScript.getKeyValue().replace("table.labelY = " +
                        "\"Time " +
                        "(UTC-\" + utcOffset + \")\";", "table.labelY = \"Time (UTC\" + ((utcOffset >= 0)? " +
                        "\"+\":\"\") + " +
                        "utcOffset + \")\";"));
                ReportDefinitionConfigDAO reportDefinitionConfigDAO = new ReportDefinitionConfigDAO();
                reportDefinitionConfigDAO.update(repDefConfigMongoScript);
                MongoScriptDAO.getInstance().update(item.getId().toString(), repDefConfigMongoScript.getKeyValue());
            }
        }
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }
}
