package com.tierconnect.riot.appgen.model;

import com.tierconnect.riot.sdk.utils.UpVisibility;

import javax.persistence.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "aps_class")
public class Clazz 
{
	static Map<String, Clazz> clazzes = new HashMap<String, Clazz>();
	
	@Id
	@GeneratedValue
	protected Long id;
	
	String name;
	
	String tableName;

	String [] annotations;

	//@OneToMany
	String [] imports;

	String implement;
	
	@OneToMany
	List<Property> properties;
	
	AppgenPackage apackage;
	
	boolean generateController = true;
    boolean generateService = true;
	UpVisibility aboveVisibility = UpVisibility.FALSE;
	Boolean belowVisibility = true;
	
	boolean generateControllerList = true;
	boolean generateControllerSelect = true;
	boolean generateControllerInsert = true;
	boolean generateControllerUpdate = true;
	boolean generateControllerDelete = true;
	
	boolean generateServiceInsert = true;
	boolean generateServiceUpdate = true;
	boolean generateServiceDelete = true;

	private boolean deprecated;
	String [] tableAnnotations;
	boolean checkNullValue = true;
	
	public static boolean has( String name )
	{
		return clazzes.containsKey( name );
	}

    public static Clazz get( String name )
    {
        return clazzes.get( name );
    }

	public Long getId() 
	{
		return id;
	}

	public void setId(Long id) 
	{
		this.id = id;
	}

	public String getClassName() 
	{
		return name;
	}

	public void setClassName(String name) 
	{
		clazzes.put( name, this );
		this.name = name;
	}
	
	public String getModelName() 
	{
		return getClassName();
	}
	
	public String getModelNameLowerCase()
	{
		String str = this.getModelName();
		return str.substring( 0, 1 ).toLowerCase() + str.substring( 1, str.length() );
	}
	
	public Object getModelNamePlural() 
	{
		return getClassName() + "s";
	}
	
	public String getModelBaseName() 
	{
		return getClassName() + "Base";
	}
	
	public String getDAOBaseName() 
	{
		return getClassName() + "DAOBase";
	}
	
	public String getDAOName() 
	{
		return getClassName() + "DAO";
	}
	
	public String getDAONameLowerCase() 
	{
		String str = this.getDAOName();
		return str.substring( 0, 1 ).toLowerCase() + str.substring( 1, str.length() );
	} 
	
	public String getServiceBaseName() 
	{
		return getClassName() + "ServiceBase";
	}
	
	public String getServiceName() 
	{
		return getClassName() + "Service";
	}
	
	public Object getControllerBaseName() 
	{
		return getClassName() + "ControllerBase";
	}

	public String getControllerName() 
	{
		return getClassName() + "Controller";
	}
	
	public String getTableName() 
	{
		return tableName;
	}

	public void setTableName(String tableName) 
	{
		this.tableName = tableName;
	}

	public String[] getAnnotations() 
	{
		return annotations;
	}

	public void setAnnotations(String[] annotations) 
	{
		this.annotations = annotations;
	}

	public String getPackageName()
	{
		//int i = name.lastIndexOf( '.' );
		//return name.substring( 0, i );
		return this.getPackage().getPackageName();
	}
	
	/*
	public String getShortClassName() 
	{
		int i = name.lastIndexOf( '.' );
		return name.substring( i + 1, name.length() );
	}
	*/
	
	public String getPackagePath()
	{
		return this.getPackage().getPackagePath();
	}
	
	public AppgenPackage getPackage() 
	{
		return apackage;
	}

	public void setPackage(AppgenPackage apackage)
	{
		this.apackage = apackage;
	}

	public String[] getImports() 
	{
		return imports;
	}

	public void setImports(String[] imports) 
	{
		this.imports = imports;
	}

	public String getImplement(){
		return implement;
	}

	public void setImplement(String implement){
		this.implement = implement;
	}

	public List<Property> getProperties() 
	{
		return properties;
	}

	public void setProperties(List<Property> properties) 
	{
		this.properties = properties;
	}

	public void setGenerateControllerAll( boolean value )
	{
		generateControllerList = value;
		generateControllerSelect = value;
		generateControllerInsert = value;
		generateControllerUpdate = value;
		generateControllerDelete = value;
	}
	
