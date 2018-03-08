package com.tierconnect.riot.api.mongoShell.connections;

import com.tierconnect.riot.api.mongoShell.ServerShellAddress;
import com.tierconnect.riot.api.mongoShell.ServerShellAddressType;
import com.mongodb.connection.ClusterConnectionMode;
import com.mongodb.connection.ClusterType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


import static org.junit.Assert.*;

/**
 * Created by achambi on 10/24/16.
 * Class to implement a single Shell Address.
 */
public class SingleShellServerClusterFailTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testGetShellAddressAndValidations() throws Exception {
        try {
            new SingleServerShellServerCluster(
                    ClusterConnectionMode.MULTIPLE,
                    ClusterType.STANDALONE,
                    "databaseTest/riotMainTestQuery",
                    null);
            fail("Expect: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("java.lang.IllegalArgumentException: connectionMode is not valid", e.toString());
        }
    }

    @Test
    public void testGetShellAddressAndValidationsCase2() {

        try {
            new SingleServerShellServerCluster(ClusterConnectionMode.SINGLE, ClusterType.REPLICA_SET, "databaseTest" +
                    "/riotMainTestQuery", null);
            fail("Expect: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("java.lang.IllegalArgumentException: clusterType is not valid", e.toString());
        }


    }

    @Test
    public void testGetShellAddressAndValidationsCase3() {
        try {
            ServerShellAddress serverShellAddress = new ServerShellAddress("localhost", 27017, ServerShellAddressType
                    .SECONDARY);
            new SingleServerShellServerCluster(ClusterConnectionMode.SINGLE, ClusterType.STANDALONE, "databaseTest" +
                    "/riotMainTestQuery",
                    serverShellAddress);
            fail("Expect: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("java.lang.IllegalArgumentException: ServerShellAddress.ServerShellAddressType should be: " +
                    "MASTER", e.toString());
        }
    }
}