package com.tierconnect.riot.migration.older.versions;

import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.older.MigrationStepOld;
import com.tierconnect.riot.migration.older.V_040300_040301;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.List;

/**
 * Created by rsejas on 9/1/16.
 */
@Deprecated
public class MV_040300_040301 extends MigrationVersionStepOld {
    private static Logger logger = Logger.getLogger(MV_040300_040301.class);
    int currentVersion = 40300;
    int targetVersion = 40301;
    int targetRelease = 40301;
    int dbVersion = 0;

    public MV_040300_040301() {
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
//    /**
//     * This method validate a version and check if version 4.3.0 version was migrated with RCs
//     * @param dbVersion Version saved on DataBase
//     * @return 4020001 if was not migrated RCs, 40300 if is not necessary to migrate.
//     */
//    private int getAndValidateVersion(int dbVersion) {
//        if (dbVersion == 40300) {
//            DBHelper dbHelper = new DBHelper();
//            int maxVersion = dbHelper.getMaxDBVersionTable();
//            if (!(maxVersion > dbVersion)) {
//                return 4030000;
//            }
//        }
//        return dbVersion;
//    }

    @Override
    public List<Integer> getFromVersions() {
        return Collections.singletonList(40300);
    }

    @Override
    public Integer getToVersion() {
        return targetVersion;
    }

    @Override
    public void fillMigrationSteps() {
        MigrationStepOld migrationStep = new V_040300_040301();
        currentVersion = migrationStep.getFromVersions().get(0);
        addMigrationStep(migrationStep); // RC: 1
    }

    @Override
    public void migrateReleaseCandidatesSQLBefore() throws Exception {
        int tmpCurrentVersion = ((dbVersion>currentVersion)?dbVersion:currentVersion);
        if (tmpCurrentVersion < targetRelease) {
            migrateByDefaultSQLBefore(tmpCurrentVersion, targetRelease);
        } else {
            logger.info("Migration from [" + currentVersion + "] to [" + targetVersion + "] is already updated.");
        }
    }

    @Override
    public void migrateReleaseCandidatesSQLHibernate() throws Exception {
        int tmpCurrentVersion = ((dbVersion>currentVersion)?dbVersion:currentVersion);
        if (tmpCurrentVersion < targetRelease) {
            migrateByDefaultSQLHibernate(tmpCurrentVersion, targetRelease);
        } else {
            logger.info("Migration from [" + currentVersion + "] to [" + targetVersion + "] is already updated.");
        }
    }

    @Override
    public void migrateReleaseCandidatesSQLAfter() throws Exception {
        int tmpCurrentVersion = ((dbVersion>currentVersion)?dbVersion:currentVersion);
        if (tmpCurrentVersion < targetRelease) {
            migrateByDefaultSQLAfter(tmpCurrentVersion, targetRelease);
        } else {
            logger.info("Migration from ["+currentVersion+"] to ["+targetVersion+"] is already updated.");
        }
    }


}
