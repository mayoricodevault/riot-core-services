package com.tierconnect.riot.iot.popdb;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.tierconnect.riot.appcore.controllers.RiotShiroRealm;
import com.tierconnect.riot.appcore.dao.CassandraUtils;
import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.popdb.PopDBRequired;
import com.tierconnect.riot.appcore.services.*;
import com.tierconnect.riot.appcore.utils.Configuration;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.fmc.utils.FMCConstants;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.sdk.servlet.security.ApiKeyToken;

import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.config.IniSecurityManagerFactory;
import org.apache.shiro.mgt.*;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.Factory;
import org.hibernate.Transaction;

import com.tierconnect.riot.iot.entities.ZoneType;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;

public class PopDBFMC
{
	private static final Logger logger = Logger.getLogger( PopDBMojixRetail.class );

	public static void main( String args[] ) throws Exception
	{
		PopDBRequired.initJDBCDrivers();
        System.getProperties().put("hibernate.hbm2ddl.auto", "update");
		CassandraUtils.init(Configuration.getProperty("cassandra.host"), Configuration.getProperty("cassandra.keyspace"));
		PopDBFMC popdb = new PopDBFMC();
		Transaction transaction = GroupService.getGroupDAO().getSession().getTransaction();
		transaction.begin();
		PopDBIOTUtils.initShiroWithRoot();
		popdb.run();

		transaction.commit();
		System.exit( 0 );
	}

	public void run()
	{
		createData();

		PopulateDBRiotMaker prm = new PopulateDBRiotMaker();
		prm.demo();
	}

