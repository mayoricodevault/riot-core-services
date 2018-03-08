package com.tierconnect.riot.appcore.dao;

import com.datastax.driver.core.*;
import com.datastax.driver.core.policies.ConstantReconnectionPolicy;
import com.datastax.driver.core.policies.DowngradingConsistencyRetryPolicy;
import com.datastax.driver.core.policies.Policies;
import org.apache.log4j.Logger;

public class CassandraUtils
{
	private static final Logger logger = Logger.getLogger( CassandraUtils.class );

	private static Session riotSession = null;
	private static Cluster cluster = null;

	public static Session getSession()
	{
		return riotSession;
	}

	/**
	 * initialize Cassandra Connection.
	 * @param hostin Host to Initialize
	 * @param keyspace DataBase name
     */
	public static void init( String hostin, String keyspace )
	{

		PoolingOptions po = new PoolingOptions();
		logger.debug("Default CoreConnectionsPerHost " + po.getCoreConnectionsPerHost(HostDistance.LOCAL));
		logger.debug("Default MaxConnectionsPerHost " + po.getMaxConnectionsPerHost(HostDistance.LOCAL));
		logger.debug("Default MaxSimultaneousRequestsPerConnectionThreshold " + po.getMaxSimultaneousRequestsPerConnectionThreshold(HostDistance.LOCAL));

		po.setMaxConnectionsPerHost(HostDistance.LOCAL, 16);
//		po.setCoreConnectionsPerHost(HostDistance.LOCAL,po.getMaxConnectionsPerHost(HostDistance.LOCAL));
		po.setMaxSimultaneousRequestsPerConnectionThreshold(HostDistance.LOCAL, 128);

		logger.info("New configured CoreConnectionsPerHost " + po.getCoreConnectionsPerHost(HostDistance.LOCAL));
		logger.info("New configured MaxConnectionsPerHost " + po.getMaxConnectionsPerHost(HostDistance.LOCAL));
		logger.info("New configured MaxSimultaneousRequestsPerConnectionThreshold " + po.getMaxSimultaneousRequestsPerConnectionThreshold(HostDistance.LOCAL));

		cluster = Cluster.builder()
						 .withPoolingOptions(po)
						 .withRetryPolicy(DowngradingConsistencyRetryPolicy.INSTANCE)
						 .withReconnectionPolicy(new ConstantReconnectionPolicy(2000L))
						 .addContactPoint(hostin)
						 .build();
		cluster.getConfiguration().getSocketOptions().setConnectTimeoutMillis(480000);
		//cluster.getConfiguration().getPoolingOptions().setMaxConnectionsPerHost(HostDistance.LOCAL,100);

		Metadata metadata = cluster.getMetadata();
		logger.info( "Connected to cluster: " + metadata.getClusterName() );

		for( Host host : metadata.getAllHosts() )
		{
			logger.info( String.format( "Datacenter: %s; Host: %s; Rack: %s\n", host.getDatacenter(), host.getAddress(), host.getRack() ) );
		}
		riotSession = cluster.connect( keyspace );


//        cluster = Cluster.builder().addContactPoint( hostin ).build();
//
//		Metadata metadata = cluster.getMetadata();
//		logger.info( "Connected to cluster: " + metadata.getClusterName() );
//
//		for( Host host : metadata.getAllHosts() )
//		{
//			logger.info( String.format( "Datacenter: %s; Host: %s; Rack: %s\n", host.getDatacenter(), host.getAddress(), host.getRack() ) );
//		}
//		riotSession = cluster.connect( keyspace );
	}

	/**
	 * Close Cassandra data base connection.
	 */
	public static void shutdown(){
		if (cluster != null && ! cluster.isClosed()) {
			cluster.close();
		}
	}
}
