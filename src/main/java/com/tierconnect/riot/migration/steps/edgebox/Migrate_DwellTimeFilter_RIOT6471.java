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
public class Migrate_DwellTimeFilter_RIOT6471 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_DwellTimeFilter_RIOT6471.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
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
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
