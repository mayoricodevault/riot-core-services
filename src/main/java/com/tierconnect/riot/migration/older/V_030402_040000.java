package com.tierconnect.riot.migration.older;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.commons.dao.mongo.MongoDAOUtil;
import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.popdb.PopDBRequiredIOT;
import com.tierconnect.riot.iot.services.EdgeboxService;
import com.tierconnect.riot.migration.DBHelper;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Created by fflores on 1/7/2016.
 * Modify by achambi on 11/02/2016.
 */
@Deprecated
public class V_030402_040000 implements MigrationStepOld {

    static Logger logger = Logger.getLogger(V_030402_040000.class);

    @Override
    public List<Integer> getFromVersions() {
        return Arrays.asList(30402);
    }

    @Override
    public int getToVersion() {
        return 40000;
    }

    @Override
    public void migrateSQLBefore() throws Exception {
        DBHelper dbHelper = new DBHelper();
        String databaseType = dbHelper.getDataBaseType();
        dbHelper.executeSQLFile("sql/" + databaseType + "/V030403_to_040000.sql");
        migrateMongoIndexes();
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateCoreBridgeConfiguration();
        Group rootGroup = GroupService.getInstance().getRootGroup();
        PopDBRequiredIOT.populateStartFlexThingTypeOldTemplate(rootGroup);

    }

    @Override
    public void migrateSQLAfter() throws Exception {
    }

    private void migrateMongoIndexes()
    {
        DBObject idxThingsToDrop = null;
        DBObject idxThingSnapshotsToDrop = null;
        for(DBObject index : MongoDAOUtil.getInstance().things.getIndexInfo())
        {
            if(index.get("key")!=null &&
                    ((DBObject) index.get("key") ).keySet().contains("parent"))
            {
                idxThingsToDrop = index;
                break;
            }
        }
        for(DBObject index : MongoDAOUtil.getInstance().thingSnapshots.getIndexInfo())
        {

            if( index.get("key")!=null &&
                    ((DBObject) index.get("key") ).keySet().contains("parent"))
            {
                idxThingSnapshotsToDrop = index;
                break;
            }
        }

        if(idxThingsToDrop != null )
        {
            try{
                MongoDAOUtil.getInstance().things.dropIndex(idxThingsToDrop.get("name").toString());
            }catch(Exception e)
            {
                logger.info("migrateMongoIndexes: We cannot delete 'parent' index in things");
            }
            MongoDAOUtil.getInstance().things.createIndex(new BasicDBObject("parent._id", 1), "parent._id_");
        }
        if(idxThingSnapshotsToDrop!=null)
        {
            try{
                MongoDAOUtil.getInstance().thingSnapshots.dropIndex(idxThingSnapshotsToDrop.get("name").toString());
            }catch(Exception e)
            {
                logger.info("migrateMongoIndexes: We cannot delete 'parent' index in thingSnapshots");
            }
            MongoDAOUtil.getInstance().thingSnapshots.createIndex(
                    new BasicDBObject("value.parent._id", 1), "value.parent._id_" );
        }
    }

    public void migrateCoreBridgeConfiguration()
    {
        logger.info("Migrating CoreBridge configuration...");
        EdgeboxService edgeboxService  = EdgeboxService.getInstance();
        Edgebox edgebox = edgeboxService.selectByCode("MCB");
        String coreBridgeConfig = edgebox.getConfiguration();

        if (!coreBridgeConfig.contains("username"))
        {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = null;
            try {
                rootNode = mapper.readTree(coreBridgeConfig);
                JsonNode mongo = rootNode.get("mongo");
                if (mongo != null)
                {
                    ((ObjectNode) mongo).put("username", "admin");
                    ((ObjectNode) mongo).put("password", "control123!");
                    coreBridgeConfig = rootNode.toString();
                    edgebox.setConfiguration(coreBridgeConfig);
                    edgeboxService.update(edgebox);
                    logger.info("Updated CoreBridge configuration, it was added username and password");
                }else{
                    logger.error("MongoDB configuration cannot be found in CoreBridge configuration");
                }
            }
            catch (IOException e) {
                logger.error(e);
            }
        }

        if (!coreBridgeConfig.contains("sourceRule"))
        {
            ObjectMapper mapper = new ObjectMapper();
            try
            {   JsonNode rootNode = mapper.readTree(coreBridgeConfig);

                ObjectNode sourceRuleNode = mapper.createObjectNode();
                sourceRuleNode.put("active", 0);

                ((ObjectNode) rootNode).put("sourceRule", sourceRuleNode);

                coreBridgeConfig = rootNode.toString();
                edgebox.setConfiguration(coreBridgeConfig);
                edgeboxService.update(edgebox);

                logger.info("SourceRule default configuration has been added to coreBridge configuration");
            }
            catch (Exception e) {
                logger.error(e);
            }
        }

    }

}
