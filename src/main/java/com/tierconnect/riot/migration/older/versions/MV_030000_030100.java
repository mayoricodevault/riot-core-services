package com.tierconnect.riot.migration.older.versions;

import com.tierconnect.riot.migration.older.V_030000_030100;

import java.util.Collections;
import java.util.List;

/**
 * Created by rsejas on 9/1/16.
 */
@Deprecated
public class MV_030000_030100 extends MigrationVersionStepOld {
    Integer currentVersion = 30000;
    Integer targetVersion = 30100;

    public MV_030000_030100() {
        fillMigrationSteps();
    }

    @Override
    public List<Integer> getFromVersions() {
        return Collections.singletonList(30000);
    }

    @Override
    public Integer getToVersion() {
        return 30100;
    }

    @Override
    public void fillMigrationSteps() {
        addMigrationStep(new V_030000_030100());
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
