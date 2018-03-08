package com.tierconnect.riot.migration.older.versions;

import com.tierconnect.riot.migration.older.V_020102_020200;

import java.util.Arrays;
import java.util.List;

/**
 * Created by rsejas on 9/1/16.
 */
@Deprecated
public class MV_020102_020200 extends MigrationVersionStepOld {
    Integer currentVersion = 20102;
    Integer targetVersion = 20200;

    public MV_020102_020200() {
        fillMigrationSteps();
    }

    @Override
    public List<Integer> getFromVersions() {
        return Arrays.asList(20102, 20103);
    }

    @Override
    public Integer getToVersion() {
        return 20200;
    }

    @Override
    public void fillMigrationSteps() {
        addMigrationStep(new V_020102_020200());
    }

    @Override
    public void migrateReleaseCandidatesSQLBefore() throws Exception {
        migrateByDefaultSQLBefore(currentVersion, targetVersion);
    }

    @Override
    public void migrateReleaseCandidatesSQLHibernate() throws Exception {
        migrateByDefaultSQLHibernate(currentVersion, targetVersion);
    }

    @Override
    public void migrateReleaseCandidatesSQLAfter() throws Exception {
        migrateByDefaultSQLAfter(currentVersion, targetVersion);
    }
}
