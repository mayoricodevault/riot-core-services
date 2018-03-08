package com.tierconnect.riot.iot.controllers;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.services.ConfigurationService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.iot.entities.Thing;
import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.services.HealthAndStatus;
import com.tierconnect.riot.iot.services.ThingService;
import com.tierconnect.riot.iot.services.ThingTypeService;
import com.tierconnect.riot.iot.services.ThingsService;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.subject.Subject;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

/**
 * Created by pablo on 2/24/15.
 *
 * Controller to associate tags coming from Sharaf. This controller will create or updated existing
 * tags/things on riot
 */


/*
 *  {
  "facilityCode": "Sharaf",
  "thingTypeCode":"Sharaf.thing",
  "Timestamp":"Wed Feb 18 16:30:02 BOT 201",
  "tagID":"1",
  "itemCode":"1",
  "supplier":"Samsung",
  "productDescrip":"It-s jmeter magic",
  "brand":"Samsung",
  "serialNUN":"J000001",
  "status":"Sold"
}
 */

@Path("/associate")
@Api("/associate")
public class AssociateController {
	private static Logger logger = Logger.getLogger(AssociateController.class);

	private final static List<String> status = Arrays.asList(
			"GRN", "PRET", "Sold", "LTI", "LTI_IN", "LTI_OUT", "Refund", "Transfer");

	/**
	 * creates or updates a thing/tag.
	 * @param thingInMap holds all the parameter required to insert or create a tag
	 * @return errorCode: 0 everything was successful. 1 error because attribute missing.
	 * 2 error in attribute format. etc
	 */
//	@POST
//	@Path("/")
//	@Produces(MediaType.APPLICATION_JSON)
//	@Consumes(MediaType.APPLICATION_JSON)
//	@RequiresAuthentication
//	@ApiOperation(position = 1, value = "Insert or update a tag/thing")
//	public Response persist( Map<String, Object> thingInMap )
//	{
//		//get the tag id.
//		Response res;
//
//		//get logged in
//		Subject subject = SecurityUtils.getSubject();
//		User currentUser = (User) subject.getPrincipal();
//
//		try
//		{
//			Map<String, Object> thingMap = clean (thingInMap);
//			String serial = (String) thingMap.get("tagID");
//			ThingType tt = ThingTypeService.getInstance().getByCode((String) thingMap.get("thingTypeCode"));
//			String aStatus = (String) thingMap.get("status");
//			String groupCode = (String) thingMap.get("facilityCode");
//
//			Group group = validate(serial, tt, aStatus, groupCode);
//
//			Thing thing = ThingService.getBySerialNumber(serial, tt);
//			//tag does not exist. create
//            Date modifiedDate = new Date();
//			if (thing == null)
//			{
//				logger.info("Inserting new serial " + serial);
//
//				ThingService.getInstance().insertWithFields( tt, serial, serial, group, currentUser, null,
//						filter( thingMap ), modifiedDate );
//			}
//			//tag does exist. update
//			else
//			{
//				logger.info("Updating serial " + serial);
//				thing.setGroup(group);
//				ThingService.getInstance().update(thing);
//				ThingService.getInstance().updateFields(thing, filter(thingMap), modifiedDate);
//			}
//
//			HealthAndStatus.getInstance().incrementEndpointAssociateCount();
//
//			//response map
//			Map<String, Object> response = new HashMap<>();
//
//			response.put("errorCode", 0);
//			response.put("tagId", serial);
//
//			res = RestUtils.sendCreatedResponse( response );
//		}
//
//		catch (NonUniqueResultException e)
//		{
//			res = handleError(3, e.getMessage());
//		}
//		catch (IllegalArgumentException e) {
//			res = handleError(1, e.getMessage());
//		}
//		catch (Exception e)
//		{
//			res = handleError(2, e.getCause().getMessage());
//		}
//		return res;
//	}

