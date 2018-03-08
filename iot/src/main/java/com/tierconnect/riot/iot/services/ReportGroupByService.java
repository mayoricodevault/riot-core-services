package com.tierconnect.riot.iot.services;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.utils.ReportExecutionUtils;
import com.tierconnect.riot.iot.utils.ReportRuleUtils;

import javax.annotation.Generated;
import java.util.*;

@Generated("com.tierconnect.riot.appgen.service.GenService")
public class ReportGroupByService extends ReportGroupByServiceBase 
{

    public Map<String, List<Long> > getThingFieldIds(List<ReportGroupBy> reportGroupByList, CompositeThing compositeThing) {
        Map<String, List<Long> > thingFieldsMap = new HashMap<>();
        Set<Long> thingFieldIdsTimeSeries = new LinkedHashSet<>();
        Set<Long> thingFieldIdsNotTimeSeries = new LinkedHashSet<>();

        for (ReportGroupBy reportGroupBy : reportGroupByList) {
            String propertyName = reportGroupBy.getPropertyName();
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

    public Map<String, List<Long> > getThingTypeFieldTypeIds(List<ReportGroupBy> reportGroupByList, CompositeThing compositeThing) {
        Map<String, List<Long> > thingFieldsMap = new HashMap<>();
        Set<Long> thingFieldIdsTimeSeries = new LinkedHashSet<>();
        Set<Long> thingFieldIdsNotTimeSeries = new LinkedHashSet<>();

        for (ReportGroupBy reportGroupBy : reportGroupByList) {
            String propertyName = reportGroupBy.getPropertyName();
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

    public static List<ThingTypeField> getZoneThingTypeFields(List<ReportGroupBy> reportProperties, CompositeThing compositeThing) {
        List<ThingTypeField> thingFieldsZone = new LinkedList<>();


        for (ReportGroupBy reportProperty : reportProperties) {
            String propertyName = reportProperty.getPropertyName();
            propertyName = ReportExecutionUtils.removeDwellTimeString(propertyName);
            propertyName = ReportExecutionUtils.removeTimeStamp(propertyName);
            ThingTypeField thingField = compositeThing.getThingTypeFieldByName(propertyName);
            if(thingField != null && (thingField.getName().equals("zone")
                    || thingField.getDataType().getId().compareTo( ThingTypeField.Type.TYPE_ZONE.value)==0)) {
                thingFieldsZone.add(thingField);
            }
        }
        return thingFieldsZone;
    }

    public List<Long> getZonePropertiesIds(List<ReportGroupBy> reportGroupByList) {
        List<Long> zonePropertiesId = new LinkedList<>();
        for(ReportGroupBy reportGroupBy : reportGroupByList) {
            String reportPropertyName = reportGroupBy.getPropertyName();
            if(reportPropertyName.contains("zoneProperty")) {
                String zonePropertyIdArray[] = reportPropertyName.split(",");
                if(zonePropertyIdArray.length > 1 && ReportRuleUtils.isNumeric(zonePropertyIdArray[1])) {
                    zonePropertiesId.add(Long.valueOf(zonePropertyIdArray[1]));
                }
            }
        }
        return zonePropertiesId;
    }

    /**
     * get GroupBy Reports that have zone property's name
     * @param propertyName zoneproperty's name table
     * @return List of Report GroupBy that have zone property's name
     */
    public List<ReportGroupBy> getGroupByPropertyName(String propertyName){
        HibernateQuery query = getReportGroupByDAO().getQuery();
        BooleanBuilder reportWhereQuery = new BooleanBuilder(QReportGroupBy.reportGroupBy.propertyName.toLowerCase().eq(propertyName.toLowerCase()));
        return query.where(reportWhereQuery).list(QReportGroupBy.reportGroupBy);
    }

    public List<ReportGroupBy> getReportGroupByThingTypeId(Long thingTypeFieldId)
    {
        List<ReportGroupBy> lstReportGroupBy = ReportGroupByService.getReportGroupByDAO().getQuery()
                .where(QReportGroupBy.reportGroupBy.thingTypeField.id.eq(thingTypeFieldId) )
                .list(QReportGroupBy.reportGroupBy);

        return lstReportGroupBy;
    }

}

