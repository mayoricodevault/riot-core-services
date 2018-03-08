package com.tierconnect.riot.migration.steps.edgebox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.services.EdgeboxService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.MigrationException;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.List;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_OutOfOrderRuleConfig_NOISSUE implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_OutOfOrderRuleConfig_NOISSUE.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateOutOfOrderRule();
    }

    private void migrateOutOfOrderRule() {

        List<Edgebox> edgeboxes = EdgeboxService.getInstance().getByType("core");
        for (Edgebox edgebox : edgeboxes) {
            if (!edgebox.getConfiguration().contains("outOfOrderRule")) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = null;
                try {
                    rootNode = mapper.readTree(edgebox.getConfiguration());
                    ObjectNode outOfOrderRule = mapper.createObjectNode();
                    outOfOrderRule.put("active", false);
                    ((ObjectNode) rootNode).set("outOfOrderRule", outOfOrderRule);

                    edgebox.setConfiguration(rootNode.toString());
                    EdgeboxService.getEdgeboxDAO().update(edgebox);
                    logger.info("Updated CoreBridge configuration, outOfOrderRule config added");

                } catch (IOException e) {
                    throw  new MigrationException("Migration step unable to run edgeBox code=" + edgebox.getCode());
                }
            }
        }
    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
