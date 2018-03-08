package com.tierconnect.riot.migration.older;

import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 *
 * @author : rchirinos
 * @date : 10/27/16 3:44 PM
 * @version:
 */
@Deprecated
public class V_040302_040303 implements MigrationStepOld {
    Logger logger = Logger.getLogger(V_040302_040303.class);

    @Override
    public List<Integer> getFromVersions() {
        return Collections.singletonList(40302);
    }

    @Override
    public int getToVersion() {
        return 40303;
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
