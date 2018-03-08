package com.tierconnect.riot.iot.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.tierconnect.riot.appcore.entities.Connection;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.QConnection;
import com.tierconnect.riot.appcore.services.ConnectionService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.utils.QueryUtils;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.iot.dao.EdgeboxRuleDAO;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.sdk.dao.Pagination;
import com.tierconnect.riot.sdk.dao.UserException;

import java.util.*;

/**
 * Created by cfernandez 12/4/2014.
 */
public class EdgeboxRuleService extends EdgeboxRuleServiceBase
{
	ValidationBean validationBean = new ValidationBean();
	static private EdgeboxRuleDAO _edgeboxRuleDAO;
	public static EdgeboxRuleDAO getEdgeBoxRuleDAO()
	{
		if( _edgeboxRuleDAO == null )
		{
			_edgeboxRuleDAO = new EdgeboxRuleDAO();
		}
		return _edgeboxRuleDAO;
	}
	public void refreshConfiguration( Edgebox bridge, String data, boolean publishMessage )
	{
		BrokerClientHelper.sendRefreshEdgeboxRule( bridge.getCode(), data, publishMessage,
				GroupService.getInstance().getMqttGroups(bridge.getGroup()));
	}
	
	public void refreshEdgeboxRuleCache( EdgeboxRule edgeboxRule, boolean delete )
	{
		BrokerClientHelper.refreshEdgeboxRuleCache( edgeboxRule, delete );
	}


	public List<EdgeboxRule> selectByCodeAndAction(String bridgeCode, String action){
		return getEdgeboxRuleDAO()
				.selectAllBy(QEdgeboxRule.edgeboxRule.edgebox.code.eq(bridgeCode)
				.and(QEdgeboxRule.edgeboxRule.output.eq(action)));
	}

	public List<EdgeboxRule> selectByAction(String action){
		return getEdgeboxRuleDAO()
				.selectAllBy(QEdgeboxRule.edgeboxRule.output.eq(action));
	}


	public List<EdgeboxRule> selectByEdgeboxId(Long edgeboxId){
		return getEdgeboxRuleDAO().selectAllBy("edgebox.id", edgeboxId);

	}

	public List<EdgeboxRule> selectByScheduledRuleId(Long scheduledRuleId){
		return getEdgeboxRuleDAO().selectAllBy("scheduledRule.id", scheduledRuleId);

	}

	public Boolean validateConnectionCode( EdgeboxRule edgeboxRule )
	{
		boolean validation = false;

		if ( edgeboxRule.getOutputConfig().contains( "connectionCode" )) {
			ObjectMapper mapper = new ObjectMapper();
			Map<String, String> mapResponse;
			try {
				mapResponse = mapper.readValue(edgeboxRule.getOutputConfig(), Map.class);
				String connectionCode = mapResponse.get("connectionCode");
				Connection connection = ConnectionService.getInstance().getConnectionDAO().selectBy(QConnection.connection.code.eq(connectionCode));
				if (connection == null)
				{
					validationBean.setErrorDescription("There is not exist the connectionCode: '" + connectionCode + "'");
					validation = true;
				}
			} catch (Exception e) {

			}
		}
		return validation;
	}

	@Override
	public EdgeboxRule update( EdgeboxRule edgeboxRule )
	{
		if ( validateConnectionCode(edgeboxRule) )
			throw new UserException(validationBean.getErrorDescription());
		else
		{
			validateUpdate( edgeboxRule );
			getEdgeboxRuleDAO().update( edgeboxRule );
			return edgeboxRule;
		}
	}

	@Override
	public EdgeboxRule insert( EdgeboxRule edgeboxRule )
	{
		if ( validateConnectionCode(edgeboxRule) )
			throw new UserException(validationBean.getErrorDescription());
		else {
			validateInsert(edgeboxRule);
			Long id = getEdgeboxRuleDAO().insert(edgeboxRule);
			edgeboxRule.setId(id);
			updateOrderRules(edgeboxRule.getEdgebox(), edgeboxRule, getParameterFromConditionTypeGroup("conditionTypes", edgeboxRule.getConditionType(), "key"));
			return edgeboxRule;
		}
	}

