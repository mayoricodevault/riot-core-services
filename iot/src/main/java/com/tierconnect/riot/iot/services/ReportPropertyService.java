package com.tierconnect.riot.iot.services;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.reports_integration.ZoneTranslator;
import com.tierconnect.riot.iot.utils.ReportExecutionUtils;
import com.tierconnect.riot.iot.utils.ReportRuleUtils;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Generated;
import java.util.*;

import static com.tierconnect.riot.commons.Constants.*;

@Generated("com.tierconnect.riot.appgen.service.GenService")
public class ReportPropertyService extends ReportPropertyServiceBase
{
    public static Map<String, List<Long> > getThingTypeFieldIds(List<ReportProperty> reportProperties, CompositeThing compositeThing) {
        Map<String, List<Long> > thingFieldsMap = new HashMap<>();
        Set<Long> thingFieldIdsTimeSeries = new LinkedHashSet<>();
        Set<Long> thingFieldIdsNotTimeSeries = new LinkedHashSet<>();

        for (ReportProperty reportProperty : reportProperties) {

            String propertyName = reportProperty.getPropertyName();
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

    public static Map<String, List<Long> > getFieldTypeIds(List<ReportProperty> reportProperties, CompositeThing compositeThing) {
        Map<String, List<Long> > thingFieldsMap = new HashMap<>();
        Set<Long> thingFieldIdsTimeSeries = new LinkedHashSet<>();
        Set<Long> thingFieldIdsNotTimeSeries = new LinkedHashSet<>();

        for (ReportProperty reportProperty : reportProperties) {

            String propertyName = reportProperty.getPropertyName();
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

    public static List<ThingTypeField> getZoneThingTypeFields(List<ReportProperty> reportProperties, CompositeThing compositeThing) {
        List<ThingTypeField> thingFieldsZone = new LinkedList<>();

        for (ReportProperty reportProperty : reportProperties) {
            String propertyName = reportProperty.getPropertyName();
            propertyName = ReportExecutionUtils.removeDwellTimeString(propertyName);
            propertyName = ReportExecutionUtils.removeTimeStamp(propertyName);
            ThingTypeField thingTypeField = compositeThing.getThingTypeFieldByName(propertyName);
            if(thingTypeField != null && (thingTypeField.getName().equals("zone")
                    || thingTypeField.getDataType().getId().compareTo( ThingTypeField.Type.TYPE_ZONE.value)==0) ) {
                thingFieldsZone.add(thingTypeField);
            }
        }
        return thingFieldsZone;
    }
    public void removePropertyFromList(Long id, List<ReportProperty> reportPropertyList) {
        for (ReportProperty reportProperty : reportPropertyList) {
            if (reportProperty.getId().equals(id)) {
                reportPropertyList.remove(reportProperty);
                break;
            }
        }
    }

    public List<Long> getZonePropertiesIds(List<ReportProperty> reportProperties) {
        List<Long> zonePropertiesId = new LinkedList<>();
        for(ReportProperty reportProperty : reportProperties) {
            String reportPropertyName = reportProperty.getPropertyName();
            if(reportPropertyName.contains("zoneProperty")) {
                String zonePropertyIdArray[] = reportPropertyName.split(",");
                if(zonePropertyIdArray.length > 1 && ReportRuleUtils.isNumeric(zonePropertyIdArray[1])) {
                    zonePropertiesId.add(Long.valueOf(zonePropertyIdArray[1]));
                }
            }
        }
        return zonePropertiesId;
    }

    public ReportProperty getPropertyNameFromOrderColumn(List<ReportProperty> reportProperties, int orderColumn) {
        for(ReportProperty reportProperty : reportProperties) {
            if(reportProperty.getDisplayOrder() != null && Math.round(reportProperty.getDisplayOrder()) == orderColumn ) {
                return reportProperty;
            }
        }return null;
    }

    public void setProperties(ReportProperty reportProperty, Map<String, Object> reportPropertyMap, ReportDefinition reportDefinition) {
        Long thingTypeIdReport = 0L;
        Long thingTypeFieldId = 0L;
        Long parentThingTypeFieldId = null;

        reportProperty.setPropertyName( (String) reportPropertyMap.get( "propertyName" ) );
        reportProperty.setLabel( (String) reportPropertyMap.get( "label" ) );
        reportProperty.setEditInline( (Boolean) reportPropertyMap.get( "editInline" ) );
        reportProperty.setDisplayOrder( reportDefinition.getDisplayOrder( reportPropertyMap.get( "displayOrder" ) ) );
        reportProperty.setSortBy( (String) reportPropertyMap.get( "sortBy" ) );
        reportProperty.setShowHover((reportPropertyMap.get("showHover") == null ? false : (Boolean) reportPropertyMap.get("showHover")));
        reportProperty.setEnableHeat((reportPropertyMap.get("enableHeat") == null ? false : (Boolean) reportPropertyMap.get("enableHeat")));

        if(reportPropertyMap.get("thingTypeId") != null) {
            thingTypeIdReport  = ((Number) reportPropertyMap.get("thingTypeId") ).longValue();
            reportProperty.setThingType( ThingTypeService.getInstance().get( thingTypeIdReport ) );
        }

        if(reportPropertyMap.get("thingTypeFieldId") != null && !reportPropertyMap.get("thingTypeFieldId").toString().equals( "" )
                && StringUtils.isNumeric( reportPropertyMap.get( "thingTypeFieldId" ).toString() ) )
        {
            thingTypeFieldId = ((Number) reportPropertyMap.get("thingTypeFieldId") ).longValue();
            reportProperty.setThingTypeField( ThingTypeFieldService.getInstance().get( thingTypeFieldId ) );
        }

        if(reportPropertyMap.get("parentThingTypeId") != null && !reportPropertyMap.get("parentThingTypeId").toString().equals( "" )  )
        {
            parentThingTypeFieldId  = ((Number) reportPropertyMap.get("parentThingTypeId") ).longValue();
            reportProperty.setParentThingType( ThingTypeService.getInstance().get( parentThingTypeFieldId ) );
        }

        validatePropertyInsertion(reportProperty.getPropertyName(), reportProperty.getThingType(),
                reportProperty.getThingTypeField());

        reportProperty.setReportDefinition(reportDefinition);
    }

    private void validatePropertyInsertion(String name, ThingType thingType, ThingTypeField thingTypeField) {
        List<String> properties = Arrays.asList(SERIAL, NAME, GROUP_PROPERTY_NAME, THING_TYPE_PROPERTY_NAME);
        if (!properties.contains(name) && !ReportDefinitionUtils.isDwell(name) && !ReportDefinitionUtils.isTimestamp(name)) {
            if (ZoneTranslator.isPropertyOfZone(name) || name.endsWith(".name")) {
                name = ZoneTranslator.reverseTranslateZoneProperty(name);
                List<ThingTypeField> thingTypeFieldByNameList = ThingTypeFieldService.getInstance().getThingTypeFieldByName(name);
                if (thingTypeFieldByNameList.isEmpty()) {
                    throw new UserException("Invalid property defined: " + name);
                }
            } else {
                thingTypeField = (thingType != null) ? thingType.getThingTypeFieldByName(name) : thingTypeField;
                if (thingTypeField == null) {
                    throw new UserException("Invalid property defined: " + name);
                } else if (thingTypeField != null && !name.equals(thingTypeField.getName())) {
                    throw new UserException("Invalid property defined: " + name +
                            " for thing type: " + thingTypeField.getThingType().getId());
                }
            }
        }
    }

    /******************************************************
     *  Method to get a list of properties which have a thingTypeFieldID
     * ****************************************************/
    public List<ReportProperty> getPropertiesByThingTypeId(Long thingTypeFieldId)
    {
        List<ReportProperty> lstOutputConfig = ReportPropertyService.getReportPropertyDAO().getQuery()
                .where(QReportProperty.reportProperty.thingTypeField.id.eq(thingTypeFieldId) )
                .list(QReportProperty.reportProperty);

        return lstOutputConfig;
    }

    /**
     * get Report Properties that have zone property's name
     * @param propertyName zoneproperty's name table
     * @return List of Report Properties that have zone property's name
     */
    public List<ReportProperty> getPropertiesByPropertyName(String propertyName){
        HibernateQuery query = getReportPropertyDAO().getQuery();
        BooleanBuilder reportWhereQuery = new BooleanBuilder(QReportProperty.reportProperty.propertyName.toLowerCase().eq(propertyName.toLowerCase()));
        return query.where(reportWhereQuery).list(QReportProperty.reportProperty);
    }

    /**
     * Create reportProperty
     * @param label
     * @param propertyName
     * @param propertyOrder
     * @param propertyTypeId
     * @param showPopUp
     * @param reportDefinition
     * @return
     */
    public ReportProperty createReportProperty( String label, String propertyName, String propertyOrder, Long propertyTypeId, Boolean showPopUp,
                                                ReportDefinition reportDefinition )
    {
        ReportProperty reportProperty = new ReportProperty();
        reportProperty.setLabel( label );
        reportProperty.setPropertyName( propertyName );
        reportProperty.setDisplayOrder( Float.parseFloat( propertyOrder ) );
        reportProperty.setThingType( ThingTypeService.getInstance().get( propertyTypeId ) );
        List<ThingTypeField> lstThingTypeField = ThingTypeFieldService.getInstance().getThingTypeFieldByName( propertyName );
        if(lstThingTypeField!=null && lstThingTypeField.size()>0)
        {
            reportProperty.setThingTypeField( lstThingTypeField.get( 0 ) );
        }

        reportProperty.setShowHover(showPopUp);
        reportProperty.setReportDefinition( reportDefinition );
        return reportProperty;
    }

    @Override public void validateInsert(ReportProperty reportProperty) {
        super.validateInsert(reportProperty);
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QReportProperty.reportProperty.label.eq(reportProperty.getLabel()));
        be = be.and(QReportProperty.reportProperty.propertyName.eq(reportProperty.getPropertyName()));
        be = be.and(QReportProperty.reportProperty.reportDefinition.eq(reportProperty.getReportDefinition()));
        if (reportProperty.getId() == null && getReportPropertyDAO().selectBy(be) != null) {
            throw new UserException("Report filter already exists");
        }
    }
}


