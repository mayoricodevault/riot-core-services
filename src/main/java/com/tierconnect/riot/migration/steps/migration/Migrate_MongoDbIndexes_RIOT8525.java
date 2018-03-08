package com.tierconnect.riot.migration.steps.migration;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.tierconnect.riot.commons.dao.mongo.MongoDAOUtil;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_MongoDbIndexes_RIOT8525 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_MongoDbIndexes_RIOT8525.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateMongoIndexes();
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

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
