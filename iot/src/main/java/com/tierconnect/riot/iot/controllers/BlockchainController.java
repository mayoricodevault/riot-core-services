package com.tierconnect.riot.iot.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.entities.Connection;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.services.ConnectionService;
import com.tierconnect.riot.appcore.services.FavoriteService;
import com.tierconnect.riot.appcore.services.RecentService;
import com.tierconnect.riot.appcore.utils.*;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.wordnik.swagger.annotations.*;
import com.wordnik.swagger.jaxrs.PATCH;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.subject.Subject;
import com.tierconnect.riot.commons.utils.JsonUtils;
import com.tierconnect.riot.commons.services.broker.KafkaPublisher;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;

/**
 * Created by djzz on 4/19/17.
 */

@Path("/blockchain")
@Api("/blockchain")
public class BlockchainController {
    private static Logger logger = Logger.getLogger(BlockchainController.class);
    private static UUID uuid = UUID.randomUUID();
    private static final int MAX_TRANSITION_LENGTH = 32;

    @GET
    @Path("/smartContracts/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @RequiresPermissions(value = {"smartcontract:r:{id}"})
    @ApiOperation(position = 2, value = "Select a SmartContract (AUTO)")
    public Response selectSmartContract(
            @PathParam("id") Long id,
            @QueryParam("createRecent") @DefaultValue("false") Boolean createRecent) {
        Map<String, Object> mapResponse = new HashMap<>();
        Subject subject = SecurityUtils.getSubject();
        User currentUser = (User) subject.getPrincipal();
        mapResponse = ThingService.getInstance().processListThings(
                1
                , 1
                , null
                , "(_id=" + id + ")"
                , null
                , null
                , null
                , null
                , ""
                , ""
                , false
                , currentUser
                , false);
        List<Map<String, Object>> results = (List<Map<String, Object>>)mapResponse.get("results");

        if (results == null || results.size() != 1) {
            return RestUtils.sendBadResponse("Invalid Smart Contract id");
        }

        Map<String,Object> smartContract = results.get(0);

        if (createRecent) {
            RecentService.getInstance().insertRecent((Long)smartContract.get("_id"), smartContract.get("name").toString(),
                    "smartcontract",
                    (Long)smartContract.get("groupId"));
        }

        return RestUtils.sendOkResponse(smartContract);
    }

    @GET
    @Path("/smartContracts/")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @RequiresPermissions(value = {"smartcontract:r"})
    @ApiOperation(position = 1, value = "Get list of smart contracts")
    @ApiResponses(
            value = {
                    @ApiResponse(code = 200, message = "Ok"),
                    @ApiResponse(code = 400, message = "Bad Request"),
                    @ApiResponse(code = 403, message = "Forbidden"),
                    @ApiResponse(code = 500, message = "Internal Server Error")
            }
    )
    public Response listSmartContracts(
            @ApiParam(value = "The number of things per page (default 10).") @QueryParam("pageSize") Integer pageSize,
            @ApiParam(value = "The page number you want to be returned (the first one is displayed by default).") @QueryParam("pageNumber") Integer pageNumber,
            @ApiParam(value = "The field to be used to sort the thing results. This can be asc or desc. i.e. name:asc") @QueryParam("order") String order,
            @ApiParam(value = "A filtering parameter to get specific things. Supported operators: Equals (=), like (~), and (&), different (<>), or (|), in ($in), not in ($nin) ") @QueryParam("where") String where,
            @ApiParam(value = "Add extra fields to the response. i.e parent, group, thingType, group.groupType, group.parent") @QueryParam("extra") String extra,
            @ApiParam(value = "The listed fields will be included in the response. i.e.  only= id,name,code") @QueryParam("only") String only,
            @ApiParam(value = "It is used to group the results of the query.") @QueryParam("groupBy") String groupBy,
            @ApiParam(value = "It is used to overridden default visibilityGroup to a lower group.") @QueryParam("visibilityGroupId") Long visibilityGroupId,
            @ApiParam(value = "It is used to disable upVisibility. It can have True or False values") @DefaultValue("") @QueryParam("upVisibility") String upVisibility,
            @ApiParam(value = "It is used to disable downVisibility. It can have True or False values") @DefaultValue("") @QueryParam("downVisibility") String downVisibility,
            @ApiParam(value = "Document Template Name") @DefaultValue("Smart Contract Document") @QueryParam("templateName") String templateName,
            @ApiParam(value = "Favorite statuses") @DefaultValue("false") @QueryParam("returnFavorite") boolean returnFavorite)
    {
        Map<String, Object> mapResponse = new HashMap<>();
        try{
            String filter = "";
            List<Map<String, Object>> typeCodeList = getSmartContractTypeByTemplateName(templateName);
            int length = typeCodeList.size() - 1;

            while(length >= 0) {
                filter += "thingTypeCode=" + typeCodeList.get(length).get("thingTypeCode");
                length--;
                if(length >= 0)
                    filter += "|";
            }

            if(filter.length() > 0) {
                where = where != null ? "(" + filter + ")&" + where : filter;
                //Get data of the user
                Subject subject = SecurityUtils.getSubject();
                User currentUser = (User) subject.getPrincipal();
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
                        , false
                        , currentUser
                        , false);

                if (returnFavorite) {
                    User user = (User) SecurityUtils.getSubject().getPrincipal();
                    List list = (List) mapResponse.get("results");
                    list = FavoriteService.getInstance().addFavoritesToList(list, user.getId(), "smartcontract");
                    mapResponse.put("results", list);
                }
            }
            else {
                mapResponse.put( "total", 0);
                mapResponse.put( "results", new ArrayList());
            }
        }
        catch(Exception e) {
            return RestUtils.sendResponseWithCode(e.getMessage() ,400 );
        }

        return RestUtils.sendOkResponse( mapResponse );
    }


