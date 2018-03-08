package com.tierconnect.riot.migration.older;

import com.tierconnect.riot.appcore.dao.CassandraUtils;
import com.tierconnect.riot.appcore.utils.Configuration;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.List;

/**
 * Created by agutierrez on 7/6/15.
 */
@Deprecated
public class V_0203xx_020400 implements MigrationStepOld {
    private Logger logger = Logger.getLogger(V_020305_020306.class);

    @Override
    public List<Integer> getFromVersions() {
        return Arrays.asList(20306);
    }

    @Override
    public int getToVersion() {
        return 20400;
    }

    @Override
    public void migrateSQLBefore() throws Exception {

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
    public void migrateHibernate() throws Exception {

    }

    @Override
    public void migrateSQLAfter() throws Exception {

    }


}
