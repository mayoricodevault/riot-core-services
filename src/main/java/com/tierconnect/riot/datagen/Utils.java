package com.tierconnect.riot.datagen;

import java.util.LinkedHashSet;
import java.util.Set;

import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.entities.ThingTypeField;

public class Utils
{
	static protected ThingTypeField getThingTypeField( String name )
	{
		ThingTypeField ttf = new ThingTypeField();
		ttf.setId( IdGenerator.getInstance().nextValue( ThingTypeField.class.getName() ) );
		ttf.setName( name );
		return ttf;
	}

	static protected ThingType getThingType( String name, ThingTypeField[] thingTypeFields )
	{
		ThingType tt = new ThingType();
		tt.setId( IdGenerator.getInstance().nextValue( ThingTypeField.class.getName() ) );
		tt.setName( name );

		Set<ThingTypeField> set = new LinkedHashSet<ThingTypeField>();
		for( ThingTypeField ttf : thingTypeFields )
		{
			set.add( ttf );
		}
		tt.setThingTypeFields( set );

		return tt;
	}
}
