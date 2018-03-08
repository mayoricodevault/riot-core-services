package com.tierconnect.riot.migration.older.versions;

import com.tierconnect.riot.migration.*;
import com.tierconnect.riot.migration.older.V_040305_040400;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.List;

/**
 * Created by rsejas on 9/1/16.
 */
@Deprecated
public class MV_040305_040400 extends MigrationVersionStepOld {
    private static Logger logger = Logger.getLogger(MV_040305_040400.class);
    int currentVersion = 40305;
    int targetVersion = 40400;
    int targetRelease = 40400;
    int dbVersion = 0;

    public MV_040305_040400() {
        fillMigrationSteps();
        dbVersion = getAndValidateDBVersion();
    }

    private int getAndValidateDBVersion() {
        DBHelper dbHelper = new DBHelper();
        int dbVersion1 = dbHelper.getDBVersionTableCurrentVersion();
        if (dbVersion1 < 4040000) {
            return currentVersion;
        }
        return dbVersion1;
    }

    @Override
    public List<Integer> getFromVersions() {
        return Collections.singletonList(currentVersion);
    }

    @Override
    public Integer getToVersion() {
        return targetVersion;
    }

    @Override
    public void fillMigrationSteps() {
        addMigrationStep(new V_040305_040400()); // RC: 1
    }

    @Override
    public void migrateReleaseCandidatesSQLBefore() throws Exception {
        int tmpCurrentVersion = dbVersion;
        if (tmpCurrentVersion < targetRelease) {
            migrateByDefaultSQLBefore(tmpCurrentVersion, targetRelease);
        } else {
            logger.info("Migration from [" + currentVersion + "] to [" + targetVersion + "] is already updated.");
        }
    }

    @Override
    public void migrateReleaseCandidatesSQLHibernate() throws Exception {
        int tmpCurrentVersion = dbVersion;
        if (tmpCurrentVersion < targetRelease) {
            migrateByDefaultSQLHibernate(tmpCurrentVersion, targetRelease);
        } else {
            logger.info("Migration from [" + currentVersion + "] to [" + targetVersion + "] is already updated.");
        }
    }

    @Override
    public void migrateReleaseCandidatesSQLAfter() throws Exception {
        int tmpCurrentVersion = dbVersion;
        if (tmpCurrentVersion < targetRelease) {
            migrateByDefaultSQLAfter(tmpCurrentVersion, targetRelease);
        } else {
            logger.info("Migration from ["+currentVersion+"] to ["+targetVersion+"] is already updated.");
        }
    }


}
