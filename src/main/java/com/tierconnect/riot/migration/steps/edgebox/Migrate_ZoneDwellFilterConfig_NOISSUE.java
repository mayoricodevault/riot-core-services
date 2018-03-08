package com.tierconnect.riot.migration.steps.edgebox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.entities.Parameters;
import com.tierconnect.riot.iot.entities.ThingTypeTemplate;
import com.tierconnect.riot.iot.services.EdgeboxService;
import com.tierconnect.riot.iot.services.ParametersService;
import com.tierconnect.riot.iot.services.ThingTypeTemplateService;
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
public class Migrate_ZoneDwellFilterConfig_NOISSUE implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_ZoneDwellFilterConfig_NOISSUE.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateZoneDwellFilter();
    }

    private void migrateZoneDwellFilter() {

        List<Edgebox> edgeboxes = EdgeboxService.getInstance().getByType("StarFLEX");
        for (Edgebox edgebox : edgeboxes) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode;
            try {
                if (!edgebox.getConfiguration().contains("zoneDwellFilter")) {
                    rootNode = mapper.readTree(edgebox.getConfiguration());
                    ((ObjectNode) rootNode).set("zoneDwellFilter", generateZDFMap(mapper));
                    edgebox.setConfiguration(rootNode.toString());
                    EdgeboxService.getEdgeboxDAO().update(edgebox);
                    logger.info("Updated CoreBridge configuration, zoneDwellFilter config added");
                }

                if (!edgebox.getConfiguration().contains("rateFilter")) {
                    rootNode = mapper.readTree(edgebox.getConfiguration());
                    ((ObjectNode) rootNode).set("rateFilter", generateRateFilterMap(mapper));
                    edgebox.setConfiguration(rootNode.toString());
                    EdgeboxService.getEdgeboxDAO().update(edgebox);
                    logger.info("Updated CoreBridge configuration, rateFilter config added");
                }
            } catch (IOException e) {
                throw new MigrationException("Migration step unable to run edgeBox code=" + edgebox.getCode(), e);
            }
        }

        // for thingTypeTemplate (parameters table)
        Parameters parameters = ParametersService.getInstance().getByCategoryAndCode("BRIDGE_TYPE", "StarFLEX");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode;
        try {
            rootNode = mapper.readTree(parameters.getValue());
            if (!parameters.getValue().contains("zoneDwellFilter")) {
                ((ObjectNode) rootNode).set("zoneDwellFilter", generateZDFMap(mapper));
                parameters.setValue(rootNode.toString());
                ParametersService.getParametersDAO().update(parameters);
                logger.info("Updated Parameters configuration, zoneDwellFilter config added");
            }

            if (!parameters.getValue().contains("rateFilter")) {
                ((ObjectNode) rootNode).set("rateFilter", generateRateFilterMap(mapper));
                parameters.setValue(rootNode.toString());
                ParametersService.getParametersDAO().update(parameters);
                logger.info("Updated Parameters configuration, rateFilter config added");
            }
        } catch(Exception e){
            throw new MigrationException("Migration step unable to run parameters category=" + parameters.getCategory() + " " +
                    "and code=" + parameters.getCode(), e);
        }
    }

    private ObjectNode generateZDFMap(ObjectMapper mapper){
        ObjectNode zoneDwellFilter = mapper.createObjectNode();
        zoneDwellFilter.put("lastDetectTimeActive", 1);
        zoneDwellFilter.put("active", 1);
        zoneDwellFilter.put("inZoneDistance", 0);
        zoneDwellFilter.put("lastDetectTimeWindow", 30);
        zoneDwellFilter.put("unlockDistance", 0);
        zoneDwellFilter.put("zoneDwellTime", 1);
        return zoneDwellFilter;
    }

    private ObjectNode generateRateFilterMap(ObjectMapper mapper){
        ObjectNode rateFilter = mapper.createObjectNode();
        rateFilter.put("active", 1);
        rateFilter.put("timeLimit", 5);
        return rateFilter;
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
