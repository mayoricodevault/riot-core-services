package com.tierconnect.riot.api.database.mongo;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.tierconnect.riot.api.database.base.FactoryDataBase;
import com.tierconnect.riot.api.database.base.GenericOperator;
import com.tierconnect.riot.api.database.base.Operation;
import com.tierconnect.riot.api.database.base.conditions.BooleanCondition;
import com.tierconnect.riot.api.database.base.conditions.ConditionBuilder;
import com.tierconnect.riot.api.database.base.ordination.Order;
import com.tierconnect.riot.api.database.codecs.MapResult;
import com.tierconnect.riot.api.database.codecs.MapResultCodec;
import com.tierconnect.riot.api.database.codecs.MapResultCodecProvider;
import com.tierconnect.riot.api.database.exception.OperationNotSupportedException;
import com.tierconnect.riot.api.database.exception.ValueNotPermittedException;
import com.tierconnect.riot.api.database.mongo.aggregate.MongoGroupBy;
import com.tierconnect.riot.api.database.mongo.aggregate.Pipeline;
import com.tierconnect.riot.api.database.mongoDrive.MongoDriver;
import com.tierconnect.riot.api.database.utils.ListAliasForTest;
import com.tierconnect.riot.api.database.utils.ZoneCodeProvider;
import com.tierconnect.riot.api.database.utils.ZoneTransformer;
import com.tierconnect.riot.api.mongoShell.query.ResultFormat;
import com.tierconnect.riot.api.mongoShell.testUtils.MongoDataBaseTestUtils;
import com.tierconnect.riot.api.mongoShell.testUtils.PropertiesReaderUtil;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.log4j.Logger;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static com.tierconnect.riot.api.database.base.Operation.*;
import static com.tierconnect.riot.api.database.base.alias.Alias.create;
import static com.tierconnect.riot.api.database.base.alias.Alias.exclude;
import static java.util.Arrays.asList;
import static junitparams.JUnitParamsRunner.$;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.junit.Assert.*;

/**
 * Created by vealaro on 12/5/16.
 * Tests cases to test mongo shell vs mongo driver
 */
@RunWith(JUnitParamsRunner.class)
public class MongoShellAndDriverTest {

    private static Logger logger = Logger.getLogger(MongoShellAndDriverTest.class);
    private ConditionBuilder builderShell = new ConditionBuilder();
    private ConditionBuilder builderDriver = new ConditionBuilder();
    private static final String mongoDataBase = "riot_mai";
    private static final String mongoDataBasePath = "databaseTest/riotMainTestComparator";
    private Mongo dataBaseShell;
    private MongoDriver dataBaseDriver;
    private String thingsCollection = "things";
    private String thingSnapshotsCollection = "thingSnapshots";

    @BeforeClass
    public static void setUp() throws Exception {
        try {
            PropertiesReaderUtil.setConfigurationFile("propertiesMongoComparatorLocalHost.properties");
        } catch (IOException e) {
            logger.error("Error in properties");
        }
        MongoDataBaseTestUtils.dropDataBase(mongoDataBase);
        MongoDataBaseTestUtils.createDataBase(mongoDataBase, mongoDataBasePath);
    }

    //
    @AfterClass
    public static void tearDown() throws Exception {
        MongoDataBaseTestUtils.dropDataBase(mongoDataBase);
    }

    @Before
    public void clean() {
        dataBaseShell = FactoryDataBase.get(Mongo.class, builderShell);
        dataBaseDriver = FactoryDataBase.get(MongoDriver.class, builderDriver);
        dataBaseShell.getBuilder().clear();
        dataBaseDriver.getBuilder().clear();
    }

    @Test
    @Parameters(method = "parameterForSingleOperators")
    public void testSingleOperator(int result, String collection, GenericOperator genericOperator, int skip, int
            limit, ListAliasForTest listAlias) throws Exception {

        dataBaseShell.setConnection("admin", "control123!", "admin", mongoDataBase, "127.0.0.1", 27017);
        dataBaseShell.getBuilder().addOperator(genericOperator);
        dataBaseShell.executeFind(collection, listAlias.getAliasList(), skip, limit, "TEST_" + System
                .currentTimeMillis());

        dataBaseDriver.getBuilder().addOperator(genericOperator);
        dataBaseDriver.executeFind(collection, listAlias.getAliasList(), skip, limit);

        List<Map<String, Object>> resultSetShell = dataBaseShell.getResultSet();
        List<Map<String, Object>> resultSetDriver = dataBaseDriver.getResultSet();
        logger.info("result  Shell = " + resultSetShell.size() + " of " + dataBaseShell.getCountAll());
        logger.info("result Driver = " + resultSetDriver.size() + " of " + dataBaseDriver.getCountAll());

        assertNotNull(resultSetShell);
        assertNotNull(resultSetDriver);
        assertEquals(result, resultSetShell.size());
        assertEquals(result, resultSetDriver.size());
        assertEquals(resultSetDriver.toString(), resultSetShell.toString());
    }

