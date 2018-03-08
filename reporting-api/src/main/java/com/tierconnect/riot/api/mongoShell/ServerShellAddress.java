package com.tierconnect.riot.api.mongoShell;

import com.mongodb.ServerAddress;
import com.tierconnect.riot.api.assertions.Assertions;
import org.apache.commons.validator.routines.IntegerValidator;

import static com.tierconnect.riot.api.assertions.Assertions.isLessOrEqualsThan;


/**
 * Created by achambi on 10/21/16.
 * Class extends to Mongo ServerAddress.
 */
public class ServerShellAddress extends ServerAddress {

    private static final int MAX_PORT_NUMBER = 65535;
    private ServerShellAddressType serverShellAddressType;

    public ServerShellAddress(String host, int port, ServerShellAddressType serverShellAddressType) {
        super(host, port);
        isLessOrEqualsThan("port", this.getPort(), MAX_PORT_NUMBER);
        this.serverShellAddressType = serverShellAddressType;
    }

    public ServerShellAddressType getServerShellAddressType() {
        return serverShellAddressType;
    }

    /**
     * Create a instance of Server shell address.
     *
     * @return a instance of ServerShellAddress
     */
     static ServerShellAddress buildServerAddress(String stringServerAddress, ServerShellAddressType
            serverShellAddressType) {
        stringServerAddress = Assertions.notNull("stringServerAddress", stringServerAddress);
        String[] serverAddress = stringServerAddress.split(":");
        Assertions.isTrueArgument("serverAddressString", "Array [length = 2]", (serverAddress.length == 2));
        String host = serverAddress[0];
        Integer port = IntegerValidator.getInstance().validate(serverAddress[1]);
        Assertions.voidNotNull("port", port);
        return new ServerShellAddress(host, port, serverShellAddressType);
    }
}
