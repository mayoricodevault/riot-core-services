package com.tierconnect.riot.cache;

import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.stat.Statistics;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.entities.EdgeboxRule;
import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.entities.ThingTypeField;
import com.tierconnect.riot.iot.entities.Zone;
import com.tierconnect.riot.iot.entities.ZonePoint;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;

/**
 * 
 * Test class for possible use in bridges (with or without storm)
 * 
 * @author tcrown
 *
 */
public class Main
{
	static Logger logger = Logger.getLogger( Main.class );

	public static void main( String args[] ) throws Exception
	{
		Main main = new Main();
		main.go( 10 );
	}

	public void go( int loopCount ) throws ClassNotFoundException
	{
		Class.forName( "org.gjt.mm.mysql.Driver" );
		Class.forName( "net.sourceforge.jtds.jdbc.Driver" );

		// System.getProperties().put( "hibernate.hbm2ddl.auto", "create" );
		// System.getProperties().put( "hibernate.cache.use_second_level_cache",
		// "false" );
		// System.getProperties().put( "hibernate.cache.use_query_cache",
		// "false" );

		System.getProperties().put( "hibernate.connection.url", "jdbc:mysql://localhost:3306/riot_main" );
		System.getProperties().put( "hibernate.connection.username", "root" );
		System.getProperties().put( "hibernate.connection.password", "control123!" );
		System.getProperties().put( "hibernate.dialect", "org.hibernate.dialect.SQLServerDialect" );

		System.getProperties().put( "hibernate.generate_statistics", "true" );
		System.getProperties().put( "hibernate.show_sql", "true" );


		long t0 = System.currentTimeMillis();

		/**
		 * EACH LOOP HERE REPRESENTS ONE TAG BLINK
		 * 
		 * INPUTS: bridgeCode, serialNumber
		 * 
		 * FETCHES: Edgebox, EdgeboxRules, ThingType, ThingTypeField, Zone,
		 * ZonePoint [more TBD]
		 * 
		 */
		String bridgeCode = "ALEB";
		String serialNumber = "12345";
		for( int i = 0; i < loopCount; i++ )
		{
			SessionFactory sf = HibernateSessionFactory.getInstance();
			Statistics stats = sf.getStatistics();
			Session s = sf.openSession();
			// s.setCacheMode( CacheMode.NORMAL );
			Transaction t = s.beginTransaction();

			System.out.println( "LOOP #" + i + ":" );

			// ok, this is cached
			Edgebox eb = this.getEdgeBoxById( s, bridgeCode );
			String conf = eb.getConfiguration();
			String thingTypeCode = this.getThingTypeCode( conf );
			Group g = eb.getGroup();
			List<EdgeboxRule> rules = getEdgeboxRules( s, eb );
			// rules = eb.getEdgeboxRules();
			ThingType tt = this.getThingTypeByCode( s, thingTypeCode );
			List<ThingTypeField> list_ttf = this.getThingTypeFields( s, tt );

			System.out.println( "eb.name=" + eb.getName() );
			System.out.println( "eb.rules=" + rules.size() );
			System.out.println( "eb.group=" + g.getName() );
			System.out.println( "thingType.name=" + tt.getName() );
			System.out.println( "thingType.ttfs.size=" + list_ttf.size() );

			List<Zone> zones = getZones( s );

			for( Zone z : zones )
			{
				System.out.println( "zone.name=" + z.getName() );
				System.out.println( "z=" + z.getCode() );
				System.out.println( "z=" + z.getDescription() );
				System.out.println( "z=" + z.getZoneGroup() );

				List<ZonePoint> zp = this.getZonePoints( s, z );
				System.out.println( "zone.zp.size=" + zp.size() );
			}

			System.out.println( "stats.connectCount              =" + stats.getConnectCount() );

			System.out.println( "stats.secondLevelCachePutCount  =" + stats.getSecondLevelCachePutCount() );
			System.out.println( "stats.secondLevelCacheHitCount  =" + stats.getSecondLevelCacheHitCount() );
			System.out.println( "stats.secondLevelCacheMissCount =" + stats.getSecondLevelCacheMissCount() );

			System.out.println( "stats.getNaturalIdCachePutCount =" + stats.getNaturalIdCachePutCount() );
			System.out.println( "stats.getNaturalIdCacheHitCount =" + stats.getNaturalIdCacheHitCount() );
			System.out.println( "stats.getNaturalIdCacheMissCount=" + stats.getNaturalIdCacheMissCount() );

			System.out.println( "stats.queryCachePutCount        =" + stats.getQueryCachePutCount() );
			System.out.println( "stats.queryCacheHitCount        =" + stats.getQueryCacheHitCount() );
			System.out.println( "stats.queryCacheMissCount       =" + stats.getQueryCacheMissCount() );

			System.out.println( "stats.entityFetchCount          =" + stats.getEntityFetchCount() );
			System.out.println( "stats.entityLoadCount           =" + stats.getEntityLoadCount() );

			System.out.println( "stats.collectionLoadCount       =" + stats.getCollectionLoadCount() );

			if( i > 0 && i % 1000 == 0 )
			{
				// System.out.println( "groupName=" + groupName );
				// System.out.println( "name=" + name );
				print( t0, i );
			}

			t.commit();
			s.close();

			System.out.println();
		}

		print( t0, loopCount );

		SessionFactory sf = HibernateSessionFactory.getInstance();
		Statistics stats = sf.getStatistics();
		if( stats.getEntityLoadCount() >= stats.getConnectCount() || stats.getCollectionLoadCount() >= stats.getConnectCount() )
		{
			System.out.println( "*** WARNING - SOMETHING IS NOT BEING CACHED ! ***" );
		}
		else
		{
			System.out.println( "*** CACHE OK ***" );
		}

		System.out.println( "done" );
	}

