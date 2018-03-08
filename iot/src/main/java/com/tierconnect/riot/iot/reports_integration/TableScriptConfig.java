package com.tierconnect.riot.iot.reports_integration;

import com.tierconnect.riot.api.database.base.GenericOperator;
import com.tierconnect.riot.api.database.base.Operation;
import com.tierconnect.riot.api.database.base.conditions.BooleanCondition;
import com.tierconnect.riot.api.database.base.conditions.ConditionBuilder;
import com.tierconnect.riot.api.database.base.operator.MultipleOperator;
import com.tierconnect.riot.api.database.base.operator.SingleOperator;
import com.tierconnect.riot.api.database.exception.ValueNotPermittedException;
import com.tierconnect.riot.api.database.mongo.Mongo;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.commons.utils.DateHelper;
import com.tierconnect.riot.iot.entities.ReportDefinition;
import com.tierconnect.riot.iot.entities.ReportFilter;
import com.tierconnect.riot.iot.services.ThingTypeService;
import com.tierconnect.riot.iot.utils.VisibilityThingUtils;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;

import java.util.*;

import static com.tierconnect.riot.api.database.base.Operation.between;
import static com.tierconnect.riot.api.database.base.Operation.greaterThanOrEquals;
import static com.tierconnect.riot.api.database.base.Operation.lessThanOrEquals;
import static com.tierconnect.riot.appcore.utils.Utilities.isEmptyOrNull;
import static com.tierconnect.riot.commons.Constants.*;

/**
 * Created by ybarriga on 3/22/17.
 */
public class TableScriptConfig extends ReportConfig {
    User user;
    Map<String, Map<String, Object>> filterMap = new HashMap<>();
    private static Logger logger = Logger.getLogger(SummaryReportConfig.class);

    public TableScriptConfig(ReportDefinition reportDefinition, Map<String, Object> dynamicFilters,
                             Integer pageNum, Integer pageSize, Date startDate, Date endDate,
                             Date now, boolean addNonUdfInProperties, User user) {
        super(reportDefinition, dynamicFilters, pageNum, pageSize, startDate, endDate, addNonUdfInProperties
                , reportDefinition.getReportType(), false);
        this.user = user;
        processConfiguration(now);
        updateFilterValues(dynamicFilters);
    }

    @Override
    protected void processConfiguration(Date now) {
        this.filterMap.clear();
        processFilters(now, dynamicFilters);
        parseDateFilters();
        parseSystemInformation();

    }
    protected void parseSystemInformation(){
        Map<String, Object> stringObjectMap = new HashMap<>();
        Map<String, Object> publicMapUser = new HashMap<>();
        Map<String, Object> publicMapGroup = new HashMap<>();

        publicMapUser.put("id", user.getId());
        publicMapUser.put("archived", user.isArchived());
        publicMapUser.put("username", user.getUsername());
        publicMapUser.put("firstName", user.getFirstName());
        publicMapUser.put("lastName", user.getLastName());
        publicMapUser.put("email", user.getEmail());
        publicMapUser.put("timeZone", user.getTimeZone());
        publicMapUser.put("dateFormat", user.getDateFormat());
        publicMapUser.remove("userRoles");

        publicMapGroup.put( "id", this.user.getActiveGroup().getId() );
        publicMapGroup.put( "archived", this.user.getActiveGroup().isArchived() );
        publicMapGroup.put( "name", this.user.getActiveGroup().getName());
        publicMapGroup.put( "code", this.user.getActiveGroup().getCode() );
        publicMapGroup.put( "description", this.user.getActiveGroup().getDescription() );
        publicMapGroup.put( "treeLevel", this.user.getActiveGroup().getTreeLevel() );
        publicMapGroup.put( "hierarchyName", this.user.getActiveGroup().getHierarchyName() );

        stringObjectMap.put("user", publicMapUser);
        stringObjectMap.put("group", publicMapGroup);
        filterMap.put("systemInformation", stringObjectMap);
    }

