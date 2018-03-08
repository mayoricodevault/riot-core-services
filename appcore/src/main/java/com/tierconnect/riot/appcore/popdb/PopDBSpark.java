package com.tierconnect.riot.appcore.popdb;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.services.*;
import com.tierconnect.riot.appcore.utils.Configuration;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import org.hibernate.Transaction;

import java.util.*;


/*
 * SHOW ROLE-RESOURCES RECORDS
 *
 * select group0.name as GroupName, groupType.name as GroupType, role.name as
 * Role, resource.name as Resource, permissions, acceptedAttributes from
 * roleResource, role, resource, groupType, group0 where
 * role.id=roleResource.role_id and resource.id= roleResource.resource_id and
 * role.groupType_id = groupType.id and group0.id=role.group_id order by
 * group0.id, role.name, resource.name;
 */

/**
 *
 * @author terry
 *
 *         This class populates the minimum required records for any appcore
 *         instance
 *
 */
public class PopDBSpark
{

	public void run() throws NonUniqueResultException {


		Group rootGroup = GroupService.getInstance().getByCode("root");

		populateConnectionTypes(rootGroup);
	}

	/**
	 * Populate Connection Types
	 * @param rootGroup A root Group
	 */
	public static void populateConnectionTypes(Group rootGroup) {

		populateKAFKAConnection(rootGroup);
		populateHadoopConnection(rootGroup);
		populateSparkConnection(rootGroup);
	}

	public static Map<String, Object> newPropertyDefinition(String code, String label, String type) {
		return newPropertyDefinition(code, label, type, null);
	}

	public static Map<String, Object> newPropertyDefinition(String code, String label, String type, String defaultValue) {
		Map<String, Object> propertyDefinition = new LinkedHashMap<>();
		propertyDefinition.put("code", code);
		propertyDefinition.put("label", label);
		propertyDefinition.put("type", type);
		if (defaultValue != null) {
			propertyDefinition.put("defaultValue", defaultValue);
		}
		return propertyDefinition;
	}

	/**
	 * Populate KAFKA connections
	 * @param rootGroup
	 */
	public static void populateKAFKAConnection(Group rootGroup) {
		//KAFKA CONNECTION
		ConnectionType dbConnectionType = new ConnectionType();
		dbConnectionType.setGroup(rootGroup);
		dbConnectionType.setCode("KAFKA");
		dbConnectionType.setDescription("KAFKA Broker");

		List<Map<String, Object>> propertyDefinitions = new ArrayList<>();

		propertyDefinitions.add(newPropertyDefinition("zookeeper", "Zookeeper", "String"));
		propertyDefinitions.add(newPropertyDefinition("server", "Server(s)", "String"));

		ObjectMapper objectMapper = new ObjectMapper();
		try {
			dbConnectionType.setPropertiesDefinitions(objectMapper.writeValueAsString(propertyDefinitions));
			ConnectionTypeService.getInstance().insert(dbConnectionType);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Populate Hadoop Connection
	 * @param rootGroup
	 */
	public static void populateHadoopConnection(Group rootGroup) {
		//Hadoop CONNECTION
		ConnectionType dbConnectionType = new ConnectionType();
		dbConnectionType.setGroup(rootGroup);
		dbConnectionType.setCode("HADOOP");
		dbConnectionType.setDescription("Hadoop DB");

		List<Map<String, Object>> propertyDefinitions = new ArrayList<>();
		propertyDefinitions.add(newPropertyDefinition("active", "Active", "String"));
		propertyDefinitions.add(newPropertyDefinition("host", "Host", "String"));
		propertyDefinitions.add(newPropertyDefinition("port", "Port", "Number"));
		propertyDefinitions.add(newPropertyDefinition("path", "Path", "String"));
		propertyDefinitions.add(newPropertyDefinition("secure", "Secure", "Boolean"));

		ObjectMapper objectMapper = new ObjectMapper();

		try {
			dbConnectionType.setPropertiesDefinitions(objectMapper.writeValueAsString(propertyDefinitions));
			ConnectionTypeService.getInstance().insert(dbConnectionType);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Populate Spark Connection Type.
	 * @param rootGroup
	 */
	public static void populateSparkConnection(Group rootGroup) {
		//Spark CONNECTION
		ConnectionType dbConnectionType = new ConnectionType();
		dbConnectionType.setGroup(rootGroup);
		dbConnectionType.setCode("SPARK");
		dbConnectionType.setDescription("Spark");

		List<Map<String, Object>> propertyDefinitions = new ArrayList<>();

		propertyDefinitions.add(newPropertyDefinition("masterHost", "Master Host", "String"));
		propertyDefinitions.add(newPropertyDefinition("port", "Port", "Number"));
		// TODO: Disable restart app in spark and remove extra configuration.
		//propertyDefinitions.add(newPropertyDefinition("applicationId", "Application Id", "String"));
		//propertyDefinitions.add(newPropertyDefinition("driverHost", "Driver Host", "String"));
		//propertyDefinitions.add(newPropertyDefinition("executorMemory", "Executor Memory", "String"));
		//propertyDefinitions.add(newPropertyDefinition("totalExecutorCores", "Total Executor Cores", "Number"));
		//propertyDefinitions.add(newPropertyDefinition("executorCores", "Executor Cores", "Number"));
		//propertyDefinitions.add(newPropertyDefinition("schedulerMode", "Scheduler Mode", "String"));
		//propertyDefinitions.add(newPropertyDefinition("numExecutors", "Number of Executors", "Number"));
		//propertyDefinitions.add(newPropertyDefinition("batchInterval", "Batch Interval", "Number"));
		//propertyDefinitions.add(newPropertyDefinition("writeToMongo", "Write to Mongo", "Boolean"));
		//propertyDefinitions.add(newPropertyDefinition("consumerPollMs", "Kafka consumer poll ms", "Number"));
		//propertyDefinitions.add(newPropertyDefinition("workers", "Workers", "String"));

		ObjectMapper objectMapper = new ObjectMapper();

		try {
			dbConnectionType.setPropertiesDefinitions(objectMapper.writeValueAsString(propertyDefinitions));
			ConnectionTypeService.getInstance().insert(dbConnectionType);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Main Task to Populate Data Base.
	 * @param args Arguments to set in command prompt.
	 */
	public static void main(String args[]) throws NonUniqueResultException {
		PopDBRequired.initJDBCDrivers();
		System.getProperties().put("hibernate.hbm2ddl.auto", "update");
		System.getProperties().put("hibernate.cache.use_second_level_cache", "false");
		System.getProperties().put("hibernate.cache.use_query_cache", "false");
		Configuration.init(null);
		PopDBSpark popDBRequired = new PopDBSpark();
		Transaction transaction = GroupService.getGroupDAO().getSession().getTransaction();
		transaction.begin();
		popDBRequired.run();
		transaction.commit();
	}
}
