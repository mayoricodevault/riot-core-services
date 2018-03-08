package com.tierconnect.riot.appcore.services;

/**
 * Created by oscar on 16-04-14.
 */

import com.mysema.query.BooleanBuilder;
import com.mysema.query.types.expr.BooleanExpression;
import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.utils.VisibilityUtils;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.servlet.exception.ForbiddenException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RoleService extends RoleServiceBase{

    static Logger logger = Logger.getLogger(RoleService.class);

    public Role get(Long roleId){
        Role role = getRoleDAO().selectById(roleId);
        if(role == null){
            throw new UserException(String.format("Role id [%d] not found", roleId));
        }
        return role;
    }

    public Role getRootRole()
    {
    	return getInstance().get( 1L );
    }
    
    public Role getTenantAdminRole()
    {
    	return getInstance().get( 2L );
    }
    
    public Role insert(Role role){
        if(role == null) {
            throw new UserException("Invalid Role object");
        }
        
        if(existRoleByName(role.getName(), null)) {
            throw new UserException("Role name already exists");
        }
        if(role.getName() == null || role.getName().equals("")) {
            throw new UserException("Role name can't be empty");
        }
        Long id = getRoleDAO().insert(role);
        role.setId(id);
        return role;
    }

    public Role update(Role role){
        if(role == null) {
            throw new UserException("Invalid role object");
        }
        if(existRoleByName(role.getName(), role.getId())) {
            throw new UserException("Role name already exists");
        }
        if(role.getName() == null || role.getName().equals("")) {
            throw new UserException("Role name can't be empty");
        }
        getRoleDAO().update(role);
        updateFavorite(role);
        return role;
    }

    public void delete(Role role){
        if(role == null) {
            throw new UserException("Invalid Role object");
        }
        getRoleDAO().delete(role);
    }

    public static boolean existRoleByName(String name, Long excludeId) {
        BooleanExpression predicate = QRole.role.name.eq(name);
        if(excludeId != null) {
            predicate = predicate.and(QRole.role.id.ne(excludeId));
        }
        return getRoleDAO().getQuery().where(predicate).exists();
    }

    /********************************************
     * Method to create a rol
     *******************************************/
    public Role createRol(Map<String, Object> roleMap)
    {
        Role role = new Role();
        role.setName((String) roleMap.get("name"));
        role.setDescription((String) roleMap.get("description"));
        Group group = GroupService.getInstance().get(((Number) roleMap.get("group.id")).longValue());
        role.setGroup(group);

        GroupType groupType = null;
        if (roleMap.containsKey("groupTypeCeiling.id")) {
            Number groupTypeId = (Number) roleMap.get("groupTypeCeiling.id");
            if (groupTypeId != null) {
                groupType = GroupTypeService.getInstance().get(groupTypeId.longValue());
            }
        }
        role.setGroupTypeCeiling(groupType);

        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(Role.class.getCanonicalName(), null);
        if(GroupService.getInstance().isGroupNotInsideTree(role.getGroup(), visibilityGroup)){
            throw new ForbiddenException("Forbidden role");
        }

        role = RoleService.getInstance().insert(role);
        return role;
    }

    /*************************************************
     * Method for setting resources to a rol
     *************************************************/
    public List setResources(Long roleId, Map<String, Object> m)
    {
        User currentUser = (User) SecurityUtils.getSubject().getPrincipal();
        logger.debug("AGG 5.START update Resource{");
        Role role = RoleService.getInstance().get(roleId);
        if(role == null) {
            throw new UserException("Not a valid roleId");
        }
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(Role.class.getCanonicalName(), null);
        if(GroupService.getInstance().isGroupNotInsideTree(role.getGroup(), visibilityGroup)){
            throw new ForbiddenException("Forbidden role");
        }

        List list = new ArrayList();
        for (Map.Entry<String,Object> entry: m.entrySet()) {
            String key = entry.getKey();
            Resource resource;
            ResourceService resourceService = ResourceService.getInstance();
            try {
                resource = resourceService.get(Long.valueOf(key));
            } catch (Exception ex) {
                resource = resourceService.getResourceDAO().selectBy(QResource.resource.name.eq(key));
            }
            if(resource == null) {
                throw new UserException("Not a valid resourceId "+ key);
            }
            if (!LicenseService.getInstance().isValidResource(currentUser, resource.getName())) {
                logger.debug("Not a valid resourceId "+ key);
            } else {
                RoleResource rs = updateResource(role, resource, (String) entry.getValue());
                if (rs != null) {
                    list.add(rs.publicMap());
                }
            }
        }
        return list;
    }

    /******************************************
     * Method to update a specific resource
     ******************************************/
    public static RoleResource updateResource(Role role, Resource resource, String permissions) {
        RoleResource roleResource = RoleResourceService.getInstance().get(role.getId(), resource.getId());
        if (roleResource == null && StringUtils.isNotEmpty(permissions)) {
            roleResource = RoleResourceService.getInstance().insert(role, resource, permissions);
        } else if (roleResource != null) {
            if (StringUtils.isEmpty(permissions)) {
                RoleResourceService.getInstance().delete(role, roleResource);
                return null;
            } else {
                roleResource.setPermissions(permissions);
                RoleResourceService.getInstance().update(roleResource);
            }
        }
        return roleResource;
    }

    /**
     * This method gets a role based on the name of the role and group
     * */
    public List<Role> getByName(String name, Group group)
    {
        List<Role> lstRole = null;
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QRole.role.name.eq(name));
        if (group!=null) {
            be = be.and(QRole.role.group.eq(group));
        }
        lstRole = RoleService.getInstance().getRoleDAO().selectAllBy( be );

        return lstRole;
    }

    public Role getByNameAndGroup(String name, Group group)
    {
//        lstRole = null;
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QRole.role.name.eq(name));
        if (group!=null) {
            be = be.and(QRole.role.group.eq(group));
        }
//        List<Role> lstRole = getRoleDAO().selectBy( be );

        return getRoleDAO().selectBy( be );
    }
}
