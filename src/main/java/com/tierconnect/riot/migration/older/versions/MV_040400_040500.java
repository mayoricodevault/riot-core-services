package com.tierconnect.riot.migration.older.versions;

import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.older.V_040400_040500;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.List;

/**
 * Created by rsejas on 9/1/16.
 */
public class MV_040400_040500 extends MigrationVersionStepOld {
    private static Logger logger = Logger.getLogger(MV_040400_040500.class);
    int currentVersion = 40400;
    int targetVersion = 40500;
    int targetRelease = 40500;
    int dbVersion = 0;

    public MV_040400_040500() {
        fillMigrationSteps();
        dbVersion = getAndValidateDBVersion();
    }

    private int getAndValidateDBVersion() {
        DBHelper dbHelper = new DBHelper();
        int dbVersion1 = dbHelper.getDBVersionTableCurrentVersion();
        if (dbVersion1 < 4050000) {
            return currentVersion;
        }
        return dbVersion1;
    }

    @Override
    public List<Integer> getFromVersions() {
        return Collections.singletonList(40400);
    }

    @Override
    public Integer getToVersion() {
        return targetVersion;
    }

    @Override
    public void fillMigrationSteps() {
        addMigrationStep(new V_040400_040500()); // RC: 1
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
