package com.tierconnect.riot.iot.controllers;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.controllers.GroupController;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.QGroup;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.services.GroupFieldService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.RecentService;
import com.tierconnect.riot.appcore.utils.*;
import com.tierconnect.riot.commons.utils.CoordinateUtils;
import com.tierconnect.riot.iot.entities.LocalMap;
import com.tierconnect.riot.iot.entities.QLocalMap;
import com.tierconnect.riot.iot.entities.Zone;
import com.tierconnect.riot.iot.entities.ZoneGroup;
import com.tierconnect.riot.iot.services.BrokerClientHelper;
import com.tierconnect.riot.iot.services.LocalMapService;
import com.tierconnect.riot.iot.services.ZoneGroupService;
import com.tierconnect.riot.sdk.dao.Pagination;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.utils.BeanUtils;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.wordnik.swagger.annotations.*;
import com.wordnik.swagger.jaxrs.PATCH;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.Charsets;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jose4j.json.internal.json_simple.JSONObject;


import javax.annotation.security.PermitAll;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

@Path("/localMap")
@Api("/localMap")

public class LocalMapController extends  LocalMapControllerBase
{
	/**
	 * LIST
	 */
	public void validateSelect( LocalMap localMap )
	{
	}

	@PATCH
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@RequiresAuthentication
	@ApiOperation(position = 1, value = "Insert or update LocalMaps in bulk")
	public Response changeMapsByJson(List<Map<String, Object>> listMap){

		List<Map<String, Object>> listMapResponse;
		try {
			EntityVisibility entityVisibility = getEntityVisibility();
			listMapResponse = LocalMapService.getInstance().bulkOperation(listMap, entityVisibility);

			JSONObject bulkResponse = new JSONObject();

			int errorCount = 0;
			int successCount = 0;
			for (Map<String, Object> localMap : listMapResponse) {

				if(localMap.get("status").equals("200")){
					successCount++;
				}
				if(localMap.get("status").equals("400")){

					errorCount++;
				}
			}

			bulkResponse.put("errorCount", errorCount);
			bulkResponse.put("successCount", successCount);
			bulkResponse.put("listResponse", listMapResponse);

			JSONObject jsonObject = new JSONObject(bulkResponse);

			Response response = RestUtils.sendJSONResponseWithCode(jsonObject.toJSONString() ,207);

			return response;
		}
		catch (Exception e){
			return RestUtils.sendBadResponse(e.getMessage());
		}

	}

	@GET
	@Path("/maximus")
	@Produces(MediaType.APPLICATION_JSON)
	@RequiresAuthentication
	@ApiOperation(position = 5, value = "Calculate maximum latitude and longitude of a localMap")
	public Response calculateMax(@QueryParam("lonMin") Double lonMin, @QueryParam("latMin") Double latMin,
								 @QueryParam("height") Double height, @QueryParam("width") Double width,
								 @QueryParam("imageUnit") String imageUnit){
		LocalMap localMap = new LocalMap();
		if (width != null) {
			localMap.setImageWidth(width);
		}else{
			throw new UserException("Facility map width is required");
		}
		if (height != null){
			localMap.setImageHeight(height);
		}else{
			throw new UserException("Facility map height is required");
		}
		if (lonMin != null){
			localMap.setLonmin(lonMin);
		}else{
			throw new UserException("Facility map minimum longitude is required");
		}
		if (latMin != null){
			localMap.setLatmin(latMin);
		}else{
			throw new UserException("Facility map minimum latitude is required");
		}
		if (imageUnit != null){
			localMap.setImageUnit(imageUnit);
		}else{
			throw new UserException("Facility map image Unit is required");
		}
		LocalMapService.calculateLatLonMax(localMap);
		Map<String,Object> responseMap = new HashMap<>();
		responseMap.put("lonMax", localMap.getLonmax());
		responseMap.put("latMax", localMap.getLatmax());
		return RestUtils.sendOkResponse(responseMap);
	}

