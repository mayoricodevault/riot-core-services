package com.tierconnect.riot.iot.entities;

import com.tierconnect.riot.appcore.utils.Utilities;
import com.tierconnect.riot.commons.Constants;

import javax.annotation.Generated;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "reportRule")
@Generated("com.tierconnect.riot.appgen.service.GenModel")
public class ReportRule extends ReportRuleBase implements Comparable<ReportRule> {

    public Map<String, Object> publicDataMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", getId());
        map.put("propertyName", getPropertyName());
        map.put("operator", getOperator());
        map.put("value", getValueWithRegionalSetting());
        map.put("color", getColor());
        map.put("displayOrder", getDisplayOrder());
        map.put("stopRules", getStopRules());
        map.put("style", getStyle());
        map.put("iconType", getIconType());
        map.put("parentThingTypeId", getParentThingType() != null ? getParentThingType().getId() : null);
        map.put("thingTypeId", getThingType() != null ? getThingType().getId() : 0L);
        map.put("thingTypeFieldId", getThingTypeField() != null ? getThingTypeField().getId() : null);
        return map;
    }

    private String getValueWithRegionalSetting() {
        // TODO refactor with new implementation
        // rules only report map
        if (Constants.REPORT_TYPE_MAP.equals(getReportDefinition().getReportType())
                && getReportDefinition().getDateFormatAndTimeZone() != null
                && Utilities.isNumber(getValue())) {
            if (isTimestamp()) {
                return getReportDefinition().getDateFormatAndTimeZone().getISODateTimeFormatWithoutTimeZone(Long.valueOf(getValue()));
            } else if (getThingTypeField() != null
                    && getThingTypeField().getDataType() != null
                    && (ThingTypeField.Type.isDateOrTimestamp(getThingTypeField().getDataType().getId()))) {
                return getReportDefinition().getDateFormatAndTimeZone().getISODateTimeFormatWithoutTimeZone(Long.valueOf(getValue()));
            }
        }
        return getValue();
    }

    public boolean isNative() {
        return ReportDefinitionUtils.isNative(getPropertyName());
    }

    public boolean isDwell() {
        return ReportDefinitionUtils.isDwell(getPropertyName());
    }

    public boolean isTimestamp() {
        return ReportDefinitionUtils.isTimestamp(getPropertyName());
    }

    public boolean isThingTypeUdf() {
        return ReportDefinitionUtils.isThingTypeUdf(getParentThingType());
    }

    @Override
    public int compareTo(ReportRule reportRule) {
        if (reportRule.getDisplayOrder() != null && this.getDisplayOrder() != null) {
            return this.getDisplayOrder().compareTo(reportRule.getDisplayOrder());
        }
        return 0;
    }
}

