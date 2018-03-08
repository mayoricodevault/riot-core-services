package com.tierconnect.riot.api.database.mongoDrive;

import com.tierconnect.riot.api.database.base.FactoryDataBase;
import com.tierconnect.riot.api.database.base.Operation;
import com.tierconnect.riot.api.database.base.alias.Alias;
import com.tierconnect.riot.api.database.base.conditions.BooleanCondition;
import com.tierconnect.riot.api.database.base.conditions.ConditionBuilder;
import com.tierconnect.riot.api.database.base.key.PrimaryKey;
import com.tierconnect.riot.api.database.base.operator.SingleOperator;
import com.tierconnect.riot.api.database.base.ordination.Order;
import com.tierconnect.riot.api.database.exception.OperationNotImplementedException;
import com.tierconnect.riot.api.database.exception.OperationNotSupportedException;
import com.tierconnect.riot.api.database.exception.ValueNotPermittedException;
import com.tierconnect.riot.api.mongoShell.testUtils.MongoDataBaseTestUtils;
import com.tierconnect.riot.api.mongoShell.testUtils.PropertiesReaderUtil;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;

import static com.tierconnect.riot.api.database.base.alias.Alias.create;
import static com.tierconnect.riot.api.database.base.alias.Alias.exclude;
import static junitparams.JUnitParamsRunner.$;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.junit.Assert.*;

/**
 * Created by vealaro on 12/8/16.
 */
@RunWith(JUnitParamsRunner.class)
public class MongoDriverTest {

    private static final String mongoDataBase = "riot_main_comparator";
    private static final String mongoDataBasePath = "databaseTest/riotMainTestComparator";
    private static Logger logger = Logger.getLogger(MongoDriverTest.class);
    private ConditionBuilder builderAND = new ConditionBuilder(BooleanCondition.AND);
    private ConditionBuilder builderOR = new ConditionBuilder(BooleanCondition.OR);
    private MongoDriver dataBase;

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

    @AfterClass
    public static void tearDown() throws Exception {
        MongoDataBaseTestUtils.dropDataBase(mongoDataBase);
    }

    @Before
    public void clearBuilder() {
        builderOR.clear();
        builderAND.clear();
        dataBase = FactoryDataBase.get(MongoDriver.class, builderAND);

    }

    @Test
    public void emptyFilter() throws OperationNotSupportedException {
        assertEquals(EMPTY, dataBase.getConditionBuilderString());
    }


    @Test
    public void notEmptyFilter() throws ValueNotPermittedException, OperationNotSupportedException {
        builderAND.addOperator(Operation.in("_id", Arrays.asList(1L, 2L)));
        dataBase.setBuilder(builderAND);
        assertFalse(EMPTY.equals(dataBase.getConditionBuilderString()));
    }

    @Test
    @Parameters(method = "parametersForSingleOperator")
    public void testTransformSingleOperator(Bson expected, SingleOperator singleOperator) throws OperationNotSupportedException {
        assertNotNull(singleOperator);
        assertNotNull(expected);
        Bson actual = dataBase.transformSingleOperator(singleOperator);
        String bsonToString = MongoDriver.bsonToString(actual);
        logger.debug("singleOperator " + singleOperator);
        logger.debug("mongo filter   " + bsonToString);
        assertEquals(MongoDriver.bsonToString(expected), bsonToString);

    }

    @Test
    @Parameters(method = "parametersForSingleOperatorError")
    public void testTransformSingleOperatorError(SingleOperator singleOperator) throws OperationNotSupportedException {
        assertNotNull(singleOperator);
        try {
            dataBase.transformSingleOperator(singleOperator);
            fail("fail test for Single Operator ELEMENT MATCH with Multiple Operator");
        } catch (OperationNotImplementedException e) {
            assertEquals(e.getMessage(), "operation ELEMENT MATCH is not implemented as Multiple Operator");
        }
    }

