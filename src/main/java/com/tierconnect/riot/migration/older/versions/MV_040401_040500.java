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
public class MV_040401_040500 extends MigrationVersionStepOld {
    private static Logger logger = Logger.getLogger(MV_040401_040500.class);
    int currentVersion = 40401;
    int targetVersion = 40500;
    int targetRelease = 40500;
    int dbVersion = 0;

    public MV_040401_040500() {
        fillMigrationSteps();
        dbVersion = getAndValidateDBVersion();
    }

    private int getAndValidateDBVersion() {
        DBHelper dbHelper = new DBHelper();
        dbVersion = dbHelper.getDBVersionTableCurrentVersion();
        if (dbVersion == 40500) {
            int maxVersion = dbHelper.getMaxDBVersionTable();
            if (!(maxVersion > dbVersion)) {
                return 4050000;
            }
        }
        if (dbVersion < 4050001) {
            return currentVersion;
        }
        return dbVersion;
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
        addMigrationStep(new V_040401_040500_RC1()); // RC: 1
        addMigrationStep(new V_040500_RC1_040500_RC2()); // RC: 2
        addMigrationStep(new V_040500_RC2_040500_RC3()); // RC: 3
        addMigrationStep(new V_040500_RC3_040500_RC4()); // RC: 4
        addMigrationStep(new V_040500_RC4_040500()); // 4.5.0
    }

    @Override
    public void migrateReleaseCandidatesSQLBefore() throws Exception {
        int tmpCurrentVersion = dbVersion;
        if (tmpCurrentVersion != targetRelease) {
            migrateByDefaultSQLBefore(tmpCurrentVersion, targetRelease);
        } else {
            logger.info("Migration from [" + currentVersion + "] to [" + targetVersion + "] is already updated.");
        }
    }

    @Override
    public void migrateReleaseCandidatesSQLHibernate() throws Exception {
        int tmpCurrentVersion = dbVersion;
        if (tmpCurrentVersion != targetRelease) {
            migrateByDefaultSQLHibernate(tmpCurrentVersion, targetRelease);
        } else {
            logger.info("Migration from [" + currentVersion + "] to [" + targetVersion + "] is already updated.");
        }
    }

    @Override
    public void migrateReleaseCandidatesSQLAfter() throws Exception {
        int tmpCurrentVersion = dbVersion;
        if (tmpCurrentVersion != targetRelease) {
            migrateByDefaultSQLAfter(tmpCurrentVersion, targetRelease);
        } else {
            logger.info("Migration from ["+currentVersion+"] to ["+targetVersion+"] is already updated.");
        }
    }


}
