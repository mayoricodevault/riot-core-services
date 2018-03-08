package com.tierconnect.riot.migration.older.versions;

import com.tierconnect.riot.migration.older.V_030302_030303;

import java.util.Collections;
import java.util.List;

/**
 * Created by rsejas on 9/1/16.
 */
@Deprecated
public class MV_030302_030303 extends MigrationVersionStepOld {
    Integer currentVersion = 30302;
    Integer targetVersion = 30303;

    public MV_030302_030303() {
        fillMigrationSteps();
    }

    @Override
    public List<Integer> getFromVersions() {
        return Collections.singletonList(30302);
    }

    @Override
    public Integer getToVersion() {
        return 30303;
    }

    @Override
    public void fillMigrationSteps() {
        addMigrationStep(new V_030302_030303());
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
