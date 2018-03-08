package com.tierconnect.riot.migration.older.versions;

import com.tierconnect.riot.migration.older.V_030101_030102;

import java.util.Collections;
import java.util.List;

/**
 * Created by rsejas on 9/1/16.
 */
@Deprecated
public class MV_030101_030102 extends MigrationVersionStepOld {
    Integer currentVersion = 30101;
    Integer targetVersion = 30102;

    public MV_030101_030102() {
        fillMigrationSteps();
    }

    @Override
    public List<Integer> getFromVersions() {
        return Collections.singletonList(30101);
    }

    @Override
    public Integer getToVersion() {
        return 30102;
    }

    @Override
    public void fillMigrationSteps() {
        addMigrationStep(new V_030101_030102());
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
