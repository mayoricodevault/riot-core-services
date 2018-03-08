package com.tierconnect.riot.controllers;

import static com.tierconnect.riot.sdk.dao.QueryDSLUtils.getPath;
import static com.tierconnect.riot.utils.TestUtils.expectResponse;
import static com.tierconnect.riot.utils.TestUtils.validateErrorMap;
import static com.tierconnect.riot.utils.TestUtils.validateOutputMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.IdClass;
import javax.ws.rs.core.Response.Status;

import com.tierconnect.riot.appcore.entities.*;
import junit.framework.Assert;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.jboss.resteasy.plugins.server.tjws.TJWSEmbeddedJaxrsServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.thetransactioncompany.cors.HTTPMethod;
import com.tierconnect.riot.appcore.dao.GroupDAO;
import com.tierconnect.riot.appcore.services.ConfigurationService;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;
import com.tierconnect.riot.utils.TestUtils;

public class ConfigurationTest {

	static QGroup qGroup = QGroup.group;
    static QRole qRole = QRole.role;
    static QResource qResource = QResource.resource;
    static QUser qUser = QUser.user;
	static QGroupField qGroupField = QGroupField.groupField;
	static QUserField qUserField = QUserField.userField;
	static QField qField = QField.field;

	private static String baseUri;
	private static TJWSEmbeddedJaxrsServer server;
	private static int port = 6660;

	@BeforeClass
	public static void setUp() throws Exception {
		server = TestUtils.startTestServer(port);
		baseUri = "http://localhost:" + port;
	}

