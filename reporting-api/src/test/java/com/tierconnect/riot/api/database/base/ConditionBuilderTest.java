package com.tierconnect.riot.api.database.base;

import com.tierconnect.riot.api.database.base.conditions.BooleanCondition;
import com.tierconnect.riot.api.database.base.conditions.ConditionBuilder;
import com.tierconnect.riot.api.database.base.operator.MultipleOperator;
import com.tierconnect.riot.api.database.exception.ValueNotPermittedException;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.tierconnect.riot.api.database.base.Operation.*;
import static org.junit.Assert.assertEquals;

/**
 * Project: reporting-api
 * Author: edwin
 * Date: 28/11/2016
 */
public class ConditionBuilderTest {

    @Test
    public void testSingleConditionBuilderAND() throws ValueNotPermittedException {
        ConditionBuilder builder = new ConditionBuilder();
        builder.addOperator(Operation.between("ID", 1, 100));
        assertEquals(BooleanCondition.AND, builder.getBooleanCondition());
        assertEquals(1, builder.getListGenericOperator().size());
        assertEquals("\"ID\" BETWEEN [1, 100]", builder.toString());
        builder.clear();
        assertEquals(builder.toString(), StringUtils.EMPTY);
    }

    @Test
    public void testSingleConditionBuilderOR() throws ValueNotPermittedException {
        ConditionBuilder builder = new ConditionBuilder(BooleanCondition.OR);
        builder.addOperator(contains("STATUS", "XY"));
        assertEquals(BooleanCondition.OR, builder.getBooleanCondition());
        assertEquals(1, builder.getListGenericOperator().size());
        assertEquals(Boolean.FALSE, builder.getListGenericOperator().get(0).isMultipleOperator());
        assertEquals("\"STATUS\" CONTAINS \"XY\"", builder.toString());
        builder.clear();
        assertEquals(builder.toString(), StringUtils.EMPTY);
    }

    @Test
    public void testMultipleConditionBuilder1() throws ValueNotPermittedException {
        MultipleOperator multipleOperatorOne = new MultipleOperator(BooleanCondition.AND);
        multipleOperatorOne.addOperator(startsWith("NAME", "ab"));
        multipleOperatorOne.addOperator(endsWith("NAME", "cd"));

        ConditionBuilder builder = new ConditionBuilder();
        builder.addOperator(multipleOperatorOne);
        assertEquals(BooleanCondition.AND, builder.getBooleanCondition());
        assertEquals(1, builder.getListGenericOperator().size());
        assertEquals(Boolean.TRUE, builder.getListGenericOperator().get(0).isMultipleOperator());
        assertEquals("(\"NAME\" STARTS_WITH \"ab\" AND \"NAME\" ENDS_WITH \"cd\")", builder.toString());
        builder.clear();
        assertEquals(builder.toString(), StringUtils.EMPTY);
    }

    @Test
    public void testMultipleConditionBuilder2() throws ValueNotPermittedException {
        MultipleOperator multipleOperatorOne = new MultipleOperator(BooleanCondition.OR);
        multipleOperatorOne.addOperator(startsWith("NAME", "ab"));
        multipleOperatorOne.addOperator(startsWith("NAME", "cd"));

        MultipleOperator multipleOperatorTwo = new MultipleOperator(BooleanCondition.OR);
        multipleOperatorTwo.addOperator(Operation.endsWith("LASTNAME", "uv"));
        multipleOperatorTwo.addOperator(Operation.endsWith("LASTNAME", "xy"));

        ConditionBuilder builder = new ConditionBuilder();
        builder.addOperator(multipleOperatorOne);
        builder.addOperator(multipleOperatorTwo);
        assertEquals(BooleanCondition.AND, builder.getBooleanCondition());
        assertEquals(2, builder.getListGenericOperator().size());
        assertEquals(Boolean.TRUE, builder.getListGenericOperator().get(0).isMultipleOperator());
        assertEquals(Boolean.TRUE, builder.getListGenericOperator().get(1).isMultipleOperator());
        assertEquals("(\"NAME\" STARTS_WITH \"ab\" OR \"NAME\" STARTS_WITH \"cd\") AND (\"LASTNAME\" ENDS_WITH \"uv\" OR \"LASTNAME\" ENDS_WITH \"xy\")", builder.toString());
        builder.clear();
        assertEquals(builder.toString(), StringUtils.EMPTY);
    }

