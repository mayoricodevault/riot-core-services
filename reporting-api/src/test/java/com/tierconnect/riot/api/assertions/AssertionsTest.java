package com.tierconnect.riot.api.assertions;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static com.tierconnect.riot.api.assertions.Assertions.*;
import static junitparams.JUnitParamsRunner.$;

/**
 * Created by achambi on 10/24/16.
 */
@RunWith(JUnitParamsRunner.class)
public class AssertionsTest {

    @Test(expected = IllegalArgumentException.class)
    public void testIsTrueArgumentTrue() throws Exception {
        isTrueArgument("FIELD", "EXTECTED", false);
    }

    @Test
    public void testIsTrueArgumentFalse() throws Exception {
        isTrueArgument("TEST", "FALSE", true);
    }

    @Test
    public void testIsBlank() {
        org.junit.Assert.assertTrue(isBlank(""));
        org.junit.Assert.assertTrue(isBlank("  "));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValueNull() {
        notNull("TEST", null);
    }

    @Test
    public void testNameNull() {
        notNull(null, "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNameAndValueNull() {
        notNull(null, null);
    }

    @Test
    public void testIsTrueArgument() {
        isTrueArgument("projection", "{}", "\\{(.*?)\\}");
        isTrueArgument("projection", "{\"key\":\"value\"}", "\\{(.*?)\\}");
    }

    @Test(expected = IllegalArgumentException.class)
    @Parameters(method = "parameterError")
    public void testIsTrueArgumentError(String value, String regExp) {
        isTrueArgument("projection", value, regExp);
    }

    public Object[] parameterError() {
        return $(
                $("{", "\\{(.*?)\\}"),
                $("\"key\":\"value\"}", "\\{(.*?)\\}")
        );
    }
}