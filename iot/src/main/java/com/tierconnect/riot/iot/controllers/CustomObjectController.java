package com.tierconnect.riot.iot.controllers;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.utils.QueryUtils;
import com.tierconnect.riot.appcore.utils.VisibilityUtils;
import com.tierconnect.riot.iot.dao.mongo.ThingMongoDAO;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.CustomFieldValueService;
import com.tierconnect.riot.iot.services.CustomObjectService;

import com.tierconnect.riot.iot.services.ThingService;
import com.tierconnect.riot.sdk.dao.Pagination;
import com.tierconnect.riot.appcore.utils.QueryUtils;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

/**
 * Created by cfernandez
 * 10/22/2014.
 */

@Path("/customObject")
@Api("/customObject")
public class CustomObjectController extends CustomObjectControllerBase{

    @GET
    @Path("/fieldValues")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"customObject:r"})
    @ApiOperation(position=1, value="Get a List of CustomObjects with their customFieldValues")
    public Response listCustomObjects( @QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber, @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
    {
        Pagination pagination = new Pagination( pageNumber, pageSize );

        BooleanBuilder be = new BooleanBuilder();
        // 2. Limit visibility based on user's group and the object's group (group based authorization)
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(CustomObject.class.getCanonicalName(), null);
        //be = be.and( VisibilityUtils.limitVisibilityPredicate( visibilityGroup, QCustomObject.customObject.group,true, true ) );
        // 4. Implement filtering
        be = be.and( QueryUtils.buildSearch(QCustomObject.customObject, where) );

        Long count = CustomObjectService.getInstance().countList( be );
        List<Map<String, Object>> list = new LinkedList<Map<String, Object>>();
        // 3. Implement pagination
        for( CustomObject customObject : CustomObjectService.getInstance().listPaginated( be, pagination, order ) )
        {
            // 5a. Implement extra
            Map<String,Object> publicMap = QueryUtils.mapWithExtraFields( customObject, extra, getExtraPropertyNames() );
            addToPublicMap(customObject, publicMap, extra);

            Long customObjId = customObject.getId();
            List<CustomFieldValue> cfieldValues = CustomFieldValueService.getInstance().getCustomFieldValuesByCustomObjectId(customObjId,"customField.position:asc");
            List<CustomFieldValue> newCustomFieldValues = new LinkedList<CustomFieldValue>();
            for (CustomFieldValue customFieldValue0 : cfieldValues) {
                if(customFieldValue0.getCustomField().getCustomFieldType().getName().equals("LOOKUP")){
                    // cloning customFieldValue
                    CustomFieldValue currentCustomFieldValue = new CustomFieldValue();
                    currentCustomFieldValue.setId(customFieldValue0.getId());
                    currentCustomFieldValue.setValue(customFieldValue0.getValue());
                    currentCustomFieldValue.setCustomField(customFieldValue0.getCustomField());
                    currentCustomFieldValue.setCustomObject(customFieldValue0.getCustomObject());

                    String splitValues[] = currentCustomFieldValue.getValue().split(",");
                    Long [] lookupObjectIds = castIds(splitValues);
                    String lookupObjectValue = "";
                    Long lookupObjectId = getId(currentCustomFieldValue.getValue());
                    Long lookupObjectFieldId = currentCustomFieldValue.getCustomField().getLookupObjectField().getId();
                    if(customFieldValue0.getCustomField().getMultipleSelect()!=null && customFieldValue0.getCustomField().getMultipleSelect()){
                        lookupObjectValue = getLookupObjectValues(lookupObjectIds, lookupObjectFieldId);
                    }
                    else{
                        lookupObjectValue = getLookupObjectValue(lookupObjectId, lookupObjectFieldId);
                    }
                    currentCustomFieldValue.setValue(lookupObjectValue);
                    newCustomFieldValues.add(currentCustomFieldValue);
                }else{
                    if(customFieldValue0.getCustomField().getCustomFieldType().getName().equals("THING")){
                        CustomFieldValue customFieldValue = new CustomFieldValue();
                        customFieldValue.setId(customFieldValue0.getId());
                        customFieldValue.setValue(customFieldValue0.getValue());
                        customFieldValue.setCustomObject(customFieldValue0.getCustomObject());

                        CustomField customField = new CustomField();
                        customField.setId(customFieldValue0.getCustomField().getId());
                        customField.setCode(customFieldValue0.getCustomField().getCode());
                        customField.setName(customFieldValue0.getCustomField().getName());
                        customField.setRequired(customFieldValue0.getCustomField().getRequired());
                        customField.setCustomObjectType(customFieldValue0.getCustomField().getCustomObjectType());
                        customFieldValue.setCustomField(customField);

                        // recovering thing relationship
                        Long thingId = getId(customFieldValue.getValue());
                        Thing thing = ThingService.getInstance().get(thingId);
                        String thingTypeFieldName = customFieldValue0.getCustomField().getThingTypeField().getName();

                        String value = "";
                        if(thing != null) {
                            value = getThingFieldValue(thing, thingTypeFieldName);
                        }
                        customFieldValue.setValue(value);
                        newCustomFieldValues.add(customFieldValue);
                    }else {
                        newCustomFieldValues.add(customFieldValue0);
                    }
                }
            }
            publicMap.put("fieldValues", newCustomFieldValues);

            // 5b. Implement only
            QueryUtils.filterOnly( publicMap, only, extra );
            QueryUtils.filterOnly( publicMap, project, extend );
            list.add( publicMap );
        }
        Map<String,Object> mapResponse = new HashMap<String,Object>();
        mapResponse.put( "total", count );
        mapResponse.put( "results", list );
        return RestUtils.sendOkResponse(mapResponse);
    }

    public String getLookupObjectValue(Long lookupObjectId, Long lookupObjectFieldId){
        String lookupObjectValue = "";
        List<CustomFieldValue> customFieldValuesLookupObject = CustomFieldValueService.getInstance().getCustomFieldValuesByCustomObjectId(lookupObjectId);
        for (CustomFieldValue customFieldValue : customFieldValuesLookupObject) {
            if (customFieldValue.getCustomField().getId().equals(lookupObjectFieldId)){
                lookupObjectValue = customFieldValue.getValue();
                break;
            }
        }
        return lookupObjectValue;
    }

    public String getLookupObjectValues(Long [] lookupObjectIds, Long lookupObjectFieldId){
        String lookupObjectValue = "";
        for(int x=0;x<lookupObjectIds.length;x++){
            if(lookupObjectIds[x]==null){
                break;
            }
            List<CustomFieldValue> customFieldValuesLookupObject = CustomFieldValueService.getInstance().getCustomFieldValuesByCustomObjectId(lookupObjectIds[x]);
            for (CustomFieldValue customFieldValue : customFieldValuesLookupObject) {
                if (customFieldValue.getCustomField().getId().equals(lookupObjectFieldId)){
                    lookupObjectValue = lookupObjectIds[x+1]==null ? lookupObjectValue+customFieldValue.getValue():lookupObjectValue+customFieldValue.getValue()+" , ";
                    break;
                }
            }
        }
        return lookupObjectValue;
    }


    public String getThingFieldValue(Thing thing, String thingTypeFieldName)
    {
        String value = "";
        String key = thingTypeFieldName + ".value";
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

    @GET
    @Path("/{id}/fieldvalues")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"customObject:r:{id}"})
    @ApiOperation(position=2, value="Select a CustomObject with its customFields and customField values")
    public Response selectCustomObjectsWithFieldValues( @PathParam("id") Long id, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
    {
        CustomObject customObject = CustomObjectService.getInstance().get( id );
        if( customObject == null )
        {
            return RestUtils.sendBadResponse( String.format( "CustomObjectId[%d] not found", id) );
        }

        validateSelect( customObject );
        // 5a. Implement extra
        Map<String,Object> publicMap = QueryUtils.mapWithExtraFields( customObject, extra, getExtraPropertyNames() );
        addToPublicMap(customObject, publicMap, extra);

        List<CustomFieldValue> fieldValues = CustomFieldValueService.getInstance().getCustomFieldValuesByCustomObjectId(id,"customField.position:asc");
        List<Map<String, Object>> fieldsDecorated = new LinkedList<>();
        for (CustomFieldValue customFieldValue0 : fieldValues) {

            Map<String,Object> customFieldObject = new HashMap<>();

            customFieldObject.put("id", customFieldValue0.getCustomField().getId());
            customFieldObject.put("code", customFieldValue0.getCustomField().getCode());
            customFieldObject.put("name", customFieldValue0.getCustomField().getName());
            customFieldObject.put("value", customFieldValue0.getValue());
            customFieldObject.put("fieldValueId", customFieldValue0.getId());
            customFieldObject.put("customFieldType", customFieldValue0.getCustomField().getCustomFieldType());
            customFieldObject.put("customObjectType", customFieldValue0.getCustomField().getCustomObjectType());
            customFieldObject.put("customField", customFieldValue0.getCustomField());

            CustomField customField = customFieldValue0.getCustomField();
            if (customField.getCustomFieldType().getName().equals("LOOKUP")){
                // if the field is of type lookup, the value is a customObject
                if(customField.getMultipleSelect()!=null && customField.getMultipleSelect()){
                    customFieldObject.put("multipleLookupInstances", getlookupInstancesSelected(customField, customFieldValue0));
                }
                else{
                    customFieldObject.put("lookupInstanceSelected", getlookupObjectSelected(customField, customFieldValue0));
                }
                // Adding lookup object information
                Long lookUpObjectId = customField.getLookupObject().getId();
                Long lookupObjFieldId = customField.getLookupObjectField().getId();
                List<CustomObject> objects = CustomObjectService.getInstance().getCustomObjectsByCustomObjectTypeId(lookUpObjectId);
                List<Map<String, Object>> lookupInstances = new LinkedList<Map<String, Object>>();
                for (CustomObject object : objects) {
                    Long customObjId = object.getId();
                    Map<String,Object> objectWithFieldValue = getLookupInstanceFieldValue(customObjId, lookupObjFieldId);
                    lookupInstances.add(objectWithFieldValue);
                }
                customFieldObject.put("lookupObject",customField.getLookupObject());
                customFieldObject.put("lookupObjectField",customField.getLookupObjectField());
                customFieldObject.put("lookupInstances", lookupInstances);
            }

            if (customField.getCustomFieldType().getName().equals("THING")){
                // adding current thing selected
                Long thingSelectedId = getId(customFieldValue0.getValue());
                Thing thingSelected = ThingService.getInstance().get(thingSelectedId);
                if(thingSelected != null){
                    Map<String,Object> thingSelectedObject = new HashMap<>();
                    thingSelectedObject.put("id", thingSelected.getId());
                    thingSelectedObject.put("name", thingSelected.getName());
                    ThingTypeField thingTypeField0 = customField.getThingTypeField();
                    String value0 = getThingFieldValue(thingSelected, thingTypeField0.getName());
                    thingSelectedObject.put("fieldValue", value0);
                    customFieldObject.put("thingSelected", thingSelectedObject);
                }else{
                    customFieldObject.put("thingSelected", "");
                }


                // Adding things and thing value
                Long thingTypeId = customField.getThingType().getId();
                List<Thing> things = ThingService.getInstance().selectByThingType(thingTypeId);
                List<Map<String, Object>> thingsDecorated = new LinkedList<Map<String, Object>>();
                for (Thing thing : things) {
                    Map<String,Object> thingObject = new HashMap<>();
                    thingObject.put("id", thing.getId());
                    thingObject.put("name", thing.getName());

                    ThingTypeField thingTypeField = customField.getThingTypeField();
                    String value = getThingFieldValue(thing, thingTypeField.getName());
                    thingObject.put("fieldValue", value);
                    thingsDecorated.add(thingObject);
                }
                customFieldObject.put("things", thingsDecorated);

                ThingType thingType = new ThingType();
                thingType.setId(customField.getThingType().getId());
                thingType.setName(customField.getThingType().getName());
                customFieldObject.put("thingType", thingType);

                ThingTypeField thingTypeField = new ThingTypeField();
                thingTypeField.setId(customField.getThingTypeField().getId());
                thingTypeField.setName(customField.getThingTypeField().getName());
                customFieldObject.put("thingTypeField", thingTypeField);

                // adding custom Field, to add directly customField is not possible because the dependencies of thingTypeField
                CustomField customField1 = new CustomField();
                customField1.setId(customField.getId());
                customField1.setCode(customField.getCode());
                customField1.setName(customField.getName());
                customField1.setRequired(customField.getRequired());
                customField1.setCustomFieldType(customField.getCustomFieldType());
                customFieldObject.put("customField", customField1);
            }

            fieldsDecorated.add(customFieldObject);
        }
        publicMap.put("fields", fieldsDecorated);

        // 5b. Implement only
        QueryUtils.filterOnly( publicMap, only, extra );
        QueryUtils.filterProjectionNested( publicMap, project, extend );
        return RestUtils.sendOkResponse( publicMap );
    }

    public Map<String,Object> getLookupInstanceFieldValue(Long customObjId, Long lookupObjFieldId){
        List<CustomFieldValue> fieldValues = CustomFieldValueService.getInstance().getCustomFieldValuesByCustomObjectId(customObjId);
        Map<String,Object> robject = new HashMap<>();
        robject.put("objectId", customObjId);
        for (CustomFieldValue customFieldValue : fieldValues) {
            if (customFieldValue.getCustomField().getId().equals(lookupObjFieldId)){
                robject.put("fieldValueId", customFieldValue.getId());
                robject.put("displayName", customFieldValue.getValue());
            }
        }
        return robject;
    }


    public Long getId(String value){
        Long newVal;
        try{
            newVal = Long.parseLong(value);
        }catch (NumberFormatException e){
            newVal = 0L;
        }catch (Exception e){
            newVal = 0L;
        }
        return newVal;
    }

    public  Map<String,Object> getlookupObjectSelected(CustomField customField, CustomFieldValue customFieldValue0){
        Long customObjectId = getId(customFieldValue0.getValue());
        List<CustomFieldValue> customFieldValues0 = CustomFieldValueService.getInstance().getCustomFieldValuesByCustomObjectId(customObjectId);

        Map<String,Object> customObjectSelected = new HashMap<String,Object>();
        customObjectSelected.put("objectId", customObjectId);
        for (CustomFieldValue customFieldValue : customFieldValues0) {
            if (customField.getLookupObjectField().getId().equals(customFieldValue.getCustomField().getId())){
                customObjectSelected.put("fieldValueId", customFieldValue.getId());
                customObjectSelected.put("displayName", customFieldValue.getValue());
            }
        }
        return customObjectSelected;
    }

    public  List<Map<String, Object>> getlookupInstancesSelected(CustomField customField, CustomFieldValue customFieldValue0){
        List<Map<String, Object>> lookupInstancesSelected = new LinkedList<Map<String, Object>>();
        String value = customFieldValue0.getValue();
        String splitValues[] = value.split(",");
        Long lookupInstancesIds [] = castIds(splitValues);
        for (Long lookupInstanceId : lookupInstancesIds) {
            if(lookupInstanceId==null){
                break;
            }
            List<CustomFieldValue> customFieldValues0 = CustomFieldValueService.getInstance().getCustomFieldValuesByCustomObjectId(lookupInstanceId);
            Map<String,Object> object = new HashMap<>();
            object.put("objectId", lookupInstanceId);
            for (CustomFieldValue customFieldValue : customFieldValues0) {
                if (customField.getLookupObjectField().getId().equals(customFieldValue.getCustomField().getId())){
                    object.put("fieldValueId", customFieldValue.getId());
                    object.put("displayName", customFieldValue.getValue());
                }
            }
            Map<String,Object> objectWithFieldValue = object;
            lookupInstancesSelected.add(objectWithFieldValue);
        }

        return lookupInstancesSelected;
    }

    private Long[] castIds(String[] splitValues) {
        Long [] ids = new Long[20];
        int i = 0;
        for (String splitValue : splitValues) {
            ids[i] = getId(splitValue);
            i++;
        }
        return ids;
    }

    /**
     * FILTER LIST
     */
    @GET
    @Path("/customApplication/")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"customObject:r"})
    @ApiOperation(position=1, value="Get a List of Custom Objects")
    public Response listEdgeBoxes(@QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber, @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only, @QueryParam("visibilityGroupId") Long visibilityGroupId, @DefaultValue("") @QueryParam("upVisibility") String upVisibility, @DefaultValue("") @QueryParam("downVisibility") String downVisibility , @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
    {
        CustomApplicationController c = new CustomApplicationController();
        return c.listCustomApplications(pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility, downVisibility, false, extend, project);
    }
}
