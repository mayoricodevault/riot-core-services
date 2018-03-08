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
import com.tierconnect.riot.migration.steps.Migrate_MigrationStepTemplate_VIZIX000;
import org.apache.log4j.Logger;
import com.tierconnect.riot.migration.steps.MigrationStep;

import java.io.IOException;
import java.util.List;

/**
 * Created by vlad on 7/25/17.
 */
public class Migrate_MigrationSTARflexBridgeSetup_VIZIX6267 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_MigrationStepTemplate_VIZIX000.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateSTARflexBridges();
        migrateSTARflexParametersObject();

        migrateSTARflexName("Starflex H&S", "STARflex H&S");
        migrateSTARflexName("Starflex tag", "STARflex Tag");
        migrateSTARflexName("STARflex tag", "STARflex Tag");
    }

    private void migrateSTARflexBridges() {
        List<Edgebox> edgeboxes = EdgeboxService.getInstance().getByType("STARflex");

        for (Edgebox edgebox : edgeboxes) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode;

            try {
                rootNode = mapper.readTree(edgebox.getConfiguration());

                if (edgebox.getConfiguration().contains("numberOfThreads")) {
                    ((ObjectNode) rootNode).remove("numberOfThreads");
                }

                if (edgebox.getConfiguration().contains("mongo")) {
                    ((ObjectNode) rootNode).remove("mongo");
                }

                edgebox.setConfiguration(rootNode.toString());
                EdgeboxService.getInstance().update(edgebox);
            } catch(IOException e) {
                throw new MigrationException("Migration step unable to parse edgeBox code=" + edgebox.getCode(), e);
            }
        }
    }

    private void migrateSTARflexParametersObject() {
        Parameters parameters = ParametersService.getInstance().getByCategoryAndCode("BRIDGE_TYPE", "STARflex");

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode;
        ObjectNode configurationNode;

        try {
            rootNode = mapper.readTree(parameters.getValue());

            if (rootNode.has("configuration")) {
                configurationNode = (ObjectNode) rootNode.get("configuration");
                if (configurationNode.has("mongo")) {
                    configurationNode.remove("mongo");
                }

                if (configurationNode.has("numberOfThreads")) {
                    configurationNode.remove("numberOfThreads");
                }

                parameters.setValue(rootNode.toString());
                ParametersService.getParametersDAO().update(parameters);
            }
        } catch(Exception e) {
            throw new MigrationException("Migration step unable to remove mongo and numberOfThreads from STARflex parameter", e);
        }
    }

    private void migrateSTARflexName(String originName, String targetName) {
        List<ThingTypeTemplate> thingTypeTemplateList = ThingTypeTemplateService.getThingTypeTemplateDAO().selectAllBy("name", originName);

        if (thingTypeTemplateList != null && !thingTypeTemplateList.isEmpty()) {
            for (ThingTypeTemplate thingTypeTemplate : thingTypeTemplateList) {
                thingTypeTemplate.setName(targetName);
                ThingTypeTemplateService.getInstance().update(thingTypeTemplate);
            }
        }
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }
}
