package com.tierconnect.riot.datagen;

import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.Random;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.entities.ThingTypeField;

/**
 * Quick dirty data generator
 *
 * Generates generates a 4 level parent child.
 *
 * Asset -> Box -> Carton -> Pallet
 */
public class DirtyDataGen
{
	static Logger logger = Logger.getLogger( DirtyDataGen.class );

	Options options;
	
	CommandLine line;
	
	int dataSize;
	int childSize;
	int udfSize;
	int groupSize;
	String db;
	String dbname;
	boolean drop;
	boolean isTimeSeries;

	long ncount;
	long incount;
	String host;

	DecimalFormat formatter;

	Persister persister;

	public static void main( String[] args ) throws Exception
	{
		DirtyDataGen ddg = new DirtyDataGen();
		ddg.options();
		ddg.parse( args );
		ddg.init();
		ddg.generate();
	}

	public void options()
	{
		options = new Options();

		options.addOption( "n", true, "number of top level things to create" );
		options.addOption( "c", true, "children for each thing" );
		options.addOption( "d", true, "database to persist to. options: 'mongo', 'mysql'" );
		options.addOption( "u", true, "number of udfs per thing" );
		options.addOption( "g", true, "number of groups to generate" );
		options.addOption( "h", true, "host db to connect" );
		options.addOption( "db", true, "dbname" );
		options.addOption( "ts", true, "generate times series data (default is lastValue" );
		options.addOption( "drop", false, "drop the collections first" );
	}
	
	public void parse( String[] args )
	{
		CommandLineParser parser = new BasicParser();

		try
		{
			// parse arguments
			line = parser.parse( options, args );

			dataSize = line.hasOption( 'n' ) ? Integer.parseInt( line.getOptionValue( 'n' ) ) : 50000;
			// number of children
			childSize = line.hasOption( 'c' ) ? childSize = Integer.parseInt( line.getOptionValue( 'c' ) ) : 5;
			db = line.hasOption( 'd' ) ? line.getOptionValue( 'd' ) : "mongo";
			// number of udf to create
			udfSize = line.hasOption( 'u' ) ? Integer.parseInt( line.getOptionValue( 'u' ) ) : 5;
			// db host
			host = line.hasOption( 'h' ) ? line.getOptionValue( 'h' ) : "localhost";
			groupSize = line.hasOption( 'g' ) ? Integer.parseInt( line.getOptionValue( 'g' ) ) : 10;
			dbname = line.hasOption( "db" ) ? line.getOptionValue( "db" ) : "riot_main";
			drop = line.hasOption( "drop" ) ? true : false;
			isTimeSeries = line.hasOption( "ts" ) ? true : false;
		}
		catch( ParseException | NumberFormatException exp )
		{
			// oops, something went wrong
			System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "java " + this.getClass().getName(), options );
			System.exit( 1 );
		}
	}

	public void init() throws UnknownHostException
	{
		if( db.equals( "mongo" ) )
		{
			if( !isTimeSeries )
			{
				throw new Error( "time series mongo has no imp yes!" );
			}
			else
			{
				persister = new MongoPersister( host, 27017, dbname, null, null, "admin", "control123!", drop );
			}
		}
		else if( db.equals( "mysql" ) )
		{
			if( !isTimeSeries )
			{
				persister = new SQLPersister();
			}
			else
			{
				persister = new SQLPersisterLV();
			}
		}

		if( !drop )
		{
			ncount = persister.getMaxId();
			incount = ncount;
			IdGenerator.setId( ncount + 1 );
		}
		else
		{
			ncount = 0;
		}

		formatter = new DecimalFormat( "###,###,###,###" );

		logger.info( "Number of things to create " + formatter.format( dataSize ) + " with " + childSize + " children each. Persister: "
				+ persister.getClass().getName() + ". Udfs " + udfSize + ". Groups " + groupSize + "." );
	}

	public void generate() throws Exception
	{
		logger.info( "Data generation start" );

		ThingTypeField number = Utils.getThingTypeField( "number" );
		ThingTypeField color = Utils.getThingTypeField( "color" );
		ThingTypeField mood = Utils.getThingTypeField( "mood" );
		ThingTypeField udf4 = Utils.getThingTypeField( "udf4" );
		ThingTypeField udf5 = Utils.getThingTypeField( "udf5" );

		ThingType ttPallet = Utils.getThingType( "Pallet", new ThingTypeField[] { number, color, mood, udf4, udf5 } );
		ThingType ttCarton = Utils.getThingType( "Carton", new ThingTypeField[] { number, color, mood, udf4, udf5 } );
		ThingType ttBox = Utils.getThingType( "Box", new ThingTypeField[] { number, color, mood, udf4, udf5 } );
		ThingType ttAsset = Utils.getThingType( "Asset", new ThingTypeField[] { number, color, mood, udf4, udf5 } );

		persister.start();

		DateGenerator dg = new DateGenerator( dataSize );

		TimerUtil tu = new TimerUtil();
		tu.start();

		for( int i = 0; i < dataSize && ncount < dataSize; i++ )
		{
			int groupId = new Random().nextInt( groupSize ) + 1;

			Thing pallet = new Thing( ttPallet, groupId, null, dg );
			persist( pallet );

			for( int j = 0; j < childSize; j++ )
			{
				Thing carton = new Thing( ttCarton, groupId, pallet, dg );
				persist( carton );

				for( int k = 0; k < childSize; k++ )
				{
					Thing box = new Thing( ttBox, groupId, carton, dg );
					persist( box );

					for( int l = 0; l < childSize; l++ )
					{
						Thing asset = new Thing( ttAsset, groupId, box, dg );
						persist( asset );
					}
				}
			}

			if( tu.step( incount, ncount, dataSize ) )
			{
				System.out.print( String.format( "%s %.1f %.1f %s\n", formatter.format( ncount ), tu.percent, tu.rate, tu.etas ) );
			}
		}

		persister.end();

		logger.info( "Data generation ended in " + ((System.currentTimeMillis() - tu.start) / 1000) + " secs." );
	}

	protected void persist( Thing box )
	{
		persister.persist( box );
		ncount++;
	}
}