	@AfterClass
	public static void afterClass() throws Exception {
		TestUtils.stopServer();
	}
	
// TODO garivera: fix and enable test
//	@Test
	public void testCreateDeleteGroupFields() throws Exception {
		String suffix = System.currentTimeMillis() + "";
		Map<String, Object> groupMap = GroupControllerTest.insertGroup(Status.CREATED, suffix, 1L, 1L);
		Number groupId = (Number) groupMap.get(getPath(qGroup.id));
		
		insertGroupField(Status.CREATED, groupId, 1L, "A");
		insertGroupField(Status.CREATED, groupId, 1L, "D");
		insertGroupField(Status.CREATED, groupId, 1L, "A");
		
		readGroupField(Status.OK, groupId, 1L, "A");
		readDerivedGroupField(Status.OK, groupId, 1L, "A");		

		suffix = System.currentTimeMillis() + "";
		
		Map<String, Object> groupTypeMap = GroupTypeControllerTest.insertGroupType(Status.CREATED,suffix,  1L, 1L);
		
		Map<String, Object> groupMapChild = GroupControllerTest.insertGroup(Status.CREATED, suffix, groupId.longValue(), ((Number)groupTypeMap.get("id")).longValue());
		Number groupChildId = (Number) groupMapChild.get(getPath(qGroup.id));

		readGroupField(Status.OK, groupChildId, 1L, null);

		readDerivedGroupField(Status.OK, groupChildId, 1L, "A");
		
		insertGroupField(Status.CREATED, groupChildId, 1L, "B");

		readGroupField(Status.OK, groupChildId, 1L, "B");

		readDerivedGroupField(Status.OK, groupChildId, 1L, "B");

		deleteGroupField(Status.fromStatusCode(204), groupChildId, 1L);
		
		readGroupField(Status.OK, groupChildId, 1L, null);

		readDerivedGroupField(Status.OK, groupChildId, 1L, "A");
				
	}

// TODO garivera: fix and enable test
//	@Test
	public void testCreateDeleteUserFields() throws Exception {
		deleteGroupField(Status.fromStatusCode(204), 1L, 1L);
		insertUserField(Status.CREATED, 1L, 1L, "A");
		readUserFields(Status.OK, 1L, 1L, "A");
		readDerivedUserFields(Status.OK, 1L, 1L, "A");
		deleteUserField(Status.fromStatusCode(204), 1L, 1L);		
		readUserFields(Status.OK, 1L, 1L, null);
		readDerivedUserFields(Status.OK, 1L, 1L, null);
		insertGroupField(Status.CREATED, 1L, 1L, "G");
		readUserFields(Status.OK, 1L, 1L, null);
		readDerivedUserFields(Status.OK, 1L, 1L, "G");
		insertUserField(Status.CREATED, 1L, 1L, "U");
		readUserFields(Status.OK, 1L, 1L, "U");
		readDerivedUserFields(Status.OK, 1L, 1L, "U");
	}

// TODO garivera: fix and enable test
//	@Test
	public void testConfiguration() throws Exception {
		String suffix = System.currentTimeMillis() + "";
		Map<String, Object> groupMap = GroupControllerTest.insertGroup(Status.CREATED, suffix, 1L, 1L);
		Number groupId = (Number) groupMap.get(getPath(qGroup.id));

		insertGroupField(Status.CREATED, groupId, 1L, "A");

		Session session = HibernateSessionFactory.getInstance().getCurrentSession();
		Transaction transaction = session.getTransaction();
		transaction.begin();

		GroupDAO a = new GroupDAO();
		Group group = a.selectById(groupId.longValue());
		System.out.println("AA"+ConfigurationService.getInstance().getAsStringMap(group));
		transaction.commit();

	}



// TODO garivera: fix and enable test
//	@Test
    public void testConfigurationPermissioning() throws Exception {
        String suffix = System.currentTimeMillis()+"";
        String suffix2 = System.currentTimeMillis()+2+"";
        // create user testUser with root user
        Map<String, Object> testUser = insertUser(Status.CREATED, suffix, 1L);
        Long testUserId = ((Number) testUser.get("id")).longValue();

        Map<String, Object> testUser2 = insertUser(Status.CREATED, suffix2, 1L);
        Long testUserId2 = ((Number) testUser2.get("id")).longValue();

        // create a userConfiguration for field 1L
        insertUserField(Status.CREATED, testUserId, 1L, "A");
        insertUserField(Status.CREATED, testUserId2, 1L, "B");
        // login as testUser user
        try {
            TestUtils.apiKey.set((String) testUser.get("apiKey"));

            // try to read userConfiguration it should pass because of DEFAULT PERMISSIONING
            readUserField(Status.OK, testUserId, 1L, "A");
            readUserFields(Status.OK, testUserId, 1L, "A");
            readDerivedUserFields(Status.OK, testUserId, 1L, "A");

            // try to read other userConfiguration it should fail
            readUserField(Status.FORBIDDEN, testUserId2, 1L, null);
            readUserFields(Status.FORBIDDEN, testUserId2, 1L, null);
            readDerivedUserFields(Status.FORBIDDEN, testUserId2, 1L, null);

            // try to set userConfiguration it should fail
            insertUserField(Status.FORBIDDEN, testUserId, 1L, "A2");
            insertUserField(Status.FORBIDDEN, testUserId2, 1L, "B2");

        } finally {
            TestUtils.apiKey.set(null);
        }

        // create role testRole with root user
        Map<String, Object> testRole = insertRole(Status.CREATED, suffix, 1L, 1L);
        Map<String, Object> resource = selectResource(Status.OK, "$userConfiguration");
        // add permission "$userConfiguration to role
        assignResourceToRole(Status.CREATED, ((Number) testRole.get("id")).longValue(), ((Number)resource.get("id")).longValue(), "rw", testUserId);
        // assign role to user
        assignRoleToUser(Status.OK, testUserId, ((Number) testRole.get("id")).longValue());

        try {
            TestUtils.apiKey.set((String) testUser.get("apiKey"));

            // try to read userConfiguration it should pass because of DEFAULT PERMISSIONING
            readUserField(Status.OK, testUserId, 1L, "A");
            readUserFields(Status.OK, testUserId, 1L, "A");
            readDerivedUserFields(Status.OK, testUserId, 1L, "A");

            // try to read other userConfiguration it should fail
            readUserField(Status.FORBIDDEN, testUserId2, 1L, null);
            readUserFields(Status.FORBIDDEN, testUserId2, 1L, null);
            readDerivedUserFields(Status.FORBIDDEN, testUserId2, 1L, null);

            // try to set userConfiguration
            insertUserField(Status.CREATED, testUserId, 1L, "A2");
            insertUserField(Status.FORBIDDEN, testUserId2, 1L, "B2");



            // try to read userConfiguration it should pass
            readUserField(Status.OK, testUserId, 1L, "A2");
            readUserFields(Status.OK, testUserId, 1L, "A2");
            readDerivedUserFields(Status.OK, testUserId, 1L, "A2");
            // try to read other userConfiguration it should fail
            readUserField(Status.FORBIDDEN, testUserId2, 1L, null);
            readUserFields(Status.FORBIDDEN, testUserId2, 1L, null);
            readDerivedUserFields(Status.FORBIDDEN, testUserId2, 1L, null);
            // try to set userConfiguration it should pass
            insertUserField(Status.CREATED, testUserId, 1L, "A2");
            // try to set other userConfiguration it should fail
            insertUserField(Status.FORBIDDEN, testUserId2, 1L, "B2");
            // try to unset userConfiguration it should pass
            // TODO
            // try to unset other userConfiguration it should fail
            // TODO

        } finally {
            TestUtils.apiKey.set(null);
        }
    }

