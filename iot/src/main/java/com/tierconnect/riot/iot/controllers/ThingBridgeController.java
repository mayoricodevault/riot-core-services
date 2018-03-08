package com.tierconnect.riot.iot.controllers;

//import com.tierconnect.riot.iot.services.FieldValueService;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.commons.dao.mongo.MongoDAOUtil;

import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.services.BrokerClientHelper;
import com.tierconnect.riot.iot.services.ThingService;
import com.tierconnect.riot.iot.services.ThingTypeService;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import com.tierconnect.riot.sdk.utils.TimerUtil;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.jaxrs.PATCH;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.authz.annotation.RequiresAuthentication;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Random;

//import com.tierconnect.riot.iot.services.ThingFieldService;

/**
 * Created by cfernandez
 * on 5/27/15.
 */

@Path("/thingBridge")
@Api("/thingBridge")
public class ThingBridgeController
{
    static Logger logger = Logger.getLogger(ThingBridgeController.class);

    //TODO: in process, to be used as a general purpose endpoint that puts messages to the MQTT Bus
    // this method DOES NOT call local service or DAOs !!!
    @PUT
	@Path("/thing")
	//@Produces(MediaType.APPLICATION_JSON)
	//@Consumes(MediaType.APPLICATION_JSON)
	@RequiresAuthentication
	@ApiOperation(value = "General Purpose Endoint: Inserts or Updates a Thing For External Systems")
	public Response updateThing( @QueryParam("bridgeCode") String bridgeCode, @QueryParam("thingTypeCode") String thingTypeCode, String body /*Map<String, Object> thingMap*/ )
	{
    	logger.info( "bridgeCode=" + bridgeCode );
    	logger.info( "thingTypeCode=" + thingTypeCode );
    	logger.info( "body=" + body );

    	/*String [] lines = body.split( "\n" );
    	for( String line : lines )
    	{
			// 0001,time,zone,z5
			String[] tokens = line.split( "," );
			String serialNumber = tokens[0];
			long time = System.currentTimeMillis();
			if( !StringUtils.isEmpty( tokens[1] ) )
			{
				time = Long.parseLong( tokens[1] );
			}
			String name = tokens[2];
			String value = tokens[3];


			//TODO: dont use this method here !!!!
    		BrokerClientHelper.sendUpdateThingField( bridgeCode, thingTypeCode, serialNumber, time, name, value );
    		BrokerClientHelper.doNow();
    	}*/
		List<Long> groupMqtt = null;
		try{
			ThingType thingType = ThingTypeService.getInstance().getByCode(thingTypeCode);
			groupMqtt = GroupService.getInstance().getMqttGroups(thingType.getGroup());
		} catch (NonUniqueResultException e) {
			logger.error("Error in get by code Thing Type: "+thingTypeCode+" > "+e.getMessage(), e);
		}
		BrokerClientHelper.publish("/v1/data/"+bridgeCode+"/"+thingTypeCode, body, null, groupMqtt, false, Thread.currentThread().getName());

    	return Response.status(200).header("content-type", "text/plain").build();
	}

    @GET
	@Path("/test/testRestEndpointSubscriber")
	//@Produces(MediaType.APPLICATION_JSON)
	//@Consumes(MediaType.APPLICATION_JSON)
	//@RequiresAuthentication
	@ApiOperation(value = "a dummy test endpoint for the core bridge RestEndpointSubscriber")
	public Response testRestEndpointSubscriberGET( String body )
	{
    	logger.info( "BEGIN testRestEndpointSubscriber" );

    	String [] lines = body.split( "\n" );
    	for( String line : lines )
    	{
			logger.info( "BODY='" + line + "'" );
    	}

    	logger.info( "END testRestEndpointSubscriber" );

    	return Response.status(200).header("content-type", "text/plain").build();
	}

