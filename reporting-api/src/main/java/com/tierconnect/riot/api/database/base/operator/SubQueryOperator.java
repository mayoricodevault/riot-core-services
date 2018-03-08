package com.tierconnect.riot.api.database.base.operator;

import com.tierconnect.riot.api.database.base.GenericOperator;
import com.tierconnect.riot.api.database.base.Operation;
import com.tierconnect.riot.api.database.base.conditions.ConditionBuilder;

import static com.tierconnect.riot.api.assertions.Assertions.voidNotNull;
import static com.tierconnect.riot.api.mongoShell.utils.CharacterUtils.betweenDoubleQuote;

/**
 * Created by julio.rocha on 09-02-17.
 * Class for create subQuery operations.
 */
public class SubQueryOperator implements GenericOperator {

    private final String key;
    private final ConditionBuilder condition;
    private final ConditionBuilder projectionFilter;
    private final String target;
    private final String fieldToProject;
    private final Operation.OperationEnum operator;

    public SubQueryOperator(String key, ConditionBuilder condition, String target, String fieldToProject, Operation.OperationEnum operator) {
        this(key, condition, target, fieldToProject, operator, null);
    }

    public SubQueryOperator(String key, ConditionBuilder condition, String target, String fieldToProject, Operation.OperationEnum operator, ConditionBuilder projectionFilter) {
        voidNotNull("key", key);
        voidNotNull("condition", condition);
        voidNotNull("target", target);
        voidNotNull("fieldToProject", fieldToProject);
        voidNotNull("operator", operator);

        this.key = key;
        this.condition = condition;
        this.target = target;
        this.fieldToProject = fieldToProject;
        this.operator = operator;
        this.projectionFilter = projectionFilter;
    }

    @Override
    public boolean isMultipleOperator() {
        return false;
    }

    public String getKey() {
        return key;
    }

    public String getKeyWithQuote() {
        return betweenDoubleQuote(key);
    }

    public ConditionBuilder getCondition() {
        return condition;
    }

    public String getTarget() {
        return target;
    }

    public String getFieldToProject() {
        return fieldToProject;
    }

    public Operation.OperationEnum getOperator() {
        return operator;
    }

    public ConditionBuilder getProjectionFilter() {
        return projectionFilter;
    }
}
