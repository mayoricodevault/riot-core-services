
package com.tierconnect.riot.api.mongoShell;

import com.mongodb.connection.ClusterConnectionMode;
import com.mongodb.connection.ClusterType;

import com.tierconnect.riot.api.mongoShell.connections.BaseServerShellCluster;
import com.tierconnect.riot.api.mongoShell.connections.MultiServerShellServerCluster;
import com.tierconnect.riot.api.mongoShell.connections.SingleServerShellServerCluster;
import com.tierconnect.riot.api.mongoShell.testUtils.PropertiesReaderUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

import static org.junit.Assert.*;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;

/**
 * Created by achambi on 10/26/16.
 * Unit Test to ClusterShellFactory
 */
@RunWith(value = Parameterized.class)
public class ClusterShellFactoryConfFileTest {

    private String propertyFileName;
    private ClusterConnectionMode clusterConnectionMode;
    private ClusterType clusterType;
    private Class baseServerShellCluster;
    private String serverAddressURIFormat;
    private String serverAddressSHELLFormat;

    @Parameterized.Parameters(name = "ClusterShellFactory {index}: with " +
            "properties file=\"{0}\", " +
            "ClusterConnectionMode=\"{1}\"," +
            "ClusterType=\"{2}\", " +
            "baseServerShellCluster=\"{3}\"," +
            "serverAddressURIFormat=\"{4}\"," +
            "serverAddressSHELLFormat=\"{5}\","
    )
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {
                        "propertiesTestCase1.properties",
                        ClusterConnectionMode.SINGLE,
                        ClusterType.SHARDED,
                        SingleServerShellServerCluster.class,
                        "10.100.1.140.1:27017/riot_main",
                        "--host 10.100.1.140.1:27017 riot_main"
                },
                {
                        "propertiesTestCase2.properties",
                        ClusterConnectionMode.SINGLE,
                        ClusterType.SHARDED,
                        SingleServerShellServerCluster.class,
                        "10.100.1.140.1:27017/riot_main",
                        "--host 10.100.1.140.1:27017 riot_main"
                },
                {
                        "propertiesTestCase3.properties",
                        ClusterConnectionMode.MULTIPLE,
                        ClusterType.REPLICA_SET,
                        MultiServerShellServerCluster.class,
                        "10.100.1.140.1:27017,10.100.1.141.1:27017,10.100.1.142.1:27017," +
                                "10.100.1.143.1:27018/riot_main",
                        "--host 10.100.1.140.1:27017,10.100.1.141.1:27017,10.100.1.142.1:27017," +
                                "10.100.1.143.1:27018 riot_main"
                },
                {
                        "propertiesTestCase4.properties",
                        ClusterConnectionMode.MULTIPLE,
                        ClusterType.REPLICA_SET,
                        MultiServerShellServerCluster.class,
                        "10.100.1.140.1:27017,10.100.1.141.1:27017,10.100.1.142.1:27017,10.100.1.143.1:27018/riot_main",
                        "--host 10.100.1.140.1:27017,10.100.1.141.1:27017,10.100.1.142.1:27017,10.100.1.143.1:27018 riot_main"
                },
                {
                        "propertiesTestCase5.properties",
                        ClusterConnectionMode.SINGLE,
                        ClusterType.SHARDED,
                        SingleServerShellServerCluster.class,
                        "10.100.1.140.1:27017/riot_main",
                        "--host 10.100.1.140.1:27017 riot_main"
                },
                {
                        "propertiesTestCase6.properties",
                        ClusterConnectionMode.SINGLE,
                        ClusterType.SHARDED,
                        SingleServerShellServerCluster.class,
                        "10.100.1.140.1:27017/riot_main",
                        "--host 10.100.1.140.1:27017 riot_main"
                },
                {
                        "propertiesTestCase7.properties",
                        ClusterConnectionMode.MULTIPLE,
                        ClusterType.REPLICA_SET,
                        MultiServerShellServerCluster.class,
                        "10.100.1.140.1:27017/riot_main",
                        "--host 10.100.1.140.1:27017 riot_main"
                },
                {
                        "propertiesTestCase8.properties",
                        ClusterConnectionMode.SINGLE,
                        ClusterType.STANDALONE,
                        SingleServerShellServerCluster.class,
                        "10.100.1.140.1:27017/riot_main",
                        "--host 10.100.1.140.1:27017 riot_main"
                },
                {
                        "propertiesTestCase9-16.properties",
                        ClusterConnectionMode.MULTIPLE,
                        ClusterType.STANDALONE,
                        SingleServerShellServerCluster.class,
                        "FAIL",
                        "Exception"
                }
        });
    }

    /**
     * Constructor to get property files to test the server address.
     */
    public ClusterShellFactoryConfFileTest(String propertyFileName,
                                           ClusterConnectionMode clusterConnectionMode,
                                           ClusterType clusterType,
                                           Class baseServerShellCluster,
                                           String shellAddressURIFormat,
                                           String serverAddressSHELLFormat) {
        this.propertyFileName = propertyFileName;
        this.clusterConnectionMode = clusterConnectionMode;
        this.clusterType = clusterType;
        this.baseServerShellCluster = baseServerShellCluster;
        this.serverAddressURIFormat = shellAddressURIFormat;
        this.serverAddressSHELLFormat = serverAddressSHELLFormat;
    }

    @Before
    public void setUp() throws Exception {
        PropertiesReaderUtil.setConfigurationFile(this.propertyFileName);
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void createSingleServer() throws Exception {
        if (this.serverAddressURIFormat.equals("FAIL")) {
            try {
                ClusterShellFactory.create();
            } catch (java.lang.IllegalArgumentException ex) {
                assertThat(ex, instanceOf(java.lang.IllegalArgumentException.class));
                assertEquals(ex.toString(), "java.lang.IllegalArgumentException: mongoPrimary should be: a valid " +
                        "address");
            }
        } else {
            BaseServerShellCluster result = ClusterShellFactory.create();
            assertThat(result, instanceOf(this.baseServerShellCluster));
            assertThat(result.getConnectionMode(), is(this.clusterConnectionMode));
            assertThat(result.getClusterType(), is(this.clusterType));
            assertThat(result.getShellAddress(ServerStringFormat.URI), is(this.serverAddressURIFormat));
            assertThat(result.getShellAddress(ServerStringFormat.SHELL), is(this.serverAddressSHELLFormat));
        }
    }
}
