package com.tierconnect.riot.iot.dao.mongo;

import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import com.tierconnect.riot.appcore.utils.Configuration;
import com.tierconnect.riot.commons.dao.mongo.MongoDAOUtil;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.tierconnect.riot.sdk.utils.TimerUtil;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.math.NumberUtils.isNumber;

/**
 * POC for mongo time series
 * 
 * @author tcrown
 */

public class TimeSeriesReportPOCDAO
{
	private static Logger logger = Logger.getLogger( TimeSeriesReportPOCDAO.class );

	static
	{
		// logger.setLevel( Level.DEBUG );
	}

	MongoDAOUtil cu = new MongoDAOUtil();

	static TimeSeriesReportPOCDAO instance;

	DBCollection timeseries;

	public static void main( String[] args ) throws UnknownHostException
	{
        MongoDAOUtil.setupMongodb(Configuration.getProperty("mongo.primary"),
                Configuration.getProperty("mongo.secondary"),
                Configuration.getProperty("mongo.replicaset"),
                Boolean.valueOf(Configuration.getProperty("mongo.ssl")),
                Configuration.getProperty("mongo.username"),
                Configuration.getProperty("mongo.password"),
                Configuration.getProperty("mongo.authdb"),
                Configuration.getProperty("mongo.db"),
                Configuration.getProperty("mongo.controlReadPreference"),
                Configuration.getProperty("mongo.reportsReadPreference"),
                Boolean.valueOf(Configuration.getProperty("mongo.sharding")),
                (isNotBlank(Configuration.getProperty("mongo.connectiontimeout")) && isNumber(Configuration
                        .getProperty("mongo.connectiontimeout"))) ? Integer.parseInt(Configuration.getProperty
                        ("mongo.connectiontimeout")) : null,
                (isNotBlank(Configuration.getProperty("mongo.maxpoolsize")) && isNumber(Configuration.getProperty
                        ("mongo.maxpoolsize"))) ? Integer.parseInt(Configuration.getProperty("mongo.maxpoolsize"))
                        : null);
		instance = TimeSeriesReportPOCDAO.getInstance();

		String nameRegex = ".*";
		String serialNumberRegex = ".*";
		int[] groupIds = new int[] { 2, 3 };
		// int[] thingTypeIds = new int[] { 1, 2, 3 };
		int[] thingTypeIds = new int[] { 6, 7, 8 };
		String[] fieldNames = new String[] { "name", "serialNumber", "groupId", "thingTypeId", "location", "zone" };

		long t1 = System.currentTimeMillis() - 48L * 60L * 60L * 1000L;
		long t2 = System.currentTimeMillis();

		String[] orderBy = new String[] { "name:1", "serialNumber:1", "groupId:1", "thingTypeId:1", "location:1", "zone:1" };

		TimeSeriesReportPOC tsr = instance.buildReport( nameRegex, serialNumberRegex, groupIds, thingTypeIds, fieldNames, t1, t2, orderBy );

		long t3 = System.currentTimeMillis();

		System.out.println( "delt=" + (t3 - t2) );
		System.out.println( "docs=" + tsr.mongo_document_count );
		System.out.println( "records=" + tsr.rows.size() );

		// tsr.printReport();
	}

	public static TimeSeriesReportPOCDAO getInstance()
	{
		if( instance == null )
		{
			instance = new TimeSeriesReportPOCDAO();
			instance.setup();
		}
		return instance;

	}

	public void setup()
	{
		timeseries = MongoDAOUtil.getInstance().db.getCollection( "timeseries" );
	}

