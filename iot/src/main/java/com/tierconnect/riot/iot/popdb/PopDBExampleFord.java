package com.tierconnect.riot.iot.popdb;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import com.tierconnect.riot.appcore.dao.CassandraUtils;
import com.tierconnect.riot.appcore.popdb.PopDBRequired;
import com.tierconnect.riot.appcore.services.*;
import com.tierconnect.riot.appcore.utils.Configuration;
import com.tierconnect.riot.iot.entities.ThingTypeField;
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

public class PopDBExampleFord 
{	
	GroupType region;
	GroupType facility;
	GroupType dept;
	GroupType zone;
	GroupType team;
	GroupType operation;
	
	Group fordGroup;
	Group northAmerica;
	
	Field f1;
	Field f2; 
	Field f3;
	
	Role teamLeaderRole;
	
	int count = 5;
	
	public static void main(String args[]) throws Exception 
	{
		PopDBRequired.initJDBCDrivers();
        System.getProperties().put("hibernate.hbm2ddl.auto", "update");
		CassandraUtils.init(Configuration.getProperty("cassandra.host"), Configuration.getProperty("cassandra.keyspace"));
		PopDBExampleFord popdb = new PopDBExampleFord();
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
		
		fordGroup = PopDBUtils.popGroup( "Ford", "ford", rootGroup, tenantGroupType, "Ford Motor Company" );		
		
		f1 = PopDBUtils.popField( "property1", "description 1", fordGroup );
		f2 = PopDBUtils.popField( "property2", "description 2", fordGroup );
		f3 = PopDBUtils.popField( "property3", "description 3", fordGroup );
		
		PopDBUtils.popGroupField( fordGroup, f1, "value1" );
		PopDBUtils.popGroupField( fordGroup, f2, "value2" );
		PopDBUtils.popGroupField( fordGroup, f3, "value3" );
		
		Random r = new Random( 0 );
		int c = 0;
		for( int i = 1; i < 5; i++ )
		{
			for( int j = 1; j < 3 + r.nextInt( 5 ); j++ )
			{
				for( int k = 1; k < 3 + r.nextInt( 5 ); k++ )
				{
					f3 = PopDBUtils.popField( "name" + i + ".name" + j + ".name" + k, "description " + c, fordGroup );
					PopDBUtils.popGroupField( fordGroup, f3, "value" + c );
					c++;
				}
			}
		}
		
		Role normalUserRole = PopDBUtils.popRole( "user", "A normal user", null, fordGroup, tenantGroupType );
		Role readonlyUserRole = PopDBUtils.popRole( "read only user", "A read only user user", null, fordGroup, tenantGroupType );
		teamLeaderRole = PopDBUtils.popRole( "team leader", "A team Leader", null, fordGroup, tenantGroupType );
		
		region = PopDBUtils.popGroupType( "Region", fordGroup, tenantGroupType, "" );
		facility = PopDBUtils.popGroupType( "Facility", fordGroup, region, "" );
		dept = PopDBUtils.popGroupType( "Department", fordGroup, facility, "" );
		zone = PopDBUtils.popGroupType( "Zone", fordGroup, dept, "" );
		team = PopDBUtils.popGroupType( "Team", fordGroup, zone, "" );
		operation = PopDBUtils.popGroupType( "Operation", fordGroup, team, "" );
		
		northAmerica = PopDBUtils.popGroup( "NorthAmerica", "NA", fordGroup, region, "" );
		Group mtp = popFacility( northAmerica, "Michigan Truck Plant", "mtp" );
		popFacility( northAmerica, "Plant 2", "p2" );
		popFacility( northAmerica, "Plant 3", "p3" );
		
		Group europe = PopDBUtils.popGroup( "Europe", "EU", fordGroup, region, "" );
		popFacility( europe, "Plant 4", "p4" );
		popFacility( europe, "Plant 5", "p5" );
		popFacility( europe, "Plant 6", "p6" );
		
		// pop role resources
		List<Resource> resources = ResourceService.getInstance().list();
		for( Resource resource : resources )
		{
			if( resource.getGroup().getId() == 1 )
			{
				RoleResourceService.getInstance().insert( normalUserRole, resource, resource.getAcceptedAttributes());
				RoleResourceService.getInstance().insert( readonlyUserRole, resource, "r" );
				RoleResourceService.getInstance().insert( teamLeaderRole, resource, "r" );
			}
		}
		
		User fordUser1 = PopDBUtils.popUser( "ford1", fordGroup, tenantAdminRole );
		User fordUser2 = PopDBUtils.popUser( "ford2", fordGroup, normalUserRole );
		User fordUser3 = PopDBUtils.popUser( "ford3", fordGroup, normalUserRole );
		User fordUser4 = PopDBUtils.popUser( "ford4", fordGroup, normalUserRole );
		
		
		ThingType dartTag = PopDBIOTUtils.popThingTypeDartTag( mtp );
		List<Thing> things = new ArrayList<Thing>();

		ThingService ts = ThingService.getInstance();
		for( int i = 0; i < 10; i++ )
		{
			String sn = "00219B" + String.format( "%02X", i );
			things.add( ts.insert( dartTag, sn, sn, fordGroup, rootUser ) );
		}
	
		Thing t1 = PopDBIOTUtils.popThing( mtp, "fork lift 1", "001", true );
		Thing t2 = PopDBIOTUtils.popThing( mtp, "fork lift 2", "002", true );
		Thing t3 = PopDBIOTUtils.popThing( mtp, "fork lift 3", "003", true );
		
		// relate the dart tags back to the forklifts they are attached to
		long time1 = System.currentTimeMillis();
		// 30 days ago
		long time2 = time1 - 30L * 86400L * 1000L;
		// 60 days ago
		long time3 = time1 - 60L * 86400L * 1000L;
		// 90 days ago
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
		
		//create thingfieldvalue records for each tag
		double value = 0;
		ThingTypeField tf = (new ArrayList<>(things.get(0).getThingType().getThingTypeFields())).get(0);
		for( long t = time4; t < time3; t += 60L * 60L * 10000L )
		{
			Date date = new Date( t );
			value = value + 1; 
			tfv( tf, date, "" + value );
		}
		tf = (new ArrayList<>(things.get(1).getThingType().getThingTypeFields())).get(0);
		for( long t = time3; t < time2; t += 60L * 60L * 10000L )
		{
			Date date = new Date( t );
			value = value + 1; 
			tfv( tf, date, "" + value );
		}
		tf = (new ArrayList<>(things.get(2).getThingType().getThingTypeFields())).get(0);
		for( long t = time2; t < time1; t += 60L * 60L * 10000L )
		{
			Date date = new Date( t );
			value = value + 1; 
			tfv( tf, date, "" + value );
		}
		
		// PROPERTY LEVEL RESOURCE
		// only create resources for properties that need to be controlled
        // otherwise we assume user:r means read you can read all user properties 
        Resource userApiKeyResource = ResourceService.getInstance().insert(new Resource(fordGroup, "user.apiKey", "rw"));
        
		// EXPERIMENTAL: set up resources and roleresources for group fields ?
		HashSet<Resource> resources2 = new HashSet<Resource>();
        resources2.add(ResourceService.getInstance().insert(new Resource( fordGroup, "property1", "rw")));
        resources2.add(ResourceService.getInstance().insert(new Resource( fordGroup, "property2", "rw")));
        resources2.add(ResourceService.getInstance().insert(new Resource( fordGroup, "property3", "rw")));
        
        for( Resource resource : resources2 )
		{
			RoleResourceService.getInstance().insert( normalUserRole, resource, "rw" );
			// A "negative" resource - this role has neither r or w permission on user.apiKey
			// A 'normalUser' should not be able to read or write the apiKey
			RoleResourceService.getInstance().insert( normalUserRole, userApiKeyResource, "" );
		}
        
        // EXPERIMANTAL: set up roles and users for things
        Role deviceRole = PopDBUtils.popRole( "thing role", "A role for inserting thing data", null, fordGroup, tenantGroupType );
		User deviceUser = PopDBUtils.popUser( "thing master", fordGroup, deviceRole );
		
		// EXPERIMENTAL: set up resources and roleresources for thingtypes and thingtypefields ?
        HashSet<Resource> resources3 = new HashSet<Resource>();
        resources3.add(ResourceService.getInstance().insert(new Resource( fordGroup, "thingTypeId:" + dartTag.getId(), "rw")));
        for( Resource resource : resources3 )
		{
			RoleResourceService.getInstance().insert( deviceRole, resource, "rw" );
		}
        
        // EXPERIMENTAL: set up resources and role resources for things and thingfields ?
        HashSet<Resource> resources4 = new HashSet<Resource>();
        resources4.add(ResourceService.getInstance().insert(new Resource( fordGroup, "thingType:" + t1.getId(), "rw")));
        resources4.add(ResourceService.getInstance().insert(new Resource( fordGroup, "thingType:" + t2.getId(), "rw")));
        resources4.add(ResourceService.getInstance().insert(new Resource( fordGroup, "thingType:" + t3.getId(), "rw")));
        for( Resource resource : resources4 )
		{
			RoleResourceService.getInstance().insert( deviceRole, resource, "rw" );
		}
	}
	
