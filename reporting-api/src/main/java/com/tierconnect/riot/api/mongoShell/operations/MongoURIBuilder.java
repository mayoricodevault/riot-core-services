package com.tierconnect.riot.api.mongoShell.operations;

import com.tierconnect.riot.api.mongoShell.MongoShellClientOption;
import com.tierconnect.riot.api.mongoShell.ServerStringFormat;
import com.tierconnect.riot.api.mongoShell.connections.BaseServerShellCluster;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Created by achambi on 11/15/16.
 * Generate URI to connect in mongoDB with replica set and shardding.
 */
public class MongoURIBuilder {

    private static Logger logger = Logger.getLogger(MongoURIBuilder.class);

    /**
     * Createa URI to mongo connect.
     *
     * @param username           the user name to mongo connect.
     * @param password           the mongo password to mongo connect [plane text]
     * @param authDataBase       the database name associated with the user’s credentials, if the users collection
     *                           do not exist in the database where the client is connecting. authSource defaults to
     *                           the database specified in the connections string.
     * @param options            the connections options.
     * @param serverShellCluster the connections address.
     * @return String with URI connections to mongo database.
     */

    static String buildMongoURI(String username,
                                String password,
                                String authDataBase,
                                MongoShellClientOption options,
                                BaseServerShellCluster serverShellCluster,
                                String dataBaseName
    ) {
        StringBuilder uri = new StringBuilder("mongodb://");
        uri.append(username).append(":").append(password);
        uri.append("@").append(serverShellCluster.getShellAddress(ServerStringFormat.URI));
        uri.append("/").append(dataBaseName);

        StringBuilder uriOptions = new StringBuilder();

        addOption(uriOptions, "authSource", authDataBase, true);
        addOption(uriOptions, "ssl", options.getSslEnabled(), true);
        addOption(uriOptions, "connectTimeoutMS", options.getConnectTimeout(), options.getConnectTimeout() > 0);
        addOption(uriOptions, "maxPoolSize", options.getMaxPoolSize(), options.getMaxPoolSize() > 0);
        addOption(uriOptions, "readPreference", options.getReadPreference().toString(), true);

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
        }else{
            logger.debug("The parameter: " + optionName + " is not valid, value: " + optionValue);
        }
    }

    private static void addShellOption(StringBuilder uriOptions,
                                       final String optionName,
                                       final Object optionValue,
                                       Boolean condition) {
        if (condition != null && condition) {
            if (uriOptions.length() > 0) {
                uriOptions.append(" ");
            }
            uriOptions.append("--").append(optionName);
            if (optionValue != null && StringUtils.isNotBlank(optionValue.toString())) {
                uriOptions.append("=").append(optionValue);
            }
        }
    }

    /**
     * Createa command to mongo connect.
     *
     * @param username           the user name to mongo connect.
     * @param password           the mongo password to mongo connect [plane text]
     * @param authDataBase       the database name associated with the user’s credentials, if the users collection
     *                           do not exist in the database where the client is connecting. authSource defaults to
     *                           the database specified in the connections string.
     * @param options            the connections options.
     * @param serverShellCluster the connections address.
     * @return String with Shell connections to mongo database.
     */
    public static String buildMongoURICommand(String username,
                                              String password,
                                              String authDataBase,
                                              MongoShellClientOption options,
                                              BaseServerShellCluster serverShellCluster
    ) {
        StringBuilder stringCommand = new StringBuilder("mongo \"mongodb://");
        stringCommand.append(serverShellCluster.getShellAddress(ServerStringFormat.URI));

        StringBuilder uriOptions = new StringBuilder();
        addOption(uriOptions, "replicaSet", options.getRequiredReplicaSetName(), StringUtils.isNotBlank(options
                .getRequiredReplicaSetName()));
        addOption(uriOptions, "readPreference", options.getReadPreference().getName(), true);
        addOption(uriOptions, "connectTimeoutMS", options.getConnectTimeout(), options.getConnectTimeout() != 0);
        addOption(uriOptions, "maxPoolSize", options.getMaxPoolSize(), options.getMaxPoolSize() != 0);
        uriOptions.append("\"");
        addShellOption(uriOptions, "authenticationDatabase", authDataBase, true);
        addShellOption(uriOptions, "quiet", "", true);
        addShellOption(uriOptions, "ssl", "", options.getSslEnabled());
        addShellOption(uriOptions, "username", username, true);
        addShellOption(uriOptions, "password", String.format("\"%1$s\"", StringEscapeUtils.escapeJavaScript(password))
                , true);

        if (uriOptions.length() > 0 && !stringCommand.toString().contains("?")) {
            stringCommand.append("?").append(uriOptions);
        }
        return stringCommand.toString();
    }
}
