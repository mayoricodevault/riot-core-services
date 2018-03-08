package com.tierconnect.riot.sdk.utils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

public class TimerUtil2
{
	private static Logger logger = Logger.getLogger( TimerUtil2.class );

	Map<String, Long> totalTimes = new LinkedHashMap<String, Long>();

	Map<String, Long> lastStart = new HashMap<String, Long>();

	public void mark( String key )
	{
		long now = System.currentTimeMillis();

		if( !totalTimes.containsKey( key ) )
		{
			totalTimes.put( key, 0L );
		}

		long totalTime = totalTimes.get( key );

		if( lastStart.containsKey( key ) )
		{
			long t0 = lastStart.get( key );
			long delt = now - t0;
			totalTimes.put( key, totalTime + delt );
			lastStart.remove( key );
		}
		else
		{
			lastStart.put( key, now );
		}
	}

	public void reset()
	{
		totalTimes = new HashMap<String, Long>();
		lastStart = new HashMap<String, Long>();
	}

	public void log( int length )
	{
		logger.info( "--------------------------------------------" );

		String format = "%-" + length + "s %5d ms";

		for( Entry<String, Long> e : totalTimes.entrySet() )
		{
			logger.info( String.format( format, e.getKey(), e.getValue() ) );
		}
	}
}