    @Test
    public void executeMultipleOperatorsOR() throws Exception {
        ConditionBuilder builder = new ConditionBuilder();
        builder.addMultiple(BooleanCondition.OR,
                Operation.equals("thingTypeCode", "default_rfid_thingtype"),
                Operation.equals("thingTypeCode", "default_gps_thingtype"));
        dataBaseShell.setBuilder(builder);
        dataBaseShell.setConnection("admin", "control123!", "admin", mongoDataBase, "127.0.0.1", 27017);
        dataBaseShell.executeFind(thingsCollection, Arrays.asList(create("thingTypeCode"), create("name"),
                create("serialNumber")), 0, 10, Collections.singletonMap("_id", Order.DESC), "TEST_" +
                System.currentTimeMillis());
        dataBaseDriver.setBuilder(builder);
        dataBaseDriver.executeFind(thingsCollection, Arrays.asList(create("thingTypeCode"), create("name"),
                create("serialNumber")), 0, 10, Collections.singletonMap("_id", Order.DESC));

        List<Map<String, Object>> resultSetShell = dataBaseShell.getResultSet();
        List<Map<String, Object>> resultSetDriver = dataBaseDriver.getResultSet();

        logger.info("result  Shell = " + resultSetShell.size() + " of " + dataBaseShell.getCountAll());
        logger.info("result Driver = " + resultSetDriver.size() + " of " + dataBaseDriver.getCountAll());

        assertEquals(10, resultSetShell.size());
        assertEquals(10, resultSetDriver.size());
        assertEquals(resultSetDriver.toString(), resultSetShell.toString());
    }


    @Test
    public void executeMultipleAND() throws Exception {
        ConditionBuilder builder = new ConditionBuilder();
        builder.addOperator(Operation.equals("thingTypeCode", "default_rfid_thingtype"));
        builder.addOperator(Operation.equals("thingTypeId", 1));
        builder.addOperator(Operation.in("thingTypeId", Arrays.asList(1, 2, 3)));

        dataBaseShell.setBuilder(builder);
        dataBaseShell.setConnection("admin", "control123!", "admin", mongoDataBase, "127.0.0.1", 27017);
        dataBaseShell.executeFind(thingsCollection, Arrays.asList(create("thingTypeCode"), create("name"),
                create("serialNumber")), 0, 10, Collections.singletonMap("_id", Order.DESC), "TEST_" +
                System.currentTimeMillis());
        dataBaseDriver.setBuilder(builder);
        dataBaseDriver.executeFind(thingsCollection, Arrays.asList(create("thingTypeCode"), create("name"),
                create("serialNumber")), 0, 10, Collections.singletonMap("_id", Order.DESC));

        List<Map<String, Object>> resultSetShell = dataBaseShell.getResultSet();
        List<Map<String, Object>> resultSetDriver = dataBaseDriver.getResultSet();

        logger.info("result  Shell = " + resultSetShell.size() + " of " + dataBaseShell.getCountAll());
        logger.info("result Driver = " + resultSetDriver.size() + " of " + dataBaseDriver.getCountAll());

        assertEquals(10, resultSetShell.size());
        assertEquals(10, resultSetDriver.size());
        assertEquals(resultSetDriver.toString(), resultSetShell.toString());
    }

