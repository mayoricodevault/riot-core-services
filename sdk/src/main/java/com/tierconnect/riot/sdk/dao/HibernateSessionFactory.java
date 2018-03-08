package com.tierconnect.riot.sdk.dao;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;

/**
 * Created by agutierrez on 21-04-14.
 */
// TODO: AGG Rename to HibernateUtils
public class HibernateSessionFactory {
	private static final Logger logger = Logger.getLogger( HibernateSessionFactory.class );

	private volatile static SessionFactory INSTANCE;

	// only test
	public static boolean instanceTest = Boolean.FALSE;

//	public static final String resource = null;

	public static SessionFactory getInstance() {
		if (INSTANCE == null) {
			synchronized (HibernateSessionFactory.class) {
				if (INSTANCE == null) {
					boolean retry = true;
					do{
						Configuration configuration = new Configuration();
						// rsejas Todo: Verify and to delete comment code, resource always is null
						configuration.configure();
						ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().applySettings(
								configuration.getProperties()).build();
						try {
							INSTANCE = configuration.buildSessionFactory(serviceRegistry);
							retry = false;
						} catch (HibernateException e){
							logger.warn("Startup process for [Services] waiting for [HazelCast], retry in 30s");
							try {
								Thread.sleep(30000);
							} catch (InterruptedException e1) {
								Thread.currentThread().interrupt();
							}
						}
					}while (retry);

				}
			}
		}
		return INSTANCE;
	}

	/**
	 * This method was deprecated on 2016/09/27
	 * deprecated version: 4.3.0_RC13
	 */
	@Deprecated
	public static synchronized void restartInstance() {
		INSTANCE.close();
		Configuration configuration = new Configuration();
		configuration.configure();
		ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().applySettings(
				configuration.getProperties()).build();
		INSTANCE = configuration.buildSessionFactory(serviceRegistry);
	}

	public static synchronized void closeInstance() {
		if (INSTANCE != null) {
			INSTANCE.close();
		}
		if (INSTANCE!=null){
			INSTANCE.close();
		}
	}

	public static synchronized void setInstance(SessionFactory newInstance) {
		if (INSTANCE != null) {
			INSTANCE.close();
		}
		INSTANCE = newInstance;
	}

	public static synchronized void setInstanceForTest(SessionFactory newInstance) {
		if (INSTANCE == null) {
			INSTANCE = newInstance;
			instanceTest = Boolean.TRUE;
		}
	}
/*

	static Map<String, Class> mapResourceClass = new HashMap<>();
	static Map<Class, String> mapClassResource = new HashMap<>();

	static
	{
		List<Class> classes = getPersistedClasses();
		for( Class clazz : classes )
		{
			mapResourceClass.put( getResourceClassName( clazz ), clazz );
			mapClassResource.put( clazz, getResourceClassName( clazz ) );
		}
	}

	public static Map<String, Class> getResourceClassMapping()
	{
		return mapResourceClass;
	}

	public static Map<Class, String> getClassResourceMapping()
	{
		return mapClassResource;
	}

	public static List<Class> getPersistedClasses()
	{
		// Get the DOM Builder Factory
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		// Get the DOM Builder
		DocumentBuilder builder;

		List<Class> list = new ArrayList<Class>();

		try
		{
			builder = factory.newDocumentBuilder();
			// Load and Parse the XML document
			// document contains the complete XML as a Tree.
			InputStream hibernateResource = HibernateSessionFactory.class.getResourceAsStream( "/hibernate.cfg.xml" );
			// InputStream hibernateResource =
			// ClassLoader.getSystemResourceAsStream("/hibernate.cfg.xml");
			Document document = builder.parse( hibernateResource );
			// HibernateSessionFactory.class.getResourceAsStream("/hibernate.cfg.xml");

			// Iterating through the nodes and extracting the data.
			NodeList nodeListLevel1 = document.getDocumentElement().getChildNodes();

			for( int i = 0; i < nodeListLevel1.getLength(); i++ )
			{
				// We have encountered an <employee> tag.
				Node nodeLevel1 = nodeListLevel1.item( i );
				if( nodeLevel1 instanceof Element )
				{
					if( nodeLevel1.getNodeName().equals( "session-factory" ) )
					{
						NodeList nodeListLevel2 = nodeLevel1.getChildNodes();
						for( int j = 0; j < nodeListLevel2.getLength(); j++ )
						{
							Node nodeLevel2 = nodeListLevel2.item( j );
							if( nodeLevel2 instanceof Element )
							{
								if( nodeLevel2.getNodeName().equals( "mapping" ) )
								{
									try
									{
										String attribute = ((Element) nodeLevel2).getAttribute( "class" );
										list.add( Class.forName( attribute ) );
									}
									catch( ClassNotFoundException e )
									{
										e.printStackTrace();
									}
								}
							}
						}
					}
				}

			}
		}
		catch( ParserConfigurationException | IOException | SAXException e )
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return list;
	}

	public static List<String> getPersistentFields( Class clazz )
	{
		List<String> result = new ArrayList<>();
		while( clazz != null
				&& (clazz.getAnnotation( Entity.class ) != null || clazz.getAnnotation( MappedSuperclass.class ) != null) )
		{
			for( Field field : clazz.getDeclaredFields() )
			{
				if( !Modifier.isTransient( field.getModifiers() ) && (field.getAnnotation( Transient.class ) == null)
						&& !Modifier.isStatic( field.getModifiers() ) )
				{
					result.add( field.getName() );
				}
			}
			clazz = clazz.getSuperclass();
		}
		return result;
	}

	public static String getResourceClassName( Class clazz )
	{
		String simpleName = clazz.getSimpleName();
		return simpleName.substring( 0, 1 ).toLowerCase() + simpleName.substring( 1 );
	}

	public static void main( String[] args )
	{
		System.out.println( getPersistedClasses() );
	}
*/

}
