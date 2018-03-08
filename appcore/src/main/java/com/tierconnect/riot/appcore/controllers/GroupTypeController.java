package com.tierconnect.riot.appcore.controllers;

import java.text.Normalizer;
import java.util.*;

import javax.ws.rs.*;

import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.services.FavoriteService;
import com.tierconnect.riot.appcore.services.RecentService;
import com.tierconnect.riot.appcore.utils.*;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.jaxrs.PATCH;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.GroupTypeService;
import com.tierconnect.riot.sdk.dao.Pagination;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.servlet.exception.ForbiddenException;
import com.tierconnect.riot.sdk.utils.RestUtils;

import static com.tierconnect.riot.sdk.dao.QueryDSLUtils.getPath;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.apache.shiro.subject.Subject;

@Path("/groupType")
@Api("/groupType")
public class GroupTypeController extends GroupTypeControllerBase {

    private static Logger logger = Logger.getLogger(GroupTypeController.class);

    static QGroupType qGroupType = QGroupType.groupType;

    private String[] IGNORE_GROUP_TYPE_FIELDS = new String[]{"children", "selected", "treeLevel"};

    @GET
    @Path("/tree")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"groupType:r"})
    @ApiOperation(position=1, value="Get a List of Group Types in Tree")
    public Response listGroupTypesInTree(@QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber, @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only
            , @QueryParam("visibilityGroupId") Long visibilityGroupId, @DefaultValue("") @QueryParam("upVisibility") String upVisibility, @DefaultValue("") @QueryParam("downVisibility") String downVisibility
            , @QueryParam("topId") String topId
            , @DefaultValue("false") @QueryParam("returnFavorite") boolean returnFavorite, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project)
    {
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(GroupType.class.getCanonicalName(), visibilityGroupId);
        Pagination pagination = new Pagination( pageNumber, pageSize );

        BooleanBuilder be = new BooleanBuilder();
        // 2. Limit visibility based on user's group and the object's group (group based authorization)
        EntityVisibility entityVisibility = getEntityVisibility();
        boolean up = StringUtils.isEmpty(upVisibility) ? entityVisibility.isNotFalseUpVisibility() : entityVisibility.isNotFalseUpVisibility() && Boolean.parseBoolean(upVisibility);
        boolean down = StringUtils.isEmpty(downVisibility) ? entityVisibility.isDownVisibility() : entityVisibility.isDownVisibility() && Boolean.parseBoolean(downVisibility);
        be = be.and(GeneralVisibilityUtils.limitVisibilitySelectAll(entityVisibility, QGroupType.groupType, visibilityGroup, upVisibility, downVisibility));
        // 4. Implement filtering
        be = be.and( QueryUtils.buildSearch( QGroupType.groupType, where ) );

        Long count = 0L;

        GroupType topGroupType = visibilityGroup.getGroupType();
//        GroupType topGroupType = null;
//        if (StringUtils.isNotEmpty(topId)) {
//            topGroupType = GroupTypeService.getInstance().get(Long.parseLong(topId));
//        }

        TreeParameters<GroupType> treeParameters = new TreeParameters<>();
        treeParameters.setOnly(only);
        treeParameters.setExtra(extra);
        treeParameters.setOrder(order);
        treeParameters.setTopObject(topGroupType);
        treeParameters.setVisibilityGroup(visibilityGroup);

        if (StringUtils.isEmpty(where)) {
            if (topGroupType == null) {
                be = be.and(QGroupType.groupType.parent.isNull());
            } else {
                be = be.and(QGroupType.groupType.id.eq(topGroupType.getId()));
            }
            count = GroupTypeService.getInstance().countList( be );
            List<GroupType> thingList = GroupTypeService.getInstance().listPaginated(be, pagination, order);
            for( GroupType thing : thingList)
            {
                mapThing(thing, treeParameters, false);
                addAllDescendants(thing, treeParameters, false, up, down);
            }
        } else {
            count = GroupTypeService.getInstance().countList( be );
            List<GroupType> thingList = GroupTypeService.getInstance().listPaginated(be, pagination, order);
            for( GroupType thing : thingList)
            {
                mapThing(thing, treeParameters, true);
            }
        }
        if (returnFavorite) {
            Subject currentUser = SecurityUtils.getSubject();
            User user = (User) currentUser.getPrincipal();
            FavoriteService.getInstance().addFavoritesForGroup(treeParameters.getList(), user.getId(), "grouptype");
        }
        // 3. Implement pagination
        Map<String,Object> mapResponse = new HashMap<>();
        mapResponse.put( "total", count );
        mapResponse.put( "results", treeParameters.getList() );
        return RestUtils.sendOkResponse( mapResponse );
    }


    public void mapThing(GroupType object, TreeParameters<GroupType> treeParameters, boolean selected) {
        Map<Long, Map<String, Object>> objectCache = treeParameters.getObjectCache();
        Map<String, Boolean> permissionCache = treeParameters.getPermissionCache();
        Map<Long, Set<Long>> childrenMapCache = treeParameters.getChildrenMapCache();
        List<Map<String, Object>> list = treeParameters.getList();
        String extra = treeParameters.getExtra();
        String only = treeParameters.getOnly();
        GroupType topGroupType = treeParameters.getTopObject();
        Integer level;
        //Map Thing
        Map<String, Object> objectMap  = objectCache.get(object.getId());
        if (objectMap == null) {
            objectMap = QueryUtils.mapWithExtraFields( object, extra, getExtraPropertyNames() );
            addToPublicMap(object, objectMap, extra);
            QueryUtils.filterOnly(objectMap, only, extra);
            QueryUtils.filterProjectionNested(objectMap, null, null);
            if (topGroupType == null) {
                if (object.getParent() == null) {
                    list.add(objectMap);
                }
            } else {
                if (object.getId().equals(topGroupType.getId())) {
                    list.add(objectMap);
                }
            }
            objectMap.put("children", new ArrayList<>());
            objectCache.put(object.getId(), objectMap);
        }
        if (selected) {
            objectMap.put("selected", Boolean.TRUE);
        }
        //Map parent
        GroupType parent = object.getParent();
        Map<String, Object> parentObjectMap = null;
        if (parent != null) {
            parentObjectMap = objectCache.get(parent.getId());
            if (parentObjectMap == null) {
                mapThing(parent, treeParameters, false);
                parentObjectMap = objectCache.get(parent.getId());
            }
            level = (Integer) parentObjectMap.get("treeLevel") +1;
        } else {
            level = 1;
        }

        objectMap.put("treeLevel", level);

        //Add child to parent
        if (parentObjectMap != null) {
            Set childrenSet = childrenMapCache.get(parent.getId());
            if (childrenSet == null) {
                childrenSet = new HashSet();
                childrenMapCache.put(parent.getId(), childrenSet);
            }
            List childrenList = (List) parentObjectMap.get("children");
            if (!childrenSet.contains(object.getId())) {
                childrenSet.add(object.getId());
                childrenList.add(objectMap);
            }
        }
    }

    public void addAllDescendants(GroupType base, TreeParameters<GroupType> treeParameters, boolean selected, boolean up, boolean down) {
        BooleanBuilder be = new BooleanBuilder();
        be = be.and( VisibilityUtils.limitVisibilityPredicate(treeParameters.getVisibilityGroup(), QGroupType.groupType.group, up, down) );
        be = be.and( QGroupType.groupType.parent.eq(base) );
        List<GroupType> children = GroupTypeService.getInstance().listPaginated(be, null, treeParameters.getOrder());
        for (GroupType child : children) {
            if (!child.getId().equals(base.getId())) {
                mapThing(child, treeParameters, selected);
                addAllDescendants(child, treeParameters, selected, up, down);
            }
        }
    }


    @PUT
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"groupType:i"})
    @ApiOperation(value="Insert GroupType")
	public Response insertGroupType(Map<String, Object> params,@QueryParam("createRecent") @DefaultValue("false") Boolean createRecent) {
		User currentUser = (User) SecurityUtils.getSubject().getPrincipal();
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(GroupType.class.getCanonicalName(), null);

		GroupType groupType = new GroupType();
		groupType.setCode((String) params.get(getPath(qGroupType.code)));
		groupType.setDescription((String) params.get(getPath(qGroupType.description)));
		Object parentId = params.get(getPath(qGroupType.parent.id));
        GroupTypeService groupTypeService = GroupTypeService.getInstance();
        if (parentId != null) {
			GroupType parentGroupType = groupTypeService.getGroupTypeDAO().selectById(Long.valueOf(parentId.toString()));
			if (parentGroupType == null) {
				throw new UserException(String.format("Group Type[%d] not found", parentId));
			}
			groupType.setParent(parentGroupType);
		} else {
			throw new UserException("You need to define a Parent Group Type");
		}
		Object groupId = params.get(getPath(qGroupType.group.id));
		if (groupId != null) {
			Group group = GroupService.getInstance().getGroupDAO().selectById(Long.valueOf(groupId.toString()));
			if (group == null) {
				throw new UserException(String.format("Group[%d] not found", parentId));
			}
			if (GroupService.getInstance().isGroupNotInsideTree(group, groupType.getParent().getGroup())) {
				throw new ForbiddenException(String.format("Forbidden Group[%d]", group.getId()));
			}
			if (GroupService.getInstance().isGroupNotInsideTree(group, visibilityGroup)) {
				throw new ForbiddenException(String.format("Forbidden Group[%d]", group.getId()));
			}						
			groupType.setGroup(group);
            if (!groupTypeService.isGroupTypeInsideTree(groupType.getParent(), groupType.getGroup().getGroupType())) {
                throw new UserException(String.format("Yo should use a group in the hierarchy, Forbidden Group[%d]", group.getId()));
            }
        } else {
			throw new UserException("You need to define a Group");			
		}
		String paramName = (String) params.get(getPath(qGroupType.name));
		if (paramName != null) {
            groupType.setName(paramName);
            groupType.setCode(Normalizer.normalize(paramName.replaceAll(" ", ""), Normalizer.Form.NFD)
                    .replaceAll("[^\\p{ASCII}]", ""));
		} else {
			throw new UserException("You need to define a Name for the Group Type");			
		}
		groupTypeService.insert(groupType);

        if (createRecent){
            RecentService.getInstance().insertRecent(groupType.getId(), groupType.getName(),"grouptype", groupType.getGroup());
        }

        sendMQTTSignal(GroupService.getInstance().getMqttGroups(groupType.getGroup()));
        groupTypeService.refreshCache(groupType, false);

		return RestUtils.sendCreatedResponse(groupType.publicMap());
	}
     
    @PATCH
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value="Update GroupType")
    @RequiresPermissions(value={"groupType:u"})
    public Response updateGroupType(@PathParam("id") Long id, Map<String, Object> params) {
		User currentUser = (User) SecurityUtils.getSubject().getPrincipal();
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(GroupType.class.getCanonicalName(), null);

		GroupType groupType = GroupTypeService.getInstance().getGroupTypeDAO().selectById(id);
		if (groupType == null) {
			throw new UserException(String.format("GroupType[%d] not found", id)); // This is transformed to 404
		}		
		if(GroupTypeService.getInstance().countList(isVisibleGroupTypeForWrite(visibilityGroup, groupType)) == 0){
			throw new ForbiddenException(String.format("Forbidden GroupType[%d]", groupType.getId()));
		}		
		if (params.containsKey(getPath(qGroupType.code))) {
			groupType.setCode((String) params.get(getPath(qGroupType.code)));
		}		
		if (params.containsKey(getPath(qGroupType.description))) {
			groupType.setDescription((String) params.get(getPath(qGroupType.description)));
		}		
		if (params.containsKey(getPath(qGroupType.name))) {
			groupType.setName((String) params.get(getPath(qGroupType.name)));
			groupType.setCode(Normalizer.normalize(((String) params.get(getPath(qGroupType.name))).replaceAll(" ", ""),
                    Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", ""));
		}
		GroupTypeService.getInstance().update(groupType);
        RecentService.getInstance().updateName(groupType.getId(), groupType.getName(),"grouptype");

        sendMQTTSignal(GroupService.getInstance().getMqttGroups(groupType.getGroup()));
        GroupTypeService.getInstance().refreshCache(groupType, false);

        return RestUtils.sendOkResponse(groupType.publicMap());
    }


    private static BooleanBuilder isVisibleGroupTypeForWrite(Group rootGroup, GroupType groupType) {
        BooleanBuilder b = new BooleanBuilder();
        b = b.and(GroupService.getInstance().getDescendantsIncludingPredicate(qGroupType.group, rootGroup));
		b = b.and(qGroupType.eq(groupType));
		return b;
	}

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"groupType:d:{id}"})
    @ApiOperation(position=5, value="Delete a GroupType")
    public Response deleteGroupType( @PathParam("id") Long id )
    {
        // 1c. TODO: Restrict access based on OBJECT level read permissions
        GroupType groupType = GroupTypeService.getInstance().get( id );
        if( groupType == null )
        {
            return RestUtils.sendBadResponse( String.format( "GroupTypeId[%d] not found", id) );
        }
        // 2. Limit visibility based on user's group and the object's group (group based authorization)
        EntityVisibility entityVisibility = getEntityVisibility();
        GeneralVisibilityUtils.limitVisibilityDelete(entityVisibility, groupType );
        // handle validation in an Extensible manner
        validateDelete( groupType );
        List<Long> groupMqtt = GroupService.getInstance().getMqttGroups(groupType.getGroup());
        GroupTypeService.getInstance().delete( groupType );
        RecentService.getInstance().deleteRecent(id,"grouptype");

        sendMQTTSignal(groupMqtt);
        GroupTypeService.getInstance().refreshCache(groupType, true);

        return RestUtils.sendDeleteResponse();
    }


    @Override
    public List<String> getExtraPropertyNames() {
        return Arrays.asList(IGNORE_GROUP_TYPE_FIELDS);
    }

    /**
     * Sending message to refresh groupTypes in coreBridge
     */
    public void sendMQTTSignal(List<Long> groupMqtt)
    {
        try
        {
            Class clazz = Class.forName("com.tierconnect.riot.iot.services.BrokerClientHelper");
            clazz.getMethod("sendRefreshGroupTypesMessage", Boolean.class, String.class, List.class)
                    .invoke(null, false, Thread.currentThread().getName(), groupMqtt);
        }
        catch (Exception e) {
            logger.error("Could not call MQTT sendRefreshGroupTypesMessage method");
        }
    }

}
