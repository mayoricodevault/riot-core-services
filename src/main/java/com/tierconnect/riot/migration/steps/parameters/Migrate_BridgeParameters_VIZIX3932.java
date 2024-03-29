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
public class Migrate_BridgeParameters_VIZIX3932 implements MigrationStep {

    private static Logger logger = Logger.getLogger(Migrate_BridgeParameters_VIZIX3932.class);

    @Override public void migrateSQLBefore(String scriptPath) throws Exception {
    }

    @Override public void migrateHibernate() throws Exception {
        Parameters param =
            ParametersService.getInstance().getByCategoryAndCode(Constants.BRIDGE_TYPE, "edge");
        param.setValue(
            "{\"filters\":{\"zoneDwellFilter\":{\"value\":{\"active\":{\"value\":false,"
                + "\"type\":\"Boolean\",\"required\":false},\"unlockDistance\":{\"value\":25,"
                + "\"type\":\"Number\",\"required\":false},\"inZoneDistance\":{\"value\":10,"
                + "\"type\":\"Number\",\"required\":false},\"zoneDwellTime\":{\"value\":300,"
                + "\"type\":\"Number\",\"required\":false},"
                + "\"lastDetectTimeActive\":{\"value\":true,\"type\":\"Boolean\","
                + "\"required\":false},\"lastDetectTimeWindow\":{\"value\":0,\"type\":\"Number\","
                + "\"required\":false}},\"type\":\"JSON\",\"required\":false}},"
                + "\"configuration\":{\"thingTypeCode\":{\"value\":\"\",\"type\":\"String\","
                + "\"required\":false},\"logRawMessages\":{\"value\":false,\"type\":\"Boolean\","
                + "\"required\":false},\"numberOfThreads\":{\"value\":10,\"type\":\"Number\","
                + "\"required\":false},\"socketTimeout\":{\"value\":60000,\"type\":\"Number\","
                + "\"required\":false},\"dynamicTimeoutRate\":{\"value\":0,\"type\":\"Number\","
                + "\"required\":false},\"send500ErrorOnTimeout\":{\"value\":false,"
                + "\"type\":\"Boolean\",\"required\":false},"
                + "\"mqtt\":{\"value\":{\"connectionCode\":{\"value\":\"\",\"type\":\"String\","
                + "\"required\":false}},\"type\":\"JSON\",\"required\":false},"
                + "\"kafka\":{\"value\":{\"active\":{\"value\":false,\"type\":\"Boolean\","
                + "\"required\":false},\"connectionCode\":{\"value\":\"\",\"type\":\"String\","
                + "\"required\":false}},\"type\":\"JSON\",\"required\":false},"
                + "\"streaming\":{\"value\":{\"active\":{\"value\":false,\"type\":\"Boolean\","
                + "\"required\":false},\"bufferSize\":{\"value\":10,\"type\":\"Number\","
                + "\"required\":false}},\"type\":\"JSON\",\"required\":false},"
                + "\"evaluateStats\":{\"value\":true,\"type\":\"Boolean\",\"required\":false},"
                + "\"bridgePort\":{\"value\":9090,\"type\":\"Number\",\"required\":false}},"
                + "\"extra\":{}}");
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
            + "\"path\":{\"value\":\"/StoreReferenceData\",\"type\":\"String\","
            + "\"required\":false},\"pattern\":{\"value\":\"*.COLOUR\",\"type\":\"String\","
            + "\"required\":false},\"patternCaseSensitive\":{\"value\":false,"
            + "\"type\":\"Boolean\",\"required\":false},\"schedule\":{\"value\":\"0 0/10 * 1/1 * "
            + "? *\",\"type\":\"String\",\"required\":false},"
            + "\"configParser\":{\"value\":{\"parserType\":{\"value\":\"fixedlength\","
            + "\"type\":\"String\",\"required\":false},\"separator\":{\"value\":null,"
            + "\"type\":\"String\",\"required\":false},\"fieldLengths\":{\"value\":\"3,16,1\","
            + "\"type\":\"String\",\"required\":false},\"ignoreHeader\":{\"value\":false,"
            + "\"type\":\"Boolean\",\"required\":false},\"ignoreFooter\":{\"value\":true,"
            + "\"type\":\"Boolean\",\"required\":false},\"fieldNames\":{\"value\":[\"Code\","
            + "\"Description\",\"Action\"],\"type\":\"ARRAY\",\"required\":false},"
            + "\"columnNumberAsSerial\":{\"value\":0,\"type\":\"Number\",\"required\":false}},"
            + "\"type\":\"JSON\",\"required\":false},\"processPolicy\":{\"value\":\"Move\","
            + "\"type\":\"String\",\"required\":false},\"localBackupFolder\":{\"value\":\"/tmp\","
            + "\"type\":\"String\",\"required\":false},"
            + "\"ftpDestinationFolder\":{\"value\":\"processed/colour\",\"type\":\"String\","
            + "\"required\":false},\"mqtt\":{\"value\":{\"connectionCode\":{\"value\":\"\","
            + "\"type\":\"String\",\"required\":false}},\"type\":\"JSON\",\"required\":false}},"
            + "\"extra\":{}}");
        ParametersService.getInstance().update(param);
        param = ParametersService.getInstance().getByCategoryAndCode(Constants.BRIDGE_TYPE, "GPS");
        param.setValue(
            "{\"filters\":{},\"configuration\":{\"thingTypeCode\":{\"value\":\"\","
                + "\"type\":\"String\",\"required\":false},\"logRawMessages\":{\"value\":false,"
                + "\"type\":\"Boolean\",\"required\":false},"
                + "\"mqtt\":{\"value\":{\"connectionCode\":{\"value\":\"\",\"type\":\"String\","
                + "\"required\":false}},\"type\":\"JSON\",\"required\":false},"
                + "\"geoforce\":{\"value\":{\"host\":{\"value\":\"app.geoforce.com\","
                + "\"type\":\"String\",\"required\":false},"
                + "\"path\":{\"value\":\"/feeds/asset_inventory.xml\",\"type\":\"String\","
                + "\"required\":false},\"port\":{\"value\":443,\"type\":\"Number\","
                + "\"required\":false},\"user\":{\"value\":\"datafeed@mojix.com\","
                + "\"type\":\"String\",\"required\":false},"
                + "\"password\":{\"value\":\"AHmgooCk8l0jo95f7YSo\",\"type\":\"String\","
                + "\"required\":false},\"period\":{\"value\":60,\"type\":\"Number\","
                + "\"required\":false}},\"type\":\"JSON\",\"required\":false}},\"extra\":{}}");
        ParametersService.getInstance().update(param);
        param = ParametersService.getInstance().getByCategoryAndCode(Constants.BRIDGE_TYPE, "core");
        param.setValue(
            "{\"filters\":{\"pointInZoneRule\":{\"value\":{\"active\":{\"value\":true,"
                + "\"type\":\"Boolean\",\"required\":false}},\"type\":\"JSON\","
                + "\"required\":false},\"doorEventRule\":{\"value\":{\"active\":{\"value\":true,"
                + "\"type\":\"Boolean\",\"required\":false},\"action\":{\"value\":\"Undefined\","
                + "\"type\":\"String\",\"required\":false},\"sendZoneInEvent\":{\"value\":true,"
                + "\"type\":\"Boolean\",\"required\":false},\"sendZoneOutEvent\":{\"value\":true,"
                + "\"type\":\"Boolean\",\"required\":false}},\"type\":\"JSON\","
                + "\"required\":false},\"shiftZoneRule\":{\"value\":{\"active\":{\"value\":false,"
                + "\"type\":\"Boolean\",\"required\":false},"
                + "\"shiftProperty\":{\"value\":\"shift\",\"type\":\"String\","
                + "\"required\":false},"
                + "\"zoneViolationStatusProperty\":{\"value\":\"zoneViolationStatus\","
                + "\"type\":\"String\",\"required\":false},"
                + "\"zoneViolationFlagProperty\":{\"value\":\"zoneViolationFlag\","
                + "\"type\":\"String\",\"required\":false}},\"type\":\"JSON\","
                + "\"required\":false},\"sourceRule\":{\"value\":{\"active\":{\"value\":true,"
                + "\"type\":\"Boolean\",\"required\":false}},\"type\":\"JSON\","
                + "\"required\":false}},\"configuration\":{\"numberOfThreads\":{\"value\":1,"
                + "\"type\":\"Number\",\"required\":false},"
                + "\"mqtt\":{\"value\":{\"connectionCode\":{\"value\":\"\",\"type\":\"String\","
                + "\"required\":false},\"topics\":{\"value\":[\"/v1/data/ALEB/#\","
                + "\"/v1/data/APP2/#\",\"/v1/data/STAR/#\",\"/v1/data/STAR1/#\"],"
                + "\"type\":\"ARRAY\",\"required\":false}},\"type\":\"JSON\",\"required\":false},"
                + "\"mongo\":{\"value\":{\"connectionCode\":{\"value\":\"\",\"type\":\"String\","
                + "\"required\":false}},\"type\":\"JSON\",\"required\":false},"
                + "\"CEPLogging\":{\"value\":{\"active\":{\"value\":false,\"type\":\"Boolean\","
                + "\"required\":false}},\"type\":\"JSON\",\"required\":false},"
                + "\"checkMultilevelReferences\":{\"value\":{\"active\":{\"value\":false,"
                + "\"type\":\"Boolean\",\"required\":false}},\"type\":\"JSON\","
                + "\"required\":false},"
                + "\"outOfOrderRule\":{\"value\":{\"active\":{\"value\":false,"
                + "\"type\":\"Boolean\",\"required\":false}},\"type\":\"JSON\","
                + "\"required\":false},\"timeOrderRule\":{\"value\":{\"active\":{\"value\":false,"
                + "\"type\":\"Boolean\",\"required\":false},\"period\":{\"value\":0,"
                + "\"type\":\"Number\",\"required\":false}},\"type\":\"JSON\","
                + "\"required\":false},"
                + "\"interCacheEviction\":{\"value\":{\"active\":{\"value\":false,"
                + "\"type\":\"Boolean\",\"required\":false}},\"type\":\"JSON\","
                + "\"required\":false},\"swarmFilter\":{\"value\":{\"active\":{\"value\":false,"
                + "\"type\":\"Boolean\",\"required\":false},\"timeGroupTimer\":{\"value\":5000,"
                + "\"type\":\"Number\",\"required\":false},"
                + "\"swarmAlgorithm\":{\"value\":\"followLastDetect\",\"type\":\"String\","
                + "\"required\":false},"
                + "\"thingTypes\":{\"value\":[{\"thingtypeCode\":\"default_rfid_thingtype\","
                + "\"udfGroupStatus\":\"groupStatus\",\"udfGroup\":\"grouping\","
                + "\"distanceFilter\":10000}],\"type\":\"ARRAY\",\"required\":false}},"
                + "\"type\":\"JSON\",\"required\":false},"
                + "\"CEPEngineConfiguration\":{\"value\":{\"insertIntoDispatchPreserveOrder"
                + "\":{\"value\":false,\"type\":\"Boolean\",\"required\":false},"
                + "\"listenerDispatchPreserveOrder\":{\"value\":false,\"type\":\"Boolean\","
                + "\"required\":false},\"multipleInstanceMode\":{\"value\":false,"
                + "\"type\":\"Boolean\",\"required\":false}},\"type\":\"JSON\","
                + "\"required\":false},\"interCacheEvictionQueueSize\":{\"value\":20000,"
                + "\"type\":\"Number\",\"required\":false},"
                + "\"fixOlderSnapshotsQueueSize\":{\"value\":20000,\"type\":\"Number\","
                + "\"required\":false},\"evaluateStats\":{\"value\":true,\"type\":\"Boolean\","
                + "\"required\":false},"
                + "\"insertThingRetryConfig\":{\"value\":{\"maxRetries\":{\"value\":10,"
                + "\"type\":\"Number\",\"required\":false},\"retryInterval\":{\"value\":5000,"
                + "\"type\":\"Number\",\"required\":false}},\"type\":\"JSON\","
                + "\"required\":false}},\"extra\":{}}");
        ParametersService.getInstance().update(param);
        param = ParametersService.getInstance().getByCategoryAndCode(Constants.BRIDGE_TYPE, "smed");
        if (param != null) {
            param.setValue(
                "{\"filters\":{},\"configuration\":{\"mqtt\":{\"value\":{\"active\":{\"value\":false,"
                    + "\"type\":\"Boolean\",\"required\":false},"
                    + "\"connectionCode\":{\"value\":\"MQTT\",\"type\":\"String\","
                    + "\"required\":false}},\"type\":\"JSON\",\"required\":false},"
                    + "\"outputTopic\":{\"value\":\"/v1/data1\",\"type\":\"String\","
                    + "\"required\":false},\"inputTopic\":{\"value\":\"/v1/data3\","
                    + "\"type\":\"String\",\"required\":false},\"outputFormat\":{\"value\":\"JSON\","
                    + "\"type\":\"String\",\"required\":false},\"numberOfThreads\":{\"value\":1,"
                    + "\"type\":\"Number\",\"required\":false},"
                    + "\"kafka\":{\"value\":{\"checkpoint\":{\"value\":false,\"type\":\"Boolean\","
                    + "\"required\":false},\"connectionCode\":{\"value\":\"KAFKA\","
                    + "\"type\":\"String\",\"required\":false},\"active\":{\"value\":true,"
                    + "\"type\":\"Boolean\",\"required\":false},"
                    + "\"consumerGroup\":{\"value\":\"group1\",\"type\":\"String\","
                    + "\"required\":false}},\"type\":\"JSON\",\"required\":false},"
                    + "\"listener\":{\"value\":{\"kafkaCode\":{\"value\":\"KAFKA\","
                    + "\"type\":\"String\",\"required\":false},\"topic\":{\"value\":\"___v1___events,"
                    + "1,1\",\"type\":\"String\",\"required\":false}},\"type\":\"JSON\","
                    + "\"required\":false},"
                    + "\"commands\":{\"value\":{\"kafkaCode\":{\"value\":\"KAFKA\","
                    + "\"type\":\"String\",\"required\":false},"
                    + "\"topic\":{\"value\":\"/v1/commands/SMED,1,1\",\"type\":\"String\","
                    + "\"required\":false},\"consortiumCode\":{\"value\":\"TEST_RETAILER\","
                    + "\"type\":\"String\",\"required\":false},"
                    + "\"adapterCode\":{\"value\":\"BlockchainAdapter\",\"type\":\"String\","
                    + "\"required\":false}},\"type\":\"JSON\",\"required\":false}},"
                    + "\"extra\":{\"flows\":{\"BASICSUPPLYCHAIN\":{\"contractThingTypeCode"
                    + "\":\"SmartContract\",\"contractName\":\"Basic Supply Chain\","
                    + "\"fields\":[{\"id\":1001,\"name\":\"PO_Number\",\"type\":\"string\","
                    + "\"initialValue\":\"\"},{\"id\":1002,\"name\":\"PO_Vendor\","
                    + "\"type\":\"string\",\"initialValue\":\"\"},{\"id\":1003,\"name\":\"PO_Date\","
                    + "\"type\":\"string\",\"initialValue\":\"\"},{\"id\":1004,"
                    + "\"name\":\"PO_InCoTerms\",\"type\":\"string\",\"initialValue\":\"\"},"
                    + "{\"id\":1005,\"name\":\"PO_Carrier\",\"type\":\"string\","
                    + "\"initialValue\":\"\"},{\"id\":1006,\"name\":\"PO_DeliveryDate\","
                    + "\"type\":\"string\",\"initialValue\":\"\"},{\"id\":1007,"
                    + "\"name\":\"ASN_Number\",\"type\":\"string\",\"initialValue\":\"\"},"
                    + "{\"id\":1008,\"name\":\"ASN_ShippingDate\",\"type\":\"string\","
                    + "\"initialValue\":\"\"},{\"id\":1009,\"name\":\"BOL_Number\","
                    + "\"type\":\"string\",\"initialValue\":\"\"},{\"id\":1010,\"name\":\"BOL_Date\","
                    + "\"type\":\"string\",\"initialValue\":\"\"},{\"id\":1013,\"name\":\"palletID\","
                    + "\"type\":\"string\",\"initialValue\":\"\"},{\"id\":1014,"
                    + "\"name\":\"containerID\",\"type\":\"string\",\"initialValue\":\"\"},"
                    + "{\"id\":1015,\"name\":\"truckID\",\"type\":\"string\",\"initialValue\":\"\"}],"
                    + "\"states\":[{\"id\":10,\"name\":\"PO_ISSUED\"},{\"id\":20,"
                    + "\"name\":\"PO_ACKNOWLEDGED\"},{\"id\":30,\"name\":\"ORDER_FULFILLED\"},"
                    + "{\"id\":40,\"name\":\"ASN_ISSUED\"},{\"id\":50,\"name\":\"BOL_REQUESTED\"},"
                    + "{\"id\":60,\"name\":\"BOL_ISSUED\"},{\"id\":70,\"name\":\"ORDER_IN_TRANSIT\"},"
                    + "{\"id\":80,\"name\":\"ORDER_VERIFIED\"},{\"id\":90,\"name\":\"PO_PAID\"}],"
                    + "\"initialState\":10,\"roles\":[{\"id\":701,\"name\":\"Retailer\"},{\"id\":702,"
                    + "\"name\":\"Supplier\"},{\"id\":703,\"name\":\"Carrier\"},{\"id\":704,"
                    + "\"name\":\"EPCIS\"}],\"accounts\":[{\"id\":7010,\"roleId\":701,"
                    + "\"name\":\"walmart\","
                    + "\"address\":\"0x67891f98e42f1e4a683b3b2e6788c4f50b8a6627\"},{\"id\":7020,"
                    + "\"roleId\":702,\"name\":\"levis\","
                    + "\"address\":\"0xb6f976803005205ce328433d9157f9ed096b766a\"},{\"id\":7030,"
                    + "\"roleId\":703,\"name\":\"xpo\","
                    + "\"address\":\"0xcf6908a88b51bb3796f25c846dd61d09167b6ae3\"},{\"id\":7040,"
                    + "\"roleId\":704,\"name\":\"epcis listener\","
                    + "\"address\":\"0x038dc17a0827b438e932faf494efd54476ad8f63\"}],"
                    + "\"transitions\":[{\"startState\":10,\"endState\":20,\"name\":\"ackPO\","
                    + "\"roleId\":702,\"customFields\":[]},{\"startState\":20,\"endState\":30,"
                    + "\"name\":\"fulfillPO\",\"roleId\":702,\"customFields\":[1013]},"
                    + "{\"startState\":30,\"endState\":40,\"name\":\"issueASN\",\"roleId\":702,"
                    + "\"customFields\":[1007,1008]},{\"startState\":40,\"endState\":50,"
                    + "\"name\":\"requestBOL\",\"roleId\":702,\"customFields\":[]},"
                    + "{\"startState\":50,\"endState\":60,\"name\":\"issueBOL\",\"roleId\":703,"
                    + "\"customFields\":[1009,1010]},{\"startState\":60,\"endState\":70,"
                    + "\"name\":\"transitPO\",\"roleId\":703,\"customFields\":[1014,1015]},"
                    + "{\"startState\":70,\"endState\":80,\"name\":\"verifyOrder\",\"roleId\":701,"
                    + "\"customFields\":[]},{\"startState\":80,\"endState\":90,\"name\":\"verifyPO\","
                    + "\"roleId\":702,\"customFields\":[]}],\"contractRules\":[{\"id\":90001,"
                    + "\"type\":\"countComparison\",\"args\":{\"listId1\":81,\"listId2\":82}}],"
                    + "\"itemListLoadStates\":[{\"stateId\":20,\"roleId\":702,\"listIds\":[81]},"
                    + "{\"stateId\":25,\"roleId\":704,\"listIds\":[81]},{\"stateId\":80,"
                    + "\"roleId\":701,\"listIds\":[82]}],\"itemLists\":[{\"id\":81,"
                    + "\"thingTypeCode\":\"SmartItem\",\"fields\":[{\"id\":8101,"
                    + "\"name\":\"epcClass\",\"type\":\"string\"},{\"id\":8102,\"name\":\"bizStep\","
                    + "\"type\":\"string\"},{\"id\":8103,\"name\":\"bizLocation\","
                    + "\"type\":\"string\"}]},{\"id\":82,\"thingTypeCode\":\"SmartItem(Verification)"
                    + "\",\"fields\":[{\"id\":8201,\"name\":\"SKU\",\"type\":\"string\"}]}]}}}}");
            ParametersService.getInstance().update(param);
        }
        param = ParametersService.getInstance()
            .getByCategoryAndCode(Constants.BRIDGE_TYPE, "Thing_Joiner");
        if (param != null) {
            param.setValue("{\"filters\":{},\"configuration\":{\"thingTypeCodes\":{\"value"
                    + "\":[\"default_rfid_thingtype\"],\"type\":\"ARRAY\",\"required\":false},"
                    + "\"inputTopic\":{\"value\":\"/v1/data1\",\"type\":\"String\","
                    + "\"required\":false},\"outputTopic\":{\"value\":\"/v1/data2\","
                    + "\"type\":\"String\",\"required\":false},"
                    + "\"streamConfig\":{\"value\":{\"appId\":{\"value\":\"ThingJoiner-app\","
                    + "\"type\":\"String\",\"required\":false},\"lingerMs\":{\"value\":5,"
                    + "\"type\":\"Number\",\"required\":false},\"numStreamThreads\":{\"value\":4,"
                    + "\"type\":\"Number\",\"required\":false},\"batchSize\":{\"value\":65536,"
                    + "\"type\":\"Number\",\"required\":false},"
                    + "\"stateDirPath\":{\"value\":\"/var/ThingJoiner/store\",\"type\":\"String\","
                    + "\"required\":false}},\"type\":\"JSON\",\"required\":false},"
                    + "\"kafka\":{\"value\":{\"connectionCode\":{\"value\":\"KAFKA\","
                    + "\"type\":\"String\",\"required\":false}},\"type\":\"JSON\","
                    + "\"required\":false},"
                    + "\"mongo\":{\"value\":{\"connectionCode\":{\"value\":\"MONGO\","
                    + "\"type\":\"String\",\"required\":false}},\"type\":\"JSON\","
                    + "\"required\":false},"
                    + "\"notificationService\":{\"value\":{\"active\":{\"value\":false,"
                    + "\"type\":\"Boolean\",\"required\":false},"
                    + "\"connectionCode\":{\"value\":\"MQTT\",\"type\":\"String\","
                    + "\"required\":false},\"recipients\":{\"value\":[],\"type\":\"ARRAY\","
                    + "\"required\":false}},\"type\":\"JSON\",\"required\":false}},\"extra\":{}}");
            ParametersService.getInstance().update(param);
        }
        param = ParametersService.getInstance()
            .getByCategoryAndCode(Constants.BRIDGE_TYPE, "Rules_Processor");
        if (param != null) {
            param.setValue(
                "{\"filters\":{},\"configuration\":{\"inputTopic\":{\"value\":\"/v1/data2\","
                    + "\"type\":\"String\",\"required\":false},"
                    + "\"outputTopic\":{\"value\":\"/v1/data3\",\"type\":\"String\","
                    + "\"required\":false},\"doorEventRule\":{\"value\":{\"active\":{\"value\":true,"
                    + "\"type\":\"Boolean\",\"required\":false}},\"type\":\"JSON\","
                    + "\"required\":false},"
                    + "\"pointInZoneRule\":{\"value\":{\"active\":{\"value\":true,"
                    + "\"type\":\"Boolean\",\"required\":false}},\"type\":\"JSON\","
                    + "\"required\":false},\"shiftZoneRule\":{\"value\":{\"active\":{\"value\":false,"
                    + "\"type\":\"Boolean\",\"required\":false},"
                    + "\"shiftProperty\":{\"value\":\"shift\",\"type\":\"String\","
                    + "\"required\":false},"
                    + "\"zoneViolationStatusProperty\":{\"value\":\"zoneViolationStatus\","
                    + "\"type\":\"String\",\"required\":false},"
                    + "\"zoneViolationFlagProperty\":{\"value\":\"zoneViolationFlag\","
                    + "\"type\":\"String\",\"required\":false}},\"type\":\"JSON\","
                    + "\"required\":false},\"sourceRule\":{\"value\":{\"active\":{\"value\":true,"
                    + "\"type\":\"Boolean\",\"required\":false}},\"type\":\"JSON\","
                    + "\"required\":false},\"swarmFilter\":{\"value\":{\"active\":{\"value\":false,"
                    + "\"type\":\"Boolean\",\"required\":false},"
                    + "\"outputTopic\":{\"value\":\"/v1/data1\",\"type\":\"String\","
                    + "\"required\":false},\"timeGroupTimer\":{\"value\":25000,\"type\":\"Number\","
                    + "\"required\":false},\"algorithm\":{\"value\":\"followLastDetect\","
                    + "\"type\":\"String\",\"required\":false},"
                    + "\"thingTypes\":{\"value\":[{\"thingTypeCode\":\"default_rfid_thingtype \","
                    + "\"udfGroup\":\"grouping\",\"distanceFilter\":10000,"
                    + "\"udfGroupStatus\":\"groupStatus\"}],\"type\":\"ARRAY\",\"required\":false}},"
                    + "\"type\":\"JSON\",\"required\":false},"
                    + "\"outOfOrderRule\":{\"value\":{\"active\":{\"value\":false,"
                    + "\"type\":\"Boolean\",\"required\":false}},\"type\":\"JSON\","
                    + "\"required\":false},\"CEPLogging\":{\"value\":{\"active\":{\"value\":false,"
                    + "\"type\":\"Boolean\",\"required\":false}},\"type\":\"JSON\","
                    + "\"required\":false},"
                    + "\"CEPEngineConfiguration\":{\"value\":{\"insertIntoDispatchPreserveOrder"
                    + "\":{\"value\":false,\"type\":\"Boolean\",\"required\":false},"
                    + "\"multipleInstanceMode\":{\"value\":false,\"type\":\"Boolean\","
                    + "\"required\":false},\"listenerDispatchPreserveOrder\":{\"value\":false,"
                    + "\"type\":\"Boolean\",\"required\":false}},\"type\":\"JSON\","
                    + "\"required\":false},"
                    + "\"streamConfig\":{\"value\":{\"appId\":{\"value\":\"RulesProcessor-app\","
                    + "\"type\":\"String\",\"required\":false},\"lingerMs\":{\"value\":5,"
                    + "\"type\":\"Number\",\"required\":false},\"numStreamThreads\":{\"value\":4,"
                    + "\"type\":\"Number\",\"required\":false},\"batchSize\":{\"value\":65536,"
                    + "\"type\":\"Number\",\"required\":false},"
                    + "\"stateDirPath\":{\"value\":\"/var/RulesProcessor/store\",\"type\":\"String\","
                    + "\"required\":false}},\"type\":\"JSON\",\"required\":false},"
                    + "\"kafka\":{\"value\":{\"connectionCode\":{\"value\":\"KAFKA\","
                    + "\"type\":\"String\",\"required\":false}},\"type\":\"JSON\","
                    + "\"required\":false},"
                    + "\"mongo\":{\"value\":{\"connectionCode\":{\"value\":\"MONGO\","
                    + "\"type\":\"String\",\"required\":false}},\"type\":\"JSON\","
                    + "\"required\":false},"
                    + "\"notificationService\":{\"value\":{\"active\":{\"value\":false,"
                    + "\"type\":\"Boolean\",\"required\":false},"
                    + "\"connectionCode\":{\"value\":\"MQTT\",\"type\":\"String\","
                    + "\"required\":false},\"recipients\":{\"value\":[],\"type\":\"ARRAY\","
                    + "\"required\":false}},\"type\":\"JSON\",\"required\":false}},\"extra\":{}}");
            ParametersService.getInstance().update(param);
        }
        param = ParametersService.getInstance()
            .getByCategoryAndCode(Constants.BRIDGE_TYPE, "Mongo_Injector");
        if (param != null) {
            param.setValue(
                "{\"filters\":{},\"configuration\":{\"inputTopic\":{\"value\":\"/v1/data3\","
                    + "\"type\":\"String\",\"required\":false},"
                    + "\"mongo\":{\"value\":{\"connectionCode\":{\"value\":\"MONGO\","
                    + "\"type\":\"String\",\"required\":false}},\"type\":\"JSON\","
                    + "\"required\":false},"
                    + "\"streamConfig\":{\"value\":{\"appId\":{\"value\":\"MongoInjector-app\","
                    + "\"type\":\"String\",\"required\":false},\"lingerMs\":{\"value\":5,"
                    + "\"type\":\"Number\",\"required\":false},\"numStreamThreads\":{\"value\":4,"
                    + "\"type\":\"Number\",\"required\":false},\"batchSize\":{\"value\":65536,"
                    + "\"type\":\"Number\",\"required\":false},"
                    + "\"stateDirPath\":{\"value\":\"/var/MongoInjector/store\",\"type\":\"String\","
                    + "\"required\":false}},\"type\":\"JSON\",\"required\":false},"
                    + "\"kafka\":{\"value\":{\"connectionCode\":{\"value\":\"KAFKA\","
                    + "\"type\":\"String\",\"required\":false}},\"type\":\"JSON\","
                    + "\"required\":false},"
                    + "\"notificationService\":{\"value\":{\"active\":{\"value\":false,"
                    + "\"type\":\"Boolean\",\"required\":false},"
                    + "\"connectionCode\":{\"value\":\"MQTT\",\"type\":\"String\","
                    + "\"required\":false},\"recipients\":{\"value\":[],\"type\":\"ARRAY\","
                    + "\"required\":false}},\"type\":\"JSON\",\"required\":false}},\"extra\":{}}");
            ParametersService.getInstance().update(param);
        }
    }

    @Override public void migrateSQLAfter(String scriptPath) throws Exception {
    }
}
