package com.tierconnect.riot.migration.older;

import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 *
 * @author : dbascope
 * @date : 8/30/16 9:17 AM
 * @version:
 */
@Deprecated
public class V_040300_040301 implements MigrationStepOld {

    Logger logger = Logger.getLogger(V_040300_040301.class);

    @Override
    public List<Integer> getFromVersions() {
        return Arrays.asList(40300, 4030000);
    }

    @Override
    public int getToVersion() {
        return 40301;
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