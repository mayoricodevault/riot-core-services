package com.tierconnect.riot.visibility;

import static com.tierconnect.riot.utils.TestUtils.expectResponse;

import org.apache.log4j.Logger;
import org.jboss.resteasy.plugins.server.tjws.TJWSEmbeddedJaxrsServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.thetransactioncompany.cors.HTTPMethod;
import com.tierconnect.riot.controllers.ResourceControllerTest;
import com.tierconnect.riot.utils.TestUtils;

public class ResourceVisibilityTest {
    static Logger logger = Logger.getLogger(ResourceControllerTest.class);

    private static String baseUri;
    private static TJWSEmbeddedJaxrsServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        int port = 6600;
        server = TestUtils.startTestServer(port);
        baseUri = "http://localhost:" + port + "/resource";
    }

    @AfterClass
    public static void afterClass() {
    	TestUtils.stopServer();
    }

    @Test
    public void testListResource() {
        expectResponse(baseUri, 200, HTTPMethod.GET, null);
    }
    
    @Test
    public void testListResourceAdmin() {
        expectResponse(baseUri, 200, HTTPMethod.GET, null,"adminc");
    }
    
    @Test
    public void testListResourceUser() {
        expectResponse(baseUri, 200, HTTPMethod.GET, null,"adminf");
    }
    
    @Test
    public void testListResourceUserW() {
        expectResponse(baseUri, 200, HTTPMethod.GET, null,"userA");
    }
}

