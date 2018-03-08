package com.tierconnect.riot.iot.kafkastreams.cacheloader;

import java.util.Properties;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 * CacheLoaderToolTest class. That is a integration test to verify the time to save in the topic and read in the KTable.
 *
 * @author jantezana
 * @version 2017/01/24
 */
public class CacheLoaderToolTest {

    private final static Logger LOGGER = Logger.getLogger(CacheLoaderToolTest.class);

    private static DataProvider dataProvider;
    private static final long RECORD_NUMBER = 100;
    private String zookeeper;
    private String kafkaServers;
    private CacheLoaderTool instance;


    public static final String APPLICATION_ID_CONFIG = "application.id";
    public static final String BOOTSTRAP_SERVERS_CONFIG = "bootstrap.servers";
    public static final String STATE_DIR_CONFIG = "state.dir";
    public static final String ZOOKEEPER_CONNECT_CONFIG = "zookeeper.connect";
    public static final String KEY_SERDE_CLASS_CONFIG = "key.serde";
    public static final String VALUE_SERDE_CLASS_CONFIG = "value.serde";
    public static final String NUM_STREAM_THREADS_CONFIG = "num.stream.threads";

    @BeforeClass
    public static void beforeClass()
            throws Exception {
        JdbcConfig jdbcConfig = new JdbcConfig();
        jdbcConfig.dialect = "org.hibernate.dialect.MySQLDialect";
        jdbcConfig.driverClassName = "com.mysql.jdbc.Driver";
        jdbcConfig.hazelcastNativeClientAddress = "172.18.0.61";
        jdbcConfig.jdbcUrl = "jdbc:mysql://172.18.0.10:3306/riot_main";
        jdbcConfig.userName = "root";
        jdbcConfig.password = "control123!";


        MongoConfig mongoConfig = new MongoConfig();
        mongoConfig.mongoPrimary = "172.18.0.20:27017";
        mongoConfig.username = "admin";
        mongoConfig.password = "control123!";
        mongoConfig.mongoAuthDB = "admin";
        mongoConfig.mongoDB = "riot_main";
        dataProvider = new DataProvider(jdbcConfig, mongoConfig);
    }

    @Before
    public void setUp()
            throws Exception {
        this.zookeeper = "localhost:2181";
        this.kafkaServers = "localhost:9092";
        String args = "-httpHost localhost -httpPort 8080 -contextPath /riot-core-services -apikey 7B4BCCDC";
        args += " -mongoCC MONGO -kafkaCC KAFKA -mysqlCC SQL";
        this.instance = new CacheLoaderTool();
        this.instance.init();
    }


    @After
    public void tearDown()
            throws Exception {
        this.kafkaServers = null;
        this.instance = null;
    }

    /**
     * Get kakfa configuration required by kafka streams. Includes:
     * - application id
     * - zookeeper ip
     * - kafka servers ips
     * - state store directory
     * - serdes
     * - number of threads
     *
     * @return properties object
     */
    private Properties buildStreamConfiguration() {
        Properties streamsConfiguration = new Properties();

        // Application unique name in the Kafka cluster
        streamsConfiguration.put(APPLICATION_ID_CONFIG, "kstreams-cacheloader-test");

        // Kafka broker(s).
        streamsConfiguration.put(BOOTSTRAP_SERVERS_CONFIG, kafkaServers);

        // Directory location for state stores
        streamsConfiguration.put(STATE_DIR_CONFIG, "kstreams-store-test");

        // ZooKeeper ensemble.
        streamsConfiguration.put(ZOOKEEPER_CONNECT_CONFIG, zookeeper);

        // Specify default (de)serializers for record keys and for record values.
        streamsConfiguration.put(KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        streamsConfiguration.put(VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

        // The number of threads to execute stream processing
        streamsConfiguration.put(NUM_STREAM_THREADS_CONFIG, "4");

        return streamsConfiguration;
    }
}