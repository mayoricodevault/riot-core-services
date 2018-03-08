package com.tierconnect.riot.migration.steps.older2;

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
public class Migrate_MigrateBridges_RIOT3999 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_MigrateBridges_RIOT3999.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateBridges();
    }

    private void migrateBridges() {
        // migrating CoreBridge configuration
        logger.info("Migrating bridges...");
        EdgeboxService edgeboxService = EdgeboxService.getInstance();
        Edgebox edgebox = edgeboxService.selectByCode("MCB");
        String coreBridgeConfig = edgebox.getConfiguration();

        if (!coreBridgeConfig.contains("shiftProperty")) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                JsonNode rootNode = mapper.readTree(coreBridgeConfig);
                JsonNode shiftZoneRuleNode = rootNode.get("shiftZoneRule");
                if (shiftZoneRuleNode != null) {
                    ((ObjectNode) shiftZoneRuleNode).put("shiftProperty", "shift");
                    ((ObjectNode) rootNode).put("shiftZoneRule", shiftZoneRuleNode);

                    coreBridgeConfig = rootNode.toString();
                    edgebox.setConfiguration(coreBridgeConfig);
                    edgeboxService.update(edgebox);
                    logger.info("Updated coreBridge configuration, added shiftProperty");
                } else {
                    logger.info("Could not find shiftZoneRule node in coreBridge configuration");
                }

            } catch (IOException e) {
                logger.warn(e);
            }
        }
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

}
