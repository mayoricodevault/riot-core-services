package com.tierconnect.riot.migration.older;

import com.tierconnect.riot.appcore.dao.CassandraUtils;
import com.tierconnect.riot.appcore.utils.Configuration;
import com.tierconnect.riot.migration.DBHelper;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.List;

/**
 * Created by agutierrez on 4/22/15.
 */
@Deprecated
public class V_020200_020300 implements MigrationStepOld {

    private Logger logger = Logger.getLogger(V_020200_020300.class);

    @Override
    public List<Integer> getFromVersions() {
        return Arrays.asList(20200);
    }

    @Override
    public int getToVersion() {
        return 20300;
    }

    @Override
    public void migrateSQLBefore() throws Exception {
        DBHelper dbHelper = new DBHelper();
        String databaseType = dbHelper.getDataBaseType();
        CassandraUtils.init(Configuration.getProperty("cassandra.host"), Configuration.getProperty("cassandra.keyspace"));
        dbHelper.executeSQLFile("sql/" + databaseType + "/V020200_to_020300.sql");
        logger.info("Executing CREATE TABLE IF NOT EXISTS field_type (\n" +
                "  field_type_id bigint,             //field type id\n" +
                "  thing_id bigint,                  //thing id\n" +
                "  value    text,                    //value\n" +
                "  time     timestamp,               //field_id value\n" +
                "  PRIMARY KEY(field_type_id, thing_id)\n" +
                ");");
        CassandraUtils.getSession().execute(CassandraUtils.getSession().prepare("CREATE TABLE IF NOT EXISTS field_type (\n" +
                "  field_type_id bigint,             //field type id\n" +
                "  thing_id bigint,                  //thing id\n" +
                "  value    text,                    //value\n" +
                "  time     timestamp,               //field_id value\n" +
                "  PRIMARY KEY(field_type_id, thing_id)\n" +
                ");").bind());

        logger.info("Executing CREATE INDEX IF NOT EXISTS thing_id_idx ON field_type( thing_id );");
        CassandraUtils.getSession().execute(CassandraUtils.getSession().
                prepare("CREATE INDEX IF NOT EXISTS thing_id_idx ON field_type( thing_id );").bind());

        logger.info("Executing CREATE INDEX IF NOT EXISTS value_idx ON field_type( value );");
        CassandraUtils.getSession().execute(CassandraUtils.getSession().
                prepare("CREATE INDEX IF NOT EXISTS value_idx ON field_type( value );").bind());
        CassandraUtils.shutdown();
    }

    @Override
    public void migrateHibernate() throws Exception {

    }

    @Override
    public void migrateSQLAfter() throws Exception {

    }

}
