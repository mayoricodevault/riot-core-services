package com.tierconnect.riot.iot.reports.autoindex.entities.reportLogs;

import com.tierconnect.riot.iot.reports.autoindex.entities.reportLogs.Header;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by achambi on 4/5/17.
 * class for test Header entities.
 */
public class HeaderTest {

    private Header header;

    @Before
    public void setUp() throws Exception {
        header = new Header();
        header.setOrigin("http://0.0.0.0:9000");
        header.setHost("127.0.0.1:8081");
        header.setUtcoffset("-240");
        header.setUserAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/57.0.2987.110 Safari/537.36");
        header.setToken("615cdfb806a0e01e88897b80648df8fe115a8709fa7aef004c9db8d6103cc077");
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void HeaderSets() throws Exception {
        assertEquals("{ \"origin\" : \"http://0.0.0.0:9000\", \"host\" : \"127.0.0.1:8081\", \"utcoffset\" : " +
                "\"-240\", \"user-agent\" : \"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/57.0.2987.110 Safari/537.36\", \"token\" : " +
                "\"615cdfb806a0e01e88897b80648df8fe115a8709fa7aef004c9db8d6103cc077\" }", header.toJson());
    }

    @Test
    public void HeaderGets() throws Exception {
        assertEquals("http://0.0.0.0:9000", header.getOrigin());
        assertEquals("127.0.0.1:8081", header.getHost());
        assertEquals("-240", header.getUtcOffset());
        assertEquals("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/57.0.2987.110 Safari/537.36", header.getUserAgent());
        assertEquals("615cdfb806a0e01e88897b80648df8fe115a8709fa7aef004c9db8d6103cc077", header.getToken());
    }
}