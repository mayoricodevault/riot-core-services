package com.tierconnect.riot.iot.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.types.path.EntityPathBase;
import com.tierconnect.riot.appcore.controllers.GroupController;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.QGroup;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.services.GroupFieldService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.RecentService;
import com.tierconnect.riot.appcore.utils.*;
import com.tierconnect.riot.iot.dao.mongo.ThingMongoDAO;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.iot.services.exceptions.InvalidVideoFeedException;
import com.tierconnect.riot.iot.services.exceptions.UserIsNotOperatorException;
import com.tierconnect.riot.iot.services.exceptions.ZoneHasNoCameraException;
import com.tierconnect.riot.sdk.dao.Pagination;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.utils.BeanUtils;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.jaxrs.PATCH;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.LngLatAlt;
import org.geojson.Polygon;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.UnknownHostException;
import java.util.*;

@Path("/zone")
@Api("/zone")
//@ApiOperation(position = 1, value = "Zones using Json")

public class ZoneController extends ZoneControllerBase
{

    static Logger logger = Logger.getLogger(ZoneController.class);

	@GET
	@Path("/geojson")
	@Produces(MediaType.APPLICATION_JSON)
	@RequiresPermissions(value = { "zone:r" })
	@ApiOperation(position = 1, value = "Get a List of Zones in GeoJson")
	public Response listZonesGeoJson( @QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber, @QueryParam("order") String order, @QueryParam("where") String where, @QueryParam("visibilityGroupId") Long visibilityGroupId, @DefaultValue("") @QueryParam("upVisibility") String upVisibility, @DefaultValue("") @QueryParam("downVisibility") String downVisibility )
	{
		Group visibilityGroup = VisibilityUtils.getVisibilityGroup( Zone.class.getCanonicalName(), visibilityGroupId);
		Pagination pagination = new Pagination( pageNumber, pageSize );

		BooleanBuilder be = new BooleanBuilder();

		EntityVisibility entityVisibility = getEntityVisibility();
		be = be.and( GeneralVisibilityUtils.limitVisibilitySelectAll(entityVisibility, QZone.zone, visibilityGroup, upVisibility, downVisibility) );
		be = be.and( QueryUtils.buildSearch( QZone.zone, where ) );

		FeatureCollection featureCollection = new FeatureCollection();

		List<EntityPathBase<?>> properties = new ArrayList<>();
		properties.add(QZone.zone.zoneGroup);
		properties.add(QZone.zone.zoneType);

        List<Zone> zoneList = ZoneService.getInstance().listPaginated(be, pagination, order, properties, QZone.zone.zonePoints);
        if(zoneList != null) {
            Set<Zone> zoneSet = new LinkedHashSet<>(zoneList);
            for (Zone zone : zoneSet) {
                featureCollection.add(zone.publicGeoJsonFeature());
            }
        }

		String featureCollectionGeoJson = "";
		try
		{
			featureCollectionGeoJson = new ObjectMapper().writeValueAsString( featureCollection );
		}
		catch( Exception e )
		{
			e.printStackTrace();
		}
		return RestUtils.sendOkResponse( featureCollectionGeoJson, false );
	}

	private boolean isGroupInsideTree( Group group, Group treeRoot )
	{
		if( group.getId().equals( treeRoot.getId() ) )
		{
			return true;
		}
		int rootLevel = treeRoot.getTreeLevel();
		if( group.getTreeLevel() < rootLevel )
		{
			return false;
		}
		if( treeRoot.getId().equals( group.getParentLevel( rootLevel ).getId() ) == false )
		{
			return false;
		}
		return true;
	}

