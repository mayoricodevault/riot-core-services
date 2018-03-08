package com.tierconnect.riot.appcore.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

import com.tierconnect.riot.sdk.entities.SurrogateKey;
import org.apache.commons.lang.StringUtils;

@Entity
@Table(name="Group0", indexes = {@Index(name = "IDX_group0_code",  columnList="code"),
		@Index(name = "IDX_group0_hierarchyName",  columnList="hierarchyName")})
public class Group extends GroupBase implements SurrogateKey, com.tierconnect.riot.commons.entities.IGroup
{
	public static final int MAX_LEVEL = 10;
	public static final String ALPHA_NUMERIC = "^[a-zA-Z0-9]*$";

	public Group()
	{
		super();
		
	}
	public Group(String name) {
		this.name = name;
	}

	public List<Group> getAscendants() {
		List<Group> result = new ArrayList<Group>();
		for (int i=1; i< getTreeLevel(); i++) {
			result.add(getParentLevel(i));
		}
		return result;
	}

	public List<Long> getAscendantIds() {
		List<Long> result = new ArrayList<Long>();
		for (Group group: getAscendants()) {
			result.add(group.getId());
		}
		return result;
	}
	
	public void setParentLevel(int level, Group group) {
		if (level == 1) {
			setParentLevel1(group);
		} else if (level == 2) {
			setParentLevel2(group);
		} else if (level == 3) {
			setParentLevel3(group);
		} else if (level == 4) {
			setParentLevel4(group);
		} else if (level == 5) {
			setParentLevel5(group);
		} else if (level == 6) {
			setParentLevel6(group);
		} else if (level == 7) {
			setParentLevel7(group);
		} else if (level == 8) {
			setParentLevel8(group);
		} else if (level == 9) {
			setParentLevel9(group);
		} else if (level == 10) {
			setParentLevel10(group);
		} else {
			throw new RuntimeException("Not supported level, we only support levels 1.."+MAX_LEVEL);
		}
	}

	public Group getParentLevel(int level) {
		if (level == 1) {
			return getParentLevel1();
		} else if (level == 2) {
			return getParentLevel2();
		} else if (level == 3) {
			return getParentLevel3();
		} else if (level == 4) {
			return getParentLevel4();
		} else if (level == 5) {
			return getParentLevel5();
		} else if (level == 6) {
			return getParentLevel6();
		} else if (level == 7) {
			return getParentLevel7();
		} else if (level == 8) {
			return getParentLevel8();
		} else if (level == 9) {
			return getParentLevel9();
		} else if (level == 10) {
			return getParentLevel10();
		} else {
			throw new RuntimeException("Not supported level, we only support levels 1.."+MAX_LEVEL);
		}
	}

	//Needs the parentGroup Set
	public void setParentGroupAndRecalculate() {
		Group parentGroup = getParent();
		if (parentGroup == null) {
			setTreeLevel(1);
			setParentLevel1(this);
		} else {
			setTreeLevel(parentGroup.getTreeLevel() + 1);
			setParentLevel(getTreeLevel(), this);
			Group parentGroupAux = parentGroup;			
			for (int i=getTreeLevel()-1; i>=1; i--) {
				setParentLevel(i, parentGroupAux);
				parentGroupAux=parentGroupAux.getParent();
			}
		}
	}

    public Map<String, Object> publicMap(boolean includeSingleRelationShip) {
        Map<String, Object> map = publicMap();
        if (includeSingleRelationShip) {
            map.put("parent", this.parent == null ? null : this.parent.publicMap());
            map.put("type", this.getGroupType() == null ? null : this.getGroupType().publicMap());
        }
        if(getGroupResources()!=null && getGroupResources().size() > 0){
            List<GroupResources> groupResourcesList = new ArrayList<>(getGroupResources());
            map.put("imageTemplateName", groupResourcesList.get(0).getImageTemplateName());
			map.put("hasImageIcon", (groupResourcesList.get(0).getImageIcon() != null));
        }
        return map;
    }

	public void setName( String name )
	{
		super.setName(name);
	}

	public void setCode(String code){
		super.setCode(code);
	}
/* For debugging Queries
    public String toString()
    {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put( "id", id );
        map.put( "name", name );
        return map.toString();
    }
*/
    @Override
    public Group getParentLevel1() {
        return super.getParentLevel1();
    }

    @Override
    public Group getParentLevel2() {
        return super.getParentLevel2();
    }

    public Group getTenantGroup()
	{
        if(getTreeLevel()==1){
            return this;
        }
		return this.getParentLevel( 2 );
	}

    @Override
    public Group getParentLevel3() {
        return super.getParentLevel3();
    }

    @Override
    public Group getParentLevel4() {
        return super.getParentLevel4();
    }

    @Override
    public Group getParentLevel5() {
        return super.getParentLevel5();
    }

    @Override
    public Group getParentLevel6() {
        return super.getParentLevel6();
    }

    @Override
    public Group getParentLevel7() {
        return super.getParentLevel7();
    }

    @Override
    public Group getParentLevel8() {
        return super.getParentLevel8();
    }

    @Override
    public Group getParentLevel9() {
        return super.getParentLevel9();
    }

    @Override
    public Group getParentLevel10() {
        return super.getParentLevel10();
    }

    @Override
    public int getTreeLevel() {
        return super.getTreeLevel();
    }

	/**
	 * Builds a complete name with all the ascendants.
	 *
	 * ex. company > facility > area
	 *
	 * TODO performance needs to improve
	 */
	public String getHierarchyName(boolean includeRoot) {
		List<String> names = new ArrayList<>();

		for (Group group : getAscendants()) {
			names.add(group.getCode());
		}

		//remove root if flag false
		if (!includeRoot && !names.isEmpty()) {
			names.remove(0);
		}
        names.add(this.getCode());

		return (names.isEmpty()? "" : ">") + StringUtils.join(names, ">");
	}

    public String getHierarchyCodeForMQTT(){
        return getHierarchyName(true).replace(">", ",").replaceAll("^,", "");
    }

}
