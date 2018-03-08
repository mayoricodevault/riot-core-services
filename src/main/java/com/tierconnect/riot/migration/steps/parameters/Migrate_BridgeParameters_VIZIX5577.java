package com.tierconnect.riot.migration.steps.parameters;

import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.iot.entities.Parameters;
import com.tierconnect.riot.iot.popdb.PopDBRequiredIOT;
import com.tierconnect.riot.iot.services.ParametersService;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

/**
 * Created by dbascope on 06/22/17
 */
public class Migrate_BridgeParameters_VIZIX5577 implements MigrationStep {

    private static Logger logger = Logger.getLogger(Migrate_BridgeParameters_VIZIX5577.class);

    @Override public void migrateSQLBefore(String scriptPath) throws Exception {
    }

    @Override public void migrateHibernate() throws Exception {
        Parameters param =
            ParametersService.getInstance().getByCategoryAndCode(Constants.BRIDGE_TYPE, "edge");
        param.setValue(
            "{\"filters\":{\"zoneDwellFilter\":{\"value\":{\"active\":{\"value\":true,"
                + "\"type\":\"Boolean\",\"required\":false},\"unlockDistance\":{\"value\":25,"
                + "\"type\":\"Number\",\"required\":false},\"inZoneDistance\":{\"value\":10,"
                + "\"type\":\"Number\",\"required\":false},\"zoneDwellTime\":{\"value\":300,"
                + "\"type\":\"Number\",\"required\":false},\"lastDetectTimeActive\":{\"value\":true,"
                + "\"type\":\"Boolean\",\"required\":false},\"lastDetectTimeWindow\":{\"value\":0,"
                + "\"type\":\"Number\",\"required\":false}},\"type\":\"JSON\",\"required\":false}},"
                + "\"configuration\":{\"thingTypeCode\":{\"value\":\"\",\"type\":\"String\","
                + "\"required\":false},\"logRawMessages\":{\"value\":false,\"type\":\"Boolean\","
                + "\"required\":false},\"numberOfThreads\":{\"value\":10,\"type\":\"Number\","
                + "\"required\":false},\"socketTimeout\":{\"value\":60000,\"type\":\"Number\","
                + "\"required\":false},\"dynamicTimeoutRate\":{\"value\":0,\"type\":\"Number\","
                + "\"required\":false},\"send500ErrorOnTimeout\":{\"value\":false,\"type\":\"Boolean\","
                + "\"required\":false},\"mqtt\":{\"value\":{\"connectionCode\":{\"value\":\"\","
                + "\"type\":\"String\",\"required\":false}},\"type\":\"JSON\",\"required\":false},"
                + "\"kafka\":{\"value\":{\"active\":{\"value\":false,\"type\":\"Boolean\","
                + "\"required\":false},\"connectionCode\":{\"value\":\"\",\"type\":\"String\","
                + "\"required\":false}},\"type\":\"JSON\",\"required\":false},"
                + "\"streaming\":{\"value\":{\"active\":{\"value\":false,\"type\":\"Boolean\","
                + "\"required\":false},\"bufferSize\":{\"value\":10,\"type\":\"Number\","
                + "\"required\":false}},\"type\":\"JSON\",\"required\":false},"
                + "\"evaluateStats\":{\"value\":true,\"type\":\"Boolean\",\"required\":false},"
                + "\"bridgePort\":{\"value\":9090,\"type\":\"Number\",\"required\":false}},"
                + "\"extra\":{\"apikey\":{\"value\":\"7B4BCCDC\",\"type\":\"String\",\"required\":false},"
                + "\"bridgeTopic\":{\"value\":\"\",\"type\":\"String\",\"required\":false},"
                + "\"httpHost\":{\"value\":\"\",\"type\":\"String\",\"required\":false}}}");
        ParametersService.getInstance().update(param);
        param = ParametersService.getInstance()
            .getByCategoryAndCode(Constants.BRIDGE_TYPE, Constants.EDGEBOX_STARFLEX_TYPE);
        param.setValue(PopDBRequiredIOT
            .getStarFlexBridgeConfiguration(Constants.TT_STARflex_CONFIG_CODE,
                Constants.TT_STARflex_STATUS_CODE, Constants.TT_STARflex_CODE, "", ""));
        ParametersService.getInstance().update(param);
        param = ParametersService.getInstance().getByCategoryAndCode(Constants.BRIDGE_TYPE, "FTP");
        param.setValue("{\"filters\":{},\"configuration\":{\"thingTypeCode\":{\"value\":\"\","
            + "\"type\":\"String\",\"required\":false},\"logRawMessages\":{\"value\":false,"
            + "\"type\":\"Boolean\",\"required\":false},"
            + "\"ftp\":{\"value\":{\"connectionCode\":{\"value\":\"\",\"type\":\"String\","
            + "\"required\":false}},\"type\":\"JSON\",\"required\":false},"
            + "\"path\":{\"value\":\"/StoreReferenceData\",\"type\":\"String\",\"required\":false},"
            + "\"pattern\":{\"value\":\"*.COLOUR\",\"type\":\"String\",\"required\":false},"
            + "\"patternCaseSensitive\":{\"value\":false,\"type\":\"Boolean\",\"required\":false},"
            + "\"schedule\":{\"value\":\"0 0/10 * 1/1 * ? *\",\"type\":\"String\","
            + "\"required\":false},\"configParser\":{\"value\":{\"parserType\":{\"value"
            + "\":\"fixedlength\",\"type\":\"String\",\"required\":false},"
            + "\"separator\":{\"value\":null,\"type\":\"String\",\"required\":false},"
            + "\"fieldLengths\":{\"value\":\"3,16,1\",\"type\":\"String\",\"required\":false},"
            + "\"ignoreHeader\":{\"value\":false,\"type\":\"Boolean\",\"required\":false},"
            + "\"ignoreFooter\":{\"value\":true,\"type\":\"Boolean\",\"required\":false},"
            + "\"fieldNames\":{\"value\":[\"Code\",\"Description\",\"Action\"],\"type\":\"ARRAY\","
            + "\"required\":false},\"columnNumberAsSerial\":{\"value\":0,\"type\":\"Number\","
            + "\"required\":false}},\"type\":\"JSON\",\"required\":false},"
            + "\"processPolicy\":{\"value\":\"Move\",\"type\":\"String\",\"required\":false},"
            + "\"localBackupFolder\":{\"value\":\"/tmp\",\"type\":\"String\",\"required\":false},"
            + "\"ftpDestinationFolder\":{\"value\":\"processed/colour\",\"type\":\"String\","
            + "\"required\":false},\"mqtt\":{\"value\":{\"connectionCode\":{\"value\":\"\","
            + "\"type\":\"String\",\"required\":false}},\"type\":\"JSON\",\"required\":false}},"
            + "\"extra\":{\"apikey\":{\"value\":\"7B4BCCDC\",\"type\":\"String\",\"required\":false},"
            + "\"bridgeTopic\":{\"value\":\"\",\"type\":\"String\",\"required\":false},"
            + "\"httpHost\":{\"value\":\"\",\"type\":\"String\",\"required\":false}}}");
        ParametersService.getInstance().update(param);
        param = ParametersService.getInstance().getByCategoryAndCode(Constants.BRIDGE_TYPE, "GPS");
        param.setValue("{\"filters\":{},"
            + "\"configuration\":{\"thingTypeCode\":{\"value\":\"\",\"type\":\"String\","
            + "\"required\":false},\"logRawMessages\":{\"value\":false,\"type\":\"Boolean\","
            + "\"required\":false},\"mqtt\":{\"value\":{\"connectionCode\":{\"value\":\"\","
            + "\"type\":\"String\",\"required\":false}},\"type\":\"JSON\",\"required\":false},"
            + "\"geoforce\":{\"value\":{\"host\":{\"value\":\"app.geoforce.com\",\"type\":\"String\","
            + "\"required\":false},\"path\":{\"value\":\"/feeds/asset_inventory.xml\","
            + "\"type\":\"String\",\"required\":false},\"port\":{\"value\":443,\"type\":\"Number\","
            + "\"required\":false},\"user\":{\"value\":\"datafeed@mojix.com\",\"type\":\"String\","
            + "\"required\":false},\"password\":{\"value\":\"AHmgooCk8l0jo95f7YSo\","
            + "\"type\":\"String\",\"required\":false},\"period\":{\"value\":60,\"type\":\"Number\","
            + "\"required\":false}},\"type\":\"JSON\",\"required\":false}},\"extra\":{\"apikey"
            + "\":{\"value\":\"7B4BCCDC\",\"type\":\"String\",\"required\":false},"
            + "\"bridgeTopic\":{\"value\":\"\",\"type\":\"String\",\"required\":false},"
            + "\"httpHost\":{\"value\":\"\",\"type\":\"String\",\"required\":false}}}");
        ParametersService.getInstance().update(param);
        param = ParametersService.getInstance().getByCategoryAndCode(Constants.BRIDGE_TYPE, "core");
        param.setValue(
            "{\"filters\":{\"shiftZoneRule\":{\"value\":{\"active\":{\"value\":false,\"type\":\"Boolean\","
            + "\"required\":false},\"shiftProperty\":{\"value\":\"shift\",\"type\":\"String\","
            + "\"required\":false},\"zoneViolationStatusProperty\":{\"value\":\"zoneViolationStatus"
            + "\",\"type\":\"String\",\"required\":false},"
            + "\"zoneViolationFlagProperty\":{\"value\":\"zoneViolationFlag\",\"type\":\"String\","
            + "\"required\":false}},\"type\":\"JSON\",\"required\":false},"
            + "\"sourceRule\":{\"value\":{\"active\":{\"value\":true,\"type\":\"Boolean\","
            + "\"required\":false}},\"type\":\"JSON\",\"required\":false}},"
            + "\"configuration\":{\"numberOfThreads\":{\"value\":1,\"type\":\"Number\","
            + "\"required\":false},\"mqtt\":{\"value\":{\"connectionCode\":{\"value\":\"\","
            + "\"type\":\"String\",\"required\":false},\"topics\":{\"value\":[],\"type\":\"ARRAY\","
            + "\"required\":false}},\"type\":\"JSON\",\"required\":false},"
            + "\"mongo\":{\"value\":{\"connectionCode\":{\"value\":\"\",\"type\":\"String\","
            + "\"required\":false}},\"type\":\"JSON\",\"required\":false},"
            + "\"CEPLogging\":{\"value\":{\"active\":{\"value\":false,\"type\":\"Boolean\","
            + "\"required\":false}},\"type\":\"JSON\",\"required\":false},"
            + "\"checkMultilevelReferences\":{\"value\":{\"active\":{\"value\":false,"
            + "\"type\":\"Boolean\",\"required\":false}},\"type\":\"JSON\",\"required\":false},"
            + "\"outOfOrderRule\":{\"value\":{\"active\":{\"value\":false,\"type\":\"Boolean\","
            + "\"required\":false}},\"type\":\"JSON\",\"required\":false},"
            + "\"timeOrderRule\":{\"value\":{\"active\":{\"value\":false,\"type\":\"Boolean\","
            + "\"required\":false},\"period\":{\"value\":0,\"type\":\"Number\",\"required\":false}},"
            + "\"type\":\"JSON\",\"required\":false},"
            + "\"interCacheEviction\":{\"value\":{\"active\":{\"value\":false,\"type\":\"Boolean\","
            + "\"required\":false}},\"type\":\"JSON\",\"required\":false},"
            + "\"swarmFilter\":{\"value\":{\"active\":{\"value\":false,\"type\":\"Boolean\","
            + "\"required\":false},\"timeGroupTimer\":{\"value\":5000,\"type\":\"Number\","
            + "\"required\":false},\"swarmAlgorithm\":{\"value\":\"followLastDetect\","
            + "\"type\":\"String\",\"required\":false},"
            + "\"thingTypes\":{\"value\":[{\"thingTypeCode\":\"default_rfid_thingtype\","
            + "\"udfGroupStatus\":\"groupStatus\",\"udfGroup\":\"grouping\","
            + "\"distanceFilter\":10000}],\"type\":\"ARRAY\",\"required\":false}},\"type\":\"JSON\","
            + "\"required\":false},"
            + "\"CEPEngineConfiguration\":{\"value\":{\"insertIntoDispatchPreserveOrder\":{\"value"
            + "\":false,\"type\":\"Boolean\",\"required\":false},"
            + "\"listenerDispatchPreserveOrder\":{\"value\":false,\"type\":\"Boolean\","
            + "\"required\":false},\"multipleInstanceMode\":{\"value\":false,\"type\":\"Boolean\","
            + "\"required\":false}},\"type\":\"JSON\",\"required\":false},"
            + "\"interCacheEvictionQueueSize\":{\"value\":20000,\"type\":\"Number\","
            + "\"required\":false},\"fixOlderSnapshotsQueueSize\":{\"value\":20000,"
            + "\"type\":\"Number\",\"required\":false},\"evaluateStats\":{\"value\":true,"
            + "\"type\":\"Boolean\",\"required\":false},"
            + "\"insertThingRetryConfig\":{\"value\":{\"maxRetries\":{\"value\":10,"
            + "\"type\":\"Number\",\"required\":false},\"retryInterval\":{\"value\":5000,"
            + "\"type\":\"Number\",\"required\":false}},\"type\":\"JSON\",\"required\":false}},"
            + "\"extra\":{\"apikey\":{\"value\":\"7B4BCCDC\",\"type\":\"String\",\"required\":false},"
            + "\"mysqlConnectionCode\":{\"value\":\"\",\"type\":\"String\",\"required\":false},"
            + "\"bridgeTopic\":{\"value\":\"\",\"type\":\"String\",\"required\":false},"
            + "\"httpHost\":{\"value\":\"\",\"type\":\"String\",\"required\":false}}}");
        ParametersService.getInstance().update(param);
        param = ParametersService.getInstance().getByCategoryAndCode(Constants.BRIDGE_TYPE, "smed");
        if (param != null) {
            param.setValue(
                "{\"filters\":{},\"configuration\":{\"documentUdfNamePrefix\":{\"value\":\"scd\","
                    + "\"type\":\"String\",\"required\":false},"
                    + "\"mqtt\":{\"value\":{\"active\":{\"value\":false,\"type\":\"Boolean\","
                    + "\"required\":false},\"connectionCode\":{\"value\":\"MQTT\","
                    + "\"type\":\"String\",\"required\":false}},\"type\":\"JSON\","
                    + "\"required\":false},\"outputTopic\":{\"value\":\"/v1/data1\","
                    + "\"type\":\"String\",\"required\":false},"
                    + "\"inputTopic\":{\"value\":\"/v1/data3\",\"type\":\"String\","
                    + "\"required\":false},\"outputFormat\":{\"value\":\"JSON\","
                    + "\"type\":\"String\",\"required\":false},\"numberOfThreads\":{\"value\":1,"
                    + "\"type\":\"Number\",\"required\":false},"
                    + "\"kafka\":{\"value\":{\"checkpoint\":{\"value\":false,"
                    + "\"type\":\"Boolean\",\"required\":false},"
                    + "\"connectionCode\":{\"value\":\"KAFKA\",\"type\":\"String\","
                    + "\"required\":false},\"active\":{\"value\":true,\"type\":\"Boolean\","
                    + "\"required\":false},\"consumerGroup\":{\"value\":\"group1\","
                    + "\"type\":\"String\",\"required\":false}},\"type\":\"JSON\","
                    + "\"required\":false},"
                    + "\"listener\":{\"value\":{\"kafkaCode\":{\"value\":\"KAFKA\","
                    + "\"type\":\"String\",\"required\":false},"
                    + "\"topic\":{\"value\":\"___v1___events,1,1\",\"type\":\"String\","
                    + "\"required\":false}},\"type\":\"JSON\",\"required\":false},"
                    + "\"commands\":{\"value\":{\"kafkaCode\":{\"value\":\"KAFKA\","
                    + "\"type\":\"String\",\"required\":false},"
                    + "\"topic\":{\"value\":\"/v1/commands/SMED,1,1\",\"type\":\"String\","
                    + "\"required\":false},\"consortiumCode\":{\"value\":\"TEST_RETAILER\","
                    + "\"type\":\"String\",\"required\":false},"
                    + "\"adapterCode\":{\"value\":\"BlockchainAdapter\",\"type\":\"String\","
                    + "\"required\":false}},\"type\":\"JSON\",\"required\":false}},"
                    + "\"extra\":{\"apikey\":{\"value\":\"7B4BCCDC\",\"type\":\"String\","
                    + "\"required\":false},\"bridgeTopic\":{\"value\":\"\",\"type\":\"String\","
                    + "\"required\":false},\"httpHost\":{\"value\":\"\",\"type\":\"String\",\"required\":false}}}");
            ParametersService.getInstance().update(param);
        }
        param = ParametersService.getInstance()
            .getByCategoryAndCode(Constants.BRIDGE_TYPE, "Thing_Joiner");
        if (param != null) {
            param.setValue("{\"filters\":{},\"configuration\":{\"thingTypeCodes\":{\"value"
                + "\":[\"default_rfid_thingtype\"],\"type\":\"ARRAY\",\"required\":false},"
                + "\"inputTopic\":{\"value\":\"/v1/data1\",\"type\":\"String\",\"required\":false},"
                + "\"outputTopic\":{\"value\":\"/v1/data2\",\"type\":\"String\",\"required\":false},"
                + "\"streamConfig\":{\"value\":{\"appId\":{\"value\":\"ThingJoiner-app\","
                + "\"type\":\"String\",\"required\":false},\"lingerMs\":{\"value\":5,\"type\":\"Number\","
                + "\"required\":false},\"numStreamThreads\":{\"value\":4,\"type\":\"Number\","
                + "\"required\":false},\"batchSize\":{\"value\":65536,\"type\":\"Number\","
                + "\"required\":false},\"stateDirPath\":{\"value\":\"/var/ThingJoiner/store\","
                + "\"type\":\"String\",\"required\":false}},\"type\":\"JSON\",\"required\":false},"
                + "\"kafka\":{\"value\":{\"connectionCode\":{\"value\":\"KAFKA\",\"type\":\"String\","
                + "\"required\":false}},\"type\":\"JSON\",\"required\":false},"
                + "\"mongo\":{\"value\":{\"connectionCode\":{\"value\":\"MONGO\",\"type\":\"String\","
                + "\"required\":false}},\"type\":\"JSON\",\"required\":false},"
                + "\"notificationService\":{\"value\":{\"active\":{\"value\":false,\"type\":\"Boolean\","
                + "\"required\":false},\"connectionCode\":{\"value\":\"MQTT\",\"type\":\"String\","
                + "\"required\":false},\"recipients\":{\"value\":[],\"type\":\"ARRAY\","
                + "\"required\":false}},\"type\":\"JSON\",\"required\":false}},\"extra\":{\"apikey"
                + "\":{\"value\":\"7B4BCCDC\",\"type\":\"String\",\"required\":false},"
                + "\"bridgeTopic\":{\"value\":\"\",\"type\":\"String\",\"required\":false},"
                + "\"httpHost\":{\"value\":\"\",\"type\":\"String\",\"required\":false}}}");
            ParametersService.getInstance().update(param);
        }
        param = ParametersService.getInstance()
            .getByCategoryAndCode(Constants.BRIDGE_TYPE, "Rules_Processor");
        if (param != null) {
            param.setValue("{\"filters\":{\"shiftZoneRule\":{\"value\":{\"active\":{\"value\":false,\"type\":\"Boolean\",\"required\":false},\"shiftProperty\":{\"value\":\"shift\","
                + "\"type\":\"String\",\"required\":false},\"zoneViolationStatusProperty\":{\"value\":\"zoneViolationStatus\",\"type\":\"String\",\"required\":false},"
                + "\"zoneViolationFlagProperty\":{\"value\":\"zoneViolationFlag\",\"type\":\"String\",\"required\":false}},\"type\":\"JSON\",\"required\":false},"
                + "\"sourceRule\":{\"value\":{\"active\":{\"value\":true,\"type\":\"Boolean\",\"required\":false}},\"type\":\"JSON\",\"required\":false},"
                + "\"swarmFilter\":{\"value\":{\"active\":{\"value\":false,\"type\":\"Boolean\",\"required\":false},\"outputTopic\":{\"value\":\"/v1/data1\","
                + "\"type\":\"String\",\"required\":false},\"timeGroupTimer\":{\"value\":25000,\"type\":\"Number\",\"required\":false},\"algorithm\":{\"value\":\"followLastDetect\","
                + "\"type\":\"String\",\"required\":false},\"thingTypes\":{\"value\":[{\"thingTypeCode\":\"default_rfid_thingtype\",\"udfGroup\":\"grouping\","
                + "\"distanceFilter\":10000,\"udfGroupStatus\":\"groupStatus\"}],\"type\":\"ARRAY\",\"required\":false}},\"type\":\"JSON\",\"required\":false}},"
                + "\"configuration\":{\"inputTopic\":{\"value\":\"/v1/data2\",\"type\":\"String\",\"required\":false},\"outputTopic\":{\"value\":\"/v1/data3\","
                + "\"type\":\"String\",\"required\":false},\"outOfOrderRule\":{\"value\":{\"active\":{\"value\":false,\"type\":\"Boolean\",\"required\":false}},"
                + "\"type\":\"JSON\",\"required\":false},\"CEPLogging\":{\"value\":{\"active\":{\"value\":false,\"type\":\"Boolean\",\"required\":false}},"
                + "\"type\":\"JSON\",\"required\":false},\"CEPEngineConfiguration\":{\"value\":{\"insertIntoDispatchPreserveOrder\":{\"value\":false,\"type\":\"Boolean\","
                + "\"required\":false},\"multipleInstanceMode\":{\"value\":false,\"type\":\"Boolean\",\"required\":false},\"listenerDispatchPreserveOrder\":{\"value\":false,"
                + "\"type\":\"Boolean\",\"required\":false}},\"type\":\"JSON\",\"required\":false},\"streamConfig\":{\"value\":{\"appId\":{\"value\":\"RulesProcessor-app\","
                + "\"type\":\"String\",\"required\":false},\"lingerMs\":{\"value\":5,\"type\":\"Number\",\"required\":false},\"numStreamThreads\":{\"value\":4,"
                + "\"type\":\"Number\",\"required\":false},\"batchSize\":{\"value\":65536,\"type\":\"Number\",\"required\":false},\"stateDirPath\":{\"value\":\"/var/RulesProcessor/store\","
                + "\"type\":\"String\",\"required\":false}},\"type\":\"JSON\",\"required\":false},\"kafka\":{\"value\":{\"connectionCode\":{\"value\":\"KAFKA\",\"type\":\"String\","
                + "\"required\":false}},\"type\":\"JSON\",\"required\":false},\"mongo\":{\"value\":{\"connectionCode\":{\"value\":\"MONGO\",\"type\":\"String\",\"required\":false}},"
                + "\"type\":\"JSON\",\"required\":false},\"notificationService\":{\"value\":{\"active\":{\"value\":false,\"type\":\"Boolean\",\"required\":false},"
                + "\"connectionCode\":{\"value\":\"MQTT\",\"type\":\"String\",\"required\":false},\"recipients\":{\"value\":[],\"type\":\"ARRAY\",\"required\":false}},"
                + "\"type\":\"JSON\",\"required\":false}},\"extra\":{\"apikey\":{\"value\":\"7B4BCCDC\","
                + "\"type\":\"String\",\"required\":false},\"bridgeTopic\":{\"value\":\"\","
                + "\"type\":\"String\",\"required\":false},\"httpHost\":{\"value\":\"\","
                + "\"type\":\"String\",\"required\":false}}}");
            ParametersService.getInstance().update(param);
        }
        param = ParametersService.getInstance()
            .getByCategoryAndCode(Constants.BRIDGE_TYPE, "Mongo_Injector");
        if (param != null) {
            param.setValue(
                "{\"filters\":{},\"configuration\":{\"inputTopic\":{\"value\":\"/v1/data3\","
                    + "\"type\":\"String\",\"required\":false},"
                    + "\"mongo\":{\"value\":{\"connectionCode\":{\"value\":\"MONGO\",\"type\":\"String\","
                    + "\"required\":false}},\"type\":\"JSON\",\"required\":false},"
                    + "\"streamConfig\":{\"value\":{\"appId\":{\"value\":\"MongoInjector-app\","
                    + "\"type\":\"String\",\"required\":false},\"lingerMs\":{\"value\":5,\"type\":\"Number\","
                    + "\"required\":false},\"numStreamThreads\":{\"value\":4,\"type\":\"Number\","
                    + "\"required\":false},\"batchSize\":{\"value\":65536,\"type\":\"Number\","
                    + "\"required\":false},\"stateDirPath\":{\"value\":\"/var/MongoInjector/store\","
                    + "\"type\":\"String\",\"required\":false}},\"type\":\"JSON\",\"required\":false},"
                    + "\"kafka\":{\"value\":{\"connectionCode\":{\"value\":\"KAFKA\",\"type\":\"String\","
                    + "\"required\":false}},\"type\":\"JSON\",\"required\":false},"
                    + "\"notificationService\":{\"value\":{\"active\":{\"value\":false,\"type\":\"Boolean\","
                    + "\"required\":false},\"connectionCode\":{\"value\":\"MQTT\",\"type\":\"String\","
                    + "\"required\":false},\"recipients\":{\"value\":[],\"type\":\"ARRAY\","
                    + "\"required\":false}},\"type\":\"JSON\",\"required\":false}},\"extra\":{\"apikey"
                    + "\":{\"value\":\"7B4BCCDC\",\"type\":\"String\",\"required\":false},"
                    + "\"bridgeTopic\":{\"value\":\"\",\"type\":\"String\",\"required\":false},"
                    + "\"httpHost\":{\"value\":\"\",\"type\":\"String\",\"required\":false}}}");
            ParametersService.getInstance().update(param);
        }
    }

    @Override public void migrateSQLAfter(String scriptPath) throws Exception {
    }
}
