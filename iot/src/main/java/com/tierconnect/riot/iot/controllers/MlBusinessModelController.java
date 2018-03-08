package com.tierconnect.riot.iot.controllers;

import com.tierconnect.riot.appcore.controllers.GroupController;
import com.tierconnect.riot.appcore.controllers.RiotShiroRealm;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.utils.VisibilityUtils;
import com.tierconnect.riot.iot.dao.MlBusinessModelDAO;
import com.tierconnect.riot.iot.entities.MlBusinessModel;
import com.tierconnect.riot.iot.entities.MlExtraction;
import com.tierconnect.riot.iot.entities.exceptions.MLModelException;
import com.tierconnect.riot.iot.services.MlModelService;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import com.tierconnect.riot.sdk.dao.UserException;
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
import java.io.IOException;
import java.util.*;

/**
 * Created by pablo on 7/26/16.
 * <p>
 * Endpoint to create and list business models.
 */

@Path("/modelBusinesses")
@Api("/modelBusinesses")
public class MlBusinessModelController {

    private static Logger logger = Logger.getLogger(MlBusinessModelController.class);


    /**
     * Query example:
     *
     * POST /modelBusinesses/
     * {
     *   "name" : "Money Mapping Clone",
     *   "description" : "This is another money mapping model...",
     *   "jarId" : "697d7eaa-cb2b-41fb-891a-6f6d2211834f"
     * }
     *
     * Response example:
     *
     * {
     *   "id" : 1
     * }
     *
     * @param params json
     * @return
     * @throws IOException
     * @throws NonUniqueResultException
     * @throws UserException
     */
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @RequiresPermissions(value={"mlBusinessModel:i"})
    @ApiOperation(position = 1, value = "Create a business model")
    public Response create(Map<String, Object> params)
    {

        Response response;

        try {

            // TODO check malformed query

            // Get business model attributes
            String name = (String) params.get("name");
            String desc = (String) params.get("description");
            String jarId = (String) params.get("jarId");

            // Create business model
            MlModelService service = new MlModelService();
            Long id = service.createBusinessModel(name, desc, jarId);

            Map<String, Object> map = new HashMap<>();
            map.put("id", id.toString());
            response =  RestUtils.sendOkResponse(map);

        } catch (MLModelException e) {
            logger.error(e.getMessage(), e);
            response = RestUtils.sendResponseWithCode(e.getMessage(), 400);
        }

        return response;

    }

    /**
     * Query example:
     *
     * GET /modelBusinesses/?groupId=3
     *
     * @param groupId
     * @return
     */
    //todo  put pagination, group, visibility?
    //todo right now it pulls ALL models.
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @RequiresPermissions(value={"mlBusinessModel:r"})
    @ApiOperation(position = 1, value = "Get the list of business models")
    public Response businessModels(@ApiParam(value = "group filter.") @QueryParam("groupId") Long groupId) {

        List<MlBusinessModel> businessModels = new MlModelService().businessModels(groupId);
        List<Map<String, Object>> businessModelMaps = new ArrayList<>();
        for (MlBusinessModel m : businessModels) {
            businessModelMaps.add(m.toMap());
        }

        Map<String, Object> res = new HashMap<>();
        res.put("total", businessModels.size());
        res.put("results", businessModelMaps);

        return RestUtils.sendOkResponse(res);
    }


    /**
     * Query example:
     *
     * GET /modelBusinesses/1
     *
     * @param id identifier of the business model
     * @return
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"mlBusinessModel:r:{id}"})
    @ApiOperation(position = 1, value = "Get details of a specific business model")
    public Response getModelById(@ApiParam(value = "Unique business model ID") @PathParam("id") Long id) {
        MlBusinessModel businessModel = new MlBusinessModelDAO().selectById(id);
        if (businessModel != null) {
            return RestUtils.sendOkResponse(businessModel.toMap());
        }
        else {
            return RestUtils.sendResponseWithCode("Business model with ID " + id + " not found", 404);
        }
    }


    /**
     *
     * Query example:
     *
     * PATCH /modelBusinesses/1
     * [
     *   { "op" : "replace", "path" : "/name",  "value" : "The new name of of this business model" },
     *   { "op" : "replace", "path" : "/description",  "value" : "New blablas blabalb blasss..." }
     * ]
     *
     * @param id identifier of the business model
     * @param paramList json list of operations to be applied
     * @return
     */
    // TODO PATCH method must be atomic - it is not the case for now
    @PATCH
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @RequiresPermissions(value={"mlBusinessModel:u:{id}"})
    @ApiOperation(position = 1, value = "Patch a business model")
    public Response update(@ApiParam(value = "Unique business model ID") @PathParam("id") Long id,
                           List<Map<String, Object>> paramList) {

        Response response;

        MlBusinessModel businessModel = new MlBusinessModelDAO().selectById(id);

        // Process operations
        if (businessModel != null) {
            boolean modified = false;
            for(Map<String, Object> op : paramList) {
                logger.info("operation: " + op);
                switch((String) op.get("op")) {
                    case "replace" :
                        String path = (String) op.get("path");
                        String value = (String) op.get("value");
                        switch (path) {
                            case "/name" :
                                businessModel.setName(value);
                                modified = true;
                                break;
                            case "/description" :
                                businessModel.setDescription(value);
                                modified = true;
                                break;
                        }
                        break;
                }
            } // next operation
            if (modified) { businessModel.setModifiedDate(new Date(System.currentTimeMillis()));}
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
    @ApiOperation(position = 1, value = "Get a List of Groups (AUTO)")
    public Response listGroups(
            @QueryParam("pageSize") Integer pageSize,
            @QueryParam("pageNumber") Integer pageNumber,
            @QueryParam("order") String order,
            @QueryParam("where") String where,
            @Deprecated @QueryParam("extra") String extra,
            @Deprecated @QueryParam("only") String only,
            @QueryParam("visibilityGroupId") Long visibilityGroupId,
            @DefaultValue("") @QueryParam("upVisibility") String upVisibility,
            @DefaultValue("") @QueryParam("downVisibility") String downVisibility,
            @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend,
            @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project) {
        validateListPermissions();
        GroupController c = new GroupController();
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(MlBusinessModel.class.getCanonicalName(),
                visibilityGroupId);
        RiotShiroRealm.getOverrideVisibilityCache().put(Group.class.getCanonicalName(), visibilityGroup);
        return c.listGroups(pageSize, pageNumber, order, where, extra, only, visibilityGroupId,
                upVisibility, downVisibility, extend, project);
    }

    @GET
    @Path("/group/tree")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 1, value = "Get a Tree of Groups (AUTO)")
    public Response listGroupsInTree(@QueryParam("pageSize") Integer pageSize,
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
                                     @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project) {
        validateListPermissions();
        GroupController c = new GroupController();
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(
                MlBusinessModel.class.getCanonicalName(),
                visibilityGroupId);
        RiotShiroRealm.getOverrideVisibilityCache().put(Group.class.getCanonicalName(), visibilityGroup);
        return c.listGroupsInTree(pageSize, pageNumber, order, where, extra, only, visibilityGroupId,
                upVisibility, downVisibility, topId, false, extend, project);
    }

    public void validateListPermissions() {
        if (!(PermissionsUtils.isPermitted(SecurityUtils.getSubject(), "mlBusinessModel:r"))) {
            throw new com.tierconnect.riot.sdk.servlet.exception.ForbiddenException("Not Authorized, Access Denied");
        }
    }

}
