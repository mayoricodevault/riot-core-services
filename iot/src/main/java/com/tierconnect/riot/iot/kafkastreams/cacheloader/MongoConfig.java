package com.tierconnect.riot.iot.kafkastreams.cacheloader;

import java.io.Serializable;

public class MongoConfig implements Serializable
{
	public String mongoPrimary;

	public String mongoSecondary;

	public String mongoReplicaSet;

	public boolean mongoSSL;

	public String username;

	public String password;

	public String mongoAuthDB;

	public String mongoDB;

	public boolean mongoSharding;

	public int mongoConnectTimeout;

	public int mongoMaxPoolSize;
}
