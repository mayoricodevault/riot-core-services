package com.tierconnect.riot.iot.reports.views.things;

import com.tierconnect.riot.api.database.base.GenericOperator;
import com.tierconnect.riot.api.database.base.Operation;
import com.tierconnect.riot.api.database.base.conditions.BooleanCondition;
import com.tierconnect.riot.api.database.base.operator.MultipleOperator;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.iot.entities.ThingType;

import com.tierconnect.riot.iot.reports.views.things.dto.Parameters;
import com.tierconnect.riot.iot.reports.views.things.dto.VisibilityPermission;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tierconnect.riot.commons.Constants.REGEX_CASE_INSENSITIVE;
import static com.tierconnect.riot.commons.Constants.REGEX_CASE_SENSITIVE;

/**
 * Created by julio.rocha on 07-07-17.
 * Class for create the default query builder for list things.
 */
class ThingConditionBuilder {
    private static ThingConditionBuilder INSTANCE = new ThingConditionBuilder();

    private ThingConditionBuilder() {
    }

    public static ThingConditionBuilder getInstance() {
        return INSTANCE;
    }


     VisibilityPermission getThingListAndGroupIdList(Map<Long, List<ThingType>> groups,
                                                           String upVisibility,
                                                           String downVisibility) {
        List<Long> lstGroupIds = new ArrayList<>();
        List<Long> lstThingTypeIds = new ArrayList<>();
        if ((upVisibility != null && upVisibility.equals("false")) &&
                (downVisibility != null && downVisibility.equals("false"))) {
            List<Long> lstGroupIdsCollector = new ArrayList<>();
            List<Long> ttList = groups.entrySet()
                    .parallelStream()
                    .map(e -> {
                        lstGroupIdsCollector.add(e.getKey());
                        return e.getValue();
                    })
                    .flatMap(List::parallelStream)
                    .map(ThingType::getId)
                    .collect(Collectors.toList());
            lstThingTypeIds.addAll(ttList);
            lstGroupIds.addAll(lstGroupIdsCollector);
        } else if ((upVisibility != null && upVisibility.equals("false")) &&
                (downVisibility != null && (downVisibility.equals("") || downVisibility.equals("true")))) {
            for (Long groupId : groups.keySet()) {
                lstGroupIds = GroupService.getInstance().getListGroupIdsChildren(groupId);
                lstThingTypeIds = getThingTypes(groups.get(groupId));
            }
        } else if ((downVisibility != null && downVisibility.equals("false")) &&
                (upVisibility != null && (upVisibility.equals("") || upVisibility.equals("true")))) {
            //TODO: This part of code is going to change when upVisibility will be available
            List<Long> lstGroupIdsCollector = new ArrayList<>();
            List<Long> ttList = groups.entrySet()
                    .parallelStream()
                    .map(e -> {
                        lstGroupIdsCollector.add(e.getKey());
                        return e.getValue();
                    })
                    .flatMap(List::parallelStream)
                    .map(ThingType::getId)
                    .collect(Collectors.toList());
            lstThingTypeIds.addAll(ttList);
            lstGroupIds.addAll(lstGroupIdsCollector);
        } else {
            //By default, children are added to the list
            for (Long groupId : groups.keySet()) {
                lstGroupIds = GroupService.getInstance().getListGroupIdsChildren(groupId);
                lstThingTypeIds = getThingTypes(groups.get(groupId));
            }
        }
        return new VisibilityPermission(lstGroupIds, lstThingTypeIds);
    }


    GenericOperator addGroupIdFilterConditionBuilder(VisibilityPermission visibilityPermission) {
        MultipleOperator andFilter = new MultipleOperator(BooleanCondition.AND);
        if (visibilityPermission.getListGroupIds() != null && visibilityPermission.getListGroupIds().size() > 0) {
            andFilter.addOperator(Operation.in("groupId", visibilityPermission.getListGroupIds()));
        }
        if (visibilityPermission.getListThingTypeIds() != null && visibilityPermission.getListThingTypeIds().size() > 0) {
            andFilter.addOperator(Operation.in("thingTypeId", visibilityPermission.getListThingTypeIds()));
        }
        return andFilter;
    }

