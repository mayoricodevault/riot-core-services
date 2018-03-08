package com.tierconnect.riot.migration.older.versions;

import com.tierconnect.riot.migration.older.V_020301_020302;

import java.util.Collections;
import java.util.List;

/**
 * Created by rsejas on 9/1/16.
 */
@Deprecated
public class MV_020301_020302 extends MigrationVersionStepOld {
    Integer currentVersion = 20301;
    Integer targetVersion = 20302;

    public MV_020301_020302() {
        fillMigrationSteps();
    }

    @Override
    public List<Integer> getFromVersions() {
        return Collections.singletonList(20301);
    }

    @Override
    public Integer getToVersion() {
        return 20302;
    }

    @Override
    public void fillMigrationSteps() {
        addMigrationStep(new V_020301_020302());
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
