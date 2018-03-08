package com.tierconnect.riot.migration.steps.edgebox;

import com.tierconnect.riot.iot.entities.Parameters;
import com.tierconnect.riot.iot.services.ParametersService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

/**
 * Defines the initial parameters for the Smart Contract Edge Bridge.
 */
public class Migrate_AddParamsForSmedBridge_VIZIX1957 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_AddParamsForSmedBridge_VIZIX1957.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        addParamsForSmedBridge();
    }

    private void addParamsForSmedBridge() {
        logger.info("Configuring SMED Bridge");
        ParametersService service = ParametersService.getInstance();
        String bridgeType = "BRIDGE_TYPE";
        String code = "smed";
        Parameters parameters = service.getByCategoryAndCode(bridgeType, code);
        if(parameters != null) {
            logger.info("SMED Bridge parameters already present in DB");
        }
        else {
            String appResourceCode = "@SYSTEM_PARAMETERS_BRIDGE_TYPE_SMED";
            String value = "{\"mqtt\":{\"active\":false,\"connectionCode\":\"MQTT\"},\"outputTopic\":\"/v1/data1\",\"inputTopic\":\"/v1/data3\",\"outputFormat\":\"JSON\",\"numberOfThreads\":1,\"kafka\":{\"checkpoint\":false,\"connectionCode\":\"KAFKA\",\"active\":true," +
                    "\"consumerGroup\":\"group1\"},\"listener\":{\"kafkaCode\":\"KAFKA\"," +
                    "\"topic\":\"___v1___events,1,1\"},\"commands\":{\"kafkaCode\":\"KAFKA\",\"topic\":\"/v1/commands/SMED,1,1\"," +
                    "\"consortiumCode\":\"TEST_RETAILER\",\"adapterCode\":\"BlockchainAdapter\"}," +
                    "\"flows\":{\"BASICSUPPLYCHAIN\":{\"contractThingTypeCode\":\"SmartContract\",\"contractName\":\"Basic Supply Chain\"," +
                    "\"fields\":[{\"id\":1001,\"name\":\"PO_Number\",\"type\":\"string\",\"initialValue\":\"\"},{" +
                    "\"id\":1002,\"name\":\"PO_Vendor\",\"type\":\"string\",\"initialValue\":\"\"},{\"id\":1003," +
                    "\"name\":\"PO_Date\",\"type\":\"string\",\"initialValue\":\"\"},{\"id\":1004,\"name\":\"PO_InCoTerms\"," +
                    "\"type\":\"string\",\"initialValue\":\"\"},{\"id\":1005,\"name\":\"PO_Carrier\",\"type\":\"string\"," +
                    "\"initialValue\":\"\"},{\"id\":1006,\"name\":\"PO_DeliveryDate\",\"type\":\"string\",\"initialValue\":" +
                    "\"\"},{\"id\":1007,\"name\":\"ASN_Number\",\"type\":\"string\",\"initialValue\":\"\"},{\"id\":1008," +
                    "\"name\":\"ASN_ShippingDate\",\"type\":\"string\",\"initialValue\":\"\"},{\"id\":1009,\"name\":\"BOL_Number\"," +
                    "\"type\":\"string\",\"initialValue\":\"\"},{\"id\":1010,\"name\":\"BOL_Date\",\"type\":\"string\"," +
                    "\"initialValue\":\"\"},{\"id\":1013,\"name\":\"palletID\",\"type\":\"string\",\"initialValue\":\"\"},{" +
                    "\"id\":1014,\"name\":\"containerID\",\"type\":\"string\",\"initialValue\":\"\"},{\"id\":1015," +
                    "\"name\":\"truckID\",\"type\":\"string\",\"initialValue\":\"\"}],\"states\":[{\"id\":10," +
                    "\"name\":\"PO_ISSUED\"},{\"id\":20,\"name\":\"PO_ACKNOWLEDGED\"},{\"id\":30,\"name\":\"ORDER_FULFILLED\"},{" +
                    "\"id\":40,\"name\":\"ASN_ISSUED\"},{\"id\":50,\"name\":\"BOL_REQUESTED\"},{\"id\":60,\"name\":\"BOL_ISSUED\"},{" +
                    "\"id\":70,\"name\":\"ORDER_IN_TRANSIT\"},{\"id\":80,\"name\":\"ORDER_VERIFIED\"},{\"id\":90," +
                    "\"name\":\"PO_PAID\"}],\"initialState\":10,\"roles\":[{\"id\":701,\"name\":\"Retailer\"},{\"id\":702," +
                    "\"name\":\"Supplier\"},{\"id\":703,\"name\":\"Carrier\"},{\"id\":704,\"name\":\"EPCIS\"}]," +
                    "\"accounts\":[{\"id\":7010,\"roleId\":701,\"name\":\"walmart\",\"address\":\"0x67891f98e42f1e4a683b3b2e6788c4f50b8a6627\"},{" +
                    "\"id\":7020,\"roleId\":702,\"name\":\"levis\",\"address\":\"0xb6f976803005205ce328433d9157f9ed096b766a\"},{" +
                    "\"id\":7030,\"roleId\":703,\"name\":\"xpo\",\"address\":\"0xcf6908a88b51bb3796f25c846dd61d09167b6ae3\"},{" +
                    "\"id\":7040,\"roleId\":704,\"name\":\"epcis listener\",\"address\":\"0x038dc17a0827b438e932faf494efd54476ad8f63\"}]," +
                    "\"transitions\":[{\"startState\":10,\"endState\":20,\"name\":\"ackPO\",\"roleId\":702,\"customFields\":[]},{" +
                    "\"startState\":20,\"endState\":30,\"name\":\"fulfillPO\",\"roleId\":702,\"customFields\":[1013]},{" +
                    "\"startState\":30,\"endState\":40,\"name\":\"issueASN\",\"roleId\":702,\"customFields\":[1007,1008]},{" +
                    "\"startState\":40,\"endState\":50,\"name\":\"requestBOL\",\"roleId\":702,\"customFields\":[]},{" +
                    "\"startState\":50,\"endState\":60,\"name\":\"issueBOL\",\"roleId\":703,\"customFields\":[1009,1010]},{" +
                    "\"startState\":60,\"endState\":70,\"name\":\"transitPO\",\"roleId\":703,\"customFields\":[1014,1015]},{" +
                    "\"startState\":70,\"endState\":80,\"name\":\"verifyOrder\",\"roleId\":701,\"customFields\":[]},{" +
                    "\"startState\":80,\"endState\":90,\"name\":\"verifyPO\",\"roleId\":702,\"customFields\":[]}]," +
                    "\"contractRules\":[{\"id\":90001,\"type\":\"countComparison\",\"args\":{\"listId1\":81,\"listId2\":82}}]," +
                    "\"itemListLoadStates\":[{\"stateId\":20,\"roleId\":702,\"listIds\":[81]},{\"stateId\":25," +
                    "\"roleId\":704,\"listIds\":[81]},{\"stateId\":80,\"roleId\":701,\"listIds\":[82]}],\"itemLists\":[{\"id\":81," +
                    "\"thingTypeCode\":\"SmartItem\",\"fields\":[{\"id\":8101,\"name\":\"epcClass\",\"type\":\"string\"},{" +
                    "\"id\":8102,\"name\":\"bizStep\",\"type\":\"string\"},{\"id\":8103,\"name\":\"bizLocation\"," +
                    "\"type\":\"string\"}]},{\"id\":82,\"thingTypeCode\":\"SmartItem(Verification)\",\"fields\":[{" +
                    "\"id\":8201,\"name\":\"SKU\",\"type\":\"string\"}]}]}}}";

            parameters = new Parameters(bridgeType, code, appResourceCode, value);
            service.insert(parameters);
            logger.info("SMED Bridge parameters migrated successfully");
        }
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }
}