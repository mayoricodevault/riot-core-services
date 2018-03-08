package com.tierconnect.riot.iot.entities;



import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.annotation.Generated;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity

@Table(name="reportEntryOptionProperty")@Generated("com.tierconnect.riot.appgen.service.GenModel")
public class ReportEntryOptionProperty extends ReportEntryOptionPropertyBase
{
    @ManyToOne
    @NotNull
    protected ReportEntryOption reportEntryOption;

    public ReportEntryOption getReportEntryOption() {
        return reportEntryOption;
    }

    public void setReportEntryOption(ReportEntryOption reportEntryOption) {
        this.reportEntryOption = reportEntryOption;
    }

    public void setProperties(Map<String, Object> reportEntryOptionPropertyMap, ReportEntryOption reportEntryOption) {
        Long thingTypeIdReport = 0L;
        Long thingTypeFieldId = 0L;

        setPropertyName((String) reportEntryOptionPropertyMap.get("propertyName"));
        setLabel((String) reportEntryOptionPropertyMap.get("label"));
        setEditInline((Boolean)reportEntryOptionPropertyMap.get("editInline"));
        setDisplayOrder(Float.parseFloat(reportEntryOptionPropertyMap.get("displayOrder").toString()));
        setSortBy((String) reportEntryOptionPropertyMap.get("sortBy"));
        setRequired((Boolean) reportEntryOptionPropertyMap.get("required"));
        setDefaultMobileValue((String) reportEntryOptionPropertyMap.get("defaultMobileValue"));
        setPickList((Boolean) reportEntryOptionPropertyMap.get("pickList"));
        setAllPropertyData((Boolean) reportEntryOptionPropertyMap.get("allPropertyData"));
        if(reportEntryOptionPropertyMap.get("thingTypeId") != null) {
            thingTypeIdReport  = ((Number) reportEntryOptionPropertyMap.get("thingTypeId") ).longValue();
            setThingTypeIdReport(thingTypeIdReport);
        }

        if(reportEntryOptionPropertyMap.get("thingTypeFieldId") != null) {
            thingTypeFieldId  = ((Number) reportEntryOptionPropertyMap.get("thingTypeFieldId") ).longValue();
            setThingTypeFieldId(thingTypeFieldId);
        }

        List entryFormPropertyDataMap = ( List )reportEntryOptionPropertyMap.get("entryFormPropertyData");
        if(entryFormPropertyDataMap!=null && entryFormPropertyDataMap.size()>0)
        {
            Map<String, Object> data = null;
            EntryFormPropertyData entryFormPropertyData = null;
            List<EntryFormPropertyData> entryFormPropertyDataLst = new ArrayList<EntryFormPropertyData>();
            for(Object dataMap: entryFormPropertyDataMap)
            {
                data = (Map<String, Object>) dataMap;
                entryFormPropertyData = new EntryFormPropertyData();
                if(data.get("value")!=null)
                {
                    entryFormPropertyData.setValue(data.get("value").toString());
                }
                entryFormPropertyData.setName((String) data.get("name"));
                entryFormPropertyDataLst.add(entryFormPropertyData);
            }
            setEntryFormPropertyDatas(entryFormPropertyDataLst);
        }

        setReportEntryOption(reportEntryOption);
    }

}

