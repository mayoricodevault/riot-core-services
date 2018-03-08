package com.tierconnect.riot.migration.older.versions;

import com.tierconnect.riot.migration.older.V_030100_030101;

import java.util.Collections;
import java.util.List;

/**
 * Created by rsejas on 9/1/16.
 */
@Deprecated
public class MV_030100_030101 extends MigrationVersionStepOld {
    Integer currentVersion = 30100;
    Integer targetVersion = 30101;

    public MV_030100_030101() {
        fillMigrationSteps();
    }

    @Override
    public List<Integer> getFromVersions() {
        return Collections.singletonList(30100);
    }

    @Override
    public Integer getToVersion() {
        return 30101;
    }

    @Override
    public void fillMigrationSteps() {
        addMigrationStep(new V_030100_030101());
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
