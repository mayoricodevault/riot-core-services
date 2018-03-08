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

/**
 * Created by agutierrez on 3/2/16.
 *
 * Persist data on a SQL DB
 */
public class SQLPersister implements Persister
{
	static Logger logger = Logger.getLogger( SQLPersister.class );

	private final static String SQL = "INSERT into %s (id, name, serial, type, udf1, udf2, udf3, udf4, udf5, groupId, parentId) "
			+ "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	private final static String CREATE_TABLE = "CREATE TABLE %s (id INTEGER not NULL, name VARCHAR(255), serial VARCHAR(255), type VARCHAR(255), "
			+ "udf1 VARCHAR(255), udf2 VARCHAR(255), udf3 VARCHAR(255), udf4 VARCHAR(255), udf5 VARCHAR(255), "
			+ "groupId INTEGER, parentId INTEGER, PRIMARY KEY ( id ))";

	private final static String DROP_TABLE = "DROP TABLE IF EXISTS %s";

	private final static long CACHE_SIZE = 100;

	private Connection conn;
	private Map<String, Map<String, Object>> tableCache = new HashMap<>();

	static
	{
		try
		{
			Class.forName( "org.gjt.mm.mysql.Driver" );
		}
		catch( Exception ex )
		{
			// empty
		}
	}

	public SQLPersister()
	{

	}

	@Override
	public void start()
	{
		Statement stmt = null;
		try
		{
			conn = DriverManager.getConnection( "jdbc:mysql://localhost:3306/riot_main", "root", "control123!" );
			conn.setAutoCommit( false );

			stmt = conn.createStatement();

			dropCreateTable( stmt, "ASSET" );
			dropCreateTable( stmt, "BOX" );
			dropCreateTable( stmt, "CARTON" );
			dropCreateTable( stmt, "PALLET" );

		}
		catch( SQLException ex )
		{
			throw new RuntimeException( ex );

		}
		finally
		{
			if( stmt != null )
			{
				try
				{
					stmt.close();
				}
				catch( SQLException e )
				{
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void persist( Thing thing )
	{
		// // save normalized
		try
		{
			batchOrFlush( thing );
		}
		catch( SQLException e )
		{
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
	public void end()
	{
		try
		{
			for( String type : tableCache.keySet() )
			{
				Map<String, Object> cache = tableCache.get( type );
				PreparedStatement ps = (PreparedStatement) cache.get( "statement" );
				if( ps != null )
				{
					ps.executeBatch();
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

	private void dropCreateTable( Statement stmt, String name ) throws SQLException
	{

		stmt.executeUpdate( String.format( DROP_TABLE, name ) );

		// create tables
		stmt.executeUpdate( String.format( CREATE_TABLE, name ) );
	}

	private void batchOrFlush( Thing thing ) throws SQLException
	{
		Map<String, Object> cache = tableCache.get( thing.tt.getName() );

		// cache found
		if( cache != null )
		{
			PreparedStatement ps = (PreparedStatement) cache.get( "statement" );
			batch( thing, ps );

			// update counter
			int counter = (int) cache.get( "counter" ) + 1;
			cache.put( "counter", counter );

			// we reached CACHE_SIZE, flush!!!!
			if( counter > CACHE_SIZE )
			{
				ps.executeBatch();
				cache.put( "counter", 0 );
				conn.commit();
				conn.setAutoCommit( false );
			}
		}
		// no cache found. init and put in map
		else
		{
			cache = new HashMap<>();
			cache.put( "counter", 1 );
			PreparedStatement ps = conn.prepareStatement( String.format( SQL, thing.tt.getName() ) );
			cache.put( "statement", ps );

			tableCache.put( thing.tt.getName(), cache );

			// batch date
			batch( thing, ps );
		}
	}

	private void batch( Thing thing, PreparedStatement ps ) throws SQLException
	{
		ps.setInt( 1, (int) thing.id );
		ps.setString( 2, thing.name );
		ps.setString( 3, thing.serial );
		ps.setString( 4, thing.tt.getName() );
		ps.setString( 5, String.valueOf( thing.udfs.get( 0 ).getValue() ) );
		ps.setString( 6, String.valueOf( thing.udfs.get( 1 ).getValue() ) );
		ps.setString( 7, String.valueOf( thing.udfs.get( 2 ).getValue() ) );
		ps.setString( 8, String.valueOf( thing.udfs.get( 3 ).getValue() ) );
		ps.setString( 9, String.valueOf( thing.udfs.get( 4 ).getValue() ) );
		ps.setInt( 10, thing.groupId );

		if( thing.parent != null )
		{
			ps.setLong( 11, thing.parent.id );
		}
		else
		{
			ps.setObject( 11, null );
		}

		ps.addBatch();
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
