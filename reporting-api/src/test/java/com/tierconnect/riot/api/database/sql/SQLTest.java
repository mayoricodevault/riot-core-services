package com.tierconnect.riot.api.database.sql;

import com.tierconnect.riot.api.database.base.FactoryDataBase;
import com.tierconnect.riot.api.database.base.Operation;
import com.tierconnect.riot.api.database.base.conditions.BooleanCondition;
import com.tierconnect.riot.api.database.base.conditions.ConditionBuilder;
import com.tierconnect.riot.api.database.base.operator.SingleOperator;
import com.tierconnect.riot.api.database.base.ordination.Order;
import com.tierconnect.riot.api.database.exception.OperationNotSupportedException;
import com.tierconnect.riot.api.database.exception.ValueNotPermittedException;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static junitparams.JUnitParamsRunner.$;
import static org.junit.Assert.*;

/**
 * Project: reporting-api
 * Author: edwin
 * Date: 28/11/2016
 */
@RunWith(JUnitParamsRunner.class)
public class SQLTest {

    private static Logger logger = Logger.getLogger(SQLTest.class);
    private SQL dataBase = FactoryDataBase.get(SQL.class, new ConditionBuilder());

    @Before
    public void clear() {
        dataBase.getBuilder().clear();
    }

    @Test
    @Parameters(method = "parametersForSingleOperator")
    public void testTransform(SingleOperator singleOperator, String result) throws Exception {
        assertNotNull(singleOperator);
        assertNotNull(result);
        String expected = dataBase.transformSingleOperator(singleOperator);
        logger.debug("SQL filter     " + expected);
        assertEquals(expected, result);
    }

    @Test
    @Parameters(method = "parametersForSingleOperatorError")
    public void testOperationNotSupported(SingleOperator singleOperator) {
        assertNotNull(singleOperator);
        try {
            dataBase.transformSingleOperator(singleOperator);
            fail("fail test operation support");
        } catch (OperationNotSupportedException e) {
            assertEquals(singleOperator.getOperator() + " Operation not supported in MYSQL", e.getMessage());
        }
    }

    @Test
    public void testOrder() {
        Map<String, Order> orderMap = new LinkedHashMap<>();
        orderMap.put("_id", Order.DESC);
        orderMap.put("name", Order.ASC);
        dataBase.setMapOrder(orderMap);
        assertEquals("_id DESC,name ASC", dataBase.getSortString());
        orderMap.clear();
        dataBase.setMapOrder(orderMap);
        assertEquals("", dataBase.getSortString());
        dataBase.setMapOrder(null);
        assertEquals("", dataBase.getSortString());
    }

    @Test
    public void multipleConditionBuilder() throws OperationNotSupportedException {
        ConditionBuilder builder = new ConditionBuilder();
        dataBase.setBuilder(builder);
        dataBase.getBuilder().addOperator(Operation.contains("name", "abc"));
        dataBase.getBuilder().addOperator(Operation.contains("surname", "xyz"));
        assertEquals("name LIKE '%abc%' AND surname LIKE '%xyz%'", dataBase.getConditionBuilderString());

        builder = new ConditionBuilder(BooleanCondition.OR);
        dataBase.setBuilder(builder);
        dataBase.getBuilder().addOperator(Operation.contains("name", "abc"));
        dataBase.getBuilder().addOperator(Operation.contains("surname", "xyz"));
        assertEquals("name LIKE '%abc%' OR surname LIKE '%xyz%'", dataBase.getConditionBuilderString());

        builder = new ConditionBuilder(BooleanCondition.OR);
        dataBase.setBuilder(builder);
        dataBase.getBuilder().addMultiple(BooleanCondition.OR, Operation.contains("name", "abc"), Operation.contains("surname", "xyz"));
        assertEquals("(name LIKE '%abc%' OR surname LIKE '%xyz%')", dataBase.getConditionBuilderString());

        builder = new ConditionBuilder(BooleanCondition.OR);
        dataBase.setBuilder(builder);
        dataBase.getBuilder().addMultiple(BooleanCondition.AND, Operation.contains("name", "abc"), Operation.contains("surname", "xyz"));
        assertEquals("(name LIKE '%abc%' AND surname LIKE '%xyz%')", dataBase.getConditionBuilderString());

        builder = new ConditionBuilder();
        builder.addOperator(Operation.in("id", Collections.emptyList()));
        dataBase.setBuilder(builder);
        assertEquals("id IN ()", dataBase.getConditionBuilderString());

    }

    @Test(expected = UnsupportedOperationException.class)
    public void executeNotimplemented() {
        dataBase.getBuilder().addOperator(Operation.in("id", Arrays.asList(1, 2, 3, 4, 5)));
        dataBase.execute("user0", null);
    }

    private Object[] parametersForSingleOperator() throws ValueNotPermittedException {
        return $(
                $(Operation.equals("status", "STOLEN"), "status = 'STOLEN'"),
                $(Operation.notEquals("status", "STOLEN"), "status <> 'STOLEN'"),
                $(Operation.greaterThan("age", 56), "age > 56"),
                $(Operation.lessThan("age", 56), "age < 56"),
                $(Operation.greaterThanOrEquals("age", 56), "age >= 56"),
                $(Operation.lessThanOrEquals("age", 56), "age <= 56"),
                $(Operation.contains("groupTypeName", "Fac"), "groupTypeName LIKE '%Fac%'"),
                $(Operation.startsWith("groupTypeName", "Fac"), "groupTypeName LIKE 'Fac%'"),
                $(Operation.endsWith("groupTypeName", "Fac"), "groupTypeName LIKE '%Fac'"),
                $(Operation.in("groupTypeName", Arrays.asList("Facility", "item")), "groupTypeName IN ('Facility','item')"),
                $(Operation.in("groupTypeName", Arrays.asList(1, 2, 3, 4)), "groupTypeName IN (1,2,3,4)"),
                $(Operation.notIn("groupTypeName", Arrays.asList("Facility", "item")), "groupTypeName NOT IN ('Facility','item')"),
                $(Operation.notIn("groupTypeName", Arrays.asList(1, 2, 3, 4)), "groupTypeName NOT IN (1,2,3,4)"),
                $(Operation.between("price", 10, 20), "price BETWEEN 10 AND 20"),
                $(Operation.empty("name"), "name = ''"),
                $(Operation.notEmpty("name"), "name <> ''"),
                $(Operation.isNull("name"), "name IS NULL"),
                $(Operation.isNotNull("name"), "name IS NOT NULL")
        );
    }

    private Object[] parametersForSingleOperatorError() throws ValueNotPermittedException {
        return $(
                $(Operation.exists("status")),
                $(Operation.notExists("status")),
                $(Operation.emptyArray("age")),
                $(Operation.notEmptyArray("age")),
                $(Operation.arraySizeMatch("age", 6)),
                $(Operation.regex("age", "ABC", "option")),
                $(Operation.elementMatch("children", Operation.equals("thingTypeId", 1)))
        );
    }

}