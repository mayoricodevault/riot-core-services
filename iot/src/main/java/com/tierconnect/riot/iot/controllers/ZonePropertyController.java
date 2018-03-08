package com.tierconnect.riot.iot.controllers;

import javax.annotation.Generated;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.apache.log4j.Logger;
import org.apache.shiro.authz.annotation.RequiresPermissions;

@Path("/zoneProperty")
@Api("/zoneProperty")
@Generated("com.tierconnect.riot.appgen.service.GenController")
public class ZonePropertyController extends ZonePropertyControllerBase 
{
    static Logger logger = Logger.getLogger(ZonePropertyController.class);
    /**
     * FILTER LIST
     * This method override the listZoneTypes Controller,
     * here we do validations about the values in the list
     */

    @GET
    @Path("/zoneType/")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"zoneProperty:r"})
    @ApiOperation(position=1, value="Get a List of ZoneTypes (AUTO)")
    public Response listZoneTypes(@QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber, @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only, @QueryParam("visibilityGroupId") Long visibilityGroupId, @DefaultValue("") @QueryParam("upVisibility") String upVisibility, @DefaultValue("") @QueryParam("downVisibility") String downVisibility, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project  )
    {
        logger.info("Verifying the input of data");
        ZoneTypeController c = new ZoneTypeController();
        return c.listZoneTypes (pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility, downVisibility,false, extend, project);
    }
}
