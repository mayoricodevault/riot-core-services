package com.tierconnect.riot.migration.older.versions;

import com.tierconnect.riot.migration.older.V_0203xx_020400;

import java.util.Collections;
import java.util.List;

/**
 * Created by rsejas on 9/1/16.
 */
@Deprecated
public class MV_0203xx_020400 extends MigrationVersionStepOld {
    Integer currentVersion = 20306;
    Integer targetVersion = 20400;

    public MV_0203xx_020400() {
        fillMigrationSteps();
    }

    @Override
    public List<Integer> getFromVersions() {
        return Collections.singletonList(20306);
    }

    @Override
    public Integer getToVersion() {
        return 20400;
    }

    @Override
    public void fillMigrationSteps() {
        addMigrationStep(new V_0203xx_020400());
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
