package com.tierconnect.riot.iot.controllers;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.services.*;
import com.tierconnect.riot.appcore.utils.*;
import com.tierconnect.riot.commons.DateFormatAndTimeZone;
import com.tierconnect.riot.commons.utils.DateHelper;
import com.tierconnect.riot.iot.dao.util.FormulaUtil;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;
import com.tierconnect.riot.sdk.dao.Pagination;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.servlet.exception.ForbiddenException;
import com.tierconnect.riot.sdk.utils.PermissionsUtils;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.jaxrs.PATCH;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.subject.Subject;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Path("/thingType")
@Api("/thingType")
public class ThingTypeController extends ThingTypeControllerBase
{

	private static Logger logger = Logger.getLogger( ThingTypeController.class );

	@Context
	ServletContext context;

	private String[] IGNORE_THING_TYPE_FIELDS = new String[] { "fields", "children", "parents", "selected", "treeLevel", "childrenUdf" };

	private static long delt1;

	private static long delt2;

	//Static variable to keep track of quantity of things being created
	private static int requestsInProgress;
	//Max number of things that can be created at a time in parallel
	//TODO: Make it Configurable/Dynamic
	private static int maxRequestsInProgress=Runtime.getRuntime().availableProcessors() * 200;

    public Response listThingTypesInTree( @QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber,
                                          @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra,
                                          @Deprecated @QueryParam("only") String only, @QueryParam("visibilityGroupId") Long visibilityGroupId,
                                          @DefaultValue("") @QueryParam("upVisibility") String upVisibility,
                                          @DefaultValue("") @QueryParam("downVisibility") String downVisibility,
			                              @DefaultValue("false") @QueryParam("enableMultilevel") String enableMultilevel) {
        return listThingTypesInTree2(pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility,
				downVisibility, "true", enableMultilevel,false);
    }

	@GET
	@Path("/tree")
	@Produces(MediaType.APPLICATION_JSON)
	@RequiresAuthentication
	@ApiOperation(position = 1, value = "Get a List of Thing Types in Tree")
	public Response listThingTypesInTree2(@QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber,
										  @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra,
										  @Deprecated @QueryParam("only") String only, @QueryParam("visibilityGroupId") Long visibilityGroupId,
										  @DefaultValue("") @QueryParam("upVisibility") String upVisibility,
										  @DefaultValue("") @QueryParam("downVisibility") String downVisibility,
										  @DefaultValue("false") @QueryParam("validateThingTypePermission") String validateThingTypePermissionParam,
										  @DefaultValue("true") @QueryParam("enableMultilevel") String enableMultilevel,
										  @DefaultValue("false") @QueryParam("returnFavorite") boolean returnFavorite) {
		Map<String, Object> mapResponse = new HashMap<>();
		List<Map<String, Object>> list;
		Long count;
		try {
			boolean validateThingTypePermission = Boolean.parseBoolean(validateThingTypePermissionParam);
			Group visibilityGroup = VisibilityUtils.getVisibilityGroup(ThingType.class.getCanonicalName(), visibilityGroupId);
			Pagination pagination = new Pagination(
					(enableMultilevel != null && enableMultilevel.trim().equals("true")) ? 1 : pageNumber,
					(enableMultilevel != null && enableMultilevel.trim().equals("true")) ? 1000 : pageSize);

			// 2. Limit visibility based on user's group and the object's group
			// (group based authorization)
			EntityVisibility entityVisibility = getEntityVisibility();
			boolean up = StringUtils.isEmpty(upVisibility) ? entityVisibility.isNotFalseUpVisibility() : entityVisibility.isNotFalseUpVisibility() && Boolean.parseBoolean(upVisibility);
			boolean down = StringUtils.isEmpty(downVisibility) ? entityVisibility.isDownVisibility() : entityVisibility.isDownVisibility() && Boolean.parseBoolean(downVisibility);

			Map<Long, Map<String, Object>> objectCache = new HashMap<>();
			Map<Long, Set<Long>> childrenMapCache = new HashMap<>();
			Map<String, Boolean> permissionCache = new HashMap<>();

			//Get List With Native Children
			Map<String, Object> mapNative = this.getListWithNativeChildren(
					visibilityGroup
					, upVisibility, downVisibility
					, only, extra, order, where, validateThingTypePermission
					, pagination
					, objectCache, childrenMapCache, permissionCache, returnFavorite);

			list = (List<Map<String, Object>>) mapNative.get("list");
			count = (Long) mapNative.get("count");

			//Sort List
			TreeUtils.sortObjects(order, list);

			//Invert Values in the Map
			if (enableMultilevel != null && enableMultilevel.trim().equals("true")) {
				List<Map<String, Object>> listInsertValue = invertValuesData(list, objectCache, childrenMapCache, permissionCache, only,
						extra, order, validateThingTypePermission);
				List<Map<String, Object>> listNoParent = setThingTypeNotParent(listInsertValue, objectCache, childrenMapCache,
						permissionCache, only, extra, order, validateThingTypePermission);

				List<Map<String, Object>> listFilterDuplicates = setThingTypeNotParent(listNoParent);
				//Sort List
				TreeUtils.sortObjects(order, listFilterDuplicates);
				List<Map<String, Object>> result = Pagination.paginationList(listFilterDuplicates, pageNumber, pageSize);
				mapResponse.put("total", listFilterDuplicates.size());
				mapResponse.put("results", result);
			} else {
				mapResponse.put("total", count);
				mapResponse.put("results", list);
			}
		} catch (Exception e) {
			return RestUtils.sendResponseWithCode(e.getMessage(), 400);
		}
		return RestUtils.sendOkResponse(mapResponse);
	}


	/*****************************************************
	 * Add Children  into thing types
	 * ****************************************************/
	public Map<String, Object> getListWithNativeChildren(
			Group visibilityGroup
			, String upVisibility
			, String downVisibility
			, String only
			, String extra
			, String order
			, String where, boolean validateThingTypePermission
			, Pagination pagination
			, Map<Long, Map<String, Object>> objectCache
			, Map<Long, Set<Long>> childrenMapCache
			, Map<String, Boolean> permissionCache
			, boolean returnFavorite) {
		Map<String, Object> result = new HashMap<>();
		List<Map<String, Object>> list = new LinkedList<>();
		Long count;
		User currentUser = (User) SecurityUtils.getSubject().getPrincipal();

		BooleanBuilder be = new BooleanBuilder();
		be = be.and(GeneralVisibilityUtils.limitVisibilitySelectAll(entityVisibility, QThingType.thingType, visibilityGroup, upVisibility, downVisibility));
		be = be.and(QueryUtils.buildSearch(QThingType.thingType, where));
		Set<Long> ttIds = currentUser.getThingTypeResources();
		if (ttIds.size() > 0) {
			be = be.and(QThingType.thingType.id.in(ttIds));
		}

		//Get base list of thing Types
		if (StringUtils.isEmpty(where)) {
			BooleanBuilder or = new BooleanBuilder();
			or = or.or(QThingType.thingType.parentTypeMaps.isEmpty());
			or = or.or(QThingType.thingType.isParent.isTrue());
			be = be.and(or);
			count = ThingTypeService.getInstance().countList(be);
			List<ThingType> thingTypeList = ThingTypeService.getInstance().listPaginated(be, pagination, order);
			for (ThingType thingType : thingTypeList) {
				mapThing(thingType, objectCache, childrenMapCache, permissionCache, list, false, only, extra, order, validateThingTypePermission);
				addAllDescendants(thingType, objectCache, childrenMapCache, permissionCache, list, false, only, extra, order, validateThingTypePermission);
			}
		} else {
			count = ThingTypeService.getInstance().countList(be);
			List<ThingType> thingTypeList = ThingTypeService.getInstance().listPaginated(be, pagination, order);
			for (ThingType thingType : thingTypeList) {
				Map<Long, Map<String, Object>> objectCacheTmp = new HashMap<>();
				Map<Long, Set<Long>> childrenMapCacheTmp = new HashMap<>();
				mapThing(thingType, objectCacheTmp, childrenMapCacheTmp, permissionCache, list, true, only, extra, order, validateThingTypePermission);
				//mapThing( thingType, objectCache, childrenMapCache, permissionCache, list, true, only, extra, order, validateThingTypePermission );
				objectCache.putAll(objectCacheTmp);
				childrenMapCache.putAll(childrenMapCacheTmp);
			}
		}

		if (returnFavorite) {
			User user = (User) SecurityUtils.getSubject().getPrincipal();
			list = FavoriteService.getInstance().addFavoritesToList(list, user.getId(), "thingtype");
		}

		result.put("list", list);
		result.put("count", count);
		return result;
	}

	/*******************************
	 * Map Thing
	 * @param object
	 * @param objectCache
	 * @param childrenMapCache
	 * @param permissionCache
	 * @param list
	 * @param selected
	 * @param only
	 * @param extra
	 * @param order
	 * @param validateThingTypePermission
	 *******************************/
	public void mapThing(ThingType object,
						 Map<Long, Map<String, Object>> objectCache,
						 Map<Long, Set<Long>> childrenMapCache,
						 Map<String, Boolean> permissionCache,
						 List<Map<String, Object>> list,
						 boolean selected,
						 String only,
						 String extra,
						 String order,
						 boolean validateThingTypePermission){
		this.mapThing(object, objectCache, childrenMapCache, permissionCache, list, selected, only, extra, order,
				validateThingTypePermission, null);
	}


	/*******************************
	 * Map Thing
	 * @param object
	 * @param objectCache
	 * @param childrenMapCache
	 * @param permissionCache
	 * @param list
	 * @param selected
	 * @param only
	 * @param extra
	 * @param order
	 * @param validateThingTypePermission
	 *******************************/
	public void mapThing(ThingType object,
						 Map<Long, Map<String, Object>> objectCache,
						 Map<Long, Set<Long>> childrenMapCache,
						 Map<String, Boolean> permissionCache,
						 List<Map<String, Object>> list,
						 boolean selected,
						 String only,
						 String extra,
						 String order,
						 boolean validateThingTypePermission, String parentCode)
	{
		Subject subject = SecurityUtils.getSubject();
        if (validateThingTypePermission) {
            if (!PermissionsUtils.isPermittedAny(subject, Resource.THING_TYPE_PREFIX + object.getId() + ":r")) {
                return;
            }
        }
		Integer level;
		// Map Thing
		Map<String, Object> objectMap = objectCache.get( object.getId() );
        List<ThingType> parents = object.getParents();
        if(parentCode != null) {
			parents = parents.stream().
					filter(p -> {
						if(!p.getCode().equals(parentCode)){ //in order to ignore it in the whole request
							Map<String, Object> parentObjectMap = QueryUtils.mapWithExtraFields(p, extra, getExtraPropertyNames());
							parentObjectMap.put( "children", new ArrayList<>() );
							parentObjectMap.put( "typeMultilevel", "PARENT" );
							objectCache.put( p.getId(), parentObjectMap);
							return false;
						}
						return true;
					})
					.collect(Collectors.toList());
		}

        boolean objectMapWasNull = false;
        boolean isRootCall = objectCache.isEmpty() && childrenMapCache.isEmpty();
        if( objectMap == null )
		{
			objectMap = QueryUtils.mapWithExtraFields( object, extra, getExtraPropertyNames() );
			addToPublicMap( object, objectMap, extra );
			QueryUtils.filterOnly( objectMap, only, extra );
			QueryUtils.filterProjectionNested( objectMap, null, null);
			if(parents.isEmpty() )
			{
				list.add( objectMap );
			}
			objectMap.put( "children", new ArrayList<>() );
			objectMap.put( "typeMultilevel", "PARENT" );
			objectCache.put( object.getId(), objectMap );
		}
        else {
            objectMapWasNull = true;
        }
		if( selected )
		{
			objectMap.put( "selected", Boolean.TRUE );
		}
		// Map parent
		List<Map<String, Object>> parentObjectMaps = new ArrayList<>();
		if( parents.size() > 0 )
		{
			// hardcoded for now
			level = 2;
            boolean allParentObjectMapAreNull = true;
			for( ThingType parent : parents )
			{
				Map<String, Object> parentObjectMap = objectCache.get( parent.getId() );
				if( parentObjectMap == null )
				{
					mapThing( parent, objectCache, childrenMapCache, permissionCache, list, false, only, extra, order, validateThingTypePermission );
					parentObjectMap = objectCache.get( parent.getId() );
				}
				// It could be null as is not allowed to see by visibility
				if( parentObjectMap != null )
				{
                    allParentObjectMapAreNull = false;
					parentObjectMaps.add( parentObjectMap );
				}
				// level = (Integer) parentObjectMap.get("treeLevel") + 1;
			}
            if (objectMapWasNull && allParentObjectMapAreNull) {
                level = 1;
                list.add( objectMap );
            }
		}
		else
		{
			level = 1;
		}

		objectMap.put( "treeLevel", level );

//		if(thingTypeUdf!=null)
//		{
		//objectMap.put( "thingTypeUdf", getThingTypeUdf(object.getThingTypeFields()));
//			objectMap.put( "thingTypeUdf", thingTypeUdf);
//		}
		// Add child to parent
		for( Map<String, Object> parentObjectMap : parentObjectMaps )
		{
			Set<Long> childrenSet = childrenMapCache.get( parentObjectMap.get( "id" ) );
			if( childrenSet == null )
			{
				childrenSet = new HashSet<Long>();
				childrenMapCache.put( (Long) parentObjectMap.get( "id" ), childrenSet );
			}
			List childrenList = (List) parentObjectMap.get( "children" );
			if( !childrenSet.contains( object.getId() ) )
			{
				childrenSet.add( object.getId() );
				childrenList.add(objectMap);
				objectMap.put("typeMultilevel","CHILD");
				TreeUtils.sortObjects(order, childrenList);
			}
		}
		if(isRootCall) {// check if it has children
			List<ThingType> children = ThingTypeService.getInstance().getReferences(object);
			children.addAll(object.getChildren());
			for(ThingType child : children){
				mapThing( child, objectCache, childrenMapCache, permissionCache, list, false,
						only, extra, order, validateThingTypePermission, object.getCode() );
			}
		}
		if(objectMap!=null && objectMap.size()>0 && !list.contains( objectMap ))
		{
			list.add( objectMap );
		}
	}

