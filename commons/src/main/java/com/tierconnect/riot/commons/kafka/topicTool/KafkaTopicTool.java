package com.tierconnect.riot.commons.kafka.topicTool;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.ZkConnection;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.tierconnect.riot.commons.kafka.topicTool.ConfigurationTopics;
import com.tierconnect.riot.commons.kafka.topicTool.Topic;

import kafka.admin.AdminUtils;
import kafka.admin.RackAwareMode;
import kafka.common.TopicAlreadyMarkedForDeletionException;
import kafka.log.LogConfig;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;

/**
 * KafkaTopicTool class
 *
 * @author aquiroz
 * @version 2017/04/13
 */
public class KafkaTopicTool
{
	private static final Logger logger = Logger.getLogger( KafkaTopicTool.class );

	private static final String BrokerTopicsPath = "/brokers/topics";

	private Options options;
	private CommandLine line;
	private ZkClient zkClient;
	private ZkUtils zkUtils;

	public static void main( String args[] ) throws Exception
	{
		KafkaTopicTool t = new KafkaTopicTool( args );
		t.run();
	}

	public KafkaTopicTool( String[] args ) throws IOException
	{
		options = new Options();
		options.addOption( "c", false, "create topics" );
		options.addOption( "d", false, "delete topics (broken, does not currently work)" );
		options.addOption( "f", true, "json config file (default [classpath]/kafkaTopics.json)" );
		options.addOption( "h", false, "show this help" );
		options.addOption( "z", true, "zookeeper (default 'localhost:2181')" );

		CommandLineParser parser = new BasicParser();
		try
		{
			line = parser.parse( options, args );
		}
		catch( Exception e )
		{
			logger.fatal( "error:", e );
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( String.format( "java -cp %s %s", "riot-core-services-all.jar", KafkaTopicTool.class.getName() ), options );
			System.exit( 1 );
		}

		if( line.hasOption( "h" ) )
		{
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( String.format( "java -cp %s %s", "riot-core-services-all.jar", KafkaTopicTool.class.getName() ), options );
			System.exit( 1 );
		}
	}

	public void run() throws Exception
	{
		System.out.println( "**********************************************" );
		System.out.println( "*****   Initializing KafkaTopicTool     ******" );
		System.out.println( "**********************************************" );

		String zookeeper = line.hasOption( "z" ) ? line.getOptionValue( "z" ) : "localhost:2181";
		logger.info( "kafkaZookeeper=" + zookeeper );

		zkClient = new ZkClient( zookeeper, 3000, 3000, ZKStringSerializer$.MODULE$ );
		zkUtils = new ZkUtils( zkClient, new ZkConnection( zookeeper ), false );

		ConfigurationTopics config = null;

		if( line.hasOption( "f" ) )
		{
			String filePath = line.getOptionValue( "f" );
			InputStream inputStream = new FileInputStream( filePath );
			ObjectMapper mapper = new ObjectMapper();
			config = mapper.readValue( inputStream, ConfigurationTopics.class );
		}
		else
		{
			ObjectMapper mapper = new ObjectMapper();
			String defaultFile = "kafkaTopics.json";
			ClassLoader classLoader = KafkaTopicTool.class.getClassLoader();
			InputStream inputStream = classLoader.getResourceAsStream( defaultFile );
			config = mapper.readValue( inputStream, ConfigurationTopics.class );
		}

		if( line.hasOption( "c" ) )
		{
			createTopics( config.topics );
		}
		else if( line.hasOption( "d" ) )
		{
			deleteTopics( config.topics );
		}
		else
		{
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( String.format( "java -cp %s %s", "riot-core-services-all.jar", KafkaTopicTool.class.getName() ), options );
			System.exit( 1 );
		}
	}

	/**
	 * Creates topics.
	 */
	public void createTopics( List<Topic> topics ) throws Exception
	{
		logger.info( "Executing create topics..." );

		for( Topic topic : topics )
		{
			String topicName = StringUtils.replace( topic.name, "/", "___" );
			try
			{
				Properties props = new Properties();

				if( topic.name.startsWith( "/v1/cache" ) )
				{
					props.put( LogConfig.MinCleanableDirtyRatioProp(), "0.01" );
					props.put( LogConfig.CleanupPolicyProp(), "compact" );
					props.put( LogConfig.SegmentMsProp(), "18000000" ); // 30min
					props.put( LogConfig.DeleteRetentionMsProp(), "18000000" ); // 30min
				}

				AdminUtils.createTopic( zkUtils, topicName, topic.partitions, topic.replicas, props, RackAwareMode.Disabled$.MODULE$ );
				logger.info( String.format( "Topic created: '%s' ", topicName ) );
			}
			catch( Exception e )
			{
				logger.warn( String.format( "Cannot create topic: '%s' ", topicName ), e );
			}
		}
	}

	public void deleteTopics( List<Topic> topics ) throws InterruptedException
	{
		for( Topic entry : topics )
		{
			deleteTopic( entry.name );
		}
		logger.info( "Restart kafka...." );
	}

	/**
	 * Delete a topic.
	 */
	public boolean deleteTopic( final String topicName ) throws InterruptedException
	{
		Preconditions.checkNotNull( topicName, "The topicName is null" );

		logger.info( "Executing delete topic..." + topicName );
		boolean isDelete = false;
		int attemps = 0;
		while( !isDelete )
		{
			try
			{
				AdminUtils.deleteTopic( zkUtils, topicName );
				zkClient.deleteRecursive( ZkUtils.getTopicPath( topicName ) );
				isDelete = true;
			}
			catch( TopicAlreadyMarkedForDeletionException e )
			{
				logger.warn( String.format( "Cannot delete topic=['%s']. Trying again...attempts=['%d']", topicName, attemps ), e );
				isDelete = true;
			}
		}
		return isDelete;
	}

	public Set<String> getAllTopicsFromServer()
	{
		List<String> topics = zkClient.getChildren( BrokerTopicsPath );
		if( topics == null )
			return Sets.newHashSet();
		else
			return Sets.newHashSet( topics );
	}
}
