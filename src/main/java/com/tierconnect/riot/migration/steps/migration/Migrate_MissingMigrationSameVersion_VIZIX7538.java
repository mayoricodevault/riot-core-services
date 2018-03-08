package com.tierconnect.riot.migration.steps.migration;

import com.tierconnect.riot.appcore.services.MigrationStepResultService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_MissingMigrationSameVersion_VIZIX7538 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_MissingMigrationSameVersion_VIZIX7538.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateFeature();
    }

    private void migrateFeature() {
        MigrationStepResultService
                .getMigrationStepResultDAO()
                .selectAll()
                .stream()
                .filter(migrationStepResult -> !StringUtils.isEmpty(migrationStepResult.getMigrationResult()))
                .forEach(migrationStepResult -> {
                    try{
                        String[] fields = migrationStepResult.getMigrationResult().split("Message:");
                        migrationStepResult.setMigrationResult(fields[0].replace("Result:", "").trim());
                        if(fields.length > 1 && StringUtils.isEmpty(migrationStepResult.getMessage())){
                            String[] fields2 = fields[1].split("StackTrace:");
                            migrationStepResult.setMessage(fields2[0].replace("Message:", "").trim());
                            if(fields2.length > 1 && StringUtils.isEmpty(migrationStepResult.getStackTrace())){
                                migrationStepResult.setStackTrace(fields2[1].trim());
                            }
                        }
                    }catch(Exception e){
                        logger.error("ERROR",e);
                    }
                });
    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