	void tfv( ThingTypeField thingField, Date time, String value )
	{
		//ThingFieldValue tfv = new ThingFieldValue();
		
		//tfv.setThingField( thingField );
		//tfv.setTime( time );
		//tfv.setValue( value );
		//ThingFieldValueService.getInstance().insert( tfv );
		
		//return null;
	}
	
	Group popFacility( Group group, String name, String code )
	{		
		Group g = PopDBUtils.popGroup( name, code, group, facility, "" );
		
		for( int i = 0; i < 3; i++ )
		{
			Group dept1 = PopDBUtils.popGroup( "Department " + i, "D" + i, g, dept, "" );	
			for( int j = 0; j < 3; j++ )
			{
				Group zone1 = PopDBUtils.popGroup( "Zone" + j, "Z" + j, dept1, zone, "" );
				for( int k = 0; k < 3; k++ )
				{
					Group team1 = PopDBUtils.popGroup( "Team" + k, "T" + k, zone1, team, "" );	
					for( int l = 0; l < 5; l++ )
					{
						//System.out.println( "populating operation " + count );
						String s = String.format( "%d-%d-%d-%d", i, j, k, l );
						Group oper1 = PopDBUtils.popGroup( "Operation " + s, "Op-" + s, team1, operation, "" );
						User fordUser1 = PopDBUtils.popUser( "ford" + count++, oper1, teamLeaderRole );
					}
				}
			}
		}
		
		PopDBUtils.popGroupField( g, f1, "value1" );
		PopDBUtils.popGroupField( g, f2, "value2" );
		PopDBUtils.popGroupField( g, f3, "value3" );
		
		return g;
	}
}
