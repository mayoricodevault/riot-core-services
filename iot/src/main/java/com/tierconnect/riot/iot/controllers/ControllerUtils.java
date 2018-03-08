package com.tierconnect.riot.iot.controllers;

import org.apache.log4j.Logger;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ControllerUtils
{
	static final Logger logger = Logger.getLogger( ControllerUtils.class );

	static ScriptEngine engine;

	static final String sdft = "yyyy-MM-dd HH:mm:ss.SSS z";

	static
	{
		ScriptEngineManager engineManager = new ScriptEngineManager();
		engine = engineManager.getEngineByName( "nashorn" );
	}

	static public void main( String[] args )
	{
		System.out.println( "hello world" );
		System.out.println( "hello world" );
	}

	// now
	// now-1s
	// now-1m
	// now-1h
	// now-1d
	static public long getTimestamp( String in ) throws ScriptException
	{
		String timestr = in;

		long now = System.currentTimeMillis();

		timestr = timestr.replaceAll( "now", "" + now );

		timestr = timestr.replaceAll( "([0-9]+)s", "$1*1000" );
		timestr = timestr.replaceAll( "([0-9]+)m", "$1*60*1000" );
		timestr = timestr.replaceAll( "([0-9]+)h", "$1*60*60*1000" );
		timestr = timestr.replaceAll( "([0-9]+)d", "$1*24*60*60*1000" );

		logger.debug( "in='" + in + "'" );
		logger.debug( "out='" + timestr + "'" );

		try
		{
			String out = engine.eval( timestr ).toString();
			logger.debug( "eval='" + out + "'" );
			long timestamp = Long.parseLong( out );

			logger.debug( "date=" + new SimpleDateFormat(sdft).format( new Date( timestamp ) ) );

			return timestamp;

		}
		catch( ScriptException e )
		{
			throw e;
		}

	}

	static public String arrayToString( String[] a )
	{
		if( a == null )
			return null;

		StringBuffer sb = new StringBuffer();
		sb.append( "[ " );
		for( int i = 0; i < a.length; i++ )
		{
			if( i > 0 )
				sb.append( ", " );
			sb.append( "'" + a[i] + "'" );
		}
		sb.append( " ]" );
		return sb.toString();
	}

	public static String arrayToString( int[] a )
	{
		if( a == null )
			return null;

		StringBuffer sb = new StringBuffer();
		sb.append( "[ " );
		for( int i = 0; i < a.length; i++ )
		{
			if( i > 0 )
				sb.append( ", " );
			sb.append( a[i] );
		}
		sb.append( " ]" );
		return sb.toString();
	}

	public static int[] getIntegerArray( String str )
	{
		if( str == null )
			return null;

		String[] tokens = str.split( "," );
		int[] a = new int[tokens.length];
		for( int i = 0; i < tokens.length; i++ )
		{
			a[i] = Integer.parseInt( tokens[i].trim() );
		}
		return a;
	}

	public static String[] getStringArray( String str )
	{
		if( str == null )
			return null;

		String[] tokens = str.split( "," );

		String[] a = new String[tokens.length];
		for( int i = 0; i < tokens.length; i++ )
		{
			a[i] = tokens[i].trim();
		}
		return a;
	}

	public static List<Long> getLongArrayList( String str )
	{
		if( str == null )
		{
			return null;
		}

		String[] tokens = str.split( "," );
		List<Long> a = new ArrayList<>(tokens.length);

		for( int i = 0; i < tokens.length; i++ )
		{
			a.add( Long.parseLong( tokens[i].trim() ) );
		}
		return a;
	}

	public static List<String> getStringArrayList( String str )
	{
		if( str == null )
		{
			return null;
		}

		String[] tokens = str.split( "," );

		List<String> a = new ArrayList<>(tokens.length);
		for( int i = 0; i < tokens.length; i++ )
		{
			a.add(tokens[i].trim());
		}
		return a;
	}

	public static Long getISODate( String dateStr)
	{
		//check if the date is a timestamp
		try {
			Long timestamp =  Long.parseLong( dateStr );
			if ( timestamp > 1400000000000L )
			{
				return Long.parseLong( dateStr );
			}
			else
			{
				return null;
			}
		} catch( NumberFormatException e )
		{
			//the date follows the ISO format  YYYY-MM-DDTHH:MM:SSZ   the timezone is the GMT timezone
			String[] tokens = dateStr.split( "T" );
			//validate the string has the date and the time
			if ( tokens.length != 2) {
				return null;
			}

			String[] dates = tokens[0].split( "-" );
			//validate the date string has the three parts, year, month, day
			if ( dates.length != 3) {
				return null;
			}

			String[] times = tokens[1].split( ":" );
			//validate the time string has the three parts, year, month, day
			if ( times.length != 3) {
				return null;
			}

			String[] secs = times[2].split( "Z" );
			//validate the time string has the Z at the end
			if ( secs.length != 1) {
				return null;
			}

			int year   = Integer.parseInt( dates[0] );
			int month  = Integer.parseInt( dates[1] ) -1;
			int day    = Integer.parseInt( dates[2] );
			int hour   = Integer.parseInt( times[0] );
			int minute = Integer.parseInt( times[1] );
			int second = Integer.parseInt( secs[0] );

			Calendar c = Calendar.getInstance();
			c.set( year, month, day, hour, minute, second );

			return c.getTime().getTime();
		}
	}

}
