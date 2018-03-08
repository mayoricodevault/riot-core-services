package com.tierconnect.riot.datagen;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
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
 * FOR TIME SERIES DATA
 * 
 * 
 * SELECT * FROM Asset PROCEDURE ANALYSE();
 * 
 */
public class SQLPersisterTS implements Persister
{
	static Logger logger = Logger.getLogger( SQLPersisterTS.class );

	private Connection conn;

	PreparedStatement insertMap;

	PreparedStatement updateMap;

	// KEY: thingType.name
	private Map<String, CacheObject> tableCache = new HashMap<>();

	ExistCache existCache;

	int verbosity = 2;

	class CacheObject
	{
		String insertString;
		PreparedStatement insert;

		String updateString;
		PreparedStatement update;

		int count = 0;
	}

	public SQLPersisterTS()
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

			Statement stmt = conn.createStatement();

			String str = dropTableString( "ThingMap" );
			stmt.executeUpdate( str );

			str = createMapTableStr();
			stmt.executeUpdate( str );

			insertMap = conn.prepareStatement( insertMapStr() );
		}
		catch( SQLException | ClassNotFoundException ex )
		{
			throw new RuntimeException( ex );
		}
	}

	public void persist( Set<Thing> things )
	{
		try
		{
			// tableCache.clear();
			conn.setAutoCommit( false );
			for( Thing thing : things )
			{
				batchOrFlush( thing );
			}
			conn.commit();
		}
		catch( SQLException e )
		{
			logger.error( "SQL Exception: ", e );
			// throw new Error( e );
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

	@Override
	public void persist( Thing thing )
	{

	}

	private void batchOrFlush( Thing thing ) throws SQLException
	{
		String thingTypeName = thing.tt.getName();

		CacheObject cache = tableCache.get( thingTypeName );

		if( cache == null )
		{
			Statement stmt = conn.createStatement();

			String str = dropTableString( thingTypeName );
			stmt.executeUpdate( str );

			str = createTableStr( thing.tt );
			stmt.executeUpdate( str );

			cache = new CacheObject();

			cache.insertString = insertStr( thing.tt );
			cache.insert = conn.prepareStatement( cache.insertString );

			// cache.updateString = updateString( thing.tt );
			// cache.update = conn.prepareStatement( cache.updateString );

			tableCache.put( thingTypeName, cache );
		}

		if( !existCache.exists( thing ) )
		{
			batchInsert( cache.insert, thing );
			batchMapInsert( insertMap, thing );

			existCache.add( thing );

			// update counter
			cache.count++;
		}
		else
		{
			// String str = updateString( thing );
			if( thing.udfs.size() > 0 )
			{
				// Statement stmt = conn.createStatement();
				// logger.info( "q=" + str );
				// stmt.executeUpdate( str );
				batchInsert( cache.insert, thing );
				// stmt.close();
			}
			// parent
			batchMapInsert( insertMap, thing );
			Statement stmt = conn.createStatement();
			updateChildren( stmt, insertMap, thing );
			stmt.close();
		}
	}

	protected String dropTableString( String name )
	{
		return "DROP TABLE IF EXISTS " + name;
	}

	String createMapTableStr()
	{
		StringBuffer sb = new StringBuffer();
		sb.append( "CREATE TABLE ThingMap (" );
		sb.append( " id bigint(20) not NULL AUTO_INCREMENT," );
		sb.append( " timestamp0 bigint(20) ," );
		sb.append( " timestamp1 bigint(20) ," );
		sb.append( " parentTable VARCHAR(255)," );
		sb.append( " parentId bigint(20) ," );
		sb.append( " childTable VARCHAR(255)," );
		sb.append( " childId bigint(20) ," );
		sb.append( " PRIMARY KEY ( id )" );
		sb.append( " )" );
		return sb.toString();
	}

	String insertMapStr()
	{
		StringBuffer sb = new StringBuffer();
		sb.append( "INSERT into ThingMap" );
		sb.append( " ( timestamp0, timestamp1, parentTable, parentId, childTable, childId )" );
		sb.append( " values ( ?, ?, ?, ?, ?, ? )" );
		return sb.toString();
	}

	protected void batchMapInsert( PreparedStatement ps, Thing child ) throws SQLException
	{
		if( child.parent != null )
		{
			ps.setLong( 1, child.timestamp );
			ps.setObject( 2, null );
			ps.setString( 3, child.parent.tt.getName() );
			ps.setLong( 4, child.parent.id );
			ps.setString( 5, child.tt.getName() );
			ps.setLong( 6, child.id );
			// ps.addBatch();
			if( verbosity > 1 )
			{
				System.out.println( "BATCH INSERT: ps=" + ps );
			}
			ps.executeUpdate();
		}
	}

	protected void batchMapInsert( PreparedStatement ps, Thing parent, String childType, long childId ) throws SQLException
	{
		ps.setLong( 1, parent.timestamp );
		ps.setObject( 2, null );
		ps.setString( 3, parent.tt.getName() );
		ps.setLong( 4, parent.id );
		ps.setString( 5, childType );
		ps.setLong( 6, childId );
		// ps.addBatch();
		if( verbosity > 1 )
		{
			System.out.println( "BATCH INSERT: ps=" + ps );
		}
		ps.executeUpdate();
	}

	// TODO implement !
	private void updateChildren( Statement stmt, PreparedStatement ps, Thing parent ) throws SQLException
	{
		// select * from ThingMap where parentId = thing.id and parentTable =
		// thing.tt.name

		StringBuffer sb = new StringBuffer();
		sb.append( "select * from ThingMap" );
		sb.append( String.format( " where parentId = '%d' and parentTable = '%s'", parent.id, parent.tt.getName() ) );

		ResultSet rs = stmt.executeQuery( sb.toString() );

		// System.out.print( "\n" );

		while( rs.next() )
		{
			if( verbosity > 2 )
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

			String childType = rs.getString( 6 );
			long childId = rs.getLong( 7 );

			batchMapInsert( ps, parent, childType, childId );
		}

	}

	String createTableStr( ThingType tt ) throws SQLException
	{
		StringBuffer sb = new StringBuffer();
		sb.append( "CREATE TABLE " + tt.getName() + " (" );
		sb.append( " id bigint(20)  not NULL," );
		sb.append( " timestamp0 bigint(20) ," );
		sb.append( " timestamp1 bigint(20) ," );
		sb.append( " name VARCHAR(32)," );
		sb.append( " serial VARCHAR(32)," );
		sb.append( " thingType VARCHAR(32)," );
		sb.append( " groupId bigint(20) ," );
		// sb.append( " parentId bigint(20) ," );
		for( ThingTypeField ttf : tt.getThingTypeFields() )
		{
			sb.append( " " + ttf.getName() + " VARCHAR(255)," );
		}
		sb.append( " PRIMARY KEY ( id, timestamp0 )" );
		sb.append( " )" );

		// System.out.println( "SQL='" + str + "'" );

		return sb.toString();
	}

	String insertStr( ThingType tt )
	{
		StringBuffer sb = new StringBuffer();
		sb.append( "INSERT into " + tt.getName() + " (" );
		sb.append( " id, timestamp0, timestamp1, name, serial, thingType, groupId" );
		for( ThingTypeField ttf : tt.getThingTypeFields() )
		{
			sb.append( ", " + ttf.getName() );
		}
		sb.append( " ) values ( ?, ?, ?, ?, ?, ?, ?" );
		for( ThingTypeField ttf : tt.getThingTypeFields() )
		{
			sb.append( ", ?" );
		}
		sb.append( " )" );
		return sb.toString();
	}

	protected void batchInsert( PreparedStatement ps, Thing thing ) throws SQLException
	{
		ps.setLong( 1, thing.id );
		ps.setLong( 2, thing.timestamp );
		ps.setObject( 3, null );
		ps.setString( 4, thing.name );
		ps.setString( 5, thing.serial );
		ps.setString( 6, thing.tt.getName() );
		ps.setInt( 7, thing.groupId );

		// if( thing.parent != null )
		// {
		// ps.setLong( 6, thing.parent.id );
		// }
		// else
		// {
		// ps.setObject( 6, null );
		// }

		int size = thing.tt.getThingTypeFields().size();
		// System.out.println( "thingType.name=" + thing.tt.getName() + "
		// numFields=" + size );
		for( int i = 0; i < size; i++ )
		{
			try
			{
				ThingField tf = thing.udfs.get( i );
				ps.setString( 8 + i, String.valueOf( tf.getValue() ) );
			}
			catch( Exception e )
			{
				ps.setString( 8 + i, "" );
			}
		}

		if( verbosity > 1 )
		{
			System.out.println( "BATCH INSERT: ps=" + ps );
		}

		// ps.addBatch();
		ps.executeUpdate();
	}

	// TODO: make into a prepared statement !
	// We dont need this as this is a TS series, we never go back and update !
	String updateString0( Thing thing )
	{
		StringBuffer sb = new StringBuffer();

		sb.append( "UPDATE " + thing.tt.getName() + " SET" );

		sb.append( " name = '" + thing.name + "'," );
		sb.append( " serial = '" + thing.serial + "'," );
		sb.append( " thingType = '" + thing.tt.getName() + "'" );

		// if( thing.parent == null )
		// {
		// sb.append( " parentId = " + null );
		// }
		// else
		// {
		// sb.append( " parentId = " + thing.parent.id );
		// }

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
			// for( String type : tableCache.keySet() )
			// {
			// CacheObject cache = tableCache.get( type );
			// PreparedStatement ps = cache.insert;
			// if( ps != null )
			// {
			// ps.executeBatch();
			// System.out.println( "executeBatch2: ps=" + ps );
			// ps.close();
			// }
			// ps = cache.update;
			// if( ps != null )
			// {
			// ps.executeBatch();
			// System.out.println( "executeBatch3: ps=" + ps );
			// ps.close();
			// }
			// insertMap.executeBatch();
			// }
			// conn.commit();
			conn.close();
		}
		catch( SQLException e )
		{
			logger.error( "Error on  close ", e );
		}
	}

	@Override
	public long getMaxId()
	{
		// TODO Auto-generated method stub
		return 0;
	}
}
