package com.tierconnect.riot.migration.steps.edgebox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.services.EdgeboxService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

import java.io.IOException;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_CoreBridgeConfig_RIOT8370 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_CoreBridgeConfig_RIOT8370.class);

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
        logger.info("Migrating CoreBridge configuration...");
        EdgeboxService edgeboxService  = EdgeboxService.getInstance();
        Edgebox edgebox = edgeboxService.selectByCode("MCB");
        String coreBridgeConfig = edgebox.getConfiguration();

        if (!coreBridgeConfig.contains("username"))
        {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = null;
            try {
                rootNode = mapper.readTree(coreBridgeConfig);
                JsonNode mongo = rootNode.get("mongo");
                if (mongo != null)
                {
                    ((ObjectNode) mongo).put("username", "admin");
                    ((ObjectNode) mongo).put("password", "control123!");
                    coreBridgeConfig = rootNode.toString();
                    edgebox.setConfiguration(coreBridgeConfig);
                    edgeboxService.update(edgebox);
                    logger.info("Updated CoreBridge configuration, it was added username and password");
                }else{
                    logger.error("MongoDB configuration cannot be found in CoreBridge configuration");
                }
            }
            catch (IOException e) {
                logger.error(e);
            }
        }

        if (!coreBridgeConfig.contains("sourceRule"))
        {
            ObjectMapper mapper = new ObjectMapper();
            try
            {   JsonNode rootNode = mapper.readTree(coreBridgeConfig);

                ObjectNode sourceRuleNode = mapper.createObjectNode();
                sourceRuleNode.put("active", 0);

                ((ObjectNode) rootNode).put("sourceRule", sourceRuleNode);

                coreBridgeConfig = rootNode.toString();
                edgebox.setConfiguration(coreBridgeConfig);
                edgeboxService.update(edgebox);

                logger.info("SourceRule default configuration has been added to coreBridge configuration");
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