	private void createData()
	{
		PopDBMojixUtils.modifyExistingRecords();

		User rootUser = UserService.getInstance().getRootUser();
		Group rootGroup = GroupService.getInstance().getRootGroup();
		GroupType tenantGroupType = GroupTypeService.getInstance().getTenantGroupType();

        GroupField ab=GroupFieldService.getInstance().get(8L);
        ab.setValue("4");
        GroupFieldService.getInstance().update(ab);
        GroupField ac=GroupFieldService.getInstance().get(27L);
        ac.setValue("4");
        GroupFieldService.getInstance().update(ac);
        GroupField ad=GroupFieldService.getInstance().get(9L);
        ad.setValue("4");
        GroupFieldService.getInstance().update(ad);

		Group mojix= GroupService.getInstance().get(2L);
		mojix.setName("FMC Surface");
		mojix.setCode("fmc.surface");
		// facility, test wing and zone
		GroupType storeGroupType = GroupTypeService.getInstance().get(3L);
				storeGroupType.setName("Group");

		 GroupType areaGroupType = PopDBUtils.popGroupType( "Base", mojix,storeGroupType, "Base" );


        //Group surface = PopDBUtils.popGroup( "Surface", "s", mojix, storeGroupType, "" );
        //Groups for plant
        Group surface = GroupService.getInstance().get(3L);
        surface.setName("Surface");
        surface.setCode("7395a");
		
		Group Odessa = PopDBUtils.popGroup("Odessa", "7370", surface, areaGroupType, "");
        Group waynesburg = PopDBUtils.popGroup("waynesburg", "7395", surface, areaGroupType, "");
		Group Oklahoma = PopDBUtils.popGroup( "Oklahoma Dist", "7350", surface, areaGroupType, "" );
		Group ops = PopDBUtils.popGroup( "Oklahoma Ops", "7550", surface, areaGroupType, "" );
		Group Brighton = PopDBUtils.popGroup( "Brighton", "7393", surface, areaGroupType, "" );
		Group Williston = PopDBUtils.popGroup( "Williston", "7440", surface, areaGroupType, "" );
		Group Antonio = PopDBUtils.popGroup( "San Antonio", "7394", surface, areaGroupType, "" );
		Group Muncy = PopDBUtils.popGroup( "Muncy", "7386", surface, areaGroupType, "" );
		Group Vernal = PopDBUtils.popGroup( "Vernal", "7430", surface, areaGroupType, "" );
		Group Casper = PopDBUtils.popGroup( "Casper", "7420", surface, areaGroupType, "" );
		Group Bakersfield = PopDBUtils.popGroup( "Bakersfield", "7450", surface, areaGroupType, "" );
		Group RockSprings = PopDBUtils.popGroup( "Rock Springs", "7410", surface, areaGroupType, "" );
		Group Justin = PopDBUtils.popGroup( "Justin", "7360", surface, areaGroupType, "" );
		Group Pampa = PopDBUtils.popGroup( "Pampa", "7380", surface, areaGroupType, "" );
		Group GrandJn  = PopDBUtils.popGroup( "Grand Jn ", "7470", surface, areaGroupType, "" );
		Group Fairfield = PopDBUtils.popGroup( "Fairfield", "7345", surface, areaGroupType, "" );

		
        
		Role tenantRole = RoleService.getInstance().getTenantAdminRole();
		tenantRole.setGroup( mojix );

		RoleService.getInstance().update( tenantRole );

		PopDBIOTUtils.popShift( waynesburg, "DAY-M-W", 800L, 1700L, "23456", "DAY-M-W" );
		LocalMap localmap=PopDBIOTUtils.populateFacilityMap("Waynesburg", "images/fmc.png", waynesburg, -80.129687, 1356, 39.908466, 866, -80.129687,39.908466 , 0,"ft");
        LocalMap localmap1=PopDBIOTUtils.populateFacilityMap("Surface-Odessa", "images/fmc.png", Odessa, -102.399292, 77, 31.873378, 31, -102.399292, 31.873378, 0,"ft");
		

		//createReportDefinitionData2( waynesburg, storeGroupType, rootUser );


        ZoneType ztd=PopDBIOTUtils.popZoneType(surface, "FMC Zone", "FMCZone", null);
		List<ZoneProperty> zoneProperties = new ArrayList<>();
        ZoneProperty zpa=PopDBIOTUtils.popZoneProperty("Zone Code Name", 1,ztd);
		zoneProperties.add(zpa);
        //Zone Group
        ZoneGroup zg=PopDBIOTUtils.popZoneGroup(localmap,"Waynesburg");
        ZoneGroup zg1=PopDBIOTUtils.popZoneGroup(localmap1,"Odessa");

		// Zones
		Zone z3 = PopDBIOTUtils.popZone( waynesburg, localmap, "InTransit_way", "#FFCC33", "On-Site" );
		PopDBIOTUtils.popZonePoint( z3, 0, -80.1276932336, 39.9080719667 );
		PopDBIOTUtils.popZonePoint( z3, 1,-80.126491604, 39.9080719667 );
		PopDBIOTUtils.popZonePoint( z3, 2, -80.1265130617, 39.9072654396);
		PopDBIOTUtils.popZonePoint( z3, 3, -80.1276074029, 39.9072325199 );
		z3.setZoneType(ztd);
		z3.setCode("7395_InTransit");
        PopDBIOTUtils.popZonePropertyValue("In Transit", z3,1L);
		
		Zone z4 = PopDBIOTUtils.popZone( waynesburg, localmap, "InUse_way", "#FFCC33", "On-Site" );
		PopDBIOTUtils.popZonePoint( z4, 0,-80.1296029665, 39.9081048859 );
		PopDBIOTUtils.popZonePoint( z4, 1,-80.1285086252, 39.9081048859 );
		PopDBIOTUtils.popZonePoint( z4, 2,-80.1285086252, 39.9073477387 );
		PopDBIOTUtils.popZonePoint( z4, 3,-80.1295600511, 39.9073641985 );
		z4.setZoneType(ztd);
		z4.setCode("7395_InUse");
        PopDBIOTUtils.popZonePropertyValue("In Use", z4,1L);
		
		Zone z5 = PopDBIOTUtils.popZone( Odessa, localmap1, "New02", "#FFCC33", "On-Site" );
		PopDBIOTUtils.popZonePoint( z5, 0, -102.3992785889,31.8735311804);
		PopDBIOTUtils.popZonePoint( z5, 1,-102.3991940993, 31.8735311804);
		PopDBIOTUtils.popZonePoint( z5, 2,-102.3991927582,31.8734856249 );
		PopDBIOTUtils.popZonePoint( z5, 3,-102.3992705423, 31.8734856249);
		z5.setZoneType(ztd);
		z5.setCode("7370_New");
        PopDBIOTUtils.popZonePropertyValue("New", z5,1L);
		
		Zone z6 = PopDBIOTUtils.popZone( waynesburg, localmap, "NI_way", "#FFCC33", "On-Site" );
		PopDBIOTUtils.popZonePoint( z6, 0, -80.1256547548, 39.908055507 );
		PopDBIOTUtils.popZonePoint( z6, 1,-80.1243458368, 39.908055507);
		PopDBIOTUtils.popZonePoint( z6, 2, -80.1243243791, 39.9072489798);
		PopDBIOTUtils.popZonePoint( z6, 3, -80.1255903818, 39.9073056747 );
		z6.setZoneType(ztd);
		z6.setCode("7395_NI");
        PopDBIOTUtils.popZonePropertyValue("Needs Inspection", z6,1L);
		
		Zone z7 = PopDBIOTUtils.popZone( Odessa, localmap1, "OdessaGate1", "#FFCC33", "On-Site" );
		PopDBIOTUtils.popZonePoint( z7, 0, -102.3992209214, 31.8734418295 );
		PopDBIOTUtils.popZonePoint( z7, 1, -102.3992142159, 31.8733848851);
		PopDBIOTUtils.popZonePoint( z7, 2, -102.3991820294, 31.8733928573);
		PopDBIOTUtils.popZonePoint( z7, 3, -102.3991820294, 31.8734372739);
		z7.setZoneType(ztd);
		z7.setCode("7370_InUse");
        PopDBIOTUtils.popZonePropertyValue("In Use", z7,1L);
		
		Zone z8 = PopDBIOTUtils.popZone( Odessa, localmap1, "Repairable02", "#FFCC33", "On-Site" );
		PopDBIOTUtils.popZonePoint( z8, 0, -102.3991283852, 31.8735334582 );
		PopDBIOTUtils.popZonePoint( z8, 1, -102.3990519422, 31.8735368748);
		PopDBIOTUtils.popZonePoint( z8, 2, -102.3990506011, 31.8734935971);
		PopDBIOTUtils.popZonePoint( z8, 3, -102.3991270441, 31.8734867638);
		z8.setZoneType(ztd);
		z8.setCode("7370_Repairable");
        PopDBIOTUtils.popZonePropertyValue("Repairable", z8,1L);
		

        ThingType rfid= ThingTypeService.getInstance().get(1L);
        ThingType gps = ThingTypeService.getInstance().get(2L);
		ThingType asset = PopDBMojixUtils.popThingTypeAsset(surface, "FMC Asset");
		asset.setThingTypeCode(FMCConstants.FMC+"."+FMCConstants.ASSET);
		ThingType tag = PopDBMojixUtils.popThingTypeFMC(surface, "FMC Tag");
		tag.setThingTypeCode(FMCConstants.FMC+"."+FMCConstants.TAG);
        
		
		PopDBUtils.migrateFieldService("thing", "thing", "Thing", rootGroup, "Ownership Levels", "java.lang.Integer", 3L, false, "4");
		PopDBUtils.migrateFieldService("thingType", "thingType", "Thing Type", rootGroup, "Ownership Levels", "java.lang.Integer", 3L, false, "3");
        PopDBUtils.popGroupField(rootGroup, FieldService.getInstance().selectByName("thing"), "4");
        PopDBUtils.popGroupField(rootGroup, FieldService.getInstance().selectByName("thingType"), "3");
		PopDBIOTUtils.popThingTypeMap(asset, tag);
		

		// Users
		rootUser.setEmail( "root@company.com" );
		// User admin = PopDBUtils.popUser( "Admin", mojix, tenantRole );

		UserService.getInstance().update( rootUser );

		// TODO watch out for empty resources
		Role companyUser = PopDBUtils.popRole( "Store User", "Store User", new ArrayList<Resource>(), mojix, storeGroupType );
		Role companyadmin = PopDBUtils.popRole( "Store Administrator", "Store Administrator", new ArrayList<Resource>(), mojix,
				storeGroupType );

		//
		Role storeManager = PopDBUtils.popRole( "Store Manager", "Role store manager", null, mojix, storeGroupType );
		Role storeEmployee = PopDBUtils.popRole( "Store Employee", "Role store employee", null, mojix, storeGroupType );
		Role pantManager = PopDBUtils.popRole( "Pants Manager", "Pants manager", null, mojix, storeGroupType );
		Role reportManager = PopDBUtils.popRole( "Report Manager", "Report manager", null, mojix, null );
		// Role inventoryManager=PopDBUtils.popRole("Inventory Manager", "sm",
		// "Inventory manager", null, mojix, storeGroupType);

		List<Resource> resources1 = ResourceService.list();

		// List<Resource> resources = ResourceService.getInstance().list();
		for( Resource resource : resources1 )
		{
			if( resource.getName().toString().equals( "Reports" ) )
			{
				RoleResourceService.getInstance().insert( storeEmployee, resource, "x" );
				RoleResourceService.getInstance().insert( reportManager, resource, "x" );
			}


			if( resource.getName().toString().equals( "reportDefinition" ) || resource.getName().toString().equals( "reportFilter" )
					|| resource.getName().toString().equals( "reportProperty" ) )
			{

				// RoleResourceService.getInstance().insert( storeManager,
				// resource, "iuda" );
				RoleResourceService.getInstance().insert( storeEmployee, resource, "r" );
				RoleResourceService.getInstance().insert( reportManager, resource, "r" );

			}
			if( resource.getName().toString().equals( "$Pants" ) || resource.getName().toString().equals( "$Jackets" )
					|| resource.getName().toString().equals( "$Passive RFID Tags" ) )
			{
				if( resource.getName().toString().equals( "$Pants" ) )
				{
					RoleResourceService.getInstance().insert( pantManager, resource, "riuda" );
					// RoleResourceService.getInstance().insert( storeManager,
					// resource, "riuda" );
				}
				/*
				 * else{ RoleResourceService.getInstance().insert( storeManager,
				 * resource, "riuda" ); }
				 */

			}
			/*
			 * if (resource.getName().toString().equals("thing")) {
			 * RoleResourceService.getInstance().insert( storeManager, resource,
			 * "riuda" ); RoleResourceService.getInstance().insert(
			 * storeEmployee, resource, "riuda" );
			 * RoleResourceService.getInstance().insert( pantManager, resource,
			 * "riuda" ); }
			 */

			if( resource.getName().toString().equals( "localMap" ) )
			{
				RoleResourceService.getInstance().insert( pantManager, resource, "riuda" );
			}

		}

		// Roles for root
		// TODO: this should be done in POPDBAPPCORE or no ?
		Role rootRole = RoleService.getInstance().getRootRole();
		for( Resource resource : resources1 )
		{
			if( resource.getName().toString().startsWith( "$" ) )
			{
				RoleResourceService.getInstance().insert( rootRole, resource, "riuda" );
				RoleResourceService.getInstance().insert( tenantRole, resource, "riuda" );
			}
		}

		// Roles for store manager
		for( Resource resource : resources1 )
		{
			if (!resource.getName().startsWith("license")) {
				RoleResourceService.getInstance().insert(storeManager, resource, resource.getAcceptedAttributes());
			}

		}
		// user mojix
		User samUser = PopDBUtils.popUser( "samuel", "samuel", waynesburg, storeManager );
		samUser.setFirstName( "Samuel" );
		samUser.setLastName( "Levy" );
		samUser.setEmail( "samuel.levy@mojix.com " );

		Set<UserRole> copia = new HashSet<UserRole>();

		
		UserRole sam2 = new UserRole();
		sam2.setRole( storeEmployee );
		sam2.setUser( samUser );
		copia.add( sam2 );
		sam2 = UserRoleService.getInstance().insert( sam2 );
		samUser.setUserRoles( copia );

		// paulUser.setUserRoles(userRoles);
		

		
		
		User adminc = PopDBUtils.popUser( "adminc", "adminc", mojix, tenantRole );

		User adminp = PopDBUtils.popUser( "adminp", "adminp", waynesburg, companyadmin );
		User employee = PopDBUtils.popUser( "employee", waynesburg, storeEmployee );
	}

 
	private ReportFilter createReportFilter( String label, String propertyName, String propertyOrder, String operatorFilter, String value,
			Boolean isEditable, Long ttId, ReportDefinition reportDefinition )
	{
		ReportFilter reportFilter = new ReportFilter();
		reportFilter.setLabel( label );
		reportFilter.setPropertyName( propertyName );
		reportFilter.setDisplayOrder( Float.parseFloat( propertyOrder ) );
		reportFilter.setOperator( operatorFilter );
		reportFilter.setValue( value );
		reportFilter.setEditable( isEditable );
		reportFilter.setThingType( ThingTypeService.getInstance().get( ttId ) );
		reportFilter.setThingTypeField( ThingTypeFieldService.getInstance().getThingTypeFieldByName( propertyName ).get( 0 ) );
		reportFilter.setReportDefinition( reportDefinition );
		return reportFilter;
	}

