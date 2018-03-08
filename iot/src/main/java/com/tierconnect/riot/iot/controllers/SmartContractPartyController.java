package com.tierconnect.riot.iot.controllers;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
import com.tierconnect.riot.iot.entities.QSmartContractParty;
import com.tierconnect.riot.iot.entities.SmartContractParty;
import com.tierconnect.riot.iot.services.SmartContractPartyService;
import com.tierconnect.riot.sdk.dao.Pagination;
import com.tierconnect.riot.sdk.utils.BeanUtils;
import com.tierconnect.riot.sdk.utils.PermissionsUtils;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.tierconnect.riot.sdk.utils.UpVisibility;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.jaxrs.PATCH;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import java.util.*;

@Path("/smartContractParty")
@Api("/smartContractParty")
public class SmartContractPartyController  {
    private static Logger logger = Logger.getLogger(SmartContractPartyController.class);

    protected static final EntityVisibility<SmartContractParty> entityVisibility;

    static {
        entityVisibility = new EntityVisibility<SmartContractParty>() {
            @Override
            public QGroup getQGroup(EntityPathBase<SmartContractParty> base) {
                return ((QSmartContractParty) base).group;
            }

            @Override
            public Group getGroup(SmartContractParty object) {
                return object.getGroup();
            }
        };
        entityVisibility.setUpVisibility(UpVisibility.TRUE_R);
        entityVisibility.setDownVisibility(true);
        entityVisibility.setEntityPathBase(QSmartContractParty.smartContractParty);
    }

    public EntityVisibility getEntityVisibility() {
        return entityVisibility;
    }

