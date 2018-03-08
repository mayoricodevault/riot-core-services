package com.tierconnect.riot.appcore.entities;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.*;

@Entity
@Table(name="resource")
public class Resource extends ResourceBase {

    public static final String THING_TYPE_PREFIX = "_thingtype_";
    public static final String DATA_ENTRY_FORM_MODULE_PREFIX = "_reportEntryOption_";
    public static final String REPORT_DEFINITION = "_reportDefinition_";

    public static final String THING_TYPES_MODULE = "Thing Types";
    public static final String REPORTS_MODULE = "Reports";
    public static final String REPORT_INSTANCES_MODULE = "Report Instances";
    public static final String IMPORT_INSTANCES_MODULE = "Import Export";
    public static final String INSERT_PERMISSION = "i";
    public static final String UPDATE_PERMISSION = "u";
    public static final String DELETE_PERMISSION = "d";
    public static final String READ_PERMISSION = "r";
    public static final String EXECUTE_PERMISSION = "x";

    //public static final String ASSOCIATE_PERMISSION = "d";
    //public static final String DISASSOCIATE_PERMISSION = "d";
    public static final String PRINT_PERMISSION = "p";
    public static final String RESOURCE_BRIDGES_RULES_NAME = "Bridges & Rules";
    public static final String RESOURCE_SCHEDULED_RULE_NAME = "Scheduled Rule";

    /**
	 * Example name.attributes:
	 *
	 *   user.rwdx
	 *   role.rwdx
	 *   chart.rx
	 *   initdb.x
	 *
	 *   attribute   HTTP METHOD     SQL
	 *   ---------------------------------------------------------
	 *   r           GET             select
	 *   w           PUT             insert update
	 *   d           DELETE          delete
	 *   x           POST            select insert update delete
	 *
	 *
	 *   Examples by terry, for discussion:
	 *
	 *   CLASS PERMISSIONING
	 *   user:[s,i,u,d,a]          - Basic CRUD: select, insert, update, delete, archive a user. Select is also used for list
	 *
	 *   Examples for hand written biz logic:
	 *
	 *   user.logon:[x] - e"x"ecute the given web service method
	 *   user.logoff:[x]
	 *   thing.activate:[x]
	 *
	 */
    public Resource(){
        super();
    }

	public Resource(String name, String acceptedAttributes) {
		this.name = name;
        this.acceptedAttributes = acceptedAttributes;
	}

	public Resource(Group group, String name, String acceptedAttributes) {
		this.group = group;
		this.name=name;
        this.acceptedAttributes = acceptedAttributes;
	}

	//TODO: use utility method (from where?)
	static private String lcfirst( String str )
	{
		return str.substring(0,1).toLowerCase() + str.substring(1);
	}

    static public Resource getModuleResource( Group group, String moduleName, String moduleDescription )
    {
        Resource r = new Resource();
        r.group = group;
        r.fqname = moduleName;
        r.name = moduleName;
        r.acceptedAttributes = "x";
        r.label = r.name;
        r.description = moduleDescription;
        r.setTreeLevel(1);
        r.setParent(null);
        r.type = ResourceType.MODULE.getId();
        return r;
    }

    static public Resource getModuleResource(Group group, String fqName, String moduleName, Resource parent){

        Resource r = new Resource();
        r.setGroup(group);
        r.setFqname(fqName);
        r.setName(moduleName);
        r.setAcceptedAttributes("x");
        r.setLabel("" + fqName);
        r.setDescription(fqName);
        r.setTreeLevel(2);
        r.setParent(parent);
        r.setType(ResourceType.MODULE.getId());
        return r;
    }


    /**
     * CLASS level resource
     * @param group
     * @param entityClass
     * @return
     */
	static public Resource getClassResource( Group group, Class entityClass, Resource module )
	{
		Resource r = new Resource();
		r.group = group;
		r.fqname = entityClass.getCanonicalName();
		r.name = lcfirst( entityClass.getSimpleName() );
		r.acceptedAttributes = "riuda";
        r.label = changeName(r.name);
		r.description = "Class level resource for " + entityClass.getSimpleName() + " CRUD";
        r.setTreeLevel(2);
        r.setParent(module);
		r.type = ResourceType.CLASS.getId();
		return r;
	}

