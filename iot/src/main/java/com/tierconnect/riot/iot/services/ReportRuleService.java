package com.tierconnect.riot.iot.services;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.tierconnect.riot.appcore.utils.Utilities;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.geojson.PinStyle;
import com.tierconnect.riot.iot.utils.ReportExecutionUtils;
import com.tierconnect.riot.iot.utils.ReportRuleUtils;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import javax.annotation.Generated;
import java.util.*;

@Generated("com.tierconnect.riot.appgen.service.GenService")
public class ReportRuleService extends ReportRuleServiceBase {
	private static Logger logger = Logger.getLogger(ReportRuleService.class);

    private static final String EQUAL = "=";
    private static final String NOT_EQUAL = "!=";
    private static final String GREATER_THAN = ">";
    private static final String LESS_THAN = "<";
    private static final String GREATER_THAN_OR_EQUAL = ">=";
    private static final String LESS_THAN_OR_EQUAL = "<=";
    private static final String LIKE = "~";
    private static final String IS_EMPTY = "isEmpty";

    public static String applyRuleForGroupBy(ReportRule reportRule, ReportDefinition reportDefinition, Object dataValue) {
        PinStyle pinStyle = null;

        String color = reportDefinition.getDefaultColorIcon();
        String reportRuleTypeIcon = reportRule.getIconType() != null && reportRule.getIconType().length() > 0 ? reportRule.getIconType() : reportDefinition.getDefaultTypeIcon();
        String reportRuleColor = reportRule.getColor() != null && reportRule.getColor().length() > 0 ? reportRule.getColor() : reportDefinition.getDefaultColorIcon();

        if (dataValue != null) {
            if(reportRule.getValue() != null && reportRule.getValue().length() == 0) return reportRuleColor;
            if (reportRule.getOperator().equals(EQUAL)) {
                if(ReportRuleUtils.isNumeric(reportRule.getValue()) && ReportRuleUtils.isNumeric(dataValue.toString())) {
                    if(Double.valueOf(reportRule.getValue()).equals(Double.valueOf(dataValue.toString()))) {
                        return reportRuleColor;
                    }
                }
                if (reportRule.getValue().equals(dataValue)) {
                    return reportRuleColor;
                }
            } else if (reportRule.getOperator().equals(NOT_EQUAL)) {
                if(reportRule.getValue().equals(dataValue)) {
                    pinStyle = null;
                }
                if (!reportRule.getValue().equals(dataValue)) {
                    return reportRuleColor;
                }
            } else if (reportRule.getOperator().equals(GREATER_THAN) && ReportRuleUtils.isNumeric(dataValue.toString()) && ReportRuleUtils.isNumeric(reportRule.getValue().toString())) {
                if (Double.valueOf(dataValue.toString()).doubleValue() > Double.valueOf(reportRule.getValue().toString()).doubleValue()) {
                    return reportRuleColor;
                }
            } else if (reportRule.getOperator().equals(LESS_THAN) && ReportRuleUtils.isNumeric(dataValue.toString()) && ReportRuleUtils.isNumeric(reportRule.getValue().toString())) {
                if (Double.valueOf(dataValue.toString()).doubleValue() < Double.valueOf(reportRule.getValue()).doubleValue()) {
                    return reportRuleColor;
                }
            } else if (reportRule.getOperator().equals(GREATER_THAN_OR_EQUAL) && ReportRuleUtils.isNumeric(dataValue.toString()) && ReportRuleUtils.isNumeric(reportRule.getValue().toString())) {
                if (Double.valueOf(dataValue.toString()).doubleValue() >= Double.valueOf(reportRule.getValue()).doubleValue()) {
                    return reportRuleColor;
                }
            } else if (reportRule.getOperator().equals(LESS_THAN_OR_EQUAL) && ReportRuleUtils.isNumeric(dataValue.toString()) && ReportRuleUtils.isNumeric(reportRule.getValue().toString())) {
                if (Double.valueOf(dataValue.toString()).longValue() <= Double.valueOf(reportRule.getValue()).longValue()) {
                    return reportRuleColor;
                }
            } else if (reportRule.getOperator().equals(LIKE)) {
                if (dataValue.toString().contains(reportRule.getValue())) {
                    return reportRuleColor;
                }
            }
        }
        return color;
    }

