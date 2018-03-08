package com.tierconnect.riot.migration.older;

import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.List;

/**
 * Created by fflores on 9/29/16.
 */
@Deprecated
public class V_040400_040401 implements MigrationStepOld {
    static Logger logger = Logger.getLogger(V_040400_040401.class);

    @Override
    public List<Integer> getFromVersions() {
        return Collections.singletonList(40400);
    }

    @Override
    public int getToVersion() {
        return 40401;
    }

    @Override
    public void migrateSQLBefore() throws Exception {
    }

    @Override
    public void migrateHibernate() throws Exception {
    }

    @Override
    public void migrateSQLAfter() throws Exception {
    }


}