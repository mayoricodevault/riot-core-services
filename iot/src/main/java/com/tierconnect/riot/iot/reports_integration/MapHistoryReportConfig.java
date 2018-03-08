package com.tierconnect.riot.iot.reports_integration;

import com.tierconnect.riot.commons.DateFormatAndTimeZone;
import com.tierconnect.riot.iot.entities.ReportDefinition;

import java.util.Date;
import java.util.Map;

import static com.tierconnect.riot.commons.Constants.REPORT_TYPE_MAP_HISTORY;

/**
 * Created by vealaro on 1/12/17.
 */
public class MapHistoryReportConfig extends MapReportConfig {

    public MapHistoryReportConfig(ReportDefinition reportDefinition, Map<String, Object> dynamicFilters,
                                  Integer pageNum, Integer pageSize, Date startDate, Date endDate, Date now,
                                  DateFormatAndTimeZone dateFormatAndTimeZone) {
        super(reportDefinition, dynamicFilters, pageNum, pageSize, startDate, endDate, now,
                REPORT_TYPE_MAP_HISTORY, dateFormatAndTimeZone);
        processConfiguration(now);
    }

    @Override
    public boolean isHistoricalReport() {
        return true;
    }
}