	@POST
	@Path("/geojson")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@RequiresPermissions(value = { "zone:u" })
	@ApiOperation(position = 1, value = "Change the Zones using GeoJson")
	public Response changeZonesByGeoJson( String geoJsonString )
	{
		FeatureCollection featureCollectionResult = new FeatureCollection();
		try
		{
			FeatureCollection featureCollection = new ObjectMapper().readValue( geoJsonString, FeatureCollection.class );
            Group visibilityGroup = VisibilityUtils.getVisibilityGroup(Zone.class.getCanonicalName(), null);
			for( Feature feature : featureCollection.getFeatures() )
			{
				Long zoneId = Long.parseLong( feature.getId() );
				Polygon polygon = (Polygon) feature.getGeometry();
				List<List<Double>> points = new LinkedList<>();
				for( List<LngLatAlt> coordinates : polygon.getCoordinates() )
				{
					for( LngLatAlt coordinate : coordinates )
					{
						points.add( Lists.newArrayList( coordinate.getLongitude(), coordinate.getLatitude(), (double) points.size() ) );
					}
				}
				List<Double> firstPoint = points.get( 0 );
				List<Double> lastPoint = points.get( points.size() - 1 );
				if( firstPoint.get( 0 ).doubleValue() == lastPoint.get( 0 ).doubleValue()
						&& firstPoint.get( 1 ).doubleValue() == lastPoint.get( 1 ).doubleValue() )
				{
					points.remove( points.size() - 1 );
				}
				Zone zone = ZoneService.getInstance().get( zoneId );
				if( zone == null )
				{
					zone = new Zone();
					zone.setGroup( visibilityGroup );
					ZoneService.getInstance().insert( zone );
				}
				if( feature.getProperties().containsKey( "name" ) )
				{
					String name = feature.getProperty( "name" ).toString();
					zone.setName( name );
				}
				if( feature.getProperties().containsKey( "description" ) )
				{
					String description = feature.getProperty( "description" ).toString();
					zone.setDescription( description );
				}
				if( feature.getProperties().containsKey( "group_id" ) )
				{
					Long groupId = new Long( feature.getProperty( "group_id" ).toString() );
					Group group = GroupService.getInstance().get( groupId );
					zone.setGroup( group );
				}
				if( zone.getGroup() == null )
				{
					zone.setGroup( visibilityGroup );
				}
				ZoneService.getInstance().updateZonePoints( zone, points );
				featureCollectionResult.add( zone.publicGeoJsonFeature() );
			}
		}
		catch( Exception e )
		{
			e.printStackTrace();
		}
		String featureCollectionGeoJson = "";
		try
		{
			featureCollectionGeoJson = new ObjectMapper().writeValueAsString( featureCollectionResult );
		}
		catch( Exception e )
		{
			e.printStackTrace();
		}
		return RestUtils.sendOkResponse( featureCollectionGeoJson, false );
	}

	@PATCH
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@RequiresAuthentication
	@ApiOperation(position = 1, value = "Change the Zones using Json")
	public Response changeZonesByJson( List<Map<String, Object>> listMap ) {
        try {
            List<Map<String, Object>> mapResponse = ZoneService.getInstance().bulkZoneProcess(listMap);
            return RestUtils.sendOkResponse(mapResponse, false);
        } catch (Exception e) {
            logger.error("Bulk zone process: Unknown error has occurred processing zones.", e);
        }
        return RestUtils.sendBadResponse("Bulk zone process: Unknown error has occurred processing zones.");
	}




	@POST
	@Path("/zoneBuilder")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@RequiresPermissions(value = { "zone:u" })
	@ApiOperation(position = 1, value = "Change the Zones using Json for ZoneBuilder")
	public Response changeZonesForZoneBuilder( List<Map<String, Object>> listMap ) {
		List<Map<String, Object>> listMapResponse = new LinkedList<Map<String, Object>>();

		for( Map<String, Object> zoneMap : listMap )
		{
			Zone zone = null;
			if( zoneMap.get( "id" ) != null )
			{
				zone = ZoneService.getInstance().get(((Number) zoneMap.get("id")).longValue());
			}
			if (zone == null) {
				zone = new Zone();
			}
			Group group;
			if( zoneMap.containsKey( "group.id" ) )
			{
				group = GroupService.getInstance().get( ((Number) zoneMap.get( "group.id" )).longValue() );
			}
			else
			{
				group = VisibilityUtils.getVisibilityGroup( Zone.class.getCanonicalName(), null);
			}
			zone.setGroup( group );

            if ( zoneMap.containsKey( "localMap.id" )) {
                if( !zoneMap.containsKey( "zoneGroup.id" ) ) {
                    return RestUtils.sendBadResponse("ZoneGroup is required when a facility map is selected");
                }
            }

            if( zoneMap.containsKey( "zoneGroup.id" ) )
            {
                ZoneGroup zoneGroup = ZoneGroupService.getInstance().get( Long.valueOf( zoneMap.get( "zoneGroup.id" ).toString() ) );
                if(zoneGroup != null) {
                    zone.setZoneGroup(zoneGroup);
                    zone.setLocalMap(zoneGroup.getLocalMap());
                }
            }

			if( zoneMap.get( "name" ) == null || zoneMap.get( "name" ).toString().isEmpty() )
			{
				zone.setName( "Zone " + zone.getId().toString() );
			}
			else
			{
				zone.setName( (String) zoneMap.get( "name" ) );
			}


			zone.setDescription( (String) zoneMap.get( "description"));
            zone.setColor((String) zoneMap.get( "color" ) );

            if( zone.getId() == null )
            {
                ZoneService.getInstance().insert( zone );
            }

			List<Map<String, Object>> zonePoints = (List<Map<String, Object>>) zoneMap.get( "zonePoints" );
			List<List<Double>> points = new LinkedList<>();
			for( Map<String, Object> zonePoint : zonePoints )
			{
				List<Double> point = new LinkedList<>();
				point.add( new Double( zonePoint.get( "x" ).toString() ) );
				point.add( new Double( zonePoint.get( "y" ).toString() ) );
				Double index = points.size() + 0.0;
				if( zonePoint.containsKey( "arrayIndex" ) )
				{
					index = new Double( zonePoint.get( "arrayIndex" ).toString() );
				}
				point.add( index );
				points.add( point );
			}
            if(zoneMap.containsKey("zoneType")) {
                ZoneTypeService.updatingZoneTypeFromZone(zoneMap.get("zoneType"), zone);
            }
            if(zoneMap.containsKey("code")) {
                zone.setCode(zoneMap.get("code").toString());
            }

//            if(!validateDuplicateName(zone)) {
//                return RestUtils.sendBadResponse( String.format( "Duplicate Name in Zones: " + zone.getName() ) );
//            }
			ZoneService.getInstance().updateZonePoints( zone, points );
            Map<String, Object> zonePublicMap = zone.publicMap();
            if(zonePublicMap.containsKey("tmpId")) {
                zonePublicMap.put("oldId", zoneMap.get("tmpId"));
            }
			listMapResponse.add( zone.publicMap() );
		}
		return RestUtils.sendOkResponse( listMapResponse, false );
	}

