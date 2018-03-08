package com.tierconnect.riot.api.database.base;

import com.tierconnect.riot.api.database.base.annotations.ClassesAllowed;
import com.tierconnect.riot.api.database.base.conditions.BooleanCondition;
import com.tierconnect.riot.api.database.base.conditions.ConditionBuilder;
import com.tierconnect.riot.api.database.base.key.PrimaryKey;
import com.tierconnect.riot.api.database.base.operator.MultipleOperator;
import com.tierconnect.riot.api.database.base.operator.SingleOperator;
import com.tierconnect.riot.api.database.base.operator.SubQueryOperator;
import com.tierconnect.riot.api.database.exception.ValueNotPermittedException;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Project: reporting-api
 * Author: edwin
 * Date: 28/11/2016
 */
public final class Operation extends OperationBase {

    private static final Map<String, Method> methodList = getMethodList(Operation.class);

    private Operation() {
    }

    public static GenericOperator OR(GenericOperator... genericOperator) {
        MultipleOperator multipleOperator = new MultipleOperator(BooleanCondition.OR);
        multipleOperator.addOperatorList(genericOperator);
        return multipleOperator;
    }

    public static GenericOperator OR(List<GenericOperator> genericOperator) {
        MultipleOperator multipleOperator = new MultipleOperator(BooleanCondition.OR);
        multipleOperator.addOperatorList(genericOperator);
        return multipleOperator;
    }

    public static GenericOperator AND(GenericOperator... genericOperator) {
        MultipleOperator multipleOperator = new MultipleOperator(BooleanCondition.AND);
        multipleOperator.addOperatorList(genericOperator);
        return multipleOperator;
    }

    public static GenericOperator AND(List<GenericOperator> genericOperator) {
        MultipleOperator multipleOperator = new MultipleOperator(BooleanCondition.AND);
        multipleOperator.addOperatorList(genericOperator);
        return multipleOperator;
    }

    public static GenericOperator elementMatch(String keyParent, List<GenericOperator> listOperator) {
        return new SingleOperator(keyParent, OperationEnum.ELEMENT_MATCH, listOperator);
    }

    public static GenericOperator elementMatch(String keyParent, GenericOperator... listOperator) {
        return elementMatch(keyParent, Arrays.asList(listOperator));
    }

    @ClassesAllowed(listClass = {String.class, Boolean.class, BigDecimal.class,
            Long.class, Integer.class, Float.class, Double.class, Date.class, PrimaryKey.class})
    public static GenericOperator equals(String key, Object value) throws ValueNotPermittedException {
        checkValuePermitted(key, value, methodList);
        return new SingleOperator(key, OperationEnum.EQUALS, value);
    }

    @ClassesAllowed(listClass = {String.class, Boolean.class, BigDecimal.class,
            Long.class, Integer.class, Float.class, Double.class, Date.class, PrimaryKey.class})
    public static GenericOperator notEquals(String key, Object value) throws ValueNotPermittedException {
        checkValuePermitted(key, value, methodList);
        return new SingleOperator(key, OperationEnum.NOT_EQUALS, value);
    }

    @ClassesAllowed(listClass = {BigDecimal.class, Long.class, Integer.class,
            Float.class, Double.class, Date.class, PrimaryKey.class})
    public static GenericOperator greaterThan(String key, Object value) throws ValueNotPermittedException {
        checkValuePermitted(key, value, methodList);
        return new SingleOperator(key, OperationEnum.GREATER_THAN, value);
    }

    @ClassesAllowed(listClass = {BigDecimal.class, Long.class, Integer.class,
            Float.class, Double.class, Date.class, PrimaryKey.class})
    public static GenericOperator lessThan(String key, Object value) throws ValueNotPermittedException {
        checkValuePermitted(key, value, methodList);
        return new SingleOperator(key, OperationEnum.LESS_THAN, value);
    }

    @ClassesAllowed(listClass = {BigDecimal.class, Long.class, Integer.class,
            Float.class, Double.class, Date.class, PrimaryKey.class})
    public static GenericOperator greaterThanOrEquals(String key, Object value) throws ValueNotPermittedException {
        checkValuePermitted(key, value, methodList);
        return new SingleOperator(key, OperationEnum.GREATER_THAN_OR_EQUALS, value);
    }

    @ClassesAllowed(listClass = {BigDecimal.class, Long.class, Integer.class,
            Float.class, Double.class, Date.class, PrimaryKey.class})
    public static GenericOperator lessThanOrEquals(String key, Object value) throws ValueNotPermittedException {
        checkValuePermitted(key, value, methodList);
        return new SingleOperator(key, OperationEnum.LESS_THAN_OR_EQUALS, value);
    }

    @ClassesAllowed(listClass = {String.class, PrimaryKey.class})
    public static GenericOperator contains(String key, Object value) throws ValueNotPermittedException {
        checkValuePermitted(key, value, methodList);
        return new SingleOperator(key, OperationEnum.CONTAINS, value);
    }

