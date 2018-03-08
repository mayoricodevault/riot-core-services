package com.tierconnect.riot.iot.popdb;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.tierconnect.riot.appcore.dao.CassandraUtils;
import com.tierconnect.riot.appcore.popdb.PopDBRequired;
import com.tierconnect.riot.appcore.services.*;
import com.tierconnect.riot.appcore.utils.Configuration;
import com.tierconnect.riot.iot.services.ThingService;
import org.hibernate.Transaction;

import com.tierconnect.riot.appcore.entities.Field;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.GroupType;
import com.tierconnect.riot.appcore.entities.Resource;
import com.tierconnect.riot.appcore.entities.Role;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;
import com.tierconnect.riot.iot.entities.Thing;
import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.services.ThingTypeService;

public class PopDBExampleGM 
{	
	public static void main(String args[]) throws Exception 
	{
		PopDBRequired.initJDBCDrivers();
        System.getProperties().put("hibernate.hbm2ddl.auto", "update");
		CassandraUtils.init(Configuration.getProperty("cassandra.host"), Configuration.getProperty("cassandra.keyspace"));
		PopDBExampleGM popdb = new PopDBExampleGM();
		Transaction transaction = GroupService.getInstance().getGroupDAO().getSession().getTransaction();
		transaction.begin();
		PopDBIOTUtils.initShiroWithRoot();
		popdb.run();
		transaction.commit();
		System.exit(0);
	}
	
