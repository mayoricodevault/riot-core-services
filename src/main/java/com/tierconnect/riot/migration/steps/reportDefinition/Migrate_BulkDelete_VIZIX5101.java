package com.tierconnect.riot.migration.steps.reportDefinition;

import com.tierconnect.riot.appcore.entities.Field;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.GroupField;
import com.tierconnect.riot.appcore.services.FieldService;
import com.tierconnect.riot.appcore.services.GroupFieldService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

/**
 * Created by rchirinos
 * on 25/05/17
 */
public class Migrate_BulkDelete_VIZIX5101 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_BulkDelete_VIZIX5101.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {

    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
    }

}
