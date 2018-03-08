package com.tierconnect.riot.migration.older;

import com.tierconnect.riot.migration.DBHelper;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.List;

/**
 * Created by cfernandez
 * 6/24/15.
 */
@Deprecated
public class V_020400_020401 implements MigrationStepOld
{

    private Logger logger = Logger.getLogger(V_020400_020401.class);

    @Override
    public List<Integer> getFromVersions() {
        return Arrays.asList(20400);
    }

    @Override
    public int getToVersion() {
        return 20401;
    }

    @Override
    public void migrateSQLBefore() throws Exception {
        DBHelper dbHelper = new DBHelper();
        String databaseType = dbHelper.getDataBaseType();
        dbHelper.executeSQLFile("sql/" + databaseType + "/V020400_020401.sql");
    }

    @Override
    public void migrateHibernate() throws Exception {

    }

    @Override
    public void migrateSQLAfter() throws Exception {

    }


}
