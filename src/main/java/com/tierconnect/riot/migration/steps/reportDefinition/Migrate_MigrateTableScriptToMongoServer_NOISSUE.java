package com.tierconnect.riot.migration.steps.reportDefinition;

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
public class Migrate_MigrateTableScriptToMongoServer_NOISSUE implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_MigrateTableScriptToMongoServer_NOISSUE.class);

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
        MongoScriptDAO.getInstance().deleteAll();
        for (ReportDefinition item :
                reportDefinitionList) {
            ReportDefinitionConfig repDefConfigMongoScript = item.getReportDefConfigItem("SCRIPT");
            MongoScriptDAO.getInstance().insert(item.getId().toString(), repDefConfigMongoScript.getKeyValue());
        }
    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
