package com.tierconnect.riot.appcore.controllers;

/**
 * Created by oscar on 16-04-14.
 */

import java.util.*;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.services.*;
import com.tierconnect.riot.appcore.utils.VisibilityUtils;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.servlet.exception.ForbiddenException;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.jaxrs.PATCH;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;


@Path("/role")
@Api("/role")
public class RoleController extends RoleControllerBase{
    static Logger logger = Logger.getLogger(RoleController.class);

	private String[] IGNORE_ROLE_FIELDS = new String[]{"permissions"};
	
	@PUT
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value="role:i")
    @ApiOperation(value="Create Role")
    public Response insertRole(Map<String, Object> roleMap,@QueryParam("createRecent") @DefaultValue("false") Boolean createRecent)
    {
        Role role = RoleService.getInstance().createRol(roleMap);
        if (createRecent){
            RecentService.getInstance().insertRecent(role.getId(), role.getName(),"role", role.getGroup());
        }
        return RestUtils.sendCreatedResponse(role.publicMap());
    }

    @PATCH
    @Path("/{role_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value="role:u")
    @ApiOperation(value="Update Role")
    public Response updateRole(@PathParam("role_id") Long roleId, Map<String, Object> roleMap){
        Role role = RoleService.getInstance().get(roleId);
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(Role.class.getCanonicalName(), null);
        if(GroupService.getInstance().isGroupNotInsideTree(role.getGroup(), visibilityGroup)){
            throw new ForbiddenException("Forbidden role");
        }

        if(roleMap.get("name") != null) {
            role.setName((String) roleMap.get("name"));
        }
        if(roleMap.get("description") != null) {
            role.setDescription((String) roleMap.get("description"));
        }
        if(roleMap.get("archived") != null) {
            role.setArchived((Boolean) roleMap.get("archived"));
        }
        if(roleMap.get("group.id") != null) {
            Group group = GroupService.getInstance().get(((Number)roleMap.get("group.id")).longValue());
            if(GroupService.getInstance().isGroupNotInsideTree(group, visibilityGroup)){
                throw new ForbiddenException("Forbidden role");
            }
            if ((group != null) && (!group.equals(role.getGroup()))){
                //has related user to this role
                List<UserRole>  userRoles = UserRoleService.getInstance().listUsersByRole(role);
                if ((userRoles != null) && (userRoles.size() > 0 )){
                    int countUserRole = 0;
                    StringBuilder nameUserRole = new StringBuilder();
                    for (UserRole userRole : userRoles){
                        if (countUserRole > 4){
                            break;
                        }
                        nameUserRole.append(userRole.getUser().getUsername()).append(", ");
                    }
                    String stringNameUserName = nameUserRole.toString();
                    stringNameUserName = stringNameUserName.substring(0, stringNameUserName.length()-2);
                    return RestUtils.sendBadResponse("You cannot change the group of " + role.getName() + " role, because it is assigned to users: " + stringNameUserName + ".");
                }
            }
            if (RoleService.getInstance().getRootRole().getId().equals(role.getId())) {
                if (!GroupService.getInstance().getRootGroup().getId().equals(group.getId())) {
                    throw new ForbiddenException("You cannot change the group of root role");
                }
            }
            role.setGroup(group);
        }
        if (roleMap.containsKey("groupTypeCeiling.id")) {
            Number number = (Number) roleMap.get("groupTypeCeiling.id");
            GroupType groupType = null;
            if (number != null) {
                groupType = GroupTypeService.getInstance().get(number.longValue());
            }
            role.setGroupTypeCeiling(groupType);
        }

        role = RoleService.getInstance().update(role);
        RecentService.getInstance().updateName(role.getId(), role.getName(),"role");

        return RestUtils.sendOkResponse(role.publicMap());
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value="role:d")
    @ApiOperation(value="Delete Role")
    public Response deleteRole(@PathParam("id") Long roleId) {
        Role role = RoleService.getInstance().get(roleId);

        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(Role.class.getCanonicalName(), null);
        if(GroupService.getInstance().isGroupNotInsideTree(role.getGroup(), visibilityGroup)){
            throw new ForbiddenException("Forbidden role");
        }

        for(RoleResource roleResource : RoleResourceService.getInstance().list(role)){
            RoleResourceService.getInstance().delete(role, roleResource);
        }
        RecentService.getInstance().deleteRecent(roleId,"role");
        RoleService.getInstance().delete(role);
        return RestUtils.sendDeleteResponse();
    }

    @PUT
    @Path("/{roleId}/permission")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"role:i", "role:u"}, logical = Logical.OR)
    @ApiOperation(value="Modify Role Permissions")
    public  Response setResources(@PathParam("roleId") Long roleId, Map<String, Object> m)
    {
        List list = RoleService.getInstance().setResources(roleId,  m);
        return RestUtils.sendOkResponse(list);
    }


    @PUT
    @Path("/{roleId}/permission/{resourceId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"role:i", "role:u"}, logical = Logical.OR)
    @ApiOperation(value="Modify Role Permissions")
    public  Response setResource(@PathParam("roleId") Long roleId, @PathParam("resourceId") Long resourceId, Map<String, Object> m) {
        User currentUser = (User) SecurityUtils.getSubject().getPrincipal();
        logger.debug("AGG 5.START update Resource{");
        Role role = RoleService.getInstance().get(roleId);
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(Role.class.getCanonicalName(), null);
        if(GroupService.getInstance().isGroupNotInsideTree(role.getGroup(), visibilityGroup)){
            throw new ForbiddenException("Forbidden role");
        }
        Resource resource = ResourceService.getInstance().get(resourceId);

        if (!LicenseService.getInstance().isValidResource(currentUser, resource.getName())) {
            throw new UserException("Not a valid resourceId "+ resourceId);
        }
        String permissions = (String) m.get("permissions");
        RoleResource rs = RoleService.getInstance().updateResource(role, resource, permissions);
        return RestUtils.sendOkResponse(rs == null? null : rs.publicMap());
    }


    @Override
    public List<String> getExtraPropertyNames() {
        return Arrays.asList(IGNORE_ROLE_FIELDS);
    }

}