	private ReportFilter createReportFilter( String label, String propertyName, String propertyOrder, String operatorFilter, String value,
			Boolean isEditable, ReportDefinition reportDefinition )
	{
		ReportFilter reportFilter = new ReportFilter();
		reportFilter.setLabel( label );
		reportFilter.setPropertyName( propertyName );
		reportFilter.setDisplayOrder( Float.parseFloat( propertyOrder ) );
		reportFilter.setOperator( operatorFilter );
		reportFilter.setValue( value );
		reportFilter.setEditable( isEditable );

		reportFilter.setReportDefinition( reportDefinition );
		return reportFilter;
	}

	private ReportProperty createReportProperty( String label, String propertyName, String propertyOrder, Long propertyTypeId,
			ReportDefinition reportDefinition )
	{
		ReportProperty reportProperty = new ReportProperty();
		reportProperty.setLabel( label );
		reportProperty.setPropertyName( propertyName );
		reportProperty.setDisplayOrder( Float.parseFloat( propertyOrder ) );
		reportProperty.setThingType( ThingTypeService.getInstance().get( propertyTypeId ));
		reportProperty.setThingTypeField( ThingTypeFieldService.getInstance().getThingTypeFieldByName( propertyName ).get( 0 ));

		reportProperty.setReportDefinition( reportDefinition );
		return reportProperty;
	}