    /**
     * LIST
     */

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresPermissions(value={"smartContractParty:r"})
    @ApiOperation(position=1, value="Get a List of SmartContractPartys (AUTO)")
    public Response listSmartContractPartys( @QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber, @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only, @QueryParam("visibilityGroupId") Long visibilityGroupId, @DefaultValue("") @QueryParam("upVisibility") String upVisibility, @DefaultValue("") @QueryParam("downVisibility") String downVisibility, @DefaultValue("false") @QueryParam("returnFavorite") boolean returnFavorite, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
    {
        Pagination pagination = new Pagination( pageNumber, pageSize );

        BooleanBuilder be = new BooleanBuilder();
        // 2. Limit visibility based on user's group and the object's group (group based authorization)
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(SmartContractParty.class.getCanonicalName(), visibilityGroupId);
        EntityVisibility entityVisibility = getEntityVisibility();
        be = be.and( GeneralVisibilityUtils.limitVisibilitySelectAll(entityVisibility, QSmartContractParty.smartContractParty,  visibilityGroup, upVisibility, downVisibility ) );
        // 4. Implement filtering
        be = be.and( QueryUtils.buildSearch( QSmartContractParty.smartContractParty, where ) );

        Long count = SmartContractPartyService.getInstance().countList( be );
        List<Map<String, Object>> list = new LinkedList<Map<String, Object>>();
        // 3. Implement pagination
        for( SmartContractParty smartContractParty : SmartContractPartyService.getInstance().listPaginated( be, pagination, order ) )
        {
            // Additional filter
            if (!includeInSelect(smartContractParty))
            {
                continue;
            }
            // 5a. Implement extra
            Map<String,Object> publicMap = QueryUtils.mapWithExtraFields( smartContractParty, extra, getExtraPropertyNames());
            publicMap = QueryUtils.mapWithExtraFieldsNested(smartContractParty, publicMap, extend, getExtraPropertyNames());
            addToPublicMap(smartContractParty, publicMap, extra);
            // 5b. Implement only
            QueryUtils.filterOnly( publicMap, only, extra );
            QueryUtils.filterProjectionNested( publicMap, project, extend );
            list.add( publicMap );
        }
        if (returnFavorite) {
            User user = (User) SecurityUtils.getSubject().getPrincipal();
            list = FavoriteService.getInstance().addFavoritesToList(list,user.getId(),"smartcontractparty");
        }
        addToResults(list, extend, project, visibilityGroupId, upVisibility, downVisibility, returnFavorite);
        Map<String,Object> mapResponse = new HashMap<String,Object>();
        mapResponse.put( "total", count );
        mapResponse.put( "results", list );
        return RestUtils.sendOkResponse( mapResponse );
    }

    public boolean includeInSelect( SmartContractParty smartContractParty )
    {
        return true;
    }

    protected Response selectSmartContractPartys(Long id, String extra,  String only,  String extend, String project)
    {
        SmartContractParty smartContractParty = SmartContractPartyService.getInstance().get( id );
        if( smartContractParty == null )
        {
            return RestUtils.sendBadResponse( String.format( "SmartContractPartyId[%d] not found", id) );
        }
        // 2. Limit visibility based on user's group and the object's group (group based authorization)
        EntityVisibility entityVisibility = getEntityVisibility();
        GeneralVisibilityUtils.limitVisibilitySelect(entityVisibility, smartContractParty);
        validateSelect( smartContractParty );
        // 5a. Implement extra
        Map<String,Object> publicMap = QueryUtils.mapWithExtraFields( smartContractParty, extra, getExtraPropertyNames());
        publicMap = QueryUtils.mapWithExtraFieldsNested( smartContractParty, publicMap, extend, getExtraPropertyNames());
        addToPublicMap(smartContractParty, publicMap, extra);
        // 5b. Implement only
        QueryUtils.filterOnly( publicMap, only, extra );
        QueryUtils.filterProjectionNested( publicMap, project, extend );
        return RestUtils.sendOkResponse( publicMap );
    }

    public void validateSelect( SmartContractParty smartContractParty )
    {

    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value = {"smartContractParty:r:{id}"})
    @ApiOperation(position = 2, value = "Select a SmartContractParty (AUTO)")
    public Response selectSmartContractPartys(
            @PathParam("id") Long id, @Deprecated @QueryParam("extra") String extra,
            @Deprecated @QueryParam("only") String only,
            @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend,
            @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project,
            @QueryParam("createRecent") @DefaultValue("false") Boolean createRecent) {

        Response response = this.selectSmartContractPartys(id, extra, only, extend, project);
        SmartContractParty party = SmartContractPartyService.getInstance().get(id);
        if (createRecent) {
            RecentService.getInstance().insertRecent(party.getId(), party.getName(), "smartContractParty",
                                                     party.getGroup());
        }
        return response;
    }

    protected Response insertSmartContractParty( Map<String, Object> map )
    {
        // 2. Limit visibility based on user's group and the object's group (group based authorization)
        EntityVisibility entityVisibility = getEntityVisibility();
        GeneralVisibilityUtils.limitVisibilityInsert(entityVisibility, VisibilityUtils.getObjectGroup(map));
        SmartContractParty smartContractParty = new SmartContractParty();
        // 7. handle insert and update
        BeanUtils.setProperties( map, smartContractParty );
        // 6. handle validation in an Extensible manner
        validateInsert( smartContractParty );
        SmartContractPartyService.getInstance().insert( smartContractParty );
        // 1[b,c]. Restrict access based on OBJECT and PROPERTY level read permissions
        Map<String,Object> publicMap = smartContractParty.publicMap();
        return RestUtils.sendCreatedResponse( publicMap );
    }

    public void validateInsert( SmartContractParty smartContractParty )
    {

    }

    @PUT
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresPermissions(value = {"smartContractParty:i"})
    @ApiOperation(position = 3, value = "Insert a SmartContractParty (AUTO)")
    public Response insertSmartContractParty(
            Map<String, Object> map,
            @QueryParam("createRecent") @DefaultValue("false") Boolean createRecent) {
        Response response = this.insertSmartContractParty(map);
        try {
            Map<String, Object> publicMap = (Map<String, Object>) response.getEntity();
            SmartContractParty party = SmartContractPartyService.getInstance().get((Long) publicMap.get("id"));
            if (createRecent) {
                RecentService.getInstance().insertRecent(party.getId(), party.getName(), "smartContractParty",
                                                         party.getGroup());
            }
        } catch (ClassCastException cce) {
            logger.warn("Failed to recreate SmartContractParty entity to store it as recent", cce);
        }
        return response;
    }

    @PATCH
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresPermissions(value={"smartContractParty:u:{id}"})
    @ApiOperation(position=4, value="Update a SmartContractParty (AUTO)")
    public Response updateSmartContractParty( @PathParam("id") Long id, Map<String, Object> map )
    {
        SmartContractParty smartContractParty = SmartContractPartyService.getInstance().get( id );
        if( smartContractParty == null )
        {
            return RestUtils.sendBadResponse( String.format( "SmartContractPartyId[%d] not found", id) );
        }
        // 2. Limit visibility based on user's group and the object's group (group based authorization)
        EntityVisibility entityVisibility = getEntityVisibility();
        GeneralVisibilityUtils.limitVisibilityUpdate( entityVisibility, smartContractParty, VisibilityUtils.getObjectGroup(map));
        // 7. handle insert and update
        BeanUtils.setProperties( map, smartContractParty );
        // 6. handle validation in an Extensible manner
        validateUpdate( smartContractParty );
        SmartContractPartyService.getInstance().update( smartContractParty );
        Map<String,Object> publicMap = smartContractParty.publicMap();
        return RestUtils.sendOkResponse( publicMap );
    }

    public void validateUpdate( SmartContractParty smartContractParty )
    {

    }



    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"smartContractParty:d:{id}"})
    // 1a. Limit access based on CLASS level resources
    @ApiOperation(position=5, value="Delete a SmartContractParty (AUTO)")
    public Response deleteSmartContractParty( @PathParam("id") Long id )
    {
        // 1c. TODO: Restrict access based on OBJECT level read permissions
        SmartContractParty smartContractParty = SmartContractPartyService.getInstance().get( id );
        if( smartContractParty == null )
        {
            return RestUtils.sendBadResponse( String.format( "SmartContractPartyId[%d] not found", id) );
        }
        // 2. Limit visibility based on user's group and the object's group (group based authorization)
        EntityVisibility entityVisibility = getEntityVisibility();
        GeneralVisibilityUtils.limitVisibilityDelete(entityVisibility, smartContractParty );
        // handle validation in an Extensible manner
        validateDelete( smartContractParty );
        SmartContractPartyService.getInstance().delete( smartContractParty );
        return RestUtils.sendDeleteResponse();
    }

    public void validateDelete( SmartContractParty smartContractParty )
    {

    }

    public void addToPublicMap(SmartContractParty smartContractParty, Map<String,Object> publicMap, String extra )
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
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(SmartContractParty.class.getCanonicalName(), visibilityGroupId);
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
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(SmartContractParty.class.getCanonicalName(), visibilityGroupId);
        RiotShiroRealm.getOverrideVisibilityCache().put(Group.class.getCanonicalName(), visibilityGroup);
        return c.listGroupsInTree(pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility, downVisibility, topId, false, extend, project);
    }

    public void validateListPermissions() {
        if (!(PermissionsUtils.isPermitted(SecurityUtils.getSubject(),"smartContractParty:r"))) {
            throw new com.tierconnect.riot.sdk.servlet.exception.ForbiddenException("Not Authorized, Access Denied");
        }
    }

}