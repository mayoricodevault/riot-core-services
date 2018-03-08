package com.tierconnect.riot.migration.steps.edgebox;

import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.iot.entities.Parameters;
import com.tierconnect.riot.iot.services.ParametersService;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

/**
 * Migrate_NewEdgeBoxes_VIZIX3612 class.
 *
 * @author jantezana
 * @version 2017/04/08
 */
public class Migrate_NewEdgeBoxes_VIZIX3612 implements MigrationStep {
    private static final Logger LOGGER = Logger.getLogger(Migrate_NewEdgeBoxes_VIZIX3612.class);

    private static final String THING_JOINER_BRIDGE_TYPE = "Thing_Joiner";
    private static final String RULES_PROCESSOR_BRIDGE_TYPE = "Rules_Processor";
    private static final String MONGO_INJECTOR_BRIDGE_TYPE = "Mongo_Injector";
    private static final String THING_JOINER_CONFIGURATION = "{\"thingTypeCodes\":[\"default_rfid_thingtype\"],\"inputTopic\":\"/v1/data1\",\"outputTopic\":\"/v1/data2\",\"streamConfig\":{\"appId\":\"ThingJoiner-app\",\"lingerMs\":5,\"numStreamThreads\":4,\"batchSize\":65536,\"stateDirPath\":\"/var/ThingJoiner/store\"},\"kafka\":{\"connectionCode\":\"KAFKA\"},\"mongo\":{\"connectionCode\":\"MONGO\"},\"notificationService\":{\"active\":false,\"connectionCode\":\"MQTT\",\"topic\":\"/v1/notification/ThingJoiner\",\"recipients\":[\"hugo.loza@mojix.com\",\"renan.huanca@mojix.com\",\"juan.antezana@mojix.com\",\"victor.ramos@mojix.com\",\"ariel.quiroz@mojix.com\"]}}";
    private static final String RULES_PROCESSOR_CONFIGURATION = "{\"inputTopic\":\"/v1/data2\",\"outputTopic\":\"/v1/data3\",\"doorEventRule\":{\"active\":1},\"pointInZoneRule\":{\"active\":1},\"shiftZoneRule\":{\"active\":0,\"shiftProperty\":\"shift\",\"zoneViolationStatusProperty\":\"zoneViolationStatus\",\"zoneViolationFlagProperty\":\"zoneViolationFlag\"},\"sourceRule\":{\"active\":1},\"swarmFilter\":{\"active\":0,\"outputTopic\":\"/v1/data1\",\"timeGroupTimer\":25000,\"algorithm\":\"followLastDetect\",\"thingTypes\":[{\"thingTypeCode\":\"default_rfid_thingtype \",\"udfGroup\":\"grouping\",\"distanceFilter\":10000,\"udfGroupStatus\":\"groupStatus\"}]},\"outOfOrderRule\":{\"active\":false},\"CEPLogging\":{\"active\":0},\"CEPEngineConfiguration\":{\"insertIntoDispatchPreserveOrder\":false,\"multipleInstanceMode\":false,\"listenerDispatchPreserveOrder\":false},\"streamConfig\":{\"appId\":\"RulesProcessor-app\",\"lingerMs\":5,\"numStreamThreads\":4,\"batchSize\":65536,\"stateDirPath\":\"/var/RulesProcessor/store\"},\"kafka\":{\"connectionCode\":\"KAFKA\"},\"mongo\":{\"connectionCode\":\"MONGO\"},\"notificationService\":{\"active\":false,\"connectionCode\":\"MQTT\",\"topic\":\"/v1/notification/RulesProcessor\",\"recipients\":[\"hugo.loza@mojix.com\",\"renan.huanca@mojix.com\",\"juan.antezana@mojix.com\",\"victor.ramos@mojix.com\",\"ariel.quiroz@mojix.com\"]}}";
    private static final String MONGO_INJECTOR_CONFIGURATION = "{\"inputTopic\":\"/v1/data3\",\"mongo\":{\"connectionCode\":\"MONGO\"},\"streamConfig\":{\"appId\":\"MongoInjector-app\",\"lingerMs\":5,\"numStreamThreads\":4,\"batchSize\":65536,\"stateDirPath\":\"/var/MongoInjector/store\"},\"kafka\":{\"connectionCode\":\"KAFKA\"},\"notificationService\":{\"active\":false,\"connectionCode\":\"MQTT\",\"topic\":\"/v1/notification/MongoInjector\",\"recipients\":[\"hugo.loza@mojix.com\",\"renan.huanca@mojix.com\",\"juan.antezana@mojix.com\",\"victor.ramos@mojix.com\",\"ariel.quiroz@mojix.com\"]}}";