 	public void addAllDescendants( ThingType base, Map<Long, Map<String, Object>> objectCache, Map<Long, Set<Long>> childrenMapCache,
			Map<String, Boolean> permissionCache, List<Map<String, Object>> list, boolean selected, String only, String extra, String order, boolean validateThingTypePermission )
	{
		// TODO copy from GroupType, limit visibility
		QThingTypeMap qThingTypeMap = QThingTypeMap.thingTypeMap;
		List<ThingType> children = ThingTypeMapService.getThingTypeMapDAO().getQuery().where( qThingTypeMap.parent.eq( base ) )
				.list( qThingTypeMap.child );
		for( ThingType child : children )
		{
			if( !child.getId().equals( base.getId() ) )
			{
				mapThing( child, objectCache, childrenMapCache, permissionCache, list, selected, only, extra, order, validateThingTypePermission );
				addAllDescendants( child, objectCache, childrenMapCache, permissionCache, list, selected, only, extra, order, validateThingTypePermission);
			}
		}
	}

	@PUT
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@RequiresPermissions(value = { "thingType:i" })
	@ApiOperation("Add Thing Type")
	public Response insertThingType( Map<String, Object> thingTypeMap, @QueryParam("createRecent") @DefaultValue("false") Boolean createRecent )
	{

        List<Map<String,Object>> fields = (List<Map<String, Object>>) thingTypeMap.get("fields");
        List<String> reservedWords = ThingTypeService.getInstance().getReservedWords();
        List<String> errorFields = new ArrayList<>();
        for(Map<String, Object> field : fields ){
            if(reservedWords.contains(field.get("name").toString())){
                errorFields.add(field.get("name").toString());
            }
        }
        if(errorFields.size() > 0){
            return RestUtils.sendBadResponse("Field names are not allowed because are reserved words " + errorFields + ".");
        }
        // 1.1.- Validating expression fields
        ValidationBean validationBean1 = ValidatorService.validateExpressionFields(thingTypeMap);
        if (validationBean1.isError()) {
            return RestUtils.sendBadResponse(validationBean1.getErrorDescription());
        }
        //1. Validation
		ValidationBean validationBean = validateInsertThingType( thingTypeMap );
		if(validationBean.isError())
		{
			return RestUtils.sendBadResponse( validationBean.getErrorDescription() );
		}
		// Insert thingType
		Group group = GroupService.getInstance().get( ((Number) thingTypeMap.get( "group.id" )).longValue() );
		ThingTypeTemplate thingTypeTemplate= ThingTypeTemplateService.getInstance().get( ((Number) thingTypeMap.get( "thingTypeTemplateId" )).longValue() );
		String name = (String) thingTypeMap.get( "name" );
		ThingType thingType = new ThingType( name );
		thingType.setThingTypeTemplate(thingTypeTemplate);
		thingType.setGroup( group );
		thingType.setModifiedTime(new Date().getTime());

		Group visibilityGroup = VisibilityUtils.getVisibilityGroup( ThingType.class.getCanonicalName(), null );
		if( GroupService.getInstance().isGroupNotInsideTree( thingType.getGroup(), visibilityGroup ) )
		{
			throw new ForbiddenException( "Forbidden ThingType" );
		}

		if( thingTypeMap.containsKey( "autoCreate" ) )
		{
			thingType.setAutoCreate( (Boolean) thingTypeMap.get( "autoCreate" ) );
		}
		if( thingTypeMap.containsKey( "thingTypeCode" ) )
		{
			thingType.setThingTypeCode( (String) thingTypeMap.get( "thingTypeCode" ) );
		}
		if( thingTypeMap.containsKey( "isParent" ) )
		{
			thingType.setIsParent( (boolean) thingTypeMap.get( "isParent" ) );
		}
		if( thingTypeMap.containsKey( "defaultOwnerGroupType.id" ) )
		{
			GroupType groupType = GroupTypeService.getInstance().get( ((Number) thingTypeMap.get( "defaultOwnerGroupType.id" )).longValue() );
			thingType.setDefaultOwnerGroupType( groupType );
		}
		if( thingTypeMap.containsKey("serialFormula") && thingTypeMap.get("serialFormula") instanceof String &&
				!((String) thingTypeMap.get("serialFormula")).isEmpty() )
		{
			thingType.setSerialFormula((String) thingTypeMap.get("serialFormula"));
			// validate formula field
			if( thingTypeMap.containsKey( "fields" ) ) {
				List<Map<String, Object>> thingsTypeMap = (List<Map<String, Object>>) thingTypeMap.get("fields");
				if (thingsTypeMap != null && !thingsTypeMap.isEmpty()) {
					Map<String, Object> ttf = new HashMap<String, Object>();
					for (Map<String, Object> thingTypeFieldMap : thingsTypeMap) {
						Map<String, Object> value = new HashMap<String, Object>();
						value.put("value", thingTypeFieldMap.get("defaultValue"));
						ttf.put((String) thingTypeFieldMap.get("name"), value);
					}
					FormulaUtil.validateFormula(thingType.getSerialFormula(), ttf, true);
				} else {
					throw new UserException("It is necessary to configure almost a Thing Type Property for evaluating serial's expression");
				}
			}
		}
		//--Validation of Licence and tree ThingType
		validateInsert( thingType );

		thingType = ThingTypeService.getInstance().insert( thingType );
		if (thingType == null) {
			throw new UserException("Error creating thing type");
		}

		// Insert Parents and Children in ThingTypeMap
		List<ThingType> parents = new ArrayList<>();
		if( thingTypeMap.containsKey( "parent.ids" ) )
		{
			for( Number n : (List<Number>) thingTypeMap.get( "parent.ids" ) )
			{
				parents.add( ThingTypeService.getInstance().get( n.longValue() ) );
			}
		}
		List<ThingType> children = new ArrayList<>();
		if( thingTypeMap.containsKey( "child.ids" ) )
		{
			for( Number n : (List<Number>) thingTypeMap.get( "child.ids" ) )
			{
				children.add( ThingTypeService.getInstance().get( n.longValue() ) );
			}
		}

		if( !parents.isEmpty() )
		{
			Set<ThingTypeMap> lstThingTypemap = new HashSet<>();
			for( ThingType parent : parents )
			{
				if(parent != null)
				{
					ThingTypeMap ftmap = new ThingTypeMap();
					ftmap.setParent( parent );
					ftmap.setChild( thingType );
					ThingTypeMap newRelation = ThingTypeMapService.getInstance().insert( ftmap );
					lstThingTypemap.add(newRelation);
					//Set Parent
					Set<ThingTypeMap> oldChildrenOfParent = parent.getChildrenTypeMaps();
					if (oldChildrenOfParent == null) {
						oldChildrenOfParent = new HashSet<>();
					}
					oldChildrenOfParent.add(newRelation);
					parent.setChildrenTypeMaps(oldChildrenOfParent);
				}
			}
			if (!lstThingTypemap.isEmpty()) {
				thingType.setParentTypeMaps(lstThingTypemap);
			}
		}
		if( !children.isEmpty() )
		{
			Set<ThingTypeMap> lstThingTypemapChildren = new HashSet<>();
			for( ThingType child : children )
			{
				if(child != null)
				{
					ThingTypeMap ftmap = new ThingTypeMap();
					ftmap.setParent( thingType );
					ftmap.setChild( child );
					ThingTypeMap newRelation = ThingTypeMapService.getInstance().insert(ftmap);
					lstThingTypemapChildren.add(newRelation);
					// Set Children
					Set<ThingTypeMap> oldParentofChildren = child.getParentTypeMaps();
					oldParentofChildren.add(newRelation);
					child.setChildrenTypeMaps(oldParentofChildren);
				}
			}
			if (!lstThingTypemapChildren.isEmpty()) {
				thingType.setChildrenTypeMaps(lstThingTypemapChildren);
			}
		}

		//Add the new ThingType as ThingType Udf in the following Types
		if( thingTypeMap.containsKey( "childrenUdf" ) )
		{
			List<Map<String, Object>> childrenUdf = (List<Map<String, Object>>) thingTypeMap.get( "childrenUdf" );
			if(childrenUdf!=null && childrenUdf.size()>0)
			{
				for( Object objChildrenUdf : childrenUdf )
				{
					Map<String, Object> childrenMap = (Map<String, Object>) objChildrenUdf;
					ThingType child = ThingTypeService.getInstance().get( Long.parseLong( childrenMap.get( "id" ).toString() ) );
					if(childrenMap.get( "operation" ).toString().equals( "add" ))
					{
						ThingTypeField field = new ThingTypeField();
						field.setName(childrenMap.get( "property" ).toString());
						field.setDataType( DataTypeService.getInstance().get( ThingTypeField.Type.TYPE_THING_TYPE.value ) );
						field.setDataTypeThingTypeId( thingType.getId() );
						field.setDefaultValue( null );
						field.setMultiple( false );
						field.setSymbol( null );
						field.setThingType( child );
						field.setThingTypeFieldTemplateId( null );
						field.setTimeSeries( false );
						field.setTimeToLive( null );
						field.setTypeParent( ThingTypeField.TypeParent.TYPE_PARENT_NATIVE_THING_TYPE.value);
						field.setUnit( null );
						ThingTypeFieldService.getInstance().insert(field);
					}
				}
			}
		}

		// Insert Thing type Fields
		List<Map<String, Object>> thingTypeFields = (List<Map<String, Object>>) thingTypeMap.get( "fields" );
		List<ThingTypeField> thingTypeFieldUDFList = new LinkedList<>();
		if( thingTypeFields != null )
		{
			for( Map<String, Object> thingTypeFieldMap : thingTypeFields )
			{
				ThingTypeField thingTypeField = new ThingTypeField();
				thingTypeField.setName( (String) thingTypeFieldMap.get( "name" ) );
				thingTypeField.setSymbol( (String) thingTypeFieldMap.get( "symbol" ) );
				thingTypeField.setUnit( (String) thingTypeFieldMap.get( "unit" ) );
				thingTypeField.setDataType(
						DataTypeService.getInstance().get( Long.parseLong( thingTypeFieldMap.get( "type" ).toString() ) ) );
				if(thingTypeFieldMap.get("dataTypeThingTypeId" )!=null)
				{
					thingTypeField.setDataTypeThingTypeId( Long.parseLong( thingTypeFieldMap.get("dataTypeThingTypeId" ).toString() ) );
				}
				thingTypeField.setTimeSeries((Boolean) thingTypeFieldMap.get("timeSeries"));
				thingTypeField.setMultiple((Boolean) thingTypeFieldMap.get( "multiple" ));
				try {
					ThingTypeService.getInstance().validateThingTypeUDF(thingTypeField);
				} catch (UserException e){
					logger.error(e.getMessage(), e);
					return RestUtils.sendBadResponse(e.getMessage());
				}
				thingTypeField.setThingType( thingType );
				Number dataTypeId = (Number) thingTypeFieldMap.get( "type" );
				thingTypeField.setTypeParent((String) thingTypeFieldMap.get("typeParent"));
				thingTypeField.setDataType( DataTypeService.getInstance().get( Long.parseLong( dataTypeId.toString() ) ) );
				String data = null;

                if(thingTypeFieldMap.get("defaultValue") != null){
                    if(!(thingTypeFieldMap.get("defaultValue") instanceof ArrayList)){
                        data = thingTypeFieldMap.get("defaultValue").toString();
                    }else{
                        ArrayList<String> aa = (ArrayList) thingTypeFieldMap.get("defaultValue");
                        data = StringUtils.join(aa, ",");
                    }
                }
				thingTypeField.setDefaultValue(data);
				Integer fieldTemplateId =  (Integer) thingTypeFieldMap.get("thingTypeFieldTemplateId");
				if(fieldTemplateId!=null) {
					ThingTypeFieldTemplate thingTypeFieldTemplate = ThingTypeFieldTemplateService.getInstance().get(Long.parseLong(fieldTemplateId.toString()));
					thingTypeField.setThingTypeFieldTemplateId(thingTypeFieldTemplate.getId());
				}
				ThingTypeFieldService.getInstance().insert( thingTypeField );
				if(thingTypeField.isThingTypeUDF()){
					thingTypeFieldUDFList.add(thingTypeField);
				}
			}
		}

		addThingTypeFields(thingType);

		ThingTypeService.getInstance().update(thingType);

		ThingTypeService.getInstance().associate(thingType, thingTypeFieldUDFList);
		if (createRecent){
			RecentService.getInstance().insertRecent(thingType.getId(), thingType.getName(),"thingtype", thingType.getGroup());
		}

		commitTransaction();
		openTransaction();
        thingType = ThingTypeService.getInstance().get(thingType.getId());//fresh version of commited tt
		ThingTypeService.getInstance().putOneInCache(thingType);
        DataTypeService.getInstance().replaceThingTypeFields(thingType.getThingTypeFields());
		Group groupFromCache = GroupService.getInstance().getFromCache(thingType.getGroup().getHierarchyName());
		// RIOT-13659 send tickle for thing type.
		BrokerClientHelper.sendRefreshThingTypeMessage(false, GroupService.getInstance().getMqttGroups(groupFromCache));
		ThingTypeService.getInstance().refreshCache(thingType, false);
		return RestUtils.sendCreatedResponse( thingType.publicMap( true, false ) );
	}

	/**
	 * This method adds thingTypeFiled to thingType.
	 * It is being used only by insertThingType method.
	 * todo refactor this part
	 * @param thingType but without thingTypeFields
     */
	private void addThingTypeFields(ThingType thingType){
		Set<ThingTypeField> fields=new HashSet<>();
		List<ThingTypeField>thingTypeFields=ThingTypeFieldService.getInstance().getThingTypeField(thingType.getId());
		for (ThingTypeField thingTypeField:thingTypeFields){
			fields.add(thingTypeField);
		}
		thingType.setThingTypeFields(fields);
	}