	@Override
	@PUT
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@RequiresPermissions(value = { "zone:i" })
	@ApiOperation(position = 3, value = "Insert a Zone")
	public Response insertZone( Map<String, Object> map,@QueryParam("createRecent") @DefaultValue("false") Boolean createRecent )
	{

		EntityVisibility entityVisibility = getEntityVisibility();
		GeneralVisibilityUtils.limitVisibilityInsert(entityVisibility, VisibilityUtils.getObjectGroup(map));
		//QueryUtils.filterWritePermissions( Zone.class, map );
		List<Map<String, Object>> zonePoints = (List<Map<String, Object>>) map.get( "zonePoints" );
		map.remove( "zonePoints" );
		Zone zone = new Zone();
		BeanUtils.setProperties( map, zone );
		validateInsert( zone );
		if( zone.getName() == null || zone.getName().toString().isEmpty() )
			zone.setName( zone.getId().toString() );
		List<List<Double>> points = new LinkedList<>();

        if(map.containsKey("zoneGroup.id") && map.get("zoneGroup.id") != null) {
            ZoneGroup zoneGroup = ZoneGroupService.getInstance().get(Long.parseLong(map.get("zoneGroup.id").toString()));
            if(zoneGroup != null) {
                zoneGroup.getZones().add(zone);
                ZoneGroupService.getInstance().update(zoneGroup);
                zone.setZoneGroup(zoneGroup);
                zone.setLocalMap(zoneGroup.getLocalMap());
            }
        }

        ZoneService.getInstance().insert( zone );

        if(map.containsKey("zoneType")) {
            ZoneTypeService.updatingZoneTypeFromZone(map.get("zoneType"), zone);
        }

		for( Map<String, Object> zonePoint : zonePoints )
		{
			List<Double> point = new LinkedList<>();
			point.add( new Double( zonePoint.get( "x" ).toString() ) );
			point.add( new Double( zonePoint.get( "y" ).toString() ) );
			Double index = points.size() + 0.0;
			if( zonePoint.containsKey( "arrayIndex" ) )
			{
				index = new Double( zonePoint.get( "arrayIndex" ).toString() );
			}
			point.add( index );
			points.add( point );
		}
		if (createRecent){
			RecentService.getInstance().insertRecent(zone.getId(), zone.getName(),"zone",zone.getGroup());
		}

		ZoneService.getInstance().updateZonePoints( zone, points );
		Map<String, Object> publicMap = zone.publicMap();
		return RestUtils.sendCreatedResponse( publicMap );
	}

	@Override
	@PATCH
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@RequiresPermissions(value = { "zone:u:{id}" })
	@ApiOperation(position = 4, value = "Update a Zone")
	public Response updateZone( @PathParam("id") Long id, Map<String, Object> map )
	{
		User currentUser = (User) SecurityUtils.getSubject().getPrincipal();
		Group currentUserGroup = currentUser.getActiveGroup();
		//QueryUtils.filterWritePermissions( Zone.class, map );
		EntityVisibility entityVisibility = getEntityVisibility();

		return RestUtils.sendOkResponse( ZoneService.getInstance().updateZone(id, map ,entityVisibility) );
	}

