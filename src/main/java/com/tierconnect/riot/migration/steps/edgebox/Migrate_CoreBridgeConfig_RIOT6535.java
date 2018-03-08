package com.tierconnect.riot.migration.steps.edgebox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.popdb.PopDBRequired;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.services.EdgeboxService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_CoreBridgeConfig_RIOT6535 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_CoreBridgeConfig_RIOT6535.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateCoreBridgeConfiguration();
    }

    private void migrateCoreBridgeConfiguration()
    {
        logger.info("Migrating coreBridge configuration...");

        EdgeboxService edgeboxService  = EdgeboxService.getInstance();
        Edgebox edgebox = edgeboxService.selectByCode("MCB");
        String coreBridgeConfig = edgebox.getConfiguration();

        if (coreBridgeConfig.contains("cassandra"))
        {
            ObjectMapper mapper = new ObjectMapper();
            try
            {
                JsonNode rootNode = mapper.readTree(coreBridgeConfig);
                ((ObjectNode) rootNode).remove("cassandra");

                coreBridgeConfig = rootNode.toString();
                edgebox.setConfiguration(coreBridgeConfig);
                edgeboxService.update(edgebox);

                logger.info("Updated coreBridge configuration, removed cassandra configuration node.");
            }
            catch (Exception e) {
                logger.error(e);
            }
        }
    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
