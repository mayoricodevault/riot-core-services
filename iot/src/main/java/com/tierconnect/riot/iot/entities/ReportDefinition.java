package com.tierconnect.riot.iot.entities;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.tierconnect.riot.appcore.services.I18NService;
import com.tierconnect.riot.appcore.utils.Utilities;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.commons.DateFormatAndTimeZone;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Generated;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.*;


@Entity
@Table(name = "reportDefinition")
@Generated("com.tierconnect.riot.appgen.service.GenModel")
public class ReportDefinition extends ReportDefinitionBase {

    @Transient
    private DateFormatAndTimeZone dateFormatAndTimeZone;

    /**
     * Get a String With the report type, name and id.
     *
     * @return A {@link String} containing the message.
     */
    public String getLogMessage() {
        return "(" + this.reportType + "): \"" + this.name + "\" -ID:" + this.id;
    }

    @Override
    public Map<String, Object> publicMap() {
        Map<String, Object> publicMap = super.publicMap();

        //Adding reportFilters
        List<Map<String, Object>> reportFilters = new LinkedList<>();
        if (this.reportFilter != null) {
            for (ReportFilter reportFilter : this.reportFilter) {
                reportFilters.add(reportFilter.publicDataMap());
            }
        }
        publicMap.put("reportFilter", reportFilters);

        //Adding reportCustomFilters
        List<Map<String, Object>> reportCustomFilters = new LinkedList<>();
        if (this.reportCustomFilter != null) {
            for (ReportCustomFilter reportCustomFilter : this.reportCustomFilter) {
                reportCustomFilters.add(reportCustomFilter.publicDataMap());
            }
        }
        publicMap.put("reportCustomFilter", reportCustomFilters);

        //Adding reportProperties
        List<Map<String, Object>> reportProperties = new LinkedList<>();
        if (this.reportProperty != null) {
            for (ReportProperty reportProperty : this.reportProperty) {
                reportProperties.add(reportProperty.publicDataMap());
            }
        }
        publicMap.put("reportProperty", reportProperties);

        //Adding reportGroupBy
        List<Map<String, Object>> reportGroupByList = new LinkedList<>();
        if (this.reportGroupBy != null) {
            for (ReportGroupBy reportGroupBy : this.reportGroupBy) {
                reportGroupByList.add(reportGroupBy.publicDataMap());
            }
        }
        publicMap.put("reportGroupBy", reportGroupByList);

        //Adding reportRule
        List<Map<String, Object>> reportRulesList = new LinkedList<>();
        if (this.reportRule != null) {
            for (ReportRule reportRule : this.reportRule) {
                reportRulesList.add(reportRule.publicDataMap());
            }
        }
        publicMap.put("reportRules", reportRulesList);

        //Adding reportEntryOptionList
        List<Map<String, Object>> reportEntryOptionList = new LinkedList<>();
        if (this.reportEntryOption != null) {
            for (ReportEntryOption reportEntryOpt : this.reportEntryOption) {
                Map<String, Object> reportEntryOptMap = reportEntryOpt.publicMap();
                List<Map<String, Object>> reportEntryOptionProperty = new ArrayList<Map<String, Object>>();
                if (reportEntryOpt.getReportEntryOptionProperties() != null && reportEntryOpt
                        .getReportEntryOptionProperties().size() > 0) {
                    for (ReportEntryOptionProperty propertyForm : reportEntryOpt.getReportEntryOptionProperties()) {
                        Map<String, Object> propertyFormMap = propertyForm.publicMap();
                        if (propertyForm.getEntryFormPropertyDatas() != null && propertyForm
                                .getEntryFormPropertyDatas().size() > 0) {
                            List<Map<String, Object>> reportEntryFormData = new ArrayList<Map<String, Object>>();
                            for (EntryFormPropertyData entryFormPropertyData : propertyForm.getEntryFormPropertyDatas
                                    ()) {
                                Map<String, Object> data = entryFormPropertyData.publicMap();
                                reportEntryFormData.add(data);
                            }
                            propertyFormMap.put("entryFormPropertyData", reportEntryFormData);
                        }
                        reportEntryOptionProperty.add(propertyFormMap);
                    }
                }
                reportEntryOptMap.put("reportEntryOptionProperty", reportEntryOptionProperty);
                reportEntryOptionList.add(reportEntryOptMap);
            }
        }
        publicMap.put("reportEntryOption", reportEntryOptionList);

        //Adding reportDefinitionConfig
        List<Map<String, Object>> reportDefinitionConfigList = new LinkedList<>();
        if (this.reportDefinitionConfig != null) {
            for (ReportDefinitionConfig reportDefConf : this.reportDefinitionConfig) {
                reportDefinitionConfigList.add(reportDefConf.publicMap());
            }
        }
        publicMap.put("reportDefinitionConfig", reportDefinitionConfigList);

        return publicMap;
    }

