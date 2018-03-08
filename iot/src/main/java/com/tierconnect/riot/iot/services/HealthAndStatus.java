package com.tierconnect.riot.iot.services;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tierconnect.riot.appcore.servlet.TokenCacheHelper;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;
//import net.sf.ehcache.Cache;
//import net.sf.ehcache.CacheManager;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

import com.tierconnect.riot.appcore.services.TokenService;

public class HealthAndStatus
{
	static Logger logger = Logger.getLogger(HealthAndStatus.class);

	static HealthAndStatus service;

	static long bootTime;
	static long seqnum;

	static
	{
		bootTime = System.currentTimeMillis();
		service = new HealthAndStatus();
		seqnum = 0;
	}

	long endpointAssociateCount = 0;

	public static HealthAndStatus getInstance()
	{
		return service;
	}

	public String getMessage( long timestamp, String app )
	{
		seqnum++;

		Runtime runtime = Runtime.getRuntime();
		long mb = 1024 * 1024;
		double used = (runtime.totalMemory() - runtime.freeMemory()) / mb;

		// ALEB,1433983070008,uptime,3939642
		StringBuffer sb = new StringBuffer();
		sb.append( "sn=" + seqnum + "\n" );

		sb.append( app + "," + timestamp + ",uptime," + (timestamp - bootTime) + "\n" );
		sb.append( app + "," + timestamp + ",mem.used," + used + "\n" );

		long concurrentUsers = TokenCacheHelper.getConcurrentUsers();
		sb.append( app + "," + timestamp + ",concurrentUsers," + concurrentUsers + "\n" );

		getL2CacheInfo(timestamp, app, sb);

		double cpuLoad = 0;
		try
		{
			OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean( OperatingSystemMXBean.class );
			cpuLoad = (Double) Class.forName( "com.sun.management.OperatingSystemMXBean" ).getMethod( "getProcessCpuLoad" ).invoke( osBean );
		}
		catch( Exception ex )
		{
		}
		
		sb.append( app + "," + timestamp + ",cpu.processLoad," + cpuLoad + "\n" );

		synchronized( this )
		{
			sb.append( app + "," + timestamp + ",endpointAssociateCount," + endpointAssociateCount + "\n" );
		}

		return sb.toString();
	}

	public void getL2CacheInfo(long timestamp, String app, StringBuffer sb)
	{
		//CacheManager manager = CacheManager.getInstance();
		Map<String, Map> map = new TreeMap();
		long secondLevelCacheHitCount2 = 0;
		long secondLevelCacheMissCount2 = 0;
		/*
		for (String cacheName : manager.getCacheNames())
		{
			Cache cache = manager.getCache(cacheName);
			Map<String, Long> entry = new TreeMap<>();
			long cacheHits = cache.getStatistics().getCacheHits();
			long cacheMisses = cache.getStatistics().getCacheMisses();
			secondLevelCacheHitCount2 += cacheHits;
			secondLevelCacheMissCount2 += cacheMisses;
			entry.put("hits", cacheHits);
			entry.put("misses", cacheMisses);
			map.put(cacheName.substring(cacheName.lastIndexOf(".")), entry);
		}
		*/
		//NOTE HibernateSessionFactory.getInstance().getStatistics() only works when hibernate.generate_statistics=true is set but logs too much info every second
		long secondLevelCacheHitCount = HibernateSessionFactory.getInstance().getStatistics().getSecondLevelCacheHitCount();
		long secondLevelCacheMissCount = HibernateSessionFactory.getInstance().getStatistics().getSecondLevelCacheMissCount();
		if (secondLevelCacheHitCount == 0 && secondLevelCacheMissCount == 0)
		{
			secondLevelCacheHitCount = secondLevelCacheHitCount2;
			secondLevelCacheMissCount = secondLevelCacheMissCount2;
		}
		Map<String, Long> entry = new TreeMap<>();
		entry.put("hits", secondLevelCacheHitCount);
		entry.put("misses", secondLevelCacheMissCount);
		map.put("hibernate", entry);
		ObjectMapper objectMapper = new ObjectMapper();
		try
		{
			logger.debug(objectMapper.writeValueAsString(map));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		sb.append( app + "," + timestamp + ",cacheL2.hits," + secondLevelCacheHitCount + "\n" );
		sb.append( app + "," + timestamp + ",cacheL2.misses," + secondLevelCacheMissCount + "\n" );
	}

	public void incrementEndpointAssociateCount()
	{
		synchronized( this )
		{
			endpointAssociateCount++;
		}
	}
}
