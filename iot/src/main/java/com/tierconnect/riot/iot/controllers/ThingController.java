/**
 * @author grea
 */
package com.tierconnect.riot.iot.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.controllers.GroupController;
import com.tierconnect.riot.appcore.controllers.GroupTypeController;
import com.tierconnect.riot.appcore.controllers.RiotShiroRealm;
import com.tierconnect.riot.appcore.controllers.UserController;
import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.services.*;
import com.tierconnect.riot.appcore.utils.QueryUtils;
import com.tierconnect.riot.appcore.utils.Utilities;
import com.tierconnect.riot.appcore.utils.VisibilityUtils;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.commons.DateFormatAndTimeZone;
import com.tierconnect.riot.commons.utils.DateHelper;
import com.tierconnect.riot.iot.dao.SequenceDAO;
import com.tierconnect.riot.iot.dao.ThingDAO;
import com.tierconnect.riot.iot.dao.mongo.ThingMongoDAO;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.fmc.utils.FMCUtils;
import com.tierconnect.riot.iot.reports.ReportAppService;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.iot.utils.ReportUtils;
import com.tierconnect.riot.iot.utils.VisibilityThingUtils;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import com.tierconnect.riot.sdk.dao.Pagination;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.servlet.exception.ForbiddenException;
import com.tierconnect.riot.sdk.utils.PermissionsUtils;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.wordnik.swagger.annotations.*;
import com.wordnik.swagger.jaxrs.PATCH;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.subject.Subject;
import org.geojson.Feature;
import org.geojson.LineString;
import org.geojson.LngLatAlt;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.tierconnect.riot.iot.utils.VisibilityThingUtils.*;

//import com.tierconnect.riot.iot.fixdb.FixDBMigrationCassandra;

@Path("/thing")
@Api("/thing")
public class ThingController extends ThingControllerBase
{
	static Logger logger = Logger.getLogger( ThingController.class );

	@Context
	ServletContext context;

	private String[] IGNORE_THING_FIELDS = new String[] { "fields", "children", "childrenMap", "selected", "x", "y" };


	/***************************************
	* @method createThing
	* @description This method creates an instance of thing and associations with parent and children
	* @params: Format Json:
	*************************************************/
	@PUT
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@RequiresAuthentication
	@RequiresPermissions(value={"thingType:r"})
	@ApiOperation(value = "Create Thing",
				  position = 4,
			      notes = "This method creates an instance of thing and associations with parent and children.<br>"
						  + "<font face=\"courier\">{ <br>&nbsp;&nbsp;\"group\": \">GCODE1>GCODE2\", \n"
						  + "<br>&nbsp;&nbsp;\"thingTypeCode\": \"default_rfid_thingtype\",\n"
						  + "<br>&nbsp;&nbsp;\"name\": \"TP10049\",\n" + "  <br>&nbsp;&nbsp;\"serialNumber\": \"TP10049\",  \n"
						  + "<br>&nbsp;&nbsp;\"parent\": {\n" + "    <br>&nbsp;&nbsp;&nbsp;\"serialNumber\": \"J00001\",\n"
						  + "<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\"time\":\"123456789\",\n"
						  + "<br>&nbsp;&nbsp;&nbsp;\"thingTypeCode\": \"jackets_code\"\n" + "  <br>&nbsp;&nbsp;},\n"
						  + "<br>&nbsp;&nbsp;\"udfs\": {\n" + "    <br>&nbsp;&nbsp;&nbsp;\"name\": {\n"
						  + "<br>&nbsp;&nbsp;&nbsp;\"value\": \"General Motors\"\n"
						  + "<br>&nbsp;&nbsp;&nbsp;},\n"
						  + "<br>&nbsp;&nbsp;&nbsp;\"Phone\": {\n" + "      <br>&nbsp;&nbsp;&nbsp;\"value\":\"7789564\"\n"
						  + "<br>&nbsp;&nbsp;&nbsp;}     \n"
						  + "<br>&nbsp;&nbsp;},\n" + "  <br>&nbsp;&nbsp;\"children\": [\n" + "  <br>&nbsp;&nbsp;  {\n"
						  + "<br>&nbsp;&nbsp;&nbsp;\"serialNumber\": \"000000000000000000474\",\n"
						  + "<br>&nbsp;&nbsp;&nbsp;\"thingTypeCode\": \"default_rfid_thingtype\",\n"
						  + "<br>&nbsp;&nbsp;&nbsp;\"udfs\": \n" + "      <br>&nbsp;&nbsp;&nbsp;{\n"
						  + "<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\"name\": {\n"
						  + "<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\"value\": \"GENERAL\"\n"
						  + "<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;}\n" + "      <br>&nbsp;&nbsp;&nbsp;}&nbsp;&nbsp;}&nbsp;&nbsp;]&nbsp;&nbsp;}</font>")
	@ApiResponses(
		value = {
			@ApiResponse(code = 200, message = "Ok"),
			@ApiResponse(code = 201, message = "Created"),
			@ApiResponse(code = 400, message = "Bad Request"),
			@ApiResponse(code = 403, message = "Forbidden"),
			@ApiResponse(code = 500, message = "Internal Server Error")
		}
	)
	public Response create(
			  @ApiParam(value = "\"group\" and \"time\" are optional fields in the JSON Request.")  Map<String, Object> thingMap
			, @QueryParam("useDefaultValue") Boolean useDefaultValue, @QueryParam("createRecent") @DefaultValue("false") Boolean createRecent)
	{
		Map<String, Object> result = null;
		try{
			Date storageDate = ThingService.getInstance().getDate( thingMap.get("time")!=null?(String)thingMap.get("time"): null);
			//Create a new Thing
            Stack<Long> recursivelyStack = new Stack<>();
			result = ThingsService.getInstance().create(
                    recursivelyStack
                    , (String) thingMap.get("thingTypeCode")
					, (String) thingMap.get("group")
					, (String) thingMap.get("name")
					, (String) thingMap.get("serialNumber")
					, (Map<String, Object>) thingMap.get("parent")
					, (Map<String, Object>) thingMap.get("udfs")
					, thingMap.get("children")
					, thingMap.get("childrenUdf")
					, true, true, storageDate
                    , useDefaultValue
			        , true);
			if (createRecent){
				RecentService.getInstance().recentThings(result);
			}

		}catch(UserException e){
			return RestUtils.sendResponseWithCode(e.getMessage() , 400);
		}
		return RestUtils.sendOkResponse(result);
	}

	/***************************************
	 * @method updateThing
	 * @description This method updates an instance of thing and associations with parent and children
	 * @params: Format Json:
	 *************************************************/
	@Override
	@PATCH
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@RequiresAuthentication
	@RequiresPermissions(value = {"thingType:r:{id}"})
	@ApiOperation(value = "Update Thing",
			position = 5,
			notes = "This method creates an instance of thing and associations with parent and children.<br>"
					+ "<font face=\"courier\">{ <br>&nbsp;&nbsp;\"group\": \">GCODE1>GCODE2\", \n"
					+ "<br>&nbsp;&nbsp;\"thingTypeCode\": \"default_rfid_thingtype\",\n"
					+ "<br>&nbsp;&nbsp;\"name\": \"TP10049\",\n" + "  <br>&nbsp;&nbsp;\"serialNumber\": \"TP10049\", "
					+ "<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\"time\":\"123456789\",\n"
					+ " \n"
					+ "<br>&nbsp;&nbsp;\"parent\": {\n" + "    <br>&nbsp;&nbsp;&nbsp;\"serialNumber\": \"J00001\",\n"
					+ "<br>&nbsp;&nbsp;&nbsp;\"thingTypeCode\": \"jackets_code\"\n" + "  <br>&nbsp;&nbsp;},\n"
					+ "<br>&nbsp;&nbsp;\"udfs\": {\n" + "    <br>&nbsp;&nbsp;&nbsp;\"name\": {\n"
					+ "<br>&nbsp;&nbsp;&nbsp;\"value\": \"General Motors\"\n"
					+ "<br>&nbsp;&nbsp;&nbsp;},\n"
					+ "<br>&nbsp;&nbsp;&nbsp;\"Phone\": {\n" + "      <br>&nbsp;&nbsp;&nbsp;\"value\":\"7789564\"\n"
					+ "<br>&nbsp;&nbsp;&nbsp;}     \n"
					+ "<br>&nbsp;&nbsp;},\n" + "  <br>&nbsp;&nbsp;\"children\": [\n" + "  <br>&nbsp;&nbsp;  {\n"
					+ "<br>&nbsp;&nbsp;&nbsp;\"serialNumber\": \"000000000000000000474\",\n"
					+ "<br>&nbsp;&nbsp;&nbsp;\"thingTypeCode\": \"default_rfid_thingtype\",\n"
					+ "<br>&nbsp;&nbsp;&nbsp;\"udfs\": \n" + "      <br>&nbsp;&nbsp;&nbsp;{\n"
					+ "<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\"name\": {\n"
					+ "<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\"value\": \"GENERAL\"\n"
					+ "<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;}\n" + "<br>&nbsp;&nbsp;&nbsp;}&nbsp;&nbsp;}&nbsp;&nbsp;" +
					"]&nbsp;&nbsp;}</font>")
	@ApiResponses(
			value = {
					@ApiResponse(code = 400, message = "Bad Request"),
					@ApiResponse(code = 200, message = "Ok"),
					@ApiResponse(code = 403, message = "Forbidden"),
					@ApiResponse(code = 500, message = "Internal Server Error")
			}
	)
	public Response updateThing(@ApiParam(value = "Id of the Thing.") @PathParam("id") Long thingId,
								@ApiParam(value = "\"group\" and \"time\" is an optional field in the JSON Request."
										+
										"<br>children=null: Do not modify any children."
										+
										"<br>children=[]: Unassign all children of the thing."
										+
										"<br>children=[{...}]: Assign these new children and replace the old ones in" +
										" " +
										"the thing.")
								Map<String, Object> thingMap) {
		Map<String, Object> result;
		try {

			ThingService thingService = ThingService.getInstance();
			Date storageDate = Utilities.getDate(thingMap.get("time"));
			Subject subject = SecurityUtils.getSubject();
			User currentUser = (User) subject.getPrincipal();
			//Update a new Thing
			Stack<Long> recursivelyStack = new Stack<>();

			result = thingService.update(
					recursivelyStack
					, thingId
					, (String) thingMap.get("thingTypeCode")
					, (String) thingMap.get("group")
					, (String) thingMap.get("name")
					, (String) thingMap.get("serialNumber")
					, (Map<String, Object>) thingMap.get("parent")
					, (Map<String, Object>) thingMap.get("udfs")
					, thingMap.get("children")
					, thingMap.get("childrenUdf")
					, true, true, storageDate, false, currentUser, true);

		} catch (UserException e) {
			return RestUtils.sendResponseWithCode(e.getMessage(), 400);
		}
		return RestUtils.sendOkResponse(result);
	}

	/*************************************************
	 * @method deleteThing
	 * @description This method updates an instance of thing and associations with parent and children
	 * @params: Format Json:
	 *************************************************/
	@Override @DELETE
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@RequiresAuthentication
	@RequiresPermissions(value={"thingType:r"})
	@ApiOperation(position = 6, value = "Delete a Thing",notes="Delete a specified Thing")
	@ApiResponses(
		value = {
			@ApiResponse(code = 204, message = "No Content"),
			@ApiResponse(code = 400, message = "Bad Request"),
			@ApiResponse(code = 403, message = "Forbidden"),
			@ApiResponse(code = 500, message = "Internal Server Error")
		}
	)
	public Response deleteThing( @ApiParam(value = "Unique Thing ID.") @PathParam("id") Long id )
	{
		try{
			RecentService.getInstance().deleteRecent(id, "thing");
			ThingService.getInstance().secureDelete(id, true, true, true, true, false);
			return RestUtils.sendDeleteResponse();
		}catch (Exception e){
			return RestUtils.sendBadResponse(e.getMessage());
		}
	}

	/*************************************************
	 * @method deleteThing
	 * @description This method updates an instance of thing and associations with parent and children
	 * @params: Format Json:
	 *************************************************/
	@DELETE
	@Path("/batchDelete")
	@Produces(MediaType.APPLICATION_JSON)
	@RequiresAuthentication
	@RequiresPermissions(value={"thingType:r"})
	@ApiOperation(position=7, value = "Updates Many Thing relationships in only one batch operation",
			notes = "It receives an array of maps. Each map has to have an 'id' of the thing that you want to delete.<br>"
					+ "<font face=\"courier\">[\n"
					+ "<br>&nbsp;&nbsp;{\"id\": 1},\n"
					+ "<br>&nbsp;&nbsp;{\"id\": 2} \n"
					+ "<br>]")
	public Response batchDelete(List<Map> elements)
	{
		try{
			boolean massiveProcesss = ReportAppService.instance().isMassiveProcessRunning();
			if (massiveProcesss) {
				return RestUtils.sendResponseWithCode("Report has bulk process in progress", 400);
			}
			Map result = new HashMap();
			List<Map> results;
			if (elements != null) {
				List<Long> thingIds = new ArrayList<>();
				for(Map element : elements){
					Object id = element.get("id");
					if (id == null
							|| (StringUtils.isEmpty(id.toString()) && !StringUtils.isNumeric(id.toString()))) {
						throw new RuntimeException("id not found");
					}
					thingIds.add(Long.parseLong(id.toString()));
				}
				results = ThingService.getInstance().secureDelete(thingIds, true, true, true, true, false);
				result.put("results", results);
				return RestUtils.sendOkResponse(result);
			} else {
				return RestUtils.sendBadResponse("No elements has been sent to delete");
			}
		}catch (Exception e){
		    logger.error(e);
			return RestUtils.sendBadResponse(e.getMessage());
		}
	}


