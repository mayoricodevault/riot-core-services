package com.tierconnect.riot.iot.dao.mongo;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.tierconnect.riot.commons.dao.mongo.MongoDAOUtil;
import com.tierconnect.riot.sdk.dao.MongoExecutionException;

import org.apache.log4j.Logger;

/**
 * Created by angelchambi on 5/27/16. Class Singleton to Mongo Migration DAO
 */
public class MongoMigrationDAO {

    static MongoMigrationDAO instance;
    private static Logger logger = Logger.getLogger(MongoMigrationDAO.class);
    private DBCollection mongoMigrationCollection;

    static {
        instance = new MongoMigrationDAO();
        instance.setup();
    }

    /**
     * Setup Instance
     */
    public void setup() {
        mongoMigrationCollection = MongoDAOUtil.getInstance().db.getCollection("mongoMigration");
    }

    /**
     * get MongoMigrationDAO Instance.
     *
     * @return A MongoMigrationDAO Instance.
     */
    public static MongoMigrationDAO getInstance() {
        return instance;
    }


    /**
     * Inserts new mongo Thing Use this method to insert the whole thing in just one call to database
     */
    public DBObject getLastMigrationStep() throws MongoExecutionException {
        DBObject mongoMigration = null;
        try {
            DBCursor cursorLastMongo = mongoMigrationCollection.find().sort(new BasicDBObject("endDate", -1));

            if (cursorLastMongo.hasNext()) {
                mongoMigration = cursorLastMongo.next();
            }
            logger.info("Get Mongo migration: " + mongoMigration);
        } catch (Exception e) {
            throw new MongoExecutionException(e);
        }
        return mongoMigration;
    }
}
