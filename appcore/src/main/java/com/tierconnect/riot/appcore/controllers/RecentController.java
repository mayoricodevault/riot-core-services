package com.tierconnect.riot.appcore.controllers;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.Recent;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.RecentService;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.wordnik.swagger.annotations.*;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.subject.Subject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 *
 * @author : aruiz
 * @date : 4/4/17 5:32 PM
 * @version:
 */
@Path("/recent")
@Api("/recent")
public class RecentController extends RecentControllerBase {
    private static Logger logger = Logger.getLogger(RecentController.class);

    @PUT
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(value = "Create Recent",
            position = 4,
            notes = "This method permits user to create a recent item." +
                    "<br>{\n" +
                    "<br>&nbsp;&nbsp;  \"elementId\": 1,\n" +
                    "<br>&nbsp;&nbsp;  \"typeElement\": \"thing,\"\n" +
                    "<br>&nbsp;&nbsp;  \"elementName\": \"thing001\"\n" +
                    "<br>}")
    @ApiResponses(
            value = {
                    @ApiResponse(code = 200, message = "Ok"),
                    @ApiResponse(code = 201, message = "Created"),
                    @ApiResponse(code = 400, message = "Bad Request"),
                    @ApiResponse(code = 403, message = "Forbidden"),
                    @ApiResponse(code = 500, message = "Internal Server Error")
            }
    )
    public Response insertRecent(
            @ApiParam(value = "element Id" ) Map<String, Object> recentMap) {
        Map<String,Object> result = null;
        result = RecentService.getInstance().createRecent(recentMap);
        return RestUtils.sendOkResponse(result);
    }

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 2, value = "Get list of Recents ",notes="Get list of Recents")
    @ApiResponses(
            value = {
                    @ApiResponse(code = 200, message = "Ok"),
                    @ApiResponse(code = 400, message = "Bad Request"),
                    @ApiResponse(code = 403, message = "Forbidden"),
                    @ApiResponse(code = 500, message = "Internal Server Error")
            }
    )
    public Response listRecents(@QueryParam("typeElement") String typeElement, @QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber, @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only, @QueryParam("visibilityGroupId") Long visibilityGroupId, @DefaultValue("") @QueryParam("upVisibility") String upVisibility, @DefaultValue("") @QueryParam("downVisibility") String downVisibility, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project ){
        Map<String, Object> mapResponse = new HashMap<>();

        try {
            Subject subject = SecurityUtils.getSubject();
            User currentUser = (User) subject.getPrincipal();

            List<Recent> list = RecentService.getInstance().listRecents(currentUser.getId(), typeElement, order);

            List result = new ArrayList<>();

            if (typeElement.equals("group")){
                for (Recent recent : list) {
                    Group group = GroupService.getInstance().get(recent.getElementId());
                    if (group != null) {
                        Map<String, Object> mapRecent = recent.publicMap();
                        mapRecent.put("treeLevel", group.getTreeLevel());
                        result.add(mapRecent);
                    } else {
                        logger.warn("Recent: group with ID[" + recent.getElementId() + "] not found");
                    }
                }
            }else {
                for (Recent recent:list) {
                    result.add(recent.publicMap());
                }
            }

            Long count = (long) result.size();
            mapResponse.put("total", count);
            mapResponse.put("results", result);

        }catch (Exception e){
            logger.info(e.getMessage());
            return  RestUtils.sendResponseWithCode(e.getMessage(),400);
        }

        return RestUtils.sendOkResponse(mapResponse);
    }

}
