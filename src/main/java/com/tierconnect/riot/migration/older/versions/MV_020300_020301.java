package com.tierconnect.riot.migration.older.versions;

import com.tierconnect.riot.migration.older.V_020300_020301;

import java.util.Collections;
import java.util.List;

/**
 * Created by rsejas on 9/1/16.
 */
@Deprecated
public class MV_020300_020301 extends MigrationVersionStepOld {
    Integer currentVersion = 20300;
    Integer targetVersion = 20301;

    public MV_020300_020301() {
        fillMigrationSteps();
    }

    @Override
    public List<Integer> getFromVersions() {
        return Collections.singletonList(20300);
    }

    @Override
    public Integer getToVersion() {
        return 20301;
    }

    @Override
    public void fillMigrationSteps() {
        addMigrationStep(new V_020300_020301());
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
