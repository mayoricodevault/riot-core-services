package com.tierconnect.riot.iot.controllers;

import com.tierconnect.riot.appcore.services.RecentService;
import com.tierconnect.riot.appcore.utils.Configuration;
import com.tierconnect.riot.appcore.utils.EntityVisibility;
import com.tierconnect.riot.appcore.utils.GeneralVisibilityUtils;
import com.tierconnect.riot.appcore.utils.VisibilityUtils;
import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.entities.ScheduledRule;
import com.tierconnect.riot.iot.services.EdgeboxService;
import com.tierconnect.riot.iot.services.ScheduledRuleService;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.utils.BeanUtils;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.jaxrs.PATCH;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import javax.annotation.Generated;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Path("/scheduledRule")
@Api("/scheduledRule")
@Generated("com.tierconnect.riot.appgen.service.GenController")
public class ScheduledRuleController extends ScheduledRuleControllerBase 
{
    private Logger logger = Logger.getLogger(ScheduledRuleController.class);
    @PUT
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresPermissions(value={"scheduledRule:i"})
    @ApiOperation(position=3, value="Insert a ScheduledRule")
    public Response insertScheduledRule(Map<String, Object> map, @QueryParam("createRecent") @DefaultValue("false") Boolean createRecent )
    {
        // 2. Limit visibility based on user's group and the object's group (group based authorization)
        EntityVisibility entityVisibility = getEntityVisibility();
        GeneralVisibilityUtils.limitVisibilityInsert(entityVisibility, VisibilityUtils.getObjectGroup(map));
        ScheduledRule scheduledRule = new ScheduledRule();
        // 7. handle insert and update
        BeanUtils.setProperties( map, scheduledRule );
        // 6. handle validation in an Extensible manner
        validateInsert( scheduledRule );
        ScheduledRuleService.getInstance().insert( scheduledRule );

        if (createRecent){
            RecentService.getInstance().insertRecent(scheduledRule.getId(), scheduledRule.getName(),"scheduledRule", scheduledRule.getGroup());
        }

        Boolean scheduledRuleActive = Boolean.valueOf(Configuration.getProperty("scheduledrule.enabled"));
        if (scheduledRuleActive) {
            // Instantiate ScheduledRuleService
            ScheduledRuleService scheduledRuleService = new ScheduledRuleService();
            // Schedule a job (verify response)
            scheduledRuleService.scheduledRuleJob(scheduledRule);
        }

        // 1[b,c]. Restrict access based on OBJECT and PROPERTY level read permissions
        Map<String,Object> publicMap = scheduledRule.publicMap();
        return RestUtils.sendCreatedResponse( publicMap );
    }

    public void validateInsert( ScheduledRule scheduledRule )
    {
        validateScheduledRule( scheduledRule );
    }

    public void validateScheduledRule( ScheduledRule scheduledRule )
    {
        String scheduledRuleCode = scheduledRule.getCode();
        String scheduledRuleName = scheduledRule.getName();
        if(StringUtils.isEmpty(scheduledRuleCode) )
        {
            throw new UserException( String.format( "Scheduled Rule Code is required.") );
        }

        boolean existsScheduledRuleCode;
        if( scheduledRule.getId() == null )
        {
            // validating for insert case
            existsScheduledRuleCode = ScheduledRuleService.getInstance().existsScheduledRuleCode( scheduledRuleCode, scheduledRule.getGroup() );
        }
        else
        {
            // validating for update case
            existsScheduledRuleCode = ScheduledRuleService.getInstance().existsScheduledRuleCode( scheduledRuleCode, scheduledRule.getGroup(), scheduledRule.getId() );
        }

        if( existsScheduledRuleCode )
        {
            throw new UserException( String.format( "Scheduled Rule Code '[%s]' already exists.", scheduledRule.getCode() ) );
        }

        boolean existsScheduledRuleName;
        if( scheduledRule.getId() == null )
        {
            // validating for insert case
            existsScheduledRuleName = ScheduledRuleService.getInstance().existsScheduledRuleName( scheduledRuleName, scheduledRule.getGroup() );
        }
        else
        {
            // validating for update case
            existsScheduledRuleName = ScheduledRuleService.getInstance().existsScheduledRuleName( scheduledRuleName, scheduledRule.getGroup(), scheduledRule.getId() );
        }

        if( existsScheduledRuleName )
        {
            throw new UserException( String.format( "Scheduled Rule Name '[%s]' already exists.", scheduledRule.getName() ) );
        }

        if (scheduledRule.getActive() && !ScheduledRuleService.getInstance().isCBRestConnection(scheduledRule.getEdgebox().getConfiguration())){
            throw new UserException( String.format( "CoreBridge=[%s] selected for Scheduled Rule Code '[%s]' doesn't contain REST " +
                    "connection, is not possible to start the Job.", scheduledRule.getEdgebox().getCode(), scheduledRule.getCode() ) );
        }
        if (!ScheduledRuleService.getInstance().validateCronExpression(scheduledRule.getCron_expression())){
            throw new UserException( String.format( "Cron Expression [" + scheduledRule.getCron_expression() + "] is not valid." ));
        }
    }

