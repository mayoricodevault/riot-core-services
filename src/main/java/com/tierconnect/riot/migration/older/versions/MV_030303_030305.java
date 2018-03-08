package com.tierconnect.riot.migration.older.versions;

import com.tierconnect.riot.migration.older.V_030303_030305;

import java.util.Collections;
import java.util.List;

/**
 * Created by rsejas on 9/1/16.
 */
@Deprecated
public class MV_030303_030305 extends MigrationVersionStepOld {
    Integer currentVersion = 30303;
    Integer targetVersion = 30305;

    public MV_030303_030305() {
        fillMigrationSteps();
    }

    @Override
    public List<Integer> getFromVersions() {
        return Collections.singletonList(30303);
    }

    @Override
    public Integer getToVersion() {
        return 30305;
    }

    @Override
    public void fillMigrationSteps() {
        addMigrationStep(new V_030303_030305());
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
