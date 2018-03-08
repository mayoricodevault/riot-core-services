package com.tierconnect.riot.datagen;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.tierconnect.riot.iot.entities.ThingField;
import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.entities.ThingTypeField;

/**
 * FOR LAST VALUE DATA
 * 
 * 
 * SELECT * FROM Asset PROCEDURE ANALYSE();
 * 
 */
public class SQLPersisterLV implements Persister
{
	static Logger logger = Logger.getLogger( SQLPersisterLV.class );

	
	private final static long CACHE_SIZE = 1;

	private Connection conn;

	// KEY: thingType.name
	Map<String, String> insertStatements = new HashMap<String, String>();

	// KEY: thingType.name, KEY:
	private Map<String, Map<String, Object>> tableCache = new HashMap<>();

	ExistCache existCache;

	public SQLPersisterLV()
	{
		existCache = new ExistCache();
	}

	@Override
	public void start()
	{
		try
		{
			Class.forName( "org.gjt.mm.mysql.Driver" );
			conn = DriverManager.getConnection( "jdbc:mysql://localhost:3306/ts_1", "root", "control123!" );
			conn.setAutoCommit( false );
		}
		catch( SQLException | ClassNotFoundException ex )
		{
			throw new RuntimeException( ex );
		}
	}
	
	@Override
	public void persist( Thing thing )
	{
		try
		{
			batchOrFlush( thing );
		}
		catch( SQLException e )
		{
			logger.error( "SQL Exception: ", e );
			//throw new Error( e );
			try
			{
				conn.rollback();
			}
			catch( SQLException ex )
			{
				logger.error( "Error on rollback ", ex );
			}
		}
	}

	private void batchOrFlush( Thing thing ) throws SQLException
	{
		String thingTypeName = thing.tt.getName();

		if( !insertStatements.containsKey( thingTypeName ) )
		{
			Statement stmt = conn.createStatement();
			String str = dropTableString( thingTypeName );
			stmt.executeUpdate( str );
			str = createTableStr( thing.tt );
			stmt.executeUpdate( str );
			str = insertStr( thing.tt );
			insertStatements.put( thingTypeName, str );
		}

		Map<String, Object> cache = tableCache.get( thingTypeName );

		// cache not found
		if( cache == null )
		{
			existCache.add( thing );
			cache = new HashMap<>();
			cache.put( "counter", 1 );
			PreparedStatement ps = conn.prepareStatement( insertStatements.get( thingTypeName ) );
			cache.put( "statement", ps );
			tableCache.put( thingTypeName, cache );
			// batch insert
			batchInsert( ps, thing );
		}
		else
		{
			if( ! existCache.exists( thing ) )
			{
				existCache.add( thing );
				PreparedStatement ps = (PreparedStatement) cache.get( "statement" );
				batchInsert( ps, thing );

				// update counter
				int counter = (int) cache.get( "counter" ) + 1;
				cache.put( "counter", counter );

				// we reached CACHE_SIZE, flush!!!!
				if( counter >= CACHE_SIZE )
				{
					//System.out.println( "executeBatch1: type=" + thingTypeName + " ps=" + ps );
					ps.executeBatch();
					cache.put( "counter", 0 );
					conn.commit();
					conn.setAutoCommit( false );
				}
			}
			else
			{
				String str = updateStr( thing );
				if( thing.udfs.size() > 0 )
				{
					Statement stmt = conn.createStatement();
					stmt.executeUpdate( str );
					stmt.close();
				}
			}
		}
	}
	
	protected String dropTableString( String name )
	{
		String str = "DROP TABLE IF EXISTS " + name;
		return str;
	}

	String createTableStr( ThingType tt ) throws SQLException
	{
		StringBuffer sb = new StringBuffer();
		sb.append( "CREATE TABLE " + tt.getName() + " (" );
		sb.append( " id bigint(20)  not NULL," );
		sb.append( " name VARCHAR(32)," );
		sb.append( " serial VARCHAR(32)," );
		sb.append( " thingType VARCHAR(32)," );
		sb.append( " groupId bigint(20) ," );
		sb.append( " parentId bigint(20) ," );
		for( ThingTypeField ttf : tt.getThingTypeFields() )
		{
			sb.append( " " + ttf.getName() + " VARCHAR(255)," );
		}
		sb.append( " PRIMARY KEY ( id )" );
		sb.append( " )" );

		// System.out.println( "SQL='" + str + "'" );

		return sb.toString();
	}

