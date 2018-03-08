package com.tierconnect.riot.iot.controllers;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.utils.QueryUtils;
import com.tierconnect.riot.appcore.utils.VisibilityUtils;
import com.tierconnect.riot.iot.entities.CustomObjectType;
import com.tierconnect.riot.iot.entities.QCustomObjectType;
import com.tierconnect.riot.iot.services.CustomObjectTypeService;
import com.tierconnect.riot.sdk.dao.Pagination;
import com.tierconnect.riot.sdk.utils.BeanUtils;
import com.tierconnect.riot.appcore.utils.QueryUtils;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.jaxrs.PATCH;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by cfernandez
 * 10/22/2014.
 */

@Path("/customObjectType")
@Api("/customObjectType")
public class CustomObjectTypeController extends CustomObjectTypeControllerBase{

    @GET
    @Path("/exclude/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"customObjectType:r"})
    @ApiOperation(position=1, value="Get a List of CustomObjectTypes excluding the object with id defined")
    public Response listCustomObjectTypesExclude(@PathParam("id") Long id, @QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber, @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
    {
        Pagination pagination = new Pagination( pageNumber, pageSize );

        BooleanBuilder be = new BooleanBuilder();

        // 2. Limit visibility based on user's group and the object's group (group based authorization)
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(CustomObjectType.class.getCanonicalName(), null);
        //be = be.and( VisibilityUtils.limitVisibilityPredicate( visibilityGroup, QCustomObjectType.customObjectType.group,true, true ) );
        // 4. Implement filtering
        be = be.and( QueryUtils.buildSearch(QCustomObjectType.customObjectType, where) );

        Long count = CustomObjectTypeService.getInstance().countList( be );
        List<Map<String, Object>> list = new LinkedList<Map<String, Object>>();

        // 3. Implement pagination
        CustomObjectTypeService customObjectTypeService = CustomObjectTypeService.getInstance();
        List<CustomObjectType> customObjectTypes = customObjectTypeService.listPaginated(be, pagination, order);
        for( CustomObjectType customObjectType : customObjectTypes)
        {
            if (!customObjectType.getId().equals(id)){
                // 5a. Implement extra
                Map<String,Object> publicMap = QueryUtils.mapWithExtraFields( customObjectType, extra, getExtraPropertyNames() );
                addToPublicMap(customObjectType, publicMap, extra);
                // 5b. Implement only
                QueryUtils.filterOnly( publicMap, only, extra );
                QueryUtils.filterOnly( publicMap, project, extend );
                list.add( publicMap );
            }
        }
        Map<String,Object> mapResponse = new HashMap<String,Object>();
        mapResponse.put( "total", count );
        mapResponse.put( "results", list );
        return RestUtils.sendOkResponse(mapResponse);
    }


    @PUT
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresPermissions(value={"customObjectType:i"})
    @ApiOperation(position=3, value="Insert a CustomObjectType (AUTO)")
    public Response insertCustomObjectType( Map<String, Object> map )
    {
        //2. TODO: Limit visibility based on user's group and the object's group (group based authorization)
        CustomObjectType customObjectType = new CustomObjectType();
        // 7. handle insert and update
        BeanUtils.setProperties(map, customObjectType);
        // 6. handle validation in an Extensible manner
        if(!uniqueCodeCreate(customObjectType)){
            return RestUtils.sendBadResponse( String.format("MakerCodeExists"));
        }
        CustomObjectTypeService.getInstance().insert( customObjectType );
        // 1[b,c]. Restrict access based on OBJECT and PROPERTY level read permissions
        Map<String,Object> publicMap = customObjectType.publicMap();
        return RestUtils.sendCreatedResponse( publicMap );
    }

    public boolean uniqueCodeCreate( CustomObjectType customObjectType )
    {
        CustomObjectType customObjectType1 = CustomObjectTypeService.getInstance().selectByCodeAndApplication(customObjectType.getCode(),customObjectType.getCustomApplication().getCode());
        return customObjectType1==null;
    }


    @PATCH
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresPermissions(value={"customObjectType:u:{id}"})
    @ApiOperation(position=4, value="Update a CustomObjectType (AUTO)")
    public Response updateCustomObjectType( @PathParam("id") Long id, Map<String, Object> map )
    {
        CustomObjectType customObjectType = CustomObjectTypeService.getInstance().get( id );
        if( customObjectType == null )
        {
            return RestUtils.sendBadResponse( String.format( "CustomObjectTypeId[%d] not found", id) );
        }
        //2. TODO: Limit visibility based on user's group and the object's group (group based authorization)
        // 7. handle insert and update
        BeanUtils.setProperties( map, customObjectType );
        // 6. handle validation in an Extensible manner
        if(!uniqueCodeEdit( customObjectType , id )){
            return RestUtils.sendBadResponse( String.format("MakerCodeExists"));
        }
        Map<String,Object> publicMap = customObjectType.publicMap();
        return RestUtils.sendOkResponse( publicMap );
    }

    public boolean uniqueCodeEdit( CustomObjectType customObjectType, Long id )
    {
        try{
            CustomObjectType customObjectType1 = CustomObjectTypeService.getInstance().selectByCodeAndApplication(customObjectType.getCode(),customObjectType.getCustomApplication().getCode());
            if(customObjectType1==null) {
                return true;
            }
            else{
                return Long.parseLong(customObjectType1.getId().toString())==id;
            }
        }
        catch(Exception err){
            return false;
        }
    }
}