	public static Map<String, Object> insertGroupField(Status status, Number groupId, Number fieldId, String value) {
		Map<String, Object> fieldMap = new HashMap<String, Object>();
		fieldMap.put(getPath(qGroupField.value), value);
		Map<String, Object> response2 = expectResponse(baseUri+"/group/"+groupId+"/field/"+fieldId, status.getStatusCode(), HTTPMethod.PUT, fieldMap);
		TestUtils.validateOutputMap(fieldMap, response2);
		return response2;
	}


	public static Map<String, Object> readGroupField(Status status,	Number groupId, Number fieldId, String value) {
		Map<String, Object> response = expectResponse(baseUri + "/group/" + groupId+ "/fields/", status.getStatusCode(), HTTPMethod.GET, null);
		findFieldValue(fieldId, value, response);
		return response;
	}

	public static Map<String, Object> readDerivedGroupField(Status status, Number groupId, Number fieldId, String value) {
		Map<String, Object> response3 = expectResponse(baseUri + "/group/" + groupId+ "/inheritedFields/", status.getStatusCode(), HTTPMethod.GET, null);
		findFieldValue(fieldId, value, response3);
		return response3;
	}

	private static void deleteGroupField(Status expectedStatus, Number groupId, long fieldId) {
		expectResponse(baseUri+"/group/"+groupId+"/field/"+fieldId, expectedStatus.getStatusCode(), HTTPMethod.DELETE, null);
	}

	public static Map<String, Object> insertUserField(Status status, Number userId, Number fieldId, String value) {
		Map<String, Object> fieldMap = new HashMap<String, Object>();
		fieldMap.put(getPath(qUserField.value), value);
		Map<String, Object> response2 = expectResponse(baseUri+"/user/"+userId+"/field/"+fieldId, status.getStatusCode(), HTTPMethod.PUT, fieldMap);
        if (status.getStatusCode() < 299) {
            TestUtils.validateOutputMap(fieldMap, response2);
        }
		return response2;
	}

    public static void readUserField(Status status, Number userId, Number fieldId, String value) {
        Map<String, Object> response = expectResponse(baseUri + "/user/" + userId + "/field/" + fieldId, status.getStatusCode(), HTTPMethod.GET, null);
        if (status.getStatusCode() == 200) {
            Assert.assertEquals(response.get("value"), value);
        }
    }


	public static Map<String, Object> readUserFields(Status status, Number userId, Number fieldId, String value) {
		Map<String, Object> response = expectResponse(baseUri + "/user/" + userId+ "/fields/", status.getStatusCode(), HTTPMethod.GET, null);
        if (value != null) {
            findFieldValue(fieldId, value, response);
        }
		return response;
	}

	public static Map<String, Object> readDerivedUserFields(Status status, Number userId, Number fieldId, String value) {
		Map<String, Object> response3 = expectResponse(baseUri + "/user/" + userId+ "/inheritedFields/", status.getStatusCode(), HTTPMethod.GET, null);
        if (value != null) {
            findFieldValue(fieldId, value, response3);
        }
		return response3;
	}

	private static void deleteUserField(Status expectedStatus, Number userId, long fieldId) {
		expectResponse(baseUri+"/user/"+userId+"/field/"+fieldId, expectedStatus.getStatusCode(), HTTPMethod.DELETE, null);
	}

	private static void findFieldValue(Number fieldId, String expectedValue,
			Map<String, Object> response) {
		List<Map> list = (List<Map>) response.get("results");
		boolean found = false;
		for (Map object : list) {
			Map field = (Map) object.get(getPath(qGroupField.field));
			if (fieldId.longValue() == ((Number) field.get(getPath(qField.id))).longValue()) {
                Object realValue = object.get(getPath(qGroupField.value));
				if (expectedValue != null) {
				   found = true;
				   assertEquals(expectedValue, realValue);
				} else {
				   found = true;
				   fail("The expectedValue for fieldId:"+fieldId +" was found with value:"+realValue+" in:"+object);
				}
			}
		}
	    if (expectedValue != null && !found) {
			fail("The expectedValue for fieldId:"+fieldId +" was not found");
		}
	}

