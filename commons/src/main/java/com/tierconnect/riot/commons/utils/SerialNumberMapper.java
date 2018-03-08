package com.tierconnect.riot.commons.utils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * 
 * @author tcrown
 *
 */
public class SerialNumberMapper implements Serializable
{
	private static final long serialVersionUID = 1L;

	private static final Logger logger = Logger.getLogger( SerialNumberMapper.class );

	private boolean debug = false;

	private int count;

	// KEY: topic VALUE: starting offset
	private Map<String, Integer> startingPoints;

	// KEY: topic VALUE: number of "threads" for this topic
	private Map<String, Integer> numberOfThreads;

	private boolean locked = false;
	
	// for debugging
	public static boolean useTrivialMapping = false;
	
	public static void main( String[] args ) throws Exception
	{
		SerialNumberMapper snm = new SerialNumberMapper();
		
		snm.addTopic( "/v1/data/ALEB1", 1 );
		snm.addTopic( "/v1/data/ALEB2", 4 );
		snm.addTopic( "/v1/data/ALEB3", 2 );
		snm.addTopic( "/v1/data/ALEB4", 3 );

		snm.addTopic("/v1/data", 100);
		
		int kp = snm.getKafkaPartition("/v1/data/ALEB1", "000000456788");
		int sp = snm.getSparkPartition( "/v1/data/ALEB2", "000000456788" );
		
		System.out.println( "kp=" + kp  );
		System.out.println( "sp=" + sp  );

		// The new logic will get the partition by serial and thing type code.
		String multiKey = String.format("%s-%s", "SERIAL0001", "item");
		int kafkaPartition1 = snm.getKafkaPartitionNew("/v1/data", multiKey);
		int kafkaPartition2 = snm.getKafkaPartitionNew("/v1/data", multiKey);

		logger.info(String.format("kafkaPartition1: %d", kafkaPartition1));
		logger.info(String.format("kafkaPartition2: %d", kafkaPartition2));
	}

	public SerialNumberMapper()
	{
		count = 0;
		startingPoints = new HashMap<String, Integer>();
		numberOfThreads = new HashMap<String, Integer>();
	}
	
	// for kafka, use number of partitions as the numOfThreads !!!
	public void addTopic( String topic, int numOfThreads ) throws Exception
	{
		if( locked )
		{
			// all of the calls to addTopic() ust be done once on start up, before any calls to getXPartion() methods.
			// if new topics are added, must shut down the system, wait for the kafka ques to be consumed, then restart.
			throw new Error( "CAN NOT CALL addTopic() AFTER CALLING getSparkKafkaPartition() OR getSparkPartition()" );
		}
		
		logger.info( "LOADING this=" + this.hashCode() + " topic=" + topic + " numOfThreads=" + numOfThreads );
		
		if( startingPoints.containsKey( topic ) )
		{
			throw new Exception( "topic=" + topic + "has already been loaded !" );
		}
		
		startingPoints.put( topic, count );
		numberOfThreads.put( topic, numOfThreads );
		count += numOfThreads;
	}

	public boolean initialized()
	{
		return startingPoints.size() > 0;
	}

	/**
	 * @param n
	 *            The number of threads, or partitions.
	 * @param str
	 *            The string to map. For example, the serialNumber.
	 * @return a number between 0 and n-1
	 */
	public int mapSerial( int n, String str )
	{
		int r;
		
		if( useTrivialMapping )
		{
			// use only on serials with [0-9] in the last two digits !
			r = Integer.parseInt( str.substring( str.length() - 2 ) );
		}
		else
		{
			// hash is 32 chars
			String hash = DigestUtils.md5Hex( str );
			// Note: Long MAX_VALUE = 0x7fffffffffffffff
			// so we take the first 15 chars from hash
			String sub = hash.substring( 0, 15 );
			long num = Long.parseLong( sub, 16 );
			r = (int) (num % n);
			
			if( debug )
			{
				System.out.println( str + " " + hash + " " + sub + " " + num + " " + r );
			}
		}
	
		if( StringUtils.isEmpty( str ) )
		{
			logger.warn( "n=" + n + "empty str='" + str + "'" );
		}
		
		return r;
	}

	/**
	 * Gets the kafka partition by serial number
	 *
	 * @param topic
	 * @param serialNumber
	 * @return
	 */
	public int getKafkaPartition(String topic, String serialNumber)
	{
		locked = true;
		int n = numberOfThreads.get( topic );
		return mapSerial( n, serialNumber );
	}

	public int getSparkPartition( String topic, String serialNumber )
	{
		locked = true;
		logger.info( "topic=" + topic + " serialNumber=" + serialNumber + " startingPoints=" + startingPoints  );
		int base = startingPoints.get( topic );
		int n = numberOfThreads.get( topic );
		int offset = mapSerial( n, serialNumber );
		return base + offset;
	}

	/**
	 * Gets the kafka partition by key
	 *
	 * @param topic the topic
	 * @param key the key (serial number - thing type code)
	 * @return the kafka partition
	 */
	public int getKafkaPartitionNew(String topic, String key)
	{
		locked = true;
		int n = numberOfThreads.get( topic );
		return mapSerial( n, key );
	}

	/**
	 * Gets the number of threads.
	 *
	 * @param topic the topic
	 * @return the number of threads
	 */
	public int getNumberOfThreads( String topic )
	{
		return numberOfThreads.get( topic );
	}

	/**
	 * Gets the base by topic
	 *
	 * @param topic the topic
	 * @return the base
	 */
	public int getBase( String topic )
	{
		return startingPoints.get( topic );
	}
}