    private List<Long> getThingTypes(List<ThingType> types) {
        return types.parallelStream().map(ThingType::getId).collect(Collectors.toList());
    }

    MultipleOperator buildDefaultFilterConditionBuilder(Parameters parameters) {
        return buildDefaultFilterConditionBuilder(parameters, parameters.getThingTypeId(), "");
    }

    MultipleOperator buildDefaultFilterConditionBuilder(Parameters parameters, Long thingTypeId, String prefix) {
        MultipleOperator queryCondition = new MultipleOperator(BooleanCondition.AND);
        addThingTypeFilter(thingTypeId, queryCondition);
        addThingIdFilter(parameters.getThingId(), queryCondition);
        addSerialOrNameFilter(parameters, queryCondition, prefix);
        return queryCondition;
    }

    private GenericOperator buildQueryBySerialNumberOrNameConditionBuilder(String value, String prefix) {
        MultipleOperator or = new MultipleOperator(BooleanCondition.OR);
        prefix = (StringUtils.isNotBlank(prefix)) ? prefix + "." : StringUtils.EMPTY;
        or.addOperator(Operation.regex(prefix + "serialNumber", value.toUpperCase(), REGEX_CASE_SENSITIVE));
        or.addOperator(Operation.regex(prefix + "name", value.toUpperCase(), REGEX_CASE_INSENSITIVE));
        return or;
    }

    GenericOperator addThingTypeFilterIn(List<Long> thingTypeIds) {
        return Operation.in("thingTypeId", thingTypeIds);
    }

    private void addThingTypeFilter(Long thingTypeId, MultipleOperator queryCondition) {
        if (thingTypeId != null) {
            queryCondition.addOperator(Operation.equals("thingTypeId", thingTypeId));
        }
    }

    private void addThingIdFilter(Long thingId, MultipleOperator queryCondition) {
        if (thingId != null) {
            queryCondition.addOperator(Operation.equals("_id", thingId));
        }
    }

    private void addSerialOrNameFilter(Parameters parameters, MultipleOperator queryCondition, String prefix) {
        String serialNumber = parameters.getSerialNumber();
        String name = parameters.getName();
        if (isNotSerialOrNameEmpty(serialNumber, name)) {
            addEqualsFilter(parameters.isEqualsSerialNumber(), prefix, "serialNumber", serialNumber, queryCondition);
            addEqualsFilter(parameters.isEqualsName(), prefix, "name", name, queryCondition);
            addRegexFilter(parameters, queryCondition, serialNumber, name, prefix);
        }
    }

    boolean isNotSerialOrNameEmpty(String serialNumber, String name) {
        return StringUtils.isNotEmpty(serialNumber) || StringUtils.isNotEmpty(name);
    }

    private void addEqualsFilter(boolean isEquals, String prefix, String fieldName, String value, MultipleOperator queryCondition) {
        if (isEquals) {
            prefix = (StringUtils.isNotBlank(prefix)) ? prefix + "." : StringUtils.EMPTY;
            queryCondition.addOperator(Operation.equals(prefix + fieldName, value));
        }
    }

    private void addRegexFilter(Parameters parameters,
                                MultipleOperator queryCondition,
                                String serialNumber,
                                String name,
                                String prefix) {
        if (!parameters.isEqualsSerialNumber() && !parameters.isEqualsName()) {
            queryCondition.addOperator(buildQueryBySerialNumberOrNameConditionBuilder(getFilterValue(serialNumber, name), prefix));
        }
    }

    private String getFilterValue(String serialNumber, String name) {
        return StringUtils.isNotEmpty(serialNumber) ? serialNumber : name;
    }
}