    static public Resource getPropertyResource(Group group,
                                               Resource classResource,
                                               String property,
                                               String label,
                                               String description){
        return getPropertyResource(group,
                                   classResource,
                                   property,
                                   label,
                                   description,
                                   classResource.getParent(),
                                   3,
                                   "u");

    }

    static public Resource getPropertyResource(Group group,
                                               Resource classResource,
                                               String property,
                                               String label,
                                               String description,
                                               Resource parent,
                                               int treeLevel,
                                               String acceptedAttributes){
        Resource r = new Resource();
        r.group = group;
        r.fqname = classResource.getFqname();
        r.name = lcfirst(classResource.getName()) + "_" + property;
        r.acceptedAttributes = acceptedAttributes;
        r.label = label;
        r.description = description;
        r.setTreeLevel(treeLevel);
        r.setParent(parent);
        r.type = ResourceType.PROPERTY.getId();
        return r;
    }


	/*in appcore 1.x
	private List roleResources;
    If this is uncommented can cause a serialization problem to JSON, recursive, stacktraceOverFlow
    @OneToMany(mappedBy="resource", fetch=FetchType.EAGER)
    protected Set<RoleResource> roleResources;

	name=parent, to-one, persistence-modifier=persistent, pattern=null, type=Resource
	private Resource parent;

	name=resourceType, to-one, persistence-modifier=persistent, pattern=null, type=ResourceType
	private ResourceType resourceType;

	name=displayName, basic, persistence-modifier=persistent, pattern=null, type=String
	private String displayName;*/

    public String getNameClass() {
        if (this.name != null && this.name.contains(".")) {
            return this.name.substring(0, this.name.indexOf("."));
        } else {
            return name;
        }
    }

    public String getNameProperty() {
        if (this.name != null && this.name.contains(".")) {
            return this.name.substring(this.name.indexOf(".")+1);
        } else {
            return null;
        }
    }

    public boolean isPropertyOrMethodResource() {
        return this.name != null && this.name.contains(".");
    }

    public String getNameEntityPart() {
        if (isPropertyOrMethodResource()) {
            return this.name.substring(0, this.name.indexOf("."));
        } else {
            return this.name;
        }
    }

    public String getNamePropertyMethodPart() {
        if (isPropertyOrMethodResource()) {
            return this.name.substring(this.name.indexOf(".")+1, this.name.length());
        } else {
            return null;
        }
    }

    //TODO: refactor to eliminate this ! Bad idea ! Do this in the services class, or better yet, handle through use of the extra param !
    @Override
    public Map<String,Object> publicMap()
    {
        Map<String, Object> map = super.publicMap();
        map.put( "acceptedAttributeList", getAcceptedAttributeList());
        return map;
    }

    @Override
    public Map<String,Object> referencedPublicMap(int level)
    {
        Map<String, Object> map = super.publicMap();
        map.put( "acceptedAttributes", getAcceptedAttributeList());
        return map;
    }

    public List<String> getAcceptedAttributeList() {
        List<String> result = new ArrayList<>();
        for (int i = 0;i < acceptedAttributes.length(); i++){
            result.add(""+acceptedAttributes.charAt(i));
        }
        return result;
    }

    public void setAcceptedAttributes(Set<String> attributeList) {
        StringBuilder r  = new StringBuilder();
        for (String attribute: attributeList){
            r.append(attribute);
        }
        acceptedAttributes = r.toString();
    }

	public static String changeName(String s)
	{
		String name=s;
		String name2=""+name.charAt(0);
		name2=name2.toUpperCase();

		for (int i = 1; i < name.length(); i++) {
			if(Character.isUpperCase(name.charAt(i)))
				name2=name2+" "+name.charAt(i);
			else name2+=name.charAt(i);
		}
		String name3="";
		if (name.indexOf("_edit") >= 0) {
			for (int i = 0; i < name2.length(); i++) {
				if(name2.charAt(i)!='_')
					name3=name3+name2.charAt(i);
				else break;

			}

			name2="Edit Own "+name3;
		}

		return name2;
	}


}
