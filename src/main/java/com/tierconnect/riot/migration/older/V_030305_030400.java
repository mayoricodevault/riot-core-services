package com.tierconnect.riot.migration.older;

import com.fasterxml.jackson.core.type.TypeReference;
import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.dao.ResourceDAO;
import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.ResourceService;
import com.tierconnect.riot.appcore.services.RoleResourceService;
import com.tierconnect.riot.appcore.services.RoleService;
import com.tierconnect.riot.appcore.utils.QueryUtils;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import com.tierconnect.riot.sdk.dao.Pagination;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by cfernandez
 * on 9/25/15.
 */
@Deprecated
public class V_030305_030400 implements MigrationStepOld
{

	static Logger logger = Logger.getLogger(V_030305_030400.class);

	@Override
	public List<Integer> getFromVersions() {
		return Arrays.asList(30305);

	}

	@Override
	public int getToVersion() {
		return 30400;
	}

	@Override
	public void migrateSQLBefore() throws Exception {
		DBHelper dbHelper = new DBHelper();
		String databaseType = dbHelper.getDataBaseType();
		dbHelper.executeSQLFile("sql/" + databaseType + "/V030303_to_030400.sql");
	}

	@Override
	public void migrateHibernate() throws Exception {
		migrateReportProperties();
		Group rootGroup = GroupService.getInstance().getRootGroup();
		Field f34 = PopDBUtils.popFieldService("playbackMaxThings", "playbackMaxThings", "Playback Max Things", rootGroup, "Look & Feel", "java.lang.Integer", 2L, true);
		PopDBUtils.popGroupField(rootGroup, f34, "100");
		//migrateDataTypes();
		migrateResources();
		migrateBridges();
		migrateRemoveCassandraConf();
	}

	private void migrateBridges()
	{
		logger.info("Start Migrating coreBridge...");

		logger.info("Setting the group of coreBridge to all rules...");
		EdgeboxService edgeboxService  = EdgeboxService.getInstance();
		Edgebox edgebox = edgeboxService.selectByCode("MCB");

		EdgeboxRuleService edgeboxRuleService = EdgeboxRuleServiceBase.getInstance();
		List<EdgeboxRule> rules = edgeboxRuleService.selectByEdgeboxId(edgebox.getId());
		for (EdgeboxRule rule : rules) {
			rule.setGroup(edgebox.getGroup());
		}

		logger.info("Assigning root as a group of coreBridge...");
		Group rootGroup = GroupService.getInstance().getRootGroup();
		edgebox.setGroup(rootGroup);

		logger.info("Rebuild Edgebox rules with new format for Esper Rules...");
		rebuildEsperRules();

		logger.info("End Migrating coreBridge");
	}


	@Override
	public void migrateSQLAfter() throws Exception {

	}

	public void migrateResources() {
		Group rootGroup = GroupService.getInstance().getRootGroup();
		ResourceService resourceService = ResourceService.getInstance();
		ResourceDAO resourceDAO = ResourceService.getResourceDAO();

		if (resourceDAO.selectBy("name", Resource.REPORT_INSTANCES_MODULE) == null) {
			Resource moduleReportInstances = ResourceService.getInstance().insert(Resource.getModuleResource(rootGroup, Resource.REPORT_INSTANCES_MODULE, "Report Instances"));
			List<Resource> resources = resourceDAO.selectAllBy(QResource.resource.type.eq(ResourceType.REPORT_DEFINITION.getId()));
			for (Resource resource : resources) {
				resource.setParent(moduleReportInstances);
			}
		}

		Resource rEditOwn = resourceDAO.selectBy("name", "reportDefinition_editOwn");
		rEditOwn.setLabel("Edit own report");

		Resource emailRecipients = resourceDAO.selectBy("name", "reportDefinition_emailRecipients");
		emailRecipients.setLabel("Set email recipients");
		emailRecipients.setAcceptedAttributes("ru");
		// Resource rdAssignThing = resourceDAO.selectBy("name", "reportDefinition_assignThing");
		// rdAssignThing.setLabel("Thing Associate");

		// Resource rdUnAssignThing = resourceDAO.selectBy("name", "reportDefinition_unAssignThing");
		// rdUnAssignThing.setLabel("Thing Disassociate");

		modifyAssociateDisassociateResource();

		Resource rdAssignUnAssignThing = resourceDAO.selectBy("name", "reportDefinition_assignUnAssignThing");
		rdAssignUnAssignThing.setLabel("Thing Associate & Disassociate");

		Resource rdInlineEdit = resourceDAO.selectBy("name", "reportDefinition_inlineEdit");
		rdInlineEdit.setLabel("Thing Property");

		Resource r = resourceDAO.selectBy("name", "reportDefinition_inlineEditGroup");
		r.setFqname("Inline Edit");
		r.setLabel("Inline Edit");

		updateZPLEThingType();
	}


