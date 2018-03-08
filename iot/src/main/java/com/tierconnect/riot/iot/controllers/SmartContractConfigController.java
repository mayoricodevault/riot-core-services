package com.tierconnect.riot.iot.controllers;

import javax.annotation.Generated;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.tierconnect.riot.appcore.utils.QueryUtils;
import com.tierconnect.riot.iot.entities.SmartContractConfig;
import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.services.SmartContractConfigService;
import com.tierconnect.riot.iot.services.ThingTypeMapService;
import com.tierconnect.riot.iot.services.ThingTypeService;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import com.tierconnect.riot.sdk.utils.BeanUtils;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


@Path("/smartContractConfig")
@Api("/smartContractConfig")
@Generated("com.tierconnect.riot.appgen.service.GenController")
public class SmartContractConfigController extends SmartContractConfigControllerBase {

    @PUT
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresPermissions(value={"smartContractConfig:i"})
    @ApiOperation(position=3, value="Insert a SmartContractConfig")
    public Response insertSmartContractConfig(Map<String, Object> map )
    {
        String thingTypeCode = (String) map.get("thingTypeCode");

        if(thingTypeCode != null) {
            try {
                map.put("fields", getDataTypesForFields(thingTypeCode));
            } catch (NonUniqueResultException e) {
                e.printStackTrace();
            }
        }

        //2. TODO: Limit visibility based on user's group and the object's group (group based authorization)
        SmartContractConfig smartContractConfig = new SmartContractConfig();
        // 7. handle insert and update
        BeanUtils.setProperties( map, smartContractConfig );
        // 6. handle validation in an Extensible manner
        validateInsert( smartContractConfig );
        SmartContractConfigService.getInstance().insert( smartContractConfig );
        // 1[b,c]. Restrict access based on OBJECT and PROPERTY level read permissions
        Map<String,Object> publicMap = smartContractConfig.publicMap();
        return RestUtils.sendCreatedResponse( publicMap );
    }

    public void validateInsert( SmartContractConfig smartContractConfig )
    {

    }

    private Map<String, String> getDataTypesForFields(final String thingTypeCode) throws NonUniqueResultException {
        ThingTypeController thingTypeController = new ThingTypeController();

        ThingType thingType = ThingTypeService.getInstance().getByCode(thingTypeCode);

        List lstChildren = ThingTypeMapService.getInstance().getThingTypeMapByChildId(thingType.getId());
        List lstParent   = ThingTypeMapService.getInstance().getThingTypeMapByParentId(thingType.getId());

        thingType.setChildrenTypeMaps(ThingTypeController.convertSet(lstChildren));
        thingType.setChildrenTypeMaps(ThingTypeController.convertSet(lstParent));

        Map publicMap = QueryUtils.mapWithExtraFields( thingType, null, null);

        thingTypeController.addToPublicMap(thingType, publicMap, null);
        QueryUtils.filterOnly( publicMap, "fields", null );
        QueryUtils.filterProjectionNested( publicMap, null, null);

        Map<String, String> fieldsMap = new HashMap<>();
        if(publicMap.get("fields") != null) {
            LinkedList<Map<String, Object>> fieldList = (LinkedList<Map<String, Object>>) publicMap.get("fields");
            fieldList.forEach((x -> {
                fieldsMap.put((String) x.get("name"), (String) x.get("typeCode"));
            }));
        }

        return fieldsMap;
    }
}


