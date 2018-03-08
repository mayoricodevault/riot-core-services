package com.tierconnect.riot.iot.reports_integration;

import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.commons.DateFormatAndTimeZone;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.ThingTypePathService;
import com.tierconnect.riot.iot.services.ThingTypeService;
import org.apache.log4j.Logger;

import java.util.*;

import static com.tierconnect.riot.commons.Constants.*;

/**
 * Created by vealaro on 1/17/17.
 */
public class MapReportConfig extends ReportConfig {

    private static Logger logger = Logger.getLogger(MapReportConfig.class);
    private static final List<String> MAP_PROPERTIES =
            Arrays.asList(ID, LABEL_NAME, NAME, SERIAL, THING_TYPE_PROPERTY_ID, THING_TYPE_PROPERTY_NAME, LOCATION);

    public MapReportConfig(ReportDefinition reportDefinition, Map<String, Object> dynamicFilters, Integer pageNum,
                           Integer pageSize, Date startDate, Date endDate, Date now, String type,
                           DateFormatAndTimeZone dateFormatAndTimeZone) {
        super(reportDefinition, dynamicFilters, pageNum, pageSize, startDate, endDate, true, type, false);
        setDateFormatAndTimeZone(dateFormatAndTimeZone);
        processConfiguration(now);
    }

    @Override
    protected void properties() {
        paths = new HashMap<>();
        propertyReportList = new ArrayList<>();
        buildMapProperties();
        buildPropertiesWithPinLabelAndHeat();
        buildPropertiesWithRules();
    }

    /**
     *
     */
    private void buildMapProperties() {
        //Parent and child should be projected
        addProjection(PARENT);
        addProjection(CHILDREN);
        String translateString;
        for (String property : MAP_PROPERTIES) {
            if (property.equals(LOCATION)) {
                if (thingType != null) {
//                    ThingTypeField thingTypeField = thingType.getThingTypeFieldByName(LOCATION, true);
                    ThingTypeField thingTypeField = getUDFCoordinateOfThingType(thingType, true);
                    if (thingTypeField != null) {
                        translateString = translateString(thingTypeField.getName(), thingType);
                        propertyReportList.add(new PropertyReport(translateString, thingTypeField.getName(), property, false, thingTypeField, isHistoricalReport()));
                        addProjection(translateString);
                        addProjection(thingTypeField.getName() + ".time");
                    } else {
                        translateString = translateString(property, thingType);
                        for (ThingType tt : ThingTypeService.getInstance().thingTypesAssociatedTo(thingType)) {
                            thingTypeField = tt.getThingTypeFieldByName(LOCATION, true);
                            if (thingTypeField != null) {
                                propertyReportList.add(new PropertyReport(translateString, property, property, false, thingTypeField, isHistoricalReport()));
                                addProjection(translateString);
                                addProjection(property + ".time");
                            }
                        }
                    }
                    Map<String, String> mapPathsByThingType = ThingTypePathService.getInstance().getMapPathsByThingType(thingType);
                    addProjection(mapPathsByThingType.values(), translateString, translateString, "none");
                    paths.putAll(mapPathsByThingType);
                } else {
                    translateString = translateString(property, null);
                    PropertyReport propertyReport = new PropertyReport(translateString(property, null), property,
                            property, false, isHistoricalReport());
                    propertyReport.setDataType(ThingTypeField.Type.TYPE_LONLATALT.value);
                    propertyReportList.add(propertyReport);
                    addProjection(translateString);
                    addProjection(property + ".time");
                }
            } else {
                translateString = translateString(property, null);
                propertyReportList.add(new PropertyReport(translateString, property, property, true, isHistoricalReport()));
                addProjection(translateString);
            }
        }
    }

    private ThingTypeField getUDFCoordinateOfThingType(ThingType thingType, boolean checkChildren) {
        ThingTypeField thingTypeField = null;
        if (thingType != null) {
            List<ThingTypeField> ttfCoordinateList = thingType.getThingTypeFieldsByType(ThingTypeField.Type.TYPE_LONLATALT.value);
            if (ttfCoordinateList != null
                    && !ttfCoordinateList.isEmpty()) {
                thingTypeField = ttfCoordinateList.get(0);
            }
            if (thingTypeField == null && checkChildren) {
                List<ThingType> children = thingType.getChildren();
                for (ThingType tt : children) {
                    ThingTypeField udfCoordinateChildren = getUDFCoordinateOfThingType(tt, false);
                    if (udfCoordinateChildren != null) {
                        thingTypeField = udfCoordinateChildren;
                    }
                }
            }
        }
        return thingTypeField;
    }


    private void buildPropertiesWithPinLabelAndHeat() {
        Map<String, ReportProperty> propertyMap = reportDefinition.getHeatProperty();
        addHeatPropery(propertyMap);
        addPinLabel(propertyMap);
    }

