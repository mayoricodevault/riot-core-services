package com.tierconnect.riot.datagen;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.tierconnect.riot.iot.entities.ThingField;
import com.tierconnect.riot.iot.entities.ThingTypeField;

/**
 * Mongo implementation of persister. Does the mongo magic to persist data.
 */
public class MongoPersister implements Persister
{
	static Logger logger = Logger.getLogger( MongoPersister.class );

	private final static long CACHE_SIZE = 800;

	private DB db;

	private Map<String, List<DBObject>> objects = new HashMap<>();

	private boolean drop;

	@Override
	public void start()
	{
		// do nothing
		if( drop )
		{
			db.getCollection( "Asset" ).drop();
			db.getCollection( "Box" ).drop();
			db.getCollection( "Carton" ).drop();
			db.getCollection( "Pallet" ).drop();

			db.getCollection( "AssetX" ).drop();
			db.getCollection( "BoxX" ).drop();
			db.getCollection( "CartonX" ).drop();
			db.getCollection( "PalletX" ).drop();
		}

		// db.getCollection("shit2").createIndex( { name: 1 } );
		// db.getCollection("shit2").createIndex( { parent: 1 } );

		// db.getCollection('Asset').createIndex( { groupId : 1, timestamp: 1 }
		// );
		// db.getCollection('Box').createIndex( { groupId : 1, timestamp: 1 } );
		// db.getCollection('Carton').createIndex( { groupId : 1, timestamp: 1 }
		// )
		// db.getCollection('Pallet').createIndex( { groupId : 1, timestamp: 1 }
		// )

		// sh.shardCollection( "bm10.Asset", { "groupId" : 1, "timestamp" : 1 }
		// );
		// sh.shardCollection( "bm10.Box", { "groupId" : 1, "timestamp" : 1 } );
		// sh.shardCollection( "bm10.Carton", { "groupId" : 1, "timestamp" : 1 }
		// );
		// sh.shardCollection( "bm10.Pallet", { "groupId" : 1, "timestamp" : 1 }
		// );
	}

	@Override
	public void end()
	{
		// flush caches
		for( String type : objects.keySet() )
		{
			List<DBObject> cache = objects.get( type );
			if( !cache.isEmpty() )
			{
				db.getCollection( type ).insert( cache );
			}
		}
	}

	public MongoPersister( String mongoHost, int mongoPort, String mongoDatabase, Integer connectTimeOut, Integer connectionsPerHost,
			String username, String password, boolean drop ) throws UnknownHostException
	{

		MongoClientOptions options = MongoClientOptions.builder().connectTimeout( connectTimeOut == null ? 3000 : connectTimeOut )
				.connectionsPerHost( connectionsPerHost == null ? 200 : connectionsPerHost )  // sets
				// the
				// connection
				// timeout
				// to
				// 3
				// seconds
				// .autoConnectRetry( true )
				.build();

		// MongoCredential credential2 =
		// MongoCredential.createPlainCredential(username, "admin",
		// password.toCharArray());
		
		MongoCredential credential = MongoCredential.createCredential( username, "admin", password.toCharArray() );
		
		//MongoClient mongoClient = new MongoClient( new ServerAddress( mongoHost, mongoPort ), Arrays.asList( credential ), options );
		
		MongoClient mongoClient = new MongoClient( new ServerAddress( mongoHost, mongoPort ), options );

		db = mongoClient.getDB( mongoDatabase );

		this.drop = drop;
	}

	@Override
	public void persist( Thing thing )
	{
		Map<String, Object> map = new LinkedHashMap<String, Object>();

		map.put( "_id", thing.id );
		map.put( "name", thing.name );
		map.put( "serial", thing.serial );
		map.put( "createdOn", thing.createdOn );
		map.put( "timestamp", thing.createdOn.getTime() );
		map.put( "type", thing.tt.getName() );
		map.put( "groupId", thing.groupId );

		if( thing.parent != null )
		{
			map.put( "parent", thing.parent.id );
		}

		for( ThingField tf : thing.udfs )
		{
			Map<String, Object> udfMap = new LinkedHashMap<>();
			ThingTypeField ttf = (ThingTypeField) tf.getThingTypeField();
			udfMap.put( "id", ttf.getId() );
			udfMap.put( "value", tf.getValue() );
			udfMap.put( "date", new Date( tf.getTimestamp() ) );
			// map.put("udf" + udfIndex++, udfMap);
			map.put( ttf.getName(), udfMap );
		}

		BasicDBObject obj = new BasicDBObject( map );

		cacheOrFlush( obj, thing.tt.getName() );
	}

	private void cacheOrFlush( DBObject obj, String type )
	{
		List<DBObject> cache = objects.get( type );

		// cache found
		if( cache != null )
		{
			cache.add( obj );
			if( cache.size() == CACHE_SIZE )
			{
				db.getCollection( type ).insert( cache );
				cache.clear();
			}
		}
		// no cache found. init and put in map
		else
		{
			cache = new ArrayList<>();
			cache.add( obj );
			objects.put( type, cache );
		}
	}

	@Override
	public long getMaxId()
	{
		long count = 0;
		count += db.getCollection( "Asset" ).count();
		count += db.getCollection( "Box" ).count();
		count += db.getCollection( "Carton" ).count();
		count += db.getCollection( "Pallet" ).count();
		return count;
	}

	@Override
	public void persist( Set<Thing> things )
	{
		// TODO Auto-generated method stub
		
	}

}