    public static PinStyle applyRule(ReportRule reportRule, ReportDefinition reportDefinition, Object[] dataPoint, Long thingTypeFieldId) {
        PinStyle pinStyle = null;
        for(int it = 1; it < dataPoint.length; it++) {
            String color = reportDefinition.getDefaultColorIcon();

            Object dataValue = dataPoint[it];
            if(dataValue != null) {
                String dataValueSplit[] = dataValue.toString().split(",");
                Long thingTypeFieldIdCompare = -1L;
                if (dataValueSplit.length > 1) {
                    dataValue = dataValueSplit[0];
                    thingTypeFieldIdCompare = ReportRuleUtils.isNumeric(dataValueSplit[1].toString()) ?
                            Long.parseLong(dataValueSplit[1]) : -1L;
                }


                String reportRuleTypeIcon = reportRule.getIconType() != null && reportRule.getIconType().length() > 0 ? reportRule.getIconType() : reportDefinition.getDefaultTypeIcon();
                String reportRuleColor = reportRule.getColor() != null && reportRule.getColor().length() > 0 ? reportRule.getColor() : reportDefinition.getDefaultColorIcon();

                if (thingTypeFieldId > 0) {
                    if ( thingTypeFieldIdCompare.longValue() != thingTypeFieldId.longValue() ) continue;
                }


                if (dataValue != null) {
                    if (reportRule.getOperator().equals(EQUAL)) {
                        if (reportRule.getValue().trim().toLowerCase().equals(dataValue.toString().trim().toLowerCase())) {
                            color = reportRuleColor;
                            pinStyle = new PinStyle(color, reportRuleTypeIcon, ((Date) dataPoint[0]).getTime());
                            break;
                        }
                    } else if (reportRule.getOperator().equals(NOT_EQUAL)) {
                        if (reportRule.getValue().trim().toLowerCase().equals(dataValue.toString().trim().toLowerCase())) {
                            pinStyle = null;
                            break;
                        }
                        if (!reportRule.getValue().equals(dataValue)) {
                            color = reportRuleColor;
                            pinStyle = new PinStyle(color, reportRuleTypeIcon, ((Date) dataPoint[0]).getTime());
                        }
                    } else if (reportRule.getOperator().equals(GREATER_THAN) && ReportRuleUtils.isNumeric(dataValue.toString()) && ReportRuleUtils.isNumeric(reportRule.getValue().toString())) {
                        if (Double.valueOf(dataValue.toString()).doubleValue() >= Double.valueOf(reportRule.getValue()).doubleValue()) {
                            color = reportRuleColor;
                            pinStyle = new PinStyle(color, reportRuleTypeIcon, ((Date) dataPoint[0]).getTime());
                            break;
                        }
                    } else if (reportRule.getOperator().equals(LESS_THAN) && ReportRuleUtils.isNumeric(dataValue.toString()) && ReportRuleUtils.isNumeric(reportRule.getValue().toString())) {
                        if (Double.valueOf(dataValue.toString()).doubleValue() <= Double.valueOf(reportRule.getValue()).doubleValue()) {
                            color = reportRuleColor;
                            pinStyle = new PinStyle(color, reportRuleTypeIcon, ((Date) dataPoint[0]).getTime());
                            break;
                        }
                    } else if (reportRule.getOperator().equals(GREATER_THAN_OR_EQUAL) && ReportRuleUtils.isNumeric(dataValue.toString()) && ReportRuleUtils.isNumeric(reportRule.getValue().toString())) {
                        if (Double.valueOf(dataValue.toString()).doubleValue() >= Double.valueOf(reportRule.getValue()).doubleValue()) {
                            color = reportRuleColor;
                            pinStyle = new PinStyle(color, reportRuleTypeIcon, ((Date) dataPoint[0]).getTime());
                            break;
                        }
                    } else if (reportRule.getOperator().equals(LESS_THAN_OR_EQUAL) && ReportRuleUtils.isNumeric(dataValue.toString()) && ReportRuleUtils.isNumeric(reportRule.getValue().toString())) {
                        if (Double.valueOf(dataValue.toString()).doubleValue() <= Double.valueOf(reportRule.getValue()).doubleValue()) {
                            color = reportRuleColor;
                            pinStyle = new PinStyle(color, reportRuleTypeIcon, ((Date) dataPoint[0]).getTime());
                            break;
                        }
                    } else if (reportRule.getOperator().equals(LIKE)) {
                        if (dataValue.toString().trim().toLowerCase().contains(reportRule.getValue().trim().toLowerCase())) {
                            color = reportRuleColor;
                            pinStyle = new PinStyle(color, reportRuleTypeIcon, ((Date) dataPoint[0]).getTime());
                            break;
                        }
                    }else if (reportRule.getOperator().equals(IS_EMPTY)) {
                        if (dataValue.toString().trim().toLowerCase().equals(reportRule.getValue().trim().toLowerCase())) {
                            color = reportRuleColor;
                            pinStyle = new PinStyle(color, reportRuleTypeIcon, ((Date) dataPoint[0]).getTime());
                            break;
                        }
                    }
//                if(pinStyle != null) break;
                }
            }
        }
        if (pinStyle == null) {
            pinStyle = new PinStyle(reportDefinition.getDefaultColorIcon(), reportDefinition.getDefaultTypeIcon(), ((Date)dataPoint[0]).getTime());
        }
        return pinStyle;
    }

