package com.tierconnect.riot.migration.steps.edgebox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.entities.Parameters;
import com.tierconnect.riot.iot.services.EdgeboxService;
import com.tierconnect.riot.iot.services.ParametersService;
import com.tierconnect.riot.migration.MigrationException;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.List;

/**
 * Created by brayan on 4/6/17.
 */
public class Migrate_AddLastDetectFilterTypesConfig_VIZIX3240 implements MigrationStep{
    private static Logger logger = Logger.getLogger(Migrate_AddLastDetectFilterTypesConfig_VIZIX3240.class);


    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {

    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateLastDetectFilterTypes();
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {

    }

    private void migrateLastDetectFilterTypes() {
        List<Edgebox> edgeboxes = EdgeboxService.getInstance().getByType("StarFLEX");
        for (Edgebox edgebox : edgeboxes) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode;
            try {
                if (!edgebox.getConfiguration().contains("lastDetectFilterTypes")) {
                    rootNode = mapper.readTree(edgebox.getConfiguration());
                    ((ObjectNode) rootNode).put("lastDetectFilterTypes", "");
                    edgebox.setConfiguration(rootNode.toString());
                    EdgeboxService.getEdgeboxDAO().update(edgebox);
                    logger.info("Updated Starflex configuration, lastDetectFilterTypes config added");
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
            if (!parameters.getValue().contains("lastDetectFilterTypes")) {
                ((ObjectNode) rootNode).put("lastDetectFilterTypes", "");
                parameters.setValue(rootNode.toString());
                ParametersService.getParametersDAO().update(parameters);
                logger.info("Updated Parameters configuration, lastDetectFilterTypes config added");
            }
        } catch(Exception e){
            throw new MigrationException("Migration step unable to run parameters category=" + parameters.getCategory() + " " +
                    "and code=" + parameters.getCode(), e);
        }
    }
}
