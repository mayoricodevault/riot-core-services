package com.tierconnect.riot.api.database.base.conditions;

import com.tierconnect.riot.api.database.base.GenericOperator;
import com.tierconnect.riot.api.database.base.operator.MultipleOperator;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Project: reporting-api
 * Author: edwin
 * Date: 28/11/2016
 */
public class ConditionBuilder {

    private BooleanCondition booleanCondition;
    private List<GenericOperator> listGenericOperator = new ArrayList<GenericOperator>();

    public ConditionBuilder() {
        this.booleanCondition = BooleanCondition.AND;
    }

    public ConditionBuilder(BooleanCondition booleanCondition) {
        this.booleanCondition = booleanCondition;
    }

    public ConditionBuilder(List<GenericOperator> listGenericOperator) {
        this(BooleanCondition.AND);
        this.listGenericOperator = listGenericOperator;
    }

    public ConditionBuilder(BooleanCondition booleanCondition, List<GenericOperator> listGenericOperator) {
        this.booleanCondition = booleanCondition;
        this.listGenericOperator = listGenericOperator;
    }

    public ConditionBuilder(BooleanCondition booleanCondition, GenericOperator... listGenericOperator) {
        this(booleanCondition, Arrays.asList(listGenericOperator));
    }

    public ConditionBuilder(GenericOperator... listGenericOperator) {
        this(Arrays.asList(listGenericOperator));
    }

    public ConditionBuilder addOperator(GenericOperator genericOperator) {
        listGenericOperator.add(genericOperator);
        return this;
    }
    public ConditionBuilder addAllOperator(List<GenericOperator> lstGenericOperator) {
        listGenericOperator.addAll(lstGenericOperator);
        return this;
    }

    public ConditionBuilder addMultiple(BooleanCondition booleanCondition, List<GenericOperator> genericOperators) {
        MultipleOperator multipleOperator = new MultipleOperator(booleanCondition);
        multipleOperator.addOperatorList(genericOperators);
        listGenericOperator.add(multipleOperator);
        return this;
    }

    public ConditionBuilder addMultiple(BooleanCondition booleanCondition, GenericOperator... genericOperators) {
        MultipleOperator multipleOperator = new MultipleOperator(booleanCondition);
        multipleOperator.addOperatorList(genericOperators);
        listGenericOperator.add(multipleOperator);
        return this;
    }

    public List<GenericOperator> getListGenericOperator() {
        return listGenericOperator;
    }

    public BooleanCondition getBooleanCondition() {
        return booleanCondition;
    }

    public void clear(){
        listGenericOperator.clear();
    }

    @Override
    public String toString() {
        return StringUtils.join(listGenericOperator, " " + booleanCondition + " ");
    }
}
