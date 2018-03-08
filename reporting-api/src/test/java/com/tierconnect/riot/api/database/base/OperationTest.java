package com.tierconnect.riot.api.database.base;

import com.tierconnect.riot.api.database.base.annotations.ClassesAllowed;
import com.tierconnect.riot.api.database.exception.ValueNotPermittedException;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Map;

import static com.tierconnect.riot.api.database.base.Operation.*;
import static org.junit.Assert.*;

/**
 * Created by vealaro on 11/29/16.
 */
public class OperationTest {

    private static final Logger logger = Logger.getLogger(OperationTest.class);
    private static Calendar instance = Calendar.getInstance();

    static {
        instance.set(Calendar.HOUR_OF_DAY, 0);
        instance.set(Calendar.MINUTE, 0);
        instance.set(Calendar.SECOND, 0);
        instance.set(Calendar.MILLISECOND, 0);
        instance.set(2016, Calendar.NOVEMBER, 28);
    }

    @Test
    public void testOR_AND() {
        try {
            assertEquals(
                    OR(
                            Operation.equals("STATUS", "ABC"), Operation.equals("STATUS", "XYZ")).toString(),
                    "(\"STATUS\" EQUALS \"ABC\" OR \"STATUS\" EQUALS \"XYZ\")");

            assertEquals(
                    OR(
                            Arrays.asList(Operation.equals("STATUS", "ABC"), Operation.equals("STATUS", "XYZ"))).toString(),
                    "(\"STATUS\" EQUALS \"ABC\" OR \"STATUS\" EQUALS \"XYZ\")");

            assertEquals(
                    OR(
                            Operation.equals("STATUS", "ABC"), Operation.greaterThan("PRICE", 2),
                            Operation.between("YEAR", 2010, 2015)).toString(),
                    "(\"STATUS\" EQUALS \"ABC\" OR \"PRICE\" GREATER_THAN 2 OR \"YEAR\" BETWEEN [2010, 2015])");

            assertEquals(
                    OR(
                            Arrays.asList(Operation.equals("STATUS", "ABC"), Operation.greaterThan("PRICE", 2),
                                    Operation.between("YEAR", 2010, 2015))).toString(),
                    "(\"STATUS\" EQUALS \"ABC\" OR \"PRICE\" GREATER_THAN 2 OR \"YEAR\" BETWEEN [2010, 2015])");

            assertEquals(
                    AND(
                            Operation.OR(Operation.equals("STATUS", "ABC"), Operation.equals("STATUS", "DEF")),
                            Operation.equals("PRICE", 200), Operation.equals("PRICE", 300)).toString(),
                    "((\"STATUS\" EQUALS \"ABC\" OR \"STATUS\" EQUALS \"DEF\") AND \"PRICE\" EQUALS 200 AND \"PRICE\" EQUALS 300)");

            assertEquals(
                    AND(
                            Arrays.asList(Operation.OR(Operation.equals("STATUS", "ABC"), Operation.equals("STATUS", "DEF")),
                                    Operation.equals("PRICE", 200), Operation.equals("PRICE", 300))).toString(),
                    "((\"STATUS\" EQUALS \"ABC\" OR \"STATUS\" EQUALS \"DEF\") AND \"PRICE\" EQUALS 200 AND \"PRICE\" EQUALS 300)");

            assertEquals(
                    AND(
                            Operation.OR(Operation.equals("STATUS", "ABC"), Operation.equals("STATUS", "DEF")),
                            Operation.AND(Operation.equals("PRICE", 200), Operation.equals("PRICE", 300))).toString(),
                    "((\"STATUS\" EQUALS \"ABC\" OR \"STATUS\" EQUALS \"DEF\") AND (\"PRICE\" EQUALS 200 AND \"PRICE\" EQUALS 300))");

            assertEquals(
                    AND(
                            Arrays.asList(Operation.OR(Operation.equals("STATUS", "ABC"), Operation.equals("STATUS", "DEF")),
                                    Operation.AND(Operation.equals("PRICE", 200), Operation.equals("PRICE", 300)))).toString(),
                    "((\"STATUS\" EQUALS \"ABC\" OR \"STATUS\" EQUALS \"DEF\") AND (\"PRICE\" EQUALS 200 AND \"PRICE\" EQUALS 300))");

            assertEquals(
                    AND(
                            Operation.OR(Operation.equals("STATUS", "ABC"), Operation.equals("STATUS", "DEF")),
                            Operation.OR(Operation.equals("PRICE", 200), Operation.equals("PRICE", 300))).toString(),
                    "((\"STATUS\" EQUALS \"ABC\" OR \"STATUS\" EQUALS \"DEF\") AND (\"PRICE\" EQUALS 200 OR \"PRICE\" EQUALS 300))");

            assertEquals(
                    OR(
                            Operation.AND(Operation.equals("STATUS", "ABC"), Operation.equals("PRICE", 200)),
                            Operation.AND(Operation.equals("STATUS", "DEF"), Operation.equals("PRICE", 300))).toString(),
                    "((\"STATUS\" EQUALS \"ABC\" AND \"PRICE\" EQUALS 200) OR (\"STATUS\" EQUALS \"DEF\" AND \"PRICE\" EQUALS 300))");

            assertEquals(
                    OR(
                            Arrays.asList(Operation.AND(Operation.equals("STATUS", "ABC"), Operation.equals("PRICE", 200)),
                                    Operation.AND(Operation.equals("STATUS", "DEF"), Operation.equals("PRICE", 300)))).toString(),
                    "((\"STATUS\" EQUALS \"ABC\" AND \"PRICE\" EQUALS 200) OR (\"STATUS\" EQUALS \"DEF\" AND \"PRICE\" EQUALS 300))");

            assertEquals(
                    AND(Operation.equals("STATUS", "ABC"), Operation.lessThan("YEAR", 2016)).toString(),
                    "(\"STATUS\" EQUALS \"ABC\" AND \"YEAR\" LESS_THAN 2016)");

            assertEquals(
                    AND(Arrays.asList(Operation.equals("STATUS", "ABC"), Operation.lessThan("YEAR", 2016))).toString(),
                    "(\"STATUS\" EQUALS \"ABC\" AND \"YEAR\" LESS_THAN 2016)");
            logger.info("The tests finished for Multiple operation OR - AND");
        } catch (ValueNotPermittedException e) {
            fail("fail for Multiple operation OR - AND");
        }
    }

