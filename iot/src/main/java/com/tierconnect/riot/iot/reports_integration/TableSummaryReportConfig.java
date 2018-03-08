package com.tierconnect.riot.iot.reports_integration;

import com.tierconnect.riot.commons.DateFormatAndTimeZone;
import com.tierconnect.riot.iot.entities.ReportDefinition;
import com.tierconnect.riot.iot.entities.ReportRule;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Created by vealaro on 1/31/17.
 */
public class TableSummaryReportConfig extends SummaryReportConfig {

    private static Logger logger = Logger.getLogger(TableSummaryReportConfig.class);

    public TableSummaryReportConfig(ReportDefinition reportDefinition, Map<String, Object> dynamicFilters,
                                    Integer pageNum, Integer pageSize, Date startDate, Date endDate,
                                    Date now, DateFormatAndTimeZone dateFormatAndTimeZone) {
        super(reportDefinition, dynamicFilters, pageNum, pageSize, startDate, endDate, now);
        setDateFormatAndTimeZone(dateFormatAndTimeZone);
        createGroupForAggregate(null);
        buildRules();
    }

    private void buildRules() {
        displayRulesSummary = new LinkedHashMap<>(reportDefinition.getReportRule().size());
        for (ReportRule reportRule : reportDefinition.getReportRuleOrderByDisplayOrder()) {
            List<IRule> listRule = new ArrayList<>(2);
            String propertyName = reportRule.getPropertyName();
            // all properties for table summary
            propertyName = StringUtils.EMPTY.equals(propertyName) ? SummaryRule.ALL_PROPERTIES : propertyName;
            if (displayRulesSummary.get(propertyName) != null) {
                listRule = displayRulesSummary.get(propertyName);
            }
            listRule.add(new SummaryRule(reportRule.getColor(), reportRule.getOperator(), reportRule.getValue(),
                    propertyName, reportRule.getStopRules()));
            displayRulesSummary.put(propertyName, listRule);
        }
    }
}
