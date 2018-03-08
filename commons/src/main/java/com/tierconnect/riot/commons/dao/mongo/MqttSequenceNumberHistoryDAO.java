package com.tierconnect.riot.commons.dao.mongo;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

/**
 * @author tcrown
 */
public class MqttSequenceNumberHistoryDAO
{
	static Logger logger = Logger.getLogger( MqttSequenceNumberHistoryDAO.class );

	private static String COLLECTION = "mqtt_sequence_number";
	private static String BRIDGE_CODE = "bridgeCode";
	private static String SEQUENCE_NUMBER = "sequenceNumber";
	private static String TIMESTAMP = "timestamp";
	private static String TTL = "ttl";

	static DBCollection collection;

	public static void setup()
	{
		collection = MongoDAOUtil.getInstance().db.getCollection( COLLECTION );
	}

	public static void insertSequenceNumberHistory( String bridgeCode, long sequenceNumber, Date now, int ttl )
	{
		if( collection == null )
		{
			setup();
		}

		// BasicDBObject doc = new BasicDBObject( "_id", thingId );
		BasicDBObject doc = new BasicDBObject();
		doc.append( BRIDGE_CODE, bridgeCode );
		doc.append( TIMESTAMP, now );
		doc.append( SEQUENCE_NUMBER, sequenceNumber );
		doc.append( TTL, ttl );
		//TODO: handle ttl with proper index the mongo way !
		collection.insert( doc );
	}

	public static void truncate()
	{
		if( collection == null )
		{
			setup();
		}
		
		collection.drop();
	}

	public static Map<String, Long> selectLastValues()
	{
		if( collection == null )
		{
			setup();
		}

		Map<String, Long> map = new HashMap<String, Long>();

		List<?> codes = collection.distinct( BRIDGE_CODE );

		Iterator<?> i = codes.iterator();
		while( i.hasNext() )
		{
			String bridgeCode = (String) i.next();
			BasicDBObject query = new BasicDBObject( BRIDGE_CODE, bridgeCode );
			DBCursor cursor = collection.find( query ).sort( new BasicDBObject( TIMESTAMP, -1 ) ).limit( 1 );
			try
			{
				if( cursor.hasNext() )
				{
					DBObject doc = cursor.next();

					if( doc.containsField( SEQUENCE_NUMBER ) && doc.get( SEQUENCE_NUMBER ) != null )
					{
						map.put( bridgeCode, (Long) doc.get( SEQUENCE_NUMBER ) );
					}
				}
			}
			catch( Exception e )
			{
				logger.warn( "could not get mqtt sequence numbers: ", e );
			}
			finally
			{
				cursor.close();
			}
		}

		return map;
	}
}
