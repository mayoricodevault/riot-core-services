package com.tierconnect.riot.sdk.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

public class Version
{
	static Logger logger = Logger.getLogger( Version.class );
	
	static Properties p;

	static 
	{
		ClassLoader classloader = Version.class.getClassLoader();
		InputStream is = classloader.getResourceAsStream( "git.properties" );
		//logger.info( "is=" + is );
		if( is != null )
		{
			try
			{
				p = new Properties();
				p.load( is );
			}
			catch( IOException e )
			{
				e.printStackTrace();
			}
		}
//		for( Object key : p.keySet() )
//		{
//			String str = "key=" + key + " value=" + p.getProperty( (String) key );
//			logger.info( str );
//			System.out.println( str );
//			System.err.println( str );
//		}
	}

	static public String getVersion()
	{
		if( p != null )
		{
			return p.getProperty( "serial" );
		}
		else
		{
			return null;
		}
	}
	
	static public String getBranch()
	{
		if( p != null )
		{
			return p.getProperty( "branch" );
		}
		else
		{
			return null;
		}
	}
}
