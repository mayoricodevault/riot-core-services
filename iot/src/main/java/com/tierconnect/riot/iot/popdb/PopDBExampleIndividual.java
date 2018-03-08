package com.tierconnect.riot.iot.popdb;

import java.util.Date;
import java.util.List;

import com.tierconnect.riot.appcore.dao.CassandraUtils;
import com.tierconnect.riot.appcore.popdb.PopDBRequired;
import com.tierconnect.riot.appcore.services.*;
import com.tierconnect.riot.appcore.utils.Configuration;
import com.tierconnect.riot.iot.services.ThingService;
import org.hibernate.Transaction;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.GroupType;
import com.tierconnect.riot.appcore.entities.Resource;
import com.tierconnect.riot.appcore.entities.Role;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;
import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.services.ThingTypeService;

/**
 * This class gives an example of how an individual be be a user of the system (a tenant)
 * 
 * 
 * @author tcrown
 *
 */
public class PopDBExampleIndividual 
{
	public static void main(String args[]) throws Exception 
	{
		PopDBRequired.initJDBCDrivers();
        System.getProperties().put("hibernate.hbm2ddl.auto", "update");
		CassandraUtils.init(Configuration.getProperty("cassandra.host"), Configuration.getProperty("cassandra.keyspace"));
		PopDBExampleIndividual popdb = new PopDBExampleIndividual();
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
		
		Group terryGroup = PopDBUtils.popGroup( "terry", "terry", rootGroup, tenantGroupType, "terry's account" );		
		
		Role guestRole = PopDBUtils.popRole( "guest user", "A guest user", null, terryGroup, tenantGroupType );
		
		User user1 = PopDBUtils.popUser( "terry", terryGroup, tenantAdminRole );
		User user2 = PopDBUtils.popUser( "guest1", terryGroup, guestRole );
		User user3 = PopDBUtils.popUser( "guest2", terryGroup, guestRole );
		User user4 = PopDBUtils.popUser( "guest3", terryGroup, guestRole );
		
		List<Resource> resources = ResourceService.getInstance().list();
		for( Resource resource : resources )
		{
			RoleResourceService.getInstance().insert( guestRole, resource, "r" );
		}
		
		ThingType iphone = PopDBIOTUtils.popThingTypeIPhone(terryGroup);

		ThingService ts = ThingService.getInstance();
		for( int i = 0; i < 4; i++ )
		{
			String serial = "000" + i;
			ts.insert( iphone, serial, serial, terryGroup, rootUser);
		}
	}
}