    @Test
    public void testMultipleConditionBuilder3() throws ValueNotPermittedException {
        List<GenericOperator> listOperator = new ArrayList<>();
        MultipleOperator multipleOperatorOne = new MultipleOperator(BooleanCondition.OR);
        multipleOperatorOne.addOperator(startsWith("NAME", "ab"));
        multipleOperatorOne.addOperator(startsWith("NAME", "cd"));
        listOperator.add(multipleOperatorOne);

        MultipleOperator multipleOperatorTwo = new MultipleOperator(BooleanCondition.OR);
        multipleOperatorTwo.addOperator(Operation.endsWith("LASTNAME", "uv"));
        multipleOperatorTwo.addOperator(Operation.endsWith("LASTNAME", "xy"));
        listOperator.add(multipleOperatorTwo);

        ConditionBuilder builder = new ConditionBuilder();
        builder.addAllOperator(listOperator);

        assertEquals(BooleanCondition.AND, builder.getBooleanCondition());
        assertEquals(2, builder.getListGenericOperator().size());
        assertEquals(Boolean.TRUE, builder.getListGenericOperator().get(0).isMultipleOperator());
        assertEquals(Boolean.TRUE, builder.getListGenericOperator().get(1).isMultipleOperator());
        assertEquals("(\"NAME\" STARTS_WITH \"ab\" OR \"NAME\" STARTS_WITH \"cd\") AND (\"LASTNAME\" ENDS_WITH \"uv\" OR \"LASTNAME\" ENDS_WITH \"xy\")", builder.toString());
        builder.clear();
        assertEquals(builder.toString(), StringUtils.EMPTY);
    }

    @Test
    public void testMultipleConditionBuilder4() throws ValueNotPermittedException {
        MultipleOperator multipleOperatorOne = new MultipleOperator(BooleanCondition.OR, Arrays.asList(startsWith("NAME", "ab"), startsWith("NAME", "cd")));
        MultipleOperator multipleOperatorTwo = new MultipleOperator(BooleanCondition.OR, Arrays.asList(Operation.endsWith("LASTNAME", "uv"), Operation.endsWith("LASTNAME", "xy")));

        ConditionBuilder builder = new ConditionBuilder();
        builder.addMultiple(BooleanCondition.AND, multipleOperatorOne, multipleOperatorTwo);

        assertEquals(BooleanCondition.AND, builder.getBooleanCondition());
        assertEquals(1, builder.getListGenericOperator().size());
        assertEquals(Boolean.TRUE, builder.getListGenericOperator().get(0).isMultipleOperator());
        assertEquals("((\"NAME\" STARTS_WITH \"ab\" OR \"NAME\" STARTS_WITH \"cd\") AND (\"LASTNAME\" ENDS_WITH \"uv\" OR \"LASTNAME\" ENDS_WITH \"xy\"))", builder.toString());
        builder.clear();
        assertEquals(builder.toString(), StringUtils.EMPTY);
    }

