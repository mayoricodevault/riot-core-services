package com.tierconnect.riot.commons;

/**
 * Created by root on 14-10-16.
 */
public class ConnectionConstants {

        public static final String CONNECTION_CODE = "connectionCode";

        public static final String MQTT = "mqtt";
        public static final String MQTT_HOST = "host";
        public static final String MQTT_PORT = "port";
        public static final String MQTT_QOS = "qos";
        public static final String MQTT_SECURE = "secure";

        public static final String KAFKA = "kafka";
        public static final String KAFKA_ZOOKEEPER = "zookeeper";
        public static final String KAFKA_SERVER = "server";
        public static final String KAFKA_TOPICS = "topics";
        public static final String KAFKA_ACTIVE = "active";

        public static final String SERVICES = "rest";
        public static final String SERVICES_HOST = "host";
        public static final String SERVICES_PORT = "port";
        public static final String SERVICES_CONTEXT_PATH = "contextpath";
        public static final String SERVICES_APIKEY = "apikey";
        public static final String SERVICES_SECURE = "secure";

        public static final String MONGO = "mongo";
        public static final String MONGO_PRIMARY = "mongoPrimary";
        public static final String MONGO_SECONDARY = "mongoSecondary";
        public static final String MONGO_DB = "mongoDB";
        public static final String MONGO_USERNAME = "username";
        public static final String MONGO_PASSWORD = "password";
        public static final String MONGO_SSL = "mongoSSL";
        public static final String MONGO_SHARDING = "mongoSharding";
        public static final String MONGO_REPLICASET = "mongoReplicaSet";
        public static final String MONGO_AUTHDB = "mongoAuthDB";
        public static final String MONGO_CONNTIMEOUT = "mongoConnectTimeout";
        public static final String MONGO_MAXPOOLSIZE = "mongoMaxPoolSize";

        public static final String FTP = "ftp";
        public static final String FTP_HOST = "host";
        public static final String FTP_PORT = "port";
        public static final String FTP_USERNAME = "username";
        public static final String FTP_PASSWORD = "password";
        public static final String FTP_SECURE = "secure";

        public static final String HADOOP = "hadoop";
        public static final String HADOOP_HOST = "host";
        public static final String HADOOP_PORT = "port";
        public static final String HADOOP_PATH = "path";
        public static final String HADOOP_SECURE = "secure";

        public static final String SQL = "sql";
        public static final String SQL_DRIVER = "driver";
        public static final String SQL_DIALECT = "dialect";
        public static final String SQL_USERNAME = "username";
        public static final String SQL_PASSWORD = "password";
        public static final String SQL_URL = "url";
        public static final String SQL_HZ = "hazelcastNativeClientAddress";

        public static final String SPARK = "spark";
        public static final String SPARK_MASTER_HOST = "masterHost";
        public static final String SPARK_PORT = "port";

        public static final String BRIDGE_STARTUP_OPTIONS = "bridgeStartupOptions";
        public static final String SCHEDULED_RULE_SERVICES_CONNECTION_CODE = "servicesConnectionCode";
        // TODO: Disable restart app in spark and remove extra configuration.
        // public static final String SPARK_APPLICATION_ID = "applicationId";
        // public static final String SPARK_DRIVER_HOST = "driverHost";
        // public static final String SPARK_EXECUTOR_MEMORY = "executorMemory";
        // public static final String SPARK_TOTAL_EXECUTOR_CORES = "totalExecutorCores";
        // public static final String SPARK_EXECUTOR_CORES = "executorCores";
        // public static final String SPARK_SCHEDULER_MODE = "schedulerMode";
        // public static final String SPARK_NUM_EXECUTORS = "numExecutors";
        // public static final String SPARK_BATCH_INTERVAL = "batchInterval";
        // public static final String SPARK_WRITE_TO_MONGO = "writeToMongo";
        // public static final String SPARK_CONSUMER_POLL_MS = "consumerPollMs";
        // public static final String SPARK_WORKERS = "workers";

}