	@POST
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@RequiresPermissions(value = { "zone:u:{id}" })
	@ApiOperation(position = 4, value = "Update a Zone (POST), it is used only for Hand Held, it does not support PATCH")
	public Response updateZonePost( @PathParam("id") Long id, Map<String, Object> map )
	{
		User currentUser = (User) SecurityUtils.getSubject().getPrincipal();
		Group currentUserGroup = currentUser.getActiveGroup();
		//QueryUtils.filterWritePermissions( Zone.class, map );
		EntityVisibility entityVisibility = getEntityVisibility();

		return RestUtils.sendOkResponse(  ZoneService.getInstance().updateZone(id, map ,entityVisibility) );
	}

	@DELETE
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@RequiresPermissions(value = { "zone:d" })
	@ApiOperation(value = "Delete several Zones")
	public Response deleteZones( Map<String, Object> map )
	{
		try {
			List<Object> ids = (List<Object>) map.get("ids");
			EntityVisibility entityVisibility = getEntityVisibility();
			for (int it = 0; it < ids.size(); it++) {
				if (ids.get(it) != null) {

					Long id = Long.valueOf(ids.get(it).toString());

					Zone zone = ZoneService.getInstance().get(id.longValue());
					if (zone != null) {
						ZoneGroup zoneGroup = zone.getZoneGroup();
						if (zoneGroup != null) {
							zoneGroup.setZones(null);
						}
						GeneralVisibilityUtils.limitVisibilityDelete(entityVisibility, zone);
						validDeleteZone(zone);
					}
				}
			}
		}catch (Exception e){
			return RestUtils.sendBadResponse(e.getMessage());
		}
		return RestUtils.sendDeleteResponse();
	}

	@Override
	public void addToPublicMap(Zone zone, Map<String, Object> publicMap, String extra) {
		if (extra != null && extra.contains("zoneType")) {
			if (zone.getZoneType() != null) {
				List<ZonePropertyValue> ZonePropertyValuesList = ZonePropertyValueService.getInstance().getZonePropertiesByZoneId(zone.getId());
				publicMap.put("zoneType", zone.getZoneType().publicMapProperties(ZonePropertyValuesList));
			}
		}
	}

//	@GET
//    @Path("/{id}")
//    @Produces(MediaType.APPLICATION_JSON)
//    // 1a. Limit access based on CLASS level resources
//    @RequiresPermissions(value={"zone:r:{id}"})
//    @ApiOperation(position=2, value="Select a Zone (AUTO)")
//    public Response selectZones( @PathParam("id") Long id, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only )
//    {
//        Zone zone = ZoneService.getInstance().get( id );
//        if( zone == null )
//        {
//            return RestUtils.sendBadResponse( String.format( "ZoneId[%d] not found", id) );
//        }
//        // 2. Limit visibility based on user's group and the object's group (group based authorization)
//        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(Zone.class.getCanonicalName(), null);
//        VisibilityUtils.limitVisibilitySelect(visibilityGroup, zone.getGroup(), defaultUpVisibility, defaultDownVisibility, null, null, null, null );
//        validateSelect( zone );
//        // 5a. Implement extra
//        Map<String,Object> publicMap = QueryUtils.mapWithExtraFields( zone, extra );
//
//
//        addToPublicMap(zone, publicMap, extra);
//        // 5b. Implement only
//        QueryUtils.filterOnly( publicMap, only, extra );
//        return RestUtils.sendOkResponse( publicMap );
//    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"zone:d:{id}"})
    // 1a. Limit access based on CLASS level resources
    @ApiOperation(position=5, value="Delete a Zone (AUTO)")
    public Response deleteZone( @PathParam("id") Long id )
    {
        // 1c. TODO: Restrict access based on OBJECT level read permissions
        try {
			Zone zone = ZoneService.getInstance().get(id);
			if (zone == null) {
				return RestUtils.sendBadResponse(String.format("ZoneId[%d] not found", id));
			}

			ZoneGroup zoneGroup = zone.getZoneGroup();
			if (zoneGroup != null) {
				zoneGroup.setZones(null);
			}
			// 2. Limit visibility based on user's group and the object's group (group based authorization)
			EntityVisibility entityVisibility = getEntityVisibility();
			GeneralVisibilityUtils.limitVisibilityDelete(entityVisibility, zone);
			// handle validation in an Extensible manner
			validateDelete(zone);
			validDeleteZone(zone);
			RecentService.getInstance().deleteRecent(zone.getId(),"zone");

		} catch (Exception e){
			return RestUtils.sendBadResponse(e.getMessage());
		}
        return RestUtils.sendDeleteResponse();
    }

