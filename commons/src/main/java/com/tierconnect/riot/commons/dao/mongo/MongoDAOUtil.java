package com.tierconnect.riot.commons.dao.mongo;

import com.mongodb.*;
import com.mongodb.client.MongoCursor;
import com.mongodb.util.JSON;
import com.tierconnect.riot.commons.utils.ShellCommandUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.bson.Document;

import javax.net.SocketFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.nio.channels.IllegalBlockingModeException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MongoDAOUtil {
    private static Logger logger = Logger.getLogger(MongoDAOUtil.class);


    private static final boolean isWindows = Os.isFamily(Os.FAMILY_WINDOWS);

    private static MongoDAOUtil instance;
    public static final String CSV_SEPARATOR = "\\|,\\|";

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
    public DBCollection thingTypesCollection;
    public DBCollection thingSnapshots;
    public DBCollection thingSnapshotIds;
    public DBCollection sapCollection;
    public DBCollection reportLogs;

    //SECONDARY
    private MongoClient mongoClientReplica;
    private DB dbReplica;
    public DBCollection thingsReplica;
    public DBCollection thingSnapshotsReplica;
    public DBCollection thingSnapshotIdsReplica;

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

//        Logger mongoLogger = Logger.getLogger("org.mongodb.driver");
//        mongoLogger.setLevel(Level.OFF);


        MongoClientURI mongoClientURI = new MongoClientURI(mongoURI);
        mongoClient = new MongoClient(mongoClientURI);
        db = mongoClient.getDB(mongoDB);

        boolean retry = true;


        String checkAuthMongoURI = MongoURIBuilder.buildMongoURI(null, null, this.mongoPrimary, this
                .mongoSecondary, this.mongoDB, this.mongoReplicaSet, this.mongoSSL, this.mongoConnectTimeout, this
                .mongoAuthDB, this.mongoMaxPoolSize, this.mongoControlReadPreference);

        //Throws SecurityException if mongo authentication is not enabled, otherwise it continues
        checkAuthenticationInServer(checkAuthMongoURI);

        do{
            try{
                MongoDAOUtil.getInstance().mongoClient.getAddress();
                retry = false;
            }catch (Exception e){
                if(e.getMessage().contains("com.mongodb.MongoSecurityException")) {
                    logger.error("Mongo authentication failed");
                    throw new MongoSecurityException(MongoDAOUtil.getInstance().mongoClient.getCredentialsList().get(0), "Mongo authentication failed.", e);
                } else {
                    //Catches MongoSocketException and MongoSocketOpenException for standalone and container mongo dependencies
                    logger.warn("Startup process for [Services] waiting for [MongoDB], retry in " + (mongoConnectTimeout == 0 ? "30" : mongoConnectTimeout) + "s");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }while(retry);

        things = db.getCollection("things");
        thingTypesCollection = db.getCollection("thingTypes");
        thingSnapshots = db.getCollection("thingSnapshots");
        thingSnapshotIds = db.getCollection("thingSnapshotIds");
        sapCollection = db.getCollection("sap");
        reportLogs = db.getCollection("reportLogs");
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
        } finally {
            thingsReplica = dbReplica.getCollection("things");
            thingSnapshotsReplica = dbReplica.getCollection("thingSnapshots");
            thingSnapshotIdsReplica = dbReplica.getCollection("thingSnapshotIds");
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
                //defaultMasterClient.getAddress().
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
        instance.initIndexes();

        logger.info("******************* CONNECTING ");
        logger.info("things='" + instance.things + "'");
        logger.info("thingTypesCollection='" + instance.thingTypesCollection + "'");
        logger.info("thingSnapshots='" + instance.thingSnapshots + "'");
        logger.info("sapCollection='" + instance.sapCollection + "'");

        /*
        if (StringUtils.isEmpty(readUsername)) {
            readUsername = username;
            readPassword = password;
        }

        MongoCredential credentialRead = MongoCredential.createCredential(readUsername, "admin", readPassword
        .toCharArray());
        mongoClient =
                new MongoClient(
                        new ServerAddress(mongoHost, mongoPort),
                        Arrays.asList(credentialRead),
                        options);

        if (readInstance.mongoClient != null) {
            readInstance.mongoClient.close();
        }
        readInstance.mongoClient = mongoClient;
        db = mongoClient.getDB(mongoDatabase);
        readInstance.db = db;

        readInstance.things = db.getCollection("things");
        readInstance.thingTypesCollection = db.getCollection("thingTypes");
        readInstance.thingSnapshots = db.getCollection("thingSnapshots");
        readInstance.thingSnapshotIds = db.getCollection("thingSnapshotIds");
        readInstance.thingBucketCollection = db.getCollection("thingBucket");
        readInstance.timeseriesCollection = db.getCollection("timeseries");
        readInstance.timeseriesControlCollection = db.getCollection("timeseriesControl");

        logger.info("******************* CONNECTING ");
        logger.info("things='" + readInstance.things + "'");
        logger.info("thingTypesCollection='" + readInstance.thingTypesCollection + "'");
        logger.info("thingSnapshots='" + readInstance.thingSnapshots + "'");

*/
//		MongoDAOUtil.setEnabled( true );

    }
//	public static boolean isEnabled()
//	{
//		return enabled;
//	}
//
//	public static void setEnabled( boolean b )
//	{
//		enabled = b;

//	}

    public static void checkIndexes() {
        logger.info("Checking indexes in Mongo.");
        try {
            URL resource = MongoDAOUtil.class.getClassLoader().getResource("mongoIndexes.json");
            if (resource != null) {
                String path = URLDecoder.decode(resource.getPath(), "utf-8");
                File file = new File(path);
                FileInputStream fis = new FileInputStream(file);
                byte[] data = new byte[(int) file.length()];
                int readResult = fis.read(data);
                fis.close();
                if (readResult != -1) {
                    String input = new String(data, "UTF-8");
                    DBObject json = (BasicDBObject) JSON.parse(input);
                    for (String collectionName : json.keySet()) {

                        DBCollection indexCollection = instance.db.getCollection(collectionName);

                        List<DBObject> currentIndexes = indexCollection.getIndexInfo();

                        WriteConcern wr = indexCollection.getWriteConcern();
                        indexCollection.setWriteConcern(WriteConcern.UNACKNOWLEDGED);

                        DBObject indexes = (BasicDBObject) json.get(collectionName);

                        for (String indexName : indexes.keySet()) {

                            DBObject key = (BasicDBObject) ((BasicDBObject) indexes.get(indexName)).get("key");
                            BasicDBObject options = (BasicDBObject) ((BasicDBObject) indexes.get(indexName)).get
                                    ("options");

                            if (!options.containsField("name")) {
                                options.append("name", indexName);
                            } else {
                                indexName = (String) options.get("name");
                            }
                            if (!options.containsField("background")) {
                                options.append("background", true);
                            }

                            DBObject existingIndex = null;
                            for(DBObject object : currentIndexes){
                                if(object.get("name").equals(indexName)){
                                    existingIndex = object;
                                }
                            }

                            if(existingIndex != null){
                                logger.warn("Skipping createIndex with name '" + indexName + "' and key " + key.toString() +
                                        ", it already exists with key " + existingIndex.get("key").toString());
                            }else{
                                try {
                                    indexCollection.createIndex(key, options);
                                    logger.info("Index " + indexName + " in " + collectionName + " successfully created.");
                                } catch (Exception e) {
                                    logger.warn("Cannot create index " + indexName + " in " + collectionName + ". Cause:" + e.getCause());
                                }
                            }

                        }
                        indexCollection.setWriteConcern(wr);
                    }
                } else {
                    logger.warn("Cannot read Mongo Indexes file.");
                }
            } else {
                logger.warn("Cannot find Mongo Indexes definition file.");
            }
        } catch (FileNotFoundException e) {
            logger.warn("Cannot find Mongo Indexes definition file.");
        } catch (IOException e) {
            logger.warn("Cannot read data from Mongo Indexes definition file.");
        }
//        catch (ParseException e) {
//            logger.warn("Cannot parse Mongo Indexes definition file.");
//        }
    }

    /**
     * Run a Mongo script in the shell terminal.
     *
     * @param command command to run in shell.
     * @return response object.
     * @throws Exception an exception to contains the error message.
     */
    public Object runCommand(String command) throws Exception {
        if (!isWindows) {
            command = "'" + command + "'";
        }
        String mongoCommand = String.format("echo %1$s | %2$s <GREP> ", command, this.mongoURIShell);
        return JSON.parse(ShellCommandUtil.executeCommand(mongoCommand).toString());
    }

    /**
     * Run a Mongo script in the shell terminal.
     *
     * @param command        The command to run in shell.
     * @param fileResultName The file name to contains the result.
     * @return File to contains the result.
     * @throws Exception The exception to contains the error message.
     */
    public File runCommandFileResult(String command, String fileResultName) throws Exception {

        String fileCommandName = fileResultName;
        if (!isWindows) {
            command = "'" + command + "'";
        } else {
            if (fileResultName.startsWith("\\") || fileResultName.startsWith("/")) {
                fileResultName = fileResultName.substring(1);
            }
            fileCommandName = fileResultName;
        }
        try {
            String mongoCommand = String.format("echo %1$s | %2$s <GREP> >\"%3$s\"",
                    command,
                    this.mongoURIShell,
                    fileCommandName);
            ShellCommandUtil.executeCommand(mongoCommand);
        } catch (Exception e) {
            logger.error("An error ocurred while executing command.");
            throw new Exception("An error ocurred while executing command.", e);
        }
        return new File(fileResultName);
    }

    public BasicDBObject runCommandBasicDBObjectResult(String code) throws Exception {
        String fileResultName = code + "x";
        String fileCommandName = fileResultName;
        if (!isWindows) {
            code = "'" + code + "'";
        } else {
            if (fileResultName.startsWith("\\") || fileResultName.startsWith("/")) {
                fileResultName = fileResultName.substring(1);
            }
            fileCommandName = fileResultName;
        }
        String mongoCommand = String.format("%1$s %2$s <GREP> >\"%3$s\"",
                mongoURIShell,
                code,
                fileCommandName);
        ShellCommandUtil.executeCommand(mongoCommand);
        File tmpOutput = new File(fileResultName);
        FileInputStream fis = new FileInputStream(tmpOutput);
        byte[] data = new byte[(int) tmpOutput.length()];
        int result = fis.read(data);
        fis.close();
        //noinspection ResultOfMethodCallIgnored
        tmpOutput.delete();
        if (result != -1) {
            String str = new String(data, "UTF-8");
            return (BasicDBObject) JSON.parse(str);
        } else {
            logger.warn("The script result is empty or not exists result to view.");
            return null;
        }
    }

    /**
     * create a command to run in mongo shell.
     *
     * @param mongoMethodName the mongo method name to run.
     * @param options         method optional parameters.
     * @return return a string to contains the command to run.
     * @throws Exception an exception to contains the error message.
     */
    public static String createCommand(String mongoMethodName, String options, boolean replicaMode, String path, String readPreference)
            throws Exception {

        File template = new File(path + "reportsTemplate.js");
        FileInputStream fis = new FileInputStream(template);
        byte[] data = new byte[(int) template.length()];
        int result = fis.read(data);
        fis.close();
        if (result != -1) {
            String str = new String(data, "UTF-8");
            return str
                    .replaceAll("<REPLICA_MODE>", (replicaMode ? "db.getMongo().setReadPref(\"" + readPreference + "\");" :
                            ""))
                    .replaceAll("<METHOD_NAME>", mongoMethodName)
                    .replaceAll("<OPTIONS>", options);
        } else {
            logger.warn("The script result is empty or not exists result to view.");
            return null;
        }
    }

    public static Object fileToJSON(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        int result = fis.read(data);
        fis.close();
        if (result != -1) {
            String str = new String(data, "UTF-8");
            //hack when we have a response javascript undefined
            str = str.replaceAll("\"\\bundefined\\b\"|\\bundefined\\b", "\"undefined\"");
            return JSON.parse(str);
        } else {
            logger.warn("The script result is empty or not exists result to view.");
            return null;
        }

    }


    /**
     * creates a shell address to connect mongo
     *
     * @param defaultMongoHost    mongo host to run the script.
     * @param defaultMongoPort    port to run the script.
     * @param mongoAddressReplica address list to run the script.
     * @param mongoDatabase       database to connect
     * @param username            username used for connection
     * @param password            password used for connection
     * @return string with host value to connect mongo shell
     * @throws Exception Throw a exception if the command fail in mongo shell.
     */
    public static String createShellAddress(String defaultMongoHost,
                                            String defaultMongoPort,
                                            String mongoAddressReplica,
                                            String mongoDatabase,
                                            String username,
                                            String password) throws Exception {
        String testDB = "show dbs;";
        if (!isWindows) {
            testDB = "'" + testDB + "'";
        }
        String mongoHost = null;
        String mongoPort = null;
        String[] address;
        String[] addressReplica = mongoAddressReplica != null ? mongoAddressReplica.split(",") : new String[0];
        if (!StringUtils.isBlank(mongoAddressReplica)) {
            for (String repAddress : addressReplica) {
                address = repAddress.split(":");
                String ret;
                try {
                    if (StringUtils.isBlank(address[0])) {
                        logger.warn("Address host cannot be empty.");
                    } else if (StringUtils.isBlank(address[1])) {
                        logger.warn("Address port cannot be empty.");
                    } else if (!(address[0] instanceof String)) {
                        logger.warn("Address host must be String.");
                    } else if ((Integer.parseInt(address[1])) <= 0) {
                        logger.warn("Address port must be Integer.");
                    } else {
                        String mongoCommand = String.format("echo %1$s | mongo --host %2$s:%3$s -u %4$s -p %5$s " +
                                        "--quiet --authenticationDatabase admin <GREP> ", testDB, address[0],
                                address[1], username, password);
                        ret = ShellCommandUtil.executeCommand(mongoCommand).toString();
                        if (StringUtils.isBlank(ret)) {
                            logger.warn("Error on ShellCommandUtil.executeCommand.");
                        } else if (!ret.contains(mongoDatabase)) {
                            logger.warn("Database '" + mongoDatabase + "' does not exists.");
                        } else {
                            mongoHost = address[0];
                            mongoPort = address[1];
                            break;
                        }
                    }
                } catch (IndexOutOfBoundsException e) {
                    logger.warn("Error on address format, it must be 'host:port'.");
                } catch (NumberFormatException e) {
                    logger.warn("Address port must be Integer.");
                } catch (Exception e) {
                    ret = e.getMessage();
                    if (ret.contains("No address associated")) {
                        logger.warn("Unknown host '" + address[0] + "'.");
                    } else if (ret.contains("Invalid port number")) {
                        logger.warn("Unknown port '" + address[1] + "'.");
                    } else if (ret.contains("Authentication failed")) {
                        logger.warn("Authentication failed with given credentials.");
                    } else {
                        logger.warn("Error on ShellCommandUtil.executeCommand");
                    }
                }
            }
            if ((mongoHost == null || mongoPort == null)) {
                if (!StringUtils.isBlank(mongoAddressReplica) &&
                        !mongoAddressReplica.contains(defaultMongoHost + ":" + defaultMongoPort)) {
                    logger.warn("All mongo replica address connection failed, trying to use main connection.");
                    return createShellAddress(defaultMongoHost,
                            defaultMongoPort,
                            defaultMongoHost + ":" + defaultMongoPort,
                            mongoDatabase,
                            username,
                            password);
                } else {
                    logger.error("Mongo main connection failed.");
                    throw new ConnectException("Cannot connect to any host.");
                }
            } else {
                logger.info("Connecting to address '" + mongoHost + ":" + mongoPort + "' to generate report.");
                return String.format("--host %1$s:%2$s -u %3$s -p %4$s",
                        mongoHost,
                        mongoPort,
                        username,
                        password);
            }
        } else {
            mongoHost = defaultMongoHost;
            mongoPort = defaultMongoPort;
            logger.info("Connecting to address '" + mongoHost + ":" + mongoPort + "' to generate report.");
            return String.format("--host %1$s:%2$s -u %3$s -p %4$s",
                    mongoHost,
                    mongoPort,
                    username,
                    password);
        }
    }

    public Object runFileCommand(String filePath)
            throws Exception {
        String mongoCommand = String.format("%1$s \"%2$s\" <GREP> ", mongoURIShell, filePath);
        return JSON.parse(ShellCommandUtil.executeCommand(mongoCommand).toString());
    }


    public void initIndexes() {

        Thread mongoIdxThread = new Thread("Check Mongo Indexes") {
            public void run() {
                MongoDAOUtil.checkIndexes();
            }
        };
        mongoIdxThread.start();

    }

    public static void setupMongoPopDb(String mongoPrimary,
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
}