    @Test
    public void executeMultipleOperatorsAND() throws Exception {
        ConditionBuilder builder = new ConditionBuilder(BooleanCondition.OR);
        builder.addMultiple(BooleanCondition.AND,
                Operation.equals("thingTypeCode", "default_rfid_thingtype"),
                Operation.contains("name", "000000000000000000"));
        dataBaseShell.setBuilder(builder);
        dataBaseShell.setConnection("admin", "control123!", "admin", mongoDataBase, "127.0.0.1", 27017);
        dataBaseShell.executeFind(thingsCollection, Arrays.asList(create("thingTypeCode"), create("name"),
                create("serialNumber")), 0, 10, Collections.singletonMap("_id", Order.DESC)
                , "TEST_" + System.currentTimeMillis());
        dataBaseDriver.setBuilder(builder);
        dataBaseDriver.executeFind(thingsCollection, Arrays.asList(create("thingTypeCode"), create("name"),
                create("serialNumber")), 0, 10, Collections.singletonMap("_id", Order.DESC));

        List<Map<String, Object>> resultSetShell = dataBaseShell.getResultSet();
        List<Map<String, Object>> resultSetDriver = dataBaseDriver.getResultSet();

        logger.info("result  Shell = " + resultSetShell.size() + " of " + dataBaseShell.getCountAll());
        logger.info("result Driver = " + resultSetDriver.size() + " of " + dataBaseDriver.getCountAll());

        assertEquals(10, resultSetShell.size());
        assertEquals(10, resultSetDriver.size());
        assertEquals(resultSetDriver.toString(), resultSetShell.toString());
    }

    @Test
    @Parameters(method = "parameterForSingleOperatorExport")
    public void exportReport(String collection, GenericOperator genericOperator, ListAliasForTest listAlias) throws
            InterruptedException, OperationNotSupportedException, IOException {
        dataBaseShell.getBuilder().addOperator(genericOperator);
        String exportFile = dataBaseShell.export(collection, listAlias.getAliasList(), "tmp", ResultFormat.CSV,
                "TEST_EXPORT_" + System.currentTimeMillis());
        logger.info("File " + exportFile);
        File file = new File(exportFile);
        assertTrue(file.exists());
        FileReader fileReader = new FileReader(file);
        BufferedReader reader = new BufferedReader(fileReader);
        String line1;
        logger.debug("--------------------------- File content ----------------------------------------");
        while ((line1 = reader.readLine()) != null) {
            logger.debug(line1);
        }
        logger.debug("--------------------------- end content ----------------------------------------");
    }


    @SuppressWarnings("unused")
    private Object[] parameterForSingleOperatorExport() throws ValueNotPermittedException {
        return $(
                $(thingsCollection, Operation.equals("_id", 1), new ListAliasForTest(exclude("_id"),
                        create("name", "Name"), create("serialNumber", "Serial"), create("groupName", "Group Name"),
                        create("thingTypeName", "Thing Type Name"), create("thingTypeCode", "Thing Type Code"),
                        create("time", "Time", "formatDate"))),
                $(thingSnapshotsCollection, Operation.equals("_id", "ObjectId(\"58480a69b4e3c81727610832\")"),
                        new ListAliasForTest(exclude("_id"),
                                create("value.name", "Name"), create("value.serialNumber", "Serial"),
                                create("value.groupName", "Group Name"), create("value.thingTypeName", "Thing Type " +
                                "Name"),
                                create("value.thingTypeCode", "Thing Type Code"),
                                create("value.time", "Time", "formatDate"),
                                create("value.eNode.value", "ENode"),
                                create("value.eNode.dwellTime", "Dwelltime ENode", "formatDwellTime"))),
                $(thingSnapshotsCollection, Operation.equals("_id", "ObjectId(\"58480a69b4e3c81727610835\")"),
                        new ListAliasForTest(exclude("_id"), create("value.name", "NAME"), create("value" +
                                ".serialNumber", "Serial"),
                                create("value.size.value", "Size"), create("value.children.name", "Children Name"),
                                create("value.children.eNode.value", "Enode Children"))),
                $(thingsCollection, Operation.equals("_id", 11),
                        new ListAliasForTest(exclude("_id"), create("name", "NAME"), create("serialNumber", "Serial"),
                                create("parent.serialNumber", "Serial Parent"), create("parent.name", "Name Parent"),
                                create("parent.color.value", "Color parent"))),
                $(thingsCollection, isNotNull("testNull"),
                        new ListAliasForTest(exclude("_id"), create("testNull"), create("serialNumber", "Serial"),
                                create("item")))
        );
    }

