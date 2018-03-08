package com.tierconnect.riot.iot.controllers;

import java.util.*;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.mongodb.DBObject;
import com.tierconnect.riot.iot.dao.mongo.ThingMongoDAO;
import com.wordnik.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.utils.QueryUtils;
import com.tierconnect.riot.appcore.utils.VisibilityUtils;
import com.tierconnect.riot.iot.entities.CustomField;
import com.tierconnect.riot.iot.entities.CustomFieldValue;
import com.tierconnect.riot.iot.entities.CustomObject;
import com.tierconnect.riot.iot.entities.QCustomField;
import com.tierconnect.riot.iot.entities.Thing;
import com.tierconnect.riot.iot.entities.ThingTypeField;
import com.tierconnect.riot.iot.services.CustomFieldService;
import com.tierconnect.riot.iot.services.CustomFieldValueService;
import com.tierconnect.riot.iot.services.CustomObjectService;
//import com.tierconnect.riot.iot.services.FieldValueService;
import com.tierconnect.riot.iot.services.ThingService;
import com.tierconnect.riot.iot.services.ThingTypeFieldService;
import com.tierconnect.riot.sdk.dao.Pagination;
import com.tierconnect.riot.sdk.utils.BeanUtils;
import com.tierconnect.riot.appcore.utils.QueryUtils;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.jaxrs.PATCH;

/**
 * Created by cfernandez
 * 10/22/2014.
 */

@Path("/customField")
@Api("/customField")
public class CustomFieldController extends CustomFieldControllerBase{

