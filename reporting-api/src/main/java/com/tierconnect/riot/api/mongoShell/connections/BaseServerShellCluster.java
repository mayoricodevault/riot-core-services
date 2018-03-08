package com.tierconnect.riot.api.mongoShell.connections;


import com.mongodb.connection.ClusterConnectionMode;
import com.mongodb.connection.ClusterType;

import com.tierconnect.riot.api.mongoShell.ServerShellAddress;
import com.tierconnect.riot.api.mongoShell.ServerStringFormat;
import com.tierconnect.riot.api.assertions.Assertions;
import com.tierconnect.riot.api.mongoShell.exception.NotImplementedException;

import static com.tierconnect.riot.api.assertions.Assertions.isJustAWord;


/**
 * Created by achambi on 10/21/16.
 * Class Base to implement a SHellCluster Server.
 */
public class BaseServerShellCluster implements ShellCluster {
    /**
     * Connection Mode Enum to set, Values:SINGLE, MULTIPLE.
     */
    private ClusterConnectionMode connectionMode;
    /**
     * Cluster Type to set, values: STANDALONE, REPLICA_SET, SHARDED, UNKNOWN
     */
    private ClusterType clusterType;

    public String getDataBaseName() {
        return dataBaseName;
    }

    /**
     * the data Base name to connect the shell cluster.
     */
    private String dataBaseName;

    /**
     * Class constructor.
     *
     * @param connectionMode {@link ClusterConnectionMode}        the connections mode to set in shell cluster [Single, MULTIPLE].
     * @param clusterType           the cluster type used in the Server [STANDALONE, REPLICA_SET, SHARDED, UNKNOWN].
     * @param connectionModeIsValid the logic sentence to verify connections mode is valid.
     * @param clusterTypeIsValid    the logic sentence to verify cluster type is valid.
     */
    BaseServerShellCluster(final ClusterConnectionMode connectionMode,
                           final ClusterType clusterType,
                           final String dataBaseName,
                           final boolean connectionModeIsValid,
                           final boolean clusterTypeIsValid,
                           final boolean dataBaseNameIsValid
    ) {

        Assertions.voidNotNull("connectionMode", connectionMode);
        Assertions.voidNotNull("clusterType", clusterType);
        Assertions.voidNotNull("dataBaseName", dataBaseName);
        isJustAWord(dataBaseName, "Invalid database name format. Database name is either empty or it " +
                "contains spaces.");

        Assertions.isTrue("connectionMode", connectionModeIsValid);
        Assertions.isTrue("clusterType", clusterTypeIsValid);
        Assertions.isTrue("dataBaseName", dataBaseNameIsValid);

        this.connectionMode = connectionMode;
        this.clusterType = clusterType;
        this.dataBaseName = dataBaseName;
    }

    /**
     * Mot implement..
     *
     * @return Nothing.
     * @throws NotImplementedException always.
     */
    @Override
    public String getShellAddress(ServerStringFormat serverStringFormat) {
        throw new NotImplementedException();
    }

    /**
     * Mot implement..
     *
     * @return Nothing.
     * @throws NotImplementedException always.
     */
    @Override
    public ServerShellAddress getShellAddress() {
        throw new NotImplementedException();
    }

    /**
     * Gets connections mode of class.
     *
     * @return ClusterConnectionMode.
     */
    public ClusterConnectionMode getConnectionMode() {
        return connectionMode;
    }

    /**
     * Gets the cluster Type of class
     *
     * @return ClusterType clusterType of class.
     */
    public ClusterType getClusterType() {
        return clusterType;
    }
}
