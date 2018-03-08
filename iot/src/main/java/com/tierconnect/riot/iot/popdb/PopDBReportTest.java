package com.tierconnect.riot.iot.popdb;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.GroupType;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.popdb.PopDBRequired;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.GroupTypeService;
import com.tierconnect.riot.appcore.services.UserService;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.*;
import org.apache.log4j.Logger;
import org.hibernate.Transaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class PopDBReportTest
{
	private static final Logger logger = Logger.getLogger( PopDBReportTest.class );

	Random r = new Random( 0 );

	int reportFilterOrder;

	public static void main( String args[] ) throws Exception
	{
		PopDBRequired.initJDBCDrivers();
		System.getProperties().put( "hibernate.hbm2ddl.auto", "update" );

		PopDBReportTest popdb = new PopDBReportTest();
		Transaction transaction = GroupService.getGroupDAO().getSession().getTransaction();
		transaction.begin();
		PopDBIOTUtils.initShiroWithRoot();

		int n = Integer.parseInt( args[0] );
		popdb.run( n );

		transaction.commit();
	}

	public void run( int n )
	{
		for( ReportDefinition rd : ReportDefinitionService.getReportDefinitionDAO().selectAll() )
		{
			ReportDefinitionService.getInstance().delete( rd );
		}

		User rootUser = UserService.getInstance().getRootUser();
		GroupType storeGroupType = GroupTypeService.getInstance().get( 3L );
		Group santaMonica = GroupService.getInstance().get( 3L );

		for( int i = 0; i < n; i++ )
		{
			createReport1( santaMonica, storeGroupType, rootUser, "Table Detail " + String.format( "%03d", i ) );
		}
	}

	private void createReport1( Group group, GroupType gt, User user, String name )
	{
		ReportDefinition rd = createReportDefinition( name, group, gt, user );

		// section 1: date
		createDateFilter( rd );

		// section 2 : Tenant Group Type
		createTenantGroupTypeFilter( rd );

		// section 3 : Tenant Group
		createTenantGroupFilter( rd );

		// Section 4: Thing Type
		createThingTypeFilter( rd );

		// Section 5 : Zone Type and property
		createZoneTypeOrPropertyFilter( rd );

		// Section 6: UDF filters
		int l = r.nextInt( 5 );
		for( int i = 0; i < l; i++ )
		{
			createUDFPropertyFilter( rd );
		}

		// PART II: report properties
		// TODO: create many report properties !!!!!
		createReportProperty( "Name", "name", "1", 1L, rd );
		createReportProperty( "Serial Number", "serial", "1", 1L, rd );

		// String[] labels4 = { "RFID Tag #", "Group", "Zone", "Zone Dwell Time
		// (zone)", "Last Detect Time", "Name", "Location" };
		// String[] propertyNames4 = { "serial", "group.name", "zone",
		// "dwellTime( zone )", "lastDetectTime", "name", "location" };
		// String[] propertyOrders4 = { "1", "2", "3", "4", "5", "6", "7" };
		// Long[] propertyTypeIds4 = { 1L, 1L, 1L, 1L, 1L, 1L, 1L };
		// for( int it = 0; it < Array.getLength( labels4 ); it++ )
		// {
		// createReportProperty( labels4[it], propertyNames4[it],
		// propertyOrders4[it], propertyTypeIds4[it], rd );
	}

	private ReportDefinition createReportDefinition( String name, Group group, GroupType gt, User createdByUser )
	{
		ReportDefinition rd = new ReportDefinition();
		rd.setName( name );
		rd.setCreatedByUser( createdByUser );
		rd.setGroup( group );
		rd.setReportType( "table" );
		// rd.setDefaultTypeIcon( "pin" );
		// rd.setPinLabels( false );
		// rd.setZoneLabels( false );
		// rd.setTrails( false );
		// rd.setClustering( false );
		// rd.setPlayback( true );
		// rd.setNupYup( false );
		rd.setDefaultList( false );
		rd.setGroupTypeFloor( gt );
		// reportDefinition4.setDefaultColorIcon( "009F6B" );
		rd.setRunOnLoad( true );
		rd.setIsMobile(Boolean.FALSE);
		rd.setIsMobileDataEntry(Boolean.FALSE);
		reportFilterOrder = 1;

		return ReportDefinitionService.getInstance().insert( rd );
	}

	private ReportFilter createReportFilter( String label, String propertyName, String operatorFilter, String value, Boolean isEditable,
			Long thingTypeId, Long thingTypeFieldId, ReportDefinition reportDefinition )
	{
		ReportFilter reportFilter = new ReportFilter();
		reportFilter.setLabel( label );
		reportFilter.setPropertyName( propertyName );
		reportFilter.setDisplayOrder( (float) reportFilterOrder++ );
		reportFilter.setOperator( operatorFilter );
		reportFilter.setValue( value );
		reportFilter.setEditable( isEditable );
		reportFilter.setThingTypeField(ThingTypeFieldService.getInstance().get(thingTypeFieldId));
		reportFilter.setThingType(ThingTypeService.getInstance().get(thingTypeId));
		reportFilter.setReportDefinition( reportDefinition );

		ReportFilterService.getInstance().insert( reportFilter );

		return reportFilter;
	}

	private void createDateFilter( ReportDefinition rd )
	{
		// TODO: dates other than NOW !
		int i = r.nextInt( 2 );
		switch( i )
		{
			case 0:
				createReportFilter( "Date", "relativeDate", "", "NOW", false, null, null, rd );
				break;

			case 1:
				createReportFilter( "Date", "relativeDate", "", "AGO_DAY_1", false, null, null, rd );
				break;
		}
	}

	private void createTenantGroupTypeFilter( ReportDefinition rd )
	{
		GroupType gt = getRandomGroupType();
		createReportFilter( "Tenant Group Type", "group.groupType.id", "=", gt.getId().toString(), false, null, null, rd );
	}

	private void createTenantGroupFilter( ReportDefinition rd )
	{
		Group g = getRandomGroup();
		createReportFilter( "Tenant Group ", "group.id", "<", g.getId().toString(), false, null, null, rd );
	}

	private void createThingTypeFilter( ReportDefinition rd )
	{
		// == means unassigned checkbox has been selected
		String op = r.nextInt( 2 ) == 0 ? "=" : "==";
		ThingType tt = getRandomThingType();
		createReportFilter( "Thing Type", "thingType.id", op, tt.getId().toString(), false, null, null, rd );
	}

	private void createZoneTypeOrPropertyFilter( ReportDefinition rd )
	{
		int i = r.nextInt( 2 );
		switch( i )
		{
			case 0:
				ZoneType zt = this.getRandomZoneType();
				// TODO: zoneType seems broken in version 1 !
				// createReportFilter( "Zone Type", "zoneType.id", "=",
				// zt.getId().toString(), false, null, null, rd );
				break;

			case 1:
				ZoneProperty zp = getRandomZoneProperty();
				// TODO: get real zone property value ?
				createReportFilter( zp.getName(), "zoneProperty.id", "=", "one", false, null, zp.getId(), rd );
				break;
		}
	}

	private void createUDFPropertyFilter( ReportDefinition rd )
	{
		// TODO: handle numeric operators
		String[] stringOperators = new String[] { "=", "!=", "isEmpty", "~" };
		String op = stringOperators[r.nextInt( stringOperators.length )];

		// simple properties Strings: =, !=, ~, isEmpty for strings
		// String type UDFS
		// number UDFS
		// native Object UDFS (value, timestamp, dwellTime)
		// zone properties
		ThingType tt = getRandomThingType();
		int i = r.nextInt( 2 );
		switch( i )
		{
			// native properties
			case 0:
				int j = r.nextInt( 3 );
				switch( j )
				{
					case 0:
						// TODO: get real name ?
						createReportFilter( "name", "name", op, "1", false, tt.getId(), null, rd );
						break;

					case 1:
						// TODO: get real serialNumber ?
						createReportFilter( "serial", "serial", op, "1", false, tt.getId(), null, rd );
						break;
				}
				break;

			// UDFs
			case 1:
				ThingTypeField ttf = getRandomThingTypeField( tt );
				String javaType = ttf.getDataType().getClazz();
				int k = r.nextInt( 3 );
				switch( k )
				{
					// UDF value
					case 0:

						// shift, zone, logicalReader, group, thing
						// TODO: switch operator on numeric type
						// TODO: if UDF is native object, then handle native
						// object properties

						// TODO: get real value ?

						// TODO: also generate arrays for specific properties !

						if( "com.tierconnect.riot.iot.entities.Zone".equals( javaType ) )
						{
							// TODO id, name, code,
							// localMap.id, zoneGroup.id
							// TODO
							// zoneGroup.name, zoneType.id, zoneType.name,
							// zoneType.zoneTypeCode, localMap.id
							switch( r.nextInt( 5 ) )
							{
								// id
								case 0:
									Long id = this.getRandomZone().getId();
									String o = getRandomOp( new String[] { "=", "!=", "isEmpty", "~" } );
									createReportFilter( ttf.getName(), ttf.getName(), o, "" + id, false, tt.getId(), ttf.getId(), rd );
									break;

								// name
								case 1:
									Zone z = this.getRandomZone();
									String o2 = getRandomOp( new String[] { "=", "!=", "isEmpty", "~" } );
									createReportFilter( ttf.getName() + " Name", ttf.getName() + ".name", o2, z.getName(), false,
											tt.getId(), ttf.getId(), rd );
									break;

								// code
								case 2:
									Zone z3 = this.getRandomZone();
									String o3 = getRandomOp( new String[] { "=", "!=", "isEmpty", "~" } );
									createReportFilter( ttf.getName() + " Code", ttf.getName() + "Code.name", o3, z3.getName(), false,
											tt.getId(), ttf.getId(), rd );
									break;

								// localMap.id
								case 3:
									LocalMap lm = this.getRandomLocalMap();
									String o4 = getRandomOp( new String[] { "=", "!=", "isEmpty" } );
									createReportFilter( "localMap.id", "localMap.id", o4, "" + lm.getId(), false, tt.getId(), ttf.getId(),
											rd );
									break;
									
									// zoneGroup.id
								case 4:
									ZoneGroup zg = this.getRandomZoneGroup();
									String o5 = getRandomOp( new String[] { "=", "!=", "isEmpty" } );
									createReportFilter( "zoneGroup.id", "zoneGroup.id", o5, "" + zg.getId(), false, tt.getId(), ttf.getId(),
											rd );
									break;
							}
						}
						else if( "com.tierconnect.riot.iot.entities.Shift".equals( javaType ) )
						{
							// notes: id (=,!=,empty), name (not in UI), code
							// (not in UI)
							Long id = this.getRandomShift().getId();
							String o = getRandomOp( new String[] { "=", "!=", "isEmpty" } );
							createReportFilter( ttf.getName(), ttf.getName(), o, "" + id, false, tt.getId(), ttf.getId(), rd );
						}
						else if( "com.tierconnect.riot.iot.entities.LogicalReader".equals( javaType ) )
						{
							// TODO id, name, code
						}
						// TODO com.tierconnect.riot.appcore.entities.Group
						// TODO com.tierconnect.riot.iot.entities.Thing
						else
						{
							// BigDecimal
							// Boolean
							// Long
							// String
							// Date
							// TODO: get real values ?
							createReportFilter( ttf.getName(), ttf.getName(), op, "1", false, tt.getId(), ttf.getId(), rd );
						}

						break;

					// UDF timestamp
					case 1:
						// TODO !
						break;

					// UDF dwellTime
					case 2:
						String dwellTime = "dwellTime( " + ttf.getName() + " )";
						String[] stringOperators2 = new String[] { "<=", ">=" };
						String op2 = stringOperators2[r.nextInt( stringOperators2.length )];
						// TODO: get real value ?
						createReportFilter( dwellTime, dwellTime, op2, "1", false, tt.getId(), null, rd );
						break;
				}
				break;
		}
	}

	private ReportProperty createReportProperty( String label, String propertyName, String propertyOrder, Long thingTypeId,
			ReportDefinition reportDefinition )
	{
		ReportProperty reportProperty = new ReportProperty();
		reportProperty.setLabel( label );
		reportProperty.setPropertyName(propertyName);
		reportProperty.setDisplayOrder(Float.parseFloat(propertyOrder));
        ThingType thingType = ThingTypeService.getInstance().get(thingTypeId);
        reportProperty.setThingType(thingType);
		reportProperty.setThingTypeField(thingType.getThingTypeFieldByName(propertyName));
		reportProperty.setReportDefinition( reportDefinition );

		ReportPropertyService.getInstance().insert( reportProperty );

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
        ThingType thingType = ThingTypeService.getInstance().get(TID);
        reportRule.setThingType(thingType);
        reportRule.setThingTypeField(thingType.getThingTypeFieldByName(propertyName));

        return reportRule;
	}

	private GroupType getRandomGroupType()
	{
		List<GroupType> list = GroupTypeService.getGroupTypeDAO().selectAll();
		Collections.shuffle( list );
		return list.get( 0 );
	}

	private Group getRandomGroup()
	{
		List<Group> list = GroupService.getGroupDAO().selectAll();
		Collections.shuffle( list );
		return list.get( 0 );
	}

	private ThingType getRandomThingType()
	{
		List<ThingType> list = ThingTypeService.getThingTypeDAO().selectAll();
		Collections.shuffle( list );
		return list.get( 0 );
	}

	private ThingTypeField getRandomThingTypeField( ThingType tt )
	{
		List<ThingTypeField> list2 = new ArrayList<ThingTypeField>( tt.getThingTypeFields() );
		Collections.shuffle( list2 );
		return list2.get( 0 );
	}

	private Zone getRandomZone()
	{
		List<Zone> list = ZoneService.getZoneDAO().selectAll();
		Collections.shuffle( list );
		return list.get( 0 );
	}
	private ZoneGroup getRandomZoneGroup()
	{
		List<ZoneGroup> list = ZoneGroupService.getZoneGroupDAO().selectAll();
		Collections.shuffle( list );
		return list.get( 0 );
	}

	private ZoneType getRandomZoneType()
	{
		List<ZoneType> list = ZoneTypeService.getZoneTypeDAO().selectAll();
		Collections.shuffle( list );
		return list.get( 0 );
	}

	private ZoneProperty getRandomZoneProperty()
	{
		List<ZoneProperty> list = ZonePropertyService.getZonePropertyDAO().selectAll();
		Collections.shuffle( list );
		return list.get( 0 );
	}

	private LocalMap getRandomLocalMap()
	{
		List<LocalMap> list = LocalMapService.getLocalMapDAO().selectAll();
		Collections.shuffle( list );
		return list.get( 0 );
	}

	// private List<Long> getRandomShiftIds()
	// {
	// List<Shift> list = ShiftService.getShiftDAO().selectAll();
	// Collections.shuffle( list );
	//
	// List<Long> list2 = new ArrayList<Long>();
	//
	// int l = r.nextInt( list.size() );
	//
	// for( int i = 0; i < l; i++ )
	// {
	// list2.add( list.get( i ).getId() );
	// }
	// return list2;
	// }

	private Shift getRandomShift()
	{
		List<Shift> list = ShiftService.getShiftDAO().selectAll();
		Collections.shuffle( list );
		return list.get( 0 );
	}

	private String getRandomOp( String[] ops )
	{
		return ops[r.nextInt( ops.length )];
	}
}
