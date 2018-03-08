package com.tierconnect.riot.api.database.base.annotations;

import com.tierconnect.riot.api.database.base.Operation;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Method;

import static junitparams.JUnitParamsRunner.$;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by vealaro on 12/20/16.
 */
@RunWith(JUnitParamsRunner.class)
public class ClassesAllowedTest {

    @Test
    @Parameters(method = "parametersAnnotationClassesAllowed")
    public void testClassesAllowed(String nameMethod, int sizeClassesAllowed) throws NoSuchMethodException {
        assertNotNull(nameMethod);
        Method method = Operation.class.getDeclaredMethod(nameMethod, String.class, Object.class);
        assertTrue(method.isAnnotationPresent(ClassesAllowed.class));
        assertNotNull(method.getAnnotation(ClassesAllowed.class).listClass());
        assertEquals(sizeClassesAllowed, method.getAnnotation(ClassesAllowed.class).listClass().length);
    }

    public Object[] parametersAnnotationClassesAllowed(){
        return $(
                $("notEquals", 9),
                $("greaterThan", 7),
                $("lessThan", 7),
                $("greaterThanOrEquals", 7),
                $("lessThanOrEquals", 7),
                $("contains", 2),
                $("startsWith", 2),
                $("endsWith", 2)
        );
    }
}
