package com.tierconnect.riot.iot.reports_integration;

import com.tierconnect.riot.commons.Constants;
import org.apache.commons.lang.StringUtils;

import java.math.BigDecimal;

/**
 * Created by vealaro on 1/20/17.
 */
public abstract class Rule {

    private String icon;
    private String color;
    private String operator;
    private Object value;
    private String property;
    private Boolean stopOnMatch;
    private boolean executed = false;
    private Long thingTypeId;

    public Rule(String icon, String color, String operator, Object value, String property, Boolean stopOnMatch, Long thingTypeId) {
        this.icon = icon;
        this.color = color;
        this.operator = operator;
        this.value = value;
        this.property = property;
        this.stopOnMatch = stopOnMatch;
        this.thingTypeId = thingTypeId;
    }

    public Rule(String color, String operator, Object value, String property, Boolean stopOnMatch) {
        this.color = color;
        this.operator = operator;
        this.value = value;
        this.property = property;
        this.stopOnMatch = stopOnMatch;
    }

    protected boolean compare(String currentValue, String ruleValue) {
        boolean matches = false;
        String currentValueLowerCase = currentValue.toLowerCase();
        String valueRuleLowerCase = ruleValue.toLowerCase();
        switch (getOperator()) {
            case Constants.OP_EQUALS:
                matches = StringUtils.equalsIgnoreCase(currentValueLowerCase, valueRuleLowerCase);
                break;
            case Constants.OP_NOT_EQUALS:
                matches = !StringUtils.equalsIgnoreCase(currentValueLowerCase, valueRuleLowerCase);
                break;
            case Constants.OP_CONTAINS:
                matches = currentValueLowerCase.contains(valueRuleLowerCase);
                break;
        }
        return matches;
    }

    protected boolean compare(BigDecimal currentValue, BigDecimal ruleValue) {
        boolean matches = false;
        int compared;
        switch (getOperator()) {
            case Constants.OP_GREATHER_THAN:
                matches = currentValue.compareTo(ruleValue) > 0;
                break;
            case Constants.OP_EQUALS:
                matches = currentValue.compareTo(ruleValue) == 0;
                break;
            case Constants.OP_LESS_THAN:
                matches = currentValue.compareTo(ruleValue) < 0;
                break;
            case Constants.OP_NOT_EQUALS:
                matches = currentValue.compareTo(ruleValue) != 0;
                break;
            case Constants.OP_GREATHER_THAN_EQUALS:
                compared = currentValue.compareTo(ruleValue);
                matches = compared > 0 || currentValue.compareTo(ruleValue) == 0;
                break;
            case Constants.OP_LESS_THAN_EQUALS:
                compared = currentValue.compareTo(ruleValue);
                matches = compared < 0 || currentValue.compareTo(ruleValue) == 0;
                break;
            case Constants.OP_CONTAINS:
                matches = currentValue.toString().contains(ruleValue.toString());
                break;
        }
        return matches;
    }

    public String getIcon() {
        return icon;
    }

    public String getColor() {
        return color;
    }

    public String getOperator() {
        return operator;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getProperty() {
        return property;
    }

    protected void setProperty(String property) {
        this.property = property;
    }

    public Boolean getStopOnMatch() {
        return stopOnMatch;
    }

    public void setExecuted(boolean executed) {
        this.executed = executed;
    }

    public boolean isExecuted() {
        return executed;
    }

    public Long getThingTypeId() {
        return thingTypeId;
    }
}
