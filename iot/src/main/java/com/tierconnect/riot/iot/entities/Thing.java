package com.tierconnect.riot.iot.entities;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.*;
import java.util.List;

/**
 * 
 * DO NOT PUT DAO CALLS HERE, PUT IT IN YOUR SERVICE METHODS !!!!!!
 * 
 */
@Entity
@Table(name="apc_thing"
		,uniqueConstraints=@UniqueConstraint(columnNames={"serial","thingType_id"}))
public class Thing extends ThingBase
{
	public String getSerialNumber()
	{
		return this.getSerial();
	}

    @Override
    public long getTime() {
        return getModifiedTime();
    }

    @Override
    //TODO Returns ThingField Map reading from mongo on every call, improve
    public Map<String, com.tierconnect.riot.commons.entities.IThingField> getThingFields() {

        Map<String, com.tierconnect.riot.commons.entities.IThingField> result = new HashMap<>();

        Object obj = getMongoDoc();

        if(obj != null){
        Map<String, Object> doc = (Map) obj;

            for (ThingTypeField thingTypeField : getThingType().getThingTypeFields()){
                //ask if thing contains thingTypeField value in mongo
                if(doc.containsKey(thingTypeField.getName())){
                    Object value = ((Map)doc.get(thingTypeField.getName())).get("value");
                    Long time = ((Date)((Map) doc.get(thingTypeField.getName())).get("time")).getTime();
                    ThingField thingField = new ThingField(value, thingTypeField, time);
                    result.put(thingTypeField.getName(), thingField);
                }
            }
        }
        return result;
    }

    @Override
    @Deprecated
    /**
     * Do not use this method, it has been deprecated because of DeadLock Exception
     */
	public List<com.tierconnect.riot.commons.entities.IThing> getChildren()
	{

        Class clazz = null;
        Object obj = null;
        try {
            clazz = Class.forName("com.tierconnect.riot.iot.services.ThingService");
            obj = clazz.getMethod("getChildrenList", Thing.class).invoke(null,this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<com.tierconnect.riot.commons.entities.IThing> result = new ArrayList<>();
        if (obj != null) {
            result.addAll((List) obj);
        }
        return result;
	}
	
	public boolean hasThingTypeField(String thingFieldName)
	{
		for( ThingTypeField thingTypeField : this.getThingType().getThingTypeFields() )
		{
			if( thingFieldName.equals(thingTypeField.getName()) )
			{
				return true;
			}
		}
		return false;
	}

    //Todo improve
	public ThingTypeField getThingTypeField(String thingFieldName)
	{
		for( ThingTypeField thingTypeField : this.getThingType().getThingTypeFields() )
		{
			if( thingFieldName != null && thingFieldName.equals(thingTypeField.getName()) )
			{
				return thingTypeField;
			}
		}
		return null;
	}

    public List<ThingTypeField> getThingTypeFieldByType(Long thingFieldType)
    {
        List<ThingTypeField> thingTypeFields = new LinkedList<>();
        for( ThingTypeField thingTypeField : this.getThingType().getThingTypeFields() )
        {
            if( thingTypeField.getDataType().getId().compareTo(Long.parseLong( thingFieldType.toString() )) == 0 )
            {
                thingTypeFields.add(thingTypeField);
            }
        }
        return thingTypeFields;
    }


	/**
	 * Find a thing field by the thing type id
	 * @param thingTypeFieldId type field id
	 * @return thing field that matches thing type
	 */
	public ThingTypeField getThingTypeField(Long thingTypeFieldId)
	{
		ThingTypeField thingTypeField = null;

		for( ThingTypeField aThingField : this.getThingType().getThingTypeFields() )
		{
			Long aThingTypeFieldId = aThingField.getId();
			if( aThingTypeFieldId != null && aThingTypeFieldId.equals(thingTypeFieldId) )
			{
				thingTypeField = aThingField;
				break;
			}
		}
		return thingTypeField;
	}

    public ThingTypeField getThingTypeFieldFromId(Long thingTypeFieldId)
    {
        ThingTypeField thingTypeField = null;

        for( ThingTypeField aThingField : this.getThingType().getThingTypeFields() )
        {
            Long aThingTypeFieldId = aThingField.getId();
            if( aThingTypeFieldId != null && aThingTypeFieldId.equals(thingTypeFieldId) )
            {
                thingTypeField = aThingField;
                break;
            }
        }
        return thingTypeField;
    }

	@Override
    @Deprecated
    //TODO Marked for remove
    public Map<String, Object> getValueForNativeObject( String s )
	{
        Map<String, Object> result = new HashMap<>();

        Object obj = getMongoDoc();
        if(obj != null){
            Map<String, Object> doc = (Map) obj;

            if(doc.containsKey(s)){
                result.putAll((Map) doc.get(s));
            }
        }

        return result;
    }

	/*
		* Public Map so as to get more information
		* */
	public Map<String,Object> publicMapExtraValues()
	{
		Map<String, Object> map = new HashMap<String, Object>();
		map.put( "id", getId() );
		map.put( "name", getName() );
		map.put( "serial", getSerial() );
		map.put( "activated", isActivated() );
		map.put( "modifiedTime", getModifiedTime() );
		map.put( "thingTypeCode", getThingType().getThingTypeCode() );
		map.put( "thingTypeName", getThingType().getName() );
		map.put( "thingTypeId", getThingType().getId() );
		return map;
	}

    //TODO review if needed
    private Object getMongoDoc(){
        //Get values from mongo
        Class clazz = null;
        Object obj = null;
        try {
            clazz = Class.forName("com.tierconnect.riot.iot.dao.mongo.ThingMongoDAO");
            Object instance = clazz.getMethod("getInstance").invoke(null);
            obj = instance.getClass().getMethod("getThing", Long.class).invoke(instance, getId());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return obj;
    }

    /*
    * Get Value of thing type
    * */
    public Object getValueOfThingField(String name)
    {
        Object response = null;
        Iterator it = this.getThingFields().entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry pair = (Map.Entry)it.next();
            if(pair.getKey().equals( name ))
            {
                response = pair.getValue();
                break;
            }
        }
        return response;
    }
}
