package com.tierconnect.riot.iot.controllers;

import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.iot.entities.BackgroundProcessDetailLog;
import com.tierconnect.riot.iot.entities.Thing;
import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.entities.ValidationBean;
import com.tierconnect.riot.iot.reports.views.things.ThingListExecutor;
import com.tierconnect.riot.iot.reports.views.things.dto.ListResult;
import com.tierconnect.riot.iot.services.BackgroundProcessDetailLogService;
import com.tierconnect.riot.iot.services.ThingService;
import com.tierconnect.riot.iot.services.ThingTypeService;
import com.tierconnect.riot.iot.services.ThingsService;
import com.tierconnect.riot.iot.services.ValidatorService;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.wordnik.swagger.annotations.*;
import com.wordnik.swagger.jaxrs.PATCH;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.subject.Subject;
import org.hibernate.FlushMode;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

/**
 * Created by fflores on 12/28/2015.
 */
@Path("/things")
@Api("/things")
public class ThingsController {
    private static Logger logger = Logger.getLogger(ThingsController.class);

    /**
     * This method creates an instance of thing and associations with parent and children
     * @param thingMap
     * @param useDefaultValue
     * @return
     */
    @PUT
    @Path("/newThing/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @RequiresPermissions(value={"thingType:i"})
    @ApiOperation(value = "Create Thing",
            position = 4,
            notes = "This method creates an instance of thing and associations with parent and children.<br>"
                    + "<font face=\"courier\">{ <br>&nbsp;&nbsp;\"group\": \">GCODE1>GCODE2\", \n"
                    + "<br>&nbsp;&nbsp;\"thingTypeCode\": \"default_rfid_thingtype\",\n"
                    + "<br>&nbsp;&nbsp;\"name\": \"TP10049\",\n" + "  <br>&nbsp;&nbsp;\"serialNumber\": \"TP10049\",  \n"
                    + "<br>&nbsp;&nbsp;&nbsp;\"time\": \"123456789\",\n"
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
            , @QueryParam("useDefaultValue") Boolean useDefaultValue )
    {
        Map<String, Object> result = null;
        try{
            ThingsService thingsService =  ThingsService.getInstance();
            Date storageDate = ThingService.getInstance().getDate( thingMap.get("time")!=null?(String)thingMap.get("time"): null);
            //Create a new Thing
            Stack<Long> recursivelyStack = new Stack<>();
            result = thingsService.create(
                    recursivelyStack,
                    (String) thingMap.get("thingTypeCode"),
                    (String) thingMap.get("group"),
                    (String) thingMap.get("name"),
                    (String) thingMap.get("serialNumber"),
                    (Map<String, Object>) thingMap.get("parent"),
                    (Map<String, Object>) thingMap.get("udfs"),
                    thingMap.get("children"),
                    thingMap.get("childrenUdf"),
                    true, //executeTickle
                    true, //validateVisibility
                    storageDate,
                    true, //disableFMCLogic
                    true, //createAndFlush
                    useDefaultValue,
                    null, // validations
                    null, // facilityMapId
                    true  ,
                    null); //fillSource
        }catch(UserException e){
            return RestUtils.sendResponseWithCode(e.getMessage() , 400);
        }catch(Exception e){
            logger.error(e.getMessage(),e);
            return RestUtils.sendResponseWithCode(e.getMessage() , 400);
        }
        return RestUtils.sendOkResponse(result);
    }

    /***************************************
     * @method createBulkThing
     * @description This method creates a bulk of thing and associations with parent and children
     * @params: Format Json:
     *************************************************/
    @PUT
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @RequiresPermissions(value={"thingType:i"})
    @ApiOperation(value = "Create Thing",
            position = 4,
            notes = "This method creates a bulk of thing and associations with parent and children.<br>"
                    + "<font face=\"courier\">{ <br>&nbsp;&nbsp;\"group\": \">GCODE1>GCODE2\", \n"
                    + "<br>&nbsp;&nbsp;\"thingTypeCode\": \"default_rfid_thingtype\",\n"
                    + "<br>&nbsp;&nbsp;\"name\": \"TP10049\",\n" + "  <br>&nbsp;&nbsp;\"serialNumber\": \"TP10049\",  \n"
                    + "<br>&nbsp;&nbsp;\"time\":\"123456789\",\n"
                    + "<br>&nbsp;&nbsp;\"parent\": {\n" + "    <br>&nbsp;&nbsp;&nbsp;\"serialNumber\": \"J00001\",\n"
                    + "<br>&nbsp;&nbsp;&nbsp;\"thingTypeCode\": \"jackets_code\"\n" + "  <br>&nbsp;&nbsp;},\n"
                    + "<br>&nbsp;&nbsp;\"udfs\": {\n" + "    <br>&nbsp;&nbsp;&nbsp;\"name\": {\n"
                    + "<br>&nbsp;&nbsp;&nbsp;\"value\": \"General Motors\"\n"
                    + "<br>&nbsp;&nbsp;&nbsp;    <br>&nbsp;&nbsp;&nbsp;},\n"
                    + "<br>&nbsp;&nbsp;&nbsp;\"Phone\": {\n" + "      <br>&nbsp;&nbsp;&nbsp;\"value\":\"7789564\"\n"
                    + "<br>&nbsp;&nbsp;&nbsp;    <br>&nbsp;&nbsp;&nbsp;}     \n"
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
    public Response createBulkThing(
            @ApiParam(value = "serialNumber and name required<br>" +
                    "\"group\" and \"time\" are optional fields in the JSON Request.")  List<Map<String, Object>> thingList
            , @QueryParam("useDefaultValue") Boolean useDefaultValue,
            @QueryParam("requestClient") @ApiParam(value = "Client Identification.") String requestClient,
            @QueryParam("verboseResult") @ApiParam(value = "Returns ") @DefaultValue("true") boolean verboseResult)
    {
        Map<String, Object> result = new HashMap<>();
        try{

            for(Map<String,Object> thingMap : thingList){
                Date storageDate = ThingService.getInstance().getDate( thingMap.get("time")!=null?(String)thingMap.get("time"): null);
                //Update a new Thing
                try{
                    Stack<Long> recursivelyStack = new Stack<>();
                    String facilityCode = null;
                    if (thingMap.containsKey("facilityCode")) {
                        facilityCode = thingMap.get("facilityCode").toString().trim();
                    }
                    Map resObject = ThingsService.getInstance().create(
                            recursivelyStack
                            , (String) thingMap.get("thingTypeCode")
                            , (String) thingMap.get("group")
                            , (String) thingMap.get("name")
                            , (String) thingMap.get("serialNumber")
                            , (Map<String, Object>) thingMap.get("parent")
                            , (Map<String, Object>) thingMap.get("udfs")
                            , thingMap.get("children")
                            , thingMap.get("childrenUdf")
                            , true
                            , true
                            , storageDate
                            , useDefaultValue
                            , facilityCode
                            , true
                    );
                    if (verboseResult){
                        result.put(thingMap.get("serialNumber").toString(), resObject);
                    }else{
                        Map<String, Object> thingResult = new HashMap<>();
                        thingResult.put("SUCCESS", "Thing " + thingMap.get("serialNumber") + " created successfully.");
                        result.put(thingMap.get("serialNumber").toString(), thingResult);
                    }
                }catch (Exception et) {

                    //TODO this is just a hack for mobile, not a feature by GusR request
                    if (((Map<String, Object>) thingMap.get("udfs")).containsKey("source")
                            && ((Map) ((Map) thingMap.get("udfs")).get("source")).get("value").toString().equalsIgnoreCase("mobile")) {

                        String serialNumber = "";
                        if (thingMap.containsKey("serialNumber")) {
                            serialNumber = thingMap.get("serialNumber").toString();
                        }

                        Map<String, Object> thingResult = new HashMap<>();
                        thingResult.put("SUCCESS", "Thing " + serialNumber + " created successfully.");
                        result.put(serialNumber, thingResult);

                        StringWriter sw = new StringWriter();
                        et.printStackTrace(new PrintWriter(sw));

                        logger.warn("****Error occurred while updating thing=" + serialNumber + " thingMap=" + thingMap.toString()
                                + " exceptionDetaild=" + et.getCause() + " exceptionDetail=" + sw.toString());
                    } else {
                        Map<String, Object> thingResult = new HashMap<>();
                        thingResult.put("ERROR", et.getMessage());
                        result.put(thingMap.get("serialNumber").toString(), thingResult);
                    }

                }
            }

        }catch(UserException e){
            return RestUtils.sendResponseWithCode(e.getMessage() , 400);
        }
        return RestUtils.sendOkResponse(result);
    }

    /***************************************
     * @method updateBulkThing
     * @description This method updates a bulk of things and their associations with parent and children
     * @params: Format Json:
     *************************************************/
    @PATCH
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @RequiresPermissions(value={"thingType:u"})
    @ApiOperation(value = "Bulk Update Thing",
            position = 5,
            notes = "This method update or create (if use query param upsert in true) a bulk of things and their associations with parent and children.<br><font " +
                    "face=\"courier\">[<br>{ <br>&nbsp;&nbsp;\"group\": \">GCODE1>GCODE2\", \n" +
                    "<br>&nbsp;&nbsp;\"thingTypeCode\": \"default_rfid_thingtype\",\n" +
                    "<br>&nbsp;&nbsp;\"name\": \"TP10049\",\n" +
                    "  <br>&nbsp;&nbsp;\"id\": \"59\",  \n" +
                    "  <br>&nbsp;&nbsp;\"serialNumber\": \"TP10049\",  \n" +
                    "  <br>&nbsp;&nbsp;&nbsp;\"time\": \"123456789\",\n" +
                    "<br>&nbsp;&nbsp;\"parent\": {\n" +
                    "    <br>&nbsp;&nbsp;&nbsp;\"serialNumber\": \"J00001\",\n" +
                    "<br>&nbsp;&nbsp;&nbsp;\"thingTypeCode\": \"jackets_code\"\n" +
                    "  <br>&nbsp;&nbsp;},\n" +
                    "<br>&nbsp;&nbsp;\"udfs\": {\n" +
                    "    <br>&nbsp;&nbsp;&nbsp;\"name\": {\n" +
                    "<br>&nbsp;&nbsp;&nbsp;\"value\": \"General Motors\"\n" +
                    "    <br>&nbsp;&nbsp;&nbsp;},\n" +
                    "<br>&nbsp;&nbsp;&nbsp;\"Phone\": {\n" +
                    "      <br>&nbsp;&nbsp;&nbsp;\"value\":\"7789564\"\n" +
                    "    <br>&nbsp;&nbsp;&nbsp;}     \n" +
                    "<br>&nbsp;&nbsp;},\n" +
                    "  <br>&nbsp;&nbsp;\"children\": [\n" +
                    "  <br>&nbsp;&nbsp;  {\n" +
                    "<br>&nbsp;&nbsp;&nbsp;\"serialNumber\": \"000000000000000000474\",\n" +
                    "<br>&nbsp;&nbsp;&nbsp;\"thingTypeCode\": \"default_rfid_thingtype\",\n" +
                    "<br>&nbsp;&nbsp;&nbsp;\"udfs\": \n" +
                    "      <br>&nbsp;&nbsp;&nbsp;{\n" +
                    "<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\"name\": {\n" +
                    "<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\"value\": \"GENERAL\"\n" +
                    "<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;}\n" +
                    "<br>&nbsp;&nbsp;&nbsp;}&nbsp;&nbsp;}&nbsp;&nbsp;]&nbsp;&nbsp;},<br>{ <br>&nbsp;&nbsp;\"group\": " +
                    "\">GCODE1>GCODE2\", \n" +
                    "<br>&nbsp;&nbsp;\"thingTypeCode\": \"default_rfid_thingtype\",\n" +
                    "<br>&nbsp;&nbsp;\"name\": \"TP10049\",\n" +
                    "  <br>&nbsp;&nbsp;\"id\": \"50\",  \n" +
                    "  <br>&nbsp;&nbsp;\"serialNumber\": \"TP10050\",  \n" +
                    "  <br>&nbsp;&nbsp;&nbsp;\"time\": \"123456789\",  \n" +
                    "<br>&nbsp;&nbsp;\"parent\": {\n" +
                    "    <br>&nbsp;&nbsp;&nbsp;\"serialNumber\": \"J00002\",\n" +
                    "<br>&nbsp;&nbsp;&nbsp;\"thingTypeCode\": \"jackets_code\"\n" +
                    "  <br>&nbsp;&nbsp;},\n" +
                    "<br>&nbsp;&nbsp;\"udfs\": {\n" +
                    "    <br>&nbsp;&nbsp;&nbsp;\"name\": {\n" +
                    "<br>&nbsp;&nbsp;&nbsp;\"value\": \"Another Motors\"\n" +
                    "    <br>&nbsp;&nbsp;&nbsp;},\n" +
                    "<br>&nbsp;&nbsp;&nbsp;\"Phone\": {\n" +
                    "      <br>&nbsp;&nbsp;&nbsp;\"value\":\"7789565\"\n" +
                    "    <br>&nbsp;&nbsp;&nbsp;}     \n" +
                    "<br>&nbsp;&nbsp;},\n" +
                    "  <br>&nbsp;&nbsp;\"children\": [\n" +
                    "  <br>&nbsp;&nbsp;  {\n" +
                    "<br>&nbsp;&nbsp;&nbsp;\"serialNumber\": \"000000000000000000475\",\n" +
                    "<br>&nbsp;&nbsp;&nbsp;\"thingTypeCode\": \"default_rfid_thingtype\",\n" +
                    "<br>&nbsp;&nbsp;&nbsp;\"udfs\": \n" +
                    "      <br>&nbsp;&nbsp;&nbsp;{\n" +
                    "<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\"name\": {\n" +
                    "<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\"value\": \"ANOTHER\"\n" +
                    "<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;}\n" +
                    "<br>&nbsp;&nbsp;&nbsp;}&nbsp;&nbsp;}&nbsp;&nbsp;]&nbsp;&nbsp;}<br>]</font>")
    @ApiResponses(
            value = {
                    @ApiResponse(code = 400, message = "Bad Request"),
                    @ApiResponse(code = 200, message = "Ok"),
                    @ApiResponse(code = 403, message = "Forbidden"),
                    @ApiResponse(code = 500, message = "Internal Server Error")
            }
    )
    public Response updateBulkThing(@ApiParam(value = "serialNumber or id required (at least one of them)" +
                                "\"group\" and \"time\" is an optional field in the JSON Request."
                                +
                                "<br>children=null: Do not modify any children."
                                +
                                "<br>children=[]: Disassociate all children of the thing."
                                +
                                "<br>children=[{...}]: Assign these new children and replace the old ones in the thing.")
                                        List<Map<String, Object>> thingList,
                                    @QueryParam("requestClient") @ApiParam(value = "Client Identification.") String requestClient,
                                    @QueryParam("verboseResult") @ApiParam(value = "Returns ") @DefaultValue("true") boolean verboseResult,
                                    @QueryParam("useDefaultValue") @DefaultValue("true") boolean useDefaultValue,
                                    @QueryParam("upsert")
                                        @ApiParam(value = "It is used to enable the upsert. It can have True or False values") @DefaultValue("false") boolean upsert) {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        try{
            FlushMode flushMode = HibernateSessionFactory.getInstance().getCurrentSession().getFlushMode();
            HibernateSessionFactory.getInstance().getCurrentSession().setFlushMode(FlushMode.COMMIT);
            Subject subject = SecurityUtils.getSubject();
            User currentUser = (User) subject.getPrincipal();
            // service Create thing
            ThingsService thingsService = ThingsService.getInstance();
            // service Update thing
            ThingService thingService = ThingService.getInstance();

            Map<String, Object> thingTypeResult;
            for (Map<String, Object> thingMap : thingList) {

                thingTypeResult = new HashMap<>();
                //TODO move this to ThingService
                ValidationBean validationBean;
                String identifier = "INDEXOF|" + thingList.indexOf(thingMap);
                try {
                    if (!thingMap.containsKey("serialNumber") && !thingMap.containsKey("id")) {
                        thingTypeResult.put("ERROR", "Id or serialNumber field required.");
                        result.put(identifier, thingTypeResult);
                        continue; // to next thing
                    }
                    validationBean = validateThingTypeCode(thingMap);
                    if (validationBean.isError()) {
                        if (thingMap.containsKey("serialNumber")) {
                            identifier = thingMap.get("serialNumber").toString();
                        } else if (thingMap.containsKey("id")) {
                            identifier = thingMap.get("id").toString();
                        }
                        thingTypeResult.put("ERROR", validationBean.getErrorDescription());
                        result.put(identifier, thingTypeResult);
                        continue; // to next thing
                    }
                } catch (NonUniqueResultException e) {
                    return RestUtils.sendResponseWithCode("More than one thingType found, details" + e.getMessage(), 400);
                }

                Thing thing = null;
                try{
                    if (thingMap.containsKey("id") && StringUtils.isNumeric(thingMap.get("id").toString())) {
                        identifier = thingMap.get("id").toString();
                        thing = ThingService.getInstance().get(Long.valueOf(thingMap.get("id").toString()));
                    } else if (thingMap.containsKey("serialNumber")) {
                        identifier = thingMap.get("serialNumber").toString();
                        thing = ThingService.getInstance().
                                getBySerialAndThingTypeCode(
                                        thingMap.get("serialNumber").toString(),
                                        thingMap.get("thingTypeCode").toString());
                    }
                    if (thing == null && !upsert) {
                        thingTypeResult.put("ERROR", "Upsert disabled, is not possible create thing ");
                        result.put(identifier, thingTypeResult);
                        continue; // to next thing
                    }
                }catch (NonUniqueResultException e) {
                    return RestUtils.sendResponseWithCode("More than one thing found, details" + e.getMessage(), 400);
                }

                validationBean = ValidatorService
                        .validateValuesIsNotEmpty((Map<String, Object>) thingMap.get("udfs"));

                if (validationBean.isError()) {
                    Map<String, Object> thingResult = new HashMap<>();
                    thingResult.put("ERROR", validationBean.getErrorDescription());
                    result.put(identifier, thingResult);
                } else {
                    Date storageDate = ThingService.getInstance().getDate(thingMap.get("time") != null ? (String) thingMap.get("time") : null);
                    Stack<Long> recursivelyStack = new Stack<>();
                    String message = "Thing %s is processing ";
                    try {
                        Map<String, Object> resultThing;
                        if (thing == null) {
                            message = "Thing %s created successfully.";
                            String facilityCode = null;
                            if (thingMap.containsKey("facilityCode")) {
                                facilityCode = thingMap.get("facilityCode").toString().trim();
                            }
                            resultThing = thingsService.create(recursivelyStack
                                    , (String) thingMap.get("thingTypeCode")
                                    , (String) thingMap.get("group")
                                    , (String) thingMap.get("name")
                                    , (String) thingMap.get("serialNumber")
                                    , (Map<String, Object>) thingMap.get("parent")
                                    , (Map<String, Object>) thingMap.get("udfs")
                                    , thingMap.get("children")
                                    , thingMap.get("childrenUdf")
                                    , true
                                    , true
                                    , storageDate
                                    , useDefaultValue
                                    , facilityCode
                                    , true);
                        } else {
                            message = "Thing %s updated successfully.";
                            resultThing = thingService.update(
                                    recursivelyStack
                                    , thing
                                    , (String) thingMap.get("thingTypeCode")
                                    , (String) thingMap.get("group")
                                    , (String) thingMap.get("name")
                                    , (String) thingMap.get("serialNumber")
                                    , (Map<String, Object>) thingMap.get("parent")
                                    , (Map<String, Object>) thingMap.get("udfs")
                                    , thingMap.get("children")
                                    , thingMap.get("childrenUdf")
                                    , true, true, storageDate, false, null, null, false, true, currentUser, true);

                        }
                        String serialNumber = ((Map) resultThing.get("thing")).get("serial").toString();
                        if (verboseResult) {
                            result.put(serialNumber, resultThing);
                        } else {
                            Map<String, Object> thingResult = new HashMap<>();
                            thingResult.put("SUCCESS", String.format(message, serialNumber));
                            result.put(serialNumber, thingResult);
                        }

                    } catch (Exception et) {
                        if (((Map<String, Object>) thingMap.get("udfs")).containsKey("source")
                                && ((Map) ((Map) thingMap.get("udfs")).get("source")).get("value").toString().equalsIgnoreCase("mobile")) {

                            //TODO this is just a hack for mobile, not a feature by GusR request
                            String serialNumber = "";
                            if (thingMap.containsKey("serialNumber")) {
                                serialNumber = thingMap.get("serialNumber").toString();
                            }

                            Map<String, Object> thingResult = new HashMap<>();
                            thingResult.put("SUCCESS", String.format(message, serialNumber));
                            result.put(serialNumber, thingResult);

                            StringWriter sw = new StringWriter();
                            et.printStackTrace(new PrintWriter(sw));

                            logger.warn("****Error occurred while updating thing=" + serialNumber + " thingMap=" + thingMap.toString()
                                    + " exceptionCause=" + et.getCause() + " exceptionDetail=" + sw.toString());
                        } else {
                            Map<String, Object> thingResult = new HashMap<>();
                            thingResult.put("ERROR", et.getMessage());
                            result.put(identifier, thingResult);
                        }
                    }
                }
            }//End for
            HibernateSessionFactory.getInstance().getCurrentSession().setFlushMode(flushMode);
        }catch(UserException e){
            return RestUtils.sendResponseWithCode(e.getMessage(), 400);
        }

        return RestUtils.sendOkResponse(result);

    }

    /**
     * Validate if thingTypeCode is valid in a thingMap
     * @param thingMap Json that contain a thing
     * @return validation for thingTypeCode
     */
    //rsejas: Todo: Move this validation to ThingService or ThingsService
    private ValidationBean validateThingTypeCode(Map<String, Object> thingMap) throws NonUniqueResultException {
        ValidationBean validationBean = new ValidationBean();
        if (!thingMap.containsKey("thingTypeCode")) {
            validationBean.setErrorDescription("'thingTypeCode' not found in Json");
            return validationBean;
        }
        String thingTypeCode = thingMap.get("thingTypeCode").toString();
        ThingType thingType = ThingTypeService.getInstance().getByCode(thingTypeCode);
        if (thingType == null) {
            validationBean.setErrorDescription("thingTypeCode '" + thingTypeCode + "' not found.");
        }
        return validationBean;
    }


    @POST
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @RequiresPermissions(value = {"thingType:u"})
    @ApiOperation(value = "Update UDF values",
            position = 2,
            notes = "Example JSON:<br>" + "<font face=\"courier\">[\n" +
                    "<br>  {\n" +
                    "<br>    \"udfs\": {\n" +
                    "<br>      \"size\": {\n" +
                    "<br>        \"value\": \"XXL\"\n" +
                    "<br>      }\n" +
                    "<br>    },\n" +
                    "<br>    \"whereThing\": \"serialNumber=J00001\",\n" +
                    "<br>    \"whereFieldValue\": \"color.value=Gray\",\n" +
                    "<br>    \"returnThings\": true\n" +
                    "<br>  },\n" +
                    "<br>  {\n" +
                    "<br>    \"udfs\": {\n" +
                    "<br>      \"size\": {\n" +
                    "<br>        \"value\": \"XXL\"\n" +
                    "<br>      }\n" +
                    "<br>    },\n" +
                    "<br>    \"whereThing\": \"serialNumber=J00002\",\n" +
                    "<br>    \"whereFieldValue\": \"color.value=Black\",\n" +
                    "<br>    \"returnThings\": true\n" +
                    "<br>  }\n" +
                    "<br>]</font>")
    @ApiResponses(
            value = {
                    @ApiResponse(code = 200, message = "Ok"),
                    @ApiResponse(code = 400, message = "Bad Request")
            }
    )
    /**
     * whereThing and whereFieldValue accept & | operators
     */
    public Response updateUDFValues(
            @QueryParam("async")
            @ApiParam(value = "Services won't save in mysql and mongo, it will just send tickles.") @DefaultValue("false") boolean async,
            @QueryParam("batchSize") @ApiParam(value = "Batch size for async process, only works if async parameter is equals true") @DefaultValue("50") int batchSize,
            @QueryParam("bridgeCode") @ApiParam(value = "Bridge code to send async tickles to, only works if async parameter is equals true") String bridgeCode,
            List<Map<String, Object>> inList) {
        long timeStamp = System.currentTimeMillis();
        List<Map<String, Object>> listResponse = new ArrayList<>();
        Subject subject = SecurityUtils.getSubject();
        User currentUser = (User) subject.getPrincipal();
        Date storageDate = new Date();

        try {
            if (null == inList || inList.isEmpty())
                return RestUtils.sendBadResponse("Invalid input parameters");

            for (Map<String, Object> inMap : inList) {

                String whereThing = inMap.get("whereThing") != null ? (String) inMap.get("whereThing") : null;
                String whereFieldValue = inMap.get("whereFieldValue") != null ? (String) inMap.get("whereFieldValue") : null;
                Map<String, Object> udfs = inMap.get("udfs") != null ? (Map) inMap.get("udfs") : null;
                boolean returnThings = inMap.get("returnThings") != null ? (boolean) inMap.get("returnThings") : false;

                Map<String, Object> mapResponse = new HashMap<>();
                Stack<Long> recursivelyStack = new Stack<>();
                Map<String, Object> result = ThingService.getInstance()
                        .updateThingsByConditions(recursivelyStack, whereThing, whereFieldValue, udfs, storageDate, currentUser, true, async, batchSize, bridgeCode);

                mapResponse.put("total", result.get("total"));
                if (returnThings) {
                    mapResponse.put("results", result.get("result"));
                }

                listResponse.add(mapResponse);
            }

            logger.info("Updated things in  " + (System.currentTimeMillis() - timeStamp));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return RestUtils.sendBadResponse("Error updating things" + e.getMessage());
        }
        return RestUtils.sendOkResponse(listResponse);
    }


    /*********************************************
     * This method returns a list of things from mongo
     *********************************************/
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 2, value = "Get list of Things",notes="Get list of Things")
    @RequiresPermissions(value = { "thing:r" })
    @ApiResponses(
            value = {
                    @ApiResponse(code = 200, message = "Ok"),
                    @ApiResponse(code = 400, message = "Bad Request"),
                    @ApiResponse(code = 403, message = "Forbidden"),
                    @ApiResponse(code = 500, message = "Internal Server Error")
            }
    )
    public Response listThings(
            @ApiParam(value = "The number of things per page (default 10).") @QueryParam("pageSize") Integer pageSize,
            @ApiParam(value = "The page number you want to be returned (the first one is displayed by default).") @QueryParam("pageNumber") Integer pageNumber,
            @ApiParam(value = "The field to be used to sort the thing results. This can be asc or desc. i.e. name:asc") @QueryParam("order") String order,
            @ApiParam(value = "A filtering parameter to get specific things. Supported operators: Equals (=), like (~), and (&), different (<>), or (|), in ($in), not in ($nin) ") @QueryParam("where") String where,
            @ApiParam(value = "Add extra fields to the response. i.e parent, group, thingType, group.groupType, group.parent") @Deprecated @QueryParam("extra") String extra,
            @ApiParam(value = "The listed fields will be included in the response. i.e.  only= id,name,code") @Deprecated @QueryParam("only") String only,
            @ApiParam(value = "It is used to group the results of the query.") @QueryParam("groupBy") String groupBy,
            @ApiParam(value = "It is used to overridden default visibilityGroup to a lower group.") @QueryParam("visibilityGroupId") Long visibilityGroupId,
            @ApiParam(value = "It is used to disable upVisibility. It can have True or False values") @DefaultValue("") @QueryParam("upVisibility") String upVisibility,
            @ApiParam(value = "It is used to disable downVisibility. It can have True or False values") @DefaultValue("") @QueryParam("downVisibility") String downVisibility,
            @ApiParam(value = "It is used to enable tree view in the format of the results. It can have True or False values") @DefaultValue("false") @QueryParam("treeView") boolean treeView,
            @ApiParam(value = "It is used to enable favorite view in the format of the results. It can have True or False values") @DefaultValue("false") @QueryParam("returnFavorite") boolean returnFavorite,
            @ApiParam(value = "It is used to enable new request algorithm. It can have True or False values") @DefaultValue("false") @QueryParam("reportApi") boolean reportApi,
            @ApiParam(value = "It is used with the new request algorithm enables return of results. It can have True or False values") @DefaultValue("true") @QueryParam("includeResults") boolean includeResults,
            @ApiParam(value = "It is used with the new request algorithm enables return of total registers that satisfies the query . It can have True or False values") @DefaultValue("true") @QueryParam("includeTotal") boolean includeTotal)
    {

        Map<String, Object> mapResponse;
        try{
            //Get data of the user
            Subject subject = SecurityUtils.getSubject();
            User currentUser = (User) subject.getPrincipal();
            if(reportApi){
                ListResult listResult = ThingListExecutor.getInstance().list(
                        pageSize,
                        pageNumber,
                        order,
                        where,
                        extra,
                        only,
                        groupBy,
                        visibilityGroupId,
                        upVisibility,
                        downVisibility,
                        treeView,
                        subject,
                        currentUser,
                        returnFavorite,
                        includeResults,
                        includeTotal);
                return RestUtils.sendOkResponse(listResult);
            } else {
                mapResponse = ThingService.getInstance().processListThings(
                        pageSize
                        , pageNumber
                        , order
                        , where
                        , extra
                        , only
                        , groupBy
                        , visibilityGroupId
                        , upVisibility
                        , downVisibility
                        , treeView
                        , currentUser
                        , returnFavorite);
                return RestUtils.sendOkResponse(mapResponse);
            }
        }catch(Exception e)
        {
            return RestUtils.sendResponseWithCode(e.getMessage(), 500);
        }
    }

    /*********************************************
     * This method returns a query result of things from mongo
     *********************************************/

    @POST
    @Path("/queryThings/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @RequiresPermissions(value = {"thing:r"})
    @ApiOperation(position = 7, value = "Get list of Things",notes="Get list of Things")
    @ApiResponses(
            value = {
                    @ApiResponse(code = 200, message = "Ok"),
                    @ApiResponse(code = 400, message = "Bad Request"),
                    @ApiResponse(code = 403, message = "Forbidden"),
                    @ApiResponse(code = 500, message = "Internal Server Error")
            }
    )
    public Response queryThings(
            @ApiParam(value = "The query in JSON format.") @DefaultValue("{}") String query,
            @ApiParam(value = "The result projection ot the query.") @DefaultValue("{}") @QueryParam("projection")
            String projection,
            @ApiParam(value = "The fields which query will be sorted.") @DefaultValue("{}") @QueryParam("sort") String
                    sort,
            @ApiParam(value = "The number of documents skipped.") @DefaultValue("0") @QueryParam("skip") Integer skip,
            @ApiParam(value = "The limit of documents returned by query.") @DefaultValue("0") @QueryParam("limit")
            Integer limit)
    {

        List<Map<String, Object>> mapResponse = new ArrayList<>();
        try{
            mapResponse = ThingService.getInstance().processQueryThings(
                    query,
                    projection,
                    sort,
                    skip,
                    limit);
        }catch(Exception e)
        {
            return RestUtils.sendResponseWithCode(e.getMessage() ,400 );
        }
        return RestUtils.sendOkResponse( mapResponse );
    }


    @GET
    @Path("/delete/pending")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @RequiresPermissions(value = { "thing:r" })
    @ApiOperation(position = 8, value = "Gets a list of Things pending to delete"
            , notes="Gets a list of Things pending to delete in NoSQL database from SQL database saved into reportbulkprocessdetaillog table")
    @ApiResponses(
            value = {
                    @ApiResponse(code = 200, message = "Ok"),
                    @ApiResponse(code = 400, message = "Bad Request"),
                    @ApiResponse(code = 403, message = "Forbidden"),
                    @ApiResponse(code = 500, message = "Internal Server Error")
            }
    )
    public Response getThingsPendingToDelete(){
        List<BackgroundProcessDetailLog> lstIdsToDeleteObj = BackgroundProcessDetailLogService.getInstance().getThingsPendingToDelete();
        return RestUtils.sendOkResponse(lstIdsToDeleteObj.size());
    }

    @POST
    @Path("/delete/executeCompensation")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @RequiresPermissions(value = {"thingType:d"})
    @ApiOperation(position = 8, value = "Executes the delete compensation algorithm"
            , notes="Executes the delete compensation algorithm into NoSQL database for things deleted in SQL database and saved in reportbulkprocessdetaillog table")
    @ApiResponses(
            value = {
                    @ApiResponse(code = 200, message = "Ok"),
                    @ApiResponse(code = 400, message = "Bad Request"),
                    @ApiResponse(code = 403, message = "Forbidden"),
                    @ApiResponse(code = 500, message = "Internal Server Error")
            }
    )
    public Response executeCompensationAlgorithm(){
        try {
            ThingService.getInstance().runCompensationAlgorithmJob(true, null);
            return RestUtils.sendOkResponse("Process successfully executed");
        } catch (Exception e) {
            return RestUtils.sendBadResponse(e.getMessage());
        }
    }
}
