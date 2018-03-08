package com.tierconnect.riot.appgen.service;

import java.util.List;

import com.tierconnect.riot.appgen.dao.ClazzDAO;
import com.tierconnect.riot.appgen.model.Clazz;

public class ClazzService 
{
	static private ClazzDAO dao;

	public static ClazzDAO getClazzDAO()
	{
		if(dao == null)
		{
			dao = new ClazzDAO();
		}
		return dao;
	}
	
	public static Clazz get(Long id) 
	{
		Clazz clazz = getClazzDAO().selectById(id);
		return clazz;
	}
	
	public static Clazz insert(Clazz clazz) 
	{
        Long id = getClazzDAO().insert(clazz);
        clazz.setId(id);
        return clazz;
    }
	
	public static List<Clazz> list()
	{
		List<Clazz> list = getClazzDAO().selectAll();
		return list;
	}

	public static void deleteAll()
	{
		getClazzDAO().deleteAll();
	}
}