    @SuppressWarnings("unused")
    private Object[] parameterForSingleOperators() throws ValueNotPermittedException {
        String thingSnapshotIdsCollection = "thingSnapshotIds";
        return $(
                /*0*/
                $(1, thingsCollection, Operation.equals("_id", 2), 0, 1, new ListAliasForTest(create("groupTypeId"),
                        create("name"), create("serialNumber"))),
                /*1*/
                $(1, thingsCollection, Operation.equals("_id", 2L), 0, 1, new ListAliasForTest(create("groupTypeId"),
                        create("name"), create("serialNumber"))),
                /*2*/
                $(1, thingsCollection, Operation.equals("serialNumber", "RFID1234567890"), 0, 1,
                        new ListAliasForTest(create("groupTypeId"), create("name"), create("serialNumber"))),
                /*3*/
                $(2, thingsCollection, Operation.equals("time", new Date(1481116249768L)), 0, 2,
                        new ListAliasForTest(create("groupTypeId"), create("name"), create("serialNumber"))),
                /*4*/
                $(10, thingsCollection, notEquals("_id", 2), 0, 10, new ListAliasForTest(create("groupTypeId"),
                        create("name"), create("serialNumber"))),
                /*5*/
                $(10, thingsCollection, notEquals("time", new Date(1481116249768L)), 0, 10,
                        new ListAliasForTest(create("groupTypeId"), create("name"), create("serialNumber"))),
                /*6*/
                $(10, thingsCollection, notEquals("time", new Date(1481116249768L)), 10, 10,
                        new ListAliasForTest(create("groupTypeId"), create("name"), create("serialNumber"))),
                /*7*/
                $(10, thingsCollection, greaterThan("_id", 6), 0, 10, new ListAliasForTest(create("groupTypeId"),
                        create("name"), create("serialNumber"))),
                /*8*/
                $(10, thingsCollection, greaterThan("_id", 6L), 0, 10, new ListAliasForTest(create("groupTypeId"),
                        create("name"), create("serialNumber"))),
                /*9*/
                $(5, thingsCollection, lessThan("_id", 6), 0, 10, new ListAliasForTest(create("groupTypeId"),
                        create("name"), create("serialNumber"))),
                /*10*/
                $(5, thingsCollection, lessThan("_id", 6L), 0, 10, new ListAliasForTest(create("groupTypeId"),
                        create("name"), create("serialNumber"))),
                /*11*/
                $(2, thingsCollection, lessThan("time", new Date(1481116264921L)), 0, 10,
                        new ListAliasForTest(create("groupTypeId"), create("name"), create("serialNumber"))),
                /*12*/
                $(10, thingsCollection, greaterThanOrEquals("_id", 6), 0, 10, new ListAliasForTest(create
                        ("groupTypeId"),
                        create("name"), create("serialNumber"))),
                /*13*/
                $(10, thingsCollection, greaterThanOrEquals("_id", 6L), 10, 10, new ListAliasForTest(
                        create("groupTypeId"), create("name"), create("serialNumber"))),
                /*14*/
                $(10, thingsCollection, greaterThanOrEquals("time", new Date(1481116264921L)), 0, 10,
                        new ListAliasForTest(create("groupTypeId"), create("name"), create("serialNumber"))),
                /*15*/
                $(10, thingsCollection, greaterThanOrEquals("time", new Date(1481116264921L)), 10, 10,
                        new ListAliasForTest(create("groupTypeId"), create("name"), create("serialNumber"))),
                /*16*/
                $(1, thingsCollection, contains("thingTypeName", "GPS"), 0, 10, new ListAliasForTest(
                        create("groupTypeId"), create("name"), create("serialNumber"))),
                /*17*/
                $(5, thingsCollection, startsWith("serialNumber", "J00"), 0, 10, new ListAliasForTest(
                        create("groupTypeId"), create("name"), create("serialNumber"))),
                /*18*/
                $(5, thingsCollection, endsWith("thingTypeCode", "_code"), 0, 5, new ListAliasForTest(
                        create("groupTypeId"), create("name"), create("serialNumber"))),
                /*19*/
                $(3, thingsCollection, in("_id", Arrays.asList(1, 2, 4)), 0, 5, new ListAliasForTest(
                        create("groupTypeId"), create("name"), create("serialNumber"))),
                /*20*/
                $(5, thingsCollection, in("groupTypeName", Arrays.asList("Facility", "Store")), 0, 5,
                        new ListAliasForTest(create("groupTypeId"), create("name"), create("serialNumber"))),
                /*21*/
                $(5, thingsCollection, notIn("_id", Arrays.asList(1, 2, 4)), 0, 5, new ListAliasForTest(
                        create("groupTypeId"), create("name"), create("serialNumber"))),
                /*22*/
                $(5, thingsCollection, notIn("groupTypeName", Collections.singletonList("Store")), 0, 5,
                        new ListAliasForTest(create("groupTypeId"), create("name"), create("serialNumber"))),
                /*23*/
                $(1, thingsCollection, in("_id", Collections.singletonList(100)), 0, 5, new ListAliasForTest(
                        create("groupTypeId"), create("name"), create("serialNumber"))),
                /*24*/
                $(5, thingsCollection, between("_id", 2, 6), 0, 5, new ListAliasForTest(create("groupTypeId"),
                        create("name"), create("serialNumber"))),
                /*25*/
                $(3, thingsCollection, between("time", new Date(1481118028019L), new Date(1481121817378L)), 0, 5,
                        new ListAliasForTest(create("groupTypeId"), create("name"), create("serialNumber"))),
                /*26*/
                $(5, thingsCollection, exists("parent"), 0, 5, new ListAliasForTest(create("groupTypeId"),
                        create("name"), create("serialNumber"), create("parent"))),
                /*27*/
                $(5, thingsCollection, notExists("parent"), 0, 5, new ListAliasForTest(create("groupTypeId"),
                        create("name"), create("serialNumber"))),
                /*28*/
                $(2, thingsCollection, empty("groupTypeCode"), 0, 5, new ListAliasForTest(create("groupTypeCode"),
                        create("name"), create("serialNumber"))),
                /*29*/
                $(5, thingsCollection, notEmpty("groupTypeCode"), 0, 5, new ListAliasForTest(create("groupTypeCode"),
                        create("name"), create("serialNumber"))),
                /*30*/
                $(4, thingsCollection, isNull("testNull"), 0, 5, new ListAliasForTest(create("_id"), create("testNull"),
                        create("serialNumber"), create("item"))),
                /*31*/
                $(2, thingsCollection, isNotNull("testNull"), 0, 5, new ListAliasForTest(create("_id"),
                        create("testNull"), create("serialNumber"), create("item"))), // TODO: decimal
                /*32*/
                $(5, thingsCollection, emptyArray("item"), 0, 5, new ListAliasForTest(create("_id"),
                        create("testNull"), create("serialNumber"), create("item"))),
                /*33*/
                $(3, thingsCollection, notEmptyArray("item"), 0, 5, new ListAliasForTest(create("_id"),
                        create("testNull"), create("serialNumber"), create("item"))),
                /*34*/
                $(3, thingsCollection, arraySizeMatch("item", 3), 0, 5, new ListAliasForTest(create("_id"),
                        create("testNull"), create("serialNumber"), create("item"))),
                /*35*/
                $(5, thingsCollection, regex("name", "48", "i"), 0, 5, new ListAliasForTest(create("_id"),
                        create("testNull"), create("serialNumber"), create("item"))),
                /*36*/
                $(5, thingsCollection, regex("name", "48", ""), 0, 5, new ListAliasForTest(create("_id"),
                        create("testNull"), create("serialNumber"), create("item"))),
                /*37*/
                $(2, thingsCollection, elementMatch("item", Operation.equals("key", "close")), 0, 5,
                        new ListAliasForTest(create("_id"), create("testNull"), create("serialNumber"), create
                                ("item"))),
                /*38*/
                $(1, thingsCollection, elementMatch("item", Operation.equals("key", "close"),
                        Operation.equals("value", "X")), 0, 5, new ListAliasForTest(create("_id"), create("testNull"),
                        create("serialNumber"), create("item"))),
                /*39*/
                $(1, thingsCollection, elementMatch("item", Arrays.asList(Operation.equals("key", "close"),
                        Operation.equals("value", "X"))), 0, 5, new ListAliasForTest(create("_id"), create("testNull"),
                        create("serialNumber"), create("item"))),
                /*40*/
                $(0, thingsCollection, in("item", Collections.emptyList()), 0, 5, new ListAliasForTest(create("_id"))),
                /*41*/
                $(47, thingSnapshotIdsCollection, elementMatch("blinks", between("time", 1481116249768L,
                        1481121817378L)), 0, 100, new ListAliasForTest(create("blinks.time"),
                        create("blinks.blink_id"))),
                /*42*/
                $(1, thingSnapshotsCollection, Operation.equals("_id", "ObjectId(\"58480a5bb4e3c816fc3d7f24\")"), 0,
                        1, new ListAliasForTest(create("value.groupName"), create("time"))),

                $(1, thingSnapshotsCollection, in("_id", Collections.singletonList("ObjectId" + "" +
                        "(\"58480a5bb4e3c816fc3d7f24\")")), 0, 1, new ListAliasForTest(create("value"), create("time")))
        );
    }