    @Override
    public void migrateSQLBefore(String scriptPath)
    throws Exception {
    }

    @Override
    public void migrateHibernate()
    throws Exception {
        // Creates the new bridge types for joiner, cep processor and mongo injector.
        createParameters();
    }

    /**
     * Creates the new parameters, to create the new bridges types for joiner, rule processor and mongo injector.
     */
    private void createParameters() {
        Parameters thingJoinerParameters = null;
        // Thing joiner parameters.
        try {
            thingJoinerParameters = ParametersService.getInstance().getByCategoryAndCode(Constants.BRIDGE_TYPE, THING_JOINER_BRIDGE_TYPE);
        } catch (Exception exception) {
            LOGGER.warn(exception.getMessage(), exception);
        }
        if (thingJoinerParameters == null) {
            thingJoinerParameters = new Parameters(Constants.BRIDGE_TYPE, THING_JOINER_BRIDGE_TYPE, "@SYSTEM_PARAMETERS_BRIDGE_TYPE_THING_JOINER",
                                                   THING_JOINER_CONFIGURATION);
            ParametersService.getInstance().insert(thingJoinerParameters);
            LOGGER.info("The new parameter to create the new bridge type for thing joiner were created successfully");
        } else {
            thingJoinerParameters.setValue(THING_JOINER_CONFIGURATION);
            ParametersService.getInstance().update(thingJoinerParameters);
            LOGGER.info("The new parameter to create the new bridge type for thing joiner were updated successfully");
        }

        // Rule processor parameters.
        Parameters rulesProcessorParameters = null;
        try {
            rulesProcessorParameters = ParametersService.getInstance().getByCategoryAndCode(Constants.BRIDGE_TYPE, RULES_PROCESSOR_BRIDGE_TYPE);
        } catch (Exception exception) {
            LOGGER.warn(exception.getMessage(), exception);
        }
        if (rulesProcessorParameters == null) {
            rulesProcessorParameters = new Parameters(Constants.BRIDGE_TYPE, RULES_PROCESSOR_BRIDGE_TYPE,
                                                      "@SYSTEM_PARAMETERS_BRIDGE_TYPE_RULES_PROCESSOR", RULES_PROCESSOR_CONFIGURATION);
            ParametersService.getInstance().insert(rulesProcessorParameters);
            LOGGER.info("The new parameter to create the new bridge type for Rules processor were created successfully");
        } else {
            rulesProcessorParameters.setValue(RULES_PROCESSOR_CONFIGURATION);
            ParametersService.getInstance().update(rulesProcessorParameters);
            LOGGER.info("The new parameter to create the new bridge type for Rules processor were updated successfully");
        }

        // Mongo Injector parameters.
        Parameters mongoInjectorParameters = null;
        try {
            mongoInjectorParameters = ParametersService.getInstance().getByCategoryAndCode(Constants.BRIDGE_TYPE, MONGO_INJECTOR_BRIDGE_TYPE);

        } catch (Exception exception) {
            LOGGER.warn(exception.getMessage(), exception);
        }

        if (mongoInjectorParameters == null) {
            mongoInjectorParameters = new Parameters(Constants.BRIDGE_TYPE, MONGO_INJECTOR_BRIDGE_TYPE,
                                                     "@SYSTEM_PARAMETERS_BRIDGE_TYPE_MONGO_INJECTOR", MONGO_INJECTOR_CONFIGURATION);
            ParametersService.getInstance().insert(mongoInjectorParameters);
            LOGGER.info("The new parameter to create the new bridge type for mongo injector were created successfully");
        } else {
            mongoInjectorParameters.setValue(MONGO_INJECTOR_CONFIGURATION);
            ParametersService.getInstance().update(mongoInjectorParameters);
            LOGGER.info("The new parameter to create the new bridge type for mongo injector were updated successfully");
        }
    }

    @Override
    public void migrateSQLAfter(String scriptPath)
    throws Exception {
    }
}
