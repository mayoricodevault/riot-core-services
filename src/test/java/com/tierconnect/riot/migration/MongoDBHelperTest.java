package com.tierconnect.riot.migration;

import com.tierconnect.riot.appcore.utils.Configuration;
import com.tierconnect.riot.commons.dao.mongo.MongoDAOUtil;
import com.tierconnect.riot.sdk.dao.UserException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.math.NumberUtils.isNumber;


/**
 * Created by angelchambi on 6/3/16.
 * A class to Test MongoDBHelper.
 */
public class MongoDBHelperTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {

    }

//    @Test
//    public void testExecuteMongoFile() throws Exception {
//        MongoDBHelper dbHelper = new MongoDBHelper();
//        dbHelper.executeMongoFile("no-sql/mongo/migrateZonePropertiesTimes.js");
//        CommandResult commandResult = dbHelper.executeMongoFile("no-sql/mongo/migrateZonePropertiesTimes.js");
//        org.junit.Assert.assertEquals(commandResult.ok(),true);
//    }

    @Test
    public void testCreateJSONFormatterNull() throws Exception {


        String exceptionMessage = "'Mongo script' can not be executed.";
        try {
            MongoDBHelper dbHelper = new MongoDBHelper();
            dbHelper.executeMongoFile(null);
            Assert.fail("Should have thrown an exception");
        } catch (UserException e) {
            Assert.assertEquals(exceptionMessage,e.getMessage());
        }
    }

    @Test
    public void testCreateFile() throws Exception {

        MongoDAOUtil.setupMongodb("localhost:27017",
                "",
                "",
                false,
                "admin",
                "control123!",
                "admin",
                "riot_main",
                "primary",
                "secondary",
                false,
                0,
                0);

        String exceptionMessage = "'Mongo script' can not be executed.";
        MongoDBHelper dbHelper = new MongoDBHelper();
        dbHelper.executeMongoFile("no-sql/mongo/V040200_to_040300.js");
        Assert.assertTrue(true);
    }

    @Test
    public void testExecuteMongoQuery() throws Exception {

    }
}
