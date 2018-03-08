package com.tierconnect.riot.migration.steps.parameters;

import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.iot.entities.Parameters;
import com.tierconnect.riot.iot.popdb.PopDBRequiredIOT;
import com.tierconnect.riot.iot.services.ParametersService;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

/**
 * Created by vramos on 5/5/17.
 */
public class Migrate_BridgeParameters_VIZIX5000 implements MigrationStep {

    private static Logger logger = Logger.getLogger(Migrate_BridgeParameters_VIZIX5000.class);

    @Override public void migrateSQLBefore(String scriptPath) throws Exception {
    }

    @Override public void migrateHibernate() throws Exception {
        Parameters param =
            ParametersService.getInstance().getByCategoryAndCode(Constants.BRIDGE_TYPE, "edge");
        param.setValue(
            "{\"filters\":{\"zoneDwellFilter\":{\"value\":{\"active\":{\"value\":false,"
                + "\"type\":\"Boolean\",\"order\":0,\"required\":false},"
                + "\"unlockDistance\":{\"value\":25,\"type\":\"Number\",\"order\":0,"
                + "\"required\":true},\"inZoneDistance\":{\"value\":10,\"type\":\"Number\","
                + "\"order\":1,\"required\":true},\"zoneDwellTime\":{\"value\":300,"
                + "\"type\":\"Number\",\"order\":2,\"required\":true},"
                + "\"lastDetectTimeActive\":{\"value\":true,\"type\":\"Boolean\",\"order\":3,"
                + "\"required\":false},\"lastDetectTimeWindow\":{\"value\":0,\"type\":\"Number\","
                + "\"order\":4,\"required\":true}},\"type\":\"JSON\",\"order\":0,"
                + "\"required\":false}},\"configuration\":{\"thingTypeCode\":{\"value\":\"\","
                + "\"type\":\"String\",\"order\":0,\"required\":true},"
                + "\"logRawMessages\":{\"value\":false,\"type\":\"Boolean\",\"order\":1,"
                + "\"required\":false},\"numberOfThreads\":{\"value\":10,\"type\":\"Number\","
                + "\"order\":2,\"required\":true},\"socketTimeout\":{\"value\":60000,"
                + "\"type\":\"Number\",\"order\":3,\"required\":true},"
                + "\"dynamicTimeoutRate\":{\"value\":0,\"type\":\"Number\",\"order\":4,"
                + "\"required\":true},\"send500ErrorOnTimeout\":{\"value\":false,"
                + "\"type\":\"Boolean\",\"order\":5,\"required\":false},"
                + "\"mqtt\":{\"value\":{\"connectionCode\":{\"value\":\"\",\"type\":\"String\","
                + "\"order\":0,\"required\":true}},\"type\":\"JSON\",\"order\":6,"
                + "\"required\":false},\"kafka\":{\"value\":{\"active\":{\"value\":false,"
                + "\"type\":\"Boolean\",\"order\":0,\"required\":false},"
                + "\"connectionCode\":{\"value\":\"\",\"type\":\"String\",\"order\":0,"
                + "\"required\":true}},\"type\":\"JSON\",\"order\":7,\"required\":false},"
                + "\"streaming\":{\"value\":{\"active\":{\"value\":false,\"type\":\"Boolean\","
                + "\"order\":0,\"required\":false},\"bufferSize\":{\"value\":10,"
                + "\"type\":\"Number\",\"order\":0,\"required\":true}},\"type\":\"JSON\","
                + "\"order\":8,\"required\":false},\"evaluateStats\":{\"value\":true,"
                + "\"type\":\"Boolean\",\"order\":9,\"required\":false},"
                + "\"bridgePort\":{\"value\":9090,\"type\":\"Number\",\"order\":10,"
                + "\"required\":false}},\"extra\":{}}");
        ParametersService.getInstance().update(param);
        param = ParametersService.getInstance()
            .getByCategoryAndCode(Constants.BRIDGE_TYPE, Constants.EDGEBOX_STARFLEX_TYPE);
        param.setValue(PopDBRequiredIOT
            .getStarFlexBridgeConfiguration(Constants.TT_STARflex_CONFIG_CODE,
                Constants.TT_STARflex_STATUS_CODE, Constants.TT_STARflex_CODE, "", ""));
        ParametersService.getInstance().update(param);
        param = ParametersService.getInstance().getByCategoryAndCode(Constants.BRIDGE_TYPE, "FTP");
        param.setValue("{\"filters\":{},\"configuration\":{\"thingTypeCode\":{\"value\":\"\","
            + "\"type\":\"String\",\"order\":0,\"required\":true},"
            + "\"logRawMessages\":{\"value\":false,\"type\":\"Boolean\",\"order\":1,"
            + "\"required\":false},\"ftp\":{\"value\":{\"connectionCode\":{\"value\":\"\","
            + "\"type\":\"String\",\"order\":0,\"required\":true}},\"type\":\"JSON\",\"order\":2,"
            + "\"required\":false},\"path\":{\"value\":\"/StoreReferenceData\","
            + "\"type\":\"String\",\"order\":3,\"required\":true},\"pattern\":{\"value\":\"*"
            + ".COLOUR\",\"type\":\"String\",\"order\":4,\"required\":true},"
            + "\"patternCaseSensitive\":{\"value\":false,\"type\":\"Boolean\",\"order\":5,"
            + "\"required\":false},\"schedule\":{\"value\":\"0 0/10 * 1/1 * ? *\","
            + "\"type\":\"String\",\"order\":6,\"required\":true},"
            + "\"configParser\":{\"value\":{\"parserType\":{\"value\":\"fixedlength\","
            + "\"type\":\"String\",\"order\":0,\"required\":true},\"separator\":{\"value\":null,"
            + "\"type\":\"String\",\"order\":1,\"required\":false},"
            + "\"fieldLengths\":{\"value\":\"3,16,1\",\"type\":\"String\",\"order\":2,"
            + "\"required\":false},\"ignoreHeader\":{\"value\":false,\"type\":\"Boolean\","
            + "\"order\":3,\"required\":false},\"ignoreFooter\":{\"value\":true,"
            + "\"type\":\"Boolean\",\"order\":4,\"required\":false},"
            + "\"fieldNames\":{\"value\":[\"Code\",\"Description\",\"Action\"],"
            + "\"type\":\"ARRAY\",\"order\":5,\"required\":true},"
            + "\"columnNumberAsSerial\":{\"value\":0,\"type\":\"Number\",\"order\":6,"
            + "\"required\":false}},\"type\":\"JSON\",\"order\":7,\"required\":false},"
            + "\"processPolicy\":{\"value\":\"Move\",\"type\":\"String\",\"order\":8,"
            + "\"required\":true},\"localBackupFolder\":{\"value\":\"/tmp\",\"type\":\"String\","
            + "\"order\":9,\"required\":true},"
            + "\"ftpDestinationFolder\":{\"value\":\"processed/colour\",\"type\":\"String\","
            + "\"order\":10,\"required\":true},"
            + "\"mqtt\":{\"value\":{\"connectionCode\":{\"value\":\"\",\"type\":\"String\","
            + "\"order\":0,\"required\":true}},\"type\":\"JSON\",\"order\":11,"
            + "\"required\":false}},\"extra\":{}}");
        ParametersService.getInstance().update(param);
        param = ParametersService.getInstance().getByCategoryAndCode(Constants.BRIDGE_TYPE, "GPS");
        param.setValue(
            "{\"filters\":{},\"configuration\":{\"thingTypeCode\":{\"value\":\"\","
                + "\"type\":\"String\",\"order\":0,\"required\":true},"
                + "\"logRawMessages\":{\"value\":false,\"type\":\"Boolean\",\"order\":1,"
                + "\"required\":false},\"mqtt\":{\"value\":{\"connectionCode\":{\"value\":\"\","
                + "\"type\":\"String\",\"order\":0,\"required\":true}},\"type\":\"JSON\","
                + "\"order\":2,\"required\":false},"
                + "\"geoforce\":{\"value\":{\"host\":{\"value\":\"app.geoforce.com\","
                + "\"type\":\"String\",\"order\":0,\"required\":true},"
                + "\"path\":{\"value\":\"/feeds/asset_inventory.xml\",\"type\":\"String\","
                + "\"order\":1,\"required\":true},\"port\":{\"value\":443,\"type\":\"Number\","
                + "\"order\":2,\"required\":true},\"user\":{\"value\":\"datafeed@mojix.com\","
                + "\"type\":\"String\",\"order\":3,\"required\":true},"
                + "\"password\":{\"value\":\"AHmgooCk8l0jo95f7YSo\",\"type\":\"String\","
                + "\"order\":4,\"required\":true},\"period\":{\"value\":60,\"type\":\"Number\","
                + "\"order\":5,\"required\":true}},\"type\":\"JSON\",\"order\":3,"
                + "\"required\":false}},\"extra\":{}}");
        ParametersService.getInstance().update(param);
        param = ParametersService.getInstance().getByCategoryAndCode(Constants.BRIDGE_TYPE, "core");
        param.setValue(
            "{\"filters\":{\"shiftZoneRule\":{\"value\":{\"active\":{\"value\":false,"
                + "\"type\":\"Boolean\",\"order\":0,\"required\":false},"
                + "\"shiftProperty\":{\"value\":\"shift\",\"type\":\"String\",\"order\":0,"
                + "\"required\":true},"
                + "\"zoneViolationStatusProperty\":{\"value\":\"zoneViolationStatus\","
                + "\"type\":\"String\",\"order\":2,\"required\":true},"
                + "\"zoneViolationFlagProperty\":{\"value\":\"zoneViolationFlag\","
                + "\"type\":\"String\",\"order\":1,\"required\":true}},\"type\":\"JSON\","
                + "\"order\":0,\"required\":false},"
                + "\"sourceRule\":{\"value\":{\"active\":{\"value\":true,\"type\":\"Boolean\","
                + "\"order\":0,\"required\":false}},\"type\":\"JSON\",\"order\":1,"
                + "\"required\":false}},\"configuration\":{\"numberOfThreads\":{\"value\":1,"
                + "\"type\":\"Number\",\"order\":0,\"required\":true},"
                + "\"mqtt\":{\"value\":{\"connectionCode\":{\"value\":\"\",\"type\":\"String\","
                + "\"order\":0,\"required\":true},\"topics\":{\"value\":[],\"type\":\"ARRAY\","
                + "\"order\":1,\"required\":false}},\"type\":\"JSON\",\"order\":1,"
                + "\"required\":false},\"mongo\":{\"value\":{\"connectionCode\":{\"value\":\"\","
                + "\"type\":\"String\",\"order\":0,\"required\":true}},\"type\":\"JSON\","
                + "\"order\":2,\"required\":false},"
                + "\"CEPLogging\":{\"value\":{\"active\":{\"value\":false,\"type\":\"Boolean\","
                + "\"order\":0,\"required\":false}},\"type\":\"JSON\",\"order\":3,"
                + "\"required\":false},"
                + "\"checkMultilevelReferences\":{\"value\":{\"active\":{\"value\":false,"
                + "\"type\":\"Boolean\",\"order\":0,\"required\":false}},\"type\":\"JSON\","
                + "\"order\":4,\"required\":false},"
                + "\"outOfOrderRule\":{\"value\":{\"active\":{\"value\":false,"
                + "\"type\":\"Boolean\",\"order\":0,\"required\":false}},\"type\":\"JSON\","
                + "\"order\":5,\"required\":false},"
                + "\"timeOrderRule\":{\"value\":{\"active\":{\"value\":false,"
                + "\"type\":\"Boolean\",\"order\":0,\"required\":false},\"period\":{\"value\":0,"
                + "\"type\":\"Number\",\"order\":0,\"required\":true}},\"type\":\"JSON\","
                + "\"order\":6,\"required\":false},"
                + "\"interCacheEviction\":{\"value\":{\"active\":{\"value\":false,"
                + "\"type\":\"Boolean\",\"order\":0,\"required\":false}},\"type\":\"JSON\","
                + "\"order\":7,\"required\":false},"
                + "\"swarmFilter\":{\"value\":{\"active\":{\"value\":false,\"type\":\"Boolean\","
                + "\"order\":0,\"required\":false},\"timeGroupTimer\":{\"value\":5000,"
                + "\"type\":\"Number\",\"order\":0,\"required\":true},"
                + "\"swarmAlgorithm\":{\"value\":\"followLastDetect\",\"type\":\"String\","
                + "\"order\":1,\"required\":true},"
                + "\"thingTypes\":{\"value\":[{\"thingTypeCode\":\"default_rfid_thingtype\","
                + "\"udfGroupStatus\":\"groupStatus\",\"udfGroup\":\"grouping\","
                + "\"distanceFilter\":10000}],\"type\":\"ARRAY\",\"order\":2,\"required\":true}},"
                + "\"type\":\"JSON\",\"order\":8,\"required\":false},"
                + "\"CEPEngineConfiguration\":{\"value\":{\"insertIntoDispatchPreserveOrder"
                + "\":{\"value\":false,\"type\":\"Boolean\",\"order\":0,\"required\":false},"
                + "\"listenerDispatchPreserveOrder\":{\"value\":false,\"type\":\"Boolean\","
                + "\"order\":1,\"required\":false},\"multipleInstanceMode\":{\"value\":false,"
                + "\"type\":\"Boolean\",\"order\":2,\"required\":false}},\"type\":\"JSON\","
                + "\"order\":9,\"required\":false},"
                + "\"interCacheEvictionQueueSize\":{\"value\":20000,\"type\":\"Number\","
                + "\"order\":10,\"required\":false},"
                + "\"fixOlderSnapshotsQueueSize\":{\"value\":20000,\"type\":\"Number\","
                + "\"order\":11,\"required\":true},\"evaluateStats\":{\"value\":true,"
                + "\"type\":\"Boolean\",\"order\":12,\"required\":false},"
                + "\"insertThingRetryConfig\":{\"value\":{\"maxRetries\":{\"value\":10,"
                + "\"type\":\"Number\",\"order\":0,\"required\":true},"
                + "\"retryInterval\":{\"value\":5000,\"type\":\"Number\",\"order\":1,"
                + "\"required\":true}},\"type\":\"JSON\",\"order\":13,\"required\":false}},"
                + "\"extra\":{}}");
        ParametersService.getInstance().update(param);
        param = ParametersService.getInstance().getByCategoryAndCode(Constants.BRIDGE_TYPE, "smed");
        if (param != null) {
            param.setValue(
                "{\"filters\":{},\"configuration\":{\"inputTopic\":{\"value\":\"/v1/data3\","
                    + "\"type\":\"String\",\"order\":0,\"required\":true},"
                    + "\"outputTopic\":{\"value\":\"/v1/data1\",\"type\":\"String\",\"order\":1,"
                    + "\"required\":true},\"outputFormat\":{\"value\":\"JSON\","
                    + "\"type\":\"String\",\"order\":2,\"required\":true},"
                    + "\"numberOfThreads\":{\"value\":1,\"type\":\"Number\",\"order\":3,"
                    + "\"required\":true},\"documentUdfNamePrefix\":{\"value\":\"scd\","
                    + "\"type\":\"String\",\"order\":4,\"required\":true},"
                    + "\"mqtt\":{\"value\":{\"active\":{\"value\":false,\"type\":\"Boolean\","
                    + "\"order\":0,\"required\":false},\"connectionCode\":{\"value\":\"MQTT\","
                    + "\"type\":\"String\",\"order\":0,\"required\":true}},\"type\":\"JSON\","
                    + "\"order\":5,\"required\":false},"
                    + "\"kafka\":{\"value\":{\"active\":{\"value\":true,\"type\":\"Boolean\","
                    + "\"order\":0,\"required\":false},\"connectionCode\":{\"value\":\"KAFKA\","
                    + "\"type\":\"String\",\"order\":0,\"required\":true},"
                    + "\"consumerGroup\":{\"value\":\"group1\",\"type\":\"String\",\"order\":1,"
                    + "\"required\":true},\"checkpoint\":{\"value\":false,\"type\":\"Boolean\","
                    + "\"order\":2,\"required\":false}},\"type\":\"JSON\",\"order\":6,"
                    + "\"required\":false},"
                    + "\"listener\":{\"value\":{\"kafkaCode\":{\"value\":\"KAFKA\","
                    + "\"type\":\"String\",\"order\":0,\"required\":true},"
                    + "\"topic\":{\"value\":\"___v1___events,1,1\",\"type\":\"String\","
                    + "\"order\":1,\"required\":true}},\"type\":\"JSON\",\"order\":7,"
                    + "\"required\":false},"
                    + "\"commands\":{\"value\":{\"kafkaCode\":{\"value\":\"KAFKA\","
                    + "\"type\":\"String\",\"order\":0,\"required\":true},"
                    + "\"topic\":{\"value\":\"/v1/commands/SMED,1,1\",\"type\":\"String\","
                    + "\"order\":1,\"required\":true},"
                    + "\"consortiumCode\":{\"value\":\"TEST_RETAILER\",\"type\":\"String\","
                    + "\"order\":2,\"required\":true},"
                    + "\"adapterCode\":{\"value\":\"BlockchainAdapter\",\"type\":\"String\","
                    + "\"order\":3,\"required\":true}},\"type\":\"JSON\",\"order\":8,"
                    + "\"required\":false}},\"extra\":{}}");
            ParametersService.getInstance().update(param);
        }
        param = ParametersService.getInstance()
            .getByCategoryAndCode(Constants.BRIDGE_TYPE, "Thing_Joiner");
        if (param != null) {
            param.setValue("{\"filters\":{},\"configuration\":{\"thingTypeCodes\":{\"value"
                + "\":[\"default_rfid_thingtype\"],\"type\":\"ARRAY\",\"order\":0,"
                + "\"required\":true},\"inputTopic\":{\"value\":\"/v1/data1\","
                + "\"type\":\"String\",\"order\":1,\"required\":true},"
                + "\"outputTopic\":{\"value\":\"/v1/data2\",\"type\":\"String\",\"order\":2,"
                + "\"required\":true},"
                + "\"streamConfig\":{\"value\":{\"appId\":{\"value\":\"ThingJoiner-app\","
                + "\"type\":\"String\",\"order\":0,\"required\":true},\"lingerMs\":{\"value\":5,"
                + "\"type\":\"Number\",\"order\":1,\"required\":true},"
                + "\"numStreamThreads\":{\"value\":4,\"type\":\"Number\",\"order\":2,"
                + "\"required\":true},\"batchSize\":{\"value\":65536,\"type\":\"Number\","
                + "\"order\":3,\"required\":true},"
                + "\"stateDirPath\":{\"value\":\"/var/ThingJoiner/store\",\"type\":\"String\","
                + "\"order\":4,\"required\":true}},\"type\":\"JSON\",\"order\":3,"
                + "\"required\":false},"
                + "\"kafka\":{\"value\":{\"connectionCode\":{\"value\":\"KAFKA\","
                + "\"type\":\"String\",\"order\":0,\"required\":true}},\"type\":\"JSON\","
                + "\"order\":4,\"required\":false},"
                + "\"mongo\":{\"value\":{\"connectionCode\":{\"value\":\"MONGO\","
                + "\"type\":\"String\",\"order\":0,\"required\":true}},\"type\":\"JSON\","
                + "\"order\":5,\"required\":false},"
                + "\"notificationService\":{\"value\":{\"active\":{\"value\":false,"
                + "\"type\":\"Boolean\",\"order\":0,\"required\":false},"
                + "\"connectionCode\":{\"value\":\"MQTT\",\"type\":\"String\",\"order\":0,"
                + "\"required\":true},\"recipients\":{\"value\":[],\"type\":\"ARRAY\","
                + "\"order\":1,\"required\":true}},\"type\":\"JSON\",\"order\":6,"
                + "\"required\":false}},\"extra\":{}}");
            ParametersService.getInstance().update(param);
        }
        param = ParametersService.getInstance()
            .getByCategoryAndCode(Constants.BRIDGE_TYPE, "Rules_Processor");
        if (param != null) {
            param.setValue(
                "{\"filters\":{\"shiftZoneRule\":{\"value\":{\"active\":{\"value\":false,"
                    + "\"type\":\"Boolean\",\"order\":0,\"required\":false},"
                    + "\"shiftProperty\":{\"value\":\"shift\",\"type\":\"String\",\"order\":0,"
                    + "\"required\":true},"
                    + "\"zoneViolationStatusProperty\":{\"value\":\"zoneViolationStatus\","
                    + "\"type\":\"String\",\"order\":1,\"required\":true},"
                    + "\"zoneViolationFlagProperty\":{\"value\":\"zoneViolationFlag\","
                    + "\"type\":\"String\",\"order\":2,\"required\":true}},\"type\":\"JSON\","
                    + "\"order\":0,\"required\":false},"
                    + "\"sourceRule\":{\"value\":{\"active\":{\"value\":true,"
                    + "\"type\":\"Boolean\",\"order\":0,\"required\":false}},\"type\":\"JSON\","
                    + "\"order\":1,\"required\":false}},"
                    + "\"configuration\":{\"inputTopic\":{\"value\":\"/v1/data2\","
                    + "\"type\":\"String\",\"order\":0,\"required\":true},"
                    + "\"outputTopic\":{\"value\":\"/v1/data3\",\"type\":\"String\",\"order\":1,"
                    + "\"required\":true},"
                    + "\"outOfOrderRule\":{\"value\":{\"active\":{\"value\":false,"
                    + "\"type\":\"Boolean\",\"order\":0,\"required\":false}},\"type\":\"JSON\","
                    + "\"order\":2,\"required\":false},"
                    + "\"CEPLogging\":{\"value\":{\"active\":{\"value\":false,"
                    + "\"type\":\"Boolean\",\"order\":0,\"required\":false}},\"type\":\"JSON\","
                    + "\"order\":3,\"required\":false},"
                    + "\"swarmFilter\":{\"value\":{\"active\":{\"value\":false,"
                    + "\"type\":\"Boolean\",\"order\":0,\"required\":false},"
                    + "\"outputTopic\":{\"value\":\"/v1/data1\",\"type\":\"String\",\"order\":0,"
                    + "\"required\":true},\"timeGroupTimer\":{\"value\":25000,"
                    + "\"type\":\"Number\",\"order\":1,\"required\":true},"
                    + "\"algorithm\":{\"value\":\"followLastDetect\",\"type\":\"String\","
                    + "\"order\":2,\"required\":true},"
                    + "\"thingTypes\":{\"value\":[{\"thingTypeCode\":\"default_rfid_thingtype\","
                    + "\"udfGroup\":\"grouping\",\"distanceFilter\":10000,"
                    + "\"udfGroupStatus\":\"groupStatus\"}],\"type\":\"ARRAY\",\"order\":3,"
                    + "\"required\":true}},\"type\":\"JSON\",\"order\":4,\"required\":false},"
                    + "\"CEPEngineConfiguration\":{\"value\":{\"insertIntoDispatchPreserveOrder"
                    + "\":{\"value\":false,\"type\":\"Boolean\",\"order\":0,\"required\":false},"
                    + "\"multipleInstanceMode\":{\"value\":false,\"type\":\"Boolean\","
                    + "\"order\":1,\"required\":false},"
                    + "\"listenerDispatchPreserveOrder\":{\"value\":false,\"type\":\"Boolean\","
                    + "\"order\":2,\"required\":false}},\"type\":\"JSON\",\"order\":5,"
                    + "\"required\":false},"
                    + "\"streamConfig\":{\"value\":{\"appId\":{\"value\":\"RulesProcessor-app\","
                    + "\"type\":\"String\",\"order\":0,\"required\":true},"
                    + "\"lingerMs\":{\"value\":5,\"type\":\"Number\",\"order\":1,"
                    + "\"required\":true},\"numStreamThreads\":{\"value\":4,\"type\":\"Number\","
                    + "\"order\":2,\"required\":true},\"batchSize\":{\"value\":65536,"
                    + "\"type\":\"Number\",\"order\":3,\"required\":true},"
                    + "\"stateDirPath\":{\"value\":\"/var/RulesProcessor/store\","
                    + "\"type\":\"String\",\"order\":4,\"required\":true}},\"type\":\"JSON\","
                    + "\"order\":6,\"required\":false},"
                    + "\"kafka\":{\"value\":{\"connectionCode\":{\"value\":\"KAFKA\","
                    + "\"type\":\"String\",\"order\":0,\"required\":true}},\"type\":\"JSON\","
                    + "\"order\":7,\"required\":false},"
                    + "\"mongo\":{\"value\":{\"connectionCode\":{\"value\":\"MONGO\","
                    + "\"type\":\"String\",\"order\":0,\"required\":true}},\"type\":\"JSON\","
                    + "\"order\":8,\"required\":false},"
                    + "\"notificationService\":{\"value\":{\"active\":{\"value\":false,"
                    + "\"type\":\"Boolean\",\"order\":0,\"required\":false},"
                    + "\"connectionCode\":{\"value\":\"MQTT\",\"type\":\"String\",\"order\":0,"
                    + "\"required\":true},\"recipients\":{\"value\":[],\"type\":\"ARRAY\","
                    + "\"order\":1,\"required\":true}},\"type\":\"JSON\",\"order\":9,"
                    + "\"required\":false}},\"extra\":{}}");
            ParametersService.getInstance().update(param);
        }
        param = ParametersService.getInstance()
            .getByCategoryAndCode(Constants.BRIDGE_TYPE, "Mongo_Injector");
        if (param != null) {
            param.setValue(
                "{\"filters\":{},\"configuration\":{\"inputTopic\":{\"value\":\"/v1/data3\","
                    + "\"type\":\"String\",\"order\":0,\"required\":true},"
                    + "\"mongo\":{\"value\":{\"connectionCode\":{\"value\":\"MONGO\","
                    + "\"type\":\"String\",\"order\":0,\"required\":true}},\"type\":\"JSON\","
                    + "\"order\":1,\"required\":false},"
                    + "\"streamConfig\":{\"value\":{\"appId\":{\"value\":\"MongoInjector-app\","
                    + "\"type\":\"String\",\"order\":0,\"required\":true},"
                    + "\"lingerMs\":{\"value\":5,\"type\":\"Number\",\"order\":1,"
                    + "\"required\":true},\"numStreamThreads\":{\"value\":4,\"type\":\"Number\","
                    + "\"order\":2,\"required\":true},\"batchSize\":{\"value\":65536,"
                    + "\"type\":\"Number\",\"order\":3,\"required\":true},"
                    + "\"stateDirPath\":{\"value\":\"/var/MongoInjector/store\","
                    + "\"type\":\"String\",\"order\":4,\"required\":true}},\"type\":\"JSON\","
                    + "\"order\":2,\"required\":false},"
                    + "\"kafka\":{\"value\":{\"connectionCode\":{\"value\":\"KAFKA\","
                    + "\"type\":\"String\",\"order\":0,\"required\":true}},\"type\":\"JSON\","
                    + "\"order\":3,\"required\":false},"
                    + "\"notificationService\":{\"value\":{\"active\":{\"value\":false,"
                    + "\"type\":\"Boolean\",\"order\":0,\"required\":false},"
                    + "\"connectionCode\":{\"value\":\"MQTT\",\"type\":\"String\",\"order\":0,"
                    + "\"required\":true},\"recipients\":{\"value\":[],\"type\":\"ARRAY\","
                    + "\"order\":1,\"required\":true}},\"type\":\"JSON\",\"order\":4,"
                    + "\"required\":false}},\"extra\":{}}");
            ParametersService.getInstance().update(param);
        }
    }

    @Override public void migrateSQLAfter(String scriptPath) throws Exception {
    }
}
