package com.tierconnect.riot.iot.reports_integration;

import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by vealaro on 2/15/17.
 */
public class MapSummaryTranslateResult extends SummaryTranslateResult implements ITranslateResult {
    private static Logger logger = Logger.getLogger(MapSummaryTranslateResult.class);

    public MapSummaryTranslateResult(MapSummaryReportConfig configuration) {
        super(configuration);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void exportResult(Map<String, Object> result) {
        labelValues = new HashMap<>(2);
        long start = System.currentTimeMillis();
        List<Map<String, Object>> resultSetCount = (List<Map<String, Object>>) result.get(MapSummaryReportExecution.RESULT_COUNT);
        List<Map<String, Object>> resultSetHeat = (List<Map<String, Object>>) result.get(MapSummaryReportExecution.RESULT_HEAT);

        // process count by zone
        loadAxisDistinct(resultSetCount);
        Map<String, BigDecimal> groupingCount = new HashMap<>(getGrouping());
        Map<String, BigDecimal> groupingHeat = Collections.emptyMap();

        if (resultSetHeat != null && !resultSetHeat.isEmpty()) {
            loadAxisDistinct(resultSetHeat);
            groupingHeat = new HashMap<>(getGrouping());
        }

        labelValues.put(MapSummaryReportExecution.RESULT_COUNT, groupingCount);
        labelValues.put(MapSummaryReportExecution.RESULT_HEAT, groupingHeat);
        logger.info("TIME EXECUTION PROCESS POST EXECUTION AGGREGATE [" + (System.currentTimeMillis() - start) + "] ms");
    }

    @Override
    public void loadAxisDistinct(List<Map<String, Object>> result) {
        if (!getSummaryReportConfig().isTwoDimension()) {
            loadAxisDistinctOneDimension(result);
        }
    }


}
