package com.tierconnect.riot.api.mongoShell.connections;

import com.tierconnect.riot.api.mongoShell.ServerShellAddress;
import com.tierconnect.riot.api.mongoShell.ServerStringFormat;

/**
 * Created by achambi on 10/21/16.
 * Interface to set getShellAddress method.
 */
interface ShellCluster {
    String getShellAddress(ServerStringFormat serverStringFormat);

    ServerShellAddress getShellAddress();
}
