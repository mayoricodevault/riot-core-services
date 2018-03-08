package com.tierconnect.riot.migration.steps.older2;

import com.tierconnect.riot.appcore.dao.CassandraUtils;
import com.tierconnect.riot.appcore.utils.Configuration;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_CassandraNewTable_RIOT4798 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_CassandraNewTable_RIOT4798.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        createCassandraTable();
    }

    private void createCassandraTable() {
        // Creating field_value_history2 table in Cassandra
        logger.info("Executing CREATE TABLE IF NOT EXISTS  field_value_history2 (\n" +
                "  field_type_id bigint,             //field type id\n" +
                "  thing_id bigint,                  //thing id\n" +
                "  time timestamp,\n" +
                "  value text,\n" +
                "  PRIMARY KEY((thing_id,field_type_id),time)\n" +
                ")  WITH gc_grace_seconds = 1;");
        CassandraUtils.init(Configuration.getProperty("cassandra.host"), Configuration.getProperty("cassandra" +
                ".keyspace"));
        CassandraUtils.getSession().execute(
                CassandraUtils.getSession().prepare(
                        "CREATE TABLE IF NOT EXISTS field_value_history2 (\n" +
                                "  field_type_id bigint,             //field type id\n" +
                                "  thing_id bigint,                  //thing id\n" +
                                "  time timestamp,\n" +
                                "  value text,\n" +
                                "  PRIMARY KEY((thing_id,field_type_id),time)\n" +
                                ")  WITH gc_grace_seconds = 1;").bind());
        CassandraUtils.shutdown();
    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
