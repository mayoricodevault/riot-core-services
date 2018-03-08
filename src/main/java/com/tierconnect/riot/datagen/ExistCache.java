package com.tierconnect.riot.datagen;

import java.util.Set;
import java.util.TreeSet;

//TODO: add lazy loading, eviction
public class ExistCache
{
	// exist cache
	private Set<String> exists = new TreeSet<String>();
	
	public void add( Thing thing )
	{
		String key = thing.tt.getId() + ":" + thing.id;
		exists.add( key );
	}
	
	public boolean exists( Thing thing )
	{
		String key = thing.tt.getId() + ":" + thing.id;
		return exists.contains( key ) ;
	}
}
