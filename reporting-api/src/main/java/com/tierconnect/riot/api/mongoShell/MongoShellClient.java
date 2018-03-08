package com.tierconnect.riot.api.mongoShell;

import com.tierconnect.riot.api.configuration.PropertyReader;
import com.tierconnect.riot.api.mongoShell.connections.BaseServerShellCluster;
import com.tierconnect.riot.api.mongoShell.operations.OperationExecutor;


import static com.tierconnect.riot.api.assertions.Assertions.*;


/**
 * Created by achambi on 11/24/16.
 * Mongo client
 */
public class MongoShellClient {

    private static final String ADMIN_DATABASE_NAME = "admin";

    //<editor-fold desc="Class properties">
    private final MongoShellClientOption options;
    private final BaseServerShellCluster serverShellCluster;
    private final String userName;
    private final String password;
    private final String authDataBase;
    private final OperationExecutor operationExecutor;

    public OperationExecutor getExecutor() {
        return operationExecutor;
    }

    //</editor-fold>

    //<editor-fold desc="Class getters">
    public MongoShellClientOption getOptions() {
        return options;
    }

    BaseServerShellCluster getServerShellCluster() {
        return serverShellCluster;
    }

    /**
     * Get user name.
     * @return a {@link String} with contains the auth database name.
     */
    String getUserName() {
        return userName;
    }

    /**
     * * Get password.
     * @return a {@link String} with contains the password.
     */
    public String getPassword() {
        return password;
    }

    /***
     *
     * @return a {@link String} with contains the auth database name.
     */
    String getAuthDataBase() {
        return authDataBase;
    }

    /**
     * @return a {@code MongoDatabase} representing the specified database
     */
    public MongoDataBase getDataBase() {
        return new MongoDataBase(this.getExecutor());
    }

    //</editor-fold>

    //<editor-fold desc="Class constructor">
    public MongoShellClient() {
        String userName = System.getProperty("mongo.username");
        String password = System.getProperty("mongo.password");
        String authDataBase = PropertyReader.getProperty("mongo.authdb", ADMIN_DATABASE_NAME, true);
        voidNotNull("userName", userName);
        voidNotNull("password", password);
        voidNotNull("authDataBase", authDataBase);

        this.userName = userName;
        this.password = password;
        this.authDataBase = authDataBase;
        this.options = new MongoShellClientOption(MongoShellClientOption.builder());
        this.serverShellCluster = ClusterShellFactory.create();
        this.operationExecutor = new OperationExecutor(
                this.userName,
                this.password,
                this.authDataBase,
                this.options,
                this.serverShellCluster);
    }

    public MongoShellClient(String userName,
                            String password,
                            String authDataBase,
                            String dataBase,
                            String host,
                            int port) {
        voidNotNull("userName", userName);
        voidNotNull("password", password);
        voidNotNull("authDataBase", authDataBase);
        voidNotNull("dataBase", dataBase);
        voidNotNull("host", host);
        voidNotNull("port", port);

        this.options = new MongoShellClientOption(MongoShellClientOption.builder().readPreference(ReadShellPreference
                .secondary()));
        this.serverShellCluster = ClusterShellFactory.create(dataBase, host, port);
        this.userName = userName;
        this.password = password;
        this.authDataBase = authDataBase;
        this.operationExecutor = new OperationExecutor(
                this.userName,
                this.password,
                this.authDataBase,
                this.options,
                this.serverShellCluster);
    }

    //</editor-fold>

    //<editor-fold desc="Singleton instance">
    private static MongoShellClient ourInstance;

    public static MongoShellClient getInstance() {
        if (ourInstance == null) {
            ourInstance = new MongoShellClient();
        }
        return ourInstance;
    }
    //</editor-fold>

}