    public static PinStyle applyRuleThingType(ReportRule reportRule,
                                              ReportDefinition reportDefinition,
                                              Object[] timestampPointValue,
                                              Thing thing,
                                              Thing childThing) {
        PinStyle pinStyle = null;

        String reportRuleTypeIcon = reportRule.getIconType() != null && reportRule.getIconType().length() > 0?reportRule.getIconType():reportDefinition.getDefaultTypeIcon();
        String reportRuleColor = reportRule.getColor() != null && reportRule.getColor().length() > 0?reportRule.getColor():reportDefinition.getDefaultColorIcon();

        if(reportRule.getThingType().getId().compareTo(thing.getThingType().getId())==0 ||
                (childThing!= null && reportRule.getThingType().getId().compareTo(childThing.getThingType().getId())==0 )) {
            pinStyle = new PinStyle(reportRuleColor, reportDefinition.getDefaultTypeIcon(), ((Date)timestampPointValue[0]).getTime());
        }
        if(reportRule.getThingType().getId().compareTo(thing.getThingType().getId())==0) {
            pinStyle = new PinStyle(pinStyle.getColor(), reportRuleTypeIcon, ((Date)timestampPointValue[0]).getTime());
        }

        if(pinStyle == null) {
            pinStyle = new PinStyle(reportDefinition.getDefaultColorIcon(), reportDefinition.getDefaultTypeIcon(), ((Date)timestampPointValue[0]).getTime());
        }
        return pinStyle;
    }

