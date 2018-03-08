package com.tierconnect.riot.iot.reports_integration;

import com.tierconnect.riot.commons.Constants;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by vealaro on 1/11/17.
 */
public class TableHistoryTranslateResult extends TranslateResult implements ITranslateResult {

    private static Logger logger = Logger.getLogger(TableHistoryTranslateResult.class);

    public TableHistoryTranslateResult(ReportConfig configuration,
                                       String serverName, String contextPath) {
        super(configuration);
        setServerName(serverName);
        setContextPath(contextPath);
    }

    @Override
    public void exportResult(Map<String, Object> result) {
        this.labelValues = new HashMap<>();
        List<String> fieldsNotChanged = new ArrayList<>();
        List<String> fieldsNotBlinked = new ArrayList<>();

        for (PropertyReport propertyReport : configuration.getPropertyReportList()) {
            Object value = value(propertyReport, result, configuration.paths, configuration.isHistoricalReport());
            if (hasNotChanged(propertyReport, result, configuration.paths, configuration.isHistoricalReport())) {
                //save label of property that did not change
                fieldsNotChanged.add(propertyReport.getLabel());
            }
            if (hasNotBlinked(propertyReport, result, configuration.paths, configuration.isHistoricalReport())) {
                //save label of property that did not change
                fieldsNotBlinked.add(propertyReport.getLabel());
            }
            addlabelValues(propertyReport.getLabel(), value);
        }
        // map last value
        labelValues.put("vizix.typeRecord", getVizixTypeRecord(result));
        if (!fieldsNotChanged.isEmpty()) {
            labelValues.put("vizix.fieldsNotChanged", fieldsNotChanged);
        }
        if (!fieldsNotBlinked.isEmpty()) {
            labelValues.put("vizix.fieldsNotBlinked", fieldsNotBlinked);
        }
    }

    private boolean hasNotChanged(PropertyReport propertyReport, Map<String, Object> result, Map<String, String> path, boolean isReportTimeSeries) {
        Object objectResult = null;
        if (propertyReport.getPropertyChanged() != null) {
            objectResult = valueToObject(propertyReport.getThingType(), propertyReport.getPropertyChanged(), result, path, isReportTimeSeries);
        }
        return isFalseOrDoesNotExists(objectResult);
    }

    private boolean hasNotBlinked(PropertyReport propertyReport, Map<String, Object> result, Map<String, String> path, boolean isReportTimeSeries) {
        Object objectResult = null;
        if (propertyReport.getPropertyBlinked() != null) {
            objectResult = valueToObject(propertyReport.getThingType(), propertyReport.getPropertyBlinked(), result, path, isReportTimeSeries);
        }
        return isFalseOrDoesNotExists(objectResult);
    }

    private boolean isFalseOrDoesNotExists(Object objectResult) {
        return objectResult == null || ((objectResult instanceof Boolean) && !((Boolean) objectResult));
    }

    private Map<String, Boolean> getVizixTypeRecord(Map<String, Object> resultDocument) {
        Map<String, Boolean> result = new HashMap<>();
        if (resultDocument.get("value") instanceof Map) {
            Map mapValue = (Map) resultDocument.get("value");
            result.put(Constants.SNAPSHOT_LASTVALUE, (Boolean) mapValue.get(Constants.SNAPSHOT_LASTVALUE));
            result.put(Constants.SNAPSHOT_TIMESERIES, (Boolean) mapValue.get(Constants.SNAPSHOT_TIMESERIES));
        }
        result.computeIfAbsent(Constants.SNAPSHOT_LASTVALUE, v -> false);
        result.computeIfAbsent(Constants.SNAPSHOT_TIMESERIES, v -> false);
        return result;
    }
}