	String createMapTableStr( ThingType tt ) throws SQLException
	{
		StringBuffer sb = new StringBuffer();
		sb.append( "CREATE TABLE ThingMap (" );
		sb.append( " id bigint(20) not NULL," );
		sb.append( " parentTable VARCHAR(255)," );
		sb.append( " parentId bigint(20) ," );
		sb.append( " childTable VARCHAR(255)," );
		sb.append( " childId bigint(20) ," );
		sb.append( " PRIMARY KEY ( id )" );
		sb.append( " )" );

		return sb.toString();
	}

	String insertStr( ThingType tt )
	{
		StringBuffer sb = new StringBuffer();

		sb.append( "INSERT into " + tt.getName() + " (" );
		sb.append( " id, name, serial, thingType, groupId, parentId" );

		for( ThingTypeField ttf : tt.getThingTypeFields() )
		{
			sb.append( ", " + ttf.getName() );
		}

		sb.append( " ) values ( ?, ?, ?, ?, ?, ?" );

		for( ThingTypeField ttf : tt.getThingTypeFields() )
		{
			sb.append( ", ?" );
		}
		sb.append( " )" );

		return sb.toString();
	}

	protected void batchInsert( PreparedStatement ps, Thing thing ) throws SQLException
	{
		ps.setInt( 1, (int) thing.id );
		ps.setString( 2, thing.name );
		ps.setString( 3, thing.serial );
		ps.setString( 4, thing.tt.getName() );
		ps.setInt( 5, thing.groupId );

		if( thing.parent != null )
		{
			ps.setLong( 6, thing.parent.id );
		}
		else
		{
			ps.setObject( 6, null );
		}

		int size = thing.tt.getThingTypeFields().size();
		// System.out.println( "thingType.name=" + thing.tt.getName() + "
		// numFields=" + size );
		for( int i = 0; i < size; i++ )
		{
			try
			{
				ThingField tf = thing.udfs.get( i );
				ps.setString( 7 + i, String.valueOf( tf.getValue() ) );
			}
			catch( Exception e )
			{
				ps.setString( 7 + i, "" );
			}
		}

		// System.out.println( "INSERT: ps=" + ps );

		ps.addBatch();
	}

	String updateStr( Thing thing ) throws SQLException
	{
		StringBuffer sb = new StringBuffer();

		sb.append( "UPDATE " + thing.tt.getName() + " SET" );

		// TODO: name, serial, thingType ?

		if( thing.parent == null )
		{
			sb.append( " parentId = " + null );
		}
		else
		{
			sb.append( " parentId = " + thing.parent.id );
		}

		int c = 0;
		for( ThingField tf : thing.udfs )
		{
			ThingTypeField ttf = (ThingTypeField) tf.getThingTypeField();
			// if( c > 0 )
			sb.append( "," );
			sb.append( " " + ttf.getName() + " = '" + tf.getValue() + "'" );
			c++;
		}
		sb.append( " WHERE id=" + thing.id );

		// System.out.println( "UPDATE: '" + sb.toString() + "'" );

		return sb.toString();
	}

	@Override
	public void end()
	{
		try
		{
			for( String type : tableCache.keySet() )
			{
				Map<String, Object> cache = tableCache.get( type );
				PreparedStatement ps = (PreparedStatement) cache.get( "statement" );
				// System.out.println( "executeBatch: type=" + type + " ps=" +
				// ps );
				if( ps != null )
				{
					ps.executeBatch();
					// System.out.println( "executeBatch: ps=" + ps );
					ps.close();
				}
			}
			conn.commit();
			conn.close();
		}
		catch( SQLException e )
		{
			logger.error( "Error on on commit ", e );
		}
	}

	@Override
	public long getMaxId()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void persist( Set<Thing> things )
	{
		// TODO Auto-generated method stub
		
	}
}
