package com.tierconnect.riot.iot.dao.mongo;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TimeSeriesReportPOCGenMongoData
{

	Random r = new Random();

	String[] cities = { "Dallas", "Ft. Worth", "Plymouth", "Anchorage", "Boulder", "Munich" };

	/**
	 * @param args
	 */
	public static void main( String[] args )
	{
		int numThings = Integer.parseInt( args[0] );
		new TimeSeriesReportPOCGenMongoData().genData( numThings );
	}

	// {
	// "_id" : {
	// "id" : NumberLong(1),
	// "thingTypeFieldId" : NumberLong(1),
	// "segment" : NumberLong(1)
	// },
	// "thingTypeId" : NumberLong(1),
	// "groupId" : NumberLong(3),
	// "name" : "001",
	// "serialNumber" : "001",
	// "fieldName" : "temp",
	// "prevEnd" : NumberLong(0),
	// "nextStart" : NumberLong(20),
	// "time" : [ NumberLong("1"), NumberLong("4"), NumberLong("7"),
	// NumberLong("10"), NumberLong("13"), NumberLong("16"), NumberLong("19"),
	// NumberLong("22") ],
	// "value" : [ 78, 79, 80, 81, 74, 72, 70, 73 ]
	// }

	class Thing
	{
		long thingId;

		String name;

		String serialNumber;

		public int thingTypeFieldId;

		public int segment;

		public int thingTypeId;

		public int groupId;

		public String fieldName;

		public long prevEnd;

		public long nextStart;

		List<Long> times = new ArrayList<Long>();

		List<String> values = new ArrayList<String>();
	}

	public void genData( int numThings )
	{
		String[] fieldNames = { "temp", "city" };

		PrintStream ps = System.out;

		ps.println( "db.getCollection('timeseries').drop()\n" );
		// ps.println( "db.getCollection('timeseries').insert(\n" );
		// ps.println( "[\n" );

		int c = 0;
		for( int i = 0; i < numThings; i++ )
		{
			for( int j = 0; j < 2; j++ )
			{
				ps.println( "db.getCollection('timeseries').insert(" );
				ps.println( "[" );

				Thing thing = new Thing();

				thing.thingId = i;
				String name = String.format( "%08d", i );
				thing.name = name;
				thing.serialNumber = name;

				thing.thingTypeFieldId = j;
				thing.segment = c++;
				thing.thingTypeId = 1;
				thing.groupId = 1;

				thing.fieldName = fieldNames[j];

				thing.prevEnd = 0;
				thing.nextStart = 0;

				for( int k = 0; k < 50; k++ )
				{
					thing.times.add( k, (long) k );
					thing.values.add( k, getData( thing.fieldName) );
				}

				String str = getString( thing );

				ps.println( str );

				// if( c > 0 )
				// ps.println( ",\n" );

				ps.println( "]" );
				ps.println( ")\n" );
			}
		}

		// ps.println( "]\n" );
		// ps.println( ")\n" );
	}

	private String getData( String fieldname )
	{
		switch( fieldname )
		{

			case "city":
				return cities[r.nextInt( cities.length )];

			case "temp":
				return "" + r.nextInt( 100 );

		}

		return "";
	}

	private String getString( Thing t )
	{
		StringBuffer sb = new StringBuffer();

		sb.append( "{\n" );

		sb.append( "\"_id\" : { \"id\" : NumberLong(" + t.thingId + "), \"thingTypeFieldId\" : NumberLong(" + t.thingTypeFieldId
				+ "), \"segment\" : NumberLong(" + t.segment + ") }\n" );
		sb.append( ",\"thingTypeId\" : NumberLong(" + t.thingTypeId + ")\n" );
		sb.append( ",\"groupId\" : NumberLong(" + t.groupId + ")\n" );
		sb.append( ",\"name\" : \"" + t.name + "\"\n" );
		sb.append( ",\"serialNumber\" : \"" + t.serialNumber + "\"\n" );
		sb.append( ",\"fieldName\" : \"" + t.fieldName + "\"\n" );
		sb.append( ",\"prevEnd\" : NumberLong(" + t.prevEnd + ")\n" );
		sb.append( ",\"nextStart\" : NumberLong(" + t.nextStart + ")\n" );

		sb.append( ",\"time\" : [\n" );
		for( int i = 0; i < t.times.size(); i++ )
		{
			if( i > 0 )
				sb.append( "," );
			sb.append( "NumberLong(" + t.times.get( i ) + ")" );
		}
		sb.append( "\n]\n" );

		sb.append( ",\"value\" : [\n" );
		for( int i = 0; i < t.values.size(); i++ )
		{
			if( i > 0 )
				sb.append( "," );
			//sb.append( "NumberLong(" + t.values.get( i ) + ")" );
			sb.append( "\"" + t.values.get( i ) + "" );
		}
		sb.append( "\n]\n" );

		sb.append( "}\n" );

		return sb.toString();
	}
}
