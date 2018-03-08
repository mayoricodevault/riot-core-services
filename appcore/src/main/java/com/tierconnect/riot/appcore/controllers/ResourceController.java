package com.tierconnect.riot.appcore.controllers;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.QResource;
import com.tierconnect.riot.appcore.entities.Resource;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.services.LicenseService;
import com.tierconnect.riot.appcore.services.ResourceService;
import com.tierconnect.riot.appcore.utils.*;
import com.tierconnect.riot.sdk.dao.Pagination;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.jaxrs.PATCH;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.subject.Subject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;
import java.util.*;

@Path("/resource")
@Api("/resource")
public class ResourceController extends ResourceControllerBase 
{

	@Override
	public boolean includeInSelect(Resource resource) {
		Subject subject = SecurityUtils.getSubject();
		User currentUser = (User) subject.getPrincipal();
		return super.includeInSelect(resource) && LicenseService.getInstance().isValidResource(currentUser, resource.getName());

	}

	@Override
	public void validateSelect(Resource resource) {
		if (!includeInSelect(resource)) {
			throw new UserException(String.format( "ResourceId[%d] not found", resource.getId()) );
		}
		super.validateSelect(resource);
	}

	@GET
	@Path("/tree")
	@Produces(MediaType.APPLICATION_JSON)
	@RequiresAuthentication
	@ApiOperation(position = 1, value = "Get a List of Resources in Tree")
	public Response listResourcesInTree(
			@QueryParam("pageSize") Integer pageSize,
			@QueryParam("pageNumber") Integer pageNumber,
			@QueryParam("order") String order,
			@QueryParam("where") String where,
			@Deprecated @QueryParam("extra") String extra,
			@Deprecated @QueryParam("only") String only,
			@QueryParam("visibilityGroupId") Long visibilityGroupId,
			@DefaultValue("") @QueryParam("upVisibility") String upVisibility,
			@DefaultValue("") @QueryParam("downVisibility") String downVisibility,
			@QueryParam("topId") String topId) {

		Group visibilityGroup = VisibilityUtils.getVisibilityGroup(Resource.class.getCanonicalName(), visibilityGroupId);
		//Pagination pagination = new Pagination( pageNumber, pageSize );
		Pagination pagination = new Pagination(1, -1);

		BooleanBuilder be = new BooleanBuilder();
		// 2. Limit visibility based on user's group and the object's group (group based authorization)
		EntityVisibility entityVisibility = getEntityVisibility();
		boolean up = StringUtils.isEmpty(upVisibility) ? entityVisibility.isNotFalseUpVisibility() : entityVisibility.isNotFalseUpVisibility() && Boolean.parseBoolean(upVisibility);
		boolean down = StringUtils.isEmpty(downVisibility) ? entityVisibility.isDownVisibility() : entityVisibility.isDownVisibility() && Boolean.parseBoolean(downVisibility);
		be = be.and(GeneralVisibilityUtils.limitVisibilitySelectAll(entityVisibility, QResource.resource, visibilityGroup, upVisibility, downVisibility));
		// 4. Implement filtering
		be = be.and(QueryUtils.buildSearch(QResource.resource, where));

		Long count = 0L;

		Resource topResource = null;
		if (StringUtils.isNotEmpty(topId)) {
			topResource = ResourceService.getInstance().get(Long.parseLong(topId));
		}

		TreeParameters<Resource> treeParameters = new TreeParameters<>();
		treeParameters.setOnly(only);
		treeParameters.setExtra(extra);
		treeParameters.setOrder(order);
		treeParameters.setTopObject(topResource);
		treeParameters.setVisibilityGroup(visibilityGroup);

		if (StringUtils.isEmpty(where)) {
			if (topId != null) {
				be = be.and(QResource.resource.id.eq(topResource.getId()));
			}
			List<Resource> thingList = ResourceService.getInstance().listPaginated(be, pagination, order);
			for (Resource thing : thingList) {
				mapThing(thing, treeParameters, false);
				addAllDescendants(thing, treeParameters, false, up, down);
			}
		} else {
			List<Resource> thingList = ResourceService.getInstance().listPaginated(be, pagination, order);
			for (Resource thing : thingList) {
				mapThing(thing, treeParameters, true);
			}
		}

		count = Long.valueOf(treeParameters.getList().size());
		// 3. Implement pagination
		Map<String,Object> mapResponse = new HashMap<>();
		mapResponse.put("total", count);
		mapResponse.put("results", sortListOfMap(treeParameters));
		return RestUtils.sendOkResponse(mapResponse);

	}