    public boolean validateDuplicateName( Zone zone )
    {
        List<Zone> zoneList = ZoneService.getInstance().getZonesByName(zone.getName());
        if(zoneList.size() > 1) {
            return false;
        }
        return true;
    }

	//EXPERIMENTAL
	//This is the new code
	@GET
	@Path("/things/count")
	@Produces(MediaType.APPLICATION_JSON)
	@RequiresPermissions(value={"zone:r"})
	@ApiOperation(position=1, value="Get a List of Zones with their things quantity for each zone (Mongo!)")
	public Response countThingsByZone(@QueryParam("pageSize") Integer pageSize, @QueryParam("updateCassandra") boolean updateCassandra)
	{
		ZoneService zoneService = ZoneService.getInstance();
		List<Zone> zones = zoneService.getZones();

		ThingService thingService = ThingService.getInstance();
		Map<String,Long> thingsByZone = thingService.getThingsByZone();
		logger.warn(thingsByZone );

		List<Map<String, Object>> zoneCountMap = new LinkedList<>();
		Map<String,Object> zoneMap;
		long thingsQuantity;
		for (Zone zone : zones)
		{
			zoneMap = new HashMap<>();
			zoneMap.put("id", zone.getId());
			zoneMap.put("name", zone.getName());
			thingsQuantity = 0;
			if (thingsByZone.containsKey(zone.getName()) ) {
				thingsQuantity = thingsByZone.get(zone.getName());
			}

			zoneMap.put("thingsQuantity", thingsQuantity);
			zoneCountMap.add(zoneMap);
		}

		// Adding unknown zone
		String zoneName = "unknown";
		zoneMap = new HashMap<>();
		zoneMap.put("id", 0L);
		zoneMap.put("name", zoneName);
		thingsQuantity = 0;
		if (thingsByZone.containsKey("unknown") ) {
			thingsQuantity = thingsByZone.get("unknown");
		}
		zoneMap.put("thingsQuantity", thingsQuantity);
		zoneCountMap.add(zoneMap);

		Map<String,Object> mapResponse = new HashMap<String,Object>();
		mapResponse.put("total", zoneCountMap.size());
		mapResponse.put("results", zoneCountMap);
		return RestUtils.sendOkResponse(mapResponse);
	}

	//EXPERIMENTAL
	// this is the original code
	/*
	@GET
	@Path("/things/count2")
	@Produces(MediaType.APPLICATION_JSON)
	@RequiresPermissions(value={"zone:r"})
	@ApiOperation(position=1, value="Get a List of Zones with their things quantity for each zone")
	public Response countThingsByZone2(@QueryParam("pageSize") Integer pageSize, @QueryParam("updateCassandra") boolean updateCassandra)
	{
		ZoneService zoneService = ZoneService.getInstance();
		List<Zone> zones = zoneService.getZones();

		ThingTypeFieldService thingFieldService = ThingTypeFieldService.getInstance();
		List<ThingTypeField> thingTypeFields = thingFieldService.getThingTypeFieldByName("zone");
		List<Long> thingTypeFieldIds = new ArrayList<>();
		for(ThingTypeField thingTypeField : thingTypeFields){
			thingTypeFieldIds.add(thingTypeField.getId());
		}

		List<Long> thingTypeIds = new ArrayList<>();
		for(ThingTypeField thingField: thingTypeFields){
			if(!thingTypeIds.contains(thingField.getId())){
				thingTypeIds.add(thingField.getId());
			}
		}

		List<Thing> things = ThingService.getInstance().selectAllThings();
		List<Long> thingIds = new ArrayList<>();
		for(Thing thing: things){
			if(!thingIds.contains(thing.getId())){
				thingIds.add(thing.getId());
			}
		}

//		Map<Long, Map<String, Object>> values = FieldValueService.values(thingTypeFieldIds);
		Map<Long,Map<Long, Map<String, Object>>>  values = FieldValueService.getFieldsValues(thingIds, thingTypeIds);

		List<Map<String, Object>> zoneCountMap = new LinkedList<>();
		Map<String,Object> zoneMap;
		long thingsQuantity;
		for (Zone zone : zones)
		{
			zoneMap = new HashMap<>();
			zoneMap.put("id", zone.getId());
			zoneMap.put("name", zone.getName());
			thingsQuantity = countThings(zone.getName(), values);
			zoneMap.put("thingsQuantity", thingsQuantity);
			zoneCountMap.add(zoneMap);

			if (updateCassandra){
				logger.info("Updating zoneCountTable, zone=" + zone.getName() + ", qty=" + thingsQuantity);
				ZoneCountService.insert(zone.getName(), thingsQuantity);
			}

		}

		// Adding unknown zone
		String zoneName = "unknown";
		zoneMap = new HashMap<>();
		zoneMap.put("id", 0L);
		zoneMap.put("name", zoneName);
		thingsQuantity = countThings(zoneName, values);
		zoneMap.put("thingsQuantity", thingsQuantity);
		zoneCountMap.add(zoneMap);

		if (updateCassandra){
			logger.info("Updating zoneCountTable, zone=" + zoneName + ", qty=" + thingsQuantity);
			ZoneCountService.insert(zoneName, thingsQuantity);
		}

		Map<String,Object> mapResponse = new HashMap<String,Object>();
		mapResponse.put("total", zoneCountMap.size());
		mapResponse.put("results", zoneCountMap);
		return RestUtils.sendOkResponse(mapResponse);
	}
	*/

