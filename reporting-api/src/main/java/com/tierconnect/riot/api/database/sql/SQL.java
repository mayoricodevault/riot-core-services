package com.tierconnect.riot.api.database.sql;

import com.tierconnect.riot.api.database.base.DataBase;
import com.tierconnect.riot.api.database.base.GenericDataBase;
import com.tierconnect.riot.api.database.base.Operation;
import com.tierconnect.riot.api.database.base.conditions.BooleanCondition;
import com.tierconnect.riot.api.database.base.conditions.ConditionBuilder;
import com.tierconnect.riot.api.database.base.operator.MultipleOperator;
import com.tierconnect.riot.api.database.base.operator.SingleOperator;
import com.tierconnect.riot.api.database.base.operator.SubQueryOperator;
import com.tierconnect.riot.api.database.base.ordination.Order;
import com.tierconnect.riot.api.database.exception.OperationNotSupportedException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.tierconnect.riot.api.mongoShell.utils.CharacterUtils.*;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.apache.commons.lang.StringUtils.join;

/**
 * Project: reporting-api
 * Author: edwin
 * Date: 28/11/2016
 */
public class SQL extends DataBase<String> implements GenericDataBase {

    private static final String SQL_AND_CONDITIONS = " AND ";
    private static final String SQL_OR_CONDITIONS = " OR ";
    private static final String EQUALS = " = ";
    private static final String NOT_EQUALS = " <> ";
    private static final String GREATER_THAN = " > ";
    private static final String LESS_THAN = " < ";
    private static final String GREATER_THAN_OR_EQUALS = " >= ";
    private static final String LESS_THAN_OR_EQUALS = " <= ";
    private static final String LIKE = " LIKE ";
    private static final String IN = " IN ";
    private static final String NOT_IN = " NOT IN ";
    private static final String BETWEEN = " BETWEEN ";
    private static final String IS_NULL = " IS NULL";
    private static final String IS_NOT_NULL = " IS NOT NULL";

    public SQL(ConditionBuilder builder) {
        super(builder);
    }

    private String sortString;
    private String filterString;

    @Override
    public String getConditionBuilderString() throws OperationNotSupportedException {
        filterString = formatFilterString();
        return filterString;
    }

    private String formatFilterString() throws OperationNotSupportedException {
        return join(
                transformMultiOperatorList(builder.getListGenericOperator()),
                convertBooleanCondition(builder.getBooleanCondition()));
    }

    public void execute(String nameTable, Map<String, Order> orderMap) {
        throw new java.lang.UnsupportedOperationException("Execute not implement in SQL");
    }

    @Override
    public String transformMultiOperator(MultipleOperator operator) throws OperationNotSupportedException {
        return betweenParenthesis(
                join(transformMultiOperatorList(operator.getGenericOperatorList())
                        , convertBooleanCondition(operator.getBooleanOperator())));
    }

