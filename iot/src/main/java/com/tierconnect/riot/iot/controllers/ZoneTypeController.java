package com.tierconnect.riot.iot.controllers;

import com.tierconnect.riot.appcore.controllers.GroupController;
import com.tierconnect.riot.appcore.controllers.RiotShiroRealm;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.RecentService;
import com.tierconnect.riot.iot.services.BrokerClientHelper;
import com.tierconnect.riot.appcore.utils.EntityVisibility;
import com.tierconnect.riot.appcore.utils.GeneralVisibilityUtils;
import com.tierconnect.riot.appcore.utils.VisibilityUtils;
import com.tierconnect.riot.iot.entities.ValidationBean;
import com.tierconnect.riot.iot.entities.ZonePropertiesBean;
import com.tierconnect.riot.iot.entities.ZoneType;
import com.tierconnect.riot.iot.services.ZoneTypeService;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.utils.BeanUtils;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.jaxrs.PATCH;
import java.util.stream.IntStream;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

@Path("/zoneType")
@Api("/zoneType")
public class ZoneTypeController extends ZoneTypeControllerBase 
{

    public static final Logger logger = Logger.getLogger(ZoneTypeController.class);


    public void validateSelect( ZoneType zoneType )
    {

    }

    /**
     * LIST
     */

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresPermissions(value={"zoneType:r"})
    @ApiOperation(position=1, value="Get a List of ZoneTypes (AUTO)")
    @Override
    public Response listZoneTypes( @QueryParam("pageSize") Integer pageSize
            , @QueryParam("pageNumber") Integer pageNumber
            , @QueryParam("order") String order
            , @QueryParam("where") String where
            , @Deprecated @QueryParam("extra") String extra
            , @Deprecated @QueryParam("only") String only
            , @QueryParam("visibilityGroupId") Long visibilityGroupId
            , @DefaultValue("") @QueryParam("upVisibility") String upVisibility
            , @DefaultValue("") @QueryParam("downVisibility") String downVisibility
            , @QueryParam("returnFavorite") boolean returnFavorite
            , @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend
            , @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project)
    {
        if (upVisibility == null || upVisibility.isEmpty()) {
            upVisibility = "false";
        }
        return super.listZoneTypes(pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility, downVisibility,returnFavorite, extend, project);

    }

    @PUT
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresPermissions(value={"zoneType:i"})
    @ApiOperation(position=3, value="Insert a ZoneType (AUTO)")
    public Response insertZoneType( Map<String, Object> map ,@QueryParam("createRecent") @DefaultValue("false") Boolean createRecent)
    {
        Map<String,Object> publicMap = null;
        try
        {
            // 2. Limit visibility based on user's group and the object's group (group based authorization)
            EntityVisibility entityVisibility = getEntityVisibility();
            GeneralVisibilityUtils.limitVisibilityInsert(entityVisibility, VisibilityUtils.getObjectGroup(map));
            ZoneType zoneType = new ZoneType();

            // 7. handle insert and update
            BeanUtils.setProperties(map, zoneType);
            // 6. handle validation in an Extensible manner
            validateInsert(zoneType, (List<Map<String, Object>>) map.get(ZoneTypeService.ZONE_PROPERTIES));
            ZoneTypeService.getInstance().insert(zoneType);

            ZoneTypeService.parseZoneProperties(zoneType, map);
            if (createRecent){
                RecentService.getInstance().insertRecent(zoneType.getId(), zoneType.getName(),"zonetype", zoneType.getGroup());
            }

            // 1[b,c]. Restrict access based on OBJECT and PROPERTY level read permissions
            publicMap = zoneType.publicMap();
            BrokerClientHelper.sendRefreshZoneTypesMessage(
                    false, GroupService.getInstance().getMqttGroups(zoneType.getGroup()));
            ZoneTypeService.getInstance().refreshCache(zoneType,false);



        }catch(Exception e)
        {
            return RestUtils.sendBadResponse( e.getMessage());
        }
        return RestUtils.sendCreatedResponse( publicMap );
    }

