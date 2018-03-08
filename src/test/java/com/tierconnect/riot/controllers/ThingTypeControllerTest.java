package com.tierconnect.riot.controllers;

import static com.tierconnect.riot.utils.TestUtils.expectResponse;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jboss.resteasy.plugins.server.tjws.TJWSEmbeddedJaxrsServer;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.thetransactioncompany.cors.HTTPMethod;
import com.tierconnect.riot.utils.TestUtils;

import javax.ws.rs.core.Response;

/**
 * 
 * @author garivera
 *
 */
public class ThingTypeControllerTest {
	static Logger logger = Logger.getLogger(ThingTypeControllerTest.class);	

    private static String baseUri;
    private static TJWSEmbeddedJaxrsServer server;

	@BeforeClass
    public static void setUp() throws Exception{
		int port = 6660;
		server = TestUtils.startTestServer(port);        
        baseUri = "http://localhost:" + port + "/thingType";
    }

	@AfterClass
    public static void afterClass() throws Exception {
		TestUtils.stopServer();
    }
	
	private Map<String,Object> createThingTypeMap(String name){
	    return createThingTypeMap(name, name, null);
	}

    private Map<String,Object> createThingTypeMap(String name, Object id){
        return createThingTypeMap(name, name, id);
    }

    private Map<String,Object> createThingTypeMap(String name, String fieldName, Object id){
		Map<String,Object> thingTypeField = new HashMap<String, Object>();
		if (id != null) {
		    thingTypeField.put("id", id);
		}
		thingTypeField.put("name", fieldName);
		thingTypeField.put("unit", System.currentTimeMillis()+"");
		thingTypeField.put("symbol", System.currentTimeMillis()+"");
		
		List<Map<String,Object>> thingTypesFields = new LinkedList<Map<String,Object>>();
		thingTypesFields.add(thingTypeField);
		
		Map<String,Object> body = new HashMap<String,Object>();
		body.put("name", name);
		body.put("fields", thingTypesFields);
		body.put("group.id", 1);
		
		return body;
	}

    @Test
    public void testListThingType() throws Exception {
        expectResponse(baseUri, 200, HTTPMethod.GET, null);
    }

    @Test
    public void testGetThingType() throws Exception {
        expectResponse(baseUri+"/1", 200, HTTPMethod.GET, null);
    }

    @Test
    public void testValidateGetThingType() throws Exception {
        expectResponse(baseUri+"/99999", 400, HTTPMethod.GET, null);
    }

    @Test
    public void testCreateUpdateDeleteThingType(){
        String name = System.currentTimeMillis() + "";
        String name2 = System.currentTimeMillis() + "";
        Object id = createThingType(name, 201);
        id = updateThingType(name2, id, 200);
        deleteThingType(id, 204);
    }

    @Test
    public void testCreateDuplicatedUpdateDeleteThingType(){
        String name = System.currentTimeMillis() + "";
        String name2 = System.currentTimeMillis() + "";
        Object id = createThingType(name, 201);
        createThingType(name, 400);
        id = updateThingType(name2, id, 200);
        deleteThingType(id, 204);
    }
    
    @Test
    public void testCreateUpdateDuplicatedDeleteThingType(){
        String name = System.currentTimeMillis() + "";
        String name2 = (System.currentTimeMillis() + 1) + "";
        Object id = createThingType(name, 201);
        Object id2 = createThingType(name2, 201);
        updateThingType(name, id2, 400);
        deleteThingType(id, 204);
        deleteThingType(id2, 204);
    }

    @Test
    public void testCreateDeleteTwiceThingType(){
        String name = System.currentTimeMillis() + "";
        Object id = createThingType(name, 201);
        deleteThingType(id, 204);
        deleteThingType(id, 400);
    }
    
    @Test
    public void testParentLevelsThingType(){
        String name = System.currentTimeMillis() + "";
        String name2 = (System.currentTimeMillis() + 1) + "";
        String name3 = (System.currentTimeMillis() + 2) + "";
        Object id = createThingType(name, 201);
        Object id2 = createThingTypeParent(name2, id, 201);
        createThingTypeParent(name3, id2, 400);
        deleteThingType(id2, 204);
        deleteThingType(id, 204);
    }

    @Test
    public void testParentAndChildThingType(){
        String name = System.currentTimeMillis() + "";
        String name2 = (System.currentTimeMillis() + 1) + "";
        String name3 = (System.currentTimeMillis() + 2) + "";
        Object id = createThingType(name, 201);
        Object id2 = createThingTypeParent(name2, id, 201);
        Object id3 = createThingType(name3, 201);
        updateThingTypeParent(name, id, id3, 400);
        deleteThingType(id3, 204);
        deleteThingType(id2, 204);
        deleteThingType(id, 204);
    }

    public Object createThingType(String name, int status) {
        Map<String, Object> body = createThingTypeMap(name);
        Map<String, Object> response = expectResponse(baseUri, status, HTTPMethod.PUT, body);
        return response.get("id");
    }

    public Object createThingTypeParent(String name, Object parentId, int status) {
        Map<String, Object> body = createThingTypeMap(name);
        body.put("parent.id", parentId);
        Map<String, Object> response = expectResponse(baseUri, status, HTTPMethod.PUT, body);
        return response.get("id");
    }

    public Object updateThingType(String name, Object id, int status) {
        Map<String, Object> body = createThingTypeMap(name);
        Map<String, Object> response = expectResponse(baseUri + "/" + id, status, HTTPMethod.PATCH, body);
        return response.get("id");
    }

    public Object updateThingTypeParent(String name, Object id, Object parentId, int status) {
        Map<String, Object> body = createThingTypeMap(name);
        body.put("parent.id", parentId);
        Map<String, Object> response = expectResponse(baseUri + "/" + id, status, HTTPMethod.PATCH, body);
        return response.get("id");
    }

    public void deleteThingType(Object id, int status) {
        Map<String, Object> body = createThingTypeMap(System.currentTimeMillis() + "", id);
        expectResponse(baseUri + "/" + id, status, HTTPMethod.DELETE, body);
    }

    @Test
    public void testSelectTree() throws Exception {
        selectTree(Response.Status.OK);
        long start = System.currentTimeMillis();
        selectTree(Response.Status.OK);
        final long elapsed = System.currentTimeMillis() - start;
        //Assert.assertTrue(elapsed < 2000);
    }

    public static Map<String, Object> selectTree(Response.Status status) {
        return expectResponse(baseUri+ "/tree", status.getStatusCode(), HTTPMethod.GET, null);
    }

}