    @SuppressWarnings("unchecked")
    public String transformSingleOperator(SingleOperator operator) throws OperationNotSupportedException {
        StringBuilder builder = new StringBuilder(operator.getKey());
        if (Operation.OperationEnum.EQUALS.equals(operator.getOperator())) {
            return builder.append(EQUALS).append(formatObject(operator.getValue())).toString();
        } else if (Operation.OperationEnum.NOT_EQUALS.equals(operator.getOperator())) {
            return builder.append(NOT_EQUALS).append(formatObject(operator.getValue())).toString();
        } else if (Operation.OperationEnum.GREATER_THAN.equals(operator.getOperator())) {
            return builder.append(GREATER_THAN).append(formatObject(operator.getValue())).toString();
        } else if (Operation.OperationEnum.LESS_THAN.equals(operator.getOperator())) {
            return builder.append(LESS_THAN).append(formatObject(operator.getValue())).toString();
        } else if (Operation.OperationEnum.GREATER_THAN_OR_EQUALS.equals(operator.getOperator())) {
            return builder.append(GREATER_THAN_OR_EQUALS).append(formatObject(operator.getValue())).toString();
        } else if (Operation.OperationEnum.LESS_THAN_OR_EQUALS.equals(operator.getOperator())) {
            return builder.append(LESS_THAN_OR_EQUALS).append(formatObject(operator.getValue())).toString();
        } else if (Operation.OperationEnum.CONTAINS.equals(operator.getOperator())) {
            return builder.append(LIKE).append(betweenSingleQuote("%" + operator.getValue() + "%")).toString();
        } else if (Operation.OperationEnum.STARTS_WITH.equals(operator.getOperator())) {
            return builder.append(LIKE).append(betweenSingleQuote(operator.getValue() + "%")).toString();
        } else if (Operation.OperationEnum.ENDS_WITH.equals(operator.getOperator())) {
            return builder.append(LIKE).append(betweenSingleQuote("%" + operator.getValue())).toString();
        } else if (Operation.OperationEnum.IN.equals(operator.getOperator())) {
            return builder.append(IN).append(formatObject(operator.getValue())).toString();
        } else if (Operation.OperationEnum.NOT_IN.equals(operator.getOperator())) {
            return builder.append(NOT_IN).append(formatObject(operator.getValue())).toString();
        } else if (Operation.OperationEnum.BETWEEN.equals(operator.getOperator())) {
            List<Object> listValues = (List<Object>) operator.getValue();
            builder.append(BETWEEN).append(listValues.get(0));
            return builder.append(SQL_AND_CONDITIONS).append(listValues.get(1)).toString();
        } else if (Operation.OperationEnum.EMPTY.equals(operator.getOperator())) {
            return builder.append(EQUALS).append("''").toString();
        } else if (Operation.OperationEnum.NOT_EMPTY.equals(operator.getOperator())) {
            return builder.append(NOT_EQUALS).append("''").toString();
        } else if (Operation.OperationEnum.IS_NULL.equals(operator.getOperator())) {
            return builder.append(IS_NULL).toString();
        } else if (Operation.OperationEnum.IS_NOT_NULL.equals(operator.getOperator())) {
            return builder.append(IS_NOT_NULL).toString();
        }
        throw new OperationNotSupportedException(operator.getOperator() + " Operation not supported in MYSQL");
    }

    @Override
    public String transformSubQueryOperator(SubQueryOperator operator) throws OperationNotSupportedException {
        return null;
    }

    private String convertBooleanCondition(BooleanCondition booleanCondition) {
        return (BooleanCondition.AND.equals(booleanCondition) ? SQL_AND_CONDITIONS : SQL_OR_CONDITIONS);
    }

    private static String formatObject(Object object) {
        StringBuilder builder = new StringBuilder();
        if (object instanceof String) {
            builder.append(betweenSingleQuote(object.toString()));
        } else if (object instanceof Collection) {
            if (((Collection) object).isEmpty()) return betweenParenthesis(EMPTY);
            if (((List) object).get(0) instanceof String) {
                builder.append(OPEN_PARENTHESIS);
                builder.append(SINGLE_QUOTE).append(join((List) object, SINGLE_QUOTE + COMMA + SINGLE_QUOTE));
                builder.append(SINGLE_QUOTE);
                builder.append(CLOSE_PARENTHESIS);
            } else {
                builder.append(betweenParenthesis(join((List) object, COMMA)));
            }
        } else {
            builder.append(object);
        }
        return builder.toString();
    }

    @Override
    public void setMapOrder(Map<String, Order> mapOrder) {
        super.setMapOrder(mapOrder);
        formatSort();
    }

    private void formatSort() {
        sortString = EMPTY;
        if (this.mapOrder != null && !this.mapOrder.isEmpty()) {
            List<String> sortList = new ArrayList<>(mapOrder.size());
            for (Map.Entry<String, Order> orderEntry : mapOrder.entrySet()) {
                sortList.add(orderEntry.getKey() + SPACE + orderEntry.getValue());
            }
            sortString = join(sortList, COMMA);
        }
    }

    public String getSortString() {
        return sortString;
    }
}