    @Test
    public void testEquals() {
        try {
            assertEquals(Operation.equals("STATUS", instance.getTime()).toString(), "\"STATUS\" EQUALS Mon Nov 28 00:00:00 UTC 2016");
            assertEquals(Operation.equals("STATUS", 1).toString(), "\"STATUS\" EQUALS 1");
            assertEquals(Operation.equals("STATUS", 1L).toString(), "\"STATUS\" EQUALS 1");
            assertEquals(Operation.equals("STATUS", 1.0).toString(), "\"STATUS\" EQUALS 1.0");
            assertEquals(Operation.equals("STATUS", 1.0f).toString(), "\"STATUS\" EQUALS 1.0");
            assertEquals(Operation.equals("STATUS", 1.0d).toString(), "\"STATUS\" EQUALS 1.0");
            assertEquals(Operation.equals("STATUS", BigDecimal.ONE).toString(), "\"STATUS\" EQUALS 1");
            assertEquals(Operation.equals("STATUS", "1").toString(), "\"STATUS\" EQUALS \"1\"");
            assertEquals(Operation.equals("STATUS", false).toString(), "\"STATUS\" EQUALS false");
            assertEquals(Operation.equals("STATUS", Boolean.TRUE).toString(), "\"STATUS\" EQUALS true");
            assertEquals(Operation.equals("STATUS", Float.parseFloat("2.35f")).toString(), "\"STATUS\" EQUALS 2.35");
            assertEquals(Operation.equals("STATUS", Float.valueOf("2.35f")).toString(), "\"STATUS\" EQUALS 2.35");
            assertEquals(Operation.equals("STATUS", Double.parseDouble("2.35d")).toString(), "\"STATUS\" EQUALS 2.35");
            assertEquals(Operation.equals("STATUS", Double.valueOf("2.35d")).toString(), "\"STATUS\" EQUALS 2.35");
            assertEquals(Operation.equals("STATUS", Double.valueOf("2.35d")).toString(), "\"STATUS\" EQUALS 2.35");
            assertEquals(Operation.equals("STATUS", 1480448422222L).toString(), "\"STATUS\" EQUALS 1480448422222");
            logger.info("The tests finished for EQUALS");
        } catch (ValueNotPermittedException e) {
            fail("fail operation EQUALS");
        }
    }

