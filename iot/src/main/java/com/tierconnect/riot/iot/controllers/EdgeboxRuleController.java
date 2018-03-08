package com.tierconnect.riot.iot.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.utils.*;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.EdgeboxRuleService;
import com.tierconnect.riot.iot.services.ThingTypeService;
import com.tierconnect.riot.sdk.dao.Pagination;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.utils.BeanUtils;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.jaxrs.PATCH;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import javax.annotation.Generated;
import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by cfernandez
 * 11/27/2014.
 */

@Path("/edgeboxRule")
@Api("/edgeboxRule")
@Generated("com.tierconnect.riot.appgen.service.GenControllerBase")
public class EdgeboxRuleController extends EdgeboxRuleControllerBase{

	private Logger logger = Logger.getLogger( EdgeboxRuleController.class );

	@Context
	ServletContext context;

	//this function takes the rule and check if the rule is in the new format,
	//new format means, they are using the MessageEventType in the select clause
	//if not is in the new format, apply regular expression to get the thingType of the rule, and then
	//rebuild the rule with the new format
	//the new rule format has this structure:
	//   select * from messageEventType where udf('thingTypeCode') = 'your_thing_type_code') and ( your_original_condition )
	private HashMap<String,String> processRule( String rule )
	{
		HashMap<String, String> resMap = new HashMap<>();
		resMap.put( "condition",   "");
		resMap.put( "thingTypeId", "");

		//because this function is auto-converting rules from previous format to the new one,
		//we are dealing with two thingTypes, the first one in the from clause, and the second in the where clause
		String whereThingType = "";
		String fromThingType  = "";
		StringBuilder condition = new StringBuilder( "" );

		rule = rule + " ";
		char car;
		boolean select = false;
		boolean asterisk = false;
		boolean from = false;
		boolean where = false;

		//1. check if the thingtype is in defined in the query
		//if we found it, then save it in this variable: whereThingType
		StringBuilder token = new StringBuilder("");

		StringBuilder strPattern = new StringBuilder( "" );
		strPattern.append( "udf" );     //udf//
		strPattern.append( "\\s*" );    //udf   //
		strPattern.append( "\\(" );     //udf   (//
		strPattern.append( "\\s*" );    //udf   (   //
		strPattern.append( "['\"]" );   //udf   (   "//
		strPattern.append( "thingTypeCode" ); //udf   (   "thingTypeCode//
		strPattern.append( "['\"]" );         //udf   (   "thingTypeCode"//
		strPattern.append( "\\s*" );          //udf   (   "thingTypeCode"  //
		strPattern.append( "\\)" );           //udf   (   "thingTypeCode"  )//
		strPattern.append( "\\s*" );          //udf   (   "thingTypeCode"  )   //
		strPattern.append( "=" );             //udf   (   "thingTypeCode"  )  =//
		strPattern.append( "\\s*" )  ;        //udf   (   "thingTypeCode"  )  =  //
		strPattern.append( "['\"]" );         //udf   (   "thingTypeCode"  )  =  "//
		strPattern.append( "([a-zA-Z0-9_-[.]]*?)" ); //udf   (   "thingTypeCode"  )  =  "assets//
		strPattern.append( "['\"]" );         //udf   (   "thingTypeCode"  )  =  "assets"//
		strPattern.append( "\\s*" );          //udf   (   "thingTypeCode"  )  =  "assets" //
		Pattern pattern = Pattern.compile( strPattern.toString() );
		Matcher matcher = pattern.matcher( rule );
		while (matcher.find()) {
			whereThingType = matcher.group(1);
		}

		//2. check the query structure
		for (int i=0; i < rule.length() ; i++ ) {
			car = rule.charAt( i );
			if ( car == '\n' || car == '\r' ||  car == ' ' || car == '\t' || i == rule.length()-1) {

				//validate every important token
				if ( token.toString().toLowerCase().equals( "select" ) && !select) {
					select = true;
					token = new StringBuilder("");
				}
				if ( token.toString().toLowerCase().equals( "*" ) && select ) {
					asterisk = true;
					token = new StringBuilder("");
				}
				if ( token.toString().toLowerCase().equals( "from" ) && asterisk ) {
					from = true;
					token = new StringBuilder("");
				}
				if ( token.toString().toLowerCase().equals( "where" ) && fromThingType.length() > 0 ) {
					where = true;
					token = new StringBuilder("");
				}
				//fromThingType, we have the default thingtype
				if (select && asterisk && from && fromThingType.length() == 0) {
					fromThingType = token.toString();
					token = new StringBuilder("");
				}

				if( token.length() > 0 || car == '\t' || car == ' ')
				{
					if (select && asterisk && from && fromThingType.length() > 0 && where ) {
						condition.append( token.toString() );
						condition.append( car );
					} else
					{
						//System.out.print( index + ". " );
						//System.out.println( token.toString() );
					}
					token = new StringBuilder("");
				}
				continue;
			}

			token = token.append( car );
		}

		//now try to build new format, getting the rule and the condition
		if (!whereThingType.equals("") ) {
			//the rule is in the new format
			if ( fromThingType.equals( "messageEventType" )) {
				resMap.put( "condition",   getConditionFromRule ( condition.toString() ) );
				resMap.put( "thingTypeId", getThingTypeFromCode ( whereThingType));
				resMap.put( "thingtype", whereThingType);
			} else {
                //building the new rule
                resMap.put( "condition",   getConditionFromRule ( condition.toString() ) );
                resMap.put( "thingTypeId", getThingTypeFromCode ( whereThingType ));
                resMap.put( "thingtype", whereThingType);
            }
        }
		else
		{
			String strCondition = condition.toString();
			if(strCondition.contains("\n")){
				strCondition = strCondition.trim();
				strCondition = strCondition.substring(1,strCondition.lastIndexOf(")"));
			}
			resMap.put( "condition",   strCondition.trim() );
            resMap.put( "thingTypeId", getThingTypeFromCode (fromThingType) );
            resMap.put( "thingtype", whereThingType);
        }
        logger.info("thingTypeId: " + resMap.get("thingTypeId") );
        logger.info("thingtype:   " + resMap.get( "thingtype" ) );
        logger.info("condition:   " + resMap.get( "condition" ) );

		return resMap;
	}


