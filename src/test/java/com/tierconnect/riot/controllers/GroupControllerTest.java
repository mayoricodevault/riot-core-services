package com.tierconnect.riot.controllers;

import static com.tierconnect.riot.sdk.dao.QueryDSLUtils.getPath;
import static com.tierconnect.riot.utils.TestUtils.expectResponse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response.Status;

import junit.framework.Assert;
import org.apache.log4j.Logger;
import org.jboss.resteasy.plugins.server.tjws.TJWSEmbeddedJaxrsServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.thetransactioncompany.cors.HTTPMethod;
import com.tierconnect.riot.appcore.entities.QGroup;
import com.tierconnect.riot.appcore.entities.QGroupField;
import com.tierconnect.riot.utils.TestUtils;
import static com.tierconnect.riot.utils.TestUtils.validateErrorMap;
import static com.tierconnect.riot.utils.TestUtils.validateOutputMap;

public class GroupControllerTest {
	static Logger logger = Logger.getLogger(UserControllerTest.class);
	static QGroup qGroup = QGroup.group;
	static QGroupField qGroupField = QGroupField.groupField;

	private static int port = 6660;
	private static String baseUri = "http://localhost:" + port + "/group";
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
	public void testListGroup() throws Exception {
		selectGroup(Status.OK);
	}

	@Test
	public void testGetGroup() throws Exception {
		selectGroup(Status.OK, 1L);
	}

	@Test
	public void testValidateGetGroup() throws Exception {
		selectGroup(Status.BAD_REQUEST, 99999L);
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

	@Test
	public void testCreateUpdateDuplicateGroup() throws Exception {
		String suffix1 = System.currentTimeMillis() + "";
		String suffix2 = (System.currentTimeMillis() + 1) +"";
		Map<String, Object> groupMap1 = insertGroup(Status.CREATED, suffix1, 1L, 1L);
		Number id1 = (Number) groupMap1.get(getPath(qGroup.id));
		insertGroup(Status.BAD_REQUEST, suffix1, 1L, 1L);
		Map<String, Object> groupMap2 = insertGroup(Status.CREATED, suffix2, 1L, 1L);
		Number id2 = (Number) groupMap2.get(getPath(qGroup.id));

		groupMap2.put(getPath(qGroup.name), groupMap1.get(getPath(qGroup.name)));
		updateGroup(Status.BAD_REQUEST, id2, groupMap1);//Duplicated Name

		deleteGroup(Status.fromStatusCode(204), id1);
		deleteGroup(Status.fromStatusCode(204), id2);

		deleteGroup(Status.BAD_REQUEST, id1);
		deleteGroup(Status.BAD_REQUEST, id2);
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
