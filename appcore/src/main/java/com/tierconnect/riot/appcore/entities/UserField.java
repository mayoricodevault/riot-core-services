package com.tierconnect.riot.appcore.entities;

import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 
 * @author garivera
 *
 */
@Entity
@Table(name="userfield")
public class UserField extends UserFieldBase
{
	//TODO: refactor to use extra parameter instead
    public Map<String, Object> publicMap(boolean includeSingleRelationShip) {
		Map<String, Object> map = publicMap();
		if (includeSingleRelationShip) {
			map.put("user", this.user == null ? null	: this.user.publicMap());
			map.put("field", this.field == null ? null : this.field.publicMap());
		}
		return map;
	}		
    
}