	public void setGenerateServiceAll( boolean value )
	{
		generateServiceInsert = value;
		generateServiceUpdate = value;
		generateServiceDelete = value;
	}

	public boolean isGenerateController()
	{
		return generateController;
	}

    public boolean isGenerateService() {
        return generateService;
    }

    public void setGenerateService(boolean generateService) {
        this.generateService = generateService;
    }

    public void setGenerateController(boolean generateController)
	{
		this.generateController = generateController;
	}

	public boolean isGenerateControllerList() 
	{
		return generateControllerList;
	}

	public void setGenerateControllerList(boolean generateControllerList) 
	{
		this.generateControllerList = generateControllerList;
	}

	public boolean isGenerateControllerSelect() 
	{
		return generateControllerSelect;
	}

	public void setGenerateControllerSelect(boolean generateControllerSelect) 
	{
		this.generateControllerSelect = generateControllerSelect;
	}

	public boolean isGenerateControllerInsert() 
	{
		return generateControllerInsert;
	}

	public void setGenerateControllerInsert(boolean generateControllerInsert) 
	{
		this.generateControllerInsert = generateControllerInsert;
	}

	public boolean isGenerateControllerUpdate() 
	{
		return generateControllerUpdate;
	}

	public void setGenerateControllerUpdate(boolean generateControllerUpdate) 
	{
		this.generateControllerUpdate = generateControllerUpdate;
	}

	public boolean isGenerateControllerDelete() 
	{
		return generateControllerDelete;
	}

	public void setGenerateControllerDelete(boolean generateControllerDelete) 
	{
		this.generateControllerDelete = generateControllerDelete;
	}

	public boolean isGenerateServiceInsert() 
	{
		return generateServiceInsert;
	}

	public void setGenerateServiceInsert(boolean generateServiceInsert)
	{
		this.generateServiceInsert = generateServiceInsert;
	}

	public boolean isGenerateServiceUpdate() 
	{
		return generateServiceUpdate;
	}

	public void setGenerateServiceUpdate(boolean generateServiceUpdate) 
	{
		this.generateServiceUpdate = generateServiceUpdate;
	}

	public boolean isGenerateServiceDelete() 
	{
		return generateServiceDelete;
	}

	public void setGenerateServiceDelete(boolean generateServiceDelete) 
	{
		this.generateServiceDelete = generateServiceDelete;
	}

	public boolean hasGroup() 
	{
		if( "Group".equals( name ) )
			return true;
		for( Property p : properties )
		{
			if( p.getName().equals( "group" ) )
				return true;
		}
		return false;
	}

    public boolean hasParent()
    {
        for( Property p : properties )
        {
            if( p.getName().equals( "parent" ) )
                return true;
        }
        return false;
    }

    public boolean hasProperty(String property)
    {
        for( Property p : properties )
        {
            if( p.getName().equals( property ) )
                return true;
        }
        return false;
    }

	public Boolean isBelowVisibility() {
		return belowVisibility;
	}

	public void setBelowVisibility(Boolean belowVisibility) {
		this.belowVisibility = belowVisibility;
	}

	public UpVisibility getAboveVisibility() {
		return aboveVisibility;
	}

	public void setAboveVisibility(UpVisibility aboveVisibility) {
		this.aboveVisibility = aboveVisibility;
	}

	public String getVisibilityGroupGetter()
	{
		if( "Group".equals( name ) )
		{
			return "getError";
		}
		else
			return "getGroup";
	}
	
	public String getVisibilityGroupQName()
	{
		if( "Group".equals( name ) )
		{
			return "error";
		}
		else
			return "group";
	}

	public void setDeprecated( boolean b )
	{
		deprecated = b;
	}
	
	public boolean isDeprecated()
	{
		return deprecated;
	}

	public String[] getTableAnnotations()
	{
		return tableAnnotations;
	}

	public void setTableAnnotations(String[] tableAnnotations)
	{
		this.tableAnnotations = tableAnnotations;
	}

	public boolean isCheckNullValue() {
		return checkNullValue;
	}

	public void setCheckNullValue(boolean checkNullValue) {
		this.checkNullValue = checkNullValue;
	}
}
