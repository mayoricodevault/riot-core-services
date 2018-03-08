package com.tierconnect.riot.appcore.utils.validator;

import com.mongodb.*;
import com.tierconnect.riot.commons.dao.mongo.MongoURIBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import javax.net.SocketFactory;
import java.io.IOException;
import java.net.*;
import java.nio.channels.IllegalBlockingModeException;
import java.util.ArrayList;
import java.util.List;

public class MongoDAOUtil {
    private static Logger logger = Logger.getLogger(MongoDAOUtil.class);

    private static MongoDAOUtil instance;

    private String mongoPrimary;
    private String mongoSecondary;
    private String mongoReplicaSet;
    private Boolean mongoSSL;
    private String mongoUsername;
    private String mongoPassword;
    private String mongoAuthDB;
    private String mongoDB;
    private String mongoControlReadPreference;
    private String mongoReportsReadPreference;
    private Boolean mongoSharding;
    private Integer mongoConnectTimeout;
    private Integer mongoMaxPoolSize;
    private String mongoURI;
    private String mongoURISecondary;
    private String mongoURIShell;

    //PRIMARY
    public MongoClient mongoClient;
    public DB db;
    public DBCollection things;
    public DBCollection thingSnapshots;
    public DBCollection thingSnapshotIds;
    public DBCollection reportLogs;

    //SECONDARY
    private MongoClient mongoClientReplica;
    private DB dbReplica;

    public MongoDAOUtil() {
    }

    public MongoDAOUtil(String mongoPrimary,
                        String mongoSecondary,
                        String mongoReplicaSet,
                        Boolean mongoSSL,
                        String mongoUsername,
                        String mongoPassword,
                        String mongoAuthDB,
                        String mongoDB,
                        String mongoControlReadPreference,
                        String mongoReportsReadPreference,
                        Boolean mongoShardding,
                        Integer mongoConnectTimeout,
                        Integer mongoMaxPoolSize) {
        this.mongoPrimary = mongoPrimary;
        this.mongoSecondary = mongoSecondary;
        this.mongoReplicaSet = mongoReplicaSet;
        this.mongoSSL = mongoSSL;
        this.mongoUsername = mongoUsername;
        this.mongoPassword = mongoPassword;
        this.mongoAuthDB = mongoAuthDB;
        this.mongoDB = mongoDB;
        this.mongoControlReadPreference = mongoControlReadPreference;
        this.mongoReportsReadPreference = mongoReportsReadPreference;
        this.mongoSharding = mongoShardding;
        this.mongoConnectTimeout = mongoConnectTimeout;
        this.mongoMaxPoolSize = mongoMaxPoolSize;
    }

    private void initDB() {

        this.mongoURI = MongoURIBuilder.buildMongoURI(this.mongoUsername, this.mongoPassword, this.mongoPrimary, this
                .mongoSecondary, this.mongoDB, this.mongoReplicaSet, this.mongoSSL, this.mongoConnectTimeout, this
                .mongoAuthDB, this.mongoMaxPoolSize, this.mongoControlReadPreference);
        this.mongoURISecondary = MongoURIBuilder.buildMongoURI(this.mongoUsername, this.mongoPassword, this
                .mongoPrimary, this
                .mongoSecondary, this.mongoDB, this.mongoReplicaSet, this.mongoSSL, this.mongoConnectTimeout, this
                .mongoAuthDB, this.mongoMaxPoolSize, this.mongoReportsReadPreference);
        this.mongoURIShell = MongoURIBuilder.buildMongoURIShell(this.mongoUsername, this.mongoPassword, this
                .mongoPrimary, this
                .mongoSecondary, this.mongoDB, this.mongoReplicaSet, this.mongoSSL, this.mongoConnectTimeout, this
                .mongoAuthDB, this.mongoMaxPoolSize, this.mongoReportsReadPreference);

        String checkAuthMongoURI = MongoURIBuilder.buildMongoURI(null, null, this.mongoPrimary, this
                .mongoSecondary, this.mongoDB, this.mongoReplicaSet, this.mongoSSL, this.mongoConnectTimeout, this
                .mongoAuthDB, 1, this.mongoControlReadPreference);

        //Throws SecurityException if mongo authentication is not enabled, otherwise it continues
        checkAuthenticationInServer(checkAuthMongoURI);


        MongoClientURI mongoClientURI = new MongoClientURI(mongoURI);
        mongoClient = new MongoClient(mongoClientURI);
        db = mongoClient.getDB(mongoDB);

            try{
                mongoClient.getAddress();
            }catch (Exception e){
                logger.error("Mongo authentication failed");
                throw new MongoSecurityException(MongoDAOUtil.getInstance().mongoClient.getCredentialsList().get(0), "Mongo authentication failed.", e);
            }finally {
                if(mongoClient!= null){
                    mongoClient.close();
                }
            }
    }


