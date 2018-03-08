package com.tierconnect.riot.iot.kafkastreams.cacheloader;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

public class ClassLoaderToolArgsNew
{
	private static final Logger logger = Logger.getLogger( ClassLoaderToolArgsNew.class );

	public List<String> include;

	public List<String> exclude;

	public boolean dl = true;

	public boolean pk = true;

	String outdir;

	public String servers;

	public String code;

	public void init( String args[] )
	{
		Options options = new Options();

		options.addOption( "i", true, "CSV list of tables to include" );
		options.addOption( "e", true, "CSV list of tables to exclude" );
		options.addOption( "help", false, "show this help" );
		options.addOption( "k", true, "kafka bootstrap servers" );

		options.addOption( "dl", false, "only down load the data and write to files on disk" );
		options.addOption( "pk", false, "only read from files on disk and publish to kakfa" );

		//options.addOption( "diff", false, "check the diff of the cache" );

		options.addOption( "d", true, "output directory (defaults to /var/tmp/cacheLoader)" );

		CommandLineParser parser = new BasicParser();
		CommandLine line = null;

		try
		{
			line = parser.parse( options, args );
		}
		catch( ParseException exp )
		{
			logger.error( "error", exp );
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( " ", options );
			System.exit( 1 );
		}

		if( line.hasOption( "h" ) )
		{
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( " ", options );
			System.exit( 1 );
		}

		// Preconditions.checkState( line.hasOption( "c" ), "Missing -c argument for the bridgeCode !" );

		servers = line.hasOption( "k" ) ? line.getOptionValue( "k" ) : "localhost:9092";

		outdir = line.hasOption( "d" ) ? line.getOptionValue( "d" ) : "/var/tmp/cacheLoader";

		if( line.hasOption( "i" ) )
		{
			String[] str = line.getOptionValue( "i" ).split( "," );
			include = new ArrayList<String>();
			for( String s : str )
			{
				include.add( s.trim() );
			}
		}

		if( line.hasOption( "dl" ) )
		{
			pk = false;
		}

		if( line.hasOption( "pk" ) )
		{
			dl = false;
		}
	}

	public boolean contains( String table )
	{
		if( include == null && exclude == null )
		{
			return true;
		}

		if( include != null && exclude == null )
		{
			return include.contains( table );
		}

		if( include == null && exclude != null )
		{
			return !exclude.contains( table );
		}

		return false;
	}
}
