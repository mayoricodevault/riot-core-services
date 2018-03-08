package com.tierconnect.riot.migration.older.versions;

import com.tierconnect.riot.migration.older.V_020400_020401;

import java.util.Collections;
import java.util.List;

/**
 * Created by rsejas on 9/1/16.
 */
@Deprecated
public class MV_020400_020401 extends MigrationVersionStepOld {
    Integer currentVersion = 20400;
    Integer targetVersion = 20401;

    public MV_020400_020401() {
        fillMigrationSteps();
    }

    @Override
    public List<Integer> getFromVersions() {
        return Collections.singletonList(20400);
    }

    @Override
    public Integer getToVersion() {
        return 20401;
    }

    @Override
    public void fillMigrationSteps() {
        addMigrationStep(new V_020400_020401());
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
