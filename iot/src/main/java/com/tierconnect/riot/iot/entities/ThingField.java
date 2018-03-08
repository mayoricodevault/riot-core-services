package com.tierconnect.riot.iot.entities;

import java.util.Map;

import com.tierconnect.riot.commons.entities.IThingField;

public class ThingField implements IThingField
{
	private ThingTypeField thingTypeField;
	private Object value;
	private Long timestamp;

	public ThingField()
	{
		
	}
	
	public ThingField( Object value, ThingTypeField thingTypeField, Long timestamp )
	{
		this.thingTypeField = thingTypeField;
		this.value = value;
		this.timestamp = timestamp;
	}

	public ThingField( Map<String, Object> value, ThingTypeField thingTypeField )
	{
		this.thingTypeField = thingTypeField;
		this.value = value.get( "value" );
	}

	public ThingField( Object value, ThingTypeField thingTypeField )
	{
		this.thingTypeField = thingTypeField;
		this.value = value;
	}

	@Override
	public com.tierconnect.riot.commons.entities.IThingTypeField getThingTypeField()
	{
		return thingTypeField;
	}

	@Override
	public Object getValue()
	{
		return value;
	}

	public void setValue( Object value )
	{
		this.value = value;
	}
	
	@Override
	public Long getTimestamp()
	{
		return timestamp;
	}
	
	public void setTimestamp( Long timestamp )
	{
		this.timestamp = timestamp;
	}
}
