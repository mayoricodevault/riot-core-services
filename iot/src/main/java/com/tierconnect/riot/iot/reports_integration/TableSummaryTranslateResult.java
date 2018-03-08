package com.tierconnect.riot.iot.reports_integration;

import org.apache.log4j.Logger;

import java.util.List;
import java.util.Map;

/**
 * Created by vealaro on 2/1/17.
 */
public class TableSummaryTranslateResult extends SummaryTranslateResult implements ITranslateResult {

    private static Logger logger = Logger.getLogger(TableSummaryTranslateResult.class);
    protected TableSummaryReportConfig summaryReportConfig;

    public TableSummaryTranslateResult(TableSummaryReportConfig tableSummaryReportConfig) {
        super(tableSummaryReportConfig);
        this.summaryReportConfig = tableSummaryReportConfig;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void exportResult(Map<String, Object> result) {
        List<Map<String, Object>> resultSet = (List<Map<String, Object>>) result.get(SummaryReportExecution.RESULT);
        loadAxisDistinct(resultSet);
        sortAxisProperties();
        List<Map<String, Object>> series;
        if (summaryReportConfig.isTwoDimension()) {
            logger.info("Process Summary report :[" + summaryReportConfig.reportDefinition.getName() + "] with two Dimensions");
            series = processSeriesTwoDimension();
        } else {
            logger.info("Process Summary report :[" + summaryReportConfig.reportDefinition.getName() + "] with one Dimension");
            series = processSeriesOneDimension();
        }
        exportMap(series,"series");
    }
}
