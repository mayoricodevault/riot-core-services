package com.tierconnect.riot.iot.controllers;

import com.tierconnect.riot.iot.entities.CustomApplication;
import com.tierconnect.riot.iot.services.CustomApplicationService;
import com.tierconnect.riot.sdk.utils.BeanUtils;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.jaxrs.PATCH;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.Charsets;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

/**
 * Created by cfernandez
 * 10/29/2014.
 */

@Path("/customApplication")
@Api("/customApplication")
public class CustomApplicationController extends CustomApplicationControllerBase {

    @PUT
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresPermissions(value={"customApplication:i"})
    @ApiOperation(position=3, value="Insert a CustomApplication (AUTO)")
    public Response insertCustomApplication( Map<String, Object> map )
    {
        //2. TODO: Limit visibility based on user's group and the object's group (group based authorization)
        CustomApplication customApplication = new CustomApplication();
        // 7. handle insert and update
        //BeanUtils.setProperties(map, customApplication);
        /**/
        String prueba = "";
        String inputStream = "";
        if(map.get("icon") != null) {
            prueba = new String((String) map.get("icon"));
            //System.out.println("prueba: "+prueba);
            inputStream = prueba.substring(22);
            //System.out.println("inputStream: "+inputStream);
        }
        byte [] decodedBytes=null;

        decodedBytes = Base64.decodeBase64(inputStream.getBytes(Charsets.UTF_8));
        customApplication.setCode((String) map.get("code"));
        customApplication.setName((String) map.get("name"));
        customApplication.setShotTab((Boolean) map.get("shotTab"));
        customApplication.setIcon(decodedBytes);
        //BeanUtils.setProperties(map, customApplication);
        /**/
        // 6. handle validation in an Extensible manner
        if(!uniqueCodeCreate(customApplication)){
            return RestUtils.sendBadResponse( String.format("MakerCodeExists"));
        }
        CustomApplicationService.getInstance().insert( customApplication );
        // 1[b,c]. Restrict access based on OBJECT and PROPERTY level read permissions
        Map<String,Object> publicMap = customApplication.publicMap();
        return RestUtils.sendCreatedResponse(publicMap);
    }

    public boolean uniqueCodeCreate( CustomApplication customApplication )
    {
        CustomApplication customApplication1 = CustomApplicationService.getInstance().selectByCode(customApplication.getCode());
        if(customApplication1==null){
            return true;
        }
        return false;
    }

    @PATCH
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresPermissions(value={"customApplication:u:{id}"})
    @ApiOperation(position=4, value="Update a CustomApplication (AUTO)")
    public Response updateCustomApplication( @PathParam("id") Long id, Map<String, Object> map )
    {
        CustomApplication customApplication = CustomApplicationService.getInstance().get( id );
        if( customApplication == null )
        {
            return RestUtils.sendBadResponse( String.format( "CustomApplicationId[%d] not found", id) );
        }
        //2. TODO: Limit visibility based on user's group and the object's group (group based authorization)
        // 7. handle insert and update
        BeanUtils.setProperties( map, customApplication );
        // 6. handle validation in an Extensible manner
        if(!uniqueCodeEdit(customApplication,id)){
            return RestUtils.sendBadResponse( String.format("MakerCodeExists"));
        }
        Map<String,Object> publicMap = customApplication.publicMap();
        return RestUtils.sendOkResponse( publicMap );
    }

    public boolean uniqueCodeEdit( CustomApplication customApplication , Long id)
    {
        try{
            CustomApplication customApplication1 = CustomApplicationService.getInstance().selectByCode(customApplication.getCode());
            if(customApplication1==null) {
                return true;
            }
            else{
                return Long.parseLong(customApplication1.getId().toString())==id;
            }
        }
        catch(Exception err){
            return false;
        }
    }

}
