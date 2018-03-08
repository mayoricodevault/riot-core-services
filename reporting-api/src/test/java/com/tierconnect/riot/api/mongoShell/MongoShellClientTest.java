package com.tierconnect.riot.api.mongoShell;

import com.mongodb.connection.ClusterConnectionMode;
import com.mongodb.connection.ClusterType;
import com.tierconnect.riot.api.mongoShell.connections.SingleServerShellServerCluster;

import com.tierconnect.riot.api.mongoShell.operations.OperationExecutor;
import com.tierconnect.riot.api.mongoShell.testUtils.PropertiesReaderUtil;

import org.junit.Before;
import org.junit.Test;


import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * Created by achambi on 11/24/16.
 * Test to verify the new functionality of mongo client.
 */
public class MongoShellClientTest {


    private MongoShellClient mongoShellClient;
    private static final String mongoDataBase = "riot_main_test";

    @Before
    public void setUp() throws Exception {
        PropertiesReaderUtil.setConfigurationFile("propertiesMongoShellLocalHost.properties");
        mongoShellClient = new MongoShellClient();
    }

    @Test
    public void MongoShellClientCase2() throws Exception {
        new MongoShellClient("admin", "control123!", "admin", "riot_main", "127.0.0.1", 0);
    }

    @Test
    public void getInstanceValidateSingleton() throws Exception {
        MongoShellClient mongoShellClientFirstInstance = MongoShellClient.getInstance();
        MongoShellClient mongoShellClientSecondInstance = MongoShellClient.getInstance();
        MongoShellClient mongoShellClientThirdInstance = MongoShellClient.getInstance();
        assertThat(mongoShellClientFirstInstance, is(mongoShellClientSecondInstance));
        assertThat(mongoShellClientSecondInstance, is(mongoShellClientThirdInstance));
    }

    @Test
    public void getInstance() throws Exception {
        assertNotNull(mongoShellClient);
        assertThat(mongoShellClient.getAuthDataBase(), is("admin"));
        assertThat(mongoShellClient.getOptions(), instanceOf(MongoShellClientOption.class));
        assertThat(mongoShellClient.getOptions().getConnectTimeout(), is(3000));
        assertThat(mongoShellClient.getOptions().getMaxPoolSize(), is(50));
        assertThat(mongoShellClient.getOptions().getSslEnabled(), is(false));
        assertThat(mongoShellClient.getOptions().getReadPreference(), is(ReadShellPreference.primary()));
        assertThat(mongoShellClient.getUserName(), is("admin"));
        assertThat(mongoShellClient.getPassword(), is("control123!"));
        assertThat(mongoShellClient.getServerShellCluster(), instanceOf(SingleServerShellServerCluster.class));
        assertThat(mongoShellClient.getServerShellCluster().getShellAddress(ServerStringFormat.SHELL), is("--host " +
                "127.0.0.1:27017 riot_main"));
        assertThat(mongoShellClient.getServerShellCluster().getShellAddress(ServerStringFormat.URI), is
                ("127.0.0.1:27017/riot_main"));
        assertThat(mongoShellClient.getServerShellCluster().getClusterType(), is(ClusterType.SHARDED));
        assertThat(mongoShellClient.getServerShellCluster().getConnectionMode(), is(ClusterConnectionMode.SINGLE));
        assertThat(mongoShellClient.getServerShellCluster().getShellAddress(), instanceOf(ServerShellAddress.class));
        assertThat(mongoShellClient.getServerShellCluster().getShellAddress().getServerShellAddressType(), is
                (ServerShellAddressType.MASTER));
    }

    @Test
    public void getDataBase() throws Exception {
        mongoShellClient = new MongoShellClient("admin", "control123!", "admin",
                mongoDataBase, "127.0.0.1", 27017);
        MongoDataBase mongoDataBase = mongoShellClient.getDataBase();
        OperationExecutor clientOperationExecutor = mongoShellClient.getExecutor();
        OperationExecutor dataBaseOperationExecutor = mongoDataBase.getExecutor();
        assertEquals(dataBaseOperationExecutor, clientOperationExecutor);
        assertEquals(dataBaseOperationExecutor.hashCode(), clientOperationExecutor.hashCode());
    }
}