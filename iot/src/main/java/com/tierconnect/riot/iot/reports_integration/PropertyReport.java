package com.tierconnect.riot.iot.reports_integration;

import com.tierconnect.riot.commons.DateFormatAndTimeZone;
import com.tierconnect.riot.iot.entities.ReportDefinitionUtils;
import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.entities.ThingTypeField;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by vealaro on 1/4/17.
 */
public class PropertyReport {

    private String property;
    private String propertyChanged;
    private String propertyBlinked;
    private String propertyOriginal;
    private String label;
    private Long dataType = 1L;
    private Long subDataTypeZone = 0L;
    private Long id = 0L;
    private String typeParent = null;
    private ThingType thingType;
    private ThingTypeField thingTypeField;
    private boolean propertyNative;
    private boolean propertyIstimeSeries = false;
    private DateFormatAndTimeZone dateFormatAndTimeZone;

    public PropertyReport(String labelSummary) {
        this(labelSummary, labelSummary, labelSummary, false);
    }

    public PropertyReport(String property, boolean propertyNative) {
        this(property, property, property, propertyNative);
    }

    public PropertyReport(String property, String propertyOriginal, String label, boolean propertyNative) {
        this(property, propertyOriginal, label, propertyNative, null, null, false);
    }

    public PropertyReport(String property, String propertyOriginal, String label, boolean propertyNative, boolean timeSeries) {
        this(property, propertyOriginal, label, propertyNative, null, null, timeSeries);
    }

    public PropertyReport(String property, String propertyOriginal, String label, boolean propertyNative, ThingType thingType) {
        this(property, propertyOriginal, label, propertyNative, thingType, null, false);
    }

    public PropertyReport(String property, String propertyOriginal, String label, boolean propertyNative, ThingType thingType, boolean timeSeries) {
        this(property, propertyOriginal, label, propertyNative, thingType, null, timeSeries);
    }

    public PropertyReport(String property, String propertyOriginal, String label, boolean propertyNative, ThingTypeField thingTypeField, boolean timeSeries) {
        this(property, propertyOriginal, label, propertyNative, null, thingTypeField, timeSeries);
    }

    public PropertyReport(String property, String propertyOriginal, String label, boolean propertyNative,
                          ThingType thingType, ThingTypeField thingTypeField, boolean timeSeries) {
        this.label = label;
        this.property = property;
        this.thingType = thingType;
        this.propertyNative = propertyNative;
        this.propertyOriginal = propertyOriginal;
        this.thingTypeField = thingTypeField;
        verified(timeSeries);
    }

    private void verified(boolean timeSeriesExpected) {
        if (thingTypeField != null && thingTypeField.getDataType() != null) {
            id = thingTypeField.getId();
            dataType = thingTypeField.getDataType().getId();
            typeParent = thingTypeField.getTypeParent();
            propertyIstimeSeries = thingTypeField.getTimeSeries() != null && thingTypeField.getTimeSeries() && timeSeriesExpected;
        }
        if (!propertyNative) {
            if (ReportDefinitionUtils.isDwell(propertyOriginal)) {
                if (propertyOriginal.contains("zoneType")) {
                    subDataTypeZone = 91L; // Zone type name
                } else if (propertyOriginal.contains("property")) {
                    subDataTypeZone = 92L; // Zone Property id
                } else if (propertyOriginal.contains("facilityMap")) {
                    subDataTypeZone = 93L; // Facility Map
                } else if (propertyOriginal.contains("zoneGroup")) {
                    subDataTypeZone = 94L; // Zone Group
                }
                dataType = 0L;
            } else if (ReportDefinitionUtils.isTimestamp(propertyOriginal)) {
                dataType = 11L;
                if (propertyOriginal.contains("zoneTypeTime")) {
                    subDataTypeZone = 91L; // Zone type name
                } else if (propertyOriginal.contains("facilityMapTime")) {
                    subDataTypeZone = 93L; // Facility Map
                } else if (propertyOriginal.contains("zoneGroupTime")) {
                    subDataTypeZone = 94L; // Zone Group
                }
            } else if (propertyOriginal.endsWith("Type.name")) {
                subDataTypeZone = 91L; // Zone type name
            } else if (propertyOriginal.contains("Property.id")) {
                subDataTypeZone = 92L; // Zone Property id
            } else if (propertyOriginal.endsWith("LocalMap.id")) {
                subDataTypeZone = 93L; // Facility Map
            } else if (propertyOriginal.endsWith("Group.id")) {
                subDataTypeZone = 94L; // Zone Group
            } else if (propertyOriginal.endsWith("Code.name")) {
                subDataTypeZone = 95L; // Zone Code
            } else if (propertyOriginal.endsWith(".name")) {
                subDataTypeZone = 96L; //Zone name
            }
        }
    }

    public void export(Map<String, Object> headers) {
        Map<String, Object> fieldDetail = new HashMap<>();
        fieldDetail.put("thingFieldType", dataType);
        fieldDetail.put("thingFieldId", id);
        fieldDetail.put("propertyName", propertyOriginal);

        headers.put(label, fieldDetail);
    }

    public String getProperty() {
        return property;
    }

    public String getLabel() {
        return label;
    }

    public ThingType getThingType() {
        return thingType;
    }

    public String getPropertyOriginal() {
        return propertyOriginal;
    }

    public boolean isPropertyNative() {
        return propertyNative;
    }

    public Long getDataType() {
        return dataType;
    }

    public ThingTypeField getThingTypeField() {
        return thingTypeField;
    }

    public void setDataType(Long dataType) {
        this.dataType = dataType;
    }

    public String getTypeParent() {
        return typeParent;
    }

    public boolean isPropertyIstimeSeries() {
        return propertyIstimeSeries;
    }

    public void setPropertyIstimeSeries(boolean propertyIstimeSeries) {
        this.propertyIstimeSeries = propertyIstimeSeries;
    }

    public void setSubDataTypeZone(Long subDataTypeZone) {
        this.subDataTypeZone = subDataTypeZone;
    }

    public Long getSubDataTypeZone() {
        return subDataTypeZone;
    }

    public String getPropertyChanged() {
        return propertyChanged;
    }

    public void setPropertyChanged(String propertyChanged) {
        this.propertyChanged = propertyChanged;
    }

    public String getPropertyBlinked() {
        return propertyBlinked;
    }

    public void setPropertyBlinked(String propertyBlinked) {
        this.propertyBlinked = propertyBlinked;
    }

    public void setDateFormatAndTimeZone(DateFormatAndTimeZone dateFormatAndTimeZone) {
        this.dateFormatAndTimeZone = dateFormatAndTimeZone;
    }

    public DateFormatAndTimeZone getDateFormatAndTimeZone() {
        return dateFormatAndTimeZone;
    }

    public boolean isTimestamp() {
        return dataType.equals(ThingTypeField.Type.TYPE_TIMESTAMP.value);
    }

    public boolean isDate() {
        return dataType.equals(ThingTypeField.Type.TYPE_DATE.value);
    }

    public boolean isTimestampOrDate() {
        return isTimestamp() || isDate();
    }
}