    @PATCH
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresPermissions(value={"scheduledRule:u:{id}"})
    @ApiOperation(position=4, value="Update a ScheduledRule (AUTO)")
    public Response updateScheduledRule(@PathParam("id") Long id, Map<String, Object> map )
    {
        ScheduledRule scheduledRule = ScheduledRuleService.getInstance().get( id );
        if( scheduledRule == null )
        {
            return RestUtils.sendBadResponse( String.format( "ScheduledRuleId[%d] not found", id) );
        }
        // 2. Limit visibility based on user's group and the object's group (group based authorization)
        EntityVisibility entityVisibility = getEntityVisibility();
        GeneralVisibilityUtils.limitVisibilityUpdate( entityVisibility, scheduledRule, VisibilityUtils.getObjectGroup(map));
        // 7. handle insert and update
        BeanUtils.setProperties( map, scheduledRule );
        // 6. handle validation in an Extensible manner
        validateUpdate( scheduledRule );
        ScheduledRuleService.getInstance().updateFavorite(scheduledRule);

        // Instantiate ScheduledRuleService
        ScheduledRuleService scheduledRuleService = new ScheduledRuleService();

        // update rules if reportDefinition selected changed
        scheduledRuleService.updateRulesByReport(scheduledRule);

        Boolean scheduledRuleActive = Boolean.valueOf(Configuration.getProperty("scheduledrule.enabled"));
        if (scheduledRuleActive) {
            // Schedule a job
            scheduledRuleService.scheduledRuleJob(scheduledRule);
            // get CoreBridge releated to Scheduled Rule
            Edgebox corebridge = scheduledRule.getEdgebox();
            Map<String, Object> publicMap = corebridge.publicMap();
            // publish update tickle to update rules in CoreBridge
            EdgeboxService edgeboxService = new EdgeboxService();
            try {
                edgeboxService.refreshConfiguration(corebridge.getCode(), corebridge.getType(), corebridge.getStatus(), corebridge.getGroup(), publicMap, true);
            } catch (IOException e) {
                logger.error("It is not possible to send update tickle to coreBridge code '"+corebridge.getCode()+"', reason=", e);
            }
        }

        ScheduledRuleService.getInstance().update( scheduledRule );
        Map<String,Object> publicMap = scheduledRule.publicMap();
        return RestUtils.sendOkResponse( publicMap );
    }

    public void validateUpdate( ScheduledRule scheduledRule )
    {
        validateScheduledRule( scheduledRule );
    }

    @GET
    @Path("/delete/validate/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(value="Validation when scheduled rule is deleted, returns true if it has dependencies")
    public Response validateDeleteLocalMap( @PathParam("id") Long id )
    {
        ScheduledRule scheduledRule = ScheduledRuleService.getInstance().get( id );
        if( scheduledRule == null ){
            return RestUtils.sendBadResponse( String.format( "ScheduledRuleId [%d] not found", id) );
        }
        // Limit visibility based on user's group and the object's group (group based authorization)
        EntityVisibility entityVisibility = getEntityVisibility();
        GeneralVisibilityUtils.limitVisibilityDelete(entityVisibility, scheduledRule);

        return RestUtils.sendOkResponse(ScheduledRuleService.getInstance().validateDeleteScheduledRule(id));
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"scheduledRule:d:{id}"})
    // 1a. Limit access based on CLASS level resources
    @ApiOperation(position=5, value="Delete a ScheduledRule (AUTO)")
    public Response deleteScheduledRule( @PathParam("id") Long id, @QueryParam("cascadeDelete") @DefaultValue("false") boolean cascadeDelete )
    {
        // 1c. TODO: Restrict access based on OBJECT level read permissions
        ScheduledRule scheduledRule = ScheduledRuleService.getInstance().get( id );
        if( scheduledRule == null )
        {
            return RestUtils.sendBadResponse( String.format( "ScheduledRuleId[%d] not found", id) );
        }
        // 2. Limit visibility based on user's group and the object's group (group based authorization)
        EntityVisibility entityVisibility = getEntityVisibility();
        GeneralVisibilityUtils.limitVisibilityDelete(entityVisibility, scheduledRule );
        // handle validation in an Extensible manner
        validateDelete( scheduledRule );
        RecentService.getInstance().deleteRecent(id,"scheduledRule");
        List<String> messageErrors = ScheduledRuleService.getInstance().deleteCurrentScheduledRule(scheduledRule, cascadeDelete);
		if (!messageErrors.isEmpty()) {
			return RestUtils.sendBadResponse(StringUtils.join(messageErrors, ","));
		}else{
			return RestUtils.sendDeleteResponse();
		}
    }

    public void validateDelete( ScheduledRule scheduledRule )
    {

    }
}

