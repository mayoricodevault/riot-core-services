package com.tierconnect.riot.iot.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.types.path.EntityPathBase;
import com.tierconnect.riot.appcore.controllers.GroupController;
import com.tierconnect.riot.appcore.controllers.RiotShiroRealm;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.QGroup;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.services.FavoriteService;
import com.tierconnect.riot.appcore.services.RecentService;
import com.tierconnect.riot.appcore.utils.EntityVisibility;
import com.tierconnect.riot.appcore.utils.GeneralVisibilityUtils;
import com.tierconnect.riot.appcore.utils.QueryUtils;
import com.tierconnect.riot.appcore.utils.VisibilityUtils;
import com.tierconnect.riot.commons.utils.JsonUtils;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.EdgeboxService;
import com.tierconnect.riot.iot.services.SmartContractDefinitionService;
import com.tierconnect.riot.iot.services.ThingTypeService;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import com.tierconnect.riot.sdk.dao.Pagination;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.utils.BeanUtils;
import com.tierconnect.riot.sdk.utils.PermissionsUtils;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.tierconnect.riot.sdk.utils.UpVisibility;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.jaxrs.PATCH;
import org.apache.commons.collections.map.HashedMap;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.hibernate.exception.ConstraintViolationException;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;

@Path("/smartContractDefinition")
@Api("/smartContractDefinition")
public class SmartContractDefinitionController {
    private static Logger logger = Logger.getLogger(SmartContractDefinitionController.class);
    private static final String NATIVE_THING_TYPE = "NATIVE_THING_TYPE";
    private static final String DATA_THING_TYPE_ID = "27";
    protected static final EntityVisibility<SmartContractDefinition> entityVisibility;

    static {
        entityVisibility = new EntityVisibility<SmartContractDefinition>() {
            @Override
            public QGroup getQGroup(EntityPathBase<SmartContractDefinition> base) {
                return ((QSmartContractDefinition) base).group;
            }

            @Override
            public Group getGroup(SmartContractDefinition object) {
                return object.getGroup();
            }
        };
        entityVisibility.setUpVisibility(UpVisibility.TRUE_R);
        entityVisibility.setDownVisibility(true);
        entityVisibility.setEntityPathBase(QSmartContractDefinition.smartContractDefinition);
    }

    public EntityVisibility getEntityVisibility() {
        return entityVisibility;
    }

    public void addToPublicMap(SmartContractDefinition smartContractDefinition, Map<String,Object> publicMap, String extra )
    {
    }

    public void addToResults(List<Map<String,Object>> results, String extend, String project, Long visibilityGroupId, String upVisibility, String downVisibility			, boolean returnFavorite)
    {
    }

    public List<String> getExtraPropertyNames()
    {
        return new ArrayList<String>();
    }

    /**
     * FILTER LIST
     */

