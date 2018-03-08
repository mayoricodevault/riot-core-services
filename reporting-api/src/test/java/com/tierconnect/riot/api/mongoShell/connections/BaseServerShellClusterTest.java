package com.tierconnect.riot.api.mongoShell.connections;

import com.mongodb.connection.ClusterConnectionMode;
import com.mongodb.connection.ClusterType;
import com.tierconnect.riot.api.mongoShell.exception.NotImplementedException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * Created by achambi on 10/24/16.
 * Test database shell cluster
 */
public class BaseServerShellClusterTest {

    private BaseServerShellCluster baseServerShellCluster;

    @Before
    public void setUp() throws Exception {
        baseServerShellCluster = new BaseServerShellCluster(ClusterConnectionMode.SINGLE, ClusterType.STANDALONE,
                "databaseTest/riotMainTestQuery", true, true, true);
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void TestConstructorNull() {
        try {
            new BaseServerShellCluster(ClusterConnectionMode.SINGLE, ClusterType.STANDALONE, "test", true, false,
                    true);
            fail("expected: IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertEquals("java.lang.IllegalArgumentException: clusterType is not valid", ex.toString());
        }
        try {
            new BaseServerShellCluster(ClusterConnectionMode.SINGLE, ClusterType.STANDALONE, "test", false, true,
                    true);
            fail("expected: IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertEquals("java.lang.IllegalArgumentException: connectionMode is not valid", ex.toString());
        }
        try {
            new BaseServerShellCluster(null, null, null, false, false, false);
            fail("expected: IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertEquals("java.lang.IllegalArgumentException: connectionMode cannot be null", ex.toString());
        }


        try {
            new BaseServerShellCluster(null, null, null, true, true, true);
            fail("expected: IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertEquals("java.lang.IllegalArgumentException: connectionMode cannot be null", ex.toString());
        }

        try {
            new BaseServerShellCluster(null, ClusterType.STANDALONE, null, true, true, true);
            fail("expected: IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertEquals("java.lang.IllegalArgumentException: connectionMode cannot be null", ex.toString());
        }

        try {
            new BaseServerShellCluster(ClusterConnectionMode.SINGLE, null, "", true, true, true);
            fail("expected: IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertEquals("java.lang.IllegalArgumentException: clusterType cannot be null", ex.toString());
        }
        try {
            new BaseServerShellCluster(ClusterConnectionMode.SINGLE, ClusterType.STANDALONE, "hello word", true,
                    true, true);
            fail("expected: IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertEquals("java.lang.IllegalArgumentException: Invalid database name format. Database name is either " +
                    "empty or it " +
                    "contains spaces.", ex.toString());
        }

        try {
            new BaseServerShellCluster(ClusterConnectionMode.SINGLE, ClusterType.STANDALONE, "", true,
                    true, true);
            fail("expected: IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertEquals("java.lang.IllegalArgumentException: Invalid database name format. Database name is either " +
                    "empty or it " +
                    "contains spaces.", ex.toString());
        }

        try {
            new BaseServerShellCluster(ClusterConnectionMode.SINGLE, ClusterType.STANDALONE, "     ", true,
                    true, true);
            fail("expected: IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertEquals("java.lang.IllegalArgumentException: Invalid database name format. Database name is either " +
                    "empty or it " +
                    "contains spaces.", ex.toString());
        }
    }

    @Test(expected = NotImplementedException.class)
    public void testGetShellAddress() throws NotImplementedException {
        baseServerShellCluster.getShellAddress();
    }

    @Test
    public void testGetConnectionMode() {
        assertEquals(ClusterConnectionMode.SINGLE, baseServerShellCluster.getConnectionMode());
    }

    @Test
    public void testGetShellAddressCase2() {
        try {
            baseServerShellCluster.getShellAddress(null);
        } catch (NotImplementedException e) {
            assertThat(e, instanceOf(NotImplementedException.class));
        }
    }


    @Test
    public void testGetType() {
        assertEquals(ClusterType.STANDALONE, baseServerShellCluster.getClusterType());
    }

}