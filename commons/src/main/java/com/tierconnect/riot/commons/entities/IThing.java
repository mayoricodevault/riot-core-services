package com.tierconnect.riot.commons.entities;

import java.util.List;
import java.util.Map;

public interface IThing
{
	Long getId();

	String getName();

	String getSerialNumber();

	IGroup getGroup();

	IThingType getThingType();

	long getTime();


	Map<String, IThingField> getThingFields();

	IThing getParent();

	List<IThing> getChildren();

	@Deprecated // TODO: does not appear to be used, remove in 4.2
	Map<String, Object> getValueForNativeObject(String key);
}