    @Override
    protected void processFilters(Date now, Map<String, Object> dynamicFiltersBody) {
        Long groupId = null;
        String groupLabel = null;
        MultipleOperator multipleOperatorParent = new MultipleOperator(BooleanCondition.AND);
        for (ReportFilter reportFilter : reportDefinition.getReportFilterOrderByDisplayOrder()) {
            Object value = reportFilter.getValueByLabel(dynamicFiltersBody);
            if (value != null) {
                if (THING_TYPE_PROPERTY_ID.equals(reportFilter.getPropertyName())) {
                    thingType = null;
                    if (!isEmptyOrNull((String) value) && StringUtils.isNumeric((String) value)) {
                        thingType = ThingTypeService.getInstance().get(Long.parseLong((String) value));
                    }
                    if ("==".equals(reportFilter.getOperator())) {
                            multipleOperatorParent.addOperator(
                                    Operation.emptyArray(verifiedSnapshotsProperty(CHILDREN)));
                            multipleOperatorParent.addOperator(
                                    Operation.notExists(verifiedSnapshotsProperty(PARENT)));
                    }
                    if (thingType != null) {
                        multipleOperatorParent.addOperator(Operation.equals(verifiedSnapshotsProperty(THING_TYPE_TRANSLATE_ID), thingType.getId()));
                        addFilterOperator(reportFilter.getLabel(), multipleOperatorParent);
                    }
                } else if (GROUP_PROPERTY_ID.equals(reportFilter.getPropertyName())
                        && !isEmptyOrNull((String) value)
                        && StringUtils.isNumeric((String) value)) {
                    groupId = Long.valueOf(value.toString());
                    groupLabel = reportFilter.getLabel();
                } else if (!ignoreFilters.contains(reportFilter.getPropertyName())) {
                    //this case for Empty values
                    if (isEmptyOrNull(value.toString()) && !OP_IS_EMPTY.equals(reportFilter.getOperator())
                            && !OP_IS_NOT_EMPTY.equals(reportFilter.getOperator())) {
                        continue;
                    }
                    //this case is just for zone
                    addFilersNotIgnored(reportFilter, value, now);
                }
            }
        }
        getGroupVisibilityFilter(groupId, groupLabel);
    }


    @Override
    public Map<String, Object> exportResult(Long total, List<Map<String, Object>> records) {
        return null;
    }

    @Override
    protected void addFilter(String label, String property, String operator, String original, Object value,
                             String childProperty, boolean timeSeries) throws ValueNotPermittedException {
        if (label != null){
            super.addFilter(label, property, operator, original, value, childProperty, timeSeries);
            Map<String, Object> stringObjectMap = filterMap.get(label);
            if (stringObjectMap == null) {
                stringObjectMap = new HashMap<>(3);
            }
            stringObjectMap.put("value", value);
            stringObjectMap.put("operator", operator);
            filterMap.put(label, stringObjectMap);
        }
    }

    @Override
    protected void addFilterOperator(String label, GenericOperator operation) {
        if (label != null){
            String operationQuery = null;
            super.addFilterOperator(label, operation);
            Map<String, Object> stringObjectMap = filterMap.get(label);
            if (stringObjectMap == null) {
                stringObjectMap = new HashMap<>(3);
            }
            Mongo mongo = new Mongo(new ConditionBuilder());
            if (operation instanceof SingleOperator){
                SingleOperator operationSingle = (SingleOperator) operation;
                try {
                    operationQuery = mongo.transformSingleOperator(operationSingle);
                } catch (Exception e){
                    logger.error("Error when trying to cast operation variable in SingleOperator", e);
                }
            } else {
                MultipleOperator operationMultiple = (MultipleOperator) operation;
                try {
                    //List<String> operationQuery1 = mongo.transformMultiOperatorList(((MultipleOperator) operation).getGenericOperatorList());
                    // operationQuery = operationQuery1.toString();
                    operationQuery = mongo.transformMultiOperator(operationMultiple);
                } catch (Exception e){
                    logger.error("Error when trying to cast operation variable in MultipleOperator", e);
                }
            }
            JSONParser parser = new JSONParser();
            JSONObject json = null;
            if ((operationQuery != null) && (!operationQuery.isEmpty())){
                try {
                    if (operationQuery.contains("ISODate")) {
                        operationQuery = operationQuery.replace("ISODate(\"", "\"--ISODate('").replace("\")", "')--\"");
                    }
                    json = (JSONObject) parser.parse(operationQuery);
                } catch (Exception e){
                    logger.error ("Error parsing operationQuery parameter", e);
                }
            }
            if (json != null){
                stringObjectMap.put("query", json);
            } else {
                stringObjectMap.put("query", operationQuery);
            }
            filterMap.put(label, stringObjectMap);
        }

    }

