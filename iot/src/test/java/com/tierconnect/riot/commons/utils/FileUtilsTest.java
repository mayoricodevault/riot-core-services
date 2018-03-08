package com.tierconnect.riot.commons.utils;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


import static org.junit.Assert.*;

/**
 * Created by achambi on 10/6/16.
 * Unit test to FileUtils.
 */
public class FileUtilsTest {

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testSetFileContent() throws Exception {
        String expectedName = "testName";
        FileUtils fileUtils = new FileUtils(expectedName, "content test");
        assertEquals(fileUtils.getFileName().contains(expectedName), true);
        assertEquals(fileUtils.getFile().getName().contains(expectedName), true);
        assertNotEquals(fileUtils.getFile(), null);
    }

    @Test
    public void testSetFileContentNullName() throws Exception {
        String exceptionMessage = "Temporal file name or content is null.";
        try {
            new FileUtils(null, "content test");
            Assert.fail("Should have thrown an exception");
        } catch (NullPointerException e) {
            Assert.assertEquals(exceptionMessage, e.getMessage());
        }
        try {
            String expectedName = "testName";
            new FileUtils(expectedName, null);
            Assert.fail("Should have thrown an exception");
        } catch (NullPointerException e) {
            Assert.assertEquals(exceptionMessage, e.getMessage());
        }
        try {
            new FileUtils(null, null);
            Assert.fail("Should have thrown an exception");
        } catch (NullPointerException e) {
            Assert.assertEquals(exceptionMessage, e.getMessage());
        }
    }

    @Test
    public void testRemoveFile() throws Exception {
        FileUtils fileUtils = new FileUtils("testName", "content");
        assertEquals(true, fileUtils.removeFile());
        assertEquals(false, fileUtils.getFile().exists());
    }

    @Test
    public void testRemoveFileNullValue() throws Exception {

        String exceptionMessage = "File remove failed, File is null.";
        try {
            FileUtils fileUtils = new FileUtils("testName", "content");
            fileUtils.setFile(null);
            fileUtils.removeFile();
            Assert.fail("Should have thrown an exception");
        } catch (NullPointerException e) {
            Assert.assertEquals(exceptionMessage,e.getMessage());
        }
    }
}