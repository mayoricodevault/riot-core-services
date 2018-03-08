package com.tierconnect.riot.iot.reports_integration;

import com.tierconnect.riot.appcore.utils.Utilities;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.iot.entities.ThingTypeField;
import org.apache.commons.lang.StringUtils;

import java.util.*;

/**
 * Created by vealaro on 1/16/17.
 */
public class MapTranslateResult extends TranslateResult implements ITranslateResult {

    private static final List<String> NON_UDF_FIELDS = Collections.unmodifiableList(Arrays.asList("name", "serial", "group", "thingType", "parent"));
    private String color;
    private String icon;
    private Long timeStamp;

    public MapTranslateResult(ReportConfig configuration) {
        super(configuration);
        this.color = configuration.reportDefinition.getDefaultColorIcon();
        this.icon = configuration.reportDefinition.getDefaultTypeIcon();
    }

    @Override
    public void exportResult(Map<String, Object> result) {
        this.labelValues = new HashMap<>();
        this.labelValuesPathExist = new HashMap<>();
        this.timeStamps = new HashMap<>();
        for (PropertyReport propertyReport : configuration.getPropertyReportList()) {
            Object value = value(propertyReport, result, configuration.paths, configuration.isHistoricalReport());
            Date timeStamp = time(propertyReport, result, configuration.paths, configuration.isHistoricalReport());
            if (StringUtils.isNotEmpty(propertyReport.getProperty()) && propertyReport.getProperty().equals("serial")) {
                value = StringUtils.upperCase((String) value);
            }
            if (propertyReport.getDataType().equals(ThingTypeField.Type.TYPE_LONLATALT.value) && timeStamp != null) {
                String locationString = (String) value;
                value = locationString + ";" + timeStamp.getTime();
            }
            addlabelValuesAndTimeStamp(propertyReport.getLabel(), value, propertyReport.getPropertyOriginal(), timeStamp);
        }
        executionRules();
    }

    private void executionRules() {
        Date dateTime = timeStamps.get(Constants.LOCATION);
        this.timeStamp = dateTime != null ? dateTime.getTime() : null;
        this.color = configuration.reportDefinition.getDefaultColorIcon();
        this.icon = configuration.reportDefinition.getDefaultTypeIcon();
        boolean stop = false;
        for (IRule mapRule : configuration.getDisplayRules()) {
            super.getLabelValues().put(PATH_EXIST, labelValuesPathExist);
            if (!stop && mapRule.matches(super.getLabelValues())) {
                if (!Utilities.isEmptyOrNull(mapRule.getColor())) {
                    this.color = mapRule.getColor();
                }
                if (!Utilities.isEmptyOrNull(mapRule.getIcon())) {
                    this.icon = mapRule.getIcon();
                }
                if (timeStamps.get(mapRule.getProperty()) != null &&
                        !NON_UDF_FIELDS.contains(mapRule.getProperty())) {
                    this.timeStamp = timeStamps.get(mapRule.getProperty()).getTime();
                }
                if (mapRule.getStopOnMatch()) {
                    stop = mapRule.getStopOnMatch();
                }
            }
            super.getLabelValues().remove(mapRule.getProperty());
        }
        getLabelValues().remove(PATH_EXIST);
    }

    @Override
    public Map<String, Object> getLabelValues() {
        Map<String, Object> labelValues = super.getLabelValues();
        List<Object> pinStyles = new ArrayList<>(3);
        pinStyles.add(color);
        pinStyles.add(icon);
        pinStyles.add(timeStamp != null ? timeStamp : 0L);
        List<List<Object>> pinStyleList = new ArrayList<>();
        pinStyleList.add(pinStyles);
        labelValues.put("pinStyles", pinStyleList);
        return labelValues;
    }
}
