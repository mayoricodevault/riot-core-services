package com.tierconnect.riot.appcore.entities;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;

import java.util.*;

@Entity
@Table(name = "Role")
@XmlRootElement(name = "Role")
public class Role extends RoleBase{

    public Role(long id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public Role(){
        super();
    }

	public Role(String name, String description, Group group) {
        this.name = name;
        this.description = description;
        this.group = group;
    }

    public Role(String name, String description) {
        this.name = name;
        this.description = description;
    }

    //TODO: refactor to eliminate this ! Bad idea ! Do this in the services class, or better yet, handle through use
    // of the extra param !
    public Map<String,Object> publicMap(){
    	Map<String, Object> publicMap = super.publicMap();
    	
    	Map<String,Object> resources = new HashMap<>();
    	if(this.getRoleResources() != null){
			for(RoleResource roleResource : this.getRoleResources()){
				resources.put(roleResource.getResource().getId()+"",roleResource.getPermissions());
			}
    	}
		
		publicMap.put("permissions", resources);
		
		return publicMap;
    }
}