    public static void applyRulesInPinStyle(Map<Long, PinStyle> shapePoints,
                                            ReportDefinition reportDefinition,
                                            Object[] timestampPointValue,
                                            Thing thing,
                                            Thing childThing,
                                            Long dateTimeLong) {
        for(ReportRule reportRule : reportDefinition.getReportRule()) {
            String propertyName = reportRule.getPropertyName();
            if(propertyName != null && propertyName.length() > 0) {
                PinStyle pinStyle = null;
                propertyName = propertyName.replace("parent.", "");

                Long thingFieldTypeId = 0L;
                ThingTypeField thingTypeField = null;
                if(thing!=null) {
                    thingTypeField = thing.getThingTypeField(propertyName);
                }
                if(thingTypeField == null && childThing != null) {
                    thingTypeField = childThing.getThingTypeField(propertyName);
                }

                if(thingTypeField != null) {
                    thingFieldTypeId = thingTypeField.getId();
                }

                //TODO This has to be fixed for Any, thingTypeid should not be 0
                //if((reportRule.getThingTypeIdReport() == null || reportRule.getThingTypeIdReport() == 0) || (reportRule.getThingTypeIdReport().equals(thing.getThingType().getId()) ||
                if((reportRule.getThingType() == null ) || (reportRule.getThingType().getId().compareTo(thing.getThingType().getId())==0 ||
                        (childThing!= null && reportRule.getThingType().getId().compareTo(childThing.getThingType().getId())==0 ))) {

                    pinStyle = applyRule(reportRule, reportDefinition, timestampPointValue, thingFieldTypeId);

                    if (pinStyle != null) {
                        if (shapePoints.get(pinStyle.getTimestamp()) != null) {
                            shapePoints.put(pinStyle.getTimestamp(),
                                    mergingPinStyles(shapePoints, pinStyle, reportDefinition.getDefaultColorIcon(), reportDefinition.getDefaultTypeIcon()));
                        } else {
                            shapePoints.put(pinStyle.getTimestamp(), pinStyle);
                        }
                        if ((reportRule.getStopRules() != null && reportRule.getStopRules()) &&
                                (!reportDefinition.getDefaultColorIcon().equals(pinStyle.getColor()) ||
                                        !reportDefinition.getDefaultTypeIcon().equals(pinStyle.getIconImage()))) {
                            break;
                        }
                    }
                }
            }else {
                //Property == null or Empty -> ApplyRule by ThingType
                PinStyle pinStyle = applyRuleThingType(reportRule, reportDefinition, timestampPointValue, thing, childThing);
                if (pinStyle != null) {
                    if(shapePoints.get(pinStyle.getTimestamp()) != null) {
                        shapePoints.put(pinStyle.getTimestamp(),
                                mergingPinStyles(shapePoints, pinStyle, reportDefinition.getDefaultColorIcon(), reportDefinition.getDefaultTypeIcon()));
                    }else {
                        shapePoints.put(pinStyle.getTimestamp(), pinStyle);
                    }
                    if((reportRule.getStopRules() != null && reportRule.getStopRules()) &&
                            (!reportDefinition.getDefaultColorIcon().equals(pinStyle.getColor()) ||
                            !reportDefinition.getDefaultTypeIcon().equals(pinStyle.getIconImage()))) {
                        break;
                    }
                }
            }
        }
    }

    public static PinStyle mergingPinStyles(Map<Long, PinStyle> shapePoints, PinStyle pinStyleNext, String defaultColor, String defaultIcon) {
        PinStyle pinStylePrevious = shapePoints.get(pinStyleNext.getTimestamp());
        PinStyle pinStyleRes = new PinStyle(pinStylePrevious.getColor(), pinStylePrevious.getIconImage(), pinStylePrevious.getTimestamp());
        if(!pinStyleNext.getColor().equals(defaultColor))
            pinStyleRes.setColor(pinStyleNext.getColor());
        if(!pinStyleNext.getIconImage().equals(defaultIcon))
            pinStyleRes.setIconImage(pinStyleNext.getIconImage());

        return pinStyleRes;
    }

    public static void insertColorAndShapeInMap(ReportDefinition reportDefinition,
                                                    ReportRule reportRule,
                                                    List<Object[] > pointsMap,
                                                    Map<Long, PinStyle> shapePoints) {
//        Long thingTypeFieldId = reportRule.getThingTypeIdReport();
//
//        for(Object[] dataPoint : pointsMap) {
//            PinStyle pinStyle = applyRule(reportRule, reportDefinition, dataPoint);
//            if(pinStyle != null) {
//                shapePoints.put(pinStyle.getTimestamp(), pinStyle);
//            }
//        }
    }

    public static List<ThingTypeField> getZoneThingFields(List<ReportRule> reportProperties, CompositeThing compositeThing) {
        List<ThingTypeField> thingFieldsZone = new LinkedList<>();

        for (ReportRule reportProperty : reportProperties) {
            String propertyName = reportProperty.getPropertyName();
            propertyName = ReportExecutionUtils.removeDwellTimeString(propertyName);
            propertyName = ReportExecutionUtils.removeTimeStamp(propertyName);
            ThingTypeField thingField = compositeThing.getThingTypeFieldByName(propertyName);
            if(thingField != null && (thingField.getName().equals("zone") || thingField.getDataType().getId().compareTo( ThingTypeField.Type.TYPE_ZONE.value ) == 0 )) {
                thingFieldsZone.add(thingField);
            }
        }
        return thingFieldsZone;
    }