	/**
	 * Get name of rules using a thing type or property thing type
	 * @param name Name of thing type or property thing type
	 * @return
     */
	public String geRulesUsingThingTypeOrProperty(String name)
	{
		Set<String> result = new HashSet<>();
		String nameFinal = "'"+name+"'";
		List<EdgeboxRule> lstOutputConfig = EdgeboxRuleService.getEdgeboxRuleDAO().getQuery()
				.where(QEdgeboxRule.edgeboxRule.outputConfig.contains(nameFinal)
						.or(QEdgeboxRule.edgeboxRule.rule.contains(nameFinal)))
				.list(QEdgeboxRule.edgeboxRule);
		if(lstOutputConfig!=null && lstOutputConfig.size()>0)
		{
			for (EdgeboxRule edgeboxRule :lstOutputConfig) {
				result.add(edgeboxRule.getEdgebox().getName());
			}
		}
		return String.join(",", result);
	}

	/**
	 * Get property of rules using a property thing type
	 * @param property property property
	 * @param thingTypeCode thingTypeCode
	 * @return property of rules
     */
	public String getRulesUsingPropertyAndThingType(String property, String thingTypeCode) {
		Set<String> result = new HashSet<>();
		property = "'" + property + "'";
		thingTypeCode = "'" + thingTypeCode + "'";
		List<EdgeboxRule> edgeboxRuleList = EdgeboxRuleService.getEdgeboxRuleDAO().getQuery()
				.where(QEdgeboxRule.edgeboxRule.outputConfig.contains(property)
						.or(QEdgeboxRule.edgeboxRule.rule.contains(property)))
				.list(QEdgeboxRule.edgeboxRule);
		if((edgeboxRuleList != null) && !edgeboxRuleList.isEmpty()) {
			for (EdgeboxRule edgeboxRule :edgeboxRuleList) {
				String rule = edgeboxRule.getRule();
				if (!rule.contains("'thingTypeCode'") || (rule.contains("'thingTypeCode'") && rule.contains(thingTypeCode))) {
					result.add(edgeboxRule.getEdgebox().getName());
				}
			}
		}
		return String.join(",", result);
	}

	/**
	 *
	 * @param edgeboxName edgebox's name
	 * @return an EdgeboxRule List
     */
	public List<EdgeboxRule> selectByEdgeboxName(String edgeboxName, Group group, Edgebox edgeBox){
		HibernateQuery query = getEdgeBoxRuleDAO().getQuery();
		return query.where(
				QEdgeboxRule.edgeboxRule.name.eq(edgeboxName)
						.and(QEdgeboxRule.edgeboxRule.group.eq(group)
						.and(QEdgeboxRule.edgeboxRule.edgebox.eq(edgeBox))))
				.list(QEdgeboxRule.edgeboxRule);
	}

	@Override public void validateInsert(EdgeboxRule edgeboxRule) {
		super.validateInsert(edgeboxRule);
		BooleanBuilder be = new BooleanBuilder();
		be = be.and(QEdgeboxRule.edgeboxRule.name.eq(edgeboxRule.getName()));
		be = be.and(QEdgeboxRule.edgeboxRule.edgebox.eq(edgeboxRule.getEdgebox()));
		if (getEdgeBoxRuleDAO().selectBy(be) != null) {
			throw new UserException("Edgebox rule already exists");
		}
	}

	@Override public void validateUpdate(EdgeboxRule edgeboxRule) {
		super.validateUpdate(edgeboxRule);
		BooleanBuilder be = new BooleanBuilder();
		be = be.and(QEdgeboxRule.edgeboxRule.name.eq(edgeboxRule.getName()));
		be = be.and(QEdgeboxRule.edgeboxRule.edgebox.eq(edgeboxRule.getEdgebox()));
		if (edgeboxRule.getId() == null && getEdgeBoxRuleDAO().selectBy(be) != null) {
			throw new UserException("Edgebox rule already exists");
		}
	}

	public List<ObjectNode> getRuleGroupParameters() {

		ObjectMapper mapper = new ObjectMapper();
		List<ObjectNode> list = new ArrayList<ObjectNode>();

		//VIZIX_CEP
		ObjectNode vizix_cep = mapper.createObjectNode();
		vizix_cep.put("key", Constants.CONDITION_TYPE_GROUP_VIZIX_CEP);
		vizix_cep.put("where","conditionType="+ Constants.CONDITION_TYPE_CEP);

		ArrayNode conditionsCEP = mapper.createArrayNode();
		conditionsCEP.add(Constants.CONDITION_TYPE_CEP);
		vizix_cep.put("conditionTypes", conditionsCEP);

		list.add(vizix_cep);

		//VIZIX_FUNCTION
		ObjectNode vizix_function = mapper.createObjectNode();
		vizix_function.put("key", Constants.CONDITION_TYPE_GROUP_VIZIX_FUNCTIONS);
		vizix_function.put("where", "conditionType="+ Constants.CONDITION_TYPE_JS + "|conditionType="+ Constants.CONDITION_TYPE_ALWAYS_TRUE);

		ArrayNode conditionsJS = mapper.createArrayNode();
		conditionsJS.add(Constants.CONDITION_TYPE_ALWAYS_TRUE);
		conditionsJS.add(Constants.CONDITION_TYPE_JS);
		vizix_function.put("conditionTypes", conditionsJS);

		list.add(vizix_function);

		return list;
	}

