package com.tierconnect.riot.iot.services;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.tierconnect.riot.commons.dao.mongo.MongoDAOUtil;

import java.util.Random;

/**
 * Created by fernando on 11/10/15.
 */
public class TestRestEndpointService
{
	private static TestRestEndpointService instance = new TestRestEndpointService();

	public static TestRestEndpointService getInstance()
	{
		return instance;
	}

	public String httpThreadToMongo( String method, String serialNumber )
	{
		DBCollection httpPoolTest = MongoDAOUtil.getInstance().db.getCollection( "httpPoolTest" );
		BasicDBObject queryDoc  = new BasicDBObject( "method", method ).append("serialNumber", serialNumber);
		BasicDBObject updateDoc = new BasicDBObject( "$inc", new BasicDBObject( "count", 1 ) );

		DBObject res = httpPoolTest.findAndModify( queryDoc, new BasicDBObject(), null, false, updateDoc, true, true );

		//wait between 0 to 100 miliseconds, before complete this request
		try
		{
			Random r = new Random();
			Thread.sleep( r.nextInt( 100 ) );
		}
		catch( InterruptedException e )
		{

		}


		return res.toString();

	}
}
