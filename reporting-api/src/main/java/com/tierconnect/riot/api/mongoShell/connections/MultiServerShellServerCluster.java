package com.tierconnect.riot.api.mongoShell.connections;


import com.mongodb.connection.ClusterConnectionMode;
import com.mongodb.connection.ClusterType;
import com.tierconnect.riot.api.mongoShell.ServerShellAddress;
import com.tierconnect.riot.api.mongoShell.ServerShellAddressType;
import com.tierconnect.riot.api.mongoShell.ServerStringFormat;

import org.apache.commons.lang.StringUtils;


import java.util.*;

import static com.tierconnect.riot.api.assertions.Assertions.*;


/**
 * Created by achambi on 10/21/16.
 * Class to implement a BasicShellServerCluster.
 */
public class MultiServerShellServerCluster extends BaseServerShellCluster {

    /**
     * A set of replicas and master address.
     */
    private Set<ServerShellAddress> serverShellAddressSet;

    public MultiServerShellServerCluster(final ClusterConnectionMode connectionMode,
                                         final String dataBaseName,
                                         final ClusterType clusterType,
                                         final List<ServerShellAddress> serverShellAddresses) {

        super(connectionMode,
                clusterType,
                dataBaseName,
                connectionMode == ClusterConnectionMode.MULTIPLE,
                clusterType == ClusterType.REPLICA_SET,
                isNotBlank(dataBaseName));

        voidNotNull("serverShellAddressSet", serverShellAddresses);

        Set<ServerShellAddress> serverDescriptionSet = new TreeSet<>(new Comparator<ServerShellAddress>() {
            @Override
            public int compare(final ServerShellAddress o1, final ServerShellAddress o2) throws
                    IllegalArgumentException {
                int valHost = o1.getHost().compareTo(o2.getHost());
                if (valHost != 0) {
                    if (o1.getServerShellAddressType().compareTo(ServerShellAddressType.MASTER) == 0 &&
                            o2.getServerShellAddressType().compareTo(ServerShellAddressType.MASTER) == 0) {
                        throw new IllegalArgumentException("There must be just one master.");
                    }
                    return valHost;
                }
                int valPort = integerCompare(o1.getPort(), o2.getPort());
                if (valPort != 0) {
                    return valPort;
                }
                int valAddressType = o1.getServerShellAddressType().compareTo(o2.getServerShellAddressType());
                if (valAddressType != 0) {
                    if (o1.getServerShellAddressType().compareTo(ServerShellAddressType.MASTER) == 0) {
                        throw new IllegalArgumentException("There must be just one master.");
                    }
                }
                return valAddressType;
            }

            private int integerCompare(final int p1, final int p2) {
                return (p1 < p2) ? -1 : ((p1 == p2) ? 0 : 1);
            }
        });

        serverDescriptionSet.addAll(serverShellAddresses);
        this.serverShellAddressSet = Collections.unmodifiableSet(serverDescriptionSet);
    }

    @Override
    public String getShellAddress(ServerStringFormat serverStringFormat) {
        if (serverStringFormat == ServerStringFormat.SHELL) {
            return "--host " + StringUtils.join(this.serverShellAddressSet.iterator(), ',') + " " + this
                    .getDataBaseName();
        } else if (serverStringFormat == ServerStringFormat.URI) {
            return StringUtils.join(this.serverShellAddressSet.iterator(), ',') + "/" + this.getDataBaseName();
        } else {
            throw new UnsupportedOperationException("Unsupported serverStringFormat mode: " + serverStringFormat);
        }
    }

    /**
     * Get the server Shell Address set.
     *
     * @return the Set of serverShellAddresses
     */
    public Set<ServerShellAddress> getServerShellAddressSet() {
        return serverShellAddressSet;
    }

}