    public static Map<String, List<Long> > getThingTypeFieldIds(List<ReportRule> ReportRules, CompositeThing compositeThing) {
        Map<String, List<Long> > thingFieldsMap = new HashMap<>();
        Set<Long> thingFieldIdsTimeSeries = new LinkedHashSet<>();
        Set<Long> thingFieldIdsNotTimeSeries = new LinkedHashSet<>();

        for (ReportRule reportRule : ReportRules) {
            String propertyName = reportRule.getPropertyName();
            if(propertyName.contains("zoneType") || propertyName.contains("zoneProperty")) {
                propertyName = "zone";
            }
            propertyName = ReportExecutionUtils.removeDwellTimeString(propertyName);
            propertyName = ReportExecutionUtils.removeTimeStamp(propertyName);
            ThingTypeField thingTypeField = compositeThing.getThingTypeFieldByName(propertyName);
            if(thingTypeField != null) {
                if (thingTypeField.getTimeSeries() != null && thingTypeField.getTimeSeries() == true) {
                    thingFieldIdsTimeSeries.add(thingTypeField.getId());
                }else {
                    thingFieldIdsNotTimeSeries.add(thingTypeField.getId());
                }
            }
        }

        thingFieldsMap.put(ReportExecutionUtils.IS_TIME_SERIES, new LinkedList<Long>(thingFieldIdsTimeSeries) );
        thingFieldsMap.put(ReportExecutionUtils.IS_NOT_TIME_SERIES, new LinkedList<Long>(thingFieldIdsNotTimeSeries) );

        return thingFieldsMap;
    }

    public static Map<String, List<Long> > getFieldTypeIds(List<ReportRule> ReportRules, CompositeThing compositeThing) {
        Map<String, List<Long> > thingFieldsMap = new HashMap<>();
        Set<Long> thingFieldIdsTimeSeries = new LinkedHashSet<>();
        Set<Long> thingFieldIdsNotTimeSeries = new LinkedHashSet<>();

        for (ReportRule reportRule : ReportRules) {
            String propertyName = reportRule.getPropertyName();
            if(propertyName.contains("zoneType") || propertyName.contains("zoneProperty")) {
                propertyName = "zone";
            }
            propertyName = ReportExecutionUtils.removeDwellTimeString(propertyName);
            propertyName = ReportExecutionUtils.removeTimeStamp(propertyName);
            ThingTypeField thingField = compositeThing.getThingTypeFieldByName(propertyName);
            if(thingField != null) {
                if (thingField.getTimeSeries() != null && thingField.getTimeSeries() == true) {
                    if(!thingFieldIdsTimeSeries.contains(thingField.getId()))
                        thingFieldIdsTimeSeries.add(thingField.getId());
                }else {
                    if(!thingFieldIdsTimeSeries.contains(thingField.getId()))
                        thingFieldIdsNotTimeSeries.add(thingField.getId());
                }
            }
        }

        thingFieldsMap.put(ReportExecutionUtils.IS_TIME_SERIES, new LinkedList<Long>(thingFieldIdsTimeSeries) );
        thingFieldsMap.put(ReportExecutionUtils.IS_NOT_TIME_SERIES, new LinkedList<Long>(thingFieldIdsNotTimeSeries) );

        return thingFieldsMap;
    }

    public boolean hasPropertyInReportFilter(List<ReportRule> reportRules, String propertyName) {
        for(ReportRule reportRule : reportRules) {
            if(reportRule.getPropertyName().toLowerCase().equals(propertyName != null ? propertyName.toLowerCase() : "" )) return true;
        }
        return false;
    }

    private void validateReportRule(ReportRule reportRule) {
        if (Utilities.isEmptyOrNull(reportRule.getPropertyName())) {
//            comment for VIZIX-7094
//            throw new UserException("Property name of rules is not value");
            reportRule.setPropertyName(StringUtils.EMPTY);
        }
        if (!Constants.OP_IS_EMPTY.equals(reportRule.getOperator()) && Utilities.isEmptyOrNull(reportRule.getValue())
                && !Constants.OP_IS_NOT_EMPTY.equals(reportRule.getOperator())) { //VIZIX-938
            throw new UserException("Value of rules is not valid");
        }
    }

