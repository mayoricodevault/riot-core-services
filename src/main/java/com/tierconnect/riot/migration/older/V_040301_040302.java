package com.tierconnect.riot.migration.older;

import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.List;

/**
 * Created by fflores on 9/29/16.
 */
@Deprecated
public class V_040301_040302 implements MigrationStepOld {

    private static final Logger logger = Logger.getLogger(V_040301_040302.class);

    @Override
    public List<Integer> getFromVersions() {
        return Collections.singletonList(40301);
    }

    @Override
    public int getToVersion() {
        return 40302;
    }

    @Override
    public void migrateSQLBefore() throws Exception {
        logger.info("Migrating from: " + getFromVersions() + " To: " + getToVersion());
    }

    @Override
    public void migrateHibernate() throws Exception {

    }

    @Override
    public void migrateSQLAfter() throws Exception {
    }

}