	@POST
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@RequiresAuthentication
	@Deprecated
	@ApiOperation(position = 1, value = "Insert or update a tag/thing")
	public Response persist( Map<String, Object> thingInMap )
	{
		logger.warn("This End Point is going to be deprecated. Please use thing/update End Point");
		ThingService thingService = ThingService.getInstance();
		Map<String, Object> response = new HashMap<>();
		Map<String, Object> responseClient = new HashMap<>();
		Date storageDate = new Date();
		Response res;
		try
		{
			//Construct the map to create a new thing
			Map<String, Object> thingCleanMap = clean( thingInMap );
			String serial = (String) thingCleanMap.get("tagID");
			ThingType thingType = ThingTypeService.getInstance().getByCode( (String) thingCleanMap.get( "thingTypeCode" ) );
			//Validate Data
			validate(serial, thingType, (String) thingInMap.get("status"), (String) thingInMap.get("facilityCode"));
			//Organize the map of data
			Map<String, Object> thingMap = thingService.getMapRequestForAssociate( thingCleanMap, storageDate.getTime() );
			//Call methods of create and update
			Thing thing = ThingService.getInstance().getBySerialNumber( serial, thingType );
			Subject subject = SecurityUtils.getSubject();
			User currentUser = (User) subject.getPrincipal();
			if (thing == null)
			{
				//Create a new Thing
                Stack<Long> recursivelyStack = new Stack<>();
				response = ThingsService.getInstance().create(recursivelyStack
                        ,(String) thingMap.get( "thingTypeCode" )
						,(String) thingMap.get( "group" )
						,(String) thingMap.get( "name" )
						,(String) thingMap.get( "serialNumber" )
						,(Map<String, Object>) thingMap.get( "parent" )
						,(Map<String, Object>) thingMap.get( "udfs" )
						,thingMap.get( "children" )
						,thingMap.get( "childrenUdf" )
						,true, true, storageDate, true, true);
			}
			else {
				//Update Thing
                Stack<Long> recursivelyStack = new Stack<>();
				response = thingService.update(
                        recursivelyStack
                        , thing.getId()
						,(String) thingMap.get( "thingTypeCode" )
						,(String) thingMap.get( "group" )
						,(String) thingMap.get( "name" )
						,(String) thingMap.get( "serialNumber" )
						,(Map<String, Object>) thingMap.get( "parent" )
						,(Map<String, Object>)thingMap.get( "udfs" )
						,thingMap.get( "children" )
						,thingMap.get( "childrenUdf" )
						,true, true, storageDate, false, currentUser, true);
			}

			HealthAndStatus.getInstance().incrementEndpointAssociateCount();
			//response map
			responseClient.put( "errorCode", 0 );
			responseClient.put("tagId", serial);
			res = RestUtils.sendCreatedResponse( responseClient );
		}
		catch (NonUniqueResultException e)
		{
			res = handleError(2, e.getMessage());
		}
		catch (IllegalArgumentException e) {
			res = handleError(1, e.getMessage());
		}
		catch (UserException e) {
			res = handleError(3, e.getMessage());
		}
		catch (Exception e)
		{
			res = handleError(2, e.getCause().getMessage());
		}
		return res;
	}

	@POST
	@Path("/HH/")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@RequiresAuthentication
	@Deprecated
	@ApiOperation(position = 1, value = "Insert or update a tag/thing")
	public Response persistHH( List<Map<String, Object> > thingInMapList )
	{
		logger.warn("This End Point is going to be deprecated. Please use thing/update End Point");
		ThingService thingService = ThingService.getInstance();
		Map<String, Object> response = new HashMap<>();
		Date storageDate = new Date();
		Response res = null;
		Subject subject = SecurityUtils.getSubject();
		User currentUser = (User) subject.getPrincipal();

		try
		{
			for(Map<String, Object> thingInMap : thingInMapList) {
				//Construct the map to create a new thing
				Map<String, Object> thingCleanMap = clean( thingInMap );
				String serial = (String) thingCleanMap.get("serial");
				ThingType thingType = ThingTypeService.getInstance().getByCode( (String) thingCleanMap.get( "thingTypeCode" ) );
				//Validations
				validateHH(serial, thingType, (String) thingInMap.get("status"), (String) thingInMap.get("facilityCode"));
				//Construct the map for Create/Update Method
				Map<String, Object> thingMap = thingService.getMapRequestForAssociateHH( thingCleanMap, storageDate.getTime() );
				//Call methods of create and update
				Thing thing = ThingService.getInstance().getBySerialNumber( serial, thingType );
				if (thing == null)
				{
					//Create a new Thing
                    Stack<Long> recursivelyStack = new Stack<>();
					response = ThingsService.getInstance().create(
                            recursivelyStack
                            , (String) thingMap.get( "thingTypeCode" )
							,(String) thingMap.get( "group" )
							,(String) thingMap.get( "name" )
							,(String) thingMap.get( "serialNumber" )
							,(Map<String, Object>) thingMap.get( "parent" )
							,(Map<String, Object>)thingMap.get( "udfs" )
							,thingMap.get( "children" )
							,thingMap.get( "childrenUdf" )
							,true, true, storageDate, true, true);
				}
				else {
					//Update Thing
                    Stack<Long> recursivelyStack = new Stack<>();
					response = thingService.update(
                            recursivelyStack
                            ,thing.getId()
							,(String) thingMap.get( "thingTypeCode" )
							,(String) thingMap.get( "group" )
							,(String) thingMap.get( "name" )
							,(String) thingMap.get( "serialNumber" )
							,(Map<String, Object>) thingMap.get( "parent" )
							,(Map<String, Object>) thingMap.get( "udfs" )
							,thingMap.get( "children" )
							,thingMap.get( "childrenUdf" )
							,true, true, storageDate, false, currentUser, true);
				}

				//response map
				response.put( "errorCode", 0 );
				response.put("tagId", serial);
				res = RestUtils.sendCreatedResponse( response );
			}
		}
		catch (NonUniqueResultException e)
		{
			res = handleError(2, e.getMessage());
		}
		catch (IllegalArgumentException e) {
			res = handleError(1, e.getMessage());
		}
		catch (UserException e) {
			res = handleError(3, e.getMessage());
		}
		catch (Exception e)
		{
			res = handleError(2, e.getCause().getMessage());
		}
		return res;
	}