	private List<String> getConditionValuesByGroup(String key) {
		List<String> response = null;

		if (key == Constants.CONDITION_TYPE_GROUP_VIZIX_CEP) {
			response = Arrays.asList(Constants.CONDITION_TYPE_CEP);
		}
		if (key == Constants.CONDITION_TYPE_GROUP_VIZIX_FUNCTIONS) {
			response = Arrays.asList(Constants.CONDITION_TYPE_JS, Constants.CONDITION_TYPE_ALWAYS_TRUE);
		}

		return response;
	}

	private String getParameterFromConditionTypeGroup(String key, String value, String parameter) {
		List<ObjectNode> conditionTypeGroups = getRuleGroupParameters();

		if (parameter.equals("conditionTypes") || (!parameter.equals("key") && !parameter.equals("where"))) {
			return null;
		}

		for (ObjectNode groupParameter : conditionTypeGroups) {
			if (key == "conditionTypes") {
				ArrayNode conditions = (ArrayNode) groupParameter.get("conditionTypes");
				for (int i = 0; i < conditions.size(); i++) {
					if (value.equals(conditions.get(i).textValue())) {
						String response = null;
						if (parameter == "where") {
							response = groupParameter.get("where").textValue();
						}
						if (parameter == "key") {
							response = groupParameter.get("key").textValue();
						}
						return response;
					}
				}
			} else if (value.equals(groupParameter.get(key).textValue())) {
				return groupParameter.get(parameter).textValue();
			}
		}

		return null;
	}

	public void updateOrderByConditionType(EdgeboxRule edgeboxRule, String lastConditionType) {
		// Don't validate if a scheduled rule is related to ScheduledRule
		ScheduledRule scheduledRule = edgeboxRule.getScheduledRule();
		if (scheduledRule != null){
			return;
		}

		String newConditionTypeGroup = getParameterFromConditionTypeGroup("conditionTypes", edgeboxRule.getConditionType(), "key");
		String lastConditionTypeGroup = getParameterFromConditionTypeGroup("conditionTypes", lastConditionType, "key");

		if (!lastConditionTypeGroup.equals(newConditionTypeGroup)) {
			updateOrderRules(edgeboxRule.getEdgebox(), edgeboxRule, newConditionTypeGroup);
		}
	}

	public Map<String, Object> bulkUpdateOrderRule(Map<String, Object> map) {
		Map<String, Object> response = new  HashMap<String, Object>();
		List<String> groupParameters = Arrays.asList(Constants.CONDITION_TYPE_GROUP_VIZIX_CEP, Constants.CONDITION_TYPE_GROUP_VIZIX_FUNCTIONS);
		for (Map.Entry<String, Object> entry : map.entrySet())
		{
			String selectedGroup = entry.getKey();
			if (groupParameters.indexOf(selectedGroup) > -1) {
				List <String> listIds = (List) entry.getValue();
				List <String> validConditions = getConditionValuesByGroup(selectedGroup);
				String whereCondition = getParameterFromConditionTypeGroup("key", selectedGroup, "where");

				if (listIds == null) {
					throw new UserException("List of ["+ selectedGroup +"] is invalid.");
				}

				if (listIds.size() > 0) {
					EdgeboxRule rule;
					Edgebox validEdgebox = null;
					for (int i = 0; i < listIds.size(); i++)
					{
						rule = EdgeboxRuleService.getInstance().get(Long.parseLong(String.valueOf(listIds.get(i))));
						if (rule == null) {
							throw new UserException( "Edgebox Rule id ["+ String.valueOf(listIds.get(i)) +"] not found." );
						}
						if (validEdgebox == null) {
							validEdgebox = rule.getEdgebox();
						}

						//validate if rule is in the correct edgebox or has the correct conditionType
						if (validEdgebox.getId().longValue() != rule.getEdgebox().getId().longValue() || validConditions.indexOf(rule.getConditionType()) < 0) {
							throw new UserException( "Edgebox Rule id ["+ String.valueOf(listIds.get(i)) +"] is not valid for this list." );
						}
					}
					BooleanBuilder be = new BooleanBuilder();
					be = be.and( QueryUtils.buildSearch( QEdgeboxRule.edgeboxRule, whereCondition ) );
					be = be.and( QueryUtils.buildSearch( QEdgeboxRule.edgeboxRule, "edgebox.id=" + Long.toString( validEdgebox.getId().longValue() ) ) );
					Long countRules = EdgeboxRuleService.getInstance().countList( be);

					if (listIds.size() != countRules) {
						throw new UserException( "ConditionType Group ["+ selectedGroup +"] doesn't contains all its rules." );
					}
				} else {
					throw new UserException( "ConditionType Group ["+ selectedGroup +"] can't has a empty list." );
				}
			} else {
				throw new UserException( "ConditionType Group ["+ entry.getKey() +"] is not valid." );
			}
		}

		for (Map.Entry<String, Object> entry : map.entrySet())
		{
			List <String> listIds = (List) entry.getValue();
			EdgeboxRule rule;
			for (int i = 0; i < listIds.size(); i++)
			{
				rule = EdgeboxRuleService.getInstance().get(Long.parseLong(String.valueOf(listIds.get(i))));
				rule.setSortOrder(i);
			}
		}

		response.put("message", "Order updated successfully.");
		return response;
	}

