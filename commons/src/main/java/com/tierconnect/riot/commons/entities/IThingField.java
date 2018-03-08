package com.tierconnect.riot.commons.entities;

public interface IThingField
{
	//Long getId();
	
	IThingTypeField getThingTypeField();
	
	Object getValue();

	Long getTimestamp();
}
