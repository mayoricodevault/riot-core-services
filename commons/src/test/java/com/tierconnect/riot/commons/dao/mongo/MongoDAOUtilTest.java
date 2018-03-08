package com.tierconnect.riot.commons.dao.mongo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by achambi on 11/14/16.
 */
public class MongoDAOUtilTest {
    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void initReplicaSet() throws Exception {

    }

    @Test
    public void getInstance() throws Exception {

    }

    @Test
    public void buildMongoURI() throws Exception {

    }

    @Test
    public void buildMongoURISecondary() throws Exception {

    }

    @Test
    public void setupMongodb() throws Exception {

    }

    @Test
    public void checkIndexes() throws Exception {

    }

    @Test
    public void runCommand() throws Exception {
        MongoDAOUtil.setupMongodb("127.0.0.1:27017",
                "",
                "",
                /*SSL flag*/
                false,
                "root",
                "control123!",
                "admin",
                "riot_main",
                "primary",
                "secondary",
                /*Shardding*/
                false,
                30,
                10);
        Object result = MongoDAOUtil.getInstance().runCommand("db.things.find({}).limit(10);");
        assertNotEquals(null, result);
    }

    @Test
    public void runCommandFileResult() throws Exception {

    }

    @Test
    public void runCommandBasicDBObjectResult() throws Exception {

    }

    @Test
    public void createCommand() throws Exception {

    }

    @Test
    public void fileToJSON() throws Exception {

    }

    @Test
    public void createShellAddress() throws Exception {

    }

    @Test
    public void runFileCommand() throws Exception {

    }

}