package com.tierconnect.riot.iot.controllers;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.script.ScriptException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.apache.shiro.authz.annotation.RequiresAuthentication;

import com.tierconnect.riot.iot.dao.mongo.TimeSeriesReportPOC;
import com.tierconnect.riot.iot.dao.mongo.TimeSeriesReportPOCDAO;
import com.tierconnect.riot.sdk.utils.TimerUtil;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

/**
 * @auhor tcrown
 */

@Path("/timeSeriesReportPOC")
@Api("/timeSeriesReportPOC")
public class TimeSeriesReportPOCController
{
	static Logger logger = Logger.getLogger( TimeSeriesReportPOCController.class );

	SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss.SSS z" );

	/**
	 * POC for thing time series (not for production !)
	 * @throws ScriptException
	 */
	@GET
	@Path("/")
	// @Produces(MediaType.TEXT_PLAIN) // commenting this line allows swagger to
	// work for some reason
	@RequiresAuthentication
	@ApiOperation(position = 1, value = "Terry's POC for mongo time series reports")
	public Response getReport(
			@ApiParam(value = "the Thing name (a java regex)", required = false) @QueryParam("name") String nameRegex,
			@ApiParam(value = "the Thing serialNumber (a java regex)", required = false) @QueryParam("serialNumber") String serialNumberRegex,
			@ApiParam(value = "csv list of groupIds", required = true) @QueryParam("groupIds") String groupIdsStr,
			@ApiParam(value = "csv list of thingTypeIds", required = true) @QueryParam("thingTypeIds") String thingTypeIdsStr,
			@ApiParam(value = "the start time (inclusive)", required = true) @QueryParam("t1") String t1str,
			@ApiParam(value = "the end time (inclusive)", required = true) @QueryParam("t2") String t2str,
			@ApiParam(value = "cvs list of UDF filters", required = false) @QueryParam("udfFilters") String udfFiltersStr,
			@ApiParam(value = "csv list of field names (native and UDFs)", required = true) @QueryParam("fieldNames") String fieldNamesStr,
			@ApiParam(value = "csv list of ordering", required = false) @QueryParam("orderBy") String orderbyStr,
			@ApiParam(value = "max number of rows to return (defaults to 10)", required = false) @QueryParam("maxRows") int maxrows )
			throws ScriptException
	{
		TimeSeriesReportPOCDAO ts = TimeSeriesReportPOCDAO.getInstance();

		// String nameRegex = ".*";
		// String serialNumberRegex = ".*";
		// int [] groupIds = new int[] { 2, 3 };
		// int [] thingTypeIds = new int[] { 1, 2, 3 };
		// String[] fieldNames = new String[] { "name", "serialNumber",
		// "groupId", "thingTypeId", "location", "zone" };
		// String[] orderBy = new String[] { "name:1", "serialNumber:1",
		// "groupId:1", "thingTypeId:1", "location:1", "zone:1" };

		int[] groupIds = ControllerUtils.getIntegerArray( groupIdsStr );
		int[] thingTypeIds = ControllerUtils.getIntegerArray( thingTypeIdsStr );
		long t1 = ControllerUtils.getTimestamp( t1str );
		long t2 = ControllerUtils.getTimestamp( t2str );

		String[] fieldNames = ControllerUtils.getStringArray( fieldNamesStr );
		String[] orderBy = ControllerUtils.getStringArray( orderbyStr );

		String[] udfFilters = ControllerUtils.getStringArray( udfFiltersStr );

		logger.info( "name='" + nameRegex + "'" );
		logger.info( "serialNumber='" + serialNumberRegex + "'" );
		logger.info( "groupIds=" + ControllerUtils.arrayToString( groupIds ) );
		logger.info( "thingTypeIds=" + ControllerUtils.arrayToString( thingTypeIds ) );

		logger.info( "t1='" + t1 + "' (" + sdf.format( new Date( t1 ) ) + ")" );
		logger.info( "t2='" + t2 + "' (" + sdf.format( new Date( t2 ) ) + ")" );

		logger.info( "fieldNames=" + ControllerUtils.arrayToString( fieldNames ) );

		logger.info( "orderBy=" + ControllerUtils.arrayToString( orderBy ) );

		logger.info( "maxrows=" + maxrows );

		TimerUtil tu = new TimerUtil();
		tu.mark();
		TimeSeriesReportPOC tsr = ts.buildReport( nameRegex, serialNumberRegex, groupIds, thingTypeIds, fieldNames, t1, t2, orderBy );
		tu.mark();
		tsr.makeNonSparse();
		tu.mark();
		tsr.setMaxRows( maxrows );
		tsr.orderBy( udfFilters, orderBy );
		tu.mark();
		String json = tsr.toJSON();
		tu.mark();

		logger.info( "delt_build=" + tu.getDelt( 0 ) );
		logger.info( "delt_make_dense=" + tu.getDelt( 1 ) );
		logger.info( "delt_sort=" + tu.getDelt( 2 ) );
		logger.info( "delt_toJSON=" + tu.getDelt( 3 ) );
		logger.info( "delt_total=" + tu.getTotalDelt() );
		logger.info( "mongo_doc_count=" + tsr.mongo_document_count );
		logger.info( "total_rows=" + tsr.getTotalRows() );

		return Response.status( 200 ).header( "content-type", "text/plain" ).entity( json ).build();
	}

	@GET
	@Path("/lastValueAtAnyDate")
	// @Produces(MediaType.TEXT_PLAIN) // commenting this line allows swagger to
	// work for some reason
	@RequiresAuthentication
	@ApiOperation(position = 2, value = "Get all things with the udf's values at any date in the past, this reports get the data from the timeseries ")
	public Response getReport( @ApiParam(value = "csv list of groupIds", required = true) @QueryParam("groupIds") String groupIdsStr,
			@ApiParam(value = "csv list of thingTypeIds", required = true) @QueryParam("thingTypeIds") String thingTypeIdsStr,
			@ApiParam(value = "cvs list of UDF filters", required = true) @QueryParam("udfFilters") String udfFiltersStr,
			@ApiParam(value = "the date ", required = true) @QueryParam("date") String dateStr,
			@ApiParam(value = "output collection", required = true) @QueryParam("output") String output )
	{
		TimeSeriesReportPOCDAO ts = TimeSeriesReportPOCDAO.getInstance();

		List<Long> arrayGroupId = new ArrayList<>();
		List<Long> arrayThingTypeId = new ArrayList<>();
		List<String> arrayFieldnames = new ArrayList<>();

		arrayGroupId = ControllerUtils.getLongArrayList( groupIdsStr );
		arrayThingTypeId = ControllerUtils.getLongArrayList( thingTypeIdsStr );
		arrayFieldnames = ControllerUtils.getStringArrayList( udfFiltersStr );

		Long reportDate = ControllerUtils.getISODate( dateStr );

		if( reportDate == null )
		{
			return Response.status( 500 ).header( "content-type", "text/plain" ).entity( "Error in Date" ).build();
		}

		String res = ts.lastValueAtAnyDate( arrayGroupId, arrayThingTypeId, arrayFieldnames, reportDate, output );

		return Response.status( 200 ).header( "content-type", "text/plain" ).entity( res ).build();

	}

}
