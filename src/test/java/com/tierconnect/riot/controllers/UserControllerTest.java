package com.tierconnect.riot.controllers;

import static com.tierconnect.riot.utils.TestUtils.expectResponse;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jboss.resteasy.plugins.server.tjws.TJWSEmbeddedJaxrsServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.thetransactioncompany.cors.HTTPMethod;
import com.tierconnect.riot.utils.TestUtils;

public class UserControllerTest {
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

    @Test
    public void testListUser() throws Exception {
        expectResponse(baseUri, 200, HTTPMethod.GET, null);
    }

	@Test
    public void testGetUser() throws Exception {
		expectResponse(baseUri+"/1", 200, HTTPMethod.GET, null);
    }

	@Test
    public void testValidateGetUser() throws Exception {
		expectResponse(baseUri+"/99999", 400, HTTPMethod.GET, null);
    }

    @Test
    public void testCreateUpdateDeleteUser(){
        String name = System.currentTimeMillis() + "";
        Object id = createUser(name);
        id = updateUser(name, id);
        deleteUser(name, id);
    }

    @Test
    public void testCreateDuplicatedUpdateDeleteUser(){
        String name = System.currentTimeMillis() + "";
        Object id = createUser(name);
        createDuplicatedUser(name);
        id = updateUser(name, id);
        deleteUser(name, id);
    }

    @Test
    public void testCreateUpdateDuplicatedDeleteUser(){
        String name = System.currentTimeMillis() + "";
        String name2 = (System.currentTimeMillis() + 1) + "";
        Object id = createUser(name);
        Object id2 = createUser(name2);
        updateDuplicatedUser(name, id2);
        deleteUser(name, id);
        deleteUser(name2, id2);
    }
    
    @Test
    public void testInsertUserWithCorrectShift(){
    	String name = System.currentTimeMillis() + "";
    	Short start = 10;
    	Short end = 20;
    	Object id = createUserWithCorrectShift(name,start,end);
    }
    
    @Test
    public void testInsertUserWithBadRangeShift(){
    	String name = System.currentTimeMillis() + "";
    	Short start = 20;
    	Short end = 10;
    	Object id = createUserWithBadShift(name,start,end);
    }
    
    @Test
    public void testInsertUserWithNullShift(){
    	String name = System.currentTimeMillis() + "";
    	Short start = null;
    	Short end = 10;
    	Object id = createUserWithBadShift(name,start,end);
    }
    
    @Test
    public void testUpdateUserWithCorrectShift(){
    	String name = System.currentTimeMillis() + "";
    	Object id = createUser(name);
    	Short start = 10;
    	Short end = 20;
    	updateUserWithCorrectShift(id, start, end);
    	deleteUser(name,id);
    }
    
    @Test
    public void testUpdateUserWithBadRangeShift(){
    	String name = System.currentTimeMillis() + "";
    	Object id = createUser(name);
    	Short start = 20;
    	Short end = 10;
    	updateUserWithBadShift(id, start, end);
    	deleteUser(name,id);
    }
    
    @Test
    public void testUpdateUserWithNullShift(){
    	String name = System.currentTimeMillis() + "";
    	Object id = createUser(name);
    	Short start = 10;
    	Short end = null;
    	updateUserWithBadShift(id, start, end);
    	deleteUser(name,id);
    }
    
    public Object createUserWithCorrectShift(String name,Short start,Short end){
    	Map<String, Object> body = createUserMap(name);
    	body.put("mondayStart",start);
    	body.put("mondayEnd",end);
        Map<String, Object> response = expectResponse(baseUri, 201, HTTPMethod.PUT, body);
        return response.get("id");
    }
    
    public Object createUserWithBadShift(String name,Short start,Short end){
    	Map<String, Object> body = createUserMap(name);
    	body.put("mondayStart",start);
    	body.put("mondayEnd",end);
        Map<String, Object> response = expectResponse(baseUri, 400, HTTPMethod.PUT, body);
        return response.get("id");
    }
    
    public Object updateUserWithCorrectShift(Object id,Short start,Short end){
    	Map<String, Object> body = new HashMap<>();
    	body.put("mondayStart",start);
    	body.put("mondayEnd",end);
    	Map<String, Object> response = expectResponse(baseUri + "/" + id, 200, HTTPMethod.PATCH, body);
        return response.get("id");
    }
    
    public Object updateUserWithBadShift(Object id,Short start,Short end){
    	Map<String, Object> body = new HashMap<>();
    	body.put("mondayStart",start);
    	body.put("mondayEnd",end);
    	Map<String, Object> response = expectResponse(baseUri + "/" + id, 400, HTTPMethod.PATCH, body);
        return response.get("id");
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
