package com.tierconnect.riot.commons.entities;

public interface IThingType
{
	public Long getId();

	public String getName();

	public String getCode();

	public IThingTypeField getThingTypeField( String propertyName );

	public Boolean isThingTypeParent();

}
