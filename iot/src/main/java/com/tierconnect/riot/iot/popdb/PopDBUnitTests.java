package com.tierconnect.riot.iot.popdb;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.tierconnect.riot.iot.entities.*;
import org.apache.log4j.Logger;
import org.hibernate.Transaction;

import com.tierconnect.riot.appcore.dao.CassandraUtils;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.GroupType;
import com.tierconnect.riot.appcore.entities.Resource;
import com.tierconnect.riot.appcore.entities.Role;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.entities.UserRole;
import com.tierconnect.riot.appcore.popdb.PopDBRequired;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.GroupTypeService;
import com.tierconnect.riot.appcore.services.ResourceService;
import com.tierconnect.riot.appcore.services.RoleResourceService;
import com.tierconnect.riot.appcore.services.RoleService;
import com.tierconnect.riot.appcore.services.UserRoleService;
import com.tierconnect.riot.appcore.services.UserService;
import com.tierconnect.riot.appcore.utils.Configuration;
import com.tierconnect.riot.iot.services.EdgeboxService;
import com.tierconnect.riot.iot.services.LogicalReaderService;
import com.tierconnect.riot.iot.services.ReportDefinitionService;
import com.tierconnect.riot.iot.services.ReportFilterService;
import com.tierconnect.riot.iot.services.ReportPropertyService;
import com.tierconnect.riot.iot.services.ReportRuleService;
import com.tierconnect.riot.iot.services.ShiftZoneService;
import com.tierconnect.riot.iot.services.ThingTypeService;

/**
 * popdb class for bridge unit tests
 * 
 * @author tcrown
 * 
 */