    @Test
    public void testMultipleConditionBuilder5() throws ValueNotPermittedException {
        List<GenericOperator> listOperator = new ArrayList<>();

        MultipleOperator multipleOperatorOne = new MultipleOperator(BooleanCondition.OR);
        multipleOperatorOne.addOperator(startsWith("NAME", "ab"));
        multipleOperatorOne.addOperator(startsWith("NAME", "cd"));
        listOperator.add(multipleOperatorOne);

        MultipleOperator multipleOperatorTwo = new MultipleOperator(BooleanCondition.OR);
        multipleOperatorTwo.addOperator(Operation.endsWith("LASTNAME", "uv"));
        multipleOperatorTwo.addOperator(Operation.endsWith("LASTNAME", "xy"));
        listOperator.add(multipleOperatorTwo);

        ConditionBuilder builder = new ConditionBuilder();
        builder.addMultiple(BooleanCondition.AND, listOperator);

        assertEquals(BooleanCondition.AND, builder.getBooleanCondition());
        assertEquals(1, builder.getListGenericOperator().size());
        assertEquals(Boolean.TRUE, builder.getListGenericOperator().get(0).isMultipleOperator());
        assertEquals("((\"NAME\" STARTS_WITH \"ab\" OR \"NAME\" STARTS_WITH \"cd\") AND (\"LASTNAME\" ENDS_WITH \"uv\" OR \"LASTNAME\" ENDS_WITH \"xy\"))", builder.toString());
        builder.clear();
        assertEquals(builder.toString(), StringUtils.EMPTY);
    }

    @Test
    public void testMultipleConditionBuilder6() throws ValueNotPermittedException {
        ConditionBuilder builder = new ConditionBuilder(BooleanCondition.AND);

        MultipleOperator operatorIsNullOrZero = new MultipleOperator(BooleanCondition.OR, isNotNull("ID"), notEquals("ID", 0));
        MultipleOperator operatorEquasls = new MultipleOperator(BooleanCondition.OR, Operation.equals("YEAR", 2015), Operation.equals("YEAR", 2016));

        builder.addOperator(operatorIsNullOrZero);
        builder.addOperator(operatorEquasls);
        builder.addOperator(in("MONTH", Arrays.asList(1, 2, 3, 4, 5)));

        assertEquals(BooleanCondition.AND, builder.getBooleanCondition());
        assertEquals(3, builder.getListGenericOperator().size());
        assertEquals(Boolean.TRUE, builder.getListGenericOperator().get(0).isMultipleOperator());
        assertEquals(Boolean.TRUE, builder.getListGenericOperator().get(1).isMultipleOperator());
        assertEquals(Boolean.FALSE, builder.getListGenericOperator().get(2).isMultipleOperator());

        ConditionBuilder builderTwo = new ConditionBuilder(BooleanCondition.AND);
        builderTwo.addOperator(operatorIsNullOrZero).addOperator(operatorEquasls).addOperator(in("MONTH", Arrays.asList(1, 2, 3, 4, 5)));

        assertEquals(BooleanCondition.AND, builderTwo.getBooleanCondition());
        assertEquals(3, builderTwo.getListGenericOperator().size());
        assertEquals(Boolean.TRUE, builderTwo.getListGenericOperator().get(0).isMultipleOperator());
        assertEquals(Boolean.TRUE, builderTwo.getListGenericOperator().get(1).isMultipleOperator());
        assertEquals(Boolean.FALSE, builderTwo.getListGenericOperator().get(2).isMultipleOperator());

        assertEquals(builder.toString(), builderTwo.toString());

        ConditionBuilder builderThree = new ConditionBuilder(BooleanCondition.AND);
        builderThree.addMultiple(BooleanCondition.OR, isNotNull("ID"), notEquals("ID", 0));
        builderThree.addMultiple(BooleanCondition.OR, Operation.equals("YEAR", 2015), Operation.equals("YEAR", 2016));
        builderThree.addOperator(in("MONTH", Arrays.asList(1, 2, 3, 4, 5)));

        assertEquals(BooleanCondition.AND, builderThree.getBooleanCondition());
        assertEquals(3, builderThree.getListGenericOperator().size());
        assertEquals(Boolean.TRUE, builderThree.getListGenericOperator().get(0).isMultipleOperator());
        assertEquals(Boolean.TRUE, builderThree.getListGenericOperator().get(1).isMultipleOperator());
        assertEquals(Boolean.FALSE, builderThree.getListGenericOperator().get(2).isMultipleOperator());

        assertEquals(builder.toString(), builderTwo.toString());
        assertEquals(builder.toString(), builderThree.toString());
        assertEquals(builderTwo.toString(), builderThree.toString());
    }