	public void updateOrderRules(Edgebox edgebox, EdgeboxRule edgeboxRuleUpdated, String conditionTypeGroup) {
		if (edgebox != null) {
			List<ObjectNode> conditionTypeGroups = getRuleGroupParameters();
			Pagination pagination = new Pagination( 1, -1 );
			for (ObjectNode groupParameter : conditionTypeGroups) {
				String order = "sortOrder:asc";
				BooleanBuilder be = new BooleanBuilder();
				Boolean updateRule = false;
				be = be.and( QueryUtils.buildSearch( QEdgeboxRule.edgeboxRule, groupParameter.get("where").textValue() ) );
				be = be.and( QueryUtils.buildSearch( QEdgeboxRule.edgeboxRule, "edgebox.id=" + Long.toString( edgebox.getId().longValue() ) ) );

				if (edgeboxRuleUpdated != null && conditionTypeGroup != null && conditionTypeGroup.equals(groupParameter.get("key").textValue())) {
					be = be.and( QueryUtils.buildSearch( QEdgeboxRule.edgeboxRule, "!(id=" + Long.toString( edgeboxRuleUpdated.getId().longValue() ) +")" ) );
					updateRule = true;
				}

				List<EdgeboxRule> ruleList = EdgeboxRuleService.getInstance().listPaginated( be, pagination, order);

				if (ruleList.size() > 0) {
					int count = 0;
					for (EdgeboxRule rule : ruleList) {
						rule.setSortOrder(count);
						update(rule);
						count = count + 1;
					}
				}
				if (updateRule) {
					edgeboxRuleUpdated.setSortOrder(ruleList.size());
					update(edgeboxRuleUpdated);
				}
			}
		}
	}

    /**
     * Generate the condition for scheduled rule based on the ReportDefinition selected
     * @param edgeboxRule
     * @return
     */
    public String createConditionForRuleScheduled(EdgeboxRule edgeboxRule){
        ScheduledRule scheduledRule = edgeboxRule.getScheduledRule();
        if (scheduledRule != null){
            ReportDefinition reportDefinition = scheduledRule.getReportDefinition();
            if (reportDefinition != null){
                return "select * from messageEventType where ( function condition(thingWrapper,things,messages,logger){\n" +
                        "\tvar sourceUdf = thingWrapper.getUdf(\"source\");\n" +
                        "\tvar result = sourceUdf === \"REP_" + reportDefinition.getId() + "\";\n" +
                        "\tlogger.info(\"JS condition source=\"+sourceUdf+\" result=\"+result);\n" +
                        "\treturn result;\n" +
                        "} )";
            }
        }
        return null;
    }

	public static List<EdgeboxRule> getEdgeBoxRulesByScheduledRule( Long id )
	{
		HibernateQuery query = EdgeboxRuleService.getEdgeboxRuleDAO().getQuery();
		return query.where( QEdgeboxRule.edgeboxRule.scheduledRule.id.eq( id ) ).list( QEdgeboxRule.edgeboxRule );
	}


	/**
	 * Set default values
	 * @param rule Edge box rule object
     */
	public void setDefaultValues(EdgeboxRule rule) {
		if( rule.getSerialExecution() == null ){
			rule.setSerialExecution(false);
		}
	}
}
