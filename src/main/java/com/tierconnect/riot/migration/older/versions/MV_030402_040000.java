package com.tierconnect.riot.migration.older.versions;

import com.tierconnect.riot.migration.older.V_030402_040000;

import java.util.Collections;
import java.util.List;

/**
 * Created by rsejas on 9/1/16.
 */
@Deprecated
public class MV_030402_040000 extends MigrationVersionStepOld {
    Integer currentVersion = 30402;
    Integer targetVersion = 40000;

    public MV_030402_040000() {
        fillMigrationSteps();
    }

    @Override
    public List<Integer> getFromVersions() {
        return Collections.singletonList(30402);
    }

    @Override
    public Integer getToVersion() {
        return 40000;
    }

    @Override
    public void fillMigrationSteps() {
        addMigrationStep(new V_030402_040000());
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
