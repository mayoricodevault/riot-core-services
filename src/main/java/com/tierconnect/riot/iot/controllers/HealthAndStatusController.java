package com.tierconnect.riot.iot.controllers;

import com.sun.jna.StringArray;
import com.tierconnect.riot.iot.services.HealthAndStatusService;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.tierconnect.riot.sdk.utils.TimerUtil;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author tcrown
 * 
 */
@Path("/healthAndStatus")
@Api("/healthAndStatus")
public class HealthAndStatusController
{
	private Logger logger = Logger.getLogger( HealthAndStatusController.class );

	@Context
	ServletContext context;

	@GET
	@Path("/")
	// @Produces(MediaType.APPLICATION_JSON)
	// 1a. Limit access based on CLASS level resources
	// @RequiresPermissions(value = { "edgebox:r" })
	@ApiOperation(position = 1, value = "Get System Health and Status")
	public Response getStatus( @DefaultValue("0") @QueryParam("time") long time )
	{
		TimerUtil tu = new TimerUtil();
		tu.mark();

		logger.debug( "time=" + time );

		long timeSize = tu.getTime( 0 ) - time;
        StringBuffer sb = new StringBuffer();

        for(HealthAndStatusService ss : HealthAndStatusService.getInstances()){
            int lines = 0;

            synchronized (ss.values) {
                for (HealthAndStatusService.Value v : ss.values) {
                    if (v.time > time) {
                        sb.append(v.bridgeCode + "," + v.propertyName + "," + v.time + "," + v.value + "\n");
                        lines++;
                    }
                }
            }
            tu.mark();

            logger.info( "timeSize=" + timeSize + " totalLines=" + ss.values.size() + " lines=" + lines + " connectionCode=" + ss.connectionCode + " delt=" + tu.getLastDelt() );
        }
		return Response.status( 200 ).header( "content-type", "text/plain" ).entity( sb.toString() ).build();
	}

	@GET
	@Path("/load")
	@ApiOperation(position = 1, value = "load Health and Status data from file")
	public Response getFile()
	{
        List<String> exceptions = new ArrayList<>();
        for(HealthAndStatusService ss : HealthAndStatusService.getInstances()){
            try
            {
                ss.loadFromFile();
            }
            catch( IOException e )
            {
                exceptions.add(e.getMessage());
            }
        }

        if(exceptions.size() > 0){
            Response.status( 500 ).header( "content-type", "text/plain" ).entity(StringUtils.join(exceptions, "\n")).build();
        }

        return Response.status( 200 ).header( "content-type", "text/plain" ).entity( "ok" ).build();
	}

	@PUT
	@Path("/save")
	@ApiOperation(position = 1, value = "save Health and Status data to file")
	public Response writeFile()
	{
        List<String> exceptions = new ArrayList<>();
        for(HealthAndStatusService ss : HealthAndStatusService.getInstances()){
            try
            {
                ss.saveToFile();
            }
            catch( IOException e )
            {
                exceptions.add(e.getMessage());
            }
        }

        if(exceptions.size() > 0){
            Response.status( 500 ).header( "content-type", "text/plain" ).entity(StringUtils.join(exceptions, "\n")).build();
        }

		return Response.status( 200 ).header( "content-type", "text/plain" ).entity( "ok" ).build();
	}

	@GET
	@Path("/maxage")
	@ApiOperation(position = 1, value = "get Health and Status max data age")
	public Response getMaxDataAge()
	{
        StringBuffer sb = new StringBuffer();

		for(HealthAndStatusService ss : HealthAndStatusService.getInstances()){
            sb.append(ss.getMaxDataAge() + "n");
        }
		return Response.status( 200 ).header( "content-type", "text/plain" ).entity( sb.toString() ).build();
	}

	@PUT
	@Path("/maxage")
	@ApiOperation(position = 1, value = "set Health and Status max data age")
	public Response setMaxDataAge( @DefaultValue("3600000") @QueryParam("time") long age )
	{
        for(HealthAndStatusService ss : HealthAndStatusService.getInstances()){
            ss.setMaxDataAge( age );
        }
		return Response.status( 200 ).header( "content-type", "text/plain" ).entity( age ).build();
	}
	
	@GET
	@Path("/loggers")
	@ApiOperation(position = 1, value = "get log4 loggers logging level")
	public Response getLoggers()
	{
        StringBuffer sb = new StringBuffer();

        for(HealthAndStatusService ss : HealthAndStatusService.getInstances()){
            sb.append(ss.getLoggers() + "n");
        }
		return Response.status( 200 ).header( "content-type", "text/plain" ).entity( sb.toString() ).build();
	}
	
	@POST
	@Path("/loggers")
	@ApiOperation(position = 1, value = "set log4 loggers logging level")
	public Response setLoggers( String body )
	{
        for(HealthAndStatusService ss : HealthAndStatusService.getInstances()){
            ss.setLoggers( body );
        }
        return Response.status( 200 ).header( "content-type", "text/plain" ).entity( "ok" ).build();
	}

	@GET
	@Path("/applicationInfo")
	@ApiOperation(position = 1, value = "get application information")
	public Response getApplicationInfo()
	{
        return RestUtils.sendOkResponse(HealthAndStatusService.getApplicationInfo());
	}

}
