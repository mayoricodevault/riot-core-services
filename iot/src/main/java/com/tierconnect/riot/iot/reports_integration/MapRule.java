package com.tierconnect.riot.iot.reports_integration;

import com.tierconnect.riot.appcore.utils.Utilities;
import com.tierconnect.riot.commons.Constants;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

/**
 * Created by vealaro on 1/20/17.
 */
public class MapRule extends Rule implements IRule {

    private static Logger logger = Logger.getLogger(MapRule.class);

    public MapRule(String icon, String color, String operator, Object value, String property, Boolean stopOnMatch, Long thingTypeId) {
        super(icon, color, operator, value, property, stopOnMatch, thingTypeId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean matches(Map<String, Object> labelValues) {
        boolean matches = false;
        Map<String, Boolean> labelValuesPathExist = (Map<String, Boolean>) labelValues.get(TranslateResult.PATH_EXIST);
        Object currentValue = getValue(labelValues.get(getProperty()));
        Boolean hasLabelValuePath = labelValuesPathExist.get(getProperty());
        if (Constants.OP_IS_EMPTY.equals(getOperator())) {
            return ((currentValue == null || currentValue.toString().isEmpty()) || !hasLabelValuePath);
        }
        if (Constants.OP_IS_NOT_EMPTY.equals(getOperator())) {
            return Utilities.isNotEmptyOrNull(currentValue);
        }
        if (currentValue instanceof String && getValue() instanceof String) {
            matches = compare(currentValue.toString(), getValue().toString());
        } else if (getValue() instanceof BigDecimal) {
            if (currentValue instanceof Long) {
                matches = compare(BigDecimal.valueOf((Long) currentValue), (BigDecimal) getValue());
            } else if (currentValue instanceof Date) {
                matches = compare(BigDecimal.valueOf(((Date) currentValue).getTime()), (BigDecimal) getValue());
            } else if (currentValue instanceof String && Utilities.isNumber((String) currentValue)) {
                matches = compare(new BigDecimal((String) currentValue), new BigDecimal(getValue().toString()));
            }
        }
        logger.debug("rule with currentValue:(" + currentValue + " " + getOperator() + " " + getValue() + ") result is " + matches);
        return matches;
    }

    private Object getValue(Object currentValue) {
        if (currentValue instanceof Map) {
            return ((Map) currentValue).get("ts");
        }
        return currentValue;
    }
}