    /*Validation of the insert*/
    public void validateInsert( ZoneType zoneType , List<Map<String, Object> > zonePropertiesMap)
    {
        if (!ZoneTypeService.getZoneTypeDAO().validateDuplicatedNameByGroup(zoneType.getName(), zoneType.getGroup().getId()) )
            throw new UserException("Duplicated Name in Zone Type: " + zoneType.getName());

        //Validate names duplicated
        if(zonePropertiesMap != null)
        {
           Set data = new HashSet();
            for( Map<String, Object> zonePropertyMap : zonePropertiesMap )
            {
                data.add( (String) zonePropertyMap.get( "name" ) );
            }
            if(zonePropertiesMap.size()!=data.size())
            {
                throw new UserException("Zone Properties is duplicated. ");
            }
        }
    }

    @PATCH
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresPermissions(value={"zoneType:u:{id}"})
    @ApiOperation(position=4, value="Update a ZoneType (AUTO)")
    public Response updateZoneType( @PathParam("id") Long id, Map<String, Object> map )
    {
        logger.info("updateZoneType");
        Map<String, Object> publicMap;
        try {
            ZoneType zoneType = ZoneTypeService.getInstance().get(id);
            //1. Validation
            ValidationBean validationBean = this.validateUpdate(id, zoneType, map);
            if (validationBean.isError()) {
                return RestUtils.sendBadResponse(validationBean.getErrorDescription());

            }
            // 2. Limit visibility based on user's group and the object's group (group based authorization)
            EntityVisibility entityVisibility = getEntityVisibility();
            GeneralVisibilityUtils.limitVisibilityUpdate(entityVisibility, zoneType, VisibilityUtils.getObjectGroup(map));

            // 7. handle insert and update
            ZoneTypeService.parseZoneProperties(zoneType, map);

            //Removing zoneTypeCode because value cannot be changed
            if (map.containsKey("zoneTypeCode")) {
                map.remove("zoneTypeCode");
            }

            BeanUtils.setProperties(map, zoneType);
            // 6. handle validation in an Extensible manner
            validateUpdate(zoneType);
            ZoneTypeService.getInstance().updateFavorite(zoneType);
            RecentService.getInstance().updateName(zoneType.getId(), zoneType.getName(),"zonetype");
            publicMap = zoneType.publicMap();
            BrokerClientHelper.sendRefreshZoneTypesMessage(
                    false, GroupService.getInstance().getMqttGroups(zoneType.getGroup()));

            ZoneTypeService.getInstance().refreshCache(zoneType,false);
        } catch(Exception e){
            return RestUtils.sendBadResponse(e.getMessage());
        }
        return RestUtils.sendOkResponse( publicMap );
    }

    /****************************
     * Validation of the data
     ******************************/
    public ValidationBean validateUpdate( Long id, ZoneType zoneType , Map<String, Object> map)
    {
        ValidationBean response = new ValidationBean();
        ArrayList<String> messages = new ArrayList<String>();
        if(zoneType == null ){
            messages.add(String.format("ZoneTypeId[%d] not found", id));
        } else {
            if (zoneType.getName() != null && !zoneType.getName().equals(map.get("name"))
                    && !ZoneTypeService.getZoneTypeDAO().validateDuplicatedNameByGroup((String) map.get("name"),
                            zoneType.getGroup().getId())){
                messages.add("Duplicated Name in Zone Type: " + map.get("name"));
            }
        }
        if(messages.size() > 0){
            response.setErrorDescription(StringUtils.join(messages, ","));
        }
        return response;
    }

