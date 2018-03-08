package com.tierconnect.riot.api.database.mongo;

import com.tierconnect.riot.api.database.base.FactoryDataBase;
import com.tierconnect.riot.api.database.base.Operation;
import com.tierconnect.riot.api.database.base.conditions.BooleanCondition;
import com.tierconnect.riot.api.database.base.conditions.ConditionBuilder;
import com.tierconnect.riot.api.database.base.operator.MultipleOperator;
import com.tierconnect.riot.api.database.base.operator.SingleOperator;
import com.tierconnect.riot.api.database.base.ordination.Order;
import com.tierconnect.riot.api.database.exception.OperationNotImplementedException;
import com.tierconnect.riot.api.database.exception.OperationNotSupportedException;
import com.tierconnect.riot.api.database.exception.ValueNotPermittedException;
import com.tierconnect.riot.api.database.utils.ListAliasForTest;
import com.tierconnect.riot.api.mongoShell.testUtils.PropertiesReaderUtil;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

import static com.tierconnect.riot.api.database.base.Operation.*;
import static com.tierconnect.riot.api.database.base.alias.Alias.create;
import static com.tierconnect.riot.api.mongoShell.utils.CharacterUtils.betweenBraces;
import static junitparams.JUnitParamsRunner.$;
import static org.apache.commons.lang.StringUtils.join;
import static org.junit.Assert.*;

/**
 * Created by vealaro on 11/28/16.
 */
@RunWith(JUnitParamsRunner.class)
public class MongoTest {

    private static Logger logger = Logger.getLogger(MongoTest.class);
    private String simpleQuery = "{\"$and\":[{\"thingTypeId\":17},{\"$or\":[{\"$and\":[{\"groupId\":{\"$in\":[1,2,3,4,5,6,7]}},{\"thingTypeId\":{\"$in\":[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17]}}]}]}]}";
    private ConditionBuilder builder;
    private Mongo dataBase = FactoryDataBase.get(Mongo.class, new ConditionBuilder());
    private String expectedSubQuery = "{\"$and\":[{\"_id\":{\"$in\":db.thingSnapshotIds.find({\"$and\":[{\"blinks\":{\"$elemMatch\":{\"time\":{\"$gte\":1469973724000,\"$lte\":1486648926999}}}}]}).map(function(_paramToProject){ return _paramToProject.blinks[0].blink_id})}}]}";
    private String expectedSubQueryWithProjectionQuery = "{\"$and\":[{\"_id\":{\"$in\":db.thingSnapshotIds.find({\"$and\":[{\"blinks\":{\"$elemMatch\":{\"time\":{\"$gte\":1469973724000,\"$lte\":1486648926999}}}}]},{\"blinks\":{\"$elemMatch\":{\"time\":{\"$gte\":1469973724000,\"$lte\":1486648926999}}}}).map(function(_paramToProject){ return _paramToProject.blinks[0].blink_id})}}]}";

    static {
        try {
            PropertiesReaderUtil.setConfigurationFile("propertiesMongoShellLocalHost.properties");
        } catch (IOException e) {
            logger.error("Error in properties");
        }
    }

    @Before
    public void clearBefore() {
        builder = dataBase.getBuilder();
        builder.clear();
    }

    @Test
    public void testEmptyQuery() throws OperationNotSupportedException {
        assertEquals("{}", dataBase.getConditionBuilderString());
    }

    @Test
    @Parameters(method = "parametersForSingleOperator")
    public void testTransformSingleOperator(SingleOperator singleOperator, String expected) throws OperationNotSupportedException {
        assertNotNull(singleOperator);
        assertNotNull(expected);
        String actual = dataBase.transformSingleOperator(singleOperator);
        logger.debug("singleOperator " + singleOperator);
        logger.debug("mongo filter   " + actual);
        assertEquals(expected, actual);
    }

    @Test
    @Parameters(method = "parametersForSingleOperatorError")
    public void testTransformSingleOperatorError(SingleOperator singleOperator, String result) {
        assertNotNull(singleOperator);
        assertNotNull(result);
        try {
            dataBase.transformSingleOperator(singleOperator);
            fail("fail test for Single Operator ELEMENT MATCH with Multiple Operator");
        } catch (OperationNotImplementedException e) {
            assertEquals(e.getMessage(), "operation ELEMENT MATCH is not implemented as Multiple Operator");
        } catch (OperationNotSupportedException e) {
            fail("Operation not support - fail test for Single Operator with " + singleOperator.getOperator());
        }
    }

