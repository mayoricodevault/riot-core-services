package com.tierconnect.riot.migration.older.versions;

import com.tierconnect.riot.migration.older.V_0204xx_030000;

import java.util.Arrays;
import java.util.List;

/**
 * Created by rsejas on 9/1/16.
 */
@Deprecated
public class MV_0204xx_030000 extends MigrationVersionStepOld {
    Integer currentVersion = 20401;
    Integer targetVersion = 30000;

    public MV_0204xx_030000() {
        fillMigrationSteps();
    }

    @Override
    public List<Integer> getFromVersions() {
        return Arrays.asList(20401, 20402);
    }

    @Override
    public Integer getToVersion() {
        return 30000;
    }

    @Override
    public void fillMigrationSteps() {
        addMigrationStep(new V_0204xx_030000());
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