    public static Map<String, Object> insertRole(Status status, String suffix, Long groupId, Long groupTypeId) {
        Map<String, Object> roleMap = createRoleMap(suffix, groupId, groupTypeId);
        Map<String, Object> response = expectResponse(baseUri+"/role/", status.getStatusCode(),	HTTPMethod.PUT, roleMap);
        roleMap.put(getPath(qGroup.id), (Number) response.get(getPath(qGroup.id)));
        if (status.getStatusCode() > 299) {
            validateErrorMap(response);
        } else {
            validateOutputMap(roleMap, response, "code");
        }
        return response;
    }


    public static Map<String, Object> insertUser(Status status, String suffix, Long groupId) {
        Map<String, Object> userMap = createUserMap(suffix, groupId);
        Map<String, Object> response = expectResponse(baseUri+"/user/", status.getStatusCode(),	HTTPMethod.PUT, userMap);
        userMap.put(getPath(qGroup.id), (Number) response.get(getPath(qGroup.id)));
        if (status.getStatusCode() > 299) {
            validateErrorMap(response);
        } else {
            validateOutputMap(userMap, response, "password", "apiKey");
        }
        return response;
    }

    public static Map<String, Object> createRoleMap(String suffixId, Long groupId, Long groupTypeId) {
        Map<String, Object> body = new HashMap<String, Object>();
//        body.put(getPath(qRole.code), getPath(qRole.code) + suffixId);
        body.put(getPath(qRole.description), getPath(qRole.description)+ suffixId);
        body.put(getPath(qRole.name), getPath(qRole.name) + suffixId);
        body.put(getPath(qRole.archived), false);
        body.put(getPath(qRole.group.id), groupId);
        body.put(getPath(qRole.groupTypeCeiling.id), groupTypeId);
        return body;
    }

    public static Map<String, Object> createUserMap(String suffixId, Long groupId) {
        Map<String, Object> body = new HashMap<String, Object>();
        body.put(getPath(qUser.email), getPath(qUser.email) + suffixId);
        body.put(getPath(qUser.firstName), getPath(qUser.firstName)+ suffixId);
        body.put(getPath(qUser.lastName), getPath(qUser.lastName) + suffixId);
        body.put(getPath(qUser.username), getPath(qUser.username) + suffixId);
        //???
        //body.put(getPath(qUser.password), getPath(qUser.password) + suffixId);
        //???
        body.put(getPath(qUser.apiKey), getPath(qUser.apiKey) + suffixId);
        body.put(getPath(qUser.archived), false);
        body.put(getPath(qUser.group.id), groupId);
        return body;
    }

    public static Map<String, Object>  selectResource(Status status, String resourceName) {
        Map<String, Object> response = expectResponse(baseUri + "/resource/?where=name%3D"+resourceName, status.getStatusCode(), HTTPMethod.GET, null);
        if (status.getStatusCode() != 200) {
            fail(""+status.getStatusCode());
        }
        List<Map> list2 = (List<Map>) response.get("results");
        List<Map> list = (List<Map>) list2.get (0).get("children");
        for (Map resource : list) {
            String name = (String) resource.get(getPath(qResource.name));
            if (name.equals(resourceName)) {
                return resource;
            }
        }
        return null;
    }

    private static Map<String, Object> assignResourceToRole(Status status, Long roleId, Long resourceId, String permissions, Long objectId) {
        Map<String,Object> map = new HashMap<>();
        map.put("permissions", permissions);
        map.put("objectId", objectId);
        Map<String, Object> response2 = expectResponse(baseUri+"/role/"+roleId+"/permission/"+resourceId, status.getStatusCode(), HTTPMethod.PUT, map);
        return response2;
    }

    private static Map<String, Object> assignRoleToUser(Status status, Long userId, Long roleId) {
        Map<String, Object> response2 = expectResponse(baseUri+"/user/"+userId+"/role/"+roleId, status.getStatusCode(), HTTPMethod.PUT);
        return response2;
    }

}
