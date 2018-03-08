package com.tierconnect.riot.iot.controllers;

import com.tierconnect.riot.iot.entities.MlModel;
import com.tierconnect.riot.iot.entities.MlPrediction;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by alfredo on 10/4/16.
 */

@Path("/modelPersistentPredictions")
@Api("/modelPersistentPredictions")
public class MlPersistentPredictionController {

    private static Logger logger = Logger.getLogger(MlPersistentPredictionController.class);


    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @RequiresPermissions(value={"mlPrediction:i"})
    @ApiOperation(position = 1, value = "Create a persistent prediction")
    public Response create(Map<String, String> params) {

        Response response;

        try {

            params.put("predictors", params.get("predictors").replace(",", ";").replace("||", ","));
            Long id = new MlModelService().createPersistentPrediction(params);

            Map<String, Object> map = new HashMap<>();
            map.put("id", id.toString());
            response = RestUtils.sendCreatedResponse(map);

        } catch (MLModelException e) {
            response = RestUtils.sendResponseWithCode(e.getMessage(), 400);
        }

        return response;

    }



    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @RequiresPermissions(value={"mlPrediction:r"})
    @ApiOperation(position = 1, value = "Get a list of persistent predictions")
    public Response getModels(@ApiParam(value = "Group ID") @QueryParam("groupId") Long groupId,
                              @ApiParam(value = "Model ID") @QueryParam("modelId") Long modelId) {

        Response response;

        List<MlPrediction> predictions = new MlModelService().getPersistentPredictions(groupId, modelId);
        List<Map<String, Object>> predictionsMaps = new ArrayList<>();
        for (MlPrediction p: predictions) { predictionsMaps.add(p.toMap()); }

        Map<String, Object> res = new HashMap<>();
        res.put("total", predictionsMaps.size());
        res.put("results", predictionsMaps);

        response = RestUtils.sendOkResponse(res);

        return response;
    }


    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @RequiresPermissions(value={"mlPrediction:r:{id}"})
    @ApiOperation(position = 1, value = "Get the details of a persistent prediction")
    public Response getModels(@ApiParam(value = "Unique persistent prediction ID") @PathParam("id") Long id) {

        Response response = null;
        try {
            MlModelService service = new MlModelService();
            MlPrediction prediction = service.getPersistentPrediction(id);
            if (prediction != null) {
                Map<String, Object> data = service.getDataPersistentPrediction(id);
                List<String> headers = (List<String>) data.get("headers");
                Map<String,Object> predictionModelInfo = prediction.toMap();
                predictionModelInfo.put("headers", headers);
                response = RestUtils.sendOkResponse(predictionModelInfo);
            } else {
                response = RestUtils.sendResponseWithCode("Persistent prediction not found", 404);
            }
        } catch (MLModelException e) {
            e.printStackTrace();
        }
        return response;
    }




}
