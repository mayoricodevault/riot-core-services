package com.tierconnect.riot.sdk.utils;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.log4j.Logger;
import org.hibernate.Session;

import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;

//maybe bad choice of name if we ever plan to use the apache commons framework as well
public class BeanUtils 
{
	static Logger logger = Logger.getLogger( BeanUtils.class );
	
	/**
	 * Given a map, copy the key values pairs to the corresponding bean setter methods
	 * 
	 * @param map
	 * @param bean
	 * 
	 * HANDLE "group_id" !
	 * 
	 * ***** THIS CLASS IN IN PROCESS ******
	 * FIRST PASS IS TO HANDLE INSERT CASE
	 * 
	 */
	/**
	 * @param map
	 * @param bean
	 */
	public static void setProperties( Map<String,Object> map, Object bean )
	{
		//DEBUG OUTPUT
		for( String key : map.keySet() )
		{
			logger.debug( "MAP: '" + key + "'='" + map.get(key) + "'" );
		}
		
		for( Entry<String, Object> e : map.entrySet() )
		{
			try 
			{
				logger.debug( "FOR BEAN OF TYPE '" + bean.getClass().getCanonicalName() + "'" );
					
				String mname;
				Class<?> clazz;
				if( e.getKey().endsWith( ".id" ) )
				{
					String str = e.getKey().substring(0 , e.getKey().indexOf( "." ) );
					mname = getSetter( str );
					clazz = getClassOfProperty( bean, str );
				}
				else
				{
					mname = getSetter( e.getKey() );
					clazz = getClassOfProperty( bean, e.getKey() );
				}
                if(clazz == null){
                    continue;
                }
				logger.debug( "map: key='" + e.getKey() + "' value='" + e.getValue() + "' value.class='"
						+ ((e.getValue() == null) ? "NULL" : e.getValue().getClass().getCanonicalName()) + "'" );
				logger.debug( "required class='" + clazz.getCanonicalName() + "'" );
				logger.debug( "mname='" + mname + "'" );

				Method m = bean.getClass().getMethod( mname, clazz );
				logger.debug( "method='" + m + "'" );

				if( e.getValue() == null )
				{
					logger.debug( "CASE NULL" );
					logger.debug( "calling m='" + m + "' with value='" + e.getValue() + "'" );
					m.invoke( bean, e.getValue() );
				}
				// handle to-One types (i.e. entities, e.g. dog.setGroup( group ) )
                else if( e.getKey().endsWith(".id") )
				{
					logger.debug( "CASE 'x.id'" );
					Long id = ( (Number) e.getValue() ).longValue();
					logger.debug( "id=" + id );
					Session session = HibernateSessionFactory.getInstance().getCurrentSession();
					Object entity = session.get( clazz, id );
					logger.debug( "calling m='" + m + "' with value='" + entity + "'" );
					m.invoke( bean, entity );
				}
				else if( Map.class.isAssignableFrom( e.getValue().getClass() ) )
				{
					if (clazz.equals(String.class)) {
						ObjectMapper objectMapper = new ObjectMapper();
						try {
							String value = objectMapper.writeValueAsString(e.getValue());
							m.invoke(bean, value);
						} catch (JsonProcessingException e2) {
							e2.printStackTrace();
						}

					} else {
						logger.debug("Map isAssignableFrom " + e.getValue().getClass().getCanonicalName());
						logger.debug("CASE 'xxxx:{id:xxx}'");
						// FOR INSERT CASE
						// get Group by id
						Map<?, ?> map1 = (Map<?, ?>) e.getValue();
						Object idObject = map1.get("id");
						if (idObject != null && idObject instanceof Number) {
							long id = ((Number) idObject).longValue();
							logger.debug("id=" + id);
							Session session = HibernateSessionFactory.getInstance().getCurrentSession();
							Object entity = session.get(clazz, id);
							logger.debug("calling m='" + m + "' with value='" + entity + "'");
							m.invoke(bean, entity);
						}
						// Can allow for recursive setting (is this pratical or needed ?)
						//setProperties((Map<String,Object>) e.getValue(), entity );
					}
				}
				else if( List.class.isAssignableFrom( e.getValue().getClass() ) )
				{
					if (clazz.equals(String.class)) {
						ObjectMapper objectMapper = new ObjectMapper();
						try {
							String value = objectMapper.writeValueAsString(e.getValue());
							m.invoke(bean, value);
						} catch (JsonProcessingException e2) {
							e2.printStackTrace();
						}

					}
				}
				else
				{
					logger.debug( "Map is NOT Assignable from " + e.getValue().getClass().getCanonicalName() );

					// TODO: make pluggable deserializers !
					if( Integer.class.getCanonicalName().equals( e.getValue().getClass().getCanonicalName() )
						&&
						Date.class.getCanonicalName().equals( clazz.getCanonicalName() )
					  )
					{
						logger.debug( "CASE DATE from INTEGER" );
						Date d = new Date( ((Number) e.getValue()).longValue() );
						logger.debug( "calling m='" + m + "' with value='" + d + "'" );
						m.invoke( bean, d );
					} else if (Long.class.getCanonicalName().equals(e.getValue().getClass().getCanonicalName())
							&&
							Date.class.getCanonicalName().equals(clazz.getCanonicalName())
							) {
						logger.debug("CASE DATE from LONG");
						Date d = new Date((Long) e.getValue());
						logger.debug("calling m='" + m + "' with value='" + d + "'");
						m.invoke(bean, d);
					} else if (Integer.class.getCanonicalName().equals(e.getValue().getClass().getCanonicalName())
							&&
							Long.class.getCanonicalName().equals(clazz.getCanonicalName())
							) {
						logger.debug("CASE LONG from INTEGER");
						Long v = new Long((Integer) e.getValue());
						logger.debug("calling m='" + m + "' with value='" + v + "'");
						m.invoke(bean, v);
					} else if (Double.class.getCanonicalName().equals(clazz.getCanonicalName())) {
						Double v;
						if (Integer.class.getCanonicalName().equals(e.getValue().getClass().getCanonicalName())) {
							logger.debug("CASE DOUBLE from INTEGER");
							v = new Double((Integer) e.getValue());
						} else {
							logger.debug("CASE DOUBLE from STRING");
							v = Double.parseDouble(e.getValue().toString());
						}
						logger.debug("calling m='" + m + "' with value='" + v + "'");
						m.invoke(bean, v);
					} else {
						logger.debug("CASE DEFAULT");
						logger.debug("calling m='" + m + "' with value='" + e.getValue() + "'");
						m.invoke(bean, e.getValue());
					}
				}

			}
			catch( IllegalArgumentException | SecurityException | IllegalAccessException | InvocationTargetException | NoSuchFieldException ex )
			{
				ex.printStackTrace();
			}
			catch( NoSuchMethodException ex2 )
			{
				
				ex2.printStackTrace();
			}
		}
	}
	
