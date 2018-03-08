package com.tierconnect.riot.migration.older;

import com.tierconnect.riot.appcore.dao.CassandraUtils;
import com.tierconnect.riot.appcore.utils.Configuration;
import com.tierconnect.riot.migration.CQLHelper;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.List;

/**
 * Created by agutierrez on 6/15/15.
 */
@Deprecated
public class V_020302_020305 implements MigrationStepOld {

    private Logger logger = Logger.getLogger(V_020302_020305.class);

    @Override
    public List<Integer> getFromVersions() {
        return Arrays.asList(20302, 20303, 20304);
    }

    @Override
    public int getToVersion() {
        return 20305;
    }

    @Override
    public void migrateSQLBefore() throws Exception {
        CassandraUtils.init(Configuration.getProperty("cassandra.host"), Configuration.getProperty("cassandra.keyspace"));
        CQLHelper dbHelper = new CQLHelper();
        dbHelper.executeCQLFile("cql/V020302_to_020305.cql");
        //Droping table zone_count
        logger.info("Executing TRUNCATE zone_count;");
        CassandraUtils.getSession().execute(CassandraUtils.getSession().prepare("TRUNCATE zone_count;").bind());
        logger.info("Executing DROP TABLE IF EXISTS zone_count;");
        CassandraUtils.getSession().execute(CassandraUtils.getSession().prepare("DROP TABLE IF EXISTS zone_count;").bind());
        // Creating zone_count table in Cassandra
        logger.info("Executing CREATE TABLE zone_count (\n" +
                "  zone_name text,                  //zoneName\n" +
                "  things_quantity counter,          //thingsQuantity\n" +
                "  PRIMARY KEY(zone_name)\n" +
                ");");
        CassandraUtils.getSession().execute(CassandraUtils.getSession().prepare("CREATE TABLE zone_count (\n" +
                "  zone_name text,                  //zoneName\n" +
                "  things_quantity counter,          //thingsQuantity\n" +
                "  PRIMARY KEY(zone_name)\n" +
                ");").bind());
        CassandraUtils.shutdown();
    }

    @Override
    public void migrateHibernate() throws Exception {

    }

    @Override
    public void migrateSQLAfter() throws Exception {

    }
}
