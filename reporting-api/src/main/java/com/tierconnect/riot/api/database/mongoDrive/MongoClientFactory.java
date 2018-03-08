package com.tierconnect.riot.api.database.mongoDrive;

import com.mongodb.*;
import com.tierconnect.riot.api.configuration.PropertyReader;
import com.tierconnect.riot.api.database.codecs.MapResultCodec;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.LinkedList;
import java.util.List;

import static com.tierconnect.riot.api.assertions.Assertions.*;

/**
 * Created by achambi on 12/22/16.
 * class to create a new client driver instance.
 */
public class MongoClientFactory {

    private static final String ADMIN_DATABASE_NAME = "admin";

    private MongoClientFactory() {
    }

    public static MongoClient createClient() {

        String userName = System.getProperty("mongo.username");
        String password = System.getProperty("mongo.password");
        String authDataBase = PropertyReader.getProperty("mongo.authdb", ADMIN_DATABASE_NAME, true);
        voidNotNull("userName", userName);
        voidNotNull("password", password);
        voidNotNull("authDataBase", authDataBase);

        String mongoPrimary = PropertyReader.getProperty("mongo.primary", "127.0.0.1:27017", true);
        String replicaSetName = PropertyReader.getProperty("mongo.replicaset", "", false);
        String mongoSecondary = PropertyReader.getProperty("mongo.secondary", "", false);
        boolean mongoShardding = Boolean.parseBoolean(PropertyReader.getProperty("mongo.sharding", "false", true));


        voidNotNull("mongoPrimary", mongoPrimary);
        isTrueArgument("mongoPrimary", "a valid address", isNotBlank(mongoPrimary));

        /*MongoOptions*/
        CodecRegistry codecRegistry = CodecRegistries.fromRegistries(MongoClient.getDefaultCodecRegistry(),
                CodecRegistries.fromCodecs(new MapResultCodec()));
        MongoClientOptions options = MongoClientOptions.builder()
                .codecRegistry(codecRegistry)
                .readPreference(
                        ReadPreference.valueOf(
                                PropertyReader.getProperty("mongo.reportsReadPreference", "primary", true)))
                .connectTimeout(Integer.parseInt(PropertyReader.getProperty("mongo.connectiontimeout", "0", true)))
                .connectionsPerHost(getMaxPoolSize())
                .sslEnabled(Boolean.parseBoolean(PropertyReader.getProperty("mongo.ssl", "false", true)))
                .build();

        /*Mongo Credentials*/
        List<MongoCredential> credentials = new LinkedList<>();
        //noinspection unchecked
        credentials.add(MongoCredential.createCredential(userName, authDataBase, password.toCharArray()));

        ServerAddress masterAddress = new ServerAddress(mongoPrimary);
        List<ServerAddress> addressList = new LinkedList<>();
        addressList.add(masterAddress);

        if (mongoShardding) {
            return new MongoClient(addressList, credentials, options);
        }
        if (isBlank(mongoSecondary) && isBlank(replicaSetName)) {
            return new MongoClient(addressList, credentials, options);
        }
        String[] serverAddressReplicaSet = mongoSecondary.split(",");
        for (String address : serverAddressReplicaSet) {
            if (!isBlank(address)) {
                addressList.add(new ServerAddress(address));
            }
        }
        return new MongoClient(addressList, credentials, options);
    }

    private static int getMaxPoolSize() {
        int poolSize = Integer.parseInt(PropertyReader.getProperty("mongo.maxpoolsize", "100", true));
        return (poolSize > 0) ? poolSize : 100;
    }
}
