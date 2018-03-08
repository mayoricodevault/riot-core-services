package com.tierconnect.riot.commons.dao.mongo;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Created by achambi on 11/15/16.
 * Generate URI to connect in mongoDB with replica set and shardding.
 */
public class MongoURIBuilder {

    /**
     * Createa URI to mongo connect.
     *
     * @param mongoUsername       the user name to mongo connect.
     * @param mongoPassword       the mongo password to mongo connect [plane text]
     * @param mongoPrimary        the mongo Master ip or domain name.
     * @param mongoSecondary      the mongo secondary list in the next string format:
     *                            "192.167.1.1:2017,192.167.1.1:2018,192.167.1.1:2019"
     * @param mongoDB             the mongo database name to connect (this field is optional).
     * @param mongoReplicaSet     the name of the replica set, if the mongod is a member of a replica set.
     * @param mongoSSL            true: Initiate the connection with TLS/SSL.
     *                            false: Initiate the connection without TLS/SSL.
     *                            The default value is false.
     * @param mongoConnectTimeout The time in milliseconds to attempt a connection before timing out. The default is
     *                            never to timeout.
     * @param mongoAuthDB         the database name associated with the user’s credentials, if the users collection
     *                            do not exist in the database where the client is connecting. authSource defaults to
     *                            the database specified in the connection string.
     * @param mongoMaxPoolSize    the maximum number of connections in the connection pool. The default value is 100.
     * @param readPreference      the replica set read preference for this connection. The read preference
     *                            values are the following:
     *                            primary
     *                            primaryPreferred
     *                            secondary
     *                            secondaryPreferred
     *                            nearest
     * @return String with URI connection to mongo database.
     */
    public static String buildMongoURI(String mongoUsername,
                                String mongoPassword,
                                String mongoPrimary,
                                String mongoSecondary,
                                String mongoDB,
                                String mongoReplicaSet,
                                Boolean mongoSSL,
                                Integer mongoConnectTimeout,
                                String mongoAuthDB,
                                Integer mongoMaxPoolSize,
                                String readPreference
    ) {
        StringBuilder uri = new StringBuilder("mongodb://");
        if(mongoUsername != null && mongoPassword != null){
            uri.append(mongoUsername).append(":").append(mongoPassword).append("@");
        }
        uri.append(mongoPrimary);
        if (StringUtils.isNotBlank(mongoSecondary)) {
            uri.append(",").append(mongoSecondary);
        }
        uri.append("/").append(mongoDB);

        StringBuilder uriOptions = new StringBuilder();

        addOption(uriOptions, "replicaSet", mongoReplicaSet, StringUtils.isNotBlank(mongoReplicaSet));
        addOption(uriOptions, "ssl", mongoSSL, (mongoSSL != null));
        addOption(uriOptions, "connectTimeoutMS", mongoConnectTimeout, (mongoConnectTimeout != null &&
                mongoConnectTimeout > 0));
        addOption(uriOptions, "authSource", mongoAuthDB, StringUtils.isNotBlank(mongoAuthDB));
        addOption(uriOptions, "maxPoolSize", mongoMaxPoolSize, (mongoMaxPoolSize != null && mongoMaxPoolSize
                > 0));
        addOption(uriOptions, "readPreference", readPreference, StringUtils.isNotBlank(readPreference));

        if (uriOptions.length() > 0) {
            uri.append("?").append(uriOptions);
        }
        return uri.toString();
    }

    /**
     * Add Option to URI Options
     *
     * @param uriOptions  URI Options in String Builder format.
     * @param optionName  the option name to set in URI option string.
     * @param optionValue the option value to set in URI option string.
     * @param condition   Condition to set or not the value in URI String Builder.
     */
    private static void addOption(StringBuilder uriOptions, final String optionName, final Object
            optionValue, boolean condition) {
        if (condition) {
            if (uriOptions.length() > 0) {
                uriOptions.append("&");
            }
            uriOptions.append(optionName).append("=").append(optionValue);
        }
    }

    private static void addShellOption(StringBuilder uriOptions, final String optionName, final Object
            optionValue, Boolean condition) {
        if (condition!=null && condition) {
            if (uriOptions.length() > 0) {
                uriOptions.append(" ");
            }
            uriOptions.append("--").append(optionName);
            if (optionValue != null && StringUtils.isNotBlank(optionValue.toString())) {
                uriOptions.append("=").append(optionValue);
            }
        }
    }

    public static String buildMongoURIShell(String mongoUsername,
                                            String mongoPassword,
                                            String mongoPrimary,
                                            String mongoSecondary,
                                            String mongoDB,
                                            String mongoReplicaSet,
                                            Boolean mongoSSL,
                                            Integer mongoConnectTimeout,
                                            String mongoAuthDB,
                                            Integer mongoMaxPoolSize,
                                            String readPreference
    ) {
        StringBuilder uri = new StringBuilder("mongo \"mongodb://");
        uri.append(mongoPrimary);
        if (StringUtils.isNotBlank(mongoSecondary)) {
            uri.append(",").append(mongoSecondary);
        }
        uri.append("/").append(mongoDB);

        StringBuilder uriOptions = new StringBuilder();
        addOption(uriOptions, "readPreference", readPreference, StringUtils.isNotBlank(readPreference));
        addOption(uriOptions, "replicaSet", mongoReplicaSet, StringUtils.isNotBlank(mongoReplicaSet));
        addOption(uriOptions, "connectTimeoutMS", mongoConnectTimeout, (mongoConnectTimeout != null &&
                mongoConnectTimeout != 0));
        addOption(uriOptions, "maxPoolSize", mongoMaxPoolSize, (mongoMaxPoolSize != null && mongoMaxPoolSize
                != 0));
        uriOptions.append("\"");
        addShellOption(uriOptions, "authenticationDatabase", mongoAuthDB, StringUtils.isNotBlank(mongoAuthDB));
        addShellOption(uriOptions, "quiet", "", true);
        addShellOption(uriOptions, "ssl", "", mongoSSL);
        addShellOption(uriOptions, "username", mongoUsername, StringUtils.isNotBlank(mongoUsername));
        addShellOption(uriOptions, "password", String.format("\"%1$s\"", StringEscapeUtils.escapeJavaScript
                (mongoPassword)), StringUtils.isNotBlank(mongoPassword));
        if (uriOptions.length() > 0) {
            uri.append("?").append(uriOptions);
        }
        return uri.toString();
    }

}
