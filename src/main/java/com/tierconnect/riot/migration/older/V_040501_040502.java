package com.tierconnect.riot.migration.older;

import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.List;

/**
 * Created by fflores on 2/10/17.
 */
@Deprecated
public class V_040501_040502 implements MigrationStepOld {
    static Logger logger = Logger.getLogger(V_040501_040502.class);

    @Override
    public List<Integer> getFromVersions() {
        return Collections.singletonList(40501);
    }

    @Override
    public int getToVersion() {
        return 40502;
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