	private List<Map<String, Object>> sortListOfMap(TreeParameters<Resource> treeParameters) {
		List<Map<String, Object>> list = treeParameters.getList();

		final LinkedHashMap<String,String> mapOrderBy = new LinkedHashMap<>(10);
		for (String order : treeParameters.getOrder().split(",")) {
			String[] orderArray = StringUtils.split(order, ":");
			if (orderArray.length == 2) {
				mapOrderBy.put(orderArray[0], orderArray[1]);
			}
		}

		list.sort(new Comparator<Map<String, Object>>() {
			@Override
			public int compare(Map<String, Object> o1, Map<String, Object> o2) {
				int valueCompare = 0;
				for (Map.Entry<String, String> orderBy : mapOrderBy.entrySet()) {
					if (valueCompare == 0) {
						Object value1 = o1.get(orderBy.getKey());
						Object value2 = o2.get(orderBy.getKey());
						if (value1 instanceof Number && value2 instanceof Number) {
							valueCompare = new BigDecimal(value1.toString()).compareTo(new BigDecimal(value2.toString()));
						} else {
							valueCompare = String.valueOf(value1).compareTo(String.valueOf(value2));
						}
						valueCompare = valueCompare * (orderBy.getValue().equalsIgnoreCase("desc") ? -1 : 1);
					}
				}
				return valueCompare;
			}
		});
		return list;
	}

	public void mapThing(Resource object, TreeParameters<Resource> treeParameters, boolean selected) {
		if (!includeInSelect(object)) {
			return;
		}

		Map<Long, Map<String, Object>> objectCache = treeParameters.getObjectCache();
		Map<String, Boolean> permissionCache = treeParameters.getPermissionCache();
		Map<Long, Set<Long>> childrenMapCache = treeParameters.getChildrenMapCache();
		List<Map<String, Object>> list = treeParameters.getList();
		String extra = treeParameters.getExtra();
		String only = treeParameters.getOnly();
		Resource topGroupType = treeParameters.getTopObject();
		Integer level;
		//Map Thing
		Map<String, Object> objectMap = objectCache.get(object.getId());
		if (objectMap == null) {
			objectMap = QueryUtils.mapWithExtraFields(object, extra, getExtraPropertyNames());
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
		Resource parent = object.getParent();
		Map<String, Object> parentObjectMap = null;
		if (parent != null) {
			parentObjectMap = objectCache.get(parent.getId());
			if (parentObjectMap == null) {
				mapThing(parent, treeParameters, false);
				parentObjectMap = objectCache.get(parent.getId());
			}

				level =  (parentObjectMap != null? ((Integer)parentObjectMap.get("treeLevel") + 1):0);

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

	public void addAllDescendants(Resource base, TreeParameters<Resource> treeParameters, boolean selected, boolean up, boolean down) {
		BooleanBuilder be = new BooleanBuilder();
		be = be.and(VisibilityUtils.limitVisibilityPredicate(treeParameters.getVisibilityGroup(), QResource.resource.group, up, down));
		be = be.and(QResource.resource.parent.eq(base));
		List<Resource> children = ResourceService.getInstance().listPaginated(be, null, treeParameters.getOrder());
		for (Resource child : children) {
			if (!child.getId().equals(base.getId())) {
				mapThing(child, treeParameters, selected);
				addAllDescendants(child, treeParameters, selected, up, down);
			}
		}
	}

    @Override
    public List<String> getExtraPropertyNames() {
        List<String> a = new ArrayList(super.getExtraPropertyNames());
        a.add("acceptedAttributeList");
        return a;
    }

	@PATCH
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	// 1a. Limit access based on CLASS level resources
	@RequiresPermissions(value={"resource:u:{id}"})
	@ApiOperation(position=4, value="Update a Resource")
	public Response updateResource(@PathParam("id") Long id, Map<String, Object> map) {
		Resource resource = ResourceService.getInstance().get( id );
		// 2. Limit visibility based on user's group and the object's group (group based authorization)
		EntityVisibility entityVisibility = getEntityVisibility();
		GeneralVisibilityUtils.limitVisibilityUpdate( entityVisibility, resource, VisibilityUtils.getObjectGroup(map));
		// 7. handle insert and update
		if (map.containsKey("description")) {
			resource.setDescription((String) map.get("description"));
		}		// 6. handle validation in an Extensible manner
		if (map.containsKey("label")) {
			resource.setLabel((String) map.get("label"));
		}		// 6. handle validation in an Extensible manner
		validateUpdate( resource );
		Map<String,Object> publicMap = resource.publicMap();
		return RestUtils.sendOkResponse(publicMap);
	}

	@Override
	public void validateUpdate(Resource resource) {
		validateSelect(resource);
		super.validateUpdate(resource);
	}

}

