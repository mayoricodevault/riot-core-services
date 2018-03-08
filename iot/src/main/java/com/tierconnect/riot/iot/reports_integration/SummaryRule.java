package com.tierconnect.riot.iot.reports_integration;

import com.tierconnect.riot.appcore.utils.Utilities;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Created by vealaro on 2/8/17.
 */
public class SummaryRule extends Rule implements IRule {

    private static Logger logger = Logger.getLogger(SummaryRule.class);
    public static final String RULE_LABEL = "RULE_LABEL";
    public static final String RULE_VALUE = "RULE_VALUE";
    public static final String ALL_PROPERTIES = "$ALL.PROPERTIES.TABLE.SUMMARY";

    public SummaryRule(String color, String operator, String value, String property, Boolean stopOnMatch) {
        super(color, operator, value, property, stopOnMatch);
        setValue(transformValueRule(value));
    }

    @Override
    public boolean matches(Map<String, Object> values) {
        return matches((String) values.get(RULE_LABEL), (BigDecimal) values.get(RULE_VALUE));
    }

    private boolean matches(String labels, BigDecimal value) {
        boolean matches = false;
        String[] properties = labels.split("\\+");
        for (String property : properties) {
            if (property.equalsIgnoreCase(getProperty())) {
                if (getValue() instanceof BigDecimal) {
                    matches = compare(value, (BigDecimal) getValue());
                } else {
                    matches = compare(String.valueOf(value), (String) getValue());
                }
            }
        }
        logger.debug("rule with currentValue[ " + getProperty() + " in " + labels + "] : (" + value + " " + getOperator() + " " + getValue() + ") result is " + matches);
        return matches;
    }

    private Object transformValueRule(String value) {
        Object newValue = value;
        if (!Utilities.isEmptyOrNull(value) && StringUtils.isNumeric(value)) {
            newValue = new BigDecimal(value);
        }
        return newValue;
    }
}
