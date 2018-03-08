package com.tierconnect.riot.dao;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Assert;

import scala.util.Random;

//import com.tierconnect.riot.examples.vetclinic.entities.Dog;
//import com.tierconnect.riot.examples.vetclinic.entities.Flea;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;

/**
 * 
 * @author tcrown
 *
 */
public class HibernateTest3 
{
    static Logger logger = Logger.getLogger(HibernateTest3.class);

    public static void main(String[] args) 
    {
        //SessionFactory factory = HibernateSessionFactory.getInstance();

    	Map<String, String> properties = new HashMap<String, String>();
        properties.put( "javax.persistence.jdbc.driver", "org.gjt.mm.mysql.Driver" );
        properties.put( "javax.persistence.jdbc.url", "jdbc:mysql://localhost:3306/riot_main" );
        properties.put( "javax.persistence.jdbc.user", "root" );
        properties.put( "javax.persistence.jdbc.password", "control123!" );
        EntityManagerFactory factory = Persistence.createEntityManagerFactory( "riot", properties );
        
        
        ////EntityManagerFactory factory = Persistence.createEntityManagerFactory( "mname", properties );
        
        //EntityManager em = factory.createEntityManager();
        //Dog dog = em.find( Dog.class, 1 );
        
        SessionFactory sf = HibernateSessionFactory.getInstance();
        Session session = sf.getCurrentSession();
        
//        session.getTransaction().begin();
//        Dog dog = new Dog();
//        dog.setName( "dog" );
//        System.out.println( "********** DOG.ID=" + dog.getId() );
//        session.getTransaction().commit();
//        
//        session = sf.getCurrentSession();
//        session.getTransaction().begin();
//        dog = (Dog) session.get( Dog.class, 1L );
//        System.out.println( "********** DOG.NAME=" + dog.getName() );
//        for( Flea flea : dog.getFleas() )
//        {
//        	 System.out.println( "********** DOG.FLEA=" + flea.getName() + " id=" + flea.getId() );
//        }
//        session.getTransaction().commit();
//        //session.close();
//        
//        Random r = new Random();
//        session = sf.getCurrentSession();
//        session.getTransaction().begin();
//        dog = (Dog) session.get( Dog.class, 1L );
//        System.out.println( "********** DOG.NAME=" + dog.getName() );
//        for( Iterator<Flea> i = dog.getFleas().iterator(); i.hasNext(); )
//        {
//        	Flea flea = i.next();
//        	if( r.nextInt( 100 ) > 50 )
//        	{
//        		i.remove();	
//        		System.out.println( "REMOVED DOG.FLEA=" + flea.getName() + " id=" + flea.getId() );
//        	}
//        }
//        for( int i = 0; i < r.nextInt( 5 ); i++  )
//        {
//        	Flea flea = new Flea();
//        	flea.setName( r.nextString( 10 ) );
//        	dog.getFleas().add( flea );
//        	session.save( flea );
//        }
//        session.update( dog );
//        session.getTransaction().commit();
//        //session.close();
//        
//        session = sf.getCurrentSession();
//        session.getTransaction().begin();
//        dog = (Dog) session.get( Dog.class, 1L );
//        System.out.println( "********** DOG.NAME=" + dog.getName() );
//        for( Flea flea : dog.getFleas() )
//        {
//        	 System.out.println( "********** DOG.FLEA=" + flea.getName() + " id=" + flea.getId() );
//        }
//        session.getTransaction().commit();
//        
//        //session.update( );
//        
//        //Transaction t =  factory.getCurrentSession().beginTransaction();
//        //factory
//        //t.commit();
//        //Assert.assertEquals(5, 5);
//        
//        logger.info( "OK" );
        
        System.exit(0);
    }
}
