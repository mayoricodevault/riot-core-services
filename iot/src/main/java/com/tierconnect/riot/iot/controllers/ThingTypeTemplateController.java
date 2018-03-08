package com.tierconnect.riot.iot.controllers;

import com.tierconnect.riot.appcore.utils.EntityVisibility;
import com.tierconnect.riot.appcore.utils.GeneralVisibilityUtils;
import com.tierconnect.riot.appcore.utils.QueryUtils;
import com.tierconnect.riot.iot.entities.DataType;
import com.tierconnect.riot.iot.entities.ThingTypeField;
import com.tierconnect.riot.iot.entities.ThingTypeFieldTemplate;
import com.tierconnect.riot.iot.entities.ThingTypeTemplate;
import com.tierconnect.riot.iot.services.ThingTypeFieldTemplateService;
import com.tierconnect.riot.iot.services.ThingTypeService;
import com.tierconnect.riot.iot.services.ThingTypeTemplateCategoryService;
import com.tierconnect.riot.iot.services.ThingTypeTemplateService;
import com.tierconnect.riot.sdk.utils.PermissionsUtils;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Path("/thingTypeTemplate")
@Api("/thingTypeTemplate")
public class ThingTypeTemplateController extends ThingTypeTemplateControllerBase
{
    private static Logger logger = Logger.getLogger( ThingTypeTemplateController.class );
    @Context
    ServletContext context;

    @GET
    @Path("/listSummaryThingTypetemplates/")
    @Produces(MediaType.APPLICATION_JSON)
    //@RequiresPermissions(value = { "thingTypeTemplate:r" })
    @ApiOperation("List a summary of thing type templates")
    public Response listSummaryThingTypetemplates(
            @QueryParam("visibilityGroupId") Long visibilityGroupId
            , @DefaultValue("") @QueryParam("upVisibility") String upVisibility
            , @DefaultValue("") @QueryParam("downVisibility") String downVisibility
            , @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend
            , @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project
            , @DefaultValue("false") @QueryParam("byCategory") boolean byCategory) {
        ThingTypeTemplateController c = new ThingTypeTemplateController();

        int pageSize = 1000;
        int pageNumber = 1;
        String order = "name:asc";
        String where = null;
        String extra = "";
        String only = "id, name, pathIcon ";
        // Force 'custom thing type template' if user has not permissions
        if (!(PermissionsUtils.isPermitted(SecurityUtils.getSubject(), "thingTypeTemplate:r"))) {
            where = "id=1";
        }
        if (byCategory) {
            List<Map<String, Object>> listCategoryWithTemplates = ThingTypeTemplateCategoryService.getInstance().listCategoryWithTemplates();
            Map<String, Object> mapResponse = new HashMap<>(2);
            mapResponse.put("total", listCategoryWithTemplates.size());
            mapResponse.put("results", listCategoryWithTemplates);
            return RestUtils.sendOkResponse(mapResponse);
        }
        return c.listThingTypeTemplates(pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility, downVisibility, extend, project);
    }

    /* Get thing type template with the thing type field template */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    //@RequiresPermissions(value={"thingTypeTemplate:r:{id}"})
    @ApiOperation(position=2, value="Select a ThingTypeTemplate (AUTO)")
    public Response selectThingTypeTemplates( @PathParam("id") Long id, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
    {
//		EntityDescription entityDescription = null;
//		String typeParentDescription = null;
//		String typeDescription = null;
//		String codeType = null;
//
//		EntityDescriptionController ed = new EntityDescriptionController();
        ThingTypeTemplate thingTypeTemplate = ThingTypeTemplateService.getInstance().get( id );
        if( thingTypeTemplate == null )
        {
            return RestUtils.sendBadResponse( String.format( "ThingTypeTemplateId[%d] not found", id) );
        }
        // 2. Limit visibility based on user's group and the object's group (group based authorization)
        EntityVisibility entityVisibility = getEntityVisibility();
        GeneralVisibilityUtils.limitVisibilitySelect(entityVisibility, thingTypeTemplate);
        validateSelect( thingTypeTemplate );
        // 5a. Implement extra
        Map<String,Object> publicMap = QueryUtils.mapWithExtraFields( thingTypeTemplate, extra, getExtraPropertyNames());
        LinkedList extraMap = (LinkedList)publicMap.get("ThingTypeFieldTemplate");
        //Obtain description of the entity description with the values of type code and default type code
        if(extraMap!=null && extraMap.size()>0)
        {
            DataTypeController dt =  new DataTypeController();
            for(Object data : extraMap)
            {
                HashMap<String, Object> dataw = (HashMap<String, Object>) data;
                ThingTypeFieldTemplate thingTypeFieldTemplate = ThingTypeFieldTemplateService.getInstance().get( Long.parseLong( dataw.get( "id" ).toString() ) );
                dataw.put( "dataType", thingTypeFieldTemplate.getType().publicMap() );
                DataType dataType = thingTypeFieldTemplate.getType();
                dataw.put("defaultValue", thingTypeFieldTemplate.getDefaultValue());
                dataw.put("timeSeries", thingTypeFieldTemplate.isTimeSeries());
                dataw.put("typeParentCode",dataType.getTypeParent());
                dataw.put("typeParentDescription", dt.getEntityDescriptionByEntityCode("THING_TYPE_PROPERTY", dataType.getTypeParent() ).getValue() );
                dataw.put("typeCode", dataType.getCode());
                dataw.put("typeDescription", dataType.getId().compareTo( ThingTypeField.Type.TYPE_THING_TYPE.value )==0?
                        ThingTypeService.getInstance().get( thingTypeFieldTemplate.getDataTypeThingTypeId() ).getName():dataType.getValue());
                dataw.put("typeCode.type", dataType.getType());
                dataw.put("dataTypeThingTypeId", thingTypeFieldTemplate.getDataTypeThingTypeId());
                dataw.put("type",dataType.getId());

//                if(dataw.get( "typeParent" ).equals( "NATIVE_THING_TYPE" ))
//                {
//                    ThingType dataType = ThingTypeService.getInstance().get((Long)dataw.get("dataType"));
//                    dataw.put("typeParentCode",dataw.get( "typeParent" ));
//                    dataw.put("typeParentDescription", dt.getEntityDescriptionByEntityCode("THING_TYPE_PROPERTY", (String) dataw.get( "typeParent" ) ).getValue() );
//                    dataw.put("typeCode", dataType.getThingTypeCode());
//                    dataw.put("typeDescription", dataType.getName() );
//                    dataw.put("typeCode.type", "");
//                }else
//                {
//                    DataType dataType = DataTypeService.getInstance().get((Long)dataw.get("dataType"));
//                    dataw.put("typeParentCode", dataType.getTypeParent());
//                    dataw.put("typeParentDescription", dt.getEntityDescriptionByEntityCode("THING_TYPE_PROPERTY", dataType.getTypeParent()).getValue());
//                    dataw.put("typeCode",dataType.getCode());
//                    dataw.put("typeDescription", dt.getEntityDescriptionByEntityCode(dataType.getTypeParent(), dataType.getCode()).getValue());
//                    dataw.put("typeCode.type", dataType.getType());
//                }

            }
        }

        addToPublicMap(thingTypeTemplate, publicMap, extra);
        // 5b. Implement only
        QueryUtils.filterOnly( publicMap, only, extra );
        QueryUtils.filterProjectionNested( publicMap, project, extend);
        return RestUtils.sendOkResponse( publicMap );
    }
}