	private static Class<?> getClassOfProperty( Object bean, String property ) throws NoSuchFieldException
	{
		try 
		{
			Class<?> clazz = (Class<?>) ((Map<?, ?>)bean.getClass().getField( "classOfProperty" ).get( bean )).get( property );
			if( clazz == null )
			{
//				throw new NoSuchFieldException( "clazz is null for property=" + property + " and bean=" + bean);
                logger.warn("clazz is null for property=" + property + " and bean=" + bean);
			}
			return clazz;
		} 
		catch (IllegalArgumentException | SecurityException | IllegalAccessException e) 
		{
			throw new Error(property, e);
		}
	}
	
	private static String getSetter( String property )
	{
		return "set" + property.substring( 0, 1 ).toUpperCase() + property.substring( 1 );
	}

    public static Object getAndRemoveObjectFromMap(String objectName, Map<String, Object> map) {
        if(map.containsKey(objectName)) {
            Object res = map.get(objectName);
            map.remove(objectName);
            return res;
        }
        return null;
    }

	/**
	 * @param map  A map that contains the object to convert a instance class.
	 * @param bean Class to set properties.
	 */
	public static void setPOJOProperties(Map<String, Object> map, Object bean) {
		for (Entry<String, Object> e : map.entrySet()) {
			try {
				setProperty(bean, e);
			} catch (IllegalArgumentException | SecurityException |
					IllegalAccessException | InvocationTargetException |
					NoSuchFieldException | NoSuchMethodException ex) {
				logger.error("I could not be set the field: " + e.getKey() +
						" with value: " + e.getValue() +
						" and class: " + (e.getValue() != null ? e.getValue().getClass().getSimpleName() : "null"));
				throw new UserException("I could not be set the field: " + e.getKey() +
						" with value: " + e.getValue() +
						" and class: " + (e.getValue() != null ? e.getValue().getClass().getSimpleName() : "null"));
			}
		}
	}

