package com.tierconnect.riot.migration.older;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.popdb.PopDBRequiredIOT;
import com.tierconnect.riot.iot.services.EdgeboxService;
import com.tierconnect.riot.migration.DBHelper;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.List;


/**
 * Created by cfernandez
 * on 9/25/15.
 */
@Deprecated
public class V_030102_030200 implements MigrationStepOld
{

    static Logger logger = Logger.getLogger(V_030102_030200.class);

    @Override
    public List<Integer> getFromVersions() {
        return Arrays.asList(30102);

    }

    @Override
    public int getToVersion() {
        return 30200;
    }

    @Override
    public void migrateSQLBefore() throws Exception {
        DBHelper dbHelper = new DBHelper();
        String databaseType = dbHelper.getDataBaseType();
        dbHelper.executeSQLFile("sql/" + databaseType + "/V030102_to_030200.sql");
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateCoreBridgeConfiguration();
        PopDBRequiredIOT.populateNotificationTemplate();
    }

    @Override
    public void migrateSQLAfter() throws Exception {

    }

    private void migrateCoreBridgeConfiguration()
    {
        logger.info("Migrating coreBridge configuration...");

        EdgeboxService edgeboxService  = EdgeboxService.getInstance();
        Edgebox edgebox = edgeboxService.selectByCode("MCB");
        String coreBridgeConfig = edgebox.getConfiguration();

        if (coreBridgeConfig.contains("cassandra"))
        {
            ObjectMapper mapper = new ObjectMapper();
            try
            {
                JsonNode rootNode = mapper.readTree(coreBridgeConfig);
                ((ObjectNode) rootNode).remove("cassandra");

                coreBridgeConfig = rootNode.toString();
                edgebox.setConfiguration(coreBridgeConfig);
                edgeboxService.update(edgebox);

                logger.info("Updated coreBridge configuration, removed cassandra configuration node.");
            }
            catch (Exception e) {
                logger.error(e);
            }
        }
    }

}
