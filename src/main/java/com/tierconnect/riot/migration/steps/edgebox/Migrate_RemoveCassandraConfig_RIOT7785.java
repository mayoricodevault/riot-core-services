package com.tierconnect.riot.migration.steps.edgebox;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.services.EdgeboxService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_RemoveCassandraConfig_RIOT7785 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_RemoveCassandraConfig_RIOT7785.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateRemoveCassandraConf();
    }

    private void migrateRemoveCassandraConf() {
        Edgebox edgebox = EdgeboxService.getInstance().selectByCode("MCB");
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        Map<String, Object> mapResponse = new HashMap<String, Object>();
        logger.info("Trying to remove Cassandra Configuration");
        try {
            mapResponse = mapper.readValue(edgebox.getConfiguration(), new TypeReference<Map<String, Object>>() {
            });
            if (mapResponse.containsKey("cassandra")) {
                mapResponse.remove("cassandra");
                edgebox.setConfiguration(mapper.writeValueAsString(mapResponse));
                EdgeboxService.getInstance().update(edgebox);
                logger.info("Cassandra Configuration removed");
            }
        } catch (Exception e) {
        }
    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
