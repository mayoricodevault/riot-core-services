package com.tierconnect.riot.appcore.controllers;

import javax.annotation.Generated;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import javax.ws.rs.core.Response;

import com.tierconnect.riot.appcore.services.GroupResourcesService;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.utils.RestUtils;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.jaxrs.PATCH;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import java.util.Map;

@Path("/groupResources")
@Api("/groupResources")
@Generated("com.tierconnect.riot.appgen.service.GenController")
public class GroupResourcesController extends GroupResourcesControllerBase 
{

    @PATCH
    @Path("/uploadGroupImage/{groupCode}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RequiresAuthentication
    @ApiOperation(position=1, value="Upload File")
    public Response createAttachmentMultipart(
            @PathParam("groupCode") String groupCode,
            @QueryParam("nameTemplate") String nameTemplate,
            @QueryParam("setLastImage") Boolean setLastImage,
            MultipartFormDataInput input) throws Exception {
        Map<String, Object> result = null;

        if (groupCode.isEmpty()) {
            throw new UserException("Group Code is required.");
        }
        result = GroupResourcesService.getInstance().uploadImageFile(groupCode, nameTemplate, setLastImage != null, input);


        return RestUtils.sendOkResponse(result);
    }
}

