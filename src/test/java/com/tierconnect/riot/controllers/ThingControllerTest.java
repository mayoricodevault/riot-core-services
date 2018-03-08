/**
 * @author grea
 */
package com.tierconnect.riot.controllers;

import static com.tierconnect.riot.utils.TestUtils.expectResponse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.jboss.resteasy.plugins.server.tjws.TJWSEmbeddedJaxrsServer;
import org.junit.*;

import com.thetransactioncompany.cors.HTTPMethod;
//import com.tierconnect.riot.iot.mqtt.RiotConfiguration;
import com.tierconnect.riot.utils.TestUtils;

public class  ThingControllerTest{
	static Logger logger = Logger.getLogger(ThingControllerTest.class);	

    private static String baseUri;
    private static TJWSEmbeddedJaxrsServer server;

	@BeforeClass
    public static void setUp(){
		int port = 6660;
		server = TestUtils.startTestServer(port);        
        baseUri = "http://localhost:" + port + "/thing";
    }

	@AfterClass
    public static void afterClass() {
		TestUtils.stopServer();
    }

	private Map<String,Object> createThingMap(String thingName){
		return createThingMap(thingName, null);
	}
	
	private Map<String, Object> createThingMap(String thingName, Long parentId) {
		Map<String,Object> deviceField = new HashMap<String, Object>();
		deviceField.put("name", System.currentTimeMillis()+"");
		deviceField.put("unit", System.currentTimeMillis()+"");
		deviceField.put("symbol", System.currentTimeMillis()+"");
		deviceField.put("value", System.currentTimeMillis()+"");
		
		List<Map<String,Object>> deviceFields = new LinkedList<Map<String,Object>>();
		deviceFields.add(deviceField);
		
		Map<String,Object> body = new HashMap<String,Object>();
		body.put("name", thingName);
		body.put("serial", System.currentTimeMillis()+"");
		body.put("fields", deviceFields);
		body.put("parent.id",parentId);
		body.put("group.id", 1);

		return body;
	}

	@Test
    public void testListThing() {
		expectResponse(baseUri, 200,HTTPMethod.GET,null);
    }
	
	@Test
	public void testListThingPlusFields(){
		Map<String,Object> results = expectResponse(baseUri+"?extra=group,parent", 200,HTTPMethod.GET,null);
		List<Map<String,Object>> list = (List<Map<String, Object>>) results.get("results");
		
		assertTrue(list.get(0).containsKey("group"));
		assertTrue(list.get(0).containsKey("parent"));
	}

	@Test
    public void testGetThing() {
		expectResponse(baseUri+"/1", 200,HTTPMethod.GET,null);
    }
	@Test
	public void testGetThingPlusFields(){
		Map<String,Object> thing;

		thing = expectResponse(baseUri+"/1?extra=group", 200,HTTPMethod.GET,null);
		assertTrue(thing.containsKey("group"));
		thing = expectResponse(baseUri+"/1?extra=group,", 200,HTTPMethod.GET,null);
		assertTrue(thing.containsKey("group"));
		thing = expectResponse(baseUri+"/1?extra=parent", 200,HTTPMethod.GET,null);
		assertTrue(thing.containsKey("parent"));
		thing = expectResponse(baseUri+"/1?extra=group,parent", 200,HTTPMethod.GET,null);
		assertTrue(thing.containsKey("group"));
		assertTrue(thing.containsKey("parent"));
		thing = expectResponse(baseUri+"/1?extra=group.groupType", 200,HTTPMethod.GET,null);
		assertTrue(thing.containsKey("group.groupType"));
	}
	@Test
	public void testGetThingPlusInvalidFields(){
		expectResponse(baseUri+"/1?extra=groupqwe", 400,HTTPMethod.GET,null);
		expectResponse(baseUri+"/1?extra=group,parentqweqwe", 400,HTTPMethod.GET,null);
	}

	@Test
    public void testValidateGetThing() {
		expectResponse(baseUri+"/99999", 400,HTTPMethod.GET,null);
		expectResponse(baseUri+"/-1", 400,HTTPMethod.GET,null);
    }
	
