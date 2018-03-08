package com.tierconnect.riot.controllers;

/**
 * Created by franco on 23-04-14.
 */

import static com.tierconnect.riot.utils.TestUtils.expectResponse;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jboss.resteasy.plugins.server.tjws.TJWSEmbeddedJaxrsServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.thetransactioncompany.cors.HTTPMethod;
import com.tierconnect.riot.appcore.controllers.ResourceController;
import com.tierconnect.riot.utils.TestUtils;

public class ResourceControllerTest {

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
}
