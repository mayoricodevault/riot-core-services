package com.tierconnect.riot.migration.older;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.services.EdgeboxService;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.List;

/**
 * Created by cfernandez
 * 6/24/15.
 */
@Deprecated
public class V_020305_020306 implements MigrationStepOld
{

    private Logger logger = Logger.getLogger(V_020305_020306.class);

    @Override
    public List<Integer> getFromVersions() {
        return Arrays.asList(20305);
    }

    @Override
    public int getToVersion() {
        return 20306;
    }

    @Override
    public void migrateSQLBefore() throws Exception {

    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateCoreBridge();
    }

    @Override
    public void migrateSQLAfter() throws Exception {

    }

    private void migrateCoreBridge()
    {
        logger.info("Migrating coreBridge configuration...");

        EdgeboxService edgeboxService  = EdgeboxService.getInstance();
        Edgebox edgebox = edgeboxService.selectByCode("MCB");
        String coreBridgeConfig = edgebox.getConfiguration();

        if (!coreBridgeConfig.contains("mongo"))
        {
            ObjectMapper mapper = new ObjectMapper();
            try
            {
                JsonNode rootNode = mapper.readTree(coreBridgeConfig);

                ObjectNode mongoNode = mapper.createObjectNode();
                mongoNode.put("active", 1);
                mongoNode.put("host", "localhost");
                mongoNode.put("port", 27017);
                mongoNode.put("dbname", "riot_main");

                ((ObjectNode) rootNode).put("mongo", mongoNode);

                coreBridgeConfig = rootNode.toString();
                edgebox.setConfiguration(coreBridgeConfig);
                edgeboxService.update(edgebox);

                logger.info("Updated coreBridge configuration, added mongo default configuration");
            }
            catch (Exception e) {
                logger.error(e);
            }
        }
    }
}