    public void setProperties(ReportRule reportRule, Map<String, Object> reportRulesMap, ReportDefinition reportDefinition) {
        Long thingTypeIdReport = 0L;
        Long thingTypeFieldId = 0L;
        Long parentThingTypeId = 0L;

        reportRule.setPropertyName( (String) reportRulesMap.get( "propertyName" ) );
        reportRule.setStyle( (String) reportRulesMap.get( "style" ) );
        reportRule.setColor( (String) reportRulesMap.get( "color" ) );
        reportRule.setValue( (String) reportRulesMap.get( "value" ) );
        reportRule.setOperator( (String) reportRulesMap.get( "operator" ) );
        reportRule.setStopRules( (Boolean) reportRulesMap.get( "stopRules" ) );
        validateReportRule(reportRule);
        String iconType = null;
        if (reportRulesMap.get("iconType") != null) {
            reportRule.setIconType( (String) reportRulesMap.get( "iconType" ) );
        }else {
            reportRule.setIconType( iconType );
        }
        reportRule.setDisplayOrder( reportDefinition.getDisplayOrder( reportRulesMap.get( "displayOrder" ) ) );

        if(reportRulesMap.get("thingTypeId") != null && !reportRulesMap.get("thingTypeId").toString().equals( "" )
                && StringUtils.isNumeric( reportRulesMap.get( "thingTypeId" ).toString() ) )
        {
            thingTypeIdReport  = ((Number) reportRulesMap.get("thingTypeId") ).longValue();
            reportRule.setThingType( ThingTypeService.getInstance().get(thingTypeIdReport )  );
        }
        if(reportRulesMap.get("thingTypeFieldId") != null && !reportRulesMap.get("thingTypeFieldId").toString().equals( "" )
                && StringUtils.isNumeric( reportRulesMap.get( "thingTypeFieldId" ).toString() ) )
        {
            thingTypeFieldId = ((Number) reportRulesMap.get("thingTypeFieldId") ).longValue();
            reportRule.setThingTypeField( ThingTypeFieldService.getInstance().get( thingTypeFieldId ) );
        }
        if(reportRulesMap.get("parentThingTypeId") != null && !reportRulesMap.get("parentThingTypeId").toString().equals( "" )  )
        {
            parentThingTypeId  = ((Number) reportRulesMap.get("parentThingTypeId") ).longValue();
            reportRule.setParentThingType( ThingTypeService.getInstance().get( parentThingTypeId ) );
        }

        reportRule.setReportDefinition( reportDefinition );
    }

    /******************************************************
     * Method to get a list of Rules which have a thingTypeFieldID
     * ****************************************************/
    public List<ReportRule> getReportRulesByThingTypeId(Long thingTypeFieldId)
    {
        List<ReportRule> lstOutputConfig = ReportRuleService.getReportRuleDAO().getQuery()
                .where(QReportRule.reportRule.thingTypeField.id.eq(thingTypeFieldId) )
                .list(QReportRule.reportRule);

        return lstOutputConfig;
    }

    /**
     * get Rule Reports that have zone property's name
     * @param propertyName zoneproperty's name table
     * @return List of Report Rule that have zone property's name
     */
    public List<ReportRule> getRuleByPropertyName(String propertyName){
        HibernateQuery query = getReportRuleDAO().getQuery();
        BooleanBuilder reportWhereQuery = new BooleanBuilder(QReportRule.reportRule.propertyName.toLowerCase().eq(propertyName.toLowerCase()));
        return query.where(reportWhereQuery).list(QReportRule.reportRule);
    }

    public List<ReportRule> getRulesByOperator(String operator){
        HibernateQuery query = getReportRuleDAO().getQuery();
        BooleanBuilder reportWhereQuery = new BooleanBuilder(QReportRule.reportRule.operator.toLowerCase().eq(operator.toLowerCase()));
        return query.where(reportWhereQuery).list(QReportRule.reportRule);
    }

}
