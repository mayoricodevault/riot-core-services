package com.tierconnect.riot.iot.services;


import com.tierconnect.riot.iot.entities.QReportEntryOptionProperty;
import com.tierconnect.riot.iot.entities.ReportEntryOptionProperty;

import java.util.List;

public class ReportEntryOptionPropertyService extends ReportEntryOptionPropertyServiceBase
{

    /******************************************************
     * Method to get a list of Report Entry Option Properties which have a thingTypeFieldID
     * ****************************************************/
    public List<ReportEntryOptionProperty> getReportEntryOptionPropertiesByThingTypeId(Long thingTypeFieldId)
    {
        List<ReportEntryOptionProperty> lstOutputConfig = ReportEntryOptionPropertyService.getReportEntryOptionPropertyDAO().getQuery()
                .where(QReportEntryOptionProperty.reportEntryOptionProperty.thingTypeFieldId.eq(thingTypeFieldId) )
                .list(QReportEntryOptionProperty.reportEntryOptionProperty);

        return lstOutputConfig;
    }

    /**
     *
     * @return a List of all ReportEntryOptionProperties
     */
    public List<ReportEntryOptionProperty> getAllReportEntryOptionProperties(){
        return ReportEntryOptionPropertyService.getReportEntryOptionPropertyDAO().getQuery()
                .list(QReportEntryOptionProperty.reportEntryOptionProperty);
    }
}
