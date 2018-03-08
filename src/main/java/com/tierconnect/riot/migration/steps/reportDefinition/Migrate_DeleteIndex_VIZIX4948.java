package com.tierconnect.riot.migration.steps.reportDefinition;

import com.tierconnect.riot.appcore.popdb.PopDBRequired;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

/**
 * Created by rsejas on 2/10/17.
 */
public class Migrate_DeleteIndex_VIZIX4948 implements MigrationStep {
    Logger logger = Logger.getLogger(Migrate_DeleteIndex_VIZIX4948.class);
    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {

    }

    @Override
    public void migrateHibernate() throws Exception {
        PopDBRequired.addAutoIndexDeleteIndex();
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
    }
}
