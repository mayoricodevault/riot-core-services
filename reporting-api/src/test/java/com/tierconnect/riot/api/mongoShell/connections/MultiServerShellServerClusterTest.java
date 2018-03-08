package com.tierconnect.riot.api.mongoShell.connections;

import com.mongodb.connection.ClusterConnectionMode;
import com.mongodb.connection.ClusterType;
import com.tierconnect.riot.api.mongoShell.ServerShellAddress;
import com.tierconnect.riot.api.mongoShell.ServerShellAddressType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

/**
 * Created by achambi on 12/23/16.
 */
public class MultiServerShellServerClusterTest {
    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testShellAddress() throws Exception {
        List<ServerShellAddress> serverShellAddressList = new ArrayList<>();
        serverShellAddressList.add(new ServerShellAddress("Master0", 27017, ServerShellAddressType.MASTER));
        serverShellAddressList.add(new ServerShellAddress("Slave1", 27018, ServerShellAddressType.SECONDARY));
        serverShellAddressList.add(new ServerShellAddress("Slave2", 27019, ServerShellAddressType.SECONDARY));
        serverShellAddressList.add(new ServerShellAddress("Slave3", 27020, ServerShellAddressType.SECONDARY));
        serverShellAddressList.add(new ServerShellAddress("Slave4", 27021, ServerShellAddressType.SECONDARY));
        serverShellAddressList.add(new ServerShellAddress("Slave5", 27022, ServerShellAddressType.SECONDARY));
        serverShellAddressList.add(new ServerShellAddress("Slave6", 27023, ServerShellAddressType.SECONDARY));
        serverShellAddressList.add(new ServerShellAddress("Slave7", 27024, ServerShellAddressType.SECONDARY));
        serverShellAddressList.add(new ServerShellAddress("Slave8", 27025, ServerShellAddressType.SECONDARY));
        serverShellAddressList.add(new ServerShellAddress("Slave9", 27026, ServerShellAddressType.SECONDARY));
        MultiServerShellServerCluster result = new MultiServerShellServerCluster(ClusterConnectionMode.MULTIPLE,
                "databaseTest/riotMainTestQuery",
                ClusterType.REPLICA_SET, serverShellAddressList);
        assertNotEquals(null, result);
        Set<ServerShellAddress> resultList = result.getServerShellAddressSet();
        assertEquals(10, resultList.size());
        int index = 0;
        int portIndex = 27017;
        for (ServerShellAddress item :
                resultList) {
            if (index == 0 && portIndex == 27017) {
                assertEquals("master" + index, item.getHost());
                assertEquals(portIndex, item.getPort());
                assertEquals(ServerShellAddressType.MASTER, item.getServerShellAddressType());
            } else {
                assertEquals("slave" + index, item.getHost());
                assertEquals(portIndex, item.getPort());
                assertEquals(ServerShellAddressType.SECONDARY, item.getServerShellAddressType());
            }
            index++;
            portIndex++;
        }
    }

    @Test
    public void testShellAddressSecondaryRepeated() throws Exception {
        List<ServerShellAddress> serverShellAddressList = new ArrayList<>();
        serverShellAddressList.add(new ServerShellAddress("Master0", 27017, ServerShellAddressType.MASTER));
        serverShellAddressList.add(new ServerShellAddress("Slave1", 27018, ServerShellAddressType.SECONDARY));
        serverShellAddressList.add(new ServerShellAddress("Slave2", 27019, ServerShellAddressType.SECONDARY));
        serverShellAddressList.add(new ServerShellAddress("Slave3", 27020, ServerShellAddressType.SECONDARY));
        serverShellAddressList.add(new ServerShellAddress("Slave4", 27021, ServerShellAddressType.SECONDARY));
        serverShellAddressList.add(new ServerShellAddress("Slave1", 27018, ServerShellAddressType.SECONDARY));
        serverShellAddressList.add(new ServerShellAddress("Slave2", 27019, ServerShellAddressType.SECONDARY));
        serverShellAddressList.add(new ServerShellAddress("Slave3", 27020, ServerShellAddressType.SECONDARY));
        serverShellAddressList.add(new ServerShellAddress("Slave4", 27021, ServerShellAddressType.SECONDARY));
        MultiServerShellServerCluster result = new MultiServerShellServerCluster(ClusterConnectionMode.MULTIPLE,
                "databaseTest/riotMainTestQuery", ClusterType.REPLICA_SET, serverShellAddressList);
        assertNotEquals(null, result);
        Set<ServerShellAddress> resultList = result.getServerShellAddressSet();
        assertThat(resultList.size(), is(5));
        int index = 0;
        int portIndex = 27017;
        for (ServerShellAddress item :
                resultList) {
            if (index == 0 && portIndex == 27017) {
                assertThat(item.getHost(), is("master" + index));
                assertThat(item.getServerShellAddressType(), is(ServerShellAddressType.MASTER));
            } else {
                assertThat(item.getHost(), is("slave" + index));
                assertThat(item.getServerShellAddressType(), is(ServerShellAddressType.SECONDARY));
            }
            assertThat(item.getPort(), is(portIndex));
            index++;
            portIndex++;
        }
    }