	public TimeSeriesReportPOC buildReport( String nameRegex, String serialNumberRegex, int[] groupIds, int[] thingTypeIds,
			String[] fieldNames, long t1, long t2, String[] orderBy )
	{
		TimeSeriesReportPOC tsr = new TimeSeriesReportPOC();

		BasicDBObject query = new BasicDBObject();

		if( !StringUtils.isEmpty( nameRegex ) )
		{
			Pattern p1 = Pattern.compile( nameRegex );
			query.put( "name", p1 );
		}

		if( !StringUtils.isEmpty( serialNumberRegex ) )
		{
			Pattern p2 = Pattern.compile( serialNumberRegex );
			query.put( "serialNumber", p2 );
		}

		query.append( "groupId", new BasicDBObject( "$in", groupIds ) );

		query.append( "thingTypeId", new BasicDBObject( "$in", thingTypeIds ) );

		query.append( "fieldName", new BasicDBObject( "$in", fieldNames ) );

		// TODO IMPLEMENT THIS BELOW !!!!
		// query.append( "prevEnd", new BasicDBObject( "$lte", t1 ) );
		// query.append( "nextStart", new BasicDBObject( "$lte", t2 ) );

		// query.append( "$or" );

		System.out.println( "query=" + query );

		DBCursor cursor = timeseries.find( query );

		try
		{
			int count = 0;
			while( cursor.hasNext() )
			{
				DBObject doc = cursor.next();
				processDocument( tsr, doc, fieldNames, t1, t2 );
				if( count % 10000 == 0 )
				{
					logger.info( "count=" + count );
				}
				tsr.mongo_document_count++;
				count++;
			}
		}
		catch( Exception e )
		{
			logger.warn( "exception: ", e );
		}
		finally
		{
			cursor.close();
		}
		return tsr;
	}

	private void processDocument( TimeSeriesReportPOC tsr, DBObject doc, String[] fieldNames, long t1, long t2 )
	{
		try
		{
			BasicDBObject oid = (BasicDBObject) doc.get( "_id" );
			Long thingId = oid.getLong( "id" );

			String fieldName = doc.get( "fieldName" ).toString();

			// Long prevEnd = Long.valueOf( doc.get( "prevEnd" ).toString() );
			// Long nextStart = Long.valueOf( doc.get( "nextStart" ).toString()
			// );

			BasicDBList time = (BasicDBList) doc.get( "time" );
			BasicDBList value = (BasicDBList) doc.get( "value" );

			// System.out.println( "id=" + thingId );
			// System.out.println( "name=" + name );
			// System.out.println( "serialNumber=" + serialNumber );
			// System.out.println( "fieldName=" + fieldName );
			// System.out.println( "prevEnd=" + prevEnd );
			// System.out.println( "nextStart=" + nextStart );

			boolean flag = true;

			for( int i = time.size() - 1; i >= 0; i-- )
			{
				long t = (long) time.get( i );
				String v = String.valueOf( value.get( i ) );

				if( t > 0 && t >= t1 && t <= t2 )
				{
					// System.out.println( "id=" + thingId + " " + fieldName +
					// " time=" + t + " value='" + v + "'" );
					// add non-time series value
					if( flag == true )
					{
						addValueNonTimeSeries( tsr, doc, fieldNames, thingId, t, "name" );
						addValueNonTimeSeries( tsr, doc, fieldNames, thingId, t, "serialNumber" );
						addValueNonTimeSeries( tsr, doc, fieldNames, thingId, t, "groupId" );
						addValueNonTimeSeries( tsr, doc, fieldNames, thingId, t, "thingTypeId" );
						flag = false;
					}

					tsr.addValue( t, thingId, fieldName, v );
				}
			}
		}
		catch( Exception e )
		{
			logger.warn( "exception: ", e );
		}
	}

	private void addValueNonTimeSeries( TimeSeriesReportPOC tsr, DBObject doc, String[] fieldNames, long thingId, long t, String fieldName )
	{
		if( ArrayUtils.contains( fieldNames, fieldName ) )
		{
			String value = doc.get( fieldName ).toString();
			tsr.addValue( t, thingId, fieldName, value );
		}
	}

