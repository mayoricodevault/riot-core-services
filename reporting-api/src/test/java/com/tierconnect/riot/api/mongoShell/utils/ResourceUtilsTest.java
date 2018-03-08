package com.tierconnect.riot.api.mongoShell.utils;

import com.tierconnect.riot.api.database.exception.OperationNotSupportedException;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;

import static org.junit.Assert.*;

/**
 * Created by achambi on 1/31/17.
 * Test Read Resource file.
 */
public class ResourceUtilsTest {

    @Test
    public void readFile() throws Exception {
        String expected = "TEST RESOURCE UTILS";
        String result = ResourceUtils.readFile("ResourceUtilsTest.txt");
        assertEquals(expected, result.trim());

        /*This method validate that the file is not locked*/
        URL url = ResourceUtilsTest.class.getClassLoader().getResource("ResourceUtilsTest.txt");
        if (url == null) {
            fail("could not be read the file!");
        }
        File file = new File(url.getPath());
        FileChannel channel = new RandomAccessFile(file, "rw").getChannel();
        FileLock lock = null;
        try {
            lock = channel.tryLock();
        } catch (OverlappingFileLockException e) {
            Assert.fail("file is locked");
        } finally {
            if (lock != null)
                lock.release();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void readFileFail() throws Exception {
        ResourceUtils.readFile("FILE_NOT_FOUND.txt");
    }

}