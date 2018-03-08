package com.tierconnect.riot.iot.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.controllers.ConnectionController;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.LicenseDetail;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.services.FavoriteService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.LicenseService;
import com.tierconnect.riot.appcore.services.RecentService;
import com.tierconnect.riot.appcore.utils.*;
import com.tierconnect.riot.commons.utils.ShellCommandUtil;
import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.entities.EdgeboxRule;
import com.tierconnect.riot.iot.entities.QEdgebox;
import com.tierconnect.riot.iot.services.EdgeboxRuleService;
import com.tierconnect.riot.iot.services.EdgeboxService;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.sdk.dao.Pagination;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.utils.BeanUtils;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.jaxrs.PATCH;
import javax.ws.rs.DELETE;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.subject.Subject;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;

import javax.annotation.Generated;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

@Path("/edgebox")
@Api("/edgebox")
@Generated("com.tierconnect.riot.appgen.service.GenController")
public class EdgeboxController extends EdgeboxControllerBase
{
	private Logger logger = Logger.getLogger( EdgeboxController.class );

	@Context
	ServletContext context;

	@PATCH
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	// 1a. Limit access based on CLASS level resources
	@RequiresPermissions(value = { "edgebox:u:{id}" })
	@ApiOperation(position = 4, value = "Update a Edgebox (AUTO)")
	public Response updateEdgebox( @PathParam("id") Long id, Map<String, Object> map )
	{
        EdgeboxService edgeboxService = new EdgeboxService();
        return edgeboxService.updateEdgebox(id, map, getEntityVisibility());
	}

