package com.tierconnect.riot.iot.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.types.path.EntityPathBase;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.QGroup;
import com.tierconnect.riot.appcore.utils.*;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.iot.entities.ActionConfiguration;
import com.tierconnect.riot.iot.entities.LogExecutionAction;
import com.tierconnect.riot.iot.entities.QActionConfiguration;
import com.tierconnect.riot.iot.services.ActionConfigurationService;
import com.tierconnect.riot.iot.services.LogExecutionActionService;
import com.tierconnect.riot.iot.services.LogExecutionActionServiceBase;
import com.tierconnect.riot.sdk.dao.Pagination;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.utils.BeanUtils;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.tierconnect.riot.sdk.utils.UpVisibility;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.jaxrs.PATCH;
import org.apache.log4j.Logger;
import org.apache.shiro.authz.annotation.RequiresAuthentication;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;


@Path("/actionConfiguration")
@Api("/actionConfiguration")
public class ActionConfigurationController {

    private static Logger logger = Logger.getLogger(ActionConfigurationController.class);
    protected static final EntityVisibility<ActionConfiguration> entityVisibility;

    static {
        entityVisibility = new EntityVisibility<ActionConfiguration>() {
            @Override
            public QGroup getQGroup(EntityPathBase<ActionConfiguration> base) {
                return ((QActionConfiguration) base).group;
            }

            @Override
            public Group getGroup(ActionConfiguration object) {
                return object.getGroup();
            }
        };
        entityVisibility.setUpVisibility(UpVisibility.FALSE);
        entityVisibility.setDownVisibility(true);
        entityVisibility.setEntityPathBase(QActionConfiguration.actionConfiguration);
    }

    public EntityVisibility getEntityVisibility() {
        return entityVisibility;
    }

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresAuthentication
    @ApiOperation(
            position = 1,
            value = "Get a List of ActionConfigurations (AUTO)",
            notes = "Return all actions")
    public Response listActionConfigurations(@QueryParam("pageSize") Integer pageSize,
                                             @QueryParam("pageNumber") Integer pageNumber,
                                             @QueryParam("order") String order,
                                             @QueryParam("where") String where,
                                             @QueryParam("extra") String extra,
                                             @QueryParam("only") String only,
                                             @QueryParam("visibilityGroupId") Long visibilityGroupId,
                                             @DefaultValue("") @QueryParam("upVisibility") String upVisibility,
                                             @DefaultValue("") @QueryParam("downVisibility") String downVisibility) {
        Pagination pagination = new Pagination(pageNumber, pageSize);

        BooleanBuilder be = new BooleanBuilder();

        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(ActionConfiguration.class.getCanonicalName(), visibilityGroupId);
        EntityVisibility entityVisibility = getEntityVisibility();

        be = be.and(GeneralVisibilityUtils.limitVisibilitySelectAll(entityVisibility, QActionConfiguration.actionConfiguration, visibilityGroup, upVisibility, downVisibility));

        be = be.and(QActionConfiguration.actionConfiguration.status.eq(Constants.ACTION_STATUS_ACTIVE)); // only actives
        be = be.and(QueryUtils.buildSearch(QActionConfiguration.actionConfiguration, where));

        Long count = ActionConfigurationService.getInstance().countList(be);
        List<Map<String, Object>> list = new LinkedList<>();
        // 3. Implement pagination
        for (ActionConfiguration actionConfiguration : ActionConfigurationService.getInstance().listPaginated(be, pagination, order)) {
            Map<String, Object> publicMap = QueryUtils.mapWithExtraFields(actionConfiguration, extra, getExtraPropertyNames());
            addToPublicMap(actionConfiguration, publicMap, extra);
            QueryUtils.filterOnly(publicMap, only, extra);

            list.add(publicMap);
        }
        Map<String, Object> mapResponse = new HashMap<String, Object>();
        mapResponse.put("total", count);
        mapResponse.put("results", list);
        return RestUtils.sendOkResponse(mapResponse);
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresAuthentication
    @ApiOperation(position = 2, value = "Select a ActionConfiguration (AUTO)", notes = "return action by id")
    public Response selectActionConfigurations(@ApiParam(value = "Identifier of the object")
                                               @PathParam("id") Long id,
                                               @QueryParam("extra") String extra,
                                               @QueryParam("only") String only) {
        ActionConfiguration actionConfiguration = ActionConfigurationService.getInstance().getActionConfigurationActive(id);
        if (actionConfiguration == null) {
            return RestUtils.sendBadResponse(String.format("ActionConfigurationId[%d] not found", id));
        }
        EntityVisibility entityVisibility = getEntityVisibility();
        GeneralVisibilityUtils.limitVisibilitySelect(entityVisibility, actionConfiguration);
        Map<String, Object> publicMap = QueryUtils.mapWithExtraFields(actionConfiguration, extra, getExtraPropertyNames());
        addToPublicMap(actionConfiguration, publicMap, extra); // add configuration
        QueryUtils.filterOnly(publicMap, only, extra);
        return RestUtils.sendOkResponse(publicMap);
    }

    @PUT
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 3, value = "Insert a Action",
            notes = "<h3>Example configuration</h3>\n" +
                    "<p>{\n" +
                    "    <br /> &emsp; \"name\": \"TEST ACTION\"," +
                    "    <br /> &emsp; \"type\": \"HTTP\"," +
                    "    <br /> &emsp; \"configuration\": {" +
                    "    <br /> &emsp; &emsp; \"method\": \"POST\"," +
                    "    <br /> &emsp; &emsp; \"openResponseIn\": \"MODAL\", <b>// values MODAL, TAB or POPUP</b>" +
                    "    <br /> &emsp; &emsp; \"url\": \"http://ip:port/path\"," +
                    "    <br /> &emsp; &emsp; \"timeout\": 600  ,  <b>// in senconds</b>" +
                    "    <br /> &emsp; &emsp; \"headers\": {" +
                    "    <br /> &emsp; &emsp; &emsp; \"Content-Type\": \"application/json\"" +
                    "    <br /> &emsp; &emsp; }," +
                    "    <br /> &emsp; \"basicAuth\": {" +
                    "    <br /> &emsp; &emsp; \"username\": \"\"," +
                    "    <br /> &emsp; &emsp; \"password\": \"\"" +
                    "    <br /> &emsp; }" +
                    "    <br /> &emsp; }," +
                    "    <br /> &emsp; \"group.id\": 2" +
                    "    <br />}</p>")
    public Response insertActionConfiguration(Map<String, Object> map) {
        EntityVisibility entityVisibility = getEntityVisibility();
        GeneralVisibilityUtils.limitVisibilityInsert(entityVisibility, VisibilityUtils.getObjectGroup(map));

        ActionConfiguration actionConfiguration = new ActionConfiguration();
        BeanUtils.setProperties(map, actionConfiguration);
        actionConfiguration.setStatus(Constants.ACTION_STATUS_ACTIVE);
        actionConfiguration.setCode("ACTION_" + System.currentTimeMillis());
        ActionConfigurationService.getInstance().insert(actionConfiguration);
        actionConfiguration.mapConfiguration();
        return RestUtils.sendOkResponse(actionConfiguration.publicMap());
    }

