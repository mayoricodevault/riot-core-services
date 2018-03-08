package com.tierconnect.riot.migration.older.versions;

import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.older.V_040500_050000_RC1;
import com.tierconnect.riot.migration.older.V_050000_RC1_050000_RC2;
import com.tierconnect.riot.migration.older.V_050000_RC2_050000_RC3;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.List;

/**
 * Created by rsejas on 12/28/16.
 */
@Deprecated
public class MV_040500_050000 extends MigrationVersionStepOld {
    private static Logger logger = Logger.getLogger(MV_040500_050000.class);
    int currentVersion = 40500;
    int targetVersion = 50000;
    int targetRelease = 5000003;
    int dbVersion = 0;

    public MV_040500_050000() {
        fillMigrationSteps();
        dbVersion = getAndValidateDBVersion();
    }

    private int getAndValidateDBVersion() {
        DBHelper dbHelper = new DBHelper();
        int dbVersion1 = dbHelper.getDBVersionTableCurrentVersion();
        if (dbVersion1 < 5000001) {
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
        addMigrationStep(new V_040500_050000_RC1()); // RC1
        addMigrationStep(new V_050000_RC1_050000_RC2()); // RC2
        addMigrationStep(new V_050000_RC2_050000_RC3()); // RC3
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
