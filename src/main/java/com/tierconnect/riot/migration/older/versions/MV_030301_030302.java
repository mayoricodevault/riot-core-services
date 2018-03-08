package com.tierconnect.riot.migration.older.versions;

import com.tierconnect.riot.migration.older.V_030301_030302;

import java.util.Collections;
import java.util.List;

/**
 * Created by rsejas on 9/1/16.
 */
@Deprecated
public class MV_030301_030302 extends MigrationVersionStepOld {
    Integer currentVersion = 30301;
    Integer targetVersion = 30302;

    public MV_030301_030302() {
        fillMigrationSteps();
    }

    @Override
    public List<Integer> getFromVersions() {
        return Collections.singletonList(30301);
    }

    @Override
    public Integer getToVersion() {
        return 30302;
    }

    @Override
    public void fillMigrationSteps() {
        addMigrationStep(new V_030301_030302());
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