    @PATCH
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 4, value = "Update a Action",
            notes = "<h3>Example configuration</h3>" +
                    "<p>{" +
                    "<br /> &emsp; \"id\": 1,  <b>// Identifier of the object </b>" +
                    "<br /> &emsp; \"name\": \"TEST ACTION\"," +
                    "<br /> &emsp; \"type\": \"HTTP\"," +
                    "<br /> &emsp; \"configuration\": {" +
                    "<br /> &emsp; &emsp; \"method\": \"POST\"," +
                    "<br /> &emsp; &emsp; \"openResponseIn\": \"MODAL\", <b>// values MODAL, TAB or POPUP</b>" +
                    "<br /> &emsp; &emsp; \"url\": \"http://ip:port/path\"," +
                    "<br /> &emsp; &emsp; \"timeout\": 600,  <b>// in senconds</b>" +
                    "<br /> &emsp; &emsp; \"headers\": {" +
                    "<br /> &emsp; &emsp; &emsp; \"Content-Type\": \"application/json\"" +
                    "<br /> &emsp; &emsp; }," +
                    "<br /> &emsp; \"basicAuth\": {" +
                    "<br /> &emsp; &emsp; \"username\": \"\"," +
                    "<br /> &emsp; &emsp; \"password\": \"\"" +
                    "<br /> &emsp; }" +
                    "<br /> &emsp; }," +
                    "<br /> &emsp; \"group.id\": 2" +
                    "<br />}</p>")
    public Response updateActionConfiguration(@ApiParam(value = "Identifier of the object")
                                              @PathParam("id") Long id, Map<String, Object> map) {
        ActionConfiguration actionConfiguration = ActionConfigurationService.getInstance().getActionConfigurationActive(id);
        if (actionConfiguration == null) {
            return RestUtils.sendBadResponse(String.format("ActionConfigurationId[%d] not found", id));
        }
        EntityVisibility entityVisibility = getEntityVisibility();
        GeneralVisibilityUtils.limitVisibilityInsert(entityVisibility, VisibilityUtils.getObjectGroup(map));
        String oldConfiguration = actionConfiguration.getConfiguration();
        BeanUtils.setProperties(map, actionConfiguration);
        actionConfiguration.setStatus(Constants.ACTION_STATUS_ACTIVE);
        ActionConfigurationService.getInstance().update(actionConfiguration, oldConfiguration);
        actionConfiguration.mapConfiguration();
        return RestUtils.sendOkResponse(actionConfiguration.publicMap());
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    // 1a. Limit access based on CLASS level resources
    @ApiOperation(position = 5, value = "Delete a Action")
    public Response deleteActionConfiguration(@ApiParam(value = "Identifier of the object")
                                              @PathParam("id") Long id) {
        ActionConfiguration actionConfiguration = ActionConfigurationService.getInstance().getActionConfigurationActive(id);
        if (actionConfiguration == null) {
            return RestUtils.sendBadResponse(String.format("ActionConfigurationId[%d] not found", id));
        }
        ActionConfigurationService.getInstance().delete(actionConfiguration);
        actionConfiguration.mapConfiguration();
        return RestUtils.sendOkResponse(actionConfiguration.publicMap());
    }

    @POST
    @Path("/logs")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 6, value = "return logs by parameters", notes = "" +
            "<h3>Example search 1</h3>" +
            "<p>{" +
            "<br /> &emsp; \"user.id\": 1," +
            "<br /> &emsp; \"iniDate\": 1491236680000," +
            "<br /> &emsp; \"endDate\": 1491236680999," +
            "<br /> &emsp; \"actionConfiguration.id\": 1" +
            "<br />}</p>" +
            "<h3>Example search 2</h3>" +
            "<p>{" +
            "<br /> &emsp; \"user.id\": 1," +
            "<br /> &emsp; \"iniDate\": 1491236680000," +
            "<br /> &emsp; \"endDate\": 1491236680999," +
            "<br />}</p>" +
            "<h3>Example search 3</h3>" +
            "<p>{" +
            "<br /> &emsp; \"user.id\": 1," +
            "<br /> &emsp; \"iniDate\": 1491236680000," +
            "<br /> &emsp; \"actionConfiguration.id\": 1" +
            "<br />}</p>" +
            "<h3>Example search 4</h3>" +
            "<p>{" +
            "<br /> &emsp; \"user.id\": 1," +
            "<br /> &emsp; \"endDate\": 1491236680999," +
            "<br /> &emsp; \"actionConfiguration.id\": 1" +
            "<br />}</p>" +
            "<h3>Example search 5</h3>" +
            "<p>{" +
            "<br /> &emsp; \"user.id\": 1," +
            "<br /> &emsp; \"actionConfiguration.id\": 1" +
            "<br />}</p>" +
            "<h3>Search invalid</h3>" +
            "<p>{" +
            "<br />}</p>")
    public Response returnLogs(Map<String, Object> body) {
        if (body.isEmpty()) {
            throw new UserException("Search parameters is empty");
        }
        LogExecutionActionService service = LogExecutionActionServiceBase.getInstance();
        return RestUtils.sendOkResponse(service.getLogs(body));
    }

    @GET
    @Path("/log/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 7, value = "return logs by parameters", notes = "")
    public Response returnLog(@ApiParam(value = "Identifier of the object")
                              @PathParam("id") Long id) {
        LogExecutionActionService service = LogExecutionActionService.getInstance();
        LogExecutionAction logExecutionAction = service.get(id);
        if (logExecutionAction == null) {
            return RestUtils.sendBadResponse(String.format("Log execution [%d] not found", id));
        }
        Map<String, Object> map = service.publicMapWithoutPassword(logExecutionAction.publicMap());
        return RestUtils.sendOkResponse(map);
    }

    @SuppressWarnings("unchecked")
    private void addToPublicMap(ActionConfiguration actionConfiguration, Map<String, Object> publicMap, String extra) {
        ObjectMapper objectMapper = new ObjectMapper();
        String configuration = actionConfiguration.getConfiguration();
        if (!Utilities.isEmptyOrNull(configuration)) {
            try {
                TreeMap<String, Object> configurationMap = (TreeMap<String, Object>) objectMapper.readValue(configuration, TreeMap.class);
                publicMap.put("configuration", configurationMap);
            } catch (IOException e) {
                logger.error("Error in configuration parser of Action with id[" + actionConfiguration.getId() + "]", e);
            }
        } else {
            publicMap.put("configuration", Collections.emptyMap());
        }
    }

    public List<String> getExtraPropertyNames() {
        return new ArrayList<String>();
    }

}