    @Test
    public void testEqualsFailed() {
        try {
            assertEquals(Operation.equals("STATUS", Arrays.asList(0, 2)).toString(), "\"STATUS\" EQUALS [0, 2]");
            fail("not valid value");
        } catch (ValueNotPermittedException e) {
            assertTrue(e.getMessage().contains("not permitted in method [equals] with key [STATUS]"));
        }
        try {
            assertEquals(Operation.equals("STATUS", new StringBuilder("0")).toString(), "\"STATUS\" EQUALS 0");
            fail("not valid value");
        } catch (ValueNotPermittedException e) {
            assertTrue(e.getMessage().contains("not permitted in method [equals] with key [STATUS]"));
        }
    }

    @Test
    public void testNotEquals() {
        try {
            assertEquals(notEquals("STATUS", instance.getTime()).toString(), "\"STATUS\" NOT_EQUALS Mon Nov 28 00:00:00 UTC 2016");
            assertEquals(notEquals("STATUS", 1).toString(), "\"STATUS\" NOT_EQUALS 1");
            assertEquals(notEquals("STATUS", 1L).toString(), "\"STATUS\" NOT_EQUALS 1");
            assertEquals(notEquals("STATUS", 1.0).toString(), "\"STATUS\" NOT_EQUALS 1.0");
            assertEquals(notEquals("STATUS", 1.0f).toString(), "\"STATUS\" NOT_EQUALS 1.0");
            assertEquals(notEquals("STATUS", 1.0d).toString(), "\"STATUS\" NOT_EQUALS 1.0");
            assertEquals(notEquals("STATUS", BigDecimal.ONE).toString(), "\"STATUS\" NOT_EQUALS 1");
            assertEquals(notEquals("STATUS", "1").toString(), "\"STATUS\" NOT_EQUALS \"1\"");
            assertEquals(notEquals("STATUS", false).toString(), "\"STATUS\" NOT_EQUALS false");
            assertEquals(notEquals("STATUS", Boolean.TRUE).toString(), "\"STATUS\" NOT_EQUALS true");
            assertEquals(notEquals("STATUS", Float.parseFloat("2.35f")).toString(), "\"STATUS\" NOT_EQUALS 2.35");
            assertEquals(notEquals("STATUS", Float.valueOf("2.35f")).toString(), "\"STATUS\" NOT_EQUALS 2.35");
            assertEquals(notEquals("STATUS", Double.parseDouble("2.35d")).toString(), "\"STATUS\" NOT_EQUALS 2.35");
            assertEquals(notEquals("STATUS", Double.valueOf("2.35d")).toString(), "\"STATUS\" NOT_EQUALS 2.35");
            assertEquals(notEquals("STATUS", Double.valueOf("2.35d")).toString(), "\"STATUS\" NOT_EQUALS 2.35");
            assertEquals(notEquals("STATUS", 1480448422222L).toString(), "\"STATUS\" NOT_EQUALS 1480448422222");
            logger.info("The tests finished for NOT EQUALS");
        } catch (ValueNotPermittedException e) {
            fail("fail operation NOT EQUALS");
        }
    }

    @Test
    public void testGreaterThan() {
        try {
            assertEquals(greaterThan("STATUS", instance.getTime()).toString(), "\"STATUS\" GREATER_THAN Mon Nov 28 00:00:00 UTC 2016");
            assertEquals(greaterThan("STATUS", 1).toString(), "\"STATUS\" GREATER_THAN 1");
            assertEquals(greaterThan("STATUS", 1L).toString(), "\"STATUS\" GREATER_THAN 1");
            assertEquals(greaterThan("STATUS", 1.0).toString(), "\"STATUS\" GREATER_THAN 1.0");
            assertEquals(greaterThan("STATUS", 1.0f).toString(), "\"STATUS\" GREATER_THAN 1.0");
            assertEquals(greaterThan("STATUS", 1.0d).toString(), "\"STATUS\" GREATER_THAN 1.0");
            assertEquals(greaterThan("STATUS", BigDecimal.ONE).toString(), "\"STATUS\" GREATER_THAN 1");
            assertEquals(greaterThan("STATUS", Float.parseFloat("2.35f")).toString(), "\"STATUS\" GREATER_THAN 2.35");
            assertEquals(greaterThan("STATUS", Float.valueOf("2.35f")).toString(), "\"STATUS\" GREATER_THAN 2.35");
            assertEquals(greaterThan("STATUS", Double.parseDouble("2.35d")).toString(), "\"STATUS\" GREATER_THAN 2.35");
            assertEquals(greaterThan("STATUS", Double.valueOf("2.35d")).toString(), "\"STATUS\" GREATER_THAN 2.35");
            assertEquals(greaterThan("STATUS", Double.valueOf("2.35d")).toString(), "\"STATUS\" GREATER_THAN 2.35");
            assertEquals(greaterThan("STATUS", 1480448422222L).toString(), "\"STATUS\" GREATER_THAN 1480448422222");
            logger.info("The tests finished for GREATER THAN");
        } catch (ValueNotPermittedException e) {
            fail("fail operation GREATER THAN");
        }
    }

