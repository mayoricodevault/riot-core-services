package com.tierconnect.riot.api.mongoShell.connections;

import com.tierconnect.riot.api.mongoShell.ServerShellAddress;
import com.tierconnect.riot.api.mongoShell.ServerShellAddressType;

import com.mongodb.connection.ClusterConnectionMode;
import com.mongodb.connection.ClusterType;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;


import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Created by achambi on 10/24/16.
 * Class to implement a single Shell Address.
 */
@RunWith(value = Parameterized.class)
public class SingleShellServerClusterTest {


    private ClusterConnectionMode clusterConnectionMode;
    private ClusterType clusterType;
    private ServerShellAddress serverShellAddress;


    @Parameterized.Parameters(name = "Single Server {index}: with clusterConnectionMode=\"{0}\", clusterType=\"{1}\"," +
            " serverShellAddress=\"{2}\"")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {
                        ClusterConnectionMode.SINGLE,
                        ClusterType.STANDALONE,
                        new ServerShellAddress("locahost", 27017, ServerShellAddressType.MASTER)
                },
                {
                        ClusterConnectionMode.SINGLE,
                        ClusterType.STANDALONE,
                        new ServerShellAddress("10.100.1.30", 12345, ServerShellAddressType.MASTER)
                },
                {
                        ClusterConnectionMode.SINGLE,
                        ClusterType.STANDALONE,
                        new ServerShellAddress("localhost", 27018, ServerShellAddressType.MASTER)
                },
                {
                        ClusterConnectionMode.SINGLE,
                        ClusterType.SHARDED,
                        new ServerShellAddress("localhost", 27018, ServerShellAddressType.MASTER)
                }
        });
    }

    public SingleShellServerClusterTest(ClusterConnectionMode clusterConnectionModeTest, ClusterType clusterTypeTest,
                                        ServerShellAddress serverShellAddressTest) {
        this.clusterConnectionMode = clusterConnectionModeTest;
        this.clusterType = clusterTypeTest;
        this.serverShellAddress = serverShellAddressTest;
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testSingleShellServerCluster() throws Exception {
        SingleServerShellServerCluster result = new SingleServerShellServerCluster
                (clusterConnectionMode, clusterType, "databaseTest/riotMainTestQuery", serverShellAddress);
        assertNotNull(null, result);
        assertEquals(result.getClusterType(), clusterType);
        assertEquals(result.getConnectionMode(), clusterConnectionMode);
        ServerShellAddress resultServerShellAddress = result.getShellAddress();
        assertNotNull(resultServerShellAddress);
        assertEquals(resultServerShellAddress.getServerShellAddressType(), this.serverShellAddress
                .getServerShellAddressType());
        assertEquals(resultServerShellAddress.getHost(), this.serverShellAddress.getHost());
        assertEquals(resultServerShellAddress.getPort(), this.serverShellAddress.getPort());
    }

    @Test
    public void testGetAndSetConnectionMode() throws Exception {
        SingleServerShellServerCluster singleShellServerCluster = new SingleServerShellServerCluster
                (clusterConnectionMode, clusterType, "databaseTest/riotMainTestQuery", serverShellAddress);

        assertNotEquals(singleShellServerCluster.getConnectionMode(), null);
        assertNotEquals(singleShellServerCluster.getClusterType(), null);
        Assert.assertNotEquals(singleShellServerCluster.getShellAddress(), null);

        assertEquals(singleShellServerCluster.getConnectionMode(), clusterConnectionMode);
        assertEquals(singleShellServerCluster.getClusterType(), clusterType);
        Assert.assertEquals(singleShellServerCluster.getShellAddress(), serverShellAddress);
    }
}