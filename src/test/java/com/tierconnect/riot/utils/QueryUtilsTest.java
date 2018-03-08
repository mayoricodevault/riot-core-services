package com.tierconnect.riot.utils;

import com.tierconnect.riot.appcore.entities.QGroup;
import org.junit.Test;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.iot.entities.QThing;
import com.tierconnect.riot.iot.services.ThingService;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.appcore.utils.QueryUtils;

import java.util.Map;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class QueryUtilsTest {
    @Test
    public void splitWithEscapingTest() {
        {
            Map result = QueryUtils.splitWithEscaping("a=b", '=');
            String[] res = (String[]) result.get("tokens");
            assertEquals("a", res[0]);
            assertEquals("b", res[1]);
        }
        {
            Map result = QueryUtils.splitWithEscaping("a~b", '=', '~');
            String[] res = (String[]) result.get("tokens");
            assertEquals("a", res[0]);
            assertEquals("b", res[1]);
        }
        {
            Map result = QueryUtils.splitWithEscaping("a=bc\\=d", '=');
            String[] res = (String[]) result.get("tokens");
            assertEquals("a", res[0]);
            assertEquals("bc=d", res[1]);
        }
    }
    @Test
    public void getEndParenthesisIndexTest() {
        assertEquals(1,QueryUtils.getIndexOfEndParenthesis("()", 0));
        assertEquals(3,QueryUtils.getIndexOfEndParenthesis("(())", 0));
        assertEquals(2,QueryUtils.getIndexOfEndParenthesis("(())", 1));
        assertEquals(8,QueryUtils.getIndexOfEndParenthesis("(a+b=c+d)", 0));
        assertEquals(-1, QueryUtils.getIndexOfEndParenthesis("(", 0));
    }

	@Test
	public void testGetOrderField(){
        assertNotNull(QThing.thing.group);
        assertNotNull(QGroup.group);
        assertNotNull(QGroup.group.groupType);
		assertNotNull(QThing.thing.group.groupType);

		String[] orderStrings = new String[]{"name","group.name","group.groupType.name"};
		for(String orderString : orderStrings){			
			QueryUtils.getOrderField(QThing.thing,orderString+":asc");
			QueryUtils.getOrderField(QThing.thing,orderString+":desc");
		}
		QueryUtils.getOrderFields(QThing.thing,"name:asc,group.name:desc");
		QueryUtils.getOrderFields(QThing.thing,"");
		QueryUtils.getOrderFields(QThing.thing,null);

		try{
			QueryUtils.getOrderField(QThing.thing,"names");
			fail();
		}catch(UserException e){}
		try{
			QueryUtils.getOrderField(QThing.thing,"group");
			fail();
		}catch(UserException e){}
		try{
			QueryUtils.getOrderField(QThing.thing,"name:asdasd");
			fail();
		}catch(UserException e){}
		try{
			QueryUtils.getOrderField(QThing.thing,"name");
			fail();
		}catch(UserException e){}
	}

	@Test
	public void testNumericCustomSearch(){
		ThingService.getInstance().getThingDAO().getSession().beginTransaction();

		BooleanBuilder be = QueryUtils.buildSearch(QThing.thing,"group.id=9");
		System.out.println(be.toString());
		Long count = ThingService.getInstance().countList(be.getValue());

		System.out.println(count);
		ThingService.getInstance().getThingDAO().getSession().getTransaction().commit();
	}

	@Test
	public void testStringCustomSearch(){
		ThingService.getInstance().getThingDAO().getSession().beginTransaction();
		
		BooleanBuilder be = QueryUtils.buildSearch(QThing.thing,"name=iPhone");
		System.out.println(be.toString());
		Long count = ThingService.getInstance().countList(be.getValue());

		System.out.println(count);
		ThingService.getInstance().getThingDAO().getSession().getTransaction().commit();
	}
	
	@Test
	public void testNullConditionCustomSearch(){
		ThingService.getInstance().getThingDAO().getSession().beginTransaction();
		
		BooleanBuilder be = QueryUtils.buildSearch(QThing.thing,"parent=\\N");
		System.out.println(be.toString());
		Long count = ThingService.getInstance().countList(be.getValue());

		System.out.println(count);
		ThingService.getInstance().getThingDAO().getSession().getTransaction().commit();
	}
	
	@Test
	public void testMultipleCustomSearch(){
		ThingService.getInstance().getThingDAO().getSession().beginTransaction();
		
		BooleanBuilder be = QueryUtils.buildSearch(QThing.thing,"name=iPhone&serial=0003&group.name=terry");
		System.out.println(be.toString());
		Long count = ThingService.getInstance().countList(be.getValue());

		System.out.println(count);
		ThingService.getInstance().getThingDAO().getSession().getTransaction().commit();
	}
	
	@Test
	public void testSpecialCharactersCustomSearch(){
		ThingService.getInstance().getThingDAO().getSession().beginTransaction();
		
		BooleanBuilder be = QueryUtils.buildSearch(QThing.thing,"name=iPh\\&one&serial=0003");
		System.out.println(be.toString());
		Long count = ThingService.getInstance().countList(be.getValue());

		System.out.println(count);
		ThingService.getInstance().getThingDAO().getSession().getTransaction().commit();
	}

	@Test 
	public void testInvalidCustomSearches(){
		try{
			QueryUtils.buildSearch(QThing.thing,"group=asde");
			fail();
		}catch(Exception e){}
		try{
			QueryUtils.buildSearch(QThing.thing,"groupasde");
			fail();
		}catch(Exception e){}		
	}
}
