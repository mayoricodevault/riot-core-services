package com.tierconnect.riot.iot.reports_integration;

import com.tierconnect.riot.api.database.base.ordination.Order;
import com.tierconnect.riot.commons.DateFormatAndTimeZone;
import com.tierconnect.riot.iot.entities.ReportDefinition;
import com.tierconnect.riot.iot.entities.ThingTypeField;
import org.apache.commons.lang.StringUtils;

import java.util.*;

import static com.tierconnect.riot.commons.Constants.*;

/**
 * Created by vealaro on 1/10/17.
 */
public class TableHistoryReportConfig extends ReportConfig {

    public TableHistoryReportConfig(ReportDefinition reportDefinition, Map<String, Object> dynamicFilters,
                                    Integer pageNum, Integer pageSize, Date startDate, Date endDate, Date now,
                                    boolean addNonUdfInProperties, DateFormatAndTimeZone dateFormatAndTimeZone) {
        this(reportDefinition, dynamicFilters, pageNum, pageSize, startDate, endDate, now, addNonUdfInProperties, false, dateFormatAndTimeZone);
    }

    public TableHistoryReportConfig(ReportDefinition reportDefinition, Map<String, Object> dynamicFilters,
                                    Integer pageNum, Integer pageSize, Date startDate, Date endDate, Date now,
                                    boolean addNonUdfInProperties, boolean skipDateFilters,
                                    DateFormatAndTimeZone dateFormatAndTimeZone) {
        super(reportDefinition, dynamicFilters, pageNum, pageSize, startDate, endDate, addNonUdfInProperties, reportDefinition.getReportType(), skipDateFilters);
        setDateFormatAndTimeZone(dateFormatAndTimeZone);
        processConfiguration(now);
    }

    @Override
    protected void processConfiguration(Date now) {
        super.processConfiguration(now);
        initSortColumns();
    }

    @Override
    protected void properties() {
        super.properties();
        PropertyReport propertyReportTime = new PropertyReport(TIME, TIME, "Time", true);
        propertyReportTime.setDataType(ThingTypeField.Type.TYPE_DATE.value);
        propertyReportTime.setDateFormatAndTimeZone(dateFormatAndTimeZone);
        propertyReportList.add(0, propertyReportTime);
        addProjection(TIME, ReportJSFunction.FORMAT_DATE);
        addProjection(SNAPSHOT_LASTVALUE);
        addProjection(SNAPSHOT_TIMESERIES);
        if (addNonUdfInProperties) {
            propertyReportList.add(new PropertyReport(_ID, ID, ID, true));
            addProjection(_ID);
        }
    }

    @Override
    public Map<String, Object> exportResult(Long total, List<Map<String, Object>> records) {
        List<String> timeSeriesLabels = new ArrayList<>();
        Map<String, Object> result = new LinkedHashMap<>(5);
        Long startDate;
        if (this.startDate == null && this.isHistoricalReport() && this.startDate != null) {
            startDate = this.startDate.getTime();
        } else {
            startDate = this.startDate != null ? this.startDate.getTime() : null;
        }
        result.put("total", total);
        result.put("startDate", startDate);
        result.put("endDate", this.endDate != null ? this.endDate.getTime() : null);

        Map<String, Object> headers = new LinkedHashMap<>();
        for (PropertyReport property : getPropertyReportList()) {
            property.export(headers);
            if (property.isPropertyIstimeSeries()) {
                timeSeriesLabels.add(property.getLabel());
            }
        }
        result.put("thingFieldTypeMap", headers);
        if (!timeSeriesLabels.isEmpty()) {
            result.put("timeSeriesLabels", timeSeriesLabels);
        }
        result.put("results", records);
        return result;
    }

    private void initSortColumns() {
        String sortOrder = (String) dynamicFilters.get("sortProperty");
        addSortTo(TIME, StringUtils.equalsIgnoreCase(sortOrder, "ASC") ? Order.ASC : Order.DESC);
    }

    @Override
    public boolean isHistoricalReport() {
        return true;
    }
}