	private long countThings(String name, Map<Long,Map<Long, Map<String, Object>>>  values) {
		long thingsQuantity = 0;
		for(Map<Long, Map<String, Object>> things : values.values()){
			for (Map<String, Object> fieldValue : things.values()) {
				String value = (String)fieldValue.get("value");
				if (value.equals(name)){
					thingsQuantity++;
				}
			}
		}
		return thingsQuantity;
	}

	/**
	 *
	 * @param id Zone Id
	 * @return Returns status code:
	 * 204 : Executed successfully and noresponsee message
	 * 428 : No camera has been found in zone
	 * 400 : An error has been occurred during execution
	 * 503 : Video server not found
	 */
	@PUT
	@Path("/{id}/video")
	@Produces(MediaType.APPLICATION_JSON)
	@RequiresAuthentication
	@ApiOperation(position = 1, value = "Sends request to display camera feed to security desk tile")
	public Response assignFeed( @PathParam("id") Long id){
		ReportDefinitionController rdc = new ReportDefinitionController();
		rdc.validateListPermissions();
		Response outcome;

		try {
			ZoneService.getInstance().assignFeedToCamera(id);
			outcome =  RestUtils.sendEmptyResponse();
		}catch (ZoneHasNoCameraException e) {
			outcome = RestUtils.sendResponseWithCode(e.getMessage(), 428);
		}catch (InvalidVideoFeedException | IllegalArgumentException e) {
			outcome = RestUtils.sendBadResponse( e.getMessage() );
		} catch (UnknownHostException e) {
			outcome = RestUtils.sendResponseWithCode(e.getMessage(), 503);
		} catch (UserIsNotOperatorException e){
			outcome =  RestUtils.sendResponseWithCode(e.getMessage(), 501);
		}

		return outcome;
	}

	/**
	 *
	 * @param name Zone Name
	 * @return Returns status code:
	 * 204 : Executed successfully and noresponsee message
	 * 428 : No camera has been found in zone
	 * 400 : An error has been occurred during execution or Zone name not found
	 * 503 : Video server not found
	 */
	@PUT
	@Path("/name/{name}/video")
	@Produces(MediaType.APPLICATION_JSON)
	@RequiresAuthentication
	@ApiOperation(position = 1, value = "Sends request to display camera feed to security desk tile (by zone name)")
	public Response assignFeed(@PathParam("name") String name) {
		ReportDefinitionController rdc = new ReportDefinitionController();
		rdc.validateListPermissions();
		Response outcome;

		if (name == null || name.isEmpty()) {
			throw new IllegalArgumentException("Zone Name can not be null");
		}

		List<Zone> zones = ZoneService.getInstance().getZonesByName(name);
		if (zones.size() == 0) {
			outcome = RestUtils.sendBadResponse(String.format("ZoneName[%s] not found", name));
		} else
			try {
				ZoneService.getInstance().assignFeedToCamera(zones.get(0).getId());
				outcome = RestUtils.sendEmptyResponse();
			} catch (ZoneHasNoCameraException e) {
				outcome = RestUtils.sendResponseWithCode(e.getMessage(), 428);
			} catch (InvalidVideoFeedException | IllegalArgumentException e) {
				outcome = RestUtils.sendBadResponse(e.getMessage());
			} catch (UnknownHostException e) {
				outcome = RestUtils.sendResponseWithCode(e.getMessage(), 503);
			}catch (UserIsNotOperatorException e){
				outcome =  RestUtils.sendResponseWithCode(e.getMessage(), 501);
			}

		return outcome;
	}

