package com.tierconnect.riot.migration.steps.edgebox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.entities.Parameters;
import com.tierconnect.riot.iot.services.EdgeboxService;
import com.tierconnect.riot.iot.services.ParametersService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_MigrateAleBridgeConf_NOISSUE implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_MigrateAleBridgeConf_NOISSUE.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateAleBridgeTemplateConfiguration();
        migrateAleBridgeConfiguration();
    }

    /**
     * migrateAleBridgeTemplateConfiguration
     * Update the "edge" parameters
     */
    private static void migrateAleBridgeTemplateConfiguration() {
        String bridgeType = "BRIDGE_TYPE";
        Parameters parameters = ParametersService.getInstance().getByCategoryAndCode(bridgeType, "edge");
        String value = parameters.getValue();
        String valueToAdd = "";
        if (!value.contains("socketTimeout")) {
            valueToAdd = ",\"socketTimeout\":60000";
        }
        if (!value.contains("dynamicTimeoutRate")) {
            valueToAdd = valueToAdd + ",\"dynamicTimeoutRate\":0";
        }
        if (!value.contains("send500ErrorOnTimeout")) {
            valueToAdd = valueToAdd + ",\"send500ErrorOnTimeout\":{\"active\":0}";
        }
        valueToAdd = valueToAdd + "}";
        String replaceParameters = value.substring(0, value.length() - 1) + valueToAdd;
        parameters.setValue(replaceParameters);
        ParametersService.getInstance().update(parameters);
    }

    /**
     * migrateAleBridgeConfiguration
     * migrate the Ale Bridge Configuration with new udfs template
     */
    private static void migrateAleBridgeConfiguration() {
        logger.info("Migrating Ale Bridge configuration...");
        EdgeboxService edgeboxService = EdgeboxService.getInstance();
        List<Edgebox> edgebox = edgeboxService.selectAll();
        for (Edgebox edge : edgebox) {
            if (StringUtils.equals(edge.getType(), "edge")) {
                setAleBridgeConfiguration(edgeboxService, edge, "socketTimeout", 60000, false);
                setAleBridgeConfiguration(edgeboxService, edge, "dynamicTimeoutRate", 0, false);
                setAleBridgeConfiguration(edgeboxService, edge, "send500ErrorOnTimeout", 0, true);
            }
        }
    }

    /**
     * @param edgeboxService   edgebox Service
     * @param edgebox          edgebox
     * @param udfConfiguration udf Configuration
     * @param udfValue         udf Value
     * @param isBooleanValue   Boolean Value
     *                         Update the new udfs ale bridge configuration
     */
    public static void setAleBridgeConfiguration(EdgeboxService edgeboxService, Edgebox edgebox, String
            udfConfiguration, int udfValue, boolean isBooleanValue) {
        String edgeboxConfig = edgebox.getConfiguration();
        if (!edgeboxConfig.contains(udfConfiguration)) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                if (StringUtils.isNotEmpty(edgeboxConfig)) {
                    JsonNode rootNode = mapper.readTree(edgeboxConfig);
                    if (isBooleanValue) {
                        ObjectNode node = mapper.createObjectNode();
                        node.put("active", 0);
                        ((ObjectNode) rootNode).put(udfConfiguration, node);
                    } else {
                        ((ObjectNode) rootNode).put(udfConfiguration, udfValue);
                    }
                    edgeboxConfig = rootNode.toString();
                    edgebox.setConfiguration(edgeboxConfig);
                    edgeboxService.update(edgebox);
                    logger.info(udfConfiguration + " default configuration has been added to aleBridge configuration");
                } else {
                    logger.info(edgebox.getCode() + " without Edgebox Config");
                }

            } catch (Exception e) {
                logger.error("Error when update AleBridge Configuration", e);
            }
        }
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

}