	private ReportRule createReportRule( String propertyName, String operator, String value, String color, String style, Long TID,
			ReportDefinition reportDefinition )
	{
		ReportRule reportRule = new ReportRule();
		reportRule.setPropertyName( propertyName );
		reportRule.setOperator( operator );
		reportRule.setValue( value );
		reportRule.setColor( color );
		reportRule.setStyle( style );
		reportRule.setReportDefinition( reportDefinition );
		reportRule.setThingType( ThingTypeService.getInstance().get( TID ));
		reportRule.setThingTypeField( ThingTypeFieldService.getInstance().getThingTypeFieldByName( propertyName ).get( 0 ));

		return reportRule;
	}

	private void createReportDefinitionData( Group group, GroupType gt )
	{
		User rootUser = UserService.getInstance().get( 1L );
		// Reporte3

		ReportDefinition reportDefinition5 = new ReportDefinition();
		reportDefinition5.setName( "Clothing by Brand" );
		reportDefinition5.setCreatedByUser( rootUser );
		reportDefinition5.setGroup( group );
		reportDefinition5.setReportType( "table" );
		reportDefinition5.setDefaultTypeIcon( "pin" );
		reportDefinition5.setPinLabels( true );
		reportDefinition5.setZoneLabels( true );
		reportDefinition5.setTrails( false );
		reportDefinition5.setClustering( true );
		reportDefinition5.setPlayback( true );
		reportDefinition5.setNupYup( true );
		reportDefinition5.setDefaultList( false );
		reportDefinition5.setGroupTypeFloor( gt );
		reportDefinition5.setDefaultColorIcon( "4DD000" );
		reportDefinition5.setRunOnLoad(true);
		reportDefinition5.setIsMobile(Boolean.FALSE);
		reportDefinition5.setIsMobileDataEntry(Boolean.FALSE);
		reportDefinition5 = ReportDefinitionService.getInstance().insert( reportDefinition5 );

		String[] labels5 = { "Brand", "Category", "RFID Tag #", "Department", "Last Detect Time", "Logical Reader", "Color", "Size", "Price",
				"Name", "Type" };
		String[] propertyNames5 = { "brand", "category", "serial", "group.name", "lastDetectTime", "logicalReader", "color", "size",
				"price", "name", "thingType.name" };
		String[] propertyOrders5 = { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11" };
		Long[] propertyTypeIds5 = { 1L, 1L, 3L, 3L, 3L, 3L, 1L, 1L, 1L, 3L, 3L };
		for( int it = 0; it < Array.getLength( labels5 ); it++ )
		{
			ReportProperty reportProperty = createReportProperty( labels5[it], propertyNames5[it], propertyOrders5[it],
					propertyTypeIds5[it], reportDefinition5 );
			ReportPropertyService.getInstance().insert( reportProperty );
		}

		String[] labelsFilter5 = { "Brand" };

		String[] propertyNamesFilter5 = { "brand" };
		String[] propertyOrdersFilter5 = { "2" };
		String[] operatorFilter5 = { "=" };
		String[] value5 = { "Calvin Klein" };
		Boolean[] isEditable5 = { true };
		Long[] thingTypeIdReport5 = { 1L };
		for( int it = 0; it < Array.getLength( labelsFilter5 ); it++ )
		{
			ReportFilter reportFilter = createReportFilter( labelsFilter5[it], propertyNamesFilter5[it], propertyOrdersFilter5[it],
					operatorFilter5[it], value5[it], isEditable5[it], thingTypeIdReport5[it], reportDefinition5 );
			ReportFilterService.getInstance().insert( reportFilter );
		}

		// Reporte3

		ReportDefinition reportDefinition6 = new ReportDefinition();
		reportDefinition6.setName( "Clothing by Brand" );
		reportDefinition6.setGroup( group );
		reportDefinition6.setReportType( "map" );
		reportDefinition6.setDefaultTypeIcon( "pin" );
		reportDefinition6.setPinLabel( "3" );
		reportDefinition6.setLocalMapId(1L);
		reportDefinition6.setDefaultZoom( 20L );
		reportDefinition6.setCenterLat( "34.048139" );
		reportDefinition6.setCenterLon( "-118.443818" );
		reportDefinition6.setPinLabels( false );
		reportDefinition6.setZoneLabels( false );
		reportDefinition6.setTrails( false );
		reportDefinition6.setClustering( false );
		reportDefinition6.setPlayback( true );
		reportDefinition6.setNupYup( true );
		reportDefinition6.setDefaultList( false );
		reportDefinition6.setDefaultTypeIcon( "Pin" );
		reportDefinition6.setGroupTypeFloor( gt );
		reportDefinition6.setDefaultColorIcon( "4DD000" );
		reportDefinition6.setCreatedByUser( rootUser );
		reportDefinition6.setRunOnLoad(true);
		reportDefinition6 = ReportDefinitionService.getInstance().insert( reportDefinition6 );

		String[] labels6 = { "Brand", "Category", "RFID Tag #", "Department", "Last Detect Time", "Logical Reader", "Color", "Size", "Price",
				"Name", "Type" };
		String[] propertyNames6 = { "brand", "category", "serial", "group.name", "lastDetectTime", "logicalReader", "color", "size",
				"price", "name", "thingType.name" };
		String[] propertyOrders6 = { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11" };
		Long[] propertyTypeIds6 = { 1L, 1L, 3L, 3L, 3L, 3L, 1L, 1L, 1L, 3L, 3L };
		for( int it = 0; it < Array.getLength( labels6 ); it++ )
		{
			ReportProperty reportProperty = createReportProperty( labels6[it], propertyNames6[it], propertyOrders6[it],
					propertyTypeIds6[it], reportDefinition6 );
			ReportPropertyService.getInstance().insert( reportProperty );
		}

		String[] labelsFilter6 = { "Brand" };

		String[] propertyNamesFilter6 = { "brand" };
		String[] propertyOrdersFilter6 = { "1" };
		String[] operatorFilter6 = { "=" };
		String[] value6 = { "Calvin Klein" };
		Boolean[] isEditable6 = { true };
		Long[] thingTypeIdReport6 = { 1L };

		for( int it = 0; it < Array.getLength( labelsFilter6 ); it++ )
		{
			ReportFilter reportFilter = createReportFilter( labelsFilter6[it], propertyNamesFilter6[it], propertyOrdersFilter6[it],
					operatorFilter6[it], value6[it], isEditable6[it], thingTypeIdReport6[it], reportDefinition6 );

			ReportFilterService.getInstance().insert( reportFilter );
		}

	}

	private void createReportDefinitionData2( Group group, GroupType gt, User createdByUser )
	{
		// Report 1
		ReportDefinition reportDefinition = new ReportDefinition();
		reportDefinition.setName( "All RFID Tags" );
		reportDefinition.setCreatedByUser( createdByUser );
		reportDefinition.setGroup( group );
		reportDefinition.setReportType( "map" );
		reportDefinition.setDefaultTypeIcon( "pin" );
		reportDefinition.setPinLabel( "1" );
		reportDefinition.setLocalMapId(1L);
		reportDefinition.setDefaultZoom( 20L );
		reportDefinition.setCenterLat( "34.048139" );
		reportDefinition.setCenterLon( "-118.443818" );
		reportDefinition.setPinLabels( false );
		reportDefinition.setZoneLabels( false );
		reportDefinition.setTrails( false );
		reportDefinition.setClustering( false );
		reportDefinition.setPlayback( true );
		reportDefinition.setNupYup( true );
		reportDefinition.setDefaultTypeIcon( "Pin" );
		reportDefinition.setDefaultColorIcon( "009F6B" );
		reportDefinition.setRunOnLoad(true);
		reportDefinition.setDefaultList( false );
		reportDefinition.setGroupTypeFloor( gt );
		reportDefinition.setIsMobile(Boolean.FALSE);
		reportDefinition.setIsMobileDataEntry(Boolean.FALSE);
		reportDefinition = ReportDefinitionService.getInstance().insert( reportDefinition );

		String[] labels = { "RFID Tag #", "Category", "Logical Reader", "Zone Dwell Time (ms)", "Last Detect Time", "Name", "Location" };
		String[] propertyNames = { "serial", "category", "logicalReader", "dwellTime", "lastDetectTime", "name", "location" };
		String[] propertyOrders = { "1", "2", "3", "4", "5", "6", "7" };
		Long[] propertyTypeIds = { 3L, 1L, 3L, 3L, 3L, 3L, 3L, 1L };

		for( int it = 0; it < Array.getLength( labels ); it++ )
		{
			ReportProperty reportProperty = createReportProperty( labels[it], propertyNames[it], propertyOrders[it], propertyTypeIds[it],
					reportDefinition );
			ReportPropertyService.getInstance().insert( reportProperty );
		}

		String[] labelsFilter = { "Thing Type", "group" };
		String[] propertyNamesFilter = { "thingType.id", "group.id" };

		String[] propertyOrdersFilter = { "1", "2" };
		String[] operatorFilter = { "=", "=" };

		String[] value = { "1", "2" };
		Boolean[] isEditable = { true, false };


		for( int it = 0; it < Array.getLength( labelsFilter ); it++ )
		{
			ReportFilter reportFilter = createReportFilter( labelsFilter[it], propertyNamesFilter[it], propertyOrdersFilter[it],
					operatorFilter[it], value[it], isEditable[it], reportDefinition );
			ReportFilterService.getInstance().insert( reportFilter );
		}
		/*
		 * for( int it = 0; it < Array.getLength( propertyNamesRules ); it++ ) {
		 * ReportRule reportRule = createReportRule( propertyNamesRules[it],
		 * operatorRules[it], valueRules[it], color[it], "",reportDefinition );
		 * 
		 * ReportRuleService.getInstance().insert(reportRule); }
		 */

		// Reporte 1
		ReportDefinition reportDefinition4 = new ReportDefinition();
		reportDefinition4.setName( "All RFID Tags" );
		reportDefinition4.setCreatedByUser( createdByUser );
		reportDefinition4.setGroup( group );
		reportDefinition4.setReportType( "table" );
		reportDefinition4.setDefaultTypeIcon( "pin" );
		reportDefinition4.setPinLabels( false );
		reportDefinition4.setZoneLabels( false );
		reportDefinition4.setTrails( false );
		reportDefinition4.setClustering( false );
		reportDefinition4.setPlayback( true );
		reportDefinition4.setNupYup( false );
		reportDefinition4.setDefaultList( false );
		reportDefinition4.setGroupTypeFloor( gt );
		reportDefinition4.setDefaultColorIcon( "009F6B" );
		reportDefinition4.setRunOnLoad(true);
		reportDefinition4 = ReportDefinitionService.getInstance().insert( reportDefinition4 );

		String[] labels4 = { "RFID Tag #", "Category", "Logical Reader", "Zone Dwell Time (ms)", "Last Detect Time", "Location" };
		String[] propertyNames4 = { "serial", "category", "logicalReader", "dwellTime", "lastDetectTime", "location" };
		String[] propertyOrders4 = { "1", "2", "3", "4", "5", "6", "7" };
		Long[] propertyTypeIds4 = { 3L, 1L, 3L, 3L, 3L, 3L, 3L };
		for( int it = 0; it < Array.getLength( labels4 ); it++ )
		{
			ReportProperty reportProperty = createReportProperty( labels4[it], propertyNames4[it], propertyOrders4[it],
					propertyTypeIds4[it], reportDefinition4 );
			ReportPropertyService.getInstance().insert( reportProperty );
		}

		String[] labelsFilter4 = { "Thing Type" };
		String[] propertyNamesFilter4 = { "thingType.id" };

		String[] propertyOrdersFilter4 = { "1" };
		String[] operatorFilter4 = { "=" };

		String[] value4 = { "1" };
		Boolean[] isEditable4 = { false };

		for( int it = 0; it < Array.getLength( labelsFilter4 ); it++ )
		{
			ReportFilter reportFilter = createReportFilter( labelsFilter4[it], propertyNamesFilter4[it], propertyOrdersFilter4[it],
					operatorFilter4[it], value4[it], isEditable4[it], reportDefinition4 );
			ReportFilterService.getInstance().insert( reportFilter );
		}

		ReportDefinition reportDefinition2 = new ReportDefinition();
		reportDefinition2.setName( "Clothing by Zone" );
		reportDefinition2.setCreatedByUser( createdByUser );
		reportDefinition2.setGroup( group );
		reportDefinition2.setReportType( "map" );
		reportDefinition2.setDefaultTypeIcon( "pin" );
		reportDefinition2.setDefaultZoom( 20L );
		reportDefinition2.setLocalMapId(1L);
		reportDefinition2.setCenterLat( "34.048139" );
		reportDefinition2.setCenterLon( "-118.443818" );
		reportDefinition2.setPinLabels( false );
		reportDefinition2.setZoneLabels( false );
		reportDefinition2.setTrails( false );
		reportDefinition2.setClustering( false );
		reportDefinition2.setPlayback( true );
		reportDefinition2.setNupYup( true );
		reportDefinition2.setDefaultList( false );
		reportDefinition2.setDefaultTypeIcon( "Pin" );
		reportDefinition2.setGroupTypeFloor( gt );
		reportDefinition2.setDefaultColorIcon( "009F6B" );
		reportDefinition2.setRunOnLoad(true);
		reportDefinition2 = ReportDefinitionService.getInstance().insert( reportDefinition2 );
		reportDefinition2.setPinLabel( "3" );
		String[] labels2 = { "Logical Reader", "Category", "RFID Tag", "Last Detect Time", "Brand", "Color", "Size", "Price", "Name" };
		String[] propertyNames2 = { "logicalReader", "category", "serial", "lastDetectTime", "brand", "color", "size", "price", "name" };
		String[] propertyOrders2 = { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11" };
		Long[] propertyTypeIds2 = { 3L, 1L, 3L, 3L, 1L, 1L, 1L, 1L, 3L };
		for( int it = 0; it < Array.getLength( labels2 ); it++ )
		{
			ReportProperty reportProperty = createReportProperty( labels2[it], propertyNames2[it], propertyOrders2[it],
					propertyTypeIds2[it], reportDefinition2 );
			ReportPropertyService.getInstance().insert( reportProperty );
		}

		String[] labelsFilter2 = { "Group", "Thing Type" };
		String[] propertyNamesFilter2 = { "group.id", "thingType.id" };
		String[] propertyOrdersFilter2 = { "1", "2" };
		String[] operatorFilter2 = { "=", "=" };
		String[] value2 = { "", "2" };
		Boolean[] isEditable2 = { true, true };

		for( int it = 0; it < Array.getLength( labelsFilter2 ); it++ )
		{
			ReportFilter reportFilter = createReportFilter( labelsFilter2[it], propertyNamesFilter2[it], propertyOrdersFilter2[it],
					operatorFilter2[it], value2[it], isEditable2[it], reportDefinition2 );
			ReportFilterService.getInstance().insert( reportFilter );
		}
		// Reporte2

		ReportDefinition reportDefinition3 = new ReportDefinition();
		reportDefinition3.setName( "Clothing by Zone" );
		reportDefinition3.setCreatedByUser( createdByUser );
		reportDefinition3.setGroup( group );
		reportDefinition3.setReportType( "table" );
		reportDefinition3.setDefaultTypeIcon( "pin" );

		reportDefinition3.setPinLabels( false );
		reportDefinition3.setZoneLabels( false );
		reportDefinition3.setTrails( false );
		reportDefinition3.setClustering( false );
		reportDefinition3.setPlayback( true );
		reportDefinition3.setNupYup( false );
		reportDefinition3.setDefaultList( false );
		reportDefinition3.setGroupTypeFloor( gt );
		reportDefinition3.setDefaultColorIcon( "009F6B" );
		reportDefinition3.setRunOnLoad(true);
		reportDefinition3 = ReportDefinitionService.getInstance().insert( reportDefinition3 );
		reportDefinition3.setDefaultColorIcon( "009F6B" );
		String[] labels3 = { "Logical Reader", "Category", "RFID Tag", "Last Detect Time", "Brand", "Color", "Size", "Price", "Name" };
		String[] propertyNames3 = { "logicalReader", "category", "serial", "lastDetectTime", "brand", "color", "size", "price", "name" };
		String[] propertyOrders3 = { "1", "2", "3", "4", "5", "6", "7", "8", "9" };
		Long[] propertyTypeIds3 = { 3L, 1L, 3L, 3L, 1L, 1L, 1L, 1L, 3L };
		for( int it = 0; it < Array.getLength( labels3 ); it++ )
		{
			ReportProperty reportProperty = createReportProperty( labels3[it], propertyNames3[it], propertyOrders3[it],
					propertyTypeIds3[it], reportDefinition3 );
			ReportPropertyService.getInstance().insert( reportProperty );
		}

		String[] labelsFilter3 = { "Group", "Thing Type" };
		String[] propertyNamesFilter3 = { "group.id", "thingType.id" };
		String[] propertyOrdersFilter3 = { "1", "2" };
		String[] operatorFilter3 = { "<", "=" };
		String[] value3 = { "2", "2" };
		Boolean[] isEditable3 = { true, false };

		for( int it = 0; it < Array.getLength( labelsFilter3 ); it++ )
		{
			ReportFilter reportFilter = createReportFilter( labelsFilter3[it], propertyNamesFilter3[it], propertyOrdersFilter3[it],
					operatorFilter3[it], value3[it], isEditable3[it], reportDefinition3 );
			ReportFilterService.getInstance().insert( reportFilter );
		}

		// last report

		ReportDefinition reportDefinition7 = new ReportDefinition();
		reportDefinition7.setName( "Jackets & Pants Dwell" );
		reportDefinition7.setCreatedByUser( createdByUser );
		reportDefinition7.setGroup( group );
		reportDefinition7.setReportType( "map" );
		reportDefinition7.setDefaultTypeIcon( "pin" );
		reportDefinition7.setDefaultZoom( 20L );
		reportDefinition7.setLocalMapId(1L);
		reportDefinition7.setCenterLat( "34.048139" );
		reportDefinition7.setCenterLon( "-118.443818" );
		reportDefinition7.setPinLabels( false );
		reportDefinition7.setZoneLabels( false );
		reportDefinition7.setTrails( false );
		reportDefinition7.setClustering( false );
		reportDefinition7.setPlayback( true );
		reportDefinition7.setNupYup( true );
		reportDefinition7.setDefaultList( false );
		reportDefinition7.setDefaultTypeIcon( "Pin" );
		reportDefinition7.setGroupTypeFloor( gt );
		reportDefinition7.setDefaultColorIcon( "009F6B" );
		reportDefinition7.setRunOnLoad(true);
		reportDefinition7 = ReportDefinitionService.getInstance().insert( reportDefinition7 );
		
		String[] labels7 = { "RFID Tag #", "Category", "Logical Reader", "Zone Dwell Time (ms)", "Last Detect Time", "Name", "Location" };
		String[] propertyNames7 = { "serial", "category", "logicalReader", "dwellTime", "lastDetectTime", "name", "location" };
		String[] propertyOrders7 = { "1", "2", "3", "4", "5", "6", "7" };
		Long[] propertyTypeIds7 = { 3L, 1L, 3L, 3L, 3L, 3L, 3L };

		for( int it = 0; it < Array.getLength( labels7 ); it++ )
		{
			ReportProperty reportProperty = createReportProperty( labels7[it], propertyNames7[it], propertyOrders7[it],
					propertyTypeIds7[it], reportDefinition7 );
			ReportPropertyService.getInstance().insert( reportProperty );
		}
		reportDefinition7.setPinLabel( "1" );
		String[] labelsFilter7 = { "Thing Type", "group" };
		String[] propertyNamesFilter7 = { "thingType.id", "group.id" };

		String[] propertyOrdersFilter7 = { "1", "2" };
		String[] operatorFilter7 = { "=", "=" };

		String[] value7 = { "2", "2" };
		Boolean[] isEditable7 = { true, false };

		String[] propertyNamesRules7 = { "dwellTime" };
		String[] operatorRules7 = { ">" };
		String[] valueRules7 = { "5000" };
		String[] color7 = { "FF0000" };
		Long[] ids = { 3L };

		for( int it = 0; it < Array.getLength( labelsFilter7 ); it++ )
		{
			ReportFilter reportFilter = createReportFilter( labelsFilter7[it], propertyNamesFilter7[it], propertyOrdersFilter7[it],
					operatorFilter7[it], value7[it], isEditable7[it], reportDefinition7 );
			ReportFilterService.getInstance().insert( reportFilter );
		}
		for( int it = 0; it < Array.getLength( propertyNamesRules7 ); it++ )
		{
			ReportRule reportRule = createReportRule( propertyNamesRules7[it], operatorRules7[it], valueRules7[it], color7[it], "",
					ids[it], reportDefinition7 );

			ReportRuleService.getInstance().insert( reportRule );
		}
		
		
		// last report

			/*	ReportDefinition reportDefinition8 = new ReportDefinition();
				reportDefinition8.setName( "Zone Dwell-Time Report" );
				reportDefinition8.setCreatedByUser( createdByUser );
				reportDefinition8.setGroup( group );
				reportDefinition8.setReportType( "timeSeries" );
				reportDefinition8.setChartType("line");
				reportDefinition8.setLocalMapId(1L);
				//reportDefinition8.setDefaultZoom( 20L );
				//reportDefinition8.setCenterLat( "34.048139" );
				//reportDefinition8.setCenterLon( "-118.443818" );
				//reportDefinition8.setPinLabels( false );
				//reportDefinition8.setZoneLabels( false );
				//reportDefinition8.setTrails( false );
				//reportDefinition8.setClustering( false );
				//reportDefinition8.setPlayback( true );
				//reportDefinition8.setNupYup( true );
				//reportDefinition8.setDefaultList( false );
				//reportDefinition8.setDefaultTypeIcon( "Pin" );
				//reportDefinition8.setGroupTypeFloor( gt );
				//reportDefinition8.setDefaultColorIcon( "009F6B" );
				reportDefinition8 = ReportDefinitionService.getInstance().insert( reportDefinition8 );

				//String[] labels7 = { "RFID Tag #", "Category", "Logical Reader", "Zone Dwell Time (ms)", "Last Detect Time", "Name", "Location" };
				//String[] propertyNames7 = { "serial", "category", "logicalReader", "dwellTime", "lastDetectTime", "name", "location" };
				//String[] propertyOrders7 = { "1", "2", "3", "4", "5", "6", "7" };
				//Long[] propertyTypeIds7 = { 3L, 1L, 3L, 3L, 3L, 3L, 3L };

				for( int it = 0; it < Array.getLength( labels7 ); it++ )
				{
					ReportProperty reportProperty = createReportProperty( labels7[it], propertyNames7[it], propertyOrders7[it],
							propertyTypeIds7[it], reportDefinition7 );
					ReportPropertyService.getInstance().insert( reportProperty );
				}
				reportDefinition7.setPinLabel( "1" );
				//String[] labelsFilter7 = { "Thing Type", "group" };
				//String[] propertyNamesFilter7 = { "thingType.id", "group.id" };

				//String[] propertyOrdersFilter7 = { "1", "2" };
				//String[] operatorFilter7 = { "=", "=" };

				//String[] value7 = { "2", "2" };
				//Boolean[] isEditable7 = { true, false };

				//String[] propertyNamesRules7 = { "dwellTime" };
				//String[] operatorRules7 = { ">" };
				//String[] valueRules7 = { "5000" };
				//String[] color7 = { "FF0000" };
				//Long[] ids = { 3L };

				/*for( int it = 0; it < Array.getLength( labelsFilter7 ); it++ )
				{
					ReportFilter reportFilter = createReportFilter( labelsFilter7[it], propertyNamesFilter7[it], propertyOrdersFilter7[it],
							operatorFilter7[it], value7[it], isEditable7[it], reportDefinition7 );
					ReportFilterService.getInstance().insert( reportFilter );
				}*/
		

	}

}