	@Test
	public void testValidateCreateThing() {
		//Creating duplicated field names
		Map<String,Object> thingMap1 = createThingMap(System.currentTimeMillis()+"");
		List<Map<String,String>> deviceFields = (List<Map<String, String>>) thingMap1.get("fields");

		Map<String,String> deviceField = new HashMap<String, String>();
		deviceField.put("name", deviceFields.get(0).get("name"));//Same name as the first field
		deviceField.put("unit", System.currentTimeMillis()+"");
		deviceField.put("symbol", System.currentTimeMillis()+"");
		deviceField.put("value", System.currentTimeMillis()+"");
		deviceFields.add(deviceField);

		List<Map<String,Object>> invalidBodies = new LinkedList<Map<String,Object>>();

		invalidBodies.add(createThingMap(""));
		invalidBodies.add(createThingMap(null));
		invalidBodies.add(thingMap1);
		for(Map<String,Object> invalidBody : invalidBodies){
			expectResponse(baseUri, 400,HTTPMethod.PUT,invalidBody);
		}
	}

	@Test
	@Ignore
	public void testDuplicatedNamesOnCreate(){
		String thingName = System.currentTimeMillis()+"";		
		expectResponse(baseUri, 201,HTTPMethod.PUT,createThingMap(thingName));
		expectResponse(baseUri, 400,HTTPMethod.PUT,createThingMap(thingName));
	}
	
	@Test
	@Ignore
	public void testDuplicatedNamesOnUpdate(){
		String thingNameA = System.currentTimeMillis()+"A";
		String thingNameB = System.currentTimeMillis()+"B";
		expectResponse(baseUri, 201,HTTPMethod.PUT,createThingMap(thingNameA));
		Map<String,Object> thingMap = expectResponse(baseUri, 201,HTTPMethod.PUT,createThingMap(thingNameB));

		int idB = (int) thingMap.get("id");
		expectResponse(baseUri+"/"+idB, 400,HTTPMethod.PATCH,createThingMap(thingNameA));
		expectResponse(baseUri+"/"+idB, 400,HTTPMethod.PATCH,createThingMap(""));
	}
	
	@Test
	public void testTreeLevelHigherThanTwo(){
		String thingNameA = System.currentTimeMillis()+"A";
		String thingNameB = System.currentTimeMillis()+"B";
		String thingNameC = System.currentTimeMillis()+"C";
		
		Map<String,Object> thingMapA = expectResponse(baseUri, 201,HTTPMethod.PUT,createThingMap(thingNameA));
		Map<String,Object> thingMapB = expectResponse(baseUri, 201,HTTPMethod.PUT,createThingMap(thingNameB,((Integer)thingMapA.get("id")).longValue()));
		expectResponse(baseUri, 400,HTTPMethod.PUT,createThingMap(thingNameC,((Integer)thingMapB.get("id")).longValue()));
	}	

	@Test
	public void testCreateThing(){
		Map<String,Object> body = createThingMap(System.currentTimeMillis()+"");

		Map<String,Object> thingA = expectResponse(baseUri, 201,HTTPMethod.PUT,body);
		assertEquals(thingA.get("name"),body.get("name"));
		int id = (int) thingA.get("id");

		Map<String,Object> thingB = expectResponse(baseUri+"/"+id, 200,HTTPMethod.GET,null);
		assertEquals(thingA.get("name"),thingB.get("name"));
		assertEquals(thingA.get("serial"),thingB.get("serial"));
		assertEquals(thingA.get("id"),thingB.get("id"));
	}

	@Test
	public void testUpdateThing(){
		Map<String,Object> body = createThingMap(System.currentTimeMillis()+"");
		
		Map<String,Object> thingA = expectResponse(baseUri, 201,HTTPMethod.PUT,body);
		assertEquals(thingA.get("name"),body.get("name"));
		int id = (int) thingA.get("id");
		
		expectResponse(baseUri+"/"+id, 200,HTTPMethod.PATCH,createThingMap(System.currentTimeMillis()+""));

		Map<String,Object> thingB = expectResponse(baseUri+"/"+id, 200,HTTPMethod.PATCH);
		assertNotEquals(thingA.get("name"),thingB.get("name"));
		assertNotEquals(thingA.get("serial"),thingB.get("serial"));
		assertEquals(thingA.get("id"),thingB.get("id"));
	}

