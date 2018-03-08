package com.tierconnect.riot.iot.controllers;

import com.tierconnect.riot.appcore.controllers.GroupController;
import com.tierconnect.riot.appcore.controllers.RiotShiroRealm;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.utils.VisibilityUtils;
import com.tierconnect.riot.iot.dao.MlModelDAO;
import com.tierconnect.riot.iot.entities.MlExtraction;
import com.tierconnect.riot.iot.entities.MlModel;
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
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages models
 *
 * @author Pablo Caballero
 * @author Alfredo Villalba
 */
@Path("/modelTrainings")
@Api("/modelTrainings")
public class MlModelController {
    static Logger logger = Logger.getLogger(MlModelController.class);


    //todo  put pagination, visibility?
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @RequiresPermissions(value={"mlModel:r"})
    @ApiOperation(position = 1, value = "Get the list of ids, names and tenants of all models")
    public Response getModels(@ApiParam(value = "group filter.") @QueryParam("groupId") Long groupId,
                              @ApiParam(value = "model filter.") @QueryParam("businessModelId") Long businessModelId,
                              @ApiParam(value = "completed.") @QueryParam("completed") Boolean completed) {

        Response response;

        List<MlModel> models = new MlModelService().models(groupId, businessModelId, completed);

        List<Map<String, Object>> modelMaps = new ArrayList<>();

        for (MlModel model: models) {
            modelMaps.add(model.toMap());
        }

        Map<String, Object> res = new HashMap<>();
        res.put("total", modelMaps.size());
        res.put("results", modelMaps);

        response = RestUtils.sendOkResponse(res);

        return response;
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    //@Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"mlModel:r:{id}"})
    @ApiOperation(position = 1, value = "Get details of a model having a specific id")
    public Response getModelById(@ApiParam(value = "Unique Model ID") @PathParam("id") Long id) {

        MlModel model = new MlModelDAO().selectById(id);

        return model != null ? RestUtils.sendOkResponse(model.toMap()) :
                RestUtils.sendResponseWithCode("model with id " + id + " does not exist", 404);
    }

    /**
     * Endpoint to start training a model
     * example:
     *
     *  {
     *      "groupId":3,
     *      "extractionId":6,
     *      "name":"model name",
     *      "comments":"bla bla bla"
     *  }
     *
     * @param paramMap map from this json:
     * @return id of the new extraction
     */

    @POST
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @RequiresPermissions(value={"mlModel:i"})
    @ApiOperation(position = 1, value = "Create a trained model")
    public Response train(Map<String, Object> paramMap) {
        Response response;
        try {
            // Parse parameters
            Long groupId = ((Integer) paramMap.get("groupId")).longValue();
            Long extractionId = ((Integer) paramMap.get("extractionId")).longValue();
            String name = (String) paramMap.get("name");
            String comments = (String) paramMap.get("comments");

            // Call service
            MlModelService service = new MlModelService();
            Long id = service.train(groupId, extractionId, name, comments);

            response = RestUtils.sendOkResponse(id.toString());
        }
        catch (MLModelException|DateTimeParseException e) {
            response = RestUtils.sendResponseWithCode(e.getMessage(), 400);
        }
        return response ;
    }



    // TODO PATCH method must be atomic - it is not the case for now
    @PATCH
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @RequiresPermissions(value={"mlModel:u"})
    @ApiOperation(position = 1, value = "Update status, name and comments of a training")
    public Response update(@ApiParam(value = "Unique extraction ID") @PathParam("id") String uuid, // TODO this is a hack - should be id instead of uuid - not REST compliant
                           List<Map<String, Object>> paramList) {

        Response response;

        // Get model trying first to use UUID and then ID
        // TODO this is not clean - we should use ID always - moreover we assume that UUID and ID won't never be the same
        MlModel model = null;

        MlModelDAO dao = new MlModelDAO();
        model = dao.selectBy("uuid", uuid);
        if (model == null) {
            try {
                model = dao.selectById(Long.parseLong(uuid));
            }
            catch (NumberFormatException e) {
                model = null;
            }
        }

        // Process operations
        if (model != null) {

            for(Map<String, Object> op : paramList) {

                logger.info("operation: " + op);

                switch((String) op.get("op")) {

                    case "replace" :
                        String path = (String) op.get("path");
                        String value = (String) op.get("value");
                        switch (path) {
                            case "/status" :
                                model.setStatus(value);
                                break;
                            case "/name" :
                                model.setName(value);
                                break;
                            case "/comments":
                                model.setComments(value);
                                break;
                            case "/error":
                                model.setError(value);
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
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(MlModel.class.getCanonicalName(),
                visibilityGroupId);
        RiotShiroRealm.getOverrideVisibilityCache().put(Group.class.getCanonicalName(), visibilityGroup);
        return c.listGroups (pageSize, pageNumber, order, where, extra, only, visibilityGroupId,
                upVisibility, downVisibility, extend, project);
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
                                      @QueryParam("visibilityGroupId") Long visibilityGroupId,
                                      @DefaultValue("") @QueryParam("upVisibility") String upVisibility,
                                      @DefaultValue("") @QueryParam("downVisibility") String downVisibility,
                                      @QueryParam("topId") String topId,
                                      @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend,
                                      @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
    {
        validateListPermissions();
        GroupController c = new GroupController();
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(
                MlModel.class.getCanonicalName(),
                visibilityGroupId);
        RiotShiroRealm.getOverrideVisibilityCache().put(Group.class.getCanonicalName(), visibilityGroup);
        return c.listGroupsInTree(pageSize, pageNumber, order, where, extra, only, visibilityGroupId,
                upVisibility, downVisibility, topId, false, extend, project);
    }

    public void validateListPermissions() {
        if (!(PermissionsUtils.isPermitted(SecurityUtils.getSubject(),"mlModel:r"))) {
            throw new com.tierconnect.riot.sdk.servlet.exception.ForbiddenException("Not Authorized, Access Denied");
        }
    }
}