	public String lastValueAtAnyDate( List<Long> arrayGroupId, List<Long> arrayThingTypeId, List<String> arrayFieldnames, Long reportDate,
			String output )
	{
		String res;

		TimerUtil tu = new TimerUtil();
		tu.mark();

		DBCollection timeseriesCollection = MongoDAOUtil.getInstance().db.getCollection( "timeseries" );
		DBCollection outputCollection = MongoDAOUtil.getInstance().db.getCollection( output );

		// drop output collection
		outputCollection.drop();

		BasicDBObject query = new BasicDBObject( "groupId", new BasicDBObject( "$in", arrayGroupId ) )
				.append( "thingTypeId", new BasicDBObject( "$in", arrayThingTypeId ) )
				.append( "fieldName", new BasicDBObject( "$in", arrayFieldnames ) )
				.append( "prevEnd", new BasicDBObject( "$lt", reportDate ) ).append( "nextStart", new BasicDBObject( "$gt", reportDate ) );

		DBCursor cursor = timeseriesCollection.find( query );
		BulkWriteOperation bulkWriteOperation = outputCollection.initializeUnorderedBulkOperation();

		Long bulkOperations = 0L;
		Long totalDocs = 0L;

		String prevSerialNumber = "";
		BasicDBObject newDoc = null;

		while( cursor.hasNext() )
		{
			DBObject timeserie = cursor.next();

			// var str = [];
			String value = null;
			Long timeLastChange = null;
			Integer i;
			BasicDBList timeList = (BasicDBList) timeserie.get( "time" );
			BasicDBList valueList = (BasicDBList) timeserie.get( "value" );

			// go over the internal array to get the latest value for the date
			// specified
			i = timeList.size() - 1;
			while( i >= 0 )
			{
				if( timeList.get( i ) != null && !timeList.get( i ).equals( 0 ) )
				{
					if( value == null )
					{
						value = valueList.get( i ).toString();
						timeLastChange = Long.parseLong( timeList.get( i ).toString() );
					}
					if( timeLastChange > reportDate )
					{
						i = -1;  // exit from this loop
					}
				}
				i--;
			}

			// build the document to be stored in the output collection
			if( !prevSerialNumber.equals( timeserie.get( "serialNumber" ) ) )
			{
				if( newDoc != null )
				{
					bulkWriteOperation.insert( newDoc );
					bulkOperations++;
					totalDocs++;
					if( bulkOperations >= 1000 )
					{
						bulkWriteOperation.execute();
						bulkWriteOperation = outputCollection.initializeUnorderedBulkOperation();
						bulkOperations = 0L;
						logger.debug( totalDocs + " docs" );
					}
				}
				BasicDBObject mongoId = (BasicDBObject) timeserie.get( "_id" );
				newDoc = new BasicDBObject().append( "id", mongoId.get( "id" ) )
						.append( "thingTypeFieldId", mongoId.get( "thingTypeFieldId" ) )
						.append( "thingTypeId", timeserie.get( "thingTypeId" ) ).append( "serialNumber", timeserie.get( "serialNumber" ) );
			}
			// add the fieldName with his value and with his date
			newDoc.append( timeserie.get( "fieldName" ).toString(), value );

			newDoc.append( timeserie.get( "fieldName" ).toString() + "Date", timeLastChange );
			newDoc.append( timeserie.get( "fieldName" ).toString() + "Date2", new Date( timeLastChange ) );

			prevSerialNumber = timeserie.get( "serialNumber" ).toString();

		}
		if( newDoc != null )
		{
			bulkWriteOperation.insert( newDoc );
			bulkOperations++;
			totalDocs++;
		}
		if( bulkOperations > 0 )
		{
			bulkWriteOperation.execute();
		}
		tu.mark();
		logger.info( totalDocs + " docs" );
		logger.info( "output collection: " + output );

		res = "time: " + tu.getLastDelt() + " ms\ntotal docs: " + totalDocs + "\noutput collection: " + output + "\ntimestamp:"
				+ reportDate.toString() + "\ndate: " + new Date( reportDate );

		return res;
	}

}
