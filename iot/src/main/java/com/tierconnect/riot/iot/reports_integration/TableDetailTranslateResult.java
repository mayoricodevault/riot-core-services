package com.tierconnect.riot.iot.reports_integration;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by vealaro on 1/11/17.
 */
public class TableDetailTranslateResult extends TranslateResult implements ITranslateResult {

    public TableDetailTranslateResult(ReportConfig configuration, String serverName, String contextPath) {
        super(configuration);
        setContextPath(contextPath);
        setServerName(serverName);
    }

    public TableDetailTranslateResult(ReportConfig configuration) {
        super(configuration);
    }

    @Override
    public void exportResult(Map<String, Object> result) {
        this.labelValues = new HashMap<>();
        for (PropertyReport propertyReport : configuration.getPropertyReportList()) {
            Object value = value(propertyReport, result, configuration.paths, configuration.isHistoricalReport());
            addlabelValues(propertyReport.getLabel(), value);
        }
    }
}