	@GET
	@Override
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	// 1a. Limit access based on CLASS level resources
	@RequiresAuthentication
	@RequiresPermissions(value = { "thing:r" })
	@ApiOperation(position = 3, value = "Get a Thing",notes="Returns the information of a specified Thing")
    @ApiResponses(
		value = {
			@ApiResponse(code = 200, message = "Ok"),
			@ApiResponse(code = 400, message = "Bad Request"),
			@ApiResponse(code = 403, message = "Forbidden"),
			@ApiResponse(code = 500, message = "Internal Server Error")
		}
    )
	public Response selectThings( @ApiParam(value = "Unique Thing ID.") @PathParam("id") Long id,
                                  @ApiParam(value = "Add extra fields to the response. i.e group, group.parent.") @Deprecated @QueryParam("extra") String extra,
                                  @ApiParam(value = "Only the listed fields will be included in the response.i.e. id,name,code") @Deprecated @QueryParam("only") String only,
								  @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend,
								  @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project,
                                  @QueryParam("createRecent") @DefaultValue("false") Boolean createRecent)
	{
		logger.info( "entering select thing" );
		Thing thing = ThingService.getInstance().get( id );
		if( thing == null )
		{
			return RestUtils.sendBadResponse( String.format( "ThingId[%d] not found", id ) );
		}
		// 2. Limit visibility based on user's group and the object's group
		// (group based authorization)
		Group visibilityGroup = getVisibilityGroup( thing );

		VisibilityThingUtils.limitVisibilitySelectT(thing, visibilityGroup, SecurityUtils.getSubject());

		if( !PermissionsUtils.isPermitted( SecurityUtils.getSubject(), Resource.THING_TYPE_PREFIX + thing.getThingType().getId() + ":r:"
				+ thing.getId() ) )
		{
			throw new ForbiddenException("Not Authorized, Access Denied");
		}

		validateSelect( thing );
		if (thing.getParent() == null){
			if (extend != null && (extend.contains("parent.thingType") || extend.contains("parent.thingType,"))){
				extend = extend.replace("parent.thingType,"," ");
				extend = extend.replace("parent.thingType"," ");
			}
		}
		// 5a. Implement extra
		Map<String, Object> publicMap = QueryUtils.mapWithExtraFields( thing, extra, getExtraPropertyNames() );
		publicMap = QueryUtils.mapWithExtraFieldsNested( thing, publicMap, extend, getExtraPropertyNames() );
		addToPublicMap( thing, publicMap, extra );


		User currentUser = (User) SecurityUtils.getSubject().getPrincipal();
		DateFormatAndTimeZone dateFormatAndTimeZone = UserService.getInstance().getDateFormatAndTimeZone(currentUser);
		logger.info("USER [" + currentUser.getUsername() + "] REGIONAL SETTING " + dateFormatAndTimeZone);
		// add values for thingFields
		//TODO IMPORTANT used to get values from mongo, ask Cristian Vertiz to refactor
		List<Map<String, Object>> fields = getThingFieldMap(thing, dateFormatAndTimeZone);
		if(publicMap.get("thingType")!=null)
		{
			addThingTypeFieldData(ThingTypeService.getInstance()
					.get( (Long) ((Map) publicMap.get( "thingType" )).get( "id" ) ), fields);
		}
		publicMap.put( "fields", fields );

		// 5b. Implement only

		QueryUtils.filterOnly( publicMap, only, extra );
		QueryUtils.filterProjectionNested( publicMap, project, extend);

		//Get the data of the father
		if(thing.getParent()!=null)
		{
			Map<String, Object> parentDataMap = thing.getParent().publicMapExtraValues();
			List<Map<String, Object>> chldrenFieldMap = getThingFieldMap(thing.getParent(), dateFormatAndTimeZone);
			addThingTypeFieldData( thing.getParent().getThingType(), chldrenFieldMap );
			parentDataMap.put( "fields" ,chldrenFieldMap );
			publicMap.put( "parentFields",parentDataMap);
		}else
		{
			publicMap.put( "parentFields",new ArrayList<HashMap<String, Object>>());
		}

		List<Map<String, Object>> childrenMap = new ArrayList<>();
		List<Thing> children = ThingService.getInstance().selectByParent( thing );
		if(children !=null && children.size()>0 )
		{
			for(Thing thingChild : children)
			{
				Map<String, Object> childrenDataMap = thingChild.publicMapExtraValues();
				List<Map<String, Object>> chldrenFieldMap = getThingFieldMap(thingChild, dateFormatAndTimeZone);
				addThingTypeFieldData( thingChild.getThingType(), chldrenFieldMap );
				childrenDataMap.put( "fields" ,chldrenFieldMap );
				childrenMap.add( childrenDataMap );
			}
			publicMap.put( "childrenFields",childrenMap);
		}else
		{
			publicMap.put( "childrenFields",new ArrayList<HashMap<String, Object>>());
		}

		//Set childrenUdfMap
		List<Map<String, Object>> childrenUdfMap = new ArrayList<>();
		List<ThingType> childrenUdf = ThingTypeService.getInstance().getChildrenUdfThingTypeCode(thing.getThingType().getId());
		if(childrenUdf !=null && childrenUdf.size()>0 )
		{
			for(ThingType thingChild : childrenUdf)
			{
				Map<String, Object> childrenDataMap = thingChild.publicMap( true, false );
				childrenUdfMap.add( childrenDataMap );
			}
			publicMap.put( "childrenUdfs",childrenUdfMap);
		}else
		{
			publicMap.put( "childrenUdfs",new ArrayList<HashMap<String, Object>>());
		}
		Map<String, Object> thingMap = new HashMap<>();
		Map<String, Object> thingData = thing.publicMap();
		thingData.put("groupId", thing.getGroup().getId());
		thingMap.put("thing",thingData);
		if (createRecent){
			RecentService.getInstance().recentThings(thingMap);
		}

		return RestUtils.sendOkResponse( publicMap );
	}

	/*************************
	 * Method for adding data of ThingTypeField
	 *************************/
	private void addThingTypeFieldData(ThingType thingType , List<Map<String, Object>> fields)
	{
		Map<String , Object> thingTypeMap = new HashMap<String , Object>();
		if(thingTypeMap!=null )
		{
			ThingTypeController thingTypeController  =  new ThingTypeController();
			thingTypeController.addToPublicMap(thingType,thingTypeMap, null);

			for(Map<String, Object> field : fields)
			{
				for( Map<String, Object> map:  (List<Map<String, Object>>) thingTypeMap.get("fields"))
				{
					if(Long.parseLong(map.get("id").toString()) == Long.parseLong(field.get("id").toString()))
					{
						field.put("thingTypeField", map);
						break;
					}
				}
			}
		}
	}

	/**
	 * TODO IMPORTANT used to get values from mongo, ask Cristian Vertiz to refactor
	 *
	 * @param thing
	 * @return
	 */
	private List<Map<String, Object>> getThingFieldMap(Thing thing, DateFormatAndTimeZone formatAndTimeZone) {
		List<Map<String, Object>> fields = new LinkedList<>();

		String where = "_id=" + thing.getId();

		List<String> filterFields = new ArrayList<>();

		filterFields.add("*");

        Map<String,Object> fieldValues = ThingMongoDAO.getInstance().getThingUdfValues(where, null, filterFields, null );
		for (ThingTypeField thingTypeField : thing.getThingType().getThingTypeFields()) {
            Map<String, Object> m = thingTypeField.publicMap();
			m.put( "thingId", thing.getId() );
            Long fieldId = thingTypeField.getId();
            String fieldName = thingTypeField.getName();
            if( fieldName != null  &&
					((int)fieldValues.get("total")) > 0 &&
					((List<Map<String,Object>>)fieldValues.get("results")).get(0).containsKey(fieldName)){

				List<Map<String, Object>> list = (List)fieldValues.get("results");
				Map<String, Object> map = list.get(0);
				Object value = map.get(fieldName);

				m.put("value", value);
				if (m.get("type") != null) {
					if (ThingTypeFieldService.isDateTimeStampType((Long) m.get("type"))) {
						if (value instanceof Date) {
							m.put("value", formatAndTimeZone.getISODateTimeFormatWithoutTimeZone((Date) value));
						} else if (value instanceof Long) {
							m.put("value", formatAndTimeZone.getISODateTimeFormatWithoutTimeZone((Long) value));
						}
					}
					// Changing timestamp to Date for expressions values
					if (m.get("type").equals(ThingTypeField.Type.TYPE_FORMULA.value) &&
							((value instanceof Date) || ((value instanceof Long) && (value.toString().length() == 13)))) {
						m.put("type", 11);
					}
				}
//				Map<String, Object> variableStructure = fieldValues.get(fieldId);
//                String value = variableStructure != null ? (String) variableStructure.get("value") : null;
//                m.put( "value", value );
            }
            fields.add( m );
        }
        return fields;
    }

	/**
	 * Get thing field map so as to get the value of the thing field with codes
	 **/
	private List<Map<String, Object>> getThingFieldCodeMap(Thing thing) {
		List<Map<String, Object>> fields = new LinkedList<>();

		String where = "_id=" + thing.getId();

		List<String> filterFields = new ArrayList<>();

		filterFields.add("*");

		Map<String,Object> fieldValues = ThingMongoDAO.getInstance().getThingUdfValues(where, null, filterFields, null );
		for (ThingTypeField thingTypeField : thing.getThingType().getThingTypeFields()) {
			Map<String, Object> m = thingTypeField.publicMap();
			Long fieldId = thingTypeField.getId();
			String fieldName = thingTypeField.getName();
			if( fieldName != null  &&
					((int)fieldValues.get("total")) > 0 &&
					((List<Map<String,Object>>)fieldValues.get("results")).get(0).containsKey(fieldName)){

				List<Map<String, Object>> list = (List)fieldValues.get("results");
				Map<String, Object> map = list.get(0);
				Object value = map.get(fieldName);

				/*****/
				DataType dataType = null;
				if(thingTypeField.getDataType().getTypeParent().equals( ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value ))
				{
					dataType =  thingTypeField.getDataType();
				}
				if( thingTypeField.getDataType().getTypeParent().equals( ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value ) && dataType!=null
						&& dataType.getType().equals( ThingTypeField.TypeParentSubGroup.TYPE_PARENT_DATA_TYPE_STANDARD_DATA.value ))
				{
					m.put( "value", value );
				}else if( thingTypeField.getDataType().getTypeParent().equals(ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value )  && dataType!=null
						&& dataType.getType().equals( ThingTypeField.TypeParentSubGroup.TYPE_PARENT_DATA_TYPE_NATIVE_OBJECT.value ))
				{
					Map<String, Object> dataValue = (Map<String, Object>) value;
					m.put( "value", dataValue.get( "code" ) );
				}else if( thingTypeField.getDataType().getTypeParent().equals( ThingTypeField.TypeParent.TYPE_PARENT_NATIVE_THING_TYPE.value ))
				{
					Map<String, Object> dataValue = (Map<String, Object>) value;
					m.put( "value", dataValue.get( "serial" ) );
				}

				/*****/
				//m.put( "value", value );
			}
			fields.add( m );
		}
		return fields;
	}