    @ClassesAllowed(listClass = {String.class, PrimaryKey.class})
    public static GenericOperator startsWith(String key, Object value) throws ValueNotPermittedException {
        checkValuePermitted(key, value, methodList);
        return new SingleOperator(key, OperationEnum.STARTS_WITH, value);
    }

    @ClassesAllowed(listClass = {String.class, PrimaryKey.class})
    public static GenericOperator endsWith(String key, Object value) throws ValueNotPermittedException {
        checkValuePermitted(key, value, methodList);
        return new SingleOperator(key, OperationEnum.ENDS_WITH, value);
    }

    public static GenericOperator in(String key, Object values) throws ValueNotPermittedException {
        if (!(values instanceof List)) {
            throw new ValueNotPermittedException(String.format("this is object [%s] of class [%s] not permitted in method [%s] with key [%s]", values, values.getClass(), "in", key));
        }
        return new SingleOperator(key, OperationEnum.IN, values);
    }

    public static GenericOperator notIn(String key, Object values) throws ValueNotPermittedException {
        if (!(values instanceof List)) {
            throw new ValueNotPermittedException(String.format("this is object [%s] of class [%s] not permitted in method [%s] with key [%s]", values, values.getClass(), "notIn", key));
        }
        return new SingleOperator(key, OperationEnum.NOT_IN, values);
    }

    @ClassesAllowed(listClass = {BigDecimal.class, Long.class, Integer.class, Float.class, Double.class, Date.class, PrimaryKey.class})
    public static GenericOperator between(String key, Object startValue, Object endValue) throws ValueNotPermittedException {
        checkValuePermitted(key, startValue, methodList);
        checkValuePermitted(key, endValue, methodList);
        return new SingleOperator(key, OperationEnum.BETWEEN, twoValues(startValue, endValue));
    }

    public static GenericOperator exists(String key) {
        return new SingleOperator(key, OperationEnum.EXISTS);
    }

    public static GenericOperator notExists(String key) {
        return new SingleOperator(key, OperationEnum.NOT_EXISTS);
    }

    public static GenericOperator empty(String key) {
        return new SingleOperator(key, OperationEnum.EMPTY, "");
    }

    public static GenericOperator notEmpty(String key) {
        return new SingleOperator(key, OperationEnum.NOT_EMPTY, "");
    }

    public static GenericOperator isNull(String key) {
        return new SingleOperator(key, OperationEnum.IS_NULL);
    }

    public static GenericOperator isNotNull(String key) {
        return new SingleOperator(key, OperationEnum.IS_NOT_NULL);
    }

    public static GenericOperator emptyArray(String key) {
        return new SingleOperator(key, OperationEnum.EMPTY_ARRAY);
    }

    public static GenericOperator notEmptyArray(String key) {
        return new SingleOperator(key, OperationEnum.NOT_EMPTY_ARRAY);
    }

    public static GenericOperator arraySizeMatch(String key, Integer length) {
        return new SingleOperator(key, OperationEnum.ARRAY_SIZE_MATCH, length);
    }

    public static GenericOperator regex(String key, String value, String option) {
        return new SingleOperator(key, OperationEnum.REGEX, twoValues(value, option));
    }

    public static GenericOperator inSubquery(String key, ConditionBuilder builder, final String target, final String projectedField){
        return new SubQueryOperator(key, builder, target, projectedField, OperationEnum.IN);
    }

    public static GenericOperator notInSubquery(String key, ConditionBuilder builder, final String target, final String projectedField){
        return new SubQueryOperator(key, builder, target, projectedField, OperationEnum.NOT_IN);
    }

    public static GenericOperator inSubquery(String key, ConditionBuilder condition, final String target, final String projectedField, ConditionBuilder filter){
        return new SubQueryOperator(key, condition, target, projectedField, OperationEnum.IN, filter);
    }

    public static GenericOperator notInSubquery(String key, ConditionBuilder condition, final String target, final String projectedField, ConditionBuilder filter){
        return new SubQueryOperator(key, condition, target, projectedField, OperationEnum.NOT_IN, filter);
    }

    public enum OperationEnum {
        EQUALS,
        NOT_EQUALS,
        GREATER_THAN,
        LESS_THAN,
        GREATER_THAN_OR_EQUALS,
        LESS_THAN_OR_EQUALS,
        CONTAINS,
        STARTS_WITH,
        ENDS_WITH,
        IN,
        NOT_IN,
        BETWEEN,
        EXISTS, // MONGO
        NOT_EXISTS, // MONGO
        EMPTY,
        NOT_EMPTY,
        IS_NULL,
        IS_NOT_NULL,
        EMPTY_ARRAY, // MONGO
        NOT_EMPTY_ARRAY, // MONGO
        ARRAY_SIZE_MATCH, // MONGO
        REGEX, // MONGO
        ELEMENT_MATCH // MONGO
        ;
    }
}
