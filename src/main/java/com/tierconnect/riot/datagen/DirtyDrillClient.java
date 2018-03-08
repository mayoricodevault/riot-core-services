package com.tierconnect.riot.datagen;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.Charsets;
import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Created by pablo on 2/29/16.
 *
 * Client to run a drill query from the command line
 */
public class DirtyDrillClient
{
	static Logger logger = Logger.getLogger( DirtyDataGen.class );

	DrillDAO drillDAO;

	Connection connection;

	// eg. mongo or mysql
	String db;

	// eg. riot_main, etc.
	String dbname;

	boolean runTimesSeries;

	int count;

	String drill;

	String dataDirName;
	
	PrintWriter out;

	public static void main( String[] args ) throws Exception
	{
		DirtyDrillClient ddc = new DirtyDrillClient();
		ddc.parse( args );
		ddc.init();
		ddc.run();
	}

	public void parse( String[] args )
	{
		Options options = new Options();
		
		options.addOption( "dr", true, "drill url" );
		options.addOption( "db", true, "dbname" );
		options.addOption( "d", true, "database to persist to. Options: 'mongo', 'mysql'" );
		options.addOption( "t", false, "run time series queries" );
		options.addOption( "dir", true, "data directory" );
		
		CommandLineParser parser = new BasicParser();

		try
		{
			CommandLine line = parser.parse( options, args );

			db = line.hasOption( 'd' ) ? line.getOptionValue( "d" ) : "mongo";
			dbname = line.hasOption( "db" ) ? line.getOptionValue( "db" ) : "riot_main";
			runTimesSeries = line.hasOption( "t" ) ? true : false;
			drill = line.hasOption( "dr" ) ? line.getOptionValue( "dr" ) : "jdbc:drill:zk=127.0.0.1:2181/drill/drillbits1;schema=mongo";
			dataDirName = line.hasOption( "dir" ) ? line.getOptionValue( "dir" ) : "data/default";
		}
		catch( ParseException | NumberFormatException exp )
		{
			System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "java " + this.getClass().getName(), options );
			System.exit( 1 );
		}
	}

	public void init() throws ClassNotFoundException, SQLException, IOException
	{
		Class.forName( "org.gjt.mm.mysql.Driver" );
		Class.forName( "org.apache.drill.jdbc.Driver" );

		if( db.equals( "mongo" ) )
		{
			logger.info( "using MONGO connection !" );
			System.out.println( "drill=" + drill );
			connection = DriverManager.getConnection( drill );
		}
		else if( db.equals( "mysql" ) )
		{
			logger.info( "using MYSQL connection !" );
			connection = DriverManager.getConnection( "jdbc:mysql://localhost:3306/" + dbname, "root", "control123!" );
		}

		drillDAO = new DrillDAO( connection, db, dbname );

		File dir = new File( dataDirName );
		if( ! dir.exists() )
		{
			dir.mkdirs();
		}
		File outfile = new File( dir, "output.json" );
		out = new PrintWriter( new OutputStreamWriter( new FileOutputStream( outfile, true ), Charsets.UTF_8) );
	}

	public void run() throws SQLException, java.text.ParseException
	{
		count = 0;

		go( getQuery( 0, 0, 0, 0 ) );
		// go( getQuery( 0, 1, 1, 1 ) );

		go( getQuery( 1, 0, 0, 0 ) );
		go( getQuery( 1, 1, 1, 1 ) );

		go( getQuery( 2, 0, 0, 0 ) );
		go( getQuery( 2, 1, 1, 1 ) );

		go( getQuery( 3, 0, 0, 0 ) );
		go( getQuery( 3, 1, 1, 1 ) );

		out.close();
	}

	public ResultSet go( String query ) throws SQLException
	{
		Statement st = connection.createStatement();
		// change DB
		// st.setQueryTimeout( 120 );

		Metric m = new Metric();
		m.id = query.hashCode();
		m.query = query;
		m.db = db;
		m.numberOfRecords = drillDAO.count();

		ResultSet rs;

		if( db.equals( "mongo" ) )
		{
			rs = st.executeQuery( "USE mongo." + dbname );
		}

		// tranform drill SQL (for mongo) into standard SQL
		if( db.equals( "mysql" ) )
		{
			query = query.replace( "`", "" );
			query = query.replace( "_id", "id" );
			query = query.replace( ".value", "" );
			query = query.replace( "parent", "parentId" );
		}

		String letter = Character.toString( (char) (65 + count) );

		System.out.print( "\n" );
		System.out.println( "QUERY " + letter + " (" + query.hashCode() + ") : " + query );

		double start = System.currentTimeMillis();

		rs = st.executeQuery( query );

		while( rs.next() )
		{
			ResultSetMetaData md = rs.getMetaData();
			int c = md.getColumnCount();
			for( int i = 0; i < c; i++ )
			{
				if( i > 0 )
				{
					System.out.print( " | " );
				}
				System.out.print( rs.getString( 1 + i ) );
			}
			System.out.print( "\n" );
		}

		double time = ((System.currentTimeMillis() - start) / 1000.0);
		m.time = time;
		System.out.println( "Total execution time " + time + "s" );

		try
		{
			ObjectMapper mapper = new ObjectMapper();
			String str = mapper.writeValueAsString( m );
			out.println( str + "," );
			out.flush();
		}
		catch( IOException e )
		{
			e.printStackTrace();
		}

		count++;

		return rs;
	}

	//
	private String getQuery( int depth, int numfilter, int numsort, int offset ) throws java.text.ParseException
	{
		boolean where = false;

		StringBuffer sb = new StringBuffer();

		switch( depth )
		{
		case 0:
			sb.append( "select t1.`_id`, t1.`name`" );
			sb.append( " from Asset as t1" );
			break;
		case 1:
			sb.append( "select t1.`_id`, t1.`name`, t2.`name`" );
			sb.append( " from Asset as t1, Box as t2" );
			sb.append( " where t1.`parent` = t2._id" );
			where = true;
			break;
		case 2:
			sb.append( "select t1.`_id`, t1.`name`, t2.`name`, t3.`name`" );
			sb.append( " from Asset as t1, Box as t2, Carton as t3" );
			sb.append( " where t1.`parent` = t2._id and t2.`parent` = t3._id" );
			where = true;
			break;
		case 3:
			sb.append( "select t1.`_id`, t1.`name`, t2.`name`, t3.`name`, t4.`name`" );
			sb.append( " from Asset as t1, Box as t2, Carton as t3, Pallet as t4" );
			sb.append( " where t1.`parent` = t2._id and t2.`parent` = t3._id and t3.`parent` = t4._id" );
			where = true;
			break;
		}

		if( numfilter > 0 )
		{
			sb.append( (where ? " and " : " where") + " t1.`color`.`value` = 'red'" );
			where = true;
		}

		if( numfilter > 1 )
			sb.append( " and t1.`mood`.`value` = 'sad'" );

		if( runTimesSeries )
		{
			SimpleDateFormat sdf = new SimpleDateFormat( "yyyy/MM/dd" );
			Date epoch = sdf.parse( "2016/01/01" );
			Date start = DateUtils.addDays( epoch, 10 );
			Date end = DateUtils.addDays( start, 10 );
			sb.append( (where ? " and " : " where") + " t1.`timestamp` > " + start.getTime() + " and t1.`timestamp` < " + end.getTime() );
		}

		if( numsort > 0 )
			sb.append( " order by t1.`name`" );

		if( numsort > 1 )
			sb.append( " order by t1.`_id`" );

		sb.append( " limit 15 offset " + offset );

		return sb.toString();
	}
}
