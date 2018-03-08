package com.tierconnect.riot.appcore.entities;

import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="groupfield")
public class GroupField extends GroupFieldBase
{   
	//TODO: refactor to use "extra" fields parameter instead
    public Map<String, Object> publicMap(boolean includeSingleRelationShip) {
		Map<String, Object> map = publicMap();
		if (includeSingleRelationShip) {
			map.put("group", this.group == null ? null	: this.group.publicMap());
			map.put("field", this.field == null ? null : this.field.publicMap());
		}
		return map;
	}		
	
}
