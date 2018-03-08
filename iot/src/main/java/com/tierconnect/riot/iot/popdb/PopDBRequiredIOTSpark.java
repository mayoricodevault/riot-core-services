package com.tierconnect.riot.iot.popdb;

import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.popdb.PopDBRequired;
import com.tierconnect.riot.appcore.services.*;
import com.tierconnect.riot.appcore.utils.Configuration;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.*;
import org.apache.log4j.Logger;
import org.hibernate.Transaction;
import org.jose4j.json.internal.json_simple.JSONArray;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;

import java.util.*;

/**
 *
 * @author terry
 *
 *         This class populates the minimum required records for any appcore
 *         instance
 *
 */
public class PopDBRequiredIOTSpark
{

	static Logger logger = Logger.getLogger( PopDBRequiredIOTSpark.class );

	public static void main(String args[]) throws ParseException {
		PopDBRequired.initJDBCDrivers();
		System.getProperties().put("hibernate.hbm2ddl.auto", "update");
		System.getProperties().put("hibernate.cache.use_second_level_cache", "false");
		System.getProperties().put("hibernate.cache.use_query_cache", "false");
        Configuration.init(null);
		PopDBRequiredIOTSpark popdb       = new PopDBRequiredIOTSpark();
		Transaction      transaction = GroupService.getInstance().getGroupDAO().getSession().getTransaction();
		transaction.begin();
		PopDBIOTUtils.initShiroWithRoot();
		popdb.run();
		transaction.commit();
	}

	public void run() throws ParseException {
		//Populate Connection
		populateConnection();

		String[ ] topicKafka = {"/v1/data,100,1", "/v1/data/ALEB,8,1","/v1/data/APP2,1,1"};

		Edgebox edgeboxMCB = EdgeboxService.getInstance().selectByCode("MCB");
		String mcbConfig = getCoreBridgeConfiguration(edgeboxMCB.getConfiguration(), topicKafka);
		edgeboxMCB.setConfiguration( mcbConfig );
		edgeboxMCB.setDescription("SPARK TEST");
		EdgeboxService.getInstance().update( edgeboxMCB );

		Edgebox edgeboxALEB = EdgeboxService.getInstance().selectByCode("ALEB");
		edgeboxALEB.setConfiguration( getAleBridgeConfiguration(edgeboxALEB.getConfiguration()) );
		EdgeboxService.getInstance().update( edgeboxALEB );

		Edgebox edgeboxALEB2 = EdgeboxService.getInstance().selectByCode("ALEB2");
		edgeboxALEB.setConfiguration( getAleBridgeConfiguration(edgeboxALEB2.getConfiguration()) );
		EdgeboxService.getInstance().update( edgeboxALEB2 );
	}

	public static String getCoreBridgeConfiguration(String config, String[ ] topicsKafka) throws ParseException {

		JSONParser parser = new JSONParser();
		JSONObject configuration = (JSONObject) parser.parse(config);
		JSONArray topicsArray=new JSONArray();


		//KAFKA
		for (String topic:topicsKafka){
			topicsArray.add(topic);
		}
		JSONObject kafka=new JSONObject();
		kafka.put("connectionCode","KAFKA");
		kafka.put("topics",topicsArray);
		kafka.put("active",false);
		kafka.put("consumerGroup","group1");
		kafka.put("checkpoint",false);
		configuration.put("kafka",kafka);

		//HADOOP
		JSONObject hadoop=new JSONObject();
		hadoop.put("connectionCode","HADOOP");
		kafka.put("active", false);
		configuration.put("hadoop",hadoop);

		//SPARK
		JSONObject spark=new JSONObject();
		spark.put("connectionCode","SPARK");
		configuration.put("spark",spark);

		return configuration.toJSONString();
	}

	public static String getAleBridgeConfiguration(String config) throws ParseException {
		JSONParser parser = new JSONParser();
		JSONObject configuration = (JSONObject) parser.parse(config);

		JSONObject kafka = new JSONObject();
		kafka.put("connectionCode", "KAFKA");
		kafka.put("active", false);
		configuration.put("kafka", kafka);

		return configuration.toJSONString();
	}

	public void populateConnection() {
		// SQLServer connection example
		Group      rootGroup  = GroupService.getInstance().getRootGroup();

		// KAFKA connection example
		Connection connection = new Connection();
		connection.setName( "Kafka" );
		connection.setCode( "KAFKA" );
		connection.setGroup( rootGroup );
		connection.setConnectionType(
				ConnectionTypeService.getInstance().getConnectionTypeDAO().selectBy( QConnectionType.connectionType.code.eq( "KAFKA" ) ) );
		JSONObject jsonProperties = new JSONObject();
		Map<String, Object> mapProperties = new LinkedHashMap<String, Object>();
		mapProperties.put( "zookeeper", "localhost:2181" );
		mapProperties.put( "server", "localhost:9092" );
		jsonProperties.putAll( mapProperties );
		connection.setProperties( jsonProperties.toJSONString() );
		ConnectionService.getInstance().insert( connection );

		// Hadoop connection example
		connection = new Connection();
		connection.setName( "Hadoop" );
		connection.setCode( "HADOOP" );
		connection.setGroup( rootGroup );
		connection.setConnectionType(
		ConnectionTypeService.getInstance().getConnectionTypeDAO().selectBy( QConnectionType.connectionType.code.eq( "HADOOP" ) ) );
		jsonProperties = new JSONObject();
		mapProperties = new LinkedHashMap<String, Object>();
		mapProperties.put( "active", "false" );
		mapProperties.put( "host", "localhost" );
		mapProperties.put( "port", 9000 );
		mapProperties.put( "path", "masterdata" );
		mapProperties.put("secure", false );
		jsonProperties.putAll( mapProperties );
		connection.setProperties( jsonProperties.toJSONString() );
		ConnectionService.getInstance().insert( connection );

		// Spark connection example
		connection = new Connection();
		connection.setName("Spark");
		connection.setCode("SPARK");
		connection.setGroup(rootGroup);
		connection.setConnectionType(ConnectionTypeService.getInstance().getConnectionTypeDAO().selectBy( QConnectionType.connectionType.code.eq("SPARK")));
		jsonProperties = new JSONObject();
		mapProperties = new LinkedHashMap<String, Object>();
		mapProperties.put("masterHost", "localhost");
		mapProperties.put("port", 8081);
		// TODO: Disable restart app in spark and remove extra configuration.
		// mapProperties.put("applicationId", null);
		// mapProperties.put("driverHost", null);
		// mapProperties.put("executorMemory", "1G");
		// mapProperties.put("totalExecutorCores", 2);
		// mapProperties.put("executorCores", 2);
		// mapProperties.put("schedulerMode", "FIFO");
		// mapProperties.put("numExecutors", 1);
		// mapProperties.put("batchInterval", 3);
		// mapProperties.put("writeToMongo", false);
		// mapProperties.put("consumerPollMs", 5000);
		// mapProperties.put("workers", "172.18.0.101,172.18.0.102,172.18.0.103");
		jsonProperties.putAll(mapProperties);
		connection.setProperties(jsonProperties.toJSONString());
		ConnectionService.getInstance().insert(connection);
	}

}
