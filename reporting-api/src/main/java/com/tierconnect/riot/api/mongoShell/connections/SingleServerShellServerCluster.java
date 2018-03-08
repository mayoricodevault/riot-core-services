package com.tierconnect.riot.api.mongoShell.connections;


import com.mongodb.connection.ClusterConnectionMode;
import com.mongodb.connection.ClusterType;
import com.tierconnect.riot.api.mongoShell.ServerShellAddress;
import com.tierconnect.riot.api.mongoShell.ServerShellAddressType;
import com.tierconnect.riot.api.mongoShell.ServerStringFormat;

import static com.mongodb.assertions.Assertions.notNull;
import static com.tierconnect.riot.api.assertions.Assertions.isNotBlank;
import static com.tierconnect.riot.api.assertions.Assertions.isTrueArgument;
import static com.tierconnect.riot.api.assertions.Assertions.voidNotNull;

/**
 * Created by achambi on 10/21/16.
 * Single Shell Server Cluster
 */
public class SingleServerShellServerCluster extends BaseServerShellCluster {

    private ServerShellAddress serverShellAddress;

    public SingleServerShellServerCluster(final ClusterConnectionMode connectionMode,
                                          final ClusterType clusterType,
                                          final String dataBaseName,
                                          final ServerShellAddress serverShellAddresses) {
        super(connectionMode,
                clusterType,
                dataBaseName,
                connectionMode == ClusterConnectionMode.SINGLE,
                (clusterType == ClusterType.STANDALONE) || (clusterType == ClusterType.SHARDED),
                isNotBlank(dataBaseName));
        voidNotNull("serverShellAddresses", serverShellAddresses);
        isTrueArgument("ServerShellAddress.ServerShellAddressType", "MASTER", ServerShellAddressType.MASTER ==
                serverShellAddresses.getServerShellAddressType());
        this.serverShellAddress = notNull("serverShellAddresses", serverShellAddresses);
    }


    /**
     * Convert ServerShellAddress object to string in SHELL AND URI command format.
     *
     * @return the String it contains the shell comand in the next format "--host [HOST_NAME]:[PORT_NAME]" or
     * "[HOST_NAME]:[PORT_NAME]"
     */
    @Override
    public String getShellAddress(ServerStringFormat serverStringFormat) {
        if (serverStringFormat == ServerStringFormat.SHELL) {
            return "--host " + serverShellAddress.toString() + " " + this.getDataBaseName();
        } else if (serverStringFormat == ServerStringFormat.URI) {
            return serverShellAddress.toString() + "/" + this.getDataBaseName();
        } else {
            throw new UnsupportedOperationException("Unsupported serverStringFormat mode: " + serverStringFormat);
        }

    }

    /**
     * get ServerShellAddress.
     *
     * @return the {@link ServerShellAddress}
     */
    @Override
    public ServerShellAddress getShellAddress() {
        return serverShellAddress;
    }
}