	private void migrateReportProperties(){
		for (ReportProperty reportProperty : ReportPropertyService.getReportPropertyDAO().selectAll()){
			reportProperty.setShowHover(false);
			ReportPropertyService.getReportPropertyDAO().update(reportProperty);
		}
	}

	//rebuild esper rules
	//this function takes the rule and check if the rule is in the new format,
	//new format means, they are using the MessageEventType in the select clause
	//if not is in the new format, apply regular expression to get the thingType of the rule, and then
	//rebuild the rule with the new format
	//the new rule format has this structure:
	//   select * from messageEventType where udf('thingTypeCode') = 'your_thing_type_code') and ( your_original_condition )
	private String tokenizerRule(String name, String rule)
	{
		//System.out.println(rule);

		//because this function is auto-converting rules from previous format to the new one,
		//we are dealing with two thingTypes, the first in the from clause, and the second in the where clause
		String whereThingType = "";
		String fromThingType = "";
		StringBuilder condition = new StringBuilder( "" );

		rule = rule + " ";
		char car;
		boolean select = false;
		boolean asterisk = false;
		boolean from = false;
		boolean where = false;


		StringBuilder token = new StringBuilder("");

		StringBuilder strPattern = new StringBuilder( "" );
		strPattern.append( "udf" );    //udf//
		strPattern.append( "\\s*" );   //udf   //
		strPattern.append( "\\(" );    //udf   (//
		strPattern.append( "\\s*" );   //udf   (   //
		strPattern.append( "['\"]" );  //udf   (   "//
		strPattern.append( "thingTypeCode" );  //udf   (   "thingTypeCode//
		strPattern.append( "['\"]" );  //udf   (   "thingTypeCode"//
		strPattern.append( "\\s*" );   //udf   (   "thingTypeCode"  //
		strPattern.append( "\\)" );    //udf   (   "thingTypeCode"  )//
		strPattern.append( "\\s*" );   //udf   (   "thingTypeCode"  )   //
		strPattern.append( "=" );      //udf   (   "thingTypeCode"  )  =//
		strPattern.append( "\\s*" );   //udf   (   "thingTypeCode"  )  =  //
		strPattern.append( "['\"]" );  //udf   (   "thingTypeCode"  )  =  "//
		strPattern.append( "(.*?)" );  //udf   (   "thingTypeCode"  )  =  "assets//
		strPattern.append( "['\"]" );  //udf   (   "thingTypeCode"  )  =  "assets"//
		strPattern.append( "\\s*" );  //udf   (   "thingTypeCode"  )  =  "assets" //
		Pattern pattern = Pattern.compile( strPattern.toString() );
		Matcher matcher = pattern.matcher( rule );
		while (matcher.find()) {
			whereThingType = matcher.group(1);
		}

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

				if( token.length() > 0)
				{
					if (select && asterisk && from && fromThingType.length() > 0 && where ) {
						condition.append( token.toString() );
						condition.append( car );
					}
					token = new StringBuilder("");
				}
				continue;
			}

