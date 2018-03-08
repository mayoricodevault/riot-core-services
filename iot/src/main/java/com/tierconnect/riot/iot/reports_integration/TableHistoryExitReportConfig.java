package com.tierconnect.riot.iot.reports_integration;

import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.commons.DateFormatAndTimeZone;
import com.tierconnect.riot.iot.entities.ReportDefinition;

import java.util.Date;
import java.util.Map;

/**
 * Created by vealaro on 3/14/17.
 */
public class TableHistoryExitReportConfig extends TableHistoryReportConfig {

    public TableHistoryExitReportConfig(ReportDefinition reportDefinition, Map<String, Object> dynamicFilters,
                                        Integer pageNum, Integer pageSize, Date startDate, Date endDate, Date now,
                                        boolean addNonUdfInProperties, DateFormatAndTimeZone dateFormatAndTimeZone) {
        super(reportDefinition, dynamicFilters, pageNum, pageSize, startDate, endDate, now, addNonUdfInProperties, false, dateFormatAndTimeZone);
        setCollectionTarget(Constants.COLLECTION_EXIT_REPORT);
    }

    @Override
    protected void properties() {
        super.properties();
        addProjection("thingId");
    }

    @Override
    public boolean isHistoricalReport() {
        return false;
    }
}
