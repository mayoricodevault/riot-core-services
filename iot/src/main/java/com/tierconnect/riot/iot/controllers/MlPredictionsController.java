package com.tierconnect.riot.iot.controllers;

import com.tierconnect.riot.appcore.controllers.GroupController;
import com.tierconnect.riot.appcore.controllers.RiotShiroRealm;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.utils.VisibilityUtils;
import com.tierconnect.riot.iot.entities.MlExtractionPredictor;
import com.tierconnect.riot.iot.services.MlModelService;
import com.tierconnect.riot.iot.entities.exceptions.MLModelException;
import com.tierconnect.riot.sdk.utils.PermissionsUtils;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by pablo on 6/14/16.
 *
 * Test Machine learning controller to test integration with Spark Job Server
 */

@Path("/modelPredictions")
@Api("/modelPredictions")
public class MlPredictionsController {
    static Logger logger = Logger.getLogger(MlPredictionsController.class);

    /**
     *
     *
     * @param modelId model id
     * @param advanced
     * @param startDate start date for predictions
     * @param endDate end date for predictions
     * @param predictors prediction axis
     * @param year if adv year of prediction YYYY
     * @param month if advance month of prediction MM
     * @param weekOfTheMonth of advance week of the month for prediction from 1 to 6
     * @param dayOfTheMonth if advance day of the month for prediction from 1 to 31 but depends of month
     * @param dayOfTheWeek if advance day of the week for prediction from 1 to seven stating day monday
     * @return predictions in range of combination of predictions.
     */

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    //@Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"mlPrediction:r:{id}"})
    @ApiOperation(position = 1, value = "Query a trained model for a prediction")
    public Response predict(@ApiParam(value = "Unique trained Model ID.") @PathParam("id") Long modelId,
                            @ApiParam(value = "Advance mode flag.") @QueryParam("advanced") Boolean advanced,
                            @ApiParam(value = "startDate required format yyyy-MM-dd")
                            @QueryParam("startDate") String startDate,
                            @ApiParam(value = "endDate required format yyyy-MM-dd")
                            @QueryParam("endDate") String endDate,
                            @ApiParam(value = "predictors' names and values") @QueryParam("predictors") String predictors,
                            //advance options
                            @ApiParam(value = "year") @QueryParam("year") String year,
                            @ApiParam(value = "month") @QueryParam("month") String month,
                            @ApiParam(value = "weekOfTheMonth") @QueryParam("weekOfTheMonth") String weekOfTheMonth,
                            @ApiParam(value = "dayOfTheMonth") @QueryParam("dayOfTheMonth") String dayOfTheMonth,
                            @ApiParam(value = "dayOfTheWeek") @QueryParam("dayOfTheWeek") String dayOfTheWeek) {

        Response response;

        try {

            Map<String, String> params = new HashMap<>();
            logger.info("advanced        : "  + advanced);
            logger.info("month           : "  + month);
            logger.info("wm              : "  + weekOfTheMonth);
            logger.info("dm              : "  + dayOfTheMonth);
            logger.info("dw              : "  + dayOfTheWeek);
            logger.info("trainedModelId  : "  + String.valueOf(modelId));
            logger.info("startDate       : "  + startDate);
            logger.info("endDate         : "  + endDate);
            logger.info("predictors      : "  + predictors);


            if (advanced != null && advanced) {
                params.put("year", year);
                params.put("month", month);
                params.put("weekOfTheMonth", weekOfTheMonth);
                params.put("dayOfTheMonth", dayOfTheMonth);
                params.put("dayOfTheWeek", dayOfTheWeek);
            }
            else {
                params.put("trainedModelId", String.valueOf(modelId));
                params.put("startDate", startDate);
                params.put("endDate", endDate);
            }
            params.put("predictors", predictors.replace(",", ";").replace("||", ","));
            logger.info("==========>>" + params);
            MlModelService service = new MlModelService();
            Map map = service.predict(modelId, params);
            response = RestUtils.sendOkResponse(map);
        }
        catch (MLModelException e) {
            response = RestUtils.sendResponseWithCode(e.getMessage(), 400);
        }
        return response ;
    }

    /**
     * Important!!! This code was added just to get permissions to work.
     */
    @GET
    @Path("/group/")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position=1, value="Get a List of Groups (AUTO)")
    public Response listGroups( @QueryParam("pageSize") Integer pageSize,
                                @QueryParam("pageNumber") Integer pageNumber,
                                @QueryParam("order") String order,
                                @QueryParam("where") String where,
                                @Deprecated @QueryParam("extra") String extra,
                                @Deprecated @QueryParam("only") String only,
                                @QueryParam("visibilityGroupId") Long visibilityGroupId,
                                @DefaultValue("") @QueryParam("upVisibility") String upVisibility,
                                @DefaultValue("") @QueryParam("downVisibility") String downVisibility,
                                @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend,
                                @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project  )
    {
        validateListPermissions();
        GroupController c = new GroupController();
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(MlExtractionPredictor.class.getCanonicalName(), visibilityGroupId);
        RiotShiroRealm.getOverrideVisibilityCache().put(Group.class.getCanonicalName(), visibilityGroup);
        return c.listGroups (pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility, downVisibility, extend, project);
    }

    @GET
    @Path("/group/tree")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position=1, value="Get a Tree of Groups (AUTO)")
    public Response listGroupsInTree( @QueryParam("pageSize") Integer pageSize,
                                      @QueryParam("pageNumber") Integer pageNumber,
                                      @QueryParam("order") String order,
                                      @QueryParam("where") String where,
                                      @Deprecated @QueryParam("extra") String extra,
                                      @Deprecated @QueryParam("only") String only,
                                      @QueryParam("visibilityGroupId")
                                          Long visibilityGroupId, @DefaultValue("") @QueryParam("upVisibility") String upVisibility, @DefaultValue("") @QueryParam("downVisibility") String downVisibility, @QueryParam("topId") String topId,
                                      @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend,
                                      @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
    {
        validateListPermissions();
        GroupController c = new GroupController();
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(MlExtractionPredictor.class.getCanonicalName(), visibilityGroupId);
        RiotShiroRealm.getOverrideVisibilityCache().put(Group.class.getCanonicalName(), visibilityGroup);
        return c.listGroupsInTree(pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility, downVisibility, topId,false, extend, project);
    }

    public void validateListPermissions() {
        if (!(PermissionsUtils.isPermitted(SecurityUtils.getSubject(),"mlPrediction:r"))) {
            throw new com.tierconnect.riot.sdk.servlet.exception.ForbiddenException("Not Authorized, Access Denied");
        }
    }
}