    private void addPinLabel(Map<String, ReportProperty> propertyMap) {
        ReportProperty pinLabelProperty = propertyMap.get(Constants.PIN_LABEL);
        if (pinLabelProperty != null) {
            PropertyReport propertyReport;
            String translate = translate(pinLabelProperty, true);
            if (pinLabelProperty.getThingType() == null) {
                propertyReport = new PropertyReport(translate, pinLabelProperty.getPropertyName(),
                        PIN_LABEL, pinLabelProperty.isNative(), pinLabelProperty.getThingTypeField(), isHistoricalReport());
                propertyReportList.add(propertyReport);
            } else {
                propertyReport = new PropertyReport(translate, pinLabelProperty.getPropertyName(),
                        PIN_LABEL, pinLabelProperty.isNative(), pinLabelProperty.getThingType(), pinLabelProperty.getThingTypeField(), isHistoricalReport());
                propertyReportList.add(propertyReport);
                Map<String, String> mapPathsByThingType = ThingTypePathService.getInstance().getMapPathsByThingType(pinLabelProperty.getThingType());
                addProjection(mapPathsByThingType.values(), translate, translate, "none");
                paths.putAll(mapPathsByThingType);
            }
            addProjection(translate);
            logger.info("Pin label: " + propertyReport.getPropertyOriginal());
        }
    }

    private void buildPropertiesWithRules() {
        List<IRule> displayRules = new ArrayList<>();
        for (ReportRule reportRule : reportDefinition.getReportRuleOrderByDisplayOrder()) {
            String propertyRule = null;
            //try to get parent thing type if the rule is for a thing type property
            ThingType thingTypeRule = null;
            if (reportRule.isThingTypeUdf() && !reportRule.getParentThingType().isChild(reportRule.getThingType().getId())) {
                thingTypeRule = reportRule.getParentThingType();
            } else if (reportRule.getThingType() != null) {
                thingTypeRule = reportRule.getThingType();
            }
            String propertyName = reportRule.getPropertyName();
            String translate = translate(reportRule, true);
            addProjection(translate);
            Long positionRule = (reportRule.getDisplayOrder() == null ? reportRule.getId() : reportRule.getDisplayOrder().intValue());
            PropertyReport propertyReport;
            if (thingTypeRule != null) {
                ThingTypeField thingTypeField = reportRule.getThingTypeField();
                if (thingTypeField != null) {
                    propertyRule = "R" + positionRule + "." + thingTypeRule.getId() + "." +
                            thingTypeField.getId() + "." + reportRule.getPropertyName();
                    propertyReport = new PropertyReport(translate, propertyName, propertyRule,
                            reportRule.isNative(), thingTypeRule, thingTypeField, isHistoricalReport());
                } else {
                    propertyRule = "R" + positionRule + "." + thingTypeRule.getId() + ".0." + reportRule.getPropertyName();
                    propertyReport = new PropertyReport(translate, propertyName, propertyRule,
                            reportRule.isNative(), thingTypeRule, isHistoricalReport());
                }
                Map<String, String> pathsByThingType = ThingTypePathService.getInstance().getMapPathsByThingType(thingTypeRule);
                addProjection(pathsByThingType.values(), translate, translate, "none");
                paths.putAll(pathsByThingType);
            } else {
                if (reportRule.getThingType() != null) {
                    propertyReport = new PropertyReport(translate, propertyName, propertyName + "." +
                            reportRule.getThingType().getId(), isHistoricalReport());
                } else {
                    propertyRule = "R" + positionRule + ".THIS." + reportRule.getPropertyName();
                    propertyReport = new PropertyReport(translate, propertyName, propertyRule, isHistoricalReport());
                }
            }
            if (propertyRule != null) {
                displayRules.add(new MapRule(reportRule.getIconType(), reportRule.getColor(), reportRule.getOperator(),
                        getRuleValue(reportRule), propertyRule, reportRule.getStopRules(),
                        (reportRule.getThingType() != null ? reportRule.getThingType().getId() : null)
                ));
            }
            if (ZoneTranslator.isZoneProperty(propertyName)) {
                String reverse = zoneTranslator.getBackToPropertyName().get(propertyName.split(",")[0]);
                propertyReport = new PropertyReport(translate, propertyName, reverse, isHistoricalReport());
            }
            propertyReport.setDateFormatAndTimeZone(dateFormatAndTimeZone);
            propertyReportList.add(propertyReport);
        }
        if (isHistoricalReport()) {
            PropertyReport propertyReportTime = new PropertyReport(TIME, true);
            propertyReportTime.setDateFormatAndTimeZone(dateFormatAndTimeZone);
            propertyReportList.add(propertyReportTime);
            addProjection(TIME);
        }
        setDisplayRules(displayRules);
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
        result.put("results", records);
        return result;
    }
}
