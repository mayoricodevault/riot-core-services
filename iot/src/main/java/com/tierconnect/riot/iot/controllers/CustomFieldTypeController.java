package com.tierconnect.riot.iot.controllers;

import com.tierconnect.riot.iot.entities.CustomFieldType;
import com.tierconnect.riot.iot.services.CustomFieldTypeService;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

/**
 * Created by cfernandez
 * 10/22/2014.
 */

@Path("/customFieldType")
@Api("/customFieldType")
public class CustomFieldTypeController extends CustomFieldTypeControllerBase{

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    @RequiresPermissions(value={"customFieldType:r:{id}"})
    @ApiOperation(position=2, value="Select a CustomFieldType by Id")
    public Response selectCustomFieldTypes(@PathParam("id") Long id, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only , @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project)
    {
        // calling service
        CustomFieldTypeService customFieldTypeService = CustomFieldTypeService.getInstance();
        CustomFieldType customFieldType = customFieldTypeService.selectById(id, extra, only);
        if( customFieldType == null )
        {
            return RestUtils.sendBadResponse(String.format("CustomFieldTypeId[%d] not found", id));
        }

        // serializing service result
        Map<String, Object> publicMap = customFieldType.publicMap();

        // sending result
        return RestUtils.sendOkResponse( publicMap );
    }
}
