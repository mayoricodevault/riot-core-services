package com.tierconnect.riot.iot.controllers;

import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.RecentService;
import com.tierconnect.riot.appcore.utils.EntityVisibility;
import com.tierconnect.riot.appcore.utils.GeneralVisibilityUtils;
import com.tierconnect.riot.appcore.utils.VisibilityUtils;
import com.tierconnect.riot.iot.entities.LocalMap;
import com.tierconnect.riot.iot.entities.Zone;
import com.tierconnect.riot.iot.entities.ZoneGroup;
import com.tierconnect.riot.iot.services.BrokerClientHelper;
import com.tierconnect.riot.iot.services.ZoneGroupService;
import com.tierconnect.riot.iot.services.ZoneService;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.utils.BeanUtils;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.jaxrs.PATCH;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/zoneGroup")
@Api("/zoneGroup")
public class ZoneGroupController extends ZoneGroupControllerBase
{

    @PUT
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"zoneGroup:i"})
    @ApiOperation(position=3, value="Insert a ZoneGroup")
    public Response insertZoneGroup( Map<String, Object> map ,@QueryParam("createRecent") @DefaultValue("false") Boolean createRecent) throws UserException
    {

        // 2. Limit visibility based on user's group and the object's group (group based authorization)
        EntityVisibility entityVisibility = getEntityVisibility();
        GeneralVisibilityUtils.limitVisibilityInsert(entityVisibility, VisibilityUtils.getObjectGroup(map));
        ZoneGroup zoneGroup = new ZoneGroup();
        // 7. handle insert and update

        BeanUtils.setProperties(map, zoneGroup);

        // 6. handle validation in an Extensible manner
        validateInsert( zoneGroup );

        if(zoneGroup.getName().length()>255){
         //  return RestUtils.sendResponseWithCode("ZoneGroup should have less than 255 characters",400);
            throw new UserException("ZoneGroup name should have less than 255 characters");
        }

        if(StringUtils.isNotEmpty(zoneGroup.getDescription()) && zoneGroup.getDescription().length()>255){
            //  return RestUtils.sendResponseWithCode("ZoneGroup should have less than 255 characters",400);
            throw new UserException("ZoneGroup description should have less than 255 characters");
        }

        ZoneGroupService zoneGroupService = ZoneGroupService.getInstance();
        if (zoneGroupService.existsZoneGroupByNameAndLocalMap(zoneGroup.getName(), zoneGroup.getLocalMap().getId())) {
            throw new UserException(String.format("Zone Group named '%s' already exists in Tenant Group", zoneGroup.getName()));
        }

        zoneGroupService.insert(zoneGroup);

        BrokerClientHelper.sendRefreshZoneGroupsMessage(false, GroupService.getInstance().getMqttGroups(zoneGroup.getGroup()));
        if (createRecent){
            RecentService.getInstance().insertRecent(zoneGroup.getId(), zoneGroup.getName(),"zonegroup", zoneGroup.getGroup());
        }

        // 1[b,c]. Restrict access based on OBJECT and PROPERTY level read permissions
        Map<String,Object> publicMap = zoneGroup.publicMap();
        return RestUtils.sendCreatedResponse( publicMap );
    }

    @PATCH
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresPermissions(value={"zoneGroup:u:{id}"})
    @ApiOperation(position=4, value="Update a ZoneGroup (AUTO)")
    public Response updateZoneGroup( @PathParam("id") Long id, Map<String, Object> map ) throws UserException
    {
        // 1[b,c]. Restrict access based on OBJECT and PROPERTY level write permissions
        //QueryUtils.filterWritePermissions(ZoneGroup.class, map);
        ZoneGroup zoneGroup = ZoneGroupService.getInstance().get( id );
        if( zoneGroup == null )
        {
            return RestUtils.sendBadResponse( String.format( "ZoneGroupId[%d] not found", id) );
        }
        // 2. Limit visibility based on user's group and the object's group (group based authorization)
        EntityVisibility entityVisibility = getEntityVisibility();
        GeneralVisibilityUtils.limitVisibilityUpdate( entityVisibility, zoneGroup, VisibilityUtils.getObjectGroup(map));

        //Removing Zones from map
        List<Map<String, Object> > zoneList = (List<Map<String, Object>>) map.get("zones");
        map.remove("zones");
        // 7. handle insert and update
        BeanUtils.setProperties(map, zoneGroup);

        // validation
        if(zoneGroup.getName().length()>255){
            //  return RestUtils.sendResponseWithCode("ZoneGroup should have less than 255 characters",400);
            throw new UserException("ZoneGroup name should have less than 255 characters");
        }

        if(StringUtils.isNotEmpty(zoneGroup.getDescription()) && zoneGroup.getDescription().length()>255){
            //  return RestUtils.sendResponseWithCode("ZoneGroup should have less than 255 characters",400);
            throw new UserException("ZoneGroup description should have less than 255 characters");
        }

        if (ZoneGroupService.getInstance().existsZoneGroupByNameAndLocalMap(zoneGroup.getName(), zoneGroup.getLocalMap().getId(), zoneGroup.getId() )) {
            throw new UserException(String.format("Zone Group named '%s' already exists in Facility", zoneGroup.getName()));
        }

        ZoneGroupService.updateZones(zoneGroup, zoneList);
        RecentService.getInstance().updateName(zoneGroup.getId(), zoneGroup.getName(),"zonegroup");

        // 6. handle validation in an Extensible manner
        validateUpdate(zoneGroup);
        Map<String,Object> publicMap = zoneGroup.publicMap();

        BrokerClientHelper.sendRefreshZoneGroupsMessage(false, GroupService.getInstance().getMqttGroups(zoneGroup.getGroup()));

        return RestUtils.sendOkResponse( publicMap );
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"zoneGroup:d:{id}"})
    // 1a. Limit access based on CLASS level resources
    @ApiOperation(position=5, value="Delete a ZoneGroup (AUTO)")
    public Response deleteZoneGroup( @PathParam("id") Long id , @QueryParam("zoneGroupId") Long zoneGroupId)
    {
        // 1c. TODO: Restrict access based on OBJECT level read permissions
        ZoneGroup zoneGroup = ZoneGroupService.getInstance().get( id );
        if( zoneGroup == null )
        {
            return RestUtils.sendBadResponse(String.format("ZoneGroupId[%d] not found", id));
        }
        // 2. Limit visibility based on user's group and the object's group (group based authorization)
        EntityVisibility entityVisibility = getEntityVisibility();
        GeneralVisibilityUtils.limitVisibilityDelete(entityVisibility, zoneGroup);
        // handle validation in an Extensible manner
        RecentService.getInstance().deleteRecent(id,"zonegroup");
        List<Long> groupMqtt = GroupService.getInstance().getMqttGroups(zoneGroup.getGroup());

        if( zoneGroup.getZones().isEmpty() ){
            //Removing References.
            LocalMap localMap = zoneGroup.getLocalMap();
            localMap.setZoneGroup( null );

            ZoneGroupService.getInstance().delete(zoneGroup);

            BrokerClientHelper.sendRefreshZoneGroupsMessage(false, groupMqtt);

            return RestUtils.sendDeleteResponse();

        }else{
            List<ZoneGroup> list=ZoneGroupService.getZoneGroupDAO().selectAllBy("localMap",zoneGroup.getLocalMap());

            switch (list.size()){
                case 0:
                case 1:{
                    return  RestUtils.sendBadResponse(String.format( "It is not possible to erase [%s] zone group because is being referenced by one or more zones.",zoneGroup.getName()));
                }
                default:{
                    if( zoneGroupId == null){
                        List<Object> publicMapList=new ArrayList<>();
                        Map<String,Object> mapResponse = new HashMap<>();
                        list.remove(zoneGroup);
                        int count=list.size();
                        for (int i = 0;i<count;i++){
                            publicMapList.add(list.get(i).publicMap());
                        }

                        mapResponse.put( "total", count );
                        mapResponse.put( "results", publicMapList);

                        return RestUtils.sendOkResponse(mapResponse);

                    }else{
                        // move zones to selected zone group
                        ZoneGroup zoneGroupSelected = ZoneGroupService.getInstance().get(zoneGroupId);

                        if (zoneGroupSelected != null) {
                            List<Zone> selectedGroupZones = zoneGroupSelected.getZones();
                            for (Zone zone : zoneGroup.getZones()) {
                                zone.setZoneGroup(zoneGroupSelected);
                                ZoneService.getInstance().update(zone);
                                selectedGroupZones.add(zone);
                            }
                            zoneGroupSelected.setZones(selectedGroupZones);
                            ZoneGroupService.getInstance().update(zoneGroupSelected);
                            zoneGroup.setZones(null);
                            ZoneGroupService.getInstance().update(zoneGroup);
                        }else {
                            return  RestUtils.sendBadResponse(String.format( "ZoneGroupId[%d] not found", zoneGroupId));
                        }

                        ZoneGroupService.getInstance().delete(zoneGroup);

                        BrokerClientHelper.sendRefreshZoneGroupsMessage(false, groupMqtt);

                        return RestUtils.sendDeleteResponse();
                    }

                }
            }


        }
    }
}

