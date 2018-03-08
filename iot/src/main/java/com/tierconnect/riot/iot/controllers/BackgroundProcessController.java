package com.tierconnect.riot.iot.controllers;

import com.tierconnect.riot.iot.entities.BackgroundProcess;
import com.tierconnect.riot.iot.services.BackgroundProcessService;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.jaxrs.PATCH;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.jboss.logging.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 *
 * @author : aruiz
 * @date : 2/8/17 3:44 PM
 * @version:
 */
@Path("/backgroundProcess")
@Api("/backgroundProcess")
public class BackgroundProcessController extends BackgroundProcessControllerBase {

    static Logger logger = Logger.getLogger(BackgroundProcess.class);

    @GET
    @Path("/{action}/{module}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 1, value = "Get status of background process")
    public Response getStatusBackgroundProcess(@ApiParam(value = "Action to Perform") @PathParam("action") String action
                                            ,@ApiParam(value = "Module Name") @PathParam("module") String module
                                            ,@ApiParam(value = "Module Id") @PathParam("id") Long id) {
        Map<String, Object> result;
        result = BackgroundProcessService.getInstance().getStatus( module,id);

        if (result.get("status").equals("error")) {
            return RestUtils.sendBadResponse(result.get("message").toString());
        }else{
            return RestUtils.sendOkResponse(result);
        }

    }

    @GET
    @Path("/activeProcesses")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 3, value = "Get all active processes")
    public Response getActiveProcesses(){
        List<Map<String, Object>> result = BackgroundProcessService.getInstance().getActiveProcesses();

        return RestUtils.sendOkResponse(result);
    }


    @PATCH
    @Path("/{action}/{module}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 2, value = "Get status of background process")
    public Response setActionProcess(@ApiParam(value = "Action to Perform") @PathParam("action") String action
            ,@ApiParam(value = "Module Name") @PathParam("module") String module
            ,@ApiParam(value = "Module Id") @PathParam("id") Long id) {
        Map<String, Object> result = new HashMap<>();

        switch (action){
            case "cancel":

                result = BackgroundProcessService.getInstance().cancelBackgroundProcess(module,id);

                if (result.get("status").equals("error")) {
                    return RestUtils.sendBadResponse(result.get("message").toString());
                }else{
                    return RestUtils.sendOkResponse(result);
                }

            case "acknowledge":

                result = BackgroundProcessService.getInstance().getAcknowledge(module,id);

                if (result.get("status").equals("error")) {
                    return RestUtils.sendBadResponse(result.get("message").toString());
                }else{
                    return RestUtils.sendOkResponse(result);
                }

            case "retry":

                result = BackgroundProcessService.getInstance().retryBackgroundProcess(module, id);
                if (result.get("status").equals("error")) {
                    return RestUtils.sendBadResponse(result.get("message").toString());
                }else{
                    return RestUtils.sendOkResponse(result);
                }

        }

        if (result.get("status").equals("error")) {
            return RestUtils.sendBadResponse(result.get("message").toString());
        }else{
            return RestUtils.sendOkResponse(result);
        }

    }


}