	@Test
	public void testActivate(){
		Map<String,Object> thingMap = expectResponse(baseUri, 201,HTTPMethod.PUT,createThingMap(System.currentTimeMillis()+""));		
		int id = (int) thingMap.get("id");

		expectResponse(baseUri+"/"+id+"/activate", 200,HTTPMethod.POST,null);
	}

	@Test
	public void testDeactivate(){
		Map<String,Object> thingMap = expectResponse(baseUri, 201,HTTPMethod.PUT,createThingMap(System.currentTimeMillis()+""));
		int id = (int) thingMap.get("id");

		expectResponse(baseUri+"/"+id+"/deactivate", 200,HTTPMethod.POST,null);
	}
	
	/*
	@Test
	public void testPushValues(){
		String newValue = System.currentTimeMillis()+"";
		Map<String,Object> datapointMap = new HashMap<String,Object>();
		datapointMap.put("value", newValue);

		Map<String,Object> thingMap = expectResponse(baseUri, 201,HTTPMethod.PUT,createThingMap(System.currentTimeMillis()+""));
		int thingId = (int) thingMap.get("id");

		assertTrue(thingMap.containsKey("fields"));
		assertTrue(((List<Map<String,Object>>)thingMap.get("fields")).size() > 0);
		int fieldId = (int) ((List<Map<String,Object>>)thingMap.get("fields")).get(0).get("id");
		
		expectResponse(baseUri+"/"+thingId+"/field/"+fieldId, 200,HTTPMethod.POST,datapointMap);
		
		thingMap = expectResponse(baseUri+"/"+thingId, 200,HTTPMethod.GET);
		assertTrue(thingMap.containsKey("fields"));
		assertTrue(((List<Map<String,Object>>)thingMap.get("fields")).size() > 0);
		String mapValue = (String) ((List<Map<String,Object>>)thingMap.get("fields")).get(0).get("value");
		
		assertEquals(newValue, mapValue);
	}
	*/

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

    @Test
    public void testRuleValidation() {
        Map<String, Object> thingMap = createThingMap(System.currentTimeMillis() + "");
        List<Map<String, Object>> fields = new LinkedList<Map<String,Object>>();
        for (int i = 0; i < 5; i++) {
            Map<String, Object> field = new HashMap<String, Object>();
            field.put("name", "field" + (i + 1));
            field.put("unit", System.currentTimeMillis() + "");
            field.put("symbol", System.currentTimeMillis() + "");
            field.put("value", "1");
            fields.add(field);
        }
        thingMap.put("fields", fields);
        Map<String, Object> thing = expectResponse(baseUri, 201, HTTPMethod.PUT, thingMap);
        Object thingId = thing.get("id");
        fields = (List<Map<String,Object>>)thing.get("fields");
        Object field1Id = fields.get(0).get("id");
        String luaValidCode = "return thing.field1 == '0'";
        String luaInvalidCode = "return thing.field1";
        String validateUrl = baseUri + String.format("/%d/%d/validate", thingId, field1Id);
        Map<String, Object> validRuleMap = new HashMap<>();
        validRuleMap.put("rule", luaValidCode);
        Map<String, Object> validResponse = expectResponse(validateUrl, 200, HTTPMethod.POST, validRuleMap);
        assertEquals(true, validResponse.get("result"));
        Map<String, Object> invalidRuleMap = new HashMap<>();
        invalidRuleMap.put("rule", luaInvalidCode);
        Map<String, Object> invalidResponse = expectResponse(validateUrl, 200, HTTPMethod.POST, invalidRuleMap);
        assertEquals(false, invalidResponse.get("result"));
    }

}
