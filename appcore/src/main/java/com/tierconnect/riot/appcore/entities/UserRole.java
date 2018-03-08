package com.tierconnect.riot.appcore.entities;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.annotation.Generated;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name="apc_user_role")
@Generated("com.tierconnect.riot.appgen.service.GenModel")
public class UserRole extends UserRoleBase 
{


    public Map<String, Object> publicMap(boolean includeSingleRelationShip) {
        Map<String, Object> map = publicMap();
        if (includeSingleRelationShip) {
            map.put("role", this.role == null ? null : this.role.publicMap());
            map.put("user", this.user == null ? null : this.user.publicMap());
        }
        return map;
    }

}