    @GET
    @Path("/createInstance")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"customField:r"})
    @ApiOperation(position=1, value="Get a List of CustomFields with the lookup instances")
    public Response listCustomFieldsWithLookup(@QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber, @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only, @QueryParam("visibilityGroupId") Long visibilityGroupId, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
    {
        Pagination pagination = new Pagination( pageNumber, pageSize );

        BooleanBuilder be = new BooleanBuilder();
        // 2. Limit visibility based on user's group and the object's group (group based authorization)
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(CustomField.class.getCanonicalName(), null);
        //be = be.and( VisibilityUtils.limitVisibilityPredicate( visibilityGroup, QCustomField.customField.group,true, true ) );
        // 4. Implement filtering
        be = be.and( QueryUtils.buildSearch( QCustomField.customField, where ) );

        Long count = CustomFieldService.getInstance().countList( be );
        List<Map<String, Object>> list = new LinkedList<Map<String, Object>>();

        // 3. Implement pagination
        for( CustomField customField : CustomFieldService.getInstance().listPaginated( be, pagination, order ) )
        {
            // 5a. Implement extra
            Map<String,Object> publicMap = QueryUtils.mapWithExtraFields( customField, extra, getExtraPropertyNames() );
            addToPublicMap(customField, publicMap, extra);

            if (customField.getCustomFieldType().getName().equals("LOOKUP")){
                // Adding lookup object instances with the specified lookupField
                Long lookUpObjectId = customField.getLookupObject().getId();
                Long lookupObjFieldId = customField.getLookupObjectField().getId();
                List<Map<String, Object>> lookupInstances = getLookupInstances(lookUpObjectId, lookupObjFieldId);
                publicMap.put("lookupObject",customField.getLookupObject());
                publicMap.put("lookupInstances", lookupInstances);
            }
            if (customField.getCustomFieldType().getName().equals("THING")){
                // Adding things and thing value
                Long thingTypeId = customField.getThingType().getId();
                Long thingTypeFieldId = customField.getThingTypeField().getId();
                List<Map<String, Object>> thingsDecorated = getThingsDecorated(thingTypeId, thingTypeFieldId);
                publicMap.put("things", thingsDecorated);
            }

            // 5b. Implement only
            QueryUtils.filterOnly( publicMap, only, extra );
            QueryUtils.filterProjectionNested( publicMap, project, extend );
            list.add( publicMap );
        }
        Map<String,Object> mapResponse = new HashMap<String,Object>();
        mapResponse.put( "total", count );
        mapResponse.put( "results", list );
        return RestUtils.sendOkResponse( mapResponse );
    }

    public List<Map<String, Object>> getLookupInstances(Long lookUpObjectId, Long lookupObjFieldId){
        List<Map<String, Object>> lookupInstances = new LinkedList<>();
        List<CustomObject> objects = CustomObjectService.getInstance().getCustomObjectsByCustomObjectTypeId(lookUpObjectId);
        for (CustomObject object : objects) {
            Map<String,Object> objectWithFieldValue = getLookupInstanceFieldValue(object.getId(), lookupObjFieldId);
            lookupInstances.add(objectWithFieldValue);
        }
        return lookupInstances;
    }

    public List<Map<String, Object>> getThingsDecorated(Long thingTypeId, Long thingTypeFieldId){
        List<Map<String, Object>> thingsDecorated = new LinkedList<>();
        List<Thing> things = ThingService.getInstance().selectByThingType(thingTypeId);
        for (Thing thing : things) {
            Map<String,Object> thingObject = new HashMap<>();
            thingObject.put("id", thing.getId());
            thingObject.put("name", thing.getName());
            thingObject.put("name", thing.getName());

            ThingTypeField thingTypeField = ThingTypeFieldService.getInstance().get(thingTypeFieldId);
            String value = getThingFieldValue(thing, thingTypeField);
            thingObject.put("fieldValue", value);
            thingsDecorated.add(thingObject);
        }
        return thingsDecorated;
    }

    public Map<String,Object> getLookupInstanceFieldValue(Long customObjId, Long lookupObjFieldId){
        Map<String,Object> lookupInstance = new HashMap<>();
        lookupInstance.put("objectId", customObjId);
        List<CustomFieldValue> fieldValues = CustomFieldValueService.getInstance().getCustomFieldValuesByCustomObjectId(customObjId);
        for (CustomFieldValue fieldValue : fieldValues) {
            if (fieldValue.getCustomField().getId().equals(lookupObjFieldId)){
                lookupInstance.put("displayName", fieldValue.getValue());
                break;
            }
        }
        return lookupInstance;
    }

    public String getThingFieldValue(Thing thing, ThingTypeField thingTypeField)
    {
        String value = "";
        String key = thingTypeField.getName() + ".value";
        String where = "_id=" + thing.getId();

        List<String> fields = new ArrayList<>();
        fields.add(key);

        Map<String,Object> fieldValues = ThingMongoDAO.getInstance().getThingUdfValues(where, null, fields, null);

        if (fieldValues.get("results") != null) {
            List<Map<String, Object>> udfValuesList = (List<Map<String, Object>>) fieldValues.get("results");

            if (udfValuesList != null && udfValuesList.size() > 0) {
                value = String.valueOf(udfValuesList.get(0).get(key));
            }
        }

        return value;
    }

    @PUT
    @Path("/fieldValue/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"customField:i"})
    @ApiOperation(position=3, value="Insert a CustomField with its customFieldValues")
    public Response insertCustomFieldWithFieldValues( Map<String, Object> map )
    {
        // 1[b,c]. Restrict access based on OBJECT and PROPERTY level write permissions
        //QueryUtils.filterWritePermissions( CustomField.class, map );
        CustomField customField = new CustomField();
        // 7. handle insert and update
        BeanUtils.setProperties(map, customField);

        // 6. handle validation in an Extensible manner
        if(!uniqueCodeCreate(customField)){
            return RestUtils.sendBadResponse( String.format("MakerCodeExists"));
        }
        CustomFieldService.getInstance().insert( customField );

        // Creating customFieldValues if exist instances of the object type
        Long customObjTypeId = customField.getCustomObjectType().getId();
        List<CustomObject> objects = CustomObjectService.getInstance().getCustomObjectsByCustomObjectTypeId(customObjTypeId);
        for (CustomObject object : objects) {
            CustomFieldValue customFieldValue = new CustomFieldValue();
            customFieldValue.setCustomObject(object);
            customFieldValue.setCustomField(customField);
            customFieldValue.setValue("");
            CustomFieldValueService.getInstance().insert( customFieldValue );
        }

        Map<String,Object> publicMap = customField.publicMap();
        return RestUtils.sendCreatedResponse( publicMap );
    }

    public boolean uniqueCodeCreate( CustomField customField )
    {
        CustomField customField1 = CustomFieldService.getInstance().selectByCodeAndBusinessObj(customField.getCode(),customField.getCustomObjectType().getId());
        return customField1==null;
    }



    @PATCH
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresPermissions(value={"customField:u:{id}"})
    @ApiOperation(position=4, value="Update a CustomField (AUTO)")
    public Response updateCustomField( @PathParam("id") Long id, Map<String, Object> map )
    {
        CustomField customField = CustomFieldService.getInstance().get( id );
        if( customField == null )
        {
            return RestUtils.sendBadResponse( String.format( "CustomFieldId[%d] not found", id) );
        }
        //2. TODO: Limit visibility based on user's group and the object's group (group based authorization)
        // 7. handle insert and update
        BeanUtils.setProperties( map, customField );
        // 6. handle validation in an Extensible manner
        if(!uniqueCodeEdit( customField , id )){
            return RestUtils.sendBadResponse( String.format("MakerCodeExists"));
        }
        Map<String,Object> publicMap = customField.publicMap();
        return RestUtils.sendOkResponse( publicMap );
    }

    public boolean uniqueCodeEdit( CustomField customField , Long id )
    {
        try{
            CustomField customField1 = CustomFieldService.getInstance().selectByCodeAndBusinessObj(customField.getCode(),customField.getCustomObjectType().getId());
            if(customField1==null) {
                return true;
            }
            else{
                return Long.parseLong(customField1.getId().toString())==id;
            }
        }
        catch(Exception err){
            return false;
        }
    }


}