    private void updateFilterValues (Map<String, Object> dynamicFilters){
        if ((dynamicFilters != null) && (dynamicFilters.size() > 0)){
            updateFilterValuesDynamics(dynamicFilters);
        } else {
            updateFilterValuesReportFilter();
        }

    }
    private void updateFilterValuesDynamics(Map<String, Object> dynamicFilters){
        for (ReportFilter reportFilter: reportDefinition.getReportFilterOrderByDisplayOrder()){
            Object value = reportFilter.getValueByLabel(dynamicFilters);
            updateFilterMap( filterMap.get(reportFilter.getLabel()), value, reportFilter);
        }
    }

    private void updateFilterValuesReportFilter(){
        for (ReportFilter reportFilter: reportDefinition.getReportFilterOrderByDisplayOrder()){
            updateFilterMap(filterMap.get(reportFilter.getLabel()), reportFilter.getValue(), reportFilter);
        }
    }
    private void updateFilterMap( Map<String, Object> stringObjectMap, Object value, ReportFilter reportFilter){
        if (reportFilter.getLabel() != null){
            if (stringObjectMap == null) {
                stringObjectMap = new HashMap<>(3);
            }
            stringObjectMap.put("value", value);
            stringObjectMap.put("operator", reportFilter.getOperator());
            filterMap.put(reportFilter.getLabel(), stringObjectMap);
        }
    }

    protected void getGroupVisibilityFilter(Long reportGroupId, String label) throws ValueNotPermittedException {
        Map<Long, List<Long>> groups =
                VisibilityThingUtils.calculateVisibilityThingsID(reportGroupId);
        if ((groups == null) || groups.isEmpty()) {
            throw new UserException("User does not have permissions to see any Group, check User's roles");
        }
        List<Long> groupIds = new ArrayList<>();
        for (Long groupId : groups.keySet()) {
            groupIds.addAll(GroupService.getInstance().getListGroupIdsChildren(groupId));
        }
        String groupId = isHistoricalReport() ? GROUP_TS_TRANSLATE : GROUP_TRANSLATE_ID;
        if (!groupIds.isEmpty()) {
            if (groupIds.size() > 1) {
                addFilterOperator(label, Operation.in(groupId, groupIds));
            } else {
                addFilterOperator(label, Operation.equals(groupId, groupIds.get(0)));
            }
        }
    }

    protected void parseDateFilters() throws ValueNotPermittedException {
        GenericOperator genericOperator;
        Date now = new Date();
        if (startDate != null && endDate != null) {
            genericOperator = between(TIME, startDate, endDate);
        } else if (startDate != null) {
            genericOperator = greaterThanOrEquals(TIME, startDate);
        } else if (endDate != null) {
            genericOperator = lessThanOrEquals(TIME, endDate);
        } else {
            genericOperator = lessThanOrEquals(TIME, now);
        }
        addFilterOperator ("Date", genericOperator);
        Map<String, Object> stringObjectMap = filterMap.get("Date");
        stringObjectMap.put("endDate", DateHelper.dateToISODate(endDate));
        stringObjectMap.put("now",  DateHelper.dateToISODate(now));
        stringObjectMap.put("startDate",  DateHelper.dateToISODate(startDate));
        filterMap.put("Date", stringObjectMap);
    }

}
