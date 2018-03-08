package com.tierconnect.riot.iot.reports.views.things;

import com.tierconnect.riot.api.database.base.conditions.BooleanCondition;
import com.tierconnect.riot.api.database.base.operator.MultipleOperator;

import com.tierconnect.riot.iot.entities.ThingTypePath;
import com.tierconnect.riot.iot.reports.views.things.dto.Parameters;
import com.tierconnect.riot.iot.services.ThingTypePathService;
import com.tierconnect.riot.iot.services.ThingTypeService;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by julio.rocha on 07-07-17.
 * Thing List as Tree view.
 */
class ThingListAsTree extends ThingList {
    private MultipleOperator or;
    private List<ThingTypePath> thingTypePathList;
    private Long[] parents;

    ThingListAsTree(Parameters parameters) {
        super(parameters);
        parents = ThingTypeService.getThingTypeDAO().getAllParents();
        thingTypePathList = ThingTypePathService
                .getInstance()
                .getPathByThingTypeId(
                        parameters.getThingTypeId(),
                        this.visibilityPermission.getListGroupIds(),
                        parents);
    }

    @Override
    protected void buildPartialQuery() {
        buildSimpleQuery();
        buildTreeQuery();
    }

    /**
     * Method that add serial number or name to find operation and thingTypeCode.
     * example: (serialNumber = "textToFind" or textToFind = "textToFind") and (thingTypeId = 7)
     */
    private void buildSimpleQuery() {
        if (parameters.getThingId() != null ||
                thingConditionBuilder.isNotSerialOrNameEmpty(parameters.getSerialNumber(), parameters.getName())) {
            MultipleOperator multipleOperator = thingConditionBuilder.buildDefaultFilterConditionBuilder(parameters);
            if (!multipleOperator.getGenericOperatorList().isEmpty()) {
                or = new MultipleOperator(BooleanCondition.OR);
                or.addOperator(multipleOperator);
            }
        }
    }

    /**
     * Build all query combinations to the tree view.
     */
    private void buildTreeQuery() {
        if (parameters.getThingId() == null &&
                StringUtils.isBlank(parameters.getSerialNumber()) &&
                StringUtils.isBlank(parameters.getName())) {
            List<Long> longList = thingTypePathList
                    .stream()
                    .map(thingTypePath -> thingTypePath.getOriginThingType().getId())
                    .collect(Collectors.toList());
            if (parameters.getThingTypeId() != null) {
                longList.add(parameters.getThingTypeId());
            } else {
                longList.addAll(Arrays.asList(parents));
            }
            longList = longList.stream().distinct().collect(Collectors.toList());
            queryCondition = thingConditionBuilder.addThingTypeFilterIn(longList);
        } else {
            for (ThingTypePath thingTypePath : thingTypePathList) {
                MultipleOperator findSerial = thingConditionBuilder.buildDefaultFilterConditionBuilder(
                        parameters,
                        thingTypePath.getOriginThingType().getId(),
                        thingTypePath.getPath());
                or.addOperator(findSerial);
            }
            queryCondition = or;
        }
    }
}
