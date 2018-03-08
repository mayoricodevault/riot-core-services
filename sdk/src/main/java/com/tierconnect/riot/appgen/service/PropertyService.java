package com.tierconnect.riot.appgen.service;

import java.util.ArrayList;
import java.util.List;

import com.tierconnect.riot.appgen.dao.PropertyDAO;
import com.tierconnect.riot.appgen.model.Clazz;
import com.tierconnect.riot.appgen.model.Property;

public class PropertyService 
{
	static private PropertyDAO dao;

	public static PropertyDAO getPropertyDAO()
	{
		if(dao == null)
		{
			dao = new PropertyDAO();
		}
		return dao;
	}
	
	public static Property get(Long id) 
	{
		Property property = getPropertyDAO().selectById(id);
		return property;
	}
	
	public static Property insert(Property property) 
	{
        Long id = getPropertyDAO().insert(property);
        property.setId(id);
        return property;
    }
	
	public static List<Property> list()
	{
		List<Property> list = getPropertyDAO().selectAll();
		return list;
	}
	
	public static void deleteAll()
	{
		getPropertyDAO().deleteAll();
	}
	
	public static List<Property> listByClass( Clazz clazz )
	{
		List<Property> list = getPropertyDAO().selectAll();
		
		List<Property> list2 = new ArrayList<Property>();
		
		for( Property p : PropertyService.list() )
		{
			//if( p.getClazz().getId() == clazz.getId() )
			{
				list2.add( p );
			}
		}
		
		return list2;
	}
}
