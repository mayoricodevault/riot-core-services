package com.tierconnect.riot.migration.older.versions;

import com.tierconnect.riot.migration.*;
import com.tierconnect.riot.migration.older.*;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.List;

/**
 * Created by rsejas on 9/1/16.
 */
@Deprecated
public class MV_040200_040300 extends MigrationVersionStepOld {
    private static Logger logger = Logger.getLogger(MV_040200_040300.class);
    int currentVersion = 40200;
    int targetVersion = 40300;
    int targetRelease = 40300; // Release Candidate Target
    int dbVersion = 0;

    public MV_040200_040300() {
        fillMigrationSteps();
        dbVersion = getAndValidateVersion(getDBVersion());
    }

    /**
     * This method validate a version and check if version 4.3.0 version was migrated with RCs
     * @param dbVersion Version saved on DataBase
     * @return 4020001 if was not migrated RCs 40300 if is not necessary to migrate.
     */
    private int getAndValidateVersion(int dbVersion) {
        if (dbVersion == 40300) {
            DBHelper dbHelper = new DBHelper();
            int maxVersion = dbHelper.getMaxDBVersionTable();
            if (!(maxVersion > dbVersion)) {
                return 4030000;
            }
        }
        return dbVersion;
    }

    @Override
    public List<Integer> getFromVersions() {
        return Collections.singletonList(40200);
    }

    @Override
    public Integer getToVersion() {
        return targetVersion;
    }

    @Override
    public void fillMigrationSteps() {
        addMigrationStep(new V_040200_040300()); // RC: 1
        addMigrationStep(new V_040300_RC10_040300_RC11()); // RC: 11
        addMigrationStep(new V_040300_RC11_040300_RC12()); // RC: 12
        addMigrationStep(new V_040300_RC12_040300_RC13()); // RC: 13
        addMigrationStep(new V_040300_RC13_040300());       // release/4.3.0 (stable)
    }

    @Override
    public void migrateReleaseCandidatesSQLBefore() throws Exception {
        int tmpCurrentVersion = ((dbVersion>currentVersion)?dbVersion:currentVersion);
        if (tmpCurrentVersion != targetRelease) {
            migrateByDefaultSQLBefore(tmpCurrentVersion, targetRelease);
        } else {
            logger.info("Migration from [" + currentVersion + "] to [" + targetVersion + "] is already updated.");
        }
    }

    @Override
    public void migrateReleaseCandidatesSQLHibernate() throws Exception {
        int tmpCurrentVersion = ((dbVersion>currentVersion)?dbVersion:currentVersion);
        if (tmpCurrentVersion != targetRelease) {
            migrateByDefaultSQLHibernate(tmpCurrentVersion, targetRelease);
        } else {
            logger.info("Migration from [" + currentVersion + "] to [" + targetVersion + "] is already updated.");
        }
    }

    private int getDBVersion() {
        int version = 0;
        DBHelper helper = new DBHelper();
        if (helper.existTable("version") || helper.existTable("Version")) {
            version = helper.getDBVersionTableCurrentVersion();
            if (version == 0) {
                logger.error("-----**DataBase is in a invalid state\n-----**Version table doesn't contain a version, revert to backup and old application version or set the version manually i.e: 'insert into Version (id, dbVersion, installTime) values (1, version, null);'");
                throw new RuntimeException("DataBase is in a invalid state, Version table doesn't contain a version");
            }
        } else {
            logger.error("DataBase is in a invalid state, Version table doesn't exist");
            throw new RuntimeException("DataBase is in a invalid state, Version table doesn't exist");
        }
        return version;
    }

    @Override
    public void migrateReleaseCandidatesSQLAfter() throws Exception {
        int tmpCurrentVersion = ((dbVersion>currentVersion)?dbVersion:currentVersion);
        if (tmpCurrentVersion != targetRelease) {
            migrateByDefaultSQLAfter(tmpCurrentVersion, targetRelease);
        } else {
            logger.info("Migration from ["+currentVersion+"] to ["+targetVersion+"] is already updated.");
        }
    }
}
