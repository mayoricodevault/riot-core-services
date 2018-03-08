package com.tierconnect.riot.visibility;

import static com.tierconnect.riot.utils.TestUtils.expectResponse;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jboss.resteasy.plugins.server.tjws.TJWSEmbeddedJaxrsServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.thetransactioncompany.cors.HTTPMethod;
import com.tierconnect.riot.controllers.UserControllerTest;
import com.tierconnect.riot.utils.TestUtils;

public class UserVisibilityTest {
	static Logger logger = Logger.getLogger(UserControllerTest.class);	

    private static String baseUri;
    private static TJWSEmbeddedJaxrsServer server;

	@BeforeClass
    public static void setUp() throws Exception{
		int port = 6660;
		server = TestUtils.startTestServer(port);        
        baseUri = "http://localhost:" + port + "/user";
    }

	@AfterClass
    public static void afterClass() throws Exception {
		TestUtils.stopServer();
    }

    private Map<String,Object> createUserMap(String name) {
        Map<String,Object> body = new HashMap<String,Object>();
        body.put("username", name);
        body.put("password", name);
        body.put("apiKey", name);
        body.put("firstName", "f" + name);
        body.put("middleName", "m" + name);
        body.put("lastName", "l" + name);
        body.put("email", "e" + name);
        body.put("group.id", 1);
        
		return body;
	}
    
    /*users list*/
    
    @Test
    public void testListUser() throws Exception {
        expectResponse(baseUri, 200, HTTPMethod.GET, null);
        expectResponse(baseUri, 200, HTTPMethod.GET,null, "adminc");
        expectResponse(baseUri, 200, HTTPMethod.GET,null, "adminf");
    }
    
    /*Users list with filter*/
    
    @Test
    public void testValidateGetUser() throws Exception {
    	expectResponse(baseUri+"/999999", 400, HTTPMethod.GET, null,"root");
    }
    
    /*users list with filter*/
    
	@Test
    public void testGetUser() throws Exception {
		expectResponse(baseUri+"/1", 200, HTTPMethod.GET, null);
		expectResponse(baseUri+"/1", 403, HTTPMethod.GET,null, "adminc");
        expectResponse(baseUri+"/1", 403, HTTPMethod.GET,null, "adminf");
    }

	

   
       public Object createUser(String name) {
        Map<String, Object> body = createUserMap(name);
        Map<String, Object> response = expectResponse(baseUri, 201, HTTPMethod.PUT, body);
        return response.get("id");
    }

    public Object createDuplicatedUser(String name) {
        Map<String, Object> body = createUserMap(name);
        Map<String, Object> response = expectResponse(baseUri, 400, HTTPMethod.PUT, body);
        return response.get("id");
    }

    public Object updateUser(String name, Object id) {
        Map<String, Object> body = createUserMap(name);
        Map<String, Object> response = expectResponse(baseUri + "/" + id, 200, HTTPMethod.PATCH, body);
        return response.get("id");
    }

    public Object updateDuplicatedUser(String name, Object id) {
        Map<String, Object> body = createUserMap(name);
        Map<String, Object> response = expectResponse(baseUri + "/" + id, 400, HTTPMethod.PATCH, body);
        return response.get("id");
    }

    public void deleteUser(String name, Object id) {
        Map<String, Object> body = createUserMap(name);
        expectResponse(baseUri + "/" + id, 204, HTTPMethod.DELETE, body);
    }

}