    @Test
    public void testTransform() {
        CodecRegistry codecRegistry = CodecRegistries.fromRegistries(MongoClient.getDefaultCodecRegistry(),
                fromProviders(asList(new ZoneCodeProvider(), new MapResultCodecProvider(new ZoneTransformer()))),
                CodecRegistries.fromCodecs(new MapResultCodec()));
        MongoClientOptions options = MongoClientOptions.builder().codecRegistry(codecRegistry).build();
        MongoClient mongoClient = new MongoClient(new ServerAddress(), options);
        MongoDatabase database = mongoClient.getDatabase("riot_main_comparator");
        MongoCollection<MapResult> thingsMongoCollection = database.getCollection(thingsCollection, MapResult.class);
        for (MapResult mapResult : thingsMongoCollection.find(Filters.eq("_id", 1))) {
            logger.info("result=" + mapResult.toMap());
        }
    }

    @Test
    @Parameters(method = "parameterForAggregate")
    public void testAggregate(String collection, ConditionBuilder builder, List<Pipeline> groupByList,
                              int limit, Long resultExpect) throws Exception {
        dataBaseShell.setConnection("admin", "control123!", "admin", mongoDataBase, "127.0.0.1", 27017);
        dataBaseDriver.setBuilder(builder);
        dataBaseShell.setBuilder(builder);
        dataBaseDriver.executeAggregate(collection, groupByList, limit);
        dataBaseShell.executeAggregate(collection, groupByList, limit);
        logger.info("result DRIVER: " + dataBaseDriver.getResultSet());
        logger.info("result  SHELL: " + dataBaseShell.getResultSet());
        logger.info("total  DRIVER: " + dataBaseDriver.getCountAll());
        logger.info("total   SHELL: " + dataBaseShell.getCountAll());
        assertEquals(resultExpect, dataBaseDriver.getCountAll());
        assertEquals(resultExpect, dataBaseShell.getCountAll());
    }

