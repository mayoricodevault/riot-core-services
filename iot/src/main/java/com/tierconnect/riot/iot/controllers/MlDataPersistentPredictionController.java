package com.tierconnect.riot.iot.controllers;

import com.tierconnect.riot.iot.entities.exceptions.MLModelException;
import com.tierconnect.riot.iot.services.MlModelService;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.apache.log4j.Logger;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by alfredo on 9/30/16.
 */

@Path("/modelDataPersistentPredictions")
@Api("/modelDataPersistentPredictions")
public class MlDataPersistentPredictionController {

    private static Logger logger = Logger.getLogger(MlDataPersistentPredictionController.class);



    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"mlPrediction:r:{id}"})
    @ApiOperation(position = 1, value = "Get a data persistent prediction")
    public Response getDataRows(@ApiParam(value = "Unique data persistent prediction ID") @PathParam("id") Long id) {

        Response response;

        try {

            Map<String, Object> data = new MlModelService().getDataPersistentPrediction(id);
            response = RestUtils.sendOkResponse(data);

        } catch (MLModelException e) {
            logger.error(e);
            response = RestUtils.sendResponseWithCode(e.getMessage(), 400);
        }

        return response;
    }

}
