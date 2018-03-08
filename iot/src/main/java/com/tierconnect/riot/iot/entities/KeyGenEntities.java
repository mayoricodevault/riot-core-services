package com.tierconnect.riot.iot.entities;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.tierconnect.riot.appcore.entities.Connection;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.commons.dtos.ConnectionDto;
import com.tierconnect.riot.commons.dtos.EdgeboxConfigurationDto;
import com.tierconnect.riot.commons.dtos.EdgeboxDto;
import com.tierconnect.riot.commons.dtos.GroupDto;
import com.tierconnect.riot.commons.dtos.LogicalReaderDto;
import com.tierconnect.riot.commons.dtos.ShiftDto;
import com.tierconnect.riot.commons.dtos.ShiftZoneDto;
import com.tierconnect.riot.commons.dtos.ThingTypeDto;
import com.tierconnect.riot.commons.dtos.ZoneDto;
import com.tierconnect.riot.commons.dtos.ZoneTypeDto;
import com.tierconnect.riot.commons.utils.TenantUtil;

/**
 * The goal of this class is to have the key generation for kafka objects side by side for Entities and commons DTOs class, to make sure
 * they stay consistent.
 * 
 * The methods here are used in generating kafka tickles and the kafka cache loader tool (BrokerClientHelper and CacheLoaderToolNew).
 * 
 * These also need to stay consistent with what is in bridges in KafkaCacheSevice and KeyMapperFunction classes.
 * 
 * 
 * @author tcrown
 *
 */
public class KeyGenEntities
{
    private static final Logger logger = Logger.getLogger(KeyGenEntities.class);
    
	static Map<String, Method> idMethods = new HashMap<String, Method>();

	private static Method getMethod( Object o )
	{
		Method m = idMethods.get( o.getClass().getName() );
		if( m == null )
		{
			try
			{
				m = o.getClass().getMethod( "getId" );
			}
			catch( Exception e )
			{
			    logger.error("", e);
			}
		}
		return m;
	}

	public static String getId( Object o )
	{
		long id = 0;
		try
		{
			Method m = getMethod( o );
			id = (long) m.invoke( o );
		}
		catch( Exception e )
		{
		    logger.error("", e);
		}
		return "ID-" + String.valueOf( id );
	}

	private static String getCode( Group group, String code )
	{
		String tenantCode = (group != null) ? TenantUtil.getTenantCode( group.getHierarchyName() ) : null;
		tenantCode = ((tenantCode != null) ? (tenantCode + "-") : "");
		return "CODE-" + tenantCode + code;
	}

	/*
	 * Connection
	 */
	public static String getCode( Connection connection )
	{
		return "CODE-" + connection.getCode();
	}

	public static String getCode( ConnectionDto connection )
	{
		return "CODE-" + connection.code;
	}

	/*
	 * Edgebox
	 * 
	 * TODO: why are these different ? which one is correct ?
	 */
	public static String getCode( Edgebox edgebox )
	{
		return "CODE-" + edgebox.getCode();
	}

	public static String getCode( EdgeboxDto edgebox )
	{
		return "CODE-" + edgebox.code;
	}

	/*
	 * EdgeboxConfiguration
	 * 
	 * NOTE: in the tickles, the code uses the Edgebox entitie code above
	 */
	public static String getCode( EdgeboxConfigurationDto edgebox )
	{
		return "CODE-" + edgebox.code;
	}

	/*
	 * Group
	 */
	public static String getCode( Group group )
	{
		return "CODE-" + group.getCode();
	}

	public static String getCode( GroupDto dto )
	{
		return "CODE-" + dto.code;
	}

	/*
	 * LogicalReader
	 */
	public static String getCode( LogicalReader logicalReader )
	{
		return getCode( logicalReader.getGroup(), logicalReader.getCode() );
	}

	public static String getCode( LogicalReaderDto logicalReader )
	{
		Group group = GroupService.getInstance().get( logicalReader.groupId );
		return getCode( group, logicalReader.getCode() );
	}

	/*
	 * Shift
	 */
	public static String getCode( Shift shift )
	{
		Group group = shift.getGroup();
		return getCode( group, shift.getCode() );
	}

	public static String getCode( ShiftDto shift )
	{
		Group group = GroupService.getInstance().get( shift.groupId );
		return getCode( group, shift.code );
	}

	/*
	 * ShiftZone
	 * 
	 * TODO: how are the tickles doing this ?
	 */
	public static String getCode( ShiftZoneDto shiftZone )
	{
		Group group = GroupService.getInstance().get( shiftZone.shift.groupId );
		return getCode( group, String.valueOf( shiftZone.shift.id ) );
	}

	/*
	 * ThingType
	 *
	 */
	public static String getCode( ThingType thingType )
	{
		return "CODE-" + thingType.getCode();
	}

	public static String getCode( ThingTypeDto thingType )
	{
		return "CODE-" + thingType.getCode();
	}

	/*
	 * Zone
	 */
	public static String getCode( Zone zone )
	{
		Group group = zone.getGroup();
		return getCode( group, zone.getCode() );
	}

	public static String getCode( ZoneDto zone )
	{
		Group group = GroupService.getInstance().get( zone.groupId );
		return getCode( group, zone.getCode() );
	}

	/*
	 * ZoneType
	 */
	public static String getCode( ZoneType zoneType )
	{
		return "CODE-" + zoneType.getZoneTypeCode();
	}

	public static String getCode( ZoneTypeDto zoneType )
	{
		return "CODE-" + zoneType.getZoneTypeCode();
	}

}
