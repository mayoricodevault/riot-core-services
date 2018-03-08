package com.tierconnect.riot.commons.utils;

import org.apache.commons.lang.StringUtils;

/**
 * KafkaUtil class.
 *
 * @author jantezana
 * @version 11/06/2016
 */
public final class KafkaUtil
{
	/**
	 * Formats the topic
	 *
	 * @param topic
	 *            the topic
	 * @return the formatted topic
	 */
	public static String formatTopic( final String topic )
	{
		// Modify the topic, replace the '/' by '____'
		String formattedTopic = StringUtils.replace( topic, "/#", ".*" );
		formattedTopic = StringUtils.replace( formattedTopic, "/", "___" );

		return formattedTopic;
	}

	/**
	 * 
	 * @param appName
	 * @param instance
	 *            the instance number of this app on any given server. Allows multiple instances to be run on one server.
	 * @param streamName
	 * @return
	 */
	public static String getApplicationId( String appName, int instance, String streamName )
	{
		return appName + "-" + instance + "-" + streamName;
	}

	public static String getBaseDir( String appName )
	{
		return "/var/vizix/" + appName;
	}
}