	/**
	 * Filter out tagID, and thingType code and validate status values
	 * @param values all in values
	 * @return just udfs
	 */
	private Map<String, Object> filter(Map<String, Object> values) throws IllegalArgumentException
	{
		//put all the udfs on a key called "fields"
		Map<String, Object> filtered = new HashMap<>(values);
		filtered.remove( "tagID" );
		filtered.remove( "thingTypeCode" );

		return filtered;
	}


	private Group validate(String serial, ThingType tt, String aStatus, String groupCode) throws Exception {
		Group group = null;

		// validate serial
		if(StringUtils.isEmpty(serial))
		{
			throw new IllegalArgumentException("missing tagID");
		}
		if(StringUtils.indexOf(serial,",") >= 0)
		{
			throw new IllegalArgumentException("invalid character in tagID");
		}

		// validate thing type
		if(tt == null)
		{
			throw new IllegalArgumentException("missing or invalid thingTypeCode");
		}

		//validate status correct format
		if(!status.contains(aStatus))
		{
			throw new IllegalArgumentException("Invalid status " + aStatus);
		}

		// validate group code
		if (null == groupCode || groupCode.isEmpty())
			throw new IllegalArgumentException("facilityCode cannot be null or empty");
		try
		{
			group = GroupService.getGroupDAO().selectBy("code",groupCode);
			if (null == group)
				throw new IllegalArgumentException("Invalid facility code: " + groupCode);
		}
		catch (org.hibernate.NonUniqueResultException e)
		{
			throw new IllegalArgumentException("There are more than one group with the same code: " + groupCode);
		}

		// validate group level
		User currentUser = (User) SecurityUtils.getSubject().getPrincipal();
		Long userLevel = ConfigurationService.getAsLong(currentUser, group, "thing");
		int thingLevel = group.getTreeLevel();
		if (userLevel != thingLevel)
			throw new IllegalArgumentException ("The group's level is " + thingLevel + " and it has to be " + userLevel);
		return  group;
	}

	private Group validateHH(String serial, ThingType tt, String aStatus, String groupCode) throws Exception {
		Group group = null;

		// validate serial
		if(StringUtils.isEmpty(serial))
		{
			throw new IllegalArgumentException("missing serial");
		}
		if(StringUtils.indexOf(serial,",") >= 0)
		{
			throw new IllegalArgumentException("invalid character in tagID");
		}

		// validate thing type
		if(tt == null)
		{
			throw new IllegalArgumentException("missing or invalid thingTypeCode");
		}

//        //validate status correct format
//        String aStatus = (String) values.get("status");
//        if(!status.contains(aStatus))
//        {
//            throw new IllegalArgumentException("Invalid status " + aStatus);
//        }

		// validate group code
		if (null == groupCode || groupCode.isEmpty())
			throw new IllegalArgumentException("facilityCode cannot be null or empty");
		try
		{
			group = GroupService.getGroupDAO().selectBy("code",groupCode);
			if (null == group)
				throw new IllegalArgumentException("Invalid facility code: " + groupCode);
		}
		catch (org.hibernate.NonUniqueResultException e)
		{
			throw new IllegalArgumentException("There are more than one group with the same code: " + groupCode, e);
		}

		// validate group level
		User currentUser = (User) SecurityUtils.getSubject().getPrincipal();
		Long userLevel = ConfigurationService.getAsLong(currentUser, group, "thing");
		int thingLevel = group.getTreeLevel();
		if (userLevel != thingLevel)
			throw new IllegalArgumentException ("The group's level is " + thingLevel + " and it has to be " + userLevel);
		return group;
	}

	/**
	 * eliminates spaces at the beginning and end in all fields
	 * @param thingMap
	 */
	private Map<String, Object> clean ( Map<String, Object> thingMap )
	{
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		for (Map.Entry<String, Object> entry : thingMap.entrySet())
		{
			String key = entry.getKey().trim();
			Object value = entry.getValue();
			if (entry.getValue() instanceof String)
			{
				value = ((String) entry.getValue()).trim();
			}
			result.put(key,value);
		}
		return result;
	}

	private Response handleError (int code, String message)
	{
		//response map
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("errorCode", code);
		if (code == 0) {
			response.put("tagId", message);
		}
		else {
			response.put("errorMessage", message);
		}

		return Response.status( 400 ).header("content-type", "application/json").entity(response).build();

	}
}
