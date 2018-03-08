package com.tierconnect.riot.api.database.base;

import com.tierconnect.riot.api.assertions.Assertions;
import com.tierconnect.riot.api.database.base.alias.Alias;
import com.tierconnect.riot.api.database.base.conditions.ConditionBuilder;
import com.tierconnect.riot.api.database.base.operator.MultipleOperator;
import com.tierconnect.riot.api.database.base.operator.SingleOperator;
import com.tierconnect.riot.api.database.base.operator.SubQueryOperator;
import com.tierconnect.riot.api.database.base.ordination.Order;
import com.tierconnect.riot.api.database.exception.OperationNotSupportedException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * Created by vealaro on 11/28/16.
 * abstract class for return database result.
 */
public abstract class DataBase<T> {

    /**
     * object with filters
     */
    protected ConditionBuilder builder;

    /**
     * explain result NOTE: executionPlan will be null if enableExplain is false.
     */
    protected Map<String, Object> executionPlan;

    /**
     * result Object
     */
    protected List<Map<String, Object>> resultSet = new ArrayList<>();

    /**
     * total records without skip and limits
     */
    protected Long countAll;

    /**
     * Object generate tu alias
     */
    protected List<Alias> aliasList;

    /**
     * sort
     */
    protected Map<String, Order> mapOrder;

    protected ExecutorService executor;
    protected ExecutionResultScope resultScope = ExecutionResultScope.INCLUDE_RESULT_AND_TOTAL;

    public enum ExecutionResultScope {
        INCLUDE_RESULT,
        INCLUDE_TOTAL,
        INCLUDE_RESULT_AND_TOTAL,
        INCLUDE_NOTHING
    }

    /**
     * Constructor Base
     *
     * @param builder {@link ConditionBuilder}
     */
    public DataBase(ConditionBuilder builder) {
        Assertions.voidNotNull("ConditionBuilder", builder);
        this.builder = builder;
    }

    /**
     * @param operator
     * @return
     * @throws OperationNotSupportedException
     */
    public abstract T transformMultiOperator(MultipleOperator operator) throws OperationNotSupportedException;

    /***
     * @param operator
     * @return
     * @throws OperationNotSupportedException
     */
    public abstract T transformSingleOperator(SingleOperator operator) throws OperationNotSupportedException;

    /***
     * @param operator
     * @return
     * @throws OperationNotSupportedException
     */
    public abstract T transformSubQueryOperator(SubQueryOperator operator) throws OperationNotSupportedException;

    /**
     * @return
     * @throws OperationNotSupportedException
     */
    public abstract String getConditionBuilderString() throws OperationNotSupportedException;

    /**
     * @return {@link List}<{@link Map}<{@link String},{@link Object}>>
     */
    public List<Map<String, Object>> getResultSet() {
        return resultSet;
    }

    /**
     * Update {@link ConditionBuilder}
     *
     * @param builder not null
     */
    public void setBuilder(ConditionBuilder builder) {
        Assertions.voidNotNull("ConditionBuilder", builder);
        this.builder = builder;
    }

    /***
     * Return object {@link ConditionBuilder}
     *
     * @return {@link ConditionBuilder}
     */
    public ConditionBuilder getBuilder() {
        return builder;
    }

    /**
     * return  {@link Map}<{@link String},{@link Object}>
     *
     * @return {@link Map}<{@link String},{@link Object}>
     */
    public Map<String, Object> getExecutionPlan() {
        return executionPlan;
    }

    /**
     * @return {@link Long} countAll
     */
    public Long getCountAll() {
        return countAll;
    }

    /**
     * @return {@link List}<{@link Alias}>
     */
    public List<Alias> getAliasList() {
        return aliasList;
    }

    /**
     * @param aliasList
     */
    public void setAliasList(List<Alias> aliasList) {
        this.aliasList = aliasList;
    }

    /**
     * @param mapOrder
     */
    public void setMapOrder(Map<String, Order> mapOrder) {
        this.mapOrder = mapOrder;
    }

    /**
     * @param genericOperatorList {@link List}<{@link GenericOperator}>
     * @return {@link List}<T>
     * @throws OperationNotSupportedException
     */
    public List<T> transformMultiOperatorList(List<GenericOperator> genericOperatorList) throws
            OperationNotSupportedException {
        List<T> listOperation = new ArrayList<>();
        for (GenericOperator genericOperator : genericOperatorList) {
            if (genericOperator.isMultipleOperator()) {
                listOperation.add(transformMultiOperator((MultipleOperator) genericOperator));
            } else {
                if (genericOperator instanceof SingleOperator) {
                    listOperation.add(transformSingleOperator((SingleOperator) genericOperator));
                } else {
                    listOperation.add(transformSubQueryOperator((SubQueryOperator) genericOperator));
                }
            }
        }
        return listOperation;
    }

    public String getQueryWithoutValues() {
        return transformMultiOperatorListWithEmptyValues(builder.getListGenericOperator());
    }

    private String transformMultiOperatorListWithEmptyValues(List<GenericOperator> genericOperatorList) {
        StringBuilder sb = new StringBuilder();
        for (GenericOperator genericOperator : genericOperatorList) {
            if (genericOperator.isMultipleOperator()) {
                MultipleOperator multipleOperator = (MultipleOperator) genericOperator;
                sb.append(multipleOperator.getBooleanOperator() + transformMultiOperatorListWithEmptyValues(multipleOperator.getGenericOperatorList()));
            } else {
                if (genericOperator instanceof SingleOperator) {
                    SingleOperator singleOperator = (SingleOperator) genericOperator;
                    sb.append(singleOperator.getKey() + singleOperator.getOperator());
                } else {
                    //SubQueryOperator
                    sb.append("");
                }
            }
        }
        return sb.toString();
    }

    public boolean includeTotal() {
        return resultScope.equals(ExecutionResultScope.INCLUDE_TOTAL)
                || resultScope.equals(ExecutionResultScope.INCLUDE_RESULT_AND_TOTAL);
    }

    public boolean includeResult() {
        return resultScope.equals(ExecutionResultScope.INCLUDE_RESULT)
                || resultScope.equals(ExecutionResultScope.INCLUDE_RESULT_AND_TOTAL);
    }
}