    /**
	 * set only one object.
	 *
	 * @param bean object to set parameter
	 * @param e    entry set parameter.
	 */
	private static void setProperty(Object bean, Entry<String, Object> e)
			throws NoSuchFieldException, NoSuchMethodException,
			InvocationTargetException, IllegalAccessException {
		logger.debug("FOR BEAN OF TYPE '" + bean.getClass().getCanonicalName() + "'");

		String mname;
		Class<?> clazz;
		if (e.getKey().endsWith(".id")) {
			String str = e.getKey().substring(0, e.getKey().indexOf("."));
			mname = getSetter(str);
			clazz = getClassOfProperty(bean, str);
		} else {
			mname = getSetter(e.getKey());
			clazz = getClassOfProperty(bean, e.getKey());
		}
		if (clazz == null) {
			return;
		}
		logger.debug("map: key='" + e.getKey() + "' value='" + e.getValue() + "' value.class='"
				+ ((e.getValue() == null) ? "NULL" : e.getValue().getClass().getCanonicalName()) + "'");
		logger.debug("required class='" + clazz.getCanonicalName() + "'");
		logger.debug("mname='" + mname + "'");

		Method m = bean.getClass().getMethod(mname, clazz);
		logger.debug("method='" + m + "'");

		if (e.getValue() == null) {
			logger.debug("CASE NULL");
			logger.debug("calling m='" + m + "' with value='" + e.getValue() + "'");
			m.invoke(bean, e.getValue());
		}
		// handle to-One types (i.e. entities, e.g. dog.setGroup( group ) )
		else if (e.getKey().endsWith(".id")) {
			logger.debug("CASE 'x.id'");
			Long id = ((Number) e.getValue()).longValue();
			logger.debug("id=" + id);
			Session session = HibernateSessionFactory.getInstance().getCurrentSession();
			Object entity = session.get(clazz, id);
			logger.debug("calling m='" + m + "' with value='" + entity + "'");
			m.invoke(bean, entity);
		} else if (Map.class.isAssignableFrom(e.getValue().getClass())) {
			if (clazz.equals(String.class)) {
				ObjectMapper objectMapper = new ObjectMapper();
				try {
					String value = objectMapper.writeValueAsString(e.getValue());
					m.invoke(bean, value);
				} catch (JsonProcessingException e2) {
					e2.printStackTrace();
				}

			} else {
				logger.debug("Map isAssignableFrom " + e.getValue().getClass().getCanonicalName());
				logger.debug("CASE 'xxxx:{id:xxx}'");
				// FOR INSERT CASE
				// get Group by id
				Map<?, ?> map1 = (Map<?, ?>) e.getValue();
				Object idObject = map1.get("id");
				if (idObject != null && idObject instanceof Number) {
					long id = ((Number) idObject).longValue();
					logger.debug("id=" + id);
					Session session = HibernateSessionFactory.getInstance().getCurrentSession();
					Object entity = session.get(clazz, id);
					logger.debug("calling m='" + m + "' with value='" + entity + "'");
					m.invoke(bean, entity);
				}else if(e.getValue() instanceof LinkedHashMap){
					logger.debug("calling m='" + bean.getClass().getSimpleName() + "." + m.getName() + "'" +
							" with value='" + e.getValue() + "'" +
							" value type='" + e.getValue().getClass().getSimpleName() + "'");
					m.invoke(bean, e.getValue());
				}
				// Can allow for recursive setting (is this pratical or needed ?)
				//setProperties((Map<String,Object>) e.getValue(), entity );
			}
		} else if (List.class.isAssignableFrom(e.getValue().getClass())) {
			if (clazz.equals(String.class)) {
				ObjectMapper objectMapper = new ObjectMapper();
				try {
					String value = objectMapper.writeValueAsString(e.getValue());
					m.invoke(bean, value);
				} catch (JsonProcessingException e2) {
					e2.printStackTrace();
				}

			} else {
				m.invoke(bean, e.getValue());
			}
		} else {
			logger.debug("Map is NOT Assignable from " + e.getValue().getClass().getCanonicalName());

			// TODO: make pluggable deserializers !
			if (Integer.class.getCanonicalName().equals(e.getValue().getClass().getCanonicalName())
					&&
					Date.class.getCanonicalName().equals(clazz.getCanonicalName())
					) {
				logger.debug("CASE DATE from INTEGER");
				Date d = new Date(((Number) e.getValue()).longValue());
				logger.debug("calling m='" + m + "' with value='" + d + "'");
				m.invoke(bean, d);
			} else if (Long.class.getCanonicalName().equals(e.getValue().getClass().getCanonicalName())
					&&
					Date.class.getCanonicalName().equals(clazz.getCanonicalName())
					) {
				logger.debug("CASE DATE from LONG");
				Date d = new Date((Long) e.getValue());
				logger.debug("calling m='" + m + "' with value='" + d + "'");
				m.invoke(bean, d);
			} else if (Integer.class.getCanonicalName().equals(e.getValue().getClass().getCanonicalName())
					&&
					Long.class.getCanonicalName().equals(clazz.getCanonicalName())
					) {
				logger.debug("CASE LONG from INTEGER");
				Long v = new Long((Integer) e.getValue());
				logger.debug("calling m='" + m + "' with value='" + v + "'");
				m.invoke(bean, v);
			} else if (Double.class.getCanonicalName().equals(clazz.getCanonicalName())) {
				Double v;
				if (Integer.class.getCanonicalName().equals(e.getValue().getClass().getCanonicalName())) {
					logger.debug("CASE DOUBLE from INTEGER");
					v = new Double((Integer) e.getValue());
				} else {
					logger.debug("CASE DOUBLE from STRING");
					v = Double.parseDouble(e.getValue().toString());
				}
				logger.debug("calling m='" + m + "' with value='" + v + "'");
				m.invoke(bean, v);
			} else {
				logger.debug("CASE DEFAULT");
				logger.debug("calling m='" + bean.getClass().getSimpleName() + "." + m.getName() + "'" +
						" with value='" + e.getValue() + "'" +
						" value type='" + e.getValue().getClass().getSimpleName() + "'");
				m.invoke(bean, e.getValue());
			}
		}
	}
}