    @Test
    public void testLessThan() {
        try {
            assertEquals(lessThan("STATUS", instance.getTime()).toString(), "\"STATUS\" LESS_THAN Mon Nov 28 00:00:00 UTC 2016");
            assertEquals(lessThan("STATUS", 1).toString(), "\"STATUS\" LESS_THAN 1");
            assertEquals(lessThan("STATUS", 1L).toString(), "\"STATUS\" LESS_THAN 1");
            assertEquals(lessThan("STATUS", 1.0).toString(), "\"STATUS\" LESS_THAN 1.0");
            assertEquals(lessThan("STATUS", 1.0f).toString(), "\"STATUS\" LESS_THAN 1.0");
            assertEquals(lessThan("STATUS", 1.0d).toString(), "\"STATUS\" LESS_THAN 1.0");
            assertEquals(lessThan("STATUS", BigDecimal.ONE).toString(), "\"STATUS\" LESS_THAN 1");
            assertEquals(lessThan("STATUS", Float.parseFloat("2.35f")).toString(), "\"STATUS\" LESS_THAN 2.35");
            assertEquals(lessThan("STATUS", Float.valueOf("2.35f")).toString(), "\"STATUS\" LESS_THAN 2.35");
            assertEquals(lessThan("STATUS", Double.parseDouble("2.35d")).toString(), "\"STATUS\" LESS_THAN 2.35");
            assertEquals(lessThan("STATUS", Double.valueOf("2.35d")).toString(), "\"STATUS\" LESS_THAN 2.35");
            assertEquals(lessThan("STATUS", Double.valueOf("2.35d")).toString(), "\"STATUS\" LESS_THAN 2.35");
            assertEquals(lessThan("STATUS", 1480448422222L).toString(), "\"STATUS\" LESS_THAN 1480448422222");
            logger.info("The tests finished for LESS THAN");
        } catch (ValueNotPermittedException e) {
            fail("fail operation LESS THAN");
        }
    }

    @Test
    public void testGreaterThanOrEquals() {
        try {
            assertEquals(greaterThanOrEquals("STATUS", instance.getTime()).toString(), "\"STATUS\" GREATER_THAN_OR_EQUALS Mon Nov 28 00:00:00 UTC 2016");
            assertEquals(greaterThanOrEquals("STATUS", 1).toString(), "\"STATUS\" GREATER_THAN_OR_EQUALS 1");
            assertEquals(greaterThanOrEquals("STATUS", 1L).toString(), "\"STATUS\" GREATER_THAN_OR_EQUALS 1");
            assertEquals(greaterThanOrEquals("STATUS", 1.0).toString(), "\"STATUS\" GREATER_THAN_OR_EQUALS 1.0");
            assertEquals(greaterThanOrEquals("STATUS", 1.0f).toString(), "\"STATUS\" GREATER_THAN_OR_EQUALS 1.0");
            assertEquals(greaterThanOrEquals("STATUS", 1.0d).toString(), "\"STATUS\" GREATER_THAN_OR_EQUALS 1.0");
            assertEquals(greaterThanOrEquals("STATUS", BigDecimal.ONE).toString(), "\"STATUS\" GREATER_THAN_OR_EQUALS 1");
            assertEquals(greaterThanOrEquals("STATUS", Float.parseFloat("2.35f")).toString(), "\"STATUS\" GREATER_THAN_OR_EQUALS 2.35");
            assertEquals(greaterThanOrEquals("STATUS", Float.valueOf("2.35f")).toString(), "\"STATUS\" GREATER_THAN_OR_EQUALS 2.35");
            assertEquals(greaterThanOrEquals("STATUS", Double.parseDouble("2.35d")).toString(), "\"STATUS\" GREATER_THAN_OR_EQUALS 2.35");
            assertEquals(greaterThanOrEquals("STATUS", Double.valueOf("2.35d")).toString(), "\"STATUS\" GREATER_THAN_OR_EQUALS 2.35");
            assertEquals(greaterThanOrEquals("STATUS", Double.valueOf("2.35d")).toString(), "\"STATUS\" GREATER_THAN_OR_EQUALS 2.35");
            assertEquals(greaterThanOrEquals("STATUS", 1480448422222L).toString(), "\"STATUS\" GREATER_THAN_OR_EQUALS 1480448422222");
            logger.info("The tests finished for GREATER THAN OR EQUALS");
        } catch (ValueNotPermittedException e) {
            fail("fail operation GREATER THAN OR EQUALS");
        }
    }

