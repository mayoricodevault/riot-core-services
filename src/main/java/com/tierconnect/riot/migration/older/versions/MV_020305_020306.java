package com.tierconnect.riot.migration.older.versions;

import com.tierconnect.riot.migration.older.V_020305_020306;

import java.util.Collections;
import java.util.List;

/**
 * Created by rsejas on 9/1/16.
 */
@Deprecated
public class MV_020305_020306 extends MigrationVersionStepOld {
    Integer currentVersion = 20305;
    Integer targetVersion = 20306;

    public MV_020305_020306() {
        fillMigrationSteps();
    }

    @Override
    public List<Integer> getFromVersions() {
        return Collections.singletonList(20305);
    }

    @Override
    public Integer getToVersion() {
        return 20306;
    }

    @Override
    public void fillMigrationSteps() {
        addMigrationStep(new V_020305_020306());
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
