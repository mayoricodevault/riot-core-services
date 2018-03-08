package com.tierconnect.riot.iot.entities;

import javax.annotation.Generated;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by rchirinos on 30/10/2015.
 */
@Entity
@Table(name="apc_reportGroupBy")
@Generated("com.tierconnect.riot.appgen.service.GenModel")
public class ReportGroupBy extends ReportGroupByBase
{
	public Map<String,Object> publicDataMap()
	{
		Map<String, Object> map = new HashMap<String, Object>();
		map.put( "id", getId() );
		map.put( "label", getLabel() );
		map.put( "propertyName", getPropertyName() );
		map.put( "sortBy", getSortBy() );
		map.put( "ranking", getRanking() );
		map.put( "other", getOther() );
		map.put( "unit", getUnit() );
		map.put( "thingTypeId", getThingType() != null ? getThingType().getId() : 0L );
		map.put( "thingTypeFieldId", getThingTypeField()!=null?getThingTypeField().getId():null);
		map.put( "parentThingTypeId", getParentThingType()!=null?getParentThingType().getId():null );
        map.put( "byPartition", getByPartition() );
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
    public void setByPartition( Boolean byPartition ){
        this.byPartition = byPartition!=null&&byPartition;
    }
}
