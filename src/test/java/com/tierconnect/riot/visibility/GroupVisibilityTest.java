package com.tierconnect.riot.visibility;

import static com.tierconnect.riot.sdk.dao.QueryDSLUtils.getPath;
import static com.tierconnect.riot.utils.TestUtils.expectResponse;
import static com.tierconnect.riot.utils.TestUtils.isEntityNameAvailableInJsonObject;
import static com.tierconnect.riot.utils.TestUtils.getNumberOfAttribsFromASingleJsonObject;
import static com.tierconnect.riot.utils.TestUtils.validateErrorMap;
import static com.tierconnect.riot.utils.TestUtils.validateOutputMap;
import static com.tierconnect.riot.utils.TestUtils.getEntityNameFromAsingleJsonObject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.ws.rs.core.Response.Status;

import org.apache.log4j.Logger;
import org.jboss.resteasy.plugins.server.tjws.TJWSEmbeddedJaxrsServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.thetransactioncompany.cors.HTTPMethod;
import com.tierconnect.riot.appcore.entities.GroupBase;
import com.tierconnect.riot.appcore.entities.QGroup;
import com.tierconnect.riot.appcore.entities.QGroupField;
import com.tierconnect.riot.controllers.UserControllerTest;
import com.tierconnect.riot.utils.TestUtils;

public class GroupVisibilityTest {
	static Logger logger = Logger.getLogger(UserControllerTest.class);
	static QGroup qGroup = QGroup.group;
	static QGroupField qGroupField = QGroupField.groupField;

	private static int port = 6660;
	private static String baseUri;
	private static TJWSEmbeddedJaxrsServer server;

	@BeforeClass
	public static void setUp() throws Exception {
		server = TestUtils.startTestServer(port);
		baseUri= "http://localhost:" + port + "/group";
	}

	@AfterClass
	public static void afterClass() throws Exception {
		TestUtils.stopServer();
	}

	

	@Test
	public void testGetGroup() throws Exception {
		
		expectResponse(baseUri+ "/", 200, HTTPMethod.GET, null,"root");
		expectResponse(baseUri+ "/", 200, HTTPMethod.GET, null,"adminc");
		expectResponse(baseUri+ "/", 200, HTTPMethod.GET, null,"adminf");
	}
	
	@Test
	public void testId(){
		///?where=id%3D1
		Map<String, Object> response=expectResponse(baseUri+"/?where=id%3D1", 200, HTTPMethod.GET, null, "root");
		
		for (Map.Entry<String, Object> entry: response.entrySet()) {
			System.out.println("key o id"+entry.getKey());
			System.out.println("\"id\":1");
			System.out.println("test paso"+entry.getValue().toString());
			CharSequence a1="\"id\":1";
			if(entry.getKey().toString().equals("results"))
				{
					System.out.println("test paso100"+entry.getValue().toString());
					
					assertTrue(entry.getValue().toString().contains("id=1"));
				}
			
		}
		
	}
	
	@Test
	public void testWhereExtraOnly(){
		//?where=id%3D1&extra=groupType&only=name
		///?where=id%3D1
				Map<String, Object> response=expectResponse(baseUri+"/?where=id%3D1&extra=groupType&only=name", 200, HTTPMethod.GET, null, "root");
				
				for (Map.Entry<String, Object> entry: response.entrySet()) {
					System.out.println("key o id"+entry.getKey());
					
					if(entry.getKey().toString().equals("results"))
						{
							System.out.println("test paso100"+entry.getValue().toString());
							
							assertTrue(entry.getValue().toString().contains("id=1") && entry.getValue().toString().contains("description=Root Group") && entry.getValue().toString().contains("name=root"));
						}
					
				}
	}
	
	@Test 
	public void only2(){
		
		
		Field[] fields=GroupBase.class.getDeclaredFields();
		for (int i = 0; i < fields.length; i++) {
			
			System.out.println("field "+fields[i].getName().toString());
		}
		
		Map<String, Object> response=expectResponse(baseUri+"/?where=id%3D1&only=billingCode", 200, HTTPMethod.GET, null, "root");	
		assertEquals(getNumberOfAttribsFromASingleJsonObject(response),1);//Verifies that the json object has just 1 attribute
		assertEquals(getEntityNameFromAsingleJsonObject(response,1),"billingCode"); //Verifies that just an specific field is present in the json objkect 
	}
	
	@Test 
	public void extra(){
		Map<String, Object> response=expectResponse(baseUri+"/?where=id%3D1&extra=groupType", 200, HTTPMethod.GET, null, "root");
		assertTrue(isEntityNameAvailableInJsonObject(response,"groupType"));
	}
	