	@POST
	@Path("/migrateZone")
	//@Produces(MediaType.APPLICATION_JSON)
	//@Consumes(MediaType.APPLICATION_JSON)
	@ApiOperation(position = 1, value = "replace zoneId instead zoneName")
	public Response migrateCassandra() {
		Response result = null;
		try {
			List<Zone> zones = ZoneService.getZoneDAO().selectAll();
			String propertyName = "zone.value";
			for (Zone zone : zones){
				ThingMongoDAO.getInstance().update(propertyName, zone.getName(), String.valueOf(zone.getId()) );
			}
			// unknown zones
			ThingMongoDAO.getInstance().update(propertyName, "unknown", "0" );
		}
		catch (Exception e) {
			return RestUtils.sendBadResponse("error migrating zone " + e.getMessage());
		}
		return RestUtils.sendOkResponse("success migrating zone");
	}

	@GET
	@Path("/tree")
	@Produces(MediaType.APPLICATION_JSON)
	@RequiresAuthentication
	@ApiOperation(position = 1, value = "Get a List of Zones in Tree")
	public Response listZonesTree(@QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber,
								  @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra,
								  @Deprecated @QueryParam("only") String only, @QueryParam("visibilityGroupId") Long visibilityGroupId,
								  @DefaultValue("") @QueryParam("upVisibility") String upVisibility,
								  @DefaultValue("") @QueryParam("downVisibility") String downVisibility,
								  @DefaultValue("") @QueryParam("mapUpVisibility") String mapUpVisibility,
								  @DefaultValue("") @QueryParam("mapDownVisibility") String mapDownVisibility,
								  @DefaultValue("") @QueryParam("zoneUpVisibility") String zoneUpVisibility,
								  @DefaultValue("") @QueryParam("zoneDownVisibility") String zoneDownVisibility,
								  @DefaultValue("true") @QueryParam("enableMultilevel") String enableMultilevel,
								  @QueryParam("topId") String topId,
								  @QueryParam("ownerName") String ownerName){


		Map<String, Object> mapResponse = new HashMap<>();
		Long count;

		try{
			GroupController c = new GroupController();
			LocalMapControllerBase mapController = new LocalMapController();
			Group visibilityGroup = VisibilityUtils.getVisibilityGroup( Zone.class.getCanonicalName(), visibilityGroupId );
			Pagination pagination = new Pagination(
					(enableMultilevel!=null && enableMultilevel.trim().equals( "true" ))?1:pageNumber,
					(enableMultilevel!=null && enableMultilevel.trim().equals( "true" ))?1000:pageSize);
			EntityVisibility entityVisibility = c.getEntityVisibility();
			boolean up = StringUtils.isEmpty(upVisibility) ? entityVisibility.isNotFalseUpVisibility() : entityVisibility.isNotFalseUpVisibility() && Boolean.parseBoolean(upVisibility);
			boolean down = StringUtils.isEmpty(downVisibility) ? entityVisibility.isDownVisibility() : entityVisibility.isDownVisibility() && Boolean.parseBoolean(downVisibility);
			BooleanBuilder be = new BooleanBuilder();
			be = be.and( GeneralVisibilityUtils.limitVisibilitySelectAll(entityVisibility, QGroup.group, visibilityGroup, upVisibility, downVisibility ) );

			int treeLevel = visibilityGroup.getTreeLevel();

			Group topGroup = null;
			User currentUser = (User) SecurityUtils.getSubject().getPrincipal();

			if (StringUtils.isNotEmpty(topId)){
				topGroup =  GroupService.getInstance().get(Long.parseLong(topId));
			}else{
				topGroup = currentUser.getActiveGroup();
				if ((visibilityGroup.getId() < topGroup.getId())) {
					topGroup = visibilityGroup;
				}
			}
			// Setting default order
			if ((order == null) || (order.isEmpty())) {
				order = "name:asc";
			}
			TreeParameters<Group> treeParameters = new TreeParameters<>();
			treeParameters.setOnly(only);
			treeParameters.setExtra(extra);
			treeParameters.setOrder(order);
			treeParameters.setTopObject(topGroup);
			treeParameters.setVisibilityGroup(visibilityGroup);
			treeParameters.setUpVisibility(up);
			treeParameters.setDownVisibility(down);

			if (StringUtils.isEmpty(where)){
				if (topGroup == null) {
					be = be.and(QGroup.group.parent.isNull());
				} else {
					be = be.and(QGroup.group.id.eq(topGroup.getId()));
				}
				count = GroupService.getInstance().countList( be );
				List<Group> groupList = GroupService.getInstance().listPaginated( be, pagination, order );
				for (Group group : groupList) {
					c.mapGroup(group, treeParameters, treeLevel, false);
					c.addAllDescendants(group, treeParameters, treeLevel, false, up, down);
				}
			}else{
				order = "treeLevel:asc,"+order;
				count = GroupService.getInstance().countList( be );
				List<Group> groupList = GroupService.getInstance().listPaginated( be, pagination, order );
				for (Group group : groupList) {
					c.mapGroup(group, treeParameters, treeLevel, true);
				}
			}

			List<Map<String, Object>> list = treeParameters.getList();

			EntityVisibility mapVisibility = mapController.getEntityVisibility();
			Group mapVisibilityGroup = VisibilityUtils.getVisibilityGroup(LocalMap.class.getCanonicalName(), visibilityGroupId);
			BooleanBuilder mapBe = new BooleanBuilder();
			mapBe = mapBe.and(GeneralVisibilityUtils.limitVisibilitySelectAll(mapVisibility, QLocalMap.localMap,  mapVisibilityGroup, mapUpVisibility, mapDownVisibility ));
			Group zoneVisibilityGroup = VisibilityUtils.getVisibilityGroup(Zone.class.getCanonicalName(), visibilityGroupId);
			EntityVisibility zoneVisibility =  getEntityVisibility();
			BooleanBuilder zoneBe = new BooleanBuilder();
			zoneBe = zoneBe.and(GeneralVisibilityUtils.limitVisibilitySelectAll(zoneVisibility, QZone.zone,  zoneVisibilityGroup, zoneUpVisibility, zoneDownVisibility ));
			zoneBe = zoneBe.and( QueryUtils.buildSearch( QZone.zone, where ) );
			for (Map map:list ) {
				map.put("mapMaker", ZoneService.getInstance().fillChildren(map, mapBe,zoneBe, where, ownerName));
				Group group = GroupService.getGroupDAO().selectById((Long)map.get("id"));
				if (ownerName != null) {
					String owner = GroupFieldService.getInstance().getOwnershipValue(group, ownerName);
					map.put("ownership", owner);
				}

			}
			mapResponse.put( "total", count );
			mapResponse.put( "results", list );

		}catch (Exception e){
			return RestUtils.sendResponseWithCode(e.getMessage() , 400);
		}
		return RestUtils.sendOkResponse(mapResponse);
	}

