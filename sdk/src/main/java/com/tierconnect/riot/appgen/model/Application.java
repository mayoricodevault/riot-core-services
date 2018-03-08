package com.tierconnect.riot.appgen.model;

import java.util.ArrayList;
import java.util.List;

public class Application 
{
	List<AppgenPackage> packages;

	public List<AppgenPackage> getPackages() 
	{
		return packages;
	}

	public void setPackages( List<AppgenPackage> packages ) 
	{
		this.packages = packages;
	}
	
	public void addPackage( AppgenPackage p )
	{
		if( packages == null )
			packages = new ArrayList<AppgenPackage>();
		
		packages.add( p );
	}

	public void addClassses(Application ap) {
		// TODO Auto-generated method stub
		
	}
}
