package com.tierconnect.riot.iot.reports_integration;

import com.tierconnect.riot.api.database.exception.ValueNotPermittedException;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.commons.DateFormatAndTimeZone;
import com.tierconnect.riot.iot.entities.ReportDefinition;
import com.tierconnect.riot.iot.entities.ReportGroupBy;
import com.tierconnect.riot.iot.entities.ReportProperty;
import com.tierconnect.riot.iot.entities.Zone;
import com.tierconnect.riot.iot.services.ZoneService;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.tierconnect.riot.commons.Constants.*;

/**
 * Created by vealaro on 2/16/17.
 */
public class TableDetailByZoneReportConfig extends TableDetailReportConfig {

    private static Logger logger = Logger.getLogger(TableDetailByZoneReportConfig.class);

    private Zone zoneFilter = null;

    public TableDetailByZoneReportConfig(ReportDefinition reportDefinition, Map<String, Object> dynamicFilters, Integer pageNum,
                                         Integer pageSize, Date startDate, Date endDate, Date now, DateFormatAndTimeZone dateFormatAndTimeZone) {
        super(reportDefinition, dynamicFilters, pageNum, pageSize, startDate, endDate, now, false, dateFormatAndTimeZone, false);
    }

    @Override
    protected void processFilters(Date now, Map<String, Object> dynamicFilters) throws ValueNotPermittedException {
        super.processFilters(now, dynamicFilters);
        setZoneFilter(dynamicFilters);
        List<ReportGroupBy> reportGroupBy = reportDefinition.getReportGroupBy();
        if (reportGroupBy != null && !reportGroupBy.isEmpty() && zoneFilter != null) {
            String translate = translate(reportGroupBy.get(0), false);
            addFilter(reportGroupBy.get(0).getLabel(), translate, Constants.OP_EQUALS, reportGroupBy.get(0).getPropertyName(), zoneFilter.getCode(), StringUtils.EMPTY, isHistoricalReport());
        }
    }

    @Override
    protected void properties() {
        super.properties();
        propertyReportList.add(new PropertyReport(_ID, ID, ID, true));
        propertyReportList.add(new PropertyReport(NAME, NAME, NAME, true));
        addProjection(_ID);
        addHeatPropery();
    }

    private void setZoneFilter(Map<String, Object> dynamicFilters) {
        Object zoneId = dynamicFilters.get("zoneId");
        if (zoneId != null && StringUtils.isNumeric(zoneId.toString())) {
            this.zoneFilter = ZoneService.getInstance().get(Long.valueOf(zoneId.toString()));
        }
    }

    @Override
    public Map<String, Object> exportResult(Long total, List<Map<String, Object>> records) {
        Map<String, Object> result = new LinkedHashMap<>(5);
        result.put("total", records.size());
        result.put("result", records);
        return result;
    }
}
