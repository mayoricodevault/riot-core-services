package com.tierconnect.riot.iot.entities;

import com.tierconnect.riot.appcore.utils.Utilities;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Generated;
import javax.persistence.Entity;
import java.util.*;

import static com.tierconnect.riot.iot.entities.ThingTypeField.Type.*;

@Entity
@Generated("com.tierconnect.riot.appgen.service.GenModel")
public class ReportCustomFilter extends ReportCustomFilterBase {
    public static final List<Long> SUPPORTED_DATA = Collections.unmodifiableList(
            Arrays.asList(TYPE_TEXT.value, TYPE_NUMBER.value, TYPE_BOOLEAN.value, TYPE_DATE.value, TYPE_TIMESTAMP.value)
    );

    public Map<String, Object> publicDataMap() {
        List<Long> longs = Arrays.asList(TYPE_TEXT.value, TYPE_NUMBER.value, TYPE_BOOLEAN.value, TYPE_DATE.value, TYPE_TIMESTAMP.value);
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("id", getId());
        map.put("propertyName", getPropertyName());
        map.put("label", getLabel());
        map.put("displayOrder", getDisplayOrder());
        map.put("operator", getOperator());
        map.put("value", getValueParsed());
        map.put("editable", getEditable());
        map.put("dataTypeId", getDataTypeId());
        return map;
    }

    public String getValueParsed() {
        String response = getValue();
        if (!StringUtils.isEmpty(response) && Utilities.isNumber(response)
                && isDateType(getDataTypeId())
                && getReportDefinition().getDateFormatAndTimeZone() != null) {
            return getReportDefinition().getDateFormatAndTimeZone().getISODateTimeFormatWithoutTimeZone(Long.valueOf(getValue()));
        }
        return response;
    }

    public static boolean isDateType(long type) {
        return TYPE_DATE.value == type || type == TYPE_TIMESTAMP.value;
    }

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
}

