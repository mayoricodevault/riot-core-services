package com.tierconnect.riot.iot.controllers;

import com.tierconnect.riot.appcore.controllers.RiotShiroRealm;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.utils.Utilities;
import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.popdb.PopDBBase;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.appcore.services.UserService;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.servlet.security.ApiKeyToken;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.wordnik.swagger.annotations.*;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

import javax.annotation.security.PermitAll;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import com.tierconnect.riot.commons.Constants;
import org.jose4j.json.internal.json_simple.JSONArray;

/**
 * Created by fflores on 2/10/16.
 */
@Path("/devices")
@Api("/devices")
public class DevicesController {
    private static Logger logger = Logger.getLogger(DevicesController.class);

    @PUT
    @Path("/{mac}/{thingTypeCode}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @PermitAll
    @ApiOperation(value = "Create or update device as a thing",
            position = 2,
            notes = "Example JSON:<br>"
                    + "<font face=\"courier\">{\n" +
                    "<br>  \"ts_ms\": 1455116346322,\n" +
                    "<br>  \"tz\": \"UTC\",\n" +
                    "<br>  \"wallclock\": \"Wed Feb 10 2016 14:59:06 GMT+0000 (UTC)\",\n" +
                    "<br>  \"upTime_sec\": 5700.569910463,\n" +
                    "<br>  \"loadAvg\": [\n" +
                    "<br>    0.0029296875,\n" +
                    "<br>    0.0224609375,\n" +
                    "<br>    0.04541015625\n" +
                    "<br>  ],\n" +
                    "<br>  \"totMem\": 252280832,\n" +
                    "<br>  \"freeMem\": 182620160,\n" +
                    "<br>  \"hostName\": \"mojix6a98e0\",\n" +
                    "<br>  \"id\": \"001F486A98E0\",\n" +
                    "<br>  \"releaseLabel\": \"47860\",\n" +
                    "<br>  \"firmwareVersion\": \"1.0\",\n" +
                    "<br>  \"apiVersion\": 1,\n" +
                    "<br>  \"deviceType\": \"STR_400\",\n" +
                    "<br>  \"webServerVersion\": \"v0.8.29-pre\",\n" +
                    "<br>  \"netIfs\": {\n" +
                    "<br>    \"lo\": [\n" +
                    "<br>      {\n" +
                    "<br>        \"address\": \"127.0.0.1\",\n" +
                    "<br>        \"family\": \"IPv4\",\n" +
                    "<br>        \"internal\": true\n" +
                    "<br>      },\n" +
                    "<br>      {\n" +
                    "<br>        \"address\": \"::1\",\n" +
                    "<br>        \"family\": \"IPv6\",\n" +
                    "<br>        \"internal\": true\n" +
                    "<br>      }\n" +
                    "<br>    ],\n" +
                    "<br>    \"eth0\": [\n" +
                    "<br>      {\n" +
                    "<br>        \"address\": \"10.100.1.187\",\n" +
                    "<br>        \"family\": \"IPv4\",\n" +
                    "<br>        \"internal\": false\n" +
                    "<br>      },\n" +
                    "<br>      {\n" +
                    "<br>        \"address\": \"fe80::21f:48ff:fe6a:98e0\",\n" +
                    "<br>        \"family\": \"IPv6\",\n" +
                    "<br>        \"internal\": false\n" +
                    "<br>      }\n" +
                    "<br>    ]\n" +
                    "<br>  },\n" +
                    "<br>  \"latestMQTTClientMsg\": {\n" +
                    "<br>    \"level\": \"info\",\n" +
                    "<br>    \"msg\": \"Verified connectivity to MQTT broker\",\n" +
                    "<br>    \"ts_ms\": 1455114410567,\n" +
                    "<br>    \"metaData\": {\n" +
                    "<br>      \"type\": \"mqtt\",\n" +
                    "<br>      \"myID\": \"001F486A98E0\"\n" +
                    "<br>    }\n" +
                    "<br>  },\n" +
                    "<br>  \"jurisdiction\": \"FCC (U.S.)\"\n" +
                    "<br> }</font>")
    @ApiResponses(
            value = {
                    @ApiResponse(code = 200, message = "Ok"),
                    @ApiResponse(code = 400, message = "Bad Request")
            }
    )
    public Response create(
            @PathParam("mac") String mac,
            @PathParam("thingTypeCode") String thingTypeCode, Map<String, Object> deviceMap,
            @Context HttpServletRequest request) {
        Map<String, Object> result = null;
        try {
            logger.info("START: Device create/update: "+mac+" ("+thingTypeCode+") Remote IP: "+request.getRemoteAddr());
            //Put Root User in order to permit to this End Point does queries
            RiotShiroRealm.initCaches();
            Subject currentUser = SecurityUtils.getSubject();
            User user = UserService.getInstance().getRootUser();
            ApiKeyToken token = new ApiKeyToken(user.getApiKey());
            currentUser.login(token);
            Stack<Long> recursivelyStackConfig = new Stack<>();
            Stack<Long> recursivelyStackStatus = new Stack<>();
            result = DevicesService.getInstance().createDevice(
                    recursivelyStackConfig, recursivelyStackStatus, mac,thingTypeCode, Constants.TT_STARflex_STATUS_CODE,deviceMap, user);
            logger.info("END: Device create/update: "+mac+" ("+thingTypeCode+") Remote IP: "+request.getRemoteAddr());

        } catch (UserException e) {
            return RestUtils.sendResponseWithCode(e.getMessage(), 400);
        }
        return RestUtils.sendOkResponse(result);
    }

    /*************************************************
     * @method createNewTenant
     * @description This method creates a new tenant and a new user.
     * In addition, this is going to migrate all things into this new Tenant
     *************************************************/
    @PUT
    @Path("/claim/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @PermitAll
    @ApiOperation(
            value = "Create a new group and migrate serials into the another group",
            position = 2,
            notes = "New Data:<br>"
                    + "<font face=\"courier\">{ \n" +
                    "<br>\"group\":\n" +
                    "<br>{\n" +
                    "<br>  \"name\":\"value Group Name\"\n" +
                    "<br>}, \n" +
                    "<br>\"user\": { \n" +
                    "<br>\"username\": \"usertest\", \n" +
                    "<br>\"password\": \"usertest\", \n" +
                    "<br>\"firstName\":\"FirstName\", \n" +
                    "<br>\"lastName\": \"lastName\", \n" +
                    "<br>\"email\": \"\" \n" +
                    "<br>}, \n" +
                    "<br>\"serials\": [ \n" +
                    "<br>{ \n" +
                    "<br>\"serial\": \"132000001ASD\", \n" +
                    "<br>\"thingTypeCode\": \"OldThingTypeCode\" \n" +
                    "<br>}, \n" +
                    "<br>{ \n" +
                    "<br>\"serial\": \"132000002ASD\", \n" +
                    "<br>\"thingTypeCode\": \"OldThingTypeCode\" \n" +
                    "<br>} \n" +
                    "<br>] \n" +
                    "<br>}</font>" +

                    "<br>Update Data:<br>" +
                    "<font face=\"courier\">{ \n" +
                    "<br>\"group\":\n" +
                    "<br>{\n" +
                    "<br>  \"id\":23,\n" +
                    "<br>  \"code\":\"example_group\"\n" +
                    "<br>}, \n" +
                    "<br>\"user\": {  \n" +
                    "<br>\"id\":12,\n" +
                    "<br>\"username\": \"usertest\", \n" +
                    "<br>\"password\": \"usertest\", \n" +
                    "<br>\"firstName\":\"FirstName\", \n" +
                    "<br>\"lastName\": \"lastName\", \n" +
                    "<br>\"email\": \"\" \n" +
                    "<br>}, \n" +
                    "<br>\"serials\": [ \n" +
                    "<br>{ \n" +
                    "<br>\"serial\": \"132000001ASD\", \n" +
                    "<br>\"thingTypeCode\": \"OldThingTypeCode\" \n" +
                    "<br>}, \n" +
                    "<br>{ \n" +
                    "<br>\"serial\": \"132000002ASD\", \n" +
                    "<br>\"thingTypeCode\": \"OldThingTypeCode\" \n" +
                    "<br>} \n" +
                    "<br>] \n" +
                    "<br>}" +
                    "</font>")
    @ApiResponses(
            value = {
                    @ApiResponse(code = 200, message = "Created"),
                    @ApiResponse(code = 400, message = "Bad Request"),
                    @ApiResponse(code = 500, message = "Internal Server Error")
            }
    )
    public Response createNewTenant(
            @ApiParam(value = "Map with three sections: group, user and serials.")  Map<String, Object> map,
            @Context HttpServletRequest request)
    {
        logger.info("Starting association between Group and Device. "+(map!=null?map.toString():"")+". Remote IP: "+request.getRemoteAddr());
        Map<String, Object> result = new HashMap<>();
        try{
            //Put Root User in order to permit to this End Point does queries
            RiotShiroRealm.initCaches();
            Subject subject = SecurityUtils.getSubject();
            User user = UserService.getInstance().getRootUser();
            ApiKeyToken token = new ApiKeyToken(user.getApiKey());
            subject.login(token);
//            //Process create new tenant
            PopDBBase popDB = new PopDBBase();
            setPopDBProperties(map);
            popDB.currentPopDB = "Claim";
            List<Map<String, Object>> serialMap = (List<Map<String, Object>>) map.get("serials");

            for (Map<String, Object> serialNumber : serialMap) {
                setSerialProperty(serialNumber);
                popDB.executeModules((JSONArray) popDB.getPopDBDependencies().get("Claim").get("executeModules"));
            }

            Stack<Long> recursivelyStack = new Stack<>();
            Group group;
            if (((Map<String, Object>) map.get("group")).containsKey("name")) {
                group = GroupService.getInstance().getByCode(Utilities.sanitizeString(
                    ((Map<String, Object>) map.get("group")).get("name").toString()));
            } else {
                group = GroupService.getInstance().getByCode(
                    ((Map<String, Object>) map.get("group")).get("code").toString());
            }

            if (group == null) {
                logger.error("An error occurred wile instantiating claim structure.");
                return RestUtils.sendResponseWithCode("An error occurred wile instantiating claim structure.", 400);
            }

            ThingType thingTypeStarflexConfig = ThingTypeService.getInstance().getByCode("SF_" + group.getCode());
            ThingType thingTypeStarflexStatus = ThingTypeService.getInstance().getByCode("SFS_" + group.getCode());
            ThingType thingTypeStarflexTag = ThingTypeService.getInstance().getByCode("SFT_" + group.getCode());

            try {
                result = DevicesService.getInstance()
                    .migrateThingsToAnotherGroup(recursivelyStack, serialMap,
                        thingTypeStarflexConfig, thingTypeStarflexStatus, group, user);
                result.put("thingTypeStarflexConfig", thingTypeStarflexConfig.publicMap());
                result.put("thingTypeStarflexStatus", thingTypeStarflexStatus.publicMap());
                result.put("thingTypeStarflexTag", thingTypeStarflexTag.publicMap());
                result.put("response", "OK");

                EdgeboxService edgeboxService = EdgeboxService.getInstance();
                List<Edgebox> sfEdgeboxes = new ArrayList<>();
                sfEdgeboxes.add(edgeboxService.getByCode("CORE_" + group.getCode()));
                sfEdgeboxes.add(edgeboxService.getByCode("ALEB_" + group.getCode()));
                sfEdgeboxes.add(edgeboxService.getByCode("STAR_" + group.getCode()));

                for (Edgebox edgebox : sfEdgeboxes) {
                    edgebox.setStatus("ON");
                    edgeboxService.addMqttConnectionPool(edgebox);
                    BrokerClientHelper.initLazyLoading();
                    BrokerClientHelper
                        .sendRefreshEdgebox(edgebox.getCode(), edgebox.getConfiguration(), false,
                                GroupService.getInstance().getMqttGroups(edgebox.getGroup()));
                    edgeboxService
                        .statusServiceAction(edgebox.getConfiguration(), edgebox.getCode(), edgebox.getType(), "ON", edgebox.getGroup().getId());
                }
            } catch (Exception e) {
                logger.error(e.getMessage(),e);
                return RestUtils.sendResponseWithCode("An error occurred in thing process, please verify that all the things exists.", 400);
            }
        }catch(Exception e)
        {
            logger.error(e.getMessage(),e);
            return RestUtils.sendResponseWithCode(e.getMessage(), 400);
        }
        return RestUtils.sendOkResponse(result);
    }

    private void setPopDBProperties(Map<String, Object> map) throws NonUniqueResultException {
        Map<String, Object> groupMap = (Map<String, Object>) map.get("group");
        Map<String, Object> userMap = (Map<String, Object>) map.get("user");
        System.setProperty("popdb.option", "Claim");
        if (groupMap.containsKey("name")) {
            System.setProperty("tenant.name", groupMap.get("name").toString());
            System.setProperty("tenant.code", Utilities.sanitizeString(groupMap.get("name").toString()));
        } else {
            Group group = GroupService.getInstance().getByCode(groupMap.get("code").toString());
            System.setProperty("tenant.name", group.getName());
            System.setProperty("tenant.code", group.getCode());
        }
        System.setProperty("user.username", userMap.get("username").toString());
        if (userMap.containsKey("password") && userMap.get("password") != null) {
            System.setProperty("user.password", userMap.get("password").toString());
        } else {
            System.setProperty("user.password", "");
        }
        System.setProperty("user.firstName", userMap.containsKey("firstName") ? userMap.get("firstName").toString() : "");
        System.setProperty("user.lastName", userMap.containsKey("lastName") ? userMap.get("lastName").toString() : "");
        System.setProperty("user.email", userMap.containsKey("email") ? userMap.get("email").toString() : "");
    }

    private void setSerialProperty(Map<String, Object> serialMap) {
        System.setProperty("serial.number", serialMap.get("serial").toString());
        System.setProperty("serial.thingTypeCode", serialMap.get("thingTypeCode").toString());
    }

    @GET
    @Path("/claim/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @PermitAll
    @ApiOperation(value = "Get Thing")
    @ApiResponses(
            value = {
                    @ApiResponse(code = 200, message = "Ok"),
                    @ApiResponse(code = 400, message = "Bad Request"),
                    @ApiResponse(code = 403, message = "Forbidden"),
                    @ApiResponse(code = 500, message = "Internal Server Error")
            }
    )
    public Response getThing( @ApiParam(value = "Serial Number ( mandatory parameter )")
                              @QueryParam("serialNumber") String serialNumber,
                              @ApiParam(value = "Group Id")
                              @QueryParam("groupId") Long groupId)
    {
        Map<String, Object> mapResponse = new HashMap<>();
        try{
            //Put Root User in order to permit to this End Point does queries
            RiotShiroRealm.initCaches();
            Subject currentUser = SecurityUtils.getSubject();
            ApiKeyToken token = new ApiKeyToken(UserService.getInstance().getRootUser().getApiKey());
            currentUser.login(token);

            User currentUserBean = (User) currentUser.getPrincipal();
            if( ( serialNumber!=null && !serialNumber.trim().equals("") )  ) {
                mapResponse = DevicesService.getInstance().searchDevice(serialNumber, groupId, currentUserBean);
            } else {
                return RestUtils.sendResponseWithCode("Serial Number is mandatory." ,400 );
            }
        }catch(Exception e)
        {
            e.printStackTrace();
            logger.error(e.getMessage(),e);
            return RestUtils.sendResponseWithCode(e.getMessage() ,400 );
        }
        return RestUtils.sendOkResponse( mapResponse );
    }


}