    /****
     * This method get a list of elements in Zone Properties
     *****/
    public static ArrayList<ZonePropertiesBean> getListZonePropertiesValues(String linkString)
    {
        ArrayList<ZonePropertiesBean> result = new ArrayList<ZonePropertiesBean>();
        ZonePropertiesBean entityBean = null;
        String[] data = linkString.split("}");

        String id = null;
        String value = null;
        String type = null;
        for (int i = 0; i < data.length-1; i++) {
            entityBean = new ZonePropertiesBean();
            value = data[i];
            id = value;
            type = value;
            type = type.substring(type.indexOf("type="), type.length());
            type = type.substring(5, type.indexOf(",")==-1?type.length():type.indexOf(","));

            value = value.substring(value.indexOf("name="), value.length());
            value = value.substring(5, value.indexOf(",")==-1?value.length():value.indexOf(","));

            entityBean.setType(type);
            entityBean.setValue(value);

            result.add(entityBean);
        }
        return result;
    }


    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"zoneType:d:{id}"})
    // 1a. Limit access based on CLASS level resources
    @ApiOperation(position=5, value="Delete a ZoneType (AUTO)")
    public Response deleteZoneType( @PathParam("id") Long id )
    {
        try {
        // 1c. TODO: Restrict access based on OBJECT level read permissions
        ZoneType zoneType = ZoneTypeService.getInstance().get( id );
        if( zoneType == null )
        {
            return RestUtils.sendBadResponse( String.format( "ZoneTypeId[%d] not found", id) );
        }
        // 2. Limit visibility based on user's group and the object's group (group based authorization)
        EntityVisibility entityVisibility = getEntityVisibility();
        GeneralVisibilityUtils.limitVisibilityDelete(entityVisibility, zoneType);
        List<Long> groupMqtt = GroupService.getInstance().getMqttGroups(zoneType.getGroup());
        // handle validation in an Extensible manner
        ZoneTypeService.getInstance().delete(zoneType);
            RecentService.getInstance().deleteRecent(id,"zonetype");

        BrokerClientHelper.sendRefreshZoneTypesMessage(false, groupMqtt);
        ZoneTypeService.getInstance().refreshCache(zoneType,true);
        } catch(Exception e){
            return RestUtils.sendBadResponse(e.getMessage());
        }
        return RestUtils.sendDeleteResponse();
    }

    public void addToPublicMap(Map<String,Object> publicMap )
    {
    }

    /**
     * FILTER LIST
     */

    @GET
    @Path("/group/")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"zoneType:r"})
    @ApiOperation(position=1, value="Get a List of Groups (AUTO)")
    public Response listGroups( @QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber,
                                @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only,
                                @QueryParam("visibilityGroupId") Long visibilityGroupId, @DefaultValue("") @QueryParam("upVisibility") String upVisibility, @DefaultValue("") @QueryParam("downVisibility") String downVisibility, @QueryParam("topId") String topId, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
    {
        GroupController c = new GroupController();
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(ZoneType.class.getCanonicalName(), visibilityGroupId);
        RiotShiroRealm.getOverrideVisibilityCache().put(Group.class.getCanonicalName(), visibilityGroup);
        return c.listGroups (pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility, downVisibility, extend, project);
    }

    /**
     * FILTER TREE
     */

    @GET
    @Path("/group/tree")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"zoneType:r"})
    @ApiOperation(position=1, value="Get a Tree of Groups (AUTO)")
    public Response listGroupsInTree( @QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber,
                                      @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only,
                                      @QueryParam("visibilityGroupId") Long visibilityGroupId, @DefaultValue("") @QueryParam("upVisibility") String upVisibility, @DefaultValue("") @QueryParam("downVisibility") String downVisibility, @QueryParam("topId") String topId, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
    {
        GroupController c = new GroupController();
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(ZoneType.class.getCanonicalName(), visibilityGroupId);
        RiotShiroRealm.getOverrideVisibilityCache().put(Group.class.getCanonicalName(), visibilityGroup);
        return c.listGroupsInTree(pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility, downVisibility, topId, false, extend, project);
    }

}