    @Test
    public void testSubQuery() throws Exception {
        ConditionBuilder queryMongoDriver = new ConditionBuilder();
        ConditionBuilder subQueryqueryMongoDriver = new ConditionBuilder();
        subQueryqueryMongoDriver.addOperator(Operation.elementMatch("blinks",
                Operation.between("time", 1469973724000L, 1486648926999L)));
        queryMongoDriver.addOperator(Operation.inSubquery("_id", subQueryqueryMongoDriver, "thingSnapshotIds", "blinks.blink_id"));
        dataBaseDriver.setBuilder(queryMongoDriver);

        ConditionBuilder queryShell = new ConditionBuilder();
        ConditionBuilder subQueryShell = new ConditionBuilder();
        subQueryShell.addOperator(Operation.elementMatch("blinks",
                Operation.between("time", 1469973724000L, 1486648926999L)));
        queryShell.addOperator(Operation.inSubquery("_id", subQueryShell, "thingSnapshotIds", "blinks[0].blink_id"));
        dataBaseShell.setConnection("admin", "control123!", "admin", mongoDataBase, "127.0.0.1", 27017);
        dataBaseShell.setBuilder(queryShell);

        dataBaseDriver.executeFind("thingSnapshots", null, 0, 100000);
        dataBaseShell.executeFind("thingSnapshots", null, 0, 100000);

        logger.info("result DRIVER: " + dataBaseDriver.getResultSet());
        logger.info("result  SHELL: " + dataBaseShell.getResultSet());
        assertEquals(dataBaseDriver.getResultSet().size(), dataBaseShell.getResultSet().size());
    }