	/*Validation for insert Thing type*/
	public ValidationBean validateInsertThingType (Map<String, Object> thingTypeMap)
	{
		ValidationBean response = new ValidationBean();
		ArrayList<String> messages = new ArrayList<String>();
		String name = (thingTypeMap.get("name") == null)?null:thingTypeMap.get("name").toString().trim();
		//Validation obligatory fields
		if( name == null || StringUtils.isEmpty( name ) || name.contains( "(" ) || name.contains( ")" ) || name.contains( "|" )
				|| name.contains( "&" ) || name.contains( "$" ) )
		{
			messages.add("Invalid Name");
		}
		Number groupId = (Number) thingTypeMap.get( "group.id" );
		if(groupId==null)
		{
			messages.add("The group.id is required." );
		}else {
			Group group = GroupService.getInstance().get(groupId.longValue());
			if (group == null) {
				messages.add("Invalid Group");
			}
		}
		Number thingTypeTemplateId = (Number) thingTypeMap.get( "thingTypeTemplateId" );
		if(thingTypeTemplateId==null)
		{
			messages.add("The thingTypeTemplateId is required.");
		} else {
			ThingTypeTemplate thingTypeTemplate = ThingTypeTemplateService.getInstance().get(thingTypeTemplateId.longValue());
			if (thingTypeTemplate == null) {
				messages.add("Invalid thingTypeTemplateId");
			}
		}

		//Valida Multilevel
		ValidationBean validaMultilevel = ThingTypeService.getInstance().validateMultilevelRelationship(thingTypeMap);
		if(validaMultilevel.isError())
		{
			messages.add(validaMultilevel.getErrorDescription());
		}

		List<Map<String, Object>> thingTypeFields = (List<Map<String, Object>>) thingTypeMap.get( "fields" );
		if( thingTypeFields != null )
		{

			Map<String, Object> ttf = new HashMap<String, Object>();
			for( Map<String, Object> thingTypeFieldMap : thingTypeFields ) {
				ttf.put((String) thingTypeFieldMap.get("name"), getValueFrom(thingTypeFieldMap));
			}

			String thingTypeRequired = "";
			String invalidDataType = "";
			String messageValidateAttachment = "";
			//int nativeThingTypeCount = 0;
			for( Map<String, Object> thingTypeFieldMap : thingTypeFields )
			{
				//Check property name does not have dot (.)
				if( thingTypeFieldMap.get( "name" )!=null && thingTypeFieldMap.get( "name" ).toString().contains( "." ))
				{
					messages.add("Property Name : '"+ thingTypeFieldMap.get( "name" ) + "' should not have the character dot (.)");
				}
				Number dataTypeId = 0;
				if (thingTypeFieldMap.containsKey("type")) {
					dataTypeId = (Number) thingTypeFieldMap.get("type");
				}
				DataType dataType = DataTypeService.getInstance().get(dataTypeId.longValue());
				if(dataTypeId.intValue() == 0) {
					thingTypeRequired = thingTypeRequired+" "+thingTypeFieldMap.get("name")+ ",";
					break;
				} else {
					if (dataType == null) {
						invalidDataType = thingTypeRequired+" "+thingTypeFieldMap.get("name")+ ",";

					}
				}

				// validate formula
				if (dataTypeId.longValue() == ThingTypeField.Type.TYPE_FORMULA.value) {
					FormulaUtil.validateFormula((String) thingTypeFieldMap.get("defaultValue"), ttf, true);
				}

				//Validate the correct configuration into attachment UDF
				ValidationBean validaAttachment = ThingTypeService.getInstance().validateAttachmentConfig(
						thingTypeFieldMap.get("type") != null ? thingTypeFieldMap.get("type").toString() : "",
						thingTypeFieldMap.get("name") != null ? thingTypeFieldMap.get("name").toString() : "",
						thingTypeFieldMap.get( "defaultValue" ) != null ? thingTypeFieldMap.get( "defaultValue" ).toString():"");
				if(validaAttachment.isError())
				{
					messageValidateAttachment = messageValidateAttachment + validaAttachment.getErrorDescription();
				}

				if (thingTypeFieldMap.get("defaultValue") != null
						&& !StringUtils.isEmpty(thingTypeFieldMap.get("defaultValue").toString())
						&& dataType != null
						&& ThingTypeFieldService.getInstance().isValidDataTypeToCheck(dataType.getId())) {
					ValidationBean validationBean = ThingTypeFieldService.getInstance()
							.validateFieldValue(thingTypeFieldMap.get("name").toString(),
									thingTypeFieldMap.get("defaultValue"), dataType.getId());
					if (validationBean.isError()) {
						messages.add(validationBean.getErrorDescription());
					}
				}

				// Validate that thing type can have only one Thing Type property
//				if ( ((Long)dataTypeId.longValue()).compareTo(ThingTypeField.Type.TYPE_THING_TYPE.value) == 0) {
//					nativeThingTypeCount++;
//				}
			}
			if(thingTypeRequired.length()>0)
			{
				messages.add("The type is required in fields: "+ thingTypeRequired.substring( 0, thingTypeRequired.length()-1 ));
			}
			if(invalidDataType.length()>0)
			{
				messages.add("Invalid type in fields: "+ invalidDataType.substring( 0, invalidDataType.length()-1 ));
			}
			if(messageValidateAttachment!=null && messageValidateAttachment.trim().length()>0)
			{
				messages.add(messageValidateAttachment);
			}
//			if (nativeThingTypeCount > 1){
//				messages.add("This Thing Type can have only one Thing Type property.");
//			}
		}

		if (LicenseService.enableLicense) {
			User user = (User) SecurityUtils.getSubject().getPrincipal();
			LicenseDetail licenseDetail = LicenseService.getInstance().getLicenseDetail(user.getActiveGroup(), true);
			Long maxNumberOfThingTypes = licenseDetail.getMaxThingTypes();
			if (maxNumberOfThingTypes != null && maxNumberOfThingTypes > 0) {
				Long countAll = count(licenseDetail);
				if (countAll >= maxNumberOfThingTypes) {
					messages.add("You have reached the: "+ maxNumberOfThingTypes+" ThingTypes license limit");
				}
			}
		}
		response = this.setMessageValidation(messages);
		return response;
	}

    private Map<String, Object> replaceNullsFromMap(Map<String, Object> objectMap, Object replaceObject){
        Map<String, Object> result = new HashMap<>();
        for(Map.Entry<String,Object> item : objectMap.entrySet()){
            result.put(item.getKey(), item.getValue() != null ? item.getValue() : replaceObject );
        }
        return  result;
    }

	/*Validation for update Thing type*/
	public ValidationBean validateUpdateThingType (Long thingTypeId, Map<String, Object> thingTypeMap)
	{
		ValidationBean response = new ValidationBean();
		ArrayList<String> messages = new ArrayList<String>();
		String name = (thingTypeMap.get("name") == null)?null:thingTypeMap.get("name").toString().trim();
		try{
			//Validation obligatory fields
			if( name == null || StringUtils.isEmpty( name ) || name.contains( "(" ) || name.contains( ")" ) || name.contains( "|" )
					|| name.contains( "&" ) || name.contains( "$" ) )
			{
				messages.add("Invalid Name");
			}

			//Valida Multilevel
			ValidationBean validaMultilevel = ThingTypeService.getInstance().validateMultilevelRelationship(thingTypeMap);
			if(validaMultilevel.isError())
			{
				messages.add(validaMultilevel.getErrorDescription());
			}
			//Thing Type As Property
			validaMultilevel = ThingTypeService.getInstance().validateThingTypeProperty( thingTypeId, thingTypeMap );
			if(validaMultilevel.isError())
			{
				messages.add(validaMultilevel.getErrorDescription());
			}
			//Thing Type valida Thing Type equals Parent Thing Type
			validaMultilevel = ThingTypeService.getInstance().validateParentEqualsThingType( thingTypeId, thingTypeMap );
			if(validaMultilevel.isError())
			{
				messages.add(validaMultilevel.getErrorDescription());
			}

			List<Map<String, Object>> thingTypeFields = (List<Map<String, Object>>) thingTypeMap.get( "fields" );
			/*if (!thingTypeMap.get("serialFormula").toString().isEmpty() && thingTypeFields.isEmpty()) {
				Map<String, Object> serialMap = new HashMap<>();
				serialMap.put("name", "serialNumber");
				serialMap.put("defaultValue", thingTypeMap.get("serialFormula"));
				serialMap.put("type", ThingTypeField.Type.TYPE_TEXT.value);
				thingTypeFields.add(serialMap);
			}*/
			if( thingTypeFields != null )
			{

				Map<String, Object> ttf = new HashMap<String, Object>();
				for( Map<String, Object> thingTypeFieldMap : thingTypeFields ) {
					Map<String,Object> value = new HashMap<String,Object>();
//					value.put("value",thingTypeFieldMap.get("defaultValue"));
					value.put("value", getValueFrom(thingTypeFieldMap));
					ttf.put((String) thingTypeFieldMap.get("name"), value);
				}

				String thingTypeRequired = "";
				String invalidDataType = "";
				String messageValidateAttachment = "";
				String messageValidaSameTTUdf = "";
				//int nativeThingTypeCount = 0;
				ThingType thingToUpdate = ThingTypeService.getInstance().getByCode(thingTypeMap.get( "thingTypeCode" ).toString());

                // Getting deleted UDFs
				List<String> udfListToDelete = new ArrayList<>();
                for (ThingTypeField thingTypeField:thingToUpdate.getThingTypeFields()) {
                    String udfName = thingTypeField.getName();
                    Boolean udfWasFound = false;
                    for (Map<String, Object> thingTypeFieldMap:thingTypeFields) {
                        if (thingTypeFieldMap.get("name").toString().equals(udfName)) {
                            udfWasFound = true;
                            break;
                        }
                    }
                    if (!udfWasFound) {
                        udfListToDelete.add(udfName);
                    }
                }
				for( Map<String, Object> thingTypeFieldMap : thingTypeFields )
				{
					//Check property name does not have dot (.)
					if( thingTypeFieldMap.get( "name" )!=null && thingTypeFieldMap.get( "name" ).toString().contains( "." ))
					{
						messages.add("Property Name : '"+ thingTypeFieldMap.get( "name" ) + "' should not have the character dot (.)");
					}
					Number dataTypeId = 0;
					if (thingTypeFieldMap.containsKey("type")) {
						dataTypeId = (Number) thingTypeFieldMap.get("type");
					}
					DataType dataType = DataTypeService.getInstance().get(dataTypeId.longValue());
					if(dataTypeId.intValue() == 0)
					{
						thingTypeRequired = thingTypeRequired+" "+thingTypeFieldMap.get("name")+ ",";
						break;
					} else {

						if (dataType == null) {
							invalidDataType = thingTypeRequired+" "+thingTypeFieldMap.get("name")+ ",";
						}
					}

					// validate formula
					if (dataTypeId.longValue() == ThingTypeField.Type.TYPE_FORMULA.value) {
                        //Validating if a deleted UDF is using in a Expression type UDF
                        for (String udf:udfListToDelete) {
                            if (thingTypeFieldMap.get("defaultValue").toString().contains(udf)) {
                                messages.add("'" + udf + "' Property cannot be deleted from '" + thingToUpdate.getName() +
                                        "' Thing Type because it is being referenced in '" + thingTypeFieldMap.get("name") +"' Property");
                                response = this.setMessageValidation(messages);
                                return response;
                            }
                        }

						FormulaUtil.validateFormula((String) thingTypeFieldMap.get("defaultValue"), ttf, true);
					}

					//Validate the correct configuration into attachment UDF
					ValidationBean validaAttachment = ThingTypeService.getInstance().validateAttachmentConfig(
							thingTypeFieldMap.get("type") != null ? thingTypeFieldMap.get("type").toString() : "",
							thingTypeFieldMap.get("name") != null ? thingTypeFieldMap.get("name").toString() : "",
							thingTypeFieldMap.get("defaultValue") != null ? thingTypeFieldMap.get("defaultValue").toString() : "");
					if(validaAttachment.isError())
					{
						messageValidateAttachment = messageValidateAttachment + validaAttachment.getErrorDescription();
					}

					if (thingTypeFieldMap.get("defaultValue") != null
							&& !StringUtils.isEmpty(thingTypeFieldMap.get("defaultValue").toString())
							&& dataType != null
							&& ThingTypeFieldService.getInstance().isValidDataTypeToCheck(dataType.getId())) {
						ValidationBean validationBean = ThingTypeFieldService.getInstance()
								.validateFieldValue(thingTypeFieldMap.get("name").toString(),
										thingTypeFieldMap.get("defaultValue"), dataType.getId());
						if (validationBean.isError()) {
							messages.add(validationBean.getErrorDescription());
						}
					}

					// Validate that thing type can have only one Thing Type property
//				if ( ((Long)dataTypeId.longValue()).compareTo(ThingTypeField.Type.TYPE_THING_TYPE.value) == 0 ) {
//					nativeThingTypeCount++;
//				}

					// Validate that thing type does not have the same thingTypeUDF
					if ( (dataType.getId().compareTo(ThingTypeField.Type.TYPE_THING_TYPE.value) == 0  &&
							thingTypeFieldMap.get( "dataTypeThingTypeId" )!=null ))
					{

						Long data = Long.parseLong(thingTypeFieldMap.get( "dataTypeThingTypeId" ).toString());
						if(thingToUpdate != null && data.compareTo(thingToUpdate.getId())==0)
						{
							messageValidaSameTTUdf = messageValidaSameTTUdf+thingTypeFieldMap.get("name").toString()+",";
						}
					}
				}
				if(thingTypeRequired.length()>0)
				{
					messages.add("The type is required in fields: "+ thingTypeRequired.substring( 0, thingTypeRequired.length()-1 ));
				}
				if(invalidDataType.length()>0)
				{
					messages.add("Invalid type in fields: "+ invalidDataType.substring( 0, invalidDataType.length()-1 ));
				}
				if(messageValidateAttachment!=null && messageValidateAttachment.length()>0)
				{
					messages.add(messageValidateAttachment);
				}
				if(thingToUpdate != null && messageValidaSameTTUdf!=null && messageValidaSameTTUdf.length()>0)
				{
					String udf = messageValidaSameTTUdf.substring(0, messageValidaSameTTUdf.length()-1);
					messages.add("You cannot assign the same Thing Type '"+thingToUpdate.getName()+"' as Thing Type UDF in: "+udf);
				}

//			if (nativeThingTypeCount > 1){
//				messages.add("This Thing Type can have only one Thing Type property.");
//			}
			}

			response = this.setMessageValidation(messages);
		}catch(Exception e){
			response.setErrorDescription("Error validating the update: "+e.getMessage());
		}


		return response;
	}

	private Object getValueFrom(Map<String, Object> thingTypeFieldMap) {
        if (Long.valueOf(thingTypeFieldMap.get("type").toString()).equals(ThingTypeField.Type.TYPE_THING_TYPE.value)) {
            Long thingTypeId = Long.valueOf(thingTypeFieldMap.get("dataTypeThingTypeId").toString());
            return ThingTypeService.getInstance().get(thingTypeId);
        } else if (Long.valueOf(thingTypeFieldMap.get("type").toString()).equals(ThingTypeField.Type.TYPE_ZONE.value)) {
            Zone tempZone = new Zone();
            Map<String, Object> zoneMap = tempZone.publicMapSummarized();
            zoneMap.put("facilityMap", "");
            zoneMap.put("zoneGroup", "");
            return replaceNullsFromMap(zoneMap, "");
        } else if (Long.valueOf(thingTypeFieldMap.get("type").toString()).equals(ThingTypeField.Type.TYPE_GROUP.value)) {
            Group tempGroup = new Group();
            return replaceNullsFromMap(tempGroup.publicMap(),"");
        } else if (Long.valueOf(thingTypeFieldMap.get("type").toString()).equals(ThingTypeField.Type.TYPE_LOGICAL_READER.value)) {
            LogicalReader tempLogicalReader = new LogicalReader();
            return replaceNullsFromMap(tempLogicalReader.publicMap(),"");
        } else if (Long.valueOf(thingTypeFieldMap.get("type").toString()).equals(ThingTypeField.Type.TYPE_SHIFT.value)) {
            Shift tempShift = new Shift();
            return replaceNullsFromMap(tempShift.publicMap(),"");
        } else
            return thingTypeFieldMap.get("defaultValue");
	}

