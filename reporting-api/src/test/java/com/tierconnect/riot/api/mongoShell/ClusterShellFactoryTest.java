package com.tierconnect.riot.api.mongoShell;

import com.tierconnect.riot.api.mongoShell.connections.BaseServerShellCluster;
import com.tierconnect.riot.api.mongoShell.connections.MultiServerShellServerCluster;
import com.tierconnect.riot.api.mongoShell.connections.SingleServerShellServerCluster;
import com.mongodb.connection.ClusterConnectionMode;
import com.mongodb.connection.ClusterType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import static org.hamcrest.CoreMatchers.instanceOf;

/**
 * Created by achambi on 10/26/16.
 * Unit Test to ClusterShellFactory
 */
public class ClusterShellFactoryTest {

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void createSingleServer() throws Exception {
        List<ServerShellAddress> singleServerShellAddress = new ArrayList<>();
        singleServerShellAddress.add(new ServerShellAddress("localhost", 27018, ServerShellAddressType.MASTER));
        BaseServerShellCluster result = ClusterShellFactory.create(
                ClusterConnectionMode.SINGLE,
                "",
                "databaseTest/riotMainTestQuery",
                ClusterType.STANDALONE,
                singleServerShellAddress);
        assertThat(result, instanceOf(SingleServerShellServerCluster.class));
    }

    @Test
    public void createSingleOnlyOneServerAddress() throws Exception {
        List<ServerShellAddress> serverShellAddressList = new ArrayList<>();
        ServerShellAddress expect = new ServerShellAddress("master", 27018, ServerShellAddressType.MASTER);
        serverShellAddressList.add(expect);
        serverShellAddressList.add(new ServerShellAddress("slave2", 27018, ServerShellAddressType.SECONDARY));
        serverShellAddressList.add(new ServerShellAddress("slave3", 27018, ServerShellAddressType.MASTER));
        serverShellAddressList.add(new ServerShellAddress("slave4", 27018, ServerShellAddressType.SECONDARY));
        serverShellAddressList.add(new ServerShellAddress("slave5", 27018, ServerShellAddressType.MASTER));
        serverShellAddressList.add(new ServerShellAddress("slave6", 27018, ServerShellAddressType.SECONDARY));
        serverShellAddressList.add(new ServerShellAddress("slave7", 27018, ServerShellAddressType.MASTER));
        try {
            ClusterShellFactory.create(ClusterConnectionMode.SINGLE, "", "", ClusterType.STANDALONE,
                    serverShellAddressList);
        } catch (IllegalStateException ex) {
            assertThat(ex.toString(), is("java.lang.IllegalStateException: state should be: one server in a direct " +
                    "cluster"));
        }
    }

    @Test
    public void createSingleOnlyOneServerAddressNull() throws Exception {

        try {
            ClusterShellFactory.create(
                    ClusterConnectionMode.SINGLE,
                    "",
                    "rio_main",
                    ClusterType.STANDALONE,
                    null);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertThat(ex.toString(), is("java.lang.IllegalArgumentException: serverShellAddressList can not be null"));
        }
    }

    @Test
    public void createMultiServer() throws Exception {
        List<ServerShellAddress> multipleServerShellAddress = new ArrayList<>();
        multipleServerShellAddress.add(new ServerShellAddress("master", 27018, ServerShellAddressType.MASTER));
        multipleServerShellAddress.add(new ServerShellAddress("slave1", 27018, ServerShellAddressType.SECONDARY));
        multipleServerShellAddress.add(new ServerShellAddress("slave1", 27018, ServerShellAddressType.SECONDARY));
        multipleServerShellAddress.add(new ServerShellAddress("slave1", 27018, ServerShellAddressType.SECONDARY));
        multipleServerShellAddress.add(new ServerShellAddress("slave1", 27018, ServerShellAddressType.SECONDARY));
        multipleServerShellAddress.add(new ServerShellAddress("slave1", 27018, ServerShellAddressType.SECONDARY));
        multipleServerShellAddress.add(new ServerShellAddress("slave1", 27018, ServerShellAddressType.SECONDARY));
        BaseServerShellCluster result = ClusterShellFactory.create(
                ClusterConnectionMode.MULTIPLE,
                "",
                "databaseTest/riotMainTestQuery",
                ClusterType.REPLICA_SET,
                multipleServerShellAddress);
        assertThat(result, instanceOf(MultiServerShellServerCluster.class));
        assertThat(((MultiServerShellServerCluster) result).getServerShellAddressSet().size(), is(2));
    }
}