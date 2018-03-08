package com.tierconnect.riot.commons.utils;

import org.apache.log4j.Logger;

import java.util.*;


public class TimerUtil
{
	private static final Logger logger = Logger.getLogger( TimerUtil.class );
	
	List<Long> times = new ArrayList<Long>();

	// store and take care of multiple laps
	Map<String, Long> starts = new LinkedHashMap<>();
	Map<String, Long> stops = new LinkedHashMap<>();
	Map<String, Long> accum = new LinkedHashMap<>();

	String threadName;
	private final boolean breakLine;

	public TimerUtil()
	{
		this(false);
	}

	public TimerUtil(boolean breakLine)
	{
		this.breakLine = breakLine;
		threadName = Thread.currentThread().getName();
	}

	public synchronized void mark()
	{
		//checkThreadName();
		times.add( System.currentTimeMillis() );
	}

	public synchronized long getLastDelt()
	{
		//checkThreadName();
		return times.get( times.size() - 1 ) - times.get( times.size() - 2 );
	}

	public synchronized long getTotalDelt()
	{
		//checkThreadName();
		return times.get( times.size() - 1 ) - times.get( 0 );
	}

	public synchronized void initLaps( String laps[] )
	{
		//checkThreadName();
		int size = laps.length;
		for( int i = 0; i < size; i++ )
		{
			starts.put( laps[i], 0L );
			stops.put( laps[i], 0L );
			accum.put( laps[i], 0L );
		}
	}

	public synchronized void initLaps()
	{
		//checkThreadName();
		String key;
		Iterator<String> it = accum.keySet().iterator();
		while( it.hasNext() )
		{
			key = it.next().toString();
			starts.put( key, 0L );
			stops.put( key, 0L );
			accum.put( key, 0L );
		}
	}

	public synchronized void start( String lap )
	{
		//checkThreadName();
		starts.put( lap, System.currentTimeMillis() );
	}

	public synchronized void stop( String lap )
	{
		//checkThreadName();
		stops.put( lap, System.currentTimeMillis() );
		long accumLap = accum.get( lap ) + (stops.get( lap ) - starts.get( lap ));

		accum.put( lap, accumLap );
	}

	public synchronized Long getLap( String lap )
	{
		//checkThreadName();
		return accum.get( lap );
	}

	public synchronized Long getTotal()
	{
		//checkThreadName();
		long total = 0;
		Iterator<String> it = accum.keySet().iterator();
		while( it.hasNext() )
		{
			total += accum.get( it.next() );
		}
		return total;
	}

	public synchronized String getLogString()
	{
		//checkThreadName();
		StringBuilder sb = new StringBuilder();
		long total = 0;
		Iterator<String> it = accum.keySet().iterator();
		while( it.hasNext() )
		{
			String key = (String) it.next();
			if( sb.length() != 0 )
			{
				sb.append( " " );
			}
			sb.append(addBreakLine());
			sb.append( key );
			sb.append( "=" );
			sb.append( accum.get( key ) );
			total += accum.get( key );
		}
		sb.append(addBreakLine());
		sb.append( " total=" + total );
		return sb.toString();
	}

	private String addBreakLine(){
		return (breakLine)? "\n" : "";
	}

	public synchronized String getLapsResult()
	{
		//checkThreadName();
		return null;
	}
	
//	private void checkThreadName()
//	{
//		//WE SHOW THE STACK TRACE (but we do not throw it !)
//		//Exception e = new Exception( "timerUtil is used by two different threads !" );
//		String thisName = Thread.currentThread().getName();
//		if( ! threadName.equals( thisName ) )
//		{
//			logger.warn( "Thread Name is different ! " + threadName + " "  + thisName );
//		}
//	}
	
}