    @PATCH
    @Path("/{group_id}/{status}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"edgebox:r"})
    @ApiOperation(position = 4, value = "Start all edgeboxes by group")
    public Response startStopAllEdgeboxesByGroup(@PathParam("group_id") Long group_id, @PathParam("status") String status)
    {
        // 2. Limit visibility based on user's group and the object's group (group based authorization)
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(Edgebox.class.getCanonicalName(), group_id);
        EdgeboxService edgeboxService = new EdgeboxService();
        return edgeboxService.startStopAllEdgeboxesByTenant(group_id, status, getEntityVisibility(), visibilityGroup);
    }

    /**
     * Gets the submissionId or Driver ID.
     *
     * @param host the host
     * @param port the port
     */
    private String getSubmissionId(final String host,
                                   final int port) {
        synchronized (this) {
            String submissionId = null;
            try {
                String command = String.format(
                    "echo $s | curl -s http://%s:%d | sed -n '/Running Drivers/,$p' | sed -n '/<table*/,/<\\/table>/p' | sed '\\=</table={p;Q}' | sed -n '/CoreBridge/,$p' | sed -n '/<tr*/,/<\\/tr>/p' | grep -m 1 'driver' | grep -oP 'driver-[\\d-]+'",
                    host, port);
                logger.info(String.format("command: %s", command));
                submissionId = ShellCommandUtil.executeCommand(command).toString();
                submissionId = StringUtils.trim(submissionId);
                logger.info(String.format("submission ID: %s", submissionId));
            } catch (Exception e) {
                logger.error("Mistakes getting the submission ID...");
            }
            return submissionId;
        }
    }

    @GET
    @Path("/configuration/{code}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    //@RequiresPermissions(value = { "edgebox:u:{id}" })
    @ApiOperation(position = 5, value = "Get edgebox configuration including connections")
    public Response getBridgeConfiguration( @PathParam("code") String code )
    {
        Map<String, Object> configMap = EdgeboxService.getInstance().getConfiguration(code);
        return RestUtils.sendOkResponse( configMap );
    }

    @GET
    @Path("/status/")
    @Produces(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresPermissions(value={"edgebox:r"})
    @ApiOperation(position=1, value="Get a List of Edgeboxs (AUTO)")
    public Response listEdgeboxesWithStatus( @QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber, @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only, @QueryParam("visibilityGroupId") Long visibilityGroupId, @DefaultValue("") @QueryParam("upVisibility") String upVisibility, @DefaultValue("") @QueryParam("downVisibility") String downVisibility,@DefaultValue("false") @QueryParam("returnFavorite") boolean returnFavorite, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
    {
        Pagination pagination = new Pagination( pageNumber, pageSize );

        BooleanBuilder be = new BooleanBuilder();
        // 2. Limit visibility based on user's group and the object's group (group based authorization)
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(Edgebox.class.getCanonicalName(), visibilityGroupId);
        EntityVisibility entityVisibility = getEntityVisibility();
        be = be.and( GeneralVisibilityUtils.limitVisibilitySelectAll(entityVisibility, QEdgebox.edgebox, visibilityGroup, upVisibility, downVisibility) );
        // 4. Implement filtering
        be = be.and( QueryUtils.buildSearch( QEdgebox.edgebox, where ) );

        Long count = EdgeboxService.getInstance().countList( be );
        List<Map<String, Object>> list = new LinkedList<Map<String, Object>>();
        // 3. Implement pagination
        for( Edgebox edgebox : EdgeboxService.getInstance().listPaginated( be, pagination, order ) )
        {
            // 5a. Implement extra
            Map<String,Object> publicMap = QueryUtils.mapWithExtraFields( edgebox, extra, getExtraPropertyNames() );
            EdgeboxService.getInstance().getCoreBridgeReference(publicMap, edgebox.getType(), edgebox.getCode(), edgebox.getGroup());
            addToPublicMap(edgebox, publicMap, extra);
            // 5b. Implement only
            QueryUtils.filterOnly( publicMap, only, extra );
            QueryUtils.filterProjectionNested( publicMap, project, extend);

            // getting status from the bridge
//            String edgeboxCode = edgebox.getCode();
//            String status = getStatusFromBridge(edgeboxCode);
//            publicMap.put("status", status);

            list.add( publicMap );
        }

        if (returnFavorite) {
            Subject currentUser = SecurityUtils.getSubject();
            User user = (User) currentUser.getPrincipal();
            FavoriteService.getInstance().addFavoritesToList(list, user.getId(), "edgebox");
        }

        Map<String,Object> mapResponse = new HashMap<String,Object>();
        mapResponse.put( "total", count );
        mapResponse.put("results", list);
        return RestUtils.sendOkResponse( mapResponse );
    }


//    private String getStatusFromBridge(String edgeboxCode) {
//        return "heartbeat=1419969080772";
//    }


    public void validateInsert( Edgebox edgebox )
    {
        EdgeboxService edgeboxService = EdgeboxService.getInstance();

        // validating code
        if (edgeboxService.existBridgeCode(edgebox.getCode(), null)){
            throw new UserException("This code is already taken, please select a different one");
        }

        // validating name
        if(edgeboxService.existBridgeName(edgebox.getName(), edgebox.getGroup().getCode(), null)){
            throw new UserException("This name is already taken in this group, please select a different one");
        }
    }

    public void validateUpdate( Edgebox edgebox )
    {
        EdgeboxService edgeboxService = EdgeboxService.getInstance();

        // validating code
        if (edgeboxService.existBridgeCode(edgebox.getCode(), edgebox.getId())){
            throw new UserException("This code is already taken, please select a different one");
        }

        // validating name
        if(edgeboxService.existBridgeName(edgebox.getName(), edgebox.getGroup().getCode(), edgebox.getId())){
            throw new UserException("This name is already taken in this group, please select a different one");
        }
    }


    /**
     * FILTER LIST
     */
    @GET
    @Path("/edgeboxRule/")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"edgebox:r"})
    @ApiOperation(position=1, value="Get a List of EdgeBoxe Rules")
    public Response listEdgeBoxes(@QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber, @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only, @QueryParam("visibilityGroupId") Long visibilityGroupId, @DefaultValue("") @QueryParam("upVisibility") String upVisibility, @DefaultValue("") @QueryParam("downVisibility") String downVisibility, @DefaultValue("") @QueryParam("returnFavorite") boolean returnFavorite , @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
    {
        EdgeboxRuleController c = new EdgeboxRuleController();
        return c.listEdgeboxRules(pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility, downVisibility, returnFavorite, extend, project);
    }

    @PUT
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresPermissions(value={"edgebox:i"})
    @ApiOperation(position=3, value="Insert a Edgebox (AUTO)")
    public Response insertEdgebox( Map<String, Object> map ,@QueryParam("createRecent") @DefaultValue("false") Boolean createRecent)
    {
        EdgeboxService edgeboxService = new EdgeboxService();

        // 2. Limit visibility based on user's group and the object's group (group based authorization)
        EntityVisibility entityVisibility = getEntityVisibility();
        GeneralVisibilityUtils.limitVisibilityInsert(entityVisibility, VisibilityUtils.getObjectGroup(map));
        Edgebox edgebox = new Edgebox();
        // 7. handle insert and update
        BeanUtils.setProperties(map, edgebox);
        // 6. handle validation in an Extensible manner
        validateInsert(edgebox);
        String bridgeType = edgebox.getType();
        String validationMessage = edgeboxService.validationMessage(edgebox.getParameterType(), bridgeType, map);
        if (validationMessage == null) {
            JSONParser parser = new JSONParser();
            try {
                edgebox.setConfiguration(edgeboxService.cleanDummyValues(
                    (JSONObject) parser.parse(edgebox.getConfiguration())).toJSONString());
            } catch (ParseException ignored) {
            }

            String coreBridgeCode = EdgeboxService.getInstance().getCoreBridgeCode(edgebox.getConfiguration(), edgebox.getType());
            EdgeboxService.getInstance().removeCoreBridgeCodeFakeParam(edgebox);

            EdgeboxService.getInstance().insert(edgebox);

            // Save rules if edgebox is "save as"
            if (map.get("edgebox.id") != null) {
                EdgeboxRuleService edgeboxRuleService = new EdgeboxRuleService();
                Long edgeboxId = Long.valueOf(map.get("edgebox.id").toString());
                List<EdgeboxRule> listEdgeboxRule = edgeboxRuleService.selectByEdgeboxId(edgeboxId);

                for (EdgeboxRule edgeboxRule : listEdgeboxRule) {
                    EdgeboxRule newEdgeboxRule = new EdgeboxRule();
                    newEdgeboxRule.setCronSchedule(edgeboxRule.getCronSchedule());
                    newEdgeboxRule.setActive(edgeboxRule.getActive());
                    newEdgeboxRule.setRunOnReorder(edgeboxRule.getRunOnReorder());
                    newEdgeboxRule.setSerialExecution(edgeboxRule.getSerialExecution());
                    newEdgeboxRule.setDescription(edgeboxRule.getDescription());
                    newEdgeboxRule.setInput(edgeboxRule.getInput());
                    newEdgeboxRule.setName(edgeboxRule.getName());
                    newEdgeboxRule.setOutput(edgeboxRule.getOutput());
                    newEdgeboxRule.setOutputConfig(edgeboxRule.getOutputConfig());
                    newEdgeboxRule.setEdgebox(edgebox);
                    newEdgeboxRule.setRule(edgeboxRule.getRule());
                    newEdgeboxRule.setGroup(edgeboxRule.getGroup());
                    edgeboxRuleService.insert(newEdgeboxRule);

                    String data = serializeData(edgeboxRule);
                    edgeboxRuleService.refreshConfiguration(edgeboxRule.getEdgebox(), data, false);
                }
            }
            EdgeboxService.getInstance().addMqttConnectionPool(edgebox);
            try {
                Map<String, Object> configuration = EdgeboxService.getInstance().getConfiguration(edgebox.getCode());
                EdgeboxService.getInstance().updateRelatedCoreBridgeConfiguration(edgebox.getType(), edgebox.getCode(),
                        coreBridgeCode, "");
                EdgeboxService.getInstance().refreshEdgeboxCache(edgebox, configuration, false);
            } catch (JsonProcessingException e) {
                logger.error("[Kafka] Unable update edgebox and edgebox configuration cache.",e);
            }

            if (createRecent) {
                RecentService.getInstance().insertRecent(edgebox.getId(), edgebox.getName(), "edgebox", edgebox.getGroup());

            }

            // 1[b,c]. Restrict access based on OBJECT and PROPERTY level read permissions
            Map<String, Object> publicMap = edgebox.publicMap();
            return RestUtils.sendCreatedResponse(publicMap);
        } else {
            return RestUtils.sendBadResponse(validationMessage);
        }
    }

    private String serializeData(EdgeboxRule edgeboxRule) {
        EdgeboxRule edgeboxRule0 = new EdgeboxRule();
        edgeboxRule0.setId(edgeboxRule.getId());
        edgeboxRule0.setName(edgeboxRule.getName());
        edgeboxRule0.setRule(edgeboxRule.getRule());
        edgeboxRule0.setInput(edgeboxRule.getInput());
        edgeboxRule0.setOutput(edgeboxRule.getOutput());
        edgeboxRule0.setOutputConfig(edgeboxRule.getOutputConfig());
        edgeboxRule0.setActive(edgeboxRule.getActive());

        String data = "";
        ObjectMapper mapper = new ObjectMapper();
        try {
            data = mapper.writeValueAsString(edgeboxRule0);
        } catch (JsonProcessingException e) {
            System.out.println("cannot serialize edgeboxRule object");
            e.printStackTrace();
        } catch (Exception e){
            e.printStackTrace();
        }
        System.out.println("EdgeboxRule serialized data: " + data);
        return data;
    }

    @GET
    @Path("/connection")
    @Produces(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresAuthentication
    @ApiOperation(position=1, value="Get a List of Connections (AUTO)")
    public Response listConnections( @QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber, @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only, @QueryParam("visibilityGroupId") Long visibilityGroupId, @DefaultValue("") @QueryParam("upVisibility") String upVisibility, @DefaultValue("") @QueryParam("downVisibility") String downVisibility, @DefaultValue("false") @QueryParam("returnFavorite") boolean returnFavorite, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
    {
        Group group = GroupService.getInstance().get(visibilityGroupId);
        if (group == null){
            throw new UserException("group with id "+visibilityGroupId+" is invalid");
        }
        String values="";
        int limit = group.getTreeLevel();
        for (int i=limit;i>1;i--){
            values = values+"group.id="+group.getParentLevel(i).getId()+"|";
        }
        if (where != null){
            where = where + "&("+values+"group.id=1)";
        }else{
            if (!values.isEmpty()){
                where = values+"group.id=1";
            }
        }
        //TODO: Temporary workaround until kafka.enabled review with system connetions
        visibilityGroupId = 1L;
        validateListPermissions();
        ConnectionController c = new ConnectionController();
        return c.listConnections(pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility, downVisibility, false, extend, project);
    }

    @GET
    @Path("/thingType")
    @Produces(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresAuthentication
    @ApiOperation(position=1, value="Get a List of ThingTypes (AUTO)")
    public Response listThingTypes(
            @QueryParam("pageSize") Integer pageSize,
            @QueryParam("pageNumber") Integer pageNumber,
            @DefaultValue("") @QueryParam("order") String order,
            @DefaultValue("") @QueryParam("where") String where,
            @DefaultValue("") @Deprecated @QueryParam("extra") String extra,
            @DefaultValue("") @Deprecated @QueryParam("only") String only,
            @QueryParam("visibilityGroupId") Long visibilityGroupId,
            @DefaultValue("") @QueryParam("upVisibility") String upVisibility,
            @DefaultValue("") @QueryParam("downVisibility") String downVisibility, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project,
            @QueryParam("thingTypeCodes") String... thingTypeCodes)
    {
        return ThingTypeService.getInstance().listThingTypes(pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility, downVisibility, extend, project,thingTypeCodes);
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"edgebox:d:{id}"})
    // 1a. Limit access based on CLASS level resources
    @ApiOperation(position=5, value="Delete a Edgebox (AUTO)")
    @Override
    public Response deleteEdgebox( @PathParam("id") Long id )
    {
        System.out.println("==========================");
        System.out.println("==>> DELETE EDGE BOX <<===");
        System.out.println("==========================");
        // 1c. TODO: Restrict access based on OBJECT level read permissions
        Edgebox edgebox = EdgeboxService.getInstance().get( id );
        if( edgebox == null )
        {
            return RestUtils.sendBadResponse( String.format( "EdgeboxId[%d] not found", id) );
        }
        RecentService.getInstance().deleteRecent(id,"edgebox");
        // 2. Limit visibility based on user's group and the object's group (group based authorization)
        EntityVisibility entityVisibility = getEntityVisibility();
        GeneralVisibilityUtils.limitVisibilityDelete(entityVisibility, edgebox );
        // handle validation in an Extensible manner
        validateDelete( edgebox );
        try {
            Map<String, Object> configuration = EdgeboxService.getInstance().getConfiguration(edgebox.getCode());
            EdgeboxService.getInstance().delete( edgebox );
            EdgeboxService.getInstance().refreshEdgeboxCache(edgebox, configuration, true);
        } catch (JsonProcessingException e) {
            logger.error("[Kafka] Unable update edgebox and edgebox configuration cache.",e);
        }
        return RestUtils.sendDeleteResponse();
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresPermissions(value={"edgebox:r:{id}"})
    @ApiOperation(position=2, value="Select a Edgebox (AUTO)")
    @Override
    public Response selectEdgeboxs( @PathParam("id") Long id, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project, @QueryParam("createRecent") @DefaultValue("false") Boolean createRecent )
    {
        Edgebox edgebox = EdgeboxService.getInstance().get( id );
        if( edgebox == null )
        {
            return RestUtils.sendBadResponse( String.format( "EdgeboxId[%d] not found", id) );
        }
        if ( createRecent ){
            RecentService.getInstance().insertRecent(edgebox.getId(), edgebox.getName(),"edgebox",edgebox.getGroup());
        }
        // 2. Limit visibility based on user's group and the object's group (group based authorization)
        EntityVisibility entityVisibility = getEntityVisibility();
        GeneralVisibilityUtils.limitVisibilitySelect(entityVisibility, edgebox);
        validateSelect( edgebox );
        // 5a. Implement extra
        Map<String,Object> publicMap = QueryUtils.mapWithExtraFields( edgebox, extra, getExtraPropertyNames());
        publicMap = QueryUtils.mapWithExtraFieldsNested( edgebox, publicMap, extend, getExtraPropertyNames());
        addToPublicMap(edgebox, publicMap, extra);
        // 5b. Implement only
        QueryUtils.filterOnly( publicMap, only, extra );
        QueryUtils.filterProjectionNested( publicMap, project, extend );
        publicMap.put("esperRulesPlugin", LicenseService.getInstance().getLicenseDetail(edgebox.getGroup(), true).getFeatures().contains(
            LicenseDetail.ESPER_RULES_PLUGIN));
        EdgeboxService.getInstance().getCoreBridgeReference(publicMap, edgebox.getType(), edgebox.getCode(), edgebox.getGroup());
        return RestUtils.sendOkResponse( publicMap );
    }

    @GET
    @Path("/localMap")
    @Produces(MediaType.APPLICATION_JSON)
// 1a. Limit access based on CLASS level resources
    @RequiresAuthentication
    @ApiOperation(position=1, value="Get a List of LocalMaps (AUTO)")
    public Response listLocalMap(
            @QueryParam("pageSize") Integer pageSize,
            @QueryParam("pageNumber") Integer pageNumber,
            @QueryParam("order") String order,
            @QueryParam("where") String where,
            @Deprecated @QueryParam("extra") String extra,
            @Deprecated @QueryParam("only") String only,
            @QueryParam("visibilityGroupId") Long visibilityGroupId,
            @DefaultValue("") @QueryParam("upVisibility") String upVisibility,
            @DefaultValue("") @QueryParam("downVisibility") String downVisibility, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
    {
        LocalMapController c = new LocalMapController();
        return c.listLocalMaps(pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility, downVisibility,false, extend, project);

    }

    @GET
    @Path("/agent/{agentCode}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(position = 5, value = "Get edgebox configuration including connections")
    public Response getBridgesConfigurationList(@DefaultValue("") @QueryParam("agentCode") String agentCode )
    {
        List<String> configMap = EdgeboxService.getInstance().getBridgesConfigurationList(agentCode);
        return RestUtils.sendOkResponse( configMap );
    }

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresPermissions(value={"edgebox:r"})
    @ApiOperation(position=1, value="Get a List of Edgeboxs (AUTO)")
    public Response listEdgeboxs( @QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber, @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only, @QueryParam("visibilityGroupId") Long visibilityGroupId, @DefaultValue("") @QueryParam("upVisibility") String upVisibility, @DefaultValue("") @QueryParam("downVisibility") String downVisibility, @DefaultValue("false") @QueryParam("returnFavorite") boolean returnFavorite, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
    {
        Pagination pagination = new Pagination( pageNumber, pageSize );

        BooleanBuilder be = new BooleanBuilder();
        // 2. Limit visibility based on user's group and the object's group (group based authorization)
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(Edgebox.class.getCanonicalName(), visibilityGroupId);
        EntityVisibility entityVisibility = getEntityVisibility();
        be = be.and( GeneralVisibilityUtils.limitVisibilitySelectAll(entityVisibility, QEdgebox.edgebox,  visibilityGroup, upVisibility, downVisibility ) );
        // 4. Implement filtering
        be = be.and( QueryUtils.buildSearch( QEdgebox.edgebox, where ) );

        Long count = EdgeboxService.getInstance().countList( be );
        List<Map<String, Object>> list = new LinkedList<Map<String, Object>>();
        // 3. Implement pagination
        for( Edgebox edgebox : EdgeboxService.getInstance().listPaginated( be, pagination, order ) )
        {
            // Additional filter
            if (!includeInSelect(edgebox))
            {
                continue;
            }
            // 5a. Implement extra
            Map<String,Object> publicMap = QueryUtils.mapWithExtraFields( edgebox, extra, getExtraPropertyNames());
            publicMap = QueryUtils.mapWithExtraFieldsNested(edgebox, publicMap, extend, getExtraPropertyNames());
            EdgeboxService.getInstance().getCoreBridgeReference(publicMap, edgebox.getType(), edgebox.getCode(), edgebox.getGroup());
            addToPublicMap(edgebox, publicMap, extra);
            // 5b. Implement only
            QueryUtils.filterOnly( publicMap, only, extra );
            QueryUtils.filterProjectionNested( publicMap, project, extend );
            list.add( publicMap );
        }
        if (returnFavorite) {
            User user = (User) SecurityUtils.getSubject().getPrincipal();
            list = FavoriteService.getInstance().addFavoritesToList(list,user.getId(),"edgebox");
        }
        addToResults(list, extend, project, visibilityGroupId, upVisibility, downVisibility, returnFavorite);
        Map<String,Object> mapResponse = new HashMap<String,Object>();
        mapResponse.put( "total", count );
        mapResponse.put( "results", list );
        return RestUtils.sendOkResponse( mapResponse );
    }

}

