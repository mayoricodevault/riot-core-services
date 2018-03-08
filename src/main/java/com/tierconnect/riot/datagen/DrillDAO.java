package com.tierconnect.riot.datagen;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DrillDAO
{
	String db;
	String dbname;
	Connection connection;
	
	public DrillDAO( Connection connection, String db, String dbname )
	{
		this.db = db;
		this.dbname = dbname;
		this.connection = connection;
	}
	
	public int count() throws SQLException
	{
		int count = 0;
		count += count( "Asset" );
		count += count( "Box" );
		count += count( "Carton" );
		count += count( "Pallet" );
		return count;
	}
	
	public int count( String table ) throws SQLException
	{
		int count = 0;
		Statement st = connection.createStatement();
		ResultSet rs;
		if( db.equals( "mongo" ) )
		{
			rs = st.executeQuery( "USE mongo." + dbname );
		}
		rs = st.executeQuery( "select count(*) from " + table );
		while( rs.next() )
		{
			count = rs.getInt( 1 );
		}
		return count;
	}
}
