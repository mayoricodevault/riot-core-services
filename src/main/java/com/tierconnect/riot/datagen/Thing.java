package com.tierconnect.riot.datagen;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tierconnect.riot.iot.entities.ThingField;
import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.entities.ThingTypeField;

/*
 * 
 * TODO: try to use Thing from services !!!!
 * 
 */

public class Thing
{
	long id;

	String name;
	String serial;
	Date createdOn;

	ThingType tt;
	Thing parent;
	int groupId;

	long timestamp;
	
	List<ThingField> udfs = new ArrayList<>();
	Map<String, ThingField> map = new HashMap<String, ThingField>();

	public Thing( ThingType tt, int groupId, Thing parent, DateGenerator dg )
	{
		this.id = IdGenerator.getInstance().nextValue( tt.getName() );
		this.tt = tt;
		this.name = tt.getName() + id;
		this.serial = tt.getName() + id;
		this.groupId = groupId;
		this.createdOn = dg.generate();

		this.parent = parent;

		for( ThingTypeField ttf : tt.getThingTypeFields() )
		{
			Object value = UDFValueGenerator.instance().nextValue( ttf.getId() );
			ThingField tf = new ThingField( value, ttf, dg.generate().getTime() );
			udfs.add( tf );
			//map.put( key, value );
		}
	}

	public Thing( long time, ThingType tt, int groupId, Thing parent )
	{
		this.id = IdGenerator.getInstance().nextValue( tt.getName() );
		this.tt = tt;
		this.name = tt.getName() + id;
		this.serial = tt.getName() + id;
		this.groupId = groupId;
		this.createdOn = new Date( time );

		this.parent = parent;
		this.timestamp = time;
	}
	
	public void setUdfValue( long timestamp, String fieldName, String value )
	{
		if( ! map.containsKey( fieldName  ))
		{
			ThingTypeField ttf = (ThingTypeField) tt.getThingTypeField( fieldName );
			ThingField tf = new ThingField( value, ttf, timestamp );
			udfs.add( tf );
			map.put( fieldName, tf );
		}
		else
		{
			ThingField tf = map.get( fieldName );
			tf.setTimestamp( timestamp );
			tf.setValue( value );
		}
		
		this.timestamp = timestamp;
	}
}
