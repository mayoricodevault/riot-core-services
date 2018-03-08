package com.tierconnect.riot.api.database.base.operator;

import com.tierconnect.riot.api.database.base.GenericOperator;
import com.tierconnect.riot.api.database.base.conditions.BooleanCondition;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Project: reporting-api
 * Author: edwin
 * Date: 27/11/2016 - 08:36 PM
 */
public class MultipleOperator implements GenericOperator {

    private BooleanCondition booleanOperator;
    private List<GenericOperator> genericOperatorList = new ArrayList<>();

    public MultipleOperator(BooleanCondition booleanOperator) {
        this.booleanOperator = booleanOperator;
    }

    public MultipleOperator(BooleanCondition booleanOperator, List<GenericOperator> genericOperatorList) {
        this.booleanOperator = booleanOperator;
        this.genericOperatorList = genericOperatorList;
    }

    public MultipleOperator(BooleanCondition booleanOperator, GenericOperator... genericOperatorList) {
        this.booleanOperator = booleanOperator;
        this.genericOperatorList = Arrays.asList(genericOperatorList);
    }

    public boolean isMultipleOperator() {
        return true;
    }

    public GenericOperator addOperator(GenericOperator genericOperators) {
        this.genericOperatorList.add(genericOperators);
        return this;
    }

    public GenericOperator addOperatorList(GenericOperator... genericOperators) {
        this.genericOperatorList.addAll(Arrays.asList(genericOperators));
        return this;
    }

    public GenericOperator addOperatorList(List<GenericOperator> genericOperatorList) {
        this.genericOperatorList.addAll(genericOperatorList);
        return this;
    }

    public BooleanCondition getBooleanOperator() {
        return booleanOperator;
    }

    public List<GenericOperator> getGenericOperatorList() {
        return genericOperatorList;
    }

    @Override
    public String toString() {
        return "(" + StringUtils.join(genericOperatorList, " " + booleanOperator.toString() + " ") + ")";
    }
}
