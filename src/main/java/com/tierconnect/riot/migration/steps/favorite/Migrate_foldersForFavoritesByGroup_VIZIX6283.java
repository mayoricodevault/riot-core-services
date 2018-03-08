package com.tierconnect.riot.migration.steps.favorite;

import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

/**
 * Created by cess on 7/10/17.
 */
public class Migrate_foldersForFavoritesByGroup_VIZIX6283 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_foldersForFavoritesByGroup_VIZIX6283.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {

    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }
}
