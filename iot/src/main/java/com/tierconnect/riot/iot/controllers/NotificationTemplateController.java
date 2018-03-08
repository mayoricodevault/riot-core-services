package com.tierconnect.riot.iot.controllers;

import javax.annotation.Generated;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.utils.QueryUtils;
import com.tierconnect.riot.appcore.utils.EntityVisibility;
import com.tierconnect.riot.appcore.utils.QueryUtils;
import com.tierconnect.riot.appcore.utils.VisibilityUtils;
import com.tierconnect.riot.iot.entities.NotificationTemplate;
import com.tierconnect.riot.iot.entities.QNotificationTemplate;
import com.tierconnect.riot.iot.services.NotificationTemplateService;
import com.tierconnect.riot.sdk.dao.Pagination;
import com.tierconnect.riot.sdk.utils.BeanUtils;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.jaxrs.PATCH;
import org.apache.shiro.authz.annotation.RequiresAuthentication;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Path("/notificationTemplate")
@Api("/notificationTemplate")
@Generated("com.tierconnect.riot.appgen.service.GenController")
public class NotificationTemplateController extends NotificationTemplateControllerBase
{
    @GET
    @Override
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 1, value = "Get a List of Notifications")
    public Response listNotificationTemplates(@QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber, @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only, @QueryParam("visibilityGroupId") Long visibilityGroupId, @DefaultValue("") @QueryParam("upVisibility") String upVisibility, @DefaultValue("") @QueryParam("downVisibility") String downVisibility, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
    {
        Pagination pagination = new Pagination( pageNumber, pageSize );

        BooleanBuilder be = new BooleanBuilder();
        //TODO: how to handle case without a group property ?
        // 2. Limit visibility based on user's group and the object's group (group based authorization)
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(NotificationTemplate.class.getCanonicalName(), visibilityGroupId);
        EntityVisibility entityVisibility = getEntityVisibility();
        //be = be.and( GeneralVisibilityUtils.limitVisibilitySelectAll(entityVisibility, QNotificationTemplate.notificationTemplate,  visibilityGroup, upVisibility, downVisibility ) );
        // 4. Implement filtering
        be = be.and( QueryUtils.buildSearch(QNotificationTemplate.notificationTemplate, where) );

        Long count = NotificationTemplateService.getInstance().countList( be );
        List<Map<String, Object>> list = new LinkedList<Map<String, Object>>();
        // 3. Implement pagination
        for( NotificationTemplate notificationTemplate : NotificationTemplateService.getInstance().listPaginated( be, pagination, order ) )
        {
            // Additional filter
            if (!includeInSelect(notificationTemplate))
            {
                continue;
            }
            // 5a. Implement extra
            Map<String,Object> publicMap = QueryUtils.mapWithExtraFields( notificationTemplate, extra, getExtraPropertyNames());
            addToPublicMap(notificationTemplate, publicMap, extra);
            // 5b. Implement only
            QueryUtils.filterOnly(publicMap, only, extra);
            QueryUtils.filterProjectionNested(publicMap, project, extend);
            list.add( publicMap );
        }
        Map<String,Object> mapResponse = new HashMap<String,Object>();
        mapResponse.put( "total", count );
        mapResponse.put( "results", list );
        return RestUtils.sendOkResponse(mapResponse);
    }

    @GET
    @Override
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position=2, value="Select a NotificationTemplate")
    public Response selectNotificationTemplates( @PathParam("id") Long id, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
    {
        NotificationTemplate notificationTemplate = NotificationTemplateService.getInstance().get( id );
        if( notificationTemplate == null )
        {
            return RestUtils.sendBadResponse( String.format( "NotificationTemplateId[%d] not found", id) );
        }
        //2. TODO: Limit visibility based on user's group and the object's group (group based authorization)
        validateSelect( notificationTemplate );
        // 5a. Implement extra
        Map<String,Object> publicMap = QueryUtils.mapWithExtraFields( notificationTemplate, extra, getExtraPropertyNames());
        addToPublicMap(notificationTemplate, publicMap, extra);
        // 5b. Implement only
        QueryUtils.filterOnly(publicMap, only, extra);
        QueryUtils.filterProjectionNested(publicMap, project, extend);
        return RestUtils.sendOkResponse( publicMap );
    }

    public void validateSelect( NotificationTemplate notificationTemplate )
    {

    }

    @PUT
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position=3, value="Insert a NotificationTemplate")
    public Response insertNotificationTemplate( Map<String, Object> map )
    {
        //2. TODO: Limit visibility based on user's group and the object's group (group based authorization)
        NotificationTemplate notificationTemplate = new NotificationTemplate();
        // 7. handle insert and update
        BeanUtils.setProperties(map, notificationTemplate);
        // 6. handle validation in an Extensible manner
        validateInsert( notificationTemplate );
        NotificationTemplateService.getInstance().insert( notificationTemplate );
        // 1[b,c]. Restrict access based on OBJECT and PROPERTY level read permissions
        Map<String,Object> publicMap = notificationTemplate.publicMap();
        return RestUtils.sendCreatedResponse( publicMap );
    }

    public void validateInsert( NotificationTemplate notificationTemplate )
    {

    }

    @PATCH
    @Override
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position=4, value="Update a NotificationTemplate")
    public Response updateNotificationTemplate( @PathParam("id") Long id, Map<String, Object> map )
    {
        NotificationTemplate notificationTemplate = NotificationTemplateService.getInstance().get( id );
        if( notificationTemplate == null )
        {
            return RestUtils.sendBadResponse( String.format( "NotificationTemplateId[%d] not found", id) );
        }
        //2. TODO: Limit visibility based on user's group and the object's group (group based authorization)
        // 7. handle insert and update
        BeanUtils.setProperties( map, notificationTemplate );
        // 6. handle validation in an Extensible manner
        validateUpdate( notificationTemplate );
        NotificationTemplateService.getInstance().update( notificationTemplate );
        Map<String,Object> publicMap = notificationTemplate.publicMap();
        return RestUtils.sendOkResponse( publicMap );
    }

    public void validateUpdate( NotificationTemplate notificationTemplate )
    {

    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 5, value = "Delete a NotificationTemplate")
    public Response deleteNotificationTemplate( @PathParam("id") Long id )
    {
        // 1c. TODO: Restrict access based on OBJECT level read permissions
        NotificationTemplate notificationTemplate = NotificationTemplateService.getInstance().get( id );
        if( notificationTemplate == null )
        {
            return RestUtils.sendBadResponse( String.format( "NotificationTemplateId[%d] not found", id) );
        }
        //2. TODO: Limit visibility based on user's group and the object's group (group based authorization)
        // handle validation in an Extensible manner
        validateDelete( notificationTemplate );
        NotificationTemplateService.getInstance().delete( notificationTemplate );
        return RestUtils.sendDeleteResponse();
    }

    public void validateDelete( NotificationTemplate notificationTemplate )
    {

    }
}
