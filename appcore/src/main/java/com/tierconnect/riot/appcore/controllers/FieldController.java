package com.tierconnect.riot.appcore.controllers;

import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.services.LicenseService;
import com.tierconnect.riot.appcore.utils.EntityVisibility;
import com.tierconnect.riot.appcore.utils.GeneralVisibilityUtils;
import com.tierconnect.riot.appcore.utils.VisibilityUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import com.tierconnect.riot.appcore.entities.Field;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.services.FieldService;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.jaxrs.PATCH;
import org.apache.shiro.subject.Subject;

/**
 * 
 * @author garivera
 *
 */
@Path("/field")
@Api("/field")
public class FieldController extends FieldControllerBase
{

    @Override
    public void validateSelect(Field field) {
        if (!includeInSelect(field)) {
            throw new UserException(String.format( "FieldId[%d] not found", field.getId()) );
        }
        super.validateSelect(field);
    }

    @Override
    public boolean includeInSelect(Field field) {
        Subject subject = SecurityUtils.getSubject();
        User currentUser = (User) subject.getPrincipal();
        return super.includeInSelect(field) && LicenseService.getInstance().isValidField(currentUser, field.getName());
    }

    @PATCH
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"field:u"})
    @Override
    @ApiOperation(value="Update User Field")
    public Response updateField( @PathParam("id") Long id, Map<String, Object> map ) {
        Field field = FieldService.getInstance().get( id );
        if( field == null )
        {
            return RestUtils.sendBadResponse( String.format( "FieldId[%d] not found", id) );
        }
        // 2. Limit visibility based on user's group and the object's group (group based authorization)
        EntityVisibility entityVisibility = getEntityVisibility();
        GeneralVisibilityUtils.limitVisibilityUpdate(entityVisibility, field, VisibilityUtils.getObjectGroup(map));
        // 7. handle insert and update
        if (map.containsKey("description")) {
            field.setDescription((String) map.get("description"));
        }
        // 6. handle validation in an Extensible manner
        validateUpdate( field );
        Map<String,Object> publicMap = field.publicMap();
        return RestUtils.sendOkResponse( publicMap );
    }

    @Override
    public void validateUpdate(Field field) {
        validateSelect(field);
        super.validateUpdate(field);
    }


}