    /**
     * Basic Map for report definition
     *
     * @return
     */
    public Map<String, Object> publicMapSimple() {
        return super.publicMap();
    }

    public Float getDisplayOrder(Object operator) {
        if (operator == null) return 0.0f;
        String temp = (String) operator.toString();
        return Float.valueOf(temp);
    }

    public String getDoubleFromString(Object operator) {
        if (operator == null) return "0.0";
        String temp = (String) operator.toString();
//		Float valueOeResult = new FloatingDecimal(valueOperator.floatValue()).doubleValue();
        return temp;
    }

    public boolean containsGroupByPartition() {
        boolean result = false;
        for (ReportGroupBy reportGroupBy : getReportGroupBy()) {
            result = result || (reportGroupBy.getByPartition() != null && reportGroupBy.getByPartition());
        }
        return result;
    }

    public boolean containsUnit() {
        boolean result = false;
        for (ReportGroupBy reportGroupBy : getReportGroupBy()) {
            result = result || (reportGroupBy.getUnit() != null && !reportGroupBy.getUnit().trim().isEmpty());
        }
        return result;
    }

    public String getMinUnit() {
        String result = null;
        for (ReportGroupBy reportGroupBy : getReportGroupBy()) {
            if (reportGroupBy.getUnit() != null) {
                switch (reportGroupBy.getUnit()) {
                    case "hour":
                        result = reportGroupBy.getUnit();
                        break;
                    case "day":
                        result = (result != null &&
                                (result.equals("hour"))) ? result : reportGroupBy.getUnit();
                        break;
                    case "week":
                        result = (result != null &&
                                (result.equals("hour") ||
                                        result.equals("day"))) ? result : reportGroupBy.getUnit();
                        break;
                    case "month":
                        result = (result != null &&
                                (result.equals("hour") ||
                                        result.equals("day") ||
                                        result.equals("week"))) ? result : reportGroupBy.getUnit();
                        break;
                    case "year":
                        result = (result != null &&
                                (result.equals("hour") ||
                                        result.equals("day") ||
                                        result.equals("week") ||
                                        result.equals("month"))) ? result : reportGroupBy.getUnit();
                        break;
                }
            }
        }
        return result;
    }

    /*****************************************************
     * Method to get the Report Property checked as heat
     * This functionality is for Zone Map Report
     ****************************************************/
    public Map<String, ReportProperty> getHeatProperty() {
        Map<String, ReportProperty> map = new HashMap<>();
        if (this.getReportProperty() != null && this.getReportProperty().size() > 0) {
            for (ReportProperty property : this.getReportProperty()) {
                if (property.getEnableHeat() != null && property.getEnableHeat()) {
                    map.put(Constants.HEAT, property);
                } else if (StringUtils.isNumeric(pinLabel)
                        && Integer.valueOf(pinLabel).equals(property.getDisplayOrder().intValue())) {
                    map.put(Constants.PIN_LABEL, property);
                }
            }
        }
        return map;
    }

    /**
     * Get a ReportDefinitionConfig object by key type.
     *
     * @param keyType key type criteria to find the result.
     * @return a report definition config object.
     */
    public ReportDefinitionConfig getReportDefConfigItem(final String keyType) {

        Predicate predicate = new Predicate<ReportDefinitionConfig>() {
            @Override
            public boolean apply(ReportDefinitionConfig reportDefinitionConfig) {
                return (keyType).equals(reportDefinitionConfig.getKeyType());
            }
        };

        List<ReportDefinitionConfig> resultRepDefinitionConf = new ArrayList<ReportDefinitionConfig>(Collections2.filter
                (reportDefinitionConfig, predicate));
        if (resultRepDefinitionConf.size() != 1) {
            throw new IllegalStateException();
        }
        return resultRepDefinitionConf.get(0);
    }

    /**
     *
     */
    public String getSanitizedName() {
        return Utilities.sanitizeString(name);
    }

    /**
     * return report filters
     *
     * @param propertyName
     * @return
     */
    public List<ReportFilter> getReportFilterByFilterPredicate(Predicate predicate) {
        return new ArrayList<>(Collections2.filter(reportFilter, predicate));
    }

