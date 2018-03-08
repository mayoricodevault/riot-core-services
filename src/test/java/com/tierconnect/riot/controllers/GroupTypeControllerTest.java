package com.tierconnect.riot.controllers;

import static com.tierconnect.riot.sdk.dao.QueryDSLUtils.getPath;
import static com.tierconnect.riot.utils.TestUtils.expectResponse;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response.Status;

import org.apache.log4j.Logger;
import org.jboss.resteasy.plugins.server.tjws.TJWSEmbeddedJaxrsServer;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.thetransactioncompany.cors.HTTPMethod;
import com.tierconnect.riot.appcore.entities.QGroupType;
import com.tierconnect.riot.utils.TestUtils;

import static com.tierconnect.riot.utils.TestUtils.validateErrorMap;
import static com.tierconnect.riot.utils.TestUtils.validateOutputMap;

public class GroupTypeControllerTest {
	static Logger logger = Logger.getLogger(UserControllerTest.class);
	static QGroupType qGroupType = QGroupType.groupType;

	private static int port = 6660;
	private static String baseUri = "http://localhost:" + port + "/groupType";
	private static TJWSEmbeddedJaxrsServer server;

	@BeforeClass
	public static void setUp() throws Exception {
		server = TestUtils.startTestServer(port);
	}

	@AfterClass
	public static void afterClass() throws Exception {
		TestUtils.stopServer();
	}

	@Test
	public void testListGroupType() throws Exception {
		expectResponse(baseUri, 200, HTTPMethod.GET, null);
	}

	@Test
	public void testValidateGetGroupType() throws Exception {
		expectResponse(baseUri + "/99999", 400, HTTPMethod.GET, null);
	}

	@Test
	public void testCreateUpdateDeleteGroupType() throws Exception {
		String suffix = System.currentTimeMillis() + "";
		Map<String, Object> groupTypeMap = insertGroupType(Status.CREATED, suffix,  1L, 1L);
		Number id = (Number) groupTypeMap.get(getPath(qGroupType.id));
		updateGroupType(Status.OK, id, groupTypeMap);
		expectResponse(baseUri + "/"+id, 200, HTTPMethod.GET, null);
		expectResponse(baseUri+ "/"+id, 204, HTTPMethod.DELETE, null);	
		expectResponse(baseUri+ "/"+id, 400, HTTPMethod.GET, null);
	}

	public static Map<String, Object> createGroupTypeMap(String suffixId, Long parentId, Long groupId) {
		Map<String, Object> body = new HashMap<String, Object>();
		body.put(getPath(qGroupType.name), getPath(qGroupType.name)+ suffixId);
		body.put(getPath(qGroupType.code), getPath(qGroupType.code)+ suffixId);
		body.put(getPath(qGroupType.description), getPath(qGroupType.description)+ suffixId);
		body.put(getPath(qGroupType.group.id), groupId);
		body.put(getPath(qGroupType.parent.id), parentId);
		return body;
	}
	
	public static Map<String, Object> insertGroupType(Status status, String suffix, Long parentId, Long groupId) {
		Map<String, Object> groupTypeMap = createGroupTypeMap(suffix, parentId, groupId);
		Map<String, Object> response = expectResponse(baseUri, status.getStatusCode(),	HTTPMethod.PUT, groupTypeMap);		
		groupTypeMap.put(getPath(qGroupType.id), (Number) response.get(getPath(qGroupType.id)));
		if (status.getStatusCode() > 299) {
			validateErrorMap(response);
		} else {
			validateOutputMap(groupTypeMap, response);
		}
		return response;
	}
	
	public static Map<String, Object> updateGroupType(Status status, Number id, Map<String, Object> groupTypeMap) {
		Map<String, Object> response = expectResponse(baseUri+"/"+id, status.getStatusCode(), HTTPMethod.PATCH, groupTypeMap);
		if (status.getStatusCode() > 299) {
			validateErrorMap(response);
		} else {
			validateOutputMap(groupTypeMap, response);
		}
		return response;
	}

    @Test
    public void testSelectTree() throws Exception {
        selectTree(Status.OK);
        long start = System.currentTimeMillis();
        selectTree(Status.OK);
        final long elapsed = System.currentTimeMillis() - start;
        //Assert.assertTrue(elapsed < 2000);
    }

    public static Map<String, Object> selectTree(Status status) {
        return expectResponse(baseUri+ "/tree", status.getStatusCode(), HTTPMethod.GET, null);
    }


}
