package com.tierconnect.riot.datagen;

import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.tierconnect.riot.iot.entities.ThingField;
import com.tierconnect.riot.iot.entities.ThingTypeField;

/**
 * Quick dirty data generator for TIME SERIES (IN PROCESS)
 *
 * Generates generates a 4 level parent child.
 *
 * Asset -> Box -> Carton -> Pallet
 * 
 * <pre>
 * select a.id, a.name, a.type, a.color, b.name, c.name, p.name from asset as a, box as b, carton as c, pallet as p where a.parentId = b.id and b.parentId = c.id and c.parentId = p.id and p.name = 'pallet36' and type = 'pants' order by b.name limit 15;
 * </pre>
 */
public class DirtyDataGen2 extends DirtyDataGen
{
	DateFormat df;
	
	private int verbosity;

	public static void main( String[] args ) throws Exception
	{
		DirtyDataGen2 ddg = new DirtyDataGen2();
		ddg.options();
		ddg.parse( args );
		ddg.init();
		ddg.generate();
	}

	public DirtyDataGen2()
	{
		String dfs = "YYYY-MM-dd HH:mm:ss.SSS";
		df = new SimpleDateFormat( dfs );
	}

	public void init() throws UnknownHostException
	{
		super.init();
		persister = new SQLPersisterTS();
		((SQLPersisterTS)persister).verbosity = verbosity;
	}

	public void options()
	{
		super.options();
		options.addOption( "v", true, "verbosity" );
	}
	
	public void parse( String[] args )
	{
		super.parse( args );
		verbosity = line.hasOption( 'v' ) ? Integer.parseInt( line.getOptionValue( 'v' ) ) : 0;
	}

	public void generate() throws Exception
	{
		logger.info( "Data generation start" );

		persister.start();

		TimerUtil tu = new TimerUtil();
		tu.start();
		
		SimpleDateFormat sdf = new SimpleDateFormat( "yyyy/MM/dd" );

		long epoch = sdf.parse( "2016/01/01" ).getTime();

		long simtime = epoch;
		FacilitySim sim = new FacilitySim();

		Set<Thing> list = sim.init( simtime );
		persister.persist( list );
		
		simtime += 15000;
		
		int count = 0;
		while( count < dataSize )
		{
			if( verbosity > 1 )
			{
			System.out.println( "********************************************************" );
			System.out.println( "SIMTIME=" + df.format( new Date( simtime ) ) );
			//System.out.println( "********************************************************" );
			}
			
			list = sim.step( simtime );
			
			if( verbosity > 1 )
			{
				for( Thing t : list )
				{
					System.out.print( "THING: " );
					System.out.println( printThing( t ) );
				}
			}
			persister.persist( list );
			ncount += list.size();
			count += list.size();
			simtime += 15000;
			
			if( tu.step( incount, ncount, dataSize ) )
			{
				System.out.print( String.format( "%s %.1f %.1f %s\n", formatter.format( ncount ), tu.percent, tu.rate, tu.etas ) );
			}
		}

		persister.end();

		logger.info( "Data generation ended in " + ((System.currentTimeMillis() - tu.start) / 1000) + " secs." );
	}

	private String printThing( Thing t )
	{
		StringBuffer sb = new StringBuffer();

		
		sb.append( " id=" + t.id + "" );
		sb.append( " name=" + t.name + "" );
		sb.append( " serial=" + t.serial + "" );
		
		if( t.parent == null )
		{
			sb.append( " parent=null" + "" );
		}
		else
		{
			sb.append( " parent=" + t.parent.name + "" );
		}
		
		sb.append( " groupId=" + t.groupId + "" );
		sb.append( " thingType.name=" + t.tt.getName() + "" );

		for( ThingField tf : t.udfs )
		{
			ThingTypeField ttf = (ThingTypeField) tf.getThingTypeField();
			sb.append( " " + ttf.getName() + " " + df.format( new Date( tf.getTimestamp() ) ) + " " + tf.getValue() + "" );
		}
		
		return sb.toString();
	}
}