    public List<ReportFilter> getReportFilterOrderByDisplayOrder() {
        Collections.sort(reportFilter);
        return reportFilter;
    }

    public List<ReportProperty> getReportPropertyOrderByDisplayOrder() {
        Collections.sort(reportProperty);
        return reportProperty;
    }

    public List<ReportRule> getReportRuleOrderByDisplayOrder() {
        Collections.sort(reportRule);
        return reportRule;
    }

    /**
     * return report property
     *
     * @param predicate
     * @return
     */
    public List<ReportProperty> getReportPropertyFilterPredicate(Predicate predicate) {
        return new ArrayList<>(Collections2.filter(reportProperty, predicate));
    }

    /**
     * Get Filters of the Report in JSON Format [Key:PropertyName], [Value:Operator+Value]
     *
     * @return
     */
    public String getFiltersFromReportInJSON() {
        StringBuilder filter = new StringBuilder();
        filter.append("{'nameReport':'").append(this.getName()).append("',");
        filter.append("'idReport':'").append(this.getId()).append("',");
        filter.append("'typeReport':'").append(this.getReportType()).append("',");
        filter.append("'filters':{");
        for (ReportFilter reportFilter : getReportFilter()) {
            filter.append("'").append(reportFilter.getPropertyName()).append("':'").append(reportFilter.getOperator() + " " + reportFilter.getValue()).append("'");
        }
        return filter.append("}}").toString();
    }

    /**
     * Get Filters of the Report in JSON Format [Key:PropertyName], [Value:Operator+Value]
     *
     * @return
     */
    public String getFiltersFromReportInString() {
        StringBuffer info = new StringBuffer("");
        info.append("nameReport:" + this.getName() + ",");
        info.append("idReport:" + this.getId() + ",");
        info.append("typeReport:" + this.getReportType() + ",");
        info.append("FILTERS::");
        for (ReportFilter reportFilter : getReportFilter()) {
            info.append(reportFilter.getPropertyName() + ",Operqator:" + reportFilter.getOperator() + ", Value: " + reportFilter.getValue() + ",");
        }
        return info.toString();
    }

    public DateFormatAndTimeZone getDateFormatAndTimeZone() {
        return dateFormatAndTimeZone;
    }

    public void setDateFormatAndTimeZone(DateFormatAndTimeZone dateFormatAndTimeZone) {
        this.dateFormatAndTimeZone = dateFormatAndTimeZone;
    }

    public String getReportTypeInView() {
        String type = reportType;
        String key = "";
        switch (reportType) {
            case Constants.REPORT_TYPE_TABLE_DETAIL:
                key = "@REPORT_VIEW_LABEL_TABLE_DETAIL";
                break;
            case Constants.REPORT_TYPE_MAP:
                key = "@REPORT_VIEW_LABEL_MAP";
                break;
            case Constants.REPORT_TYPE_MAP_SUMMARY:
                key = "@REPORT_VIEW_LABEL_ZONE_MAP";
                break;
            case Constants.REPORT_TYPE_TABLE_HISTORY:
                key = "@REPORT_VIEW_LABEL_TABLE_HISTORY";
                break;
            case Constants.REPORT_TYPE_TABLE_SUMMARY:
                key = "@REPORT_VIEW_LABEL_TABLE_SUMMARY";
                break;
            case Constants.REPORT_TYPE_TABLE_SCRIPT:
                key = "@REPORT_VIEW_LABEL_TABLE_MONGO";
                break;
        }
        if (!key.isEmpty()) {
            type = (new I18NService()).getKey(key, null, null);
        }
        return type;
    }

    /**
     * Validate if the report is new and comparable:
     *
     * @return a boolean containing the result true or false.
     */
    public boolean isNewAndComparable() {
        return this.id == null && isComparable();
    }

    /**
     * Validate if the report is comparable for three fields:
     * 2) name is not blank.
     * 3) report type isnot blank.
     * 4) group is not blank.
     *
     * @return a boolean containing the result true or false.
     */
    public boolean isComparable() {
        return StringUtils.isNotBlank(this.name) &&
                StringUtils.isNotBlank(this.reportType) &&
                this.group != null;
    }

    /**
     * Validate if the report is updateable and comparable:
     *
     * @return a boolean containing the result true or false.
     */
    public boolean isUpdateAndComparable() {
        return this.id != null && isComparable();
    }

    @Deprecated
    @Override
    public Boolean getFillHistoryData() {
        return super.getFillHistoryData();
    }
}
