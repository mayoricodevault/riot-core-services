package com.tierconnect.riot.iot.popdb;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.tierconnect.riot.appcore.dao.CassandraUtils;
import com.tierconnect.riot.appcore.popdb.PopDBRequired;
import com.tierconnect.riot.appcore.utils.Configuration;
import com.tierconnect.riot.iot.entities.*;

import com.tierconnect.riot.iot.services.*;
import org.apache.log4j.Logger;
import org.hibernate.Transaction;

import com.tierconnect.riot.appcore.entities.Field;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.GroupType;
import com.tierconnect.riot.appcore.entities.Resource;
import com.tierconnect.riot.appcore.entities.Role;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.entities.UserRole;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.GroupTypeService;
import com.tierconnect.riot.appcore.services.ResourceService;
import com.tierconnect.riot.appcore.services.RoleResourceService;
import com.tierconnect.riot.appcore.services.RoleService;
import com.tierconnect.riot.appcore.services.UserRoleService;
import com.tierconnect.riot.appcore.services.UserService;

public class PopDBMojixAramco
{
	private static final Logger logger = Logger.getLogger( PopDBMojixRetail.class );

	public static void main( String args[] ) throws Exception
	{
		PopDBRequired.initJDBCDrivers();
        System.getProperties().put("hibernate.hbm2ddl.auto", "update");
		CassandraUtils.init(Configuration.getProperty("cassandra.host"), Configuration.getProperty("cassandra.keyspace"));
		PopDBMojixAramco popdb = new PopDBMojixAramco();
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
		
		// Groups
		Group aramco= GroupService.getInstance().get(2L);
		aramco.setName("Aramco");
		aramco.setCode("Aramco");
		Group barges= GroupService.getInstance().get(3L);
		barges.setName("Barges");
        barges.setCode("Barges");
		//Group vessel=PopDBUtils.popGroup("Cruise Ship", "cruise", chouest, storeGroupType, "");

        
		// Group Types
		GroupType companyGroupType = GroupTypeService.getInstance().getTenantGroupType();
		GroupType facilityGroupType= GroupTypeService.getInstance().get(3L);
		facilityGroupType.setGroup(aramco);
		facilityGroupType.setParent(companyGroupType);
		facilityGroupType.setName("Facility");
		GroupType areaGroupType = PopDBUtils.popGroupType( "Area", aramco, facilityGroupType, "" );

		// Group Class-1 Families
        Group class1Families = PopDBUtils.popGroup("Visitors","visitors", barges, areaGroupType, "");
        /*
		Resource re = new Resource();
		re.setAcceptedAttributes("riuda");
		re.setGroup(rootGroup);
		re.setModule("Aramco");
		re.setLabel("Aramco Safe-Zone Dashboard");
		re.setType(1);
		re.setFqname("");
		re.setName("aramco.safe.zone.dashboard");
		ResourceService.getInstance().insert( re );
		List<Resource> resources1 = ResourceService.getInstance().list();
		Role rootRole = RoleService.getInstance().getRootRole();
		for (Resource resource : resources1) {
			if ((resource.getName().toString().equals("aramco.safe.zone.dashboard"))) {
				RoleResourceService.getInstance().insert( rootRole, resource, "riuda" );
			}
		}
		*/
		// facility, test wing and zone
		
		PopDBIOTUtils.popShift( aramco, "DAY-M-W", 800L, 1700L, "23456", "DAY-M-W" );
        
		// Roles
		Role chouestAdminRole = RoleService.getInstance().getTenantAdminRole();
		chouestAdminRole.setName("Tenant administrator");
		// TODO: THIS IS FUCK UP !!!! must not hijack system records for tenant
		// usage !
		chouestAdminRole.setGroup( aramco );
		chouestAdminRole.setGroupTypeCeiling(null);
		chouestAdminRole.setDescription("");
		// TODO: THIS IS FUCK UP !!!!
		RoleService.getInstance().update( chouestAdminRole );

		// Role Chouest Reporting
		Role facilityManager = PopDBUtils.popRole("Facility Manager", "Facility Manager", null, aramco, null);
        // Role Family Reporting
        Role visitorRole = PopDBUtils.popRole("Visitor Role", "Visitor Role", null, aramco, null);
        
        
		// MINS: -118.444142 34.047880
		
		// pants thingtype
		/*ThingType peopleAboard = PopDBMojixUtils.popThingTypePeople(barges, "People Aboard");
		peopleAboard.setThingTypeCode("people");*/
		//Moved to RequiredIOT
		//ThingType rfid = PopDBIOTUtils.popThingTypeRFID( santaMonica, "default_rfid_thingtype" );
		ThingType rfid = ThingTypeService.getInstance().get( 1L );
		rfid.setGroup( barges );
		rfid.setName("RFID Tag");
		rfid.setThingTypeCode( "rfid.tag" );
		
		//PopDBIOTUtils.popThingTypeMap( peopleAboard, rfid );
		
		//localMap
		//PopDBIOTUtils.populateFacilityMap(name, imageName, group, lonmin, width, latmin, height, lonOrigin, latOrigin, declination, unit)
		LocalMap localmap=PopDBIOTUtils.populateFacilityMap("ARB-1", "images/arb-1.png", barges, 49.92091, 1356, 27.751262, 866, 49.92091, 27.751262, 0,"ft");
		LocalMap localmap1=PopDBIOTUtils.populateFacilityMap("ARB-2", "images/arb-2.png", barges, 48.97916, 1356, 28.175388, 866, 48.97916, 28.175388, 0,"ft");
		LocalMap localmap2=PopDBIOTUtils.populateFacilityMap("ARB-3", "images/arb-3.png", barges, 50.90922, 1356, 26.584111, 866, 50.90922, 26.584111, 0,"ft");
		
		//zones groups
		ZoneGroup zg0 = PopDBIOTUtils.popZoneGroup(localmap, "MainDeck");
		ZoneGroup zg1 = PopDBIOTUtils.popZoneGroup(localmap1, "MainDeck");
		ZoneGroup zg2 = PopDBIOTUtils.popZoneGroup(localmap2, "Main-Deck");
		ZoneGroup zg3 = PopDBIOTUtils.popZoneGroup(localmap2, "A-Deck");
		ZoneGroup zg4 = PopDBIOTUtils.popZoneGroup(localmap2, "InnerBottom-Deck");
		ZoneGroup zg5 = PopDBIOTUtils.popZoneGroup(localmap2, "Tween-Deck");
		ZoneGroup zg6 = PopDBIOTUtils.popZoneGroup(localmap2, "B-Deck");
		ZoneGroup zg7 = PopDBIOTUtils.popZoneGroup(localmap2, "C-Deck");
		ZoneGroup zg8 = PopDBIOTUtils.popZoneGroup(localmap2, "D-Deck");
		// Zones
				Zone z1 = PopDBIOTUtils.popZone( barges, localmap, "Bedrooms (MD)", "#FFFF00", "On-Site");
				PopDBIOTUtils.popZonePoint( z1, 0, 49.9210427818966, 27.7528611109452 );
				PopDBIOTUtils.popZonePoint( z1, 1, 49.9211903033924, 27.7528611109452 );
				PopDBIOTUtils.popZonePoint( z1, 2, 49.9211903033924, 27.7525477874466 );
				PopDBIOTUtils.popZonePoint( z1, 3, 49.9210427818966, 27.7525477874466 );
				z1.setZoneGroup(zg0);
				
				Zone z2 = PopDBIOTUtils.popZone( barges, localmap1, "Bedrooms", "#FFFF00", "On-Site" );
				PopDBIOTUtils.popZonePoint( z2, 0, 48.979310915967, 28.1769855201413 );
				PopDBIOTUtils.popZonePoint( z2, 1, 48.9794289331636, 28.1769784270941 );
				PopDBIOTUtils.popZonePoint( z2, 2, 48.9794316153727, 28.1766616038389 );
				PopDBIOTUtils.popZonePoint( z2, 3, 48.979318962594, 28.1766592394827);
				z2.setZoneGroup(zg1);
				
				Zone z3 = PopDBIOTUtils.popZone( barges, localmap2, "Bedrooms (MD)", "#FFFF00", "On-Site" );
				PopDBIOTUtils.popZonePoint( z3, 0, 50.9093911398035, 26.585499782583 );
				PopDBIOTUtils.popZonePoint( z3, 1, 50.909474288283, 26.585494985358 );
				PopDBIOTUtils.popZonePoint( z3, 2, 50.909476970492, 26.5854014394316 );
				PopDBIOTUtils.popZonePoint( z3, 3, 50.9093938220125, 26.5853942435879 );
				z3.setZoneGroup(zg2);
				
				Zone z4 = PopDBIOTUtils.popZone( barges, localmap2, "Controls (MD)", "#FFFF00", "On-Site" );
				PopDBIOTUtils.popZonePoint( z4, 0, 50.9101260650736, 26.5856077200909 );
				PopDBIOTUtils.popZonePoint( z4, 1, 50.9103513706308, 26.5856029228705 );
				PopDBIOTUtils.popZonePoint( z4, 2, 50.9103486884218, 26.585499782583 );
				PopDBIOTUtils.popZonePoint( z4, 3, 50.9101287472826, 26.5854925867455 );
				z4.setZoneGroup(zg2);
				
				Zone z5 = PopDBIOTUtils.popZone( barges, localmap2, "ComputerLab (MD)", "#FFFF00", "On-Site" );
				PopDBIOTUtils.popZonePoint( z5, 0, 50.9093911398035, 26.5857324477512);
				PopDBIOTUtils.popZonePoint( z5, 1, 50.909485017119, 26.5857348463587 );
				PopDBIOTUtils.popZonePoint( z5, 2, 50.909479652701, 26.585610118701 );
				PopDBIOTUtils.popZonePoint( z5, 3, 50.9093884575945, 26.5856053214808 );
				z5.setZoneGroup(zg2);
				
				Zone z6 = PopDBIOTUtils.popZone( barges, localmap2, "Bedrooms (A)", "#FFFF00", "On-Site" );
				PopDBIOTUtils.popZonePoint( z6, 0, 50.9094004057696, 26.5860312000604 );
				PopDBIOTUtils.popZonePoint( z6, 1, 50.9094956241896, 26.5860335986616);
				PopDBIOTUtils.popZonePoint( z6, 2, 50.9094916008761, 26.5859016755177 );
				PopDBIOTUtils.popZonePoint( z6, 3, 50.9093990646651, 26.5859016755177 );
				z6.setZoneGroup(zg3);
				
				Zone z7 = PopDBIOTUtils.popZone( barges, localmap2, "Stairs (A)", "#FFFF00", "On-Site" );
				PopDBIOTUtils.popZonePoint( z7, 0, 50.9101842243633, 26.5860148834411 );
				PopDBIOTUtils.popZonePoint( z7, 1, 50.9103692967854, 26.5860148834411);
				PopDBIOTUtils.popZonePoint( z7, 2, 50.9103585679493, 26.585870967253);
				PopDBIOTUtils.popZonePoint( z7, 3, 50.9101922709904, 26.5858685686484 );
				z7.setZoneGroup(zg3);
				
				Zone z8 = PopDBIOTUtils.popZone( barges, localmap2, "Bedrooms (B)", "#0000FF", "On-Site" );
				PopDBIOTUtils.popZonePoint( z8, 0, 50.9119116743734, 26.5843831270083 );
				PopDBIOTUtils.popZonePoint( z8, 1, 50.9120122572114, 26.5843831270083);
				PopDBIOTUtils.popZonePoint( z8, 2, 50.912018962734, 26.5842272155743);
				PopDBIOTUtils.popZonePoint( z8, 3, 50.9119156976869, 26.5842272155743);
				z8.setZoneGroup(zg6);
				
				Zone z9 = PopDBIOTUtils.popZone( barges, localmap2, "Stairs (B)", "#FC5308", "On-Site" );
				PopDBIOTUtils.popZonePoint( z9, 0, 50.9110224146805, 26.5843490424921 );
				PopDBIOTUtils.popZonePoint( z9, 1, 50.9112182159386, 26.5843514411286);
				PopDBIOTUtils.popZonePoint( z9, 2, 50.9112182159386, 26.584189533052);
				PopDBIOTUtils.popZonePoint( z9, 3, 50.9110130269489, 26.5841919316918);
				z9.setZoneGroup(zg6);
				
				Zone z10 = PopDBIOTUtils.popZone( barges, localmap2, "Bedrooms (C)", "#FFFF00", "On-Site" );
				PopDBIOTUtils.popZonePoint( z10, 0, 50.9119090874737, 26.58496950673 );
				PopDBIOTUtils.popZonePoint( z10, 1, 50.9120284457749, 26.5849671081065);
				PopDBIOTUtils.popZonePoint( z10, 2, 50.9120257635659,26.5847956063922);
				PopDBIOTUtils.popZonePoint( z10, 3, 50.9119050641602, 26.5847956063922);
				z10.setZoneGroup(zg7);
				
				Zone z11 = PopDBIOTUtils.popZone( barges, localmap2, "Stairs (C)", "#FFFF00", "On-Site" );
				PopDBIOTUtils.popZonePoint( z11, 0, 50.911021073576, 26.5848851365008 );
				PopDBIOTUtils.popZonePoint( z11, 1, 50.9112517435513, 26.5848827378755);
				PopDBIOTUtils.popZonePoint( z11, 2, 50.9112571079693,26.5847412188926);
				PopDBIOTUtils.popZonePoint( z11, 3, 50.9110371668301, 26.5847364216359);
				z11.setZoneGroup(zg7);
				
				Zone z12 = PopDBIOTUtils.popZone( barges, localmap2, "Bedrooms (D)", "#FFFF00", "On-Site");
				PopDBIOTUtils.popZonePoint( z12, 0, 50.9110438723526, 26.5854110338891 );
				PopDBIOTUtils.popZonePoint( z12, 1, 50.9111525018177, 26.5854098345819);
				PopDBIOTUtils.popZonePoint( z12, 2, 50.9111565251312,26.5852707148678);
				PopDBIOTUtils.popZonePoint( z12, 3, 50.9110425312481, 26.5852707148678);
				z12.setZoneGroup(zg8);
				
				Zone z13 = PopDBIOTUtils.popZone( barges, localmap2, "Muster", "#FF95C4", "On-Site" );
				PopDBIOTUtils.popZonePoint( z13, 0, 50.9099505527439, 26.5844488491676 );
				PopDBIOTUtils.popZonePoint( z13, 1, 50.9100310190143, 26.5844488491676);
				PopDBIOTUtils.popZonePoint( z13, 2, 50.9100283368053,26.5843325153384);
				PopDBIOTUtils.popZonePoint( z13, 3, 50.9099492116394, 26.5843313160199);
				z13.setZoneGroup(zg4);
				
				Zone z14 = PopDBIOTUtils.popZone( barges, localmap2, "Lifeboats", "#FFFF00", "On-Site" );
				PopDBIOTUtils.popZonePoint( z14, 0, 50.9099492116394, 26.5842425664203 );
				PopDBIOTUtils.popZonePoint( z14, 1, 50.9100685699406, 26.5842425664203);
				PopDBIOTUtils.popZonePoint( z14, 2, 50.9100672288361,26.584195792955);
				PopDBIOTUtils.popZonePoint( z14, 3, 50.9099518938484, 26.5841945936351);
				z14.setZoneGroup(zg4);
				
				Zone z15 = PopDBIOTUtils.popZone( barges, localmap2, "Kitchen", "#FFFF00", "On-Site" );
				PopDBIOTUtils.popZonePoint( z15, 0, 50.9094771428528, 26.5844572443878 );
				PopDBIOTUtils.popZonePoint( z15, 1, 50.9095817490044, 26.584458443705);
				PopDBIOTUtils.popZonePoint( z15, 2, 50.9095871134224,26.5843996771511);
				PopDBIOTUtils.popZonePoint( z15, 3, 50.9094717784348, 26.5843972785156);
				z15.setZoneGroup(zg4);
				
				Zone z16 = PopDBIOTUtils.popZone( barges, localmap2, "EngineRoom", "#FFFF00", "On-Site" );
				PopDBIOTUtils.popZonePoint( z16, 0, 50.909599183363, 26.5843864846553 );
				PopDBIOTUtils.popZonePoint( z16, 1, 50.9097185416641, 26.5843852853374);
				PopDBIOTUtils.popZonePoint( z16, 2, 50.9097172005596,26.5843337146568);
				PopDBIOTUtils.popZonePoint( z16, 3, 50.9095951600494, 26.5843313160199);
				z16.setZoneGroup(zg4);
       
				Zone z17 = PopDBIOTUtils.popZone( barges, localmap2, "Walkway", "#FFFF00", "On-Site" );
				PopDBIOTUtils.popZonePoint( z17, 0, 50.9096702619019, 26.5844572443878 );
				PopDBIOTUtils.popZonePoint( z17, 1, 50.9098057134571, 26.5844572443878);
				PopDBIOTUtils.popZonePoint( z17, 2, 50.9098003490391,26.5843936805623);
				PopDBIOTUtils.popZonePoint( z17, 3, 50.9096742852154, 26.5843960791979);
				z17.setZoneGroup(zg4);
				
				Zone z18 = PopDBIOTUtils.popZone( barges, localmap2, "Bedrooms", "#4DD000", "On-Site" );
				PopDBIOTUtils.popZonePoint( z18, 0, 50.9093752189102, 26.5845795746697 );
				PopDBIOTUtils.popZonePoint( z18, 1, 50.9095696790638, 26.5845783753539);
				PopDBIOTUtils.popZonePoint( z18, 2, 50.9095683379593,26.5845184095451);
				PopDBIOTUtils.popZonePoint( z18, 3, 50.9093671722832, 26.5845148115956);
				z18.setZoneGroup(zg4);
				
				Zone z20 = PopDBIOTUtils.popZone( barges, localmap2, "Gas Safe Area", "#4DD000", "On-Site");
				PopDBIOTUtils.popZonePoint( z20, 0, 50.9102164108715, 26.5846146911793 );
				PopDBIOTUtils.popZonePoint( z20, 1, 50.9103451569042, 26.5846182891257);
				PopDBIOTUtils.popZonePoint( z20, 2, 50.9103424746952,26.5844899623014);
				PopDBIOTUtils.popZonePoint( z20, 3, 50.910212387558, 26.5844863643509);
				z20.setZoneGroup(zg4);

		// Users
		rootUser.setEmail( "root@company.com" );

		UserService.getInstance().update( rootUser );

	

				// Roles for root
		// TODO: this should be done in POPDBAPPCORE or no ?  
		List<Resource> resources1 = ResourceService.list();
		
		for( Resource resource : resources1 )
		{
				RoleResourceService.getInstance().insert( facilityManager, resource, resource.getAcceptedAttributes() );
				RoleResourceService.getInstance().insert( visitorRole, resource, resource.getAcceptedAttributes() );
				
			
		}

	
		// user aramco
		
		User aramcoUser = PopDBUtils.popUser( "aramco", "aramco", aramco, facilityManager );
		aramcoUser.setFirstName( "Aramco" );
		aramcoUser.setLastName( "" );
		aramcoUser.setEmail( "" );

        // user family

        User adminp = PopDBUtils.popUser( "adminp", "adminp", barges,chouestAdminRole );
        
        
        /*Set<UserRole> roles = new HashSet<UserRole>();

        UserRole familyUserRole = new UserRole();
        familyUserRole.setRole( facilityManager );
        familyUserRole.setUser( adminp );
        roles.add( familyUserRole );
        familyUserRole = UserRoleService.getInstance().insert( familyUserRole );
        adminp.setUserRoles( roles );*/
        
        // Reports
      //  createReportDefinitionData2(barges, facilityGroupType, rootUser);
        
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
		reportProperty.setThingType( ThingTypeService.getInstance().get( propertyTypeId ) );
		reportProperty.setThingTypeField( ThingTypeFieldService.getInstance().getThingTypeFieldByName( propertyName ).get( 0 ) );

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
		reportRule.setThingType( ThingTypeService.getInstance().get( TID ) );
		reportRule.setThingTypeField( ThingTypeFieldService.getInstance().getThingTypeFieldByName( propertyName ).get( 0 ) );

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
		// Report 1 - All People Aboard
		ReportDefinition reportDefinition = new ReportDefinition();
		reportDefinition.setName( "1. All People Aboard" );
		reportDefinition.setCreatedByUser( createdByUser );
		reportDefinition.setGroup( group );
		reportDefinition.setReportType( "map" );
		reportDefinition.setDefaultTypeIcon( "pin" );
		reportDefinition.setPinLabel( "1" );
		reportDefinition.setLocalMapId(1L);
		reportDefinition.setDefaultZoom( 19L );
		reportDefinition.setCenterLat( "24.28917307884997" );
		reportDefinition.setCenterLon( "-89.44617127525494" );
		reportDefinition.setPinLabels( false );
		reportDefinition.setZoneLabels( false );
		reportDefinition.setTrails( false );
		reportDefinition.setClustering( false );
		reportDefinition.setPlayback( true );
		reportDefinition.setNupYup( true );
		reportDefinition.setDefaultTypeIcon( "Person" );
		reportDefinition.setDefaultColorIcon( "4DD000" );
		reportDefinition.setDefaultList( false );
		reportDefinition.setGroupTypeFloor( gt );
		reportDefinition.setIsMobile(Boolean.FALSE);
		reportDefinition.setIsMobileDataEntry(Boolean.FALSE);
		reportDefinition = ReportDefinitionService.getInstance().insert( reportDefinition );

		String[] labels = { "Name", "Passenger Type", "Nationality", "Class", "Party of", "Age", "In Zone", "dwellTime( zone )", "serial" };
		String[] propertyNames = { "name", "aboardType", "nationality", "class", "family", "age", "zone", "dwellTime( zone )", "serial" };
		String[] propertyOrders = { "1", "2", "3", "4", "5", "6", "7", "8", "9" };
		Long[] propertyTypeIds = { 2L, 2L, 2L, 2L, 2L, 2L, 1L, 1L, 1L };

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

		String[] value = { "2", "3" };
		Boolean[] isEditable = { false, false };

		String[] propertyNamesRules = { "gender", "gender", "age", "age", "aboardType", "aboardType" };
		String[] operatorRules = { "=", "=", "=", "=", "=", "=" };
		String[] valueRules = { "male", "female", "Baby", "Child", "Captain", "Crew" };
		String[] color = { "1ABC9C", "1ABC9C", "FF9900", "D0E92B", "0093D1", "660099" };
		Long[] ids = { 2L, 2L, 2L, 2L, 2L, 2L };

		for( int it = 0; it < Array.getLength( labelsFilter ); it++ )
		{
			ReportFilter reportFilter = createReportFilter( labelsFilter[it], propertyNamesFilter[it], propertyOrdersFilter[it],
					operatorFilter[it], value[it], isEditable[it], reportDefinition );
			ReportFilterService.getInstance().insert( reportFilter );
		}
		
		for( int it = 0; it < Array.getLength( propertyNamesRules ); it++ )
		{
			ReportRule reportRule = createReportRule( propertyNamesRules[it], operatorRules[it], valueRules[it], color[it], "",
					ids[it], reportDefinition );

			ReportRuleService.getInstance().insert( reportRule );
		}
		
		
		// Report 2 - Crew Members
		ReportDefinition reportDefinition2 = new ReportDefinition();
		reportDefinition2.setName( "2. Crew Members" );
		reportDefinition2.setCreatedByUser( createdByUser );
		reportDefinition2.setGroup( group );
		reportDefinition2.setReportType( "map" );
		reportDefinition2.setDefaultTypeIcon( "pin" );
		reportDefinition2.setPinLabel( "1" );
		reportDefinition2.setLocalMapId(1L);
		reportDefinition2.setDefaultZoom( 19L );
		reportDefinition2.setCenterLat( "24.28917307884997" );
		reportDefinition2.setCenterLon( "-89.44617127525494" );
		reportDefinition2.setPinLabels( false );
		reportDefinition2.setZoneLabels( true );
		reportDefinition2.setTrails( false );
		reportDefinition2.setClustering( false );
		reportDefinition2.setPlayback( false );
		reportDefinition2.setNupYup( true );
		reportDefinition2.setDefaultTypeIcon( "Person" );
		reportDefinition2.setDefaultColorIcon( "4DD000" );
		reportDefinition2.setDefaultList( false );
		reportDefinition2.setGroupTypeFloor( gt );
		reportDefinition2 = ReportDefinitionService.getInstance().insert( reportDefinition2 );

		String[] labels2 = { "Name", "Passenger Type", "Nationality", "Class", "Party of", "Age", "In Zone" };
		String[] propertyNames2 = { "name", "aboardType", "nationality", "class", "family", "age", "zone" };
		String[] propertyOrders2 = { "1", "2", "3", "4", "5", "6", "7" };
		Long[] propertyTypeIds2 = { 2L, 2L, 2L, 2L, 2L, 2L, 1L };

		for( int it = 0; it < Array.getLength( labels2 ); it++ )
		{
			ReportProperty reportProperty = createReportProperty( labels2[it], propertyNames2[it], propertyOrders2[it], propertyTypeIds2[it],
					reportDefinition2 );
			ReportPropertyService.getInstance().insert( reportProperty );
		}

		String[] labelsFilter2 = { "Thing Type", "group" };
		String[] propertyNamesFilter2 = { "thingType.id", "group.id" };

		String[] propertyOrdersFilter2 = { "1", "2" };
		String[] operatorFilter2 = { "=", "=" };

		String[] value2 = { "2", "3" };
		Boolean[] isEditable2 = { false, false };

		String[] propertyNamesRules2 = { "gender", "gender", "age", "age", "aboardType", "aboardType" };
		String[] operatorRules2 = { "=", "=", "=", "=", "=", "=" };
		String[] valueRules2 = { "male", "female", "Baby", "Child", "Captain", "Crew" };
		String[] color2 = { "1ABC9C", "1ABC9C", "FF9900", "D0E92B", "0093D1", "660099" };
		Long[] ids2 = { 2L, 2L, 2L, 2L, 2L, 2L };

		for( int it = 0; it < Array.getLength( labelsFilter2 ); it++ )
		{
			ReportFilter reportFilter = createReportFilter( labelsFilter2[it], propertyNamesFilter2[it], propertyOrdersFilter2[it],
					operatorFilter2[it], value2[it], isEditable2[it], reportDefinition2 );
			ReportFilterService.getInstance().insert( reportFilter );
		}
		
		for( int it = 0; it < Array.getLength( propertyNamesRules2 ); it++ )
		{
			ReportRule reportRule = createReportRule( propertyNamesRules2[it], operatorRules2[it], valueRules2[it], color2[it], "",
					ids2[it], reportDefinition2 );

			ReportRuleService.getInstance().insert( reportRule );
		}
		
		
		// Report 3 - Passengers
		ReportDefinition reportDefinition3 = new ReportDefinition();
		reportDefinition3.setName( "3. Passengers" );
		reportDefinition3.setCreatedByUser( createdByUser );
		reportDefinition3.setGroup( group );
		reportDefinition3.setReportType( "map" );
		reportDefinition3.setDefaultTypeIcon( "pin" );
		reportDefinition3.setPinLabel( "1" );
		reportDefinition3.setLocalMapId(1L);
		reportDefinition3.setDefaultZoom( 19L );
		reportDefinition3.setCenterLat( "24.28917307884997" );
		reportDefinition3.setCenterLon( "-89.44617127525494" );
		reportDefinition3.setPinLabels( false );
		reportDefinition3.setZoneLabels( true );
		reportDefinition3.setTrails( false );
		reportDefinition3.setClustering( false );
		reportDefinition3.setPlayback( false );
		reportDefinition3.setNupYup( true );
		reportDefinition3.setDefaultTypeIcon( "Person" );
		reportDefinition3.setDefaultColorIcon( "4DD000" );
		reportDefinition3.setDefaultList( false );
		reportDefinition3.setGroupTypeFloor( gt );
		reportDefinition3 = ReportDefinitionService.getInstance().insert( reportDefinition3 );

		String[] labels3 = { "Name", "Passenger Type", "Nationality", "Class", "Party of", "Age", "In Zone" };
		String[] propertyNames3 = { "name", "aboardType", "nationality", "class", "family", "age", "zone" };
		String[] propertyOrders3 = { "1", "2", "3", "4", "5", "6", "7" };
		Long[] propertyTypeIds3 = { 2L, 2L, 2L, 2L, 2L, 2L, 1L };

		for( int it = 0; it < Array.getLength( labels3 ); it++ )
		{
			ReportProperty reportProperty = createReportProperty( labels3[it], propertyNames3[it], propertyOrders3[it], propertyTypeIds3[it],
					reportDefinition3 );
			ReportPropertyService.getInstance().insert( reportProperty );
		}

		String[] labelsFilter3 = { "Thing Type", "group" };
		String[] propertyNamesFilter3 = { "thingType.id", "group.id" };

		String[] propertyOrdersFilter3 = { "1", "2" };
		String[] operatorFilter3 = { "=", "=" };

		String[] value3 = { "2", "3" };
		Boolean[] isEditable3 = { false, false };

		String[] propertyNamesRules3 = { "gender", "gender", "age", "age", "aboardType", "aboardType" };
		String[] operatorRules3 = { "=", "=", "=", "=", "=", "=" };
		String[] valueRules3 = { "male", "female", "Baby", "Child", "Captain", "Crew" };
		String[] color3 = { "1ABC9C", "1ABC9C", "FF9900", "D0E92B", "0093D1", "660099" };
		Long[] ids3 = { 2L, 2L, 2L, 2L, 2L, 2L };

		for( int it = 0; it < Array.getLength( labelsFilter3 ); it++ )
		{
			ReportFilter reportFilter = createReportFilter( labelsFilter3[it], propertyNamesFilter3[it], propertyOrdersFilter3[it],
					operatorFilter3[it], value3[it], isEditable3[it], reportDefinition3 );
			ReportFilterService.getInstance().insert( reportFilter );
		}
		
		for( int it = 0; it < Array.getLength( propertyNamesRules3 ); it++ )
		{
			ReportRule reportRule = createReportRule( propertyNamesRules3[it], operatorRules3[it], valueRules3[it], color3[it], "",
					ids3[it], reportDefinition3 );

			ReportRuleService.getInstance().insert( reportRule );
		}
		
		
		// Report 4 - 1st Class
		ReportDefinition reportDefinition4 = new ReportDefinition();
		reportDefinition4.setName( "4. 1st Class" );
		reportDefinition4.setCreatedByUser( createdByUser );
		reportDefinition4.setGroup( group );
		reportDefinition4.setReportType( "map" );
		reportDefinition4.setDefaultTypeIcon( "pin" );
		reportDefinition4.setPinLabel( "1" );
		reportDefinition4.setLocalMapId(1L);
		reportDefinition4.setDefaultZoom( 19L );
		reportDefinition4.setCenterLat( "24.28917307884997" );
		reportDefinition4.setCenterLon( "-89.44617127525494" );
		reportDefinition4.setPinLabels( false );
		reportDefinition4.setZoneLabels( true );
		reportDefinition4.setTrails( false );
		reportDefinition4.setClustering( false );
		reportDefinition4.setPlayback( false );
		reportDefinition4.setNupYup( true );
		reportDefinition4.setDefaultTypeIcon( "Person" );
		reportDefinition4.setDefaultColorIcon( "4DD000" );

		reportDefinition4.setDefaultList( false );
		reportDefinition4.setGroupTypeFloor( gt );
		reportDefinition4 = ReportDefinitionService.getInstance().insert( reportDefinition4 );

		String[] labels4 = { "Name", "Passenger Type", "Nationality", "Class", "Party of", "Age", "In Zone" };
		String[] propertyNames4 = { "name", "aboardType", "nationality", "class", "family", "age", "zone" };
		String[] propertyOrders4 = { "1", "2", "3", "4", "5", "6", "7" };
		Long[] propertyTypeIds4 = { 2L, 2L, 2L, 2L, 2L, 2L, 1L };

		for( int it = 0; it < Array.getLength( labels4 ); it++ )
		{
			ReportProperty reportProperty = createReportProperty( labels4[it], propertyNames4[it], propertyOrders4[it], propertyTypeIds4[it],
					reportDefinition4 );
			ReportPropertyService.getInstance().insert( reportProperty );
		}

		String[] labelsFilter4 = { "Thing Type", "group" };
		String[] propertyNamesFilter4 = { "thingType.id", "group.id" };

		String[] propertyOrdersFilter4 = { "1", "2" };
		String[] operatorFilter4 = { "=", "=" };

		String[] value4 = { "2", "3" };
		Boolean[] isEditable4 = { false, false };

		String[] propertyNamesRules4 = { "gender", "gender", "age", "age", "aboardType", "aboardType" };
		String[] operatorRules4 = { "=", "=", "=", "=", "=", "=" };
		String[] valueRules4 = { "male", "female", "Baby", "Child", "Captain", "Crew" };
		String[] color4 = { "1ABC9C", "1ABC9C", "FF9900", "D0E92B", "0093D1", "660099" };
		Long[] ids4 = { 2L, 2L, 2L, 2L, 2L, 2L };

		for( int it = 0; it < Array.getLength( labelsFilter4 ); it++ )
		{
			ReportFilter reportFilter = createReportFilter( labelsFilter4[it], propertyNamesFilter4[it], propertyOrdersFilter4[it],
					operatorFilter4[it], value4[it], isEditable4[it], reportDefinition4 );
			ReportFilterService.getInstance().insert( reportFilter );
		}
		
		for( int it = 0; it < Array.getLength( propertyNamesRules4 ); it++ )
		{
			ReportRule reportRule = createReportRule( propertyNamesRules4[it], operatorRules4[it], valueRules4[it], color4[it], "",
					ids4[it], reportDefinition4 );

			ReportRuleService.getInstance().insert( reportRule );
		}
		
		
		// Report 5 - Fire Drill
		ReportDefinition reportDefinition5 = new ReportDefinition();
		reportDefinition5.setName( "5. Fire Drill" );
		reportDefinition5.setCreatedByUser( createdByUser );
		reportDefinition5.setGroup( group );
		reportDefinition5.setReportType( "map" );
		reportDefinition5.setDefaultTypeIcon( "pin" );
		reportDefinition5.setPinLabel( "1" );
		reportDefinition5.setLocalMapId(1L);
		reportDefinition5.setDefaultZoom( 19L );
		reportDefinition5.setCenterLat( "24.28917307884997" );
		reportDefinition5.setCenterLon( "-89.44617127525494" );
		reportDefinition5.setPinLabels( false );
		reportDefinition5.setZoneLabels( true );
		reportDefinition5.setTrails( false );
		reportDefinition5.setClustering( false );
		reportDefinition5.setPlayback( false );
		reportDefinition5.setNupYup( true );
		reportDefinition5.setDefaultTypeIcon( "Person" );
		reportDefinition5.setDefaultColorIcon( "FF0000" );

		reportDefinition5.setDefaultList( false );
		reportDefinition5.setGroupTypeFloor( gt );
		reportDefinition5 = ReportDefinitionService.getInstance().insert( reportDefinition5 );

		String[] labels5 = { "Name", "Passenger Type", "Nationality", "Class", "Party of", "Age", "In Zone", "dwellTime( zone )", "RFID Tag #" };
		String[] propertyNames5 = { "name", "aboardType", "nationality", "class", "family", "age", "zone", "dwellTime( zone )", "serial" };
		String[] propertyOrders5 = { "1", "2", "3", "4", "5", "6", "7", "8", "9" };
		Long[] propertyTypeIds5 = { 2L, 2L, 2L, 2L, 2L, 2L, 1L, 1L, 1L };

		for( int it = 0; it < Array.getLength( labels5 ); it++ )
		{
			ReportProperty reportProperty = createReportProperty( labels5[it], propertyNames5[it], propertyOrders5[it], propertyTypeIds5[it],
					reportDefinition5 );
			ReportPropertyService.getInstance().insert( reportProperty );
		}

		String[] labelsFilter5 = { "Thing Type", "group" };
		String[] propertyNamesFilter5 = { "thingType.id", "group.id" };

		String[] propertyOrdersFilter5 = { "1", "2" };
		String[] operatorFilter5 = { "=", "=" };

		String[] value5 = { "2", "3" };
		Boolean[] isEditable5 = { false, false };

		String[] propertyNamesRules5 = { "zone", "zone", "zone", "zone", "zone" };
		String[] operatorRules5 = { "=", "=", "=", "=", "=" };
		String[] valueRules5 = { "Safe Zone 0", "Safe Zone 1", "Safe Zone 2", "Muster Area #1", "Muster Area #2" };
		String[] color5 = { "4DD000", "4DD000", "4DD000", "1ABC9C", "1ABC9C" };
		Long[] ids5 = { 1L, 1L, 1L, 1L, 1L };

		for( int it = 0; it < Array.getLength( labelsFilter5 ); it++ )
		{
			ReportFilter reportFilter = createReportFilter( labelsFilter5[it], propertyNamesFilter5[it], propertyOrdersFilter5[it],
					operatorFilter5[it], value5[it], isEditable5[it], reportDefinition5 );
			ReportFilterService.getInstance().insert( reportFilter );
		}
		
		for( int it = 0; it < Array.getLength( propertyNamesRules5 ); it++ )
		{
			ReportRule reportRule = createReportRule( propertyNamesRules5[it], operatorRules5[it], valueRules5[it], color5[it], "",
					ids5[it], reportDefinition5 );

			ReportRuleService.getInstance().insert( reportRule );
		}
		
		// Report 6 - Crew in Zone too long
		ReportDefinition reportDefinition6 = new ReportDefinition();
		reportDefinition6.setName( "6. Crew in Zone too long" );
		reportDefinition6.setCreatedByUser( createdByUser );
		reportDefinition6.setGroup( group );
		reportDefinition6.setReportType( "map" );
		reportDefinition6.setDefaultTypeIcon( "pin" );
		reportDefinition6.setPinLabel( "1" );
		reportDefinition6.setLocalMapId(1L);
		reportDefinition6.setDefaultZoom( 19L );
		reportDefinition6.setCenterLat( "24.28917307884997" );
		reportDefinition6.setCenterLon( "-89.44617127525494" );
		reportDefinition6.setPinLabels( false );
		reportDefinition6.setZoneLabels( true );
		reportDefinition6.setTrails( false );
		reportDefinition6.setClustering( false );
		reportDefinition6.setPlayback( false );
		reportDefinition6.setNupYup( true );
		reportDefinition6.setDefaultTypeIcon( "CrewMember" );
		reportDefinition6.setDefaultColorIcon( "1ABC9C" );

		reportDefinition6.setDefaultList( false );
		reportDefinition6.setGroupTypeFloor( gt );
		reportDefinition6 = ReportDefinitionService.getInstance().insert( reportDefinition6 );

		String[] labels6 = { "Name", "Passenger Type", "Nationality", "Age", "In Zone", "RFID Tag #", "dwellTime( zone )" };
		String[] propertyNames6 = { "name", "aboardType", "nationality", "age", "zone", "serial", "dwellTime( zone )" };
		String[] propertyOrders6 = { "1", "2", "3", "4", "5", "6", "7" };
		Long[] propertyTypeIds6 = { 2L, 2L, 2L, 2L, 1L, 1L, 1L };

		for( int it = 0; it < Array.getLength( labels6 ); it++ )
		{
			ReportProperty reportProperty = createReportProperty( labels6[it], propertyNames6[it], propertyOrders6[it], propertyTypeIds6[it],
					reportDefinition6 );
			ReportPropertyService.getInstance().insert( reportProperty );
		}

		String[] labelsFilter6 = { "Thing Type", "group" };
		String[] propertyNamesFilter6 = { "thingType.id", "group.id" };

		String[] propertyOrdersFilter6 = { "1", "2" };
		String[] operatorFilter6 = { "=", "=" };

		String[] value6 = { "2", "3" };
		Boolean[] isEditable6 = { false, false };

		String[] propertyNamesRules6 = { "dwellTime( zone )" };
		String[] operatorRules6 = { ">" };
		String[] valueRules6 = { "5000" };
		String[] color6 = { "FF0000" };
		Long[] ids6 = { 1L };

		for( int it = 0; it < Array.getLength( labelsFilter6 ); it++ )
		{
			ReportFilter reportFilter = createReportFilter( labelsFilter6[it], propertyNamesFilter6[it], propertyOrdersFilter6[it],
					operatorFilter6[it], value6[it], isEditable6[it], reportDefinition6 );
			ReportFilterService.getInstance().insert( reportFilter );
		}
		
		for( int it = 0; it < Array.getLength( propertyNamesRules6 ); it++ )
		{
			ReportRule reportRule = createReportRule( propertyNamesRules6[it], operatorRules6[it], valueRules6[it], color6[it], "",
					ids6[it], reportDefinition6 );

			ReportRuleService.getInstance().insert( reportRule );
		}
		
		// Report 7 - Where is the captain
		ReportDefinition reportDefinition7 = new ReportDefinition();
		reportDefinition7.setName( "7. Where is the Captain" );
		reportDefinition7.setCreatedByUser( createdByUser );
		reportDefinition7.setGroup( group );
		reportDefinition7.setReportType( "map" );
		reportDefinition7.setDefaultTypeIcon( "pin" );
		reportDefinition7.setPinLabel( "1" );
		reportDefinition7.setLocalMapId(1L);
		reportDefinition7.setDefaultZoom( 19L );
		reportDefinition7.setCenterLat( "24.28917307884997" );
		reportDefinition7.setCenterLon( "-89.44617127525494" );
		reportDefinition7.setPinLabels( false );
		reportDefinition7.setZoneLabels( true );
		reportDefinition7.setTrails( false );
		reportDefinition7.setClustering( false );
		reportDefinition7.setPlayback( true );
		reportDefinition7.setNupYup( true );
		reportDefinition7.setDefaultTypeIcon( "Captain" );
		reportDefinition7.setDefaultColorIcon( "1ABC9C" );

		reportDefinition7.setDefaultList( false );
		reportDefinition7.setGroupTypeFloor( gt );
		reportDefinition7 = ReportDefinitionService.getInstance().insert( reportDefinition7 );

		String[] labels7 = { "Name", "Passenger Type", "Nationality", "Age", "In Zone", "RFID Tag #", "dwellTime( zone )" };
		String[] propertyNames7 = { "name", "aboardType", "nationality", "age", "zone", "serial", "dwellTime( zone )" };
		String[] propertyOrders7 = { "1", "2", "3", "4", "5", "6", "7" };
		Long[] propertyTypeIds7 = { 2L, 2L, 2L, 2L, 1L, 1L, 1L };

		for( int it = 0; it < Array.getLength( labels7 ); it++ )
		{
			ReportProperty reportProperty = createReportProperty( labels7[it], propertyNames7[it], propertyOrders7[it], propertyTypeIds7[it],
					reportDefinition7 );
			ReportPropertyService.getInstance().insert( reportProperty );
		}

		String[] labelsFilter7 = { "Thing Type", "group" };
		String[] propertyNamesFilter7 = { "thingType.id", "group.id" };

		String[] propertyOrdersFilter7 = { "1", "2" };
		String[] operatorFilter7 = { "=", "=" };

		String[] value7 = { "2", "3" };
		Boolean[] isEditable7 = { false, false };

		/*String[] propertyNamesRules7 = { "dwellTime( zone )" };
		String[] operatorRules7 = { ">" };
		String[] valueRules7 = { "5000" };
		String[] color7 = { "FF0000" };
		Long[] ids7 = { 1L };*/

		for( int it = 0; it < Array.getLength( labelsFilter7 ); it++ )
		{
			ReportFilter reportFilter = createReportFilter( labelsFilter7[it], propertyNamesFilter7[it], propertyOrdersFilter7[it],
					operatorFilter7[it], value7[it], isEditable7[it], reportDefinition7 );
			ReportFilterService.getInstance().insert( reportFilter );
		}
		
		/*for( int it = 0; it < Array.getLength( propertyNamesRules ); it++ )
		{
			ReportRule reportRule = createReportRule( propertyNamesRules6[it], operatorRules6[it], valueRules6[it], color6[it], "",
					ids6[it], reportDefinition6 );

			ReportRuleService.getInstance().insert( reportRule );
		}*/
		
		// Report 8 - Li Family
		ReportDefinition reportDefinition8 = new ReportDefinition();
		reportDefinition8.setName( "Li Family" );
		reportDefinition8.setCreatedByUser( createdByUser );
		reportDefinition8.setGroup( group );
		//reportDefinition8.setGroupShare();
		reportDefinition8.setReportType( "map" );
		reportDefinition8.setDefaultTypeIcon( "pin" );
		reportDefinition8.setPinLabel( "1" );
		reportDefinition8.setLocalMapId(1L);
		reportDefinition8.setDefaultZoom( 19L );
		reportDefinition8.setCenterLat( "24.28917307884997" );
		reportDefinition8.setCenterLon( "-89.44617127525494" );
		reportDefinition8.setPinLabels( false );
		reportDefinition8.setZoneLabels( true );
		reportDefinition8.setTrails( false );
		reportDefinition8.setClustering( false );
		reportDefinition8.setPlayback( false );
		reportDefinition8.setNupYup( true );
		reportDefinition8.setDefaultTypeIcon( "Person" );
		reportDefinition8.setDefaultColorIcon( "4DD000" );

		reportDefinition8.setDefaultList( false );
		reportDefinition8.setGroupTypeFloor( gt );
		reportDefinition8 = ReportDefinitionService.getInstance().insert( reportDefinition8 );

		String[] labels8 = { "Name", "Passenger Type", "Nationality", "Class", "Party of", "Age", "In Zone" };
		String[] propertyNames8 = { "name", "aboardType", "nationality", "class", "family", "age", "zone" };
		String[] propertyOrders8 = { "1", "2", "3", "4", "5", "6", "7" };
		Long[] propertyTypeIds8 = { 2L, 2L, 2L, 2L, 2L, 2L, 1L };

		for( int it = 0; it < Array.getLength( labels8 ); it++ )
		{
			ReportProperty reportProperty = createReportProperty( labels8[it], propertyNames8[it], propertyOrders8[it], propertyTypeIds8[it],
					reportDefinition8 );
			ReportPropertyService.getInstance().insert( reportProperty );
		}

		String[] labelsFilter8 = { "Thing Type", "group" };
		String[] propertyNamesFilter8 = { "thingType.id", "group.id" };

		String[] propertyOrdersFilter8 = { "1", "2" };
		String[] operatorFilter8 = { "=", "=" };

		String[] value8 = { "2", "3" };
		Boolean[] isEditable8 = { false, false };

		String[] propertyNamesRules8 = { "gender", "gender", "age", "age", "aboardType", "aboardType" };
		String[] operatorRules8 = { "=", "=", "=", "=", "=", "=" };
		String[] valueRules8 = { "male", "female", "Baby", "Child", "Captain", "Crew" };
		String[] color8 = { "1ABC9C", "1ABC9C", "FF9900", "D0E92B", "0093D1", "660099" };
		Long[] ids8 = { 2L, 2L, 2L, 2L, 2L, 2L };

		for( int it = 0; it < Array.getLength( labelsFilter8 ); it++ )
		{
			ReportFilter reportFilter = createReportFilter( labelsFilter8[it], propertyNamesFilter8[it], propertyOrdersFilter8[it],
					operatorFilter8[it], value8[it], isEditable8[it], reportDefinition8 );
			ReportFilterService.getInstance().insert( reportFilter );
		}
		
		for( int it = 0; it < Array.getLength( propertyNamesRules8 ); it++ )
		{
			ReportRule reportRule = createReportRule( propertyNamesRules8[it], operatorRules8[it], valueRules8[it], color8[it], "",
					ids8[it], reportDefinition8 );

			ReportRuleService.getInstance().insert( reportRule );
		}
		
		
		
		
		
		// Report 9 - Crew 
		ReportDefinition reportDefinition9 = new ReportDefinition();
		reportDefinition9.setName( "Crew" );
		reportDefinition9.setCreatedByUser( createdByUser );
		reportDefinition9.setGroup( group );
		reportDefinition9.setReportType( "table" );
		reportDefinition9.setDefaultTypeIcon( "pin" );
		//reportDefinition9.setPinLabel( "1" );
		//reportDefinition9.setLocalMapId(1L);
		//reportDefinition9.setDefaultZoom( 19L );
		//reportDefinition9.setCenterLat( "24.28917307884997" );
		//reportDefinition9.setCenterLon( "-89.44617127525494" );
		reportDefinition9.setPinLabels( false );
		reportDefinition9.setZoneLabels( false );
		reportDefinition9.setTrails( false );
		reportDefinition9.setClustering( false );
		reportDefinition9.setPlayback( true );
		reportDefinition9.setNupYup( true );
		//reportDefinition9.setDefaultTypeIcon( "Person" );
		//reportDefinition9.setDefaultColorIcon( "4DD000" );
		reportDefinition9.setDefaultList( false );
		reportDefinition9.setGroupTypeFloor( gt );
		reportDefinition9 = ReportDefinitionService.getInstance().insert( reportDefinition9 );

		String[] labels9 = { "Name", "Zone Group", "Zone", "dwellTime( zone )", "RFID Tag #", "Site", "locationXYZ" };
		String[] propertyNames9 = { "name", "zoneGroup", "zone", "dwellTime( zone )", "serial", "group", "locationXYZ" };
		String[] propertyOrders9 = { "1", "2", "3", "4", "5", "6", "7" };
		Long[] propertyTypeIds9 = { 2L, 1L, 1L, 1L, 1L, 1L, 1L };

		for( int it = 0; it < Array.getLength( labels9 ); it++ )
		{
			ReportProperty reportProperty = createReportProperty( labels9[it], propertyNames9[it], propertyOrders9[it], propertyTypeIds9[it],
					reportDefinition9 );
			ReportPropertyService.getInstance().insert( reportProperty );
		}

		String[] labelsFilter9 = { "Thing Type" };
		String[] propertyNamesFilter9 = { "thingType.id" };

		String[] propertyOrdersFilter9 = { "1" };
		String[] operatorFilter9 = { "=" };

		String[] value9 = { "2" };
		Boolean[] isEditable9 = { false };

		/*String[] propertyNamesRules9 = { "gender", "gender", "age", "age", "aboardType", "aboardType" };
		String[] operatorRules9 = { "=", "=", "=", "=", "=", "=" };
		String[] valueRules9 = { "male", "female", "Baby", "Child", "Captain", "Crew" };
		String[] color9 = { "1ABC9C", "1ABC9C", "FF9900", "D0E92B", "0093D1", "660099" };
		Long[] ids9 = { 2L };*/

		for( int it = 0; it < Array.getLength( labelsFilter9 ); it++ )
		{
			ReportFilter reportFilter = createReportFilter( labelsFilter9[it], propertyNamesFilter9[it], propertyOrdersFilter9[it],
					operatorFilter9[it], value9[it], isEditable9[it], reportDefinition9 );
			ReportFilterService.getInstance().insert( reportFilter );
		}
		
		/*for( int it = 0; it < Array.getLength( propertyNamesRules9 ); it++ )
		{
			ReportRule reportRule = createReportRule( propertyNamesRules9[it], operatorRules9[it], valueRules9[it], color9[it], "",
					ids9[it], reportDefinition9 );

			ReportRuleService.getInstance().insert( reportRule );
		}*/
		
		// Report 10 - All people aboard table
		ReportDefinition reportDefinition10 = new ReportDefinition();
		reportDefinition10.setName( "1. All People Aboard Table Summary" );
		reportDefinition10.setCreatedByUser( createdByUser );
		reportDefinition10.setGroup( group );
		reportDefinition10.setReportType( "tableSummary" );
		reportDefinition10.setDefaultTypeIcon( "pin" );
		//reportDefinition10.setPinLabel( "1" );
		//reportDefinition10.setLocalMapId(1L);
		//reportDefinition10.setDefaultZoom( 19L );
		//reportDefinition10.setCenterLat( "24.28917307884997" );
		//reportDefinition10.setCenterLon( "-89.44617127525494" );
		//reportDefinition10.setPinLabels( false );
		//reportDefinition10.setZoneLabels( false );
		//reportDefinition10.setTrails( false );
		//reportDefinition10.setClustering( false );
		//reportDefinition10.setPlayback( true );
		//reportDefinition10.setNupYup( true );
		//reportDefinition10.setDefaultTypeIcon( "Person" );
		reportDefinition10.setDefaultColorIcon( "4DD000" );

		//reportDefinition10.setDefaultList( false );
		//reportDefinition10.setGroupTypeFloor( gt );
		reportDefinition10 = ReportDefinitionService.getInstance().insert( reportDefinition10 );

		String[] labels10 = { "Name", "Zone Group", "Zone", "dwellTime( zone )", "RFID Tag #", "Site", "locationXYZ" };
		String[] propertyNames10 = { "name", "zoneGroup", "zone", "dwellTime( zone )", "serial", "group", "locationXYZ" };
		String[] propertyOrders10 = { "1", "2", "3", "4", "5", "6", "7" };
		Long[] propertyTypeIds10 = { 2L, 1L, 1L, 1L, 1L, 1L, 1L };

		for( int it = 0; it < Array.getLength( labels10 ); it++ )
		{
			ReportProperty reportProperty = createReportProperty( labels10[it], propertyNames10[it], propertyOrders10[it], propertyTypeIds10[it],
					reportDefinition10 );
			ReportPropertyService.getInstance().insert( reportProperty );
		}

		String[] labelsFilter10 = { "Thing Type", "group" };
		String[] propertyNamesFilter10 = { "thingType.id", "group.id" };

		String[] propertyOrdersFilter10 = { "1", "2" };
		String[] operatorFilter10 = { "=", "=" };

		String[] value10 = { "2", "3" };
		Boolean[] isEditable10 = { false, false };
		
		String[] propertyNamesRules10 = { "aboardType" };
		String[] operatorRules10 = { "=" };
		String[] valueRules10 = { "Crew" };
		String[] color10 = { "FF9900" };
		Long[] ids10 = { 2L };
		 
		for( int it = 0; it < Array.getLength( labelsFilter10 ); it++ )
		{
			ReportFilter reportFilter = createReportFilter( labelsFilter10[it], propertyNamesFilter10[it], propertyOrdersFilter10[it],
					operatorFilter10[it], value10[it], isEditable10[it], reportDefinition10 );
			ReportFilterService.getInstance().insert( reportFilter );
		}
		
		for( int it = 0; it < Array.getLength( propertyNamesRules10 ); it++ )
		{
			ReportRule reportRule = createReportRule( propertyNamesRules10[it], operatorRules10[it], valueRules10[it], color10[it], "",
					ids10[it], reportDefinition10 );

			ReportRuleService.getInstance().insert( reportRule );
		}
		
		
		
		// Report 11 - All people aboard time
		ReportDefinition reportDefinition11 = new ReportDefinition();
		reportDefinition11.setName( "1. All People Aboard Time Series" );
		reportDefinition11.setCreatedByUser( createdByUser );
		reportDefinition11.setGroup( group );
		reportDefinition11.setReportType( "timeSeries" );
		reportDefinition11.setChartType("line");
		reportDefinition11.setLocalMapId(1L);
		//reportDefinition11.setDefaultTypeIcon( "pin" );
		//reportDefinition11.setPinLabel( "1" );
		//reportDefinition11.setDefaultZoom( 19L );
		//reportDefinition11.setCenterLat( "24.28917307884997" );
		//reportDefinition11.setCenterLon( "-89.44617127525494" );
		//reportDefinition11.setPinLabels( false );
		//reportDefinition11.setZoneLabels( false );
		//reportDefinition11.setTrails( false );
		//reportDefinition11.setClustering( false );
		//reportDefinition11.setPlayback( true );
		//reportDefinition11.setNupYup( true );
		//reportDefinition11.setDefaultTypeIcon( "Person" );
		//reportDefinition11.setDefaultColorIcon( "4DD000" );

		//reportDefinition11.setDefaultList( false );
		//reportDefinition11.setGroupTypeFloor( gt );
		
		
		reportDefinition11 = ReportDefinitionService.getInstance().insert( reportDefinition11 );

		//String[] labels11 = { "Name", "Zone Group", "Zone", "dwellTime( zone )", "RFID Tag #", "Site", "locationXYZ" };
		//String[] propertyNames11 = { "name", "zoneGroup", "zone", "dwellTime( zone )", "serial", "group", "locationXYZ" };
		//String[] propertyOrders11 = { "1", "2", "3", "4", "5", "6", "7", "8", "9" };
		//Long[] propertyTypeIds11 = { 2L, 1L, 1L, 1L, 1L, 1L, 1L };

		/*for( int it = 0; it < Array.getLength( labels11 ); it++ )
		{
			ReportProperty reportProperty = createReportProperty( labels11[it], propertyNames11[it], propertyOrders11[it], propertyTypeIds11[it],
					reportDefinition11 );
			ReportPropertyService.getInstance().insert( reportProperty );
		}
*/
		/*String[] labelsFilter11 = { "Thing Type", "group" };
		String[] propertyNamesFilter11 = { "thingType.id", "group.id" };

		String[] propertyOrdersFilter11 = { "1", "2" };
		String[] operatorFilter11 = { "=", "=" };

		String[] value11 = { "2", "3" };
		Boolean[] isEditable11 = { false, false };
*/
		/*String[] propertyNamesRules9 = { "gender", "gender", "age", "age", "aboardType", "aboardType" };
		String[] operatorRules9 = { "=", "=", "=", "=", "=", "=" };
		String[] valueRules9 = { "male", "female", "Baby", "Child", "Captain", "Crew" };
		String[] color9 = { "1ABC9C", "1ABC9C", "FF9900", "D0E92B", "0093D1", "660099" };
		Long[] ids9 = { 2L };*/

		/*for( int it = 0; it < Array.getLength( labelsFilter11 ); it++ )
		{
			ReportFilter reportFilter = createReportFilter( labelsFilter11[it], propertyNamesFilter11[it], propertyOrdersFilter11[it],
					operatorFilter11[it], value11[it], isEditable11[it], reportDefinition11 );
			ReportFilterService.getInstance().insert( reportFilter );
		}
		*/
		/*for( int it = 0; it < Array.getLength( propertyNamesRules9 ); it++ )
		{
			ReportRule reportRule = createReportRule( propertyNamesRules9[it], operatorRules9[it], valueRules9[it], color9[it], "",
					ids9[it], reportDefinition9 );

			ReportRuleService.getInstance().insert( reportRule );
		}*/
		
		
		
		
	}

}
