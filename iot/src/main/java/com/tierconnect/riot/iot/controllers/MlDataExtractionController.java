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
import java.util.*;

/**
 * Created by alfredo on 9/27/16.
 */

@Path("/modelDataExtractions")
@Api("/modelDataExtractions")
public class MlDataExtractionController {

    private static Logger logger = Logger.getLogger(MlDataExtractionController.class);

    /**
     * Returns a certain number of rows from an extraction (the data itself).
     * It is important to keep clear that the data of an extraction is like a table composed
     * of rows and columns. Each column is an attribute / property of the extracted data (e.g. date, zone)
     * and each row is a set of values for these attributes / properties.
     *
     * This endpoint provides rows of an extracted data as resources. So each row is considered a resource,
     * being the id of a resource row just the number of the row. The whole extracted data (all the rows)
     * is also considered as a resource whose id is the same as the id of the related extraction.
     *
     * The fact of providing rows as resources was done in order to apply pagination concepts in a clean way.
     * Thanks to this, it is possible to query for just some rows of the extracted data, and avoid in this way
     * to receive the whole extracted data.
     *
     * Query example:
     *
     * GET /modelDataExtractions/27/rows?pageSize=50,pageNumber=3
     *
     * @param id identifier of data extraction
     * @return
     */
    @GET
    @Path("/{id}/rows")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"mlExtraction:r:{id}"})
    @ApiOperation(position = 1, value = "Get rows from an extraction")
    public Response getDataRows(
            @ApiParam(value = "Unique data extraction ID") @PathParam("id") Long id,
            @ApiParam(value = "Page size") @QueryParam("pageSize") Long pageSize,
            @ApiParam(value = "Page number") @QueryParam("pageNumber") Long pageNumber) {

        Response response;

        try {
            MlModelService service = new MlModelService();
            List<MlModelService.DataRow> rows = service.getDataRowsExtraction(id, (pageNumber - 1) * pageSize, pageSize);

            List<Map<String, Object>> list = new ArrayList<>();
            for(MlModelService.DataRow row : rows) { list.add(row.asMap()); }

            response = RestUtils.sendOkResponse(list);
        } catch (MLModelException e) {
            response = RestUtils.sendResponseWithCode(e.getMessage(), 400);
        }

        return response;

    }

}
