package com.tierconnect.riot.iot.dao.mongo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * 
 * POC for mongo time series
 * 
 * @author tcrown
 */

class Column
{
	String name;
	long time;
	String value;

	public void add( String fieldName, long time, String value )
	{
		this.name = fieldName;
		this.time = time;
		this.value = value;
	}
}

class Thing
{
	long time;

	long id;

	// KEY: fieldName
	Map<String, Column> columns = new TreeMap<String, Column>();

	public Thing( long time, long thingId )
	{
		this.time = time;
		this.id = thingId;
	}

	public void add( long time, String fieldName, String value )
	{
		Column c = columns.get( fieldName );
		if( c == null )
		{
			c = new Column();
			columns.put( fieldName, c );
		}

		c.add( fieldName, time, value );
	}

	public void copy( Thing previousThing )
	{
		for( Entry<String, Column> e : previousThing.columns.entrySet() )
		{
			if( !this.columns.containsKey( e.getKey() ) )
			{
				this.columns.put( e.getKey(), e.getValue() );
			}
		}
	}
}

class Row
{
	// KEY: timestamp
	Map<Long, Thing> things = new TreeMap<Long, Thing>();

	public void add( long time, long thingId, String fieldName, String value )
	{
		Thing thing = things.get( thingId );

		if( thing == null )
		{
			thing = new Thing( time, thingId );
			things.put( thingId, thing );
		}

		thing.add( time, fieldName, value );
	}
}

public class TimeSeriesReportPOC
{
	Map<Long, Row> rows = new TreeMap<Long, Row>();

	List<Thing> list = new ArrayList<Thing>();

	int maxRows = 5;

	public int mongo_document_count = 0;

	public int getMaxRows()
	{
		return maxRows;
	}

	public void setMaxRows( int maxRows )
	{
		this.maxRows = maxRows;
	}

	public void addValue( long time, long thingId, String fieldName, String value )
	{
		Row row = rows.get( time );
		if( row == null )
		{
			row = new Row();
			row.add( time, thingId, fieldName, value );
			rows.put( time, row );
		}
		else
		{
			row.add( time, thingId, fieldName, value );
		}
	}

	public int getTotalRows()
	{
		return rows.size();
	}

	public void makeNonSparse()
	{
		Map<Long, Thing> previousThings = new TreeMap<Long, Thing>();

		// TODO: check implementation !
		for( Long time : rows.keySet() )
		{
			Row r = rows.get( time );

			for( Long thingId : r.things.keySet() )
			{
				Thing thing = r.things.get( thingId );
				if( previousThings.containsKey( thingId ) )
				{
					Thing previousThing = previousThings.get( thingId );
					thing.copy( previousThing );
				}
				previousThings.put( thingId, thing );
			}
		}
	}

	public void orderBy( String[] filterBy, String[] orderBy )
	{
		this.setFilterBy( filterBy );
		MyCompare c = new MyCompare( orderBy );
		int count = 0;
		list.clear();
		for( Row row : rows.values() )
		{
			for( Thing thing : row.things.values() )
			{
				if( count < maxRows )
				{
					if( passesFilter() )
					{
						list.add( thing );
						count++;
					}
				}
				else
				{
					if( passesFilter() )
					{
						list.add( thing );
						Collections.sort( list, c );
						list.remove( list.size() - 1 );
					}
				}
			}
		}

	}

	private void setFilterBy( String[] filterBy )
	{
		// TODO Auto-generated method stub
	}

	private boolean passesFilter()
	{
		// TODO: honor filter by udfs !
		return true;
	}

	public String toJSON()
	{
		StringBuilder sb = new StringBuilder();

		sb.append( "[\n" );

		int count = 0;
		for( Thing thing : list )
		{
			if( count > 0 )
			{
				sb.append( ",\n" );
			}

			sb.append( "{ \"time\":" + thing.time + "," );
			sb.append( "\"thingId\":" + thing.id );

			for( String key : thing.columns.keySet() )
			{
				Column c = thing.columns.get( key );
				sb.append( ",\"" + c.name + "\":{\"time\":" + c.time + ",\"value\":\"" + c.value + "\"}" );
			}

			sb.append( "}" );

			count++;
		}

		sb.append( "\n]\n" );
		return sb.toString();
	}

	class MyCompare implements Comparator<Thing>
	{
		public MyCompare( String[] orderBy )
		{
			// TODO Auto-generated constructor stub
		}

		@Override
		public int compare( Thing r1, Thing r2 )
		{
			Column str1 = r1.columns.get( "city" );
			Column str2 = r2.columns.get( "city" );
			int v = str1.value.compareTo( str2.value );
			if( v == 0 )
			{
				Column str1b = r1.columns.get( "temp" );
				Column str2b = r2.columns.get( "temp" );
				Double d1 = Double.parseDouble( str1b.value );
				Double d2 = Double.parseDouble( str2b.value );
				return d1.compareTo( d2 );
			}
			else
			{
				return v;
			}

			// TODO: if value is number, compare numerically !!
		}
	}
}