    @POST
	@Path("/test/testRestEndpointSubscriber")
	//@Produces(MediaType.APPLICATION_JSON)
	//@Consumes(MediaType.APPLICATION_JSON)
	//@RequiresAuthentication
	@ApiOperation(value = "a dummy test endpoint for the core bridge RestEndpointSubscriber")
	public Response testRestEndpointSubscriberPOST( String body )
	{
    	logger.info( "BEGIN testRestEndpointSubscriber" );

    	String [] lines = body.split( "\n" );
    	for( String line : lines )
    	{
			logger.info( "BODY='" + line + "'" );
    	}

    	logger.info( "END testRestEndpointSubscriber" );
    	return Response.status(200).header( "content-type", "text/plain" ).build();
	}

	//private fuction for test HTTPThreadPool (coreBridge) aka. RestEndpointSubscriber,
	//the tdd app will generate thousand calls
	//for test purposes we need a valid endpoint for each method  GET, POST, PUT, DELETE and PATCH
	//an have any way to count the calls, (for that we are using httpPoolTest collection in Mongo)

	private String httpThreadToMongo(String method, String serialNumber)
	{
		DBCollection httpPoolTest = MongoDAOUtil.getInstance().db.getCollection( "httpPoolTest" );
		BasicDBObject queryDoc  = new BasicDBObject( "method", method ).append("serialNumber", serialNumber);
		BasicDBObject updateDoc = new BasicDBObject( "$inc", new BasicDBObject( "count", 1 ) );

		DBObject res = httpPoolTest.findAndModify( queryDoc, new BasicDBObject(), null, false, updateDoc, true, true );
		return res.toString();
	}

	@GET
	@Path("/test/HttpThreadPool/{serialNumber}")
	@ApiOperation(value = "a more generic test endpoint for the HTTP Thread Pool implementation of bridge RestEndpointSubscriber")
	public Response testHttpThreadPoolGET( String body,  @PathParam("serialNumber") String serialNumber)
	{
		String res = httpThreadToMongo( "GET", serialNumber );
		logger.info( "testHttpThreadPool: " + res );

		return Response.status(200).header( "content-type", "text/plain" ).entity( res ).build();
	}

	@POST
	@Path("/test/HttpThreadPool/{serialNumber}")
	@ApiOperation(value = "a more generic test endpoint for the HTTP Thread Pool implementation of bridge RestEndpointSubscriber")
	public Response testHttpThreadPoolPOST( String body,  @PathParam("serialNumber") String serialNumber)
	{
		String res = httpThreadToMongo( "POST", serialNumber );
		logger.info( "testHttpThreadPool: " + res );

		//wait between 1 to 9 seconds, before complete this request
		try
		{
			Random r = new Random();
			Thread.sleep( r.nextInt( 100 ) );
		}
		catch( InterruptedException e )
		{

		}

		return Response.status(200).header( "content-type", "text/plain" ).entity( res ).build();
	}

	@PUT
	@Path("/test/HttpThreadPool/{serialNumber}")
	@ApiOperation(value = "a more generic test endpoint for the HTTP Thread Pool implementation of bridge RestEndpointSubscriber")
	public Response testHttpThreadPoolPUT( String body,  @PathParam("serialNumber") String serialNumber)
	{
		String res = httpThreadToMongo( "PUT", serialNumber );
		logger.info( "testHttpThreadPool: " + res );

		return Response.status(200).header( "content-type", "text/plain" ).entity( res ).build();
	}

	@PATCH
	@Path("/test/HttpThreadPool/{serialNumber}")
	@ApiOperation(value = "a more generic test endpoint for the HTTP Thread Pool implementation of bridge RestEndpointSubscriber")
	public Response testHttpThreadPoolPATCH( String body,  @PathParam("serialNumber") String serialNumber)
	{
		String res = httpThreadToMongo( "PATCH", serialNumber );
		logger.info( "testHttpThreadPool: " + res );

		return Response.status(200).header( "content-type", "text/plain" ).entity( res ).build();
	}


}
