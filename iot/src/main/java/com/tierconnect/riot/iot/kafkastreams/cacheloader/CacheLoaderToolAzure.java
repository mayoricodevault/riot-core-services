package com.tierconnect.riot.iot.kafkastreams.cacheloader;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.table.CloudTable;
import com.microsoft.azure.storage.table.CloudTableClient;
import com.microsoft.azure.storage.table.TableOperation;
import com.microsoft.azure.storage.table.TableServiceEntity;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import org.apache.log4j.Logger;

/**
 * CacheLoaderTool class.
 *
 * For loading into Microsoft Azure
 * 
 * @author tcrown
 *
 */
public class CacheLoaderToolAzure
{
	private static Logger logger = Logger.getLogger( CacheLoaderToolAzure.class );

	private String args[];

	File outdir;

	String storageConnectionString = "DefaultEndpointsProtocol=https;AccountName=azurefunctions7adfe218;AccountKey=ioNEwuJpAzYSN8apY/zgSud+q5ttfeAlnPTuI0e3EBVrPRqK/0NtQlb2k6wQ1O29/wSxXd6RhVeyoAsYZSuU4w==;EndpointSuffix=core.windows.net";

	public static void main( String args[] ) throws Exception
	{
		CacheLoaderToolAzure cacheLoaderTool = new CacheLoaderToolAzure( args );
		cacheLoaderTool.run();
	}

	public CacheLoaderToolAzure( String[] args ) throws Exception
	{
		this.args = args;
		outdir = new File( "/var/tmp/cacheLoader" );
	}

	private void run() throws Exception
	{
		File[] file_tables = outdir.listFiles();

		for( int i = 0; i < file_tables.length; i++ )
		{
			if( file_tables[i].isDirectory() )
			{
				String n = file_tables[i].getName();
				
				String tableName = "cache" + n.substring(0, 1).toUpperCase() + n.substring(1) ;
				
				CloudTable t = createTable( tableName );

				File[] file_rows = file_tables[i].listFiles();
				for( int j = 0; j < file_rows.length; j++ )
				{
					File file_row = file_rows[j];
					String partitionKey = "1";
					String rowKey = file_row.getName().substring( 3 );
					String json = this.readFile( file_row.toPath() );
					
					insertRow( t, tableName, partitionKey, rowKey, json );
				}
			}
		}
	}

	private String readFile( Path f ) throws IOException
	{
		byte[] b = Files.readAllBytes( f );
		// StandardCharsets.UTF_8
		return new String( b, Charset.defaultCharset() );
	}

	public CloudTable createTable( String tableName ) throws Exception
	{
		CloudStorageAccount storageAccount = CloudStorageAccount.parse( storageConnectionString );

		// Create the table client.
		CloudTableClient tableClient = storageAccount.createCloudTableClient();

		// Create the table if it doesn't exist.
		CloudTable cloudTable = tableClient.getTableReference( tableName );
		
		return cloudTable;
	}

	public class CacheEntity extends TableServiceEntity
	{
		String json;

		public CacheEntity( String partitionKey, String rowKey, String json )
		{
			this.partitionKey = partitionKey;
			this.rowKey = rowKey;
			this.json = json;
		}

		public String getJSON()
		{
			return json;
		}

		public void setJSON( String json )
		{
			this.json = json;
		}
	}

	public void insertRow( CloudTable cloudTable, String tableName, String partitionKey, String rowKey, String json ) throws InvalidKeyException, URISyntaxException, StorageException
	{
		logger.info( tableName + " " + partitionKey + " " + rowKey + " " + json );

		// Create a new customer entity.
		CacheEntity customer1 = new CacheEntity( partitionKey, rowKey, json );

		// Create an operation to add the new customer to the people table.
		TableOperation insertCustomer1 = TableOperation.insertOrReplace( customer1 );

		// Submit the operation to the table service.
		cloudTable.execute( insertCustomer1 );
	}
}