    @Test
    public void testLessThanOrEquals() {
        try {
            assertEquals(lessThanOrEquals("STATUS", instance.getTime()).toString(), "\"STATUS\" LESS_THAN_OR_EQUALS Mon Nov 28 00:00:00 UTC 2016");
            assertEquals(lessThanOrEquals("STATUS", 1).toString(), "\"STATUS\" LESS_THAN_OR_EQUALS 1");
            assertEquals(lessThanOrEquals("STATUS", 1L).toString(), "\"STATUS\" LESS_THAN_OR_EQUALS 1");
            assertEquals(lessThanOrEquals("STATUS", 1.0).toString(), "\"STATUS\" LESS_THAN_OR_EQUALS 1.0");
            assertEquals(lessThanOrEquals("STATUS", 1.0f).toString(), "\"STATUS\" LESS_THAN_OR_EQUALS 1.0");
            assertEquals(lessThanOrEquals("STATUS", 1.0d).toString(), "\"STATUS\" LESS_THAN_OR_EQUALS 1.0");
            assertEquals(lessThanOrEquals("STATUS", BigDecimal.ONE).toString(), "\"STATUS\" LESS_THAN_OR_EQUALS 1");
            assertEquals(lessThanOrEquals("STATUS", Float.parseFloat("2.35f")).toString(), "\"STATUS\" LESS_THAN_OR_EQUALS 2.35");
            assertEquals(lessThanOrEquals("STATUS", Float.valueOf("2.35f")).toString(), "\"STATUS\" LESS_THAN_OR_EQUALS 2.35");
            assertEquals(lessThanOrEquals("STATUS", Double.parseDouble("2.35d")).toString(), "\"STATUS\" LESS_THAN_OR_EQUALS 2.35");
            assertEquals(lessThanOrEquals("STATUS", Double.valueOf("2.35d")).toString(), "\"STATUS\" LESS_THAN_OR_EQUALS 2.35");
            assertEquals(lessThanOrEquals("STATUS", Double.valueOf("2.35d")).toString(), "\"STATUS\" LESS_THAN_OR_EQUALS 2.35");
            assertEquals(lessThanOrEquals("STATUS", 1480448422222L).toString(), "\"STATUS\" LESS_THAN_OR_EQUALS 1480448422222");
            logger.info("The tests finished for LESS THAN OR EQUALS");
        } catch (ValueNotPermittedException e) {
            fail("fail operation LESS THAN OR EQUALS");
        }
    }

    @Test
    public void testContainsStartsWithEndsWith() {
        try {
            assertEquals(contains("STATUS", "122334454556667777").toString(), "\"STATUS\" CONTAINS \"122334454556667777\"");
            assertEquals(startsWith("STATUS", "63637378383").toString(), "\"STATUS\" STARTS_WITH \"63637378383\"");
            assertEquals(endsWith("STATUS", "ABCDF").toString(), "\"STATUS\" ENDS_WITH \"ABCDF\"");
            logger.info("The tests finished for CONTAINS, STARTS WITH and ENDS WITH");
        } catch (ValueNotPermittedException e) {
            fail("fail operation CONTAINS, STARTS WITH and ENDS WITH");
        }

    }

