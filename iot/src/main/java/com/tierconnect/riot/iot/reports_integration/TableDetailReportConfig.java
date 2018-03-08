package com.tierconnect.riot.iot.reports_integration;

import com.tierconnect.riot.api.database.base.ordination.Order;
import com.tierconnect.riot.appcore.utils.Utilities;
import com.tierconnect.riot.commons.DateFormatAndTimeZone;
import com.tierconnect.riot.iot.entities.ReportDefinition;
import com.tierconnect.riot.iot.entities.ReportProperty;
import org.apache.log4j.Logger;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.tierconnect.riot.commons.Constants.*;

/**
 * Created by vealaro on 1/10/17.
 * Class responsible for executing a table detail.
 */
public class TableDetailReportConfig extends ReportConfig {

    private static Logger logger = Logger.getLogger(TableDetailReportConfig.class);

    public TableDetailReportConfig(ReportDefinition reportDefinition, Map<String, Object> dynamicFilters,
                                   Integer pageNum, Integer pageSize, Date startDate, Date endDate,
                                   Date now, boolean addNonUdfInProperties){
        this(reportDefinition,dynamicFilters,pageNum,pageSize,startDate,endDate,now,addNonUdfInProperties,null, false);
    }

    public TableDetailReportConfig(ReportDefinition reportDefinition, Map<String, Object> dynamicFilters,
                                   Integer pageNum, Integer pageSize, Date startDate, Date endDate,
                                   Date now, boolean addNonUdfInProperties, DateFormatAndTimeZone dateFormatAndTimeZone,
                                   boolean thingTypeUdfAsObject) {
        super(reportDefinition, dynamicFilters, pageNum, pageSize, startDate, endDate, addNonUdfInProperties,
                reportDefinition.getReportType(), thingTypeUdfAsObject);
        setDateFormatAndTimeZone(dateFormatAndTimeZone);
        processConfiguration(now);
    }

    @Override
    protected void properties() {
        super.properties();
        if (addNonUdfInProperties) {
            propertyReportList.add(new PropertyReport(_ID, ID, ID, true));
            propertyReportList.add(new PropertyReport(THING_TYPE_TRANSLATE_ID, true));
            addProjection(_ID);
        }
    }

    @Override
    protected void processConfiguration(Date now) {
        super.processConfiguration(now);
        initSortColumns();
    }

    @Override
    public Map<String, Object> exportResult(Long total, List<Map<String, Object>> records) {
        Map<String, Object> result = new LinkedHashMap<>(5);
        Long startDate;
        if (this.startDate == null && this.isHistoricalReport() && this.startDate != null) {
            startDate = this.startDate.getTime();
        } else {
            startDate = this.startDate != null ? this.startDate.getTime() : null;
        }
        result.put("startDate", startDate);
        result.put("endDate", this.endDate != null ? this.endDate.getTime() : null);
        result.put("total", total);
        Map<String, Object> headers = new LinkedHashMap<>();
        for (PropertyReport property : this.getPropertyReportList()) {
            property.export(headers);
        }
        result.put("thingFieldTypeMap", headers);
        result.put("results", records);
        return result;
    }

    private void initSortColumns() {
        Integer column = (Integer) dynamicFilters.get("orderByColumn");
        String sortOrder = (String) dynamicFilters.get("sortProperty");
        //pageSize:-1. When it is processing 'Export Report in .csv'
        if (column != null && !Utilities.isEmptyOrNull(sortOrder)
                && pageSize != null && pageSize != -1) {
            for (ReportProperty rp : reportDefinition.getReportProperty()) {
                if (rp.getDisplayOrder().intValue() == column) {
                    //TODO: child property sort?????
                    String columnName = verifiedSnapshotsProperty(translate(rp, true));
                    logger.info("sorting by \"" + columnName + "\" " + sortOrder);
                    if (sortOrder.equalsIgnoreCase("asc")) {
                        addSortTo(columnName, Order.ASC);
                    } else {
                        addSortTo(columnName, Order.DESC);
                    }
                    break;
                }
            }
        }
    }


}
