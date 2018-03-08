package com.tierconnect.riot.iot.appgen;

import com.tierconnect.riot.appcore.appgen.AppgenExtends;
import com.tierconnect.riot.appgen.model.AppgenPackage;
import com.tierconnect.riot.appgen.model.Application;
import com.tierconnect.riot.appgen.model.Clazz;
import com.tierconnect.riot.appgen.model.Property;
import com.tierconnect.riot.sdk.utils.UpVisibility;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AppgenIot extends com.tierconnect.riot.appgen.service.Appgen {
	public static void main(String args[]) throws Exception {
		// HibernateSessionFactory.resource = args[0];
		//todo make appgenIot a interface... instanciate with factory. This way we only have one main
		AppgenIot appgenIot = new AppgenIot();
		// Transaction transaction =
		// ClazzService.getInstance().getClazzDAO().getSession().getTransaction();
		// transaction.begin();
		File dir = new File(System.getProperty("user.dir"));
		File indir = new File(dir, args[0]);
		File outdir = new File(dir, args[1]);
		System.out.println("INDIR=" + indir.getCanonicalPath());
		System.out.println("OUTDIR=" + outdir.getCanonicalPath());

		appgenIot.genApplication(indir, outdir);
		// transaction.commit();
		System.exit(0);
	}

	/**
	 * When adding a new class, also add to hibernate.cfg.xml and
	 * RiotResetEasyApplication.java
	 *
	 * @throws IOException
	 */
	public Application getApplication() throws IOException
	{
		/**
		 * NOTES
		 *
		 * 1. add new classes to hibernate.cfg.xml TODO: do this programtically
		 * instead of thru config file ! 2. to add to swagger, add to
		 * com.tierconnect.riot.iot.servlet.RiotRestEasyApplication
		 *
		 */

		List<Property> p;

        Clazz gateway = new Clazz();
        gateway.setClassName("Edgebox");
        gateway.setAboveVisibility(UpVisibility.TRUE_R_U_SPECIAL);
        gateway.setBelowVisibility(true);
        gateway.setImports(new String[]{"java.util.List",
				"com.tierconnect.riot.appcore.entities.Group"});
        p = new ArrayList<Property>();
        p.add(property("Long", "id", new String[] { "@Id", "@GeneratedValue" }));
		p.add(property("Group", "group", new String [] { "@ManyToOne(fetch=FetchType.LAZY)" } ));
        p.add(property("String", "name"));
        p.add(property("String", "code"));
        // valid types are "core" and "edge"
        p.add(property("String", "type"));
		p.add(property("String", "parameterType"));
        p.add(property("String", "description"));
        p.add(property("String", "ipAddress"));
        p.add(property("String", "filterRule", new String[] { "@Column(length = 8000)"}));
        p.add(property("Boolean", "active"));
        p.add(property("String", "status"));
		p.add( property( "Long", "port" ) );
        p.add(property("String", "configuration", new String[] { "@Column(length = 8000)"}));
		p.add(property("List<EdgeboxRule>", "edgeboxRules",
				new String[]{"@OneToMany(mappedBy=\"edgebox\", fetch=FetchType.LAZY, cascade = CascadeType.ALL)"}));
        gateway.setProperties(p);

        Clazz rule = new Clazz();
		rule.setClassName("EdgeboxRule");
		rule.setAboveVisibility(UpVisibility.FALSE);
		rule.setBelowVisibility(true);
        rule.setImports(new String[]{"java.util.List", "com.tierconnect.riot.appcore.entities.Group"});
        p = new ArrayList<Property>();
        p.add(property("Long", "id", new String[] { "@Id", "@GeneratedValue" }));
        p.add(property("String", "name"));
        p.add(property("String", "input"));
        p.add(property("String", "output"));
        p.add(property("String", "outputConfig", new String[] { "@Column(length = 8000)" }));
        p.add(property("String", "rule", new String[] { "@Column(name=\"rule0\", length = 8000)"}));
        p.add(property("String", "cronSchedule"));
        p.add(property("String", "description"));
		p.add(property("Boolean", "active"));
		p.add(property("Boolean", "runOnReorder"));
		p.add(property("Boolean", "serialExecution") );
		// this is used to order the rules for the Esper engine
		p.add(property("int", "sortOrder"));
		p.add(property("Boolean", "honorLastDetect"));
		p.add(property("Boolean", "executeLoop"));
		p.add(property("String", "conditionType"));
		p.add(property("String", "parameterConditionType"));
        p.add(property("Group", "group", new String [] { "@ManyToOne(fetch=FetchType.LAZY)" } ));
        p.add(property("Edgebox", "edgebox", new String[]{"@ManyToOne(fetch=FetchType.LAZY)"}));
		p.add(property("ScheduledRule", "scheduledRule", new String[]{"@ManyToOne(fetch=FetchType.LAZY)"}));
        rule.setProperties( p );

		Clazz importExport = new Clazz();
		importExport.setClassName( "ImportExport" );
		importExport.setAboveVisibility(UpVisibility.FALSE);
		importExport.setBelowVisibility(true);
		importExport.setGenerateController(false);
		importExport.setGenerateService(true);

		p = new ArrayList<Property>();
		p.add(property("Long","id", new String[] { "@Id", "@GeneratedValue" } ) );
		p.add(property("Long","userId"));
		p.add(property("String", "type"));
		p.add(property("Long","totalRecord"));
		p.add(property("Long","errorRecord"));
		p.add(property("Long","successRecord"));
		p.add(property("String","processType"));
		p.add(property("Date","startDate"));
		p.add(property("Date","endDate"));
		p.add(property("Long","duration"));

		importExport.setProperties(p);

		p = new ArrayList<Property>();
		p.add(property("Long","id", new String[] { "@Id", "@GeneratedValue" } ) );
		p.add(property("Long","userId"));
		p.add(property("String", "type"));
		p.add(property("Long","totalRecord"));
		p.add(property("Long","errorRecord"));
		p.add(property("Long","successRecord"));
		p.add(property("String","processType"));
		p.add(property("Date","startDate"));
		p.add(property("Date","endDate"));
		p.add(property("Long","duration"));

		importExport.setProperties(p);

		// KEEP IN ALPHABETICAL ORDER !!!

		Clazz localMap = new Clazz();
		localMap.setClassName( "LocalMap" );
		localMap.setAboveVisibility( UpVisibility.TRUE_R );
		localMap.setBelowVisibility( true );
		localMap.setGenerateControllerDelete(false);
		localMap.setImports( new String[] { "java.util.List",
                "java.util.Set",
                "com.tierconnect.riot.iot.entities.LocalMapPoint",
                "com.tierconnect.riot.appcore.entities.Group", "javax.persistence.*" } );
		p = new ArrayList<Property>();
		p.add( property( "Long", "id", new String[] { "@Id", "@GeneratedValue" } ) );
		p.add( property( "Group", "group", new String[] { "@ManyToOne(fetch=FetchType.LAZY)" } ) );
		p.add(property("String", "name"));
		p.add(property("String", "description"));
		p.add(property("byte[]", "image", new String [] { "@Lob" }));
		p.add(property("Double", "lonOrigin"));
		p.add(property("Double", "latOrigin"));
		p.add(property("Double", "altOrigin"));
		p.add(property("Double", "declination"));
        p.add(property("Double", "imageWidth"));
        p.add(property("Double", "imageHeight"));
		p.add(property("Double", "xNominal"));
		p.add(property("Double", "yNominal"));
		p.add(property("Double", "latOriginNominal"));
        p.add(property("Double", "lonOriginNominal"));
        p.add(property("String", "imageUnit")); //feet, m, cm...
		p.add(property("Double", "lonmin"));
		p.add(property("Double", "lonmax"));
		p.add(property("Double", "latmin"));
		p.add(property("Double", "latmax"));
		p.add(property("Long", "opacity"));
		p.add(property("Double","rotationDegree"));
		p.add(property( "Long", "modifiedTime" ) );

        p.add(property(
                "List<ZoneGroup>",
                "zoneGroup",
                new String[] { "@OneToMany(mappedBy=\"localMap\", fetch=FetchType.LAZY, cascade = CascadeType.ALL)" }));

        p.add(property(
                "Set<LocalMapPoint>",
                "localMapPoints",
                new String[]{"@OneToMany(mappedBy=\"localMap\", fetch=FetchType.LAZY, cascade = CascadeType.ALL)"}));
		localMap.setProperties( p );

        Clazz localMapPoint = new Clazz();
        localMapPoint.setClassName("LocalMapPoint");
        localMapPoint.setAboveVisibility( UpVisibility.FALSE);
        localMapPoint.setBelowVisibility(true);
        localMapPoint.setImports( new String[] { "java.util.List", "java.util.Set",
                "org.hibernate.annotations.Cache",
                "com.tierconnect.riot.appcore.entities.Group",
                "org.hibernate.annotations.CacheConcurrencyStrategy" });
        p = new ArrayList<Property>();
        p.add( property( "Long", "id", new String[] { "@Id", "@GeneratedValue" } ) );
        p.add( property( "Double", "x" ) );
        p.add( property( "Double", "y" ) );
        p.add( property( "Long", "arrayIndex" ) );
        p.add( property( "LocalMap", "localMap", new String[] { "@ManyToOne(fetch=FetchType.LAZY)" } ) );
        localMapPoint.setProperties(p);

        //Logical Reader table

        Clazz logicalReader = new Clazz();
        logicalReader.setClassName( "LogicalReader" );
		logicalReader.setTableAnnotations(new String[]{"uniqueConstraints={@UniqueConstraint(columnNames={\"code\",\"group_id\"})","@UniqueConstraint(columnNames={\"name\",\"group_id\"})}"});
        logicalReader.setAboveVisibility( UpVisibility.FALSE );
        logicalReader.setBelowVisibility( true );

        logicalReader.setImports( new String[] { "java.util.List",
        		"com.tierconnect.riot.appcore.entities.Group",
        		"org.hibernate.annotations.Cache",
        		"org.hibernate.annotations.CacheConcurrencyStrategy"} );

        p = new ArrayList<Property>();
        p.add( property( "Long", "id", new String[] { "@Id", "@GeneratedValue" } ) );
        p.add( property( "Group", "group", new String[] { "@ManyToOne(fetch=FetchType.LAZY)" } ) );
        p.add(property("String", "name"));
        p.add(property("String", "code"));

        p.add(property("Double", "x"));
        p.add(property("Double", "y"));
        p.add(property("Double", "z"));

        p.add( property( "Zone", "zoneIn", new String [] { "@ManyToOne(fetch=FetchType.LAZY)", "@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)" } ) );
        p.add( property( "Zone", "zoneOut", new String [] { "@ManyToOne(fetch=FetchType.LAZY)", "@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)" } ) );

        logicalReader.setProperties( p );


        Clazz pickListFields = new Clazz();
        pickListFields.setClassName( "PickListFields" );
        pickListFields.setAboveVisibility(UpVisibility.FALSE);
        pickListFields.setBelowVisibility( true );
        pickListFields.setImports( new String[] { "javax.persistence.*" } );

        p = new ArrayList<Property>();

        p.add( property( "Long", "id", new String[] { "@Id", "@GeneratedValue" } ) );
        p.add( property( "Long", "thingFieldId" ) );
//        p.add(property("String", "fieldsStored", new String [] { "@Column(length=36500)" }));
        p.add(property("String", "fieldsStored", new String[] { "@Column(name=\"fieldsStored0\", length = 8000)"}));
        pickListFields.setProperties( p );


		Clazz reportDefinition = new Clazz();
		reportDefinition.setClassName( "ReportDefinition" );
		reportDefinition.setAboveVisibility(UpVisibility.TRUE_R_U_SPECIAL);
		reportDefinition.setBelowVisibility( true );
//		reportDefinition.setImports( new String[] { "java.util.List", "com.tierconnect.riot.appcore.entities.*", "javax.persistence.*" } );
		reportDefinition.setImports( new String[] { "java.util.List", "java.util.Set",
				"org.hibernate.annotations.Cache",
				"com.tierconnect.riot.appcore.entities.*",
				"org.hibernate.annotations.CacheConcurrencyStrategy",
				"javax.persistence.*" });

		p = new ArrayList<Property>();
		p.add( property( "Long", "id", new String[] { "@Id", "@GeneratedValue" } ) );
        p.add( property( "User", "createdByUser", new String[] { "@ManyToOne(fetch=FetchType.LAZY)", "@NotNull" } ) );
        p.add(property("String", "name"));
		p.add(property("String", "description", new String[] { "@Column(name=\"description\", length = 8000)"}));
		p.add(property("String", "chartSummarizeBy"));
		p.add(property("String", "chartFunction"));
		p.add(property("String", "chartType"));
		p.add(property("String", "chartSubType"));
		p.add(property("String", "chartOrientation"));
		p.add(property("String", "pinLabel"));
		p.add(property("Long","localMapId"));

        p.add(property("Integer","clusterDistance"));
        p.add(property("String","mapUnit"));
		//Default Zoom Level
		p.add(property("Long", "defaultZoom"));
		p.add(property("String", "centerLat"));
		p.add(property("String", "centerLon"));

		//Pin Appearance
        p.add(property("String", "pinStyle"));
		p.add(property("String", "defaultTypeIcon"));
		p.add(property("String", "defaultColorIcon"));

		//Default configurations
		p.add(property("Boolean", "pinLabels"));
		p.add(property("Boolean", "zoneLabels"));
		p.add(property("Boolean", "trails"));
		p.add(property("Boolean", "clustering"));
		p.add(property("Boolean", "playback"));
		p.add(property("Boolean", "nupYup"));
		p.add(property("Boolean", "defaultList"));

        p.add(property("Boolean", "pinIcons"));
        p.add(property("String", "pinDisplay"));
        p.add(property("String", "zoneDisplay"));
		p.add(property("Long", "typeOrder"));

        p.add(property("Boolean", "interpolation"));
        p.add(property("Boolean", "runOnLoad"));
        p.add(property("Boolean", "fillHistoryData"));

//        p.add( property("String", "mapViewProperties" , new String [] { "@columnDefinition='NVARCHAR(MAX)'}" } ) );

//        p.add(property("String", "mapViewProperties", new String [] { "nvarchar(255)" }));
        p.add(property("String", "mapViewProperties", new String[] { "@Column(name=\"mapViewProperties0\", length = 8000)"}));

        //ReportDef configuration
        p.add(property("Long", "maxRecords"));

        //LiveMap
        p.add(property("Boolean","liveMap"));
        p.add(property("Integer","autoRefreshInterval"));

        //Edit Available button
        p.add(property("Boolean", "editInline"));
		//Mark which reports are going to be available in mobile versions
		p.add(property("Boolean", "isMobile"));
		p.add(property("Boolean", "isMobileDataEntry"));

        //Object visibility
        p.add( property( "GroupType", "groupTypeFloor", new String [] { "@ManyToOne(fetch=FetchType.LAZY)" } ) );
		p.add( property( "Role", "roleShare", new String [] { "@ManyToOne(fetch=FetchType.LAZY)" } ) );
		p.add( property( "Group", "groupShare", new String [] { "@ManyToOne(fetch=FetchType.LAZY)" } ) );
		p.add( property( "Shift", "shift", new String [] { "@ManyToOne(fetch=FetchType.LAZY)" } ) );

		//Report Type -> (Map or Table or Both)
		p.add(property("String", "reportType"));

        p.add(property("Integer", "fontSizeText"));

		p.add(property("Group", "group", new String[] { "@ManyToOne(fetch=FetchType.LAZY)",
				"@NotNull" }));
		/*Changes to show total columns vertical and/or horizontal*/
		p.add(property("Boolean", "verticalTotal"));
		p.add(property("Boolean", "horizontalTotal"));
		/*Time Out cache*/
		p.add(property("Integer", "timeoutCache"));

		p.add(property("Integer", "zoneOpacity"));
		p.add(property("Integer", "mapOpacity"));
		p.add(property("Boolean", "editInLineEntryForm"));
		p.add(property(
				"List<ReportFilter>",
				"reportFilter",
				new String[] { "@OneToMany(mappedBy=\"reportDefinition\", fetch=FetchType.LAZY, cascade = CascadeType.ALL)" }));
		p.add(property(
				"List<ReportGroupBy>",
				"reportGroupBy",
				new String[] { "@OneToMany(mappedBy=\"reportDefinition\", fetch=FetchType.LAZY, cascade = CascadeType.ALL)" }));
		p.add(property(
				"List<ReportProperty>",
				"reportProperty",
				new String[]{"@OneToMany(mappedBy=\"reportDefinition\", fetch=FetchType.LAZY, cascade = CascadeType.ALL)"}));
		p.add(property(
				"List<ReportRule>",
				"reportRule",
				new String[]{"@OneToMany(mappedBy=\"reportDefinition\", fetch=FetchType.LAZY, cascade = CascadeType.ALL)"}));
		p.add(property("ReportDefinition", "parent",
				new String[] { "@ManyToOne(fetch=FetchType.LAZY)" }));
		p.add(property(
				"List<ReportEntryOption>",
				"reportEntryOption",
				new String[]{"@OneToMany(mappedBy=\"reportDefinition\", fetch=FetchType.LAZY, cascade = CascadeType.ALL)"}));
		p.add(property(
				"List<ReportDefinitionConfig>",
				"reportDefinitionConfig",
				new String[]{"@OneToMany(mappedBy=\"reportDefinition\", fetch=FetchType.LAZY, cascade = CascadeType.ALL)"}));

		p.add(property(
				"List<ReportActions>",
				"reportActionsList",
				new String[]{"@OneToMany(mappedBy=\"reportDefinition\", fetch=FetchType.LAZY, cascade = CascadeType.ALL)"}));

		p.add(property(
				"List<ReportCustomFilter>",
				"reportCustomFilter",
				new String[] { "@OneToMany(mappedBy=\"reportDefinition\", fetch=FetchType.LAZY, cascade = CascadeType.ALL)" }));

		p.add(property("String", "schedule"));
		p.add(property("String", "emails"));
		p.add(property("User", "emailRunAs", new String[]{"@ManyToOne(fetch=FetchType.LAZY)"}));

		p.add(property("boolean", "delete", new String[]{"@Column(name=\"delete0\")", "@NotNull"}));
		p.add(property("boolean", "rfidPrint"));
		p.add(property("boolean", "dismiss"));
		p.add(property("Integer", "playbackMaxThing"));
		p.add(property("boolean", "bulkEdit"));
		p.add(property("String", "chartViewProperties", new String[]{"@Column(name=\"chartViewProperties0\", length = 8000)"}));

		p.add(property("boolean", "heatmap"));
		p.add(property("Integer", "heatmapBlur"));
		p.add(property("Integer", "heatmapRadio"));
		p.add(property("String", "heatmapLabel"));
		p.add(property("Folder", "folder", new String[] { "@ManyToOne(fetch=FetchType.LAZY)"}));

		p.add(property("boolean", "dateFormatColumns"));

        reportDefinition.setProperties( p );

		Clazz reportFilter = new Clazz();
		reportFilter.setClassName("ReportFilter" );
		reportFilter.setAboveVisibility(UpVisibility.TRUE_R );
		reportFilter.setBelowVisibility(true);
		reportFilter.setGenerateController(false);
		p = new ArrayList<Property>();
		p.add(property("Long", "id", new String[] { "@Id", "@GeneratedValue" }));
		p.add(property("String", "propertyName"));
		p.add(property("ThingType", "thingType", new String[] { "@ManyToOne(fetch=FetchType.LAZY)" }));
		p.add(property("ThingTypeField", "thingTypeField", new String[] { "@ManyToOne(fetch=FetchType.LAZY)" }));
		p.add(property("String", "label"));
		p.add(property("Integer", "fieldType"));

		// Rename order to displayOrder -> Because order is a reserved word
		p.add(property("Float", "displayOrder"));
		p.add(property("String", "operator"));
		p.add(property("String", "value"));
		p.add(property("Boolean", "editable"));
        p.add(property("Boolean", "autoComplete"));
		//p.add(property("String", "parentThingTypeCode"));
		p.add(property("ThingType", "parentThingType", new String[] { "@ManyToOne(fetch=FetchType.LAZY)" }));
		p.add( property( "ReportDefinition", "reportDefinition", new String[] { "@ManyToOne(fetch=FetchType.LAZY)" } ) );
		reportFilter.setProperties(p);

		Clazz reportCustomFilter = new Clazz();
		reportCustomFilter.setClassName("ReportCustomFilter" );
		reportCustomFilter.setAboveVisibility(UpVisibility.TRUE_R );
		reportCustomFilter.setBelowVisibility(true);
		reportCustomFilter.setGenerateController(false);
		p = new ArrayList<Property>();
		p.add(property("Long", "id", new String[] { "@Id", "@GeneratedValue" }));
		p.add(property("String", "propertyName"));
		p.add(property("String", "label"));
		p.add(property("Float", "displayOrder"));
		p.add(property("String", "operator"));
		p.add(property("String", "value"));
		p.add(property("Boolean", "editable"));
		p.add(property("Long", "dataTypeId"));
		p.add( property( "ReportDefinition", "reportDefinition", new String[] { "@ManyToOne(fetch=FetchType.LAZY)",
				"@org.hibernate.annotations.ForeignKey(name = \"FK_customfilter_reportdefinition\")"} ) );
		reportCustomFilter.setProperties(p);

		Clazz reportGroupBy = new Clazz();
		reportGroupBy.setClassName( "ReportGroupBy" );
		reportGroupBy.setAboveVisibility( UpVisibility.FALSE );
		reportGroupBy.setBelowVisibility(true);
		p = new ArrayList<Property>();
		p.add(property("Long", "id", new String[] { "@Id", "@GeneratedValue" }));
		p.add(property("String", "label"));
        p.add(property("String", "propertyName"));
		p.add(property("String", "sortBy"));
		p.add(property("Float", "ranking"));
		p.add(property("Boolean", "other"));
		p.add(property("String", "unit"));
        p.add(property("Boolean", "byPartition"));
		p.add(property("ThingType", "thingType", new String[] { "@ManyToOne(fetch=FetchType.LAZY)" } ) );
		p.add(property("ThingTypeField", "thingTypeField", new String[] { "@ManyToOne(fetch=FetchType.LAZY)" }));
		//p.add(property("String", "parentThingTypeCode"));
		p.add(property("ThingType", "parentThingType", new String[] { "@ManyToOne(fetch=FetchType.LAZY)" }));

		p.add( property( "ReportDefinition", "reportDefinition", new String[] { "@ManyToOne(fetch=FetchType.LAZY)" } ) );
		reportGroupBy.setProperties(p);

		Clazz reportProperty = new Clazz();
		reportProperty.setClassName( "ReportProperty" );
		reportProperty.setAboveVisibility( UpVisibility.FALSE );
		reportProperty.setBelowVisibility( true );
		reportProperty.setGenerateController(false);
		p = new ArrayList<Property>();
		p.add(property("Long", "id", new String[] { "@Id", "@GeneratedValue" }));
		p.add( property( "String", "label" ) );
		p.add(property("Float", "displayOrder"));
		p.add(property("String", "propertyName"));
		p.add(property("String", "sortBy"));
        p.add(property("Boolean", "editInline"));
        p.add(property("Boolean", "showHover") );
		p.add(property("Boolean", "enableHeat", new String[] { "@Column(name=\"enableHeat\", columnDefinition=\"BIT DEFAULT 0\")" } ) );
		p.add(property("ThingType", "thingType", new String[] { "@ManyToOne(fetch=FetchType.LAZY)" } ) );
		p.add(property("ThingTypeField", "thingTypeField", new String[] { "@ManyToOne(fetch=FetchType.LAZY)" }));
		//p.add(property("String", "parentThingTypeCode"));
		p.add(property("ThingType", "parentThingType", new String[] { "@ManyToOne(fetch=FetchType.LAZY)" }));
		p.add( property( "ReportDefinition", "reportDefinition", new String[] { "@ManyToOne(fetch=FetchType.LAZY)", "@NotNull" } ) );
		//TODO: for 3.1.1
		//p.add( property( "Unit", "unit", new String[] { "@ManyToOne" } ) );
		reportProperty.setProperties( p );

		Clazz reportRules = new Clazz();
		reportRules.setClassName( "ReportRule" );
		reportRules.setAboveVisibility( UpVisibility.FALSE );
		reportRules.setBelowVisibility( true );
		p = new ArrayList<Property>();
		p.add(property("Long", "id", new String[] { "@Id", "@GeneratedValue" }));
		p.add(property("String", "propertyName"));
		p.add(property("String", "operator"));
		p.add(property("String", "value"));
		p.add(property("String", "color"));
        p.add( property( "Float", "displayOrder" ) );
        p.add(property("Boolean", "stopRules"));
		p.add(property("String", "style"));
        p.add(property("String", "iconType"));
		p.add(property("ThingType", "thingType", new String[] { "@ManyToOne(fetch=FetchType.LAZY)" }));
		p.add(property("ThingTypeField", "thingTypeField", new String[] { "@ManyToOne(fetch=FetchType.LAZY)" }));
		//p.add(property("String", "parentThingTypeCode"));
		p.add(property("ThingType", "parentThingType", new String[] { "@ManyToOne(fetch=FetchType.LAZY)" }));
		p.add(property("ReportDefinition", "reportDefinition", new String[] {
				"@ManyToOne(fetch=FetchType.LAZY)", "@NotNull" }));
		reportRules.setProperties(p);

		//Report Entry Options
		Clazz reportEntryOption = new Clazz();
		reportEntryOption.setClassName("ReportEntryOption" );
		reportEntryOption.setAboveVisibility( UpVisibility.FALSE );
		reportEntryOption.setImports( new String[] { "java.util.List", "com.tierconnect.riot.appcore.entities.*", "javax.persistence.*"});
		reportEntryOption.setBelowVisibility(true);
		reportEntryOption.setGenerateController(false);
		p = new ArrayList<Property>();
		p.add( property( "Long", "id", new String[] { "@Id", "@GeneratedValue" } ) );
		p.add(property("String", "name"));
		p.add(property("String", "label"));
		p.add(property("Float", "displayOrder"));
		p.add(property("Boolean", "associate"));
		p.add(property("Boolean", "disassociate"));
		p.add(property("Boolean", "newOption"));
		p.add(property("Boolean", "editOption"));
		p.add(property("Boolean", "deleteOption"));
		p.add(property("Boolean", "RFIDPrint"));
		p.add(property("Long", "defaultRFIDPrint"));
		p.add(property("Long", "defaultZPLTemplate"));
        p.add(property("Boolean", "isMobile"));
		p.add(property("ReportDefinition", "reportDefinition", new String[] {
				"@ManyToOne(fetch=FetchType.LAZY)", "@NotNull" }));
		p.add(property("Group", "group", new String[]{"@ManyToOne(fetch=FetchType.LAZY)"} ) );
		p.add(property( "ThingType", "thingType", new String[] { "@ManyToOne(fetch=FetchType.LAZY)" } ) );
		reportEntryOption.setProperties( p );


		Clazz reportEntryOptionProperty = new Clazz();
		reportEntryOptionProperty.setClassName( "ReportEntryOptionProperty" );
		reportEntryOptionProperty.setAboveVisibility( UpVisibility.FALSE);
		reportEntryOptionProperty.setBelowVisibility( true );
		reportEntryOptionProperty.setGenerateController( false );
		reportEntryOptionProperty.setImports( new String[] { "java.util.List" } );
		p = new ArrayList<Property>();
		p.add( property( "Long", "id", new String[] { "@Id", "@GeneratedValue" } ) );
		p.add( property( "String", "label" ) );
		p.add(property("Float", "displayOrder"));
		p.add(property("Long", "thingTypeFieldId"));
		p.add(property("String", "propertyName"));
		p.add(property("Long", "thingTypeIdReport"));
		p.add(property("String", "sortBy"));
		p.add(property("String", "defaultMobileValue"));
		p.add( property( "Boolean", "editInline" ) );
		//<REQ-4779--
		p.add( property( "Boolean", "required" ) );
		p.add( property( "Boolean", "pickList" ) );
		p.add( property( "List<EntryFormPropertyData>", "entryFormPropertyDatas", new String[] {
				"@OneToMany(mappedBy=\"reportEntryOptionProperty\", "
						+ "fetch=FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval=true)" } ) );
		//--REQ-4779>
		p.add( property( "Boolean", "allPropertyData" ) );
		reportEntryOptionProperty.setProperties( p );

		//<REQ-4779--New Table
		Clazz entryFormPropertyData = new Clazz();
		entryFormPropertyData.setClassName( "EntryFormPropertyData" );
		entryFormPropertyData.setAboveVisibility( UpVisibility.FALSE);
		entryFormPropertyData.setBelowVisibility( true );
		entryFormPropertyData.setGenerateController( false );
		p = new ArrayList<Property>();
		p.add( property( "Long", "id", new String[] { "@Id", "@GeneratedValue" } ) );
		p.add( property( "ReportEntryOptionProperty", "reportEntryOptionProperty", new String[] { "@ManyToOne(fetch=FetchType.LAZY)" } ) );
		p.add(property("String", "value"));
		p.add(property("String", "name"));
		entryFormPropertyData.setProperties( p );


		// VIZIX-2025 action HTTP REQUEST
		Clazz actionConfiguration = new Clazz();
		actionConfiguration.setClassName("ActionConfiguration");
		actionConfiguration.setAboveVisibility(UpVisibility.FALSE);
		actionConfiguration.setBelowVisibility(true);
		actionConfiguration.setImports(new String[]{"java.util.List", "com.tierconnect.riot.appcore.entities.*"});
		p = new ArrayList<Property>();
		p.add(property("Long", "id", new String[]{"@Id", "@GeneratedValue"}));
		p.add(property("String", "name", new String[]{"@NotNull", "@Column(name=\"name\", length = 150, nullable=false)"}));
		p.add(property("String", "code", new String[]{"@NotNull", "@Column(name=\"code\", length = 20, nullable=false)"}));
		p.add(property("String", "type", new String[]{"@NotNull", "@Column(name=\"type\", length = 100, nullable=false)"}));
		p.add(property("String", "configuration", new String[]{"@NotNull", "@Column(name=\"configuration\", length = 4000)"}));
		p.add(property("String", "status", new String[]{"@NotNull", "@Column(name=\"status\", length = 20)"}));
		p.add(property("Group", "group", new String[]{"@ManyToOne(fetch=FetchType.LAZY)", "@NotNull"}));
		p.add(property(
				"List<ReportActions>",
				"reportActionsList",
				new String[]{"@OneToMany(mappedBy=\"actionConfiguration\", fetch=FetchType.LAZY, cascade = CascadeType.ALL)"}));
		p.add(property(
				"List<LogExecutionAction>",
				"logExecutionActionList",
				new String[]{"@OneToMany(mappedBy=\"actionConfiguration\", fetch=FetchType.LAZY, cascade = CascadeType.ALL)"}));
		actionConfiguration.setProperties(p);

		Clazz reportActions = new Clazz();
		reportActions.setClassName("ReportActions");
		reportActions.setAboveVisibility(UpVisibility.FALSE);
		reportActions.setBelowVisibility(true);
		reportActions.setImports(new String[]{"java.util.List", "com.tierconnect.riot.appcore.entities.*"});
		p = new ArrayList<Property>();
		p.add(property("Long", "id", new String[]{"@Id", "@GeneratedValue"}));
		p.add(property("Integer", "displayOrder", new String[]{"@NotNull"}));
		p.add(property("ReportDefinition", "reportDefinition", new String[]{
				"@ManyToOne(fetch=FetchType.LAZY)", "@NotNull"}));
		p.add(property("ActionConfiguration", "actionConfiguration", new String[]{
				"@ManyToOne(fetch=FetchType.LAZY)", "@NotNull"}));
		p.add(property("User", "createdByUser", new String[]{"@ManyToOne(fetch=FetchType.LAZY)", "@NotNull"}));
		reportActions.setProperties(p);

		Clazz logExecutionAction = new Clazz();
		logExecutionAction.setClassName("LogExecutionAction");
		logExecutionAction.setAboveVisibility(UpVisibility.FALSE);
		logExecutionAction.setBelowVisibility(true);
		logExecutionAction.setImports(new String[]{"java.util.List", "com.tierconnect.riot.appcore.entities.*", "javax.persistence.Lob"});
		p = new ArrayList<Property>();
		p.add(property("Long", "id", new String[]{"@Id", "@GeneratedValue"}));
		p.add(property("String", "request", new String[]{"@Lob", "@Column(name=\"request\")"}));
		p.add(property("String", "responseCode", new String[]{"@Column(name=\"responseCode\", length = 20)"}));
		p.add(property("String", "response", new String[]{"@Lob", "@Column(name=\"response\")"}));
		p.add(property("Date", "iniDate", new String[]{"@NotNull"}));
		p.add(property("Date", "endDate"));
		p.add(property("Long", "processTime"));
		p.add(property("User", "createdByUser", new String[]{"@ManyToOne(fetch=FetchType.LAZY)", "@NotNull"}));
		p.add(property("ActionConfiguration", "actionConfiguration", new String[]{"@ManyToOne(fetch=FetchType.LAZY)", "@NotNull"}));
		logExecutionAction.setProperties(p);


		Clazz reportDefinitionConfig = new Clazz();
		reportDefinitionConfig.setClassName( "ReportDefinitionConfig" );
		reportDefinitionConfig.setAboveVisibility( UpVisibility.FALSE );
		reportDefinitionConfig.setBelowVisibility( true );
		reportDefinitionConfig.setImports( new String[] { "java.util.List",  "java.sql.*" } );
		p = new ArrayList<Property>();
		p.add(property("Long", "id", new String[] { "@Id", "@GeneratedValue" }));
		p.add(property("ReportDefinition", "reportDefinition", new String[] {
				"@ManyToOne(fetch=FetchType.LAZY)", "@NotNull" }));
		p.add(property("String", "keyType"));
		p.add(property("String", "keyValue", new String[] { "@Column(name=\"keyValue\", length = 8000)"}));
		reportDefinitionConfig.setProperties(p);

		Clazz thing = new Clazz();
		thing.setClassName( "Thing" );
		thing.setAboveVisibility( UpVisibility.FALSE );
		thing.setBelowVisibility( true );
		thing.setImports( new String[] { "java.util.List", "com.tierconnect.riot.appcore.entities.*", "com.tierconnect.riot.commons.entities.IThing" } );
		thing.setImplement("IThing");
		p = new ArrayList<Property>();
		p.add( property( "Long", "id", new String[] { "@Id", "@GeneratedValue" } ) );

        p.add( property( "User", "createdByUser", new String[] { "@ManyToOne(fetch=FetchType.LAZY)", "@NotNull" } ) );
		p.add( property( "String", "name" ) );
		p.add( property( "String", "serial" ) );
		p.add(property("boolean", "activated"));
        //Object Visibility
        p.add( property( "GroupType", "groupTypeFloor", new String [] { "@ManyToOne(fetch=FetchType.LAZY)" } ) );
//		p.add(property(
//				"List<ThingField>",
//				"thingFields",
//				new String[] { "@Transient" }));
		p.add(property("Group", "group", new String[]{"@ManyToOne(fetch=FetchType.LAZY)",
				"@NotNull"}));
		p.add( property( "ThingType", "thingType", new String[] { "@ManyToOne(fetch=FetchType.LAZY)" } ) );
		p.add(property("Thing", "parent",
				new String[]{"@ManyToOne(fetch=FetchType.LAZY)"}));
		p.add( property( "Long", "modifiedTime" ) );
		thing.setProperties( p );
		thing.setGenerateControllerInsert( false );
		thing.setGenerateServiceInsert( false );

//		Clazz thingField = new Clazz();
//		thing.setDeprecated( true ); //
//		thingField.setClassName("ThingField");
//		thingField.setTableName("apc_thingfield");
//		thingField.setAnnotations(new String[]{"@org.hibernate.annotations.Entity(dynamicUpdate = true)"});
//		thingField.setAboveVisibility(UpVisibility.FALSE);
//		thingField.setBelowVisibility(true);
//        thingField.setGenerateController(true);
//		p = new ArrayList<Property>();
//		p.add(property("Long", "id", new String[] { "@Id", "@GeneratedValue" }));
//		p.add(property("String", "name"));
//		// unit symbol, e.g., m, ft, kg, s, etc.
//		p.add( property( "String", "symbol" ) );
//		// unit name, e.g. meter, foot, kilogram, second, etc.
//		p.add( property( "String", "unit" ) );
//		// quantity name, e.g. length, mass, time, etc.
//		//TODO: add this one !
//		// p.add(property("String", "quantityName"));
//		p.add(property("Long", "type")); // RRCC
//		p.add(property("Boolean", "timeSeries"));
//
//		//ThingTypeFieldId
//		//todo this should be a @ManytoOne??
//		p.add(property("Long", "thingTypeFieldId", new String[] { "@NotNull", "@Column(nullable = false)" }));
//
//		p.add(property("Thing", "thing",
//				new String[]{"@ManyToOne(fetch=FetchType.EAGER)"}));
//		p.add(property("Long", "modifiedTime"));
//		thingField.setProperties(p);
//		thingField.setGenerateControllerUpdate(false);
//		thingField.setGenerateControllerInsert(false);

		Clazz thingImage = new Clazz();
		thingImage.setClassName("ThingImage");
		thingImage.setAboveVisibility( UpVisibility.TRUE_R);
		thingImage.setBelowVisibility( true );
		thingImage.setImports( new String[] { "java.util.List", "com.tierconnect.riot.appcore.entities.*", "javax.persistence.*" } );
		p = new ArrayList<Property>();
		p.add( property( "Long", "id", new String[] { "@Id", "@GeneratedValue" } ) );
		p.add( property( "byte[]", "image", new String[] { "@Lob" } ) );
		p.add( property( "String", "contentType", new String[] { "@NotNull"}));
		p.add(property("String", "fileName", new String[] { "@NotNull"}));

		thingImage.setProperties( p );
		thingImage.setGenerateController( false );
		thingImage.setGenerateControllerAll( false );


		Clazz thingParentHistory = new Clazz();
		thingParentHistory.setClassName( "ThingParentHistory" );
		thingParentHistory.setAboveVisibility( UpVisibility.FALSE);
		thingParentHistory.setBelowVisibility( true );
		// thingParentHistory.setImports( new String [] {
		// "com.tierconnect.riot.appcore.entities.Group" } );
		p = new ArrayList<Property>();
		p.add( property( "Long", "id", new String[] { "@Id", "@GeneratedValue" } ) );
		p.add( property( "Date", "startDate", new String[] { "@NotNull" } ) );
		p.add( property( "Date", "endDate" ) );
		p.add(property("Thing", "child", new String[]{"@ManyToOne",
				"@NotNull"}));
		p.add(property("Thing", "parent", new String[]{"@ManyToOne",
				"@NotNull"} ) );
		thingParentHistory.setProperties( p );
		thingParentHistory.setGenerateControllerDelete( false );
		thingParentHistory.setGenerateControllerInsert( false );
		thingParentHistory.setGenerateControllerUpdate( false );
		thingParentHistory.setGenerateControllerSelect( false );


		//Thing type template
		Clazz dataType = new Clazz();
		dataType.setClassName( "DataType");
		dataType.setImplement("java.io.Serializable");
		dataType.setAboveVisibility(UpVisibility.TRUE_R);
		dataType.setBelowVisibility( true );
		dataType.setGenerateController( true );
		p = new ArrayList<Property>();
		p.add( property( "Long", "id", new String[] { "@Id" } ) );
		p.add( property( "String", "typeParent" ) );
		p.add(property("String", "code"));
		p.add(property("String", "value"));
		p.add(property("String", "type"));
		p.add(property("String", "description"));
		p.add(property("String", "clazz"));
		dataType.setProperties( p );
		// Thing type Template category
		Clazz thingTypeTemplateCategory = new Clazz();
		thingTypeTemplateCategory.setClassName("ThingTypeTemplateCategory");
		thingTypeTemplateCategory.setImplement("java.io.Serializable");
		thingTypeTemplateCategory.setAboveVisibility(UpVisibility.TRUE_R);
		thingTypeTemplateCategory.setBelowVisibility(true);
		thingTypeTemplateCategory.setGenerateController(false);
		thingTypeTemplateCategory.setImports(new String[]{"java.util.List"});
		p = new ArrayList<Property>();
		p.add(property("Long", "id", new String[]{"@Id", "@GeneratedValue"}));
		p.add(property("String", "code", new String[]{"@NotNull", "@Column(name = \"code\", nullable = false, length = 100)"}));
		p.add(property("String", "name", new String[]{"@NotNull", "@Column(name = \"name\", nullable = false, length = 100)"}));
		p.add(property("Integer", "displayOrder", new String[]{"@NotNull", "@Column(name = \"displayOrder\")"}));
		p.add(property("String", "pathIcon", new String[]{"@NotNull", "@Column(name = \"pathIcon\", nullable = false, length = 100)"}));
		p.add(property("List<ThingTypeTemplate>", "thingTypeTemplateList",
				new String[]{"@OneToMany(mappedBy=\"thingTypeTemplateCategory\", " +
						"fetch=FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval=true)"}));
		thingTypeTemplateCategory.setProperties(p);
		//Thing type template
		Clazz thingTypeTemplate = new Clazz();
		thingTypeTemplate.setClassName("ThingTypeTemplate");
		thingTypeTemplate.setImplement("java.io.Serializable");
		thingTypeTemplate.setAboveVisibility(UpVisibility.TRUE_R);
		thingTypeTemplate.setBelowVisibility(true);
		thingTypeTemplate.setGenerateController(true);
		thingTypeTemplate.setGenerateControllerInsert(true);
		thingTypeTemplate.setGenerateControllerDelete(true);
		thingTypeTemplate.setImports(new String[]{"com.tierconnect.riot.appcore.entities.*", "java.util.Set"});
		p = new ArrayList<Property>();
		p.add(property("Long", "id", new String[]{"@Id", "@GeneratedValue"}));
		p.add(property("String", "code", new String[]{"@Column(name = \"code\", length = 100)"}));
		p.add(property("String", "name", new String[]{"@NotNull", "@Column(name = \"name\", nullable = false, length = 255)"}));
		p.add(property("boolean", "autoCreate"));
		p.add(property("Integer", "displayOrder", new String[]{"@Column(name = \"displayOrder\")"}));
		p.add(property("String", "pathIcon", new String[]{"@Column(name = \"pathIcon\", nullable = false, length = 255)"}));
		p.add(property("String", "description", new String[]{"@NotNull", "@Column(name = \"description\", length = 255)"}));
		p.add(property("Group", "group", new String[]{"@ManyToOne(fetch=FetchType.LAZY)", "@NotNull"}));
		p.add(property("ThingTypeTemplateCategory", "thingTypeTemplateCategory", new String[]{"@ManyToOne(fetch=FetchType.LAZY)"}));

		p.add( property( "Set<ThingTypeFieldTemplate>", "thingTypeFieldTemplate",
				new String [] { "@OneToMany(mappedBy=\"thingTypeTemplate\", " +
						"fetch=FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval=true)" } ) );
		p.add( property( "Set<ThingType>", "thingType",
				new String [] { "@OneToMany(mappedBy=\"thingTypeTemplate\", " +
						"fetch=FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval=true)" } ) );
		thingTypeTemplate.setProperties( p );

		//Thing type field template
		Clazz thingTypeFieldTemplate = new Clazz();
		thingTypeFieldTemplate.setClassName( "ThingTypeFieldTemplate" );
		thingTypeFieldTemplate.setAboveVisibility( UpVisibility.TRUE_R );
		thingTypeFieldTemplate.setBelowVisibility( true );
		thingTypeFieldTemplate.setGenerateController( true );
		thingTypeFieldTemplate.setImports( new String[] { "com.tierconnect.riot.appcore.entities.*", "java.util.Set" } );

		p = new ArrayList<Property>();
		p.add( property( "Long", "id", new String[] { "@Id", "@GeneratedValue" } ) );
		p.add( property( "String", "name" ) );
		p.add( property( "String", "description" ) );
		p.add(property("String", "unit"));
		p.add(property("String", "symbol"));
		p.add(property("String", "defaultValue", new String[] { "@Column(length = 1024)" } ) ); /*defaultValue*/
		p.add(property("boolean", "timeSeries"));
		p.add(property("ThingTypeTemplate", "thingTypeTemplate", new String[]{"@ManyToOne", "@NotNull"}));
		p.add(property("String", "typeParent"));
		p.add(property("DataType", "type", new String[]{"@ManyToOne(fetch=FetchType.LAZY)","@NotNull"}));
		p.add(property("Long", "dataTypeThingTypeId"));
		thingTypeFieldTemplate.setProperties(p);

		Clazz thingType = new Clazz();
		thingType.setClassName( "ThingType" );
		thingType.setImplement("java.io.Serializable");
		thingType.setAboveVisibility( UpVisibility.TRUE_R );
		thingType.setBelowVisibility(true);
		thingType.setImports(new String[]{"com.tierconnect.riot.appcore.entities.*", "java.util.Set", "org.hibernate" +
				".annotations.Cache", "org.hibernate.annotations.CacheConcurrencyStrategy"});

		p = new ArrayList<Property>();
		p.add( property( "Long", "id", new String[] { "@Id", "@GeneratedValue" } ) );
		p.add(property("boolean", "archived"));
		p.add( property( "String", "name" ) );
		p.add(property("Group", "group", new String[]{
				"@ManyToOne(fetch=FetchType.LAZY)",
				"@NotNull",
				"@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)"}));
		//p.add(property("ThingType", "parent", new String[] { "@ManyToOne" }));
		p.add( property( "String", "thingTypeCode" ) );
		p.add( property( "boolean", "autoCreate" ) );
        p.add(property("GroupType", "defaultOwnerGroupType", new String[] { "@ManyToOne" }));
        p.add(property("Set<ThingTypeMap>", "parentTypeMaps", new String[]{"@OneToMany(mappedBy=\"child\", " +
				"fetch=FetchType.LAZY)", "@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)"}));
        p.add(property("Set<ThingTypeMap>", "childrenTypeMaps", new String[]{"@OneToMany(mappedBy=\"parent\", " +
				"fetch=FetchType.LAZY)", "@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)"}));
        // 2016 Jan 19: change to FetchType.LAZY for bridge cache. If someone really needs this EAGER, talk to T before changing it
		p.add(property("Set<ThingTypeField>", "thingTypeFields", new String[]{"@OneToMany(mappedBy=\"thingType\", " +
				"fetch=FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval=true)", "@Cache(usage = " +
				"CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)"}));
		p.add( property( "Long", "modifiedTime" ) );
		//<REQ-4417> Thing type
		p.add( property( "ThingTypeTemplate", "thingTypeTemplate", new String[] { "@ManyToOne(fetch=FetchType.LAZY)" } ) );
		//--<REQ-4417>
		p.add( property( "String", "serialFormula" ) );
		p.add( property( "boolean", "isParent"));
		thingType.setProperties( p );

		Clazz thingTypeField = new Clazz();
		thingTypeField.setClassName( "ThingTypeField" );
		thingTypeField.setImplement("java.io.Serializable");
		thingTypeField.setAboveVisibility( UpVisibility.TRUE_R);
		thingTypeField.setBelowVisibility( true );
        thingTypeField.setGenerateController( false );
		thingTypeField.setImports(new String[]{"org.hibernate.annotations.Cache", "org.hibernate.annotations" +
				".CacheConcurrencyStrategy"});
		p = new ArrayList<>();
		p.add(property("Long", "id", new String[] { "@Id", "@GeneratedValue" }));
		p.add( property( "String", "name", new String[] {"@Column(columnDefinition = \"varchar(255) binary\")"} ) );
		// unit symbol, e.g., m, ft, kg, s, etc.
		p.add( property( "String", "symbol" ) );
		// unit name, e.g. meter, foot, kilogram, second, etc.
		p.add( property( "String", "unit" ) );
		// quantity name, e.g. length, mass, time, etc.
		//TODO: add this one !
		//p.add(property("String", "quantityName"));
		p.add(property("Long", "timeToLive"));
		p.add(property("Boolean", "timeSeries"));
		p.add(property("ThingType", "thingType", new String[]{"@ManyToOne", "@NotNull", "@Cache(usage = " +
				"CacheConcurrencyStrategy.READ_WRITE)"}));
		//<REQ-4417> Thing Type Field
		p.add( property( "String", "typeParent" ) );
		p.add( property( "String", "defaultValue", new String[] { "@Column(length = 1024)" } ) );
		p.add( property( "Boolean", "multiple" ) );
		p.add( property( "Long", "thingTypeFieldTemplateId" ) );
		p.add( property( "DataType", "dataType", new String[] { "@ManyToOne(fetch=FetchType.LAZY)", "@NotNull",
				"@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)" } ) );
		p.add( property( "Long", "dataTypeThingTypeId" ) );

		//--<REQ-4417>
		thingTypeField.setProperties( p );

		Clazz zone = new Clazz();
		zone.setClassName("Zone");
		zone.setAboveVisibility( UpVisibility.TRUE_R);
		zone.setBelowVisibility(true);
		zone.setImports( new String[] { "java.util.List", "java.util.Set",
			                              "com.tierconnect.riot.appcore.entities.Group",
			                              "org.hibernate.annotations.Cache",
			                              "org.hibernate.annotations.CacheConcurrencyStrategy" });
		p = new ArrayList<Property>();
		p.add(property("Long", "id", new String[] { "@Id", "@GeneratedValue" }));
		p.add(property("String", "name"));
		p.add(property("String", "description"));
		p.add(property("String", "color"));
        p.add(property("String", "code"));
        // 2016 Jan 19: change to FetchType.LAZY for bridge cache. If someone really needs this EAGER, talk to T before changing it
		p.add(property(
				"Set<ZonePoint>",
				"zonePoints",
				new String[]{"@OneToMany(mappedBy=\"zone\", fetch=FetchType.LAZY, cascade = CascadeType.ALL)", "@Cache" +
						"(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)"}));
		p.add(property("Group", "group", new String[] { "@ManyToOne",
				"@NotNull"}));
		p.add(property("ZoneGroup", "zoneGroup", new String[]{"@ManyToOne(fetch=FetchType.LAZY)"}));
		p.add(property("ZoneType", "zoneType", new String[] { "@ManyToOne(fetch=FetchType.LAZY)" }));
        p.add(property("LocalMap", "localMap", new String[] { "@ManyToOne(fetch=FetchType.LAZY)"}));
		zone.setProperties( p );

		Clazz zoneGroup = new Clazz();
		zoneGroup.setClassName( "ZoneGroup" );
		zoneGroup.setAboveVisibility( UpVisibility.TRUE_R);
		zoneGroup.setBelowVisibility(true);
		zoneGroup.setGenerateControllerDelete(false);
		zoneGroup.setImports( new String[] { "java.util.List", "com.tierconnect.riot.appcore.entities.Group"});
		p = new ArrayList<Property>();
		p.add( property( "Long", "id", new String[] { "@Id", "@GeneratedValue" } ) );
		p.add(property("String", "name"));
		p.add(property("String", "description"));
		p.add(property("Group", "group", new String[]{"@ManyToOne(fetch=FetchType.LAZY)",
				"@NotNull"}));
		p.add(property(
				"List<Zone>",
				"zones",
				new String[]{"@OneToMany(mappedBy=\"zoneGroup\", fetch=FetchType.LAZY, cascade = CascadeType.ALL)" }));
        p.add(property("LocalMap", "localMap", new String[] { "@ManyToOne(fetch=FetchType.LAZY)" }));
		zoneGroup.setProperties( p );

		Clazz zonePoint = new Clazz();
		zonePoint.setClassName("ZonePoint");
		zonePoint.setAboveVisibility( UpVisibility.TRUE_R);
		zonePoint.setBelowVisibility(true);
		zonePoint.setImports( new String[] { "com.tierconnect.riot.appcore.entities.Group" });
		p = new ArrayList<Property>();
		p.add( property( "Long", "id", new String[] { "@Id", "@GeneratedValue" } ) );
		p.add( property( "Double", "x" ) );
		p.add( property( "Double", "y" ) );
		p.add( property( "Long", "arrayIndex" ) );
		p.add( property( "Zone", "zone", new String[] { "@ManyToOne(fetch=FetchType.LAZY)" } ) );
		zonePoint.setProperties(p);

        Clazz zoneProperty = new Clazz();
        zoneProperty.setClassName("ZoneProperty");
        zoneProperty.setAboveVisibility( UpVisibility.FALSE);
        zoneProperty.setBelowVisibility(true);
        p = new ArrayList<Property>();
        p.add( property( "Long", "id", new String[] { "@Id", "@GeneratedValue" } ) );
        p.add(property("String", "name"));
        p.add( property( "Integer", "type" ) );
        p.add( property( "ZoneType", "zoneType", new String[] { "@ManyToOne(fetch=FetchType.LAZY)" } ) );
        zoneProperty.setProperties( p );

        Clazz zonePropertyValue = new Clazz();
        zonePropertyValue.setClassName("ZonePropertyValue");
        zonePropertyValue.setAboveVisibility( UpVisibility.FALSE);
        zonePropertyValue.setBelowVisibility(true);
        p = new ArrayList<Property>();

        p.add(property("Long", "id", new String[]{"@Id", "@GeneratedValue" }));
        p.add(property("Long", "zonePropertyId", new String[] { "@NotNull", "@Column(nullable = false)" }));
        // TODO: make a Zone object instead of long ?
        p.add(property("Long", "zoneId", new String[] { "@NotNull", "@Column(nullable = false)" }));
        p.add( property( "String", "value" ) );
        zonePropertyValue.setProperties( p );

		Clazz zoneType = new Clazz();
		zoneType.setClassName( "ZoneType" );
		zoneType.setAboveVisibility( UpVisibility.TRUE_R);
		zoneType.setBelowVisibility(true);
		zoneType.setImports( new String[] { "com.tierconnect.riot.appcore.entities.Group", "java.util.List" });
		p = new ArrayList<Property>();
		p.add( property( "Long", "id", new String[] { "@Id", "@GeneratedValue" } ) );
		p.add(property("String", "name"));
		p.add( property( "String", "description" ) );
        p.add( property( "String", "zoneTypeCode" ) );
		p.add( property( "Group", "group", new String[] { "@ManyToOne(fetch=FetchType.LAZY)", "@NotNull" } ) );
        p.add( property( "List<ZoneProperty>", "zoneProperties",
				new String[] { "@OneToMany(mappedBy=\"zoneType\", fetch=FetchType.LAZY, cascade = CascadeType.ALL)" } ) );

		zoneType.setProperties(p);

		Clazz thingTypeMap = new Clazz();
		thingTypeMap.setClassName("ThingTypeMap");
		thingTypeMap.setImplement("java.io.Serializable");
		thingTypeMap.setImports(new String[]{"org.hibernate.annotations.Cache", "org.hibernate.annotations" +
				".CacheConcurrencyStrategy"});
		thingTypeMap.setAnnotations( new String[] { "@org.hibernate.annotations.Entity(dynamicUpdate = true)" } );
		thingTypeMap.setAboveVisibility( UpVisibility.TRUE_R);
		thingTypeMap.setBelowVisibility(true);
		p = new ArrayList<>();
		p.add( property( "Long", "id", new String[] { "@Id", "@GeneratedValue" } ) );
		p.add(property("ThingType", "parent", new String[]{"@ManyToOne", "@Cache(usage = CacheConcurrencyStrategy" +
				".NONSTRICT_READ_WRITE)"}));
		p.add(property("ThingType", "child", new String[]{"@ManyToOne", "@Cache(usage = CacheConcurrencyStrategy" +
				".NONSTRICT_READ_WRITE)"}));

		thingTypeMap.setProperties( p );
		//thingField.setGenerateControllerUpdate(false);
		//thingField.setGenerateControllerInsert(false);

		Clazz shift = new Clazz();
		shift.setClassName("Shift");
		shift.setAboveVisibility( UpVisibility.TRUE_R);
		shift.setBelowVisibility(true);
		shift.setImports( new String[] { "java.util.List", "com.tierconnect.riot.appcore.entities.Group" });
		p = new ArrayList<Property>();
		p.add(property("Long", "id", new String[] { "@Id", "@GeneratedValue" }));
		p.add( property( "Group", "group", new String[] { "@ManyToOne" } ) );
		p.add( property( "String", "name" ) );
		p.add( property( "Long", "startTimeOfDay" ) );
		p.add( property( "Long", "endTimeOfDay" ) );
		p.add( property( "String", "daysOfWeek" ) );
        p.add(property("Boolean", "active"));
        p.add(property("String", "code"));
		shift.setProperties( p );

		Clazz shiftZone = new Clazz();
		shiftZone.setClassName( "ShiftZone" );
		shiftZone.setAboveVisibility( UpVisibility.TRUE_R);
		shiftZone.setBelowVisibility(true);
		shiftZone.setImports(
				new String[] { "java.util.List", "com.tierconnect.riot.appcore.entities.Group", "com.tierconnect.riot.iot.entities.Shift",
						"com.tierconnect.riot.iot.entities.Zone"});
		p = new ArrayList<Property>();
		p.add( property( "Long", "id", new String[] { "@Id", "@GeneratedValue" } ) );
		p.add( property( "Group", "group", new String[] { "@ManyToOne" } ) );
		p.add( property( "Shift", "shift", new String[] { "@ManyToOne" } ) );
		p.add(property("Zone", "zone", new String [] { "@ManyToOne" } ));
		shiftZone.setProperties( p );
		shiftZone.setGenerateController( false );
		shiftZone.setGenerateControllerAll( false );

		Clazz shiftThing = new Clazz();
		shiftThing.setClassName("ShiftThing");
		shiftThing.setAboveVisibility( UpVisibility.TRUE_R);
		shiftThing.setBelowVisibility(true);
		shiftThing.setImports(
				new String[] { "java.util.List", "com.tierconnect.riot.appcore.entities.Group", "com.tierconnect.riot.iot.entities.Shift",
						"com.tierconnect.riot.iot.entities.Zone", "com.tierconnect.riot.iot.entities.Thing"});
		p = new ArrayList<Property>();
		p.add( property( "Long", "id", new String[] { "@Id", "@GeneratedValue" } ) );
		p.add( property( "Group", "group", new String[] { "@ManyToOne" } ) );
		p.add( property( "Shift", "shift", new String[] { "@ManyToOne" } ) );
		p.add( property( "Thing", "thing", new String[] { "@ManyToOne" } ) );
		shiftThing.setProperties( p );
		shiftThing.setGenerateController(false);
		shiftThing.setGenerateControllerAll( false );

		Clazz unit = new Clazz();
		unit.setClassName("Unit");
		unit.setAboveVisibility( UpVisibility.TRUE_R);
		unit.setBelowVisibility(true);
		unit.setImports( new String[] { "java.util.List", "com.tierconnect.riot.appcore.entities.Group" });
		p = new ArrayList<Property>();
		p.add( property( "Long", "id", new String[] { "@Id", "@GeneratedValue" } ) );
		p.add( property( "Group", "group", new String[] { "@ManyToOne" } ) );
		// e.g. meter, kilogram, second
		p.add( property( "String", "unitName" ) );
		// e.g. m, kg, s
		//TODO: make unique !
		p.add(property("String", "unitSymbol"));
		// e.g. length, mass, time
		p.add(property("String", "quantityName"));
		p.add(property("String", "definition"));
		unit.setProperties(p);


        Clazz customApplication = new Clazz();
        customApplication.setClassName("CustomApplication");
        customApplication.setAboveVisibility( UpVisibility.TRUE_R);
        customApplication.setBelowVisibility(true);
        customApplication.setImports( new String[] { "java.util.List", "com.tierconnect.riot.appcore.entities.Group", "javax.persistence.*" });
        p = new ArrayList<Property>();
		p.add( property( "Long", "id", new String[] { "@Id", "@GeneratedValue" } ) );
		p.add(property("String", "code"));
        p.add(property("String", "name"));
		p.add(property("Boolean", "shotTab"));
        p.add(property("byte[]", "icon", new String [] { "@Lob" }));
        customApplication.setProperties( p );

        Clazz customObjectType = new Clazz();
        customObjectType.setClassName("CustomObjectType");
        customObjectType.setAboveVisibility( UpVisibility.TRUE_R);
        customObjectType.setBelowVisibility(true);
        p = new ArrayList<Property>();
        p.add( property( "Long", "id", new String[] { "@Id", "@GeneratedValue" } ) );
		p.add(property("String", "code"));
		p.add( property( "String", "name" ) );
        p.add( property( "String", "description" ) );
		p.add( property( "CustomApplication", "customApplication", new String[] { "@ManyToOne" } ) );
        customObjectType.setProperties( p );

        Clazz customObject = new Clazz();
		customObject.setClassName("CustomObject");
        customObject.setAboveVisibility( UpVisibility.TRUE_R);
        customObject.setBelowVisibility(true);
        p = new ArrayList<Property>();
        p.add(property("Long", "id", new String[]{"@Id", "@GeneratedValue" }));
        p.add( property( "Boolean", "archived" ) );
		p.add(property("CustomObjectType", "customObjectType", new String[] { "@ManyToOne" }));
		customObject.setProperties( p );

        Clazz customFieldType = new Clazz();
        customFieldType.setClassName("CustomFieldType");
        customFieldType.setAboveVisibility( UpVisibility.TRUE_R);
        customFieldType.setBelowVisibility(true);
        p = new ArrayList<Property>();
		p.add(property("Long", "id", new String[]{"@Id", "@GeneratedValue"}));
		p.add(property("String", "name"));
		p.add(property("String", "displayName"));
		p.add( property( "String", "description" ) );
        customFieldType.setProperties(p);
        customFieldType.setGenerateControllerInsert( false );
        customFieldType.setGenerateControllerUpdate( false );
        customFieldType.setGenerateControllerDelete( false );

        Clazz customField = new Clazz();
		customField.setClassName("CustomField");
        customField.setAboveVisibility( UpVisibility.TRUE_R);
        customField.setBelowVisibility(true);
        p = new ArrayList<Property>();
        p.add(property("Long", "id", new String[] { "@Id", "@GeneratedValue" }));
        p.add( property( "String", "code" ) );
        p.add( property( "String", "name" ) );
        p.add( property( "Boolean", "required" ) );
        p.add( property( "CustomFieldType", "customFieldType", new String[] { "@ManyToOne" } ) );
        p.add(property("CustomObjectType", "customObjectType", new String[] { "@ManyToOne"}));
		p.add(property("ThingType", "thingType", new String[] { "@ManyToOne" }));     // this field should be mapped one-to-one
		p.add(property("ThingTypeField", "thingTypeField", new String[] { "@ManyToOne" }));     // this field should be mapped one-to-one
        p.add(property("CustomObjectType", "lookupObject", new String[] { "@ManyToOne" })); // this field should be mapped one-to-one
		p.add(property("CustomField", "lookupObjectField", new String[] { "@ManyToOne" })); // this field should be mapped one-to-one
        p.add( property( "String", "render" ) );
        p.add( property( "Boolean", "multipleSelect" ) );
        p.add( property( "Long", "position" ) );
        customField.setProperties( p );

        Clazz customFieldValue = new Clazz();
        customFieldValue.setClassName("CustomFieldValue");
        customFieldValue.setAboveVisibility( UpVisibility.TRUE_R);
        customFieldValue.setBelowVisibility(true);
        p = new ArrayList<Property>();
		p.add(property("Long", "id", new String[]{"@Id", "@GeneratedValue"}));
		p.add(property("String", "value"));
        p.add(property("CustomObject", "customObject", new String[]{"@ManyToOne"}));
		p.add(property("CustomField", "customField", new String[] { "@ManyToOne" }));
        customFieldValue.setProperties( p );

		// sequence
		Clazz sequence = new Clazz();
		sequence.setClassName("Sequence");
		sequence.setAboveVisibility( UpVisibility.TRUE_R);
		sequence.setBelowVisibility(true);
		sequence.setGenerateController(true);
		sequence.setGenerateControllerInsert(true);
		sequence.setGenerateControllerDelete(true);
		sequence.setGenerateService(false);
		p = new ArrayList<Property>();
		p.add( property( "Long", "id", new String[] { "@Id", "@GeneratedValue" } ) );
		p.add( property( "ThingTypeField", "thingTypeField", new String[] { "@ManyToOne" } ) );
		p.add( property( "Long", "initialValue" ) );
		p.add(property("Long", "currentValue"));
		p.add(property("String", "name"));
		sequence.setProperties( p );

		// notification
		Clazz notificationTemplate = new Clazz();
		notificationTemplate.setClassName( "NotificationTemplate" );
		notificationTemplate.setAboveVisibility(UpVisibility.TRUE_R);
		notificationTemplate.setGenerateController( true );
		notificationTemplate.setGenerateControllerInsert( false );
		notificationTemplate.setGenerateControllerDelete( false );
		p = new ArrayList<Property>();
		p.add( property( "Long", "id", new String[] { "@Id", "@GeneratedValue" } ) );
		p.add( property( "String", "templateName" ) );
		p.add( property( "String", "templateBody", new String[] { "@Column(length = 8000)" } ) );
		notificationTemplate.setProperties( p );

		//Attachments
		Clazz attachment = new Clazz();
		attachment.setClassName( "Attachment" );
		attachment.setAboveVisibility( UpVisibility.TRUE_R );
		attachment.setBelowVisibility( true );
		attachment.setImports( new String[] { "java.util.List", "com.tierconnect.riot.appcore.entities.*", "java.sql.*", "javax.annotation.*" } );
		attachment.setAnnotations( new String[] { "@Cacheable(value = false)" } );
		notificationTemplate.setGenerateController(true);
		notificationTemplate.setGenerateControllerInsert(false);
		notificationTemplate.setGenerateControllerDelete( false );
		p = new ArrayList<Property>();
		p.add( property( "Long", "id", new String[] { "@Id", "@GeneratedValue" } ) );
		p.add( property( "Blob", "fileAttachment" ) );
		p.add( property( "String", "name" ) );
		p.add( property( "String", "type" ) );
		p.add( property( "Long", "size" ) );
		p.add( property( "String", "comment" ) );
		p.add( property( "Date", "dateUploaded" ) );
		p.add( property( "String", "operationOverFile" ) );
		p.add( property( "User", "uploadedBy", new String[] { "@ManyToOne", "@NotNull" } ) );
		attachment.setProperties(p);

		Clazz thingTypePath = new Clazz();
		thingTypePath.setClassName("ThingTypePath");
		thingTypePath.setAboveVisibility( UpVisibility.FALSE);
		thingTypePath.setBelowVisibility(true);
		thingTypePath.setAnnotations(new String[]{"@Table(name = \"thingtypepath\")"});
		p = new ArrayList<Property>();
		p.add( property( "Long", "id", new String[] { "@Id", "@GeneratedValue" } ) );
		p.add( property( "ThingType", "originThingType", new String[] { "@ManyToOne(fetch=FetchType.LAZY)" } ) );
		p.add( property( "ThingType", "destinyThingType", new String[] { "@ManyToOne(fetch=FetchType.LAZY)" } ) );
		p.add(property("String", "path"));
		thingTypePath.setProperties( p );

		//Parameters
		Clazz descriptor = new Clazz();
		descriptor.setClassName( "Parameters");
		descriptor.setAboveVisibility(UpVisibility.TRUE_R);
		descriptor.setBelowVisibility( true );
		descriptor.setGenerateController( true );
		p = new ArrayList<Property>();
		p.add( property( "Long", "id", new String[] { "@Id", "@GeneratedValue" } ) );
		p.add(property( "String", "category"));
		p.add(property("String", "code"));
		p.add(property("String", "appResourceCode"));
		p.add(property("String", "value", new String[] { "@Column(length = 8000)"}));
		descriptor.setProperties( p );

		//BackgroundProcess
		Clazz backgroundProcess = new Clazz();
		backgroundProcess.setClassName( "BackgroundProcess");
		backgroundProcess.setAboveVisibility(UpVisibility.TRUE_R);
		backgroundProcess.setBelowVisibility( true );
		backgroundProcess.setGenerateController( true);
		backgroundProcess.setImports( new String[] { "java.util.List", "com.tierconnect.riot.appcore.entities.*", "java.sql.*", "javax.annotation.*" } );
		p = new ArrayList<Property>();
		p.add( property( "Long", "id", new String[] { "@Id", "@GeneratedValue" } ) );
		p.add( property( "Date", "iniDate" ) );
		p.add( property( "User", "createdByUser", new String[] { "@ManyToOne(fetch=FetchType.LAZY)", "@NotNull" } ) );
		p.add( property( "Date", "endDate" ) );
		p.add( property( "Long", "totalAffectedRecords" ) );
		p.add( property( "Long", "totalOmittedRecords" ) );
		p.add( property( "Long", "totalRecords" ) );
		p.add( property( "Long", "processTime" ) );
		p.add( property( "String", "typeProcess" ) );
		p.add( property( "String", "status"));
		p.add( property( "boolean", "checked"));
		p.add( property( "Integer", "progress"));
		p.add( property( "String", "threadName"));
		p.add( property( "String", "fileName"));
		p.add( property( "String", "thingTypes", new String[] { "@Column(length = 8000)"}));
		backgroundProcess.setProperties( p );

		//BackgroundProcessDetail
		Clazz backgroundProcessDetail = new Clazz();
		backgroundProcessDetail.setClassName( "BackgroundProcessDetail");
		backgroundProcessDetail.setAboveVisibility(UpVisibility.TRUE_R);
		backgroundProcessDetail.setBelowVisibility( true );
		backgroundProcessDetail.setGenerateController( false );
		backgroundProcessDetail.setGenerateService( true );
		backgroundProcessDetail.setImports( new String[] { "java.util.List", "com.tierconnect.riot.appcore.entities.*", "java.sql.*", "javax.annotation.*" } );
		p = new ArrayList<Property>();
		p.add( property( "Long", "id", new String[] { "@Id", "@GeneratedValue" } ) );
		p.add( property( "BackgroundProcess", "backgroundProcess", new String[] { "@ManyToOne(fetch=FetchType.LAZY)", "@NotNull" } ) );
		p.add( property( "ThingType", "thingType", new String[] { "@ManyToOne(fetch=FetchType.LAZY)" }));
		p.add( property( "String", "query", new String[] { "@Column(name=\"query\", length = 4000)"}));
		p.add( property( "String", "valuesToChange", new String[] { "@Column(name=\"valuesToChange\", length = 4000)"}));
		p.add( property( "Date", "iniDate" ) );
		p.add( property( "Date", "endDate" ) );
		p.add( property( "Long", "totalAffectedRecords" ) );
		p.add( property( "Long", "totalOmittedRecords" ) );
		p.add( property( "Long", "totalRecords" ) );
		p.add( property( "Long", "processTime" ) );
		p.add( property( "String", "status"));
		backgroundProcessDetail.setProperties( p );

		//BackgroundProcessDetailLog
		Clazz backgroundProcessDetailLog = new Clazz();
		backgroundProcessDetailLog.setClassName( "BackgroundProcessDetailLog");
		backgroundProcessDetailLog.setAboveVisibility(UpVisibility.TRUE_R);
		backgroundProcessDetailLog.setBelowVisibility( true );
		backgroundProcessDetailLog.setGenerateController( true );
		backgroundProcessDetailLog.setImports( new String[] { "java.util.List", "com.tierconnect.riot.appcore.entities.*", "java.sql.*", "javax.annotation.*" } );
		p = new ArrayList<Property>();
		p.add( property( "Long", "id", new String[] { "@Id", "@GeneratedValue" } ) );
		p.add( property( "Long", "thingId", new String[] {"@NotNull"}));
		p.add( property( "BackgroundProcessDetail", "backgroundProcessDetail", new String[] { "@ManyToOne(fetch=FetchType.LAZY)", "@Null" } ) );
		p.add( property( "String", "status"));
		p.add( property( "String", "serialNumber"));
		p.add( property( "String", "thingTypeCode"));
		backgroundProcessDetailLog.setProperties( p );

		//BackgroundProcessEntity
		Clazz backgroundProcessEntity = new Clazz();
		backgroundProcessEntity.setClassName( "BackgroundProcessEntity");
		backgroundProcessEntity.setAboveVisibility(UpVisibility.TRUE_R);
		backgroundProcessEntity.setBelowVisibility( true );
		backgroundProcessEntity.setGenerateController( false );
		backgroundProcessEntity.setImports( new String[] { "java.util.List", "com.tierconnect.riot.appcore.entities.*", "java.sql.*", "javax.annotation.*" } );
		p = new ArrayList<Property>();
		p.add( property( "Long", "id", new String[] { "@Id", "@GeneratedValue" } ) );
		p.add( property( "String", "moduleName", new String[] {"@NotNull"}));
		p.add( property( "BackgroundProcess", "backgroundProcess", new String[] { "@ManyToOne(fetch=FetchType.LAZY)", "@NotNull" } ) );
		p.add( property( "String", "columnName"));
		p.add( property( "String", "columnValue"));
		backgroundProcessEntity.setProperties( p );


		/**
		 * classes for data analytics/machine learning
		 */

		Clazz mlBusinessModel = new Clazz();
		mlBusinessModel.setClassName( "MlBusinessModel" );
		mlBusinessModel.setTableName( "ml_business_model" );
		mlBusinessModel.setAboveVisibility( UpVisibility.TRUE_R );
		mlBusinessModel.setBelowVisibility( true );
		mlBusinessModel.setImports(
				new String[] {
						"java.util.List", "com.tierconnect.riot.appcore.entities.*", "java.sql.*", "javax.annotation.*" } );
		mlBusinessModel.setGenerateController(false);
		mlBusinessModel.setGenerateService(true);

		p = new ArrayList<Property>();
		p.add( property( "Long", "id", new String[] { "@Id", "@GeneratedValue" } ) );
		p.add( property( "String", "name" ) );
		p.add( property( "String", "description" ) );
		p.add( property("List<MlBusinessModelPredictor>", "predictors",
				new String[]{"@OneToMany(mappedBy=\"businessModel\", fetch=FetchType.LAZY, cascade = CascadeType.ALL)"}));
		p.add( property( "String", "appName") );
		p.add( property( "String", "jar" ) );
		p.add( property( "Date", "modifiedDate" ) );

		mlBusinessModel.setProperties( p );


		Clazz mlBusinessModelPredictor = new Clazz();
		mlBusinessModelPredictor.setClassName( "MlBusinessModelPredictor" );
		mlBusinessModelPredictor.setTableName( "ml_business_model_predictor" );
		mlBusinessModelPredictor.setAboveVisibility( UpVisibility.TRUE_R );
		mlBusinessModelPredictor.setBelowVisibility( true );
		mlBusinessModelPredictor.setImports(
				new String[] {
						"java.util.List", "com.tierconnect.riot.appcore.entities.*", "java.sql.*", "javax.annotation.*" } );
		mlBusinessModelPredictor.setGenerateController(false);
		mlBusinessModelPredictor.setGenerateService(true);

		p = new ArrayList<Property>();
		p.add( property( "Long", "id", new String[] { "@Id", "@GeneratedValue" } ) );
		p.add( property( "MlBusinessModel", "businessModel", new String[] { "@ManyToOne(fetch=FetchType.LAZY)", "@NotNull" }));
		p.add( property( "String", "name" ) );
		p.add( property( "String", "type" ) );

		mlBusinessModelPredictor.setProperties( p );


		// models
		Clazz mlModel = new Clazz();
		mlModel.setClassName( "MlModel" );
		mlModel.setTableName( "ml_model" );
		mlModel.setAboveVisibility( UpVisibility.TRUE_R );
		mlModel.setBelowVisibility( true );
		mlModel.setImports(
				new String[] {
						"java.util.List", "com.tierconnect.riot.appcore.entities.*", "java.sql.*", "javax.annotation.*" } );
		mlModel.setGenerateController(false);
		mlModel.setGenerateService(true);

		p = new ArrayList<Property>();
		p.add( property( "Long", "id", new String[] { "@Id", "@GeneratedValue" } ) );
		p.add( property( "Group", "group", new String[] { "@ManyToOne(fetch=FetchType.LAZY)", "@NotNull"  }));
		p.add( property( "MlExtraction", "extraction", new String[] { "@ManyToOne(fetch=FetchType.LAZY)", "@NotNull"  }));
		p.add( property( "String", "name" ) );
		p.add( property( "String", "comments" ) );
		p.add( property( "String", "sparkJobId" ) );
		p.add( property( "String", "status" ) );
		p.add( property( "String", "uuid" ) );
		p.add( property( "String", "error" ) );

		mlModel.setProperties( p );

		// model types associated to tenants
		Clazz mlBusinessModelTenant = new Clazz();
		mlBusinessModelTenant.setClassName( "MlBusinessModelTenant" );
		mlBusinessModelTenant.setTableName( "ml_business_model_tenant" );
		mlBusinessModelTenant.setAboveVisibility( UpVisibility.TRUE_R );
		mlBusinessModelTenant.setBelowVisibility( true );
		mlBusinessModelTenant.setImports(
				new String[] {
						"java.util.List", "com.tierconnect.riot.appcore.entities.*", "java.sql.*", "javax.annotation.*" } );
		mlBusinessModelTenant.setGenerateController(false);
		mlBusinessModelTenant.setGenerateService(true);

		p = new ArrayList<Property>();
		p.add( property( "Long", "id", new String[] { "@Id", "@GeneratedValue" } ) );
		p.add( property( "MlBusinessModel", "businessModel",
				new String[] { "@ManyToOne(fetch=FetchType.LAZY, cascade = {CascadeType.ALL})" }));
		p.add( property( "Group", "group", new String[] { "@ManyToOne(fetch=FetchType.LAZY)" }));
		p.add( property( "String", "collection" ) );
		p.add( property( "Boolean", "enabled" ) );

		mlBusinessModelTenant.setProperties( p );


		// features extractions
		Clazz mlExtraction = new Clazz();
		mlExtraction.setClassName( "MlExtraction" );
		mlExtraction.setTableName( "ml_extraction" );
		mlExtraction.setAboveVisibility( UpVisibility.TRUE_R );
		mlExtraction.setBelowVisibility( true );
		mlExtraction.setImports(
				new String[] {
						"java.util.List", "com.tierconnect.riot.appcore.entities.*", "java.sql.*", "javax.annotation.*" } );
		mlExtraction.setGenerateController(false);
		mlExtraction.setGenerateService(true);

		p = new ArrayList<Property>();
		p.add( property( "Long", "id", new String[] { "@Id", "@GeneratedValue" } ) );
		p.add( property( "MlBusinessModel", "businessModel", new String[] { "@ManyToOne(fetch=FetchType.LAZY)" }));
		p.add( property( "Group", "group", new String[] { "@ManyToOne(fetch=FetchType.LAZY)", "@NotNull" }));
		p.add(property("List<MlExtractionPredictor>", "predictors",
				new String[]{"@OneToMany(mappedBy=\"extraction\", fetch=FetchType.LAZY, cascade = CascadeType.ALL)"}));
		p.add( property( "String", "name" ) );
		p.add( property( "String", "comments" ) );
		p.add( property( "String", "jobId" ) );
		p.add( property( "String", "status" ) );
		p.add( property( "String", "uuid" ) );
		p.add( property( "Date", "startDate" ) );
		p.add( property( "Date", "endDate" ) );

		mlExtraction.setProperties( p );


		// extraction predictors
		Clazz mlExtractionPredictor = new Clazz();
		mlExtractionPredictor.setClassName( "MlExtractionPredictor" );
		mlExtractionPredictor.setTableName( "ml_extraction_predictor" );
		mlExtractionPredictor.setAboveVisibility( UpVisibility.TRUE_R );
		mlExtractionPredictor.setBelowVisibility( true );
		mlExtractionPredictor.setImports(
				new String[] {
						"java.util.List", "com.tierconnect.riot.appcore.entities.*", "java.sql.*", "javax.annotation.*" } );
		mlExtractionPredictor.setGenerateController(false);
		mlExtractionPredictor.setGenerateService(true);

		p = new ArrayList<Property>();
		p.add( property( "Long", "id", new String[] { "@Id", "@GeneratedValue" } ) );
		p.add( property( "MlExtraction", "extraction", new String[] { "@ManyToOne(fetch=FetchType.LAZY)", "@NotNull" }));
		p.add( property( "ThingType", "thingType", new String[] { "@ManyToOne(fetch=FetchType.LAZY)", "@NotNull" }));
		p.add( property( "MlBusinessModelPredictor", "businessModelPredictor",
				new String[] { "@ManyToOne(fetch=FetchType.LAZY)", "@NotNull" }));
		p.add( property( "String", "propertyName" ) );
		p.add( property( "String", "propertyPath" ) );

		mlExtractionPredictor.setProperties( p );



		// predictions
		Clazz mlPrediction = new Clazz();
		mlPrediction.setClassName( "MlPrediction" );
		mlPrediction.setTableName( "ml_prediction" );
		mlPrediction.setAboveVisibility( UpVisibility.TRUE_R );
		mlPrediction.setBelowVisibility( true );
		mlPrediction.setImports(
				new String[] {
						"java.util.List", "com.tierconnect.riot.appcore.entities.*", "java.sql.*", "javax.annotation.*" } );
		mlPrediction.setGenerateController(false);
		mlPrediction.setGenerateService(true);

		p = new ArrayList<Property>();
		p.add( property( "Long", "id", new String[] { "@Id", "@GeneratedValue" } ) );
		p.add( property( "Group", "group", new String[] { "@ManyToOne(fetch=FetchType.LAZY)", "@NotNull"  }));
		p.add( property( "MlModel", "trainedModel", new String[] { "@ManyToOne(fetch=FetchType.LAZY)", "@NotNull"  }));
		p.add( property( "String", "name" ) );
		p.add( property( "String", "comments" ) );
		p.add( property( "String", "uuid" ) );
		p.add( property( "Date", "modifiedDate" ) );
		p.add( property("List<MlPredictionPredictor>", "predictors",
				new String[]{"@OneToMany(mappedBy=\"prediction\", fetch=FetchType.LAZY, cascade = CascadeType.ALL)"}));

		mlPrediction.setProperties( p );


		// predictions' predictors
		Clazz mlPredictionPredictor = new Clazz();
		mlPredictionPredictor.setClassName( "MlPredictionPredictor" );
		mlPredictionPredictor.setTableName( "ml_prediction_predictor" );
		mlPredictionPredictor.setAboveVisibility( UpVisibility.TRUE_R );
		mlPredictionPredictor.setBelowVisibility( true );
		mlPredictionPredictor.setImports(
				new String[] {
						"java.util.List", "com.tierconnect.riot.appcore.entities.*", "java.sql.*", "javax.annotation.*" } );
		mlPredictionPredictor.setGenerateController(false);
		mlPredictionPredictor.setGenerateService(true);

		p = new ArrayList<Property>();
		p.add( property( "Long", "id", new String[] { "@Id", "@GeneratedValue" } ) );
		p.add( property( "MlPrediction", "prediction", new String[] { "@ManyToOne(fetch=FetchType.LAZY)", "@NotNull"  }));
		p.add( property( "String", "name" ) );
		p.add( property( "String", "value" ) );

		mlPredictionPredictor.setProperties( p );

        Clazz folder = new Clazz();
        folder.setClassName( "Folder" );
        folder.setImports( new String[] { "java.util.List",
                "java.util.Set",
				"com.tierconnect.riot.appcore.entities.*",
                "com.tierconnect.riot.iot.entities.ReportDefinition",
                "com.tierconnect.riot.appcore.entities.Group", "javax.persistence.*" } );
        folder.setAboveVisibility(UpVisibility.TRUE_R);
        folder.setBelowVisibility(true);
        p = new ArrayList<>();
        p.add( property( "Long", "id", new String [] { "@Id", "@GeneratedValue" } ) );
        p.add( property( "String", "code" ) );
        p.add( property( "String", "name" ) );
        p.add( property( "Date", "creationDate" ) );
        p.add( property( "Date", "lastModificationDate" ) );
        p.add( property( "Group", "group", new String[]{"@ManyToOne(fetch=FetchType.LAZY)", "@NotNull"}));
        p.add( property( "Folder", "folderId", new String[]{"@ManyToOne(fetch=FetchType.LAZY)"}));
        p.add( property( "Long", "sequence") );
        p.add( property( "String", "typeElement"));
        p.add(property(
                "Set<ReportDefinition>",
                "reportDefinitions",
                new String[]{"@OneToMany(mappedBy=\"folder\", fetch=FetchType.LAZY, cascade = CascadeType.ALL)"}));
		p.add( property( "User", "createdByUser", new String[] { "@ManyToOne(fetch=FetchType.LAZY)", "@NotNull" } ) );

		folder.setGenerateController(true);
        folder.setGenerateControllerDelete(false);
        folder.setGenerateService(true);
        folder.setProperties(p);

		// Smart Contract Definition
		Clazz smartContractDefinition = new Clazz();
		smartContractDefinition.setClassName("SmartContractDefinition");
		smartContractDefinition.setAboveVisibility(UpVisibility.TRUE_R);
		smartContractDefinition.setBelowVisibility(true);
		smartContractDefinition.setGenerateController(false);
		smartContractDefinition.setImports(new String[]{"com.tierconnect.riot.appcore.entities.Group"});
		p = new ArrayList<>();
		p.add(property("Long", "id", new String[]{"@Id", "@GeneratedValue"}));
		p.add(property("Group", "group", new String[]{"@ManyToOne(fetch=FetchType.LAZY)"}));
		p.add(property("String", "name"));
		p.add(property("String", "description"));
		p.add(property("String", "issuingPublicKey"));
		p.add(property("Boolean", "enabled"));
		p.add(property("int", "initialState"));
		p.add(property("int", "finalState"));
		p.add(property("String", "documentThingType", new String[]{"@Column(length=8000)"}));
		p.add(property("String", "items", new String[]{"@Column(length=8000)"}));
		p.add(property("String", "states", new String[]{"@Column(length=8000)"}));
		p.add(property("String", "roles", new String[]{"@Column(length=8000)"}));
		p.add(property("String", "transitions", new String[]{"@Column(length=8000)"}));
		p.add(property("String", "constrains", new String[]{"@Column(length=8000)"}));
		smartContractDefinition.setProperties(p);

		// Smart Contract Party
		Clazz smartContractParty = new Clazz();
		smartContractParty.setClassName("SmartContractParty");
		smartContractParty.setTableName("smartcontractparty");
		smartContractParty.setTableAnnotations(new String[]{"uniqueConstraints={ @UniqueConstraint(columnNames={\"blockchainId\",\"group_id\"})}"});
		smartContractParty.setAboveVisibility(UpVisibility.TRUE_R);
		smartContractParty.setBelowVisibility(true);
		smartContractParty.setGenerateController(false);
		smartContractParty.setImports(new String[]{"com.tierconnect.riot.appcore.entities.Group"});
		p = new ArrayList<>();
		p.add(property("Long", "id", new String[]{"@Id", "@GeneratedValue"}));
		p.add(property("Group", "group", new String[]{"@ManyToOne(fetch=FetchType.LAZY)"}));
		p.add(property("String", "name"));
		p.add(property("String", "blockchainId"));
		p.add(property("String", "description"));
		smartContractParty.setProperties(p);

		// Smart Contract Config
		Clazz smartContractConfig = new Clazz();
		smartContractConfig.setClassName("SmartContractConfig");
		smartContractConfig.setTableName("smartcontractconfig");
		smartContractConfig.setAboveVisibility(UpVisibility.TRUE_R);
		smartContractConfig.setBelowVisibility(true);
		smartContractConfig.setGenerateController(true);
		smartContractConfig.setTableAnnotations(new String[]{"uniqueConstraints={ @UniqueConstraint(columnNames={\"contractSerial\"})}"});

		p = new ArrayList<>();
		p.add(property("Long", "id", new String[]{"@Id", "@GeneratedValue"}));
		p.add( property( "String", "contractSerial", new String[]{"@NotNull"} ) );
		p.add(property("String", "blockchainId"));
		p.add(property("String", "config", new String[]{"@Column(length=8000)"}));
		p.add(property("String", "thingTypeCode"));
		p.add(property("Boolean", "transitionEnabled"));
		p.add(property("String", "transitionInExecution", new String[]{"@Column(length=8000)"}));
		p.add(property("String", "fields", new String[]{"@Column(length=8000)"}));
		p.add(property("String", "roles", new String[]{"@Column(length=8000)"}));
		p.add(property("String", "assignedRoles", new String[]{"@Column(length=8000)"}));
		smartContractConfig.setProperties(p);
		smartContractConfig.setGenerateControllerInsert( false );

        // Scheduled Rule
        Clazz scheduledRule = new Clazz();
        scheduledRule.setClassName("ScheduledRule");
        scheduledRule.setAboveVisibility(UpVisibility.TRUE_R_U_SPECIAL);
        scheduledRule.setGenerateControllerInsert(false);
		scheduledRule.setGenerateControllerDelete(false);
        scheduledRule.setBelowVisibility(true);
        scheduledRule.setImports(new String[]{"java.util.List",
                "com.tierconnect.riot.appcore.entities.Group"});
        p = new ArrayList<>();
        p.add(property("Long", "id", new String[] { "@Id", "@GeneratedValue" }));
        p.add(property("Group", "group", new String [] { "@ManyToOne(fetch=FetchType.LAZY)" } ));
        p.add( property( "ReportDefinition", "reportDefinition", new String[] { "@ManyToOne(fetch=FetchType.LAZY)" } ) );
        p.add(property("Edgebox", "edgebox", new String[]{"@ManyToOne(fetch=FetchType.LAZY)"}));
        p.add(property("String", "name"));
        p.add(property("String", "code"));
        p.add(property("Boolean", "active"));
        p.add(property("String", "status"));
        p.add(property("String", "rule_execution_mode"));
        p.add(property("String", "description"));
        p.add(property("String", "cron_expression"));
		p.add(property("String", "extra_configuration"));
        scheduledRule.setProperties(p);

        AppgenPackage pk2 = new AppgenPackage("com.tierconnect.riot.iot");
        pk2.addClazz(gateway);
        pk2.addClazz(rule);
		pk2.addClazz(importExport);
        pk2.addClazz(localMap);
        pk2.addClazz(localMapPoint);
        pk2.addClazz(logicalReader);
        pk2.addClazz(pickListFields);
		pk2.addClazz(reportDefinition);
		pk2.addClazz(reportGroupBy);
        pk2.addClazz(reportProperty);
		pk2.addClazz(reportFilter);
		pk2.addClazz(reportCustomFilter);
		pk2.addClazz(reportRules);
		pk2.addClazz(thing);
		//pk2.addClazz(thingField);
		pk2.addClazz(thingParentHistory);
		pk2.addClazz(thingType);
		pk2.addClazz(thingTypeField);
		pk2.addClazz(zone);
		pk2.addClazz(zoneGroup);
		pk2.addClazz(zonePoint);
		pk2.addClazz(zoneType);
        pk2.addClazz(zoneProperty);
        pk2.addClazz(zonePropertyValue);
		pk2.addClazz(thingTypeMap);
		pk2.addClazz(thingImage);
		pk2.addClazz(unit);
		pk2.addClazz(shift);
		pk2.addClazz(shiftZone);
		pk2.addClazz(shiftThing);
		pk2.addClazz(thingTypePath);

        pk2.addClazz(customApplication);
        pk2.addClazz(customObject);
        pk2.addClazz(customObjectType);
        pk2.addClazz(customFieldType);
		pk2.addClazz(customField);
        pk2.addClazz(customFieldValue);
		pk2.addClazz(reportEntryOption);
		pk2.addClazz(reportEntryOptionProperty);

		pk2.addClazz(thingTypeTemplateCategory);
		pk2.addClazz(thingTypeTemplate);
		pk2.addClazz(thingTypeFieldTemplate);
		pk2.addClazz(dataType);
		pk2.addClazz(entryFormPropertyData);
		pk2.addClazz(actionConfiguration);
		pk2.addClazz(reportActions);
		pk2.addClazz(logExecutionAction);
		pk2.addClazz(sequence);
		pk2.addClazz(notificationTemplate);
		pk2.addClazz(attachment);
		pk2.addClazz(reportDefinitionConfig);
		pk2.addClazz(descriptor);
		pk2.addClazz(backgroundProcess);
		pk2.addClazz(backgroundProcessDetail);
		pk2.addClazz(backgroundProcessDetailLog);
		pk2.addClazz(backgroundProcessEntity);

		// analytics - new database tables
		pk2.addClazz(mlBusinessModel);
		pk2.addClazz(mlBusinessModelPredictor);

		pk2.addClazz(mlModel);

		pk2.addClazz(mlBusinessModelTenant);
		pk2.addClazz(mlExtraction);
		pk2.addClazz(mlExtractionPredictor);

		pk2.addClazz(mlPrediction);
		pk2.addClazz(mlPredictionPredictor);

        pk2.addClazz(folder);

		pk2.addClazz(smartContractDefinition);
		pk2.addClazz(smartContractParty);
		pk2.addClazz(smartContractConfig);
        pk2.addClazz(scheduledRule);

		Application app = new Application();
		app.addPackage(pk2);


		// force UPDATE of Clazz.clazzes variable !
		AppgenExtends a = new AppgenExtends();
		Application ap = a.getApplication();
        return app;
	}
}
