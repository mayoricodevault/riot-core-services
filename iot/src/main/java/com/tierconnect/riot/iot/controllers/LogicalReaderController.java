package com.tierconnect.riot.iot.controllers;

import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.RecentService;
import com.tierconnect.riot.iot.entities.LogicalReader;
import com.tierconnect.riot.iot.entities.Zone;
import com.tierconnect.riot.iot.services.LogicalReaderService;
import com.tierconnect.riot.iot.services.ZoneService;
import com.tierconnect.riot.sdk.utils.BeanUtils;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.jaxrs.PATCH;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import javax.annotation.Generated;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

@Path("/logicalReader")
@Api("/logicalReader")
@Generated("com.tierconnect.riot.appgen.service.GenController")
public class LogicalReaderController extends LogicalReaderControllerBase 
{
    @PUT
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresPermissions(value={"logicalReader:i"})
    @ApiOperation(position=3, value="Insert a LogicalReader (AUTO)")
    public Response insertLogicalReader( Map<String, Object> map, @QueryParam("createRecent") @DefaultValue("false") Boolean createRecent )
    {
        //2. TODO: Limit visibility based on user's group and the object's group (group based authorization)
        // 1[b,c]. Restrict access based on OBJECT and PROPERTY level write permissions
        //QueryUtils.filterWritePermissions(LogicalReader.class, map);
        LogicalReader logicalReader = new LogicalReader();
        // 7. handle insert and update
        BeanUtils.setProperties(map, logicalReader);

//        if(map.get("group.id") == null || (map.get("group.id") instanceof Integer) == false){
//            throw new UserException("Invalid Group");
//        }
//        Group group = GroupService.getInstance().get(((Number)map.get("group.id")).longValue());
//        if(group == null){
//            throw new UserException("Invalid Group");
//        }
//        logicalReader.setGroup(group);


        if(map.containsKey("zoneIn.id")) {
            Zone zoneIn = ZoneService.getInstance().get(Long.valueOf(map.get("zoneIn.id").toString()));
            logicalReader.setZoneIn(zoneIn);
        }
        if(map.containsKey("zoneOut.id")) {
            Zone zoneOut = ZoneService.getInstance().get(Long.valueOf(map.get("zoneOut.id").toString()));
            logicalReader.setZoneOut(zoneOut);
        }

        // 6. handle validation in an Extensible manner
        LogicalReaderService.getInstance().validateInsert(logicalReader);
        LogicalReaderService.getInstance().insert( logicalReader );
        if (createRecent) {
            RecentService.getInstance().insertRecent(logicalReader.getId(), logicalReader.getName(),"logicalreader", logicalReader.getGroup());
        }
        Map<String,Object> publicMap = logicalReader.publicMap();
        return RestUtils.sendCreatedResponse(publicMap);
    }

    @PATCH
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresPermissions(value={"logicalReader:u:{id}"})
    @ApiOperation(position=4, value="Update a LogicalReader (AUTO)")
    public Response updateLogicalReader( @PathParam("id") Long id, Map<String, Object> map )
    {
        // 1[b,c]. Restrict access based on OBJECT and PROPERTY level write permissions
        //QueryUtils.filterWritePermissions( LogicalReader.class, map );
        LogicalReader logicalReader = LogicalReaderService.getInstance().get( id );
        if( logicalReader == null )
        {
            return RestUtils.sendBadResponse( String.format( "LogicalReaderId[%d] not found", id) );
        }
        //RIOT-13304: There is an exception in services logs when you edit a Logical Reader
        map.put("id", logicalReader.getId());
        //2. TODO: Limit visibility based on user's group and the object's group (group based authorization)
        // 7. handle insert and update
        if (null != map.get("name") && !map.get("name").equals(logicalReader.getName()) )
            LogicalReaderService.getInstance().validateConstraintByNameAndGroup((String) map.get("name"), logicalReader.getGroup().getId());
        if (null != map.get("code") && !map.get("code").equals(logicalReader.getCode()) )
            LogicalReaderService.getInstance().validateConstraintByCodeAndGroup((String) map.get("code"), logicalReader.getGroup().getId());
        BeanUtils.setProperties( map, logicalReader );

//        if(map.get("group.id") == null || (map.get("group.id") instanceof Integer) == false){
//            throw new UserException("Invalid Group");
//        }
//        Group group = GroupService.getInstance().get(((Number)map.get("group.id")).longValue());
//        if(group == null){
//            throw new UserException("Invalid Group");
//        }
//        logicalReader.setGroup(group);


        if(map.containsKey("zoneIn.id")) {
            Zone zoneIn = ZoneService.getInstance().get(Long.valueOf(map.get("zoneIn.id").toString()));
            logicalReader.setZoneIn(zoneIn);
        }
        if(map.containsKey("zoneOut.id")) {
            Zone zoneOut = ZoneService.getInstance().get(Long.valueOf(map.get("zoneOut.id").toString()));
            logicalReader.setZoneOut(zoneOut);
        }

     
        // 6. handle validation in an Extensible manner
        validateUpdate(logicalReader );
        LogicalReaderService.getInstance().updateFavorite(logicalReader);
        RecentService.getInstance().updateName(logicalReader.getId(), logicalReader.getName(),"logicalreader");
        Map<String,Object> publicMap = logicalReader.publicMap();

        // RIOT-13659 send tickle for logical reader.
        LogicalReaderService.getInstance().sendRefreshConfigurationMessage(
                false, GroupService.getInstance().getMqttGroups(logicalReader.getGroup()));
        LogicalReaderService.getInstance().refreshCache(logicalReader,false);
        return RestUtils.sendOkResponse( publicMap );
    }

    /**
     * FILTER LIST
     */
    @GET
    @Path("/zoneGroup/")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"logicalReader:r"})
    @ApiOperation(position=1, value="Get a List of Zone Groups")
    public Response listEdgeBoxes(@QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber, @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only, @QueryParam("visibilityGroupId") Long visibilityGroupId, @DefaultValue("") @QueryParam("upVisibility") String upVisibility, @DefaultValue("") @QueryParam("downVisibility") String downVisibility , @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project  )
    {
        ZoneGroupController c = new ZoneGroupController();
        return c.listZoneGroups(pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility, downVisibility,false, extend, project);
    }

}

