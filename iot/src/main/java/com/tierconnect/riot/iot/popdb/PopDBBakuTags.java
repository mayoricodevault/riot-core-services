package com.tierconnect.riot.iot.popdb;

import org.apache.log4j.Logger;
import org.hibernate.Transaction;

import com.tierconnect.riot.appcore.dao.CassandraUtils;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.popdb.PopDBRequired;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.UserService;
import com.tierconnect.riot.appcore.utils.Configuration;
import com.tierconnect.riot.iot.controllers.ThingTypeController;
import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.services.ThingService;
import com.tierconnect.riot.iot.services.ThingTypeService;

// a debugging class for coreBridge long thing instantiate times issue
public class PopDBBakuTags
{
	private static final Logger logger = Logger.getLogger( PopDBBakuTags.class );

	public static void main( String args[] ) throws Exception
	{
		int method = Integer.parseInt( args[0] ); // 1=service or 2=controller

		switch( method )
		{
			case 1:
			logger.info( "Using SERVICE method" );
			break;
			
			case 2:
			logger.info( "Using CONTROLLER method" );
			break;
		}
		
		PopDBRequired.initJDBCDrivers();
		System.getProperties().put( "hibernate.hbm2ddl.auto", "update" );
		CassandraUtils.init( Configuration.getProperty( "cassandra.host" ), Configuration.getProperty( "cassandra.keyspace" ) );

		long groupId = 1;
		long userId = 1;

		String code = "default_gps_thingtype";


		Transaction transaction = GroupService.getGroupDAO().getSession().getTransaction();
		transaction.begin();

		ThingType thingType = ThingTypeService.getInstance().getByCode( code );
		User user = UserService.getInstance().get( userId );
		Group group = GroupService.getInstance().get( groupId );

		PopDBIOTUtils.initShiroWithRoot();

		transaction.commit();

		if( thingType == null )
		{
			logger.error( "thingType for thingTypeCode=" + code + " does not exist !" );
			System.exit( 1 );
		}

		ThingTypeController ttc = new ThingTypeController();

		long t0 = System.currentTimeMillis();
		long count = 0;
		//for( int k = 0; k < 2; k++ )
		for( int i = 0; i < 180; i++ )
		{
			transaction = GroupService.getGroupDAO().getSession().getTransaction();
			transaction.begin();

			StringBuffer sb = new StringBuffer();
			int sn = 0;
			
			for( int j = 0; j < 1000; j++ )
			{
				sn = i * 1000 + j + 1;
				String serial = String.format( " %06d ", sn );

				if( method == 1 )
				{
					ThingService.getInstance().insert( thingType, serial, serial, group, user );
					
					if( j % 100 == 0 )
					{
						GroupService.getGroupDAO().getSession().flush();
						GroupService.getGroupDAO().getSession().clear();
					}
				}
				else
				{
					sb.append( serial + "\n" );
				}

				count++;
			}

			if( method == 2 )
			{
				ttc.instantiateManyThings( thingType.getId(), group.getId(), sb.toString(), true );
			}

			transaction.commit();

			long t1 = System.currentTimeMillis();
			double delt_sec = (t1 - t0) / 1000.0;
			double rate = count / delt_sec;
			double totalTime = 180000.0 / rate;
			double timeRemaining = totalTime - delt_sec;
			logger.info( String.format(
					"THINGS CREATED: count=%6d  rate=%6.3f [things/sec]  estimatedTime=%6.1f [min] timeRemaining=%6.1f [min]", sn, rate,
					totalTime / 60.0, timeRemaining / 60.0 ) );

		}

		System.exit( 0 );
	}
}
