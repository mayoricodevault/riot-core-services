package com.tierconnect.riot.iot.controllers;

/**
 * Created by rchirinos on 10/7/2015.
 */

import javax.annotation.Generated;
import javax.ws.rs.Path;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.utils.QueryUtils;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.iot.entities.DataType;
import com.tierconnect.riot.iot.entities.QDataType;
import com.tierconnect.riot.iot.services.DataTypeService;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.wordnik.swagger.annotations.*;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import com.wordnik.swagger.jaxrs.PATCH;
import javax.ws.rs.Path;
import javax.ws.rs.DefaultValue;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jose4j.json.internal.json_simple.JSONArray;
import org.jose4j.json.internal.json_simple.JSONObject;

import javax.ws.rs.QueryParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.io.*;
import java.net.URISyntaxException;
import java.util.List;


@Path("/dataType")
@Api("/dataType")
@Generated("com.tierconnect.riot.appgen.service.GenController")
public class DataTypeController extends DataTypeControllerBase
{
    //Get values based on ENTITY field
    @GET
    @Path("/entities/{entityValue}")
    @Produces(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresPermissions(value={"dataType:r"})
    @ApiOperation(position=1, value="Get a List of DataTypes based on ENTITY field")
    public Response listDataTypesEntitiesData(
            @PathParam("entityValue") String entityValue
            , @QueryParam("visibilityGroupId") Long visibilityGroupId
            , @DefaultValue("") @QueryParam("upVisibility") String upVisibility
            , @DefaultValue("") @QueryParam("downVisibility") String downVisibility,
            @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend,
            @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
    {
        Integer pageSize = 100;
        Integer pageNumber = 1;
        String order = "code:asc";
        String where = "typeParent=" + entityValue + "";
        String extra = null;
        String only = "id, code, value, type, description";

        return listDataTypes(pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility, downVisibility,false, extend, project);
    }

    /*Get value of a code in cnf_entity_description*/
    public DataType getEntityDescriptionByEntityCode(String entityCode, String code)
    {
        DataType response = null;
        BooleanBuilder be = new BooleanBuilder();
        be = be.and( QueryUtils.buildSearch(QDataType.dataType, "typeParent=" + entityCode + "&code=" + code + "") );
        List data = DataTypeService.getInstance().listPaginated( be, null, "" );
        if(data.size() > 0 )
        {
            response  = (DataType) data.get(0);
        }
        return response;
    }

    @GET
    @Path("/icons")
    @Produces(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresPermissions(value={"dataType:r"})
    @ApiOperation(position=1, value="Get a list of icons from css codes")
    public Response listIcons() {
        String resourcesPath = Constants.TYPE_ICON_PATH;
        try {
            String path = FilenameUtils.getFullPath(
                this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath())
                + "../../" + resourcesPath;
            JSONObject json = new JSONObject();
            JSONArray array = new JSONArray();
            for (File file : (new File(path + "/font")).listFiles()) {
                array.add(file.getName());
            }
            json.put("font", array);
            array = new JSONArray();
            for (File file : (new File(path + "/css")).listFiles()) {
                array.add(file.getName());
            }
            json.put("css", array);
            InputStream is = new FileInputStream(path + "/css/" + Constants.TYPE_ICON_PREFIX + "-codes.css");
            if (is == null) {
                return RestUtils.sendBadResponse("Cannot find ViZix codes.");
            }
            String[] text = IOUtils.toString(is, "UTF-8").split("\n");
            array = new JSONArray();
            for (String line : text) {
                if (!StringUtils.isBlank(line)) {
                    array.add(line.substring(1).split(":")[0]);
                }
            }
            json.put("codes", array);
            json.put("path", resourcesPath);
            return RestUtils.sendOkResponse(json);
        } catch (Exception e) {
            return RestUtils.sendBadResponse(e.getMessage());
        }
    }

}
