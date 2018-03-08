package com.tierconnect.riot.migration.steps.edgebox;

import com.tierconnect.riot.iot.entities.Parameters;
import com.tierconnect.riot.iot.services.ParametersService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;


public class Migrate_RemoveParamsForSmedBridge_VIZIX5137 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_RemoveParamsForSmedBridge_VIZIX5137.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        removeParamsForSmedBridge();
    }

    private void removeParamsForSmedBridge() {
        logger.info("Removing SMED Bridge type");
        ParametersService service = ParametersService.getInstance();

        String bridgeType = "BRIDGE_TYPE";
        String code = "smed";

        Parameters parameters = service.getByCategoryAndCode(bridgeType, code);
        if(parameters != null) {
            logger.info("Removing SMED Bridge parameters...");
            service.delete(parameters);
            logger.info("SMED Bridge parameters removed successfully");
        } else {
            logger.info("SMED Bridge parameters not found");
        }
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }
}
