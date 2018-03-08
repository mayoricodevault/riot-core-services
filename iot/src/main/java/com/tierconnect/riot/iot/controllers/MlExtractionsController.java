package com.tierconnect.riot.iot.controllers;

import com.tierconnect.riot.appcore.controllers.GroupController;
import com.tierconnect.riot.appcore.controllers.RiotShiroRealm;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.utils.VisibilityUtils;
import com.tierconnect.riot.iot.dao.MlBusinessModelPredictorDAO;
import com.tierconnect.riot.iot.dao.MlExtractionDAO;
import com.tierconnect.riot.iot.entities.MlExtraction;
import com.tierconnect.riot.iot.entities.exceptions.MLModelException;
import com.tierconnect.riot.iot.services.MlModelService;
import com.tierconnect.riot.sdk.utils.PermissionsUtils;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.jaxrs.PATCH;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Manages models
 *
 * @author Alfredo Villalba
 */
@Path("/modelExtractions")
@Api("/modelExtractions")

public class MlExtractionsController {

    static Logger logger = Logger.getLogger(MlExtractionsController.class);


    /**
     * Endpoint to start extracting features for a model
     *
     * example:
     *
     *  {
     *      "businessModelId" : 1,
     *      "groupId" : 3,
     *      "name" : "Extraction with Data from Summer 2015",
     *      "comments" : "Bla bla blab bla... and bla bla bla",
     *      "startDate" : "2016-03-01",
     *      "endDate" : "2016-04-01",
     *      "inputs" :
     *      [
     *          { "predictorId" : 1, "thingTypeId" : 3, "property" : "Time" },
     *          { "predictorId" : 2, "thingTypeId" : 3, "property" : "Zone" }
     *      ]
     *   }
     *
     * @param paramMap map from this json:
     * @return id of the new extraction
     */

    @POST
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @RequiresPermissions(value={"mlExtraction:i"})
    @ApiOperation(position = 1, value = "Create a features extraction")
    public Response create(Map<String, Object> paramMap) {

        Response response;
        try {
            //parse parameters
            Long businessModelId = ((Integer) paramMap.get("businessModelId")).longValue();
            Long groupId = ((Integer) paramMap.get("groupId")).longValue();
            String name = (String) paramMap.get("name");
            String comments = (String) paramMap.get("comments");
            LocalDate start = LocalDate.parse((String) paramMap.get("startDate"));
            LocalDate end = LocalDate.parse((String) paramMap.get("endDate"));;

            //call service
            MlModelService service = new MlModelService();
            Long id = service.extract(businessModelId, groupId, name, comments,
                    start, end, extractMlPredictors((List)paramMap.get("inputs")));

            response = RestUtils.sendOkResponse(id.toString());
        }
        catch (MLModelException |DateTimeParseException e) {
            response = RestUtils.sendResponseWithCode(e.getMessage(), 400);
        }
        return response ;

    }

    private List<MlModelService.Predictor> extractMlPredictors(List maps) {
        List<MlModelService.Predictor> predictors = new ArrayList<>();
        logger.info("}{!}{}${}}@!>>>>>>>>>>>>>>>>> maps : " + maps );
        for (Object o : maps) {
            Map map = (Map) o;
            predictors.add(
                    new MlModelService.Predictor(
                            ((Integer)map.get("thingTypeId")).longValue(),
                            ((Integer)map.get("predictorId")).longValue(),
                            (String) map.get("property"),
                            (String) map.get("featureName")

                    )
            );
        }

        return predictors;
    }