	public void run( )
	{
        User rootUser = UserService.getInstance().get(1L);

		// TODO: make selection more robust ?
		Group rootGroup = GroupService.getInstance().get(1L);
		// TODO: make selection more robust ?
		GroupType tenantGroupType = GroupTypeService.getInstance().get(2L);
		// TODO: make selection more robust ?
		Role tenantAdminRole = RoleService.getInstance().get(2L);
				
		System.out.print( "rootGroup=" + rootGroup );
		System.out.print( "tenant=" + tenantGroupType );
		
		Group gmGroup = PopDBUtils.popGroup( "GM", "GM", rootGroup, tenantGroupType, "General Motors, Inc." );		
		
		GroupType region = PopDBUtils.popGroupType( "Region", gmGroup, tenantGroupType, "" );
		GroupType facility = PopDBUtils.popGroupType( "Facility", gmGroup, region, "" );
		GroupType dept = PopDBUtils.popGroupType( "Department", gmGroup, facility, "" );
		GroupType zone = PopDBUtils.popGroupType( "Zone", gmGroup, dept, "" );
		GroupType team = PopDBUtils.popGroupType( "Team", gmGroup, zone, "" );
		GroupType operation = PopDBUtils.popGroupType( "Operation", gmGroup, team, "" );
		
		Group northAmerica = PopDBUtils.popGroup( "NorthAmerica", "NA", gmGroup, region, "" );		
		Group mtp2 = PopDBUtils.popGroup( "Michigan Truck Plant2", "MTP", northAmerica, facility, "" );	
		Group dept21 = PopDBUtils.popGroup( "Department 1", "D1", mtp2, dept, "" );	
		Group zone21 = PopDBUtils.popGroup( "Zone1", "Z1", dept21, zone, "" );	
		Group team21 = PopDBUtils.popGroup( "Team1", "T1", zone21, team, "" );	
		Group oper21 = PopDBUtils.popGroup( "Op1", "O1", team21, operation, "" );	
		
		Group europe = PopDBUtils.popGroup( "Europe", "EU", gmGroup, region, "" );	
		
		//GroupType region2 = PopDBUtils.popGroupType( "Region", gmGroup, "" );
		GroupType facility2 = PopDBUtils.popGroupType( "Facility", europe, region, "");
		GroupType dept2 = PopDBUtils.popGroupType( "Department", europe, facility2, "");
		GroupType zone2 = PopDBUtils.popGroupType( "Zone", europe, dept2, "" );
		GroupType team2 = PopDBUtils.popGroupType( "Team", europe, zone2, "" );
		GroupType operation2 = PopDBUtils.popGroupType( "Operation", europe, team2, "" );
		
			
		Group mtp = PopDBUtils.popGroup( "Michigan Truck Plant", "MTP", northAmerica, facility, "" );	
		Group dept1 = PopDBUtils.popGroup( "Department 1", "D1", mtp, dept, "" );	
		Group zone1 = PopDBUtils.popGroup( "Zone1", "Z1", dept1, zone, "" );	
		Group team1 = PopDBUtils.popGroup( "Team1", "T1", zone1, team, "" );	
		Group oper1 = PopDBUtils.popGroup( "Op1", "O1", team1, operation, "" );
		
		Role normalUserRole = PopDBUtils.popRole( "user", "A normal user", null, gmGroup, tenantGroupType );
		Role readonlyUserRole = PopDBUtils.popRole( "read only user", "A read only user user", null, gmGroup, tenantGroupType );
		
		List<Resource> resources = ResourceService.getInstance().list();
		for( Resource resource : resources )
		{
			if( resource.getGroup().getId() == 1 )
			{
				try
				{
					RoleResourceService.getInstance().insert( normalUserRole, resource, resource.getAcceptedAttributes());
				}
				catch( Exception e )
				{
					System.out.println( "resource=" + resource.getName() );
					System.out.println( "resource=" + resource.getAcceptedAttributes() );
					e.printStackTrace();
				}
				RoleResourceService.getInstance().insert( readonlyUserRole, resource, "r" );
			}
		}
		
		User gmUser1 = PopDBUtils.popUser( "gm1", gmGroup, tenantAdminRole );
		User gmUser2 = PopDBUtils.popUser( "gm2", gmGroup, normalUserRole );
		User gmUser3 = PopDBUtils.popUser( "gm3", gmGroup, normalUserRole );
		User gmUser4 = PopDBUtils.popUser( "gm4", gmGroup, normalUserRole );
		
		
		Field f1 = PopDBUtils.popField( "property1", "description 1", gmGroup );
		Field f2 = PopDBUtils.popField( "property2", "description 2", gmGroup );
		Field f3 = PopDBUtils.popField( "property3", "description 3", gmGroup );
		
		PopDBUtils.popGroupField( gmGroup, f1, "value1" );
		PopDBUtils.popGroupField( gmGroup, f2, "value2" );
		PopDBUtils.popGroupField( gmGroup, f3, "value3" );
		
		PopDBUtils.popGroupField( mtp, f1, "value1" );
		PopDBUtils.popGroupField( mtp, f2, "value2" );
		PopDBUtils.popGroupField( mtp, f3, "value3" );
		
		ThingType dartTag = PopDBIOTUtils.popThingTypeDartTag( mtp );
		List<Thing> things = new ArrayList<Thing>();

		ThingService ts = ThingService.getInstance();
		for( int i = 0; i < 10; i++ )
		{
			String sn = "00719B" + String.format( "%02X", i );
			things.add( ts.insert(dartTag, sn, sn, gmGroup, rootUser) );
		}
	
		Thing t1 = PopDBIOTUtils.popThing( mtp, "fork lift 1", "001", true );
		Thing t2 = PopDBIOTUtils.popThing( mtp, "fork lift 2", "002", true );
		Thing t3 = PopDBIOTUtils.popThing( mtp, "fork lift 3", "003", true );
		
		// relate the dart tags back to the forklifts they are attached to
		long time1 = System.currentTimeMillis();
		long time2 = time1 - 30L * 86400L * 1000L;
		long time3 = time1 - 60L * 86400L * 1000L;
		long time4 = time1 - 90L * 86400L * 1000L;
		
		PopDBIOTUtils.popThingMap( t1, things.get(0), new Date(time4), new Date(time3) );
		PopDBIOTUtils.popThingMap( t1, things.get(1), new Date(time3), new Date(time2) );
		PopDBIOTUtils.popThingMap( t1, things.get(2), new Date(time2), null );
		
		PopDBIOTUtils.popThingMap( t2, things.get(3), new Date(time4), new Date(time3) );
		PopDBIOTUtils.popThingMap( t2, things.get(4), new Date(time3), new Date(time2) );
		PopDBIOTUtils.popThingMap( t2, things.get(5), new Date(time2), null );
		
		PopDBIOTUtils.popThingMap( t3, things.get(6), new Date(time4), new Date(time3) );
		PopDBIOTUtils.popThingMap( t3, things.get(7), new Date(time3), new Date(time2) );
		PopDBIOTUtils.popThingMap( t3, things.get(8), new Date(time2), null );
		
		/*
		// EXPERIMENTAL: set up resources and roleresources for group fields ?
		HashSet<Resource> resources2 = new HashSet<Resource>();
        resources2.add(ResourceService.getInstance().insert(new Resource( gmGroup, "property1", "rw")));
        resources2.add(ResourceService.getInstance().insert(new Resource( gmGroup, "property2", "rw")));
        resources2.add(ResourceService.getInstance().insert(new Resource( gmGroup, "property3", "rw")));
        for( Resource resource : resources2 )
		{
			RoleResourceService.getInstance().insert( normalUserRole, resource, "rw" );
		}
        
        // EXPERIMANTAL: set up roles and users for things
        Role deviceRole = PopDBUtils.popRole( "thing role", "thing user", "A role for inserting thing data", null, gmGroup, tenantGroupType );
		User deviceUser = PopDBUtils.popUser( "thing master", gmGroup, deviceRole );
		
		// EXPERIMENTAL: set up resources and roleresources for thingtypes and thingtypefields ?
        HashSet<Resource> resources3 = new HashSet<Resource>();
        resources3.add(ResourceService.getInstance().insert(new Resource( gmGroup, "thingTypeId:" + dartTag.getId(), "rw")));
        for( Resource resource : resources3 )
		{
			RoleResourceService.getInstance().insert( deviceRole, resource, "rw" );
		}
        
        // EXPERIMENTAL: set up resources and role resources for things and thingfields ?
        HashSet<Resource> resources4 = new HashSet<Resource>();
        resources4.add(ResourceService.getInstance().insert(new Resource( gmGroup, "thingType:" + t1.getId(), "rw")));
        resources4.add(ResourceService.getInstance().insert(new Resource( gmGroup, "thingType:" + t2.getId(), "rw")));
        resources4.add(ResourceService.getInstance().insert(new Resource( gmGroup, "thingType:" + t3.getId(), "rw")));
        for( Resource resource : resources4 )
		{
			RoleResourceService.getInstance().insert( deviceRole, resource, "rw" );
		}
		*/
	}
}