    /**
     * Set Up the replica connection.
     */
    private void initReplicaSet() {
        try {
            List<ServerAddress> replicaSet = testServers(mongoSecondary, mongoClient.getMongoOptions().socketFactory);
            if (replicaSet.isEmpty()) {
                mongoClientReplica = mongoClient;
                dbReplica = db;
            } else {
                MongoClientURI mongoClientURI = new MongoClientURI(mongoURISecondary);
                mongoClientReplica = new MongoClient(mongoClientURI);
                dbReplica = mongoClientReplica.getDB(mongoDB);
            }
        } catch (UnknownHostException ex) {
            logger.warn(ex.getMessage());
            mongoClientReplica = mongoClient;
            dbReplica = db;

        } catch (SecurityException ex) {
            logger.error("the security manager denies access.", ex);
            mongoClientReplica = mongoClient;
            dbReplica = db;

        } catch (MongoTimeoutException e) {
            if (e.getMessage().contains("com.mongodb.MongoSocketOpenException")) {
                logger.warn("Mongo replica connection failed, host and/or port are closed");
            } else if (e.getMessage().contains("com.mongodb.MongoSecurityException")) {
                logger.warn("Mongo authentication failed");
            }
            logger.info("Trying to connect to master database.");
            mongoClientReplica = mongoClient;
            dbReplica = db;
        }
    }

    private static List<ServerAddress> testServers(String mongoSecondary, SocketFactory socketFactory) throws
            UnknownHostException {
        if (StringUtils.isBlank(mongoSecondary)) {
            throw new UnknownHostException("Host list names for replica are empty.");
        }
        String[] mongoAddress = mongoSecondary.split(",");
        List<ServerAddress> replicaSet = new ArrayList<>();
        for (String host : mongoAddress) {
            try {
                String[] address = host.split(":");
                ServerAddress serverAddress = new ServerAddress(address[0], Integer.parseInt(address[1]));
                Socket socket = socketFactory.createSocket();
                socket.connect(serverAddress.getSocketAddress());
                socket.close();
                replicaSet.add(serverAddress);
            } catch (NumberFormatException e) {
                logger.error("Host port not is an Integer.", e);
            } catch (IndexOutOfBoundsException e) {
                logger.error("Host port is not defined.", e);
            } catch (NullPointerException e) {
                logger.error("Host port is blank.", e);
            } catch (MongoSocketException e) {
                logger.error("An error occurred verifying hostname replica is incorrect or does not exist.", e);
            } catch (IOException e) {
                logger.error("An error occurs during the connection to mongo replica.", e);
            } catch (IllegalBlockingModeException e) {
                logger.error("this mongo replica socket has an associated channel, and the channel is in " +
                        "non-blocking mode.", e);
            }
        }
        return replicaSet;
    }

    public static MongoDAOUtil getInstance() {
        return instance;
    }


    public static void setupMongodb(String mongoPrimary,
                                    String mongoSecondary,
                                    String mongoReplicaSet,
                                    Boolean mongoSSL,
                                    String mongoUsername,
                                    String mongoPassword,
                                    String mongoAuthDB,
                                    String mongoDB,
                                    String mongoControlReadPreference,
                                    String mongoReportsReadPreference,
                                    Boolean mongoShardding,
                                    Integer mongoConnectTimeout,
                                    Integer mongoMaxPoolSize
    ) throws UnknownHostException {

        instance = new MongoDAOUtil(mongoPrimary,
                mongoSecondary,
                mongoReplicaSet,
                mongoSSL,
                mongoUsername,
                mongoPassword,
                mongoAuthDB,
                mongoDB,
                mongoControlReadPreference,
                mongoReportsReadPreference,
                mongoShardding,
                mongoConnectTimeout,
                mongoMaxPoolSize);


        instance.initDB();
        instance.initReplicaSet();
    }

    private void checkAuthenticationInServer(String mongoURI) throws SecurityException{

        Boolean authenticated = false;
        MongoClient mongoClient = null;
        try{
            MongoClientURI mongoClientURI = new MongoClientURI(mongoURI);
            mongoClient = new MongoClient(mongoClientURI);
            mongoClient.listDatabases().first();
            authenticated = true;
            logger.info("Authentication NOT enabled in Mongo host");
        }catch (MongoCommandException e){
            logger.info("Authentication enabled in Mongo host");
        }finally{
            if(mongoClient != null){
                try{
                    mongoClient.close();
                }catch (Exception e){
                    //Nothing to do here
                }
            }
        }

        if(authenticated){
            throw  new SecurityException("Mongo Authentication not enabled in host");
        }
    }
}
