package com.tierconnect.riot.api.database.sql;

import com.tierconnect.riot.api.database.base.GenericDataBase;
import com.tierconnect.riot.api.database.base.Operation;
import com.tierconnect.riot.api.database.base.conditions.ConditionBuilder;
import com.tierconnect.riot.api.database.exception.ValueNotPermittedException;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;

import static junitparams.JUnitParamsRunner.$;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by vealaro on 11/28/16.
 */
@RunWith(JUnitParamsRunner.class)
public class SQLTestTransform {

    @Test
    @Parameters(method = "parametersForSingleOperator")
    public void testTransformSingleOperator(ConditionBuilder builder, String whereQueryResult) throws Exception {
        assertNotNull(builder);
        assertNotNull(whereQueryResult);
//        GenericDataBase mySQL = new SQL(builder);
//        String transform = mySQL.getFilterString();
//        assertEquals(transform, whereQueryResult);
    }

    private Object[] parametersForSingleOperator() throws ValueNotPermittedException {
        return $(
                // String
                $(new ConditionBuilder().addOperator(Operation.equals("Name", "test")), "Name = 'test'"),
                // Number
                $(new ConditionBuilder().addOperator(Operation.equals("ID", 100)), "ID = 100"),
                $(new ConditionBuilder().addOperator(Operation.equals("ID", 100001L)), "ID = 100001"),
                $(new ConditionBuilder().addOperator(Operation.equals("Price", 2.5f)), "Price = 2.5"),
                $(new ConditionBuilder().addOperator(Operation.equals("Price", 2.54)), "Price = 2.54"),
                $(new ConditionBuilder().addOperator(Operation.equals("Price", 3.55d)), "Price = 3.55"),
                $(new ConditionBuilder().addOperator(Operation.equals("Price", Float.valueOf("2.5d"))), "Price = 2.5"),
                $(new ConditionBuilder().addOperator(Operation.equals("Price", Float.valueOf("2.5f"))), "Price = 2.5"),
                $(new ConditionBuilder().addOperator(Operation.equals("Price", Float.valueOf("2.5"))), "Price = 2.5"),
                $(new ConditionBuilder().addOperator(Operation.equals("Price", Double.valueOf("3.55d"))), "Price = 3.55"),
                $(new ConditionBuilder().addOperator(Operation.equals("Price", Double.valueOf("3.55f"))), "Price = 3.55"),
                $(new ConditionBuilder().addOperator(Operation.equals("Price", Double.valueOf("3.55"))), "Price = 3.55"),
                $(new ConditionBuilder().addOperator(Operation.equals("Price", BigDecimal.TEN)), "Price = 10"),
//                $(new ConditionBuilder().addOperator(Operation.equals("Price", BigDecimal.valueOf(3.55d))), "Price = 3.55"),
//                $(new ConditionBuilder().addOperator(Operation.equals("Price", BigDecimal.valueOf(3.55f))), "Price = 3.55"),
                $(new ConditionBuilder().addOperator(Operation.equals("Price", BigDecimal.valueOf(3.55))), "Price = 3.55"),
                // Boolean
                $(new ConditionBuilder().addOperator(Operation.equals("status", Boolean.FALSE)), "status = false"),
                $(new ConditionBuilder().addOperator(Operation.equals("status", true)), "status = true")
        );
    }


}