	@Test
	public void only(){
		Map<String, Object> response=expectResponse(baseUri+"/?where=id%3D1&only=name%2Cdescription", 200, HTTPMethod.GET, null, "root");
		assertEquals(getNumberOfAttribsFromASingleJsonObject(response),2);
		assertTrue(isEntityNameAvailableInJsonObject(response,"name"));
		assertTrue(isEntityNameAvailableInJsonObject(response,"description"));
	}
	@Test
	public void testAdmin(){
		expectResponse(baseUri+ "/", 200, HTTPMethod.GET, null,"root");
	}
	
	
	@Test
	public void testValidateGetGroup() throws Exception {
		expectResponse(baseUri+ "/999999", 400, HTTPMethod.GET, null,"root");
		expectResponse(baseUri+ "/999999", 400, HTTPMethod.GET, null,"adminc");
		expectResponse(baseUri+ "/999999", 400, HTTPMethod.GET, null,"adminf");
		Thread.sleep(2000);
	}
	
	@Test 
	public void testGet() throws InterruptedException {
		
			Thread.sleep(1000);
		expectResponse(baseUri+ "/", 403, HTTPMethod.GET, null,"root");
		
		
	}
	
	@Test
	public void testCreateUpdateDeleteGroup() throws Exception {
		String suffix = (System.currentTimeMillis() + 2) + "";
		insertGroup(Status.BAD_REQUEST, suffix, null, 1L);
		Map<String, Object>  groupMap = insertGroup(Status.CREATED, suffix, 1L, 1L);
		Number id = (Number) groupMap.get(getPath(qGroup.id));
		groupMap = updateGroup(Status.OK, id, groupMap);
		groupMap.put(getPath(qGroup.parent.id), null);
		updateGroup(Status.BAD_REQUEST, id, groupMap);
		deleteGroup(Status.fromStatusCode(204), id);
		selectGroup(Status.BAD_REQUEST, id);
	}
	
	public static int nroElementos(String query,int i)
	{int tot=0;
	Map.Entry<String, Object> sol=null;
    Map<String, Object> response=expectResponse(baseUri+"/"+query, 200, HTTPMethod.GET, null, "root");		
	for (Map.Entry<String, Object> entry: response.entrySet()) {
		System.out.println("key o id"+entry.getKey());
		
		if(entry.getKey().toString().equals("results"))
			{
				System.out.println("test paso100"+entry.getValue().toString());
				String d=entry.getValue().toString();
				d=d.replace('{', ' ');
				d=d.replace('}', ' ');
				d=d.replace('[', ' ');
				d=d.replace(']', ' ');
				d=d.replace(" , ",":");
				//d=d.replace(',', ' ');
				 sol=entry;
				StringTokenizer b=new StringTokenizer(d,":");
				System.out.println(b.countTokens()+" "+d);
				tot=b.countTokens();
				
				
			}
		
	}	
	
	return tot;
	}
	
	public static Map<String, Object> createGroupMap(String suffixId, Long parentId, Long groupTypeId) {
		Map<String, Object> body = new HashMap<String, Object>();
		body.put(getPath(qGroup.code), getPath(qGroup.code) + suffixId);
		body.put(getPath(qGroup.description), getPath(qGroup.description)+ suffixId);
		body.put(getPath(qGroup.name), getPath(qGroup.name) + suffixId);
		body.put(getPath(qGroup.archived), false);
		body.put(getPath(qGroup.parent.id), parentId);
		body.put(getPath(qGroup.groupType.id), groupTypeId);
		return body;
	}
	
	public static Map<String, Object> insertGroup(Status status, String suffix, Long parentId, Long groupTypeId) {
		Map<String, Object> groupMap = createGroupMap(suffix, parentId, groupTypeId);
		Map<String, Object> response = expectResponse(baseUri, status.getStatusCode(),	HTTPMethod.PUT, groupMap);		
		groupMap.put(getPath(qGroup.id), (Number) response.get(getPath(qGroup.id)));
		if (status.getStatusCode() > 299) {
			validateErrorMap(response);
		} else {
			validateOutputMap(groupMap, response);
		}
		return response;
	}
	
	public static Map<String, Object> updateGroup(Status status, Number id, Map<String, Object> groupMap) {
		Map<String, Object> response = expectResponse(baseUri+"/"+id, status.getStatusCode(), HTTPMethod.PATCH, groupMap);
		if (status.getStatusCode() > 299) {
			validateErrorMap(response);
		} else {
			validateOutputMap(groupMap, response);
		}
		return response;
	}

	public static Map<String, Object> selectGroup(Status status, Number id) {
		return expectResponse(baseUri+ "/"+id, status.getStatusCode(), HTTPMethod.GET, null);
	}

	public static Map<String, Object> selectGroup(Status status) {
		return expectResponse(baseUri+ "/", status.getStatusCode(), HTTPMethod.GET, null);
	}

    @Test
    public void testSelectTree() throws Exception {
        selectTree(Status.OK);
        long start = System.currentTimeMillis();
        selectTree(Status.OK);
        final long elapsed = System.currentTimeMillis() - start;
        //org.junit.Assert.assertTrue(elapsed < 2000);
    }

    public static Map<String, Object> selectTree(Status status) {
        return expectResponse(baseUri+ "/tree", status.getStatusCode(), HTTPMethod.GET, null);
    }

	public static Map<String, Object> deleteGroup(Status status, Number id) {
		return expectResponse(baseUri+ "/"+id, status.getStatusCode(), HTTPMethod.DELETE, null);
	}
	
}