    @Test
    public void testPrimaryKey() throws ValueNotPermittedException, OperationNotSupportedException {
        builderOR.addOperator(Operation.equals("_id", PrimaryKey.create("58480a5bb4e3c816fc3d7f24", "org.bson.types.ObjectId")));
        builderOR.addOperator(Operation.equals("_id", PrimaryKey.create("58480a5cb4e3c816fc3d7f25", ObjectId.class)));
        builderOR.addOperator(Operation.equals("_id", PrimaryKey.create("58480a69b4e3c81727610832", "org.bson.types.ObjectId")));
        builderOR.addOperator(Operation.equals("_id", "ObjectId(\"58480a69b4e3c81727610832\")"));
        dataBase.setBuilder(builderOR);
        dataBase.executeFind("thingSnapshots", Arrays.asList(create("_id"), create("value._id"), create("value.name")), 0, 10);
        logger.info(dataBase.getResultSet());
        logger.info(dataBase.getResultSet().size());
        assertEquals(3, dataBase.getResultSet().size());
        builderOR.clear();
        builderOR.addOperator(Operation.equals("_id", PrimaryKey.create(1L, Long.class)));
        builderOR.addOperator(Operation.equals("_id", PrimaryKey.create(2, Integer.class)));
        dataBase.setBuilder(builderOR);
        dataBase.executeFind("things", Arrays.asList(create("_id"), create("value._id"), create("value.name")), 0, 10);
        logger.info(dataBase.getResultSet());
        logger.info(dataBase.getResultSet().size());
        assertEquals(2, dataBase.getResultSet().size());
    }

    @Test
    public void executeWithoutProjection() throws OperationNotSupportedException {
        builderAND.addOperator(Operation.equals("_id", 1));
        dataBase.setBuilder(builderAND);
        dataBase.executeFind("things", Collections.<Alias>emptyList(), 0, 1);
        logger.info(dataBase.getResultSet());
        logger.info(dataBase.getResultSet().size());
        assertEquals(1, dataBase.getResultSet().size());
        dataBase.executeFind("things", null, 0, 1);
        logger.info(dataBase.getResultSet());
        logger.info(dataBase.getResultSet().size());
        assertEquals(1, dataBase.getResultSet().size());
    }

    @Test
    public void executeWithoutProjectionAndOrder() throws OperationNotSupportedException {
        builderOR.addOperator(Operation.equals("_id", 1));
        builderOR.addOperator(Operation.equals("_id", 2));
        dataBase.setBuilder(builderOR);
        dataBase.executeFind("things", Collections.<Alias>emptyList(), 0, 2, Collections.<String, Order>singletonMap("_id", Order.DESC));
        logger.info(dataBase.getResultSet());
        logger.info(dataBase.getResultSet().size());
        assertEquals(2, dataBase.getResultSet().size());
        dataBase.executeFind("things", null, 0, 2, Collections.<String, Order>singletonMap("_id", Order.DESC));
        logger.info(dataBase.getResultSet());
        logger.info(dataBase.getResultSet().size());
        assertEquals(2, dataBase.getResultSet().size());
    }

    @Test
    public void executeWithOnlyFilters() throws OperationNotSupportedException {
        builderOR.addOperator(Operation.equals("_id", 1));
        builderOR.addOperator(Operation.equals("_id", 2));
        dataBase.setBuilder(builderOR);
        dataBase.executeFind("things", Collections.<Alias>emptyList(), 0, 2, Collections.<String, Order>emptyMap());
        logger.info(dataBase.getResultSet());
        logger.info(dataBase.getResultSet().size());
        assertEquals(2, dataBase.getResultSet().size());
        dataBase.executeFind("things", null, 0, 2, null);
        logger.info(dataBase.getResultSet());
        logger.info(dataBase.getResultSet().size());
        assertEquals(2, dataBase.getResultSet().size());
    }

    @Test
    public void executeWithOnlyProjections() throws OperationNotSupportedException {
        dataBase.setBuilder(builderOR);
        dataBase.executeFind("things", Collections.<Alias>singletonList(create("name")), 0, 2, Collections.<String, Order>emptyMap());
        logger.info(dataBase.getResultSet());
        logger.info(dataBase.getResultSet().size());
        assertEquals(2, dataBase.getResultSet().size());
        dataBase.executeFind("things", Collections.<Alias>singletonList(create("name")), 0, 2, null);
        logger.info(dataBase.getResultSet());
        logger.info(dataBase.getResultSet().size());
        assertEquals(2, dataBase.getResultSet().size());
    }

