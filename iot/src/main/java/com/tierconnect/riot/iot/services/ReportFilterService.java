package com.tierconnect.riot.iot.services;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.utils.ReportExecutionUtils;
import com.tierconnect.riot.sdk.dao.UserException;

import javax.annotation.Generated;
import java.util.*;

@Generated("com.tierconnect.riot.appgen.service.GenService")
public class ReportFilterService extends ReportFilterServiceBase 
{
    public static Map<String, List<Long> > getThingTypeFieldIds(List<ReportFilter> ReportFilters, CompositeThing compositeThing) {
        Map<String, List<Long> > thingFieldsMap = new HashMap<>();
        Set<Long> thingFieldIdsTimeSeries = new LinkedHashSet<>();
        Set<Long> thingFieldIdsNotTimeSeries = new LinkedHashSet<>();

        for (ReportFilter reportFilter : ReportFilters) {
            String propertyName = reportFilter.getPropertyName();
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

    public static Map<String, List<Long> > getFieldTypeIds(List<ReportFilter> ReportFilters, CompositeThing compositeThing) {
        Map<String, List<Long> > thingFieldsMap = new HashMap<>();
        Set<Long> thingFieldIdsTimeSeries = new LinkedHashSet<>();
        Set<Long> thingFieldIdsNotTimeSeries = new LinkedHashSet<>();

        for (ReportFilter reportFilter : ReportFilters) {
            String propertyName = reportFilter.getPropertyName();
            propertyName = ReportExecutionUtils.removeDwellTimeString(propertyName);
            propertyName = ReportExecutionUtils.removeTimeStamp(propertyName);
            ThingTypeField thingTypeField = compositeThing.getThingTypeFieldByName(propertyName);
            if(thingTypeField != null) {
                if (thingTypeField.getTimeSeries() != null && thingTypeField.getTimeSeries() == true) {
                    if(!thingFieldIdsTimeSeries.contains(thingTypeField.getId()))
                        thingFieldIdsTimeSeries.add(thingTypeField.getId());
                }else {
                    if(!thingFieldIdsTimeSeries.contains(thingTypeField.getId()))
                        thingFieldIdsNotTimeSeries.add(thingTypeField.getId());
                }
            }
        }

        thingFieldsMap.put(ReportExecutionUtils.IS_TIME_SERIES, new LinkedList<Long>(thingFieldIdsTimeSeries) );
        thingFieldsMap.put(ReportExecutionUtils.IS_NOT_TIME_SERIES, new LinkedList<Long>(thingFieldIdsNotTimeSeries) );

        return thingFieldsMap;
    }

    public static List<ThingTypeField> getZoneThingFields(List<ReportFilter> reportProperties, CompositeThing compositeThing) {
        List<ThingTypeField> thingFieldsZone = new LinkedList<>();

        for (ReportFilter reportProperty : reportProperties) {
            String propertyName = reportProperty.getPropertyName();
            propertyName = ReportExecutionUtils.removeDwellTimeString(propertyName);
            propertyName = ReportExecutionUtils.removeTimeStamp(propertyName);
            ThingTypeField thingField = compositeThing.getThingTypeFieldByName(propertyName);
            if(thingField != null && (thingField.getName().equals("zone") ||
                    thingField.getDataType().getId().compareTo( ThingTypeField.Type.TYPE_ZONE.value) == 0 ))
            {
                thingFieldsZone.add(thingField);
            }
        }
        return thingFieldsZone;
    }

    public List<Long> getZonePropertiesIds(List<ReportFilter> reportFilters) {
        List<Long> zonePropertiesId = new LinkedList<>();
        for(ReportFilter reportFilter : reportFilters) {
            String reportPropertyName = reportFilter.getPropertyName();
            if(reportPropertyName.contains("zoneProperty") || reportPropertyName.contains("ScanZoneProperty")) {
                zonePropertiesId.add(reportFilter.getThingTypeField().getId());
            }
        }
        return zonePropertiesId;
    }

    public boolean hasPropertyInReportFilter(List<ReportFilter> reportFilters, String propertyName) {
        for(ReportFilter reportFilter : reportFilters) {
            if(reportFilter.getPropertyName().toLowerCase().equals(propertyName != null ? propertyName.toLowerCase() : "" )) return true;
        }
        return false;
    }
    public ReportFilter getPropertyInReportFilter(List<ReportFilter> reportFilters, String propertyName) {
        for(ReportFilter reportFilter : reportFilters) {
            if(reportFilter.getPropertyName().toLowerCase().equals(propertyName != null ? propertyName.toLowerCase() : "" )) return reportFilter;
        }return null;
    }

    public void setProperties(ReportFilter reportFilter, Map<String, Object> reportFilterMap, ReportDefinition reportDefinition) {
        Long thingTypeIdReport = 0L;
        Long thingTypeFieldId = 0L;
        Long parentThingTypedId = 0L;
        Integer fieldType = 0;

        reportFilter.setPropertyName( (String) reportFilterMap.get( "propertyName" ) );
        reportFilter.setValue( (String) reportFilterMap.get( "value" ) );
        reportFilter.setDisplayOrder( reportDefinition.getDisplayOrder( reportFilterMap.get( "displayOrder" ) ) );
        reportFilter.setOperator( (String) reportFilterMap.get( "operator" ) );
        reportFilter.setLabel( (String) reportFilterMap.get( "label" ) );
        reportFilter.setEditable( (Boolean) reportFilterMap.get( "editable" ) );

        if(reportFilterMap.get("thingTypeId") != null) {
            thingTypeIdReport  = ((Number) reportFilterMap.get("thingTypeId") ).longValue();
            reportFilter.setThingType( ThingTypeService.getInstance().get( thingTypeIdReport ) );
        }
        if(reportFilterMap.get("thingTypeFieldId") != null) {
            thingTypeFieldId  = ((Number) reportFilterMap.get("thingTypeFieldId") ).longValue();
            reportFilter.setThingTypeField( ThingTypeFieldService.getInstance().get( thingTypeFieldId ) );
        }

        if(reportFilterMap.get("fieldType") != null) {
            fieldType  = ((Number) reportFilterMap.get("fieldType") ).intValue();
            reportFilter.setFieldType( fieldType );
        }else{
            reportFilter.setFieldType( null );
        }
        if(reportFilterMap.get("parentThingTypeId") != null && !reportFilterMap.get("parentThingTypeId").toString().equals( "" )  )
        {
            parentThingTypedId  = ((Number) reportFilterMap.get("parentThingTypeId") ).longValue();
            reportFilter.setParentThingType( ThingTypeService.getInstance().get( parentThingTypedId ) );
        }
        reportFilter.setReportDefinition(reportDefinition);
    }

    /******************************************************
     * Method to get a list of filters which have a thingTypeFieldID
     * ****************************************************/
    public List<ReportFilter> getFiltersByThingTypeId(Long thingTypeFieldId)
    {
        List<ReportFilter> lstOutputConfig = ReportFilterService.getReportFilterDAO().getQuery()
                .where(QReportFilter.reportFilter.thingTypeField.id.eq(thingTypeFieldId) )
                .list(QReportFilter.reportFilter);

        return lstOutputConfig;
    }

    /**
     * getFilterByPropertyName
     * @param propertyName zone property's name
     * @return List of Report filters with property name
     */
    public List<ReportFilter> getFiltersByPropertyName(String propertyName){
        HibernateQuery query = getReportFilterDAO().getQuery();
        BooleanBuilder reportWhereQuery = new BooleanBuilder(QReportFilter.reportFilter.propertyName.toLowerCase().eq(propertyName.toLowerCase()));
        return query.where(reportWhereQuery).list(QReportFilter.reportFilter);
    }

    public List<ReportFilter> getFiltersByPropertyNameAndZonePropertyId(String propertyName, Long zonePropertyId){
        BooleanBuilder reportWhereQuery = new BooleanBuilder(QReportFilter.reportFilter.propertyName.toLowerCase().eq(propertyName.toLowerCase()));
        reportWhereQuery = reportWhereQuery.and(QReportFilter.reportFilter.thingTypeField.id.eq(zonePropertyId));
        return getReportFilterDAO().getQuery().where(reportWhereQuery).list(QReportFilter.reportFilter);
    }

    public List<ReportFilter> getFiltersByPropertyNameValueLabel(String propertyName, String value, String label){
        HibernateQuery query = getReportFilterDAO().getQuery();
        BooleanBuilder reportWhereQuery = new BooleanBuilder(
                QReportFilter.reportFilter.propertyName.toLowerCase().eq(propertyName.toLowerCase())
                        .and(QReportFilter.reportFilter.value.toLowerCase().eq(value.toLowerCase())).
                and(QReportFilter.reportFilter.label.toLowerCase().eq(label.toLowerCase())));
        return query.where(reportWhereQuery).list(QReportFilter.reportFilter);
    }

    /**
     * Create report filter
     * @param label
     * @param propertyName
     * @param propertyOrder
     * @param operatorFilter
     * @param value
     * @param isEditable
     * @param ttId
     * @param reportDefinition
     * @return
     */
    public ReportFilter createReportFilter( String label, String propertyName, String propertyOrder, String operatorFilter, String value,
                                            Boolean isEditable, Long ttId, ReportDefinition reportDefinition )
    {
        ReportFilter reportFilter = new ReportFilter();
        reportFilter.setLabel( label );
        reportFilter.setPropertyName( propertyName );
        reportFilter.setDisplayOrder( Float.parseFloat( propertyOrder ) );
        reportFilter.setOperator( operatorFilter );
        reportFilter.setValue( value );
        reportFilter.setEditable( isEditable );
        if (ttId != null) {
            ThingType thingtype = ThingTypeService.getInstance().get( ttId );
            reportFilter.setThingType(thingtype!=null?thingtype:null );
            reportFilter.setThingTypeField( ThingTypeFieldService.getInstance().getThingTypeFieldByName( propertyName ).get( 0 ) );
        }
        reportFilter.setReportDefinition( reportDefinition );
        return reportFilter;
    }

    public List<ReportFilter> getFiltersByOperator(String operator){
        HibernateQuery query = getReportFilterDAO().getQuery();
        BooleanBuilder reportWhereQuery = new BooleanBuilder(QReportFilter.reportFilter.operator.toLowerCase().eq(operator.toLowerCase()));
        return query.where(reportWhereQuery).list(QReportFilter.reportFilter);
    }

    @Override public void validateInsert(ReportFilter reportFilter) {
        super.validateInsert(reportFilter);
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QReportFilter.reportFilter.label.eq(reportFilter.getLabel()));
        be = be.and(QReportFilter.reportFilter.operator.eq(reportFilter.getOperator()));
        be = be.and(QReportFilter.reportFilter.reportDefinition.eq(reportFilter.getReportDefinition()));
        if (reportFilter.getId() == null && getReportFilterDAO().selectBy(be) != null) {
            throw new UserException("Report filter already exists");
        }
    }

    @Override public void validateUpdate(ReportFilter reportFilter) {
        super.validateUpdate(reportFilter);
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QReportFilter.reportFilter.label.eq(reportFilter.getLabel()));
        be = be.and(QReportFilter.reportFilter.operator.eq(reportFilter.getOperator()));
        be = be.and(QReportFilter.reportFilter.reportDefinition.eq(reportFilter.getReportDefinition()));
        if (reportFilter.getId() == null && getReportFilterDAO().selectBy(be) != null) {
            throw new UserException("Report filter already exists");
        }
    }
}
