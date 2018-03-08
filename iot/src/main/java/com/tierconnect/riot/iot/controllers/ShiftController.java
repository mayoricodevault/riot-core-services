package com.tierconnect.riot.iot.controllers;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.RecentService;
import com.tierconnect.riot.appcore.utils.*;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.sdk.dao.Pagination;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.apache.log4j.Logger;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import javax.annotation.Generated;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

@Path("/shift")
@Api("/shift")
@Generated("com.tierconnect.riot.appgen.service.GenController")
public class ShiftController extends ShiftControllerBase
{
    static Logger logger = Logger.getLogger(ShiftController.class);

    @PUT
    @Path("/{shiftId}/configure")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"shift:i", "shift:u"}, logical = Logical.OR)
    @ApiOperation(value="Assign Zones to a Shift")
    public Response configure(@PathParam("shiftId") Long shiftId, Map<String, Object> m) {
        Shift shift = ShiftService.getInstance().get(shiftId);
        if (shift == null) {
            return RestUtils.sendBadResponse("Not a valid shiftId");
        }
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(Shift.class.getCanonicalName(), null);
        if (GroupService.getInstance().isGroupNotInsideTree(shift.getGroup(), visibilityGroup)) {
            throw new com.tierconnect.riot.sdk.servlet.exception.ForbiddenException("Forbidden shift");
        }
        List zoneIds = (List) m.get("zoneIds");
        if (zoneIds != null) {
            List<Long> newZoneIds = toLongList(zoneIds);
            QShiftZone qShiftZone = QShiftZone.shiftZone;
            List<Long> oldZoneIds = ShiftZoneService.getInstance().getShiftZoneDAO().getQuery().where(qShiftZone.shift.eq(shift)).list(qShiftZone.zone.id);
            List<Long> deletedZoneids = new ArrayList<>(oldZoneIds);
            deletedZoneids.removeAll(newZoneIds);
            if (deletedZoneids.size() > 0) {
                ShiftZoneService.getInstance().getShiftZoneDAO().deleteAllBy(qShiftZone.shift.id.eq(shiftId).and(qShiftZone.zone.id.in(deletedZoneids)));
            }

            List<Long> createdZoneIds = new ArrayList<>(newZoneIds);
            createdZoneIds.removeAll(oldZoneIds);
            for (Long id : createdZoneIds) {
                ShiftZone shiftZone = new ShiftZone();
                shiftZone.setGroup(shift.getGroup());
                shiftZone.setShift(shift);
                shiftZone.setZone(ZoneService.getZoneDAO().selectById(id));
                ShiftZoneService.getInstance().insert(shiftZone);
            }

        }

        List thingIds = (List) m.get("thingIds");
        if (thingIds != null) {
            List<Long> newThingIds = toLongList(thingIds);
            QShiftThing qShiftThing = QShiftThing.shiftThing;
            List<Long> oldThingIds = ShiftThingService.getInstance().getShiftThingDAO().getQuery().where(qShiftThing.shift.eq(shift)).list(qShiftThing.thing.id);
            List<Long> deletedThingIds = new ArrayList<>(oldThingIds);
            if (deletedThingIds.size() > 0) {
                ShiftThingService.getInstance().getShiftThingDAO().deleteAllBy(qShiftThing.shift.id.eq(shiftId).and(qShiftThing.thing.id.in(deletedThingIds)));
            }

            List<Long> cratedThingIds = new ArrayList<>(newThingIds);
            for (Long id : cratedThingIds) {
                ShiftThing shiftThing = new ShiftThing();
                shiftThing.setGroup(shift.getGroup());
                shiftThing.setShift(shift);
                shiftThing.setThing(ThingService.getThingDAO().selectById(id));
                ShiftThingService.getInstance().insert(shiftThing);
            }
        }

        // RIOT-13659 send tickle for shifts.
        ShiftService.getInstance().sendRefreshConfigurationMessage(false, GroupService.getInstance().getMqttGroups(shift.getGroup()));
        ShiftService.getInstance().refreshCache(shift,false);

        return RestUtils.sendOkResponse(shift.publicMap());
    }

    public static List<Long> toLongList(List list) {
        List<Long> result = new ArrayList<>();
        for (Object number : list) {
            if (number instanceof  Number) {
                result.add(((Number)number).longValue());
            } else if (number == null) {
            } else {
                result.add(Long.parseLong(number.toString()));
            }
        }
        return result;
    }

    @Override
    public void addToPublicMap(Shift shift, Map<String, Object> publicMap, String extra) {
        QShiftZone qShiftZone = QShiftZone.shiftZone;
        QShiftThing qShiftThing = QShiftThing.shiftThing;
        List<Zone> oldZones = ShiftZoneService.getInstance().getShiftZoneDAO().getQuery().where(qShiftZone.shift.eq(shift)).list(qShiftZone.zone);
        List<Thing> oldThings = ShiftThingService.getInstance().getShiftThingDAO().getQuery().where(qShiftThing.shift.eq(shift)).list(qShiftThing.thing);
        List<Map> zones = new ArrayList<>();
        for (Zone zone: oldZones) {
            zones.add(zone.publicMap());
        }
        List<Map> things = new ArrayList<>();
        for (Thing thing: oldThings) {
            Map<String, Object> publicMapThing = thing.publicMap();
            publicMapThing.put("thingType", thing.getThingType().publicMap());
            things.add(publicMapThing);
        }
        publicMap.put("zones",zones);
        publicMap.put("things", things);
    }

    @Override
    //validate and preDelete
    public void validateDelete(Shift shift) {
        QShiftZone qShiftZone = QShiftZone.shiftZone;
        QShiftThing qShiftThing = QShiftThing.shiftThing;
        ShiftZoneService shiftZoneService = ShiftZoneService.getInstance();
        ShiftThingService shiftThingService = ShiftThingService.getInstance();
        List<ShiftZone> shiftZones = shiftZoneService.getShiftZoneDAO().getQuery().where(qShiftZone.shift.eq(shift)).list(qShiftZone);
        List<ShiftThing> shiftThings = shiftThingService.getShiftThingDAO().getQuery().where(qShiftThing.shift.eq(shift)).list(qShiftThing);
        for (ShiftZone shiftZone: shiftZones) {
            shiftZoneService.delete(shiftZone);
        }
        for (ShiftThing shiftThing: shiftThings) {
            shiftThingService.delete(shiftThing);
        }
    }

    @GET
    @Path("/{id}/zones")
    @Produces(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresPermissions(value={"shift:r:{id}"})
    @ApiOperation(position=1, value="Get a List of Zones for a Shift")
    public Response listShiftZones( @PathParam("id") Long id, @QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber, @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only, @QueryParam("visibilityGroupId") Long visibilityGroupId, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
    {
        Shift shift = ShiftService.getInstance().get( id );
        if( shift == null )
        {
            return RestUtils.sendBadResponse( String.format( "ShiftId[%d] not found", id) );
        }
        // 2. Limit visibility based on user's group and the object's group (group based authorization)
        EntityVisibility entityVisibility = getEntityVisibility();
        GeneralVisibilityUtils.limitVisibilitySelect(entityVisibility, shift);
        validateSelect(shift);

        Pagination pagination = new Pagination( pageNumber, pageSize );

        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QShiftZone.shiftZone.shift.id.eq(shift.getId()));
        // 4. Implement filtering
        be = be.and( QueryUtils.buildSearch( QShiftZone.shiftZone.zone, where ) );

        Long count = ShiftZoneService.getInstance().countList( be );
        List<Map<String, Object>> list = new LinkedList<Map<String, Object>>();
        // 3. Implement pagination
        for( ShiftZone shiftZone : ShiftZoneService.getInstance().listPaginated( be, pagination, order ) )
        {
            // 5a. Implement extra
            Map<String,Object> publicMap = QueryUtils.mapWithExtraFields( shiftZone.getZone(), extra, getExtraPropertyNames() );
            //addToPublicMap(publicMap);
            // 5b. Implement only
            QueryUtils.filterOnly( publicMap, only, extra );
            QueryUtils.filterProjectionNested( publicMap, project, extend);
            list.add( publicMap );
        }
        Map<String,Object> mapResponse = new HashMap<String,Object>();
        mapResponse.put( "total", count );
        mapResponse.put( "results", list );
        return RestUtils.sendOkResponse( mapResponse );
    }

    @GET
    @Path("/{id}/things")
    @Produces(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresPermissions(value={"shift:r:{id}"})
    @ApiOperation(position=1, value="Get a List of Things for a Shift")
    public Response listShiftThings( @PathParam("id") Long id, @QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber, @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only, @QueryParam("visibilityGroupId") Long visibilityGroupId, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
    {
        Shift shift = ShiftService.getInstance().get( id );
        if( shift == null )
        {
            return RestUtils.sendBadResponse( String.format( "ShiftId[%d] not found", id) );
        }
        // 2. Limit visibility based on user's group and the object's group (group based authorization)
        EntityVisibility entityVisibility = getEntityVisibility();
        GeneralVisibilityUtils.limitVisibilitySelect(entityVisibility, shift);
        validateSelect( shift );

        Pagination pagination = new Pagination( pageNumber, pageSize );

        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QShiftThing.shiftThing.shift.id.eq(shift.getId()));
        // 4. Implement filtering
        be = be.and( QueryUtils.buildSearch( QShiftThing.shiftThing.thing, where ) );

        Long count = ShiftThingService.getInstance().countList( be );
        List<Map<String, Object>> list = new LinkedList<Map<String, Object>>();

        // 3. Implement pagination
        for( ShiftThing shiftThing : ShiftThingService.getInstance().listPaginated( be, pagination, order ) )
        {
            // 5a. Implement extra
            Map<String,Object> publicMap = QueryUtils.mapWithExtraFields( shiftThing.getThing(), extra, getExtraPropertyNames() );
            //addToPublicMap(publicMap);
            // 5b. Implement only
            QueryUtils.filterOnly( publicMap, only, extra );
            QueryUtils.filterProjectionNested( publicMap, project, extend);
            list.add( publicMap );
        }
        Map<String,Object> mapResponse = new HashMap<String,Object>();
        mapResponse.put( "total", count );
        mapResponse.put( "results", list );
        return RestUtils.sendOkResponse( mapResponse );
    }

    /**
     * FILTER LIST
     */
    @GET
    @Path("/thingType/")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"shift:r"})
    @ApiOperation(position = 1, value = "Get a List of ThingTypes (AUTO)")
    public Response listThingTypes( @QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber,
                                    @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only,
                                    @QueryParam("visibilityGroupId") Long visibilityGroupId, @DefaultValue("") @QueryParam("upVisibility") String upVisibility, @DefaultValue("") @QueryParam("downVisibility") String downVisibility, @QueryParam("topId") String topId, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
    {
        ThingTypeController c = new ThingTypeController();
        return c.listThingTypes( pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility, downVisibility, extend, project );
    }

    @GET
    @Path("/thingType/tree")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"shift:r"})
    @ApiOperation(position = 1, value = "Get a Tree of GroupTypes (AUTO)")
    public Response listThingTypesInTree( @QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber,
                                          @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only,
                                          @QueryParam("visibilityGroupId") Long visibilityGroupId
            , @DefaultValue("") @QueryParam("upVisibility") String upVisibility
            , @DefaultValue("") @QueryParam("downVisibility") String downVisibility
            , @QueryParam("topId") String topId
            , @DefaultValue("false") @QueryParam("enableMultilevel") String enableMultilevel)
    {
        ThingTypeController c = new ThingTypeController();
        return c.listThingTypesInTree( pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility, downVisibility, enableMultilevel );
    }


    @GET
    @Path("/thing/")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"shift:r"})
    @ApiOperation(position=1, value="Get a List of Things (AUTO)")
    public Response listThings(@QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber,
                               @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only,
                               @QueryParam("visibilityGroupId") Long visibilityGroupId, @DefaultValue("") @QueryParam("upVisibility") String upVisibility, @DefaultValue("") @QueryParam("downVisibility") String downVisibility, @QueryParam("topId") String topId, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
    {
        ThingController c = new ThingController();
        return c.listThings(pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility, downVisibility, extend, project);
    }

    /**
     * FILTER TREE
     */

    @GET
    @Path("/thing/tree")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"shift:r"})
    @ApiOperation(position=1, value="Get a Tree of Things (AUTO)")
    public Response listThingsInTree( @QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber,
                                      @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only,
                                      @QueryParam("visibilityGroupId") Long visibilityGroupId, @DefaultValue("") @QueryParam("upVisibility") String upVisibility, @DefaultValue("") @QueryParam("downVisibility") String downVisibility, @QueryParam("topId") String topId, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
    {
        ThingController c = new ThingController();
        return c.listThingsInTree(pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility, downVisibility, topId, extend, project);
    }

    @GET
    @Path("/zoneGroup/")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"shift:r"})
    @ApiOperation(position=1, value="Get a List of ZoneGroups (AUTO)")
    public Response listZoneGroups( @QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber,
                                    @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only,
                                    @QueryParam("visibilityGroupId") Long visibilityGroupId, @DefaultValue("") @QueryParam("upVisibility") String upVisibility, @DefaultValue("") @QueryParam("downVisibility") String downVisibility, @QueryParam("topId") String topId, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
    {
        ZoneGroupController c = new ZoneGroupController();
        return c.listZoneGroups (pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility, downVisibility, false, extend, project);
    }

    @GET
    @Path("/zone/")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"shift:r"})
    @ApiOperation(position=1, value="Get a List of Zones (AUTO)")
    public Response listZones( @QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber,
                               @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only,
                               @QueryParam("visibilityGroupId") Long visibilityGroupId, @DefaultValue("") @QueryParam("upVisibility") String upVisibility, @DefaultValue("") @QueryParam("downVisibility") String downVisibility, @QueryParam("topId") String topId, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
    {
        ZoneController c = new ZoneController();
        return c.listZones (pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility, downVisibility, false, extend, project);
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"shift:d:{id}"})
    // 1a. Limit access based on CLASS level resources
    @ApiOperation(position=5, value="Delete a Shift (AUTO)")
    @Override
    public Response deleteShift( @PathParam("id") Long id )
    {
        // 1c. TODO: Restrict access based on OBJECT level read permissions
        Shift shift = ShiftService.getInstance().get( id );
        if( shift == null )
        {
            return RestUtils.sendBadResponse( String.format( "ShiftId[%d] not found", id) );
        }
        RecentService.getInstance().deleteRecent(id,"shift");
        // 2. Limit visibility based on user's group and the object's group (group based authorization)
        EntityVisibility entityVisibility = getEntityVisibility();
        GeneralVisibilityUtils.limitVisibilityDelete(entityVisibility, shift );
        // handle validation in an Extensible manner
        validateDelete( shift );
        ShiftService.getInstance().delete( shift );
        //delete shift and shiftZone from kafkaCache
        ShiftService.getInstance().refreshCache(shift,true);
        return RestUtils.sendDeleteResponse();
    }

}