    @Test
    public void executeAllEmptyOrnull() throws OperationNotSupportedException {
        dataBase.setBuilder(builderAND);
        dataBase.executeFind("things", Collections.<Alias>emptyList(), 0, 2, Collections.<String, Order>emptyMap());
        logger.info(dataBase.getResultSet());
        logger.info(dataBase.getResultSet().size());
        assertEquals(2, dataBase.getResultSet().size());
        dataBase.executeFind("things", null, 0, 2, null);
        logger.info(dataBase.getResultSet());
        logger.info(dataBase.getResultSet().size());
        assertEquals(2, dataBase.getResultSet().size());
    }

    @Test
    public void testAlias() {
        dataBase.setAliasList(Collections.singletonList(create("name")));
        assertEquals(new Document("name", 1).toJson(), dataBase.getProjectionString());
        dataBase.setAliasList(Arrays.asList(create("name"), create("serialNumber")));
        assertEquals(new Document("name", 1).append("serialNumber", 1).toJson(), dataBase.getProjectionString());
        dataBase.setAliasList(Arrays.asList(exclude("_id"), exclude("name")));
        assertEquals(new Document("_id", 0).append("name", 0).toJson(), dataBase.getProjectionString());
        dataBase.setAliasList(Arrays.asList(exclude("_id"), create("name")));
        assertEquals(new Document("_id", 0).append("name", 1).toJson(), dataBase.getProjectionString());
    }

    @Test
    public void testAliasNullOrEmpty() {
        dataBase.setAliasList(null);
        assertEquals("null", dataBase.getProjectionString());
        dataBase.setAliasList(Collections.<Alias>emptyList());
        assertEquals("null", dataBase.getProjectionString());
    }

    @Test
    public void testSortNullOrEmpty() {
        dataBase.setMapOrder(null);
        assertEquals("null", dataBase.getSortString());
        dataBase.setMapOrder(Collections.<String, Order>emptyMap());
        assertEquals("null", dataBase.getSortString());
    }


    @Test
    public void testSortNotNull() {
        dataBase.setMapOrder(Collections.singletonMap("_id", Order.ASC));
        assertEquals(new Document("_id", 1).toJson(), dataBase.getSortString());
        dataBase.setMapOrder(Collections.singletonMap("_id", Order.DESC));
        assertEquals(new Document("_id", -1).toJson(), dataBase.getSortString());
    }


    @Test(expected = OperationNotSupportedException.class)
    public void testOperatorNull() throws NoSuchFieldException, IllegalAccessException, OperationNotSupportedException {
        SingleOperator singleOperator = new SingleOperator("name", Operation.OperationEnum.EQUALS, "SSSASSSS");
        Field fieldOperator = singleOperator.getClass().getDeclaredField("operator");
        fieldOperator.setAccessible(true);
        fieldOperator.set(singleOperator, null);
        dataBase.transformSingleOperator(singleOperator);
    }

    private Document createDocument(String key, Object value) {
        return new Document(key, value);
    }

