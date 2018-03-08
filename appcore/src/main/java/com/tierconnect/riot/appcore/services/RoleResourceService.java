package com.tierconnect.riot.appcore.services;

/**
 * Created by oscar on 16-04-14.
 */
import com.tierconnect.riot.appcore.entities.Resource;
import com.tierconnect.riot.appcore.entities.Role;
import com.tierconnect.riot.appcore.entities.RoleResource;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.log4j.Logger;

import java.util.*;

public class RoleResourceService extends RoleResourceServiceBase {
    static Logger logger = Logger.getLogger(RoleResourceService.class);


    public List<RoleResource> list(Role role){
        if(role == null) {
            throw new UserException("Invalid role");
        }

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("role.id", role.getId());
        List<RoleResource> resourceRoles = getRoleResourceDAO().selectAllBy(map);
        return resourceRoles;
    }

    public RoleResource insert(Role role, Resource resource, String permissions){
        // permissions may be null (or empty string), for property level permissions. this indicates a negative permission
        if( permissions != null && ! "".equals( permissions ) )
            for(int i = 0; i < permissions.length(); i++) {
                if(!resource.getAcceptedAttributes().contains(permissions.charAt(i)+"")){
                    throw new UserException("Permissions not valid for Resource: name: '" + resource.getName() + "' accepted: '" + resource.getAcceptedAttributes()
                            + "' requested: '" + permissions + "' failed: '" + permissions.charAt(i) + "'" );
                }
            }
        RoleResource rs = get(role.getId(), resource.getId());
        if (rs == null) {
            rs = new RoleResource(role, resource, permissions);
            getRoleResourceDAO().insert(rs);
            //Updating the relationship between role and resource
            if(role.getRoleResources() != null){
                role.getRoleResources().add(rs);
            }else{
                Set roleResource = new HashSet<>();
                roleResource.add(rs);
                role.setRoleResources(roleResource);
            }
        } else {
            rs.setPermissions(permissions);
            getRoleResourceDAO().update(rs);
        }
        return rs;
    }

    public void delete(Role role, RoleResource roleResource) {
        if (role == null && roleResource == null) {
            throw new UserException("Invalid roleResource object");
        }
        if (role != null) {
            role.getRoleResources().remove(roleResource);
        }
        getRoleResourceDAO().delete(roleResource);
    }

	//TODO refactor this method should accept objectId and null values
    public RoleResource get(Long roleId, Long resourceId) {
        Map<String, Object> map= new HashMap<String, Object>();
        map.put("role.id", roleId);
        map.put("resource.id", resourceId);
        List<RoleResource> rs = getRoleResourceDAO().selectAllBy(map);
        if (rs.size() == 0) {
            return null;
        } else if (rs.size() == 1) {
            return rs.get(0);
        } else {
            logger.error("Found n records for "+map);
            return rs.get(0);
        }
    }

    public RoleResource update(RoleResource rs) {
        for(int i = 0; i < rs.getPermissions().length(); i++) {
            if(!rs.getResource().getAcceptedAttributes().contains(rs.getPermissions().charAt(i)+"")){
                throw new UserException("Permissions not valid for Resource");
            }	
        }
        getRoleResourceDAO().update(rs);
        return rs;
    }

    @Override public RoleResource insert(RoleResource roleResource) {
        validateInsert(roleResource);
        return super.insert(roleResource);
    }

    @Override public void validateInsert(RoleResource roleResource) {
        super.validateInsert(roleResource);
        if (roleResource == null || roleResource.getRole() == null || roleResource.getResource() == null){
            throw new UserException("Role Resource does not exists.");
        }
    }
}