    //todo  put pagination, group, visibility?
    //todo right now it pulls ALL models.
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @RequiresPermissions(value={"mlExtraction:r"})
    @ApiOperation(position = 1, value = "Get the list of ids, names and tenants of all models")
    public Response getModels(@ApiParam(value = "group filter.") @QueryParam("groupId") Long groupId,
                              @ApiParam(value = "model filter.") @QueryParam("businessModelId") Long businessModelId,
                              @ApiParam(value = "completed.") @QueryParam("completed") Boolean completed) {

        Response response;

        List<MlExtraction> extractions = new MlModelService().extractions(groupId, businessModelId, completed);

        List<Map<String, Object>> extractionMaps = new ArrayList<>();

        for (MlExtraction extraction: extractions) {
            extractionMaps.add(extraction.toMap());
        }

        Map<String, Object> res = new HashMap<>();
        res.put("total", extractions.size());
        res.put("results", extractionMaps);


        response = RestUtils.sendOkResponse(res);

        return response;
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    //@Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"mlExtraction:r:{id}"})
    @ApiOperation(position = 1, value = "Get details of a model having a specific id")
    public Response getModelById(@ApiParam(value = "Unique Model ID") @PathParam("id") Long id) {

        MlExtraction extraction = new MlExtractionDAO().selectById(id);

        return extraction != null ? RestUtils.sendOkResponse(extraction.toMap()) :
                RestUtils.sendResponseWithCode("model with id " + id + " does not exist", 404);
    }




    // TODO PATCH method must be atomic - it is not the case for now
    @PATCH
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @RequiresPermissions(value={"mlExtraction:u:{id}"})
    @ApiOperation(position = 1, value = "Update status of an extraction")
    public Response update(@ApiParam(value = "Unique extraction ID") @PathParam("id") String uuid, // TODO this is a hack - should be id instead of uuid - not REST compliant
                           List<Map<String, Object>> paramList) {

        Response response;

        // Get extraction trying first to use UUID and then ID
        // TODO this is not clean - we should use ID always - moreover we assume that UUID and ID won't never be the same
        MlExtraction extraction = null;
        MlExtractionDAO dao = new MlExtractionDAO();
        extraction = dao.selectBy("uuid", uuid);
        if (extraction == null) {
            try {
                extraction = dao.selectById(Long.parseLong(uuid));
            }
            catch (NumberFormatException e) {
                extraction = null;
            }
        }

        // Process operations
        if (extraction != null) {
            for(Map<String, Object> op : paramList) {
                logger.info("operation: " + op);
                switch((String) op.get("op")) {
                    case "replace" :
                        String path = (String) op.get("path");
                        String value = (String) op.get("value");
                        switch (path) {
                            case "/status" :
                                extraction.setStatus(value);
                                break;
                            case "/name" :
                                extraction.setName(value);
                                break;
                            case "/comments":
                                extraction.setComments(value);
                                break;
                        }
                        break;
                }
            } // next operation
            response = RestUtils.sendOkResponse("Patch successfully processed");
        }
        else {
            response = RestUtils.sendBadResponse("Patch failed");
        }

        return response;
    }

    /**
     * Important!!! This code was added just to get permissions to work.
     */
    @GET
    @Path("/group/")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position=1, value="Get a List of Groups (AUTO)")
    public Response listGroups( @QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber, @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only, @QueryParam("visibilityGroupId") Long visibilityGroupId, @DefaultValue("") @QueryParam("upVisibility") String upVisibility, @DefaultValue("") @QueryParam("downVisibility") String downVisibility, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project  )
    {
        validateListPermissions();
        GroupController c = new GroupController();
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(MlExtraction.class.getCanonicalName(), visibilityGroupId);
        RiotShiroRealm.getOverrideVisibilityCache().put(Group.class.getCanonicalName(), visibilityGroup);
        return c.listGroups (pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility, downVisibility, extend, project);
    }

    @GET
    @Path("/group/tree")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position=1, value="Get a Tree of Groups (AUTO)")
    public Response listGroupsInTree( @QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber, @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only, @QueryParam("visibilityGroupId") Long visibilityGroupId, @DefaultValue("") @QueryParam("upVisibility") String upVisibility, @DefaultValue("") @QueryParam("downVisibility") String downVisibility, @QueryParam("topId") String topId, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
    {
        validateListPermissions();
        GroupController c = new GroupController();
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(MlExtraction.class.getCanonicalName(), visibilityGroupId);
        RiotShiroRealm.getOverrideVisibilityCache().put(Group.class.getCanonicalName(), visibilityGroup);
        return c.listGroupsInTree(pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility, downVisibility, topId,false, extend, project);
    }

    public void validateListPermissions() {
        if (!(PermissionsUtils.isPermitted(SecurityUtils.getSubject(),"mlExtraction:r"))) {
            throw new com.tierconnect.riot.sdk.servlet.exception.ForbiddenException("Not Authorized, Access Denied");
        }
    }
}
