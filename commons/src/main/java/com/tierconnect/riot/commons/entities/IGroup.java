package com.tierconnect.riot.commons.entities;

public interface IGroup
{
	public Long getId();
	
	public String getName();
	
	public String getCode();
	
	public IGroupType getGroupType();
}