	private String getThingTypeFromCode( String thingTypeCode)
	{
		String id = "";

		try
		{
			ThingType tt = ThingTypeService.getInstance().getByCode( thingTypeCode );
			id = "" + tt.getId();
		} catch ( Exception e ) {

		}

		return id;
	}

	private String getConditionFromRule( String rule)
	{
		//add a space before the \n, because we found some errors with the regular expression
		rule  = rule.trim();

		String condition = "";
		StringBuilder strPattern = new StringBuilder( "" );
		strPattern.append( "(udf\\('thingTypeCode'\\) = " );     //udf('thingTypeCode') =
		strPattern.append( "['\"]" );     //udf('thingTypeCode') = '
		strPattern.append( "[a-zA-Z0-9_[-][.]]*?" );     //udf('thingTypeCode') = 'assets'
		strPattern.append( "['\"]" );     //udf('thingTypeCode') = 'assets'
		strPattern.append( "\\s*" );      //udf('thingTypeCode') = 'assets'
		strPattern.append( "and\\s*)" );       //udf('thingTypeCode') = 'assets' and
		Pattern pattern = Pattern.compile( strPattern.toString() );

		Matcher matcher = pattern.matcher( rule );
        while (matcher.find()) {
            if (matcher.groupCount() > 0) {
                //get the second part
                String temp = matcher.group(1);
                condition = rule.substring(temp.length(), rule.length()).trim();

                //remove parentheses just at the beginning and end
                int start = 0;
                int end = condition.length();
                Pattern patternParenthesis = Pattern.compile("\\((.*)\\)");
                Matcher matcherParenthesis = patternParenthesis.matcher(condition);
                if (matcherParenthesis.matches()) {
                    start++;
                    end--;
                }
                condition = condition.substring(start, end).trim();
            }
        }

		return condition;
	}

	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	// 1a. Limit access based on CLASS level resources
	@RequiresPermissions(value={"edgeboxRule:r:{id}"})
	@ApiOperation(position=2, value="Select a EdgeboxRule (AUTO)")
	public Response selectEdgeboxRules( @PathParam("id") Long id, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
	{
		EdgeboxRule edgeboxRule = EdgeboxRuleService.getInstance().get( id );
		if( edgeboxRule == null )
		{
			return RestUtils.sendBadResponse( String.format( "EdgeboxRuleId[%d] not found", id) );
		}
		// 2. Limit visibility based on user's group and the object's group (group based authorization)
		EntityVisibility entityVisibility = getEntityVisibility();
		GeneralVisibilityUtils.limitVisibilitySelect( entityVisibility, edgeboxRule );
		validateSelect( edgeboxRule );
		// 5a. Implement extra
		Map<String,Object> publicMap = QueryUtils.mapWithExtraFields( edgeboxRule, extra, getExtraPropertyNames() );
		addToPublicMap(edgeboxRule, publicMap, extra);

		//add condition and thingType
		HashMap<String,String> resMap = processRule( publicMap.get("rule").toString());
		publicMap.put( "condition",   resMap.get("condition")   );
		publicMap.put( "thingTypeId", resMap.get("thingTypeId") );
		// 5b. Implement only
		QueryUtils.filterOnly( publicMap, only, extra );
		QueryUtils.filterProjectionNested( publicMap, project, extend);
		return RestUtils.sendOkResponse( publicMap );
	}


	@PATCH
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresPermissions(value={"edgeboxRule:u:{id}"})
    @ApiOperation(position=4, value="Update a EdgeboxRule and Refresh Configuration CoreBridge")
    public Response updateEdgeboxRule( @PathParam("id") Long id, Map<String, Object> map )
    {
        // 1[b,c]. Restrict access based on OBJECT and PROPERTY level write permissions
        //QueryUtils.filterWritePermissions(EdgeboxRule.class, map);
        EdgeboxRuleService edgeboxRuleService = EdgeboxRuleService.getInstance();
        EdgeboxRule edgeboxRule = edgeboxRuleService.get(id);
        if( edgeboxRule == null )
        {
            return RestUtils.sendBadResponse(String.format("EdgeboxRuleId[%d] not found", id));
        }

		String lastConditionType = edgeboxRule.getConditionType();
		String rule = buildRule(map);
		logger.debug("create rule " + rule);
		map.put("rule", rule);
		String bridgeRuleName = map.get("name").toString();
		boolean isValidName = validateEdgeBoxRuleName(bridgeRuleName, edgeboxRule);
		if (!isValidName){
			return RestUtils.sendBadResponse( "Bridge Rule \"" + bridgeRuleName+ "\" already exists.");
		}
		//remove added items in the map, these fields are not in the database
		map.remove( "condition" );
		map.remove( "thingtype");
		//remove sortOrder to block update in the patch
		map.remove( "sortOrder");
        // 7. handle insert and update
        BeanUtils.setProperties(map, edgeboxRule);

		// generate the condition for scheduled rules
		String condition = edgeboxRuleService.createConditionForRuleScheduled(edgeboxRule);
		if (condition!=null){
			edgeboxRule.setRule(condition);
		}

        // 6. handle validation in an Extensible manner
        validateUpdate(edgeboxRule);
		EdgeboxRuleService.getInstance().setDefaultValues(edgeboxRule);
        edgeboxRuleService.update( edgeboxRule );
		edgeboxRuleService.updateOrderByConditionType(edgeboxRule, lastConditionType);
        Map<String,Object> publicMap = edgeboxRule.publicMap();

        String data = serializeData(edgeboxRule);
        edgeboxRuleService.refreshConfiguration(edgeboxRule.getEdgebox(), data, false);
        edgeboxRuleService.refreshEdgeboxRuleCache(edgeboxRule, false);

        return RestUtils.sendOkResponse( publicMap );
    }

    @PUT
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresPermissions(value={"edgeboxRule:i"})
    @ApiOperation(position=3, value="Insert a EdgeboxRule and Refresh Configuration CoreBridge")
    public Response insertEdgeboxRule( Map<String, Object> map )
    {
        //2. TODO: Limit visibility based on user's group and the object's group (group based authorization)
        EdgeboxRule edgeboxRule = new EdgeboxRule();
        EdgeboxRuleService edgeboxRuleService = EdgeboxRuleService.getInstance();

		String rule = buildRule(map);
		logger.debug("create rule " + rule);
		map.put("rule", rule);
		String bridgeRuleName = map.get("name").toString();
		BeanUtils.setProperties(map, edgeboxRule);
		boolean isValidName = validateEdgeBoxRuleName(bridgeRuleName, edgeboxRule);
		if (!isValidName){
			return RestUtils.sendBadResponse( "Bridge Rule \"" + bridgeRuleName+ "\" already exists.");
		}
		// 7. handle insert and update
		edgeboxRuleService.validateConnectionCode(edgeboxRule);

		// generate the condition for scheduled rules
		String condition = edgeboxRuleService.createConditionForRuleScheduled(edgeboxRule);
		if (condition!=null){
			edgeboxRule.setRule(condition);
		}

        // 6. handle validation in an Extensible manner
        validateInsert( edgeboxRule );
		EdgeboxRuleService.getInstance().setDefaultValues(edgeboxRule);
        edgeboxRuleService.insert(edgeboxRule);
        // 1[b,c]. Restrict access based on OBJECT and PROPERTY level read permissions
        Map<String,Object> publicMap = edgeboxRule.publicMap();

        String data = serializeData(edgeboxRule);
        edgeboxRuleService.refreshConfiguration(edgeboxRule.getEdgebox(), data, false);
        edgeboxRuleService.refreshEdgeboxRuleCache(edgeboxRule, false);
        
        return RestUtils.sendCreatedResponse(publicMap);
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value={"edgeboxRule:d:{id}"})
    // 1a. Limit access based on CLASS level resources
    @ApiOperation(position=5, value="Delete a EdgeboxRule and Refresh Configuration CoreBridge")
    public Response deleteEdgeboxRule( @PathParam("id") Long id )
    {
        // 1c. TODO: Restrict access based on OBJECT level read permissions
        EdgeboxRuleService edgeboxRuleService = EdgeboxRuleService.getInstance();

        EdgeboxRule edgeboxRule = edgeboxRuleService.get(id);
        if( edgeboxRule == null )
        {
            return RestUtils.sendBadResponse( String.format( "EdgeboxRuleId[%d] not found", id) );
        }
        //2. TODO: Limit visibility based on user's group and the object's group (group based authorization)
        // handle validation in an Extensible manner
        validateDelete( edgeboxRule );
		edgeboxRule.setActive(false);
        edgeboxRuleService.delete(edgeboxRule);

        String data = serializeData(edgeboxRule);
        edgeboxRuleService.refreshConfiguration(edgeboxRule.getEdgebox(), data, false);
        edgeboxRuleService.refreshEdgeboxRuleCache(edgeboxRule, true);
        //update all sortOrder values in rules by edgebox
        edgeboxRuleService.updateOrderRules(edgeboxRule.getEdgebox(), null, "");
        return RestUtils.sendDeleteResponse();
    }

    private String serializeData(EdgeboxRule edgeboxRule) {
        EdgeboxRule edgeboxRule0 = new EdgeboxRule();
        edgeboxRule0.setId(edgeboxRule.getId());
        edgeboxRule0.setName(edgeboxRule.getName());
        edgeboxRule0.setRule(edgeboxRule.getRule());
        edgeboxRule0.setInput(edgeboxRule.getInput());
        edgeboxRule0.setOutput(edgeboxRule.getOutput());
        edgeboxRule0.setOutputConfig(edgeboxRule.getOutputConfig());
        edgeboxRule0.setActive(edgeboxRule.getActive());

        String data = "";
        ObjectMapper mapper = new ObjectMapper();
        try {
            data = mapper.writeValueAsString(edgeboxRule0);
        } catch (JsonProcessingException e) {
            System.out.println("cannot serialize edgeboxRule object");
            e.printStackTrace();
        } catch (Exception e){
            e.printStackTrace();
        }
        System.out.println("EdgeboxRule serialized data: " + data);
        return data;
    }

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresPermissions(value={"edgeboxRule:r"})
    @ApiOperation(position=1, value="Get a List of EdgeboxRules")
    @Override
    public Response listEdgeboxRules(@QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber, @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only, @QueryParam("visibilityGroupId") Long visibilityGroupId, @DefaultValue("") @QueryParam("upVisibility") String upVisibility, @DefaultValue("") @QueryParam("downVisibility") String downVisibility, @DefaultValue("") @QueryParam("returnFavorite") boolean returnFavorite, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
    {
        Pagination pagination = new Pagination( pageNumber, pageSize );

        BooleanBuilder be = new BooleanBuilder();
        // 2. Limit visibility based on user's group and the object's group (group based authorization)
			Group visibilityGroup = VisibilityUtils.getVisibilityGroup(EdgeboxRule.class.getCanonicalName(), visibilityGroupId);
        EntityVisibility entityVisibility = getEntityVisibility();
        be = be.and( GeneralVisibilityUtils.limitVisibilitySelectAll(entityVisibility, QEdgeboxRule.edgeboxRule,  visibilityGroup, upVisibility, downVisibility ) );
        // 4. Implement filtering
        be = be.and( QueryUtils.buildSearch( QEdgeboxRule.edgeboxRule, where ) );

        Long count = EdgeboxRuleService.getInstance().countList( be );
        List<Map<String, Object>> list = new LinkedList<Map<String, Object>>();
        // 3. Implement pagination
        for( EdgeboxRule edgeboxRule : EdgeboxRuleService.getInstance().listPaginated( be, pagination, order ) )
        {
            // Additional filter
            if (!includeInSelect(edgeboxRule))
            {
                continue;
            }
            // 5a. Implement extra
            Map<String,Object> publicMap = QueryUtils.mapWithExtraFields( edgeboxRule, extra, getExtraPropertyNames());

            //TODO refactor this ... this canche is because ther's no enough time

            HashMap<String,String> resMap = processRule( publicMap.get("rule").toString());
            publicMap.put( "condition",   resMap.get("condition")   );
            publicMap.put( "thingTypeId", resMap.get("thingTypeId") );
            publicMap.put( "thingtype", resMap.get("thingtype") );

            addToPublicMap(edgeboxRule, publicMap, extra);
            // 5b. Implement only
            QueryUtils.filterOnly( publicMap, only, extra );
            QueryUtils.filterOnly( publicMap, project, extend );
            list.add( publicMap );
        }
        Map<String,Object> mapResponse = new HashMap<String,Object>();
        mapResponse.put( "total", count );
        mapResponse.put( "results", list );
        return RestUtils.sendOkResponse( mapResponse );
    }

	/**
	 * @param map
	 * @return
	 */
	private String buildRule(Map<String, Object> map) throws UserException {
		// join thingtype and the condition in the rule
		// according to new interface, when we have the thingtype in a dropdown, and the condition in a textarea
		logger.info(String.format("Create query rule with conditionType: \"%s\", condition: \"%s\" , thingtype \"%s\"",
				map.get("conditionType"), map.get("condition"), map.get("thingtype")));

		List<String> listCondition = new ArrayList<>();
		String rule = "select * from messageEventType";
		String thingtype = null;
		String condition = null;

		if (map.containsKey("condition")
				&& map.get("condition") != null
				&& !Utilities.isEmptyOrNull(map.get("condition").toString())) {
			condition = map.get("condition").toString().trim();
		}

		if (map.containsKey("thingtype")
				&& map.get("thingtype") != null
				&& !Utilities.isEmptyOrNull(map.get("thingtype").toString())) {
			thingtype = map.get("thingtype").toString();
		}

		if (!Utilities.isEmptyOrNull(thingtype)) {
			listCondition.add("udf('thingTypeCode') = '" + thingtype + "' ");
		}

		if (!Utilities.isEmptyOrNull(condition)) {
			if (condition.endsWith(";")) {
				condition = condition.substring(0, condition.length() - 1);
			}
			listCondition.add("( " + condition + " )");
		}

		if (!listCondition.isEmpty()) {
			rule += " where " + StringUtils.join(listCondition, " and ");
		}
		return rule;
	}

	/**
	 *
	 * @param edgeboxRuleName
	 * @return true, if there isn't an edgebox rule with the "edgeboxRuleName" value
	 */
	public boolean validateEdgeBoxRuleName(String edgeboxRuleName, EdgeboxRule edgeboxRule){
		boolean response = true;
		if(edgeboxRule!=null){
			List<EdgeboxRule> edgeboxRules = EdgeboxRuleService.getInstance().selectByEdgeboxName(
					edgeboxRuleName, edgeboxRule.getGroup(), edgeboxRule.getEdgebox());
			if ((edgeboxRule.getId() != null) && (edgeboxRules.size() == 1) && (edgeboxRule.getId().equals(edgeboxRules.get(0).getId()))) {
				return response;
			} else {
				response =  edgeboxRules.size() <= 0;
			}
		}
		return response;
	}

	@GET
	@Path("/conditionType")
	@Produces(MediaType.APPLICATION_JSON)
	@RequiresPermissions(value={"edgeboxRule:r"})
	@ApiOperation(position=1, value="Get a List of EdgeboxRules Mapping by Group")
	public Response listEdgeboxRulesByGroup(@QueryParam("edgeboxId") Long edgeboxId, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only, @QueryParam("visibilityGroupId") Long visibilityGroupId, @DefaultValue("") @QueryParam("upVisibility") String upVisibility, @DefaultValue("") @QueryParam("downVisibility") String downVisibility, @DefaultValue("") @QueryParam("returnFavorite") boolean returnFavorite, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
	{
		Pagination pagination = new Pagination( 1, -1 );

		Group visibilityGroup = VisibilityUtils.getVisibilityGroup(EdgeboxRule.class.getCanonicalName(), visibilityGroupId);
		EntityVisibility entityVisibility = getEntityVisibility();

		List <ObjectNode> categories = EdgeboxRuleService.getInstance().getRuleGroupParameters();
		Map<String,Object> mapResponse = new HashMap<String,Object>();
		for (ObjectNode category : categories) {
			JsonNode where = category.get("where");
			JsonNode key = category.get("key");
			List<Map<String, Object>> list = new LinkedList<Map<String, Object>>();

			for (EdgeboxRule edgeboxRule : getFilteredRulesList ( edgeboxId, pagination, where.asText(), visibilityGroup, upVisibility, downVisibility ) )
			{
				if (!includeInSelect(edgeboxRule))
				{
					continue;
				}
				Map<String,Object> publicMap = QueryUtils.mapWithExtraFields( edgeboxRule, extra, getExtraPropertyNames());

				HashMap<String,String> resMap = processRule( publicMap.get("rule").toString());
				publicMap.put( "condition",   resMap.get("condition")   );
				publicMap.put( "thingTypeId", resMap.get("thingTypeId") );
				publicMap.put( "thingtype", resMap.get("thingtype") );

				addToPublicMap(edgeboxRule, publicMap, extra);
				QueryUtils.filterOnly( publicMap, only, extra );
				QueryUtils.filterOnly( publicMap, project, extend );
				list.add( publicMap );
			}
			mapResponse.put( key.asText(), list );
		}

		return RestUtils.sendOkResponse( mapResponse );
	}

	public List<EdgeboxRule> getFilteredRulesList(Long edgeboxId, Pagination pagination, String where, Group visibilityGroup, String upVisibility, String downVisibility)
	{
		String order = "sortOrder:asc";
		BooleanBuilder be = new BooleanBuilder();
		be = be.and( GeneralVisibilityUtils.limitVisibilitySelectAll(entityVisibility, QEdgeboxRule.edgeboxRule,  visibilityGroup, upVisibility, downVisibility ) );
		be = be.and( QueryUtils.buildSearch( QEdgeboxRule.edgeboxRule, where ) );
		if (edgeboxId != null) {
			be = be.and( QueryUtils.buildSearch( QEdgeboxRule.edgeboxRule, "edgebox.id=" + Long.toString( edgeboxId ) ) );
		}
		return EdgeboxRuleService.getInstance().listPaginated( be, pagination, order);
	}

	@PATCH
	@Path("/conditionType/bulk")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@RequiresPermissions(value={"edgeboxRule:u"})
	@ApiOperation(position=6, value="Update order of execution of EdgeboxRules Mapping by Group")
	public Response updateOrderEdgeboxRulesByGroup(Map<String, Object> map)
	{
		Map<String,Object> mapResponse = new HashMap<String, Object>();
		mapResponse = EdgeboxRuleService.getInstance().bulkUpdateOrderRule( map );
		return RestUtils.sendOkResponse( mapResponse );
	}
}
