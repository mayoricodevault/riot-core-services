package com.tierconnect.riot.migration.older.versions;

import com.tierconnect.riot.migration.older.V_040000_040100;

import java.util.Collections;
import java.util.List;

/**
 * Created by rsejas on 9/1/16.
 */
@Deprecated
public class MV_040000_040100 extends MigrationVersionStepOld {
    Integer currentVersion = 40000;
    Integer targetVersion = 40100;

    public MV_040000_040100() {
        fillMigrationSteps();
    }

    @Override
    public List<Integer> getFromVersions() {
        return Collections.singletonList(40000);
    }

    @Override
    public Integer getToVersion() {
        return 40100;
    }

    @Override
    public void fillMigrationSteps() {
        addMigrationStep(new V_040000_040100());
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
