package com.tierconnect.riot.iot.popdb;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Date;
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
import com.tierconnect.riot.appcore.popdb.PopDBRequired;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.GroupTypeService;
import com.tierconnect.riot.appcore.services.ResourceService;
import com.tierconnect.riot.appcore.services.RoleResourceService;
import com.tierconnect.riot.appcore.services.RoleService;
import com.tierconnect.riot.appcore.services.UserService;
import com.tierconnect.riot.appcore.utils.Configuration;

//import com.tierconnect.riot.iot.services.FieldValueService;
import com.tierconnect.riot.iot.services.ReportDefinitionService;
import com.tierconnect.riot.iot.services.ReportFilterService;
import com.tierconnect.riot.iot.services.ReportPropertyService;
import com.tierconnect.riot.iot.services.ThingService;

public class PopDBMojixGM
{
	private static final Logger logger = Logger.getLogger( PopDBRiot.class );

    public static void main( String args[] ) throws Exception
    {
        System.out.println("**********************************************************");
        System.out.println("popdb batch deprecated");
        System.out.println("Usage: " +
                "\t" +
                "gradle clean assemble popdb");
    }

/*
	public static void main( String args[] ) throws Exception
	{
		PopDBRequired.initJDBCDrivers();
        System.getProperties().put("hibernate.hbm2ddl.auto", "update");

		CassandraUtils.init(Configuration.getProperty("cassandra.host"), Configuration.getProperty("cassandra.keyspace"));
		PopDBMojixGM popdb = new PopDBMojixGM();
		PopDBMojixRetail popdb2 = new PopDBMojixRetail();
		PopDBMojixDemo popdb3= new PopDBMojixDemo();
		Transaction transaction = GroupService.getGroupDAO().getSession().getTransaction();
		transaction.begin();
		PopDBIOTUtils.initShiroWithRoot();

		popdb2.run();
		popdb3.run();
		popdb.run();

		transaction.commit();
		System.exit( 0 );
	}

	public void run()
	{
		createData();
	}

	private void createData()
	{
		PopDBMojixUtils.modifyExistingRecords();

		User rootUser = UserService.getInstance().getRootUser();
		Group rootGroup = GroupService.getInstance().getRootGroup();
		GroupType tenantGroupType = GroupTypeService.getInstance().getTenantGroupType();

		// facility, test wing and zone
		// Groups GM
		Group gm = PopDBUtils.popGroup( "GM", "GM", rootGroup, tenantGroupType, "" );
		gm.setCode("gm");

		GroupType cityGroupType1 = PopDBUtils.popGroupType( "Facility", gm, tenantGroupType, "" );
		GroupType company = PopDBUtils.popGroupType( "Test Wing", gm, cityGroupType1, "" );
		GroupType location = PopDBUtils.popGroupType( "Zone", gm, company, "" );

		createReportDefinitionData( gm );

		
		LocalMap lm=PopDBIOTUtils.populateFacilityMap( "Map Facility Pontiac Powertrain", "images/GMPontiac_grayscale.png", gm, -83.2844606256,
				-83.2831409788, 42.6613942702, 42.6617433876, 0.0,"ft" );

		// facility pontiac
		Group pontiac = PopDBUtils.popGroup( "Pontiac Powertrain", "PP", gm, cityGroupType1, "" );

		Edgebox edgebox = PopDBIOTUtils.popEdgebox( pontiac, "ALE Bridge GM", "ALEBGM", "{ \"mqtt\" : { \"host\" : \"localhost\", \"port\" : 1883 }, \"timeDistanceFilter\" : { \"time\" : 0.0, \"distance\" : 10.0 }, \"timeZoneFilter\" : { \"time\" : 0.0 }, \"log\" : 0  }" );
        String description0 = "Send the first point according to the serial number";
       // PopDBIOTUtils.popEdgeboxRule(edgebox.getId(), "FirstLocationFilter", PopDBIOTUtils.getDefaultFilterQuery(), "MojixMemberMessage", "DefaultSubscriber","", description0,false);
        String description1 = "Send a point if the distance between two points is major than a distance d";
        //PopDBIOTUtils.popEdgeboxRule(edgebox.getId(), "DistanceFilter", PopDBIOTUtils.getLocationFilterQuery(), "MojixMemberMessage", "LocationSubscriber", "{\"sendPreviousPoint\":false}", description1,false);
		
		Group locationGroup = PopDBUtils.popGroup( "1", "1", pontiac, company, "1" );

		// facility Tonawanda
		Group tona = PopDBUtils.popGroup( "Tonawanda Engine Plant", "TEP", gm, cityGroupType1, "" );

	
		// Zones for Test Wing "1"
		Group area = PopDBUtils.popGroup( "Staging Area", "SA", locationGroup, location, "" );

		Group area1 = PopDBUtils.popGroup( "Engine Test Cell D141", "ETCD141", locationGroup, location,
				"" );

		Group area2 = PopDBUtils.popGroup( "Engine Test Cell D139", "ETCD139", locationGroup, location,
				"" );

		Group area3 = PopDBUtils.popGroup( "Engine Test Cell D101", "ETCD101", locationGroup, location,
				"" );

		Zone z1 = PopDBIOTUtils.popZone( pontiac, lm, "Test Wing", "#FF0000" );
		PopDBIOTUtils.popZoneBB( z1, -83.2844606256, 42.6613942702, -83.2831409788, 42.6617433876 );

		Zone z2 = PopDBIOTUtils.popZone( pontiac, lm, "Engine Test Cell D139", "#FF0000" );
		PopDBIOTUtils.popZoneBB( z2, -83.28440379564589, 42.6614439888007, -83.28435082201787, 42.66149872338002 );

		Zone z3 = PopDBIOTUtils.popZone( pontiac, lm, "Engine Test Cell D141", "#FF0000" );
		PopDBIOTUtils.popZoneBB( z3, -83.28435082201787, 42.6614439888007, -83.28430187170335, 42.66149872338002 );

		Zone z4 = PopDBIOTUtils.popZone( pontiac, lm, "Engine Test Cell D101", "#FF0000" );
		PopDBIOTUtils.popZoneBB( z4, -83.28325651263812, 42.6614439888007, -83.28320823287584, 42.66149872338002 );

		Zone z5 = PopDBIOTUtils.popZone( pontiac, lm, "Staging Area", "#FF0000" );
		PopDBIOTUtils.popZoneBB( z5, -83.2844554281694, 42.66152288547627, -83.28421268822237, 42.661627615028465 );

		PopDBIOTUtils.populateFacilityMap( "Map Coderoad", "images/CodeRoad_transparent.png", gm, -68.085278, -68.084812, -16.541923,
				-16.541561, 30,"ft" );

		Zone z6 = PopDBIOTUtils.popZone( pontiac, lm, "Code Road Office", "#FF0000" );
		PopDBIOTUtils.popZoneBB( z6, -68.085278 - 0.0001, -16.541923 - 0.0001, -68.084812 + 0.0001, -16.541561 + 0.0001 );

        ThingType rfid = PopDBIOTUtils.popThingTypeRFID(pontiac, "default_rfid_thingtype.gm");
		ThingType engine = PopDBIOTUtils.popThingType(pontiac, null, "Engine");
		PopDBIOTUtils.popThingTypeField( engine, "Model", "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, ThingTypeField.Type.TYPE_TEXT.value, false ,null, null);
		PopDBIOTUtils.popThingTypeField( engine, "LastUpdatedTimestamp", "millisecond", "ms",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, ThingTypeField.Type.TYPE_NUMBER.value, false ,null, null);


		PopDBIOTUtils.popThingTypeMap( engine, rfid );

		ThingService ts = ThingService.getInstance();

		for( int i = 1; i < 10; i++ )
		{
			String sn = String.format( "%dLv%05d", 39 + i, i );
			Thing th = ts.insert( engine, sn, sn, pontiac, rootUser);

			Set<ThingTypeField> fields = th.getThingType().getThingTypeFields();
			for( ThingTypeField field : fields )
			{
				if( field.getName().equals( "Model" ) )
				{
					switch( i % 4 )
					{
					case (0):
					{
						FieldValueService.insert( th.getId(), field.getId(),
								new Date(), "5.8L V8", field.getTimeSeries() );
					}
						break;
					case (1):
					{
						FieldValueService.insert( th.getId(), field.getId(),
								new Date(), "3.6L V6", field.getTimeSeries() );
					}
						break;
					case (2):
					{
						FieldValueService.insert( th.getId(), field.getId(),
								new Date(), "2.3L V6", field.getTimeSeries() );
					}
						break;
					case (3):
					{
						FieldValueService.insert( th.getId(), field.getId(),
								new Date(), "2.3L I4", field.getTimeSeries() );
					}
						break;
					}

				}
				else
				{
					FieldValueService.insert( th.getId(), field.getId(),
							new Date(), "2014-09-11T16:13:30.583-07:00", field.getTimeSeries() );
				}
			}

			int nm = i + 473;
			String serial = String.format( "B%020d", nm );

			Thing thi = ThingService.getInstance().insert(rfid, serial, serial, pontiac, rootUser, th);

			Set<ThingTypeField> fields2 = thi.getThingType().getThingTypeFields();
			for( ThingTypeField field : fields2 )
			{
				switch( field.getName().toString() )
				{
				case ("LastUpdatedTimestamp"):
				{
					FieldValueService.insert( thi.getId(), field.getId(),
							new Date(), "2014-09-11T16:13:30.583-07:00", field.getTimeSeries() );
					break;
				}
				case ("LogicalReader"):
				{
					switch( i % 3 )
					{
					case (1):
					{
						FieldValueService.insert( thi.getId(), field.getId(),
								new Date(), "Engine Test Cell D141", field.getTimeSeries() );
						break;
					}
					case (2):
					{
						FieldValueService.insert( thi.getId(), field.getId(),
								new Date(), "Engine Test Cell D139", field.getTimeSeries() );
						break;
					}
					case (0):
					{
						FieldValueService.insert( thi.getId(), field.getId(),
								new Date(), "Engine Test Cell D101", field.getTimeSeries() );
						break;
					}

					}
					;
					break;

				}
				// case ("Location"):{FieldValueDAO.update(th.getId(),
				// field.getId(), "2014-09-11T16:13:30.583-07:00", new Date(),
				// 0.0f, 0.0f);break;}
				case ("eNode"):
				{
					FieldValueService.insert( thi.getId(), field.getId(),
							new Date(), "x3ed9371", field.getTimeSeries() );
					break;
				}
//				case ("dwellTime"):
//				{
//					FieldValueDAO.update( thi.getId(), field.getId(), new Date(), "0h" );
//					break;
//				}
				case ("direction"):
				{
					if( i == 1 || i == 9 )
						FieldValueService.insert( thi.getId(), field.getId(),
								new Date(), "Out", field.getTimeSeries() );
					else
						FieldValueService.insert( thi.getId(), field.getId(),
								new Date(), "In", field.getTimeSeries() );
					break;
				}
				}
			}
		}

		// Users		
		rootUser.setEmail( "root@company.com" );

		Role companyUser = PopDBUtils.popRole( "User", "CU", "User", new ArrayList<Resource>(), gm, tenantGroupType );
		Role companyadmin = PopDBUtils.popRole( "Company Administrator GM", "CU", "Company Administrator GM", new ArrayList<Resource>(),
				gm, company );
		Role facilityadmin = PopDBUtils.popRole( "Facility Administrator", "CU", "Facility Administrator", new ArrayList<Resource>(), gm,
				company );

		Role rootRole = RoleService.getInstance().getRootRole();
		List<Resource> resources1 = ResourceService.list();
		for( Resource resource : resources1 )
		{
			if( resource.getName().toString().startsWith( "$" ) && !resource.getName().toString().equals( "$Pants" )
					&& !resource.getName().toString().equals( "$Jackets" ) && !resource.getName().toString().equals( "$Passive RFID Tags" ) )
			{
				RoleResourceService.getInstance().insert( rootRole, resource, "riuda" );
			}
		}
		// user gm
		User admin = PopDBUtils.popUser( "adminp2", gm, companyadmin );
		User samUser = PopDBUtils.popUser( "ken", "ken", gm, companyadmin );
		samUser.setFirstName( "Ken" );
		samUser.setLastName( "Hamkins" );
		User paulUser = PopDBUtils.popUser( "scott", "scott", pontiac, companyUser );
		paulUser.setFirstName( "Scott" );
		paulUser.setLastName( "Chalfant" );
		User adminp = PopDBUtils.popUser( "adminc2", "adminc2", pontiac, facilityadmin );

		UserService.getInstance().update( rootUser );
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

	private ReportProperty createReportProperty( String label, String propertyName, String propertyOrder, ReportDefinition reportDefinition )
	{
		ReportProperty reportProperty = new ReportProperty();
		reportProperty.setLabel( label );
		reportProperty.setPropertyName( propertyName );
		reportProperty.setDisplayOrder( Float.parseFloat( propertyOrder ) );
		reportProperty.setReportDefinition( reportDefinition );
		return reportProperty;
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

	private void createReportDefinitionData( Group group )
	{
		User rootUser = UserService.getInstance().get( 1L );

		// ReportDefinition 1
		ReportDefinition reportDefinition = new ReportDefinition();
		reportDefinition.setName( "Where are my engines?" );
		reportDefinition.setGroup( group );
		reportDefinition.setPinLabel( "1" );
		reportDefinition.setReportType( "map" );
		reportDefinition.setDefaultZoom( 20L );
		reportDefinition.setCenterLat( "42.661560" );
		reportDefinition.setCenterLon( "-83.283795" );
		reportDefinition.setDefaultTypeIcon( "pin" );
		reportDefinition.setCreatedByUser( rootUser );
		reportDefinition = ReportDefinitionService.getInstance().insert( reportDefinition );

		String[] labels = { "Engine Model", "Passive RFID Tag#", "Logical Reader", "Last Updated", "Type", "Name" };
		String[] propertyNames = { "parent.model", "serial", "LogicalReader", "LastUpdatedTimestamp", "thingType.name", "name" };
		String[] propertyOrders = { "1", "2", "3", "4", "5", "6" };
		Long[] propertyTypeIds = { 4L, 3L, 4L, 4L, 4L, 4L };

		for( int it = 0; it < Array.getLength( labels ); it++ )
		{
			ReportProperty reportProperty = createReportProperty( labels[it], propertyNames[it], propertyOrders[it], propertyTypeIds[it],
					reportDefinition );
			ReportPropertyService.getInstance().insert( reportProperty );
		}

		String[] labelsFilter = { "Group", "Thing" };
		String[] propertyNamesFilter = { "group.id", "thingType.id" };
		String[] propertyOrdersFilter = { "1", "2" };
		String[] operatorFilter = { "=", "=" };
		String[] value = { "13", "4" };
		Boolean[] isEditable = { true, true };

		for( int it = 0; it < Array.getLength( labelsFilter ); it++ )
		{
			ReportFilter reportFilter = createReportFilter( labelsFilter[it], propertyNamesFilter[it], propertyOrdersFilter[it],
					operatorFilter[it], value[it], isEditable[it], reportDefinition );
			ReportFilterService.getInstance().insert( reportFilter );
		}

		// ReportDefinition2
		ReportDefinition reportDefinition2 = new ReportDefinition();
		reportDefinition2.setName( "Engine Zone Changes" );
		reportDefinition2.setGroup( group );
		reportDefinition2.setPinLabel( "2" );
		reportDefinition2.setReportType( "table" );
		reportDefinition2.setDefaultTypeIcon( "pin" );
		reportDefinition2.setCreatedByUser( rootUser );
		reportDefinition2 = ReportDefinitionService.getInstance().insert( reportDefinition2 );

		String[] labels2 = { "Engine Serial", "Engine Model", "Passive RFID Tag#", "Event", "Logical Reader", "Last Updated", "Type", "Name" };
		String[] propertyNames2 = { "parent.serial", "parent.model", "serial", "direction", "LogicalReader", "LastUpdatedTimestamp",
				"thingType.name", "name" };
		String[] propertyOrders2 = { "1", "2", "3", "4", "5", "6", "7", "8" };
		Long[] propertyTypeIds2 = { 3L, 3L, 4L, 4L, 4L, 4L, 4L, 4L };

		for( int it = 0; it < Array.getLength( labels2 ); it++ )
		{
			ReportProperty reportProperty = createReportProperty( labels2[it], propertyNames2[it], propertyOrders2[it],
					propertyTypeIds2[it], reportDefinition2 );
			ReportPropertyService.getInstance().insert( reportProperty );
		}

		String[] labelsFilter2 = { "Group", "Thing" };
		String[] propertyNamesFilter2 = { "group.id", "thingType.id" };
		String[] propertyOrdersFilter2 = { "1", "2" };
		String[] operatorFilter2 = { "=", "=" };
		String[] value2 = { "13", "4" };
		Boolean[] isEditable2 = { true, true };

		for( int it = 0; it < Array.getLength( labelsFilter2 ); it++ )
		{
			ReportFilter reportFilter = createReportFilter( labelsFilter2[it], propertyNamesFilter2[it], propertyOrdersFilter2[it],
					operatorFilter2[it], value2[it], isEditable2[it], reportDefinition2 );
			ReportFilterService.getInstance().insert( reportFilter );
		}
	}
*/
}
