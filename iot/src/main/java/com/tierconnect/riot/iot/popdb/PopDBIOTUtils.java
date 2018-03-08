package com.tierconnect.riot.iot.popdb;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.controllers.RiotShiroRealm;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.services.UserService;
import com.tierconnect.riot.appcore.utils.Configuration;
import com.tierconnect.riot.appcore.utils.QueryUtils;
import com.tierconnect.riot.commons.dao.mongo.MongoDAOUtil;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import com.tierconnect.riot.sdk.servlet.security.ApiKeyToken;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.config.IniSecurityManagerFactory;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.Factory;

import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.math.NumberUtils.isNumber;

public class PopDBIOTUtils
{

	public static final String ZPL_COMMAND = "^XA^RS8^FO50,50^BCN,100,Y,N,N^FD${DocumentId}^FS^RFW,H^FD${serialNumber}^FN3^FS^FN3^RFR,H^FS^HV3^PQ${numCopies}^XZ";
	static Logger logger = Logger.getLogger( PopDBRequiredIOT.class );
	public static void initShiroWithRoot() {
		Factory<SecurityManager> factory         = new IniSecurityManagerFactory("classpath:shiro_riot.ini");
		SecurityManager          securityManager = factory.getInstance();
		SecurityUtils.setSecurityManager(securityManager);
		RiotShiroRealm.initCaches();
		Subject     currentUser = SecurityUtils.getSubject();
		ApiKeyToken token       = new ApiKeyToken(UserService.getInstance().getRootUser().getApiKey());
		currentUser.login(token);
		
		try {
            MongoDAOUtil.setupMongodb(Configuration.getProperty("mongo.primary"),
                    Configuration.getProperty("mongo.secondary"),
                    Configuration.getProperty("mongo.replicaset"),
                    Boolean.valueOf(Configuration.getProperty("mongo.ssl")),
                    Configuration.getProperty("mongo.username"),
                    Configuration.getProperty("mongo.password"),
                    Configuration.getProperty("mongo.authdb"),
                    Configuration.getProperty("mongo.db"),
                    Configuration.getProperty("mongo.controlReadPreference"),
                    Configuration.getProperty("mongo.reportsReadPreference"),
                    Boolean.valueOf(Configuration.getProperty("mongo.sharding")),
                    (isNotBlank(Configuration.getProperty("mongo.connectiontimeout")) && isNumber(Configuration
                            .getProperty("mongo.connectiontimeout"))) ? Integer.parseInt(Configuration.getProperty
                            ("mongo.connectiontimeout")) : null,
                    (isNotBlank(Configuration.getProperty("mongo.maxpoolsize")) && isNumber(Configuration.getProperty
                            ("mongo.maxpoolsize"))) ? Integer.parseInt(Configuration.getProperty("mongo.maxpoolsize"))
                            : null);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static ThingType popThingTypeRFID(Group group, String thingTypeCode) {
		ThingType rfid = PopDBIOTUtils.popThingType(group, null, "Default RFID Thing Type");
		rfid.setThingTypeCode(thingTypeCode);
		rfid.setAutoCreate(true);
		ThingTypeService.getInstance().update(rfid);
		PopDBIOTUtils.popThingTypeField(rfid,
										"location",
										"",
										"",
										ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
										ThingTypeField.Type.TYPE_LONLATALT.value,
										true,
										null,
										null,
										null);
		PopDBIOTUtils.popThingTypeField(rfid,
										"locationXYZ",
										"",
										"",
										ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
										ThingTypeField.Type.TYPE_XYZ.value,
										true,
										null,
										null,
										null);
		PopDBIOTUtils.popThingTypeField(rfid,
										"logicalReader",
										"",
										"",
										ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
										ThingTypeField.Type.TYPE_LOGICAL_READER.value,
										true,
										null,
										null,
										null);
		// TEMPORARY, WORKING ON POINT-IN-POLYGON
		PopDBIOTUtils.popThingTypeField(rfid,
										"zone",
										"",
										"",
										ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
										ThingTypeField.Type.TYPE_ZONE.value,
										true,
										null,
										null,
										null);
		PopDBIOTUtils.popThingTypeField(rfid,
										"eNode",
										"",
										"",
										ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
										ThingTypeField.Type.TYPE_TEXT.value,
										true,
										null,
										null,
										null);
		PopDBIOTUtils.popThingTypeField(rfid,
										"lastLocateTime",
										"millisecond",
										"ms",
										ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
										ThingTypeField.Type.TYPE_TIMESTAMP.value,
										false,
										null,
										null,
										null);
		PopDBIOTUtils.popThingTypeField(rfid,
										"lastDetectTime",
										"millisecond",
										"ms",
										ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
										ThingTypeField.Type.TYPE_TIMESTAMP.value,
										false,
										null,
										null,
										null);
		PopDBIOTUtils.popThingTypeField(rfid,
										"image",
										"",
										"",
										ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
										ThingTypeField.Type.TYPE_IMAGE_ID.value,
										false,
										null,
										null,
										null);
		PopDBIOTUtils.popThingTypeField(rfid,
										"registered",
										"millisecond",
										"ms",
										ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
										ThingTypeField.Type.TYPE_NUMBER.value,
										true,
										null,
										null,
										null);
		
		PopDBIOTUtils.popThingTypeField(rfid,
										"doorEvent",
										"",
										"",
										ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
										ThingTypeField.Type.TYPE_TEXT.value,
										true,
										null,
										null,
										null);

		PopDBIOTUtils.popThingTypeField(rfid,
										"shift",
										"",
										"",
										ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
										ThingTypeField.Type.TYPE_SHIFT.value,
										true,
										null,
										null,
										null);

		PopDBIOTUtils.popThingTypeField(rfid,
										"status",
										"",
										"",
										ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
										ThingTypeField.Type.TYPE_TEXT.value,
										true,
										null,
										null,
										null);

		return rfid;
	}
	
	public static ThingType popThingTypeGPS( Group group, String thingTypeCode )
	{
		ThingType rfid = PopDBIOTUtils.popThingType( group, null, "Default GPS Thing Type" );
		rfid.setThingTypeCode( thingTypeCode );
		rfid.setAutoCreate( true );
		ThingTypeService.getInstance().update( rfid );
		PopDBIOTUtils.popThingTypeField( rfid, "location", "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value, ThingTypeField.Type.TYPE_LONLATALT.value, true ,null, null, null);
		PopDBIOTUtils.popThingTypeField( rfid, "locationXYZ", "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value, ThingTypeField.Type.TYPE_XYZ.value, true ,null, null, null);
		//PopDBIOTUtils.popThingTypeField( rfid, "logicalReader", "", "", ThingField.TYPE_TEXT, true );
		// TEMPORARY, WORKING ON POINT-IN-POLYGON
		PopDBIOTUtils.popThingTypeField( rfid, "zone", "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value, ThingTypeField.Type.TYPE_ZONE.value, true,null, null, null);
		//PopDBIOTUtils.popThingTypeField( rfid, "eNode", "", "", ThingField.TYPE_TEXT , true);
		PopDBIOTUtils.popThingTypeField( rfid, "lastLocateTime", "millisecond", "ms",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value, ThingTypeField.Type.TYPE_TIMESTAMP.value, false ,null, null, null);
		//PopDBIOTUtils.popThingTypeField( rfid, "image", "", "", ThingField.TYPE_IMAGE_ID, false );
		PopDBIOTUtils.popThingTypeField( rfid, "lastDetectTime", "millisecond", "ms",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value, ThingTypeField.Type.TYPE_TIMESTAMP.value, false ,null, null, null);
		//PopDBIOTUtils.popThingTypeField( rfid, "registered", "millisecond", "ms", ThingField.TYPE_NUMBER, true );
		
		//PopDBIOTUtils.popThingTypeField( rfid, "doorEvent", "", "", ThingField.TYPE_TEXT, true );

        PopDBIOTUtils.popThingTypeField( rfid, "shift", "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value, ThingTypeField.Type.TYPE_SHIFT.value, true ,null, null, null);

		//what is this for ?
		//PopDBIOTUtils.popThingTypeField( rfid, "direction", "", "", ThingField.TYPE_TEXT, true );

        PopDBIOTUtils.popThingTypeField( rfid, "speed", "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value, ThingTypeField.Type.TYPE_NUMBER.value, false ,null, null, null);
        PopDBIOTUtils.popThingTypeField( rfid, "heading", "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value, ThingTypeField.Type.TYPE_NUMBER.value, false ,null, null, null);
        
		return rfid;
	}

	public static ThingType popThingTypeZPL( Group group, String thingTypeCode )
	{
		ThingType zpl = PopDBIOTUtils.popThingType( group, null, "Default ZPL Thing Type" );
		zpl.setThingTypeCode(thingTypeCode);
		try {
		ThingTypeTemplate thingTypeTemplate = ThingTypeTemplateService.getInstance().getByCode("ZPL");
		BooleanBuilder be = new BooleanBuilder();
		be = be.and( QueryUtils.buildSearch(QThingTypeFieldTemplate.thingTypeFieldTemplate, "thingTypeTemplate.id="+thingTypeTemplate.getId()) );
		List<ThingTypeFieldTemplate> thingTypeFieldTemplateLst = ThingTypeFieldTemplateService.getInstance().listPaginated(be, null, null);
		zpl.setThingTypeTemplate(thingTypeTemplate);
		zpl.setAutoCreate(true);
		ThingTypeService.getInstance().update(zpl);
		if (thingTypeFieldTemplateLst != null) {
			for (ThingTypeFieldTemplate thingTypeFieldTemplate : thingTypeFieldTemplateLst) {
				if (thingTypeFieldTemplate.getName().equals("zpl")) {
					PopDBIOTUtils.popThingTypeField(zpl, "zpl", "", "", ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
							ThingTypeField.Type.TYPE_ZPL_SCRIPT.value, false, ZPL_COMMAND, thingTypeFieldTemplate.getId(), ThingTypeField.Type.TYPE_ZPL_SCRIPT.value);
				}
				if (thingTypeFieldTemplate.getName().equals("rfidEncode")) {
					PopDBIOTUtils.popThingTypeField(zpl, "rfidEncode", "", "", ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
							ThingTypeField.Type.TYPE_BOOLEAN.value, false, "true", thingTypeFieldTemplate.getId(), ThingTypeField.Type.TYPE_BOOLEAN.value);
				}
			}
		}
		} catch (NonUniqueResultException e) {
			logger.error("Non unique result exception on ZPL ThingTypeTemplate", e);
		}
		return zpl;
	}
	
	public static Shift popShift( Group group, String name, Long startTimeOfDay, Long endTimeOfDay, String daysOfWeek, String code )
	{
		Shift b = new Shift();
		b.setName( name );
		b.setCode(code);
		b.setStartTimeOfDay( startTimeOfDay );
		b.setEndTimeOfDay( endTimeOfDay );
		b.setDaysOfWeek( daysOfWeek );
		b.setGroup( group );
        b.setActive(true);
		ShiftService.getInstance().insert( b );
		return b;
	}

//	// type = "core" or "edge"
//	public static Edgebox popEdgebox( Group group, String name, String code, String type, String configuration,Long port )
//	{
//		Edgebox b = new Edgebox();
//		b.setName( name );
//		b.setCode( code );
//		b.setGroup( group );
//		b.setConfiguration( configuration );
//		b.setParameterType( "BRIDGE_TYPE");
//		b.setType( type );
//		b.setPort(port);
//		EdgeboxService.getInstance().insert( b );
//		return b;
//	}

	public static EdgeboxRule popEdgeboxRule( Long edgeBoxId, String name, String rule, String input, String output, String outputConfig, String description, boolean active, boolean runOnReorder )
	{
		Edgebox edgebox = EdgeboxService.getInstance().get( edgeBoxId );

		EdgeboxRule edgeboxRule = new EdgeboxRule();
        edgeboxRule.setName(name);
		edgeboxRule.setRule(rule);
		edgeboxRule.setInput(input);
		edgeboxRule.setOutput(output);
        edgeboxRule.setOutputConfig(outputConfig);
        edgeboxRule.setDescription(description);
		edgeboxRule.setActive(active);
		edgeboxRule.setRunOnReorder(runOnReorder);
		edgeboxRule.setEdgebox(edgebox);
		EdgeboxRuleService.getInstance().insert( edgeboxRule );
		return edgeboxRule;
	}

   public static String getLocationFilterQuery() {
        return "select * from MojixMemberMessage "
             + "match_recognize ( "
             + "    partition by serial "
             + "    measures A as previousPoint, B as point "
             + "    after match skip to next row "
             + "    pattern (A B) "
             + "    define "
             + "        B as (((B.x-A.x)*(B.x-A.x))+((B.y-A.y)*(B.y-A.y))) > ? "
             + ")";
    }

    public static String getNotificationFilterQuery(){
        return "select * from MojixMemberMessage "
            + "match_recognize ( "
            + "    partition by serial "
            + "    measures A as point"
            + "    pattern (A) "
            + "    define "
            + "        A as A.x = prev(A.x) and A.y = prev(A.y))";
    }

    public static String getDefaultFilterQuery(){
        return "select * from MojixMemberMessage "
            + "match_recognize ( "
            + "    partition by serial "
            + "    measures A as point "
            + "    pattern (A) "
            + "    define "
            + "        A as prev(A.serial) is null"
            + ")";
    }

    public static String getZoneLocationFiltepopThingTyperQuery(){
        return "select message.* from MqttMemberMessage as message where currentTime < shiftScheduleStart or currentTime > shiftScheduleEnd";
    }

	public static ThingType popThingType(Group group, ThingType parent, String name) {
		return popThingType(group, parent, name, name.toLowerCase().replace(" ", "_") + "_code");
	}
	public static ThingType popThingType(Group group, ThingType parent, String name, String thingTypeCode) {
		ThingType thingType = new ThingType(name);
		thingType.setArchived(false);
		thingType.setGroup(group);
		thingType.setThingTypeTemplate(ThingTypeTemplateService.getInstance().get(1L));
		thingType.setModifiedTime(new Date().getTime());
		thingType.setThingTypeCode(thingTypeCode);
		ThingTypeService.getInstance().insert(thingType);
		return thingType;
	}

	public static ThingType popThingTypeWithTemplate(Group group, String name, String thingTypeCode, ThingTypeTemplate thingTypeTemplate, boolean autoCreate) {
		ThingType thingType = popThingType(group, null, name, thingTypeCode);
		thingType.setThingTypeTemplate(thingTypeTemplate);
		thingType.setAutoCreate(autoCreate);
		return ThingTypeService.getInstance().update(thingType);
	}

	/**
	 * @param name
	 * @param unitName
	 *            e.g. "meter", "foot", "kilogram", "second", etc.
	 * @param unitSymbol
	 *            e.g., "m", "ft", "kg", "s", etc.
	 * @param thingTypeFieldTemplateId
	 * @return
	 */
	public static ThingTypeField popThingTypeField(ThingType thingType,
												   String name,
												   String unitName,
												   String unitSymbol,
												   String thingTypeParent,
												   Long type,
												   boolean isTimeSeries,
												   String defaultValue,
												   Long thingTypeFieldTemplateId,
												   Long dataTypeThingTypeId)
	{
		ThingTypeField thingTypeField = new ThingTypeField(name,
														   unitName,
														   unitSymbol,
														   thingTypeParent,
														   DataTypeService.getInstance().get(type),
														   null);
		thingTypeField.setTimeSeries(isTimeSeries);
		thingTypeField.setThingType(thingType);
		thingTypeField.setMultiple(false);
		if (thingType.getThingTypeFields() == null)
		{
			thingType.setThingTypeFields(new HashSet<ThingTypeField>());
		}
		thingType.getThingTypeFields().add(thingTypeField);
		if (defaultValue != null && !defaultValue.trim().equals(""))
		{
			thingTypeField.setDefaultValue(defaultValue);
		}
		if (thingTypeFieldTemplateId != null)
		{
			thingTypeField.setThingTypeFieldTemplateId(thingTypeFieldTemplateId);
		}
		thingTypeField.setDataType(DataTypeService.getInstance().get(type));
		ThingTypeFieldService.getInstance().insert(thingTypeField);
		return thingTypeField;
	}

	public static ThingTypeMap popThingTypeMap( ThingType parent, ThingType child )
	{
		ThingTypeMap ttm = new ThingTypeMap();
		ttm.setParent( parent );
		ttm.setChild( child );
		return ThingTypeMapService.getInstance().insert( ttm );
	}

	public static Thing popThing( Group group, String name, String serial, boolean activated)
	{
		Thing thing = new Thing();
		thing.setGroup( group );
		thing.setName( name );
		thing.setSerial( serial );
		thing.setActivated( activated );
		ThingService.insert( thing, new Date() );
		return thing;
	}


	public static ThingParentHistory popThingMap( Thing parent, Thing child, Date startDate, Date endDate )
	{
		ThingParentHistory h = new ThingParentHistory();
		h.setParent( parent );
		h.setChild( child );
		h.setStartDate( startDate );
		h.setEndDate( endDate );
		ThingParentHistoryService.getInstance().insert( h );
		child.setParent( parent );
		ThingService.update( child, null );
		return h;
	}

	public static ThingType popThingTypeIPhone( Group group )
	{
		ThingType thingType = popThingType( group, null, "iPhone" );
		popThingTypeField( thingType, "lat", "degree", "deg",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value, ThingTypeField.Type.TYPE_NUMBER.value, true ,null, null, null);
		popThingTypeField( thingType, "lon", "degree", "deg",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value, ThingTypeField.Type.TYPE_NUMBER.value, true ,null, null, null);
		popThingTypeField( thingType, "ele", "feet", "ft",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value, ThingTypeField.Type.TYPE_NUMBER.value, true ,null, null, null);
		// update with thingtypefields
		thingType = ThingTypeService.getInstance().get( thingType.getId() );
		return thingType;
	}

	public static ThingType popThingTypeDartTag( Group group )
	{
		ThingType thingType = popThingType( group, null, "DartTag" );
		popThingTypeField( thingType, "temperature", "farenheit", "f",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value, ThingTypeField.Type.TYPE_NUMBER.value, true ,null, null, null);
		popThingTypeField( thingType, "battery", "volts", "v",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value, ThingTypeField.Type.TYPE_NUMBER.value, true,null, null, null);
		popThingTypeField( thingType, "x", "feet", "ft",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value, ThingTypeField.Type.TYPE_NUMBER.value, true,null, null, null);
		popThingTypeField( thingType, "y", "feet", "ft",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value, ThingTypeField.Type.TYPE_NUMBER.value, true,null, null, null);
		popThingTypeField( thingType, "z", "feet", "ft",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value, ThingTypeField.Type.TYPE_NUMBER.value, true,null, null, null);
		// update with thingtypefields
		thingType = ThingTypeService.getInstance().get( thingType.getId() );
		return thingType;
	}

	public static ZoneGroup popZoneGroup( LocalMap localmap, String name )
	{
		ZoneGroup onsite = new ZoneGroup();
		onsite.setGroup( localmap.getGroup() );
		onsite.setName( name );
		onsite.setLocalMap( localmap );
		return ZoneGroupService.getInstance().insert( onsite );
	}

	public static LocalMap populateFacilityMap( String name, String imageName, Group group, double lonmin, double lonmax, double latmin,
			double latmax, double declination,String unit )
	{
		return populateFacilityMap( name, imageName, group, lonmin, lonmax, latmin, latmax, -200, -200, declination,unit );
	}

	public static LocalMap populateFacilityMap( String name, String imageName, Group group, double lonmin, double width, double latmin,
			double height, double lonOrigin, double latOrigin, double declination,String unit )
	{
		LocalMap map = new LocalMap();
		map.setLonmin( lonmin );
		map.setImageWidth(width);
		map.setLatmin( latmin );
		map.setImageHeight(height);
		map.setImageUnit(unit);

		Date transactionDate = new Date();
		map.setModifiedTime(transactionDate.getTime());
		
	if( lonOrigin != 200 )
			map.setLonOrigin( lonOrigin );
		if( latOrigin != 200 )
			map.setLatOrigin( latOrigin );

		map.setAltOrigin(0.0);
		// new values for map maker
		map.setXNominal(0.0);
		map.setYNominal(0.0);
		map.setRotationDegree(0.0);
		map.setLatOriginNominal(map.getLatOrigin());
		map.setLonOriginNominal(map.getLonOrigin());
        // end new values for map maker
		map.setDeclination( declination );
		map.setName( name );
		map.setGroup( group );
		LocalMapService.calculateLatLonMax(map);
		byte[] bFile = null;
		try
		{
			InputStream inputStream = LocalMap.class.getClassLoader().getResourceAsStream( imageName );
			bFile = IOUtils.toByteArray( inputStream );
		}
		catch( IOException e )
		{
			e.printStackTrace();
		}
		map.setImage( bFile );

		LocalMapService.getInstance().insert( map );
		List<ZoneGroup> zoneGroups = new LinkedList<>();

		ZoneGroup zoneGroupOnSite = new ZoneGroup();
		zoneGroupOnSite.setDescription( (map.getName() != null ? map.getName() : "") + " On-Site" );
		zoneGroupOnSite.setName( "On-Site" );
		zoneGroupOnSite.setGroup( group );
		ZoneGroupService.getInstance().insert( zoneGroupOnSite );
		zoneGroupOnSite.setLocalMap( map );

		ZoneGroup zoneGroupOffSite = new ZoneGroup();
		zoneGroupOffSite.setDescription( (map.getName() != null ? map.getName() : "") + " Off-Site" );
		zoneGroupOffSite.setName( "Off-Site" );
		zoneGroupOffSite.setGroup( group );
		ZoneGroupService.getInstance().insert( zoneGroupOffSite );
		zoneGroupOffSite.setLocalMap( map );

		zoneGroups.add( zoneGroupOnSite );
		zoneGroups.add( zoneGroupOffSite );

		map.setZoneGroup( zoneGroups );
        popMapPoint(map, 0, map.getLonmin(), map.getLatmin());
        popMapPoint(map, 1, map.getLonmax(), map.getLatmin());
        popMapPoint(map, 2, map.getLonmax(), map.getLatmax());
        popMapPoint(map, 3, map.getLonmin(), map.getLatmax());
		return map;
	}

    public static void popMapPoint(LocalMap localMap, long index, Double lon, Double lat) {
        LocalMapPoint mapPoint = new LocalMapPoint();
        mapPoint.setX(lon);
        mapPoint.setY(lat);
        mapPoint.setArrayIndex(index);
        mapPoint.setLocalMap(localMap);
        LocalMapPointService.getInstance().insert(mapPoint);
    }

	public static Zone popZone( Group group, LocalMap lm, String name, String color, String zoneGroup )
	{
		Zone zone = new Zone();
		zone.setName( name );
		zone.setDescription( name );
		zone.setGroup( group );
		zone.setColor( color );
		zone.setLocalMap( lm );
		zone.setCode(name.substring(0,2)+(name.length() > 4 ? name.substring(name.length()-4, name.length()) : "1"));
		List<ZoneGroup> a = lm.getZoneGroup();
		for( ZoneGroup zg : a )
		{
			System.out.println( "zone====" + zg.getName() );
			System.out.println( "map====" + (lm.getName().toString() +  " " + zoneGroup) );
			if( zg.getName().toString().equals( zoneGroup ) )
				zone.setZoneGroup( zg );
		}

		zone = ZoneService.getInstance().insert( zone );
		return zone;
	}
	
	public static ZoneType popZoneType(Group group,String name,String code, List<ZoneProperty> zoneProperties)
	{
		ZoneType zt=new ZoneType();
		zt.setName(name);
		zt.setZoneTypeCode(code);
		zt.setGroup(group);
		zt.setZoneProperties(zoneProperties);
		ZoneTypeService.getInstance().insert(zt);
		
		return zt;
	}

    public static ZoneProperty popZoneProperty(String name,int type,ZoneType zoneType)
    {
        ZoneProperty zp=new ZoneProperty();
        zp.setName(name);
        zp.setType(type);
        		zp.setZoneType(zoneType);
        ZonePropertyService.getInstance().insert(zp);

        return zp;
    }
    	public static ZonePropertyValue popZonePropertyValue(String value,Zone zone,Long idPro)
    	{
    		ZonePropertyValue zp=new ZonePropertyValue();
    		zp.setValue(value);
    		zp.setZoneId(zone.getId());
    		zp.setZonePropertyId(idPro);
    		ZonePropertyValueService.getInstance().insert(zp);

           		return zp;
    	}
	
	public static void popZonePoint( Zone zone, long i, double x, double y )
	{
		ZonePoint zp = new ZonePoint();
		zp.setX( x );
		zp.setY( y );
		zp.setArrayIndex( i );
		zp.setZone( zone );
		ZonePointService.getInstance().insert( zp );
	}

	// NOTE: this is useful when declination is zero, not so much otherwise
	public static void popZoneBB( Zone zone, double lonmin, double latmin, double lonmax, double latmax )
	{
		popZonePoint( zone, 0, lonmin, latmin );
		popZonePoint( zone, 1, lonmax, latmin );
		popZonePoint( zone, 2, lonmax, latmax );
		popZonePoint( zone, 3, lonmin, latmax );
	}

	public static ZoneBuilder getZoneBuilder()
	{
		// return new ZoneBuilder();
		return null;
	}

	public class ZoneBuilder

	{

	}
	/**
	 * Method to infer the correct bridge type code
	 * @param edgeBox EdgeBox to migrate
	 * @return
	 */
	public static String getCorrectBridgeTypeCode(Edgebox edgeBox) {
		String response = null;
		if (edgeBox != null) {
			if(edgeBox.getConfiguration().contains("ftp")) {
				response = "FTP";
			} else if(edgeBox.getConfiguration().contains("geoforce")) {
				response = "GPS";
			} else if(edgeBox.getConfiguration().contains("messageMode")) {
				response = "StarFLEX";
			} else if(edgeBox.getConfiguration().contains("topics") ||
					edgeBox.getConfiguration().contains("threadDispatchMode")) {
				response = "core";
			} else if(edgeBox.getConfiguration().contains("zoneDwellFilter")) {
				response = "edge";
			} else  {
				response = "edge";
			}
		}
		return response;
	}
}
