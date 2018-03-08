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
public class Migrate_ZoneMapReports_RIOT4108 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_ZoneMapReports_RIOT4108.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
        CassandraUtils.init(Configuration.getProperty("cassandra.host"), Configuration.getProperty("cassandra" +
                ".keyspace"));
        // Creating zone_count table in Cassandra
        logger.info("Executing CREATE INDEX IF NOT EXISTS field_value_value_idx ON riot_main.field_value (value);");
        CassandraUtils.getSession().execute(CassandraUtils.getSession().prepare("CREATE INDEX IF NOT EXISTS " +
                "field_value_value_idx ON riot_main.field_value (value);").bind());

        // Creating zone_count table in Cassandra
        logger.info("Executing CREATE IF NOT EXISTS TABLE zone_count ( zone_name text, things_quantity bigint, " +
                "PRIMARY KEY(zone_name);");
        CassandraUtils.getSession().execute(CassandraUtils.getSession().prepare("CREATE TABLE IF NOT EXISTS " +
                "zone_count ( zone_name text, things_quantity bigint, PRIMARY KEY(zone_name));").bind());
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateFeature();
    }

    private void migrateFeature() {
        //TODO rename this function and put your code here
    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }
}
