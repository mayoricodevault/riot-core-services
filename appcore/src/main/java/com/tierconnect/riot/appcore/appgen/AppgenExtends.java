package com.tierconnect.riot.appcore.appgen;

import com.tierconnect.riot.appgen.model.AppgenPackage;
import com.tierconnect.riot.appgen.model.Application;
import com.tierconnect.riot.appgen.model.Clazz;
import com.tierconnect.riot.appgen.model.Property;
import com.tierconnect.riot.sdk.utils.UpVisibility;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AppgenExtends extends com.tierconnect.riot.appgen.service.Appgen
{
	public static void main( String args[] ) throws Exception
	{
		//HibernateSessionFactory.resource = args[0];
		AppgenExtends appgenExtends = new AppgenExtends();
		//Transaction transaction = ClazzService.getInstance().getClazzDAO().getSession().getTransaction();
		//transaction.begin();
		File dir = new File( System.getProperty( "user.dir" ) );
		File indir = new File( dir, args[0] );
		File outdir = new File( dir, args[1] );
		System.out.println( "INDIR=" + indir.getCanonicalPath() );
		System.out.println( "OUTDIR=" + outdir.getCanonicalPath() );
		
		appgenExtends.genApplication( indir, outdir );
		//transaction.commit();
		System.exit(0);
	}

	/**
	 * Factory method that creates the application. Should be extended by extending classes
	 * @return Application class instantiated
	 * @throws IOException
	 */
	public Application getApplication() throws IOException
	{			
		/**
		 * NOTES
		 * 
		 * 1. add new classes to hibernate.cfg.xml   TODO: do this programtically instead of thru config file !
		 * 2. to add to swagger, add to com.tierconnect.riot.iot.servlet.RiotRestEasyApplication 
		 * 
		 */
		
		List<Property> p;
		
		// KEEP IN ALPHABETICAL ORDER !!!

		Clazz favorite = new Clazz();
		favorite.setClassName("Favorite");
		favorite.setAboveVisibility(UpVisibility.FALSE);
		favorite.setBelowVisibility(true);
		favorite.setGenerateController(true);
		favorite.setGenerateControllerUpdate(false);
		favorite.setGenerateControllerSelect(false);
		favorite.setGenerateControllerDelete(false);
		favorite.setGenerateService(true);
		p = new ArrayList<>();
		p.add(property("Long","id", new String[]{"@Id","@GeneratedValue"}));
		p.add(property("Long","elementId"));
		p.add(property("String","typeElement"));
		p.add( property( "User", "user", new String [] { "@ManyToOne(fetch=FetchType.LAZY)", "@NotNull" } ) );
		p.add(property("Long","date"));
		p.add(property("Long","sequence"));
		p.add(property("String","elementName"));
		p.add( property("Group","group", new String [] { "@ManyToOne(fetch=FetchType.LAZY)", "@NotNull" } ) );
		p.add(property("String","status"));

		favorite.setProperties(p);

		
		Clazz field = new Clazz();
		field.setClassName( "Field" );
		field.setAboveVisibility(UpVisibility.TRUE_R);
		field.setBelowVisibility(true);
		p = new ArrayList<Property>();
		p.add( property( "Long", "id", new String [] { "@Id", "@GeneratedValue" } ) );
		p.add( property( "String", "name" ) );
		p.add( property( "String", "description" ) );
		p.add( property( "Long", "editLevel" ) );
		p.add( property( "String", "module" ) );
		p.add( property( "String", "type" ) );
		p.add( property( "Boolean", "userEditable" ) );
		p.add(property("Group", "group", new String[]{"@ManyToOne(fetch=FetchType.LAZY)", "@NotNull"}));
		p.add( property( "Field", "parentField", new String[]{"@ManyToOne(fetch=FetchType.LAZY)"}));
		field.setGenerateControllerAll(false);
		field.setGenerateControllerSelect(true);
		field.setGenerateControllerList(true);
		field.setGenerateControllerUpdate(true);
		field.setProperties(p);

		Clazz group = new Clazz();
		group.setClassName( "Group" );
		group.setImplement("java.io.Serializable");
		group.setAboveVisibility(UpVisibility.TRUE_R);
		group.setBelowVisibility(true);
        group.setImports(new String[]{"com.tierconnect.riot.appcore.entities.*", "java.util.*"});
		//group.setImports( new String [] { "java.util.Set" } );
		p = new ArrayList<Property>();
		p.add( property( "Long", "id", new String [] { "@Id", "@GeneratedValue" } ) );
		p.add( property( "boolean", "archived" ) );
		p.add( property( "String", "name" ) );
		p.add( property( "String", "code" ) );
		p.add( property( "String", "description" ) );
		p.add( property( "Group", "parent", new String [] { "@ManyToOne(fetch=FetchType.LAZY)" } ) );
		p.add( property( "GroupType", "groupType", new String [] { "@ManyToOne(fetch=FetchType.LAZY)" } ) );
		p.add( property( "protected", "Group", "parentLevel1", new String [] { "@ManyToOne(fetch=FetchType.LAZY)" } ) );
		p.add( property( "protected", "Group", "parentLevel2", new String [] { "@ManyToOne(fetch=FetchType.LAZY)" } ) );
		p.add( property( "protected", "Group", "parentLevel3", new String [] { "@ManyToOne(fetch=FetchType.LAZY)" } ) );
		p.add( property( "protected", "Group", "parentLevel4", new String [] { "@ManyToOne(fetch=FetchType.LAZY)" } ) );
		p.add( property( "protected", "Group", "parentLevel5", new String [] { "@ManyToOne(fetch=FetchType.LAZY)" } ) );
		p.add( property( "protected", "Group", "parentLevel6", new String [] { "@ManyToOne(fetch=FetchType.LAZY)" } ) );
		p.add( property( "protected", "Group", "parentLevel7", new String [] { "@ManyToOne(fetch=FetchType.LAZY)" } ) );
		p.add( property( "protected", "Group", "parentLevel8", new String [] { "@ManyToOne(fetch=FetchType.LAZY)" } ) );
		p.add( property( "protected", "Group", "parentLevel9", new String [] { "@ManyToOne(fetch=FetchType.LAZY)" } ) );
		p.add( property( "protected", "Group", "parentLevel10", new String [] { "@ManyToOne(fetch=FetchType.LAZY)" } ) );
		p.add( property( "int", "treeLevel" ) );
        p.add( property( "String", "hierarchyName", new String[]{"@Column(unique=true)"} ) );
        p.add( property( "Set<GroupResources>", "groupResources",
                new String [] { "@OneToMany(mappedBy=\"group\", " +
                        "fetch=FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval=true)" } ) );
        group.setProperties( p );

        Clazz groupResources = new Clazz();
        groupResources.setClassName("GroupResources");
		groupResources.setTableName("apc_groupresources");
        groupResources.setAboveVisibility(UpVisibility.TRUE_R);
        groupResources.setBelowVisibility(true);
        groupResources.setGenerateControllerInsert(false);
        groupResources.setGenerateControllerDelete( false );
        groupResources.setImports(new String[]{"java.util.Set", "com.tierconnect.riot.appcore.entities.*"});
        groupResources.setAnnotations(new String[]{"@Cacheable(value = false)"});

        p = new ArrayList<Property>();
        p.add( property( "Long", "id", new String [] { "@Id", "@GeneratedValue" } ) );
        p.add( property( "byte[]", "imageIcon" , new String[] { "@Column(length = 1048576)" } ) );
        p.add( property( "String", "imageTemplateName" ) );
        p.add (property( "Group", "group", new String[]{"@ManyToOne(fetch=FetchType.LAZY)", "@NotNull"}));
        groupResources.setProperties(p);

		Clazz groupType = new Clazz();
		groupType.setClassName( "GroupType" );
		groupType.setImplement("java.io.Serializable");
		groupType.setTableName("grouptype");
		groupType.setAboveVisibility(UpVisibility.TRUE_R);
		groupType.setBelowVisibility(true);
		groupType.setGenerateControllerInsert(false);
		p = new ArrayList<Property>();
		p.add( property( "Long", "id", new String [] { "@Id", "@GeneratedValue" } ) );
		p.add( property( "boolean", "archived" ) );
		p.add( property( "String", "name" ) );
		p.add( property( "String", "description" ) );
		p.add( property( "String", "code" ) );
		p.add( property( "Group", "group", new String [] { "@ManyToOne(fetch=FetchType.LAZY)" } ) );
		p.add( property( "GroupType", "parent", new String [] { "@ManyToOne(fetch=FetchType.LAZY)" } ) );
		groupType.setProperties( p );
		
		Clazz groupField = new Clazz();
		groupField.setClassName( "GroupField" );
		groupField.setTableName("groupfield");
        groupField.setGenerateController(false);
		groupField.setAboveVisibility(UpVisibility.TRUE_R);
		groupField.setBelowVisibility(true);
		p = new ArrayList<Property>();
		p.add( property( "Long", "id", new String [] { "@Id", "@GeneratedValue" } ) );
		p.add( property( "String", "value" ) );
		p.add( property( "Group", "group", new String [] { "@ManyToOne(fetch=FetchType.LAZY, optional = false)", "@NotNull" } ) );
		p.add( property( "Field", "field", new String [] { "@ManyToOne(fetch=FetchType.LAZY)", "@NotNull" } ) );
		groupField.setProperties( p );


		Clazz recent = new Clazz();
		recent.setClassName("Recent");
		recent.setAboveVisibility(UpVisibility.FALSE);
		recent.setBelowVisibility(true);
		recent.setGenerateController(true);
		recent.setGenerateControllerDelete(false);
		recent.setGenerateControllerUpdate(false);
		recent.setGenerateControllerSelect(false);
		recent.setGenerateService(true);
		p = new ArrayList<>();
		p.add(property("Long","id", new String[]{"@Id","@GeneratedValue"}));
		p.add(property("Long","elementId"));
		p.add(property("String","typeElement"));
		p.add( property("User", "user", new String [] { "@ManyToOne(fetch=FetchType.LAZY)", "@NotNull" } ) );
		p.add(property("Long","date"));
		p.add(property("String","elementName"));
		p.add(property("Long","elementGroupId"));

		recent.setProperties(p);
		
		Clazz resource = new Clazz();
		resource.setClassName( "Resource" );
		resource.setAboveVisibility(UpVisibility.TRUE_R);
		resource.setBelowVisibility(true);
		resource.setImports( new String [] { "java.util.Set" } );
        resource.setCheckNullValue(false);
		p = new ArrayList<Property>();
		p.add( property( "Long", "id", new String [] { "@Id", "@GeneratedValue" } ) );
		p.add( property( "String", "name" ) );
        p.add( property( "String", "label" ) );
		p.add( property( "String", "description", new String [] { "@Column(name=\"description\", length = 500)" } ) );
		p.add( property( "String", "acceptedAttributes" ) );
		p.add( property( "String", "fqname" ) );
		p.add( property( "int", "type" ) );
		p.add( property( "Group", "group", new String [] { "@ManyToOne(fetch=FetchType.LAZY)" } ) );
        p.add( property( "Long", "typeId" ) );
		p.add( property( "int", "treeLevel" ) );
		p.add( property( "Resource", "parent", new String [] { "@ManyToOne(fetch=FetchType.LAZY)" } ) );
		resource.setProperties( p );
		resource.setGenerateControllerAll(false);
		resource.setGenerateControllerSelect(true);
		resource.setGenerateControllerList(true);
        resource.setGenerateControllerUpdate(true);

		Clazz role = new Clazz();
		role.setClassName( "Role" );
		role.setAboveVisibility(UpVisibility.TRUE_R);
		role.setBelowVisibility(true);
		role.setImports(new String[]{"java.util.Set", "org.hibernate.annotations.Cache", "org.hibernate.annotations" +
				".CacheConcurrencyStrategy"});
		role.setCheckNullValue(false);
		role.setGenerateControllerInsert(false);
		p = new ArrayList<Property>();
		p.add( property( "Long", "id", new String [] { "@Id", "@GeneratedValue" } ) );
		p.add( property( "boolean", "archived" ) );
		p.add( property( "String", "name" ) );
		p.add( property( "String", "description" ) );
		p.add( property( "Group", "group", new String [] { "@ManyToOne(fetch=FetchType.LAZY)", "@NotNull" } ) );
		p.add( property( "GroupType", "groupTypeCeiling", new String [] { "@ManyToOne(fetch=FetchType.LAZY)"} ) );
		p.add(property("Set<RoleResource>", "roleResources", new String[]{"@OneToMany(mappedBy=\"role\", " +
				"fetch=FetchType.LAZY)", "@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)"}));
		role.setProperties( p );
		
		Clazz roleResource = new Clazz();
		roleResource.setClassName( "RoleResource" );
		roleResource.setTableName("roleresource");
        roleResource.setGenerateController(false);
		roleResource.setAboveVisibility(UpVisibility.TRUE_R);
		roleResource.setBelowVisibility(true);
        roleResource.setImports(new String[]{"java.util.Set",
                "org.hibernate.annotations.CacheConcurrencyStrategy",
                "org.hibernate.annotations.Cache"});
		p = new ArrayList<Property>();
		p.add( property( "Long", "id", new String [] { "@Id", "@GeneratedValue" } ) );
		p.add( property( "String", "permissions" ) );
		p.add( property( "Role", "role", new String [] { "@ManyToOne(fetch=FetchType.LAZY)", "@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)", "@NotNull" } ) );
		p.add( property( "Resource", "resource", new String [] { "@ManyToOne(fetch=FetchType.LAZY)", "@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)", "@NotNull" } ) );
		roleResource.setProperties( p );
		
		Clazz user = new Clazz();
		user.setClassName( "User" );
		user.setAboveVisibility(UpVisibility.FALSE);
		user.setBelowVisibility(true);
		user.setImports(new String[]{"java.util.Set",
				"javax.persistence.TemporalType",
				"org.hibernate.annotations.CacheConcurrencyStrategy",
				"org.hibernate.annotations.Cache"});
		p = new ArrayList<>();
		p.add( property( "Long", "id", new String [] { "@Id", "@GeneratedValue" } ) );
		p.add( property( "boolean", "archived" ) );
		p.add( property( "String", "username" ) );
		p.add( property( "String", "password", new String[]{"@Transient" }));
		p.add( property( "String", "apiKey" ) );
		p.add( property( "String", "firstName" ) );
		p.add( property( "String", "lastName" ) );
		p.add( property( "String", "hiddenTabs" ) );
		//p.add( property( "String", "middleName" ) );
		p.add( property( "String", "email" ) );
		p.add( property( "String", "timeZone" ) );
		p.add( property( "String", "dateFormat" ) );
		p.add(property("Group", "group", new String[]{"@ManyToOne(fetch=FetchType.LAZY)", "@Cache(usage = " +
				"CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)", "@NotNull"}));
		p.add(property("Group", "roamingGroup", new String[]{"@ManyToOne(fetch=FetchType.LAZY)", "@Cache(usage = " +
				"CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)"}));
		p.add( property( "Set<UserPassword>", "userPasswords", new String [] { "@OneToMany(mappedBy=\"user\", fetch=FetchType.LAZY)" } ) );
		p.add( property( "Set<UserRole>", "userRoles", new String [] { "@OneToMany(mappedBy=\"user\", fetch=FetchType.LAZY)" } ) );
		p.add( property( "Set<UserField>", "userFieldUsers", new String [] { "@OneToMany(mappedBy=\"user\", fetch=FetchType.LAZY)" } ) );
		user.setProperties( p );

		Clazz userPassword = new Clazz();
		userPassword.setClassName( "UserPassword" );
		userPassword.setAboveVisibility(UpVisibility.FALSE);
		userPassword.setGenerateController(false);
		userPassword.setBelowVisibility(true);
		userPassword.setImports(new String[]{"java.util.Set",
			"javax.persistence.TemporalType",
			"org.hibernate.annotations.CacheConcurrencyStrategy",
			"org.hibernate.annotations.Cache"});
		p = new ArrayList<>();
		p.add( property( "Long", "id", new String [] { "@Id", "@GeneratedValue" } ) );
		p.add( property( "String", "hashedPassword" ) );
		p.add( property( "String", "status" ) );
		p.add( property( "Long", "creationTime" ));
		p.add( property( "Long", "failedAttempts" ));
		p.add( property( "Long", "lastFailedTime" ));
		p.add(property("User", "user", new String[]{"@ManyToOne(fetch=FetchType.LAZY)", "@Cache(usage = " +
			"CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)", "@NotNull"}));
		userPassword.setProperties( p );

		Clazz userField = new Clazz();
		userField.setClassName( "UserField" );
		userField.setTableName("userfield");
        userField.setGenerateController(false);
		userField.setAboveVisibility(UpVisibility.FALSE);
		userField.setBelowVisibility(true);
		p = new ArrayList<Property>();
		p.add( property( "Long", "id", new String [] { "@Id", "@GeneratedValue" } ) );
		p.add( property( "String", "value" ) );
		p.add( property( "User", "user", new String [] { "@ManyToOne(fetch=FetchType.LAZY)", "@NotNull" } ) );
		p.add( property( "Field", "field", new String [] { "@ManyToOne(fetch=FetchType.LAZY)", "@NotNull" } ) );
		userField.setProperties( p );
		
		Clazz userRole = new Clazz();
		userRole.setClassName( "UserRole" );
		userRole.setAboveVisibility(UpVisibility.TRUE_R);
		userRole.setBelowVisibility(true);
		p = new ArrayList<Property>();
		p.add( property( "Long", "id", new String [] { "@Id", "@GeneratedValue" } ) );
		p.add( property( "User", "user", new String [] { "@ManyToOne(fetch=FetchType.LAZY)", "@NotNull" } ) );
		p.add( property( "Role", "role", new String [] { "@ManyToOne(fetch=FetchType.LAZY)", "@NotNull" } ) );
		userRole.setProperties( p );
		userRole.setGenerateController(false);

        Clazz token = new Clazz();
        token.setClassName( "Token" );
        token.setAboveVisibility(UpVisibility.FALSE);
        token.setBelowVisibility(false);
        token.setImports(new String[]{"java.util.Set", "javax.persistence.TemporalType"});
        p = new ArrayList<Property>();
        p.add( property( "Long", "id", new String [] { "@Id", "@GeneratedValue" } ) );
        p.add( property( "String", "tokenString", new String[]{"@Column(unique=true)"} ) );
        p.add( property( "Date", "creationTime", new String[] {"@Temporal(TemporalType.TIMESTAMP)"}));
        p.add( property( "Date", "tokenExpirationTime", new String[] {"@Temporal(TemporalType.TIMESTAMP)"}));
        p.add( property( "Boolean", "tokenActive", new String[] {"@Column(columnDefinition=\"boolean default true\")", "@NotNull" }));
        p.add( property( "User", "user", new String [] { "@ManyToOne(fetch=FetchType.LAZY)", "@NotNull" } ) );
        token.setProperties( p );
        token.setGenerateController(false);


        Clazz version = new Clazz();
        version.setClassName("Version");
		version.setTableName("version");
        version.setGenerateController(false);
        version.setAboveVisibility(UpVisibility.FALSE);
        version.setBelowVisibility(false);
        p = new ArrayList<Property>();
        p.add( property( "Long", "id", new String [] { "@Id", "@GeneratedValue" } ) );
        p.add( property( "String", "dbVersion" ) );
        p.add( property( "String", "prevDbVersion" ) );
        p.add( property( "String", "versionName" ) );
        p.add( property( "String", "versionDesc" ) );
        p.add( property( "Date", "installTime", new String[] {"@Temporal(TemporalType.TIMESTAMP)"}));
        p.add( property( "String", "computerName" ) );
        p.add( property( "String", "computerIP" ) );
        p.add( property( "String", "computerUser" ) );
        p.add( property( "String", "gitBranch" ) );
        version.setProperties( p );


        Clazz migrationResult = new Clazz();
        migrationResult.setClassName( "MigrationStepResult" );
        migrationResult.setTableName("migration_step_result");
        migrationResult.setAboveVisibility(UpVisibility.FALSE);
        migrationResult.setBelowVisibility(false);
        migrationResult.setImports(new String[]{"java.util.Set"});
        p = new ArrayList<Property>();
        p.add( property( "Long", "id", new String [] { "@Id", "@GeneratedValue" } ) );
        p.add( property( "String", "migrationPath") );
        p.add( property( "String", "migrationResult", new String[] { "@Column(length = 4000)"}) );
        p.add( property( "String", "description"));
        p.add( property( "String", "hash"));
        p.add( property( "String", "message", new String[] { "@Column(length = 4000)"}) );
        p.add( property( "String", "stackTrace", new String[] { "@Column(length = 4000)"}) );
        p.add( property( "Version", "version", new String [] { "@ManyToOne(fetch=FetchType.LAZY)", "@NotNull" } ) );
        migrationResult.setProperties( p );
        migrationResult.setGenerateController(false);


		Clazz license = new Clazz();
		license.setClassName("License");
		license.setAboveVisibility(UpVisibility.FALSE);
		license.setBelowVisibility(true);
		p = new ArrayList<Property>();
		p.add(property("Long", "id", new String[]{"@Id", "@GeneratedValue"}));
		p.add(property("Group", "group", new String[]{"@ManyToOne(fetch=FetchType.LAZY)", "@NotNull"}));
		p.add(property("String", "licenseString", new String[] { "@Column(length = 8000)"}));
		p.add( property( "Date", "installTime", new String[] {"@Temporal(TemporalType.TIMESTAMP)"}));
		license.setProperties(p);
		license.setGenerateControllerAll(false);
		license.setGenerateControllerSelect(true);
		license.setGenerateControllerList(true);

		//Connection
		Clazz connection = new Clazz();
		connection.setClassName("Connection");
		connection.setAboveVisibility(UpVisibility.FALSE);
		connection.setBelowVisibility(true);
		p = new ArrayList<Property>();
		p.add(property("Long", "id", new String[]{"@Id", "@GeneratedValue"}));
		p.add(property("Group", "group", new String[]{"@ManyToOne(fetch=FetchType.LAZY)", "@NotNull"}));
		p.add(property("String", "name"));
		p.add(property("String", "code", new String[] {"@Column(unique=true)"}));
		p.add(property("String", "properties", new String[]{"@Column(length = 8000)"}));
		p.add(property("ConnectionType", "connectionType", new String [] { "@ManyToOne(fetch=FetchType.LAZY)", "@NotNull" } ) );
		connection.setProperties(p);

		//Connection
		Clazz connectionType = new Clazz();
		connectionType.setClassName("ConnectionType");
		connectionType.setAboveVisibility(UpVisibility.TRUE_R);
		connectionType.setBelowVisibility(true);
		p = new ArrayList<Property>();
		p.add(property("Long", "id", new String[]{"@Id", "@GeneratedValue"}));
		p.add(property("Group", "group", new String[]{"@ManyToOne(fetch=FetchType.LAZY)", "@NotNull"}));
		p.add(property("String", "description"));
		p.add(property("String", "code", new String[] {"@Column(unique=true)"}));
		p.add(property("String", "propertiesDefinitions", new String[]{"@Column(length = 8000)"}));
		p.add(property("Boolean", "requiredTestOnCreateEdit"));
		connectionType.setProperties(p);



		//classOfProperty.put("name",String.class);
		//classOfProperty.put("thingTypeId",Long.class);
		//classOfProperty.put("isRoot",Long.class);
		//classOfProperty.put("favOrRec",String.class);
		//classOfProperty.put("timestamp",Long.class);


		/*
		//Connection
		Clazz datasource = new Clazz();
		datasource.setClassName("Datasource");
		datasource.setTableName("datasource");
		datasource.setAboveVisibility(UpVisibility.TRUE_R);
		datasource.setBelowVisibility(true);
		p = new ArrayList<Property>();
		p.add(property("Long", "id", new String[]{"@Id", "@GeneratedValue"}));
		p.add(property("Group", "group", new String[]{"@ManyToOne", "@NotNull"}));
		p.add(property("String", "name"));
		p.add(property("String", "properties", new String[]{"@Column(length = 8000)"}));
		p.add(property("Connection", "connection", new String [] { "@ManyToOne", "@NotNull" } ) );
		p.add(property("DatasourceType", "datasourceType", new String [] { "@ManyToOne", "@NotNull" } ) );
		datasource.setProperties(p);

		//ConnectionType
		Clazz datasourceType = new Clazz();
		datasourceType.setClassName("DatasourceType");
		datasourceType.setTableName("datasource_type");
		datasourceType.setAboveVisibility(UpVisibility.TRUE_R);
		datasourceType.setBelowVisibility(true);
		p = new ArrayList<Property>();
		p.add(property("Long", "id", new String[]{"@Id", "@GeneratedValue"}));
		p.add(property("Group", "group", new String[]{"@ManyToOne", "@NotNull"}));
		p.add(property("String", "name"));
		p.add(property("String", "propertiesDefinitions", new String[] { "@Column(length = 8000)"}));
		p.add(property("ConnectionType", "connectionType", new String [] { "@ManyToOne", "@NotNull" } ) );
		datasourceType.setProperties(p);
		datasourceType.setGenerateControllerAll(false);
		datasourceType.setGenerateControllerSelect(true);
		datasourceType.setGenerateControllerList(true);
		*/

		AppgenPackage pk1 = new AppgenPackage( "com.tierconnect.riot.appcore" );
		
		pk1.addClazz(favorite);
		pk1.addClazz( field );
		pk1.addClazz( group );
		pk1.addClazz( groupResources );
		pk1.addClazz( groupField );
		pk1.addClazz( groupType );
		pk1.addClazz( resource );
		pk1.addClazz( role );
		pk1.addClazz( roleResource );
		pk1.addClazz( user );
		pk1.addClazz( userPassword );
		pk1.addClazz( userField );
		pk1.addClazz( userRole );
        pk1.addClazz( token);
		pk1.addClazz(recent);

        pk1.addClazz( version );
        pk1.addClazz( migrationResult);
        pk1.addClazz( license );
		pk1.addClazz( connectionType );
		pk1.addClazz( connection );
		/*
		pk1.addClazz( datasourceType );
		pk1.addClazz( datasource );
		*/

		Application app = new Application();
		app.addPackage( pk1 );
		
		return app;
	}
}
