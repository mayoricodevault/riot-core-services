package com.tierconnect.riot.api.mongoShell;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by achambi on 10/21/16.
 *
 */
public class ServerShellAddressTest {



    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testConstructorFail() throws Exception {
        try {
            new ServerShellAddress("10.100.1.30", 1234567456, ServerShellAddressType.SECONDARY);
            fail("Expected: NumberFormatException");
        } catch (NumberFormatException ex) {
            assertEquals("java.lang.NumberFormatException: port should be less or equals than 65535", ex.toString());
        }
    }

    /**
     * Test verify send port in host name [<HOSTNAME>]:<PORT>,<HOSTNAME>:PORT.
     *
     * @throws Exception Throws exception if test failed.
     */
    @Test
    public void testConstructorWithoutPort() throws Exception {
        ServerShellAddress resultA = new ServerShellAddress("[10.100.1.30]:1234", 27017, ServerShellAddressType
                .SECONDARY);
        assertEquals("10.100.1.30", resultA.getHost());
        assertEquals(1234, resultA.getPort());
        ServerShellAddress resultB = new ServerShellAddress("10.100.1.30:5678", 27017, ServerShellAddressType
                .MASTER);
        assertEquals("10.100.1.30", resultB.getHost());
        assertEquals(5678, resultB.getPort());

        ServerShellAddress result = new ServerShellAddress("10.100.1.30]:1234", 27017, ServerShellAddressType
                .MASTER);
        assertEquals("10.100.1.30]", result.getHost());
        assertEquals(1234, result.getPort());
    }

    @Test
    public void testConstructor() throws Exception {
        ServerShellAddress serverShellAddress = new ServerShellAddress("LOCALHOSTMASTER", 27017, ServerShellAddressType.MASTER);
        assertEquals("localhostmaster", serverShellAddress.getHost());
        assertEquals(27017, serverShellAddress.getPort());
        assertEquals(ServerShellAddressType.MASTER, serverShellAddress.getServerShellAddressType());
    }

    @Test
    public void testEquals() throws Exception {
        ServerShellAddress serverShellAddress = new ServerShellAddress("localhostmaster", 27017,
                ServerShellAddressType.MASTER);
        //noinspection ConstantConditions
        assertEquals(false, serverShellAddress == null);
        assertEquals(true, serverShellAddress.equals(new ServerShellAddress("localhostmaster", 27017,
                ServerShellAddressType.SECONDARY)));
        assertEquals(true, serverShellAddress.equals(new ServerShellAddress("localhostmaster", 27017,
                ServerShellAddressType.MASTER)));
        assertEquals(false, serverShellAddress.equals(new ServerShellAddress("localhostmaster", 27018,
                ServerShellAddressType.MASTER)));
        assertEquals(false, serverShellAddress.equals(new ServerShellAddress("localhostmaster", 27018,
                ServerShellAddressType.SECONDARY)));
        assertEquals(false, serverShellAddress.equals(new ServerShellAddress("localhostmaster2", 27017,
                ServerShellAddressType.MASTER)));
        assertEquals(false, serverShellAddress.equals(new ServerShellAddress("localhostmaster2", 27017,
                ServerShellAddressType.SECONDARY)));
    }

    @Test
    public void testHashCode() throws Exception {

    }

    @Test
    public void testGetHost() throws Exception {

    }

    @Test
    public void testGetPort() throws Exception {

    }

    @Test
    public void testGetSocketAddress() throws Exception {

    }

    @Test
    public void testToString() throws Exception {
        ServerShellAddress serverShellAddress = new ServerShellAddress("lOcAlHoStMaStEr", 27018,
                ServerShellAddressType.SECONDARY);
        String expect = "localhostmaster:27018";
        String result = serverShellAddress.toString();
        assertEquals(result, expect);
    }

    @Test
    public void testToStringCase2() throws Exception {
        ServerShellAddress serverShellAddress = new ServerShellAddress("lOcAlHoStMaStEr", 27018,
                ServerShellAddressType.SECONDARY);
        String expect = "localhostmaster:27018";
        String result = serverShellAddress.toString();
        assertEquals(result, expect);
    }

    @Test
    public void testDefaultHost() throws Exception {
        assertEquals("127.0.0.1",ServerShellAddress.defaultHost());

    }

    @Test
    public void testDefaultPort() throws Exception {
        assertEquals(27017,ServerShellAddress.defaultPort());
    }

    @Test
    public void testSameHost() throws Exception {
        ServerShellAddress serverShellAddress = new ServerShellAddress("localhostmaster", 27017,
                ServerShellAddressType.MASTER);
        assertEquals(true,serverShellAddress.sameHost("locAlHostMasTer"));
        assertEquals(false,serverShellAddress.sameHost("locAlHostMasterTest"));
    }
}