    @Test
    public void testInNotIn() {
        try {
            assertEquals(in("STATUS", Arrays.asList(1, 2, 3, 4)).toString(), "\"STATUS\" IN [1, 2, 3, 4]");
            assertEquals(in("STATUS", Arrays.asList("AA", "AB", "AC", "AD")).toString(), "\"STATUS\" IN [AA, AB, AC, AD]");
            assertEquals(in("STATUS", Arrays.asList(1, 2, "AC", "AD")).toString(), "\"STATUS\" IN [1, 2, AC, AD]");
            assertEquals(notIn("STATUS", Arrays.asList(1, 2, 3, 4)).toString(), "\"STATUS\" NOT_IN [1, 2, 3, 4]");
            assertEquals(notIn("STATUS", Arrays.asList("AA", "AB", "AC", "AD")).toString(), "\"STATUS\" NOT_IN [AA, AB, AC, AD]");
            assertEquals(notIn("STATUS", Arrays.asList("AA", "AB", 3, 4)).toString(), "\"STATUS\" NOT_IN [AA, AB, 3, 4]");
            logger.info("The tests finished for IN and NOT IN");
        } catch (ValueNotPermittedException e) {
            fail("fail operation CONTAINS, STARTS WITH and ENDS WITH");
        }
    }

    @Test
    public void testBetween() {
        try {
            assertEquals(between("STATUS", instance.getTime(), instance.getTime()).toString(), "\"STATUS\" BETWEEN [Mon Nov 28 00:00:00 UTC 2016, Mon Nov 28 00:00:00 UTC 2016]");
            assertEquals(between("STATUS", 0, 100).toString(), "\"STATUS\" BETWEEN [0, 100]");
            assertEquals(between("STATUS", 0.5f, 100f).toString(), "\"STATUS\" BETWEEN [0.5, 100.0]");
            assertEquals(between("STATUS", 0.5d, 100d).toString(), "\"STATUS\" BETWEEN [0.5, 100.0]");
            assertEquals(between("STATUS", 1L, 1480448422222L).toString(), "\"STATUS\" BETWEEN [1, 1480448422222]");
            assertEquals(between("STATUS", BigDecimal.ZERO, BigDecimal.TEN).toString(), "\"STATUS\" BETWEEN [0, 10]");
            assertEquals(between("STATUS", Float.parseFloat("0.5f"), Float.parseFloat("100f")).toString(), "\"STATUS\" BETWEEN [0.5, 100.0]");
            assertEquals(between("STATUS", Float.parseFloat("0.5d"), Float.parseFloat("100d")).toString(), "\"STATUS\" BETWEEN [0.5, 100.0]");
            assertEquals(between("STATUS", Float.valueOf("0.5f"), Float.valueOf("100f")).toString(), "\"STATUS\" BETWEEN [0.5, 100.0]");
            assertEquals(between("STATUS", Float.valueOf("0.534343434f"), Float.valueOf("100f")).toString(), "\"STATUS\" BETWEEN [0.5343434, 100.0]");
            assertEquals(between("STATUS", Double.parseDouble("0.5"), Double.parseDouble("100")).toString(), "\"STATUS\" BETWEEN [0.5, 100.0]");
            assertEquals(between("STATUS", Double.valueOf("0.534343434f"), Double.valueOf("100f")).toString(), "\"STATUS\" BETWEEN [0.534343434, 100.0]");
            assertEquals(between("STATUS", Double.valueOf("0.534343434d"), Double.valueOf("100d")).toString(), "\"STATUS\" BETWEEN [0.534343434, 100.0]");
            assertEquals(between("STATUS", Double.valueOf("0.534343434748234792384d"), Double.valueOf("100d")).toString(), "\"STATUS\" BETWEEN [0.5343434347482348, 100.0]");
            assertEquals(between("STATUS", new BigDecimal("0.534343434748234792384"), new BigDecimal("100")).toString(), "\"STATUS\" BETWEEN [0.534343434748234792384, 100]");
            assertEquals(between("STATUS", new BigDecimal("0.534343434748234792384"), new BigDecimal("10056345345345.32423")).toString(), "\"STATUS\" BETWEEN [0.534343434748234792384, 10056345345345.32423]");
            logger.info("The tests finished for BETWEEN");
        } catch (ValueNotPermittedException e) {
            fail("fail operation for BETWEEN");
        }
    }

