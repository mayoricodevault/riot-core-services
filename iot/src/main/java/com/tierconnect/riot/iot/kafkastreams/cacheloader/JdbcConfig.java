package com.tierconnect.riot.iot.kafkastreams.cacheloader;

import java.io.Serializable;

public class JdbcConfig implements Serializable {
	
	public String driverClassName;
	public String jdbcUrl;
	public String userName;
	public String password;
	public String dialect;
	public String hazelcastNativeClientAddress;
}
