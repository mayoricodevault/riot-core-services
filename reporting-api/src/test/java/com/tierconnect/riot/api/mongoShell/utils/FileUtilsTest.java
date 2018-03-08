package com.tierconnect.riot.api.mongoShell.utils;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.Assert.*;

/**
 * Created by achambi on 10/19/16.
 * FileUtils JSON test
 */
public class FileUtilsTest {

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void loadInputStreamTestCase1() throws Exception {
        String jsonExpect = "{\"example\": \"jsonExample\"}";
        InputStream jsonStream = new ByteArrayInputStream(jsonExpect.getBytes());
        StringBuffer stringBufferResult = FileUtils.loadInputStream(jsonStream);
        assertEquals(jsonExpect, stringBufferResult.toString().trim());
    }

    @Test
    public void loadInputStreamTestCase2() throws Exception {
        String jsonExpect = "{\"example\": \"jsonExample\", \"exampleField2\": 1234}";
        InputStream jsonStream = new ByteArrayInputStream(jsonExpect.getBytes());
        StringBuffer stringBufferResult = FileUtils.loadInputStream(jsonStream);
        assertEquals(jsonExpect, stringBufferResult.toString().trim());
    }

    @Test
    @Ignore
    public void loadInputStreamTestCase3Null() throws Exception {
        try {
            FileUtils.loadInputStream(null);
            fail("expected exception:  NullPointerException");

        } catch (NullPointerException ex) {
            assertEquals("java.lang.NullPointerException: inputStream is Null.", ex.toString());
        }
    }
}