	private Edgebox getEdgeBoxById( Session s, String code )
	{
		Criteria c = s.createCriteria( Edgebox.class );
		c.add( Restrictions.eq( "code", code ) );
		// c.setFetchMode( "edgeboxRules", FetchMode.LAZY );
		c.setFetchMode( "group", FetchMode.SELECT );
		c.setCacheable( true );
		return (Edgebox) c.uniqueResult();
	}

	@SuppressWarnings("unchecked")
	private List<EdgeboxRule> getEdgeboxRules( Session s, Edgebox eb )
	{
		Criteria c = s.createCriteria( EdgeboxRule.class );
		c.add( Restrictions.eq( "edgebox", eb ) );
		c.setCacheable( true );
		return c.list();
	}

	private ThingType getThingTypeByCode( Session s, String thingTypeCode )
	{
		Criteria c = s.createCriteria( ThingType.class );
		c.add( Restrictions.eq( "thingTypeCode", thingTypeCode ) );
		// c.setFetchMode( "thingTypeFields", FetchMode.SELECT ); // does not
		// seem to work !
		// c.setFetchMode( "thingTypeFields.dataType", FetchMode.SELECT );
		// NOTE: had to set ThingType.thingTypeFields to FetchType.LAZY in
		// AppgenExtends.java !
		c.setCacheable( true );

		return (ThingType) c.uniqueResult();
	}

	@SuppressWarnings("unchecked")
	private List<ThingTypeField> getThingTypeFields( Session s, ThingType tt )
	{
		Criteria c = s.createCriteria( ThingTypeField.class );
		c.add( Restrictions.eq( "thingType", tt ) );
		c.setCacheable( true );
		return c.list();
	}

	private List<Zone> getZones( Session s )
	{
		Criteria c = s.createCriteria( Zone.class );
		// does not work
		// c.setFetchMode( "zonePoints", FetchMode.SELECT );
		// NOTE: had to set Zone.zonePoints to FetchType.LAZY in
		// AppgenExtends.java !
		c.setCacheable( true );
		return c.list();
	}

	private List<ZonePoint> getZonePoints( Session s, Zone zone )
	{
		Criteria c = s.createCriteria( ZonePoint.class );
		c.add( Restrictions.eq( "zone", zone ) );
		c.setCacheable( true );
		return c.list();
	}

	private String getThingTypeCode( String conf )
	{
		// TODO: parse JSON and extract thingTypeCode !
		return "default_rfid_thingtype";
	}

	void print( long t0, long n )
	{
		long t1 = System.currentTimeMillis();
		long delt = t1 - t0;
		double rate = (double) n / ((double) delt / 1000.0);
		// System.out.println( "delt=" + delt );
		System.out.println( String.format( "%d delt=%5d rate=%.2f [records/sec]", n, delt, rate ) );
	}
}
