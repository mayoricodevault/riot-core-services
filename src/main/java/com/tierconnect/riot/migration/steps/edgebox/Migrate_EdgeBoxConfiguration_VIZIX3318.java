package com.tierconnect.riot.migration.steps.edgebox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.services.EdgeboxService;
import com.tierconnect.riot.migration.steps.MigrationStep;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Migrate_EdgeBoxConfiguration_VIZIX3318 class.
 *
 * @author jantezana
 * @version 2017/03/30
 */
public class Migrate_EdgeBoxConfiguration_VIZIX3318 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_EdgeBoxConfiguration_VIZIX3318.class);
    private static final String JSON_EMPTY_BODY = "{}";

    @Override
    public void migrateSQLBefore(String scriptPath)
    throws Exception {
    }

    @Override
    public void migrateHibernate()
    throws Exception {
        migrateEdgeBoxConfigurations();
    }

    @Override
    public void migrateSQLAfter(String scriptPath)
    throws Exception {
    }

    /**
     * Migrate edge box configurations.
     */
    private void migrateEdgeBoxConfigurations()
    throws IOException {
        EdgeboxService edgeboxService = EdgeboxService.getInstance();
        List<Edgebox> edgeBoxes;
        try {
            edgeBoxes = edgeboxService.getByType("edge");
        } catch (Exception exception) {
            logger.warn(exception.getMessage(), exception);
            edgeBoxes = new LinkedList<>();
        }

        for (Edgebox edgebox : edgeBoxes) {
            updateAleBridge(edgebox);
        }
        
        // hack. need to delete
        try {
            logger.info("Migrating Fosse.");
            Edgebox fosse = edgeboxService.selectByCode("ALEFosse");
            fosse.setGroup(GroupService.getInstance().get(5l));
            edgeboxService.update(fosse);
        } catch (Exception e) {
            logger.warn("Unable to migrate Fosse");
        }
    }

    /**
     * Update ale bridge configuration.
     *
     * @param aleBridge the ale bridge
     */
    private void updateAleBridge(Edgebox aleBridge)
    throws IOException {
        String aleBridgeConfiguration = aleBridge.getConfiguration();
        if (StringUtils.isEmpty(aleBridgeConfiguration)) {
            aleBridgeConfiguration = JSON_EMPTY_BODY;
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(aleBridgeConfiguration);

        // Update kafka configuration.
        ObjectNode kafkaObjectNode;
        if (rootNode.has("kafka")) {
            kafkaObjectNode = (ObjectNode) rootNode.get("kafka").deepCopy();
        } else {
            kafkaObjectNode = mapper.createObjectNode();
        }
        
        if(!kafkaObjectNode.has("connectionCode")) {
            kafkaObjectNode.put("connectionCode", "KAFKA");
            kafkaObjectNode.put("active", false);    
        }

        ((ObjectNode) rootNode).set("kafka", kafkaObjectNode);

        // Update mqtt configuration.
        ObjectNode mqttObjectNode;
        if (rootNode.has("mqtt")) {
            mqttObjectNode = ((ObjectNode) rootNode.get("mqtt")).deepCopy();
        } else {
            mqttObjectNode = mapper.createObjectNode();
        }
        
        if(!mqttObjectNode.has("connectionCode")) {
            mqttObjectNode.put("connectionCode", "MQTT");
        }
        
        ((ObjectNode) rootNode).set("mqtt", mqttObjectNode);

        aleBridgeConfiguration = rootNode.toString();
        aleBridge.setConfiguration(aleBridgeConfiguration);
        
        EdgeboxService.getInstance().update(aleBridge);

        logger.info("Updated ale bridge configuration, updated the configurations for kafka, facility map and output format");
    }
}
