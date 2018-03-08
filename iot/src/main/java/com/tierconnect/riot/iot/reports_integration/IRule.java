package com.tierconnect.riot.iot.reports_integration;

import java.util.Map;

/**
 * Created by vealaro on 2/15/17.
 */
public interface IRule {

    boolean matches(Map<String, Object> values);

    String getColor();

    String getIcon();

    String getProperty();

    Boolean getStopOnMatch();

    void setExecuted(boolean executed);

    boolean isExecuted();

    Long getThingTypeId();
}