    @Test
    public void testShellAddressSingleFailAndStandAloneFail() throws Exception {
        try {
            List<ServerShellAddress> serverShellAddressList = new ArrayList<>();
            serverShellAddressList.add(new ServerShellAddress("Master0", 27017, ServerShellAddressType.MASTER));
            serverShellAddressList.add(new ServerShellAddress("Slave1", 27018, ServerShellAddressType.SECONDARY));
            serverShellAddressList.add(new ServerShellAddress("Slave2", 27018, ServerShellAddressType.SECONDARY));
            new MultiServerShellServerCluster(ClusterConnectionMode.SINGLE, "test", ClusterType.STANDALONE,
                    serverShellAddressList);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertThat(ex.toString(), is("java.lang.IllegalArgumentException: connectionMode is not valid"));
        }

        try {
            List<ServerShellAddress> serverShellAddressList = new ArrayList<>();
            serverShellAddressList.add(new ServerShellAddress("Master0", 27017, ServerShellAddressType.MASTER));
            serverShellAddressList.add(new ServerShellAddress("Slave1", 27018, ServerShellAddressType.SECONDARY));
            serverShellAddressList.add(new ServerShellAddress("Slave2", 27018, ServerShellAddressType.SECONDARY));
            new MultiServerShellServerCluster(ClusterConnectionMode.SINGLE, "test", ClusterType.SHARDED,
                    serverShellAddressList);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertThat(ex.toString(), is("java.lang.IllegalArgumentException: connectionMode is not valid"));
        }
    }

    @Test
    public void testShellAddressMasterUnique() throws Exception {
        try {
            List<ServerShellAddress> serverShellAddressList = new ArrayList<>();
            serverShellAddressList.add(new ServerShellAddress("Master0", 27017, ServerShellAddressType.MASTER));
            serverShellAddressList.add(new ServerShellAddress("Slave1", 27018, ServerShellAddressType.MASTER));
            serverShellAddressList.add(new ServerShellAddress("Slave1", 27018, ServerShellAddressType.SECONDARY));
            new MultiServerShellServerCluster(ClusterConnectionMode.MULTIPLE, "databaseTest/riotMainTestQuery",
                    ClusterType.REPLICA_SET,
                    serverShellAddressList);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertThat(ex.toString(), is("java.lang.IllegalArgumentException: There must be just one master."));
        }
    }

    @Test
    public void testShellAddressMasterUniqueCase2() throws Exception {
        try {
            List<ServerShellAddress> serverShellAddressList = new ArrayList<>();
            serverShellAddressList.add(new ServerShellAddress("Master0", 27017, ServerShellAddressType.MASTER));
            serverShellAddressList.add(new ServerShellAddress("Slave1", 27018, ServerShellAddressType.MASTER));
            serverShellAddressList.add(new ServerShellAddress("Slave1", 27018, ServerShellAddressType.MASTER));
            new MultiServerShellServerCluster(ClusterConnectionMode.MULTIPLE, "databaseTest/riotMainTestQuery",
                    ClusterType.REPLICA_SET,
                    serverShellAddressList);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertThat(ex.toString(), is("java.lang.IllegalArgumentException: There must be just one master."));
        }
    }

    @Test
    public void testShellAddressMasterUniqueCase3() throws Exception {
        try {
            List<ServerShellAddress> multipleServerShellAddress = new ArrayList<>();
            multipleServerShellAddress.add(new ServerShellAddress("master", 27018, ServerShellAddressType.MASTER));
            multipleServerShellAddress.add(new ServerShellAddress("slave1", 27018, ServerShellAddressType.MASTER));
            multipleServerShellAddress.add(new ServerShellAddress("slave2", 27018, ServerShellAddressType.MASTER));
            multipleServerShellAddress.add(new ServerShellAddress("slave3", 27018, ServerShellAddressType.MASTER));
            multipleServerShellAddress.add(new ServerShellAddress("slave4", 27018, ServerShellAddressType.MASTER));
            multipleServerShellAddress.add(new ServerShellAddress("slave5", 27018, ServerShellAddressType.MASTER));
            multipleServerShellAddress.add(new ServerShellAddress("slave6", 27018, ServerShellAddressType.MASTER));
            new MultiServerShellServerCluster(ClusterConnectionMode.MULTIPLE, "databaseTest/riotMainTestQuery",
                    ClusterType.REPLICA_SET,
                    multipleServerShellAddress);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertThat(ex.toString(), is("java.lang.IllegalArgumentException: There must be just one master."));
        }
    }