	@GET
	@Path("/tree")
	@Produces(MediaType.APPLICATION_JSON)
	@RequiresAuthentication
	@RequiresPermissions(value = { "thing:r" })
    @ApiOperation(position = 2, value = "Get a List of Things in Tree", notes="Get a List of Things in Tree")
    @ApiResponses(
		value = {
			@ApiResponse(code = 200, message = "Ok"),
			@ApiResponse(code = 400, message = "Bad Request"),
			@ApiResponse(code = 403, message = "Forbidden"),
			@ApiResponse(code = 500, message = "Internal Server Error")
		}
    )
	// TODO implement topId
	public Response listThingsInTree( @ApiParam(value = "The number of things per page (default 10).") @QueryParam("pageSize") Integer pageSize,
									  @ApiParam(value = "The page number you want to be returned (the first one is displayed by default).") @QueryParam("pageNumber") Integer pageNumber,
									  @ApiParam(value = "The field to be used to sort the thing results. This can be asc or desc. i.e. name:asc") @QueryParam("order") String order,
									  @ApiParam(value = "A filtering parameter to get specific things. Supported operators: Equals (=), like (~), and (&), or (|) ") @QueryParam("where") String where,
									  @ApiParam(value = "Add extra fields to the response. i.e parent, group, thingType, group.groupType, group.parent") @Deprecated @QueryParam("extra") String extra,
									  @ApiParam(value = "The listed fields will be included in the response. i.e.  only= id,name,code") @Deprecated @QueryParam("only") String only,
									  @ApiParam(value = "It is used to overridden default visibilityGroup to a lower group.") @QueryParam("visibilityGroupId") Long visibilityGroupId,
									  @ApiParam(value = "It is used to disable upVisibility. It can have True or False values") @DefaultValue("") @QueryParam("upVisibility") String upVisibility,
									  @ApiParam(value = "It is used to disable downVisibility. It can have True or False values") @DefaultValue("") @QueryParam("downVisibility") String downVisibility,
									  @ApiParam(value = "If UpVisibility is true, it shows the top level of a group.") @QueryParam("topId") String topId,
									  @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
	{
		Pagination pagination = new Pagination( pageNumber, pageSize );

		BooleanBuilder masterVisibilityOr = VisibilityThingUtils.limitSelectAllT(upVisibility, downVisibility, visibilityGroupId);
		if (masterVisibilityOr == null) {
			Map<String, Object> mapResponse = new HashMap<>();
			mapResponse.put( "total", 1 );
			mapResponse.put( "results", new ArrayList<>() );
			return RestUtils.sendOkResponse( mapResponse );
		}
		BooleanBuilder masterBe = new BooleanBuilder( masterVisibilityOr );

		// 4. Implement filtering
		masterBe = masterBe.and( QueryUtils.buildSearch( QThing.thing, where));

		Long count = 0L;
		List<Map<String, Object>> list = new LinkedList<>();
		Map<Long, Map<String, Object>> objectCache = new HashMap<>();
		Map<Long, Set<Long>> childrenMapCache = new HashMap<>();
		Map<String, Boolean> permissionCache = new HashMap<>();

		count = ThingService.getInstance().countList( masterBe );
		List<Thing> thingList = ThingService.getInstance().listPaginated( masterBe, pagination, order );
		for( Thing thing : thingList )
		{
			mapThing( thing, objectCache, childrenMapCache, permissionCache, list, StringUtils.isNotEmpty(where), only, extra );
			addAllDescendants( thing, objectCache, childrenMapCache, permissionCache, list, false, only, extra );
		}

		// 3. Implement pagination
		Map<String, Object> mapResponse = new HashMap<>();
		mapResponse.put( "total", count );
		mapResponse.put( "results", list );
		return RestUtils.sendOkResponse( mapResponse );
	}

	public void mapThing( Thing object, Map<Long, Map<String, Object>> objectCache, Map<Long, Set<Long>> childrenMapCache,
						  Map<String, Boolean> permissionCache, List<Map<String, Object>> list, boolean selected, String only, String extra )
	{
		Integer level;
		Subject subject = SecurityUtils.getSubject();
		if( !PermissionsUtils.isPermittedAny( subject, Resource.THING_TYPE_PREFIX + object.getThingType().getId() + ":r" ) )
		{
			return;
		}
		// Map Thing
		Map<String, Object> objectMap = objectCache.get( object.getId() );
		if( objectMap == null )
		{
			objectMap = QueryUtils.mapWithExtraFields( object, extra, getExtraPropertyNames() );
			addToPublicMap( object, objectMap, extra );
			QueryUtils.filterOnly( objectMap, only, extra );
			QueryUtils.filterProjectionNested( objectMap, null, null);
			if( object.getParent() == null )
			{
				list.add( objectMap );
			}
			objectMap.put( "children", new ArrayList<>() );
			objectCache.put( object.getId(), objectMap );
		}
		if( selected )
		{
			objectMap.put( "selected", Boolean.TRUE );
		}
		// Map parent
		Thing parent = object.getParent();
		Map<String, Object> parentObjectMap = null;
		if( parent != null )
		{
			if( !PermissionsUtils.isPermittedAny( subject, Resource.THING_TYPE_PREFIX + parent.getThingType().getId() + ":r" ) )
			{
				list.add( objectMap );
				return;
			}
			parentObjectMap = objectCache.get( parent.getId() );
			if( parentObjectMap == null )
			{
				mapThing( parent, objectCache, childrenMapCache, permissionCache, list, false, only, extra );
				parentObjectMap = objectCache.get( parent.getId() );
			}
			level = (Integer) parentObjectMap.get( "treeLevel" ) + 1;
		}
		else
		{
			level = 1;
		}
		objectMap.put( "treeLevel", level );

		// Add child to parent
		if( parentObjectMap != null )
		{
			Set childrenSet = childrenMapCache.get( parent.getId() );
			if( childrenSet == null )
			{
				childrenSet = new HashSet();
				childrenMapCache.put( parent.getId(), childrenSet );
			}
			List childrenList = (List) parentObjectMap.get( "children" );
			if( !childrenSet.contains( object.getId() ) )
			{
				childrenSet.add( object.getId() );
				childrenList.add( objectMap );
			}
		}
	}

	public void addAllDescendants( Thing base, Map<Long, Map<String, Object>> objectCache, Map<Long, Set<Long>> childrenMapCache,
								   Map<String, Boolean> permissionCache, List<Map<String, Object>> list, boolean selected, String only, String extra )
	{
		List<Thing> children = ThingService.getThingDAO().selectAllBy(QThing.thing.parent.eq(base));
		for( Thing child : children )
		{
			if( !child.getId().equals( base.getId() ) )
			{
				mapThing( child, objectCache, childrenMapCache, permissionCache, list, selected, only, extra );
				addAllDescendants( child, objectCache, childrenMapCache, permissionCache, list, selected, only, extra );
			}
		}
	}


	/*
	@Deprecated
	public Map<String, Object> createThingStructure(Map<String, Object> thingMap ) throws UserException{
		Subject subject = SecurityUtils.getSubject();
		User currentUser = (User) subject.getPrincipal();

		String serial = (String) thingMap.get("serial");
		//serial = serial.trim();

		if(!StringUtils.isAlphanumeric(serial)){
			throw new UserException( "Invalid serial \"" + serial + "\""  );
		}

		Thing parent = null;
		ThingService thingService = ThingService.getInstance();
		if( thingMap.get( "parent.id" ) != null )
		{
			Long parentId = ((Integer) thingMap.get( "parent.id" )).longValue();
			parent = thingService.get(parentId);
			if( parent == null )
			{
				throw new UserException( String.format( "Parent[%d] does not exist", parentId ) );
			}
		}

		if( thingMap.get( "group.id" ) == null || (thingMap.get( "group.id" ) instanceof Integer) == false )
		{
			throw new UserException( "Invalid Group" );
		}
		Group group = GroupService.getInstance().get( ((Number) thingMap.get( "group.id" )).longValue() );
		if( group == null )
		{
			throw new UserException( "Invalid Group" );
		}

		ArrayList<Object> childrenList = (ArrayList) thingMap.get( "childrenList" );
		thingMap.remove("childrenList");

		ThingType thingType = null;
		if( thingMap.get( "thingType.id" ) != null || (thingMap.get( "thingType.id" ) instanceof Integer) == true )
		{
			thingType = ThingTypeService.getInstance().get( ((Number) thingMap.get( "thingType.id" )).longValue() );
		}

		limitVisibilityInsertT(thingType, VisibilityUtils.getObjectGroup(thingMap));

		Thing thing = new Thing();
		thing.setCreatedByUser(currentUser);
		BeanUtils.setProperties(thingMap, thing);
		Date storeDate = new Date();
		if( thingMap.containsKey( "parent.id" ) )
		{
			thing.setParent(parent);
			if (parent != null) {
				//TODO Review if FMC must be called
				FMCUtils.fmcHandleAssignTag(parent, thing, thing.getGroup(), storeDate, currentUser);
			}
		}
		thing.setGroup(group);
		thing.setThingType(thingType);

		if( thingMap.containsKey( "groupTypeFloor.id" ) )
		{
			Number number = (Number) thingMap.get( "groupTypeFloor.id" );
			GroupType groupType = null;
			if( number != null )
			{
				groupType = GroupTypeService.getInstance().get( number.longValue() );
			}
			thing.setGroupTypeFloor( groupType );
		}

		thing.setSerial(thing.getSerial().trim());
		validateInsert(thing, thing.getSerial());

		thing = thingService.insert(thing, storeDate);

		// adding/modifying value for sequence type UDFs
		addSequenceValues((ArrayList) thingMap.get("values"), thingType);

		Map<String, Object> thingData = new HashMap<>();
		thingData.put("values", thingMap.get("values"));

		Map thingResponse =  updateThingValues(thing.getId(), thingData);

		List<Map> childrenThingResponse = new ArrayList<>();
		if( childrenList != null )
		{
            List<Long> newChildrenIds = new ArrayList<>();
			for( Object childThingMap : childrenList )
			{
				Long childThingId = Long.valueOf(((Map) childThingMap).get("thingId").toString());
                newChildrenIds.add(childThingId);
				Thing childThing = thingService.get(childThingId);
				if( ThingService.getFirstChild( childThing ) != null )
				{
					throw new ForbiddenException( "The thing [ " + childThing.getId() + " ] is a parent." );
				}

                //save id to dis-associate child from parent
                Thing oldParent = childThing.getParent();
                if (oldParent != null) {
                    ThingMongoDAO.getInstance().disAssociateChild(oldParent, childThingId);
                }

				childThing.setParent(thing);
				thingService.update(childThing);

				Map<String, Object> childThingData = new HashMap<>();
				childThingData.put("values", ((Map) childThingMap).get("values"));
				Map childResponse = updateThingValues(childThingId, childThingData);
				childrenThingResponse.add(childResponse);
				//TODO Review if FMC must be called
				FMCUtils.fmcHandleAssignTag(thing, childThing, thing.getGroup(), storeDate, currentUser);
			}

            ThingMongoDAO.getInstance().associateChildren(thing, newChildrenIds);
		}

        //TODO repeated code. this should be above but thing is not created yet.
        if( thingMap.containsKey( "parent.id" ) ) {
            if (parent != null) {
                ThingMongoDAO.getInstance().associateChild(parent, thing.getId());
            }
        }

		//BrokerClientHelper.sendRefreshThingMessage();
		BrokerClientHelper.sendRefreshSingleThingMessage(thing.getThingType().getThingTypeCode(), thing.getSerial());

		Map<String, Object> object = thing.publicMap();

		object.put("thingValues", thingResponse);
		object.put("childrenValues", childrenThingResponse);

		object.put("timestamp", storeDate.getTime());
		return object;
	}
	*/

	private void addSequenceValues(List<Map<String, Object>> values, ThingType thingType){
		List<ThingTypeField> thingTypeFields = thingType.getThingTypeFieldsByType(Long.valueOf(ThingTypeField.Type.TYPE_SEQUENCE.value));
		if ( null != thingTypeFields && !thingTypeFields.isEmpty()){
			if (null == values)
				values = new ArrayList<Map<String, Object>>();
			if (values.isEmpty()){
				for (ThingTypeField thingTypeField : thingTypeFields){
					// add value for sequences
					Long value = SequenceDAO.getInstance().incrementAndGetSequence(thingTypeField.getId());
					Map<String,Object> properties = new HashMap<String,Object>();
					properties.put("operationId",0);
					properties.put("value",String.valueOf(value));
					properties.put("fieldTypeId",thingTypeField.getId());
					values.add(properties);
				}
			} else {
				// replace value for sequences
				for (ThingTypeField thingTypeField : thingTypeFields){
					Long value = SequenceDAO.getInstance().incrementAndGetSequence(thingTypeField.getId());
					boolean addNew = true;
					for (Map<String, Object> properties : values){
						if (null != properties.get("fieldTypeId") && properties.get("fieldTypeId") instanceof Integer){
							Integer fieldTypeId = (Integer) properties.get("fieldTypeId");
							if (Long.valueOf(fieldTypeId).compareTo(thingTypeField.getId()) == 0){
								properties.put("value", String.valueOf(value));
								addNew = false;
								break;
							}
						}
					}
					if (addNew){
						Map<String,Object> propertiesNew = new HashMap<String,Object>();
						propertiesNew.put("operationId",0);
						propertiesNew.put("value",String.valueOf(value));
						propertiesNew.put("fieldTypeId",thingTypeField.getId());
						values.add(propertiesNew);
					}
				}
			}
		}
	}

	private void validateInsert( Thing thing, String serial )
	{
		validateSerial( thing, serial );
		User user = (User) SecurityUtils.getSubject().getPrincipal();
		GroupService groupService = GroupService.getInstance();
		ThingDAO thingDAO = ThingService.getThingDAO();
		if (LicenseService.enableLicense) {
			LicenseDetail licenseDetail = LicenseService.getInstance().getLicenseDetail(user.getActiveGroup(), true);
			Group licenseGroup = groupService.get(licenseDetail.getGroupId());
			boolean isRootLicense = groupService.getRootGroup().getId().equals(licenseGroup.getId());
			Long maxNumberOfThings = licenseDetail.getMaxThings();
			if (maxNumberOfThings != null && maxNumberOfThings > 0) {
				Long countAll;
				if (isRootLicense) {
					countAll = thingDAO.countAll(null);
				} else {
					countAll = thingDAO.countAll(QThing.thing.group.parentLevel2.id.eq(licenseGroup.getParentLevel2().getId()));
				}
				if (countAll >= maxNumberOfThings) {
					throw new UserException("You cannot insert more things because you reach the limit of your license");
				}
			}
		}
	}

	private void validateSerial( Thing thing, String serial )
	{
		ThingService thingService = ThingService.getInstance();
		boolean existsSerial;
		if( thing.getId() == null )
		{
			// insert case
			existsSerial = thingService.existsSerial( serial, thing.getThingType().getId() );
		}
		else
		{
			// update case
			existsSerial = thingService.existsSerial( serial, thing.getThingType().getId(), thing.getId() );
		}
		if( existsSerial )
		{
			throw new UserException( String.format( "Serial '[%s]' already exist for Thing Type '[%s]'", serial, thing.getThingType()
					.getName() ) );
		}
	}

	/*@PATCH
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@RequiresAuthentication
	@ApiOperation(value = "Update Thing Deprecated")
	@Override
	@Deprecated
	public Response updateThing( @PathParam("id") Long thingId, Map<String, Object> thingMap )
	{
		ThingService thingService = ThingService.getInstance();
		Thing thing = thingService.get(thingId);
		if( thing == null )
		{
			return RestUtils.sendBadResponse( String.format( "ThingId[%d] not found", thingId ) );
		}
		try {
			Map<String, Object> result = updateThingStructure(thingId, thingMap);
			return RestUtils.sendOkResponse(result);
		} catch (UserException e) {
			return RestUtils.sendResponseWithCode(e.getMessage(), 400);
		}

	}*/

	/*
	public Map<String, Object> updateThingStructure(Long thingId, Map<String, Object> thingMap) throws UserException{
		Map<String, Object> object;
		ThingService thingService = ThingService.getInstance();
		Thing thing = thingService.get(thingId);
		if( thing == null )
		{
			return null;
		}

		String serial = (String) thingMap.get("serial");
		//serial = serial.trim();

		if(!StringUtils.isAlphanumeric(serial)){
			throw new UserException( "Invalid serial \"" + serial + "\""   );
		}

		String name = (String) thingMap.get("name");
		name = name.trim();

		validateUpdate(thing, serial);

		Subject subject = SecurityUtils.getSubject();
		User currentUser = (User) subject.getPrincipal();

		Thing parent = null;
		if( thingMap.get( "parent.id" ) != null )
		{
			Long parentId = ((Number) thingMap.get( "parent.id" )).longValue();
			parent = thingService.get(parentId);
			if( parent == null )
			{
				throw new UserException( String.format( "Parent[%d] does not exist", parentId ) );
			}
		}

		limitVisibilityUpdateT(thing, VisibilityUtils.getObjectGroup(thingMap));
		validateUpdate( thing, (String) thingMap.get("serial") );

		ArrayList<Object> childrenList = (ArrayList) thingMap.get( "childrenList" );
		thingMap.remove("childrenList");

		// Updating Thing
		Thing oldParent = thing.getParent();
		BeanUtils.setProperties( thingMap, thing );
		thing.setSerial(thing.getSerial().trim());

		Date storeDate = new Date();

        //dis-associate from child
        if( parent == null  ) {
            //check if we are dis-associating parent
			if (oldParent != null) {
                ThingMongoDAO.getInstance().disAssociateChild(oldParent, thing.getId());
            }
		}
        //associate from child
        else {
            ThingMongoDAO.getInstance().associateChild(parent, oldParent, thing.getId());
        }

        thing.setParent(parent);

		if( thingMap.containsKey( "group.id" ) )
		{
			Group group = GroupService.getInstance().get( Long.parseLong(thingMap.get("group.id").toString()));
			if( group == null )
			{
				throw new UserException( "Invalid Group" );
			}
			thing.setGroup( group );
		}

		if( thingMap.get( "groupTypeFloor.id" ) != null )
		{
			Number number = (Number) thingMap.get( "groupTypeFloor.id" );
			GroupType groupType = null;
			if( number != null )
			{
				groupType = GroupTypeService.getInstance().get( number.longValue() );
			}
			thing.setGroupTypeFloor( groupType );
		}

		Map<String, Object> thingData = new HashMap<>();
		thingData.put("values", thingMap.get("values"));

		Map thingResponse =  updateThingValues(thingId, thingData);
        ThingMongoDAO.getInstance().updateParent(thingId);

		List<Map> childrenThingResponse = new ArrayList<>();

		if( childrenList != null )
		{
            List<Long> oldChildIds = new ArrayList<>();
            List<Long> newChildIds = new ArrayList<>();

            List<Thing> childrenThing = ThingService.getChildrenList( thing );
			for( Thing childThing : childrenThing )
			{
				oldChildIds.add(childThing.getId());
			}

			for( Object childThingMap : childrenList )
			{
				Long childThingId = Long.valueOf(((Map) childThingMap).get("thingId").toString());
				Thing childThing = thingService.get(childThingId);
				if( ThingService.getFirstChild( childThing ) != null )
				{
					throw new ForbiddenException( "The thing [ " + childThing.getId() + " ] is a parent." );
				}
				newChildIds.add(childThingId);

				Map<String, Object> childThingData = new HashMap<>();
				childThingData.put("values", ((Map)childThingMap).get("values"));
				Map childResponse = updateThingValues(childThingId, childThingData);
                //update parent copy
                ThingMongoDAO.getInstance().updateParent(childThingId);

                childrenThingResponse.add(childResponse);

			}
			List<Long> aux = new ArrayList<>(newChildIds);
			aux.removeAll(oldChildIds);
			for (Long childId : aux) {
				Thing child = thingService.get(childId);
				child.setParent( thing );
				thingService.update(child);
				//FMCUtils.fmcHandleAssignTag(thing, child, thing.getGroup(), storeDate, currentUser);
			}
			List<Long> aux2 = new ArrayList<>(oldChildIds);
			aux2.removeAll(newChildIds);
			for (Long childId : aux2) {
				Thing child = thingService.get(childId);
				child.setParent( null );
				thingService.update(child);
				//FMCUtils.fmcHandleUnAssignTag(thing, child, thing.getGroup(), storeDate, currentUser);
			}
            ThingMongoDAO.getInstance().associateChildren(thing, newChildIds, oldChildIds );
        }

		ThingService.getInstance().update(thing, storeDate);

		BrokerClientHelper.sendRefreshSingleThingMessage(thing.getThingType().getThingTypeCode(), thing.getSerial());

		object = thing.publicMap();

		object.put("thingValues", thingResponse);
		object.put("childrenValues", childrenThingResponse);

		if(thingMap.get("parent.id")!=null) {
			object.put("parent.id", thingMap.get("parent.id"));
		}
		object.put("timestamp", storeDate.getTime());
		return object;
	}
	*/

	/*
	@Deprecated
	public Map updateThingValues(Long thingId, Map<String, Object> inputMap){
		Subject subject = SecurityUtils.getSubject();
		User currentUser = (User) subject.getPrincipal();
		ObjectMapper objectMapper = new ObjectMapper();
		long startTime = (new Date()).getTime();
		String transactionCode = ""+startTime;
		logger.warn("updateThingValues, transactionCode: " + transactionCode + ", input: " + inputMap);

		ThingService thingService = ThingService.getInstance();
		Map result = new HashMap();

		boolean someError = false;
		int auxOperationId = 0;
		List<Map> valuesResponses = new ArrayList<>();
		{
			List<Map> listOfMaps = (List<Map>) inputMap.get("values");
			if (listOfMaps != null) {
				for (Map map : listOfMaps) {
					auxOperationId++;
					Map mapResponse = new HashMap();
					mapResponse.put("error", false);
					mapResponse.put("errorMessage", "");
					try {
						Long id = map.get("operationId") != null ? ((Number) map.get("operationId")).longValue() : null;
						mapResponse.put("operationId", id);
						String value = (String) map.get("value");
						Date date = getDateFromMap(map);
						Thing thing = thingId != null ? thingService.get(thingId) : getThingFromMap1L(map);
						if (thing == null) {
							throw new RuntimeException("thing can't be determined");
						}
						ThingTypeField thingTypeField = getThingTypeFieldFromMap1L(thing, map);
						if (thingTypeField == null) {
							throw new RuntimeException("thing type field can't be determined");
						}
						ThingType thingType = thing.getThingType();
						validatePermissionsUpdate(subject, thing);
						Date storeDate = date == null ? new Date(startTime) : date;

						// validation for shift
						if (thingTypeField.getDataType().getId().compareTo( ThingTypeField.Type.TYPE_SHIFT.value ) == 0 )
						{
							String [] shifts = StringUtils.split(value, ",");
							for (String shift : shifts)
							{
								if (!StringUtils.isNumeric(shift))
									throw new RuntimeException("Shift is not valid");
							}
						}
						// TODO call Update
						//FieldValueService.insert(thing.getId(), thingTypeField.getId(),
						//		storeDate, value, thingTypeField.getTimeSeries());
						BrokerClientHelper.sendUpdateThingField(thingType.getThingTypeCode(), thing.getSerial(), storeDate.getTime(), thingTypeField.getName(), value);
                        BrokerClientHelper.doNow();
						FMCUtils.fmcHandleUpdateTag(thing, thingTypeField, value, storeDate, currentUser, transactionCode, id != null ? id : auxOperationId, true);

					} catch (Exception ex) {
						someError =true;
						logger.error(ex.getMessage(), ex);
						mapResponse.put("error", true);
						mapResponse.put("errorMessage", ex.getMessage());
					}
					valuesResponses.add(mapResponse);
				}
			}
		}

		result.put("values", valuesResponses);

		boolean storeLog = inputMap.get("storeLog") != null && Boolean.TRUE.equals(inputMap.get("storeLog"));
		if (storeLog) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
			sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
			try {
				File logFile = getLogFile(currentUser, transactionCode);
				if (logFile != null) {
					logger.warn("writing log file: " + logFile.getAbsolutePath());
					FileUtils.writeStringToFile(logFile, "THING BATCH UPDATE: \r\n", true);
					FileUtils.writeStringToFile(logFile, "TransactionId: " + transactionCode + "\r\n", true);
					FileUtils.writeStringToFile(logFile, "Status: " + (someError ? " some error" : " all ok") + "\r\n", true);
					FileUtils.writeStringToFile(logFile, "User: " + currentUser.getUsername() + " " + currentUser.getId() + "\r\n", true);
					long finalTime = System.currentTimeMillis();
					FileUtils.writeStringToFile(logFile, "Time: start " + startTime + " end " + finalTime + " elapsed " + (finalTime - startTime) + "\r\n", true);
					FileUtils.writeStringToFile(logFile, "Input:\r\n", true);
					FileUtils.writeStringToFile(logFile, objectMapper.writeValueAsString(inputMap) + "\r\n", true);
					FileUtils.writeStringToFile(logFile, "Output:\r\n", true);
					FileUtils.writeStringToFile(logFile, objectMapper.writeValueAsString(result) + "\r\n", true);
					FileUtils.writeStringToFile(logFile, "------------\r\n", true);
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
		logger.warn("updateThingValues, transactionCode: " + transactionCode + ", output: " + result);

		return result;
	}
	*/

	/*Update Thing*/
	/*
	public Map<String, Object> updateThingCommon( Long thingId, Map<String, Object> thingMap )
	{
		Map<String, Object> object = new HashMap<String, Object>();
		ThingService thingService = ThingService.getInstance();
		Thing thing = thingService.get(thingId);
		if( thing == null )
		{
			return null;
		}

		String serial = (String) thingMap.get("serial");
		serial = serial.trim();

		validateUpdate(thing, serial);

		Thing parent = null;
		if( thingMap.get( "parent.id" ) != null )
		{
			Long parentId = ((Number) thingMap.get( "parent.id" )).longValue();
			parent = thingService.get(parentId);
			if( parent == null )
			{
				throw new UserException( String.format( "Parent[%d] does not exist", parentId ) );
			}
		}
		Subject subject = SecurityUtils.getSubject();
		User currentUser = (User) subject.getPrincipal();
		limitVisibilityUpdateT(thing, VisibilityUtils.getObjectGroup(thingMap));
		validateUpdate( thing, (String) thingMap.get("serial") );

		List<Map<String, Object>> thingFieldsMap = (List<Map<String, Object>>) thingMap.get( "fields" );
		List<ThingTypeField> thingTypeFields = null;

		ArrayList<Object> childrenIdList = (ArrayList) thingMap.get( "childrenIdList" );
		thingMap.remove( "childrenIdList" );

		if( thingFieldsMap != null )
		{
			thingTypeFields = new LinkedList<ThingTypeField>();
			for( Map<String, Object> thingFieldMap : thingFieldsMap )
			{
				ThingTypeField thingTypeField = new ThingTypeField();
				BeanUtils.setProperties( thingFieldMap, thingTypeField );

				thingTypeFields.add(thingTypeField);
			}
		}

		// Updating Thing
		thingMap.remove( "fields" );
		Thing oldParent = thing.getParent();
		BeanUtils.setProperties( thingMap, thing );
		thing.setSerial(thing.getSerial().trim());

		Date storeDate = new Date();
		if( thingMap.containsKey( "parent.id" ) )
		{
			if (parent == null && oldParent != null) {
				FMCUtils.fmcHandleUnAssignTag(oldParent, thing, thing.getGroup(), storeDate, currentUser);
			} else if (parent != null) {
				FMCUtils.fmcHandleAssignTag(parent, thing, thing.getGroup(), storeDate, currentUser);
			}
			thing.setParent(parent);
		}
		if( thingMap.containsKey( "group.id" ) )
		{
			Group group = GroupService.getInstance().get( ((Number) thingMap.get( "group.id" )).longValue() );
			if( group == null )
			{
				throw new UserException( "Invalid Group" );
			}
			thing.setGroup( group );
			// if(GroupService.getInstance().isGroupNotInsideTree(thing.getGroup(),
			// visibilityGroup)){
			// throw new ForbiddenException("Forbidden thing");
			// }
		}

		if( thingMap.get( "groupTypeFloor.id" ) != null )
		{
			Number number = (Number) thingMap.get( "groupTypeFloor.id" );
			GroupType groupType = null;
			if( number != null )
			{
				groupType = GroupTypeService.getInstance().get( number.longValue() );
			}
			thing.setGroupTypeFloor( groupType );
		}

		if( childrenIdList != null )
		{
			List<Thing> childrenThing = ThingService.getChildrenList( thing );
			List<Long> oldChildIds = new ArrayList<>();
			List<Long> newChildIds = new ArrayList<>();
			for( Thing childThing : childrenThing )
			{
				oldChildIds.add(childThing.getId());
			}

			for( Object childThingIdInt : childrenIdList )
			{
				Long childThingId = Long.valueOf( childThingIdInt.toString() );
				Thing childThing = thingService.get(childThingId);
				if( ThingService.getFirstChild( childThing ) != null )
				{
					throw new ForbiddenException( "The thing [ " + childThing.getId() + " ] is a parent." );
				}
				newChildIds.add(childThingId);
			}
			List<Long> aux = new ArrayList<>(newChildIds);
			aux.removeAll(oldChildIds);
			for (Long childId : aux) {
				Thing child = thingService.get(childId);
				child.setParent( thing );
				thingService.update(child);
				FMCUtils.fmcHandleAssignTag(thing, child, thing.getGroup(), storeDate, currentUser);
			}
			List<Long> aux2 = new ArrayList<>(oldChildIds);
			aux2.removeAll(newChildIds);
			for (Long childId : aux2) {
				Thing child = thingService.get(childId);
				child.setParent( null );
				thingService.update(child);
				FMCUtils.fmcHandleUnAssignTag(thing, child, thing.getGroup(), storeDate, currentUser);
			}
		}

		// ThingService.updateImage(thing, thingMap);

		ThingService.getInstance().update( thing, storeDate);

		//BrokerClientHelper.sendRefreshThingMessage();
		BrokerClientHelper.sendRefreshSingleThingMessage(thing.getThingType().getThingTypeCode(), thing.getSerial());

		object = thing.publicMap();
		if(thingMap.get("parent.id")!=null) {
			object.put("parent.id", thingMap.get("parent.id"));
		}
		object.put("timestamp", storeDate.getTime());
		return object;
	}
	*/

	public void validateUpdate( Thing thing, String serial )
	{
		validateSerial(thing, serial);
	}



	/*private void deleteThing(Thing thing) {
		String thingSerialNumber = thing.getSerial();
		String thingTypeCode = thing.getThingType().getThingTypeCode();

		limitVisibilityDeleteT(thing);
		// handle validation in an Extensible manner
		validateDelete( thing );

		// Deleting Thing

		ThingService.deleteThing(thing);

		//BrokerClientHelper.sendRefreshThingMessage();
		Subject subject = SecurityUtils.getSubject();
		User currentUser = (User) subject.getPrincipal();
		FMCUtils.fmcHandleDeleteAsset(thing, thing.getGroup(), new Date(), currentUser);
		BrokerClientHelper.sendDeleteThingMessage(thingTypeCode, thingSerialNumber);
	}*/

	private Date getNowMinusRelativeTime(Long timeToKeep, String periodType){
		Calendar now = Calendar.getInstance();

		//Relative Time Values to subtract to now
		long day = now.get(Calendar.DAY_OF_MONTH);
		long hour = now.get(Calendar.HOUR_OF_DAY);
		long minute = now.get(Calendar.MINUTE);
		long second = now.get(Calendar.SECOND);

		switch (periodType){
			case "s":
			case "sec":
			case "second":
				day = (int) TimeUnit.SECONDS.toDays(timeToKeep);

				hour = TimeUnit.SECONDS.toHours(timeToKeep) -
						TimeUnit.DAYS.toHours(day);

				minute = TimeUnit.SECONDS.toMinutes(timeToKeep) -
						TimeUnit.DAYS.toMinutes(day) -
						TimeUnit.HOURS.toMinutes(hour);

				second = TimeUnit.SECONDS.toSeconds(timeToKeep) -
						TimeUnit.DAYS.toSeconds(day) -
						TimeUnit.HOURS.toSeconds(hour) -
						TimeUnit.MINUTES.toSeconds(minute);
				break;
			case "m":
			case "min":
			case "minute":
				day = (int) TimeUnit.MINUTES.toDays(timeToKeep);

				hour = TimeUnit.MINUTES.toHours(timeToKeep) -
						TimeUnit.DAYS.toHours(day);

				minute = TimeUnit.MINUTES.toMinutes(timeToKeep) -
						TimeUnit.DAYS.toMinutes(day) -
						TimeUnit.HOURS.toMinutes(hour);
				break;
			case "h":
			case "hrs":
			case "hour":
				day = (int) TimeUnit.HOURS.toDays(timeToKeep);

				hour = TimeUnit.HOURS.toHours(timeToKeep) -
						TimeUnit.DAYS.toHours(day);

				break;
			case "d":
			case "day":
				day = timeToKeep;
				break;
			default:
				throw new IllegalArgumentException("Relative time period unsupported");
		}

		day = timeToKeep==0?0:day;
		hour = timeToKeep==0?0:hour;
		minute = timeToKeep==0?0:minute;
		second = timeToKeep==0?0:second;

		now.add(Calendar.SECOND, (int) second * -1);
		now.add(Calendar.MINUTE, (int) minute * -1);
		now.add(Calendar.HOUR, (int) hour * -1);
		now.add(Calendar.DAY_OF_MONTH, (int) day * -1);

		return now.getTime();

	}

	/*
	* This method updates a unique udf of a thing, if you want to update many udf's of a thing
	* you have to send the udfs into an array
	* */
	@POST
	@Path("/updateUDFs")
	@Produces(MediaType.APPLICATION_JSON)
	@RequiresAuthentication
	@RequiresPermissions(value = {"thingType:r"})
	@Deprecated
	@ApiOperation(position=7, value = "Updates Many Thing udfs in one batch operation")
	public Response updateUDfs(Map<String, Object> inputMap)
	{
		logger.warn("This End Point is going to be deprecated. Please use thing/update End Point");
		Subject subject = SecurityUtils.getSubject();
		User currentUser = (User) subject.getPrincipal();
		ObjectMapper objectMapper = new ObjectMapper();
		long startTime = (new Date()).getTime();
		String transactionCode = ""+startTime;
		logger.warn("updateThingValues, transactionCode: " + transactionCode + ", input: " + inputMap);

		ThingService thingService = ThingService.getInstance();
		Map result = new HashMap();

		boolean someError = false;
		int auxOperationId = 0;
		List<Map> valuesResponses = new ArrayList<>();
		{
			List<Map> listOfMaps = (List<Map>) inputMap.get("values");
			if (listOfMaps != null) {
				for (Map map : listOfMaps) {
					auxOperationId++;
					Map mapResponse = new HashMap();
					mapResponse.put("error", false);
					mapResponse.put("errorMessage", "");
					try {
						Long id = map.get("operationId") != null ? ((Number) map.get("operationId")).longValue() : null;
						mapResponse.put("operationId", id);
						String value = (String) map.get("value");
						Date date = getDateFromMap(map);
						Thing thing = getThingFromMap1L(map);
						if (thing == null) {
							throw new RuntimeException("thing can't be determined");
						}
						ThingTypeField thingTypeField = getThingTypeFieldFromMap1L(thing, map);
						if (thingTypeField == null) {
							throw new RuntimeException("thing type field can't be determined");
						}
						ThingType thingType = thing.getThingType();
						validatePermissionsUpdate(subject, thing);
						Date storeDate = date == null ? new Date(startTime) : date;

						// validation for shift
						if (thingTypeField.getDataType().getId().compareTo( ThingTypeField.Type.TYPE_SHIFT.value ) == 0 )
						{
							String [] shifts = StringUtils.split(value, ",");
							for (String shift : shifts)
							{
								if (!StringUtils.isNumeric(shift))
									throw new RuntimeException("Shift is not valid");
							}
						}
						DateTimeFormatter isoDateFormat = ISODateTimeFormat.dateTime();

						Map<String, Object> udf = new HashMap<>();
						udf.put("value", value);
						udf.put("time", isoDateFormat.print(new DateTime(storeDate)));
						Map<String, Object> udfs = new HashMap<>();
						udfs.put(thingTypeField.getName(), udf);
                        Stack<Long> recursivelyStack = new Stack<>();
						thingService.update(
                                recursivelyStack
                                , thing.getId()
								, thingType.getThingTypeCode()
								, thing.getGroup().getHierarchyName()
								, thing.getName()
								, thing.getSerial()
								, null
								, udfs
								, null
								, null
								, true, true, storeDate, false, currentUser, true);

					} catch (Exception ex) {
						someError =true;
						logger.error(ex.getMessage(), ex);
						mapResponse.put("error", true);
						mapResponse.put("errorMessage", ex.getMessage());
					}
					valuesResponses.add(mapResponse);
				}
			}
		}

		result.put("values", valuesResponses);

		boolean storeLog = inputMap.get("storeLog") != null && Boolean.TRUE.equals(inputMap.get("storeLog"));
		if (storeLog) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
			sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
			try {
				File logFile = getLogFile(currentUser, transactionCode);
				if (logFile != null) {
					logger.warn("writing log file: " + logFile.getAbsolutePath());
					FileUtils.writeStringToFile(logFile, "THING BATCH UPDATE: \r\n", true);
					FileUtils.writeStringToFile(logFile, "TransactionId: " + transactionCode + "\r\n", true);
					FileUtils.writeStringToFile(logFile, "Status: " + (someError ? " some error" : " all ok") + "\r\n", true);
					FileUtils.writeStringToFile(logFile, "User: " + currentUser.getUsername() + " " + currentUser.getId() + "\r\n", true);
					long finalTime = System.currentTimeMillis();
					FileUtils.writeStringToFile(logFile, "Time: start " + startTime + " end " + finalTime + " elapsed " + (finalTime - startTime) + "\r\n", true);
					FileUtils.writeStringToFile(logFile, "Input:\r\n", true);
					FileUtils.writeStringToFile(logFile, objectMapper.writeValueAsString(inputMap) + "\r\n", true);
					FileUtils.writeStringToFile(logFile, "Output:\r\n", true);
					FileUtils.writeStringToFile(logFile, objectMapper.writeValueAsString(result) + "\r\n", true);
					FileUtils.writeStringToFile(logFile, "------------\r\n", true);
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
		logger.warn("updateThingValues, transactionCode: " + transactionCode + ", output: " + result);

		return RestUtils.sendOkResponse(result);
	}

	public static final ThreadLocal<Long> fmcGroup = new ThreadLocal<Long>();
	/*
	* This end poit unassign all the old children so as to add the new one which is sending to it,
	* then the Thing Parent is going to have just one children
	* */
	@POST
	@Path("/updateParentChildRelationships")
	@Produces(MediaType.APPLICATION_JSON)
	@RequiresAuthentication
	@RequiresPermissions(value = {"thingType:r"})
	@Deprecated
	@ApiOperation(position = 7, value = "Updates Many Thing relationships in one batch operation, and replace all old children for the new one which is sending to it")
	public Response updateParentChildRelationships(Map<String, Object> inputMap) {
		logger.warn("This End Point is going to be deprecated. Please use thing/update End Point");
		Subject subject = SecurityUtils.getSubject();
		User currentUser = (User) subject.getPrincipal();
		ObjectMapper objectMapper = new ObjectMapper();
		long startTime = (new Date()).getTime();
		String transactionCode = ""+startTime;
		logger.warn("batchUpdate, transactionCode: " + transactionCode + ", input: " + inputMap);

		ThingService thingService = ThingService.getInstance();
		Map result = new HashMap();
		Number groupIdNumber = (Number) inputMap.get("groupId");
		Group group = groupIdNumber == null ? currentUser.getActiveGroup() : GroupService.getInstance().get(groupIdNumber.longValue());
		fmcGroup.set(group.getId());
		boolean someError = false;
		int auxOperationId = 0;
        Stack<Long> recursivelyStack = new Stack<>();
		List<Map> mappingsResponses = new ArrayList<>();
		{
			List<Map> mappings = (List<Map>) inputMap.get("mappings");
			if (mappings != null) {
				for (Map mapping : mappings) {
					auxOperationId++;
					Map mapResponce = new HashMap();
					mapResponce.put("error", false);
					mapResponce.put("errorMessage", "");
					try {
						Long id = mapping.get("operationId") != null ? ((Number) mapping.get("operationId")).longValue() : null;
						mapResponce.put("operationId", id);
						Map parentMap = (Map) mapping.get("parent");
						Map childMap = (Map) mapping.get("child");
						if (parentMap == null) {
							throw new RuntimeException("Parent Map not specified");
						}
						Thing parent = getThingFromMap1L(parentMap);
						if (parent == null) {
							throw new RuntimeException("Parent not found");
						}
						Date date = getDateFromMap(mapping);
						Date storeDate = date == null ? new Date(startTime) : date;
						String operation = (String) mapping.get("operation");
						if ("assign".equals(operation)) {
							Thing child = getThingFromMap1L(childMap);
							ThingType childThingType = child == null? getThingTypeFromMap1L(childMap) : child.getThingType();

							List<Thing> children = ThingService.getThingDAO().getQuery().where(QThing.thing.parent.eq(parent)).list(QThing.thing);
							boolean alreadyAssigned = false;
							for (Thing oldChild : children) {
								if (childThingType.getId().equals(oldChild.getThingType().getId())) {
									if (oldChild.getId().equals(child != null ? child.getId() : null)) {
										//already assigned
										alreadyAssigned = true;
									} else {
										//oldChildren.setParent(null);
										Map<String, Object> pMap = new HashMap<>();
										thingService.update(
												recursivelyStack, oldChild, pMap, null, null,null, true, true, storeDate,
												false, currentUser, true);
									}
								}
							}
							if (!alreadyAssigned) {
								if (child == null) {
									//create child
									Map<String, Object> pMap = new HashMap<>();
									pMap.put("serialNumber", parent.getSerial());
									pMap.put("thingTypeCode", parent.getThingType().getThingTypeCode());
                                    ThingsService.getInstance().create(
                                            recursivelyStack,
                                            childThingType.getThingTypeCode(),
                                            parent.getGroup().getHierarchyName( true ),
                                            (String) childMap.get("serial"),
                                            (String) childMap.get("serial") ,
											pMap,
                                            null,
                                            null,
											null,
                                            true,
                                            true,
                                            storeDate, true, true );
                                } else {
									//update child
									validatePermissionsUpdate(subject, child);
									Map<String, Object> pMap = new HashMap<>();
									pMap.put("serialNumber", parent.getSerial());
									pMap.put("thingTypeCode", parent.getThingType().getThingTypeCode());
									thingService.update(
											recursivelyStack, child, pMap, null, null, null, true, true, storeDate, false,
											currentUser, true);

								}
							}
						} else if ("unassign".equals(operation)) {
							if (childMap == null || childMap.isEmpty()) {
								List<Thing> childThings = thingService.getThingDAO().selectAllBy(QThing.thing.parent.eq(parent));
								for (Thing child : childThings) {
									Map<String, Object> pMap = new HashMap<>();
									thingService.update(
											recursivelyStack, child, pMap, null, null,null, true, true, storeDate,
											false, currentUser, true);
								}
							} else {
								Thing child = getThingFromMap1L(childMap);
								if (child != null) {
									validatePermissionsUpdate(subject, child);
									Thing parentOld = child.getParent();
									//If is not unassigned
									if (parentOld !=null && parentOld.getId().equals(parent.getId())) {
										//Call to PATCH UPDATE UNASSIGN
										Map<String, Object> pMap = new HashMap<>();
										thingService.update(recursivelyStack, child, pMap, null, null, null, true, true,
												storeDate, false, currentUser, true);
									}
								}
							}
						} else {
							throw new RuntimeException("Unsupported operation: "+operation);
						}
					} catch (Exception ex) {
						someError =true;
						logger.error(ex.getMessage(), ex);
						mapResponce.put("error", true);
						mapResponce.put("errorMessage", ex.getMessage());
					}
					mappingsResponses.add(mapResponce);
				}
			}
		}

		result.put("mappings", mappingsResponses);

		boolean storeLog = inputMap.get("storeLog") != null && Boolean.TRUE.equals(inputMap.get("storeLog"));
		if (storeLog) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
			sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
			try {
				File logFile = getLogFile(currentUser, transactionCode);
				if (logFile != null) {
					logger.warn("writing log file: " + logFile.getAbsolutePath());
					FileUtils.writeStringToFile(logFile, "THING BATCH UPDATE: \r\n", true);
					FileUtils.writeStringToFile(logFile, "TransactionId: " + transactionCode + "\r\n", true);
					FileUtils.writeStringToFile(logFile, "Status: " + (someError ? " some error" : " all ok") + "\r\n", true);
					FileUtils.writeStringToFile(logFile, "User: " + currentUser.getUsername() + " " + currentUser.getId() + "\r\n", true);
					long finalTime = System.currentTimeMillis();
					FileUtils.writeStringToFile(logFile, "Time: start " + startTime + " end " + finalTime + " elapsed " + (finalTime - startTime) + "\r\n", true);
					FileUtils.writeStringToFile(logFile, "Input:\r\n", true);
					FileUtils.writeStringToFile(logFile, objectMapper.writeValueAsString(inputMap) + "\r\n", true);
					FileUtils.writeStringToFile(logFile, "Output:\r\n", true);
					FileUtils.writeStringToFile(logFile, objectMapper.writeValueAsString(result) + "\r\n", true);
					FileUtils.writeStringToFile(logFile, "------------\r\n", true);
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
		logger.warn("batchUpdate, transactionCode: " + transactionCode + ", output: " + result);
		return RestUtils.sendOkResponse(result);
	}


	@POST
	@Path("/coreBridgeListener")
	@Produces(MediaType.APPLICATION_JSON)
	@RequiresAuthentication
	@RequiresPermissions(value = {"thingType:r"})
	@ApiOperation(position= 7, value = "Receives direct notifications from coreBridge")
	public Response coreBridgeListener(Map<String, Object> inputMap) {
		String thingTypeCode = (String) inputMap.get("thingTypeCode");
		String serialNumber = (String) inputMap.get("serialNumber");
		String thingFieldName = (String) inputMap.get("thingFieldName");
		String value = (String) inputMap.get("value");
		Long time = inputMap.get("time") != null ? ((Number) inputMap.get("time")).longValue() : null;
		if (StringUtils.isNotEmpty(thingTypeCode) && StringUtils.isNotEmpty(serialNumber) && StringUtils.isNotEmpty(thingFieldName) && value != null) {
			try {
				QThing qThing = QThing.thing;
				ThingDAO thingDAO = ThingService.getInstance().getThingDAO();
				Thing thing = thingDAO.selectBy(qThing.thingType.thingTypeCode.eq(thingTypeCode).and(qThing.serial.eq(serialNumber)));
				ThingTypeField thingTypeField = thing.getThingType().getThingTypeFieldByName(thingFieldName);

				FMCUtils.fmcHandleUpdateTagZone(thing, thingTypeField, value, new Date(time));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		Map result = new HashMap();
		return RestUtils.sendOkResponse(result);
	}


	public static File getLogFile(User currentUser, String transactionCode)  {
		String batchUpdateLogDirectory = ConfigurationService.getAsString(currentUser, "batchUpdateLogDirectory");
		String fileExtension = ".log";
		File logFile = null;
		String fileName = "ThingUpdate-"+ transactionCode+ "-"+ currentUser.getId()+"-";
		if (StringUtils.isEmpty(batchUpdateLogDirectory)) {
			return null;
		} else {
			try {
				File dirFile = new File(batchUpdateLogDirectory);
				logFile = new File(dirFile, fileName + fileExtension);
			} catch (Exception ex) {
				try {
					logFile = File.createTempFile(fileName, fileExtension);
				} catch (IOException expected) {

				}
			}
		}
		return logFile;
	}

	private void validatePermissionsUpdate(Subject subject, Thing thing) {
		limitVisibilityUpdateT(thing, null);
	}

	private Date getDateFromMap(Map map) {
		if (map.get("dateJavaLong") != null) {
			return new Date(((Number) map.get("dateJavaLong")).longValue());
		} else  if (map.get("dateIso") != null) {
			return ISODateTimeFormat.dateTimeParser().parseDateTime((String) map.get("dateIso")).toDate();
		}
		return null;
	}

	private ThingType getThingTypeFromMap1L(Map map) {
		ThingType thingType = null;
		if (map.containsKey("thingTypeId") && map.get("thingTypeId") != null) {
			Long thingTypeId = ((Number) map.get("thingTypeId")).longValue();
			thingType = ThingTypeService.getInstance().get(thingTypeId);
		} else if (map.containsKey("thingTypeCode") && StringUtils.isNotEmpty("thingTypeCode")) {
			String thingTypeCode = (String) map.get("thingTypeCode");
			try {
				thingType = ThingTypeService.getInstance().getByCode(thingTypeCode);
			} catch (NonUniqueResultException e) {
				e.printStackTrace();
			}
		}
		return thingType;
	}

	private Thing getThingFromMap1L(Map map) {
		ThingDAO thingDAO = ThingService.getInstance().getThingDAO();
		QThing qThing = QThing.thing;
		Thing thing = null;
//		Long thingId = map.get("thingId") != null ? ((Number) map.get("thingId")).longValue() : null;
		Long thingId = map.containsKey("thingId") ? Long.parseLong(map.get("thingId").toString()) : null;
		if (thingId != null) {
			thing = ThingService.getInstance().get(thingId);

			int flag = 0;
			int status = 0;
			for(ThingTypeField data : thing.getThingType().getThingTypeFields())
			{
				if(data.getName().length()>=4
						&& data.getName().substring(data.getName().length()-4 , data.getName().length()).equals( "Flag" ))
				{
					flag++;
				}
				if(data.getName().length()>=6
						&& data.getName().substring( data.getName().length() - 6, data.getName().length() ).equals( "Status" ))
				{
					status++;
				}
			}
			if(flag>0 && status>0)
			{
				thing = ThingService.getInstance().get(thingId);
			}else
			{
				flag = 0;
				status = 0;
				List<Thing> children = ThingService.getInstance().getChildrenList( thing )	;
				for(Thing child : children)
				{
					for(ThingTypeField data : child.getThingType().getThingTypeFields())
					{
						if(data.getName().length()>=4 &&
								data.getName().substring(data.getName().length()- 4, data.getName().length()).equals( "Flag" ))
						{
							flag++;
						}
						if(data.getName().length()>=6 &&
								data.getName().substring( data.getName().length() - 6, data.getName().length() ).equals( "Status" ))
						{
							status++;
						}
					}
					if(flag>0 && status>0)
					{
						thing = child;
					}else
					{
						throw new UserException( "Thing does not have Flag and Status properties for 'Dismiss' logic." );
					}
				}

			}

		} else {
			ThingType thingType = getThingTypeFromMap1L(map);
			if (thingType != null) {
				String serial = map.get("serial") != null ? ((String) map.get("serial")) : null;
				if (StringUtils.isNotEmpty(serial)) {
					String serialNumber = serial.toUpperCase();
					List<Thing> things = thingDAO.getQuery().where(qThing.thingType.id.eq(thingType.getId()).and(qThing.serial.eq(serialNumber))).list(qThing);
					thing = (things.size() > 0) ? things.get(0) : null;
				} else if (map.containsKey("keyFieldName") && map.containsKey("keyFieldValue")) {

					List<String> serialNumbers = listThingByThingTypeCodeUDFValue(thingType.getThingTypeCode(), (String) map.get("keyFieldName"), (String) map.get("keyFieldValue"));
					if (serialNumbers.size() > 0) {
						List<Thing> things = thingDAO.getQuery().where(qThing.thingType.id.eq(thingType.getId()).and(qThing.serial.eq(serialNumbers.get(0)))).list(qThing);
						thing = (things.size() > 0) ? things.get(0) : null;
					}
				}
			}
		}
		return thing;
	}

	private ThingTypeField getThingTypeFieldFromMap1L(Thing thing, Map map) {
		ThingTypeField thingTypeField;
		Long fieldTypeId = map.get("fieldTypeId") != null ? ((Number) map.get("fieldTypeId")).longValue() : null;
		if (fieldTypeId != null ) {
			thingTypeField = ThingTypeFieldService.getInstance().get(fieldTypeId);
		} else {
			String fieldName = map.get("fieldName") != null ? ((String) map.get("fieldName")) : null;
			if (fieldName == null) {
				throw new RuntimeException("Can't determine field, parameters were not sent");
			}
			thingTypeField = thing.getThingType().getThingTypeFieldByName(fieldName);
		}
		return thingTypeField;
	}

	/**
	 * Used by mcb bridge and mqtt topic in order to allow hand held device to
	 * associate two things
	 *
	 * /*@see com. tierconnect. riot. iot. connectors. CoreBridge
	 *
	 * @param serialNumber1
	 * @param serialNumber2
	 * @return
	 *
	 *         TODO: move into new controller class for hand helds !
	 */
	@POST
	@Path("/{serialNumber1}/setParent/{serialNumber2}")
	@Produces(MediaType.APPLICATION_JSON)
	@RequiresAuthentication
	@RequiresPermissions(value = {"thingType:r"})
	// TODO: FIX PERMISSIONING
	@ApiOperation(position= 7, value = "Set a Thing's parent")
	public Response setThingParent( @PathParam("serialNumber1") String serialNumber1, @PathParam("serialNumber2") String serialNumber2 )
	{
		// ThingService.getInstance()selectThings( id, extra, only )
		// TODO

		Thing thing1 = null;
		Thing thing2 = null;
		try
		{
			thing1 = ThingService.getInstance().getBySerialNumber( serialNumber1!=null?serialNumber1.toUpperCase():null );
		}
		catch( NonUniqueResultException e )
		{
			return RestUtils.sendBadResponse( String.format( "Duplicate serial number [%s]", serialNumber1 ) );
		}

		try
		{
			thing2 = ThingService.getInstance().getBySerialNumber( serialNumber2!=null?serialNumber2.toUpperCase():null );
		}
		catch( NonUniqueResultException e )
		{
			return RestUtils.sendBadResponse( String.format( "Duplicate serial number [%s]", serialNumber2 ) );
		}

		if( thing2 == null || thing1 == null )
		{
			return RestUtils.sendBadResponse( String.format( "ThingSerial[%s] not found", serialNumber1 ) );
		}

		Group visibilityGroup = getVisibilityGroup( thing1 );
		if( GroupService.getInstance().isGroupNotInsideTree( thing1.getGroup(), visibilityGroup ) )
		{
			throw new ForbiddenException( "Forbidden thing1" );
		}

		if( GroupService.getInstance().isGroupNotInsideTree( thing2.getGroup(), visibilityGroup ) )
		{
			throw new ForbiddenException( "Forbidden thing1" );
        }

        thing1.setParent(thing2);
		ThingService.getInstance().update( thing1, new Date() );
		return RestUtils.sendOkResponse( thing1.publicMap() );
	}

	@POST
	@Path("/{id}/activate")
	@Produces(MediaType.APPLICATION_JSON)
	@RequiresAuthentication
	@RequiresPermissions(value = {"thingType:r"})
	// TODO this method looks like it is not being used at all, confirm and
	// delete
	@ApiOperation(position= 7, value = "Activate Thing")
	public Response activateThing( @PathParam("id") Long thingId )
	{
		Thing thing = ThingService.getInstance().get( thingId );
		if( thing == null )
		{
			return RestUtils.sendBadResponse( String.format( "ThingId[%d] not found", thingId ) );
		}
		if( !PermissionsUtils.isPermitted( SecurityUtils.getSubject(), Resource.THING_TYPE_PREFIX + thing.getThingType().getId() + ":u" ) )
		{
			throw new ForbiddenException( "Not Allowed access" );
		}

		Group visibilityGroup = getVisibilityGroup(thing);
		if( GroupService.getInstance().isGroupNotInsideTree( thing.getGroup(), visibilityGroup ) )
		{
			throw new ForbiddenException( "Forbidden thing" );
		}

		thing.setActivated(true);
		ThingService.getInstance().update( thing, new Date() );
		return RestUtils.sendOkResponse( thing.publicMap() );
	}

	@POST
	@Path("/{id}/deactivate")
	@Produces(MediaType.APPLICATION_JSON)
	@RequiresAuthentication
	@RequiresPermissions(value = {"thingType:r"})
	// TODO this method looks like it is not being used at all, confirm and
	// delete
	@ApiOperation(position= 7, value = "Deactivate Thing")
	public Response deactivateThing( @PathParam("id") Long thingId )
	{
		Thing thing = ThingService.getInstance().get(thingId);
		if( thing == null )
		{
			return RestUtils.sendBadResponse( String.format( "ThingId[%d] not found", thingId ) );
		}
		if( !PermissionsUtils.isPermitted( SecurityUtils.getSubject(), Resource.THING_TYPE_PREFIX + thing.getThingType().getId() + ":u" ) )
		{
			throw new ForbiddenException( "Not Allowed access" );
		}
		Group visibilityGroup = getVisibilityGroup(thing);
		if( GroupService.getInstance().isGroupNotInsideTree( thing.getGroup(), visibilityGroup ) )
		{
			throw new ForbiddenException( "Forbidden thing" );
		}

		thing.setActivated(false);
		ThingService.getInstance().update( thing, new Date() );
		return RestUtils.sendOkResponse( thing.publicMap() );
	}



	@GET
	@Path("/{id}/field/{thingTypeFieldId}")
	@Produces(MediaType.APPLICATION_JSON)
	// TODO: FIX PERMISSIONING
	@RequiresAuthentication
	@RequiresPermissions(value = { "thing:r" })
	@ApiOperation(position= 7, value = "Thing Field value history")
	public Response getValueHistory(@PathParam("id") Long thingId, @PathParam("thingTypeFieldId") Long thingTypeFieldId,
									@QueryParam("startDate") Long startDate, @QueryParam("endDate") Long endDate, @QueryParam("relativeDate") String relativeDate,
									@QueryParam("pageSize") Integer pageSize, @QueryParam("pageIndex") Integer pageIndex,
									@QueryParam("ascending") boolean ascending) {
		List<Map<String, Object>> list = new LinkedList<Map<String, Object>>();
		Thing thing = ThingService.getInstance().get(thingId);
		if (thing == null) {
			throw new UserException("Invalid thingId");
		}
		ThingType thingType = thing.getThingType();
		if (thingType == null) {
			throw new UserException("Invalid thingType");
		}
		ThingTypeField thingTypeField = thingType.getThingTypeFieldById(thingTypeFieldId);
		if (thingTypeField == null) {
			throw new UserException("Invalid thingTypeFieldId");
		}
		Group visibilityGroup = getVisibilityGroup(thing);
		if (GroupService.getInstance().isGroupNotInsideTree(thing.getGroup(), visibilityGroup)) {
			throw new ForbiddenException("Forbidden thing");
		}
		if (pageSize == null) {
			pageSize = 10;
		}

		pageIndex = pageIndex == null ? 1 : pageIndex;
		User currentUser = (User) SecurityUtils.getSubject().getPrincipal();
		DateFormatAndTimeZone dateFormatAndTimeZone = UserService.getInstance().getDateFormatAndTimeZone(currentUser);
		logger.info("USER [" + currentUser.getUsername() + "] REGIONAL SETTING " + dateFormatAndTimeZone);
		DateHelper dh = DateHelper.getRelativeDateHelper(relativeDate, startDate, endDate, new Date(), dateFormatAndTimeZone);

		for (Map<String, Object> datapoint : ThingMongoDAO.getInstance().getFieldHistoryWithPaging(thing.getId(),
				thingTypeField.getName(),
				dh.from(),
				dh.to(),
				pageSize,
				pageIndex,
				ascending)) {

			Map<String, Object> datapointAux = new HashMap<>();
			datapointAux.put("value", datapoint.get("value"));
			if (ThingTypeFieldService.isDateTimeStampType(thingTypeField.getDataType().getId())) {
				if (datapoint.get("value") instanceof Long) {
					datapointAux.put("value", dateFormatAndTimeZone.getISODateTimeFormatWithoutTimeZone((Long) datapoint.get("value")));
				} else if (datapoint.get("value") instanceof Date) {
					datapointAux.put("value", dateFormatAndTimeZone.getISODateTimeFormatWithoutTimeZone((Date) datapoint.get("value")));
				}
			}
			datapointAux.put("at", dateFormatAndTimeZone.getISODateTimeFormatWithoutTimeZone((Date) datapoint.get("time")));
			list.add(datapointAux);
		}

		Map<String, Object> ans = new HashMap<>();
		ans.put("total", ThingMongoDAO.lastQueryLength);
		ans.putAll(thingTypeField.publicMap());
		ans.put("results", list);

		return RestUtils.sendOkResponse(ans);
	}

	public List<String> getExtraPropertyNames()
	{
		return Arrays.asList(IGNORE_THING_FIELDS);
	}



	/**
	 * Moved here from Thing.java
	 */
	private Feature publicGeoJsonFeature( List<LngLatAlt> points, Thing thing )
	{

		// System.out.println(
		// "*********************** METHOD IS USED ******************" );
		// Thread.dumpStack();

		List<Map<String, Object>> fieldsList = new LinkedList<>();

		// CassandraThing cassandraThing =
		// CassandraThingDAO.getThing(thing.getId());
		// if (cassandraThing == null) {
		// return null;
		// }
		// if(thing.getThingTypeFields() != null){
		// for(CassandraField thingField : cassandraThing.getFields()){
		// Map<String,Object> publicMapField = thingField.publicMap();
		// for(ThingField field : thing.getThingTypeFields()){
		// if(thingField.getId() == field.getId()){
		// publicMapField.put("name", field.getName());
		// publicMapField.put("unit", field.getUnit());
		// publicMapField.put("symbol", field.getSymbol());
		// publicMapField.put("blinkDate", field.getBlinkDate());
		// break;
		// }
		// }
		// System.out.println( "******** map1=" + publicMapField );
		// fieldsList.add(publicMapField);
		// }
		// }

		Set<ThingTypeField> tfl = thing.getThingType().getThingTypeFields();
		for( ThingTypeField tf : tfl )
		{
			Map<String, Object> publicMapField = tf.publicMap();
			// System.out.println( "******** map2=" + publicMapField );
			fieldsList.add( publicMapField );
		}

		Feature feature = new Feature();
		feature.setId( thing.getId() + "" );
		feature.setProperty( "name", thing.getName() + "" );
		feature.setProperty( "serial", thing.getSerial() + "" );
		feature.setProperty( "fields", fieldsList );
		if( points == null )
		{
			// Point point = new Point();
			// LngLatAlt coordinates = new LngLatAlt(cassandraThing.getX(),
			// cassandraThing.getY(), 0);
			// point.setCoordinates(coordinates);
			// feature.setGeometry(point);
		}
		else
		{
			LineString lineString = new LineString();
			lineString.setCoordinates( points );
			feature.setGeometry( lineString );
		}
		return feature;
	}

	/**
	 *
	 * IMPORTANT... overwrite of base class because need to pass the group
	 * parameters
	 *
	 */
	@GET
	@Path("/")
	@Override
	@Produces(MediaType.APPLICATION_JSON)
	// 1a. Limit access based on CLASS level resources
	@RequiresAuthentication
	@RequiresPermissions(value = { "thing:r" })
	@ApiOperation(position = 1, value = "Get a List of Things",notes="Get an autogenerated list of Things")
	@ApiResponses(
		value = {
			@ApiResponse(code = 200, message = "Ok"),
			@ApiResponse(code = 400, message = "Bad Request"),
			@ApiResponse(code = 403, message = "Forbidden"),
			@ApiResponse(code = 500, message = "Internal Server Error")
		}
	)
	public Response listThings( @ApiParam(value = "The number of things per page (default 10).") @QueryParam("pageSize") Integer pageSize,
								@ApiParam(value = "The page number you want to be returned (the first one is displayed by default).") @QueryParam("pageNumber") Integer pageNumber,
								@ApiParam(value = "The field to be used to sort the thing results. This can be asc or desc. i.e. name:asc") @QueryParam("order") String order,
								@ApiParam(value = "A filtering parameter to get specific things. Supported operators: Equals (=), like (~), and (&), or (|) ") @QueryParam("where") String where,
								@ApiParam(value = "Add extra fields to the response. i.e parent, group, thingType, group.groupType, group.parent") @Deprecated @QueryParam("extra") String extra,
								@ApiParam(value = "The listed fields will be included in the response. i.e.  only= id,name,code") @Deprecated @QueryParam("only") String only,
								@ApiParam(value = "It is used to overridden default visibilityGroup to a lower group.") @QueryParam("visibilityGroupId") Long visibilityGroupId,
								@ApiParam(value = "It is used to disable upVisibility. It can have True or False values") @DefaultValue("") @QueryParam("upVisibility") String upVisibility,
								@ApiParam(value = "It is used to disable downVisibility. It can have True or False values") @DefaultValue("") @QueryParam("downVisibility") String downVisibility,
								@ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project)
	{
		Pagination pagination = new Pagination( pageNumber, pageSize );

		BooleanBuilder masterVisibilityOr = VisibilityThingUtils.limitSelectAllT(upVisibility, downVisibility, visibilityGroupId);

		BooleanBuilder masterBe = new BooleanBuilder( masterVisibilityOr );

		// 4. Implement filtering
		masterBe = masterBe.and( QueryUtils.buildSearch( QThing.thing, where) );

		Long count = ThingService.getInstance().countList( masterBe );
		List<Map<String, Object>> list = new LinkedList<Map<String, Object>>();
		// 3. Implement pagination
		for( Thing thing : ThingService.getInstance().listPaginated( masterBe, pagination, order ) )
		{
			//thing.getThingTypeFields().get(0).getId()
			// 5a. Implement extra
			Map<String, Object> publicMap = QueryUtils.mapWithExtraFields( thing, extra, getExtraPropertyNames() );
			addToPublicMap(thing, publicMap, extra);
			// 5b. Implement only
			QueryUtils.filterOnly( publicMap, only, extra );
			QueryUtils.filterProjectionNested( publicMap, project, extend);
			list.add( publicMap );
		}
		Map<String, Object> mapResponse = new HashMap<String, Object>();
		mapResponse.put( "total", count );
		mapResponse.put( "results", list );
		return RestUtils.sendOkResponse( mapResponse );
	}


	/**
	 *
	 * IMPORTANT... overwrite of base class because need to pass the group
	 * parameters
	 *
	 */
	@GET
	@Path("/udfForUI/")
	@Produces(MediaType.APPLICATION_JSON)
	// 1a. Limit access based on CLASS level resources
	@RequiresAuthentication
	@RequiresPermissions(value = { "thing:r" })
	@Deprecated
	@ApiOperation(position = 7, value = "Get a List of Things with Udf's codes")
	public Response listThingsWithUdfsCodes( @QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber,
			@QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra,
			@Deprecated @QueryParam("only") String only, @QueryParam("visibilityGroupId") Long visibilityGroupId,
			@DefaultValue("") @QueryParam("upVisibility") String upVisibility,
			@DefaultValue("") @QueryParam("downVisibility") String downVisibility,
			@QueryParam("onlyUdfsCode") String onlyUdfsCode, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project)
	{
		Pagination pagination = new Pagination( pageNumber, pageSize );

		BooleanBuilder masterVisibilityOr = VisibilityThingUtils.limitSelectAllT(upVisibility, downVisibility, visibilityGroupId);

		BooleanBuilder masterBe = new BooleanBuilder( masterVisibilityOr );

		// 4. Implement filtering
		masterBe = masterBe.and( QueryUtils.buildSearch( QThing.thing, where) );

		Long count = ThingService.getInstance().countList( masterBe );
		List<Map<String, Object>> list = new LinkedList<Map<String, Object>>();
		// 3. Implement pagination
		for( Thing thing : ThingService.getInstance().listPaginated( masterBe, pagination, order ) )
		{
			//thing.getThingTypeFields().get(0).getId()
			// 5a. Implement extra
			Map<String, Object> publicMap = QueryUtils.mapWithExtraFields( thing, extra, getExtraPropertyNames() );
			addToPublicUdfsCodesMap( thing, publicMap, extra );
			// 5b. Implement only
			QueryUtils.filterOnly( publicMap, only, extra );
			QueryUtils.filterProjectionNested( publicMap, project, extend);
			list.add( publicMap );
		}
		Map<String, Object> mapResponse = new HashMap<String, Object>();
		mapResponse.put( "total", count );
		mapResponse.put( "results", list );
		return RestUtils.sendOkResponse( mapResponse );
	}


	@GET
	@Path("/user/")
	@Produces(MediaType.APPLICATION_JSON)
	@RequiresAuthentication
	@RequiresPermissions(value = { "thing:r" })
	@ApiOperation(position = 7, value = "Get a List of Users (AUTO)")
	public Response listUsers( @QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber,
							   @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra,
							   @Deprecated @QueryParam("only") String only, @QueryParam("visibilityGroupId") Long visibilityGroupId,
							   @DefaultValue("") @QueryParam("upVisibility") String upVisibility,
							   @DefaultValue("") @QueryParam("downVisibility") String downVisibility, @QueryParam("topId") String topId, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
	{
		calculateThingsVisibility(visibilityGroupId);
		UserController c = new UserController();
        return c.listUsers(pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility, downVisibility,false, extend, project);
	}

	@GET
	@Path("/shift/")
	@Produces(MediaType.APPLICATION_JSON)
	@RequiresAuthentication
	@RequiresPermissions(value = { "thing:r" })
	@ApiOperation(position = 7, value = "Get a List of Shifts (AUTO)")
	public Response listShifts( @QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber,
								@QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra,
								@Deprecated @QueryParam("only") String only, @QueryParam("visibilityGroupId") Long visibilityGroupId,
								@DefaultValue("") @QueryParam("upVisibility") String upVisibility,
								@DefaultValue("") @QueryParam("downVisibility") String downVisibility, @QueryParam("topId") String topId,
								@ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend,
								@ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
	{
		calculateThingsVisibility(visibilityGroupId);
		ShiftController c = new ShiftController();
		return c.listShifts( pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility, downVisibility ,false, extend, project);
	}

	@Override
	public void addToPublicMap( Thing thing, Map<String, Object> publicMap, String extra)
	{
		if( extra != null && extra.contains( "thingType" ) )
		{
			publicMap.put( "thingType", ThingTypeService.getInstance().get( (Long) ((Map) publicMap.get( "thingType" )).get( "id" ) )
					.publicMap( true, true ) );
		}
		if (extra != null && extra.contains("fields")) {
			User currentUser = (User) SecurityUtils.getSubject().getPrincipal();
			DateFormatAndTimeZone dateFormatAndTimeZone = UserService.getInstance().getDateFormatAndTimeZone(currentUser);
			List<Map<String, Object>> fields = getThingFieldMap(thing, dateFormatAndTimeZone);
			Map<String , Object> thingTypeMap = (Map<String , Object>) publicMap.get( "thingType");
			if(thingTypeMap!=null )
			{
				ThingTypeController thingTypeController  =  new ThingTypeController();
				thingTypeController.addToPublicMap(thing.getThingType(),thingTypeMap, null);

				for(Map<String, Object> field : fields)
				{
					for( Map<String, Object> map:  (List<Map<String, Object>>) thingTypeMap.get("fields"))
					{
						if(Long.parseLong(map.get("id").toString()) == Long.parseLong(field.get("id").toString()))
						{
							field.put("thingTypeField", map);
							break;
						}
					}
				}
			}
			publicMap.put("fields", fields);
		}
	}

	/*
	* Get the public map of the thing with codes in Udf's
	* */
	public void addToPublicUdfsCodesMap( Thing thing, Map<String, Object> publicMap, String extra)
	{
		if( extra != null && extra.contains( "thingType" ) )
		{
			publicMap.put( "thingType", ThingTypeService.getInstance().get( (Long) ((Map) publicMap.get( "thingType" )).get( "id" ) )
					.publicMap( true, true ) );
		}
		if (extra != null && extra.contains("fields")) {
			List<Map<String, Object>> fields = getThingFieldCodeMap( thing );
			Map<String , Object> thingTypeMap = (Map<String , Object>) publicMap.get( "thingType");
			if(thingTypeMap!=null )
			{
				ThingTypeController thingTypeController  =  new ThingTypeController();
				thingTypeController.addToPublicMap(thing.getThingType(),thingTypeMap, null);

				for(Map<String, Object> field : fields)
				{
					for( Map<String, Object> map:  (List<Map<String, Object>>) thingTypeMap.get("fields"))
					{
						if(Long.parseLong(map.get("id").toString()) == Long.parseLong(field.get("id").toString()))
						{
							field.put("thingTypeField", map);
							break;
						}
					}
				}
			}
			publicMap.put("fields", fields);
		}
	}

	/**
	 * FILTER LIST
	 */

	@GET
	@Path("/groupType/")
	@Produces(MediaType.APPLICATION_JSON)
	@RequiresAuthentication
	@RequiresPermissions(value = { "thing:r" })
	@ApiOperation(position = 8, value = "Get a List of GroupTypes (AUTO)")
	public Response listGroupTypes( @QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber,
									@QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra,
									@Deprecated @QueryParam("only") String only, @QueryParam("visibilityGroupId") Long visibilityGroupId,
									@DefaultValue("") @QueryParam("upVisibility") String upVisibility,
									@DefaultValue("") @QueryParam("downVisibility") String downVisibility, @QueryParam("topId") String topId, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
	{
		Map<Long, List<ThingType>> map = calculateThingsVisibility( visibilityGroupId );
		GroupTypeController c = new GroupTypeController();
		Group visibilityGroup = calculateUpperVisibilityGroup( map );
		RiotShiroRealm.getOverrideVisibilityCache().put( GroupType.class.getCanonicalName(), visibilityGroup );
        return c.listGroupTypes(pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility, downVisibility, extend, project);
	}

	/**
	 * FILTER TREE
	 */

	@GET
	@Path("/groupType/tree")
	@Produces(MediaType.APPLICATION_JSON)
	@RequiresAuthentication
	@RequiresPermissions(value = { "thing:r" })
	@ApiOperation(position = 8, value = "Get a Tree of GroupTypes (AUTO)")
	public Response listGroupTypesInTree( @QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber,
										  @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra,
										  @Deprecated @QueryParam("only") String only, @QueryParam("visibilityGroupId") Long visibilityGroupId,
										  @DefaultValue("") @QueryParam("upVisibility") String upVisibility,
										  @DefaultValue("") @QueryParam("downVisibility") String downVisibility, @QueryParam("topId") String topId, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
	{
		Map<Long, List<ThingType>> map = calculateThingsVisibility( visibilityGroupId );
		GroupTypeController c = new GroupTypeController();
		Group visibilityGroup = calculateUpperVisibilityGroup( map );
		RiotShiroRealm.getOverrideVisibilityCache().put( GroupType.class.getCanonicalName(), visibilityGroup );
		return c.listGroupTypesInTree(pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility, downVisibility,
				topId, false, extend, project);
	}

	/**
	 * FILTER LIST
	 */

	@GET
	@Path("/group/")
	@Produces(MediaType.APPLICATION_JSON)
	@RequiresAuthentication
	@RequiresPermissions(value = { "thing:r" })
	@ApiOperation(position = 7, value = "Get a List of Groups (AUTO)")
	public Response listGroups( @QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber,
								@QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra,
								@Deprecated @QueryParam("only") String only, @QueryParam("visibilityGroupId") Long visibilityGroupId,
								@DefaultValue("") @QueryParam("upVisibility") String upVisibility,
								@DefaultValue("") @QueryParam("downVisibility") String downVisibility, @QueryParam("topId") String topId, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
	{
		Map<Long, List<ThingType>> map = calculateThingsVisibility( visibilityGroupId);
		GroupController c = new GroupController();
		Group visibilityGroup = calculateUpperVisibilityGroup( map );
		RiotShiroRealm.getOverrideVisibilityCache().put( Group.class.getCanonicalName(), visibilityGroup);
        return c.listGroups(pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility, downVisibility, extend, project);
	}

	/**
	 * FILTER TREE
	 */

	@GET
	@Path("/group/tree")
	@Produces(MediaType.APPLICATION_JSON)
	@RequiresAuthentication
	@RequiresPermissions(value = { "thing:r" })
	@ApiOperation(position = 7, value = "Get a Tree of Groups (AUTO)")
	public Response listGroupsInTree( @QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber,
									  @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra,
									  @Deprecated @QueryParam("only") String only, @QueryParam("visibilityGroupId") Long visibilityGroupId,
									  @DefaultValue("") @QueryParam("upVisibility") String upVisibility,
									  @DefaultValue("") @QueryParam("downVisibility") String downVisibility, @QueryParam("topId") String topId, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
	{
		Map<Long, List<ThingType>> map = calculateThingsVisibility( visibilityGroupId );
		GroupController c = new GroupController();
		Group visibilityGroup = calculateUpperVisibilityGroup( map );
		RiotShiroRealm.getOverrideVisibilityCache().put( Group.class.getCanonicalName(), visibilityGroup );
		return c.listGroupsInTree(pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility, downVisibility, topId, false, extend, project);
	}

	/**
	 * FILTER LIST
	 */

	@GET
	@Path("/thingType/")
	@Produces(MediaType.APPLICATION_JSON)
	@RequiresAuthentication
	@RequiresPermissions(value = { "thing:r" })
	@ApiOperation(position = 7, value = "Get a List of ThingTypes (AUTO)")
	public Response listThingTypes( @QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber,
									@QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra,
									@Deprecated @QueryParam("only") String only, @QueryParam("visibilityGroupId") Long visibilityGroupId,
									@DefaultValue("") @QueryParam("upVisibility") String upVisibility,
									@DefaultValue("") @QueryParam("downVisibility") String downVisibility, @QueryParam("topId") String topId, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
	{
		calculateThingsVisibility(visibilityGroupId);
		ThingTypeController c = new ThingTypeController();
        return c.listThingTypes(pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility, downVisibility, extend, project);
	}

	@GET
	@Path("/thingType/tree")
	@Produces(MediaType.APPLICATION_JSON)
	@RequiresAuthentication
	@RequiresPermissions(value = { "thing:r" })
	@ApiOperation(position = 7, value = "Get a Tree of GroupTypes (AUTO)")
	public Response listThingTypesInTree( @QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber,
										  @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra,
										  @Deprecated @QueryParam("only") String only, @QueryParam("visibilityGroupId") Long visibilityGroupId,
										  @DefaultValue("") @QueryParam("upVisibility") String upVisibility,
										  @DefaultValue("") @QueryParam("downVisibility") String downVisibility, @QueryParam("topId") String topId,
			                              @DefaultValue("false") @QueryParam("enableMultilevel") String enableMultilevel )
	{
		calculateThingsVisibility(visibilityGroupId);
		ThingTypeController c = new ThingTypeController();
		return c.listThingTypesInTree(pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility, downVisibility, enableMultilevel);
	}

	@POST
	@Path("/syncFields")
	@Produces(MediaType.APPLICATION_JSON)
	@RequiresAuthentication
	@RequiresPermissions(value = {"thingType:r"})
	@ApiOperation(position = 7, value = "Sync thing with thing type")
	public Response syncThingFieldsToThingTypeFields( Map<String, Object> body )
	{
		ThingService.getInstance().synchronizeThings(
				ThingTypeService.getInstance().get(((Integer) body.get("thingTypeId")).longValue()));
		return RestUtils.sendOkResponse("Things synchronized");
	}

	@GET
	@Path("/withShift")
	@Produces(MediaType.APPLICATION_JSON)
	// 1a. Limit access based on CLASS level resources
	@RequiresPermissions(value = { "thing:r" })
	@ApiOperation(position = 7, value = "Get a List of Things with the shift information of each thing")
	public Response listThingsWithShifts( @QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber,
										  @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra,
										  @Deprecated @QueryParam("only") String only, @QueryParam("visibilityGroupId") Long visibilityGroupId, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
	{
		Pagination pagination = new Pagination( pageNumber, pageSize );

		BooleanBuilder be = new BooleanBuilder();
		// 2. Limit visibility based on user's group and the object's group
		// (group based authorization)
		Group visibilityGroup = VisibilityUtils.getVisibilityGroup( Thing.class.getCanonicalName(), visibilityGroupId );
		be = be.and( VisibilityUtils.limitVisibilityPredicate( visibilityGroup, QThing.thing.group, false, true ) );
		// 4. Implement filtering
		be = be.and( QueryUtils.buildSearch( QThing.thing, where ) );

		Long count = ThingService.getInstance().countList( be );
		List<Map<String, Object>> list = new LinkedList<Map<String, Object>>();

		ShiftService shiftService = ShiftServiceBase.getInstance();

		// 3. Implement pagination
		for( Thing thing : ThingService.getInstance().listPaginated( be, pagination, order ) )
		{
			// 5a. Implement extra
			Map<String, Object> publicMap = QueryUtils.mapWithExtraFields( thing, extra, getExtraPropertyNames() );
			addToPublicMap( thing, publicMap, extra );
			// 5b. Implement only
			QueryUtils.filterOnly( publicMap, only, extra );
			QueryUtils.filterProjectionNested( publicMap, project, extend);

			// adding shift information
			List<Shift> shifts = shiftService.findAllByThing( thing );
			List<Shift> simpleShifts = new ArrayList<>();
			for( Shift shift : shifts )
			{
				Shift shift0 = new Shift();
				shift0.setId( shift.getId() );
				shift0.setName( shift.getName() );
				shift0.setDaysOfWeek( shift.getDaysOfWeek() );
				shift0.setStartTimeOfDay( shift.getStartTimeOfDay() );
				shift0.setEndTimeOfDay( shift.getEndTimeOfDay() );
				simpleShifts.add( shift0 );
			}
			publicMap.put( "shifts", simpleShifts );

			list.add( publicMap );
		}
		Map<String, Object> mapResponse = new HashMap<String, Object>();
		mapResponse.put( "total", count );
		mapResponse.put( "results", list );
		return RestUtils.sendOkResponse( mapResponse );
	}

	@GET
	@Path("/fields/trueFlags")
	@Produces(MediaType.APPLICATION_JSON)
	@RequiresAuthentication
	@RequiresPermissions(value = { "thing:r" })
	@ApiOperation(position = 6, value = "Get a list of thing fields", notes="Get a list of thing fields with value true, name %flag% and boolean")
	@ApiResponses(
		value = {
			@ApiResponse(code = 200, message = "Ok"),
			@ApiResponse(code = 403, message = "Forbidden"),
			@ApiResponse(code = 500, message = "Internal Server Error")
		}
	)
	public Response listTrueFlagsThingsFields() {
		Group visibilityGroup = VisibilityUtils.getVisibilityGroup(Thing.class.getCanonicalName(), null);
		Long total = ThingService.getInstance().countTrueFlags(visibilityGroup);
		Map<String, Object> mapResponse = new HashMap<String, Object>();
		mapResponse.put("total", total);
		return RestUtils.sendOkResponse(mapResponse);
	}


	/*******************************************
	 *This method is to get a list of thins based on where thing, where fields and fields to list
	 * @param where Condition with the values of thing. Ex: thingTypeCode=Father
	 * @param whereFields Conditions with the values of fields, children and parents. Ex: children.status.value=ACTIVE|children.status.value=INACTIVE
	 * @param fields Fileds to get the list of thing. Ex: serialNumber,name,children.serialNumber,thingRFID.value.serialNumber,children.status.value
	 * @return
	 *******************************************/
	@GET
	@Path("/field")
	@Produces(MediaType.APPLICATION_JSON)
	@RequiresAuthentication
	@RequiresPermissions(value = { "thing:r" })
	@ApiOperation(position = 7, value = "Get a Things with specific fields. Small footprint")
	// TODO implement topId
	public Response listThingsSummarized(
			@QueryParam("where") String where,
			@QueryParam("whereFields") String whereFields,
			@QueryParam("fields") String fields) {
		long timeStamp = System.currentTimeMillis();

		//Get list of fields
		List<String> fieldsFilter = new ArrayList<>(  );
		if(fields!=null && !fields.trim().equals( "" ))
		{
			String[] dataFields = fields.split( "," );
			for( int i = 0; i < dataFields.length; i++ )
			{
				fieldsFilter.add( dataFields[i] );
			}
		}else
		{
			fieldsFilter = Arrays.asList( "*" );
		}
		List<Map<String, Object>> docs = (List)ThingMongoDAO.getInstance().getThingUdfValues(where, whereFields, fieldsFilter, null).get("results");
		logger.info( "Done with getting things in  " + (System.currentTimeMillis() - timeStamp));
		//put in map format
		Map<String, Object> mapResponse = new HashMap<String, Object>();
		mapResponse.put( "total", docs.size() );
		mapResponse.put( "results", docs );

		return RestUtils.sendOkResponse(mapResponse);
	}


	@GET
	@Path("/{id}/zone")
	@Produces(MediaType.APPLICATION_JSON)
	@RequiresAuthentication
	@RequiresPermissions(value = { "thing:r" })
	@ApiOperation(position = 7, value = "Get last zone which thing is")
	public Response getThingLastZone(@PathParam("id") Long id) {
		ReportDefinitionController rdc = new ReportDefinitionController();
		rdc.validateListPermissions();
		Response outcome;

		try{
			outcome = RestUtils.sendOkResponse(ThingService.getInstance().getThingLastZone(id));
		}catch (Exception e){
			outcome = RestUtils.sendBadResponse(e.getMessage());
		}
		return outcome;
	}

	@GET
	@Path("/zone")
	@Produces(MediaType.APPLICATION_JSON)
	@RequiresPermissions(value={"thing:r"})
	@ApiOperation(position = 7, value="Get a List of Zones")
	public Response listThingTypes( @QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber, @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only, @QueryParam("visibilityGroupId") Long visibilityGroupId, @DefaultValue("") @QueryParam("upVisibility") String upVisibility, @DefaultValue("") @QueryParam("downVisibility") String downVisibility , @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
	{
		Map<Long, List<ThingType>> map = calculateThingsVisibility( visibilityGroupId );
		ZoneController c = new ZoneController();
		Group visibilityGroup = calculateUpperVisibilityGroup( map );
		//TODO verify this override
		RiotShiroRealm.getOverrideVisibilityCache().put(Zone.class.getCanonicalName(), visibilityGroup);
		return c.listZones(pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility, downVisibility, false, extend, project);
	}


	@GET
	@Path("/limits")
	@Produces(MediaType.APPLICATION_JSON)
	@RequiresAuthentication
	@RequiresPermissions(value = { "thing:r" })
	@ApiOperation(position=6, value="Get a List of Limits for Things",notes="Get a List of Limits for Things and how many things are left to be created")
	@ApiResponses(
		value = {
			@ApiResponse(code = 200, message = "Ok"),
			@ApiResponse(code = 403, message = "Forbidden"),
			@ApiResponse(code = 500, message = "Internal Server Error")
		}
	)
	public Response verifyObjectLimits()
	{
		User user = (User) SecurityUtils.getSubject().getPrincipal();
		HashMap<String, Number> defaultHM = new HashMap<>();
		defaultHM.put("limit", -1);
		defaultHM.put("used", 0);
		Map<String,Map> mapResponse = new HashMap<String,Map>();
		mapResponse.put("numberOfThings", defaultHM);
		if (LicenseService.enableLicense) {
			LicenseDetail licenseDetail = LicenseService.getInstance().getLicenseDetail(user.getActiveGroup(), true);
			Long maxNumberOfThings = licenseDetail.getMaxThings();
			if (maxNumberOfThings != null && maxNumberOfThings > 0) {
				Long countAll = count(licenseDetail);
				defaultHM.put("limit", maxNumberOfThings);
				defaultHM.put("used", countAll);
			}
		}
		return RestUtils.sendOkResponse(mapResponse);
	}

	//Please do not delete this method is called using reflection
	public static Long count(LicenseDetail licenseDetail) {
		return ThingService.getInstance().count(licenseDetail);
	}


	private List listThingByThingTypeCodeUDFValue(String thingTypeCode, String udfField, String value) {
		String where = "thingTypeCode="+thingTypeCode;
		String whereFieldValue = udfField+".value='"+value+"'|"+ udfField+".value="+value;
		List<String> fields = new ArrayList<>();
		fields.add("serialNumber");
		fields.add("_id");
		Map<String, Object> resultMongo = ThingMongoDAO.getInstance().getThingUdfValues(where, whereFieldValue, fields, null);
		List result = new ArrayList();
		if (((Integer) resultMongo.get("total")).intValue() > 0) {
			for (Map map : (List<Map>) resultMongo.get("results")) {
				result.add(map.get("serialNumber"));
			}
		}
		return result;
	}

	@GET
	@Path("/fieldValues")
	@Produces(MediaType.APPLICATION_JSON)
	@RequiresAuthentication
	@RequiresPermissions(value = { "thing:r" })
	@ApiResponses(
			value = {
					@ApiResponse(code = 200, message = "Ok"),
					@ApiResponse(code = 400, message = "Bad Request"),
					@ApiResponse(code = 500, message = "Internal Server Error")
			}
	)
	@ApiOperation(position = 7, value = "Get things by specific fields. Small footprint")
	/**
	 * whereThing and whereFieldValue accept & | operators
	 */
	public Response listThingsByFieldValuesEndPoint(@ApiParam(value = "It is used to list the selected fields.") @QueryParam("fieldValues") String fieldValues
			,@ApiParam(value = "A filtering parameter for udf values to get specific things. Supported operators: Equals (=), like (~), and (&), different (<>), or (|), in ($in), not in ($nin) ")  @QueryParam("whereThing") String whereThing
			,@ApiParam(value = "A filtering parameter for non udf values to get specific things. Supported operators: Equals (=), like (~), and (&), different (<>), or (|), in ($in), not in ($nin) ")  @QueryParam("whereFieldValue") String whereFieldValued
			,@ApiParam(value = "It is used to group the results of the query.") @QueryParam("groupBy") String groupBy)
	{
		long timeStamp = System.currentTimeMillis();
		Map<String, Object> mapResponse = new HashMap<String, Object>();
		List<String> filterFields = new ArrayList<String>();
		List<String> groupByFields = new ArrayList<String>();

		if (fieldValues == null || fieldValues.trim().isEmpty()) {
			return RestUtils.sendBadResponse( String.format( "'FieldValues' should have a value." ) );
		}
		//TODO: Apply this change when mobile is ready
		/*if( pageNumber !=null && pageNumber.intValue()<1)
		{
				return RestUtils.sendBadResponse( String.format( "'pageNumber' should have a number greater than 0." ) );
		}
		if( pageSize !=null && pageSize.intValue()<1)
		{
			return RestUtils.sendBadResponse( String.format( "'pageSize' should have a number greater than 0." ) );
		}*/

		// lists of filter fields and groupBy fields
		if ( groupBy != null  && !groupBy.isEmpty()) {
			groupByFields = Arrays.asList(StringUtils.split(groupBy, ","));
		} else {
			filterFields = Arrays.asList(StringUtils.split(fieldValues, ","));
		}
		try
		{
			List<Map<String,Object>> result = ThingMongoDAO.getInstance().listThingsByFieldValues(filterFields, groupByFields, whereFieldValued, whereThing);

			logger.info("Done with getting things in  " + (System.currentTimeMillis() - timeStamp));
			//put in map format
			mapResponse.put( "total", result.size() );
			mapResponse.put( "results", result );

		}
		catch (Exception e)
		{
			logger.error(e.getMessage(), e);
			return RestUtils.sendResponseWithCode(e.getMessage() ,400 );
		}
		return RestUtils.sendOkResponse(mapResponse);
	}

	@POST
	@Path("/migrateCassandra")
	//@Produces(MediaType.APPLICATION_JSON)
	//@Consumes(MediaType.APPLICATION_JSON)
	@RequiresAuthentication
	@RequiresPermissions(value = {"thingType:r"})
	@ApiOperation(position = 7, value = "migrate Cassandra")
	public Response migrateCassandra() {
		Response result = null;
		try {
			Method method = Class.forName("com.tierconnect.riot.iot.fixdb.FixDBMigrationCassandra").getMethod("migrateCassandra", String.class);
			method.invoke(null, "sharaf.rfid");
			//FixDBMigrationCassandra.migrateCassandra("sharaf.rfid");
		}
		catch (Exception e) {
			return RestUtils.sendBadResponse("error migrating Cassandra " + e.getMessage());
		}
		return RestUtils.sendOkResponse("success migrating Cassandra");
	}

	@GET
	@Path("/{id}/record")
	@Produces(MediaType.APPLICATION_JSON)
	// 1a. Limit access based on CLASS level resources
	@RequiresAuthentication
	@RequiresPermissions(value = { "thing:r" })
	@ApiOperation(position = 7, value="Get the thing history at specified time with all its fields," +
            " used by pinpopups and table popups")
	public Response getThingRecord( @PathParam("id") Long id,
                                    @QueryParam("time") Long time,
                                    @QueryParam("reportDefId") Long reportDefId,
                                    @QueryParam("comparator") String comparator,
                                    @QueryParam("mode") String mode,
									@Context HttpServletRequest request){
		ReportDefinitionController reportDefinitionController = new ReportDefinitionController();
		reportDefinitionController.validateListPermissions();
		try{

            Map<String, Object> result = null;

			if (reportDefId != null) {
				User user = (User) SecurityUtils.getSubject().getPrincipal();

				DateFormatAndTimeZone dateFormatAndTimeZone = new DateFormatAndTimeZone(
						UserService.getInstance().getValueRegionalSettings(user, Constants.TIME_ZONE_CONFIG),
						UserService.getInstance().getValueRegionalSettings(user, Constants.DATE_FORMAT_CONFIG));

				result = ThingService.getInstance().getThingRecord(id, time, reportDefId, comparator, mode,
						request.getRequestURL().toString(), request.getContextPath(),dateFormatAndTimeZone);
			} else {
                result = ThingService.getInstance().getThingRecord(id, time, comparator);
            }
            logger.debug("RECORD BEGIN\n" + ReportUtils.getPrettyPrint(result) + "\nRECORD END");
            return RestUtils.sendOkResponse(result);
		}catch(UserException e){
			return RestUtils.sendBadResponse(e.getMessage());
		}
	}

	/****Logic of Attachments****/

	@POST
	@Path("/attachment/upload")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	@RequiresAuthentication
	@RequiresPermissions(value = {"thingType:r"})
	@ApiOperation(position=33, value="Upload a File", notes="Upload a file temporarily, the returned ID should be used in the  Create/Update Thing method in a specific UDF of the  'Attachment' type")
	@ApiResponses(
		value = {
			@ApiResponse(code = 200, message = "Ok"),
			@ApiResponse(code = 400, message = "Bad Request"),
			@ApiResponse(code = 403, message = "Forbidden"),
			@ApiResponse(code = 500, message = "Internal Server Error")
		}
	)
	public Response uploadFile(
			  @ApiParam(value = "File to be uploaded.")   MultipartFormDataInput input
			, @ApiParam(value = "Additional comments of the file.")
			  	@QueryParam("comments") String comment
			, @ApiParam(value = "Operation over file, it accepts: 'override'")
				@QueryParam("operationOverFile") String operationOverFile
			, @ApiParam(value = "ID of the thing") @QueryParam("thingId") Long thingId
			, @ApiParam(value = "ID of the thingTypeFieldID") @QueryParam("thingTypeFieldId") Long thingTypeFieldId
			, @ApiParam(value = "Path of Attachments configured in the Thing Type") @QueryParam("pathAttachments") String pathAttachments
			, @ApiParam(value = "Hierarchical name of the group") @QueryParam("hierarchicalNameGroup") String hierarchicalNameGroup
			, @ApiParam(value = "ID's of temporary attachments (Optional)") @QueryParam("attachmentTempIds") String attachmentTempIds
	        ) {

		Map<String, Object> result = null;
		User user = (User) SecurityUtils.getSubject().getPrincipal();
		try{
			//get the folder path of the attachments
			String folderAttachments = AttachmentService.getInstance().getPathDirectory(
					  pathAttachments
					, hierarchicalNameGroup
					, thingId
					, thingTypeFieldId);
			ValidationBean validationBean = ThingTypeService.getInstance()
					.isUploadedAttachmentValid(thingTypeFieldId, input);
			if(validationBean.isError()){
				return RestUtils.sendBadResponse( validationBean.getErrorDescription() );
			}
			//Save in temporary table
			result = AttachmentService.getInstance().saveFileInTempDB(
					  comment
					, user.getId().toString()
					, input, operationOverFile
					, folderAttachments
					, attachmentTempIds);
		}catch(Exception e)
		{
			return RestUtils.sendResponseWithCode(e.getMessage(), 400);
		}
		return RestUtils.sendOkResponse( result );
	}

	@GET
	@Path("/attachment/download")
	@RequiresAuthentication
	@RequiresPermissions(value = { "thing:r" })
	@ApiOperation(position = 34, value="Download a File", notes="Download a File. It returns the content of the file to be downloaded in the UI")
	@ApiResponses(
		value = {
			@ApiResponse(code = 200, message = "Ok"),
			@ApiResponse(code = 400, message = "Bad Request"),
			@ApiResponse(code = 403, message = "Forbidden"),
			@ApiResponse(code = 500, message = "Internal Server Error")
		}
	)
		public Response getFile(@ApiParam(value = "Path of the file, example: D:\\directory\\file.txt")  @QueryParam("pathFile") String pathFile) throws Exception
	{
		AttachmentController c = new AttachmentController();
		return c.getFile( pathFile );
	}

	@GET
	@Path("/attachment/buildJson")
	@RequiresAuthentication
	@RequiresPermissions(value = { "thing:r" })
	@ApiOperation(position = 35, value="Build Json based on the physical path", notes="Build Json based on the physical path.")
	@ApiResponses(
			value = {
					@ApiResponse(code = 200, message = "Ok"),
					@ApiResponse(code = 400, message = "Bad Request"),
					@ApiResponse(code = 403, message = "Forbidden"),
					@ApiResponse(code = 500, message = "Internal Server Error")
			}
	)
	public Response buildJson(@ApiParam(value = "Path where the files are")  @QueryParam("pathFile") String pathFile) throws Exception
	{
		AttachmentController c = new AttachmentController();
		return c.buildJson( pathFile );
	}

	/**** End Logic of Attachments****/

	//TODO:Delete when thingsController is available
	@GET
	@Path("/mongo/")
	@Produces(MediaType.APPLICATION_JSON)
	@RequiresAuthentication
	@RequiresPermissions(value = { "thing:r" })
	@ApiOperation(position = 36, value = "Get things by specific fields.", notes="Query is constructed by udfs in MongoDB.")
	/**
	 * whereThing and whereFieldValue accept & | operators
	 */
	public Response getListOfThings(
			@ApiParam(value = "List of conditions of the thing. Example: [thingTypeCode=asset_code|thingTypeCode=rfid_code] ") @QueryParam("whereThing") String whereThing
			, @ApiParam(value = "List of conditions for fields. Example: [soAsset.value.serialNumber='SO10001'|sorfid.value.serialNumber='SO10001']") @QueryParam("whereFieldValue") String whereFieldValue
			, @ApiParam(value = "Pagination, number (greater than 0) of page to display") @QueryParam("pageNumber") Integer pageNumber
			, @ApiParam(value = "Pagination, quantity (greater than 0) of records to display") @QueryParam("pageSize") Integer pageSize)
	{
		Map<String, Object> mapResponse = new HashMap<String, Object>();
		List<String> filterFields = new ArrayList<String>();
		List<String> groupByFields = new ArrayList<String>();

		if (pageNumber != null && pageNumber < 1) {
			return RestUtils.sendBadResponse("'pageNumber' should have a number greater than 0.");
		}
		if (pageSize != null && pageSize < 1) {
			return RestUtils.sendBadResponse("'pageSize' should have a number greater than 0.");
		}

		String fieldValues = "_id";
		filterFields = Arrays.asList(StringUtils.split(fieldValues, ","));

		try
		{
			List<Map<String, Object>> result = new ArrayList<>(  );
			List<Map<String, Object>> childrenUdf = ThingService.getInstance().listThingsByFieldValues(
					filterFields
					, groupByFields
					, whereFieldValue
					, whereThing
					, pageNumber, pageSize);
			if(childrenUdf !=null && childrenUdf.size()>0 )
			{
				User currentUser = (User) SecurityUtils.getSubject().getPrincipal();
				DateFormatAndTimeZone dateFormatAndTimeZone = UserService.getInstance().getDateFormatAndTimeZone(currentUser);
				logger.info("USER [" + currentUser.getUsername() + "] REGIONAL SETTING " + dateFormatAndTimeZone);
				for(Object childUdf : childrenUdf)
				{
					Map<String, Object> childUdfMap = (Map<String, Object>) childUdf;
					Thing thing = ThingService.getInstance().get( Long.parseLong( childUdfMap.get( "_id" ).toString() ) ) ;
					if(thing!=null)
					{
						Map<String, Object> childrenDataMap = thing.publicMapExtraValues();
						List<Map<String, Object>> chldrenFieldMap = getThingFieldMap(thing, dateFormatAndTimeZone);
						addThingTypeFieldData( thing.getThingType(), chldrenFieldMap );
						childrenDataMap.put( "fields", chldrenFieldMap );
						result.add( childrenDataMap );
					}
				}
			}
			//put in map format
			mapResponse.put( "total", result.size() );
			mapResponse.put( "results", result );
		}
		catch (Exception e)
		{
			logger.error(e.getMessage(), e);
			return RestUtils.sendResponseWithCode(e.getMessage() , 400);
		}
		return RestUtils.sendOkResponse(mapResponse);
	}


	@GET
	@Path("/logicalReader")
	@Produces(MediaType.APPLICATION_JSON)
	// 1a. Limit access based on CLASS level resources
	@RequiresAuthentication
	@ApiOperation(position=1, value="Get a List of LogicalReaders")
	public Response listLogicalReaders( @QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber, @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only, @QueryParam("visibilityGroupId") Long visibilityGroupId, @DefaultValue("") @QueryParam("upVisibility") String upVisibility, @DefaultValue("") @QueryParam("downVisibility") String downVisibility, @DefaultValue("false") @QueryParam("returnFavorite") boolean returnFavorite, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
	{
		LogicalReaderController c = new LogicalReaderController();
		return c.listLogicalReaders(pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility, downVisibility,false, extend, project);
	}
}
