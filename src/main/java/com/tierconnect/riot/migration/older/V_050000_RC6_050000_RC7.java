package com.tierconnect.riot.migration.older;

import com.tierconnect.riot.migration.DBHelper;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.List;

/**
 * Created by vealaro on 4/11/17.
 */
@Deprecated
public class V_050000_RC6_050000_RC7 implements MigrationStepOld {

    private static Logger logger = Logger.getLogger(V_050000_RC6_050000_RC7.class);

    @Override
    public List<Integer> getFromVersions() {
        return Collections.singletonList(5000006);
    }

    @Override
    public int getToVersion() {
        return 5000007;
    }

    @Override
    public void migrateSQLBefore() throws Exception {
        logger.info("Migrating from: " + getFromVersions() + " To: " + getToVersion());
        DBHelper dbHelper = new DBHelper();
        String databaseType = DBHelper.getDataBaseType();
        if(!dbHelper.existTable("actionconfiguration")) {
            dbHelper.executeSQLFile("sql/" + databaseType + "/V050000_RC6_to_050000_RC7.sql");
        }

    }

    @Override
    public void migrateHibernate() throws Exception {

    }

    @Override
    public void migrateSQLAfter() throws Exception {

    }
}