	/*******************************************************
	 This method sets message to response
     ******************************************************/
	public ValidationBean setMessageValidation (List<String> messages)
	{
		ValidationBean response = new ValidationBean();
		if(messages!=null && messages.size()>0)
		{
			String msg = "";
			for (String a: messages)
			{
				msg = msg + a+"|||";
			}
			response.setErrorDescription(msg.substring(0,msg.length()-3));
		}
		return response;
	}


	// todo why PATCH? should it be PUT?
	@PATCH
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@RequiresPermissions(value = { "thingType:u" })
	@ApiOperation("Update Thing Type")
	public Response updateThingType( @PathParam("id") Long thingTypeId, Map<String, Object> thingTypeMap )
{
    // 1.1.- Validating expression fields
    ValidationBean validationBean1 = ValidatorService.validateExpressionFields(thingTypeMap);
    if (validationBean1.isError()) {
        return RestUtils.sendBadResponse(validationBean1.getErrorDescription());
    }
	//1. Validation
	List<Map<String,Object>> fields = (List<Map<String, Object>>) thingTypeMap.get("fields");
	List<String> errorFields = new ArrayList<>();
	for(Map<String, Object> field : fields ){
		if(ThingType.NonUDF.getEnums().contains(field.get("name").toString())){
			errorFields.add(field.get("name").toString());
		}
	}
	if(errorFields.size() > 0){
		return RestUtils.sendBadResponse("Field names are not allowed because are reserved words " + errorFields + ".");
	}
	ValidationBean validationBean = validateUpdateThingType( thingTypeId, thingTypeMap );
	if(validationBean.isError())
	{
		return RestUtils.sendBadResponse( validationBean.getErrorDescription() );
	}
	ThingType thingType = ThingTypeService.getInstance().get( thingTypeId );
	if (thingType == null) {
		return RestUtils.sendBadResponse("ThingTypeId not found");
	}
	if( thingTypeMap.containsKey( "name" ) )
	{
		String name = (String) thingTypeMap.get( "name" );
		if( name == null || StringUtils.isEmpty( name ) || name.contains( "(" ) || name.contains( ")" ) || name.contains( "|" )
			|| name.contains( "&" ) || name.contains( "$") )
		{
			throw new UserException( "Invalid Name" );
		}
		thingType.setName( name );
	}
	RecentService.getInstance().updateName(thingType.getId(), thingType.getName(),"thingtype");
	List<ThingType> parentThingTypesToDelete = new LinkedList<>();
	List<ThingType> childrenThingTypesToDelete = new LinkedList<>();
	if (thingTypeMap.containsKey("parent.ids")) {
		Set<ThingTypeMap> parentMapsOld = thingType.getParentTypeMaps();
		if (parentMapsOld != null) {
			thingType.setParentTypeMaps(new HashSet<ThingTypeMap>());
			for (ThingTypeMap parentMap : parentMapsOld) {
				if (parentMap.getParent().getChildrenTypeMaps()!=null){
					parentMap.getParent().getChildrenTypeMaps().remove(parentMap);
				}
				ThingTypeMapService.getInstance().delete(parentMap);
				parentThingTypesToDelete.add(parentMap.getParent());
			}
		}
		for (Number n : (List<Number>) thingTypeMap.get("parent.ids")) {
			ThingType parent = ThingTypeService.getInstance().get(n.longValue());
			if (parent != null) {
				ThingTypeMap tTM = new ThingTypeMap();
				tTM.setParent(parent);
				tTM.setChild(thingType);
				ThingTypeMap newRelation = ThingTypeMapService.getInstance().insert(tTM);
				thingType.getParentTypeMaps().add(newRelation);
				parent.getChildrenTypeMaps().add(newRelation);
				parentThingTypesToDelete.remove(parent);
			}
		}
	}
	if (thingTypeMap.containsKey("child.ids")) {

		Set<ThingTypeMap> childMaps = thingType.getChildrenTypeMaps();
		if (childMaps != null) {
			thingType.setChildrenTypeMaps(new HashSet<ThingTypeMap>());
			for (ThingTypeMap childMap : childMaps) {
				childMap.getChild().getParentTypeMaps().remove(childMap);
				ThingTypeMapService.getInstance().delete(childMap);
				childrenThingTypesToDelete.add(childMap.getChild());
			}
		}
		for (Number n : (List<Number>) thingTypeMap.get("child.ids")) {
			ThingType child = ThingTypeService.getInstance().get(n.longValue());
			if (child != null) {
				ThingTypeMap tTM = new ThingTypeMap();
				tTM.setParent(thingType);
				tTM.setChild(child);
				ThingTypeMap newRelations = ThingTypeMapService.getInstance().insert(tTM);
				if (thingType.getChildrenTypeMaps() == null) {
					thingType.setChildrenTypeMaps(new HashSet<>());
				}
				thingType.getChildrenTypeMaps().add(newRelations);
				if (child.getParentTypeMaps() == null) {
					child.setParentTypeMaps(new HashSet<>());
				}
				child.getParentTypeMaps().add(newRelations);
				childrenThingTypesToDelete.remove(child);
			}
		}
	}

	if( thingTypeMap.containsKey( "childrenUdf" ) )
	{
		List<Map<String, Object>> childrenUdf = (List<Map<String, Object>>) thingTypeMap.get( "childrenUdf" );
		if(childrenUdf!=null && childrenUdf.size()>0)
		{
			for( Object objChildrenUdf : childrenUdf )
			{
				Map<String, Object> childrenMap = (Map<String, Object>) objChildrenUdf;
				ThingType child = ThingTypeService.getInstance().get( Long.parseLong( childrenMap.get( "id" ).toString() ) );
				if(childrenMap.get( "operation" ).toString().equals( "add" ))
				{
					ThingTypeField field = new ThingTypeField();
					field.setName(childrenMap.get( "property" ).toString());
					field.setDataType( DataTypeService.getInstance().get( ThingTypeField.Type.TYPE_THING_TYPE.value ) );
					field.setDataTypeThingTypeId( thingType.getId() );
					field.setDefaultValue( null );
					field.setMultiple( false );
					field.setSymbol( null );
					field.setThingType( child );
					field.setThingTypeFieldTemplateId( null );
					field.setTimeSeries( false );
					field.setTimeToLive( null );
					field.setTypeParent( ThingTypeField.TypeParent.TYPE_PARENT_NATIVE_THING_TYPE.value);
					field.setUnit( null );
					ThingTypeFieldService.getInstance().insert(field);
				}else if(childrenMap.get( "operation" ).toString().equals( "delete" ))
				{
					ThingTypeField thingField = null;
					for(ThingTypeField thingTypeField: child.getThingTypeFields())
					{
						if(thingTypeField.getDataTypeThingTypeId()!=null
						   && thingTypeField.getDataTypeThingTypeId().compareTo( thingTypeId )==0)
						{
							thingField = thingTypeField;
							break;
						}
					}
					child.getThingTypeFields().remove( thingField );
					ThingTypeService.getInstance().update( child );
				}
			}
		}
	}
	if( thingTypeMap.containsKey( "defaultOwnerGroupType.id" ) )
	{
		GroupType groupType = GroupTypeService.getInstance()
											  .get( ((Number) thingTypeMap.get( "defaultOwnerGroupType.id" )).longValue() );
		thingType.setDefaultOwnerGroupType( groupType );
	}

	User currentUser = (User) SecurityUtils.getSubject().getPrincipal();
	Group visibilityGroup = VisibilityUtils.getVisibilityGroup( ThingType.class.getCanonicalName(), null );
	if( GroupService.getInstance().isGroupNotInsideTree( thingType.getGroup(), visibilityGroup ) )
	{
		throw new ForbiddenException( "Forbidden ThingType" );
	}

	if( thingTypeMap.containsKey( "autoCreate" ) )
	{
		thingType.setAutoCreate( (Boolean) thingTypeMap.get( "autoCreate" ) );
	}
	if( thingTypeMap.containsKey( "thingTypeCode" ) )
	{
		thingType.setThingTypeCode( (String) thingTypeMap.get( "thingTypeCode" ) );
	}
	if( thingTypeMap.containsKey( "isParent" ) )
	{
		thingType.setIsParent( (boolean) thingTypeMap.get( "isParent" ) );
	}
	if( thingTypeMap.containsKey("serialFormula") && thingTypeMap.get("serialFormula") instanceof String )
	{
		thingType.setSerialFormula( ((String) thingTypeMap.get( "serialFormula" )).trim() );
		if( !((String) thingTypeMap.get("serialFormula")).trim().isEmpty() )
		{
			// validate formula field
			if( thingTypeMap.containsKey( "fields" ) ) {
				List<Map<String, Object>> thingsTypeMap = (List<Map<String, Object>>) thingTypeMap.get("fields");
				if (thingsTypeMap != null && !thingsTypeMap.isEmpty()) {
					Map<String, Object> ttf = new HashMap<String, Object>();
					for (Map<String, Object> thingTypeFieldMap : thingsTypeMap) {
						Map<String, Object> value = new HashMap<String, Object>();
						value.put("value", thingTypeFieldMap.get("defaultValue"));
						ttf.put((String) thingTypeFieldMap.get("name"), value);
					}
					FormulaUtil.validateFormula(thingType.getSerialFormula(), ttf, true);
				} else {
					throw new UserException("It is necessary to configure almost a Thing Type Property for evaluating serial's expression");
				}
			}
		}
	}

	validateUpdate( thingType );
	List<ThingTypeField> thingTypeFieldUDFList = new LinkedList<>();
	List<ThingTypeField> thingTypeFieldToDelete = new LinkedList<>();
	if( thingTypeMap.containsKey( "fields" ) )
	{
		// validate formula field
		List<Map<String, Object>> thingsTypeMap = (List<Map<String, Object>>) thingTypeMap.get( "fields" );
		if( thingsTypeMap != null ) {
			Map<String, Object> ttf = new HashMap<String, Object>();
			for (Map<String, Object> thingTypeFieldMap : thingsTypeMap) {
				Map<String, Object> value = new HashMap<String, Object>();
				value.put("value", getValueFrom(thingTypeFieldMap));
				ttf.put((String) thingTypeFieldMap.get("name"), value);
			}
			for (Map<String, Object> thingTypeFieldMap : thingsTypeMap) {
				if (thingTypeFieldMap.get("type") instanceof Integer) {
					if (Long.parseLong(thingTypeFieldMap.get("type").toString()) == ThingTypeField.Type.TYPE_FORMULA.value) {
						FormulaUtil.validateFormula((String) thingTypeFieldMap.get("defaultValue"), ttf, true);
					}
				}
			}

			// update fields even though thingsTypeMap is empty
			try {
				ThingTypeService.getInstance().updateFields(thingsTypeMap, thingType, true, thingTypeFieldUDFList, thingTypeFieldToDelete);
			} catch (ConstraintViolationException e) {
				String message = String.format("A Thing Type udf could not be deleted from [%s] because it is referenced in a report.", thingType.getName());
				logger.error(message);
				return RestUtils.sendBadResponse(message);
			} catch (UserException e) {
				logger.error(e.getMessage(), e);
				return RestUtils.sendBadResponse(e.getMessage());
			}
		}
	}

	thingType.setModifiedTime(new Date().getTime());

	try {
		thingType = ThingTypeService.getInstance().update(thingType);
	} catch (ConstraintViolationException e) {
		logger.error(String.format("Error updating Thing Type [%s]. Please review references to UDFs in report properties.", thingType.getName()));
		return RestUtils.sendBadResponse( String.format( "Error updating Thing Type [%s]. Please review references to UDFs in report properties.", thingType.getName() ) );
	} catch (UserException e)
	{
		String message = String.format("Error updating Thing Type [%s]. Please review references to UDFs in Bridges Rules.", thingType.getName());
		return RestUtils.sendBadResponse( message);
	}

	if( thingType == null )
	{
		return RestUtils.sendBadResponse( String.format( "Error updating Thing Type with Id [%s].", thingTypeId ) );
	}
	else
	{
		ThingTypeService.getInstance().associate(thingType,
				thingTypeFieldUDFList, thingTypeFieldToDelete,
				parentThingTypesToDelete, childrenThingTypesToDelete);
        commitTransaction();
        openTransaction();
        thingType = ThingTypeService.getInstance().get(thingType.getId());//fresh version of commited tt
        ThingTypeService.getInstance().putOneInCache(thingType);
        DataTypeService.getInstance().replaceThingTypeFields(thingType.getThingTypeFields());
        Group groupFromCache = GroupService.getInstance().getFromCache(thingType.getGroup().getHierarchyName());
		// ThingTypeService.fillSets(thingTypeUpdated);
		// RIOT-13659 send tickle for thing type.
		BrokerClientHelper.sendRefreshThingTypeMessage(false, GroupService.getInstance().getMqttGroups(groupFromCache));
		ThingTypeService.getInstance().refreshCache(thingType, false);
		return RestUtils.sendOkResponse( thingType.publicMap( true, true ) );
	}
}

	public void validateUpdate( ThingType thingType )
	{
		validateParentLevel( thingType );
	}

