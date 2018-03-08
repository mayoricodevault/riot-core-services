package com.tierconnect.riot.migration.older.versions;

import com.tierconnect.riot.migration.older.V_030300_030301;

import java.util.Collections;
import java.util.List;

/**
 * Created by rsejas on 9/1/16.
 */
@Deprecated
public class MV_030300_030301 extends MigrationVersionStepOld {
    Integer currentVersion = 30300;
    Integer targetVersion = 30301;

    public MV_030300_030301() {
        fillMigrationSteps();
    }

    @Override
    public List<Integer> getFromVersions() {
        return Collections.singletonList(30300);
    }

    @Override
    public Integer getToVersion() {
        return 30301;
    }

    @Override
    public void fillMigrationSteps() {
        addMigrationStep(new V_030300_030301());
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
