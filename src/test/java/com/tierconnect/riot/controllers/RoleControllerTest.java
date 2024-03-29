package com.tierconnect.riot.controllers;

/**
 * Created by franco on 23-04-14.
 */

import static com.tierconnect.riot.utils.TestUtils.expectResponse;

import java.util.*;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.services.GroupService;
import org.apache.log4j.Logger;
import org.hibernate.Transaction;
import org.jboss.resteasy.plugins.server.tjws.TJWSEmbeddedJaxrsServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.thetransactioncompany.cors.HTTPMethod;
import com.tierconnect.riot.utils.TestUtils;

public class RoleControllerTest {
    static Logger logger = Logger.getLogger(RoleControllerTest.class);

    private static String baseUri;
    private static String resourcesBaseUri;
    private static TJWSEmbeddedJaxrsServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        int port = 6600;
        server = TestUtils.startTestServer(port);
        baseUri = "http://localhost:" + port + "/role";
        resourcesBaseUri = "http://localhost:" + port + "/resource";
    }

    @AfterClass
    public static void afterClass() {
    	TestUtils.stopServer();
    }

    private Map<String, Object> createRoleMap(String name, String description) {
        Transaction transaction = GroupService.getInstance().getGroupDAO().getSession().getTransaction();
        transaction.begin();
        List<Group> groups = GroupService.getInstance().getGroupDAO().selectAll();
        transaction.commit();

        Random random = new Random();
        Map<String, Object> role = new HashMap<String, Object>();
        role.put("name", name);
        role.put("description", description);
        role.put("group.id", groups.get(random.nextInt(groups.size())).getId());
        return role;
    }

    private Map<String, Object> createPermissionMap(String permissions) {
        Map<String, Object> permission = new HashMap<String, Object>();
        permission.put("permissions", permissions);
        return permission;
    }

    @Test
    public void testListRole() {
        expectResponse(baseUri, 200, HTTPMethod.GET, null);
    }

    @Test
    public void testGetRole() {
        expectResponse(baseUri + "/1", 200, HTTPMethod.GET, null);
    }

    @Test
    public void testValidateGetRole() {
        expectResponse(baseUri + "/99999", 400, HTTPMethod.GET, null);
    }

    @Test
    public void testInsertRole() {
        String roleName = System.currentTimeMillis()+"";
        Map<String, Object> role = createRoleMap(roleName, "role for testing");
        expectResponse(baseUri, 201, HTTPMethod.PUT, role);
        //TODO reenable
        //expectResponse(baseUri, 400, HTTPMethod.PUT, role);
    }

    @Test
    public void testCompleteRoleLifecycle() {
        // Insert role, insert role resources then delete
        String roleName = System.currentTimeMillis()+"";
        Map<String, Object> role = createRoleMap(roleName, "test role and resources");
        role = expectResponse(baseUri, 201, HTTPMethod.PUT, role);
        Map<String, Object> resources = expectResponse(resourcesBaseUri, 200, HTTPMethod.GET, null);
        ArrayList<Map> rs = (ArrayList<Map>) resources.get("results");
        //int limit = 3;
        for(Map r : rs) {
            if (r.get("isFolder") != null && Boolean.TRUE.equals(r.get("isFolder"))) {
                ArrayList<Map> rs2 = (ArrayList<Map>) r.get("children");
                for(Map r2 : rs2) {
                    expectResponse(baseUri + "/" + role.get("id") + "/permission/" + r2.get("id"), 201, HTTPMethod.PUT, createPermissionMap((String) r2.get("acceptedAttributes")));
                }
            } else {
                expectResponse(baseUri + "/" + role.get("id") + "/permission/" + r.get("id"), 201, HTTPMethod.PUT, createPermissionMap((String) r.get("acceptedAttributes")));
            }
            //limit--;
            //if(limit < 0){
            //	break;
            //}
        }
        expectResponse(baseUri + "/" + role.get("id"), 204, HTTPMethod.DELETE, null);
    }

    @Test
    public void testInvalidPermissions(){
        // Insert role, insert invalid role resource
        String roleName = System.currentTimeMillis()+"";
        Map<String, Object> role = createRoleMap(roleName, "test role and resources");
        role = expectResponse(baseUri, 201, HTTPMethod.PUT, role);
        Map<String, Object> resources = expectResponse(resourcesBaseUri, 200, HTTPMethod.GET, null);
        ArrayList<Map> rs = (ArrayList<Map>) resources.get("results");
        for(Map r : rs) {
            if (r.get("isFolder") != null && Boolean.TRUE.equals(r.get("isFolder"))) {
                ArrayList<Map> rs2 = (ArrayList<Map>) r.get("children");
                for(Map r2 : rs2) {
                    expectResponse(baseUri + "/" + role.get("id") + "/permission/" + r2.get("id"), 400, HTTPMethod.PUT, createPermissionMap("wrxpmnxyz"));
                }
            } else {
                expectResponse(baseUri + "/" + role.get("id") + "/permission/" + r.get("id"), 400, HTTPMethod.PUT, createPermissionMap("wrxpmnxyz"));
            }
        }
    }

    @Test
    public void testUpdateRole(){
        String roleName = System.currentTimeMillis() + "";
        Map<String, Object> role = createRoleMap(roleName, "test role update");
        role = expectResponse(baseUri, 201, HTTPMethod.PUT, role);
        String newName = System.currentTimeMillis() + "";
        role.put("name", newName);
        expectResponse(baseUri + "/" + role.get("id"), 200, HTTPMethod.PATCH, role);
    }
    
    @Test
    public void deleteRole(){
    	String roleName = System.currentTimeMillis()+"";
        Map<String, Object> role = createRoleMap(roleName, "role for testing");
        role = expectResponse(baseUri, 201, HTTPMethod.PUT, role);
        expectResponse(baseUri + "/" + role.get("id"), 204, HTTPMethod.DELETE, null);
    }
}