	@DELETE
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@RequiresPermissions(value = { "thingType:d" })
	@ApiOperation("Delete Thing Type")
	public Response deleteThingType( @PathParam("id") Long thingTypeId )
	{
		ThingType thingType = ThingTypeService.getInstance().get( thingTypeId );
		if( thingType == null )
		{
			return RestUtils.sendBadResponse( String.format( "ThingTypeId[%d] not found", thingTypeId ) );
		}
		else if( thingType.isArchived() )
		{
			return RestUtils.sendBadResponse( String.format( "ThingTypeId[%d] archived", thingTypeId));
		}

		Group visibilityGroup = VisibilityUtils.getVisibilityGroup( ThingType.class.getCanonicalName(), null );
		if( GroupService.getInstance().isGroupNotInsideTree( thingType.getGroup(), visibilityGroup ) )
		{
			throw new ForbiddenException( "Forbidden ThingType" );
		}
        Group groupFromCache = GroupService.getInstance().getFromCache(thingType.getGroup().getHierarchyName());
		List<Long> groupMqtt = GroupService.getInstance().getMqttGroups(groupFromCache);
		List<ThingTypeField> thingTypeFields = new LinkedList<>();
		for( ThingTypeField thingTypeField : thingType.getThingTypeFields() )
		{
			thingTypeFields.add( thingTypeField );
		}

		for( ThingTypeField thingTypeField : thingTypeFields )
		{
			thingType.getThingTypeFields().remove( thingTypeField );
			ThingTypeFieldService.getInstance().delete( thingTypeField );
		}
		validateDelete( thingType );
		//Validate if the thing type or thing type property is used in Bridges Rules
		String validarules = EdgeboxRuleService.getInstance().geRulesUsingThingTypeOrProperty(thingType.getCode());
		if( (validarules !=null) && (!validarules.isEmpty()))
		{
			return RestUtils.sendBadResponse( String.format( "ThingType '%s' cannot be deleted it is used in Bridges Rules: %s",
					thingType.getCode(), validarules));
		}
		ThingTypeService.getInstance().disassociateAll(thingType);
		RecentService.getInstance().deleteRecent(thingType.getId(), "thingtype");
		ThingTypeService.getInstance().delete( thingType );
        ThingTypeService.getInstance().removeOneFromCache(thingType.getThingTypeCode());
        DataTypeService.getInstance().removeThingTypeFieldsFromCache(thingType.getThingTypeCode());
		// RIOT-13659 send tickle for thing type.
		BrokerClientHelper.sendRefreshThingTypeMessage(false, groupMqtt);
		ThingTypeService.getInstance().refreshCache(thingType, true);
		return RestUtils.sendDeleteResponse();
	}

	public void validateDelete( ThingType thingType )
	{
		// throw new UserException( "delete validation failed" )
	}

	// this method used by CoreBridge to instantiate new things
	/*@POST
	@Path("/many/{id}")
	//@Consumes(MediaType.TEXT_PLAIN)
	//@Produces(MediaType.TEXT_PLAIN)
	@RequiresAuthentication
	@ApiOperation("Instantiate many Things from a ThingType")
	public Response instantiateManyThings( @PathParam("id") Long thingTypeId, @QueryParam("groupId") Long groupId, String body )
	{
		long t0 = System.currentTimeMillis();
		//logger.info( "REQUEST:\n" + body );
		StringBuilder sb = new StringBuilder();
		String [] lines = body.split( "\n" );

		ThingType thingType = ThingTypeService.getInstance().get( thingTypeId );

		long t1 = System.currentTimeMillis();

		//logger.info( "got thingType=" + thingType );
//		if( !PermissionsUtils.isPermitted( SecurityUtils.getSubject(), Resource.THING_TYPE_PREFIX + thingType.getId() + ":i" ) )
//		{
//			throw new ForbiddenException( "Not Allowed access" );
//		}

		User currentUser = (User) SecurityUtils.getSubject().getPrincipal();

		long t2 = System.currentTimeMillis();

		//logger.info( "got user=" + currentUser );

//		Group visibilityGroup = VisibilityUtils.getVisibilityGroup( ThingType.class.getCanonicalName(), null );
		//logger.info( "got group=" + visibilityGroup );

//		if( GroupService.getInstance().isGroupNotInsideTree( thingType.getGroup(), visibilityGroup ) )
//		{
//			throw new ForbiddenException( "Forbidden thing" );
//		}

		//logger.info( "checked group tree" );
		Group g = GroupService.getInstance().get( groupId );
		logger.info( "instantiating things: thingTypeId=" + thingType.getId() + " count=" + lines.length );

		long t3 = System.currentTimeMillis();

		delt1 = 0;
		delt2 = 0;
		for( String line : lines )
		{
			String serial = line.trim();
			//logger.info( "LINE=" + line );
			// throws "org.hibernate.exception.ConstraintViolationException: could not execute statement" exception if not unique
			//TODO: catch ConstraintViolationException !
			String str = instantiateThingType2( thingType, serial, g, currentUser );
			sb.append( str );
			//logger.info( "response='" + str + "'" );
		}

		//logger.info( "RESPONSE:\n" + sb.toString() );
		ThingService.getThingDAO().getSession().clear();

		long t4 = System.currentTimeMillis();

		long t5 = System.currentTimeMillis();

		Response r = Response.status( 200 ).header( "content-type", "text/plain"/*"application/json"*/
	/*).entity( sb.toString() ).build();

		long t6 = System.currentTimeMillis();

		if( logger.isDebugEnabled() )
		{
		logger.debug( String.format( "THING INSERT: 1=%.3f 2=%.1f t=%.3f", delt1/1000.0 , delt2/1000.0, (delt1 + delt2)/1000.0 ) );
		logger.debug( String.format( "1=%.3f 2=%.3f 3=%.3f 4=%.3f 5=%.3f 6=%.3f t=%.3f", (t1-t0)/1000.0, (t2-t1)/1000.0, (t3-t2)/1000.0,
				(t4-t3)/1000.0, (t5-t4)/1000.0, (t6-t5)/1000.0, (t6-t0)/1000.0 ) );
		}
		return r;
	}

	*/

	public synchronized void modifyRequestsInProgress(int value)
	{
		requestsInProgress = requestsInProgress+ value;
	}

