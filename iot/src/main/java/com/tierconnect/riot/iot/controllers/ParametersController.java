package com.tierconnect.riot.iot.controllers;

import javax.annotation.Generated;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.LicenseDetail;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.services.LicenseService;
import com.tierconnect.riot.appcore.utils.EntityVisibility;
import com.tierconnect.riot.appcore.utils.QueryUtils;
import com.tierconnect.riot.appcore.utils.VisibilityUtils;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.iot.entities.Parameters;
import com.tierconnect.riot.iot.entities.QParameters;
import com.tierconnect.riot.iot.services.ParametersService;
import com.tierconnect.riot.sdk.dao.Pagination;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.subject.Subject;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Path("/parameters")
@Api("/parameters")
@Generated("com.tierconnect.riot.appgen.service.GenController")
public class ParametersController extends ParametersControllerBase
{
    private static final String CONDITION_TYPE_JS = "JS";

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresPermissions(value={"parameters:r"})
    @ApiOperation(position=1, value="Get a List of Parameterss (AUTO)")
    public Response listParameterss(@QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber, @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only, @QueryParam("visibilityGroupId") Long visibilityGroupId, @DefaultValue("") @QueryParam("upVisibility") String upVisibility, @DefaultValue("") @QueryParam("downVisibility") String downVisibility, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
    {
        Pagination pagination = new Pagination( pageNumber, pageSize );

        BooleanBuilder be = new BooleanBuilder();
        //TODO: how to handle case without a group property ?
        // 2. Limit visibility based on user's group and the object's group (group based authorization)
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(Parameters.class.getCanonicalName(), visibilityGroupId);
        EntityVisibility entityVisibility = getEntityVisibility();
        //be = be.and( GeneralVisibilityUtils.limitVisibilitySelectAll(entityVisibility, QParameters.parameters,  visibilityGroup, upVisibility, downVisibility ) );

        // 3. Implement filtering
        be = be.and( QueryUtils.buildSearch( QParameters.parameters, where ) );
        Long count = ParametersService.getInstance().countList( be );
        List<Map<String, Object>> list = new LinkedList<Map<String, Object>>();

        // 4. Implement pagination
        List<Parameters> parameters = ParametersService.getInstance().listPaginated(be, pagination, order);
        for( Parameters parameter : parameters)
        {
            // Additional filter
            if (!includeInSelect(parameter, visibilityGroup))
            {
                continue;
            }
            // 5a. Implement extra
            Map<String,Object> publicMap = QueryUtils.mapWithExtraFields( parameter, extra, getExtraPropertyNames());
            publicMap = QueryUtils.mapWithExtraFieldsNested(parameter, publicMap, extend, getExtraPropertyNames());
            addToPublicMap(parameter, publicMap, extra);
            // 5b. Implement only
            QueryUtils.filterOnly( publicMap, only, extra );
            QueryUtils.filterProjectionNested( publicMap, project, extend );
            list.add( publicMap );
        }
        addToResults(list, extend, project, visibilityGroupId, upVisibility, downVisibility);
        Map<String,Object> mapResponse = new HashMap<String,Object>();
        mapResponse.put( "total", count );
        mapResponse.put( "results", list );
        return RestUtils.sendOkResponse( mapResponse );
    }


    public boolean includeInSelect(Parameters parameter, Group group){
        boolean include = true;
        if(parameter.getCategory().equals(Constants.CONDITION_TYPE) && parameter.getCode().equals(Constants.CONDITION_TYPE_CEP)){
            Subject subject = SecurityUtils.getSubject();
            User currentUser = (User) subject.getPrincipal();
            LicenseDetail licenseDetail = LicenseService.getInstance().getLicenseDetail(group, true);
            if(licenseDetail != null){
                include = licenseDetail.hasFeature(licenseDetail.ESPER_RULES_PLUGIN);
            }
        }
        return include;
    }
}

