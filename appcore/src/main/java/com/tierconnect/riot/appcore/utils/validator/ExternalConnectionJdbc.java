package com.tierconnect.riot.appcore.utils.validator;

import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;


public class ExternalConnectionJdbc
{
	private static Logger logger = Logger.getLogger( ExternalConnectionJdbc.class );

	private static ExternalConnectionJdbc instance;
	private Connection connectionInstance;

	protected ExternalConnectionJdbc()
	{
	}

	/*
	* Get a unique Connection
	* */
	public static ExternalConnectionJdbc getInstance(
						   String driver
						 , String url
						 , String schema
						 , String user
						 , String password )
	{
		try
		{
		if(instance == null)
		{
				instance = new ExternalConnectionJdbc();
				Class.forName(driver).newInstance();
				instance.setConnectionInstance(DriverManager.getConnection(url, user, password));
				logger.info("ExternalConnectionJdbc created for Data Base " + schema + " with the params:");
				logger.info( "-Driver" + driver);
				logger.info( "-URL"+ url);
				logger.info( "-Schema"+ schema);
				logger.info( "-user"+ user);
				logger.info( "-password"+ password);
			} else {
				Class.forName(driver).newInstance();
				instance.setConnectionInstance(DriverManager.getConnection(url, user, password));
				logger.info( "Connection Reloaded with URL: " + url );
			}
		} catch( Exception e )
			{
				logger.error( "Error to open the data base"+schema+" with url connection: "+url+". >>\n" + e );
				throw new UserException(e);
			}

		return instance;
	}

	/*
	* Method to get connection
	* */

	public Connection getConnection()
	{
		return connectionInstance;
	}
	/*
	* Method to close Connection
	* */

	public void closeConnection()
	{
		try{
			if(connectionInstance!=null && !connectionInstance.isClosed())
			{
				connectionInstance.close();
			}
			connectionInstance = null;
			logger.info( "Connection closed. " );
		}catch(Exception e)
		{
			logger.error( "Error in close conenction." + e );
			throw new UserException(e);
		}
	}

	/*
	* Method to verify if there is a connection active for the database
	* */

	public boolean isActiveConnection()
	{
		boolean response = false;
		try{
			if(connectionInstance!=null && !connectionInstance.isClosed())
			{
				response  = true;
			}
		}catch(Exception e)
		{
			logger.error( "Error to get the status of data base connection");
		}
		return response;
	}

	public Connection getConnectionInstance()
	{
		return connectionInstance;
	}

	private void setConnectionInstance( Connection connectionInstance )
	{
		this.connectionInstance = connectionInstance;
	}
}

