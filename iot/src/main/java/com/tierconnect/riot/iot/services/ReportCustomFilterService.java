package com.tierconnect.riot.iot.services;

import com.tierconnect.riot.iot.entities.ReportCustomFilter;
import com.tierconnect.riot.iot.entities.ReportDefinition;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Generated;
import java.util.Map;

import static com.tierconnect.riot.iot.entities.ReportCustomFilter.SUPPORTED_DATA;

@Generated("com.tierconnect.riot.appgen.service.GenService")
public class ReportCustomFilterService extends ReportCustomFilterServiceBase {
    public void setProperties(ReportCustomFilter reportCustomFilter, Map<String, Object> reportFilterMap, ReportDefinition reportDefinition) {
        Integer dataTypeId = (Integer) reportFilterMap.get("dataTypeId");
        String label = (String) reportFilterMap.get("label");
        String propertyName = (String) reportFilterMap.get("propertyName");
        String operator = (String) reportFilterMap.get("operator");
        Boolean editable = (Boolean) reportFilterMap.get("editable");
        Object value = reportFilterMap.get("value");
        if (StringUtils.isEmpty(label) || StringUtils.isEmpty(propertyName) || StringUtils.isEmpty(operator)) {
            throw new UserException("Label, Property Name and Operator are requiered in Custom Filters");
        }
        if (dataTypeId == null && !SUPPORTED_DATA.contains(dataTypeId)) {
            throw new UserException("Invalid data type for custom field '" + label + "'");
        }
        reportCustomFilter.setPropertyName(propertyName);
        reportCustomFilter.setValue((value == null) ? "" : (value instanceof String) ? (String) value : String.valueOf(value));
        reportCustomFilter.setDisplayOrder(reportDefinition.getDisplayOrder(reportFilterMap.get("displayOrder")));
        reportCustomFilter.setOperator(operator);
        reportCustomFilter.setLabel(label);
        reportCustomFilter.setEditable(editable);
        reportCustomFilter.setDataTypeId(dataTypeId.longValue());
        reportCustomFilter.setReportDefinition(reportDefinition);
    }
}

