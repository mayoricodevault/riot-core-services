package com.tierconnect.riot.migration.older.versions;

import com.tierconnect.riot.migration.older.V_040100_040200;

import java.util.Collections;
import java.util.List;

/**
 * Created by rsejas on 9/1/16.
 */
@Deprecated
public class MV_040100_040200 extends MigrationVersionStepOld {
    Integer currentVersion = 40100;
    Integer targetVersion = 40200;

    public MV_040100_040200() {
        fillMigrationSteps();
    }

    @Override
    public List<Integer> getFromVersions() {
        return Collections.singletonList(40100);
    }

    @Override
    public Integer getToVersion() {
        return 40200;
    }

    @Override
    public void fillMigrationSteps() {
        addMigrationStep(new V_040100_040200());
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
