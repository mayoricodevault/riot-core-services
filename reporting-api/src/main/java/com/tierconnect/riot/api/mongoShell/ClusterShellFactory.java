package com.tierconnect.riot.api.mongoShell;


import com.mongodb.connection.ClusterConnectionMode;
import com.mongodb.connection.ClusterType;

import com.tierconnect.riot.api.mongoShell.connections.BaseServerShellCluster;
import com.tierconnect.riot.api.mongoShell.connections.MultiServerShellServerCluster;
import com.tierconnect.riot.api.mongoShell.connections.SingleServerShellServerCluster;
import com.tierconnect.riot.api.configuration.PropertyReader;

import java.util.LinkedList;
import java.util.List;

import static com.mongodb.assertions.Assertions.isTrue;
import static com.mongodb.assertions.Assertions.notNull;
import static com.tierconnect.riot.api.assertions.Assertions.*;

/**
 * Created by achambi on 10/21/16.
 * Cluster Shell Factory to create a single shell address or multiple.
 */
class ClusterShellFactory {

    /**
     * Creates a cluster with the given settings.  The cluster mode will be based on the mode from the settings.
     *
     * @param connectionMode         the cluster connections Mode
     * @param clusterType            the cluster Type
     * @param serverShellAddressList the list of server to connect
     * @return the ShellCluster
     */
    @SuppressWarnings("unused")
    public static BaseServerShellCluster create(final ClusterConnectionMode connectionMode,
                                                final String replicaSetName,
                                                final String dataBaseName,
                                                final ClusterType clusterType,
                                                final List<ServerShellAddress> serverShellAddressList) {
        if (connectionMode == ClusterConnectionMode.SINGLE) {
            List<ServerShellAddress> serverShellAddressSingleList = notNull("serverShellAddressList",
                    serverShellAddressList);
            isTrue("one server in a direct cluster", serverShellAddressList.size() == 1);
            return new SingleServerShellServerCluster(connectionMode,
                    ClusterType.STANDALONE,
                    dataBaseName,
                    serverShellAddressSingleList.get(0));
        } else if (connectionMode == ClusterConnectionMode.MULTIPLE) {
            return new MultiServerShellServerCluster(connectionMode, dataBaseName, clusterType, serverShellAddressList);
        } else {
            throw new UnsupportedOperationException("Unsupported cluster mode: " + connectionMode);
        }
    }

    public static BaseServerShellCluster create(final String dataBaseName, final String host, int port) {
        return new SingleServerShellServerCluster(ClusterConnectionMode.SINGLE, ClusterType.STANDALONE, dataBaseName,
                new ServerShellAddress(host, port, ServerShellAddressType.MASTER));
    }

    /***
     * Creates a cluster with the given settings.  The cluster mode will be based on the mode from the settings.
     *
     * @return the ShellCluster
     */
    static BaseServerShellCluster create() {
        String mongoPrimary = PropertyReader.getProperty("mongo.primary", "127.0.0.1:27017", true);
        String replicaSetName = PropertyReader.getProperty("mongo.replicaset", "", false);
        String mongoSecondary = PropertyReader.getProperty("mongo.secondary", "", false);
        boolean mongoShardding = Boolean.parseBoolean(PropertyReader.getProperty("mongo.sharding", "false", true));
        String mongoDataBase = PropertyReader.getProperty("mongo.db", "", true);

        voidNotNull("mongoPrimary", mongoPrimary);
        isTrueArgument("mongoPrimary", "a valid address", isNotBlank(mongoPrimary));

        ServerShellAddress masterAddress = ServerShellAddress.buildServerAddress(mongoPrimary,
                ServerShellAddressType.MASTER);

        if (mongoShardding) {
            return new SingleServerShellServerCluster(ClusterConnectionMode.SINGLE, ClusterType.SHARDED,
                    mongoDataBase, masterAddress);
        }
        if (isBlank(mongoSecondary) && isBlank(replicaSetName)) {
            return new SingleServerShellServerCluster(ClusterConnectionMode.SINGLE, ClusterType.STANDALONE,
                    mongoDataBase, masterAddress);
        }

        List<ServerShellAddress> listServerShellAddresses = new LinkedList<>();
        listServerShellAddresses.add(masterAddress);
        String[] serverAddressReplicaSet = mongoSecondary.split(",");
        for (String address : serverAddressReplicaSet) {
            if (!isBlank(address)) {
                listServerShellAddresses.add(ServerShellAddress.buildServerAddress(address, ServerShellAddressType
                        .SECONDARY));
            }
        }
        return new MultiServerShellServerCluster(
                ClusterConnectionMode.MULTIPLE,
                mongoDataBase,
                ClusterType.REPLICA_SET,
                listServerShellAddresses);
    }
}