    /***************************************
     * @method Execute Command
     * @description Executes Smart Contract Commands
     * @params: Format Json:
     *************************************************/

    @PATCH
    @Path("/smartContracts/executeCommand")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 5, value = "Update Thing")
    @ApiResponses(
            value = {
                    @ApiResponse(code = 400, message = "Bad Request"),
                    @ApiResponse(code = 200, message = "Ok"),
                    @ApiResponse(code = 403, message = "Forbidden"),
                    @ApiResponse(code = 500, message = "Internal Server Error")
            }
    )
    public Response updateThing(@ApiParam(value = "\"command\" = Command to Execute."
                                        +
                                        "<br>\"commandData\" = Data in Json format for the command to be executed"
                                        +
                                        "<br>\"thing\" = Json with params and body to update the thing.")
                                        Map<String, Object> executionMap) {
        Response result;

        if((executionMap != null) && (executionMap.get("command") != null)) {
            switch (executionMap.get("command").toString()) {
                case "executeTransition":
                case "registerAccount":
                    result = executeCommand((Map<String, Object>)executionMap.get("commandData"),
                                            (Map<String, Object>)executionMap.get("smartContractConfig"));
                    break;
                default:
                    result = RestUtils.sendResponseWithCode("Command not provided", 400);
                    break;
            }
        }
        else {
            result = RestUtils.sendResponseWithCode("Command not provided", 400);
        }

        return result;
    }

    /**
     * Sends a comman execution instruction through Kafka.
     * @param commandData
     * @param smartContractConfig
     * @return
     */
    private Response executeCommand(Map<String,Object> commandData, Map<String, Object> smartContractConfig) {
        Response result;

        if((commandData != null) && (smartContractConfig != null)) {
            try {
                validateCommandExecutionFieldLength(commandData);
                Map<String,Object> operationResult = publishCommand(JsonUtils.convertObjectToJson(commandData));
                if((operationResult.containsKey("operationSuccess"))) {
                    result = updateSmartContractConfig(smartContractConfig);
                }
                else {
                    result = RestUtils.sendResponseWithCode(operationResult.get("operationFail").toString(), 500);
                }

            } catch (JsonProcessingException e) {
                result = RestUtils.sendResponseWithCode(e.getMessage(), 500);
            }
        }
        else {
            result = RestUtils.sendResponseWithCode("Command not provided", 400);
        }

        return result;
    }

    private Map<String,Object> publishCommand(String body) {
        Map<String,Object> result = new HashMap<String, Object>();
        List<Edgebox> edgeboxes = EdgeboxService.getInstance().getByType("smed");
        if(edgeboxes.size() > 0) {
            Edgebox eb = edgeboxes.get(0);
            try {
                JsonNode js = JsonUtils.convertStringToObject(eb.getConfiguration(), JsonNode.class);
                if (js.has("commands")) {
                    JsonNode comm = js.get("commands");
                    String kafkaCode = comm.get("kafkaCode").asText();
                    String[] topicParts = comm.get("topic").asText().split(","); // It could include partition and replica counters.
                    Connection connection = ConnectionService.getInstance().getByCode(kafkaCode);
                    String servers = connection.getPropertiesMap().get("server").toString();
                    String clientId = String.format("SMED-pub-%s", uuid.toString());
                    KafkaPublisher kafkaPublisher = new KafkaPublisher(servers, clientId, connection.getCode(), connection.getConnectionType().getCode());
                    kafkaPublisher.initConnection();
                    kafkaPublisher.publishWithRetry(topicParts[0], body);
                    result.put("operationSuccess", true);
                }

            } catch (IOException e) {
                result.put("operationFail", e.getMessage());
            }
        }
        else {
            result.put("operationFail", "smed configuration not found");
        }

        return result;
    }

    private List<Map<String, Object>> getSmartContractTypeByTemplateName(String templateName)
    {
        String where = "name=" + templateName;
        Long templateId = getSmartContractTemplateCode(where, "id");
        String query = "thingTypeTemplate.id=" + templateId;
        String only  = "thingTypeCode";
        BooleanBuilder be = new BooleanBuilder();
        be = be.and( QueryUtils.buildSearch( QThingType.thingType, query ) );

        List<Map<String, Object>> list = new LinkedList<Map<String, Object>>();
        for( ThingType thingType : ThingTypeService.getInstance().getInstance().listPaginated( be, null, "" ) )
        {
            Map<String,Object> publicMap = QueryUtils.mapWithExtraFields( thingType, "", null);
            QueryUtils.filterOnly( publicMap, only, "" );
            list.add( publicMap );
        }

        return list;
    }

    private Long getSmartContractTemplateCode(String where, String only) {
        BooleanBuilder be = new BooleanBuilder();
        be = be.and( QueryUtils.buildSearch( QThingTypeTemplate.thingTypeTemplate, where ) );

        List<Map<String, Object>> list = new LinkedList<Map<String, Object>>();
        for( ThingTypeTemplate thingTypeTemplate : ThingTypeTemplateService.getInstance().listPaginated( be, null, "" ) )
        {
            Map<String,Object> publicMap = QueryUtils.mapWithExtraFields( thingTypeTemplate, "", null);
            QueryUtils.filterOnly( publicMap, only, "" );
            list.add( publicMap );
        }

        return list.isEmpty() ? null : (Long) list.get(0).get("id");
    }

    /**
     * Update the smart contract config as for the command execution instructions. Only a set of properties
     * are considered to be updatable. 'transitionInExecution' and 'assignedRoles'.
     * @param configMap
     * @return
     */
    private Response updateSmartContractConfig(Map<String, Object> configMap) {
        Response result = RestUtils.sendBadResponse("Failed to update the smart contract config");
        if (configMap != null && configMap.containsKey("id") && configMap.get("id") != null) {
            Long id = Long.parseLong(configMap.get("id").toString());
            configMap.remove("id");
            SmartContractConfig config = SmartContractConfigService.getInstance().get(id);
            if (config != null) {
                if (configMap.containsKey("transitionInExecution")) {
                    config.setTransitionInExecution(configMap.get("transitionInExecution").toString());
                }
                if (configMap.containsKey("assignedRoles")) {
                    config.setAssignedRoles(configMap.get("assignedRoles").toString());
                }
                SmartContractConfigService.getInstance().update(config);
                result = RestUtils.sendOkResponse(config);
            } else {
                result = RestUtils.sendResponseWithCode("Smart contract configuration not found", 404);
            }
        } else {
            result = RestUtils.sendResponseWithCode("smartContractConfig Id not provided", 500);
        }

        return result;
    }

    private void validateCommandExecutionFieldLength(Map<String,Object> data) {
        Map<String,Object> arguments = (Map<String,Object>)data.get("arguments");
        if(arguments != null && arguments.containsKey("fields")) {
            ArrayList<Object> fields = (ArrayList<Object>)arguments.get("fields");
            if(fields !=null) {
                for(int i = 0; i< fields.size(); i++) {
                    if(fields.get(i) instanceof String) {
                        String value = fields.get(i).toString();
                        if(value.length() > MAX_TRANSITION_LENGTH) {
                            fields.set(i, value.substring(0, MAX_TRANSITION_LENGTH));
                            logger.info("field length will be truncated");
                        }
                    }
                }
            }
        }
    }

    private String updateTransitionInExecutionData(String transitionInExecution) {
        String result = transitionInExecution;

        if(transitionInExecution != null) {
            try {
                JsonNode transition = JsonUtils.convertStringToObject(transitionInExecution, JsonNode.class);
                if((transition != null) && (transition.size() > 0)) {
                    Iterator<String> fieldNames = transition.fieldNames();
                    if(fieldNames.hasNext()) {
                        JsonNode firstTransition = transition.get(fieldNames.next());
                        if(firstTransition.has("fieldList")) {
                            JsonNode fieldList = firstTransition.get("fieldList");
                            for (int i = 0; i < fieldList.size(); i++) {
                                JsonNode field = fieldList.get(i);
                                if (field.has("type") && field.get("type").asText().equals("STRING") &&
                                        field.has("value")) {
                                    String value = field.get("value").asText();
                                    if(value.length() > MAX_TRANSITION_LENGTH) {
                                        ((ObjectNode)field).put("value", value.substring(0, MAX_TRANSITION_LENGTH));
                                    }
                                }
                            }
                            result = JsonUtils.convertObjectToJson(transition);
                        }
                    }
                }
            } catch (IOException e) {
                logger.warn("Failed to convert string to Object", e);
            }
        }

        return result;
    }
}
