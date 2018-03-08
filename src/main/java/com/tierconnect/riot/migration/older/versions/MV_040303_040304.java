package com.tierconnect.riot.migration.older.versions;

import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.older.MigrationStepOld;
import com.tierconnect.riot.migration.older.V_040303_040304;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 *
 * @author : cvertiz
 * @date : 11/01/16
 * @version:
 */
@Deprecated
public class MV_040303_040304 extends MigrationVersionStepOld {

    private static Logger logger = Logger.getLogger(MV_040303_040304.class);
    int currentVersion = 40303;
    int targetVersion = 40304;
    int targetRelease = 40304;
    int dbVersion = 0;

    public MV_040303_040304() {
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
        return Collections.singletonList(40303);
    }

    @Override
    public Integer getToVersion() {
        return targetVersion;
    }

    @Override
    public void fillMigrationSteps() {
        MigrationStepOld migrationStep = new V_040303_040304();
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