    @Test
    public void testGetShellAddressFail() throws Exception {
        try {
            List<ServerShellAddress> serverShellAddressList = new ArrayList<>();
            serverShellAddressList.add(new ServerShellAddress("Master0", 27017, ServerShellAddressType.MASTER));
            serverShellAddressList.add(new ServerShellAddress("Slave1", 27018, ServerShellAddressType.SECONDARY));
            serverShellAddressList.add(new ServerShellAddress("Slave2", 27019, ServerShellAddressType.SECONDARY));
            BaseServerShellCluster result = new MultiServerShellServerCluster(ClusterConnectionMode.MULTIPLE,
                    "databaseTest/riotMainTestQuery",
                    ClusterType.REPLICA_SET,
                    serverShellAddressList);
            result.getShellAddress(null);

        } catch (UnsupportedOperationException ex) {
            assertThat(ex.getMessage(), is("Unsupported serverStringFormat mode: null"));
        }
    }

    @Test
    public void testGetShellAddressFail3() throws Exception {
        try {
            List<ServerShellAddress> serverShellAddressList = new ArrayList<>();
            serverShellAddressList.add(new ServerShellAddress("Master0", 27017, ServerShellAddressType.MASTER));
            serverShellAddressList.add(new ServerShellAddress("Slave1", 27017, ServerShellAddressType.MASTER));
            new MultiServerShellServerCluster(ClusterConnectionMode.MULTIPLE,
                    "databaseTest/riotMainTestQuery",
                    ClusterType.REPLICA_SET,
                    serverShellAddressList);


        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage(), is("There must be just one master."));
        }
    }

    @Test
    public void testGetShellAddressFail4() throws Exception {
        List<ServerShellAddress> serverShellAddressList = new ArrayList<>();
        serverShellAddressList.add(new ServerShellAddress("Master0", 27017, ServerShellAddressType.MASTER));
        serverShellAddressList.add(new ServerShellAddress("Master0", 27018, ServerShellAddressType.SECONDARY));
        BaseServerShellCluster result = new MultiServerShellServerCluster(ClusterConnectionMode.MULTIPLE,
                "databaseTest/riotMainTestQuery",
                ClusterType.REPLICA_SET,
                serverShellAddressList);
        assertThat(result.getConnectionMode(), is(ClusterConnectionMode.MULTIPLE));
        assertThat(result.getClusterType(), is(ClusterType.REPLICA_SET));
        assertThat(result.getDataBaseName(), is("databaseTest/riotMainTestQuery"));
    }

    @Test
    public void testGetShellAddressFail5() throws Exception {
        try {
            List<ServerShellAddress> serverShellAddressList = new ArrayList<>();
            serverShellAddressList.add(new ServerShellAddress("Master0", 27017, ServerShellAddressType.SECONDARY));
            serverShellAddressList.add(new ServerShellAddress("Master0", 27017, ServerShellAddressType.MASTER));
            new MultiServerShellServerCluster(ClusterConnectionMode.MULTIPLE,
                    "databaseTest/riotMainTestQuery",
                    ClusterType.REPLICA_SET,
                    serverShellAddressList);
        } catch (Exception ex) {
            assertThat(ex.getMessage(), is("There must be just one master."));
        }
    }

    @Test
    public void testGetShellAddressFail6() throws Exception {
        try {
            List<ServerShellAddress> serverShellAddressList = new ArrayList<>();
            serverShellAddressList.add(new ServerShellAddress("Master0", 27017, ServerShellAddressType.MASTER));
            serverShellAddressList.add(new ServerShellAddress("Master0", 27017, ServerShellAddressType.MASTER));
            new MultiServerShellServerCluster(ClusterConnectionMode.MULTIPLE,
                    "databaseTest/riotMainTestQuery",
                    ClusterType.REPLICA_SET,
                    serverShellAddressList);
        } catch (Exception ex) {
            assertThat(ex.getMessage(), is("There must be just one master."));
        }
    }
}