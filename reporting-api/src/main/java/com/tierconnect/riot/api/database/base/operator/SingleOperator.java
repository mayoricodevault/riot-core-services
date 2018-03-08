package com.tierconnect.riot.api.database.base.operator;

import com.tierconnect.riot.api.database.base.GenericOperator;
import com.tierconnect.riot.api.database.base.Operation;

import static com.tierconnect.riot.api.assertions.Assertions.voidNotNull;
import static com.tierconnect.riot.api.mongoShell.utils.CharacterUtils.betweenDoubleQuote;

/**
 * Project: reporting-api
 * Author: edwin
 * Date: 27/11/2016 - 06:53 PM
 */
public class SingleOperator implements GenericOperator {

    private final String key;
    private final Operation.OperationEnum operator;
    private Object value;

    public SingleOperator(String key, Operation.OperationEnum operator, Object value) {
        voidNotNull("key", key);
        voidNotNull("operator", operator);
        this.key = key;
        this.operator = operator;
        this.value = value;
    }

    public SingleOperator(String key, Operation.OperationEnum operator) {
        this(key, operator, null);
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

    public Operation.OperationEnum getOperator() {
        return operator;
    }

    public Object getValue() {
        if (value instanceof String && !((String) value).startsWith("ObjectId(")) {
            return escapeJava((String) value);
        }
        return value;
    }

    private String escapeJava(String s) {
        s = s.replace("\\", ("\\\\u005c"));
        s = s.replace("$", ("\\\\u0024"));
        s = s.replace("\"", ("\\\\u0022"));
        return s;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("\"").append(key).append("\" ").append(operator);
        if (value != null) {
            if (value instanceof String) {
                result.append(" \"").append(value).append("\"");
            } else {
                result.append(" ").append(value);
            }
        }
        return result.toString();
    }

}