    @GET
    @Path("/group/")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position=1, value="Get a List of Groups (AUTO)")
    public Response listGroups( @QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber, @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only, @QueryParam("visibilityGroupId") Long visibilityGroupId, @DefaultValue("") @QueryParam("upVisibility") String upVisibility, @DefaultValue("") @QueryParam("downVisibility") String downVisibility, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project  )
    {
        validateListPermissions();
        GroupController c = new GroupController();
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(SmartContractDefinition.class.getCanonicalName(), visibilityGroupId);
        RiotShiroRealm.getOverrideVisibilityCache().put(Group.class.getCanonicalName(), visibilityGroup);
        return c.listGroups (pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility, downVisibility, extend, project);
    }

    /**
     * FILTER TREE
     */

    @GET
    @Path("/group/tree")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position=1, value="Get a Tree of Groups (AUTO)")
    public Response listGroupsInTree( @QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber, @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only, @QueryParam("visibilityGroupId") Long visibilityGroupId, @DefaultValue("") @QueryParam("upVisibility") String upVisibility, @DefaultValue("") @QueryParam("downVisibility") String downVisibility, @QueryParam("topId") String topId, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
    {
        validateListPermissions();
        GroupController c = new GroupController();
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(SmartContractDefinition.class.getCanonicalName(), visibilityGroupId);
        RiotShiroRealm.getOverrideVisibilityCache().put(Group.class.getCanonicalName(), visibilityGroup);
        return c.listGroupsInTree(pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility, downVisibility, topId, false, extend, project);
    }

    public void validateListPermissions() {
        if (!(PermissionsUtils.isPermitted(SecurityUtils.getSubject(),"smartContractDefinition:r"))) {
            throw new com.tierconnect.riot.sdk.servlet.exception.ForbiddenException("Not Authorized, Access Denied");
        }
    }


    /**
     * LIST
     */

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresPermissions(value={"smartContractDefinition:r"})
    @ApiOperation(position=1, value="Get a List of SmartContractDefinitions (AUTO)")
    public Response listSmartContractDefinitions( @QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber, @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only, @QueryParam("visibilityGroupId") Long visibilityGroupId, @DefaultValue("") @QueryParam("upVisibility") String upVisibility, @DefaultValue("") @QueryParam("downVisibility") String downVisibility, @DefaultValue("false") @QueryParam("returnFavorite") boolean returnFavorite, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
    {
        Pagination pagination = new Pagination( pageNumber, pageSize );

        BooleanBuilder be = new BooleanBuilder();
        // 2. Limit visibility based on user's group and the object's group (group based authorization)
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(SmartContractDefinition.class.getCanonicalName(), visibilityGroupId);
        EntityVisibility entityVisibility = getEntityVisibility();
        be = be.and( GeneralVisibilityUtils.limitVisibilitySelectAll(entityVisibility, QSmartContractDefinition.smartContractDefinition,  visibilityGroup, upVisibility, downVisibility ) );
        // 4. Implement filtering
        be = be.and( QueryUtils.buildSearch( QSmartContractDefinition.smartContractDefinition, where ) );

        Long count = SmartContractDefinitionService.getInstance().countList( be );
        List<Map<String, Object>> list = new LinkedList<Map<String, Object>>();
        // 3. Implement pagination
        for( SmartContractDefinition smartContractDefinition : SmartContractDefinitionService.getInstance().listPaginated( be, pagination, order ) )
        {
            // Additional filter
            if (!includeInSelect(smartContractDefinition))
            {
                continue;
            }
            // 5a. Implement extra
            Map<String,Object> publicMap = QueryUtils.mapWithExtraFields( smartContractDefinition, extra, getExtraPropertyNames());
            publicMap = QueryUtils.mapWithExtraFieldsNested(smartContractDefinition, publicMap, extend, getExtraPropertyNames());
            addToPublicMap(smartContractDefinition, publicMap, extra);
            // 5b. Implement only
            QueryUtils.filterOnly( publicMap, only, extra );
            QueryUtils.filterProjectionNested( publicMap, project, extend );
            list.add( publicMap );
        }
        if (returnFavorite) {
            User user = (User) SecurityUtils.getSubject().getPrincipal();
            list = FavoriteService.getInstance().addFavoritesToList(list,user.getId(),"smartcontractdefinition");
        }
        addToResults(list, extend, project, visibilityGroupId, upVisibility, downVisibility, returnFavorite);
        Map<String,Object> mapResponse = new HashMap<String,Object>();
        mapResponse.put( "total", count );
        mapResponse.put( "results", list );
        return RestUtils.sendOkResponse( mapResponse );
    }

    public boolean includeInSelect( SmartContractDefinition smartContractDefinition )
    {
        return true;
    }

    private Response selectSmartContractDefinitions( Long id, String extra,  String only,   String extend,  String project)
    {
        SmartContractDefinition smartContractDefinition = SmartContractDefinitionService.getInstance().get( id );
        if( smartContractDefinition == null )
        {
            return RestUtils.sendBadResponse( String.format( "SmartContractDefinitionId[%d] not found", id) );
        }
        // 2. Limit visibility based on user's group and the object's group (group based authorization)
        EntityVisibility entityVisibility = getEntityVisibility();
        GeneralVisibilityUtils.limitVisibilitySelect(entityVisibility, smartContractDefinition);
        validateSelect( smartContractDefinition );
        // 5a. Implement extra
        Map<String,Object> publicMap = QueryUtils.mapWithExtraFields( smartContractDefinition, extra, getExtraPropertyNames());
        publicMap = QueryUtils.mapWithExtraFieldsNested( smartContractDefinition, publicMap, extend, getExtraPropertyNames());
        addToPublicMap(smartContractDefinition, publicMap, extra);
        // 5b. Implement only
        QueryUtils.filterOnly( publicMap, only, extra );
        QueryUtils.filterProjectionNested( publicMap, project, extend );
        return RestUtils.sendOkResponse( publicMap );
    }

    public void validateSelect( SmartContractDefinition smartContractDefinition )
    {

    }


    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresPermissions(value = {"smartContractDefinition:r:{id}"})
    @ApiOperation(position = 2, value = "Select a SmartContractDefinition (AUTO)")
    public Response selectSmartContractDefinitions(
            @PathParam("id") Long id,
            @Deprecated @QueryParam("extra") String extra,
            @Deprecated @QueryParam("only") String only,
            @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend,
            @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project,
            @QueryParam("createRecent") @DefaultValue("false") Boolean createRecent) {
        Response response = this.selectSmartContractDefinitions(id, extra, only, extend, project);
        SmartContractDefinition definition = SmartContractDefinitionService.getInstance().get(id);
        if (createRecent) {
            RecentService.getInstance().insertRecent(definition.getId(), definition.getName(),
                                                     "smartContractDefinition",
                                                     definition.getGroup());
        }
        return response;
    }

    private Response insertSmartContractDefinition( Map<String, Object> map )
    {
        // 2. Limit visibility based on user's group and the object's group (group based authorization)
        EntityVisibility entityVisibility = getEntityVisibility();
        GeneralVisibilityUtils.limitVisibilityInsert(entityVisibility, VisibilityUtils.getObjectGroup(map));
        SmartContractDefinition smartContractDefinition = new SmartContractDefinition();
        // 7. handle insert and update
        BeanUtils.setProperties( map, smartContractDefinition );
        // 6. handle validation in an Extensible manner
        validateInsert( smartContractDefinition );
        SmartContractDefinitionService.getInstance().insert( smartContractDefinition );
        // 1[b,c]. Restrict access based on OBJECT and PROPERTY level read permissions
        Map<String,Object> publicMap = smartContractDefinition.publicMap();
        return RestUtils.sendCreatedResponse( publicMap );
    }

    public void validateInsert( SmartContractDefinition smartContractDefinition )
    {

    }

    @PUT
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresPermissions(value = {"smartContractDefinition:i"})
    @ApiOperation(position = 3, value = "Insert a SmartContractDefinition (AUTO)")
    public Response insertSmartContractDefinition(
            Map<String, Object> map,
            @QueryParam("createRecent") @DefaultValue("false") Boolean createRecent) {
        Response response = this.insertSmartContractDefinition(map);
        try {
            Map<String, Object> publicMap = (Map<String, Object>) response.getEntity();
            SmartContractDefinition definition = SmartContractDefinitionService.getInstance().get((Long) publicMap.get("id"));
            if (createRecent) {
                RecentService.getInstance().insertRecent(definition.getId(), definition.getName(), "smartContractDefinition",
                                                         definition.getGroup());
            }

            updateDefinitionItems(definition.getItems(), definition.getDocumentThingType());

        } catch (ClassCastException cce) {
            logger.warn("Failed to recreate SmartContractParty entity to store it as recent", cce);
        }
        return response;
    }

    private Response updateSmartContractDefinitionBase(  Long id, Map<String, Object> map )
    {
        SmartContractDefinition smartContractDefinition = SmartContractDefinitionService.getInstance().get( id );
        if( smartContractDefinition == null )
        {
            return RestUtils.sendBadResponse( String.format( "SmartContractDefinitionId[%d] not found", id) );
        }
        // 2. Limit visibility based on user's group and the object's group (group based authorization)
        EntityVisibility entityVisibility = getEntityVisibility();
        GeneralVisibilityUtils.limitVisibilityUpdate( entityVisibility, smartContractDefinition, VisibilityUtils.getObjectGroup(map));
        // 7. handle insert and update
        BeanUtils.setProperties( map, smartContractDefinition );
        // 6. handle validation in an Extensible manner
        validateUpdate( smartContractDefinition );
        SmartContractDefinitionService.getInstance().update( smartContractDefinition );
        Map<String,Object> publicMap = smartContractDefinition.publicMap();
        return RestUtils.sendOkResponse( publicMap );
    }

    public void validateUpdate( SmartContractDefinition smartContractDefinition )
    {

    }

    @PATCH
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresPermissions(value={"smartContractDefinition:u:{id}"})
    @ApiOperation(position=4, value="Update a SmartContractDefinition (AUTO)")
    public Response updateSmartContractDefinition( @PathParam("id") Long id, Map<String, Object> map )
    {
        Response response = this.updateSmartContractDefinitionBase(id, map);
        try {
            Map<String, Object> publicMap = (Map<String, Object>) response.getEntity();
            SmartContractDefinition definition = SmartContractDefinitionService.getInstance().get((Long) publicMap.get("id"));
            updateDefinitionItems(definition.getItems(), definition.getDocumentThingType());

        } catch (ClassCastException cce) {
            logger.warn("Failed to recreate SmartContractParty entity to store it as recent", cce);
        }
        return response;
    }

    private void updateDefinitionItems(String items, String documentThingType) {
        try {
            if(items != null && items.length() > 0) {
                JsonNode itemsNode = JsonUtils.convertStringToObject(items, JsonNode.class);
                JsonNode documentNode = JsonUtils.convertStringToObject(documentThingType, JsonNode.class);
                if(documentNode.has("id") && documentNode.has("thingTypeCode")) {
                    String documentId = documentNode.get("id").asText();
                    String documentName = getUdfNameWithPrefix(documentNode.get("thingTypeCode").asText());
                    for (JsonNode node : itemsNode) {
                        if (node.has("thingType")) {
                            JsonNode thingTypeNode = node.get("thingType");
                            updateFields(thingTypeNode.get("thingTypeCode").asText(), documentName, documentId);
                        }
                    }
                }
                else {
                    logger.warn("Smart contract definition Name or Id not found");
                }
            }
        } catch (IOException e) {
            logger.warn("Failed to convert json string to object", e);
        }
    }

    private void updateFields(String thingTypeCode, String definitionName, String definitiondDataTypeThingTypeId) {
        List<Map<String, Object>> thingsTypeMap = new LinkedList<Map<String, Object>>();
        try {
            ThingType thingType = ThingTypeService.getInstance().getByCode(thingTypeCode);
            thingsTypeMap.add(buildSmartContractDefinitionProperty(definitionName, definitiondDataTypeThingTypeId, thingType));
            ThingTypeService.getInstance().updateFields(thingsTypeMap, thingType, false, null, null);
        } catch (ConstraintViolationException e) {
            logger.info(e);
        } catch (UserException e) {
            logger.info(e.getMessage(), e);
        } catch (NonUniqueResultException e) {
            logger.info(e.getMessage(), e);
        }
    }

    private Map<String, Object> buildSmartContractDefinitionProperty(String definitionName,
                                                                     String dataTypeThingTypeId,
                                                                     ThingType thingType) {
        ThingTypeField typeField = thingType.getThingTypeFieldByName(definitionName);
        Map<String, Object> result = new HashedMap();
        result.put("defaultValue", null);
        result.put("dataTypeThingTypeId", dataTypeThingTypeId);
        result.put("multiple", false);
        result.put("typeParent", NATIVE_THING_TYPE);
        result.put("type", DATA_THING_TYPE_ID);
        result.put("name", definitionName);
        result.put("timeSeries", true);
        if (typeField != null) {
         result.put("id", typeField.getId());
        }
        return result;
    }

    private String getUdfNameWithPrefix(String name) {
        String prefix = "scd";
        List<Edgebox> edgeboxes = EdgeboxService.getInstance().getByType("smed");
        if (edgeboxes.size() > 0) {
            Edgebox eb = edgeboxes.get(0);
            try {
                JsonNode js = JsonUtils.convertStringToObject(eb.getConfiguration(), JsonNode.class);
                if (js.has("documentUdfNamePrefix")) {
                    prefix = js.get("documentUdfNamePrefix").asText();
                }
                else {
                    logger.warn("smed property not found. using default name prefix.");
                }

            } catch (IOException e) {
                logger.warn("Failed to convert json string to object. using default name prefix.", e);
            }
        } else {
            logger.warn("smed edge bridge not found. using default name prefix.");
        }

        return prefix + "_" + name;
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"smartContractDefinition:d:{id}"})
    // 1a. Limit access based on CLASS level resources
    @ApiOperation(position=5, value="Delete a SmartContractDefinition (AUTO)")
    public Response deleteSmartContractDefinition( @PathParam("id") Long id )
    {
        // 1c. TODO: Restrict access based on OBJECT level read permissions
        SmartContractDefinition smartContractDefinition = SmartContractDefinitionService.getInstance().get( id );
        if( smartContractDefinition == null )
        {
            return RestUtils.sendBadResponse( String.format( "SmartContractDefinitionId[%d] not found", id) );
        }
        // 2. Limit visibility based on user's group and the object's group (group based authorization)
        EntityVisibility entityVisibility = getEntityVisibility();
        GeneralVisibilityUtils.limitVisibilityDelete(entityVisibility, smartContractDefinition );
        // handle validation in an Extensible manner
        validateDelete( smartContractDefinition );
        SmartContractDefinitionService.getInstance().delete( smartContractDefinition );
        return RestUtils.sendDeleteResponse();
    }

    public void validateDelete( SmartContractDefinition smartContractDefinition )
    {

    }
}
