package com.tierconnect.riot.appcore.controllers;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.services.*;
import com.tierconnect.riot.appcore.utils.*;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.sdk.dao.NotFoundException;
import com.tierconnect.riot.sdk.dao.Pagination;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.servlet.exception.ForbiddenException;
import com.tierconnect.riot.sdk.utils.PermissionsUtils;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.jaxrs.PATCH;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.subject.Subject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

import static com.tierconnect.riot.sdk.dao.QueryDSLUtils.getPath;

/**
 * 
 * @author agutierrez
 *R
 */

@Path("/group")
@Api("/group")
public class GroupController extends GroupControllerBase{
    static Logger logger = Logger.getLogger(GroupController.class);

	static QGroup qGroup = QGroup.group;
	static QGroupField qGroupField = QGroupField.groupField;

    // Return a Tree view of Groups
    @GET
    @Path("/tree")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"group:r"})
    @ApiOperation(value="Get a Tree List of Groups")
    public Response listGroupsInTree(@QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber, @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only
            , @QueryParam("visibilityGroupId") Long visibilityGroupId, @DefaultValue("") @QueryParam("upVisibility") String upVisibility, @DefaultValue("") @QueryParam("downVisibility") String downVisibility
            , @QueryParam("topId") String topId
            , @DefaultValue("false") @QueryParam("returnFavorite") boolean returnFavorite, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project)
    {
        long starTime = System.currentTimeMillis();
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(Group.class.getCanonicalName(), visibilityGroupId);
        Pagination pagination = new Pagination( pageNumber, pageSize );

        BooleanBuilder be = new BooleanBuilder();
        // 2. Limit visibility based on user's group and the object's group (group based authorization)
        EntityVisibility entityVisibility = getEntityVisibility();
        boolean up = StringUtils.isEmpty(upVisibility) ? entityVisibility.isNotFalseUpVisibility() : entityVisibility.isNotFalseUpVisibility() && Boolean.parseBoolean(upVisibility);
        boolean down = StringUtils.isEmpty(downVisibility) ? entityVisibility.isDownVisibility() : entityVisibility.isDownVisibility() && Boolean.parseBoolean(downVisibility);
        be = be.and(GeneralVisibilityUtils.limitVisibilitySelectAll(entityVisibility, QGroup.group, visibilityGroup, upVisibility, downVisibility));
        // 4. Implement filtering
        be = be.and(QueryUtils.buildSearch(QGroup.group, where));

        int treeLevel = visibilityGroup.getTreeLevel();
        Long count;

        Group topGroup = null;
        if (StringUtils.isNotEmpty(topId)) {
            topGroup = GroupService.getInstance().get(Long.parseLong(topId));
        }

        TreeParameters<Group> treeParameters = new TreeParameters<>();
        treeParameters.setOnly(only);
        treeParameters.setExtra(extra);
        treeParameters.setOrder(order);
        treeParameters.setTopObject(topGroup);
        treeParameters.setVisibilityGroup(visibilityGroup);
        treeParameters.setUpVisibility(up);
        treeParameters.setDownVisibility(down);

        if (StringUtils.isEmpty(where)) {
            if (topGroup == null) {
                be = be.and(QGroup.group.parent.isNull());
            } else {
                be = be.and(QGroup.group.id.eq(topGroup.getId()));
            }
            count = GroupService.getInstance().countList( be );
            List<Group> groupList = GroupService.getInstance().listPaginated( be, pagination, order );
            for (Group group : groupList) {
                mapGroup(group, treeParameters, treeLevel, false);
                addAllDescendants(group, treeParameters, treeLevel, false, up, down);
            }
        } else {
            count = GroupService.getInstance().countList( be );
            List<Group> groupList = GroupService.getInstance().listPaginated( be, pagination, order );
            for (Group group : groupList) {
                mapGroup(group, treeParameters, treeLevel, true);
            }
        }
        if (returnFavorite) {
            Subject currentUser = SecurityUtils.getSubject();
            User user = (User) currentUser.getPrincipal();
            FavoriteService.getInstance().addFavoritesForGroup(treeParameters.getList(), user.getId(), "group");
        }
        Map<String,Object> mapResponse = new HashMap<>();
        mapResponse.put( "total", count );
        mapResponse.put( "results", treeParameters.getList() );
        logger.info("GROUP TREE FINISHED ELAPSED" + (System.currentTimeMillis() - starTime));
        return RestUtils.sendOkResponse( mapResponse );
    }

    public void mapGroup(Group object, TreeParameters<Group> treeParameters, int treeLevel, boolean selected) {
        Map<Long, Map<String, Object>> objectCache = treeParameters.getObjectCache();
        Map<String, Boolean> permissionCache = treeParameters.getPermissionCache();
        Map<Long, Set<Long>> childrenMapCache = treeParameters.getChildrenMapCache();
        List<Map<String, Object>> list = treeParameters.getList();
        String extra = treeParameters.getExtra();
        String only = treeParameters.getOnly();
        Group topGroup = treeParameters.getTopObject();

        //Map Group
        Map<String, Object> objectMap  = objectCache.get(object.getId());
        if (objectMap == null) {
            objectMap = QueryUtils.mapWithExtraFields( object, extra, getExtraPropertyNames() ); //parent and type can be added as extra field
            addToPublicMap(object, objectMap, extra);
            QueryUtils.filterOnly(objectMap, only, extra);
            QueryUtils.filterProjectionNested(objectMap, null, null);
            //if (object.getTreeLevel() == treeLevel + 1) {
            if (topGroup == null) {
                if (treeParameters.isUpVisibility()) {
                    if (object.getParent() == null) {
                        list.add(objectMap);
                    }
                } else if (object.getTreeLevel() == treeLevel) {
                    list.add(objectMap);
                }
            } else {
                if (object.getId().equals(topGroup.getId())) {
                    list.add(objectMap);
                }
            }
            //objectMap.put("childrenMap", new HashMap<>());
            objectMap.put("children", new ArrayList<>());
            objectCache.put(object.getId(), objectMap);
        }
        if (selected) {
            objectMap.put("selected", Boolean.TRUE);
        }
        //Map parent
        Group parent = object.getParent();
        Map<String, Object> parentObjectMap = null;
        if (parent != null) {
            parentObjectMap = objectCache.get(parent.getId());
            //if (parentObjectMap == null && object.getTreeLevel() > treeLevel + 1) { //parent.getTreeLevel > treeLevel
            if (parentObjectMap == null && (treeParameters.isUpVisibility() || object.getTreeLevel() >= treeLevel)) {
                mapGroup(parent, treeParameters, treeLevel, false);
                parentObjectMap = objectCache.get(parent.getId());
            }
        }

        //Add child to parent
        if (parentObjectMap != null) {
            //Map childrenMap = (Map) parentObjectMap.get("childrenMap");
//            if (!childrenMap.containsKey(group.getId())) {
//                childrenMap.put(group.getId(), objectMap);
//                childrenList.add(objectMap);
//            }
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

    public void addAllDescendants(Group base, TreeParameters<Group> treeParameters, int treeLevel, boolean selected, boolean up, boolean down) {
        BooleanBuilder be = new BooleanBuilder();
        be = be.and( VisibilityUtils.limitVisibilityPredicate(treeParameters.getVisibilityGroup(), QGroup.group, up, down) );
        be = be.and( GroupService.getInstance().getQParentGroupLevel(QGroup.group, base.getTreeLevel()).eq(base));
        String order = (treeParameters.getOrder() == null ? "treeLevel:asc" : "treeLevel:asc,"+treeParameters.getOrder());
        List<Group> children = GroupService.getInstance().listPaginated(be, null, order);

        for (Group child: children) {
            if (!child.getId().equals(base.getId())) {
                mapGroup(child, treeParameters, treeLevel, selected);
            }
        }
    }

    @PUT
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"group:i"})
    @ApiOperation(value="Create Group")
    @Override
	public Response insertGroup(Map<String, Object> params,@QueryParam("createRecent") @DefaultValue("false") Boolean createRecent)
    {
        Group group = GroupService.getInstance().createGroup(params);
        if (createRecent){
            RecentService.getInstance().insertRecent(group.getId(), group.getName(),"group", group.getTenantGroup());
        }
        // RIOT-13659 enable tickles for groups.
        GroupService.getInstance().sendMQTTSignal(GroupService.getInstance().getMqttGroups(group));
        GroupService.getInstance().refreshGroupCache(group, false);
		return RestUtils.sendCreatedResponse(group.publicMap(true));
	}



