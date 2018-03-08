package com.tierconnect.riot.migration.older;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tierconnect.riot.appcore.dao.CassandraUtils;
import com.tierconnect.riot.appcore.utils.Configuration;
import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.entities.EdgeboxRule;
import com.tierconnect.riot.iot.services.EdgeboxRuleService;
import com.tierconnect.riot.iot.services.EdgeboxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tierconnect.riot.migration.DBHelper;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Created by agutierrez on 4/22/15.
 */
@Deprecated
public class V_020102_020200 implements MigrationStepOld {

    private Logger logger = Logger.getLogger(V_020102_020200.class);

    @Override
    public List<Integer> getFromVersions() {
        return Arrays.asList(20102, 20103);
    }

    @Override
    public int getToVersion() {
        return 20200;
    }

    @Override
    public void migrateSQLBefore() throws Exception {
        DBHelper dbHelper = new DBHelper();
        String databaseType = dbHelper.getDataBaseType();
        dbHelper.executeSQLFile("sql/" + databaseType + "/V020102_to_020200.sql");

        CassandraUtils.init(Configuration.getProperty("cassandra.host"), Configuration.getProperty("cassandra.keyspace"));
        // Creating zone_count table in Cassandra
        logger.info("Executing CREATE INDEX IF NOT EXISTS field_value_value_idx ON riot_main.field_value (value);");
        CassandraUtils.getSession().execute(CassandraUtils.getSession().prepare("CREATE INDEX IF NOT EXISTS field_value_value_idx ON riot_main.field_value (value);").bind());

        // Creating zone_count table in Cassandra
        logger.info("Executing CREATE IF NOT EXISTS TABLE zone_count ( zone_name text, things_quantity bigint, PRIMARY KEY(zone_name);");
        CassandraUtils.getSession().execute(CassandraUtils.getSession().prepare("CREATE TABLE IF NOT EXISTS zone_count ( zone_name text, things_quantity bigint, PRIMARY KEY(zone_name));").bind());

    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateBridges();
        migrateAlienReaderRules();
    }

    @Override
    public void migrateSQLAfter() throws Exception {

    }

    private void migrateBridges()
    {
        // migrating CoreBridge configuration
        logger.info("Migrating bridges...");
        EdgeboxService edgeboxService  = EdgeboxService.getInstance();
        Edgebox edgebox = edgeboxService.selectByCode("MCB");
        String coreBridgeConfig = edgebox.getConfiguration();

        if (!coreBridgeConfig.contains("shiftProperty"))
        {
            ObjectMapper mapper = new ObjectMapper();
            try {
                JsonNode rootNode = mapper.readTree(coreBridgeConfig);
                JsonNode shiftZoneRuleNode = rootNode.get("shiftZoneRule");
                if (shiftZoneRuleNode != null)
                {
                    ((ObjectNode) shiftZoneRuleNode).put("shiftProperty", "shift");
                    ((ObjectNode) rootNode).put("shiftZoneRule", shiftZoneRuleNode);

                    coreBridgeConfig = rootNode.toString();
                    edgebox.setConfiguration(coreBridgeConfig);
                    edgeboxService.update(edgebox);
                    logger.info("Updated coreBridge configuration, added shiftProperty");
                }else{
                    logger.info("Could not find shiftZoneRule node in coreBridge configuration");
                }

            } catch (IOException e) {
                logger.warn(e);
            }
        }
    }

    private void migrateAlienReaderRules()
    {
        logger.info("Migrating Alien Reader rules...");
        EdgeboxRuleService edgeboxRuleService = EdgeboxRuleService.getInstance();
        List<EdgeboxRule> edgeBoxRuleList = edgeboxRuleService.selectByCodeAndAction("MCB", "AlienReaderGPIOSubscriber");
        String actionConfig, newActionConfig, ip = "10.100.1.61", username = "alien", password = "password";
        for (EdgeboxRule edgeboxRule : edgeBoxRuleList)
        {
            actionConfig = edgeboxRule.getOutputConfig();
            if (!actionConfig.contains("lightPinMap"))
            {
                logger.info("Updating action configuration, Rule="+edgeboxRule.getName());
                String[] lines = actionConfig.split("\n");

                try
                {
                    String authentication[] = lines[0].split("\\s+");
                    ip = authentication[1];
                    username = authentication[3];
                    password = authentication[5];
                }
                catch (Exception e){
                    logger.error("Action configuration from rule=" + edgeboxRule.getName() + " could not be read, please set it manually");
                }

                newActionConfig = getLightBuzzerRuleActionConfig(ip, username, password);
                edgeboxRule.setOutputConfig(newActionConfig);
                edgeboxRule.setOutput("AlienReaderGPIOSubscriber3");
                edgeboxRuleService.update(edgeboxRule);
            }
        }
    }

    public static String getLightBuzzerRuleActionConfig(String ip, String username, String password)
    {
        return "{\"ip\":\"" + ip + "\"," +
                "\"port\":23," +
                "\"username\":\"" + username + "\"," +
                "\"password\":\"" + password + "\"," +
                "\"times\":{\"lightOn\":5000,\"buzzerOn\":3000,\"buzzerOff\":1000}," +
                "\"lightPinMap\":{\"light1\":0,\"light2\":2,\"light3\":3,\"light4\":4}," +
                "\"buzzerPinMap\":{\"buzzer1\":1},\"zoneLightBuzzerMap\":{\"Exit1\":[\"light1\"," +
                "\"buzzer1\"],\"Exit2\":[\"light2\",\"buzzer1\"],\"Exit3\":[\"light3\",\"buzzer1\"]," +
                "\"Exit4\":[\"light4\",\"buzzer1\"]}}";

    }
}