    @Test
    public void testValueNotPermitted() {
        try {
            in("_id", 1);
        } catch (ValueNotPermittedException ve) {
            assertEquals(ve.getMessage(), "this is object [1] of class [class java.lang.Integer] not permitted in method [in] with key [_id]");
        }
        try {
            in("_id", "2");
        } catch (ValueNotPermittedException ve) {
            assertEquals(ve.getMessage(), "this is object [2] of class [class java.lang.String] not permitted in method [in] with key [_id]");
        }
        try {
            notIn("_id", 1);
        } catch (ValueNotPermittedException ve) {
            assertEquals(ve.getMessage(), "this is object [1] of class [class java.lang.Integer] not permitted in method [notIn] with key [_id]");
        }
        try {
            notIn("_id", "2");
        } catch (ValueNotPermittedException ve) {
            assertEquals(ve.getMessage(), "this is object [2] of class [class java.lang.String] not permitted in method [notIn] with key [_id]");
        }
    }

    @Test
    public void testSimpleQuery1() throws OperationNotSupportedException, ValueNotPermittedException {
        builder.addOperator(Operation.equals("thingTypeId", 17));
        MultipleOperator multipleOperatorOR = new MultipleOperator(BooleanCondition.OR);
        MultipleOperator multipleOperatorAND = new MultipleOperator(BooleanCondition.AND);
        multipleOperatorAND.addOperator(in("groupId", Arrays.asList(1, 2, 3, 4, 5, 6, 7)));
        multipleOperatorAND.addOperator(in("thingTypeId", Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17)));
        multipleOperatorOR.addOperator(multipleOperatorAND);
        builder.addOperator(multipleOperatorOR);
        assertEquals(dataBase.getConditionBuilderString(), simpleQuery);
    }

    @Test
    public void testSimpleQuery2() throws OperationNotSupportedException, ValueNotPermittedException {
        builder.addOperator(Operation.equals("thingTypeId", 17));


        MultipleOperator multipleOperatorAND = new MultipleOperator(BooleanCondition.AND);
        multipleOperatorAND.addOperatorList(
                Arrays.asList(
                        in("groupId", Arrays.asList(1, 2, 3, 4, 5, 6, 7)),
                        in("thingTypeId", Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17))
                )
        );

        builder.addOperator(new MultipleOperator(BooleanCondition.OR, multipleOperatorAND));
        dataBase.setBuilder(builder);
        assertEquals(dataBase.getConditionBuilderString(), simpleQuery);
    }

    @Test
    public void testSimpleQuery3() throws OperationNotSupportedException, ValueNotPermittedException {
        builder.addOperator(Operation.equals("thingTypeId", 17));
        builder.addMultiple(BooleanCondition.OR,
                AND(
                        in("groupId", Arrays.asList(1, 2, 3, 4, 5, 6, 7)),
                        in("thingTypeId", Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17))
                )
        );
        dataBase.setBuilder(builder);
        assertEquals(dataBase.getConditionBuilderString(), simpleQuery);
    }

    @Test
    public void testSimpleQuery4() throws OperationNotSupportedException, ValueNotPermittedException {
        builder.addOperator(Operation.equals("thingTypeId", 17));
        builder.addOperator(OR(
                AND(
                        in("groupId", Arrays.asList(1, 2, 3, 4, 5, 6, 7)),
                        in("thingTypeId", Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17))
                )
        ));
        dataBase.setBuilder(builder);
        assertEquals(dataBase.getConditionBuilderString(), simpleQuery);
    }

    @Test
    public void testSimpleQuery5() throws OperationNotSupportedException, ValueNotPermittedException {
        builder.addOperator(Operation.equals("thingTypeId", 17));
        builder.addOperator(new MultipleOperator(BooleanCondition.OR).addOperator(
                AND(
                        in("groupId", Arrays.asList(1, 2, 3, 4, 5, 6, 7)),
                        in("thingTypeId", Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17))
                )
        ));
        dataBase.setBuilder(builder);
        assertEquals(dataBase.getConditionBuilderString(), simpleQuery);
    }

    @Test
    public void testSimpleQuery6() throws OperationNotSupportedException, ValueNotPermittedException {

        builder = new ConditionBuilder(
                Operation.equals("thingTypeId", 17),
                OR(
                        AND(
                                in("groupId", Arrays.asList(1, 2, 3, 4, 5, 6, 7)),
                                in("thingTypeId", Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17))
                        )
                )
        );
        dataBase.setBuilder(builder);
        assertEquals(dataBase.getConditionBuilderString(), simpleQuery);
    }

    @Test
    public void testSimpleQuery7() throws OperationNotSupportedException, ValueNotPermittedException {

        builder = new ConditionBuilder(BooleanCondition.AND,
                Operation.equals("thingTypeId", 17),
                OR(
                        AND(
                                in("groupId", Arrays.asList(1, 2, 3, 4, 5, 6, 7)),
                                in("thingTypeId", Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17))
                        )
                )
        );
        dataBase.setBuilder(builder);
        assertEquals(dataBase.getConditionBuilderString(), simpleQuery);
    }

    @Test
    public void testSimpleQuery8() throws OperationNotSupportedException, ValueNotPermittedException {

        builder = new ConditionBuilder(BooleanCondition.AND,
                Operation.equals("thingTypeId", 17),
                OR(
                        AND(
                                Arrays.asList(
                                        in("groupId", Arrays.asList(1, 2, 3, 4, 5, 6, 7)),
                                        in("thingTypeId", Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17))
                                )
                        )
                )
        );
        dataBase.setBuilder(builder);
        assertEquals(dataBase.getConditionBuilderString(), simpleQuery);
    }


    @Test
    @Parameters(method = "parametersForOrder")
    public void testOrder(String result, Map<String, Order> orderMap) {
        dataBase.setMapOrder(orderMap);
        assertEquals(result, dataBase.getSortString());
    }

    @Test
    @Parameters(method = "parametersForAlias")
    public void testAlias(String projection, String alias, ListAliasForTest listAliasForTest) {
        assertNotNull(listAliasForTest);
        assertNotNull(projection);
        assertNotNull(alias);
        dataBase.setAliasList(listAliasForTest.getAliasList());
        logger.debug("projection= " + projection);
        logger.debug("alias= " + alias);
        assertEquals(projection, (dataBase.getProjectionString()));
        assertEquals(alias, (dataBase.getAliasListString()));
    }


    @Test
    public void testAliasNull() {
        dataBase.setAliasList(null);
        assertEquals("{}", (dataBase.getProjectionString()));
        assertEquals("{\"_id\":{\"alias\":\"_id\",\"function\":\"none\"}}", (dataBase.getAliasListString()));
    }


    @Test
    public void testSortNullOrEmpty() {
        dataBase.setMapOrder(null);
        assertNull(dataBase.getSortString());
        dataBase.setMapOrder(new LinkedHashMap<String, Order>());
        assertNull(dataBase.getSortString());
    }

    @Test(expected = OperationNotSupportedException.class)
    public void testOperatorNull() throws NoSuchFieldException, IllegalAccessException, OperationNotSupportedException {
        SingleOperator singleOperator = new SingleOperator("name", OperationEnum.REGEX, "ASB");
        Field fieldOperator = singleOperator.getClass().getDeclaredField("operator");
        fieldOperator.setAccessible(true);
        fieldOperator.set(singleOperator, null);
        dataBase.transformSingleOperator(singleOperator);
    }

    /**
     * require format "property-ASC"
     *
     * @param ordervalues
     * @return
     */
    private Map<String, Order> createOrder(String... ordervalues) {
        Map<String, Order> map = new LinkedHashMap<>();
        for (String order : ordervalues) {
            String[] split = order.split("-");
            map.put(split[0], Order.valueOf(split[1]));
        }
        return map;
    }


    private Object[] parametersForAlias() {
        String formatAlias = "\"%1s\":{\"alias\":\"%2s\",\"function\":\"%3s\"}";
        return $(
                $("{}", betweenBraces(String.format(formatAlias, "_id", "_id", "none")), new ListAliasForTest()),
                $(betweenBraces("\"name\":1"),
                        betweenBraces(
                                join(Collections.singletonList(
                                        String.format(formatAlias, "name", "name", "none")), ",")
                        ),
                        new ListAliasForTest(create("name"))
                ),
                $(betweenBraces("\"time\":1,\n\"name\":1"), betweenBraces(
                        join(Arrays.asList(
                                String.format(formatAlias, "time", "time", "none"),
                                String.format(formatAlias, "name", "name", "none")), ",\n")
                        ),
                        new ListAliasForTest(create("time"), create("name"))),
                $(betweenBraces("\"time\":1,\n\"name\":1"), betweenBraces(
                        join(Arrays.asList(
                                String.format(formatAlias, "time", "time", "formatDate"),
                                String.format(formatAlias, "name", "name", "none")), ",\n")
                        ),
                        new ListAliasForTest(create("time", "time", "formatDate"), create("name"))),
                $(betweenBraces("\"time\":1,\n\"name\":1,\n\"dwelltime\":1,\n\"serialNumber\":1"), betweenBraces(
                        join(Arrays.asList(
                                String.format(formatAlias, "time", "time", "none"),
                                String.format(formatAlias, "name", "name", "none"),
                                String.format(formatAlias, "dwelltime", "dwelltime", "formatDwellTime"),
                                String.format(formatAlias, "serialNumber", "serial", "none")), ",\n")
                        ),
                        new ListAliasForTest(create("time"), create("name"), create("dwelltime", "dwelltime", "formatDwellTime"), create("serialNumber", "serial")))
        );
    }


    private Object[] parametersForOrder() {
        return $(
                $("{\"_id\":1,\"value.status\":1,\"value.name\":1,\"value.thingTypeId\":-1}",
                        createOrder("_id-ASC", "value.status-ASC", "value.name-ASC", "value.thingTypeId-DESC")),
                $("{\"_id\":1,\"value.status\":-1,\"value.name\":1,\"value.thingTypeId\":-1}",
                        createOrder("_id-ASC", "value.status-DESC", "value.name-ASC", "value.thingTypeId-DESC")),
                $("{\"_id\":-1,\"value.status\":1,\"value.name\":-1,\"value.thingTypeId\":1}",
                        createOrder("_id-DESC", "value.status-ASC", "value.name-DESC", "value.thingTypeId-ASC"))
        );
    }

    private Object[] parametersForSingleOperator() throws ValueNotPermittedException {
        List<Integer> listIntegers = new ArrayList<>(60);
        for (int i = 1; i <= 60; i++) {
            listIntegers.add(i);
        }
        return $(
                $(Operation.equals("status.value", "STOLEN"), "{\"status.value\":\"STOLEN\"}"),
                $(notEquals("status.value", "STOLEN"), "{\"status.value\":{\"$ne\":\"STOLEN\"}}"),
                $(greaterThan("age.value", 56), "{\"age.value\":{\"$gt\":56}}"),
                $(lessThan("age.value", 56), "{\"age.value\":{\"$lt\":56}}"),
                $(greaterThanOrEquals("age.value", 56), "{\"age.value\":{\"$gte\":56}}"),
                $(lessThanOrEquals("age.value", 56), "{\"age.value\":{\"$lte\":56}}"),
                $(contains("groupTypeName", "Fac"), "{\"groupTypeName\":{\"$regex\":/Fac/}}"),
                $(startsWith("groupTypeName", "Fac"), "{\"groupTypeName\":{\"$regex\":/^Fac?/}}"),
                $(endsWith("groupTypeName", "Fac"), "{\"groupTypeName\":{\"$regex\":/.*Fac\\\\\\b/}}"),
                $(in("groupTypeName", Arrays.asList("Facility", "item")), "{\"groupTypeName\":{\"$in\":[\"Facility\",\"item\"]}}"),
                $(in("groupTypeName", Arrays.asList(1, 2, 3, 4)), "{\"groupTypeName\":{\"$in\":[1,2,3,4]}}"),
                $(in("groupTypeName", Collections.singletonList("Facility")),"{\"groupTypeName\":{\"$in\":[\"Facility\"]}}"),
                $(in("groupTypeName", Collections.singletonList(1)), "{\"groupTypeName\":{\"$in\":[1]}}"),
                $(in("groupTypeName", Arrays.asList(null, "Facility")), "{\"groupTypeName\":{\"$in\":[null,\"Facility\"]}}"),
                $(in("groupTypeName", Arrays.asList(null,1)), "{\"groupTypeName\":{\"$in\":[null,1]}}"),
                $(in("groupTypeName", Collections.singletonList(null)), "{\"groupTypeName\":{\"$in\":[null]}}"),
                $(in("groupTypeName", listIntegers), "{\"groupTypeName\":{\"$in\":[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20," +
                        "21,22,23,24,25\n,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49\n,50,51,52,53,54,55,56,57,58,59,60]}}"),
                $(notIn("groupTypeName", Arrays.asList("Facility", "item")), "{\"groupTypeName\":{\"$nin\":[\"Facility\",\"item\"]}}"),
                $(notIn("groupTypeName", Arrays.asList(1, 2, 3, 4)), "{\"groupTypeName\":{\"$nin\":[1,2,3,4]}}"),
                $(between("price", 10, 20), "{\"price\":{\"$gte\":10,\"$lte\":20}}"),
                $(exists("price"), "{\"price\":{\"$exists\":true}}"),
                $(notExists("price"), "{\"price\":{\"$exists\":false}}"),
                $(empty("name"), "{\"name\":{\"$eq\":\"\"}}"),
                $(notEmpty("name"), "{\"name\":{\"$ne\":\"\"}}"),
                $(isNull("name"), "{\"$and\":[{\"name\":{\"$exists\":true}},{\"name\":{\"$eq\":null}}]}"),
                $(isNotNull("name"), "{\"name\":{\"$ne\":null}}"),
                $(emptyArray("children"), "{\"$or\":[{\"children\":{\"$exists\":false}},{\"children\":null},{\"children\":{\"$size\":0}}]}"),
                $(emptyArray("parent"), "{\"$or\":[{\"parent\":{\"$exists\":false}},{\"parent\":null},{\"parent\":{\"$size\":0}}]}"),
                $(notEmptyArray("children"), "{\"children\":{\"$exists\":true,\"$not\":{\"$size\":0}}}"),
                $(notEmptyArray("parent"), "{\"parent\":{\"$exists\":true,\"$not\":{\"$size\":0}}}"),
                $(arraySizeMatch("children", 0), "{\"children\":{\"$size\":0}}"),
                $(arraySizeMatch("children", 5), "{\"children\":{\"$size\":5}}"),
                $(regex("name", "4", "i"), "{\"name\":{\"$regex\":\"4\",\"$options\":\"i\"}}"),
                $(regex("status.value", "abc", "x"), "{\"status.value\":{\"$regex\":\"abc\",\"$options\":\"x\"}}"),
                $(elementMatch("children", Operation.equals("thingTypeId", 1)), "{\"children\":{\"$elemMatch\":{\"thingTypeId\":1}}}"),
                $(elementMatch("results", Operation.equals("score", 8), Operation.equals("product", "abc")), "{\"results\":{\"$elemMatch\":{\"score\":8,\"product\":\"abc\"}}}"),
                $(elementMatch("children", Operation.equals("thingTypeId", 1), Operation.equals("groupTypeId", 3), in("groupCode", Arrays.asList("SM", "SS"))), "{\"children\":{\"$elemMatch\":{\"thingTypeId\":1,\"groupTypeId\":3,\"groupCode\":{\"$in\":[\"SM\",\"SS\"]}}}}")
        );
    }

    private Object[] parametersForSingleOperatorError() throws ValueNotPermittedException {
        return $(
                $(elementMatch("results", AND(Operation.equals("score", 8), Operation.equals("product", "abc"))),
                        "{\"results\":{\"$elemMatch\":{\"score\":{\"$eq\":8},\"product\":{\"$eq\":\"abc\"}}}}"),
                $(elementMatch("children", Operation.equals("thingTypeId", 1),
                        OR(Operation.equals("groupTypeId", 3), in("groupCode", Arrays.asList("SM", "SS")))),
                        "{\"children\":{\"$elemMatch\":{\"thingTypeId\":{\"$eq\":1}}}}")
        );
    }

    @Test
    public void subqueryInTest() throws OperationNotSupportedException {
        ConditionBuilder query = new ConditionBuilder();
        ConditionBuilder subQuery = new ConditionBuilder();
        subQuery.addOperator(Operation.elementMatch("blinks",
                Operation.between("time", 1469973724000L, 1486648926999L)));
        query.addOperator(Operation.inSubquery("_id", subQuery, "thingSnapshotIds", "blinks[0].blink_id"));
        Mongo mongoBase = new Mongo(query);
        assertEquals(expectedSubQuery, mongoBase.getConditionBuilderString());
    }

    @Test
    public void subqueryInTestWithProjection() throws OperationNotSupportedException {
        ConditionBuilder query = new ConditionBuilder();
        ConditionBuilder subQuery = new ConditionBuilder();
        subQuery.addOperator(Operation.elementMatch("blinks",
                Operation.between("time", 1469973724000L, 1486648926999L)));
        query.addOperator(Operation.inSubquery("_id", subQuery, "thingSnapshotIds", "blinks[0].blink_id", subQuery));
        Mongo mongoBase = new Mongo(query);
        assertEquals(expectedSubQueryWithProjectionQuery, mongoBase.getConditionBuilderString());
    }

}