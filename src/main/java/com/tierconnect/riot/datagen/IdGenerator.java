package com.tierconnect.riot.datagen;

import java.util.HashMap;
import java.util.Map;

public class IdGenerator
{
	private Map<String,Long> map = new HashMap<String,Long>();

	private static IdGenerator INSTANCE;

	static
	{
		INSTANCE = new IdGenerator();
	}

	public static void setId( long id )
	{
		//TODO:!
		//INSTANCE.id = id;
	}

	public static IdGenerator getInstance()
	{
		return INSTANCE;
	}

	public long nextValue( String className )
	{
		Long id = map.get( className );
		
		if( id == null )
		{
			id = 1L;
		}
		
		map.put( className, id + 1 );
		
		//System.out.println( "name=" + className + " id=" + id );
		
		return id;
	}
}
