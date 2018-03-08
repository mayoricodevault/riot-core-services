package com.tierconnect.riot.commons.dao.mongo;

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

/**
 * 
 * Writes bridge statistics to the [BRIDGE_CODE]Log collection(s)
 * 
 * @author fernando
 * 
 */
public class StatsDAO
{
	// 1 ref in CoreBridge
	public static void saveStats( LinkedHashMap<String, Object> statsMap )
	{
		Object o = statsMap.get( "bridgeCode" );
		if( o != null )
		{
			String bridgeCode = o.toString();
			DBCollection statsCollection = MongoDAOUtil.getInstance().db.getCollection( "bridge_stats_" + bridgeCode );

			BasicDBObject statsDoc = new BasicDBObject( "time", new Date() );
			Iterator<String> it = statsMap.keySet().iterator();
			while( it.hasNext() )
			{
				String key = it.next();
				statsDoc.append( key, statsMap.get( key ) );
			}

			statsCollection.insert( statsDoc );
		}
	}
}
