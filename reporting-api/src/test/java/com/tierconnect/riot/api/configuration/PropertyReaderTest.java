package com.tierconnect.riot.api.configuration;

import com.tierconnect.riot.api.mongoShell.testUtils.PropertiesReaderUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * Created by achambi on 11/29/16.
 */
public class PropertyReaderTest {

    @Before
    public void setUp() throws Exception {
        PropertiesReaderUtil.setConfigurationFile("propertiesPropertyReaderTest.properties");
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void getProperty() throws Exception {
        assertThat(PropertyReader.getProperty("testNullProperty", "defaultValue",false), is("defaultValue"));
        assertThat(PropertyReader.getProperty("testNullProperty", "defaultValue",true), is("defaultValue"));
        assertThat(PropertyReader.getProperty("keyToTestBlank", "defaultValue",true), is("defaultValue"));
        assertThat(PropertyReader.getProperty("keyToTestBlank", "defaultValue",false), is(""));
        assertThat(PropertyReader.getProperty("keyToTestWithValue", "defaultValue",true), is("expectedValue"));
        assertThat(PropertyReader.getProperty("keyToTestWithValue", "defaultValue",false), is("expectedValue"));
    }

}