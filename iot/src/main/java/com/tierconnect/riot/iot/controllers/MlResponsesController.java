package com.tierconnect.riot.iot.controllers;

import com.tierconnect.riot.iot.entities.exceptions.MLModelException;
import com.tierconnect.riot.iot.services.MlModelService;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.apache.log4j.Logger;
import org.apache.shiro.authz.annotation.RequiresAuthentication;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by alfredo on 11/29/16.
 */

@Path("/modelResponses")
@Api("/modelResponses")
public class MlResponsesController {

    private static Logger logger = Logger.getLogger(MlDataExtractionController.class);


    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 1, value = "Update a response")
    public Response update(
            @ApiParam(value = "Unique response ID") @PathParam("id") String responseId,
            String data)
    {
        Response response;
        try {
            logger.info("update response: " + responseId + " ==>  \n " +
                    (data.length() > 1000 ? data.substring(0,1000) + "...(TOO LARGE)" : data));
            String message = new MlModelService().handleResponse(responseId, data);
            response =  RestUtils.sendOkResponse(message);

        } catch (MLModelException e) {
            logger.error(e.getMessage(), e);
            response = RestUtils.sendResponseWithCode(e.getMessage(), 400);
        }
        return response;
    }


}
