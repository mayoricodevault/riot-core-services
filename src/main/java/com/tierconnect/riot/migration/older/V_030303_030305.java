package com.tierconnect.riot.migration.older;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.services.EdgeboxService;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Created by cfernandez
 * on 1/18/16.
 */
@Deprecated
public class V_030303_030305 implements MigrationStepOld
{

    private Logger logger = Logger.getLogger(V_030303_030305.class);

    @Override
    public List<Integer> getFromVersions() {
        return Arrays.asList(30303);
    }

    @Override
    public int getToVersion() {
        return 30305;
    }

    @Override
    public void migrateSQLBefore() throws Exception {

    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateAleBridgeConfiguration();
    }

    private void migrateAleBridgeConfiguration()
    {
        logger.info("Migrating AleBridge configuration.");
        EdgeboxService edgeboxService  = EdgeboxService.getInstance();
        String config;
        ObjectNode zoneDwellFilter;
        JsonNode rootNode;
        for (Edgebox edgebox : edgeboxService.selectAll())
        {
            config = edgebox.getConfiguration();
            if (config.contains("timeDistanceFilter") && !config.contains("zoneDwellFilter"))
            {
                ObjectMapper mapper = new ObjectMapper();
                try
                {
                    rootNode = mapper.readTree(config);

                    zoneDwellFilter = mapper.createObjectNode();
                    zoneDwellFilter.put("active",0);
                    zoneDwellFilter.put("unlockDistance",25);
                    zoneDwellFilter.put("inZoneDistance",10);
                    zoneDwellFilter.put("zoneDwellTime",300);
                    ((ObjectNode) rootNode).put("zoneDwellFilter", zoneDwellFilter);

                    config = rootNode.toString();
                    edgebox.setConfiguration(config);
                    edgeboxService.update(edgebox);
                    logger.info("Updated Edge Bridge with code: " + edgebox.getCode());

                } catch (IOException e) {
                    logger.error(e);
                }
            }
        }
    }

    @Override
    public void migrateSQLAfter() throws Exception {

    }
}
