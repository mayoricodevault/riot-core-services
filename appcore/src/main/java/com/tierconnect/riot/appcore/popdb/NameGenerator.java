package com.tierconnect.riot.appcore.popdb;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import org.apache.log4j.Logger;

public class NameGenerator
{
	static Logger	logger	= Logger.getLogger( NameGenerator.class );
	Properties		lastNames, maleNames, femaleNames;
	List<String>	lname, mname, fname;

	Random			random;

	public NameGenerator()
	{
		random = new Random( 0 );
		maleNames = load( "names/names-male.properties" );
		femaleNames = load( "names/names-female.properties" );
		lastNames = load( "names/names-last.properties" );

		lname = new ArrayList( lastNames.keySet() );
		mname = new ArrayList( femaleNames.keySet() );
		fname = new ArrayList( maleNames.keySet() );
	}

	public String[] getName()
	{
		String names[] = new String[3];
		if( random.nextBoolean() )
		{
			names[0] = getName( mname );
			names[1] = getName( mname );
		}
		else
		{
			names[0] = getName( fname );
			names[1] = getName( fname );
		}
		names[2] = getName( lname );
		return names;
	}

	private Properties load( String file )
	{
		URL url = NameGenerator.class.getClassLoader().getResource( file );
		Properties p = new Properties();
		try
		{
			InputStream is = url.openStream();
			p.load( is );
			is.close();
		}
		catch (IOException e)
		{
			logger.error( "Error loading url=" + url.toExternalForm() );
		}
		return p;
	}

	private String getName( List<String> list )
	{
		int s = list.size();
		int n = random.nextInt( s );
		return (String) list.get( n );
	}
}