    @Test
    public void testSubQueryWithProjection() throws Exception {
        ConditionBuilder queryMongoDriver = new ConditionBuilder();
        ConditionBuilder conditionBuilder = new ConditionBuilder();
        conditionBuilder.addOperator(Operation.elementMatch("blinks",
                Operation.between("time", 1469973724000L, 1486648926999L)));
        queryMongoDriver.addOperator(Operation.inSubquery("_id", conditionBuilder, "thingSnapshotIds", "blinks.blink_id", conditionBuilder));
        dataBaseDriver.setBuilder(queryMongoDriver);

        ConditionBuilder queryShell = new ConditionBuilder();
        ConditionBuilder subQueryShell = new ConditionBuilder();
        subQueryShell.addOperator(Operation.elementMatch("blinks",
                Operation.between("time", 1469973724000L, 1486648926999L)));
        queryShell.addOperator(Operation.inSubquery("_id", subQueryShell, "thingSnapshotIds", "blinks[0].blink_id", subQueryShell));
        dataBaseShell.setConnection("admin", "control123!", "admin", mongoDataBase, "127.0.0.1", 27017);
        dataBaseShell.setBuilder(queryShell);

        dataBaseDriver.executeFind("thingSnapshots", null, 0, 100000);
        dataBaseShell.executeFind("thingSnapshots", null, 0, 100000);

        logger.info("result DRIVER: " + dataBaseDriver.getResultSet());
        logger.info("result  SHELL: " + dataBaseShell.getResultSet());
        assertEquals(dataBaseDriver.getResultSet().size(), dataBaseShell.getResultSet().size());
    }

    @SuppressWarnings({"unused", "ArraysAsListWithZeroOrOneArgument"})
    private Object[] parameterForAggregate() {
        /*builders*/
        ConditionBuilder builderCase1 = new ConditionBuilder();
        builderCase1.addOperator(Operation.equals("thingTypeId", 1));

        ConditionBuilder builderCase2 = new ConditionBuilder();
        builderCase2.addOperator(Operation.equals("thingTypeId", 1));
        builderCase2.addOperator(exists("eNode"));

        ConditionBuilder builderCase4 = new ConditionBuilder();
        builderCase4.addOperator(Operation.equals("thingTypeCode", "ThingNumber"));
        builderCase4.addOperator(exists("number.value"));

        /*groups*/
        Map<String, Object> mapCase2 = Collections.<String, Object>singletonMap("ENODE_X", "$eNode.value");
        List<MongoGroupBy> case2 = Collections.singletonList(new MongoGroupBy(mapCase2, "total", MongoGroupBy.Accumulator.COUNT, "1"));

        Map<String, Object> mapCase4 = Collections.<String, Object>singletonMap("Code", "$thingTypeCode");
        List<MongoGroupBy> case4 = Arrays.asList(new MongoGroupBy(mapCase4, "total", MongoGroupBy.Accumulator.SUM, "$number.value"));

        List<MongoGroupBy> case5 = Arrays.asList(new MongoGroupBy(mapCase4, "total", MongoGroupBy.Accumulator.AVG, "$number.value"));

        List<MongoGroupBy> case6 = Arrays.asList(new MongoGroupBy(mapCase4, "total", MongoGroupBy.Accumulator.MAX, "$number.value"));

        List<MongoGroupBy> case7 = Arrays.asList(new MongoGroupBy(mapCase4, "total", MongoGroupBy.Accumulator.MIN, "$number.value"));

        String groupString = "$thingTypeCode";
        List<MongoGroupBy> case8 = Arrays.asList(new MongoGroupBy(groupString, "total", MongoGroupBy.Accumulator.SUM, "$number.value"));

        groupString = null;
        List<MongoGroupBy> case9 = Arrays.asList(new MongoGroupBy(groupString, "total", MongoGroupBy.Accumulator.SUM, "$number.value"));

        return $(
                /*0*/
                $(thingsCollection, builderCase1, null, 10, 10L),
                /*1*/
                $(thingsCollection, builderCase1, Collections.<MongoGroupBy>emptyList(), 10, 10L),
                /*2*/
                $(thingsCollection, builderCase1, case2, 10, 2L),
                /*3*/
                $(thingsCollection, builderCase2, case2, 10, 1L),
                /*4*/
                $(thingsCollection, builderCase4, case4, 10, 1L),
                /*5*/
                $(thingsCollection, builderCase4, case5, 10, 1L),
                /*6*/
                $(thingsCollection, builderCase4, case6, 10, 1L),
                /*7*/
                $(thingsCollection, builderCase4, case7, 10, 1L),
                /*8*/
                $(thingsCollection, builderCase4, case8, 10, 1L),
                /*8*/
                $(thingsCollection, builderCase4, case9, 10, 1L)
        );
    }
}
