package com.tierconnect.riot.commons.dao.mongo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by achambi on 11/15/16.
 *
 */
public class MongoURIBuilderTest {

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void buildMongoURI() throws Exception {
        String result = MongoURIBuilder.buildMongoURI("admin", "control123!", "masterip", "sec1:1234,sec2:1235," +
                "sec3:1235", "riot_main", "replicaSetName", true, 3000, "adminDataBase", 15, null);
        assertNotEquals(null, result);
        assertNotEquals("", result);
        assertEquals("mongodb://admin:control123!@masterip,sec1:1234,sec2:1235," +
                "sec3:1235/riot_main?replicaSet=replicaSetName&ssl=true&connectTimeoutMS=3000&authSource" +
                "=adminDataBase&maxPoolSize=15", result);
    }

    @Test
    public void buildMongoURICase2() throws Exception {
        String result = MongoURIBuilder.buildMongoURI("admin", "control123!", "masterip", "sec1:1234,sec2:1235," +
                "sec3:1235", "riot_main", "replicaSetName", false, 4000, "adminDataBase", 16, "");
        assertNotEquals(null, result);
        assertNotEquals("", result);
        assertEquals("mongodb://admin:control123!@masterip,sec1:1234,sec2:1235," +
                "sec3:1235/riot_main?replicaSet=replicaSetName&ssl=false&connectTimeoutMS=4000&authSource" +
                "=adminDataBase&maxPoolSize=16", result);

        result = MongoURIBuilder.buildMongoURI("admin", "control123!", "masterip", "sec1:1234,sec2:1235," +
                "sec3:1235", "riot_main", "replicaSetName", null, null, "adminDataBase", 16, "");
        assertNotEquals(null, result);
        assertNotEquals("", result);
        assertEquals("mongodb://admin:control123!@masterip,sec1:1234,sec2:1235," +
                "sec3:1235/riot_main?replicaSet=replicaSetName&authSource" +
                "=adminDataBase&maxPoolSize=16", result);

        result = MongoURIBuilder.buildMongoURI("admin", "control123!", "masterip", "", "riot_main", "", null, null,
                "", null, null);
        assertNotEquals(null, result);
        assertNotEquals("", result);
        assertEquals("mongodb://admin:control123!@masterip/riot_main", result);

        result = MongoURIBuilder.buildMongoURI("admin", "control123!", "masterip", "", "riot_main", "", null, 0,
                "", 0, "secondary");
        assertNotEquals(null, result);
        assertNotEquals("", result);
        assertEquals("mongodb://admin:control123!@masterip/riot_main?readPreference=secondary", result);
    }

    @Test
    public void buildMongoURICase3() throws Exception {
        String result = MongoURIBuilder.buildMongoURI("admin", "control123!", "masterip", "", "riot_main",
                "replicaSetName", false, 4000, "adminDataBase", 16, "");
        assertNotEquals(null, result);
        assertNotEquals("", result);
        assertEquals("mongodb://admin:control123!@masterip/riot_main?replicaSet=replicaSetName&ssl=false" +
                "&connectTimeoutMS=4000&authSource=adminDataBase&maxPoolSize=16", result);

        result = MongoURIBuilder.buildMongoURI("admin", "control123!", "masterip", "", "riot_main",
                "", false, 4000, "adminDataBase", 16, "");
        assertNotEquals(null, result);
        assertNotEquals("", result);
        assertEquals("mongodb://admin:control123!@masterip/riot_main?ssl=false" +
                "&connectTimeoutMS=4000&authSource=adminDataBase&maxPoolSize=16", result);
    }


    @Test
    public void buildMongoURISHell() {
        String result = MongoURIBuilder.buildMongoURIShell(
                "mnstest",
                "mnstest123",
                "shadowcluster-shard-00-00-zwzrd.mongodb.net:37017",
                "",
                "riot_main",
                "",
                true,
                4000,
                "admin",
                16,
                "secondary");
        assertNotEquals(null, result);
        assertNotEquals("", result);
        assertEquals("mongo \"mongodb://shadowcluster-shard-00-00-zwzrd.mongodb" +
                ".net:37017/riot_main?readPreference=secondary&connectTimeoutMS=4000&maxPoolSize=16\" " +
                "--authenticationDatabase admin --quiet --ssl " +
                "--username mnstest --password mnstest123", result);
    }


    @Test
    public void buildMongoURISHellCase2() {
        String result = MongoURIBuilder.buildMongoURIShell(
                "mnstest",
                "mnstest123",
                "shadowcluster-shard-00-00-zwzrd.mongodb.net:37017",
                "",
                "riot_main",
                "",
                null,
                4000,
                "admin",
                16,
                "secondary");
        assertNotEquals(null, result);
        assertNotEquals("", result);
        assertEquals("mongo \"mongodb://shadowcluster-shard-00-00-zwzrd.mongodb" +
                ".net:37017/riot_main?readPreference=secondary&connectTimeoutMS=4000&maxPoolSize=16\" " +
                "--authenticationDatabase admin --quiet " +
                "--username mnstest --password mnstest123", result);
    }

    @Test
    public void buildMongoURISHellCase3() {
        String result = MongoURIBuilder.buildMongoURIShell(
                "mnstest",
                "mnstest123",
                "shadowcluster-shard-00-00-zwzrd.mongodb.net:37017",
                "",
                "riot_main",
                "",
                false,
                4000,
                "admin",
                16,
                "secondary");
        assertNotEquals(null, result);
        assertNotEquals("", result);
        assertEquals("mongo \"mongodb://shadowcluster-shard-00-00-zwzrd.mongodb" +
                ".net:37017/riot_main?readPreference=secondary&connectTimeoutMS=4000&maxPoolSize=16\" " +
                "--authenticationDatabase admin --quiet " +
                "--username mnstest --password mnstest123", result);
    }
}