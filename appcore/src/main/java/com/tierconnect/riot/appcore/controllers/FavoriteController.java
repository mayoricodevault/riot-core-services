package com.tierconnect.riot.appcore.controllers;

import com.tierconnect.riot.appcore.entities.Favorite;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.services.FavoriteService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.utils.QueryUtils;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.wordnik.swagger.annotations.*;
import com.wordnik.swagger.jaxrs.PATCH;
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

@Path("/favorite")
@Api("/favorite")
public class FavoriteController extends FavoriteControllerBase
{
    private static Logger logger = Logger.getLogger(FavoriteController.class);

    @PUT
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(value = "Create Favorite",
            position = 4,
            notes = "This method permits user to create a favorite item." +
                    "<br>{\n" +
                    "<br>&nbsp;&nbsp;  \"elementId\": 1,\n" +
                    "<br>&nbsp;&nbsp;  \"typeElement\": \"thing\",\n" +
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
    public Response insertFavorite(
            @ApiParam(value = "element Id" ) Map<String, Object> favoriteMap) {

        Map<String,Object> result = null;
        try {
            result = FavoriteService.getInstance().createFavorite(favoriteMap, getEntityVisibility());
        } catch (Exception e) {
            logger.info(e.getMessage());
            return  RestUtils.sendResponseWithCode(e.getMessage(),400);
        }
        return RestUtils.sendOkResponse(result);
    }

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @Override
    @ApiOperation(position = 2, value = "Get list of Favorites",notes="Get list of Favorites")
    @ApiResponses(
            value = {
                    @ApiResponse(code = 200, message = "Ok"),
                    @ApiResponse(code = 400, message = "Bad Request"),
                    @ApiResponse(code = 403, message = "Forbidden"),
                    @ApiResponse(code = 500, message = "Internal Server Error")
            }
    )
    public Response listFavorites(@QueryParam("typeElement") String typeElement, @QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber, @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only, @QueryParam("visibilityGroupId") Long visibilityGroupId, @DefaultValue("") @QueryParam("upVisibility") String upVisibility, @DefaultValue("") @QueryParam("downVisibility") String downVisibility, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project ){
        Map<String, Object> mapResponse = new HashMap<>();

        try {
            Subject subject = SecurityUtils.getSubject();
            User currentUser = (User) subject.getPrincipal();

            List<Favorite> list = FavoriteService.getInstance().listFavorites(currentUser.getId(), typeElement, order, getEntityVisibility(), visibilityGroupId);

            List result = new ArrayList<>();
            if (typeElement.equals("group")){
                for (Favorite favorite : list) {
                    Group group = GroupService.getInstance().get(favorite.getElementId());
                    if (group != null) {
                        Map<String, Object> publicMap = QueryUtils.mapWithExtraFields(favorite, extra, getExtraPropertyNames());
                        publicMap.put("treeLevel", group.getTreeLevel());
                        result.add(publicMap);
                    } else {
                        logger.warn("Favorite: group with ID[" + favorite.getElementId() + "] not found");
                    }
                }
            }else {
                for (Favorite favorite : list) {
                    Map<String,Object> publicMap = QueryUtils.mapWithExtraFields( favorite, extra, getExtraPropertyNames());
                    result.add(publicMap);
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
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position=5, value="Delete a Favorite")
    public Response deleteFavorite( @PathParam("id") String id)
    {
        Favorite favorite = FavoriteService.getInstance().get(Long.parseLong(id));

        if (favorite == null){
            return RestUtils.sendBadResponse( String.format( "FavoriteId[%s] not found", id) );
        }

        try {
            FavoriteService.getInstance().delete(favorite);
        } catch (Exception e) {
            logger.info(e.getMessage());
            return  RestUtils.sendResponseWithCode(e.getMessage(),400);
        }
        return RestUtils.sendDeleteResponse();
    }

    @PATCH
    @Path("/updateSequence")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position=5, value="Update order of favorites")
    public Response updateSequence(List<Long> favorites)
    {
        List<Map<String, Object>> favoriteResult = FavoriteService.getInstance().updateSequence(favorites);
        return RestUtils.sendOkResponse(favoriteResult);
    }
}

