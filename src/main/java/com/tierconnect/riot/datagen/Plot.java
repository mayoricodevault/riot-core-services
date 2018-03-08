package com.tierconnect.riot.datagen;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.Charsets;


public class Plot
{
	String dataDirName;
	int maxx;
	int maxy;
	
	Map<Long, Integer> map = new HashMap<Long, Integer>();
	int count = 1;

	static public void main( String[] args ) throws FileNotFoundException, IOException
	{
		Plot p = new Plot();
		p.parse( args );
		p.run();
	}

	public void parse( String[] args )
	{
		Options options = new Options();
		
		options.addOption( "dir", true, "data directory" );
		options.addOption( "maxx", true, "max x value" );
		options.addOption( "maxy", true, "max y value" );
		
		CommandLineParser parser = new BasicParser();

		try
		{
			CommandLine line = parser.parse( options, args );
			dataDirName = line.hasOption( "dir" ) ? line.getOptionValue( "dir" ) : "data/default";
			maxx = line.hasOption( "maxx" ) ? Integer.parseInt( line.getOptionValue( "maxx" ) ): 0;
			maxy = line.hasOption( "maxy" ) ? Integer.parseInt( line.getOptionValue( "maxy" ) ): 0;
		}
		catch( ParseException | NumberFormatException exp )
		{
			System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "java " + this.getClass().getName(), options );
			System.exit( 1 );
		}
	}
	
	public void run() throws FileNotFoundException, IOException
	{
		StringBuffer sb = new StringBuffer();

		Metric[] ma = read();

		TreeSet<Integer> names = getNames( ma );

		for( Integer name : names )
		{
			sb.append( "# NAME: " + name + "\n" );
			List<Metric> mlist = getMetricByNames( ma, name );
			for( Metric m : mlist )
			{
				sb.append( String.format( "%d %f %d\n", m.numberOfRecords, m.time, func( m.id ) ) );
			}
			sb.append( "\n" );
			sb.append( "\n" );
		}

		File dir = new File( dataDirName );
		File file1 = new File( dir, "plot.dat" );
		File file2 = new File( dir, "plot.def" );
		
		System.out.println( "writing to " + file1.getAbsolutePath() );
		PrintWriter out = new PrintWriter( new OutputStreamWriter( new FileOutputStream( file1, false ),Charsets.UTF_8 ) );
		out.println( sb.toString() );
		out.close();
		
		System.out.println( "writing to " + file2.getAbsolutePath() );
		out = new PrintWriter( new OutputStreamWriter( new FileOutputStream( file2, false ),Charsets.UTF_8 ) );
		out.println( buildPlotDef( names.size() ) );
		out.close();
	}

	private List<Metric> getMetricByNames( Metric[] ma, Integer name )
	{
		List<Metric> list = new ArrayList<Metric>();
		for( int i = 0; i < ma.length; i++ )
		{
			if( name == func( ma[i].id ) )
			{
				list.add( ma[i] );
			}
		}
		return list;
	}

	private TreeSet<Integer> getNames( Metric[] ma )
	{
		TreeSet<Integer> ts = new TreeSet<Integer>();
		for( int i = 0; i < ma.length; i++ )
		{
			ts.add( func( ma[i].id ) );
		}
		return ts;
	}

	private int func( long id )
	{
		if( !map.containsKey( id ) )
		{
			map.put( id, count++ );
		}
		return map.get( id );
	}

	public Metric[] read() throws FileNotFoundException, IOException
	{
		File dir = new File( dataDirName );
		File file = new File( dir, "output.json" );

		System.out.println( "reading from " + file.getAbsolutePath() );
		
		StringBuffer sb = new StringBuffer();

		try(BufferedReader br = new BufferedReader( new InputStreamReader (new FileInputStream( file), Charsets.UTF_8) ) )
		{
			String line;
			while( (line = br.readLine()) != null )
			{
				sb.append( line );
			}
		}

		String str = sb.toString();
		String str2 = str.substring( 0, str.length() - 1 );
		ObjectMapper mapper = new ObjectMapper();
		Metric[] ma = mapper.readValue( "[" + str2 + "]", Metric[].class );

		//System.out.println( "ma=" + ma.length );

		return ma;
	}

	public String buildPlotDef( int size )
	{
		StringBuffer sb = new StringBuffer();

		sb.append( "set terminal png size 1200,800\n" );
		sb.append( "set format x \"%8.0f\"\n" );
		
		if( maxx > 0 )
		{
			sb.append( String.format( "set xrange [0:%d]\n", maxx ) );
		}
		
		if( maxy > 0 )
		{
			sb.append( String.format( "set yrange [0:%d]\n", maxy ) );
		}
		
		//set format x "%8.0f"
		// set terminal png size 1200,800
		//
		// #plot "plot.dat" with points palette
		// #plot "plot.dat" with points
		// #plot "plot.data" using [1:2] with points
		//
		//
		// set style line 1 lc rgb '#0060ad' lt 1 lw 2 pt 7 ps 1.5 # --- blue
		// set style line 2 lc rgb '#dd181f' lt 1 lw 2 pt 5 ps 1.5 # --- red

		//
		// plot 'plot.dat' index 0 with linespoints,\
		// 'plot.dat' index 1 with linespoints,\
		// 'plot.dat' index 2 with linespoints,\
		
		for( int i = 0; i < size; i++ )
		{
			sb.append( i > 0 ? ",\\\n" : "" );
			sb.append( i == 0 ? "plot" : "   " );
			sb.append( " 'plot.dat' index " + i + " with linespoints" );
		}
		sb.append( "\n" );
		
		return sb.toString();
	}
}
