package com.tierconnect.riot.iot.controllers;

import com.tierconnect.riot.iot.services.SecondLevelCacheService;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.apache.log4j.Logger;
import org.apache.shiro.authz.annotation.RequiresAuthentication;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by vealaro on 8/16/16.
 */
@Path("/secondLevelCache")
@Api("/secondLevelCache")
public class SecondLevelCacheController {

    static Logger logger = Logger.getLogger(SecondLevelCacheController.class);

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(value = "clear second level cache hibernate", position = 1,
            notes = "This method clean second level cache and hazelcast maps")
    @ApiResponses(
            value = {
                    @ApiResponse(code = 200, message = "Ok"),
                    @ApiResponse(code = 400, message = "Bad Request")
            }
    )
    public Response clearCache() {
        logger.info("Clear second level hibernate");
        try {
            SecondLevelCacheService.getInstance().clearCache();
            return RestUtils.sendOkResponse("clear cache", true);
        } catch (Exception e) {
            logger.error("Error in clean second level");
            return RestUtils.sendBadResponse("Error at clean second level cache");
        }
    }
}
