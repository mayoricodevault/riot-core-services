package com.tierconnect.riot.api.mongoShell.operations;

import com.mysema.commons.lang.URLEncoder;
import com.tierconnect.riot.api.mongoShell.testUtils.MongoDataBaseTestUtils;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.bson.Document;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by achambi on 2/7/17.
 * Class to test Operation ExecutorTest.
 */
public class OperationExecutorTest {

    private static final String mongoDataBase = "riot_main_test";
    private static final String mongoDataBasePath = "databaseTest/riotMainTestComparator";
    private static final String MongoTestFunctionName = "\"OperationFuncTest\"";

    @Before
    public void setUp() throws Exception {
        MongoDataBaseTestUtils.dropDataBase(mongoDataBase);
        MongoDataBaseTestUtils.createDataBase(mongoDataBase, mongoDataBasePath);
        MongoDataBaseTestUtils.createDummyFunction(MongoTestFunctionName, mongoDataBase);
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void executeFunction() throws Exception {
        OperationExecutor operationExecutor = Mockito.spy(new OperationExecutor());
        Mockito.when(operationExecutor.getConnection()).thenReturn("mongo \"mongodb://127.0.0.1:27017/riot_main_test" +
                "?readPreference=primary&connectTimeoutMS=3000&maxPoolSize=50\" --authenticationDatabase admin " +
                "--quiet --username admin --password control123!");
        Map<String, Object> options = new HashMap<>();
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("pageSize", 5);
        pagination.put("pageNumber", 1);
        options.put("options", pagination);
        Document result = operationExecutor.executeFunction(OperationExecutorTest.MongoTestFunctionName, options,
                "/tmp/resultName.json");
        String expected = "Document{{options={\"options\":{\"pageSize\":5,\"pageNumber\":1}}, title=List of Things, " +
                "totalRows=53, data=[[org.bson.BsonUndefined@0, org.bson.BsonUndefined@0, org.bson.BsonUndefined@0], " +
                "[org.bson.BsonUndefined@0, testNull001, org.bson.BsonUndefined@0], [org.bson.BsonUndefined@0, " +
                "testNull002, org.bson.BsonUndefined@0], [org.bson.BsonUndefined@0, testNull003, org.bson" +
                ".BsonUndefined@0], [org.bson.BsonUndefined@0, testNull004, org.bson.BsonUndefined@0], [org.bson" +
                ".BsonUndefined@0, testNull010, org.bson.BsonUndefined@0], [Asset, 000000000000000000483, " +
                "000000000000000000483], [Asset, 000000000000000000484, 000000000000000000484], [Asset, " +
                "000000000000000000485, 000000000000000000485], [Asset, 000000000000000000486, " +
                "000000000000000000486], [Asset, 000000000000000000487, 000000000000000000487], [Default GPS Thing " +
                "Type, GPS1234567890, GPS1234567890], [Default RFID Thing Type, RFID1234567890, RFID1234567890], " +
                "[Default RFID Thing Type, 000000000000000000473, 000000000000000000473], [Default RFID Thing Type, " +
                "000000000000000000474, 000000000000000000474], [Default RFID Thing Type, 000000000000000000475, " +
                "000000000000000000475], [Default RFID Thing Type, 000000000000000000476, 000000000000000000476], " +
                "[Default RFID Thing Type, 000000000000000000477, 000000000000000000477], [Default RFID Thing Type, " +
                "000000000000000000478, 000000000000000000478], [Default RFID Thing Type, 000000000000000000479, " +
                "000000000000000000479], [Default RFID Thing Type, 000000000000000000480, 000000000000000000480], " +
                "[Default RFID Thing Type, 000000000000000000481, 000000000000000000481], [Default RFID Thing Type, " +
                "000000000000000000482, 000000000000000000482], [Jackets, J00001, Jacket1], [Jackets, J00002, " +
                "Jacket2], [Jackets, J00003, Jacket3], [Jackets, J00004, Jacket4], [Jackets, J00005, Jacket5], " +
                "[Pants, P00001, Pants1], [Pants, P00002, Pants2], [Pants, P00003, Pants3], [Pants, P00004, Pants4], " +
                "[Pants, P00005, Pants5], [ShippingOrder, SO00001, ShippingOrder1], [ShippingOrder, SO00002, " +
                "ShippingOrder2], [ShippingOrder, SO00003, ShippingOrder3], [ShippingOrder, SO00004, ShippingOrder4]," +
                " [ShippingOrder, SO00005, ShippingOrder5], [Tag, 000000000000000000001, 000000000000000000001], " +
                "[Tag, 000000000000000000002, 000000000000000000002], [Tag, 000000000000000000003, " +
                "000000000000000000003], [Tag, 000000000000000000004, 000000000000000000004], [Tag, " +
                "000000000000000000005, 000000000000000000005], [ThingNumber, TN0001, TN0001], [ThingNumber, TN0002, " +
                "TN0002], [ThingNumber, TN0003, TN0003], [ThingNumber, TN0004, TN0004], [ThingNumber, TN0005, " +
                "TN0005], [ThingNumber, TN0006, TN0006], [ThingNumber, TN0007, TN0007], [ThingNumber, TN0008, " +
                "TN0008], [ThingNumber, TN0009, TN0009], [ThingNumber, TN0010, TN0010]], columnNames=[Type, " +
                "SerialNumber, Name]}}";
        assertNotEquals(expected, result);
    }

    @Test
    public void executeQuery() throws Exception {
        String userTest = "userTest";
        //(jIJ9Iy\\")@7\'.?\'
        String passwordTest = StringEscapeUtils.escapeJavaScript("(jIJ9Iy\")@7'.?'");

        try {
            String passwordTestEnc = passwordTest.replace("\"", "\\\"");
            MongoDataBaseTestUtils.createUser(userTest, passwordTestEnc, mongoDataBase);
            OperationExecutor operationExecutor = Mockito.spy(new OperationExecutor());
            Mockito.when(operationExecutor.getConnection()).thenReturn("mongo " +
                    "\"mongodb://127.0.0.1:27017/riot_main_test" +
                    "?readPreference=primary&connectTimeoutMS=3000&maxPoolSize=50\" --authenticationDatabase " +
                    "riot_main_test " +
                    "--quiet --username " + userTest + " --password $'" + passwordTest + "'");
            Document result = operationExecutor.executeCommand("echo 'db.serverStatus()' | " + operationExecutor
                            .getConnection() + " > /tmp/testPassword.txt",
                    "/tmp/testPassword.txt");
            assertEquals(1, result.get("ok"));
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        } finally {
            MongoDataBaseTestUtils.dropUser(userTest, mongoDataBase);
        }
    }
}