	public static void validDeleteZone (Zone zone) {
		List<String> messageErrors = ZoneService.getInstance().deleteCurrentZone(zone, true, true, true);
		if (messageErrors.size() > 0) {
			for (int position = 0; position < messageErrors.size(); position++) {
				messageErrors.set(position, "Zone could not be removed because there are Zone(s) that " + messageErrors.get(position));
			}
			throw new UserException(StringUtils.join(messageErrors, ", "));
		}
	}


	@POST
	@Path("/gridNames")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@RequiresPermissions(value = { "zone:i" })
	@ApiOperation(position = 1, value = "Get a list of names for grid zones")
	public Response listGridNames
             (Map<String, Object> body) {
        try {
            Map<String, Object> mapResponse = ZoneService.getInstance().getGridZoneNames(body);
            return RestUtils.sendOkResponse(mapResponse.get("gridZoneNames"));
        } catch (UserException e) {
            return RestUtils.sendBadResponse(e.getMessage());
        }
	}

	@GET
	@Path("/zonePoints")
	@Produces(MediaType.APPLICATION_JSON)
	@RequiresPermissions(value = { "zone:i" })
	@ApiOperation(position = 1, value = "Get a list of zone Points for grid zones")
	public Response listZonePoints
			(@QueryParam("rowSize") Integer rowSize
					, @QueryParam("columnSize") Integer columnSize
					, @QueryParam("facilityId") Long facilityId
					, @QueryParam("zoneWidth") Double zoneWidth
					, @QueryParam("zoneHeight") Double zoneHeight
					, @QueryParam("gapWidth") Double gapWidth
					, @QueryParam("gapHeight") Double gapHeight
					, @QueryParam("zoneLatitude") Double zoneLatitude
					, @QueryParam("zoneLongitude") Double zoneLongitude) {
		try {
			Map<String, Object> mapResponse = ZoneService.getInstance().getZonePoints(rowSize, columnSize,
					zoneWidth, zoneHeight, gapWidth,gapHeight,
					zoneLatitude, zoneLongitude, facilityId);
			return RestUtils.sendOkResponse(mapResponse.get("gridZonePoints"));
		} catch (UserException e) {
			return RestUtils.sendBadResponse(e.getMessage());
		}
	}

	@POST
	@Path("/validateCodeNewZone")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@ApiOperation(position = 1, value = "Validate new zone name")
	public Response validateNameNewZone(Map<String, Object> body) {
        try {
            if (ZoneService.getInstance().isValidNewZoneCodeAndNewName(body, false)) {
                return RestUtils.sendOkResponse("OK");
            } else {
                return RestUtils.sendBadResponse("[newName or newCode] is invalid.");
            }
        } catch (UserException e) {
            return RestUtils.sendBadResponse(e.getMessage());
        }
	}
}
