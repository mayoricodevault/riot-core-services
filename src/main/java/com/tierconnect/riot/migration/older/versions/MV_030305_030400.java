package com.tierconnect.riot.migration.older.versions;

import com.tierconnect.riot.migration.older.V_030305_030400;

import java.util.Collections;
import java.util.List;

/**
 * Created by rsejas on 9/1/16.
 */
@Deprecated
public class MV_030305_030400 extends MigrationVersionStepOld {
    Integer currentVersion = 30305;
    Integer targetVersion = 30400;

    public MV_030305_030400() {
        fillMigrationSteps();
    }

    @Override
    public List<Integer> getFromVersions() {
        return Collections.singletonList(30305);
    }

    @Override
    public Integer getToVersion() {
        return 30400;
    }

    @Override
    public void fillMigrationSteps() {
        addMigrationStep(new V_030305_030400());
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
