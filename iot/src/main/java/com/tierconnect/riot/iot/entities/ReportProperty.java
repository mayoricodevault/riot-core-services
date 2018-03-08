package com.tierconnect.riot.iot.entities;

import javax.annotation.Generated;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name="reportProperty")
@Generated("com.tierconnect.riot.appgen.service.GenModel")
public class ReportProperty extends ReportPropertyBase implements Comparable<ReportProperty>
{
    public Map<String,Object> publicDataMap()
    {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("id", getId());
        map.put("label", getLabel());
        map.put("displayOrder", getDisplayOrder());
        map.put( "propertyName", getPropertyName() );
        map.put("sortBy", getSortBy());
        map.put("editInline", getEditInline());
        map.put("enableHeat", getEnableHeat());
        map.put("showHover", getShowHover());
        map.put("parentThingTypeId", getParentThingType() != null ? getParentThingType().getId() : null);
        map.put( "thingTypeId", getThingType() != null ? getThingType().getId() : 0L );
        map.put("thingTypeFieldId", getThingTypeField() != null ? getThingTypeField().getId() : null);
        return map;
    }

    public boolean isNative() {
        return ReportDefinitionUtils.isNative(getPropertyName());
    }

    public boolean isDwell() {
        return ReportDefinitionUtils.isDwell(getPropertyName());
    }

    public boolean isThingTypeUdf() {
        return ReportDefinitionUtils.isThingTypeUdf(getParentThingType());
    }

    @Override
    public int compareTo(ReportProperty reportProperty) {
        return this.getDisplayOrder().compareTo(reportProperty.getDisplayOrder());
    }
}
