package com.tierconnect.riot.iot.entities;

import com.tierconnect.riot.appcore.utils.Utilities;

import javax.annotation.Generated;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "reportFilter")
@Generated("com.tierconnect.riot.appgen.service.GenModel")
public class ReportFilter extends ReportFilterBase implements Comparable<ReportFilter> {

    public Map<String, Object> publicDataMap() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("id", getId());
        map.put("propertyName", getPropertyName());
        map.put("label", getLabel());
        map.put("fieldType", getFieldType());
        map.put("displayOrder", getDisplayOrder());
        map.put("operator", getOperator());
        map.put("value", getValueWithRegionalSetting());
        map.put("editable", getEditable());
        map.put("autoComplete", getAutoComplete());
        map.put("parentThingTypeId", getParentThingType() != null ? getParentThingType().getId() : null);
        map.put("thingTypeId", getThingType() != null ? getThingType().getId() : 0L);
        map.put("thingTypeFieldId", getThingTypeField() != null ? getThingTypeField().getId() : null);
        return map;
    }

    private String getValueWithRegionalSetting() {
        // TODO refactor with new implementation
        if (getReportDefinition().getDateFormatAndTimeZone() != null && Utilities.isNumber(getValue())) {
            List<String> propertiesWithRegionalSetting = Arrays.asList("startDate", "endDate");
            if (propertiesWithRegionalSetting.contains(getPropertyName())) {
                return getReportDefinition().getDateFormatAndTimeZone().getISODateTimeFormatWithoutTimeZone(Long.valueOf(getValue()));
            } else if (getFieldType() != null && (getFieldType() == 11 || getFieldType() == 24)) {
                return getReportDefinition().getDateFormatAndTimeZone().getISODateTimeFormatWithoutTimeZone(Long.valueOf(getValue()));
            }
        }
        return getValue();
    }

    /**
     * Get value from parameters or this configuration. if there is no parameter value,
     * get value from filter configuration
     *
     * @param values parameter values
     * @return value either from filter configuration or parameter value
     */
    public Object getValue(Map<String, Object> values) {
        Object value = null;

        String id = getId().toString();
        if (values.containsKey(id)) {
            value = values.get(getId().toString());
        } else {
            value = getValue();
        }
        return value;
    }

    public String getDefinition() {
        return "'" + label + "' : '" + operator + "'";
    }

    /**
     * Get value from parameters or this configuration. if there is no parameter value,
     * get value from filter configuration
     *
     * @param values parameter values
     * @return value either from filter configuration or parameter value
     */
    public Object getValueByLabel(Map<String, Object> values) {
        Object value = null;

        String propertyLabel = getLabel();
        if (values.containsKey(propertyLabel)) {
            value = values.get(propertyLabel);
        } else {
            value = getValue();
        }
        return value;
    }

    public boolean isNative() {
        return ReportDefinitionUtils.isNative(getPropertyName());
    }

    public boolean isThingTypeUdf() {
        return ReportDefinitionUtils.isThingTypeUdf(getParentThingType());
    }

    public boolean isDwell() {
        return ReportDefinitionUtils.isDwell(getPropertyName());
    }

    @Override
    public int compareTo(ReportFilter reportFilter) {
        if (this.getDisplayOrder() == null) return 0;
        return this.getDisplayOrder().compareTo(reportFilter.getDisplayOrder());
    }
}


