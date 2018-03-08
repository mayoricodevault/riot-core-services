package com.tierconnect.riot.migration.older.versions;

import com.tierconnect.riot.migration.older.V_020302_020305;

import java.util.Arrays;
import java.util.List;

/**
 * Created by rsejas on 9/1/16.
 */
@Deprecated
public class MV_020302_020305 extends MigrationVersionStepOld {
    Integer currentVersion = 20302;
    Integer targetVersion = 20305;

    public MV_020302_020305() {
        fillMigrationSteps();
    }

    @Override
    public List<Integer> getFromVersions() {
        return Arrays.asList(20302, 20303, 20304);
    }

    @Override
    public Integer getToVersion() {
        return 20305;
    }

    @Override
    public void fillMigrationSteps() {
        addMigrationStep(new V_020302_020305());
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
