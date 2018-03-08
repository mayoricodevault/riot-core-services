package com.tierconnect.riot.iot.reports_integration;

import com.tierconnect.riot.api.database.mongo.aggregate.Pipeline;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.commons.DateFormatAndTimeZone;
import com.tierconnect.riot.iot.entities.ReportDefinition;
import com.tierconnect.riot.iot.entities.ReportProperty;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by vealaro on 2/15/17.
 */
public class MapSummaryReportConfig extends SummaryReportConfig {

    private static Logger logger = Logger.getLogger(MapSummaryReportConfig.class);
    private List<Pipeline> pipelineCount;
    private List<Pipeline> pipelineHead;

    public MapSummaryReportConfig(ReportDefinition reportDefinition, Map<String, Object> dynamicFilters,
                                  Integer pageNum, Integer pageSize, Date startDate, Date endDate, Date now,
                                  DateFormatAndTimeZone dateFormatAndTimeZone) {
        super(reportDefinition, dynamicFilters, pageNum, pageSize, startDate, endDate, now);
        setDateFormatAndTimeZone(dateFormatAndTimeZone);
        createGroupForAggregate(null);
        updatePipelines();
    }

    private void updatePipelines() {
        pipelineCount = new ArrayList<>(getPipelineList());
        Map<String, ReportProperty> propertyMap = reportDefinition.getHeatProperty();
        ReportProperty heatProperty = propertyMap.get(Constants.HEAT);
        if (heatProperty != null) {
            String heatString = translate(heatProperty, false);
            logger.info("Heat Map Filter with: " + heatString);
            createGroupForAggregate(heatString);
            pipelineHead = new ArrayList<>(getPipelineList());
        }
    }


    public List<Pipeline> getPipelineCount() {
        return pipelineCount;
    }

    public List<Pipeline> getPipelineHead() {
        return pipelineHead;
    }
}
