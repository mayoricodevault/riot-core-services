package com.tierconnect.riot.iot.controllers;

import com.tierconnect.riot.appcore.utils.VersionUtils;
import com.tierconnect.riot.appcore.version.CodeVersion;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

import javax.annotation.security.PermitAll;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by julio.rocha on 18-11-16.
 */
@Path("/version")
@Api("/version")
public class VersionController {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    @ApiOperation(position = 1, value = "Gets the current version of the system")
    public Response systemVersion(){
        Map<String,Object> info = new HashMap<String,Object>();
        int versionNumber = CodeVersion.getInstance().getCodeVersion();
        String version = VersionUtils.getAppVersionString(versionNumber);
        info.put("versionNumber", versionNumber);
        info.put("version", version);
        info.put("versionName", CodeVersion.getInstance().getCodeVersionName());
        return RestUtils.sendOkResponse(info);
    }
}