    @Test
    public void testExistsNotExistsEmptyNotEmptyIsNullIsNotNull() {
        assertEquals(exists("STATUS").toString(), "\"STATUS\" EXISTS");
        assertEquals(notExists("STATUS").toString(), "\"STATUS\" NOT_EXISTS");
        assertEquals(empty("STATUS").toString(), "\"STATUS\" EMPTY \"\"");
        assertEquals(notEmpty("STATUS").toString(), "\"STATUS\" NOT_EMPTY \"\"");
        assertEquals(isNull("STATUS").toString(), "\"STATUS\" IS_NULL");
        assertEquals(isNotNull("STATUS").toString(), "\"STATUS\" IS_NOT_NULL");
        logger.info("The tests finished for EXISTS, NOT_EXISTS, EMPTY, NOT_EMPTY, IS_NULL and IS_NOT NULL");
    }

    @Test
    public void testValueOfEnum() {
        assertEquals(OperationEnum.EQUALS, OperationEnum.valueOf("EQUALS"));
        assertEquals(OperationEnum.NOT_EQUALS, OperationEnum.valueOf("NOT_EQUALS"));
        assertEquals(OperationEnum.GREATER_THAN, OperationEnum.valueOf("GREATER_THAN"));
        assertEquals(OperationEnum.LESS_THAN, OperationEnum.valueOf("LESS_THAN"));
        assertEquals(OperationEnum.GREATER_THAN_OR_EQUALS, OperationEnum.valueOf("GREATER_THAN_OR_EQUALS"));
        assertEquals(OperationEnum.LESS_THAN_OR_EQUALS, OperationEnum.valueOf("LESS_THAN_OR_EQUALS"));
        assertEquals(OperationEnum.CONTAINS, OperationEnum.valueOf("CONTAINS"));
        assertEquals(OperationEnum.STARTS_WITH, OperationEnum.valueOf("STARTS_WITH"));
        assertEquals(OperationEnum.IN, OperationEnum.valueOf("IN"));
        assertEquals(OperationEnum.NOT_IN, OperationEnum.valueOf("NOT_IN"));
        assertEquals(OperationEnum.BETWEEN, OperationEnum.valueOf("BETWEEN"));
        assertEquals(OperationEnum.EMPTY, OperationEnum.valueOf("EMPTY"));
        assertEquals(OperationEnum.NOT_EMPTY, OperationEnum.valueOf("NOT_EMPTY"));
        assertEquals(OperationEnum.IS_NULL, OperationEnum.valueOf("IS_NULL"));
        assertEquals(OperationEnum.IS_NOT_NULL, OperationEnum.valueOf("IS_NOT_NULL"));
    }

    @SuppressWarnings("unchecked")
    @Test(expected = ValueNotPermittedException.class)
    public void testMethodList() throws Exception {
        Constructor constructor = Operation.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        Object newInstance = constructor.newInstance();
        Method methodList = OperationBase.class.getDeclaredMethod("getMethodList", Class.class);
//
        Map<String, Method> mapMethod = (Map<String, Method>) methodList.invoke(newInstance, Class1.class);
        assertTrue(mapMethod.isEmpty());
        Method methodCkeck = OperationBase.class.getDeclaredMethod("checkValuePermitted", String.class, Object.class, Map.class);
        assertTrue(Modifier.isProtected(methodCkeck.getModifiers()));
        methodCkeck.setAccessible(true);
        methodCkeck.invoke(newInstance, "testKey", 1, mapMethod);

        Class2.testA();
        Class2.testB();
    }

    public static class Class1 {

        private static void testA() {
        }

        @Override
        public boolean equals(Object obj) {
            return super.equals(obj);
        }
    }

    public static class Class2 extends OperationBase {

        private static final Map<String, Method> methodList = getMethodList(Class2.class);

        public static void testA() {
            checkValuePermitted("testKey", 2, methodList);
        }

        @ClassesAllowed(listClass = {String.class})
        public static void testB() {
            checkValuePermitted("testKey", 2, methodList);
        }

        @Override
        public boolean equals(Object obj) {
            return super.equals(obj);
        }
    }
}