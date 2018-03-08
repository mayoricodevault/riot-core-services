package com.tierconnect.riot.appcore.entities;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.*;

import com.tierconnect.riot.sdk.entities.SurrogateKey;

// was 'Type' in appcore 1.x
@Entity
@Table(name="grouptype")
public class GroupType extends GroupTypeBase implements SurrogateKey, com.tierconnect.riot.commons.entities.IGroupType {
    
    public static boolean equalIds(GroupType a, GroupType b) {
    	if (a == b) {
    		return true;
    	}
    	if (a == null) {
    		return false;
    	}
    	return numberutils_equals(a.getId(), b.getId());
    }
    
	public static boolean numberutils_equals(Long l1, Long l2) {
		if (l1 == l2) {
			return true;
		}
		if (l1 == null) {
			return false;
		}
		return l1.equals(l2);
	}

/*  For debugging Queries
    public String toString()
    {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put( "id", id );
        map.put( "name", name );
        return map.toString();
    }
*/
}