    // This method was deprecated on 2017/03/28
    // Remove it, when localMap's resize by left panel is working correctly
    @POST
    @Path("/resizeMapPoints")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 5, value = "Calculate maximum latitude and longitude of a localMap")
    @Deprecated
    public Response resizeMapPoints(Map<String, Object> body) {
        Map<String, Object> responseMap = LocalMapService.calculateLatLonMax(body);
        return RestUtils.sendOkResponse(responseMap);
    }

	@Override
	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	@RequiresPermissions(value={"localMap:r"})
	@ApiOperation(position=1, value="Get a List of LocalMaps")
	public Response listLocalMaps(@QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber,
								  @QueryParam("order") String order, @QueryParam("where") String where,
								  @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only,
								  @QueryParam("visibilityGroupId") Long visibilityGroupId,
								  @QueryParam("upVisibility") @DefaultValue("") String upVisibility,
								  @QueryParam("downVisibility") @DefaultValue("") String downVisibility,
								  @QueryParam("returnFavorite") @DefaultValue("false") boolean returnFavorite,
								  @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend,
								  @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project) {
		return super.listLocalMaps(pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility, downVisibility, returnFavorite, extend, project);
	}

	@PUT
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	// 1a. Limit access based on CLASS level resources
    @RequiresAuthentication
	//@RequiresPermissions(value={"localMap:i"})
	@ApiOperation(position=3, value="Insert a LocalMap")
	public Response insertLocalMap( Map<String, Object> map, @QueryParam("createRecent") @DefaultValue("false") Boolean createRecent )
	{
		if (!LocalMapService.getInstance().isValidNewLocalMapName(map.get("name").toString(),
				Long.parseLong (((Map<String, Object>)map.get("group")).get("id").toString()), null)) {
			return RestUtils.sendBadResponse("Facility Map named '" + map.get("name") + "' already exists in Tenant Group");
		}

		// 2. Limit visibility based on user's group and the object's group (group based authorization)
		EntityVisibility entityVisibility = getEntityVisibility();
		GeneralVisibilityUtils.limitVisibilityInsert(entityVisibility, VisibilityUtils.getObjectGroup(map));
		// 1[b,c]. Restrict access based on OBJECT and PROPERTY level write permissions
		//QueryUtils.filterWritePermissions( LocalMap.class, map );
		LocalMap localMap = new LocalMap();

		String imageName = "";
        String inputStream = "";
        if(map.get("image") != null) {
			imageName = new String((String) map.get("image"));
            inputStream = imageName.substring(22);
        }

		if(localMap.getName() != null && localMap.getName().length() > 255){
			throw new UserException("Facility map name is too long, please use a name with less than 255 characters.");
		}

		byte [] decodedBytes=null;
		
			decodedBytes = Base64.decodeBase64(inputStream.getBytes(Charsets.UTF_8));
			
		localMap.setName((String) map.get("name"));
		localMap.setLonmin(new Double(map.get("lonmin").toString()));
		localMap.setLatmin(new Double(map.get("latmin").toString()));


		if(map.get("altOrigin")!=null)
			localMap.setAltOrigin(new Double(map.get("altOrigin").toString()));
		if(map.get("declination")!=null)
			localMap.setDeclination(new Double(map.get("declination").toString()));
		if(map.get("latOrigin")!=null)
			localMap.setLatOrigin(new Double(map.get("latOrigin").toString()));
		if(map.get("lonOrigin")!=null)
			localMap.setLonOrigin(new Double(map.get("lonOrigin").toString()));
        if(map.get("imageWidth")!=null)
            localMap.setImageWidth(new Double(map.get("imageWidth").toString()));
        if(map.get("imageHeight")!=null)
            localMap.setImageHeight(new Double(map.get("imageHeight").toString()));
		if (map.get("opacity") != null)
			localMap.setOpacity(Long.parseLong(map.get("opacity").toString()));

        List<Map<String, Object>> mapPoints = (List<Map<String, Object>>)map.get("mapPoints");
        map.remove("mapPoints");

		localMap.setDescription((String)map.get("description"));
		Group group;

		if(map.containsKey("group")) {
			System.out.println(map.get("group"));
            Map<String, Object> groupObject = (Map<String, Object>) map.get("group");
            Long groupId = new Long( (Integer) (groupObject.get( "id" ) ) );
            group = GroupService.getInstance().get(groupId);
		}else {
			User currentUser = (User) SecurityUtils.getSubject().getPrincipal();
            group = VisibilityUtils.getVisibilityGroup(Zone.class.getCanonicalName(), null);
		}
		localMap.setImage(decodedBytes);
		
		// 7. handle insert and update
		BeanUtils.setProperties( map, localMap );

        //Calculating latMax and lonMax
        LocalMapService.calculateLatLonMax(localMap);

		// 6. handle validation in an Extensible manner
		validateInsert( localMap );
		LocalMapService.getInstance().insert( localMap );

		// save local map points
		LocalMapService.getInstance().updateMapPoints(localMap, mapPoints);

		//Adding two Groups when a zone is created

        List<ZoneGroup> zoneGroups = new LinkedList<>();

        ZoneGroup zoneGroupOnSite = new ZoneGroup();
        zoneGroupOnSite.setDescription((localMap.getName()!= null? localMap.getName().length() > 246?localMap.getName():localMap.getName()+ "On-Site":""+ "On-Site"));
        zoneGroupOnSite.setName("On-Site");
        zoneGroupOnSite.setGroup(group);
        zoneGroupOnSite.setLocalMap(localMap);

        ZoneGroup zoneGroupOffSite = new ZoneGroup();
        zoneGroupOffSite.setDescription((localMap.getName()!= null?localMap.getName().length() > 246?localMap.getName():localMap.getName()+" Off-Site":""+" Off-Site") );
        zoneGroupOffSite.setName("Off-Site");
        zoneGroupOffSite.setGroup(group);
        zoneGroupOffSite.setLocalMap(localMap);

        zoneGroups.add(zoneGroupOnSite);
        zoneGroups.add(zoneGroupOffSite);

        localMap.setZoneGroup(zoneGroups);

        ZoneGroupService.getInstance().insert(zoneGroupOnSite);
        ZoneGroupService.getInstance().insert(zoneGroupOffSite);

        BrokerClientHelper.sendRefreshFacilityMapsMessage(
				false, GroupService.getInstance().getMqttGroups(localMap.getGroup()));
		if (createRecent){
			RecentService.getInstance().insertRecent(localMap.getId(), localMap.getName(), "localmap", localMap.getGroup());
		}

		Map<String,Object> publicMap = localMap.publicMap();
		return RestUtils.sendCreatedResponse( publicMap );
	}

