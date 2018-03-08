package com.tierconnect.riot.migration.older.versions;

import com.tierconnect.riot.migration.older.V_030200_030201;

import java.util.Collections;
import java.util.List;

/**
 * Created by rsejas on 9/1/16.
 */
@Deprecated
public class MV_030200_030201 extends MigrationVersionStepOld {
    Integer currentVersion = 30200;
    Integer targetVersion = 30201;

    public MV_030200_030201() {
        fillMigrationSteps();
    }

    @Override
    public List<Integer> getFromVersions() {
        return Collections.singletonList(30200);
    }

    @Override
    public Integer getToVersion() {
        return 30201;
    }

    @Override
    public void fillMigrationSteps() {
        addMigrationStep(new V_030200_030201());
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