			token = token.append( car );
		}

		StringBuilder newRule   = new StringBuilder( "" );

		if (!whereThingType.equals("") ) {
			//fromThingType = whereThingType;
			if ( fromThingType.equals( "messageEventType" )) {
				newRule.append( rule );
			} else {
				newRule.append( "select * from messageEventType where udf('thingTypeCode') = '" + whereThingType + "'" );
				if( condition.toString().length() > 0 )
				{
					newRule.append( " and ( " + condition.toString() + " ) " );
				}
			}
		}
		else
		{
			newRule.append( "select * from messageEventType where udf('thingTypeCode') = '" + fromThingType + "'" );
			if( condition.toString().length() > 0 )
			{
				newRule.append( " and ( " + condition.toString() + " ) " );
			}
		}
		logger.info("converting EdgeboxRule " + name);
		logger.info("from: " + rule);
		logger.info("to: " + newRule.toString() );

		return newRule.toString();
	}

	private void rebuildEsperRules()
	{
		EdgeboxRuleService ruleService  = EdgeboxRuleServiceBase.getInstance();

		String extra = "";
		List<String> getExtraPropertyNames = new ArrayList<String>();
		List<Map<String, Object>> list = new LinkedList<Map<String, Object>>();

		Pagination pagination = new Pagination(1, -1);
		BooleanBuilder be = new BooleanBuilder();
		be = be.and( QueryUtils.buildSearch( QEdgeboxRule.edgeboxRule, "" ) );

		Long count = EdgeboxRuleService.getInstance().countList( be );

		for( EdgeboxRule edgeboxRule : ruleService.listPaginated( be, pagination, "" ) )
		{
			Map<String,Object> publicMap = QueryUtils.mapWithExtraFields( edgeboxRule, extra, getExtraPropertyNames);
			edgeboxRule.setRule( tokenizerRule( edgeboxRule.getName(), edgeboxRule.getRule()));

			ruleService.update( edgeboxRule );

			list.add( publicMap );
		}

	}

	private void migrateRemoveCassandraConf()
	{
		Edgebox edgebox = EdgeboxService.getInstance().selectByCode("MCB");
		com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
		Map<String, Object> mapResponse = new HashMap<String, Object>();
		logger.info( "Trying to remove Cassandra Configuration" );
		try {
			mapResponse = mapper.readValue( edgebox.getConfiguration(), new TypeReference<Map<String, Object>>(){} );
			if ( mapResponse.containsKey("cassandra") )
			{
				mapResponse.remove("cassandra");
				edgebox.setConfiguration(mapper.writeValueAsString(mapResponse));
				EdgeboxService.getInstance().update(edgebox);
				logger.info( "Cassandra Configuration removed" );
			}
		} catch (Exception e) {
		}
	}

    /*private void migrateDataTypes() {
        Map<String, String> map = new LinkedHashMap<>();

        map.put("BOOLEAN", "java.lang.Boolean");
        map.put("COORDINATES", "java.lang.String");
        map.put("IMAGE", "java.lang.String");
        map.put("IMAGE_URL", "java.lang.String");
        map.put("NUMBER", "java.lang.BigDecimal");
        map.put("STRING", "java.lang.BigDecimal");
        map.put("TIMESTAMP", "java.lang.Long");
        map.put("XYZ", "java.lang.String");
        map.put("DATE", "java.util.Date");
        map.put("URL", "java.lang.String");
        map.put("ZPL_SCRIPT", "java.lang.String");
        map.put("GROUP", "com.tierconnect.riot.appcore.entities.Group");
        map.put("LOGICAL_READER", "com.tierconnect.riot.iot.entities.LogicalReader");
        map.put("SHIFT", "com.tierconnect.riot.iot.entities.Shift");
        map.put("ZONE", "com.tierconnect.riot.iot.entities.Zone");
        map.put("SEQUENCE", "java.lang.String");
        map.put("FORMULA", "java.lang.String");
        map.put("THING_TYPE", "com.tierconnect.riot.iot.entities.Thing");
        DataTypeService service = DataTypeService.getInstance();

        for (Map.Entry<String, String> entry : map.entrySet()) {
            DataType dataType = service.getDataTypeDAO().selectBy("code", entry.getKey());
            dataType.setClazz(entry.getValue());
        }

    }



*/

	public static void modifyAssociateDisassociateResource(){
		// get old resources
		List<Resource> resourceAssignList = ResourceService.getResourceDAO().selectAllBy("name", "reportDefinition_assignThing");
		Resource resourceAssign = resourceAssignList != null && !resourceAssignList.isEmpty() ? resourceAssignList.get(0): null;
		List<Resource> resourceUnAssignList = ResourceService.getResourceDAO().selectAllBy("name","reportDefinition_unAssignThing");
		Resource resourceUnAssign = (resourceUnAssignList == null)? null:(!resourceUnAssignList.isEmpty() ? resourceUnAssignList.get(0):null);

		if (resourceAssign != null && resourceUnAssign != null) {
			// create new resource
			Resource rsAssignUnAssignThing = null;
			List<Resource> resourceAssignUnAssignList = ResourceService.getResourceDAO().selectAllBy("name","reportDefinition_assignUnAssignThing");
			if(resourceAssignUnAssignList == null){
				rsAssignUnAssignThing = asignResource(resourceAssign);
			} else if(resourceAssignUnAssignList.isEmpty()){
				rsAssignUnAssignThing = asignResource(resourceAssign);
			} else {
				rsAssignUnAssignThing = new Resource();
				rsAssignUnAssignThing.setAcceptedAttributes("u");
				rsAssignUnAssignThing.setName("reportDefinition_assignUnAssignThing");
				rsAssignUnAssignThing.setDescription("Allow user to assign or un-assign a thing to other on report definition");
				rsAssignUnAssignThing.setFqname("com.tierconnect.riot.iot.entities.ReportDefinition");
				rsAssignUnAssignThing.setGroup(GroupService.getInstance().getRootGroup());
				rsAssignUnAssignThing.setLabel("Thing Associate & Disassociate");
				rsAssignUnAssignThing.setParent(resourceAssign.getParent());
				rsAssignUnAssignThing.setTreeLevel(3);
				rsAssignUnAssignThing.setType(4);
				ResourceService.getInstance().insert(rsAssignUnAssignThing);
			}
			List<Role> roles = RoleService.getRoleDAO().selectAll();
			for (Role role : roles) {
				Set<RoleResource> rolResources = role.getRoleResources();
				String permissions = null;
				boolean createRoleResource = false;
				List<RoleResource> roleResourceToRemove = new ArrayList<RoleResource>();
				for (RoleResource rolResource : rolResources) {
					Resource resource = rolResource.getResource();
					if (resource.getId().compareTo(resourceAssign.getId()) == 0 || resource.getId().compareTo(resourceUnAssign.getId()) == 0) {
						permissions = rolResource.getPermissions();
						createRoleResource = true;
						// remove old relation
						roleResourceToRemove.add(rolResource);
					}
				}
				// update roleResources
				if (createRoleResource){
					RoleResource roleResourceNew = new RoleResource();
					roleResourceNew.setPermissions(permissions);
					roleResourceNew.setResource(rsAssignUnAssignThing);
					roleResourceNew.setRole(role);
					RoleResourceService.getInstance().insert(roleResourceNew);
					rolResources.add(roleResourceNew);
				}
				if (!roleResourceToRemove.isEmpty()){
					for (RoleResource rolResource : roleResourceToRemove){
						rolResources.remove(rolResource);
						RoleResourceService.getRoleResourceDAO().delete(rolResource);
					}
				}
				role.setRoleResources(rolResources);
				RoleService.getRoleDAO().update(role);
			}
			// remove old resources
			ResourceService.getResourceDAO().delete(resourceAssign);
			ResourceService.getResourceDAO().delete(resourceUnAssign);
		}
	}

	private void updateZPLEThingType()
	{
		// Get Group level 3
		long groupId = 3;
		Group group = GroupService.getInstance().get(groupId);
		// If there not exist the group level 3 get the level 2
		if ( group == null )
		{
			groupId = 2;
			group = GroupService.getInstance().get(groupId);
			if (group != null)
			{
				groupId = 1;
				group = GroupService.getInstance().get(groupId);
			}
		}

		// if there not exist a group do nothing
		if ( group != null )
		{
			// Find thingtype ZPL default and update it
			ThingType thingTypeZPL = null;
			try {
				thingTypeZPL = ThingTypeService.getInstance().getByCode("default_zpl_thingtype");
				if (thingTypeZPL != null) {
					thingTypeZPL.setGroup(group);
					ThingTypeService.getInstance().update(thingTypeZPL);
				}
			} catch (NonUniqueResultException e) {
				e.printStackTrace();
			}
		}
	}

	private static Resource asignResource(Resource resourceAssign){
		Resource rsAssignUnAssignThing = new Resource();
		rsAssignUnAssignThing.setAcceptedAttributes("u");
		rsAssignUnAssignThing.setName("reportDefinition_assignUnAssignThing");
		rsAssignUnAssignThing.setDescription("Allow user to assign or un-assign a thing to other on report definition");
		rsAssignUnAssignThing.setFqname("com.tierconnect.riot.iot.entities.ReportDefinition");
		rsAssignUnAssignThing.setGroup(GroupService.getInstance().getRootGroup());
		rsAssignUnAssignThing.setLabel("Thing Associate & Disassociate");
		rsAssignUnAssignThing.setParent(resourceAssign.getParent());
		rsAssignUnAssignThing.setTreeLevel(3);
		rsAssignUnAssignThing.setType(4);
		ResourceService.getInstance().insert(rsAssignUnAssignThing);
		return rsAssignUnAssignThing;
	}
}