    @Test
    public void testMultipleConditionBuilder7() throws ValueNotPermittedException {
        ConditionBuilder builder = new ConditionBuilder(BooleanCondition.AND);

        MultipleOperator operatorIsNullOrZero = new MultipleOperator(BooleanCondition.OR, Arrays.asList(isNotNull("ID"), notEquals("ID", 0)));
        MultipleOperator operatorEquasls = new MultipleOperator(BooleanCondition.OR, Arrays.asList(Operation.equals("YEAR", 2015), Operation.equals("YEAR", 2016)));

        builder.addOperator(operatorIsNullOrZero);
        builder.addOperator(operatorEquasls);
        builder.addOperator(in("MONTH", Arrays.asList(1, 2, 3, 4, 5)));

        assertEquals(BooleanCondition.AND, builder.getBooleanCondition());
        assertEquals(3, builder.getListGenericOperator().size());
        assertEquals(Boolean.TRUE, builder.getListGenericOperator().get(0).isMultipleOperator());
        assertEquals(Boolean.TRUE, builder.getListGenericOperator().get(1).isMultipleOperator());
        assertEquals(Boolean.FALSE, builder.getListGenericOperator().get(2).isMultipleOperator());

        ConditionBuilder builderTwo = new ConditionBuilder(BooleanCondition.AND);
        builderTwo.addOperator(operatorIsNullOrZero).addOperator(operatorEquasls).addOperator(in("MONTH", Arrays.asList(1, 2, 3, 4, 5)));

        assertEquals(BooleanCondition.AND, builderTwo.getBooleanCondition());
        assertEquals(3, builderTwo.getListGenericOperator().size());
        assertEquals(Boolean.TRUE, builderTwo.getListGenericOperator().get(0).isMultipleOperator());
        assertEquals(Boolean.TRUE, builderTwo.getListGenericOperator().get(1).isMultipleOperator());
        assertEquals(Boolean.FALSE, builderTwo.getListGenericOperator().get(2).isMultipleOperator());

        assertEquals(builder.toString(), builderTwo.toString());

        ConditionBuilder builderThree = new ConditionBuilder(BooleanCondition.AND);
        builderThree.addMultiple(BooleanCondition.OR, isNotNull("ID"), notEquals("ID", 0));
        builderThree.addMultiple(BooleanCondition.OR, Operation.equals("YEAR", 2015), Operation.equals("YEAR", 2016));
        builderThree.addOperator(in("MONTH", Arrays.asList(1, 2, 3, 4, 5)));

        assertEquals(BooleanCondition.AND, builderThree.getBooleanCondition());
        assertEquals(3, builderThree.getListGenericOperator().size());
        assertEquals(Boolean.TRUE, builderThree.getListGenericOperator().get(0).isMultipleOperator());
        assertEquals(Boolean.TRUE, builderThree.getListGenericOperator().get(1).isMultipleOperator());
        assertEquals(Boolean.FALSE, builderThree.getListGenericOperator().get(2).isMultipleOperator());

        assertEquals(builder.toString(), builderTwo.toString());
        assertEquals(builder.toString(), builderThree.toString());
        assertEquals(builderTwo.toString(), builderThree.toString());
    }

    @Test
    public void testBooleanCondition(){
        ConditionBuilder conditionBuilder = new ConditionBuilder();
        assertEquals(conditionBuilder.getBooleanCondition(),BooleanCondition.valueOf("AND"));
        conditionBuilder = new ConditionBuilder(BooleanCondition.OR);
        assertEquals(conditionBuilder.getBooleanCondition(),BooleanCondition.valueOf("OR"));
    }
}