//    @DELETE
//    @Path("/{id}")
//    @Produces(MediaType.APPLICATION_JSON)
//    @RequiresPermissions(value={"localMap:d:{id}"})
//    // 1a. Limit access based on CLASS level resources
//    @ApiOperation(position=5, value="Delete a LocalMap (AUTO)")
//    public Response deleteLocalMap( @PathParam("id") Long id )
//    {
//        // 1c. TODO: Restrict access based on OBJECT level read permissions
//        LocalMap localMap = LocalMapService.getInstance().get( id );
//        if( localMap == null )
//        {
//            return RestUtils.sendBadResponse( String.format( "LocalMapId[%d] not found", id) );
//        }
//        // 2. Limit visibility based on user's group and the object's group (group based authorization)
//        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(LocalMap.class.getCanonicalName());
//        VisibilityUtils.limitVisibilityDelete(visibilityGroup, localMap.getGroup() );
//        // handle validation in an Extensible manner
//        validateDelete( localMap );
//        LocalMapService.getInstance().delete( localMap );
//        return RestUtils.sendDeleteResponse();
//    }

	public void validateInsert( LocalMap localMap )
	{

	}


	@PATCH
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	// 1a. Limit access based on CLASS level resources
    @RequiresAuthentication
	//@RequiresPermissions(value={"localMap:u:{id}"})
	@ApiOperation(position=4, value="Update a LocalMap")
	public Response updateLocalMap( @PathParam("id") Long id, Map<String, Object> map )
	{
		if (!LocalMapService.getInstance().isValidNewLocalMapName(map.get("name").toString(),
				Long.parseLong (((Map<String, Object>)map.get("group")).get("id").toString()),
				Long.parseLong (map.get("id").toString()))){
			return RestUtils.sendBadResponse("Facility Map named '" + map.get("name") + "' already exists in Tenant Group");
		}
		// 1[b,c]. Restrict access based on OBJECT and PROPERTY level write permissions
		//QueryUtils.filterWritePermissions( LocalMap.class, map );
		LocalMap localMap = LocalMapService.getInstance().get( id );
		if( localMap == null )
		{
			return RestUtils.sendBadResponse( String.format( "LocalMapId[%d] not found", id) );
		}
		String imageName=new String((String) map.get("image"));
		//
		String inputStream =imageName.substring(22);
		
		
		byte [] decodedBytes=null;
		
			decodedBytes = Base64.decodeBase64(inputStream.getBytes(Charsets.UTF_8));
			
		
		
		localMap.setName((String) map.get("name"));
		localMap.setDescription((String)map.get("description"));
		localMap.setLonmin(new Double(map.get("lonmin").toString()));
		localMap.setLonmax(new Double(map.get("lonmax").toString()));
		localMap.setLatmin(new Double(map.get("latmin").toString()));
		localMap.setLatmax(new Double(map.get("latmax").toString()));
        if(map.get("altOrigin")!=null)
            localMap.setAltOrigin(new Double(map.get("altOrigin").toString()));
        if(map.get("declination")!=null)
            localMap.setDeclination(new Double(map.get("declination").toString()));
        if(map.get("latOrigin")!=null)
            localMap.setLatOrigin(new Double(map.get("latOrigin").toString()));
        if(map.get("lonOrigin")!=null)
            localMap.setLonOrigin(new Double(map.get("lonOrigin").toString()));
        if(map.get("imageWidth")!=null)
            localMap.setImageWidth(new Double(map.get("imageWidth").toString()));
        if(map.get("imageHeight")!=null)
            localMap.setImageHeight(new Double(map.get("imageHeight").toString()));

        List<Map<String, Object>> mapPoints = (List<Map<String, Object>>)map.get("mapPoints");
        map.remove("mapPoints");
        LocalMapService.getInstance().updateMapPoints(localMap, mapPoints);
		RecentService.getInstance().updateName(localMap.getId(), localMap.getName(),"localmap");
		
		Group group;
		if(map.containsKey("group.id")) {
			group = GroupService.getInstance().get(((Number)map.get("group.id")).longValue());
		}else {
			User currentUser = (User) SecurityUtils.getSubject().getPrincipal();
            group = VisibilityUtils.getVisibilityGroup(Zone.class.getCanonicalName(), null);
		}
		localMap.setImage(decodedBytes);
		// 2. Limit visibility based on user's group and the object's group (group based authorization)
		EntityVisibility entityVisibility = getEntityVisibility();
		GeneralVisibilityUtils.limitVisibilityUpdate( entityVisibility, localMap, VisibilityUtils.getObjectGroup(map));

		// 7. handle insert and update
		BeanUtils.setProperties( map, localMap );

        LocalMapService.calculateLatLonMax(localMap);
		// 6. handle validation in an Extensible manner
		validateUpdate( localMap );
		LocalMapService.getInstance().updateFavorite(localMap);

        BrokerClientHelper.sendRefreshFacilityMapsMessage(
				false, GroupService.getInstance().getMqttGroups(localMap.getGroup()));

		Map<String,Object> publicMap = localMap.publicMap();
		return RestUtils.sendOkResponse( publicMap );
	}

	public void validateUpdate( LocalMap localMap )
	{

	}

	@DELETE
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	//@RequiresPermissions(value={"localMap:d:{id}"})
    @RequiresAuthentication
	// 1a. Limit access based on CLASS level resources
	@ApiOperation(position=5, value="Delete a LocalMap")
	public Response deleteLocalMap( @PathParam("id") Long id, @QueryParam("cascadeDelete") @DefaultValue("false") boolean cascadeDelete)
	{
		// 1c. TODO: Restrict access based on OBJECT level read permissions
		LocalMap localMap = LocalMapService.getInstance().get( id );
		if( localMap == null )
		{
			return RestUtils.sendBadResponse( String.format( "LocalMapId[%d] not found", id) );
		}
		// 2. Limit visibility based on user's group and the object's group (group based authorization)
		EntityVisibility entityVisibility = getEntityVisibility();
		GeneralVisibilityUtils.limitVisibilityDelete(entityVisibility, localMap);
		List<Long> groupMqtt = GroupService.getInstance().getMqttGroups(localMap.getGroup());
		// handle validation in an Extensible manner
		validateDelete( localMap );
		RecentService.getInstance().deleteRecent(id,"localmap");
		List<String> messageErrors = LocalMapService.getInstance().deleteCurrentLocalMap(localMap, cascadeDelete);
		if (!messageErrors.isEmpty()) {
			return RestUtils.sendBadResponse(StringUtils.join(messageErrors, ","));
		}else{
			BrokerClientHelper.sendRefreshFacilityMapsMessage(false, groupMqtt);
			return RestUtils.sendDeleteResponse();
		}

	}

	public void validateDelete( LocalMap localMap )
	{

	}

	public void addToPublicMap(Map<String,Object> publicMap )
	{
	}


	@GET
	@Path("/tree")
	@Produces(MediaType.APPLICATION_JSON)
	@RequiresAuthentication
	@ApiOperation(position = 1, value = "Get a List of Local Maps in Tree",notes="Get a List of Local Maps in Tree")
	@ApiResponses(
			value = {
					@ApiResponse(code = 200, message = "Ok"),
					@ApiResponse(code = 400, message = "Bad Request"),
					@ApiResponse(code = 403, message = "Forbidden"),
					@ApiResponse(code = 500, message = "Internal Server Error")
			}
	)
	public Response listZonesTree(@ApiParam(value = "The number of things per page (default 10).") @QueryParam("pageSize") Integer pageSize,
								  @ApiParam(value = "The page number you want to be returned (the first one is displayed by default).") @QueryParam("pageNumber") Integer pageNumber,
								  @ApiParam(value = "The field to be used to sort the thing results. This can be asc or desc. i.e. name:asc") @QueryParam("order") String order,
								  @ApiParam(value = "A filtering parameter to get specific facilities.") @QueryParam("where") String where,
								  @ApiParam(value = "Add extra fields to the response. i.e parent, group, thingType, group.groupType, group.parent") @Deprecated @QueryParam("extra") String extra,
								  @ApiParam(value = "The listed fields will be included in the response. i.e.  only= id,name,code") @Deprecated @QueryParam("only") String only,
								  @ApiParam(value = "It is used to overridden default visibilityGroup to a lower group.") @QueryParam("visibilityGroupId") Long visibilityGroupId,
								  @ApiParam(value = "It is used to disable upVisibility. It can have True or False values") @DefaultValue("") @QueryParam("upVisibility") String upVisibility,
								  @ApiParam(value = "It is used to disable downVisibility. It can have True or False values") @DefaultValue("") @QueryParam("downVisibility") String downVisibility,
								  @ApiParam(value = "It is used to disable upVisibility of localMap. It can have True or False values") @DefaultValue("") @QueryParam("mapUpVisibility") String mapUpVisibility,
								  @ApiParam(value = "It is used to disable downVisibility of localMap. It can have True or False values") @DefaultValue("") @QueryParam("mapDownVisibility") String mapDownVisibility,
								  @ApiParam(value = "It is used to able many levels. It can have True or False values") @DefaultValue("true") @QueryParam("enableMultilevel") String enableMultilevel,
								  @ApiParam(value = "It is the group Id of the root in the tree of groups.") @QueryParam("topId") String topId,
								  @ApiParam(value = "It is the name of the field used to search ownership") @QueryParam("ownerName") String ownerName){


		Map<String, Object> mapResponse = new HashMap<>();
		Long count;

		try{
			GroupController c = new GroupController();
			Group visibilityGroup = VisibilityUtils.getVisibilityGroup( LocalMap.class.getCanonicalName(), visibilityGroupId );
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

			EntityVisibility mapVisibility = getEntityVisibility();
			Group mapVisibilityGroup = VisibilityUtils.getVisibilityGroup(LocalMap.class.getCanonicalName(), visibilityGroupId);
			BooleanBuilder mapBe = new BooleanBuilder();
			mapBe = mapBe.and(GeneralVisibilityUtils.limitVisibilitySelectAll(mapVisibility, QLocalMap.localMap,  mapVisibilityGroup, mapUpVisibility, mapDownVisibility ));
			mapBe = mapBe.and( QueryUtils.buildSearch( QLocalMap.localMap, where ) );
			for (Map map:list ) {
				map.put("mapMaker", LocalMapService.getInstance().fillChildren(map, mapBe, where, ownerName));

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

    @GET
    @Path("/calculateOrigin")
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    @ApiOperation(position = 1, value = "Calculate origin point")
    public Response calculateOrigin(@QueryParam("imageUnit") String imageUnit, @QueryParam("latitude") Double latitude,
                                  @QueryParam("longitude") Double longitude,
                                  @QueryParam("xValue") Double xValue, @QueryParam("yValue") Double yValue) {
        Map<String, Object> mapResponse = LocalMapService.calculateOrigin(imageUnit, latitude, longitude, xValue, yValue);
        if (mapResponse.containsKey("error")) {
            return RestUtils.sendBadResponse(mapResponse.get("error").toString());
        }
        return RestUtils.sendOkResponse(mapResponse);
    }

    @POST
    @Path("/calculateOriginWithPoints")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 5, value = "Calculate origin point using old and new values from a Local Map")
    public Response calculateOriginWithPoints(Map<String, Object> body) {
        Map<String, Object> responseMap = LocalMapService.calculateOriginWithPoints(body);
        return RestUtils.sendOkResponse(responseMap);
    }

    @GET
    @Path("/{id}/calculateScale")
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    @ApiOperation(position = 1, value = "Get scale")
    public Response setScale(@PathParam("id") Long id
            , @QueryParam("pointA") String pointA
            , @QueryParam("pointB") String pointB
            , @QueryParam("distance") Double distance) {
        Map<String, Object> result = LocalMapService.setScale(id, pointA, pointB, distance);
        if (result.containsKey("error")) {
            return RestUtils.sendBadResponse(result.get("error").toString());
        }
        return RestUtils.sendOkResponse(result);
    }

	@GET
	@Path("/resize")
	@Produces(MediaType.APPLICATION_JSON)
	@RequiresAuthentication
	@ApiOperation(position = 6, value = "Calculate width and height of a local map after a resize")
	public Response calculateResize(@QueryParam("lonMin") Double lonMin, @QueryParam("latMin") Double latMin,
								    @QueryParam("altMin") Double altMin, @QueryParam("lonMax") Double lonMax,
									@QueryParam("latMax") Double latMax,@QueryParam("declination") Double declination,
									@QueryParam("imageUnit") String imageUnit,
									@QueryParam("localMapId") Long id){

		if (lonMin == null){
			throw new UserException("Longitude Min value is required");
		}

		if (latMin == null){
			throw new UserException("Latitude Min value is required");
		}

		if (altMin == null){
			throw new UserException("Altitude Min value is required");
		}

		if (lonMax == null){
			throw new UserException("Longitude Max value is required");
		}

		if (latMax == null){
			throw new UserException("Latitude Max value is required");
		}

		if (declination == null){
			throw new UserException("Declination value is required");
		}

		if (imageUnit == null){
			throw new UserException("Image Unit value is required");
		}

		CoordinateUtils cu = new CoordinateUtils(lonMin, latMin, altMin,declination, imageUnit);
		double[] measure = cu.lonlat2xy(lonMax,latMax,altMin);
		Map<String,Object> mapResponse = new HashMap<>();
		mapResponse.put("width", Math.abs(measure[0]));
		mapResponse.put("height", Math.abs(measure[1]));
		return RestUtils.sendOkResponse(mapResponse);
	}

	@POST
	@Path("/validateNameNewLocalMap")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@ApiOperation(position = 1, value = "Validate new localMap name")
	public Response validateNameNewLocalMap(Map<String, Object> body) {
		try {
			if (LocalMapService.getInstance().isValidNewLocalMapName(body)) {
				return RestUtils.sendOkResponse("OK");
			} else {
				return RestUtils.sendBadResponse("[newName] is invalid.");
			}
		} catch (UserException e) {
			return RestUtils.sendBadResponse(e.getMessage());
		}
	}

	@GET
	@Path("/validate/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@RequiresAuthentication
	@ApiOperation(value="Validation when local map is deleted")
	public Response validateDeleteLocalMap( @PathParam("id") Long id )
	{
		LocalMap localMap = LocalMapService.getInstance().get( id );
		if( localMap == null ){
			return RestUtils.sendBadResponse( String.format( "LocalMapId[%d] not found", id) );
		}
		// Limit visibility based on user's group and the object's group (group based authorization)
		EntityVisibility entityVisibility = getEntityVisibility();
		GeneralVisibilityUtils.limitVisibilityDelete(entityVisibility, localMap);

		return RestUtils.sendOkResponse(LocalMapService.getInstance().validateDeleteLocalMap(id));
	}

}