//    @Override
//    public void validateInsert(Group group) {
//        super.validateInsert(group);
//        User currentUser = (User) SecurityUtils.getSubject().getPrincipal();
//        Group currentGroup = currentUser.getGroup();
//        if (group.getParent() == null) {
//            throw new NotFoundException(String.format("Parent Group is required."));
//        }
//        if(GroupService.getInstance().isGroupNotInsideTree(group.getParent(), currentGroup)){
//            throw new ForbiddenException(String.format("Forbidden Group[%d]", group.getParent().getId()));
//        }
//        if (group.getGroupType() == null) {
//            throw new NotFoundException(String.format("Group Type is required."));
//        }
//    }

    @PATCH
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"group:u:{id}"})
    @ApiOperation(value="Update Group")
    @Override
    public Response updateGroup(@PathParam("id") Long id, Map<String, Object> params) {
        User currentUser = (User) SecurityUtils.getSubject().getPrincipal();
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(Group.class.getCanonicalName(), null);
		
		Group group = GroupService.getInstance().getGroupDAO().selectById(id);
		if (group == null) {
			throw new NotFoundException(String.format("Group[%d] not found", id));
		}
		if(GroupService.getInstance().isGroupNotInsideTree(group, visibilityGroup)){
			throw new ForbiddenException(String.format("Forbidden Group[%d]", group.getId()));
		}		
		if (params.containsKey(getPath(qGroup.archived))) {
			group.setArchived((Boolean) params.get(getPath(qGroup.archived)));
		}		
		if (params.containsKey(getPath(qGroup.name))) {
			group.setName((String) params.get(getPath(qGroup.name)));
		}
		if (params.containsKey(getPath(qGroup.code))) {
			group.setCode((String) params.get(getPath(qGroup.code)));
		}
		if (params.containsKey(getPath(qGroup.description))) {
			group.setDescription((String) params.get(getPath(qGroup.description)));
		}
		if (params.containsKey(getPath(qGroup.parent.id))) {
			throw new UserException("It is not supported to change the parent Group");
//			Object parentId = params.get(getPath(qGroup.parent.id));
//			if (parentId == null) {
//				if (group.getParent() != null) {
//					throw new UserException("It is not supported to change the parent Group");
//			    }
//			} 
//			else {
//				Group parentGroup = GroupService.getInstance().getGroupDAO().selectById(Long.valueOf(parentId.toString()));
//				if (parentGroup == null) {
//					throw new UserException(String.format("Parent Group[%d] not found", parentId));
//				}				
//				if (group.getParent() == null || !parentGroup.getId().equals(group.getParent().getId())) {
//					throw new UserException("It is not supported to change the parent Group");
//			    }
//			}
//			if (parentId == null) {
//				throw new UserException("You need to define a Parent Group");
//			}
//			Group parentGroup = GroupService.getInstance().getGroupDAO().selectById(Long.valueOf(parentId.toString()));
//			if (parentGroup == null) {
//				throw new UserException(String.format("Parent Group[%d] not found", parentId));
//			}
//			if(!GroupService.getInstance().isGroupInTree(parentGroup, currentGroup)){
//				throw new ForbiddenException(String.format("Forbidden Group[%d]", group.getId()));
//			}		
//			group.setParent(parentGroup);
		}
		if (params.containsKey(getPath(qGroup.groupType.id))) {
			Object typeId = params.get(getPath(qGroup.groupType.id));
			if (typeId != null) {
				GroupType type = GroupTypeService.getInstance().getGroupTypeDAO().selectById(Long.valueOf(typeId.toString()));
				if (type == null) {
					throw new UserException(String.format("Type [%d] not found", typeId));
				}
				if (!GroupType.equalIds(group.getGroupType().getParent(), type.getParent())) {
					throw new UserException("It is not supported to change the GroupType unless is a sibling");					
				}
				group.setGroupType(type);
			}
		}
		GroupService.getInstance().update(group);
        RecentService.getInstance().updateName(group.getId(), group.getName(),"group");

        // RIOT-13659 enable tickles for groups.
        GroupService.getInstance().putOneInCache(group);
        GroupService.getInstance().sendMQTTSignal(GroupService.getInstance().getMqttGroups(group));
        GroupService.getInstance().refreshGroupCache(group, false);

        return RestUtils.sendOkResponse(group.publicMap(true));
    }


    @PUT
    @Path("/{id}/field/{field}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(value="Update GroupField")
    public Response setGroupField(@PathParam("id") Long groupId, @PathParam("field") String fieldParam, Map<String, Object> params) {
    	if (!params.containsKey(getPath(qGroupField.value))) {
    		throw new UserException("parameter value is required");
    	}
        Map<String, Object> params2 = new HashMap<>();
        params2.put(fieldParam, params.get(getPath(qGroupField.value)));
        List result = GroupService.getInstance().setGroupFieldsBase(groupId, params2);
        return RestUtils.sendCreatedResponse(result.get(0));
    }

    @GET
    @Path("/{id}/field/{field}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(value="Get a GroupField")
    public Response getGroupField(@PathParam("id") Long groupId, @PathParam("field") String fieldParam) {
        User currentUser = (User) SecurityUtils.getSubject().getPrincipal();
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(Group.class.getCanonicalName(), null);
        Group group = GroupService.getInstance().get(groupId);
        if (group == null) {
            throw new NotFoundException(String.format("Group[%d] not found", groupId));
        }
        Field field;
        try {
            field = FieldService.getInstance().get(Long.valueOf(fieldParam));
        } catch (Exception ex) {
            field = FieldService.getInstance().selectByName(fieldParam);
        }
        if (field == null) {
            throw new NotFoundException(String.format("Field[%s] not found", fieldParam));
        }
        GroupField groupField = GroupFieldService.getInstance().selectByGroupField(group, field);
        String[] permissions = new String[] {"group:r:"+group.getId()};
        if (!PermissionsUtils.isPermittedAny(SecurityUtils.getSubject(), Arrays.asList(permissions))) {
            throw new ForbiddenException(String.format("Forbidden GroupField[%d]", groupField.getId()));
        }
        if(GroupService.getInstance().isGroupNotInsideTree(group, visibilityGroup)){
            throw new ForbiddenException(String.format("Forbidden Group[%d]", group.getId()));
        }
//        if(GroupService.getInstance().isGroupNotInsideTree(field.getGroup(), visibilityGroup)){
//            throw new ForbiddenException(String.format("Forbidden Field[%d]", field.getId()));
//        }
        return RestUtils.sendCreatedResponse(groupField.publicMap(true));
    }

    @DELETE
    @Path("/{id}/field/{field}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(value="Delete GroupField")
    public Response deleteGroupField(@PathParam("id") Long groupId, @PathParam("field") String fieldParam) {
        User currentUser = (User) SecurityUtils.getSubject().getPrincipal();
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(Group.class.getCanonicalName(), null);
    	Group group = GroupService.getInstance().get(groupId);
        if (group == null) {
			throw new NotFoundException(String.format("Group[%d] not found", groupId));
        }
        Field field;
        try {
            field = FieldService.getInstance().get(Long.valueOf(fieldParam));
        } catch (Exception ex) {
            field = FieldService.getInstance().selectByName(fieldParam);
        }
        if (field == null) {
            throw new NotFoundException(String.format("Field[%s] not found", fieldParam));
        }
        GroupField groupField = GroupFieldService.getInstance().selectByGroupField(group, field);
        if (groupField == null) {
            return RestUtils.sendDeleteResponse();
        }
        String[] permissions = new String[] {"group:i:"+group.getId(),"group:u:"+group.getId()};
        if (!PermissionsUtils.isPermittedAny(SecurityUtils.getSubject(), Arrays.asList(permissions))) {
            throw new ForbiddenException(String.format("Forbidden GroupField[%d]", groupField.getId()));
        }
        if(GroupService.getInstance().isGroupNotInsideTree(group, visibilityGroup)){
            throw new ForbiddenException(String.format("Forbidden Group[%d]", group.getId()));
        }
        RecentService.getInstance().deleteRecent(groupId, "group");
//        if(GroupService.getInstance().isGroupNotInsideTree(field.getGroup(), visibilityGroup)){
//            throw new ForbiddenException(String.format("Forbidden Field[%d]", field.getId()));
//        }
        GroupFieldService.getInstance().unset(group, field);
        return RestUtils.sendDeleteResponse();
    }

    
	@GET
	@Path("/{id}/fields")
	@Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"group:r:{id}"}, logical = Logical.OR)
    @ApiOperation(value="Get GroupFields")
	public Response listGroupFields(@PathParam("id") Long id, @QueryParam("pageSize") Integer pageSize,
			@QueryParam("pageNumber") Integer pageNumber) throws Exception {
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(Group.class.getCanonicalName(), null);
    	Group group = GroupService.getInstance().get(id);
        if (group == null) {
			throw new NotFoundException(String.format("Group[%d] not found", id));
        }    	
		if(GroupService.getInstance().isGroupNotInsideTree(group, visibilityGroup)){
			throw new ForbiddenException(String.format("Forbidden Group[%d]", group.getId()));
		}		
		Map<String, Object> mapResponse = new HashMap<String, Object>();
		Pagination pagination = new Pagination(pageNumber, pageSize);
		Long count = GroupFieldService.getInstance().countFieldsByGroup(group);
		List<GroupField> groupFields = GroupFieldService.getInstance().listFieldsPaginatedByGroup(pagination, group);
        List<Map<String, Object>> list = new LinkedList<>();
        LicenseService licenseService = LicenseService.getInstance();
		for (GroupField groupField: groupFields) {
            if (licenseService.isValidField(group, groupField.getField().getName())) {
                list.add(groupField.publicMap(true));
            }
		}
		mapResponse.put("total", count);

		mapResponse.put("results", list);
		return RestUtils.sendOkResponse(mapResponse);
	}

    @PUT
    @Path("/{id}/fields")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"group:u", "group:i"}, logical = Logical.OR)
    @ApiOperation(value="Set Group Fields")
    public  Response setGroupFields(@PathParam("id") Long groupId, Map<String, Object> m)
    {
        List list = null;
        try{
            list = GroupService.getInstance().setGroupFieldsBase(groupId, m);
        }catch(UserException e){
            return RestUtils.sendResponseWithCode(e.getMessage() , 400);
        }catch(Exception e2){
            return RestUtils.sendResponseWithCode(e2.getMessage() , 500);
        }
        return RestUtils.sendOkResponse(list);
    }





    @GET
    @Path("/{id}/emailTest")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"group:r:{id}"}, logical = Logical.OR)
    @ApiOperation(value="Get GroupFields")
    public Response emailTest(@PathParam("id") Long id) throws Exception, UserException {
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(Group.class.getCanonicalName(), null);
        Group group = GroupService.getInstance().get(id);
        if (group == null) {
            throw new NotFoundException(String.format("Group[%d] not found", id));
        }
        if(GroupService.getInstance().isGroupNotInsideTree(group, visibilityGroup)){
            throw new ForbiddenException(String.format("Forbidden Group[%d]", group.getId()));
        }
        validationEmail(group, Constants.EMAIL_SMTP_USER);
        Map<String, Object> mapResponse = new HashMap<String, Object>();
        mapResponse.put("result", "OK");
        return RestUtils.sendOkResponse(mapResponse);
    }

    public void validationEmail (Group group, String email) throws Exception {
        EmailSender.SmtpParameters mailParameters = new EmailSender.SmtpParameters();
        mailParameters.setHost(ConfigurationService.getAsString(group, "emailSmtpHost"));
        Long emailSmtpPort = ConfigurationService.getAsLong(group, "emailSmtpPort");
        mailParameters.setPort(emailSmtpPort != null? emailSmtpPort.intValue(): 25);
        mailParameters.setSsl(ConfigurationService.getAsBoolean(group, "emailSmtpSsl", false));
        mailParameters.setTls(ConfigurationService.getAsBoolean(group, "emailSmtpTls", false));
        mailParameters.setUserName(ConfigurationService.getAsString(group, email));
        mailParameters.setPassword(ConfigurationService.getAsString(group, "emailSmtpPassword"));
        EmailSender e = new EmailSender(mailParameters);
        EmailSender.EmailMessageParameters messageParameters = new EmailSender.EmailMessageParameters();
        messageParameters.setFrom(mailParameters.getUserName());
        messageParameters.setToEmail(mailParameters.getUserName());
        messageParameters.setSubject("Test Mail");
        messageParameters.setMsg("test message");
        e.send(messageParameters);
    }

	@GET
	@Path("/{id}/inheritedFields")
	@Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(value="Get Derived GroupFields")
	public Response listMapDerivedGroupFields(@PathParam("id") Long id,  @DefaultValue("list") @QueryParam("format") String format) throws Exception {
        LicenseService licenseService = LicenseService.getInstance();
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(Group.class.getCanonicalName(), null);
    	Group group = GroupService.getInstance().get(id);
        if (group == null) {
			throw new NotFoundException(String.format("Group[%d] not found", id));
        }    	
		if(GroupService.getInstance().isGroupNotInsideTree(group, visibilityGroup)){
            if(GroupService.getInstance().isGroupNotInsideTree(visibilityGroup, group)) {
                throw new ForbiddenException(String.format("Forbidden Group[%d]", group.getId()));
            }
        }
		Map<String, Object> mapResponse = new HashMap<String, Object>();
		Map<Long, GroupField> groupFields = GroupFieldService.getInstance().listInheritedFieldsByGroup(group);
        Map<Long, GroupField> groupFieldsParent = group.getParent() == null ? new HashMap<Long, GroupField>() : GroupFieldService.getInstance().listInheritedFieldsByGroup(group.getParent());
		Long count = (long) groupFields.size();
        List<Map<String, Object>> listResult = new LinkedList<>();
        Map<String, Object> mapResult = new HashMap<>();
		for (Map.Entry<Long,GroupField> entry: groupFields.entrySet()) {
            GroupField groupField = entry.getValue();
            if (!licenseService.isValidField(group, groupField.getField().getName())) {
                continue;
            }
            mapResult.put(groupField.getField().getName(), groupField.publicMap(true));
            Map<String, Object> map = groupField.publicMap(true);
            if (groupFieldsParent.containsKey(groupField.getField().getId())) {
                GroupField groupFieldParent = groupFieldsParent.get(groupField.getField().getId());
                map.put("parentValue", groupFieldParent.getValue());
                map.put("parentGroup", groupFieldParent.getGroup().publicMap(false));
            } else {
                map.put("parentValue", null);
                map.put("parentGroup", null);
            }
            // set parent field
            List<Field> parentFields = FieldService.getFieldsByParentField(groupField.getField());
            if (parentFields != null && !parentFields.isEmpty()){
                List<Map<String,Object>> parentFieldsMap = new ArrayList<>();
                for (Field parentField : parentFields){
                    parentFieldsMap.add(parentField.publicMap());
                }
                Map <String,Object> fields = (Map) map.get("field");
                fields.put("values", parentFieldsMap);
            }
            listResult.add(map);
		}
		mapResponse.put("total", count);
		mapResponse.put("results", listResult);

        if ("list".equalsIgnoreCase(format)) {
            return RestUtils.sendOkResponse(mapResponse);
        } else {
            return RestUtils.sendOkResponse(mapResult);
        }
	}

    @GET
    @Path("/{id}/inheritedFields/{field}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(value="Get Derived GroupFields")
    public Response listDerivedGroupFields(@PathParam("id") Long id, @PathParam("field") String fieldParam) throws Exception {
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(Group.class.getCanonicalName(), null);
        Group group = GroupService.getInstance().get(id);
        if (group == null) {
            throw new NotFoundException(String.format("Group[%d] not found", id));
        }
        if(GroupService.getInstance().isGroupNotInsideTree(group, visibilityGroup)){
            if(GroupService.getInstance().isGroupNotInsideTree(visibilityGroup, group)) {
                throw new ForbiddenException(String.format("Forbidden Group[%d]", group.getId()));
            }
        }
        Field field;
        try {
            field = FieldService.getInstance().get(Long.valueOf(fieldParam));
        } catch (Exception ex) {
            field = FieldService.getInstance().selectByName(fieldParam);
        }

        if (field == null) {
            throw new NotFoundException(String.format("Field[%s] not found", fieldParam));
        }

        Map<Long, GroupField> groupFields = GroupFieldService.getInstance().listInheritedFieldsByGroup(group);
        Map<Long, GroupField> groupFieldsParent = group.getParent() == null ? new HashMap<Long, GroupField>() : GroupFieldService.getInstance().listInheritedFieldsByGroup(group.getParent());
        for (Map.Entry<Long,GroupField> entry: groupFields.entrySet()) {
            GroupField groupField = entry.getValue();
            Map<String, Object> map = groupField.publicMap(true);
            if (groupFieldsParent.containsKey(groupField.getField().getId())) {
                GroupField groupFieldParent = groupFieldsParent.get(groupField.getField().getId());
                map.put("parentValue", groupFieldParent.getValue());
                map.put("parentGroup", groupFieldParent.getGroup().publicMap(false));
            } else {
                map.put("parentValue", null);
                map.put("parentGroup", null);
            }
            if (field.getName().equals( ((Map) map.get("field")).get("name"))) {
                if (field.getName().equals("authenticationMode") &&
                        !AuthenticationUtils.getConfProperty("authentication.mode").isEmpty()){
                    String authe = AuthenticationUtils.getConfProperty("authentication.mode");
                    map.put("parentValue",authe);
                    map.put("value",authe);
                }
                return RestUtils.sendOkResponse(map);
            }
        }
        return RestUtils.sendEmptyResponse();
    }

    @GET
    @Path("/{id}/children")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"group:r:{id}"}, logical = Logical.OR)
    @ApiOperation(value="facilities for a company group")
    public Response listGroupFacilities(@PathParam("id") Long id) throws Exception {
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(Group.class.getCanonicalName(), null);
        Group group = GroupService.getInstance().get(id);
        if (group == null) {
            throw new NotFoundException(String.format("Group[%d] not found", id));
        }
        if(GroupService.getInstance().isGroupNotInsideTree(group, visibilityGroup)){
            throw new ForbiddenException(String.format("Forbidden Group[%d]", group.getId()));
        }

        Map<String, Object> mapResponse = new HashMap<String, Object>();

        //get company group for root
        if (group.getTreeLevel() == 1) {
            mapResponse.put("companies", getChildrenMaps(group));
        }
        //company group for company admin
        else if (group.getTreeLevel() == 2){
            mapResponse.put("facilities", getChildrenMaps(group));
            Map company = group.getParentLevel2().publicMap();
            company.put("type",  group.getParentLevel2().getGroupType().publicMap());
            mapResponse.put("companyGroup", company);
        }
        //else {
        //    mapResponse.put("companyGroup", group.getParentLevel2().publicMap());
        //}
        return RestUtils.sendOkResponse(mapResponse);
    }



    @Override
    public List<String> getExtraPropertyNames() {
        List<String> a = new ArrayList<>();
        a.add("children");
        a.add("childrenMap");
        a.add("selected");
        return a;
    }

    //todo put in util. repeated code from UserControl.
    private List<Map<String, Object>> getChildrenMaps(Group group) {
        List<Map<String, Object>> maps = new ArrayList<>();
        //get root companies
        for (Group _group : GroupService.getInstance().findByParent(group)) {
            Map<String, Object> groupMap = _group.publicMap(false);
            groupMap.put("groupType", _group.getGroupType().publicMap());
            maps.add(groupMap);
        }
        return maps;
    }

    @GET
    @Path("/limits")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 20, value = "Get a List of Limits for Groups")
    public Response verifyObjectLimits() {
        User user = (User) SecurityUtils.getSubject().getPrincipal();
        HashMap<String, Number> defaultHM = new HashMap<>();
        defaultHM.put("limit", -1);
        defaultHM.put("used", 0);
        Map<String, Map> mapResponse = new HashMap<String, Map>();
        HashMap<String, Number> level2Groups = new HashMap<>(defaultHM);
        mapResponse.put("level2Groups", level2Groups);
        HashMap<String, Number> level3Groups = new HashMap<>(defaultHM);
        mapResponse.put("level3Groups", level3Groups);
        if (LicenseService.enableLicense) {
            LicenseDetail licenseDetail = LicenseService.getInstance().getLicenseDetail(user.getActiveGroup(), true);
            Long maxLevel2Groups = licenseDetail.getMaxLevel2Groups();
            if (maxLevel2Groups != null && maxLevel2Groups > 0) {
                Long countAll;
                GroupService groupService = GroupService.getInstance();
                countAll = groupService.countAllActiveTenants();
                level2Groups.put("limit", maxLevel2Groups);
                level2Groups.put("used", countAll);
            }
            Long maxLevel3Groups = licenseDetail.getMaxLevel3Groups();
            if (maxLevel3Groups != null && maxLevel3Groups > 0) {
                Long countAll = countSubTenants(licenseDetail);
                level3Groups.put("limit", maxLevel3Groups);
                level3Groups.put("used", countAll);
            }

        }
        return RestUtils.sendOkResponse(mapResponse);
    }

    public static Long countSubTenants(LicenseDetail licenseDetail) {
        GroupService groupService = GroupService.getInstance();
        Long countAll;
        Group licenseGroup = groupService.get(licenseDetail.getGroupId());
        boolean isRootLicense = groupService.getRootGroup().getId().equals(licenseGroup.getId());
        if (isRootLicense) {
            countAll = groupService.countAllActiveSubTenants();
        } else {
            countAll = groupService.countAllActiveSubTenants(licenseGroup.getParentLevel2());
        }
        return countAll;
    }


    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"group:d:{id}"})
    @ApiOperation(position=5, value="Delete a Group")
    public Response deleteGroup( @PathParam("id") Long id )
    {
        // 1c. TODO: Restrict access based on OBJECT level read permissions
        Group group = GroupService.getInstance().get( id );
        if( group == null )
        {
            return RestUtils.sendBadResponse( String.format( "GroupId[%d] not found", id) );
        }
        // 2. Limit visibility based on user's group and the object's group (group based authorization)
        EntityVisibility entityVisibility = getEntityVisibility();
        GeneralVisibilityUtils.limitVisibilityDelete(entityVisibility, group );
        // handle validation in an Extensible manner
        validateDelete( group );
        List<Long> groupMqtt = GroupService.getInstance().getMqttGroups(group);
        GroupService.getInstance().delete( group );

        // RIOT-13659 enable tickles for groups.
        GroupService.getInstance().sendMQTTSignal(groupMqtt);
        GroupService.getInstance().refreshGroupCache(group, true);

        return RestUtils.sendDeleteResponse();
    }


    @GET
    @Path("/attachments/validate")
    @RequiresAuthentication
    @ApiOperation(position = 21, value = "Validate File Path")
    public Response validatePathFile(@QueryParam ("pathFile") String pathFile) throws Exception
    {
        Map<String, Object> result = new HashMap<>(  );
        try{
            result = GroupService.getInstance().validatePathFile( pathFile );
        }catch(Exception e)
        {
            return RestUtils.sendResponseWithCode(e.getMessage(), 400);
        }
        return RestUtils.sendOkResponse( result );
    }

    @GET
    @Path("/regionalSetting/timeZone")
    @RequiresAuthentication
    @ApiOperation(position = 22, value = "return all list Time zone")
    public Response getRegionalSettings() {
        return RestUtils.sendOkResponse(GroupService.getInstance().getRegionalSettings());
    }

    @GET
    @Path("/connection")
    @Produces(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresAuthentication
    @ApiOperation(position=1, value="Get a List of Connections (AUTO)")
    public Response listConnections( @QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber, @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only, @QueryParam("visibilityGroupId") Long visibilityGroupId, @DefaultValue("") @QueryParam("upVisibility") String upVisibility, @DefaultValue("") @QueryParam("downVisibility") String downVisibility, @DefaultValue("false") @QueryParam("returnFavorite") boolean returnFavorite, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
    {
        validateListPermissions();
        ConnectionController c = new ConnectionController();
        return c.listConnections(pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility, downVisibility, false, extend, project);
    }
}

