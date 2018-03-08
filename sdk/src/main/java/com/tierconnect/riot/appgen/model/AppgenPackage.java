package com.tierconnect.riot.appgen.model;

import java.util.ArrayList;
import java.util.List;

public class AppgenPackage 
{
	String packageName;
	
	List<Clazz> classes;

	public AppgenPackage( String packageName ) 
	{
		this.packageName = packageName;
	}
	
	public String getPackageName() 
	{
		return packageName;
	}

	public void setPackageName( String packageName ) 
	{
		this.packageName = packageName;
	}

	public String getPackagePath()
	{
		return getPackageName().replaceAll( "\\.", "/" );
	}
	
	public List<Clazz> getClasses() 
	{
		return classes;
	}

	public void addClazz( Clazz clazz ) 
	{
		if( classes == null )
			classes = new ArrayList<Clazz>();	
		classes.add( clazz );
		clazz.setPackage( this );	
	}
}
