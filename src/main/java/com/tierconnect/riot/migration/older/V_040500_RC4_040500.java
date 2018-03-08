package com.tierconnect.riot.migration.older;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;
import com.tierconnect.riot.appcore.services.GroupService;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.List;

/**
 * Created by rsejas on 12/22/16.
 */
@Deprecated
public class V_040500_RC4_040500 implements MigrationStepOld {
    static Logger logger = Logger.getLogger(V_040500_RC4_040500.class);

    @Override
    public List<Integer> getFromVersions() {
        return Collections.singletonList(4050004);
    }

    @Override
    public int getToVersion() {
        return 40500;
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