    private Object[] parametersForSingleOperator() throws ValueNotPermittedException {
        return $(
                $(createDocument("status.value", "STOLEN"), Operation.equals("status.value", "STOLEN")),
                $(createDocument("status.value", createDocument("$ne", "STOLEN")), Operation.notEquals("status.value", "STOLEN")),
                $(createDocument("age.value", createDocument("$gt", 56)), Operation.greaterThan("age.value", 56)),
                $(createDocument("age.value", createDocument("$lt", 56)), Operation.lessThan("age.value", 56)),
                $(createDocument("age.value", createDocument("$gte", 56)), Operation.greaterThanOrEquals("age.value", 56)),
                $(createDocument("age.value", createDocument("$lte", 56)), Operation.lessThanOrEquals("age.value", 56)),
                $(createDocument("groupTypeName", createDocument("$regex", ".*\\QFac\\E.*").append("$options", "i")), Operation.contains("groupTypeName", "Fac")),
                $(createDocument("groupTypeName", createDocument("$regex", "^\\QFac\\E").append("$options", "i")), Operation.startsWith("groupTypeName", "Fac")),
                $(createDocument("groupTypeName", createDocument("$regex", "\\QFac\\E$").append("$options", "i")), Operation.endsWith("groupTypeName", "Fac")),
                $(createDocument("groupTypeName", createDocument("$in", Arrays.asList("Facility", "item"))), Operation.in("groupTypeName", Arrays.asList("Facility", "item"))),
                $(createDocument("groupTypeName", createDocument("$in", Arrays.asList(1, 2, 3, 4))), Operation.in("groupTypeName", Arrays.asList(1, 2, 3, 4))),
                $(createDocument("groupTypeName", createDocument("$nin", Arrays.asList("Facility", "item"))), Operation.notIn("groupTypeName", Arrays.asList("Facility", "item"))),
                $(createDocument("groupTypeName", createDocument("$nin", Arrays.asList(1, 2, 3, 4))), Operation.notIn("groupTypeName", Arrays.asList(1, 2, 3, 4))),
                $(createDocument("price", createDocument("$gte", 10).append("$lte", 20)), Operation.between("price", 10, 20)),
                $(createDocument("price", createDocument("$exists", true)), Operation.exists("price")),
                $(createDocument("price", createDocument("$exists", false)), Operation.notExists("price")),
                $(createDocument("name", EMPTY), Operation.empty("name")),
                $(createDocument("name", createDocument("$ne", EMPTY)), Operation.notEmpty("name")),
                $(createDocument("$and", Arrays.asList(createDocument("name", createDocument("$exists", true)), createDocument("name", null))), Operation.isNull("name")),
                $(createDocument("name", createDocument("$ne", null)), Operation.isNotNull("name")),
                $(createDocument("$or", Arrays.asList(
                        createDocument("children", createDocument("$exists", false)),
                        createDocument("children", null),
                        createDocument("children", createDocument("$size", 0)))), Operation.emptyArray("children")),
                $(createDocument("children", createDocument("$exists", true).append("$not", createDocument("$size", 0))), Operation.notEmptyArray("children")),
                $(createDocument("children", createDocument("$size", 0)), Operation.arraySizeMatch("children", 0)),
                $(createDocument("children", createDocument("$size", 5)), Operation.arraySizeMatch("children", 5)),
                $(createDocument("name", createDocument("$regex", "4").append("$options", "i")), Operation.regex("name", "4", "i")),
                $(createDocument("status.value", createDocument("$regex", "abc").append("$options", "x")), Operation.regex("status.value", "abc", "x")),
                $(createDocument("children", createDocument("$elemMatch", createDocument("thingTypeId", 1))), Operation.elementMatch("children", Operation.equals("thingTypeId", 1))),
                $(createDocument("results", createDocument("$elemMatch", createDocument("score", 8).append("product", "abc"))), Operation.elementMatch("results", Operation.equals("score", 8), Operation.equals("product", "abc"))),
                $(createDocument("children", createDocument("$elemMatch", createDocument("thingTypeId", 1).append("groupTypeId", 3).append("groupCode", createDocument("$in", Arrays.asList("SM", "SS"))))),
                        Operation.elementMatch("children", Operation.equals("thingTypeId", 1), Operation.equals("groupTypeId", 3), Operation.in("groupCode", Arrays.asList("SM", "SS"))))
        );
    }

    private Object[] parametersForSingleOperatorError() throws ValueNotPermittedException {
        return $(
                $(Operation.elementMatch("results", Operation.AND(Operation.equals("score", 8), Operation.equals("product", "abc")))),
                $(Operation.elementMatch("children", Operation.equals("thingTypeId", 1),
                        Operation.OR(Operation.equals("groupTypeId", 3), Operation.in("groupCode", Arrays.asList("SM", "SS")))))
        );
    }

}