public class PopDBUnitTests
{
	private static final Logger logger = Logger.getLogger( PopDBUnitTests.class );
	/*
	public static void main( String args[] ) throws Exception
	{
		PopDBRequired.initJDBCDrivers();
        System.getProperties().put("hibernate.hbm2ddl.auto", "update");
		CassandraUtils.init( Configuration.getProperty( "cassandra.host" ), Configuration.getProperty( "cassandra.keyspace" ) );
		PopDBUnitTests popdb = new PopDBUnitTests();
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

		// group mojix
		// Group mojix = PopDBUtils.popGroup( "Mojix Retail", "Mojix Retail",
		// rootGroup, tenantGroupType, "" );
		Group mojix = GroupService.getInstance().get( 2L );
		mojix.setName( "Mojix Retail" );
		mojix.setCode( "mojix" );
		// facility, test wing and zone
		GroupType storeGroupType = PopDBUtils.popGroupType( "Store", mojix, tenantGroupType, "" );

		// GroupType areaGroupType = PopDBUtils.popGroupType( "Area", rootGroup,
		// facilityGroupType, "Area" );
		GroupType departamentGroupType = PopDBUtils.popGroupType( "Department", mojix, storeGroupType, "" );

		// Groups Mojix
		// facility santa monica
		Group santaMonica = PopDBUtils.popGroup( "Santa Monica", "SM", mojix, storeGroupType, "" );

		Edgebox eb1 = EdgeboxService.getInstance().get( (long) 1 );
		eb1.setGroup( santaMonica );

		Edgebox eb2 = EdgeboxService.getInstance().get( (long) 2 );
		eb2.setGroup( santaMonica );

		Edgebox eb3 = EdgeboxService.getInstance().get( (long) 3 );
		eb3.setGroup( santaMonica );

		ThingType ttrfid = ThingTypeService.getInstance().get( (long) 1 );
		PopDBIOTUtils.popThingTypeField( ttrfid, "zoneViolationStatus", "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				ThingTypeField.Type.TYPE_TEXT.value, true ,null, null);
		PopDBIOTUtils.popThingTypeField( ttrfid, "zoneViolationFlag", "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				ThingTypeField.Type.TYPE_TEXT.value, true,null, null);

		
		// Edgebox eb4 = EdgeboxService.getInstance().get( (long) 3 );
		// eb4.setGroup( santaMonica );
		
		Role tenantRole = RoleService.getInstance().getTenantAdminRole();
		tenantRole.setGroup( mojix );
		RoleService.getInstance().update( tenantRole );

		// MINS: -118.444142 34.047880
		LocalMap localmap = PopDBIOTUtils.populateFacilityMap( "Map Store Santa Monica", "images/mojixmap.png", santaMonica, -118.444142,
				175.5, 34.047880, 174, -118.443969, 34.048092, 20.0, "ft" );

		createReportDefinitionData2( santaMonica, storeGroupType, rootUser );

		// new groups
		Group frontend = PopDBUtils.popGroup( "Front-end", "FE", santaMonica, departamentGroupType, "" );
		Group stocking = PopDBUtils.popGroup( "Stocking", "S", santaMonica, departamentGroupType, "" );
		Group clothing = PopDBUtils.popGroup( "Clothing", "C", santaMonica, departamentGroupType, "" );

		// Zones
		Zone z1 = PopDBIOTUtils.popZone( santaMonica, localmap, "Entrance", "#FF0000" );
		PopDBIOTUtils.popZonePoint( z1, 0, -118.443980544741, 34.048119816839275 );
		PopDBIOTUtils.popZonePoint( z1, 1, -118.443972498113, 34.04810259330263 );
		PopDBIOTUtils.popZonePoint( z1, 2, -118.443932724570, 34.048114422716736 );
		PopDBIOTUtils.popZonePoint( z1, 3, -118.443940646881, 34.04813120659546 );

		Zone z2 = PopDBIOTUtils.popZone( santaMonica, localmap, "PoS", "#FF0000" );
		PopDBIOTUtils.popZonePoint( z2, 0, -118.44393829994901, 34.04814426314336 );
		PopDBIOTUtils.popZonePoint( z2, 1, -118.44393071291418, 34.04812720146832 );
		PopDBIOTUtils.popZonePoint( z2, 2, -118.4439104720305, 34.04813315118781 );
		PopDBIOTUtils.popZonePoint( z2, 3, -118.44391784810527, 34.04815037471825 );

		Zone z3 = PopDBIOTUtils.popZone( santaMonica, localmap, "Stockroom", "#FF0000" );
		PopDBIOTUtils.popZonePoint( z3, 0, -118.44396414381346, 34.04826240930372 );
		PopDBIOTUtils.popZonePoint( z3, 1, -118.4439424738065, 34.0482158438294 );
		PopDBIOTUtils.popZonePoint( z3, 2, -118.44388656651233, 34.04823362294016 );
		PopDBIOTUtils.popZonePoint( z3, 3, -118.44390781742415, 34.0482776882189 );
		PopDBIOTUtils.popZonePoint( z3, 4, -118.44392070445531, 34.048283253696866 );
		PopDBIOTUtils.popZonePoint( z3, 5, -118.44395968030507, 34.04827165561227 );

		Zone z4 = PopDBIOTUtils.popZone( santaMonica, localmap, "Salesfloor", "#FF0000" );
		PopDBIOTUtils.popZonePoint( z4, 0, -118.4438802149873, 34.0482288161938 );
		PopDBIOTUtils.popZonePoint( z4, 1, -118.4438541638471, 34.048170531850744 );
		PopDBIOTUtils.popZonePoint( z4, 2, -118.44374701635302, 34.04820320715321 );
		PopDBIOTUtils.popZonePoint( z4, 3, -118.44377259594499, 34.04826137303725 );

		Shift shift = PopDBIOTUtils.popShift( santaMonica, "DAY-M-W", 800L, 1700L, "23456" );
		
		ShiftZone shiftZone = new ShiftZone();
		shiftZone.setGroup( santaMonica );
		shiftZone.setShift( shift );
		shiftZone.setZone( z2 );
		ShiftZoneService.getInstance().insert( shiftZone );
		
		//TODO: set shift for tag #1 for ShiftZoneAction unit test ??
		
		LogicalReader lr = new LogicalReader();
		lr.setName( "For_Unit_Test" );
		lr.setCode( "LR100" );
		lr.setZoneIn( z1 );
		lr.setZoneOut( z2 );
		lr.setGroup( santaMonica );
		LogicalReaderService.getInstance().insert( lr );

		// pants thingtype
		ThingType pantThingType = PopDBMojixUtils.popThingTypeClothingItem( santaMonica, "Pants" );
		ThingType jacketThingType = PopDBMojixUtils.popThingTypeClothingItem( santaMonica, "Jackets" );
		// Moved to RequiredIOT
		// ThingType rfid = PopDBIOTUtils.popThingTypeRFID( santaMonica,
		// "default_rfid_thingtype" );
		ThingType rfid = ThingTypeService.getInstance().get( 1L );
		rfid.setGroup( santaMonica );
		rfid.setThingTypeCode( "default_rfid_thingtype" );

		PopDBIOTUtils.popThingTypeMap( jacketThingType, rfid );
		PopDBIOTUtils.popThingTypeMap( pantThingType, rfid );

		for( int i = 1; i <= 5; i++ )
		{
			Thing th = PopDBMojixUtils.instantiateClothingItem( jacketThingType, i, "J0000" + i, "Jacket" + i, santaMonica, rootUser );
			String sn = String.format( "%021d", 473 + i );
			PopDBMojixUtils.instantiateRFIDTag( rfid, th, 473 + i, sn, santaMonica, rootUser );
		}

		for( int i = 1; i <= 5; i++ )
		{
			Thing th = PopDBMojixUtils.instantiateClothingItem( pantThingType, i, "P0000" + i, "Pants" + i, santaMonica, rootUser );
			String sn = String.format( "%021d", 478 + i );
			PopDBMojixUtils.instantiateRFIDTag( rfid, th, 478 + i, sn, santaMonica, rootUser );
		}

		// Users
		rootUser.setEmail( "root@company.com" );
		// User admin = PopDBUtils.popUser( "Admin", mojix, tenantRole );

		UserService.getInstance().update( rootUser );

		// TODO watch out for empty resources
		Role companyUser = PopDBUtils.popRole( "Store User", "CU", "Store User", new ArrayList<Resource>(), mojix, storeGroupType );
		Role companyadmin = PopDBUtils.popRole( "Store Administrator", "CU", "Store Administrator", new ArrayList<Resource>(), mojix,
				storeGroupType );

		//
		Role storeManager = PopDBUtils.popRole( "Store Manager", "sm", "Role store manager", null, mojix, storeGroupType );
		Role storeEmployee = PopDBUtils.popRole( "Store Employee", "sm", "Role store employee", null, mojix, storeGroupType );
		Role pantManager = PopDBUtils.popRole( "Pants Manager", "sm", "Pants manager", null, mojix, storeGroupType );
		Role reportManager = PopDBUtils.popRole( "Report Manager", "sm", "Report manager", null, mojix, null );
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
			}

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
		User samUser = PopDBUtils.popUser( "samuel", "samuel", santaMonica, storeManager );
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
		User paulUser = PopDBUtils.popUser( "paul", "paul", clothing, pantManager );
		paulUser.setFirstName( "Paul" );
		paulUser.setLastName( "Barriga" );
		paulUser.setEmail( "paul.barriga@mojix.com " );
		createReportDefinitionData( santaMonica, departamentGroupType );
		Set<UserRole> copia2 = new HashSet<UserRole>();

		UserRole paul2 = new UserRole();
		paul2.setRole( reportManager );
		paul2.setUser( paulUser );
		copia2.add( paul2 );
		paul2 = UserRoleService.getInstance().insert( paul2 );
		paulUser.setUserRoles( copia2 );

		User adminc = PopDBUtils.popUser( "adminc", "adminc", mojix, tenantRole );

		User adminp = PopDBUtils.popUser( "adminp", "adminp", santaMonica, companyadmin );
		User employee = PopDBUtils.popUser( "employee", santaMonica, storeEmployee );
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
		reportFilter.setThingTypeIdReport( ttId );
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
		reportProperty.setThingTypeIdReport( propertyTypeId );

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
		reportRule.setThingTypeIdReport( TID );

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
		reportDefinition5.setRunOnLoad( true );
		reportDefinition5 = ReportDefinitionService.getInstance().insert( reportDefinition5 );

		String[] labels5 = { "Brand", "Category", "RFID Tag #", "Department", "Last Detect Time", "Logical Reader", "Color", "Size",
				"Price", "Name", "Type" };
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
		reportDefinition6.setLocalMapId( 1L );
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
		reportDefinition6.setRunOnLoad( true );
		reportDefinition6 = ReportDefinitionService.getInstance().insert( reportDefinition6 );

		String[] labels6 = { "Brand", "Category", "RFID Tag #", "Department", "Last Detect Time", "Logical Reader", "Color", "Size",
				"Price", "Name", "Type" };
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
		reportDefinition.setLocalMapId( 1L );
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
		reportDefinition.setRunOnLoad( true );
		reportDefinition.setDefaultList( false );
		reportDefinition.setGroupTypeFloor( gt );
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

		String[] propertyNamesRules = { "" };
		String[] operatorRules = { "" };
		String[] valueRules = { "" };
		String[] color = { "009F6B" };
		String[] tytpeID = {};
		for( int it = 0; it < Array.getLength( labelsFilter ); it++ )
		{
			ReportFilter reportFilter = createReportFilter( labelsFilter[it], propertyNamesFilter[it], propertyOrdersFilter[it],
					operatorFilter[it], value[it], isEditable[it], reportDefinition );
			ReportFilterService.getInstance().insert( reportFilter );
		}

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
		reportDefinition4.setRunOnLoad( true );
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
		reportDefinition2.setLocalMapId( 1L );
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
		reportDefinition2.setRunOnLoad( true );
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
		reportDefinition3.setRunOnLoad( true );
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
		reportDefinition7.setLocalMapId( 1L );
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
		reportDefinition7.setRunOnLoad( true );
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

	}
	*/

}
