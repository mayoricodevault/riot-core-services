package com.tierconnect.riot.appcore.entities;

/**
 * Created by oscar on 17-04-14.
 */
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Entity
@Table(name="roleresource")
public class RoleResource extends RoleResourceBase {


	public RoleResource(Role role, Resource resource, String permissions) {
		this.role = role;
		this.resource = resource;
		this.permissions = permissions;
	}

	public RoleResource() {
		super();
	}

	public List<String> getPermissionsList() {
		List<String> result = new ArrayList<>();
		for (int i = 0;i < permissions.length(); i++){
			result.add(""+permissions.charAt(i));
		}
		return result;
	}

	public void setPermissions(Set<String> attributeList) {
		StringBuilder r  = new StringBuilder();
		for (String attribute: attributeList){
			r.append(attribute);
		}
		this.permissions = r.toString();
	}
}
