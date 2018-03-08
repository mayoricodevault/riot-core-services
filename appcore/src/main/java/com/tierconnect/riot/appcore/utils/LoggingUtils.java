package com.tierconnect.riot.appcore.utils;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class LoggingUtils
{
	private static Logger logger = Logger.getLogger( LoggingUtils.class );

	static Map<String, Object> map = new HashMap<String, Object>();

	static
	{
		map.put( "error", Level.ERROR );
		map.put( "info", Level.INFO );
		map.put( "warn", Level.WARN );
		map.put( "debug", Level.DEBUG );
		map.put( "trace", Level.TRACE );

		map.put( "ERROR", Level.ERROR );
		map.put( "INFO", Level.INFO );
		map.put( "WARN", Level.WARN );
		map.put( "DEBUG", Level.DEBUG );
		map.put( "TRACE", Level.TRACE );
	}

	static public void setLoggingLevel( String body )
	{
		String[] lines = body.split( "\n" );
		for( String line : lines )
		{
			String[] m = line.split( "=" );
			String name = m[0].trim();
			String slevel = m[1].trim();
			Level level = (Level) map.get( slevel );
			if( level == null )
			{
				logger.warn( "unknown log4j logger level '" + slevel + "'" );
			}
			else
			{
				Logger l = LogManager.getLogger( name );
				if( l == null )
				{
					logger.warn( "unknown logger '" + name + "'" );
				}
				else
				{
					logger.info( "setting log4j logger " + name + "=" + slevel );
					l.setLevel( level );
				}
			}
		}
	}

	static public Map<String, Logger> getLoggers()
	{
		Map<String, Logger> smap = new TreeMap<String, Logger>();

		Enumeration<?> e = LogManager.getCurrentLoggers();
		while( e.hasMoreElements() )
		{
			Logger o = (org.apache.log4j.Logger) e.nextElement();
			smap.put( o.getName(), o );
		}

		return smap;
	}


}