	//TODO: Move this to another class!
	public static final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 200);
	/**
	 * USED BY CORE BRIDGE TO INSTANTIATE NEW THINGS
	 *
	 * @param thingTypeId
	 * @param groupId
	 * @param body
	 * @param useDefaultValues
	 * @return
	 */
	@POST
	@Path("/many/{id}")
	@RequiresAuthentication
	@ApiOperation("Instantiate many Things from a ThingType")
	public Response instantiateManyThings(
			@ApiParam(value = "thingType id", required = true) @PathParam("id") Long thingTypeId,
			@ApiParam(value = "group id", required = true) @QueryParam("groupId") Long groupId,
			@ApiParam(value = "list of serial numbers, one serial number per line", required = true) String body,
			@ApiParam(value = "default values", required = false) @QueryParam("useDefaultValues") Boolean
					useDefaultValues) {


		logger.info("[INSTANTIATEMANY] Requests in Progress: "+requestsInProgress);
		if(maxRequestsInProgress>0) {
			logger.info("[INSTANTIATEMANY]  Max Requests Allowed: " + maxRequestsInProgress);
		}else{
			logger.info("[INSTANTIATEMANY]  Max Requests Allowed: No limit");
		}
		int maxPoolSize = ((ThreadPoolExecutor) executor).getMaximumPoolSize();
		int activeCount = ((ThreadPoolExecutor) executor).getActiveCount();
		int queueSize = ((ThreadPoolExecutor) executor).getQueue().size();
		logger.info("[INSTANTIATEMANY] maxPoolSize: "+maxPoolSize+" | activeCount: "+activeCount+" | queueSize: "+queueSize);
		String tmpMsg="";
		StringBuilder sb = new StringBuilder();
		String[] lines = body.split("\n");
		int count = lines.length;
		if((count==1)||(maxRequestsInProgress==0)||(requestsInProgress<=maxRequestsInProgress)) {
			modifyRequestsInProgress(count);


			logger.info("[INSTANTIATEMANY] Processing " + requestsInProgress);


			logger.info("[INSTANTIATEMANY] START: Instantiate many Things from a ThingType. Thing Type ID: " + thingTypeId + " Group ID: " + groupId);
			Date iniTime = new Date();

			ThingType thingType = ThingTypeService.getInstance().get(thingTypeId);
			Group group = GroupService.getInstance().get(groupId);
			final Boolean useDafaults = useDefaultValues == null ? true : useDefaultValues;
			//useDefaultValues = useDefaultValues == null ? true : useDefaultValues;
			Subject subject = SecurityUtils.getSubject();
			//User currentUser = (User) subject.getPrincipal();

			//ThingService thingService = ThingService.getInstance();
			//Date storageDate = new Date();
			//Map<String, Object> result;
			//Map<String, Object> resultOne = new HashMap<>();
			//int count = 0;
			//String serial;
			sb.append("[\n");
		/*String responseList = Arrays.asList(lines)
				.parallelStream()
				.map(line -> instanceOne(line, thingType, group, useDafaults, currentUser))
				.collect(Collectors.joining(",\n"));*/
			//sb.append(responseList);
			List<CompletableFuture<String>> futuresResponse = Arrays.asList(lines)
					.parallelStream()
							//.stream()
					.map(line -> CompletableFuture.supplyAsync(() -> instanceOne(
							line, thingType, group, useDafaults, subject), executor))
					.collect(Collectors.toList());
			String responseList = futuresResponse.stream()
					.map(CompletableFuture::join)
					.collect(Collectors.joining(",\n"));
			sb.append(responseList);
		/*for (String line : lines) {
			if (count > 0) {
				sb.append(",\n");
			}

			if (line.contains(",")) {
				String data[] = line.trim().split(",");
				serial = data[0];
				storageDate = DateHelper.getDateAndDetermineFormat(data[1]);
			} else {
				serial = line.trim();
			}

			try {
				Map<String, Boolean> validations = new HashMap<>();
				validations.put("thingType", true);
				validations.put("group", true);
				validations.put("thing.maxNumberThings", true);
				validations.put("thing.serial", true);
				validations.put("thing.parent", false);
				validations.put("thing.children", false);
				validations.put("thing.childrenUDF", false);
				validations.put("thing.udfs", false);
				//Create a new Thing
				Stack<Long> recursivelyStack = new Stack<>();
				result = thingService.upsert(
						recursivelyStack,
						thingType,
						group.getHierarchyName(),
						serial, // name
						serial, // serialNumer
						null, // parent
						null, // udfs
						null, // children
						null, // childrenUdf
						false, // executeTickle
						false, // validateVisibility
						storageDate, // transactionDate
						false, // disableFMCLogic
						true, // createAndFlush. Changed to true. Flush for each thing created (trying to avoid LockTimeoutException)
						useDefaultValues,
						validations,
						currentUser,
						false);

				sb.append(buildResponseMany(result));
			} catch (UserException e) {
				resultOne.put(count + "-" + serial, e.getMessage());
				//sb.append( "thingId=error,"+serial+","+ e.getMessage()+"\n" );
				sb.append("{\n");
				sb.append(" \"error\" : 1,\n");
				sb.append(" \"serialNumber\" : \"" + serial + "\"\n,");
				sb.append(" \"message\" : \"" + e.getMessage() + "\"\n");
				sb.append("}\n");

			}
			count++;
		}*/
			sb.append("]\n");
			// the following line is not necessary because createAndFlush upsert parameter is true
//		HibernateSessionFactory.getInstance().getCurrentSession().flush();
			activeCount = ((ThreadPoolExecutor) executor).getActiveCount();
			queueSize = ((ThreadPoolExecutor) executor).getQueue().size();

			Response response = Response.status(200).header("content-type", "text/plain").entity(sb.toString()).build();
			Date endTime = new Date();

			double totalTime = (endTime.getTime() - iniTime.getTime()) / 1000.0;
			double rate = count / totalTime;
			logger.info("[INSTANTIATEMANY]END: Instantiate many Things from a ThingType. Thing Type ID: " + thingTypeId + " Group ID: " + groupId
					+ " thing_count=" + count + ", time=" + String.format("%.3f", totalTime) + " [sec], rate=" + String.format("%.1f", rate) + " [things/sec]");
			logger.info("[INSTANTIATEMANY] maxPoolSize: "+maxPoolSize+" | activeCount: "+activeCount+" | queueSize: "+queueSize);
			logger.info(Arrays.toString(lines));
			modifyRequestsInProgress(0-count);
			return response;
		}else{
			logger.info("[INSTANTIATEMANY] Back Pressure ");
			logger.info("[INSTANTIATEMANY]"+Arrays.toString(lines));
			Response response = Response.status(502).header("content-type", "text/plain").entity("Please try again later..").build();
			return response;
		}

	}

	private void openTransaction(){
		Session session = HibernateSessionFactory.getInstance().getCurrentSession();
		Transaction transaction = session.getTransaction();
		if(!transaction.isActive()){
			transaction.begin();
		} //else {
			//logger.warn("Joining to another transaction");
		//}
	}

	private void commitTransaction(){
		Session session = HibernateSessionFactory.getInstance().getCurrentSession();
		Transaction transaction = session.getTransaction();
		if(transaction.isActive()){
			transaction.commit();
			//logger.warn("Transaction commited");
		}
	}

	private String instanceOne(String line, ThingType thingType, Group group, Boolean useDefaultValues, Subject subject){
		String response;
		Map<String, Object> result;
		ThingService thingService = ThingService.getInstance();
		String serial;
		Date storageDate = null;
		if(line.contains(",")){
			String data[] = line.trim().split(",");
			serial = data[0];
			storageDate = DateHelper.getDateAndDetermineFormat(data[1]);
		} else {
			serial = line.trim();
		}

		try{
			Map<String, Boolean> validations = new HashMap<>();
			validations.put("thingType", true);
			validations.put("group", true);
			validations.put("thing.maxNumberThings", true);
			validations.put("thing.serial", true);
			validations.put("thing.parent", false);
			validations.put("thing.children", false);
			validations.put("thing.childrenUDF", false);
			validations.put("thing.udfs", false);
			Stack<Long> recursivelyStack = new Stack<>();
			openTransaction(); //open a transaction could be slow
			storageDate = (storageDate == null) ? new Date() : storageDate;
			/*Session session = HibernateSessionFactory.getInstance().getCurrentSession();
			Transaction transaction = session.getTransaction();
			if(!transaction.isActive()){
				transaction.begin();
			}*/ /*else {
				logger.warn("Joining to another transaction");
			}*/
			result = thingService.upsert(
					recursivelyStack,
					thingType,
					group.getHierarchyName(),
					serial, // name
					serial, // serialNumer
					null, // parent
					null, // udfs
					null, // children
					null, // childrenUdf
					false, // executeTickle
					false, // validateVisibility
					storageDate, // transactionDate
					false, // disableFMCLogic
					true, // createAndFlush. Changed to true. Flush for each thing created (trying to avoid LockTimeoutException)
					useDefaultValues,
					validations,
					subject,
					false);
			response = buildResponseMany(result);
			commitTransaction();
			/*if(transaction.isActive()){
				transaction.commit();
			} else {
				logger.warn("Thing will not be committed");
			}*/
		//}catch(UserException e){
		}catch(Exception e){
			logger.error("Exception in upsert"+
					"Line: "+line+
					"thingType: "+thingType.toString()+
					"Group: "+group.toString()+
					"useDefaultValues: "+useDefaultValues+
					"currentUser: "+((User)subject.getPrincipal()).toString(), e);
			response = "{\n"+" \"error\" : 1,\n" + " \"serialNumber\" : \"" + serial + "\"\n," + " \"message\" : \"" + e.getMessage() + "\"\n" + "}\n";
		}
		return response;
	}




	private String buildResponseMany(Map<String, Object> result)
	{
		StringBuffer sb = new StringBuffer();

		Map<String, Object> t = (Map<String, Object>) result.get("thing");
		long time = (long)result.get("time");
		Thing thing = ThingService.getInstance().get( (Long) t.get( "id" ) );

		sb.append( "{\n" );
		sb.append( " \"id\" : " + thing.getId() + ",\n" );
		sb.append( " \"name\" : \"" + thing.getName() + "\",\n" );
		sb.append( " \"serialNumber\" : \"" + thing.getSerialNumber() + "\",\n" );
		sb.append( " \"groupId\": " + thing.getGroup().getId() + ",\n" );
		sb.append( " \"thingTypeId\" : " + thing.getThingType().getId() + ",\n" );
		sb.append( " \"thingTypeCode\" : \"" + thing.getThingType().getCode() + "\",\n" );
		sb.append( " \"createdTime\" : \"" + ((Date)t.get("createdTime")).getTime() + "\",\n" );
		sb.append( " \"modifiedTime\" : \"" + ((Date)t.get("modifiedTime")).getTime() + "\",\n" );
		sb.append( " \"time\" : \"" + time + "\",\n" );
		sb.append( " \"thingFields\" : [" );

		int c = 0;
		Map<?, ?> fields = thing.getThingFields();
		for( Object key : fields.keySet() )
		{
			if( c > 0 )
			{
				sb.append( "," );
			}
			sb.append( "\n" );
			ThingField tf = (ThingField) fields.get( key );
			sb.append( " {" );
			sb.append( " \"thingTypeFieldId\" : " + tf.getThingTypeField().getId() + ",");

            Object value = null;

            if(((ThingTypeField)tf.getThingTypeField()).getDataType().getId().equals(ThingTypeField.Type.TYPE_THING_TYPE.value)){
                value = ((Map<?,?>)tf.getValue()).get("_id");
            }else if(((ThingTypeField)tf.getThingTypeField()).getDataType().getId().equals(ThingTypeField.Type.TYPE_GROUP.value)||
                    ((ThingTypeField)tf.getThingTypeField()).getDataType().getId().equals(ThingTypeField.Type.TYPE_ZONE.value)||
                    ((ThingTypeField)tf.getThingTypeField()).getDataType().getId().equals(ThingTypeField.Type.TYPE_SHIFT.value)||
                    ((ThingTypeField)tf.getThingTypeField()).getDataType().getId().equals(ThingTypeField.Type.TYPE_LOGICAL_READER.value)){
                value = ((Map<?,?>)tf.getValue()).get("id");
            }else{
                value = tf.getValue();
            }
			if(value!=null){
				sb.append(" \"value\" : \"" + StringEscapeUtils.escapeJava(value.toString()) + "\"," );
			}
			sb.append( " \"timestamp\" : " + tf.getTimestamp() + "" );
			sb.append( " }" );
			c++;
		}
		sb.append( " ]\n" );
		sb.append( "}\n" );

		return sb.toString();
	}

	@Deprecated
	public String instantiateThingType2( ThingType thingType, String serial, Group group, User user )
	{
		long t0 = System.currentTimeMillis();

		Thing thing = ThingService.getInstance().insert( thingType, serial, serial, group, user );

		long t1 = System.currentTimeMillis();

		StringBuffer sb = new StringBuffer();

		sb.append( "thingId=" + thing.getId()  );
		sb.append( "," + thing.getName() );
		sb.append( "," + thing.getSerial() );

		sb.append( "," + thing.getGroup().getId() );
		sb.append( ",\"" + thing.getGroup().getName() + "\"");

		if (thing.getGroup().getCode() == null) {
			sb.append( "," + "null" );
		} else
		{
			sb.append( ",\"" + thing.getGroup().getCode() + "\"");
		}

		sb.append( "," + thing.getGroup().getGroupType().getId() );
		sb.append( ",\"" + thing.getGroup().getGroupType().getName() + "\"" );
		if (thing.getGroup().getGroupType().getCode() == null) {
			sb.append( "," + "null" );
		} else
		{
			sb.append( ",\"" + thing.getGroup().getGroupType().getCode() + "\"");
		}

		sb.append( "," + thing.getThingType().getThingTypeCode() );

		sb.append( "\n" );

		//todo n+1 here?
		for( ThingTypeField tf : thing.getThingType().getThingTypeFields() )
		{
			sb.append( tf.getName() + "=" + tf.getId() + "," + tf.getTimeSeries() + "," + tf.getId() +"\n" );
		}

		long t2 = System.currentTimeMillis();

		delt1 += t1 - t0;
		delt2 += t2 - t1;

		return sb.toString();
	}

	private void validateSerial( String serial, ThingType thingType )
	{
		ThingService thingService = ThingService.getInstance();
		if( thingService.existsSerial( serial, thingType.getId() ) )
		{
			throw new UserException( String.format( "Serial '[%s]' already exist for Thing Type '[%s]'", serial, thingType.getName() ) );
		}
	}

	public static void validateParentLevel( ThingType thingType )
	{
		if( thingType.getParents() != null && !thingType.getParents().isEmpty() )
		{

			for( ThingTypeMap thingTypeMap2 : thingType.getParentTypeMaps() )
			{
				Set<ThingTypeMap> grandParents = thingTypeMap2.getParent().getParentTypeMaps();
				if( grandParents != null && grandParents.size() > 0 )
				{
					throw new UserException( "Only two levels is allowed." );
				}
			}
			// if (thingType.getParent().getParent() != null) {
			// throw new UserException("Only two levels is allowed.");
			// }
			if( thingType.getChildren() != null )
			{
				List<Map<String, Object>> children = (List<Map<String, Object>>) thingType.publicMap( true, true ).get( "children" );
				if( children.size() > 0 )
				{
					throw new UserException( "Only two levels is allowed." );
				}
				if( thingType.getChildren().size() > 0 )
				{
					throw new UserException( "Only two levels is allowed." );
				}
			}
		}
	}

    @Override
    public List<String> getExtraPropertyNames() {
        return Arrays.asList(IGNORE_THING_TYPE_FIELDS);
    }

	@Override
	// Just refactored nor originally from Safadi
	public void addToPublicMap( ThingType thingType, Map<String, Object> publicMap, String extra )
	{
//		User currentUser = (User) SecurityUtils.getSubject().getPrincipal();
//		DateFormatAndTimeZone dateFormatAndTimeZone = UserService.getInstance().getDateFormatAndTimeZone(currentUser);
		Map<String, Object> stringObjectMap = thingType.publicMap( true, true );
		publicMap.put( "fields", stringObjectMap.get( "fields" ) );
		publicMap.put( "children", stringObjectMap.get( "children" ) );
		publicMap.put( "parents", stringObjectMap.get( "parents" ) );

		publicMap.put( "groupId", thingType.getGroup().getId() );

		if ( (extra!=null) && (extra.contains("typeMultilevel"))) {

			publicMap.put("typeMultilevel", ThingTypeService.getInstance().getTypeMultilevel(thingType));
		}
		if(publicMap.get( "fields" )!=null )
		{
			Map<String, Object> dataType = new HashMap<>();
			List<Map<String, Object>> field = (List<Map<String, Object>>) publicMap.get( "fields" );
			for(Map<String, Object> fieldData: field)
			{
				Iterator it = fieldData.entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry pair = (Map.Entry)it.next();

					//If it is a thing Type Field it needs to have thingType
					if(pair.getKey().equals("dataTypeThingTypeId") && pair.getValue()!=null
							&& !pair.getValue().toString().trim().equals("") )

					{
						dataType = new HashMap<>();
						ThingType thingTypeUdf = ThingTypeService.getInstance().get(Long.parseLong(pair.getValue().toString()));
						dataType.put("dataTypeThingType",thingTypeUdf.publicMap() );
					}

					//Logic for default value and multiple
					if( pair.getKey().equals( "defaultValue" ) && fieldData.get("multiple") != null && fieldData.get("multiple" ).toString().equals( "true" )
							&& pair.getValue()!=null && !pair.getValue().toString().trim().equals( "" )
							&& pair.getValue().toString().split( "," ).length>1)
					{
						String [] data = pair.getValue().toString().split( "," );
						pair.setValue( data );
					}else
					{
						if( pair.getKey().equals( "defaultValue" ))
						{
							Long idDataType = Long.parseLong(((Map) fieldData.get("dataType")).get("id").toString());
							String defaultValue = String.valueOf(pair.getValue());
							if (ThingTypeFieldService.isDateTimeStampType(idDataType) && Utilities.isNumber(String.valueOf(defaultValue))) {
								User currentUser = (User) SecurityUtils.getSubject().getPrincipal();
								DateFormatAndTimeZone dateFormatAndTimeZone = UserService.getInstance().getDateFormatAndTimeZone(currentUser);
								pair.setValue(dateFormatAndTimeZone.getISODateTimeFormatWithoutTimeZone(Long.valueOf(defaultValue)));
							} else {
								pair.setValue(ThingService.getInstance().getStandardDataType(DataTypeService.getInstance().get(idDataType), pair.getValue()));
							}
						}

					}
				}

				if(dataType!=null && dataType.size()>0)
				{
					fieldData.putAll(dataType);
				}
			}
		}

		if(thingType.getThingTypeTemplate()!=null) {
			ThingTypeTemplate thingTypeTemplate = ThingTypeTemplateService.getInstance().get(thingType.getThingTypeTemplate().getId());
			publicMap.put("thingTypeTemplateId", thingType.getThingTypeTemplate().getId());
			publicMap.put( "nameTemplate",  thingTypeTemplate.getName()  );
			publicMap.put( "pathIcon", thingTypeTemplate.getPathIcon() );
		}else
		{
			publicMap.put("thingTypeTemplateId", null);
		}
		//Add data of type to fields
		LinkedList data = (LinkedList) stringObjectMap.get( "fields" );
		addTypeInfoToFields(data, thingType);
		//Add data of type to children fields
		LinkedList childrenList = (LinkedList) stringObjectMap.get( "children" );
		if(childrenList!=null && childrenList.size()>0 )
		{
			for (Object child : childrenList)
			{
				Map<String, Object> childMap = (Map<String, Object>) child;

				for(ThingType thingTypeChild: thingType.getChildren())
				{
						if(Long.parseLong( childMap.get( "id" ).toString())==  thingTypeChild.getId() )
						{
							addTypeInfoToFields((LinkedList) childMap.get( "fields" ), thingTypeChild);
						}
				}
			}
		}
		//Add data of type to parent fields
		LinkedList parentsList = (LinkedList) stringObjectMap.get( "parents" );
		if(parentsList!=null && parentsList.size()>0 )
		{
			for (Object parent : parentsList)
			{
				Map<String, Object> parentdMap = (Map<String, Object>) parent;

				for(ThingType thingTypeParent: thingType.getParents())
				{
					if(Long.parseLong( parentdMap.get( "id" ).toString())==  thingTypeParent.getId() )
					{
						addTypeInfoToFields((LinkedList) parentdMap.get( "fields" ), thingTypeParent);
					}
				}
			}
		}
	}

	/*
	* This method add the info of types to fields
	* */
	public void addTypeInfoToFields(LinkedList data, ThingType thingType)
	{
		List<DataType> dataTypes = DataTypeService.getInstance().listPaginated(null, "");

		if(data.size()>0)
		{
			for (Object fields : data)
			{
				Map<String,Object> fieldData = (Map<String,Object>) fields;
				for(ThingTypeField thingTypeField : thingType.getThingTypeFields())
				{
					if(Long.parseLong(fieldData.get("id").toString())==thingTypeField.getId())
					{

						ThingType thingTypeData = null;
						if(thingTypeField.getDataType().getTypeParent()!=null
								&& thingTypeField.getDataType().getTypeParent().equals(ThingTypeField.TypeParent.TYPE_PARENT_NATIVE_THING_TYPE.value))
						{
							DataType dataTypeParent = getEntityDescriptionByEntityCode("THING_TYPE_PROPERTY", thingTypeField.getTypeParent(), dataTypes);
							fieldData.put("typeParentId", dataTypeParent.getId());
							fieldData.put("typeParentCode", thingTypeField.getTypeParent());
							fieldData.put("typeParentDescription", dataTypeParent.getValue());
							fieldData.put("typeCode.type",thingTypeField.getTypeParent() );
							if(thingTypeField.getDataTypeThingTypeId()!=null)
							{
								thingTypeData = ThingTypeService.getInstance().get( thingTypeField.getDataTypeThingTypeId() );
								fieldData.put( "typeCode", thingTypeData.getId() );
								fieldData.put( "typeDescription", thingTypeData.getName() );
							}

						}else
						{
							DataType dataType = thingTypeField.getDataType();
							DataType dataTypeParent = getEntityDescriptionByEntityCode("THING_TYPE_PROPERTY", dataType.getTypeParent(), dataTypes);
							fieldData.put("typeParentId", dataTypeParent.getId());
							fieldData.put("typeParentCode", dataType.getTypeParent());
							fieldData.put("typeParentDescription", dataTypeParent.getValue());
							fieldData.put("typeCode",dataType.getCode());
							fieldData.put("typeDescription", getEntityDescriptionByEntityCode(dataType.getTypeParent(), dataType.getCode(), dataTypes).getValue());
							fieldData.put("typeCode.type", dataType.getType());
						}
						fieldData.put( "type", thingTypeField.getDataType().getId() );
						break;
					}
				}
			}
		}
	}

	private DataType getEntityDescriptionByEntityCode(String entityCode, String code, List<DataType> dataTypes)
	{
		for (DataType dataType : dataTypes){
			if (dataType.getCode().equals(code) && dataType.getTypeParent().equals(entityCode)){
				return dataType;
			}
		}
		return null;
	}

	@GET
	@Path("/limits")
	@Produces(MediaType.APPLICATION_JSON)
	@RequiresAuthentication
	@ApiOperation(position=20, value="Get a List of Limits for ThingTypes")
	public Response verifyObjectLimits()
	{
		User user = (User) SecurityUtils.getSubject().getPrincipal();
		HashMap<String, Number> defaultHM = new HashMap<>();
		defaultHM.put("limit", -1);
		defaultHM.put("used", 0);
		Map<String,Map> mapResponse = new HashMap<>();
		mapResponse.put("numberOfThingTypes", defaultHM);
		if (LicenseService.enableLicense) {
			LicenseDetail licenseDetail = LicenseService.getInstance().getLicenseDetail(user.getActiveGroup(), true);
			Long maxNumberOfThingTypes = licenseDetail.getMaxThingTypes();
			if (maxNumberOfThingTypes != null && maxNumberOfThingTypes > 0) {
				Long countAll = count(licenseDetail);
				defaultHM.put("limit", maxNumberOfThingTypes);
				defaultHM.put("used", countAll);
			}
		}
		return RestUtils.sendOkResponse( mapResponse );
	}

	public static Long count(LicenseDetail licenseDetail) {
		GroupService groupService = GroupService.getInstance();
		ThingTypeService thingTypeService = ThingTypeService.getInstance();
		Long countAll;
		Group licenseGroup = groupService.get(licenseDetail.getGroupId());
		boolean isRootLicense = groupService.getRootGroup().getId().equals(licenseGroup.getId());
		if (isRootLicense) {
			countAll = thingTypeService.countAllActive();
		} else {
			countAll = thingTypeService.countAllActive(licenseGroup.getParentLevel2());
		}
		return countAll;
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

	/****************************************************************************
	 * This method inverts values in the map in order to support 'childrenUDFs'
	 ****************************************************************************/
	public List<Map<String, Object>> invertValuesData(
			  List<Map<String, Object>>  originalList
			, Map<Long, Map<String, Object>> objectCache
			, Map<Long, Set<Long>> childrenMapCache
			, Map<String, Boolean> permissionCache
			, String only
			, String extra
			, String order
			, boolean validateThingTypePermission ) throws Exception
	{
		List<Map<String, Object>> response = new ArrayList<Map<String, Object>>();
		Set<String> thingTypesTemp = new HashSet<String>();
		//Ordering the list, first things with Thing Type UDF
		List<Map<String, Object>> list = ThingTypeService.getInstance().getThingTypesWithThingsUDFs( originalList, order );
		for(Object dataThingType: list)
		{
			Map<String, Object> data = new HashMap<String, Object>();
			data.putAll((Map<String,Object>) dataThingType);
			//Verify if it does not repeat
			if( !thingTypesTemp.contains( data.get( "thingTypeCode" ).toString() ) )
			{
				ThingType thingType = ThingTypeService.getInstance().getByCode(data.get("thingTypeCode").toString());
				List<Map<String, Object>> lstThingTypeUdf = ThingTypeService.getInstance()
																			.getThingTypeUdf(thingType.getThingTypeFields());

				Map<String, Object> dataNew = null;
				// Check if the thingType has ThingType as udf's
				if( lstThingTypeUdf != null && lstThingTypeUdf.size() > 0 )
				{
					int count = 0;
					for( Object ignored : lstThingTypeUdf )
					{
						Map<String, Object> dataMapThingTypeUdf = (Map<String, Object>)ignored;
						ThingType thingTypeObject = ThingTypeService.getInstance().getByCode(dataMapThingTypeUdf.get(
								"thingTypeCode").toString());

						if(thingTypeObject.isIsParent()){
							List<Map<String, Object>> listInverted = new LinkedList<>();
							mapThing(thingTypeObject,
									 objectCache,
									 childrenMapCache,
									 permissionCache,
									 listInverted,
									 false,
									 only,
									 extra,
									 order,
									 validateThingTypePermission);
							addAllDescendants(thingType,
											  objectCache,
											  childrenMapCache,
											  permissionCache,
											  listInverted,
											  false,
											  only,
											  extra,
											  order,
											  validateThingTypePermission);
							dataNew = new HashMap<String, Object>();
							dataNew.putAll(listInverted.get(0));
							dataNew.put("typeMultilevel", "UDF");
							dataNew.put("treeLevel", 1);

							data.put("parentUdf", thingTypeObject.publicMap());
							((List)dataNew.get("children")).add(data);
							if(data.get("children") != null && ((List)data.get("children")).size() > 0){
								//Add a child Thing Type  into the HashSet
								thingTypesTemp.add(((Map)((List)data.get("children")).get(0)).get("thingTypeCode")
																							 .toString());
							}
							dataNew.put("children", this.setValueTreeLevel((List)dataNew.get("children"), 1));
							//Replace or not the dataNew Map
							int numberCheck = this.checkOldValue(response,
																 Long.parseLong(dataNew.get("id").toString()));
							if(numberCheck == - 1){
								response.add(dataNew);
							}
							else if(numberCheck > - 1){
								response.set(numberCheck, dataNew);
							}

							//Add  the UDF into the HashSet
							thingTypesTemp.add(listInverted.get(0).get("thingTypeCode").toString());
							//Add the thing contained the udf in the HashSet
							thingTypesTemp.add(data.get("thingTypeCode").toString());
						}
						else{
							count++;
						}
					}
					if(count==lstThingTypeUdf.size())
					{
						response.add( data );
						//Add the thing contained the udf in the HashSet
						thingTypesTemp.add( data.get( "thingTypeCode" ).toString() );
					}
				}
				else
				{
					//Check if it is not a children
					ThingType thingTypeCheck = ThingTypeService.getInstance().get( Long.parseLong( data.get( "id" ).toString() ) );
					if( !ThingTypeService.getInstance().isChild( thingTypeCheck ) )
					{
						response.add( data );
						//Add the thing in the HashSet
						thingTypesTemp.add( data.get( "thingTypeCode" ).toString() );
						if( data.get( "children" ) != null && ((List) data.get( "children" )).size() > 0 )
						{
							this.setValueTreeLevel((List) data.get("children"), 1);
						}
					}
				}
			}
		}
		return response;
	}

	/****************************************************************************
	 * This method sets thing type UDF as children ( Parent: false)
	 ****************************************************************************/
	public List<Map<String, Object>> setThingTypeNotParent(
			List<Map<String, Object>>  originalList
			, Map<Long, Map<String, Object>> objectCache
			, Map<Long, Set<Long>> childrenMapCache
			, Map<String, Boolean> permissionCache
			, String only
			, String extra
			, String order
			, boolean validateThingTypePermission ) throws Exception
	{
		Map<String, ThingType> thingTypes = new HashMap<>();
		for (ThingType tt :  ThingTypeService.getInstance().getAllThingTypes()){
			thingTypes.put(tt.getCode(), tt);
		}
		List<Map<String, Object>> response = new ArrayList<Map<String, Object>>();
		Set<String> thingTypesTemp = new HashSet<String>();
		//Ordering the list, first things with Thing Type UDF
		List<Map<String, Object>> list = ThingTypeService.getInstance().getThingTypesWithThingsUDFs( originalList, order );
		for(Map<String,Object> data: list)
		{
			ThingType thingType;
			List<Map<String, Object>> lstThingTypeUdf;
			//Verify if it does not repeat
			if (!thingTypesTemp.contains(data.get("thingTypeCode").toString())) {
				Map<String, Object> dataNew = null;
					// Check if the thingType has ThingType as udf's
				List<Map<String, Object>> childrens = ((List<Map<String, Object>>) data.get("children"));
				List<Map<String, Object>> lstChildrenTempLevel3 = new ArrayList<>();
				if (childrens != null && !childrens.isEmpty()) {
					for (Map<String, Object> childrenData : childrens) {
//						thingType = ThingTypeService.getInstance().getByCode(childrenData.get("thingTypeCode").toString());
						thingType = thingTypes.get(childrenData.get("thingTypeCode").toString());
						lstThingTypeUdf = ThingTypeService.getInstance().getThingTypeUdf(thingType.getThingTypeFields(), Boolean.FALSE);
						List<Map<String, Object>> udfChildrenTemp= new ArrayList<>();
						for (Map<String, Object> udfChildren : lstThingTypeUdf) {
							if (!thingTypesTemp.contains(udfChildren.get("thingTypeCode").toString())) {
								ThingType thingTypeChildren = ThingTypeService.getInstance().getByCode(udfChildren.get("thingTypeCode").toString());
								Map<String, Object> objectMap = QueryUtils.mapWithExtraFields( thingTypeChildren, extra, getExtraPropertyNames() );
								addToPublicMap(thingTypeChildren, objectMap, extra);
								QueryUtils.filterOnly( objectMap, only, extra );
								QueryUtils.filterProjectionNested( objectMap, null, null);
								udfChildren.putAll(objectMap);
								udfChildren.put("treeLevel", 3);
								ThingType thingTypeCheck = ThingTypeService.getInstance().get( Long.parseLong( udfChildren.get( "id" ).toString() ) );
								if( !ThingTypeService.getInstance().isChild( thingTypeCheck ) )
								{
									udfChildren.put( "typeMultilevel", "UDF");
								}
								udfChildrenTemp.add(udfChildren);
								thingTypesTemp.add((String) udfChildren.get("thingTypeCode"));
							}
						}
							List<Map<String, Object>> listInverted = new LinkedList<>();
						mapThing(thingType, objectCache, childrenMapCache, permissionCache, listInverted, false, only, extra,
									order, validateThingTypePermission );
						addAllDescendants(thingType, objectCache, childrenMapCache, permissionCache, listInverted, false, only,
									extra, order, validateThingTypePermission );
						Map<String, Object> dataThing = new HashMap<>();
						dataThing.putAll(listInverted.get(0));
						dataThing.put("treeLevel", 2);
						List<Map<String, Object>> dataChildren = (List<Map<String, Object>>) dataThing.get("children");
						dataChildren.addAll(udfChildrenTemp);
						lstChildrenTempLevel3.add(dataThing);
							}
				}

				data.put("children", lstChildrenTempLevel3);
				List<Map<String, Object>> lstChildrenTemp = new ArrayList<>();
				thingType = ThingTypeService.getInstance().getByCode(data.get("thingTypeCode").toString());
				lstThingTypeUdf = ThingTypeService.getInstance().getThingTypeUdf(thingType.getThingTypeFields());
				if (lstThingTypeUdf != null && lstThingTypeUdf.size() > 0) {
					lstChildrenTemp.addAll(getChildrenData(lstThingTypeUdf, objectCache, childrenMapCache, only, permissionCache, extra, order, validateThingTypePermission, 2));
					for (Map<String, Object> ignored : lstChildrenTemp) {
						thingTypesTemp.add((String) ignored.get("thingTypeCode"));
					}
					List<Map<String, Object>> listInverted = new LinkedList<>();
					mapThing( thingType, objectCache, childrenMapCache, permissionCache, listInverted, false, only, extra,
							order, validateThingTypePermission );
					addAllDescendants( thingType, objectCache, childrenMapCache, permissionCache, listInverted, false, only,
							extra, order, validateThingTypePermission );
					thingTypesTemp.add( listInverted.get( 0 ).get( "thingTypeCode" ).toString() );
					Map<String, Object> dataThing = new HashMap<String, Object>();
					dataThing.putAll( listInverted.get( 0 ) );
					dataThing.put( "treeLevel", 1 );
					List<Map<String, Object>> dataChildren = (List<Map<String, Object>>) dataThing.get( "children" );
					dataChildren.addAll( lstChildrenTemp );
					response.add( dataThing );
				}else
				{
					response.add( data );
					//Add the thing contained the udf in the HashSet
					thingTypesTemp.add( data.get( "thingTypeCode" ).toString() );
				}
			}
		}
		return response;
	}

	/**
	 * return mapping children data
	 *
	 * @param lstThingTypeUdf
	 * @param objectCache
	 * @param childrenMapCache
	 * @param only
	 * @param permissionCache
	 * @param extra
	 * @param order
	 * @param validateThingTypePermission
	 * @param level
	 * @return {@link List}<{@link Map}<{@link String}, {@link Object}>>
	 * @throws Exception
     */
	private List<Map<String, Object>> getChildrenData(List<Map<String, Object>> lstThingTypeUdf,
													  Map<Long, Map<String, Object>> objectCache,
													  Map<Long, Set<Long>> childrenMapCache, String only,
													  Map<String, Boolean> permissionCache, String extra,
													  String order, boolean validateThingTypePermission, int level) throws Exception {
		List<Map<String, Object>> lstChildrenTemp = new ArrayList<>();
		for (Map<String, Object> dataMapThingTypeUdf : lstThingTypeUdf) {
			ThingType thingTypeObject = ThingTypeService.getInstance()
					.getByCode(dataMapThingTypeUdf.get("thingTypeCode").toString());
			List<Map<String, Object>> listInverted = new LinkedList<>();
			mapThing(thingTypeObject, objectCache, childrenMapCache, permissionCache, listInverted, false, only, extra,
					order, validateThingTypePermission);
			addAllDescendants(thingTypeObject, objectCache, childrenMapCache, permissionCache, listInverted, false, only,
					extra, order, validateThingTypePermission);
			Map<String, Object> dataNew = new HashMap<>();
			dataNew.putAll(listInverted.get(0));
			dataNew.put("treeLevel", level);
			ThingType thingTypeCheck = ThingTypeService.getInstance().get(Long.parseLong(dataNew.get("id").toString()));
			if (!ThingTypeService.getInstance().isChild(thingTypeCheck)) {
				dataNew.put("typeMultilevel", "UDF");
			}
			lstChildrenTemp.add(dataNew);
		}
		return lstChildrenTemp;
	}

	/****************************************************************************
	 * This method sets thing type UDF as children ( Parent: false)
	 ****************************************************************************/
	public List<Map<String, Object>> setThingTypeNotParent(
			List<Map<String, Object>>  originalList)
	{
		//Map<Integer, Integer> ids = new HashMap<>();
		List<Object> ids = new ArrayList<>();
		if(originalList!=null && originalList.size()>0)
		{
			for(Object obj: originalList) //Main list
			{
				Map<String, Object> objMap = (Map) obj;
				int count = 0;
				for(Object obj2: originalList) //Search children to be replaced
				{
					count = 0;
					Map<String, Object> obj2Map = (Map) obj2;
					//Case: If exists children in a first level, replace data of the children  with the first level
					if(obj2Map.get("children")!=null && ((List) obj2Map.get("children")).size()>0)
					{
						for(Object objChild: (List) obj2Map.get("children"))
						{
							Map<String, Object> objChildMap = (Map) objChild;
							if(objChildMap.get("thingTypeCode").toString().equals(objMap.get("thingTypeCode").toString()))
							{
								//replace children of this Ids, for this other
								ids.add(obj);
								count++;
								break;
							}
						}
					}

					if(count>0)
					{
						break;
					}
				}
			}
			/*for(Object obj: originalList) //Main list
			{
				Map<String, Object> objMap = (Map) obj;
				int count = 0;
				for(Object obj2: originalList) //Search children to be replaced
				{
					count = 0;
					Map<String, Object> obj2Map = (Map) obj2;
					//1 case: If there is the same parent, delete duplicated parent and merge children in one parent,
					if(originalList.indexOf(obj)!=originalList.indexOf(obj2) && ids.indexOf(obj2)==-1 &&
							objMap.get("thingTypeCode").toString().equals(obj2Map.get("thingTypeCode").toString()))
					{
						List<Map<String, Object>> objMapChild = (List)objMap.get("children");
						List<Map<String, Object>> obj2MapChild = (List)obj2Map.get("children");
						objMapChild.addAll(obj2MapChild);
						ids.add(obj2);
					}
				}
			}*/
			for(Object id : ids)
			{
				originalList.remove(id);
			}
			for(Object obj : originalList)
			{
				Map<String, Object> objMap = (Map) obj;
				objMap.put("children", this.setValueTreeLevel( (List) objMap.get( "children" ), 1 ));
			}
		}
		return originalList;
	}
	/***********************************************************
	 * Set the value treeLevel in order to have the correct level
	 ************************************************************/
	public int checkOldValue(List <Map<String, Object>> response, Long newThingTypeId)
	{
		int resp = -1;
		if(response!=null && response.size()>0)
		{
			for(Object dataObj: response)
			{
				Map<String, Object> data = (Map<String, Object>) dataObj;
				Long id = Long.parseLong( data.get( "id" ).toString() );
				if(id.compareTo( newThingTypeId )==0 )
				{
					resp = response.indexOf( dataObj );
					break;
				}
			}
		}
		return resp;
	}

	/***********************************************************
	 * Set the value treeLevel in order to have the correct level
	 ************************************************************/
	public List <Map<String, Object>> setValueTreeLevel(List <Map<String, Object>> things, int level)
	{
		List <Map<String, Object>> result =  new ArrayList();
		if(things!=null && things.size()>0)
		{
			level++;
			for(Object data : things)
			{
				Map<String, Object> child = new HashMap();
				Utilities.cloneHashMap((Map) data, child);
				child.put( "treeLevel", level);
				if(child.get( "children" )!=null && ((List)child.get( "children" )).size()>0 )
				{
					child.put("children", this.setValueTreeLevel( (List) child.get( "children" ), level ));
				}
				//data = child;
				result.add(child);
			}
		}
		return result;
	}

	@GET
	@Path("/shift/")
	@Produces(MediaType.APPLICATION_JSON)
	@RequiresAuthentication
	@ApiOperation(position=7, value="Get a List of Shifts (AUTO)")
	public Response listShifts( @QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber, @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only, @QueryParam("visibilityGroupId") Long visibilityGroupId, @DefaultValue("") @QueryParam("upVisibility") String upVisibility, @DefaultValue("") @QueryParam("downVisibility") String downVisibility , @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
	{
		validateListPermissions();
		ShiftController c = new ShiftController();
		return c.listShifts(pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility, downVisibility,false, extend, project);
	}

	@GET
	@Path("/logicalReader/")
	@Produces(MediaType.APPLICATION_JSON)
	@RequiresAuthentication
	@ApiOperation(position=7, value="Get a List of LogicalReader (AUTO)")
	public Response listLogicalReaders( @QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber, @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only, @QueryParam("visibilityGroupId") Long visibilityGroupId, @DefaultValue("") @QueryParam("upVisibility") String upVisibility, @DefaultValue("") @QueryParam("downVisibility") String downVisibility , @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
	{
		validateListPermissions();
		LogicalReaderController c = new LogicalReaderController();
		return c.listLogicalReaders(pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility, downVisibility,false, extend, project);
	}

	@GET
	@Path("/zone/")
	@Produces(MediaType.APPLICATION_JSON)
	@RequiresAuthentication
	@ApiOperation(position=7, value="Get a List of Zones (AUTO)")
	public Response listZones( @QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber, @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only, @QueryParam("visibilityGroupId") Long visibilityGroupId, @DefaultValue("") @QueryParam("upVisibility") String upVisibility, @DefaultValue("") @QueryParam("downVisibility") String downVisibility , @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
	{
		validateListPermissions();
		ZoneController c = new ZoneController();
		return c.listZones(pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility, downVisibility, false, extend, project);
	}

	/*@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	// 1a. Limit access based on CLASS level resources
	@RequiresPermissions(value={"thingType:r"})
	@ApiOperation(position=1, value="Get a List of ThingTypes (AUTO)")
	public Response listThingTypes( @QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber
			, @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra
			, @Deprecated @QueryParam("only") String only, @QueryParam("visibilityGroupId") Long visibilityGroupId
			, @DefaultValue("") @QueryParam("upVisibility") String upVisibility
			, @DefaultValue("") @QueryParam("downVisibility") String downVisibility )
	{
		Pagination pagination = new Pagination( pageNumber, pageSize );

		BooleanBuilder be = new BooleanBuilder();
		// 2. Limit visibility based on user's group and the object's group (group based authorization)
		Group visibilityGroup = VisibilityUtils.getVisibilityGroup(ThingType.class.getCanonicalName(), visibilityGroupId);
		EntityVisibility entityVisibility = getEntityVisibility();
		be = be.and( GeneralVisibilityUtils.limitVisibilitySelectAll(entityVisibility, QThingType.thingType,  visibilityGroup, upVisibility, downVisibility ) );
		// 4. Implement filtering
		be = be.and( QueryUtils.buildSearch( QThingType.thingType, where ) );

		Long count = ThingTypeService.getInstance().countList( be );
		List<Map<String, Object>> list = new LinkedList<Map<String, Object>>();
		// 3. Implement pagination
		for( ThingType thingType : ThingTypeService.getInstance().listPaginated( be, pagination, order ) )
		{
			// Additional filter
			if (!includeInSelect(thingType))
			{
				continue;
			}
			// 5a. Implement extra
			Map<String,Object> publicMap = QueryUtils.mapWithExtraFields( thingType, extra, getExtraPropertyNames());
			addToPublicMap(thingType, publicMap, extra);
			// 5b. Implement only
			QueryUtils.filterOnly( publicMap, only, extra );

			//childrenUdf
			if(extra!=null && extra.contains( "childrenUdf" ))
			{
				publicMap.put("childrenUdf",
						this.getChildrenThingTypeUdf( thingType.getId(),  ThingTypeService.getInstance().listPaginated( be, pagination, order )));


//				QThingTypeMap qThingTypeMap = QThingTypeMap.thingTypeMap;
//				List<ThingType> data = ThingTypeMapService.getThingTypeMapDAO().getQuery().where(
//						QThingType.thingType.thingTypeFields..eq( thingType ) )
//				.list( qThingTypeMap.child );


//
//				BooleanBuilder beThingType = new BooleanBuilder();
//				beThingType = beThingType.and( QThingTypeField.thingTypeField.dataTypeThingTypeId ) );
//				List<ThingType> isChild = ThingTypeMapService.getThingTypeMapDAO().getQuery().where( QThingTypeField.child.eq( thingType ) )

			}

			list.add( publicMap );
		}
		Map<String,Object> mapResponse = new HashMap<String,Object>();
		mapResponse.put( "total", count );
		mapResponse.put( "results", list );
		return RestUtils.sendOkResponse( mapResponse );
	}*/

	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	// 1a. Limit access based on CLASS level resources
	@RequiresPermissions(value={"thingType:r:{id}"})
	@ApiOperation(position=2, value="Select a ThingType (AUTO)")
	public Response selectThingTypes( @PathParam("id") Long id, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project, @QueryParam("createRecent") @DefaultValue("false") Boolean createRecent )
	{
		ThingType thingType = ThingTypeService.getInstance().get( id );
		if( thingType == null )
		{
			return RestUtils.sendBadResponse( String.format( "ThingTypeId[%d] not found", id) );
		}
		List<ThingTypeMap> lstChildren = ThingTypeMapService.getInstance().getThingTypeMapByChildId(thingType.getId());
		List<ThingTypeMap> lstParent   = ThingTypeMapService.getInstance().getThingTypeMapByParentId(thingType.getId());
		thingType.setChildrenTypeMaps(convertSet(lstChildren));
		thingType.setChildrenTypeMaps(convertSet(lstParent));

		// 2. Limit visibility based on user's group and the object's group (group based authorization)
		EntityVisibility entityVisibility = getEntityVisibility();
		GeneralVisibilityUtils.limitVisibilitySelect(entityVisibility, thingType);
		validateSelect( thingType );
		// 5a. Implement extra
		Map<String,Object> publicMap = QueryUtils.mapWithExtraFields( thingType, extra, getExtraPropertyNames());
		addToPublicMap(thingType, publicMap, extra);

		//<Add publicMap childrenUdf
		if(extra!=null && extra.contains( "childrenUdf" ))
		{
			BooleanBuilder be = new BooleanBuilder();
			Group visibilityGroup = VisibilityUtils.getVisibilityGroup(ThingType.class.getCanonicalName(), null);
			be = be.and( GeneralVisibilityUtils.limitVisibilitySelectAll(entityVisibility, QThingType.thingType
					,  visibilityGroup, null, null ) );
			be = be.and( QueryUtils.buildSearch( QThingType.thingType, null ) );

			publicMap.put("childrenUdf",
					this.getChildrenThingTypeUdf( thingType.getId(),  ThingTypeService.getInstance().listPaginated( be, null, null)));
		}
		//Add publicMap childrenUdf>

		if (createRecent){
			RecentService.getInstance().insertRecent(thingType.getId(), thingType.getName(), "thingtype", thingType.getGroup());
		}

		// 5b. Implement only
		QueryUtils.filterOnly( publicMap, only, extra );
		QueryUtils.filterProjectionNested( publicMap, project, extend);
		return RestUtils.sendOkResponse( publicMap );
	}

	/**
	 * Convert List to Set
	 */
	public static Set<ThingTypeMap> convertSet(List<ThingTypeMap> lstData) {
		Set<ThingTypeMap> setData = null;
		if (lstData != null && !lstData.isEmpty()) {
			setData = new HashSet<>(lstData);
		}
		return setData;
	}

	/************************************************
	* Get children Udf based on a thingTypeId
	 * @thingParentId thingtypeId
	 * @lstThingType list of Thing Types
	* ***********************************************/
	public List<Map<String, Object>> getChildrenThingTypeUdf(Long thingParentId , List<ThingType> lstThingType)
	{
		List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
		if(lstThingType!=null && lstThingType.size()>0 )
		{
			for( ThingType thingType : lstThingType )
			{
            	 if(thingType.getThingTypeFields()!=null && thingType.getThingTypeFields().size()>0)
				 {
					 for(ThingTypeField thingTypeField: thingType.getThingTypeFields())
					 {
						 if(thingTypeField.getDataTypeThingTypeId()!=null
								 && thingTypeField.getDataTypeThingTypeId().compareTo(thingParentId)==0)
						 {
							 ThingType thingTypeParent = ThingTypeService.getInstance().get(thingTypeField.getDataTypeThingTypeId());
							 if(thingTypeParent.isIsParent())
							 {
								 Map<String,Object> publicMap = QueryUtils.mapWithExtraFields( thingType, null, getExtraPropertyNames());
								 addToPublicMap(thingType, publicMap, null);
								 result.add(publicMap);
								 break;
							 }
						 }
					 }
				 }
			}
		}
		return result;

	}

	/**
	 * LIST
	 */

	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	// 1a. Limit access based on CLASS level resources
	@RequiresPermissions(value={"thingType:r"})
	@ApiOperation(position=1, value="Get a List of ThingTypes (AUTO)")
	public Response listThingTypes(
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
		Pagination pagination = new Pagination( pageNumber, pageSize );

		BooleanBuilder be = new BooleanBuilder();
		// 2. Limit visibility based on user's group and the object's group (group based authorization)
		Group visibilityGroup = VisibilityUtils.getVisibilityGroup(ThingType.class.getCanonicalName(), visibilityGroupId);
		EntityVisibility entityVisibility = getEntityVisibility();
		be = be.and( GeneralVisibilityUtils.limitVisibilitySelectAll(entityVisibility, QThingType.thingType,  visibilityGroup, upVisibility, downVisibility ) );
		// 4. Implement filtering
		be = be.and( QueryUtils.buildSearch( QThingType.thingType, where ) );

		Long count = ThingTypeService.getInstance().countList( be );
		List<Map<String, Object>> list = new LinkedList<Map<String, Object>>();
		// 3. Implement pagination
		for( ThingType thingType : ThingTypeService.getInstance().listPaginated( be, pagination, order ) )
		{
			// Additional filter
			if (!includeInSelect(thingType))
			{
				continue;
			}
			// 5a. Implement extra
			String extraNew = extra;
			if ( (extra!=null) && (extra.contains("typeMultilevel")) ) {
				String[] data = extra.split(",");
				List<String> lstData = new ArrayList<>();
				for (int i = 0; i < data.length; i++) {
					if(!data[i].equals("typeMultilevel")) {
						lstData.add(data[i]);
					}
				}
				extraNew = StringUtils.join(lstData, ",");
			}
			Map<String,Object> publicMap = QueryUtils.mapWithExtraFields( thingType, extraNew, getExtraPropertyNames());
			addToPublicMap(thingType, publicMap, extra);
			// 5b. Implement only
			QueryUtils.filterOnly( publicMap, only, extraNew );
			QueryUtils.filterProjectionNested( publicMap, project, extend);

			publicMap.put("childrenUdf",
					this.getChildrenThingTypeUdf( thingType.getId(),  ThingTypeService.getInstance().listPaginated( be, pagination, order )));
			publicMap.put( "hasThingTypeUdf", ThingTypeService.getInstance().isWithThingTypeUdf( thingType ));
			list.add( publicMap );
		}
		Map<String,Object> mapResponse = new HashMap<String,Object>();
		mapResponse.put( "total", count );
		mapResponse.put( "results", list );
		return RestUtils.sendOkResponse( mapResponse );
	}

    @GET
    @Path("/reservedWords")
    @Produces(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresAuthentication
    @ApiOperation(position=1, value="Get a List of Reserved words in ThingType module")
    public Response listThingTypes(){
        return RestUtils.sendOkResponse(ThingTypeService.getInstance().getReservedWords());
    }
}
