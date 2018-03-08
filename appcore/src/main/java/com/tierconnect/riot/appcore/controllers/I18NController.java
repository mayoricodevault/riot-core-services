package com.tierconnect.riot.appcore.controllers;

import com.tierconnect.riot.appcore.services.ConfigurationService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.I18NService;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authz.annotation.RequiresGuest;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by agutierrez on 4/17/15.
 */
@Path("/i18N")
@Api("/i18N")
public class I18NController {

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresGuest
    @ApiOperation(position=1, value="Get a List of all messages")
    public Response listAllMessages(@QueryParam("module") String module, @QueryParam("locale") String locale)
    {
        I18NService service = new I18NService();
        Locale locale1 = service.getLocale(locale);
        String resourceDir = ConfigurationService.getAsString(GroupService.getInstance().getRootGroup(), "i18NDirectory");
        Map result = service.getAllStrings(module, locale1, resourceDir);
        return RestUtils.sendOkResponse(result);
    }

    @GET
    @Path("/{key}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresGuest
    @ApiOperation(position=2, value="Get a message")
    public Response getMessage(@QueryParam("module") String module, @QueryParam("locale") String locale, @PathParam("key") String key)
    {
        // Next code was refactored in 2017-02-14, delete it if there are not bugs reported related to translate functions
//        I18NService service = new I18NService();
//        Locale locale1 = service.getLocale(locale);
//        Map result = new HashMap();
//        String resourceDir = ConfigurationService.getAsString(GroupService.getInstance().getRootGroup(), "i18NDirectory");
//        result.put(key, service.getString(module, key, locale1, resourceDir));
        Map<String, Object> result = new HashMap<>();
        result.put(key, (new I18NService()).getKey(key, locale, module));
        return RestUtils.sendOkResponse(result);
    }

    @GET
    @Path("/keys")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresGuest
    @ApiOperation(position=2, value="Get all message specified by keys")
    public Response getMessages(@QueryParam("module") String module, @QueryParam("locale") String locale, @QueryParam("keys") String keys)
    {
        String resourceDir = ConfigurationService.getAsString(GroupService.getInstance().getRootGroup(), "i18NDirectory");
        I18NService service = new I18NService();
        Locale locale1 = service.getLocale(locale);
        Map result = new HashMap();
        if (StringUtils.isNotEmpty(keys)) {
            String[] keyss = keys.split(",", -1);
            for (String key : keyss) {
                result.put(key, service.getString(module, key, locale1, resourceDir));
            }
        }
        return RestUtils.sendOkResponse(result);
    }



}
